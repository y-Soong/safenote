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
          <span>{{ isEditMode ? "공지 수정" : "공지 상세" }}</span>
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
          <!-- 대상 / 등록정보 (조회) — 한 행에 3개 항목 인라인 표시 -->
          <div class="form-row">
            <label>등록정보</label>
            <div class="meta-info">
              <span class="meta-info__item">
                <span>대상</span>
                <input v-model="formData.targetSummary" readonly />
              </span>
              <span class="meta-info__item">
                <span>등록자</span>
                <input v-model="formData.insertUserNm" readonly />
              </span>
              <span class="meta-info__item">
                <span>등록일시</span>
                <input v-model="formData.insertDate" readonly />
              </span>
            </div>
          </div>

          <!-- 기본 정보 -->
          <!-- [PRAFTA-049 D1] 게시기간 중 잠금(isContentLocked) 제거 → 수정모드면 항상 편집 가능 -->
          <div class="form-row">
            <label>제목</label>
            <input v-model="formData.title" maxlength="200" :readonly="!isEditMode" />
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
                <!-- 파일명+다운로드 아이콘을 한 영역으로 묶어 클릭 시 다운로드 -->
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
              <li v-if="!formData.fileList || formData.fileList.length === 0" class="file-empty">
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

              <!-- 기존 첨부(서버 보유) 삭제 가능 칩 + 신규 추가 파일(미업로드) -->
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

          <!-- 비밀번호 검증 (수정 진입 게이트) -->
          <div class="form-row" v-if="!isEditMode">
            <label>비밀번호</label>
            <div class="pwd-area">
              <input
                v-model="editPwdInput"
                type="password"
                maxlength="50"
                :disabled="isMaster"
                :placeholder="'비밀번호'"
              />
              <button class="btn-secondary" @click="fnEnterEditMode">
                {{ isMaster ? "수정 모드" : "확인" }}
              </button>
            </div>
          </div>

          <!-- 수정 모드: 대상 + 노출 설정 (생성 팝업과 동일 필드) -->
          <template v-if="isEditMode">
            <hr class="form-divider" />
            <div class="form-section-title">대상 지정</div>
            <div class="form-row">
              <label>스코프</label>
              <div class="radio-group">
                <label class="radio-item" v-if="canSelectAll">
                  <input type="radio" value="ALL" v-model="formData.targetScope" />
                  전사
                </label>
                <label class="radio-item">
                  <input type="radio" value="SITE" v-model="formData.targetScope" />
                  사업장
                </label>
                <label class="radio-item">
                  <input type="radio" value="NODE" v-model="formData.targetScope" />
                  사업장+노드
                </label>
              </div>
            </div>

            <div class="form-row" v-if="formData.targetScope !== 'ALL'">
              <label>대상</label>
              <div class="target-area">
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
                    <button class="search-btn" title="사업장 조회" @click="fnOpenSiteSearch">
                      <img class="search_icon" :src="search_icon" alt="사업장 조회" />
                    </button>
                  </div>
                  <!-- 노드: NODE 스코프일 때만. 사업장 선택 후 활성화 -->
                  <div class="search-field" v-if="formData.targetScope === 'NODE'">
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
                      <img class="search_icon" :src="search_icon" alt="노드 조회" />
                    </button>
                  </div>
                  <button class="btn-secondary add-target-btn" @click="fnAddTarget">
                    + 대상 추가
                  </button>
                </div>
                <ul class="target-chip-list">
                  <li
                    v-for="(t, i) in formData.targetList"
                    :key="i"
                    class="target-chip"
                  >
                    <span class="target-chip__label">
                      {{ t.siteNm }}<template v-if="t.nodeNm"> / {{ t.nodeNm }}</template>
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
                    <button class="target-chip__del" @click="fnRemoveTarget(i)">×</button>
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
                  :disabled="formData.targetScope === 'ALL'"
                />
                일용직 포함 (사업장 단위로만 노출)
              </label>
            </div>

            <hr class="form-divider" />

            <div class="form-row">
              <label>로그인 팝업</label>
              <label class="checkbox-item">
                <input type="checkbox" v-model="formData.popupYn" true-value="Y" false-value="N" />
                로그인 시 팝업으로 노출
              </label>
            </div>
            <div class="form-row" v-if="formData.popupYn === 'Y'">
              <label>팝업 기간</label>
              <div class="period-area">
                <CalendarSrch v-model="formData.popupFromYmd" />
                <span class="date-range-sep">~</span>
                <CalendarSrch v-model="formData.popupToYmd" />
              </div>
            </div>
            <!-- 049-04: 상단 고정 + 고정 순번 한 행 배치 -->
            <div class="form-row">
              <label>상단 고정</label>
              <div class="pin-row">
                <label class="checkbox-item">
                  <input type="checkbox" v-model="formData.pinYn" true-value="Y" false-value="N" />
                  목록 상단에 고정
                </label>
                <div class="pin-order-inline" v-if="formData.pinYn === 'Y'">
                  <span class="pin-order-label">고정 순번</span>
                  <input v-model.number="formData.pinOrder" type="number" min="1" class="pin-order-input" />
                </div>
              </div>
            </div>
            <div class="form-row" v-if="formData.pinYn === 'Y'">
              <span></span>
              <p class="hint">순번은 저장 시 서버에서 1..N 연속으로 보정됩니다.</p>
            </div>
          </template>
        </div>

        <!-- 액션 (수정모드: 저장만. 닫기/삭제 제거 — 헤더 X 로 닫음) -->
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
import { ymdCompactToDash, ymdDashToCompact } from "@/utils/noticeDate";
import { resolveBaseURL } from "@/api/baseUrl";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { useModal } from "@/utils/useModal";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import search_icon from "@/assets/img/search_icon.png";

