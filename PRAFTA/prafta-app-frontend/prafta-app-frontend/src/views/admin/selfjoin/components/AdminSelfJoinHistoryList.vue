<!--
  AdminSelfJoinHistoryList.vue — 셀프가입 처리 이력 리스트(표시 전용, 무한스크롤)
  - 작업 ID: A6
  - 백엔드: GET /appApi/admin/self-join/history?siteCd=&nodeCd=&incSubNodeYn=Y&userKeyword=
              &actionType=&page=&pageSize=
      · 정렬은 서버 고정(처리일시 DESC, AUDIT_ID DESC) — sort 파라미터 없음.
      · ★응답은 { historyList, totalCount } 다. items/hasMore 가 아니다.
        hasMore 는 클라이언트가 (누적 건수 < totalCount) 로 계산한다.
      · startDate/endDate 는 BE 가 받으나 v1 UI 미노출(결정 N — 파라미터 미전송 = 전체).
  - 이력은 클릭/네비게이션 없음(표시 전용). 상세 화면 없음.
  - 항목 key 는 반드시 auditId 다. userCd 를 쓰면 재가입 이력에서 중복 key 가 된다.
  - 패턴 차용: AdminApprovalHistoryList.vue(칩 · 무한스크롤 · inflightSeq 동시호출 가드).
  - 디자인 토큰: 부모(.admin-selfjoin-view) 상속.
-->
<template>
  <div class="sjh">
    <!-- 처리결과 칩(단일 선택) -->
    <div class="sjh-chips" role="tablist" aria-label="처리결과 필터">
      <button
        v-for="c in actionChips"
        :key="c.key"
        type="button"
        class="sjh-chip"
        :class="{ 'is-active': activeAction === c.key }"
        :aria-selected="activeAction === c.key"
        @click="onSelectAction(c)"
      >
        {{ c.label }}
      </button>
    </div>

    <div ref="bodyRef" class="sjh-list">
      <!-- 로딩(첫 페이지) -->
      <p v-if="isLoading && items.length === 0" class="sjh-state" aria-live="polite">
        불러오는 중...
      </p>

      <!-- 빈 상태: 전체 0 -->
      <p
        v-else-if="!isLoading && items.length === 0 && totalCount === 0 && !hasFilter"
        class="sjh-state"
        aria-live="polite"
      >
        처리된 가입 신청 이력이 없습니다.
      </p>

      <!-- 빈 상태: 필터 0 -->
      <div
        v-else-if="!isLoading && items.length === 0"
        class="sjh-state sjh-state--filtered"
        aria-live="polite"
      >
        <p class="sjh-state__text">조건에 맞는 이력이 없습니다.</p>
        <button type="button" class="sjh-state__reset" @click="onResetFilters">필터 해제</button>
      </div>

      <!-- 이력 항목(표시 전용) -->
      <template v-else>
        <article v-for="item in items" :key="item.auditId" class="sjh-row">
          <header class="sjh-row__top">
            <span class="sjh-row__date">{{ item.processDtime || '-' }}</span>
            <span class="sjh-row__badge" :class="actionClass(item.actionType)">
              {{ actionLabel(item.actionType) }}
            </span>
          </header>

          <div class="sjh-row__body">
            <p class="sjh-row__who">
              <span class="sjh-row__name">{{ item.userNm || '-' }}</span>
              <span v-if="item.nodeNm" class="sjh-row__dept">{{ item.nodeNm }}</span>
              <span v-if="showSite && item.siteNm" class="sjh-row__dept">{{ item.siteNm }}</span>
            </p>
            <!-- 휴대폰은 서버 마스킹값 -->
            <p class="sjh-row__line">{{ item.userId || '-' }} · {{ item.mblNo || '-' }}</p>
            <!-- 승인 정보(승인 행만 값이 있다 — 거부 행은 서버가 null 로 내린다) -->
            <p v-if="item.hireDate || item.rankNm" class="sjh-row__line">
              <span v-if="item.hireDate">입사일 {{ item.hireDate }}</span>
              <span v-if="item.rankNm" class="sjh-row__rank">{{ item.rankNm }}</span>
            </p>
          </div>

          <!-- 거부 사유(거부 행만) -->
          <div v-if="item.rejectReason" class="sjh-row__reason">
            <span class="sjh-row__reason-label">거부 사유</span>
            <span class="sjh-row__reason-body">{{ item.rejectReason }}</span>
          </div>

          <footer class="sjh-row__meta">
            <span>처리자 {{ item.processorNm || '-' }}</span>
            <span>신청 {{ item.applyDtime || '-' }}</span>
          </footer>
        </article>

        <!-- 무한 스크롤 sentinel -->
        <div ref="sentinelRef" class="sjh-sentinel" aria-hidden="true"></div>
        <p v-if="isLoadingMore" class="sjh-state sjh-state--more">불러오는 중...</p>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

