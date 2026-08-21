<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <div class="viewBody">
      <div class="snapshot-layout">
        <!-- 좌: 스냅샷 목록 -->
        <div class="table-wrapper snapshot-list">
          <div
            class="table-box overflow-x-auto rounded-md border border-slate-300"
            style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
          >
            <table class="data-grid w-full table-fixed text-sm text-left">
              <thead>
                <tr>
                  <th class="event_cell" style="text-align: center; width: 8%">
                    No
                  </th>
                  <th style="width: 14%">유형</th>
                  <th>출처 회사</th>
                  <th>사업장</th>
                  <th>기간</th>
                  <th style="width: 12%">버전</th>
                  <th style="width: 12%">행수</th>
                  <th>표식</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!snapshotList.length">
                  <tr>
                    <td colspan="8" class="edu-grid-empty">
                      공유받은 자료가 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="(row, idx) in snapshotList"
                    :key="row.snapshotId"
                    :class="{ 'is-selected': row.snapshotId === selectedId }"
                    @click="fnSelect(row)"
                  >
                    <td style="text-align: center">{{ idx + 1 }}</td>
                    <td style="text-align: center">
                      {{ dataTypeLabel(row.dataType) }}
                    </td>
                    <td>{{ row.srcCmpnyNm }}</td>
                    <td>{{ row.siteNm }}</td>
                    <td>{{ periodLabel(row) }}</td>
                    <td style="text-align: center">v{{ row.version }}</td>
                    <td style="text-align: center">{{ row.rowCnt }}</td>
                    <td>
                      <span
                        v-if="row.unclosedIncludedYn === 'Y'"
                        class="status-badge is-warn"
                        >미마감 포함</span
                      >
                      <span
                        v-if="
                          row.dataType === 'ATTD' &&
                          row.closedPartialYn === 'Y'
                        "
                        class="status-badge is-warn"
                        >마감분만 · 부분 포함</span
                      >
                      <span
                        v-if="row.relationActiveYn === 'N'"
                        class="status-badge is-closed"
                        >연동 종료</span
                      >
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 우: 상세(읽기전용) -->
        <div class="table-wrapper snapshot-detail">
          <div v-if="selected" class="detail-head">
            <div class="detail-meta">
              출처 {{ selected.srcCmpnyNm }} · {{ periodLabel(selected) }} ·
              생성 {{ selected.createDtime }}
              <span v-if="selected.consentExcludedCnt > 0">
                · 미동의 제외 {{ selected.consentExcludedCnt }}명</span
              >
            </div>
            <div class="detail-ver">
              <label>버전</label>
              <span class="detail-ver-current"
                >v{{ selected.version }} ({{ versionList.length }}개)</span
              >
              <button class="btn btn-custom" @click="showVersionPop = true">
                버전 선택
              </button>
            </div>
          </div>
          <p
            v-if="selected && selected.relationActiveYn === 'N'"
            class="detail-note"
          >
            연동이 종료된 회사의 자료입니다. 열람만 가능합니다.
          </p>

          <!-- ATTD 마감 커버리지 가이드(배지 판정표 D-2~D-4 — RISK/NEARMISS 표식 없음 유지) -->
          <template v-if="selected && selected.dataType === 'ATTD'">
            <p v-if="selected.closedPartialYn === 'Y'" class="detail-note">
              마감분만 포함된 자료입니다.<span v-if="coverageExcludedText">
                제외 내역: {{ coverageExcludedText }}</span
              >
            </p>
            <p
              v-if="selected.closedPartialYn === 'Y' && relayPartialIncluded"
              class="detail-note"
            >
              함께 제공된 연동사 자료에도 부분 포함분이 있습니다.
            </p>
            <p
              v-if="selected.unclosedIncludedYn === 'Y'"
              class="detail-note"
            >
              미마감 근태가 포함된 자료입니다.
            </p>
            <p
              v-if="
                selected.unclosedIncludedYn !== 'Y' &&
                selected.closedPartialYn === 'N' &&
                selected.closedOnlyYn === 'Y'
              "
              class="detail-note"
            >
              마감분 전체가 포함된 자료입니다.
            </p>
            <p
              v-if="
                selected.unclosedIncludedYn !== 'Y' &&
                selected.closedPartialYn == null
              "
              class="detail-note"
            >
              전체 포함 자료입니다.
            </p>
          </template>

          <!-- ATTD: 기존 근태 그리드(그대로 유지) -->
          <template v-if="selected && selected.dataType === 'ATTD'">
            <div
              class="table-box overflow-x-auto rounded-md border border-slate-300"
              style="--box-h: 62vh; --box-sticky-top: 1px; --box-ox: auto"
            >
              <table class="data-grid w-full table-fixed text-sm text-left">
                <thead>
                  <tr>
                    <th style="width: 10%">일자</th>
                    <th style="width: 9%">성명</th>
                    <th>소속</th>
                    <th style="width: 6%">구분</th>
                    <th>근무타입</th>
                    <th>스케줄</th>
                    <th style="width: 8%">출근</th>
                    <th style="width: 8%">퇴근</th>
                    <th style="width: 8%">판정</th>
                    <!-- 인정(분): 스냅샷 생성 시점 고정값(RECOG_MINUTES). NULL(구스냅샷·산출불가)은
                         '-' 로 표시하며 클라이언트 재계산 폴백은 만들지 않는다(반차일 과대 표기 방지). -->
                    <th style="width: 8%">인정(분)</th>
                    <th style="width: 8%">초과(분)</th>
                    <!-- PRAFTA-FIXEDOT-3(M21): 고정연장 실적(분) — 실근태가 근무타입 고정연장
                         구간을 커버한 분. 승인 시점 스냅샷 고정값이며, "연장 미이행" 배지는
                         판정 보조 정보라 공유 대상이 아니다(plan §5-2). -->
                    <th style="width: 8%">고정연장(분)</th>
                    <th>연차</th>
                  </tr>
                </thead>
                <tbody>
                  <template v-if="!detailList.length">
                    <tr>
                      <td colspan="13" class="edu-grid-empty">
                        표시할 데이터가 없습니다.
                      </td>
                    </tr>
                  </template>
                  <template v-else>
                    <tr v-for="row in detailList" :key="row.detailId">
                      <td>{{ fmtYmd(row.workYmd) }}</td>
                      <td>{{ row.workerNm }}</td>
                      <td>{{ row.affilCmpnyNm }}</td>
                      <td style="text-align: center">
                        {{ workerTypeLabel(row.workerType) }}
                      </td>
                      <td>{{ row.schNm }}</td>
                      <td>{{ planLabel(row) }}</td>
                      <td style="text-align: center">
                        {{ fmtHm(row.checkInTime) }}
                      </td>
                      <td style="text-align: center">
                        {{ fmtHm(row.checkOutTime) }}
                      </td>
                      <td style="text-align: center">
                        {{ attdStatusLabel(row.attdStatusCd) }}
                      </td>
                      <!-- 0 은 0 으로 표시 — NULL 만 '-' (fixedOtMinutes 의 || 0 패턴과 다름, ?? 필수) -->
                      <td style="text-align: center">
                        {{ row.recogMinutes ?? "-" }}
                      </td>
                      <td style="text-align: center">
                        {{ row.otMinutes || 0 }}
                      </td>
                      <!-- 구서버/구스냅샷(미수신)이면 0 표시 -->
                      <td style="text-align: center">
                        {{ row.fixedOtMinutes || 0 }}
                      </td>
                      <td>
                        {{ row.leaveNm
                        }}<span v-if="row.leaveMinutes > 0"> · {{ row.leaveMinutes }}분</span>
                      </td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>

            <!-- 페이지 이동 — 상세는 서버가 200행씩 끊어 준다(전체 건수는 목록의 rowCnt). -->
            <div v-if="totalPages > 1" class="detail-pager">
              <button
                class="btn btn-custom"
                :disabled="page <= 1 || isLoading"
                @click="fnMovePage(page - 1)"
              >
                이전
              </button>
              <span class="detail-pager-label">
                {{ page }} / {{ totalPages }} 페이지 · 전체
                {{ selected?.rowCnt || 0 }}건
              </span>
              <button
                class="btn btn-custom"
                :disabled="page >= totalPages || isLoading"
                @click="fnMovePage(page + 1)"
              >
                다음
              </button>
            </div>
          </template>

          <!-- RISK: 위험성평가 표형 상세(자체 조회) -->
          <SnapshotRiskDetail
            v-else-if="selected && selected.dataType === 'RISK'"
            :snapshot-id="selectedId"
          />

          <!-- NEARMISS: 아차사고 카드 + 사진 뷰어(자체 조회) -->
          <SnapshotNearmissDetail
            v-else-if="selected && selected.dataType === 'NEARMISS'"
            :snapshot-id="selectedId"
          />
        </div>
      </div>
    </div>

    <!-- 스냅샷 버전 선택 팝업 — 데이터는 versionList computed 그대로 전달(팝업 자체 API 호출 없음). -->
    <SnapshotVersionSelectPop
      v-if="showVersionPop"
      :version-list="versionList"
      :current-snapshot-id="selectedId"
      @select="fnSelectVersion"
      @close="showVersionPop = false"
    />
  </div>
