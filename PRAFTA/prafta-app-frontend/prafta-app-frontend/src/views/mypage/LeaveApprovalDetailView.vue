<!--
  LeaveApprovalDetailView.vue — 연차 결재 상세 + 결정(승인/반려) (사용자 모드)
  - 작업: 사용자연차결재-03 / UI 명세: UI-LA02
  - 진입: LeaveApprovalView 카드 → /LeaveApprovalDetail?reqId=&approvalStep=
  - gate(서버 산출)로 ⑥ 결정 영역 활성/비활성 렌더만(C1). 연차는 조정 불가 → 승인/반려 2지선다(재기획서 §5.8.4).
  - 반려 시트: 기존 AdminApprovalRejectSheet.vue 재사용(self-contained, 10자↑ 사유).
  - 디자인 토큰: MyPageView/ApprovalPresetListView 세트를 .leave-approval-detail 루트에 1회 선언.
-->
<template>
  <div class="leave-approval-detail">
    <!-- 헤더 -->
    <header class="lad-hd">
      <button type="button" class="lad-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true"><use href="#i-lad-chev-left" /></svg>
      </button>
      <h1 class="lad-hd__title">연차 결재 상세</h1>
      <span class="lad-hd__spacer" aria-hidden="true"></span>
    </header>

    <main class="lad-body">
      <div v-if="isLoading" class="lad-loading" aria-live="polite">불러오는 중...</div>

      <template v-else-if="meta">
        <!-- ① 메타 -->
        <section class="lad-card">
          <div class="lad-meta__top">
            <span class="lad-chip lad-chip--type">{{ body.leaveNm || meta.reqTypeNm || '연차' }}</span>
            <span v-if="body.paidYn" class="lad-chip">{{ body.paidYn === 'Y' ? '유급' : '무급' }}</span>
            <span v-if="body.unitNm" class="lad-chip">{{ body.unitNm }}</span>
            <span class="lad-meta__status">{{ meta.reqStatusNm }}</span>
          </div>
          <p class="lad-meta__requester">
            <strong>{{ meta.requesterUserNm }}</strong>
            <span v-if="meta.nodeNm" class="lad-meta__dept">{{ meta.nodeNm }}</span>
          </p>
          <p class="lad-meta__sub">대상일자 {{ fmtYmd(meta.targetYmd) }} · 요청 {{ fmtDt(meta.reqDate) }}</p>
        </section>

        <!-- ② 게이트 배너(조건부) -->
        <section v-if="!gate.canProcess && gateMessage" class="lad-banner" :class="bannerClass">
          {{ gateMessage }}
        </section>

        <!-- ③ 사용 구간 + 잔여 -->
        <section class="lad-card">
          <h2 class="lad-card__title">사용 구간</h2>
          <p class="lad-row">{{ rangeText }}</p>
          <div class="lad-balance">
            <span>부여 {{ body.balance?.granted ?? '-' }}</span>
            <span>사용 {{ body.balance?.used ?? '-' }}</span>
            <span class="lad-balance__remain">잔여 {{ body.balance?.remain ?? '-' }}</span>
          </div>
        </section>

        <!-- ④ 결재선 타임라인 -->
        <section class="lad-card">
          <h2 class="lad-card__title">결재선</h2>
          <ul class="lad-steps">
            <li
              v-for="s in body.steps || []"
              :key="s.approvalStep"
              class="lad-step"
              :class="{ 'is-mine': s.approvalStep === myStep }"
            >
              <span class="lad-step__no">{{ s.approvalStep }}</span>
              <span class="lad-step__name">{{ s.approverUserNm }}</span>
              <span class="lad-step__status" :class="stepStatusClass(s.approvalStatus)">
                {{ s.approvalStatusNm }}
              </span>
              <span v-if="s.approvalDate" class="lad-step__date">{{ fmtDt(s.approvalDate) }}</span>
            </li>
          </ul>
        </section>

        <!-- ⑤ 사유 -->
        <section v-if="reason" class="lad-card">
          <h2 class="lad-card__title">사유</h2>
          <p class="lad-row">{{ reason }}</p>
        </section>
      </template>

      <div v-else class="lad-empty">요청을 불러오지 못했습니다.</div>
    </main>

    <!-- ⑥ 결정(대기 + 내 차례일 때만) -->
    <footer v-if="meta && gate.canProcess" class="lad-actions">
      <button type="button" class="lad-btn lad-btn--reject" :disabled="submitting" @click="openRejectSheet">
        반려
      </button>
      <button type="button" class="lad-btn lad-btn--approve" :disabled="submitting" @click="onApprove">
        요청대로 승인
      </button>
    </footer>

    <!-- 반려 사유 시트(기존 관리자 컴포넌트 재사용 — self-contained) -->
    <AdminApprovalRejectSheet
      :open="rejectSheetOpen"
      :submitting="submitting"
      @close="rejectSheetOpen = false"
      @confirm="onRejectConfirm"
    />

    <svg width="0" height="0" class="lad-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-lad-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatYmdDisplay, formatDateTimeDisplay } from '@/utils/approvalFormat'

