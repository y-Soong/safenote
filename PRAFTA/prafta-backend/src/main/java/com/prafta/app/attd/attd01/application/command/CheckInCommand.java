package com.prafta.app.attd.attd01.application.command;

/**
 * prafta-app-003 A1: 셀프 출근 INSERT 커맨드 (TB_USER_ATTD_MGMT).
 *
 * <p>ATTD_ID 는 매퍼에서 selectAttdId 로 선채번하여 주입한다.
 *   CHECK_OUT_* 는 INSERT 시점에 NULL(퇴근은 별도 check-out 에서 채움).
 *   checkInMethod='01'(사용자등록, SYS031). 출근 시각은 서버 NOW() 기준 raw 실제 시각(표준화 미적용).
 *   INSERT_NO=userCd, DEL_YN='N'.
 */
public record CheckInCommand(
    String attdId
    , String cmpnyCd
    , String siteCd
    , String userCd
    , String workYmd       // YYYYMMDD (출근 대상 근무일)
    , String nodeCd        // 소속부서(JWT 또는 사용자 NODE_CD)
    , int workSeq          // 그 일자 기존 근태 개수 + 1
    , String checkInDate   // YYYYMMDD (서버 today)
    , String checkInTime   // HHMM (서버 NOW)
    , String checkInMethod // '01'
    , String deviceUuid    // prafta-com-003 D3: 출근 실행 디바이스UUID(CHECK_IN_DEVICE_UUID, nullable)
    , String insertNo      // userCd
) {
}
