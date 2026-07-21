<template>
  <!-- 웹 관리자 대시보드 (PRAFTA-DASHBOARD-T1 골격)
       - 근태·안전 위젯 통합 배치 + 상단 조회(사업장/부서/월). 화면 내 탭은 없다.
       - 부서/하위부서는 근태 위젯만 사용한다(안전 위젯은 사업장/월만 소비).
       - 위젯 이동 버튼 → dashboardNavStore.requestOpen(대상 라우트명, 조회조건) → MainLayout이 탭 오픈 -->
  <div class="viewComm dash-view">
    <!-- 헤더: 타이틀 + 현재 사업장 + ⚙조회조건 토글 -->
    <div class="dash-header">
      <span class="dash-title">대시보드</span>
      <div class="dash-header__right">
        <span v-if="siteNm" class="dash-header__site" :title="siteNm">
          {{ siteNm }}
        </span>
        <span v-else class="dash-header__site is-empty">사업장 미선택</span>
        <button
          type="button"
          class="dash-gear"
          :class="{ 'is-open': showFilter }"
          @click="showFilter = !showFilter"
        >
          ⚙ 조회조건
        </button>
      </div>

      <!-- 조회 조건 드롭다운 — 공간 절약을 위해 ⚙ 뒤로 숨김. 닫히면 세로 공간 0(떠 있는 패널) -->
      <div v-if="showFilter" class="dash-filter">
        <div class="dash-filter__row">
          <label>사업장</label>
          <input
            id="siteNo"
            type="text"
            v-model="siteNo"
            placeholder="사업장코드"
            @blur="focusKill"
          />
          <button class="search-btn" @click="fnSiteSearchPopOpen">
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

        <div class="dash-filter__row">
          <label>조직부서</label>
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
            @click="fnSiteNodeSearchPopOpen"
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

        <div class="dash-filter__row dash-filter__row--foot">
          <label class="checkbox-label">
            <input type="checkbox" v-model="incSubNodeYn" />
            하위부서 조회
          </label>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 통합 본문: 무사고 배너(상단 풀폭) / 근태 4개(좌)·안전 4개(우) / 초과근무(하단 풀폭).
         자식 컴포넌트 루트를 display:contents 로 해제 → 위젯이 이 그리드의 직접 아이템이 된다.
         위치(레이아웃)는 .dash-body 의 grid-template-areas 한 곳에서만 제어. -->
    <div class="viewBody dash-body">
      <DashboardAttdTab
        :search-params="searchTicket"
        v-model:base-ym="baseYm"
        @move="fnMove"
      />
      <DashboardSafetyTab
        :ticket="searchTicket"
        :site-nm="siteNm"
        @move="fnMove"
      />
      <!-- 근태 ↔ 안전 영역 구분 세로 디바이더 (중간 2행만 차지) -->
      <div class="dash-divider" aria-hidden="true"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, getCurrentInstance } from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useDashboardNavStore } from "@/stores/dashboardNavStore";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import DashboardAttdTab from "./components/DashboardAttdTab.vue";
import DashboardSafetyTab from "./components/DashboardSafetyTab.vue";
import search_icon from "@/assets/img/search_icon.png";

// 홈 라우트명("Dashboard")과 동일하게 두어 MainLayout keep-alive 캐시 대상이 되게 한다.
defineOptions({ name: "Dashboard" });

// 홈 탭은 buttons가 비어 오지만 관례상 수용 (ViewHeader 미사용)
defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();
const dashNav = useDashboardNavStore();

// ── 조회조건 드롭다운(⚙) 표시 상태 (공간 절약 — 기본 닫힘) ──
const showFilter = ref(false);

// 안전 위젯 이동 대상 라우트 (사업장만 물고 이동 — 부서 파라미터 제외).
//   그 외(근태 위젯)는 부서까지 물고 이동. 위젯 종류를 이 집합으로 판별한다.
const SAFETY_ROUTES = new Set(["ChkLst_03", "Risk_03", "Acct_01", "Tbm_04"]);

// ── 상단 조회 조건 ─────────────────────────────────────────
// 사업장/기준월 = 근태·안전 위젯 공용, 부서 = 근태 위젯 전용
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);

// 기준월 (YYYY-MM), 기본값 = 현재월.
//   UI(화살표+피커)는 '근무 계획 등록율' 위젯 안에 있고, v-model:base-ym 로 양방향 동기.
//   위젯에서 월을 바꾸면 이 값이 갱신되며, 전체 조회(fnSearch)·위젯 이동(fnMove)이 이 월을 쓴다.
const currentYm = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
};
const baseYm = ref(currentYm());

