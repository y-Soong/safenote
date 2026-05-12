<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 조회 영역 -->
    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          ref="siteNoFcs"
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
          <input type="checkbox" v-model="incSubNodeYn" />
          하위부서 조회
        </label>
      </div>
      <div>
        <label>사용자명</label>
        <input v-model.trim="searchUserNm" type="text" />
      </div>
    </div>

    <!-- 캘린더 툴바 (월 네비 / 뷰 토글 / 페이지 액션) -->
    <div class="a07-toolbar">
      <div class="a07-nav">
        <button class="a07-nav-arr" @click="fnPrevMonth">‹</button>
        <CalendarSrchMonth v-model="workYm" class="a07-nav-month-picker" />
        <button class="a07-nav-arr" @click="fnNextMonth">›</button>
        <div class="a07-view-toggle">
          <button
            type="button"
            :class="['a07-view-btn', { active: viewMode === 'calendar' }]"
            @click="viewMode = 'calendar'"
          >
            캘린더 뷰
          </button>
          <button
            type="button"
            :class="['a07-view-btn', { active: viewMode === 'list' }]"
            @click="viewMode = 'list'"
          >
            목록 뷰
          </button>
        </div>
      </div>
      <div class="a07-actions">
        <button
          class="a07-btn-issue"
          :class="{ disabled: issueCount === 0 }"
          @click="fnOpenIssueListPop"
        >
          처리 필요 <b>{{ issueCount }}</b
          >건 →
        </button>
        <button
          class="a07-btn-line"
          :class="{ disabled: !canMonthClose }"
          @click="fnOpenMonthClosePop"
        >
          근태 마감
        </button>
        <button class="a07-btn-line" @click="fnOpenExcelUploadPop">
          ↑ 엑셀 업로드
        </button>
      </div>
    </div>

    <!-- ============================================================ -->
    <!-- 캘린더 뷰 -->
    <!-- ============================================================ -->
    <div v-show="viewMode === 'calendar'" class="viewBody a07-cal-body">
      <div class="a07-cal-wrap">
        <div class="a07-cal-scroll">
          <table class="a07-matrix">
            <thead>
              <tr>
                <th class="m-user-head sticky-top sticky-left">사용자 정보</th>
                <th
                  v-for="d in daysInMonth"
                  :key="d.day"
                  class="m-day-head sticky-top"
                  :class="{ 'head-sun': d.dow === 0, 'head-sat': d.dow === 6 }"
                >
                  {{ d.day }}<br /><span class="m-dow"
                    >({{ dowLabels[d.dow] }})</span
                  >
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in userList" :key="u.userId">
                <td class="m-user-cell sticky-left">
                  <div class="m-user-name">{{ u.name }}</div>
                  <div class="m-user-meta">
                    {{ u.userId }} · {{ u.dept }} · {{ u.role }}
                  </div>
                </td>
                <td
                  v-for="d in daysInMonth"
                  :key="d.day"
                  class="m-day-cell"
                  :class="getCellClass(u, d)"
                  @click="fnOpenDayDetailPop(u, d)"
                >
                  <template v-if="getCell(u, d).type === 'off'">
                    <span class="m-off">−</span>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'normal'">
                    <div class="m-time-1">
                      <div>{{ getCell(u, d).t1 }}</div>
                      <div>{{ getCell(u, d).t2 }}</div>
                    </div>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'normal2'">
                    <div class="m-time-2">
                      <div>{{ getCell(u, d).a1 }}</div>
                      <div>{{ getCell(u, d).a2 }}</div>
                      <div class="m-div"></div>
                      <div>{{ getCell(u, d).b1 }}</div>
                      <div>{{ getCell(u, d).b2 }}</div>
                    </div>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'issue-dash'">
                    <span class="m-off">−</span>
                    <span class="m-corner"></span>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'issue-marker'">
                    <div class="m-time-1">
                      <div>{{ getCell(u, d).t1 }}</div>
                      <div>{{ getCell(u, d).t2 }}</div>
                    </div>
                    <span class="m-corner"></span>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'pre-dash'">
                    <span class="m-off">−</span>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'pre-time'">
                    <div class="m-time-1">
                      <div>{{ getCell(u, d).t1 }}</div>
                      <div>{{ getCell(u, d).t2 }}</div>
                    </div>
                  </template>
                  <template v-else-if="getCell(u, d).type === 'code'">
                    <div
                      class="m-code-label"
                      v-html="getCell(u, d).label"
                    ></div>
                  </template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="a07-cal-legend">
          <spanㄹ class="lg-item"
            ><span class="sw sw-issue"></span>처리 필요 — 라벨형</spanㄹ
          >
          <span class="lg-item"
            ><span class="sw sw-marker"></span>처리 필요 — 코너 마커</span
          >
          <span class="lg-item"><span class="sw sw-code"></span>근태 코드</span>
          <span class="lg-sat">토</span>
          <span class="lg-sun">일·공휴일</span>
          <span class="lg-summary">
            정상 <b>{{ summary.ok }}</b> · 처리필요
            <b class="warn">{{ summary.issue }}</b> · 마감률
            <b>{{ summary.rate }}%</b>
          </span>
        </div>
      </div>
    </div>

    <!-- ============================================================ -->
    <!-- 목록 뷰 -->
    <!-- ============================================================ -->
    <div v-show="viewMode === 'list'" class="viewBody a07-list-body">
      <div class="a07-list-wrap">
        <!-- 좌측: 사용자 리스트 -->
        <div class="a07-user-pane">
          <div class="a07-user-head">
            <span
              >사용자 <b>{{ userList.length }}</b
              >명</span
            >
            <span class="a07-user-sort">정렬 ↓</span>
          </div>
          <div class="a07-user-list">
            <div
              v-for="u in userList"
              :key="u.userId"
              :class="[
                'a07-user-item',
                { active: selectedUserId === u.userId },
              ]"
              @click="selectedUserId = u.userId"
            >
              <div class="ui-top">
                <span class="ui-name">{{ u.name }}</span>
                <span v-if="u.issues > 0" class="ui-badge-issue">{{
                  u.issues
                }}</span>
                <span v-else class="ui-badge-ok">정상</span>
              </div>
              <div class="ui-meta">{{ u.userId }}</div>
              <div class="ui-meta">{{ u.dept }}</div>
            </div>
          </div>
        </div>

        <!-- 우측: 사용자 상세 -->
        <div class="a07-detail">
          <div class="a07-detail-head" v-if="selectedUser">
            <div class="a07-uinfo">
              <div class="a07-avatar">{{ selectedUser.name.charAt(0) }}</div>
              <div>
                <div class="a07-uname">{{ selectedUser.name }}</div>
                <div class="a07-umeta">
                  {{ selectedUser.userId }} · {{ selectedUser.dept }} ·
                  {{ selectedUser.role }}
                </div>
              </div>
            </div>
            <div class="a07-metrics">
              <div class="a07-m a07-m-warn">
                <span class="a07-m-lbl">확인필요</span>
                <span class="a07-m-val"
                  >{{ detailSummary.alerts }}<small>건</small></span
                >
              </div>
              <div class="a07-m">
                <span class="a07-m-lbl">총 근무 기록</span>
                <span class="a07-m-val">
                  {{ detailSummary.recDays }}<small>일</small> /
                  {{ detailSummary.totalH }}<small>시간</small>
                  {{ detailSummary.totalM }}<small>분</small>
                </span>
              </div>
            </div>
          </div>

          <div class="a07-detail-table-wrap">
            <table class="a07-detail-table">
              <colgroup>
                <col class="c-date" />
                <col class="c-plan" />
                <col class="c-plan" />
                <col class="c-act-time" />
                <col class="c-act-time" />
                <col class="c-act-loc" />
                <col class="c-act-loc" />
                <col class="c-norm" />
                <col class="c-norm" />
                <col class="c-plan" />
                <col class="c-plan" />
                <col class="c-act-time" />
                <col class="c-act-time" />
                <col class="c-act-loc" />
                <col class="c-act-loc" />
                <col class="c-norm" />
                <col class="c-norm" />
                <col class="c-total" />
                <col class="c-note" />
              </colgroup>
              <thead>
                <!-- Level 1: 날짜 / 1구간 / 2구간 / 근무시간 / 비고 -->
                <tr class="lvl1">
                  <th class="date-h" rowspan="3">날짜</th>
                  <th class="l1-shift bdr-section" colspan="8">1</th>
                  <th class="l1-shift bdr-section" colspan="8">2</th>
                  <th class="l1-rs bdr-section" rowspan="3">근무시간</th>
                  <th class="l1-rs col-note bdr-section" rowspan="3">비고</th>
                </tr>
                <!-- Level 2: 계획 / 실적 / 표준화 -->
                <tr class="lvl2">
                  <th class="l2-plan bdr-section" colspan="2">계획</th>
                  <th class="l2-actual bdr-sub" colspan="4">실적</th>
                  <th class="l2-norm bdr-sub" colspan="2">표준화</th>
                  <th class="l2-plan bdr-section" colspan="2">계획</th>
                  <th class="l2-actual bdr-sub" colspan="4">실적</th>
                  <th class="l2-norm bdr-sub" colspan="2">표준화</th>
                </tr>
                <!-- Level 3: 컬럼명 -->
                <tr class="lvl3">
                  <th class="l3-plan bdr-section">시작</th>
                  <th class="l3-plan">종료</th>
                  <th class="l3-actual bdr-sub">출근</th>
                  <th class="l3-actual">퇴근</th>
                  <th class="l3-actual">출근장소</th>
                  <th class="l3-actual">퇴근장소</th>
                  <th class="l3-norm bdr-sub">출근</th>
                  <th class="l3-norm">퇴근</th>
                  <th class="l3-plan bdr-section">시작</th>
                  <th class="l3-plan">종료</th>
                  <th class="l3-actual bdr-sub">출근</th>
                  <th class="l3-actual">퇴근</th>
                  <th class="l3-actual">출근장소</th>
                  <th class="l3-actual">퇴근장소</th>
                  <th class="l3-norm bdr-sub">출근</th>
                  <th class="l3-norm">퇴근</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="r in detailRows"
                  :key="r.day"
                  :class="r.status"
                  @click="fnOpenAttdAdjustPop(r)"
                >
                  <td class="date">
                    <span v-if="r.status === 'alert'" class="alert-dot"></span>
                    {{ r.day }}
                    <span :class="dowClass(r.dow)"
                      >({{ dowLabels[r.dow] }})</span
                    >
                  </td>
                  <!-- 1구간 계획 -->
                  <td class="col-plan bdr-section">
                    {{ valOrDash(r.p1Start) }}
                  </td>
                  <td class="col-plan">{{ valOrDash(r.p1End) }}</td>
                  <!-- 1구간 실적 -->
                  <td class="col-actual bdr-sub">{{ valOrDash(r.a1In) }}</td>
                  <td class="col-actual">{{ valOrDash(r.a1Out) }}</td>
                  <td class="col-actual" v-html="locOrDash(r.a1InLoc)"></td>
                  <td class="col-actual" v-html="locOrDash(r.a1OutLoc)"></td>
                  <!-- 1구간 표준화 -->
                  <td class="col-norm bdr-sub">{{ valOrDash(r.n1In) }}</td>
                  <td class="col-norm">{{ valOrDash(r.n1Out) }}</td>
                  <!-- 2구간 계획 -->
                  <td class="col-plan bdr-section">
                    {{ valOrDash(r.p2Start) }}
                  </td>
                  <td class="col-plan">{{ valOrDash(r.p2End) }}</td>
                  <!-- 2구간 실적 -->
                  <td class="col-actual bdr-sub">{{ valOrDash(r.a2In) }}</td>
                  <td class="col-actual">{{ valOrDash(r.a2Out) }}</td>
                  <td class="col-actual" v-html="locOrDash(r.a2InLoc)"></td>
                  <td class="col-actual" v-html="locOrDash(r.a2OutLoc)"></td>
                  <!-- 2구간 표준화 -->
                  <td class="col-norm bdr-sub">{{ valOrDash(r.n2In) }}</td>
                  <td class="col-norm">{{ valOrDash(r.n2Out) }}</td>
                  <!-- 근무시간 -->
                  <td class="bdr-section right">{{ valOrDash(r.total) }}</td>
                  <!-- 비고 -->
                  <td class="bdr-section col-note">
                    <span
                      v-if="r.note"
                      :class="
                        r.status === 'alert' ? 'badge-warn' : 'badge-info'
                      "
                      >{{ r.note }}</span
                    >
                    <span v-else class="dash">−</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted,
  getCurrentInstance,
  defineProps,
  defineOptions,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CalendarSrchMonth from "@/components/common/CalendarSrchMonth.vue";
