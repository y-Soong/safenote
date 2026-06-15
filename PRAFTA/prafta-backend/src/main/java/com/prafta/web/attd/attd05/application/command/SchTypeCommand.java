package com.prafta.web.attd.attd05.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.application.model.SchTypeModel;

public record SchTypeCommand(
	String siteCd
	, String userCd
	, String workYmd
	, String workPlanCd
	, String gvCmpnyCd
	, String gvUserCd
) {
	
	public static SchTypeCommand from(SchTypeModel model) {

        if (model == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchTypeCommand(
    		model.siteCd()
    		, model.userCd()
    		, model.workYmd()
    		, model.workPlanCd()
    		, model.gvCmpnyCd()
    		, model.gvUserCd()
		);
    }

	/**
	 * prafta-com-008-E-6: 근무계획코드를 override 하여 생성.
	 * 연차 셀이어도 work_plan 에는 LEAVE_CD 대신 사용자 기본 근무타입(SCH_CD)을 기록하기 위함.
	 */
	public static SchTypeCommand from(SchTypeModel model, String overrideWorkPlanCd) {

        if (model == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchTypeCommand(
    		model.siteCd()
    		, model.userCd()
    		, model.workYmd()
    		, overrideWorkPlanCd
    		, model.gvCmpnyCd()
    		, model.gvUserCd()
		);
    }
}
