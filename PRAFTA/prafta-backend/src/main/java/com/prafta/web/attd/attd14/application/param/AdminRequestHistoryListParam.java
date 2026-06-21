package com.prafta.web.attd.attd14.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.attd14.dto.request.AdminRequestHistoryListRequest;

/**
 * 관리자 발신 연차 변경 요청 이력 목록 조회 Param (prafta-com-016-H).
 *
 * <p>회사/권한/사용자 식별은 토큰에서만 도출한다(IDOR). siteCd 는 토큰 사업장과 일치 강제(attd13 ChangeRequestListParam 계승).
 * 페이징(page/size) 정규화 + OFFSET 산출. INITIATOR_TYPE 은 서비스에서 'ADMIN' 고정.
 */
public record AdminRequestHistoryListParam(
      String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String userNm
    , String reqType
    , String reqStatus
    , String fromDate
    , String toDate
    , int page
    , int size
    , int offset
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
) {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public static AdminRequestHistoryListParam from(AdminRequestHistoryListRequest request, TokenInfo tokenInfo) {
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
        boolean siteWide = AuthRoleUtils.isManager(tokenInfo.gv_authCd()); // master/hr = 회사 전사(attd13 D1+D3)
        if (siteWide) {
            // master/hr: 사업장 미지정이면 회사 전사(전체 사업장)로 빈값 유지. 지정 사업장은 임의 허용(전사 권한).
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

        int page = (request.getPAGE() == null || request.getPAGE() < 1) ? 1 : request.getPAGE();
        int size = (request.getPAGE_SIZE() == null || request.getPAGE_SIZE() < 1) ? DEFAULT_SIZE : request.getPAGE_SIZE();
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        int offset = (page - 1) * size;

        return new AdminRequestHistoryListParam(
              siteCd
            , request.getNODE_CD()
            , "Y".equals(request.getINC_SUB_NODE_YN()) ? "Y" : "N"
            , request.getUSER_NM()
            , request.getREQ_TYPE()
            , request.getREQ_STATUS()
            , normalizeYmd(request.getFROM_DATE())
            , normalizeYmd(request.getTO_DATE())
            , page
            , size
            , offset
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_userCd()
        );
    }

    /** YYYY-MM-DD/YYYYMMDD 등 구분자 제거 후 8자리 YYYYMMDD 만 허용, 그 외는 null(필터 미적용). */
    private static String normalizeYmd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return digits.length() == 8 ? digits : null;
    }
}
