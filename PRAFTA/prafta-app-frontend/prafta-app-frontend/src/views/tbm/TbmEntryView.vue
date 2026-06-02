<!--
  TbmEntryView.vue — TBM 입실/종료 (모바일 앱)
  - 작업 ID: APP004-C4 (분해: .claude/requests/app_requests/prafta-app-004-plan.md)
  - UI 명세: .claude/requests/app_requests/prafta-app-004-ui-spec.md (UI-A0xx)
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격만.
  - developer 라운드 스코프(아래 TODO): API 호출(C1/C2/C3), GPS 검증, QR 연계, 서명(조건부), 라우팅.
  - 디자인 토큰: MyAttendanceView(.my-attd-view)와 동일 세트를 .tbm-entry-view 루트에 1회 선언.
    자식 컴포넌트(scoped)는 var(--...) 상속. 하드코딩 색상/픽셀 금지.
  - ⚠️ 정규직(REGULAR) MVP 전용. 일용직(DAILY)은 본 화면 범위 밖(C-D2).
  - ⚠️ 서명 영역 노출/GPS 좌표 노출 방식은 C-D1/C-D5 확정 후 보완.
-->
<template>
  <div class="tbm-entry-view">
    <!-- 헤더 -->
    <header class="tbm-hd">
      <button type="button" class="tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-tbm-chev-left" />
        </svg>
      </button>
      <h1 class="tbm-hd__title">TBM 입실</h1>
      <span class="tbm-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 본문 -->
    <main class="tbm-body">
      <!-- 로딩 -->
      <p v-if="isLoading" class="tbm-loading">불러오는 중…</p>

      <template v-else>
        <!-- 세션 정보 카드 -->
        <section class="card">
          <div class="card__head">
            <p class="card__title">{{ session.title || 'TBM 세션' }}</p>
            <span class="badge" :class="badgeToneClass">{{ statusLabel }}</span>
          </div>
          <p v-if="sessionMetaText" class="card__meta">{{ sessionMetaText }}</p>
        </section>

        <!-- GPS 콜아웃 (DISABLED 면 숨김 — C-D5) -->
        <div
          v-if="showGpsCallout"
          class="callout"
          :class="gpsToneClass"
        >
          <svg class="icon" width="16" height="16" aria-hidden="true">
            <use :href="`#${gpsIconId}`" />
          </svg>
          <span>{{ gpsCalloutText }}</span>
        </div>

        <!-- 입실 불가 안내 -->
        <p v-if="!isEnterable && !isEntered" class="tbm-unavailable">
          현재 입실할 수 없는 세션입니다
        </p>

        <!-- 입실 단계 (미입실) -->
        <section v-if="isEnterable && !isEntered" class="step">
          <label class="field">
            <span class="field__label">입실 비밀번호</span>
            <input
              v-model="entryPwd"
              class="field__input"
              type="text"
              inputmode="numeric"
              maxlength="6"
              autocomplete="off"
              placeholder="6자리 숫자"
            />
          </label>

          <div class="divider"><span>또는</span></div>

          <button type="button" class="btn btn--ghost" @click="onScanQrToEnter">
            <svg class="icon" width="18" height="18" aria-hidden="true">
              <use href="#i-tbm-qr" />
            </svg>
            QR 스캔으로 입실
          </button>

          <!-- 서명 영역 (C-D1 "서명 필수" 확정 시 노출/구현 — C6) -->
          <div v-if="requireEntrySign" class="sign-box">
            <p class="sign-box__hint">입실 서명</p>
            <!-- TODO(developer): 서명 캡처(canvas 또는 Flutter 네이티브) + 파일 업로드 -->
            <div class="sign-box__pad" aria-label="서명 영역"></div>
          </div>

          <p v-if="entryError" class="form-error">{{ entryError }}</p>

          <button
            type="button"
            class="btn btn--primary"
            :disabled="!canSubmitEnter"
            @click="onSubmitEnter"
          >
            입실하기
          </button>
        </section>

        <!-- 종료 단계 (입실 완료) -->
        <section v-else-if="isEntered" class="step">
          <p class="entered-info">{{ enteredInfoText }}</p>

          <label v-if="exitPwdRequired" class="field">
            <span class="field__label">종료 비밀번호</span>
            <input
              v-model="exitPwd"
              class="field__input"
              type="text"
              inputmode="numeric"
              maxlength="6"
              autocomplete="off"
              placeholder="6자리 숫자"
            />
          </label>

          <button type="button" class="btn btn--ghost" @click="onScanQrToExit">
            <svg class="icon" width="18" height="18" aria-hidden="true">
              <use href="#i-tbm-qr" />
            </svg>
            QR 스캔으로 종료
          </button>

          <div v-if="requireExitSign" class="sign-box">
            <div class="sign-box__head">
              <p class="sign-box__hint">종료 서명 (필수)</p>
              <button type="button" class="sign-box__clear" @click="clearSignature">지우기</button>
            </div>
            <canvas
              ref="signCanvasRef"
              class="sign-box__pad"
              aria-label="서명 영역"
              @pointerdown="onSignPointerDown"
              @pointermove="onSignPointerMove"
              @pointerup="onSignPointerUp"
              @pointerleave="onSignPointerUp"
              @pointercancel="onSignPointerUp"
            ></canvas>
          </div>

          <p v-if="exitError" class="form-error">{{ exitError }}</p>

          <button
            type="button"
            class="btn btn--primary"
            :disabled="!canSubmitExit"
            @click="onSubmitExit"
          >
            종료하기
          </button>
        </section>

        <!-- 이수 완료 -->
        <p v-if="isCompleted" class="tbm-done">이수 완료</p>
      </template>
    </main>

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-tbm-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6" /></symbol>
        <symbol id="i-tbm-qr" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><line x1="14" y1="14" x2="14" y2="21" /><line x1="18" y1="14" x2="21" y2="14" /><line x1="21" y1="18" x2="21" y2="21" /></symbol>
        <symbol id="i-tbm-loc-ok" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 21s-7-6.5-7-11a7 7 0 0 1 14 0c0 4.5-7 11-7 11z" /><polyline points="9 10 11 12 15 8" /></symbol>
        <symbol id="i-tbm-loc-warn" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 21s-7-6.5-7-11a7 7 0 0 1 14 0c0 4.5-7 11-7 11z" /><line x1="12" y1="7" x2="12" y2="11" /><line x1="12" y1="14" x2="12" y2="14" /></symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import { requestGps } from '@/utils/gpsBridge'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선, 없으면 window.alert) — MyAttendanceView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 반응형 상태 (developer: 초기값/리셋 보완) ──────────────────────
