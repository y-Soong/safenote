<!--
  AdminTbmHistoryList.vue — 탭4 TBM 이력 리스트 + 통계 (종료/취소 세션)
  - 작업 ID: 001-P5-T-F14 (분해: 001-phase5-admin-tbm-plan.md §2-7, §3-I T-A9)
  - 백엔드: GET /appApi/admin/tbm/history?statusCd=&startDate=&endDate=&keyword=&page=&pageSize= (T-A9)
      식별자(회사)·노드/사업장 스코프는 서버(토큰+resolveScope, R3 리스트와 동일). 프론트는 필터 값만 전송.
  - 상단 통계 요약: TBM 횟수 / 참여 / 이수 / 미이수 / 평균 이수율(stat).
  - 필터: 상태(COMPLETED/CANCELLED) + 기간(시작~종료, native input[type=date]) + 제목 검색.
  - 카드: 상태 / 제목 / 위험성수 / 출결 / 이수 / 미이수 / 종료일. 카드 클릭 → select emit({ sessionCd }).
  - 상태별 동작: loading / error(재시도) / empty / success.
  - 디자인 토큰은 부모(.admin-tbm-view)에서 상속(셸 내부에서만 사용 — 루트 재선언 없음).
  - 참조 패턴: AdminTbmManageList.vue(필터칩/상태) + AdminTbmSessionCard.vue(카드/배지/카운트)
      + AdminTbmCompletedView.vue(summary 통계 카드) + req/RequestDateRangeFilterSheet(native date 패턴).
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격 + UI 필터 토글만(조회 로직은 developer).
-->
<template>
  <div class="admin-tbm-hist">
    <!-- 통계 요약 -->
    <section class="hist-stat">
      <div class="hist-stat__item">
        <span class="hist-stat__label">TBM</span>
        <span class="hist-stat__value">{{ num(stat.sessionCount) }}</span>
      </div>
      <div class="hist-stat__item">
        <span class="hist-stat__label">참여</span>
        <span class="hist-stat__value">{{ num(stat.attendanceCount) }}</span>
      </div>
      <div class="hist-stat__item hist-stat__item--ok">
        <span class="hist-stat__label">이수</span>
        <span class="hist-stat__value">{{ num(stat.completedCount) }}</span>
      </div>
      <div class="hist-stat__item hist-stat__item--ng">
        <span class="hist-stat__label">미이수</span>
        <span class="hist-stat__value">{{ num(stat.notCompletedCount) }}</span>
      </div>
      <div class="hist-stat__item">
        <span class="hist-stat__label">평균 이수율</span>
        <span class="hist-stat__value">{{ rateText }}</span>
      </div>
    </section>

    <!-- 필터: 기간 + 검색 -->
    <div class="hist-filter">
      <div class="hist-filter__dates">
        <input
          v-model="startDate"
          class="hist-filter__date"
          type="date"
          aria-label="시작일"
          @change="onApplyFilter"
        />
        <span class="hist-filter__tilde" aria-hidden="true">~</span>
        <input
          v-model="endDate"
          class="hist-filter__date"
          type="date"
          aria-label="종료일"
          @change="onApplyFilter"
        />
      </div>
      <input
        v-model.trim="keyword"
        class="hist-filter__search"
        type="text"
        maxlength="200"
        placeholder="교육 제목 검색"
        @keyup.enter="onApplyFilter"
      />
    </div>

    <!-- 상태 필터 칩 -->
    <div class="hist-filter__chips" role="tablist" aria-label="상태 필터">
      <button
        v-for="f in statusFilters"
        :key="f.value"
        type="button"
        class="filter-chip"
        :class="{ 'is-active': statusCd === f.value }"
        @click="onChangeStatus(f.value)"
      >
        {{ f.label }}
      </button>
    </div>

    <!-- loading -->
    <p v-if="isLoading" class="admin-tbm-hist__state">불러오는 중…</p>

    <!-- error -->
    <div v-else-if="loadError" class="admin-tbm-hist__state">
      <p class="admin-tbm-hist__state-msg">이력을 불러오지 못했어요.</p>
      <button type="button" class="admin-tbm-hist__retry" @click="onRetry">다시 시도</button>
    </div>

    <!-- empty -->
    <p v-else-if="!historyList.length" class="admin-tbm-hist__state admin-tbm-hist__state--empty">
      조회된 이력이 없어요
    </p>

    <!-- success -->
    <template v-else>
      <button
        v-for="h in historyList"
        :key="h.sessionCd"
        type="button"
        class="hist-card"
        @click="$emit('select', { sessionCd: h.sessionCd })"
      >
        <div class="hist-card__head">
          <span class="hist-card__status" :class="statusToneClass(h)">{{ statusLabel(h) }}</span>
          <p class="hist-card__title">{{ h.title || 'TBM 세션' }}</p>
        </div>

        <div class="hist-card__counts">
          <span class="count">
            <span class="count__label">위험성</span>
            <span class="count__value">{{ num(h.riskCount) }}</span>
          </span>
          <span class="count">
            <span class="count__label">출결</span>
            <span class="count__value">{{ num(h.attendanceCount) }}</span>
          </span>
          <span class="count">
            <span class="count__label">이수</span>
            <span class="count__value count__value--ok">{{ num(h.completedCount) }}</span>
          </span>
          <span class="count">
            <span class="count__label">미이수</span>
            <span class="count__value count__value--danger">{{ num(h.notCompletedCount) }}</span>
          </span>
        </div>

        <p v-if="metaText(h)" class="hist-card__meta">{{ metaText(h) }}</p>
      </button>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'

