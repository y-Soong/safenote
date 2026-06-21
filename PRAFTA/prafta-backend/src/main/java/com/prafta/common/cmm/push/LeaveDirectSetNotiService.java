package com.prafta.common.cmm.push;

import java.util.List;

/**
 * 관리자 연차/월차 직접 등록 통보 PUSH 생산자 (prafta-com-016-C-2).
 *
 * <p>{@code ApprovalResultNotiService} 등과 동일하게 afterCommit + REQUIRES_NEW 격리 패턴을 따른다.
 * 저장 트랜잭션이 확정 커밋된 뒤에만 outbox 를 적재한다(저장 본 흐름 롤백 금지).
 *
 * <p>016-D-2(교대 PUSH)도 본 인터페이스의 묶음 발송/afterCommit 패턴을 재사용할 수 있도록
 * 시그니처를 (대상 근로자 1인 + 등록된 날짜목록) 으로 범용화한다.
 */
public interface LeaveDirectSetNotiService {

    /**
     * 진입부 — 본 트랜잭션 커밋 이후 1건 묶음 발송을 예약한다(afterCommit).
     *
     * @param cmpnyCd     회사 코드
     * @param siteCd      사업장 코드
     * @param targetUserCd 통보 대상 근로자(연차/월차가 등록된 본인)
     * @param workYmds    이번 저장에서 연차/월차로 등록된 날짜목록(YYYYMMDD). 비면 발송 생략.
     * @param actorUserCd 등록 수행자(관리자) — 적재 INSERT_NO 용. PII 미포함.
     */
    void notifyLeaveDirectSet(String cmpnyCd, String siteCd, String targetUserCd,
                              List<String> workYmds, String actorUserCd);

    /**
     * 실행부 — REQUIRES_NEW 새 트랜잭션 경계(자기 프록시 경유 호출). 직접 호출 금지.
     */
    void runLeaveDirectSetOutbox(String cmpnyCd, String siteCd, String targetUserCd,
                                 List<String> workYmds, String actorUserCd);
}
