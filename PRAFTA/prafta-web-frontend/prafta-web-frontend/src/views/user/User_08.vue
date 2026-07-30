<template>
  <!--
    User_08.vue — 일용직 입장 승인(탭1) + 계약서 서명 이력(탭2) (웹, 화면 B/C)
    - 분해: .claude/requests/common/작업지시서_일용직-계약서-서명-승인제.plan.md §4 UI-DC-06 / §2 T5
    - 요청서 근거: §4-2 화면 B(개별/일괄 승인·거부 D9, 블랙리스트 바로가기 D10), 화면 C(서명 이력 — 만료 후에도 조회, §6-2 3년 보존)
    - 메뉴: 일일계정 관리 하위(T1 메뉴 SQL). 탭바는 Attd_01 밑줄형 표준.
    - 참조 패턴: Attd_01(탭바), User_06(검색/그리드/배지), DailyBlacklistRegPop(D10 재사용)
    - planner 라운드 스코프: template + style + 탭/체크 UI 토글. script 는 선언 + TODO(developer).
  -->
  <div class="viewComm user08-container">
    <!-- 공통 헤더(제목 + 메뉴 버튼 플래그 기반 공통 버튼: BTN_SRCH=Y → 조회) -->
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="props.buttons"
      @search="fnSearch"
    />

    <!-- 탭바 (Attd_01 밑줄형 표준) -->
    <div class="user08-tab-bar">
      <button
        type="button"
        :class="['user08-tab-btn', { active: activeTab === 'entry' }]"
        @click="activeTab = 'entry'"
      >
        입장 승인
      </button>
      <button
        type="button"
        :class="['user08-tab-btn', { active: activeTab === 'sign' }]"
        @click="activeTab = 'sign'"
      >
        서명 이력
      </button>
    </div>

    <!-- ═══════════ 탭1: 입장 승인 (화면 B) ═══════════ -->
    <div v-show="activeTab === 'entry'" class="user08-tab-content">
      <div class="viewSearch">
        <div>
          <label>사업장</label>
          <select
            v-model="entrySiteCd"
            class="w-select"
            @change="fnSearchEntry"
          >
            <option value="">전체</option>
            <option
              v-for="site in siteList"
              :key="site.siteCd"
              :value="site.siteCd"
            >
              {{ site.siteNm }}
            </option>
          </select>
        </div>
        <div>
          <label>상태</label>
          <select
            v-model="entryStatus"
            class="w-select-sm"
            @change="fnSearchEntry"
          >
            <option value="">전체</option>
            <option value="01">대기</option>
            <option value="02">승인</option>
            <option value="03">거부</option>
            <option value="04">만료</option>
            <option value="05">소진</option>
          </select>
        </div>
        <div>
          <label>유형</label>
          <select
            v-model="entryType"
            class="w-select-sm"
            @change="fnSearchEntry"
          >
            <option value="">전체</option>
            <option value="01">신규가입</option>
            <option value="02">재입장</option>
          </select>
        </div>
        <div>
          <label>요청일</label>
          <CalendarSrch
            v-model="entryDate"
            @update:modelValue="fnSearchEntry"
          />
        </div>
      </div>

      <div class="viewBody">
        <div class="table-wrapper subtitle-pane">
          <div class="subtitle-row">
            <div class="subtitle">
              <span class="subtitle-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="18" height="18">
                  <path d="M4 4h16v4H4zM4 10h10v10H4z" />
                </svg>
              </span>
              <span class="subtitle-text">입장 승인 요청</span>
            </div>
            <!-- 일괄 승인 사전 요약 — 클릭 전에 재서명 대상 규모를 인지시킨다 -->
            <span v-if="checkedReqIds.length > 0" class="bulk-summary">
              선택 {{ checkedReqIds.length }}건
              <template v-if="checkedResignCnt > 0">
                · 재서명 대상 {{ checkedResignCnt }}건
              </template>
            </span>
            <!-- D9: 전체 체크 → 일괄 승인 -->
            <button
              class="btn btn-sm btn-primary"
              :disabled="checkedReqIds.length === 0 || processing"
              @click="fnBulkApprove"
            >
              선택 {{ checkedReqIds.length }}건 일괄 승인
            </button>
          </div>

          <div
            class="table-box overflow-x-auto rounded-md border border-slate-300"
            style="--box-h: 62vh; --box-sticky-top: 1px; --box-ox: auto"
          >
            <table
              class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
            >
              <thead>
                <tr>
                  <th
                    class="event_cell"
                    style="text-align: center; width: 36px"
                  >
                    <input
                      type="checkbox"
                      :checked="allChecked"
                      aria-label="전체 선택"
                      @change="fnToggleAll"
                    />
                  </th>
                  <th class="event_cell" style="text-align: center; width: 4%">
                    No
                  </th>
                  <ThSortable
                    label="이름"
                    col-key="userNm"
                    :sort-key="entrySortKey"
                    :sort-order="entrySortOrder"
                    :width="entryColWidths.userNm"
                    @sort="onEntrySort"
                    @update:width="onEntryResize"
                  />
                  <ThSortable
                    label="휴대폰번호"
                    col-key="mblNo"
                    :sort-key="entrySortKey"
                    :sort-order="entrySortOrder"
                    :width="entryColWidths.mblNo"
                    @sort="onEntrySort"
                    @update:width="onEntryResize"
                  />
                  <ThSortable
                    label="유형"
                    col-key="reqType"
                    :sort-key="entrySortKey"
                    :sort-order="entrySortOrder"
                    :width="entryColWidths.reqType"
                    @sort="onEntrySort"
                    @update:width="onEntryResize"
                  />
                  <!-- 확정 계약서(K9) — 대기 행은 승인 시 확정될 활성 버전, 승인/소진 행은 pin 된 버전.
                       표시 출처가 행 상태에 따라 갈리므로 ThSortable(단일 colKey 정렬)을 쓰지 않는다. -->
                  <th class="event_cell" style="text-align: center; width: 150px">
                    확정 계약서
                  </th>
                  <ThSortable
                    label="요청일시"
                    col-key="reqDtime"
                    :sort-key="entrySortKey"
                    :sort-order="entrySortOrder"
                    :width="entryColWidths.reqDtime"
                    @sort="onEntrySort"
                    @update:width="onEntryResize"
                  />
                  <ThSortable
                    label="상태"
                    col-key="reqStatus"
                    :sort-key="entrySortKey"
                    :sort-order="entrySortOrder"
                    :width="entryColWidths.reqStatus"
                    @sort="onEntrySort"
                    @update:width="onEntryResize"
                  />
                  <ThSortable
                    label="처리자/일시"
                    col-key="procDtime"
                    :sort-key="entrySortKey"
                    :sort-order="entrySortOrder"
                    :width="entryColWidths.procDtime"
                    @sort="onEntrySort"
                    @update:width="onEntryResize"
                  />
                  <th
                    class="event_cell"
                    style="text-align: center; width: 120px"
                  >
                    관리
                  </th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!entryRequests || entryRequests.length === 0">
                  <tr>
                    <td colspan="10" class="edu-grid-empty">
                      등록된 세부 항목이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="(row, idx) in entrySortedData" :key="row.reqId">
                    <td style="text-align: center">
                      <input
                        v-if="row.reqStatus === '01'"
                        type="checkbox"
                        :checked="row.checked"
                        aria-label="선택"
                        @change="row.checked = !row.checked"
                      />
                    </td>
                    <td style="text-align: center">{{ idx + 1 }}</td>
                    <td>{{ row.userNm }}</td>
                    <!-- 서버 복호화 평문("010-XXXX-XXXX") 그대로 표기 (2026-07-17 마스킹 해제) -->
                    <td style="text-align: center">{{ row.mblNo }}</td>
                    <td style="text-align: center">
                      <span
                        class="type-badge"
                        :class="row.reqType === '01' ? 'is-new' : 'is-reentry'"
                      >
                        {{ row.reqType === "01" ? "신규가입" : "재입장" }}
                      </span>
                    </td>
                    <!-- 확정 계약서 — 값 없음/센티넬 0/레거시 NULL 을 구분해 표기(2-2 표) -->
                    <td class="contract-cell" style="text-align: center">
                      <span
                        class="contract-cell__ver"
                        :class="{ 'is-none': !fnEffVerText(row).hasVer }"
                        :title="fnEffVerText(row).title"
                      >
                        {{ fnEffVerText(row).text }}
                      </span>
                      <span v-if="fnEffFormat(row)" class="contract-cell__fmt">
                        · {{ fnEffFormat(row) }}
                      </span>
                      <span
                        v-if="fnNeedsResign(row)"
                        class="resign-badge"
                        :title="fnResignTitle(row)"
                      >
                        재서명 대상
                      </span>
                    </td>
                    <td>{{ row.reqDtime }}</td>
                    <td style="text-align: center">
                      <span
                        class="status-badge"
                        :class="statusClass(row.reqStatus)"
                      >
                        {{ statusLabel(row.reqStatus) }}
                      </span>
                    </td>
                    <td>
                      {{
                        row.procNm ? `${row.procNm} / ${row.procDtime}` : "-"
                      }}
                    </td>
                    <td style="text-align: center">
                      <template v-if="row.reqStatus === '01'">
                        <button
                          class="btn btn-sm btn-primary"
                          :disabled="processing"
                          @click="fnApprove(row)"
                        >
                          승인
                        </button>
                        <button
                          class="btn btn-sm btn-primary btn-reject"
                          :disabled="processing"
                          @click="fnOpenReject(row)"
                        >
                          거부
                        </button>
                      </template>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 거부 사유 인라인 패널 (D10: 사유 기록 + 블랙리스트 바로가기) -->
      <div
        v-if="rejectTarget"
        class="reject-overlay"
        @click.self="fnCloseReject"
      >
        <div class="reject-panel">
          <p class="reject-panel__title">입장 거부</p>
          <p class="reject-panel__who">
            {{ rejectTarget.userNm }} ({{ rejectTarget.mblNo }})
          </p>
          <textarea
            ref="rejectReasonFcs"
            v-model.trim="rejectReason"
            class="reject-panel__reason"
            rows="3"
            maxlength="200"
            placeholder="거부 사유 (필수, 최대 200자 — 내부 기록용, 근로자에게 노출되지 않습니다)"
          ></textarea>
          <div class="reject-panel__btns">
            <button class="btn btn-sm btn-primary" @click="fnCloseReject">
              취소
            </button>
            <button
              class="btn btn-sm btn-primary btn-reject"
              :disabled="rejectReason.length === 0 || processing"
              @click="fnConfirmReject"
            >
              거부 확정
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══════════ 탭2: 서명 이력 (화면 C) ═══════════ -->
    <div v-show="activeTab === 'sign'" class="user08-tab-content">
      <div class="viewSearch">
        <div>
          <label>사업장</label>
          <select v-model="signSiteCd" class="w-select" @change="fnSearchSign">
            <option value="">전체</option>
            <option
              v-for="site in siteList"
              :key="site.siteCd"
              :value="site.siteCd"
            >
              {{ site.siteNm }}
            </option>
          </select>
        </div>
        <div>
          <label>서명일</label>
          <CalendarSrch
            v-model="signFromDate"
            @update:modelValue="fnSearchSign"
          />
          <span class="date-range-sep">~</span>
          <CalendarSrch
            v-model="signToDate"
            @update:modelValue="fnSearchSign"
          />
        </div>
        <div>
          <label>이름</label>
          <input
            v-model.trim="signUserNm"
            type="text"
            placeholder="이름"
            style="width: 120px"
            @keyup.enter="fnSearchSign"
          />
        </div>
      </div>

      <div class="viewBody">
        <div class="table-wrapper subtitle-pane">
          <div class="subtitle-row">
            <div class="subtitle">
              <span class="subtitle-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="18" height="18">
                  <path d="M4 4h16v4H4zM4 10h10v10H4z" />
                </svg>
              </span>
              <span class="subtitle-text">계약서 서명 이력</span>
            </div>
            <span class="retain-note">
              서명본은 계정 만료·탈퇴 후에도 3년간 보존·조회됩니다 (근로기준법
              §42)
            </span>
          </div>

          <div
            class="table-box overflow-x-auto rounded-md border border-slate-300"
            style="--box-h: 62vh; --box-sticky-top: 1px; --box-ox: auto"
          >
            <table
              class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
            >
              <thead>
                <tr>
                  <th class="event_cell" style="text-align: center; width: 4%">
                    No
                  </th>
                  <ThSortable
                    label="이름"
                    col-key="userNm"
                    :sort-key="signSortKey"
                    :sort-order="signSortOrder"
                    :width="signColWidths.userNm"
                    @sort="onSignSort"
                    @update:width="onSignResize"
                  />
                  <ThSortable
                    label="휴대폰번호"
                    col-key="mblNo"
                    :sort-key="signSortKey"
                    :sort-order="signSortOrder"
                    :width="signColWidths.mblNo"
                    @sort="onSignSort"
                    @update:width="onSignResize"
                  />
                  <ThSortable
                    label="계약서 버전"
                    col-key="contractVer"
                    :sort-key="signSortKey"
                    :sort-order="signSortOrder"
                    :width="signColWidths.contractVer"
                    @sort="onSignSort"
                    @update:width="onSignResize"
                  />
                  <ThSortable
                    label="서명일시"
                    col-key="signDtime"
                    :sort-key="signSortKey"
                    :sort-order="signSortOrder"
                    :width="signColWidths.signDtime"
                    @sort="onSignSort"
                    @update:width="onSignResize"
                  />
                  <ThSortable
                    label="해시(SHA-256)"
                    col-key="mergedSha256"
                    :sort-key="signSortKey"
                    :sort-order="signSortOrder"
                    :width="signColWidths.mergedSha256"
                    @sort="onSignSort"
                    @update:width="onSignResize"
                  />
                  <th
                    class="event_cell"
                    style="text-align: center; width: 130px"
                  >
                    관리
                  </th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!signHistories || signHistories.length === 0">
                  <tr>
                    <td colspan="7" class="edu-grid-empty">
                      등록된 세부 항목이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="(row, idx) in signSortedData" :key="row.signId">
                    <td style="text-align: center">{{ idx + 1 }}</td>
                    <td>{{ row.userNm }}</td>
                    <!-- 서버 복호화 평문("010-XXXX-XXXX") 그대로 표기 (2026-07-17 마스킹 해제) -->
                    <td style="text-align: center">{{ row.mblNo }}</td>
                    <td style="text-align: center">v{{ row.contractVer }}</td>
                    <td>{{ row.signDtime }}</td>
                    <td class="hash-cell" :title="row.mergedSha256">
                      {{ shortHash(row.mergedSha256) }}
                    </td>
                    <td style="text-align: center">
                      <button
                        class="btn btn-sm btn-primary"
                        @click="fnViewSign(row)"
                      >
                        열람
                      </button>
                      <button
                        class="btn btn-sm btn-primary"
                        @click="fnDownloadSign(row)"
                      >
                        다운로드
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
  nextTick,
  watch,
} from "vue";
import { useModal } from "@/utils/useModal";
import {
  resolveApiErrorMessage,
  resolveBlobApiErrorMessage,
} from "@/utils/apiError";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import DailyBlacklistRegPop from "@/views/user/popup/DailyBlacklistRegPop.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

