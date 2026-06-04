package com.prafta.web.attd.attd03.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd03.application.param.LeaveTypeParam;

public record LeaveTypeCommand(
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
    , String adminAvailTermType
    , String adminAvailFromDt
    , String adminAvailToDt
    , String grantBaseType
    , Integer grantOffsetMonth
    , String grantAssignMmdd
    , String aprvUseYn
    , String evidenceYn
    , String evidenceGuideMsg
){
	public static LeaveTypeCommand from(LeaveTypeParam param) {
		return from(param, param == null ? null : param.useUnitType());
	}

	/**
	 * prafta-044-1: 서버에서 정규화(화이트리스트 검증 + 자동부여 null 강제)한 useUnitType 을
	 * 영속하기 위한 오버로드. 다른 필드는 param 값을 그대로 사용한다.
	 */
	public static LeaveTypeCommand from(LeaveTypeParam param, String useUnitType) {
		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		// 날짜 필드는 param 원본 그대로(정규화 미적용 경로)
		return from(
			param
			, useUnitType
			, param.availFromDt()
			, param.availToDt()
			, param.adminAvailFromDt()
			, param.adminAvailToDt()
		);
	}

	/**
	 * prafta-044-FU(검토 후속): 사용가능기간 타입(availTermType / adminAvailTermType)이
	 * '03'(기간설정)이 아닐 때 from/to 를 서버에서 null 로 강제한 값을 영속하기 위한 오버로드.
	 *
	 * <p>useUnitType 정규화 + 날짜 4종(availFrom/To, adminAvailFrom/To) 정규화를 함께 반영한다.
	 * 나머지 필드는 param 값을 그대로 사용한다.
	 */
	public static LeaveTypeCommand from(
			LeaveTypeParam param
			, String useUnitType
			, String availFromDt
			, String availToDt
			, String adminAvailFromDt
			, String adminAvailToDt) {

		if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new LeaveTypeCommand(
			param.leaveCd()
			, param.leaveType()
			, param.grantType()
			, param.leaveNo()
			, param.leaveNm()
			, param.paidType()
			, param.leaveNatureType()
			, param.useYn()
			, param.leaveDesc()
			, param.maxAplyDays()
			, useUnitType
			, param.availTermType()
			, availFromDt
			, availToDt
			, param.adminAvailTermType()
			, adminAvailFromDt
			, adminAvailToDt
			, param.grantBaseType()
			, param.grantOffsetMonth()
			, param.grantAssignMmdd()
			, param.aprvUseYn()
			, param.evidenceYn()
			, param.evidenceGuideMsg()
		);
	}
}
