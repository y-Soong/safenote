package com.prafta.app.selfjoin.admin.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 관리자 모드 — 셀프가입 승인 화면의 부서 필터 칩 소스 응답.
 *
 * <p>앱은 웹의 "부서 찾기 팝업" 대신 칩으로 부서를 고른다. 노드관리자는 조회 EP 에 {@code nodeCd}
 * 를 반드시 실어야 서버 게이트({@code canManageNodeExcludeSafe})를 통과하므로, 고를 수 있는
 * 노드 목록을 서버가 내려준다.
 *
 * <ul>
 *   <li>{@code companyWide=true} (master/hr) — 전 부서 조회 가능. {@code nodes} 는 비워 내린다.</li>
 *   <li>{@code companyWide=false} + {@code nodes} 1건 이상 — 그 중 하나를 반드시 선택해 전송.</li>
 *   <li>{@code companyWide=false} + {@code nodes} 0건 — 관리 부서 없음. 화면은 목록 EP 를
 *       호출하지 않는다(호출해도 서버가 403).</li>
 * </ul>
 *
 * <p>PII 없음(부서 코드/명칭만).
 */
@Getter
@Builder
public class AppSelfJoinScopeNodesResponse {

    /**
     * 전사 조회 역할 여부(master/hr).
     *
     * <p>판정은 {@code AuthRoleUtils.isManager} 단일 출처를 쓴다 —
     * {@code canManageNodeExcludeSafe} 의 전사 통과 조건과 같은 함수여야 두 축이 어긋나지 않는다.
     * safe(안전관리자) 단독은 false 다(셀프가입 승인은 인사 행위).
     */
    private final boolean companyWide;

    /** 요청자가 정/부 관리자인 노드(seed) 목록. 자손은 서버가 {@code incSubNodeYn='Y'} 로 전개한다. */
    private final List<Node> nodes;

    /** 부서 칩 1건. */
    @Getter
    @Builder
    public static class Node {

        /** 노드(부서) 코드 */
        private final String nodeCd;

        /** 노드(부서) 명 */
        private final String nodeNm;
    }
}
