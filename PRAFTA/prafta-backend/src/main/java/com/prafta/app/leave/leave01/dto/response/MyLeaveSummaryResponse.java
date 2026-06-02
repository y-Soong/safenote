package com.prafta.app.leave.leave01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-005: 앱 "연차 현황" 응답.
 * <p>plan §1-2 JSON 스키마와 1:1 (camelCase). 일수는 소수 1자리 반올림 double,
 * usageRate 는 정수(%), hireDate 는 YYYYMMDD 원본 문자열.
 * <p>groups 는 TOTAL/STATUTORY/NON_STATUTORY 3종을 항상 포함(부여 없으면 0 채움).
 */
@Getter
@Builder
public class MyLeaveSummaryResponse {

    private final User user;
    private final Groups groups;
    private final ExpiringSoon expiringSoon;

    /** 사용자 메타 영역. */
    @Getter
    @Builder
    public static class User {
        /** 사용자명(평문 PII, 본인 자기조회 한정 노출. 로그 출력 금지). */
        private final String userNm;
        /** 입사일 YYYYMMDD 원본(없으면 null). FE 에서 하이픈 포맷. */
        private final String hireDate;
        /** 실근속 개월수(입사일~오늘, 서버 산출. 경력 미포함). hireDate 없으면 0. */
        private final int serviceMonths;
        /** 경력 인정 개월 합계(USE_YN='Y'). 0이면 FE 에서 숨김. */
        private final int serviceCreditMonths;
    }

    /** 그룹 3종 묶음. */
    @Getter
    @Builder
    public static class Groups {
        @com.fasterxml.jackson.annotation.JsonProperty("TOTAL")
        private final Group TOTAL;
        @com.fasterxml.jackson.annotation.JsonProperty("STATUTORY")
        private final Group STATUTORY;
        @com.fasterxml.jackson.annotation.JsonProperty("NON_STATUTORY")
        private final Group NON_STATUTORY;
    }

    /** 그룹별 수치(일단위). */
    @Getter
    @Builder
    public static class Group {
        /** 부여 합계 = SUM(GRANT_DAYS). */
        private final double granted;
        /** 사용(과거/오늘 확정 소비분) = usedTotal - planned. */
        private final double used;
        /** 사용예정(미도래 CONFIRMED 분) = SUM(LEAVE_DAYS, START_DATE>오늘). */
        private final double planned;
        /** 잔여 = granted - usedTotal. */
        private final double remaining;
        /** 사용률(%) = (granted==0)?0:round(usedTotal/granted*100). 그룹별. */
        private final int usageRate;
    }

    /** 소멸 임박(D-30) 영역. 그룹 무관 전체 1회. */
    @Getter
    @Builder
    public static class ExpiringSoon {
        /** 30일 이내 소멸 대상(잔여>0) 존재 여부. */
        private final boolean exists;
        /** 가장 임박한 소멸까지 남은 일수(D-day). 없으면 0. */
        private final int daysUntilExpiry;
        /** 대상 부여들의 잔여 합계. 없으면 0.0. */
        private final double totalRemainingDays;
        /** 가장 임박한 소멸일 YYYYMMDD(없으면 null). UI 미표시/확장용. */
        private final String expiryDate;
    }
}
