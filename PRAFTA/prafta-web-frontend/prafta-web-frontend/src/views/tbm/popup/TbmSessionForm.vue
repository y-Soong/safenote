<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>{{ isEditMode ? "TBM 교육 수정" : "TBM 교육 개설" }}</span>
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

        <div class="content-wrapper">
          <div class="form-container">
            <!-- 사업장: NoticeCreatePop 패턴 — 사업장명(readonly) + 조회 팝업 버튼 -->
            <div class="form-row-max">
              <label>사업장</label>
              <div class="site-search-field">
                <input
                  id="siteCd"
                  type="text"
                  :value="siteNm"
                  placeholder="사업장 조회"
                  readonly
                  :disabled="isEditMode"
                  @click="onSiteSearchClick"
                />
                <button
                  class="search-btn"
                  :disabled="isEditMode"
                  @click="onSiteSearchClick"
                >
                  <img class="search_icon" :src="search_icon" alt="사업장 조회" />
                </button>
              </div>
              <span v-if="isEditMode" class="hint">
                ⓘ 사업장은 수정할 수 없습니다.
              </span>
            </div>

            <!-- 제목 -->
            <div class="form-row-max">
              <label>교육 제목</label>
              <input
                id="title"
                v-model="formData.title"
                maxlength="200"
                placeholder="교육 제목"
                style="width: 100%"
              />
            </div>

            <!-- 교육 내용 (리치 HTML) -->
            <div class="form-row-max editor-row">
              <label>교육 내용</label>
              <div class="editor-wrap">
                <QuillEditor
                  v-model:content="formData.contentBody"
                  contentType="html"
                  theme="snow"
                  style="height: 14rem"
                  ref="editorRef"
                />
              </div>
            </div>

            <!-- GPS 검증 -->
            <div class="form-row-max">
              <label>GPS 검증</label>
              <div class="gps-group">
                <label class="radio-item">
                  <input
                    type="radio"
                    value="AUTO"
                    v-model="formData.gpsVerifyTypeCd"
                  />
                  활성화
                </label>
                <label class="radio-item">
                  <input
                    type="radio"
                    value="MANUAL"
                    v-model="formData.gpsVerifyTypeCd"
                  />
                  수동 확인(위치정보만 수집, 입실 시 관리자가 직접 확인)
                </label>
                <label class="radio-item">
                  <input
                    type="radio"
                    value="DISABLED"
                    v-model="formData.gpsVerifyTypeCd"
                  />
                  비활성화
                </label>
              </div>
            </div>

            <div
              v-if="formData.gpsVerifyTypeCd === 'AUTO'"
              class="form-row-max"
            >
              <label>현재 위치</label>
              <div class="gps-coord">
                <span v-if="gpsStatus === 'ok'" class="gps-ok">
                  위도 {{ formData.managerGpsLat }} / 경도
                  {{ formData.managerGpsLon }}
                </span>
                <span v-else-if="gpsStatus === 'loading'" class="gps-muted">
                  위치 수집 중...
                </span>
                <span v-else class="gps-muted">
                  위치를 가져오지 못했습니다.
                </span>
                <button
                  type="button"
                  class="btn btn-second btn-sm"
                  @click="fnCaptureGps"
                >
                  위치 다시 가져오기
                </button>
              </div>
            </div>

            <div
              v-if="formData.gpsVerifyTypeCd === 'MANUAL'"
              class="form-row-max"
            >
              <label>수동 확인</label>
              <label class="radio-item">
                <input type="checkbox" v-model="manualConfirm" />
                개설 위치를 직접 확인했습니다.
              </label>
            </div>

            <div
              v-if="formData.gpsVerifyTypeCd !== 'DISABLED'"
              class="form-row-max"
            >
              <label>검증 반경(m)</label>
              <div style="width: 30%">
                <input
                  type="number"
                  v-model.number="formData.gpsVerifyRadiusM"
                  min="50"
                  max="1000"
                  style="width: 100%"
                />
              </div>
              <span class="hint">50 ~ 1000m</span>
            </div>

            <!-- 콘텐츠 매핑 -->
            <div class="form-row-max grid-row">
              <label>교육 콘텐츠</label>
              <div class="grid-content">
                <div class="grid-toolbar">
                  <button
                    type="button"
                    class="btn btn-second btn-sm"
                    @click="fnOpenContentSelector"
                  >
                    교육자료 선택
                  </button>
                </div>
                <table class="data-grid sub-grid">
                  <thead>
                    <tr>
                      <th style="width: 6%; text-align: center">순서</th>
                      <th style="width: 35%">콘텐츠 제목</th>
                      <th style="width: 14%">카테고리</th>
                      <th>비고</th>
                      <th style="width: 8%; text-align: center">삭제</th>
                    </tr>
                  </thead>
                  <tbody>
                    <template v-if="contentRows.length === 0">
                      <tr>
                        <td colspan="5" class="edu-grid-empty">
                          선택된 콘텐츠가 없습니다.
                        </td>
                      </tr>
                    </template>
                    <template v-else>
                      <tr v-for="(row, idx) in contentRows" :key="row.mtrlCd">
                        <td style="text-align: center">{{ idx + 1 }}</td>
                        <td>{{ row.title }}</td>
                        <td>{{ row.mtrlTypeNm || row.mtrlType }}</td>
                        <td>
                          <input
                            v-model="row.overrideDesc"
                            maxlength="500"
                            style="width: 100%"
                          />
                        </td>
                        <td style="text-align: center">
                          <button
                            type="button"
                            class="btn btn-second btn-sm"
                            @click="fnRemoveContent(idx)"
                          >
                            삭제
                          </button>
                        </td>
                      </tr>
                    </template>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- 위험성평가 매핑 -->
            <div class="form-row-max grid-row">
              <label>위험성평가</label>
              <div class="grid-content">
                <div class="grid-toolbar">
                  <button
                    type="button"
                    class="btn btn-second btn-sm"
                    @click="fnOpenRiskSelector"
                  >
                    위험성평가 선택
                  </button>
                  <span v-if="riskRows.length === 0" class="risk-warn">
                    ⚠️ 위험성평가가 연계되지 않았습니다. (개설은 가능)
                  </span>
                </div>
                <table class="data-grid sub-grid">
                  <thead>
                    <tr>
                      <th style="width: 6%; text-align: center">순서</th>
                      <th>위험성평가</th>
                      <th style="width: 14%">공정</th>
                      <th style="width: 8%; text-align: center">삭제</th>
                    </tr>
                  </thead>
                  <tbody>
                    <template v-if="riskRows.length === 0">
                      <tr>
                        <td colspan="4" class="edu-grid-empty">
                          선택된 위험성평가가 없습니다.
                        </td>
                      </tr>
                    </template>
                    <template v-else>
                      <tr v-for="(row, idx) in riskRows" :key="riskKey(row)">
                        <td style="text-align: center">{{ idx + 1 }}</td>
                        <td>{{ row.displayName || "(이름 미정)" }}</td>
                        <td>{{ row.processNm || row.processCd }}</td>
                        <td style="text-align: center">
                          <button
                            type="button"
                            class="btn btn-second btn-sm"
                            @click="fnRemoveRisk(idx)"
                          >
                            삭제
                          </button>
                        </td>
                      </tr>
                    </template>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button
              v-if="!isEditMode"
              class="btn btn-second"
              @click="fnSave('DRAFT')"
            >
              임시저장
            </button>
            <button
              v-if="!isEditMode"
              class="btn btn-primary"
              @click="fnSave('OPENED')"
            >
              개설하기
            </button>
            <button v-if="isEditMode" class="btn btn-primary" @click="fnUpdate">
              저장
            </button>
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  reactive,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { QuillEditor } from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useModal } from "@/utils/useModal";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import TbmContentSelector from "./TbmContentSelector.vue";
import TbmRiskSelector from "./TbmRiskSelector.vue";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import search_icon from "@/assets/img/search_icon.png";

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const props = defineProps({
  sessionCd_p: String, // 수정 모드 진입 시 교육코드
  detail_p: Object, // 수정 모드 진입 시 기존 상세(상세 팝업에서 전달)
  onSearch: Function, // 목록 갱신 콜백
  onCreated: Function, // 개설/임시저장 성공 시 sessionCd 전달
});
const emit = defineEmits(["close"]);

