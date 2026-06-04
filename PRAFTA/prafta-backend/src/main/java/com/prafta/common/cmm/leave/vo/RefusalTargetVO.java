package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 노무수령거부 대상일 판정 결과 1건 (PRAFTA-COM-001 기능2).
 *
 * <p>{@code tb_leave_refusal_log} 의 NOTICED 행(=사용지정일 데이터, 메인세션 A-3=옵션1)을
 * 휴일 게이트(tb_holiday 일자휴일 + tb_holiday_rule 매년 MMDD 둘 다 NOT EXISTS)와 함께
 * 조회한 결과다. 결과가 존재하면 노무수령거부 대상으로 본다(휴일이면 결과가 비어 대상 아님).
 */
@Getter
@Setter
public class RefusalTargetVO {

    /** 통지(NOTICED) 행의 REFUSAL_ID */
    private String refusalId;

    /** 노무수령거부 대상일 (YYYYMMDD) */
    private String targetYmd;
}
