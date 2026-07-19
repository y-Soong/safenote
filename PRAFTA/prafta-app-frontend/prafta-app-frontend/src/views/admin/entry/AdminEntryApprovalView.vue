<!--
  AdminEntryApprovalView.vue — 관리자 모드 일용직 입장 승인 목록 (앱)
  - 분해: .claude/requests/common/작업지시서_일용직-계약서-서명-승인제.plan.md §4 UI-DC-03 / §2 T4
  - 요청서 근거: §4-2(앱 관리자 승인), D9(전체 체크 → 일괄 승인 필수), D10(거부 사유 기록)
  - 진입: AdminLauncherView/AdminTabBar 신규 모듈 ENTRY → /AdminEntryApproval (보호 라우트, developer)
      진입 게이팅은 access-context.moduleActiveMap(서버 산출) — 본 화면은 클라이언트 역할 분기 없음(AdminApprovalView 미러).
  - 참조 패턴: AdminApprovalView(헤더/토큰), AdminApprovalPendingList(카드 리스트), AdminApprovalRejectSheet(거부 시트)
  - planner 라운드 스코프: template + style + 체크 선택 UI 토글 완성. script 는 선언 + TODO(developer).
  - developer 라운드 스코프(TODO):
      (1) GET /appApi/entryadmin01/pending-lists — 당일 대기 목록(현재 사업장 스코프)
      (2) POST /appApi/entryadmin01/approve { reqIds: [] } — 개별/일괄 공용
      (3) POST /appApi/entryadmin01/reject { reqId, reason }
      (4) /AdminEntryApproval 보호 라우트 + AdminLauncher 진입점(ENTRY) 연결
      (5) 처리 성공 후 목록 재조회 + $alert 안내
-->
<template>
  <div class="admin-entry-view">
    <!-- 헤더: 관리자 모드(런처) 복귀 + 타이틀 -->
    <header class="ae-hd">
      <button type="button" class="ae-hd__back" aria-label="뒤로" @click="onBack">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <h1 class="ae-hd__title">입장 승인</h1>
      <span class="ae-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 일괄 승인 바 (D9) -->
    <div v-if="!isLoading && requests.length > 0" class="ae-bulk">
      <label class="ae-bulk__all">
        <input type="checkbox" class="ae-check" :checked="allChecked" @change="onToggleAll" />
        <span>전체 선택</span>
      </label>
      <button
        type="button"
        class="ae-bulk__btn"
        :disabled="checkedReqIds.length === 0 || isProcessing"
        @click="onBulkApprove"
      >
        선택 {{ checkedReqIds.length }}건 일괄 승인
      </button>
    </div>

    <!-- 본문: 당일 승인 대기 목록 -->
    <main class="ae-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="ae-state" aria-live="polite">불러오는 중...</div>

      <!-- 빈 상태 -->
      <div v-else-if="requests.length === 0" class="ae-state">
        오늘 승인 대기 건이 없습니다.
      </div>

      <!-- 대기 카드 리스트 -->
      <ul v-else class="ae-list">
        <li v-for="req in requests" :key="req.reqId" class="ae-card">
          <label class="ae-card__check">
            <input
              type="checkbox"
              class="ae-check"
              :checked="req.checked"
              @change="onToggleOne(req)"
            />
          </label>
          <div class="ae-card__main">
            <div class="ae-card__top">
              <span class="ae-card__name">{{ req.userNm }}</span>
              <span
                class="ae-badge"
                :class="req.reqType === '01' ? 'ae-badge--new' : 'ae-badge--reentry'"
              >
                {{ req.reqType === '01' ? '신규가입' : '재입장' }}
              </span>
            </div>
            <!-- 서버 마스킹 문자열("***-****-1234") 그대로 표기 — 010 프리픽스 재조립 금지(qa L-4) -->
            <p class="ae-card__meta">
              {{ req.mblNoMasked }} · 요청 {{ req.reqDtime }}
            </p>
          </div>
          <div class="ae-card__actions">
            <button
              type="button"
              class="ae-btn ae-btn--approve"
              :disabled="isProcessing"
              @click="onApprove(req)"
            >
              승인
            </button>
            <button
              type="button"
              class="ae-btn ae-btn--reject"
              :disabled="isProcessing"
              @click="onOpenReject(req)"
            >
              거부
            </button>
          </div>
        </li>
      </ul>
    </main>

    <!-- 거부 사유 바텀시트 (D10: 사유 기록) -->
    <transition name="ae-sheet-fade">
      <div
        v-if="rejectTarget"
        class="ae-sheet__dimmer"
        role="dialog"
        aria-modal="true"
        aria-label="입장 거부"
        @click.self="onCloseReject"
      >
        <div class="ae-sheet">
          <div class="ae-sheet__handle" aria-hidden="true"></div>
          <header class="ae-sheet__header">
            <h2 class="ae-sheet__title">입장 거부</h2>
            <button type="button" class="ae-sheet__close" aria-label="닫기" @click="onCloseReject">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </header>
          <div class="ae-sheet__body">
            <p class="ae-sheet__who">
              {{ rejectTarget.userNm }} ({{ rejectTarget.mblNoMasked }})
            </p>
            <label class="ae-field">
              <span class="ae-field__label">거부 사유<span class="ae-req">*</span></span>
              <textarea
                v-model.trim="rejectReason"
                class="ae-field__textarea"
                rows="3"
                maxlength="200"
                placeholder="거부 사유 (필수, 최대 200자 — 내부 기록용, 근로자에게 노출되지 않습니다)"
              ></textarea>
            </label>
            <p class="ae-sheet__hint">
              반복 거부 인원은 웹 관리자 화면(일일계정 블랙리스트)에서 차단할 수 있습니다.
            </p>
          </div>
          <footer class="ae-sheet__footer">
            <button
              type="button"
              class="ae-btn ae-btn--reject ae-sheet__submit"
              :disabled="rejectReason.length === 0 || isProcessing"
              @click="onConfirmReject"
            >
              거부 확정
            </button>
          </footer>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/axios'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert/confirm 폴백(앱 전역 $alert/$confirm 우선) — AdminLauncherView 패턴 동일.
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const showConfirm = (message) => {
  if (proxy?.$confirm) return proxy.$confirm(message)
  return Promise.resolve(window.confirm(message))
}

