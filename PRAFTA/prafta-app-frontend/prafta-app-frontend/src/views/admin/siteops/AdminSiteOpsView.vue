<!--
  AdminSiteOpsView.vue — 관리자 현장 처리(일용직 QR 출퇴근 등록) [prafta-app-025 J1-7]
  - 진입: AdminLauncherView 본문 "현장 처리"(moduleActiveMap.SITE_OPS===true) → router.push('/AdminSiteOps').
  - 동작: 상단 [출근][퇴근] 모드 토글 + 단건 QR 스캔(스캔 1회→스캐너 정지→서버 처리→결과 토스트→관리자 홈 복귀).
      ※ 과거 연속 스캔 방식은 동일 QR 이 매 프레임 재인식되어 출근행 반복 생성을 시도(멱등 409 로 거부)하던 혼란이 있어
        1회 처리 후 자동 종료로 변경. 여러 명을 연속 처리하려면 종료 후 다시 진입한다.
  - 백엔드:
      POST /appApi/admin/site-ops/attendance/check-in   { qrPayload, siteCd }   (S1 출근, 멱등)
      POST /appApi/admin/site-ops/attendance/check-out  { qrPayload, siteCd }   (S2 퇴근, 멱등)
  - 참조 패턴: views/_common/QrScanner.vue(html5-qrcode 다크 뷰파인더/4코너/스캔라인) — 단 라우팅이 아니라 콜백 연속 처리.
  - C1: 진입/스코프/대상유효성/멱등은 서버만 신뢰. 클라 역할(AUTH_CD) 분기 없음. QR 파싱/식별/검증은 백엔드 몫(가공 없이 payload 전달).
  - PII: 토스트엔 마스킹 이름만(서버가 마스킹해 내려줌). 휴대폰/평문 PII 노출·표시 금지.
  - 현장 권위(★미결 3=b 확정): 화면이 현재 선택 사업장(currentSiteCd)을 바디로 전달하고, 서버가 그 사업장이
    토큰 사용자의 접근가능 사업장(USE_YN='Y') 멤버십인지 재검증(IDOR 방어). currentSiteCd 는 access-context 로 산출.
-->
<template>
  <div class="site-ops">
    <!-- 카메라 권한/초기화 실패 폴백 (QrScanner.vue 와 동일 폴백 재사용) — cameraFailed 에 실패 사유 -->
    <SafetyCameraPermissionView
      v-if="cameraFailed"
      :reason="cameraFailed"
      @cancel="onClose"
      @open-settings="openAppSettings"
      @retry="retryCamera"
    />

    <template v-else>
      <!-- 다크 헤더 -->
      <header class="site-ops-hd">
        <button type="button" class="site-ops-hd__close" aria-label="닫기" @click="onClose">
          <svg class="icon" width="22" height="22" aria-hidden="true">
            <use href="#i-siteops-x" />
          </svg>
        </button>
        <h1 class="site-ops-hd__title">현장 처리</h1>
        <span class="site-ops-hd__spacer" aria-hidden="true"></span>
      </header>

      <!-- 모드 세그먼트 토글 (출근 / 퇴근) — UI 토글(허용 범위) -->
      <div class="site-ops-seg" role="tablist" aria-label="출퇴근 모드">
        <button
          type="button"
          class="site-ops-seg__btn"
          :class="{ 'is-active': mode === 'IN' }"
          role="tab"
          :aria-selected="mode === 'IN'"
          @click="setMode('IN')"
        >
          출근
        </button>
        <button
          type="button"
          class="site-ops-seg__btn"
          :class="{ 'is-active': mode === 'OUT' }"
          role="tab"
          :aria-selected="mode === 'OUT'"
          @click="setMode('OUT')"
        >
          퇴근
        </button>
      </div>

      <!-- 카메라 뷰파인더 -->
      <div class="site-ops-cam">
        <div id="site-ops-reader" class="site-ops-cam__reader"></div>
        <div class="site-ops-cam__overlay" aria-hidden="true"></div>

        <!-- 가이드 프레임 (4코너) -->
        <div class="qr-frame" aria-hidden="true">
          <span class="qr-frame__c qr-frame__c--tl"></span>
          <span class="qr-frame__c qr-frame__c--tr"></span>
          <span class="qr-frame__c qr-frame__c--bl"></span>
          <span class="qr-frame__c qr-frame__c--br"></span>
          <span v-if="!busy" class="qr-scanline"></span>
        </div>

        <!-- 모드별 안내 문구 -->
        <p class="site-ops-tip">
          일용직 QR을 스캔하면 <strong>{{ mode === 'IN' ? '출근' : '퇴근' }}</strong> 처리됩니다
        </p>
        <p class="site-ops-tip-sub">QR이 사각형 안에 들어오면 자동으로 인식돼요</p>

        <!-- 처리 중 표시(연속 스캔 중복 방지) -->
        <p v-if="busy" class="site-ops-busy" aria-live="polite">처리 중…</p>

        <!-- 결과 토스트(비차단, 자동 소멸) — 연속 스캔 흐름 유지 -->
        <transition name="toast-fade">
          <div
            v-if="toast.visible"
            class="site-ops-toast"
            :class="`site-ops-toast--${toast.tone}`"
            aria-live="polite"
          >
            {{ toast.message }}
          </div>
        </transition>

        <!-- 하단 원형 닫기 -->
        <button type="button" class="site-ops-cancel" aria-label="닫기" @click="onClose">
          <svg class="icon" width="22" height="22" aria-hidden="true">
            <use href="#i-siteops-x" />
          </svg>
        </button>
      </div>
    </template>

    <!-- 현장 계약서 서명 시트(2026-08-30) — 출근 스캔 응답 SIGN_REQUIRED 시 전체 오버레이.
         관리자 폰을 근로자에게 전달해 근로자 본인이 열람·서명한다(대리 입력 아님 — 시트가 고지).
         서명 완료 시 보관해 둔 qrPayload 로 출근을 자동 재요청해 완료한다. -->
    <SiteOpsContractSignSheet
      v-if="pendingSign"
      :target-user-cd="pendingSign.userCd"
      :user-nm-masked="pendingSign.userNmMasked"
      :contract-nm="pendingSign.contractNm"
      :site-cd="currentSiteCd"
      @signed="onContractSigned"
      @cancel="onContractSignCancel"
    />

    <!-- 인라인 SVG 스프라이트 -->
    <svg width="0" height="0" class="site-ops-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-siteops-x"
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
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { openNativeAppSettings } from '@/utils/appSettingsBridge'
import { requestNativeCameraPermission } from '@/utils/cameraPermissionBridge'
import { startBackCameraScan, CAMERA_FAIL } from '@/utils/qrCameraStart'
import { startCoverScale } from '@/utils/qrPreviewCover'
import SafetyCameraPermissionView from '@/views/chkLst/components/SafetyCameraPermissionView.vue'
import SiteOpsContractSignSheet from '@/views/admin/siteops/SiteOpsContractSignSheet.vue'

