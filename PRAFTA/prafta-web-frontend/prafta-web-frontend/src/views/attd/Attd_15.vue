<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 조회 영역 (Attd_08.vue 사업장/소속부서/하위부서/사용자명 패턴 그대로 재사용) -->
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
          <input type="checkbox" v-model="incSubNodeYn" :disabled="!nodeCd" />
          하위부서 조회
        </label>
      </div>
      <div>
        <label>사용자명</label>
        <input v-model.trim="searchUserNm" type="text" />
      </div>
    </div>

    <!-- 주 네비게이터: Attd_08 .a08-toolbar/.a08-date-nav 패턴을 1일→1주 이동으로 변형 -->
    <div class="a15-toolbar">
      <div class="a15-week-nav">
        <button
          type="button"
          class="a15-week-arr"
          @click="fnWeekPrev"
          aria-label="이전 주"
        >
          ‹
        </button>
        <CalendarSrch :range="false" v-model="weekAnchorDate" />
        <button
          type="button"
          class="a15-week-arr"
          @click="fnWeekNext"
          aria-label="다음 주"
        >
          ›
        </button>
      </div>
      <div class="a15-week-range">
        {{ fmtYmdDisplay(weekStartYmd) }} ({{ fmtDow(weekStartYmd) }}) ~
        {{ fmtYmdDisplay(weekEndYmd) }} ({{ fmtDow(weekEndYmd) }})
      </div>
    </div>

    <!-- 결과 테이블 -->
    <div class="table-wrapper subtitle-pane">
      <div class="viewBody a15-body">
        <div class="a15-table-wrap">
          <table class="a15-table">
            <thead>
              <tr>
                <th>사업장</th>
                <th>부서</th>
                <th>사용자명</th>
                <th>대상 주</th>
                <th>스케줄기준</th>
                <th>실제기준</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="rows.length === 0">
                <td colspan="6" class="a15-empty">조회 결과가 없습니다.</td>
              </tr>
              <tr v-for="r in rows" :key="r.userCd">
                <td>{{ r.siteNm }}</td>
                <td>{{ r.nodeNm }}</td>
                <td>{{ r.userNm }} ({{ r.userId }})</td>
                <td>
                  {{ fmtYmdDisplay(r.weekStartYmd) }} ~
                  {{ fmtYmdDisplay(r.weekEndYmd) }}
                </td>
                <td>
                  <span class="a15-cell-minutes">{{
                    fmtDuration(r.scheduledMinutes)
                  }}</span>
                  <span
                    :class="['a15-badge', statusBadgeClass(r.scheduledStatus)]"
                  >
                    {{ statusLabel(r.scheduledStatus) }}
                  </span>
                </td>
                <td>
                  <span class="a15-cell-minutes">{{
                    fmtDuration(r.actualMinutes)
                  }}</span>
                  <span
                    :class="['a15-badge', statusBadgeClass(r.actualStatus)]"
                  >
                    {{ statusLabel(r.actualStatus) }}
                  </span>
                  <span v-if="r.provisionalYn === 'Y'" class="a15-provisional">
                    잠정치
                  </span>
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
import {
  ref,
  watch,
  onMounted,
  getCurrentInstance,
  defineProps,
  defineOptions,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { useModal } from "@/utils/useModal";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";

defineOptions({ name: "Attd_15" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });

// 조회 버튼만 노출 (엑셀/생성/저장/삭제는 요청서에 명시되지 않아 미도입 — TODO(developer): 필요 시 활성화)
(() => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
})();

// ── 조회 조건 (Attd_08.vue 사업장/소속부서 패턴 그대로 이식) ──────────────
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

// Attd_08.vue 의 focusKill / fnSrchSiteInfo / fnSrchNodeInfo / fnCallback /
//   onSiteSelected / fnSiteSearchPopOpen / fnSiteNodeSearchPopOpen 함수를
//   변수명 100% 동일하게 이식한다(plan.md ATTD15-T2 참조).
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

// ── 주 네비게이터 ──────────────────────────────────────────────────────
// weekAnchorDate: 사용자가 선택한 임의 날짜(기본값 오늘). weekStartYmd/weekEndYmd는
//   이 날짜가 속한 주의 월요일/일요일로 파생 계산한다(주 기준 월~일 고정 — 사용자 결정 §2.1).
const weekAnchorDate = ref(toIsoDate(new Date()));
const weekStartYmd = ref("");
const weekEndYmd = ref("");

