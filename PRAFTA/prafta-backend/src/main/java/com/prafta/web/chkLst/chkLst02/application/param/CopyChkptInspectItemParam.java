package com.prafta.web.chkLst.chkLst02.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst02.dto.request.CopyChkptInspectItemRequest;

/**
 * PRAFTA-SUBCON-T0-04: 타 사업장 점검문항 가져오기 파라미터.
 */
public record CopyChkptInspectItemParam(
	String srcSiteCd
	, String dstSiteCd
	, String chkLstType
	, List<String> inspectItemCdList
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static CopyChkptInspectItemParam from(CopyChkptInspectItemRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        // 원본/대상 사업장이 같으면 가져오기 의미 없음(자기 복제 차단)
        if (request.getSrcSiteCd() != null && request.getSrcSiteCd().equals(request.getDstSiteCd()))
        	throw new ApiException(CommonErrorCode.COMMON_400_001, "원본 사업장과 대상 사업장이 같습니다.");

        return new CopyChkptInspectItemParam(
        	request.getSrcSiteCd()
        	, request.getDstSiteCd()
        	, request.getChkLstType()
        	, request.getInspectItemCdList()
        	, tokenInfo.gv_cmpnyCd()
        	, tokenInfo.gv_userCd()
        );
    }
}
