package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * TB_LEAVE_POLICY_HISTORY 단건 운반체.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.2 (정책 변경 이력 보존)
 *
 * <p>PREV_SNAPSHOT / NEW_SNAPSHOT / IMPACT_SUMMARY는 JSON 컬럼이지만
 * 화면 표시 및 외부 응답에서는 raw 문자열 그대로 직렬화한다 (재가공 불필요).
 */
@Getter
@Setter
public class LeavePolicyHistoryVO {

    /** 이력 ID (PK, varchar(20)) */
    private String histId;

    /** 회사 코드 */
    private String cmpnyCd;

    /** 변경된 정책 일련번호 */
    private Long policySeq;

    /** 변경 유형: CREATE / UPDATE / PRESET_CHANGE */
    private String changeType;

    /** 변경 전 정책 전체 스냅샷 (JSON 문자열) */
    private String prevSnapshot;

    /** 변경 후 정책 전체 스냅샷 (JSON 문자열) */
    private String newSnapshot;

    /** 변경 사유 */
    private String changeReason;

    /** 영향 분석 결과 (JSON 문자열) */
    private String impactSummary;

    /** 입력자 */
    private String insertNo;

    /** 입력일시 */
    private String insertDate;
}