import { useModal } from "@/utils/useModal";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import AttdDayDetailPop from "@/views/attd/popup/AttdDayDetailPop.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";

defineOptions({ name: "Attd_07" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });

const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

// ── 조회 조건 ─────────────────────────────────────────────
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
const searchUserNm = ref("");
const siteNoFcs = ref(null);

// ── 화면 상태 ─────────────────────────────────────────────
const viewMode = ref("calendar"); // 'calendar' | 'list'
const workYm = ref(currentYm()); // YYYY-MM

const dowLabels = ["일", "월", "화", "수", "목", "금", "토"];

function currentYm() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

const fnPrevMonth = () => shiftMonth(-1);
const fnNextMonth = () => shiftMonth(1);
const shiftMonth = (delta) => {
  const [y, m] = workYm.value.split("-").map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  workYm.value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
};

// ── 사업장 / 부서 포커스 처리 (Attd_06_2 패턴 차용) ────────
//   값이 비어있으면 관련 필드 클리어
//   값이 있으면 짝 필드 비우고 즉시 자동조회 (단건 매칭이면 자동 세팅, 다건이면 팝업)
const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNo.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "nodeCd") {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeNm.value = "";
    } else {
      nodeNm.value = "";
      fnSrchNodeInfo();
    }
  } else if (e.target.id === "nodeNm") {
    if (proxy.$util.isEmpty(nodeNm.value)) {
      nodeCd.value = "";
    } else {
      nodeCd.value = "";
      fnSrchNodeInfo();
    }
  }
};

