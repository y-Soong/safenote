<!--
  LeaveChangeConsentView.vue — 근로자 연차 변경/삭제 동의·거부 화면 (prafta-com-008-C-3, 앱)
  유형: frontend-screen (모바일 앱, 근로자)
  연결 작업: PRAFTA-{C-4-app}
  참조 패턴: views/leave/LeaveApplyView.vue (헤더 + 본문 스크롤 + 디자인 토큰 루트 1회 선언 + TODO(developer)),
            views/leave/MyLeaveSummaryView.vue
  역할 분담: 골격 = 목록/상세/동의·거부 UI 구조 + 거부 사유 입력. developer = 조회/응답 API + 라우팅.
  ※ 관리자가 발의(REQUESTED)한 본인 연차 변경/삭제 요청에 대해 동의(AGREE)/거부(REJECT, 사유필수) 한다.
    응답은 본인 LEAVE_ID 한정(서버 JWT USER_CD 도출, body 비신뢰).
-->
<template>
  <div class="leave-consent-view">
    <header class="lcv-hd">
      <button type="button" class="lcv-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true"><use href="#i-lcv-chev-left" /></svg>
      </button>
      <h1 class="lcv-hd__title">연차 변경 동의</h1>
      <span class="lcv-hd__spacer" aria-hidden="true"></span>
    </header>

    <main class="lcv-body">
      <p v-if="isLoading" class="lcv-state">불러오는 중...</p>

      <div v-else-if="loadError" class="lcv-state lcv-state--err">
        <p>{{ loadError }}</p>
        <button type="button" class="lcv-retry" @click="loadRequests">다시 시도</button>
      </div>

      <p v-else-if="!requests.length" class="lcv-state">동의가 필요한 요청이 없어요.</p>

      <ul v-else class="lcv-list">
        <li v-for="req in requests" :key="req.changeReqId" class="lcv-card">
          <div class="lcv-card__row">
            <span class="lcv-card__label">대상 연차일</span>
            <span class="lcv-card__value">{{ req.targetStartDate }}</span>
          </div>
          <div class="lcv-card__row">
            <span class="lcv-card__label">요청유형</span>
            <span class="lcv-card__value">{{ req.reqTypeNm }}</span>
          </div>
          <div v-if="req.reqType === 'MOVE'" class="lcv-card__row">
            <span class="lcv-card__label">이동대상일</span>
            <span class="lcv-card__value">{{ req.moveTargetDate }}</span>
          </div>
          <div class="lcv-card__row">
            <span class="lcv-card__label">관리자 사유</span>
            <span class="lcv-card__value">{{ req.reqReason }}</span>
          </div>

          <!-- 거부 사유 입력(거부 선택 시 노출) -->
          <textarea
            v-if="rejectingId === req.changeReqId"
            v-model="rejectReason"
            class="lcv-reject-reason"
            rows="2"
            maxlength="500"
            placeholder="거부 사유를 입력하세요 (필수)"
          ></textarea>

          <div class="lcv-card__actions">
            <template v-if="rejectingId === req.changeReqId">
              <button type="button" class="lcv-btn lcv-btn--ghost" @click="cancelReject">취소</button>
              <button
                type="button"
                class="lcv-btn lcv-btn--danger"
                :disabled="!rejectReason.trim() || submitting"
                @click="onReject(req)"
              >
                거부 확정
              </button>
            </template>
            <template v-else>
              <button
                type="button"
                class="lcv-btn lcv-btn--ghost"
                :disabled="submitting"
                @click="startReject(req)"
              >
                거부
              </button>
              <button
                type="button"
                class="lcv-btn lcv-btn--primary"
                :disabled="submitting"
                @click="onAgree(req)"
              >
                동의
              </button>
            </template>
          </div>
        </li>
      </ul>
    </main>

    <svg width="0" height="0" class="lcv-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-lcv-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 상태 (developer: 조회/응답 로직 보완) ────────────────────────────────
const isLoading = ref(true)
const loadError = ref('')
const requests = ref([])
const submitting = ref(false)
const rejectingId = ref('')
const rejectReason = ref('')

const onBack = () => router.back()

const startReject = (req) => {
  rejectingId.value = req.changeReqId
  rejectReason.value = ''
}
const cancelReject = () => {
  rejectingId.value = ''
  rejectReason.value = ''
}

