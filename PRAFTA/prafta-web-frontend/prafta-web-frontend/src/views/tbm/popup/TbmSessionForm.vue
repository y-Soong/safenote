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
                  <img
                    class="search_icon"
                    :src="search_icon"
                    alt="사업장 조회"
                  />
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
              <div class="editor-col">
                <!-- RC-3: AI 교육안 생성 툴바 -->
                <div class="ai-toolbar">
                  <button
                    v-if="isEditMode"
                    type="button"
                    class="btn btn-second btn-sm"
                    :disabled="isGenerating"
                    @click="fnGenerateAiContent"
                  >
                    {{ isGenerating ? "생성 중…" : "🪄 AI 교육안 생성" }}
                  </button>
                  <button
                    v-if="isEditMode"
                    type="button"
                    class="btn btn-second btn-sm"
                    :disabled="isCheckingUnconfirmed || isGenerating"
                    @click="fnLoadUnconfirmed"
                  >
                    {{ isCheckingUnconfirmed ? "확인 중…" : "새로고침" }}
                  </button>
                  <!-- R3: 목표 글자수 입력(기본 1000, 800~5000 — 서버 TBM_400_061 과 동일 범위) -->
                  <label v-if="isEditMode" class="ai-target-chars">
                    목표 글자수
                    <input
                      type="number"
                      v-model.number="aiTargetChars"
                      min="800"
                      max="5000"
                      :disabled="isGenerating"
                    />
                    자
                  </label>
                  <span v-if="!isEditMode" class="ai-hint">
                    ⓘ 임시저장 후 AI 교육안을 생성할 수 있습니다.
                  </span>
                </div>

                <!-- R1/R3 안내 문구(수정모드) — 반영 재료(D11) + 분량 오차(D12) -->
                <span v-if="isEditMode" class="ai-hint">
                  ⓘ 확정된 교육자료와 연계된 위험성평가가 교육안 생성에 반영됩니다.
                </span>
                <span v-if="isEditMode" class="ai-hint">
                  ⓘ 800~5000자. 실제 생성 분량은 목표치에서 다소 오차가 있을 수 있습니다.
                </span>

                <!-- 미확정 AI 분석 항목 안내 블록(있을 때만) — 생성은 차단하지 않고 해당 항목만 제외됨 -->
                <div
                  v-if="isEditMode && unconfirmedItems.length > 0"
                  class="ai-unconfirmed"
                >
                  <p class="ai-unconfirmed-msg">
                    AI 분석이 확정되지 않은 아래 자료는 교육안 생성에서 제외됩니다.
                    포함하려면 [AI 분석 관리] 탭에서 확정한 뒤 [새로고침]을 눌러 주세요.
                  </p>
                  <ul class="ai-unconfirmed-list">
                    <li
                      v-for="item in unconfirmedItems"
                      :key="item.mtrlItemCd"
                    >
                      {{ item.mtrlTitle }} /
                      {{ item.mtrlDesc || "(설명 없음)" }} ({{ item.statusLabel }})
                    </li>
                  </ul>
                </div>

                <div class="editor-wrap">
                  <QuillEditor
                    v-model:content="formData.contentBody"
                    contentType="html"
                    theme="snow"
                    style="height: 14rem"
                    ref="editorRef"
                  />
                </div>

                <!-- RC-3: 생성 결과 안내(제외항목/품질저하) -->
                <p v-if="aiNotice" class="ai-notice">{{ aiNotice }}</p>
                <p v-if="aiQualityWarn" class="ai-notice-warn">
                  ⚠ 관리자 교육내용 미입력 — 확정 자료만으로 추정 생성되었습니다.
                </p>
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

            <!-- 교육 시간(인정 분): 1~60, 개설 단계 선택(빈값 허용) -->
            <div class="form-row-max">
              <label>교육 시간(분)</label>
              <div style="width: 30%">
                <input
                  type="number"
                  v-model.number="formData.eduMinutes"
                  min="1"
                  max="60"
                  style="width: 100%"
                />
              </div>
              <span class="hint">1분 이상 60분 이하</span>
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

            <!-- 연동 회사 지정(PRAFTA-SUBCON-T5) -->
            <div class="form-row-max grid-row">
              <label>연동 회사</label>
              <div class="grid-content">
                <div class="grid-toolbar">
                  <button
                    type="button"
                    class="btn btn-second btn-sm"
                    :disabled="!isEditMode"
                    @click="fnOpenSharePop"
                  >
                    연동 회사 선택
                  </button>
                  <span v-if="!isEditMode" class="hint">
                    ⓘ 임시저장 후 연동 회사를 지정할 수 있습니다.
                  </span>
                  <span v-else class="hint">
                    ⓘ 지정한 회사 직원이 이 교육에 입실할 수 있습니다.
                  </span>
                </div>
                <table class="data-grid sub-grid">
                  <thead>
                    <tr>
                      <th style="width: 6%; text-align: center">순서</th>
                      <th>회사명</th>
                      <th style="width: 14%">지정일시</th>
                      <th style="width: 8%; text-align: center">해제</th>
                    </tr>
                  </thead>
                  <tbody>
                    <template v-if="shareRows.length === 0">
                      <tr>
                        <td colspan="4" class="edu-grid-empty">
                          지정된 연동 회사가 없습니다.
                        </td>
                      </tr>
                    </template>
                    <template v-else>
                      <tr v-for="(row, idx) in shareRows" :key="row.shareId">
                        <td style="text-align: center">{{ idx + 1 }}</td>
                        <td>{{ row.cmpnyNm }}</td>
                        <td>{{ row.designatedDtime }}</td>
                        <td style="text-align: center">
                          <button
                            type="button"
                            class="btn btn-second btn-sm"
                            @click="fnReleaseShare(row)"
                          >
                            해제
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
import TbmShareCmpnyPop from "./TbmShareCmpnyPop.vue";
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