import AdminApprovalRejectSheet from '@/views/admin/approval/components/AdminApprovalRejectSheet.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// alert / confirm 폴백(앱 전역 우선) — AdminApprovalDetailView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const askConfirm = async (message) => {
  if (proxy?.$confirm) return await proxy.$confirm(message)
  return window.confirm(message)
}

// ── 서버 주입 상태 ──
const isLoading = ref(true)
const meta = ref(null) // GET detail.meta
const gate = ref({}) // GET detail.gate
const body = ref({}) // GET detail.body
const reason = ref('') // GET detail.reason
const myStep = ref(null) // detail.gate.myStep(서버 권위값) — 결재선에서 내 단계 강조

// ── UI 상태 ──
const rejectSheetOpen = ref(false)
const submitting = ref(false)

// 게이트 배너 메시지/색상(서버 *Yn 렌더만 — 비즈니스 판정 없음)
const gateMessage = computed(() => {
  const g = gate.value || {}
  if (g.selfBlockedYn) return '본인이 신청한 요청은 결재할 수 없습니다.'
  if (g.closedYn) return '대상 기간 근태가 마감되어 결재할 수 없습니다.'
  if (g.conflictYn) return g.conflictMsg || '이미 처리되었거나 결재 차례가 아닙니다.'
  return ''
})
const bannerClass = computed(() => {
  const g = gate.value || {}
  return g.closedYn || g.conflictYn ? 'lad-banner--danger' : 'lad-banner--warning'
})

const rangeText = computed(() => {
  const r = body.value?.appliedRange
  if (!r) return ''
  const sd = fmtYmd(r.startDate)
  const st = fmtHm(r.startTime)
  const ed = fmtYmd(r.endDate)
  const et = fmtHm(r.endTime)
  const left = [sd, st].filter(Boolean).join(' ')
  const right = [ed, et].filter(Boolean).join(' ')
  return right ? `${left} ~ ${right}` : left
})

const stepStatusClass = (st) => {
  if (st === '02') return 'is-approved'
  if (st === '03') return 'is-rejected'
  if (st === '01') return 'is-current'
  return 'is-wait'
}

// ── UI 토글 ──
const openRejectSheet = () => {
  rejectSheetOpen.value = true
}

// 처리(승인/반려) 위임 호출. 멱등 충돌(409) = 이미 처리 → 안내 후 목록 복귀.
const submitProcess = async (extra) => {
  if (submitting.value) return
  submitting.value = true
  try {
    const payload = {
      reqId: route.query.reqId,
      approvalStep: myStep.value,
      ...extra,
    }
    await api.post('/appApi/leaveflow/approval/process', payload)
    rejectSheetOpen.value = false
    await showAlert('처리되었습니다.')
    router.back()
  } catch (e) {
    if (e?.response?.status === 409) {
      rejectSheetOpen.value = false
      await showAlert('이미 처리된 요청입니다.')
      router.back()
      return
    }
    await showAlert(resolveApiErrorMessage(e, '처리에 실패했어요. 잠시 후 다시 시도해 주세요.'))
  } finally {
    submitting.value = false
  }
}

// 승인 → confirm → POST process { decision:'APPROVE' }
const onApprove = async () => {
  if (!gate.value.canProcess || submitting.value) return
  const ok = await askConfirm('요청대로 승인하시겠습니까?')
  if (!ok) return
  await submitProcess({ decision: 'APPROVE' })
}

