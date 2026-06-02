package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 연차 현황 대시보드(attd09) 부서 필터 옵션.
 *
 * <p>정책서 결정 D-5: 부서 옵션 출처는 {@code tb_site_node}(회사 전체, CMPNY_CD 스코프).
 * 노드 타입 무관 전체 노드를 부서 후보로 노출하되, 실제 직원이 소속된 NODE_CD로 필터링한다.
 */
@Getter
@Setter
public class DeptOptionVO {

    /** 노드 코드 (tb_site_node.NODE_CD) */
    private String nodeCd;

    /** 노드명 */
    private String nodeNm;
}
