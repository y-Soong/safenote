<!--
  AttendanceWeekList.vue — 이번주 7일 리스트 + 주 합계
  - 작업 ID: APP002-07 (UI 명세: UI-A003)
  - 시안 화면 6: 주 네비 + 7 요일 카드 + 합계 카드
  - 정책: 시안 §3.7·§4.3, attd §10.1
  - 카드 탭 → 부모에 select-day emit → 부모가 바텀시트 오픈
-->
<template>
  <div v-if="week" class="week-list">
    <!-- 주 네비게이션 -->
    <div class="cd cd--nav">
      <div class="wn">
        <button type="button" class="wn__btn" aria-label="이전 주" @click="$emit('prev-week')">
          <svg class="icon" width="18" height="18" aria-hidden="true">
            <use href="#i-wk-left" />
          </svg>
        </button>
        <span class="wn__label">{{ weekRangeText }}</span>
        <button type="button" class="wn__btn" aria-label="다음 주" @click="$emit('next-week')">
          <svg class="icon" width="18" height="18" aria-hidden="true">
            <use href="#i-wk-right" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 7 요일 카드 -->
    <button
      v-for="day in days"
      :key="day.workYmd"
      type="button"
      class="dc"
      :class="dayCardClass(day)"
      @click="$emit('select-day', day)"
    >
      <span class="dp">
        <span class="dp__dow" :class="dowToneClass(day.dayOfWeek)">{{
          dowLabel(day.dayOfWeek)
        }}</span>
        <span class="dp__num">{{ dayNumber(day.workYmd) }}</span>
      </span>
      <span class="db">
        <span class="db__title">
          <span v-if="day.isToday" class="bd bd-p">오늘</span>
          <span v-else-if="day.isTwoSlot" class="bd bd-i">2구간</span>
          <span v-else-if="isLeave(day)" class="bd bd-w">연차</span>
          <span v-else-if="isWorkDay(day)" class="bd bd-n">{{ planShortCode(day) }}</span>
          <span v-if="day.holidayName" class="bd bd-h">{{ day.holidayName }}</span>
          <!-- 초과근무 있는 날 작은 배지(info 톤). overtimeMinutes>0 일 때만. -->
          <span v-if="hasOvertime(day)" class="bd bd-ot">초과</span>
          <span class="db__name" :class="{ 'db__name--muted': isOffDay(day) }">{{
            planTitleText(day)
          }}</span>
        </span>
        <!-- prafta-app-018-E: 연차일은 부분연차 마커(plain text). 근무/휴무는 기존 시간요약(v-html).
             같은 날 시간차/반차 다건이면 각 건을 1줄씩 표시.
             PRAFTA_COM_002-B-1: 승인 대기(요청중) 연차는 마커 옆에 "요청중" 배지 부가(표시/색은 유지). -->
        <template v-if="isLeave(day)">
          <span v-for="(marker, idx) in leaveMarkers(day)" :key="idx" class="db__summary">
            {{ marker.text }}
            <span v-if="marker.pending" class="bd bd-w lv-pending-bd">요청중</span>
          </span>
        </template>
        <span v-else-if="summaryHtml(day)" class="db__summary" v-html="summaryHtml(day)"></span>
        <!-- 초과근무 보조 요약 라인 — "· 초과 N시간 M분"(요약과 별도 줄, info 톤). -->
        <span v-if="overtimeSummaryText(day)" class="db__ot">{{ overtimeSummaryText(day) }}</span>
      </span>
      <svg class="icon dc__chev" width="16" height="16" aria-hidden="true">
        <use href="#i-wk-right" />
      </svg>
    </button>

    <!-- 주 합계 -->
    <div class="ws">
      <p class="ws__title">이번주 합계 <span class="ws__note">완료된 근무만</span></p>
      <div class="ws__grid">
        <div class="ws__item">
          <p class="ws__lbl">예정 근로시간</p>
          <p class="ws__val">{{ plannedText }}</p>
        </div>
        <div class="ws__item ws__item--actual">
          <p class="ws__lbl">실 근로시간</p>
          <p class="ws__val">{{ actualText }}</p>
        </div>
      </div>
      <!-- 주 초과근무 합계 — 있을 때만 보조 1줄(과하지 않게). -->
      <p v-if="weekOvertimeText" class="ws__ot">초과근무 {{ weekOvertimeText }}</p>
    </div>

    <!-- sprite -->
    <svg width="0" height="0" class="week-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-wk-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol
          id="i-wk-right"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="9 18 15 12 9 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  dowShort,
  dowKey,
  dayNumber as fmtDayNumber,
  minutesToHhMm,
  formatLeaveMarkers,
  formatTimeSummary,
  formatOvertimeMinutes,
} from '../attdFormat'
import { formatYmdDisplay, formatMdDot } from '@/utils/approvalFormat'

