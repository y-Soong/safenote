package com.prafta.web.user.user01.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.prafta.common.cmm.shift.service.ShiftMembershipService;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.web.user.user01.mapper.UserTransferMapper;
import com.prafta.web.user.user01.result.PartialLeaveTimeResult;
import com.prafta.web.user.user01.result.TransferBlockReason;
import com.prafta.web.user.user01.result.UserTransferBasicResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소속이동 5종 불가케이스 사전 검증기 (정규직 한정) — PRAFTA-WEB_001-1.
 *
 * <p>판정 결과를 {@link TransferBlockReason} 목록으로 반환한다(다중 사유 누적).
 * eligibility 조회(다중 사유 표시)와 등록 검증(첫 사유로 fail-closed)에서 공통 사용한다.
 *
 * <p>정규직 정의 = EMPLOYMENT_TYPE != 'DAILY'(NULL 포함이 정규직). 일용직은 본 검증을 호출하지 않는다.
 *
 * <p>판정 기준(확정 결정사항):
 * <ul>
 *   <li>① 사업장 관리자 = 대상자가 현재 사업장 SITE_ADMIN_CD.</li>
 *   <li>② 노드 마지막 담당자 = 대상자가 MAIN/SUB 담당자이고 제거 시 담당자 0명.</li>
 *   <li>③ 순회점검 담당자 = 대상자가 TB_CHKPT_TYPE_MGMT.MGMT_USER_CD(USE_YN='Y').</li>
 *   <li>④ 교대조 소속 = 이동일 기준 {@link ShiftMembershipService#isInShiftTeamOn}.</li>
 *   <li>⑤ 미래 시간차 연차 보유 = 발효일 이후 확정 시간차 사용 또는 미결 시간차 신청 존재
 *       (E1·W8 당일분모 전환, 2026-08-04 사용자 확정 — 구 "커버리지 미커버" 판정 대체.
 *       발효 시 구 사업장 미래 근무계획이 전량 삭제(WORK_YMD &gt;= 발효일)되어 시간차 분모
 *       (당일 배정 스케줄)가 소실되므로, 커버리지와 무관하게 존재 자체로 차단한다.
 *       종일/반차/반반차는 차감량이 스케줄 무관이라 이동 허용).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTransferValidator {

    private final UserTransferMapper userTransferMapper;
    private final ShiftMembershipService shiftMembershipService;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 5종 불가케이스를 평가해 불가 사유 목록을 반환한다(빈 목록이면 가능).
     *
     * @param cmpnyCd         회사 코드(토큰 도출값, 스코프)
     * @param target          대상 사용자 기본 정보(현재 사업장/부서/고용형태)
     * @param toDefaultSchCd  관리자가 지정한 기본 근무타입(구 ⑤ 커버리지 판정용 — E1·W8 로 ⑤가 존재 판정으로
     *                        바뀌어 미사용. 호출부 시그니처 안정성을 위해 유지)
     * @param toSiteCd        이동 사업장(구 ⑤ 커버리지 판정용 — 상동, 미사용 유지)
     * @param moveDate        소속이동일 YYYYMMDD(④/⑤ 판정용, null/blank 이면 ④는 오늘 기준, ⑤는 생략 —
     *                        예약·발효 경로는 항상 보유하므로 강제 경로에서 생략되는 일은 없다)
     * @return 불가 사유 목록(없으면 빈 목록)
     */
    public List<TransferBlockReason> evaluate(String cmpnyCd, UserTransferBasicResult target, String toDefaultSchCd,
            String toSiteCd, String moveDate) {

        List<TransferBlockReason> reasons = new ArrayList<>();
        String userCd = target.userCd();
        String fromSiteCd = target.siteCd();

        // ① 사업장 관리자
        if (fromSiteCd != null && !fromSiteCd.isBlank()
                && userTransferMapper.selectIsSiteAdmin(cmpnyCd, fromSiteCd, userCd) > 0) {
            reasons.add(reason(UserErrorCode.USER_400_065));
        }

        // ② 노드 마지막 담당자(정/부 불문)
        if (userTransferMapper.selectLastAdminNodeCnt(cmpnyCd, userCd) > 0) {
            reasons.add(reason(UserErrorCode.USER_400_066));
        }

        // ③ 순회점검 담당자
        if (userTransferMapper.selectIsChkptManager(cmpnyCd, userCd) > 0) {
            reasons.add(reason(UserErrorCode.USER_400_067));
        }

        // ④ 교대조 소속(이동일 기준, 이동일 없으면 오늘 기준)
        String shiftJudgeDate = (moveDate != null && !moveDate.isBlank()) ? moveDate : LocalDate.now().format(YMD);
        if (shiftMembershipService.isInShiftTeamOn(cmpnyCd, fromSiteCd, userCd, shiftJudgeDate)) {
            reasons.add(reason(UserErrorCode.USER_400_068));
        }

        // ⑤ 미래 시간차 연차 보유(E1·W8 당일분모 전환 — 이동일 있어야 판정 가능. 예약·발효 경로는 항상 보유).
        //   구 커버리지 판정(기본근무타입이 시간대를 감싸는지, USER_400_069)은 대체 — 발효 시 미래 근무계획이
        //   전량 삭제되어 분모 소스가 소실되므로, 커버리지와 무관하게 존재 자체로 차단(USER_400_073).
        if (moveDate != null && !moveDate.isBlank()) {
            if (hasFutureHourlyLeave(cmpnyCd, userCd, moveDate)) {
                reasons.add(reason(UserErrorCode.USER_400_073));
            }
        }

        return reasons;
    }

    /**
     * 불가⑤ 판정(E1·W8): 발효일(moveDate, 당일 포함) 이후 확정 시간차 사용 또는 미결 시간차 신청이
     * 하나라도 있으면 true. 경계는 발효 시 미래 근무계획 삭제(deleteFutureWorkPlansOnSite,
     * WORK_YMD &gt;= 발효일)와 동일 — 삭제 범위 안의 시간차가 차단 대상이다.
     * 판정 쿼리 예외는 삼키지 않고 전파한다(fail-closed — 허용으로 열리지 않음).
     */
    private boolean hasFutureHourlyLeave(String cmpnyCd, String userCd, String moveDate) {
        List<PartialLeaveTimeResult> confirmed = userTransferMapper.selectFuturePartialLeaves(cmpnyCd, userCd, moveDate);
        if (confirmed != null && !confirmed.isEmpty()) {
            log.info("소속이동 불가⑤(E1·W8) - 발효일 이후 확정 시간차 보유. userCd={}, moveDate={}, cnt={}",
                    userCd, moveDate, confirmed.size());
            return true;
        }
        int pendingCnt = userTransferMapper.selectFuturePendingHourlyReqCnt(cmpnyCd, userCd, moveDate);
        if (pendingCnt > 0) {
            log.info("소속이동 불가⑤(E1·W8) - 발효일 이후 미결 시간차 신청 보유. userCd={}, moveDate={}, cnt={}",
                    userCd, moveDate, pendingCnt);
            return true;
        }
        return false;
    }

    private TransferBlockReason reason(UserErrorCode code) {
        return new TransferBlockReason(code.code(), code.message());
    }
}
