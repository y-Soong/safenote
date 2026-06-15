package com.prafta.common.cmm.push;

/**
 * 결재 결과 통보(W2) PUSH 생산자(outbox PENDING 적재) (PRAFTA-APP-021-3a).
 *
 * <p>연차/근태/초과근무 보정 요청의 승인·반려 결과를 <b>신청자 본인 1인</b>에게 통보한다.
 * 연차(web LeaveFlowServiceImpl)·근태/OT 보정(web Attd07ServiceImpl) 양쪽 결재 처리부가 공유한다.
 * 앱이 web 을 직접 호출하지 않는 원칙과 무관하게 본 서비스는 공용 영역(common.cmm.push)에 둔다.
 *
 * <p><b>트랜잭션 격리</b>: {@code AttdApprovalNotiServiceImpl} 와 동일하게 본 결재 트랜잭션에
 * outbox INSERT 를 직접 실행하지 않고 커밋 이후(afterCommit)로 등록하며, 실제 적재는
 * {@code REQUIRES_NEW} 새 트랜잭션에서 수행한다. 적재 실패가 결재 본 흐름을 절대 롤백시키지 않는다.
 */
public interface ApprovalResultNotiService {

    /**
     * 연차 결재 결과 통보(승인/반려). 신청자 본인에게 1건.
     *
     * @param cmpnyCd     회사 코드
     * @param siteCd      사업장 코드(신청 요청의 SITE_CD)
     * @param applicantUserCd 신청자(수신 대상) 코드
     * @param reqId       요청 ID(라우팅 키 + dedupKey)
     * @param approved    true=승인 / false=반려
     * @param actorUserCd 처리자(INSERT_NO 기록용)
     */
    void notifyLeaveResult(String cmpnyCd, String siteCd, String applicantUserCd,
                           String reqId, boolean approved, String actorUserCd);

    /**
     * 근태/초과근무 보정 결재 결과 통보(승인/반려). 신청자 본인에게 1건.
     */
    void notifyAttdResult(String cmpnyCd, String siteCd, String applicantUserCd,
                          String reqId, boolean approved, String actorUserCd);

    // ── 실행부: afterCommit 콜백이 프록시 경유로 호출하는 REQUIRES_NEW 경계 메서드 ──

    /** 연차 결과 outbox 적재(REQUIRES_NEW). 콜백 전용 — 직접 호출 금지. */
    void runLeaveResultOutbox(String cmpnyCd, String siteCd, String applicantUserCd,
                              String reqId, boolean approved, String actorUserCd);

    /** 근태/OT 결과 outbox 적재(REQUIRES_NEW). 콜백 전용 — 직접 호출 금지. */
    void runAttdResultOutbox(String cmpnyCd, String siteCd, String applicantUserCd,
                             String reqId, boolean approved, String actorUserCd);
}