const router = useRouter()

// ── 상태 ─────────────────────────────────────────────────────────────
// 출퇴근 모드: 'IN'=출근 / 'OUT'=퇴근 (UI 토글 — 허용 범위)
const mode = ref('IN')

// 카메라 초기화/권한 실패 → 폴백 화면. null=정상, 그 외 CAMERA_FAIL 사유 문자열.
const cameraFailed = ref(null)
// 한 건 처리 중 잠금(연속 스캔에서 중복 POST 방지). 처리 끝나면 해제하여 다음 스캔 대기.
const busy = ref(false)

// 비차단 토스트(자동 소멸). tone: 'success' | 'info' | 'error'
const toast = ref({ visible: false, message: '', tone: 'success' })

// 현장 권위(★미결 3=b): access-context 가 확정한 현재 사업장. 출퇴근 EP 바디로 전달 → 서버가 멤버십 재검증.
const currentSiteCd = ref('')

// 현장 계약서 서명(2026-08-30): 출근 응답 SIGN_REQUIRED 시 대상 정보 + 재출근용 qrPayload 보관.
//   null=시트 미표시. { userCd, userNmMasked, contractNm, qrPayload }
const pendingSign = ref(null)

// html5-qrcode 인스턴스(단건 스캔 — 1회 인식 후 stop)
let html5QrCode = null
let isStarting = false
let toastTimer = null
let closeTimer = null
let stopCoverScale = null // 프리뷰 cover 배율 감시 해제 함수

// ── 모드 토글(UI) ────────────────────────────────────────────────────
const setMode = (next) => {
  mode.value = next
}

// ── 토스트 표시(비차단, 일정 시간 후 자동 해제) ────────────────────────
const showToast = (message, tone = 'success') => {
  toast.value = { visible: true, message, tone }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.value = { ...toast.value, visible: false }
  }, 2200)
}

