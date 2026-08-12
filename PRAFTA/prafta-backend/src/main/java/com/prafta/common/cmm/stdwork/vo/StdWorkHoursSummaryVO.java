package com.prafta.common.cmm.stdwork.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 소정-02: 특정일 기준 근로자 소정근로 요약 (폴백 해석 + 단시간 파생 판정 결과).
 *
 * <p>후속 작업(소정-03·06·07, 2단계 부여·차감 엔진)이 소비하는 단일 진입 결과다.
 * "이력 행이 있었는지 / 어디서 폴백했는지"를 함께 실어, 화면이 "미입력(통상 기준 40h 간주)"
 * 배지를 그릴 수 있게 한다(plan UI-C).
 */
@Getter
@Builder
public class StdWorkHoursSummaryVO {

    /** 회사 코드 */
    private final String cmpnyCd;

    /** 사용자 코드 */
    private final String userCd;

    /** 판정 기준일 (YYYYMMDD) */
    private final String baseYmd;

    /** 해석된 본인 주 소정근로 분 (폴백 포함, 항상 값 존재) */
    private final int weekStdMinutes;

    /** 회사 통상근로자 기준 주 소정근로 분 (비교 분모, 항상 값 존재) */
    private final int cmpnyWeekStdMinutes;

    /** 값의 출처 — {@link StdWorkSource} */
    private final StdWorkSource source;

    /**
     * 단시간근로자 파생 여부 (본인 주 소정 &lt; 회사 통상 기준).
     *
     * <p>지시서 B-2: 법적 정의는 "동종 업무 통상근로자 대비"이나 기계 구현은 과설계이므로
     * 회사 기준값 대비 단순 비교로 파생한다. 경계 사례는 관리자 수동 판단 영역.
     * EMPLOYMENT_TYPE 에 값을 추가하지 않는다(파생만).
     */
    private final boolean partTime;

    /** 기준일 유효 이력 행 (없으면 null — 폴백으로 해석된 경우) */
    private final StdWorkHoursVO effectiveRow;

    /**
     * 소정근로시간 관리 대상 계정인지 여부 (계정이 없거나 탈퇴·사용중지·일용직이면 false).
     *
     * <p>★소비처 주의: 본 VO 의 {@code weekStdMinutes} 는 <b>대상 여부와 무관하게</b> 폴백
     * 체인으로 항상 채워진다. 즉 일용직 계정도 2400분(또는 회사 기준값)을 받는다 —
     * 소정근로 개념이 없는 계정에 의미 없는 값이므로, 연차 부여·정산·단시간 판정 소비처는
     * 반드시 이 플래그로 먼저 걸러야 한다.
     */
    private final boolean eligible;

    /** 일용직 계정 여부 (EMPLOYMENT_TYPE='DAILY'). */
    private final boolean dailyWorker;

    /** 소정근로 값의 출처. */
    public enum StdWorkSource {
        /** 근로자 소정근로시간 이력 행에서 해석 */
        USER_HISTORY,
        /** 이력 부재 → 회사 통상 기준값(TB_CMPNY_STD_WORK_POLICY) 폴백 */
        COMPANY_POLICY,
        /** 이력·회사 기준값 모두 부재 → 코드 상수 2400분 폴백 */
        SYSTEM_DEFAULT
    }

    /** 소정근로 이력이 실제로 입력된 계정인지 여부 (false = 폴백 해석). */
    public boolean isFromHistory() {
        return source == StdWorkSource.USER_HISTORY;
    }
}
