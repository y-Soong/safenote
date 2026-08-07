package com.prafta.app.leave.leave01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-005: 앱 "연차 현황" 응답.
 * <p>plan §1-2 JSON 스키마와 1:1 (camelCase). 일수는 소수 1자리 반올림 double,
 * usageRate 는 정수(%), hireDate 는 YYYYMMDD 원본 문자열.
 * <p>groups 는 TOTAL/STATUTORY/NON_STATUTORY 3종을 항상 포함(부여 없으면 0 채움).
 * <p>연차 개편(표시): {@code appliedLeaveTypes} 는 신청형 휴가(LEAVE_TYPE='01') 타입별 한도/잔여 목록으로,
 *   법정/관리자부여(groups) 와 절대 합산하지 않는 별도 섹션이다(기존 groups/expiringSoon/user 필드는 불변).
 */
@Getter
@Builder
public class MyLeaveSummaryResponse {

    private final User user;
    private final Groups groups;
    private final ExpiringSoon expiringSoon;
    /**
     * 연차 개편(표시): 신청형 휴가(LEAVE_TYPE='01') 타입별 항목(법정/관리자부여와 분리, 합산 금지).
     * '01' 타입이 0개면 빈 리스트. 각 타입은 한도(MAX_APLY_DAYS) - 당해 회계연도 사용분으로 잔여를 산출한다.
     */
    private final List<AppliedLeaveType> appliedLeaveTypes;
    /**
     * prafta-com-011-5: 미상계 가불 사용 합계(일, 소수1자리). 표시 전용(MVP, 결정 §5).
     * <p>가불 GRANT(GRANT_REASON LIKE '[가불]%')의 live(STATUS!='CANCELED' AND DEL_YN='N') GRANT_DAYS 합.
     *   가불 GRANT 는 당겨 쓴(=차감된) 일수만큼 생성되므로 합계 = 사용자가 가불로 끌어다 쓴 일수.
     *   추후 정기 부여 배치가 동일 멱등키로 skip(자동 상계)되면 별도 차감 없이 표시 유지(MVP).
     *   0 이면 FE 가 가불 카드를 숨긴다(기존 필드/응답 구조는 불변, additive).
     */
    private final double borrowedDays;

    /**
     * LC-07(표기): 현재(오늘) 기준 1일 환산시간(분, 기본 480). FE(연차현황·마이페이지 요약)가
     * 잔여/사용/부여 일수를 "N일 H시간 M분"으로 조립하는 분모(기존 필드 불변 — additive).
     */
    private final int convMinutes;

    /**
     * LC-07(표기): 시간차(SYS025 02/03/04) CONFIRMED 사용 분 합계(전 기간). FE "시간차 사용
     * N시간 M분" 원본 표기용 — 차감 일수 합계와 별개(잔여/부여 수치 무관, additive).
     */
    private final int hourlyUsedMinutes;

    /**
     * HB-13(F-3): 시간차 <b>사용</b>(START_DATE &le; 오늘) 분 합계. {@code hourlyUsedMinutes} 를
     * 사용/사용예정으로 쪼갠 값(additive — 구 필드/구 앱 동작 불변).
     *
     * <p>FE 는 이 실분을 그대로 표기해야 한다. 일수→시간 역환산(단일 분모)은 당일분모 전환(E1) 이후
     * 실제 3시간을 2시간 48분으로 보이게 만든다(잔결함 F-3). 잔여만 근사치 역환산을 유지한다(E4).
     */
    private final int hourlyUsedMinutesPast;

    /** HB-13(F-3): 시간차 <b>사용예정</b>(START_DATE &gt; 오늘) 분 합계(additive). */
    private final int hourlyUsedMinutesPlanned;

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

    /**
     * 연차 개편(표시): 신청형 휴가('01') 타입 1건.
     * <p>한도(maxAplyDays) - 당해 회계연도 사용분(usedDays) = 잔여(remainDays). 한도 NULL → 0(잔여 0, fail-closed).
     *   법정/관리자부여(Group)와 합산 금지. 잔여/한도는 서버 권위값 — FE 는 재계산 없이 그대로 렌더.
     */
    @Getter
    @Builder
    public static class AppliedLeaveType {
        /** 연차코드(TB_LEAVE_TYPE_MGMT.LEAVE_CD). */
        private final String leaveCd;
        /** 연차명(TB_LEAVE_TYPE_MGMT.LEAVE_NM). */
        private final String leaveNm;
        /** 한도(MAX_APLY_DAYS). NULL 이면 0(신청불가 = 잔여 0). */
        private final double maxAplyDays;
        /** 당해 회계연도 CONFIRMED 사용분 합계. */
        private final double usedDays;
        /** 잔여 = 한도 - 사용분(0 미만 방지 없이 그대로 — 음수는 한도 변경 등 비정상, 서버 정의 그대로). */
        private final double remainDays;
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
