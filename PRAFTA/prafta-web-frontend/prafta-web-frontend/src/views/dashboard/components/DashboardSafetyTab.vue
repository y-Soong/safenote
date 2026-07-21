<template>
  <!-- 안전관리 탭 위젯 레이아웃 (PRAFTA-DASHBOARD-T1 골격 / T5: S2·S3·S4 구현)
       배치:
       ┌────────────────────────────────┐
       │ S1 무사고 배너 (풀폭)            │
       ├──────────┬──────────┬─────────┤
       │ S2 순회   │ S3 위험성 │ S5 사고  │
       ├──────────┴──────────┴─────────┤
       │ S4 TBM 추이 (풀폭)              │
       └────────────────────────────────┘
       S1(무사고 배너)·S5(사고관리)는 T4 구현 (GET /webApi/dashboard01/safety-acct).
       S2/S3/S4 데이터 조회 규약: PRAFTA-DASHBOARD-T5.plan.md §2 (조회 티켓) -->
  <div class="dash-safety-grid">
    <!-- S1. 무사고 배너: 존재감 있게(풀폭 primary). 오늘 기준 고정 — 조회월 무관 (T4.plan §1-2) -->
    <div class="dash-banner area-banner">
      <span class="dash-banner__site">{{ siteNm || "사업장" }}</span>
      <span class="dash-banner__text">
        무사고 <b class="dash-banner__days">{{ bannerDays }}</b> 일
      </span>
      <span class="dash-banner__since">{{ bannerSince }}</span>
    </div>

    <!-- S2. 순회점검: 당일 x/y 분수 + 진행바, 하단 월 미이행 danger → ChkLst_03 -->
    <DashboardWidgetCard
      class="area-patrol"
      title="순회점검"
      move-target="ChkLst_03"
      @move="onMove"
    >
      <div v-if="patrolStatus === 'idle'" class="dash-widget-state">
        조회 시 표시됩니다
      </div>
      <div v-else-if="patrolStatus === 'loading'" class="dash-widget-state">
        불러오는 중…
      </div>
      <div
        v-else-if="patrolStatus === 'error'"
        class="dash-widget-state dash-widget-state--error"
      >
        조회에 실패했습니다. 다시 조회해 주세요.
      </div>
      <div v-else class="dash-patrol">
        <!-- 당일 카드는 조회월과 무관하게 항상 오늘 기준 (사용자 확정 2026-07-07) -->
        <span class="dash-patrol__label">
          당일 점검 <em class="dash-patrol__hint">(오늘 기준)</em>
        </span>
        <div class="dash-patrol__fraction">
          <b class="dash-patrol__done">{{ patrol.todayInspectCnt }}</b>
          <span class="dash-patrol__slash">/</span>
          <span class="dash-patrol__total">{{ patrol.todayTotalCnt }}</span>
          <span class="dash-patrol__unit">개소</span>
          <span class="dash-patrol__rate">{{ patrolRate }}%</span>
        </div>
        <!-- 진행바는 사용자 요청으로 제거(2026-07-08) — 분수 + 미이행만 표시 -->
        <div
          :class="[
            'dash-patrol__miss',
            { 'dash-patrol__miss--danger': patrol.monthMissCnt > 0 },
          ]"
        >
          <template v-if="patrol.monthMissCnt > 0">
            이번달 미이행 <b>{{ patrol.monthMissCnt }}</b> 회
          </template>
          <template v-else>이번달 미이행 없음</template>
        </div>
      </div>
    </DashboardWidgetCard>

    <!-- S3. 위험성 평가: 검토요청/개선예정 카운트 2매 + 아차사고 배지 → Risk_03 -->
    <DashboardWidgetCard
      class="area-risk"
      title="위험성 평가"
      move-target="Risk_03"
      @move="onMove"
    >
      <div v-if="riskStatus === 'idle'" class="dash-widget-state">
        조회 시 표시됩니다
      </div>
      <div v-else-if="riskStatus === 'loading'" class="dash-widget-state">
        불러오는 중…
      </div>
      <div
        v-else-if="riskStatus === 'error'"
        class="dash-widget-state dash-widget-state--error"
      >
        조회에 실패했습니다. 다시 조회해 주세요.
      </div>
      <div v-else class="dash-risk">
        <!-- 상태 카운트는 조회월 무관(사업장+상태만) — Risk_03 목록 건수와 일치 축 -->
        <div class="dash-risk__counts">
          <div class="dash-count-card">
            <span class="dash-count-card__label">검토요청</span>
            <b class="dash-count-card__num">{{ risk.reviewRequestCnt }}</b>
          </div>
          <div class="dash-count-card">
            <span class="dash-count-card__label">개선예정</span>
            <b class="dash-count-card__num">{{ risk.improvePlanCnt }}</b>
          </div>
        </div>
        <!-- 아차사고는 발생일시 기준 조회월 필터 (NearMiss_01 기간필터와 동일 축) -->
        <div class="dash-risk__nearmiss">
          <span class="dash-badge">아차사고</span>
          조회월 등록
          <b class="dash-risk__nearmiss-cnt">{{ risk.nearMissCnt }}</b> 건
        </div>
      </div>
    </DashboardWidgetCard>

    <!-- S5. 사고관리: 조회월 건수 + 등급 칩 + 최근 사고(전체 기간) → Acct_01 (T4) -->
    <DashboardWidgetCard
      class="area-acct"
      title="사고관리"
      move-target="Acct_01"
      @move="onMove"
    >
      <div v-if="acctStatus === 'idle'" class="dash-widget-state">
        조회 시 표시됩니다
      </div>
      <div v-else-if="acctStatus === 'loading'" class="dash-widget-state">
        불러오는 중…
      </div>
      <div
        v-else-if="acctStatus === 'error'"
        class="dash-widget-state dash-widget-state--error"
      >
        조회에 실패했습니다. 다시 조회해 주세요.
      </div>
      <div v-else class="dash-acct">
        <!-- 조회월 표기는 마지막 소비된 티켓의 ym -->
        <div class="dash-acct__month">
          조회월 {{ acctYm }} 사고
          <b class="dash-acct__total">{{ acct.monthTotalCnt }}</b> 건
        </div>
        <!-- 등급 칩 3개 항상 표시 (0건 포함) — 색 매핑은 Acct_01 gradeClass 관례 이식 -->
        <div class="dash-acct__grades">
          <span
            :class="[
              'dash-grade-chip',
              'critical',
              { muted: acct.grade100Cnt === 0 },
            ]"
          >
            중대재해 {{ acct.grade100Cnt }}
          </span>
          <span
            :class="[
              'dash-grade-chip',
              'normal',
              { muted: acct.grade200Cnt === 0 },
            ]"
          >
            일반산재 {{ acct.grade200Cnt }}
          </span>
          <span
            :class="[
              'dash-grade-chip',
              'exempt',
              { muted: acct.grade300Cnt === 0 },
            ]"
          >
            신고제외 {{ acct.grade300Cnt }}
          </span>
        </div>
        <!-- 최근 사고는 전체 기간 기준 (조회월 카운트와 기준 다름 — 캡션으로 명시) -->
        <div class="dash-acct__recent">
          <div class="dash-acct__caption">최근 사고 (전체 기간)</div>
          <div v-if="recentAcctTop.length === 0" class="dash-acct__empty">
            등록된 사고가 없습니다.
          </div>
          <div
            v-for="a in recentAcctTop"
            :key="a.acctId"
            class="dash-acct__row"
            @click="onMove('Acct_01')"
          >
            <span class="dash-acct__row-when">
              {{ formatYmdDot(a.occurYmd) }} {{ formatHm(a.occurTime) }}
            </span>
            <span class="dash-acct__row-sep">·</span>
            <span>{{ a.acctGradeNm || a.acctGradeCd }}</span>
            <span class="dash-acct__row-sep">·</span>
            <span>{{ a.processStatusNm || a.processStatusCd }}</span>
            <span class="dash-acct__row-sep">·</span>
            <span class="dash-acct__row-place">{{ a.occurPlace || "-" }}</span>
          </div>
        </div>
      </div>
    </DashboardWidgetCard>

    <!-- S4. TBM: 당월 완료 세션 건수 (사용자 요청 2026-07-08: 12개월 추이 → 당월 건수만) → Tbm_04 -->
    <DashboardWidgetCard
      class="area-tbm"
      title="TBM (당월 건수)"
      move-target="Tbm_04"
      @move="onMove"
    >
      <div v-if="tbmStatus === 'idle'" class="dash-widget-state">
        조회 시 표시됩니다
      </div>
      <div v-else-if="tbmStatus === 'loading'" class="dash-widget-state">
        불러오는 중…
      </div>
      <div
        v-else-if="tbmStatus === 'error'"
        class="dash-widget-state dash-widget-state--error"
      >
        조회에 실패했습니다. 다시 조회해 주세요.
      </div>
      <div v-else class="dash-tbm">
        <div class="dash-tbm__stat">
          <b class="dash-tbm__count">{{ tbmCurrentCnt }}</b>
          <span class="dash-tbm__unit">건</span>
        </div>
        <span class="dash-tbm__caption">조회월 완료 TBM</span>
      </div>
    </DashboardWidgetCard>
  </div>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import DashboardWidgetCard from "./DashboardWidgetCard.vue";
