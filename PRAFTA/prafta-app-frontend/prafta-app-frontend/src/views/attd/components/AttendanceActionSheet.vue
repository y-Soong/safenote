<!--
  AttendanceActionSheet.vue — 이번주 카드 탭 시 바텀시트
  - 작업 ID: APP002-10 (UI 명세: UI-A006)
  - 시안 화면 7·8: 핸들 + 헤더(날짜+X) + 메타 1줄 + 액션 4종
  - 정책: §3.3 매트릭스(서버 산출 actions 표시만) + attd §9.2/§11/§9.3/§9.4
  - 액션 4종 항상 노출. 안내 문구 없음. 활성/비활성만 제어(비활성=.sa.x).
  - v-model(open) 로 열림/닫힘. 오버레이/X/핸들 탭 → close.
-->
<template>
  <transition name="sheet-fade">
    <div v-if="modelValue" class="bs" @click.self="close">
      <div class="sh" role="dialog" aria-modal="true" aria-label="근태 액션">
        <div class="sh__handle" @click="close" />

        <div class="sh__row">
          <p class="sh__title">{{ dateTitle }}</p>
          <button type="button" class="sh__close" aria-label="닫기" @click="close">
            <svg class="icon" width="18" height="18" aria-hidden="true"><use href="#i-as-x" /></svg>
          </button>
        </div>

        <p class="sh__meta">{{ metaText }}</p>

        <button
          v-for="act in actionList"
          :key="act.type"
          type="button"
          class="sa"
          :class="{ 'sa--x': !act.enabled }"
          :disabled="!act.enabled"
          @click="onAction(act)"
        >
          <span class="sa__icon">
            <svg class="icon" width="18" height="18" aria-hidden="true"><use :href="act.iconId" /></svg>
          </span>
          <span class="sa__body">
            <span class="sa__title">{{ act.label }}</span>
          </span>
          <svg
            v-if="act.enabled"
            class="icon sa__chev"
            width="16"
            height="16"
            aria-hidden="true"
          >
            <use href="#i-as-chev" />
          </svg>
        </button>

        <svg width="0" height="0" class="as-sprite" aria-hidden="true" focusable="false">
          <defs>
            <symbol id="i-as-x" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="6" y1="6" x2="18" y2="18" /><line x1="18" y1="6" x2="6" y2="18" /></symbol>
            <symbol id="i-as-chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6" /></symbol>
            <symbol id="i-as-cal" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="5" width="16" height="16" rx="2" /><line x1="16" y1="3" x2="16" y2="7" /><line x1="8" y1="3" x2="8" y2="7" /><line x1="4" y1="11" x2="20" y2="11" /></symbol>
            <symbol id="i-as-edit" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3z" /><line x1="13.5" y1="6.5" x2="17.5" y2="10.5" /></symbol>
            <symbol id="i-as-clock" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="13" r="8" /><polyline points="12 9 12 13 15 13" /><line x1="18" y1="4" x2="22" y2="4" /><line x1="20" y1="2" x2="20" y2="6" /></symbol>
            <symbol id="i-as-umbrella" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 0 1 18 0z" /><line x1="12" y1="4" x2="12" y2="3" /><path d="M12 12v6a2 2 0 0 0 4 0" /></symbol>
          </defs>
        </svg>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'
import { formatYmdShortWithDow } from '../attdFormat'

const props = defineProps({
  // 열림 여부 (v-model)
  modelValue: {
    type: Boolean,
    default: false,
  },
  // 선택된 day 객체 (week.days[] 항목). 메타/액션 활성도 출처
  day: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue', 'action'])

const close = () => {
  emit('update:modelValue', false)
}

// 헤더 날짜 — "5월 19일 (화)"
const dateTitle = computed(() => formatYmdShortWithDow(props.day && props.day.workYmd))

// 메타 1줄 — "ST001 정규근무 · 09:30~18:00 · 근태 09:42~18:02" (있는 것만)
const metaText = computed(() => {
  const d = props.day
  if (!d) return ''
  const parts = []
  if (d.workPlanName) parts.push(d.workPlanName)
  else if (d.leaveTypeName) parts.push(d.leaveTypeName)
  if (d.scheduleSummary) parts.push(d.scheduleSummary)
  if (d.attendanceSummary) parts.push(`근태 ${d.attendanceSummary}`)
  return parts.join(' · ')
})

// 액션 4종 — 항상 노출, 활성/비활성만 제어 (서버 산출 day.actions, §3.3)
const actionList = computed(() => {
  const a = (props.day && props.day.actions) || {}
  return [
    { type: 'scheduleModify', label: '스케줄 수정 요청', iconId: '#i-as-cal', enabled: !!a.canRequestScheduleModify },
    { type: 'attendanceCorrection', label: '근태 보정 요청', iconId: '#i-as-edit', enabled: !!a.canRequestAttendanceCorrection },
    { type: 'overtime', label: '초과근무 신청', iconId: '#i-as-clock', enabled: !!a.canRequestOvertime },
    { type: 'leave', label: '연차 신청', iconId: '#i-as-umbrella', enabled: !!a.canRequestLeave },
  ]
})

const onAction = (act) => {
  if (!act.enabled) return
  // TODO(developer): 액션별 폼/라우팅 — 본 라운드는 emit만 (필요 시 close)
  emit('action', { type: act.type, day: props.day })
}
</script>

<style scoped>
.bs {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: var(--color-overlay);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.sh {
  background: var(--color-surface);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  padding: var(--space-sm) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom));
}

.sh__handle {
  width: 36px;
  height: 4px;
  margin: 0 auto var(--space-md);
  background: var(--color-border);
  border-radius: 2px;
  cursor: pointer;
}

.sh__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.sh__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.sh__close {
  width: 32px;
  height: 32px;
  margin-right: -8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-family: inherit;
}
.sh__meta {
  margin: 0 0 var(--space-md);
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* 액션 행 */
.sa {
  width: 100%;
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: 14px 12px;
  margin-bottom: var(--space-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
}
.sa:last-of-type {
  margin-bottom: 0;
}
.sa--x {
  background: var(--color-border-light);
  cursor: not-allowed;
  opacity: 0.55;
}
.sa__icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.sa--x .sa__icon {
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
}
.sa__body {
  flex: 1;
  min-width: 0;
}
.sa__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.sa--x .sa__title {
  color: var(--color-text-tertiary);
}
.sa__chev {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}

/* 전환 */
.sheet-fade-enter-active,
.sheet-fade-leave-active {
  transition: opacity 0.2s ease;
}
.sheet-fade-enter-from,
.sheet-fade-leave-to {
  opacity: 0;
}

.as-sprite {
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
