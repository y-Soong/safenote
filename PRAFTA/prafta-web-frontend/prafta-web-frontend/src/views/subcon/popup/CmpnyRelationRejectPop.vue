<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <div class="modal-header">
          <span>연동 요청 거부</span>
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
              ref="reasonFcs"
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
            <button class="btn btn-primary" @click="fnReject">거부</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, defineProps, defineEmits, getCurrentInstance, nextTick } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({ relationId: [Number, String], onSaved: Function });
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();

const comment = ref("");
const reasonFcs = ref(null);

// 저장 중복 클릭 방지 플래그.
const saving = ref(false);

// 거부 — POST /webApi/subcon01/relation-reject { relationId, comment }.
//   사유 필수(서버도 이중 검증). 성공: 알림 → 부모 목록 재조회(onSaved) → 닫기.
const fnReject = async () => {
  if (!comment.value) {
    await proxy.$alert("거부 사유를 입력해주세요.");
    await nextTick();
    reasonFcs.value?.focus();
    return;
  }

  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post("/webApi/subcon01/relation-reject", {
      relationId: props.relationId,
      comment: comment.value,
    });

    if (response.status === 200) {
      await proxy.$alert("처리되었습니다.");
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
.reason-input {
  width: 100%;
  resize: vertical;
}
</style>
