package com.prafta.web.user.user10.dto.response;

import java.util.List;

import com.prafta.common.cmm.stdwork.vo.StdWorkHoursVO;

import lombok.Builder;
import lombok.Value;

/**
 * 소정-10: 특정 근로자의 소정근로시간 이력 + 오늘 기준 요약 응답 (User_10 타임라인).
 *
 * <p>요약({@code source}/{@code fromHistory})은 화면이 "미입력 → 통상 기준 간주" 배지를
 * 정확히 구분해 그리기 위한 값이다(plan UI-C).
 */
@Value
@Builder
public class StdWorkHistoryResponse {

    String userCd;

    String userNm;

    /** 고용형태 [SYS041] */
    String employmentType;

    /** 판정 기준일 (YYYYMMDD) */
    String baseYmd;

    /** 오늘 기준 해석된 주 소정근로 분 (폴백 포함 — 항상 값 존재) */
    int weekStdMinutes;

    /** 회사 통상근로자 주 소정근로 분 (비교 분모) */
    int cmpnyWeekStdMinutes;

    /** 값의 출처 — USER_HISTORY / COMPANY_POLICY / SYSTEM_DEFAULT */
    String source;

    /** 이력이 실제 입력된 계정인지 (false = 폴백 해석) */
    boolean fromHistory;

    /** 단시간근로자 파생 여부 */
    boolean partTime;

    /** 소정근로시간 관리 대상 계정인지 (일용직·사용중지·미존재면 false → 화면은 등록 버튼을 막는다) */
    boolean eligible;

    /** 일용직 계정 여부 */
    boolean dailyWorker;

    /** 이력 타임라인 (적용 시작일 내림차순) */
    List<StdWorkHoursVO> historyList;
}