// ISO(YYYY-MM-DD) 변환 헬퍼 (Attd_08 toIsoDate 패턴 차용)
function toIsoDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}
// YYYYMMDD 변환 헬퍼
function toYmd(d) {
  return toIsoDate(d).replaceAll("-", "");
}
// 기간 픽커 하루 단위 이동 헬퍼 (Attd_08 shiftIsoDate 패턴 차용)
function shiftIsoDate(iso, deltaDays) {
  const base = iso ? iso.split("-").map(Number) : null;
  const dt = base ? new Date(base[0], base[1] - 1, base[2]) : new Date();
  dt.setDate(dt.getDate() + deltaDays);
  return toIsoDate(dt);
}

// weekAnchorDate 가 변경될 때마다(초기 mount 포함, immediate) 그 주의 월요일/일요일을
//   계산해 weekStartYmd/weekEndYmd(YYYYMMDD)를 갱신한다(주 기준 월~일 고정 — 사용자 결정 §2.1).
function recomputeWeekRange() {
  const iso = weekAnchorDate.value;
  if (!iso) return;
  const [y, m, d] = iso.split("-").map(Number);
  const anchor = new Date(y, m - 1, d);
  const dow = anchor.getDay(); // 0=일 ~ 6=토
  const diffToMonday = dow === 0 ? -6 : 1 - dow;
  const monday = new Date(anchor);
  monday.setDate(monday.getDate() + diffToMonday);
  const sunday = new Date(monday);
  sunday.setDate(sunday.getDate() + 6);
  weekStartYmd.value = toYmd(monday);
  weekEndYmd.value = toYmd(sunday);
}
watch(weekAnchorDate, recomputeWeekRange, { immediate: true });

// 이전/다음 주 이동 (Attd_08 fnFromPrev/fnFromNext 패턴을 7일 단위로 변형)
const fnWeekPrev = () => {
  weekAnchorDate.value = shiftIsoDate(weekAnchorDate.value, -7);
};
const fnWeekNext = () => {
  weekAnchorDate.value = shiftIsoDate(weekAnchorDate.value, 7);
};

// ── 결과 ──────────────────────────────────────────────────────────────
const rows = ref([]);

// 조회 실행
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert("사업장을 선택해 주세요.");
    return;
  }
  // ATTD15-T1 API 호출: 사업장/소속부서(+하위)/사용자명/대상 주(월요일) 조건으로 조회.
  try {
    const response = await axios.get("/webApi/attd15/weekly-52h-lists", {
      params: {
        siteCd: siteCd.value,
        nodeCd: nodeCd.value || "",
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        userNm: searchUserNm.value || "",
        weekStartYmd: weekStartYmd.value,
      },
    });
    if (response.status === 200) {
      rows.value = response.data?.weekly52hListsResultList ?? [];
    }
  } catch (err) {
    console.error("[Attd_15] search failed", err);
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// ── 표시 헬퍼 ─────────────────────────────────────────────────────────
// yyyyMMdd → "MM.DD" 표기 (Attd_08 fmtYmd/formatYmdDot 계열과 톤 통일)
const fmtYmdDisplay = (ymd) => {
  const s = String(ymd ?? "");
  if (s.length !== 8) return "-";
  return `${s.slice(4, 6)}.${s.slice(6, 8)}`;
};
// yyyyMMdd → 요일 라벨
const dowLabels = ["일", "월", "화", "수", "목", "금", "토"];
const fmtDow = (ymd) => {
  const s = String(ymd ?? "");
  if (s.length !== 8) return "";
  const day = new Date(
    Number(s.slice(0, 4)),
    Number(s.slice(4, 6)) - 1,
    Number(s.slice(6, 8))
  ).getDay();
  return dowLabels[day] ?? "";
};
// 분 → "N시간 M분" 표기 (Attd_08 fmtDuration 과 동일 톤)
const fmtDuration = (min) => {
  if (min == null) return "-";
  const m = Math.max(0, Math.round(min));
  const h = Math.floor(m / 60);
  const mm = m % 60;
  if (h > 0 && mm > 0) return `${h}시간 ${mm}분`;
  if (h > 0) return `${h}시간`;
  return `${mm}분`;
};
// 상태 코드(NORMAL/CAUTION/DANGER/EXCESS) → 라벨/뱃지 클래스
//   경계값은 plan.md ATTD15-T1 §7 정의를 서버가 이미 판정해 내려준다(프론트는 표시만 함).
const statusLabel = (cd) => {
  switch (cd) {
    case "CAUTION":
      return "주의";
    case "DANGER":
      return "위험";
    case "EXCESS":
      return "초과";
    case "NORMAL":
    default:
      return "정상";
  }
};
const statusBadgeClass = (cd) => {
  switch (cd) {
    case "CAUTION":
      return "status-caution";
    case "DANGER":
      return "status-danger";
    case "EXCESS":
      return "status-excess";
    case "NORMAL":
    default:
      return "status-normal";
  }
};

// 진입 시 sessionStorage 의 사업장 정보로 초기화 (Attd_08 fnInit 패턴 차용)
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
  if (siteCd.value) {
    nodeDisabled.value = false;
  }
};

