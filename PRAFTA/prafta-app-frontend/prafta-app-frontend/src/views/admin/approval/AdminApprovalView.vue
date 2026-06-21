<!--
  AdminApprovalView.vue — 관리자 모드 승인 관리 (2탭 셸: 승인 대기 / 승인 이력)
  - 작업 ID: 001-P2-F1 (분해: .claude/requests/app_requests/001-phase2-admin-approval-plan.md §2 / §6)
  - 진입: AdminLauncherView/AdminTabBar 의 승인(approval) 섹션·탭 → router.push('/AdminApproval') (보호 라우트, developer)
      현재 Phase1 은 /ComingSoon?module=APPROVAL 로 라우팅됨 → Phase2 에서 /AdminApproval 로 교체.
  - 권한 단일 출처: 001_관리자모드-권한매트릭스.md §3(승인 = master ∥ hr ∥ nodeAdmin, safe ⛔) / §2(노드관리자 🔵 자기노드+자손)
      ⚠️ C1: 진입 게이팅은 access-context.moduleActiveMap.APPROVAL(서버 산출)로 상위(AdminLauncher)에서 처리.
              본 화면은 클라이언트 역할(AUTH_CD) 분기 없음.
  - 디폴트 탭 = 승인 대기(요청서). 탭 전환은 UI 토글(허용 범위). 각 탭 데이터 조회는 자식 컴포넌트가 담당.
  - 디자인 토큰: AdminTbmView/MyRequestsView 세트를 .admin-approval-view 루트에 1회 선언.
      자식(리스트/카드)은 scoped 상태로 var(--...) 상속. 하드코딩 색/픽셀 금지.
  - planner 라운드 스코프: template + style 완성. script 는 선언 + TODO(developer) 골격만.
  - developer 라운드 스코프(TODO):
      (1) /AdminApproval 보호 라우트 추가 + AdminLauncher/AdminTabBar 의 APPROVAL → /AdminApproval 연결
      (2) 대기 카드 선택 → 상세(/AdminApprovalDetail?reqId&group) 라우팅 + 선점 lock(§7.2, A2)
      (3) 이력 탭 = AdminApprovalHistoryList(후속 골격 F7) 작성 후 placeholder 교체
      (4) 탭 전환 시 자식 조회 위임(자식이 담당) — 셸은 탭 전환만
-->
<template>
  <div class="admin-approval-view">
    <!-- 헤더: 관리자 모드(런처) 복귀 + 타이틀 -->
    <header class="admin-approval-hd">
      <button type="button" class="admin-approval-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-admin-approval-chev-left" />
        </svg>
      </button>
      <h1 class="admin-approval-hd__title">승인 관리</h1>
      <span class="admin-approval-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 탭바 (2탭, 디폴트=승인 대기) -->
    <nav class="admin-approval-tabs" role="tablist" aria-label="승인 관리 탭">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        class="admin-approval-tabs__btn"
        :class="{ 'is-active': activeTab === t.key }"
        role="tab"
        :aria-selected="activeTab === t.key"
        @click="activeTab = t.key"
      >
        {{ t.label }}
        <span v-if="t.key === 'PENDING' && pendingTotal > 0" class="admin-approval-tabs__badge">
          {{ pendingTotal }}
        </span>
      </button>
    </nav>

    <!-- 본문: 선택 탭 -->
    <main class="admin-approval-body">
      <!-- 탭1 승인 대기(디폴트) -->
      <AdminApprovalPendingList
        v-if="activeTab === 'PENDING'"
        @select="onSelectPending"
        @update:total="onPendingTotal"
      />

      <!-- 탭2 승인 이력 (016-G-1: 표시 전용 리스트 — 클릭/네비게이션 없음) -->
      <AdminApprovalHistoryList v-else />
    </main>

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="admin-approval-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-admin-approval-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import AdminApprovalPendingList from './components/AdminApprovalPendingList.vue'
import AdminApprovalHistoryList from './components/AdminApprovalHistoryList.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — MainView/AdminLauncher 패턴 동일
// eslint-disable-next-line no-unused-vars
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 탭 정의(고정). 디폴트 = PENDING(승인 대기, 요청서).
const tabs = [
  { key: 'PENDING', label: '승인 대기' },
  { key: 'HISTORY', label: '승인 이력' },
]
const activeTab = ref('PENDING')

// 대기 탭 배지용 총 건수(자식 PendingList 가 emit) — 표시 전용
const pendingTotal = ref(0)
const onPendingTotal = (total) => {
  pendingTotal.value = Number(total) || 0
}

// ── 액션 ──────────────────────────────────────────────────────────
// 관리자 모드(런처) 복귀
const onBack = () => {
  router.replace('/AdminHome')
}

// 대기 카드 선택 → 상세 이동.
//   선점 잠금(§7.2, A2)은 v1 미구현(사용자 확정) → lock 호출 없이 상세로 push.
const onSelectPending = (item) => {
  if (!item?.reqId) return
  router.push({
    path: '/AdminApprovalDetail',
    query: { reqId: item.reqId, group: item.group },
  })
}
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminTbmView/MyRequestsView 세트) — 자식 scoped 가 상속 */
.admin-approval-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-disabled-bg: #f3f4f6;
  --color-disabled-text: #9ca3af;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100%;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.admin-approval-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.admin-approval-hd__back {
  width: 36px;
  height: 36px;
  margin-left: -8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.admin-approval-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-approval-hd__spacer {
  width: 36px;
}

/* 탭바 (2탭) */
.admin-approval-tabs {
  display: flex;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.admin-approval-tabs__btn {
  position: relative;
  flex: 1;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.admin-approval-tabs__btn.is-active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 700;
}
.admin-approval-tabs__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 11px;
  font-weight: 700;
}

/* 본문 */
.admin-approval-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
}

/* 이력 탭 placeholder(후속 골격 F7) */
.admin-approval-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 160px;
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
}
.admin-approval-placeholder__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.admin-approval-placeholder__sub {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
  text-align: center;
  line-height: 1.5;
}

/* 스프라이트 */
.admin-approval-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
