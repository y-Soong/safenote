<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 검색바: 사업장 / 소속부서(+하위) / 이름·아이디
         전사 권한이 아니면 서버 게이트가 부서 지정을 요구한다(관리 부서만 조회 가능). -->
    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
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
          @click="fnSiteNodeSearchPopOpen()"
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
          <input type="checkbox" v-model="incSubNodeYn" :disabled="nodeDisabled" />
          하위부서 조회
        </label>
      </div>

      <div>
        <label>사용자정보</label>
        <input
          v-model.trim="userKeyword"
          type="text"
          placeholder="사용자ID 또는 이름"
          style="width: 200px"
          @keyup.enter="fnSearch"
        />
      </div>
    </div>

    <div class="viewBody">
      <!-- 근로자 목록 -->
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">근로자별 소정근로시간</span>
          </div>
          <div class="subtitle-info" v-if="cmpnyWeekStdMinutes">
            통상근로자 기준 {{ fnFmtMinutes(cmpnyWeekStdMinutes) }}
          </div>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 38vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed text-sm text-left rtl:text-right">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 4%">No</th>
                <ThSortable
                  label="이름"
                  col-key="userNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.userNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="아이디"
                  col-key="userId"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.userId"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="부서"
                  col-key="nodeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.nodeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="event_cell" style="text-align: center; width: 90px">고용형태</th>
                <th class="event_cell" style="text-align: center; width: 120px">
                  현재 주 소정
                </th>
                <th class="event_cell" style="text-align: center; width: 110px">사유</th>
                <th class="event_cell" style="text-align: center; width: 180px">적용기간</th>
                <th class="event_cell" style="text-align: center; width: 90px">구분</th>
                <th class="event_cell" style="text-align: center; width: 80px">이력</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="listLoading">
                <tr>
                  <td colspan="10" class="edu-grid-empty">조회 중입니다...</td>
                </tr>
              </template>
              <template v-else-if="!stdWorkUserList || stdWorkUserList.length === 0">
                <tr>
                  <td colspan="10" class="edu-grid-empty">조회된 근로자가 없습니다.</td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(row, idx) in sortedData"
                  :key="row.userCd"
                  :class="{ 'is-selected': selectedUserCd === row.userCd }"
                  @click="fnSelectUser(row)"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.userNm }}</td>
                  <td>{{ row.userId }}</td>
                  <td>{{ row.nodeNm }}</td>
                  <td style="text-align: center">
                    {{ fnEmploymentTypeNm(row.employmentType) }}
                  </td>
                  <td style="text-align: center">
                    <span v-if="row.fromHistory">{{ fnFmtMinutes(row.weekStdMinutes) }}</span>
                    <span v-else class="std-badge is-none">미입력</span>
                  </td>
                  <td style="text-align: center">{{ row.reasonNm || "-" }}</td>
                  <td style="text-align: center">
                    {{ fnFmtPeriod(row.applyStrDate, row.applyEndDate) }}
                  </td>
                  <td style="text-align: center">
                    <span v-if="row.partTime" class="std-badge is-part">단시간</span>
                    <span v-else-if="row.fromHistory" class="std-badge is-normal">통상</span>
                    <span v-else class="std-badge is-none">기준 간주</span>
                  </td>
                  <td style="text-align: center">
                    <button class="btn btn-sm btn-primary" @click.stop="fnSelectUser(row)">
                      이력
                    </button>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 이력 타임라인 -->
      <div class="table-wrapper subtitle-pane history-pane">
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">
              소정근로시간 이력{{ historyUserNm ? ` — ${historyUserNm}` : "" }}
            </span>
          </div>
          <div class="custom-btn-area" v-if="canSave && selectedUserCd">
            <button
              class="btn btn-custom"
              :disabled="!historyEligible"
              @click="fnOpenRegisterPop"
            >
              변경 등록
            </button>
          </div>
        </div>

        <!-- 현재 해석 상태 요약 — 이력 미입력 계정의 폴백 실태를 감추지 않는다. -->
        <p class="history-summary" v-if="selectedUserCd && !historyLoading">
          <template v-if="!historyEligible">
            ⓘ 소정근로시간 관리 대상이 아닌 계정입니다(일용직 또는 사용중지 계정).
          </template>
          <template v-else-if="historySummary.fromHistory">
            현재({{ historySummary.baseYmd }}) 적용 =
            <strong>{{ fnFmtMinutes(historySummary.weekStdMinutes) }}</strong>
            <span v-if="historySummary.partTime" class="std-badge is-part">단시간</span>
          </template>
          <template v-else>
            <span class="std-badge is-none">미입력</span>
            소정근로 이력이 없어
            <strong>{{ fnFmtMinutes(historySummary.cmpnyWeekStdMinutes) }}</strong>
            ({{ fnSourceNm(historySummary.source) }})으로 간주됩니다.
          </template>
        </p>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 26vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed text-sm text-left rtl:text-right">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 180px">적용기간</th>
                <th class="event_cell" style="text-align: center; width: 110px">주 소정</th>
                <th class="event_cell" style="text-align: center; width: 110px">사유</th>
                <th class="event_cell">사유 상세</th>
                <th class="event_cell" style="text-align: center; width: 150px">입력</th>
                <th class="event_cell" style="text-align: center; width: 80px">정정</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!selectedUserCd">
                <tr>
                  <td colspan="6" class="edu-grid-empty">
                    목록에서 근로자를 선택하면 이력이 표시됩니다.
                  </td>
                </tr>
              </template>
              <template v-else-if="historyLoading">
                <tr>
                  <td colspan="6" class="edu-grid-empty">조회 중입니다...</td>
                </tr>
              </template>
              <template v-else-if="historyList.length === 0">
                <tr>
                  <td colspan="6" class="edu-grid-empty">
                    등록된 소정근로시간 이력이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="row in historyList" :key="row.applyStrDate">
                  <td style="text-align: center">
                    {{ fnFmtPeriod(row.applyStrDate, row.applyEndDate) }}
                  </td>
                  <td style="text-align: center">{{ fnFmtMinutes(row.weekStdMinutes) }}</td>
                  <td style="text-align: center">{{ row.reasonNm || row.reasonCd }}</td>
                  <td class="detail-cell">{{ row.reasonDetail || "-" }}</td>
                  <td style="text-align: center">{{ fnFmtDtime(row.insertDate) }}</td>
                  <td style="text-align: center">
                    <button
                      v-if="canSave && historyEligible"
                      class="btn btn-sm btn-second"
                      @click="fnOpenCorrectPop(row)"
                    >
                      정정
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
  computed,
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
import ThSortable from "@/components/common/ThSortable.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import StdWorkHoursEditPop from "./popup/StdWorkHoursEditPop.vue";
import { useTableSort, useColumnResize } from "@/composables/useTableFeatures.js";

