package com.prafta.web.attd.attd03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd03.dto.request.LeaveTypeRequest;

public record LeaveTypeParam(
	    String leaveCd
	    , String leaveType
	    , String grantType
	    , String leaveNo
	    , String leaveNm
	    , String paidType
	    , String leaveNatureType
	    , String useYn
	    , String leaveDesc
	    , Integer maxAplyDays
	    , String useUnitType
	    , String availTermType
	    , String availFromDt
	    , String availToDt
	    , Integer grantDays
	    , String adminAvailTermType
	    , String adminAvailFromDt
	    , String adminAvailToDt
	    , String grantBaseType
	    , Integer grantOffsetMonth
	    , String aprvUseYn
	    , Integer aprvStepCnt
	    , String hrFinalAprvYn
	    , String evidenceYn
	    , String evidenceGuideMsg
	    , String gvCmpnyCd
	    , String gvUserCd
	) {
	    public static LeaveTypeParam from(LeaveTypeRequest request, TokenInfo tokenInfo) {

	        if (request == null)
	            throw new ApiException(CommonErrorCode.COMMON_400_001);

	        return new LeaveTypeParam(
	            request.getLeaveCd()
	            , request.getLeaveType()
	            , request.getGrantType()
	            , request.getLeaveNo()
	            , request.getLeaveNm()
	            , request.getPaidType()
	            , request.getLeaveNatureType()
	            , request.getUseYn()
	            , request.getLeaveDesc()
	            , request.getMaxAplyDays()
	            , request.getUseUnitType()
	            , request.getAvailTermType()
	            , request.getAvailFromDt()
	            , request.getAvailToDt()
	            , request.getGrantDays()
	            , request.getAdminAvailTermType()
	            , request.getAdminAvailFromDt()
	            , request.getAdminAvailToDt()
	            , request.getGrantBaseType()
	            , request.getGrantOffsetMonth()
	            , request.getAprvUseYn()
	            , request.getAprvStepCnt()
	            , request.getHrFinalAprvYn()
	            , request.getEvidenceYn()
	            , request.getEvidenceGuideMsg()
	            , tokenInfo.gv_cmpnyCd()
	            , tokenInfo.gv_userCd()
	        );
	    }
	}