const props = defineProps({
  // GET /api/app/attd/my/week 응답. null=로딩/미주입
  week: {
    type: Object,
    default: null,
  },
})

defineEmits(['prev-week', 'next-week', 'select-day'])

const days = computed(() => (props.week && props.week.days) || [])
const summary = computed(() => (props.week && props.week.summary) || {})

// 주 범위 — "2026.05.18 ~ 05.24"
const weekRangeText = computed(() => {
  const start = (props.week && props.week.weekStartYmd) || ''
  const end = (props.week && props.week.weekEndYmd) || ''
  if (String(start).length < 8 || String(end).length < 8) return ''
  // 표시 통일(D1): 시작=YYYY.MM.DD, 종료=MM.DD
  return `${formatYmdDisplay(start)} ~ ${formatMdDot(end)}`
})

// 요일 라벨/색
const dowLabel = (dow) => dowShort(dow)
const dowToneClass = (dow) => {
  const k = dowKey(dow)
  if (k === 'SAT') return 'dp__dow--sat'
  if (k === 'SUN') return 'dp__dow--sun'
  return ''
}
const dayNumber = (ymd) => fmtDayNumber(ymd)

// 일자 분류 (표시용)
const isLeave = (day) => !!day.isLeaveUsed
const isWorkDay = (day) => !!day.workPlanCode && !isLeave(day) && !!day.scheduleSummary
const isOffDay = (day) => !day.scheduleSummary && !isLeave(day)

const dayCardClass = (day) => {
  const cls = []
  if (day.isToday) cls.push('dc--today')
  if (isLeave(day)) cls.push('dc--leave')
  return cls
}

// 근무코드 짧은 라벨 / 제목
//   workPlanName 표기는 "ST001 정규근무" 형태(첫 토큰=코드, 나머지=근무명).
//   별도 workPlanCode 필드가 오면 우선 사용.
const planShortCode = (day) => {
  if (day.workPlanCode) return day.workPlanCode
  const name = String(day.workPlanName || '').trim()
  if (!name) return ''
  return name.split(/\s+/)[0]
}
const planTitleText = (day) => {
  // 연차: 연차 종류명(시안: 배지로 처리되나 텍스트 보조)
  if (isLeave(day)) return day.leaveTypeName || '연차'
  // 휴무: 칩/배지 금지, 텍스트만. 공휴일 겹치면 "휴무 · {holidayName}"
  if (isOffDay(day)) {
    return day.holidayName ? `휴무 · ${day.holidayName}` : '휴무'
  }
  // 근무: 코드부를 제거한 근무명만 (배지로 코드 별도 표기)
  const name = String(day.workPlanName || '').trim()
  const code = planShortCode(day)
  if (code && name.startsWith(code)) {
    return name.slice(code.length).trim()
  }
  return name
}

// 근태 요약 톤 — attendanceStatus 기준. 정상/근무중=.ac, 지각/조퇴/이상=.wr
const attendanceToneClass = (status) => {
  switch (status) {
    case 'LATE':
    case 'EARLY_LEAVE':
    case 'MISSING':
      return 'wr'
    case 'NORMAL':
    case 'WORKING':
    default:
      return 'ac'
  }
}
// 근태 요약 접미 라벨 (지각/조퇴) — 시안 "근태 09:42~18:02 (지각)"
const attendanceSuffix = (status) => {
  if (status === 'LATE') return ' (지각)'
  if (status === 'EARLY_LEAVE') return ' (조퇴)'
  return ''
}

// 시간 요약 HTML — "스케줄 0930~1800 · 근태 0925~1808"
//   근태 색: 정상=.ac(primary), 지각/이상=.wr(warning). 미래=근태 미표시.
//   휴무/연차는 빈 문자열 반환(요약 없음).
const summaryHtml = (day) => {
  if (isLeave(day) || isOffDay(day)) return ''
  if (!day.scheduleSummary) return ''
  let html = `<span class="lbl">스케줄</span> ${formatTimeSummary(day.scheduleSummary)}`
  // 근태 요약은 생성된 경우만(미래/미생성=null)
  if (day.attendanceSummary) {
    const tone = attendanceToneClass(day.attendanceStatus)
    const suffix = attendanceSuffix(day.attendanceStatus)
    html += ` · <span class="${tone}">근태 ${formatTimeSummary(day.attendanceSummary)}${suffix}</span>`
  }
  return html
}

