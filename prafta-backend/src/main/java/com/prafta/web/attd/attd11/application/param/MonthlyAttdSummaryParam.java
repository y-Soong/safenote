package com.prafta.web.attd.attd11.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd11.dto.request.MonthlyAttdSummaryRequest;

/**
 * PRAFTA-034 - Attd_11 월별 사용자 근태 판정 조회 파라미터.
 *
 * attd07 MonthlyAttdListParam 패턴을 따른다. siteCd 는 세션 고정 사업장(JWT gv_siteCd)과
 * 일치해야 하며(cross-site IDOR 가드), 권한 게이팅(PRAFTA-028/decisions §7)은 서버
 * 서비스 진입부에서 canManageNode 로 강제한다(master/hr 또는 노드 관리자만 허용).
 * nodeCd 가 비어 있으면 사업장 전체 스코프이며, 비 master/hr 은 canManageNode 에서 차단된다.
 */
public record MonthlyAttdSummaryParam(
        String workYm
        , String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String userNm
        , String gvCmpnyCd
        , String gvAuthCd
        , String gvUserCd
) {
    public static MonthlyAttdSummaryParam from(MonthlyAttdSummaryRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (request.getWorkYm() == null || request.getWorkYm().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        // cross-site IDOR 가드 — 요청 siteCd 가 세션 고정 사업장(JWT gv_siteCd)과 다르면 거부
        if (!request.getSiteCd().equals(tokenInfo.gv_siteCd()))
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new MonthlyAttdSummaryParam(
                request.getWorkYm()
                , request.getSiteCd()
                , request.getNodeCd()
                , request.getIncSubNodeYn() == null ? "N" : request.getIncSubNodeYn()
                , request.getUserNm()
                , tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_authCd()
                , tokenInfo.gv_userCd()
        );
    }
}
