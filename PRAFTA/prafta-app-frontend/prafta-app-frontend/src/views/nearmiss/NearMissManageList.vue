<!--
  NearMissManageList.vue — 관리자 사업장 사건 목록 (모바일 앱, 신규)
  - 작업 ID: PRAFTA-app-012-4 (분해: .claude/requests/app_requests/prafta-app-012-plan.md)
  - UI 명세: UI-app-012-2 (.claude/requests/app_requests/prafta-app-012-ui-spec.md)
  - 설계 출처: .claude/context/near-miss-incident-design.md §5-B(목록)
  - planner 라운드 스코프: 헤더 + 상태탭(카운트) + 카드 리스트 + 빈/로딩 상태. template/style 완성.
  - developer 라운드 스코프: A3 목록/A4 카운트 호출, 탭 필터, 카드 클릭 라우팅, 403/에러 처리.
  - 디자인 토큰: MainView 세트를 .near-miss-list 루트에 1회 선언.
  - 참조: MyRequestsView(헤더/리스트), MyAttendance(탭/카운트).
-->
<template>
  <div class="near-miss-list">
    <!-- 헤더 -->
    <header class="nml-hd">
      <button type="button" class="nml-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-nml-chev-left" />
        </svg>
      </button>
      <h1 class="nml-hd__title">사건 관리</h1>
      <span class="nml-hd__site">{{ siteName }}</span>
    </header>

    <!-- 상태 탭 (카운트 배지) -->
    <nav class="nml-tabs" aria-label="처리상태 필터">
      <button
        v-for="tab in statusTabs"
        :key="tab.code"
        type="button"
        class="nml-tab"
        :class="{ 'nml-tab--on': activeStatus === tab.code }"
        @click="onSelectStatus(tab.code)"
      >
        <span class="nml-tab__label">{{ tab.label }}</span>
        <span class="nml-tab__count">{{ tab.count }}</span>
      </button>
    </nav>

    <!-- 본문(스크롤) -->
    <main class="nml-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="nml-loading" aria-live="polite">불러오는 중...</div>

      <!-- 빈 상태 -->
      <div v-else-if="items.length === 0" class="nml-empty">표시할 사건이 없어요</div>

      <!-- 카드 리스트 -->
      <template v-else>
        <button
          v-for="item in items"
          :key="item.nearMissId"
          type="button"
          class="nml-card"
          @click="onCardClick(item)"
        >
          <div class="nml-card__top">
            <span
              class="nml-badge"
              :class="severityClass(item.potentialSeverityCd)"
            >
              {{ item.potentialSeverityNm || '미분류' }}
            </span>
            <span class="nml-card__id">{{ item.nearMissId }}</span>
          </div>
          <p class="nml-card__desc">{{ item.description }}</p>
          <div class="nml-card__meta">
            <span v-if="item.processNm">{{ item.processNm }}</span>
            <span>{{ item.reporterNm }}</span>
            <span>{{ item.occurDtime }}</span>
          </div>
          <span class="nml-chip" :class="statusClass(item.reportStatusCd)">
            {{ item.reportStatusNm }}
          </span>
        </button>
      </template>
    </main>

    <!-- 인라인 SVG 스프라이트 -->
    <svg width="0" height="0" class="nml-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-nml-chev-left"
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
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 헤더 사업장명 — 세션 직결 (gv_siteNm)
const siteName = ref('')

// ───────────────────────────────────────────────────────────
// 상태 탭 + 카운트 (A4 status-counts). 앱 관리자 조치범위는 100→200 까지라
// 탭은 접수/검토중/조치중 중심(완료는 웹). '' = 전체.
// ───────────────────────────────────────────────────────────
const activeStatus = ref('') // '' | '100' | '200' | '300'
const counts = ref({ receivedCnt: 0, reviewingCnt: 0, actingCnt: 0, completedCnt: 0 })

const statusTabs = computed(() => [
  { code: '', label: '전체', count: totalCount.value },
  { code: '100', label: '접수', count: counts.value.receivedCnt },
  { code: '200', label: '검토중', count: counts.value.reviewingCnt },
  { code: '300', label: '조치중', count: counts.value.actingCnt },
])

const totalCount = computed(() => {
  const c = counts.value
  return (c.receivedCnt || 0) + (c.reviewingCnt || 0) + (c.actingCnt || 0) + (c.completedCnt || 0)
})

// ───────────────────────────────────────────────────────────
// 리스트 상태
// ───────────────────────────────────────────────────────────
const items = ref([]) // IncidentResult[]
const isLoading = ref(false)

// 잠재중대성 배지 클래스 (경미/중대/치명)
const severityClass = (code) => {
  if (code === '300') return 'nml-badge--critical'
  if (code === '200') return 'nml-badge--major'
  if (code === '100') return 'nml-badge--minor'
  return 'nml-badge--none'
}