const modalRef = ref(null);
const editorRef = ref(null);
const siteList = ref([]);
const siteNm = ref(""); // 사업장명 표시용(조회 팝업 선택값)
const contentRows = ref([]);
const riskRows = ref([]);
const manualConfirm = ref(false);
const gpsStatus = ref("idle"); // idle | loading | ok | fail

const isEditMode = computed(() => !proxy.$util.isEmpty(props.sessionCd_p));

const formData = reactive({
  siteCd: "",
  title: "",
  contentBody: "",
  gpsVerifyTypeCd: "AUTO",
  managerGpsLat: "",
  managerGpsLon: "",
  gpsVerifyRadiusM: 100,
});

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const riskKey = (row) =>
  [row.siteCd, row.processCd, row.assessmentCd].join("|");

onMounted(async () => {
  await fnGetSiteList();

  if (isEditMode.value && props.detail_p) {
    fnLoadFromDetail(props.detail_p);
  } else {
    // 신규: 기본 사업장 = 본인 사업장
    formData.siteCd = sessionStorage.getItem("gv_siteCd") || "";
    siteNm.value =
      resolveSiteNm(formData.siteCd) ||
      sessionStorage.getItem("gv_siteNm") ||
      "";
    // AUTO 모드 기본: 현재 위치 수집 시도
    fnCaptureGps();
  }
});