const isLoading = ref(false)

// 입실 컨텍스트(GET /appApi/tbm/entry-context 응답 매핑 — developer)
// 백엔드 계약 필드(statusCd/gpsVerifyTypeCd/...)를 template/computed 가 참조하는
// 기존 키(status/gpsVerifyType/...)로 매핑해 담는다(템플릿 구조 보존).
const session = ref({
  sessionCd: '',
  title: '',
  status: '',            // SYS046: OPENED / IN_PROGRESS / COMPLETED / ... (서버 statusCd)
  gpsVerifyType: '',     // SYS048: AUTO / MANUAL / DISABLED (서버 gpsVerifyTypeCd)
  gpsVerifyRadiusM: 0,
  managerLeader: '',
  location: '',
})

// 입실 허용 여부는 서버(entryAvailable, D3=OPENED)가 단일 권위. 클라 추측 금지.
const entryAvailable = ref(false)

const requireEntrySign = ref(false)   // C-D1: 입실 서명 없음(종료만)
const requireExitSign = ref(false)    // C-D1: 종료 서명 필수(requiresExitSignature)
const exitPwdRequired = ref(true)

// GPS 결과(developer: requestGps 결과 매핑)
const gpsStatus = ref('')             // OK / PERMISSION_DENIED / SERVICE_DISABLED / TIMEOUT / BRIDGE_UNAVAILABLE
const gpsDistanceM = ref(null)        // 입실 응답의 서버 산출 거리(entryDistanceM)

