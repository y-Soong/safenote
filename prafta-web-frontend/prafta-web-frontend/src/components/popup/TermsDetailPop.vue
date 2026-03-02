<template>
  <div class="modal-overlay prafta-modal-popup min-h-screen">
    <div
      class="modal-content-wide"
      :style="{ top: position.y + 'px', left: position.x + 'px' }"
      ref="modalRef"
    >
      <div class="modal-header" @mousedown="startDrag">
        <span>{{ props.termsNm_p }}</span>
        <button class="icon-button" @click="$emit('close')">✕</button>
      </div>

      <!-- 약관 본문: 스크롤 한 곳만 (이중 스크롤바 방지) -->
      <div class="form-container terms-detail-form ml-5 mr-5">
        <div class="terms-detail-scroll">
          <label>약관 본문</label>
          <QuillEditor
            v-model:content="termsContent"
            contentType="html"
            theme="bubble"
            class="terms-detail-editor"
            :readOnly="readonly"
            ref="editorRef"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineProps } from "vue";
import { QuillEditor } from "@vueup/vue-quill";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import axios from "@/api/axios";

const modalRef = ref(null);
const props = defineProps({
  termsId_p: String,
  termsNm_p: String,
});

// 공통 훅으로 화면 중앙(살짝 위쪽)에 배치 + 드래그 가능
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const cmpnyCd = ref("");
const termsId = ref("");
const termsNm = ref("");
const termsContent = ref("");
const readonly = ref(true);

onMounted(async () => {
  cmpnyCd.value = sessionStorage.getItem("gv_cmpnyCd");
  termsId.value = props.termsId_p;
  termsNm.value = props.termsNm_p;

  await fnGetTermsInfo();
});

// API 호출
const fnGetTermsInfo = async () => {
  try {
    const response = await axios.post("/comApi/baseinfo/getTermsDInfo", {
      termsId: termsId.value,
    });

    if (response.status === 200) {
      termsContent.value = response.data.TERMS_CONTENT;
    }
  } catch (err) {
    alert(err.response.data.message);
  }
};

/* User Function */
</script>

<style scoped>
/* 이중 스크롤바 방지: form-container는 스크롤 없음, 내부 .terms-detail-scroll만 스크롤 */
.form-container.terms-detail-form {
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.terms-detail-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-top: 0.5rem;
}

.terms-detail-editor {
  min-height: 20rem;
}

/* Quill 내부 스크롤 제거 — .terms-detail-scroll만 스크롤 */
.terms-detail-scroll :deep(.ql-container) {
  overflow: visible;
}
.terms-detail-scroll :deep(.ql-editor) {
  overflow: visible;
}

.input {
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 8px;
  width: 100%;
}
.btn-save {
  background-color: #0d9488;
  color: white;
  padding: 10px 16px;
  border-radius: 6px;
}
.btn-cancel {
  background-color: #ddd;
  padding: 10px 16px;
  border-radius: 6px;
}
</style>
