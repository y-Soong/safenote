<!--
  AdminApprovalPendingList.vue — 승인 대기 리스트 (유형 칩 + 정렬 + 검색 + 카드)
  - 작업 ID: 001-P2-F2 (분해: 001-phase2-admin-approval-plan.md §2-1 / §3-B A-1)
  - web 단일 출처: Attd_10.vue 접수함 / 재기획서 05-screen-structure §5.4(필터·정렬)·§5.5(접수함)
  - 백엔드: GET /appApi/admin/approval/pending?group=&sort=&keyword=&page=&pageSize=
  - 유형 칩(요청서): 전체 / 스케줄수정* / 근태보정 / 초과근무 / 연차  (*스케줄수정 = A5 확정 전 비활성)
  - 정렬: 요청순(REQUESTED 최신) · 임박순(DEADLINE D-N, A1 deadline 확보 시)
  - 무한 스크롤: MyRequestsView 패턴 차용(IntersectionObserver) — 본 골격은 sentinel 마크업만, 관찰 로직은 developer.
  - 디자인 토큰: 부모(.admin-approval-view)에서 선언한 var(--...) 상속.
  - planner 라운드 스코프: template + style 완성. script 는 선언 + TODO(developer) 골격만.
  - developer 라운드 스코프(TODO):
      (1) GET pending 조회(group/sort/keyword/page) → items/counts/totalCount/hasMore 바인딩
      (2) 칩/정렬/검색 변경 시 재조회(첫 페이지부터)
      (3) 무한 스크롤(IntersectionObserver, MyRequestsView 참조) 연결
      (4) 카드 선택 → emit('select', item) / 총 건수 → emit('update:total', totalCount)
-->
<template>
  <div class="ap-pending">
    <!-- 유형 칩 필터(단일 선택 세그먼트) -->
    <div class="ap-chips" role="tablist" aria-label="요청 유형 필터">
      <button
        v-for="c in chips"
        :key="c.key"
        type="button"
        class="ap-chip"
        :class="{ 'is-active': activeGroup === c.key, 'is-disabled': c.disabled }"
        :disabled="c.disabled"
        :aria-selected="activeGroup === c.key"
        @click="onSelectGroup(c)"
      >
        {{ c.label }}
        <span v-if="!c.disabled && countOf(c.key) > 0" class="ap-chip__count">
          {{ countOf(c.key) }}
        </span>
      </button>
    </div>

    <!-- 정렬 + 검색 -->
    <div class="ap-toolbar">
      <input
        v-model.trim="keyword"
        type="search"
        class="ap-toolbar__search"
        placeholder="요청자명 · 사번 검색"
        @keyup.enter="onSearch"
      />
      <!-- 임박순 정렬은 A1(마감 기준일 소스) 미확정으로 항상 요청순 동작 → 무동작 오인 방지 위해 토글 숨김(Fix2). -->
      <button
        v-if="deadlineSortEnabled"
        type="button"
        class="ap-toolbar__sort"
        :aria-pressed="sort === 'DEADLINE'"
        @click="onToggleSort"
      >
        {{ sortLabel }}
      </button>
    </div>

    <!-- 본문 -->
    <div ref="bodyRef" class="ap-list">
      <!-- 로딩(첫 페이지) -->
      <p v-if="isLoading && items.length === 0" class="ap-state" aria-live="polite">
        불러오는 중...
      </p>

      <!-- 빈 상태: 전체 0 -->
      <p
        v-else-if="!isLoading && items.length === 0 && totalCount === 0"
        class="ap-state"
        aria-live="polite"
      >
        처리할 승인 요청이 없습니다.
      </p>

      <!-- 빈 상태: 필터 0 -->
      <div
        v-else-if="!isLoading && items.length === 0 && totalCount > 0"
        class="ap-state ap-state--filtered"
        aria-live="polite"
      >
        <p class="ap-state__text">조건에 맞는 요청이 없습니다.</p>
        <button type="button" class="ap-state__reset" @click="onResetFilters">필터 해제</button>
      </div>

      <!-- 카드 리스트 -->
      <template v-else>
        <AdminApprovalCard
          v-for="item in items"
          :key="item.reqId"
          :item="item"
          @click="$emit('select', item)"
        />
        <!-- 무한 스크롤 sentinel(관찰 로직은 developer) -->
        <div ref="sentinelRef" class="ap-sentinel" aria-hidden="true"></div>
        <p v-if="isLoadingMore" class="ap-state ap-state--more">불러오는 중...</p>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatYmdDisplay, formatDateTimeDisplay } from '@/utils/approvalFormat'

