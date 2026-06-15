package com.prafta.app.leave.promotion.leavepromo01.application.param;

import java.util.List;

import com.prafta.app.leave.promotion.leavepromo01.dto.request.PromotionPlanRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.leavepromo.LeavePromoErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-com-008-A-3: 앱 1차 계획서 등록 Param.
 *
 * <p>신청자(cmpny/site/user)는 토큰에서만 강제(IDOR 차단). 본문은 dates 만 신뢰한다. 토큰 식별값 누락은
 * COMMON_400_001(토큰오류 아님 — 인터셉터 강제로그아웃 회피, 메모리 app_req07), 날짜 검증 실패는
 * LEAVEPROMO_400_001 로 분리한다.
 */
public record PromotionPlanParam(
        List<String> dates,
        String gvCmpnyCd,
        String gvSiteCd,
        String gvUserCd
) {
    public static PromotionPlanParam from(PromotionPlanRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request == null || request.getDates() == null || request.getDates().isEmpty()) {
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
        }
        for (String d : request.getDates()) {
            if (d == null || d.length() != 8 || !d.chars().allMatch(Character::isDigit)) {
                throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
            }
        }
        return new PromotionPlanParam(
                request.getDates(),
                tokenInfo.gv_cmpnyCd(),
                tokenInfo.gv_siteCd(),
                tokenInfo.gv_userCd()
        );
    }
}
