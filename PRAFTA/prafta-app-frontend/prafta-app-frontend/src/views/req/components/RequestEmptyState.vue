<!--
  RequestEmptyState.vue — 빈 상태 (2종)
  - 작업 ID: PRAFTA-APP-006-9
  - kind: 'total' | 'filtered'
-->
<template>
  <div class="req-empty">
    <div class="req-empty__icon-wrap" aria-hidden="true">
      <svg
        width="28"
        height="28"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <polyline points="22 12 16 12 14 15 10 15 8 12 2 12" />
        <path
          d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2 -2v-6l-3.45 -6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0 -1.79 1.11Z"
        />
      </svg>
    </div>
    <p class="req-empty__msg">{{ msg }}</p>
    <p class="req-empty__sub">{{ sub }}</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  kind: { type: String, default: 'total', validator: (v) => ['total', 'filtered'].includes(v) },
})

const msg = computed(() =>
  props.kind === 'total' ? '아직 등록한 요청이 없어요' : '선택한 조건의 요청이 없어요',
)
const sub = computed(() =>
  props.kind === 'total' ? '근태 화면에서 요청을 등록할 수 있어요' : '필터를 변경해 보세요',
)
</script>

<style scoped>
.req-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 16px;
  gap: 12px;
}

.req-empty__icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-full);
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.req-empty__msg {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.req-empty__sub {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
</style>
