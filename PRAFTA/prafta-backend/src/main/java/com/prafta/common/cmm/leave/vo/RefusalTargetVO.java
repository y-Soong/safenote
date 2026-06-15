package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 노무수령거부 대상일 판정 결과 1건 (PRAFTA-COM-001 / -008-B 차단 전환).
 *
 * <p>판정 소스가 NOTICED 로그 → {@code tb_user_leave_use} 촉진단계로 전환되었다(-008-B §3).
 * {@code selectLaborRefusalTarget} 가 3게이트(촉진단계∈{FIRST,SECOND} · 법정 본연차 · 시도 당일
 * 비휴일)를 모두 통과한 종일 CONFIRMED 연차 행을 1건 반환한다. 결과가 존재하면 노무수령거부
 * 차단 대상으로 본다(자발/비법정/휴일이면 결과가 비어 대상 아님).
 */
@Getter
@Setter
public class RefusalTargetVO {

    /** 차단 대상 연차사용 ID (tb_user_leave_use.LEAVE_ID, RELATED_LEAVE_ID 용 — selectLaborRefusalTarget). */
    private String leaveId;

    /** 노무수령거부 대상일 (YYYYMMDD) = leave_use.START_DATE(최종 확정일) */
    private String targetYmd;
}
