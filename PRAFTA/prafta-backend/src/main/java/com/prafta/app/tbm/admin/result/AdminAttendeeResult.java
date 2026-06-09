package com.prafta.app.tbm.admin.result;

/**
 * R3 출결 리스트 행(진행/종료/교육준비 화면 공용).
 *
 * <p>web Tbm04Mapper.selectSessionAttendances 축약 포팅(이벤트/이상신호/서명/타임라인 제외).
 * deptNm 은 정규직(REGULAR)의 소속 노드명(TB_SITE_NODE.NODE_NM), 일용직(DAILY)은 NULL.
 * exited 는 서버 산출(EXIT_AT IS NOT NULL). 이름은 web 규칙 준용(관리자 화면, 별도 마스킹 없음).
 *
 * <p>prafta-051 R-C(E12): 교육준비(PREP) 입실자 화면에서 GPS 이탈자 판단을 위해 입실 거리를
 * 추가 노출한다. distanceM=입실 시 개설지점과의 거리(m, ENTRY_DISTANCE_M). 대리입실(MANAGER_DIRECT)은
 * 거리검증을 하지 않아 distanceM=NULL. 보안 검토(R-C, 최소노출)에 따라 정밀 입실좌표(ENTRY_GPS_LAT/LON)는
 * 프론트가 사용하지 않아 응답에서 제외한다(이탈 판단은 distanceM 단일 기준).
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
    , Integer distanceM
){
}
