<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="props.buttons"
      @search="fnSearch"
      @create="fnCreate"
    />

    <!-- 탭(PRAFTA-SUBCON-T5 D2): 내가 개설한 교육 / 다른 회사에서 연동받은 교육.
         탭 스타일은 Attd_01 표준(밑줄형)을 따른다. -->
    <div class="attd01-tab-bar">
      <button
        type="button"
        :class="['attd01-tab-btn', { active: activeTab === 'own' }]"
        @click="fnSwitchTab('own')"
      >
        내 교육
      </button>
      <button
        type="button"
        :class="['attd01-tab-btn', { active: activeTab === 'shared' }]"
        @click="fnSwitchTab('shared')"
      >
        연동받은 교육
      </button>
    </div>

    <!-- ===== 연동받은 교육(비개설사 전용): 재지정 관리만 가능. 상세/콘솔 진입점 없음 ===== -->
    <div v-if="activeTab === 'shared'" class="viewBody">
      <div class="table-box overflow-x-auto rounded-md border border-slate-300">
        <table class="data-grid w-full table-fixed text-sm text-left">
          <thead>
            <tr>
              <th style="width: 4%; text-align: center">No</th>
              <th style="width: 30%">교육 제목</th>
              <th style="width: 16%">연동해 준 회사</th>
              <th style="width: 12%">상태</th>
              <th style="width: 14%">지정일시</th>
              <th style="width: 10%; text-align: center">우리 참석자</th>
              <th style="width: 14%; text-align: center">관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="sharedList.length === 0">
              <td colspan="7" class="edu-grid-empty">
                연동받은 교육이 없습니다.
              </td>
            </tr>
            <tr v-for="(row, idx) in sharedList" :key="row.sessionCd" v-else>
              <td style="text-align: center">
                {{ (sharedPage - 1) * pageSize + idx + 1 }}
              </td>
              <td>{{ row.title }}</td>
              <td>{{ row.designatedByCmpnyNm || "-" }}</td>
              <td>{{ row.statusNm || row.statusCd }}</td>
              <td>{{ row.designatedDtime || "-" }}</td>
              <td style="text-align: center">{{ row.myAttendanceCount }}</td>
              <td style="text-align: center">
                <button
                  class="btn btn-second btn-sm"
                  @click="fnOpenSharePop(row)"
                >
                  재지정 관리
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="sharedTotalCount > 0" class="pager">
        <button
          class="btn btn-second btn-sm"
          :disabled="sharedPage <= 1"
          @click="fnGoSharedPage(sharedPage - 1)"
        >
          이전
        </button>
        <span class="pager-info">
          {{ sharedPage }} / {{ sharedTotalPages }} (총
          {{ sharedTotalCount }}건)
        </span>
        <button
          class="btn btn-second btn-sm"
          :disabled="sharedPage >= sharedTotalPages"
          @click="fnGoSharedPage(sharedPage + 1)"
        >
          다음
        </button>
      </div>
    </div>

    <div v-show="activeTab === 'own'" class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          ref="siteNoFcs"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          @blur="focusKill"
        />
        <button class="search-btn" @click="fnSiteSearchPopOpen()">
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          @blur="focusKill"
        />
      </div>
      <div>
        <label>상태</label>
        <select v-model.trim="statusCd" name="combo">
          <option value="">전체</option>
          <option
            v-for="opt in statusOptions"
            :key="opt.code"
            :value="opt.code"
          >
            {{ opt.name }}
          </option>
        </select>
      </div>
      <div>
        <label>개설일(시작)</label>
        <CalendarSrch v-model="startDate" />
      </div>
      <div>
        <label>개설일(종료)</label>
        <CalendarSrch v-model="endDate" />
      </div>
      <div>
        <label>제목</label>
        <input
          v-model.trim="searchKeyword"
          type="text"
          placeholder="교육 제목"
          @keyup.enter="fnSearch"
        />
      </div>
    </div>

    <div v-show="activeTab === 'own'" class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">TBM 교육 목록</span>
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
                <th class="event_cell" style="text-align: center; width: 3%">
                  No
                </th>
                <th style="width: 9%">상태</th>
                <th style="width: 10%">사업장</th>
                <ThSortable
                  label="교육 제목"
                  col-key="title"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.title"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th style="width: 7%; text-align: center">교육 시간</th>
                <th style="width: 8%">위험성평가</th>
                <th style="width: 7%">출결</th>
                <th style="width: 7%">이수</th>
                <th style="width: 7%">미이수</th>
                <th style="width: 9%">개설자</th>
                <ThSortable
                  label="개설일시"
                  col-key="openedAt"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.openedAt"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="등록일시"
                  col-key="insertDate"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.insertDate"
                  @sort="onSort"
                  @update:width="onResize"
                />
              </tr>
            </thead>
            <tbody>
              <template v-if="!sessionList || sessionList.length === 0">
                <tr>
                  <td colspan="12" class="edu-grid-empty">
                    조회된 TBM 교육이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in sortedData" :key="row.sessionCd">
                  <td style="text-align: center">
                    {{ (page - 1) * pageSize + idx + 1 }}
                  </td>
                  <td style="text-align: center">
                    <span
                      class="status-badge"
                      :class="statusClass(row.statusCd)"
                    >
                      {{ row.statusNm || statusNm(row.statusCd) }}
                    </span>
                  </td>
                  <td>{{ row.siteNm || row.siteCd }}</td>
                  <td>
                    <button
                      type="button"
                      class="title-link"
                      @click="fnDetail(row)"
                    >
                      {{ row.title }}
                    </button>
                  </td>
                  <td style="text-align: center">
                    {{ row.eduMinutes ? row.eduMinutes + "분" : "-" }}
                  </td>
                  <td style="text-align: center">
                    <span
                      class="risk-chip"
                      :class="
                        Number(row.riskCount) > 0
                          ? 'risk-chip-ok'
                          : 'risk-chip-warn'
                      "
                      :title="
                        Number(row.riskCount) > 0
                          ? '위험성평가 연계됨'
                          : '위험성평가가 연계되지 않았습니다.'
                      "
                    >
                      {{ Number(row.riskCount) > 0 ? "🔗" : "⚠️" }}
                      {{ row.riskCount }}건
                    </span>
                  </td>
                  <td style="text-align: center">{{ row.attendanceCount }}</td>
                  <td style="text-align: center">{{ row.completedCount }}</td>
                  <td style="text-align: center">
                    {{ row.notCompletedCount }}
                  </td>
                  <td>{{ row.managerUserNm }}</td>
                  <td style="text-align: center">{{ row.openedAt || "-" }}</td>
                  <td style="text-align: center">{{ row.insertDate }}</td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <!-- 페이징 -->
        <div v-if="totalCount > 0" class="pager">
          <button
            class="btn btn-second btn-sm"
            :disabled="page <= 1"
            @click="fnGoPage(page - 1)"
          >
            이전
          </button>
          <span class="pager-info">
            {{ page }} / {{ totalPages }} (총 {{ totalCount }}건)
          </span>
          <button
            class="btn btn-second btn-sm"
            :disabled="page >= totalPages"
            @click="fnGoPage(page + 1)"
          >
            다음
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import {
  ref,
  computed,
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
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import TbmSessionForm from "./popup/TbmSessionForm.vue";
import TbmSessionConsole from "./popup/TbmSessionConsole.vue";
import TbmShareCmpnyPop from "./popup/TbmShareCmpnyPop.vue";

// ================ Options ================
defineOptions({ name: "Tbm_02" });

// ================ Props ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs ================
const sessionList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(sessionList);
const { colWidths, onResize } = useColumnResize({
  title: 200,
  openedAt: 130,
  insertDate: 130,
});

// 조회조건
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteNoFcs = ref(null);
const statusCd = ref("");
const startDate = ref("");
const endDate = ref("");
const searchKeyword = ref("");

// 페이징
const page = ref(1);
const pageSize = ref(20);
const totalCount = ref(0);

// prafta-033-B: 상태 코드(SYS046) - 프론트 표시용 상수
const statusOptions = [
  { code: "DRAFT", name: "작성중" },
  { code: "OPENED", name: "개설" },
  { code: "IN_PROGRESS", name: "진행중" },
  { code: "COMPLETED", name: "종료" },
  { code: "CANCELLED", name: "취소" },
];

// ===== 연동받은 교육 탭(PRAFTA-SUBCON-T5 D2) =====
// 내 회사가 지정받은 타사 세션 목록. 헤더 최소 필드만 내려오며(본문/자료/참석자/사업장 없음),
// 이 탭에서 할 수 있는 것은 재지정 관리(TbmShareCmpnyPop)뿐이다 — 상세/콘솔 진입점을 두지 않는다.
const activeTab = ref("own"); // own | shared
const sharedList = ref([]);
const sharedPage = ref(1);
const sharedTotalCount = ref(0);

// ================ Computed ================
const totalPages = computed(() => {
  const pages = Math.ceil(totalCount.value / pageSize.value);
  return pages < 1 ? 1 : pages;
});

const sharedTotalPages = computed(() => {
  const pages = Math.ceil(sharedTotalCount.value / pageSize.value);
  return pages < 1 ? 1 : pages;
});

// ================ Life Cycle ================
onMounted(async () => {
  fnInit();
  await fnSearch();
});

// ================ 연동받은 교육 탭 ================
const fnSwitchTab = (tab) => {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
  if (tab === "shared") {
    fnSearchShared();
  }
};

const fnSearchShared = async () => {
  try {
    const response = await axios.get("/webApi/tbm02/shared-sessions", {
      params: {
        page: sharedPage.value,
        pageSize: pageSize.value,
      },
    });
    if (response.status === 200) {
      sharedList.value = response.data?.sessionList || [];
      sharedTotalCount.value = response.data?.totalCount || 0;
    }
  } catch (err) {
    sharedList.value = [];
    sharedTotalCount.value = 0;
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnGoSharedPage = (p) => {
  if (p < 1 || p > sharedTotalPages.value) return;
  sharedPage.value = p;
  fnSearchShared();
};

// 재지정 관리(기존 지정/해제/현황 엔드포인트 재사용 — 신규 API 없음).
// 지정 현황은 서버가 "내가 지정한 행"만 내려주므로 형제 회사의 지정 내역은 보이지 않는다.
const fnOpenSharePop = (row) => {
  openPop(TbmShareCmpnyPop, {
    sessionCd_p: row.sessionCd,
    onSaved: fnSearchShared,
  });
};

// ================ API Functions ================
const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
      },
    });
    if (response.status === 200) fnCallback(response);
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/tbm02/sessions", {
      params: {
        siteCd: siteCd.value,
        statusCd: statusCd.value,
        startDate: startDate.value,
        endDate: endDate.value,
        searchKeyword: searchKeyword.value,
        page: page.value,
        pageSize: pageSize.value,
      },
    });

    if (response.status === 200) {
      sessionList.value = response.data?.sessionList || [];
      totalCount.value = response.data?.totalCount || 0;
    }
  } catch (err) {
    sessionList.value = [];
    totalCount.value = 0;
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

// ================ Methods ================
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
    } else {
      siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
    } else {
      siteNo.value = "";
      fnSrchSiteInfo();
    }
  }
};

