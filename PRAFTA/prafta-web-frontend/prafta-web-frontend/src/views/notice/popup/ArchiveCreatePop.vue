<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 (드래그) -->
        <div class="modal-header" @mousedown="startDrag">
          <span>신규 자료</span>
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

        <div class="archive-form">
          <!-- 자료타입 (필수) -->
          <div class="form-row">
            <label>자료타입 <span class="req">*</span></label>
            <!-- 자료타입 = 공통코드 COM008 (tb_baim_val_d) 재사용. 서버도 필수 재검증. -->
            <select
              v-model="formData.archiveTypeCd"
              name="combo"
              class="type-select"
            >
              <!-- 등록 폼에는 "전체"(빈값) 옵션을 두지 않는다. 안내 placeholder 만 두되
                   disabled+hidden 으로 빈값 재선택을 막는다(미선택 저장은 fnSave + 서버가 이중 차단). -->
              <option value="" disabled hidden>자료타입을 선택해 주세요</option>
              <option
                v-for="t in archiveTypeList"
                :key="t.baimValDCd"
                :value="t.baimValDCd"
              >
                {{ t.baimValDNm }}
              </option>
            </select>
          </div>

          <!-- 기본 정보 -->
          <div class="form-row">
            <label>제목 <span class="req">*</span></label>
            <input
              v-model="formData.title"
              maxlength="200"
              placeholder="제목을 입력해 주세요"
              autocomplete="off"
            />
          </div>
          <div class="form-row">
            <label>내용</label>
            <textarea
              v-model="formData.content"
              rows="6"
              placeholder="내용을 입력해 주세요"
            ></textarea>
          </div>

          <hr class="form-divider" />

          <!-- 비밀번호 (필수, BCrypt 저장). 생성 시엔 master 포함 비번 필수(공지와 동일).
               master 면제는 등록 이후의 수정/삭제/접근에만 적용된다. -->
          <div class="form-row">
            <label>비밀번호 <span class="req">*</span></label>
            <input
              v-model="formData.editPwd"
              type="password"
              maxlength="50"
              placeholder="수정 비밀번호 (저장 시 암호화)"
              autocomplete="new-password"
            />
          </div>
          <div class="form-row">
            <label>비밀번호 확인 <span class="req">*</span></label>
            <div class="pwd-confirm-area">
              <input
                v-model="formData.editPwdConfirm"
                type="password"
                maxlength="50"
                placeholder="비밀번호를 다시 입력해 주세요"
                autocomplete="new-password"
              />
              <!-- 오타 방지: 입력값이 있을 때 일치 여부 즉시 표시 -->
              <p
                v-if="formData.editPwdConfirm"
                class="pwd-confirm-msg"
                :class="pwdMatched ? 'is-ok' : 'is-error'"
              >
                {{
                  pwdMatched
                    ? "비밀번호가 일치합니다."
                    : "비밀번호가 일치하지 않습니다."
                }}
              </p>
            </div>
          </div>

          <hr class="form-divider" />

          <!-- 첨부 (다건) -->
          <div class="form-row">
            <label>첨부파일</label>
            <div class="file-area">
              <!-- 공통 파일 컴포넌트 미존재 → native input 은 숨기고 버튼으로 트리거. 다건. -->
              <input
                ref="fileInputRef"
                type="file"
                multiple
                accept="image/*,video/*,audio/*,text/*,.csv,.tsv,.log,.md,.json,.xml,.rtf"
                class="file-input-hidden"
                @change="fnOnFileChange"
              />
              <button
                type="button"
                class="btn-secondary file-select-btn"
                @click="fnTriggerFileSelect"
              >
                파일 선택
              </button>
              <ul class="file-list" v-if="fileList.length > 0">
                <li v-for="(f, i) in fileList" :key="i" class="file-item">
                  <span>{{ f.name }}</span>
                  <button class="file-item__del" @click="fnRemoveFile(i)">
                    ×
                  </button>
                </li>
              </ul>
              <p class="hint">
                텍스트/이미지/동영상/음성 파일만 첨부할 수
                있습니다(실행/스크립트 형식 제외). 저장 시 함께 업로드됩니다.
              </p>
            </div>
          </div>
        </div>

        <!-- 액션 (F-10 규약: 왼쪽=진행/확정(저장), 오른쪽=이탈(취소), 폭 균등) -->
        <div class="modal-footer">
          <button class="btn-primary" @click="fnSave">저장</button>
          <button class="btn-secondary" @click="$emit('close')">취소</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  reactive,
  computed,
  onMounted,
  defineProps,
  defineEmits,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const props = defineProps({
  onSave: Function,
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

// 모달 드래그 (공통 훅으로 화면 중앙 배치 + 드래그 가능)
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// 폼 상태 (대상지정/팝업/고정 없음 — 자료실은 회사 전체 공통)
const formData = reactive({
  archiveTypeCd: "",
  title: "",
  content: "",
  editPwd: "",
  editPwdConfirm: "",
});
const fileList = ref([]);
const fileInputRef = ref(null);
// 저장/업로드 진행 중 플래그(중복 제출 방지)
const saving = ref(false);

// 자료타입 드롭다운 목록 (공통코드 COM008 로 채움 — baimValDCd/baimValDNm)
const archiveTypeList = ref([]);

// 비밀번호/비밀번호 확인 일치 여부 (오타 방지 즉시 피드백)
const pwdMatched = computed(
  () => !!formData.editPwd && formData.editPwd === formData.editPwdConfirm
);

onMounted(async () => {
  await fnLoadArchiveTypes();
});

// 자료타입 드롭다운 조회 — 공통코드 COM008(tb_baim_val_d) 재사용
const fnLoadArchiveTypes = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM008"],
      },
    });
    if (response.status === 200) {
      // base-info-lists 는 여러 코드그룹을 평면 배열로 반환 → COM008 만 필터링
      const list = response.data?.baseInfoList || [];
      archiveTypeList.value = list.filter((item) => item.baimValCd === "COM008");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "자료타입 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  }
};