onMounted(() => {
  fnInit();
});
</script>

<style scoped>
/* 주 네비게이터 툴바 (Attd_08 .a08-toolbar 패턴 차용) */
.a15-toolbar {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg);
  flex-wrap: wrap;
}
.a15-week-nav {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}
.a15-week-nav :deep(.calendar-input) {
  height: 28px;
  padding: 0 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-strong);
  background: var(--color-surface);
  cursor: pointer;
  text-align: center;
  min-width: 120px;
}
.a15-week-nav :deep(.calendar-input:hover) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.a15-week-arr {
  width: 24px;
  height: 24px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  border-radius: var(--btn-radius);
  cursor: pointer;
  color: var(--color-text-muted);
  font-size: 0.875rem;
  line-height: 1;
  flex: 0 0 auto;
}
.a15-week-arr:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}
.a15-week-range {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-strong);
  margin-inline-start: 0.4rem;
}

/* 결과 테이블 영역 (Attd_08 .a08-body/.a08-table-wrap 패턴 차용, 상세 패널 없음) */
.a15-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
.a15-table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  background: var(--color-surface);
}
.a15-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}
.a15-table thead th {
  position: sticky;
  top: 0;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.4rem;
  line-height: 1.2;
  box-sizing: border-box;
  z-index: 1;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text);
  font-weight: 600;
}
.a15-table tbody td {
  border: 1px solid var(--color-border);
  padding: 0.4rem;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text);
}
.a15-table tbody tr:hover {
  background: var(--color-bg);
}
/* 전역 표준 .edu-grid-empty(table.css)와 동일 높이 — .a15-table tbody td 패딩을 이기기 위해 !important 유지 */
.a15-empty {
  padding: 1.2rem 0.5rem !important;
  color: var(--color-text-muted);
  text-align: center;
}
.a15-cell-minutes {
  margin-right: 0.4rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

/* 상태 뱃지 4단계 — tokens.css 에 "위험" 전용 색상 토큰이 없어(plan.md 확인필요 ③)
   color-mix() 로 기존 3개 토큰(primary/warning/danger)만 조합해 파생한다.
   신규 하드코딩 hex 없음 — 전부 var() 소스만 사용. */
.a15-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
}
.status-normal {
  background: color-mix(in srgb, var(--color-primary) 15%, var(--color-surface));
  color: var(--color-primary-pressed);
}
.status-caution {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}
.status-danger {
  background: color-mix(in srgb, var(--color-danger) 35%, var(--color-warning-bg));
  color: color-mix(in srgb, var(--color-danger) 70%, var(--color-text-strong));
}
.status-excess {
  background: var(--color-danger);
  color: var(--color-surface);
}

/* 잠정치 보조 뱃지 — 중립색(마감 전 데이터임을 알리는 정보성 표식, 위험도색과 무관) */
.a15-provisional {
  display: inline-block;
  margin-left: 0.3rem;
  padding: 0.1rem 0.4rem;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 600;
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border-strong);
}

/* Attd_08 패턴: 소속부서 입력 뭉치와 체크박스 라벨 간격 보정 */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
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
  accent-color: var(--color-primary);
  flex-shrink: 0;
}
</style>