// RC-3: AI 교육안 생성 상태
const isGenerating = ref(false);
const aiNotice = ref(""); // 통합/제외 안내 문구
const aiQualityWarn = ref(false); // qualityDegraded 안내
// R3: 목표 글자수(기본 1000, 800~5000 — 서버 TBM_400_061 과 동일 하드계약)
const aiTargetChars = ref(1000);

// 미확정 AI 분석 항목(제외 예정 목록 안내용 — 생성은 차단하지 않음, 2026-07-11 기획 변경).
// 확정 자료 0건이면 서버 generate 가 "분석할 자료가 없습니다"(TBM_409_060)로 거부한다.
const unconfirmedItems = ref([]);
const isCheckingUnconfirmed = ref(false);

const isEditMode = computed(() => !proxy.$util.isEmpty(props.sessionCd_p));

const formData = reactive({
  siteCd: "",
  title: "",
  contentBody: "",
  gpsVerifyTypeCd: "AUTO",
  managerGpsLat: "",
  managerGpsLon: "",
  gpsVerifyRadiusM: 100,
  eduMinutes: null,
});

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const riskKey = (row) =>
  [row.siteCd, row.processCd, row.assessmentCd].join("|");

// ===== 연동 회사 지정(PRAFTA-SUBCON-T5) =====
// shareRows = 내가 직접 지정한 회사 목록. 하위 재지정 회사는 개사 수(subCount)로만 내려온다.
const shareRows = ref([]);

