<!--
  LeaveApplyForm.vue — 연차 신청 폼 (prafta-app-018-C, 화면 명세 UI-018C-1)
  - 분해: .claude/requests/app_requests/prafta-app-018-C-tasks.md
  - 역할: 프레젠테이션 폼(입력/표시/검증). API 호출/라우팅은 부모(LeaveApplyView)가 담당.
  - 참조 패턴: views/req/components/OvertimeForm.vue · AttdCorrectionForm.vue
      (컨텍스트 박스 + 필드 + sticky 푸터 + DateStepperField/TimeStepperField + emit submit/cancel)
  - props:
      meta        : 018-A apply-meta 응답 ({ leaveTypes:[...] })
      presets     : 018-A approval-presets 응답의 presets 배열
      context     : 진입 컨텍스트 ({ workYmd?, nodeCd?, siteName?, scheduleSummary?, slots? }) — 없을 수 있음
      submitting  : 제출 진행 플래그(부모 소유)
      preview        : LC-10 예상 차감 preview 응답({ chargeDays, floorApplied, capApplied,
                       insufficientBalance, convMinutes, floorDays }) — 부모 소유. 실패/비대상이면 null(표시 생략)
      previewLoading : preview 호출 진행 플래그(부모 소유)
  - emits:
      submit({ leaveCd, leaveType, workYmd, useUnitType, startTime, endTime, reason,
               approverUserCds, presetId })   ← 018-B POST /appApi/leaveflow/apply 요청 본문 키와 1:1
      cancel
      preview-request(payload|null)          ← LC-10: 시간차/반반차 입력 완성 시 디바운스 후 emit.
                                                null 이면 preview 표시 해제(입력 미완성/비대상 단위)
  - ⚠️ allowedUnits/balanceDays/aprvRequired 는 전부 서버(meta) 권위. 클라 추측 금지.
  - ⚠️ 종일(00)/반차(01)/시간차(02·03·04) 분기는 선택된 종류의 allowedUnits 안에서만.
       종일/반차 편의버튼은 시작/종료 시각을 자동입력(표시·BE 차감용)하되 제출 useUnitType 은 단위코드 그대로.
  - ⚠️ 결재자/프리셋 step 의 approverUserCd 는 식별자다. 위치 index 로 재인덱싱하지 않는다(서버가 STEP_NO=배열 순서로 INSERT).
