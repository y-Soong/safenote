<!--
  AdminAttdMonthlyList.vue — 월별 집계 (월 네비 + 검색 + 직원별 집계 카드)
  - 작업 ID: prafta-app-025 J1-5
  - 백엔드: GET /appApi/admin/attd-detail/monthly?yearMonth=&nodeCd=&keyword=&page=&pageSize=
  - 직원별 근무일수/근무시간/지각·조퇴 카운트. 식별자/스코프는 서버 강제(토큰·노드). PII 미노출(이름·노드명).
  - 디자인 토큰: 부모(.admin-attd-view) 상속. 무한 스크롤은 일자 리스트 패턴 동일.
-->
<template>
  <div class="aam">
    <!-- 월 네비 -->
    <div class="aam-monthnav">
      <button type="button" class="aam-monthnav__btn" aria-label="이전 달" @click="shiftMonth(-1)">
        <svg class="icon" width="20" height="20" aria-hidden="true"><use href="#i-admin-attd-chev-l" /></svg>
      </button>
      <div class="aam-monthnav__center">
        <span class="aam-monthnav__label">{{ monthLabel }}</span>
        <button v-if="!isThisMonth" type="button" class="aam-monthnav__today" @click="goThisMonth">
          이번 달
        </button>
      </div>
      <button
        type="button"
        class="aam-monthnav__btn"
        aria-label="다음 달"
        :disabled="isThisMonth"
        @click="shiftMonth(1)"
      >
        <svg class="icon" width="20" height="20" aria-hidden="true"><use href="#i-admin-attd-chev-r" /></svg>
      </button>
    </div>

    <!-- 검색 -->
    <div class="aam-toolbar">
      <input
        v-model.trim="keyword"
        type="search"
        class="aam-toolbar__search"
        placeholder="직원명 검색"
        @keyup.enter="onSearch"
      />
    </div>

    <!-- 본문 -->
    <div ref="bodyRef" class="aam-list">
      <p v-if="isLoading && items.length === 0" class="aam-state" aria-live="polite">불러오는 중...</p>

      <p
        v-else-if="!isLoading && items.length === 0 && !keyword"
        class="aam-state"
        aria-live="polite"
      >
        해당 월의 근태 기록이 없습니다.
      </p>

      <div
        v-else-if="!isLoading && items.length === 0 && keyword"
        class="aam-state aam-state--filtered"
        aria-live="polite"
      >
        <p class="aam-state__text">조건에 맞는 직원이 없습니다.</p>
        <button type="button" class="aam-state__reset" @click="onResetFilters">검색 해제</button>
      </div>

      <template v-else>
        <article v-for="item in items" :key="item.userCd" class="aamc">
          <header class="aamc__top">
            <span class="aamc__name">{{ item.userNm || '-' }}</span>
            <span v-if="item.nodeNm" class="aamc__dept">{{ item.nodeNm }}</span>
          </header>

          <div class="aamc__metrics">
            <div class="aamc__metric">
              <span class="aamc__metric-value">{{ item.workDays || 0 }}</span>
              <span class="aamc__metric-label">근무일</span>
            </div>
            <div class="aamc__metric">
              <span class="aamc__metric-value">{{ workHourLabel(item) }}</span>
              <span class="aamc__metric-label">근무시간</span>
            </div>
            <div class="aamc__metric">
              <span class="aamc__metric-value" :class="{ 'is-warn': (item.lateCnt || 0) > 0 }">
                {{ item.lateCnt || 0 }}
              </span>
              <span class="aamc__metric-label">지각</span>
            </div>
            <div class="aamc__metric">
              <span class="aamc__metric-value" :class="{ 'is-warn': (item.earlyCnt || 0) > 0 }">
                {{ item.earlyCnt || 0 }}
              </span>
              <span class="aamc__metric-label">조퇴</span>
            </div>
          </div>
        </article>

        <div ref="sentinelRef" class="aam-sentinel" aria-hidden="true"></div>
        <p v-if="isLoadingMore" class="aam-state aam-state--more">불러오는 중...</p>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

const PAGE_SIZE = 20

// ── 월 상태(YYYYMM) ─────────────────────────────────────────────────────────
const pad2 = (n) => String(n).padStart(2, '0')
const now = new Date()
const thisMonth = `${now.getFullYear()}${pad2(now.getMonth() + 1)}`

