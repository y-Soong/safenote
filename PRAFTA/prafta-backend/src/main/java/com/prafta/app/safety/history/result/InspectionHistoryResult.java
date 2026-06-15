package com.prafta.app.safety.history.result;

/**
 * 내 안전활동 이력 — 순회점검 본인 행 VO (prafta-app-025 J1-10 B-6).
 *
 * <p>본인 점검 = TB_CHKPT_INSPECT_ANSWER.INSERT_NO = JWT gv_userCd. 항목 평면 나열(occurredAt=WORK_DATE).
 *    사진 경로는 평문 FILE_PATH 직노출 대신 FNC_CMM_INFO_SRCH('FILE_PATH') 로 해석(J1-6 동형).
 *    inspectAnswerType 은 SQL CASE 로 'O'(양호)/'X'(불량) 변환(OGNL Character 함정 회피).
 *
 * <p>매핑은 위치 기반(SELECT 컬럼 순서 = 생성자 인자 순서) — 순서 변경 시 SQL 동기 필수.
 */
public record InspectionHistoryResult(
      String chkptCd
    , String chkptNm
    , String inspectItemCd
    , String inspectItemSubj
    , String workDate          // YYYYMMDD (정렬/표시용 원본)
    , String occurredDate      // YYYY-MM-DD (병합 정렬 키)
    , String inspectAnswerType // 'O' | 'X'
    , String answerDesc
    , String fileMgmtCd
    , String filePath
) {
}
