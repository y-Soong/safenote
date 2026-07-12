<!--
  SafetyHubView.vue — 안전 허브 화면 (사용자 모드)
  - 작업 ID: prafta-app-025 J1-3
  - UI 명세: J1-3-safety-hub-view.md
  - 진입:
      (1) MainView › SafetyActivityCard › 헤더 ">"(click:detail) → router.push('/SafetyHub')
      (2) 바텀 탭바 '안전' 탭(click:tab 'safety') → router.push('/SafetyHub')
  - 성격: 안전 활동 3종(안전점검 / 위험성 발굴 / 아차사고 보고)의 진입 허브.
          MainView 안전 활동 카드의 하위 항목 성격을 그대로 화면 단위로 승격한 것.
  - 근무중 게이트(PRAFTA-022): 안전점검·위험성 발굴은 근무중(WORKING)에만 허용, 아차사고 보고는 항상 허용.
          blocked 판정은 home-summary attendance.status 로 산출(MainView 와 동일 정책).
  - 디자인 토큰: MainView .home-view 세트를 .safety-hub-view 루트에 1회 선언. 자식 scoped 상속. 하드코딩 금지.
-->
<template>
  <div class="safety-hub-view">
    <!-- 아이콘 스프라이트(본 화면 전역 use 참조 전제) -->
    <svg width="0" height="0" class="safety-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-safety-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol id="i-safety-chev-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6" />
        </symbol>
        <symbol id="i-safety-clipboard" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="8" y="2" width="8" height="4" rx="1" /><path d="M9 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-3" />
        </symbol>
        <symbol id="i-safety-camera" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" /><circle cx="12" cy="13" r="4" />
        </symbol>
        <symbol id="i-safety-alert" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" /><line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" />
        </symbol>
        <symbol id="i-safety-lock" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </symbol>
      </defs>
    </svg>

    <!-- 헤더 -->
    <header class="safety-hd">
      <button type="button" class="safety-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-safety-chev-left" />
        </svg>
      </button>
      <h1 class="safety-hd__title">안전 활동</h1>
      <span class="safety-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 본문 -->
    <main
      class="safety-body"
      ref="scrollRef"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출 -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <!-- 근무중 아님 차단 배너(안전점검·위험성 발굴만 차단) -->
      <div v-if="blocked" class="blocked-banner">
        <svg class="icon" width="14" height="14" aria-hidden="true">
          <use href="#i-safety-lock" />
        </svg>
        <span>안전점검과 위험성 발굴은 근무 중에만 이용할 수 있어요</span>
      </div>

      <!-- 액션 리스트 -->
      <div class="action-list">
        <button
          type="button"
          class="action-row"
          :class="{ 'action-row--disabled': blocked }"
          :disabled="blocked"
          @click="onSafetyCheck"
        >
          <svg class="icon row-icon" width="22" height="22" aria-hidden="true"><use href="#i-safety-clipboard" /></svg>
          <span class="row-text">
            <span class="row-label">안전점검 시작</span>
            <span class="row-desc">QR 스캔으로 점검 대상 확인 후 점검</span>
          </span>
          <svg class="icon row-chev" width="18" height="18" aria-hidden="true"><use href="#i-safety-chev-right" /></svg>
        </button>

        <button
          type="button"
          class="action-row"
          :class="{ 'action-row--disabled': blocked }"
          :disabled="blocked"
          @click="onRiskDiscovery"
        >
          <svg class="icon row-icon" width="22" height="22" aria-hidden="true"><use href="#i-safety-camera" /></svg>
          <span class="row-text">
            <span class="row-label">위험성 발굴</span>
            <span class="row-desc">현장 위험요인을 사진과 함께 등록</span>
          </span>
          <svg class="icon row-chev" width="18" height="18" aria-hidden="true"><use href="#i-safety-chev-right" /></svg>
        </button>

        <!-- 아차사고 보고 — 즉시성 예외(게이트 미적용, 항상 활성) -->
        <button type="button" class="action-row" @click="onNearMissReport">
          <svg class="icon row-icon" width="22" height="22" aria-hidden="true"><use href="#i-safety-alert" /></svg>
          <span class="row-text">
            <span class="row-label">아차사고 보고</span>
            <span class="row-desc">아차사고를 즉시 신고합니다</span>
          </span>
          <svg class="icon row-chev" width="18" height="18" aria-hidden="true"><use href="#i-safety-chev-right" /></svg>
        </button>
      </div>
    </main>

    <!-- 하단 탭바 (안전 활성) -->
    <AppBottomTabBar :active-tab="'safety'" />
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'
import AppBottomTabBar from '@/components/common/AppBottomTabBar.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — MainView 패턴 동일.
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 근무중 아님 차단 플래그(안전점검·위험성 발굴 게이트, PRAFTA-022).
//   home-summary attendance.status !== 'WORKING' 면 true.
const blocked = ref(false)

