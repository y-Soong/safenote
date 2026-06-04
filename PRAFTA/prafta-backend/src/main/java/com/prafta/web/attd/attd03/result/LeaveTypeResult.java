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

    // prafta-044-FU: 관리자 부여 사용가능기간 편집 복원용 원본 컬럼 직접 노출
    // (기존 availTermType/availPeriod 는 목록 표시용 CASE/CONCAT 결과 — 병존 유지).
    // ⚠️ record 위치기반 매핑: SELECT 컬럼도 동일하게 '끝'에 추가(아래 3개) — 순서 1:1 정합.
    , String adminAvailTermType
    , String adminAvailFromDt
    , String adminAvailToDt
){

}
