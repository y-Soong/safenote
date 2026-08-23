<!--
  MyRequestsView.vue — 내 승인 요청 목록 화면 (모바일 앱)
  - 작업 ID: PRAFTA-APP-006-10 (분해: .claude/requests/app_requests/prafta-app-006-plan.md)
  - planner 라운드 스코프: 헤더 + 필터바 + 리스트 + 4종 시트 조립 (template/style 완성)
  - developer 라운드 스코프(아래): API 호출, 무한 스크롤, 시트 상태 전파, 라우팅
  - 디자인 토큰: MainView(.home-view) 와 동일 세트를 .my-requests-view 루트에 1회 선언.
-->
<template>
  <div class="my-requests-view">
    <!-- 헤더 -->
    <header class="req-hd">
      <button type="button" class="req-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-req-chev-left" />
        </svg>
      </button>
      <h1 class="req-hd__title">내 승인 요청</h1>
      <span class="req-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 필터 영역 -->
    <RequestFilterBar
      :total-count="totalCount"
      :filtered-count="filteredCount"
      :active-filters="activeFilters"
      :sort-label="currentSortLabel"
      @open-status-sheet="statusSheetOpen = true"
      @open-date-sheet="dateSheetOpen = true"
      @open-type-sheet="typeSheetOpen = true"
      @open-sort-sheet="sortSheetOpen = true"
      @reset="onResetFilters"
    />

    <!-- 본문 (스크롤 영역) -->
    <main
      class="req-body"
      ref="bodyRef"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출 -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <!-- 빈 상태 -->
      <RequestEmptyState v-if="items.length === 0 && !isLoading && totalCount === 0" kind="total" />
      <RequestEmptyState
        v-else-if="items.length === 0 && !isLoading && totalCount > 0"
        kind="filtered"
      />

      <!-- 카드 리스트 -->
      <template v-else>
        <RequestCard
          v-for="item in items"
          :key="item.reqId"
          :item="item"
          :current-site-cd="currentSiteCd"
          @click="onCardClick(item)"
        />
        <!-- 무한 스크롤 sentinel -->
        <div ref="sentinelRef" class="req-sentinel" aria-hidden="true"></div>
        <p v-if="isLoadingMore" class="req-load-more">불러오는 중...</p>
      </template>
    </main>

    <!-- 시트 4종 -->
    <RequestStatusFilterSheet
      v-model="statusSheetOpen"
      :selected="selectedStatuses"
      @apply="onApplyStatus"
    />
    <RequestDateRangeFilterSheet
      v-model="dateSheetOpen"
      :from="dateFrom"
      :to="dateTo"
      @apply="onApplyDate"
    />
    <RequestTypeFilterSheet
      v-model="typeSheetOpen"
      :selected="selectedTypes"
      @apply="onApplyType"
    />
    <RequestSortSheet v-model="sortSheetOpen" :selected="sort" @apply="onApplySort" />

    <!-- 결재라인 상세 (PRAFTA-내승인요청결재라인-2) -->
    <ApprovalLineDetailSheet
      v-model="approvalSheetOpen"
      :item="selectedItem"
      :current-site-cd="currentSiteCd"
    />

    <!-- 인라인 SVG sprite (본 화면 전용) -->
    <svg width="0" height="0" class="req-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-req-chev-left"
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
import { ref, computed, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { isDailyWorker } from '@/utils/employment'
import { getCurrentSiteCd } from '@/utils/currentSite'

import RequestFilterBar from './components/RequestFilterBar.vue'
import RequestCard from './components/RequestCard.vue'
import RequestEmptyState from './components/RequestEmptyState.vue'
import RequestStatusFilterSheet from './components/RequestStatusFilterSheet.vue'
import RequestDateRangeFilterSheet from './components/RequestDateRangeFilterSheet.vue'
import RequestTypeFilterSheet from './components/RequestTypeFilterSheet.vue'
import RequestSortSheet from './components/RequestSortSheet.vue'
import ApprovalLineDetailSheet from './components/ApprovalLineDetailSheet.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 작업지시서_소속이동-이력가시성-보정 T3: 화면 체류 중 불변(세션 값) — ref 불필요.
const currentSiteCd = getCurrentSiteCd()

// 공통: alert 폴백 (MainView 패턴 동일)
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 페이지 크기 (백엔드도 20 고정 — 클라이언트 측은 표시 목적)
const PAGE_LIMIT = 20

// ───────────────────────────────────────────────────────────
// 필터·정렬 상태
// ───────────────────────────────────────────────────────────
const selectedStatuses = ref([]) // string[] (SYS033 코드)
const dateFrom = ref('') // YYYYMMDD or ''
const dateTo = ref('')
const selectedTypes = ref([]) // string[] (SYS032 코드 01~06)
const sort = ref('PENDING_FIRST') // PENDING_FIRST | RECENT | TARGET_DATE

const SORT_LABELS = {
  PENDING_FIRST: '대기 우선',
  RECENT: '최근 요청순',
  TARGET_DATE: '대상일자 가까운순',
}
const currentSortLabel = computed(() => SORT_LABELS[sort.value] || SORT_LABELS.PENDING_FIRST)

const activeFilters = computed(() => ({
  statuses: selectedStatuses.value,
  dateFrom: dateFrom.value,
  dateTo: dateTo.value,
  types: selectedTypes.value,
}))

// ───────────────────────────────────────────────────────────
// 시트 open 상태
// ───────────────────────────────────────────────────────────
const statusSheetOpen = ref(false)
const dateSheetOpen = ref(false)
const typeSheetOpen = ref(false)
const sortSheetOpen = ref(false)

// ───────────────────────────────────────────────────────────
// 리스트 / 페이지네이션 상태
// ───────────────────────────────────────────────────────────
const items = ref([]) // MyReqItemResponse[]
const totalCount = ref(0)
const filteredCount = ref(0)
const hasMore = ref(false)
const offset = ref(0)
const isLoading = ref(false)
const isLoadingMore = ref(false)

const bodyRef = ref(null)
const sentinelRef = ref(null)
let intersectionObserver = null

// 동시 호출 가드 (시트 빠른 연속 적용 등)
let inflightSeq = 0

// ───────────────────────────────────────────────────────────
// API 호출
// ───────────────────────────────────────────────────────────
// 빈 배열은 파라미터 미전달 (백엔드가 기본값 적용 — plan §4.3 Q5)
const buildParams = (currentOffset) => {
  const params = {
    sort: sort.value,
    offset: currentOffset,
    limit: PAGE_LIMIT,
  }
  if (selectedTypes.value.length > 0) {
    params.reqTypes = selectedTypes.value.join(',')
  }
  if (selectedStatuses.value.length > 0) {
    params.reqStatuses = selectedStatuses.value.join(',')
  }
  if (dateFrom.value) params.targetYmdFrom = dateFrom.value
  if (dateTo.value) params.targetYmdTo = dateTo.value
  return params
}

const loadPage = async (append = false) => {
  // 동시 호출 가드 — 가장 최신 요청만 화면에 반영.
  const mySeq = ++inflightSeq

  if (append) {
    if (!hasMore.value || isLoadingMore.value || isLoading.value) return
    isLoadingMore.value = true
  } else {
    offset.value = 0
    items.value = []
    hasMore.value = false
    isLoading.value = true
  }

  try {
    const res = await api.get('/appApi/req06/my', { params: buildParams(offset.value) })
    // 최신 요청만 반영
    if (mySeq !== inflightSeq) return

    const data = res?.data || {}
    const fetched = Array.isArray(data.items) ? data.items : []

    if (append) {
      items.value = items.value.concat(fetched)
    } else {
      items.value = fetched
    }

    totalCount.value = typeof data.totalCount === 'number' ? data.totalCount : 0
    filteredCount.value = typeof data.filteredCount === 'number' ? data.filteredCount : 0
    hasMore.value = !!data.hasMore

    // 다음 페이지 offset 갱신 (서버 응답 길이 기준)
    offset.value = items.value.length
  } catch (e) {
    if (mySeq !== inflightSeq) return
    // 401/403 은 axios 인터셉터가 처리. 그 외만 폴백 알림.
    console.error('[MyRequests] 목록 조회 실패')
    const msg = resolveApiErrorMessage(
      e,
      '요청 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
    showAlert(msg)
    if (!append) {
      // 첫 페이지 실패 시 빈 상태로 보이지 않도록 표시 카운트는 그대로 둠
      items.value = []
      hasMore.value = false
    }
  } finally {
    if (mySeq === inflightSeq) {
      isLoading.value = false
      isLoadingMore.value = false
    }
  }
}

// ───────────────────────────────────────────────────────────
// 무한 스크롤 (IntersectionObserver)
//   sentinelRef 가 뷰포트(루트=bodyRef)에 진입하면 다음 페이지 로드.
//   필터 변경 시 카드 리스트가 새로 그려져 sentinel 이 분리/재생성될 수 있으므로
//   재관찰을 별도 함수로 분리.
// ───────────────────────────────────────────────────────────
const observeSentinel = () => {
  if (!intersectionObserver || !sentinelRef.value) return
  try {
    intersectionObserver.disconnect()
  } catch (_e) {
    // 무시
  }
  intersectionObserver.observe(sentinelRef.value)
}

const setupInfiniteScroll = () => {
  if (typeof IntersectionObserver === 'undefined') return
  intersectionObserver = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry?.isIntersecting && hasMore.value && !isLoadingMore.value && !isLoading.value) {
        loadPage(true)
      }
    },
    {
      root: bodyRef.value || null,
      rootMargin: '0px 0px 120px 0px',
      threshold: 0,
    },
  )
  observeSentinel()
}

