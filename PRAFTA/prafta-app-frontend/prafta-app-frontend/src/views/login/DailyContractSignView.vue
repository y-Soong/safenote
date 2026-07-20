<!--
  DailyContractSignView.vue — 일용직 근로계약서 서명 게이트 화면 (앱)
  - 분해: .claude/requests/common/작업지시서_일용직-계약서-서명-승인제.plan.md §4 UI-DC-01 / §2 T4
  - 요청서 근거: §4-1(서명 플로우), D3(승인 사이클당 1회), R2(계약서 미등록=스킵 — 게이트 판정은 termsGate 담당)
  - 진입: termsGate.routeAfterRequiredTerms 내부 게이트 체인 ①-b (일용직 전용, /DailyContractSign 보호 라우트)
  - 참조 패턴: TermsAgreeView(이탈=로그아웃 confirm), TbmExitSignSheet(서명 캔버스 이식 — UI 캡처 로직)
  - planner 라운드 스코프: template + style + 서명 캔버스 UI 로직(드로잉/지우기/파일변환) + 스크롤 끝 도달 감지(UI 토글).
  - developer 라운드 스코프(TODO):
      (1) GET /appApi/dailycontract01/contract-image — 계약서 이미지 로드(활성 계약서, blob→objectURL)
      (2) POST /appApi/dailycontract01/sign — multipart(signFile) 제출, 성공 시 routeAfterContractSign(router, redirect)
      (3) 닫기/이탈 거부 시 forceLogout + userStore.logout (TermsAgreeView fnCancel/onBeforeRouteLeave 미러)
      (4) /DailyContractSign 보호 라우트 등록 + termsGate 체인 삽입(routeAfterRequiredTerms 내부만)
-->
<template>
  <div class="contract-sign-view">
    <!-- 헤더: 뒤로/닫기 = 서명 거부 → confirm 후 로그아웃 (필수약관 게이트 미러) -->
    <header class="cs-hd">
      <button type="button" class="cs-hd__back" aria-label="닫기" @click="onCancel">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
      <h1 class="cs-hd__title">근로계약서 서명</h1>
      <span class="cs-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 안내 배너: 끝까지 읽어야 서명란 활성 -->
    <p class="cs-guide" role="note">
      계약서를 <strong>끝까지 읽은 후</strong> 하단에 서명해 주세요.
      서명하지 않으면 서비스를 이용할 수 없습니다.
    </p>

    <!-- 본문: 계약서 이미지 스크롤 열람 영역 -->
    <main class="cs-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="cs-state" aria-live="polite">계약서를 불러오는 중...</div>

      <!-- 이미지 로드 실패 -->
      <div v-else-if="loadFailed" class="cs-state cs-state--error">
        <p>계약서를 불러오지 못했습니다.</p>
        <button type="button" class="cs-retry" @click="onRetry">다시 시도</button>
      </div>

      <template v-else>
        <!-- 계약서 열람 스크롤 컨테이너: 끝 도달 시 서명란 활성 -->
        <div ref="scrollBoxRef" class="cs-doc" @scroll.passive="onDocScroll">
          <img
            class="cs-doc__img"
            :src="contractImageUrl"
            alt="근로계약서"
            @load="onImageLoaded"
          />

          <!-- 계약 정보(자동 생성) 미리보기 블록 — 실제 합성은 서버(§4-3). 화면은 안내용 표기만 -->
          <section class="cs-auto" aria-label="계약 정보(시스템 자동 생성)">
            <h2 class="cs-auto__title">▣ 계약 정보 (시스템 자동 생성)</h2>
            <dl class="cs-auto__rows">
              <div class="cs-auto__row">
                <dt>성명</dt>
                <dd>{{ userNm }}</dd>
              </div>
              <div class="cs-auto__row">
                <dt>최초 근로일</dt>
                <dd>{{ firstWorkDateText }} (서명일)</dd>
              </div>
              <div class="cs-auto__row">
                <dt>계약 단위</dt>
                <dd>근로일 당일 1일</dd>
              </div>
              <div class="cs-auto__row">
                <dt>서명일시</dt>
                <dd>서명 완료 시 서버 시각으로 기록됩니다</dd>
              </div>
            </dl>
          </section>
        </div>

        <!-- 서명 영역: 스크롤 끝 도달 전 잠금 -->
        <section class="cs-sign" :class="{ 'is-locked': !readToEnd }">
          <div class="cs-sign__head">
            <p class="cs-sign__label">
              서명<span class="cs-req" aria-hidden="true">*</span>
              <span v-if="!readToEnd" class="cs-sign__lock-hint">— 계약서를 끝까지 읽으면 활성화됩니다</span>
            </p>
            <button
              type="button"
              class="cs-sign__clear"
              :disabled="!readToEnd || !hasSignature"
              @click="clearSignature"
            >
              지우기
            </button>
          </div>
          <canvas
            ref="signCanvasRef"
            class="cs-sign__pad"
            aria-label="서명 영역"
            @pointerdown="onSignPointerDown"
            @pointermove="onSignPointerMove"
            @pointerup="onSignPointerUp"
            @pointerleave="onSignPointerUp"
            @pointercancel="onSignPointerUp"
          ></canvas>
        </section>

        <p v-if="errorMsg" class="cs-error" role="alert">{{ errorMsg }}</p>
      </template>
    </main>

    <!-- 하단 제출 -->
    <footer class="cs-ft">
      <button
        type="button"
        class="cs-ft__btn"
        :disabled="!canSubmit || isSubmitting"
        @click="onSubmit"
      >
        {{ isSubmitting ? '제출 중...' : '서명 완료' }}
      </button>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, getCurrentInstance } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import api from '@/api/axios'