const props = defineProps({
  siteCd: { type: String, default: '' },
  nodeCd: { type: String, default: null },
  keyword: { type: String, default: '' },
  // 부서 칩이 '전체'(전사역할)일 때만 사업장명을 함께 보여준다.
  showSite: { type: Boolean, default: false },
})

// reset-filters → 셸이 소유한 검색어 초기화 요청(처리결과 칩은 본 컴포넌트가 직접 되돌린다).
const emit = defineEmits(['reset-filters'])

const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — AdminApprovalHistoryList 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

const PAGE_SIZE = 20

// 처리결과 칩. 값은 서버 화이트리스트(''|APPROVE|REJECT)와 1:1 이다.
const actionChips = [
  { key: '', label: '전체' },
  { key: 'APPROVE', label: '승인' },
  { key: 'REJECT', label: '거부' },
]
const activeAction = ref('')

// ── 데이터 상태(서버 응답으로 채움) ───────────────────────────────────
const items = ref([])
const totalCount = ref(0)
const page = ref(1)
const isLoading = ref(false)
const isLoadingMore = ref(false)

const bodyRef = ref(null)
const sentinelRef = ref(null)
let intersectionObserver = null

// 동시 호출 가드(칩/검색 빠른 연속 변경) — 가장 최신 요청만 반영
let inflightSeq = 0

const hasFilter = computed(() => !!props.keyword || !!activeAction.value)

// ★응답에 hasMore 필드가 없다. 누적 건수와 totalCount 로 계산한다.
const hasMore = computed(() => items.value.length < totalCount.value)

// 결과 배지 — 서버는 APPROVE/REJECT 만 내린다(매퍼 화이트리스트). 그 외는 '-'.
const actionLabel = (t) => (t === 'APPROVE' ? '승인' : t === 'REJECT' ? '거부' : '-')
const actionClass = (t) =>
  t === 'APPROVE'
    ? 'sjh-row__badge--primary'
    : t === 'REJECT'
      ? 'sjh-row__badge--danger'
      : 'sjh-row__badge--neutral'

// 조회 파라미터. 식별자(회사/요청자)는 axios 인터셉터의 토큰 클레임 경유 — 쿼리로 보내지 않는다.
//   actionType 은 빈 문자열이면 미전송(서버 화이트리스트가 빈 값을 400 으로 보지 않도록).
//   처리기간(startDate/endDate)은 v1 UI 미노출이라 전송하지 않는다(= 전체).
const buildParams = (targetPage) => {
  const params = { incSubNodeYn: 'Y', page: targetPage, pageSize: PAGE_SIZE }
  if (props.siteCd) params.siteCd = props.siteCd
  if (props.nodeCd) params.nodeCd = props.nodeCd
  if (props.keyword) params.userKeyword = props.keyword
  if (activeAction.value) params.actionType = activeAction.value
  return params
}

