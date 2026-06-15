package com.prafta.common.cmm.push;

/**
 * 위험성평가 검토요청 통보(M5) PUSH 생산자(outbox PENDING 적재) (PRAFTA-APP-021-3d).
 *
 * <p>위험성평가가 "검토 요청"(001)으로 전이된 직후 호출된다. 수신 대상 = 사업장 안전관리자(safe 역할)
 * ∪ 작성자 소속 노드 main/sub 관리자(합집합·중복 제거·본인 제외, §8-R 1). afterCommit + REQUIRES_NEW
 * 격리로 위험성평가 저장 본 흐름을 절대 롤백시키지 않는다.
 */
public interface RiskAssessNotiService {

    /**
     * 검토요청 통보. 저장 트랜잭션 커밋 이후 적재.
     *
     * @param cmpnyCd      회사 코드
     * @param siteCd       사업장 코드(위험성평가 SITE_CD)
     * @param assessmentCd 위험성평가 코드(dedupKey + 라우팅 키)
     * @param requesterUserCd 검토 요청 작성자(노드 관리자 산출 기준 + 본인 제외)
     * @param actorUserCd  적재 INSERT_NO(= 작성자)
     */
    void notifyReviewRequested(String cmpnyCd, String siteCd, String assessmentCd,
                               String requesterUserCd, String actorUserCd);

    /** 검토요청 outbox 적재(REQUIRES_NEW). afterCommit 콜백 전용 — 직접 호출 금지. */
    void runReviewRequestedOutbox(String cmpnyCd, String siteCd, String assessmentCd,
                                  String requesterUserCd, String actorUserCd);
}
