<!--
  OvertimeForm.vue — 초과근무 신청 폼 (prafta-app-016 개선 골격)
  - 작업 ID: prafta-app-016-2 (분해: .claude/requests/app_requests/prafta-app-016-plan.md)
  - 변경점:
      #1 근태 기반 프리필: context.slots 의 존재 구간 모두 카드화 + 실 출퇴근(checkInTime/checkOutTime) 프리필.
      #2 유형(OT_TYPE) 칩 제거 — emit payload 에서 otType 미포함(백엔드가 NULL 저장: 016-1).
      #3 구간별 등록 가능 시간 표시(앞 OT=실출근~스케줄시작 / 뒤 OT=스케줄종료~실퇴근). 표시 전용, 차단 아님.
  - emits: submit ({ slots:[{workSeq, startDate, startTime, endDate, endTime}], reqReason }), cancel
  - ⚠️ developer 가 채울 로직은 // TODO(developer) 로 표시. (날짜 경계/자정 넘김 정밀 계산 포함)
  - ⚠️ workSeq 는 구간 식별자(1/2). 위치 기반 재인덱싱 금지(prafta-app-007 메모리).
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
        <span class="ctx__val"
          >{{ context.workPlanName }} · {{ context.scheduleSummary || '-' }}</span
        >
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
        <!-- #3 등록 가능 시간 안내 (구간별, 표시 전용) -->
        <div class="ot-window" :class="{ 'ot-window--empty': !slotWindowText(slot.workSeq) }">
          <span class="ot-window__lbl">등록 가능 시간</span>
          <span class="ot-window__val">
            {{ slotWindowText(slot.workSeq) || '등록 가능한 초과 시간이 없어요' }}
          </span>
        </div>

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
        <!-- #2 유형(OT_TYPE) 칩 제거됨 — 유형 확정은 관리자 승인 단계(서버 NULL 저장) -->
      </SlotCard>

      <button v-if="slots.length === 1" type="button" class="btn-add" @click="onAddSlot">
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
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
      관리자 승인 후 추가근무로 반영돼요. 초과근무 유형(연장/야간/휴일)은 승인 시 확정돼요.
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

// ── 형식 유틸 (input 값 ↔ 컨텍스트/emit 값) ─────────────────────────────
function ymdToInput(ymd) {
  if (!ymd || ymd.length !== 8) return ''
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
}
function hhmmToTime(hhmm) {
  if (!hhmm) return ''
  if (hhmm.length === 4) return `${hhmm.slice(0, 2)}:${hhmm.slice(2)}`
  if (/^\d{2}:\d{2}/.test(hhmm)) return hhmm.slice(0, 5)
  return ''
}
function inputToYmd(s) {
  return s ? s.replace(/-/g, '') : ''
}
function timeToHhmm(s) {
  return s ? s.replace(':', '').slice(0, 4) : ''
}

// ── #1 근태 기반 프리필 ─────────────────────────────────────────────────
// ⚠️ 백엔드 attendance 응답 키는 checkInDate/checkInTime/checkOutDate/checkOutTime 이다.
//    (AttdCorrectionForm 이 쓰던 attendance.startTime/endTime 은 잘못된 키 — 본 폼은 정확한 키 사용.)
const buildInitialSlots = () => {
  const ctxSlots = props.context?.slots || []
  if (!Array.isArray(ctxSlots) || ctxSlots.length === 0) {
    // 폴백: 슬롯 정보 없음 → 1구간 빈 카드.
    return [makeEmptySlot(1)]
  }
  // TODO(developer): ctxSlots 를 순회하여 존재 구간 모두를 카드로 생성한다.
  //   - workSeq = s.workSeq ?? (i+1)
  //   - startDate = ymdToInput(s.attendance?.checkInDate || props.context.workYmd)
  //   - startTime = hhmmToTime(s.attendance?.checkInTime)
  //   - endDate   = ymdToInput(s.attendance?.checkOutDate || props.context.workYmd)
  //   - endTime   = hhmmToTime(s.attendance?.checkOutTime)
  //   - attendance 가 없는 구간(스케줄만 존재)은 시간 공란 카드로.
  //   - 최대 2구간(.slice(0, 2)).
  return []
}

// 빈 카드(폴백/수동 추가용).
const makeEmptySlot = (workSeq) => ({
  workSeq,
  startDate: ymdToInput(props.context.workYmd),
  startTime: '',
  endDate: ymdToInput(props.context.workYmd),
  endTime: '',
})

