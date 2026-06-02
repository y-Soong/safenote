<!--
  AttendanceMonthCalendar.vue — 이번달 캘린더 (셀 색상 코딩)
  - 작업 ID: APP002-08 (UI 명세: UI-A004)
  - 시안 화면 9·10: 월 네비 + 월 합계 + 범례 + 7열 캘린더
  - 정책: 시안 §2.3·§4.4, attd §13.3(처리필요 근거), §8.2(연차)
  - 셀 색상: wk(근무)/lv(연차)/of(휴무)/ac(처리필요) + out/td/sel
  - 셀 선택 → select-date emit (부모가 day-detail 조회)
-->
<template>
  <div v-if="month" class="cd month-cal">
    <!-- 월 네비 -->
    <div class="mn">
      <div class="mn__nav">
        <button type="button" class="mn__btn" aria-label="이전 달" @click="$emit('prev-month')">
          <svg class="icon" width="18" height="18" aria-hidden="true"><use href="#i-mo-left" /></svg>
        </button>
        <span class="mn__label">{{ monthLabel }}</span>
        <button type="button" class="mn__btn" aria-label="다음 달" @click="$emit('next-month')">
          <svg class="icon" width="18" height="18" aria-hidden="true"><use href="#i-mo-right" /></svg>
        </button>
      </div>
    </div>

    <!-- 월 합계 -->
    <div class="mn__sum">
      예정 <strong>{{ plannedText }}</strong> · 실 <strong>{{ actualText }}</strong>
      <span class="mn__sum-note">(완료분)</span>
    </div>

    <!-- 범례 -->
    <div class="lgd">
      <span class="lgd__item"><span class="lgd__dot lgd__dot--wk" />근무</span>
      <span class="lgd__item"><span class="lgd__dot lgd__dot--lv" />연차</span>
      <span class="lgd__item"><span class="lgd__dot lgd__dot--of" />휴무</span>
      <span class="lgd__item"><span class="lgd__dot lgd__dot--ac" />처리 필요</span>
    </div>

    <!-- 캘린더 그리드 -->
    <div class="cal">
      <div class="cal__h cal__h--sun">일</div>
      <div class="cal__h">월</div>
      <div class="cal__h">화</div>
      <div class="cal__h">수</div>
      <div class="cal__h">목</div>
      <div class="cal__h">금</div>
      <div class="cal__h cal__h--sat">토</div>

      <button
        v-for="(cell, idx) in cells"
        :key="idx"
        type="button"
        class="cal__d"
        :class="cellClass(cell)"
        :disabled="cell.isOutside"
        @click="onSelectCell(cell)"
      >
        <span class="cal__n">{{ cell.dayNum }}</span>
        <span v-if="hasMarker(cell)" class="cal__mk" />
        <span v-if="cell.dayType === 'ACTION_REQUIRED' && !isSelected(cell)" class="cal__alert" />
      </button>
    </div>

    <!-- sprite -->
    <svg width="0" height="0" class="mo-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-mo-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6" /></symbol>
        <symbol id="i-mo-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6" /></symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatYearMonth, minutesToHhMm, dateToYmd, dayNumber as fmtDayNumber } from '../attdFormat'

