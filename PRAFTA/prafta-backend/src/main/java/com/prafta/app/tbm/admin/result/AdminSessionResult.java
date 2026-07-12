package com.prafta.app.tbm.admin.result;

/**
 * T-A2 세션 단건 상세(헤더) 조회 결과.
 *
 * <p>비밀번호(entryPwd/exitPwd)는 매퍼에서 그대로 조회하되, 서비스가 상태/권한 게이트에 따라
 * 응답 노출 여부를 결정한다(OPENED/IN_PROGRESS + 관리자만).
 */
public record AdminSessionResult(
    String sessionCd
    , String cmpnyCd
    , String siteCd
    , String siteNm
    , String eduTypeCd
    , String title
    , String contentBody
    , String contentFormatCd
    , String statusCd
    , String statusNm
    , String entryPwd
    , String exitPwd
    , String managerUserCd
    , String managerUserNm
    , String managerGpsLat
    , String managerGpsLon
    , String gpsVerifyTypeCd
    , Integer gpsVerifyRadiusM
    , Integer eduMinutes        // 교육 인정시간(분, 1~60). 미설정 시 null
    , String gpsManualConfirmYn
    , String openedAt
    , String prepStartAt
    , String startedAt
    , String endedAt
    , String cancelledAt
    , String cancelReason
    , String insertNm
    , String insertDate
){
}