import axios from "@/api/axios";
import { formatYmdDot, formatHm } from "@/utils/dateFormat";

defineOptions({ name: "DashboardSafetyTab" });

const props = defineProps({
  // 무사고 배너에 표시할 사업장명 (부모의 상단 조회 사업장과 동기화)
  siteNm: { type: String, default: "" },
  // 조회 티켓 — 부모 fnSearch가 발행. { siteCd, siteNo, siteNm, nodeCd, nodeNm, incSubNodeYn, ym, ts }
  // 안전 위젯은 siteCd / ym 만 사용한다 (PRAFTA-DASHBOARD-T5.plan.md §2)
  ticket: { type: Object, default: null },
});

const emit = defineEmits(["move"]);

// ── 위젯별 조회 상태 (idle: 최초 미조회 / loading / error / done) ──
// 3위젯은 독립 API·독립 상태 — 한 위젯 실패가 다른 위젯 표시를 막지 않는다.

// S2 순회점검 — GET /webApi/dashboard01/safety-patrol { siteCd, ym }
const patrolStatus = ref("idle");
const patrol = ref({ todayInspectCnt: 0, todayTotalCnt: 0, monthMissCnt: 0 });

// S3 위험성평가 — GET /webApi/dashboard01/safety-risk { siteCd, ym }
const riskStatus = ref("idle");
const risk = ref({ reviewRequestCnt: 0, improvePlanCnt: 0, nearMissCnt: 0 });

