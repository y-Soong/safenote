package com.prafta.web.leave.promotion.leavepromo01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.leavepromo.LeavePromoErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.PromotionDesignateRequest;

/**
 * prafta-com-008-A-4: 2차 회사직권 지정 Param.
 *
 * <p>관리자 식별값(cmpny/site/user/auth)은 토큰에서만 강제. 본문은 targetUserCd/dates 만 신뢰하며,
 * 대상자 소속 사업장/부서는 서비스가 서버 재조회하여 권한·스코프를 강제한다(IDOR 차단).
 */
public record PromotionDesignateParam(
        String targetUserCd,
        List<String> dates,
        String gvCmpnyCd,
        String gvSiteCd,
        String gvAuthCd,
        String gvUserCd
) {
    public static PromotionDesignateParam from(PromotionDesignateRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request == null || request.getTargetUserCd() == null || request.getTargetUserCd().isBlank()
                || request.getDates() == null || request.getDates().isEmpty()) {
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
        }
        for (String d : request.getDates()) {
            if (d == null || d.length() != 8 || !d.chars().allMatch(Character::isDigit)) {
                throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
            }
        }
        return new PromotionDesignateParam(
                request.getTargetUserCd(),
                request.getDates(),
                tokenInfo.gv_cmpnyCd(),
                tokenInfo.gv_siteCd(),
                tokenInfo.gv_authCd(),
                tokenInfo.gv_userCd()
        );
    }
}
