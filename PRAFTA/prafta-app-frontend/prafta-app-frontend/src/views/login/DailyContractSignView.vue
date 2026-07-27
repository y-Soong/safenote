<!--
  DailyContractSignView.vue — 일용직 근로계약서 서명 게이트 (앱) / 멀티페이지 pager 개편
  - 분해: .claude/requests/common/작업지시서_계약서-멀티페이지-PDF지원.plan.md §5 T6 / §8
  - 요청서 근거: §6-1(pager 레이아웃), P4(전 페이지 방문 강제), P6(150DPI 렌더)
  - 정책서: safety/09-daily-worker.md §9.3 (계약 단위 문안·서명=승인 사이클당 1회)
  - 진입: termsGate.routeAfterRequiredTerms 내부 게이트 체인 ①-b (일용직 전용, /DailyContractSign 보호 라우트)
  - 소비 EP: GET /appApi/dailycontract01/contract-meta, GET /appApi/dailycontract01/contract-page?page=N,
             POST /appApi/dailycontract01/sign
  - 보존(검증 완료 로직 — 삭제·재작성 금지): 서명 캔버스 일체, onBeforeRouteLeave 이탈 가드(=로그아웃 confirm),
             DAILYCONTRACT_400_002 멱등 처리, routeAfterContractSign 합류, objectURL revoke 패턴
-->
<template>
  <div class="contract-sign-view">
    <!-- 헤더: 닫기 = 서명 거부 → confirm 후 로그아웃 (기존 동작 보존) -->
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

    <!-- 안내 배너 -->
    <p class="cs-guide" role="note">
      계약서 <strong>모든 페이지를 확인</strong>한 후 서명해 주세요.
      서명하지 않으면 서비스를 이용할 수 없습니다.
    </p>

    <!-- ─────────────── 열람 단계 ─────────────── -->
    <template v-if="!isSignStep">
      <!-- 페이지 인디케이터: 채운 점 = 방문 완료, 링 강조 = 현재 페이지 -->
      <nav v-if="pageCount > 0" class="cs-pager" aria-label="계약서 페이지 이동">
        <ul class="cs-pager__dots">
          <li v-for="p in pageCount" :key="p">
            <button
              type="button"
              class="cs-pager__dot"
              :class="{ 'is-current': p === currentPage, 'is-visited': isVisited(p) }"
              :aria-label="`${p}페이지로 이동`"
              :aria-current="p === currentPage ? 'page' : undefined"
              @click="goPage(p)"
            ></button>
          </li>
        </ul>
        <span class="cs-pager__count">{{ currentPage }} / {{ pageCount }}</span>
      </nav>

      <main class="cs-body">
        <!-- 문서 메타 로딩 -->
        <div v-if="isLoading" class="cs-state" aria-live="polite">계약서를 불러오는 중...</div>

        <!-- 문서 메타 실패 -->
        <div v-else-if="loadFailed" class="cs-state cs-state--error">
          <p>계약서를 불러오지 못했습니다.</p>
          <button type="button" class="cs-retry" @click="onRetry">다시 시도</button>
        </div>

        <template v-else>
          <!-- 페이지 뷰포트: 확대 시 내부 스크롤. 제스처 분리는 developer -->
          <div
            ref="pageBoxRef"
            class="cs-page"
            :class="{ 'is-zoomed': zoom > 1 }"
            @dblclick="toggleZoom"
            @touchstart.passive="onPageTouchStart"
            @touchmove.passive="onPageTouchMove"
            @touchend="onPageTouchEnd"
            @touchcancel="onPageTouchEnd"
          >
            <div v-if="isPageLoading" class="cs-state" aria-live="polite">페이지를 불러오는 중...</div>
            <div v-else-if="pageFailed" class="cs-state cs-state--error">
              <p>{{ currentPage }}페이지를 불러오지 못했습니다.</p>
              <button type="button" class="cs-retry" @click="onPageRetry">다시 시도</button>
            </div>
            <img
              v-else
              class="cs-page__img"
              :src="currentPageUrl"
              :style="{ transform: `scale(${zoom})` }"
              :alt="`근로계약서 ${currentPage}페이지`"
            />
          </div>

          <!-- 미방문 안내(P4) -->
          <p v-if="!allPagesVisited" class="cs-hint">
            아직 확인하지 않은 페이지가 있습니다. 모든 페이지를 확인해야 서명할 수 있습니다.
          </p>
        </template>

        <p v-if="errorMsg" class="cs-error" role="alert">{{ errorMsg }}</p>
      </main>

      <!-- 하단 네비게이션 -->
      <footer class="cs-ft cs-ft--nav">
        <button
          type="button"
          class="cs-ft__nav"
          :disabled="currentPage <= 1"
          @click="goPrev"
        >
          ← 이전
        </button>
        <button
          type="button"
          class="cs-ft__nav cs-ft__nav--primary"
          :disabled="!canGoForward"
          @click="goNext"
        >
          {{ isLastPage ? '서명하기' : '다음 →' }}
        </button>
      </footer>
    </template>

    <!-- ─────────────── 서명 단계 ─────────────── -->
    <template v-else>
      <main class="cs-body">
        <!-- 계약 정보(자동 생성) 미리보기 — 실제 합성은 서버. 문안 변경 금지(safety §9.3) -->
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

        <!-- 서명 캔버스 (기존 로직 그대로 — 잠금 개념은 pager 게이트로 대체) -->
        <section class="cs-sign">
          <div class="cs-sign__head">
            <p class="cs-sign__label">
              서명<span class="cs-req" aria-hidden="true">*</span>
            </p>
            <button
              type="button"
              class="cs-sign__clear"
              :disabled="!hasSignature"
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
      </main>

      <footer class="cs-ft">
        <button type="button" class="cs-ft__link" @click="backToPages">계약서 다시 보기</button>
        <button
          type="button"
          class="cs-ft__btn"
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
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'
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

