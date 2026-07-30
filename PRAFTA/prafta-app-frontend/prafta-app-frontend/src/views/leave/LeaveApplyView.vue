<!--
  LeaveApplyView.vue — 연차 신청 라우트 컨테이너 (모바일 앱, prafta-app-018-C)
  - 분해: .claude/requests/app_requests/prafta-app-018-C-tasks.md
  - 라우트: /LeaveApply?workYmd=YYYYMMDD&nodeCd=N001 (workYmd/nodeCd 는 선택 — 연차현황 진입은 없음)
  - 참조 패턴: views/req/AttdRequestView.vue
      (헤더 + 본문 스크롤 + 폼 컴포넌트 + sessionStorage 컨텍스트 + API 제출 + 디자인 토큰 루트 1회 선언)
  - 역할 분담: 본 컨테이너 = 메타/프리셋 조회(018-A) + 제출(018-B) + 라우팅. 폼(LeaveApplyForm) = 입력/표시/검증.
  - 디자인 토큰: AttdRequestView(.attd-req-view) 와 동일 세트를 .leave-apply-view 루트에 1회 선언.
-->
<template>
  <div class="leave-apply-view">
    <!-- 헤더 -->
    <header class="lav-hd">
      <button type="button" class="lav-hd__back" aria-label="뒤로" @click="onCancel">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-lav-chev-left" />
        </svg>
      </button>
      <h1 class="lav-hd__title">연차 신청</h1>
      <span class="lav-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 (스크롤 영역) -->
    <main class="lav-body">
      <!-- 로딩 -->
      <p v-if="isLoadingMeta" class="lav-state">불러오는 중...</p>

      <!-- 에러 (메타 조회 실패) -->
      <div v-else-if="metaError" class="lav-state lav-state--err">
        <p>{{ metaError }}</p>
        <button type="button" class="lav-retry" @click="loadMeta">다시 시도</button>
      </div>

      <!-- 폼 -->
      <LeaveApplyForm
        v-else
        :meta="meta"
        :presets="presets"
        :context="context"
        :submitting="isSubmitting"
        :preview="preview"
        :preview-loading="isPreviewLoading"
        :day-schedule="daySchedule"
        @submit="onSubmit"
        @cancel="onCancel"
        @preview-request="onPreviewRequest"
        @day-schedule-request="onDayScheduleRequest"
      />
    </main>

    <!-- 인라인 SVG sprite -->
    <svg width="0" height="0" class="lav-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-lav-chev-left"
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
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

import LeaveApplyForm from './components/LeaveApplyForm.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 반응형 상태 (developer: 조회/제출/컨텍스트 로딩 로직 보완) ────────────
const isLoadingMeta = ref(true)
const metaError = ref('')
// 018-A apply-meta 응답 ({ leaveTypes: [...] })
const meta = ref({ leaveTypes: [] })
// 018-A approval-presets 응답의 presets 배열
const presets = ref([])
// 진입 컨텍스트 (특정 일자 진입 시 sessionStorage; 연차현황 진입은 빈 객체)
const context = ref({})
const isSubmitting = ref(false)

// 컨텍스트 sessionStorage 키 (MyAttendanceView 가 저장 → 본 화면이 1회 읽고 제거)
const LEAVE_CTX_KEY = 'leave_apply_ctx_v1'

const onCancel = () => {
  router.back()
}

// ── 018-A 메타/프리셋 조회 ───────────────────────────────────────────────
const loadMeta = async () => {
  // TODO(developer): 병렬 조회
  //   GET /appApi/leaveflow/apply-meta        → meta.value = data ({ leaveTypes })
  //   GET /appApi/leaveflow/approval-presets  → presets.value = data.presets || []
  //   실패 시 metaError 세팅. isLoadingMeta=false 로 마무리. (401/403/500 은 인터셉터 처리)
  isLoadingMeta.value = true
  metaError.value = ''
  try {
    const [metaRes, presetRes] = await Promise.all([
      api.get('/appApi/leaveflow/apply-meta'),
      api.get('/appApi/leaveflow/approval-presets'),
    ])
    meta.value = metaRes?.data || { leaveTypes: [] }
    presets.value = presetRes?.data?.presets || []
  } catch (err) {
    console.error('[LeaveApply] 메타 조회 실패:', err?.message)
    metaError.value = resolveApiErrorMessage(err, '연차 정보를 불러오지 못했어요.')
  } finally {
    isLoadingMeta.value = false
  }
}

// ── LC-10: 예상 차감 preview (POST /appApi/leaveflow/preview-deduction) ──
// 폼(LeaveApplyForm)이 디바운스 후 emit 한 payload 를 받아 조회 전용 preview 를 호출한다.
//   실패는 비치명적: 카드 표시만 생략하고 신청은 가능(서버가 최종 판정 — plan §5-D).
// { chargeDays, floorApplied, capApplied, insufficientBalance, convMinutes, floorDays,
//   remnantTriggered, remnantDays, companyCoverMinutes(PC-05 짜투리 보전 — 발동 시 insufficientBalance=false) } | null
const preview = ref(null)
const isPreviewLoading = ref(false)
// 응답 역전 방지 시퀀스 — 마지막 요청의 응답만 채택(빠른 입력 변경 시 stale 응답 무시).
let previewSeq = 0

