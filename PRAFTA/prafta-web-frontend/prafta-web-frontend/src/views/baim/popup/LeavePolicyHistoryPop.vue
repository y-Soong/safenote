<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide leave-history-modal"
        :style="positionStyle"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header leave-history-header" @mousedown="startDrag">
          <span>연차 부여 정책 변경 이력</span>
          <button
            class="icon-button lh-close-btn"
            @click="$emit('close')"
            aria-label="닫기"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- 바디 -->
        <div class="modal-body leave-history-body">
          <!-- 로딩 -->
          <p v-if="isLoading" class="lh-state">조회 중입니다...</p>

          <!-- 비어 있음 -->
          <p v-else-if="historyList.length === 0" class="lh-state">
            변경 이력이 없습니다.
          </p>

          <!-- 이력 목록 -->
          <div v-else class="lh-table-wrap">
            <table class="lh-table">
              <thead>
                <tr>
                  <th class="lh-th-toggle"></th>
                  <th>변경 유형</th>
                  <th>변경 사유</th>
                  <th>영향 인원</th>
                  <th>변경자</th>
                  <th>변경 일시</th>
                </tr>
              </thead>
              <tbody>
                <template v-for="row in historyList" :key="row.histId">
                  <!-- 요약 행 (클릭 시 전후 비교 펼침) -->
                  <tr
                    class="lh-row"
                    :class="{ 'is-open': isExpanded(row.histId) }"
                    @click="fnToggleRow(row.histId)"
                  >
                    <td class="lh-td-toggle">
                      <span
                        class="lh-caret"
                        :class="{ 'is-open': isExpanded(row.histId) }"
                        aria-hidden="true"
                        >▶</span
                      >
                    </td>
                    <td>{{ changeTypeLabel(row.changeType) }}</td>
                    <td class="lh-reason">{{ row.changeReason || "-" }}</td>
                    <td>{{ affectedUserCount(row.impactSummary) }}</td>
                    <td>{{ changerLabel(row) }}</td>
                    <td>{{ row.insertDate || "-" }}</td>
                  </tr>

                  <!-- 전후 비교 펼침 행 -->
                  <tr
                    v-if="isExpanded(row.histId)"
                    class="lh-diff-row"
                    :key="row.histId + '-diff'"
                  >
                    <td :colspan="6" class="lh-diff-cell">
                      <div class="lh-diff-wrap">
                        <!-- 최초 등록(prevSnapshot=null) -->
                        <p v-if="isInitialCreate(row)" class="lh-diff-initial">
                          최초 등록 — 이전 정책이 없습니다.
                        </p>

                        <!-- 변경된 axis 비교 -->
                        <template v-else>
                          <div
                            v-for="d in buildDiff(row)"
                            :key="d.key"
                            class="lh-diff-item"
                            :class="{ 'is-unchanged': !d.changed }"
                          >
                            <span class="lh-diff-axis">{{ d.label }}</span>
                            <span class="lh-diff-values">
                              <span class="lh-diff-from">{{ d.from }}</span>
                              <span class="lh-diff-arrow" aria-hidden="true"
                                >→</span
                              >
                              <span class="lh-diff-to">{{ d.to }}</span>
                            </span>
                          </div>
                          <p
                            v-if="buildDiff(row).length === 0"
                            class="lh-diff-initial"
                          >
                            변경된 항목이 없습니다.
                          </p>
                        </template>
                      </div>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 푸터 -->
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="$emit('close')">
            닫기
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ================ Imports ================
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// ================ Props & Emits ================
defineEmits(["close"]);

// ================ Instance ================
const { proxy } = getCurrentInstance();

// ================ Drag (SlotHistoryPop.vue 패턴 이식) ================
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 2,
});
const positionStyle = computed(() => {
  const padding = 16;
  const modalWidth = 900;
  const modalHeight = 560;
  const maxX = window.innerWidth - (modalWidth + padding);
  const maxY = window.innerHeight - (modalHeight + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: y + "px", left: x + "px" };
});

// ================ Refs ================
const historyList = ref([]);
const isLoading = ref(false);
// 전후 비교 펼침 상태 (histId Set)
const expandedSet = ref(new Set());

