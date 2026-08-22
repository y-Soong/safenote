package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 입사일 기준 차액 조회 대상 후보 1건 — Mapper 조회 결과 (경력인정 이원화 Phase 2 §2-2).
 *
 * <p>계산(정답 누적/실제 부여 누적/기보전 합)은 서비스 계층에서 사용자별로 산정한다(SQL 집계 불가 — 엔진
 * {@code computeHireBasisAccrual} 이 다년 반복 계산이라 단일 SQL로 표현할 수 없다).
 */
@Getter
@Setter
public class ShortfallCandidateVO {

    /** 사용자 코드 */
    private String userCd;

    /** 성명 */
    private String userNm;

    /** 입사일 (YYYYMMDD) */
    private String hireDate;
}
