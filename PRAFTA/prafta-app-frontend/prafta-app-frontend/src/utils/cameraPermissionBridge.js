//
// 네이티브(Flutter 셸) REQUEST_CAMERA_PERMISSION 브리지 래퍼.
//
// 웹뷰 QR 스캐너가 getUserMedia 를 부르기 "전"에 네이티브 카메라 권한을 확인/요청한다.
//
// 왜 필요한가 (2026-08-01 갤럭시 실측):
//   안드로이드 웹뷰는 네이티브 CAMERA 권한이 없을 때 getUserMedia 를
//   NotAllowedError(권한 거부)가 아니라 NotReadableError("Could not start video
//   source")로 실패시킨다. 에러명만으로는 "권한 거부"와 "카메라 점유"를 구분할 수
//   없어 사용자 안내가 불가능하므로, 스캐너 시작 전에 이 브리지로 권한을 선확인한다.
//   (당시 증상: 권한 미허용 상태에서 스캐너 화면 전체가 원인 안내 없이 검게 표시)
//
// 응답 계약(네이티브와 동일): {status:'GRANTED'|'DENIED'|'PERMANENTLY_DENIED'}
//
// 비즈니스 로직 금지 — 권한 확인/요청만 담당한다.
//

import { isKnownMissing } from '@/utils/shellCapability'

// 브리지 응답 대기 타임아웃(ms). hang(이론적 케이스)에서만 발동한다.
const CALL_TIMEOUT_MS = 3000

// 타임아웃 판별용 센티널(네이티브 응답과 절대 충돌하지 않는 고유 객체).
const TIMEOUT_SENTINEL = Object.freeze({})

function isBridgeAvailable() {
  return (
    typeof window !== 'undefined' &&
    window.flutter_inappwebview &&
    typeof window.flutter_inappwebview.callHandler === 'function'
  )
}

/**
 * 네이티브 카메라 권한을 확인하고, 미허용이면 요청한다(OS 프롬프트 노출 가능).
 *
 * @returns {Promise<'GRANTED'|'DENIED'|'PERMANENTLY_DENIED'|'UNAVAILABLE'>}
 *   UNAVAILABLE = 브리지 없음(PC 브라우저) 또는 구버전 셸(핸들러 미등록 → null 응답).
 *   호출부는 UNAVAILABLE 이면 선확인 없이 그대로 진행한다(기존 동작 보존).
 */
export async function requestNativeCameraPermission() {
  if (!isBridgeAvailable()) {
    console.log('[cameraPermissionBridge] 브리지 없음 → 선확인 생략')
    return 'UNAVAILABLE'
  }

  // 셸이 이 핸들러를 모른다고 선언(원격 Vue + 구버전 셸 스큐) → 호출 없이
  // 기존 null 응답 경로와 동일하게 선확인 생략.
  if (isKnownMissing('REQUEST_CAMERA_PERMISSION')) {
    console.log('[cameraPermissionBridge] 셸 미지원 핸들러 선언 → 선확인 생략')
    return 'UNAVAILABLE'
  }

  try {
    // callHandler 호출과 타임아웃 레이스(gpsBridge 패턴). hang 시에만 발동한다.
    const callPromise = window.flutter_inappwebview.callHandler('REQUEST_CAMERA_PERMISSION')
    const timeoutPromise = new Promise((resolve) => {
      setTimeout(() => resolve(TIMEOUT_SENTINEL), CALL_TIMEOUT_MS)
    })
    const res = await Promise.race([callPromise, timeoutPromise])
    if (res === TIMEOUT_SENTINEL) {
      // 타임아웃 = 기존 null 응답 경로와 동일 의미(선확인 생략 후 진행).
      console.log('[cameraPermissionBridge] 응답 타임아웃 → 선확인 생략')
      return 'UNAVAILABLE'
    }
    const status = res && res.status
    if (status === 'GRANTED' || status === 'DENIED' || status === 'PERMANENTLY_DENIED') {
      return status
    }
    // 구버전 셸: 미등록 핸들러 호출은 null 로 resolve 된다(플러그인 동작 실측).
    console.log('[cameraPermissionBridge] 알 수 없는 응답(구버전 셸 추정) → 선확인 생략')
    return 'UNAVAILABLE'
  } catch (e) {
    console.log(`[cameraPermissionBridge] callHandler 실패: ${e && e.message}`)
    return 'UNAVAILABLE'
  }
}
