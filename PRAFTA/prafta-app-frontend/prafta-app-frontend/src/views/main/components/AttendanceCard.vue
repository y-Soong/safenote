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

    <!-- 3·4영역: 출근/퇴근 버튼 2분할 -->
    <div class="btn-grid">
      <button
        type="button"
        class="btn"
        :class="canCheckIn ? 'btn--primary' : 'btn--disabled'"
        :disabled="!canCheckIn"
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
})

const emit = defineEmits(['click:checkin', 'click:checkout'])

const isBeforeWork = computed(() => props.status === 'BEFORE_WORK')
const isWorking = computed(() => props.status === 'WORKING')
const isOffWork = computed(() => props.status === 'OFF_WORK')

// 시간 텍스트 muted 여부 — 출근 전(예정/스케줄 없음)일 때만 흐림 처리.
const isTimeMuted = computed(() => isBeforeWork.value)

// 상태 배지 — OFF_WORK(퇴근 완료, 중립) / WORKING(근무중) / 그외(출근 전)
const badgeClass = computed(() => {
  if (isOffWork.value) return 'badge--done'
  return isWorking.value ? 'badge--ok' : 'badge--before'
})
const badgeText = computed(() => {
  if (isOffWork.value) return '퇴근 완료'
  return isWorking.value ? '근무중' : '출근 전'
})

// 시간 텍스트 — 단순 포매팅만 (HHMM → HH:MM)
const formatHHMM = (s) => {
  if (!s || s.length < 4) return ''
  return `${s.slice(0, 2)}:${s.slice(2, 4)}`
}
const timeText = computed(() => {
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

// 액션 핸들러 — 확인 모달은 본 라운드 outside scope
const onCheckIn = () => {
  // TODO(developer): 출근 확인 모달 호출(가이드 §4.9.1) → 확인 시 POST /api/app/attd/check-in
  //                  사업장 외 출근은 정책 7.1 확정 후 모달 분기. 본 라운드는 emit 만.
  emit('click:checkin')
}

const onCheckOut = () => {
  // TODO(developer): 퇴근 확인 모달 호출. 사업장 외 퇴근은 지도 카드 포함 모달(가이드 §5.1.2)
  //                  → 확인 시 POST /api/app/attd/check-out. 본 라운드는 emit 만.
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
