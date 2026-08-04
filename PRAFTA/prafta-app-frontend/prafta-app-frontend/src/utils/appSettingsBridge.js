//
// 네이티브(Flutter 셸) OPEN_APP_SETTINGS 브리지 래퍼.
//
// 웹뷰 안에서 window.flutter_inappwebview.callHandler('OPEN_APP_SETTINGS') 로
// OS 앱 설정 화면을 연다(deviceBridge / gpsBridge 패턴 동일).
//
// 왜 필요한가:
//   권한이 영구 거부되면 앱 안에서 다시 물을 수단이 없고(iOS 는 시스템 권한
//   프롬프트를 최초 1회만 띄운다) 설정 앱에서 직접 켜는 길밖에 없다. 그런데
//   웹뷰에서 `window.location = 'app-settings:'` 로는 열리지 않는다 — 네이티브
//   전용 스킴이라 WKWebView 가 네비게이션에 실패하며 화면이 로딩 상태로 멈춘다.
//   (실측: iOS 에서 '설정으로 이동' 버튼이 아무 반응 없이 로딩만 됨)
//
// 응답 계약(네이티브와 동일): {status:'OK'} | {status:'ERROR'}
//
// 비즈니스 로직 금지 — 설정 화면 호출만 담당한다.
//

import { isKnownMissing } from '@/utils/shellCapability'

// 브리지 응답 대기 타임아웃(ms). hang(이론적 케이스)에서만 발동한다.
const CALL_TIMEOUT_MS = 5000

function isBridgeAvailable() {
  return (
    typeof window !== 'undefined' &&
    window.flutter_inappwebview &&
    typeof window.flutter_inappwebview.callHandler === 'function'
  )
}

/**
 * OS 앱 설정 화면을 연다.
 *
 * @returns {Promise<boolean>} 열기 성공 여부. 브리지가 없는 환경(PC 브라우저 등)
 *   이거나 네이티브가 실패하면 false — 호출부는 이때 수동 안내를 노출한다.
 */
export async function openNativeAppSettings() {
  if (!isBridgeAvailable()) {
    // 웹 디버그 등 브리지 없음. app-settings: 스킴은 브라우저에서도 동작하지 않으므로
    // 대체 시도를 하지 않는다(로딩만 걸리는 증상의 원인이었다).
    console.log('[appSettingsBridge] 브리지 없음 → 설정 열기 불가')
    return false
  }

  // 셸이 이 핸들러를 모른다고 선언(원격 Vue + 구버전 셸 스큐) → 호출 없이 즉시 실패 처리.
  if (isKnownMissing('OPEN_APP_SETTINGS')) {
    console.log('[appSettingsBridge] 셸 미지원 핸들러 선언 → 설정 열기 불가')
    return false
  }

  try {
    // callHandler 호출과 타임아웃 레이스(gpsBridge 패턴). hang 시에만 발동한다.
    // 타임아웃은 null 로 resolve → 아래 status 판정에서 false(기존 실패 경로와 동일).
    const callPromise = window.flutter_inappwebview.callHandler('OPEN_APP_SETTINGS')
    const timeoutPromise = new Promise((resolve) => {
      setTimeout(() => resolve(null), CALL_TIMEOUT_MS)
    })
    const res = await Promise.race([callPromise, timeoutPromise])
    return !!res && res.status === 'OK'
  } catch (e) {
    console.log(`[appSettingsBridge] callHandler 실패: ${e && e.message}`)
    return false
  }
}
