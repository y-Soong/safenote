package com.prafta.web.attd.attd07.result;

/**
 * 조직 노드(tb_site_node) 단건의 근태 승인 정책 조회 결과.
 *
 * <p>com-013-06-FU(r28): 웹 Attd07 직접 승인/반려 경로에서 노드별
 * {@code SELF_ATTD_APPRV_YN} 정책에 따라 자기처리/상위결재를 강제하기 위한 최소 필드.
 *
 * <ul>
 *   <li>{@code selfAttdApprvYn} — 자체근태승인여부('Y'/'N'). 노드 관리자가 자신의
 *       근태/OT를 자체 승인할 수 있는지.</li>
 *   <li>{@code parentNodeCd} — 부모 1단계 노드코드(없으면 null).</li>
 *   <li>{@code mainAdminCd} / {@code subAdminCd} — 해당 노드의 정/부 관리자(없으면 null).</li>
 * </ul>
 */
public record NodeApprovalInfoResult(
      String selfAttdApprvYn
    , String parentNodeCd
    , String mainAdminCd
    , String subAdminCd
) {
}