// =========================== Define ===========================
defineOptions({ name: "User_08" });
const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// =========================== 공통 ===========================
const activeTab = ref("entry");
const siteList = ref([]);
const processing = ref(false);

// =========================== 탭1: 입장 승인 ===========================
const entrySiteCd = ref("");
const entryStatus = ref("01"); // 기본 = 대기
const entryType = ref("");
// 기본값 = 오늘(YYYY-MM-DD, 로컬 기준). CalendarSrch 는 부모의 프로그램적 세팅에도 update 를
//   재방출하므로(onMounted 세팅 시 사업장 미선택 상태에서 조회 오발동) 선언 시점에 초기화한다.
const entryDate = ref(fnTodayIso());

// 항목: { reqId, userNm, mblNo(서버 복호화 평문 "010-XXXX-XXXX"), reqType, reqStatus, reqDtime, procNm, procDtime, checked }
const entryRequests = ref([]);
const {
  sortKey: entrySortKey,
  sortOrder: entrySortOrder,
  sortedData: entrySortedData,
  onSort: onEntrySort,
} = useTableSort(entryRequests);
const { colWidths: entryColWidths, onResize: onEntryResize } = useColumnResize({
  userNm: 110,
  mblNo: 130,
  reqType: 90,
  reqDtime: 160,
  reqStatus: 80,
  procDtime: 180,
});

