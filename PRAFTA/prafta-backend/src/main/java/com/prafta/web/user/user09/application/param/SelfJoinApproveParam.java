package com.prafta.web.user.user09.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user09.dto.request.SelfJoinApproveRequest;

/**
 * 소정-09: 셀프가입 승인 파라미터.
 *
 * <p>대상 사용자(userCd)만 바디에서 받고, 회사/권한/요청자/토큰 사업장은 JWT 클레임에서만
 * 도출한다. 대상의 사업장·부서는 서비스가 DB 에서 재조회한다(바디 값 불신 — IDOR 차단).
 */
public record SelfJoinApproveParam(
        String userCd
        , String hireDate
        , String employmentType
        , String rankCd
        , String stdWorkType
        , Integer stdWorkWeekMinutes
        , String stdWorkReasonCd
        , String gvCmpnyCd
        , String gvAuthCd
        , String gvUserCd
        , String gvSiteCd
) {
    public static SelfJoinApproveParam from(SelfJoinApproveRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getUserCd() == null || request.getUserCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new SelfJoinApproveParam(
                request.getUserCd().trim()
                , trimToNull(request.getHireDate())
                , trimToNull(request.getEmploymentType())
                , trimToNull(request.getRankCd())
                , trimToNull(request.getStdWorkType())
                , request.getStdWorkWeekMinutes()
                , trimToNull(request.getStdWorkReasonCd())
                , tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_authCd()
                , tokenInfo.gv_userCd()
                , tokenInfo.gv_siteCd()
        );
    }

    private static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