-->
<template>
  <form class="lvf" @submit.prevent="onSubmit">
    <!-- 컨텍스트 박스 (특정 일자 진입 시) -->
    <section v-if="hasContext" class="ctx">
      <p class="ctx__date">
        <strong>{{ ctxDateDisplay }}</strong>
        <small>{{ ctxSiteDisplay }}</small>
      </p>
      <div v-if="context.scheduleSummary" class="ctx__row">
        <span class="ctx__lbl">스케줄</span>
        <span class="ctx__val">{{ context.scheduleSummary }}</span>
      </div>
    </section>

    <!-- 1) 연차 종류 -->
    <section class="fs">
      <p class="fs__title">연차 종류</p>
      <div class="type-list">
        <button
          v-for="lt in leaveTypes"
          :key="lt.leaveCd"
          type="button"
          class="type-item"
          :class="{
            'type-item--on': selectedLeaveCd === lt.leaveCd,
            'type-item--off': !lt.applicable,
          }"
          :disabled="!lt.applicable"
          @click="onSelectType(lt)"
        >
          <span class="type-item__name">{{ lt.leaveNm }}</span>
          <span class="type-item__bal">잔여 {{ formatLeaveDays(lt.balanceDays, metaConvMinutes) }}</span>
        </button>

        <p v-if="leaveTypes.length === 0" class="fs__empty">신청 가능한 연차 종류가 없어요</p>
      </div>
    </section>

    <!-- 종류 선택 이후 노출되는 본문 -->
    <template v-if="selectedType">
      <!-- 잔여 요약 -->
      <div class="balance-box">
        <span class="balance-box__lbl">선택한 연차 잔여</span>
        <span class="balance-box__val">{{ formatLeaveDays(selectedType.balanceDays, metaConvMinutes) }}</span>
      </div>

      <!-- 2) 사용 단위 (allowedUnits 게이팅) -->
      <section class="fs">
        <p class="fs__title">사용 단위</p>
        <div class="unit-list">
          <button
            v-for="u in unitOptions"
            :key="u.code"
            type="button"
            class="unit-chip"
            :class="{ 'unit-chip--on': useUnitType === u.code }"
            @click="onSelectUnit(u.code)"
          >
            {{ u.label }}
          </button>
        </div>
      </section>

      <!-- 3) 날짜 -->
      <section class="fs">
        <p class="fs__title">신청 일자</p>
        <label class="field">
          <span class="field__label"><span class="req">*</span>날짜</span>
          <DateStepperField v-model="workDateInput" placeholder="날짜 선택" />
        </label>
      </section>

      <!-- 4) 시간차 단위(02·03·04) — 시작~종료 시각 -->
      <section v-if="isTimeUnit" class="fs">
        <div class="time-head">
          <p class="fs__title">신청 시간</p>
          <!-- 종일/반차 편의버튼: allowedUnits 에 해당 단위가 있을 때만 노출.
               누르면 시각 자동입력(시작=스케줄시작, 종일=스케줄종료/반차=절반) — 계산은 developer. -->
          <div class="quick-btns">
            <button
              v-if="canQuickFullDay"
              type="button"
              class="quick-btn"
              @click="onQuickFill('00')"
            >
              종일
            </button>
            <button
              v-if="canQuickHalfDay"
              type="button"
              class="quick-btn"
              @click="onQuickFill('01')"
            >
              반차
            </button>
          </div>
        </div>

        <label class="field">
          <span class="field__label"><span class="req">*</span>시작</span>
          <TimeStepperField v-model="startTimeInput" :step="30" placeholder="시작 시각" />
        </label>
        <label class="field">
          <span class="field__label"><span class="req">*</span>종료</span>
          <div class="end-stepper">
            <button
              type="button"
              class="end-stepper__btn"
              aria-label="종료 시각 줄이기"
              :disabled="stepCount <= 1"
              @click="onStepDown"
            >
              −
            </button>
            <span class="end-stepper__val">{{ endTimeInput || '--:--' }}</span>
            <span class="end-stepper__n">{{ stepCount }}{{ unitShortLabel }}</span>
            <button
              type="button"
              class="end-stepper__btn"
              aria-label="종료 시각 늘리기"
              :disabled="!canStepUp"
              @click="onStepUp"
            >
              +
            </button>
          </div>
        </label>

        <p class="time-guide">
          <span class="time-guide__dot" aria-hidden="true">·</span>
          {{ unitGuideText }}
        </p>
      </section>

      <!-- 4-1) 가불(미래 연차 당겨쓰기) 동의 — 시스템 법정 연차 + 가불 가능 + 잔여 부족 시에만 노출 (prafta-com-011-4) -->
      <section v-if="showBorrowToggle" class="fs">
        <label class="borrow-toggle">
          <input
            v-model="borrowAgreed"
            type="checkbox"
            class="borrow-toggle__cb"
          />
          <span class="borrow-toggle__txt">미래 연차를 당겨 사용(가불)</span>
        </label>

        <!-- 토글 ON 시: 가불 한도/만료 안내 -->
        <div v-if="borrowAgreed" class="borrow-info">
          <div class="borrow-info__row">
            <span class="borrow-info__lbl">가불 가능 한도</span>
            <span class="borrow-info__val">{{ formatDays(borrowQuota) }}일</span>
          </div>
          <div v-if="borrowExpiryDisplay" class="borrow-info__row">
            <span class="borrow-info__lbl">만료(소멸)</span>
            <span class="borrow-info__val">{{ borrowExpiryDisplay }}</span>
          </div>
          <p v-if="borrowDeficitText" class="borrow-info__deficit">{{ borrowDeficitText }}</p>
          <p class="borrow-info__guide">
            <span class="borrow-info__dot" aria-hidden="true">·</span>
            결재 승인 후 확정돼요. 미래에 발생할 연차에서 자동 차감됩니다.
          </p>
        </div>
      </section>

      <!-- 5) 사유 -->
      <section class="fs">
        <label class="field">
          <span class="field__label">
            신청 사유
            <span class="field__help">{{ reason.length }}/500</span>
          </span>
          <textarea
            v-model="reason"
            class="field__textarea"
            placeholder="사유를 입력해 주세요."
            maxlength="500"
            rows="4"
          ></textarea>
        </label>
      </section>

      <!-- 6) 결재선 (aprvRequired 종류만) -->
      <section v-if="aprvRequired" class="fs">
        <p class="fs__title">결재선</p>

        <!-- 프리셋 선택 -->
        <div v-if="presets.length > 0" class="preset-list">
          <button
            v-for="p in presets"
            :key="p.presetId"
            type="button"
            class="preset-chip"
            :class="{ 'preset-chip--on': selectedPresetId === p.presetId }"
            @click="onSelectPreset(p)"
          >
            {{ p.presetNm }}
            <span v-if="p.defaultYn" class="preset-chip__tag">기본</span>
          </button>
        </div>

        <!-- 결재자 순서 리스트 -->
        <ul v-if="approverList.length > 0" class="aprv-list">
          <li v-for="(ap, idx) in approverList" :key="ap.approverUserCd" class="aprv-row">
            <span class="aprv-row__step">{{ idx + 1 }}</span>
            <div class="aprv-row__info">
              <p class="aprv-row__name">{{ ap.userNm }}</p>
              <p class="aprv-row__meta">{{ approverMetaOf(ap) }}</p>
            </div>
            <button
              type="button"
              class="aprv-row__del"
              aria-label="결재자 제거"
              @click="onRemoveApprover(ap.approverUserCd)"
            >
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
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </li>
        </ul>
        <p v-else class="aprv-empty">결재자를 추가해 주세요</p>

        <button type="button" class="btn-add" @click="onOpenApproverPicker">
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
          결재자 추가
        </button>
      </section>

      <!-- 잔여 초과 사전 안내 (서버 051 도 표면화) -->
      <p v-if="overBalanceWarning" class="warn-msg">
        신청 일수가 남은 연차보다 많아요. 신청이 거절될 수 있어요.
      </p>

      <!-- LC-10: 예상 차감 요약 카드 (시간차/반반차 preview — 신청 버튼 위, plan §5-D).
           preview 실패 시 카드 미노출(신청은 가능 — 서버가 최종 판정). -->
      <section v-if="showPreviewCard" class="preview-card" aria-live="polite">
        <p v-if="previewLoading" class="preview-card__loading">예상 차감 계산 중...</p>
        <template v-else-if="preview">
          <div class="preview-card__row">
            <span class="preview-card__lbl">예상 차감</span>
            <span class="preview-card__val">{{ previewChargeText }}</span>
          </div>
          <p v-if="preview.floorApplied" class="preview-card__floor">
            {{ floorNoticeText }}
          </p>
          <p v-if="preview.insufficientBalance" class="preview-card__warn">
            예상 차감이 남은 연차를 초과해요. 이대로 신청하면 거절될 수 있어요.
          </p>
        </template>
      </section>
    </template>

    <p class="helper">
      <span class="helper__dot" aria-hidden="true">·</span>
      {{ helperText }}
    </p>

    <footer class="form-ft">
      <button type="button" class="btn btn--x" @click="$emit('cancel')">취소</button>
      <button type="submit" class="btn btn--p" :disabled="!isValid || submitting">
        {{ submitting ? '신청 중...' : '신청하기' }}
      </button>
    </footer>

    <!-- 결재자 추가 바텀시트 (참조: PresetApproverPickerSheet.vue) -->
    <!-- developer: 후보 검색은 018-A GET /appApi/leaveflow/approver-search?keyword=&page=&size= -->
    <LeaveApproverPickerSheet
      v-if="aprvRequired"
      v-model="approverPickerOpen"
      :excluded-user-cds="approverUserCds"
      @add="onAddApprovers"
    />
  </form>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, getCurrentInstance } from 'vue'
