<!--
  SiteOpsContractSignSheet.vue — 현장 처리(관리자 QR 출근) 일용직 근로계약서 현장 서명 시트 (2026-08-30)
  - 진입: AdminSiteOpsView 출근 스캔 응답 result='SIGN_REQUIRED' 시 전체 화면 오버레이로 표시.
  - 핵심 고지(요구 4·5): 이 서명은 관리자 대리 입력이 아니다 — 관리자 휴대폰을 근로자에게 전달해
    근로자 본인이 계약서를 확인하고 직접 서명한다. 고지 단계(step 0)를 반드시 거친다.
  - 소비 EP(관리자 컨텍스트 — 서버가 진입 게이트/사업장/대상 유효성 재검증):
      GET  /appApi/admin/site-ops/contract/meta?targetUserCd=&siteCd=
      GET  /appApi/admin/site-ops/contract/page?targetUserCd=&siteCd=&page=N (blob)
      POST /appApi/admin/site-ops/contract/sign (multipart file+targetUserCd+siteCd)
  - pager(전 페이지 방문 강제)·서명 캔버스는 DailyContractSignView.vue(근로자 앱 서명)에서 이식.
  - 서명 성공 또는 DAILYCONTRACT_400_002(이미 서명 — 멱등)면 'signed' emit → 부모가 출근 재요청.
