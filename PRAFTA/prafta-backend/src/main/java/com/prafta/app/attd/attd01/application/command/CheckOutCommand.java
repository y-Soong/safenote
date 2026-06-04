package com.prafta.app.attd.attd01.application.command;

/**
 * prafta-app check-out: 출근 레코드의 퇴근 채움 UPDATE 커맨드.
 *
 * <p>대상 ATTD_ID 의 CHECK_OUT_DATE/TIME/METHOD 를 채운다. 동시성 가드를 위해
 *   WHERE 절에 CHECK_OUT_TIME IS NULL 을 두고(이미 퇴근됐으면 0건), CMPNY_CD/USER_CD 도 함께 건다.
 *
 * <p>checkOutMethod='01'(사용자등록, SYS031). 퇴근 시각은 서버 NOW() 기준 raw 실제 시각(표준화 미적용).
 */
public record CheckOutCommand(
    String attdId
    , String cmpnyCd
    , String userCd
    , String checkOutDate   // YYYYMMDD (서버 today)
    , String checkOutTime   // HHMM (서버 NOW)
    , String checkOutMethod // '01'
    , String deviceUuid     // prafta-com-003 D3: 퇴근 실행 디바이스UUID(CHECK_OUT_DEVICE_UUID, nullable)
) {
}
