<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 검색바(1행): 사업장 / 소속부서 / 하위부서 조회
         (2행): 사용자명 / 전화번호 / 슬롯 점유일시(from ~ to) -->
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
          @click="fnSiteSearchPopOpen()"
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
        <label>소속부서</label>
        <input
          id="nodeCd"
          type="text"
          v-model="nodeCd"
          placeholder="부서코드"
          :disabled="nodeDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="nodeDisabled"
          @click="fnSiteNodeSearchPopOpenForCondition()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="nodeNm"
          type="text"
          v-model="nodeNm"
          placeholder="부서명"
          :disabled="nodeDisabled"
          @blur="focusKill"
        />
      </div>

      <div>
        <label class="checkbox-label">
          <input
            type="checkbox"
            v-model="incSubNodeYn"
            :disabled="nodeDisabled"
          />
          하위부서 조회
        </label>
      </div>

      <div>
        <label>사용자명</label>
        <input
          v-model.trim="userNm"
          type="text"
          placeholder="사용자명"
          style="width: 160px"
          @keyup.enter="fnSearch"
        />
      </div>

      <div>
        <label>전화번호</label>
        <input
          v-model.trim="mblNo"
          type="text"
          placeholder="휴대폰번호"
          style="width: 160px"
          @keyup.enter="fnSearch"
        />
      </div>

      <div>
        <label>슬롯 점유일시</label>
        <CalendarSrch v-model="occupyFrom" />
        <span class="date-range-sep">~</span>
        <CalendarSrch v-model="occupyTo" />
      </div>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">일일사용자 리스트</span>
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
                <th class="event_cell" style="text-align: center; width: 4%">
                  No
                </th>
                <ThSortable
                  label="사용자명"
                  col-key="userNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.userNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="휴대폰번호"
                  col-key="mblNo"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.mblNo"
                  @sort="onSort"
                  @update:width="onResize"
                />
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
                  label="소속부서"
                  col-key="nodeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.nodeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="슬롯번호"
                  col-key="slotNo"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.slotNo"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="슬롯 점유일시"
                  col-key="occupyDtime"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.occupyDtime"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="슬롯 점유해제 일시"
                  col-key="releaseDtime"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.releaseDtime"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <!-- 계정별 QR (Baim_05 QRCODE 컬럼 미러 — 점유 중 행만 노출) -->
                <th style="text-align: center; width: 90px">QRCODE</th>
                <!-- 계약서 서명/입장 이력 팝업 -->
                <th style="text-align: center; width: 90px">계약이력</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!dailyUserList || dailyUserList.length === 0">
                <tr>
                  <td colspan="10" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in sortedData" :key="row.hisId">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.userNm }}</td>
                  <td>{{ row.mblNo }}</td>
                  <td>{{ row.siteNm }}</td>
                  <td>{{ row.nodeNm }}</td>
                  <td style="text-align: center">{{ row.slotNo }}</td>
                  <td>{{ row.occupyDtime }}</td>
                  <td>{{ row.releaseDtime ?? "점유 중" }}</td>
                  <!-- QR: 점유 중(해제 전) + 계정 존재 행만. 해제/만료 계정 QR 은 무효라 미노출. -->
                  <td style="text-align: center">
                    <button
                      v-if="row.userCd && !row.releaseDtime"
                      class="btn btn-sm btn-primary"
                      @click="fnQrCodePopOpen(row)"
                    >
                      QRCODE
                    </button>
                    <span v-else>-</span>
                  </td>
                  <!-- 계약이력: 계정 존재 행이면 항상(만료 계정도 서명본 3년 보존 — 조회 가능). -->
                  <td style="text-align: center">
                    <button
                      v-if="row.userCd"
                      class="btn btn-sm btn-primary"
                      @click="fnContractHistPopOpen(row)"
                    >
                      이력
                    </button>
                    <span v-else>-</span>
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
} from "vue";
import { useModal } from "@/utils/useModal";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import QrCodePop from "@/components/popup/QrCodePop.vue";
import DailyContractHistPop from "./popup/DailyContractHistPop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

// =========================== Define ===========================
defineOptions({ name: "User_05" });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const dailyUserList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(dailyUserList);
const { colWidths, onResize } = useColumnResize({
  userNm: 110,
  mblNo: 140,
  siteNm: 150,
  nodeNm: 140,
  slotNo: 90,
  occupyDtime: 170,
  releaseDtime: 170,
});

const SiteSearchPopOpen = ref(false);

// 조회조건
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
const userNm = ref("");
const mblNo = ref("");
const occupyFrom = ref("");
const occupyTo = ref("");

const siteDisabled = ref(false);

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// =========================== Life Cycle ===========================
const getSession = (key) => {
  const v = sessionStorage.getItem(key);
  return v && v !== "null" && v !== "undefined" ? v : "";
};

const fnInit = () => {
  siteCd.value = getSession("gv_siteCd");
  siteNo.value = getSession("gv_siteNo");
  siteNm.value = getSession("gv_siteNm");
  if (siteCd.value) {
    nodeDisabled.value = false;
    nodeCd.value = getSession("gv_nodeCd");
    nodeNm.value = getSession("gv_nodeNm");
  }
};