// 입실/종료 입력
const entryPwd = ref('')
const exitPwd = ref('')
const entryError = ref('')
const exitError = ref('')

// 진행 상태(developer: 컨텍스트/응답 기반 갱신)
const isEntered = ref(false)
const isCompleted = ref(false)
const enteredAt = ref('')

// 중복 제출 가드
const isSubmitting = ref(false)

// 종료 서명 캔버스(서명 여부/blob 보관)
const signCanvasRef = ref(null)
const hasSignature = ref(false)

// ── 파생 상태 ────────────────────────────────────────────────────
// 입실 가능 여부는 서버 entryAvailable(D3) 를 그대로 따른다.
const isEnterable = computed(() => entryAvailable.value)

const statusLabel = computed(() => {
  switch (session.value.status) {
    case 'OPENED':
      return '개설'
    case 'IN_PROGRESS':
      return '진행중'
    case 'COMPLETED':
      return '종료'
    case 'CANCELLED':
      return '취소'
    case 'DRAFT':
      return '작성중'
    default:
      return ''
  }
})

const badgeToneClass = computed(() =>
  session.value.status === 'IN_PROGRESS' || session.value.status === 'OPENED'
    ? 'badge--ok'
    : 'badge--muted',
)

const sessionMetaText = computed(() => {
  const parts = [session.value.location, session.value.managerLeader].filter(Boolean)
  return parts.join(' · ')
})

// GPS 콜아웃: DISABLED 면 숨김(C-D5)
const showGpsCallout = computed(() => session.value.gpsVerifyType !== 'DISABLED')

// AUTO=거리 안/밖, MANUAL=확인 안내 (좌표 비노출 — C-D5)
const isWithinRadius = computed(
  () =>
    gpsDistanceM.value !== null &&
    session.value.gpsVerifyRadiusM > 0 &&
    gpsDistanceM.value <= session.value.gpsVerifyRadiusM,
)

const gpsToneClass = computed(() => (isWithinRadius.value ? 'callout--ok' : 'callout--warn'))
const gpsIconId = computed(() => (isWithinRadius.value ? 'i-tbm-loc-ok' : 'i-tbm-loc-warn'))
const gpsCalloutText = computed(() => {
  if (session.value.gpsVerifyType === 'MANUAL') return '관리자 위치 확인이 필요합니다'
  if (gpsStatus.value && gpsStatus.value !== 'OK') return '위치 확인 중입니다'
  if (gpsDistanceM.value === null) return '위치 확인 중입니다'
  return isWithinRadius.value ? '근무지 안에 있습니다' : '근무지에서 벗어나 있습니다'
})

// 단순 form validation(값 가공 아님)
const canSubmitEnter = computed(() => entryPwd.value.length === 6)
const canSubmitExit = computed(() => !exitPwdRequired.value || exitPwd.value.length === 6)

const enteredInfoText = computed(() => {
  const parts = []
  if (enteredAt.value) parts.push(`입실 ${enteredAt.value}`)
  if (isWithinRadius.value) parts.push('근무지 안')
  return parts.join(' · ')
})

// ── 컨텍스트 조회 (GET /appApi/tbm/entry-context — C3) ────────────
// 백엔드 응답: { sessionCd, title, statusCd, gpsVerifyTypeCd, gpsVerifyRadiusM,
//               entryAvailable, alreadyEntered, requiresExitSignature }
// USER_CD/CMPNY_CD 등 세션값은 axios 인터셉터가 자동 주입한다.
const loadEntryContext = async (sessionCd) => {
  isLoading.value = true
  try {
    const res = await api.get('/appApi/tbm/entry-context', { params: { sessionCd } })
    const data = res?.data
    if (!data) {
      showAlert('세션 정보를 불러오지 못했어요.')
      return
    }
    // 계약 필드 → 템플릿이 참조하는 기존 키로 매핑
    session.value = {
      sessionCd: data.sessionCd || sessionCd,
      title: data.title || '',
      status: data.statusCd || '',
      gpsVerifyType: data.gpsVerifyTypeCd || '',
      gpsVerifyRadiusM: data.gpsVerifyRadiusM || 0,
      managerLeader: '',
      location: '',
    }
    entryAvailable.value = !!data.entryAvailable
    requireExitSign.value = !!data.requiresExitSignature
    // 본인 기입실이면 종료 단계로 진입
    isEntered.value = !!data.alreadyEntered
  } catch (e) {
    console.error('[TbmEntry] entry-context 조회 실패:', e?.message)
    const msg = e?.response?.data?.message || '세션 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
    showAlert(msg)
  } finally {
    isLoading.value = false
  }
}