import { formatLeaveDays, trimRawDays } from '@/utils/leaveFormat'
import DateStepperField from '@/components/common/DateStepperField.vue'
import TimeStepperField from '@/components/common/TimeStepperField.vue'
// developer: 결재자 추가 시트(LeaveApproverPickerSheet)는 본 작업의 후속 골격 또는
//            mypage PresetApproverPickerSheet 를 leaveflow approver-search 엔드포인트로 재구성하여 사용.
//            (prafta-app-018-C-tasks.md §결재자 시트 참조 — 신규 시트 골격 본 파일과 함께 작성)
import LeaveApproverPickerSheet from './LeaveApproverPickerSheet.vue'

const props = defineProps({
  // 018-A apply-meta 응답: { leaveTypes: [{ leaveCd, leaveNm, systemYn, aprvRequired, allowedUnits[], balanceDays, applicable }],
  //   convMinutes(오늘 기준 1일 환산시간(분) — 잔여 표기용 근사치, 구응답이면 부재 → 480 폴백) }
  meta: { type: Object, default: () => ({ leaveTypes: [] }) },
  // 018-A approval-presets 응답의 presets 배열: [{ presetId, presetNm, defaultYn, steps:[{ stepNo, approverUserCd, userNm, userId, rankNm, nodeNm }] }]
  presets: { type: Array, default: () => [] },
  // 진입 컨텍스트(특정 일자 진입 시): { workYmd?, nodeCd?, siteName?, scheduleSummary?, slots? }
  context: { type: Object, default: () => ({}) },
  submitting: { type: Boolean, default: false },
  // LC-10: 예상 차감 preview 응답(부모 소유). { chargeDays, floorApplied, capApplied, insufficientBalance,
  //   convMinutes, floorDays(발동 마일스톤 요금 0.25/0.5/1 — 구응답이면 부재) }
  //   preview 실패/비대상이면 null — 표시 생략하고 신청은 가능(서버가 최종 판정).
  preview: { type: Object, default: null },
  // LC-10: preview 호출 진행 플래그(부모 소유) — 요약 카드 로딩 표시용.
  previewLoading: { type: Boolean, default: false },
})
const emit = defineEmits(['submit', 'cancel', 'preview-request'])

const { proxy } = getCurrentInstance() || { proxy: null }
// 공통: alert 폴백(앱 전역 $alert 우선, 없으면 window.alert) — LeaveApplyView 패턴 동일.
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 사용 단위 라벨(SYS025) — 표시 전용 상수 ──────────────────────────────
// 00 종일 / 01 반차 / 02 2시간 / 03 1시간 / 04 30분 / 05 반반차(LC-06 — ALLOW_QUARTER='Y' 회사만
//   서버가 allowedUnits 에 '05' 를 포함하므로, 노출 게이트는 기존 allowedUnits 패턴 그대로)
const UNIT_LABELS = {
  '00': '종일',
  '01': '반차',
  '02': '2시간',
  '03': '1시간',
  '04': '30분',
  '05': '반반차(0.25일)',
}

// 시간차 단위(02·03·04)별 1스텝 분량(분). 종료 = 시작 + N×단위분 계산에 사용.
const UNIT_MINUTES = {
  '02': 120,
  '03': 60,
  '04': 30,
}

// ── 반응형 상태 (developer: 초기값/리셋/시각 자동계산 로직 보완) ──────────
const selectedLeaveCd = ref('')
const useUnitType = ref('') // SYS025 코드
const workDateInput = ref('') // 'YYYY-MM-DD' (DateStepperField v-model)
const startTimeInput = ref('') // 'HH:MM' (TimeStepperField v-model, 30분 단위)
// 종료 시각 = 시작 + stepCount × 단위분. [+]/[−] 로 stepCount 조정(최소 1).
const stepCount = ref(1)
const reason = ref('')

// 가불(미래 연차 당겨쓰기) 동의 상태 (prafta-com-011-4). 종류/날짜 변경 시 리셋.
const borrowAgreed = ref(false)

