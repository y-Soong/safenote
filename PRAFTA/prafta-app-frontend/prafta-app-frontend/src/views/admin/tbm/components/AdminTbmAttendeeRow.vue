<!--
  AdminTbmAttendeeRow.vue — TBM 출결 근로자 행 (진행/종료 공용, variant 분기)
  - 작업 ID: 001-P5-T-F8 (분해: 001-phase5-admin-tbm-plan.md §2-4/§2-5, §3-F/§3-H)
  - 부모:
      AdminTbmLiveView(진행화면, variant='LIVE') — 입실자 + 강제퇴실 버튼.
      AdminTbmCompletedView(종료화면, variant='COMPLETED') — 이수상태 배지 + (GPS세션) 미이수처리 버튼.
  - 버튼 노출은 부모가 prop 으로 제어(GPS 세션/세션상태 판단은 서버 산출값 기준 — C1).
  - emits: force-exit(attendee) / toggle-completion(attendee).
  - 디자인 토큰은 부모(.admin-tbm-live-view / .admin-tbm-completed-view 루트)에서 상속(자급 안 함).
  - planner 라운드 스코프: template + style 완성. script 는 props/emits + 단순 표시 computed 만.
-->
<template>
  <div class="attendee-row">
    <div class="attendee-row__main">
      <div class="attendee-row__head">
        <span class="attendee-row__name">{{ attendee.userNm || '-' }}</span>
        <span v-if="userTypeLabel" class="attendee-row__type">{{ userTypeLabel }}</span>
      </div>
      <p v-if="attendee.deptNm" class="attendee-row__dept">{{ attendee.deptNm }}</p>
      <p class="attendee-row__time">
        입실 {{ attendee.entryAt || '-' }}
        <template v-if="attendee.exitAt"> · 종료 {{ attendee.exitAt }}</template>
      </p>
    </div>

    <div class="attendee-row__side">
      <!-- 이수 상태 배지(COMPLETED variant) -->
      <span
        v-if="variant === 'COMPLETED'"
        class="attendee-badge"
        :class="completionToneClass"
      >
        {{ completionLabel }}
      </span>
      <!-- 교육준비 입실자 거리 배지(PREP variant) — 반경 초과 시 danger 톤 -->
      <span
        v-else-if="variant === 'PREP' && distanceLabel"
        class="attendee-badge"
        :class="distanceToneClass"
      >
        {{ distanceLabel }}
      </span>
      <!-- 진행 중 퇴실 상태(LIVE variant) -->
      <span v-else-if="isExited" class="attendee-badge attendee-badge--exited">퇴실</span>

      <!-- LIVE: 강제 퇴실(미퇴실 + 허용 시) -->
      <button
        v-if="variant === 'LIVE' && canForceExit && !isExited"
        type="button"
        class="attendee-btn attendee-btn--danger"
        @click="$emit('force-exit', attendee)"
      >
        강제 퇴실
      </button>

      <!-- PREP: 입실자 내보내기(입실취소, D-3) -->
      <button
        v-if="variant === 'PREP'"
        type="button"
        class="attendee-btn attendee-btn--danger"
        @click="$emit('cancel-entry', attendee)"
      >
        내보내기
      </button>

      <!-- COMPLETED: 개별 미이수 처리(GPS 세션 한정 — 부모가 canManageCompletion 으로 제어) -->
      <button
        v-if="variant === 'COMPLETED' && canManageCompletion"
        type="button"
        class="attendee-btn attendee-btn--ghost"
        @click="$emit('toggle-completion', attendee)"
      >
        이수 상태 변경
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 출결 항목: { attendanceCd, userNm, userTypeCd, deptNm, entryAt, exitAt, completionStatusCd, distanceM }
  attendee: { type: Object, required: true },
  // 'LIVE'(진행화면) | 'COMPLETED'(종료화면) | 'PREP'(교육준비화면)
  variant: { type: String, default: 'LIVE' },
  // LIVE: 강제퇴실 버튼 노출 여부(세션 IN_PROGRESS 일 때 부모가 true)
  canForceExit: { type: Boolean, default: true },
  // COMPLETED: 미이수 처리 버튼 노출(GPS 검증 세션 한정 — 부모가 서버값으로 판정해 전달)
  canManageCompletion: { type: Boolean, default: false },
  // PREP: 세션 GPS 검증 반경(m). distanceM > radiusM 면 거리 배지 danger 톤
  radiusM: { type: Number, default: null },
})

// force-exit: 강제퇴실 / toggle-completion: 이수상태 변경 / cancel-entry: 입실취소(내보내기, PREP)
defineEmits(['force-exit', 'toggle-completion', 'cancel-entry'])

// 대상유형(SYS050) — 표시용
const USER_TYPE_LABELS = { REGULAR: '정규직', DAILY: '일용직' }
const userTypeLabel = computed(() => USER_TYPE_LABELS[props.attendee?.userTypeCd] || '')

// 퇴실 여부(EXIT_AT 존재)
const isExited = computed(() => !!props.attendee?.exitAt)

// 이수상태(SYS053) 라벨/톤
const completionLabel = computed(() => {
  switch (props.attendee?.completionStatusCd) {
    case 'COMPLETED':
      return '이수'
    case 'NOT_COMPLETED':
      return '미이수'
    default:
      return '미처리'
  }
})
const completionToneClass = computed(() => {
  switch (props.attendee?.completionStatusCd) {
    case 'COMPLETED':
      return 'attendee-badge--completed'
    case 'NOT_COMPLETED':
      return 'attendee-badge--not-completed'
    default:
      return 'attendee-badge--pending'
  }
})

// 교육준비(PREP) 거리 배지: distanceM(m) 표시. 값 없으면 미표시.
const distanceLabel = computed(() => {
  const d = props.attendee?.distanceM
  if (d == null || Number.isNaN(Number(d))) return ''
  return `${Math.round(Number(d))}m`
})
// 반경(radiusM) 초과 시 danger 톤(반경 미지정 시 중립).
const distanceToneClass = computed(() => {
  const d = Number(props.attendee?.distanceM)
  const r = Number(props.radiusM)
  if (!Number.isNaN(d) && !Number.isNaN(r) && r > 0 && d > r) {
    return 'attendee-badge--not-completed'
  }
  return 'attendee-badge--pending'
})
</script>

<style scoped>
.attendee-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.attendee-row__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.attendee-row__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.attendee-row__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.attendee-row__type {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  font-size: 11px;
  color: var(--color-text-secondary);
}
.attendee-row__dept {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  word-break: break-all;
}
.attendee-row__time {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.attendee-row__side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-sm);
}

/* 이수/퇴실 배지 */
.attendee-badge {
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.attendee-badge--completed {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.attendee-badge--not-completed {
  background: var(--color-danger-tint);
  color: var(--color-danger-text);
}
.attendee-badge--pending {
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.attendee-badge--exited {
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

/* 행 액션 버튼 */
.attendee-btn {
  height: 32px;
  padding: 0 var(--space-md);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.attendee-btn--danger {
  background: var(--color-surface);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger);
}
.attendee-btn--ghost {
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
}
</style>
