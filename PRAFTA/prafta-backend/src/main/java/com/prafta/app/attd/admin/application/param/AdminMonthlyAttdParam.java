package com.prafta.app.attd.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * J1-5: 월별 집계(monthly) 조회 Param.
 *
 * <p>식별자(cmpny/user/site/auth)는 JWT 클레임에서만 도출한다(IDOR 차단). yearMonth/nodeCd/keyword/page 만 클라 입력.
 * <p>yearMonth 는 YYYYMM 또는 YYYY-MM 모두 허용하며 내부에서 YYYYMM 로 정규화한다.
 */
public record AdminMonthlyAttdParam(
      String workYm
    , String nodeCd
    , String keyword
    , int page
    , int pageSize
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAGE = 1_000_000;

    public static AdminMonthlyAttdParam of(String yearMonth, String nodeCd, String keyword,
            Integer page, Integer pageSize, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String ym = yearMonth == null ? "" : yearMonth.replace("-", "").trim();
        if (ym.length() != 6 || !ym.chars().allMatch(Character::isDigit)) {
            // 대상월(YYYYMM) 필수 — 형식 오류는 잘못된 요청.
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        int p = (page == null || page < 1) ? 1 : Math.min(page, MAX_PAGE);
        int ps = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        return new AdminMonthlyAttdParam(
                ym,
                (nodeCd == null || nodeCd.isBlank()) ? null : nodeCd.trim(),
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                p, ps,
                token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }
}
