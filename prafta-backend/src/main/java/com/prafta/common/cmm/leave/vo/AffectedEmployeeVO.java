package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 정책 변경 영향 분석(화면 8)의 영향받는 직원 1행(응답용).
 *
 * <p>정책서: {@code .claude/requests/ref/prafta-017/CLAUDE_CODE_INSTRUCTIONS.md} §9.6 (주요 영향 추출 규칙)
 *
 * <p>{@code currentGrant}/{@code currentUsed}/{@code expectedAdditional}은 1년치 부여
 * 시뮬레이션 기반 <b>근사치</b>이며 실제 일배치 부여 결과와 차이가 있을 수 있다(§9.6/§9.8 근사 한계).
 *
 * <p>{@code positionNm}(직급)은 TB_USER에 직급 컬럼이 없어 항상 null(미표시) — 메인 세션 결정 D-4.
 */
@Getter
@Builder
public class AffectedEmployeeVO {

    /** 사용자 코드 */
    private final String userCd;

    /** 사용자명 (PII 평문, 관리자 화면 한정) */
    private final String userNm;

    /** 소속 부서명 */
    private final String deptNm;

    /** 직급 (TB_USER 직급 컬럼 없음 → 항상 null, D-4) */
    private final String positionNm;

    /** 입사일 (YYYYMMDD) */
    private final String hireDate;

    /** 기존 부여 일수 (근사) */
    private final BigDecimal currentGrant;

    /** 기존 사용 일수 (근사) */
    private final BigDecimal currentUsed;

    /** 예상 추가 부여 일수 (타깃 - 현재, 근사) */
    private final BigDecimal expectedAdditional;

    /** 주요 영향 메시지 (§9.6 우선순위 단일 메시지) */
    private final String mainImpact;
}
