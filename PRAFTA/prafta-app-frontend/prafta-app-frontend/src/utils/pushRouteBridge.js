// /src/utils/pushRouteBridge.js
//
// PRAFTA-WEB_001-5: 푸시 알림 "탭(open)" 라우팅 브리지.
//
// Flutter 셸이 알림 탭(onMessageOpenedApp / 콜드스타트 getInitialMessage) 시
// window.__onPushOpened(<DATA_PAYLOAD>) 를 호출한다(push 모델, pushTokenBridge 의
// window.__onPushTokenRefresh 와 동일 계약 방향). 본 모듈은 그 콜백을 전역 1회 등록하고,
// DATA_PAYLOAD 의 type 에 따라 해당 화면으로 라우팅한다.
//
// DATA_PAYLOAD 계약(백엔드 각 *NotiServiceImpl 의 buildPayload):
//   모든 payload 는 { type: '<NOTI_TYPE>' } 를 필수로 갖고, 유형별 식별자를 덧붙인다.
//     TRANSFER_RESERVED        : reservationId
//     SELFJOIN_PENDING         : siteCd
//     ATTD_APPROVAL_TURN/REQUEST: reqId, approvalStep, applicantUserCd
//     LEAVE_APPROVAL_TURN      : reqId, approvalStep, applicantUserCd
//     NEAR_MISS_REPORTED       : nearMissId, potentialSeverityCd
//     TBM_STARTED/COMPLETED    : sessionCd
//     RISK_ASSESS_REQUESTED    : assessmentCd
//     ATTD_LATE_EARLY_DETECTED : event, workerUserCd, workYmd, attdId
//     *_RESULT_*               : reqId, reqType
//   라우팅 대상은 아래 PUSH_ROUTE_MAP 이 단일 출처다.
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

// 문자열 query 값만 추출(값 없으면 키 자체를 넣지 않는다 — 빈 문자열 query 오염 방지).
function pickQuery(data, keys) {
  const query = {}
  keys.forEach((k) => {
    const v = data[k]
    if (v !== undefined && v !== null && String(v) !== '') query[k] = String(v)
  })
  return query
}

/**
 * NOTI_TYPE → 이동 대상 라우트 매핑(단일 출처).
 *
 * 값은 payload(data)를 받아 router.push 인자를 반환하는 함수다. 파라미터가 필요한 상세 화면은
 * 값이 없을 때 목록으로 폴백한다(상세가 빈 화면으로 뜨는 것 방지).
 *
 * ★여기에 없는 type 은 MainView 로 보낸다(사용자 확정 2026-08-19). 목적 화면이 불분명한
 *   LEAVE_USED_NO_APRV · LEAVE_REFUSAL_* · LEAVE_PROMOTION_* 등이 여기 해당한다.
 * ★TRANSFER_RESERVED 는 라우팅이 아니라 시트 재오픈 신호라서 이 표를 쓰지 않는다(별도 분기).
 */
const PUSH_ROUTE_MAP = {
  // ── 관리자 대상 ──
  // 승인 상세(/AdminApprovalDetail)는 reqId+group 을 함께 요구하는데 payload 에 group 이 없어
  //   목록으로 보낸다(사용자 확정 — 백엔드 payload 무수정).
  ATTD_APPROVAL_TURN: () => ({ path: '/AdminApproval' }),
  ATTD_APPROVAL_REQUEST: () => ({ path: '/AdminApproval' }),
  // 연차 결재는 payload(reqId·approvalStep)가 상세 화면 요구 파라미터와 정확히 일치한다.
  LEAVE_APPROVAL_TURN: (d) =>
    d.reqId
      ? { path: '/LeaveApprovalDetail', query: pickQuery(d, ['reqId', 'approvalStep']) }
      : { path: '/LeaveApproval' },
  LEAVE_CHANGE_REQUEST: () => ({ path: '/AdminLeaveChangeConfirm' }),
  LEAVE_CHANGE_RESPONSE: () => ({ path: '/AdminLeaveChangeConfirm' }),
  NEAR_MISS_REPORTED: (d) =>
    d.nearMissId
      ? { path: '/NearMissManageDetail', query: pickQuery(d, ['nearMissId']) }
      : { path: '/NearMissManageList' },
  RISK_ASSESS_REQUESTED: () => ({ path: '/AdminSafetyRisk' }),
  ATTD_LATE_EARLY_DETECTED: () => ({ path: '/AdminAttdDetail' }),

  // ── 근로자 대상 ──
  // 요청 결과는 근로자용 상세 라우트가 없어 목록까지만 보낸다(사용자 확정).
  LEAVE_RESULT_APPROVED: () => ({ path: '/MyRequests' }),
  LEAVE_RESULT_REJECTED: () => ({ path: '/MyRequests' }),
  ATTD_RESULT_APPROVED: () => ({ path: '/MyRequests' }),
  ATTD_RESULT_REJECTED: () => ({ path: '/MyRequests' }),
  LEAVE_CHANGE_CONFIRMED: () => ({ path: '/MyRequests' }),
  LEAVE_CHANGE_REJECTED: () => ({ path: '/MyRequests' }),
  LEAVE_GRANT_RECALLED: () => ({ path: '/MyLeaveSummaryView' }),
  LEAVE_DIRECT_SET: () => ({ path: '/MyLeaveSummaryView' }),
  SHIFT_SCH_CHANGED: () => ({ path: '/MyAttendance' }),
  // TBM 시작은 입실 화면(sessionCd 필수), 종료는 이미 끝난 세션이라 허브로 보낸다.
  TBM_STARTED: (d) =>
    d.sessionCd ? { path: '/TbmEntry', query: pickQuery(d, ['sessionCd']) } : { path: '/TbmHub' },
  TBM_COMPLETED: () => ({ path: '/TbmHub' }),
  // 출퇴근 리마인더는 홈 출퇴근 카드가 목적지다.
  ATTD_CHECKIN_REMINDER: () => ({ path: '/MainView' }),
  ATTD_CHECKOUT_REMINDER: () => ({ path: '/MainView' }),

  // 셀프가입 승인 대기 — 기존 동작 보존(payload siteCd 를 query 로 전달).
  //   ★siteCd 를 넘기지 않으면 서버가 토큰 gv_siteCd 로 폴백하는데, 수신자의 토큰 사업장과
  //     신청 사업장은 다를 수 있다(타 사업장 노드 관리자·master/hr 폴백 수신자) —
  //     "알림은 왔는데 목록에 그 신청이 없다"가 된다. 알림이 가리키는 사업장을 그대로 연다.
  //     siteCd 는 화면 조회 조건일 뿐이고 인가는 서버 2단 게이트가 판정하므로 신뢰 경계 문제 없음.
  SELFJOIN_PENDING: (d) =>
    d.siteCd
      ? { path: '/AdminSelfJoin', query: pickQuery(d, ['siteCd']) }
      : { path: '/AdminSelfJoin' },
}

