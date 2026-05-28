<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-show="true"
        ref="overlayRef"
        class="modal-overlay prafta-modal-popup"
        tabindex="-1"
        @click.self="$emit('cancel')"
        @keydown.esc="$emit('cancel')"
      >
        <div class="modal-content reason-modal">
          <div class="reason-modal__header">
            {{ title }}
          </div>

          <div class="modal-body reason-modal__body">
            <p v-if="message" class="reason-modal__message">{{ message }}</p>

            <textarea
              v-model="reason"
              class="reason-modal__textarea"
              :placeholder="placeholder"
              :maxlength="maxLength"
              rows="5"
            ></textarea>

            <div class="reason-modal__meta">
              <span v-if="required && !isValid" class="reason-modal__hint">
                사유를 입력해 주세요.
              </span>
              <span class="reason-modal__count">
                {{ reason.length }} / {{ maxLength }}
              </span>
            </div>
          </div>

          <div class="modal-footer reason-modal__footer">
            <button
              class="btn btn-primary"
              :disabled="required && !isValid"
              @click="handleConfirm"
            >
              확인
            </button>
            <button class="btn btn-primary" @click="$emit('cancel')">
              취소
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from "vue";

const props = defineProps({
  title: { type: String, default: "반려 사유 입력" },
  message: { type: String, default: "" },
  placeholder: { type: String, default: "사유를 입력해 주세요." },
  maxLength: { type: Number, default: 500 },
  required: { type: Boolean, default: true },
});

const emit = defineEmits(["confirm", "cancel", "close"]);

// 입력 상태
const reason = ref("");
const overlayRef = ref(null);

// 단순 form validation (공백만 입력 차단) — UI 검증 범위
const isValid = computed(() => reason.value.trim().length > 0);

// 확인: 입력된 사유 문자열만 emit (반려 API 호출은 부모 영역)
const handleConfirm = () => {
  if (props.required && !isValid.value) return;
  emit("confirm", reason.value.trim());
};

onMounted(() => {
  nextTick(() => {
    overlayRef.value?.focus();
  });
});
</script>

<style scoped>
/* ConfirmModal.vue 오버레이 패턴 차용 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.modal-content {
  background: var(--color-surface);
  border-radius: var(--card-radius);
  width: 90%;
}

.reason-modal {
  max-width: 420px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.reason-modal__header {
  padding: var(--card-padding);
  font-weight: 600;
  font-size: 1rem;
  color: var(--color-text-strong);
  border-bottom: 1px solid var(--color-border);
}

.reason-modal__body {
  padding: var(--card-padding);
}

.reason-modal__message {
  margin: 0 0 0.6rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  white-space: pre-line;
}

.reason-modal__textarea {
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
  padding: 0.6rem 0.7rem;
  font-size: 0.9rem;
  color: var(--color-text);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius);
}

.reason-modal__textarea:focus {
  outline: var(--focus-ring-width) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset);
  border-color: var(--color-primary);
}

.reason-modal__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.4rem;
  font-size: 0.75rem;
}

.reason-modal__hint {
  color: var(--color-danger);
}

.reason-modal__count {
  margin-left: auto;
  color: var(--color-text-muted);
}

.reason-modal__footer {
  display: flex;
  justify-content: center;
  gap: 1rem;
  padding: 0 var(--card-padding) var(--card-padding);
}

.btn.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
