// /src/utils/foregroundBridge.js
//
// prafta-051-10: 네이티브(Flutter 셸) GET_APP_FOREGROUND_SEC 브리지 래퍼.
//
// 웹뷰 안에서 window.flutter_inappwebview.callHandler('GET_APP_FOREGROUND_SEC') 로
// 네이티브가 누적한 앱 포그라운드 시간(초)을 받는다(pull 모델, gpsBridge 패턴 동일).
//
// 응답 계약(네이티브와 동일):
//   - 정상:       { status: 'OK', foregroundSec: number }
//   - 브리지부재:  { status: 'BRIDGE_UNAVAILABLE', foregroundSec: null }
//   - 실패/타임아웃:{ status: 'BRIDGE_UNAVAILABLE', foregroundSec: null }
//
// 누적/합산은 네이티브 몫이며, 여기서는 값 취득/정규화/전달까지만 담당한다.
// 세션 귀속·NULL 처리·저장은 백엔드 몫이다.

import { isKnownMissing } from '@/utils/shellCapability'

// 브리지 사용 가능 여부(웹뷰 내부에서만 true).
function isBridgeAvailable() {
  return (
    typeof window !== 'undefined' &&
    window.flutter_inappwebview &&
    typeof window.flutter_inappwebview.callHandler === 'function'
  )
}

// 응답을 표준 형태로 정규화한다.
// foregroundSec 가 유효한 0 이상 정수가 아니면 BRIDGE_UNAVAILABLE/null 로 처리(BE NULL 저장).
function normalize(res) {
  if (!res || typeof res !== 'object' || res.status !== 'OK') {
    return { status: 'BRIDGE_UNAVAILABLE', foregroundSec: null }
  }
  const sec = Number(res.foregroundSec)
  if (!Number.isFinite(sec) || sec < 0) {
    return { status: 'BRIDGE_UNAVAILABLE', foregroundSec: null }
  }
  return { status: 'OK', foregroundSec: Math.floor(sec) }
}

/**
 * 네이티브 앱 포그라운드 누적초를 요청한다.
 *
 * @param {Object} [opts]
 * @param {number} [opts.timeoutMs=3000] 브리지 응답 대기 타임아웃(ms).
 * @returns {Promise<{status:string, foregroundSec:(number|null)}>}
 */
export async function requestForegroundSec(opts = {}) {
  const timeoutMs = typeof opts.timeoutMs === 'number' ? opts.timeoutMs : 3000

  // 웹 디버그 등 브리지가 없으면 즉시 반환(네트워크 호출 금지).
  if (!isBridgeAvailable()) {
    console.log('[foregroundBridge] flutter_inappwebview 브리지 없음 → BRIDGE_UNAVAILABLE')
    return { status: 'BRIDGE_UNAVAILABLE', foregroundSec: null }
  }

  // 셸이 GET_APP_FOREGROUND_SEC 를 모른다고 선언(원격 Vue + 구버전 셸 스큐)
  // → 타임아웃 대기 없이 즉시 부재 처리.
  if (isKnownMissing('GET_APP_FOREGROUND_SEC')) {
    console.log('[foregroundBridge] 셸 미지원 핸들러 선언(GET_APP_FOREGROUND_SEC) → BRIDGE_UNAVAILABLE')
    return { status: 'BRIDGE_UNAVAILABLE', foregroundSec: null }
  }

  // callHandler 호출과 타임아웃 레이스.
  const callPromise = window.flutter_inappwebview
    .callHandler('GET_APP_FOREGROUND_SEC')
    .then((res) => normalize(res))
    .catch((e) => {
      console.log(`[foregroundBridge] callHandler 실패: ${e && e.message}`)
      return { status: 'BRIDGE_UNAVAILABLE', foregroundSec: null }
    })

  const timeoutPromise = new Promise((resolve) => {
    setTimeout(() => resolve({ status: 'BRIDGE_UNAVAILABLE', foregroundSec: null }), timeoutMs)
  })

  return Promise.race([callPromise, timeoutPromise])
}

export default {
  requestForegroundSec,
  isBridgeAvailable,
}
