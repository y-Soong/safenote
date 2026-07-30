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
    <!-- 요일 헤더(시각 표현은 B 고유 유지) -->
    <div class="lpc__grid lpc__head">
      <div class="lpc__h lpc__h--sun">일</div>
      <div class="lpc__h">월</div>
      <div class="lpc__h">화</div>
      <div class="lpc__h">수</div>
      <div class="lpc__h">목</div>
      <div class="lpc__h">금</div>
      <div class="lpc__h lpc__h--sat">토</div>
    </div>

    <!-- 6주 42칸 그리드 계산은 공통 베이스에 위임. 토글 선택/마커는 B 고유. -->
    <MonthCalendarBase :year-month="ym" grid-class="lpc__grid">
      <template #cell="{ cell }">
        <button
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
      </template>
    </MonthCalendarBase>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance } from 'vue'

import { formatYmDot } from '@/utils/approvalFormat'
import MonthCalendarBase from '@/components/common/MonthCalendarBase.vue'

const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

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
  // 선택 가능 최대 일수(미지정 잔여 연차) — null 이면 상한 없음
  maxCount: {
    type: Number,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue', 'prev-month', 'next-month'])

// YYYYMM → "YYYY.MM" (표시 단일 출처 위임, D1)
const monthLabel = computed(() => formatYmDot(props.ym))

// 6주 42칸 그리드 계산은 MonthCalendarBase 에 위임(베이스 셀 = { ymd, dayNum, isOutside, dow }).
//   outside 셀은 베이스가 실제 인접월 일자를 채우지만, B 는 lpc__cell--out(visibility:hidden)로 숨긴다.

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
  if (i >= 0) {
    next.splice(i, 1)
  } else {
    // 미지정 잔여 연차 개수를 넘는 날짜 지정 차단(해제는 항상 허용).
    if (props.maxCount != null && next.length >= props.maxCount) {
      showAlert(`미지정 잔여 연차 ${props.maxCount}일까지만 선택할 수 있습니다.`)
      return
    }
    next.push(cell.ymd)
  }
  emit('update:modelValue', next)
}
</script>

<style scoped>
/* com-014-7 F1: 색상/간격 토큰 리터럴 재정의 제거 → 호스트(LeavePromotionPlanView)가 제공하는
   전역 토큰을 그대로 소비(A 패턴과 동일). 폴백 리터럴은 단독 렌더 대비 최소만 유지. */
.lpc {
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
/* 셀 그리드(MonthCalendarBase 루트)는 헤더와 그리드 간격(2px)만 띄움 */
.lpc__grid:not(.lpc__head) {
  margin-top: 2px;
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
