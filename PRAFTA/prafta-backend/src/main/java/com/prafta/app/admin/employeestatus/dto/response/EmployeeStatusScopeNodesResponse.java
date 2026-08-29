package com.prafta.app.admin.employeestatus.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * PRAFTA-002: 직원 현황 화면의 부서 필터 칩 소스 응답.
 *
 * <p>{@code AppAdminSelfJoinController.getScopeNodes()}를 그대로 이식했다(plan §PRAFTA-002 상세 —
 * 동일 응답 구조·동일 게이트 조합).
 *
 * <ul>
 *   <li>{@code companyWide=true}(master/hr) — 전 부서 조회 가능. {@code nodes} 는 비워 내린다.</li>
 *   <li>{@code companyWide=false} + {@code nodes} 1건 이상 — 그 중 하나를 반드시 선택해 전송.</li>
 *   <li>{@code companyWide=false} + {@code nodes} 0건 — 관리 부서 없음.</li>
 * </ul>
 *
 * <p>PII 없음(부서 코드/명칭만).
 */
@Getter
@Builder
public class EmployeeStatusScopeNodesResponse {

    private final boolean companyWide;

    private final List<Node> nodes;

    @Getter
    @Builder
    public static class Node {
        private final String nodeCd;
        private final String nodeNm;
    }
}