// ── 상태 ────────────────────────────────────────────────────────────
const isLoading = ref(true)
const isProcessing = ref(false)

// 당일 승인 대기 목록. 항목: { reqId, userNm, mblNoMasked(서버 마스킹 문자열), reqType('01'|'02'), reqDtime, checked }
const requests = ref([])

// 거부 시트 대상/사유
const rejectTarget = ref(null)
const rejectReason = ref('')

const checkedReqIds = computed(() =>
  requests.value.filter((r) => r.checked).map((r) => r.reqId),
)
const allChecked = computed(
  () => requests.value.length > 0 && requests.value.every((r) => r.checked),
)

// ── 조회 ─────────────────────────────────────────────────────────────
// 당일 승인 대기('01') 목록 — 사업장 스코프는 서버가 JWT gv_siteCd 로 강제(클라 파라미터 없음).
//   서버 응답 mblNo 는 마스킹 문자열("***-****-1234") — 가공 없이 그대로 표시(qa L-4, 010 재조립 금지).
const loadPendingList = async () => {
  isLoading.value = true
  try {
    const { data } = await api.get('/appApi/entryadmin01/pending-lists')
    const list = Array.isArray(data?.pendingList) ? data.pendingList : []
    requests.value = list.map((r) => ({
      reqId: r.reqId,
      userNm: r.userNm,
      mblNoMasked: r.mblNo || '-',
      reqType: r.reqType,
      reqDtime: r.reqDtime,
      checked: false,
    }))
  } catch (e) {
    console.warn('[AdminEntryApproval] 대기 목록 조회 실패:', e?.message)
    requests.value = []
    await showAlert(
      e?.response?.data?.message || '승인 대기 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.',
    )
  } finally {
    isLoading.value = false
  }
}

// ── 라이프사이클 ─────────────────────────────────────────────────────
onMounted(() => {
  loadPendingList()
})

// ── 체크 선택 (UI 토글 — 허용 범위) ──────────────────────────────────
const onToggleAll = () => {
  const next = !allChecked.value
  requests.value.forEach((r) => {
    r.checked = next
  })
}

const onToggleOne = (req) => {
  req.checked = !req.checked
}

// ── 액션 ────────────────────────────────────────────────────────────
const onBack = () => {
  router.replace('/AdminHome')
}

// 승인 공용 호출기(개별/일괄 공용, D9). all-or-nothing — 실패 시 서버가 전체 롤백.
const callApprove = async (reqIds) => {
  if (!reqIds || reqIds.length === 0 || isProcessing.value) return
  isProcessing.value = true
  try {
    const { data } = await api.post('/appApi/entryadmin01/approve', { reqIds })
    await showAlert(`${data?.processedCount ?? reqIds.length}건 승인 처리되었습니다.`)
  } catch (e) {
    console.warn('[AdminEntryApproval] 승인 처리 실패:', e?.message)
    await showAlert(
      e?.response?.data?.message || '승인 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.',
    )
  } finally {
    isProcessing.value = false
    // 성공/실패(이미 처리됨 포함) 모두 최신 상태로 재조회.
    await loadPendingList()
  }
}

