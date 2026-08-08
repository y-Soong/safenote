<!--
  AdminLeaveChangeConfirmView.vue — 관리자 모드 연차 변경/삭제 최종 확인 (앱)
  - 맥락: 근로자가 동의(AGREED)한 관리자 발의 연차 변경/삭제 요청을, 관리자가 앱에서 최종 확인(CONFIRMED, 실제 반영)
    또는 반려(REJECTED, 원 연차 불변) 한다. 웹 Attd_13(연차 변경 동의 관리)의 앱 대응.
  - 진입: AdminLauncherView 상단 "연차 변경 확인 대기 N건" 배너 → /AdminLeaveChangeConfirm.
  - 권한/스코프: 서버(공유 Attd13Service)가 단일 출처로 검증(master/hr=전사, 노드 관리자=담당 노드+하위, safe 제외).
    비관리자 직접 진입은 서버가 fail-closed 차단(빈 목록/403).
  - 참조 패턴: AdminApprovalView(헤더) + MainView 연차 동의 카드 UI.
-->
<template>
  <div class="alc-view">
    <header class="alc-hd">
      <button type="button" class="alc-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true"><use href="#i-alc-chev-left" /></svg>
      </button>
      <h1 class="alc-hd__title">연차 변경 확인</h1>
      <span class="alc-hd__spacer" aria-hidden="true"></span>
    </header>

    <main
      class="alc-body"
      ref="scrollRef"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출(공통 컴포저블) -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <p v-if="isLoading" class="alc-state">불러오는 중...</p>

      <div v-else-if="loadError" class="alc-state alc-state--err">
        <p>{{ loadError }}</p>
        <button type="button" class="alc-retry" @click="loadRequests">다시 시도</button>
      </div>

      <p v-else-if="!requests.length" class="alc-state">확인 대기 중인 요청이 없어요.</p>

      <ul v-else class="alc-list">
        <li v-for="req in requests" :key="req.changeReqId" class="alc-card">
          <div class="alc-card__row">
            <span class="alc-card__label">대상자</span>
            <span class="alc-card__value">{{ req.targetUserNm }}</span>
          </div>
          <div class="alc-card__row">
            <span class="alc-card__label">대상 연차일</span>
            <span class="alc-card__value">{{ fmtYmd(req.targetStartDate) }}</span>
          </div>
          <!-- G1: 대상 연차 속성(종류/단위/구간/차감량). 사용 구간은 시간차(02~04)만 값이 있다. -->
          <div v-if="req.leaveNm" class="alc-card__row">
            <span class="alc-card__label">연차 종류</span>
            <span class="alc-card__value">{{ req.leaveNm }}</span>
          </div>
          <div v-if="unitLabel(req)" class="alc-card__row">
            <span class="alc-card__label">사용 단위</span>
            <span class="alc-card__value">{{ unitLabel(req) }}</span>
          </div>
          <div v-if="timeRange(req)" class="alc-card__row">
            <span class="alc-card__label">사용 구간</span>
            <span class="alc-card__value">{{ timeRange(req) }}</span>
          </div>
          <div v-if="leaveDaysLabel(req)" class="alc-card__row">
            <span class="alc-card__label">차감 일수</span>
            <span class="alc-card__value">{{ leaveDaysLabel(req) }}</span>
          </div>
          <div class="alc-card__row">
            <span class="alc-card__label">요청유형</span>
            <span class="alc-card__value">{{ reqTypeNm(req.reqType) }}</span>
          </div>
          <div v-if="req.reqType === 'MOVE'" class="alc-card__row">
            <span class="alc-card__label">이동대상일</span>
            <span class="alc-card__value">{{ fmtYmd(req.moveTargetDate) }}</span>
          </div>
          <div class="alc-card__row">
            <span class="alc-card__label">관리자 사유</span>
            <span class="alc-card__value">{{ req.reqReason || '-' }}</span>
          </div>
          <div class="alc-card__row">
            <span class="alc-card__label">근로자 응답</span>
            <span class="alc-card__value alc-card__value--ok">동의함</span>
          </div>

          <!-- 반려 사유 입력(반려 선택 시 노출) -->
          <textarea
            v-if="rejectingId === req.changeReqId"
            v-model="rejectReason"
            class="alc-reject-reason"
            rows="2"
            maxlength="500"
            placeholder="반려 사유를 입력하세요 (필수)"
          ></textarea>

          <!-- F-10 규약: 왼쪽=진행/확정(파괴적 진행=danger), 오른쪽=이탈(취소) -->
          <div class="alc-card__actions">
            <template v-if="rejectingId === req.changeReqId">
              <button
                type="button"
                class="alc-btn alc-btn--danger"
                :disabled="!rejectReason.trim() || submitting"
                @click="onReject(req)"
              >
                반려 확정
              </button>
              <button type="button" class="alc-btn alc-btn--ghost" :disabled="submitting" @click="cancelReject">
                취소
              </button>
            </template>
            <template v-else>
              <button type="button" class="alc-btn alc-btn--ghost" :disabled="submitting" @click="startReject(req)">
                반려
              </button>
              <button type="button" class="alc-btn alc-btn--primary" :disabled="submitting" @click="onConfirm(req)">
                확인(확정)
              </button>
            </template>
          </div>
        </li>
      </ul>
    </main>

    <svg width="0" height="0" class="alc-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-alc-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor"
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
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatYmdDisplay } from '@/utils/approvalFormat'
import { formatMinutesToHm, trimRawDays } from '@/utils/leaveFormat'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const askConfirm = (message) => {
  if (proxy?.$confirm) return proxy.$confirm(message)
  return Promise.resolve(window.confirm(message))
}

