<template>
  <!-- 근태관리 탭 위젯 레이아웃 (PRAFTA-DASHBOARD-T1 골격 / T2에서 A1·A2, T3에서 A3·A4·A5 구현)
       배치:
       ┌──────────┬──────────┬──────────┐
       │ A1 계획   │ A2 근태   │ A5 요청  │
       ├──────────┴──────────┼──────────┤
       │ A3 초과근무 추이(2칸) │ A4 연차   │
       └─────────────────────┴──────────┘
       A1·A2 = T2, A3·A4·A5 = T3 (각 골격 + script 는 developer 가 채움)
       T3 데이터 정의·API 계약: PRAFTA-DASHBOARD-T3.plan.md §1·§2 -->
  <div class="dash-attd-grid">
    <!-- A1. 근무 계획: 조직부서별 근무계획 등록율 → Attd_05
         표현(사용자 확정): 상단 전체 등록율 % 대형 숫자 + 부서별 가로 진행바 리스트,
         미등록/저조 부서 danger 강조 -->
    <DashboardWidgetCard
      class="area-plan"
      title="근무 계획 등록율"
      move-target="Attd_05"
      @move="onMove"
    >
      <!-- 기준월 네비 (상단 조회조건에서 이동) — 제목·바로가기 사이 헤더에 컴팩트 배치.
           화살표/피커 변경 시 A1 만 즉시 재조회 -->
      <template #head-actions>
        <div class="plan-month-nav">
          <button
            type="button"
            class="plan-month-nav__arr"
            aria-label="이전 달"
            @click="planPrevMonth"
          >
            ‹
          </button>
          <CalendarSrchMonth
            v-model="planMonth"
            class="plan-month-nav__picker"
            :style="{
              width: '76px',
              padding: '0.14rem 0.3rem',
              fontSize: '0.7rem',
              textAlign: 'center',
            }"
          />
          <button
            type="button"
            class="plan-month-nav__arr"
            aria-label="다음 달"
            @click="planNextMonth"
          >
            ›
          </button>
        </div>
      </template>

      <div v-if="isPlanLoading" class="dash-state">조회 중…</div>
      <div v-else-if="planError" class="dash-state is-error">{{ planError }}</div>
      <div v-else-if="!planReg" class="dash-state">
        조회 조건 선택 후 [조회]를 누르세요.
      </div>
      <div v-else class="plan-reg">
        <!-- 전체 등록율 대형 숫자 (부서별 진행바는 사용자 요청으로 제거 2026-07-08) -->
        <div class="plan-reg__summary">
          <strong class="plan-reg__rate">
            {{ planReg.regRate }}<span class="plan-reg__unit">%</span>
          </strong>
          <span class="plan-reg__count">
            등록 {{ planReg.regUserCnt }}명 / 대상 {{ planReg.totalUserCnt }}명
          </span>
        </div>
      </div>
    </DashboardWidgetCard>

    <!-- A2. 근태: 근무계획 대비 정상/비정상 근무율 → Attd_08
         표현(사용자 수정 2026-07-08): 파이 1개(정상 vs 비정상)만 유지.
           파이2(지각/조퇴/미출근 구성)는 사용자 요청으로 제거. -->
    <DashboardWidgetCard
      class="area-attd"
      title="근태 (정상 · 비정상 근무율)"
      move-target="Attd_08"
      @move="onMove"
    >
      <div v-if="isAttdLoading" class="dash-state">조회 중…</div>
      <div v-else-if="attdError" class="dash-state is-error">{{ attdError }}</div>
      <div v-else-if="!attdStatus" class="dash-state">
        조회 조건 선택 후 [조회]를 누르세요.
      </div>
      <div v-else-if="!attdStatus.targetDayCnt" class="dash-state">
        판정 대상 근무계획이 없습니다.
      </div>
      <div v-else class="attd-rate">
        <!-- 파이: 정상 vs 비정상 -->
        <div class="attd-rate__unit">
          <div class="attd-rate__donut">
            <svg class="attd-rate__svg" viewBox="0 0 120 120" role="img" aria-label="정상/비정상 근무율">
              <circle class="attd-rate__track" cx="60" cy="60" :r="DONUT_R" />
              <circle
                v-for="seg in statusDonutSegments"
                :key="seg.key"
                class="attd-rate__seg"
                :class="'is-' + seg.key"
                cx="60"
                cy="60"
                :r="DONUT_R"
                :stroke-dasharray="seg.dashArray"
                :stroke-dashoffset="seg.dashOffset"
              />
            </svg>
            <div class="attd-rate__center">
              <strong class="attd-rate__pct">{{ attdStatus.normalRate }}%</strong>
              <span class="attd-rate__label">정상</span>
            </div>
          </div>
          <ul class="attd-rate__legend">
            <li><i class="attd-rate__dot is-normal"></i>정상 <b>{{ attdStatus.normalCnt }}</b>건</li>
            <li><i class="attd-rate__dot is-abnormal"></i>비정상 <b>{{ abnormalCnt }}</b>건</li>
          </ul>
        </div>
      </div>
    </DashboardWidgetCard>

    <!-- A5. 요청정보: 숫자 카드 3분할 + 합계 강조 → Attd_10 (탭 열기만 — T1 확정)
         표현(사용자 확정): 숫자 카드 3분할(근태보정/초과근무/연차) + 합계 강조.
         카운트 출처(T3 plan §1 A5): Attd_10 이 쓰는 기존 3 엔드포인트의 목록 length 그대로
         (건수 일치를 구조적으로 보장). 상단 조회조건과 무관한 "내 결재함" 기준이다.
         API가 값을 주면 그대로 표시, 거부/미제공(403 등) 시 "-" (T3 plan §5 확정 4). -->
    <DashboardWidgetCard
      class="area-req"
      title="요청정보"
      move-target="Attd_10"
      @move="onMove"
    >
      <div v-if="isReqLoading" class="dash-state">조회 중…</div>
      <div v-else-if="reqError" class="dash-state is-error">{{ reqError }}</div>
      <div v-else class="req-info">
        <!-- 합계 강조 -->
        <div class="req-info__total">
          <strong class="req-info__total-num">{{ reqTotalCnt }}</strong>
          <span class="req-info__total-label">건 결재 대기</span>
        </div>

        <!-- 숫자 카드 3분할 (Attd_10 탭 3종과 1:1 대응) -->
        <div class="req-info__cards">
          <div class="req-info__card">
            <span class="req-info__card-label">근태 보정</span>
            <span class="req-info__card-num">{{ fmtCnt(correctionCnt) }}</span>
          </div>
          <div class="req-info__card">
            <span class="req-info__card-label">초과근무</span>
            <span class="req-info__card-num">{{ fmtCnt(overtimeCnt) }}</span>
          </div>
          <div class="req-info__card">
            <span class="req-info__card-label">연차</span>
            <span class="req-info__card-num">{{ fmtCnt(leaveCnt) }}</span>
          </div>
        </div>

        <p class="req-info__note">내 결재함(요청 승인 관리) 기준</p>
        <p
          v-if="correctionCnt === null || overtimeCnt === null || leaveCnt === null"
          class="req-info__note"
        >
          조회 권한이 있는 항목만 건수가 표시됩니다.
        </p>
      </div>
    </DashboardWidgetCard>

    <!-- A3. 초과근무: 과거 6개월 추이 → Attd_08
         표현(사용자 수정 2026-07-08): 인라인 SVG 라인차트(6개월). 12→6개월로 축소.
         값 = 월별 SUM(WORK_MINUTES), 술어는 Attd_07/08 과 동일 (T3 plan §1 A3) -->
    <DashboardWidgetCard
      class="area-ot"
      title="초과근무 (6개월 추이)"
      move-target="Attd_08"
      @move="onMove"
    >
      <div v-if="isOtLoading" class="dash-state">조회 중…</div>
      <div v-else-if="otError" class="dash-state is-error">{{ otError }}</div>
      <div v-else-if="!otMonthly.length" class="dash-state">
        조회 조건 선택 후 [조회]를 누르세요.
      </div>
      <div v-else class="ot-trend">
        <div class="ot-trend__plot">
        <svg
          class="ot-trend__chart"
          :viewBox="`0 0 ${OT_CHART.w} ${OT_CHART.h}`"
          preserveAspectRatio="none"
          role="img"
          aria-label="초과근무 6개월 추이"
        >
          <!-- 격자선: 가로(수평) + 세로(수직) — 데이터 라인 아래에 먼저 그림 -->
          <line
            v-for="(gy, i) in otGrid.horizontal"
            :key="`gh-${i}`"
            class="ot-trend__grid"
            :x1="OT_CHART.padX"
            :y1="gy"
            :x2="OT_CHART.w - OT_CHART.padX"
            :y2="gy"
          />
          <line
            v-for="(gx, i) in otGrid.vertical"
            :key="`gv-${i}`"
            class="ot-trend__grid"
            :x1="gx"
            :y1="OT_CHART.padTop"
            :x2="gx"
            :y2="OT_CHART.h - OT_CHART.padBottom"
          />
          <!-- 하단 기준선(x축) -->
          <line
            class="ot-trend__axis"
            :x1="OT_CHART.padX"
            :y1="OT_CHART.h - OT_CHART.padBottom"
            :x2="OT_CHART.w - OT_CHART.padX"
            :y2="OT_CHART.h - OT_CHART.padBottom"
          />
          <polyline class="ot-trend__line" :points="otPolyline" />
          <!-- 포인트: hover 시 값 라벨 표시 (네이티브 title 은 중복 툴팁이라 미사용) -->
          <g v-for="p in otPoints" :key="p.ym" class="ot-trend__pt">
            <text
              class="ot-trend__val"
              :x="p.x"
              :y="p.y - 13"
              text-anchor="middle"
            >
              {{ fmtOtHours(p.minutes) }}
            </text>
            <circle
              class="ot-trend__dot"
              :class="{ 'is-base': p.ym === searchYm }"
              :cx="p.x"
              :cy="p.y"
              r="2.45"
            />
            <!-- hover 판정 확장용 투명 히트 영역 -->
            <circle class="ot-trend__hit" :cx="p.x" :cy="p.y" r="12" />
          </g>
        </svg>
        <!-- y축 눈금값(초과근무 시간): SVG 밖 HTML 오버레이 — 격자선 높이에 맞춰 좌측 배치 -->
        <div class="ot-trend__yaxis" aria-hidden="true">
          <span
            v-for="(t, i) in otYTicks"
            :key="`yt-${i}`"
            class="ot-trend__ytick"
            :style="{ top: t.topPct }"
          >
            {{ t.label }}
          </span>
        </div>
        </div>
        <!-- 월 라벨: SVG 밖 HTML 로 렌더(파이 범례와 동일 실측 폰트). 각 포인트 아래 정렬 -->
        <div class="ot-trend__months">
          <span
            v-for="p in otPoints"
            :key="p.ym"
            class="ot-trend__month"
            :class="{ 'is-base': p.ym === searchYm }"
            :style="{ left: otMonthLeft(p.x) }"
          >
            {{ fmtMonthLabel(p.ym) }}
          </span>
        </div>
      </div>
    </DashboardWidgetCard>

    <!-- A4. 연차: 법정연차 사용/사용예정/미사용 3분할 → Attd_05
         표현(사용자 확정): 반원 게이지(SVG) 2색 세그먼트(사용/사용예정) + 잔여 트랙(미사용)
         + 범례 3종 일수 병기 + "현재 기준" 캡션.
         부여 = STATUTORY_% · STATUS IN ('ACTIVE','EXHAUSTED'), 사용/예정 = tb_user_leave_use
         실합계(GRANT_ID 경유·오늘 기준 분리). 현재 시점 스냅샷 — 조회월 미적용.
         상세: T3 plan §1 A4·§5 확정 1~3 -->
    <DashboardWidgetCard
      class="area-leave"
      title="연차 사용율 (법정연차)"
      move-target="Attd_05"
      @move="onMove"
    >
      <div v-if="isLeaveLoading" class="dash-state">조회 중…</div>
      <div v-else-if="leaveError" class="dash-state is-error">{{ leaveError }}</div>
      <div v-else-if="!leaveUsage" class="dash-state">
        조회 조건 선택 후 [조회]를 누르세요.
      </div>
      <div v-else class="leave-rate">
        <div class="leave-rate__gauge-wrap">
          <svg class="leave-rate__gauge" viewBox="0 0 200 112" aria-hidden="true">
            <!-- 반원 트랙 = 미사용 잔여 (r=84, 반둘레 ≈ 263.9) -->
            <path
              class="leave-rate__track"
              d="M 16 104 A 84 84 0 0 1 184 104"
            />
            <!-- 사용 세그먼트 (시작점부터) -->
            <path
              class="leave-rate__seg is-used"
              d="M 16 104 A 84 84 0 0 1 184 104"
              :stroke-dasharray="`${leaveUsedLen} ${LEAVE_GAUGE_FULL}`"
            />
            <!-- 사용예정 세그먼트 (사용 길이만큼 뒤에서 이어짐 — 음수 dashoffset) -->
            <path
              class="leave-rate__seg is-planned"
              d="M 16 104 A 84 84 0 0 1 184 104"
              :stroke-dasharray="`${leavePlannedLen} ${LEAVE_GAUGE_FULL}`"
              :stroke-dashoffset="-leaveUsedLen"
            />
          </svg>
          <!-- 중앙: 사용율(사용/부여) -->
          <div class="leave-rate__pct">
            {{ leaveUsedRatePct }}<span class="leave-rate__pct-unit">%</span>
          </div>
        </div>
        <!-- 범례 3종 (총량 기준 비율). 일수는 표기하지 않는다 — 인원수에 비례해 커져
             부서 간 비교가 불가능하고, 시간차 연차가 순환소수로 환산돼 소수점이 길게 붙는다. -->
        <ul class="leave-rate__legend">
          <li>
            <i class="leave-rate__dot is-used"></i>사용
            <b>{{ leaveUsedRatePct }}</b>%
          </li>
          <li>
            <i class="leave-rate__dot is-planned"></i>예정
            <b>{{ leavePlannedRatePct }}</b>%
          </li>
          <li>
            <i class="leave-rate__dot is-unused"></i>미사용
            <b>{{ leaveUnusedRatePct }}</b>%
          </li>
        </ul>
        <p class="leave-rate__note">현재 기준 · 법정연차(본연차·월차·근속가산)</p>
      </div>
    </DashboardWidgetCard>
  </div>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import CalendarSrchMonth from "@/components/common/CalendarSrchMonth.vue";
