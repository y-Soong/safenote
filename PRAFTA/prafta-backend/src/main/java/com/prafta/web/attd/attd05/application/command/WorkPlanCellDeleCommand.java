package com.prafta.web.attd.attd05.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.application.model.WorkPlanCellDeleModel;

/**
 * PRAFTA-041 - 근무계획 셀 단위 삭제 커맨드(PK 완전 일치 DELETE).
 */
public record WorkPlanCellDeleCommand(
	String siteCd
	, String userCd
	, String workYmd
	, String gvCmpnyCd
	, String gvUserCd
) {

	public static WorkPlanCellDeleCommand from(WorkPlanCellDeleModel model) {

        if (model == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new WorkPlanCellDeleCommand(
    		model.siteCd()
    		, model.userCd()
    		, model.workYmd()
    		, model.gvCmpnyCd()
    		, model.gvUserCd()
		);
    }
}
