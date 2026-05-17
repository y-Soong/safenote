<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
    />

    <!-- @Search="fnSearch"
    @save="fnSave"
    @create="fnCreate"
    @delete="fnDelete"
    @excel="fnExcel" -->

    <!-- 버튼 영역과 사업장 조회 영역 -->
    <div class="button-area-wrapper">
      <div class="button-group" ref="buttonGroupRef">
        <button
          v-for="(button, idx) in (baseInfoArr['COM002'] || []).filter(
            (o) => o.baimValDCd != null
          )"
          :key="idx"
          :ref="(el) => setButtonRef(el, idx)"
          :class="[
            'category-button',
            { active: selectedCategoryId === button.baimValDCd },
          ]"
          @click="fnCategoryClick(button, idx)"
        >
          <span class="button-text">{{ button.baimValDNm }}</span>
        </button>
        <div class="triangle-indicator" :style="triangleStyle"></div>
      </div>

      <!-- 사업장 조회 영역 -->
      <div class="site-search-area">
        <input
          type="checkbox"
          v-model="commonChk"
          @change="fnCommonChkChange"
        />
        <label
          style="
            margin-right: 10px;
            padding-right: 15px;
            border-right: 1px solid #ccc;
          "
          >공통관리</label
        >

        <label>사업장</label>
        <input
          id="siteNo"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          :disabled="commonChk"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="commonChk"
          @click="fnSiteSearchPopOpen()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          :disabled="commonChk"
          @blur="focusKill"
        />
      </div>
    </div>

    <!-- ✅ 테이블 2개 나란히 -->
    <div class="viewBody tables-row">
      <!-- FIRST TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 0 0 30%">
        <div class="subtitle-row">
          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <!-- 단순 마크 아이콘 (SVG) -->
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">위험분류</span>
          </div>

          <div class="custom-btn-area">
            <button class="btn btn-custom" @click="fnSearch_fst()">조회</button>
            <button class="btn btn-custom" @click="fnAddRow_fst()">생성</button>
            <button class="btn btn-custom" @click="fnSaveRow_fst()">
              저장
            </button>
            <button
              class="btn btn-custom"
              v-if="!commonChk"
              @click="fnDeleteRow_fst()"
            >
              삭제
            </button>
          </div>
        </div>

        <div
          class="table-box"
          style="--box-h: 65vh; --box-sticky-top: 1px; --box-ox: hidden"
        >
          <table class="data-grid w-full border-collapse text-sm">
            <thead>
              <tr>
                <th class="event_cell" style="width: 8%">No</th>
                <th style="width: 8%">
                  <input
                    id="headChk"
                    v-model="headChk_fst"
                    type="checkbox"
                    @click="fnHeadChk_fst()"
                  />
                </th>
                <th class="event_cell">
                  분류명 <br />
                  <input
                    v-model.trim="srchData_fst.riskTypeNm"
                    class="header-input"
                    type="text"
                    :maxlength="25"
                  />
                </th>
                <th class="event_cell" style="width: 30%">
                  사용여부 <br />
                  <select
                    v-model.trim="srchData_fst.useYn"
                    name="combo"
                    style="width: 100%"
                  >
                    <option
                      v-for="opt in systCodeArr['SYS003'] || []"
                      :key="opt.systValDCd"
                      :value="opt.systValDCd"
                    >
                      {{ opt.systValDNm }}
                    </option>
                  </select>
                </th>
              </tr>
            </thead>
            <tbody>
              <template
                v-if="
                  !filteredriskTypeResultList ||
                  filteredriskTypeResultList.length === 0
                "
              >
                <tr>
                  <td colspan="4" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(risk, idx) in filteredriskTypeResultList"
                  :key="risk.id"
                  @dblclick="fnSelectRiskType(risk)"
                >
                  <!-- @dblclick="fnSearch_sec(risk)" -->
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input
                      type="checkbox"
                      v-model="risk.chk"
                      v-if="commonChk || !proxy.$util.isEmpty(risk.siteCd)"
                    />
                  </td>
                  <td>
                    <input
                      style="width: 100%"
                      v-model="risk.riskTypeNm"
                      :disabled="!commonChk && proxy.$util.isEmpty(risk.siteCd)"
                    />
                  </td>
                  <td>
                    <BaseSelect
                      v-model="risk.useYn"
                      name="codeDetailSrc"
                      :disabled="!commonChk && proxy.$util.isEmpty(risk.siteCd)"
                    >
                      <option
                        v-for="opt in (systCodeArr['SYS003'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <!-- SECOND TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 1">
        <div class="subtitle-row">
          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <!-- 단순 마크 아이콘 (SVG) -->
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">위험발생상황</span>
          </div>

          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-text">[{{ targetValNm }}]</span>
          </div>

          <div class="custom-btn-area">
            <button class="btn btn-custom" @click="fnSearch_sec()">조회</button>
            <button
              class="btn btn-custom"
              type="button"
              @click="fnAddRow_sec()"
              v-if="canAddRiskHazardRow || commonChk"
            >
              생성
            </button>
            <button class="btn btn-custom" @click="fnSaveRow_sec()">
              저장
            </button>
            <button
              class="btn btn-custom"
              v-if="!commonChk"
              @click="fnDeleteRow_sec()"
            >
              삭제
            </button>
          </div>
        </div>

        <div
          class="table-box"
          style="
            --box-h: 65vh;
            --box-sticky-top: 1px;
            --box-ox: auto;
            width: 100%;
          "
        >
          <table class="data-grid w-full table-fixed border-collapse text-sm">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <th style="width: 4%">
                  <input
                    id="headChk"
                    v-model="headChk_sec"
                    type="checkbox"
                    @click="fnHeadChk_sec()"
                  />
                </th>
                <th style="width: 20%">
                  상황명<br />
                  <input
                    v-model.trim="srchData_sec.hazardNm"
                    class="header-input"
                    type="text"
                    :maxlength="25"
                  />
                </th>
                <th>
                  비고<br />
                  <input
                    v-model.trim="srchData_sec.hazardDesc"
                    class="header-input"
                    type="text"
                  />
                </th>
              </tr>
            </thead>
            <tbody>
              <template
                v-if="
                  !filteredRiskHazardResultList ||
                  filteredRiskHazardResultList.length === 0
                "
              >
                <tr>
                  <td colspan="4" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(hazard, idx) in filteredRiskHazardResultList"
                  :key="hazard.id"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input
                      type="checkbox"
                      v-model="hazard.chk"
                      v-if="commonChk || !proxy.$util.isEmpty(hazard.siteCd)"
                    />
                  </td>
                  <td>
                    <input
                      style="width: 100%"
                      v-model="hazard.hazardNm"
                      :disabled="
                        !commonChk && proxy.$util.isEmpty(hazard.siteCd)
                      "
                    />
                  </td>
                  <td>
                    <input
                      style="width: 100%"
                      v-model="hazard.hazardDesc"
                      :disabled="
                        !commonChk && proxy.$util.isEmpty(hazard.siteCd)
                      "
                    />
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
  computed,
  reactive,
} from "vue";
import { useFieldWatcher } from "@/utils/useFieldWatcher";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import { useModal } from "@/utils/useModal";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";

