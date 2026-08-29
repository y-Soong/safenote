<!--
  AdminEmployeeStatusList.vue — 인원 현황 카드 리스트(무한 스크롤)
  - 작업 ID: PRAFTA-004 (UI 명세: UI-001)
  - 백엔드: GET /appApi/admin/employee-status/daily?workYmd=&siteCd=&nodeCd=&keyword=&page=&pageSize=
  - 상태 4종(근무중/미출근/휴무/퇴근) + 연차/외근 배지. 연차는 상태와 배타가 아닌 additive 배지(§UI 설계 결정 2).
  - 참조 패턴: AdminAttdDailyList.vue(날짜 무관 무한 스크롤 카드 셸을 그대로 이식, 날짜 자체는 부모 prop)
  - 디자인 토큰: 부모(.ees-view) 상속.
-->
<template>
  <div class="eesl">
    <p v-if="isLoading && items.length === 0" class="eesl-state" aria-live="polite">
      불러오는 중...
    </p>

    <p
      v-else-if="!isLoading && items.length === 0 && !keyword"
      class="eesl-state"
      aria-live="polite"
    >
      대상 인원이 없습니다.
    </p>

    <div
      v-else-if="!isLoading && items.length === 0 && keyword"
      class="eesl-state eesl-state--filtered"
      aria-live="polite"
    >
      <p class="eesl-state__text">조건에 맞는 직원이 없습니다.</p>
      <button type="button" class="eesl-state__reset" @click="$emit('reset-filters')">
        검색 해제
      </button>
    </div>

    <template v-else>
      <article v-for="item in items" :key="item.userCd" class="eec">
        <header class="eec__top">
          <span class="eec__name">{{ item.userNm || '-' }}</span>
          <span v-if="item.nodeNm" class="eec__dept">{{ item.nodeNm }}</span>
        </header>

        <div class="eec__status-row">
          <!-- 연차(휴무+종일연차)면 연차를 주 라벨로 강조(UI 설계 결정 3) -->
          <span
            v-if="item.status === 'DAY_OFF' && item.isOnLeave"
            class="eec__badge eec__badge--leave"
          >
            연차
          </span>
          <span v-else class="eec__badge" :class="badgeClass(item.status)">
            {{ statusLabel(item.status) }}
          </span>
          <span
            v-if="item.isOnLeave && item.status !== 'DAY_OFF'"
            class="eec__chip eec__chip--leave"
            >연차</span
          >
          <button
            v-if="item.isOffsite"
            type="button"
            class="eec__chip eec__chip--offsite"
            @click="$emit('select-offsite', item)"
          >
            외근 · 위치 보기
          </button>
        </div>

        <div class="eec__times">
          <div class="eec__time">
            <span class="eec__time-label">출근</span>
            <span class="eec__time-value">{{ fmtHm(item.checkInTime) }}</span>
          </div>
          <span class="eec__time-sep" aria-hidden="true">~</span>
          <div class="eec__time">
            <span class="eec__time-label">퇴근</span>
            <span class="eec__time-value">{{ fmtHm(item.checkOutTime) }}</span>
          </div>
        </div>
      </article>

      <div ref="sentinelRef" class="eesl-sentinel" aria-hidden="true"></div>
      <p v-if="isLoadingMore" class="eesl-state eesl-state--more">불러오는 중...</p>
    </template>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — AdminAttdDailyList 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

const props = defineProps({
  siteCd: { type: String, default: '' },
  nodeCd: { type: String, default: null },
  workYmd: { type: String, required: true },
  keyword: { type: String, default: '' },
})

defineEmits(['select-offsite', 'reset-filters'])

const PAGE_SIZE = 20

const items = ref([])
const totalCount = ref(0)
const hasMore = ref(false)
const page = ref(1)
const isLoading = ref(false)
const isLoadingMore = ref(false)

const sentinelRef = ref(null)
let intersectionObserver = null
let inflightSeq = 0

// HHMM → "HH:MM" (없으면 '-') — AdminAttdDailyList.fmtHm 동일
const fmtHm = (hm) => {
  if (!hm || String(hm).length < 4) return '-'
  const s = String(hm)
  return `${s.slice(0, 2)}:${s.slice(2, 4)}`
}