// S4 TBM 추이 — GET /webApi/dashboard01/safety-tbm-trend { siteCd, ym }
// 응답은 항상 12포인트(빈 월 cnt=0, BE에서 채움), 과거→조회월 오름차순 [{ ym: 'YYYY-MM', cnt }]
const tbmStatus = ref("idle");
const tbmTrend = ref([]);

// S1 무사고 배너 + S5 사고 summary — GET /webApi/dashboard01/safety-acct { siteCd, ym } (T4)
const acctStatus = ref("idle");
const acct = ref({
  noAcctDays: null,
  baselineYmd: null,
  baselineType: null,
  monthTotalCnt: 0,
  grade100Cnt: 0,
  grade200Cnt: 0,
  grade300Cnt: 0,
  recentAcctList: [],
});
// S5 "조회월" 표기용 — 마지막으로 소비(성공)한 티켓의 ym
const acctYm = ref("");

// ── 조회 티켓 소비 (T5.plan §2 규약 — T2~T4 공용) ─────────────
// 대시보드 탭이 제거되어 이 위젯은 항상 표시 상태다 → 티켓이 오면 즉시 소비한다.
//   (탭 시절의 active/pendingTicket 지연 소비 로직은 도달 불가라 제거)
watch(
  () => props.ticket,
  (ticket) => {
    if (!ticket) return;
    fetchAll(ticket);
  }
);