// =========================== Define ===========================
defineOptions({ name: "User_10" });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const stdWorkUserList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(stdWorkUserList);
const { colWidths, onResize } = useColumnResize({
  userNm: 100,
  userId: 120,
  nodeNm: 130,
});

// 조회조건
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
const userKeyword = ref("");

// 목록/이력 상태
const listLoading = ref(false);
const historyLoading = ref(false);
const cmpnyWeekStdMinutes = ref(null);
const selectedUserCd = ref("");
const historyUserNm = ref("");
const historyList = ref([]);
const historySummary = ref({});
const historyEligible = ref(false);
const employmentTypeMap = ref({});

// 등록/정정 버튼 노출 — 메뉴 버튼 권한(BTN_SAVE). 실제 인가는 서버가 강제한다(부서 관리 권한).
const canSave = computed(() => localButtons.value?.save === "Y");

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// =========================== Life Cycle ===========================
const getSession = (key) => {
  const v = sessionStorage.getItem(key);
  return v && v !== "null" && v !== "undefined" ? v : "";
};

// 세션 사업장/부서 프리필 — 비전사 권한자는 부서 지정이 필요하므로 본인 소속을 채운다.
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

onMounted(async () => {
  fnInit();
  await fnLoadEmploymentTypes();
  if (siteCd.value) {
    await fnSearch();
  }
});

