<!--
  AttendanceCard.vue — 출퇴근 카드 (케이스 1/2/3 통합)
  - 상세 요청서 §3.2 (5영역 + 상태 매트릭스), §4.2
  - attd §5.1 §5.2 §7.1 §7.2 §7.3
  - 5영역: 상태배지 / 시간텍스트 / 출근버튼 / 퇴근버튼 / 위치
  - 영역 위치·크기는 케이스 무관 고정. 활성·비활성과 텍스트만 케이스별로 변화
-->
<template>
  <div class="card card--first attendance-card">
    <!-- 1·2영역: 상태배지 + 시간텍스트 (같은 행) -->
    <div class="att-head">
      <span class="badge" :class="badgeClass">
        <span class="badge__dot"></span>{{ badgeText }}
      </span>
      <span class="att-time" :class="{ 'att-time--muted': isTimeMuted }">{{ timeText }}</span>
    </div>

    <!-- 3·4영역: 출근/퇴근 버튼 -->
    <!--
      prafta-app-015: 2구간 스케줄이고 출근 우선 상태(퇴근 불가)이면 "1구간 출근"/"2구간 출근" 컴팩트 2버튼.
        각 버튼 enabled = 서버 구간 플래그(slot.canCheckInThisSlot). 그 외(1구간/스케줄없음/퇴근 우선)는
        기존 출근하기/퇴근하기 2분할 유지.
    -->
    <div v-if="showSlotCheckInButtons" class="btn-grid">
      <button
        v-for="slot in checkInSlots"
        :key="`ci-${slot.workSeq}`"
        type="button"
        class="btn"
        :class="slot.canCheckInThisSlot ? 'btn--primary' : 'btn--disabled'"
        :disabled="!slot.canCheckInThisSlot"
        @click="onSlotCheckIn(slot.workSeq)"
      >
        {{ slot.workSeq === 1 ? '1구간 출근' : '2구간 출근' }}
      </button>
    </div>
    <div v-else class="btn-grid">
      <button
        type="button"
        class="btn"
        :class="checkInEnabled ? 'btn--primary' : 'btn--disabled'"
        :disabled="!checkInEnabled"
        @click="onCheckIn"
      >
        출근하기
      </button>
      <button
        type="button"
        class="btn"
        :class="canCheckOut ? 'btn--primary' : 'btn--disabled'"
        :disabled="!canCheckOut"
        @click="onCheckOut"
      >
        퇴근하기
      </button>
    </div>

    <!-- prafta-app-021: 전날 미퇴근 마감 대기 안내 -->
    <p v-if="prevDayCheckoutPending" class="prevday-hint">
      전날 근무가 아직 안 닫혔어요. 먼저 퇴근을 등록해 주세요.
    </p>

    <!-- 5영역: 하단 위치 표시 -->
    <div class="loc-meta" :class="isOffsite ? 'loc-meta--warn' : 'loc-meta--neutral'">
      <svg
        v-if="isOffsite"
        class="icon loc-meta__icon loc-meta__icon--warn"
        width="14"
        height="14"
        aria-hidden="true"
      >
        <use href="#i-mappinoff" />
      </svg>
      <svg
        v-else
        class="icon loc-meta__icon"
        :class="isBeforeWork ? 'loc-meta__icon--tertiary' : 'loc-meta__icon--primary'"
        width="14"
        height="14"
        aria-hidden="true"
      >
        <use href="#i-mappin" />
      </svg>
      <span>{{ locationText }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 'BEFORE_WORK' | 'WORKING' | 'OFF_WORK'
  status: {
    type: String,
    default: 'BEFORE_WORK',
  },
  // 사업장 외 여부 (status === 'WORKING' / 'OFF_WORK' 일 때 의미 있음)
  isOffsite: {
    type: Boolean,
    default: false,
  },
  // 스케줄 시작/종료 시각 (HHMM, 예: "0930")
  scheduleStartTime: {
    type: String,
    default: '',
  },
  scheduleEndTime: {
    type: String,
    default: '',
  },
  // 실제 출근 시각 (HHMM, 예: "0928")
  checkInTime: {
    type: String,
    default: '',
  },
  // 실제 퇴근 시각 (HHMM, 예: "2156") — OFF_WORK 시 표시
  checkOutTime: {
    type: String,
    default: '',
  },
  // 기준일 근무 스케줄 존재 여부 — BEFORE_WORK 시 "스케줄 없음" 분기
  scheduleExists: {
    type: Boolean,
    default: true,
  },
  // 소속 사업장명 (위치 표시용)
  siteName: {
    type: String,
    default: '',
  },
  // 서버가 산출한 활성 여부
  canCheckIn: {
    type: Boolean,
    default: false,
  },
  canCheckOut: {
    type: Boolean,
    default: false,
  },
  // prafta-app-015: 2구간 스케줄 여부(구간 선택 버튼 노출 판정)
  isTwoSlot: {
    type: Boolean,
    default: false,
  },
  // prafta-app-015: 구간별 게이팅 플래그 [{ workSeq, canCheckInThisSlot, alreadyCheckedIn }]
  slots: {
    type: Array,
    default: () => [],
  },
  // prafta-app-021: 전날 미퇴근 마감 대기 여부 — true 시 출근 차단·퇴근 유도.
  prevDayCheckoutPending: {
    type: Boolean,
    default: false,
  },
  // prafta-app-021: 전날 출근 시각 (HHMM) — "출근 {HH:MM} (전날)" 표시용.
  prevDayCheckInTime: {
    type: String,
    default: '',
  },
})

