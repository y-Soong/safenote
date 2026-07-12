<!--
  LeavePromotion_01.vue — 연차 사용촉진(2차 회사직권) 관리 화면 (관리자 웹, 신규)
  - 작업 ID: prafta-com-008-A-8 (분해: .claude/requests/common/refs/prafta-com-008/prafta-com-008-A-decomposition.md)
  - UI 명세: UI-web-008-A-1
  - 정책 출처: 작업지시서 §3(2차 회사직권 — 조회조건/미사용수/2차촉진/팝업/엑셀/자동배치), §3-6(노드 권한)
  - 참조 패턴: views/attd/Attd_12.vue (ViewHeader + viewSearch[사업장/부서+하위] + viewBody 테이블 + master/hr 스코프)
  - planner 라운드: template + scoped style 완성, script 는 import/ref 선언 + 검색/포커스 골격 + TODO.
  - developer 라운드:
      · GET /webApi/leavepromo01/targets (조회조건) → rows.
      · 행 [2차촉진 지정] → DesignatePop (POST /webApi/leavepromo01/designate + PUSH).
      · [자동배치] → AutoBatchPop (preview/commit).
      · [엑셀 업로드/다운로드] → template/upload (prafta-052 실패행 2시트).
-->
<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @excel="fnExcelDownload"
    />

    <!-- 조회 영역 (Attd_12 패턴 차용) -->
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
        <input type="text" v-model="userNm" placeholder="사용자명" />
      </div>
      <div>
        <label>1년차 구분</label>
        <select v-model="tenureType" class="lp-type-select">
          <option value="ALL">전체</option>
          <option value="UNDER1">1년차 미만</option>
          <option value="OVER1">1년차 이상</option>
        </select>
      </div>
      <!-- 자동배치 버튼 (헤더 excel 과 별도 액션) -->
      <div class="lp-actions">
        <button class="lp-batch-btn" @click="fnAutoBatchPopOpen">
          자동배치
        </button>
        <button class="lp-excel-up-btn" @click="fnUploadExcelClick">
          엑셀 업로드
        </button>
        <input
          ref="excelFileRef"
          type="file"
          accept=".xlsx"
          class="lp-excel-input"
          @change="fnExcelFileChange"
        />
      </div>
    </div>

    <!-- 본문: 2차 대상자 테이블 -->
    <div class="viewBody lp-body subtitle-pane">
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
      <div class="lp-table-wrap">
        <table class="lp-table">
          <thead>
            <tr>
              <th>이름</th>
              <th>부서</th>
              <th>사업장</th>
              <th>미사용 연차</th>
              <th>2차 대상</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="rows.length === 0">
              <td colspan="6" class="lp-empty">대상자가 없습니다.</td>
            </tr>
            <tr v-for="r in rows" :key="r.userCd">
              <td class="lp-cell-left">{{ r.userNm }}</td>
              <td class="lp-cell-left">{{ r.nodeNm }}</td>
              <td class="lp-cell-left">{{ r.siteNm }}</td>
              <td class="lp-cell-num">{{ fmtDays(r.unusedDays) }}</td>
              <!-- 2차 대상 = 실시간 grant 잔여(unusedDays). stage2TargetDays(스냅샷)는 cap/표시 근거로 쓰지 않음(H1). -->
              <td class="lp-cell-num">{{ fmtDays(r.unusedDays) }}</td>
              <td>
                <button
                  class="lp-designate-btn"
                  :disabled="!r.unusedDays || r.unusedDays <= 0"
                  @click="fnDesignatePopOpen(r)"
                >
                  2차촉진 지정
                </button>
              </td>
            </tr>
          </tbody>
        </table>
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
import { useModal } from "@/utils/useModal";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import LeavePromotionDesignatePop from "./popup/LeavePromotionDesignatePop.vue";
import LeavePromotionAutoBatchPop from "./popup/LeavePromotionAutoBatchPop.vue";
import LeavePromotionExcelPop from "./popup/LeavePromotionExcelPop.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";

