<!--
  OvertimeForm.vue — 초과근무 신청 폼
  - 작업 ID: PRAFTA-APP-007-8 (분해: .claude/requests/app_requests/prafta-app-007-plan.md §8.5)
  - emits: submit ({ slots:[{workSeq, startDate, startTime, endDate, endTime, otType}], reqReason }), cancel
  - OT_TYPE 은 인라인 칩 단일 선택 (EXTEND/NIGHT/HOLIDAY, P12). BaseBottomSheet 미사용.
-->
<template>
  <form class="ot-form" @submit.prevent="onSubmit">
    <!-- 컨텍스트 -->
    <section class="ctx">
      <p class="ctx__date">
        <strong>{{ ctxDateDisplay }}</strong>
        <small>{{ ctxSiteDisplay }}</small>
      </p>
      <div class="ctx__row">
        <span class="ctx__lbl">스케줄</span>
        <span class="ctx__val">{{ context.workPlanName }} · {{ context.scheduleSummary || '-' }}</span>
      </div>
      <div v-if="context.attendanceSummary" class="ctx__row">
        <span class="ctx__lbl">근태</span>
        <span class="ctx__val">{{ context.attendanceSummary }}</span>
      </div>
    </section>

    <!-- 초과근무 시간 -->
    <section class="fs">
      <p class="fs__title">초과근무 시간</p>

      <SlotCard
        v-for="slot in slots"
        :key="slot.workSeq"
        :work-seq="slot.workSeq"
        :title="slot.workSeq + '구간 초과근무'"
        :removable="slots.length > 1"
        @remove="onRemoveSlot"
      >
        <label class="field">
          <span class="field__label"><span class="req">*</span>시작</span>
          <div class="input-dt">
            <DateStepperField v-model="slot.startDate" placeholder="날짜" />
            <TimeStepperField v-model="slot.startTime" placeholder="시각" />
          </div>
        </label>
        <label class="field">
          <span class="field__label"><span class="req">*</span>종료</span>
          <div class="input-dt">
            <DateStepperField v-model="slot.endDate" placeholder="날짜" />
            <TimeStepperField v-model="slot.endTime" placeholder="시각" />
          </div>
        </label>
        <label class="field">
          <span class="field__label"><span class="req">*</span>유형</span>
          <div class="ot-type-row">
            <button
              v-for="opt in OT_TYPE_OPTIONS"
              :key="opt.code"
              type="button"
              class="ot-type-chip"
              :class="{ 'ot-type-chip--on': slot.otType === opt.code }"
              @click="slot.otType = opt.code"
            >
              {{ opt.label }}
            </button>
          </div>
        </label>
      </SlotCard>

      <button
        v-if="slots.length === 1"
        type="button"
        class="btn-add"
        @click="onAddSlot"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        구간 추가
      </button>

      <!-- 신청 합계 -->
      <div class="total-box">
        <span class="total-box__lbl">신청 합계</span>
        <span class="total-box__val">{{ totalDisplay }}</span>
      </div>

      <p v-if="overlapWarning" class="warn-msg">
        2구간 시작 시각은 1구간 종료 시각 이후여야 합니다.
      </p>

      <label class="field">
        <span class="field__label">
          <span class="req">*</span>신청 사유
          <span class="field__help">{{ reqReason.length }}/100</span>
        </span>
        <textarea
          v-model="reqReason"
          class="field__textarea"
          placeholder="사유를 입력해 주세요."
          maxlength="100"
          rows="4"
        ></textarea>
      </label>
    </section>

    <p class="helper">
      <span class="helper__dot" aria-hidden="true">·</span>
      관리자 승인 후 추가근무로 반영돼요. 근태 마감 전까지 신청해 주세요.
    </p>

    <footer class="form-ft">
      <button type="button" class="btn btn--x" @click="$emit('cancel')">취소</button>
      <button type="submit" class="btn btn--p" :disabled="!isValid || submitting">
        {{ submitting ? '등록 중...' : '요청하기' }}
      </button>
    </footer>
  </form>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import SlotCard from './SlotCard.vue'
import DateStepperField from '@/components/common/DateStepperField.vue'
import TimeStepperField from '@/components/common/TimeStepperField.vue'

const props = defineProps({
  context: { type: Object, required: true },
  submitting: { type: Boolean, default: false },
})
const emit = defineEmits(['submit', 'cancel'])

const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (m) => (proxy?.$alert ? proxy.$alert(m) : window.alert(m))

const OT_TYPE_OPTIONS = [
  { code: 'EXTEND', label: '연장' },
  { code: 'NIGHT', label: '야간' },
  { code: 'HOLIDAY', label: '휴일' },
]

function ymdToInput(ymd) {
  if (!ymd || ymd.length !== 8) return ''
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
}
function inputToYmd(s) {
  return s ? s.replace(/-/g, '') : ''
}
function timeToHhmm(s) {
  return s ? s.replace(':', '').slice(0, 4) : ''
}

const slots = ref([
  {
    workSeq: 1,
    startDate: ymdToInput(props.context.workYmd),
    startTime: '',
    endDate: ymdToInput(props.context.workYmd),
    endTime: '',
    otType: 'EXTEND',
  },
])
const reqReason = ref('')

