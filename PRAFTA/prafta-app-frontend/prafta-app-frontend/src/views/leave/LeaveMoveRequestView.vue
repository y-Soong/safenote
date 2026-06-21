<!--
  LeaveMoveRequestView.vue — 근로자 발의 연차 이동 요청 화면 (prafta-com-008-C-3, 앱)
  유형: frontend-screen (모바일 앱, 근로자)
  연결 작업: PRAFTA-{C-5-app}
  참조 패턴: views/leave/LeaveApplyView.vue (헤더 + 본문 + 폼 + TODO(developer) + 디자인 토큰 루트 1회)
  역할 분담: 골격 = 본인 연차일 선택 + 이동대상일 + 사유 입력 UI. developer = 조회/제출 API + 라우팅.
  ※ 근로자는 본인 연차일(특히 촉진 확정 연차)을 다른 날로 "이동만" 요청(취소·삭제 불가).
    만료일(AVAIL_TO_DATE) 이내 + DIRECT_USE_KEY 충돌 + 마감월은 서버 강제. 본인 LEAVE_ID 한정(JWT).
-->
<template>
  <div class="leave-move-view">
    <header class="lmv-hd">
      <button type="button" class="lmv-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true"><use href="#i-lmv-chev-left" /></svg>
      </button>
      <h1 class="lmv-hd__title">연차 이동 요청</h1>
      <span class="lmv-hd__spacer" aria-hidden="true"></span>
    </header>

    <main class="lmv-body">
      <p v-if="isLoading" class="lmv-state">불러오는 중...</p>

      <div v-else-if="loadError" class="lmv-state lmv-state--err">
        <p>{{ loadError }}</p>
        <button type="button" class="lmv-retry" @click="loadMyLeaves">다시 시도</button>
      </div>

      <template v-else>
        <!-- 이동할 연차일 선택 -->
        <section class="lmv-section">
          <h2 class="lmv-section__title">이동할 연차일</h2>
          <select v-model="selectedLeaveId" class="lmv-select">
            <option value="" disabled>연차일을 선택하세요</option>
            <option v-for="lv in movableLeaves" :key="lv.leaveId" :value="lv.leaveId">
              {{ lv.startDate }} · {{ lv.leaveNm }}{{ lv.promotionStageNm ? ' (' + lv.promotionStageNm + ')' : '' }}
            </option>
          </select>
          <p class="lmv-hint">취소는 불가하며 다른 날짜로 이동만 가능합니다.</p>
        </section>

        <!-- 이동 대상일 -->
        <section class="lmv-section">
          <h2 class="lmv-section__title">이동 대상일</h2>
          <!-- 공통 날짜 휠 필드(modelValue 'YYYY-MM-DD'). 만료일 이내/마감/충돌은 서버 강제. -->
          <DateStepperField v-model="moveTargetDate" placeholder="이동 대상일 선택" />
        </section>

        <!-- 사유 -->
        <section class="lmv-section">
          <h2 class="lmv-section__title">이동 사유</h2>
          <textarea
            v-model="reason"
            class="lmv-textarea"
            rows="3"
            maxlength="500"
            placeholder="이동 사유를 입력하세요"
          ></textarea>
        </section>

        <button
          type="button"
          class="lmv-submit"
          :disabled="!canSubmit || submitting"
          @click="onSubmit"
        >
          이동 요청
        </button>
      </template>
    </main>

    <svg width="0" height="0" class="lmv-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-lmv-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatYmdDisplay } from '@/utils/approvalFormat'
import DateStepperField from '@/components/common/DateStepperField.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 상태 (developer: 조회/제출 보완) ─────────────────────────────────────
const isLoading = ref(true)
const loadError = ref('')
const movableLeaves = ref([])
const selectedLeaveId = ref('')
const moveTargetDate = ref('')
const reason = ref('')
const submitting = ref(false)

