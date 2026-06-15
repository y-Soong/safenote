package com.prafta.web.attd.attd13.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.attd13.dto.request.ChangeRequestListRequest;

/**
 * 연차 변경 요청 목록 조회 Param (PRAFTA-COM-008-C).
 *
 * <p>회사/권한/사용자 식별은 토큰에서만 도출한다. siteCd 는 토큰 사업장과 일치 강제(IDOR 가드).
 */
public record ChangeRequestListParam(
      String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String userNm
    , String reqStatus
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
) {
    public static ChangeRequestListParam from(ChangeRequestListRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()
                || tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String siteCd = request.getSITE_CD();
        boolean siteWide = AuthRoleUtils.isManager(tokenInfo.gv_authCd()); // master/hr = 회사 전사(D1+D3)
        if (siteWide) {
            // master/hr: 사업장 미지정이면 회사 전사(전체 사업장)로 빈값 유지. 지정 사업장은 임의 허용(전사 권한).
            //   (siteCd 가 빈값이면 매퍼가 사업장 필터를 적용하지 않는다.)
            if (siteCd != null && siteCd.isBlank()) {
                siteCd = null;
            }
        } else {
            // 노드 관리자: siteCd 미지정 시 토큰 사업장으로, 지정 시 토큰 사업장과 일치 강제(cross-site IDOR 차단)
            if (siteCd == null || siteCd.isBlank()) {
                siteCd = tokenInfo.gv_siteCd();
            } else if (!siteCd.equals(tokenInfo.gv_siteCd())) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
        }
        return new ChangeRequestListParam(
              siteCd
            , request.getNODE_CD()
            , "Y".equals(request.getINC_SUB_NODE_YN()) ? "Y" : "N"
            , request.getUSER_NM()
            , request.getREQ_STATUS()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_userCd()
        );
    }
}
