package com.prafta.common.cmm.leave.feature.dto.response;

import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSummaryVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursVO;

import lombok.Builder;
import lombok.Value;

/**
 * 소정-06: 본인 소정근로 요약(단시간 파생 판정 포함) 응답.
 *
 * <p>{@code GET /prafta/comApi/leave-feature/std-work-summary?baseYmd=YYYYMMDD}
 *
 * <p><b>★0단계 경계</b>: 조회 전용이다. 본 응답의 {@code partTime}/{@code weekStdMinutes} 는
 * 아직 어떤 부여·차감·판정 로직에도 연결되지 않는다(지시서 0단계 완료 기준).
 */
@Value
@Builder
public class MyStdWorkSummaryResponse {

    /** 판정 기준일 (YYYYMMDD) */
    String baseYmd;

    /** 본인 주 소정근로 분 (폴백 포함 — 항상 값 존재. 2400 = 주 40시간) */
    int weekStdMinutes;

    /** 회사 통상근로자 주 소정근로 분 (비교 분모) */
    int cmpnyWeekStdMinutes;

    /**
     * 값의 출처. USER_HISTORY(이력 행) / COMPANY_POLICY(회사 기준값 폴백) / SYSTEM_DEFAULT(코드 상수 2400).
     * 화면이 "미입력(통상 기준 40h 간주)" 배지를 그릴 때 쓴다.
     */
    String source;

    /** 소정근로 이력이 실제로 입력된 계정인지 (false = 폴백 해석) */
    boolean fromHistory;

    /** 단시간근로자 파생 여부 (본인 주 소정 &lt; 회사 통상 기준) */
    boolean partTime;

    /** 소정근로시간 관리 대상 계정인지 (일용직·사용중지·미존재 계정은 false) */
    boolean eligible;

    /** 일용직 계정 여부 */
    boolean dailyWorker;

    /** 기준일 유효 이력 행의 적용 시작일 (없으면 null) */
    String applyStrDate;

    /** 기준일 유효 이력 행의 적용 종료일 (없거나 무기한이면 null) */
    String applyEndDate;

    /** 기준일 유효 이력 행의 사유코드 [SYS083] (없으면 null) */
    String reasonCd;

    /** 기준일 유효 이력 행의 사유 명칭 (없으면 null) */
    String reasonNm;

    public static MyStdWorkSummaryResponse of(StdWorkHoursSummaryVO vo) {
        StdWorkHoursVO row = (vo == null) ? null : vo.getEffectiveRow();
        return MyStdWorkSummaryResponse.builder()
                .baseYmd(vo == null ? null : vo.getBaseYmd())
                .weekStdMinutes(vo == null ? 0 : vo.getWeekStdMinutes())
                .cmpnyWeekStdMinutes(vo == null ? 0 : vo.getCmpnyWeekStdMinutes())
                .source(vo == null || vo.getSource() == null ? null : vo.getSource().name())
                .fromHistory(vo != null && vo.isFromHistory())
                .partTime(vo != null && vo.isPartTime())
                .eligible(vo != null && vo.isEligible())
                .dailyWorker(vo != null && vo.isDailyWorker())
                .applyStrDate(row == null ? null : row.getApplyStrDate())
                .applyEndDate(row == null ? null : row.getApplyEndDate())
                .reasonCd(row == null ? null : row.getReasonCd())
                .reasonNm(row == null ? null : row.getReasonNm())
                .build();
    }
}
