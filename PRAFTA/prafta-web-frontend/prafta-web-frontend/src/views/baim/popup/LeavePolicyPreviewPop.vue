<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide leave-preview-modal"
        :style="positionStyle"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header leave-preview-header" @mousedown="startDrag">
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
                    v-for="grp in headerGroups"
                    :key="grp.key"
                    :colspan="grp.colspan"
                    :class="{ empty: grp.empty }"
                  >
                    {{ grp.label }}
                  </th>
                </tr>
                <tr>
                  <th
                    v-for="col in timeAxis"
                    :key="col.key"
                    :class="{ event: col.isEvent }"
                  >
                    {{ col.subLabel }}
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
import { computed, ref } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

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
  // ★근속가산(AXIS5) — 2026-08-19 추가. 종전엔 본연차를 15 고정으로만 표기해
  //   가산 시작 연차를 CUSTOM(1~2년차)으로 설정한 회사에서 화면과 실제 부여가 어긋났다.
  //   백엔드 tenureBonusDays 와 동일 산식으로 계산한다.
  tenureStartYear: { type: [Number, String], default: 3 },
  tenureInterval: { type: [Number, String], default: 2 },
  tenureMaxDays: { type: [Number, String], default: 25 },
});
defineEmits(["close"]);

// ================ 드래그 (SlotHistoryPop.vue 표준 이식) ================
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 2,
});

const positionStyle = computed(() => {
  const padding = 16;
  const modalWidth = 1100;
  const modalHeight = 600;
  const maxX = window.innerWidth - (modalWidth + padding);
  const maxY = window.innerHeight - (modalHeight + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: y + "px", left: x + "px" };
});

// ================ 상수 ================
const BASE_DAYS = 15; // 본연차 기본 일수
const MONTHLY_MAX = 11; // 1년 미만 법정 월차 최대치

/**
 * 근속연차 → 본연차 총일수(본연차 + 근속가산, 상한 적용).
 * 백엔드 LeaveGrantEngineServiceImpl.tenureBonusDays 와 동일 산식:
 *   bonus = (year >= start) ? floor((year - start) / interval) + 1 : 0
 *   total = min(BASE_DAYS + bonus, maxDays)
 * @param {number} tenureYear 해당 부여 시점의 근속연차(회계연도 기준이면 crossed-1)
 */
const annualDaysAt = (tenureYear) => {
  const start = Number(props.tenureStartYear) || 3;
  const interval = Math.max(1, Number(props.tenureInterval) || 2);
  const maxDays = Number(props.tenureMaxDays) || 25;
  const bonus =
    tenureYear >= start ? Math.floor((tenureYear - start) / interval) + 1 : 0;
  return Math.min(BASE_DAYS + bonus, maxDays);
};

/** 셀 표기 문자열 — 가산이 붙으면 내역을 함께 보여준다(예: "본연차 16 (15+가산1)"). */
const annualText = (tenureYear) => {
  const total = annualDaysAt(tenureYear);
  const bonus = total - BASE_DAYS;
  return bonus > 0
    ? `본연차 ${total} (${BASE_DAYS}+가산${bonus})`
    : `본연차 ${total}`;
};

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

// ================ 단일 시간축 모델 (idx 0~16) ================
// 헤더(그룹)·서브헤더(시점)·셀 배열을 하나의 시간축 정의에서 파생시켜
// 컬럼 수 불일치(과거 headerCols 8 / subHeaderCols 12 / cells 17)를 제거한다.
// 회계연도 시작월/일(props.fiscalStartMm/Dd)·입사월(hireParts.mo)을 모두 반영.

// 0 패딩 헬퍼.
const pad2 = (n) => String(n).padStart(2, "0");

// 입사일 기준 N개월 후의 (연,월,일)을 계산한다(말일 보정 포함).
const addMonthsToHire = (n) => {
  const { y, mo, d } = hireParts.value;
  // mo는 1~12. 0-base 월로 변환 후 가산.
  const base = new Date(y, mo - 1, 1);
  base.setMonth(base.getMonth() + n);
  const ty = base.getFullYear();
  const tm = base.getMonth() + 1; // 1~12
  // 입사일(d)을 적용하되 해당 월 말일을 넘지 않도록 보정.
  const lastDay = new Date(ty, tm, 0).getDate();
  const td = Math.min(d, lastDay);
  return { y: ty, mo: tm, d: td };
};

// 시점 라벨: "YY.MM.DD" (연도 구분 가독). 회계 시점은 연도 전체 노출.
const fmtYmd = (p) => `${String(p.y).slice(2)}.${pad2(p.mo)}.${pad2(p.d)}`;

// 회계연도 시작일(특정 연도) (연,월,일).
const fiscalStartOf = (year) => ({
  y: year,
  mo: Number(props.fiscalStartMm) || 1,
  d: Number(props.fiscalStartDd) || 1,
});

