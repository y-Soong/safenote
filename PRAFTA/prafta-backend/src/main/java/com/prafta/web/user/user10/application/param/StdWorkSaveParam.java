package com.prafta.web.user.user10.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user10.dto.request.StdWorkSaveRequest;

/**
 * 소정-10: 소정근로시간 이력 등록/정정 파라미터.
 *
 * <p>회사/작업자는 JWT 클레임에서만 도출한다 — {@code StdWorkHoursSaveCommand} 의
 * cmpnyCd/actorNo 규약(클라 바디 신뢰 금지, IDOR 차단)과 직결된다.
 */
public record StdWorkSaveParam(
        String userCd
        , String applyStrDate
        , String applyEndDate
        , Integer weekStdMinutes
        , String reasonCd
        , String reasonDetail
        , String gvCmpnyCd
        , String gvAuthCd
        , String gvUserCd
        , String gvSiteCd
) {
    /** 사유 상세 최대 길이 (DDL varchar(500) 정합 — 서버측 truncation/500 방지). */
    private static final int REASON_DETAIL_MAX_LEN = 500;

    public static StdWorkSaveParam from(StdWorkSaveRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getUserCd() == null || request.getUserCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String reasonDetail = trimToNull(request.getReasonDetail());
        if (reasonDetail != null && reasonDetail.length() > REASON_DETAIL_MAX_LEN)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new StdWorkSaveParam(
                request.getUserCd().trim()
                , trimToNull(request.getApplyStrDate())
                , trimToNull(request.getApplyEndDate())
                , request.getWeekStdMinutes()
                , trimToNull(request.getReasonCd())
                , reasonDetail
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
