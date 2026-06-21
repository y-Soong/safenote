<!--
  AdminAttdDailyList.vue — 일자 근태 현황 (날짜 네비 + 검색 + 직원 카드 리스트)
  - 작업 ID: prafta-app-025 J1-5
  - 백엔드: GET /appApi/admin/attd-detail/daily?workYmd=&nodeCd=&keyword=&page=&pageSize=
  - 직원별 출/퇴근/지각/조퇴/외근 요약. 식별자/스코프는 서버 강제(토큰·노드). PII 미노출(이름·노드명).
  - 디자인 토큰: 부모(.admin-attd-view) 상속. 무한 스크롤은 AdminApprovalPendingList 패턴 차용.
-->
<template>
  <div class="aad">
    <!-- 날짜 네비 -->
    <div class="aad-datenav">
      <button type="button" class="aad-datenav__btn" aria-label="이전 날짜" @click="shiftDate(-1)">
        <svg class="icon" width="20" height="20" aria-hidden="true"><use href="#i-admin-attd-chev-l" /></svg>
      </button>
      <div class="aad-datenav__center">
        <span class="aad-datenav__label">{{ dateLabel }}</span>
        <button v-if="!isToday" type="button" class="aad-datenav__today" @click="goToday">오늘</button>
      </div>
      <button
        type="button"
        class="aad-datenav__btn"
        aria-label="다음 날짜"
        :disabled="isToday"
        @click="shiftDate(1)"
      >
        <svg class="icon" width="20" height="20" aria-hidden="true"><use href="#i-admin-attd-chev-r" /></svg>
      </button>
    </div>

    <!-- 검색 -->
    <div class="aad-toolbar">
      <input
        v-model.trim="keyword"
        type="search"
        class="aad-toolbar__search"
        placeholder="직원명 검색"
        @keyup.enter="onSearch"
      />
    </div>

    <!-- 본문 -->
    <div ref="bodyRef" class="aad-list">
      <p v-if="isLoading && items.length === 0" class="aad-state" aria-live="polite">불러오는 중...</p>

      <p
        v-else-if="!isLoading && items.length === 0 && !keyword"
        class="aad-state"
        aria-live="polite"
      >
        해당 일자의 근태 기록이 없습니다.
      </p>

      <div
        v-else-if="!isLoading && items.length === 0 && keyword"
        class="aad-state aad-state--filtered"
        aria-live="polite"
      >
        <p class="aad-state__text">조건에 맞는 직원이 없습니다.</p>
        <button type="button" class="aad-state__reset" @click="onResetFilters">검색 해제</button>
      </div>

      <template v-else>
        <article v-for="item in items" :key="item.userCd" class="aadc">
          <header class="aadc__top">
            <span class="aadc__name">{{ item.userNm || '-' }}</span>
            <span v-if="item.nodeNm" class="aadc__dept">{{ item.nodeNm }}</span>
          </header>

          <div class="aadc__times">
            <div class="aadc__time">
              <span class="aadc__time-label">출근</span>
              <span class="aadc__time-value">{{ fmtHm(item.checkInTime) }}</span>
            </div>
            <span class="aadc__time-sep" aria-hidden="true">~</span>
            <div class="aadc__time">
              <span class="aadc__time-label">퇴근</span>
              <span class="aadc__time-value">{{ fmtHm(item.checkOutTime) }}</span>
            </div>
            <span v-if="workHourLabel(item)" class="aadc__work">{{ workHourLabel(item) }}</span>
          </div>

          <div v-if="hasBadges(item)" class="aadc__badges">
            <span v-if="item.isLate" class="aadc__chip aadc__chip--late">지각</span>
            <span v-if="item.isEarly" class="aadc__chip aadc__chip--early">조퇴</span>
            <span v-if="item.isOffsite" class="aadc__chip aadc__chip--offsite">외근</span>
            <span v-if="item.slotCount > 1" class="aadc__chip aadc__chip--slot">2구간</span>
          </div>
        </article>

        <div ref="sentinelRef" class="aad-sentinel" aria-hidden="true"></div>
        <p v-if="isLoadingMore" class="aad-state aad-state--more">불러오는 중...</p>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatYmdDisplay } from '@/utils/approvalFormat'
import { dowShort } from '@/views/attd/attdFormat'

const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

const PAGE_SIZE = 20

