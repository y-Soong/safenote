package com.prafta.app.safety.admin.application.query;

/**
 * H2 순회점검 상세 매퍼 쿼리 (사업장+체크포인트 스코프 + 월 필터 + 사진 FILE_TYPE).
 *
 * <p>fileType 은 일일점검 사진 타입 '001'(웹 InspectAnswerQuery 하드코딩과 동일).
 *    리소스 키(siteCd/chkLstType/chkptCd)는 CMPNY+SITE WHERE 로 스코프 강제(IDOR).
 */
public record InspectionDetailQuery(
      String gvCmpnyCd
    , String siteCd
    , String chkLstType
    , String chkptCd
    , String workMonth
    , String fileType
) {
}