// 안전 탭 위젯 일괄 조회 — 위젯별 API는 개별 Promise 로 독립 호출
// (한 위젯 실패가 다른 위젯 표시를 막지 않음)
const fetchAll = (ticket) => {
  fetchAcct(ticket); // S1 + S5 (T4)
  fetchPatrol(ticket); // S2 (T5)
  fetchRisk(ticket); // S3 (T5)
  fetchTbmTrend(ticket); // S4 (T5)
};

// S2 순회점검 — 당일 x/y + 조회월 미이행 수. 실패는 카드 로컬 error 표기만 ($alert 금지)
const fetchPatrol = async (ticket) => {
  patrolStatus.value = "loading";
  try {
    const response = await axios.get("/webApi/dashboard01/safety-patrol", {
      params: { siteCd: ticket.siteCd, ym: ticket.ym },
    });
    const d = response.data ?? {};
    patrol.value = {
      todayInspectCnt: d.todayInspectCnt ?? 0,
      todayTotalCnt: d.todayTotalCnt ?? 0,
      monthMissCnt: d.monthMissCnt ?? 0,
    };
    patrolStatus.value = "done";
  } catch (err) {
    patrolStatus.value = "error";
  }
};

// S3 위험성평가 — 검토요청/개선예정 카운트 + 조회월 아차사고 건수
const fetchRisk = async (ticket) => {
  riskStatus.value = "loading";
  try {
    const response = await axios.get("/webApi/dashboard01/safety-risk", {
      params: { siteCd: ticket.siteCd, ym: ticket.ym },
    });
    const d = response.data ?? {};
    risk.value = {
      reviewRequestCnt: d.reviewRequestCnt ?? 0,
      improvePlanCnt: d.improvePlanCnt ?? 0,
      nearMissCnt: d.nearMissCnt ?? 0,
    };
    riskStatus.value = "done";
  } catch (err) {
    riskStatus.value = "error";
  }
};

// S4 TBM 추이 — 12포인트(BE 채움) 그대로 그린다
const fetchTbmTrend = async (ticket) => {
  tbmStatus.value = "loading";
  try {
    const response = await axios.get("/webApi/dashboard01/safety-tbm-trend", {
      params: { siteCd: ticket.siteCd, ym: ticket.ym },
    });
    tbmTrend.value = response.data?.trend ?? [];
    tbmStatus.value = "done";
  } catch (err) {
    tbmStatus.value = "error";
  }
};

// S1 배너 + S5 summary 단일 fetch — 실패는 카드 로컬 error 표기만 ($alert 금지)
const fetchAcct = async (ticket) => {
  acctStatus.value = "loading";
  try {
    const response = await axios.get("/webApi/dashboard01/safety-acct", {
      params: { siteCd: ticket.siteCd, ym: ticket.ym },
    });
    // 전 필드 기본값(0/null/[]) 위에 응답을 덮어 숫자 필드 결측 시 공백 렌더 방지
    acct.value = {
      noAcctDays: null,
      baselineYmd: null,
      baselineType: null,
      monthTotalCnt: 0,
      grade100Cnt: 0,
      grade200Cnt: 0,
      grade300Cnt: 0,
      recentAcctList: [],
      ...(response.data ?? {}),
    };
    acctYm.value = ticket.ym;
    acctStatus.value = "done";
  } catch (err) {
    acctStatus.value = "error";
  }
};

// ── S1: 배너 표시 문자열 ──────────────────────────────────────
// 무사고 일수 — 미조회/로딩/에러/null 은 '-'
const bannerDays = computed(() =>
  acctStatus.value === "done" && acct.value.noAcctDays != null
    ? acct.value.noAcctDays
    : "-"
);

// 기산일 성격 라벨 (T4.plan §2-2 — 사고 이력 없는 사업장의 기산 근거를 명시)
const BASELINE_LABELS = {
  ACCT: "최근 사고 발생일",
  SITE_STR: "사업개시일 기준",
  SITE_INSERT: "사업장 등록일 기준",
};

const bannerSince = computed(() => {
  if (acctStatus.value !== "done" || !acct.value.baselineYmd) {
    return "-";
  }
  const label = BASELINE_LABELS[acct.value.baselineType];
  const ymd = formatYmdDot(acct.value.baselineYmd);
  return label ? `${ymd} (${label})` : ymd;
});

