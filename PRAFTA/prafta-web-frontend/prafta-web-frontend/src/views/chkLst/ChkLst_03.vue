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
          v-model="formData.siteNo"
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
          v-model="formData.siteNm"
          placeholder="사업장명"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
      </div>

      <div>
        <label>점검대상명칭</label>
        <input v-model.trim="formData.chkptNm" type="text" />
      </div>

      <div>
        <label>순회점검구분</label>
        <select v-model="formData.chkLstType" name="combo">
          <option
            v-for="opt in baseCodeArr['COM001'] || []"
            :key="opt.baimValDCd"
            :value="opt.baimValDCd"
          >
            {{ opt.baimValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>점검시행월</label>
        <CalendarSrchMonth
          :range="false"
          style="width: 100px"
          v-model="formData.fromDate"
        />
        -
        <CalendarSrchMonth
          :range="false"
          style="width: 100px"
          v-model="formData.toDate"
        />
      </div>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle-row">
          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <!-- 단순 마크 아이콘 (SVG) -->
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">점검결과 리스트</span>
          </div>
          <div class="custom-btn-area">
            <button
              class="btn btn-custom"
              @click="fnSelectedChkLstRstPopOpen()"
            >
              점검일지 조회
            </button>
          </div>
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
                <th style="width: 4%">
                  <input
                    id="headChk"
                    v-model="headChk"
                    type="checkbox"
                    @click="fnHeadChk"
                  />
                </th>
                <ThSortable
                  label="사업장"
                  col-key="siteNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.siteNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="점검대상명칭"
                  col-key="chkptNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.chkptNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="editableCell" style="width: 10%">점검구분</th>
                <ThSortable
                  label="점검시행행월"
                  col-key="workDate"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.workDate"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="관리자"
                  col-key="siteAdminNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.siteAdminNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="점검시행일수"
                  col-key="inspectDayCnt"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.inspectDayCnt"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="불량항목 수"
                  col-key="defectiveResultCnt"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.defectiveResultCnt"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="editableCell">점검결과확인</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(chkptResult, idx) in sortedData"
                :key="chkptResult.siteCd"
              >
                <td style="text-align: center">{{ idx + 1 }}</td>
                <td>
                  <input type="checkbox" v-model="chkptResult.chk" />
                </td>
                <td>{{ chkptResult.siteNm }}</td>
                <td>{{ chkptResult.chkptNm }}</td>
                <td>
                  <BaseSelect
                    v-model="chkptResult.chkLstType"
                    :readonly="true"
                    name="codeDetailSrc"
                  >
                    <option
                      v-for="opt in (baseCodeArr['COM001'] || []).filter(
                        (o) => o.baimValDCd != null
                      )"
                      :key="opt.baimValDCd"
                      :value="opt.baimValDCd"
                    >
                      {{ opt.baimValDNm }}
                    </option>
                  </BaseSelect>
                </td>
                <!-- <td>
                  {{ chkptResult.chkLstType }}
                </td> -->
                <td>{{ chkptResult.workDate }}</td>
                <td>{{ chkptResult.siteAdminNm }}</td>
                <td>{{ chkptResult.inspectDayCnt }}</td>
                <td>{{ chkptResult.defectiveResultCnt }}</td>
                <td>
                  <div class="flex items-center gap-2 w-full">
                    <button
                      class="btn btn-primary btn-sm"
                      @click="fnChkLstRstPopOpen(chkptResult)"
                    >
                      점검일지
                    </button>
                  </div>
                </td>
              </tr>
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
  reactive,
  defineProps,
  onMounted,
  onActivated,
  getCurrentInstance,
  defineOptions,
  nextTick,
  watch,
} from "vue";
import { useModal } from "@/utils/useModal";
import { useDashboardNavStore } from "@/stores/dashboardNavStore";
import { useFieldWatcher } from "@/utils/useFieldWatcher";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import BaseSelect from "@/components/common/BaseSelect.vue";
import CalendarSrchMonth from "@/components/common/CalendarSrchMonth.vue";
import ChkLstRstPop from "@/views/chkLst/popup/ChkLstRstPop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import { useTableSort, useColumnResize } from "@/composables/useTableFeatures.js";

// =========================== Define ===========================
defineOptions({ name: "ChkLst_03" });
const props = defineProps({
  buttons: Object,
  title: String,
});