-->
<template>
  <div class="sos-sheet" role="dialog" aria-modal="true" aria-label="근로계약서 현장 서명">
    <!-- 헤더 -->
    <header class="sos-hd">
      <button type="button" class="sos-hd__back" aria-label="닫기" @click="onCancel">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
      <h1 class="sos-hd__title">근로계약서 서명</h1>
      <span class="sos-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- ─────────────── 0단계: 본인 서명 고지 ─────────────── -->
    <template v-if="step === 'notice'">
      <main class="sos-body sos-body--notice">
        <div class="sos-notice-icon" aria-hidden="true">✍️</div>
        <h2 class="sos-notice-title">
          {{ userNmMasked }} 님의 근로계약서 서명이 필요해요
        </h2>
        <p class="sos-notice-desc">
          <template v-if="contractNm">
            서명 대상: <strong>{{ contractNm }}</strong><br />
          </template>
          서명해야 출근 처리가 완료됩니다.
        </p>
        <!-- ★핵심 고지(요구 4·5): 관리자 대리 서명이 아님을 명확히 안내 -->
        <div class="sos-notice-warn" role="note">
          <strong>이 서명은 관리자가 대신할 수 없습니다.</strong><br />
          지금부터 이 휴대폰을 <strong>{{ userNmMasked }} 님(근로자 본인)</strong>에게
          전달해, 근로자가 계약서 내용을 직접 확인하고 <strong>본인이 서명</strong>하도록
          해주세요.
        </div>
      </main>
      <footer class="sos-ft sos-ft--col">
        <button type="button" class="sos-btn sos-btn--primary" @click="startReading">
          근로자에게 전달했어요 — 계약서 확인 시작
        </button>
        <button type="button" class="sos-btn sos-btn--ghost" @click="onCancel">
          취소 (출근 처리 안 함)
        </button>
      </footer>
    </template>

    <!-- ─────────────── 1단계: 계약서 열람(pager) ─────────────── -->
    <template v-else-if="step === 'read'">
      <!-- 본인 확인 리마인드 배너 -->
      <p class="sos-guide" role="note">
        <strong>{{ userNmMasked }} 님 본인</strong>이 모든 페이지를 확인한 후 서명해
        주세요.
      </p>

      <nav v-if="pageCount > 0" class="sos-pager" aria-label="계약서 페이지 이동">
        <ul class="sos-pager__dots">
          <li v-for="p in pageCount" :key="p">
            <button
              type="button"
              class="sos-pager__dot"
              :class="{ 'is-current': p === currentPage, 'is-visited': isVisited(p) }"
              :aria-label="`${p}페이지로 이동`"
              @click="goPage(p)"
            ></button>
          </li>
        </ul>
        <span class="sos-pager__count">{{ currentPage }} / {{ pageCount }}</span>
      </nav>

      <main class="sos-body">
        <div v-if="isLoading" class="sos-state" aria-live="polite">계약서를 불러오는 중...</div>
        <div v-else-if="loadFailed" class="sos-state sos-state--error">
          <p>계약서를 불러오지 못했습니다.</p>
          <button type="button" class="sos-retry" @click="loadMeta">다시 시도</button>
        </div>
        <template v-else>
          <div class="sos-page">
            <div v-if="isPageLoading" class="sos-state" aria-live="polite">페이지를 불러오는 중...</div>
            <div v-else-if="pageFailed" class="sos-state sos-state--error">
              <p>{{ currentPage }}페이지를 불러오지 못했습니다.</p>
              <button type="button" class="sos-retry" @click="onPageRetry">다시 시도</button>
            </div>
            <img
              v-else
              class="sos-page__img"
              :src="currentPageUrl"
              :alt="`근로계약서 ${currentPage}페이지`"
            />
          </div>
          <p v-if="!allPagesVisited" class="sos-hint">
            아직 확인하지 않은 페이지가 있습니다. 모든 페이지를 확인해야 서명할 수 있습니다.
          </p>
        </template>
        <p v-if="errorMsg" class="sos-error" role="alert">{{ errorMsg }}</p>
      </main>

      <footer class="sos-ft sos-ft--nav">
        <button type="button" class="sos-btn" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">
          ← 이전
        </button>
        <button
          type="button"
          class="sos-btn sos-btn--primary"
          :disabled="isLastPage && !allPagesVisited"
          @click="goNext"
        >
          {{ isLastPage ? '서명하기' : '다음 →' }}
        </button>
      </footer>
    </template>

    <!-- ─────────────── 2단계: 서명 ─────────────── -->
    <template v-else>
      <main class="sos-body">
        <!-- 본인 서명 리마인드(요구 5 — 서명 단계에서도 재고지) -->
        <div class="sos-notice-warn sos-notice-warn--compact" role="note">
          <strong>{{ userNmMasked }} 님 본인</strong>이 직접 서명해 주세요. 관리자가
          대신 서명할 수 없습니다.
        </div>

        <!-- 계약 정보(자동 생성) — 실제 합성은 서버(근로자 앱 서명과 동일 문안) -->
        <section class="sos-auto" aria-label="계약 정보(시스템 자동 생성)">
          <h2 class="sos-auto__title">▣ 계약 정보 (시스템 자동 생성)</h2>
          <dl class="sos-auto__rows">
            <div class="sos-auto__row">
              <dt>성명</dt>
              <dd>{{ userNmMasked }}</dd>
            </div>
            <div class="sos-auto__row">
              <dt>최초 근로일</dt>
              <dd>{{ firstWorkDateText }} (서명일)</dd>
            </div>
            <div class="sos-auto__row">
              <dt>계약 단위</dt>
              <dd>근로일 당일 1일</dd>
            </div>
            <div class="sos-auto__row">
              <dt>서명일시</dt>
              <dd>서명 완료 시 서버 시각으로 기록됩니다</dd>
            </div>
          </dl>
        </section>

        <!-- 서명 캔버스 (DailyContractSignView 이식) -->
        <section class="sos-sign">
          <div class="sos-sign__head">
            <p class="sos-sign__label">근로자 본인 서명<span class="sos-req" aria-hidden="true">*</span></p>
            <button type="button" class="sos-sign__clear" :disabled="!hasSignature" @click="clearSignature">
              지우기
            </button>
          </div>
          <canvas
            ref="signCanvasRef"
            class="sos-sign__pad"
            aria-label="서명 영역"
            @pointerdown="onSignPointerDown"
            @pointermove="onSignPointerMove"
            @pointerup="onSignPointerUp"
            @pointerleave="onSignPointerUp"
            @pointercancel="onSignPointerUp"
          ></canvas>
        </section>

        <p v-if="errorMsg" class="sos-error" role="alert">{{ errorMsg }}</p>
      </main>

      <footer class="sos-ft sos-ft--col">
        <button type="button" class="sos-link" @click="backToPages">계약서 다시 보기</button>
        <button
          type="button"
          class="sos-btn sos-btn--primary"
          :disabled="!canSubmit || isSubmitting"
          @click="onSubmit"
        >
          {{ isSubmitting ? '제출 중...' : '서명 완료' }}
        </button>
      </footer>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, getCurrentInstance } from 'vue'
