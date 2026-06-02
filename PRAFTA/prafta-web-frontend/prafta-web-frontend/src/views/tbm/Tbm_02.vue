<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="props.buttons"
      @search="fnSearch"
      @create="fnCreate"
    />

    <div class="viewSearch">
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
        <input v-model.trim="startDate" type="date" />
      </div>
      <div>
        <label>개설일(종료)</label>
        <input v-model.trim="endDate" type="date" />
      </div>
      <div>
        <label>제목</label>
        <input
          v-model.trim="searchKeyword"
          type="text"
          placeholder="세션 제목"
          @keyup.enter="fnSearch"
        />
      </div>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">TBM 세션 목록</span>
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
                  label="세션 제목"
                  col-key="title"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.title"
                  @sort="onSort"
                  @update:width="onResize"
                />
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
                  <td colspan="11" class="edu-grid-empty">
                    조회된 TBM 세션이 없습니다.
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
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import TbmSessionForm from "./popup/TbmSessionForm.vue";
import TbmSessionDetail from "./popup/TbmSessionDetail.vue";

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

// ================ Computed ================
const totalPages = computed(() => {
  const pages = Math.ceil(totalCount.value / pageSize.value);
  return pages < 1 ? 1 : pages;
});

// ================ Life Cycle ================
onMounted(async () => {
  fnInit();
  await fnSearch();
});

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

// prafta-033-B: 개설 성공 후 W-06 상세로 이동(W-07 콘솔 아님, C 보류)
const fnOpenDetailByCd = (sessionCd) => {
  openPop(TbmSessionDetail, {
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
