package com.prafta.app.admin.dashboard.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * J1-10 (B-5): 관리자 대시보드 요약 응답.
 *
 * <p>4영역(근태/순회/위험성/아차) 카운트 + 위젯별 available(서버 산출 권한 플래그, C1). PII 미포함 — 숫자만.
 * <ul>
 *   <li>attendance: 노드축 게이트(master/hr 전사 ∥ 노드관리자 자손, safe ⛔).</li>
 *   <li>patrol/risk/nearMiss: 사업장축 2단 게이트(canManageCommon ∥ 노드관리자 + 멤버십 IDOR).
 *       세 위젯은 동일 게이트라 available 값이 같다(서버는 한 번만 평가).</li>
 * </ul>
 * available=false 위젯은 카운트가 0으로 내려가며 프론트가 "권한 없음" 처리한다.
 */
@Getter
@Builder
public class DashboardSummaryResponse {

    private final Attendance attendance;
    private final Patrol patrol;
    private final Risk risk;
    private final NearMiss nearMiss;

    /** 근태 위젯(노드 스코프). */
    @Getter
    @Builder
    public static class Attendance {
        // Lombok+Jackson is-접두 직렬화 함정 회피: available 은 is-접두가 아니라 무관하나, 명시적으로 키 고정.
        @JsonProperty("available")
        private final boolean available;
        /** 당일 실제 출근 인원(distinct USER_CD, 체크인 존재). */
        private final int checkedInCnt;
        /** 당일 출근 예정 인원(distinct USER_CD, work_plan→sch_mgmt 매칭). */
        private final int scheduledCnt;
        /** 당일 종일 연차 인원(distinct USER_CD, USE_UNIT_TYPE='00' 확정). */
        private final int leaveCnt;
    }

    /** 순회 위젯(사업장 스코프, 금일 기준). */
    @Getter
    @Builder
    public static class Patrol {
        @JsonProperty("available")
        private final boolean available;
        /** 금일 점검 대상 개소(사업장 활성 체크포인트 수 — 분모). */
        private final int targetCnt;
        /** 금일 점검 완료 개소(WORK_DATE=오늘 답변 존재 distinct CHKPT_CD — 분자). */
        private final int completedCnt;
    }

    /** 위험성 평가 위젯(사업장 스코프). */
    @Getter
    @Builder
    public static class Risk {
        @JsonProperty("available")
        private final boolean available;
        /** 미처리 검토요청(SYS011='001') 건수. */
        private final int pendingCnt;
    }

    /** 아차사고 위젯(사업장 스코프). */
    @Getter
    @Builder
    public static class NearMiss {
        @JsonProperty("available")
        private final boolean available;
        /** 신규 접수(REPORT_STATUS_CD='100') 건수. */
        private final int newCnt;
    }
}
