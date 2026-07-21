<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @excel="fnExcel"
    />

    <!-- 조회 영역 (Attd_07 패턴 차용: 사업장/소속부서/하위부서/사용자명) -->
    <div class="viewSearch">
      <!-- 조회월: 단일 월 (PRAFTA-034 §3-5 단일 월 조회) -->
      <div>
        <label>조회월</label>
        <CalendarSrchMonth
          :range="false"
          style="width: 100px"
          v-model="workYm"
        />
      </div>
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
        <input v-model.trim="searchUserNm" type="text" placeholder="사용자명" />
      </div>
    </div>

    <!-- 본문: 사용자 1명 = 1행 (월별 근태 판정 요약) -->
    <div class="viewBody a11-body">
      <div class="table-wrapper subtitle-pane a11-subtitle-pane">
        <!-- 소제목 바 (User_01 패턴 차용) -->
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">사용자 리스트</span>
          </div>
        </div>
        <div class="a11-table-wrap">
          <table class="a11-table">
            <thead>
              <tr>
                <th rowspan="2">사번</th>
                <th rowspan="2">이름</th>
                <th rowspan="2">부서</th>
                <th rowspan="2">직책</th>
                <th rowspan="2">근무일수</th>
                <th rowspan="2">총 근무시간</th>
                <th rowspan="2">초과근무시간</th>
                <th colspan="2">지각</th>
                <th colspan="2">조퇴</th>
                <th rowspan="2">미출근</th>
              </tr>
              <tr>
                <th>횟수</th>
                <th>시간 누계</th>
                <th>횟수</th>
                <th>시간 누계</th>
              </tr>
            </thead>
            <tbody>
              <!-- 조회 결과 0건 -->
              <tr v-if="rows.length === 0">
                <td colspan="12" class="a11-empty">조회 결과가 없습니다.</td>
              </tr>
              <tr v-for="r in rows" :key="r.userCd">
                <td>{{ r.userId }}</td>
                <td class="a11-cell-left">{{ r.userNm }}</td>
                <td class="a11-cell-left">{{ r.deptNm }}</td>
                <td>{{ r.authNm }}</td>
                <!-- 근무일수 / 횟수: 숫자 -->
                <td class="a11-cell-num">{{ fmtCount(r.workDayCnt) }}</td>
                <!-- 시간 컬럼: "N시간 M분" -->
                <td class="a11-cell-num">{{ fmtMinutes(r.workMinutes) }}</td>
                <td class="a11-cell-num">{{ fmtMinutes(r.otMinutes) }}</td>
                <td class="a11-cell-num">{{ fmtCount(r.lateCnt) }}</td>
                <td class="a11-cell-num">{{ fmtMinutes(r.lateMinutes) }}</td>
                <td class="a11-cell-num">{{ fmtCount(r.earlyLeaveCnt) }}</td>
                <td class="a11-cell-num">
                  {{ fmtMinutes(r.earlyLeaveMinutes) }}
                </td>
                <td class="a11-cell-num">{{ fmtCount(r.absentDayCnt) }}</td>
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
  computed,
  getCurrentInstance,
  defineProps,
  defineOptions,
  onMounted,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CalendarSrchMonth from "@/components/common/CalendarSrchMonth.vue";
import { useModal } from "@/utils/useModal";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { exportStyledExcel } from "@/utils/excelExport";

defineOptions({ name: "Attd_11" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ── 헤더 버튼 (조회 전용 화면 — 생성/저장/삭제만 숨김, 엑셀 노출) ──
const localButtons = ref({ ...props.buttons });
const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "Y";
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
// 단일 월 (YYYY-MM). PRAFTA-034 §3-5 — from~to 범위 아님.
const workYm = ref(currentYm());

// ── 조회 결과 (사용자 1명 = 1행) ─────────────────────────
// 각 행 형태(PRAFTA-034 §9 응답):
//   { userCd, userId, userNm, deptNm, authCd, authNm,
//     workDayCnt, workMinutes, otMinutes,
//     lateCnt, lateMinutes, earlyLeaveCnt, earlyLeaveMinutes, absentDayCnt }
const rows = ref([]);

// PRAFTA-028 - master/hr 여부 (그 외 권한은 사업장+소속부서 필수)
const isMasterOrHr = computed(() => {
  const a = sessionStorage.getItem("gv_authCd");
  return a === "master" || a === "hr";
});

// ── 월 유틸 ───────────────────────────────────────────────
function currentYm() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

// ── 표시 포맷 헬퍼 ────────────────────────────────────────
// 분(min) → "N시간 M분" 표기. 0/빈값은 "0시간 0분".
const fmtMinutes = (min) => {
  const m = parseInt(min, 10);
  if (isNaN(m) || m <= 0) return "0시간 0분";
  const h = Math.floor(m / 60);
  const rm = m % 60;
  return `${h}시간 ${rm}분`;
};
// 횟수/일수 → 숫자(빈값은 0).
const fmtCount = (n) => {
  const v = parseInt(n, 10);
  return isNaN(v) ? 0 : v;
};

// ── 사업장 / 부서 포커스 처리 (Attd_07 패턴 차용) ──────────
//   값이 비어있으면 관련 필드 클리어 / 값이 있으면 짝 필드 비우고 자동조회
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

// 자동조회 응답 처리 — 0건/1건/다건 분기 (Attd_07 fnCallback 패턴 차용)
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

// 사업장 검색 팝업 선택 콜백
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

// ── 조회 ──────────────────────────────────────────────────
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_INPUT_REQUIRED));
    siteNoFcs.value?.focus();
    return;
  }

  // PRAFTA-028 / PRAFTA-034 §7 - master/hr 이 아니면 사업장+소속부서 필수
  if (!isMasterOrHr.value && proxy.$util.isEmpty(nodeCd.value)) {
    await proxy.$alert("소속 부서를 선택해 주세요.");
    return;
  }

  try {
    const response = await axios.get("/webApi/attd11/monthly-attd-summary", {
      params: {
        workYm: workYm.value,
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        userNm: searchUserNm.value,
      },
    });

    if (response.status === 200) {
      rows.value = response.data?.monthlyAttdSummaryResultList ?? [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR));
    await proxy.$alert(msg);
  }
};

