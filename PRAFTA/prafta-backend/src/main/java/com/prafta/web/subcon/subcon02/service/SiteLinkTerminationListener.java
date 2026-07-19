package com.prafta.web.subcon.subcon02.service;

import com.prafta.web.subcon.subcon02.result.SiteLinkRaw;

/**
 * 사업장 링크 해지 시 산하 연동 자동 독립화 확장점(PRAFTA-SUBCON-T6-08).
 *
 * <p>T1 {@code RelationTerminationHandler} 계약과 동형이다. 구현체 빈이 없어도 기동/동작에 지장이
 * 없도록 호출부는 {@code ObjectProvider} 로 수집한다(후보 0개 = no-op).
 *
 * <p>호출 경로 2곳(개별 해지 / 관계 해지 캐스케이드) 모두 해지 확정 직후 <b>동일 트랜잭션</b>에서
 * 호출된다 — 리스너 예외 시 해지 전체가 롤백된다(반쪽 해지 방지).
 */
public interface SiteLinkTerminationListener {

    /**
     * 사업장 링크 해지 확정 직후 호출.
     *
     * @param link         해지된 링크 원시행(SRC/DST 좌표 — 조건부 UPDATE 성공으로 당사자성 기증명)
     * @param actionUserCd 해지 행위자 사용자코드(JWT 도출 또는 시스템 처리자)
     */
    void onSiteLinkTerminated(SiteLinkRaw link, String actionUserCd);
}
