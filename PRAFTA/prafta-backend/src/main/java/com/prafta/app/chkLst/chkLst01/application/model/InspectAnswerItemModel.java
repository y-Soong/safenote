package com.prafta.app.chkLst.chkLst01.application.model;

/**
 * prafta-036-B1: 점검 답변 항목 모델.
 * <p>multipart 'items' JSON 배열을 1건 단위로 파싱한 결과.
 * <p>FE(ChkLst.vue) 가 보내는 JSON 키와 1:1 호환 유지 (변경 금지).
 *   <ul>
 *     <li>itemCd: 점검항목코드</li>
 *     <li>fileName: 클라이언트 파일명(선택)</li>
 *     <li>answerDesc: 답변비고</li>
 *     <li>inspectValue: 답변(Y/N 등 SYS009)</li>
 *   </ul>
 */
public record InspectAnswerItemModel(
    String itemCd
    , String fileName
    , String answerDesc
    , String inspectValue
) {

}