import { forceLogout } from '@/composables/useAuth'
import { useUserStore } from '@/stores/userStore'
// 서명 완료 후 게이트 체인 ②(제3자 동의)부터 재개 — routeAfterRequiredTerms 를 부르면 ①-b 재조회(재진입 위험).
import { routeAfterContractSign } from '@/utils/termsGate'

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: confirm 폴백(TermsAgreeView 패턴 동일).
const showConfirm = (message) => {
  if (proxy?.$confirm) return proxy.$confirm(message)
  return Promise.resolve(window.confirm(message))
}

// ── 상태 ────────────────────────────────────────────────────────────
const isLoading = ref(true)
const loadFailed = ref(false)
const isSubmitting = ref(false)
const errorMsg = ref('')

// 계약서 이미지 objectURL — developer 가 API blob 로드 후 세팅
const contractImageUrl = ref('')

// 스크롤 끝 도달 여부(서명란 잠금 해제 조건)
const readToEnd = ref(false)
const scrollBoxRef = ref(null)

// 표시용 사용자 정보(세션) — 합성의 단일 출처는 서버, 화면은 안내 표기만
const userNm = ref(sessionStorage.getItem('gv_userNm') || '')
// 최초 근로일(=서명일, D1) 표시 — 오늘(YYYY.MM.DD). 실제 기록값은 서버 시각이 단일 출처.
const firstWorkDateText = ref(
  (() => {
    const now = new Date()
    const mm = String(now.getMonth() + 1).padStart(2, '0')
    const dd = String(now.getDate()).padStart(2, '0')
    return `${now.getFullYear()}.${mm}.${dd}`
  })(),
)

// redirect 목적지(history state 로 전달; 없으면 /MainView) — TermsAgreeView 미러.
const redirect = ref('/MainView')

// 라우트 이탈 가드 우회 플래그(서명 완료 / 로그아웃 완료 후 통과) — TermsAgreeView 미러.
let bypassGuard = false

// 서명 캔버스
const signCanvasRef = ref(null)
const hasSignature = ref(false)

const canSubmit = computed(() => readToEnd.value && hasSignature.value)

// ── 계약서 이미지 로드 ───────────────────────────────────────────────
// GET contract-image (blob 스트림 — 인증 헤더는 api 인스턴스가 동봉, 경로 미노출).
const loadContract = async () => {
  isLoading.value = true
  loadFailed.value = false
  try {
    const { data } = await api.get('/appApi/dailycontract01/contract-image', {
      responseType: 'blob',
    })
    // 재시도 시 이전 objectURL 누수 방지.
    if (contractImageUrl.value) URL.revokeObjectURL(contractImageUrl.value)
    contractImageUrl.value = URL.createObjectURL(data)
  } catch (e) {
    console.warn('[DailyContractSign] 계약서 이미지 로드 실패:', e?.message)
    loadFailed.value = true
  } finally {
    isLoading.value = false
  }
}

// ── 라이프사이클 ─────────────────────────────────────────────────────
onMounted(async () => {
  const state = window.history.state || {}
  if (state.redirect) redirect.value = state.redirect

  await loadContract()
})

