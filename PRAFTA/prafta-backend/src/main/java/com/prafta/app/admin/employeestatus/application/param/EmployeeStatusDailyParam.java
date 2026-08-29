package com.prafta.app.admin.employeestatus.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PRAFTA-002: 직원 현황(일자) 조회 Param.
 *
 * <p>식별자(cmpny/user/authCd/토큰사업장)는 JWT 클레임에서만 도출한다(IDOR 차단). workYmd/nodeCd/keyword/page
 * 는 클라 입력. {@code siteCd}는 요청 파라미터 우선, 없으면 토큰 {@code gv_siteCd} 로 폴백한다(plan §PRAFTA-002
 * 상세 — "현장 전환 시 갱신" 요구사항. 기존 {@code ATTD_DETAIL/daily}는 이 파라미터가 없었다는 차이점).
 * 최종 인가는 서비스가 {@code SiteAccessService.assertSiteAccess}로 재검증한다(클라 값 신뢰 아님).
 *
 * <p>nodeCd 는 리소스 키이며 서버가 토큰 스코프 내인지 재검증한다(스코프 밖이면 빈 결과).
 */
public record EmployeeStatusDailyParam(
      String workYmd
    , String siteCd        // 조회 대상 사업장(해석 후 값)
    , String nodeCd
    , String keyword
    , int page
    , int pageSize
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd      // 토큰 원 사업장(assertSiteAccess 판정 입력)
    , String gvAuthCd
) {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAGE = 1_000_000;

    public static EmployeeStatusDailyParam of(String workYmd, String siteCd, String nodeCd, String keyword,
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

        // 조회 대상 사업장 — 요청 파라미터 우선, 없으면 토큰 사업장(AppAdminSelfJoinController.resolveSiteCd 패턴).
        String resolvedSiteCd = StringUtils.hasText(siteCd) ? siteCd.trim() : token.gv_siteCd();

        return new EmployeeStatusDailyParam(
                workYmd.trim(),
                resolvedSiteCd,
                (nodeCd == null || nodeCd.isBlank()) ? null : nodeCd.trim(),
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                p, ps,
                token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }
}