const checkedReqIds = computed(() =>
  entryRequests.value.filter((r) => r.checked && r.reqStatus === "01").map((r) => r.reqId),
);
const pendingRows = computed(() =>
  entryRequests.value.filter((r) => r.reqStatus === "01"),
);
const allChecked = computed(
  () => pendingRows.value.length > 0 && pendingRows.value.every((r) => r.checked),
);

// 거부 패널
const rejectTarget = ref(null);
const rejectReason = ref("");
const rejectReasonFcs = ref(null);

const statusLabel = (s) =>
  ({ "01": "대기", "02": "승인", "03": "거부", "04": "만료", "05": "소진" })[s] || s;
const statusClass = (s) =>
  ({
    "01": "is-pending",
    "02": "is-approved",
    "03": "is-rejected",
    "04": "is-expired",
    "05": "is-consumed",
  })[s] || "";

// ─────────── 확정 계약서 표시(K9) ───────────
// 표시 출처: 대기('01')=activeContractVer(승인 시 확정될 값) / 승인·소진('02','05')=pinnedContractVer(pin)
//   거부·만료('03','04')는 확정 행위가 없어 pin 이 없다(J2) → 표기하지 않는다.
// 특수값: null(대기)=사업장 미등록 / 0(승인·소진)=승인 시점 미등록 센티넬 / null(승인·소진)=pin 도입 전 레거시
const ENTRY_PINNED_STATUS = ["02", "05"];

