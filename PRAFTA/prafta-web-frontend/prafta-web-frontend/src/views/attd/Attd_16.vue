<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 조회 영역 (Attd_07 viewSearch 패턴 미러) -->
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
    </div>

    <div class="viewBody attd16-body">
      <div class="attd16-main">
        <!-- 좌측: 연차 사용 캘린더 (Attd_02 캘린더 구조 미러) -->
        <div class="attd16-calendar">
          <div class="calendar-header">
            <div class="calendar-title-row">
              <h2 class="calendar-title">연차 사용 캘린더</h2>
            </div>
            <div class="calendar-nav-row">
              <div class="calendar-nav">
                <button type="button" class="btn-nav" @click="prevMonth">
                  &lt;
                </button>
                <span
                  class="calendar-month calendar-month-clickable"
                  @click="fnOpenMonthPicker"
                >
                  {{ displayYear }}년 {{ displayMonth }}월
                </span>
                <button type="button" class="btn-nav" @click="nextMonth">
                  &gt;
                </button>
              </div>
            </div>
          </div>
          <div class="calendar-body">
            <table class="calendar-table">
              <thead>
                <tr>
                  <th
                    v-for="d in dayLabels"
                    :key="d"
                    :class="dayHeaderClass(d)"
                  >
                    {{ d }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(week, wi) in calendarWeeks" :key="wi">
                  <td
                    v-for="(cell, ci) in week"
                    :key="ci"
                    :class="[
                      cellClass(cell),
                      {
                        'cell-selected': cell && selectedDate === cell.dateStr,
                      },
                    ]"
                    @click="cell && fnSelectDate(cell)"
                  >
                    <div v-if="cell" class="cell-inner">
                      <div class="cell-top">
                        <span class="cell-num">{{ cell.date }}</span>
                        <span
                          v-if="cell.userCount > 0"
                          class="cell-count-badge"
                        >
                          {{ cell.userCount }}명
                        </span>
                      </div>
                      <div class="cell-users">
                        <span
                          v-for="(nm, nmIdx) in cell.visibleNames"
                          :key="`${cell.dateStr}-${nmIdx}`"
                          class="user-tag"
                          :title="nm"
                        >
                          {{ nm }}
                        </span>
                        <span
                          v-if="cell.overflowCount > 0"
                          class="user-tag user-tag-more"
                        >
                          외 {{ cell.overflowCount }}명
                        </span>
                      </div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 우측: 일자 상세 패널 (Attd_02 우측 패널 구조 미러, 탭 없이 상세 단일) -->
        <div class="attd16-panel">
          <div class="panel-header">일자 상세</div>
          <div class="panel-body">
            <template v-if="selectedDate">
              <div class="detail-date">
                {{ selectedDateLabel }}
                <span v-if="selectedDateUserCount > 0" class="detail-count">
                  {{ selectedDateUserCount }}명
                </span>
              </div>
              <div v-if="selectedDateDetails.length > 0" class="detail-cards">
                <div
                  v-for="(item, itemIdx) in selectedDateDetails"
                  :key="`detail-${itemIdx}`"
                  class="detail-card"
                >
                  <div class="card-header">
                    <h3 class="card-title">{{ item.userNm }}</h3>
                    <span class="card-node">{{ item.nodeNm ?? "-" }}</span>
                  </div>
                  <div class="card-info-row">
                    <span class="card-tag tag-leave">{{ item.leaveNm }}</span>
                    <span class="card-unit">{{ item.useUnitLabel }}</span>
                    <span v-if="item.timeRange" class="card-time">
                      {{ item.timeRange }}
                    </span>
                  </div>
                </div>
              </div>
              <div v-else class="detail-empty">
                해당 날짜에 연차 사용 인원이 없습니다.
              </div>
            </template>
            <div v-else class="detail-empty">
              캘린더에서 날짜를 선택해주세요.
            </div>
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
  watch,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import CalendarMonthPickerPop from "@/views/attd/popup/CalendarMonthPickerPop.vue";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";

defineOptions({ name: "Attd_16" });

const props = defineProps({ title: String, buttons: Object });
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });

// ── 조회 조건 (Attd_07 미러) ──────────────────────────────
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);

// ── 캘린더 상태 ───────────────────────────────────────────
const now = new Date();
const displayYear = ref(now.getFullYear());
const displayMonth = ref(now.getMonth() + 1);
const selectedDate = ref(""); // YYYY-MM-DD
const isLoading = ref(false);

