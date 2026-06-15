<!--
  LeaveChangeRequestPop.vue — 관리자 연차 변경(이동)/삭제 발의 팝업 (prafta-com-008-C-3)
  유형: frontend-component (웹 관리자 팝업)
  연결 작업: PRAFTA-{C-4-web}
  참조 패턴: views/notice/popup/ArchiveCreatePop.vue (modal-popup-guide 본문 스크롤),
            views/attd/popup/AttdDayDetailPop.vue
  역할 분담: 골격 = 입력 폼 구조 + 유형 토글 + 사유 필수 UI. developer = 검증/제출 API.
  ※ 발의는 "이미 등록된 연차일(LEAVE_ID)"에 대해서만. 최초 등록(셀 신규)은 동의 불요 → 본 팝업 대상 아님.
-->
<template>
  <div class="modal-overlay" @click.self="onClose">
    <div class="modal-content lcr-pop">
      <header class="modal-header">
        <h2 class="modal-title">연차 변경/삭제 요청</h2>
        <button type="button" class="modal-close" aria-label="닫기" @click="onClose">×</button>
      </header>

      <div class="modal-body lcr-body">
        <!-- 대상 연차일 정보 (읽기 전용) -->
        <section class="lcr-section">
          <h3 class="lcr-section__title">대상 연차</h3>
          <dl class="lcr-target">
            <div><dt>사용자</dt><dd>{{ target?.userNm }}</dd></div>
            <div><dt>연차일</dt><dd>{{ target?.startDate }}</dd></div>
            <div><dt>연차종류</dt><dd>{{ target?.leaveNm }}</dd></div>
            <div><dt>촉진단계</dt><dd>{{ target?.promotionStageNm || '비촉진' }}</dd></div>
          </dl>
        </section>

        <!-- 요청 유형 -->
        <section class="lcr-section">
          <h3 class="lcr-section__title">요청 유형</h3>
          <div class="lcr-radio-group">
            <label class="lcr-radio">
              <input type="radio" value="MOVE" v-model="reqType" />
              이동(변경)
            </label>
            <label class="lcr-radio">
              <input type="radio" value="DELETE" v-model="reqType" />
              삭제(근무 복귀)
            </label>
          </div>
        </section>

        <!-- 이동 대상일 (MOVE 시에만) -->
        <section v-if="reqType === 'MOVE'" class="lcr-section">
          <h3 class="lcr-section__title">이동 대상일</h3>
          <!-- TODO(developer): 캘린더 컴포넌트 바인딩. 만료일(AVAIL_TO_DATE) 이내 + 마감월 제외는 서버 강제. -->
          <CalendarSrch v-model="moveTargetDate" />
          <p class="lcr-hint">
            연차 만료일 이내로만 이동할 수 있습니다. 대상일에 같은 법정연차가 있으면 거부됩니다.
          </p>
        </section>

        <!-- 요청 사유 (필수) -->
        <section class="lcr-section">
          <h3 class="lcr-section__title">요청 사유 <span class="lcr-req">*</span></h3>
          <textarea
            v-model="reason"
            class="lcr-textarea"
            rows="3"
            maxlength="500"
            placeholder="변경/삭제 사유를 입력하세요 (필수)"
          ></textarea>
        </section>
      </div>

      <footer class="modal-footer lcr-footer">
        <button type="button" class="btn btn-ghost" @click="onClose">취소</button>
        <button
          type="button"
          class="btn btn-primary"
          :disabled="!canSubmit || submitting"
          @click="onSubmit"
        >
          요청
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import CalendarSrch from '@/components/common/CalendarSrch.vue'
import axios from '@/api/axios'
import { getMessage, MSG } from '@/messages'
import { resolveApiErrorMessage } from '@/utils/apiError'

const props = defineProps({
  // { leaveId, userCd, userNm, startDate, leaveNm, promotionStageNm }
  target: { type: Object, default: null },
})
const emit = defineEmits(['close', 'submitted'])

const { proxy } = getCurrentInstance()

// ── 입력 상태 ────────────────────────────────────────────────────────────
const reqType = ref('MOVE')
const moveTargetDate = ref('')
const reason = ref('')
const submitting = ref(false)

// 단순 입력 검증(필수값)만 화면에서 처리. 만료일/마감/충돌은 서버 강제.
const canSubmit = computed(() => {
  if (!reason.value.trim()) return false
  if (reqType.value === 'MOVE' && !moveTargetDate.value) return false
  return true
})

// CalendarSrch v-model 값(YYYY-MM-DD 등) → YYYYMMDD 정규화
const toYmd8 = (v) => (v ? String(v).replace(/[^0-9]/g, '').slice(0, 8) : '')

const onClose = () => emit('close')

// POST /webApi/attd13/change-requests
//   body(대문자 키) = { TARGET_LEAVE_ID, REQ_TYPE, MOVE_TARGET_DATE(MOVE만), REQ_REASON }
//   식별/스코프/만료/충돌/마감/중복요청은 서버 JWT + 재검증(body 비신뢰).
const onSubmit = async () => {
  if (!canSubmit.value || submitting.value) return
  if (!props.target?.leaveId) {
    await proxy.$alert('대상 연차 정보가 없습니다.')
    return
  }
  submitting.value = true
  try {
    await axios.post('/webApi/attd13/change-requests', {
      TARGET_LEAVE_ID: props.target.leaveId,
      REQ_TYPE: reqType.value,
      MOVE_TARGET_DATE: reqType.value === 'MOVE' ? toYmd8(moveTargetDate.value) : null,
      REQ_REASON: reason.value.trim(),
    })
    await proxy.$alert('변경 요청이 등록되었습니다.')
    emit('submitted')
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, getMessage(MSG.SAVE_ERROR)))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.lcr-pop {
  width: 480px;
  max-width: 92vw;
  max-height: 84vh;
  display: flex;
  flex-direction: column;
}

.lcr-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--card-padding, 20px);
  padding: var(--card-padding, 20px);
}

.lcr-section__title {
  margin: 0 0 var(--space-sm, 8px);
  font-size: var(--btn-font, 11px);
  font-weight: 600;
  color: var(--color-text-strong);
}

.lcr-req {
  color: var(--color-danger);
}

.lcr-target {
  display: grid;
  gap: var(--space-xs, 4px);
  padding: var(--space-sm, 8px);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}
.lcr-target div {
  display: flex;
  gap: var(--space-sm, 8px);
  font-size: var(--btn-font, 11px);
}
.lcr-target dt {
  width: 64px;
  color: var(--color-text-muted);
}
.lcr-target dd {
  margin: 0;
  color: var(--color-text-strong);
}

.lcr-radio-group {
  display: flex;
  gap: var(--card-padding, 20px);
}
.lcr-radio {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 4px);
  font-size: var(--btn-font, 11px);
  color: var(--color-text);
}

.lcr-textarea {
  width: 100%;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius);
  padding: var(--space-sm, 8px);
  font-family: inherit;
  font-size: var(--btn-font, 11px);
  resize: vertical;
}

.lcr-hint {
  margin: var(--space-xs, 4px) 0 0;
  font-size: var(--btn-font-sm, 11px);
  color: var(--color-warning-text);
}

.lcr-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm, 8px);
  padding: var(--space-sm, 8px) var(--card-padding, 20px);
  border-top: 1px solid var(--color-border);
}
</style>
