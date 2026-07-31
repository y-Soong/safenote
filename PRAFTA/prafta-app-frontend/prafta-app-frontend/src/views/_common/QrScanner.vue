<!--
  QrScanner.vue — 안전점검 QR 스캐너 (prafta-app-011 화면 A 리디자인)
  - 다크 뷰파인더 + 240x240 가이드 프레임(4코너) + 그린 스캔라인 애니메이션 + 안내문구 + 56x56 원형 닫기.
  - 스캔 엔진(html5-qrcode) / 라우팅(router.replace('/ChkLst', { query:{ qr } })) 보존.
  - 카메라 실패 폴백(케이스6): SafetyCameraPermissionView.
  - 형식오류(케이스7): SafetyQrErrorOverlay (파싱 불가/형식 오류만 스캐너 단계 처리.
    등록여부/사업장 불일치는 백엔드 응답 기반으로 ChkLst 화면에서 처리).
  - 색/간격은 컴포넌트 scoped 디자인 토큰만 사용(하드코딩 금지).
-->
<template>
  <div class="qr-scan">
    <!-- 카메라 권한/초기화 실패 폴백 (케이스 6) -->
    <SafetyCameraPermissionView
      v-if="cameraFailed"
      @cancel="goHome"
      @open-settings="openAppSettings"
    />

    <!-- 정상 스캐너 -->
    <template v-else>
      <!-- 다크 헤더 -->
      <header class="qr-hd">
        <button type="button" class="qr-hd__close" aria-label="닫기" @click="goHome">
          <svg class="icon" width="22" height="22" aria-hidden="true">
            <use href="#i-qr-x" />
          </svg>
        </button>
        <h1 class="qr-hd__title">QR 스캔</h1>
        <span class="qr-hd__spacer" aria-hidden="true"></span>
      </header>

      <!-- 카메라 뷰파인더 -->
      <div class="qr-cam">
        <!-- html5-qrcode 가 그리는 비디오 영역 -->
        <div id="qr-reader" class="qr-cam__reader"></div>

        <!-- 어둡게 처리되는 배경 오버레이 (실제 카메라 위에 얹힘) -->
        <div class="qr-cam__overlay" aria-hidden="true"></div>

        <!-- 가이드 프레임 (4코너 마커) -->
        <div class="qr-frame" :class="{ 'qr-frame--dim': qrError }" aria-hidden="true">
          <span class="qr-frame__c qr-frame__c--tl"></span>
          <span class="qr-frame__c qr-frame__c--tr"></span>
          <span class="qr-frame__c qr-frame__c--bl"></span>
          <span class="qr-frame__c qr-frame__c--br"></span>
          <!-- 스캔라인 (오류 시 숨김) -->
          <span v-if="!qrError" class="qr-scanline"></span>
        </div>

        <!-- 안내 문구 (오류 토스트 노출 시 숨김) -->
        <template v-if="!qrError">
          <p class="qr-tip">점검 개소의 QR 코드를 스캔해 주세요</p>
          <p class="qr-tip-sub">QR이 사각형 안에 들어오면 자동으로 인식돼요</p>
        </template>

        <!-- 미등록/형식오류 토스트 (케이스 7) -->
        <SafetyQrErrorOverlay v-if="qrError" :message="qrErrorMessage" />

        <!-- 하단 원형 닫기/재시도 버튼 -->
        <button
          type="button"
          class="qr-cancel"
          :aria-label="qrError ? '다시 스캔' : '닫기'"
          @click="qrError ? retryScan() : goHome()"
        >
          <svg class="icon" width="22" height="22" aria-hidden="true">
            <use :href="qrError ? '#i-qr-refresh' : '#i-qr-x'" />
          </svg>
        </button>
      </div>
    </template>

    <!-- 인라인 SVG 스프라이트 (본 화면 전용) -->
    <svg width="0" height="0" class="qr-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-qr-x"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </symbol>
        <symbol
          id="i-qr-refresh"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="23 4 23 10 17 10" />
          <polyline points="1 20 1 14 7 14" />
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'
import { useRouter } from 'vue-router'
import SafetyCameraPermissionView from '@/views/chkLst/components/SafetyCameraPermissionView.vue'
import SafetyQrErrorOverlay from '@/views/chkLst/components/SafetyQrErrorOverlay.vue'