// 진입 시 근무상태 산출 — GET /appApi/home01/home-summary 의 attendance.status 로 blocked 판정.
//   MainView 의 safetyBlocked(attdStatus !== 'WORKING')와 동일 정책.
//   조회 실패 시 보수적으로 차단(blocked=true) — 근무 미확인 상태에서 안전점검/위험성 발굴 진입 방지.
const loadAttendanceState = async () => {
  try {
    const { data } = await api.get('/appApi/home01/home-summary')
    const status = data?.attendance?.status
    blocked.value = status !== 'WORKING'
  } catch (e) {
    // 401/403 등 토큰 에러는 axios 인터셉터가 강제 로그아웃 처리. 그 외 실패는 보수적 차단.
    console.warn('[SafetyHub] home-summary 조회 실패(보수적 차단):', e?.message)
    blocked.value = true
  }
}

// 당겨서 새로고침 — 스크롤 최상단에서 아래로 더 당기면 근무상태(blocked) 재조회(MainView 패턴).
const scrollRef = ref(null)
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(scrollRef, async () => {
  await loadAttendanceState()
})

onMounted(() => {
  loadAttendanceState()
})

// 뒤로가기 — 히스토리 있으면 back, 없으면 메인으로 replace.
const onBack = () => {
  if (window.history.length > 1) router.back()
  else router.replace('/MainView')
}

// "안전점검 시작" → /QrScanner (근무중에만)
const onSafetyCheck = () => {
  if (blocked.value) {
    showAlert('근무 중에만 이용할 수 있어요. 출근 후 다시 시도해 주세요.')
    return
  }
  router.push('/QrScanner')
}

// "위험성 발굴" → /Risk_01 (근무중에만)
const onRiskDiscovery = () => {
  if (blocked.value) {
    showAlert('근무 중에만 이용할 수 있어요. 출근 후 다시 시도해 주세요.')
    return
  }
  router.push('/Risk_01')
}

// "아차사고 보고" → /NearMissReport (즉시성 예외, 항상 허용)
const onNearMissReport = () => {
  router.push('/NearMissReport')
}
</script>

<style scoped>
/* 디자인 토큰 1회 선언(MainView .home-view 세트) — 자식 scoped 상속, 하드코딩 금지 */
.safety-hub-view {
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-on-danger: #ffffff;
  --color-warning-tint: #fffbeb;
  --color-warning-text-strong: #92400e;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-lg: 14px;
  --radius-full: 9999px;

  height: 100vh;
  height: 100dvh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.safety-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 8px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
}
.safety-hd__back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  min-height: 44px;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
}
.safety-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}
.safety-hd__spacer {
  min-width: 44px;
}

/* 본문 — 탭바(72px)에 가려지지 않도록 하단 패딩(MainView .main 패턴) */
.safety-body {
  flex: 1;
  min-height: 0;
  padding: 16px 16px 88px;
  overflow-y: auto;
}

.blocked-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: var(--color-warning-tint);
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--color-warning-text-strong);
}

.action-list {
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 4px 16px;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 16px 2px;
  background: transparent;
  border: 0;
  border-top: 0.5px solid var(--color-border-light);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.action-row:first-child {
  border-top: 0;
}
.row-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.row-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.row-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.row-icon {
  color: var(--color-primary);
}
.row-chev {
  color: var(--color-text-tertiary);
}
.action-row--disabled {
  cursor: not-allowed;
}
.action-row--disabled .row-label,
.action-row--disabled .row-icon {
  color: var(--color-text-tertiary);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
.safety-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
</style>
