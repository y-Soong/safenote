package com.prafta.common.cmm.push;

/**
 * 사용자 소속이동 예약 통보 PUSH 생산자 (PRAFTA-WEB_001-3, Terminal C).
 *
 * <p>{@code LeaveDirectSetNotiService}/{@code ApprovalResultNotiService} 와 동일한
 * afterCommit + REQUIRES_NEW 격리 패턴을 따른다. 예약 등록 트랜잭션이 정상 커밋된 뒤에만 outbox 를
 * 적재하며, 적재/발송 실패는 등록 본 흐름을 롤백하지 않는다(best-effort).
 *
 * <p>표시용 명칭(사업장/부서/근무타입)은 호출자(소속이동 서비스)가 스냅샷으로 전달한다 — 본 생산자는
 * 특정 도메인 매퍼에 의존하지 않는다(common.cmm.push 격리).
 */
public interface TransferReservedNotiService {

    /**
     * 진입부 — 본(등록) 트랜잭션 커밋 이후 1건 발송을 예약한다(afterCommit).
     *
     * @param cmpnyCd       회사 코드(스코프)
     * @param siteCd        outbox SITE_CD(이동 사업장)
     * @param targetUserCd  통보 대상(소속이동 대상자 본인)
     * @param reservationId 소속이동 예약 ID(멱등 DEDUP 키)
     * @param moveDateYmd   소속이동일(YYYYMMDD)
     * @param toSiteNm      이동 사업장명
     * @param toNodeNm      이동 부서명
     * @param defaultSchNm  기본 근무타입명(정규직, 없으면 null/blank — 일용직)
     * @param moveReason    소속이동 사유
     * @param actorUserCd   등록 수행자(master/hr) — 적재 INSERT_NO 용
     */
    void notifyTransferReserved(String cmpnyCd, String siteCd, String targetUserCd, String reservationId,
                                String moveDateYmd, String toSiteNm, String toNodeNm,
                                String defaultSchNm, String moveReason, String actorUserCd);

    /**
     * 실행부 — REQUIRES_NEW 새 트랜잭션 경계(자기 프록시 경유 호출). 직접 호출 금지.
     */
    void runTransferReservedOutbox(String cmpnyCd, String siteCd, String targetUserCd, String reservationId,
                                   String moveDateYmd, String toSiteNm, String toNodeNm,
                                   String defaultSchNm, String moveReason, String actorUserCd);
}
