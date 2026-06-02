package com.prafta.web.baim.baim02.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim02.application.model.CompCmmCodeDModel;

public record CompCmmCodeDCommand(
		String chk
		, String cmpnyCd
		, String baimValCd
		, String baimValDCd
		, String baimValDNm
		, String sortIdx
		, String useYn
		, String valDInfo2
		, String valDInfo1
		, String valDDesc
		, String gvCmpnyCd
		, String gvUserCd
){
	public static CompCmmCodeDCommand from(CompCmmCodeDModel model) {

        // 1) 리스트 자체 검증
        if (model == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        
        return new CompCmmCodeDCommand(
        	model.chk()
        	, model.cmpnyCd()
        	, model.baimValCd()
        	, model.baimValDCd()
        	, model.baimValDNm()
        	, model.sortIdx()
        	, model.useYn()
        	, model.valDInfo2()
        	, model.valDInfo1()
        	, model.valDDesc()
        	, model.gvCmpnyCd()
        	, model.gvUserCd()
        );
    }
}