</template>

<script setup>
/* eslint-disable */
import { ref, computed, defineProps, onMounted, getCurrentInstance, defineOptions } from "vue";
import { resolveApiErrorMessage } from "@/utils/apiError";
import {
  parseCoverageMeta,
  coverageExcludedSummary,
} from "@/utils/snapshotCoverage";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import SnapshotVersionSelectPop from "@/views/subcon/popup/SnapshotVersionSelectPop.vue";
// RISK/NEARMISS 상세는 자식 컴포넌트가 snapshotId prop 변화를 감지해 자체 조회한다 (qa D1: 미등록으로 미렌더되던 기존 결함 수정)
import SnapshotRiskDetail from "@/components/subcon/SnapshotRiskDetail.vue";
import SnapshotNearmissDetail from "@/components/subcon/SnapshotNearmissDetail.vue";

// =========================== Define ===========================
defineOptions({ name: "Subcon_04" });
const props = defineProps({ title: String, buttons: Object });

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const snapshotList = ref([]); // GET /webApi/subcon03/snapshot-lists
const detailList = ref([]); // GET /webApi/subcon03/snapshot-detail
const selectedId = ref(null);
const showVersionPop = ref(false); // 스냅샷 버전 선택 팝업 표시 여부

// 상세 페이징 — 서버가 200행씩 끊는다. 전체 건수는 목록 행의 rowCnt(스냅샷 상세행 수).
const DETAIL_PAGE_SIZE = 200;
const page = ref(1);
const isLoading = ref(false);