// =========================== Define ===========================
defineOptions({ name: "Risk_01" });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Reactive ===========================
/* 위험요인구분 조회 데이터 */
const srchData_fst = reactive({
  useYn: null,
  riskTypeNm: "",
});

/* 유해요인 조회 데이터 */
const srchData_sec = reactive({
  hazardNm: "",
  hazardDesc: "",
});

// =========================== Ref ===========================
/* 버튼 관련 */
const localButtons = ref({ ...props.buttons });
const buttonGroupRef = ref("");
const buttonRefs = ref([]);
const selectedButtonIndex = ref(0);

/* 카테고리/선택 관련 */
const selectedCategoryId = ref("");
const selectedRiskTypeCd = ref("");
const selectedRiskTypeNm = ref("");
const targetValNm = ref("");

/* 사업장 관련 */
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const commonChk = ref(true);

/* 체크박스 */
const headChk_fst = ref(false);
const headChk_sec = ref(false);

/* 데이터 리스트 */
const systCodeArr = ref([]);
const baseInfoArr = ref([]);
const riskTypeResultList = ref([]);
const riskHazardResultList = ref([]);

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// =========================== Computed ===========================
// 필터링된 위험요인구분 리스트
const filteredriskTypeResultList = computed(() => {
  let filtered = [...riskTypeResultList.value];

  // 위험요인구분명 필터링
  if (srchData_fst.riskTypeNm && srchData_fst.riskTypeNm.trim() !== "") {
    filtered = filtered.filter((item) =>
      item.riskTypeNm
        ?.toLowerCase()
        .includes(srchData_fst.riskTypeNm.toLowerCase())
    );
  }

  // 사용여부 필터링
  if (srchData_fst.useYn !== null && srchData_fst.useYn !== "") {
    filtered = filtered.filter((item) => item.useYn === srchData_fst.useYn);
  }

  return filtered;
});