// DATA_PAYLOAD.type 분기. 신규 트리거는 PUSH_ROUTE_MAP 에 항목을 추가한다.
//   ★진입 인가는 서버(access-context / EP 게이트)가 최종 판정한다. 비로그인 상태면
//     라우터 beforeEach 가 로그인 화면으로 보냈다가 로그인 후 목적 경로로 복귀시킨다(redirect 쿼리).
function routeByPushType(data) {
  // 소속이동만 라우팅이 아닌 "시트 재오픈 신호"라 별도 처리(기존 동작 보존).
  if (data.type === 'TRANSFER_RESERVED') {
    openTransferNotice()
    return
  }

  const resolve = PUSH_ROUTE_MAP[data.type]
  // 매핑 없는 type → 메인 화면(사용자 확정). 향후 목적 화면이 정해지면 표에 추가한다.
  const target = resolve ? resolve(data) : '/MainView'
  if (!resolve) {
    console.log('[pushRouteBridge] 매핑 없는 푸시 type → MainView:', data.type)
  }
  navigateAfterReady(target, data.type)
}

/**
 * 라우터의 "초기 내비게이션이 끝난 뒤"에 이동한다.
 *
 * <p>★콜드스타트 경합 방지(2026-08-19 실기기 실측): 셸은 onLoadStop 에서 __onPushOpened 를 호출하는데,
 *    그 시점에 라우터는 아직 초기 이동('/' → ensureAccessToken 네트워크 대기 → /MainView replace)을
 *    진행 중일 수 있다. 이때 곧바로 push 하면 뒤늦게 완료된 초기 이동이 목적지를 덮어써
 *    <b>항상 MainView 에서 멈추는</b> 증상이 된다(실패가 .catch 로 삼켜져 무증상).
 *    router.isReady() 는 초기 이동이 확정된 뒤 resolve 되므로 그 이후에 push 하면 덮어쓰기가 없다.
 *    (이미 준비됐으면 즉시 resolve — 백그라운드 복귀 경로에는 영향 없음.)
 */
function navigateAfterReady(target, notiType) {
  const go = () => {
    router
      .push(target)
      .then(() => {
        console.log('[pushRouteBridge] 라우팅 완료:', notiType, '→', targetLabel(target))
      })
      .catch((e) => {
        // best-effort: 실패해도 앱 기동/조작을 막지 않는다. 다만 원인 추적을 위해 남긴다.
        console.warn('[pushRouteBridge] 라우팅 실패:', notiType, targetLabel(target), e && e.message)
      })
  }
  try {
    if (typeof router.isReady === 'function') {
      router.isReady().then(go).catch(go)
    } else {
      go()
    }
  } catch (e) {
    console.warn('[pushRouteBridge] 라우팅 준비 실패(무시):', e && e.message)
  }
}

// 로그용 라벨(문자열 경로 / {path,query} 객체 모두 허용).
function targetLabel(target) {
  if (!target) return ''
  return typeof target === 'string' ? target : target.path || ''
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