import api from '@/api/axios'

const props = defineProps({
  // SIGN_REQUIRED 응답의 대상 일용직 USER_CD(리소스 키 — 인가는 서버가 매 호출 재검증)
  targetUserCd: { type: String, required: true },
  // 마스킹 이름(서버 응답 그대로 — 평문 PII 미보유)
  userNmMasked: { type: String, default: '' },
  // 서명 대상 계약서명(안내 표기)
  contractNm: { type: String, default: '' },
  // 현장전환 사업장(출퇴근 EP 와 동일 값 전달)
  siteCd: { type: String, default: '' },
})

const emit = defineEmits(['signed', 'cancel'])

const { proxy } = getCurrentInstance() || { proxy: null }
const showConfirm = (message) => {
  if (proxy?.$confirm) return proxy.$confirm(message)
  return Promise.resolve(window.confirm(message))
}

// ── 단계: notice(본인 서명 고지) → read(열람) → sign(서명) ─────────────
const step = ref('notice')

// ── 열람 pager 상태 (DailyContractSignView 이식 — 전 페이지 방문 강제) ──
const isLoading = ref(true)
const loadFailed = ref(false)
const errorMsg = ref('')
const pageCount = ref(0)
const currentPage = ref(1)
const visitedPages = ref([])
const pageUrls = ref({})
const isPageLoading = ref(false)
const pageFailed = ref(false)
const inFlightPages = new Set()

const currentPageUrl = computed(() => pageUrls.value[currentPage.value] || '')
const isLastPage = computed(() => pageCount.value > 0 && currentPage.value >= pageCount.value)
const allPagesVisited = computed(
  () => pageCount.value > 0 && visitedPages.value.length >= pageCount.value,
)

// 공통 쿼리 파라미터(대상 리소스 키 + 현장전환 사업장)
const targetParams = () => ({ targetUserCd: props.targetUserCd, siteCd: props.siteCd })

const revokeAllPageUrls = () => {
  Object.values(pageUrls.value).forEach((url) => {
    if (url) URL.revokeObjectURL(url)
  })
  pageUrls.value = {}
}

const fetchPage = async (page) => {
  if (page < 1 || page > pageCount.value) return
  if (inFlightPages.has(page)) return
  inFlightPages.add(page)

  const isCurrent = () => page === currentPage.value
  if (isCurrent()) {
    isPageLoading.value = true
    pageFailed.value = false
  }
  try {
    const { data } = await api.get('/appApi/admin/site-ops/contract/page', {
      params: { ...targetParams(), page },
      responseType: 'blob',
    })
    const prevUrl = pageUrls.value[page]
    pageUrls.value[page] = URL.createObjectURL(data)
    if (prevUrl) URL.revokeObjectURL(prevUrl)
    if (isCurrent()) pageFailed.value = false
  } catch (e) {
    console.warn('[SiteOpsContractSign] 페이지 로드 실패:', page, e?.message)
    if (isCurrent()) pageFailed.value = true
  } finally {
    inFlightPages.delete(page)
    if (isCurrent()) isPageLoading.value = false
  }
}

const isVisited = (page) => visitedPages.value.includes(page)
const markVisited = (page) => {
  if (page >= 1 && page <= pageCount.value && !visitedPages.value.includes(page)) {
    visitedPages.value.push(page)
  }
}

