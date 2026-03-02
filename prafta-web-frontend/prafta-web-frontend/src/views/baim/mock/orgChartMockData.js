/**
 * 사업장 조직도(Org Chart) MOCK DATA
 * - 각 노드: 고유 키(orgNodeId) + 상위 노드 참조(parentOrgNodeId)로 계층 연결
 * - API 응답: 트리 구조 (root + children 재귀)
 * - DB 테이블: ORG_NODE_ID(PK), PARENT_ORG_NODE_ID(FK) 로 동일 구조
 */

// ================ 1. API 응답 형태 (트리) ================
// 노드 스키마: orgNodeId(고유키), parentOrgNodeId(상위노드키, 루트는 null), label, type, managerCnt, workerCnt, selfAttendanceApprovalYn, children[]
// GET /webApi/baim06/org-tree?siteCd=SITE01 응답 예시
export const orgTreeMockResponse = {
  root: {
    orgNodeId: "n1",
    parentOrgNodeId: null,
    label: "경영본부",
    type: "본부",
    managerCnt: 2,
    workerCnt: 0,
    selfAttendanceApprovalYn: true,
    children: [
      {
        orgNodeId: "n2",
        parentOrgNodeId: "n1",
        label: "인사팀",
        type: "팀",
        managerCnt: 1,
        workerCnt: 3,
        selfAttendanceApprovalYn: true,
        children: [
          {
            orgNodeId: "n3",
            parentOrgNodeId: "n2",
            label: "채용파트",
            type: "파트",
            managerCnt: 0,
            workerCnt: 5,
            selfAttendanceApprovalYn: false,
            children: [],
          },
          {
            orgNodeId: "n4",
            parentOrgNodeId: "n2",
            label: "교육파트",
            type: "파트",
            managerCnt: 1,
            workerCnt: 2,
            selfAttendanceApprovalYn: true,
            children: [],
          },
        ],
      },
      {
        orgNodeId: "n5",
        parentOrgNodeId: "n1",
        label: "재무팀",
        type: "팀",
        managerCnt: 1,
        workerCnt: 2,
        selfAttendanceApprovalYn: true,
        children: [
          {
            orgNodeId: "n6",
            parentOrgNodeId: "n5",
            label: "회계파트",
            type: "파트",
            managerCnt: 0,
            workerCnt: 4,
            selfAttendanceApprovalYn: false,
            children: [],
          },
        ],
      },
    ],
  },
};

// 사업장별로 다른 트리 예시 (다른 siteCd)
export const orgTreeMockBySite = {
  SITE01: orgTreeMockResponse.root,
  SITE02: {
    orgNodeId: "n101",
    parentOrgNodeId: null,
    label: "생산본부",
    type: "본부",
    managerCnt: 3,
    workerCnt: 0,
    selfAttendanceApprovalYn: true,
    children: [
      {
        orgNodeId: "n102",
        parentOrgNodeId: "n101",
        label: "제조팀",
        type: "팀",
        managerCnt: 2,
        workerCnt: 10,
        selfAttendanceApprovalYn: true,
        children: [],
      },
    ],
  },
};

// ================ 1-1. 플랫 배열 → 트리 변환 (DB 조회 결과 → orgTreeMockResponse 형태) ================
/**
 * DB에서 읽어온 플랫 배열을 트리 구조(root + children)로 변환
 * @param {Array} flatList - [{ orgNodeId, parentOrgNodeId, label, type, managerCnt, workerCnt, selfAttendanceApprovalYn, ... }]
 * @returns {{ root: Object | null }} - orgTreeMockResponse 와 동일한 형태
 */
export function flatToOrgTree(flatList) {
  if (!Array.isArray(flatList) || flatList.length === 0) return { root: null };

  const list = flatList.map((n) => ({
    ...n,
    children: [],
  }));
  const byId = new Map(list.map((n) => [n.orgNodeId, n]));

  const root = list.find((n) => n.parentOrgNodeId == null);
  if (!root) return { root: null };

  for (const node of list) {
    if (node.parentOrgNodeId == null) continue;
    const parent = byId.get(node.parentOrgNodeId);
    if (parent) parent.children.push(node);
  }

  const sortByOrder = (nodes) => {
    if (nodes.some((n) => n.sortOrder != null)) {
      nodes.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
    }
    nodes.forEach((n) => sortByOrder(n.children));
  };
  sortByOrder(root.children);

  return { root };
}