const selected = computed(() => snapshotList.value.find((s) => s.snapshotId === selectedId.value) || null);

const totalPages = computed(() => {
  const cnt = selected.value?.rowCnt || 0;
  return cnt > 0 ? Math.ceil(cnt / DETAIL_PAGE_SIZE) : 1;
});

// 선택 스냅샷의 커버리지 메타 — JSON 문자열 안전 파싱(실패/부재 시 null → 제외 내역 문구 생략 fallback).
const selectedCoverageMeta = computed(() =>
  parseCoverageMeta(selected.value?.coverageMeta)
);

// 상세 헤더 가이드용 제외 내역 요약(status !== FULL 월만) — 빈 문자열이면 표기 생략.
const coverageExcludedText = computed(() =>
  coverageExcludedSummary(selectedCoverageMeta.value)
);

// 릴레이 병합 부분 포함 여부(D-3) — 함께 제공된 연동사 자료 안내 줄.
const relayPartialIncluded = computed(
  () => selectedCoverageMeta.value?.relayPartialIncludedYn === "Y"
);

// 같은 요청 조건(출처·사업장·기간·유형)의 버전 목록 — 버전 간 이동
const versionList = computed(() => {
  const s = selected.value;
  if (!s) return [];
  return snapshotList.value
    .filter(
      (r) =>
        r.srcCmpnyNm === s.srcCmpnyNm &&
        r.siteNm === s.siteNm &&
        r.dataType === s.dataType &&
        r.periodStr === s.periodStr &&
        r.periodEnd === s.periodEnd
    )
    .sort((a, b) => b.version - a.version);
});

// 라벨
const dataTypeLabel = (t) => ({ ATTD: "근태", RISK: "위험성평가", NEARMISS: "아차사고" }[t] || t);
const workerTypeLabel = (t) => ({ REGULAR: "정규", DAILY: "일용" }[t] || t);
const attdStatusLabel = (s) =>
  ({ NORMAL: "정상", LATE: "지각", EARLY_LEAVE: "조퇴", ABSENT: "미출근" }[s] || "-");
const fmtYmd = (v) => (v && v.length === 8 ? `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}` : v || "");
const fmtHm = (v) => (v && v.length === 4 ? `${v.slice(0, 2)}:${v.slice(2, 4)}` : "-");
const periodLabel = (row) => `${fmtYmd(row.periodStr)} ~ ${fmtYmd(row.periodEnd)}`;
const planLabel = (row) =>
  row.planStrTime ? `${fmtHm(row.planStrTime)} ~ ${fmtHm(row.planEndTime)}` : "-";