// 단일 시간축: idx 0~16. 각 컬럼이 kind/날짜라벨/그룹귀속을 모두 보유.
//   kind: 'hire'(입사일) | 'monthly'(월차 발생) | 'fiscal'(회계 본연차 시점) | 'anniv'(입사기념=입사기준 본연차)
//   monthOffset: 입사일로부터 경과 개월(월차/기념일 판정용)
const timeAxis = computed(() => {
  const hy = hireParts.value.y;
  const list = [];
  // idx 0 — 입사일 (월차 없음)
  list.push({
    key: "t0",
    kind: "hire",
    monthOffset: 0,
    isEvent: false,
    subLabel: fmtYmd(hireParts.value),
    group: "hire",
  });
  // idx 1~5 — 입사 +1~5개월 월차
  for (let n = 1; n <= 5; n++) {
    list.push({
      key: `t${n}`,
      kind: "monthly",
      monthOffset: n,
      isEvent: false,
      subLabel: fmtYmd(addMonthsToHire(n)),
      group: "y1",
    });
  }
  // idx 6 — 차년 회계연도 시작 (회계 본연차 시점)
  list.push({
    key: "t6",
    kind: "fiscal",
    monthOffset: null,
    isEvent: true,
    subLabel: fmtYmd(fiscalStartOf(hy + 1)),
    group: "fy1",
  });
  // idx 7~12 — 입사 +6~11개월 월차
  for (let n = 6; n <= 11; n++) {
    list.push({
      key: `t${n + 1}`,
      kind: "monthly",
      monthOffset: n,
      isEvent: false,
      subLabel: fmtYmd(addMonthsToHire(n)),
      group: "y2",
    });
  }
  // idx 13 — 입사 1주년 (입사일 기준 본연차)
  list.push({
    key: "t13",
    kind: "anniv",
    monthOffset: 12,
    isEvent: true,
    subLabel: fmtYmd(addMonthsToHire(12)),
    group: "anniv1",
  });
  // idx 14 — 차차년 회계연도 시작 (회계 본연차 반복)
  list.push({
    key: "t14",
    kind: "fiscal",
    monthOffset: null,
    isEvent: true,
    subLabel: fmtYmd(fiscalStartOf(hy + 2)),
    group: "fy2",
  });
  // idx 15 — 입사 2주년 (입사일 기준 본연차 반복)
  list.push({
    key: "t15",
    kind: "anniv",
    monthOffset: 24,
    isEvent: true,
    subLabel: fmtYmd(addMonthsToHire(24)),
    group: "anniv2",
  });
  // idx 16 — 3년차 회계연도 시작 (회계 본연차 반복)
  list.push({
    key: "t16",
    kind: "fiscal",
    monthOffset: null,
    isEvent: true,
    subLabel: fmtYmd(fiscalStartOf(hy + 3)),
    group: "fy3",
  });
  return list;
});

// 상단 그룹 헤더: 시간축의 group 키를 연속 구간으로 묶어 colspan 산출.
// 셀 개수(17)와 합계가 정확히 일치한다.
const headerGroups = computed(() => {
  const hy = hireParts.value.y;
  const labels = {
    hire: { label: "입사일", empty: false },
    y1: { label: `${hy}년 (입사 첫해)`, empty: false },
    fy1: { label: `${hy + 1} 회계시작`, empty: true },
    y2: { label: `${hy + 1}년 (~1년 도래)`, empty: false },
    anniv1: { label: `${hy + 1} 입사1주년`, empty: true },
    fy2: { label: `${hy + 2} 회계시작`, empty: true },
    anniv2: { label: `${hy + 2} 입사2주년`, empty: true },
    fy3: { label: `${hy + 3} 회계시작`, empty: true },
  };
  const groups = [];
  for (const col of timeAxis.value) {
    const last = groups[groups.length - 1];
    if (last && last.key === col.group) {
      last.colspan += 1;
    } else {
      const meta = labels[col.group] || { label: col.group, empty: false };
      groups.push({
        key: col.group,
        label: meta.label,
        empty: meta.empty,
        colspan: 1,
      });
    }
  }
  return groups;
});

