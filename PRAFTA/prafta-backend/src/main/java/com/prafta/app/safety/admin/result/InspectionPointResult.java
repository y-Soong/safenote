package com.prafta.app.safety.admin.result;

/**
 * H1 순회점검 결과 리스트 행 VO (포인트별 집계).
 *
 * <p>웹 chkLst03 selectChkptInspectItemList 를 앱 단일 사업장 스코프로 포팅한 결과.
 *    매핑은 camelCase 위치 기반(SELECT 컬럼 순서 = 생성자 인자 순서).
 */
public record InspectionPointResult(
    String cmpnyCd
    , String siteCd
    , String siteNm
    , String chkptCd
    , String chkptNm
    , String chkLstType
    , Integer inspectDayCnt
    , Integer defectiveResultCnt
){
}