defineOptions({ name: "LeavePromotion_01" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ── 헤더 버튼 (조회 + 엑셀 다운로드 노출, 생성/저장/삭제 숨김) ──
const localButtons = ref({ ...props.buttons });
// Attd_12 fnButtonControll 패턴 — 조회 전용 화면이라 생성/저장/삭제 숨김, 엑셀(양식 다운로드) 노출.
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
const userNm = ref("");
const tenureType = ref("ALL");
const siteNoFcs = ref(null);
const excelFileRef = ref(null);

// ── 조회 결과 (2차 대상자 행) ─────────────────────────────
//   PromotionTargetRowResult: { userCd, userNm, nodeNm, siteNm, hireDate,
//     unusedDays, stage2TargetDays, baseAvailToDate }  — 사번 없음(확정-4).
const rows = ref([]);

// master/hr 여부 (그 외 권한은 사업장+소속부서 필수 — Attd_12 동일)
const isMasterOrHr = computed(() => {
  const a = sessionStorage.getItem("gv_authCd");
  return a === "master" || a === "hr";
});

// ── 표시 유틸 ─────────────────────────────────────────────
const fmtDays = (d) => (d == null ? "-" : `${d}일`);

// ── 포커스 처리 (Attd_12 focusKill 패턴) ───────────────────
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

// 사업장 자동조회 (코드/명 입력 후 blur) — Attd_12 동일
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

// 부서 자동조회 (코드/명 입력 후 blur) — Attd_12 동일
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

// 자동조회 응답 처리 — 0건/1건/다건 분기 (Attd_12 fnCallback 패턴)
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

// ── 사업장/부서 검색 팝업 (Attd_12 패턴) ────────────────────
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

// ── 현재 조회조건 스냅샷 (자동배치/엑셀 양식에 동일 전달) ────
const buildFilter = () => ({
  siteCd: siteCd.value,
  nodeCd: nodeCd.value,
  incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
  userNm: userNm.value,
  tenureType: tenureType.value,
});

// ── 조회 ───────────────────────────────────────────────────
const fnSearch = async () => {
  // 사업장 필수. master/hr 외에는 소속부서까지 필수(Attd_12 동일 스코프).
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_INPUT_REQUIRED));
    siteNoFcs.value?.focus();
    return;
  }
  if (!isMasterOrHr.value && proxy.$util.isEmpty(nodeCd.value)) {
    await proxy.$alert("소속 부서를 선택해 주세요.");
    return;
  }
  try {
    // cmpnyCd/권한 스코프·siteCd IDOR 는 서버 JWT 강제. 응답은 PromotionTargetListResponse.targetList.
    const { data } = await axios.get("/webApi/leavepromo01/targets", {
      params: buildFilter(),
    });
    rows.value = data?.targetList || [];
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// ── 개별 2차촉진 지정 팝업 ─────────────────────────────────
const fnDesignatePopOpen = (row) => {
  openPop(LeavePromotionDesignatePop, {
    userCd: row.userCd,
    userNm: row.userNm,
    // cap = 실시간 grant 잔여(unusedDays). 지정 진행 시 USED_DAYS↑로 자연 감소(자기정합) — H1.
    targetDays: Number(row.unusedDays) || 0,
    // 직권 지정 완료(done) → 재조회로 미사용 연차/2차 대상 갱신.
    onDone: () => {
      fnSearch();
    },
  });
};

// ── 자동배치 팝업 ──────────────────────────────────────────
const fnAutoBatchPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_INPUT_REQUIRED));
    return;
  }
  openPop(LeavePromotionAutoBatchPop, {
    filter: buildFilter(),
    // commit 성공(done) → 재조회.
    onDone: () => {
      fnSearch();
    },
  });
};

// ── 엑셀 양식 다운로드 (조회조건 기준 대상자 + 날짜 칸) ─────
const fnExcelDownload = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_INPUT_REQUIRED));
    return;
  }
  try {
    const response = await axios.get("/webApi/leavepromo01/excel/template", {
      params: buildFilter(),
      responseType: "blob",
    });
    if (response.status === 200) {
      const blob = new Blob([response.data], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "연차일괄지정양식.xlsx";
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "양식 다운로드 중 오류가 발생했습니다.")
    );
  }
};

// ── 엑셀 업로드 (행=사용자-연차날짜) → 결과 팝업(실패행 2시트) ─
const fnUploadExcelClick = () => {
  if (excelFileRef.value) {
    excelFileRef.value.value = "";
    excelFileRef.value.click();
  }
};
const fnExcelFileChange = async (e) => {
  const file = e?.target?.files?.[0];
  if (!file) return;
  try {
    const formData = new FormData();
    formData.append("file", file);
    const { data } = await axios.post(
      "/webApi/leavepromo01/excel/upload",
      formData,
      { headers: { "Content-Type": "multipart/form-data" } }
    );
    // PromotionExcelUploadResponse: { totalCount, successCount, failCount, failItems, failsToken }
    openPop(LeavePromotionExcelPop, { result: data || null });
    // 일부라도 등록됐을 수 있으므로 재조회.
    fnSearch();
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "엑셀 업로드 중 오류가 발생했습니다.")
    );
  } finally {
    // 같은 파일 재선택 허용.
    if (e?.target) e.target.value = "";
  }
};

// ── 초기화 (Attd_12 fnInit 패턴) ──────────────────────────
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
/* ── 조회 영역 (Attd_12 패턴 차용) ── */
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
.lp-type-select {
  height: 28px;
  padding: 0 0.5rem;
  border: 1px solid var(--color-border-strong);
  border-radius: 4px;
  font-size: 0.875rem;
  color: var(--color-text-strong);
  background: var(--color-surface);
}

/* ── 조회 영역 액션 버튼 (자동배치 / 엑셀 업로드) ── */
.lp-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.lp-excel-input {
  display: none;
}
.lp-batch-btn,
.lp-excel-up-btn {
  height: 28px;
  padding: 0 0.6rem;
  border-radius: 4px;
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-text-strong);
  font-size: 0.875rem;
  cursor: pointer;
}
.lp-batch-btn:hover,
.lp-excel-up-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.lp-batch-btn {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* ── 본문 / 테이블 (Attd_12 패턴 차용) ── */
.lp-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
.lp-table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-surface);
}
.lp-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}
.lp-table thead th {
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
.lp-table tbody td {
  border: 1px solid var(--color-border);
  padding: 0.4rem;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text);
  vertical-align: middle;
}
.lp-table tbody tr:hover {
  background: var(--color-bg);
}
.lp-cell-left {
  text-align: left;
}
.lp-cell-num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.lp-empty {
  padding: 2rem;
  color: var(--color-text-muted);
  text-align: center;
}

/* ── 행 액션 버튼 (2차촉진 지정) ── */
.lp-designate-btn {
  height: 26px;
  padding: 0 0.6rem;
  border-radius: 4px;
  border: 0;
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 0.8rem;
  cursor: pointer;
}
.lp-designate-btn:disabled {
  background: var(--color-border);
  color: var(--color-text-muted);
  cursor: default;
}
</style>