// 카드 리스트가 새로 그려진 뒤 sentinel 을 다시 관찰
const reobserveAfterRender = async () => {
  await nextTick()
  if (sentinelRef.value) observeSentinel()
}

// ───────────────────────────────────────────────────────────
// 당겨서 새로고침 — 현재 필터/정렬 유지한 채 첫 페이지 재조회(부작용 없는 조회).
//   스크롤 컨테이너는 무한 스크롤 root 와 동일한 bodyRef 를 재사용한다(제스처는 touch 핸들러로 별개).
// ───────────────────────────────────────────────────────────
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(bodyRef, async () => {
  await loadPage(false)
  await reobserveAfterRender()
})

// ───────────────────────────────────────────────────────────
// 이벤트 핸들러
// ───────────────────────────────────────────────────────────
const onBack = () => {
  router.back()
}

const onApplyStatus = async (selected) => {
  selectedStatuses.value = Array.isArray(selected) ? [...selected] : []
  await loadPage(false)
  await reobserveAfterRender()
}

const onApplyDate = async ({ from, to }) => {
  dateFrom.value = from || ''
  dateTo.value = to || ''
  await loadPage(false)
  await reobserveAfterRender()
}

const onApplyType = async (selected) => {
  selectedTypes.value = Array.isArray(selected) ? [...selected] : []
  await loadPage(false)
  await reobserveAfterRender()
}

