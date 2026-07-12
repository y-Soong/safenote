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
          <svg class="icon" width="18" height="18" aria-hidden="true">
            <use href="#i-mo-left" />
          </svg>
        </button>
        <button type="button" class="mn__label" aria-label="연월 선택" @click="openMonthPicker">
          <span>{{ monthLabel }}</span>
          <svg
            class="mn__label-ic"
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>
        <button type="button" class="mn__btn" aria-label="다음 달" @click="$emit('next-month')">
          <svg class="icon" width="18" height="18" aria-hidden="true">
            <use href="#i-mo-right" />
          </svg>
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
      <!-- PRAFTA_COM_002-B-2-2: 셀 우상단 빨간점(cal__alert)과 붉은 셀(cal__d--ac)은 같은 데이터(ACTION_REQUIRED).
           파란점("초과")과 동일한 점-형태 범례로 통합 안내(중복 사각 범례 제거). -->
      <span class="lgd__item"><span class="lgd__alert" />처리 필요(미보정)</span>
      <span class="lgd__item"><span class="lgd__ot" />초과</span>
    </div>

    <!-- 캘린더 그리드 -->
    <!-- 요일 헤더(시각 표현은 A 고유 유지) -->
    <div class="cal cal__head">
      <div class="cal__h cal__h--sun">일</div>
      <div class="cal__h">월</div>
      <div class="cal__h">화</div>
      <div class="cal__h">수</div>
      <div class="cal__h">목</div>
      <div class="cal__h">금</div>
      <div class="cal__h cal__h--sat">토</div>
    </div>

    <!-- 6주 42칸 그리드 계산은 공통 베이스에 위임. 셀 렌더/색상/선택은 A 고유. -->
    <MonthCalendarBase :year-month="currentYearMonth" grid-class="cal">
      <template #cell="{ cell }">
        <button
          type="button"
          class="cal__d"
          :class="cellClass(decorate(cell))"
          :disabled="cell.isOutside"
          @click="onSelectCell(cell)"
        >
          <span class="cal__n">{{ cell.dayNum }}</span>
          <span v-if="hasMarker(decorate(cell))" class="cal__mk" />
          <span
            v-if="dayTypeOf(cell) === 'ACTION_REQUIRED' && !isSelected(cell)"
            class="cal__alert"
          />
          <!-- 초과근무 마커 — overtimeMinutes>0 셀 좌상단 작은 점(처리필요 우상단과 구분). -->
          <span v-if="hasOvertime(cell)" class="cal__ot" />
        </button>
      </template>
    </MonthCalendarBase>

    <!-- 연월 선택 시트 -->
    <MonthPickerSheet
      v-model="showMonthPicker"
      :year-month="currentYearMonth"
      @confirm="onConfirmMonth"
    />

    <!-- sprite -->
    <svg width="0" height="0" class="mo-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-mo-left"
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
          id="i-mo-right"
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
import { computed, ref } from 'vue'
import { formatYearMonth, minutesToHhMm, dateToYmd } from '../attdFormat'
import MonthPickerSheet from '@/components/common/MonthPickerSheet.vue'
import MonthCalendarBase from '@/components/common/MonthCalendarBase.vue'

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

const emit = defineEmits(['prev-month', 'next-month', 'select-date', 'select-month'])

const monthSummary = computed(() => (props.month && props.month.monthlySummary) || {})

// 월 라벨 — "2026년 5월"
const monthLabel = computed(() => formatYearMonth(props.month && props.month.yearMonth))

// 연월 선택 시트 ('YYYYMM')
const currentYearMonth = computed(() => String((props.month && props.month.yearMonth) || ''))
const showMonthPicker = ref(false)
const openMonthPicker = () => {
  showMonthPicker.value = true
}
const onConfirmMonth = (yyyymm) => {
  if (yyyymm && yyyymm !== currentYearMonth.value) emit('select-month', yyyymm)
}

// 합계 — "163h 30m"
const plannedText = computed(() => minutesToHhMm(monthSummary.value.plannedWorkMinutes))
const actualText = computed(() => minutesToHhMm(monthSummary.value.actualWorkMinutes))