const yearMonth = ref(thisMonth)
const isThisMonth = computed(() => yearMonth.value === thisMonth)
const monthLabel = computed(() => {
  const y = yearMonth.value
  if (!y || y.length !== 6) return ''
  return `${y.slice(0, 4)}.${y.slice(4, 6)}`
})

const keyword = ref('')

// ── 데이터 상태 ─────────────────────────────────────────────────────────────
const items = ref([])
const totalCount = ref(0)
const hasMore = ref(false)
const page = ref(1)
const isLoading = ref(false)
const isLoadingMore = ref(false)

const bodyRef = ref(null)
const sentinelRef = ref(null)
let intersectionObserver = null
let inflightSeq = 0

// 근무 분 → "Nh" / "Nh Mm" (0/미산정이면 '-')
const workHourLabel = (item) => {
  const m = Number(item?.workMinutes) || 0
  if (m <= 0) return '-'
  const h = Math.floor(m / 60)
  const mm = m % 60
  if (h > 0 && mm > 0) return `${h}h ${mm}m`
  if (h > 0) return `${h}h`
  return `${mm}m`
}

const buildParams = (targetPage) => {
  const params = { yearMonth: yearMonth.value, page: targetPage, pageSize: PAGE_SIZE }
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
    const res = await api.get('/appApi/admin/attd-detail/monthly', { params: buildParams(page.value) })
    if (mySeq !== inflightSeq) return

    const data = res?.data || {}
    const fetched = Array.isArray(data.items) ? data.items : []

    items.value = append ? items.value.concat(fetched) : fetched
    totalCount.value = typeof data.totalCount === 'number' ? data.totalCount : items.value.length
    hasMore.value = !!data.hasMore

    if (hasMore.value) page.value += 1
  } catch (e) {
    if (mySeq !== inflightSeq) return
    console.error('[AdminAttdMonthlyList] 월별 집계 조회 실패')
    const msg = resolveApiErrorMessage(e, '월별 집계를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
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

// ── 무한 스크롤 ─────────────────────────────────────────────────────────────
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
    { root: bodyRef.value || null, rootMargin: '0px 0px 120px 0px', threshold: 0 },
  )
  observeSentinel()
}

const reload = async () => {
  await loadPage(false)
  await nextTick()
  observeSentinel()
}

// ── 핸들러 ──────────────────────────────────────────────────────────────────
const shiftMonth = (delta) => {
  const y = Number(yearMonth.value.slice(0, 4))
  const m = Number(yearMonth.value.slice(4, 6))
  const dt = new Date(y, m - 1 + delta, 1)
  const next = `${dt.getFullYear()}${pad2(dt.getMonth() + 1)}`
  // 미래 달은 막는다(이번 달까지).
  if (next > thisMonth) return
  yearMonth.value = next
  reload()
}

const goThisMonth = () => {
  if (isThisMonth.value) return
  yearMonth.value = thisMonth
  reload()
}

const onSearch = () => {
  reload()
}

const onResetFilters = () => {
  keyword.value = ''
  reload()
}

// ── 라이프사이클 ────────────────────────────────────────────────────────────
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
.aam {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  min-height: 0;
}

/* 월 네비 */
.aam-monthnav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-xs) var(--space-sm);
}
.aam-monthnav__btn {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.aam-monthnav__btn:disabled {
  color: var(--color-disabled-text);
  cursor: not-allowed;
}
.aam-monthnav__center {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
}
.aam-monthnav__label {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.aam-monthnav__today {
  height: 26px;
  padding: 0 var(--space-sm);
  border: 1px solid var(--color-primary-tint-border);
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

/* 검색 */
.aam-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.aam-toolbar__search {
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
.aam-toolbar__search:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 리스트 */
.aam-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.aam-state {
  margin: 0;
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.aam-state--more {
  padding: 12px 0;
  font-size: 13px;
}
.aam-state--filtered {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
}
.aam-state__text {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.aam-state__reset {
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
.aam-sentinel {
  height: 1px;
}

/* 직원 집계 카드 */
.aamc {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.aamc__top {
  display: flex;
  align-items: baseline;
  gap: var(--space-sm);
}
.aamc__name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.aamc__dept {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.aamc__metrics {
  display: flex;
  align-items: stretch;
  gap: var(--space-sm);
}
.aamc__metric {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-xs);
  background: var(--color-bg);
  border-radius: var(--radius-md);
}
.aamc__metric-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.aamc__metric-value.is-warn {
  color: var(--color-warning-text);
}
.aamc__metric-label {
  font-size: 11px;
  color: var(--color-text-secondary);
}
</style>