// ── 현재 사업장(현장전환 권위) 조회 ─────────────────────────────────────
// access-context 의 currentSiteCd 가 단일 출처(현장전환 반영). 401/403 토큰 에러는 axios 인터셉터가 처리.
//   조회 실패 시 토큰 사업장(gv_siteCd)으로 폴백한다(서버가 최종적으로 멤버십을 재검증하므로 안전).
const loadCurrentSite = async () => {
  try {
    const { data } = await api.get('/appApi/admin/access-context')
    currentSiteCd.value = data?.currentSiteCd || sessionStorage.getItem('gv_siteCd') || ''
  } catch (e) {
    console.warn('[AdminSiteOps] access-context 조회 실패, 토큰 사업장 폴백:', e?.message)
    currentSiteCd.value = sessionStorage.getItem('gv_siteCd') || ''
  }
}

// ── 에러코드 → 토스트 색/문구 매핑 ─────────────────────────────────────
// 멱등(409_080 이미 출근 / 409_082 이미 퇴근)은 정상 흐름이므로 info 톤으로 안내하고 스캔을 계속한다.
//   그 외(409_081 출근기록없음 / 403 거부 / 400 형식오류 등)는 error 톤. 문구는 서버 메시지를 우선 사용.
const isIdempotentCode = (code) => code === 'ATTD_409_080' || code === 'ATTD_409_082'

// ── 스캔 성공 콜백(연속) ──────────────────────────────────────────────
// busy 가드 → mode 별 EP POST(qrPayload 가공 없이 전달) → 응답/에러코드 분기 → 토스트 → busy 해제.
//   파싱/식별/유효성/멱등은 백엔드(S1/S2). 멱등(409_080/082)은 정상 안내로 처리하고 스캔 계속.
const onScanSuccess = async (decodedText) => {
  if (busy.value) return // 처리 중/종료 중에는 다음 프레임 무시
  busy.value = true

  // 단건 처리: 한 번 인식되면 즉시 스캐너를 정지한다.
  //   동일 QR 이 매 프레임 재인식되어 출근행을 반복 생성 시도(멱등 409 로 거부되지만 혼란)하던 문제를 차단.
  stopScanner()

  const url =
    mode.value === 'IN'
      ? '/appApi/admin/site-ops/attendance/check-in'
      : '/appApi/admin/site-ops/attendance/check-out'

  try {
    const { data } = await api.post(url, {
      qrPayload: decodedText,
      siteCd: currentSiteCd.value,
    })

    // 현장 계약서 서명 게이트(2026-08-30): 출근인데 미서명이면 출근이 등록되지 않고
    // SIGN_REQUIRED 가 온다 → 종료하지 않고 서명 시트를 연다(서명 후 자동 재출근).
    if (data?.result === 'SIGN_REQUIRED') {
      pendingSign.value = {
        userCd: data.userCd,
        userNmMasked: data.userNmMasked || '',
        contractNm: data.contractNm || '',
        qrPayload: decodedText,
      }
      busy.value = false
      return
    }

    const actionLabel = mode.value === 'IN' ? '출근' : '퇴근'
    const time = formatHhmm(data?.processedTime)
    const name = data?.userNmMasked || ''
    showToast(`${name} ${actionLabel} 처리됐어요${time ? ' ' + time : ''}`, 'success')
  } catch (e) {
    const code = e?.response?.data?.errorCode
    const message = e?.response?.data?.message
    if (isIdempotentCode(code)) {
      // 멱등(이미 출근/퇴근) — 정상 흐름, 회색 안내.
      showToast(message || '이미 처리됐어요', 'info')
    } else {
      showToast(message || 'QR 처리에 실패했어요. 다시 스캔해 주세요.', 'error')
    }
  } finally {
    busy.value = false
    // 결과 토스트를 잠깐 보여준 뒤 원래(관리자 홈) 화면으로 복귀(단건 스캔 종료).
    //   서명 시트가 열린 경우(pendingSign)는 종료하지 않는다 — 서명 완료/취소 핸들러가 마무리.
    if (!pendingSign.value) scheduleClose()
  }
}

// ── 현장 계약서 서명 시트 콜백 ────────────────────────────────────────
// 서명 완료: 보관해 둔 qrPayload 로 출근을 재요청해 완료한다(게이트는 이제 'N').
const onContractSigned = async () => {
  const ctx = pendingSign.value
  pendingSign.value = null
  if (!ctx) return
  try {
    const { data } = await api.post('/appApi/admin/site-ops/attendance/check-in', {
      qrPayload: ctx.qrPayload,
      siteCd: currentSiteCd.value,
    })
    const time = formatHhmm(data?.processedTime)
    const name = data?.userNmMasked || ctx.userNmMasked
    showToast(`서명 완료! ${name} 출근 처리됐어요${time ? ' ' + time : ''}`, 'success')
  } catch (e) {
    const code = e?.response?.data?.errorCode
    const message = e?.response?.data?.message
    if (isIdempotentCode(code)) {
      showToast(message || '이미 처리됐어요', 'info')
    } else {
      // 서명은 저장됐으나 출근 재요청 실패 — 다시 스캔하면 서명 없이 바로 출근된다.
      showToast(message || '서명은 저장됐어요. 출근은 다시 스캔해 주세요.', 'error')
    }
  } finally {
    scheduleClose()
  }
}

