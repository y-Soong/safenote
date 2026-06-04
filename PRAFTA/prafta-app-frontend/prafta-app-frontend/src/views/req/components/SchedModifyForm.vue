<!--
  SchedModifyForm.vue — 스케줄 수정 요청 폼
  - 작업 ID: PRAFTA-APP-007-6 (분해: .claude/requests/app_requests/prafta-app-007-plan.md §8.3)
  - props.context: day 객체 { workYmd, nodeCd, siteName, scheduleSummary, workPlanName, slots[] }
  - 스케줄은 그 자체에 1·2구간 정의가 포함되므로 구간별로 따로 고르지 않고
    "하나의 스케줄"만 선택한다.
  - emits: submit ({ slots:[{workSeq:1, schCd}], reqReason }), cancel
    (백엔드 /appApi/req07/sched-modify 가 slots 배열을 받으므로, 단일 스케줄을
     대표 구간 한 건으로 감싸 전송한다.)
-->
<template>
  <form class="sched-form" @submit.prevent="onSubmit">
    <!-- 컨텍스트 박스 -->
    <section class="ctx">
      <p class="ctx__date">
        <strong>{{ ctxDateDisplay }}</strong>
        <small>{{ ctxSiteDisplay }}</small>
      </p>
      <div class="ctx__row">
        <span class="ctx__lbl">현재 스케줄</span>
        <span class="ctx__val">{{ context.workPlanName || '-' }}</span>
      </div>
      <div v-if="context.scheduleSummary" class="ctx__row">
        <span class="ctx__lbl"></span>
        <span class="ctx__val ctx__val--muted">{{ context.scheduleSummary }}</span>
      </div>
    </section>

    <!-- 변경할 스케줄 -->
    <section class="fs">
      <p class="fs__title">변경할 스케줄</p>

      <label class="field">
        <span class="field__label"><span class="req">*</span>스케줄</span>
        <!--
          스케줄은 그 자체에 1·2구간이 포함되므로 구간별로 따로 고르지 않고
          하나의 스케줄만 선택한다.
          F2: GET /appApi/req07/schedules 목록 → 바텀시트 선택형.
        -->
        <button
          type="button"
          class="field__trigger"
          :class="{ 'field__trigger--placeholder': !schCd }"
          :disabled="optDisabled"
          @click="openSheet"
        >
          <span class="field__trigger-text">{{ triggerLabel }}</span>
          <svg
            class="field__chevron"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>
        <span v-if="optError" class="field__error">
          스케줄을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.
        </span>
        <span v-else-if="!optLoading && !schedOptions.length" class="field__error">
          선택 가능한 스케줄이 없어요. 관리자에게 문의해 주세요.
        </span>
      </label>

      <label class="field">
        <span class="field__label">
          <span class="req">*</span>변경 사유
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
    <!-- selfApprvYn='N'(또는 미상=폴백) → 결재선 섹션 노출. 'Y' → 안내문만(서버 분기 위임). -->
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

    <!-- 헬퍼 메시지 -->
    <p class="helper">
      <span class="helper__dot" aria-hidden="true">·</span>
      요청은 관리자 승인 후 반영돼요. 스케줄 마감 전까지 신청해 주세요.
    </p>

    <!-- 푸터 -->
    <footer class="form-ft">
      <button type="button" class="btn btn--x" @click="$emit('cancel')">취소</button>
      <button type="submit" class="btn btn--p" :disabled="!isValid || submitting">
        {{ submitting ? '등록 중...' : '요청하기' }}
      </button>
    </footer>

    <!-- 스케줄 선택 바텀시트 -->
    <SchedPickSheet
      v-model="sheetOpen"
      :options="schedOptions"
      :selected="schCd"
      :loading="optLoading"
      :error="optError"
      @apply="onPickSched"
    />

    <!-- 결재자 추가 바텀시트 (prafta-app-009) -->
    <AttdApproverPickerSheet
      v-if="showApprovalSection"
      v-model="approverPickerOpen"
      :excluded-user-cds="approverUserCds"
      @add="onAddApprovers"
    />
  </form>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import api from '@/api/axios'
import SchedPickSheet from './SchedPickSheet.vue'
import ApprovalLineSection from './ApprovalLineSection.vue'
import AttdApproverPickerSheet from './AttdApproverPickerSheet.vue'

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

// 스케줄은 그 자체에 1·2구간이 포함되므로 구간별이 아니라 "하나의 스케줄"만 선택한다.
const schCd = ref('')
const selectedSchNm = ref('')
const reqReason = ref('')

// 스케줄 옵션 조회 상태
const schedOptions = ref([])
const optLoading = ref(false)
const optError = ref(false)
const sheetOpen = ref(false)

// ── 결재선 상태 (prafta-app-009) ─────────────────────────────────────────
// approverList: [{ approverUserCd, userNm, userId, rankNm, nodeNm }] (순서 = 결재 단계)
const approverList = ref([])
const approverPickerOpen = ref(false)
const approvalSectionRef = ref(null)

// 분기값. approvalContext 미상(null)이면 결재선 노출 폴백(서버가 'Y'면 무시).
const selfApprvYn = computed(() => props.approvalContext?.selfApprvYn || null)
// 결재선 섹션 노출: 'Y'(자체근태승인)가 아닐 때(= 'N' 또는 미상). 'Y'면 안내문만.
const showApprovalSection = computed(() => selfApprvYn.value !== 'Y')
// 'Y' 케이스 안내문(섹션 숨김 시): 노드 관리자면 즉시 승인, 일반 근로자면 관리자 승인.
const approvalNotice = computed(() => {
  if (selfApprvYn.value !== 'Y') return ''
  return props.approvalContext?.isNodeAdmin
    ? '요청하면 즉시 승인 처리돼요.'
    : '부서 관리자 승인 후 반영돼요. 결재선을 지정하지 않아도 돼요.'
})
// 결재자 emit 용 userCd 배열(순서 보존 — 위치 재인덱싱 아님, 표시 순서 그대로).
const approverUserCds = computed(() => approverList.value.map((a) => a.approverUserCd))
// 결재 필수 여부: 'N'(결재선 사용)일 때만. 미상이면 폴백상 필수 아님(서버가 최종 판정).
const approverRequired = computed(() => selfApprvYn.value === 'N')