// ================ 1-2. 트리 → 플랫 배열 변환 (화면 노드 → DB 저장용 플랫 형태) ================
/**
 * 화면에 나와 있는 트리(root + children)를 플랫 배열로 변환
 * @param {Object} tree - 루트 노드 또는 { root: 루트노드 } 형태
 * @param {Object} [options] - { includeSortOrder: true, includeDepthLevel: true }
 * @returns {Array} - [{ orgNodeId, parentOrgNodeId, label, type, managerCnt, workerCnt, selfAttendanceApprovalYn, ... }]
 */
export function orgTreeToFlat(tree, options = {}) {
  const { includeSortOrder = true, includeDepthLevel = true } = options;
  const root = tree?.root ?? tree;
  if (!root || typeof root !== "object") return [];

  const result = [];

  function visit(node, parentNode, sortOrder) {
    const depthLevel =
      parentNode == null ? 0 : (parentNode.depthLevel ?? -1) + 1;
    const row = {
      orgNodeId: node.orgNodeId,
      parentOrgNodeId: parentNode?.orgNodeId ?? null,
      label: node.label,
      type: node.type,
      managerCnt: node.managerCnt ?? 0,
      workerCnt: node.workerCnt ?? 0,
      selfAttendanceApprovalYn: node.selfAttendanceApprovalYn ?? false,
    };
    if (includeSortOrder) row.sortOrder = sortOrder;
    if (includeDepthLevel) row.depthLevel = depthLevel;
    result.push(row);

    const children = Array.isArray(node.children) ? node.children : [];
    children.forEach((child, idx) =>
      visit(child, { ...node, depthLevel }, idx + 1)
    );
  }

  visit(root, null, 1);
  return result;
}

// ================ 2. DB 저장/조회용 플랫 구조 (테이블 설계 기준) ================
// 트리 노드를 한 행씩 저장할 때의 컬럼 예시
export const orgNodeFlatMock = [
  {
    orgNodeId: "n1",
    siteCd: "SITE01",
    parentOrgNodeId: null,
    label: "경영본부",
    type: "본부",
    managerCnt: 2,
    workerCnt: 0,
    selfAttendanceApprovalYn: "Y",
    sortOrder: 1,
    depthLevel: 0,
  },
  {
    orgNodeId: "n2",
    siteCd: "SITE01",
    parentOrgNodeId: "n1",
    label: "인사팀",
    type: "팀",
    managerCnt: 1,
    workerCnt: 3,
    selfAttendanceApprovalYn: "Y",
    sortOrder: 1,
    depthLevel: 1,
  },
  {
    orgNodeId: "n3",
    siteCd: "SITE01",
    parentOrgNodeId: "n2",
    label: "채용파트",
    type: "파트",
    managerCnt: 0,
    workerCnt: 5,
    selfAttendanceApprovalYn: "N",
    sortOrder: 1,
    depthLevel: 2,
  },
  {
    orgNodeId: "n4",
    siteCd: "SITE01",
    parentOrgNodeId: "n2",
    label: "교육파트",
    type: "파트",
    managerCnt: 1,
    workerCnt: 2,
    selfAttendanceApprovalYn: "Y",
    sortOrder: 2,
    depthLevel: 2,
  },
  {
    orgNodeId: "n5",
    siteCd: "SITE01",
    parentOrgNodeId: "n1",
    label: "재무팀",
    type: "팀",
    managerCnt: 1,
    workerCnt: 2,
    selfAttendanceApprovalYn: "Y",
    sortOrder: 2,
    depthLevel: 1,
  },
  {
    orgNodeId: "n6",
    siteCd: "SITE01",
    parentOrgNodeId: "n5",
    label: "회계파트",
    type: "파트",
    managerCnt: 0,
    workerCnt: 4,
    selfAttendanceApprovalYn: "N",
    sortOrder: 1,
    depthLevel: 2,
  },
];

