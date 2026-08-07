<!--
  LeaveBalanceCard.vue — 메인 잔여 카드 (큰 숫자 + 진행바 + 범례)
  - 작업: prafta-app-005 슬롯 E (UI 명세: UI-A005)
  - 시안 §4.4 / 참조: views/attd/components/AttendanceTodayCard.vue (.cd 카드 토큰)
  - group: { granted, used, planned, remaining }. 잔여>0=primary green / ==0=text-primary.
  - 진행바 3분할: 사용(primary) / 사용예정(primary-tint-border) / 잔여(배경). 부여0=회색 단일.
  - 표시 전용(비즈니스 로직 없음). 토큰은 부모(.my-leave-view)에서 상속.
-->
<template>
  <div class="card">
    <!-- HB-13(F-3): 잔여 시간 표기는 참고용 근사치 — 라벨 옆 안내 버튼으로만 고지한다.
         ("(참고)" 텍스트 접미 금지 / 사용·사용예정은 정확값이라 버튼을 붙이지 않는다) -->
    <p class="card__label">
      {{ label }}
      <button
        type="button"
        class="card__guide"
        aria-label="잔여 연차 시간 표기 안내"
        @click="$emit('guide')"
      >
        <svg
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="11" x2="12" y2="16" />
          <line x1="12" y1="8" x2="12.01" y2="8" />
        </svg>
      </button>
    </p>

    <!-- 큰 숫자 행 (LC-11: 소수점 금지 — 일은 대형 숫자, 시간·분은 보조 텍스트) -->
    <div class="num-row">
      <span class="num" :class="{ 'num--zero': isZeroRemaining }">{{ remainingParts.dayText }}</span>
      <span class="num__unit">일</span>
      <span v-if="remainingParts.subText" class="num__sub">{{ remainingParts.subText }}</span>
      <span class="num__total">/ {{ grantedText }}</span>
    </div>

    <!-- 진행 바 -->
    <div class="bar">
      <template v-if="hasGrant">
        <div class="bar__used" :style="{ width: usedPercent }"></div>
        <div class="bar__plan" :style="{ width: plannedPercent }"></div>
      </template>
      <div v-else class="bar__zero"></div>
    </div>

    <!-- 부여 없음 안내 / 범례 -->
    <p v-if="!hasGrant" class="no-grant">부여된 연차가 없습니다</p>
    <div v-else class="legend">
      <span class="lg"><i class="lg__dot lg__dot--used"></i>사용 {{ usedText }}</span>
      <span class="lg"><i class="lg__dot lg__dot--plan"></i>사용예정 {{ plannedText }}</span>
      <span class="lg"><i class="lg__dot lg__dot--rest"></i>잔여 {{ remainingText }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatLeaveDays, formatLeaveDaysWithHourly, splitLeaveDays } from '@/utils/leaveFormat'

defineEmits(['guide'])

const props = defineProps({
  // 메인 카드 라벨 ("잔여 일수" / "법정 잔여 일수" / "법정 외 잔여 일수")
  label: {
    type: String,
    default: '잔여 일수',
  },
  // { granted, used, planned, remaining }
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

const granted = computed(() => numOr0(props.group?.granted))
const used = computed(() => numOr0(props.group?.used))
const planned = computed(() => numOr0(props.group?.planned))
const remaining = computed(() => numOr0(props.group?.remaining))

const hasGrant = computed(() => granted.value > 0)
const isZeroRemaining = computed(() => remaining.value === 0)

// LC-11: 소수점 노출 금지 — "N일 H시간 M분" 표기(내부 계산값은 그대로, 표시만 교체).
const grantedText = computed(() => formatLeaveDays(granted.value, props.convMinutes))
// HB-13(F-3 §20-2): 사용/사용예정은 반차 건수 + 시간차 실분을 별도 항목화(역환산 제거).
//   반차·시간차가 모두 0이면 기존 표기와 완전히 동일.
const usedText = computed(() =>
  formatLeaveDaysWithHourly(
    used.value,
    props.convMinutes,
    props.hourlyUsedMinutes,
    props.halfDayUsedDays,
  ),
)
const plannedText = computed(() =>
  formatLeaveDaysWithHourly(
    planned.value,
    props.convMinutes,
    props.hourlyPlannedMinutes,
    props.halfDayPlannedDays,
  ),
)
// 잔여는 역환산 참고치 유지(E4) — 안내 버튼으로 근사치임을 고지한다.
const remainingText = computed(() => formatLeaveDays(remaining.value, props.convMinutes))
// 대형 숫자 레이아웃용 분리 표기: dayText(큰 숫자) + subText("H시간 M분", 없으면 '').
const remainingParts = computed(() => splitLeaveDays(remaining.value, props.convMinutes))

// 진행바 분할 (부여 100% 기준) — 단순 비율 산출(표시용)
const pct = (v) => (granted.value > 0 ? `${(v / granted.value) * 100}%` : '0%')
const usedPercent = computed(() => pct(used.value))
const plannedPercent = computed(() => pct(planned.value))
</script>

<style scoped>
.card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 16px;
}
.card__label {
  margin: 0 0 4px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
/* 잔여 시간 표기 안내 버튼(ⓘ) — 탭 영역 확보를 위해 최소 크기 유지 */
.card__guide {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  cursor: pointer;
}

.num-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 14px;
}
.num {
  font-size: 36px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}
.num--zero {
  color: var(--color-text-primary);
}
.num__unit {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
/* LC-11: 시간·분 보조 텍스트 ("3시간 30분") — 일 단위 옆 축소 표기 */
.num__sub {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}
.num__total {
  margin-left: 6px;
  font-size: 13px;
  font-weight: 400;
  color: var(--color-text-secondary);
}

/* 진행 바 */
.bar {
  height: 8px;
  display: flex;
  margin-bottom: 12px;
  background: var(--color-border-light);
  border-radius: var(--radius-full);
  overflow: hidden;
}
.bar__used {
  height: 100%;
  background: var(--color-primary);
}
.bar__plan {
  height: 100%;
  background: var(--color-primary-tint-border);
}
.bar__zero {
  width: 100%;
  height: 100%;
  background: var(--color-text-tertiary);
}

.no-grant {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* 범례 */
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.lg {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.lg__dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
}
.lg__dot--used {
  background: var(--color-primary);
}
.lg__dot--plan {
  background: var(--color-primary-tint-border);
  border: 1px solid var(--color-primary);
}
.lg__dot--rest {
  background: var(--color-border-light);
  border: 1px solid var(--color-border);
}
</style>
