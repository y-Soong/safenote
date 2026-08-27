<!--
  AttdCorrectionForm.vue — 근태 보정 요청 폼
  - 작업 ID: PRAFTA-APP-007-7 (분해: .claude/requests/app_requests/prafta-app-007-plan.md §8.4)
  - emits: submit ({ slots:[{workSeq, startDate, startTime, endDate, endTime}], reqReason }), cancel
-->
<template>
  <form class="attd-form" @submit.prevent="onSubmit">
    <!-- 컨텍스트 박스 -->
    <section class="ctx">
      <p class="ctx__date">
        <strong>{{ ctxDateDisplay }}</strong>
        <small>{{ ctxSiteDisplay }}</small>
      </p>
      <div class="ctx__row">
        <span class="ctx__lbl">스케줄</span>
        <span class="ctx__val"
          >{{ context.workPlanName }} · {{ scheduleSummaryDisplay || '-' }}</span
        >
      </div>
      <div v-if="context.attendanceSummary" class="ctx__row">
        <span class="ctx__lbl">현재 근태</span>
        <span class="ctx__val" :class="{ 'ctx__val--warn': hasIssue }">
          {{ attendanceSummaryDisplay }}{{ hasIssue ? ' (확인 필요)' : '' }}
        </span>
      </div>
    </section>

    <!-- 보정할 시간 -->
    <section class="fs">
      <p class="fs__title">보정할 시간</p>

      <SlotCard
        v-for="slot in slots"
        :key="slot.workSeq"
        :work-seq="slot.workSeq"
        :title="slot.workSeq + '구간'"
        :removable="isRemovable(slot.workSeq)"
        @remove="onRemoveSlot"
      >
        <label class="field">
          <span class="field__label"><span class="req">*</span>출근</span>
          <div class="input-dt">
            <DateStepperField v-model="slot.startDate" placeholder="날짜" />
            <TimeStepperField v-model="slot.startTime" placeholder="시각" />
          </div>
        </label>
        <label class="field">
          <span class="field__label"><span class="req">*</span>퇴근</span>
          <div class="input-dt">
            <DateStepperField v-model="slot.endDate" placeholder="날짜" />
            <TimeStepperField v-model="slot.endTime" placeholder="시각" />
          </div>
        </label>
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

      <!-- 2구간 겹침 인라인 경고 (P9, 차단 안 함) -->
      <p v-if="overlapWarning" class="warn-msg">
        2구간 시작 시각은 1구간 종료 시각 이후여야 합니다.
      </p>

      <label class="field">
        <span class="field__label">
          <span class="req">*</span>보정 사유
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

    <!-- 결재선 (prafta-app-009) -->
    <ApprovalLineSection
      v-if="showApprovalSection"
      ref="approvalSectionRef"
      v-model="approverList"
      :presets="presets"
      @open-picker="onOpenApproverPicker"
    />
    <p v-else-if="approvalNotice" class="aprv-notice">
      <span class="aprv-notice__dot" aria-hidden="true">·</span>
      {{ approvalNotice }}
    </p>

    <p class="helper">
      <span class="helper__dot" aria-hidden="true">·</span>
      관리자 승인 후 근태에 반영돼요. 근태 마감 전까지 신청해 주세요. 원본 출퇴근 기록은 보존돼요.
    </p>

    <!-- F-10 규약: 왼쪽=진행/확정, 오른쪽=이탈, 폭 균등 -->
    <footer class="form-ft">
      <button type="submit" class="btn btn--p" :disabled="!isValid || submitting">
        {{ submitting ? '등록 중...' : '요청하기' }}
      </button>
      <button type="button" class="btn btn--x" @click="$emit('cancel')">취소</button>
    </footer>

    <!-- 결재자 추가 바텀시트 (prafta-app-009) -->
    <ApproverPickerSheet
      v-if="showApprovalSection"
      v-model="approverPickerOpen"
      :excluded-user-cds="approverUserCds"
      @add="onAddApprovers"
    />
  </form>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import SlotCard from './SlotCard.vue'
import DateStepperField from '@/components/common/DateStepperField.vue'
import TimeStepperField from '@/components/common/TimeStepperField.vue'
import ApprovalLineSection from './ApprovalLineSection.vue'
import ApproverPickerSheet from '@/components/common/ApproverPickerSheet.vue'
import { formatTimeSummary } from '@/views/attd/attdFormat'