// prafta-app-015: 구간 선택 출근 시 workSeq 를 payload 로 전달.
const emit = defineEmits(['click:checkin', 'click:checkout'])

const isBeforeWork = computed(() => props.status === 'BEFORE_WORK')
const isWorking = computed(() => props.status === 'WORKING')
const isOffWork = computed(() => props.status === 'OFF_WORK')

// 시간 텍스트 muted 여부 — 출근 전(예정/스케줄 없음)일 때만 흐림 처리.
const isTimeMuted = computed(() => isBeforeWork.value)

// 상태 배지 — OFF_WORK(퇴근 완료, 중립) / WORKING(근무중) / 그외(출근 전)
// prafta-app-021: 전날 미퇴근 마감 대기 시 최우선 분기.
const badgeClass = computed(() => {
  if (props.prevDayCheckoutPending) return 'badge--warn'
  if (isOffWork.value) return 'badge--done'
  return isWorking.value ? 'badge--ok' : 'badge--before'
})
const badgeText = computed(() => {
  if (props.prevDayCheckoutPending) return '전날 미퇴근'
  if (isOffWork.value) return '퇴근 완료'
  return isWorking.value ? '근무중' : '출근 전'
})

// prafta-app-021: 출근 버튼 활성 — 전날 미퇴근 대기 중에는 출근 차단(퇴근 선행 유도).
const checkInEnabled = computed(() => !props.prevDayCheckoutPending && props.canCheckIn)

// 시간 텍스트 — 단순 포매팅만 (HHMM → HH:MM)
const formatHHMM = (s) => {
  if (!s || s.length < 4) return ''
  return `${s.slice(0, 2)}:${s.slice(2, 4)}`
}
const timeText = computed(() => {
  // prafta-app-021: 전날 미퇴근 마감 대기 시 전날 출근 시각을 우선 표시.
  if (props.prevDayCheckoutPending) {
    const t = formatHHMM(props.prevDayCheckInTime)
    return t ? `출근 ${t} (전날)` : '출근 --:-- (전날)'
  }
  if (isOffWork.value) {
    const inT = formatHHMM(props.checkInTime)
    const outT = formatHHMM(props.checkOutTime)
    if (inT && outT) return `출근 ${inT} · 퇴근 ${outT}`
    if (inT) return `출근 ${inT}`
    return '-'
  }
  if (isBeforeWork.value) {
    if (!props.scheduleExists) return '스케줄 없음'
    const start = formatHHMM(props.scheduleStartTime)
    const end = formatHHMM(props.scheduleEndTime)
    return start && end ? `예정 ${start} ~ ${end}` : '예정 --:-- ~ --:--'
  }
  // WORKING
  const t = formatHHMM(props.checkInTime)
  return t ? `출근 ${t}` : '출근 --:--'
})