const slots = ref(buildInitialSlots())
const reqReason = ref('')

// ── 컨텍스트 표시 ───────────────────────────────────────────────────────
const ctxDateDisplay = computed(() => {
  const y = props.context.workYmd?.slice(0, 4)
  const m = props.context.workYmd?.slice(4, 6)
  const d = props.context.workYmd?.slice(6, 8)
  return y && m && d ? `${y}년 ${Number(m)}월 ${Number(d)}일` : '-'
})
const ctxSiteDisplay = computed(() => props.context.siteName || '')

// ── #3 구간별 등록 가능 시간 (표시 전용) ───────────────────────────────
// 산식(plan §0-4): 앞 OT=실출근~스케줄시작, 뒤 OT=스케줄종료~실퇴근.
//   schedule == null 인 구간 → 그 구간 실근무 전체가 등록 가능.
//   윈도우가 없으면 '' 반환(템플릿이 "없어요" 안내로 표시).
const slotWindowText = (workSeq) => {
  // TODO(developer): props.context.slots 에서 해당 workSeq 의 schedule/attendance 를 찾아
  //   앞/뒤 등록가능 윈도우 문자열을 만든다. 예) "08:30~09:00, 18:00~18:40".
  //   - schedule: { startTime, endTime } (HHMM)
  //   - attendance: { checkInTime, checkInDate, checkOutTime, checkOutDate } (HHMM/YYYYMMDD)
  //   - 앞 OT: 실출근 < 스케줄시작 일 때 "실출근~스케줄시작"
  //   - 뒤 OT: 실퇴근 > 스케줄종료 일 때 "스케줄종료~실퇴근"
  //   - schedule 없음 → 실근무 전체 한 구간
  //   - 자정 넘김(checkOutDate > checkInDate)은 표시 우선이므로 1차 단순 처리(날짜 라벨 부가는 선택).
  //   비즈니스 차단이 아니라 표시이므로 계산 실패 시 '' 반환(예외 던지지 말 것).
  return ''
}

// ── 겹침 경고 ───────────────────────────────────────────────────────────
const overlapWarning = computed(() => {
  if (slots.value.length < 2) return false
  const s1End = slots.value[0].endTime
  const s2Start = slots.value[1].startTime
  if (!s1End || !s2Start) return false
  return s2Start < s1End
})

// ── 신청 합계 ───────────────────────────────────────────────────────────
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

// ── 검증 (#2: otType 조건 제거) ─────────────────────────────────────────
const isValid = computed(() => {
  if (!reqReason.value.trim()) return false
  return slots.value.every((s) => s.startDate && s.startTime && s.endDate && s.endTime)
})

// ── 구간 추가/삭제 (workSeq 식별자 보존) ────────────────────────────────
const onAddSlot = () => {
  if (slots.value.length >= 2) return
  const existing = new Set(slots.value.map((s) => s.workSeq))
  const missing = [1, 2].find((n) => !existing.has(n))
  if (!missing) return
  // TODO(developer): 추가 구간도 가능하면 context.slots 의 해당 workSeq 근태로 프리필,
  //   없으면 makeEmptySlot(missing). (AttdCorrectionForm.makeSlotFromContext 패턴 참고하되 키 교정.)
  slots.value.push(makeEmptySlot(missing))
  slots.value.sort((a, b) => a.workSeq - b.workSeq)
}
const onRemoveSlot = (workSeq) => {
  slots.value = slots.value.filter((s) => s.workSeq !== workSeq)
}

// ── 제출 (#2: emit 에서 otType 제거) ────────────────────────────────────
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

/* #3 등록 가능 시간 안내 (카드 상단) */
.ot-window {
  display: flex;
  align-items: baseline;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-primary-tint);
  border: 0.5px solid var(--color-primary-tint-border);
  border-radius: var(--radius-md);
}
.ot-window--empty {
  background: var(--color-surface);
  border-color: var(--color-border);
}
.ot-window__lbl {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-primary-text-deep);
  white-space: nowrap;
}
.ot-window--empty .ot-window__lbl {
  color: var(--color-text-tertiary);
}
.ot-window__val {
  font-size: 12px;
  color: var(--color-primary-text-darkest);
  font-variant-numeric: tabular-nums;
}
.ot-window--empty .ot-window__val {
  color: var(--color-text-tertiary);
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