// 결재선 상태
const selectedPresetId = ref('')
// approverList: [{ approverUserCd, userNm, userId, rankNm, nodeNm }] (순서 = 결재 단계)
const approverList = ref([])
const approverPickerOpen = ref(false)

// ── 파생값 (단순 표시/필터 — 비즈니스 로직 아님) ─────────────────────────
const leaveTypes = computed(() => props.meta?.leaveTypes || [])

// 잔여 "N일 H시간 M분" 표기용 환산시간(분) — apply-meta convMinutes(오늘 기준 근사치, 서버 산출).
//   구응답(필드 부재)/무효면 undefined → formatLeaveDays 내부 480 폴백.
const metaConvMinutes = computed(() => props.meta?.convMinutes)

const selectedType = computed(
  () => leaveTypes.value.find((t) => t.leaveCd === selectedLeaveCd.value) || null,
)

// 결재 필요 여부(선택 종류의 서버 플래그). 가불(borrowAgreed) ON 이면 체크박스 설정 무관하게 결재 강제(결정 §4).
const aprvRequired = computed(() => Boolean(selectedType.value?.aprvRequired) || borrowAgreed.value)

// 선택 종류 allowedUnits(서버 권위) → 표시용 옵션
const unitOptions = computed(() => {
  const allowed = selectedType.value?.allowedUnits || []
  return allowed.map((code) => ({ code, label: UNIT_LABELS[code] || code }))
})

// 시간차 단위 여부(02·03·04)
const isTimeUnit = computed(() => ['02', '03', '04'].includes(useUnitType.value))

// 시간차 단위 짧은 라벨(종료 스텝퍼 옆 'N2시간' 등 표시).
const unitShortLabel = computed(() => UNIT_LABELS[useUnitType.value] || '')

// 종료 시각(읽기전용 파생): 시작 미입력/비시간차면 ''. 아니면 시작 + stepCount×단위분(24h wrap).
//   minutesToInput() 재사용. 시작은 30분 단위 TimeStepperField 값.
const endTimeInput = computed(() => {
  if (!isTimeUnit.value) return ''
  const startM = toMinutes(startTimeInput.value)
  if (startM < 0) return ''
  const unitMin = UNIT_MINUTES[useUnitType.value]
  if (!unitMin) return ''
  return minutesToInput(startM + stepCount.value * unitMin)
})

// 종료 분이 자정(1440) 이상으로 넘어가는지(=익일 wrap) 판정. 시간차+시작입력+단위분 유효 전제.
//   1440(=24:00)은 minutesToInput 이 '00:00' 으로만 표현해 BE 가 받지 못하므로 1440 도 차단(유효 종료 ≤ 1439분).
const endOverflowsDay = computed(() => {
  if (!isTimeUnit.value) return false
  const startM = toMinutes(startTimeInput.value)
  if (startM < 0) return false
  const unitMin = UNIT_MINUTES[useUnitType.value]
  if (!unitMin) return false
  return startM + stepCount.value * unitMin >= 1440
})

// 한 단계 더 늘렸을 때 자정을 넘지 않는지(=종료 [+] 활성 가능). 시작 미입력/비시간차/단위분 무효면 false.
const canStepUp = computed(() => {
  if (!isTimeUnit.value) return false
  const startM = toMinutes(startTimeInput.value)
  if (startM < 0) return false
  const unitMin = UNIT_MINUTES[useUnitType.value]
  if (!unitMin) return false
  return startM + (stepCount.value + 1) * unitMin < 1440
})

// 종일/반차 편의버튼 노출 — allowedUnits 에 해당 단위가 있을 때만
const canQuickFullDay = computed(() => (selectedType.value?.allowedUnits || []).includes('00'))
const canQuickHalfDay = computed(() => (selectedType.value?.allowedUnits || []).includes('01'))

// 컨텍스트(특정 일자 진입) 유무
const hasContext = computed(() => Boolean(props.context?.workYmd))

const ctxDateDisplay = computed(() => {
  const ymd = props.context?.workYmd
  if (!ymd || ymd.length !== 8) return '-'
  return `${ymd.slice(0, 4)}년 ${Number(ymd.slice(4, 6))}월 ${Number(ymd.slice(6, 8))}일`
})
const ctxSiteDisplay = computed(() => props.context?.siteName || '')

// 단위별 안내 문구(시간차 단위 입력 영역)
const unitGuideText = computed(() => {
  const label = UNIT_LABELS[useUnitType.value] || ''
  // developer: 휴게시간 가로지름 불가 등 정책 문구 확정(attd §8.5). 골격은 기본 안내만.
  return `${label} 단위로 신청해 주세요. 휴게시간을 가로지를 수 없어요.`
})

// 결재자 emit 용 userCd 배열(순서 보존 — 위치 재인덱싱 아님, 표시 순서 그대로)
const approverUserCds = computed(() => approverList.value.map((a) => a.approverUserCd))

const helperText = computed(() =>
  aprvRequired.value
    ? '신청 후 결재선의 승인을 거쳐 연차로 반영돼요.'
    : '신청 시 바로 연차로 반영돼요.',
)

