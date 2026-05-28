package com.prafta.web.attd.leaveflow.vo;

/**
 * 결재 없는 직접 연차 사용 기록(근무계획 적용, prafta-021) 처리 결과.
 *
 * <ul>
 *   <li>{@code RECORDED}     : 신규 사용 기록 + 잔여 차감 완료</li>
 *   <li>{@code SKIPPED_DUP}  : 동일 직원·일자·연차코드로 이미 기록됨(멱등 — 중복 차감 방지)</li>
 *   <li>{@code INSUFFICIENT} : 차감 가능한 부여(잔여) 없음 → 기록하지 않음</li>
 * </ul>
 */
public enum DirectLeaveResult {
    RECORDED,
    SKIPPED_DUP,
    INSUFFICIENT
}