onUnmounted(() => {
  // objectURL 정리(메모리 누수 방지).
  if (contractImageUrl.value) URL.revokeObjectURL(contractImageUrl.value)
})

// 이미지 로드 완료 후 캔버스 셋업 + 짧은 문서(스크롤 불필요)면 즉시 끝 도달 처리
const onImageLoaded = async () => {
  await nextTick()
  setupSignCanvas()
  checkScrollEnd()
}

const onRetry = () => {
  loadContract()
}

// ── 스크롤 끝 도달 감지(UI 토글 — 허용 범위) ─────────────────────────
const SCROLL_END_TOLERANCE = 24 // px — 끝 판정 여유

const checkScrollEnd = () => {
  const el = scrollBoxRef.value
  if (!el) return
  if (el.scrollHeight - el.scrollTop - el.clientHeight <= SCROLL_END_TOLERANCE) {
    readToEnd.value = true
  }
}

const onDocScroll = () => {
  if (readToEnd.value) return
  checkScrollEnd()
}

// ── 서명 캔버스(pointer 드로잉 — TbmExitSignSheet/TbmEntryView 이식) ──
let signCtx = null
let signDrawing = false

const setupSignCanvas = () => {
  const canvas = signCanvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const dpr = window.devicePixelRatio || 1
  canvas.width = Math.max(1, Math.round(rect.width * dpr))
  canvas.height = Math.max(1, Math.round(rect.height * dpr))
  signCtx = canvas.getContext('2d')
  signCtx.scale(dpr, dpr)
  signCtx.lineWidth = 2
  signCtx.lineCap = 'round'
  signCtx.lineJoin = 'round'
  signCtx.strokeStyle = '#111827' // 서명 잉크(고정 — 서명 이미지 표준 색)
  hasSignature.value = false
}

const pointFromEvent = (e) => {
  const canvas = signCanvasRef.value
  const rect = canvas.getBoundingClientRect()
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}

const onSignPointerDown = (e) => {
  if (!readToEnd.value) return // 잠금 상태 드로잉 차단
  if (!signCtx) setupSignCanvas()
  if (!signCtx) return
  signDrawing = true
  const p = pointFromEvent(e)
  signCtx.beginPath()
  signCtx.moveTo(p.x, p.y)
  try {
    signCanvasRef.value.setPointerCapture(e.pointerId)
  } catch (err) {
    // setPointerCapture 미지원 환경 무시
  }
}

const onSignPointerMove = (e) => {
  if (!signDrawing || !signCtx) return
  const p = pointFromEvent(e)
  signCtx.lineTo(p.x, p.y)
  signCtx.stroke()
  hasSignature.value = true
}

const onSignPointerUp = () => {
  signDrawing = false
}

const clearSignature = () => {
  const canvas = signCanvasRef.value
  if (!canvas || !signCtx) return
  signCtx.save()
  signCtx.setTransform(1, 0, 0, 1, 0, 0)
  signCtx.clearRect(0, 0, canvas.width, canvas.height)
  signCtx.restore()
  hasSignature.value = false
}

const signatureToFile = () =>
  new Promise((resolve) => {
    const canvas = signCanvasRef.value
    if (!canvas) {
      resolve(null)
      return
    }
    canvas.toBlob((blob) => {
      if (!blob) {
        resolve(null)
        return
      }
      resolve(new File([blob], 'daily-contract-signature.png', { type: 'image/png' }))
    }, 'image/png')
  })

// ── 액션 ────────────────────────────────────────────────────────────
const onSubmit = async () => {
  if (!canSubmit.value || isSubmitting.value) return
  const signFile = await signatureToFile()
  if (!signFile) {
    errorMsg.value = '서명 이미지를 생성하지 못했습니다. 다시 시도해 주세요.'
    return
  }

  isSubmitting.value = true
  errorMsg.value = ''
  try {
    // multipart 필드명은 컨트롤러 @RequestParam("file") 과 정합. Content-Type 은 axios 가 자동 설정.
    const formData = new FormData()
    formData.append('file', signFile)

    await api.post('/appApi/dailycontract01/sign', formData)

    // 서명 완료 → 게이트 체인 ②(제3자 동의)에 합류.
    bypassGuard = true
    await routeAfterContractSign(router, redirect.value)
  } catch (e) {
    // 이미 서명됨(멱등 가드)은 성공과 동일하게 체인 진행 — 재로그인 중복 제출 등.
    if (e?.response?.data?.errorCode === 'DAILYCONTRACT_400_002') {
      bypassGuard = true
      await routeAfterContractSign(router, redirect.value)
      return
    }
    errorMsg.value =
      e?.response?.data?.message || '서명 제출에 실패했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isSubmitting.value = false
  }
}