const props = defineProps({
  noticeData: {
    type: Object,
    default: () => ({ noticeId: "" }),
  },
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

// 폼/모드 상태
const formData = reactive({
  noticeId: "",
  title: "",
  content: "",
  fileList: [], // [{ fileMgmtCd, fileNm, fileExt, filePath, sortIdx }]
  targetSummary: "",
  targetScope: "ALL",
  targetList: [], // [{ siteCd, siteNm, nodeCd, nodeNm, includeDescendantsYn }]
  includeDailyYn: "N",
  insertUserNm: "",
  insertDate: "",
  popupYn: "N",
  popupFromYmd: "", // 폼(달력)용 YYYY-MM-DD
  popupToYmd: "",
  pinYn: "N",
  pinOrder: 1,
});
const editPwdInput = ref("");
const isEditMode = ref(false);

// 049-03: 수정모드 첨부 편집 상태
const fileInputRef = ref(null);
const newFiles = ref([]); // 신규로 추가한(아직 미업로드) File 객체 목록
const saving = ref(false); // 저장/업로드 진행 중 플래그(중복 제출 방지)

// master 여부: 서버가 최종 판단(클라 신뢰 금지). 여기서는 비번 컴포넌트 disable 등 UI 보조에만 사용.
const isMaster = ref(sessionStorage.getItem("gv_authCd") === "master");

// [PRAFTA-049 D1] 게시기간 중 수정 차단(isContentLocked) 정책 제거 — 게시기간 무관 전체 수정 허용.

// ── 발행자 대상 트리(scope-tree) ──────────────────────────────
const canSelectAll = ref(true);
const scopeList = ref([]);
// 조회 팝업에서 선택된 사업장/노드(코드+명칭). 명칭은 disabled input 으로만 노출.
const pickSiteCd = ref("");
const pickSiteNm = ref("");
const pickNodeCd = ref("");
const pickNodeNm = ref("");

onMounted(async () => {
  formData.noticeId = props.noticeData?.noticeId || "";
  await fnLoadDetail();
  await fnLoadScopeTree();
});

// 상세 조회 → formData 매핑 (서버가 LAST_READ_DATE 도 갱신)
const fnLoadDetail = async () => {
  if (!formData.noticeId) return;
  try {
    const response = await axios.get("/webApi/notice01/notice-info", {
      params: { noticeId: formData.noticeId },
    });
    if (response.status === 200) {
      const info = response.data?.noticeInfo || {};
      formData.title = info.title || "";
      formData.content = info.content || "";
      formData.targetScope = info.targetScope || "ALL";
      formData.includeDailyYn = info.includeDailyYn || "N";
      formData.popupYn = info.popupYn || "N";
      formData.pinYn = info.pinYn || "N";
      formData.pinOrder = info.pinOrder || 1;
      formData.insertUserNm = info.insertUserNm || "";
      formData.insertDate = info.insertDate || "";

      // 팝업 기간: 서버 YYYYMMDD → 폼(달력) YYYY-MM-DD
      formData.popupFromYmd = ymdCompactToDash(info.popupFromYmd);
      formData.popupToYmd = ymdCompactToDash(info.popupToYmd);

      formData.fileList = response.data?.fileList || [];

      const targetList = response.data?.targetList || [];
      formData.targetList = targetList.map((t) => ({
        siteCd: t.siteCd,
        siteNm: t.siteNm,
        nodeCd: t.nodeCd || null,
        nodeNm: t.nodeNm || null,
        includeDescendantsYn: t.includeDescendantsYn || "Y",
      }));

      // 대상 요약(조회 표시용)
      formData.targetSummary = fnBuildTargetSummary(info.targetScope, formData.targetList);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 대상 요약 문자열(전사 / 사업장명·노드명 외 N건)
const fnBuildTargetSummary = (scope, list) => {
  if (scope === "ALL") return "전사";
  if (!list || list.length === 0) return "-";
  const first = list[0];
  const head = first.nodeNm
    ? `${first.siteNm} / ${first.nodeNm}`
    : first.siteNm;
  return list.length > 1 ? `${head} 외 ${list.length - 1}건` : head;
};

// 발행자 대상선택 트리(수정 시 대상 재설정용)
const fnLoadScopeTree = async () => {
  try {
    const response = await axios.get("/webApi/notice01/scope-tree");
    if (response.status === 200) {
      canSelectAll.value = response.data?.canSelectAll === true;
      scopeList.value = response.data?.scopeList || [];
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

const fnAddTarget = () => {
  if (!pickSiteCd.value) return proxy.$alert("사업장을 조회하여 선택해 주세요.");

  let nodeCd = null;
  let nodeNm = null;
  if (formData.targetScope === "NODE") {
    if (!pickNodeCd.value) return proxy.$alert("노드를 조회하여 선택해 주세요.");
    nodeCd = pickNodeCd.value;
    nodeNm = pickNodeNm.value || pickNodeCd.value;
  }

  const dup = formData.targetList.some(
    (t) => t.siteCd === pickSiteCd.value && (t.nodeCd || null) === (nodeCd || null)
  );
  if (dup) return proxy.$alert("이미 추가된 대상입니다.");

  formData.targetList.push({
    siteCd: pickSiteCd.value,
    siteNm: pickSiteNm.value || pickSiteCd.value,
    nodeCd,
    nodeNm,
    includeDescendantsYn: "Y",
  });
  // 노드만 초기화(사업장은 연속 추가 편의를 위해 유지)
  pickNodeCd.value = "";
  pickNodeNm.value = "";
};

const fnRemoveTarget = (i) => {
  formData.targetList.splice(i, 1);
};

// 049-03: 첨부 편집 — 숨긴 native input 을 버튼으로 트리거
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
  const response = await axios.post("/webApi/notice01/upload-file", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return response.data; // { fileMgmtCd, fileNm }
};

// 수정 모드 진입 (비번 검증, master 면제)
const fnEnterEditMode = async () => {
  if (isMaster.value) {
    isEditMode.value = true;
    return;
  }
  if (!editPwdInput.value) return proxy.$alert("비밀번호를 입력해 주세요.");
  try {
    const response = await axios.post(
      "/webApi/notice01/verify-pwd",
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
    const response = await axios.get("/webApi/notice01/file-download-token", {
      params: { noticeId: formData.noticeId, fileMgmtCd: file.fileMgmtCd },
    });
    if (response.status === 200 && response.data?.token) {
      const base = String(resolveBaseURL() || "").replace(/\/$/, "");
      const url =
        `${base}/webApi/notice01/file-download?token=` +
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
  if (!formData.title) return proxy.$alert("제목을 입력해 주세요.");
  if (!formData.content) return proxy.$alert("내용을 입력해 주세요.");
  if (formData.popupYn === "Y" && (!formData.popupFromYmd || !formData.popupToYmd)) {
    return proxy.$alert("팝업 기간을 입력해 주세요.");
  }
  if (
    formData.targetScope !== "ALL" &&
    (!formData.targetList || formData.targetList.length === 0)
  ) {
    return proxy.$alert("대상을 1건 이상 추가해 주세요.");
  }

  const targetList =
    formData.targetScope === "ALL"
      ? []
      : formData.targetList.map((t) => ({
          siteCd: t.siteCd,
          nodeCd: formData.targetScope === "NODE" ? t.nodeCd : null,
          includeDescendantsYn:
            formData.targetScope === "NODE" ? t.includeDescendantsYn : "N",
        }));

  // 저장/업로드 진행 중이면 중복 제출 차단
  if (saving.value) return;
  saving.value = true;

  try {
    // 049-03: 첨부 재구성 = 유지된 기존 첨부 + 신규 업로드분.
    //   - 신규 newFiles 를 먼저 업로드해 FILE_MGMT_CD 발급(확장자 화이트리스트는 서버 최종 검증).
    //   - 서버 update-notice 는 fileList 로 TB_NOTICE_FILE 전체 재구성(delete+insert)하므로,
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
      // master 가 아니면 검증 비번 동반(수정 모드 진입 시 통과한 비번 재사용)
      editPwd: editPwdInput.value,
      title: formData.title,
      content: formData.content,
      targetScope: formData.targetScope,
      includeDailyYn: formData.targetScope === "ALL" ? "N" : formData.includeDailyYn,
      popupYn: formData.popupYn,
      popupFromYmd:
        formData.popupYn === "Y" ? ymdDashToCompact(formData.popupFromYmd) : "",
      popupToYmd:
        formData.popupYn === "Y" ? ymdDashToCompact(formData.popupToYmd) : "",
      pinYn: formData.pinYn,
      pinOrder: formData.pinYn === "Y" ? formData.pinOrder : null,
      targetList,
      // 유지된 기존 첨부 + 신규 업로드분
      fileList: [...keptFiles, ...newPayload],
    };

    const response = await axios.post(
      "/webApi/notice01/update-notice",
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
</script>

<style scoped>
/* 전역 .modal-content-wide(max-width:1400px) override — 이 팝업은 약 70% 폭으로 축소 */
.modal-content-wide {
  max-width: 980px;
}

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
}

.notice-form input:not([type="checkbox"]):not([type="radio"]):not(.search-input),
.notice-form textarea {
  width: 100%;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: var(--space-xs, 0.375rem) var(--space-sm, 0.5rem);
  font-size: var(--font-size-sm, 0.875rem);
}

.notice-form input[readonly],
.notice-form textarea[readonly] {
  background: var(--color-bg, #f9fafb);
  color: var(--color-text-muted, #4b5563);
}

.form-divider {
  border: none;
  border-top: 1px solid var(--color-border, #e5e7eb);
  margin: var(--space-sm, 0.5rem) 0;
}

.pwd-area,
.period-area {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
}

/* 비밀번호 입력칸: 숫자 6자리 정도만 들어가게 고정(전역 width:100% 보다 높은 우선순위로 override) */
.notice-form .pwd-area input:not([type="checkbox"]):not([type="radio"]):not(.search-input) {
  flex: 0 0 auto;
  width: 7rem;
  max-width: 100%;
}

.pwd-area .btn-secondary {
  flex: 0 0 auto;
  width: auto;
  white-space: nowrap;
}

.checkbox-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 0.375rem);
  flex-shrink: 0;
  white-space: nowrap;
  margin-left: 0;
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
  cursor: pointer;
}

.date-range-sep {
  margin: 0 var(--space-xs, 0.25rem);
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

/* 전역 .form-row span(flex:0 0 50px) 상쇄 — 파일명/아이콘이 내용 폭만큼만 차지(아이콘 우측 여백 제거) */
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

/* 049-03: 수정모드 첨부 편집 영역 */
.file-area {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-xs, 0.375rem);
}

/* native file input 숨김 → '파일 추가' 버튼으로만 트리거 */
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

/* 수정모드 첨부 칩 목록(가로 wrap) — 조회모드 세로 리스트와 구분 */
.file-list--edit {
  flex-direction: row;
  flex-wrap: wrap;
  gap: var(--space-xs, 0.375rem);
}

/* 첨부 칩(기존/신규) */
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

/* 등록정보(대상/등록자/등록일시) — 3등분 균등 영역, 한 줄 표시(전체 폭 = textarea 동일) */
/* 등록정보 행: 라벨('등록정보')과 input 의 아래 끝단을 맞춤(높이 차 → 하단 정렬) */
.form-row.meta-row {
  align-items: end;
}

.meta-info {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-sm, 0.5rem);
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
}

/* 각 항목: 라벨(고정) + input(나머지 폭 가득) → 3등분 영역을 input 이 꽉 채움 */
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

/* 전역 .notice-form input(width:100%) 위에서 셀 내 남는 폭을 채우도록 */
.meta-info__item input {
  flex: 1 1 auto;
  min-width: 0;
}

/* 수정 모드: 대상 지정 블록 (생성 팝업과 동일 스타일) */
.form-section-title {
  font-weight: 700;
  color: var(--color-text-strong, #111827);
  font-size: var(--font-size-sm, 0.875rem);
}

/* 스코프 선택: 세그먼트(pill) 형태, 좁은 폭에서는 통째로 줄바꿈 */
.radio-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-xs, 0.5rem);
}

.radio-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 0.375rem);
  flex-shrink: 0;
  white-space: nowrap;
  /* 전역 .form-row label(margin-left:10px) 제거 — 대상 영역(select)과 시작 위치 정렬 */
  margin-left: 0;
  padding: var(--space-xs, 0.375rem) var(--space-md, 0.875rem);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-pill, 999px);
  background: var(--color-surface, #ffffff);
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
  cursor: pointer;
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

.target-area {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs, 0.375rem);
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

.target-chip-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs, 0.375rem);
}

.target-chip {
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

.target-chip__del {
  flex: 0 0 auto;
  margin-left: 0;
  border: none;
  background: transparent;
  color: var(--color-text-muted, #4b5563);
  cursor: pointer;
  font-size: var(--font-size-sm, 0.875rem);
  line-height: 1;
}

.pin-order-input {
  width: 80px;
}

/* 049-04: 상단 고정 + 고정 순번 한 행 배치 */
.pin-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-md, 1rem);
}

.pin-order-inline {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 0.5rem);
}

.pin-order-label {
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
  white-space: nowrap;
}

.hint {
  margin-top: var(--space-xxs, 0.25rem);
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted, #4b5563);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm, 0.5rem);
  padding: var(--space-sm, 0.5rem) var(--space-md, 1rem);
  border-top: 1px solid var(--color-border, #e5e7eb);
}

.btn-primary {
  height: var(--btn-height, 29px);
  padding: 0 var(--btn-padding, 11px);
  border: none;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-primary, #16a34a);
  color: #ffffff;
  font-size: var(--btn-font, 11px);
  cursor: pointer;
}

.btn-primary:hover {
  background: var(--color-primary-hover, #15803d);
}

/* 049-05: 수정모드 저장 버튼(footer 유일 버튼) — 텍스트 개행 방지 + 최소 너비 */
.modal-footer .btn-primary {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  white-space: nowrap;
  line-height: 1;
}

.btn-secondary {
  height: var(--btn-height, 29px);
  padding: 0 var(--btn-padding, 11px);
  border: 1px solid var(--color-border-strong, #d1d5db);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-surface, #ffffff);
  color: var(--color-text, #374151);
  font-size: var(--btn-font, 11px);
  cursor: pointer;
}
</style>