// 사업장코드 → 사업장명 매핑(siteList 기준). 표시용 siteNm 복원에 사용
const resolveSiteNm = (cd) => {
  if (proxy.$util.isEmpty(cd)) return "";
  const found = (siteList.value || []).find((s) => s.siteCd === cd);
  return found ? found.siteNm : "";
};

// 사업장 조회 팝업 열기(수정 모드에서는 잠금 — 사업장 변경 불가)
const onSiteSearchClick = () => {
  if (isEditMode.value) return;
  fnOpenSiteSearch();
};

const fnOpenSiteSearch = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    // SiteSearchPop onSelect 인자 순서 = (siteCd, siteNo, siteNm)
    onSelect: (siteCdVal, siteNoVal, siteNmVal) => {
      formData.siteCd = siteCdVal ?? "";
      siteNm.value = siteNmVal ?? "";
    },
  });
};

const fnGetSiteList = async () => {
  try {
    const response = await axios.get("/webApi/tbm02/site-options");
    if (response.status === 200) {
      siteList.value = response.data?.siteList || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

// 수정 모드: 상세 응답을 폼에 매핑
const fnLoadFromDetail = (detail) => {
  const s = detail.session || {};
  formData.siteCd = s.siteCd || "";
  siteNm.value = s.siteNm || resolveSiteNm(formData.siteCd);
  formData.title = s.title || "";
  formData.contentBody = s.contentBody || "";
  formData.gpsVerifyTypeCd = s.gpsVerifyTypeCd || "AUTO";
  formData.managerGpsLat = s.managerGpsLat || "";
  formData.managerGpsLon = s.managerGpsLon || "";
  formData.gpsVerifyRadiusM = s.gpsVerifyRadiusM || 100;
  manualConfirm.value = s.gpsManualConfirmYn === "Y";

  contentRows.value = (detail.contents || []).map((c) => ({
    mtrlCd: c.mtrlCd,
    title: c.title,
    mtrlType: c.mtrlType,
    mtrlTypeNm: c.mtrlTypeNm,
    overrideDesc: c.overrideDesc || "",
  }));

  riskRows.value = (detail.risks || []).map((r) => ({
    siteCd: r.siteCd,
    processCd: r.processCd,
    processNm: r.processNm,
    assessmentCd: r.assessmentCd,
    displayName: r.displayName,
  }));
};

// GPS 수집(AUTO 모드). 실패 시 MANUAL 안내
const fnCaptureGps = () => {
  if (formData.gpsVerifyTypeCd === "DISABLED") return;
  if (!navigator.geolocation) {
    gpsStatus.value = "fail";
    return;
  }
  gpsStatus.value = "loading";
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      formData.managerGpsLat = String(pos.coords.latitude.toFixed(7));
      formData.managerGpsLon = String(pos.coords.longitude.toFixed(7));
      gpsStatus.value = "ok";
    },
    () => {
      gpsStatus.value = "fail";
      // 좌표 수집 실패 시 수동 확인으로 전환 안내
      formData.gpsVerifyTypeCd = "MANUAL";
    },
    { enableHighAccuracy: true, timeout: 10000 }
  );
};

// 콘텐츠 선택 모달
const fnOpenContentSelector = () => {
  if (proxy.$util.isEmpty(formData.siteCd)) {
    proxy.$alert("사업장을 먼저 선택해 주세요.");
    return;
  }
  openPop(TbmContentSelector, {
    siteCd_p: formData.siteCd,
    selectedMtrlCds_p: contentRows.value.map((r) => r.mtrlCd),
    onConfirm: (selected) => {
      const existing = {};
      contentRows.value.forEach((r) => {
        existing[r.mtrlCd] = r.overrideDesc;
      });
      contentRows.value = selected.map((s) => ({
        mtrlCd: s.mtrlCd,
        title: s.title,
        mtrlType: s.mtrlType,
        mtrlTypeNm: s.mtrlTypeNm,
        overrideDesc: existing[s.mtrlCd] || "",
      }));
    },
  });
};

const fnRemoveContent = (idx) => {
  contentRows.value.splice(idx, 1);
};

// 위험성평가 선택 모달
const fnOpenRiskSelector = () => {
  if (proxy.$util.isEmpty(formData.siteCd)) {
    proxy.$alert("사업장을 먼저 선택해 주세요.");
    return;
  }
  openPop(TbmRiskSelector, {
    siteCd_p: formData.siteCd,
    selectedKeys_p: riskRows.value.map((r) => riskKey(r)),
    onConfirm: (selected) => {
      riskRows.value = selected.map((s) => ({
        siteCd: s.siteCd,
        processCd: s.processCd,
        processNm: s.processNm,
        assessmentCd: s.assessmentCd,
        displayName: s.displayName,
      }));
    },
  });
};

const fnRemoveRisk = (idx) => {
  riskRows.value.splice(idx, 1);
};

// 클라이언트 1차 검증(서버가 최종 권위)
const fnValidate = (mode) => {
  if (proxy.$util.isEmpty(formData.siteCd)) {
    proxy.$alert("사업장을 선택해 주세요.");
    return false;
  }
  if (proxy.$util.isEmpty(formData.title)) {
    proxy.$alert("교육 제목을 입력해 주세요.");
    return false;
  }
  // 개설 시에만 교육 내용/ GPS 강제
  if (mode === "OPENED") {
    const text = stripHtml(formData.contentBody);
    if (text.length < 10) {
      proxy.$alert("교육 내용을 10자 이상 입력해 주세요.");
      return false;
    }
    if (formData.gpsVerifyTypeCd === "AUTO") {
      if (
        proxy.$util.isEmpty(formData.managerGpsLat) ||
        proxy.$util.isEmpty(formData.managerGpsLon)
      ) {
        proxy.$alert(
          "현재 위치를 가져오지 못했습니다. 위치를 다시 가져오거나 수동 확인을 선택해 주세요."
        );
        return false;
      }
    } else if (formData.gpsVerifyTypeCd === "MANUAL") {
      if (!manualConfirm.value) {
        proxy.$alert("수동 확인 체크박스를 확인해 주세요.");
        return false;
      }
    }
  }
  return true;
};

const stripHtml = (html) => {
  if (!html) return "";
  const tmp = document.createElement("div");
  tmp.innerHTML = html;
  return (tmp.textContent || tmp.innerText || "").replace(/\s+/g, "");
};

const buildPayload = () => ({
  siteCd: formData.siteCd,
  title: formData.title,
  contentBody: formData.contentBody,
  gpsVerifyTypeCd: formData.gpsVerifyTypeCd,
  managerGpsLat:
    formData.gpsVerifyTypeCd === "AUTO" ? formData.managerGpsLat : "",
  managerGpsLon:
    formData.gpsVerifyTypeCd === "AUTO" ? formData.managerGpsLon : "",
  gpsVerifyRadiusM:
    formData.gpsVerifyTypeCd === "DISABLED" ? null : formData.gpsVerifyRadiusM,
  gpsManualConfirmYn:
    formData.gpsVerifyTypeCd === "MANUAL" && manualConfirm.value ? "Y" : "N",
  contents: contentRows.value.map((r, idx) => ({
    mtrlCd: r.mtrlCd,
    displayOrder: idx,
    overrideDesc: r.overrideDesc || null,
  })),
  risks: riskRows.value.map((r, idx) => ({
    siteCd: r.siteCd,
    processCd: r.processCd,
    assessmentCd: r.assessmentCd,
    displayOrder: idx,
  })),
});

const fnSave = async (mode) => {
  if (!fnValidate(mode)) return;

  const confirmMsg =
    mode === "OPENED"
      ? "TBM 교육을 개설하시겠습니까? (입실/종료 비밀번호가 발급됩니다.)"
      : "임시저장하시겠습니까?";
  const ok = await proxy.$confirm(confirmMsg);
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/tbm02/save-session",
      { saveMode: mode, ...buildPayload() },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      const data = response.data || {};
      if (data.warningMessage) {
        await proxy.$alert(data.warningMessage);
      }
      await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      emit("close");
      if (typeof props.onSearch === "function") props.onSearch();
      // 개설 성공 -> 상세(비번 표시)로 이동. 임시저장 -> 목록만 갱신
      if (
        mode === "OPENED" &&
        data.sessionCd &&
        typeof props.onCreated === "function"
      ) {
        props.onCreated(data.sessionCd);
      } else if (data.sessionCd && typeof props.onCreated === "function") {
        // 임시저장도 상세로 이동(이어서 수정 가능)
        props.onCreated(data.sessionCd);
      }
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.")
    );
  }
};

