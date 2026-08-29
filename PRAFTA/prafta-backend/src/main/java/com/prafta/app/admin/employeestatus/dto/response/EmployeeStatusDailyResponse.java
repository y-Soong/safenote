package com.prafta.app.admin.employeestatus.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * PRAFTA-002: 직원 현황(일자) 응답.
 *
 * <p>사업장/부서 스코프 내 활성 사용자 전체(로스터)를 대상으로 근무중/미출근/휴무/퇴근 4개 상태를 산출해
 * 내려준다(정책서 attd §14.2). 연차는 상태와 배타가 아닌 additive 배지({@code isOnLeave})다(plan §2-3).
 * PII(휴대폰/이메일) 미포함 — 이름·노드명만.
 */
@Getter
@Builder
public class EmployeeStatusDailyResponse {

    private final List<DailyItem> items;
    private final int totalCount;
    private final boolean hasMore;

    @Getter
    @Builder
    public static class DailyItem {
        private final String userCd;
        private final String userNm;
        private final String nodeNm;
        /** 근무중(WORKING) | 미출근(ABSENT) | 휴무(DAY_OFF) | 퇴근(CHECKED_OUT) — 서버 확정, Java 산출. */
        private final String status;
        // Lombok+Jackson 의 is-접두 직렬화 함정 방지: @JsonProperty 로 키를 isXxx 로 고정한다
        //   (미지정 시 isOnLeave→"onLeave" 로 떨어져 프론트 계약 불일치 — 메모리 feedback_lombok_jackson_boolean_is_prefix).
        /** 연차 계열(종일+반차+시간차) 사용 여부 — 상태와 배타 아닌 additive 배지. */
        @JsonProperty("isOnLeave")
        private final boolean isOnLeave;
        /** 외근(그날 GPS 행 존재) 여부 — true 인 카드만 GPS 시트 진입 가능. */
        @JsonProperty("isOffsite")
        private final boolean isOffsite;
        /** 출근 HHMM(1차 대표). 미출근이면 null. */
        private final String checkInTime;
        /** 퇴근 HHMM(1차 대표). 미퇴근이면 null. */
        private final String checkOutTime;
        /** 그날 이 사용자의 ATTD_ID 전부(2구간이면 최대 2개) — GPS 조회(gps-trail) 순차 호출용. */
        private final List<String> attdIds;
    }
}
