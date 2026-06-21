package com.prafta.common.cmm.push;

import java.util.List;

/**
 * 교대근무 팀 스케줄 변경 통보 PUSH 생산자 (prafta-com-016-D-2).
 *
 * <p>{@code LeaveDirectSetNotiService} 와 동일하게 afterCommit + REQUIRES_NEW 격리 패턴을 따른다.
 * 저장 트랜잭션이 확정 커밋된 뒤에만 outbox 를 적재한다(저장 본 흐름 롤백 금지).
 *
 * <p>대상 근로자 1인 + 실제 덮인 날짜목록을 받아 묶음 1건만 적재한다. 실제 덮인 날이 없으면 발송 생략.
 */
public interface ShiftSchChangeNotiService {

    /**
     * 진입부 — 본 트랜잭션 커밋 이후 1건 묶음 발송을 예약한다(afterCommit).
     *
     * @param cmpnyCd      회사 코드
     * @param siteCd       사업장 코드
     * @param targetUserCd 통보 대상 근로자(스케줄이 덮어씌워진 조원)
     * @param shiftTeamNm  교대근무 팀명(본문 치환용, PII 아님)
     * @param changedYmds  이번 저장에서 실제 교대 스케줄로 덮어씌워진 날짜목록(YYYYMMDD). 비면 발송 생략.
     * @param actorUserCd  변경 수행자(관리자) — 적재 INSERT_NO 용. PII 미포함.
     */
    void notifyShiftSchChange(String cmpnyCd, String siteCd, String targetUserCd,
                              String shiftTeamNm, List<String> changedYmds, String actorUserCd);

    /**
     * 실행부 — REQUIRES_NEW 새 트랜잭션 경계(자기 프록시 경유 호출). 직접 호출 금지.
     */
    void runShiftSchChangeOutbox(String cmpnyCd, String siteCd, String targetUserCd,
                                 String shiftTeamNm, List<String> changedYmds, String actorUserCd);
}