// 코드 → 라벨 매핑 (서버 row 는 코드값만 반환)
const REQ_TYPE_NM = { MOVE: '이동', DELETE: '삭제' }

// YYYYMMDD → "YYYY-MM-DD"
const fmtYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return ymd || ''
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
}

// 서버 row → 카드 표시 객체로 보강(라벨/포맷)
const toReq = (r) => ({
  changeReqId: r.changeReqId,
  reqType: r.reqType,
  reqTypeNm: REQ_TYPE_NM[r.reqType] || r.reqType,
  targetStartDate: fmtYmd(r.targetStartDate),
  moveTargetDate: fmtYmd(r.moveTargetDate),
  reqReason: r.reqReason,
})

// GET /appApi/leavechange/pending-consents
//   본인(JWT USER_CD) 대상 + 상태 REQUESTED 인 관리자 발의 요청 목록. → requests.value
const loadRequests = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const res = await api.get('/appApi/leavechange/pending-consents')
    const list = res?.data?.list || []
    requests.value = list.map(toReq)
  } catch (err) {
    loadError.value = resolveApiErrorMessage(err, '요청을 불러오지 못했어요.')
  } finally {
    isLoading.value = false
  }
}

// POST /appApi/leavechange/{changeReqId}/respond, body = { WORKER_RESPONSE: 'AGREE' }
//   서버: 본인 LEAVE_ID 검증 + REQUESTED 검증 → AGREED + 관리자 PUSH. 실제 반영은 관리자 최종 확인(웹).
const onAgree = async (req) => {
  if (submitting.value) return
  submitting.value = true
  try {
    await api.post(`/appApi/leavechange/${req.changeReqId}/respond`, {
      WORKER_RESPONSE: 'AGREE',
    })
    await showAlert('동의했어요. 관리자 확인 후 반영됩니다.')
    await loadRequests()
  } catch (err) {
    await showAlert(resolveApiErrorMessage(err, '처리에 실패했어요.'))
  } finally {
    submitting.value = false
  }
}

// POST /appApi/leavechange/{changeReqId}/respond,
//   body = { WORKER_RESPONSE: 'REJECT', RESPONSE_REASON: rejectReason }
//   서버: 본인 검증 + REQUESTED 검증 → REJECTED(원 연차 불변) + 관리자 PUSH.
const onReject = async (req) => {
  if (submitting.value) return
  if (!rejectReason.value.trim()) {
    await showAlert('거부 사유를 입력해 주세요.')
    return
  }
  submitting.value = true
  try {
    await api.post(`/appApi/leavechange/${req.changeReqId}/respond`, {
      WORKER_RESPONSE: 'REJECT',
      RESPONSE_REASON: rejectReason.value.trim(),
    })
    await showAlert('거부했어요.')
    cancelReject()
    await loadRequests()
  } catch (err) {
    await showAlert(resolveApiErrorMessage(err, '처리에 실패했어요.'))
  } finally {
    submitting.value = false
  }
}

onMounted(loadRequests)
</script>

<style scoped>
.leave-consent-view {
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
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

.lcv-hd {
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
.lcv-hd__back {
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
.lcv-hd__title {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 500;
}
.lcv-hd__spacer {
  width: 44px;
  height: 44px;
}

.lcv-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.lcv-state {
  padding: 40px 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
}
.lcv-state--err {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
}
.lcv-retry {
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

.lcv-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.lcv-card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.lcv-card__row {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
  font-size: 14px;
}
.lcv-card__label {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}
.lcv-card__value {
  color: var(--color-text-primary);
  text-align: right;
}

.lcv-reject-reason {
  width: 100%;
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-sm);
  font-family: inherit;
  font-size: 14px;
  resize: vertical;
}

.lcv-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
  margin-top: var(--space-xs);
}
.lcv-btn {
  height: 40px;
  padding: 0 var(--space-lg);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  border: 0.5px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-primary);
}
.lcv-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.lcv-btn--primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}
.lcv-btn--danger {
  background: var(--color-danger);
  border-color: var(--color-danger);
  color: #fff;
}
.lcv-btn--ghost {
  background: var(--color-surface);
}

.icon {
  display: block;
}
</style>