const fnButtonControll = () => {
  // 조회 전용 화면 — 생성/저장/삭제/엑셀 비활성.
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

onMounted(async () => {
  fnButtonControll();
  fnInit();
  await fnSearch();
});

// =========================== Methods ===========================
const fnSearch = async () => {
  dailyUserList.value = [];

  try {
    const response = await axios.get("/webApi/user05/daily-user-lists", {
      params: {
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        userNm: userNm.value,
        mblNo: mblNo.value,
        occupyFrom: occupyFrom.value,
        occupyTo: occupyTo.value,
      },
    });

    if (response.status === 200) {
      dailyUserList.value = response.data?.dailyUserList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
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

const focusKill = (e) => {
  if (e.target.id == "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNm.value = "";
      siteFocusKill();
    }
  } else if (e.target.id == "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNo.value = "";
      siteFocusKill();
    }
  } else if (e.target.id == "nodeCd") {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeCd.value = "";
      nodeNm.value = "";
      return;
    } else {
      nodeNm.value = "";
      nodeFocusKill();
    }
  } else if (e.target.id == "nodeNm") {
    if (proxy.$util.isEmpty(nodeNm.value)) {
      nodeCd.value = "";
      nodeNm.value = "";
      return;
    } else {
      nodeCd.value = "";
      nodeFocusKill();
    }
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
        nodeDisabled.value = false;
        // 사업장 변경 시 소속부서 초기화(부서는 사업장 종속 — 팝업 선택 경로와 정합)
        nodeCd.value = "";
        nodeNm.value = "";
      } else if (siteList.length > 1) {
        fnSiteSearchPopOpen();
        SiteSearchPopOpen.value = true;
      } else {
        siteCd.value = "";
        siteNo.value = "";
        siteNm.value = "";
        nodeDisabled.value = true;
        nodeCd.value = "";
        nodeNm.value = "";
      }
    } else if (apiId == "site-node-lists") {
      const list = res.data?.siteNodeInfoList || [];
      if (list.length === 0) {
        nodeCd.value = "";
        nodeNm.value = "";
      } else if (list.length === 1) {
        nodeCd.value = list[0].nodeCd ?? "";
        nodeNm.value = list[0].nodeNm ?? "";
      } else {
        fnSiteNodeSearchPopOpenForCondition();
      }
    }
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  nodeDisabled.value = false;
  nodeCd.value = "";
  nodeNm.value = "";
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

const fnSrchNodeInfo = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) return;
  try {
    const response = await axios.get("/comApi/baseinfo/site-node-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        nodeNm: nodeNm.value,
      },
    });
    if (response.status === 200) {
      fnCallback({ ...response, config: { url: "/dummy/site-node-lists" } });
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const nodeFocusKill = async () => {
  await fnSrchNodeInfo();
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnSiteNodeSearchPopOpenForCondition = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userId_p: "",
    onSelect: onSiteNodeSelectedForCondition,
  });
};

const onSiteNodeSelectedForCondition = (nodeCdVal, nodeNmVal) => {
  nodeCd.value = nodeCdVal ?? "";
  nodeNm.value = nodeNmVal ?? "";
};

// 계정 QR 팝업 — Baim_05 fnSlotQrCodePopOpen 미러(출퇴근/식별용 JSON + 수동 입력용 코드값).
const fnQrCodePopOpen = (row) => {
  openPop(QrCodePop, {
    qrValue: JSON.stringify({
      cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      siteCd: row.siteCd,
      userCd: row.userCd,
      qrTitle: `${row.siteNm} - ${row.userNm} QR코드`,
    }),
    // 웹 수동 입력 보완: 스캐너 없는 웹 관리자가 코드값을 직접 입력할 수 있게 노출.
    displayCode: row.userCd,
  });
};

// 계약이력 팝업 — 서명 이력 + 입장 승인/로그인 이력(계약서 미등록 로그인 포함).
const fnContractHistPopOpen = (row) => {
  openPop(DailyContractHistPop, {
    userCd_p: row.userCd,
    userNm_p: row.userNm,
  });
};
</script>

<style scoped>
/* 조회조건이 여러 행으로 줄바꿈될 때 각 행의 왼쪽 끝선을 사업장과 맞춘다. (User_01 패턴) */
.viewSearch {
  padding-left: calc(0.5rem + var(--space-md, 0.75rem));
  row-gap: 0.5rem;
}
.viewSearch > div:first-child {
  margin-left: 0;
}

/* 하위부서 조회 체크박스 (User_01 checkbox-label 패턴 차용) */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  user-select: none;
  margin-left: -1rem;
  margin-right: 0.4rem;
  white-space: nowrap;
}

.checkbox-label input[type="checkbox"] {
  width: 13px;
  height: 13px;
  cursor: pointer;
  accent-color: var(--color-primary, #16a34a);
  flex-shrink: 0;
}

/* 슬롯 점유일시 from~to 구분자 */
.date-range-sep {
  margin: 0 0.4rem;
  color: var(--color-text-muted, #6b7280);
}
</style>
