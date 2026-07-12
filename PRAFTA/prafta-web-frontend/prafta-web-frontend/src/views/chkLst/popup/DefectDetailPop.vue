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
          <span>불량 내용 상세</span>
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

        <!-- 본문 -->
        <div class="defect-detail-body">
          <div class="detail-row">
            <label>점검항목명</label>
            <input
              class="detail-input"
              type="text"
              :value="props.inspectItemSubj_p || '-'"
              disabled
            />
          </div>
          <div class="detail-row">
            <label>점검일자</label>
            <input
              class="detail-input"
              type="text"
              :value="props.workDate_p || '-'"
              disabled
            />
          </div>
          <div class="detail-row">
            <label>비고</label>
            <textarea
              class="detail-input detail-textarea"
              :value="props.answerDesc_p || '-'"
              rows="3"
              disabled
            ></textarea>
          </div>
          <div class="detail-row">
            <label>첨부사진</label>
            <div class="photo-area">
              <img
                v-if="props.fileMgmtCd_p"
                :src="imageUrl"
                alt="첨부사진"
                class="defect-photo"
              />
              <span v-else>-</span>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">
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
import { defineProps, defineEmits, ref, computed } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { buildFileServingUrl } from "@/utils/fileUrl";

const props = defineProps({
  inspectItemSubj_p: String,
  workDate_p: String,
  answerDesc_p: String,
  fileMgmtCd_p: String,
  filePath_p: String,
});

defineEmits(["close"]);

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// 이미지 서빙 URL: 공통 유틸로 조립 (백슬래시 정규화 + 동일 출처 상대경로)
const imageUrl = computed(() =>
  buildFileServingUrl(props.filePath_p, props.fileMgmtCd_p)
);
</script>

<style scoped>
/* 기본 .modal-content 의 padding(20px) 제거 — 헤더가 모서리까지 밀착되도록
   (modal-content-wide/normal/narrow 와 달리 기본 클래스에만 패딩이 있어 발생) */
.modal-content {
  width: 768px;
  max-width: 90vw;
  padding: 0;
}

.defect-detail-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 20px;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.detail-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.detail-row > label {
  font-weight: 600;
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

/* 데이터 표시부: 읽기전용 disabled input/textarea */
.detail-input {
  width: 100%;
  box-sizing: border-box;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  background: #f9fafb;
  color: var(--color-text);
  font-family: inherit;
  font-size: 0.875rem;
  line-height: 1.5;
}

/* disabled 기본 흐림 처리 무력화 — 값을 또렷이 보이게 */
.detail-input:disabled {
  opacity: 1;
  -webkit-text-fill-color: var(--color-text);
  cursor: default;
}

.detail-textarea {
  resize: vertical;
  white-space: pre-wrap;
  word-break: break-word;
}

.photo-area {
  display: flex;
  align-items: center;
  justify-content: center;
}

.defect-photo {
  max-width: 100%;
  max-height: 40vh;
  object-fit: contain;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

.modal-footer .btn-group {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
  width: 100%;
}
</style>
