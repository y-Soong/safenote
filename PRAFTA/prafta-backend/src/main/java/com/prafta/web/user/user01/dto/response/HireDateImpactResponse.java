package com.prafta.web.user.user01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 입사일 변경 영향 분석 응답 (PRAFTA-017-4 / prafta-022 작업 F / prafta-032 옵션 시뮬 폐기).
 *
 * <p>기존 부여(existingGrant)/사용(used)은 {@code tb_user_leave_grant} 활성 법정부여의 <b>실집계</b>다.
 * 누락 부여(missingGrant)/다음 부여 예정(nextGrant)은 <b>법정 기본 근사</b>로, 정책(AXIS) 정밀 산정이 아니다.
 * 입사일 변경 자체는 grant를 조작하지 않고 기록만 유지한다(정책서 §8.5.6 영향 스냅샷 / §8.5.8 기부여 보호).
 *
 * <p>prafta-032(D1): 옵션별 재할당 시뮬 미리보기(options[]·reclaimNote)를 <b>폐기</b>한다(처리방식 자동계산 폐기).
 * 단 "FISCAL 다음 회계연도 발생예정"({@code fiscalNextGrantText})은 유지한다. 연차 조정은 입사일 변경 화면의
 * 수동 입력(목표 법정 부여량과의 차액 추가/회수)으로 처리한다.
 */
@Getter
@Builder
public class HireDateImpactResponse {
    private String scenarioLabel;      // 예: "1년 미만 · 입사일 과거로"
    private String existingGrantText;  // 활성 법정부여 GRANT_DAYS 합계 (실집계)
    private String usedText;           // 활성 법정부여 USED_DAYS 합계 (실집계)
    private String missingGrantText;   // 변경 후 기준 법정 기본 기대치 − 현재 부여합 (근사)
    private String nextGrantText;      // 새 입사일 다음 anniversary (근사)
    private String changeSummaryText;  // prev/new 비교 요약

    /** FISCAL 정책 시 "본연차 다음 회계연도(YYYY-MM-DD) 발생 예정", 그 외 "" (prafta-032 D1 유지). */
    private String fiscalNextGrantText;
}