const onApprove = async (req) => {
  const ok = await showConfirm(`${req.userNm} 님의 입장을 승인하시겠습니까?`)
  if (!ok) return
  await callApprove([req.reqId])
}

const onBulkApprove = async () => {
  const reqIds = checkedReqIds.value
  if (reqIds.length === 0) return
  const ok = await showConfirm(`선택한 ${reqIds.length}건을 승인하시겠습니까?`)
  if (!ok) return
  await callApprove(reqIds)
}

const onOpenReject = (req) => {
  rejectTarget.value = req
  rejectReason.value = ''
}

const onCloseReject = () => {
  rejectTarget.value = null
  rejectReason.value = ''
}

const onConfirmReject = async () => {
  if (!rejectTarget.value || rejectReason.value.length === 0 || isProcessing.value) return
  isProcessing.value = true
  try {
    await api.post('/appApi/entryadmin01/reject', {
      reqId: rejectTarget.value.reqId,
      reason: rejectReason.value,
    })
    onCloseReject()
    await showAlert('거부 처리되었습니다.')
  } catch (e) {
    console.warn('[AdminEntryApproval] 거부 처리 실패:', e?.message)
    await showAlert(
      e?.response?.data?.message || '거부 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.',
    )
  } finally {
    isProcessing.value = false
    await loadPendingList()
  }
}
</script>

<style scoped>
.admin-entry-view {
  /* 디자인 토큰 자급(AdminApprovalView 세트 미러) — 하드코딩 사용 금지 */
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-danger-text: #b91c1c;
  --color-danger-bg: #fef2f2;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-badge-new-bg: #dbeafe;
  --color-badge-new-text: #1d4ed8;
  --color-badge-reentry-bg: #dcfce7;
  --color-badge-reentry-text: #15803d;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--color-bg);
}

/* 헤더 */
.ae-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.ae-hd__back {
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
.ae-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ae-hd__spacer {
  width: 32px;
}

/* 일괄 승인 바 */
.ae-bulk {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-sm) var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.ae-bulk__all {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
}
.ae-bulk__btn {
  padding: var(--space-sm) var(--space-md);
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.ae-bulk__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ae-check {
  width: 18px;
  height: 18px;
  accent-color: var(--color-primary);
}

/* 본문/리스트 */
.ae-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg);
}
.ae-state {
  padding: 48px 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.ae-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

/* 대기 카드 */
.ae-card {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
}
.ae-card__check {
  display: inline-flex;
  align-items: center;
}
.ae-card__main {
  flex: 1;
  min-width: 0;
}
.ae-card__top {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.ae-card__name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ae-card__meta {
  margin: var(--space-xs) 0 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ae-badge {
  padding: 1px var(--space-sm);
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
}
.ae-badge--new {
  background: var(--color-badge-new-bg);
  color: var(--color-badge-new-text);
}
.ae-badge--reentry {
  background: var(--color-badge-reentry-bg);
  color: var(--color-badge-reentry-text);
}

/* 카드 액션 */
.ae-card__actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.ae-btn {
  min-width: 56px;
  padding: var(--space-xs) var(--space-md);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.ae-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.ae-btn--approve {
  border: 0;
  background: var(--color-primary);
  color: var(--color-surface);
}
.ae-btn--reject {
  border: 1px solid var(--color-danger);
  background: var(--color-surface);
  color: var(--color-danger-text);
}

/* 거부 바텀시트 */
.ae-sheet__dimmer {
  position: fixed;
  inset: 0;
  background: var(--color-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 120;
}
.ae-sheet {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}
.ae-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.ae-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.ae-sheet__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ae-sheet__close {
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
.ae-sheet__body {
  padding: var(--space-xs) var(--space-lg) var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  overflow-y: auto;
}
.ae-sheet__who {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.ae-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.ae-field__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.ae-req {
  color: var(--color-danger);
  margin-left: 2px;
}
.ae-field__textarea {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
  resize: vertical;
}
.ae-field__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}
.ae-sheet__hint {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ae-sheet__footer {
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.ae-sheet__submit {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 700;
  background: var(--color-danger);
  color: var(--color-surface);
  border: 0;
}

.ae-sheet-fade-enter-active,
.ae-sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}
.ae-sheet-fade-enter-from,
.ae-sheet-fade-leave-to {
  opacity: 0;
}
</style>
