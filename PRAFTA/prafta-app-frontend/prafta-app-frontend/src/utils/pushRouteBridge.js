// /src/utils/pushRouteBridge.js
//
// PRAFTA-WEB_001-5: 푸시 알림 "탭(open)" 라우팅 브리지.
//
// Flutter 셸이 알림 탭(onMessageOpenedApp / 콜드스타트 getInitialMessage) 시
// window.__onPushOpened(<DATA_PAYLOAD>) 를 호출한다(push 모델, pushTokenBridge 의
// window.__onPushTokenRefresh 와 동일 계약 방향). 본 모듈은 그 콜백을 전역 1회 등록하고,
// DATA_PAYLOAD 의 type 에 따라 해당 화면으로 라우팅한다.
//
// DATA_PAYLOAD 계약(백엔드 PushSenderServiceImpl):
//   소속이동: { type: 'TRANSFER_RESERVED', reservationId: '...' }
//
// payload 는 문자열(JSON) 또는 객체로 올 수 있어 둘 다 허용한다(Flutter evaluateJavascript
// 로 JSON 문자열을 넘기는 게 안전하므로 기본은 문자열 파싱).
//
// ★ best-effort: 라우팅 실패/비로그인/브리지 미동작은 조용히 무시한다(앱 기동/조작을 막지 않는다).
//   소속이동 안내 시트의 실제 데이터는 MainView 가 GET /appApi/user01/my-transfer-notice 로
//   재조회하므로, 본 모듈은 "MainView 로 보내고 시트를 다시 띄우라는 신호"만 책임진다.

import router from '@/router/index.js'

// MainView 가 수신하는 "소속이동 안내 시트 재오픈" 커스텀 이벤트명(파일 간 단일 출처).
export const TRANSFER_NOTICE_OPEN_EVENT = 'prafta:transfer-notice-open'

// 로그인 토큰 보관 키(axios.js / pushTokenBridge.js 와 동일).
const LOGIN_TOKEN_KEY = 'token'

// payload(문자열 JSON | 객체)를 객체로 정규화. 파싱 불가 시 null.
function normalizePayload(payload) {
  if (payload == null) return null
  if (typeof payload === 'object') return payload
  if (typeof payload === 'string') {
    try {
      return JSON.parse(payload)
    } catch (e) {
      console.warn('[pushRouteBridge] payload 파싱 실패:', e && e.message)
      return null
    }
  }
  return null
}

// 소속이동 안내 시트로 진입(또는 재오픈) 신호.
//   - 비로그인: MainView 진입 시 my-transfer-notice 가 자동 노출되므로 별도 처리 불필요(best-effort).
//   - MainView 가 아니면 이동 후 신호, 이미 MainView 면 즉시 신호.
function openTransferNotice() {
  let token = null
  try {
    token = sessionStorage.getItem(LOGIN_TOKEN_KEY)
  } catch (e) {
    console.warn('[pushRouteBridge] sessionStorage 접근 실패:', e && e.message)
  }
  // 비로그인 상태면 라우팅하지 않는다(로그인 후 MainView 가 자연 노출).
  if (!token) {
    console.log('[pushRouteBridge] 비로그인 상태 → 소속이동 라우팅 스킵')
    return
  }

  const dispatch = () => {
    try {
      window.dispatchEvent(new CustomEvent(TRANSFER_NOTICE_OPEN_EVENT))
    } catch (e) {
      console.warn('[pushRouteBridge] 이벤트 디스패치 실패:', e && e.message)
    }
  }

  try {
    const current = router.currentRoute.value
    if (current && current.path !== '/MainView') {
      // MainView 진입(onMounted)에서 my-transfer-notice 를 로드하므로 신호는 보조(중복은 멱등).
      router
        .push('/MainView')
        .then(dispatch)
        .catch(() => {})
    } else {
      dispatch()
    }
  } catch (e) {
    // 라우터 미준비 등은 best-effort 로 무시.
    console.warn('[pushRouteBridge] 라우팅 실패(무시):', e && e.message)
  }
}

// DATA_PAYLOAD.type 분기. 신규 트리거는 여기에 case 를 추가한다.
function routeByPushType(data) {
  switch (data.type) {
    case 'TRANSFER_RESERVED':
      openTransferNotice()
      break
    default:
      // 미지원 type 은 무시(향후 확장 지점).
      console.log('[pushRouteBridge] 미지원 푸시 type:', data.type)
      break
  }
}

/**
 * 푸시 탭(open) 콜백(window.__onPushOpened)을 전역에 1회 등록한다.
 * Flutter 가 알림 탭 시 window.__onPushOpened(<DATA_PAYLOAD>) 를 호출한다.
 * 앱 전역 진입(App.vue onMounted)에서 1회 호출한다(installPushTokenRefreshHandler 와 동형).
 */
export function installPushOpenedHandler() {
  if (typeof window === 'undefined') return

  window.__onPushOpened = (payload) => {
    const data = normalizePayload(payload)
    if (!data || typeof data !== 'object' || !data.type) {
      console.warn('[pushRouteBridge] 유효하지 않은 푸시 payload → 무시')
      return
    }
    routeByPushType(data)
  }
}

export default {
  installPushOpenedHandler,
  TRANSFER_NOTICE_OPEN_EVENT,
}
