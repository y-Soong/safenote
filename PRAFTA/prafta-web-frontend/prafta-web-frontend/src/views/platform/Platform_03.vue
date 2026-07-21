<!--
  Platform_03.vue — 고객 리스트 (플랫폼 운영자 전용 콘솔, read-only)
  - 메뉴: tb_syst_menu_d MENU_D_ID='Platform_03', MENU_VIEW='platform/Platform_03.vue'
  - 접근: CMPNY_CD='prafta_system_admin' 운영자만(서버 /platformApi 게이트가 강제. 메뉴 숨김은 보조).
  - 동작: 회사명/계약여부/사용여부 검색 → GET /platformApi/customer/customer-lists → 그리드 표시.
  - 강조: 계약종료일 만료=danger, 30일 이내=warning (plan PLT-LOC-05 참조).
  - 골격: planner 작성(template + scoped style), script 로직: developer 작성(PLT-LOC-05).
-->
<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 검색 영역 -->
    <div class="viewSearch">
      <div>
        <label>회사명</label>
        <input v-model.trim="srchCmpnyNm" type="text" @keyup.enter="fnSearch" />
      </div>
      <div>
        <label>계약여부</label>
        <BaseSelect v-model="srchContractYn">
          <option value="">전체</option>
          <option value="Y">Y</option>
          <option value="N">N</option>
        </BaseSelect>
      </div>
      <div>
        <label>사용여부</label>
        <BaseSelect v-model="srchUseYn">
          <option value="">전체</option>
          <option value="Y">Y</option>
          <option value="N">N</option>
        </BaseSelect>
      </div>
    </div>

    <div class="viewBody">
      <!-- 500건 초과 절단 안내 (요청서 §3-1 LIMIT 필수) -->
      <div v-if="truncated" class="p03-truncated-banner">
        검색 결과가 500건을 초과합니다. 상위 500건만 표시합니다. 검색 조건을
        좁혀 주세요.
      </div>

      <div class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">고객사 목록</span>
          <span class="p03-count" v-if="customerList.length > 0">{{
            countLabel
          }}</span>
        </div>

        <div
          class="table-box"
          style="--box-h: 68vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table
            class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <ThSortable
                  label="회사코드"
                  col-key="cmpnyCd"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.cmpnyCd"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="회사명"
                  col-key="cmpnyNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.cmpnyNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="사업자등록번호"
                  col-key="bsnsLcnNo"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.bsnsLcnNo"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="주소"
                  col-key="addr"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.addr"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="계약여부"
                  col-key="contractYn"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.contractYn"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="계약종료일자"
                  col-key="contractEndDate"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.contractEndDate"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="사용여부"
                  col-key="useYn"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.useYn"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="당월 AI 사용량"
                  col-key="usedTokens"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.usedTokens"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="AI 한도"
                  col-key="tokenLimit"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.tokenLimit"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="사용률"
                  col-key="usageRate"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.usageRate"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="event_cell" style="text-align: center; width: 70px">
                  한도변경
                </th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!customerList || customerList.length === 0">
                <tr>
                  <td colspan="12" class="edu-grid-empty">
                    등록된 고객사가 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(customer, idx) in sortedData"
                  :key="customer.cmpnyCd"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td class="p03-mono">{{ customer.cmpnyCd }}</td>
                  <td>{{ customer.cmpnyNm }}</td>
                  <td>{{ customer.bsnsLcnNo }}</td>
                  <td>{{ fnJoinAddr(customer) }}</td>
                  <td style="text-align: center">{{ customer.contractYn }}</td>
                  <td
                    style="text-align: center"
                    :class="fnContractClass(customer.contractEndDate)"
                  >
                    {{
                      customer.contractEndDate
                        ? formatYmdDot(customer.contractEndDate)
                        : "무기한"
                    }}
                  </td>
                  <td style="text-align: center">{{ customer.useYn }}</td>
                  <td style="text-align: right">
                    {{ fnFormatMan(customer.usedTokens) }}
                  </td>
                  <td style="text-align: right">
                    {{ fnLimitLabel(customer.tokenLimit)
                    }}<span
                      v-if="customer.quotaCustomYn === 'N'"
                      class="p03-default-tag"
                      >(기본)</span
                    >
                  </td>
                  <td
                    style="text-align: center"
                    :class="fnUsageRateClass(customer)"
                  >
                    {{ fnUsageRateLabel(customer) }}
                  </td>
                  <td style="text-align: center">
                    <button
                      class="btn btn-primary p03-quota-btn"
                      @click="fnOpenQuotaPop(customer)"
                    >
                      변경
                    </button>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <!-- AI 토큰 한도 변경 팝업 -->
      <AiTokenQuotaPop
        v-if="quotaPopVisible"
        :cmpny-cd="quotaTarget.cmpnyCd"
        :cmpny-nm="quotaTarget.cmpnyNm"
        :token-limit="quotaTarget.tokenLimit"
        :used-tokens="quotaTarget.usedTokens"
        :on-saved="fnSearch"
        @close="quotaPopVisible = false"
      />
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted,
  getCurrentInstance,
  defineOptions,
  defineProps,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import BaseSelect from "@/components/common/BaseSelect.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";
