package com.prafta.web.attd.attd09.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd09.dto.request.LeaveDashboardListRequest;

/**
 * 연차 현황 대시보드 목록 조회 Param.
 *
 * <p>GET endpoint. CMPNY_CD는 JWT에서만 취득(요청 body/query의 cmpnyCd 미신뢰 — 가드레일 3).
 *
 * <p>대시보드 조회는 전 직원 PII를 노출하므로 관리자(MASTER/HR) 권한 가드를 강제한다
 * (정책서 §8.5.7). 이를 위해 JWT의 {@code gv_authCd}를 함께 운반하여 서비스 진입부에서 검증한다.
 */
public record LeaveDashboardListParam(
      String gvCmpnyCd
    , String gvAuthCd
    , String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String userNm
    , int page
    , int size
) {

    public static LeaveDashboardListParam from(LeaveDashboardListRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        LeaveDashboardListRequest req = (request == null) ? new LeaveDashboardListRequest() : request;
        int page = (req.getPage() == null) ? 1 : req.getPage();
        int size = (req.getSize() == null) ? 20 : req.getSize();
        // 하위부서 포함 플래그 정규화 (Y만 허용, 그 외/null은 N — attd08 패턴)
        String incSubNodeYn = "Y".equals(req.getIncSubNodeYn()) ? "Y" : "N";

        return new LeaveDashboardListParam(
              tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , req.getSiteCd()
            , req.getNodeCd()
            , incSubNodeYn
            , req.getUserNm()
            , page
            , size
        );
    }
}