const loadPage = async (append = false) => {
  // ★조기 return 가드를 시퀀스 증가보다 **먼저** 둔다(qa D4).
  //   순서가 뒤바뀌면, 조기 return 한 호출이 seq 만 올려놓고 빠져나가 진행 중이던 요청의
  //   mySeq 를 무효화한다. 그 응답은 폐기되고 finally 의 소유권 검사도 거짓이 되어
  //   isLoading 이 true 로 고착된다("불러오는 중..." 영구 표시).
  if (append && (!hasMore.value || isLoadingMore.value || isLoading.value)) return

  const mySeq = ++inflightSeq

  if (append) {
    isLoadingMore.value = true
  } else {
    page.value = 1
    items.value = []
    totalCount.value = 0
    isLoading.value = true
  }

  try {
    const res = await api.get('/appApi/admin/self-join/history', {
      params: buildParams(page.value),
    })
    if (mySeq !== inflightSeq) return

    const data = res?.data || {}
    const fetched = Array.isArray(data.historyList) ? data.historyList : []

    items.value = append ? items.value.concat(fetched) : fetched
    totalCount.value = typeof data.totalCount === 'number' ? data.totalCount : items.value.length

    // 응답이 비면(서버/필터 불일치) 다음 페이지 요청이 무한 반복되지 않도록 페이지를 올리지 않는다.
    if (hasMore.value && fetched.length > 0) page.value += 1
  } catch (e) {
    if (mySeq !== inflightSeq) return
    // 401(토큰) 은 axios 인터셉터가 처리. 그 외(403 권한 포함)는 반드시 사유를 알린다 —
    //   403 을 조용한 빈 목록으로 바꾸면 권한 문제가 "데이터 없음"으로 오인된다.
    console.error('[AdminSelfJoinHistoryList] 처리 이력 조회 실패')
    showAlert(
      resolveApiErrorMessage(e, '처리 이력을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'),
    )
    if (!append) {
      items.value = []
      totalCount.value = 0
    }
  } finally {
    if (mySeq === inflightSeq) {
      isLoading.value = false
      isLoadingMore.value = false
    }
  }
}

// ── 무한 스크롤(IntersectionObserver — AdminApprovalHistoryList 패턴) ──
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

// ── 핸들러 ────────────────────────────────────────────────────────────
const onSelectAction = (chip) => {
  if (activeAction.value === chip.key) return
  activeAction.value = chip.key
  reload()
}

const onResetFilters = () => {
  activeAction.value = ''
  if (props.keyword) {
    // 검색어는 셸이 소유한다 — 비워지면 keyword prop watch 가 재조회를 트리거한다.
    emit('reset-filters')
    return
  }
  reload()
}

// ── 라이프사이클 ──────────────────────────────────────────────────────
watch(
  () => [props.siteCd, props.nodeCd, props.keyword],
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
</script>

<style scoped>
.sjh {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  min-height: 0;
}

/* 처리결과 칩 */
.sjh-chips {
  display: flex;
  gap: var(--space-sm);
  overflow-x: auto;
  padding-bottom: var(--space-xs);
  -webkit-overflow-scrolling: touch;
}
.sjh-chip {
  flex: 0 0 auto;
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
.sjh-chip.is-active {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
  font-weight: 700;
}

/* 리스트 */
.sjh-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.sjh-state {
  margin: 0;
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.sjh-state--more {
  padding: 12px 0;
  font-size: 13px;
}
.sjh-state--filtered {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
}
.sjh-state__text {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.sjh-state__reset {
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
.sjh-sentinel {
  height: 1px;
}

/* 이력 항목 */
.sjh-row {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}
.sjh-row__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}
.sjh-row__date {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}
.sjh-row__badge {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.sjh-row__badge--primary {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.sjh-row__badge--danger {
  background: var(--color-danger-tint);
  color: var(--color-danger-text);
}
.sjh-row__badge--neutral {
  background: var(--color-disabled-bg);
  color: var(--color-text-secondary);
}

.sjh-row__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.sjh-row__who {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 6px;
  flex-wrap: wrap;
}
.sjh-row__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.sjh-row__dept {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.sjh-row__line {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}
.sjh-row__rank {
  margin-left: 6px;
  color: var(--color-text-tertiary);
}

.sjh-row__reason {
  background: var(--color-danger-tint);
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  color: var(--color-danger-text);
  line-height: 17px;
}
.sjh-row__reason-label {
  font-weight: 600;
  margin-right: 6px;
}

.sjh-row__meta {
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
