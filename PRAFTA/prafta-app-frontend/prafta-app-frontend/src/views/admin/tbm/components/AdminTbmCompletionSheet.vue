<!--
  AdminTbmCompletionSheet.vue — 개별 이수/미이수 처리 시트 (R3)
  - 작업 ID: 001-P5-T-F10 (분해: 001-phase5-admin-tbm-plan.md §2-5, §3-H T-A8)
  - 트리거: 종료화면(AdminTbmCompletedView, GPS 검증 세션 한정)에서 근로자 행 "이수 상태 변경" → open=true.
  - 백엔드: POST /appApi/admin/tbm/sessions/{sessionCd}/attendees/{attendanceCd}/completion
      body { completionStatusCd, reason } (T4 확정: GPS 세션만. 미이수 사유 10자 이상 서버 검증).
  - 동작: 이수↔미이수 토글. 미이수 선택 시 사유 textarea(10자 이상 필수, web TbmCompletionModal 준용).
      확인 시 confirm({ completionStatusCd, reason }) emit. 실제 API 는 부모/developer.
  - 참조 패턴: views/tbm/components/TbmEntryPwdSheet.vue(시트) + web popup/TbmCompletionModal.vue(검증 규칙).
  - planner 라운드 스코프: template + style 완성. script 는 props/emits + v-model + 단순 검증/토글만.
-->
<template>
  <transition name="cp-sheet-fade">
    <div
      v-if="open"
      class="cp-sheet__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="이수 상태 변경"
      @click.self="onClose"
    >
      <div class="cp-sheet">
        <div class="cp-sheet__handle" aria-hidden="true"></div>

        <header class="cp-sheet__header">
          <h2 class="cp-sheet__title">이수 상태 변경</h2>
          <button type="button" class="cp-sheet__close" aria-label="닫기" @click="onClose">
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

        <div class="cp-sheet__body">
          <p v-if="attendee" class="cp-sheet__target">
            <span class="cp-sheet__target-label">대상</span>
            <span class="cp-sheet__target-name">{{ attendee.userNm || '-' }}</span>
          </p>

          <!-- 이수↔미이수 토글 -->
          <div class="cp-toggle" role="radiogroup" aria-label="이수 상태">
            <label
              v-for="opt in statusOptions"
              :key="opt.value"
              class="cp-toggle__item"
              :class="{ 'is-checked': completionStatusCd === opt.value }"
            >
              <input
                type="radio"
                name="completionStatus"
                :value="opt.value"
                v-model="completionStatusCd"
              />
              <span>{{ opt.label }}</span>
            </label>
          </div>

          <!-- 미이수 사유(미이수 선택 시 — 10자 이상 필수) -->
          <label v-if="isNotCompleted" class="cp-field">
            <span class="cp-field__label">변경 사유 (10자 이상)</span>
            <textarea
              v-model="reason"
              class="cp-field__textarea"
              rows="4"
              maxlength="500"
              placeholder="미이수 처리 사유를 10자 이상 입력해 주세요."
            ></textarea>
            <span class="cp-field__meta">
              <span v-if="reasonTrimmed.length > 0 && reasonTrimmed.length < 10" class="cp-field__hint">
                10자 이상 입력해 주세요.
              </span>
              <span class="cp-field__count">{{ reason.length }} / 500</span>
            </span>
          </label>

          <p class="cp-sheet__notice">
            이수 상태 변경은 관리자 책임으로 기록돼요. (변경자 / 변경시각 저장)
          </p>
        </div>

        <!-- F-10 규약: 왼쪽=진행/확정(변경), 오른쪽=이탈(취소) -->
        <footer class="cp-sheet__footer">
          <button
            type="button"
            class="cp-sheet__btn cp-sheet__btn--primary"
            :disabled="!canSubmit || submitting"
            @click="onConfirm"
          >
            변경
          </button>
          <button type="button" class="cp-sheet__btn cp-sheet__btn--ghost" @click="onClose">
            취소
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
  // 대상 출결: { attendanceCd, userNm, completionStatusCd, ... }
  attendee: { type: Object, default: null },
  // 부모가 completion API 호출 중일 때 버튼 잠금
  submitting: { type: Boolean, default: false },
})

// close: 닫기 / confirm: { completionStatusCd, reason }
const emit = defineEmits(['close', 'confirm'])

// 이수상태(SYS053) 토글 옵션
const statusOptions = [
  { value: 'COMPLETED', label: '이수' },
  { value: 'NOT_COMPLETED', label: '미이수' },
]

// 선택 상태/사유(developer: 초기값은 watch 에서 현재 상태 반대로 유도)
const completionStatusCd = ref('COMPLETED')
const reason = ref('')

const reasonTrimmed = computed(() => reason.value.trim())
const isNotCompleted = computed(() => completionStatusCd.value === 'NOT_COMPLETED')

// 단순 검증(미이수면 사유 10자 이상 필수 — web TbmCompletionModal 준용)
const canSubmit = computed(() => {
  if (!completionStatusCd.value) return false
  if (isNotCompleted.value) return reasonTrimmed.value.length >= 10
  return true
})

const onClose = () => emit('close')

const onConfirm = () => {
  if (!canSubmit.value || props.submitting) return
  emit('confirm', {
    completionStatusCd: completionStatusCd.value,
    reason: isNotCompleted.value ? reasonTrimmed.value : '',
  })
}

// 열릴 때: 현재 상태의 반대로 기본 선택(없으면 COMPLETED), 사유 초기화
watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) return
    completionStatusCd.value =
      props.attendee?.completionStatusCd === 'COMPLETED' ? 'NOT_COMPLETED' : 'COMPLETED'
    reason.value = ''
  },
)
</script>

<style scoped>
.cp-sheet__dimmer {
  /* 토큰 자급(self-contained): 종료화면 위 카드에서 띄울 수 있어 직접 선언 */
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-surface: #ffffff;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-danger-text: #b91c1c;
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
.cp-sheet {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 82vh;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}
.cp-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.cp-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.cp-sheet__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.cp-sheet__close {
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
.cp-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.cp-sheet__target {
  margin: 0;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.cp-sheet__target-label {
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.cp-sheet__target-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.cp-toggle {
  display: flex;
  gap: var(--space-sm);
}
.cp-toggle__item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  padding: 10px var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-primary);
  cursor: pointer;
  background: var(--color-surface);
}
.cp-toggle__item.is-checked {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-weight: 600;
}
.cp-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.cp-field__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.cp-field__textarea {
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
.cp-field__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}
.cp-field__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}
.cp-field__hint {
  color: var(--color-danger-text);
}
.cp-field__count {
  margin-left: auto;
  color: var(--color-text-tertiary);
}
.cp-sheet__notice {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-tertiary);
}
.cp-sheet__footer {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.cp-sheet__btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.cp-sheet__btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.cp-sheet__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.cp-sheet__btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.cp-sheet-fade-enter-active,
.cp-sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}
.cp-sheet-fade-enter-from,
.cp-sheet-fade-leave-to {
  opacity: 0;
}
</style>
