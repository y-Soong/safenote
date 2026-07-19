package com.prafta.web.chkLst.chkLst02.application.command;

import java.util.List;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst02.application.param.CopyChkptInspectItemParam;

/**
 * PRAFTA-SUBCON-T0-04: 타 사업장 점검문항 가져오기 커맨드.
 * 원본 문항 조회(selectCopySourceItemList)·정렬순서 append 기준(selectMaxSortIdx)에 사용.
 */
public record CopyChkptInspectItemCommand(
	String srcSiteCd
	, String dstSiteCd
	, String chkLstType
	, List<String> inspectItemCdList
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static CopyChkptInspectItemCommand from(CopyChkptInspectItemParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new CopyChkptInspectItemCommand(
        	param.srcSiteCd()
        	, param.dstSiteCd()
        	, param.chkLstType()
        	, param.inspectItemCdList()
        	, param.gvCmpnyCd()
        	, param.gvUserCd()
        );
    }
}