const props = defineProps({
  context: { type: Object, required: true },
  submitting: { type: Boolean, default: false },
  // prafta-app-009: 본인 소유 결재선 프리셋([{ presetId, presetNm, defaultYn, steps[] }]).
  presets: { type: Array, default: () => [] },
  // prafta-app-009: 결재선 분기 컨텍스트 { selfApprvYn:'Y'|'N', isNodeAdmin:bool } | null(미상).
  approvalContext: { type: Object, default: null },
})
const emit = defineEmits(['submit', 'cancel'])

const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (m) => (proxy?.$alert ? proxy.$alert(m) : window.alert(m))

// 형식 유틸 — 컨텍스트 → input 값 / emit 값 변환
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

// 현재 근태 시각으로 프리필 (P16).
// prafta-app-016-FU1: 실제 attendance 응답 키는 checkInTime/checkOutTime(+checkInDate/checkOutDate).
//   종전 코드는 attendance.startTime/endTime(존재하지 않는 키)을 읽어 프리필이 빈 값이던 잠복 버그 → 교정.
const buildInitialSlots = () => {
  const ctxSlots = (props.context?.slots || []).map((s, i) => ({
    workSeq: s.workSeq ?? i + 1,
    startDate: ymdToInput(s.attendance?.checkInDate || props.context.workYmd),
    startTime: hhmmToTime(s.attendance?.checkInTime),
    endDate: ymdToInput(s.attendance?.checkOutDate || props.context.workYmd),
    endTime: hhmmToTime(s.attendance?.checkOutTime),
  }))
  if (ctxSlots.length === 0) {
    return [
      {
        workSeq: 1,
        startDate: ymdToInput(props.context.workYmd),
        startTime: '',
        endDate: ymdToInput(props.context.workYmd),
        endTime: '',
      },
    ]
  }
  return ctxSlots.slice(0, 2)
}

const slots = ref(buildInitialSlots())
const reqReason = ref('')

// 이미 근태가 찍혀 있는(서버가 내려준) 구간의 workSeq 집합.
//   보정 요청은 "시각을 고쳐 달라"는 것이지 "근태를 지워 달라"는 게 아니므로, 기존 구간은 삭제 대상이 아니다.
//   → 이 폼에서 새로 추가한 구간만 삭제(쓰레기통)를 허용한다.
const serverSlotSeqs = new Set(
  (props.context?.slots || []).map((s, i) => s.workSeq ?? i + 1),
)

// 삭제 가능 여부 — 서버에 이미 존재하는 구간이면 불가.
//   근태가 아예 없는 날(서버 구간 0개)의 신규 작성은 종전대로 2구간일 때 서로 삭제 가능.
const isRemovable = (workSeq) => !serverSlotSeqs.has(workSeq) && slots.value.length > 1

// ── 결재선 상태 (prafta-app-009) ─────────────────────────────────────────
const approverList = ref([]) // [{ approverUserCd, userNm, userId, rankNm, nodeNm }] (순서 = 결재 단계)
const approverPickerOpen = ref(false)
const approvalSectionRef = ref(null)

const selfApprvYn = computed(() => props.approvalContext?.selfApprvYn || null)
const showApprovalSection = computed(() => selfApprvYn.value !== 'Y')
const approvalNotice = computed(() => {
  if (selfApprvYn.value !== 'Y') return ''
  return props.approvalContext?.isNodeAdmin
    ? '결재선을 지정하지 않으면 본인이 부서 기본 결재자로 자동 지정돼요. 본인이 직접 승인해야 반영돼요.'
    : '부서 관리자 승인 후 반영돼요. 결재선을 지정하지 않아도 돼요.'
})
const approverUserCds = computed(() => approverList.value.map((a) => a.approverUserCd))
const approverRequired = computed(() => selfApprvYn.value === 'N')

const onOpenApproverPicker = () => {
  approverPickerOpen.value = true
}

// 시트 add(picked[]) 수신 → approverList 순서 append. userCd 식별자 dedup. 직접 추가 시 프리셋 이탈.
const onAddApprovers = (picked) => {
  const existing = new Set(approverList.value.map((a) => a.approverUserCd))
  const additions = (picked || [])
    .filter((p) => p && p.userCd && !existing.has(p.userCd))
    .map((p) => ({
      approverUserCd: p.userCd,
      userNm: p.userNm,
      userId: p.userId,
      rankNm: p.rankNm,
      nodeNm: p.nodeNm,
    }))
  if (additions.length > 0) {
    approverList.value = [...approverList.value, ...additions]
    approvalSectionRef.value?.resetPreset?.()
  }
  approverPickerOpen.value = false
}