// ── S2: 당일 점검율 (분모 0이면 0%) ──
const patrolRate = computed(() => {
  const total = patrol.value.todayTotalCnt;
  return total > 0
    ? Math.round((patrol.value.todayInspectCnt / total) * 100)
    : 0;
});

// ── S4: 당월(조회월) 완료 TBM 건수 ──
// 응답 trend 는 과거→조회월 오름차순이라 마지막 포인트가 조회월 건수다.
const tbmCurrentCnt = computed(() => {
  const list = tbmTrend.value;
  if (!list.length) return 0;
  return list[list.length - 1].cnt ?? 0;
});

// ── S5: 최근 사고 — 통합 무스크롤 목표로 상위 2건만 표시 (사용자 확정 2026-07-08) ──
const recentAcctTop = computed(() =>
  (acct.value.recentAcctList ?? []).slice(0, 2)
);

// 위젯 이동 버튼 → 부모(Dashboard_01)로 대상 라우트명 위임
const onMove = (routeName) => {
  emit("move", routeName);
};
</script>

<style scoped>
/* 통합 대시보드에서는 부모(.dash-body) 그리드가 위치를 제어한다.
   루트를 display:contents 로 해제해 위젯 카드가 부모 그리드의 직접 아이템이 되게 함. */
.dash-safety-grid {
  display: contents;
}

.area-banner {
  grid-area: banner;
}
.area-patrol {
  grid-area: patrol;
}
.area-risk {
  grid-area: risk;
}
.area-acct {
  grid-area: acct;
}
.area-tbm {
  grid-area: tbm;
}