// =========================== Reactive ===========================
const formData = reactive({
  fromDate: "",
  toDate: "",
  siteCd: "",
  siteNo: "",
  siteNm: "",
  chkLstType: "",
  chkptNm: "",
});

// =========================== Ref ===========================
const chkptResultList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(chkptResultList);
const { colWidths, onResize } = useColumnResize({
  siteNm: 130,
  chkptNm: 150,
  workDate: 110,
  siteAdminNm: 110,
  inspectDayCnt: 110,
  defectiveResultCnt: 110,
});
const baseCodeArr = ref([]);
const SiteSearchPopOpen = ref(false);
const headChk = ref(false);
const siteDisabled = ref(false);

// =========================== Data ===========================
const { open: openPop } = useModal();
const { proxy } = getCurrentInstance();
const dashNav = useDashboardNavStore();
const localButtons = ref({ ...props.buttons });
// 날짜 자동 조정 플래그 (무한 루프 방지)
let isAdjustingDate = false;

// =========================== Life Cycle ===========================
const fnInit = () => {
  formData.siteCd = sessionStorage.getItem("gv_siteCd") ?? "";
  formData.siteNo = sessionStorage.getItem("gv_siteNo") ?? "";
  formData.siteNm = sessionStorage.getItem("gv_siteNm") ?? "";
};

// ── 대시보드 조회조건 주입 (PRAFTA-DASHBOARD-T1) ──────────────
// 대시보드(Dashboard_01)에서 넘어온 조회조건이 있으면 반영한다 (없으면 no-op).
// 본 화면의 조회기간은 월 단위(YYYY-MM) — 기준월(ym) 단월로 설정한다. 반영 여부를 반환한다.
const applyDashboardParams = () => {
  const p = dashNav.consumeParams("ChkLst_03");
  if (!p) return false;
  formData.siteCd = p.siteCd ?? "";
  formData.siteNo = p.siteNo ?? "";
  formData.siteNm = p.siteNm ?? "";
  if (p.ym) {
    formData.fromDate = p.ym;
    formData.toDate = p.ym;
  }
  return true;
};

onMounted(async () => {
  fnInit();
  initializeFormData();
  fnButtonControll();
  await fnGetBaseinfoList();
  // 대시보드 경유 진입 시 기본값(당월/소속 사업장)을 덮어쓰고 재조회
  if (applyDashboardParams()) await fnSearch();
});

// keep-alive 로 이미 열린 탭에 재진입하는 경우 대응
onActivated(() => {
  if (applyDashboardParams()) fnSearch();
});