// ================ 3. 테이블 설계 참고 (DDL 예시) ================
/*
-- 각 노드: 고유키(ORG_NODE_ID) + 상위노드키(PARENT_ORG_NODE_ID)로 계층 연결
-- PARENT_ORG_NODE_ID = NULL 이면 루트, 아니면 상위 행의 ORG_NODE_ID 참조
CREATE TABLE TB_BAIM_ORG_NODE (
    ORG_NODE_ID     VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '조직노드ID',
    SITE_CD         VARCHAR(20)  NOT NULL COMMENT '사업장코드',
    PARENT_ORG_NODE_ID VARCHAR(32)  NULL COMMENT '상위조직노드ID',
    LABEL           VARCHAR(100) NOT NULL COMMENT '구성요소명',
    TYPE_CD         VARCHAR(20)  NOT NULL COMMENT '구성요소유형(본부/팀/파트/기타)',
    MANAGER_CNT     INT NOT NULL DEFAULT 0 COMMENT '관리자수',
    WORKER_CNT      INT NOT NULL DEFAULT 0 COMMENT '근로자수',
    SELF_ATTENDANCE_APPROVAL_YN CHAR(1) NOT NULL DEFAULT 'N' COMMENT '자체근태승인여부(Y/N)',
    SORT_ORDER      INT NOT NULL DEFAULT 1 COMMENT '형제노드 정렬순서',
    DEPTH_LEVEL     INT NOT NULL DEFAULT 0 COMMENT '트리 깊이(0=루트)',
    CREATED_AT      DATETIME NULL,
    UPDATED_AT      DATETIME NULL,
    CONSTRAINT FK_ORG_PARENT FOREIGN KEY (PARENT_ORG_NODE_ID) REFERENCES TB_BAIM_ORG_NODE(ORG_NODE_ID),
    INDEX IDX_ORG_SITE (SITE_CD),
    INDEX IDX_ORG_PARENT (PARENT_ORG_NODE_ID)
) COMMENT '사업장 조직도 노드';

-- 조회 예시: 특정 사업장 트리 (플랫 → 트리 변환은 애플리케이션 또는 재귀 CTE로 처리)
-- SELECT * FROM TB_BAIM_ORG_NODE WHERE SITE_CD = ? ORDER BY DEPTH_LEVEL, SORT_ORDER;
*/

// ================ 4. 트리 조회용 쿼리 참고 (재귀 CTE, MySQL 8+ / Oracle / PostgreSQL) ================
/*
-- MySQL 8.0 재귀 CTE 예시: 특정 사업장의 전체 트리 플랫 조회
WITH RECURSIVE org_tree AS (
    SELECT ORG_NODE_ID, SITE_CD, PARENT_ORG_NODE_ID, LABEL, TYPE_CD,
           MANAGER_CNT, WORKER_CNT, SELF_ATTENDANCE_APPROVAL_YN, SORT_ORDER, DEPTH_LEVEL, 1 AS lv
    FROM TB_BAIM_ORG_NODE
    WHERE SITE_CD = 'SITE01' AND PARENT_ORG_NODE_ID IS NULL
    UNION ALL
    SELECT n.ORG_NODE_ID, n.SITE_CD, n.PARENT_ORG_NODE_ID, n.LABEL, n.TYPE_CD,
           n.MANAGER_CNT, n.WORKER_CNT, n.SELF_ATTENDANCE_APPROVAL_YN, n.SORT_ORDER, n.DEPTH_LEVEL, t.lv + 1
    FROM TB_BAIM_ORG_NODE n
    INNER JOIN org_tree t ON n.PARENT_ORG_NODE_ID = t.ORG_NODE_ID
)
SELECT * FROM org_tree ORDER BY lv, SORT_ORDER;
*/

export default orgTreeMockResponse;