// 시간축 셀 구성 헬퍼. timeAxis(17)와 1:1 대응하는 셀 배열을 생성한다.
//   fiscalFirstGrant — idx 6  (차년 회계 본연차)      · 근속연차 0 (첫 회계연도=부분기간)
//   fiscalRecur2     — idx 14 (차차년 회계 본연차)    · 근속연차 1
//   fiscalRecur3     — idx 16 (3년차 회계 본연차)     · 근속연차 2
//   hireGrant        — idx 13 (입사 1주년 본연차)     · 근속연차 1
//   hireRecur        — idx 15 (입사 2주년 본연차)     · 근속연차 2
// ★2026-08-19: 회계 반복분(14·16)을 하나의 문자열로 공유하던 것을 시점별로 분리.
//   근속연차가 다르면 가산도 달라지므로 같은 값을 쓸 수 없다.
const buildCells = (opts) => {
  const { fiscalFirstGrant, fiscalRecur2, fiscalRecur3, hireGrant, hireRecur } =
    opts;
  return timeAxis.value.map((col, idx) => {
    if (col.kind === "hire") {
      // idx 0 — 입사일: 월차 표기 금지(3.2.4.1).
      return { text: col.subLabel, cls: "hire" };
    }
    if (col.kind === "monthly") {
      // 입사 +1~11개월 월차(법정 최대 11일). monthOffset 1~11 모두 발생.
      return col.monthOffset <= MONTHLY_MAX
        ? { text: "월차 1", cls: "monthly" }
        : { text: "", cls: "" };
    }
    if (idx === 6) {
      // 차년 회계 본연차(3.2.4.2).
      return fiscalFirstGrant
        ? { text: fiscalFirstGrant, cls: "event" }
        : { text: "", cls: "" };
    }
    if (idx === 13) {
      // 입사 1주년 본연차(3.2.4.3).
      return hireGrant
        ? { text: hireGrant, cls: "event" }
        : { text: "", cls: "" };
    }
    if (idx === 15) {
      // 입사 2주년 본연차 반복(3.2.4.4).
      return hireRecur
        ? { text: hireRecur, cls: "event" }
        : { text: "", cls: "" };
    }
    if (idx === 14) {
      // 차차년 회계 본연차 반복(3.2.4.4) — 근속연차 1.
      return fiscalRecur2
        ? { text: fiscalRecur2, cls: "event" }
        : { text: "", cls: "" };
    }
    if (idx === 16) {
      // 3년차 회계 본연차 반복(3.2.4.4) — 근속연차 2.
      return fiscalRecur3
        ? { text: fiscalRecur3, cls: "event" }
        : { text: "", cls: "" };
    }
    return { text: "", cls: "" };
  });
};

// 3개 정책의 시간순 부여 행. "회계연도 기준 - 월차만 부여" 행은 정책상 폐기(3.2.3).
const previewRows = computed(() => {
  // ★근속연차는 시점마다 다르다(2026-08-19 반영).
  //   입사일 기준: 1주년=1년차, 2주년=2년차.
  //   회계연도 기준: 근속연차 = 회계연도 시작 도래 횟수 - 1 (첫 회계연도는 부분기간이라 0년차).
  //     → 차년(첫 부여)=0, 차차년=1, 3년차 회계시작=2.
  return [
    {
      name: "입사일 기준 - 월차만 부여",
      cells: buildCells({
        fiscalFirstGrant: "",
        fiscalRecur2: "",
        fiscalRecur3: "",
        hireGrant: annualText(1),
        hireRecur: annualText(2),
      }),
    },
    {
      name: "회계연도 기준 - 비례 부여",
      cells: buildCells({
        // 첫 회계연도는 비례분(근속가산 대상 아님).
        fiscalFirstGrant: `비례 약 ${prorateDays.value}`,
        fiscalRecur2: annualText(1),
        fiscalRecur3: annualText(2),
        hireGrant: "",
        hireRecur: "",
      }),
    },
    {
      name: "회계연도 기준 - 차년도 일괄 부여",
      cells: buildCells({
        // 첫 회계연도 일괄분은 부분기간에 대한 부여라 근속 0년차 → 가산 없음.
        fiscalFirstGrant: annualText(0),
        fiscalRecur2: annualText(1),
        fiscalRecur3: annualText(2),
        hireGrant: "",
        hireRecur: "",
      }),
    },
  ];
});

// 하단 요약 — 1년차(첫해 ~ 입사 1주년 직후) 누적 부여량 비교.
//   - 회계연도 비례: 월차 누적 + 비례 본연차
//   - 입사일 기준: 월차 누적(11) + 입사 1주년 본연차 15
//   - 회계연도 차년 일괄: 월차 누적(11) + 차년 회계 본연차 15
//   ("회계연도 월차만" 정책은 폐기되어 요약에서도 제외 — 3.2.3)
const burdenSummary = computed(() => {
  const monthlyTotal = MONTHLY_MAX; // 1년 경과 시 월차 최대 누적
  const prorate = monthlyTotal + prorateDays.value;
  const hireBase = monthlyTotal + BASE_DAYS;
  const bulk = monthlyTotal + BASE_DAYS;
  return `회계연도 (비례부여) 약 ${prorate}일 < 입사일 기준 ${hireBase}일 = 회계연도 (차년도 일괄) ${bulk}일`;
});

const firstGrantSummary = computed(() => {
  const fiscalP = fiscalStartOf(hireParts.value.y + 1);
  const annivP = addMonthsToHire(12);
  return `회계연도 (비례부여) · (차년도 일괄) — ${fiscalP.y}.${pad2(
    fiscalP.mo
  )}.${pad2(fiscalP.d)} / 입사일 기준 — ${annivP.y}.${pad2(annivP.mo)}.${pad2(
    annivP.d
  )}`;
});
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.leave-preview-modal {
  width: 1100px;
  max-width: 95vw;
}

.leave-preview-header {
  background: rgba(22, 163, 74, 0.08);
  cursor: move;
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

/* 본연차/비례 발생 시점 컬럼(서브헤더) 강조 */
.preview-table th.event {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
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

/* 푸터 [닫기] 버튼: 텍스트 너비만큼만 차지하고 우측 정렬(.modal-footer 가 flex-end) */
.modal-footer .btn-secondary {
  width: fit-content;
  margin-left: auto;
}
</style>
