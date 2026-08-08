<!--
  LogoutConfirmDialog.vue — 로그아웃 확인 모달
  - 작업 ID: PRAFTA-APP-010-23
  - 호출자: MyPageView (010-10)
  - 단순 질문(부가 설명 없음) + [취소]/[로그아웃]. confirm 시 부모가 010-06 처리.
-->
<template>
  <transition name="lcd-fade">
    <div
      v-if="modelValue"
      class="lcd__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="로그아웃 확인"
      @click.self="onCancel"
    >
      <div class="lcd">
        <div class="lcd__icon" aria-hidden="true">
          <svg
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <polyline points="16 17 21 12 16 7" />
            <line x1="21" y1="12" x2="9" y2="12" />
          </svg>
        </div>
        <h2 class="lcd__title">로그아웃할까요?</h2>
        <!-- F-10 규약: 왼쪽=진행/확정(로그아웃), 오른쪽=이탈(취소) -->
        <div class="lcd__actions">
          <button type="button" class="lcd__btn lcd__btn--primary" @click="onConfirm">
            로그아웃
          </button>
          <button type="button" class="lcd__btn lcd__btn--ghost" @click="onCancel">취소</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const onCancel = () => {
  emit('update:modelValue', false)
}
const onConfirm = () => {
  // 부모(MyPageView)가 실제 로그아웃 처리(010-06). 모달은 확정 신호만.
  emit('confirm')
  emit('update:modelValue', false)
}
</script>

<style scoped>
.lcd__dimmer {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.lcd {
  width: 100%;
  max-width: 320px;
  padding: 24px 20px 16px;
  background: var(--color-surface, #fff);
  border-radius: var(--radius-lg, 14px);
  text-align: center;
}
.lcd__icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full, 9999px);
  background: var(--color-primary-tint, #f0fdf4);
  color: var(--color-primary, #16a34a);
}
.lcd__title {
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary, #111827);
}
.lcd__actions {
  display: flex;
  gap: 8px;
}
.lcd__btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md, 10px);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  border: 0;
}
.lcd__btn--ghost {
  background: var(--color-surface, #fff);
  color: var(--color-text-secondary, #6b7280);
  border: 1px solid var(--color-border, #e5e7eb);
}
.lcd__btn--primary {
  background: var(--color-primary, #16a34a);
  color: var(--color-surface, #fff);
}

.lcd-fade-enter-active,
.lcd-fade-leave-active {
  transition: opacity 0.2s ease;
}
.lcd-fade-enter-from,
.lcd-fade-leave-to {
  opacity: 0;
}
</style>