// ================ axis 코드→한글 라벨 매핑 (FE 상수) ================
// LeavePolicyImpactPop.vue fnBuildPolicySummary의 axis1Map/axis3Map과 동일 사전.
// snapshot 키는 LeavePolicyServiceImpl.serializePolicyForSnapshot 기준(camelCase).
const AXIS1_MAP = { HIRE_DATE: "입사일 기준", FISCAL_YEAR: "회계연도 기준" };
const AXIS3_MAP = {
  MONTHLY_ONLY: "월차만 부여",
  PRORATE: "비례 부여",
  NEXT_YEAR_BULK: "차년도 일괄 부여",
};
const AXIS4_MAP = {
  CEIL: "올림",
  ROUND: "반올림",
  FLOOR: "버림",
  HALF_DAY: "0.5일 절사",
};
const AXIS5_MAP = { LEGAL: "법정 기준", CUSTOM: "사용자 정의" };
const YN_MAP = { Y: "사용", N: "미사용" };
const USAGE_UNIT_MAP = {
  FULL_DAY: "1일 단위",
  HALF_DAY: "0.5일 단위",
};

// 비교 대상 axis 정의 (표시 라벨 + 값 포맷터)
const DIFF_FIELDS = [
  {
    key: "axis1GrantBase",
    label: "부여 기준",
    fmt: (v) => mapLabel(AXIS1_MAP, v),
  },
  {
    key: "axis2FiscalStartMm",
    label: "회계연도 시작월",
    fmt: (v) => plainLabel(v),
  },
  {
    key: "axis2FiscalStartDd",
    label: "회계연도 시작일",
    fmt: (v) => plainLabel(v),
  },
  {
    key: "axis3FirstYearMethod",
    label: "첫해 부여 방식",
    fmt: (v) => mapLabel(AXIS3_MAP, v),
  },
  {
    key: "axis4ProrateRounding",
    label: "비례 부여 반올림",
    fmt: (v) => mapLabel(AXIS4_MAP, v),
  },
  {
    key: "axis5TenureMode",
    label: "근속 가산 방식",
    fmt: (v) => mapLabel(AXIS5_MAP, v),
  },
  { key: "axis5StartYear", label: "가산 시작 연차", fmt: (v) => plainLabel(v) },
  { key: "axis5Interval", label: "가산 주기(년)", fmt: (v) => plainLabel(v) },
  { key: "axis5MaxDays", label: "최대 부여 일수", fmt: (v) => plainLabel(v) },
  {
    key: "axis6ValidityMonths",
    label: "유효 기간(개월)",
    fmt: (v) => plainLabel(v),
  },
  {
    key: "axis7UsePromotion",
    label: "사용 촉진",
    fmt: (v) => mapLabel(YN_MAP, v),
  },
  { key: "aprvUseYn", label: "결재 사용", fmt: (v) => mapLabel(YN_MAP, v) },
  {
    key: "usageUnit",
    label: "사용 단위",
    fmt: (v) => mapLabel(USAGE_UNIT_MAP, v),
  },
];

const mapLabel = (map, v) => {
  if (v == null || v === "") return "-";
  return map[v] || String(v);
};
const plainLabel = (v) => {
  if (v == null || v === "") return "-";
  return String(v);
};

// ================ Life Cycle ================
onMounted(() => {
  fnLoadHistory();
});