// =========================== Methods ===========================
// 고용형태(SYS041) 코드 → 명칭 매핑(표기 전용).
const fnLoadEmploymentTypes = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: { systCodeList: ["SYS041"] },
    });
    if (response.status === 200) {
      const map = {};
      (response.data?.systInfoList || []).forEach((item) => {
        if (item.systValDCd != null) map[item.systValDCd] = item.systValDNm;
      });
      employmentTypeMap.value = map;
    }
  } catch (err) {
    // 표기용 부가 정보라 실패해도 화면을 막지 않는다(코드값 그대로 노출).
    employmentTypeMap.value = {};
  }
};

// 목록 조회 — GET /webApi/user10/std-work-user-lists.
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }

  listLoading.value = true;
  stdWorkUserList.value = [];
  fnClearHistory();

  try {
    const response = await axios.get("/webApi/user10/std-work-user-lists", {
      params: {
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        userKeyword: userKeyword.value,
      },
    });

    if (response.status === 200) {
      stdWorkUserList.value = response.data?.stdWorkUserList || [];
      cmpnyWeekStdMinutes.value = response.data?.cmpnyWeekStdMinutes ?? null;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    listLoading.value = false;
  }
};

// 행 선택 → 이력 타임라인 조회.
const fnSelectUser = async (row) => {
  selectedUserCd.value = row.userCd;
  historyUserNm.value = row.userNm;
  await fnLoadHistory();
};

const fnLoadHistory = async () => {
  if (proxy.$util.isEmpty(selectedUserCd.value)) return;

  historyLoading.value = true;
  historyList.value = [];

  try {
    const response = await axios.get("/webApi/user10/std-work-histories", {
      params: { userCd: selectedUserCd.value },
    });

    if (response.status === 200) {
      const data = response.data || {};
      historyList.value = data.historyList || [];
      historySummary.value = data;
      historyEligible.value = !!data.eligible;
      historyUserNm.value = data.userNm || historyUserNm.value;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "이력 조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    historyLoading.value = false;
  }
};

const fnClearHistory = () => {
  selectedUserCd.value = "";
  historyUserNm.value = "";
  historyList.value = [];
  historySummary.value = {};
  historyEligible.value = false;
};

// 변경 등록(새 이력 행) — 직전 열린 행은 서버가 자동 마감한다.
const fnOpenRegisterPop = () => {
  openPop(StdWorkHoursEditPop, {
    mode_p: "REGISTER",
    userCd_p: selectedUserCd.value,
    userNm_p: historyUserNm.value,
    // 통상 기준값은 사업장별로 다를 수 있다 — 조회 사업장 기준으로 옵션을 받는다.
    siteCd_p: siteCd.value,
    row_p: null,
    onSaved: fnAfterSave,
  });
};

// 정정(오입력 수정) — 적용 시작일은 키라서 변경할 수 없다.
const fnOpenCorrectPop = (row) => {
  openPop(StdWorkHoursEditPop, {
    mode_p: "CORRECT",
    userCd_p: selectedUserCd.value,
    userNm_p: historyUserNm.value,
    siteCd_p: siteCd.value,
    row_p: { ...row },
    onSaved: fnAfterSave,
  });
};

// 저장 후 목록·이력 동시 갱신(현재 유효 소정이 바뀌므로 목록도 다시 읽는다).
const fnAfterSave = async () => {
  const keepUserCd = selectedUserCd.value;
  const keepUserNm = historyUserNm.value;
  await fnSearch();
  selectedUserCd.value = keepUserCd;
  historyUserNm.value = keepUserNm;
  await fnLoadHistory();
};

// ── 표기 헬퍼 ─────────────────────────────────────────────
const fnFmtMinutes = (minutes) => {
  const m = Number(minutes);
  if (!m || m <= 0) return "-";
  const h = Math.floor(m / 60);
  const rest = m % 60;
  return rest === 0 ? `주 ${h}시간` : `주 ${h}시간 ${rest}분`;
};

const fnFmtYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return "";
  return `${ymd.substring(0, 4)}-${ymd.substring(4, 6)}-${ymd.substring(6, 8)}`;
};

