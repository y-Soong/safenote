package com.prafta.app.leave.leaveflow.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-leavemulti: 연차 기간(From-To) 신청 미리보기 응답.
 *
 * <p>화면은 {@code days} 로 날짜별 체크리스트를 그리고, {@code shortageDays > 0} 이면 제출을 막는다.
 * 제출은 이 응답의 {@code ymd} 중 사용자가 체크한 것만 <b>날짜 목록</b>으로 보낸다
 * (범위가 아니라 목록이 서버 계약 — 체크 = 포함 의도이므로 서버가 휴일을 재판정하지 않는다).
 */
@Getter
@Builder
public class LeaveApplyMultiPreviewResponse {

    /** 구간 날짜별 판정 결과(오름차순). */
    private final List<Day> days;

    /** 기본 체크된 날짜 수 = 제출 시 기본 신청 일수. */
    private final int defaultCheckedCount;

    /** 기본 체크 기준 필요 일수(= defaultCheckedCount). */
    private final BigDecimal neededDays;

    /** 기본 체크 기준 배정 가능 일수. */
    private final BigDecimal assignedDays;

    /**
     * 부족 일수. {@code > 0} 이면 잔여 부족으로 <b>전체 거부</b> 대상이다(정책 ③).
     *
     * <p>날짜별 배정 시뮬레이션 결과다 — 잔여는 부여 유효기간 때문에 날짜마다 다르므로
     * "총 N일 ≤ 잔여" 단순 비교로는 구할 수 없다.
     */
    private final BigDecimal shortageDays;

    /** 날짜 1건. */
    @Getter
    @Builder
    public static class Day {
        private final String ymd;
        private final String dow;
        private final boolean weekend;
        private final boolean holiday;
        /** 그날 근무계획(SCH) 배정 여부 — 기본 체크의 근거 */
        private final boolean hasSchedule;
        /** 기본 체크 상태 */
        private final boolean defaultChecked;
        /** 선택 가능 여부. false 면 화면에서 ⊘(선택 불가)로 표시하고 사유를 노출한다 */
        private final boolean selectable;
        private final String blockedReasonCode;
        private final String blockedReason;
    }
}
