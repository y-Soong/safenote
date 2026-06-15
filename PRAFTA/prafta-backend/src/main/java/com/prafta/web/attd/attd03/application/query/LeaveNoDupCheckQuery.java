package com.prafta.web.attd.attd03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd03.application.param.LeaveTypeParam;

public record LeaveNoDupCheckQuery(
	String leaveNo
	, String gvCmpnyCd
	, String leaveCd
){
	// prafta-app-026 검수정정(attd03/F-02): 번호 중복검사에서 자기 자신(leaveCd) 제외용.
	//   신규 채번 시에는 새로 발급한 leaveCd(아직 DB에 없음)를, 수정 시에는 기존 leaveCd 를 전달한다.
	public static LeaveNoDupCheckQuery from(LeaveTypeParam param, String leaveCd) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new LeaveNoDupCheckQuery(
        	param.leaveNo()
        	, param.gvCmpnyCd()
        	, leaveCd
        );
	}
	
}