/** 표시 대상 버전 원시값 — 행 상태별 출처만 고른다(가공 없음). undefined = 표기 대상 아님 */
const fnEffVerRaw = (row) => {
  if (row.reqStatus === "01") return row.activeContractVer;
  if (ENTRY_PINNED_STATUS.includes(row.reqStatus)) return row.pinnedContractVer;
  return undefined;
};

/** 셀 표시 텍스트 + tooltip + 버전 보유 여부 */
const fnEffVerText = (row) => {
  const raw = fnEffVerRaw(row);
  if (raw === undefined) {
    return { text: "-", title: "", hasVer: false };
  }
  if (raw === null) {
    return ENTRY_PINNED_STATUS.includes(row.reqStatus)
      ? {
          text: "레거시",
          title:
            "pin 도입 전 승인 건 — 서명 시점의 활성 계약서 기준으로 판정됩니다.",
          hasVer: false,
        }
      : {
          text: "없음",
          title: "계약서 미등록 사업장 — 승인해도 서명 게이트를 건너뜁니다.",
          hasVer: false,
        };
  }
  if (Number(raw) === 0) {
    return {
      text: "없음",
      title:
        "승인 시점에 계약서가 미등록이었습니다 — 이 사이클은 서명 게이트를 건너뜁니다.",
      hasVer: false,
    };
  }
  return { text: `v${raw}`, title: "", hasVer: true };
};

