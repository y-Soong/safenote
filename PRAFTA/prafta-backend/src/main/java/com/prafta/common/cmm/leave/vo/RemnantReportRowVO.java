package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

/**
 * PC-07(D9-③·N2): 소멸 임박 짜투리 리포트 1행 (OFF 회사 — 연말 수당 정산 지원).
 *
 * <p>문구 원칙(D9): "근로자 손해" 표현 금지 — FE 는 "미사용분은 연차미사용수당 정산 대상" 으로 표기.
 *
 * @param userCd            사용자 코드
 * @param userNm            사용자명(관리자 화면 한정 평문)
 * @param remnantDays       대상 5종 합산 잔여(일) — 0 &lt; 잔여 &lt; 최소단위 요금
 * @param minUnitType       그 사용자의 최소 사용단위(SYS025 — 교대자는 시간차 제외 후 최소)
 * @param minUnitChargeDays 최소단위 요금(일, 본인 분모 기준)
 * @param roundingDust      절사 끝수 여부(잔여 &lt; 0.001 — §5-④ 임계) / false = 실질 짜투리
 * @param nearestExpireYmd  최근접 소멸일(YYYYMMDD, 잔여 보유 부여 중 최소 AVAIL_TO_DATE)
 * @param convMinutes       본인 분모(분, 오늘 기준 — 미산출 480 폴백, 표기용)
 */
public record RemnantReportRowVO(
      String userCd
    , String userNm
    , BigDecimal remnantDays
    , String minUnitType
    , BigDecimal minUnitChargeDays
    , boolean roundingDust
    , String nearestExpireYmd
    , int convMinutes
) {
}
