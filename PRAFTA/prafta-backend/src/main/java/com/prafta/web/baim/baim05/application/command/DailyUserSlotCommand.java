package com.prafta.web.baim.baim05.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.application.param.LinkPoliciesParam;

public record DailyUserSlotCommand(
	String siteCd
	, int slotNo
	, String slotType
	, String useYn
	, String slotStatus
	, String nodeCd			// 신규 슬롯(INSERT)에 부여할 기본 소속부서. 기존 슬롯(ON DUPLICATE)은 미변경.
	, String gvCmpnyCd
	, String gvUserCd
){
	public static DailyUserSlotCommand from(LinkPoliciesParam param, int slotNo, String useYn, String nodeCd) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (useYn == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new DailyUserSlotCommand(
        		param.siteCd()
        		, slotNo
        		, "01"					// 01:일반사용자, 02:QR사용자
        		, useYn
        		, "01"					// 01:비점유중, 02:점유중
        		, nodeCd				// 신규 슬롯 기본 소속부서(사업장 최상단 노드). null 가능.
        		, param.gvCmpnyCd()
        		, param.gvUserCd()
        );
    }
}
