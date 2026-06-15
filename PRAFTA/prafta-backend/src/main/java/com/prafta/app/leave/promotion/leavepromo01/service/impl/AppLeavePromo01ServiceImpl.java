package com.prafta.app.leave.promotion.leavepromo01.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.app.leave.promotion.leavepromo01.application.param.PromotionPlanParam;
import com.prafta.app.leave.promotion.leavepromo01.dto.response.PromotionActiveResponse;
import com.prafta.app.leave.promotion.leavepromo01.dto.response.PromotionPlanResultResponse;
import com.prafta.app.leave.promotion.leavepromo01.service.AppLeavePromo01Service;
import com.prafta.common.cmm.leave.promotion.mapper.LeavePromotionMapper;
import com.prafta.common.cmm.leave.promotion.service.LeavePromotionRegistrationService;
import com.prafta.common.cmm.leave.promotion.vo.PromotionActiveContextVO;
import com.prafta.common.cmm.leave.promotion.vo.PromotionRegisterResult;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-com-008-A-3: 앱 1차 연차 사용촉진 계획서 서비스 구현(앱 완전 분리).
 *
 * <p>식별값은 JWT(TokenInfo)에서만 강제. 촉진 연차 등록은 공용 헬퍼
 * {@code LeavePromotionRegistrationService}(stage=FIRST, designator=VOLUNTARY)를 재사용한다.
 * 마스터(STAGE1_DESIGNATED_DAYS) 갱신 시 회차 키는 A-2 통지와 동일 규약
 * {@code PROMO_NOTICE_{USER}_{availTo}} 를 따른다(같은 회차 일관성).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppLeavePromo01ServiceImpl implements AppLeavePromo01Service {

    private static final String STAGE_FIRST = "FIRST";
    private static final String DESIGNATOR_VOLUNTARY = "VOLUNTARY";
    private static final String REASON_FIRST = "연차 사용촉진 1차 계획(자발)";

    private final LeavePromotionMapper leavePromotionMapper;
    private final LeavePromotionRegistrationService promotionRegistrationService;

    @Override
    public PromotionActiveResponse getActiveContext(TokenInfo tokenInfo) {
        String cmpnyCd = requireCmpny(tokenInfo);
        String userCd = requireUser(tokenInfo);

        PromotionActiveContextVO ctx = leavePromotionMapper.selectActiveFirstContext(cmpnyCd, userCd);
        if (ctx == null) {
            // 진행 중 1차 촉진 없음 → 빈 컨텍스트(프론트가 팝업/계획화면 미노출).
            return PromotionActiveResponse.builder()
                    .inProgress(false)
                    .designatedDates(new ArrayList<>())
                    .build();
        }
        List<String> dates = leavePromotionMapper.selectMyPromotionLeaveDates(cmpnyCd, userCd);
        return PromotionActiveResponse.builder()
                .inProgress(true)
                .grantedDays(ctx.getGrantedDays())
                .remainingDays(ctx.getRemainingDays())
                .baseAvailToDate(ctx.getBaseAvailToDate())
                .designatedDates(dates == null ? new ArrayList<>() : dates)
                .loginNotifiedYn(ctx.getLoginNotifiedYn())
                .build();
    }

    @Override
    @Transactional
    public PromotionPlanResultResponse submitPlan(PromotionPlanParam param) {
        String cmpnyCd = param.gvCmpnyCd();
        String siteCd = param.gvSiteCd();
        String userCd = param.gvUserCd();

        // 진행 중 1차 촉진 컨텍스트 확인(회차 기준 만료일 = 마스터 DEDUP 키 산출에 필요).
        PromotionActiveContextVO ctx = leavePromotionMapper.selectActiveFirstContext(cmpnyCd, userCd);
        if (ctx == null) {
            // 진행 중 1차 촉진이 없으면 등록 대상 아님(프론트 게이트 우회 방어).
            log.info("[appLeavePromo] 진행 중 1차 촉진 없음 — 등록 거부. userCd={}", userCd);
            return PromotionPlanResultResponse.builder()
                    .registeredDates(new ArrayList<>())
                    .skippedDates(new ArrayList<>())
                    .failedDates(new ArrayList<>(param.dates()))
                    .remainingDays(BigDecimal.ZERO)
                    .build();
        }

        List<String> registered = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        int registeredCount = 0;

        for (String workYmd : param.dates()) {
            PromotionRegisterResult r = promotionRegistrationService.register(
                    cmpnyCd, siteCd, userCd, workYmd,
                    STAGE_FIRST, DESIGNATOR_VOLUNTARY, REASON_FIRST, userCd);
            switch (r) {
                case REGISTERED -> {
                    registered.add(workYmd);
                    registeredCount++;
                }
                case SKIPPED_DUP -> skipped.add(workYmd);
                case ATTENDANCE_EXISTS -> {
                    // §9.4 상호배제(B-M1): 출근 기록이 있는 날은 촉진 연차로 등록할 수 없음 → 실패 분류.
                    log.info("[appLeavePromo] 출근 기록 존재로 촉진 등록 불가 — userCd={}, workYmd={}", userCd, workYmd);
                    failed.add(workYmd);
                }
                default -> failed.add(workYmd); // INSUFFICIENT / NOT_SCHEDULED / CLOSED
            }
        }

        // 마스터 1차 자발 지정 일수 스냅샷 누적(신규 등록분만, 1일=1.0).
        if (registeredCount > 0) {
            String dedupKey = "PROMO_NOTICE_" + userCd + "_" + ctx.getBaseAvailToDate();
            leavePromotionMapper.addStage1DesignatedDays(
                    cmpnyCd, dedupKey, BigDecimal.valueOf(registeredCount), userCd);
        }

        // 등록 후 잔여 재조회(즉시 USED_DAYS 반영분 = 미지정 잔여 감소).
        PromotionActiveContextVO after = leavePromotionMapper.selectActiveFirstContext(cmpnyCd, userCd);
        BigDecimal remaining = (after != null && after.getRemainingDays() != null)
                ? after.getRemainingDays() : BigDecimal.ZERO;

        log.info("[appLeavePromo] 1차 계획 등록 — userCd={}, 요청 {}건, 등록 {}, 스킵 {}, 실패 {}",
                userCd, param.dates().size(), registered.size(), skipped.size(), failed.size());

        return PromotionPlanResultResponse.builder()
                .registeredDates(registered)
                .skippedDates(skipped)
                .failedDates(failed)
                .remainingDays(remaining)
                .build();
    }

    @Override
    @Transactional
    public void markLoginNotified(TokenInfo tokenInfo) {
        String cmpnyCd = requireCmpny(tokenInfo);
        String userCd = requireUser(tokenInfo);
        leavePromotionMapper.markLoginNotified(cmpnyCd, userCd, userCd);
    }

    private String requireCmpny(TokenInfo t) {
        if (t == null || !StringUtils.hasText(t.gv_cmpnyCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return t.gv_cmpnyCd();
    }

    private String requireUser(TokenInfo t) {
        if (t == null || !StringUtils.hasText(t.gv_userCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return t.gv_userCd();
    }
}