// ── 날짜 상태(YYYYMMDD) ─────────────────────────────────────────────────────
const pad2 = (n) => String(n).padStart(2, '0')
const toYmd = (d) => `${d.getFullYear()}${pad2(d.getMonth() + 1)}${pad2(d.getDate())}`
const todayYmd = toYmd(new Date())

const workYmd = ref(todayYmd)
const isToday = computed(() => workYmd.value === todayYmd)
const dateLabel = computed(() => {
  const y = workYmd.value
  if (!y || y.length !== 8) return ''
  // 표시 통일(D1): 점 일자 + 요일은 attdFormat.dowShort 로 산출
  return `${formatYmdDisplay(y)} (${dowShort(null, y)})`
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

// HHMM → "HH:MM" (없으면 '-')
const fmtHm = (hm) => {
  if (!hm || String(hm).length < 4) return '-'
  const s = String(hm)
  return `${s.slice(0, 2)}:${s.slice(2, 4)}`
}

// 근무 분 → "Nh Mm" (0/미산정이면 빈 문자열)
const workHourLabel = (item) => {
  const m = Number(item?.workMinutes) || 0
  if (m <= 0) return ''
  const h = Math.floor(m / 60)
  const mm = m % 60
  return h > 0 ? `${h}시간 ${mm}분` : `${mm}분`
}

const hasBadges = (item) =>
  item.isLate || item.isEarly || item.isOffsite || (Number(item.slotCount) || 0) > 1

const buildParams = (targetPage) => {
  const params = { workYmd: workYmd.value, page: targetPage, pageSize: PAGE_SIZE }
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
    const res = await api.get('/appApi/admin/attd-detail/daily', { params: buildParams(page.value) })
    if (mySeq !== inflightSeq) return

    const data = res?.data || {}
    const fetched = Array.isArray(data.items) ? data.items : []

    items.value = append ? items.value.concat(fetched) : fetched
    totalCount.value = typeof data.totalCount === 'number' ? data.totalCount : items.value.length
    hasMore.value = !!data.hasMore

    if (hasMore.value) page.value += 1
  } catch (e) {
    if (mySeq !== inflightSeq) return
    console.error('[AdminAttdDailyList] 일자 근태 조회 실패')
    const msg = resolveApiErrorMessage(e, '근태 현황을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
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
const shiftDate = (deltaDays) => {
  const y = workYmd.value
  const dt = new Date(Number(y.slice(0, 4)), Number(y.slice(4, 6)) - 1, Number(y.slice(6, 8)))
  dt.setDate(dt.getDate() + deltaDays)
  const next = toYmd(dt)
  // 미래 날짜는 막는다(오늘까지).
  if (next > todayYmd) return
  workYmd.value = next
  reload()
}

const goToday = () => {
  if (isToday.value) return
  workYmd.value = todayYmd
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
.aad {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  min-height: 0;
}

/* 날짜 네비 */
.aad-datenav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-xs) var(--space-sm);
}
.aad-datenav__btn {
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
.aad-datenav__btn:disabled {
  color: var(--color-disabled-text);
  cursor: not-allowed;
}
.aad-datenav__center {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
}
.aad-datenav__label {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.aad-datenav__today {
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
.aad-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.aad-toolbar__search {
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
.aad-toolbar__search:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 리스트 */
.aad-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.aad-state {
  margin: 0;
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.aad-state--more {
  padding: 12px 0;
  font-size: 13px;
}
.aad-state--filtered {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
}
.aad-state__text {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.aad-state__reset {
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
.aad-sentinel {
  height: 1px;
}

/* 직원 카드 */
.aadc {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.aadc__top {
  display: flex;
  align-items: baseline;
  gap: var(--space-sm);
}
.aadc__name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.aadc__dept {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.aadc__times {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}
.aadc__time {
  display: inline-flex;
  align-items: baseline;
  gap: var(--space-xs);
}
.aadc__time-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.aadc__time-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.aadc__time-sep {
  color: var(--color-text-tertiary);
}
.aadc__work {
  margin-left: auto;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.aadc__badges {
  display: flex;
  gap: var(--space-xs);
  flex-wrap: wrap;
}
.aadc__chip {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 var(--space-sm);
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 700;
}
.aadc__chip--late {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.aadc__chip--early {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.aadc__chip--offsite {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.aadc__chip--slot {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
</style>
