<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearchReset"
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
        <label>이수일</label>
        <CalendarSrch v-model="startDate" />
        <span class="date-range-sep">~</span>
        <CalendarSrch v-model="endDate" />
      </div>
      <div>
        <label>이름/사번</label>
        <input
          class="kw-search-input"
          v-model.trim="searchKeyword"
          type="text"
          placeholder="이름 또는 사번"
          @keyup.enter="fnSearchReset"
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
          <span class="subtitle-text">사용자별 TBM 진행 현황</span>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 64vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table
            class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 4%">
                  No
                </th>
                <th style="width: 10%">사번</th>
                <th style="width: 12%">이름</th>
                <th style="width: 9%">고용형태</th>
                <th style="width: 13%">소속부서</th>
                <th style="width: 13%; text-align: center">누적 교육시간</th>
                <th style="width: 9%; text-align: center">수료</th>
                <th style="width: 9%; text-align: center">미이수</th>
                <th style="width: 12%; text-align: center">최근 이수일</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!progressList || progressList.length === 0">
                <tr>
                  <td colspan="9" class="edu-grid-empty">
                    조회된 사용자가 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(row, idx) in progressList"
                  :key="row.userTypeCd + '_' + row.userCd"
                >
                  <td style="text-align: center">
                    {{ (page - 1) * pageSize + idx + 1 }}
                  </td>
                  <td>{{ row.userId || "-" }}</td>
                  <td>
                    <button
                      type="button"
                      class="name-link"
                      @click="fnDetail(row)"
                    >
                      {{ row.userNm || "-" }}
                    </button>
                  </td>
                  <td style="text-align: center">
                    <span
                      class="type-badge"
                      :class="
                        row.userTypeCd === 'DAILY'
                          ? 'type-daily'
                          : 'type-regular'
                      "
                    >
                      {{
                        row.userTypeCd === "DAILY"
                          ? "일용직"
                          : row.employmentTypeNm || "정규직"
                      }}
                    </span>
                  </td>
                  <td>{{ row.deptNm || "-" }}</td>
                  <td style="text-align: center">
                    {{ fmtMinutes(row.totalEduMinutes) }}
                  </td>
                  <td style="text-align: center">
                    {{ row.completedSessionCount || 0 }}
                  </td>
                  <td style="text-align: center">
                    <span
                      :class="
                        Number(row.notCompletedSessionCount) > 0
                          ? 'not-completed'
                          : ''
                      "
                    >
                      {{ row.notCompletedSessionCount || 0 }}
                    </span>
                  </td>
                  <td style="text-align: center">
                    {{ row.lastCompletedAt || "-" }}
                  </td>
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
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import TbmUserProgressDetail from "./popup/TbmUserProgressDetail.vue";

// ================ Options ================
defineOptions({ name: "Tbm_03" });

// ================ Props ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs ================
const progressList = ref([]);

// ── 헤더 버튼 (조회 전용 화면 — 생성/저장/삭제/엑셀 숨김) ──
const localButtons = ref({ ...props.buttons });
const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

// 조회조건
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteNoFcs = ref(null);
// 이수일(STATUS_UPDATED_AT) 기준. 기본값: 당해 연도 1월 1일 ~ 12월 31일(Acct_01 발생기간과 동일)
const nowYear = new Date().getFullYear();
const startDate = ref(`${nowYear}-01-01`);
const endDate = ref(`${nowYear}-12-31`);
const searchKeyword = ref(""); // 이름/사번

// 페이징
const page = ref(1);
const pageSize = ref(20);
const totalCount = ref(0);

// ================ Computed ================
const totalPages = computed(() => {
  const pages = Math.ceil(totalCount.value / pageSize.value);
  return pages < 1 ? 1 : pages;
});

// ================ Life Cycle ================
onMounted(async () => {
  fnInit();
  fnButtonControll();
  await fnSearch();
});

// ================ API Functions ================
// 사업장 검색(Tbm_04.vue fnSrchSiteInfo 흐름 복제)
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

// 검색 트리거(조회 버튼/키워드 Enter): 조건 변경 시 1페이지로 복귀 후 조회.
//   (qa L2: 페이지>1에서 조건 변경 후 조회 시 빈 마지막장이 뜨던 문제 해소.
//    페이지 이동(fnGoPage)은 선택 페이지를 유지해야 하므로 fnSearch를 재사용하고 리셋은 여기서만.)
const fnSearchReset = () => {
  page.value = 1;
  fnSearch();
};

// 사용자별 진행 집계 목록 조회(GET /webApi/tbm03/user-progress-list)
const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/tbm03/user-progress-list", {
      params: {
        siteCd: siteCd.value,
        startDate: startDate.value,
        endDate: endDate.value,
        searchKeyword: searchKeyword.value,
        page: page.value,
        pageSize: pageSize.value,
      },
    });

    if (response.status === 200) {
      const data = response.data || {};
      progressList.value = data.progressList || [];
      totalCount.value = data.totalCount || 0;
    }
  } catch (err) {
    progressList.value = [];
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

// Tbm_04.vue focusKill 복제(siteNo/siteNm 상호 클리어 + 단건 조회)
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

// Tbm_04.vue fnCallback 복제(site-lists 단일/다중 처리)
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

// 행 클릭 → 사용자별 세션 이수 이력 드릴다운(UI-002)
const fnDetail = (row) => {
  openPop(TbmUserProgressDetail, {
    userCd_p: row.userCd,
    userTypeCd_p: row.userTypeCd,
    userNm_p: row.userNm,
  });
};

// 누적 교육시간(분) 표시 포맷: 0/NULL→"0분", <60→"N분", >=60→"h시간 m분"(m=0이면 "h시간")
const fmtMinutes = (min) => {
  const m = Number(min) || 0;
  if (m < 60) return `${m}분`;
  const h = Math.floor(m / 60);
  const rest = m % 60;
  return rest === 0 ? `${h}시간` : `${h}시간 ${rest}분`;
};
</script>

<style scoped>
/* 이름/사번 검색 input 너비(.viewSearch input 기본 120px → 200px) */
.kw-search-input {
  width: 200px;
}

/* 발생기간(Acct_01) 과 동일한 기간 구분자 */
.date-range-sep {
  margin: 0 var(--space-xs, 0.25rem);
}

/* 미이수 강조 */
.not-completed {
  color: var(--color-danger);
  font-weight: 700;
}

/* 고용형태 배지 */
.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
  white-space: nowrap;
}

.type-regular {
  background: var(--color-bg);
  color: var(--color-text-strong);
  border: 1px solid var(--color-border-strong);
}

.type-daily {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

/* 이름 링크(드릴다운 진입) */
.name-link {
  background: none;
  border: none;
  padding: 0;
  color: var(--color-primary);
  cursor: pointer;
  text-align: left;
  text-decoration: underline;
  font: inherit;
}

.name-link:hover {
  color: var(--color-primary-hover);
}

/* 페이징 */
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
