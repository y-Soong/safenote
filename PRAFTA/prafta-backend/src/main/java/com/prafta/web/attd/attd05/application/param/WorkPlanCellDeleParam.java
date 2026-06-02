package com.prafta.web.attd.attd05.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.application.model.WorkPlanCellDeleModel;
import com.prafta.web.attd.attd05.dto.request.WorkPlanCellDeleRequst;

/**
 * PRAFTA-041 - 근무계획 셀 단위 삭제 파라미터.
 */
public record WorkPlanCellDeleParam(
	List<WorkPlanCellDeleModel> workPlanCellDeleModelList
	, String gvAuthCd
) {

	public static WorkPlanCellDeleParam from(List<WorkPlanCellDeleRequst> requests, TokenInfo tokenInfo) {

        if (requests == null || tokenInfo == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        List<WorkPlanCellDeleModel> models = requests.stream()
    		.map(req -> {
    			return new WorkPlanCellDeleModel(
    				req.getSiteCd()
    				, req.getUserCd()
    				, req.getWorkYmd()
    				, tokenInfo.gv_cmpnyCd()
    				, tokenInfo.gv_userCd()
				);
    		})
        	.toList();

        return new WorkPlanCellDeleParam(models, tokenInfo.gv_authCd());
    }
}