/** 표시 버전의 형식 라벨 — 값이 없으면 표기 생략(추측 금지).
 *  서버가 pinnedFormatType 을 주지 않는 동안은 "표시 버전 == 활성 버전"일 때만 activeFormatType 을 쓴다. */
const fnEffFormat = (row) => {
  const info = fnEffVerText(row);
  if (!info.hasVer) return "";
  const raw = fnEffVerRaw(row);
  const type =
    row.pinnedFormatType ??
    (Number(raw) === Number(row.activeContractVer) ? row.activeFormatType : null);
  if (!type) return "";
  return type === "PDF" ? "PDF" : "이미지";
};

/** 재서명 대상 — 표시 버전이 있고, 그 버전이 최종 서명 버전과 다를 때 */
const fnNeedsResign = (row) => {
  const info = fnEffVerText(row);
  if (!info.hasVer) return false;
  return Number(fnEffVerRaw(row)) !== Number(row.lastSignedContractVer);
};

const fnResignTitle = (row) => {
  const last = row.lastSignedContractVer;
  if (last === null || last === undefined) {
    return "서명 이력 없음 — 다음 로그인 시 서명이 필요합니다.";
  }
  return `최종 서명 v${last} → 확정 v${fnEffVerRaw(row)} (다음 로그인 시 재서명)`;
};

/** 일괄 승인 사전 요약 — 체크된 대기 행 중 재서명 대상 건수 */
const checkedResignCnt = computed(
  () =>
    entryRequests.value.filter(
      (r) => r.checked && r.reqStatus === "01" && fnNeedsResign(r),
    ).length,
);

// =========================== 탭2: 서명 이력 ===========================
const signSiteCd = ref("");
const signFromDate = ref("");
const signToDate = ref("");
const signUserNm = ref("");

// 항목: { signId, userNm(스냅샷), mblNo(서버 복호화 평문 "010-XXXX-XXXX"), contractVer, signDtime, mergedSha256 }
const signHistories = ref([]);
const {
  sortKey: signSortKey,
  sortOrder: signSortOrder,
  sortedData: signSortedData,
  onSort: onSignSort,
} = useTableSort(signHistories);
const { colWidths: signColWidths, onResize: onSignResize } = useColumnResize({
  userNm: 110,
  mblNo: 130,
  contractVer: 100,
  signDtime: 160,
  mergedSha256: 220,
});

const shortHash = (h) => (h ? `${h.slice(0, 12)}…` : "-");

// =========================== Life Cycle ===========================
// sessionStorage 값이 "null"/"undefined" 문자열로 저장된 경우 방어(User_05 getSession 미러).
const getSession = (key) => {
  const v = sessionStorage.getItem(key);
  return v && v !== "null" && v !== "undefined" ? v : "";
};

// 로컬 기준 오늘(YYYY-MM-DD) — toISOString()은 UTC 라 KST 00~09시에 전날로 밀리는 결함 전례, 사용 금지.
// 함수 선언문(호이스팅) — 위 entryDate 선언 시점 초기화에서 호출되므로 화살표 const 로 바꾸지 말 것.
function fnTodayIso() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

// CalendarSrch 값(YYYY-MM-DD) → 서버 포맷(YYYYMMDD)
const fnToYyyymmdd = (v) => (v ? String(v).replace(/-/g, "") : "");

onMounted(async () => {
  await fnLoadSiteList();
});

// 탭2 최초 진입 시 1회 자동 조회(사업장이 기본 선택되어 있을 때만 — 서버는 사업장 단위 조회만 허용).
const signLoaded = ref(false);
watch(activeTab, async (tab) => {
  if (tab === "sign" && !signLoaded.value && signSiteCd.value) {
    signLoaded.value = true;
    await fnSearchSign();
  }
});

// =========================== 공통 Methods ===========================
// 공통 헤더 조회 버튼 — 활성 탭 기준으로 해당 탭 조회를 실행한다.
const fnSearch = async () => {
  if (activeTab.value === "entry") {
    await fnSearchEntry();
  } else {
    await fnSearchSign();
  }
};

// 사업장 목록 조회 — /comApi/baseinfo/site-lists (공통 사업장 조회 미러, 셀렉트 채움용).
//   기본 선택: 세션 사업장(gv_siteCd)이 목록에 있으면 우선, 없으면 단일 사업장일 때 자동 선택.
//   (서버 entry/sign 조회는 siteCd 필수 — "전체" 상태에서는 조회하지 않는다.)
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
  let defaultSiteCd = "";
  if (sessionSiteCd && siteList.value.some((s) => s.siteCd === sessionSiteCd)) {
    defaultSiteCd = sessionSiteCd;
  } else if (siteList.value.length === 1) {
    defaultSiteCd = siteList.value[0].siteCd;
  }

  entrySiteCd.value = defaultSiteCd;
  signSiteCd.value = defaultSiteCd;

  if (entrySiteCd.value) {
    await fnSearchEntry();
  }
};