/* ── S1 무사고 배너: primary 강조(존재감) — T4 소관, 미변경 ──── */
.dash-banner {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 0.625rem;
  flex-wrap: wrap;
  /* 상단 얇은 스트립 */
  padding: 0.4375rem 1rem;
  background: var(--color-primary, #16a34a);
  border-radius: var(--card-radius, 12px);
  box-shadow: var(--card-shadow, 0 1px 2px rgba(16, 24, 40, 0.06));
  color: var(--color-surface, #ffffff);
}

.dash-banner__site {
  font-size: 0.9375rem;
  font-weight: 700;
}

.dash-banner__text {
  font-size: 0.9375rem;
  font-weight: 600;
}

.dash-banner__days {
  font-size: 1.375rem;
  font-weight: 800;
  line-height: 1;
  margin: 0 0.25rem;
}

.dash-banner__since {
  font-size: 0.75rem;
  opacity: 0.85;
  align-self: center;
}

/* ── 위젯 조회 상태 공통 (idle / loading / error) ────────────── */
.dash-widget-state {
  flex: 1;
  min-height: 92px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: var(--color-bg, #f9fafb);
  border-radius: var(--input-radius, 10px);
  color: var(--color-text-muted, #4b5563);
  font-size: 0.8125rem;
}

.dash-widget-state--error {
  color: var(--color-danger, #ef4444);
}

/* ── S2 순회점검: 분수형 숫자 + 진행바 + 미이행 danger ───────── */
.dash-patrol {
  flex: 1;
  min-height: 96px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0.5rem;
}

.dash-patrol__label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-muted, #4b5563);
}

.dash-patrol__hint {
  font-style: normal;
  font-weight: 400;
  font-size: 0.6875rem;
  color: var(--color-text-muted, #4b5563);
}

.dash-patrol__fraction {
  display: flex;
  align-items: baseline;
  gap: 0.375rem;
}

.dash-patrol__done {
  font-size: 2rem;
  font-weight: 800;
  line-height: 1;
  color: var(--color-primary, #16a34a);
}

.dash-patrol__slash {
  font-size: 1.125rem;
  color: var(--color-text-muted, #4b5563);
}

.dash-patrol__total {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text-strong, #111827);
}

.dash-patrol__unit {
  font-size: 0.75rem;
  color: var(--color-text-muted, #4b5563);
}

.dash-patrol__rate {
  margin-left: auto;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-text-strong, #111827);
}

.dash-patrol__miss {
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-border, #e5e7eb);
  font-size: 0.8125rem;
  color: var(--color-text-muted, #4b5563);
}

.dash-patrol__miss b {
  font-size: 1rem;
}

.dash-patrol__miss--danger {
  color: var(--color-danger, #ef4444);
  font-weight: 600;
}

/* ── S3 위험성 평가: 카운트 카드 2매 + 아차사고 배지 ─────────── */
.dash-risk {
  flex: 1;
  min-height: 96px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0.625rem;
}

.dash-risk__counts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.625rem;
}

.dash-count-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.375rem;
  padding: 0.75rem 0.5rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
}

.dash-count-card__label {
  font-size: 0.75rem;
  color: var(--color-text-muted, #4b5563);
}

.dash-count-card__num {
  font-size: 1.75rem;
  font-weight: 800;
  line-height: 1;
  color: var(--color-text-strong, #111827);
}

.dash-risk__nearmiss {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-border, #e5e7eb);
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
}

.dash-badge {
  padding: 0.125rem 0.5rem;
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
  border-radius: 999px;
  font-size: 0.6875rem;
  font-weight: 700;
  white-space: nowrap;
}

.dash-risk__nearmiss-cnt {
  font-size: 1rem;
  color: var(--color-text-strong, #111827);
}

/* ── S4 TBM: 당월 완료 건수 (숫자 스탯) ──────────────────────── */
.dash-tbm {
  flex: 1;
  min-height: 140px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
}

.dash-tbm__stat {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
}

.dash-tbm__count {
  font-size: 2.5rem;
  font-weight: 800;
  line-height: 1;
  color: var(--color-text-strong, #111827);
}

.dash-tbm__unit {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-muted, #4b5563);
}

.dash-tbm__caption {
  font-size: 0.8125rem;
  color: var(--color-text-muted, #4b5563);
}

/* ── S5 사고관리 summary (T4) ────────────────────────────────── */
.dash-acct {
  flex: 1;
  min-height: 96px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0.5rem;
}

.dash-acct__month {
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
}

.dash-acct__total {
  font-size: 1.25rem;
  font-weight: 800;
  line-height: 1;
  color: var(--color-text-strong, #111827);
}

.dash-acct__grades {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
}

/* 등급 칩 — Acct_01 .grade 칩(critical/normal/exempt) 색 매핑 이식 */
.dash-grade-chip {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.125rem 0.5rem;
  border-radius: 999px;
  white-space: nowrap;
}

.dash-grade-chip.critical {
  background: var(--danger-tint, #fef2f2);
  color: var(--color-danger, #ef4444);
}

.dash-grade-chip.normal {
  background: var(--color-warning-bg, #fffbeb);
  color: var(--color-warning-text, #b45309);
}

.dash-grade-chip.exempt {
  background: var(--color-bg, #f1f5f9);
  color: var(--color-text-muted, #475569);
}

/* 0건 칩은 muted 처리 (색 매핑은 유지, 존재감만 낮춤) */
.dash-grade-chip.muted {
  opacity: 0.45;
}

.dash-acct__recent {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-border, #e5e7eb);
}

.dash-acct__caption {
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--color-text-muted, #4b5563);
}

.dash-acct__empty {
  font-size: 0.8125rem;
  color: var(--color-text-muted, #4b5563);
}

/* 행 클릭 = 카드 이동 버튼과 동일(Acct_01 이동) — 커서로 어포던스 표시 */
.dash-acct__row {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: var(--color-text, #374151);
  white-space: nowrap;
  cursor: pointer;
}

.dash-acct__row:hover {
  color: var(--color-primary, #16a34a);
}

.dash-acct__row-when {
  flex-shrink: 0;
}

.dash-acct__row-sep {
  color: var(--color-text-muted, #4b5563);
}

/* 발생장소는 길면 말줄임 */
.dash-acct__row-place {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 좁은 화면: 1열 스택 */
/* 반응형은 부모(.dash-body) 통합 그리드에서 일괄 처리 (여기서는 미제어) */
</style>