// ── 형식 유틸 (input 값 ↔ emit/스케줄 값) — OvertimeForm 패턴 차용 ─────────
// 'YYYY-MM-DD' → 'YYYYMMDD'
const toYmd = (s) => (s ? s.replace(/-/g, '') : '')
// 'HH:MM' → 'HHMM' (앞 4자리)
const toHHMM = (s) => (s ? s.replace(':', '').slice(0, 4) : '')
// 'HH:MM' → 분(minute). 형식 위반 시 -1.
const toMinutes = (s) => {
  if (!/^\d{2}:\d{2}/.test(s || '')) return -1
  const h = Number(s.slice(0, 2))
  const m = Number(s.slice(3, 5))
  if (Number.isNaN(h) || Number.isNaN(m) || h > 23 || m > 59) return -1
  return h * 60 + m
}
// 분 → 'HH:MM' (24시간 wrap). 자동입력(반차 절반)용.
const minutesToInput = (mins) => {
  let m = mins
  if (m < 0) m += 24 * 60
  m = m % (24 * 60)
  const h = Math.floor(m / 60)
  const mm = m % 60
  return `${String(h).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
}

// 컨텍스트 스케줄(첫 구간)의 시작/종료 시각(HHMM). 편의버튼 자동입력 출처. 없으면 null.
const contextSchedule = computed(() => {
  const slots = props.context?.slots || []
  if (!Array.isArray(slots) || slots.length === 0) return null
  const sch = slots[0]?.schedule
  if (!sch || (!sch.startTime && !sch.endTime)) return null
  return { startTime: sch.startTime || '', endTime: sch.endTime || '' }
})

// 신청 일수 추정(종일 1.0 / 반차 0.5 / 반반차 0.25 / 시간차 (종료-시작)분÷소정근로분). 계산 불가/미선택 시 null.
//   표시 전용 근사(서버가 최종 판정). 잔여초과 경고와 가불 토글 노출 판정의 단일출처.
const estimatedDays = computed(() => {
  if (!selectedType.value) return null
  if (useUnitType.value === '00') return 1.0
  if (useUnitType.value === '01') return 0.5
  if (useUnitType.value === '05') return 0.25 // 반반차(LC-06): 0.25일 고정단위
  if (isTimeUnit.value) {
    // 시간차: (종료-시작)분 ÷ 소정근로분. 소정근로 출처는 컨텍스트 스케줄(시작~종료), 휴게 미반영 근사.
    const startM = toMinutes(startTimeInput.value)
    const endM = toMinutes(endTimeInput.value)
    if (startM < 0 || endM < 0) return null
    const reqMin = endM - startM
    if (reqMin <= 0) return null
    const sch = contextSchedule.value
    const schStart = sch ? toMin4(sch.startTime) : -1
    const schEnd = sch ? toMin4(sch.endTime) : -1
    const workMin = schStart >= 0 && schEnd >= 0 ? schEnd - schStart : -1
    if (workMin <= 0) return null // 소정근로 산출 불가 → 추정 보류
    return reqMin / workMin
  }
  return null
})

// 잔여 초과 사전 경고 — 신청 일수 추정 > 선택 종류 balanceDays 면 true. 계산 불가 시 false.
const overBalanceWarning = computed(() => {
  const type = selectedType.value
  if (!type) return false
  const bal = Number(type.balanceDays)
  if (Number.isNaN(bal)) return false
  const est = estimatedDays.value
  if (est === null) return false
  return est > bal
})

// ── 가불(미래 연차 당겨쓰기) 파생값 (prafta-com-011-4) ─────────────────────
// 가불 한도(서버 권위, apply-meta borrowQuota). 비대상이면 0.
const borrowQuota = computed(() => Number(selectedType.value?.borrowQuota) || 0)

// 가불분 만료(소멸)일 YYYYMMDD(서버 산출). 없으면 ''.
const borrowExpiryYmd = computed(() => String(selectedType.value?.borrowExpiryYmd || ''))

// 만료일 표시(YYYY-MM-DD). 미산정이면 ''.
const borrowExpiryDisplay = computed(() => {
  const ymd = borrowExpiryYmd.value
  if (!ymd || ymd.length !== 8 || !/^\d{8}$/.test(ymd)) return ''
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
})

// 가불 토글 노출: 시스템 법정 연차(systemYn='Y') + 가불 가능(borrowable) + 잔여(balanceDays) 부족(추정 신청일수 초과).
//   잔여 충분이거나 추정 불가/비대상이면 미노출(결정 §6-1: 부족할 때만).
const showBorrowToggle = computed(() => {
  const type = selectedType.value
  if (!type) return false
  if (String(type.systemYn) !== 'Y') return false
  if (!type.borrowable) return false
  const bal = Number(type.balanceDays)
  const est = estimatedDays.value
  if (Number.isNaN(bal) || est === null) return false
  return est > bal
})

// 가불 충당(부족) 안내 텍스트 — 예: "남은 0일 + 가불 3일". 추정 불가/충분이면 ''.
const borrowDeficitText = computed(() => {
  const type = selectedType.value
  if (!type) return ''
  const bal = Number(type.balanceDays)
  const est = estimatedDays.value
  if (Number.isNaN(bal) || est === null) return ''
  const deficit = est - Math.max(0, bal)
  if (deficit <= 0) return ''
  return `남은 ${formatDays(Math.max(0, bal))}일 + 가불 ${formatDays(deficit)}일`
})

// 선택 일자가 가불 만료(소멸)일을 지났는지(가불 토글 ON 한정 가드). 만료 미산정이면 false.
const borrowDateExpired = computed(() => {
  if (!borrowAgreed.value) return false
  const exp = borrowExpiryYmd.value
  const ymd = toYmd(workDateInput.value)
  if (!exp || !ymd || ymd.length !== 8) return false
  return ymd > exp
})

// 가불 토글 노출 조건이 깨지면(잔여 충분/비대상 전환 등) 동의 자동 해제 — 잔존 동의 누수 방지.
watch(showBorrowToggle, (visible) => {
  if (!visible && borrowAgreed.value) borrowAgreed.value = false
})

// 가불 토글 ON + 만료 경과 일자 선택 → alert 안내 후 차단(결정 §3, 서버도 fail-closed). 날짜 초기화.
watch(borrowDateExpired, (expired) => {
  if (expired) {
    showAlert('가불 만료일이 지난 날짜에는 사용할 수 없어요.')
    workDateInput.value = ''
  }
})

// ── LC-10: 예상 차감 preview 요청 (시간차/반반차 — POST /appApi/leaveflow/preview-deduction) ──
// 입력 완성 시 디바운스 후 부모에 emit(API 호출은 부모 소유 — 컨테이너/폼 역할 분담 유지).
const PREVIEW_DEBOUNCE_MS = 400
let previewTimer = null

// preview 대상 payload(요청 본문 키 1:1). 비대상(종일/반차)·입력 미완성이면 null.
//   시간차(02/03/04) = 날짜 + 시작/종료 완성 + 자정 미초과일 때. 반반차(05) = 날짜만(시간대 미기록).
const previewPayload = computed(() => {
  if (!selectedType.value) return null
  const ymd = toYmd(workDateInput.value)
  if (!ymd || ymd.length !== 8) return null
  const unit = useUnitType.value
  if (unit === '05') {
    return {
      leaveCd: selectedLeaveCd.value,
      workYmd: ymd,
      useUnitType: unit,
      startTime: null,
      endTime: null,
    }
  }
  if (['02', '03', '04'].includes(unit)) {
    if (!startTimeInput.value || !endTimeInput.value || endOverflowsDay.value) return null
    return {
      leaveCd: selectedLeaveCd.value,
      workYmd: ymd,
      useUnitType: unit,
      startTime: toHHMM(startTimeInput.value),
      endTime: toHHMM(endTimeInput.value),
    }
  }
  return null
})

// payload 변경 → 디바운스 후 preview 요청 emit. null 전환은 즉시(잔존 카드 누수 방지).
watch(previewPayload, (payload) => {
  if (previewTimer) {
    clearTimeout(previewTimer)
    previewTimer = null
  }
  if (!payload) {
    emit('preview-request', null)
    return
  }
  previewTimer = setTimeout(() => {
    previewTimer = null
    emit('preview-request', payload)
  }, PREVIEW_DEBOUNCE_MS)
})

// 폼 해제 시 잔여 타이머 정리(unmount 후 emit 방지).
onUnmounted(() => {
  if (previewTimer) clearTimeout(previewTimer)
})

// 예상 차감 카드 노출: preview 대상 단위(시간차/반반차) + (로딩 중 또는 응답 보유).
//   preview 실패(null)면 미노출 — 신청은 가능(서버 최종 판정).
const showPreviewCard = computed(() => {
  const eligible = isTimeUnit.value || useUnitType.value === '05'
  return eligible && (props.previewLoading || !!props.preview)
})

// "예상 차감: 0일 4시간 (0.5일)" — 일·시간 표기(convMinutes 분모) + 원시 차감액 병기(plan §5-D).
const previewChargeText = computed(() => {
  const p = props.preview
  if (!p) return ''
  return `${formatLeaveDays(p.chargeDays, p.convMinutes)} (${trimRawDays(p.chargeDays)}일)`
})

// 하한 발동 마일스톤 요금(floorDays) → 단위 라벨. 0.25=반반차 / 0.5=반차 / 1=종일.
const FLOOR_UNIT_LABELS = { 0.25: '반반차', 0.5: '반차', 1: '종일' }

// 하한 발동 안내 문구 — floorDays 기반 단위 분기. floorDays 없으면(구응답) 일반 문구 폴백.
const floorNoticeText = computed(() => {
  const p = props.preview
  if (!p || !p.floorApplied) return ''
  const label = FLOOR_UNIT_LABELS[Number(p.floorDays)]
  if (!label) {
    // 구응답(floorDays 부재)/미지 값 폴백 — 일반화 문구.
    return '같은 날 누적 신청이 고정 단위(반반차·반차·종일) 기준 시간에 도달하여 고정 단위 요금이 적용됩니다.'
  }
  return `같은 날 누적 신청이 ${label} 시간에 도달하여 ${label} 요금(${trimRawDays(p.floorDays)}일)이 적용됩니다.`
})

// 'HHMM' → 분. 형식 위반 시 -1. (스케줄 HHMM 용)
function toMin4(hhmm) {
  if (!hhmm || hhmm.length !== 4 || !/^\d{4}$/.test(hhmm)) return -1
  const h = Number(hhmm.slice(0, 2))
  const m = Number(hhmm.slice(2))
  if (h > 23 || m > 59) return -1
  return h * 60 + m
}

// ── 검증 (단순 필수입력 — 그 외 분기/계산은 developer) ────────────────────
const isValid = computed(() => {
  if (!selectedType.value) return false
  if (!useUnitType.value) return false
  if (!workDateInput.value) return false
  if (isTimeUnit.value && (!startTimeInput.value || !endTimeInput.value)) return false
  // 종료가 자정을 넘어가면(익일 wrap) 제출 차단 — BE(eMin<=sMin) 가 ATTD_400_052 로 거부하므로 사전 방어.
  if (isTimeUnit.value && endOverflowsDay.value) return false
  if (aprvRequired.value && approverList.value.length === 0) return false
  // 가불 토글 ON + 만료 경과 일자면 제출 차단(결정 §3, 서버 fail-closed 사전 방어).
  if (borrowDateExpired.value) return false
  return true
})

// ── 표시 헬퍼 (UI — 허용) ────────────────────────────────────────────────
const formatDays = (d) => {
  const n = Number(d)
  if (Number.isNaN(n)) return '0'
  // 정수면 정수로, 소수면 1자리. 표시 전용.
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}
const approverMetaOf = (ap) => [ap?.nodeNm, ap?.rankNm].filter(Boolean).join(' · ')

// ── UI 토글/선택 (developer: 종류 변경 시 단위/시각/결재선 재구성 로직 보완) ─
// 선택 종류의 허용 단위(allowedUnits, 서버 권위) 안에서 기본 사용 단위 결정.
//   종일('00') 우선, 없으면 첫 허용 단위, 허용 단위가 없으면 빈 값 유지.
const resolveDefaultUnit = (type) => {
  const allowed = type?.allowedUnits || []
  if (allowed.includes('00')) return '00'
  return allowed[0] || ''
}

// 종류 변경 시 단위/시각/결재선 재초기화. 단위는 종류별 허용 단위 안에서 종일을 기본 선택한다.
const onSelectType = (lt) => {
  if (!lt?.applicable) return
  selectedLeaveCd.value = lt.leaveCd
  useUnitType.value = resolveDefaultUnit(lt)
  startTimeInput.value = ''
  stepCount.value = 1
  selectedPresetId.value = ''
  approverList.value = []
  borrowAgreed.value = false // 가불 동의는 종류별 — 종류 변경 시 해제
}

// 단위 전환. 시간차가 아닌 단위(종일/반차)로 전환 시 잔존 시작값을 비워 누수 방지.
//   시간차로 전환/변경 시 stepCount 를 1 로 리셋(종료는 computed 라 자동 재계산).
const onSelectUnit = (code) => {
  useUnitType.value = code
  if (['02', '03', '04'].includes(code)) {
    stepCount.value = 1
  } else {
    startTimeInput.value = ''
    stepCount.value = 1
  }
}

// 종료 스텝 증감 — 최소 N=1. 증가 시 자정 초과(익일 wrap)면 무시.
const onStepUp = () => {
  // 시작 입력된 시간차에서 다음 증가가 자정을 넘으면(>=1440) 상한 도달 — 증가 무시.
  if (!canStepUp.value) return
  stepCount.value += 1
}
const onStepDown = () => {
  stepCount.value = Math.max(1, stepCount.value - 1)
}

// 편의버튼('00'=종일 / '01'=반차): 단위만 세팅(비시간차).
//   종일/반차는 시각입력 UI 없이 단위코드만 제출(submit 에서 startTime/endTime 은 null).
//   시간차 진입 시 잔존 시작값을 비워 누수 방지하고 stepCount 리셋.
const onQuickFill = (unitCode) => {
  useUnitType.value = unitCode
  startTimeInput.value = ''
  stepCount.value = 1
}

// 프리셋 선택 → steps 를 approverList 로 전개(STEP_NO=배열 순서 보존). 같은 프리셋 재선택 시 토글 해제.
const onSelectPreset = (preset) => {
  if (!preset) return
  if (selectedPresetId.value === preset.presetId) {
    // 재선택 토글: 해제 + 전개 결재선 비움.
    selectedPresetId.value = ''
    approverList.value = []
    return
  }
  selectedPresetId.value = preset.presetId
  approverList.value = (preset.steps || []).map((s) => ({
    approverUserCd: s.approverUserCd,
    userNm: s.userNm,
    userId: s.userId,
    rankNm: s.rankNm,
    nodeNm: s.nodeNm,
  }))
}

const onOpenApproverPicker = () => {
  approverPickerOpen.value = true
}

// 시트 add(picked[]) 수신 → approverList 에 순서 append. userCd 식별자 dedup.
//   직접 추가 시 프리셋 이탈(selectedPresetId 해제) — 폼은 approverUserCds 를 SSOT 로 제출하므로 정합.
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
    selectedPresetId.value = ''
  }
  approverPickerOpen.value = false
}

// 결재자 제거 — userCd 식별자 필터(위치 index 재인덱싱 금지).
const onRemoveApprover = (approverUserCd) => {
  approverList.value = approverList.value.filter((a) => a.approverUserCd !== approverUserCd)
  // 프리셋 전개에서 일부 제거 시 더 이상 프리셋과 동일하지 않으므로 이탈 표시.
  selectedPresetId.value = ''
}

// ── 제출 (018-B LeaveApplyRequest 키 1:1) ─────────────────────────────────
const onSubmit = () => {
  if (!isValid.value) return
  const timeUnit = isTimeUnit.value
  const reasonText = reason.value.trim()
  emit('submit', {
    leaveCd: selectedLeaveCd.value,
    // ⚠️ leaveType(성격코드)은 018-A apply-meta 응답에 없음 → 보낼 값 없음(추측 금지). 미전송(서버 null 저장).
    workYmd: toYmd(workDateInput.value),
    useUnitType: useUnitType.value,
    startTime: timeUnit ? toHHMM(startTimeInput.value) : null,
    endTime: timeUnit ? toHHMM(endTimeInput.value) : null,
    reason: reasonText || null,
    // 결재 불필요 종류면 미전송. 결재 필요면 전개된 최종 순서의 userCd 배열(SSOT).
    approverUserCds: aprvRequired.value ? approverUserCds.value : undefined,
    // presetId 는 생략 — 폼이 approverUserCds(전개)를 SSOT 로 보냄(018-B 결정 2 정합).
    presetId: undefined,
    // 가불 동의(prafta-com-011-4): 토글 ON 시 true. 미선택이면 false(서버 미전송 시 false 취급).
    isBorrow: borrowAgreed.value,
  })
}

// ── 진입 컨텍스트(특정 일자)면 날짜 프리필. 폼 날짜는 이후 사용자 선택이 SSOT. ──
onMounted(() => {
  const ymd = props.context?.workYmd
  if (ymd && ymd.length === 8 && /^\d{8}$/.test(ymd)) {
    workDateInput.value = `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
  }
})
</script>