// =========================== 탭1 Methods ===========================
// 목록 조회 — GET /webApi/user08/entry-request-lists
//   params: { siteCd, reqStatus, reqType, reqDate } (cmpnyCd 는 서버 JWT. 휴대폰은 서버 last4 마스킹)
const fnSearchEntry = async () => {
  entryRequests.value = [];
  if (!entrySiteCd.value) {
    // 서버는 사업장 인가 가드 대상이라 siteCd 필수 — "전체" 선택은 조회 불가 안내.
    await proxy.$alert("사업장을 선택해주세요.");
    return;
  }

  try {
    const response = await axios.get("/webApi/user08/entry-request-lists", {
      params: {
        siteCd: entrySiteCd.value,
        reqStatus: entryStatus.value,
        reqType: entryType.value,
        reqDate: fnToYyyymmdd(entryDate.value),
      },
    });

    if (response.status === 200) {
      // 서버 항목(procUserNm)을 템플릿 필드(procNm)로 매핑 + checked 초기화.
      //   휴대폰은 서버가 복호화·하이픈 포맷한 평문("010-XXXX-XXXX")을 가공 없이 그대로 표시.
      entryRequests.value = (response.data?.entryRequestList || []).map((r) => ({
        reqId: r.reqId,
        userNm: r.userNm,
        mblNo: r.mblNo || "-",
        reqType: r.reqType,
        reqStatus: r.reqStatus,
        reqDtime: r.reqDtime,
        procNm: r.procUserNm,
        procDtime: r.procDtime,
        checked: false,
        // 확정 계약서 표시용(백엔드 T4) — 값이 없으면 화면이 "-"/형식 생략으로 degrade 한다.
        //   ★반드시 `??` — `||` 로 폴백하면 센티넬 0(승인 시 미등록=게이트 스킵)이
        //     null(레거시=활성 폴백)로 바뀌어 화면이 정반대 정보를 준다.
        pinnedContractVer: r.pinnedContractVer ?? null,
        activeContractVer: r.activeContractVer ?? null,
        lastSignedContractVer: r.lastSignedContractVer ?? null,
        activeFormatType: r.activeFormatType ?? null,
        pinnedFormatType: r.pinnedFormatType ?? null,
      }));
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 전체 선택 토글(대기 상태 행만 — UI 토글)
const fnToggleAll = () => {
  const next = !allChecked.value;
  pendingRows.value.forEach((r) => {
    r.checked = next;
  });
};

// 승인 공통 처리 — POST /webApi/user08/entry-approve { reqIds } (D9, all-or-nothing 서버 롤백)
const fnProcessApprove = async (reqIds) => {
  if (processing.value) return;
  processing.value = true;

  try {
    const response = await axios.post("/webApi/user08/entry-approve", {
      reqIds,
    });

    if (response.status === 200) {
      const count = response.data?.processedCount ?? reqIds.length;
      await proxy.$alert(`${count}건 승인 처리되었습니다.`);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "승인 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    processing.value = false;
    // 성공/실패(타 관리자 선처리 400_001 포함) 모두 최신 상태로 재조회 —
    //   stale 목록으로 동일 세트 반복 실패 방지(앱 AdminEntryApprovalView finally 재조회 미러, qa L-3).
    await fnSearchEntry();
  }
};

// 개별 승인 — POST /webApi/user08/entry-approve { reqIds: [row.reqId] }
const fnApprove = async (row) => {
  const ok = await proxy.$confirm(`${row.userNm} 님의 입장을 승인하시겠습니까?`);
  if (!ok) return;
  await fnProcessApprove([row.reqId]);
};

// 일괄 승인(D9) — POST /webApi/user08/entry-approve { reqIds: checkedReqIds }
const fnBulkApprove = async () => {
  // 재서명 대상이 있으면 규모를 confirm 에 함께 노출한다(§2-4 문구 원문). 0건이면 기존 문구 유지.
  const bulkCnt = checkedReqIds.value.length;
  const resignCnt = checkedResignCnt.value;
  const ok = await proxy.$confirm(
    resignCnt > 0
      ? `선택한 ${bulkCnt}건을 일괄 승인하시겠습니까?\n${bulkCnt}건 중 ${resignCnt}건이 재서명 대상입니다.`
      : `선택한 ${bulkCnt}건을 일괄 승인하시겠습니까?`,
  );
  if (!ok) return;
  await fnProcessApprove(checkedReqIds.value);
};

// 거부 패널 열기/닫기
const fnOpenReject = async (row) => {
  rejectTarget.value = row;
  rejectReason.value = "";
  await nextTick();
  rejectReasonFcs.value?.focus();
};
const fnCloseReject = () => {
  rejectTarget.value = null;
  rejectReason.value = "";
};

// 거부 확정 — POST /webApi/user08/entry-reject { reqId, reason }
const fnConfirmReject = async () => {
  if (!rejectTarget.value || rejectReason.value.length === 0) return;
  if (processing.value) return;
  processing.value = true;

  let rejected = false;
  try {
    const response = await axios.post("/webApi/user08/entry-reject", {
      reqId: rejectTarget.value.reqId,
      reason: rejectReason.value,
    });

    if (response.status === 200) {
      rejected = true;
      fnCloseReject();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "거부 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    processing.value = false;
    // 성공/실패(타 관리자 선처리 400_001 포함) 모두 최신 상태로 재조회 —
    //   stale 목록으로 동일 건 반복 실패 방지(앱 AdminEntryApprovalView finally 재조회 미러, qa L-3).
    await fnSearchEntry();
  }

  if (!rejected) return;

  // D10 블랙리스트 등록 바로가기 — 서버는 휴대폰 마스킹 값만 응답하므로 평문 프리필 불가,
  //   팝업만 열고 수기 입력한다(DailyBlacklistRegPop 무변경).
  const toBlacklist = await proxy.$confirm("블랙리스트에 등록하시겠습니까?");
  if (toBlacklist) {
    openPop(DailyBlacklistRegPop, { onSaved: () => {} });
  }
};

// =========================== 탭2 Methods ===========================
// 서명 이력 조회 — GET /webApi/user08/contract-sign-lists
//   params: { siteCd, fromDate, toDate, userNm } (만료/탈퇴 계정 포함 — 서버가 스냅샷 이름 반환)
const fnSearchSign = async () => {
  signHistories.value = [];
  if (!signSiteCd.value) {
    // 서버는 사업장 인가 가드 대상이라 siteCd 필수 — "전체" 선택은 조회 불가 안내.
    await proxy.$alert("사업장을 선택해주세요.");
    return;
  }

  try {
    const response = await axios.get("/webApi/user08/contract-sign-lists", {
      params: {
        siteCd: signSiteCd.value,
        fromDate: fnToYyyymmdd(signFromDate.value),
        toDate: fnToYyyymmdd(signToDate.value),
        userNm: signUserNm.value,
      },
    });

    if (response.status === 200) {
      // 휴대폰은 서버가 복호화·하이픈 포맷한 평문("010-XXXX-XXXX")을 가공 없이 그대로 표시.
      signHistories.value = (response.data?.contractSignList || []).map((r) => ({
        signId: r.signId,
        userNm: r.userNm,
        mblNo: r.mblNo || "-",
        contractVer: r.contractVer,
        signDtime: r.signDtime,
        mergedSha256: r.mergedSha256,
      }));
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 서명본 확장자 매핑 — 서명본은 신규=PDF, 레거시=PNG 합성본이 혼재한다.
//   화면 데이터로 추측하면 혼재 목록에서 오판하므로 반드시 응답 Content-Type(blob.type)으로 판정한다.
const SIGN_EXT_BY_MIME = {
  "application/pdf": "pdf",
  "image/png": "png",
  "image/jpeg": "jpg",
};

/** 서명본 저장 확장자 — 미상/공백 Content-Type 은 png 폴백(다운로드 자체는 실패시키지 않는다). */
const resolveSignFileExt = (blob) => {
  const mime = String(blob?.type || "")
    .split(";")[0]
    .trim()
    .toLowerCase();
  return SIGN_EXT_BY_MIME[mime] || "png";
};

// 서명본 blob 조회 공통 — 스트림 EP 만 사용(파일 경로 비노출, 인가는 서버 core 가드).
const fnLoadSignBlob = async (signId) => {
  const response = await axios.get("/webApi/user08/contract-sign-image", {
    params: { signId },
    responseType: "blob",
  });
  return response.data;
};

// 서명본 열람 — GET /webApi/user08/contract-sign-image?signId= (스트림. 파일 경로 직접 노출 금지)
const fnViewSign = async (row) => {
  try {
    const blob = await fnLoadSignBlob(row.signId);
    const url = URL.createObjectURL(blob);
    // 응답 대기(await) 뒤의 window.open 은 사용자 제스처와 끊겨 팝업 차단에 걸릴 수 있다.
    const win = window.open(url, "_blank");
    if (!win) {
      URL.revokeObjectURL(url);
      await proxy.$alert(
        "브라우저가 새 창을 차단했습니다.\n팝업 차단을 해제한 뒤 다시 시도해주세요.",
      );
      return;
    }
    // 새 탭 로드가 끝난 뒤 objectURL 해제(즉시 해제 시 탭에서 로드 실패).
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  } catch (err) {
    // 스트림 EP 라 에러 본문도 Blob 으로 도착한다 — blob 전용 리졸버로 서버 사유를 그대로 노출.
    const msg = await resolveBlobApiErrorMessage(
      err,
      "열람 중 오류가 발생했습니다.",
    );
    await proxy.$alert(msg);
  }
};

// 서명본 다운로드 — 동일 EP blob → a[download] 트리거 (User_01 양식 다운로드 전례 미러)
const fnDownloadSign = async (row) => {
  try {
    const blob = await fnLoadSignBlob(row.signId);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    // 파일명은 signId 만 사용(성명/휴대폰 등 PII 삽입 금지) + 확장자는 응답 Content-Type 기준.
    link.download = `contract-sign-${row.signId}.${resolveSignFileExt(blob)}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  } catch (err) {
    const msg = await resolveBlobApiErrorMessage(
      err,
      "다운로드 중 오류가 발생했습니다.",
    );
    await proxy.$alert(msg);
  }
};
</script>

<style scoped>
.user08-container {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 탭바 — Attd_01 밑줄형 표준 미러 */
.user08-tab-bar {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.user08-tab-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
}
.user08-tab-btn:hover {
  color: var(--color-text, #374151);
}
.user08-tab-btn.active {
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  border-bottom-color: var(--color-primary);
}
.user08-tab-content {
  flex: 1;
  min-height: 0;
}

/* 검색바 좌측 정렬(User_05/06 패턴) */
.viewSearch {
  padding-left: calc(0.5rem + var(--space-md, 0.75rem));
  row-gap: 0.5rem;
}
.viewSearch > div:first-child {
  margin-left: 0;
}
.w-select {
  width: 180px;
}
.w-select-sm {
  width: 110px;
}
/* from~to 구분자 (User_05 .date-range-sep 표준) */
.date-range-sep {
  margin: 0 0.4rem;
  color: var(--color-text-muted, #6b7280);
}

/* 유형 배지 */
.type-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
.type-badge.is-new {
  background: var(--color-info-bg, #dbeafe);
  color: var(--color-info-text, #1d4ed8);
}
.type-badge.is-reentry {
  background: var(--color-success-bg, #dcfce7);
  color: var(--color-success-text, #15803d);
}

/* 상태 배지 */
.status-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
.status-badge.is-pending {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.status-badge.is-approved {
  background: var(--color-success-bg, #dcfce7);
  color: var(--color-success-text, #15803d);
}
.status-badge.is-rejected {
  background: var(--color-danger-bg, #fee2e2);
  color: var(--color-danger-text, #b91c1c);
}
.status-badge.is-expired,
.status-badge.is-consumed {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-muted, #4b5563);
}

/* 확정 계약서 셀 — 버전 + 형식 + 재서명 배지(2줄 허용) */
.contract-cell {
  line-height: 1.4;
  white-space: normal;
}
.contract-cell__ver {
  font-weight: 600;
  color: var(--color-text);
}
.contract-cell__ver.is-none {
  font-weight: 400;
  color: var(--color-text-muted);
}
.contract-cell__fmt {
  margin-left: 0.15rem;
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

/* 재서명 대상 배지 — "조치 필요" 톤(대기 배지와 동일 계열) */
.resign-badge {
  display: block;
  margin-top: 0.15rem;
  padding: 0.1rem 0.4rem;
  border-radius: var(--btn-radius);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  font-size: var(--btn-font-sm);
  line-height: 1.4;
  white-space: nowrap;
}

/* 일괄 승인 사전 요약 칩 */
.bulk-summary {
  /* .subtitle-row(table.css)가 space-between 이라 자식이 3개(제목·칩·버튼)가 되면 칩이 행 중앙으로
     밀린다. margin-left:auto 로 남는 공간을 왼쪽에 흡수시켜 버튼 옆(우측)에 붙인다. */
  margin-left: auto;
  margin-right: 0.4rem;
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
  white-space: nowrap;
}

/* 거부 버튼 톤 */
.btn-reject {
  background: var(--color-danger, #ef4444);
  border-color: var(--color-danger, #ef4444);
}

/* 거부 인라인 패널(오버레이) */
.reject-overlay {
  position: fixed;
  inset: 0;
  background: var(--color-overlay, rgba(0, 0, 0, 0.45));
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 90;
}
.reject-panel {
  width: min(420px, calc(100vw - 2rem));
  background: var(--color-surface, #ffffff);
  border-radius: var(--btn-radius, 8px);
  padding: 1rem 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.reject-panel__title {
  margin: 0;
  font-weight: 700;
  color: var(--color-text, #374151);
}
.reject-panel__who {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-muted, #6b7280);
}
.reject-panel__reason {
  width: 100%;
  resize: vertical;
  border: 1px solid var(--color-border-strong, #d1d5db);
  border-radius: var(--input-radius, 10px);
  padding: 0.5rem 0.75rem;
  font: inherit;
  color: var(--color-text, #374151);
}
.reject-panel__reason:focus {
  outline: var(--focus-ring-width, 3px) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset, 2px);
}
.reject-panel__btns {
  display: flex;
  justify-content: flex-end;
  gap: 0.4rem;
}

/* 보존 안내 */
.retain-note {
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-text-muted, #6b7280);
}

/* 해시 셀 — 축약 표시(title 로 전체 노출) */
.hash-cell {
  font-family: var(--font-mono, monospace);
  font-size: 0.75rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
