<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />
    <!-- 
      @create="fnCreate"
      @save="fnSave"
      @delete="fnDelete"
      @excel="fnExcel" -->

    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="siteDisabled"
          @click="fnSiteSearchPopOpen('searchForm')"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
      </div>

      <div>
        <label>진행상태</label>
        <select v-model="assessmentStatus" name="combo">
          <option
            v-for="opt in systCodeArr['SYS011'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>위험성구분</label>
        <select v-model="proccessCd" name="combo">
          <option
            v-for="opt in baseCodeArr['COM002'] || []"
            :key="opt.baimValDCd"
            :value="opt.baimValDCd"
          >
            {{ opt.baimValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>위험성분류</label>
        <select v-model="riskTypeCd" name="combo">
          <option
            v-for="opt in riskTypeArr.filter((o) => {
              if (proxy.$util.isEmpty(proccessCd)) {
                if (proxy.$util.isEmpty(o.processCd)) {
                  return o;
                }
              } else {
                if (
                  o.processCd == proccessCd ||
                  proxy.$util.isEmpty(o.processCd)
                ) {
                  return o;
                }
              }
            })"
            :key="opt.riskTypeCd"
            :value="opt.riskTypeCd"
          >
            {{ opt.riskTypeNm }}
          </option>
        </select>
      </div>

      <div>
        <label>평가요청자</label>
        <input
          id="initAssessorNm"
          type="text"
          v-model="initAssessorNm"
          placeholder="평가요청자명"
          @blur="focusKill"
        />
      </div>

      <div>
        <label>평가요청일</label>
        <CalendarSrch v-model="initAssessDate" :range="false" />
        <button
          type="button"
          class="date-clear-btn"
          title="평가요청일 초기화"
          :disabled="!initAssessDate"
          @click="initAssessDate = ''"
        >
          ✕
        </button>
      </div>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <!-- ⬇️ 소제목 바 -->
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <!-- 단순 마크 아이콘 (SVG) -->
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">평가대상 리스트</span>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table
            class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <!-- <th style="width: 4%">
                  <input
                    id="headChk"
                    v-model="headChk"
                    type="checkbox"
                    @click="fnHeadChk"
                  />
                </th> -->
                <ThSortable
                  label="위험구분"
                  col-key="processNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.processNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="위험분류"
                  col-key="riskTypeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.riskTypeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="평가요청일"
                  col-key="initAssessDate"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.initAssessDate"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="유해요인명"
                  col-key="hazardNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.hazardNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="평가요청자"
                  col-key="initAssessorNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.initAssessorNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="진행상태"
                  col-key="assessmentStatusNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.assessmentStatusNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="개선 전 위험도"
                  col-key="initRiskLv"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.initRiskLv"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="개선 후 위험도"
                  col-key="revalRiskLv"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.revalRiskLv"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="유해요인설명"
                  col-key="initDesc"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.initDesc"
                  @sort="onSort"
                  @update:width="onResize"
                />
              </tr>
            </thead>
            <tbody>
              <template
                v-if="
                  !riskAssessmentResultList ||
                  riskAssessmentResultList.length === 0
                "
              >
                <tr>
                  <td colspan="10" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(risk, idx) in sortedData"
                  :key="risk.assessmentCd"
                  @dblclick="fnOpenRow(risk)"
                  style="cursor: pointer"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    {{ risk.processNm }}
                  </td>
                  <td>
                    {{ risk.riskTypeNm }}
                  </td>
                  <td>
                    {{ risk.initAssessDate }}
                  </td>
                  <td>
                    {{ risk.hazardNm }}
                  </td>
                  <td>
                    {{ risk.initAssessorNm }}
                  </td>
                  <td>
                    {{ risk.assessmentStatusNm }}
                  </td>
                  <td>
                    <span :class="getRiskLevelClass6(risk.initRiskLv)">
                      {{ formatRiskLevelText(risk.initRiskLv) }}
                    </span>
                  </td>
                  <td>
                    <span :class="getRiskLevelClass6(risk.revalRiskLv)">
                      {{ formatRiskLevelText(risk.revalRiskLv) }}
                    </span>
                  </td>
                  <td>
                    {{ risk.initDesc }}
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
  onActivated,
  getCurrentInstance,
  defineOptions,
  computed,
  watch,
  defineAsyncComponent,
} from "vue";
import { useModal } from "@/utils/useModal";
import { useDashboardNavStore } from "@/stores/dashboardNavStore";
import { useFieldWatcher } from "@/utils/useFieldWatcher";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import ViewHeader from "@/components/common/ViewHeader.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import RiskAssessInfo from "./popup/RiskAssessInfo.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import { useTableSort, useColumnResize } from "@/composables/useTableFeatures.js";
import {
  formatRiskLevelText,
  getRiskLevelClass6,
} from "@/utils/riskLevel";

