package com.prafta.app.attd.attd01.application.command;

import java.math.BigDecimal;

/**
 * prafta-app check-out: 퇴근 GPS INSERT 커맨드 (TB_USER_ATTD_GPS).
 *
 * <p>GPS_ID 는 매퍼에서 selectGpsId 로 선채번하여 주입한다.
 *   GPS_INFO_TYPE='02'(퇴근, SYS028). SITE_CD 는 출근 레코드의 SITE_CD(세션과 동일성 검증 후)를 사용한다.
 *   API_CALL_DATE/TIME 은 서버 시각(YYYYMMDD / HHmmss). INSERT_NO=userCd.
 */
public record CheckOutGpsCommand(
    String gpsId
    , String cmpnyCd
    , String attdId
    , String siteCd
    , String userCd
    , String gpsInfoType   // '02' (퇴근)
    , BigDecimal lat
    , BigDecimal lon
    , BigDecimal accuracy
    , String apiCallDate   // YYYYMMDD
    , String apiCallTime   // HHmmss
    , String isMocked      // 'Y'/'N'
    , String ipAddr
    , String offsiteReason // prafta-app-008: 외근 사유(외근 GPS 행에만 저장)
    , String insertNo      // userCd
) {
}
