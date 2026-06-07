<!--
  AdminApprovalRejectSheet.vue — 반려 사유 입력 시트
  - 작업 ID: 001-P2-F5 (분해: 001-phase2-admin-approval-plan.md §2-2 ⑤ / §5 ②)
  - 트리거: 상세(AdminApprovalDetailView) ⑤ 결정에서 '반려' + 처리하기 → open=true.
  - 백엔드: POST /appApi/admin/approval/process { reqId, group, decision:'REJECT', comment }
      반려 사유는 10자 이상 필수(공통 정책서 §9.5 / 재기획서 §5.6). 시트가 1차 검증.
  - 참조 패턴: views/admin/tbm/components/AdminTbmForceExitSheet.vue (바텀시트 + 토큰 자급).
  - planner 라운드 스코프: template + style 완성. script 는 props/emits + v-model + 단순 검증(길이)만.
  - developer 라운드 스코프: confirm 수신 → 부모가 process 호출.
-->
<template>
  <transition name="ap-sheet-fade">
    <div
      v-if="open"
      class="ap-sheet__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="반려 사유"
      @click.self="onClose"
    >
      <div class="ap-sheet">
        <div class="ap-sheet__handle" aria-hidden="true"></div>

        <header class="ap-sheet__header">
          <h2 class="ap-sheet__title">반려 사유</h2>
          <button type="button" class="ap-sheet__close" aria-label="닫기" @click="onClose">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="ap-sheet__body">
          <label class="ap-field">
            <span class="ap-field__label">반려 사유 (10자 이상 필수)</span>
            <textarea
              v-model="reason"
              class="ap-field__textarea"
              rows="4"
              maxlength="500"
              placeholder="요청자에게 전달될 반려 사유를 10자 이상 입력해 주세요."
            ></textarea>
          </label>
          <p class="ap-field__counter" :class="{ 'is-invalid': !isValid && reason.length > 0 }">
            {{ reason.length }} / 500
          </p>
        </div>

        <footer class="ap-sheet__footer">
          <button type="button" class="ap-sheet__btn ap-sheet__btn--ghost" @click="onClose">
            취소
          </button>
          <button
            type="button"
            class="ap-sheet__btn ap-sheet__btn--danger"
            :disabled="!isValid || submitting"
            @click="onConfirm"
          >
            반려하기
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  // 시트 열림 여부(부모 제어)
  open: { type: Boolean, default: false },
  // 부모가 process API 호출 중일 때 버튼 잠금
  submitting: { type: Boolean, default: false },
})

// close: 닫기 / confirm: 반려 확정 → 사유 문자열(10자↑ 보장)
const emit = defineEmits(['close', 'confirm'])

// 반려 사유(필수 10자↑)
const reason = ref('')

// 단순 길이 검증(UI 보조 — 서버도 재검증)
const isValid = computed(() => reason.value.trim().length >= 10)

const onClose = () => emit('close')

const onConfirm = () => {
  if (!isValid.value || props.submitting) return
  emit('confirm', reason.value.trim())
}

// 열릴 때 입력 초기화
watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) reason.value = ''
  },
)
</script>

<style scoped>
.ap-sheet__dimmer {
  /* 토큰 자급(self-contained): 상세 위에서 띄울 수 있어 직접 선언 */
  --color-surface: #ffffff;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-danger: #ef4444;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --radius-md: 10px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: fixed;
  inset: 0;
  background: var(--color-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 130;
}
.ap-sheet {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 80vh;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}
.ap-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.ap-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.ap-sheet__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ap-sheet__close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.ap-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
}
.ap-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.ap-field__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.ap-field__textarea {
  width: 100%;
  box-sizing: border-box;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
  resize: vertical;
  line-height: 1.5;
}
.ap-field__textarea:focus {
  outline: none;
  border-color: var(--color-danger);
}
.ap-field__counter {
  margin: var(--space-xs) 0 0;
  text-align: right;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.ap-field__counter.is-invalid {
  color: var(--color-danger);
}
.ap-sheet__footer {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.ap-sheet__btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.ap-sheet__btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.ap-sheet__btn--danger {
  background: var(--color-danger);
  color: var(--color-surface);
  border: 0;
}
.ap-sheet__btn--danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.ap-sheet-fade-enter-active,
.ap-sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}
.ap-sheet-fade-enter-from,
.ap-sheet-fade-leave-to {
  opacity: 0;
}
</style>
