package com.prafta.web.chkLst.chkLst01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst01.application.model.ChkptInfoModel;

public record ChkptInfoCommand(
	String chk
	, String siteCd
	, String siteNm
	, String chkLstType
	, String chkptCd
	, String chkptNm
	, String chkptDesc
	, String useYn
	, String mgmtUserCd
	, String mgmtUserNm
	, String gvCmpnyCd
	, String gvUserCd
){
	public static ChkptInfoCommand from(ChkptInfoModel model) {

        if (model == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ChkptInfoCommand(
        	model.chk()
        	, model.siteCd()
        	, model.siteNm()
        	, model.chkLstType()
        	, model.chkptCd()
        	, model.chkptNm()
        	, model.chkptDesc()
        	, model.useYn()
        	, model.mgmtUserCd()
        	, model.mgmtUserNm()
        	, model.gvCmpnyCd()
        	, model.gvUserCd()
        );
    }
}