// ── 종료 서명 캔버스 (외부 라이브러리 없이 pointer 드로잉) ─────────
let signCtx = null
let signDrawing = false

// 캔버스 백버퍼를 표시 크기에 맞춰 초기화(고해상도 대응).
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
    // setPointerCapture 미지원 환경은 무시
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
  // scale() 영향 없이 전체를 지우기 위해 변환 초기화 후 clearRect
  signCtx.save()
  signCtx.setTransform(1, 0, 0, 1, 0, 0)
  signCtx.clearRect(0, 0, canvas.width, canvas.height)
  signCtx.restore()
  hasSignature.value = false
}

// 캔버스 → PNG Blob(File) 변환.
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
      resolve(new File([blob], 'tbm-exit-signature.png', { type: 'image/png' }))
    }, 'image/png')
  })

// ── 액션 ──────────────────────────────────────────────────────────
const onBack = () => {
  router.back()
}

// QR 입실/종료: 현재 앱 self-device 입실 경로는 비밀번호만 지원하고,
// 공용 QrScanner(/QrScanner)는 ChkLst 전용 복귀로 하드코딩되어 범용 반환 경로가 없다.
// TBM 전용 QR 반환/관리자 스캔 입실은 본 라운드 범위 밖(planner 재분해 필요) → 안내만.
const onScanQrToEnter = () => {
  showAlert('QR 입실은 준비 중입니다. 입실 비밀번호로 진행해 주세요.')
}
const onScanQrToExit = () => {
  showAlert('QR 종료는 준비 중입니다. 종료 비밀번호로 진행해 주세요.')
}

// 입실: 비밀번호 + GPS 좌표 전송. 서버가 GPS 유형(AUTO/MANUAL/DISABLED)대로 처리.
const onSubmitEnter = async () => {
  if (!canSubmitEnter.value || isSubmitting.value) return
  entryError.value = ''

  // 1) 현재 좌표 획득(권한은 앱 기동 시 하드게이트로 보장).
  const gps = await requestGps()
  gpsStatus.value = gps.status

  if (gps.status === 'PERMISSION_DENIED' || gps.status === 'SERVICE_DISABLED') {
    showAlert('위치 권한 또는 위치 서비스가 꺼져 있어요. 설정에서 위치를 허용해 주세요.')
    return
  }
  if (gps.status !== 'OK') {
    // TIMEOUT / BRIDGE_UNAVAILABLE 등 측위 실패.
    showAlert('현재 위치를 확인하지 못했어요. 잠시 후 다시 시도해 주세요.')
    return
  }
  // Mock 위치는 서버가 거부할 수 있으나, 사용자 경험상 먼저 안내 후 중단.
  if (gps.isMocked) {
    showAlert('위치 위변조가 감지되어 입실할 수 없어요.')
    return
  }

  // 2) 입실 호출
  isSubmitting.value = true
  try {
    const res = await api.post('/appApi/tbm/enter', {
      sessionCd: session.value.sessionCd,
      entryPwd: entryPwd.value,
      lat: gps.lat,
      lon: gps.lon,
    })
    const data = res?.data || {}
    // 거리(서버 산출)만 화면에 반영(좌표 비노출 — D5)
    gpsDistanceM.value = typeof data.entryDistanceM === 'number' ? data.entryDistanceM : null
    enteredAt.value = data.entryAt || ''
    isEntered.value = true
    if (data.alreadyEntered) {
      showAlert('이미 입실되어 있어요.')
    } else {
      showAlert('입실이 완료되었어요.')
    }
  } catch (e) {
    console.error('[TbmEntry] enter 실패:', e?.message)
    // 비번 불일치/잠금/상태/거리초과 등은 서버 errorCode 메시지를 인라인 표시.
    const msg = e?.response?.data?.message || '입실하지 못했어요. 잠시 후 다시 시도해 주세요.'
    entryError.value = msg
  } finally {
    isSubmitting.value = false
  }
}

