package com.prafta.app.approval.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 001-P2-B2: 앱 관리자 승인 대기 리스트(A-1) 조회 Param.
 *
 * <p>식별자(cmpny/user/site/auth)는 JWT 클레임에서만 도출한다(IDOR 차단). group/sort/keyword/page 만 클라 입력.
 */
public record ApprovalPendingParam(
      String group
    , String sort
    , String keyword
    , int page
    , int pageSize
    /**
     * prafta-leavemulti: 연차 기간(From-To) 신청 묶음을 카드 1건으로 접어 내릴지 여부.
     *
     * <p>구버전 앱 번들 무회귀를 위한 게이팅 값이다. 미지정(false)이면 종전 SQL·종전 응답 그대로다.
     */
    , boolean groupLeave
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    // Fix1: (page-1)*pageSize 의 int 오버플로/offset 폭주 방어용 page 상한. 실제 offset 상한(빈 페이지 전환)은
    //   서비스(MAX_OFFSET)에서 일관 처리하므로, 여기서는 오버플로만 막는 넉넉한 천장만 둔다.
    private static final int MAX_PAGE = 1_000_000;

    public static ApprovalPendingParam of(String group, String sort, String keyword,
            Integer page, Integer pageSize, String groupLeave, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String g = (group == null || group.isBlank()) ? "ALL" : group.trim().toUpperCase();
        String s = "DEADLINE".equalsIgnoreCase(sort) ? "DEADLINE" : "REQUESTED";
        int p = (page == null || page < 1) ? 1 : Math.min(page, MAX_PAGE);
        int ps = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        // prafta-leavemulti: 'Y'/'true' 만 접기 활성(그 외·미지정은 종전 동작). 구버전 앱 번들 무회귀 게이트.
        boolean gl = "Y".equalsIgnoreCase(groupLeave) || "true".equalsIgnoreCase(groupLeave);
        return new ApprovalPendingParam(g, s,
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                p, ps, gl, token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }
}
