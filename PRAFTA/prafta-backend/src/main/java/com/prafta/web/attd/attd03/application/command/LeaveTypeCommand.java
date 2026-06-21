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
    , Integer adminAvailMonths
    , String grantBaseType
    , Integer grantOffsetMonth
    , String grantAssignMmdd
    , String aprvUseYn
    , String evidenceYn
    , String evidenceGuideMsg
    , String gvCmpnyCd
    , String gvUserCd
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
		// 사용자 신청 날짜(MMDD)는 param 원본 그대로(정규화 미적용 경로),
		// 관리자 개월수도 param 원본 그대로(정규화 미적용 경로).
		return from(
			param
			, useUnitType
			, param.availFromDt()
			, param.availToDt()
			, param.adminAvailMonths()
		);
	}

	/**
	 * prafta-044-FU(검토 후속) + prafta-com-016-B(3-2): 사용가능기간 정규화 결과를 영속하기 위한 오버로드.
	 *
	 * <p>useUnitType 정규화 + 사용자 신청 날짜(availFrom/To, MMDD) 정규화 + 관리자 개월수
	 * (adminAvailMonths, '03'이 아니면 null 강제) 정규화를 함께 반영한다.
	 * 나머지 필드는 param 값을 그대로 사용한다.
	 */
	public static LeaveTypeCommand from(
			LeaveTypeParam param
			, String useUnitType
			, String availFromDt
			, String availToDt
			, Integer adminAvailMonths) {

		// leaveCd 미지정 오버로드는 param 원본 leaveCd 를 그대로 사용(수정 경로 호환).
		return from(
			param
			, useUnitType
			, availFromDt
			, availToDt
			, adminAvailMonths
			, param == null ? null : param.leaveCd()
		);
	}

	/**
	 * com-013-03(03-1): 신규 등록 시 서버에서 채번(FNC_CMM_SEQ_NEXTVAL)한 leaveCd 를
	 * INSERT 키로 영속하기 위한 오버로드. 기존엔 param.leaveCd()(신규=null)를 그대로
	 * 사용해 PK(LEAVE_CD) 가 null 로 들어가 INSERT 가 실패하던 결함을 해소한다.
	 * 수정 경로는 호출부에서 resolvedLeaveCd 에 기존 leaveCd 를 그대로 전달한다.
	 */
	public static LeaveTypeCommand from(
			LeaveTypeParam param
			, String useUnitType
			, String availFromDt
			, String availToDt
			, Integer adminAvailMonths
			, String resolvedLeaveCd) {

		if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new LeaveTypeCommand(
			resolvedLeaveCd
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
			, adminAvailMonths
			, param.grantBaseType()
			, param.grantOffsetMonth()
			, param.grantAssignMmdd()
			, param.aprvUseYn()
			, param.evidenceYn()
			, param.evidenceGuideMsg()
			// com-013-03(QA D-1): INSERT VALUES 절의 #{gvCmpnyCd}/#{gvUserCd} 바인딩을 위해
			// Command record 에 토큰 유래 값을 포함한다. 누락 시 MyBatis ReflectionException 으로
			// 신규/수정 upsert 가 전면 런타임 실패하던 결함(연차타입 등록·수정 불가)을 해소.
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
