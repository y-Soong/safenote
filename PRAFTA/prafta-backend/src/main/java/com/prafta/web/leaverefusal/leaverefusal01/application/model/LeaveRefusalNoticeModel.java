package com.prafta.web.leaverefusal.leaverefusal01.application.model;

/**
 * 노무수령거부 통지 대상 1건 모델 (PRAFTA-COM-001 기능1).
 *
 * <p>List 요청을 param 으로 변환할 때 회사 스코프(gvCmpnyCd)를 각 대상에 결합한 캐노니컬 단위.
 * 식별자(gvCmpnyCd/gvUserCd)는 JWT(TokenInfo) 출처만 사용한다.
 */
public record LeaveRefusalNoticeModel(
        String gvCmpnyCd,
        String siteCd,
        String userCd,
        String targetYmd,
        String gvUserCd
) {
}
