<!--
  LeaveApprovalView.vue — 연차 결재 관리 (사용자 모드, 마이페이지 컨텍스트)
  - 작업: 사용자연차결재-02 / UI 명세: UI-LA01
  - 진입: MyPageView 결재 그룹 "연차 결재 관리" → router.push('/LeaveApproval')
  - 2탭(결재 대기/처리 내역). 관리자 헤더/탭바 미사용 — ApprovalPresetListView 사용자 모드 헤더 패턴.
  - 디자인 토큰: MyPageView/ApprovalPresetListView 세트를 .leave-approval-view 루트에 1회 선언.
-->
<template>
  <div class="leave-approval-view">
    <!-- 헤더 -->
    <header class="la-hd">
      <button type="button" class="la-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true"><use href="#i-la-chev-left" /></svg>
      </button>
      <h1 class="la-hd__title">연차 결재 관리</h1>
      <span class="la-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 탭 -->
    <nav class="la-tabs" role="tablist">
      <button
        type="button"
        class="la-tabs__btn"
        :class="{ 'is-active': activeTab === 'pending' }"
        role="tab"
        :aria-selected="activeTab === 'pending'"
        @click="switchTab('pending')"
      >
        결재 대기<span v-if="pendingCount > 0" class="la-tabs__count">{{ pendingCount }}</span>
      </button>
      <button
        type="button"
        class="la-tabs__btn"
        :class="{ 'is-active': activeTab === 'history' }"
        role="tab"
        :aria-selected="activeTab === 'history'"
        @click="switchTab('history')"
      >
        처리 내역
      </button>
    </nav>

    <!-- 검색 -->
    <div class="la-search">
      <input
        v-model="keyword"
        type="search"
        class="la-search__input"
        placeholder="요청자명 또는 사번 검색"
        @keyup.enter="onSearch"
      />
    </div>

    <!-- 본문 -->
    <main class="la-body">
      <div v-if="isLoading" class="la-loading" aria-live="polite">불러오는 중...</div>

      <template v-else>
        <!-- 빈 상태 -->
        <div v-if="items.length === 0" class="la-empty">
          <p class="la-empty__title">
            {{ activeTab === 'pending' ? '결재할 연차 요청이 없습니다' : '처리한 내역이 없습니다' }}
          </p>
        </div>

        <!-- 카드 리스트 -->
        <LeaveApprovalCard
          v-for="item in items"
          :key="item.reqId + '-' + item.approvalStep"
          :item="item"
          :mode="activeTab"
          @click="onCardClick(item)"
        />
      </template>
    </main>

    <!-- 인라인 SVG sprite -->
    <svg width="0" height="0" class="la-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-la-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

import LeaveApprovalCard from './components/LeaveApprovalCard.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// alert 폴백(앱 전역 우선) — ApprovalPresetListView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── UI 상태 ──
const activeTab = ref('pending') // 'pending' | 'history'
const keyword = ref('')
const isLoading = ref(true)

// ── 서버 주입 상태 ──
const items = ref([]) // GET /appApi/leaveflow/approval/{pending|history} 의 items
const pendingCount = ref(0) // 대기 탭 배지(대기 totalCount)

// 현재 탭 목록 조회. 대기/이력 분기. keyword 는 서버 전달(요청자명/사번 부분일치).
const loadList = async () => {
  isLoading.value = true
  try {
    const kw = keyword.value.trim() || undefined
    if (activeTab.value === 'pending') {
      const { data } = await api.get('/appApi/leaveflow/approval/pending', {
        params: { keyword: kw },
      })
      items.value = data?.items || []
      pendingCount.value = data?.totalCount ?? items.value.length
    } else {
      const { data } = await api.get('/appApi/leaveflow/approval/history', {
        params: { keyword: kw },
      })
      items.value = data?.items || []
    }
  } catch (e) {
    // 401/403 은 axios 인터셉터가 처리. 그 외만 폴백 알림.
    console.warn('[LeaveApproval] 목록 조회 실패:', e?.message)
    items.value = []
    await showAlert(
      resolveApiErrorMessage(e, '목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'),
    )
  } finally {
    isLoading.value = false
  }
}

// 탭 전환 시 검색어 초기화 후 해당 목록 재조회
const switchTab = (tab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  keyword.value = ''
  loadList()
}

// 검색 트리거 → 현재 탭 목록 재조회
const onSearch = () => {
  loadList()
}

// 카드 클릭 → 상세(reqId, approvalStep 전달)
const onCardClick = (item) => {
  router.push({
    path: '/LeaveApprovalDetail',
    query: { reqId: item.reqId, approvalStep: item.approvalStep },
  })
}

// 뒤로 → 마이페이지
const onBack = () => {
  router.push('/MyPage')
}

// 진입 시 대기 탭(디폴트) + pendingCount 조회
onMounted(() => {
  loadList()
})
</script>

<style scoped>
.leave-approval-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.06);
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.la-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}
.la-hd__back {
  width: 44px;
  height: 44px;
  margin-left: -10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.la-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}
.la-hd__spacer {
  width: 44px;
}

/* 탭 */
.la-tabs {
  flex-shrink: 0;
  display: flex;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.la-tabs__btn {
  flex: 1;
  height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-family: inherit;
}
.la-tabs__btn.is-active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}
.la-tabs__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 700;
  border-radius: var(--radius-full);
}

/* 검색 */
.la-search {
  flex-shrink: 0;
  padding: var(--space-md) var(--space-lg) 0;
  background: var(--color-bg);
}
.la-search__input {
  width: 100%;
  box-sizing: border-box;
  height: 40px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
}
.la-search__input:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 본문 */
.la-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-md) var(--space-lg) 40px;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.la-loading {
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.la-empty {
  padding: 48px var(--space-lg);
  text-align: center;
}
.la-empty__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.la-sprite {
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