// 필터링된 유해요인 리스트
const filteredRiskHazardResultList = computed(() => {
  let filtered = [...riskHazardResultList.value];

  // 유해요인명 필터링
  if (srchData_sec.hazardNm && srchData_sec.hazardNm.trim() !== "") {
    filtered = filtered.filter((item) =>
      item.hazardNm?.toLowerCase().includes(srchData_sec.hazardNm.toLowerCase())
    );
  }

  // 유해요인 비고 필터링
  if (srchData_sec.hazardDesc && srchData_sec.hazardDesc.trim() !== "") {
    filtered = filtered.filter((item) =>
      item.hazardDesc
        ?.toLowerCase()
        .includes(srchData_sec.hazardDesc.toLowerCase())
    );
  }

  return filtered;
});

/** 우측 위험발생상황 생성: 헤더 사업장 + 좌측 선택 분류 행의 siteCd 필요(테이블 disabled와 동일) */
const canAddRiskHazardRow = computed(() => {
  if (proxy.$util.isEmpty(siteCd.value)) return false;
  if (proxy.$util.isEmpty(selectedRiskTypeCd.value)) return false;
  const row = riskTypeResultList.value.find(
    (r) => r.riskTypeCd === selectedRiskTypeCd.value
  );
  if (!row) return false;
  if (proxy.$util.isEmpty(row.siteCd)) return false;
  return true;
});

// 세모 위치 계산
const triangleStyle = computed(() => {
  if (!buttonRefs.value[selectedButtonIndex.value]) {
    return { left: "0px", opacity: 0 };
  }

  const button = buttonRefs.value[selectedButtonIndex.value];
  const buttonGroup = buttonGroupRef.value;

  if (!buttonGroup) {
    return { left: "0px", opacity: 0 };
  }

  const buttonRect = button.getBoundingClientRect();
  const groupRect = buttonGroup.getBoundingClientRect();
  const left = buttonRect.left - groupRect.left + buttonRect.width / 2;

  return {
    left: `${left}px`,
    opacity: 1,
  };
});

// =========================== Life Cycle ===========================
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

onMounted(async () => {
  fnInit();
  fnButtonControll();
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
});

