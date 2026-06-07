<!--
  AdminApprovalAdjustSheet.vue — 조정 후 승인 입력 시트 (초과근무 / 근태보정)
  - 작업 ID: 001-P2-F6 (분해: 001-phase2-admin-approval-plan.md §2-2 / §5 ① / 재기획서 §5.8.1·§5.8.2·§5.8.3, §7.6)
  - 트리거: 상세(AdminApprovalDetailView) ⑤ 결정 '조정 후 승인' → '조정 값 입력' → open=true.
  - 백엔드: POST /appApi/admin/approval/process { reqId, group, decision:'APPROVE_ADJUST', adjusted }
      · OVERTIME: adjusted = { startDate, startTime, endDate, endTime } (시스템 계산값 기본채움 §5.8.3)
      · CORRECTION: adjusted = { checkInDate, checkInTime, checkOutDate, checkOutTime } (보정 시각·구간 §5.8.2)
      · LEAVE: 조정 불가(§5.8.4) → 본 시트 미노출. SCHEDULE: A5 확정 전 미노출.
  - ⚠️ 인라인 검증(attd §5 1일2구간·2회출근 / §6.6 겹침 / §10.2 표준화)은 서버 최종 + developer 보조(본 골격은 형식 입력만).
  - 참조 패턴: views/admin/tbm/components/AdminTbmForceExitSheet.vue (바텀시트 + 토큰 자급).
  - planner 라운드 스코프: template + style 완성. script 는 props/emits + v-model + 기본채움(prefill) 토글만.
-->
<template>
  <transition name="ap-adj-fade">
    <div
      v-if="open"
      class="ap-adj__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="조정 후 승인"
      @click.self="onClose"
    >
      <div class="ap-adj">
        <div class="ap-adj__handle" aria-hidden="true"></div>

        <header class="ap-adj__header">
          <h2 class="ap-adj__title">조정 후 승인</h2>
          <button type="button" class="ap-adj__close" aria-label="닫기" @click="onClose">
            <svg
              width="20"
              height="20"
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
        </header>

        <div class="ap-adj__body">
          <!-- 초과근무: 시작/종료 시각 -->
          <template v-if="group === 'OVERTIME'">
            <p class="ap-adj__guide">시스템 계산값이 기본 입력됩니다. 필요 시 수정하세요.</p>
            <div class="ap-adj__grid">
              <label class="ap-adj__field">
                <span class="ap-adj__label">시작 일자</span>
                <input v-model="ot.startDate" type="date" class="ap-adj__input" />
              </label>
              <label class="ap-adj__field">
                <span class="ap-adj__label">시작 시각</span>
                <input v-model="ot.startTime" type="time" class="ap-adj__input" />
              </label>
              <label class="ap-adj__field">
                <span class="ap-adj__label">종료 일자</span>
                <input v-model="ot.endDate" type="date" class="ap-adj__input" />
              </label>
              <label class="ap-adj__field">
                <span class="ap-adj__label">종료 시각</span>
                <input v-model="ot.endTime" type="time" class="ap-adj__input" />
              </label>
            </div>
          </template>

          <!-- 근태보정: 출근/퇴근 시각 -->
          <template v-else-if="group === 'CORRECTION'">
            <p class="ap-adj__guide">원본 추정값이 기본 입력됩니다. 보정 시각을 조정하세요.</p>
            <div class="ap-adj__grid">
              <label class="ap-adj__field">
                <span class="ap-adj__label">출근 일자</span>
                <input v-model="corr.checkInDate" type="date" class="ap-adj__input" />
              </label>
              <label class="ap-adj__field">
                <span class="ap-adj__label">출근 시각</span>
                <input v-model="corr.checkInTime" type="time" class="ap-adj__input" />
              </label>
              <label class="ap-adj__field">
                <span class="ap-adj__label">퇴근 일자</span>
                <input v-model="corr.checkOutDate" type="date" class="ap-adj__input" />
              </label>
              <label class="ap-adj__field">
                <span class="ap-adj__label">퇴근 시각</span>
                <input v-model="corr.checkOutTime" type="time" class="ap-adj__input" />
              </label>
            </div>
          </template>

          <p class="ap-adj__notice">
            조정 값은 표준화 단위·구간 규칙에 따라 서버에서 최종 검증됩니다.
          </p>
        </div>

        <footer class="ap-adj__footer">
          <button type="button" class="ap-adj__btn ap-adj__btn--ghost" @click="onClose">
            취소
          </button>
          <button
            type="button"
            class="ap-adj__btn ap-adj__btn--primary"
            :disabled="submitting"
            @click="onConfirm"
          >
            조정 후 승인
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { reactive, watch } from 'vue'

