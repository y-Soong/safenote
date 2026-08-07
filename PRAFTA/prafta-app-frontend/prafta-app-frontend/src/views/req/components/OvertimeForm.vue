<!--
  OvertimeForm.vue — 초과근무 신청 폼 (prafta-app-016 개선)
  - 작업 ID: prafta-app-016-2 (분해: .claude/requests/app_requests/prafta-app-016-plan.md)
  - 변경점:
      #1 근태 기반 프리필: context.slots 의 존재 구간 모두 카드화 + 실 출퇴근(checkInTime/checkOutTime) 프리필.
      #2 유형(OT_TYPE) 칩 제거 — emit payload 에서 otType 미포함(백엔드가 NULL 저장: 016-1).
          prafta-043: 초과근무 유형(OT_TYPE) 전면 파기 — 유형 안내 문구 제거(컬럼 DROP, 단일 '초과근무').
      #3 구간별 등록 가능 시간 표시(앞 OT=실출근~스케줄시작 / 뒤 OT=스케줄종료~실퇴근). 표시 전용, 차단 아님.
  - emits: submit ({ slots:[{workSeq, startDate, startTime, endDate, endTime}], reqReason }), cancel
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
          >{{ context.workPlanName }} · {{ scheduleSummaryDisplay || '-' }}</span
        >
      </div>
      <div v-if="context.attendanceSummary" class="ctx__row">
        <span class="ctx__lbl">근태</span>
        <span class="ctx__val">{{ attendanceSummaryDisplay }}</span>
      </div>
    </section>

    <!-- prafta-app-030: 이미 등록된 초과근무 (읽기전용). 적용분 + 대기중(미승인) 신청 모두 노출.
         대기중은 "대기중" 배지로 구분(표시 전용 — 겹침 사전차단 비대상). -->
    <section
      v-if="existingOvertimeDisplays.length || pendingOvertimeDisplays.length"
      class="existing-ot"
    >
      <p class="existing-ot__title">이미 등록된 초과근무</p>
      <ul class="existing-ot__list">
        <li v-for="item in existingOvertimeDisplays" :key="item.key" class="existing-ot__item">
          <span class="existing-ot__range">{{ item.rangeText }}</span>
        </li>
        <li v-for="item in pendingOvertimeDisplays" :key="item.key" class="existing-ot__item">
          <span class="existing-ot__range">{{ item.rangeText }}</span>
          <span class="ot-badge-pending">대기중</span>
        </li>
      </ul>
      <p class="existing-ot__hint">위 시간대와 겹치지 않도록 입력해 주세요.</p>
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
        <!-- 등록 가능 시간 (구간별). 웹 Attd_07 일자상세 팝업의 "등록 가능" 칩과 동일 동작 —
             탭하면 그 범위가 시작/종료 입력칸에 그대로 채워진다. -->
        <div class="ot-window" :class="{ 'ot-window--empty': !slotWindows(slot.workSeq).length }">
          <span class="ot-window__lbl">등록 가능 시간</span>
          <ul v-if="slotWindows(slot.workSeq).length" class="ot-window__list">
            <li v-for="(w, wi) in slotWindows(slot.workSeq)" :key="wi">
              <button
                type="button"
                class="ot-window__chip"
                :aria-label="`${w.label} 범위로 초과근무 시간 채우기`"
                @click="applyWindow(slot.workSeq, w)"
              >
                {{ w.label }}
              </button>
            </li>
          </ul>
          <span v-else class="ot-window__val">등록 가능한 초과 시간이 없어요</span>
        </div>

        <!-- prafta-app-017(이슈①) 정규 스케줄 겹침 경고 (구간별, 사전차단) -->
        <p v-if="slotOverlap(slot.workSeq)" class="warn-msg">
          스케줄 시간 내에는 초과근무를 등록할 수 없어요.
        </p>

        <!-- prafta-app-030 기존 적용 OT 겹침 경고 (구간별, 사전차단) -->
        <p v-if="slotExistingOverlap(slot.workSeq)" class="warn-msg">
          이미 등록된 초과근무와 시간이 겹쳐요.
        </p>

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
      관리자 승인 후 초과근무로 반영돼요.
    </p>

    <footer class="form-ft">
      <button type="button" class="btn btn--x" @click="$emit('cancel')">취소</button>
      <button type="submit" class="btn btn--p" :disabled="!isValid || submitting">
        {{ submitting ? '등록 중...' : '요청하기' }}
      </button>
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
import { formatYmdDisplay } from '@/utils/approvalFormat'
import { formatTimeSummary } from '@/views/attd/attdFormat'

