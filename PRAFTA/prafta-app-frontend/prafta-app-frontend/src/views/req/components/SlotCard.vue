<!--
  SlotCard.vue — 1구간/2구간 공통 카드 (3 폼 공유)
  - 작업 ID: PRAFTA-APP-007-5 (분해: .claude/requests/app_requests/prafta-app-007-plan.md §8.2)
-->
<template>
  <div class="slot-card">
    <div class="slot-card__head">
      <span class="slot-card__tag">{{ title }}</span>
      <button
        v-if="removable"
        type="button"
        class="slot-card__del"
        aria-label="구간 삭제"
        @click="$emit('remove', workSeq)"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M3 6h18" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
          <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          <line x1="10" y1="11" x2="10" y2="17" />
          <line x1="14" y1="11" x2="14" y2="17" />
        </svg>
      </button>
    </div>
    <div class="slot-card__body">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  workSeq: { type: Number, required: true },
  title: { type: String, default: '구간' },
  removable: { type: Boolean, default: false },
})
defineEmits(['remove'])
</script>

<style scoped>
.slot-card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.slot-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.slot-card__tag {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}

.slot-card__del {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.slot-card__del:hover {
  color: var(--color-danger);
}

.slot-card__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
</style>
