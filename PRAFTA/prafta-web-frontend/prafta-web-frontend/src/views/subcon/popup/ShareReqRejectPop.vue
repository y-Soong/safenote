<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <div class="modal-header">
          <span>데이터 공유 요청 거부</span>
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
        <div class="form-container">
          <div class="form-row-max form-row-top">
            <label>거부 사유</label>
            <textarea
              v-model.trim="comment"
              class="reason-input"
              placeholder="거부 사유 (필수, 최대 500자)"
              maxlength="500"
              rows="3"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">
              취소
            </button>
            <button
              class="btn btn-primary"
              :disabled="!comment"
              @click="fnReject"
            >
              거부
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, defineProps, defineEmits, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({ shareReqId: [Number, String], onSaved: Function });
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();

const comment = ref("");

// 거부 중복 클릭 방지 플래그.
const saving = ref(false);

// 거부 — POST /webApi/subcon03/share-req-reject { shareReqId, comment }. 사유 필수(서버도 재검증).
const fnReject = async () => {
  if (!comment.value) {
    await proxy.$alert("거부 사유를 입력해주세요.");
    return;
  }

  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post("/webApi/subcon03/share-req-reject", {
      shareReqId: props.shareReqId,
      comment: comment.value,
    });

    if (response.status === 200) {
      await proxy.$alert("거부되었습니다.");
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "거부 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.form-row-max {
  display: flex;
  gap: var(--space-sm, 0.5rem);
}
.form-row-top {
  align-items: flex-start;
}
.form-row-max label {
  width: 90px;
  flex-shrink: 0;
  padding-top: 0.35rem;
  color: var(--color-text-muted, #6b7280);
}
.reason-input {
  flex: 1;
  min-width: 0;
  resize: vertical;
}
</style>