// =========================== Watch, Watcher ===========================
useFieldWatcher(
  chkptResultList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// fromDate 변경 감시 - toDate보다 클 경우 toDate를 fromDate의 한달 후로 세팅
watch(
  () => formData.fromDate,
  (newFromDate) => {
    if (isAdjustingDate || !newFromDate || !formData.toDate) return;

    const fromDateParsed = parseDate(newFromDate);
    const toDateParsed = parseDate(formData.toDate);

    if (!fromDateParsed || !toDateParsed) return;

    // fromDate가 toDate보다 클 경우, toDate를 fromDate의 한달 후로 세팅
    if (fromDateParsed > toDateParsed) {
      isAdjustingDate = true;
      const adjustedDate = new Date(
        fromDateParsed.getFullYear(),
        fromDateParsed.getMonth() + 1,
        1
      );
      formData.toDate = formatDate(adjustedDate);
      nextTick(() => {
        isAdjustingDate = false;
      });
    }
  }
);

// toDate 변경 감시 - fromDate보다 작을 경우 fromDate를 toDate의 한달 전으로 세팅
watch(
  () => formData.toDate,
  (newToDate) => {
    if (isAdjustingDate || !newToDate || !formData.fromDate) return;

    const fromDateParsed = parseDate(formData.fromDate);
    const toDateParsed = parseDate(newToDate);

    if (!fromDateParsed || !toDateParsed) return;

    // toDate가 fromDate보다 작을 경우, fromDate를 toDate의 한달 전으로 세팅
    if (toDateParsed < fromDateParsed) {
      isAdjustingDate = true;
      const adjustedDate = new Date(
        toDateParsed.getFullYear(),
        toDateParsed.getMonth() - 1,
        1
      );
      formData.fromDate = formatDate(adjustedDate);
      nextTick(() => {
        isAdjustingDate = false;
      });
    }
  }
);

// =========================== Methods ===========================
// 날짜 validation 관련 함수들
// yyyy-mm 형식의 날짜를 Date 객체로 변환
const parseDate = (dateStr) => {
  if (!dateStr) return null;
  const [year, month] = dateStr.split("-").map(Number);
  return new Date(year, month - 1, 1);
};

// Date 객체를 yyyy-mm 형식으로 변환
const formatDate = (date) => {
  if (!date) return "";
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
    2,
    "0"
  )}`;
};

// 날짜 validation 체크 (조회 전에 호출)
const validateDateRange = () => {
  if (!formData.fromDate || !formData.toDate) {
    proxy.$alert(getMessage(MSG.MONTH_RANGE_REQUIRED));
    return false;
  }

  const fromDateParsed = parseDate(formData.fromDate);
  const toDateParsed = parseDate(formData.toDate);

  if (!fromDateParsed || !toDateParsed) {
    proxy.$alert(getMessage(MSG.DATE_FORMAT_INVALID));
    return false;
  }

  // fromDate가 toDate보다 크면, fromDate를 toDate보다 1개월 전으로 설정
  if (fromDateParsed > toDateParsed) {
    const adjustedDate = new Date(
      toDateParsed.getFullYear(),
      toDateParsed.getMonth() - 1,
      1
    );
    formData.fromDate = formatDate(adjustedDate);
    proxy.$alert(getMessage(MSG.MONTH_ORDER_AUTO_ADJUSTED));
    return false; // 조정 후 다시 확인하도록 false 반환
  }

  return true;
};

// focusKill 이벤트
function focusKill(e) {
  if (e.target.id == "siteNo") {
    if (proxy.$util.isEmpty(formData.sr_siteNo)) {
      formData.siteCd = "";
      formData.siteNm = "";
    } else {
      formData.siteNm = "";
      siteFocusKill();
    }
  } else if (e.target.id == "siteNm") {
    if (proxy.$util.isEmpty(formData.siteNm)) {
      formData.siteCd = "";
      formData.siteNo = "";
    } else {
      formData.siteNo = "";
      siteFocusKill();
    }
  }
}

const initializeFormData = () => {
  const now = new Date();
  const currentMonth = `${now.getFullYear()}-${String(
    now.getMonth() + 1
  ).padStart(2, "0")}`;

  // PRAFTA_COM_001-T5-11.2.1: 기본값은 현재월만 선택 (fromDate = toDate = 당월)
  formData.toDate = currentMonth;
  formData.fromDate = currentMonth;
};

const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM001"],
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

      formData.chkLstType = baseCodeArr.value.COM001[0].baimValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  // 날짜 validation 체크
  if (!validateDateRange()) {
    return; // validation 실패 시 조회 중단
  }

  const param = {
    fromDate: formData.fromDate.split("T")[0].replaceAll("-", ""),
    toDate: formData.toDate.split("T")[0].replaceAll("-", ""),
    siteCd: formData.siteCd,
    chkptNm: formData.chkptNm,
    chkLstType: formData.chkLstType,
  };

  try {
    const response = await axios.get("/webApi/chkLst03/inspect-results", {
      params: param,
    });

    if (response.status === 200) {
      chkptResultList.value = response.data?.inspectResult;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

function fnDataValidationChk(filteredData) {
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
}

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: formData.siteNo,
        siteNm: formData.siteNm,
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
        formData.siteCd = siteList[0].siteCd;
        formData.siteNo = siteList[0].siteNo;
        formData.siteNm = siteList[0].siteNm;
      } else if (siteList.length > 1) {
        //        handleResetSiteSearchPop();
        fnSiteSearchPopOpen("searchForm");
        SiteSearchPopOpen.value = true;
      } else {
        formData.siteCd = "";
        formData.siteNo = "";
        formData.siteNm = "";
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
}

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  formData.siteCd = siteCdVal;
  formData.siteNo = siteNoVal;
  formData.siteNm = siteNmVal;
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  chkptResultList.value.forEach((item) => {
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
  } else {
    openPop(SiteSearchPop, {
      cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo_p: "",
      siteNm_p: "",
      onSelect: (siteCdVal, siteNoVal, siteNmVal) => {
        chkptList.value[callPoint].siteCd = siteCdVal;
        chkptList.value[callPoint].siteNm = siteNmVal;
      },
    });
  }
};

// 날짜 범위 생성 함수 (fromDate ~ toDate 사이의 모든 월)
const generateDateRange = (fromDate, toDate) => {
  const dates = [];

  if (!fromDate || !toDate) return dates;

  // yyyy-mm 형식을 파싱
  const [fromYear, fromMonth] = fromDate.split("-").map(Number);
  const [toYear, toMonth] = toDate.split("-").map(Number);

  let currentYear = fromYear;
  let currentMonth = fromMonth;

  // 시작 월부터 종료 월까지 반복
  while (
    currentYear < toYear ||
    (currentYear === toYear && currentMonth <= toMonth)
  ) {
    // yyyyMM 형식으로 변환 (예: 202411)
    const workDate = `${currentYear}${String(currentMonth).padStart(2, "0")}`;
    dates.push(workDate);

    // 다음 월로 이동
    currentMonth++;
    if (currentMonth > 12) {
      currentMonth = 1;
      currentYear++;
    }
  }

  return dates;
};

const fnChkLstRstPopOpen = (chkptResult) => {
  // fromDate와 toDate 사이의 모든 월에 대해 workDate 생성
  const workMonths = generateDateRange(formData.fromDate, formData.toDate);
  const baseCode = baseCodeArr.value["COM001"].filter(
    (item) => item.baimValDCd == chkptResult.chkLstType
  );
  let chkLstTypeNm = "";

  if (proxy.$util.isNotEmpty(baseCode) && baseCode.length == 1) {
    chkLstTypeNm = baseCode[0]?.baimValDNm || "";
  }

  // 각 workDate마다 chkptInfo 항목 생성
  const chkptInfo = workMonths.map((workMonth) => ({
    siteCd: chkptResult.siteCd || "",
    siteNm: chkptResult.siteNm || "",
    workMonth: workMonth,
    chkptCd: chkptResult.chkptCd || "",
    chkptNm: chkptResult.chkptNm || "",
    chkLstType: chkptResult.chkLstType || "",
    chkLstTypeNm: chkLstTypeNm || "",
    siteAdminNm: chkptResult.siteAdminNm || "",
    chkptDesc: chkptResult.chkptDesc || "",
  }));

  openPop(ChkLstRstPop, {
    cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
    chkptInfo: chkptInfo,
  });
};

// 점검일지 조회 버튼 클릭 시 - 체크된 항목들만 모아서 팝업 열기
const fnSelectedChkLstRstPopOpen = () => {
  // chk가 true인 항목들만 필터링
  const selectedItems = chkptResultList.value.filter(
    (item) => item.chk === true
  );

  if (selectedItems.length === 0) {
    proxy.$alert(getMessage(MSG.SEARCH_ITEM_REQUIRED));
    return;
  }

  // fromDate와 toDate 사이의 모든 월에 대해 workDate 생성
  const workMonths = generateDateRange(formData.fromDate, formData.toDate);

  // chkptInfo 배열 생성 (각 체크된 항목 × 각 월의 조합)
  const chkptInfo = [];

  selectedItems.forEach((chkptResult) => {
    // chkLstTypeNm 찾기
    const baseCode =
      baseCodeArr.value["COM001"]?.filter(
        (item) => item.baimValDCd == chkptResult.chkLstType
      ) || [];
    let chkLstTypeNm = "";

    if (proxy.$util.isNotEmpty(baseCode) && baseCode.length == 1) {
      chkLstTypeNm = baseCode[0]?.baimValDNm || "";
    }

    // 각 workMonth마다 chkptInfo 항목 생성
    workMonths.forEach((workMonth) => {
      chkptInfo.push({
        siteCd: chkptResult.siteCd || "",
        siteNm: chkptResult.siteNm || "",
        workMonth: workMonth,
        chkptCd: chkptResult.chkptCd || "",
        chkptNm: chkptResult.chkptNm || "",
        chkLstType: chkptResult.chkLstType || "",
        chkLstTypeNm: chkLstTypeNm || "",
        siteAdminNm: chkptResult.siteAdminNm || "",
        chkptDesc: chkptResult.chkptDesc || "",
      });
    });
  });

  openPop(ChkLstRstPop, {
    cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
    chkptInfo: chkptInfo,
  });
};

const fnAlertMsg = async (message, afterConfirmCallback) => {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
};
</script>

<style scoped></style>
