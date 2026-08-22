package com.prafta.common.cmm.leave.command;

import java.math.BigDecimal;

/**
 * 입사일 기준 차액 보전(법정 수기부여) 입력 객체 (경력인정 이원화 Phase 2 §2-3, attd09).
 *
 * <p>정책서 출처: 작업지시서 §2-3 / plan §F-2. 지급일 기산(T-4)이므로 폼 입력에 AVAIL_FROM 은 없다
 * (서버가 실행 시점 오늘로 채운다). {@code baseYmd} 는 "남은 부족분" 서버 재계산 기준일(클라이언트가 화면에서
 * 조회한 기준일과 동일해야 화면 표시와 실제 상한이 일치 — 클라 부족분 값 자체는 신뢰하지 않는다).
 *
 * @param userCd    보전 대상 사용자 코드
 * @param grantDays 보전 부여 일수 (0.5 단위, 0 초과)
 * @param reason    부여 사유
 * @param baseYmd   남은 부족분 재계산 기준일 (YYYYMMDD)
 */
public record CoverGrantCommand(
      String userCd
    , BigDecimal grantDays
    , String reason
    , String baseYmd
) {
}
