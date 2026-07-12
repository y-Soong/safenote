<!--
  MySafetyHistoryView.vue — 내 안전활동 이력 화면 (사용자 모드, prafta-app-025 J1-10 B-6)
  - 작업 지시서: .claude/requests/app_requests/job_1/J1-10-B6-my-safety-history.md
  - 위치: src/views/safety/MySafetyHistoryView.vue
  - 진입: MainView › 안전 활동 카드 ">"(onSafetyDetail) → router.push('/MySafetyHistory')
          (하단 "안전" 탭 / SafetyHubView 는 허브로 유지 — 본 화면과 역할 분리)
  - 성격: 본인이 처리한 순회점검 + 본인이 등록한 위험성평가 이력(시간순 합본).
          (TBM 이수 이력은 이번 범위 제외 — 확장 여지. 지시서 §6 미결 1)
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격만.
  - developer 라운드 스코프(TODO):
      (1) GET /appApi/safety/history/my?kind=...&page=...&pageSize=... 조회 → items 매핑
      (2) 필터 칩(전체/순회점검/위험성평가) 전환 시 재조회 또는 클라 필터(서버 kind 권장)
      (3) (선택) 무한 스크롤 — MyRequestsView sentinel 패턴 차용
      (4) (선택) 행 클릭 상세 — 기존 점검/위험성 상세 화면 존재 시 연결(지시서 §6 미결 3)
      (5) loading/empty/error 상태 처리
  - ⚠️ IDOR: 본인 이력은 서버가 USER_CD(JWT)로 필터한다. 클라가 USER_CD 를 전송하지 않는다.
  - 디자인 토큰: MainView(.home-view) 세트를 .my-safety-history-view 루트에 1회 선언(SafetyHubView 동형).
    자식 scoped 상속. 하드코딩 색상/픽셀/!important/비scoped 금지.
-->
<template>
  <div class="my-safety-history-view">
    <!-- 아이콘 스프라이트(본 화면 전역 use 참조 전제) -->
    <svg width="0" height="0" class="hist-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-hist-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol id="i-hist-chev-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6" />
        </symbol>
        <symbol id="i-hist-clipboard" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="8" y="2" width="8" height="4" rx="1" /><path d="M9 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-3" />
        </symbol>
        <symbol id="i-hist-alert" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" /><line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" />
        </symbol>
      </defs>
    </svg>

    <!-- 헤더 (SafetyHubView safety-hd 패턴) -->
    <header class="hist-hd">
      <button type="button" class="hist-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true"><use href="#i-hist-chev-left" /></svg>
      </button>
      <h1 class="hist-hd__title">내 안전활동 이력</h1>
      <span class="hist-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 필터 칩 -->
    <div class="hist-filter" role="tablist">
      <button
        v-for="opt in filterOptions"
        :key="opt.value"
        type="button"
        class="chip"
        :class="{ 'chip--active': kind === opt.value }"
        role="tab"
        :aria-selected="kind === opt.value"
        @click="onSelectKind(opt.value)"
      >
        {{ opt.label }}
      </button>
    </div>

    <!-- 본문 (스크롤 영역) -->
    <main
      class="hist-body"
      ref="bodyRef"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출 -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <!-- 로딩 -->
      <div v-if="isLoading" class="hist-state" aria-live="polite">불러오는 중...</div>

      <!-- 에러 -->
      <div v-else-if="loadError" class="hist-state hist-state--error" aria-live="polite">
        <p class="hist-state__text">이력을 불러오지 못했습니다</p>
        <button type="button" class="hist-state__retry" @click="onRetry">다시 시도</button>
      </div>

      <!-- 빈 상태 -->
      <div v-else-if="items.length === 0" class="hist-empty" aria-live="polite">
        <svg class="icon hist-empty__icon" width="40" height="40" aria-hidden="true"><use href="#i-hist-clipboard" /></svg>
        <p class="hist-empty__text">아직 안전활동 이력이 없어요</p>
      </div>

      <!-- 리스트 -->
      <template v-else>
        <button
          v-for="(item, idx) in items"
          :key="item.key || idx"
          type="button"
          class="hist-row"
          @click="onRowClick(item)"
        >
          <span class="hist-row__icon" :class="`hist-row__icon--${item.type === 'RISK' ? 'risk' : 'inspect'}`">
            <svg class="icon" width="18" height="18" aria-hidden="true">
              <use :href="item.type === 'RISK' ? '#i-hist-alert' : '#i-hist-clipboard'" />
            </svg>
          </span>
          <!-- 사진 썸네일: filePath 가 있을 때만 표시(점검/위험성 공통). 로드 실패 시 숨김 -->
          <span v-if="item.filePath" class="hist-row__thumb">
            <img
              :src="item.filePath"
              class="hist-row__thumb-img"
              alt="첨부 사진"
              loading="lazy"
              @error="onThumbError"
            />
          </span>
          <span class="hist-row__main">
            <span class="hist-row__top">
              <span class="hist-row__badge" :class="`hist-row__badge--${item.type === 'RISK' ? 'risk' : 'inspect'}`">
                {{ item.type === 'RISK' ? '위험성평가' : '순회점검' }}
              </span>
              <span class="hist-row__title">{{ item.title }}</span>
            </span>
            <span class="hist-row__sub">{{ item.subText }}</span>
          </span>
          <span class="hist-row__meta">
            <span class="hist-row__date">{{ item.displayDate }}</span>
            <svg class="icon hist-row__chev" width="16" height="16" aria-hidden="true"><use href="#i-hist-chev-right" /></svg>
          </span>
        </button>

        <!-- 무한 스크롤 sentinel(선택 — developer 가 IntersectionObserver 연결) -->
        <div ref="sentinelRef" class="hist-sentinel" aria-hidden="true"></div>
        <p v-if="isLoadingMore" class="hist-load-more">불러오는 중...</p>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백 (MainView 패턴 동일)
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 필터 칩 (UI 토글 — 허용 범위) ───────────────────────────────────────────
const filterOptions = [
  { value: 'ALL', label: '전체' },
  { value: 'INSPECT', label: '순회점검' },
  { value: 'RISK', label: '위험성평가' },
]
const kind = ref('ALL')