const fnFmtPeriod = (strDate, endDate) => {
  if (!strDate) return "-";
  return `${fnFmtYmd(strDate)} ~ ${endDate ? fnFmtYmd(endDate) : "무기한"}`;
};

const fnFmtDtime = (dtime) => {
  if (!dtime) return "-";
  return String(dtime).replace("T", " ").substring(0, 16);
};

const fnEmploymentTypeNm = (code) => {
  if (!code) return "-";
  return employmentTypeMap.value[code] || code;
};

// 폴백 출처 표기 — 사업장 오버라이드(SITE_POLICY) / 회사 기준값(COMPANY_POLICY) /
//   시스템 기본값(SYSTEM_DEFAULT) 3단을 구분한다.
const fnSourceNm = (source) => {
  if (source === "SITE_POLICY") return "사업장 통상 기준";
  if (source === "COMPANY_POLICY") return "회사 통상 기준";
  if (source === "SYSTEM_DEFAULT") return "시스템 기본값";
  return "통상 기준";
};

// ── 사업장/부서 검색 팝업 (User_01 조회조건 패턴 동일) ────
const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal ?? "";
  siteNo.value = siteNoVal ?? "";
  siteNm.value = siteNmVal ?? "";
  // 사업장이 바뀌면 이전 부서 조건은 무효(부서코드는 사업장별로 중복 사용된다).
  nodeCd.value = "";
  nodeNm.value = "";
  nodeDisabled.value = proxy.$util.isEmpty(siteCd.value);
};

const fnSiteNodeSearchPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userId_p: "",
    onSelect: onSiteNodeSelected,
  });
};

const onSiteNodeSelected = (nodeCdVal, nodeNmVal) => {
  nodeCd.value = nodeCdVal ?? "";
  nodeNm.value = nodeNmVal ?? "";
};

const focusKill = (e) => {
  const id = e?.target?.id;
  if (id === "siteNo" || id === "siteNm") {
    if (proxy.$util.isEmpty(siteNo.value) && proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      nodeCd.value = "";
      nodeNm.value = "";
      nodeDisabled.value = true;
    }
  } else if (id === "nodeCd" || id === "nodeNm") {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeNm.value = "";
    }
  }
};
</script>

<style scoped>
/* 검색바 좌측 정렬(User_01/User_06 패턴) */
.viewSearch {
  padding-left: calc(0.5rem + var(--space-md, 0.75rem));
  row-gap: 0.5rem;
}
.viewSearch > div:first-child {
  margin-left: 0;
}

/* 하위부서 조회 체크박스 — Attd_07 규격과 동일(검색바 표기 통일).
   스타일이 없으면 검색바의 일반 input 규칙이 적용돼 체크박스가 과도하게 커진다. */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  user-select: none;
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

/* 목록 아래 이력 패널 */
.history-pane {
  margin-top: 0.75rem;
}

.subtitle-info {
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #4b5563);
}

.history-summary {
  margin: 0 0 0.5rem 0;
  padding: 0.5rem 0.75rem;
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.6;
  color: var(--color-text, #374151);
  background: var(--color-surface-muted, #f3f4f6);
  border-radius: var(--btn-radius, 8px);
}

/* 소정근로 상태 배지 */
.std-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  margin-left: 0.25rem;
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
.std-badge.is-part {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.std-badge.is-normal {
  background: var(--color-surface-muted, #f3f4f6);
  color: var(--color-text-muted, #4b5563);
}
.std-badge.is-none {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-muted, #4b5563);
}

/* 선택 행 강조 */
tr.is-selected {
  background: var(--color-surface-muted, #f3f4f6);
}

/* 사유 상세 — 길면 줄바꿈 */
.detail-cell {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
