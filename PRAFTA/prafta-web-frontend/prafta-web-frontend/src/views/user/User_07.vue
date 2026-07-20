<template>
  <!--
    User_07.vue — 일용직 근로계약서 관리 (웹, 화면 A)
    - 분해: .claude/requests/common/작업지시서_일용직-계약서-서명-승인제.plan.md §4 UI-DC-05 / §2 T5
    - 요청서 근거: §4-2 화면 A(업로드/교체=버전 증가/미리보기/사용중지), R1(사업장 단위), D8(교체=재서명 트리거), §4-3(권장 조항)
    - 메뉴: 일일계정 관리 하위(T1 메뉴 SQL 적용 후 라우트 등록 — User_05/06 미러)
    - 참조 패턴: User_06(viewComm/ViewHeader/viewSearch/data-grid), DailyContractRegPop(등록 팝업)
    - planner 라운드 스코프: template + style. script 는 선언 + TODO(developer).
  -->
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @create="fnOpenRegPop"
    />

    <!-- 검색바: 사업장 선택(R1 — 계약서는 사업장 단위) -->
    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <select v-model="siteCd" class="site-select" @change="fnSearch">
          <option value="">선택</option>
          <option v-for="site in siteList" :key="site.siteCd" :value="site.siteCd">
            {{ site.siteNm }}
          </option>
        </select>
      </div>
    </div>

    <div class="viewBody">
      <!-- 권장 조항 안내(§4-3 — 관리자 안내문) -->
      <div class="clause-guide">
        <p class="clause-guide__title">계약서 양식 권장 조항 (노무사 최종 검토 권장)</p>
        <p class="clause-guide__text">
          "근로계약기간은 근로일 당일 1일로 한다. 역일을 달리하여 계속되는 근무는 시업일의
          1근로일로 본다. 회사가 근로자의 노무 제공을 수령한 근로일에 한하여 본 계약과 동일
          조건으로 계약이 성립하며, 앱 로그인·계정 활성화·출근 기록의 존재만으로는 계약이
          성립하지 아니한다."
        </p>
        <p class="clause-guide__warn">
          계약서를 교체(새 버전 등록)하면 해당 사업장의 모든 일용직이 다음 로그인 시
          재서명해야 합니다. 반복 갱신 장기화(기간제법 2년) 이슈는 시스템이 통제하지 않으므로
          운영상 별도 관리가 필요합니다.
        </p>
      </div>

      <!-- 활성 계약서 요약 카드 -->
      <div class="active-card" v-if="siteCd">
        <template v-if="activeContract">
          <div class="active-card__info">
            <span class="status-badge is-active">사용중</span>
            <span class="active-card__name">{{ activeContract.contractNm }}</span>
            <span class="active-card__meta">
              v{{ activeContract.contractVer }} · 등록 {{ activeContract.insertDate }} ·
              {{ activeContract.insertNm }}
            </span>
          </div>
          <div class="active-card__actions">
            <button class="btn btn-sm btn-primary" @click="fnPreview(activeContract)">
              미리보기
            </button>
            <button
              v-if="canDelete"
              class="btn btn-sm btn-primary"
              @click="fnStop"
            >
              사용중지
            </button>
          </div>
        </template>
        <template v-else>
          <p class="active-card__empty">
            등록된 계약서가 없습니다. 계약서 미등록 사업장은 입장 승인제만 적용되고
            서명 게이트는 건너뜁니다.
          </p>
        </template>
      </div>

      <!-- 버전 이력 테이블 -->
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">계약서 버전 이력</span>
          </div>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 52vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed text-sm text-left rtl:text-right">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 4%">No</th>
                <ThSortable
                  label="버전"
                  col-key="contractVer"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.contractVer"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="계약서명"
                  col-key="contractNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.contractNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="상태"
                  col-key="useYn"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.useYn"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="등록자"
                  col-key="insertNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.insertNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="등록일시"
                  col-key="insertDate"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.insertDate"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="event_cell" style="text-align: center; width: 100px">관리</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!contracts || contracts.length === 0">
                <tr>
                  <td colspan="7" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in sortedData" :key="row.contractVer">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td style="text-align: center">v{{ row.contractVer }}</td>
                  <td>{{ row.contractNm }}</td>
                  <td style="text-align: center">
                    <span
                      class="status-badge"
                      :class="row.useYn === 'Y' ? 'is-active' : 'is-released'"
                    >
                      {{ row.useYn === "Y" ? "사용중" : "종료" }}
                    </span>
                  </td>
                  <td>{{ row.insertNm }}</td>
                  <td>{{ row.insertDate }}</td>
                  <td style="text-align: center">
                    <button class="btn btn-sm btn-primary" @click="fnPreview(row)">
                      미리보기
                    </button>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  computed,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import { resolveApiErrorMessage } from "@/utils/apiError";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import DailyContractRegPop from "@/views/user/popup/DailyContractRegPop.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

// =========================== Define ===========================
defineOptions({ name: "User_07" });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const siteList = ref([]);
const siteCd = ref("");
const contracts = ref([]);

const { sortKey, sortOrder, sortedData, onSort } = useTableSort(contracts);
const { colWidths, onResize } = useColumnResize({
  contractVer: 70,
  contractNm: 260,
  useYn: 90,
  insertNm: 120,
  insertDate: 170,
});

// 활성(사용중) 계약서 — 사업장당 1건(서버 기능성 유니크 보장)
const activeContract = computed(
  () => contracts.value.find((c) => c.useYn === "Y") || null,
);

