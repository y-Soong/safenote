package com.prafta.web.tbm.tbm04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm04.dto.request.EvidenceSessionListRequest;

/**
 * TBM 증빙자료 조회 파라미터 (년도+반기 → 기간 파생은 EvidenceQuery 가 수행).
 *
 * <p>식별자(회사/사업장/권한)는 JWT 클레임에서만 도출한다(IDOR 차단).
 */
public record EvidenceListParam(
    String year
    , String half
    , String siteCd
    , String gvCmpnyCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static EvidenceListParam from(EvidenceSessionListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // 년도(4자리 숫자) / 반기(H1|H2) 형식 검증 — 기간 파생의 입력이므로 여기서 차단.
        String year = request.getYear();
        String half = request.getHalf();
        if (year == null || !year.matches("^(19|20)\\d{2}$"))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (!"H1".equals(half) && !"H2".equals(half))
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new EvidenceListParam(
            year
            , half
            , request.getSiteCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