import DashboardWidgetCard from "./DashboardWidgetCard.vue";

defineOptions({ name: "DashboardAttdTab" });

const props = defineProps({
  // Dashboard_01 조회 버튼이 갱신하는 조회조건 스냅샷 (객체 교체로 watch 트리거).
  //   { siteCd, nodeCd, nodeNm, incSubNodeYn(Boolean), ym: 'YYYY-MM', ts: Date.now() }
  //   ts 는 동일 조건 재조회도 트리거하기 위한 필드.
  searchParams: { type: Object, default: null },
  // 기준월 (YYYY-MM) — 부모와 v-model:base-ym 로 양방향 동기.
  //   '근무 계획 등록율' 위젯의 월 네비(화살표/피커)가 이 값을 바꾸면 A1 만 재조회한다.
  baseYm: { type: String, default: "" },
});

const emit = defineEmits(["move", "update:baseYm"]);

// 위젯 이동 버튼 → 부모(Dashboard_01)로 대상 라우트명 위임
const onMove = (routeName) => {
  emit("move", routeName);
};

// ── A1 기준월 네비 (사용자 요청 2026-07-08: 기준월을 A1 위젯으로 이동) ──
//   월 변경 시: 부모 baseYm 동기(update:baseYm) + A1(근무계획 등록율)만 재조회.
//   A2/A3/안전탭은 건드리지 않는다(전체 재조회는 상단 [조회] 버튼 전용).
const planMonth = computed({
  get: () => props.baseYm,
  set: (ym) => {
    emit("update:baseYm", ym);
    fetchPlanRegByMonth(ym);
  },
});

