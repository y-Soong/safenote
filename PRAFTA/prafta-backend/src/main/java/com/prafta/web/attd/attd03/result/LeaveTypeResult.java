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
    // ⚠️ record 위치기반 매핑: SELECT 컬럼도 동일하게 '끝'에 추가(아래 2개) — 순서 1:1 정합.
    // prafta-com-016-B(3-2): 관리자 부여 '03' 기간설정을 절대 날짜 → "부여일로부터 N개월"로 변경.
    //   기존 adminAvailFromDt/ToDt(절대 날짜) 복원 필드를 adminAvailMonths(Integer)로 대체.
    , String adminAvailTermType
    , Integer adminAvailMonths
){

}
