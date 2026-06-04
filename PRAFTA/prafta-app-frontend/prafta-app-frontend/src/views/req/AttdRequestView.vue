<!--
  AttdRequestView.vue — 근태 요청 폼 라우트 컨테이너 (모바일 앱)
  - 작업 ID: PRAFTA-APP-007-9 (분해: .claude/requests/app_requests/prafta-app-007-plan.md §8.1)
  - 라우트: /AttdRequest?type=schedModify|attdCorrection|overtime&workYmd=YYYYMMDD&nodeCd=N001
  - 디자인 토큰: MyAttendanceView(.my-attd-view) 와 동일 세트를 .attd-req-view 루트에 1회 선언.
-->
<template>
  <div class="attd-req-view">
    <!-- 헤더 -->
    <header class="req-hd">
      <button type="button" class="req-hd__back" aria-label="뒤로" @click="onCancel">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-req-chev-left" />
        </svg>
      </button>
      <h1 class="req-hd__title">{{ headerTitle }}</h1>
      <span class="req-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 (스크롤 영역, 폼 컴포넌트 분기) -->
    <main class="req-body">
      <SchedModifyForm
        v-if="formType === 'schedModify' && context"
        :context="context"
        :submitting="isSubmitting"
        :presets="presets"
        :approval-context="approvalContext"
        @submit="onSubmit"
        @cancel="onCancel"
      />
      <AttdCorrectionForm
        v-else-if="formType === 'attdCorrection' && context"
        :context="context"
        :submitting="isSubmitting"
        :presets="presets"
        :approval-context="approvalContext"
        @submit="onSubmit"
        @cancel="onCancel"
      />
      <OvertimeForm
        v-else-if="formType === 'overtime' && context"
        :context="context"
        :submitting="isSubmitting"
        :presets="presets"
        :approval-context="approvalContext"
        @submit="onSubmit"
        @cancel="onCancel"
      />

      <!-- 컨텍스트 누락 폴백 -->
      <div v-else class="req-fallback">
        <p>요청 화면을 열 수 없습니다. 근태 화면으로 돌아가 주세요.</p>
      </div>
    </main>

    <!-- 인라인 SVG sprite -->
    <svg width="0" height="0" class="req-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-req-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

import SchedModifyForm from './components/SchedModifyForm.vue'
import AttdCorrectionForm from './components/AttdCorrectionForm.vue'
import OvertimeForm from './components/OvertimeForm.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 폼 타입 (쿼리 파라미터)
const ALLOWED_TYPES = ['schedModify', 'attdCorrection', 'overtime']
const formType = computed(() => {
  const t = String(route.query.type || '')
  return ALLOWED_TYPES.includes(t) ? t : ''
})

const HEADER_TITLES = {
  schedModify: '스케줄 수정 요청',
  attdCorrection: '근태 보정 요청',
  overtime: '초과근무 신청',
}
const headerTitle = computed(() => HEADER_TITLES[formType.value] || '근태 요청')

// 폼 타입 별 endpoint 매핑
const ENDPOINTS = {
  schedModify: '/appApi/req07/sched-modify',
  attdCorrection: '/appApi/req07/attd-correction',
  overtime: '/appApi/req07/overtime',
}

// 컨텍스트 (sessionStorage 로 이전 화면에서 전달)
const context = ref(null)
const isSubmitting = ref(false)

// prafta-app-009: 결재선 메타 — 본인 소유 프리셋 + 결재선 분기 컨텍스트(자체근태승인 여부).
//   폼 진입 시 1회 로드하여 자식 폼에 props 로 전달(각 폼이 중복 호출하지 않도록 부모 단일 로드).
const presets = ref([])
//   approvalContext: { selfApprvYn:'Y'|'N', isNodeAdmin:bool } | null(로드 전/실패 → 폼이 결재선 노출 폴백).
const approvalContext = ref(null)

const onCancel = () => {
  router.back()
}

// 결재선 프리셋 로드(기존 endpoint 재사용, D1 풀 공유). 실패해도 폼은 빈 프리셋으로 동작.
const loadPresets = async () => {
  try {
    const { data } = await api.get('/appApi/mypage01/approval-presets')
    presets.value = Array.isArray(data?.presets) ? data.presets : []
  } catch (e) {
    console.error('[AttdRequest] 결재선 프리셋 로드 실패:', e?.message)
    presets.value = []
  }
}

