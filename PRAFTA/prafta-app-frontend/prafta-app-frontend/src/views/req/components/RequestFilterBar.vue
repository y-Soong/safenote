<!--
  RequestFilterBar.vue — 내 승인 요청 필터 영역 (상단 행 + 트리거 행)
  - 작업 ID: PRAFTA-APP-006-8
-->
<template>
  <section class="req-filter-bar" aria-label="요청 필터">
    <!-- 상단 행: 총 건수 + 정렬 -->
    <div class="req-filter-bar__top">
      <div class="req-filter-bar__count-wrap">
        <span class="req-filter-bar__count">전체 {{ filteredCount }}건</span>
        <span v-if="activeFilterCount > 0" class="req-filter-bar__filter-tag"
          >필터 {{ activeFilterCount }}</span
        >
      </div>
      <button
        type="button"
        class="req-filter-bar__sort"
        aria-label="정렬 옵션 열기"
        @click="$emit('openSortSheet')"
      >
        <span>{{ sortLabel }}</span>
        <svg width="14" height="14" aria-hidden="true">
          <use href="#i-req-chev-down" />
        </svg>
      </button>
    </div>

    <!-- 트리거 행: 3종 필터 + 초기화 -->
    <div class="req-filter-bar__triggers" role="group" aria-label="필터 트리거">
      <button
        type="button"
        class="req-trigger"
        :class="{ 'req-trigger--on': activeFilters.statuses.length > 0 }"
        @click="$emit('openStatusSheet')"
      >
        <span v-if="activeFilters.statuses.length > 0" class="req-trigger__badge">{{
          activeFilters.statuses.length
        }}</span>
        <span class="req-trigger__label">상태</span>
        <svg width="14" height="14" aria-hidden="true">
          <use href="#i-req-chev-down" />
        </svg>
      </button>

      <button
        type="button"
        class="req-trigger"
        :class="{ 'req-trigger--on': hasDateRange }"
        @click="$emit('openDateSheet')"
      >
        <span class="req-trigger__label">{{ dateRangeLabel }}</span>
        <svg width="14" height="14" aria-hidden="true">
          <use href="#i-req-chev-down" />
        </svg>
      </button>

      <button
        type="button"
        class="req-trigger"
        :class="{ 'req-trigger--on': activeFilters.types.length > 0 }"
        @click="$emit('openTypeSheet')"
      >
        <span v-if="activeFilters.types.length > 0" class="req-trigger__badge">{{
          activeFilters.types.length
        }}</span>
        <span class="req-trigger__label">유형</span>
        <svg width="14" height="14" aria-hidden="true">
          <use href="#i-req-chev-down" />
        </svg>
      </button>

      <button
        v-if="activeFilterCount > 0"
        type="button"
        class="req-reset"
        aria-label="필터 전체 초기화"
        @click="$emit('reset')"
      >
        <svg width="12" height="12" aria-hidden="true">
          <use href="#i-req-x" />
        </svg>
        <span>초기화</span>
      </button>
    </div>

    <!-- 인라인 SVG sprite (본 컴포넌트 전용) -->
    <svg width="0" height="0" class="req-filter-bar__sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-req-chev-down"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="6 9 12 15 18 9" />
        </symbol>
        <symbol
          id="i-req-x"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </symbol>
      </defs>
    </svg>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  totalCount: { type: Number, default: 0 },
  filteredCount: { type: Number, default: 0 },
  activeFilters: {
    type: Object,
    default: () => ({ statuses: [], dateFrom: '', dateTo: '', types: [] }),
  },
  sortLabel: { type: String, default: '대기 우선' },
})

defineEmits(['openStatusSheet', 'openDateSheet', 'openTypeSheet', 'openSortSheet', 'reset'])

const hasDateRange = computed(() => !!props.activeFilters.dateFrom || !!props.activeFilters.dateTo)

const activeFilterCount = computed(() => {
  let n = 0
  if (props.activeFilters.statuses?.length > 0) n += 1
  if (hasDateRange.value) n += 1
  if (props.activeFilters.types?.length > 0) n += 1
  return n
})

// YYYYMMDD → M/D
const fmtMD = (ymd) => {
  if (!ymd || ymd.length !== 8) return ''
  const m = Number(ymd.slice(4, 6))
  const d = Number(ymd.slice(6, 8))
  return `${m}/${d}`
}

const dateRangeLabel = computed(() => {
  if (!hasDateRange.value) return '기간'
  const f = fmtMD(props.activeFilters.dateFrom)
  const t = fmtMD(props.activeFilters.dateTo)
  if (f && t) return `${f} ~ ${t}`
  if (f) return `${f} ~`
  return `~ ${t}`
})
</script>

<style scoped>
.req-filter-bar {
  padding: 8px 16px 12px;
  background: var(--color-bg);
  border-bottom: 0.5px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.req-filter-bar__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.req-filter-bar__count-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.req-filter-bar__count {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.req-filter-bar__filter-tag {
  background: var(--color-primary-tint);
  color: var(--color-primary);
  border-radius: 4px;
  padding: 0 6px;
  height: 18px;
  font-size: 11px;
  font-weight: 500;
  line-height: 18px;
}

.req-filter-bar__sort {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 10px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  font-size: 12px;
  cursor: pointer;
}

.req-filter-bar__triggers {
  display: flex;
  flex-direction: row;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;
}
.req-filter-bar__triggers::-webkit-scrollbar {
  display: none;
}

.req-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 8px 0 12px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  font-size: 12px;
  white-space: nowrap;
  cursor: pointer;
}

.req-trigger--on {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
}

.req-trigger__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: var(--color-primary);
  color: var(--color-surface);
  border-radius: var(--radius-full);
  font-size: 10px;
  font-weight: 600;
}

.req-reset {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 10px;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}
</style>
