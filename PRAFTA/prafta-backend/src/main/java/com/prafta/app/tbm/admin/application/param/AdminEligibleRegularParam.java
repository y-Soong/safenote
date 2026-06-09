package com.prafta.app.tbm.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * E9 정규직 대리입실 후보 검색 파라미터(prafta-051 R-B).
 *
 * <p>sessionCd 는 path, 검색어/페이지는 query 에서 받되 식별자(회사/사용자/사업장/권한)는 JWT
 * 클레임에서만 도출한다(IDOR 차단). 검색 사업장은 클라이언트가 보낸 값을 신뢰하지 않고 세션 사업장
 * (guard.siteCd)으로 서버가 확정한다.
 */
public record AdminEligibleRegularParam(
    String sessionCd
    , String keyword
    , int page
    , int pageSize
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    /** 기본 페이지 크기(검색 결과 상한). 과도한 PII 노출/대량조회 방지. */
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    public static AdminEligibleRegularParam of(
            String sessionCd, String keyword, Integer page, Integer pageSize, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        int normalizedPage = (page == null || page < 1) ? 1 : page;
        int normalizedPageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : pageSize;
        if (normalizedPageSize > MAX_PAGE_SIZE) {
            normalizedPageSize = MAX_PAGE_SIZE;
        }

        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        return new AdminEligibleRegularParam(
            sessionCd
            , normalizedKeyword
            , normalizedPage
            , normalizedPageSize
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
