package com.prafta.web.attd.attd03.result;

public record LeaveTypeResult(
	String cmpnyCd
    , String leaveCd
    , String leaveNo
    , String leaveNm

    , String leaveType
    , String leaveTypeNm

    , String grantType
    , String grantTypeNm

    , String paidType
    , String paidTypeNm

    , String leaveNatureType
    , String leaveNatureTypeNm

    , Integer leaveDays

    , String useUnitType
    , String useUnitTypeNm

    , String availTermType
    , String availTermTypeNm

    , String availPeriod

    , String useYn
    , String leaveDesc
    
    , String grantBaseType
    , String grantBaseTypeNm

    , String grantOffsetMonth

    , String grantAssignMmdd

    , String aprvUseYn
    , String evidenceYn
    , String evidenceGuideMsg
){

}