// 사업장 자동조회 (코드/명 입력 후 blur)
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
      err?.response?.data?.message ||
        err?.message ||
        getMessage(MSG.SEARCH_ERROR_DEFAULT)
    );
  }
};

// 부서 자동조회 (코드/명 입력 후 blur)
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
    await proxy.$alert(
      err?.response?.data?.message ||
        err?.message ||
        getMessage(MSG.SEARCH_ERROR_DEFAULT)
    );
  }
};

// 자동조회 응답 처리 — 0건/1건/다건 분기
const fnCallback = (res) => {
  if (!proxy.$util.isNotEmpty(res)) return;
  const apiId = res.config.url.split("/").pop();
  if (apiId === "site-lists") {
    const list = res.data?.siteInfoResultList ?? [];
    if (list.length === 1) {
      siteCd.value = list[0].siteCd;
      siteNo.value = list[0].siteNo;
      siteNm.value = list[0].siteNm;
      nodeDisabled.value = false;
    } else if (list.length > 1) {
      fnSiteSearchPopOpen();
    } else {
      siteCd.value = "";
      siteNo.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    }
  } else if (apiId === "site-node-lists") {
    const list = res.data?.siteNodeInfoList || [];
    if (list.length === 0) {
      nodeCd.value = "";
      nodeNm.value = "";
    } else if (list.length === 1) {
      nodeCd.value = list[0].nodeCd ?? "";
      nodeNm.value = list[0].nodeNm ?? "";
    } else {
      fnSiteNodeSearchPopOpen();
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

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnSiteNodeSearchPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED_FIRST));
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userCd_p: "",
    onSelect: (nodeCdVal, nodeNmVal) => {
      nodeCd.value = nodeCdVal ?? "";
      nodeNm.value = nodeNmVal ?? "";
    },
  });
};

// ── 사용자/근태 데이터 (조회 결과로 채움) ─────────────────
const userList = ref([]);
// key: `${userId}_${day}` → 응답 레코드 1건
const recordMap = ref({});
// 처리 필요 목록 (response.data.monthlyAttdReqSummaryResultList)
//   { reqId, workYmd, userCd } — "처리 필요 n 건" 카운트 + 캘린더 셀 라벨형 표시
const reqIdList = ref([]);
// userCd_workYmd 키 Set — 캘린더 셀별 매칭 O(1)
const reqCellSet = computed(() => {
  const s = new Set();
  for (const r of reqIdList.value) {
    if (r?.userCd && r?.workYmd) s.add(`${r.userCd}_${r.workYmd}`);
  }
  return s;
});

const selectedUserId = ref("");
const selectedUser = computed(
  () => userList.value.find((u) => u.userId === selectedUserId.value) ?? null
);

// ── 월의 일자 목록 ────────────────────────────────────────
const daysInMonth = computed(() => {
  const [y, m] = workYm.value.split("-").map(Number);
  const last = new Date(y, m, 0).getDate();
  const arr = [];
  for (let d = 1; d <= last; d++) {
    arr.push({ day: d, dow: new Date(y, m - 1, d).getDay() });
  }
  return arr;
});

// ── 응답 변환 헬퍼 ────────────────────────────────────────
// "0930" → "09:30"
const fmtTime = (hhmm) => {
  if (!hhmm) return "";
  const v = String(hhmm);
  if (v.length < 4) return v;
  return `${v.slice(0, 2)}:${v.slice(2, 4)}`;
};
// "093015" → "09:30:15", "0930" → "09:30:00"
const fmtTimeSec = (v) => {
  if (!v) return "";
  const s = String(v);
  if (s.length === 4) return `${s.slice(0, 2)}:${s.slice(2, 4)}:00`;
  if (s.length >= 6)
    return `${s.slice(0, 2)}:${s.slice(2, 4)}:${s.slice(4, 6)}`;
  return s;
};
// 응답 record에서 day(1~31) 추출 — workYmd(YYYYMMDD) 우선, fallback cmpnyCd
const pickDay = (r) => {
  const ymd = String(r.workYmd ?? "");
  if (/^\d{8}$/.test(ymd)) return Number(ymd.slice(6, 8));
  const alt = String(r.cmpnyCd ?? "");
  if (/^\d{8}$/.test(alt)) return Number(alt.slice(6, 8));
  return 0;
};
// 오늘 YYYYMMDD
const todayYmd = () => {
  const n = new Date();
  return `${n.getFullYear()}${String(n.getMonth() + 1).padStart(2, "0")}${String(n.getDate()).padStart(2, "0")}`;
};
// 현재 HHMM
const nowHhmm = () => {
  const n = new Date();
  return `${String(n.getHours()).padStart(2, "0")}${String(n.getMinutes()).padStart(2, "0")}`;
};
// 시간값을 4자리 HHMM로 정규화
const normHhmm = (v) =>
  String(v ?? "")
    .replace(/\D/g, "")
    .slice(0, 4);