// 처리상태 칩 클래스
const statusClass = (code) => {
  if (code === '900') return 'nml-chip--rejected'
  if (code === '400') return 'nml-chip--done'
  return 'nml-chip--progress'
}

// ───────────────────────────────────────────────────────────
// 이벤트
// ───────────────────────────────────────────────────────────
const onBack = () => {
  router.back()
}

const onSelectStatus = (code) => {
  activeStatus.value = code
  // 선택 상태로 목록만 재조회(카운트는 필터 무관 전체 집계라 유지).
  loadIncidents()
}

const onCardClick = (item) => {
  router.push({ path: '/NearMissManageDetail', query: { nearMissId: item.nearMissId } })
}

// ───────────────────────────────────────────────────────────
// 데이터 로딩 (A3 목록 / A4 카운트). 식별자(siteCd 등)는 axios 인터셉터가 gv_* 자동 주입.
//   - 사업장 권한은 서버 assertSiteAccess 가 최종 판정 → 403 이면 안내 후 복귀.
// ───────────────────────────────────────────────────────────
const buildFilterParams = () => {
  const params = {}
  if (activeStatus.value) params.reportStatusCd = activeStatus.value
  return params
}

const loadIncidents = async () => {
  isLoading.value = true
  try {
    const res = await api.get('/appApi/nearmiss/site-incidents', { params: buildFilterParams() })
    items.value = res?.data?.incidentResultList || []
  } catch (err) {
    handleListError(err)
  } finally {
    isLoading.value = false
  }
}

const loadCounts = async () => {
  try {
    // 카운트는 상태 필터를 제외(전체 집계). 그 외 필터는 본 화면 미사용.
    const res = await api.get('/appApi/nearmiss/status-counts')
    counts.value = res?.data?.statusCount || {
      receivedCnt: 0,
      reviewingCnt: 0,
      actingCnt: 0,
      completedCnt: 0,
    }
  } catch (err) {
    // 카운트 실패는 목록 에러 처리에 위임(403 등은 목록에서 안내). 배지는 0 유지.
    console.warn('[NearMissManageList] 카운트 조회 실패:', err?.message)
  }
}

const handleListError = async (err) => {
  const status = err?.response?.status
  if (status === 403) {
    await showAlert('사건 관리 권한이 없어요')
    router.back()
    return
  }
  console.error('[NearMissManageList] 목록 조회 실패:', err?.message)
  showAlert(err?.response?.data?.message || '사건 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
}

// ───────────────────────────────────────────────────────────
// 라이프사이클
// ───────────────────────────────────────────────────────────
onMounted(() => {
  siteName.value = sessionStorage.getItem('gv_siteNm') || ''
  // 권한 게이팅은 서버(assertSiteAccess)가 최종 판정한다. 프론트는 정상 호출하고 403 을 안내 처리.
  loadCounts()
  loadIncidents()
})
</script>

<style scoped>
.near-miss-list {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
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
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;

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
.nml-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  display: grid;
  grid-template-columns: 44px 1fr auto;
  align-items: center;
  padding-right: 16px;
  position: sticky;
  top: 0;
  z-index: 10;
  padding-top: env(safe-area-inset-top);
}
.nml-hd__back {
  width: 44px;
  height: 44px;
  background: transparent;
  border: 0;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-family: inherit;
}
.nml-hd__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
.nml-hd__site {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* 상태 탭 */
.nml-tabs {
  flex-shrink: 0;
  display: flex;
  gap: 6px;
  padding: 10px 16px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  overflow-x: auto;
}
.nml-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  white-space: nowrap;
  font-family: inherit;
}
.nml-tab--on {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-tint);
  font-weight: 600;
}
.nml-tab__count {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

/* 본문 */
.nml-body {
  flex: 1;
  padding: 12px 16px 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.nml-loading,
.nml-empty {
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

/* 카드 */
.nml-card {
  display: block;
  width: 100%;
  text-align: left;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px;
  cursor: pointer;
  font-family: inherit;
}
.nml-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.nml-card__id {
  font-size: 12px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}
.nml-card__desc {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nml-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}
.nml-card__meta span:not(:last-child)::after {
  content: ' ·';
}

/* 잠재중대성 배지 */
.nml-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
}
.nml-badge--minor {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.nml-badge--major {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.nml-badge--critical {
  background: var(--color-danger-tint);
  color: var(--color-danger);
}
.nml-badge--none {
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
}

/* 처리상태 칩 */
.nml-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}
.nml-chip--progress {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
.nml-chip--done {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.nml-chip--rejected {
  background: var(--color-danger-tint);
  color: var(--color-danger);
}

.nml-sprite {
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