const isLoading = ref(true)
const loadError = ref('')
const requests = ref([])
const submitting = ref(false)
const rejectingId = ref('')
const rejectReason = ref('')

const onBack = () => router.back()

const REQ_TYPE_NM = { MOVE: '이동', DELETE: '삭제' }
const reqTypeNm = (t) => REQ_TYPE_NM[t] || t

const fmtYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return ymd || ''
  return formatYmdDisplay(ymd)
}

// ── G1: 대상 연차 속성 표기 ────────────────────────────────────────────────
// 시간차 단위(SYS025 02:2시간 / 03:1시간 / 04:30분)만 시각 구간을 보유한다.
//   반차(01)·반반차(05)는 신청 시 시각을 기록하지 않아 startTime/endTime 이 NULL — 구간 행을 숨긴다.
const HOURLY_UNITS = ['02', '03', '04']
const isHourlyUnit = (unitCode) => HOURLY_UNITS.includes(unitCode)

// "1230" → "12:30"
const fmtHm = (hhmm) => {
  const s = String(hhmm ?? '')
  return s.length >= 4 ? `${s.slice(0, 2)}:${s.slice(2, 4)}` : ''
}
const hhmmToMin = (hhmm) => {
  const s = String(hhmm ?? '')
  if (s.length !== 4) return null
  const h = parseInt(s.slice(0, 2), 10)
  const m = parseInt(s.slice(2, 4), 10)
  if (Number.isNaN(h) || Number.isNaN(m)) return null
  if (h < 0 || h > 23 || m < 0 || m > 59) return null
  return h * 60 + m
}

// 사용 단위 라벨 — 시간차면 '시간차 ' 접두(웹 AttdDayDetailPop 표기 관례와 동형).
const unitLabel = (req) => {
  if (!req?.unitNm) return ''
  return isHourlyUnit(req.useUnitType) ? `시간차 ${req.unitNm}` : req.unitNm
}

// 사용 구간 — "10:00~12:00 (2시간)". 차감 분(원본)이 있으면 그 값을, 없으면 구간에서 산출.
const timeRange = (req) => {
  if (!isHourlyUnit(req?.useUnitType) || !req?.startTime || !req?.endTime) return ''
  const range = `${fmtHm(req.startTime)}~${fmtHm(req.endTime)}`
  const raw = Number(req.leaveMinutes)
  if (Number.isFinite(raw) && raw > 0) return `${range} (${formatMinutesToHm(raw)})`
  const s = hhmmToMin(req.startTime)
  const e = hhmmToMin(req.endTime)
  if (s == null || e == null || e <= s) return range
  return `${range} (${formatMinutesToHm(e - s)})`
}

