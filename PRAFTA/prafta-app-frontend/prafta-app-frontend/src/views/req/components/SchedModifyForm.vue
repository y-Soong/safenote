<!--
  SchedModifyForm.vue — 스케줄 수정 요청 폼
  - 작업 ID: PRAFTA-APP-007-6 (분해: .claude/requests/app_requests/prafta-app-007-plan.md §8.3)
  - props.context: day 객체 { workYmd, nodeCd, siteName, scheduleSummary, workPlanName, slots[] }
  - emits: submit ({ slots:[{workSeq, schCd}], reqReason }), cancel
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

      <SlotCard
        v-for="slot in slots"
        :key="slot.workSeq"
        :work-seq="slot.workSeq"
        :title="slot.workSeq + '구간'"
        :removable="slots.length > 1"
        @remove="onRemoveSlot"
      >
        <label class="field">
          <span class="field__label"><span class="req">*</span>근무 타입</span>
          <!--
            1차: SCH_CD 목록 endpoint 미도입 → 사용자 직접 코드 입력.
            follow-up F2 도입 시 BaseBottomSheet 로 교체 예정.
          -->
          <input
            v-model="slot.schCd"
            class="field__input"
            type="text"
            placeholder="스케줄 코드를 입력해 주세요"
            maxlength="20"
          />
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
  </form>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import SlotCard from './SlotCard.vue'

const props = defineProps({
  context: { type: Object, required: true },
  submitting: { type: Boolean, default: false },
})
const emit = defineEmits(['submit', 'cancel'])

const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (m) => (proxy?.$alert ? proxy.$alert(m) : window.alert(m))

// 1구간 기본 (사용자가 "구간 추가" 누르면 2구간 추가)
const slots = ref([{ workSeq: 1, schCd: '' }])
const reqReason = ref('')

// 컨텍스트 표시 (workYmd → "YYYY년 M월 D일")
const ctxDateDisplay = computed(() => {
  const y = props.context.workYmd?.slice(0, 4)
  const m = props.context.workYmd?.slice(4, 6)
  const d = props.context.workYmd?.slice(6, 8)
  return y && m && d ? `${y}년 ${Number(m)}월 ${Number(d)}일` : '-'
})
const ctxSiteDisplay = computed(() => props.context.siteName || '')

const isValid = computed(() => {
  if (!reqReason.value.trim()) return false
  return slots.value.every((s) => s.schCd.trim())
})

const onAddSlot = () => {
  if (slots.value.length >= 2) return
  slots.value.push({ workSeq: 2, schCd: '' })
}

const onRemoveSlot = (workSeq) => {
  slots.value = slots.value.filter((s) => s.workSeq !== workSeq)
  // workSeq 재정렬 (1구간 단독 케이스)
  slots.value.forEach((s, i) => (s.workSeq = i + 1))
}

const onSubmit = () => {
  if (!isValid.value) {
    showAlert('모든 필수 항목을 입력해 주세요.')
    return
  }
  emit('submit', {
    slots: slots.value.map((s) => ({ workSeq: s.workSeq, schCd: s.schCd.trim() })),
    reqReason: reqReason.value.trim(),
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

/* 구간 추가 버튼 */
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