// 화살표 이동 (Attd_07 shiftMonth 패턴) — 현재월 없으면 오늘 기준
const shiftPlanMonth = (delta) => {
  const cur = props.baseYm || (() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
  })();
  const [y, m] = cur.split("-").map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  planMonth.value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
};
const planPrevMonth = () => shiftPlanMonth(-1);
const planNextMonth = () => shiftPlanMonth(1);

// A1 만 재조회 — 사업장/부서는 마지막 조회조건(searchParams)에서, 월만 새 값 사용.
//   사업장 미선택이면 A1 조회 불가 → 스킵.
const fetchPlanRegByMonth = (ym) => {
  const p = props.searchParams;
  if (!p || !p.siteCd) return;
  fetchPlanReg({
    siteCd: p.siteCd,
    nodeCd: p.nodeCd,
    incSubNodeYn: p.incSubNodeYn ? "Y" : "N",
    workYm: ym,
  });
};

// ── A1. 근무계획 등록율 상태 ──────────────────────────────────
const isPlanLoading = ref(false);
const planError = ref("");
// 응답 계약: { totalUserCnt, regUserCnt, regRate,
//              deptList: [...] } — deptList 는 더 이상 표시하지 않음(부서별 진행바 제거)
const planReg = ref(null);

// ── A2. 정상/비정상 근무율 상태 ───────────────────────────────
const isAttdLoading = ref(false);
const attdError = ref("");
// 응답 계약: { targetDayCnt, normalCnt, lateCnt, earlyLeaveCnt, absentCnt, normalRate }
const attdStatus = ref(null);

