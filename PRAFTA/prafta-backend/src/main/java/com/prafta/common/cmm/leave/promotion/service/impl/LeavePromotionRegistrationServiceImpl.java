package com.prafta.common.cmm.leave.promotion.service.impl;

import java.math.BigDecimal;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.leave.promotion.mapper.LeavePromotionMapper;
import com.prafta.common.cmm.leave.promotion.service.LeavePromotionRegistrationService;
import com.prafta.common.cmm.leave.promotion.vo.PromotionLeaveUseInsertVO;
import com.prafta.common.cmm.leave.promotion.vo.PromotionRegisterResult;
import com.prafta.common.cmm.sch.service.DefaultSchGenService;
import com.prafta.common.cmm.shift.service.ShiftMembershipService;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.leaveflow.mapper.LeaveFlowMapper;
import com.prafta.web.attd.leaveflow.vo.DeductibleGrantVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 촉진 연차 1일 등록 공용 헬퍼 구현 (PRAFTA-COM-008-A-3/A-4).
 *
 * <p>1차(앱 자발)·2차(웹 회사직권) 공용. {@code LeaveFlowMapper} 의 grant 차감/USED_DAYS 동기화/
 * 직접차감 멱등 카운트를 재사용하고, leave_use INSERT 만 촉진 마커를 싣는 전용 매퍼
 * ({@code LeavePromotionMapper.insertPromotionLeaveUse})를 쓴다.
 *
 * <p>흐름(단일일):
 *   ① 마감월 가드(CLOSED) → ② 교대 가드(교대일이면 work_plan 존재 필수, 아니면 ensureWorkPlanDay 폴백)
 *   → ③ 멱등 카운트(이미 등록=SKIPPED_DUP) → ④ 본연차 grant 차감 대상 선택(없으면 INSUFFICIENT)
 *   → ⑤ leave_use INSERT(촉진 마커) → ⑥ grant USED_DAYS 재계산.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeavePromotionRegistrationServiceImpl implements LeavePromotionRegistrationService {

    private final LeavePromotionMapper leavePromotionMapper;
    private final LeaveFlowMapper leaveFlowMapper;
    private final DefaultSchGenService defaultSchGenService;
    private final ShiftMembershipService shiftMembershipService;
    private final AttdCloseService attdCloseService;

    @Override
    @Transactional
    public PromotionRegisterResult register(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                            String promotionStage, String designatorType,
                                            String reason, String operatorUserCd) {
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(siteCd)
                || !StringUtils.hasText(userCd) || !StringUtils.hasText(workYmd)
                || workYmd.length() != 8) {
            return PromotionRegisterResult.INSUFFICIENT;
        }

        // ① 마감월 가드(prafta-028) — 마감된 부서·월에는 등록하지 않는다.
        if (attdCloseService.isClosedForUser(cmpnyCd, siteCd, userCd, workYmd.substring(0, 6))) {
            return PromotionRegisterResult.CLOSED;
        }

        // ①-B 출근 근태 상호배제 게이트(§9.4, 이관설계 §5-4, B-M1).
        //   근로자 신청 경로(AppLeaveFlowServiceImpl §9.4=ATTD_400_108)와 동일 술어를 적용해,
        //   출근만 찍힌 날에 촉진 연차가 얹혀 열린 근태를 못 닫는 퇴근 트랩을 입구에서 원천 차단한다.
        //   register 는 enum 반환 계약이므로 throw 가 아닌 결과값으로 반환한다.
        if (leavePromotionMapper.countAttendanceOn(cmpnyCd, siteCd, userCd, workYmd) > 0) {
            log.info("[leavePromotion] 출근 근태 존재 일자 촉진 등록 거부 — userCd={}, workYmd={}", userCd, workYmd);
            return PromotionRegisterResult.ATTENDANCE_EXISTS;
        }

        // ② work_plan 폴백 / 교대 가드.
        //   - 교대팀 소속일: 즉석 생성 금지(교대패턴 우선) → 기존 근무일(work_plan 존재)에만 허용.
        //   - 교대 비소속일: ensureWorkPlanDay 로 폴백 생성(기본근무 미설정이면 미생성 — leave_use 로만 표현).
        boolean shiftDay = shiftMembershipService.isInShiftTeamOn(cmpnyCd, siteCd, userCd, workYmd);
        if (shiftDay) {
            if (leavePromotionMapper.countWorkPlanDay(cmpnyCd, siteCd, userCd, workYmd) <= 0) {
                log.info("[leavePromotion] 교대자 비근무일 촉진 등록 거부 — userCd={}, workYmd={}", userCd, workYmd);
                return PromotionRegisterResult.NOT_SCHEDULED;
            }
        } else {
            defaultSchGenService.ensureWorkPlanDay(cmpnyCd, userCd, workYmd);
        }

        // ③ 멱등: 동일 사용자·일자·SYS_ANNUAL 직접 사용기록이 있으면 중복 차감 방지.
        if (leaveFlowMapper.countDirectLeaveUse(cmpnyCd, userCd, workYmd, SYS_ANNUAL) > 0) {
            return PromotionRegisterResult.SKIPPED_DUP;
        }

        // ④ 차감 대상 본연차 grant 1일 (SYS_ANNUAL, 만료 임박순, FOR UPDATE). 없으면 잔여 부족.
        BigDecimal oneDay = BigDecimal.ONE;
        DeductibleGrantVO grant =
                leaveFlowMapper.selectDeductibleGrant(cmpnyCd, userCd, SYS_ANNUAL, workYmd, oneDay);
        if (grant == null) {
            return PromotionRegisterResult.INSUFFICIENT;
        }

        // ⑤ leave_use INSERT(촉진 마커). ORIG_DESIGNATED_DATE = 최초 등록 시 START_DATE 와 동일.
        String leaveId = leaveFlowMapper.selectNextLeaveId(cmpnyCd);
        PromotionLeaveUseInsertVO use = PromotionLeaveUseInsertVO.builder()
                .leaveId(leaveId)
                .cmpnyCd(cmpnyCd)
                .siteCd(siteCd)
                .userCd(userCd)
                .leaveCd(SYS_ANNUAL)
                .grantId(grant.grantId())
                .startDate(workYmd)
                .endDate(workYmd)
                .useUnitType(UNIT_FULL)
                .leaveDays(oneDay)
                .leaveReason(reason)
                .leaveStatus("CONFIRMED")
                .promotionStage(promotionStage)
                .designatorType(designatorType)
                .origDesignatedDate(workYmd)
                .insertNo(operatorUserCd)
                .build();
        try {
            leavePromotionMapper.insertPromotionLeaveUse(use);
        } catch (DuplicateKeyException e) {
            // 동시 저장 경합(TOCTOU): UK_LEAVE_USE_DIRECT 가 최종 차단 → 이미 등록된 것으로 본다.
            log.info("[leavePromotion] 촉진 연차 등록 멱등키 경합 — userCd={}, workYmd={}", userCd, workYmd);
            return PromotionRegisterResult.SKIPPED_DUP;
        }

        // ⑥ grant USED_DAYS 재계산(차감 반영 = 미사용 잔여에서 즉시 차감 = "미래 등록=사용 간주").
        leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, grant.grantId(), operatorUserCd);

        log.info("[leavePromotion] 촉진 연차 등록 — userCd={}, workYmd={}, stage={}, designator={}, grantId={}",
                userCd, workYmd, promotionStage, designatorType, grant.grantId());
        return PromotionRegisterResult.REGISTERED;
    }
}
