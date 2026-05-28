<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content comp-modal" ref="modalRef">
        <div class="modal-header" @mousedown="startDrag">
          <span>이수 상태 변경</span>
          <button class="icon-button" @click="$emit('close')">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <div class="comp-body">
          <div class="comp-row">
            <span class="comp-label">대상</span>
            <span class="comp-value">{{ props.userNm_p }}</span>
          </div>
          <div class="comp-row">
            <span class="comp-label">현재 상태</span>
            <span class="comp-value">{{ currentStatusNm }}</span>
          </div>

          <div class="comp-radio-group">
            <label class="comp-radio">
              <input
                type="radio"
                value="COMPLETED"
                v-model="newStatusCd"
              />
              이수
            </label>
            <label class="comp-radio">
              <input
                type="radio"
                value="NOT_COMPLETED"
                v-model="newStatusCd"
              />
              미이수
            </label>
          </div>

          <div class="comp-reason">
            <label>변경 사유 (10자 이상)</label>
            <textarea
              v-model="reason"
              rows="4"
              maxlength="500"
              placeholder="이수 상태 변경 사유를 10자 이상 입력해 주세요."
            ></textarea>
            <div class="comp-reason-meta">
              <span v-if="reason.trim().length > 0 && reason.trim().length < 10" class="comp-hint">
                10자 이상 입력해 주세요.
              </span>
              <span class="comp-count">{{ reason.length }} / 500</span>
            </div>
          </div>

          <p class="comp-notice">
            ⚠️ 이수 상태 사후 변경은 관리자 책임으로 기록됩니다. (변경자/변경시각 저장)
          </p>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button
              class="btn btn-primary"
              :disabled="!isValid"
              @click="fnSubmit"
            >
              변경
            </button>
            <button class="btn btn-second" @click="$emit('close')">취소</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  computed,
  defineProps,
  defineEmits,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const { proxy } = getCurrentInstance();

const props = defineProps({
  attendanceCd_p: String,
  userNm_p: String,
  currentStatusCd_p: String,
  currentReason_p: String,
  onUpdated: Function,
});
const emit = defineEmits(["close"]);

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3,
});

// 기본 선택: 현재 상태의 반대로 유도(없으면 COMPLETED)
const newStatusCd = ref(
  props.currentStatusCd_p === "COMPLETED" ? "NOT_COMPLETED" : "COMPLETED"
);
const reason = ref("");

const currentStatusNm = computed(() => {
  switch (props.currentStatusCd_p) {
    case "COMPLETED":
      return "이수";
    case "NOT_COMPLETED":
      return "미이수";
    default:
      return "미처리";
  }
});

const isValid = computed(
  () =>
    !!newStatusCd.value &&
    reason.value.trim().length >= 10
);

const fnSubmit = async () => {
  if (!isValid.value) return;

  try {
    const response = await axios.post(
      "/webApi/tbm04/update-completion",
      {
        attendanceCd: props.attendanceCd_p,
        completionStatusCd: newStatusCd.value,
        reason: reason.value.trim(),
      },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      await proxy.$alert("이수 상태가 변경되었습니다.");
      emit("close");
      if (typeof props.onUpdated === "function") props.onUpdated();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "변경 중 오류가 발생했습니다.")
    );
  }
};
</script>

<style scoped>
.comp-modal {
  max-width: 460px;
  display: flex;
  flex-direction: column;
}

.comp-body {
  padding: var(--card-padding);
}

.comp-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.comp-label {
  width: 80px;
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.comp-value {
  font-weight: 600;
  color: var(--color-text-strong);
}

.comp-radio-group {
  display: flex;
  gap: 1.5rem;
  margin: 0.75rem 0;
  padding: 0.5rem 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}

.comp-radio {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--color-text);
  cursor: pointer;
}

.comp-reason {
  margin-top: 0.75rem;
}

.comp-reason label {
  display: block;
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
  margin-bottom: 0.35rem;
}

.comp-reason textarea {
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
  padding: 0.6rem 0.7rem;
  font-size: 0.9rem;
  color: var(--color-text);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius);
}

.comp-reason textarea:focus {
  outline: var(--focus-ring-width) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset);
  border-color: var(--color-primary);
}

.comp-reason-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.35rem;
  font-size: 0.75rem;
}

.comp-hint {
  color: var(--color-danger);
}

.comp-count {
  margin-left: auto;
  color: var(--color-text-muted);
}

.comp-notice {
  margin: 0.75rem 0 0;
  font-size: var(--btn-font-sm);
  color: var(--color-danger);
}

.btn.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
