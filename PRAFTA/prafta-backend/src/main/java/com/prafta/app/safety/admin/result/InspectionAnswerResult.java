package com.prafta.app.safety.admin.result;

/**
 * H2 순회점검 상세 답변 행 VO (일자별 항목 답변 + 불량 사진/비고).
 *
 * <p>웹 chkLst03 selectInspectAnswerList 를 앱에 포팅하되, 사진 경로는 평문 FILE_PATH 직노출 대신
 *    nearmiss 패턴(FNC_CMM_INFO_SRCH ... 'FILE_PATH')으로 서빙 경로를 해석한다(IDOR 결함 비상속).
 *
 * <p>inspectAnswerType 은 SQL CASE 로 'O'(양호) / 'X'(불량) 변환(OGNL Character 함정 회피).
 *    매핑은 camelCase 위치 기반(SELECT 컬럼 순서 = 생성자 인자 순서).
 */
public record InspectionAnswerResult(
    String cmpnyCd
    , String chkptCd
    , String inspectItemCd
    , String inspectItemSubj
    , String workDate
    , String inspectAnswerType
    , String answerDesc
    , String fileMgmtCd
    , String fileName
    , String filePath
){
}