const props = defineProps({
  // GET /api/app/attd/my/month 응답. null=로딩/미주입
  month: {
    type: Object,
    default: null,
  },
  // 현재 선택된 일자 (YYYYMMDD)
  selectedYmd: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['prev-month', 'next-month', 'select-date'])

const monthSummary = computed(() => (props.month && props.month.monthlySummary) || {})

// 월 라벨 — "2026년 5월"
const monthLabel = computed(() => formatYearMonth(props.month && props.month.yearMonth))

// 합계 — "163h 30m"
const plannedText = computed(() => minutesToHhMm(monthSummary.value.plannedWorkMinutes))
const actualText = computed(() => minutesToHhMm(monthSummary.value.actualWorkMinutes))

// ───────────────────────────────────────────────────────────
// 캘린더 셀 구성 (6주 × 7일 = 42칸. 인접월은 isOutside)
//   month.yearMonth + month.days[] → 42칸 cell 배열 생성.
//   각 cell: { ymd, dayNum, dayType, holidayName, hasIssue, dow, isToday, isOutside }
//   주 시작=일요일(시안 캘린더 헤더 일~토 순서).
// ───────────────────────────────────────────────────────────
const DOW_KEY = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
const todayYmd = dateToYmd(new Date())

// 서버 days[] 를 ymd → day 매핑으로 인덱싱
const dayMap = computed(() => {
  const map = new Map()
  const list = (props.month && props.month.days) || []
  list.forEach((d) => {
    if (d && d.workYmd) map.set(String(d.workYmd), d)
  })
  return map
})

// out 셀 1칸 생성 (인접월)
const makeOutsideCell = (date) => ({
  ymd: dateToYmd(date),
  dayNum: String(date.getDate()),
  isOutside: true,
})

const cells = computed(() => {
  const ym = String((props.month && props.month.yearMonth) || '')
  if (ym.length < 6) return []
  const year = Number(ym.slice(0, 4))
  const month = Number(ym.slice(4, 6)) // 1~12

  const first = new Date(year, month - 1, 1)
  const startOffset = first.getDay() // 0=일 ~ 6=토 (그리드 앞 빈칸 수)
  const daysInMonth = new Date(year, month, 0).getDate()

  const result = []

  // 1) 앞쪽 인접월(이전 달 말일들)
  for (let i = startOffset; i > 0; i -= 1) {
    const d = new Date(year, month - 1, 1 - i)
    result.push(makeOutsideCell(d))
  }

  // 2) 당월 일자
  for (let d = 1; d <= daysInMonth; d += 1) {
    const date = new Date(year, month - 1, d)
    const ymd = dateToYmd(date)
    const serverDay = dayMap.value.get(ymd) || {}
    result.push({
      ymd,
      dayNum: fmtDayNumber(ymd),
      dayType: serverDay.dayType || 'OFF',
      holidayName: serverDay.holidayName || '',
      hasIssue: !!serverDay.hasIssue,
      dow: DOW_KEY[date.getDay()],
      isToday: ymd === todayYmd,
      isOutside: false,
    })
  }

  // 3) 뒤쪽 인접월(다음 달 초)을 42칸까지 채움
  let nextDay = 1
  while (result.length < 42) {
    const date = new Date(year, month, nextDay)
    result.push(makeOutsideCell(date))
    nextDay += 1
  }

  return result
})

// 셀 클래스 — 색상 코딩 (시안 §4.4.2)
const cellClass = (cell) => {
  const cls = []
  if (cell.isOutside) {
    cls.push('cal__d--out')
    return cls
  }
  switch (cell.dayType) {
    case 'WORK':
      cls.push('cal__d--wk')
      break
    case 'LEAVE':
      cls.push('cal__d--lv')
      break
    case 'OFF':
      cls.push('cal__d--of')
      break
    case 'ACTION_REQUIRED':
      // 처리필요는 근무 톤 위에 붉은 강조 (시안: .wk.ac 조합 가능)
      cls.push('cal__d--wk', 'cal__d--ac')
      break
    default:
      break
  }
  if (cell.dow === 'SAT') cls.push('cal__d--sat')
  if (cell.dow === 'SUN') cls.push('cal__d--sun')
  if (cell.isToday) cls.push('cal__d--td')
  if (isSelected(cell)) cls.push('cal__d--sel')
  return cls
}

const isSelected = (cell) => !cell.isOutside && cell.ymd === props.selectedYmd
const hasMarker = (cell) =>
  !cell.isOutside && (cell.dayType === 'WORK' || cell.dayType === 'LEAVE' || cell.dayType === 'ACTION_REQUIRED')

const onSelectCell = (cell) => {
  if (cell.isOutside) return
  emit('select-date', cell.ymd)
}
</script>

<style scoped>
.cd {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 12px 14px;
}

.mn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2px;
}
.mn__nav {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.mn__btn {
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
.mn__sum {
  font-size: 11px;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}
.mn__sum strong {
  color: var(--color-primary);
  font-weight: 700;
}
.mn__sum-note {
  color: var(--color-text-tertiary);
}

/* 범례 */
.lgd {
  display: flex;
  gap: var(--space-md);
  flex-wrap: wrap;
  margin: var(--space-sm) 0 var(--space-xs);
  font-size: 10px;
  color: var(--color-text-secondary);
}
.lgd__item {
  display: inline-flex;
  align-items: center;
}
.lgd__dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  margin-right: var(--space-xs);
}
.lgd__dot--wk {
  background: var(--color-primary-tint);
  border: 1px solid var(--color-primary-tint-border);
}
.lgd__dot--lv {
  background: var(--color-warning-tint);
  border: 1px solid var(--color-warning-border-light);
}
.lgd__dot--of {
  background: var(--color-attd-cell-off);
  border: 1px solid var(--color-border-light);
}
.lgd__dot--ac {
  background: var(--color-danger-tint);
  border: 1px solid var(--color-danger-border);
}

/* 캘린더 그리드 */
.cal {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 3px;
  margin-top: var(--space-xs);
}
.cal__h {
  text-align: center;
  font-size: 10px;
  font-weight: 600;
  color: var(--color-text-secondary);
  padding: var(--space-xs) 0;
}
.cal__h--sat {
  color: var(--color-info);
}
.cal__h--sun {
  color: var(--color-danger);
}

.cal__d {
  aspect-ratio: 1 / 1;
  padding: var(--space-xs) 2px 2px;
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 2px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  cursor: pointer;
  position: relative;
  font-family: inherit;
}
.cal__n {
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.cal__mk {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.2);
}

.cal__d--sat .cal__n {
  color: var(--color-info);
}
.cal__d--sun .cal__n {
  color: var(--color-danger);
}

.cal__d--wk {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
}
.cal__d--wk .cal__n {
  color: var(--color-primary-text-darkest);
}
.cal__d--wk .cal__mk {
  background: var(--color-primary);
}
.cal__d--lv {
  background: var(--color-warning-tint);
  border-color: var(--color-warning-border-light);
}
.cal__d--lv .cal__n {
  color: var(--color-warning-text);
}
.cal__d--lv .cal__mk {
  background: var(--color-warning);
}
.cal__d--of {
  background: var(--color-attd-cell-off);
  border-color: var(--color-border-light);
}
.cal__d--of .cal__n {
  color: var(--color-text-secondary);
}

.cal__d--ac {
  background: var(--color-danger-tint);
  border-color: var(--color-danger-border);
}
.cal__d--ac .cal__n {
  color: var(--color-danger);
}
.cal__d--ac .cal__mk {
  background: var(--color-danger);
}
.cal__alert {
  position: absolute;
  top: 3px;
  right: 3px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-danger);
}

.cal__d--out {
  background: transparent;
  border-color: transparent;
  cursor: default;
}
.cal__d--out .cal__n {
  color: var(--color-text-tertiary);
  font-weight: 400;
}

/* 오늘 (outline) */
.cal__d--td {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}

/* 선택 (진한 배경 + 흰 글씨) */
.cal__d--sel {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.cal__d--sel .cal__n {
  color: var(--color-surface);
}
.cal__d--sel .cal__mk {
  background: rgba(255, 255, 255, 0.6);
}
.cal__d--sel.cal__d--lv {
  background: var(--color-warning);
  border-color: var(--color-warning);
}
.cal__d--sel.cal__d--of {
  background: var(--color-text-secondary);
  border-color: var(--color-text-secondary);
}
.cal__d--sel.cal__d--ac {
  background: var(--color-danger);
  border-color: var(--color-danger);
}

.mo-sprite {
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
