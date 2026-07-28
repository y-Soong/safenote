package com.prafta.web.attd.leaveflow.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.approval.mapper.ApprovalLineMapper;
import com.prafta.common.cmm.approval.vo.ApprovalStepVO;
import com.prafta.common.cmm.leave.mapper.LeavePolicyMapper;
import com.prafta.common.cmm.leave.service.LeaveApprovalNotiService;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveDeductionService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService.BorrowFamily;
import com.prafta.common.cmm.leave.service.LeaveHourlyResettleService;
import com.prafta.common.cmm.leave.service.LeaveRemnantCoverService;
import com.prafta.common.cmm.leave.util.FiscalYearUtils;
import com.prafta.common.cmm.leave.util.HourlyLeaveChargeUtils;
import com.prafta.common.cmm.leave.vo.BorrowGrantResultVO;
import com.prafta.common.cmm.leave.vo.BorrowGrantResultVO.BorrowGrantSlotVO;
import com.prafta.common.cmm.leave.vo.HourlyChargeVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.RemnantTriggerPlanVO;
import com.prafta.common.cmm.push.ApprovalResultNotiService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AdvisoryLockTxUtils;
import com.prafta.common.util.DateTimeUtils;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.leaveflow.application.command.LeaveReqInsertCommand;
import com.prafta.web.attd.leaveflow.application.param.LeaveApplyParam;
import com.prafta.web.attd.leaveflow.application.param.LeaveApprovalActionParam;
import com.prafta.web.attd.leaveflow.application.param.LeaveDeductionPreviewParam;
import com.prafta.web.attd.leaveflow.dto.response.LeaveDeductionPreviewResponse;
import com.prafta.web.attd.leaveflow.mapper.LeaveFlowMapper;
import com.prafta.web.attd.leaveflow.service.LeaveFlowService;
import com.prafta.web.attd.leaveflow.vo.AutoDeductibleGrantVO;
import com.prafta.web.attd.leaveflow.vo.DeductibleGrantVO;
import com.prafta.web.attd.leaveflow.vo.DirectLeaveResult;
import com.prafta.web.attd.leaveflow.vo.LeaveModifyTargetVO;
import com.prafta.web.attd.leaveflow.vo.LeaveReqRowVO;
import com.prafta.web.attd.leaveflow.vo.LeaveUseDetailVO;
import com.prafta.web.attd.leaveflow.vo.LeaveTypeInfoVO;
import com.prafta.web.attd.leaveflow.vo.LeaveUseVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveFlowService} 구현 (prafta-019-E).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveFlowServiceImpl implements LeaveFlowService {

    // 요청 유형 [SYS032]
    private static final String REQ_TYPE_LEAVE_USE = "05";
    private static final String REQ_TYPE_LEAVE_MODIFY = "06";
    // 요청 상태 [SYS033]
    private static final String REQ_APPLIED = "01";
    private static final String REQ_APPROVED = "02";
    private static final String REQ_REJECTED = "03";
    // 결재 단계 상태 [SYS044]
    private static final String STEP_WAIT = "00";
    private static final String STEP_APPLIED = "01";
    private static final String STEP_APPROVED = "02";
    private static final String STEP_REJECTED = "03";
    // 사용 단위 [SYS025]
    private static final String UNIT_FULL = "00";
    private static final String UNIT_HALF = "01";
    private static final String UNIT_HOUR2 = "02";
    private static final String UNIT_HOUR1 = "03";
    private static final String UNIT_MIN30 = "04";
    /** 반반차(0.25일 고정단위). 처리 = 반차 패턴 미러(시간대 미기록, 시간차 누적 미포함). */
    private static final String UNIT_QUARTER = "05";

    /** LC-10: 반반차를 개방하는 회사 사용단위 값(tb_leave_usage_policy.USAGE_UNIT). */
    private static final String USAGE_UNIT_QUARTER_DAY = "QUARTER_DAY";

    private static final String USE_CONFIRMED = "CONFIRMED";

    /** 연차개편: 사용자 신청 타입 [SYS021] '01'. 한도=MAX_APLY_DAYS, 잔여=회계연도 사용분 차감. */
    private static final String LEAVE_TYPE_USER_APPLY = "01";
    /** prafta-com-016-B(3-1): 사용가능기간 [SYS026] '01' 설정안함 = 전체 누적(lifetime). 그 외는 회계연도 윈도우. */
    private static final String AVAIL_TERM_NONE = "01";
    /** 연차개편 동시성: '01' 신청 직렬화 advisory lock 타임아웃(초). */
    private static final int LEAVE01_LOCK_TIMEOUT_SEC = 5;

    /** 근무계획 연차 셀 비우기 시 직접 사용기록 취소 사유 (prafta-041) */
    private static final String CANCEL_REASON_PLAN_CLEAR = "근무계획 연차 비우기";

    // ===== prafta-com-011-2 가불 시스템 연차 코드(엔진 LEAVE_CD 상수와 동일) =====
    /** 본연차 시스템 코드 → BorrowFamily.ANNUAL. */
    private static final String LEAVE_CD_ANNUAL = "SYS_ANNUAL";
    /** 월차 시스템 코드 → BorrowFamily.MONTHLY. */
    private static final String LEAVE_CD_MONTHLY = "SYS_MONTHLY";

    private final LeaveFlowMapper leaveFlowMapper;
    private final ApprovalLineMapper approvalLineMapper;
    private final LeaveDeductionService leaveDeductionService;
    private final AttdCloseService attdCloseService;
    /** 연차개편: 활성정책 AXIS2_FISCAL_START_MM/_DD 조회(회계연도 경계 산출 입력). 락 없는 조회 메서드 재사용. */
    private final LeavePolicyMapper leavePolicyMapper;
    /** PRAFTA-COM-004: 연차 결재 PUSH 생산자(outbox 적재, 예외 격리). */
    private final LeaveApprovalNotiService leaveApprovalNotiService;
    /** PRAFTA-APP-021-3a(W2): 연차 결재 결과(승인/반려) 통보 PUSH 생산자(신청자 1인, afterCommit 격리). */
    private final ApprovalResultNotiService approvalResultNotiService;
    /** prafta-com-011-2 가불: 한도 projection/만료검증/가불 GRANT 생성·회수 코어(com-011-1 산출). */
    private final LeaveGrantEngineService leaveGrantEngineService;
    /** LC-05(F1·F2): 시간차 취소·반려·최종승인 시 그날 잔존 건 시간순 재정산(코어 산식 LC-03 공유). */
    private final LeaveHourlyResettleService leaveHourlyResettleService;
    /** LC-07: preview 응답의 convMinutes(고정단위 케이스) — 신청 대상일 기준 환산시간(F4) 단일 출처. */
    private final LeaveConversionPolicyService leaveConversionPolicyService;
    /** PC-05/06: 짜투리 잔여 보전 — 발동 판정(D5)·발동 처리(D6)·회수(D7) 단일 출처(앱과 공유 빈). */
    private final LeaveRemnantCoverService leaveRemnantCoverService;

    @Override
    @Transactional
    public void submitLeave(LeaveApplyParam p) {
        final String cmpny = p.gvCmpnyCd();
        final String site = p.gvSiteCd();
        final String user = p.gvUserCd();
        final String workYmd = p.workYmd();
        final String leaveCd = p.leaveCd();
        final String unit = p.useUnitType();

        // 1) 타입 메타 + 결재 여부 (회사정의=타입 APRV_USE_YN, 법정 시드=정책 APRV_USE_YN)
        LeaveTypeInfoVO type = leaveFlowMapper.selectLeaveTypeInfo(cmpny, leaveCd);
        if (type == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }
        boolean statutory = "Y".equals(type.systemYn());
        boolean aprvRequired;
        if (statutory) {
            aprvRequired = "Y".equals(leaveFlowMapper.selectPolicyAprvUseYn(cmpny));
        } else {
            aprvRequired = "Y".equals(type.aprvUseYn());
        }

        // 2) 사용 단위 검증 + 차감 계산.
        //    연차개편(LC-03/LC-04): 시간차(02/03/04)는 여기서 구조 검증만 하고, 차감액(그날 누적
        //    기준 차액)은 아래 try 블록에서 사용자·일 advisory lock 획득 후 산출한다(F5 직렬화).
        boolean hourlyUnit = UNIT_HOUR2.equals(unit) || UNIT_HOUR1.equals(unit) || UNIT_MIN30.equals(unit);
        BigDecimal leaveDays = null;
        Integer leaveMinutes = null;
        String startTime = null;
        String endTime = null;

        if (UNIT_FULL.equals(unit)) {
            leaveDays = new BigDecimal("1.00000");
        } else if (UNIT_HALF.equals(unit)) {
            leaveDays = new BigDecimal("0.50000");
            // 반차는 소정근로의 절반을 차감하므로 근무 스케줄이 있어야 한다.
            //   스케줄 없는 날(getDailyStdWorkMinutes==null)은 반차 신청 불가(종일 연차만 가능).
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd);
            if (daily == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_110);
            }
            leaveMinutes = daily / 2;
        } else if (UNIT_QUARTER.equals(unit)) {
            // 반반차 = 반차 패턴 미러. leaveDays 0.25 고정, 분 = daily/4(정수 나눗셈),
            //   시간대(START/END_TIME) 미기록(plan §8-④). 시간차 누적(하한 마일스톤)엔 미포함(§8-⑤),
            //   1.0 점유 가드에는 아래 selectOccupiedLeaveDaysOnDate 합산으로 자동 포함.
            // LC-10 허용 게이트: 법정 = 회사 사용정책 USAGE_UNIT='QUARTER_DAY'(null=미허용 fail-closed),
            //   비법정 = 타입 USE_UNIT_TYPE='05' 명시 설정(계층 삽입 없음 — 하위호환).
            boolean quarterAllowed = statutory
                    ? USAGE_UNIT_QUARTER_DAY.equals(leaveFlowMapper.selectPolicyUsageUnit(cmpny))
                    : UNIT_QUARTER.equals(type.useUnitType());
            if (!quarterAllowed) {
                log.info("[leaveflow] 반반차 신청 거부: 미허용 (userCd={}, leaveCd={}, statutory={})",
                        user, leaveCd, statutory);
                throw new ApiException(AttdErrorCode.ATTD_400_054);
            }
            leaveDays = new BigDecimal("0.25000");
            // 반반차도 소정근로 기준 분(daily/4)을 병기하므로 스케줄 필수(반차와 동일 거부).
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd);
            if (daily == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_110);
            }
            leaveMinutes = daily / 4;
        } else if (hourlyUnit) {
            startTime = p.startTime();
            endTime = p.endTime();
            Integer sMin = DateTimeUtils.hhmmToMinutes(startTime);
            Integer eMin = DateTimeUtils.hhmmToMinutes(endTime);
            if (sMin == null || eMin == null || eMin <= sMin) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            int minutes = eMin - sMin;
            int unitMin = unitMinutes(unit);
            if (minutes % unitMin != 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_054);
            }
            // 시간차 마일스톤(하한 가드) 기준 D = 그날 소정근로분 — 스케줄 필수(기존 가드 유지).
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd);
            if (daily == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_110);
            }
            if (minutes > daily) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            // 휴게 가로지름 거부 (§8.5.9) — 휴게시각 미설정이면 skip
            if (leaveDeductionService.crossesBreak(cmpny, site, user, workYmd, sMin, eMin)) {
                throw new ApiException(AttdErrorCode.ATTD_400_055);
            }
            leaveMinutes = minutes;
            // 차감액(leaveDays)은 lock 획득 후 calcHourlyCharge 로 산출(아래 try 블록).
        } else {
            throw new ApiException(AttdErrorCode.ATTD_400_054);
        }

        // 3) 사후 신청은 근태 마감 전까지만 (결정 #4)
        //    PRAFTA-028: 부서 단위 마감 도입 → 신청자 소속부서 기준으로 정밀 판정한다
        //    (다른 부서가 마감돼도 본 신청자 부서가 열려 있으면 사후 신청 허용).
        String today = todayYmd();
        if (workYmd.compareTo(today) < 0) {
            String closeYm = workYmd.substring(0, 6);
            if (attdCloseService.isClosedForUser(cmpny, site, user, closeYm)) {
                throw new ApiException(AttdErrorCode.ATTD_400_050);
            }
        }

        // 4) 잔여 확보(타입 분기 — 앱 AppLeaveFlowServiceImpl 미러).
        //    '02'(또는 SYSTEM_YN='Y'): 기존 차감 GRANT 경로(만료 임박 우선, FOR UPDATE) 유지 — 회귀 0.
        //    '01'(사용자 신청): GRANT 가 없어 회계연도 한도(MAX_APLY_DAYS) 대비 사용분 검증.
        //      FOR UPDATE 를 못 쓰니 (USER_CD,LEAVE_CD) advisory lock 으로 직렬화
        //      → 사용분 재집계 → 한도검증 → INSERT 순서로 중복신청 레이스를 방지한다.
        boolean userApplyType = LEAVE_TYPE_USER_APPLY.equals(type.leaveType()) && !statutory;

        // prafta-com-011-2 가불: isBorrow=true 면 결재 강제(결정 §4) + 시스템 법정 월차/본연차만 허용.
        //   직접입력/무결재 자동확정 경로로 가불 진입 차단(가불은 항상 결재선 필수). 식별값은 토큰 도출.
        boolean borrow = p.isBorrow();
        BorrowFamily borrowFamily = null;
        String hireDate = null;
        if (borrow) {
            if (hourlyUnit || UNIT_QUARTER.equals(unit)) {
                // LC-04/LC-06(plan §8-③): 시간차·반반차 + 가불 조합 서버 거부 — 가불은 종일/반차만.
                //   (가불 분할 INSERT 는 LEAVE_MINUTES 를 첫 행에만 실어 그날 누적 판정을 오염시킴.)
                throw new ApiException(AttdErrorCode.ATTD_400_183);
            }
            if (!statutory || userApplyType) {
                throw new ApiException(AttdErrorCode.ATTD_400_180); // 가불=법정(시스템) 연차만
            }
            borrowFamily = borrowFamilyOf(leaveCd);
            if (borrowFamily == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_180); // 월차/본연차 외 법정타입은 가불 비대상
            }
            aprvRequired = true; // 결재 강제(체크박스/타입 APRV_USE_YN 무시)
            hireDate = leaveFlowMapper.selectUserHireDate(cmpny, user);
            // 만료(소멸) 경과 일자 fail-closed 차단(서버, 프론트 alert 우회 방지).
            leaveGrantEngineService.assertBorrowWorkYmdWithinExpiry(cmpny, user, hireDate, workYmd, borrowFamily);
        }

        List<GrantCharge> charges = null; // 차감 대상(부여 ID + 일수). '01'은 [(null, leaveDays)]. 짜투리 발동 시 null.
        RemnantTriggerPlanVO remnantPlan = null; // PC-05: 짜투리 발동 계획(발동 시 charges 대신 사용)
        Integer hourlyConv = null;       // 시간차 분모(발동 판정 입력 — calcHourlyCharge 결과 재사용)
        String lockKey = null;
        String dayLockKey = null;
        String remnantLockKey = null;
        boolean lockDeferred = false;    // '01' lock 해제가 afterCompletion 에 등록됐는지
        boolean dayLockDeferred = false; // leaveDay lock 해제가 afterCompletion 에 등록됐는지
        boolean remnantLockDeferred = false; // leaveRemnant lock 해제가 afterCompletion 에 등록됐는지
        try {
            if (hourlyUnit) {
                // F5: 같은 사용자·같은 날 시간차 누적 판정 직렬화 — leave01 advisory lock 패턴 재사용.
                //   재정산(LC-05)과 같은 키로 상호 배타(재정산 중 신청 차단).
                dayLockKey = HourlyLeaveChargeUtils.leaveDayLockKey(cmpny, user, workYmd);
                acquireLeaveDayLock(dayLockKey);
                // 보안리뷰(Medium): finally 해제는 커밋 이전이라 "해제~커밋" 창에서 후속 신청이
                //   미커밋 스냅샷으로 그날 누적(하한 마일스톤/점유 가드)을 판정할 수 있다
                //   → 해제를 트랜잭션 완료(afterCompletion, 커밋/롤백 불문) 시점으로 미룬다.
                //   같은 커넥션 보장 근거는 AdvisoryLockTxUtils javadoc 참조. 등록 실패 시 finally 폴백.
                dayLockDeferred = AdvisoryLockTxUtils.deferReleaseToAfterCompletion(dayLockKey, this::releaseLeaveDayLock);

                // LC-03: 분모 = 회사 환산시간(신청 대상일 기준, F4). 그날 시간차 누적(전 타입 합산, F3)
                //   기준 하한(3단 마일스톤)/캡(1.0) 가드를 거친 이번 건 차액을 부과한다.
                HourlyChargeVO hc = leaveDeductionService.calcHourlyCharge(cmpny, site, user, workYmd, leaveMinutes);
                if (hc == null) {
                    throw new ApiException(AttdErrorCode.ATTD_400_052);
                }
                leaveDays = hc.chargeDays();
                hourlyConv = hc.convMinutes(); // PC-05: 발동 판정 입력(분모 재조회 없이 재사용)
                log.info("[leaveflow] 시간차 차감 산출: userCd={}, workYmd={}, {}분(누적 {}분), conv={}, "
                                + "charge={}, dayTotal={}, 하한={}, 캡={}",
                        user, workYmd, leaveMinutes, hc.cumMinutesAfter(), hc.convMinutes(),
                        hc.chargeDays().toPlainString(), hc.dayTotalDays().toPlainString(),
                        hc.floorApplied(), hc.capApplied());
            }

            // 3-B) 같은 날 중복 등록 가드(앱 AppLeaveFlowServiceImpl 미러).
            //   - 이미 점유된 연차 일수(종일=1.0 / 반차·시간차=LEAVE_DAYS) + 신규 신청 일수 > 1.0 이면 거부.
            //     → 종일 등록일에 반차 추가, 종일 중복, 반차 누적 초과 등 "하루 초과" 중복 차단(ATTD_400_111).
            //   - 시간차(02/03/04)는 합산이 1.0 이하여도 기존 시간차와 시간대가 겹치면 거부(ATTD_400_112).
            //     겹치지 않는 시간차 병행 신청은 허용(정책).
            //   (LC-04: 시간차 leaveDays 산출이 lock 하에 이뤄지므로 본 가드도 try 내부로 이동 — 판정 동일.)
            BigDecimal occupied = leaveFlowMapper.selectOccupiedLeaveDaysOnDate(cmpny, user, workYmd);
            if (occupied == null) {
                occupied = BigDecimal.ZERO;
            }
            if (occupied.add(leaveDays).compareTo(BigDecimal.ONE) > 0) {
                log.info("[leaveflow] 연차 신청 거부: 같은 날 하루 초과 중복 (userCd={}, workYmd={}, 점유={}, 신규={})",
                        user, workYmd, occupied.toPlainString(), leaveDays.toPlainString());
                throw new ApiException(AttdErrorCode.ATTD_400_111);
            }
            if (hourlyUnit) {
                if (leaveFlowMapper.countOverlappingTimeLeaveOnDate(cmpny, user, workYmd, startTime, endTime) > 0) {
                    log.info("[leaveflow] 연차 신청 거부: 같은 날 시간차 시간대 겹침 (userCd={}, workYmd={}, {}~{})",
                            user, workYmd, startTime, endTime);
                    throw new ApiException(AttdErrorCode.ATTD_400_112);
                }
            }

            if (userApplyType) {
                lockKey = leave01LockKey(cmpny, user, leaveCd);
                acquireLeave01Lock(lockKey);
                // 보안리뷰(Medium): leaveDay lock 과 동일 사유 — 회계연도 사용분 합산 판정도
                //   커밋 이전 해제 시 같은 창이 생기므로 afterCompletion 해제로 통일.
                lockDeferred = AdvisoryLockTxUtils.deferReleaseToAfterCompletion(lockKey, this::releaseLeave01Lock);

                Integer maxAplyDays = type.maxAplyDays();
                if (maxAplyDays == null) {
                    log.info("[leaveflow] 연차 신청 거부: 사용자 신청 한도(MAX_APLY_DAYS) 미설정 (userCd={}, leaveCd={})", user, leaveCd);
                    throw new ApiException(AttdErrorCode.ATTD_400_051);
                }
                // prafta-com-016-B(3-1): 사용가능기간 분기.
                //   '01' 설정안함 = 전체 누적(윈도우 없음, lifetime), 그 외('02' 해당연도내 포함) = 회계연도 윈도우(현행).
                BigDecimal used;
                if (AVAIL_TERM_NONE.equals(type.availTermType())) {
                    used = leaveFlowMapper.selectTotalUsedDays(cmpny, user, leaveCd);
                } else {
                    FiscalYearUtils.FiscalWindow fiscal = resolveFiscalWindow(cmpny);
                    used = leaveFlowMapper.selectFiscalUsedDays(
                            cmpny, user, leaveCd, fiscal.fiscalStartYmd(), fiscal.fiscalEndYmdExclusive());
                }
                if (used == null) {
                    used = BigDecimal.ZERO;
                }
                if (used.add(leaveDays).compareTo(BigDecimal.valueOf(maxAplyDays)) > 0) {
                    log.info("[leaveflow] 연차 신청 거부: 사용가능기간({}) 한도 초과 (userCd={}, leaveCd={}, used={}, req={}, max={})",
                            type.availTermType(), user, leaveCd, used, leaveDays, maxAplyDays);
                    throw new ApiException(AttdErrorCode.ATTD_400_051);
                }
                charges = List.of(new GrantCharge(null, leaveDays));
            } else if (borrow) {
                // prafta-com-011-2 (Q1=b): 잔여 우선 차감 + 부족분만 가불.
                charges = resolveBorrowCharges(cmpny, user, leaveCd, workYmd, leaveDays, borrowFamily, hireDate, user);
            } else {
                // PC-02(D8): 일반 신청 분할 차감 — 단일 부여 전량 충당(selectDeductibleGrant 단건)에서
                //   만료 임박순 다부여 분할 충당으로 교체(조각 부여 교착 해소, resolveBorrowCharges
                //   잔여 우선 루프와 동일 패턴 — 가불 없이). 합산 잔여 부족이면 기존 ATTD_400_051 유지.
                // PC-05(D3~D6): 잔여 부족 지점에서 짜투리 발동을 판정해 충족 시 거부 대신 발동 처리(앱 미러).
                if (leaveDays.signum() > 0
                        && sumDeductibleRemaining(cmpny, user, leaveCd, workYmd).compareTo(leaveDays) < 0) {
                    // N9: 발동 판정~기록을 사용자 단위 advisory lock 으로 직렬화.
                    //   시간차 신청이면 위 leaveDay lock 이후 획득(획득 순서 leaveDay → leaveRemnant 고정 — 데드락 방지).
                    remnantLockKey = LeaveRemnantCoverService.remnantLockKey(cmpny, user);
                    acquireRemnantLock(remnantLockKey);
                    remnantLockDeferred = AdvisoryLockTxUtils.deferReleaseToAfterCompletion(
                            remnantLockKey, this::releaseRemnantLock);
                    Integer convForRemnant = (hourlyConv != null)
                            ? hourlyConv
                            : leaveConversionPolicyService.resolvePersonalConvMinutes(cmpny, user, workYmd);
                    remnantPlan = leaveRemnantCoverService.evaluateTrigger(
                            cmpny, user, workYmd, leaveCd, unit, leaveMinutes, leaveDays, convForRemnant);
                    if (remnantPlan == null) {
                        log.info("[leaveflow] 연차 신청 거부: 합산 잔여 부족(짜투리 발동 비대상) "
                                        + "(userCd={}, leaveCd={}, needed={})",
                                user, leaveCd, leaveDays.toPlainString());
                        throw new ApiException(AttdErrorCode.ATTD_400_051);
                    }
                    // D6: 실제 차감 = 잔여 전액 — 요청(REQ.LEAVE_DAYS)·통보도 실차감 기준(원장·표시 정합).
                    leaveDays = remnantPlan.remnantDays();
                } else {
                    charges = resolveGeneralCharges(cmpny, user, leaveCd, workYmd, leaveDays);
                }
            }

        // 5) 요청 생성 (REQ_TYPE='05'). 결재 Y면 신청('01'), N이면 즉시 승인('02').
        String reqId = leaveFlowMapper.selectNextReqId(cmpny);
        String reqStatus = aprvRequired ? REQ_APPLIED : REQ_APPROVED;
        leaveFlowMapper.insertLeaveReq(new LeaveReqInsertCommand(
                reqId, cmpny, site, user, reqStatus, p.reason(), workYmd, p.nodeCd(),
                workYmd, startTime, workYmd, endTime, p.leaveType(), leaveDays, user));

        // 6) 결재 Y → 라인 일괄 생성. 자기 승인 원칙(§9.5): 본인이 결재자인 단계는
        //    소속 노드 자체근태승인(SELF_ATTD_APPRV_YN) ON + 본인이 그 노드 담당 정/부일 때만
        //    자동승인('02'), 자격 미달이면 본인 지정 불가(ATTD_400_056).
        boolean fullyAutoApproved = false;
        // PRAFTA-COM-004 시나리오 A hook 용: 차례가 도래한(=STEP_APPLIED) 첫 수동 단계의 결재자/단계번호.
        String turnApprover = null;
        int turnStep = -1;
        if (aprvRequired) {
            List<String> approvers = p.approverUserCds();
            if (approvers == null || approvers.isEmpty()) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            // PRAFTA-COM-004 보안(결재자 스코프 가드): 본문 approverUserCds 는 신뢰 입력이므로, 각 결재자가
            //   동일 회사 + 동일 사업장 + 재직 사용자인지 서버에서 검증한다(타 사업장/회사/존재없는
            //   USER_CD 를 결재자로 주입 → 그 결재함에 신청자 실명 PUSH 가 노출되는 cross-tenant 침해 차단).
            //   중복 제거 수와 유효 사용자 수가 다르면 거부(앱 AppLeaveFlowServiceImpl 미러).
            List<String> distinctApprovers = new ArrayList<>(new LinkedHashSet<>(approvers));
            int validCount = leaveFlowMapper.countValidApprovers(cmpny, site, distinctApprovers);
            if (validCount != distinctApprovers.size()) {
                log.info("[leaveflow] 연차 신청 거부: 유효하지 않은 결재자 포함 (userCd={}, 요청={}, 유효={})",
                        user, distinctApprovers.size(), validCount);
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            boolean selfAllowed = "Y".equals(leaveFlowMapper.selectUserNodeSelfApproveYn(cmpny, user));

            int currentIdx = -1; // 첫 수동(비-자동승인) 단계 인덱스
            for (int i = 0; i < approvers.size(); i++) {
                boolean isSelf = user.equals(approvers.get(i));
                if (isSelf && !selfAllowed) {
                    throw new ApiException(AttdErrorCode.ATTD_400_056);
                }
                if (!isSelf && currentIdx < 0) {
                    currentIdx = i;
                }
            }
            for (int i = 0; i < approvers.size(); i++) {
                String approver = approvers.get(i);
                boolean isSelf = user.equals(approver);
                ApprovalStepVO s = new ApprovalStepVO();
                s.setReqId(reqId);
                s.setApprovalStep(i + 1);
                s.setCmpnyCd(cmpny);
                s.setApproverUserCd(approver);
                if (isSelf) {
                    s.setApprovalStatus(STEP_APPROVED); // 본인 + 자체근태승인 ON → 자동 승인
                    s.setApprovalComment("자체근태승인 자동 승인");
                } else {
                    s.setApprovalStatus(i == currentIdx ? STEP_APPLIED : STEP_WAIT);
                }
                s.setInsertNo(user);
                approvalLineMapper.insertApprovalStep(s);
            }
            fullyAutoApproved = (currentIdx < 0); // 전 단계가 본인 자동승인 → 즉시 확정
            if (currentIdx >= 0) {
                // 차례가 도래한 첫 수동 단계(STEP_APPLIED) — 시나리오 A 발송 대상.
                turnApprover = approvers.get(currentIdx);
                turnStep = currentIdx + 1; // approvalStep 은 1-based
            }
        }

        // 7) 차감 예약 (CONFIRMED) + 부여 USED_DAYS 동기화.
        //    비가불/'01'은 charges 가 1건([단일 grantId, leaveDays] 또는 [null, leaveDays])이라 기존과 동일 동작.
        //    가불(Q1=b)은 잔여+가불 여러 GRANT 로 분할 차감될 수 있어 charge 별로 leave_use 를 분할 INSERT 한다.
        //    '01'(사용자 신청)은 grantId=null → GRANT 가 없으므로 recomputeGrantUsedDays 생략(잔여=회계연도 사용분 파생).
        //    leaveMinutes 는 분할 시 모호하므로 첫 charge 에만 싣는다(가불은 종일 위주, 표시·집계는 일수 기준).
        String leaveId = null; // 무결재 즉시확정 통보(notifyLeaveUsedNoAprv)용 — 마지막 INSERT 한 사용기록 ID.
        if (remnantPlan != null) {
            // PC-05(D6): 짜투리 발동 — 잔여 전액을 대상 5종 부여로 분할 차감(use 행: 신청 REQ_ID·단위,
            //   LEAVE_CD 는 부여 귀속) + 회사 부담분 TB_LEAVE_REMNANT_COVER 기록 + 영향 GRANT 재집계.
            leaveId = leaveRemnantCoverService.applyTrigger(cmpny, site, user, workYmd, unit,
                    startTime, endTime, leaveMinutes, p.reason(), reqId, remnantPlan, user);
        } else {
            boolean firstCharge = true;
            for (GrantCharge charge : charges) {
                leaveId = leaveFlowMapper.selectNextLeaveId(cmpny);
                LeaveUseVO use = LeaveUseVO.builder()
                        .leaveId(leaveId).cmpnyCd(cmpny).siteCd(site).userCd(user).leaveCd(leaveCd)
                        .reqId(reqId).grantId(charge.grantId())
                        .startDate(workYmd).startTime(startTime).endDate(workYmd).endTime(endTime)
                        .useUnitType(unit).leaveDays(charge.days()).leaveMinutes(firstCharge ? leaveMinutes : null)
                        .leaveReason(p.reason()).leaveStatus(USE_CONFIRMED).insertNo(user)
                        .build();
                leaveFlowMapper.insertLeaveUse(use);
                if (charge.grantId() != null) {
                    leaveFlowMapper.recomputeGrantUsedDays(cmpny, charge.grantId(), user);
                }
                firstCharge = false;
            }
        }

        // 결재 Y인데 전 단계가 본인 자동승인이면 요청 즉시 확정(§9.5 자기 승인 원칙)
        if (aprvRequired && fullyAutoApproved) {
            leaveFlowMapper.updateReqStatus(cmpny, reqId, REQ_APPROVED, user, "자체근태승인 자동 승인");
        }
        // prafta-com-008-E-2: 연차-스케줄 모델 전환 — work_plan 에 LEAVE_CD 를 더 이상 쓰지 않는다.
        //   출근 차단(§8.3)은 leave_use(CONFIRMED 종일) 존재 기준으로 판정한다(work_plan 은 SCH_CD 유지).
        //   따라서 위 insertLeaveUse 가 곧 차단 근거 — 별도 work_plan 연차 덮어쓰기 불필요.

        // PRAFTA-COM-004 PUSH 적재 hook (예외 격리 — 연차 신청 본 흐름에 영향 금지).
        //  - 시나리오 A: 결재 Y + 차례 도래 단계(첫 수동)가 있을 때 그 결재자 1인에게.
        //    fullyAutoApproved(전건 본인 자동승인)면 turnApprover=null → 미발송(D1).
        //  - 시나리오 B: 순수 무결재(!aprvRequired) 즉시확정만. fullyAutoApproved 는 제외(D1).
        try {
            if (aprvRequired && turnApprover != null) {
                leaveApprovalNotiService.notifyApprovalTurn(cmpny, site, user, reqId, turnStep, turnApprover, user);
            }
            if (!aprvRequired) {
                leaveApprovalNotiService.notifyLeaveUsedNoAprv(cmpny, site, user, leaveId, unit, leaveDays,
                        workYmd, startTime, endTime, user);
            }
        } catch (Exception e) {
            log.error("연차 신청 PUSH 적재 hook 실패(신청 영향 없음). reqId={}", reqId, e);
        }

        log.info("연차 신청 완료. reqId={}, user={}, leaveCd={}, unit={}, days={}, aprv={}",
                reqId, user, leaveCd, unit, leaveDays, aprvRequired);
        } finally {
            // afterCompletion 등록 성공분은 여기서 해제하지 않는다(이중 해제 방지) —
            //   커밋/롤백 직후 같은 커넥션에서 해제된다. 등록 실패(동기화 비활성) 시에만 폴백.
            if (lockKey != null && !lockDeferred) {
                releaseLeave01Lock(lockKey);
            }
            if (dayLockKey != null && !dayLockDeferred) {
                releaseLeaveDayLock(dayLockKey);
            }
            if (remnantLockKey != null && !remnantLockDeferred) {
                releaseRemnantLock(remnantLockKey);
            }
        }
    }

    @Override
    public LeaveDeductionPreviewResponse previewDeduction(LeaveDeductionPreviewParam p) {
        final String cmpny = p.gvCmpnyCd();
        final String site = p.gvSiteCd();
        final String user = p.gvUserCd();
        final String workYmd = p.workYmd();
        final String leaveCd = p.leaveCd();
        final String unit = p.useUnitType();

        // 1) 타입 메타 (submitLeave 미러)
        LeaveTypeInfoVO type = leaveFlowMapper.selectLeaveTypeInfo(cmpny, leaveCd);
        if (type == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }
        boolean statutory = "Y".equals(type.systemYn());
        boolean hourlyUnit = isHourlyUnit(unit);

        // 2) 단위 구조검증 + 예상 차감 산출 (submitLeave 단위 분기 미러 — INSERT 없음, 조회 전용)
        BigDecimal charge;
        boolean floorApplied = false;
        boolean capApplied = false;
        Integer convFromCharge = null; // 시간차는 calcHourlyCharge 가 이미 분모를 조회하므로 재사용
        BigDecimal floorDays = null; // 발동 마일스톤 요금(0.25/0.5/1.0) — FE 하한 안내 단위 분기용(미발동 null)
        Integer previewMinutes = null; // 시간차 신청 분(짜투리 발동 판정 입력 — PC-05)

        if (UNIT_FULL.equals(unit)) {
            charge = new BigDecimal("1.00000");
        } else if (UNIT_HALF.equals(unit)) {
            // 반차는 스케줄 필수(submitLeave 동일 거부).
            if (leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd) == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_110);
            }
            charge = new BigDecimal("0.50000");
        } else if (UNIT_QUARTER.equals(unit)) {
            // LC-10 허용 게이트 미러: 법정=USAGE_UNIT='QUARTER_DAY' / 비법정=타입 '05' 설정.
            boolean quarterAllowed = statutory
                    ? USAGE_UNIT_QUARTER_DAY.equals(leaveFlowMapper.selectPolicyUsageUnit(cmpny))
                    : UNIT_QUARTER.equals(type.useUnitType());
            if (!quarterAllowed) {
                throw new ApiException(AttdErrorCode.ATTD_400_054);
            }
            if (leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd) == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_110);
            }
            charge = new BigDecimal("0.25000");
        } else if (hourlyUnit) {
            Integer sMin = DateTimeUtils.hhmmToMinutes(p.startTime());
            Integer eMin = DateTimeUtils.hhmmToMinutes(p.endTime());
            if (sMin == null || eMin == null || eMin <= sMin) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            int minutes = eMin - sMin;
            if (minutes % unitMinutes(unit) != 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_054);
            }
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd);
            if (daily == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_110);
            }
            if (minutes > daily) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            if (leaveDeductionService.crossesBreak(cmpny, site, user, workYmd, sMin, eMin)) {
                throw new ApiException(AttdErrorCode.ATTD_400_055);
            }
            // advisory lock 없이 산출(조회 전용 추정치). 제출 시 lock 하에 재계산되며 코어 산식이
            //   단일 출처(HourlyLeaveChargeUtils)라 동시 신청이 없는 한 preview == 확정값.
            HourlyChargeVO hc = leaveDeductionService.calcHourlyCharge(cmpny, site, user, workYmd, minutes);
            if (hc == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            charge = hc.chargeDays();
            floorApplied = hc.floorApplied();
            capApplied = hc.capApplied();
            convFromCharge = hc.convMinutes();
            floorDays = hc.floorDays();
            previewMinutes = minutes;
        } else {
            throw new ApiException(AttdErrorCode.ATTD_400_054);
        }

        // 3) 사후 신청 마감 가드 (submitLeave 미러 — 신청 시 거부될 값 사전 차단)
        if (workYmd.compareTo(todayYmd()) < 0) {
            String closeYm = workYmd.substring(0, 6);
            if (attdCloseService.isClosedForUser(cmpny, site, user, closeYm)) {
                throw new ApiException(AttdErrorCode.ATTD_400_050);
            }
        }

        // 4) 같은 날 중복 등록 가드 (submitLeave 3-B 미러)
        BigDecimal occupied = leaveFlowMapper.selectOccupiedLeaveDaysOnDate(cmpny, user, workYmd);
        if (occupied == null) {
            occupied = BigDecimal.ZERO;
        }
        if (occupied.add(charge).compareTo(BigDecimal.ONE) > 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_111);
        }
        if (hourlyUnit
                && leaveFlowMapper.countOverlappingTimeLeaveOnDate(
                        cmpny, user, workYmd, p.startTime(), p.endTime()) > 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_112);
        }

        // 5) 잔여 부족 판정 — 에러가 아니라 플래그(FE 사전 경고, §3-3 잔여 부족 반려 해소).
        //    가불(isBorrow)은 preview 비대상(가불은 종일/반차 전용 + 결재 강제 — 잔여 기준만 판정).
        boolean insufficient = false;
        boolean grantBased = false; // 부여 기반 신청 여부(짜투리 발동 preview 대상 — PC-05)
        if (charge.signum() > 0) {
            boolean userApplyType = LEAVE_TYPE_USER_APPLY.equals(type.leaveType()) && !statutory;
            if (userApplyType) {
                Integer maxAplyDays = type.maxAplyDays();
                if (maxAplyDays == null) {
                    insufficient = true; // 한도 미설정 = 신청불가(fail-closed, submitLeave 동일 기준)
                } else {
                    BigDecimal used;
                    if (AVAIL_TERM_NONE.equals(type.availTermType())) {
                        used = leaveFlowMapper.selectTotalUsedDays(cmpny, user, leaveCd);
                    } else {
                        FiscalYearUtils.FiscalWindow fiscal = resolveFiscalWindow(cmpny);
                        used = leaveFlowMapper.selectFiscalUsedDays(
                                cmpny, user, leaveCd, fiscal.fiscalStartYmd(), fiscal.fiscalEndYmdExclusive());
                    }
                    if (used == null) {
                        used = BigDecimal.ZERO;
                    }
                    insufficient = used.add(charge).compareTo(BigDecimal.valueOf(maxAplyDays)) > 0;
                }
            } else {
                // PC-02(D8): 부여 기반 — 합산 잔여(만료 임박순 전 부여) 기준으로 교체(분할 차감과 동일 판정).
                //   (FOR UPDATE SQL 재사용 — 비트랜잭션 preview 라 autocommit 으로 행 잠금이 즉시
                //   해제되어 실질 잠금 부작용 없음 — 기존 단건 판정과 동일 관례.)
                grantBased = true;
                insufficient = sumDeductibleRemaining(cmpny, user, leaveCd, workYmd).compareTo(charge) < 0;
            }
        }

        // PC-03(N7·N8): convMinutes = 대상일 기준 본인 분모. 시간차는 calcHourlyCharge 가 이미
        //   조회(산출 불가면 ATTD_400_193 전파), 고정단위(종일/반차/반반차)는 표기 전용이라
        //   산출 불가 시 480 폴백(FE formatLeaveDays 폴백과 정합).
        Integer convPersonal = (convFromCharge != null)
                ? convFromCharge
                : leaveConversionPolicyService.resolvePersonalConvMinutes(cmpny, user, workYmd);
        int conv = (convPersonal != null) ? convPersonal : LeaveConversionPolicyService.DEFAULT_CONV_MINUTES;

        // PC-05(D6) preview: 부여 기반 신청이 잔여 부족이면 짜투리 발동 여부를 판정해 안내(FE UI-C/D).
        //   발동 예상이면 신청은 성공하므로 insufficient 를 내리고 발동 필드를 싣는다.
        //   lock 없는 추정치 — 제출 시 remnant lock 하에 재판정(시간차 preview 관례 미러).
        boolean remnantTriggered = false;
        BigDecimal remnantDays = null;
        Integer companyCoverMinutes = null;
        if (insufficient && grantBased) {
            RemnantTriggerPlanVO plan = leaveRemnantCoverService.evaluateTrigger(
                    cmpny, user, workYmd, leaveCd, unit, previewMinutes, charge, convPersonal);
            if (plan != null) {
                remnantTriggered = true;
                remnantDays = plan.remnantDays();
                companyCoverMinutes = plan.coverMinutes();
                insufficient = false;
            }
        }

        log.debug("[leaveflow] 예상 차감 preview: userCd={}, workYmd={}, unit={}, charge={}, 하한={}, 캡={}, "
                        + "잔여부족={}, conv={}, 짜투리발동={}",
                user, workYmd, unit, charge.toPlainString(), floorApplied, capApplied, insufficient, conv,
                remnantTriggered);

        return LeaveDeductionPreviewResponse.builder()
                .chargeDays(charge)
                .floorApplied(floorApplied)
                .capApplied(capApplied)
                .insufficientBalance(insufficient)
                .convMinutes(conv)
                .floorDays(floorDays)
                .remnantTriggered(remnantTriggered)
                .remnantDays(remnantDays)
                .companyCoverMinutes(companyCoverMinutes)
                .build();
    }

    /**
     * 연차개편: 당해 회계연도 윈도우 산출(단일출처 {@link FiscalYearUtils}).
     * 활성정책 AXIS2_FISCAL_START_MM/_DD 로 산출하며, 정책 미존재/NULL 이면 1월 1일 폴백.
     */
    private FiscalYearUtils.FiscalWindow resolveFiscalWindow(String cmpnyCd) {
        LeavePolicyVO policy = leavePolicyMapper.selectActivePolicy(cmpnyCd);
        String mm = (policy == null) ? null : policy.getAxis2FiscalStartMm();
        String dd = (policy == null) ? null : policy.getAxis2FiscalStartDd();
        return FiscalYearUtils.fiscalWindow(LocalDate.now(), mm, dd);
    }

    /** 연차개편 동시성: '01' 신청 직렬화 키(회사+사용자+연차코드). */
    private String leave01LockKey(String cmpnyCd, String userCd, String leaveCd) {
        return "leave01:" + cmpnyCd + ":" + userCd + ":" + leaveCd;
    }

    /** advisory lock 획득. 타임아웃/오류면 동시 처리로 보고 ATTD_400_051 로 변환(중복신청 차단). */
    private void acquireLeave01Lock(String lockKey) {
        Integer got = leaveFlowMapper.getAdvisoryLock(lockKey, LEAVE01_LOCK_TIMEOUT_SEC);
        if (got == null || got != 1) {
            log.info("[leaveflow] '01' 신청 advisory lock 미획득 — lockKey={}, got={}", lockKey, got);
            throw new ApiException(AttdErrorCode.ATTD_400_051);
        }
    }

    /** advisory lock 해제(예외 무시 — 세션 종료 시 자동 해제됨). */
    private void releaseLeave01Lock(String lockKey) {
        try {
            leaveFlowMapper.releaseAdvisoryLock(lockKey);
        } catch (Exception e) {
            log.warn("[leaveflow] '01' 신청 advisory lock 해제 실패(무시) — lockKey={}", lockKey, e);
        }
    }

    /**
     * LC-04(F5): 시간차 사용자·일 단위 직렬화 lock 획득(앱 미러). 타임아웃/오류면 같은 날 동시
     * 처리로 보고 중복신청 계열(ATTD_400_111)로 변환 — leave01 lock 실패→400_051 변환 관례 미러.
     */
    private void acquireLeaveDayLock(String lockKey) {
        Integer got = leaveFlowMapper.getAdvisoryLock(lockKey, LEAVE01_LOCK_TIMEOUT_SEC);
        if (got == null || got != 1) {
            log.info("[leaveflow] 시간차 leaveDay advisory lock 미획득 — lockKey={}, got={}", lockKey, got);
            throw new ApiException(AttdErrorCode.ATTD_400_111);
        }
    }

    /** LC-04: 시간차 leaveDay advisory lock 해제(예외 무시 — 세션 종료 시 자동 해제됨). */
    private void releaseLeaveDayLock(String lockKey) {
        try {
            leaveFlowMapper.releaseAdvisoryLock(lockKey);
        } catch (Exception e) {
            log.warn("[leaveflow] 시간차 leaveDay advisory lock 해제 실패(무시) — lockKey={}", lockKey, e);
        }
    }

    /**
     * PC-05(N9): 짜투리 발동 판정~기록 직렬화 lock 획득(앱 미러). 타임아웃/오류면 동시 신청으로
     * 보고 잔여 부족 계열(ATTD_400_051)로 변환 — leave01 lock 실패 변환 관례 미러.
     */
    private void acquireRemnantLock(String lockKey) {
        Integer got = leaveFlowMapper.getAdvisoryLock(lockKey, LEAVE01_LOCK_TIMEOUT_SEC);
        if (got == null || got != 1) {
            log.info("[leaveflow] 짜투리 leaveRemnant advisory lock 미획득 — lockKey={}, got={}", lockKey, got);
            throw new ApiException(AttdErrorCode.ATTD_400_051);
        }
    }

    /** PC-05: 짜투리 leaveRemnant advisory lock 해제(예외 무시 — 세션 종료 시 자동 해제됨). */
    private void releaseRemnantLock(String lockKey) {
        try {
            leaveFlowMapper.releaseAdvisoryLock(lockKey);
        } catch (Exception e) {
            log.warn("[leaveflow] 짜투리 leaveRemnant advisory lock 해제 실패(무시) — lockKey={}", lockKey, e);
        }
    }

    /** 시간차(02/03/04) 단위 여부 — LC-05 재정산 훅 대상 판정. */
    private boolean isHourlyUnit(String unit) {
        return UNIT_HOUR2.equals(unit) || UNIT_HOUR1.equals(unit) || UNIT_MIN30.equals(unit);
    }

    /** PRAFTA-028 - 마감된 기간(신청자 부서)의 연차 결재(승인/반려)를 차단한다. */
    private void ensureLeaveNotClosed(String cmpnyCd, LeaveReqRowVO req) {
        String ym = (req.workYmd() != null && req.workYmd().length() >= 6)
                ? req.workYmd().substring(0, 6) : req.workYmd();
        if (attdCloseService.isClosedForUser(cmpnyCd, req.siteCd(), req.userCd(), ym)) {
            throw new ApiException(AttdErrorCode.ATTD_400_042);
        }
    }

    @Override
    @Transactional
    public void approveStep(LeaveApprovalActionParam p) {
        LeaveReqRowVO req = loadProcessableReq(p.gvCmpnyCd(), p.reqId());
        ApprovalStepVO step = loadCurrentStep(p, req);

        // PRAFTA-028 - 마감된 기간(신청자 부서)의 연차 결재 승인 차단
        ensureLeaveNotClosed(p.gvCmpnyCd(), req);

        approvalLineMapper.updateStepStatus(p.gvCmpnyCd(), p.reqId(), p.approvalStep(), STEP_APPROVED, p.comment(), p.gvUserCd());

        // 다음 대기('00') 단계로 진행(자기승인 자동승인 '02' 단계는 자동 skip). 없으면 최종 승인.
        Integer nextStep = approvalLineMapper.selectFirstWaitingStep(p.gvCmpnyCd(), p.reqId());
        if (nextStep != null) {
            approvalLineMapper.updateStepStatus(p.gvCmpnyCd(), p.reqId(), nextStep, STEP_APPLIED, null, p.gvUserCd());
            // PRAFTA-COM-004 시나리오 A hook: 다음 단계로 차례가 도래한 결재자 1인에게 PUSH 적재(예외 격리).
            //  단계 번호↔결재자 매핑은 selectApprovalStep 으로 정확히 조회(인덱스 위치밀림 방지).
            try {
                ApprovalStepVO nextStepVo =
                        approvalLineMapper.selectApprovalStep(p.gvCmpnyCd(), p.reqId(), nextStep);
                if (nextStepVo != null) {
                    leaveApprovalNotiService.notifyApprovalTurn(p.gvCmpnyCd(), req.siteCd(), req.userCd(),
                            p.reqId(), nextStep, nextStepVo.getApproverUserCd(), p.gvUserCd());
                }
            } catch (Exception e) {
                log.error("연차 승인 다음단계 PUSH 적재 hook 실패(승인 영향 없음). reqId={}, step={}",
                        p.reqId(), nextStep, e);
            }
        } else {
            leaveFlowMapper.updateReqStatus(p.gvCmpnyCd(), p.reqId(), REQ_APPROVED, p.gvUserCd(), p.comment());
            // PRAFTA-025: 06(연차수정)은 최종 승인 시 기존 사용기록(TARGET_ID)을 새 값으로 in-place 갱신,
            //   05(연차사용)는 기존대로 일 단위 출근 차단을 적용한다.
            if (REQ_TYPE_LEAVE_MODIFY.equals(req.reqType())) {
                applyLeaveModify(p.gvCmpnyCd(), req, p.gvUserCd());
                // PC-06(D7): 수정 승인으로 차감이 줄어 잔여가 복원될 수 있다 — 미도래 짜투리 보전 건 회수.
                leaveRemnantCoverService.reclaimIfPossible(p.gvCmpnyCd(), req.userCd(), p.gvUserCd());
                log.info("연차 수정 최종 승인. reqId={}, leaveId={}", p.reqId(), req.targetId());
            } else {
                applyFullDayAttendanceBlock(p.gvCmpnyCd(), p.reqId(), p.gvUserCd());
                // LC-05(F2): 시간차 최종 승인 시점 재판정 — 결재 대기 중 같은 날 다른 시간차가
                //   취소·반려로 빠졌으면 하한 차액 배치가 어긋나므로, 그날 잔존 시간차를 시간순
                //   재적용해 확정값을 보정한다(코어 산식 LC-03 공유 — 신청 계산과 항상 동치).
                LeaveUseDetailVO aprvDetail = leaveFlowMapper.selectLeaveUseDetailByReqId(p.gvCmpnyCd(), p.reqId());
                if (aprvDetail != null && isHourlyUnit(aprvDetail.useUnitType())) {
                    leaveHourlyResettleService.resettleHourlyLeaveOnDate(p.gvCmpnyCd(),
                            aprvDetail.siteCd(), aprvDetail.userCd(), aprvDetail.startDate(), p.gvUserCd());
                }
                log.info("연차 최종 승인. reqId={}, approver={}", p.reqId(), p.gvUserCd());
            }
            // PRAFTA-APP-021-3a(W2): 최종 승인 시점에 신청자 본인에게 결과 통보 PUSH 적재(afterCommit 격리).
            //   중간 단계 승인(nextStep != null)은 통보 대상 아님(최종 확정에서만 1회).
            try {
                approvalResultNotiService.notifyLeaveResult(
                        p.gvCmpnyCd(), req.siteCd(), req.userCd(), p.reqId(), true, p.gvUserCd());
            } catch (Exception e) {
                log.error("연차 최종 승인 결과 통보 PUSH 적재 hook 실패(승인 영향 없음). reqId={}", p.reqId(), e);
            }
        }
    }

    @Override
    @Transactional
    public void rejectStep(LeaveApprovalActionParam p) {
        LeaveReqRowVO req = loadProcessableReq(p.gvCmpnyCd(), p.reqId());
        loadCurrentStep(p, req);

        // PRAFTA-028 - 마감된 기간(신청자 부서)의 연차 결재 반려 차단
        ensureLeaveNotClosed(p.gvCmpnyCd(), req);

        // 반려 사유 필수(이력 보존 §9.5). 프론트 최소길이와 별개로 서버에서도 비어있음을 차단.
        if (p.comment() == null || p.comment().trim().isEmpty()) {
            throw new ApiException(AttdErrorCode.ATTD_400_057);
        }

        approvalLineMapper.updateStepStatus(p.gvCmpnyCd(), p.reqId(), p.approvalStep(), STEP_REJECTED, p.comment(), p.gvUserCd());
        leaveFlowMapper.updateReqStatus(p.gvCmpnyCd(), p.reqId(), REQ_REJECTED, p.gvUserCd(), p.comment());

        // PRAFTA-025: 06(연차수정) 반려는 기존 사용기록을 절대 건드리지 않는다.
        //   수정은 '최종 승인 시에만' 반영되므로, 반려 시점엔 되돌릴 변경이 없다.
        //   (05 반려 로직의 차감 해제/부여 동기화/출근차단 해제를 06에 적용하면 기존 휴가가 잘못 취소됨)
        if (REQ_TYPE_LEAVE_MODIFY.equals(req.reqType())) {
            log.info("연차 수정 반려(기존 휴가 유지). reqId={}, approver={}", p.reqId(), p.gvUserCd());
            // PRAFTA-APP-021-3a(W2): 연차 수정 반려 결과를 신청자 본인에게 통보(afterCommit 격리).
            try {
                approvalResultNotiService.notifyLeaveResult(
                        p.gvCmpnyCd(), req.siteCd(), req.userCd(), p.reqId(), false, p.gvUserCd());
            } catch (Exception e) {
                log.error("연차 수정 반려 결과 통보 PUSH 적재 hook 실패(반려 영향 없음). reqId={}", p.reqId(), e);
            }
            return;
        }

        // 차감 해제 + 부여 동기화 + 가불 회수 + 시간차 재정산(F1).
        //   QT-11-7: 동일 시퀀스를 소속이동 발효 등 "결재 흐름 밖 강제 종료" 경로도 재사용해야 하므로
        //   restoreLeaveLedger 로 추출했다(단일 출처). 여기서 변경 시 그 경로에도 함께 반영된다.
        restoreLeaveLedger(p.gvCmpnyCd(), p.reqId(), "결재 반려", p.gvUserCd());

        log.info("연차 반려. reqId={}, approver={}", p.reqId(), p.gvUserCd());

        // PRAFTA-APP-021-3a(W2): 연차 사용 반려 결과를 신청자 본인에게 통보(afterCommit 격리).
        try {
            approvalResultNotiService.notifyLeaveResult(
                    p.gvCmpnyCd(), req.siteCd(), req.userCd(), p.reqId(), false, p.gvUserCd());
        } catch (Exception e) {
            log.error("연차 반려 결과 통보 PUSH 적재 hook 실패(반려 영향 없음). reqId={}", p.reqId(), e);
        }
    }

    /**
     * QT-11-7 — 결재 흐름 밖 강제 종료(소속이동 발효 등) 시 연차 원장 원복 진입점.
     *
     * <p>호출자가 이미 TB_USER_ATTD_REQ 상태를 종료로 UPDATE 한 뒤 호출한다(상태는 여기서 건드리지 않는다).
     *   REQ_TYPE 을 서버에서 재확인해 연차 사용('05')만 원복하며, 연차 수정('06')·근태 요청 등은 no-op 이다.
     *   호출자 트랜잭션에 참여한다(REQUIRED) — 원복 실패 시 발효 전체가 롤백되어야 한다(부분 처리 금지).
     */
    @Override
    @Transactional
    public void restoreLeaveLedgerOnTerminate(String cmpnyCd, String reqId, String reason, String actor) {
        if (cmpnyCd == null || cmpnyCd.isBlank() || reqId == null || reqId.isBlank()) {
            return;
        }
        LeaveReqRowVO req = leaveFlowMapper.selectLeaveReq(cmpnyCd, reqId);
        if (req == null) {
            return; // 대상 없음 → 멱등 no-op
        }
        if (!REQ_TYPE_LEAVE_USE.equals(req.reqType())) {
            // 연차 수정('06')은 승인 시에만 반영되므로 되돌릴 차감이 없다(rejectStep 과 동일 규칙).
            // 근태/OT 요청은 애초에 연차 원장과 무관.
            return;
        }
        restoreLeaveLedger(cmpnyCd, reqId, reason, actor);
        log.info("연차 원장 원복(결재 흐름 밖 종료). reqId={}, 사유={}, actor={}", reqId, reason, actor);
    }

    /**
     * 연차 사용 요청('05')의 차감을 원복하는 단일 시퀀스 —
     * ① use 행 취소(CANCELLED) ② 관련 GRANT USED_DAYS 재집계 ③ 가불 GRANT 회수 ④ 시간차 재정산(F1).
     *
     * <p>{@link #rejectStep} 과 {@link #restoreLeaveLedgerOnTerminate} 가 공유한다(단일 출처).
     *   호출 전 대상이 REQ_TYPE='05' 임이 보장되어야 한다.
     */
    private void restoreLeaveLedger(String cmpnyCd, String reqId, String reason, String actor) {
        // 출근 차단 해제용 detail 은 취소(LEAVE_STATUS 변경) 전에 확보.
        LeaveUseDetailVO detail = leaveFlowMapper.selectLeaveUseDetailByReqId(cmpnyCd, reqId);

        // prafta-com-011-2: 차감 해제 + 부여 동기화 (분할 차감 대응 — 잔여+가불 여러 GRANT 를 모두 재계산).
        List<String> grantIds = leaveFlowMapper.selectGrantIdsByReqId(cmpnyCd, reqId);
        leaveFlowMapper.cancelLeaveUseByReqId(cmpnyCd, reqId, reason, actor);
        for (String grantId : grantIds) {
            if (grantId != null && !grantId.isEmpty()) {
                leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, grantId, actor);
            }
        }
        // prafta-com-011-2 가불 회수: 차감 해제 후 이 reqId 가 만든 가불 GRANT 를 회수(CANCELED).
        //   유령 미래 부여 방지(결정 §6). 가불 reqId 한정(비가불은 0건이라 안전). USED_DAYS>0 가불은 회수 안 함.
        try {
            int recalled = leaveGrantEngineService.cancelBorrowGrantByReqId(cmpnyCd, reqId, actor);
            if (recalled > 0) {
                log.info("연차 원복 — 가불 GRANT 회수 {}건. reqId={}", recalled, reqId);
            }
        } catch (Exception e) {
            // 가불 회수 실패는 트랜잭션 롤백 사유(차감 해제와 일관성 유지).
            log.error("연차 원복 가불 GRANT 회수 실패. reqId={}", reqId, e);
            throw e;
        }
        // prafta-com-008-E-2: 출근 차단은 leave_use 기준 → cancelLeaveUseByReqId 로 자동 해제된다.

        // LC-05(F1): 그날 시간차 구성이 바뀌면 잔존 시간차 건을 시간순 재적용(하한 차액 재배치).
        if (detail != null && isHourlyUnit(detail.useUnitType())) {
            leaveHourlyResettleService.resettleHourlyLeaveOnDate(cmpnyCd,
                    detail.siteCd(), detail.userCd(), detail.startDate(), actor);
        }

        // PC-06(D7): 잔여 복원 후 미도래(WORK_YMD > 오늘) 짜투리 보전 건을 정상 차감으로 전환(부분 회수 허용).
        //   반려·강제종료(앱 위임 포함) 전 경로가 본 메서드로 수렴하므로 이 한 지점으로 커버(plan §0-4).
        //   detail null 이면 되돌린 CONFIRMED 행이 없던 요청 — 복원분 없음, 회수 생략.
        if (detail != null) {
            leaveRemnantCoverService.reclaimIfPossible(cmpnyCd, detail.userCd(), actor);
        }
    }

    @Override
    public java.util.List<com.prafta.web.attd.leaveflow.vo.MyLeaveApprovalVO> getMyPendingLeaveApprovals(
            String cmpnyCd, String approverUserCd) {
        return leaveFlowMapper.selectMyPendingLeaveApprovals(cmpnyCd, approverUserCd);
    }

    @Override
    @Transactional
    public DirectLeaveResult recordDirectLeaveUsage(String cmpnyCd, String siteCd, String userCd,
                                                    String workYmd, String leaveCd, String operatorUserCd) {
        // 연차개편: 근무계획 직접입력은 시스템 연차(SYS_ANNUAL) 등 부여 기반 차감만 허용한다(메모리 정책).
        //   사용자 신청('01') 타입은 GRANT 가 없어 회계연도 한도 모델이므로, 직접입력 경로로 차감하면 잔여 모델이
        //   어긋난다. 기존엔 selectDeductibleGrant 가 null → INSUFFICIENT 로 자연 거부되었으나(데이터 오염 없음),
        //   의도를 명시적으로 빠르게 거부하고 로그를 남긴다(현 동작과 결과 동일, 진입부 fail-fast).
        LeaveTypeInfoVO type = leaveFlowMapper.selectLeaveTypeInfo(cmpnyCd, leaveCd);
        if (type != null && LEAVE_TYPE_USER_APPLY.equals(type.leaveType()) && !"Y".equals(type.systemYn())) {
            log.info("[leaveflow] 근무계획 직접입력 거부: 사용자 신청('01') 타입은 직접 차감 불가 (userCd={}, workYmd={}, leaveCd={})",
                    userCd, workYmd, leaveCd);
            return DirectLeaveResult.INSUFFICIENT;
        }

        // 멱등: 동일 직원·일자·연차코드 직접 사용기록이 있으면 중복 차감 방지
        if (leaveFlowMapper.countDirectLeaveUse(cmpnyCd, userCd, workYmd, leaveCd) > 0) {
            return DirectLeaveResult.SKIPPED_DUP;
        }
        // 차감 대상 부여 1일 (동일 LEAVE_CD, 만료 임박순, FOR UPDATE). 없으면 잔여 부족.
        BigDecimal oneDay = BigDecimal.ONE;
        DeductibleGrantVO grant = leaveFlowMapper.selectDeductibleGrant(cmpnyCd, userCd, leaveCd, workYmd, oneDay);
        if (grant == null) {
            return DirectLeaveResult.INSUFFICIENT;
        }
        // 사용 기록(CONFIRMED, REQ_ID 없음 = 결재 없는 직접 차감) + 부여 USED_DAYS 동기화
        String leaveId = leaveFlowMapper.selectNextLeaveId(cmpnyCd);
        LeaveUseVO use = LeaveUseVO.builder()
                .leaveId(leaveId).cmpnyCd(cmpnyCd).siteCd(siteCd).userCd(userCd).leaveCd(leaveCd)
                .reqId(null).grantId(grant.grantId())
                .startDate(workYmd).startTime(null).endDate(workYmd).endTime(null)
                .useUnitType(UNIT_FULL).leaveDays(oneDay).leaveMinutes(null)
                .leaveReason("근무계획 연차 적용").leaveStatus(USE_CONFIRMED).insertNo(operatorUserCd)
                .build();
        try {
            leaveFlowMapper.insertLeaveUse(use);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 동시 저장 경합(TOCTOU): UK_LEAVE_USE_DIRECT(회사+직원+일자+연차코드) 유니크가 최종 차단.
            // 이미 기록된 것으로 보고 건너뜀(이중 차감 방지).
            log.info("근무계획 직접 연차 사용 - 멱등키 경합으로 건너뜀. cmpnyCd={}, userCd={}, workYmd={}, leaveCd={}",
                    cmpnyCd, userCd, workYmd, leaveCd);
            return DirectLeaveResult.SKIPPED_DUP;
        }
        leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, grant.grantId(), operatorUserCd);

        log.info("근무계획 직접 연차 사용 기록. cmpnyCd={}, userCd={}, workYmd={}, leaveCd={}, grantId={}",
                cmpnyCd, userCd, workYmd, leaveCd, grant.grantId());
        return DirectLeaveResult.RECORDED;
    }

    @Override
    @Transactional
    public DirectLeaveResult recordDirectLeaveUsageAuto(String cmpnyCd, String siteCd, String userCd,
                                                        String workYmd, java.util.List<String> candidateLeaveCds,
                                                        String operatorUserCd) {
        // prafta-com-016-C-4: 종류 미지정 자동 차감 — 후보(연차/월차) 중 소멸 임박 통합순으로 1일 차감.
        if (candidateLeaveCds == null || candidateLeaveCds.isEmpty()) {
            return DirectLeaveResult.INSUFFICIENT;
        }
        // 멱등: 해당 셀에 이미 종일 CONFIRMED 연차(직접/승인 무관)가 있으면 중복 차감 방지.
        //   재저장 시 동일 셀이 다시 적용돼도, 또 다른 종류로 이중 차감되지 않도록 종류 무관으로 막는다.
        if (leaveFlowMapper.countAnyFullDayLeaveOnCell(cmpnyCd, userCd, workYmd) > 0) {
            return DirectLeaveResult.SKIPPED_DUP;
        }
        // 차감 대상 부여 1일 (후보 전체, 만료 임박 통합순, FOR UPDATE). 없으면 잔여 부족.
        BigDecimal oneDay = BigDecimal.ONE;
        AutoDeductibleGrantVO grant = leaveFlowMapper.selectAutoDeductibleGrant(
                cmpnyCd, userCd, candidateLeaveCds, workYmd, oneDay);
        if (grant == null) {
            return DirectLeaveResult.INSUFFICIENT;
        }
        String leaveCd = grant.leaveCd();
        // 사용 기록(CONFIRMED, REQ_ID 없음 = 결재 없는 직접 차감) + 부여 USED_DAYS 동기화
        String leaveId = leaveFlowMapper.selectNextLeaveId(cmpnyCd);
        LeaveUseVO use = LeaveUseVO.builder()
                .leaveId(leaveId).cmpnyCd(cmpnyCd).siteCd(siteCd).userCd(userCd).leaveCd(leaveCd)
                .reqId(null).grantId(grant.grantId())
                .startDate(workYmd).startTime(null).endDate(workYmd).endTime(null)
                .useUnitType(UNIT_FULL).leaveDays(oneDay).leaveMinutes(null)
                .leaveReason("근무계획 연차 적용(소멸임박순 자동)").leaveStatus(USE_CONFIRMED).insertNo(operatorUserCd)
                .build();
        try {
            leaveFlowMapper.insertLeaveUse(use);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 동시 저장 경합(TOCTOU): UK_LEAVE_USE_DIRECT(회사+직원+일자+연차코드) 유니크가 최종 차단.
            log.info("근무계획 자동 연차 사용 - 멱등키 경합으로 건너뜀. cmpnyCd={}, userCd={}, workYmd={}, leaveCd={}",
                    cmpnyCd, userCd, workYmd, leaveCd);
            return DirectLeaveResult.SKIPPED_DUP;
        }
        leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, grant.grantId(), operatorUserCd);

        log.info("근무계획 자동 연차 사용 기록(소멸임박순). cmpnyCd={}, userCd={}, workYmd={}, leaveCd={}, grantId={}",
                cmpnyCd, userCd, workYmd, leaveCd, grant.grantId());
        return DirectLeaveResult.RECORDED;
    }

    @Override
    @Transactional
    public int cancelDirectLeaveUsage(String cmpnyCd, String userCd, String workYmd,
                                      String leaveCd, String operatorUserCd) {
        // 1) 취소 전, 영향받을 부여(GRANT_ID) 목록을 선조회한다.
        //    취소(CANCELLED) 후에는 직접 사용기록 매칭이 사라져 재계산 대상을 찾을 수 없으므로 미리 확보.
        List<String> grantIds = leaveFlowMapper.selectDirectLeaveGrantIdsByCell(cmpnyCd, userCd, workYmd, leaveCd);

        // 2) 직접 사용기록 soft cancel (이력 보존 §8.5.8). 대상 0건이면 멱등 no-op.
        int cancelled = leaveFlowMapper.cancelDirectLeaveUseByCell(
                cmpnyCd, userCd, workYmd, leaveCd, CANCEL_REASON_PLAN_CLEAR, operatorUserCd);
        if (cancelled == 0) {
            return 0;
        }

        // 3) 영향받은 부여의 USED_DAYS 를 잔존 CONFIRMED 합계로 재계산하여 잔여를 복원한다.
        for (String grantId : grantIds) {
            if (grantId != null && !grantId.isEmpty()) {
                leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, grantId, operatorUserCd);
            }
        }

        log.info("근무계획 직접 연차 사용 취소(차감 복원). cmpnyCd={}, userCd={}, workYmd={}, leaveCd={}, 취소 {}건, 부여재계산 {}건",
                cmpnyCd, userCd, workYmd, leaveCd, cancelled, grantIds.size());
        return cancelled;
    }

    /** 요청 소유권(회사) + 처리 가능 상태('01' 신청) 검증. (D 인계: 회사 소유권 상위 검증) */
    private LeaveReqRowVO loadProcessableReq(String cmpnyCd, String reqId) {
        LeaveReqRowVO req = leaveFlowMapper.selectLeaveReq(cmpnyCd, reqId);
        if (req == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }
        if (!REQ_APPLIED.equals(req.reqStatus())) {
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }
        return req;
    }

    /** 현재 단계 로드 + 결재자 본인·단계 순서('01' 신청 상태) 검증. (D 인계: 본인·순서 검증) */
    private ApprovalStepVO loadCurrentStep(LeaveApprovalActionParam p, LeaveReqRowVO req) {
        ApprovalStepVO step = approvalLineMapper.selectApprovalStep(p.gvCmpnyCd(), p.reqId(), p.approvalStep());
        if (step == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }
        if (!p.gvUserCd().equals(step.getApproverUserCd())) {
            log.warn("연차 결재 권한 없음 - reqId={}, step={}, actor={}, approver={}",
                    p.reqId(), p.approvalStep(), p.gvUserCd(), step.getApproverUserCd());
            throw new ApiException(AttdErrorCode.ATTD_403_030);
        }
        if (!STEP_APPLIED.equals(step.getApprovalStatus())) {
            // 아직 차례가 아니거나(대기 '00') 이미 처리된 단계
            throw new ApiException(AttdErrorCode.ATTD_400_053);
        }
        return step;
    }

    /**
     * 일 단위(00) 휴가 확정 시 출근 차단.
     * prafta-com-008-E-2: 연차-스케줄 모델 전환 — 출근 차단은 leave_use(CONFIRMED 종일) 존재로 판정한다.
     * 연차 확정 시점에 leave_use 가 이미 CONFIRMED 이므로 추가 work_plan 덮어쓰기 없이도 차단된다(no-op).
     * (work_plan 은 SCH_CD 유지 → 연차 취소/반려 시 자동 근무일 복귀.)
     */
    private void applyFullDayAttendanceBlock(String cmpnyCd, String reqId, String actorUserCd) {
        // 의도적 no-op (모델 전환). 차단 근거 = leave_use. 호출부 안정성을 위해 메서드는 유지한다.
    }

    /**
     * PRAFTA-025 - 연차 수정('06') 최종 승인 반영.
     *
     * <p>기존 사용기록(TARGET_ID=LEAVE_ID)을 요청이 담은 새 값(일자/시각/일수)으로 in-place
     * 갱신하고, 부여 USED_DAYS 를 재동기화하며, 일 단위 출근 차단 블록을 새 일자로 이동한다.
     *
     * <p>범위: 연차코드(LEAVE_CD)/차감 부여(GRANT_ID)/사용단위(USE_UNIT_TYPE)는 보존한다.
     * 일 단위↔반차/시간차 전환이나 연차 종류 변경은 요청 테이블이 사용단위/분을 담지 않아
     * 별도 신청(취소 후 재신청)으로 처리한다(스키마 확장 시 후속).
     */
    private void applyLeaveModify(String cmpnyCd, LeaveReqRowVO req, String actorUserCd) {
        String leaveId = req.targetId();
        if (leaveId == null || leaveId.isEmpty()) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }
        LeaveModifyTargetVO tgt = leaveFlowMapper.selectLeaveModifyTarget(cmpnyCd, leaveId);
        if (tgt == null) {
            // 대상 사용기록이 없거나 이미 취소됨 → 수정 대상 부재
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }

        // 새 차감 일수: 요청값 우선, 없으면 기존 유지.
        BigDecimal newDays = (req.leaveDays() != null) ? req.leaveDays() : tgt.leaveDays();

        // 잔여 가드: '본 건의 기존 차감을 되돌린 가용분' 안에서만 증액 허용.
        //   availableExclSelf = GRANT_DAYS - USED_DAYS + (본 건 기존 차감)
        if (tgt.grantDays() != null && tgt.usedDays() != null && tgt.leaveDays() != null) {
            BigDecimal availableExclSelf = tgt.grantDays()
                    .subtract(tgt.usedDays())
                    .add(tgt.leaveDays());
            if (newDays.compareTo(availableExclSelf) > 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_051);
            }
        }

        // 기존 사용기록 in-place 갱신(연차코드/부여/사용단위 보존). 일 단위 기준이라 minutes는 null.
        int updated = leaveFlowMapper.updateLeaveUseModify(
                cmpnyCd, leaveId,
                req.startDate(), req.startTime(), req.endDate(), req.endTime(),
                newDays, null, actorUserCd);
        if (updated == 0) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }

        // 부여 USED_DAYS 재동기화(차감 일수 변동 반영).
        if (tgt.grantId() != null && !tgt.grantId().isEmpty()) {
            leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, tgt.grantId(), actorUserCd);
        }

        // prafta-com-008-E-2: 출근 차단은 leave_use(START_DATE) 기준 → 위 updateLeaveUseModify 의
        //   START_DATE 갱신만으로 차단일이 이동한다. work_plan 은 SCH_CD 유지(연차블록 미조정).
    }

    private int unitMinutes(String unit) {
        if (UNIT_HOUR2.equals(unit)) return 120;
        if (UNIT_HOUR1.equals(unit)) return 60;
        return 30; // UNIT_MIN30
    }

    private String todayYmd() {
        LocalDate d = LocalDate.now();
        return String.format("%04d%02d%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    // ============================================================
    // prafta-com-011-2 연차 가불 합류 헬퍼 (앱 AppLeaveFlowServiceImpl 미러)
    // ============================================================

    /**
     * 가불 패밀리 판정. 시스템 법정 월차(SYS_MONTHLY)/본연차(SYS_ANNUAL)만 대상.
     * 그 외 leaveCd(타 법정/비법정)는 가불 비대상 → null(호출부에서 ATTD_400_180).
     */
    private BorrowFamily borrowFamilyOf(String leaveCd) {
        if (LEAVE_CD_MONTHLY.equals(leaveCd)) {
            return BorrowFamily.MONTHLY;
        }
        if (LEAVE_CD_ANNUAL.equals(leaveCd)) {
            return BorrowFamily.ANNUAL;
        }
        return null;
    }

    /**
     * PC-02(D8): 일반(비가불·비'01') 신청의 부여 충당 계획 — 만료 임박순(AVAIL_TO_DATE ASC) 다부여 분할 차감(앱 미러).
     *
     * <ul>
     *   <li>잔여&gt;0 활성 부여를 만료 임박순으로 needed 까지 채운다(FOR UPDATE 직렬화 —
     *       {@link #resolveBorrowCharges} 잔여 우선 루프와 동일 패턴, 가불 없이).</li>
     *   <li>합산 잔여 &lt; needed 면 기존과 동일하게 ATTD_400_051(조각 부여 교착만 해소, 거부 기준 불변).</li>
     *   <li>needed 0(하한/캡 이후 차액 0 등)은 기존 단건 경로 유지 — 잔여 0 부여에도 0 차감 행이
     *       기록되던 기존 동작 보존(REQ-사용행 연결 유지, 회귀 0).</li>
     * </ul>
     */
    private List<GrantCharge> resolveGeneralCharges(String cmpny, String user, String leaveCd, String workYmd,
                                                    BigDecimal needed) {
        if (needed.signum() <= 0) {
            DeductibleGrantVO grant = leaveFlowMapper.selectDeductibleGrant(cmpny, user, leaveCd, workYmd, needed);
            if (grant == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_051);
            }
            return List.of(new GrantCharge(grant.grantId(), needed));
        }
        List<DeductibleGrantVO> grants = leaveFlowMapper.selectDeductibleGrants(cmpny, user, leaveCd, workYmd);
        List<GrantCharge> charges = new ArrayList<>();
        BigDecimal remaining = needed;
        for (DeductibleGrantVO g : grants) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal avail = nz(g.grantDays()).subtract(nz(g.usedDays()));
            if (avail.signum() <= 0) {
                continue;
            }
            BigDecimal take = avail.min(remaining);
            charges.add(new GrantCharge(g.grantId(), take));
            remaining = remaining.subtract(take);
        }
        if (remaining.signum() > 0) {
            log.info("[leaveflow] 연차 신청 거부: 합산 잔여 부족 (userCd={}, leaveCd={}, needed={}, 부족분={})",
                    user, leaveCd, needed.toPlainString(), remaining.toPlainString());
            throw new ApiException(AttdErrorCode.ATTD_400_051);
        }
        return charges;
    }

    /**
     * PC-02(D8): 차감 가능한 활성 부여의 합산 잔여 — preview 잔여 부족 판정·짜투리 사전 판정(앱 미러).
     *
     * <p>보안리뷰 M-1: FOR UPDATE 목록 조회 재사용 시 행 잠금 보유 후 remnant advisory lock 을
     * 대기하는 순서 역전(회수 경로와 교차 → GET_LOCK 타임아웃 정지)이 생겨, 잠금 없는 SUM 전용
     * 쿼리로 분리했다. 실제 차감 계획({@code resolveGeneralCharges})은 여전히 FOR UPDATE 로 재판정한다.
     */
    private BigDecimal sumDeductibleRemaining(String cmpny, String user, String leaveCd, String workYmd) {
        BigDecimal sum = leaveFlowMapper.selectDeductibleRemainingSum(cmpny, user, leaveCd, workYmd);
        return (sum == null) ? BigDecimal.ZERO : sum;
    }

    /**
     * 가불(Q1=b): 신청 일수를 잔여 부여로 만료 임박순 분할 차감하고, 부족분(deficit)만큼 가불 슬롯에 충당하는
     *   차감 계획을 만든다(앱 미러).
     *
     * <ul>
     *   <li>잔여(현재 사용가능 active GRANT 의 GRANT_DAYS-USED_DAYS, AVAIL_FROM&lt;=workYmd)를 만료 임박순으로
     *       needed 까지 채운다(FOR UPDATE 직렬화). 미발생 가불 GRANT(AVAIL_FROM&gt;workYmd)는 여기 안 잡힘(D2 잠금).</li>
     *   <li>남은 deficit 이 0 이면 가불 0건(=일반 신청과 동일 결과, 단 결재 강제).</li>
     *   <li>deficit>0 이면 computeBorrowQuota 와 비교 — 초과면 ATTD_400_182. 통과면 createBorrowGrant 로 가불 슬롯
     *       (전량 GRANT, AVAIL_FROM=발생일)을 기존 재사용→신규 생성으로 충당받아(통일 모델 §6-2), 각 슬롯 grantId 로
     *       deficit 만큼 leave_use 를 분할 차감한다. 슬롯 days 는 슬롯별 충당분(take)이라 합이 deficit 과 같다.</li>
     * </ul>
     */
    private List<GrantCharge> resolveBorrowCharges(String cmpny, String user, String leaveCd, String workYmd,
                                                   BigDecimal needed, BorrowFamily family, String hireDate,
                                                   String operatorUserCd) {
        List<GrantCharge> charges = new ArrayList<>();
        BigDecimal remaining = needed;

        // 1) 잔여 우선 차감(만료 임박순, FOR UPDATE).
        List<DeductibleGrantVO> grants =
                leaveFlowMapper.selectBorrowDeductibleGrants(cmpny, user, leaveCd, workYmd);
        for (DeductibleGrantVO g : grants) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal avail = nz(g.grantDays()).subtract(nz(g.usedDays()));
            if (avail.signum() <= 0) {
                continue;
            }
            BigDecimal take = avail.min(remaining);
            charges.add(new GrantCharge(g.grantId(), take));
            remaining = remaining.subtract(take);
        }

        // 2) 부족분만 가불.
        if (remaining.signum() > 0) {
            BigDecimal quota = leaveGrantEngineService.computeBorrowQuota(cmpny, user, hireDate, family);
            if (remaining.compareTo(nz(quota)) > 0) {
                log.info("[leaveflow] 가불 한도 초과 거부 userCd={}, leaveCd={}, deficit={}, quota={}",
                        user, leaveCd, remaining.toPlainString(), nz(quota).toPlainString());
                throw new ApiException(AttdErrorCode.ATTD_400_182);
            }
            BorrowGrantResultVO result = leaveGrantEngineService.createBorrowGrant(
                    cmpny, user, hireDate, family, remaining, workYmd, operatorUserCd);
            BigDecimal borrowRemaining = remaining;
            for (BorrowGrantSlotVO slot : result.getSlots()) {
                if (borrowRemaining.signum() <= 0) {
                    break;
                }
                if (slot.getGrantId() == null) {
                    // 통일 모델(§6-2): 충당 슬롯은 항상 유효 grantId(재사용/신규)를 가진다. 이미 정기 발생 슬롯은
                    //   createBorrowGrant 가 slots 에서 제외(또는 ATTD_400_182 롤백)하므로 null 도달은 비정상 → 차단.
                    throw new ApiException(AttdErrorCode.ATTD_400_182);
                }
                BigDecimal take = nz(slot.getDays()).min(borrowRemaining);
                charges.add(new GrantCharge(slot.getGrantId(), take));
                borrowRemaining = borrowRemaining.subtract(take);
            }
            if (borrowRemaining.signum() > 0) {
                // 가불 GRANT 슬롯 합이 부족분에 못 미침 → 한도 초과로 차단.
                throw new ApiException(AttdErrorCode.ATTD_400_182);
            }
        }
        return charges;
    }

    /** null-safe BigDecimal(0 폴백). */
    private BigDecimal nz(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }

    /** prafta-com-011-2: 차감 대상 1건(부여 ID + 일수). 가불 split 시 한 신청이 여러 GrantCharge 로 분할된다. */
    private record GrantCharge(String grantId, BigDecimal days) {
    }
}
