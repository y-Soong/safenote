<!--
  TbmEntryPwdSheet.vue — TBM 입실 비밀번호 바텀시트
  - 작업 ID: PRAFTA-TBM-ENTRY-PWD (분해: prafta-app-tbm-user-detail-plan.md §4 F4)
  - 트리거: 참석가능/교육중 카드 선택 시 부모가 open. 비번 입력 → submit emit.
  - 백엔드: 실제 POST /appApi/tbm/enter(비번+GPS) 호출은 부모/developer 가 담당.
    이 시트는 비번 입력 수집 + submit 만 책임진다(Q4: 앱은 입력만 받음 전제).
  - 참조 패턴: views/req/components/BaseBottomSheet.vue, OffsiteReasonSheet.vue(토큰 자급).
  - planner 라운드 스코프: template + style 완성. script 는 props/emits + v-model + 단순 검증만.
-->
<template>
  <transition name="tbm-sheet-fade">
    <div
      v-if="modelValue"
      class="tbm-sheet__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="입실 비밀번호 입력"
      @click.self="onClose"
    >
      <div class="tbm-sheet">
        <div class="tbm-sheet__handle" aria-hidden="true"></div>

        <header class="tbm-sheet__header">
          <h2 class="tbm-sheet__title">입실하기</h2>
          <button type="button" class="tbm-sheet__close" aria-label="닫기" @click="onClose">
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

        <div class="tbm-sheet__body">
          <p v-if="title" class="tbm-sheet__session">{{ title }}</p>

          <label class="tbm-field">
            <span class="tbm-field__label">입실 비밀번호</span>
            <input
              v-model="entryPwd"
              class="tbm-field__input"
              type="text"
              inputmode="numeric"
              maxlength="6"
              autocomplete="off"
              placeholder="6자리 숫자"
            />
          </label>

          <p v-if="errorMsg" class="tbm-form-error">{{ errorMsg }}</p>
        </div>

        <footer class="tbm-sheet__footer">
          <button
            type="button"
            class="tbm-sheet__btn tbm-sheet__btn--primary"
            :disabled="!canSubmit || submitting"
            @click="onSubmit"
          >
            입실하기
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 선택된 세션 제목(표시용)
  title: { type: String, default: '' },
  // 부모가 enter 호출 중일 때 버튼 잠금
  submitting: { type: Boolean, default: false },
  // 부모가 서버 오류 메시지를 내려줄 수 있음(비번 불일치/거리초과 등)
  errorMsg: { type: String, default: '' },
})

// submit: { entryPwd } — 실제 enter API + GPS 는 부모/developer 가 처리
const emit = defineEmits(['update:modelValue', 'submit'])

// 입력 바인딩
const entryPwd = ref('')

// 단순 검증(6자리). 값 가공 아님.
const canSubmit = computed(() => entryPwd.value.length === 6)

const onClose = () => {
  emit('update:modelValue', false)
}

const onSubmit = () => {
  if (!canSubmit.value || props.submitting) return
  emit('submit', { entryPwd: entryPwd.value })
}

// 열릴 때 입력 초기화
watch(
  () => props.modelValue,
  (open) => {
    if (open) entryPwd.value = ''
  },
)
</script>

<style scoped>
.tbm-sheet__dimmer {
  /* 토큰 자급(self-contained): 허브 외부(카드 위)에서 띄울 수 있어 필수 토큰을 직접 선언 */
  --color-primary: #16a34a;
  --color-surface: #ffffff;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-danger-text: #b91c1c;
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
  z-index: 110;
}
.tbm-sheet {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}
.tbm-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.tbm-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.tbm-sheet__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.tbm-sheet__close {
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
.tbm-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.tbm-sheet__session {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.tbm-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.tbm-field__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.tbm-field__input {
  height: 44px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.tbm-field__input:focus {
  outline: none;
  border-color: var(--color-primary);
}
.tbm-form-error {
  margin: 0;
  font-size: 13px;
  color: var(--color-danger-text);
}
.tbm-sheet__footer {
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.tbm-sheet__btn {
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.tbm-sheet__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
}
.tbm-sheet__btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.tbm-sheet-fade-enter-active,
.tbm-sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}
.tbm-sheet-fade-enter-from,
.tbm-sheet-fade-leave-to {
  opacity: 0;
}
</style>
