package com.prafta.common.cmm.dailyentry.result;

/**
 * 일용직 로그인 시 입장 승인 판정 결과 유형 (plan §1 판단 규칙).
 *
 * <ul>
 *   <li>APPROVED — 승인('02') 요청 존재 → 재활성/활성화 + 슬롯 점유 + 소진 진행</li>
 *   <li>PENDING — 대기('01') 요청 존재 → DAILYLOGIN_400_006 안내</li>
 *   <li>REJECTED_TODAY — 당일 거부('03') 이력 존재 → DAILYLOGIN_400_007 안내(신규 요청 미생성)</li>
 *   <li>NONE — open 요청/당일 거부 없음 → 신규 요청 생성 후 006 안내</li>
 * </ul>
 */
public enum EntryLoginDecisionType {
    APPROVED
    , PENDING
    , REJECTED_TODAY
    , NONE
}