// 첨부 (UI 바인딩만) — 숨긴 native input 을 버튼으로 트리거
const fnTriggerFileSelect = () => {
  fileInputRef.value?.click();
};
const fnOnFileChange = (e) => {
  const files = Array.from(e.target.files || []);
  fileList.value = [...fileList.value, ...files];
  // 동일 파일 재선택 시에도 change 가 발생하도록 input 값 초기화
  if (e.target) e.target.value = "";
};
const fnRemoveFile = (i) => {
  fileList.value.splice(i, 1);
};

// 첨부 단건 업로드 → tb_file_info 선저장 후 발급된 FILE_MGMT_CD 반환(저장 시 fileList 에 매핑)
const fnUploadFile = async (file) => {
  const form = new FormData();
  form.append("file", file);
  const response = await axios.post("/webApi/notice02/upload-file", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return response.data; // { fileMgmtCd, fileNm }
};

// 저장
const fnSave = async () => {
  // 필수값 검증(UI 레벨, 서버도 재검증)
  if (!formData.archiveTypeCd) return proxy.$alert("자료타입을 선택해 주세요.");
  if (!formData.title) return proxy.$alert("제목을 입력해 주세요.");
  // 생성 시엔 master 포함 비번/확인 필수(공지와 동일). 면제는 등록 이후 수정/삭제/접근에만.
  if (!formData.editPwd) return proxy.$alert("비밀번호를 입력해 주세요.");
  if (!formData.editPwdConfirm)
    return proxy.$alert("비밀번호 확인을 입력해 주세요.");
  if (formData.editPwd !== formData.editPwdConfirm)
    return proxy.$alert("비밀번호가 일치하지 않습니다.");

  // 저장/업로드 진행 중이면 중복 제출 차단
  if (saving.value) return;
  saving.value = true;

  try {
    // 첨부 업로드: 선택한 파일을 먼저 multipart 로 업로드해 FILE_MGMT_CD 를 발급받는다.
    // 선택 순서를 정렬순서(sortIdx)로 사용. 확장자 화이트리스트는 서버가 최종 검증.
    const filePayload = [];
    for (let i = 0; i < fileList.value.length; i++) {
      const uploaded = await fnUploadFile(fileList.value[i]);
      if (uploaded?.fileMgmtCd) {
        filePayload.push({ fileMgmtCd: uploaded.fileMgmtCd, sortIdx: i + 1 });
      }
    }

    // NOTICE_TYPE='ARCHIVE'/POPUP_YN='N'/PIN_YN='N'/TARGET_SCOPE='ALL' 등은 서버가 강제.
    const requestBody = {
      archiveTypeCd: formData.archiveTypeCd,
      title: formData.title,
      content: formData.content,
      editPwd: formData.editPwd,
      fileList: filePayload, // 업로드로 발급된 FILE_MGMT_CD 매핑
    };

    const response = await axios.post(
      "/webApi/notice02/save-archive",
      requestBody,
      { headers: { "Content-Type": "application/json" } }
    );
    if (response.status === 200) {
      await proxy.$alert("저장되었습니다.");
      props.onSave?.();
      emit("close");
    }
  } catch (err) {
    // 업로드 실패(확장자 차단 등) / 서버 필수검증 실패 등 메시지 노출
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.archive-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm, 0.5rem);
  padding: var(--space-md, 1rem);
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.form-row {
  display: grid;
  grid-template-columns: 96px 1fr;
  align-items: start;
  gap: var(--space-sm, 0.5rem);
}

.form-row > label {
  padding-top: var(--space-xs, 0.25rem);
  color: var(--color-text, #374151);
  font-size: var(--font-size-sm, 0.875rem);
  /* '비밀번호 확인 *' 처럼 라벨 폭(96px)을 넘는 경우 별표(*)가 개행되지 않도록 */
  white-space: nowrap;
}

.req {
  color: var(--color-danger, #ef4444);
}

.form-row input[type="text"],
.form-row input[type="password"],
.form-row textarea,
.archive-form
  input:not([type="checkbox"]):not([type="radio"]):not([type="file"]),
.archive-form textarea {
  width: 100%;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: var(--space-xs, 0.375rem) var(--space-sm, 0.5rem);
  font-size: var(--font-size-sm, 0.875rem);
}

/* 자료타입 드롭다운 — 전역 select 위에서 입력칸과 동일 폭/형태로 */
.type-select {
  width: 100%;
  max-width: 320px;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: var(--space-xs, 0.375rem) var(--space-sm, 0.5rem);
  font-size: var(--font-size-sm, 0.875rem);
  background: var(--color-surface, #ffffff);
}

.form-divider {
  border: none;
  border-top: 1px solid var(--color-border, #e5e7eb);
  margin: var(--space-sm, 0.5rem) 0;
}

.hint {
  margin-top: var(--space-xxs, 0.25rem);
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted, #4b5563);
}

.file-area {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-xs, 0.375rem);
}

/* 비밀번호 확인 입력 + 일치 여부 메시지 */
.pwd-confirm-area {
  display: flex;
  flex-direction: column;
  gap: var(--space-xxs, 0.25rem);
}

.pwd-confirm-area input[type="password"] {
  flex: 0 0 auto;
  width: 100%;
}

.pwd-confirm-msg {
  margin: 0;
  font-size: var(--font-size-xs, 0.75rem);
}

.pwd-confirm-msg.is-ok {
  color: var(--color-primary, #16a34a);
}

.pwd-confirm-msg.is-error {
  color: var(--color-danger, #ef4444);
}

/* native file input 숨김 → '파일 선택' 버튼으로만 트리거 */
.file-input-hidden {
  display: none;
}

.file-area .file-select-btn {
  align-self: flex-start;
  flex: 0 0 auto;
  width: fit-content;
  min-width: 0;
  height: var(--btn-height-sm, 26px);
  padding: 0 var(--space-md, 0.75rem);
  margin-left: 0;
  font-size: var(--btn-font, 11px);
}

.file-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs, 0.375rem);
}

.file-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 0.375rem);
  padding: var(--space-xxs, 0.25rem) var(--space-sm, 0.625rem);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-pill, 999px);
  background: var(--color-surface-muted, #f3f4f6);
  font-size: var(--font-size-xs, 0.75rem);
  white-space: nowrap;
}

.file-item__del {
  flex: 0 0 auto;
  margin-left: 0;
  border: none;
  background: transparent;
  color: var(--color-text-muted, #4b5563);
  cursor: pointer;
  font-size: var(--font-size-sm, 0.875rem);
  line-height: 1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm, 0.5rem);
  padding: var(--space-sm, 0.5rem) var(--space-md, 1rem);
  border-top: 1px solid var(--color-border, #e5e7eb);
}

/* 취소/저장 버튼 공통 규격 — 동일 크기로 우측 하단 정렬 */
.btn-primary,
.btn-secondary {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  height: var(--btn-height, 29px);
  padding: 0 var(--btn-padding, 11px);
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font, 11px);
  line-height: 1;
  cursor: pointer;
}

.btn-primary {
  border: none;
  background: var(--color-primary, #16a34a);
  color: #ffffff;
}

.btn-primary:hover {
  background: var(--color-primary-hover, #15803d);
}

.btn-secondary {
  border: 1px solid var(--color-border-strong, #d1d5db);
  background: var(--color-surface, #ffffff);
  color: var(--color-text, #374151);
}

/* F-10 규약: 좌우 버튼 폭 균등 */
.modal-footer .btn-primary,
.modal-footer .btn-secondary {
  flex: 1;
}
</style>
