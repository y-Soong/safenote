<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-wide leave-preview-modal">
        <!-- 헤더 -->
        <div class="modal-header leave-preview-header">
          <span>부여 시점 미리보기</span>
          <button class="icon-button" @click="$emit('close')">
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
        <div class="modal-body leave-preview-body">
          <!-- 가정 안내 -->
          <div class="preview-config">
            <strong>가정</strong>: {{ hireDate }} 입사자, 회계연도
            {{ fiscalLabel }} 시작, 비례부여 반올림 = {{ roundingLabel }}<br />
            각 정책이 시간순으로 어떻게 부여되는지 비교합니다. 색상: 노란색 =
            본연차/비례 발생, 파란색 = 입사일
          </div>

          <!-- 시간순 부여 표 (가로 스크롤) -->
          <div class="preview-table-wrap">
            <table class="preview-table">
              <thead>
                <tr>
                  <th rowspan="2">정책</th>
                  <th
                    v-for="col in headerCols"
                    :key="col.key"
                    :rowspan="col.rowspan || 1"
                    :colspan="col.colspan || 1"
                    :class="{ empty: col.empty }"
                  >
                    {{ col.label }}
                  </th>
                </tr>
                <tr>
                  <th v-for="sub in subHeaderCols" :key="sub.key">
                    {{ sub.label }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in previewRows" :key="row.name">
                  <td class="label">{{ row.name }}</td>
                  <td
                    v-for="(cell, idx) in row.cells"
                    :key="idx"
                    :class="cell.cls"
                  >
                    {{ cell.text }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 하단 요약 (회사 부담 비교 / 첫 본연차 시점) -->
          <p class="preview-help-line">
            <strong>회사 부담 비교 (1년차)</strong>: {{ burdenSummary }}<br />
            <strong>첫 본연차 부여 시점</strong>: {{ firstGrantSummary }}
          </p>
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
import { computed } from "vue";

// ================ Options ================
// (defineOptions 미사용 - 단일 팝업)

// ================ Props & Emits ================
// 호출 화면(Baim_07)에서 현재 axis 설정값을 넘겨받아 클라이언트 계산으로 표를 구성한다.
// ⭐ 백엔드 신규 엔드포인트 호출 금지 — 모든 계산은 이 컴포넌트 내부에서 수행.
const props = defineProps({
  // 1번 axis: HIRE_DATE / FISCAL_YEAR
  axis1GrantBase: { type: String, default: "HIRE_DATE" },
  // 회계연도 시작월 (MM, 01~12) — 백엔드 axis2FiscalStartMm
  fiscalStartMm: { type: String, default: "01" },
  // 회계연도 시작일 (DD, 01~31) — 백엔드 axis2FiscalStartDd
  fiscalStartDd: { type: String, default: "01" },
  // 비례 반올림 — 백엔드 axis4ProrateRounding (CEIL/ROUND/FLOOR/HALF_DAY)
  prorateRounding: { type: String, default: "CEIL" },
  // 시뮬레이션 기준 입사일 (기본 2025-07-15)
  hireDate: { type: String, default: "2025-07-15" },
});
defineEmits(["close"]);

// ================ 상수 ================
const BASE_DAYS = 15; // 본연차 기본 일수
const MONTHLY_MAX = 11; // 1년 미만 법정 월차 최대치

// ================ Computed ================
const fiscalLabel = computed(
  () => `${props.fiscalStartMm}월 ${props.fiscalStartDd}일`
);

const roundingLabel = computed(() => {
  const map = {
    CEIL: "올림(CEIL)",
    ROUND: "반올림(ROUND)",
    FLOOR: "내림(FLOOR)",
    HALF_DAY: "0.5일 절사(HALF_DAY)",
  };
  return map[props.prorateRounding] || props.prorateRounding;
});

// 입사일 파싱 (YYYY-MM-DD). 시안 기본 2025-07-15.
const hireParts = computed(() => {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(props.hireDate || ""));
  if (!m) return { y: 2025, mo: 7, d: 15 };
  return { y: Number(m[1]), mo: Number(m[2]), d: Number(m[3]) };
});

// 입사 첫해 잔여 개월 수(입사월 다음달 ~ 12월). 시안: 2025-07 → 8~12월 = 5개월.
const firstYearMonthlyCount = computed(() => {
  const remain = 12 - hireParts.value.mo;
  return Math.min(Math.max(remain, 0), MONTHLY_MAX);
});

// 회계연도 비례 본연차 = 잔여기간(개월)/12 × 15, prorateRounding 적용.
// 시안: 입사월 다음달부터 차년도 회계연도 시작(1/1) 직전까지 = 5개월 → 5/12*15 ≈ 6.25 → CEIL 7.
const prorateDays = computed(() => {
  const ratio = (firstYearMonthlyCount.value / 12) * BASE_DAYS;
  switch (props.prorateRounding) {
    case "CEIL":
      return Math.ceil(ratio);
    case "FLOOR":
      return Math.floor(ratio);
    case "HALF_DAY":
      return Math.floor(ratio * 2) / 2; // 0.5일 단위 절사
    case "ROUND":
    default:
      return Math.round(ratio);
  }
});

// 표 헤더(상단/하단 2단). 시안 §4.8.2 기준 시간축(회계연도 1/1 가정) 정적 정의.
// 회계연도 시작월/일이 1/1이 아닐 경우 시간축 동적 재계산은 향후 보강 대상
// (시안 자체가 회계연도 1/1 고정 기준으로 정의됨).
const headerCols = computed(() => {
  const hy = hireParts.value.y;
  return [
    { key: "hire", label: "입사일", rowspan: 2 },
    { key: "y1", label: `${hy}년 (입사 첫해)`, colspan: 6 },
    { key: "fy1", label: `${hy + 1}.01.01`, rowspan: 2, empty: true },
    { key: "y2", label: `${hy + 1}년 (~1년 도래)`, colspan: 6 },
    { key: "anniv1", label: `${hy + 1}.07.15`, rowspan: 2, empty: true },
    { key: "fy2", label: `${hy + 2}.01.01`, rowspan: 2, empty: true },
    { key: "anniv2", label: `${hy + 2}.07.15`, rowspan: 2, empty: true },
    { key: "fy3", label: `${hy + 3}.01.01`, rowspan: 2, empty: true },
  ];
});

const subHeaderCols = computed(() => {
  const hy = hireParts.value.y;
  return [
    { key: "s0", label: `${String(hy).slice(2)}.07.15` },
    { key: "s1", label: "08.15" },
    { key: "s2", label: "09.15" },
    { key: "s3", label: "10.15" },
    { key: "s4", label: "11.15" },
    { key: "s5", label: "12.15" },
    { key: "s6", label: "01.15" },
    { key: "s7", label: "02.15" },
    { key: "s8", label: "03.15" },
    { key: "s9", label: "04.15" },
    { key: "s10", label: "05.15" },
    { key: "s11", label: "06.15" },
  ];
});

// 시간축 셀 구성 헬퍼. 컬럼 순서:
//   [0]입사일, [1~5]첫해 8~12월(월차), [6]차년 1/1 회계, [7~12]차년 1~6월(월차),
//   [13]차년 7/15(입사기념), [14]차차년 1/1, [15]차차년 7/15, [16]3년차 1/1
const buildCells = (opts) => {
  const { fiscalFirstGrant, fiscalRecur, hireGrant, hireRecur } = opts;
  const hireYmd = `${hireParts.value.y}.07.15`;
  const cells = [];
  // [0] 입사일
  cells.push({ text: hireYmd, cls: "hire" });
  // [1~5] 첫해 5개월 월차 (8~12월)
  for (let i = 0; i < 5; i++) {
    cells.push(
      i < firstYearMonthlyCount.value
        ? { text: "월차 1", cls: "monthly" }
        : { text: "", cls: "" }
    );
  }
  // [6] 차년 회계연도 시작(1/1) — 비례/차년일괄 본연차 발생 시점
  cells.push(
    fiscalFirstGrant
      ? { text: fiscalFirstGrant, cls: "event" }
      : { text: "", cls: "" }
  );
  // [7~12] 차년 1~6월 월차 (잔여분, 최대 11일까지)
  const remainMonthly = MONTHLY_MAX - firstYearMonthlyCount.value;
  for (let i = 0; i < 6; i++) {
    cells.push(
      i < remainMonthly
        ? { text: "월차 1", cls: "monthly" }
        : { text: "", cls: "" }
    );
  }
  // [13] 차년 입사기념일(7/15) — 입사일 기준 본연차
  cells.push(
    hireGrant ? { text: hireGrant, cls: "event" } : { text: "", cls: "" }
  );
  // [14] 차차년 회계연도 시작(1/1) — 회계연도 기준 본연차 반복
  cells.push(
    fiscalRecur ? { text: fiscalRecur, cls: "event" } : { text: "", cls: "" }
  );
  // [15] 차차년 입사기념일(7/15) — 입사일 기준 본연차 반복
  cells.push(
    hireRecur ? { text: hireRecur, cls: "event" } : { text: "", cls: "" }
  );
  // [16] 3년차 회계연도 시작(1/1)
  cells.push(
    fiscalRecur ? { text: fiscalRecur, cls: "event" } : { text: "", cls: "" }
  );
  return cells;
};

// 4가지 정책의 시간순 부여 행 (시안 §4.8.2 표 이식).
const previewRows = computed(() => {
  const annual = `본연차 ${BASE_DAYS}`;
  return [
    {
      name: "입사일 기준 - 월차만 부여",
      cells: buildCells({
        fiscalFirstGrant: "",
        fiscalRecur: "",
        hireGrant: annual,
        hireRecur: annual,
      }),
    },
    {
      name: "회계연도 기준 - 월차만 부여",
      cells: buildCells({
        fiscalFirstGrant: "",
        fiscalRecur: annual,
        hireGrant: "",
        hireRecur: "",
      }),
    },
    {
      name: "회계연도 기준 - 비례 부여",
      cells: buildCells({
        fiscalFirstGrant: `비례 약 ${prorateDays.value}`,
        fiscalRecur: annual,
        hireGrant: "",
        hireRecur: "",
      }),
    },
    {
      name: "회계연도 기준 - 차년도 일괄 부여",
      cells: buildCells({
        fiscalFirstGrant: annual,
        fiscalRecur: annual,
        hireGrant: "",
        hireRecur: "",
      }),
    },
  ];
});

// 하단 요약 — 1년차(첫해 ~ 입사 1주년 직후) 누적 부여량 비교.
//   - 회계연도 월차만: 월차 누적(최대 11)만
//   - 회계연도 비례: 월차 누적 + 비례 본연차
//   - 입사일 기준: 월차 누적(11) + 입사 1주년 본연차 15
//   - 회계연도 차년 일괄: 월차 누적(11) + 차년 회계 본연차 15
const burdenSummary = computed(() => {
  const monthlyTotal = MONTHLY_MAX; // 1년 경과 시 월차 최대 누적
  const monthlyOnly = monthlyTotal;
  const prorate = monthlyTotal + prorateDays.value;
  const hireBase = monthlyTotal + BASE_DAYS;
  const bulk = monthlyTotal + BASE_DAYS;
  return `회계연도 (월차누적) ${monthlyOnly}일 < 회계연도 (비례부여) 약 ${prorate}일 < 입사일 기준 ${hireBase}일 = 회계연도 (차년도 일괄) ${bulk}일`;
});

const firstGrantSummary = computed(() => {
  const fy = hireParts.value.y + 1;
  return `회계연도 (비례부여) · (차년도 일괄) — ${fy}.01.01 / 입사일 기준 — ${fy}.07.15 / 회계연도 (월차누적) — ${hireParts.value.y + 2}.01.01`;
});
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.leave-preview-modal {
  width: 100%;
  max-width: 1100px;
}

.leave-preview-header {
  background: rgba(22, 163, 74, 0.08);
}

.leave-preview-body {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

/* 가정 안내 박스 */
.preview-config {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0.75rem 0.875rem;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.preview-config strong {
  color: var(--color-text-strong);
  font-weight: 600;
}

/* 가로 스크롤 표 래퍼 */
.preview-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.6875rem;
  min-width: 900px;
}

.preview-table th,
.preview-table td {
  border: 1px solid var(--color-border);
  padding: 0.375rem 0.5rem;
  text-align: center;
  white-space: nowrap;
}

.preview-table th {
  background: rgba(22, 163, 74, 0.08);
  font-weight: 600;
  color: var(--color-primary-pressed);
}

.preview-table th.empty {
  background: var(--color-bg);
  color: var(--color-text-muted);
}

/* 정책명 셀 */
.preview-table td.label {
  text-align: left;
  font-weight: 500;
  background: var(--color-bg);
  padding-left: 0.75rem;
  color: var(--color-text-strong);
}

/* 입사일 셀 (파란 강조 대체 — 토큰 기반 surface + primary 텍스트) */
.preview-table td.hire {
  background: rgba(22, 163, 74, 0.04);
  color: var(--color-primary-pressed);
  font-weight: 500;
}

/* 본연차/비례 발생 셀 (노란 강조) */
.preview-table td.event {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  font-weight: 600;
}

.preview-table td.monthly {
  color: var(--color-text-muted);
}

.preview-help-line {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  line-height: 1.7;
  margin: 0;
}

.preview-help-line strong {
  color: var(--color-text-strong);
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
</style>