const router = useRouter()

// ───────────────────────────────────────────────────────────
// 상태
// ───────────────────────────────────────────────────────────
const cameraFailed = ref(false) // 카메라 초기화/권한 실패 → 폴백 화면(케이스 6)
const qrError = ref(false) // QR 형식 오류 → 토스트 오버레이(케이스 7)
const qrErrorMessage = ref('QR 코드 형식을 확인할 수 없어요. 다시 스캔해 주세요.')

let html5QrCode = null
let isStarting = false
let qrErrorTimer = null

// ───────────────────────────────────────────────────────────
// QR 페이로드 파싱 — 스캐너 단계에서는 "형식 검증"만 수행.
//   siteCd/chkptCd 추출 성공 시 ChkLst 로 위임(등록여부/사업장 일치는 백엔드 응답 기반).
//   지원 포맷: JSON / URL-encoded JSON / "k=v&k=v" 쿼리 / "siteCd|chkptCd" 파이프.
// ───────────────────────────────────────────────────────────
const parseQrPayload = (raw) => {
  if (!raw) return null
  let s = Array.isArray(raw) ? raw[0] : raw
  if (typeof s !== 'string') s = String(s)
  s = s.trim()
  if (!s) return null

  // 1) JSON
  try {
    const obj = JSON.parse(s)
    if (obj && (obj.siteCd || obj.chkptCd)) return obj
  } catch {
    /* noop */
  }
  // 2) URL-encoded JSON
  try {
    const obj = JSON.parse(decodeURIComponent(s))
    if (obj && (obj.siteCd || obj.chkptCd)) return obj
  } catch {
    /* noop */
  }
  // 3) k=v&k=v 쿼리 스트링
  if (s.includes('=')) {
    const obj = {}
    s.split('&').forEach((pair) => {
      const [k, v] = pair.split('=')
      if (k) obj[decodeURIComponent(k)] = decodeURIComponent(v || '')
    })
    if (obj.siteCd || obj.chkptCd) return obj
  }
  // 4) "siteCd|chkptCd" 파이프 복합 (요청서 §3.1 표준 인코딩)
  if (s.includes('|')) {
    const [siteCd, chkptCd] = s.split('|')
    if (siteCd && chkptCd) return { siteCd: siteCd.trim(), chkptCd: chkptCd.trim() }
  }
  return null
}

// siteCd/chkptCd 형식 검증 정규식 (브리프 §수정항목2)
const QR_CD_PATTERN = /^[A-Za-z0-9_-]{1,50}$/

// ───────────────────────────────────────────────────────────
// 스캔 성공 콜백
// ───────────────────────────────────────────────────────────
const onScanSuccess = (decodedText) => {
  // 오류 토스트가 떠 있는 동안에는 중복 처리 방지
  if (qrError.value) return

  const parsed = parseQrPayload(decodedText)
  if (!parsed || !parsed.siteCd || !parsed.chkptCd) {
    // 형식 오류 → 케이스 7 토스트 (스캔은 계속 동작)
    showQrError('등록되지 않은 QR 코드예요. 점검 개소의 QR 코드인지 확인 후 다시 스캔해 주세요.')
    return
  }

  // siteCd/chkptCd 형식 검증: 허용 패턴 미충족 시 토스트 처리 후 재스캔
  if (!QR_CD_PATTERN.test(parsed.siteCd) || !QR_CD_PATTERN.test(parsed.chkptCd)) {
    showQrError('QR 코드 형식이 올바르지 않아요. 다시 스캔해 주세요.')
    return
  }

  // 정상 → 점검 응답 화면으로 위임 (등록여부/사업장 일치는 백엔드가 판정)
  stopScanner()
  router.replace({ path: '/ChkLst', query: { qr: decodedText } })
}

