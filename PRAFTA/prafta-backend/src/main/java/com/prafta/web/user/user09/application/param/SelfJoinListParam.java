package com.prafta.web.user.user09.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user09.dto.request.SelfJoinListRequest;

/**
 * 소정-09: 셀프가입 신청 목록 조회 파라미터.
 *
 * <p>Attd_16 {@code LeaveUsageCalendarParam} 패턴 — 회사/요청자/권한/토큰 사업장은 오직 JWT
 * 클레임에서만 도출하고, 조회 사업장(siteCd)의 접근 인가는 서비스 계층
 * {@code SiteAccessService.assertSiteAccess} + {@code canManageNodeExcludeSafe} 가 강제한다.
 */
public record SelfJoinListParam(
        String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String accountStatus
        , String userKeyword
        , String gvCmpnyCd
        , String gvAuthCd
        , String gvUserCd
        , String gvSiteCd
) {
    /** 승인대기 [SYS013 '06'] — 기본 필터. */
    public static final String STATUS_PENDING = "06";

    /** 가입거부 [SYS013 '07'] */
    public static final String STATUS_REJECTED = "07";

    public static SelfJoinListParam from(SelfJoinListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // 상태 필터는 화이트리스트로만 받는다 — 임의 상태를 넣어 활성 계정 목록(PII)을 뽑는 것을 막는다.
        String status = request.getAccountStatus();
        if (status == null || status.isBlank()) {
            status = STATUS_PENDING;
        }
        if (!STATUS_PENDING.equals(status) && !STATUS_REJECTED.equals(status)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new SelfJoinListParam(
                request.getSiteCd()
                , request.getNodeCd()
                , "Y".equals(request.getIncSubNodeYn()) ? "Y" : "N"
                , status
                , request.getUserKeyword()
                , tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_authCd()
                , tokenInfo.gv_userCd()
                , tokenInfo.gv_siteCd()
        );
    }
}
