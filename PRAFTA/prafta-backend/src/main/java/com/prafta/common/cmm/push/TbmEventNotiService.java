package com.prafta.common.cmm.push;

/**
 * TBM 교육 시작/종료 통보(W3) PUSH 생산자(outbox PENDING 적재) (PRAFTA-APP-021-3b).
 *
 * <p>교육 시작(IN_PROGRESS)·종료(COMPLETED) 전이 직후, 해당 세션에 <b>실제 입실한 참석자(enter)</b>
 * 전원에게 1건씩 통보한다. afterCommit + REQUIRES_NEW 격리로 전이 본 흐름을 절대 롤백시키지 않는다.
 */
public interface TbmEventNotiService {

    /**
     * TBM 교육 시작 통보(입실 참석자 전원). 전이 트랜잭션 커밋 이후 적재.
     *
     * @param cmpnyCd     회사 코드
     * @param siteCd      사업장 코드(세션 SITE_CD)
     * @param sessionCd   TBM 세션 코드(대상 조회 + dedupKey)
     * @param actorUserCd 전이 수행자(INSERT_NO)
     */
    void notifyTbmStarted(String cmpnyCd, String siteCd, String sessionCd, String actorUserCd);

    /** TBM 교육 종료 통보(입실 참석자 전원). 전이 트랜잭션 커밋 이후 적재. */
    void notifyTbmCompleted(String cmpnyCd, String siteCd, String sessionCd, String actorUserCd);

    // ── 실행부: afterCommit 콜백이 프록시 경유로 호출하는 REQUIRES_NEW 경계 메서드 ──

    /** 교육 시작 outbox 적재(REQUIRES_NEW). 콜백 전용 — 직접 호출 금지. */
    void runTbmStartedOutbox(String cmpnyCd, String siteCd, String sessionCd, String actorUserCd);

    /** 교육 종료 outbox 적재(REQUIRES_NEW). 콜백 전용 — 직접 호출 금지. */
    void runTbmCompletedOutbox(String cmpnyCd, String siteCd, String sessionCd, String actorUserCd);
}
