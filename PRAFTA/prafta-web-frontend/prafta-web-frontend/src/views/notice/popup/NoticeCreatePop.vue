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
          <span>신규 공지</span>
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

        <div class="notice-form">
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
            <label>내용 <span class="req">*</span></label>
            <textarea
              v-model="formData.content"
              rows="6"
              placeholder="내용을 입력해 주세요"
            ></textarea>
          </div>
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

          <!-- 대상 지정 -->
          <div class="form-section-title">대상 지정</div>
          <div class="form-row">
            <label>스코프</label>
            <div class="radio-group">
              <label class="radio-item" v-if="canSelectAll">
                <input
                  type="radio"
                  value="ALL"
                  v-model="formData.targetScope"
                />
                전사
              </label>
              <label class="radio-item">
                <input
                  type="radio"
                  value="SITE"
                  v-model="formData.targetScope"
                />
                사업장
              </label>
              <label class="radio-item">
                <input
                  type="radio"
                  value="NODE"
                  v-model="formData.targetScope"
                />
                사업장+노드
              </label>
            </div>
          </div>

          <div class="form-row" v-if="formData.targetScope !== 'ALL'">
            <label>대상</label>
            <div class="target-area">
              <!-- 발행자 스코프(서버 재검증) 내에서만 대상 추가. 사업장 조회 → (NODE 면) 노드 조회 후 추가 -->
              <div class="target-picker">
                <!-- 사업장: 조회 팝업으로만 선택(직접입력 불가) -->
                <div class="search-field">
                  <input
                    type="text"
                    class="search-input"
                    :value="pickSiteNm"
                    placeholder="사업장 조회"
                    readonly
                    @click="fnOpenSiteSearch"
                  />
                  <button
                    class="search-btn"
                    title="사업장 조회"
                    @click="fnOpenSiteSearch"
                  >
                    <img
                      class="search_icon"
                      :src="search_icon"
                      alt="사업장 조회"
                    />
                  </button>
                </div>
                <!-- 노드: NODE 스코프일 때만. 사업장 선택 후 활성화 -->
                <div
                  class="search-field"
                  v-if="formData.targetScope === 'NODE'"
                >
                  <input
                    type="text"
                    class="search-input"
                    :value="pickNodeNm"
                    placeholder="노드 조회"
                    readonly
                    :disabled="!pickSiteCd"
                    @click="fnOpenNodeSearch"
                  />
                  <button
                    class="search-btn"
                    title="노드 조회"
                    :disabled="!pickSiteCd"
                    @click="fnOpenNodeSearch"
                  >
                    <img
                      class="search_icon"
                      :src="search_icon"
                      alt="노드 조회"
                    />
                  </button>
                </div>
                <button
                  class="btn-secondary add-target-btn"
                  @click="fnAddTarget"
                >
                  + 대상 추가
                </button>
              </div>
              <!-- 선택된 대상 칩 목록 -->
              <ul class="target-chip-list">
                <li
                  v-for="(t, i) in formData.targetList"
                  :key="i"
                  class="target-chip"
                >
                  <span class="target-chip__label">
                    {{ t.siteNm
                    }}<template v-if="t.nodeNm"> / {{ t.nodeNm }}</template>
                  </span>
                  <label
                    v-if="formData.targetScope === 'NODE' && t.nodeCd"
                    class="target-chip__desc"
                  >
                    <input
                      type="checkbox"
                      v-model="t.includeDescendantsYn"
                      true-value="Y"
                      false-value="N"
                    />
                    하위 포함
                  </label>
                  <button
                    class="target-chip__del"
                    title="삭제"
                    @click="fnRemoveTarget(i)"
                  >
                    ×
                  </button>
                </li>
              </ul>
            </div>
          </div>

          <div class="form-row">
            <label>일용직</label>
            <label class="checkbox-item">
              <input
                type="checkbox"
                v-model="formData.includeDailyYn"
                true-value="Y"
                false-value="N"
              />
              일용직 포함 (사업장 단위로만 노출)
            </label>
          </div>

          <hr class="form-divider" />

          <!-- 노출 설정 -->
          <div class="form-row">
            <label>로그인 팝업</label>
            <label class="checkbox-item">
              <input
                type="checkbox"
                v-model="formData.popupYn"
                true-value="Y"
                false-value="N"
              />
              로그인 시 팝업으로 노출
            </label>
          </div>
          <div class="form-row" v-if="formData.popupYn === 'Y'">
            <label>팝업 기간 <span class="req">*</span></label>
            <div class="period-area">
              <CalendarSrch v-model="formData.popupFromYmd" />
              <span class="date-range-sep">~</span>
              <CalendarSrch v-model="formData.popupToYmd" />
            </div>
          </div>

          <div class="form-row">
            <label>상단 고정</label>
            <label class="checkbox-item">
              <input
                type="checkbox"
                v-model="formData.pinYn"
                true-value="Y"
                false-value="N"
              />
              목록 상단에 고정
            </label>
          </div>
          <div class="form-row" v-if="formData.pinYn === 'Y'">
            <label>고정 순번</label>
            <div>
              <input
                v-model.number="formData.pinOrder"
                type="number"
                min="1"
                class="pin-order-input"
              />
              <p class="hint">
                순번은 저장 시 서버에서 1..N 연속으로 보정됩니다.
              </p>
            </div>
          </div>

          <hr class="form-divider" />

          <!-- 첨부 -->
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
import { ymdDashToCompact } from "@/utils/noticeDate";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { useModal } from "@/utils/useModal";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import search_icon from "@/assets/img/search_icon.png";

