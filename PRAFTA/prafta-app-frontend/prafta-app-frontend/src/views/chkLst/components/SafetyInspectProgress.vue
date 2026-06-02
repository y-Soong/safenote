<!--
  SafetyInspectProgress.vue — 응답 진행 카운터 + 진행 바 (prafta-app-011 화면 B)
  - 좌: 응답 N/M, 우: 양호 X · 불량 Y. 진행 바 (N/M)%.
  - 접근성: aria-live 로 진행 상황 발화.
-->
<template>
  <div class="prog-wrap">
    <div class="progress" aria-live="polite">
      <span
        >응답 <strong>{{ answered }}</strong> / {{ total }}</span
      >
      <span
        >양호 <span class="pp">{{ okCount }}</span> · 불량
        <span class="pw">{{ badCount }}</span></span
      >
    </div>
    <div
      class="pbar"
      role="progressbar"
      :aria-valuenow="answered"
      aria-valuemin="0"
      :aria-valuemax="total"
      :aria-label="`응답 ${answered} / ${total}, 양호 ${okCount}, 불량 ${badCount}`"
    >
      <div class="pbar-fill" :style="{ width: percent + '%' }"></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  answered: { type: Number, default: 0 },
  total: { type: Number, default: 0 },
  okCount: { type: Number, default: 0 },
  badCount: { type: Number, default: 0 },
})

const percent = computed(() => {
  if (!props.total) return 0
  return Math.round((props.answered / props.total) * 100)
})
</script>

<style scoped>
.prog-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.progress {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.progress strong {
  color: var(--color-text-primary);
  font-weight: 700;
}
.progress .pp {
  color: var(--color-primary);
  font-weight: 700;
}
.progress .pw {
  color: var(--color-warning-text);
  font-weight: 700;
}
.pbar {
  height: 4px;
  background: var(--color-border-light);
  border-radius: 2px;
  overflow: hidden;
}
.pbar-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 2px;
  transition: width 0.2s;
}
</style>