// 출퇴근 상태 판정 (오늘/미래는 누락이 아니라 "출근전"/"퇴근전" 으로 분류)
// 반환: { status: 'alert' | 'pre' | '', note }
//  - 미래 날짜: 미입력은 "출근전" / "퇴근전"
//  - 오늘 날짜: 스케줄 시간 전이면 "전", 시간이 지났는데도 없으면 "누락"
//  - 과거 날짜: 기존 누락 규칙 그대로
const detectAttdState = (r, workYmd) => {
  if (!r) return { status: "", note: "" };
  if (r.leaveNm) return { status: "pre", note: r.leaveNm };
  if (!r.plan1Start && !r.plan2Start) return { status: "", note: "" };

  const ymd = String(workYmd ?? "");
  const today = todayYmd();
  const isFuture = ymd > today;
  const isToday = ymd === today;
  const now = nowHhmm();

  const beforePlan = (planTime) => isToday && now < normHhmm(planTime);

  if (r.plan1Start && !r.act1InTime) {
    if (isFuture || beforePlan(r.plan1Start))
      return { status: "pre", note: "출근전" };
    return { status: "alert", note: "출근누락" };
  }
  if (r.plan1End && r.act1InTime && !r.act1OutTime) {
    if (isFuture || beforePlan(r.plan1End))
      return { status: "pre", note: "퇴근전" };
    return { status: "alert", note: "퇴근누락" };
  }
  if (r.plan2Start && !r.act2InTime) {
    if (isFuture || beforePlan(r.plan2Start))
      return { status: "pre", note: "출근전" };
    return { status: "alert", note: "출근누락" };
  }
  if (r.plan2End && r.act2InTime && !r.act2OutTime) {
    if (isFuture || beforePlan(r.plan2End))
      return { status: "pre", note: "퇴근전" };
    return { status: "alert", note: "퇴근누락" };
  }
  return { status: "", note: "" };
};

// 출퇴근 누락 등 처리필요 여부 (사용자 issue 카운트용)
const isIssueRecord = (r) => detectAttdState(r, r?.workYmd).status === "alert";
// 분 단위 근무시간 계산
const calcMin = (inT, outT, breakMin) => {
  if (!inT || !outT) return 0;
  const i = String(inT),
    o = String(outT);
  if (i.length < 4 || o.length < 4) return 0;
  const im = parseInt(i.slice(0, 2), 10) * 60 + parseInt(i.slice(2, 4), 10);
  const om = parseInt(o.slice(0, 2), 10) * 60 + parseInt(o.slice(2, 4), 10);
  return Math.max(0, om - im - (parseInt(breakMin, 10) || 0));
};
const calcTotal = (r) => {
  const t =
    calcMin(r.act1InTime, r.act1OutTime, r.plan1BreakMin) +
    calcMin(r.act2InTime, r.act2OutTime, r.plan2BreakMin);
  if (t <= 0) return "";
  return `${Math.floor(t / 60)}시간 ${String(t % 60).padStart(2, "0")}분`;
};

// 캘린더 셀 데이터
const getCell = (user, dayInfo) => {
  const r = recordMap.value[`${user.userId}_${dayInfo.day}`];
  if (!r) return { type: "off" };
  // 실적이 하나라도 있으면 스케줄 유무와 관계없이 데이터 표시
  const hasAct = !!(
    r.act1InTime ||
    r.act1OutTime ||
    r.act2InTime ||
    r.act2OutTime
  );
  // 계획도 없고 실적도 없으면 휴무
  if (!r.plan1Start && !r.plan2Start && !hasAct) return { type: "off" };
  // 휴가
  if (r.leaveNm) return { type: "code", label: r.leaveNm };

  // 오늘/미래/과거 판정용 YYYYMMDD
  const ymd =
    String(r.workYmd ?? "") ||
    `${workYm.value.replace("-", "")}${String(dayInfo.day).padStart(2, "0")}`;
  // 1구간(plan1) 기준으로만 셀 누락/예정 판정 — 정상이면 아래에서 시간 표시로 떨어짐
  // (2구간 누락 여부는 isIssueRecord/목록 뷰에서 별도 처리)
  const today = todayYmd();
  const isFuture = ymd > today;
  const isToday = ymd === today;
  const now = nowHhmm();
  const beforePlan = (planTime) => isToday && now < normHhmm(planTime);

  // 1구간 출근 자체가 없음
  if (r.plan1Start && !r.act1InTime) {
    if (isFuture || beforePlan(r.plan1Start)) return { type: "pre-dash" };
    return { type: "issue-dash" };
  }
  // 1구간 출근은 있고 퇴근만 없음
  if (r.plan1End && r.act1InTime && !r.act1OutTime) {
    if (isFuture || beforePlan(r.plan1End)) {
      return { type: "pre-time", t1: fmtTime(r.act1InTime), t2: "-" };
    }
    return { type: "issue-marker", t1: fmtTime(r.act1InTime), t2: "-" };
  }

  // 분할 근무 — act2 실적이 있을 때만 4줄(1·2구간) 표시
  if (r.act2InTime || r.act2OutTime) {
    return {
      type: "normal2",
      a1: fmtTime(r.act1InTime),
      a2: fmtTime(r.act1OutTime),
      b1: fmtTime(r.act2InTime),
      b2: fmtTime(r.act2OutTime),
    };
  }
  // 단일 근무 또는 분할근무지만 act2 미입력 — act1만 2줄로 표시
  return {
    type: "normal",
    t1: fmtTime(r.act1InTime),
    t2: fmtTime(r.act1OutTime),
  };
};

// workYm.value(YYYY-MM) + day → YYYYMMDD
const toCellYmd = (day) =>
  `${workYm.value.replace("-", "")}${String(day).padStart(2, "0")}`;

const getCellClass = (user, dayInfo) => {
  const c = getCell(user, dayInfo);
  // 백엔드 처리필요 목록(monthlyAttdReqSummaryResultList)과 매칭되면 라벨형 강조
  const isReq = reqCellSet.value.has(
    `${user.userCd}_${toCellYmd(dayInfo.day)}`
  );
  return {
    off: c.type === "off",
    "issue-marker": c.type === "issue-marker" || c.type === "issue-dash",
    "issue-label": isReq,
    "code-cell": c.type === "code",
  };
};

// ── 요약 (샘플) ────────────────────────────────────────────
// "처리 필요 n 건" 카운트 — fnSearch 응답의 reqIdResult 리스트 길이를 그대로 사용
const issueCount = computed(() => reqIdList.value.length);
const canMonthClose = computed(() => issueCount.value === 0);
const summary = computed(() => {
  const total = userList.value.length * daysInMonth.value.length;
  const issue = issueCount.value;
  const ok = total - issue;
  return { ok, issue, rate: total ? Math.round((ok / total) * 100) : 0 };
});