// 지정 현황 조회(수정 모드 = sessionCd 확정 이후에만 호출 가능)
const fnSearchShares = async () => {
  if (!isEditMode.value) {
    shareRows.value = [];
    return;
  }
  try {
    const response = await axios.get("/webApi/tbm02/session-shares", {
      params: { sessionCd: props.sessionCd_p },
    });
    if (response.status === 200) {
      shareRows.value = response.data?.shareList || [];
    }
  } catch (err) {
    shareRows.value = [];
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

// 연동 회사 선택 팝업(신규 개설 전에는 sessionCd 가 없어 호출 불가 → 버튼 비활성)
const fnOpenSharePop = () => {
  if (!isEditMode.value) return;
  openPop(TbmShareCmpnyPop, {
    sessionCd_p: props.sessionCd_p,
    onSaved: fnSearchShares,
  });
};

// 지정 해제(하위 재지정 캐스케이드 — 서버가 함께 해제한다)
const fnReleaseShare = async (row) => {
  const confirmed = await proxy.$confirm(
    "지정을 해제하면 해당 회사(및 그 하위 재지정 회사)의 신규 입실이 차단됩니다. 이미 입실한 참석자는 유지됩니다."
  );
  if (!confirmed) return;

  try {
    const response = await axios.post("/webApi/tbm02/session-share-release", {
      sessionCd: props.sessionCd_p,
      shareCmpnyCd: row.cmpnyCd,
    });
    if (response.status === 200) {
      await proxy.$alert("연동 회사 지정을 해제했습니다.");
      fnSearchShares();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다.")
    );
  }
};

onMounted(async () => {
  await fnGetSiteList();

  if (isEditMode.value) {
    if (props.detail_p) {
      fnLoadFromDetail(props.detail_p);
    }
    // 수정 모드: 미확정 AI 분석 항목 선조회(제외 예정 목록 안내용)
    fnLoadUnconfirmed();
    // 연동 회사 지정 현황 로딩
    fnSearchShares();
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
  formData.eduMinutes =
    s.eduMinutes !== undefined && s.eduMinutes !== null ? s.eduMinutes : null;
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
  // 교육 시간: 입력 시에만 1~60 범위 검증(개설 단계는 빈값 허용, 서버가 최종 권위)
  if (
    formData.eduMinutes !== null &&
    formData.eduMinutes !== "" &&
    formData.eduMinutes !== undefined
  ) {
    const m = Number(formData.eduMinutes);
    if (!Number.isInteger(m) || m < 1 || m > 60) {
      proxy.$alert("교육 시간은 1분 이상 60분 이하로 입력해 주세요.");
      return false;
    }
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
  eduMinutes:
    formData.eduMinutes === "" || formData.eduMinutes === undefined
      ? null
      : formData.eduMinutes,
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

// R4: 수정모드 저장 성공 후 팝업을 유지하며 내부 데이터를 최신화하는 재조회.
//     콘솔(TbmSessionConsole)은 fnEdit 시 이미 닫혀 detail_p 재전달 경로가 없으므로
//     session-detail 을 직접 재조회한다(D9). 실패해도 팝업은 닫지 않는다(재시도 가능).
const fnReloadDetail = async () => {
  try {
    const response = await axios.get("/webApi/tbm02/session-detail", {
      params: { sessionCd: props.sessionCd_p },
    });
    if (response.status === 200) {
      fnLoadFromDetail(response.data || {});
      fnLoadUnconfirmed();
      fnSearchShares();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
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
      // R4: 팝업을 닫지 않고 유지 — 저장 직후 AI 교육안 생성 등 연속 작업 지원(D10).
      await proxy.$alert(
        "저장되었습니다. 이어서 AI 교육안 생성 등 추가 수정이 가능합니다."
      );
      if (typeof props.onSearch === "function") props.onSearch();
      await fnReloadDetail();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.")
    );
  }
};

// 에디터 HTML → 공백 보존 plain text(관리자 방향 시드용). stripHtml 은 공백을 제거하므로 분리 사용
const extractPlainText = (html) => {
  if (!html) return "";
  const tmp = document.createElement("div");
  tmp.innerHTML = html;
  return (tmp.textContent || tmp.innerText || "").trim();
};

// 미확정 AI 분석 항목 조회 — GET /webApi/tbmai02/unconfirmed-items {sessionCd}
//   제외 예정 목록 안내 전용(생성 차단 없음). 조회 실패해도 기능을 막지 않는다.
const fnLoadUnconfirmed = async () => {
  if (!isEditMode.value) return;
  isCheckingUnconfirmed.value = true;
  try {
    const response = await axios.get("/webApi/tbmai02/unconfirmed-items", {
      params: { sessionCd: props.sessionCd_p },
    });
    unconfirmedItems.value = response.data?.items || [];
  } catch (err) {
    console.warn("미확정 AI 분석 항목 조회 실패", err);
    unconfirmedItems.value = [];
  } finally {
    isCheckingUnconfirmed.value = false;
  }
};

// RC-3: [AI 교육안 생성] — POST /webApi/tbmai02/generate {sessionCd, adminContentText, targetChars}
//        성공 시 formData.contentBody 를 응답 genContent(HTML)로 덮어쓰고 안내 갱신.
//        영속은 기존 [저장] 버튼(update-session)으로 처리(generate 는 DB 미기록 초안 반환).
const fnGenerateAiContent = async () => {
  if (!isEditMode.value) return; // 신규 폼은 버튼 미노출 — 방어

  // R3: 목표 글자수 클라 1차 검증(서버 TBM_400_061 과 동일 문구/범위) — 빈 입력·비정수 포함
  if (
    !Number.isInteger(aiTargetChars.value) ||
    aiTargetChars.value < 800 ||
    aiTargetChars.value > 5000
  ) {
    await proxy.$alert("목표 글자수는 800자 이상 5000자 이하로 입력해 주세요.");
    return;
  }

  // 기존 교육 내용이 있으면 덮어쓰기 확인
  const hasExisting = stripHtml(formData.contentBody).length > 0;
  if (hasExisting) {
    const ok = await proxy.$confirm(
      "기존 교육 내용을 AI 초안으로 덮어씁니다. 진행할까요?"
    );
    if (!ok) return;
  }

  // 현재 에디터 텍스트를 관리자 방향 시드로 전달(없으면 서버가 qualityDegraded=true)
  const adminContentText = extractPlainText(formData.contentBody);

  isGenerating.value = true;
  try {
    const response = await axios.post(
      "/webApi/tbmai02/generate",
      {
        sessionCd: props.sessionCd_p,
        adminContentText,
        targetChars: aiTargetChars.value,
      },
      { headers: { "Content-Type": "application/json" } }
    );

    const data = response.data || {};
    formData.contentBody = data.genContent || "";
    aiNotice.value = `AI 분석·확정 자료 ${
      data.includedItemCount ?? 0
    }건, 연계 위험성평가 ${
      data.includedRiskCount ?? 0
    }건을 반영했습니다. 미분석/미확정 자료는 제외됩니다.`;
    aiQualityWarn.value = !!data.qualityDegraded;
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "AI 교육안 생성 중 오류가 발생했습니다.")
    );
  } finally {
    isGenerating.value = false;
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
  height: 100%;
  min-height: 0;
}

/* 헤더~첫 행 과다 여백 수정:
   전역 .prafta-modal-popup .form-container(padding:20px)와 .content-wrapper(padding:1.2rem)가 겹쳐
   상단에만 여백이 쌓인다. 자손 선택자로 명시도를 올려 전역 20px 를 확실히 덮고,
   좌우/상하 여백은 .content-wrapper 의 1.2rem 만 남기되 스크롤바 여백(우측 0.5rem)은 유지한다. */
.content-wrapper .form-container {
  padding: 0 0.5rem 0 0;
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

/* RC-3: AI 교육안 생성 툴바/안내 */
.editor-col {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs, 0.375rem);
  width: 100%;
}

.ai-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
}

.ai-hint {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
  /* 안내 문구는 개행 없이 한 줄로 표시 */
  white-space: nowrap;
}

/* R3: 목표 글자수 입력 그룹 — 기존 .ai-toolbar flex 흐름에 편승 */
.ai-target-chars {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 0.375rem);
  font-size: var(--btn-font-sm);
  color: var(--color-text);
  white-space: nowrap;
}

.ai-target-chars input {
  width: 6rem;
}

.ai-notice {
  margin: 0;
  font-size: var(--btn-font-sm);
  color: var(--color-text);
}

.ai-notice-warn {
  margin: 0;
  font-size: var(--btn-font-sm);
  color: var(--color-danger);
}

/* 미확정 AI 분석 항목 경고 블록 */
.ai-unconfirmed {
  border: 1px solid var(--color-danger);
  background: var(--color-warning-bg);
  border-radius: var(--radius-sm, 0.375rem);
  padding: var(--space-sm, 0.5rem);
}

.ai-unconfirmed-msg {
  margin: 0 0 var(--space-xs, 0.375rem);
  font-size: var(--btn-font-sm);
  color: var(--color-danger);
}

.ai-unconfirmed-list {
  margin: 0;
  padding-left: 1.2rem;
  max-height: 8rem;
  overflow-y: auto;
  font-size: var(--btn-font-sm);
  color: var(--color-text);
}

.ai-unconfirmed-list li {
  margin: 0.125rem 0;
}
</style>