// 조회 결과: 일자 전개된 연차 사용 실적 행 (plan §3 응답 계약)
const leaveUsageList = ref([]);

const dayLabels = ["일", "월", "화", "수", "목", "금", "토"];

// 셀에 이름 태그를 전원 노출하는 최대 인원(초과 시 앞 2명 + "외 N명") — plan §4.2
const CELL_NAME_LIMIT = 3;
const CELL_NAME_VISIBLE_ON_OVERFLOW = 2;

// 사용 단위(SYS025) 라벨 — plan §4.4 / 정책서 attd §8.5.9
const USE_UNIT_LABELS = {
  "00": "종일",
  "01": "반차",
  "02": "시간차",
  "03": "시간차",
  "04": "시간차",
  "05": "반반차",
};
// 시간대(START_TIME~END_TIME) 병기 대상 단위 = 시간차 3종
const HOURLY_UNIT_TYPES = ["02", "03", "04"];

// 조회 연월(YYYYMM) — 백엔드 searchYm 파라미터
const searchYm = computed(
  () => `${displayYear.value}${String(displayMonth.value).padStart(2, "0")}`
);

// ── 응답 가공 유틸 ────────────────────────────────────────

// "0900" → "09:00" (4자리 미만/빈값은 빈 문자열)
const fmtHhmm = (v) => {
  const s = String(v ?? "").replace(/\D/g, "");
  if (s.length < 4) return "";
  return `${s.slice(0, 2)}:${s.slice(2, 4)}`;
};

// 사용 단위 라벨. 미상/미정의 코드는 코드 원문 그대로 노출(plan §4.4)
const resolveUseUnitLabel = (useUnitType) => {
  const code = String(useUnitType ?? "");
  return USE_UNIT_LABELS[code] ?? code;
};

// 시간차(02/03/04)만 "HH:MM~HH:MM". 그 외/시각 결측이면 null(미표시)
const resolveTimeRange = (row) => {
  if (!HOURLY_UNIT_TYPES.includes(String(row?.useUnitType ?? ""))) return null;
  const start = fmtHhmm(row?.startTime);
  const end = fmtHhmm(row?.endTime);
  if (!start || !end) return null;
  return `${start}~${end}`;
};

// 행 목록에서 중복 제거된 사용자명(응답 순서=가나다 유지). 동일인이 같은 날
// 시간차 여러 건이어도 1명으로 계산한다(plan §4.2 distinct userCd).
const distinctUserNames = (rows) => {
  const seen = new Set();
  const names = [];
  for (const row of rows) {
    const key = String(row?.userCd ?? "");
    if (seen.has(key)) continue;
    seen.add(key);
    names.push(row?.userNm ?? "");
  }
  return names;
};

// dateYmd(YYYYMMDD) → 해당 일자 행 배열. 응답 순서를 그대로 보존한다.
const usageByDate = computed(() => {
  const map = new Map();
  for (const row of leaveUsageList.value) {
    const ymd = String(row?.dateYmd ?? "");
    if (!/^\d{8}$/.test(ymd)) continue;
    if (!map.has(ymd)) map.set(ymd, []);
    map.get(ymd).push(row);
  }
  return map;
});

// ── computed (plan §4.2/§4.3 계약) ────────────────────────

// 월 그리드 + 셀 데이터: 주 배열(7칸, 패딩 null). 셀 = { date, dateStr,
// dow, userCount(distinct 인원), visibleNames(3명 이하 전원 / 4명 이상 앞 2명),
// overflowCount(4명 이상일 때 userCount-2) }
const calendarWeeks = computed(() => {
  const y = displayYear.value;
  const m = displayMonth.value;
  const mm = String(m).padStart(2, "0");
  const first = new Date(y, m - 1, 1);
  const last = new Date(y, m, 0);
  const startPad = first.getDay();
  const totalDays = last.getDate();

  const cells = [];
  for (let i = 0; i < startPad; i++) cells.push(null);
  for (let d = 1; d <= totalDays; d++) {
    const dd = String(d).padStart(2, "0");
    const rows = usageByDate.value.get(`${y}${mm}${dd}`) ?? [];
    const names = distinctUserNames(rows);
    const userCount = names.length;
    const overflow = userCount > CELL_NAME_LIMIT;
    cells.push({
      date: d,
      dateStr: `${y}-${mm}-${dd}`,
      dow: new Date(y, m - 1, d).getDay(),
      userCount,
      visibleNames: overflow
        ? names.slice(0, CELL_NAME_VISIBLE_ON_OVERFLOW)
        : names,
      overflowCount: overflow ? userCount - CELL_NAME_VISIBLE_ON_OVERFLOW : 0,
    });
  }

  const weeks = [];
  for (let i = 0; i < cells.length; i += 7) {
    const week = cells.slice(i, i + 7);
    while (week.length < 7) week.push(null);
    weeks.push(week);
  }
  return weeks;
});