import AdminApprovalCard from './AdminApprovalCard.vue'

// select: 카드 선택(상세 이동) / update:total: 대기 총 건수(셸 배지)
const emit = defineEmits(['select', 'update:total'])

const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — MyRequestsView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 페이지 크기(MyRequestsView 동일 — 서버도 동일 기본값 가정, A7)
const PAGE_SIZE = 20

// 유형 칩(요청서). 스케줄수정은 029(SCHEDULE 처리경로 완성)으로 활성화.
//   유형 카드 라벨/요약은 서버(reqTypeNm/summaryLines)가 단일출처. 칩 label 은 필터 세그먼트 표기 전용.
const chips = [
  { key: 'ALL', label: '전체', disabled: false },
  { key: 'SCHEDULE', label: '스케줄수정', disabled: false },
  { key: 'CORRECTION', label: '근태보정', disabled: false },
  { key: 'OVERTIME', label: '초과근무', disabled: false },
  { key: 'LEAVE', label: '연차', disabled: false },
]
const activeGroup = ref('ALL')

// 정렬: REQUESTED(요청순 최신) | DEADLINE(임박순). 기본 요청순(재기획서 §5.4).
const sort = ref('REQUESTED')
const sortLabel = computed(() => (sort.value === 'DEADLINE' ? '마감 임박순' : '요청 최신순'))

// Fix2: A1(마감 기준일 소스) 미확정 → 백엔드가 DEADLINE 요청도 INSERT_DATE DESC 로만 정렬하므로
//   임박순 토글이 무동작으로 오인된다. A1 확정 전까지 토글을 숨긴다(백엔드 sort 파라미터/계약은 유지).
// TODO(developer): A1(마감 기준일) 소스 확정 후 deadlineSortEnabled = true 로 전환하여 임박순 토글을 활성화.
const deadlineSortEnabled = false

// 검색어(요청자명/사번)
const keyword = ref('')

// ── 데이터 상태(서버 응답으로 채움) ───────────────────────────────────────
const items = ref([]) // 정규화 요청 아이템[]
const counts = ref({}) // { ALL, CORRECTION, OVERTIME, LEAVE }
const totalCount = ref(0)
const hasMore = ref(false)
const page = ref(1)
const isLoading = ref(false)
const isLoadingMore = ref(false)

const bodyRef = ref(null)
const sentinelRef = ref(null)
let intersectionObserver = null

// 동시 호출 가드(칩/정렬/검색 빠른 연속 변경 등) — 가장 최신 요청만 반영
let inflightSeq = 0

// 칩별 건수 배지(서버 counts) — 비활성 칩은 표시 안 함
const countOf = (key) => Number(counts.value?.[key]) || 0

// 서버 pending 아이템 → 카드 표시용 정규화.
//   카드 template 은 *Display 파생 필드를 소비(planner 골격). 식별자는 토큰 전용이므로 전송하지 않는다.
//   선점 잠금(lockedYn)은 v1 미구현(사용자 확정) → 항상 false 로 강제(‘처리 중’ 배지 비노출).
const normalizeItem = (raw) => ({
  ...raw,
  targetYmdDisplay: raw.targetYmdDisplay || formatYmdDisplay(raw.targetYmd),
  reqDateDisplay: raw.reqDateDisplay || formatDateTimeDisplay(raw.reqDate),
  summaryLines: Array.isArray(raw.summaryLines) ? raw.summaryLines : [],
  selfYn: raw.selfYn === 'Y' || raw.selfYn === true ? 'Y' : 'N',
  lockedYn: false,
  lockedByNm: '',
})