// select: 이력 상세로({ sessionCd })
defineEmits(['select'])

// 상태 필터(이력 탭 = 종료/취소). MANAGE 탭과 분리.
const statusFilters = [
  { value: '', label: '전체' },
  { value: 'COMPLETED', label: '종료' },
  { value: 'CANCELLED', label: '취소' },
]

// ── 반응형 상태 ───────────────────────────────────────────────────
const isLoading = ref(false)
const loadError = ref(false)
const statusCd = ref('') // 선택 상태 필터
const startDate = ref('') // YYYY-MM-DD (native input)
const endDate = ref('')
const keyword = ref('')

// 통계: { sessionCount, attendanceCount, completedCount, notCompletedCount, avgCompletionRate }
const stat = ref({})
// 이력 카드: [{ sessionCd, statusCd, statusNm, title, riskCount, attendanceCount, completedCount, notCompletedCount, managerUserNm, endedAt }]
const historyList = ref([])

// 카운트 표시(없으면 0)
const num = (v) => (v == null ? 0 : v)

// 평균 이수율(0~100, 서버 산출값). null 이면 '-'.
const rateText = computed(() => {
  const r = stat.value?.avgCompletionRate
  return r == null ? '-' : `${r}%`
})

// 상태 라벨/톤(SYS046) — 서버 statusNm 우선
const STATUS_LABELS = { COMPLETED: '종료', CANCELLED: '취소' }
const statusLabel = (h) => h?.statusNm || STATUS_LABELS[h?.statusCd] || h?.statusCd || '-'
const statusToneClass = (h) =>
  h?.statusCd === 'CANCELLED' ? 'hist-card__status--cancelled' : 'hist-card__status--completed'

// 보조 메타(개설자 + 종료일)
const metaText = (h) => {
  const parts = []
  if (h?.managerUserNm) parts.push(`개설자 ${h.managerUserNm}`)
  if (h?.endedAt) parts.push(`${h.endedAt} 종료`)
  return parts.join(' · ')
}

// 이력 + 통계 조회 — GET /appApi/admin/tbm/history (T-A9).
//   식별자/스코프는 서버(토큰+resolveScope)가 산출. 클라이언트는 상태/기간/검색 필터만 전달(C1).
//   기간은 native YYYY-MM-DD 그대로 전송(서버가 DATE() 파싱하므로 변환 불필요).
const loadHistory = async () => {
  isLoading.value = true
  loadError.value = false
  try {
    const params = {}
    if (statusCd.value) params.statusCd = statusCd.value
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    if (keyword.value) params.keyword = keyword.value
    const { data } = await api.get('/appApi/admin/tbm/history', { params })
    historyList.value = Array.isArray(data?.historyList) ? data.historyList : []
    stat.value = data?.stat || {}
  } catch (e) {
    console.error('[AdminTbmHistoryList] 이력 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

// 상태 필터 변경(UI 토글) → 재조회
const onChangeStatus = (value) => {
  if (statusCd.value === value) return
  statusCd.value = value
  loadHistory()
}

// 기간/검색 적용(change/엔터) → 재조회
const onApplyFilter = () => {
  loadHistory()
}

const onRetry = () => {
  loadHistory()
}

onMounted(loadHistory)
</script>

<style scoped>
.admin-tbm-hist {
  display: flex;
  flex-direction: column;
}

/* 통계 요약 */
.hist-stat {
  display: flex;
  gap: var(--space-xs);
  margin-bottom: var(--space-md);
  padding: var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.hist-stat__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-xs);
  min-width: 0;
}
.hist-stat__label {
  font-size: 11px;
  color: var(--color-text-tertiary);
  text-align: center;
}
.hist-stat__value {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.hist-stat__item--ok .hist-stat__value {
  color: var(--color-primary);
}
.hist-stat__item--ng .hist-stat__value {
  color: var(--color-danger);
}

/* 필터: 기간 + 검색 */
.hist-filter {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}
.hist-filter__dates {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.hist-filter__date {
  flex: 1;
  min-width: 0;
  box-sizing: border-box;
  height: 40px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.hist-filter__date:focus {
  outline: none;
  border-color: var(--color-primary);
}
.hist-filter__tilde {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}
.hist-filter__search {
  box-sizing: border-box;
  height: 40px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.hist-filter__search:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 상태 칩 */
.hist-filter__chips {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
  flex-wrap: wrap;
}
.filter-chip {
  height: 32px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.filter-chip.is-active {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
  font-weight: 700;
}

/* 이력 카드 */
.hist-card {
  width: 100%;
  text-align: left;
  display: block;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  margin-bottom: var(--space-md);
  cursor: pointer;
  font-family: inherit;
}
.hist-card:active {
  background: var(--color-bg);
}
.hist-card__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.hist-card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 상태 배지 */
.hist-card__status {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.hist-card__status--completed {
  background: var(--color-bg);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}
.hist-card__status--cancelled {
  background: var(--color-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-border);
}

/* 카운트 */
.hist-card__counts {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
  margin-top: var(--space-md);
}
.count {
  display: inline-flex;
  align-items: baseline;
  gap: var(--space-xs);
}
.count__label {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.count__value {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.count__value--ok {
  color: var(--color-primary);
}
.count__value--danger {
  color: var(--color-danger);
}

.hist-card__meta {
  margin: var(--space-sm) 0 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* 상태 메시지 */
.admin-tbm-hist__state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.admin-tbm-hist__state--empty {
  padding: var(--space-lg) 0;
}
.admin-tbm-hist__state-msg {
  margin: 0 0 var(--space-sm);
}
.admin-tbm-hist__retry {
  height: 36px;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
</style>