// ───────────────────────────────────────────────────────────
// 셀 구성: 42칸 그리드 계산은 MonthCalendarBase 가 담당(베이스 셀 = { ymd, dayNum, isOutside, dow }).
//   여기서는 베이스 셀에 서버 days[](dayType/holidayName/hasIssue) + isToday 를 decorate 로 보강한다.
//   주 시작=일요일(시안 캘린더 헤더 일~토 순서).
// ───────────────────────────────────────────────────────────
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

// 베이스 셀(공통) → A 도메인 셀로 보강. 색상/마커/오늘 판정에 필요한 필드만 추가.
const decorate = (cell) => {
  if (cell.isOutside) return cell
  const serverDay = dayMap.value.get(cell.ymd) || {}
  return {
    ...cell,
    dayType: serverDay.dayType || 'OFF',
    holidayName: serverDay.holidayName || '',
    hasIssue: !!serverDay.hasIssue,
    isToday: cell.ymd === todayYmd,
  }
}

// 셀의 dayType(보강 전 베이스 셀에서도 호출) — alert dot 판정용.
const dayTypeOf = (cell) => {
  if (cell.isOutside) return ''
  const serverDay = dayMap.value.get(cell.ymd) || {}
  return serverDay.dayType || 'OFF'
}

// 셀의 승인된 초과근무 유무 — overtimeMinutes>0(0/null 이면 마커 없음). 베이스 셀에서 직접 조회.
const hasOvertime = (cell) => {
  if (cell.isOutside) return false
  const serverDay = dayMap.value.get(cell.ymd) || {}
  return Number(serverDay.overtimeMinutes) > 0
}

// 셀 클래스 — 색상 코딩 (시안 §4.4.2). 입력은 decorate 된 셀.
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
  // 웹 휴일관리(TB_HOLIDAY) 등록 휴일 — 날짜를 빨간색으로(일요일과 동일 컨벤션) 강조.
  if (cell.holidayName) cls.push('cal__d--hol')
  if (cell.isToday) cls.push('cal__d--td')
  if (isSelected(cell)) cls.push('cal__d--sel')
  return cls
}

const isSelected = (cell) => !cell.isOutside && cell.ymd === props.selectedYmd
const hasMarker = (cell) =>
  !cell.isOutside &&
  (cell.dayType === 'WORK' || cell.dayType === 'LEAVE' || cell.dayType === 'ACTION_REQUIRED')

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
.mn__label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: 0;
  cursor: pointer;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  padding: 4px 6px;
  border-radius: var(--radius-sm);
}
.mn__label:active {
  background: var(--color-bg);
}
.mn__label-ic {
  color: var(--color-text-tertiary);
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
/* PRAFTA_COM_002-B-2-2: 범례 — 처리 필요(셀 우상단 .cal__alert 빨간점과 동일한 원형 danger 마커).
   파란점(.lgd__ot)과 동일 크기/마진으로 점 형태 통일. 붉은 셀(cal__d--ac)도 같은 ACTION_REQUIRED 를 가리키므로 이 한 항목으로 안내. */
.lgd__alert {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: var(--space-xs);
  background: var(--color-danger);
}
/* 범례 — 초과근무(셀 좌상단 점과 동일한 원형 info 마커). */
.lgd__ot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: var(--space-xs);
  background: var(--color-info);
}

/* 캘린더 그리드(헤더 + 베이스 셀 그리드 공용) */
.cal {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 3px;
}
/* 요일 헤더 행: 블록 상단 여백 */
.cal__head {
  margin-top: var(--space-xs);
}
/* 셀 그리드(MonthCalendarBase 루트)는 헤더와 그리드 간격(3px)만 띄움 */
.cal:not(.cal__head) {
  margin-top: 3px;
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

/* 휴일(웹 휴일관리 등록) — 날짜를 빨간색으로(일/공휴일 컨벤션). dayType 색상 위에 덮어쓴다.
   선택 셀(.cal__d--sel .cal__n, 흰색)은 소스 순서상 뒤라 그대로 흰색 유지. */
.cal__d--hol .cal__n {
  color: var(--color-danger);
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

/* 초과근무 마커 — 좌상단 작은 info 점(처리필요 우상단 .cal__alert 와 위치/색 구분). */
.cal__ot {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-info);
}
/* 선택 셀(진한 배경) 위에서는 흰 점으로 대비 유지. */
.cal__d--sel .cal__ot {
  background: rgba(255, 255, 255, 0.85);
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