// 도넛 지오메트리 (viewBox 120 기준 반지름)
const DONUT_R = 48;

// 도넛 세그먼트 빌더 — parts(순서대로) 를 denom 기준 원둘레 비율로 dashArray/dashOffset 계산.
//   len = C * (cnt / denom), dashArray=`${len} ${C-len}`, dashOffset=-(앞 누적)
//   cnt=0 세그먼트는 제외, denom=0 이면 [] (빈 트랙만). 12시 기점은 CSS rotate(-90deg).
const buildSegments = (parts, denom) => {
  if (!denom) return [];
  const C = 2 * Math.PI * DONUT_R;
  const segments = [];
  let acc = 0; // 앞 세그먼트 길이 누적
  for (const p of parts) {
    if (!p.cnt) continue; // 0건 세그먼트는 그리지 않음
    const len = C * (p.cnt / denom);
    segments.push({
      key: p.key,
      dashArray: `${len} ${C - len}`,
      dashOffset: -acc,
    });
    acc += len;
  }
  return segments;
};

// 비정상 합계 (지각 + 조퇴 + 미출근)
const abnormalCnt = computed(() => {
  const s = attdStatus.value;
  if (!s) return 0;
  return (s.lateCnt || 0) + (s.earlyLeaveCnt || 0) + (s.absentCnt || 0);
});

// 파이1: 정상 → 비정상 (분모 = 판정 대상 계획일)
const statusDonutSegments = computed(() => {
  const s = attdStatus.value;
  if (!s || !s.targetDayCnt) return [];
  return buildSegments(
    [
      { key: "normal", cnt: s.normalCnt },
      { key: "abnormal", cnt: abnormalCnt.value },
    ],
    s.targetDayCnt
  );
});

// ── A3. 초과근무 6개월 추이 상태 (T3) ─────────────────────────
const isOtLoading = ref(false);
const otError = ref("");
// 응답 계약: monthlyList = [{ ym: 'YYYY-MM', totalMinutes: Number }] × 6 (오름차순, 서버가 0 채움)
const otMonthly = ref([]);

// ── A4. 연차 3분할 상태 (T3) ──────────────────────────────────
const isLeaveLoading = ref(false);
const leaveError = ref("");
// 응답 계약: { grantDays, usedDays, plannedDays } — 법정(STATUTORY_%)·ACTIVE+EXHAUSTED 기준.
//   usedDays = 사용(START_DATE <= 오늘) / plannedDays = 사용예정(START_DATE > 오늘).
//   일수 3종은 게이지/범례 비율의 분모로만 쓰고 화면에는 표기하지 않는다
//   (인원수에 비례해 커져 부서 간 비교 불가 + 시간차 연차가 순환소수라 소수점이 길게 붙음).
const leaveUsage = ref(null);

// ── A5. 요청정보 상태 (T3) ────────────────────────────────────
const isReqLoading = ref(false);
const reqError = ref("");
// null = 미조회/미제공(권한 거부 포함) → "-" 표시. 숫자 = Attd_10 동일 엔드포인트 목록 length 그대로.
const correctionCnt = ref(null);
const overtimeCnt = ref(null);
const leaveCnt = ref(null);

// ── T3 표시 전용 계산 (프레젠테이션 로직) ─────────────────────

// A5 합계 — 조회된 카운트만 합산 (null 은 제외)
const reqTotalCnt = computed(
  () =>
    (correctionCnt.value ?? 0) + (overtimeCnt.value ?? 0) + (leaveCnt.value ?? 0)
);

const fmtCnt = (v) => (v === null || v === undefined ? "-" : v);

// A3: 기준월(조회월) — 막대 강조용
const searchYm = computed(() => props.searchParams?.ym ?? "");

// A3: 분 → 시간 라벨 (정수면 소수부 생략, 아니면 소수 1자리)
const fmtOtHours = (minutes) => {
  const h = (Number(minutes) || 0) / 60;
  return `${Number.isInteger(h) ? h : h.toFixed(1)}h`;
};

// A3: SVG 라인차트 좌표 (순수 SVG — 외부 차트 라이브러리 금지, TBM 추이와 동일 규약).
//   CHART 는 viewBox 내부 좌표계(화면 px 아님 — 카드 폭에 비례 스케일).
//   ★ 월 라벨은 SVG 밖 HTML 로 렌더(viewBox 스케일 미적용)해 파이 범례와 동일 실측 폰트를 쓴다.
//     그래서 하단 여백(padBottom)은 x축 선만 있으면 되므로 작게 둔다.
//   ★ 하단 풀폭 스트립 → 플롯 컨테이너에 동일 종횡비(aspect-ratio: w/h)를 주고 SVG 는
//     preserveAspectRatio="none" 로 정확히 채운다. 박스=viewBox 종횡비라 스케일이 x·y 균일 →
//     왜곡 없음(점 원형 유지)·넘침 없음·y축 오버레이 정렬 유지. h 를 줄여 스트립을 얇게.
//   ※ 아래 .ot-trend__plot { aspect-ratio } 값을 이 w/h 와 반드시 동일하게 유지할 것(1200/170).
const OT_CHART = { w: 1200, h: 170, padX: 60, padTop: 22, padBottom: 10 };

// y 스케일 상한 (전부 0이어도 1로 나눠 폴리라인이 바닥에 붙게)
const otMaxMinutes = computed(() =>
  Math.max(1, ...otMonthly.value.map((m) => Number(m.totalMinutes) || 0))
);

