package com.prafta.app.attd.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * J1-5: 일자 근태 현황(daily) 조회 Param.
 *
 * <p>식별자(cmpny/user/site/auth)는 JWT 클레임에서만 도출한다(IDOR 차단). workYmd/nodeCd/keyword/page 만 클라 입력.
 * <p>nodeCd 는 리소스 키이며 서버가 토큰 스코프 내인지 재검증한다(스코프 밖이면 빈 결과).
 */
public record AdminDailyAttdParam(
      String workYmd
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

    public static AdminDailyAttdParam of(String workYmd, String nodeCd, String keyword,
            Integer page, Integer pageSize, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (workYmd == null || workYmd.trim().length() != 8 || !workYmd.trim().chars().allMatch(Character::isDigit)) {
            // 대상일자(YYYYMMDD) 필수 — 형식 오류는 잘못된 요청.
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        int p = (page == null || page < 1) ? 1 : Math.min(page, MAX_PAGE);
        int ps = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        return new AdminDailyAttdParam(
                workYmd.trim(),
                (nodeCd == null || nodeCd.isBlank()) ? null : nodeCd.trim(),
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                p, ps,
                token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }
}