const props = defineProps({
  onSave: Function,
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();
// 사업장/노드 조회 팝업용 모달 인스턴스(자체 컨테이너 → 본 팝업을 닫지 않고 위에 띄움)
const { open: openPop } = useModal();

// 모달 드래그 (공통 훅으로 화면 중앙 배치 + 드래그 가능)
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// 폼 상태
const formData = reactive({
  title: "",
  content: "",
  editPwd: "",
  editPwdConfirm: "",
  targetScope: "ALL",
  targetList: [], // [{ siteCd, siteNm, nodeCd, nodeNm, includeDescendantsYn }]
  includeDailyYn: "N",
  popupYn: "N",
  popupFromYmd: "", // CalendarSrch → YYYY-MM-DD (저장 시 YYYYMMDD 변환)
  popupToYmd: "",
  pinYn: "N",
  pinOrder: 1,
});
const fileList = ref([]);
const fileInputRef = ref(null);
// 저장/업로드 진행 중 플래그(중복 제출 방지)
const saving = ref(false);

// 비밀번호/비밀번호 확인 일치 여부 (오타 방지 즉시 피드백)
const pwdMatched = computed(
  () => !!formData.editPwd && formData.editPwd === formData.editPwdConfirm
);

// ── 발행자 대상 트리(scope-tree) ──────────────────────────────
// canSelectAll=false 면 '전사' 옵션 자체를 숨긴다(전사 권한자만 ALL 선택 가능).
const canSelectAll = ref(true);
const scopeList = ref([]); // [{ level, siteCd, siteNm, nodeCd, nodeNm, parentNodeCd }]
// 조회 팝업에서 선택된 사업장/노드(코드+명칭). 명칭은 disabled input 으로만 노출.
const pickSiteCd = ref("");
const pickSiteNm = ref("");
const pickNodeCd = ref("");
const pickNodeNm = ref("");

onMounted(async () => {
  await fnLoadScopeTree();
});

// 발행자 대상선택 트리 조회 (식별자/스코프는 서버 JWT 도출)
const fnLoadScopeTree = async () => {
  try {
    const response = await axios.get("/webApi/notice01/scope-tree");
    if (response.status === 200) {
      canSelectAll.value = response.data?.canSelectAll === true;
      scopeList.value = response.data?.scopeList || [];
      // 전사 선택 불가인데 기본값이 ALL 이면 SITE 로 전환
      if (!canSelectAll.value && formData.targetScope === "ALL") {
        formData.targetScope = "SITE";
      }
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "대상 트리 조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 사업장 조회 팝업 열기 → 선택 시 코드/명칭 세팅(사업장 변경 시 노드 초기화)
const fnOpenSiteSearch = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: (siteCdVal, _siteNoVal, siteNmVal) => {
      pickSiteCd.value = siteCdVal ?? "";
      pickSiteNm.value = siteNmVal ?? "";
      pickNodeCd.value = "";
      pickNodeNm.value = "";
    },
  });
};

// 노드 조회 팝업 열기 → 선택한 사업장 기준 노드만 조회
const fnOpenNodeSearch = () => {
  if (!pickSiteCd.value) {
    return proxy.$alert("사업장을 먼저 조회하여 선택해 주세요.");
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: pickSiteCd.value,
    nodeCd_p: "",
    userId_p: "",
    onSelect: (nodeCdVal, nodeNmVal) => {
      pickNodeCd.value = nodeCdVal ?? "";
      pickNodeNm.value = nodeNmVal ?? "";
    },
  });
};

// 선택한 사업장(+노드)을 대상 목록에 추가(중복 제거)
const fnAddTarget = () => {
  if (!pickSiteCd.value) {
    return proxy.$alert("사업장을 조회하여 선택해 주세요.");
  }

  let nodeCd = null;
  let nodeNm = null;
  if (formData.targetScope === "NODE") {
    if (!pickNodeCd.value) {
      return proxy.$alert("노드를 조회하여 선택해 주세요.");
    }
    nodeCd = pickNodeCd.value;
    nodeNm = pickNodeNm.value || pickNodeCd.value;
  }

  // 중복 검사(사업장+노드 동일)
  const dup = formData.targetList.some(
    (t) => t.siteCd === pickSiteCd.value && (t.nodeCd || null) === (nodeCd || null)
  );
  if (dup) {
    return proxy.$alert("이미 추가된 대상입니다.");
  }

  formData.targetList.push({
    siteCd: pickSiteCd.value,
    siteNm: pickSiteNm.value || pickSiteCd.value,
    nodeCd,
    nodeNm,
    includeDescendantsYn: "Y", // NODE 대상 기본 하위 포함
  });

  // 노드만 초기화(사업장은 연속 추가 편의를 위해 유지)
  pickNodeCd.value = "";
  pickNodeNm.value = "";
};

const fnRemoveTarget = (i) => {
  formData.targetList.splice(i, 1);
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
  const response = await axios.post("/webApi/notice01/upload-file", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return response.data; // { fileMgmtCd, fileNm }
};

// 저장
const fnSave = async () => {
  // 필수값 검증(UI 레벨, 서버도 재검증)
  if (!formData.title) return proxy.$alert("제목을 입력해 주세요.");
  if (!formData.content) return proxy.$alert("내용을 입력해 주세요.");
  if (!formData.editPwd) return proxy.$alert("비밀번호를 입력해 주세요.");
  if (!formData.editPwdConfirm)
    return proxy.$alert("비밀번호 확인을 입력해 주세요.");
  if (formData.editPwd !== formData.editPwdConfirm)
    return proxy.$alert("비밀번호가 일치하지 않습니다.");
  if (formData.popupYn === "Y" && (!formData.popupFromYmd || !formData.popupToYmd)) {
    return proxy.$alert("팝업 기간을 입력해 주세요.");
  }
  if (
    formData.targetScope !== "ALL" &&
    (!formData.targetList || formData.targetList.length === 0)
  ) {
    return proxy.$alert("대상을 1건 이상 추가해 주세요.");
  }

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

    // 대상 페이로드: ALL 이면 빈 배열, 그 외 선택 대상.
    const targetList =
      formData.targetScope === "ALL"
        ? []
        : formData.targetList.map((t) => ({
            siteCd: t.siteCd,
            nodeCd: formData.targetScope === "NODE" ? t.nodeCd : null,
            includeDescendantsYn:
              formData.targetScope === "NODE" ? t.includeDescendantsYn : "N",
          }));

    const requestBody = {
      title: formData.title,
      content: formData.content,
      editPwd: formData.editPwd,
      targetScope: formData.targetScope,
      includeDailyYn: formData.targetScope === "ALL" ? "N" : formData.includeDailyYn,
      popupYn: formData.popupYn,
      // 백엔드는 YYYYMMDD(VARCHAR8) 저장 — CalendarSrch 의 YYYY-MM-DD 를 변환.
      popupFromYmd:
        formData.popupYn === "Y" ? ymdDashToCompact(formData.popupFromYmd) : "",
      popupToYmd:
        formData.popupYn === "Y" ? ymdDashToCompact(formData.popupToYmd) : "",
      pinYn: formData.pinYn,
      pinOrder: formData.pinYn === "Y" ? formData.pinOrder : null,
      targetList,
      fileList: filePayload, // 업로드로 발급된 FILE_MGMT_CD 매핑
    };

    const response = await axios.post(
      "/webApi/notice01/save-notice",
      requestBody,
      { headers: { "Content-Type": "application/json" } }
    );
    if (response.status === 200) {
      await proxy.$alert("저장되었습니다.");
      props.onSave?.();
      emit("close");
    }
  } catch (err) {
    // 업로드 실패(확장자 차단 등) / 서버 발행자 스코프 재검증 실패(범위 초과) 등 메시지 노출
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.notice-form {
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

.form-row input[type="text"]:not(.search-input),
.form-row input[type="password"],
.form-row input[type="number"],
.form-row textarea,
.notice-form
  input:not([type="checkbox"]):not([type="radio"]):not([type="file"]):not(.search-input),
.notice-form textarea {
  width: 100%;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: var(--space-xs, 0.375rem) var(--space-sm, 0.5rem);
  font-size: var(--font-size-sm, 0.875rem);
}

.form-divider {
  border: none;
  border-top: 1px solid var(--color-border, #e5e7eb);
  margin: var(--space-sm, 0.5rem) 0;
}

.form-section-title {
  font-weight: 700;
  color: var(--color-text-strong, #111827);
  font-size: var(--font-size-sm, 0.875rem);
}

.period-area {
  display: flex;
  align-items: center;
  gap: var(--space-md, 1rem);
}

/* 스코프 선택: 세그먼트(pill) 형태, 좁은 폭에서는 통째로 줄바꿈 */
.radio-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-xs, 0.5rem);
}

.radio-item,
.checkbox-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 0.375rem);
  flex-shrink: 0;
  white-space: nowrap;
  /* 전역 .form-row label(margin-left:10px) 제거 — 대상 영역(select)과 시작 위치 정렬 */
  margin-left: 0;
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
  cursor: pointer;
}

/* 라디오는 클릭 영역이 넓은 pill 로 표현, 선택 시 강조 */
.radio-item {
  padding: var(--space-xs, 0.375rem) var(--space-md, 0.875rem);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-pill, 999px);
  background: var(--color-surface, #ffffff);
  transition: border-color 0.15s ease, background 0.15s ease,
    color 0.15s ease;
}

.radio-item:hover {
  border-color: var(--color-primary, #16a34a);
}

.radio-item:has(input:checked) {
  border-color: var(--color-primary, #16a34a);
  background: var(--color-primary-soft, #ecfdf3);
  color: var(--color-primary, #16a34a);
  font-weight: 600;
}

/* 전역 .form-row input(flex:0 0 150px) override — 라디오/체크박스는 원 크기에 딱 맞게 */
.radio-item input[type="radio"],
.checkbox-item input[type="checkbox"],
.radio-item input[type="radio"]:focus,
.checkbox-item input[type="checkbox"]:focus {
  flex: 0 0 auto;
  width: auto;
  min-width: 0;
  margin: 0;
  padding: 0;
  background: none;
  border: none;
  box-shadow: none;
  outline: none;
}

.checkbox-item {
  padding-top: var(--space-xs, 0.25rem);
}

.date-range-sep {
  margin: 0 var(--space-xs, 0.25rem);
}

.pin-order-input {
  width: 80px;
}

.hint {
  margin-top: var(--space-xxs, 0.25rem);
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted, #4b5563);
}

.target-area,
.file-area {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs, 0.375rem);
}

.file-area {
  align-items: flex-start;
}

/* 비밀번호 확인 입력 + 일치 여부 메시지 */
.pwd-confirm-area {
  display: flex;
  flex-direction: column;
  gap: var(--space-xxs, 0.25rem);
}

/* 전역 .form-row input(flex:0 0 150px) override — column flex 안에서 150px 가 '높이'로 적용되는 것 방지 */
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

/* native file input 숨김 → '파일 선택' 버튼으로만 트리거 (전역 flex:0 0 150px 가 만든 빈 박스 제거) */
.file-input-hidden {
  display: none;
}

/* '파일 선택' 버튼: 글자 크기에 맞춰 축소 + 전역 .form-row button(margin-left:10px)·
   공통 .btn-secondary(min-width:72px) override 위해 .file-area 결합으로 specificity 상향.
   하단 안내문구(.hint)와 좌측 끝단 정렬 */
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

.target-picker {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
}

/* 사업장/노드 조회: disabled input + 돋보기 버튼 (직접입력 불가) */
.search-field {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xxs, 0.25rem);
}

.search-field .search-input {
  width: 180px;
  height: var(--btn-height-sm, 26px);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: var(--space-xs, 0.375rem) var(--space-sm, 0.5rem);
  font-size: var(--font-size-sm, 0.875rem);
  background: var(--color-surface-muted, #f9fafb);
  color: var(--color-text-strong, #111827);
  cursor: pointer;
}

.search-field .search-input:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

/* 전역 .form-row button(margin-left:10px)·.search-btn(음수마진) 상쇄 */
.target-picker .search-field .search-btn {
  margin: 0;
}

.target-picker .add-target-btn {
  flex: 0 0 auto;
  width: auto;
  margin-left: 0;
  white-space: nowrap;
}

.target-chip-list,
.file-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs, 0.375rem);
}

.target-chip,
.file-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 0.375rem);
  padding: var(--space-xxs, 0.25rem) var(--space-sm, 0.625rem);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-pill, 999px);
  background: var(--color-surface-muted, #f3f4f6);
  font-size: var(--font-size-xs, 0.75rem);
  /* 칩 전체 한 줄 유지(글자 단위 개행 방지) */
  white-space: nowrap;
}

/* 사업장/노드 명칭 — 전역 .form-row span(flex:0 0 50px) 상쇄로 줄바꿈 방지 */
.target-chip .target-chip__label {
  flex: 0 0 auto;
  white-space: nowrap;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
}

/* '하위 포함' 라벨 — 전역 .form-row label(flex/margin) 상쇄 */
.target-chip .target-chip__desc {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xxs, 0.125rem);
  flex: 0 0 auto;
  margin-left: 0;
  white-space: nowrap;
  font-weight: 400;
  color: var(--color-text-muted, #4b5563);
}

/* 칩 내부 체크박스 — 전역 .form-row input(flex:0 0 150px) 상쇄 */
.target-chip .target-chip__desc input[type="checkbox"] {
  flex: 0 0 auto;
  width: auto;
  min-width: 0;
  margin: 0;
  padding: 0;
  background: none;
  border: none;
  box-shadow: none;
}

.target-chip__del,
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

/* 취소/저장 버튼 공통 규격 — 동일 크기로 우측 하단 정렬 (footer 가 flex-end) */
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