const STATUS_LABEL = {
  WORKING: '근무중',
  ABSENT: '미출근',
  DAY_OFF: '휴무',
  CHECKED_OUT: '퇴근',
}
const statusLabel = (status) => STATUS_LABEL[status] || '-'

const STATUS_BADGE_CLASS = {
  WORKING: 'eec__badge--working',
  ABSENT: 'eec__badge--absent',
  DAY_OFF: 'eec__badge--dayoff',
  CHECKED_OUT: 'eec__badge--checkedout',
}
const badgeClass = (status) => STATUS_BADGE_CLASS[status] || ''

// 조회 파라미터 — nodeCd 는 값이 있을 때만 싣는다(전사역할=null 은 사업장 전체, AdminSelfJoinPendingList 패턴 동일).
const buildParams = (targetPage) => {
  const params = { workYmd: props.workYmd, page: targetPage, pageSize: PAGE_SIZE }
  if (props.siteCd) params.siteCd = props.siteCd
  if (props.nodeCd) params.nodeCd = props.nodeCd
  if (props.keyword) params.keyword = props.keyword
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
    const res = await api.get('/appApi/admin/employee-status/daily', {
      params: buildParams(page.value),
    })
    if (mySeq !== inflightSeq) return

    const data = res?.data || {}
    const fetched = Array.isArray(data.items) ? data.items : []

    items.value = append ? items.value.concat(fetched) : fetched
    totalCount.value = typeof data.totalCount === 'number' ? data.totalCount : items.value.length
    hasMore.value = !!data.hasMore

    if (hasMore.value) page.value += 1
  } catch (e) {
    if (mySeq !== inflightSeq) return
    console.error('[AdminEmployeeStatusList] 인원 현황 조회 실패')
    const msg = resolveApiErrorMessage(
      e,
      '인원 현황을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
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

// ── 무한 스크롤 ─────────────────────────────────────────────────────────────
// root: null(뷰포트 기준) — 이 컴포넌트는 실제 스크롤 컨테이너(부모 .ees-body)에 대한
//   ref 를 갖지 않으므로 AdminAttdDailyList 처럼 자체 루트를 root 로 쓰지 않는다.
//   앱 웹뷰는 단일 폭 전체화면이라 뷰포트 기준으로도 동일하게 동작한다.
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
    { root: null, rootMargin: '0px 0px 120px 0px', threshold: 0 },
  )
  observeSentinel()
}

const reload = async () => {
  await loadPage(false)
  await nextTick()
  observeSentinel()
}

// 부모(셸)가 소유한 스코프/날짜/검색 prop 이 바뀌면 스스로 재조회한다(AdminSelfJoinPendingList 패턴 동일).
watch(
  () => [props.siteCd, props.nodeCd, props.workYmd, props.keyword],
  () => reload(),
)

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

defineExpose({ reload })
</script>

<style scoped>
.eesl {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  min-height: 0;
}
.eesl-state {
  margin: 0;
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.eesl-state--more {
  padding: 12px 0;
  font-size: 13px;
}
.eesl-state--filtered {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
}
.eesl-state__text {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.eesl-state__reset {
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
.eesl-sentinel {
  height: 1px;
}

/* 카드 */
.eec {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.eec__top {
  display: flex;
  align-items: baseline;
  gap: var(--space-sm);
}
.eec__name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.eec__dept {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.eec__status-row {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  flex-wrap: wrap;
}
.eec__badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 var(--space-sm);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 700;
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
.eec__badge--working {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.eec__badge--absent {
  background: var(--color-danger-tint);
  color: var(--color-danger-text);
}
.eec__badge--dayoff {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
.eec__badge--checkedout {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
.eec__badge--leave {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.eec__chip {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 var(--space-sm);
  border: 0;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 700;
  font-family: inherit;
  cursor: default;
}
.eec__chip--leave {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.eec__chip--offsite {
  background: var(--color-primary-tint);
  color: var(--color-primary);
  cursor: pointer;
}
.eec__times {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.eec__time {
  display: inline-flex;
  align-items: baseline;
  gap: var(--space-xs);
}
.eec__time-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.eec__time-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.eec__time-sep {
  color: var(--color-text-tertiary);
}
</style>