// 차감 일수 — 개인 분모(convMinutes)가 응답에 없으므로 "N일 H시간" 조립을 하지 않는다
//   (480 폴백 오표기 방지). 단순 일수 표기 + 시간차는 원본 분을 병기.
const leaveDaysLabel = (req) => {
  const v = req?.leaveDays
  if (v === null || v === undefined || v === '') return ''
  const n = Number(v)
  if (!Number.isFinite(n)) return ''
  const base = `${trimRawDays(n)}일`
  const raw = Number(req.leaveMinutes)
  if (isHourlyUnit(req.useUnitType) && Number.isFinite(raw) && raw > 0) {
    return `${base} (${formatMinutesToHm(raw)})`
  }
  return base
}

const startReject = (req) => {
  rejectingId.value = req.changeReqId
  rejectReason.value = ''
}
const cancelReject = () => {
  rejectingId.value = ''
  rejectReason.value = ''
}

// GET /appApi/leavechange/admin/pending-confirms — 관리자 스코프 내 AGREED(확인 대기) 목록.
const loadRequests = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const res = await api.get('/appApi/leavechange/admin/pending-confirms')
    requests.value = Array.isArray(res?.data?.list) ? res.data.list : []
  } catch (err) {
    loadError.value = resolveApiErrorMessage(err, '요청을 불러오지 못했어요.')
  } finally {
    isLoading.value = false
  }
}

// POST /appApi/leavechange/admin/{id}/confirm — 최종 확인(실제 반영). 되돌릴 수 없으므로 확인 후 진행.
const onConfirm = async (req) => {
  if (submitting.value) return
  const msg = req.reqType === 'MOVE'
    ? '이 요청을 확정하면 해당 연차가 실제로 이동됩니다. 진행할까요?'
    : '이 요청을 확정하면 해당 연차가 실제로 삭제(근무일 복귀)됩니다. 진행할까요?'
  const ok = await askConfirm(msg)
  if (!ok) return
  submitting.value = true
  try {
    await api.post(`/appApi/leavechange/admin/${req.changeReqId}/confirm`)
    await showAlert('확정했어요. 연차에 반영됩니다.')
    await loadRequests()
  } catch (err) {
    await showAlert(resolveApiErrorMessage(err, '처리에 실패했어요.'))
  } finally {
    submitting.value = false
  }
}

// POST /appApi/leavechange/admin/{id}/reject — 반려(사유필수). 원 연차 불변.
const onReject = async (req) => {
  if (submitting.value) return
  const reason = rejectReason.value.trim()
  if (!reason) {
    await showAlert('반려 사유를 입력해 주세요.')
    return
  }
  submitting.value = true
  try {
    await api.post(`/appApi/leavechange/admin/${req.changeReqId}/reject`, {
      REJECT_REASON: reason,
    })
    await showAlert('반려했어요.')
    cancelReject()
    await loadRequests()
  } catch (err) {
    await showAlert(resolveApiErrorMessage(err, '처리에 실패했어요.'))
  } finally {
    submitting.value = false
  }
}

// ── 당겨서 새로고침 (공통 컴포저블) — 확인 대기 목록을 재조회. 부작용 없는 조회만. ──
const scrollRef = ref(null)
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(scrollRef, async () => {
  await loadRequests()
})

onMounted(loadRequests)
</script>

<style scoped>
.alc-view {
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

  height: 100vh;
  height: 100dvh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR', sans-serif;
}

.alc-hd {
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
.alc-hd__back {
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
.alc-hd__title {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 500;
}
.alc-hd__spacer {
  width: 44px;
  height: 44px;
}

.alc-body {
  flex: 1;
  min-height: 0;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.alc-state {
  padding: 40px 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
}
.alc-state--err {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
}
.alc-retry {
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

.alc-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.alc-card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.alc-card__row {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
  font-size: 14px;
}
.alc-card__label {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}
.alc-card__value {
  color: var(--color-text-primary);
  text-align: right;
  word-break: break-all;
}
.alc-card__value--ok {
  color: var(--color-primary);
  font-weight: 600;
}

.alc-reject-reason {
  width: 100%;
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-sm);
  font-family: inherit;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
}

.alc-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
  margin-top: var(--space-xs);
}
.alc-btn {
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
.alc-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.alc-btn--primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}
.alc-btn--danger {
  background: var(--color-danger);
  border-color: var(--color-danger);
  color: #fff;
}
.alc-btn--ghost {
  background: var(--color-surface);
}

.icon {
  display: block;
}
.alc-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
</style>
