package com.prafta.web.user.user09.application.param;

import java.util.regex.Pattern;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user09.dto.request.SelfJoinHistoryListRequest;

/**
 * 소정-09: 셀프가입 처리 이력 목록 조회 파라미터.
 *
 * <p>{@link SelfJoinListParam} 과 동일 골격 — 회사/요청자/권한/토큰 사업장은 오직 JWT 클레임에서만
 * 도출하고, 조회 사업장(siteCd)의 접근 인가는 서비스 계층
 * {@code SiteAccessService.assertSiteAccess} + {@code canManageNodeExcludeSafe} 가 강제한다.
 *
 * <p><b>앱 재사용 계약</b> — 본 record 와 뒤따르는 Query/Result/Response 는 {@code HttpServletRequest}
 * 등 웹 전용 타입을 일절 참조하지 않는다. 모바일 앱은 {@code com.prafta.app.*} 아래에 컨트롤러만
 * 추가하고 {@code from(자체 Request, tokenInfo)} 또는 record 정식 생성자로 본 파라미터를 만들어
 * {@code User09Service} 빈을 그대로 호출하면 된다(무한 스크롤이어도 page/pageSize/totalCount
 * 계약만 맞추면 된다).
 */
public record SelfJoinHistoryListParam(
        String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String userKeyword
        , String actionType
        , String startDate
        , String endDate
        , int page
        , int pageSize
        , String gvCmpnyCd
        , String gvAuthCd
        , String gvUserCd
        , String gvSiteCd
) {
    /** 처리 결과 — 승인 (감사 로그 detailJson 의 action 값). */
    public static final String ACTION_APPROVE = "APPROVE";

    /** 처리 결과 — 거부 (감사 로그 detailJson 의 action 값). */
    public static final String ACTION_REJECT = "REJECT";

    /** 기본 페이지 크기. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 페이지 크기 상한.
     *
     * <p>★상한이 없으면 {@code pageSize=100000} 한 방으로 사업장 전 인원의 이름·마스킹 휴대폰을
     * 덤프할 수 있다(PII 최소 노출 원칙 위배).
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 페이지 번호 상한.
     *
     * <p>★상한이 없으면 {@code page * pageSize} 의 int 곱셈이 오버플로해 <b>음수 OFFSET</b> 이 되고
     * (예: {@code page=2147483647&pageSize=100} → offset -200) MySQL 1064 로 500 이 난다.
     * 실데이터로 도달할 수 없는 값이므로 넉넉히 두되 곱셈이 int 범위를 넘지 않게 막는다.
     */
    public static final int MAX_PAGE = 100_000;

    /** 처리기간 입력 형식(yyyy-MM-dd). 화면은 CalendarSrch 로만 입력하므로 형식 위반은 미입력 취급한다. */
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    public static SelfJoinHistoryListParam from(SelfJoinHistoryListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // 처리 결과는 화이트리스트로만 받는다 — 임의 문자열이 조회 술어로 흘러가지 않게 계약을 좁힌다.
        String actionType = request.getActionType();
        if (actionType != null && actionType.isBlank()) {
            actionType = null;
        }
        if (actionType != null && !ACTION_APPROVE.equals(actionType) && !ACTION_REJECT.equals(actionType)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        int page = (request.getPage() == null || request.getPage() < 1) ? 1 : request.getPage();
        if (page > MAX_PAGE) {
            page = MAX_PAGE;
        }
        int pageSize = (request.getPageSize() == null || request.getPageSize() < 1)
                ? DEFAULT_PAGE_SIZE : request.getPageSize();
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        return new SelfJoinHistoryListParam(
                request.getSiteCd()
                , request.getNodeCd()
                , "Y".equals(request.getIncSubNodeYn()) ? "Y" : "N"
                , request.getUserKeyword()
                , actionType
                , normalizeDate(request.getStartDate())
                , normalizeDate(request.getEndDate())
                , page
                , pageSize
                , tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_authCd()
                , tokenInfo.gv_userCd()
                , tokenInfo.gv_siteCd()
        );
    }

    /** yyyy-MM-dd 형식만 통과시키고 그 외(공백/형식 위반)는 미입력(null)으로 본다. */
    private static String normalizeDate(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return DATE_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }
}
