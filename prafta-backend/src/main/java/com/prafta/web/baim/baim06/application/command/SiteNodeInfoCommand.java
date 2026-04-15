package com.prafta.web.baim.baim06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.application.model.SiteNodeModel;

public record SiteNodeInfoCommand(
	String siteCd
	, String nodeCd
	, String nodeNm
	, String nodeType
	, String parentNodeCd
	, String selfAttdApprvYn
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SiteNodeInfoCommand from(SiteNodeModel model) {

        if (model == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteNodeModel");

        return new SiteNodeInfoCommand(
        	model.siteCd()
        	, model.nodeCd()
        	, model.nodeNm()
        	, model.nodeType()
        	, model.parentNodeCd()
        	, model.selfAttdApprvYn()
        	, model.gvCmpnyCd()
        	, model.gvUserCd()
        );        
    }
}
