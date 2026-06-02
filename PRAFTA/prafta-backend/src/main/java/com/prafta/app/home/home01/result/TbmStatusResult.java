package com.prafta.app.home.home01.result;

/**
 * prafta-app-001: 오늘 TBM 세션 + 본인 참석 상태 조회 결과.
 * <p>매핑(AppHome01Mapper.selectTodayTbm):
 * <pre>
 *   S.STATUS_CD              AS sessionStatus
 *   DATE_FORMAT(S.OPENED_AT,'%H%i') AS openedTime  (HHMM, null 허용)
 *   S.TITLE                  AS title
 *   M.USER_NM                AS presenterName       (MANAGER_USER_CD → TB_USER)
 *   ATT.ENTRY_AT HHMM        AS myEntryTime          (null 허용)
 *   ATT.EXIT_AT 존재 여부     AS exitExists ('Y'/'N')
 *   ATT.COMPLETION_STATUS_CD AS completionStatusCd  (null 허용)
 *   ATT.ATTENDANCE_CD        AS attendanceCd        (참석 레코드 존재 판정용)
 * </pre>
 * 오늘 세션 자체가 없으면 결과 없음 → 서비스에서 hasToday=false / NONE 처리.
 */
public record TbmStatusResult(
    String sessionCd
    , String sessionStatus
    , String openedTime
    , String title
    , String presenterName
    , String myEntryTime
    , String exitExists
    , String completionStatusCd
    , String attendanceCd
) {
}
