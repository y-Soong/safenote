<!--
  MonthCalendarBase.vue — 월 캘린더 공통 베이스 (그리드/셀 6주 42칸 계산 + outside 처리)
  - 목적: 앱 월 캘린더 2종(AttendanceMonthCalendar / LeavePromotionCalendar)의
    중복된 6주(42칸) 그리드 계산을 단일화한다.
  - 책임 범위(공통):
      · yearMonth(YYYYMM) 기준 42칸 셀 배열 생성(당월 + 인접월 outside).
      · 주 시작 = 일요일. 각 셀 = { ymd, dayNum, isOutside, dow('SUN'~'SAT') }.
      · 7열 그리드 컨테이너 렌더 + 셀별 scoped slot 제공.
  - 책임 밖(각 부모가 유지): 월 네비/라벨/범례/합계, 요일 헤더의 시각 표현,
    셀의 색상/선택/토글/마커 등 도메인 표현. (호출부 0수정 목표 — A/B 외부 props/emit 불변)
  - outside 셀도 실제 인접월 일자(dayNum/ymd)를 채워 제공한다.
    A는 outside 를 비활성 표시, B는 visibility:hidden 으로 각자 처리한다(베이스는 표현 미관여).
-->
<template>
  <div class="mcb-grid" :class="gridClass">
    <slot
      v-for="(cell, idx) in cells"
      name="cell"
      :cell="cell"
      :index="idx"
      :is-outside="cell.isOutside"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 표시 중인 연/월 (YYYYMM)
  yearMonth: { type: String, default: '' },
  // 그리드 컨테이너에 추가할 부모측 클래스(스코프 스타일 적용용)
  gridClass: { type: [String, Array, Object], default: '' },
})

const DOW_KEY = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

// 'YYYYMMDD' 로컬 조립
const toYmd = (date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}${m}${d}`
}

// 6주(42칸) 셀 배열. 주 시작=일요일. 앞/뒤 인접월은 isOutside=true(실제 일자 채움).
const cells = computed(() => {
  const ym = String(props.yearMonth || '')
  if (ym.length < 6) return []
  const year = Number(ym.slice(0, 4))
  const month = Number(ym.slice(4, 6)) // 1~12
  if (!year || month < 1 || month > 12) return []

  const first = new Date(year, month - 1, 1)
  const startOffset = first.getDay() // 0=일 ~ 6=토 (앞쪽 빈칸 수)
  const daysInMonth = new Date(year, month, 0).getDate()

  const makeCell = (date, isOutside) => ({
    ymd: toYmd(date),
    dayNum: String(date.getDate()),
    isOutside,
    dow: DOW_KEY[date.getDay()],
  })

  const result = []
  // 1) 앞쪽 인접월(이전 달 말일들)
  for (let i = startOffset; i > 0; i -= 1) {
    result.push(makeCell(new Date(year, month - 1, 1 - i), true))
  }
  // 2) 당월 일자
  for (let d = 1; d <= daysInMonth; d += 1) {
    result.push(makeCell(new Date(year, month - 1, d), false))
  }
  // 3) 뒤쪽 인접월(다음 달 초)을 42칸까지 채움
  let nextDay = 1
  while (result.length < 42) {
    result.push(makeCell(new Date(year, month, nextDay), true))
    nextDay += 1
  }
  return result
})
</script>

<style scoped>
/* 그리드 레이아웃은 부모가 gridClass 로 세부 조정(gap 등). 베이스는 7열만 보장. */
.mcb-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}
</style>
