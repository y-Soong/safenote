//
// html5-qrcode 스캐너를 "후면 카메라 + 타임아웃 + 실패 사유 분류"로 시작하는 공용 유틸.
// (QrScanner.vue / AdminSiteOpsView.vue 공용 — 2026-08-01 갤럭시 검은 화면 건 후속)
//
// 기존 방식의 문제 3가지를 한 번에 제거한다:
//   1) getCameras() 로 목록을 얻고 라벨 정규식으로 후면을 고르던 방식은
//      - 카메라를 두 번 연다(getCameras 가 내부적으로 getUserMedia 1회) → 안드 재오픈 레이스 소지
//      - 권한 미허용이면 라벨이 빈 문자열이라 후면 탐색이 조용히 실패하고 devices[0](전면)로
//        떨어진다(08-01 실측: REJECT 로그가 전면 카메라 "1" 로 찍힘)
//      → facingMode 제약으로 브라우저가 직접 후면을 고르게 한다(카메라는 한 번만 연다).
//   2) start() 가 실패 시 reject 대신 hang 으로 끝나는 경우가 있어(08-01 실측)
//      catch 미도달 → 폴백 화면이 안 뜨고 검은 화면만 남는다 → 타임아웃으로 반드시 결말을 낸다.
//   3) 실패가 전부 같은 안내로 뭉개진다 → 사유(reason)를 분류해 던진다.
//
// iOS(WKWebView)/안드 웹뷰 공통 동작:
//   - facingMode:'environment' 는 양쪽 모두 표준 지원. iPhone 은 후면 카메라가 항상 있어
//     exact 제약이 안전하고, exact 를 지원하지 못하는 특이 기기는 비강제 재시도로 흡수한다.
//   - 타임아웃 후 뒤늦게 열린 카메라 세션은 즉시 stop 해 유령 점유를 막는다.
//

/** 실패 사유 — 폴백 화면(SafetyCameraPermissionView)의 reason prop 과 동일 어휘. */
export const CAMERA_FAIL = {
  DENIED: 'denied', // 권한 거부 → 설정 이동 안내
  BUSY: 'busy', // 카메라 점유/개방 실패 → 다른 앱 종료 후 재시도 안내
  ERROR: 'error', // 타임아웃 포함 그 외 → 재시도 안내
}

// ★html5-qrcode 는 getUserMedia 실패를 DOMException 그대로가 아니라
//   "Error getting userMedia, error = <원본 toString>" 문자열로 감싸 reject 한다
//   (src/html5-qrcode.ts errorGettingUserMedia). err.name 이 소실되므로 분류는
//   name 검사와 "문자열 안에 박힌 에러명/문구" 매칭을 병행해야 한다.
const errName = (err) => (err && err.name) || ''
const errText = (err) => String((err && err.message) || err || '')

/** getUserMedia 계열 실패를 사용자 안내 가능한 사유로 분류한다. */
const toFailReason = (err) => {
  const name = errName(err)
  const text = errText(err)
  // 권한 거부 — "not allowed" 는 iOS WebKit 문구("not allowed by the user agent") 대응.
  if (
    name === 'NotAllowedError' ||
    name === 'PermissionDeniedError' ||
    /NotAllowedError|PermissionDenied|permission|not allowed/i.test(text)
  ) {
    return CAMERA_FAIL.DENIED
  }
  // NotReadableError/AbortError: 다른 프로세스가 점유 중이거나 HAL 개방 실패.
  // ★안드 웹뷰는 "네이티브 권한 부재"도 NotReadableError 로 보고하므로(08-01 실측),
  //   권한 판정은 이 분류가 아니라 사전의 REQUEST_CAMERA_PERMISSION 브리지가 담당한다.
  if (
    name === 'NotReadableError' ||
    name === 'TrackStartError' ||
    name === 'AbortError' ||
    /NotReadableError|TrackStartError|AbortError|video source|in use|busy|hardware/i.test(text)
  ) {
    return CAMERA_FAIL.BUSY
  }
  return CAMERA_FAIL.ERROR
}

const isOverconstrained = (err) =>
  errName(err) === 'OverconstrainedError' || /overconstrained/i.test(errText(err))

/**
 * start() 1회 시도 + 타임아웃. 타임아웃 이후 뒤늦게 성공한 세션은 정리(stop)한다.
 * hang(결말 없는 대기)을 사용자에게 보이는 실패로 반드시 전환하기 위한 래퍼.
 */
const startWithTimeout = (scanner, constraints, config, onSuccess, onFrameError, timeoutMs) =>
  new Promise((resolve, reject) => {
    let timedOut = false
    const timer = setTimeout(() => {
      timedOut = true
      const e = new Error(`카메라 시작 응답 없음 (${timeoutMs}ms)`)
      e.reason = CAMERA_FAIL.ERROR
      reject(e)
    }, timeoutMs)

    scanner.start(constraints, config, onSuccess, onFrameError).then(
      () => {
        clearTimeout(timer)
        if (timedOut) {
          // 이미 실패로 처리된 뒤 뒤늦게 열린 유령 세션 → 즉시 정리(카메라 점유 방지).
          scanner
            .stop()
            .then(() => scanner.clear())
            .catch(() => {})
          return
        }
        resolve()
      },
      (err) => {
        clearTimeout(timer)
        if (!timedOut) reject(err)
      },
    )
  })

/**
 * 후면 카메라로 스캐너를 시작한다.
 *
 * @param {import('html5-qrcode').Html5Qrcode} scanner 미시작 인스턴스
 * @param {object} config html5-qrcode 설정 ({fps} 등)
 * @param {Function} onSuccess 스캔 성공 콜백
 * @param {Function} onFrameError 프레임별 인식 실패 콜백(보통 무시)
 * @param {{timeoutMs?: number}} [opts]
 * @throws {Error} 실패 시 err.reason 에 CAMERA_FAIL 값이 실린다.
 */
export async function startBackCameraScan(
  scanner,
  config,
  onSuccess,
  onFrameError,
  { timeoutMs = 8000 } = {},
) {
  try {
    try {
      // 1차: 후면 강제. 브라우저가 페이싱 메타데이터로 직접 고른다(라벨 문자열 의존 제거).
      await startWithTimeout(
        scanner,
        { facingMode: { exact: 'environment' } },
        config,
        onSuccess,
        onFrameError,
        timeoutMs,
      )
    } catch (err) {
      // exact 미충족(후면 페이싱을 보고하지 않는 특이 기기)만 비강제로 1회 재시도.
      // 그 외 실패는 재시도해도 같은 이유로 실패하므로 그대로 올린다.
      if (!isOverconstrained(err)) throw err
      await startWithTimeout(
        scanner,
        { facingMode: 'environment' },
        config,
        onSuccess,
        onFrameError,
        timeoutMs,
      )
    }
  } catch (err) {
    const fail = err instanceof Error ? err : new Error(errText(err))
    if (!fail.reason) fail.reason = toFailReason(err)
    throw fail
  }
}
