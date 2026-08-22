package com.prafta.web.attd.attd09.dto.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 입사일 기준 차액 보전(법정 수기부여) 요청 body (경력인정 이원화 Phase 2 §2-3).
 * POST /attd09/leave-grant/cover-grant.
 *
 * <p>cmpnyCd는 요청 body로 받지 않는다(JWT 스코프만 신뢰 — 가드레일 3). AVAIL_FROM(지급일)도 폼 입력이
 * 아니라 서버가 실행 시점(오늘)으로 채운다(T-4).
 */
@Getter
@Setter
@NoArgsConstructor
public class CoverGrantRequest {

    /** 보전 대상 사용자 코드 */
    private String userCd;

    /** 보전 부여 일수 (0.5 단위, 0 초과) */
    private BigDecimal grantDays;

    /** 부여 사유 */
    private String reason;

    /** 남은 부족분 서버 재계산 기준일 (YYYYMMDD, 필수 — 차액 조회 화면의 조회 기준일과 동일해야 함) */
    private String baseYmd;
}