// ── 목록 뷰: 선택 사용자 상세 행 빌드 ─────────────────────
const detailRows = computed(() => {
  if (!selectedUser.value) return [];
  const u = selectedUser.value;
  return daysInMonth.value.map((d) => buildDetailRow(u, d));
});

function buildDetailRow(user, d) {
  const r = recordMap.value[`${user.userId}_${d.day}`];
  if (!r) return emptyRow(d, "off");
  // 실적이 하나라도 있으면 스케줄이 없어도 행을 채워서 표시
  const hasAct = !!(
    r.act1InTime ||
    r.act1OutTime ||
    r.act2InTime ||
    r.act2OutTime
  );
  if (!r.plan1Start && !r.plan2Start && !hasAct) {
    return emptyRow(d, "off");
  }

  // 선택 월 + 일자로 YYYYMMDD 구성 (오늘/미래/과거 판정용)
  const ymd =
    String(r.workYmd ?? "") ||
    `${workYm.value.replace("-", "")}${String(d.day).padStart(2, "0")}`;
  const { status, note } = detectAttdState(r, ymd);

  return {
    day: d.day,
    dow: d.dow,
    p1Start: fmtTime(r.plan1Start),
    p1End: fmtTime(r.plan1End),
    a1In: fmtTimeSec(r.act1InTime),
    a1Out: fmtTimeSec(r.act1OutTime),
    a1InLoc: r.act1InMethod ?? "",
    a1OutLoc: r.act1OutMethod ?? "",
    n1In: fmtTime(r.act1InTime),
    n1Out: fmtTime(r.act1OutTime),
    p2Start: fmtTime(r.plan2Start),
    p2End: fmtTime(r.plan2End),
    a2In: fmtTimeSec(r.act2InTime),
    a2Out: fmtTimeSec(r.act2OutTime),
    a2InLoc: r.act2InMethod ?? "",
    a2OutLoc: r.act2OutMethod ?? "",
    n2In: fmtTime(r.act2InTime),
    n2Out: fmtTime(r.act2OutTime),
    total: calcTotal(r),
    note,
    status,
  };
}

function emptyRow(d, status) {
  return {
    day: d.day,
    dow: d.dow,
    p1Start: "",
    p1End: "",
    a1In: "",
    a1Out: "",
    a1InLoc: "",
    a1OutLoc: "",
    n1In: "",
    n1Out: "",
    p2Start: "",
    p2End: "",
    a2In: "",
    a2Out: "",
    a2InLoc: "",
    a2OutLoc: "",
    n2In: "",
    n2Out: "",
    total: "",
    note: "",
    status,
  };
}

const detailSummary = computed(() => {
  const rows = detailRows.value.filter((r) => r.status !== "off");
  const alerts = rows.filter((r) => r.status === "alert").length;
  let recDays = 0,
    totalH = 0,
    totalM = 0;
  rows.forEach((r) => {
    const m = (r.total || "").match(/^(\d+)시간(?:\s*(\d+)분)?/);
    if (m) {
      recDays++;
      totalH += parseInt(m[1], 10);
      if (m[2]) totalM += parseInt(m[2], 10);
    }
  });
  totalH += Math.floor(totalM / 60);
  totalM = totalM % 60;
  return { alerts, recDays, totalH, totalM };
});

const valOrDash = (v) => (v ? v : "−");
const locOrDash = (v) => {
  if (!v) return '<span class="dash">−</span>';
  if (v === "사업장") return "사업장";
  return '<span class="loc-out">사업장 외</span>';
};
const dowClass = (dow) => (dow === 0 ? "dow-sun" : dow === 6 ? "dow-sat" : "");

// ── 팝업 핸들러 ───────────────────────────────────────────
// 일자 상세 팝업(AttdDayDetailPop)은 record 데이터를 그대로 전달
// 잔여 팝업: AttdIssueListPop / AttdMonthClosePop / AttdExcelUploadPop (추후)
const fnOpenIssueListPop = () => {
  if (issueCount.value === 0) return;
  proxy.$alert(getMessage(MSG.ISSUE_LIST_PREPARING));
};
const fnOpenMonthClosePop = () => {
  if (!canMonthClose.value) {
    proxy.$alert(getMessage(MSG.MONTH_CLOSE_BLOCKED));
    return;
  }
  proxy.$alert(getMessage(MSG.MONTH_CLOSE_PREPARING));
};
const fnOpenExcelUploadPop = () =>
  proxy.$alert(getMessage(MSG.EXCEL_UPLOAD_PREPARING));

// 팝업은 식별자만 전달하고 상세/이력은 팝업 내부에서 API로 조회한다.
// (attd1Id 우선, 없으면 attd2Id, 둘 다 없으면 신규 케이스로 빈문자 전달)
// attdId가 없는 케이스는 팝업이 API를 안 부르므로 사용자/스케줄을 fallback_p로 같이 넘긴다.
const buildFallback = (user, record) => ({
  userId: user.userId ?? "",
  userCd: user.userCd ?? "",
  userNm: user.name ?? "",
  authCd: user.role ?? "",
  authNm: user.authNm ?? "",
  plan1Start: record.plan1Start ?? "",
  plan1End: record.plan1End ?? "",
  plan2Start: record.plan2Start ?? "",
  plan2End: record.plan2End ?? "",
});

const fnOpenDayDetailPop = (user, d) => {
  // 스케줄 데이터가 없는(off) 셀도 클릭 시 팝업이 열리도록 허용
  const [y, m] = workYm.value.split("-").map(Number);
  const ymd = `${y}-${String(m).padStart(2, "0")}-${String(d.day).padStart(2, "0")}`;
  const record = recordMap.value[`${user.userId}_${d.day}`] ?? {};
  openPop(AttdDayDetailPop, {
    attdId_p: record.attd1Id ?? record.attd2Id ?? "",
    siteCd_p: siteCd.value,
    userCd_p: user.userCd ?? "",
    nodeCd_p: record.nodeCd ?? "",
    date_p: ymd,
    dow_p: d.dow,
    fallback_p: buildFallback(user, record),
    onSaved: fnSearch,
  });
};