// 서명 취소: 출근 미등록 상태 그대로 종료(안내 후 관리자 홈 복귀).
const onContractSignCancel = () => {
  const name = pendingSign.value?.userNmMasked || ''
  pendingSign.value = null
  showToast(`${name} 서명이 취소되어 출근이 처리되지 않았어요`, 'info')
  scheduleClose()
}

// 결과 안내 토스트를 잠깐 노출한 뒤 자동 종료(관리자 홈 복귀). 단건 스캔 UX.
const scheduleClose = () => {
  if (closeTimer) clearTimeout(closeTimer)
  closeTimer = setTimeout(() => {
    router.replace('/AdminHome')
  }, 1400)
}

// HHMM(4자리) → HH:MM 표시. 형식이 아니면 원문 반환.
const formatHhmm = (hhmm) => {
  if (!hhmm || typeof hhmm !== 'string' || hhmm.length !== 4) return hhmm || ''
  return `${hhmm.slice(0, 2)}:${hhmm.slice(2, 4)}`
}

// ── 스캐너 시작/정지 ─────────────────────────────────────────────────
// QrScanner.vue startScanner 패턴 동형. 단 onScanSuccess 에서 stop 하지 않고 busy 락으로 연속 처리.
const startScanner = async () => {
  if (isStarting) return
  isStarting = true
  try {
    // 0) 네이티브 카메라 권한 선확인 — 사유/근거는 QrScanner.vue 동일 주석 참조.
    const perm = await requestNativeCameraPermission()
    if (perm === 'DENIED' || perm === 'PERMANENTLY_DENIED') {
      cameraFailed.value = CAMERA_FAIL.DENIED
      return
    }

    html5QrCode = new Html5Qrcode('site-ops-reader')
    const config = { fps: 10 }

    // 후면 카메라 + 타임아웃 + 실패 사유 분류 — 상세는 utils/qrCameraStart.js 주석 참조.
    await startBackCameraScan(html5QrCode, config, onScanSuccess, () => {
      /* 프레임별 인식 실패는 정상 동작(무시) */
    })

    // 프리뷰를 화면에 꽉 차게(cover) 보이도록 배율 주입. CSS 로 늘리면 디코딩이 깨진다
    // — 상세는 utils/qrPreviewCover.js 주석 참조.
    stopCoverScale = startCoverScale(document.getElementById('site-ops-reader'))
  } catch (err) {
    console.warn('[AdminSiteOps] 카메라 초기화 실패:', err?.reason, err?.message)
    // 미시작 인스턴스 잔재 정리(시작 실패라 stop 은 불필요)
    try {
      html5QrCode?.clear()
    } catch {
      /* noop */
    }
    html5QrCode = null
    cameraFailed.value = err?.reason || CAMERA_FAIL.ERROR
  } finally {
    isStarting = false
  }
}

// 폴백 화면의 '다시 시도' — 스캐너 템플릿 재마운트(#site-ops-reader 재생성) 후 재시작.
const retryCamera = async () => {
  cameraFailed.value = null
  await nextTick()
  startScanner()
}

const stopScanner = () => {
  if (stopCoverScale) {
    stopCoverScale()
    stopCoverScale = null
  }
  if (!html5QrCode) return
  const instance = html5QrCode
  html5QrCode = null
  try {
    instance
      .stop()
      .then(() => instance.clear())
      .catch(() => {})
  } catch {
    /* noop */
  }
}

// ── 네비게이션 / 권한 ────────────────────────────────────────────────
// 닫기 → 관리자 모드 런처 복귀. 히스토리 누적 방지를 위해 replace.
const onClose = () => {
  stopScanner()
  router.replace('/AdminHome')
}

// OS 앱 설정 화면 열기. 네이티브 브리지로만 가능하다 —
// window.location='app-settings:' 는 웹뷰에서 열리지 않고 로딩만 걸린다(실측).
const openAppSettings = async () => {
  const ok = await openNativeAppSettings()
  if (!ok) {
    window.alert('설정 앱에서 PRAFTA > 카메라 권한을 직접 켜주세요.')
  }
}

onMounted(async () => {
  await loadCurrentSite()
  startScanner()
})

onBeforeUnmount(() => {
  if (toastTimer) clearTimeout(toastTimer)
  if (closeTimer) clearTimeout(closeTimer)
  stopScanner()
})
</script>

