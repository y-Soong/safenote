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
          <span>{{ isEditMode ? "자료 수정" : "자료 상세" }}</span>
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
          <!-- 등록정보 (조회) -->
          <div class="form-row">
            <label>등록정보</label>
            <div class="meta-info">
              <span class="meta-info__item">
                <span class="meta-info__k">등록자</span>
                <input v-model="formData.insertUserNm" readonly />
              </span>
              <span class="meta-info__item">
                <span class="meta-info__k">등록일시</span>
                <input v-model="formData.insertDate" readonly />
              </span>
            </div>
          </div>

          <!-- 자료타입 -->
          <div class="form-row">
            <label>자료타입</label>
            <!-- 조회모드: 명칭 표시 / 수정모드: 드롭다운 -->
            <input v-if="!isEditMode" v-model="formData.archiveTypeNm" readonly />
            <select
              v-else
              v-model="formData.archiveTypeCd"
              name="combo"
              class="type-select"
            >
              <option value="">자료타입을 선택해 주세요</option>
              <!-- TODO(developer): archive-types 결과(archiveTypeList) 바인딩.
                   기존 저장된 타입이 USE_YN='N' 이어도 표시되도록 developer 가 보강(목록에 없으면 현재값 추가). -->
              <option
                v-for="t in archiveTypeList"
                :key="t.archiveTypeCd"
                :value="t.archiveTypeCd"
              >
                {{ t.archiveTypeNm }}
              </option>
            </select>
          </div>

          <!-- 기본 정보 -->
          <div class="form-row">
            <label>제목</label>
            <input
              v-model="formData.title"
              maxlength="200"
              :readonly="!isEditMode"
            />
          </div>
          <div class="form-row">
            <label>내용</label>
            <textarea
              v-model="formData.content"
              rows="6"
              :readonly="!isEditMode"
            ></textarea>
          </div>

          <!-- 첨부: 조회모드 = 다운로드 / 수정모드 = 편집(기존 삭제 + 신규 추가) -->
          <div class="form-row">
            <label>첨부파일</label>

            <!-- 조회모드: 기존 다운로드 리스트 -->
            <ul class="file-list" v-if="!isEditMode">
              <li v-for="(f, i) in formData.fileList" :key="i" class="file-item">
                <div
                  class="file-item__link"
                  role="button"
                  tabindex="0"
                  @click="fnDownloadFile(f)"
                  @keydown.enter.space.prevent="fnDownloadFile(f)"
                >
                  <span class="file-item__name">{{ f.fileNm }}</span>
                  <span class="file-item__dl" aria-hidden="true">⬇</span>
                </div>
              </li>
              <li
                v-if="!formData.fileList || formData.fileList.length === 0"
                class="file-empty"
              >
                첨부 없음
              </li>
            </ul>

            <!-- 수정모드: 편집 영역 (기존첨부 삭제칩 + 신규파일 추가) -->
            <div class="file-area" v-else>
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
                파일 추가
              </button>

              <ul
                class="file-list file-list--edit"
                v-if="formData.fileList.length > 0 || newFiles.length > 0"
              >
                <li
                  v-for="(f, i) in formData.fileList"
                  :key="'old-' + i"
                  class="file-item file-item--chip"
                >
                  <span class="file-item__name">{{ f.fileNm }}</span>
                  <button class="file-item__del" @click="fnRemoveExistingFile(i)">×</button>
                </li>
                <li
                  v-for="(nf, i) in newFiles"
                  :key="'new-' + i"
                  class="file-item file-item--chip file-item--new"
                >
                  <span class="file-item__name">{{ nf.name }}</span>
                  <button class="file-item__del" @click="fnRemoveNewFile(i)">×</button>
                </li>
              </ul>
              <p class="hint">
                텍스트/이미지/동영상/음성 파일만 첨부할 수 있습니다(실행/스크립트 형식 제외). 저장 시 함께 업로드됩니다.
              </p>
            </div>
          </div>

          <!-- 비밀번호 검증 (수정/삭제 진입 게이트) — master 면 disabled -->
          <div class="form-row" v-if="!isEditMode">
            <label>비밀번호</label>
            <div class="pwd-area">
              <input
                v-model="editPwdInput"
                type="password"
                maxlength="50"
                :disabled="isMaster"
                placeholder="비밀번호"
              />
              <button class="btn-secondary" @click="fnEnterEditMode">
                {{ isMaster ? "수정 모드" : "확인" }}
              </button>
              <button class="btn-danger" @click="fnDelete">삭제</button>
            </div>
          </div>
        </div>

        <!-- 액션 (수정모드: 저장만. 헤더 X 로 닫음) -->
        <div class="modal-footer" v-if="isEditMode">
          <button class="btn-primary" @click="fnUpdate">저장</button>
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
  onMounted,
  defineProps,
  defineEmits,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { resolveBaseURL } from "@/api/baseUrl";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const props = defineProps({
  archiveData: {
    type: Object,
    default: () => ({ noticeId: "" }),
  },
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

// 폼/모드 상태 (대상지정/팝업/고정 없음 — 자료실은 회사 전체 공통)
const formData = reactive({
  noticeId: "",
  archiveTypeCd: "",
  archiveTypeNm: "",
  title: "",
  content: "",
  fileList: [], // [{ fileMgmtCd, fileNm, fileExt, filePath, sortIdx }]
  insertUserNm: "",
  insertDate: "",
});
const editPwdInput = ref("");
const isEditMode = ref(false);

// 수정모드 첨부 편집 상태
const fileInputRef = ref(null);
const newFiles = ref([]); // 신규로 추가한(아직 미업로드) File 객체 목록
const saving = ref(false); // 저장/업로드 진행 중 플래그(중복 제출 방지)

// 자료타입 드롭다운 목록 (TODO(developer): archive-types API 로 채움)
const archiveTypeList = ref([]);

// master 여부: 서버가 최종 판단(클라 신뢰 금지). 여기서는 비번 컴포넌트 disable 등 UI 보조에만 사용.
const isMaster = ref(sessionStorage.getItem("gv_authCd") === "master");

onMounted(async () => {
  formData.noticeId = props.archiveData?.noticeId || "";
  // 상세 먼저 로드(현재 자료타입 파악) → 드롭다운 로드(현재값 보강은 상세 이후 수행)
  await fnLoadDetail();
  await fnLoadArchiveTypes();
});

// 자료타입 드롭다운 조회 (코드그룹 BAIM_VAL_CD 미주입 시 빈 목록)
//   R3: 현재 자료의 archiveTypeCd 가 USE_YN='N' 이라 목록에 없을 수 있음 → 현재값을 옵션에 보강.
const fnLoadArchiveTypes = async () => {
  try {
    const response = await axios.get("/webApi/notice02/archive-types");
    if (response.status === 200) {
      const list = response.data?.typeList || [];
      // 현재 자료타입이 목록(USE_YN='Y')에 없으면 현재값(명칭 포함)을 맨 앞에 보강.
      if (
        formData.archiveTypeCd &&
        !list.some((t) => t.archiveTypeCd === formData.archiveTypeCd)
      ) {
        list.unshift({
          archiveTypeCd: formData.archiveTypeCd,
          archiveTypeNm: formData.archiveTypeNm || formData.archiveTypeCd,
          sortIdx: -1,
        });
      }
      archiveTypeList.value = list;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "자료타입 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  }
};

// 상세 조회 → formData 매핑
//   GET /webApi/notice02/archive-info?noticeId=
//   info: archiveTypeCd/archiveTypeNm/title/content/insertUserNm/insertDate, fileList
const fnLoadDetail = async () => {
  if (!formData.noticeId) return;
  try {
    const response = await axios.get("/webApi/notice02/archive-info", {
      params: { noticeId: formData.noticeId },
    });
    if (response.status === 200) {
      const info = response.data?.archiveInfo || {};
      formData.archiveTypeCd = info.archiveTypeCd || "";
      formData.archiveTypeNm = info.archiveTypeNm || "";
      formData.title = info.title || "";
      formData.content = info.content || "";
      formData.insertUserNm = info.insertUserNm || "";
      formData.insertDate = info.insertDate || "";
      formData.fileList = response.data?.fileList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 첨부 편집 — 숨긴 native input 을 버튼으로 트리거
const fnTriggerFileSelect = () => {
  fileInputRef.value?.click();
};
const fnOnFileChange = (e) => {
  const files = Array.from(e.target.files || []);
  newFiles.value = [...newFiles.value, ...files];
  // 동일 파일 재선택 시에도 change 가 발생하도록 input 값 초기화
  if (e.target) e.target.value = "";
};
// 신규 추가 파일(미업로드) 제거
const fnRemoveNewFile = (i) => {
  newFiles.value.splice(i, 1);
};
// 기존 첨부(서버 보유) 제거 — 저장 시 fileList 페이로드에서 빠지면 서버가 재구성(삭제 반영)
const fnRemoveExistingFile = (i) => {
  formData.fileList.splice(i, 1);
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

// 수정 모드 진입 (비번 검증, master 면제 — 최종 판정은 서버)
const fnEnterEditMode = async () => {
  if (isMaster.value) {
    isEditMode.value = true;
    return;
  }
  if (!editPwdInput.value) return proxy.$alert("비밀번호를 입력해 주세요.");
  try {
    const response = await axios.post(
      "/webApi/notice02/verify-pwd",
      { noticeId: formData.noticeId, editPwd: editPwdInput.value },
      { headers: { "Content-Type": "application/json" } }
    );
    if (response.status === 200 && response.data?.verified === true) {
      isEditMode.value = true;
    } else {
      await proxy.$alert("비밀번호가 일치하지 않습니다.");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "비밀번호가 일치하지 않습니다.");
    await proxy.$alert(msg);
  }
};

// 첨부 다운로드 (단기 토큰 발급 → file-download?token= 로 브라우저 다운로드)
const fnDownloadFile = async (file) => {
  if (!file?.fileMgmtCd) return;
  try {
    const response = await axios.get("/webApi/notice02/file-download-token", {
      params: { noticeId: formData.noticeId, fileMgmtCd: file.fileMgmtCd },
    });
    if (response.status === 200 && response.data?.token) {
      const base = String(resolveBaseURL() || "").replace(/\/$/, "");
      const url =
        `${base}/webApi/notice02/file-download?token=` +
        encodeURIComponent(response.data.token);
      // 토큰 쿼리 방식(서버 @NoAuth) → 브라우저 직접 다운로드
      window.open(url, "_blank");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "다운로드 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 수정 저장
const fnUpdate = async () => {
  if (!formData.archiveTypeCd) return proxy.$alert("자료타입을 선택해 주세요.");
  if (!formData.title) return proxy.$alert("제목을 입력해 주세요.");

  // 저장/업로드 진행 중이면 중복 제출 차단
  if (saving.value) return;
  saving.value = true;

  try {
    // 첨부 재구성 = 유지된 기존 첨부 + 신규 업로드분.
    //   - 신규 newFiles 를 먼저 업로드해 FILE_MGMT_CD 발급(확장자 화이트리스트는 서버 최종 검증).
    //   - 서버 update-archive 는 fileList 로 TB_NOTICE_FILE 전체 재구성(delete+insert)하므로,
    //     유지/추가만 포함하면 삭제(빠진 항목)도 자동 반영된다.
    const keptFiles = (formData.fileList || []).map((f, idx) => ({
      fileMgmtCd: f.fileMgmtCd,
      sortIdx: idx + 1,
    }));

    const base = keptFiles.length;
    const newPayload = [];
    for (let i = 0; i < newFiles.value.length; i++) {
      const uploaded = await fnUploadFile(newFiles.value[i]);
      if (uploaded?.fileMgmtCd) {
        newPayload.push({
          fileMgmtCd: uploaded.fileMgmtCd,
          sortIdx: base + i + 1,
        });
      }
    }

    const requestBody = {
      noticeId: formData.noticeId,
      // master 가 아니면 수정 모드 진입 시 통과한 비번 동반(master 면 미전송 → 서버가 면제)
      editPwd: isMaster.value ? "" : editPwdInput.value,
      archiveTypeCd: formData.archiveTypeCd,
      title: formData.title,
      content: formData.content,
      // 유지된 기존 첨부 + 신규 업로드분
      fileList: [...keptFiles, ...newPayload],
    };

    const response = await axios.post(
      "/webApi/notice02/update-archive",
      requestBody,
      { headers: { "Content-Type": "application/json" } }
    );
    if (response.status === 200) {
      await proxy.$alert("수정되었습니다.");
      props.onSave?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "수정 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};

// 삭제 (논리삭제, master 아니면 비번 검증 — 최종 판정은 서버)
const fnDelete = async () => {
  if (!isMaster.value && !editPwdInput.value) {
    return proxy.$alert("비밀번호를 입력해 주세요.");
  }
  const ok = await proxy.$confirm("이 자료를 삭제하시겠습니까?");
  if (!ok) return;

  // 삭제/저장 진행 중이면 중복 제출 차단
  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post(
      "/webApi/notice02/delete-archive",
      {
        noticeId: formData.noticeId,
        // master 면 미전송(서버 면제). 그 외는 입력 비번 동반.
        editPwd: isMaster.value ? "" : editPwdInput.value,
      },
      { headers: { "Content-Type": "application/json" } }
    );
    if (response.status === 200) {
      await proxy.$alert("삭제되었습니다.");
      props.onSave?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "삭제 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
/* 전역 .modal-content-wide(max-width:1400px) override — 이 팝업은 약 70% 폭으로 축소 */
.modal-content-wide {
  max-width: 980px;
}

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
}

.archive-form input:not([type="checkbox"]):not([type="radio"]),
.archive-form textarea {
  width: 100%;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: var(--space-xs, 0.375rem) var(--space-sm, 0.5rem);
  font-size: var(--font-size-sm, 0.875rem);
}

.archive-form input[readonly],
.archive-form textarea[readonly] {
  background: var(--color-bg, #f9fafb);
  color: var(--color-text-muted, #4b5563);
}

/* 자료타입 드롭다운 */
.type-select {
  width: 100%;
  max-width: 320px;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: var(--space-xs, 0.375rem) var(--space-sm, 0.5rem);
  font-size: var(--font-size-sm, 0.875rem);
  background: var(--color-surface, #ffffff);
}

.pwd-area {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
}

/* 비밀번호 입력칸: 고정 폭(전역 width:100% override) */
.archive-form .pwd-area input:not([type="checkbox"]):not([type="radio"]) {
  flex: 0 0 auto;
  width: 7rem;
  max-width: 100%;
}

.pwd-area .btn-secondary,
.pwd-area .btn-danger {
  flex: 0 0 auto;
  width: auto;
  white-space: nowrap;
}

/* 등록정보(등록자/등록일시) — 2등분 균등 영역, 한 줄 표시 */
.meta-info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-sm, 0.5rem);
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
}

.meta-info__item {
  display: flex;
  align-items: center;
  gap: var(--space-xxs, 0.25rem);
  min-width: 0;
}

.meta-info__k {
  flex: 0 0 auto;
  color: var(--color-text-muted, #4b5563);
  font-weight: 600;
  white-space: nowrap;
}

.meta-info__item input {
  flex: 1 1 auto;
  min-width: 0;
}

.hint {
  margin-top: var(--space-xxs, 0.25rem);
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted, #4b5563);
}

.file-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xxs, 0.25rem);
}

.file-item {
  font-size: var(--font-size-sm, 0.875rem);
}

/* 파일명+아이콘 묶음: 영역 전체 클릭 시 다운로드 (내용 폭만큼만 차지) */
.file-item__link {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  gap: var(--space-xs, 0.375rem);
  padding: var(--space-xxs, 0.25rem) var(--space-sm, 0.5rem);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  background: var(--color-surface, #ffffff);
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.file-item__link:hover {
  border-color: var(--color-primary, #16a34a);
  background: var(--color-primary-soft, #ecfdf3);
}

.file-item__link .file-item__name,
.file-item__link .file-item__dl {
  flex: 0 0 auto;
}

.file-item__name {
  color: var(--color-text, #374151);
}

.file-item__dl {
  color: var(--color-primary, #16a34a);
  line-height: 1;
}

.file-empty {
  color: var(--color-text-muted, #4b5563);
  font-size: var(--font-size-sm, 0.875rem);
}

/* 수정모드 첨부 편집 영역 */
.file-area {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-xs, 0.375rem);
}

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

/* 수정모드 첨부 칩 목록(가로 wrap) */
.file-list--edit {
  flex-direction: row;
  flex-wrap: wrap;
  gap: var(--space-xs, 0.375rem);
}

.file-item--chip {
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

.file-item--new {
  border-style: dashed;
}

.file-item--chip .file-item__name {
  flex: 0 0 auto;
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

.form-divider {
  border: none;
  border-top: 1px solid var(--color-border, #e5e7eb);
  margin: var(--space-sm, 0.5rem) 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm, 0.5rem);
  padding: var(--space-sm, 0.5rem) var(--space-md, 1rem);
  border-top: 1px solid var(--color-border, #e5e7eb);
}

/* 버튼 공통 규격 */
.btn-primary,
.btn-secondary,
.btn-danger {
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

.btn-danger {
  border: 1px solid var(--color-danger, #ef4444);
  background: var(--color-surface, #ffffff);
  color: var(--color-danger, #ef4444);
}

.btn-danger:hover {
  background: var(--color-danger-soft, #fef2f2);
}

.modal-footer .btn-primary {
  width: 80px;
  min-width: 80px;
}
</style>