// 선택일 상세 카드 리스트(건 단위): { userNm, nodeNm, leaveNm,
// useUnitLabel(plan §4.4 SYS025 매핑), timeRange(시간차만 "HH:MM~HH:MM") }
const selectedDateDetails = computed(() => {
  if (!selectedDate.value) return [];
  const rows =
    usageByDate.value.get(selectedDate.value.replace(/-/g, "")) ?? [];
  return rows.map((row) => ({
    userCd: row?.userCd ?? "",
    userNm: row?.userNm ?? "",
    // 부서 미배정(NODE_CD NULL) 사용자는 null 로 넘겨 템플릿이 "-" 로 표시하게 한다.
    nodeNm: row?.nodeNm ? row.nodeNm : null,
    // 연차 종류명이 없으면(유형 마스터 미매칭) 코드 원문 노출
    leaveNm: row?.leaveNm ? row.leaveNm : (row?.leaveCd ?? ""),
    useUnitLabel: resolveUseUnitLabel(row?.useUnitType),
    timeRange: resolveTimeRange(row),
  }));
});

// 선택일 distinct 인원 수 (동일인 다건은 1명)
const selectedDateUserCount = computed(() => {
  const seen = new Set();
  for (const item of selectedDateDetails.value) {
    seen.add(String(item?.userCd ?? ""));
  }
  return seen.size;
});

// 선택일 라벨: "YYYY.MM.DD (요일)" — formatYmdDot 재사용(Attd_02 미러)
const selectedDateLabel = computed(() => {
  if (!selectedDate.value) return "";
  const d = selectedDate.value;
  const parts = d.split("-");
  if (parts.length < 3) return d;
  const [y, m, day] = parts;
  const dt = new Date(parseInt(y), parseInt(m) - 1, parseInt(day));
  return isNaN(dt.getTime())
    ? d
    : `${formatYmdDot(d)} (${dayLabels[dt.getDay()]})`;
});

// ── 표시 유틸 (UI 전용 — 로직 아님) ───────────────────────
const dayHeaderClass = (d) => {
  if (d === "일") return "hd-sun";
  if (d === "토") return "hd-sat";
  return "";
};

const cellClass = (cell) => {
  if (!cell) return "cell-empty";
  if (cell.dow === 0) return "cell-sun";
  if (cell.dow === 6) return "cell-sat";
  return "";
};

// ── 조회/이벤트 (developer 구현 대상) ─────────────────────

// 응답 역전/누락 방지 시퀀스(latest-wins). isLoading 으로 후속 호출을 버리면
//   조회 중 월을 한 번 더 넘겼을 때 그리드는 새 달인데 데이터는 이전 달이 되어
//   "전부 0명인 빈 달"이 조용히 표시된다(qa 결함 D2). 요청은 항상 보내고
//   마지막 요청의 응답만 채택한다.
let searchSeq = 0;

// 월별 연차 사용 현황 조회 (plan §3 API 계약)
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_INPUT_REQUIRED));
    return;
  }
  const seq = ++searchSeq;
  isLoading.value = true;

  try {
    const response = await axios.get("/webApi/attd16/leave-usage-calendar", {
      params: {
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        searchYm: searchYm.value,
      },
    });
    if (seq !== searchSeq) return; // 뒤늦게 도착한 이전 요청의 응답은 버린다
    if (response.status === 200) {
      leaveUsageList.value = response.data?.leaveUsageCalendarResultList ?? [];
    }
  } catch (err) {
    if (seq !== searchSeq) return; // stale 요청의 에러로 최신 결과를 지우지 않는다
    leaveUsageList.value = [];
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR))
    );
  } finally {
    if (seq === searchSeq) isLoading.value = false;
  }
};

const fnSelectDate = (cell) => {
  selectedDate.value = cell.dateStr;
};

// 월 이동/월 선택 후 재조회 — 조회 조건이 덜 채워진 상태에서는 얼럿 없이 건너뛴다
//   (qa 결함 D6: 사업장 미선택 상태의 월 네비가 얼럿을 띄우던 문제).
const fnSearchIfReady = () => {
  if (!canAutoSearch()) {
    leaveUsageList.value = [];
    return;
  }
  fnSearch();
};