// ── EP(계약서 열람은 전부 스트림/메타 EP — 파일 경로 미노출, 인증 헤더는 api 인스턴스가 동봉) ──
const META_URL = '/appApi/dailycontract01/contract-meta'
const PAGE_URL = '/appApi/dailycontract01/contract-page'

// ── 상태 ────────────────────────────────────────────────────────────
const isLoading = ref(true)
const loadFailed = ref(false)
const isSubmitting = ref(false)
const errorMsg = ref('')

// ── pager 상태 ──────────────────────────────────────────────────────
const pageCount = ref(0)
const formatType = ref('') // 'PDF' | 'IMG' (표시에는 쓰지 않고 진단 로그용)
const currentPage = ref(1)
const visitedPages = ref([]) // 방문 완료 페이지 번호 배열(1-base)
const pageUrls = ref({}) // { [page]: objectURL } — 캐시 + revoke 대상
const isPageLoading = ref(false)
const pageFailed = ref(false)
const zoom = ref(1) // 1(기본) ~ ZOOM_MAX. 더블탭 토글 + 핀치 배율
const pageBoxRef = ref(null)

const currentPageUrl = computed(() => pageUrls.value[currentPage.value] || '')
const isLastPage = computed(() => pageCount.value > 0 && currentPage.value >= pageCount.value)
const allPagesVisited = computed(
  () => pageCount.value > 0 && visitedPages.value.length >= pageCount.value,
)
const canGoForward = computed(() => (isLastPage.value ? allPagesVisited.value : true))

// 서명 단계 토글
const isSignStep = ref(false)

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

// 제출 가능 = 전 페이지 방문(P4) && 서명 존재
const canSubmit = computed(() => allPagesVisited.value && hasSignature.value)

// ── 페이지 이미지 로드(objectURL 캐시) ───────────────────────────────
// 동일 페이지에 대한 중복 요청 방지(현재 페이지 로드 + 프리페치가 겹칠 수 있음).
const inFlightPages = new Set()

/** 페이지 objectURL 전량 해제 — 재조회/언마운트 시 누수 방지. */
const revokeAllPageUrls = () => {
  Object.values(pageUrls.value).forEach((url) => {
    if (url) URL.revokeObjectURL(url)
  })
  pageUrls.value = {}
}

/**
 * 페이지 단건 로드(1-base). 현재 페이지일 때만 로딩/실패 상태를 갱신하고,
 * 프리페치(비현재 페이지)는 화면 상태를 건드리지 않는다 → 페이지별 에러 격리(전체 실패 아님).
 */
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
    const { data } = await api.get(PAGE_URL, {
      params: { page },
      responseType: 'blob',
    })
    // 재로드 시 이전 objectURL 누수 방지(새 URL 로 교체한 뒤 해제).
    const prevUrl = pageUrls.value[page]
    pageUrls.value[page] = URL.createObjectURL(data)
    if (prevUrl) URL.revokeObjectURL(prevUrl)
    if (isCurrent()) pageFailed.value = false
  } catch (e) {
    console.warn('[DailyContractSign] 계약서 페이지 로드 실패:', page, e?.message)
    if (isCurrent()) pageFailed.value = true
  } finally {
    inFlightPages.delete(page)
    if (isCurrent()) isPageLoading.value = false
  }
}