const fnUpdate = async () => {
  if (!fnValidate("OPENED")) return; // 수정은 OPENED 수준 검증 유지(서버가 상태 재검증)

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/tbm02/update-session",
      { sessionCd: props.sessionCd_p, ...buildPayload() },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      emit("close");
      if (typeof props.onSearch === "function") props.onSearch();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.")
    );
  }
};
</script>

<style scoped>
.modal-content-wide {
  width: min(1100px, 95vw);
  max-height: 90vh;
}

.content-wrapper {
  padding: 1.2rem;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  overflow-y: auto;
  padding-right: 0.5rem;
  height: 100%;
  min-height: 0;
}

.hint {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
  flex: 0 0 auto;
  white-space: nowrap;
}

/* 사업장 조회: 사업장명(readonly) + 돋보기 버튼 (NoticeCreatePop 패턴) */
.site-search-field {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 20%;
}

.site-search-field input {
  flex: 1 1 auto;
  min-width: 0;
  cursor: pointer;
}

.site-search-field input:disabled {
  cursor: not-allowed;
}

.editor-row {
  align-items: flex-start;
}

.editor-wrap {
  flex: 1;
  background: var(--color-surface);
}

.gps-group {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, max-content));
  gap: 1rem;
  align-items: center;
  white-space: nowrap;
}

.radio-item {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  cursor: pointer;
  color: var(--color-text);
  white-space: nowrap;
}

.gps-coord {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.75rem;
  align-items: center;
  min-width: 0;
  white-space: nowrap;
}

.gps-coord span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gps-ok {
  color: var(--color-text);
  font-size: var(--btn-font);
}

.gps-muted {
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
}

.grid-row {
  align-items: flex-start;
}

.grid-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.grid-toolbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.risk-warn {
  font-size: var(--btn-font-sm);
  color: var(--color-danger);
  white-space: nowrap;
}

.sub-grid {
  width: 100%;
}

.risk-warn {
  font-size: var(--btn-font-sm);
  color: var(--color-danger);
}

.btn-sm {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}
</style>