// 형식 오류 토스트 표시 (일정 시간 후 자동 해제하여 재스캔 가능)
const showQrError = (message) => {
  qrErrorMessage.value = message
  qrError.value = true
  if (qrErrorTimer) clearTimeout(qrErrorTimer)
  qrErrorTimer = setTimeout(() => {
    qrError.value = false
  }, 3000)
}

// 다시 스캔 (토스트 즉시 해제)
const retryScan = () => {
  if (qrErrorTimer) clearTimeout(qrErrorTimer)
  qrError.value = false
}

// ───────────────────────────────────────────────────────────
// 스캐너 시작/정지
// ───────────────────────────────────────────────────────────
const startScanner = async () => {
  if (isStarting) return
  isStarting = true
  try {
    html5QrCode = new Html5Qrcode('qr-reader')
    // qrbox 를 지정하지 않는다. 지정 시 html5-qrcode 가 자체 스캔영역 가이드 박스를
    // 추가로 그려, 우리 디자인 프레임(.qr-frame)과 겹쳐 박스가 2개로 보인다.
    // 전체 프레임 스캔으로 두고, 시각 가이드는 .qr-frame 만 사용한다.
    const config = { fps: 10 }

    const devices = await Html5Qrcode.getCameras()
    if (!devices || !devices.length) throw new Error('No camera found')

    // 후면 카메라 우선
    const backCam = devices.find((d) => /back|rear|environment/i.test(d.label)) || devices[0]

    await html5QrCode.start({ deviceId: { exact: backCam.id } }, config, onScanSuccess, () => {
      /* 프레임별 인식 실패는 정상 동작(무시) */
    })
  } catch (err) {
    // 권한 거부/카메라 점유/장치 부재 → 폴백 화면(케이스 6)
    console.warn('[QrScanner] 카메라 초기화 실패:', err?.message)
    cameraFailed.value = true
  } finally {
    isStarting = false
  }
}

const stopScanner = () => {
  if (!html5QrCode) return
  const instance = html5QrCode
  html5QrCode = null
  try {
    instance
      .stop()
      .then(() => instance.clear())
      .catch(() => {
        /* 이미 정지된 경우 무시 */
      })
  } catch {
    /* noop */
  }
}

// ───────────────────────────────────────────────────────────
// 네비게이션 / 권한 설정
// ───────────────────────────────────────────────────────────
const goHome = () => {
  stopScanner()
  router.push('/MainView')
}

// 설정 앱 deep link 시도 (웹뷰 환경에서는 동작이 제한될 수 있어 안내 폴백).
const openAppSettings = () => {
  try {
    window.location.href = 'app-settings:'
  } catch {
    /* deep link 미지원 환경 — 사용자 수동 설정 안내 */
  }
}

onMounted(() => {
  startScanner()
})

onBeforeUnmount(() => {
  if (qrErrorTimer) clearTimeout(qrErrorTimer)
  stopScanner()
})
</script>

<style scoped>
.qr-scan {
  /* 디자인 토큰 — 시안 토큰을 컴포넌트 scoped 변수로 선언 */
  --color-primary: #16a34a;
  --color-primary-deep: #15803d;
  --color-surface: #ffffff;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;

  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0a0a0a;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 다크 헤더 (카메라 위 absolute) */
.qr-hd {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 3;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  padding-top: env(safe-area-inset-top);
  background: rgba(0, 0, 0, 0.4);
}
.qr-hd__close {
  width: 44px;
  height: 44px;
  margin-left: -10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: #ffffff;
  font-family: inherit;
}
.qr-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #ffffff;
}
.qr-hd__spacer {
  width: 44px;
}