/** 현재 페이지 보장 로드(캐시 히트면 요청 없음). */
const ensurePage = (page) => {
  if (pageUrls.value[page]) return
  fetchPage(page)
}

/** 인접 페이지 프리페치(앞/뒤 1장) — 실패는 조용히 무시하고 진입 시 재시도된다. */
const prefetchNeighbors = (page) => {
  ;[page + 1, page - 1].forEach((p) => {
    if (p >= 1 && p <= pageCount.value && !pageUrls.value[p]) fetchPage(p)
  })
}

// ── pager 조작 ───────────────────────────────────────────────────────
const isVisited = (page) => visitedPages.value.includes(page)

const markVisited = (page) => {
  if (page >= 1 && page <= pageCount.value && !visitedPages.value.includes(page)) {
    visitedPages.value.push(page)
  }
}

const goPage = (page) => {
  if (page < 1 || page > pageCount.value) return
  currentPage.value = page
  zoom.value = 1
  // 확대 상태에서 이동한 경우 스크롤 위치도 원점으로 되돌린다(다음 페이지가 잘려 보이는 것 방지).
  if (pageBoxRef.value) {
    pageBoxRef.value.scrollTop = 0
    pageBoxRef.value.scrollLeft = 0
  }
  markVisited(page)

  // 캐시 히트면 즉시 표시, 아니면 로딩 상태로 진입.
  pageFailed.value = false
  isPageLoading.value = !pageUrls.value[page]

  ensurePage(page)
  prefetchNeighbors(page)
}

const goPrev = () => goPage(currentPage.value - 1)

const goNext = () => {
  if (isLastPage.value) {
    enterSignStep()
    return
  }
  goPage(currentPage.value + 1)
}

const enterSignStep = async () => {
  // P4 게이트 — 미방문 페이지가 있으면 서명 단계 진입 차단(안내는 cs-hint 가 담당)
  if (!allPagesVisited.value) return
  isSignStep.value = true
  await nextTick()
  setupSignCanvas() // 캔버스는 서명 단계에서만 마운트되므로 진입 후 셋업
}

const backToPages = () => {
  isSignStep.value = false
}

// ── 확대 제스처(더블탭 토글 + 핀치) ──────────────────────────────────
// Flutter 셸 웹뷰는 supportZoom=false(네이티브 확대 비활성)라 배율은 화면이 직접 관리한다.
const ZOOM_MAX = 3
const clampZoom = (v) => Math.min(ZOOM_MAX, Math.max(1, v))

const toggleZoom = () => {
  zoom.value = zoom.value > 1 ? 1 : 2
}

// ★스와이프는 배율 1 + 단일 터치일 때만 활성 — 확대 상태의 좌우 패닝이 페이지 넘김으로
//   오발동하면 안 된다(요청서 §6-1). 핀치(2터치) 중에도 즉시 비활성화한다.
const SWIPE_MIN_DISTANCE = 48 // px — 페이지 넘김 최소 수평 이동
const SWIPE_MAX_OFF_AXIS = 40 // px — 이 이상 수직 이동은 스크롤 의도로 간주

let swipeActive = false
let swipeStartX = 0
let swipeStartY = 0
let swipeDx = 0
let swipeDy = 0

let pinchStartDist = 0
let pinchStartZoom = 1

const touchDistance = (touches) => {
  const dx = touches[0].clientX - touches[1].clientX
  const dy = touches[0].clientY - touches[1].clientY
  return Math.sqrt(dx * dx + dy * dy)
}

const onPageTouchStart = (e) => {
  const touches = e.touches || []

  if (touches.length >= 2) {
    // 핀치 시작 — 스와이프 비활성.
    swipeActive = false
    pinchStartDist = touchDistance(touches)
    pinchStartZoom = zoom.value
    return
  }

  pinchStartDist = 0
  if (touches.length === 1 && zoom.value === 1) {
    swipeActive = true
    swipeStartX = touches[0].clientX
    swipeStartY = touches[0].clientY
    swipeDx = 0
    swipeDy = 0
  } else {
    swipeActive = false
  }
}

const onPageTouchMove = (e) => {
  const touches = e.touches || []

  if (touches.length >= 2) {
    // 핀치 확대/축소 — 도중 스와이프 판정은 취소.
    swipeActive = false
    if (pinchStartDist > 0) {
      zoom.value = clampZoom((pinchStartZoom * touchDistance(touches)) / pinchStartDist)
    }
    return
  }

  if (!swipeActive) return
  if (zoom.value !== 1) {
    swipeActive = false
    return
  }
  swipeDx = touches[0].clientX - swipeStartX
  swipeDy = touches[0].clientY - swipeStartY
}