// =========================== Watch, Watcher ===========================
useFieldWatcher(
  riskTypeResultList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

useFieldWatcher(
  filteredRiskHazardResultList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// =========================== Methods ===========================
// API 호출
const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM002"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.baseInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.baimValCd;
        if (item.baimValDCd == null) return;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      baseInfoArr.value = grouped;

      fnCategoryClick(baseInfoArr.value["COM002"][0], 0);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS003"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      systCodeArr.value = grouped;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

// 조회
const fnSearch_fst = async () => {
  if (!commonChk.value) {
    if (proxy.$util.isEmpty(siteCd.value)) {
      proxy.$alert(getMessage(MSG.SITE_REQUIRED));
      return;
    }
  }

  riskTypeResultList.value = [];

  try {
    const response = await axios.get("/webApi/risk01/risk-type-lists", {
      params: {
        processCd: selectedCategoryId.value,
        siteCd: siteCd.value,
        riskTypeNm: srchData_fst.riskTypeNm,
        useYn: srchData_fst.useYn,
      },
    });

    if (response.status === 200) {
      const resData = response.data?.riskTypeResultList || [];

      riskTypeResultList.value = resData;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSearch_sec = async () => {
  targetValNm.value =
    baseInfoArr.value.COM002.filter(
      (o) => o.baimValDCd == selectedCategoryId.value
    )[0].baimValDNm +
    " - " +
    selectedRiskTypeNm.value;

  riskHazardResultList.value = [];

  if (proxy.$util.isEmpty(selectedRiskTypeCd.value)) {
    proxy.$alert(getMessage(MSG.RISK_FACTOR_REQUIRED));
    return;
  }

  try {
    const response = await axios.get("/webApi/risk01/risk-hazard-lists", {
      params: {
        riskTypeCd: selectedRiskTypeCd.value,
        processCd: selectedCategoryId.value,
        siteCd: siteCd.value,
        hazardNm: srchData_sec.hazardNm,
        hazardDesc: srchData_sec.hazardDesc,
      },
    });
    if (response.status === 200) {
      const resData = response.data?.riskHazardResultList || [];

      riskHazardResultList.value = resData;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

// 저장/삭제
const fnSaveRow_fst = async () => {
  const filteredDetail = riskTypeResultList.value.filter((item) => item.chk);

  if (filteredDetail.length === 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/risk01/update-risk-types",
      filteredDetail
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSearch_fst();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSaveRow_sec = async () => {
  const filteredDetail = riskHazardResultList.value.filter((item) => item.chk);
  if (!(await fnDataValidationChk(filteredDetail))) {
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/risk01/update-risk-hazards",
      filteredDetail
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      // fnSearch_fst();
      fnSearch_sec();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnDataValidationChk = async (filteredData) => {
  if (filteredData.length === 0) {
    await proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return false;
  }

  for (let i = 0; i < filteredData.length; i++) {
    if (proxy.$util.isEmpty(filteredData[i].hazardNm)) {
      await proxy.$alert(
        getMessage(MSG.GRID_ROW_FIELD_REQUIRED, {
          row: i + 1,
          fieldLabel: "상황명",
        })
      );
      return false;
    }
  }

  return true;
};

const fnDeleteRow_fst = async () => {
  const filteredDetail = riskTypeResultList.value.filter((item) => item.chk);

  if (filteredDetail.length === 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/risk01/delete-risk-types",
      filteredDetail
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSearch_fst();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "삭제 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnDeleteRow_sec = async () => {
  const filteredDetail = riskHazardResultList.value.filter((item) => item.chk);

  if (filteredDetail.length === 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/risk01/delete-risk-hazards",
      filteredDetail
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSearch_sec();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "삭제 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

// 행 추가
const fnAddRow_fst = () => {
  if (!commonChk.value && proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }

  riskTypeResultList.value.push({
    chk: true,
    cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd: siteCd.value,
    processCd: selectedCategoryId.value,
    useYn: "Y",
  });
};

const fnAddRow_sec = () => {
  if (proxy.$util.isEmpty(selectedRiskTypeCd.value)) {
    proxy.$alert(getMessage(MSG.RISK_FACTOR_REQUIRED));
    return;
  }

  if (!commonChk.value && proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }

  const parentRisk = riskTypeResultList.value.find(
    (r) => r.riskTypeCd === selectedRiskTypeCd.value
  );

  if (
    !commonChk.value &&
    parentRisk &&
    proxy.$util.isEmpty(parentRisk.siteCd)
  ) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }

  const hazardSiteCd =
    !commonChk.value && parentRisk && proxy.$util.isNotEmpty(parentRisk.siteCd)
      ? parentRisk.siteCd
      : siteCd.value;

  riskHazardResultList.value.push({
    chk: true,
    cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
    riskTypeCd: selectedRiskTypeCd.value,
    processCd: selectedCategoryId.value,
    siteCd: hazardSiteCd,
    useYn: "Y",
  });
};

// 체크박스
const fnHeadChk_fst = () => {
  headChk_fst.value = !headChk_fst.value;
  filteredriskTypeResultList.value.forEach((item) => {
    // 체크박스가 표시되지 않는 경우는 무조건 false
    const isVisible =
      commonChk.value ||
      (item.siteCd !== null && item.siteCd !== undefined && item.siteCd !== "");
    if (!isVisible) {
      item.chk = false;
    } else {
      item.chk = headChk_fst.value;
    }
  });
};

const fnHeadChk_sec = () => {
  headChk_sec.value = !headChk_sec.value;
  filteredRiskHazardResultList.value.forEach((item) => {
    // 체크박스가 표시되지 않는 경우는 무조건 false
    const isVisible =
      commonChk.value ||
      (item.siteCd !== null && item.siteCd !== undefined && item.siteCd !== "");
    if (!isVisible) {
      item.chk = false;
    } else {
      item.chk = headChk_sec.value;
    }
  });
};

// 카테고리/버튼 관련
const fnCategoryClick = (button, idx) => {
  selectedCategoryId.value = button.baimValDCd;
  selectedButtonIndex.value = idx;

  selectedRiskTypeCd.value = "";
  srchData_fst.riskTypeNm = "";
  srchData_fst.useYn = null;

  riskTypeResultList.value = [];
  riskHazardResultList.value = [];

  fnSearch_fst();
};

const fnSelectRiskType = (risk) => {
  selectedRiskTypeCd.value = risk.riskTypeCd;
  selectedRiskTypeNm.value = risk.riskTypeNm;

  srchData_sec.hazardNm = "";
  srchData_sec.hazardDesc = "";

  fnSearch_sec(risk);
};

const setButtonRef = (el, idx) => {
  if (el) {
    buttonRefs.value[idx] = el;
  }
};

const fnButtonControll = () => {
  localButtons.value.search = "N";
  localButtons.value.save = "N";
  localButtons.value.create = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

const fnCommonChkChange = () => {
  if (commonChk.value) {
    siteCd.value = "";
    siteNo.value = "";
    siteNm.value = "";
    fnSearch_fst();
  }

  selectedRiskTypeCd.value = "";
  srchData_fst.riskTypeNm = "";
  srchData_fst.useYn = null;

  srchData_sec.hazardNm = "";
  srchData_sec.hazardDesc = "";

  targetValNm.value = "";

  riskTypeResultList.value = [];
  riskHazardResultList.value = [];
};

// 사업장 관련
const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        userCd: sessionStorage.getItem("gv_userCd"),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
      },
    });

    if (response.status === 200) {
      fnCallback(response);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnCallback = (res) => {
  if (proxy.$util.isNotEmpty(res)) {
    const apiId = res.config.url.split("/").pop();

    if (apiId == "site-lists") {
      const siteList = res.data?.siteInfoResultList ?? [];
      if (siteList.length === 1) {
        siteCd.value = siteList[0].siteCd;
        siteNo.value = siteList[0].siteNo;
        siteNm.value = siteList[0].siteNm;
      } else if (siteList.length > 1) {
        fnSiteSearchPopOpen();
      } else {
        siteCd.value = "";
        siteNo.value = "";
        siteNm.value = "";
      }
    }
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

const focusKill = (e) => {
  if (e.target.id == "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
    } else {
      siteNm.value = "";
      siteFocusKill();
    }
  } else if (e.target.id == "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
    } else {
      siteNo.value = "";
      siteFocusKill();
    }
  }
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};
</script>

<style scoped>
.button-area-wrapper {
  background-color: #d5eee5;
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 0;
}

.button-group {
  display: flex;
  gap: 0;
  padding: 0;
  background-color: #d5eee5;
  position: relative;
  overflow: visible;
  margin-bottom: 0;
  flex: 1;
}

.site-search-area {
  display: flex;
  align-items: center;
  gap: 8px;
  /* padding: 10px; */
  background-color: #d5eee5;
  border-radius: 4px;
  margin-right: 10px;
}

.site-search-area label {
  white-space: nowrap;
  margin-right: 4px;
  font-family: "Pretendard", sans-serif;
  font-size: 0.9rem;
  font-weight: 500;
}

.site-search-area input {
  padding: 6px 10px;
  border: 1px solid #d5eee5;
  border-radius: 4px;
  font-size: 14px;
}

.site-search-area input[type="text"] {
  border: 1px solid #ccc;
  border-radius: 4px;
  padding: 0.05rem 0.3rem;
  width: 120px;
}

.search-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.search_icon {
  width: 16px;
  height: 16px;
}

.category-button {
  padding: 10px 40px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-weight: 600;
  font-size: clamp(14px, 1.2vw, 15px);
  letter-spacing: 0.2px;
  color: #1f1e1e60;
  position: relative;
  transition: all 10.3s ease;
  z-index: 1;
}

.category-button:hover {
  opacity: 0.9;
}

.category-button.active {
  color: #30796a;
  z-index: 2;
}

.triangle-indicator {
  position: absolute;
  bottom: -5px;
  width: 0;
  height: 0;
  border-left: 15px solid transparent;
  border-right: 15px solid transparent;
  border-bottom: 15px solid #fff;
  transform: translateX(-50%);
  transition: left 0.3s ease;
  z-index: 3;
  pointer-events: none;
}

.button-text {
  position: relative;
  z-index: 4;
}

/* 헤더 input 스타일 */
.header-input {
  width: 100%;
  box-sizing: border-box;
  padding: 0.2rem 0.3rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.25rem;
  font-size: 0.7rem;
  background: #fff;
  margin-top: 0.2rem;
}

/* 헤더 th에 input이 있을 때 padding 조정 */
.data-grid th:has(.header-input) {
  padding: 0.2rem 0.3rem;
  vertical-align: top;
}

/* 테이블 가로 스크롤 방지 */
.table-wrapper:first-child .table-box {
  overflow-x: hidden !important;
}

.table-wrapper:first-child .table-box .data-grid {
  width: 100% !important;
  min-width: 100% !important;
  max-width: 100% !important;
  table-layout: fixed;
}
</style>
