<!--
  LeavePromotionCalendar.vue — 연차 사용촉진 계획서 캘린더 (모바일 앱, 신규)
  - 작업 ID: prafta-com-008-A-7 (UI 명세: UI-app-008-A-1)
  - 정책 출처: 작업지시서 §2-2(캘린더 1일 단위), §0-5(근무 스케줄 있는 평일만)
  - 참조 패턴: views/attd/components/AttendanceMonthCalendar.vue (월 네비 + 7열 그리드 + 셀 선택)
  - 동작: 선택 가능한 날(근무 평일·휴일/주말/기존연차 아님)만 토글 선택. 선택분은 부모로 emit.
  - planner 라운드: template + scoped style 완성, script 는 props/emits/computed 선언 + TODO.
  - developer 라운드:
      · selectableYmds / existingLeaveYmds 는 부모(A-3 active 응답)로부터 props.
      · cells 계산(달력 6주 그리드), selectedYmds 토글, 월 네비 emit 본문 채움.
-->
<template>
  <div class="lpc">
    <!-- 월 네비 -->
    <div class="lpc__nav">
      <button type="button" class="lpc__nav-btn" aria-label="이전 달" @click="$emit('prev-month')">
        ‹
      </button>
      <span class="lpc__nav-label">{{ monthLabel }}</span>
      <button type="button" class="lpc__nav-btn" aria-label="다음 달" @click="$emit('next-month')">
        ›
      </button>
    </div>

    <!-- 범례 -->
    <div class="lpc__legend">
      <span class="lpc__lg"><i class="lpc__dot lpc__dot--sel" />선택</span>
      <span class="lpc__lg"><i class="lpc__dot lpc__dot--exist" />이미 연차</span>
      <span class="lpc__lg"><i class="lpc__dot lpc__dot--off" />선택 불가</span>
    </div>

    <!-- 캘린더 그리드 -->
    <div class="lpc__grid">
      <div class="lpc__h lpc__h--sun">일</div>
      <div class="lpc__h">월</div>
      <div class="lpc__h">화</div>
      <div class="lpc__h">수</div>
      <div class="lpc__h">목</div>
      <div class="lpc__h">금</div>
      <div class="lpc__h lpc__h--sat">토</div>

      <button
        v-for="(cell, idx) in cells"
        :key="idx"
        type="button"
        class="lpc__cell"
        :class="cellClass(cell)"
        :disabled="cell.isOutside || !isSelectable(cell)"
        @click="onToggle(cell)"
      >
        <span class="lpc__day">{{ cell.dayNum }}</span>
        <span v-if="isExisting(cell)" class="lpc__mk lpc__mk--exist" />
        <span v-else-if="isSelected(cell)" class="lpc__mk lpc__mk--sel" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 표시 중인 연/월 (YYYYMM)
  ym: {
    type: String,
    default: '',
  },
  // 선택 가능한 날짜 목록 (YYYYMMDD) — 근무 평일·휴일/주말 아님·만료 이내 (A-3 가공값)
  selectableYmds: {
    type: Array,
    default: () => [],
  },
  // 이미 CONFIRMED 로 등록된 연차일 (YYYYMMDD) — 토글 불가, 표시만
  existingLeaveYmds: {
    type: Array,
    default: () => [],
  },
  // 현재 선택(계획)한 날짜 (YYYYMMDD) — v-model
  modelValue: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'prev-month', 'next-month'])

// YYYYMM → "YYYY.MM"
const monthLabel = computed(() => {
  const v = props.ym
  if (!v || v.length !== 6) return ''
  return `${v.slice(0, 4)}.${v.slice(4, 6)}`
})