// 위치 표시 텍스트
const locationText = computed(() => {
  if (isBeforeWork.value) return '-'
  if (props.isOffsite) return '사업장 외'
  return props.siteName || '-'
})

// prafta-app-015: 2구간 스케줄 구간 선택 버튼 노출 판정(today 카드와 동일 규칙).
//   isTwoSlot && 퇴근 우선 아님(canCheckOut=false) && 출근 가능 구간이 1개 이상.
const checkInSlots = computed(() =>
  (props.slots || [])
    .filter((s) => s && (s.workSeq === 1 || s.workSeq === 2))
    .slice()
    .sort((a, b) => a.workSeq - b.workSeq),
)
// prafta-app-021: 전날 미퇴근 대기 시엔 구간 출근 버튼도 노출하지 않음(표시 정합).
const showSlotCheckInButtons = computed(
  () =>
    !props.prevDayCheckoutPending &&
    props.isTwoSlot &&
    !props.canCheckOut &&
    checkInSlots.value.some((s) => s.canCheckInThisSlot === true),
)

// 액션 핸들러 — 확인 모달/GPS/API 는 컨테이너(MainView)가 처리. 본 컴포넌트는 emit 만.
const onCheckIn = () => {
  // 단일 출근(1구간/스케줄없음) — targetWorkSeq 미동봉.
  emit('click:checkin')
}
const onSlotCheckIn = (workSeq) => {
  // prafta-app-015: 2구간 구간 선택 출근 — 선택 구간을 payload 로 전달.
  emit('click:checkin', { targetWorkSeq: workSeq })
}

const onCheckOut = () => {
  emit('click:checkout')
}
</script>

<style scoped>
.card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--color-border);
  padding: 16px;
  margin-bottom: 12px;
}
.card--first {
  box-shadow: var(--shadow-sm);
}

.att-head {
  display: flex;
  align-items: center;
  height: 22px;
  margin-bottom: 12px;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 500;
  border-radius: var(--radius-sm);
}
.badge__dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
}
.badge--ok {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.badge--ok .badge__dot {
  background: var(--color-primary);
}
.badge--before {
  background: var(--color-info-tint);
  color: var(--color-info-strong);
}
.badge--before .badge__dot {
  background: var(--color-info);
}
.badge--done {
  background: var(--color-bg);
  color: var(--color-text-secondary);
}
.badge--done .badge__dot {
  background: var(--color-text-tertiary);
}
/* prafta-app-021: 전날 미퇴근 마감 대기 배지(토큰은 MainView.home-view 선언분 상속) */
.badge--warn {
  background: var(--color-warning-tint);
  color: var(--color-warning-text-strong);
}
.badge--warn .badge__dot {
  background: var(--color-warning);
}

.att-time {
  margin-left: 8px;
  height: 22px;
  font-size: 14px;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
  display: inline-flex;
  align-items: center;
}
.att-time--muted {
  color: var(--color-text-tertiary);
}

.btn-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 10px;
}

.btn {
  height: 52px;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  border: 0;
  padding: 0;
  font-family: inherit;
}
.btn--primary {
  background: var(--color-primary);
  color: #ffffff;
}
.btn--disabled {
  background: var(--color-disabled-bg);
  color: var(--color-disabled-text);
  cursor: not-allowed;
}

/* prafta-app-021: 전날 미퇴근 마감 대기 안내 문구 */
.prevday-hint {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--color-warning-text-strong);
  line-height: 1.4;
}

.loc-meta {
  padding: 8px 10px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.loc-meta--neutral {
  background: var(--color-bg);
  color: var(--color-text-secondary);
}
.loc-meta--warn {
  background: var(--color-warning-tint);
  color: var(--color-warning-text-strong);
}
.loc-meta__icon--primary {
  color: var(--color-primary);
}
.loc-meta__icon--warn {
  color: var(--color-warning-text);
}
.loc-meta__icon--tertiary {
  color: var(--color-text-tertiary);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