const goPage = (page) => {
  if (page < 1 || page > pageCount.value) return
  currentPage.value = page
  markVisited(page)
  pageFailed.value = false
  isPageLoading.value = !pageUrls.value[page]
  if (!pageUrls.value[page]) fetchPage(page)
  // 인접 페이지 프리페치(실패는 조용히 — 진입 시 재시도)
  ;[page + 1, page - 1].forEach((p) => {
    if (p >= 1 && p <= pageCount.value && !pageUrls.value[p]) fetchPage(p)
  })
}

const goNext = () => {
  if (isLastPage.value) {
    enterSignStep()
    return
  }
  goPage(currentPage.value + 1)
}

const onPageRetry = () => {
  pageFailed.value = false
  isPageLoading.value = true
  fetchPage(currentPage.value)
}

const loadMeta = async () => {
  isLoading.value = true
  loadFailed.value = false
  errorMsg.value = ''
  try {
    const { data } = await api.get('/appApi/admin/site-ops/contract/meta', {
      params: targetParams(),
    })
    const count = Number(data?.pageCount)
    if (!Number.isFinite(count) || count < 1) throw new Error('invalid pageCount')

    revokeAllPageUrls()
    visitedPages.value = []
    pageCount.value = count
    currentPage.value = 1
    isLoading.value = false
    goPage(1)
  } catch (e) {
    console.warn('[SiteOpsContractSign] 메타 조회 실패:', e?.message)
    loadFailed.value = true
    errorMsg.value = e?.response?.data?.message || ''
    isLoading.value = false
  }
}

const startReading = () => {
  step.value = 'read'
  loadMeta()
}

// ── 서명 단계 (캔버스 — DailyContractSignView 이식) ──────────────────
const isSubmitting = ref(false)
const signCanvasRef = ref(null)
const hasSignature = ref(false)
let signCtx = null
let signDrawing = false

const canSubmit = computed(() => allPagesVisited.value && hasSignature.value)

const firstWorkDateText = ref(
  (() => {
    const now = new Date()
    const mm = String(now.getMonth() + 1).padStart(2, '0')
    const dd = String(now.getDate()).padStart(2, '0')
    return `${now.getFullYear()}.${mm}.${dd}`
  })(),
)

const enterSignStep = async () => {
  if (!allPagesVisited.value) return
  step.value = 'sign'
  await nextTick()
  setupSignCanvas()
}

const backToPages = () => {
  step.value = 'read'
}

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
  const rect = signCanvasRef.value.getBoundingClientRect()
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}

const onSignPointerDown = (e) => {
  if (!signCtx) setupSignCanvas()
  if (!signCtx) return
  signDrawing = true
  const p = pointFromEvent(e)
  signCtx.beginPath()
  signCtx.moveTo(p.x, p.y)
  try {
    signCanvasRef.value.setPointerCapture(e.pointerId)
  } catch {
    /* setPointerCapture 미지원 환경 무시 */
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
      resolve(new File([blob], 'siteops-contract-signature.png', { type: 'image/png' }))
    }, 'image/png')
  })

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
    const formData = new FormData()
    formData.append('file', signFile)
    formData.append('targetUserCd', props.targetUserCd)
    formData.append('siteCd', props.siteCd)

    await api.post('/appApi/admin/site-ops/contract/sign', formData)
    emit('signed')
  } catch (e) {
    // 이미 서명됨(멱등 가드) — 성공과 동일 취급(부모가 출근 재요청).
    if (e?.response?.data?.errorCode === 'DAILYCONTRACT_400_002') {
      emit('signed')
      return
    }
    errorMsg.value =
      e?.response?.data?.message || '서명 제출에 실패했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isSubmitting.value = false
  }
}

// ── 취소(어느 단계든): 서명 없이는 출근이 등록되지 않음을 확인 ──────────
const onCancel = async () => {
  const ok = await showConfirm(
    '서명하지 않으면 출근이 처리되지 않아요.\n서명을 취소할까요?',
  )
  if (!ok) return
  emit('cancel')
}

onMounted(() => {
  // notice 단계에서 시작 — 계약서 로드는 근로자에게 전달 후(startReading) 수행.
})

