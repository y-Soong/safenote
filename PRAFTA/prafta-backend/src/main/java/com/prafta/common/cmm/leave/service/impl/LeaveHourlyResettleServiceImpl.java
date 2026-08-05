package com.prafta.common.cmm.leave.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.leave.mapper.LeaveHourlyResettleMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveDeductionService;
import com.prafta.common.cmm.leave.service.LeaveHourlyResettleService;
import com.prafta.common.cmm.leave.util.HourlyLeaveChargeUtils;
import com.prafta.common.cmm.leave.vo.HourlyLeaveUseRowVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AdvisoryLockTxUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveHourlyResettleService} 구현 (연차 시간차 환산 개편 LC-05 — F1·F2
 * → 개인 분모 개편 PC-01: REQ_ID 묶음 단위 재작업).
 *
 * <p>코어 산식은 {@link HourlyLeaveChargeUtils}(LC-03)를 공유 — 신청 계산과 재정산의 단일 출처.
 * PC-01(N1): 분할 INSERT(한 신청=여러 use 행, LEAVE_MINUTES 첫 행만)에 대비해 행 단위가 아닌
 * REQ_ID 묶음 단위로 분을 합산·재산출하고, charge 를 묶음 내 행들에 만료 임박순으로 배분한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveHourlyResettleServiceImpl implements LeaveHourlyResettleService {

    /** F5 직렬화 advisory lock 타임아웃(초) — 신청 흐름 leave01/leaveDay 패턴과 동일. */
    private static final int LOCK_TIMEOUT_SEC = 5;

    private final LeaveHourlyResettleMapper leaveHourlyResettleMapper;
    /** 분모(1일 환산시간, 신청 대상일 기준 F4) — LC-03 신청 흐름과 동일 출처. */
    private final LeaveConversionPolicyService leaveConversionPolicyService;
    /** 마일스톤 기준 D(그날 소정근로분) — com-016-D filterLockedDays 잠금으로 사후 불변. */
    private final LeaveDeductionService leaveDeductionService;

    @Override
    @Transactional
    public void resettleHourlyLeaveOnDate(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                          String actorUserCd) {
        if (cmpnyCd == null || userCd == null || workYmd == null) {
            // 필수 식별값 결손 — 재정산 불가(호출부 detail 조회 실패 등). 본 흐름을 깨지 않도록 no-op.
            log.warn("[leave-resettle] 필수 식별값 결손으로 재정산 생략. cmpnyCd={}, userCd={}, workYmd={}",
                    cmpnyCd, userCd, workYmd);
            return;
        }

        // F5: 신청 흐름(LC-04)과 같은 키로 직렬화 — 재정산 중 동시 신청 차단(상호 배타).
        String lockKey = HourlyLeaveChargeUtils.leaveDayLockKey(cmpnyCd, userCd, workYmd);
        acquireLock(lockKey);
        // 보안리뷰(Medium): 본 메서드는 호출부의 외부 트랜잭션에 join 하므로 finally 해제~외부
        //   커밋 사이 창이 가장 길다 — 그 창에서 동시 신청이 미커밋 재산출 결과로 그날 누적을
        //   판정할 수 있다. 해제를 (최외곽) 트랜잭션 완료(afterCompletion, 커밋/롤백 불문)
        //   시점으로 미룬다. 같은 커넥션 보장 근거는 AdvisoryLockTxUtils javadoc 참조.
        boolean lockDeferred = AdvisoryLockTxUtils.deferReleaseToAfterCompletion(lockKey, this::releaseLock);
        try {
            List<HourlyLeaveUseRowVO> rows =
                    leaveHourlyResettleMapper.selectHourlyLeaveRowsOnDate(cmpnyCd, userCd, workYmd);
            if (rows.isEmpty()) {
                // 그날 시간차 전멸 → 재산출 대상 없음(취소분 GRANT 재계산은 호출부가 이미 수행)
                return;
            }

            // E1(당일분모 전환): 분모 = 당일 배정 스케줄 소정근로분(480 캡 E7) — 신청 흐름과 동일 출처.
            //   E3 잠금(시간차 존재일 스케줄 변경 불가)으로 신규분은 재조회해도 원 차감 시점 분모와 동일(M4).
            //   산출 불가(사업장 이동 후 근무계획 소실·레거시 데이터 등)면 480 폴백 — 레거시(480/기본분모
            //   시절) 차감분 근사 유지. ★레거시 차감분은 신 분모(당일)로 재계산되어 값이 달라질 수 있음 —
            //   E6 무보정 원칙 승계로 허용(plan §7-② / qa §8-Q7).
            Integer convResolved = leaveConversionPolicyService.resolveDailyConvMinutes(cmpnyCd, siteCd, userCd, workYmd);
            int conv = (convResolved != null) ? convResolved : LeaveConversionPolicyService.DEFAULT_CONV_MINUTES;
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpnyCd, siteCd, userCd, workYmd);

            // PC-01(N1): REQ_ID 묶음 단위 재적용. 분할 INSERT(가불·일반 분할 차감)는 LEAVE_MINUTES 를
            //   첫 행에만 실으므로 행 단위 누적은 2번째 이후 행을 0분 취급해 원장을 누수시킨다 —
            //   묶음(신청) 합산 분으로 누적하고 재산출 charge 를 묶음 내 행들에 배분한다.
            //   REQ_ID NULL 행은 LEAVE_ID 단독 묶음(직접 차감 방어). 정렬(시간순, REQ 연속)은 SQL 보장.
            Map<String, List<HourlyLeaveUseRowVO>> groups = new LinkedHashMap<>();
            for (HourlyLeaveUseRowVO row : rows) {
                String key = (row.reqId() != null && !row.reqId().isEmpty())
                        ? "R:" + row.reqId() : "L:" + row.leaveId();
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }

            // PC-05: 짜투리 보전(COVER) 발동 REQ 묶음은 use 행 합 = "잔여 전액"(정상 요금과 상이) —
            //   재배분하면 잔여(0) 초과 차감이 되므로 행 갱신을 건너뛴다. 누적 분(cumMin)과 가상
            //   정상요금 누적(cumCharged=dayTotal)에는 그대로 포함해 후속 묶음 부과가 어긋나지 않게 한다
            //   (부담분 차이는 COVER 가 보존 — 근로자 원장에 전가하지 않는다).
            Set<String> coveredReqIds = new LinkedHashSet<>(
                    leaveHourlyResettleMapper.selectRemnantCoveredReqIds(cmpnyCd, userCd, workYmd));

            // 시간순 재적용(지시서 F1): 각 묶음 charge = dayTotal(누적) − 직전 dayTotal 의 차액.
            //   dayTotal 은 누적 분에 단조 증가하므로 charge 는 항상 0 이상 — 음수 부과 없음.
            int cumMin = 0;
            BigDecimal cumCharged = BigDecimal.ZERO;
            int updatedCount = 0;
            Set<String> grantIds = new LinkedHashSet<>();
            for (List<HourlyLeaveUseRowVO> group : groups.values()) {
                // 묶음 분 = Σ LEAVE_MINUTES (NULL=0 — 분할행은 첫 행만 분 보유하므로 합산이 곧 신청 분).
                int reqMinutes = 0;
                for (HourlyLeaveUseRowVO row : group) {
                    reqMinutes += (row.leaveMinutes() == null) ? 0 : row.leaveMinutes();
                    if (row.grantId() != null && !row.grantId().isEmpty()) {
                        grantIds.add(row.grantId());
                    }
                }
                if (reqMinutes <= 0) {
                    // 묶음 합산이 0분인 경우만 비정상(분할행 개별 NULL 은 정상 케이스 — PC-01).
                    log.warn("[leave-resettle] LEAVE_MINUTES 합산 0분 시간차 묶음 감지(0분 취급). 첫 leaveId={}",
                            group.get(0).leaveId());
                }
                cumMin += reqMinutes;
                BigDecimal dayTotal = HourlyLeaveChargeUtils.dayTotalDays(cumMin, conv, daily);
                BigDecimal groupCharge = dayTotal.subtract(cumCharged);

                // PC-05: COVER 발동 묶음은 행 재배분 skip(위 주석 참조) — 누적만 반영하고 다음 묶음으로.
                String groupReqId = group.get(0).reqId();
                if (groupReqId != null && coveredReqIds.contains(groupReqId)) {
                    log.info("[leave-resettle] 짜투리 보전 묶음 재배분 skip: reqId={}, 묶음 {}분(누적 {}분)",
                            groupReqId, reqMinutes, cumMin);
                    cumCharged = dayTotal;
                    continue;
                }

                // 배분: 부여 만료 임박순(AVAIL_TO_DATE ASC, null 최후)으로 각 행 기존 LEAVE_DAYS 를
                //   상한으로 채우고, 초과 잔량은 마지막(만료 최후순) 행에 가산(증가 재정산 흡수 — plan PC-01-②).
                List<HourlyLeaveUseRowVO> byExpiry = new ArrayList<>(group);
                byExpiry.sort(Comparator
                        .comparing((HourlyLeaveUseRowVO r) -> r.availToDate() == null ? "99999999" : r.availToDate())
                        .thenComparing(HourlyLeaveUseRowVO::leaveId));
                BigDecimal[] newDays = new BigDecimal[byExpiry.size()];
                BigDecimal remaining = groupCharge;
                for (int i = 0; i < byExpiry.size(); i++) {
                    BigDecimal cap = (byExpiry.get(i).leaveDays() == null)
                            ? BigDecimal.ZERO : byExpiry.get(i).leaveDays();
                    BigDecimal take = cap.min(remaining).max(BigDecimal.ZERO);
                    newDays[i] = take;
                    remaining = remaining.subtract(take);
                }
                if (remaining.signum() > 0) {
                    newDays[byExpiry.size() - 1] = newDays[byExpiry.size() - 1].add(remaining);
                }

                for (int i = 0; i < byExpiry.size(); i++) {
                    HourlyLeaveUseRowVO row = byExpiry.get(i);
                    if (row.leaveDays() == null || newDays[i].compareTo(row.leaveDays()) != 0) {
                        leaveHourlyResettleMapper.updateLeaveUseDays(cmpnyCd, row.leaveId(), newDays[i], actorUserCd);
                        updatedCount++;
                        log.info("[leave-resettle] 시간차 재산출: leaveId={}, reqId={}, 묶음 {}분(누적 {}분), {} → {}",
                                row.leaveId(), row.reqId(), reqMinutes, cumMin,
                                row.leaveDays() == null ? null : row.leaveDays().toPlainString(),
                                newDays[i].toPlainString());
                    }
                }
                cumCharged = dayTotal;
            }

            // 원장 자동 정합(지시서 §3-1): 변경이 있었을 때만 영향 GRANT 전부 USED_DAYS 재집계.
            //   grantId=null('01' 사용자 신청 타입)은 GRANT 가 없어 생략(잔여=사용분 파생).
            if (updatedCount > 0) {
                for (String grantId : grantIds) {
                    leaveHourlyResettleMapper.recomputeGrantUsedDays(cmpnyCd, grantId, actorUserCd);
                }
            }

            log.info("[leave-resettle] 시간차 재정산 완료. userCd={}, workYmd={}, 대상 {}행/{}묶음, 변경 {}건, "
                            + "GRANT 재계산 {}건, conv={}, D={}",
                    userCd, workYmd, rows.size(), groups.size(), updatedCount,
                    updatedCount > 0 ? grantIds.size() : 0, conv, daily);
        } finally {
            // afterCompletion 등록 성공 시 여기서 해제하지 않는다(이중 해제 방지) —
            //   커밋/롤백 직후 같은 커넥션에서 해제된다. 등록 실패(동기화 비활성) 시에만 폴백.
            if (!lockDeferred) {
                releaseLock(lockKey);
            }
        }
    }

    /**
     * advisory lock 획득. 타임아웃/오류면 동시 처리 중으로 보고 재시도 안내(트랜잭션 롤백 —
     * 반려/승인/삭제 본 흐름과 재정산은 원자적이어야 하므로 부분 반영을 남기지 않는다).
     */
    private void acquireLock(String lockKey) {
        Integer got = leaveHourlyResettleMapper.getAdvisoryLock(lockKey, LOCK_TIMEOUT_SEC);
        if (got == null || got != 1) {
            log.info("[leave-resettle] advisory lock 미획득 — lockKey={}, got={}", lockKey, got);
            throw new ApiException(AttdErrorCode.ATTD_409_071);
        }
    }

    /** advisory lock 해제(예외 무시 — 세션 종료 시 자동 해제됨). */
    private void releaseLock(String lockKey) {
        try {
            leaveHourlyResettleMapper.releaseAdvisoryLock(lockKey);
        } catch (Exception e) {
            log.warn("[leave-resettle] advisory lock 해제 실패(무시) — lockKey={}", lockKey, e);
        }
    }
}