// prafta-app-018-E: 연차일 마커 — "월차 · 시간차 · 03:00~04:30 · 0.19일"(종일=라벨만).
//   같은 날 시간차/반차 다건이면 각 건을 1줄씩(day.leaves[]). 구버전 응답은 단건 스칼라 폴백.
const leaveMarkers = (day) => formatLeaveMarkers(day)

// 승인된 초과근무 — day.overtimeMinutes(0 가능)/day.overtimes[]. 0건이면 배지/요약 모두 미표기.
const hasOvertime = (day) => Number(day && day.overtimeMinutes) > 0
const overtimeSummaryText = (day) => {
  const txt = formatOvertimeMinutes(day && day.overtimeMinutes)
  return txt ? `· 초과 ${txt}` : ''
}
// 주 초과 합계 — 각 일자 overtimeMinutes 합산(서버 합계 필드 미제공 가정, 표시 전용).
const weekOvertimeText = computed(() => {
  const total = days.value.reduce((sum, d) => sum + (Number(d && d.overtimeMinutes) || 0), 0)
  return formatOvertimeMinutes(total)
})

// 합계 — "Nh Nm"
const plannedText = computed(() => minutesToHhMm(summary.value.plannedWorkMinutes))
const actualText = computed(() => minutesToHhMm(summary.value.actualWorkMinutes))
</script>

<style scoped>
.week-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cd {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.cd--nav {
  padding: 10px 14px;
}
.wn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.wn__btn {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-family: inherit;
}

/* 요일 카드 */
.dc {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
}
.dc--today {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
}
.dc--leave {
  background: var(--color-warning-tint);
  border-color: var(--color-warning-border-light);
}

.dp {
  display: flex;
  align-items: baseline;
  gap: 5px;
  min-width: 44px;
  flex-shrink: 0;
}
.dp__dow {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.dp__dow--sat {
  color: var(--color-info);
}
.dp__dow--sun {
  color: var(--color-danger);
}
.dp__num {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.dc--today .dp__num {
  color: var(--color-primary);
}

.db {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.db__title {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
  margin-bottom: 2px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.db__name--muted {
  color: var(--color-text-secondary);
  font-weight: 500;
}
.db__summary {
  font-size: 11px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  font-variant-numeric: tabular-nums;
}
/* 초과근무 보조 요약 라인 — info 톤, 시간요약 바로 아래. */
.db__ot {
  font-size: 11px;
  color: var(--color-info);
  font-weight: 600;
  line-height: 1.5;
  font-variant-numeric: tabular-nums;
}
.dc__chev {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}

/* 배지 (요일 카드 내) */
.bd {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}
.bd-p {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.bd-n {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
.bd-i {
  background: var(--color-info-tint);
  color: var(--color-info);
}
.bd-w {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.bd-h {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
/* 초과근무 배지 — info 톤(연차 warning / 2구간 info 와 톤 통일하되 라벨로 구분). */
.bd-ot {
  background: var(--color-info-tint);
  color: var(--color-info);
}
/* PRAFTA_COM_002-B-1: 연차 마커 옆 "요청중" 배지(기존 .bd .bd-w 톤 재사용). 마커 텍스트와 간격/정렬. */
.lv-pending-bd {
  margin-left: var(--space-xs);
  vertical-align: middle;
}

/* deep — 시간요약 v-html 색상 */
.db__summary :deep(.lbl) {
  color: var(--color-text-tertiary);
  font-weight: 500;
}
.db__summary :deep(.ac) {
  color: var(--color-primary);
  font-weight: 600;
}
.db__summary :deep(.wr) {
  color: var(--color-warning-text);
  font-weight: 600;
}
.db__summary :deep(.mu) {
  color: var(--color-text-tertiary);
}

/* 주 합계 */
.ws {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}
.ws__title {
  margin: 0 0 var(--space-sm);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.ws__note {
  font-weight: 400;
  font-size: 10px;
  color: var(--color-text-tertiary);
}
.ws__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.ws__lbl {
  margin: 0;
  font-size: 11px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.ws__val {
  margin: 2px 0 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.ws__item--actual .ws__val {
  color: var(--color-primary);
}
/* 주 초과근무 합계 — 합계 그리드 아래 보조 1줄(info 톤). */
.ws__ot {
  margin: 10px 0 0;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-info);
  font-variant-numeric: tabular-nums;
}

.week-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