const onPreviewRequest = async (payload) => {
  // null = 입력 미완성/비대상 단위 → 표시 해제(잔존 카드 누수 방지).
  if (!payload) {
    previewSeq += 1
    preview.value = null
    isPreviewLoading.value = false
    return
  }
  const seq = ++previewSeq
  isPreviewLoading.value = true
  try {
    const res = await api.post('/appApi/leaveflow/preview-deduction', payload)
    if (seq !== previewSeq) return // stale 응답 폐기
    preview.value = res?.data ?? null
  } catch (err) {
    // preview 실패는 표시 생략(에러 알림 없음 — 신청 자체는 서버가 최종 판정).
    console.warn('[LeaveApply] 예상 차감 preview 실패(표시 생략):', err?.message)
    if (seq === previewSeq) preview.value = null
  } finally {
    if (seq === previewSeq) isPreviewLoading.value = false
  }
}

// ── 시간차 휴게시간 안내: 대상일 근무/휴게 시각 조회 (GET /appApi/leaveflow/day-schedule) ──
// 폼이 시간차 단위 + 날짜 완성 시 emit → 조회 전용 호출. 실패는 비치명적(표시만 생략).
// { hasSchedule, fstSchStrTime, fstSchEndTime, secSchStrTime, secSchEndTime,
//   fstBrkStrTime, fstBrkEndTime, secBrkStrTime, secBrkEndTime } | null
const daySchedule = ref(null)
// 응답 역전 방지 시퀀스(preview 패턴 미러) — 빠른 날짜 변경 시 stale 응답 무시.
let dayScheduleSeq = 0

const onDayScheduleRequest = async (workYmd) => {
  // null = 비대상 단위/날짜 미완성 → 표시 해제(잔존 안내 누수 방지).
  if (!workYmd) {
    dayScheduleSeq += 1
    daySchedule.value = null
    return
  }
  const seq = ++dayScheduleSeq
  try {
    const res = await api.get('/appApi/leaveflow/day-schedule', { params: { workYmd } })
    if (seq !== dayScheduleSeq) return // stale 응답 폐기
    daySchedule.value = res?.data ?? null
  } catch (err) {
    console.warn('[LeaveApply] 일자 스케줄 조회 실패(표시 생략):', err?.message)
    if (seq === dayScheduleSeq) daySchedule.value = null
  }
}

// ── 018-B 제출 ───────────────────────────────────────────────────────────
// payload(폼 emit): { leaveCd, leaveType, workYmd, useUnitType, startTime, endTime,
//                     reason, approverUserCds, presetId } ← 018-B 요청 본문과 1:1
const onSubmit = async (payload) => {
  if (isSubmitting.value) return
  // TODO(developer):
  //   - workYmd 폴백: payload.workYmd || context.value.workYmd (연차현황 진입은 폼에서 날짜 직접 선택).
  //   - POST /appApi/leaveflow/apply, body = payload (식별값/nodeCd 는 서버 JWT — 본문 비신뢰).
  //   - 성공 → showAlert('연차가 신청되었어요') 후 router.back().
  //   - 실패 → resolveApiErrorMessage 로 서버 메시지 표면화(ATTD_400_050/051/052/054/055/056/102 등).
  //     ⚠️ 앱 인터셉터 토큰오류 오발동 주의: ATTD_400_* 는 인터셉터가 로그아웃 처리하지 않음(안전).
  isSubmitting.value = true
  try {
    // workYmd 폴백: 폼이 보낸 날짜 우선, 없으면 컨텍스트 workYmd(특정 일자 진입). 식별값/nodeCd 는 서버 JWT.
    const body = {
      ...payload,
      workYmd: payload?.workYmd || context.value?.workYmd || '',
    }
    await api.post('/appApi/leaveflow/apply', body)
    await showAlert('연차가 신청되었어요')
    router.back()
  } catch (err) {
    console.error('[LeaveApply] 신청 실패:', err?.message)
    showAlert(resolveApiErrorMessage(err, '연차 신청 중 오류가 발생했습니다.'))
  } finally {
    isSubmitting.value = false
  }
}

// ── 컨텍스트 로드 (선택) + 메타 조회 ─────────────────────────────────────
onMounted(() => {
  // 1) 특정 일자 진입 시 sessionStorage 컨텍스트 로드(있으면) 후 제거. 없으면 빈 컨텍스트(연차현황 진입).
  try {
    const raw = sessionStorage.getItem(LEAVE_CTX_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      sessionStorage.removeItem(LEAVE_CTX_KEY)
      // route.query.workYmd 와 정합할 때만 채택(stale 방지).
      const qYmd = String(route.query.workYmd || '')
      if (!qYmd || parsed.workYmd === qYmd) context.value = parsed || {}
    }
  } catch (e) {
    console.error('[LeaveApply] 컨텍스트 파싱 실패:', e?.message)
  }
  // 2) 메타/프리셋 조회
  loadMeta()
})
</script>

<style scoped>
.leave-apply-view {
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
.lav-hd {
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
.lav-hd__back {
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
.lav-hd__title {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.lav-hd__spacer {
  width: 44px;
  height: 44px;
}

/* 본문 */
.lav-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.lav-state {
  padding: 40px 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
}
.lav-state--err {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
}
.lav-retry {
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

.icon {
  display: block;
}
</style>
