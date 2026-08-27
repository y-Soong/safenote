<!--
  AdminApprovalHistoryList.vue — 승인 이력 리스트 (유형 칩 + 검색 + 무한스크롤 + 표시전용 항목)
  - 작업 ID: 016-G-1 (분해: prafta-com-016-G.md §016-G-1)
  - web 단일 출처: 요청승인관리 재기획서 05-screen-structure §5.3(유형군)·§5.8(이력 표시)
  - 백엔드: GET /appApi/admin/approval/history?group=&keyword=&page=&pageSize=
      · 정렬은 서버 고정(PROCESS_DATE DESC) — sort 파라미터 없음.
      · 응답: { items:HistoryItem[], totalCount, hasMore }  ※ counts(그룹별 건수) 객체 없음.
      · startDate/endDate 는 BE 가 받으나 v1 UI 미노출(파라미터 미전송).
  - 유형 칩: 전체 / 스케줄수정 / 근태보정 / 초과근무 / 연차 (app-029 SCHEDULE='10' 포함).
      · 이력은 counts 가 없으므로 칩 카운트 배지 미표시.
  - 항목은 표시 전용(클릭/네비게이션 없음, v1 결정) — emit 없음. 항목 마크업은 본 파일 내부에 인라인.
  - 패턴 차용: AdminApprovalPendingList.vue 의 칩·검색·무한스크롤(IntersectionObserver)·
      동시호출 가드(inflightSeq)·에러폴백(resolveApiErrorMessage/showAlert).
  - 디자인 토큰: 부모(.admin-approval-view)에서 선언한 var(--...) 상속. 하드코딩 색/픽셀 금지.
-->
<template>
  <div class="ap-pending">
    <!-- 유형 칩 필터(단일 선택 세그먼트). 이력은 카운트 배지 없음. -->
    <div class="ap-chips" role="tablist" aria-label="요청 유형 필터">
      <button
        v-for="c in chips"
        :key="c.key"
        type="button"
        class="ap-chip"
        :class="{ 'is-active': activeGroup === c.key }"
        :aria-selected="activeGroup === c.key"
        @click="onSelectGroup(c)"
      >
        {{ c.label }}
      </button>
    </div>

    <!-- 검색 -->
    <div class="ap-toolbar">
      <input
        v-model.trim="keyword"
        type="search"
        class="ap-toolbar__search"
        placeholder="요청자명 · 사번 검색"
        @keyup.enter="onSearch"
      />
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
        처리된 승인 이력이 없습니다.
      </p>

      <!-- 빈 상태: 필터 0 -->
      <div
        v-else-if="!isLoading && items.length === 0 && totalCount > 0"
        class="ap-state ap-state--filtered"
        aria-live="polite"
      >
        <p class="ap-state__text">조건에 맞는 이력이 없습니다.</p>
        <button type="button" class="ap-state__reset" @click="onResetFilters">필터 해제</button>
      </div>

      <!-- 이력 항목 리스트(표시 전용 — 클릭/네비게이션 없음) -->
      <template v-else>
        <article
          v-for="item in items"
          :key="item.reqId"
          class="ap-history"
        >
          <!-- 상단: 유형명 + 처리상태 배지 -->
          <header class="ap-history__top">
            <span class="ap-history__type">{{ item.reqTypeNm || '-' }}</span>
            <span class="ap-history__status" :class="statusClass(item.reqStatus)">
              {{ item.reqStatusNm || '-' }}
            </span>
          </header>

          <!-- 본문: 요청자 · 부서 · 대상일 -->
          <div class="ap-history__body">
            <p class="ap-history__requester">
              <span class="ap-history__requester-name">{{ item.requesterUserNm || '-' }}</span>
              <span v-if="item.nodeNm" class="ap-history__requester-dept">{{ item.nodeNm }}</span>
            </p>
            <p v-if="targetDisplay(item)" class="ap-history__target">{{ targetDisplay(item) }}</p>
          </div>

          <!-- 반려 사유(반려 '03' 건만) -->
          <div v-if="item.reqStatus === '03' && item.rejectReason" class="ap-history__reject">
            <span class="ap-history__reject-label">반려 사유</span>
            <span class="ap-history__reject-body">{{ item.rejectReason }}</span>
          </div>

          <!-- 처리 정보: 처리자 · 처리일시 -->
          <footer class="ap-history__meta">
            <span class="ap-history__meta-user">처리자 {{ item.processUserNm || '-' }}</span>
            <span class="ap-history__meta-date">{{ processDisplay(item) }}</span>
          </footer>
        </article>

        <!-- 무한 스크롤 sentinel -->
        <div ref="sentinelRef" class="ap-sentinel" aria-hidden="true"></div>
        <p v-if="isLoadingMore" class="ap-state ap-state--more">불러오는 중...</p>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatYmdDisplay, formatDateTimeDisplay } from '@/utils/approvalFormat'

