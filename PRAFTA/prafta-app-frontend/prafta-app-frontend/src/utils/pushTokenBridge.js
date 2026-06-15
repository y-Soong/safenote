// /src/utils/pushTokenBridge.js
//
// prafta-com-008-F (PRAFTA-F03): 네이티브(Flutter 셸) GET_PUSH_TOKEN 브리지 래퍼 +
// 푸시 토큰 등록 호출 + onTokenRefresh 콜백 등록.
//
// 웹뷰 안에서 window.flutter_inappwebview.callHandler('GET_PUSH_TOKEN') 로
// 네이티브가 보유한 FCM 토큰과 알림 권한 상태를 받는다(pull 모델, deviceBridge/foregroundBridge 패턴 동일).
//
// 응답 계약(네이티브와 동일):
//   { pushToken: String|null, platform: 'android', permission: 'granted'|'denied' }
//   - 권한 거부(denied) 또는 pushToken == null 이면 등록 스킵(getPushToken() 이 null 반환).
//
// 등록 호출은 기존 axios 인스턴스(@/api/axios)를 그대로 사용한다(새 HTTP 클라이언트 생성 금지).
//   - gv_deviceId(DEVICE_UUID)는 axios 요청 인터셉터가 전 요청에 자동 동봉 → body 엔 pushToken/platform 만.
//   - 등록은 fire-and-forget: 실패해도 throw 하지 않고 console.warn 만(로그인/앱 기동 차단 금지).
//   - USER_CD 는 서버가 JWT 클레임에서만 도출(IDOR 방지). 클라는 JWT(Authorization 헤더)만 실어 보낸다.
//
// 토큰 자체는 발송키일 뿐 PII 가 아니나, 로깅 시 마스킹(앞 8자 + ***)한다.

import api from '@/api/axios'

// 등록 엔드포인트(F01 계약). gv_deviceId 는 axios 인터셉터가 자동 동봉한다.
const REGISTER_ENDPOINT = '/appApi/device01/push-token'

// 로그인 토큰 보관 키(axios.js / LoginView.vue 와 동일). 등록 API 는 JWT 필요 → 이 키로 로그인 상태를 가드한다.
const LOGIN_TOKEN_KEY = 'token'

// 브리지 사용 가능 여부(웹뷰 내부에서만 true).
function isBridgeAvailable() {
  return (
    typeof window !== 'undefined' &&
    window.flutter_inappwebview &&
    typeof window.flutter_inappwebview.callHandler === 'function'
  )
}

// 로깅용 토큰 마스킹(앞 8자 + ***). 평문 로그 금지.
function maskToken(token) {
  if (!token || typeof token !== 'string') return '(none)'
  return token.length <= 8 ? `${token}***` : `${token.slice(0, 8)}***`
}

// GET_PUSH_TOKEN 응답을 표준 형태로 정규화한다.
// 권한이 granted 가 아니거나 pushToken 이 없으면 null 반환(등록 스킵 신호).
function normalize(res) {
  if (!res || typeof res !== 'object') return null
  const permission = res.permission == null ? null : String(res.permission)
  if (permission !== 'granted') return null
  const pushToken = res.pushToken == null || res.pushToken === '' ? null : String(res.pushToken)
  if (!pushToken) return null
  const platform = res.platform == null || res.platform === '' ? 'android' : String(res.platform)
  return { pushToken, platform }
}

/**
 * 네이티브로부터 현재 FCM 토큰을 취득한다(브리지 부재/실패/권한거부 시 null).
 *
 * @param {Object} [opts]
 * @param {number} [opts.timeoutMs=5000] 브리지 응답 대기 타임아웃(ms).
 * @returns {Promise<{pushToken:string, platform:string}|null>}
 *   - null: 웹 디버그(브리지 부재) / 권한 denied / 토큰 미발급 → 등록 스킵.
 */