onUnmounted(() => {
  revokeAllPageUrls()
})
</script>

<style scoped>
.sos-sheet {
  /* 앱 FE 규약: 디자인 토큰은 화면 루트마다 선언(:root 없음) — DailyContractSignView 세트 동일 */
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

  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
}

/* 헤더 */
.sos-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) var(--space-lg);
  padding-top: calc(var(--space-md) + env(safe-area-inset-top, 0px));
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.sos-hd__back {
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
.sos-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.sos-hd__spacer {
  width: 32px;
}

/* 본문 */
.sos-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
  padding: var(--space-md) var(--space-lg);
  gap: var(--space-md);
}
.sos-body--notice {
  justify-content: center;
  align-items: center;
  text-align: center;
  gap: var(--space-lg);
}

/* 고지 단계 */
.sos-notice-icon {
  font-size: 44px;
  line-height: 1;
}
.sos-notice-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  word-break: keep-all;
}
.sos-notice-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-secondary);
  word-break: keep-all;
}
.sos-notice-warn {
  padding: var(--space-md);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.6;
  text-align: left;
  word-break: keep-all;
}
.sos-notice-warn--compact {
  font-size: 13px;
  padding: var(--space-sm) var(--space-md);
}

/* 열람 배너/페이저 */
.sos-guide {
  margin: var(--space-md) var(--space-lg) 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.5;
  word-break: keep-all;
}
.sos-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg) 0;
}
.sos-pager__dots {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin: 0;
  padding: 0;
  list-style: none;
  flex-wrap: wrap;
  justify-content: center;
}
.sos-pager__dot {
  width: 10px;
  height: 10px;
  padding: 0;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  background: var(--color-surface);
  cursor: pointer;
}
.sos-pager__dot.is-visited {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.sos-pager__dot.is-current {
  box-shadow: 0 0 0 3px var(--color-border-light);
}
.sos-pager__count {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

/* 페이지 뷰포트 */
.sos-page {
  flex: 1;
  min-height: 40vh;
  overflow: auto;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
}
.sos-page__img {
  display: block;
  width: 100%;
  height: auto;
  user-select: none;
  -webkit-user-drag: none;
}

.sos-state {
  padding: 48px 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.sos-state--error {
  color: var(--color-danger-text);
}
.sos-retry {
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
.sos-hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-warning-text);
  word-break: keep-all;
}
.sos-error {
  margin: 0;
  font-size: 13px;
  color: var(--color-danger-text);
}

/* 계약 정보 자동 블록 */
.sos-auto {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  background: var(--color-surface);
}
.sos-auto__title {
  margin: 0 0 var(--space-sm);
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.sos-auto__rows {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.sos-auto__row {
  display: flex;
  gap: var(--space-sm);
  font-size: 13px;
}
.sos-auto__row dt {
  flex: 0 0 84px;
  color: var(--color-text-secondary);
}
.sos-auto__row dd {
  margin: 0;
  color: var(--color-text-primary);
}

/* 서명 영역 */
.sos-sign {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.sos-sign__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sos-sign__label {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.sos-req {
  color: var(--color-danger);
  margin-left: 2px;
}
.sos-sign__clear {
  background: transparent;
  border: 0;
  padding: var(--space-xs) var(--space-sm);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.sos-sign__clear:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.sos-sign__pad {
  display: block;
  width: 100%;
  height: 140px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  touch-action: none;
}

/* 하단 */
.sos-ft {
  padding: var(--space-sm) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border-light);
}
.sos-ft--nav {
  display: flex;
  gap: var(--space-sm);
}
.sos-ft--col {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.sos-btn {
  flex: 1;
  height: 48px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  word-break: keep-all;
}
.sos-btn--primary {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
  font-weight: 700;
}
.sos-btn--ghost {
  border-color: transparent;
  background: transparent;
  color: var(--color-text-secondary);
}
.sos-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.sos-link {
  display: block;
  width: 100%;
  padding: var(--space-xs) 0;
  background: transparent;
  border: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
</style>
