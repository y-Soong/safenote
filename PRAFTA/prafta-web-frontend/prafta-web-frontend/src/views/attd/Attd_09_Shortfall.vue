<template>
  <div class="a09s">
    <!-- 조회 영역 (Attd_09 연차 현황 탭과 동일 패턴 — 코드/버튼/명 + 하위부서 조회) -->
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
          <input type="checkbox" v-model="incSubNodeYn" :disabled="!nodeCd" />
          하위부서 조회
        </label>
      </div>
      <div>
        <label>기준일</label>
        <input class="a09s-date-input" type="date" v-model="baseDate" />
      </div>
      <div>
        <button class="btn btn-primary" @click="fnSearch">조회</button>
      </div>
    </div>

    <!-- 조회조건 아래 본문(여백은 공용 .viewBody 컨벤션 — Attd_09 연차 현황 탭과 동일) -->
    <div class="viewBody a09s-body">
      <!-- 안내 문구 -->
      <div class="a09s-notice">
        <p>ⓘ 기준일에 <strong>퇴사(예정)일</strong>을 입력하면 퇴직정산 참고 조회가 됩니다.</p>
        <p>
          ⓘ 차액은 조회 시점에 따라 <strong>요동칠 수 있습니다</strong>.
          남은 부족분이 <strong>음수</strong>인 구간은 회계연도 부여가 입사일 기준을 앞서는
          정상 상태입니다(보전 불필요).
        </p>
      </div>

      <!-- 결과 테이블 -->
      <div class="a09s-table-wrap">
        <table class="a09s-table">
          <thead>
            <tr>
              <th class="a09s-idx-col">No.</th>
              <th>사번</th>
              <th>성명</th>
              <th>입사일</th>
              <th class="is-right">입사일기준 누적(정답)</th>
              <th class="is-right">실제 부여 누적</th>
              <th class="is-right">차액</th>
              <th class="is-right">기보전 합</th>
              <th class="is-right">남은 부족분</th>
              <th>보전</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in rows" :key="row.userCd">
              <td class="a09s-idx-col">{{ idx + 1 }}</td>
              <td>{{ row.userCd }}</td>
              <td>{{ row.userNm }}</td>
              <td>{{ fnFormatDate(row.hireDate) }}</td>
              <td class="is-right">{{ row.hireBasisAccrual }}</td>
              <td class="is-right">{{ row.actualAccrual }}</td>
              <td class="is-right" :class="{ 'a09s-negative': row.diff < 0 }">{{ row.diff }}</td>
              <td class="is-right">{{ row.coveredTotal }}</td>
              <td class="is-right" :class="{ 'a09s-negative': row.remainingShortfall < 0 }">
                {{ row.remainingShortfall }}
              </td>
              <td>
                <!-- 소정-05 OFF 여도 활성 유지 — 클릭 시 사유 안내 (disabled 금지) -->
                <button class="btn btn-primary btn-sm" @click="fnCoverGrantOpen(row)">
                  보전 부여
                </button>
              </td>
            </tr>
            <tr v-if="!isLoading && rows.length === 0">
              <td colspan="10" class="a09s-table-empty">조회 결과가 없습니다</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import { ref, getCurrentInstance, onMounted, watch } from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { getMessage, MSG } from "@/messages";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import CoverGrantPop from "./popup/CoverGrantPop.vue";

// ================ Options ================
defineOptions({ name: "Attd_09_Shortfall" });

