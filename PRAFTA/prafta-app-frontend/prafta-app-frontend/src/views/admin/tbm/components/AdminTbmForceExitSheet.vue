<!--
  AdminTbmForceExitSheet.vue — 강제 퇴실 사유 입력 시트 (R3)
  - 작업 ID: 001-P5-T-F9 (분해: 001-phase5-admin-tbm-plan.md §2-4, §3-G T-A7)
  - 트리거: 진행화면(AdminTbmLiveView)에서 입실자 행의 "강제 퇴실" 버튼 → open=true.
  - 백엔드: POST /appApi/admin/tbm/sessions/{sessionCd}/attendees/{attendanceCd}/force-exit
      body { reason } (T3 확정: 사유 nullable — 비워도 제출 가능. 자동 미이수 처리됨).
  - 동작: 사유 textarea(선택 입력) + 자동 미이수 안내. 확인 시 confirm(reason||'') emit.
      실제 API 호출은 부모(LiveView)/developer 가 수행.
  - 참조 패턴: views/tbm/components/TbmEntryPwdSheet.vue (바텀시트 + 토큰 자급).
  - planner 라운드 스코프: template + style 완성. script 는 props/emits + v-model + 단순 토글만.
-->
<template>
  <transition name="fx-sheet-fade">
    <div
      v-if="open"
      class="fx-sheet__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="강제 퇴실"
      @click.self="onClose"
    >
      <div class="fx-sheet">
        <div class="fx-sheet__handle" aria-hidden="true"></div>

        <header class="fx-sheet__header">
          <h2 class="fx-sheet__title">강제 퇴실</h2>
          <button type="button" class="fx-sheet__close" aria-label="닫기" @click="onClose">
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

        <div class="fx-sheet__body">
          <p v-if="attendee" class="fx-sheet__target">
            <span class="fx-sheet__target-label">대상</span>
            <span class="fx-sheet__target-name">{{ attendee.userNm || '-' }}</span>
          </p>

          <label class="fx-field">
            <span class="fx-field__label">강제 퇴실 사유 (선택)</span>
            <textarea
              v-model="reason"
              class="fx-field__textarea"
              rows="4"
              maxlength="500"
              placeholder="사유를 입력하지 않아도 강제 퇴실할 수 있어요."
            ></textarea>
          </label>

          <p class="fx-sheet__notice">
            강제 퇴실하면 해당 근로자는 <strong>자동으로 미이수 처리</strong>되며,
            처리자/처리시각이 기록돼요.
          </p>
        </div>

        <!-- F-10 규약: 왼쪽=진행/확정(파괴적 진행=danger), 오른쪽=이탈(취소) -->
        <footer class="fx-sheet__footer">
          <button
            type="button"
            class="fx-sheet__btn fx-sheet__btn--danger"
            :disabled="submitting"
            @click="onConfirm"
          >
            강제 퇴실
          </button>
          <button type="button" class="fx-sheet__btn fx-sheet__btn--ghost" @click="onClose">
            취소
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  // 시트 열림 여부(부모 제어)
  open: { type: Boolean, default: false },
  // 대상 출결: { attendanceCd, userNm, ... }
  attendee: { type: Object, default: null },
  // 부모가 force-exit API 호출 중일 때 버튼 잠금
  submitting: { type: Boolean, default: false },
})

// close: 닫기 / confirm: 강제 퇴실 확정 → reason 문자열(빈 문자열 허용)
const emit = defineEmits(['close', 'confirm'])

// 사유 입력(선택 — T3: nullable)
const reason = ref('')

const onClose = () => emit('close')

// 확정: 사유는 비어도 제출 가능. 공백만이면 '' 로 정규화.
const onConfirm = () => {
  if (props.submitting) return
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
.fx-sheet__dimmer {
  /* 토큰 자급(self-contained): 진행화면 위 카드에서 띄울 수 있어 직접 선언 */
  --color-surface: #ffffff;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-danger: #ef4444;
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
.fx-sheet {
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
.fx-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.fx-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.fx-sheet__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.fx-sheet__close {
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
.fx-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.fx-sheet__target {
  margin: 0;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.fx-sheet__target-label {
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.fx-sheet__target-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.fx-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.fx-field__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.fx-field__textarea {
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
.fx-field__textarea:focus {
  outline: none;
  border-color: var(--color-danger);
}
.fx-sheet__notice {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-danger-text);
}
.fx-sheet__footer {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.fx-sheet__btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.fx-sheet__btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.fx-sheet__btn--danger {
  background: var(--color-danger);
  color: var(--color-surface);
  border: 0;
}
.fx-sheet__btn--danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.fx-sheet-fade-enter-active,
.fx-sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}
.fx-sheet-fade-enter-from,
.fx-sheet-fade-leave-to {
  opacity: 0;
}
</style>