const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — PendingList 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 페이지 크기(대기 탭과 동일)
const PAGE_SIZE = 20

// 유형 칩(대기 탭과 동일 세트). app-029 로 SCHEDULE('10') 이력 포함.
//   이력은 counts(그룹별 건수)가 없으므로 칩 카운트 배지를 표시하지 않는다.
const chips = [
  { key: 'ALL', label: '전체' },
  { key: 'SCHEDULE', label: '스케줄수정' },
  { key: 'CORRECTION', label: '근태보정' },
  { key: 'OVERTIME', label: '초과근무' },
  { key: 'LEAVE', label: '연차' },
  { key: 'DEFAULT_SCH_CHANGE', label: '기본근무타입변경' },
]
const activeGroup = ref('ALL')

// 검색어(요청자명/사번)
const keyword = ref('')

// ── 데이터 상태(서버 응답으로 채움) ───────────────────────────────────────
// 이력은 counts 객체가 없어 칩 배지를 표시하지 않으므로 counts 상태 미보유.
const items = ref([]) // 이력 아이템[]
const totalCount = ref(0)
const hasMore = ref(false)
const page = ref(1)
const isLoading = ref(false)
const isLoadingMore = ref(false)

const bodyRef = ref(null)
const sentinelRef = ref(null)
let intersectionObserver = null

// 동시 호출 가드(칩/검색 빠른 연속 변경 등) — 가장 최신 요청만 반영
let inflightSeq = 0

// 처리상태 배지 색상(reqStatus → SYS033: 02승인 / 03반려 / 04취소).
//   라벨은 서버 reqStatusNm 단일출처, 색만 클라이언트 분기.
const statusClass = (reqStatus) => {
  switch (reqStatus) {
    case '02':
      return 'ap-history__status--primary'
    case '03':
      return 'ap-history__status--danger'
    case '04':
      return 'ap-history__status--neutral'
    default:
      return 'ap-history__status--neutral'
  }
}

// 대상일(YYYYMMDD) → "YYYY.MM.DD". 빈 값은 빈 문자열(템플릿에서 v-if 로 graceful).
const targetDisplay = (item) => formatYmdDisplay(item?.targetYmd)

// 처리일시(타임스탬프) → "YYYY.MM.DD HH:mm". 빈 값은 '-' 로 graceful.
const processDisplay = (item) => formatDateTimeDisplay(item?.processDate) || '-'

// 칩/검색/페이지만 서버로 전달(식별자는 axios 인터셉터의 토큰 클레임 경유 — IDOR 차단).
//   정렬은 서버 고정(PROCESS_DATE DESC) → sort 미전송. startDate/endDate v1 미노출 → 미전송.
const buildParams = (targetPage) => {
  const params = {
    group: activeGroup.value,
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
    const res = await api.get('/appApi/admin/approval/history', {
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
    // 401/403 은 axios 인터셉터가 처리. 그 외만 폴백 알림.
    console.error('[AdminApprovalHistoryList] 이력 목록 조회 실패')
    const msg = resolveApiErrorMessage(
      e,
      '승인 이력을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
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

// ── 무한 스크롤(IntersectionObserver — PendingList 패턴) ────────────────────
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
  if (activeGroup.value === chip.key) return
  activeGroup.value = chip.key
  reload()
}

const onSearch = () => {
  reload()
}

const onResetFilters = () => {
  activeGroup.value = 'ALL'
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

/* 검색 */
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

/* 이력 항목(표시 전용 — 카드 클릭 없음) */
.ap-history {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}
.ap-history__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}
.ap-history__type {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.ap-history__status {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.ap-history__status--primary {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.ap-history__status--danger {
  background: var(--color-danger-tint);
  color: var(--color-danger-text);
}
.ap-history__status--neutral {
  background: var(--color-disabled-bg);
  color: var(--color-text-secondary);
}

.ap-history__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ap-history__requester {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.ap-history__requester-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.ap-history__requester-dept {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.ap-history__target {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}

.ap-history__reject {
  background: var(--color-danger-tint);
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  color: var(--color-danger-text);
  line-height: 17px;
}
.ap-history__reject-label {
  font-weight: 600;
  margin-right: 6px;
}

.ap-history__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 0.5px solid var(--color-border-light);
  font-size: 12px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}
</style>