// =========================== Data ===========================
const { proxy } = getCurrentInstance();

// =========================== Life Cycle ===========================
onMounted(async () => {
  await fnSearch();
});

// =========================== Methods ===========================
// 수신 스냅샷 목록 조회 — GET /webApi/subcon03/snapshot-lists (자사 소유분만, 서버 스코프 강제).
const fnSearch = async () => {
  snapshotList.value = [];
  detailList.value = [];
  selectedId.value = null;

  try {
    const response = await axios.get("/webApi/subcon03/snapshot-lists");

    if (response.status === 200) {
      snapshotList.value = response.data?.snapshots || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 목록 행 선택 → 우측 상세 로드(1페이지부터).
//   ATTD 만 이 화면이 직접 근태 상세를 조회한다. RISK/NEARMISS 는 자식 컴포넌트
//   (SnapshotRiskDetail/SnapshotNearmissDetail)가 자체 조회하므로 ATTD 상세 왕복을 건너뛴다.
const fnSelect = (row) => {
  if (selectedId.value === row.snapshotId) return;

  selectedId.value = row.snapshotId;
  page.value = 1;
  detailList.value = [];
  if (row.dataType === "ATTD") {
    fnLoadDetail();
  }
};

// 버전 변경 → 1페이지부터 다시(ATTD 만 직접 조회 — RISK/NEARMISS 는 자식이 snapshotId 변화를 감지해 재조회).
const fnChangeVersion = () => {
  page.value = 1;
  detailList.value = [];
  if (selected.value?.dataType === "ATTD") {
    fnLoadDetail();
  }
};

// 팝업에서 버전 선택 → 기존 버전전환 흐름 재사용 (ATTD 직접 재조회, RISK/NEARMISS 는 자식이 snapshotId prop 변화 감지).
const fnSelectVersion = (snapshotId) => {
  if (selectedId.value === snapshotId) return;
  selectedId.value = snapshotId;
  fnChangeVersion();
};

// 페이지 이동.
const fnMovePage = (next) => {
  if (next < 1 || next > totalPages.value || isLoading.value) return;

  page.value = next;
  fnLoadDetail();
};

// 상세(읽기전용) 조회 — GET /webApi/subcon03/snapshot-detail?snapshotId=...&page=
//   소유 검증은 서버 SQL 안에서 강제된다(타사 스냅샷ID 는 빈 목록).
const fnLoadDetail = async () => {
  detailList.value = [];

  if (!selectedId.value) return;

  isLoading.value = true;
  try {
    const response = await axios.get("/webApi/subcon03/snapshot-detail", {
      params: { snapshotId: selectedId.value, page: page.value },
    });

    if (response.status === 200) {
      detailList.value = response.data?.rows || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
/* 상세 페이저 — 200행 단위 서버 페이징 */
.detail-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm, 0.5rem);
  padding: var(--space-sm, 0.5rem) 0 0;
}
.detail-pager-label {
  font-size: var(--btn-font-sm, 12px);
  color: var(--color-text-muted, #6b7280);
}

.snapshot-layout {
  display: flex;
  gap: var(--space-md, 0.75rem);
  align-items: flex-start;
}
.snapshot-list {
  flex: 0 0 35%;
  min-width: 0;
}
.snapshot-detail {
  flex: 1 1 auto;
  min-width: 0;
}
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm, 0.5rem);
  padding: 0 0 var(--space-sm, 0.5rem);
}
.detail-meta {
  font-size: var(--btn-font-sm, 12px);
  color: var(--color-text-muted, #6b7280);
}
.detail-ver {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
}
.detail-ver label {
  color: var(--color-text-muted, #6b7280);
  font-size: var(--btn-font-sm, 12px);
}
.detail-ver-current {
  font-size: var(--btn-font-sm, 12px);
}
.detail-note {
  margin: 0 0 var(--space-sm, 0.5rem);
  color: var(--color-text-muted, #6b7280);
  font-size: var(--btn-font-sm, 12px);
}
.is-selected {
  background: var(--color-primary-bg, #dcfce7);
}
.status-badge {
  display: inline-block;
  margin-right: 0.25rem;
  padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
.status-badge.is-warn {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.status-badge.is-closed {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-muted, #4b5563);
}
</style>