const fnCallback = (res) => {
  if (!proxy.$util.isNotEmpty(res)) return;
  const apiId = res.config.url.split("/").pop();
  if (apiId === "site-lists") {
    const list = res.data?.siteInfoResultList ?? [];
    if (list.length === 1) {
      siteCd.value = list[0].siteCd;
      siteNo.value = list[0].siteNo;
      siteNm.value = list[0].siteNm;
    } else if (list.length > 1) {
      fnSiteSearchPopOpen();
    } else {
      siteCd.value = "";
      siteNo.value = "";
      siteNm.value = "";
    }
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnGoPage = (target) => {
  if (target < 1 || target > totalPages.value) return;
  page.value = target;
  fnSearch();
};

const fnCreate = () => {
  openPop(TbmSessionForm, {
    onSearch: fnSearch,
    onCreated: (sessionCd) => fnOpenDetailByCd(sessionCd),
  });
};

const fnDetail = (row) => {
  fnOpenDetailByCd(row.sessionCd);
};

// prafta-051-13: 상세 진입 시 상태머신 콘솔(TbmSessionConsole)로 이동
const fnOpenDetailByCd = (sessionCd) => {
  openPop(TbmSessionConsole, {
    sessionCd_p: sessionCd,
    onSearch: fnSearch,
  });
};

const statusNm = (statusCd) => {
  const found = statusOptions.find((o) => o.code === statusCd);
  return found ? found.name : statusCd || "-";
};

const statusClass = (statusCd) => {
  switch (statusCd) {
    case "IN_PROGRESS":
      return "status-progress";
    case "OPENED":
      return "status-opened";
    case "DRAFT":
      return "status-draft";
    case "COMPLETED":
      return "status-completed";
    case "CANCELLED":
      return "status-cancelled";
    default:
      return "status-draft";
  }
};
</script>

<style scoped>
/* 탭 바(PRAFTA-SUBCON-T5 D2) — Attd_01 표준(밑줄형 14px) 준용 */
.attd01-tab-bar {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border);
}
.attd01-tab-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted);
  cursor: pointer;
}
.attd01-tab-btn:hover {
  color: var(--color-text);
}
.attd01-tab-btn.active {
  font-weight: 600;
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

/* prafta-033-B: 상태 배지 */
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
  line-height: 1.6;
  white-space: nowrap;
}

.status-progress {
  background: var(--color-primary);
  color: var(--color-surface);
}

.status-opened {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.status-draft {
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.status-completed {
  background: var(--color-bg);
  color: var(--color-text-strong);
  border: 1px solid var(--color-border-strong);
}

.status-cancelled {
  background: var(--color-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-border);
}

/* prafta-033-B: 위험성평가 연계 칩(0건이면 경고색) */
.risk-chip {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
  white-space: nowrap;
}

.risk-chip-ok {
  background: var(--color-bg);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.risk-chip-warn {
  background: var(--color-warning-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-danger);
}

/* prafta-033-B: 제목 링크(상세 진입) */
.title-link {
  background: none;
  border: none;
  padding: 0;
  color: var(--color-primary);
  cursor: pointer;
  text-align: left;
  text-decoration: underline;
  font: inherit;
}

.title-link:hover {
  color: var(--color-primary-hover);
}

/* prafta-033-B: 페이징 */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.pager-info {
  font-size: var(--btn-font);
  color: var(--color-text-muted);
}

.btn-sm {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}
</style>