/* 카메라 뷰파인더 */
.qr-cam {
  position: relative;
  flex: 1;
  overflow: hidden;
  background: #0a0a0a;
}
/* ★position:absolute 에 !important 가 반드시 필요하다.
   html5-qrcode 는 start() 시 이 컨테이너에 인라인으로 position:relative 를 박는다
   (html5-qrcode.js: element.style.position = "relative"). 인라인 선언이 클래스 규칙을
   이기므로 !important 가 없으면 absolute 가 무효화되고, inset:0 은 relative 박스의 크기를
   만들지 못해 컨테이너 높이가 auto 로 풀린다. 그러면 아래 video 의 height:100% 가
   "auto 높이 부모에 대한 백분율"이라 무시되고, 라이브러리가 폭만 지정한 채(높이 미지정,
   camera/core-impl.js createVideoElement) 비디오가 제 화면비 높이만 차지해
   남은 아래 영역이 .qr-cam 배경(#0a0a0a)으로 남는다 = 화면 절반 검정 증상. */
.qr-cam__reader {
  position: absolute !important;
  inset: 0;
}
.qr-cam__reader :deep(video) {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover;
}
/* html5-qrcode 기본 UI(테두리/안내문) 숨김 — 자체 가이드 사용 */
.qr-cam__reader :deep(#qr-reader__dashboard),
.qr-cam__reader :deep(#qr-reader__header_message),
.qr-cam__reader :deep(img[alt='Info icon']),
.qr-cam__reader :deep(#qr-reader__scan_region img),
.qr-cam__reader :deep(#qr-shaded-region) {
  display: none !important;
}
.qr-cam__overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: radial-gradient(circle at 50% 45%, rgba(0, 0, 0, 0.15) 0%, rgba(0, 0, 0, 0.55) 80%);
  pointer-events: none;
}

/* 가이드 프레임 */
.qr-frame {
  position: absolute;
  left: 50%;
  top: 45%;
  z-index: 2;
  transform: translate(-50%, -50%);
  width: 240px;
  height: 240px;
  pointer-events: none;
}
.qr-frame--dim {
  opacity: 0.5;
}
.qr-frame__c {
  position: absolute;
  width: 32px;
  height: 32px;
  border-color: #ffffff;
  border-style: solid;
  border-width: 0;
}
.qr-frame__c--tl {
  top: 0;
  left: 0;
  border-top-width: 3px;
  border-left-width: 3px;
  border-top-left-radius: 6px;
}
.qr-frame__c--tr {
  top: 0;
  right: 0;
  border-top-width: 3px;
  border-right-width: 3px;
  border-top-right-radius: 6px;
}
.qr-frame__c--bl {
  bottom: 0;
  left: 0;
  border-bottom-width: 3px;
  border-left-width: 3px;
  border-bottom-left-radius: 6px;
}
.qr-frame__c--br {
  bottom: 0;
  right: 0;
  border-bottom-width: 3px;
  border-right-width: 3px;
  border-bottom-right-radius: 6px;
}

/* 스캔라인 애니메이션 */
.qr-scanline {
  position: absolute;
  left: 8px;
  right: 8px;
  height: 2px;
  background: var(--color-primary);
  box-shadow: 0 0 12px var(--color-primary);
  border-radius: 2px;
  animation: qr-scan-move 2s ease-in-out infinite;
}
@keyframes qr-scan-move {
  0% {
    top: 8px;
  }
  50% {
    top: calc(100% - 10px);
  }
  100% {
    top: 8px;
  }
}

/* 안내 문구 */
.qr-tip {
  position: absolute;
  z-index: 2;
  left: 0;
  right: 0;
  bottom: 88px;
  margin: 0;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  padding: 0 24px;
}
.qr-tip-sub {
  position: absolute;
  z-index: 2;
  left: 0;
  right: 0;
  bottom: 64px;
  margin: 0;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  padding: 0 24px;
}

/* 하단 원형 닫기 (56x56) */
.qr-cancel {
  position: absolute;
  z-index: 3;
  left: 50%;
  bottom: calc(20px + env(safe-area-inset-bottom));
  transform: translateX(-50%);
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  cursor: pointer;
  font-family: inherit;
}

.qr-sprite {
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