const ctxDateDisplay = computed(() => {
  const y = props.context.workYmd?.slice(0, 4)
  const m = props.context.workYmd?.slice(4, 6)
  const d = props.context.workYmd?.slice(6, 8)
  return y && m && d ? `${y}년 ${Number(m)}월 ${Number(d)}일` : '-'
})
const ctxSiteDisplay = computed(() => props.context.siteName || '')

const overlapWarning = computed(() => {
  if (slots.value.length < 2) return false
  const s1End = slots.value[0].endTime
  const s2Start = slots.value[1].startTime
  if (!s1End || !s2Start) return false
  return s2Start < s1End
})

// 신청 합계 (분 단위 → 시간/분 표시)
function toMinutes(hhmm) {
  if (!/^\d{2}:\d{2}/.test(hhmm)) return -1
  return Number(hhmm.slice(0, 2)) * 60 + Number(hhmm.slice(3, 5))
}
const totalMinutes = computed(() => {
  let total = 0
  for (const s of slots.value) {
    if (!s.startTime || !s.endTime) continue
    const sM = toMinutes(s.startTime)
    const eM = toMinutes(s.endTime)
    if (sM < 0 || eM < 0) continue
    let diff = eM - sM
    if (diff < 0) diff += 24 * 60
    total += diff
  }
  return total
})
const totalDisplay = computed(() => {
  const m = totalMinutes.value
  if (m === 0) return '0분'
  const h = Math.floor(m / 60)
  const min = m % 60
  if (h === 0) return `${min}분`
  if (min === 0) return `${h}시간`
  return `${h}시간 ${min}분`
})

const isValid = computed(() => {
  if (!reqReason.value.trim()) return false
  return slots.value.every(
    (s) => s.startDate && s.startTime && s.endDate && s.endTime && s.otType,
  )
})

const onAddSlot = () => {
  if (slots.value.length >= 2) return
  slots.value.push({
    workSeq: 2,
    startDate: ymdToInput(props.context.workYmd),
    startTime: '',
    endDate: ymdToInput(props.context.workYmd),
    endTime: '',
    otType: 'EXTEND',
  })
}
const onRemoveSlot = (workSeq) => {
  slots.value = slots.value.filter((s) => s.workSeq !== workSeq)
  slots.value.forEach((s, i) => (s.workSeq = i + 1))
}

const onSubmit = () => {
  if (!isValid.value) {
    showAlert('모든 필수 항목을 입력해 주세요.')
    return
  }
  emit('submit', {
    slots: slots.value.map((s) => ({
      workSeq: s.workSeq,
      startDate: inputToYmd(s.startDate),
      startTime: timeToHhmm(s.startTime),
      endDate: inputToYmd(s.endDate),
      endTime: timeToHhmm(s.endTime),
      otType: s.otType,
    })),
    reqReason: reqReason.value.trim(),
  })
}
</script>

<style scoped>
.ot-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* (ctx / fs / field / input-dt / btn-add / helper / form-ft / btn 은 AttdCorrectionForm 과 동일 패턴 — 향후 공통 SCSS 모듈로 분리 가능) */
.ctx {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.ctx__date {
  margin: 0 0 var(--space-xs);
  display: flex;
  flex-direction: column;
}
.ctx__date strong {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.ctx__date small {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ctx__row {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: var(--space-sm);
  align-items: baseline;
}
.ctx__lbl {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ctx__val {
  font-size: 13px;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

.fs {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.fs__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field__label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.field__label .req {
  color: var(--color-danger);
}
.field__help {
  margin-left: auto;
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.input-dt {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}

.field__input {
  height: 44px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: inherit;
  font-variant-numeric: tabular-nums;
  box-sizing: border-box;
}
.field__input:focus {
  outline: none;
  border-color: var(--color-primary);
}
.field__textarea {
  width: 100%;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: inherit;
  box-sizing: border-box;
  resize: vertical;
  min-height: 96px;
}
.field__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* OT_TYPE 칩 (단일 선택 인라인) */
.ot-type-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.ot-type-chip {
  height: 36px;
  padding: 0 14px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}
.ot-type-chip--on {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
  font-weight: 500;
}

.btn-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 40px;
  background: var(--color-surface);
  border: 0.5px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.btn-add:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* 신청 합계 박스 */
.total-box {
  background: var(--color-primary-tint);
  border: 0.5px solid var(--color-primary-tint-border);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.total-box__lbl {
  font-size: 13px;
  color: var(--color-primary-text-deep);
  font-weight: 500;
}
.total-box__val {
  font-size: 14px;
  color: var(--color-primary-text-darkest);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.warn-msg {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-danger-tint);
  border: 0.5px solid var(--color-danger);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--color-danger);
}

.helper {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-warning-tint);
  border: 0.5px solid var(--color-warning);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--color-warning-text);
  display: flex;
  gap: var(--space-xs);
}
.helper__dot {
  color: var(--color-warning);
}

.form-ft {
  position: sticky;
  bottom: 0;
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: var(--space-sm);
  padding: var(--space-sm) 0 calc(var(--space-sm) + env(safe-area-inset-bottom));
  background: var(--color-bg);
  border-top: 0.5px solid var(--color-border);
  margin: 0 calc(-1 * var(--space-lg));
  padding-left: var(--space-lg);
  padding-right: var(--space-lg);
}

.btn {
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.btn--x {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  color: var(--color-text-secondary);
}
.btn--p {
  background: var(--color-primary);
  border: 0;
  color: var(--color-surface);
}
.btn--p:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
</style>
