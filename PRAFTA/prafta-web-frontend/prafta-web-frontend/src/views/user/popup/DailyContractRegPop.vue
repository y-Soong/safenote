<template>
  <!--
    DailyContractRegPop.vue — 일용직 근로계약서 등록/교체 팝업 (웹)
    - 분해: .claude/requests/common/작업지시서_일용직-계약서-서명-승인제.plan.md §4 UI-DC-07 / §2 T5
    - 요청서 근거: §4-2 화면 A(업로드/교체=버전 증가), D8(교체 시 전원 재서명 트리거)
    - 참조 패턴: DailyBlacklistRegPop(modal-overlay/modal-content-narrow 규약), 팝업 중앙정렬/높이 바운딩 규약
    - planner 라운드 스코프: template + style + 파일 선택 미리보기(UI 토글). script 저장은 TODO(developer).
  -->
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <!-- Title -->
        <div class="modal-header">
          <span>{{
            props.hasActive ? "계약서 교체(새 버전)" : "계약서 등록"
          }}</span>
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

        <!-- Form -->
        <div class="form-container">
          <p class="reg-guide">
            등록하면 새 버전이 생성되고 기존 활성 계약서는 자동 종료됩니다. 해당
            사업장의 모든 일용직은 <strong>다음 로그인 시 재서명</strong>해야
            합니다.
          </p>

          <!-- 계약서명 고정 — 일용직 로그인 시 공통 서명 대상이라 별도 명칭 불필요(수정 불가) -->
          <div class="form-row-max">
            <label>계약서명</label>
            <input id="contractNm" :value="CONTRACT_NM" disabled />
          </div>

          <div class="form-row-max form-row-top">
            <label>계약서 이미지</label>
            <div class="file-field">
              <input
                ref="fileInputRef"
                type="file"
                accept="image/*"
                class="file-field__input"
                @change="onFileChange"
              />
              <p class="file-field__hint">이미지 파일(JPG/PNG), 10MB 이하</p>
              <span class="form-msg">{{ fileMsg }}</span>
            </div>
          </div>

          <!-- 미리보기 -->
          <div v-if="previewUrl" class="preview-box">
            <img
              class="preview-box__img"
              :src="previewUrl"
              alt="계약서 미리보기"
            />
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">
              취소
            </button>
            <button class="btn btn-primary" :disabled="saving" @click="fnSave">
              저장
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
  ref,
  defineProps,
  defineEmits,
  getCurrentInstance,
  onBeforeUnmount,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// siteCd: 대상 사업장 / hasActive: 활성 계약서 존재 여부(타이틀·안내 분기) / onSaved: 저장 성공 콜백
const props = defineProps({
  siteCd: String,
  hasActive: Boolean,
  onSaved: Function,
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

// =========================== Ref ===========================
// 계약서명 고정값 — 사용자 입력 불가(2026-07-17 결정: 로그인 공통 서명 대상이라 명칭 구분 불필요)
const CONTRACT_NM = "일용직 표준 근로 계약서";
const fileMsg = ref("");
const selectedFile = ref(null);
const previewUrl = ref("");
const saving = ref(false);

const fileInputRef = ref(null);

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

// =========================== Methods ===========================
// 파일 선택 → 클라이언트 1차 검증(이미지/크기) + 미리보기(UI 토글 — 허용 범위)
const onFileChange = (e) => {
  fileMsg.value = "";
  const file = e.target.files?.[0] || null;

  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
    previewUrl.value = "";
  }
  selectedFile.value = null;

  if (!file) return;

  if (!file.type.startsWith("image/")) {
    fileMsg.value = "이미지 파일만 업로드할 수 있습니다.";
    fileInputRef.value.value = "";
    return;
  }
  if (file.size > MAX_FILE_SIZE) {
    fileMsg.value = "파일 크기는 10MB 이하여야 합니다.";
    fileInputRef.value.value = "";
    return;
  }

  selectedFile.value = file;
  previewUrl.value = URL.createObjectURL(file);
};

const fnSave = async () => {
  fileMsg.value = "";

  // 1) 입력 검증 — 파일 필수(계약서명은 고정값이라 검증 불필요).
  if (!selectedFile.value) {
    fileMsg.value = "계약서 이미지 파일을 선택해주세요.";
    return;
  }

  // 2) D8 재서명 트리거 경고 — 업로드 = 새 버전(기존 활성 자동 종료 → 전원 재서명).
  const ok = await proxy.$confirm(
    props.hasActive
      ? "계약서를 교체(새 버전 등록)하시겠습니까?\n해당 사업장의 모든 일용직은 다음 로그인 시 재서명해야 합니다."
      : "계약서를 등록하시겠습니까?\n해당 사업장의 모든 일용직은 다음 로그인 시 재서명해야 합니다.",
  );
  if (!ok) return;

  if (saving.value) return;
  saving.value = true;

  // 3) 등록 — POST /webApi/user07/contract (multipart/form-data).
  //    cmpnyCd 는 서버 JWT 클레임 사용(절대 전달 금지). 서버가 확장자/크기/이미지 디코딩을 재검증한다.
  try {
    const formData = new FormData();
    formData.append("siteCd", props.siteCd);
    formData.append("contractNm", CONTRACT_NM);
    formData.append("file", selectedFile.value);

    const response = await axios.post("/webApi/user07/contract", formData);

    if (response.status === 200) {
      await proxy.$alert("등록되었습니다. 다음 로그인부터 재서명이 적용됩니다.");
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "등록 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};

onBeforeUnmount(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value);
});
</script>

<style scoped>
.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.2rem;
  max-width: 520px;
  margin: 0 auto;
}

.reg-guide {
  margin: 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #4b5563);
  background: var(--color-warning-bg, #fef3c7);
  border-radius: var(--btn-radius, 8px);
  padding: 0.5rem 0.75rem;
  line-height: 1.5;
}

.form-row-top {
  align-items: flex-start;
}

.file-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  width: 100%;
}
.file-field__input {
  font-size: 0.8rem;
}
.file-field__hint {
  margin: 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #6b7280);
}

/* 미리보기 — 팝업 높이 바운딩 규약: 내부 스크롤로 수용 */
.preview-box {
  max-height: 38vh;
  overflow-y: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 8px);
  padding: 0.5rem;
}
.preview-box__img {
  display: block;
  width: 100%;
  height: auto;
}
</style>