const fnOpenAttdAdjustPop = (row) => {
  if (row.status !== "alert") return;
  const [y, m] = workYm.value.split("-").map(Number);
  const ymd = `${y}-${String(m).padStart(2, "0")}-${String(row.day).padStart(2, "0")}`;
  const record =
    recordMap.value[`${selectedUser.value.userId}_${row.day}`] ?? {};
  openPop(AttdDayDetailPop, {
    attdId_p: record.attd1Id ?? record.attd2Id ?? "",
    siteCd_p: siteCd.value,
    userCd_p: selectedUser.value.userCd ?? "",
    nodeCd_p: record.nodeCd ?? "",
    date_p: ymd,
    dow_p: row.dow,
    fallback_p: buildFallback(selectedUser.value, record),
    onSaved: fnSearch,
  });
};

// ── 응답 → 화면 모델 매핑 ─────────────────────────────────
const fnBindResponse = (data) => {
  reqIdList.value = data?.monthlyAttdReqSummaryResultList ?? [];

  const recs = data?.attdRecordResultList ?? [];

  // 1. 사용자 목록 (userId 기준 unique, 등장 순서 유지)
  const seen = new Set();
  const list = [];
  const issuesByUser = {};
  const map = {};

  for (const r of recs) {
    if (!seen.has(r.userId)) {
      seen.add(r.userId);
      list.push({
        userId: r.userId,
        userCd: r.userCd,
        name: r.userNm,
        dept: r.deptNm,
        role: r.authNm ?? "",
        shift: r.plan2Start ? 2 : 1,
        issues: 0,
      });
    }

    const day = pickDay(r);
    if (day) map[`${r.userId}_${day}`] = r;

    if (isIssueRecord(r)) {
      issuesByUser[r.userId] = (issuesByUser[r.userId] || 0) + 1;
    }
  }

  // 분할근무 여부는 사용자 전체 record를 보고 한 번이라도 plan2가 있으면 2로
  list.forEach((u) => {
    u.issues = issuesByUser[u.userId] || 0;
  });

  userList.value = list;
  recordMap.value = map;

  // 선택 사용자 유지 또는 첫 사용자로 전환
  if (list.length && !list.find((u) => u.userId === selectedUserId.value)) {
    selectedUserId.value = list[0].userId;
  }
};

// ── 조회 ──────────────────────────────────────────────────
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_INPUT_REQUIRED));
    siteNoFcs.value?.focus();
    return;
  }

  try {
    const response = await axios.get("/webApi/attd07/monthly-attd-lists", {
      params: {
        workYm: workYm.value,
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        userNm: searchUserNm.value,
      },
    });

    if (response.status === 200) {
      fnBindResponse(response.data);
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      getMessage(MSG.SEARCH_ERROR);
    await proxy.$alert(msg);
  }
};

// ── 초기화 ────────────────────────────────────────────────
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
  if (siteCd.value) {
    nodeDisabled.value = false;
    nodeCd.value = sessionStorage.getItem("gv_nodeCd") ?? "";
    nodeNm.value = sessionStorage.getItem("gv_nodeNm") ?? "";
  }
};

onMounted(() => {
  fnInit();
  fnButtonControll();
});
</script>

<style scoped>
/* ── 공통 ─────────────────────────────────────────────────── */
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