<style scoped>
.lvf {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 컨텍스트 박스 (OvertimeForm .ctx 패턴 동일) */
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

/* 섹션 공통 */
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
.fs__empty {
  margin: 0;
  padding: var(--space-md);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 연차 종류 리스트 */
.type-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.type-item--on {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
}
.type-item--off {
  opacity: 0.5;
  cursor: not-allowed;
}
.type-item__name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.type-item__bal {
  font-size: 12px;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}

/* 잔여 요약 박스 */
.balance-box {
  background: var(--color-primary-tint);
  border: 0.5px solid var(--color-primary-tint-border);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.balance-box__lbl {
  font-size: 13px;
  color: var(--color-primary-text-deep);
  font-weight: 500;
}
.balance-box__val {
  font-size: 14px;
  color: var(--color-primary-text-darkest);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* 사용 단위 칩 */
.unit-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}
.unit-chip {
  min-height: 40px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.unit-chip--on {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
  color: var(--color-primary-text-deep);
  font-weight: 500;
}

/* 시간차 입력 영역 헤더(제목 + 편의버튼) */
.time-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.quick-btns {
  display: flex;
  gap: var(--space-xs);
}
.quick-btn {
  height: 32px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.quick-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.time-guide {
  margin: 0;
  display: flex;
  gap: var(--space-xs);
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.time-guide__dot {
  color: var(--color-text-tertiary);
}

/* 종료 시각 [−]/[+] 스텝퍼 (시작 + N×단위분 자동계산) */
.end-stepper {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  height: 44px;
  padding: 0 var(--space-sm);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.end-stepper__btn {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  font-family: inherit;
}
.end-stepper__btn:disabled {
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
.end-stepper__val {
  flex: 1;
  text-align: center;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.end-stepper__n {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* 필드 공통(OvertimeForm .field 패턴) */
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

/* 결재선 — 프리셋 칩 */
.preset-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}
.preset-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.preset-chip--on {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
  color: var(--color-primary-text-deep);
  font-weight: 500;
}
.preset-chip__tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-surface);
}

/* 결재자 리스트 */
.aprv-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.aprv-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.aprv-row__step {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary-text-deep);
  font-size: 12px;
  font-weight: 600;
}
.aprv-row__info {
  flex: 1;
  min-width: 0;
}
.aprv-row__name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.aprv-row__meta {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.aprv-row__del {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  cursor: pointer;
}
.aprv-empty {
  margin: 0;
  padding: var(--space-md);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
  background: var(--color-surface);
  border: 0.5px dashed var(--color-border);
  border-radius: var(--radius-md);
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
  font-family: inherit;
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

/* LC-10: 예상 차감 요약 카드 — balance-box 톤 재사용(CSS 변수만) */
.preview-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-primary-tint);
  border: 0.5px solid var(--color-primary-tint-border);
  border-radius: var(--radius-md);
}
.preview-card__loading {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.preview-card__row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: var(--space-sm);
}
.preview-card__lbl {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary-text-deep);
}
.preview-card__val {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary-text-darkest);
  font-variant-numeric: tabular-nums;
  text-align: right;
}
.preview-card__floor {
  margin: 0;
  font-size: 12px;
  color: var(--color-warning-text);
}
.preview-card__warn {
  margin: 0;
  font-size: 12px;
  color: var(--color-danger);
}

/* 가불 동의 토글 + 안내 (prafta-com-011-4) — 기존 토큰/패턴 재사용 */
.borrow-toggle {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  min-height: 44px;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
}
.borrow-toggle__cb {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  accent-color: var(--color-primary);
  cursor: pointer;
}
.borrow-toggle__txt {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.borrow-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-primary-tint);
  border: 0.5px solid var(--color-primary-tint-border);
  border-radius: var(--radius-md);
}
.borrow-info__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.borrow-info__lbl {
  font-size: 13px;
  color: var(--color-primary-text-deep);
}
.borrow-info__val {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary-text-darkest);
  font-variant-numeric: tabular-nums;
}
.borrow-info__deficit {
  margin: 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary-text-deep);
}
.borrow-info__guide {
  margin: 0;
  display: flex;
  gap: var(--space-xs);
  font-size: 12px;
  color: var(--color-primary-text-deep);
}
.borrow-info__dot {
  color: var(--color-primary-text-deep);
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

/* sticky 푸터(OvertimeForm .form-ft 패턴 동일) */
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
