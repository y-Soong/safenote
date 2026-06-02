// /src/utils/gpsBridge.js
//
// 네이티브(Flutter 셸) GET_GPS 브리지 래퍼.
//
// 웹뷰 안에서 동작할 때 window.flutter_inappwebview.callHandler('GET_GPS') 를
// 호출하여 네이티브가 취득한 현재 위치를 받는다.
//
// 응답 계약(네이티브와 동일):
//   - 정상:    { status: 'OK', lat, lon, accuracy, isMocked }
//   - 권한거부: { status: 'PERMISSION_DENIED' }
//   - 서비스OFF:{ status: 'SERVICE_DISABLED' }
//   - 타임아웃: { status: 'TIMEOUT' }
//   - 브리지부재(웹 디버그 등): { status: 'BRIDGE_UNAVAILABLE' }
//
// 좌표 활용(지오펜스/저장)은 백엔드 몫이며, 여기서는 좌표 획득/전달까지만 담당한다.

// 브리지 사용 가능 여부(웹뷰 내부에서만 true).
function isBridgeAvailable() {
  return (
    typeof window !== 'undefined' &&
    window.flutter_inappwebview &&
    typeof window.flutter_inappwebview.callHandler === 'function'
  )
}

// 응답을 표준 형태로 정규화한다.
// 네이티브가 isMocked 를 bool 로 주므로 숫자 필드만 Number 로 보정.
function normalize(res) {
  if (!res || typeof res !== 'object') {
    return { status: 'TIMEOUT' }
  }
  if (res.status === 'OK') {
    return {
      status: 'OK',
      lat: Number(res.lat),
      lon: Number(res.lon),
      accuracy: Number(res.accuracy),
      isMocked: res.isMocked === true || res.isMocked === 'true',
    }
  }
  // 그 외 상태값은 그대로 전달.
  return { status: res.status }
}

/**
 * 네이티브 현재 위치를 요청한다.
 *
 * @param {Object} [opts]
 * @param {number} [opts.timeoutMs=10000] 브리지 응답 대기 타임아웃(ms).
 * @returns {Promise<{status:string, lat?:number, lon?:number, accuracy?:number, isMocked?:boolean}>}
 */
export async function requestGps(opts = {}) {
  const timeoutMs = typeof opts.timeoutMs === 'number' ? opts.timeoutMs : 10000

  // 웹 디버그 등 브리지가 없으면 즉시 반환(네트워크 호출 금지).
  if (!isBridgeAvailable()) {
    console.log('[gpsBridge] flutter_inappwebview 브리지 없음 → BRIDGE_UNAVAILABLE')
    return { status: 'BRIDGE_UNAVAILABLE' }
  }

  // callHandler 호출과 타임아웃 레이스.
  const callPromise = window.flutter_inappwebview
    .callHandler('GET_GPS')
    .then((res) => normalize(res))
    .catch((e) => {
      console.log(`[gpsBridge] callHandler 실패: ${e && e.message}`)
      return { status: 'TIMEOUT' }
    })

  const timeoutPromise = new Promise((resolve) => {
    setTimeout(() => resolve({ status: 'TIMEOUT' }), timeoutMs)
  })

  return Promise.race([callPromise, timeoutPromise])
}

export default {
  requestGps,
  isBridgeAvailable,
}
