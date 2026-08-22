<template>
  <div class="a09s">
    <!-- 조회 영역 (Attd_09 viewSearch 패턴) -->
    <div class="viewSearch">
      <!-- P2-D5 재작업: 필터에 실반영되지 않던 장식 입력칸(사업장명/부서명) 제거 — 동작하는 코드 입력만 유지 -->
      <div>
        <label>사업장</label>
        <input type="text" v-model="siteNo" placeholder="사업장코드 직접 입력" />
      </div>
      <div>
        <label>소속부서</label>
        <input type="text" v-model="nodeCd" placeholder="부서코드 직접 입력" />
      </div>
      <div>
        <label>기준일</label>
        <input type="date" v-model="baseDate" />
      </div>
      <div>
        <button class="btn btn-primary" @click="fnSearch">조회</button>
      </div>
    </div>

    <!-- 안내 문구 -->
    <div class="a09s-notice">
      <p>ⓘ 기준일에 <strong>퇴사(예정)일</strong>을 입력하면 퇴직정산 참고 조회가 됩니다.</p>
      <p>
        ⓘ 차액은 조회 시점에 따라 <strong>요동칠 수 있습니다</strong>.
        남은 부족분이 <strong>음수</strong>인 구간은 회계연도 부여가 입사일 기준을 앞서는
        정상 상태입니다(보전 불필요).
      </p>
    </div>

    <!-- P2-D4 재작업: 총 인원 표시 — 100명 초과 회사의 조용한 누락 방지(전 페이지 누적 로드와 함께) -->
    <div class="a09s-count">조회 인원: {{ totalCount }}명</div>

    <!-- 결과 테이블 -->
    <div class="a09s-table-wrap">
      <table class="a09s-table">
        <thead>
          <tr>
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
          <tr v-for="row in rows" :key="row.userCd">
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
            <td colspan="9" class="a09s-table-empty">조회 결과가 없습니다</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import { ref, getCurrentInstance, onMounted } from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import CoverGrantPop from "./popup/CoverGrantPop.vue";

// ================ Options ================
defineOptions({ name: "Attd_09_Shortfall" });

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
// 조회 조건. 사업장/부서는 Attd_09 본문의 SiteSearchPop 팝업 대신 코드 직접 입력으로 단순화했다
//   (골격에 검색 버튼이 없어 신규 UI 요소를 임의 추가하지 않음 — developer 판단, dev-notes 기록).
//   P2-D5 재작업: 필터에 실반영되지 않던 사업장명/부서명 장식 입력칸(siteNm/nodeNm)은 제거했다.
const siteNo = ref("");
const nodeCd = ref("");
const baseDate = ref(""); // YYYY-MM-DD (input[type=date])

// 결과
const rows = ref([]);
const totalCount = ref(0);
const isLoading = ref(false);

// ================ Life Cycle Functions ================
onMounted(() => {
  baseDate.value = fnTodayYyyyMmDd();
  fnSearch();
});

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
      siteCd: siteNo.value || "",
      nodeCd: nodeCd.value || "",
      incSubNodeYn: "N",
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
  gap: 0.75rem;
  min-height: 0;
  flex: 1;
}

.a09s-notice {
  margin: 0.5rem 0;
  padding: 0.625rem 0.875rem;
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-radius: var(--input-radius);
  font-size: 0.75rem;
}

.a09s-notice p {
  margin: 0.125rem 0;
}

.a09s-count {
  font-size: 0.8125rem;
  color: var(--color-text-strong);
  font-weight: 600;
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