// ================ API ================
const fnLoadHistory = async () => {
  isLoading.value = true;
  try {
    const response = await axios.get("/webApi/baim07/policy/history", {
      params: { page: 1, size: 50 },
    });
    // 응답: { history: { page, size, totalCount, items } }
    historyList.value = response.data?.history?.items ?? [];
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "이력 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};

// ================ Methods ================
const changeTypeLabel = (type) => {
  const map = {
    CREATE: "신규 등록",
    UPDATE: "변경",
    PRESET_CHANGE: "프리셋 변경",
  };
  return map[type] || type || "-";
};

// 변경자 표기: insertUserId(insertUserNm). 둘 중 누락 시 폴백.
//   1) 둘 다 있음 → "user01(홍길동)"
//   2) ID만 → "user01"
//   3) 이름만 → "홍길동"
//   4) 둘 다 없음 → insertNo 원문
const changerLabel = (row) => {
  const id = row.insertUserId;
  const nm = row.insertUserNm;
  if (id && nm) return `${id}(${nm})`;
  if (id) return id;
  if (nm) return nm;
  return row.insertNo || "-";
};

// IMPACT_SUMMARY(JSON 문자열)에서 영향 인원 추출
const affectedUserCount = (impactSummary) => {
  if (!impactSummary) return "-";
  try {
    const parsed = JSON.parse(impactSummary);
    const cnt = parsed?.affectedUserCount;
    return cnt == null ? "-" : `${cnt}명`;
  } catch (e) {
    return "-";
  }
};

// ===== 전후 비교(펼침) =====
const isExpanded = (histId) => expandedSet.value.has(histId);

const fnToggleRow = (histId) => {
  const next = new Set(expandedSet.value);
  if (next.has(histId)) {
    next.delete(histId);
  } else {
    next.add(histId);
  }
  expandedSet.value = next;
};

// prevSnapshot=null(최초 CREATE) graceful 표기
const isInitialCreate = (row) => {
  return !row.prevSnapshot;
};

const parseSnapshot = (json) => {
  if (!json) return null;
  try {
    return typeof json === "string" ? JSON.parse(json) : json;
  } catch (e) {
    return null;
  }
};

// axis별 이전값 → 변경값 비교 목록. 변경된 axis만 반환.
const buildDiff = (row) => {
  const prev = parseSnapshot(row.prevSnapshot);
  const next = parseSnapshot(row.newSnapshot);
  if (!next) return [];
  const result = [];
  for (const f of DIFF_FIELDS) {
    const pv = prev ? prev[f.key] : null;
    const nv = next[f.key];
    const changed = String(pv ?? "") !== String(nv ?? "");
    // 변경 없는 axis는 제외 (흐리게 대신 미표시로 간결화)
    if (!changed) continue;
    result.push({
      key: f.key,
      label: f.label,
      from: f.fmt(pv),
      to: f.fmt(nv),
      changed: true,
    });
  }
  return result;
};
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.leave-history-modal {
  width: 100%;
  max-width: 900px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}

.leave-history-header {
  background: rgba(22, 163, 74, 0.08);
  cursor: move;
}

/* (3.1.1) 닫기 버튼: 텍스트 크기에 맞춰 축소 + 우측 정렬 */
.lh-close-btn {
  margin-left: auto;
  width: 1.5rem;
  height: 1.5rem;
  padding: 0.125rem;
}

.lh-close-btn svg {
  width: 1.25rem;
  height: 1.25rem;
}

.leave-history-body {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.lh-state {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  text-align: center;
  padding: 1.5rem 0;
  margin: 0;
}

.lh-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

.lh-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.75rem;
  min-width: 720px;
}

.lh-table th,
.lh-table td {
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.625rem;
  text-align: center;
  white-space: nowrap;
}

.lh-table th {
  background: rgba(22, 163, 74, 0.08);
  font-weight: 600;
  color: var(--color-primary-pressed);
}

.lh-th-toggle,
.lh-td-toggle {
  width: 2rem;
}

.lh-table td.lh-reason {
  text-align: left;
  white-space: normal;
  color: var(--color-text);
}

/* 요약 행 (클릭 가능) */
.lh-row {
  cursor: pointer;
}

.lh-row:hover {
  background: var(--color-bg);
}

.lh-row.is-open {
  background: rgba(22, 163, 74, 0.04);
}

.lh-caret {
  display: inline-block;
  font-size: 0.625rem;
  color: var(--color-text-muted);
  transition: transform 0.15s ease;
}

.lh-caret.is-open {
  transform: rotate(90deg);
  color: var(--color-primary);
}

/* 전후 비교 펼침 영역 */
.lh-diff-cell {
  background: var(--color-bg);
  text-align: left;
  padding: 0.75rem 1rem;
}

.lh-diff-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.lh-diff-initial {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin: 0;
}

.lh-diff-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.lh-diff-item.is-unchanged {
  opacity: 0.5;
}

.lh-diff-axis {
  flex: 0 0 9rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.lh-diff-values {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
}

.lh-diff-from {
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.lh-diff-arrow {
  color: var(--color-primary);
}

.lh-diff-to {
  color: var(--color-primary);
  font-weight: 600;
}

/* 푸터 버튼 */
.btn {
  padding: 0.5rem 1rem;
  border-radius: var(--btn-radius);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.btn-secondary {
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  color: var(--color-text);
}

.btn-secondary:hover {
  background: var(--color-bg);
}

/* 푸터 [닫기] 버튼: 텍스트 너비만큼만 차지하고 우측 정렬(.modal-footer 가 flex-end) */
.modal-footer .btn-secondary {
  width: fit-content;
  margin-left: auto;
}
</style>