// 달력 셀 6주 그리드(7×6=42칸). props.ym(YYYYMM) 기준 1일 요일~말일까지 채우고,
//   앞뒤 빈칸은 isOutside=true(렌더는 숨김). 각 cell = { ymd, dayNum, isOutside, dow }.
const cells = computed(() => {
  const ym = props.ym
  if (!ym || ym.length !== 6) return []
  const year = Number(ym.slice(0, 4))
  const month = Number(ym.slice(4, 6)) // 1~12
  const first = new Date(year, month - 1, 1)
  const firstDow = first.getDay() // 0=일 ~ 6=토
  const daysInMonth = new Date(year, month, 0).getDate() // 말일

  const list = []
  // 1일 앞쪽 빈칸(이전 달 자리)
  for (let i = 0; i < firstDow; i += 1) {
    list.push({ ymd: '', dayNum: 0, isOutside: true, dow: i })
  }
  // 당월 일자
  for (let d = 1; d <= daysInMonth; d += 1) {
    const dt = new Date(year, month - 1, d)
    const ymd = `${year}${String(month).padStart(2, '0')}${String(d).padStart(2, '0')}`
    list.push({ ymd, dayNum: d, isOutside: false, dow: dt.getDay() })
  }
  // 6주(42칸) 채우기 — 뒤쪽 빈칸(다음 달 자리)
  while (list.length % 7 !== 0 || list.length < 42) {
    list.push({ ymd: '', dayNum: 0, isOutside: true, dow: list.length % 7 })
    if (list.length >= 42) break
  }
  return list
})

const isSelectable = (cell) => props.selectableYmds.includes(cell?.ymd)
const isExisting = (cell) => props.existingLeaveYmds.includes(cell?.ymd)
const isSelected = (cell) => props.modelValue.includes(cell?.ymd)

const cellClass = (cell) => ({
  'lpc__cell--out': cell?.isOutside,
  'lpc__cell--exist': isExisting(cell),
  'lpc__cell--sel': isSelected(cell),
  'lpc__cell--off': !cell?.isOutside && !isSelectable(cell) && !isExisting(cell),
})

// 셀 토글 — 선택/해제 후 v-model emit. (이미 연차/선택불가 셀은 disabled 라 진입 안 함)
const onToggle = (cell) => {
  if (!cell || cell.isOutside || !isSelectable(cell) || isExisting(cell)) return
  const next = [...props.modelValue]
  const i = next.indexOf(cell.ymd)
  if (i >= 0) next.splice(i, 1)
  else next.push(cell.ymd)
  emit('update:modelValue', next)
}
</script>

<style scoped>
.lpc {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-danger: #ef4444;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --radius-md: 10px;
  --space-sm: 8px;
  --space-md: 12px;

  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  color: var(--color-text-primary);
}

.lpc__nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}
.lpc__nav-btn {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
  font-size: 20px;
  cursor: pointer;
}
.lpc__nav-label {
  font-size: 15px;
  font-weight: 600;
}

.lpc__legend {
  display: flex;
  justify-content: center;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}
.lpc__lg {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--color-text-secondary);
}
.lpc__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.lpc__dot--sel {
  background: var(--color-primary);
}
.lpc__dot--exist {
  background: var(--color-warning);
}
.lpc__dot--off {
  background: var(--color-border);
}

.lpc__grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}
.lpc__h {
  text-align: center;
  font-size: 11px;
  color: var(--color-text-tertiary);
  padding: 4px 0;
}
.lpc__h--sun {
  color: var(--color-danger);
}
.lpc__h--sat {
  color: var(--color-primary);
}

.lpc__cell {
  position: relative;
  aspect-ratio: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border-light);
  border-radius: 6px;
  color: var(--color-text-primary);
  font-size: 13px;
  cursor: pointer;
}
.lpc__cell:disabled {
  cursor: default;
}
.lpc__cell--out {
  visibility: hidden;
}
.lpc__cell--off {
  color: var(--color-text-tertiary);
  background: var(--color-bg, #f9fafb);
}
.lpc__cell--sel {
  background: var(--color-primary-tint);
  border-color: var(--color-primary);
}
.lpc__cell--exist {
  background: var(--color-warning-tint);
  border-color: var(--color-warning);
}
.lpc__day {
  line-height: 1;
}
.lpc__mk {
  position: absolute;
  bottom: 4px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
}
.lpc__mk--sel {
  background: var(--color-primary);
}
.lpc__mk--exist {
  background: var(--color-warning);
}
</style>
