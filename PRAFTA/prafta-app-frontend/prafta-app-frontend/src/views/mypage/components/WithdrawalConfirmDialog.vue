<!--
  WithdrawalConfirmDialog.vue — 회원 탈퇴 확인 모달 (추가 게이트)
  - 작업 ID: PRAFTA-APP-010-24
  - 호출자: MyPageView (010-10)
  - 사용자 호명 + 콜아웃(3년 보존/재가입 미연계) + 확인 체크박스 + Danger 버튼.
  - 체크박스 체크 시에만 [탈퇴하기] 활성(추가 게이트). confirm 시 부모가 010-07 처리.
  - 정책 출처: 공통 §3.5(탈퇴), 근로기준법 §42(3년 보존).
-->
<template>
  <transition name="wcd-fade">
    <div
      v-if="modelValue"
      class="wcd__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="회원 탈퇴 확인"
      @click.self="onCancel"
    >
      <div class="wcd">
        <div class="wcd__icon" aria-hidden="true">
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
            <path
              d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"
            />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
        </div>

        <h2 class="wcd__title">{{ userNm }}님, 탈퇴 전 꼭 확인해주세요</h2>
        <p class="wcd__desc">탈퇴 후 취소는 불가합니다</p>

        <!-- 콜아웃 -->
        <div class="wcd__callout">
          출퇴근 및 근태 기록은 법령에 따라 <strong>3년간 보존</strong>됩니다.<br />
          탈퇴 후 재가입 시 기존 출퇴근 및 근태 기록은 연계되지 않습니다.
        </div>

        <!-- 확인 체크박스 (추가 게이트) -->
        <button
          type="button"
          class="wcd__check"
          role="checkbox"
          :aria-checked="agreed"
          @click="agreed = !agreed"
        >
          <span class="wcd__check-box" :class="{ 'wcd__check-box--on': agreed }" aria-hidden="true">
            <svg
              v-if="agreed"
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </span>
          <span class="wcd__check-label">회원탈퇴 유의사항을 모두 확인했습니다</span>
        </button>

        <!-- 액션 (F-10 규약: 왼쪽=진행/확정(파괴적 진행=danger), 오른쪽=이탈(취소)) -->
        <div class="wcd__actions">
          <button
            type="button"
            class="wcd__btn wcd__btn--danger"
            :class="{ 'wcd__btn--off': !agreed }"
            :disabled="!agreed"
            @click="onConfirm"
          >
            탈퇴하기
          </button>
          <button type="button" class="wcd__btn wcd__btn--ghost" @click="onCancel">취소</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  // 사용자 이름 (호명용)
  userNm: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'confirm'])

// 확인 체크 (UI 상태 — 허용)
const agreed = ref(false)

// 모달이 닫히면 체크 초기화 (재오픈 시 다시 게이트)
watch(
  () => props.modelValue,
  (open) => {
    if (!open) agreed.value = false
  },
)

const onCancel = () => {
  emit('update:modelValue', false)
}
const onConfirm = () => {
  if (!agreed.value) return
  // 부모(MyPageView)가 실제 탈퇴 처리(010-07). 모달은 게이트 통과 신호만.
  emit('confirm')
  emit('update:modelValue', false)
}
</script>

<style scoped>
.wcd__dimmer {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.wcd {
  width: 100%;
  max-width: 340px;
  padding: 24px 20px 16px;
  background: var(--color-surface, #fff);
  border-radius: var(--radius-lg, 14px);
  text-align: center;
}
.wcd__icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full, 9999px);
  background: #fef2f2;
  color: var(--color-danger, #ef4444);
}
.wcd__title {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary, #111827);
}
.wcd__desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--color-text-secondary, #6b7280);
}
.wcd__callout {
  padding: 12px;
  margin-bottom: 16px;
  background: var(--color-bg, #f9fafb);
  border-radius: var(--radius-md, 10px);
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-secondary, #6b7280);
  text-align: left;
}
.wcd__callout strong {
  color: var(--color-text-primary, #111827);
}

/* 체크박스 */
.wcd__check {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 4px;
  background: transparent;
  border: 0;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.wcd__check-box {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-sm, 6px);
  color: var(--color-surface, #fff);
  background: var(--color-surface, #fff);
}
.wcd__check-box--on {
  background: var(--color-danger, #ef4444);
  border-color: var(--color-danger, #ef4444);
}
.wcd__check-label {
  font-size: 13px;
  color: var(--color-text-primary, #111827);
}

/* 액션 */
.wcd__actions {
  display: flex;
  gap: 8px;
}
.wcd__btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md, 10px);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  border: 0;
}
.wcd__btn--ghost {
  background: var(--color-surface, #fff);
  color: var(--color-text-secondary, #6b7280);
  border: 1px solid var(--color-border, #e5e7eb);
}
.wcd__btn--danger {
  background: var(--color-danger, #ef4444);
  color: var(--color-surface, #fff);
}
.wcd__btn--off {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-tertiary, #9ca3af);
  cursor: not-allowed;
}

.wcd-fade-enter-active,
.wcd-fade-leave-active {
  transition: opacity 0.2s ease;
}
.wcd-fade-enter-from,
.wcd-fade-leave-to {
  opacity: 0;
}
</style>