// T6-14C-hook-1: 지속평가대상 관리 팝업(P2 소유 컴포넌트, 고정 계약 파일경로).
//   P2 산출물이 아직 없어도 빌드가 깨지지 않도록 import.meta.glob 로 지연 해석한다.
//   (glob 은 매칭이 없으면 에러 없이 빈 맵을 반환 → 런타임에 안내 처리.)
const continuousPopModules = import.meta.glob(
  "./popup/RiskContinuousImproveManage.vue"
);
const RiskContinuousImproveManage = defineAsyncComponent(
  () =>
    continuousPopModules["./popup/RiskContinuousImproveManage.vue"]?.() ??
    Promise.reject(new Error("RiskContinuousImproveManage 미존재"))
);

defineOptions({ name: "Risk_03" });
const props = defineProps({
  title: String,
  buttons: Object,
});

const localButtons = ref({ ...props.buttons });
const { open: openPop } = useModal();
const dashNav = useDashboardNavStore();


const riskAssessmentResultList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(riskAssessmentResultList);
// 기본 정렬: 평가요청일 내림차순(최신 항목이 가장 위로)
sortKey.value = "initAssessDate";
sortOrder.value = "desc";
const { colWidths, onResize } = useColumnResize({
  processNm: 110,
  riskTypeNm: 110,
  initAssessDate: 110,
  hazardNm: 120,
  initAssessorNm: 110,
  assessmentStatusNm: 110,
  initRiskLv: 130,
  revalRiskLv: 130,
  initDesc: 160,
});
const systCodeArr = ref([]);
const baseCodeArr = ref([]);
const riskTypeArr = ref([]);
const SiteSearchPopOpen = ref(false);

/* 조회조건 변수 세팅 */
const assessmentStatus = ref();
const proccessCd = ref();
const riskTypeCd = ref("");
const revalDate = ref();
// 평가요청자(이름 부분일치) / 평가요청일(단일일 YYYY-MM-DD) 조회조건
const initAssessorNm = ref("");
const initAssessDate = ref("");

const sr_chkptNm = ref("");
const sr_useYn = ref("Y");
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");

/* UserInfoPop 파라미터 변수 */
const headChk = ref(false);

const siteDisabled = ref(false);

const { proxy } = getCurrentInstance();

const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

// ── 대시보드 조회조건 주입 (PRAFTA-DASHBOARD-T1) ──────────────
// 대시보드(Dashboard_01)에서 넘어온 조회조건이 있으면 반영한다 (없으면 no-op).
// 본 화면은 월/기간 조회조건이 없어 사업장만 주입한다 (T1 확정). 반영 여부를 반환한다.
const applyDashboardParams = () => {
  const p = dashNav.consumeParams("Risk_03");
  if (!p) return false;
  siteCd.value = p.siteCd ?? "";
  siteNo.value = p.siteNo ?? "";
  siteNm.value = p.siteNm ?? "";
  return true;
};

onMounted(async () => {
  fnInit();
  // 대시보드 경유 진입 시 사업장 덮어쓰기 — 아래 fnSearch 가 반영하므로 이중 조회 없음
  applyDashboardParams();
  fnButtonControll();
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  await fnGetriskTypeResultList();
  await fnSearch();
});

// keep-alive 로 이미 열린 탭에 재진입하는 경우 대응
onActivated(() => {
  if (applyDashboardParams()) fnSearch();
});

