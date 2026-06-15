package com.prafta.web.tbm.tbm02.result;

/**
 * PRAFTA-APP-021-3b(W3 자동시작): 15분 자동 교육시작 배치의 시작 PUSH 통보 대상 키.
 *
 * <p>{@code bulkStartExpiredPrep} 와 동일 WHERE(OPENED + PREP_START_AT 15분 경과)로 일괄 전이
 * 직전에 포착한 세션 키 집합. {@code TbmEventNotiService.notifyTbmStarted} 입력
 * (cmpnyCd/siteCd/sessionCd/manager)에 1:1 매핑된다.
 */
public record AutoStartTargetResult(
    String cmpnyCd
    , String sessionCd
    , String siteCd
    , String managerUserCd
){
}
