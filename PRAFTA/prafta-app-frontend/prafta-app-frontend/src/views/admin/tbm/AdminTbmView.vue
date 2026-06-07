<!--
  AdminTbmView.vue — 관리자 모드 TBM 관리 (4탭 셸: 교육관리 / 교육개설 / 교육자료 관리 / 이력)
  - 작업 ID: 001-P5-T-F1 (분해: .claude/requests/app_requests/001-phase5-admin-tbm-plan.md §2 / §4)
  - 진입: AdminLauncherView/AdminTabBar 의 TBM 섹션·탭 → router.push('/AdminTbm') (보호 라우트, developer)
      현재 Phase1 은 /ComingSoon?module=TBM 으로 라우팅됨 → Phase5 에서 /AdminTbm 으로 교체.
  - 권한 단일 출처: 001_관리자모드-권한매트릭스.md §3(TBM = master ∥ safe ∥ nodeAdmin, hr ⛔) / §2(노드관리자 🔵 자기노드+자손)
      ⚠️ C1: 진입 게이팅은 access-context.moduleActiveMap.TBM(서버 산출)로 상위(AdminLauncher)에서 처리.
              본 화면은 클라이언트 역할(AUTH_CD) 분기 없음.
  - 디폴트 탭 = 교육 관리(요청서). 탭 전환은 UI 토글(허용 범위). 각 탭 데이터 조회는 자식 컴포넌트가 담당.
  - 디자인 토큰: TbmHubView/AdminLauncherView 세트를 .admin-tbm-view 루트에 1회 선언.
      자식(리스트/카드/폼)은 scoped 상태로 var(--...) 상속. 하드코딩 색/픽셀 금지.
  - planner 라운드 스코프: template + style 완성. script 는 선언 + TODO(developer) 골격만.
  - developer 라운드 스코프(TODO):
      (1) /AdminTbm 보호 라우트 추가 + AdminLauncher/AdminTabBar 의 TBM → /AdminTbm 연결
      (2) 교육관리 카드 선택 → 세션 상세(/AdminTbmSessionDetail) 라우팅
      (3) 개설 폼 개설/임시저장 성공 → 세션 상세 라우팅(비번 표시)
      (4) 자료/이력 탭 자식 조회 위임(자식이 담당) — 셸은 탭 전환만
-->
<template>
  <div class="admin-tbm-view">
    <!-- 헤더: 관리자 모드(런처) 복귀 + 타이틀 -->
    <header class="admin-tbm-hd">
      <button type="button" class="admin-tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-admin-tbm-chev-left" />
        </svg>
      </button>
      <h1 class="admin-tbm-hd__title">TBM 관리</h1>
      <span class="admin-tbm-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 탭바 (4탭, 디폴트=교육관리) -->
    <nav class="admin-tbm-tabs" role="tablist" aria-label="TBM 관리 탭">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        class="admin-tbm-tabs__btn"
        :class="{ 'is-active': activeTab === t.key }"
        role="tab"
        :aria-selected="activeTab === t.key"
        @click="activeTab = t.key"
      >
        {{ t.label }}
      </button>
    </nav>

    <!-- 본문: 선택 탭 -->
    <main class="admin-tbm-body">
      <!-- 탭1 교육 관리(디폴트) -->
      <AdminTbmManageList v-if="activeTab === 'MANAGE'" @select="onSelectSession" />

      <!-- 탭2 교육 개설 -->
      <AdminTbmCreateForm
        v-else-if="activeTab === 'CREATE'"
        @created="onSessionCreated"
        @close="onCreateClose"
      />

      <!-- 탭3 교육자료 관리 (R5) -->
      <AdminTbmMaterialList
        v-else-if="activeTab === 'MATERIAL'"
        @select="onSelectMaterial"
        @create="onCreateMaterial"
      />

      <!-- 탭4 이력 (R6) -->
      <AdminTbmHistoryList v-else @select="onSelectHistory" />
    </main>

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="admin-tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-admin-tbm-chev-left"
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

import AdminTbmManageList from './components/AdminTbmManageList.vue'
import AdminTbmCreateForm from './components/AdminTbmCreateForm.vue'
import AdminTbmMaterialList from './components/AdminTbmMaterialList.vue'
import AdminTbmHistoryList from './components/AdminTbmHistoryList.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — MainView/AdminLauncher 패턴 동일
// eslint-disable-next-line no-unused-vars
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 탭 정의(고정). 디폴트 = MANAGE(교육 관리, 요청서).
const tabs = [
  { key: 'MANAGE', label: '교육 관리' },
  { key: 'CREATE', label: '교육 개설' },
  { key: 'MATERIAL', label: '교육자료' },
  { key: 'HISTORY', label: '이력' },
]
const activeTab = ref('MANAGE')

// ── 액션 ──────────────────────────────────────────────────────────
// 관리자 모드(런처) 복귀
const onBack = () => {
  router.replace('/AdminHome')
}

// 교육관리 카드 선택 → 세션 상세 이동(개설된 교육이면 비번 표시 + 상태별 액션은 상세 화면에서 처리)
const onSelectSession = (session) => {
  if (!session?.sessionCd) return
  router.push({
    path: '/AdminTbmSessionDetail',
    query: { sessionCd: session.sessionCd },
  })
}

// 개설/임시저장 성공 → 세션 상세 이동(개설된 교육 = 동일 상세 + 비번)
const onSessionCreated = (result) => {
  if (!result?.sessionCd) return
  router.push({
    path: '/AdminTbmSessionDetail',
    query: { sessionCd: result.sessionCd },
  })
}

// 개설 폼 닫기(미저장 삭제 얼럿은 폼 내부에서 confirm 처리) → 교육관리 탭으로 복귀
const onCreateClose = () => {
  activeTab.value = 'MANAGE'
}

// 탭3 교육자료: 카드 선택 → 자료 상세 / "자료 등록" → 자료 등록 폼(mtrlCd 없이)
const onSelectMaterial = ({ mtrlCd } = {}) => {
  if (!mtrlCd) return
  router.push({ path: '/AdminTbmMaterialDetail', query: { mtrlCd } })
}
const onCreateMaterial = () => {
  router.push({ path: '/AdminTbmMaterialForm' })
}

// 탭4 이력: 카드 선택 → 이력 상세
const onSelectHistory = ({ sessionCd } = {}) => {
  if (!sessionCd) return
  router.push({ path: '/AdminTbmHistoryDetail', query: { sessionCd } })
}
</script>

<style scoped>
/* 디자인 토큰 1회 선언(TbmHubView/AdminLauncherView 세트) — 자식 scoped 가 상속 */
.admin-tbm-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
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
.admin-tbm-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.admin-tbm-hd__back {
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
.admin-tbm-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-tbm-hd__spacer {
  width: 36px;
}

/* 탭바 (4탭) */
.admin-tbm-tabs {
  display: flex;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.admin-tbm-tabs__btn {
  flex: 1;
  height: 44px;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.admin-tbm-tabs__btn.is-active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 700;
}

/* 본문 */
.admin-tbm-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
}

/* 자료/이력 탭 placeholder(후속 골격 R5/R6) */
.admin-tbm-placeholder {
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
.admin-tbm-placeholder__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.admin-tbm-placeholder__sub {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 스프라이트 */
.admin-tbm-sprite {
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
