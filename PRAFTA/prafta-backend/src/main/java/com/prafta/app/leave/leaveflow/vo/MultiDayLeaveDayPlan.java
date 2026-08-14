package com.prafta.app.leave.leaveflow.vo;

/**
 * prafta-leavemulti: 기간(From-To) 신청 미리보기의 날짜 1건 판정 결과.
 *
 * <p>화면은 이 값으로 3단계를 렌더한다:
 * <ul>
 *   <li>{@code selectable=true,  defaultChecked=true}  → ☑ 신청 가능 · 근무계획 있음</li>
 *   <li>{@code selectable=true,  defaultChecked=false} → ☐ 신청 가능 · 스케줄 없음(주말·휴무). <b>사용자가 체크하면 신청된다</b></li>
 *   <li>{@code selectable=false}                       → ⊘ 신청 불가 + {@code blockedReason}</li>
 * </ul>
 *
 * <p>★ "선택 불가(⊘)"와 "기본 해제(☐)"는 반드시 구분해 표시해야 한다.
 * 앞은 물리적으로 안 되는 날, 뒤는 사용자가 고를 수 있는 날이다.
 * (앱 단일일 신청에는 원래 휴일 검증이 없어 주말 연차가 가능하다 — 기간신청도 그 능력을 뺏지 않는다.)
 */
public record MultiDayLeaveDayPlan(
        String ymd
        , String dow
        , boolean weekend
        , boolean holiday
        /** 그날 근무계획(SCH) 배정 여부 — 기본 체크의 1순위 근거 */
        , boolean hasSchedule
        /** 기본 체크 상태 */
        , boolean defaultChecked
        /** 선택 가능 여부 (false = ⊘) */
        , boolean selectable
        /** 선택 불가 사유 코드 (선택 가능하면 null) */
        , String blockedReasonCode
        /** 선택 불가 사유 문구 (선택 가능하면 null) */
        , String blockedReason
) {
}