import { formatYmdDot } from "@/utils/dateFormat";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import AiTokenQuotaPop from "@/views/platform/popup/AiTokenQuotaPop.vue";

// keep-alive 매칭용 컴포넌트 이름 = 라우트 이름(MENU_D_ID)
defineOptions({ name: "Platform_03" });

// MainLayout 이 주입하는 공통 props(탭 제목/버튼 권한)
const props = defineProps({
  title: { type: String, default: "고객 리스트" },
  buttons: { type: Object, default: () => ({}) },
});

const { proxy } = getCurrentInstance();

/* 조회조건 */
const srchCmpnyNm = ref("");
const srchContractYn = ref("");
const srchUseYn = ref("");

/* 목록 상태 */
const customerList = ref([]);
const truncated = ref(false); // 서버 LIMIT 500 초과 여부
const totalCnt = ref(0); // 서버 전체 건수(절단 전 — qa D-3)

/* AI 토큰 한도 변경 팝업 상태 */
const quotaPopVisible = ref(false);
const quotaTarget = ref({});

/* 총건수 라벨: 절단 시 서버 전체 건수와 표시 건수를 함께 표기(qa D-3) */
const countLabel = computed(() => {
  if (truncated.value) {
    return `총 ${totalCnt.value.toLocaleString()}건 중 ${customerList.value.length.toLocaleString()}건 표시`;
  }
  return `총 ${customerList.value.length.toLocaleString()}건`;
});

/* 정렬/컬럼 리사이즈 (Platform_02 전례) */
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(customerList);
const { colWidths, onResize } = useColumnResize({
  cmpnyCd: 200,
  cmpnyNm: 160,
  bsnsLcnNo: 120,
  addr: 260,
  contractYn: 70,
  contractEndDate: 110,
  useYn: 70,
  usedTokens: 110,
  tokenLimit: 100,
  usageRate: 80,
});

/* read-only 화면 — 조회 외 버튼 숨김 (Platform_02 fnButtonControll 전례) */
const localButtons = ref({ ...props.buttons });
function fnButtonControll() {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
}

onMounted(async () => {
  fnButtonControll();
  await fnSearch();
});

/* 주소 결합: ADDR_1 + ADDR_2 (요청서 §3-1) */
function fnJoinAddr(customer) {
  return [customer.addr1, customer.addr2].filter(Boolean).join(" ");
}

