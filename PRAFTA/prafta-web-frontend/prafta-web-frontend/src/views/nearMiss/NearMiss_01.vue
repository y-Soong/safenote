<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

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
        <label>잠재중대성</label>
        <select v-model="potentialSeverityCd" name="combo">
          <option value="">전체</option>
          <option
            v-for="opt in systCodeArr['SYS062'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>발생기간</label>
        <CalendarSrch v-model="startDate" />
        <span class="date-range-sep">~</span>
        <CalendarSrch v-model="endDate" />
      </div>
    </div>

    <!-- 상태 탭 (SYS063 재번호: 100 접수 / 200 조치중 / 300 완료 / 400 미처리대상) -->
    <div class="status-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.code"
        class="status-tab"
        :class="{ 'status-tab--active': reportStatusCd === tab.code }"
        @click="fnSelectStatusTab(tab.code)"
      >
        <span class="status-tab__label">{{ tab.label }}</span>
        <span class="status-tab__count">{{ statusCounts[tab.code] ?? 0 }}</span>
      </button>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">사건 리스트</span>
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
                <ThSortable
                  label="사건ID"
                  col-key="nearMissId"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.nearMissId"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="잠재중대성"
                  col-key="potentialSeverityNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.potentialSeverityNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="공정"
                  col-key="processNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.processNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="발생장소"
                  col-key="locationDesc"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.locationDesc"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="경위"
                  col-key="description"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.description"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="즉시조치"
                  col-key="immediateActionDesc"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.immediateActionDesc"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="보고자"
                  col-key="reporterNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.reporterNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="발생일시"
                  col-key="occurDtime"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.occurDtime"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="상태"
                  col-key="reportStatusNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.reportStatusNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
              </tr>
            </thead>
            <tbody>
              <template
                v-if="!incidentResultList || incidentResultList.length === 0"
              >
                <tr>
                  <td colspan="10" class="edu-grid-empty">
                    등록된 사건이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(item, idx) in sortedData"
                  :key="item.nearMissId"
                  @dblclick="fnOpenNearMissInfo(item)"
                  style="cursor: pointer"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ item.nearMissId }}</td>
                  <td>
                    <span
                      class="severity-badge"
                      :class="fnSeverityClass(item.potentialSeverityCd)"
                    >
                      {{ item.potentialSeverityNm || "-" }}
                    </span>
                  </td>
                  <td>{{ item.processNm }}</td>
                  <!-- 긴 텍스트 3종: 고정폭 + 말줄임, 전체 내용은 툴팁으로 -->
                  <td class="cell-ellipsis" :title="item.locationDesc">
                    {{ item.locationDesc || "-" }}
                  </td>
                  <td class="cell-ellipsis" :title="item.description">
                    {{ item.description || "-" }}
                  </td>
                  <td class="cell-ellipsis" :title="item.immediateActionDesc">
                    {{ item.immediateActionDesc || "-" }}
                  </td>
                  <td>{{ item.reporterNm }}</td>
                  <td>{{ item.occurDtime }}</td>
                  <td>{{ item.reportStatusNm }}</td>
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
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import NearMissInfo from "./popup/NearMissInfo.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import { useTableSort, useColumnResize } from "@/composables/useTableFeatures.js";

defineOptions({ name: "NearMiss_01" });
const props = defineProps({
  title: String,
  buttons: Object,
});

const localButtons = ref({ ...props.buttons });
const { open: openPop } = useModal();
const { proxy } = getCurrentInstance();

// 목록/정렬 상태
const incidentResultList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } =
  useTableSort(incidentResultList);
const { colWidths, onResize } = useColumnResize({
  nearMissId: 150,
  potentialSeverityNm: 110,
  processNm: 110,
  locationDesc: 150,
  description: 220,
  immediateActionDesc: 220,
  reporterNm: 100,
  occurDtime: 140,
  reportStatusNm: 90,
});

// 코드/조회조건
const systCodeArr = ref([]);
const potentialSeverityCd = ref("");
// 발생기간 기본값: 당해년도 1/1 ~ 12/31 (CalendarSrch 모델은 YYYY-MM-DD 문자열)
const thisYear = new Date().getFullYear();
const startDate = ref(`${thisYear}-01-01`);
const endDate = ref(`${thisYear}-12-31`);
const reportStatusCd = ref(""); // "" = 전체

// 상태 탭 (SYS063 재번호: 100 접수 / 200 조치중 / 300 완료 / 400 미처리대상).
// 라벨은 SYS063 매핑이 로드되면 갱신 가능; 골격은 고정 라벨.
const statusTabs = ref([
  { code: "100", label: "접수" },
  { code: "200", label: "조치중" },
  { code: "300", label: "완료" },
  { code: "400", label: "미처리대상" },
]);
const statusCounts = ref({});

// 사업장
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);

const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

onMounted(async () => {
  fnInit();
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

// 검색 버튼만 사용 (저장/완결은 상세 팝업에서 처리)
const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

// 상태 탭 선택 → 재조회
const fnSelectStatusTab = (code) => {
  reportStatusCd.value = reportStatusCd.value === code ? "" : code;
  fnSearch();
};

// 잠재중대성 배지 클래스 (UI 표현만)
const fnSeverityClass = (code) => {
  if (code === "300") return "severity-badge--critical";
  if (code === "200") return "severity-badge--major";
  if (code === "100") return "severity-badge--minor";
  return "";
};

// focusKill (Risk_03 동일 패턴 — 사업장 코드/명 상호 클리어)
const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
    } else {
      siteNm.value = "";
      siteFocusKill();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
    } else {
      siteNo.value = "";
      siteFocusKill();
    }
  }
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

// 사업장 단건 조회(Risk_03 동일 패턴) — 입력한 코드/명으로 사업장 확정
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