// ── 상태 ───────────────────────────────────────────────────────────────────
const isLoading = ref(true)
const isLoadingMore = ref(false)
const loadError = ref(false)
const items = ref([]) // [{ key, type:'INSPECT'|'RISK', title, subText, displayDate, occurredDate, filePath }]
const hasMore = ref(false)

const bodyRef = ref(null)
const sentinelRef = ref(null)

// ── 페이징(무한 스크롤) ─────────────────────────────────────────────────────
const PAGE_SIZE = 20
const page = ref(0)
let observer = null

// 서버 응답 행을 화면 표시 계약으로 정규화.
//   서버가 이미 합본/시간순 정렬 + 표시 필드(type/title/subText/displayDate)를 내려주므로
//   여기서는 안전한 기본값만 보정한다(키 폴백 등). IDOR: 클라는 USER_CD 를 전송하지 않는다.
const toRow = (raw, idx) => ({
  key: raw?.key || `${raw?.type || 'ITEM'}:${idx}`,
  type: raw?.type === 'RISK' ? 'RISK' : 'INSPECT',
  title: raw?.title || '',
  subText: raw?.subText || '',
  displayDate: raw?.displayDate || '',
  occurredDate: raw?.occurredDate || '',
  filePath: raw?.filePath || '',
})

// ── 조회 ───────────────────────────────────────────────────────────────────
//   GET /appApi/safety/history/my?kind=&page=&pageSize=
//   서버가 점검(INSERT_NO=JWT userCd)·위험성(INIT_ASSESSOR_ID=JWT userCd) 본인 이력을 병합·정렬해 내려준다.
//   append=true 면 다음 페이지(무한 스크롤), 아니면 첫 페이지(필터 전환/재시도/최초)로 목록 초기화.
const loadHistory = async ({ append = false } = {}) => {
  if (append) {
    if (isLoadingMore.value || !hasMore.value) return
    isLoadingMore.value = true
  } else {
    isLoading.value = true
    loadError.value = false
    page.value = 0
  }
  try {
    const params = { kind: kind.value, page: page.value, pageSize: PAGE_SIZE }
    const { data } = await api.get('/appApi/safety/history/my', { params })
    const baseIdx = append ? items.value.length : 0
    const list = Array.isArray(data?.items)
      ? data.items.map((raw, i) => toRow(raw, baseIdx + i))
      : []
    items.value = append ? items.value.concat(list) : list
    hasMore.value = !!data?.hasMore
  } catch (e) {
    // 401/403 토큰 에러는 axios 인터셉터가 처리. 그 외는 영역 에러.
    console.warn('[MySafetyHistory] history 조회 실패:', e?.message)
    if (!append) {
      loadError.value = true
      items.value = []
      hasMore.value = false
    }
  } finally {
    if (append) isLoadingMore.value = false
    else isLoading.value = false
  }
}

// 다음 페이지 로드(무한 스크롤 sentinel 진입 시).
const loadMore = async () => {
  if (!hasMore.value || isLoadingMore.value) return
  page.value += 1
  await loadHistory({ append: true })
}

// IntersectionObserver 등록(본문 스크롤 컨테이너 기준). 첫 조회 후 sentinel 이 그려진 뒤 1회 등록.
const setupObserver = () => {
  if (observer || typeof IntersectionObserver === 'undefined') return
  if (!sentinelRef.value) return
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((en) => en.isIntersecting)) loadMore()
    },
    { root: bodyRef.value || null, rootMargin: '120px', threshold: 0 },
  )
  observer.observe(sentinelRef.value)
}