// 조회 티켓 — 자식 위젯이 watch 로 소비 (T4.plan §5-1 / T5.plan §2 공용 규약)
//   ts 포함 새 객체로 교체 발행 → 같은 조건 재조회도 트리거된다
const searchTicket = ref(null);

// ── developer 구현 영역 ───────────────────────────────────

// 초기값 세팅 — 로그인 사용자 소속 기준 (Attd_05 fnInit 패턴).
//   사업장 = gv_siteCd/gv_siteNo/gv_siteNm, 부서 = gv_nodeCd/gv_nodeNm.
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
  if (siteCd.value) {
    nodeDisabled.value = false;

    if (proxy.$util.isEmpty(sessionStorage.getItem("gv_nodeCd"))) {
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      nodeCd.value = sessionStorage.getItem("gv_nodeCd");
      nodeNm.value = sessionStorage.getItem("gv_nodeNm");
    }
  }
};

// ── 사업장 / 부서 포커스 처리 (Attd_07 focusKill 패턴 이식) ──
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
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
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
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// 자동조회 응답 처리 — 0건/1건/다건 분기 (Attd_07 fnCallback 패턴)
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

// 사업장 팝업 선택 반영 — 사업장 변경 시 부서는 초기화 (Attd_07 onSiteSelected 패턴)
const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  nodeDisabled.value = false;
  nodeCd.value = "";
  nodeNm.value = "";
};

// 사업장 검색 팝업 (Attd_07 패턴)
const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

// 부서 검색 팝업 (Attd_07 패턴) — 사업장 미선택 시 안내 후 중단
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

// 조회 티켓 발행 — 현재 조회조건 스냅샷을 새 객체로 교체 (자식 위젯 watch 트리거)
//   근태 위젯(AttdTab)·안전 위젯(SafetyTab)이 같은 티켓 객체를 각자 계약대로 소비한다
const publishTicket = () => {
  searchTicket.value = {
    siteCd: siteCd.value,
    siteNo: siteNo.value,
    siteNm: siteNm.value,
    nodeCd: nodeCd.value,
    nodeNm: nodeNm.value,
    incSubNodeYn: incSubNodeYn.value,
    ym: baseYm.value,
    ts: Date.now(),
  };
};

// 조회 — 검증 통과 시 조회 티켓 발행 (PRAFTA-DASHBOARD-T4 §5-1). 성공 시 드롭다운 닫음.
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_REQUIRED_FIRST));
    return;
  }
  publishTicket();
  showFilter.value = false;
};

// 사업장 전환(팝업 선택/blur 단건 매칭) 시 자동 재조회 → 전체 위젯 재조회.
//   ※ 기준월(baseYm) 변경은 여기서 제외 — 월 이동은 '근무 계획 등록율' 위젯만 refresh 한다
//     (전체 재조회는 상단 [조회] 버튼으로만). siteCd 비어 있으면 무시(자동 트리거는 alert 금지 — silent)
watch(siteCd, () => {
  if (proxy.$util.isEmpty(siteCd.value)) return;
  publishTicket();
});

// 위젯 이동 규약 — dashboardNavStore.requestOpen(routeName, params)
//   - 근태 위젯: { siteCd, siteNo, siteNm, nodeCd, nodeNm, incSubNodeYn, ym: baseYm }
//   - 안전 위젯(SAFETY_ROUTES): { siteCd, siteNo, siteNm, ym: baseYm }
//   - Attd_10(내 결재함)은 조회조건 수신 지점이 없어 파라미터 없이 이동만 (T1 확정)
//   상세: PRAFTA-DASHBOARD-T1.plan.md §1 이동 규약
const fnMove = (routeName) => {
  if (routeName === "Attd_10") {
    dashNav.requestOpen(routeName);
    return;
  }

  // 조회조건을 물고 이동하는 화면은 사업장이 있어야 의미가 있다
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED_FIRST));
    return;
  }

  const params = SAFETY_ROUTES.has(routeName)
    ? {
        siteCd: siteCd.value,
        siteNo: siteNo.value,
        siteNm: siteNm.value,
        ym: baseYm.value,
      }
    : {
        siteCd: siteCd.value,
        siteNo: siteNo.value,
        siteNm: siteNm.value,
        nodeCd: nodeCd.value,
        nodeNm: nodeNm.value,
        incSubNodeYn: incSubNodeYn.value,
        ym: baseYm.value,
      };

  dashNav.requestOpen(routeName, params);
};