// 칩/정렬/검색만 서버로 전달(식별자는 axios 인터셉터의 토큰 클레임 경유 — IDOR 차단, plan §3/§4).
const buildParams = (targetPage) => {
  const params = {
    group: activeGroup.value,
    sort: sort.value,
    page: targetPage,
    pageSize: PAGE_SIZE,
  }
  if (keyword.value) params.keyword = keyword.value
  return params
}

const loadPage = async (append = false) => {
  const mySeq = ++inflightSeq

  if (append) {
    if (!hasMore.value || isLoadingMore.value || isLoading.value) return
    isLoadingMore.value = true
  } else {
    page.value = 1
    items.value = []
    hasMore.value = false
    isLoading.value = true
  }

  try {
    const res = await api.get('/appApi/admin/approval/pending', {
      params: buildParams(page.value),
    })
    if (mySeq !== inflightSeq) return

    const data = res?.data || {}
    const fetched = (Array.isArray(data.items) ? data.items : []).map(normalizeItem)

    items.value = append ? items.value.concat(fetched) : fetched
    counts.value = data.counts || {}
    totalCount.value = typeof data.totalCount === 'number' ? data.totalCount : items.value.length
    hasMore.value = !!data.hasMore

    // 셸 배지용 총 건수(전체 대기 건수 = counts.ALL 우선, 없으면 totalCount).
    emit('update:total', Number(counts.value?.ALL ?? totalCount.value) || 0)

    if (hasMore.value) page.value += 1
  } catch (e) {
    if (mySeq !== inflightSeq) return
    // 401/403 은 axios 인터셉터가 처리. 그 외만 폴백 알림.
    console.error('[AdminApprovalPendingList] 대기 목록 조회 실패')
    const msg = resolveApiErrorMessage(
      e,
      '요청 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
    showAlert(msg)
    if (!append) {
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

// ── 무한 스크롤(IntersectionObserver — MyRequestsView 패턴) ────────────────
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

const reload = async () => {
  await loadPage(false)
  await nextTick()
  observeSentinel()
}

// ── 핸들러 ────────────────────────────────────────────────────────────────
const onSelectGroup = (chip) => {
  if (chip.disabled || activeGroup.value === chip.key) return
  activeGroup.value = chip.key
  reload()
}

const onToggleSort = () => {
  sort.value = sort.value === 'REQUESTED' ? 'DEADLINE' : 'REQUESTED'
  reload()
}

const onSearch = () => {
  reload()
}

const onResetFilters = () => {
  activeGroup.value = 'ALL'
  sort.value = 'REQUESTED'
  keyword.value = ''
  reload()
}

// ── 라이프사이클 ──────────────────────────────────────────────────────────
onMounted(async () => {
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
.ap-pending {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  min-height: 0;
}

/* 유형 칩 */
.ap-chips {
  display: flex;
  gap: var(--space-sm);
  overflow-x: auto;
  padding-bottom: var(--space-xs);
  -webkit-overflow-scrolling: touch;
}
.ap-chip {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  height: 34px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
}
.ap-chip.is-active {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
  font-weight: 700;
}
.ap-chip.is-disabled {
  background: var(--color-disabled-bg);
  color: var(--color-disabled-text);
  cursor: not-allowed;
}
.ap-chip__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 10px;
  font-weight: 700;
}
.ap-chip.is-active .ap-chip__count {
  background: var(--color-primary);
}

/* 정렬 + 검색 */
.ap-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.ap-toolbar__search {
  flex: 1;
  height: 40px;
  box-sizing: border-box;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 14px;
  font-family: inherit;
}
.ap-toolbar__search:focus {
  outline: none;
  border-color: var(--color-primary);
}
.ap-toolbar__sort {
  flex: 0 0 auto;
  height: 40px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
}

/* 리스트 */
.ap-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.ap-state {
  margin: 0;
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.ap-state--more {
  padding: 12px 0;
  font-size: 13px;
}
.ap-state--filtered {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
}
.ap-state__text {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.ap-state__reset {
  height: 36px;
  padding: 0 var(--space-lg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.ap-sentinel {
  height: 1px;
}
</style>
