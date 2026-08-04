// /src/utils/shellCapability.js
//
// 셸 능력(capability) 탐지 유틸 — 원격 호스팅 전환(T2) 대응.
//
// ★ 기존 래퍼들의 isBridgeAvailable() 과 역할이 다르다:
//   - isBridgeAvailable() = "웹뷰 브리지 객체(window.flutter_inappwebview) 존재" 판별
//     → 브라우저 vs 웹뷰 구분.
//   - isKnownMissing()    = "셸이 이 핸들러를 모른다고 명시적으로 선언" 판별
//     → 원격 Vue(최신) + 구버전 셸 조합의 브리지 버전 스큐 대응.
//
// 셸 계약(T1, document-start 주입 예정):
//   window.__SHELL__ = {
//     bridgeVersion: 1,            // 현행 8핸들러 세트 = v1 기준선
//     platform: 'android'|'ios',
//     appVersion: '1.0.0',
//     handlers: ['JS_CONSOLE','GET_GPS', ...],
//   }
//
// 무회귀 원칙: __SHELL__ 이 없는 셸(현행 전 버전)에서는 getShellInfo()=null,
// hasHandler()=null, isKnownMissing()=false 가 되어 기존 경로가 그대로 유지된다.
// (사전 감사 T0: flutter_inappwebview ^6.0.0 은 미등록 핸들러 호출 시 null 로
//  resolve 하므로, 구셸에서 그대로 호출해도 각 래퍼의 기존 폴백이 동작한다.)
//

/**
 * 셸이 주입한 __SHELL__ 정보를 반환한다.
 * 원격 페이지는 셸 전용 진입점이므로 검증은 형태 체크 수준(객체 + handlers 배열)만 한다.
 *
 * @returns {{bridgeVersion:number, platform:string, appVersion:string, handlers:string[]}|null}
 *   유효한 __SHELL__ 이 없으면(구버전 셸/브라우저) null.
 */
export function getShellInfo() {
  if (typeof window === 'undefined') return null
  const shell = window.__SHELL__
  if (!shell || typeof shell !== 'object' || !Array.isArray(shell.handlers)) return null
  return shell
}

/**
 * 셸이 해당 핸들러를 지원하는지 3상으로 반환한다.
 *
 * @param {string} name 핸들러명(예: 'SCAN_QR')
 * @returns {boolean|null}
 *   - true:  셸이 지원한다고 선언.
 *   - false: 셸이 handlers 목록을 줬는데 해당 핸들러가 없음(미지원 확정).
 *   - null:  알 수 없음(__SHELL__ 미주입 = 구버전 셸 또는 브라우저).
 */
export function hasHandler(name) {
  const shell = getShellInfo()
  if (!shell) return null
  return shell.handlers.includes(name)
}

/**
 * "셸이 이 핸들러를 모른다"고 확정된 경우에만 true.
 * null(구버전 셸 — 알 수 없음)은 false — 구셸에서는 기존 호출 경로를 그대로 탄다(무회귀 원칙).
 *
 * @param {string} name 핸들러명
 * @returns {boolean}
 */
export function isKnownMissing(name) {
  return hasHandler(name) === false
}

export default {
  getShellInfo,
  hasHandler,
  isKnownMissing,
}