// 6포인트 → viewBox 좌표 (과거→조회월 오름차순 그대로)
const otPoints = computed(() => {
  const n = otMonthly.value.length;
  if (n === 0) return [];
  const innerW = OT_CHART.w - OT_CHART.padX * 2;
  const innerH = OT_CHART.h - OT_CHART.padTop - OT_CHART.padBottom;
  return otMonthly.value.map((m, i) => {
    const minutes = Number(m.totalMinutes) || 0;
    return {
      x: OT_CHART.padX + (n === 1 ? innerW / 2 : (i * innerW) / (n - 1)),
      y: OT_CHART.h - OT_CHART.padBottom - (minutes / otMaxMinutes.value) * innerH,
      ym: m.ym,
      minutes,
    };
  });
});

const otPolyline = computed(() =>
  otPoints.value.map((p) => `${p.x},${p.y}`).join(" ")
);

// A3: 격자선 — 가로(수평) 4등분 선 + 세로(수직) 각 월 위치 선.
//   차트 안쪽 영역(padTop~padBottom) 좌표로 계산, 데이터 라인 아래에 깔린다.
const otGrid = computed(() => {
  const top = OT_CHART.padTop;
  const bottom = OT_CHART.h - OT_CHART.padBottom;
  const innerH = bottom - top;
  const rows = 4; // 가로선 등분 수 (기준선 제외 위쪽 3개 + 최상단)

  const horizontal = [];
  for (let i = 0; i < rows; i++) {
    // i=0 은 최상단, 맨 아래(bottom)는 x축(axis)이 대신하므로 제외
    horizontal.push(top + (innerH * i) / rows);
  }
  const vertical = otPoints.value.map((p) => p.x);
  return { horizontal, vertical };
});

// A3: y축 눈금 — 0 ~ 최대값을 4등분(총 5개). 각 눈금은 격자선/기준선과 같은 높이에 온다.
//   HTML 로 좌측에 렌더(월 라벨과 동일 실측 폰트)하므로 topPct(=SVG 높이 대비 %)를 함께 계산.
const otYTicks = computed(() => {
  const rows = 4;
  const top = OT_CHART.padTop;
  const bottom = OT_CHART.h - OT_CHART.padBottom;
  const innerH = bottom - top;
  const max = otMaxMinutes.value;

  const ticks = [];
  for (let k = 0; k <= rows; k++) {
    const y = bottom - (innerH * k) / rows;
    ticks.push({
      topPct: `${(y / OT_CHART.h) * 100}%`,
      label: fmtOtHours((max * k) / rows),
    });
  }
  return ticks;
});

// A3: 'YYYY-MM' → 'YYYY-M' (한 자리 월은 앞 0 제거, 두 자리 월은 그대로)
//   예) '2025-08' → '2025-8', '2026-11' → '2026-11'
const fmtMonthLabel = (ym) => {
  if (!ym || ym.length < 7) return ym || "";
  return `${ym.slice(0, 4)}-${Number(ym.slice(5, 7))}`;
};

// A3: HTML 월 라벨의 가로 위치(%) — SVG 포인트 x(viewBox 좌표)를 폭 비율로 환산.
//   SVG 와 라벨 컨테이너가 같은 100% 폭이라 포인트 바로 아래 정렬된다.
const otMonthLeft = (x) => `${(x / OT_CHART.w) * 100}%`;

// A4: 반원 게이지 (r=84 반둘레). 세그먼트 합이 반둘레를 넘지 않게 클램프.
const LEAVE_GAUGE_FULL = 263.9;

// 부여 대비 비율 (0~1 클램프)
const leaveRatio = (v) => {
  const grant = Number(leaveUsage.value?.grantDays) || 0;
  if (grant <= 0) return 0;
  return Math.min(Math.max((Number(v) || 0) / grant, 0), 1);
};

// 중앙 % = 총량 소진율(Σ사용 ÷ Σ부여). 게이지 세그먼트와 동일하게 0~100 클램프.
const leaveUsedRatePct = computed(
  () => Math.round(leaveRatio(leaveUsage.value?.usedDays) * 100)
);

// 예정 % — 게이지 세그먼트와 동일 규칙(사용 잔여분까지만 채움).
//   사용·예정이 각각 올림되어 합이 101%가 되지 않도록, 반올림 후에도 (100 − 사용) 상한을 건다.
const leavePlannedRatePct = computed(() => {
  const usedR = leaveRatio(leaveUsage.value?.usedDays);
  const plannedR = Math.min(leaveRatio(leaveUsage.value?.plannedDays), 1 - usedR);
  return Math.min(Math.round(plannedR * 100), 100 - leaveUsedRatePct.value);
});

// 미사용 % = 잔여(100 − 사용 − 예정). 셋의 합이 항상 100 이 되도록 잔여로 계산한다.
//   부여 자체가 0 이면 미사용도 0 — "연차 없음"을 "100% 미사용"으로 보이게 하지 않는다.
const leaveUnusedRatePct = computed(() => {
  if ((Number(leaveUsage.value?.grantDays) || 0) <= 0) return 0;
  return Math.max(100 - leaveUsedRatePct.value - leavePlannedRatePct.value, 0);
});

// 게이지 세그먼트 길이 — 사용(시작점부터), 사용예정(사용 뒤에 이어붙임)
const leaveUsedLen = computed(
  () => leaveRatio(leaveUsage.value?.usedDays) * LEAVE_GAUGE_FULL
);
const leavePlannedLen = computed(() => {
  const usedR = leaveRatio(leaveUsage.value?.usedDays);
  const plannedR = Math.min(leaveRatio(leaveUsage.value?.plannedDays), 1 - usedR);
  return plannedR * LEAVE_GAUGE_FULL;
});