const prevMonth = () => {
  if (displayMonth.value <= 1) {
    displayMonth.value = 12;
    displayYear.value--;
  } else displayMonth.value--;
  fnSearchIfReady();
};

const nextMonth = () => {
  if (displayMonth.value >= 12) {
    displayMonth.value = 1;
    displayYear.value++;
  } else displayMonth.value++;
  fnSearchIfReady();
};

const fnOpenMonthPicker = () => {
  openPop(CalendarMonthPickerPop, {
    year_p: displayYear.value,
    month_p: displayMonth.value,
    onConfirm: (y, m) => {
      displayYear.value = y;
      displayMonth.value = m;
      fnSearchIfReady();
    },
  });
};

// 사업장/부서 blur 자동조회 — Attd_07 focusKill 패턴 미러
//   값이 비어있으면 관련 필드 클리어 / 값이 있으면 짝 필드를 비우고 즉시 자동조회
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

// 사업장 자동조회 (코드/명 입력 후 blur) — Attd_07 미러
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
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// 부서 자동조회 (코드/명 입력 후 blur) — Attd_07 미러
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
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// 자동조회 응답 처리 — 0건/1건/다건 분기 (Attd_07 fnCallback 미러)
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
      // 사업장 변경 시 소속부서 초기화(부서는 사업장 종속 — 팝업 선택 경로와 정합)
      nodeCd.value = "";
      nodeNm.value = "";
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

const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

// 전사 부서 조회 가능 권한(서버 AuthRoleUtils.canManageAllNodes 와 동일 집합).
//   이 권한만 "부서 미지정 = 전체 부서" 조회가 서버 게이트를 통과한다.
const WHOLE_SITE_AUTH_CDS = ["master", "hr", "safe"];

// 초기값 세팅 — Attd_07 fnInit 미러.
//   기본 사업장 = 세션 gv_siteCd. 기본 부서는 권한에 따라 갈린다:
//   · 전사 권한(master/hr/safe) → 전체 부서(공란). 진입 즉시 사업장 전체가 조회된다.
//   · 그 외(부서 관리자 등) → 서버 canManageNode 게이트가 "부서 미지정" 조회를 403 으로 막으므로
//     본인 소속 부서(gv_nodeCd)를 프리필한다. 없으면 공란으로 두고 자동조회를 생략한다
//     (진입 즉시 403 얼럿 방지 — qa 지적: 게이트와 기본값 충돌).
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
  if (siteCd.value) {
    nodeDisabled.value = false;
  }

  const authCd = sessionStorage.getItem("gv_authCd") ?? "";
  if (!WHOLE_SITE_AUTH_CDS.includes(authCd)) {
    nodeCd.value = sessionStorage.getItem("gv_nodeCd") ?? "";
    nodeNm.value = nodeCd.value
      ? (sessionStorage.getItem("gv_nodeNm") ?? "")
      : "";
    // 하위부서까지 함께 보는 것이 부서 관리자의 기본 니즈(정·부 관리자는 하위 부서도 관리 범위).
    if (nodeCd.value) incSubNodeYn.value = "Y";
  }
};

// 자동 조회 가능 여부 — 사업장 필수. 비전사 권한은 부서까지 정해져야 서버 게이트를 통과한다.
const canAutoSearch = () => {
  if (proxy.$util.isEmpty(siteCd.value)) return false;
  const authCd = sessionStorage.getItem("gv_authCd") ?? "";
  if (WHOLE_SITE_AUTH_CDS.includes(authCd)) return true;
  return !proxy.$util.isEmpty(nodeCd.value);
};

onMounted(() => {
  fnButtonControll();
  fnInit();

  // 조회 조건 변경 시 자동 재조회(plan §4.5). 초기값 세팅 이후에 등록해야
  // fnInit 의 ref 변경으로 최초 조회가 두 번 발생하지 않는다.
  //   사업장이 비워진 상태(입력 클리어 등)에서는 재조회하지 않는다 — 사업장 필수 얼럿은
  //   사용자가 명시적으로 조회를 시도한 경우(조회 버튼)에만 띄운다(plan §4.5).
  watch([siteCd, nodeCd, incSubNodeYn], () => {
    fnSearchIfReady();
  });

  // 최초 조회 — 조회 조건이 갖춰진 경우에만 자동 조회(진입 즉시 얼럿/403 방지)
  fnSearchIfReady();
});
</script>

