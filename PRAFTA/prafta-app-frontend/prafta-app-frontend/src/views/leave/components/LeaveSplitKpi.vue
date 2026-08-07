<!--
  LeaveSplitKpi.vue — 3분할 KPI (부여 / 사용 / 사용예정)
  - 작업: prafta-app-005 슬롯 F (UI 명세: UI-A005)
  - 시안 §4.5 / 3열 grid. 0값은 muted(text-tertiary)로 노이즈 감소.
  - group: { granted, used, planned }. 표시 전용. 토큰은 부모(.my-leave-view)에서 상속.
-->
<template>
  <div class="split">
    <div v-for="cell in cells" :key="cell.key" class="cell">
      <span class="cell__lbl">{{ cell.label }}</span>
      <!-- NEW-2: 셀 폭(360px 뷰포트에서 약 101px)에 "2일 (반차 1회, 시간차 3시간)" 한 줄 표기는
           4행 가까이 접혀 3열 높이가 어긋난다. 마이페이지 요약 카드와 동일하게
           대형 숫자(일) + 보조행(반차·시간차)으로 분리한다. 보조행이 없으면 기존과 동일한 "N일". -->
      <span class="cell__val" :class="{ 'cell__val--muted': cell.value === 0 }">
        {{ cell.dayText }}<span class="cell__unit">일</span>
        <span v-if="cell.subText" class="cell__sub">{{ cell.subText }}</span>
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { splitLeaveDays, splitLeaveDaysWithHourly } from '@/utils/leaveFormat'

const props = defineProps({
  // { granted, used, planned }
  group: {
    type: Object,
    default: null,
  },
  // LC-11: 1일 환산시간(분) — "N일 H시간 M분" 표기 분모(서버 권위). 미제공 시 480 폴백.
  convMinutes: {
    type: Number,
    default: 480,
  },
  // HB-13(F-3): 시간차 실사용 분(서버 hourlyUsedMinutesPast) — 사용 셀의 역환산 대체용. 0이면 기존 표기.
  hourlyUsedMinutes: {
    type: Number,
    default: 0,
  },
  // HB-13(F-3): 시간차 사용예정 분(서버 hourlyUsedMinutesPlanned) — 사용예정 셀용. 0이면 기존 표기.
  hourlyPlannedMinutes: {
    type: Number,
    default: 0,
  },
  // HB-13 §20-2: 반차 사용 "일수"(서버 halfDayUsedDaysPast) — 정수부 표기에서 증발하던 0.5일 보전용.
  //   건수가 아니라 일수다(분할차감 대응). 표기 건수 환산은 leaveFormat 이 0.5 로 나눠 처리한다.
  halfDayUsedDays: {
    type: Number,
    default: 0,
  },
  // HB-13 §20-2: 반차 사용예정 "일수"(서버 halfDayUsedDaysPlanned) — 사용예정 셀용.
  halfDayPlannedDays: {
    type: Number,
    default: 0,
  },
})

const numOr0 = (v) => Number(v ?? 0)

// LC-11: 소수점 노출 금지 — 일(dayText)은 큰 숫자, 부가(subText)는 보조행으로 분리 표기.
// HB-13(F-3 §20-2): 사용/사용예정은 반차 건수 + 시간차 실분 병기("2일" + "반차 1회, 시간차 3시간")
//   — 일수→시간 역환산 제거. 반차·시간차가 모두 0이면 splitLeaveDays 와 완전히 동일(회귀 없음).
// NEW-2: 3열 셀 폭이 좁아 한 줄 텍스트(formatLeaveDaysWithHourly)는 4행 가까이 접힌다.
//   마이페이지 요약 카드(MyPageView)와 같은 분리형(splitLeaveDays*)으로 통일해 소비처 간 표기도 맞춘다.
//   부여는 부가 항목이 없는 값이므로 splitLeaveDays(잔여와 동일 규칙)를 쓴다.
const cells = computed(() => {
  const g = props.group || {}
  const parts = (v) => splitLeaveDays(numOr0(v), props.convMinutes)
  const partsWith = (v, min, halfDays) =>
    splitLeaveDaysWithHourly(numOr0(v), props.convMinutes, min, halfDays)
  return [
    { key: 'granted', label: '부여', value: numOr0(g.granted), ...parts(g.granted) },
    {
      key: 'used',
      label: '사용',
      value: numOr0(g.used),
      ...partsWith(g.used, props.hourlyUsedMinutes, props.halfDayUsedDays),
    },
    {
      key: 'planned',
      label: '사용예정',
      value: numOr0(g.planned),
      ...partsWith(g.planned, props.hourlyPlannedMinutes, props.halfDayPlannedDays),
    },
  ]
})
</script>

<style scoped>
.split {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 4px;
  position: relative;
}
.cell + .cell::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 1px;
  background: var(--color-border-light);
}
.cell__lbl {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.cell__val {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.cell__val--muted {
  color: var(--color-text-tertiary);
}
/* NEW-2: 단위("일") — 큰 숫자 옆 축소 표기(MyPageView .mp-leave__unit 대응). */
.cell__unit {
  margin-left: 2px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-tertiary);
}
/* NEW-2: 보조행("반차 1회, 시간차 3시간") — 블록이라 큰 숫자 아래로 내려간다.
   높이 고정(min-height)은 두지 않는다: .cell 은 grid item 이라 행 높이만큼 stretch 되고
   내용이 상단 정렬이라, 보조행이 있는 셀만 아래로 길어질 뿐 라벨·큰 숫자의 세로 위치는
   3열이 동일하게 유지된다. 고정 높이를 주면 보조행이 전혀 없는 일반 케이스에 빈 공간이
   생기고, 보조행 자체가 가변 줄수라 정확히 맞출 수도 없다. */
.cell__sub {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}
</style>