const ctxDateDisplay = computed(() => {
  const y = props.context.workYmd?.slice(0, 4)
  const m = props.context.workYmd?.slice(4, 6)
  const d = props.context.workYmd?.slice(6, 8)
  return y && m && d ? `${y}년 ${Number(m)}월 ${Number(d)}일` : '-'
})
const ctxSiteDisplay = computed(() => props.context.siteName || '')
// BE raw HHMM 요약("0716~1811" / "0700~1300 / 1700~2100")을 "HH:MM ~ HH:MM" 표시형으로 변환.
const scheduleSummaryDisplay = computed(() => formatTimeSummary(props.context.scheduleSummary))
const attendanceSummaryDisplay = computed(() => formatTimeSummary(props.context.attendanceSummary))
const hasIssue = computed(() => Boolean(props.context.hasIssue))

const overlapWarning = computed(() => {
  if (slots.value.length < 2) return false
  const s1End = slots.value[0].endTime
  const s2Start = slots.value[1].startTime
  if (!s1End || !s2Start) return false
  return s2Start < s1End
})

const isValid = computed(() => {
  // 사유 미입력은 버튼 비활성 사유에서 제외(제출 시 사유 전용 alert 로 안내).
  if (!slots.value.every((s) => s.startDate && s.startTime && s.endDate && s.endTime)) return false
  // 결재 필수('N') 케이스는 결재자 1명 이상이어야 제출 활성.
  if (approverRequired.value && approverList.value.length === 0) return false
  return true
})

// context.slots 에서 해당 workSeq 의 현재 근태 시각을 찾아 폼 slot 으로 변환 (없으면 빈 값).
// prafta-app-016-FU1: checkInTime/checkOutTime(+checkInDate/checkOutDate) 키로 교정.
const makeSlotFromContext = (workSeq) => {
  const ctx = (props.context?.slots || []).find((s, i) => (s.workSeq ?? i + 1) === workSeq)
  return {
    workSeq,
    startDate: ymdToInput(ctx?.attendance?.checkInDate || props.context.workYmd),
    startTime: hhmmToTime(ctx?.attendance?.checkInTime),
    endDate: ymdToInput(ctx?.attendance?.checkOutDate || props.context.workYmd),
    endTime: hhmmToTime(ctx?.attendance?.checkOutTime),
  }
}

const onAddSlot = () => {
  if (slots.value.length >= 2) return
  // workSeq 는 구간 식별자(1/2)이므로 비어 있는 번호를 채운다. (위치 기반 재인덱싱 금지)
  const existing = new Set(slots.value.map((s) => s.workSeq))
  const missing = [1, 2].find((n) => !existing.has(n))
  if (!missing) return
  slots.value.push(makeSlotFromContext(missing))
  slots.value.sort((a, b) => a.workSeq - b.workSeq)
}

// 구간 삭제 — workSeq 는 구간 식별자이므로 남은 구간 번호를 재인덱싱하지 않는다.
// (1구간 삭제 시 2구간이 1구간으로 바뀌던 버그 수정)
const onRemoveSlot = (workSeq) => {
  slots.value = slots.value.filter((s) => s.workSeq !== workSeq)
}

const onSubmit = () => {
  // 사유 전용 가드(버튼은 기본 활성 → 빈값 제출 시 사유 안내).
  if (!reqReason.value.trim()) {
    showAlert('사유를 입력해 주세요.')
    return
  }
  if (approverRequired.value && approverList.value.length === 0) {
    showAlert('결재자를 1명 이상 추가해 주세요.')
    return
  }
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
    // prafta-app-009: 결재선 노출 케이스만 approverUserCds 전개 전송(SSOT). 'Y' 케이스는 미전송(서버 분기).
    approverUserCds: showApprovalSection.value ? approverUserCds.value : undefined,
    presetId: undefined,
  })
}
</script>

<style scoped>
.attd-form {
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
.ctx__val--warn {
  color: var(--color-warning-text);
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
  font-variant-numeric: tabular-nums;
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

.warn-msg {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-danger-tint);
  border: 0.5px solid var(--color-danger);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--color-danger);
}

.aprv-notice {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-primary-tint);
  border: 0.5px solid var(--color-primary-tint-border);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--color-primary-text-deep);
  display: flex;
  gap: var(--space-xs);
}
.aprv-notice__dot {
  color: var(--color-primary);
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
  grid-template-columns: 1fr 1fr;
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