/* ── 툴바 ─────────────────────────────────────────────────── */
.a07-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
  flex-wrap: wrap;
  font-family: "Pretendard", sans-serif;
}
.a07-nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
}
.a07-nav-arr {
  width: 24px;
  height: 24px;
  border: 1px solid var(--color-border, #d1d5db);
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  color: var(--color-text-muted, #6b7280);
  font-size: 0.875rem;
  line-height: 1;
}
.a07-nav-arr:hover {
  color: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
}
.a07-nav-month-picker {
  display: inline-flex;
  align-items: center;
}
.a07-nav-month-picker :deep(.calendar-input) {
  height: 28px;
  padding: 0 0.5rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  background: #fff;
  cursor: pointer;
  text-align: center;
  min-width: 110px;
  font-family: "Pretendard", sans-serif;
}
.a07-nav-month-picker :deep(.calendar-input:hover) {
  border-color: var(--color-primary, #16a34a);
  color: var(--color-primary, #16a34a);
}
.a07-view-toggle {
  display: inline-flex;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 4px;
  overflow: hidden;
  margin-left: 0.5rem;
}
.a07-view-btn {
  background: none;
  border: none;
  padding: 0.3rem 0.85rem;
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
}
.a07-view-btn.active {
  background: var(--color-primary, #16a34a);
  color: #fff;
  font-weight: 600;
}
.a07-actions {
  display: flex;
  gap: 0.4rem;
  align-items: center;
  font-size: 0.8125rem;
}
.a07-btn-issue {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  border: 1.5px solid #f59e0b;
  background: #fef3c7;
  color: #92400e;
  padding: 0.3rem 0.8rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8125rem;
  font-family: "Pretendard", sans-serif;
}
.a07-btn-issue b {
  font-size: 0.9rem;
}
.a07-btn-issue.disabled {
  background: #fff;
  border-color: var(--color-border, #d1d5db);
  color: var(--color-text-muted, #9ca3af);
  cursor: not-allowed;
}
.a07-btn-line {
  padding: 0.3rem 0.8rem;
  border-radius: 4px;
  border: 1px solid var(--color-border, #d1d5db);
  background: #fff;
  cursor: pointer;
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
  font-family: "Pretendard", sans-serif;
}
.a07-btn-line:hover {
  background: var(--color-bg, #f3f4f6);
}
.a07-btn-line.disabled {
  color: var(--color-text-muted, #9ca3af);
  cursor: not-allowed;
  background: #fff;
}

/* ============================================================ */
/* 캘린더 뷰 */
/* ============================================================ */
.a07-cal-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
.a07-cal-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  overflow: hidden;
}
.a07-cal-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

table.a07-matrix {
  border-collapse: collapse;
  font-size: 0.6875rem;
  min-width: max-content;
  font-family: "Pretendard", sans-serif;
}
table.a07-matrix th,
table.a07-matrix td {
  border-right: 0.5px solid var(--color-border, #e5e7eb);
  border-bottom: 0.5px solid var(--color-border, #e5e7eb);
}
table.a07-matrix th {
  font-weight: 500;
  color: var(--color-text-muted, #6b7280);
  background: #fff;
  font-size: 0.6875rem;
  white-space: nowrap;
}
table.a07-matrix th.sticky-top {
  position: sticky;
  top: 0;
  z-index: 2;
  background: var(--color-bg, #f9fafb);
}
table.a07-matrix th.sticky-left,
table.a07-matrix td.sticky-left {
  position: sticky;
  left: 0;
  background: #fff;
  z-index: 1;
}
table.a07-matrix th.sticky-left.sticky-top {
  z-index: 3;
}

.m-user-head {
  padding: 8px 12px;
  width: 200px;
  min-width: 200px;
  text-align: left;
}
.m-day-head {
  width: 52px;
  min-width: 52px;
  text-align: center;
  line-height: 1.3;
  padding: 6px 2px;
}
.m-day-head .m-dow {
  font-size: 0.625rem;
  opacity: 0.85;
}
.m-day-head.head-sat {
  color: #3b82f6;
}
.m-day-head.head-sun {
  color: #ef4444;
}

.m-user-cell {
  padding: 9px 12px;
  vertical-align: middle;
  border-right: 1px solid var(--color-border, #d1d5db);
}
.m-user-name {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  line-height: 1.2;
}
.m-user-meta {
  font-size: 0.625rem;
  color: var(--color-text-muted, #9ca3af);
  margin-top: 2px;
  line-height: 1.3;
}

table.a07-matrix td.m-day-cell {
  padding: 6px 3px;
  text-align: center;
  vertical-align: middle;
  color: var(--color-text-muted, #6b7280);
  min-width: 52px;
  height: 62px;
  position: relative;
  background: #fff;
  cursor: pointer;
}
table.a07-matrix td.m-day-cell:hover {
  background: rgba(22, 163, 74, 0.04);
}
table.a07-matrix td.m-day-cell.off {
  background: #fafaf7;
  color: #9a9a95;
}
table.a07-matrix td.m-day-cell.off:hover {
  background: rgba(22, 163, 74, 0.04);
}
.m-time-1 {
  line-height: 1.25;
}
.m-time-2 {
  line-height: 1.15;
  font-size: 0.625rem;
}
.m-time-2 .m-div {
  height: 0.5px;
  background: var(--color-border, #e5e7eb);
  margin: 2px 6px;
}
table.a07-matrix td.m-day-cell.code-cell {
  background: #f1efe8;
  color: var(--color-text-muted, #6b7280);
}
/* 처리필요 — 라벨형 (monthlyAttdReqSummaryResultList 매칭 셀) */
table.a07-matrix td.m-day-cell.issue-label {
  background: #fef3c7;
  box-shadow: inset 0 0 0 1px #f59e0b;
  color: #92400e;
}
table.a07-matrix td.m-day-cell.issue-label:hover {
  background: #fde68a;
}
.m-code-label {
  line-height: 1.2;
}
.m-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 10px 10px 0;
  border-color: transparent #f59e0b transparent transparent;
}

.a07-cal-legend {
  display: flex;
  gap: 1rem;
  padding: 9px 14px;
  border-top: 0.5px solid var(--color-border, #e5e7eb);
  font-size: 0.6875rem;
  color: var(--color-text-muted, #6b7280);
  align-items: center;
  flex-wrap: wrap;
  background: #fff;
}
.a07-cal-legend .lg-item {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}
.sw {
  width: 11px;
  height: 11px;
  border-radius: 2px;
  display: inline-block;
}
.sw-issue {
  background: #fef3c7;
  border: 0.5px solid #f59e0b;
}
.sw-marker {
  background: #fff;
  border: 0.5px solid var(--color-border, #d1d5db);
  position: relative;
}
.sw-marker::after {
  content: "";
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 5px 5px 0;
  border-color: transparent #f59e0b transparent transparent;
}
.sw-code {
  background: #f1efe8;
}
.lg-sat {
  color: #3b82f6;
}
.lg-sun {
  color: #ef4444;
}
.lg-summary {
  margin-left: auto;
  font-size: 0.6875rem;
}
.lg-summary b {
  color: var(--color-text-strong, #111827);
}
.lg-summary .warn {
  color: #92400e;
}
.m-off {
  color: var(--color-text-muted, #9ca3af);
}

/* ============================================================ */
/* 목록 뷰 */
/* ============================================================ */
.a07-list-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
.a07-list-wrap {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  background: #fff;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  overflow: hidden;
}

/* 좌측 사용자 리스트 */
.a07-user-pane {
  border-right: 1px solid var(--color-border, #e5e7eb);
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
}
.a07-user-head {
  padding: 9px 12px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-bg, #f9fafb);
}
.a07-user-head b {
  color: var(--color-text-strong, #111827);
}
.a07-user-sort {
  color: var(--color-text-muted, #9ca3af);
  font-size: 0.6875rem;
  cursor: pointer;
}
.a07-user-list {
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}
.a07-user-item {
  padding: 9px 12px 9px 14px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  cursor: pointer;
  position: relative;
}
.a07-user-item:hover {
  background: #fafaf7;
}
.a07-user-item.active {
  background: rgba(22, 163, 74, 0.08);
}
.a07-user-item.active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--color-primary, #16a34a);
}
.ui-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.4rem;
}
.ui-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  line-height: 1.2;
}
.a07-user-item.active .ui-name {
  color: #15803d;
}
.ui-meta {
  font-size: 0.6875rem;
  color: var(--color-text-muted, #9ca3af);
  margin-top: 3px;
  line-height: 1.4;
}
.ui-badge-issue {
  font-size: 0.625rem;
  background: #fef3c7;
  color: #92400e;
  border: 0.5px solid #f59e0b;
  padding: 1px 6px;
  border-radius: 9px;
  font-weight: 600;
}
.ui-badge-ok {
  font-size: 0.625rem;
  color: var(--color-text-muted, #9ca3af);
}

/* 우측 상세 영역 */
.a07-detail {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}
.a07-detail-head {
  padding: 10px 18px;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  background: #fff;
}
.a07-uinfo {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.a07-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(22, 163, 74, 0.12);
  color: #15803d;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.8125rem;
  flex-shrink: 0;
}
.a07-uname {
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.2;
  color: var(--color-text-strong, #111827);
}
.a07-umeta {
  font-size: 0.6875rem;
  color: var(--color-text-muted, #6b7280);
  margin-top: 2px;
  line-height: 1.3;
}
.a07-metrics {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-left: auto;
}
.a07-m {
  display: flex;
  align-items: baseline;
  gap: 0.4rem;
  font-size: 0.75rem;
  white-space: nowrap;
}
.a07-m .a07-m-lbl {
  color: var(--color-text-muted, #6b7280);
}
.a07-m .a07-m-val {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-strong, #111827);
}
.a07-m .a07-m-val small {
  font-weight: 400;
  color: var(--color-text-muted, #6b7280);
  font-size: 0.6875rem;
  margin-left: 2px;
}
.a07-m-warn {
  padding: 3px 10px;
  border-radius: 14px;
  background: #fef3c7;
  border: 0.5px solid #f59e0b;
}
.a07-m-warn .a07-m-lbl,
.a07-m-warn .a07-m-val,
.a07-m-warn .a07-m-val small {
  color: #92400e;
}

.a07-detail-table-wrap {
  overflow: auto;
  flex: 1;
  min-height: 0;
}

/* ── 상세 테이블 (3단 헤더) ─────────────────────────────── */
table.a07-detail-table {
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
  width: 1410px;
  font-size: 0.75rem;
  font-family: "Pretendard", sans-serif;
}
table.a07-detail-table col.c-date {
  width: 80px;
}
table.a07-detail-table col.c-plan {
  width: 60px;
}
table.a07-detail-table col.c-act-time {
  width: 80px;
}
table.a07-detail-table col.c-act-loc {
  width: 80px;
}
table.a07-detail-table col.c-norm {
  width: 60px;
}
table.a07-detail-table col.c-total {
  width: 90px;
}
table.a07-detail-table col.c-note {
  width: 120px;
}

table.a07-detail-table th {
  padding: 7px 8px;
  text-align: center;
  font-weight: 500;
  color: var(--color-text-muted, #6b7280);
  font-size: 0.6875rem;
  background: var(--color-bg, #f9fafb);
  white-space: nowrap;
  position: sticky;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 3단 헤더 sticky */
table.a07-detail-table thead tr.lvl1 th {
  top: 0;
  z-index: 5;
  height: 30px;
  border-bottom: 0.5px solid var(--color-border, #e5e7eb);
}
table.a07-detail-table thead tr.lvl2 th {
  top: 30px;
  z-index: 4;
  height: 28px;
  font-size: 0.6875rem;
  border-bottom: 0.5px solid var(--color-border, #e5e7eb);
}
table.a07-detail-table thead tr.lvl3 th {
  top: 58px;
  z-index: 3;
  height: 26px;
  font-size: 0.6875rem;
  font-weight: 500;
  color: var(--color-text-muted, #9a9a95);
  border-bottom: 1px solid #cfcfc8;
}

/* 그룹 컬러 */
table.a07-detail-table th.l1-shift {
  background: #ededdc;
  color: var(--color-text-strong, #111827);
  font-weight: 700;
  letter-spacing: 0.5px;
}
table.a07-detail-table th.l1-rs {
  background: var(--color-bg, #f9fafb);
}
table.a07-detail-table th.l2-plan,
table.a07-detail-table th.l3-plan {
  background: #eef4f9;
  color: #3b5a75;
}
table.a07-detail-table th.l2-actual,
table.a07-detail-table th.l3-actual {
  background: #f7f7f2;
}
table.a07-detail-table th.l2-norm,
table.a07-detail-table th.l3-norm {
  background: #ecf7ee;
  color: #15803d;
}

/* 셀 */
table.a07-detail-table td {
  padding: 8px 8px;
  border-bottom: 0.5px solid var(--color-border, #e5e7eb);
  font-size: 0.75rem;
  color: var(--color-text, #374151);
  white-space: nowrap;
  background: #fff;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
}
table.a07-detail-table td.right {
  text-align: right;
}
table.a07-detail-table td.col-plan {
  background: #f8fafc;
}
table.a07-detail-table td.col-actual {
  background: #fbfbf7;
}
table.a07-detail-table td.col-norm {
  background: #f4fbf6;
}

/* 그룹 경계선 */
table.a07-detail-table th.bdr-section,
table.a07-detail-table td.bdr-section {
  border-left: 1.5px solid #b8b8b0;
}
table.a07-detail-table th.bdr-sub,
table.a07-detail-table td.bdr-sub {
  border-left: 0.5px solid #cfcfc8;
}

/* 날짜 sticky */
table.a07-detail-table th.date-h {
  position: sticky;
  left: 0;
  top: 0;
  z-index: 6;
  background: var(--color-bg, #f9fafb);
  box-shadow: 1px 0 0 var(--color-border, #e5e7eb);
  min-width: 78px;
  border-bottom: 1px solid #cfcfc8;
}
table.a07-detail-table td.date {
  position: sticky;
  left: 0;
  z-index: 2;
  background: #fff;
  box-shadow: 1px 0 0 var(--color-border, #e5e7eb);
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  text-align: left;
  min-width: 78px;
}

/* 행 상태 */
table.a07-detail-table tr.off td {
  background: #fafaf7;
  color: var(--color-text-muted, #9a9a95);
}
table.a07-detail-table tr.off td.col-plan,
table.a07-detail-table tr.off td.col-norm,
table.a07-detail-table tr.off td.col-actual {
  background: #f4f4f0;
}
table.a07-detail-table tr.alert td {
  background: #fffbeb;
  cursor: pointer;
}
table.a07-detail-table tr.alert td.col-plan,
table.a07-detail-table tr.alert td.col-norm,
table.a07-detail-table tr.alert td.col-actual {
  background: #fbf7e5;
}
table.a07-detail-table tr.alert td.date {
  box-shadow:
    inset 3px 0 0 #f59e0b,
    1px 0 0 var(--color-border, #e5e7eb);
  background: #fffbeb;
}
table.a07-detail-table tr.alert:hover td {
  background: #fdf6c4;
}
table.a07-detail-table tr.pre td {
  color: var(--color-text-muted, #6b7280);
}

.alert-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f59e0b;
  margin-right: 6px;
  vertical-align: middle;
}
.dow-sat {
  color: #3b82f6;
}
.dow-sun {
  color: #ef4444;
}

.badge-warn {
  font-size: 0.625rem;
  padding: 2px 8px;
  border-radius: 9px;
  font-weight: 600;
  background: #fef3c7;
  color: #92400e;
  border: 0.5px solid #f59e0b;
}
.badge-info {
  font-size: 0.625rem;
  padding: 2px 8px;
  border-radius: 9px;
  font-weight: 600;
  background: #eff6ff;
  color: #1d4ed8;
  border: 0.5px solid #93c5fd;
}
.dash {
  color: var(--color-text-muted, #9ca3af);
}
.loc-out {
  color: #92400e;
}
</style>
