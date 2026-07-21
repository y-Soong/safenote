<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @excel="fnExcel"
    />

    <!-- 조회 영역 (Attd_11 패턴 차용) -->
    <div class="viewSearch">
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
        <label>의심유형</label>
        <select v-model="suspectType" class="a12-type-select">
          <option value="">전체</option>
          <option value="SHARED_DEVICE">기기 공유 의심</option>
        </select>
      </div>
    </div>

    <!-- 본문: 의심 케이스(기기·날짜 그룹) 목록 -->
    <div class="viewBody a12-body">
      <div class="table-wrapper subtitle-pane a12-subtitle-pane">
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
        <div class="a12-table-wrap">
          <table class="a12-table">
            <thead>
              <tr>
                <th>기기</th>
                <th>관련 계정</th>
                <th>로그인 시각</th>
                <th>부서</th>
                <th>사업장</th>
                <th>의심유형</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="rows.length === 0">
                <td colspan="6" class="a12-empty">의심 케이스가 없습니다.</td>
              </tr>
              <tr v-for="r in rows" :key="r.suspectKey">
                <td class="a12-cell-device" :title="r.deviceUuid">
                  {{ shortDevice(r.deviceUuid) }}
                </td>
                <td class="a12-cell-left">
                  <div
                    v-for="(m, i) in r.members"
                    :key="`${r.suspectKey}-u-${i}`"
                  >
                    {{ m.userNm }} ({{ m.userId }})
                  </div>
                </td>
                <td class="a12-cell-left">
                  <div
                    v-for="(m, i) in r.members"
                    :key="`${r.suspectKey}-t-${i}`"
                  >
                    {{ fmtLoginDtime(m.loginDtime) }}
                  </div>
                </td>
                <td class="a12-cell-left">{{ r.nodeNm }}</td>
                <td class="a12-cell-left">{{ r.siteNm }}</td>
                <td>
                  <span class="a12-badge" :class="badgeClass(r.suspectType)">
                    {{ badgeLabel(r.suspectType) }}
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
import { formatDateTimeDot } from "@/utils/dateFormat";

defineOptions({ name: "Attd_12" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ── 헤더 버튼 (조회 전용 화면 — 생성/저장/삭제 숨김, 엑셀 노출) ──
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
const suspectType = ref("");
const workYm = ref(currentYm());
const siteNoFcs = ref(null);

// ── 조회 결과 (의심 케이스 행 — 기기 중심) ────────────────
//   { suspectKey, deviceUuid, suspectType, nodeNm, siteNm,
//     members:[{ userCd, userId, userNm, loginDtime, clientType }] }
const rows = ref([]);

// master/hr 여부 (그 외 권한은 사업장+소속부서 필수 — Attd_11 동일)
const isMasterOrHr = computed(() => {
  const a = sessionStorage.getItem("gv_authCd");
  return a === "master" || a === "hr";
});

// ── 월/표시 유틸 ──────────────────────────────────────────
function currentYm() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}
// 로그인 일시(YYYYMMDDHHMMSS) → "YYYY.MM.DD HH:mm" (빈값은 "-").
//   날짜·시각 표시는 dateFormat 단일 출처에 위임(점/콜론 통일, com-014).
const fmtLoginDtime = (dtime) => {
  if (!dtime) return "-";
  return formatDateTimeDot(dtime);
};
// 기기 UUID 축약 (앞 6 + … + 뒤 4). title 에 전체값 노출.
const shortDevice = (uuid) => {
  if (!uuid) return "";
  if (uuid.length <= 12) return uuid;
  return `${uuid.slice(0, 6)}…${uuid.slice(-4)}`;
};

// ── 의심유형 배지 ─────────────────────────────────────────
const badgeLabel = (type) => {
  if (type === "SHARED_DEVICE") return "기기 공유 의심";
  return type ?? "";
};
const badgeClass = (type) => {
  if (type === "SHARED_DEVICE") return "a12-badge-strong";
  return "a12-badge-soft";
};

// ── 사업장 / 부서 포커스 처리 (Attd_11 패턴 차용) ──────────
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

// 자동조회 응답 처리 — 0건/1건/다건 분기 (Attd_11 fnCallback 패턴 차용)
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

// ── 조회 ──────────────────────────────────────────────────
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_INPUT_REQUIRED));
    siteNoFcs.value?.focus();
    return;
  }

  // master/hr 이 아니면 사업장+소속부서 필수 (Attd_11 동일)
  if (!isMasterOrHr.value && proxy.$util.isEmpty(nodeCd.value)) {
    await proxy.$alert("소속 부서를 선택해 주세요.");
    return;
  }

  try {
    const response = await axios.get("/webApi/attd12/fraud-attd-suspects", {
      params: {
        workYm: workYm.value,
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        suspectType: suspectType.value,
      },
    });

    if (response.status === 200) {
      rows.value = response.data?.fraudSuspectRowList ?? [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR));
    await proxy.$alert(msg);
  }
};

