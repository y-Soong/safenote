<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>조치 내역 입력</span>
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

        <!-- 본문: 조치 상세 textarea -->
        <div class="action-input-body">
          <label for="actionDesc">조치 상세 내역</label>
          <textarea
            id="actionDesc"
            v-model="actionDesc"
            class="action-textarea"
            placeholder="불량에 대한 처리 상세 내역을 입력해주세요."
            rows="8"
          ></textarea>
        </div>

        <!-- Footer -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSave">저장</button>
            <button class="btn btn-secondary" @click="$emit('close')">
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import {
  defineProps,
  defineEmits,
  ref,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({
  // 불량 식별키 (조치 upsert PK)
  siteCd_p: String,
  chkptCd_p: String,
  inspectItemCd_p: String,
  workDate_p: String,
  // 기존 조치 내역 (수정 시 프리필)
  actionDesc_p: String,
  // 저장 완료 콜백 (부모 목록 갱신)
  onSaved: Function,
});

const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const actionDesc = ref("");

onMounted(() => {
  // 기존 조치행이 있으면 프리필(수정 모드)
  actionDesc.value = props.actionDesc_p || "";
});

// 조치 내역 upsert 저장
const fnSave = async () => {
  // 단순 필수값 검증 (UI 레벨)
  if (proxy.$util.isEmpty(actionDesc.value)) {
    proxy.$alert("조치 상세 내역을 입력해주세요.");
    return;
  }

  try {
    const response = await axios.post("/webApi/chkLst04/save-defect-action", {
      siteCd: props.siteCd_p,
      chkptCd: props.chkptCd_p,
      inspectItemCd: props.inspectItemCd_p,
      workDate: props.workDate_p,
      actionDesc: actionDesc.value,
    });

    if (response.status === 200) {
      await proxy.$alert("저장되었습니다.");
      if (typeof props.onSaved === "function") {
        props.onSaved();
      }
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};
</script>

<style scoped>
/* 팝업 폭 고정 + 기본 .modal-content 의 padding(20px) 제거.
   기본 .modal-content 에만 패딩이 있어 헤더가 모서리까지 안 닿고
   바깥으로 흰 여백이 생기는 문제 → padding:0 으로 헤더/바디/푸터가 모서리에 밀착 */
.modal-content {
  width: 480px;
  max-width: 90vw;
  padding: 0;
}

/* 바디: 공통 가이드 패딩(20px)과 동일하게 맞춤 */
.action-input-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.action-input-body > label {
  font-weight: 600;
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.action-textarea {
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
  min-height: 8rem;
  padding: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  font-family: inherit;
  font-size: 0.875rem;
  line-height: 1.5;
}

/* 푸터 버튼: 한 줄 가로 배치 + 우측 정렬 (전역 .btn-group flex-wrap 무력화) */
.modal-footer .btn-group {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}
</style>