// ── 조회 트리거 (T2: A1·A2) ───────────────────────────────────
// Dashboard_01 fnSearch 가 searchParams 객체를 교체하면 A1/A2 를 병행 재조회한다.
watch(
  () => props.searchParams,
  (p) => {
    if (!p || !p.siteCd) return;
    const params = {
      siteCd: p.siteCd,
      nodeCd: p.nodeCd,
      incSubNodeYn: p.incSubNodeYn ? "Y" : "N",
      workYm: p.ym,
    };
    fetchPlanReg(params);
    fetchAttdStatus(params);
  }
);

// A1 근무계획 등록율 — 위젯 독립 로딩/에러 (한쪽 실패가 다른 위젯을 죽이지 않는다)
const fetchPlanReg = async (params) => {
  isPlanLoading.value = true;
  planError.value = "";
  try {
    const res = await axios.get("/webApi/dashboard01/attd-plan-reg-rate", {
      params,
    });
    planReg.value = res.data;
  } catch (err) {
    planError.value = resolveApiErrorMessage(
      err,
      getMessage(MSG.SEARCH_ERROR_DEFAULT)
    );
  } finally {
    isPlanLoading.value = false;
  }
};

// A2 정상/비정상 근무율 — 위젯 독립 로딩/에러
const fetchAttdStatus = async (params) => {
  isAttdLoading.value = true;
  attdError.value = "";
  try {
    const res = await axios.get("/webApi/dashboard01/attd-status-rate", {
      params,
    });
    attdStatus.value = res.data;
  } catch (err) {
    attdError.value = resolveApiErrorMessage(
      err,
      getMessage(MSG.SEARCH_ERROR_DEFAULT)
    );
  } finally {
    isAttdLoading.value = false;
  }
};

// ── 조회 트리거 (T3: A3·A4·A5) — T3-04 ────────────────────────
// 실패는 위젯별 에러 상태로만 표시한다(배지성 데이터 — alert 남발 금지, T3 plan §3).
watch(
  () => props.searchParams,
  (p) => {
    if (!p || !p.siteCd) return;
    fetchOtTrend(p); // A3
    fetchLeaveUsage(p); // A4
    fetchReqCounts(); // A5 (상단 조회조건 무관 — 내 결재함 기준)
  }
);

// A3 초과근무 6개월 추이 — 위젯 독립 로딩/에러. baseYm=조회월, 서버가 6포인트 0채움
const fetchOtTrend = async (p) => {
  isOtLoading.value = true;
  otError.value = "";
  try {
    const res = await axios.get("/webApi/dashboard01/overtime-trend", {
      params: {
        siteCd: p.siteCd,
        nodeCd: p.nodeCd,
        incSubNodeYn: p.incSubNodeYn ? "Y" : "N",
        baseYm: p.ym,
      },
    });
    otMonthly.value = res.data?.monthlyList ?? [];
  } catch (err) {
    otError.value = resolveApiErrorMessage(
      err,
      getMessage(MSG.SEARCH_ERROR_DEFAULT)
    );
    otMonthly.value = [];
  } finally {
    isOtLoading.value = false;
  }
};

// A4 법정연차 3분할 — baseYm 미전송(현재 스냅샷). 미사용 %는 FE 파생(leaveUnusedRatePct)
const fetchLeaveUsage = async (p) => {
  isLeaveLoading.value = true;
  leaveError.value = "";
  try {
    const res = await axios.get("/webApi/dashboard01/leave-usage", {
      params: {
        siteCd: p.siteCd,
        nodeCd: p.nodeCd,
        incSubNodeYn: p.incSubNodeYn ? "Y" : "N",
      },
    });
    const d = res.data ?? {};
    leaveUsage.value = {
      grantDays: d.grantDays ?? 0,
      usedDays: d.usedDays ?? 0,
      plannedDays: d.plannedDays ?? 0,
    };
  } catch (err) {
    leaveError.value = resolveApiErrorMessage(
      err,
      getMessage(MSG.SEARCH_ERROR_DEFAULT)
    );
    leaveUsage.value = null;
  } finally {
    isLeaveLoading.value = false;
  }
};

// A5 요청정보 — Attd_10 fnLoadCounts 와 동일 3 엔드포인트 재사용(건수 일치 보장).
// 3건 개별 처리(catch) — 실패(403 권한 거부 포함) 시 해당 카운트만 null 유지("-").
// reqError 는 3건 전부 실패했을 때만 세팅(위젯 전체 에러) — T3 plan §5 확정 4.
const fetchReqCounts = async () => {
  isReqLoading.value = true;
  reqError.value = "";
  correctionCnt.value = null;
  overtimeCnt.value = null;
  leaveCnt.value = null;

  const tasks = [
    axios
      .get("/webApi/leaveflow/my-approvals")
      .then((r) => {
        leaveCnt.value = (r.data?.approvalList ?? []).length;
        return true;
      })
      .catch(() => false),
    axios
      .get("/webApi/reqinbox/pending", {
        params: { reqTypeGroup: "correction" },
      })
      .then((r) => {
        correctionCnt.value = (r.data?.pendingList ?? []).length;
        return true;
      })
      .catch(() => false),
    axios
      .get("/webApi/reqinbox/pending", {
        params: { reqTypeGroup: "overtime" },
      })
      .then((r) => {
        overtimeCnt.value = (r.data?.pendingList ?? []).length;
        return true;
      })
      .catch(() => false),
  ];

  const results = await Promise.all(tasks);
  isReqLoading.value = false;
  // 3건 전부 실패 시에만 위젯 에러 (일부 실패는 해당 카운트만 "-")
  if (results.every((ok) => !ok)) {
    reqError.value = getMessage(MSG.SEARCH_ERROR_DEFAULT);
  }
};
</script>

<style scoped>
/* 통합 대시보드에서는 부모(.dash-body) 그리드가 위치를 제어한다.
   이 루트를 display:contents 로 해제해 위젯 카드가 부모 그리드의 직접 아이템이 되게 함.
   각 카드의 grid-area(.area-*)는 그대로 유지 → 부모 grid-template-areas 가 배치. */
