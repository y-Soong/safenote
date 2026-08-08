package com.prafta.web.attd.attd01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.dto.request.AssignedUsersRequest;

/**
 * F-12-2: 근무타입별 배정현황 조회 파라미터.
 *
 * <p>schCd 는 경로변수, siteCd 는 필수 쿼리파라미터(TB_SCH_MGMT PK 가 CMPNY_CD+SITE_CD+SCH_CD
 * 복합키라 schCd 만으로는 유일하지 않다). gvAuthCd/gvUserCd/gvSiteCd 는 SiteAccessService
 * cross-site IDOR 가드(assertSiteAccess)에 사용한다.
 *
 * <p>보안수정(security High): gvNodeCd 는 canManageNode 부서 스코프 게이트(Attd_11/Attd_16 전례)에
 * 사용한다. 이 화면은 nodeCd 를 요청 파라미터로 받지 않으므로(요청자가 임의로 넓힐 수 없도록),
 * 세션 클레임(gv_nodeCd)을 조회 범위의 앵커로 강제한다.
 */
public record AssignedUsersParam(
	String schCd
	, String siteCd
	, String gvCmpnyCd
	, String gvAuthCd
	, String gvUserCd
	, String gvSiteCd
	, String gvNodeCd
){
	public static AssignedUsersParam from(String schCd, AssignedUsersRequest request, TokenInfo tokenInfo) {

        if (request == null || tokenInfo == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (schCd == null || schCd.isBlank())
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty())
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AssignedUsersParam(
        		schCd
        		, request.getSiteCd()
        		, tokenInfo.gv_cmpnyCd()
        		, tokenInfo.gv_authCd()
        		, tokenInfo.gv_userCd()
        		, tokenInfo.gv_siteCd()
        		, tokenInfo.gv_nodeCd()
        );
	}
}
