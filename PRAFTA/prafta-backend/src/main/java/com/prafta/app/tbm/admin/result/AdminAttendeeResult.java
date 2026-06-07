package com.prafta.app.tbm.admin.result;

/**
 * R3 출결 리스트 행(진행/종료 화면 공용).
 *
 * <p>web Tbm04Mapper.selectSessionAttendances 축약 포팅(이벤트/이상신호/서명/타임라인 제외).
 * deptNm 은 정규직(REGULAR)의 소속 노드명(TB_SITE_NODE.NODE_NM), 일용직(DAILY)은 NULL.
 * exited 는 서버 산출(EXIT_AT IS NOT NULL). 이름은 web 규칙 준용(관리자 화면, 별도 마스킹 없음).
 */
public record AdminAttendeeResult(
    String attendanceCd
    , String userNm
    , String userTypeCd
    , String deptNm
    , String entryAt
    , boolean exited
    , String exitAt
    , String completionStatusCd
){
}
