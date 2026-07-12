<!--
  LeaveSplitKpi.vue — 3분할 KPI (부여 / 사용 / 사용예정)
  - 작업: prafta-app-005 슬롯 F (UI 명세: UI-A005)
  - 시안 §4.5 / 3열 grid. 0값은 muted(text-tertiary)로 노이즈 감소.
  - group: { granted, used, planned }. 표시 전용. 토큰은 부모(.my-leave-view)에서 상속.
-->
<template>
  <div class="split">
    <div v-for="cell in cells" :key="cell.key" class="cell">
      <span class="cell__lbl">{{ cell.label }}</span>
      <span class="cell__val" :class="{ 'cell__val--muted': cell.value === 0 }">
        {{ cell.text }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatLeaveDays } from '@/utils/leaveFormat'

const props = defineProps({
  // { granted, used, planned }
  group: {
    type: Object,
    default: null,
  },
  // LC-11: 1일 환산시간(분) — "N일 H시간 M분" 표기 분모(서버 권위). 미제공 시 480 폴백.
  convMinutes: {
    type: Number,
    default: 480,
  },
})

const numOr0 = (v) => Number(v ?? 0)

// LC-11: 소수점 노출 금지 — "N일 H시간 M분" 표기(내부 계산값은 그대로, 표시만 교체).
const cells = computed(() => {
  const g = props.group || {}
  const fmt = (v) => formatLeaveDays(numOr0(v), props.convMinutes)
  return [
    { key: 'granted', label: '부여', value: numOr0(g.granted), text: fmt(g.granted) },
    { key: 'used', label: '사용', value: numOr0(g.used), text: fmt(g.used) },
    { key: 'planned', label: '사용예정', value: numOr0(g.planned), text: fmt(g.planned) },
  ]
})
</script>

<style scoped>
.split {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 4px;
  position: relative;
}
.cell + .cell::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 1px;
  background: var(--color-border-light);
}
.cell__lbl {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.cell__val {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.cell__val--muted {
  color: var(--color-text-tertiary);
}
</style>