// ================ Props ================
// 부모(Attd_09)가 v-show 로 이 컴포넌트를 항상 마운트 상태로 두므로(탭 전환 시 언마운트 안 됨),
// onMounted 1회 조회만으로는 다른 탭(정책 기준 부여 등)에서 생긴 변경분이 반영되지 않는다.
// active 로 "지금 이 탭이 보이는 시점"을 받아 그때마다 재조회한다.
const props = defineProps({
  active: { type: Boolean, default: false },
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
// 사업장/소속부서 — Attd_09 연차 현황 탭과 동일 패턴(코드/버튼/명 + 하위부서 조회).
const siteCd = ref(""); // API 전달용 내부 코드
const siteNo = ref(""); // 화면 표시용 사업장코드 입력칸
const siteNm = ref("");
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true); // 사업장 선택 전에는 부서 입력 비활성(연차 현황 탭과 동일)
const incSubNodeYn = ref(false);
const baseDate = ref(""); // YYYY-MM-DD (input[type=date])

// 결과
const rows = ref([]);
const totalCount = ref(0);
const isLoading = ref(false);

// ================ Life Cycle Functions ================
onMounted(() => {
  baseDate.value = fnTodayYyyyMmDd();
});

// active 가 true 로 바뀔 때(탭이 보일 때)마다 재조회. immediate:true 로 최초 활성화 시점도 커버.
watch(
  () => props.active,
  (val) => {
    if (val) fnSearch();
  },
  { immediate: true }
);

// ================ 사업장/소속부서 조회 (Attd_09 연차 현황 탭 패턴 차용) ================
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
    if (response.status === 200) fnSiteCallback(response);
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
      fnSiteCallback({
        ...response,
        config: { url: "/dummy/site-node-lists" },
      });
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

const fnSiteCallback = (res) => {
  if (!proxy.$util.isNotEmpty(res)) return;
  const apiId = res.config.url.split("/").pop();
  if (apiId === "site-lists") {
    const siteList = res.data?.siteInfoResultList ?? [];
    if (siteList.length === 1) {
      siteCd.value = siteList[0].siteCd;
      siteNo.value = siteList[0].siteNo;
      siteNm.value = siteList[0].siteNm;
      nodeDisabled.value = false;
      nodeCd.value = "";
      nodeNm.value = "";
    } else if (siteList.length > 1) {
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
    const nodeList = res.data?.siteNodeInfoList || [];
    if (nodeList.length === 0) {
      nodeCd.value = "";
      nodeNm.value = "";
    } else if (nodeList.length === 1) {
      nodeCd.value = nodeList[0].nodeCd ?? "";
      nodeNm.value = nodeList[0].nodeNm ?? "";
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

// ================ API Functions ================
// 조회 — GET /webApi/attd09/leave-dashboard/shortfall/list
//   P2-D4 재작업: 종전 size=100 단발 호출은 100명 초과 회사에서 101번째 이후 직원이 조용히
//   누락됐다. Attd_09 본문 fnLoad 의 누적 로드 패턴(com-013-08-4)을 그대로 미러 —
//   totalCount 를 모두 채울 때까지 페이지를 순회하며 누적한다(백엔드 계약/상한 불변).
const PAGE_SIZE = 100; // 백엔드 MAX_PAGE_SIZE 와 동일(한 번에 받을 최대 건수)
const MAX_FETCH_PAGES = 200; // 무한 루프 방어(이론상 최대 2만 명)

const fnSearch = async () => {
  if (!baseDate.value) {
    proxy.$alert("기준일을 입력해 주세요.");
    return;
  }
  isLoading.value = true;
  try {
    const baseParams = {
      siteCd: siteCd.value || "",
      nodeCd: nodeCd.value || "",
      incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
      userNm: "",
      baseYmd: fnToYyyymmdd(baseDate.value),
      size: PAGE_SIZE,
    };

    const accumulated = [];
    let total = 0;
    for (let page = 1; page <= MAX_FETCH_PAGES; page++) {
      const response = await axios.get(
        "/webApi/attd09/leave-dashboard/shortfall/list",
        { params: { ...baseParams, page } }
      );
      const data = response.data || {};

      // 총건수는 회사 공통 값이라 첫 페이지 응답만 채택한다(Attd_09 본문 관례 미러).
      if (page === 1) {
        total = data.totalCount ?? 0;
      }
      const pageRows = Array.isArray(data.rows) ? data.rows : [];
      accumulated.push(...pageRows);

      // 더 받을 게 없으면 종료(누적 건수가 총건수 도달 or 빈 페이지 — HIRE_DATE 회사 빈 응답 포함).
      if (accumulated.length >= total || pageRows.length === 0) {
        break;
      }
    }
    rows.value = accumulated;
    totalCount.value = total;
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};

// CoverGrantPop 오픈(row 전달) — 성공 콜백 시 fnSearch 재호출
//   P2-D5 재작업: 부서명 장식 입력칸 제거로 deptNm 전달원이 없어졌다 — 팝업은 "-" 폴백 표시.
const fnCoverGrantOpen = (row) => {
  openPop(CoverGrantPop, {
    targetUser: { userCd: row.userCd, userNm: row.userNm, deptNm: "" },
    remainingShortfall: row.remainingShortfall,
    baseYmd: fnToYyyymmdd(baseDate.value),
    onGranted: fnSearch,
  });
};

// ================ 내부 유틸 ================
// YYYY-MM-DD → YYYYMMDD
const fnToYyyymmdd = (ymd) => String(ymd || "").replace(/-/g, "");

// 오늘 YYYY-MM-DD (input[type=date] 값)
const fnTodayYyyyMmDd = () => {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
};

// YYYYMMDD → YYYY-MM-DD (표시용)
const fnFormatDate = (ymd) => {
  const s = String(ymd || "");
  return s.length === 8 ? `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}` : s;
};
</script>

<style scoped>
.a09s {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
}

/* 기준일: 전역 .viewSearch input 기본폭(120px)이 date picker 아이콘까지 담기엔 좁아 잘려 보이던 문제 보정 */
.a09s-date-input {
  width: 160px !important;
  min-width: 160px;
}

/* 하위부서 조회 체크박스 — Attd_09 연차 현황 탭과 동일 스타일(scoped 라 상속 안 되어 별도 선언 필요) */
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

.a09s-body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  flex: 1;
  min-height: 0;
}

.a09s-notice {
  margin: 0;
  padding: 0.55rem 0.8rem;
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border, rgba(180, 83, 9, 0.25));
  color: var(--color-warning-text);
  border-radius: var(--input-radius);
  font-size: 0.75rem;
  line-height: 1.55;
}

.a09s-notice p {
  margin: 0;
}

.a09s-notice p + p {
  margin-top: 0.3rem;
  padding-top: 0.3rem;
  border-top: 1px dashed var(--color-warning-border, rgba(180, 83, 9, 0.25));
}

.a09s-table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
}

.a09s-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}

.a09s-table thead th {
  text-align: center;
  padding: 0.5rem 0.4rem;
  font-weight: 600;
  color: var(--color-text-strong);
  white-space: nowrap;
  background: var(--thead-bg, #f3f4f6);
  border: 1px solid var(--color-border);
  position: sticky;
  top: 0;
}

.a09s-table td {
  padding: 0.4rem;
  border: 1px solid var(--color-border);
  text-align: center;
}

.a09s-idx-col {
  width: 3rem;
  color: var(--color-text-muted);
}

.a09s-table th.is-right,
.a09s-table td.is-right {
  text-align: right;
}

.a09s-table td.a09s-negative {
  color: var(--color-danger);
}

.a09s-table-empty {
  text-align: center;
  padding: 1.5rem;
  color: var(--color-text-muted);
}
</style>