// 사업장 단건 조회 콜백 — 1건이면 확정, 다건이면 팝업, 없으면 클리어
const fnCallback = (res) => {
  if (proxy.$util.isNotEmpty(res)) {
    const apiId = res.config.url.split("/").pop();
    if (apiId === "site-lists") {
      const siteList = res.data?.siteInfoResultList ?? [];
      if (siteList.length === 1) {
        siteCd.value = siteList[0].siteCd;
        siteNo.value = siteList[0].siteNo;
        siteNm.value = siteList[0].siteNm;
      } else if (siteList.length > 1) {
        fnSiteSearchPopOpen("searchForm");
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

const fnSiteSearchPopOpen = (callPoint) => {
  if (callPoint === "searchForm") {
    openPop(SiteSearchPop, {
      cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo_p: "",
      siteNm_p: "",
      onSelect: onSiteSelected,
    });
  }
};

const fnOpenNearMissInfo = (item) => {
  openPop(NearMissInfo, {
    nearMissData: {
      cmpnyCd: item.cmpnyCd || "",
      siteCd: item.siteCd || "",
      nearMissId: item.nearMissId || "",
    },
    onSave: () => {
      fnSearch(); // 저장/상태전환 후 목록 새로고침
    },
  });
};

// 코드(SYS062 잠재중대성 / SYS063 처리상태) 조회 + systValCd 기준 그룹핑
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS062", "SYS063"],
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

      // 상태탭 라벨을 SYS063 코드명으로 갱신(코드 누락 시 골격 고정 라벨 유지)
      const statusCodes = grouped["SYS063"] || [];
      if (statusCodes.length > 0) {
        statusTabs.value = statusTabs.value.map((tab) => {
          const matched = statusCodes.find((c) => c.systValDCd === tab.code);
          return matched ? { ...tab, label: matched.systValDNm } : tab;
        });
      }
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 조회조건 공통 파라미터(식별자 cmpnyCd/userCd는 서버 JWT에서 도출 → 전송하지 않음)
const fnBuildSearchParams = () => ({
  siteCd: siteCd.value,
  potentialSeverityCd: potentialSeverityCd.value,
  startDate: startDate.value,
  endDate: endDate.value,
});

// 상태별 카운트 조회(상태탭 배지). 상태 필터는 카운트에 적용하지 않음
const fnGetStatusCounts = async () => {
  try {
    const response = await axios.get("/webApi/nearmiss01/status-counts", {
      params: fnBuildSearchParams(),
    });

    if (response.status === 200) {
      const cnt = response.data?.statusCount || {};
      // SYS063 재번호: 100 접수 / 200 조치중 / 300 완료 / 400 미처리대상
      statusCounts.value = {
        "100": cnt.receivedCnt ?? 0,
        "200": cnt.actingCnt ?? 0,
        "300": cnt.completedCnt ?? 0,
        "400": cnt.unaddressedCnt ?? 0,
      };
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 사건 목록 + 상태 카운트 조회
const fnSearch = async () => {
  incidentResultList.value = [];

  try {
    const response = await axios.get("/webApi/nearmiss01/incident-lists", {
      params: {
        ...fnBuildSearchParams(),
        reportStatusCd: reportStatusCd.value,
      },
    });

    if (response.status === 200) {
      incidentResultList.value = response.data?.incidentResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }

  await fnGetStatusCounts();
};
</script>

<style scoped>
.date-range-sep {
  margin: 0 var(--space-xs, 0.25rem);
}

/* 긴 텍스트 컬럼(발생장소/경위/즉시조치): 고정폭 안에서 한 줄 말줄임(...) */
.cell-ellipsis {
  max-width: 0; /* table-fixed 에서 컬럼폭을 넘지 않도록 강제 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 상태 탭 — 왼쪽은 아래 테이블(.viewBody 좌측 padding)과 동일 들여쓰기로 맞추고,
   아래쪽은 padding 을 비워 "사건 리스트" 소제목과의 간격이 위 조회 영역과의 간격과 비슷해지도록 함 */
.status-tabs {
  display: flex;
  gap: var(--space-sm, 0.5rem);
  padding: var(--space-sm, 0.5rem) 0 0 var(--space-lg, 1rem);
}

.status-tab {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 0.375rem);
  padding: var(--space-xs, 0.375rem) var(--space-md, 0.75rem);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-md, 6px);
  background: var(--color-surface, #ffffff);
  color: var(--color-text, #374151);
  font-size: var(--font-size-sm, 0.875rem);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.status-tab:hover {
  background: var(--color-surface-hover, #f9fafb);
}

.status-tab--active {
  border-color: var(--color-primary, #16a34a);
  color: var(--color-primary, #16a34a);
  font-weight: 600;
}

.status-tab__count {
  min-width: 1.25rem;
  padding: 0 var(--space-xs, 0.25rem);
  border-radius: var(--radius-pill, 999px);
  background: var(--color-surface-muted, #f3f4f6);
  text-align: center;
  font-size: var(--font-size-xs, 0.75rem);
}

.status-tab--active .status-tab__count {
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.12));
}

/* 잠재중대성 배지 */
.severity-badge {
  display: inline-block;
  padding: var(--space-xxs, 0.125rem) var(--space-sm, 0.5rem);
  border-radius: var(--radius-pill, 999px);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
}

.severity-badge--minor {
  background: var(--color-severity-minor-bg, #ecfdf5);
  color: var(--color-severity-minor-fg, #047857);
}

.severity-badge--major {
  background: var(--color-severity-major-bg, #fff7ed);
  color: var(--color-severity-major-fg, #c2410c);
}

.severity-badge--critical {
  background: var(--color-severity-critical-bg, #fef2f2);
  color: var(--color-severity-critical-fg, #b91c1c);
}
</style>
