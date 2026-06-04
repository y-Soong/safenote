package com.prafta.app.req.req09.service;

/**
 * 근태 요청 결재 PUSH 생산자(outbox PENDING 적재) (PRAFTA-APP-009-2).
 *
 * <p>연차 {@code LeaveApprovalNotiService} 미러. 근태 신청 hook(결재 분기 서비스)에서 호출된다.
 * 호출부도 try-catch 로 격리하지만, 본 구현도 내부에서 예외를 흡수(로그만)하여 근태 신청 본
 * 흐름에 절대 영향을 주지 않는다. {@code @Transactional} 미부여(신청 흐름 트랜잭션 내부 호출).
 *
 * <p>본문의 신청자명은 평문 {@code USER_NM} 조회값이다(복호화 호출 없음). DATA_PAYLOAD 에는
 * 평문 PII 를 넣지 않고 라우팅 키만 직렬화한다.
 *
 * <p><b>트랜잭션 격리(PRAFTA-APP-009-001 보안 Medium):</b> {@code notify*} 는 신청 본
 * 트랜잭션에 참여하지 않고, outbox 적재를 본 트랜잭션 <i>커밋 이후</i>({@code afterCommit})로
 * 분리한다. 따라서 dedupKey UNIQUE 충돌 등 적재 중 DB 예외가 발생해도 이미 커밋된 요청+결재라인
 * 을 롤백시키지 못한다({@code UnexpectedRollbackException} 전파 차단). 활성 트랜잭션이 없으면
 * (테스트/배치) 즉시 실행 폴백. 실제 적재({@code runTurnOutbox}/{@code runRequestOutbox})는
 * {@code REQUIRES_NEW} 새 트랜잭션 경계에서 수행된다.
 */
public interface AttdApprovalNotiService {

    /**
     * 'N' 결재라인의 첫 수동 단계(차례 도래) 결재자 1인에게 PUSH 적재(NOTI_TYPE='ATTD_APPROVAL_TURN').
     * dedupKey = "ATTD_TURN_" + reqId + "_" + approvalStep. 결재자가 null/blank 면 적재 생략.
     *
     * @param cmpnyCd         회사 코드
     * @param siteCd          사업장 코드
     * @param applicantUserCd 신청자 사용자 코드(본문 신청자명 합성용)
     * @param reqId           요청 ID(tb_user_attd_req)
     * @param approvalStep    차례가 도래한 결재 단계(1-based)
     * @param approverUserCd  차례 도래 결재자 사용자 코드(수신 대상)
     * @param insertNo        적재자(보통 신청자)
     */
    void notifyAttdApprovalTurn(String cmpnyCd, String siteCd, String applicantUserCd,
                                String reqId, int approvalStep, String approverUserCd,
                                String insertNo);

    /**
     * 'Y' 자체근태승인 요청 — 신청자 소속 노드 Main/Sub 관리자 전원(자기 제외)에게 PUSH 적재
     * (NOTI_TYPE='ATTD_APPROVAL_REQUEST'). dedupKey = "ATTD_REQ_" + reqId + "_" + targetUserCd.
     * 동일 키 중복 적재는 흡수(멱등).
     *
     * @param cmpnyCd         회사 코드
     * @param siteCd          사업장 코드
     * @param applicantUserCd 신청자 사용자 코드(노드 관리자 조회 기준 + 본문 합성)
     * @param reqId           요청 ID(tb_user_attd_req)
     * @param insertNo        적재자(보통 신청자)
     */
    void notifyAttdApprovalRequest(String cmpnyCd, String siteCd, String applicantUserCd,
                                   String reqId, String insertNo);

    /**
     * 차례 도래 outbox 적재 실행부 — 본 트랜잭션 커밋 이후 {@code REQUIRES_NEW} 새 트랜잭션에서
     * 호출된다. 직접 호출하지 말고 {@link #notifyAttdApprovalTurn} 를 통해 afterCommit 으로 진입한다.
     * (인터페이스에 노출하는 이유: 자기 프록시 경유로 {@code @Transactional} 을 적용하기 위함.)
     */
    void runTurnOutbox(String cmpnyCd, String siteCd, String applicantUserCd,
                       String reqId, int approvalStep, String approverUserCd, String insertNo);

    /**
     * 승인 요망 outbox 적재 실행부 — 본 트랜잭션 커밋 이후 {@code REQUIRES_NEW} 새 트랜잭션에서
     * 호출된다. 직접 호출하지 말고 {@link #notifyAttdApprovalRequest} 를 통해 afterCommit 으로 진입한다.
     */
    void runRequestOutbox(String cmpnyCd, String siteCd, String applicantUserCd,
                          String reqId, String insertNo);
}