// ── 엑셀 다운로드 ─────────────────────────────────────────
//   화면 테이블과 동일한 컬럼/포맷으로 내보낸다. 시간 컬럼은 "N시간 M분" 그대로 표기.
const fnExcel = async () => {
  if (rows.value.length === 0) {
    await proxy.$alert("내보낼 데이터가 없습니다.");
    return;
  }
  const columns = [
    { header: "사번", fixed: false, width: 14 },
    { header: "이름", fixed: false, width: 12 },
    { header: "부서", fixed: false, width: 18 },
    { header: "직책", fixed: false, width: 12 },
    { header: "근무일수", fixed: false, width: 10 },
    { header: "총 근무시간", fixed: false, width: 14 },
    { header: "초과근무시간", fixed: false, width: 14 },
    { header: "지각 횟수", fixed: false, width: 10 },
    { header: "지각 시간 누계", fixed: false, width: 14 },
    { header: "조퇴 횟수", fixed: false, width: 10 },
    { header: "조퇴 시간 누계", fixed: false, width: 14 },
    { header: "미출근", fixed: false, width: 10 },
  ];
  const data = rows.value.map((r) => [
    r.userId ?? "",
    r.userNm ?? "",
    r.deptNm ?? "",
    r.authNm ?? "",
    fmtCount(r.workDayCnt),
    fmtMinutes(r.workMinutes),
    fmtMinutes(r.otMinutes),
    fmtCount(r.lateCnt),
    fmtMinutes(r.lateMinutes),
    fmtCount(r.earlyLeaveCnt),
    fmtMinutes(r.earlyLeaveMinutes),
    fmtCount(r.absentDayCnt),
  ]);
  try {
    await exportStyledExcel({
      fileName: `월별근태판정_${(workYm.value || "").replace("-", "")}.xlsx`,
      sheets: [{ name: "월별근태판정", columns, data }],
    });
  } catch (err) {
    console.error("[Attd_11] excel export failed", err);
    await proxy.$alert("엑셀 다운로드 중 오류가 발생했습니다.");
  }
};

// ── 초기화 (Attd_07 fnInit 패턴 차용) ─────────────────────
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
/* ── 조회 영역 (Attd_07/08 패턴 차용) ──────────────────────── */
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

/* ── 본문 / 테이블 (Attd_08 a08-table 패턴 차용) ───────────── */
.a11-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
/* 소제목 + 테이블을 감싸는 subtitle-pane 래퍼: flex 컬럼 레이아웃에서 스크롤 보존 */
.a11-subtitle-pane {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}
.a11-table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 6px;
  background: var(--color-surface, #fff);
}

.a11-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}
.a11-table thead th {
  background: var(--thead-bg, #f3f4f6);
  /* 헤더/본문 모두 사방 1px 테두리로 통일 (.data-grid 표준 패턴) */
  border: 1px solid var(--color-border, #e5e7eb);
  padding: 0.5rem 0.4rem;
  line-height: 1.2;
  z-index: 1;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text, #374151);
  font-weight: 600;
}
/* 2단 헤더 sticky: 1행은 상단, 2행은 1행 높이만큼 아래에 고정 */
.a11-table thead tr:first-child th {
  position: sticky;
  top: 0;
}
.a11-table thead tr:last-child th {
  position: sticky;
  top: 2.1rem;
}
.a11-table tbody td {
  /* 헤더와 동일하게 컬럼별 세로선이 보이도록 사방 테두리 */
  border: 1px solid var(--color-border, #e5e7eb);
  padding: 0.4rem;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text, #374151);
}
.a11-table tbody tr:hover {
  background: var(--color-bg, #f9fafb);
}
/* 이름/부서는 좌측 정렬 */
.a11-cell-left {
  text-align: left;
}
/* 숫자/시간 컬럼은 우측 정렬 (가독성) */
.a11-cell-num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.a11-empty {
  padding: 2rem;
  color: var(--color-text-muted, #9ca3af);
  text-align: center;
}
</style>