useFieldWatcher(
  riskAssessmentResultList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

watch(proccessCd, (newVal) => {
  riskTypeCd.value = "";
});

// focusKill 이벤트
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

// API 호출
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS011"],
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

      assessmentStatus.value = systCodeArr.value.SYS011[0].systValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

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
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      baseCodeArr.value = grouped;

      proccessCd.value = baseCodeArr.value.COM002[0].baimValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnGetriskTypeResultList = async () => {
  try {
    const response = await axios.get("/webApi/risk03/risk-type-info-lists", {});

    if (response.status === 200) {
      riskTypeArr.value = response.data.riskTypeResultList;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  riskAssessmentResultList.value = [];

  try {
    const response = await axios.get("/webApi/risk03/risk-assessment-lists", {
      params: {
        siteCd: siteCd.value,
        assessmentStatus: assessmentStatus.value,
        processCd: proccessCd.value,
        riskTypeCd: riskTypeCd.value,
        initAssessorNm: initAssessorNm.value,
        initAssessDate: initAssessDate.value,
      },
    });

    if (response.status === 200) {
      console.log(response.data);
      riskAssessmentResultList.value = response.data.riskAssessmentResultList;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnDataValidationChk = (filteredData) => {
  let alertMsg = "";
  let retVal = true;

  for (var i = 0; i < filteredData.length; i++) {
    if (proxy.$util.isEmpty(filteredData[i].siteCd)) {
      alertMsg = "사업장은 필수 입력 값 입니다.";

      fnAlertMsg(alertMsg);
      retVal = false;
    } else if (proxy.$util.isEmpty(filteredData[i].chkLstType)) {
      alertMsg = "점검구분은 필수 입력 값 입니다.";

      fnAlertMsg(alertMsg);
      retVal = false;
    } else if (proxy.$util.isEmpty(filteredData[i].chkptNm)) {
      alertMsg = "점검대상명칭은 필수 입력 값 입니다.";

      fnAlertMsg(alertMsg);
      retVal = false;
    } else if (proxy.$util.isEmpty(filteredData[i].mgmtUserId)) {
      alertMsg = "관리자는 필수 입력 값 입니다.";

      fnAlertMsg(alertMsg);
      retVal = false;
    }

    if (!retVal) {
      return retVal;
    }
  }

  return retVal;
};

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
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

/* fnCallback */
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
        //        handleResetSiteSearchPop();
        fnSiteSearchPopOpen("searchForm");
        SiteSearchPopOpen.value = true;
      } else {
        siteCd.value = "";
        siteNo.value = "";
        siteNm.value = "";
      }
    }
  }
};

/* user function */
const fnButtonControll = () => {
  // localButtons.value.search = "N";
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  riskAssessmentResultList.value.forEach((item) => {
    item.chk = headChk.value;
  });
};

const fnSiteSearchPopOpen = (callPoint) => {
  if (callPoint == "searchForm") {
    openPop(SiteSearchPop, {
      cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo_p: "",
      siteNm_p: "",
      onSelect: onSiteSelected,
    });
  }
};

// T6-14C-hook-1: 지속개선대상(005) 행은 위험성평가 정보팝업이 아니라
//   "지속평가대상 관리" 팝업(P2 소유)을 연다. 그 외 상태는 기존 정보팝업 유지.
const fnOpenRow = (risk) => {
  if (risk.assessmentStatus === "005") {
    openPop(RiskContinuousImproveManage, {
      riskAssessmentData: buildRiskAssessmentData(risk),
      onSaved: () => fnSearch(),
      onCompleted: () => fnSearch(),
    });
    return;
  }
  fnOpenRiskAssessInfo(risk);
};

// 행 → RiskAssessInfo/지속관리 팝업 공통 데이터 매핑(중복 제거)
const buildRiskAssessmentData = (risk) => ({
  cmpnyCd: risk.cmpnyCd || "",
  siteCd: risk.siteCd || "",
  processCd: risk.processCd || "",
  processNm: risk.processNm || "",
  riskTypeCd: risk.riskTypeCd || "",
  riskTypeNm: risk.riskTypeNm || "",
  hazardCd: risk.hazardCd || "",
  hazardNm: risk.hazardNm || "",
  assessmentCd: risk.assessmentCd || "",
  assessmentStatus: risk.assessmentStatus || "",
  assessmentStatusNm: risk.assessmentStatusNm || "",
  initLikelihoodScore: risk.initLikelihoodScore || "",
  initSeverityScore: risk.initSeverityScore || "",
  initRiskLv: risk.initRiskLv || "",
  initDesc: risk.initDesc || "",
  initAssessorId: risk.initAssessorId || "",
  initAssessorNm: risk.initAssessorNm || "",
  initAssessDate: risk.initAssessDate || "",
  initFileMgmtCd: risk.initFileMgmtCd || "",
  initFilePath: risk.initFilePath || "",
  revalDate: risk.revalDate || "",
  revalBeforeDesc: risk.revalBeforeDesc || "",
  revalLikelihoodScore: proxy.$util.isEmpty(risk.revalLikelihoodScore)
    ? risk.initLikelihoodScore
    : risk.revalLikelihoodScore || "",
  revalSeverityScore: proxy.$util.isEmpty(risk.revalSeverityScore)
    ? risk.initSeverityScore
    : risk.revalSeverityScore || "",
  revalRiskLv: risk.revalRiskLv || "",
  revalDesc: risk.revalDesc || "",
  revalAssessorId: risk.revalAssessorId || "",
  revalAssessorNm: risk.revalAssessorNm || "",
  revalAssessDate: risk.revalAssessDate || "",
  revalFileMgmtCd: risk.revalFileMgmtCd || "",
  revalFilePath: risk.revalFilePath || "",
});

const fnOpenRiskAssessInfo = (risk) => {
  console.log(risk);

  openPop(RiskAssessInfo, {
    riskAssessmentData: {
      cmpnyCd: risk.cmpnyCd || "",
      siteCd: risk.siteCd || "",
      processCd: risk.processCd || "",
      processNm: risk.processNm || "",
      riskTypeCd: risk.riskTypeCd || "",
      riskTypeNm: risk.riskTypeNm || "",
      hazardCd: risk.hazardCd || "",
      hazardNm: risk.hazardNm || "",
      assessmentCd: risk.assessmentCd || "",
      assessmentStatus: risk.assessmentStatus || "",
      assessmentStatusNm: risk.assessmentStatusNm || "",
      initLikelihoodScore: risk.initLikelihoodScore || "",
      initSeverityScore: risk.initSeverityScore || "",
      initRiskLv: risk.initRiskLv || "",
      initDesc: risk.initDesc || "",
      initAssessorId: risk.initAssessorId || "",
      initAssessorNm: risk.initAssessorNm || "",
      initAssessDate: risk.initAssessDate || "",
      initFileMgmtCd: risk.initFileMgmtCd || "",
      initFilePath: risk.initFilePath || "",
      revalDate: risk.revalDate || "",
      revalBeforeDesc: risk.revalBeforeDesc || "",
      revalLikelihoodScore: proxy.$util.isEmpty(risk.revalLikelihoodScore)
        ? risk.initLikelihoodScore
        : risk.revalLikelihoodScore || "",
      revalSeverityScore: proxy.$util.isEmpty(risk.revalSeverityScore)
        ? risk.initSeverityScore
        : risk.revalSeverityScore || "",
      revalRiskLv: risk.revalRiskLv || "",
      revalDesc: risk.revalDesc || "",
      revalAssessorId: risk.revalAssessorId || "",
      revalAssessorNm: risk.revalAssessorNm || "",
      revalAssessDate: risk.revalAssessDate || "",
      revalFileMgmtCd: risk.revalFileMgmtCd || "",
      revalFilePath: risk.revalFilePath || "",
      revalAssessDate: risk.revalAssessDate || "",
    },
    onSave: (data) => {
      // 저장 후 처리 로직
      console.log("Saved data:", data);
      fnSearch(); // 목록 새로고침
    },
  });
};

const fnAlertMsg = async (message, afterConfirmCallback) => {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
};
</script>

<style scoped>
/* 조회조건이 여러 행으로 줄바꿈될 때 각 행의 왼쪽 끝선을 첫 항목과 맞춘다.
   (전역 form.css는 첫 항목에만 margin-left를 줘서 두 번째 행이 좌측으로 밀린다.)
   Attd_14 패턴 차용. */
.viewSearch {
  padding-left: calc(0.5rem + var(--space-md, 0.75rem));
  /* 행 간 간격 축소(열 간격 2rem은 유지). 전역 2rem은 과하고 0.5rem은 좁아 중간값 사용 */
  row-gap: 1rem;
}
.viewSearch > div:first-child {
  margin-left: 0;
}

/* 평가요청일 초기화(✕) 버튼. 전역 search-btn 크기에 맞춘 중립 톤 버튼 */
.date-clear-btn {
  width: 26px;
  min-width: 26px;
  height: var(--btn-height-sm, 26px);
  min-height: var(--btn-height-sm, 26px);
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  color: var(--color-text-muted);
  font-size: 0.75rem;
  line-height: 1;
  cursor: pointer;
}
.date-clear-btn:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  color: var(--color-text-strong);
}
.date-clear-btn:disabled {
  opacity: 0.4;
  cursor: default;
}
.date-clear-btn:focus-visible {
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

/* 위험도 등급 색 칩(6단계). Risk_02 관리기준표 팔레트와 동일. */
.risk-very-high,
.risk-high,
.risk-slightly-high,
.risk-normal,
.risk-low,
.risk-very-low {
  display: inline-block;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  font-weight: 600;
  font-size: 0.8rem;
  white-space: nowrap;
}
.risk-very-high {
  background: #ff4444;
  color: #fff;
}
.risk-high {
  background: #ff8800;
  color: #fff;
}
.risk-slightly-high {
  background: #ffaa00;
  color: #1f1e1e;
}
.risk-normal {
  background: #ffd700;
  color: #1f1e1e;
}
.risk-low {
  background: #90ee90;
  color: #1f1e1e;
}
.risk-very-low {
  background: #228b22;
  color: #fff;
}
</style>