// 결재선 분기 컨텍스트 로드(009-3 매퍼 재사용 endpoint). 실패 시 null → 폼은 결재선 노출 폴백.
const loadApprovalContext = async (workYmd) => {
  try {
    const { data } = await api.get('/appApi/req09/approval-context', {
      params: { workYmd },
    })
    approvalContext.value = data || null
  } catch (e) {
    console.error('[AttdRequest] 결재선 컨텍스트 로드 실패:', e?.message)
    approvalContext.value = null
  }
}

// 폼별 submit payload 구조 :
//   schedModify    → { slots:[{workSeq, schCd}], reqReason }
//   attdCorrection → { slots:[{workSeq, startDate, startTime, endDate, endTime}], reqReason }
//   overtime       → { slots:[{workSeq, startDate, startTime, endDate, endTime}], reqReason } (prafta-app-016: otType 제거)
const onSubmit = async (payload) => {
  if (isSubmitting.value) return
  const endpoint = ENDPOINTS[formType.value]
  if (!endpoint || !context.value) {
    showAlert('요청을 처리할 수 없습니다.')
    return
  }
  const body = {
    workYmd: context.value.workYmd,
    nodeCd: context.value.nodeCd || '',
    slots: payload.slots,
    reqReason: payload.reqReason,
  }
  // prafta-app-009: 결재선 전개 결과(SSOT). 폼이 전달한 경우에만 본문에 포함.
  //   approverUserCds 는 전개된 최종 순서(SSOT). presetId 는 미전송이 기본(백엔드 폴백 허용).
  if (Array.isArray(payload.approverUserCds)) {
    body.approverUserCds = payload.approverUserCds
  }
  if (payload.presetId) {
    body.presetId = payload.presetId
  }
  isSubmitting.value = true
  try {
    await api.post(endpoint, body)
    await showAlert('요청이 등록되었습니다')
    router.back()
  } catch (err) {
    // 백엔드 메시지 우선, 폴백은 한국어. PII 로깅 회피 — 메시지만.
    console.error('[AttdRequest] 요청 등록 실패:', err?.message)
    const msg = resolveApiErrorMessage(err, '요청 등록 중 오류가 발생했습니다.')
    showAlert(msg)
  } finally {
    isSubmitting.value = false
  }
}

const CONTEXT_KEY = 'attd_req_ctx_v1'

onMounted(() => {
  // 1) type / workYmd 유효성
  if (!formType.value) {
    showAlert('잘못된 요청 화면입니다.')
    router.back()
    return
  }
  const workYmd = String(route.query.workYmd || '')
  if (!/^\d{8}$/.test(workYmd)) {
    showAlert('대상 일자가 없습니다.')
    router.back()
    return
  }

  // 2) sessionStorage 에서 컨텍스트 로드 + 즉시 제거 (P7 stale 방지)
  try {
    const raw = sessionStorage.getItem(CONTEXT_KEY)
    if (!raw) {
      showAlert('컨텍스트가 만료되었습니다. 근태 화면에서 다시 시도해 주세요.')
      router.back()
      return
    }
    const parsed = JSON.parse(raw)
    sessionStorage.removeItem(CONTEXT_KEY)
    if (parsed.workYmd !== workYmd) {
      showAlert('컨텍스트가 일치하지 않습니다.')
      router.back()
      return
    }
    context.value = parsed

    // prafta-app-009: 컨텍스트 확정 후 결재선 메타(프리셋 + 분기) 로드(비동기, 폼 표시 비차단).
    loadPresets()
    loadApprovalContext(workYmd)
  } catch (e) {
    console.error('[AttdRequest] 컨텍스트 파싱 실패:', e?.message)
    showAlert('컨텍스트를 불러오지 못했습니다.')
    router.back()
  }
})
</script>

<style scoped>
.attd-req-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-primary-text-deep: #15803d;
  --color-primary-text-darkest: #14532d;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-warning: #f59e0b;
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
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100vh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.req-hd {
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
.req-hd__back {
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
.req-hd__title {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.req-hd__spacer {
  width: 44px;
  height: 44px;
}

/* 본문 */
.req-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.req-fallback {
  padding: 40px 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.icon {
  display: block;
}
</style>