export async function getPushToken(opts = {}) {
  const timeoutMs = typeof opts.timeoutMs === 'number' ? opts.timeoutMs : 5000

  // 웹 디버그 등 브리지가 없으면 즉시 null(네트워크 호출 금지).
  if (!isBridgeAvailable()) {
    console.log('[pushTokenBridge] flutter_inappwebview 브리지 없음 → 등록 스킵')
    return null
  }

  const callPromise = window.flutter_inappwebview
    .callHandler('GET_PUSH_TOKEN')
    .then((res) => normalize(res))
    .catch((e) => {
      console.log(`[pushTokenBridge] callHandler 실패: ${e && e.message}`)
      return null
    })

  const timeoutPromise = new Promise((resolve) => {
    setTimeout(() => resolve(null), timeoutMs)
  })

  return Promise.race([callPromise, timeoutPromise])
}

/**
 * 푸시 토큰을 백엔드에 등록한다(F01 엔드포인트).
 * - getPushToken() 으로 토큰 취득 → 있으면 POST, 없으면(권한거부 등) 조용히 스킵.
 * - fire-and-forget: 실패해도 throw 하지 않는다(console.warn 만). 로그인/앱 기동을 절대 막지 않는다.
 *   ★앱 인터셉터는 COMMON_400_003/600 을 토큰오류로 보고 강제 로그아웃시키므로, 여기서 에러를
 *    전파하면 로그인 직후 튕김이 생긴다 → 반드시 삼킨다.
 *
 * @returns {Promise<void>}
 */
export async function registerPushToken() {
  let info = null
  try {
    info = await getPushToken()
  } catch (e) {
    // getPushToken 은 자체적으로 null 폴백하지만 방어적으로 한 번 더 격리한다.
    console.warn('[pushTokenBridge] 토큰 취득 실패(등록 스킵):', e?.message)
    return
  }

  if (!info || !info.pushToken) {
    // 권한 거부 / 토큰 미발급 / 웹 디버그 → 등록 스킵(정상 동작).
    console.log('[pushTokenBridge] 푸시 토큰 없음 → 등록 스킵')
    return
  }

  try {
    await api.post(REGISTER_ENDPOINT, {
      pushToken: info.pushToken,
      platform: info.platform,
    })
    console.log(`[pushTokenBridge] 푸시 토큰 등록 완료: ${maskToken(info.pushToken)}`)
  } catch (e) {
    // 등록 실패는 로그인/앱 기동에 영향 주지 않는다(콘솔 경고만, 예외 전파 금지).
    console.warn('[pushTokenBridge] 푸시 토큰 등록 실패(영향 없음):', e?.message)
  }
}

/**
 * 토큰 refresh 콜백(window.__onPushTokenRefresh)을 전역에 1회 등록한다.
 * Flutter 가 onTokenRefresh 발화 시 window.__onPushTokenRefresh('<token>') 를 호출한다.
 *
 * refresh 는 로그인 여부와 무관하게 발생할 수 있으나 등록 API 는 JWT 가 필요하므로,
 * 콜백 내부에서 sessionStorage 의 로그인 토큰('token') 존재를 가드한다(로그인 상태일 때만 등록 API 호출).
 * 앱 전역 진입(App.vue onMounted)에서 1회 호출한다(N2: 전역 등록 + 내부 토큰 가드).
 */
export function installPushTokenRefreshHandler() {
  if (typeof window === 'undefined') return

  window.__onPushTokenRefresh = () => {
    // 비로그인 상태면 등록 스킵(JWT 없음 → 호출해도 401). 토큰은 로그인 시 다시 등록된다.
    const loginToken = (() => {
      try {
        return sessionStorage.getItem(LOGIN_TOKEN_KEY)
      } catch (e) {
        console.warn('[pushTokenBridge] sessionStorage 접근 실패:', e?.message)
        return null
      }
    })()

    if (!loginToken) {
      console.log('[pushTokenBridge] refresh 수신: 비로그인 상태 → 등록 스킵')
      return
    }

    // registerPushToken 이 GET_PUSH_TOKEN 을 다시 호출해 최신 토큰을 재취득 후 등록한다.
    // fire-and-forget(실패 삼킴) — 콜백이므로 await 하지 않는다.
    registerPushToken()
  }
}

export default {
  getPushToken,
  registerPushToken,
  installPushTokenRefreshHandler,
  isBridgeAvailable,
}
