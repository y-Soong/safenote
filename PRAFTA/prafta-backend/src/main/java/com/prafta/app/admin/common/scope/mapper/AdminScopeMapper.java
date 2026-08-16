package com.prafta.app.admin.common.scope.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.app.admin.common.scope.application.query.ScopedNodeQuery;
import com.prafta.app.admin.common.scope.result.AdminSeedNodeResult;

/**
 * 001-P1-B2: 관리자 데이터 스코프 공용 Mapper(노드 자손 전개).
 *
 * <p>[권한매트릭스 §2] 노드관리자 스코프 = 자기노드 + 모든 자손 노드. Phase 2~8 목록/집계 쿼리가
 * {@code <sql id="scopedNodeCds">} 조각을 {@code <include>} 로 재사용한다(단일 출처).
 *
 * <p>D2 확정: 재귀 CTE(WITH RECURSIVE) 사용. IX_NODE_PARENT 활용, 깊이 가드(<100)로 순환 방지.
 * <p>Phase 1 access-context 는 본 매퍼를 호출하지 않는다(EXISTS 판정만 필요). 선행 자산으로만 제공.
 */
@Mapper
public interface AdminScopeMapper {

    /**
     * 내가 정/부 관리자인 노드(seed) + 모든 자손 NODE_CD 집합 조회.
     * <p>Phase 2~8 에서는 직접 호출보다 {@code <include refid="...scopedNodeCds">} 로 IN 절에 임베드하는 것을 권장한다.
     */
    List<String> selectScopedNodeCds(ScopedNodeQuery query);

    /**
     * 내가 정/부 관리자인 노드(seed) 목록 — 코드 + 노드명.
     *
     * <p>{@link #selectScopedNodeCds} 와 <b>목적이 다르다</b>. 저쪽은 "조회 술어에 넣을 자손 전개
     * 집합"이고, 이쪽은 "화면이 하나를 고르게 할 seed 목록"이다. 자손을 섞으면 사용자가 자손 노드를
     * 골랐을 때도 통과해 칩 개수만 늘고 의미가 흐려진다 → 자손을 전개하지 않는다.
     *
     * <p>기존 {@code selectScopedNodeCds} 를 고치지 않는다(근태 상세 등 기존 호출부 회귀 방지).
     *
     * <p>반환 record 는 위치 매핑이다 — SELECT 컬럼 순서(nodeCd, nodeNm) 고정.
     */
    List<AdminSeedNodeResult> selectAdminSeedNodes(ScopedNodeQuery query);
}