const onPageTouchEnd = () => {
  pinchStartDist = 0
  if (!swipeActive) return
  swipeActive = false

  if (Math.abs(swipeDx) < SWIPE_MIN_DISTANCE) return
  if (Math.abs(swipeDy) > SWIPE_MAX_OFF_AXIS) return

  if (swipeDx < 0) {
    // 좌 스와이프 = 다음 페이지. 마지막 페이지에서는 서명 단계로 넘기지 않는다(버튼 전용).
    if (!isLastPage.value) goPage(currentPage.value + 1)
  } else {
    goPage(currentPage.value - 1)
  }
}

// ── 계약서 메타 로드 ────────────────────────────────────────────────
// GET contract-meta → pageCount/formatType. 성공 시 1페이지 표시(1페이지 문서도 동일 경로).
const loadMeta = async () => {
  isLoading.value = true
  loadFailed.value = false
  errorMsg.value = ''
  try {
    const { data } = await api.get(META_URL)

    const count = Number(data?.pageCount)
    if (!Number.isFinite(count) || count < 1) {
      // 페이지 수를 알 수 없으면 pager 를 구성할 수 없다 → 재시도 유도.
      throw new Error('invalid pageCount')
    }

    // 재조회(다시 시도) 시 이전 페이지 캐시/방문 이력 초기화.
    revokeAllPageUrls()
    visitedPages.value = []
    pageCount.value = count
    formatType.value = data?.formatType || ''
    currentPage.value = 1
    zoom.value = 1

    isLoading.value = false
    goPage(1)
  } catch (e) {
    console.warn('[DailyContractSign] 계약서 메타 조회 실패:', e?.message)
    loadFailed.value = true
    // 서버가 사유(미등록/원본 결손 등)를 내려주면 그대로 노출한다.
    const serverMsg = e?.response ? resolveApiErrorMessage(e, '') : ''
    errorMsg.value = serverMsg || ''
    isLoading.value = false
  }
}

// ── 라이프사이클 ─────────────────────────────────────────────────────
onMounted(async () => {
  const state = window.history.state || {}
  if (state.redirect) redirect.value = state.redirect

  await loadMeta()
})

onUnmounted(() => {
  // 페이지 objectURL 전량 정리(메모리 누수 방지).
  revokeAllPageUrls()
})

const onRetry = () => {
  loadMeta()
}

const onPageRetry = () => {
  pageFailed.value = false
  isPageLoading.value = true
  fetchPage(currentPage.value)
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
  /* 디자인 토큰 자급(기존 세트 그대로 — 신규 하드코딩 금지) */
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

/* 페이지 인디케이터 */
.cs-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg) 0;
}
.cs-pager__dots {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin: 0;
  padding: 0;
  list-style: none;
  flex-wrap: wrap;
  justify-content: center;
}
.cs-pager__dot {
  width: 10px;
  height: 10px;
  padding: 0;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  background: var(--color-surface);
  cursor: pointer;
}
.cs-pager__dot.is-visited {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.cs-pager__dot.is-current {
  box-shadow: 0 0 0 3px var(--color-border-light);
}
.cs-pager__count {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
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

/* 페이지 뷰포트 — 확대 시 내부 스크롤. 가로 스와이프/핀치는 developer 가 제스처 분리 */
.cs-page {
  flex: 1;
  min-height: 40vh;
  overflow: auto;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  touch-action: pan-x pan-y pinch-zoom;
  overscroll-behavior: contain;
}
.cs-page.is-zoomed {
  touch-action: pan-x pan-y;
}
.cs-page__img {
  display: block;
  width: 100%;
  height: auto;
  transform-origin: top left;
  user-select: none;
  -webkit-user-drag: none;
}

/* 미방문 안내 */
.cs-hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-warning-text);
}

/* 계약 정보 자동 블록 */
.cs-auto {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  background: var(--color-surface);
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

/* 하단 */
.cs-ft {
  padding: var(--space-sm) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border-light);
}
.cs-ft--nav {
  display: flex;
  gap: var(--space-sm);
}
.cs-ft__nav {
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
}
.cs-ft__nav--primary {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
  font-weight: 700;
}
.cs-ft__nav:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.cs-ft__link {
  display: block;
  width: 100%;
  margin-bottom: var(--space-sm);
  padding: var(--space-xs) 0;
  background: transparent;
  border: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
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
