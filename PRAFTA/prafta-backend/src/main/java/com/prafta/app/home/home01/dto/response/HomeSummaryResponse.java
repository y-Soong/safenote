package com.prafta.app.home.home01.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-001: 앱 메인화면 요약 응답.
 * <p>계약서 §1 JSON 스키마와 1:1 (camelCase, 시각은 HHMM 문자열 또는 null).
 * 4개 영역(attendance / leave / approval / tbm)을 단일 응답으로 묶는다.
 */
@Getter
@Builder
public class HomeSummaryResponse {

    private final Attendance attendance;
    private final Leave leave;
    private final Approval approval;
    private final Tbm tbm;

    /** 출퇴근 표시 영역. */
    @Getter
    @Builder
    public static class Attendance {
        /** BEFORE_WORK / WORKING / OFF_WORK */
        private final String status;
        /**
         * prafta-app-013-2: 기준일(baseYmd) 근무 스케줄 존재 여부(휴무/미배정/미존재면 false).
         * Jackson 이 boolean is* getter 에서 "is" 를 떼고 직렬화하는 것을 방지(계약 키 고정).
         * (isOffsite 와 동일 패턴 — JSON 키가 반드시 "scheduleExists" 로 나가야 함.)
         */
        @JsonProperty("scheduleExists")
        private final boolean scheduleExists;
        /** 예정 출근시각 HHMM (없으면 null) */
        private final String scheduleStart;
        /** 예정 퇴근시각 HHMM (없으면 null) */
        private final String scheduleEnd;
        /** 실제 출근시각 HHMM (없으면 null) */
        private final String checkInTime;
        /** 실제 퇴근시각 HHMM (없으면 null) */
        private final String checkOutTime;
        /**
         * 외근 여부 (prafta-app-003 B-1): 오늘 가장 최근 출근 레코드에 출근 GPS 행(GPS_INFO_TYPE='01')이
         * 존재하면 true(근무지 외=외근). 오늘 출근이 없으면 false.
         * Jackson 이 boolean is* getter 에서 "is" 를 떼고 직렬화하는 것을 방지(계약 키 고정).
         */
        @JsonProperty("isOffsite")
        private final boolean isOffsite;
        /** 출근 가능 여부 (서버 산출) */
        private final boolean canCheckIn;
        /** 퇴근 가능 여부 (서버 산출) */
        private final boolean canCheckOut;
        /**
         * prafta-app-021 (§7.6): 직전일(today-1) 미퇴근이 남아 메인에서 마감 대기 중인지 여부.
         * true 면 프론트가 "전날 미퇴근" 배지/안내를 노출하고 출근 버튼을 비활성, 퇴근 버튼을 활성화한다.
         * (오늘 진행 중(canCheckOut=hasOpen) 케이스가 우선이며, 그 경우 false 일 수 있다.)
         * Jackson 이 boolean is* getter 에서 "is" 를 떼는 것을 방지(계약 키 고정).
         */
        @JsonProperty("prevDayCheckoutPending")
        private final boolean prevDayCheckoutPending;
        /** prafta-app-021: 직전일 미퇴근 근무의 출근시각 HHMM (없으면 null). 카드 "출근 HH:MM (전날)" 표기용. */
        private final String prevDayCheckInTime;
        /**
         * prafta-app-015: 2구간 스케줄 여부. 메인 홈 출퇴근 카드가 구간 선택(1구간/2구간) 버튼을
         * 노출할지 판정한다(attd01 isTwoSlot 과 동일 의미). is-접두 회피로 직렬화 키 "isTwoSlot" 고정.
         */
        @JsonProperty("isTwoSlot")
        private final boolean isTwoSlot;
        /**
         * prafta-app-015: 2구간 스케줄 구간 선택 게이팅용 슬롯 플래그(서버 산출, 1·2구간).
         *   1구간/스케줄없음은 비어 있다(MainView 는 단일 canCheckIn 버튼 유지).
         */
        private final List<SlotFlag> slots;
    }

    /**
     * prafta-app-015: 2구간 스케줄 구간 선택 버튼 게이팅(attd01 SlotResponse 플래그와 동일 의미).
     */
    @Getter
    @Builder
    public static class SlotFlag {
        private final int workSeq;
        private final boolean canCheckInThisSlot;
        private final boolean alreadyCheckedIn;
    }

    /** 연차 요약 영역(법정+약정 합산). */
    @Getter
    @Builder
    public static class Leave {
        private final double grantedDays;
        private final double remainingDays;
    }

    /** 결재 대기 영역. */
    @Getter
    @Builder
    public static class Approval {
        private final int pendingCount;
    }

    /** TBM(오늘 안전교육) 영역. */
    @Getter
    @Builder
    public static class Tbm {
        private final boolean hasToday;
        /** TBM 세션코드 (입실 화면 진입용, 없으면 null) */
        private final String sessionCd;
        /** OPENED / IN_PROGRESS / COMPLETED / NONE */
        private final String sessionStatus;
        /** 개설시각 HHMM (없으면 null) */
        private final String openedTime;
        private final String title;
        private final String presenterName;
        /** NOT_ENTERED / ENTERED / COMPLETED */
        private final String myAttendanceStatus;
        /** 본인 입실시각 HHMM (없으면 null) */
        private final String myEntryTime;
    }
}