// 단순 필수값 검증만 골격에서. 만료/충돌/마감은 서버(developer).
const canSubmit = computed(
  () => !!selectedLeaveId.value && !!moveTargetDate.value && !!reason.value.trim(),
)

const onBack = () => router.back()

// 촉진단계 코드 → 라벨 (서버 row 는 코드값만 반환)
const PROMOTION_STAGE_NM = { NONE: '', FIRST: '1차 촉진', SECOND: '2차 촉진' }

// YYYYMMDD → "YYYY.MM.DD" (셀렉트 표시, D1 점 통일)
const fmtYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return ymd || ''
  return formatYmdDisplay(ymd)
}

// "YYYY-MM-DD"(input[type=date]) → YYYYMMDD
const toYmd8 = (v) => (v ? String(v).replace(/[^0-9]/g, '').slice(0, 8) : '')

// 서버 row → 셀렉트 표시 객체로 보강(이름 미제공 → 일수 라벨, 촉진단계 라벨)
const toLeave = (lv) => ({
  leaveId: lv.leaveId,
  startDate: fmtYmd(lv.startDate),
  leaveNm: lv.leaveDays != null ? `연차 ${lv.leaveDays}일` : '연차',
  promotionStageNm: PROMOTION_STAGE_NM[lv.promotionStage] || '',
})

// GET /appApi/leavechange/movable-leaves
//   본인(JWT) 미래 확정 연차일 목록(이동 가능 대상). → movableLeaves.value
const loadMyLeaves = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const res = await api.get('/appApi/leavechange/movable-leaves')
    const list = res?.data?.list || []
    movableLeaves.value = list.map(toLeave)
  } catch (err) {
    loadError.value = resolveApiErrorMessage(err, '연차 목록을 불러오지 못했어요.')
  } finally {
    isLoading.value = false
  }
}

// POST /appApi/leavechange/move-requests
//   body(대문자 키) = { TARGET_LEAVE_ID, MOVE_TARGET_DATE, REQ_REASON }
//   서버: 본인 LEAVE_ID 검증(body 비신뢰) + 만료/충돌/마감 검증 →
//         initiatorType=WORKER, reqType=MOVE 요청 생성(관리자 승인 대상) + 관리자 PUSH.
const onSubmit = async () => {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    await api.post('/appApi/leavechange/move-requests', {
      TARGET_LEAVE_ID: selectedLeaveId.value,
      MOVE_TARGET_DATE: toYmd8(moveTargetDate.value),
      REQ_REASON: reason.value.trim(),
    })
    await showAlert('이동 요청을 보냈어요. 관리자 승인 후 반영됩니다.')
    router.back()
  } catch (err) {
    await showAlert(resolveApiErrorMessage(err, '요청에 실패했어요.'))
  } finally {
    submitting.value = false
  }
}

onMounted(loadMyLeaves)
</script>

<style scoped>
.leave-move-view {
  --color-primary: #16a34a;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-warning-text: #b45309;
  --color-border: #e5e7eb;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-md: 10px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100vh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR', sans-serif;
}

.lmv-hd {
  height: 56px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
}
.lmv-hd__back {
  width: 44px;
  height: 44px;
  background: transparent;
  border: 0;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.lmv-hd__title {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 500;
}
.lmv-hd__spacer {
  width: 44px;
  height: 44px;
}

.lmv-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.lmv-state {
  padding: 40px 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
}
.lmv-state--err {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
}
.lmv-retry {
  height: 40px;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}

.lmv-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.lmv-section__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.lmv-select,
.lmv-date,
.lmv-textarea {
  width: 100%;
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  font-family: inherit;
  font-size: 14px;
  background: var(--color-surface);
  color: var(--color-text-primary);
}
.lmv-textarea {
  resize: vertical;
}

.lmv-hint {
  margin: 0;
  font-size: 12px;
  color: var(--color-warning-text);
}

.lmv-submit {
  height: 48px;
  margin-top: var(--space-sm);
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
}
.lmv-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.icon {
  display: block;
}
</style>