.dash-attd-grid {
  display: contents;
}

.area-plan {
  grid-area: plan;
}
.area-attd {
  grid-area: attd;
}
.area-req {
  grid-area: req;
}
.area-ot {
  grid-area: ot;
}
.area-leave {
  grid-area: leave;
}

/* ── 위젯 상태 공통 (loading / error / 미조회 / empty) ───────── */
.dash-state {
  flex: 1;
  min-height: 92px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  line-height: 1.6;
  color: var(--color-text-muted, #4b5563);
  font-size: 0.8125rem;
}

.dash-state.is-error {
  color: var(--color-danger, #ef4444);
}

/* ── A1. 근무계획 등록율 ─────────────────────────────────────── */
/* 기준월 네비 (제목·바로가기 사이 헤더에 컴팩트 배치) — 화살표 ‹ [피커] › */
.plan-month-nav {
  display: flex;
  align-items: center;
  gap: 0.125rem;
}

.plan-month-nav__arr {
  width: 20px;
  height: 20px;
  border: 1px solid var(--color-border-strong, #d1d5db);
  background: var(--color-surface, #ffffff);
  border-radius: var(--btn-radius, 8px);
  cursor: pointer;
  color: var(--color-text-muted, #4b5563);
  font-size: 0.8125rem;
  line-height: 1;
  flex-shrink: 0;
  padding: 0;
}

.plan-month-nav__arr:hover {
  color: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
}

/* flat-pickr 입력창(내부) 여백 축소 — :style prop 과 함께 컴팩트하게 */
.plan-month-nav__picker :deep(.calendar-input) {
  border-radius: var(--btn-radius, 8px);
}

.plan-reg {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  min-height: 92px;
}

/* 전체 등록율만 표시 (부서별 진행바 제거) — 중앙 큰 숫자 + 하단 카운트 */
.plan-reg__summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.375rem;
}

.plan-reg__rate {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1;
  color: var(--color-text-strong, #111827);
}

.plan-reg__unit {
  font-size: 1.125rem;
  font-weight: 600;
  margin-left: 2px;
}

.plan-reg__count {
  font-size: 0.8125rem;
  color: var(--color-text-muted, #4b5563);
}

/* ── A2. 정상/비정상 근무율 도넛 1개 (정상 vs 비정상) ── */
.attd-rate {
  flex: 1;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  min-height: 100px;
  flex-wrap: wrap;
}

/* 도넛(왼쪽) + 범례(오른쪽) 1조 — 콘텐츠 폭만 차지해 영역 중앙에 오도록 flex 성장 안 함 */
.attd-rate__unit {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.attd-rate__donut {
  position: relative;
  width: 108px;
  height: 108px;
  flex-shrink: 0;
}

.attd-rate__svg {
  width: 100%;
  height: 100%;
  display: block;
}

.attd-rate__track {
  fill: none;
  stroke: var(--color-bg, #f9fafb);
  stroke-width: 14;
}

.attd-rate__seg {
  fill: none;
  stroke: currentColor;
  stroke-width: 14;
  /* 12시 기점 시계방향 (dashOffset 은 누적길이 음수만 주면 됨) */
  transform: rotate(-90deg);
  transform-origin: 60px 60px;
}

.attd-rate__center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.125rem;
  pointer-events: none;
}

.attd-rate__pct {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1;
  color: var(--color-text-strong, #111827);
}

.attd-rate__label {
  font-size: 0.6875rem;
  color: var(--color-text-muted, #4b5563);
}

.attd-rate__legend {
  flex: 1;
  min-width: 0;
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: var(--color-text, #374151);
}

.attd-rate__legend b {
  font-variant-numeric: tabular-nums;
}

.attd-rate__dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  margin-right: 0.375rem;
  vertical-align: middle;
}

/* 상태 색 매핑 — tokens.css 상태 토큰만 사용 (도넛 세그먼트 stroke + 범례 dot 공용) */
.is-normal {
  color: var(--color-primary, #16a34a);
}
.is-abnormal {
  color: var(--color-danger, #ef4444);
}
.is-late {
  color: var(--color-warning-text, #b45309);
}
.is-early {
  color: var(--color-text-muted, #4b5563);
}
.is-absent {
  color: var(--color-danger, #ef4444);
}

/* ── A5. 요청정보 (숫자 카드 3분할 + 합계 강조) ──────────────── */
.req-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 92px;
}

.req-info__total {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 0.375rem;
  padding: 0.5rem 0 0.75rem;
}

.req-info__total-num {
  font-size: 2rem;
  font-weight: 700;
  line-height: 1;
  color: var(--color-primary, #16a34a);
}

.req-info__total-label {
  font-size: 0.8125rem;
  color: var(--color-text-muted, #4b5563);
}

.req-info__cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.5rem;
}

.req-info__card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  padding: 0.625rem 0.25rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
}

.req-info__card-label {
  font-size: 0.6875rem;
  color: var(--color-text-muted, #4b5563);
  white-space: nowrap;
}

.req-info__card-num {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text-strong, #111827);
  font-variant-numeric: tabular-nums;
}

.req-info__note {
  margin: 0.5rem 0 0;
  font-size: 0.6875rem;
  line-height: 1.4;
  color: var(--color-text-muted, #4b5563);
  text-align: center;
}

/* ── A3. 초과근무 6개월 추이 (인라인 SVG 라인차트 + HTML 월 라벨) ──
   ★ 컨테이너에 정의된 px 높이를 준다. aspect-ratio/percent 높이는 그리드 auto-row 계산에서
     과소집계되어 카드가 실제보다 짧게 잡히고 하단 월 라벨이 카드 밖으로 넘쳤다. 확정 px 로 해소. */
.ot-trend {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  height: 188px;
}

/* 플롯 영역 — 남은 공간을 채운다(월 라벨 행은 하단 고정). SVG(preserveAspectRatio="none")가 정확히 채움 */
.ot-trend__plot {
  position: relative;
  flex: 1;
  min-height: 0;
  width: 100%;
}

.ot-trend__chart {
  display: block;
  width: 100%;
  height: 100%;
}

/* y축 눈금값 오버레이 — 플롯(SVG) 박스에 겹쳐 좌측 여백(padX) 안에 우측정렬 */
.ot-trend__yaxis {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.ot-trend__ytick {
  position: absolute;
  left: 0;
  /* padX(60)/w(1200) = 5% 폭 안에 우측정렬 */
  width: 5%;
  padding-right: 4px;
  transform: translateY(-50%);
  text-align: right;
  white-space: nowrap;
  font-size: 0.75rem;
  color: var(--color-text-muted, #4b5563);
}

/* 월 라벨 행 — SVG 와 동일 100% 폭. 각 라벨을 포인트 x(%) 아래 절대배치. 하단 고정(축소 금지) */
.ot-trend__months {
  position: relative;
  width: 100%;
  height: 1.1rem;
  flex-shrink: 0;
}

/* SVG 내부 단위는 viewBox 좌표계(px 표기) — 카드 폭에 비례 스케일 */
/* preserveAspectRatio=none 로 x·y 스케일이 달라지므로, 선 굵기는 스케일 무시(non-scaling)해 균일하게 */
.ot-trend__axis {
  stroke: var(--color-border-strong, #d1d5db);
  stroke-width: 1;
  vector-effect: non-scaling-stroke;
}

.ot-trend__grid {
  vector-effect: non-scaling-stroke;
}

.ot-trend__line {
  fill: none;
  stroke: #555555;
  stroke-width: 2;
  stroke-linejoin: round;
  stroke-linecap: round;
  vector-effect: non-scaling-stroke;
}

/* 격자선(가로/세로) — 연한 톤으로 눈금 보조, 데이터 라인 뒤에 깔림 */
.ot-trend__grid {
  stroke: var(--color-border, #e5e7eb);
  stroke-width: 1;
}

.ot-trend__dot {
  fill: #555555;
  stroke: var(--color-surface, #ffffff);
  stroke-width: 1.5;
}

/* 조회월(기준월) 포인트 강조 (기본 3.5 → 70% 축소 반영해 5→3.5) */
.ot-trend__dot.is-base {
  r: 3.5;
}

.ot-trend__hit {
  fill: transparent;
}

/* 값 라벨: 평소 숨김, 포인트 hover 시 표시 (본문 텍스트와 동일 서식 — 앱 폰트 상속).
   선 위에 겹쳐도 읽히도록 흰색 외곽선(halo) 을 두른다(paint-order 로 stroke 를 fill 아래에). */
.ot-trend__val {
  fill: var(--color-text-strong, #111827);
  font-family: inherit;
  font-size: 11px;
  font-weight: 600;
  paint-order: stroke;
  stroke: var(--color-surface, #ffffff);
  stroke-width: 3px;
  stroke-linejoin: round;
  opacity: 0;
  transition: opacity 0.15s ease;
  pointer-events: none;
}

.ot-trend__pt:hover .ot-trend__val {
  opacity: 1;
}

.ot-trend__pt:hover .ot-trend__dot {
  r: 3.5;
}

/* 월 라벨(HTML) — A2 파이 범례(.attd-rate__legend)와 동일 서식(실측 0.75rem, viewBox 스케일 무영향) */
.ot-trend__month {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  white-space: nowrap;
  font-size: 0.75rem;
  color: var(--color-text, #374151);
}

.ot-trend__month.is-base {
  color: var(--color-text-strong, #111827);
  font-weight: 700;
}

/* ── A4. 연차 3분할 (반원 게이지 — 사용/사용예정 세그먼트 + 미사용 트랙) ── */
.leave-rate {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100px;
}

.leave-rate__gauge-wrap {
  position: relative;
  width: 180px;
}

.leave-rate__gauge {
  display: block;
  width: 100%;
  height: auto;
}

/* 트랙 = 미사용 잔여 */
.leave-rate__track {
  fill: none;
  stroke: var(--color-bg, #f9fafb);
  stroke-width: 16;
  stroke-linecap: round;
}

.leave-rate__seg {
  fill: none;
  stroke: currentColor;
  stroke-width: 16;
  stroke-linecap: butt; /* 세그먼트 이음새가 겹치지 않게 round 미사용 */
  transition: stroke-dasharray 0.3s ease, stroke-dashoffset 0.3s ease;
}

/* 3분할 색 매핑 (게이지 세그먼트 stroke + 범례 dot 공용) */
.leave-rate__seg.is-used,
.leave-rate__dot.is-used {
  color: var(--color-primary, #16a34a);
}

.leave-rate__seg.is-planned,
.leave-rate__dot.is-planned {
  color: var(--color-warning-text, #b45309);
}

.leave-rate__dot.is-unused {
  color: var(--color-border-strong, #d1d5db);
}

.leave-rate__pct {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0.25rem;
  text-align: center;
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1;
  color: var(--color-text-strong, #111827);
}

.leave-rate__pct-unit {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-muted, #4b5563);
  margin-left: 0.125rem;
}

.leave-rate__legend {
  list-style: none;
  margin: 0.5rem 0 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.25rem 0.75rem;
  font-size: 0.75rem;
  color: var(--color-text, #374151);
}

.leave-rate__legend b {
  font-variant-numeric: tabular-nums;
}

.leave-rate__dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  margin-right: 0.25rem;
  vertical-align: middle;
}

.leave-rate__note {
  margin: 0.375rem 0 0;
  font-size: 0.6875rem;
  color: var(--color-text-muted, #4b5563);
}

/* 반응형은 부모(.dash-body) 통합 그리드에서 일괄 처리 (여기서는 미제어) */
</style>
