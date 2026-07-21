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

        <!-- 본문: 조치 상세 textarea + 조치 사진 -->
        <div class="action-input-body">
          <label for="actionDesc">조치 상세 내역</label>
          <textarea
            id="actionDesc"
            v-model="actionDesc"
            class="action-textarea"
            placeholder="불량에 대한 처리 상세 내역을 입력해주세요."
            rows="6"
          ></textarea>

          <!-- 조치 사진(선택) -->
          <label>조치 사진</label>
          <div class="action-photo-area">
            <!-- 새로 선택한 사진 미리보기 우선, 없으면 기존 조치 사진 표시 -->
            <img
              v-if="newPreviewUrl"
              :src="newPreviewUrl"
              alt="조치 사진 미리보기"
              class="action-photo"
            />
            <img
              v-else-if="existingImageUrl"
              :src="existingImageUrl"
              alt="기존 조치 사진"
              class="action-photo"
            />
            <span v-else class="action-photo-empty"
              >첨부된 조치 사진이 없습니다.</span
            >

            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              class="file-input-hidden"
              @change="fnOnFileChange"
            />
            <div class="photo-btn-row">
              <button
                type="button"
                class="btn btn-secondary"
                @click="fnTriggerFileSelect"
              >
                사진 선택
              </button>
              <button
                v-if="newFile"
                type="button"
                class="btn btn-secondary"
                @click="fnClearNewFile"
              >
                선택 취소
              </button>
            </div>
            <p class="hint">
              이미지 파일만 첨부할 수 있습니다. 새 사진을 첨부하지 않고 저장하면
              기존 조치 사진은 제거됩니다.
            </p>
          </div>
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
  computed,
  onMounted,
  onBeforeUnmount,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { buildFileServingUrl } from "@/utils/fileUrl";

const props = defineProps({
  // 불량 식별키 (조치 upsert PK)
  siteCd_p: String,
  chkptCd_p: String,
  inspectItemCd_p: String,
  workDate_p: String,
  // 기존 조치 내역 (수정 시 프리필)
  actionDesc_p: String,
  // 기존 조치 사진 (수정 시 표시) — 코드(+확장자) / 경로
  actionFileMgmtCd_p: String,
  actionFilePath_p: String,
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
const fileInputRef = ref(null);
const newFile = ref(null);
const newPreviewUrl = ref("");

// 기존 조치 사진 서빙 URL (새로 선택한 사진이 없을 때 표시)
const existingImageUrl = computed(() =>
  props.actionFileMgmtCd_p
    ? buildFileServingUrl(props.actionFilePath_p, props.actionFileMgmtCd_p)
    : ""
);

onMounted(() => {
  // 기존 조치행이 있으면 프리필(수정 모드)
  actionDesc.value = props.actionDesc_p || "";
});

onBeforeUnmount(() => {
  revokeNewPreview();
});

const fnTriggerFileSelect = () => {
  fileInputRef.value?.click();
};

const fnOnFileChange = (e) => {
  const file = e.target?.files?.[0] || null;
  revokeNewPreview();
  if (!file) {
    newFile.value = null;
    newPreviewUrl.value = "";
    return;
  }
  if (!file.type || !file.type.startsWith("image/")) {
    proxy.$alert("이미지 파일만 첨부할 수 있습니다.");
    e.target.value = "";
    return;
  }
  newFile.value = file;
  newPreviewUrl.value = URL.createObjectURL(file);
};

const fnClearNewFile = () => {
  revokeNewPreview();
  newFile.value = null;
  newPreviewUrl.value = "";
  if (fileInputRef.value) fileInputRef.value.value = "";
};

const revokeNewPreview = () => {
  if (newPreviewUrl.value) {
    URL.revokeObjectURL(newPreviewUrl.value);
    newPreviewUrl.value = "";
  }
};

// 조치 내역 upsert 저장 (multipart: 텍스트 + 조치 사진(선택))
const fnSave = async () => {
  // 단순 필수값 검증 (UI 레벨)
  if (proxy.$util.isEmpty(actionDesc.value)) {
    proxy.$alert("조치 상세 내역을 입력해주세요.");
    return;
  }

  try {
    const form = new FormData();
    form.append("siteCd", props.siteCd_p);
    form.append("chkptCd", props.chkptCd_p);
    form.append("inspectItemCd", props.inspectItemCd_p);
    form.append("workDate", props.workDate_p);
    form.append("actionDesc", actionDesc.value);
    if (newFile.value) {
      form.append("file", newFile.value, newFile.value.name);
    }

    const response = await axios.post(
      "/webApi/chkLst04/save-defect-action",
      form,
      { headers: { "Content-Type": "multipart/form-data" } }
    );

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
  min-height: 6rem;
  padding: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  font-family: inherit;
  font-size: 0.875rem;
  line-height: 1.5;
}

/* 조치 사진 영역 */
.action-photo-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-photo {
  max-width: 100%;
  max-height: 32vh;
  object-fit: contain;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

.action-photo-empty {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
}

.file-input-hidden {
  display: none;
}

.photo-btn-row {
  display: flex;
  flex-direction: row;
  gap: 8px;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  line-height: 1.4;
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