// 반려 확정(시트가 10자↑ 보장) → POST process { decision:'REJECT', comment }
const onRejectConfirm = async (rejectReason) => {
  await submitProcess({ decision: 'REJECT', comment: rejectReason })
}

// 뒤로 → 직전(리스트)
const onBack = () => {
  router.back()
}

// 진입 시 상세 조회. reqId 없으면 잘못된 접근.
const loadDetail = async () => {
  const reqId = route.query.reqId
  if (!reqId) {
    await showAlert('잘못된 접근입니다.')
    router.back()
    return
  }
  isLoading.value = true
  try {
    const { data } = await api.get('/appApi/leaveflow/approval/detail', {
      params: { reqId, approvalStep: route.query.approvalStep || undefined },
    })
    meta.value = data?.meta || null
    gate.value = data?.gate || {}
    body.value = data?.body || {}
    reason.value = data?.reason || ''
    myStep.value =
      data?.gate?.myStep ??
      (route.query.approvalStep != null ? Number(route.query.approvalStep) : null)
    if (!meta.value) {
      await showAlert('요청 정보를 불러오지 못했어요.')
      router.back()
    }
  } catch (e) {
    // 401/403 은 axios 인터셉터가 처리. 그 외만 폴백 알림 후 복귀.
    console.warn('[LeaveApprovalDetail] 상세 조회 실패:', e?.message)
    await showAlert(
      resolveApiErrorMessage(e, '요청 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'),
    )
    router.back()
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadDetail()
})

// 날짜/일시 표시는 approvalFormat 단일 출처에 위임(점). HHMM → HH:MM 시각 포맷.
function fmtYmd(v) {
  return formatYmdDisplay(v)
}
function fmtHm(v) {
  if (v && v.length === 4) return `${v.slice(0, 2)}:${v.slice(2, 4)}`
  return v || ''
}
function fmtDt(v) {
  return formatDateTimeDisplay(v)
}
</script>

<style scoped>
.leave-approval-detail {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.06);
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.lad-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}
.lad-hd__back {
  width: 44px;
  height: 44px;
  margin-left: -10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.lad-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}
.lad-hd__spacer {
  width: 44px;
}

/* 본문 */
.lad-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-md) var(--space-lg) 24px;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.lad-loading,
.lad-empty {
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 카드 섹션 */
.lad-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--space-md) var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.lad-card__title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.lad-row {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

/* ① 메타 */
.lad-meta__top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-xs);
}
.lad-chip {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 600;
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
.lad-chip--type {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.lad-meta__status {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.lad-meta__requester {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 15px;
}
.lad-meta__dept {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.lad-meta__sub {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}

/* ② 게이트 배너 */
.lad-banner {
  padding: var(--space-md);
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.5;
}
.lad-banner--warning {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.lad-banner--danger {
  background: var(--color-danger-tint);
  color: var(--color-danger);
}

/* ③ 잔여 */
.lad-balance {
  display: flex;
  gap: var(--space-md);
  font-size: 13px;
  color: var(--color-text-secondary);
}
.lad-balance__remain {
  color: var(--color-primary);
  font-weight: 700;
}

/* ④ 결재선 */
.lad-steps {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.lad-step {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 13px;
}
.lad-step.is-mine {
  font-weight: 700;
}
.lad-step__no {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: 11px;
}
.lad-step__name {
  flex: 1;
  color: var(--color-text-primary);
}
.lad-step__status {
  font-size: 12px;
  font-weight: 600;
}
.lad-step__status.is-approved {
  color: var(--color-primary);
}
.lad-step__status.is-rejected {
  color: var(--color-danger);
}
.lad-step__status.is-current {
  color: var(--color-warning-text);
}
.lad-step__status.is-wait {
  color: var(--color-text-tertiary);
}
.lad-step__date {
  font-size: 11px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}

/* ⑥ 결정 */
.lad-actions {
  flex-shrink: 0;
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg) calc(var(--space-md) + env(safe-area-inset-bottom, 0px));
  background: var(--color-surface);
  border-top: 1px solid var(--color-border-light);
}
.lad-btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.lad-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.lad-btn--reject {
  background: var(--color-surface);
  color: var(--color-danger);
  border: 1px solid var(--color-border);
}
.lad-btn--approve {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}

.lad-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
