package com.prafta.web.chkLst.chkLst02.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst02.application.model.ChkptInspectItemModel;

public record ChkptInspectItemCommand(
	String cmpnyCd
	, String siteCd
	, String chkLstType
	, String inspectItemCd
	, String inspectItemSubj
	, int sortIdx
	, String strDate
	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static ChkptInspectItemCommand from(ChkptInspectItemModel model) {

        if (model == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ChkptInspectItemCommand(
    		model.cmpnyCd()
    		, model.siteCd()
    		, model.chkLstType()
    		, model.inspectItemCd()
    		, model.inspectItemSubj()
    		, model.sortIdx()
    		, model.strDate()
    		, model.useYn()
    		, model.gvCmpnyCd()
    		, model.gvUserCd()
        );
    }
}
