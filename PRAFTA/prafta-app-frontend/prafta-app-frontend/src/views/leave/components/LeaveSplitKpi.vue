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
        {{ cell.text }}일
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // { granted, used, planned }
  group: {
    type: Object,
    default: null,
  },
})

// 0.5 단위 표기 (정수면 정수, 소수면 1자리)
const trimDays = (v) => {
  if (v == null) return '0'
  const n = Number(v)
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}
const numOr0 = (v) => Number(v ?? 0)

const cells = computed(() => {
  const g = props.group || {}
  return [
    { key: 'granted', label: '부여', value: numOr0(g.granted), text: trimDays(g.granted) },
    { key: 'used', label: '사용', value: numOr0(g.used), text: trimDays(g.used) },
    { key: 'planned', label: '사용예정', value: numOr0(g.planned), text: trimDays(g.planned) },
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