onMounted(() => {
  fnInit();
  // 초기 1회 자동 조회 — watch immediate 는 fnInit 이전 빈 값에 발화하므로 명시 트리거가 안전
  if (siteCd.value) publishTicket();
});
</script>

<style scoped>
.dash-view {
  padding: 0.625rem 1rem;
  /* 한 화면(무스크롤) 우선 — 넘칠 때만 스크롤 */
  overflow-y: auto;
}

/* ── 헤더 (타이틀 + 현재 사업장 + ⚙조회조건) ─────────────────── */
.dash-header {
  position: relative;
  display: flex;
  align-items: center;
  gap: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}

.dash-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-strong, #111827);
}

.dash-header__right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.dash-header__site {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text, #374151);
}

.dash-header__site.is-empty {
  font-weight: 400;
  color: var(--color-text-muted, #9ca3af);
}

.dash-gear {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  height: var(--btn-height, 29px);
  padding: 0 0.75rem;
  border: 1px solid var(--color-border-strong, #d1d5db);
  background: var(--color-surface, #ffffff);
  border-radius: var(--btn-radius, 8px);
  color: var(--color-text-muted, #4b5563);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.dash-gear:hover,
.dash-gear.is-open {
  color: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
}

/* ── 조회조건 드롭다운 (떠 있는 패널 — 닫히면 레이아웃 높이 0) ── */
.dash-filter {
  position: absolute;
  top: 100%;
  right: 0;
  z-index: 30;
  margin-top: 0.375rem;
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  background: var(--color-surface, #ffffff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--card-radius, 12px);
  box-shadow: var(--card-shadow, 0 4px 16px rgba(16, 24, 40, 0.12));
}

.dash-filter__row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dash-filter__row > label:first-child {
  width: 3.5rem;
  flex-shrink: 0;
  font-size: 0.8125rem;
  color: var(--color-text-muted, #4b5563);
}

.dash-filter__row--foot {
  justify-content: space-between;
  padding-top: 0.25rem;
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted, #4b5563);
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}

.checkbox-label input[type="checkbox"] {
  width: 13px;
  height: 13px;
  cursor: pointer;
  accent-color: var(--color-primary, #16a34a);
  flex-shrink: 0;
}

/* ── 통합 본문 그리드 (배너 상단 / 근태·안전 좌우 / 초과근무 하단) ──
   자식 컴포넌트 루트(display:contents)를 통해 위젯이 이 그리드의 직접 아이템이 된다.
   위치는 여기 grid-template-areas 한 곳에서만 바꾸면 된다. */
.dash-body {
  padding: 0.625rem 0.25rem;
  background: transparent;
  display: grid;
  grid-template-columns: 1fr 1fr 16px 1fr 1fr;
  grid-template-areas:
    "banner banner banner banner banner"
    "plan   attd   gut    patrol risk"
    "leave  req    gut    acct   tbm"
    "ot     ot     ot     ot     ot";
  gap: 0.625rem;
  align-content: start;
  overflow-y: auto;
}

/* 자식 컴포넌트 루트(.dash-attd-grid/.dash-safety-grid)는 각 컴포넌트 CSS 에서 display:contents 로
   해제되어, 그 위젯 카드들이 이 부모 그리드의 직접 아이템(각자 grid-area 보유)이 된다. */

/* 근태 ↔ 안전 영역 구분 세로 디바이더 (중간 2행 gut 영역) */
.dash-divider {
  grid-area: gut;
  width: 1px;
  justify-self: center;
  background: var(--color-border, #e5e7eb);
}

/* 좁은 화면: 좌우 분할 해제 → 2열, 배너/초과근무는 풀폭 유지 */
@media (max-width: 1200px) {
  .dash-body {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-areas:
      "banner banner"
      "plan   attd"
      "leave  req"
      "patrol risk"
      "acct   tbm"
      "ot     ot";
  }
  .dash-divider {
    display: none;
  }
}

@media (max-width: 640px) {
  .dash-body {
    grid-template-columns: 1fr;
    grid-template-areas:
      "banner"
      "plan"
      "attd"
      "leave"
      "req"
      "patrol"
      "risk"
      "acct"
      "tbm"
      "ot";
  }
}
</style>