<style scoped>
.site-ops {
  /* 디자인 토큰(QrScanner.vue 동형 — 다크 뷰파인더 화면) */
  --color-primary: #16a34a;
  --color-primary-deep: #15803d;
  --color-info: #f59e0b;
  --color-danger: #ef4444;

  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0a0a0a;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 다크 헤더(카메라 위 absolute) */
.site-ops-hd {
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
.site-ops-hd__close {
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
.site-ops-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #ffffff;
}
.site-ops-hd__spacer {
  width: 44px;
}

/* 모드 세그먼트 토글(헤더 아래 absolute 띠) */
.site-ops-seg {
  position: absolute;
  top: calc(56px + env(safe-area-inset-top));
  left: 0;
  right: 0;
  z-index: 3;
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.35);
}
.site-ops-seg__btn {
  flex: 1;
  height: 44px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.75);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition:
    background 0.15s ease,
    color 0.15s ease,
    border-color 0.15s ease;
}
.site-ops-seg__btn.is-active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
}

/* 카메라 뷰파인더 */
.site-ops-cam {
  position: relative;
  flex: 1;
  overflow: hidden;
  background: #0a0a0a;
}
/* ★!important 필수 — html5-qrcode 가 start() 시 인라인 position:relative 를 박아
   absolute 를 덮어쓰면 컨테이너 높이가 auto 로 풀리고, video 의 height:100% 가 무시되어
   비디오가 제 화면비 높이만 차지한다(아래 영역이 배경색으로 남는 검정 증상).
   상세는 views/_common/QrScanner.vue 의 동일 주석 참조 — 두 화면 동형 구조다. */
.site-ops-cam__reader {
  position: absolute !important;
  inset: 0;
}
/* ★video 의 width/height 를 건드리지 않는다 — 늘리면 html5-qrcode 의 디코딩 좌표가
   깨져 QR 이 인식되지 않는다. 화면 채우기는 transform 으로만 한다.
   상세는 views/_common/QrScanner.vue 의 동일 주석 / utils/qrPreviewCover.js 참조. */
.site-ops-cam__reader :deep(video) {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) scale(var(--qr-cover-scale, 1));
  transform-origin: center center;
}
.site-ops-cam__reader :deep(#site-ops-reader__dashboard),
.site-ops-cam__reader :deep(#site-ops-reader__header_message),
.site-ops-cam__reader :deep(img[alt='Info icon']),
.site-ops-cam__reader :deep(#site-ops-reader__scan_region img) {
  display: none !important;
}
.site-ops-cam__overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: radial-gradient(circle at 50% 50%, rgba(0, 0, 0, 0.15) 0%, rgba(0, 0, 0, 0.55) 80%);
  pointer-events: none;
}

/* 가이드 프레임(4코너 + 스캔라인) */
.qr-frame {
  position: absolute;
  left: 50%;
  top: 48%;
  z-index: 2;
  transform: translate(-50%, -50%);
  width: 240px;
  height: 240px;
  pointer-events: none;
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
.qr-scanline {
  position: absolute;
  left: 8px;
  right: 8px;
  height: 2px;
  background: var(--color-primary);
  box-shadow: 0 0 12px var(--color-primary);
  border-radius: 2px;
  animation: site-ops-scan 2s ease-in-out infinite;
}
@keyframes site-ops-scan {
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
.site-ops-tip {
  position: absolute;
  z-index: 2;
  left: 0;
  right: 0;
  bottom: 132px;
  margin: 0;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  padding: 0 24px;
}
.site-ops-tip strong {
  color: var(--color-primary);
}
.site-ops-tip-sub {
  position: absolute;
  z-index: 2;
  left: 0;
  right: 0;
  bottom: 110px;
  margin: 0;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  padding: 0 24px;
}

/* 처리 중 표시 */
.site-ops-busy {
  position: absolute;
  z-index: 2;
  left: 0;
  right: 0;
  bottom: 88px;
  margin: 0;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
}

/* 결과 토스트(비차단) */
.site-ops-toast {
  position: absolute;
  z-index: 4;
  left: 16px;
  right: 16px;
  bottom: calc(96px + env(safe-area-inset-bottom));
  padding: 12px 16px;
  border-radius: 12px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.35);
}
.site-ops-toast--success {
  background: var(--color-primary-deep);
}
.site-ops-toast--info {
  background: var(--color-info);
}
.site-ops-toast--error {
  background: var(--color-danger);
}
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.18s ease;
}
.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
}

/* 하단 원형 닫기(56x56) */
.site-ops-cancel {
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

.site-ops-sprite {
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