<style scoped>
/* Attd_02 관례 미러. 토큰 부재 색(주말/태그 틴트)은 지역 변수로 정의(하드코딩 산재 방지) */
.attd16-body {
  --a16-sun: #dc2626; /* Attd_02 .cell-sun 미러 */
  --a16-sat: #2563eb; /* Attd_02 .cell-sat 미러 */
  --a16-tag-bg: #d1fae5; /* Attd_02 .ev-manual 틴트 미러 */
  --a16-tag-text: #065f46;

  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.attd16-main {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  gap: 1rem;
  overflow: hidden;
}

/* 조회 영역 — 하위부서 체크 (Attd_07 미러) */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  user-select: none;
}
.checkbox-label input[type="checkbox"] {
  width: 13px;
  height: 13px;
  cursor: pointer;
  accent-color: var(--color-primary, #16a34a);
  flex-shrink: 0;
}

/* 캘린더 */
.attd16-calendar {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  padding: 1rem;
  background: var(--color-surface, #fff);
  overflow: hidden;
}
.calendar-header {
  flex-shrink: 0;
}
.calendar-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.5rem;
}
.calendar-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0;
  color: var(--color-text-strong, #111827);
}
.calendar-nav-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.calendar-nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.btn-nav {
  padding: 0.35rem 0.6rem;
  border: 1px solid var(--color-border-strong, #d1d5db);
  border-radius: 6px;
  background: var(--color-surface, #fff);
  cursor: pointer;
  font-size: 0.875rem;
}
.calendar-month {
  font-weight: 600;
  font-size: 1rem;
  min-width: 7rem;
  text-align: center;
}
.calendar-month-clickable {
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  transition: background 0.2s;
}
.calendar-month-clickable:hover {
  background: var(--color-focus-ring, rgba(22, 163, 74, 0.08));
}
.calendar-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
}
.calendar-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
  table-layout: fixed;
}
.calendar-table th,
.calendar-table td {
  border: 1px solid var(--color-border, #e5e7eb);
  padding: 0.35rem;
  vertical-align: top;
}
.calendar-table thead th {
  background: var(--color-bg, #f9fafb);
  font-weight: 600;
}
.hd-sun {
  color: var(--a16-sun);
}
.hd-sat {
  color: var(--a16-sat);
}
.cell-empty {
  background: var(--color-bg, #fafafa);
}
.cell-sun {
  color: var(--a16-sun);
}
.cell-sat {
  color: var(--a16-sat);
}
.cell-inner {
  min-height: 90px;
  height: 90px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  cursor: pointer;
}
.cell-selected .cell-inner {
  outline: 2px solid var(--color-primary, #16a34a);
  outline-offset: 5px;
}
.cell-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.25rem;
  margin-bottom: 0.25rem;
  flex-shrink: 0;
}
.cell-num {
  font-weight: 500;
}
.cell-count-badge {
  font-size: 0.7rem;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 9999px;
  background: var(--color-primary, #16a34a);
  color: var(--color-surface, #fff);
  white-space: nowrap;
}
.cell-users {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}
.user-tag {
  font-size: 0.7rem;
  padding: 2px 6px;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: var(--a16-tag-bg);
  color: var(--a16-tag-text);
}
.user-tag-more {
  background: var(--color-bg, #f9fafb);
  color: var(--color-text-muted, #6b7280);
}

/* 우측 패널 */
.attd16-panel {
  width: 340px;
  flex-shrink: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  background: var(--color-surface, #fff);
  overflow: hidden;
}
.panel-header {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  flex-shrink: 0;
}
.panel-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 1rem;
}
.detail-date {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  margin-bottom: 0.75rem;
  flex-shrink: 0;
}
.detail-count {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  background: var(--color-primary, #16a34a);
  color: var(--color-surface, #fff);
}
.detail-cards {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.detail-card {
  padding: 0.75rem 1rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  flex-shrink: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}
.card-title {
  font-size: 0.9375rem;
  font-weight: 600;
  margin: 0;
  color: var(--color-text-strong, #111827);
}
.card-node {
  font-size: 0.8125rem;
  color: var(--color-text-muted, #6b7280);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-info-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.card-tag {
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.75rem;
}
.tag-leave {
  background: var(--a16-tag-bg);
  color: var(--a16-tag-text);
}
.card-unit {
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
}
.card-time {
  font-size: 0.8125rem;
  color: var(--color-text-muted, #6b7280);
}
.detail-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80px;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  text-align: center;
}
</style>
