package com.prafta.app.admin.common.scope.application.helper;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.prafta.app.admin.common.scope.application.query.ScopedNodeQuery;
import com.prafta.app.admin.common.scope.mapper.AdminScopeMapper;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [권한매트릭스 §0-1] ATTD_DETAIL 축(master ∥ hr ∥ nodeAdmin, safe 단독 ⛔) 데이터 스코프 판정 공용 헬퍼.
 *
 * <p>작업지시서_관리자앱-직원관리-신규화면.plan.md PRAFTA-002 상세가 "새 서비스에 이 판정을 그대로 복붙하지
 * 말고 공용 헬퍼로 추출해 공유하라"고 권장한 대상이다. {@code AppAdminAttdServiceImpl}(ATTD_DETAIL)과
 * {@code AppAdminEmployeeStatusServiceImpl}(EMPLOYEE_STATUS)이 동일 축을 쓰므로, "노드관리자 여부" 판정이
 * 두 곳에서 어긋나면 그 자체로 보안 결함이 된다(메모리 {@code feedback_web_new_query_screen_needs_node_gate}
 * — 신규 조회화면 노드 스코프 게이트 누락 3회 재발 이력). 원 구현은 {@code AppAdminAttdServiceImpl}에 있던
 * private {@code resolveScope}/{@code effectiveScopedNodes}이며, 판정 로직은 완전히 동일하게 이관했다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdminAttdLikeScopeResolver {

    private final AdminScopeMapper adminScopeMapper;

    /**
     * ATTD_DETAIL 축 스코프 산출: master=전사(회사 내 siteCd) / hr=사업장 / 노드관리자=자기노드+자손.
     * safe 단독 ⛔.
     */
    public ScopeContext resolveScope(String cmpnyCd, String userCd, String siteCd, String authCd) {
        if (AuthRoleUtils.isAccessDenied(authCd)) {
            log.warn("관리자 데이터 스코프 접근 차단(권한 없음) - authCd={}", authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        if (!StringUtils.hasText(siteCd)) {
            // 사업장 미지정이면 어느 축이든 노드/사업장 필터를 적용할 수 없다 → 차단.
            log.warn("관리자 데이터 스코프 사업장 미지정 - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        if (AuthRoleUtils.AUTH_MASTER.equals(authCd) || AuthRoleUtils.AUTH_HR_MANAGER.equals(authCd)) {
            // master/hr: 사업장 전부서(노드 무필터). companyWide 플래그는 매퍼에서 노드 필터 생략 의미로만 쓴다.
            return new ScopeContext(true, false, Collections.emptyList());
        }
        // safe 단독 또는 노드관리자 후보 → 노드 스코프. 노드관리자 아니면(빈 집합) 차단(safe ⛔).
        List<String> scopedNodeCds =
                adminScopeMapper.selectScopedNodeCds(ScopedNodeQuery.of(cmpnyCd, siteCd, userCd));
        if (scopedNodeCds == null || scopedNodeCds.isEmpty()) {
            log.warn("관리자 데이터 스코프 진입 권한 없음(노드관리자 아님/safe 단독) - authCd={}, siteCd={}", authCd, siteCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        return new ScopeContext(false, true, scopedNodeCds);
    }

    /**
     * 요청 nodeCd(있으면)를 토큰 스코프로 좁힌다(IDOR 재검증) — 목록 조회의 IN 필터 구성용.
     * <ul>
     *   <li>master/hr(companyWide): nodeCd 지정 시 단일 노드로 좁힘(사업장 내 임의 노드 허용).</li>
     *   <li>노드관리자(useNodeScope): nodeCd 가 자기 스코프 밖이면 빈 결과(403 대신 스코프 외 노출 0).
     *       nodeCd 미지정이면 전체 스코프 노드를 사용한다.</li>
     * </ul>
     * @return 매퍼에 실을 노드 IN 집합(companyWide 이고 nodeCd 미지정이면 null=무필터).
     */
    public List<String> effectiveScopedNodes(ScopeContext s, String nodeCd) {
        if (s.companyWide()) {
            if (StringUtils.hasText(nodeCd)) {
                return List.of(nodeCd);
            }
            return null;
        }
        if (StringUtils.hasText(nodeCd)) {
            if (s.scopedNodeCds().contains(nodeCd)) {
                return List.of(nodeCd);
            }
            log.warn("관리자 데이터 스코프 노드 위반(IDOR 차단) - 요청 nodeCd={}", nodeCd);
            return Collections.emptyList();
        }
        return s.scopedNodeCds();
    }

    /**
     * 특정 리소스가 이미 속한 nodeCd(요청 파라미터가 아니라 DB 조회로 확정된 값)가 내 스코프 안인지 단건 판정.
     * {@link #effectiveScopedNodes}는 "목록 필터" 용도라 nodeCd 미지정 시 무필터/전체스코프로 열리므로,
     * 단건 리소스 인가(예: GPS 궤적 조회 — PRAFTA-003)에는 이 메서드를 쓴다(용도 혼동 금지).
     */
    public boolean isNodeAllowed(ScopeContext s, String nodeCd) {
        if (s.companyWide()) {
            return true;
        }
        return StringUtils.hasText(nodeCd) && s.scopedNodeCds().contains(nodeCd);
    }

    public record ScopeContext(boolean companyWide, boolean useNodeScope, List<String> scopedNodeCds) {
    }
}