// ── 핸들러 ─────────────────────────────────────────────────────────────────
const onBack = () => {
  router.back()
}

// 필터 전환 — 서버 kind 재조회(목록/페이지 초기화).
const onSelectKind = async (value) => {
  if (kind.value === value) return
  kind.value = value
  await loadHistory()
  // 필터 전환 후 리스트가 다시 그려지면 sentinel 도 재생성되므로 관찰 대상을 갱신한다.
  await nextTick()
  if (observer && sentinelRef.value) observer.observe(sentinelRef.value)
}

// 썸네일 로드 실패 시 깨진 이미지 아이콘 노출 방지 — 해당 썸네일 컨테이너를 숨긴다.
const onThumbError = (e) => {
  const wrap = e?.target?.closest?.('.hist-row__thumb')
  if (wrap) wrap.style.display = 'none'
}

// 행 클릭 상세 — 기존 점검/위험성 상세 화면 존재 시 연결(지시서 §6 미결 3). MVP는 무동작(상세 후속).
const onRowClick = () => {
  // MVP 범위: 리스트 표시까지. 행 클릭 상세 라우팅은 후속(지시서 §6 미결 3).
}

const onRetry = async () => {
  await loadHistory()
  await nextTick()
  if (observer && sentinelRef.value) observer.observe(sentinelRef.value)
}

// 당겨서 새로고침 — 스크롤 최상단에서 아래로 더 당기면 현재 필터(kind)로 첫 페이지 재조회(MainView 패턴).
//   목록이 다시 그려지면 sentinel 도 재생성되므로 무한 스크롤 관찰 대상을 갱신한다(onRetry 동형).
//   스크롤 컨테이너는 기존 bodyRef(.hist-body) 재사용.
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(bodyRef, async () => {
  await loadHistory()
  await nextTick()
  if (observer && sentinelRef.value) observer.observe(sentinelRef.value)
})

onMounted(async () => {
  await loadHistory()
  await nextTick()
  setupObserver()
})

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
})
</script>

<style scoped>
/* 디자인 토큰 — MainView .home-view 세트를 본 화면 루트에 1회 선언(SafetyHubView 동형) */
.my-safety-history-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
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
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

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
.hist-hd {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 var(--space-sm);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.hist-hd__back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: transparent;
  border: none;
  color: var(--color-text-primary);
  cursor: pointer;
}
.hist-hd__title {
  flex: 1;
  margin: 0;
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.hist-hd__spacer {
  width: 40px;
  height: 40px;
}

/* 필터 칩 */
.hist-filter {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}
.chip {
  padding: var(--space-xs) var(--space-md);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
}
.chip--active {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
  font-weight: 600;
}

/* 본문 */
.hist-body {
  flex: 1;
  min-height: 0;
  padding: var(--space-md) var(--space-lg) 32px;
  overflow-y: auto;
}

/* 로딩/에러 */
.hist-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  padding: 48px 16px;
  color: var(--color-text-secondary);
  font-size: 14px;
}
.hist-state__text {
  margin: 0;
}
.hist-state__retry {
  padding: var(--space-xs) var(--space-md);
  background: var(--color-primary-tint);
  border: 1px solid var(--color-primary-tint-border);
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

/* 빈 상태 */
.hist-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
  padding: 64px 16px;
  color: var(--color-text-tertiary);
}
.hist-empty__icon {
  color: var(--color-text-tertiary);
}
.hist-empty__text {
  margin: 0;
  font-size: 14px;
}

/* 이력 행 */
.hist-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  width: 100%;
  padding: var(--space-md);
  margin-bottom: var(--space-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  text-align: left;
  cursor: pointer;
}
.hist-row__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}
.hist-row__icon--inspect {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.hist-row__icon--risk {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
/* 사진 썸네일(점검/위험성 공통) — 토큰 기반 크기/모서리 */
.hist-row__thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-border-light);
  flex-shrink: 0;
}
.hist-row__thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.hist-row__main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}
.hist-row__top {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  min-width: 0;
}
.hist-row__badge {
  flex-shrink: 0;
  padding: 1px var(--space-xs);
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 600;
}
.hist-row__badge--inspect {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.hist-row__badge--risk {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.hist-row__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hist-row__sub {
  font-size: 12px;
  color: var(--color-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hist-row__meta {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  flex-shrink: 0;
}
.hist-row__date {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.hist-row__chev {
  color: var(--color-text-tertiary);
}

/* 무한 스크롤 */
.hist-sentinel {
  height: 1px;
}
.hist-load-more {
  text-align: center;
  padding: var(--space-md);
  font-size: 13px;
  color: var(--color-text-secondary);
}

.icon {
  display: inline-block;
  vertical-align: middle;
}
</style>
