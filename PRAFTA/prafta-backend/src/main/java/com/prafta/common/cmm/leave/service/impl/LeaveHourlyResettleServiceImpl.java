package com.prafta.common.cmm.leave.service.impl;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
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
 * {@link LeaveHourlyResettleService} 구현 (연차 시간차 환산 개편 LC-05 — F1·F2).
 *
 * <p>plan §2 LC-05-② 재정산 알고리즘(의사코드)을 그대로 구현한다. 코어 산식은
 * {@link HourlyLeaveChargeUtils}(LC-03)를 공유 — 신청 계산과 재정산의 단일 출처.
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

            int conv = leaveConversionPolicyService.selectConversionMinutes(cmpnyCd, workYmd); // F4: 대상일 기준 분모
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpnyCd, siteCd, userCd, workYmd);

            // 시간순 재적용(지시서 F1): 각 건 charge = dayTotal(누적) − 직전 dayTotal 의 차액.
            //   dayTotal 은 누적 분에 단조 증가하므로 charge 는 항상 0 이상 — 음수 부과 없음.
            int cumMin = 0;
            BigDecimal cumCharged = BigDecimal.ZERO;
            int updatedCount = 0;
            Set<String> grantIds = new LinkedHashSet<>();
            for (HourlyLeaveUseRowVO row : rows) {
                if (row.leaveMinutes() == null) {
                    // plan §8-③: 시간차 분할행(LEAVE_MINUTES NULL)은 실데이터 0건 + LC-04 가불 차단으로
                    //   신규 유입 없음 — 비정상 데이터 방어(0분 취급, 로그만).
                    log.warn("[leave-resettle] LEAVE_MINUTES 없는 시간차 행 감지(0분 취급). leaveId={}", row.leaveId());
                }
                cumMin += (row.leaveMinutes() == null) ? 0 : row.leaveMinutes();
                BigDecimal dayTotal = HourlyLeaveChargeUtils.dayTotalDays(cumMin, conv, daily);
                BigDecimal charge = dayTotal.subtract(cumCharged);
                if (row.leaveDays() == null || charge.compareTo(row.leaveDays()) != 0) {
                    leaveHourlyResettleMapper.updateLeaveUseDays(cmpnyCd, row.leaveId(), charge, actorUserCd);
                    updatedCount++;
                    log.info("[leave-resettle] 시간차 재산출: leaveId={}, {}분(누적 {}분), {} → {}",
                            row.leaveId(), row.leaveMinutes(), cumMin,
                            row.leaveDays() == null ? null : row.leaveDays().toPlainString(),
                            charge.toPlainString());
                }
                cumCharged = dayTotal;
                if (row.grantId() != null && !row.grantId().isEmpty()) {
                    grantIds.add(row.grantId());
                }
            }

            // 원장 자동 정합(지시서 §3-1): 변경이 있었을 때만 영향 GRANT 전부 USED_DAYS 재집계.
            //   grantId=null('01' 사용자 신청 타입)은 GRANT 가 없어 생략(잔여=사용분 파생).
            if (updatedCount > 0) {
                for (String grantId : grantIds) {
                    leaveHourlyResettleMapper.recomputeGrantUsedDays(cmpnyCd, grantId, actorUserCd);
                }
            }

            log.info("[leave-resettle] 시간차 재정산 완료. userCd={}, workYmd={}, 대상 {}건, 변경 {}건, "
                            + "GRANT 재계산 {}건, conv={}, D={}",
                    userCd, workYmd, rows.size(), updatedCount,
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