// 사용중지 권한 — 메뉴 버튼 권한(BTN_DELT)으로 노출 제어(User_06 미러)
const canDelete = computed(() => localButtons.value?.delete === "Y");

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// =========================== Life Cycle ===========================
// sessionStorage 값이 "null"/"undefined" 문자열로 저장된 경우 방어(User_05 getSession 미러).
const getSession = (key) => {
  const v = sessionStorage.getItem(key);
  return v && v !== "null" && v !== "undefined" ? v : "";
};

onMounted(async () => {
  await fnLoadSiteList();
});

// =========================== Methods ===========================
// 사업장 목록 조회 — /comApi/baseinfo/site-lists (공통 사업장 조회 미러, 셀렉트 채움용).
//   기본 선택: 세션 사업장(gv_siteCd)이 목록에 있으면 우선, 없으면 단일 사업장일 때 자동 선택.
const fnLoadSiteList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: "",
        siteNm: "",
      },
    });

    if (response.status === 200) {
      siteList.value = response.data?.siteInfoResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "사업장 조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
    return;
  }

  const sessionSiteCd = getSession("gv_siteCd");
  if (sessionSiteCd && siteList.value.some((s) => s.siteCd === sessionSiteCd)) {
    siteCd.value = sessionSiteCd;
  } else if (siteList.value.length === 1) {
    siteCd.value = siteList.value[0].siteCd;
  }

  if (siteCd.value) {
    await fnSearch();
  }
};

// 버전 이력 조회 — GET /webApi/user07/contract-lists?siteCd= (cmpnyCd 는 서버 JWT)
const fnSearch = async () => {
  contracts.value = [];
  if (!siteCd.value) return;

  try {
    const response = await axios.get("/webApi/user07/contract-lists", {
      params: { siteCd: siteCd.value },
    });

    if (response.status === 200) {
      // 등록자명(insertNm)은 서버가 TB_USER 조인 이름으로 반환(탈퇴/부재 시 서버가 USER_CD 폴백).
      //   구버전 응답 대비 insertNo 폴백 유지. 활성 요약 카드(activeContract)는 computed 가 자동 도출.
      contracts.value = (response.data?.versionList || []).map((c) => ({
        contractVer: c.contractVer,
        contractNm: c.contractNm,
        useYn: c.useYn,
        insertNm: c.insertNm || c.insertNo,
        insertDate: c.insertDate,
      }));
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 등록/교체 팝업 — 저장 성공 시 목록 갱신. 새 버전 등록 = 기존 활성 자동 종료(D8 재서명 트리거)
const fnOpenRegPop = () => {
  if (!siteCd.value) {
    proxy.$alert("사업장을 먼저 선택해주세요.");
    return;
  }
  openPop(DailyContractRegPop, {
    siteCd: siteCd.value,
    hasActive: !!activeContract.value,
    onSaved: fnSearch,
  });
};

// 미리보기 — GET /webApi/user07/contract-image?siteCd=&contractVer= (이미지 스트림)
//   파일 경로 직접 노출 금지 — 스트림 EP blob → objectURL 로 새 탭 표시(subcon03 blob 로드 전례 미러).
const fnPreview = async (row) => {
  try {
    const response = await axios.get("/webApi/user07/contract-image", {
      params: { siteCd: siteCd.value, contractVer: row.contractVer },
      responseType: "blob",
    });

    if (response.status === 200) {
      const url = URL.createObjectURL(response.data);
      window.open(url, "_blank");
      // 새 탭 로드가 끝난 뒤 objectURL 해제(즉시 해제 시 탭에서 로드 실패).
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "미리보기 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 사용중지 — POST /webApi/user07/contract-stop { siteCd }
const fnStop = async () => {
  const ok = await proxy.$confirm(
    "계약서 사용을 중지하시겠습니까?\n중지하면 해당 사업장의 서명 게이트가 비활성화됩니다.",
  );
  if (!ok) return;

  try {
    const response = await axios.post("/webApi/user07/contract-stop", {
      siteCd: siteCd.value,
    });

    if (response.status === 200) {
      await proxy.$alert("사용중지되었습니다.");
      await fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "사용중지 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};
</script>

<style scoped>
/* 검색바 좌측 정렬(User_05/User_06 패턴) */
.viewSearch {
  padding-left: calc(0.5rem + var(--space-md, 0.75rem));
  row-gap: 0.5rem;
}
.viewSearch > div:first-child {
  margin-left: 0;
}

.site-select {
  width: 200px;
}

/* 권장 조항 안내 */
.clause-guide {
  margin: 0 0 0.75rem;
  padding: 0.6rem 0.85rem;
  background: var(--color-warning-bg, #fef3c7);
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.6;
  color: var(--color-text-muted, #4b5563);
}
.clause-guide__title {
  margin: 0 0 0.25rem;
  font-weight: 700;
  color: var(--color-warning-text, #b45309);
}
.clause-guide__text {
  margin: 0;
  white-space: pre-line;
}
.clause-guide__warn {
  margin: 0.4rem 0 0;
  font-weight: 600;
  color: var(--color-warning-text, #b45309);
}

/* 활성 계약서 요약 카드 */
.active-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin: 0 0 0.75rem;
  padding: 0.75rem 1rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-surface, #ffffff);
}
.active-card__info {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  min-width: 0;
}
.active-card__name {
  font-weight: 600;
  color: var(--color-text, #374151);
}
.active-card__meta {
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #6b7280);
}
.active-card__actions {
  display: flex;
  gap: 0.4rem;
  flex-shrink: 0;
}
.active-card__empty {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted, #6b7280);
}

/* 상태 배지(User_06 미러) */
.status-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
.status-badge.is-active {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.status-badge.is-released {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-muted, #4b5563);
}
</style>