// ── 엑셀 다운로드 ─────────────────────────────────────────
//   화면과 동일하게 그룹(기기·날짜) 단위 1행 + 관련 계정은 줄바꿈 병합 표기.
const fnExcel = async () => {
  if (rows.value.length === 0) {
    await proxy.$alert("내보낼 데이터가 없습니다.");
    return;
  }
  const columns = [
    { header: "기기UUID", fixed: false, width: 38 },
    { header: "관련 계정", fixed: false, width: 28 },
    { header: "로그인 시각", fixed: false, width: 22 },
    { header: "부서", fixed: false, width: 18 },
    { header: "사업장", fixed: false, width: 18 },
    { header: "의심유형", fixed: false, width: 16 },
  ];
  const data = rows.value.map((r) => [
    r.deviceUuid ?? "",
    (r.members ?? []).map((m) => `${m.userNm}(${m.userId})`).join("\n"),
    (r.members ?? []).map((m) => fmtLoginDtime(m.loginDtime)).join("\n"),
    r.nodeNm ?? "",
    r.siteNm ?? "",
    badgeLabel(r.suspectType),
  ]);
  try {
    await exportStyledExcel({
      fileName: `부정출퇴근의심_${(workYm.value || "").replace("-", "")}.xlsx`,
      sheets: [{ name: "부정출퇴근의심", columns, data }],
    });
  } catch (err) {
    console.error("[Attd_12] excel export failed", err);
    await proxy.$alert("엑셀 다운로드 중 오류가 발생했습니다.");
  }
};

// ── 초기화 (Attd_11 fnInit 패턴 차용) ─────────────────────
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
/* ── 조회 영역 (Attd_11 패턴 차용) ── */
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
.a12-type-select {
  height: 28px;
  padding: 0 0.5rem;
  border: 1px solid var(--color-border-strong);
  border-radius: 4px;
  font-size: 0.875rem;
  color: var(--color-text-strong);
  background: var(--color-surface);
}
/* ── 본문 / 테이블 (Attd_11 패턴 차용) ── */
.a12-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
/* 소제목 + 테이블을 감싸는 subtitle-pane 래퍼: flex 컬럼 레이아웃에서 스크롤 보존 */
.a12-subtitle-pane {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}
.a12-table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-surface);
}
.a12-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}
.a12-table thead th {
  background: var(--thead-bg, #f3f4f6);
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.4rem;
  line-height: 1.2;
  position: sticky;
  top: 0;
  z-index: 1;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text);
  font-weight: 600;
}
.a12-table tbody td {
  border: 1px solid var(--color-border);
  padding: 0.4rem;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text);
  vertical-align: top;
}
.a12-table tbody tr:hover {
  background: var(--color-bg);
}
.a12-cell-left {
  text-align: left;
}
.a12-cell-num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.a12-cell-device {
  font-family: monospace;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
.a12-badge {
  display: inline-block;
  padding: 0.1rem 0.45rem;
  border-radius: 10px;
  font-size: 0.75rem;
  font-weight: 600;
}
.a12-badge-strong {
  background: var(--color-danger);
  color: var(--color-surface);
}
.a12-badge-soft {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}
.a12-empty {
  padding: 2rem;
  color: var(--color-text-muted);
  text-align: center;
}
</style>