const onApplySort = async (newSort) => {
  sort.value = newSort
  await loadPage(false)
  await reobserveAfterRender()
}

const onResetFilters = async () => {
  selectedStatuses.value = []
  dateFrom.value = ''
  dateTo.value = ''
  selectedTypes.value = []
  // 정렬은 영향받지 않음 (plan §4.3)
  await loadPage(false)
  await reobserveAfterRender()
}

// 결재라인 상세 시트 상태 (PRAFTA-내승인요청결재라인-2)
const approvalSheetOpen = ref(false)
const selectedItem = ref(null)

// LC_MOVE/LC_DELETE 는 RequestCard.vue 가 애초에 click 을 emit 하지 않으므로
// (PRAFTA-내승인요청결재라인-3, 옵션 B) 여기서는 근태 요청(01~06,10)만 들어온다는 전제로 단순화.
const onCardClick = (item) => {
  selectedItem.value = item
  approvalSheetOpen.value = true
}

// ───────────────────────────────────────────────────────────
// 라이프사이클
// ───────────────────────────────────────────────────────────
onMounted(async () => {
  // prafta-app-025 J1-4: 일용직(DAILY)은 근태 요청/승인요청 대상이 아님 → 직접 진입 방어.
  //   정상 동선(메인 근태조회 카드 onApprovalClick)은 이미 숨겨졌으나 딥링크/직접 URL 진입을 차단한다.
  if (isDailyWorker()) {
    showAlert('일용직 사용자는 근태 조회 대상이 아닙니다.')
    router.replace('/MainView')
    return
  }
  await loadPage(false)
  await nextTick()
  setupInfiniteScroll()
})

onBeforeUnmount(() => {
  if (intersectionObserver) {
    try {
      intersectionObserver.disconnect()
    } catch (_e) {
      // 무시
    }
    intersectionObserver = null
  }
})
</script>

<style scoped>
/*
 * 디자인 토큰 — MainView 와 동일 세트를 .my-requests-view 에 1회 선언.
 * 자식 컴포넌트(scoped)는 var(--color-...) 를 상속받아 사용 가능.
 */
.my-requests-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
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
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);

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
.req-hd {
  height: 56px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
}
.req-hd__back {
  width: 44px;
  height: 44px;
  background: transparent;
  border: 0;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.req-hd__title {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.req-hd__spacer {
  width: 44px;
  height: 44px;
}

/* 본문 */
.req-body {
  flex: 1;
  min-height: 0;
  padding: 8px 16px 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.req-sentinel {
  height: 1px;
}

.req-load-more {
  margin: 8px 0;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.icon {
  display: block;
}
</style>