/* Date → YYYYMMDD (로컬 시간 기준 — toISOString(UTC) 금지: KST 00~09시 전날 판정 사고 전례) */
function fnToYmd(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}${m}${d}`;
}

/*
 * 계약종료일 강조 클래스 판정 (plan PLT-LOC-05-2).
 *   반환: 'is-expired'(오늘 이전=만료) / 'is-expiring'(오늘~30일 이내) / ''(그 외·무기한).
 *   YYYYMMDD 문자열 비교(동일 자릿수 — 사전순=시간순).
 */
function fnContractClass(contractEndDate) {
  if (!contractEndDate) return "";
  const end = String(contractEndDate).replace(/\D/g, "");
  if (end.length !== 8) return "";

  const now = new Date();
  const today = fnToYmd(now);
  if (end < today) return "is-expired";

  // 오늘 + 30일 (월말 이월은 Date 생성자가 처리)
  const limit = fnToYmd(
    new Date(now.getFullYear(), now.getMonth(), now.getDate() + 30)
  );
  if (end <= limit) return "is-expiring";
  return "";
}

/* 원시 토큰 수 → 만 단위 표기(소수 1자리 반올림, ".0" 은 생략 — 예: 234567 → "23.5만", 800000 → "80만") */
function fnFormatMan(tokens) {
  const man = (tokens ?? 0) / 10000;
  return (Math.round(man * 10) / 10).toFixed(1).replace(/\.0$/, "") + "만";
}

/* AI 한도 라벨: -1 → "무제한" / 0 → "차단" / 양수 → 만 단위 표기 */
function fnLimitLabel(limit) {
  if (limit === -1) return "무제한";
  if (limit === 0) return "차단";
  return fnFormatMan(limit);
}

/* 사용률 라벨: 한도 양수일 때만 %(소수 1자리), 무제한/차단은 "-" */
function fnUsageRateLabel(customer) {
  if (!(customer.tokenLimit > 0)) return "-";
  const rate = (customer.usedTokens / customer.tokenLimit) * 100;
  return (Math.round(rate * 10) / 10).toFixed(1).replace(/\.0$/, "") + "%";
}

/* 사용률 강조: >=100 danger(is-expired) / >=90 warning(is-expiring) — 기존 클래스 재사용 */
function fnUsageRateClass(customer) {
  if (!(customer.tokenLimit > 0)) return "";
  const rate = (customer.usedTokens / customer.tokenLimit) * 100;
  if (rate >= 100) return "is-expired";
  if (rate >= 90) return "is-expiring";
  return "";
}

/* 행 [변경] → AI 토큰 한도 변경 팝업 오픈(저장 콜백 = fnSearch 재조회) */
function fnOpenQuotaPop(customer) {
  quotaTarget.value = customer;
  quotaPopVisible.value = true;
}

/*
 * 고객사 목록 조회 — GET /platformApi/customer/customer-lists (PLT-LOC-02).
 *   응답: { customerList, totalCnt, truncated } — truncated=true 면 500건 초과 안내 배너.
 */
async function fnSearch() {
  customerList.value = [];
  truncated.value = false;
  totalCnt.value = 0;

  try {
    const response = await axios.get("/platformApi/customer/customer-lists", {
      params: {
        cmpnyNm: srchCmpnyNm.value,
        contractYn: srchContractYn.value,
        useYn: srchUseYn.value,
      },
    });

    if (response.status === 200) {
      // addr: 주소 정렬용 파생 필드(템플릿 col-key="addr" 와 일치 — qa D-2)
      // usedTokens/tokenLimit: 서버 필드 부재 시(구버전 응답) 0/기본값 폴백
      // usageRate: 정렬용 파생 필드(숫자, 무제한/차단은 -1 — col-key="usageRate" 정렬 정합)
      customerList.value = (response.data?.customerList || []).map((row) => {
        const usedTokens = row.usedTokens ?? 0;
        const tokenLimit = row.tokenLimit ?? 800000;
        return {
          ...row,
          addr: fnJoinAddr(row),
          usedTokens,
          tokenLimit,
          quotaCustomYn: row.quotaCustomYn ?? "N",
          usageRate: tokenLimit > 0 ? (usedTokens / tokenLimit) * 100 : -1,
        };
      });
      truncated.value = response.data?.truncated === true;
      totalCnt.value = response.data?.totalCnt ?? customerList.value.length;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
}
</script>

<style scoped>
/* 계약여부/사용여부 셀렉트가 BaseSelect 인라인 width:100% 때문에 한 줄을
   통째로 차지해 2행으로 떨어지는 것을 방지 — 조회조건 3개를 1행 정렬
   (VictimSearchPop :deep(select) !important 전례) */
.viewSearch :deep(select) {
  width: 90px !important;
}

/* 500건 초과 절단 안내 배너 — 차단/제한 상황은 배너로 명시(공통 정책서 §13.3) */
.p03-truncated-banner {
  margin: 0 0 0.5rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-warning-text);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

/* 소제목 우측 총건수 */
.p03-count {
  margin-left: 0.5rem;
  font-size: 0.78rem;
  font-weight: 400;
  color: var(--color-text-muted);
}

/* 회사코드 — 등폭 표기(Platform_01 result-panel mono 전례) */
.p03-mono {
  font-family: "D2Coding", Consolas, monospace;
  word-break: break-all;
}

/* 계약종료일 강조: 만료=danger / 30일 이내=warning (AI 사용률 >=100/>=90 강조에도 재사용) */
.is-expired {
  color: var(--color-danger);
  font-weight: 700;
}
.is-expiring {
  color: var(--color-warning-text);
  font-weight: 700;
}

/* 기본 한도 표기 태그 */
.p03-default-tag {
  margin-left: 0.25rem;
  font-size: 0.72rem;
  color: var(--color-text-muted);
}
/* 행 내 한도변경 버튼 — 그리드 행 높이에 맞춘 소형 */
.p03-quota-btn {
  padding: 0.15rem 0.6rem;
  font-size: var(--btn-font-sm, 11px);
}
</style>
