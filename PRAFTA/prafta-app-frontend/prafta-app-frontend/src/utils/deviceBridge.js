// /src/utils/deviceBridge.js
//
// prafta-com-003 C4: 네이티브(Flutter 셸) GET_DEVICE_INFO 브리지 래퍼.
//
// 웹뷰 안에서 window.flutter_inappwebview.callHandler('GET_DEVICE_INFO') 로
// 네이티브 디바이스 식별자/메타를 받는다(pull 모델, gpsBridge 패턴 동일).
//
// 응답 계약(네이티브와 동일):
//   { deviceId, deviceType: 'ANDROID'|'IOS', model, osVersion, appVersion }
//   - 취득 실패 시 deviceId = null → localStorage UUID 폴백(D1 graceful).
//
// deviceId 는 axios 인터셉터가 매 요청에 gv_deviceId 로 동봉하므로, 네이티브값을
// 받으면 localStorage('gv_deviceId')를 네이티브값으로 덮어쓴다(캐싱). 그러면 이후
// 모든 요청(출퇴근 포함)이 네이티브 deviceId 로 일관 전송된다.
//
// 비즈니스 로직(저장/판정/부정탐지)은 백엔드 몫. 여기서는 취득/캐싱/전달만 담당한다.

const STORAGE_KEY = 'gv_deviceId'

// 마지막으로 받은 디바이스 메타(로그인 요청 동봉용). null = 미취득/웹 디버그.
let cachedDeviceMeta = null

function isBridgeAvailable() {
  return (
    typeof window !== 'undefined' &&
    window.flutter_inappwebview &&
    typeof window.flutter_inappwebview.callHandler === 'function'
  )
}

// 응답 정규화. 문자열 필드만 안전 보정(객체/배열 방어).
function normalize(res) {
  if (!res || typeof res !== 'object') return null
  const str = (v) => (v == null || v === '' ? null : String(v))
  return {
    deviceId: str(res.deviceId),
    deviceType: str(res.deviceType),
    deviceModel: str(res.model),
    osVersion: str(res.osVersion),
    appVersion: str(res.appVersion),
  }
}

/**
 * 네이티브 디바이스 정보를 취득하여 캐시한다(브리지 부재 시 폴백).
 * 네이티브 deviceId 가 있으면 localStorage('gv_deviceId')를 그 값으로 덮어써
 * 이후 axios 의 getDeviceId() 가 네이티브값을 사용하게 한다.
 *
 * @param {Object} [opts]
 * @param {number} [opts.timeoutMs=5000]
 * @returns {Promise<{deviceId, deviceType, deviceModel, osVersion, appVersion}|null>}
 */
export async function requestDeviceInfo(opts = {}) {
  const timeoutMs = typeof opts.timeoutMs === 'number' ? opts.timeoutMs : 5000

  if (!isBridgeAvailable()) {
    // 웹 디버그 등 브리지 없음 → 폴백. 네트워크 호출 금지.
    console.log('[deviceBridge] flutter_inappwebview 브리지 없음 → 폴백(localStorage UUID)')
    return null
  }

  const callPromise = window.flutter_inappwebview
    .callHandler('GET_DEVICE_INFO')
    .then((res) => normalize(res))
    .catch((e) => {
      console.log(`[deviceBridge] callHandler 실패: ${e && e.message}`)
      return null
    })

  const timeoutPromise = new Promise((resolve) => {
    setTimeout(() => resolve(null), timeoutMs)
  })

  const meta = await Promise.race([callPromise, timeoutPromise])

  if (meta) {
    cachedDeviceMeta = meta
    // 네이티브 deviceId 가 있으면 캐싱(이후 모든 요청에 gv_deviceId 로 일관 전송).
    if (meta.deviceId && typeof window !== 'undefined') {
      try {
        localStorage.setItem(STORAGE_KEY, meta.deviceId)
      } catch (e) {
        console.warn('[deviceBridge] deviceId 캐싱 실패:', e?.message)
      }
    }
  }
  return meta
}

/**
 * 마지막으로 취득한 디바이스 메타(로그인 요청 동봉용). 미취득이면 빈 객체.
 * deviceId 는 axios 인터셉터가 gv_deviceId 로 별도 동봉하므로 여기서는 메타만 반환한다.
 */
export function getCachedDeviceMeta() {
  if (!cachedDeviceMeta) return {}
  return {
    deviceType: cachedDeviceMeta.deviceType,
    deviceModel: cachedDeviceMeta.deviceModel,
    osVersion: cachedDeviceMeta.osVersion,
    appVersion: cachedDeviceMeta.appVersion,
  }
}

export default {
  requestDeviceInfo,
  getCachedDeviceMeta,
  isBridgeAvailable,
}
