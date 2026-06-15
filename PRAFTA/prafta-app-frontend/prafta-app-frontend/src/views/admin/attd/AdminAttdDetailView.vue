<!--
  AdminAttdDetailView.vue — 관리자 모드 근태 상세 (2탭 셸: 일자 근태 / 월별 집계)
  - 작업 ID: prafta-app-025 J1-5 (.claude/requests/app_requests/job_1/J1-5-admin-attd-detail.md)
  - 진입: AdminLauncherView/AdminTabBar 의 근태(attd) 섹션·탭 → router.push('/AdminAttdDetail') (보호 라우트)
  - 권한 단일 출처: 001_관리자모드-권한매트릭스.md §3(근태상세 = master ∥ hr ∥ nodeAdmin, safe ⛔) / §2(노드관리자 자기노드+자손)
      ⚠️ C1: 진입 게이팅은 access-context.moduleActiveMap.ATTD_DETAIL(서버 산출)로 상위(AdminLauncher)에서 처리.
              본 화면은 클라이언트 역할(AUTH_CD) 분기 없음. 화면 진입 후 서버 EP 가 스코프 재강제.
  - MVP = 조회 전용 2탭(요청서 §2). 직접 근태 upsert/월마감 제외(승인은 AdminApproval 담당).
  - 디자인 토큰: AdminApprovalView 세트를 .admin-attd-view 루트에 1회 선언. 자식(리스트)은 scoped 상속.
-->
<template>
  <div class="admin-attd-view">
    <!-- 헤더: 관리자 모드(런처) 복귀 + 타이틀 -->
    <header class="admin-attd-hd">
      <button type="button" class="admin-attd-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-admin-attd-chev-left" />
        </svg>
      </button>
      <h1 class="admin-attd-hd__title">근태 상세</h1>
      <span class="admin-attd-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 탭바 (2탭, 디폴트=일자 근태) -->
    <nav class="admin-attd-tabs" role="tablist" aria-label="근태 상세 탭">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        class="admin-attd-tabs__btn"
        :class="{ 'is-active': activeTab === t.key }"
        role="tab"
        :aria-selected="activeTab === t.key"
        @click="activeTab = t.key"
      >
        {{ t.label }}
      </button>
    </nav>

    <!-- 본문: 선택 탭 -->
    <main class="admin-attd-body">
      <AdminAttdDailyList v-if="activeTab === 'DAILY'" />
      <AdminAttdMonthlyList v-else />
    </main>

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="admin-attd-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-admin-attd-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol
          id="i-admin-attd-chev-l"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol
          id="i-admin-attd-chev-r"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="9 18 15 12 9 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import AdminAttdDailyList from './components/AdminAttdDailyList.vue'
import AdminAttdMonthlyList from './components/AdminAttdMonthlyList.vue'

const router = useRouter()

// 탭 정의(고정). 디폴트 = DAILY(일자 근태 현황).
const tabs = [
  { key: 'DAILY', label: '일자 근태' },
  { key: 'MONTHLY', label: '월별 집계' },
]
const activeTab = ref('DAILY')

// 관리자 모드(런처) 복귀
const onBack = () => {
  router.replace('/AdminHome')
}
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminApprovalView 세트) — 자식 scoped 가 상속 */
.admin-attd-view {
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
.admin-attd-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.admin-attd-hd__back {
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
.admin-attd-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-attd-hd__spacer {
  width: 36px;
}

/* 탭바 (2탭) */
.admin-attd-tabs {
  display: flex;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.admin-attd-tabs__btn {
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
.admin-attd-tabs__btn.is-active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 700;
}

/* 본문 */
.admin-attd-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
}

/* 스프라이트 */
.admin-attd-sprite {
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