/**
 * 'HHmm' → 'HH:MM'. 형식이 4자리 숫자가 아니면 원본 반환.
 */
const fmtTime = (t) => {
  if (!t || t.length !== 4) return t || ''
  return `${t.slice(0, 2)}:${t.slice(2, 4)}`
}

/**
 * 스케줄 옵션 라벨 조립.
 * - 1구간만: "09:00~18:00"
 * - 2구간: "09:00~12:00 / 13:00~18:00"
 */
const buildLabel = (s) => {
  const fst = `${fmtTime(s.fstStrTime)}~${fmtTime(s.fstEndTime)}`
  if (s.secStrTime && s.secEndTime) {
    return `${fst} / ${fmtTime(s.secStrTime)}~${fmtTime(s.secEndTime)}`
  }
  return fst
}

// 트리거 버튼 표시 라벨
const triggerLabel = computed(() => (schCd.value ? selectedSchNm.value : '스케줄을 선택해 주세요'))

// 옵션 0건 또는 조회 실패 시 트리거 비활성 (제출 차단)
const optDisabled = computed(
  () => optLoading.value || optError.value || schedOptions.value.length === 0,
)

// 컨텍스트 표시 (workYmd → "YYYY년 M월 D일")
const ctxDateDisplay = computed(() => {
  const y = props.context.workYmd?.slice(0, 4)
  const m = props.context.workYmd?.slice(4, 6)
  const d = props.context.workYmd?.slice(6, 8)
  return y && m && d ? `${y}년 ${Number(m)}월 ${Number(d)}일` : '-'
})
const ctxSiteDisplay = computed(() => props.context.siteName || '')

// 사유 미입력은 버튼 비활성 사유에서 제외(제출 시 사유 전용 alert 로 안내).
//   결재 필수('N') 케이스는 결재자 1명 이상이어야 제출 활성.
const isValid = computed(() => {
  if (!schCd.value) return false
  if (approverRequired.value && approverList.value.length === 0) return false
  return true
})

const openSheet = () => {
  if (optDisabled.value) return
  sheetOpen.value = true
}

// 시트에서 스케줄 선택 적용
const onPickSched = ({ schCd: code, label }) => {
  schCd.value = code
  selectedSchNm.value = label
}

// ── 결재자 추가/제거 (prafta-app-009, LeaveApplyForm 패턴 차용) ───────────
const onOpenApproverPicker = () => {
  approverPickerOpen.value = true
}

// 시트 add(picked[]) 수신 → approverList 에 순서 append. userCd 식별자 dedup.
//   직접 추가 시 프리셋 이탈(섹션의 selectedPresetId 해제) — approverUserCds 가 SSOT 라 정합.
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

// 스케줄 옵션 조회 (식별값은 서버가 JWT 로 도출 — 쿼리 미전송)
const fetchSchedOptions = async () => {
  optLoading.value = true
  optError.value = false
  try {
    const { data } = await api.get('/appApi/req07/schedules')
    const list = Array.isArray(data?.schedules) ? data.schedules : []
    schedOptions.value = list.map((s) => ({
      schCd: s.schCd,
      schNo: s.schNo,
      baseYn: s.baseYn,
      label: buildLabel(s),
    }))
  } catch (e) {
    optError.value = true
    schedOptions.value = []
  } finally {
    optLoading.value = false
  }
}

onMounted(fetchSchedOptions)

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
  // 단일 스케줄을 대표 구간(workSeq=1) 한 건으로 감싸 보낸다.
  // (백엔드 /appApi/req07/sched-modify 가 slots 배열을 받으므로 계약 호환 유지.)
  // prafta-app-009: 결재선 노출 케이스만 approverUserCds 전개 전송(SSOT). 'Y' 케이스는 미전송(서버 분기).
  emit('submit', {
    slots: [{ workSeq: 1, schCd: schCd.value }],
    reqReason: reqReason.value.trim(),
    approverUserCds: showApprovalSection.value ? approverUserCds.value : undefined,
    presetId: undefined,
  })
}
</script>

<style scoped>
.sched-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 컨텍스트 박스 */
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
.ctx__val--muted {
  color: var(--color-text-tertiary);
}

/* 폼 섹션 */
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

/* 필드 */
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
.field__input,
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
}
.field__textarea {
  resize: vertical;
  min-height: 96px;
}
.field__input:focus,
.field__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 스케줄 선택 트리거 버튼 (input 룩 미러링) */
.field__trigger {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: inherit;
  box-sizing: border-box;
  cursor: pointer;
  text-align: left;
}
.field__trigger:focus-visible {
  outline: none;
  border-color: var(--color-primary);
}
.field__trigger--placeholder .field__trigger-text {
  color: var(--color-text-tertiary);
}
.field__trigger:disabled {
  background: var(--color-bg);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
.field__trigger-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.field__chevron {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}
.field__error {
  font-size: 11px;
  color: var(--color-danger);
}

/* 결재선 안내문('Y' 케이스) */
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

/* 헬퍼 */
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

/* 푸터 */
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