// 서명 거부/취소 → 로그아웃 후 로그인 화면 복귀(미서명 상태로 진입 불가 — 필수약관 게이트 미러).
const fnLogout = async () => {
  await forceLogout()
  try {
    userStore.logout()
  } catch (e) {
    console.warn('[DailyContractSign] userStore logout skip:', e?.message)
  }
}

const CANCEL_CONFIRM_MSG =
  '계약서에 서명하지 않으면 서비스를 이용할 수 없어요.\n로그아웃하고 로그인 화면으로 돌아갈까요?'

const onCancel = async () => {
  const ok = await showConfirm(CANCEL_CONFIRM_MSG)
  if (!ok) return
  await fnLogout()
  bypassGuard = true
  router.replace('/')
}

// 뒤로가기/라우트 이탈 가드 — 서명/로그아웃이 아닌 이탈은 로그아웃 후 허용(TermsAgreeView 미러).
onBeforeRouteLeave(async (to, from, next) => {
  if (bypassGuard) {
    next()
    return
  }
  // 방어: 세션이 이미 없으면(인터셉터 강제 로그아웃 후 리다이렉트) confirm 없이 통과(TermsAgreeView 미러).
  if (!sessionStorage.getItem('token')) {
    next()
    return
  }
  const ok = await showConfirm(CANCEL_CONFIRM_MSG)
  if (ok) {
    await fnLogout()
    bypassGuard = true
    next({ path: '/' })
  } else {
    next(false)
  }
})
</script>

<style scoped>
.contract-sign-view {
  /* 디자인 토큰 자급(TbmExitSignSheet/MyPageView 세트 미러) — 하드코딩 사용 금지 */
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-danger-text: #b91c1c;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-warning-bg: #fef3c7;
  --color-warning-text: #b45309;
  --radius-md: 10px;
  --radius-lg: 14px;
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
.cs-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.cs-hd__back {
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
.cs-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.cs-hd__spacer {
  width: 32px;
}

/* 안내 배너 */
.cs-guide {
  margin: var(--space-md) var(--space-lg) 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.5;
}

/* 본문 */
.cs-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: var(--space-md) var(--space-lg);
  gap: var(--space-md);
}

.cs-state {
  padding: 48px 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.cs-state--error {
  color: var(--color-danger-text);
}
.cs-retry {
  margin-top: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 14px;
  cursor: pointer;
  font-family: inherit;
}

/* 계약서 열람 스크롤 박스 */
.cs-doc {
  flex: 1;
  min-height: 40vh;
  overflow-y: auto;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
}
.cs-doc__img {
  display: block;
  width: 100%;
  height: auto;
}

/* 계약 정보 자동 블록(미리보기) */
.cs-auto {
  margin-top: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  background: var(--color-bg);
}
.cs-auto__title {
  margin: 0 0 var(--space-sm);
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.cs-auto__rows {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.cs-auto__row {
  display: flex;
  gap: var(--space-sm);
  font-size: 13px;
}
.cs-auto__row dt {
  flex: 0 0 84px;
  color: var(--color-text-secondary);
}
.cs-auto__row dd {
  margin: 0;
  color: var(--color-text-primary);
}

/* 서명 영역 */
.cs-sign {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.cs-sign.is-locked .cs-sign__pad {
  opacity: 0.45;
  pointer-events: none;
}
.cs-sign__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cs-sign__label {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.cs-req {
  color: var(--color-danger);
  margin-left: 2px;
}
.cs-sign__lock-hint {
  margin-left: var(--space-xs);
  font-size: 12px;
  color: var(--color-text-secondary);
}
.cs-sign__clear {
  background: transparent;
  border: 0;
  padding: var(--space-xs) var(--space-sm);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.cs-sign__clear:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.cs-sign__pad {
  display: block;
  width: 100%;
  height: 140px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  touch-action: none;
}

.cs-error {
  margin: 0;
  font-size: 13px;
  color: var(--color-danger-text);
}

/* 하단 제출 */
.cs-ft {
  padding: var(--space-sm) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border-light);
}
.cs-ft__btn {
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.cs-ft__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