// 종료: 종료 비밀번호 + 서명(필수) → multipart 업로드.
const onSubmitExit = async () => {
  if (!canSubmitExit.value || isSubmitting.value) return
  exitError.value = ''

  // 서명 필수 검증(서버도 거부하나 선차단).
  if (requireExitSign.value && !hasSignature.value) {
    showAlert('종료 서명을 입력해 주세요.')
    return
  }

  const signFile = requireExitSign.value ? await signatureToFile() : null
  if (requireExitSign.value && !signFile) {
    showAlert('서명 이미지를 만들지 못했어요. 다시 시도해 주세요.')
    return
  }

  // FormData 구성(Content-Type 은 axios 가 multipart 로 자동 설정 — 인터셉터가 gv_* append).
  const formData = new FormData()
  formData.append('sessionCd', session.value.sessionCd)
  formData.append('exitPwd', exitPwd.value)
  if (signFile) formData.append('item', signFile)

  isSubmitting.value = true
  try {
    await api.post('/appApi/tbm/exit', formData)
    isCompleted.value = true
    await showAlert('이수가 완료되었어요.')
    router.push('/MainView')
  } catch (e) {
    console.error('[TbmEntry] exit 실패:', e?.message)
    const msg = e?.response?.data?.message || '종료하지 못했어요. 잠시 후 다시 시도해 주세요.'
    exitError.value = msg
  } finally {
    isSubmitting.value = false
  }
}

// ── 진입 ────────────────────────────────────────────────────────
onMounted(() => {
  const sessionCd = route.query.sessionCd || route.params.sessionCd || ''
  if (!sessionCd) {
    showAlert('세션 정보가 없어 입실 화면을 열 수 없어요.')
    router.replace('/MainView')
    return
  }
  loadEntryContext(sessionCd)
})
</script>

<style scoped>
/* 디자인 토큰 1회 선언(MyAttendanceView 세트와 동일) — 자식 scoped 가 상속 */
.tbm-entry-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
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
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100%;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
}

/* 헤더 */
.tbm-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.tbm-hd__back {
  width: 36px;
  height: 36px;
  margin-left: -8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.tbm-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.tbm-hd__spacer {
  width: 36px;
}

/* 본문 */
.tbm-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.tbm-loading,
.tbm-unavailable,
.tbm-done {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.tbm-done {
  color: var(--color-primary);
  font-weight: 600;
}

/* 세션 카드 */
.card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
}
.card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.card__meta {
  margin: var(--space-sm) 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.badge {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.badge--ok {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.badge--muted {
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
}

/* GPS 콜아웃 */
.callout {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 10px 12px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
}
.callout--ok {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.callout--warn {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}

/* 입실/종료 단계 */
.step {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.entered-info {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 입력 필드 */
.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.field__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.field__input {
  height: 44px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.field__input:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 구분선 */
.divider {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--color-text-tertiary);
  font-size: 12px;
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border-light);
}

/* 서명 영역(조건부) */
.sign-box {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.sign-box__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sign-box__hint {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.sign-box__clear {
  background: transparent;
  border: 0;
  padding: var(--space-xs) var(--space-sm);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.sign-box__pad {
  display: block;
  width: 100%;
  height: 160px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  touch-action: none;
}

.form-error {
  margin: 0;
  font-size: 13px;
  color: var(--color-danger-text);
}

/* 버튼 */
.btn {
  width: 100%;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-family: inherit;
}
.btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.btn--primary:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
.btn--ghost {
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
}

/* 스프라이트 */
.tbm-sprite {
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