import { splitDateTime } from '@/utils/approvalFormat'

const props = defineProps({
  // 시트 열림 여부(부모 제어)
  open: { type: Boolean, default: false },
  // 요청 그룹: 'OVERTIME' | 'CORRECTION' (LEAVE/SCHEDULE 는 미노출)
  group: { type: String, default: '' },
  // 상세 body(기본채움 소스) — 서버 응답의 systemCalc / before 등(developer 가 정규화)
  body: { type: Object, default: null },
  // 부모가 process API 호출 중일 때 버튼 잠금
  submitting: { type: Boolean, default: false },
})

// close: 닫기 / confirm: 조정 확정 → adjusted 페이로드
const emit = defineEmits(['close', 'confirm'])

// 초과근무 조정 입력(시스템 계산값 기본채움)
const ot = reactive({ startDate: '', startTime: '', endDate: '', endTime: '' })
// 근태보정 조정 입력(원본 추정값 기본채움)
const corr = reactive({ checkInDate: '', checkInTime: '', checkOutDate: '', checkOutTime: '' })

const onClose = () => emit('close')

const onConfirm = () => {
  if (props.submitting) return
  const adjusted = props.group === 'OVERTIME' ? { ...ot } : { ...corr }
  emit('confirm', adjusted)
}

// 열릴 때 body 기준 기본채움(prefill).
//   OVERTIME → body.systemCalc.{startAt,endAt}(시스템 계산값, §5.8.3),
//   CORRECTION → body.before.{checkIn,checkOut}(원본 추정값, §5.8.2)를 date/time input 으로 분해.
const prefill = () => {
  const body = props.body || {}
  if (props.group === 'OVERTIME') {
    const sys = body.systemCalc || {}
    const s = splitDateTime(sys.startAt)
    const e = splitDateTime(sys.endAt)
    ot.startDate = s.date
    ot.startTime = s.time
    ot.endDate = e.date
    ot.endTime = e.time
  } else if (props.group === 'CORRECTION') {
    const before = body.before || {}
    const ci = splitDateTime(before.checkIn)
    const co = splitDateTime(before.checkOut)
    corr.checkInDate = ci.date
    corr.checkInTime = ci.time
    corr.checkOutDate = co.date
    corr.checkOutTime = co.time
  }
}

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) prefill()
  },
)
</script>

<style scoped>
.ap-adj__dimmer {
  /* 토큰 자급(self-contained) */
  --color-surface: #ffffff;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-primary: #16a34a;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --radius-md: 10px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: fixed;
  inset: 0;
  background: var(--color-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 130;
}
.ap-adj {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 80vh;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}
.ap-adj__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.ap-adj__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.ap-adj__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ap-adj__close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.ap-adj__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.ap-adj__guide {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}
.ap-adj__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}
.ap-adj__field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.ap-adj__label {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.ap-adj__input {
  width: 100%;
  box-sizing: border-box;
  height: 44px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.ap-adj__input:focus {
  outline: none;
  border-color: var(--color-primary);
}
.ap-adj__notice {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
  line-height: 1.5;
}
.ap-adj__footer {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.ap-adj__btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.ap-adj__btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.ap-adj__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.ap-adj__btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.ap-adj-fade-enter-active,
.ap-adj-fade-leave-active {
  transition: opacity 0.18s ease;
}
.ap-adj-fade-enter-from,
.ap-adj-fade-leave-to {
  opacity: 0;
}
</style>