const props = defineProps({
  context: { type: Object, required: true },
  submitting: { type: Boolean, default: false },
  // prafta-app-009: 본인 소유 결재선 프리셋([{ presetId, presetNm, defaultYn, steps[] }]).
  presets: { type: Array, default: () => [] },
  // prafta-app-009: 결재선 분기 컨텍스트 { selfApprvYn:'Y'|'N', isNodeAdmin:bool } | null(미상).
  approvalContext: { type: Object, default: null },
  // prafta-app-030: 이미 등록(적용)된 초과근무 목록([{ startDate, startTime, endDate, endTime, otStatus, workMinutes }]).
  //   표시(읽기전용) + 신규 슬롯 겹침 경고/제출 차단(오버나이트 인지). 빈 배열이면 표시/경고 모두 없음.
  existingOvertimes: { type: Array, default: () => [] },
  // prafta-app-030 후속: 대기중(미승인) OT 신청([{ startDate, startTime, endDate, endTime }]).
  //   표시 전용 — 겹침 사전차단 비대상(같은 날 대기 OT 등록은 서버 countDuplicateReq 가 ATTD_400_090 으로 신규 제출 자체를 차단).
  pendingOvertimes: { type: Array, default: () => [] },
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

// HHMM(4자리) → "HH:MM" 표시. 형식 위반 시 ''.
function hhmmDisplay(hhmm) {
  if (!hhmm || hhmm.length !== 4) return ''
  if (!/^\d{4}$/.test(hhmm)) return ''
  return `${hhmm.slice(0, 2)}:${hhmm.slice(2)}`
}

// ── #1 근태 기반 프리필 ─────────────────────────────────────────────────
// ⚠️ 백엔드 attendance 응답 키는 checkInDate/checkInTime/checkOutDate/checkOutTime 이다.
//    (AttdCorrectionForm 이 쓰던 attendance.startTime/endTime 은 잘못된 키 — 본 폼은 정확한 키 사용.)
const slotFromContext = (s, idx) => ({
  workSeq: s?.workSeq ?? idx + 1,
  startDate: ymdToInput(s?.attendance?.checkInDate || props.context.workYmd),
  startTime: hhmmToTime(s?.attendance?.checkInTime),
  endDate: ymdToInput(s?.attendance?.checkOutDate || props.context.workYmd),
  endTime: hhmmToTime(s?.attendance?.checkOutTime),
})

const buildInitialSlots = () => {
  const ctxSlots = props.context?.slots || []
  if (!Array.isArray(ctxSlots) || ctxSlots.length === 0) {
    // 폴백: 슬롯 정보 없음 → 1구간 빈 카드.
    return [makeEmptySlot(1)]
  }
  // 존재하는 구간 모두를 카드로 생성(최대 2). attendance 없는 구간은 시각 공란 카드.
  return ctxSlots.slice(0, 2).map((s, i) => slotFromContext(s, i))
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

// ── 결재선 상태 (prafta-app-009) ─────────────────────────────────────────
const approverList = ref([]) // [{ approverUserCd, userNm, userId, rankNm, nodeNm }] (순서 = 결재 단계)
const approverPickerOpen = ref(false)
const approvalSectionRef = ref(null)

const selfApprvYn = computed(() => props.approvalContext?.selfApprvYn || null)
const showApprovalSection = computed(() => selfApprvYn.value !== 'Y')
const approvalNotice = computed(() => {
  if (selfApprvYn.value !== 'Y') return ''
  return props.approvalContext?.isNodeAdmin
    ? '요청하면 즉시 승인 처리돼요.'
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

// ── 컨텍스트 표시 ───────────────────────────────────────────────────────
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

// ── 구간별 등록 가능 시간 (칩 — 탭하면 입력칸에 자동 반영) ──────────────
// 산식(plan §0-4, 웹 Attd_07 일자상세 팝업과 동일): 등록 가능 OT = 실근태 − 스케줄.
//   앞 OT = 실출근~스케줄시작, 뒤 OT = 스케줄종료~실퇴근.
//   schedule == null 인 구간 → 그 구간 실근무 전체가 등록 가능(§9.3.3 "스케줄 없는 날 전량").
//   반환: [{ startYmd, startHhmm, endYmd, endHhmm, label }] — 칩 표시와 자동 입력에 함께 쓴다.
//   ⚠️ 계산 실패는 차단이 아니라 무음(빈 배열) 처리 — 사용자가 직접 입력하는 길을 막지 않는다.
//   경계 판정은 전부 (일자+시각) stamp 로 한다. 시:분만 비교하면 야간/자정 넘김(전날 출근,
//   0시 시작 스케줄, 익일 퇴근 등)에서 구간이 통째로 누락되거나 날짜가 틀어진다.
const slotWindows = (workSeq) => {
  try {
    const ctxSlots = props.context?.slots || []
    const ctx = ctxSlots.find((s, i) => (s?.workSeq ?? i + 1) === workSeq)
    if (!ctx) return []

    const attendance = ctx.attendance
    const schedule = ctx.schedule
    // 근태(실 출퇴근)가 없으면 등록가능 윈도우 산출 불가.
    if (!attendance) return []

    const workYmd = props.context.workYmd
    const inHhmm = attendance.checkInTime
    const outHhmm = attendance.checkOutTime
    const inYmd = attendance.checkInDate || workYmd
    const outYmd = attendance.checkOutDate || workYmd

    const inStamp = stampOf(inYmd, inHhmm)
    const outStamp = stampOf(outYmd, outHhmm)
    // 출근만 있고 퇴근 전이거나 실근무 0분(출근=퇴근)이면 등록할 초과근무가 없다 → 칩 없음.
    if (Number.isNaN(inStamp) || Number.isNaN(outStamp) || outStamp <= inStamp) return []
    const actual = [inStamp, outStamp]

    // 스케줄 없는 구간(추가 출근 등) → 실근무 전체가 등록 가능.
    if (!schedule || (!schedule.startTime && !schedule.endTime)) {
      return [makeWindow(actual[0], actual[1])]
    }

    const schStartStamp = stampOf(workYmd, schedule.startTime)
    let schEndStamp = stampOf(workYmd, schedule.endTime)
    if (Number.isNaN(schStartStamp) || Number.isNaN(schEndStamp)) return []
    // 자정을 넘기는 스케줄(종료 < 시작)은 종료가 다음날이다.
    if (schEndStamp < schStartStamp) schEndStamp += 1440

    // 실근태 − 스케줄 (구간 차집합). 스케줄 밖에서 실제로 일한 시간만 등록 가능하다.
    //   "스케줄 종료 ~ 실퇴근" 으로 잡으면 실제로 근무하지 않은 공백(예: 스케줄 18:00 종료 후
    //   19:50 에 출근한 경우의 18:00~19:50)까지 포함돼 웹/백엔드 산출과 어긋난다.
    return subtractInterval(actual, [schStartStamp, schEndStamp]).map(([s, e]) => makeWindow(s, e))
  } catch (e) {
    // 표시 전용 — 계산 실패는 무음 처리.
    return []
  }
}

// 구간 차집합 (a − b). 웹 AttdDayDetailPop.subtractIntervals / 백엔드 AttdScheduleUtils 와 동일 규칙.
//   구간이 하나씩이므로 앞/뒤 잔여 조각(최대 2개)만 나온다. 겹치지 않으면 a 를 그대로 돌려준다.
function subtractInterval([aStart, aEnd], [bStart, bEnd]) {
  const out = []
  if (bStart > aStart) out.push([aStart, Math.min(bStart, aEnd)])
  if (bEnd < aEnd) out.push([Math.max(bEnd, aStart), aEnd])
  // 길이 0 이하(빈 조각) 제거.
  return out.filter(([s, e]) => e > s)
}

// stamp 쌍 → 칩 1건({ startYmd, startHhmm, endYmd, endHhmm, label }).
function makeWindow(startStamp, endStamp) {
  const startYmd = stampToYmd(startStamp)
  const startHhmm = stampToHhmm(startStamp)
  const endYmd = stampToYmd(endStamp)
  const endHhmm = stampToHhmm(endStamp)
  // 날짜가 다르면(자정 넘김) 종료 쪽에 (익일) 표시를 붙여 사용자가 헷갈리지 않게 한다.
  const nextDay = endYmd !== startYmd
  const label = `${hhmmDisplay(startHhmm)}~${hhmmDisplay(endHhmm)}${nextDay ? ' (익일)' : ''}`
  return { startYmd, startHhmm, endYmd, endHhmm, label }
}

// 칩 탭 → 해당 구간의 시작/종료 입력칸을 그 범위로 채운다(웹 addOtFromWindow 와 동형).
//   이미 같은 값이면 그대로 둔다(멱등). 사용자가 이후 수동으로 조정하는 건 자유.
const applyWindow = (workSeq, w) => {
  const slot = slots.value.find((s) => s.workSeq === workSeq)
  if (!slot || !w) return
  slot.startDate = ymdToInput(w.startYmd)
  slot.startTime = hhmmToTime(w.startHhmm)
  slot.endDate = ymdToInput(w.endYmd)
  slot.endTime = hhmmToTime(w.endHhmm)
}

// HHMM(4자리) → 분. 형식 위반 시 -1.
function toMin(hhmm) {
  if (!hhmm || hhmm.length !== 4 || !/^\d{4}$/.test(hhmm)) return -1
  const h = Number(hhmm.slice(0, 2))
  const m = Number(hhmm.slice(2))
  if (h > 23 || m > 59) return -1
  return h * 60 + m
}

// YYYYMMDD + HHMM → 통합 분(minute) stamp. 일자+시각을 함께 비교(자정/야간 넘김 안전).
//   형식 위반 시 NaN(호출부에서 Number.isNaN 으로 가드).
function stampOf(ymd, hhmm) {
  if (!ymd || ymd.length !== 8 || !/^\d{8}$/.test(ymd)) return NaN
  const m = toMin(hhmm)
  if (m < 0) return NaN
  const y = Number(ymd.slice(0, 4))
  const mo = Number(ymd.slice(4, 6))
  const d = Number(ymd.slice(6, 8))
  const days = Math.round(Date.UTC(y, mo - 1, d) / 86400000)
  return days * 1440 + m
}

// stamp → YYYYMMDD. stampOf 의 역변환(동일하게 UTC 기준 — 로컬 타임존 영향 없음).
function stampToYmd(stamp) {
  const days = Math.floor(stamp / 1440)
  const dt = new Date(days * 86400000)
  const y = dt.getUTCFullYear()
  const mo = String(dt.getUTCMonth() + 1).padStart(2, '0')
  const d = String(dt.getUTCDate()).padStart(2, '0')
  return `${y}${mo}${d}`
}

// stamp → HHMM(그날 0시 기준 분).
function stampToHhmm(stamp) {
  const m = ((stamp % 1440) + 1440) % 1440
  const h = String(Math.floor(m / 60)).padStart(2, '0')
  const mm = String(m % 60).padStart(2, '0')
  return `${h}${mm}`
}

// ── 겹침 경고 (2슬롯) ────────────────────────────────────────────────────
//   prafta-app-030: 기존 시각 문자열 단순비교(s2Start < s1End)는 오버나이트(날짜 넘김)를 무시하는 버그였다.
//   stampOf(YYYYMMDD, HHMM)(일자+시각 결합)로 1구간 종료 > 2구간 시작 인지를 정확히 판정한다(자정 넘김 안전).
//   slots.value 의 startDate/startTime 은 input 포맷 → inputToYmd()/timeToHhmm() 로 변환 후 stamp 화.
//   ⚠️ workSeq 식별자로 1·2 구간을 매칭(위치 index 금지).
const overlapWarning = computed(() => {
  if (slots.value.length < 2) return false
  const s1 = slots.value.find((s) => s.workSeq === 1)
  const s2 = slots.value.find((s) => s.workSeq === 2)
  if (!s1 || !s2) return false
  const s1End = stampOf(inputToYmd(s1.endDate), timeToHhmm(s1.endTime))
  const s2Start = stampOf(inputToYmd(s2.startDate), timeToHhmm(s2.startTime))
  // 어느 값이라도 NaN(미입력/형식위반) → 경고 안 함(입력 완료 후 판정).
  if (Number.isNaN(s1End) || Number.isNaN(s2Start)) return false
  return s2Start < s1End
})

// ── prafta-app-030: 기존 적용 OT 표시/겹침 ───────────────────────────────
// 기존 OT 1건을 stamp 구간 [start,end] 으로 환산(오버나이트는 startDate/endDate 가 일자를 각각 보유).
//   파싱 불가(형식 이상) 시 null(겹침 판정에서 건너뜀).
function existingOtStamps(ot) {
  if (!ot) return null
  const exStart = stampOf(ot.startDate, ot.startTime)
  const exEnd = stampOf(ot.endDate, ot.endTime)
  if (Number.isNaN(exStart) || Number.isNaN(exEnd)) return null
  return { exStart, exEnd }
}

// 표시용 목록(읽기전용 섹션). 시각 표기는 규약(점 YYYY.MM.DD / 콜론 HH:mm).
//   오버나이트면 종료에 날짜를 동반 표기, 같은 날이면 시각만.
const existingOvertimeDisplays = computed(() =>
  (props.existingOvertimes || []).map((ot, i) => {
    const sameDay = ot.startDate && ot.endDate && ot.startDate === ot.endDate
    const startTxt = `${formatYmdDisplay(ot.startDate)} ${hhmmDisplay(ot.startTime)}`
    const endTxt = sameDay
      ? hhmmDisplay(ot.endTime)
      : `${formatYmdDisplay(ot.endDate)} ${hhmmDisplay(ot.endTime)}`
    return {
      key: `${ot.startDate || ''}-${ot.startTime || ''}-${ot.endDate || ''}-${ot.endTime || ''}-${i}`,
      rangeText: `${startTxt} ~ ${endTxt}`,
    }
  }),
)

// prafta-app-030 후속: 대기중(미승인) OT 표시 목록. existingOvertimeDisplays 와 동일 패턴(점/콜론 규약).
//   각 item 에 pending:true 플래그 부여(템플릿 "대기중" 배지용). 표시 전용 — 겹침 계산에 미사용.
const pendingOvertimeDisplays = computed(() =>
  (props.pendingOvertimes || []).map((ot, i) => {
    const sameDay = ot.startDate && ot.endDate && ot.startDate === ot.endDate
    const startTxt = `${formatYmdDisplay(ot.startDate)} ${hhmmDisplay(ot.startTime)}`
    const endTxt = sameDay
      ? hhmmDisplay(ot.endTime)
      : `${formatYmdDisplay(ot.endDate)} ${hhmmDisplay(ot.endTime)}`
    return {
      key: `p-${ot.startDate || ''}-${ot.startTime || ''}-${ot.endDate || ''}-${ot.endTime || ''}-${i}`,
      rangeText: `${startTxt} ~ ${endTxt}`,
      pending: true,
    }
  }),
)

// 신규 슬롯 1건이 기존 적용 OT 와 겹치는지(오버나이트 인지). 겹침: otStart < exEnd && exStart < otEnd(접함 허용).
//   slots.value 는 input 포맷 → 변환 후 stamp 화. 계산 불가(NaN) 슬롯은 false(BE 가 최종 차단).
const slotExistingOverlap = (workSeq) => {
  const list = props.existingOvertimes || []
  if (list.length === 0) return false
  const slot = slots.value.find((s) => s.workSeq === workSeq)
  if (!slot) return false
  const otStart = stampOf(inputToYmd(slot.startDate), timeToHhmm(slot.startTime))
  const otEnd = stampOf(inputToYmd(slot.endDate), timeToHhmm(slot.endTime))
  if (Number.isNaN(otStart) || Number.isNaN(otEnd)) return false
  return list.some((ex) => {
    const st = existingOtStamps(ex)
    if (!st) return false
    return otStart < st.exEnd && st.exStart < otEnd
  })
}

// 어느 한 구간이라도 기존 적용 OT 와 겹치면 true(제출 차단/경고).
const hasExistingOverlap = computed(() => slots.value.some((s) => slotExistingOverlap(s.workSeq)))

// ── #프라프타-app-017(이슈①) 정규 스케줄 겹침 사전차단 ───────────────────────
//   OT [start,end] ∩ 정규스케줄[schStart,schEnd] ≠ ∅ 이면 제출 비활성 + 경고.
//   서버(작업1)가 최종 권위. FE 는 UX 사전차단이므로 계산 불가(NaN) 시 false(비차단) — BE 가 막는다.
//   ⚠️ workSeq 식별자로 schedule/slot 매칭(위치 index 금지).
//   ⚠️ slots.value 의 startDate/startTime 은 input 포맷(YYYY-MM-DD / HH:MM) →
//      inputToYmd()/timeToHhmm() 로 변환 후 stampOf(YYYYMMDD, HHMM) 에 전달.
const slotOverlap = (workSeq) => {
  try {
    const ctxSlots = props.context?.slots || []
    const ctx = ctxSlots.find((s, i) => (s?.workSeq ?? i + 1) === workSeq)
    const schedule = ctx?.schedule
    // 정규구간 없음(스케줄 없는 날/구간) → 겹침 아님.
    if (!schedule || (!schedule.startTime && !schedule.endTime)) return false

    const slot = slots.value.find((s) => s.workSeq === workSeq)
    if (!slot) return false

    // 입력값(input 포맷) → YYYYMMDD / HHMM 변환 후 인스턴트화.
    const otStart = stampOf(inputToYmd(slot.startDate), timeToHhmm(slot.startTime))
    const otEnd = stampOf(inputToYmd(slot.endDate), timeToHhmm(slot.endTime))

    // 스케줄 인스턴트(근무일 기준). 종료 ≤ 시작 → 익일 보정(야간).
    const schStartStamp = stampOf(props.context.workYmd, schedule.startTime)
    let schEndStamp = stampOf(props.context.workYmd, schedule.endTime)
    if (
      !Number.isNaN(schEndStamp) &&
      !Number.isNaN(schStartStamp) &&
      schEndStamp <= schStartStamp
    ) {
      schEndStamp += 1440
    }

    // 어느 값이라도 NaN → 계산 불가 → false(차단 안 함, BE 최종 판정).
    if (
      Number.isNaN(otStart) ||
      Number.isNaN(otEnd) ||
      Number.isNaN(schStartStamp) ||
      Number.isNaN(schEndStamp)
    ) {
      return false
    }

    // 겹침: otStart < schEnd && schStart < otEnd (접함 허용).
    return otStart < schEndStamp && schStartStamp < otEnd
  } catch (e) {
    // 사전차단 계산 실패는 무음(BE 가 최종 차단).
    return false
  }
}

// 어느 한 구간이라도 정규 스케줄과 겹치면 true.
const hasOverlap = computed(() => slots.value.some((s) => slotOverlap(s.workSeq)))

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

// ── 검증 (#2: otType 조건 제거 / prafta-app-017: 스케줄 겹침 사전차단) ─────
const isValid = computed(() => {
  // 사유 미입력은 버튼 비활성 사유에서 제외(제출 시 사유 전용 alert 로 안내).
  if (hasOverlap.value) return false
  // prafta-app-030: 신규 슬롯이 기존 적용 OT 또는 2구간 상호 간 겹치면 제출 차단.
  if (hasExistingOverlap.value) return false
  if (overlapWarning.value) return false
  if (!slots.value.every((s) => s.startDate && s.startTime && s.endDate && s.endTime)) return false
  // 결재 필수('N') 케이스는 결재자 1명 이상이어야 제출 활성.
  if (approverRequired.value && approverList.value.length === 0) return false
  return true
})

// ── 구간 추가/삭제 (workSeq 식별자 보존) ────────────────────────────────
const onAddSlot = () => {
  if (slots.value.length >= 2) return
  const existing = new Set(slots.value.map((s) => s.workSeq))
  const missing = [1, 2].find((n) => !existing.has(n))
  if (!missing) return
  // 추가 구간도 context.slots 의 해당 workSeq 근태로 프리필, 없으면 빈 카드.
  const ctxSlots = props.context?.slots || []
  const idx = ctxSlots.findIndex((s, i) => (s?.workSeq ?? i + 1) === missing)
  const added = idx >= 0 ? slotFromContext(ctxSlots[idx], idx) : makeEmptySlot(missing)
  added.workSeq = missing
  slots.value.push(added)
  slots.value.sort((a, b) => a.workSeq - b.workSeq)
}
const onRemoveSlot = (workSeq) => {
  slots.value = slots.value.filter((s) => s.workSeq !== workSeq)
}

// ── 제출 (#2: emit 에서 otType 제거) ────────────────────────────────────
const onSubmit = () => {
  // prafta-app-017(이슈①): 정규 스케줄 겹침은 우선 안내(서버도 ATTD_400_100 으로 최종 차단).
  if (hasOverlap.value) {
    showAlert('스케줄 시간 내에는 초과근무를 등록할 수 없어요.')
    return
  }
  // prafta-app-030: 2구간 상호 겹침(오버나이트 인지) 안내.
  if (overlapWarning.value) {
    showAlert('2구간 시작 시각은 1구간 종료 시각 이후여야 합니다.')
    return
  }
  // prafta-app-030: 기존 적용 초과근무와 시간 겹침 안내(서버도 ATTD_409_002 로 최종 차단).
  if (hasExistingOverlap.value) {
    showAlert('이미 등록된 초과근무와 시간이 겹칩니다. 시간이 겹치지 않도록 입력해 주세요.')
    return
  }
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

/* prafta-app-030: 이미 등록된 초과근무(읽기전용) */
.existing-ot {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.existing-ot__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.existing-ot__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.existing-ot__item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-bg);
  border: 0.5px solid var(--color-border-light);
  border-radius: var(--radius-md);
}
.existing-ot__range {
  font-size: 13px;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
/* prafta-app-030 후속: 대기중(미승인) OT 구분 배지 — neutral/secondary 톤(CSS 변수만). */
.ot-badge-pending {
  margin-left: auto;
  flex-shrink: 0;
  padding: 2px var(--space-sm);
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}
.existing-ot__hint {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
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
/* 등록 가능 범위 칩 — 탭하면 시작/종료 입력칸이 그 범위로 채워진다. */
.ot-window__list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
  margin: 0;
  padding: 0;
  list-style: none;
}
.ot-window__chip {
  padding: 4px 10px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-primary);
  border-radius: var(--radius-full);
  color: var(--color-primary-text-darkest);
  font-family: inherit;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
}
.ot-window__chip:active {
  background: var(--color-primary-tint-border);
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
