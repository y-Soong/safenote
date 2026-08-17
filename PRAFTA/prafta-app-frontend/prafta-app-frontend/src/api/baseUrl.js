// src/api/baseUrl.js (APP)
// baseURL 결정 규칙. web 프론트(@/api/baseUrl)와 동일한 정책을 사용한다.
export const resolveBaseURL = () => {
  // 네이티브 셸(Flutter)이 document-start 시점에 주입하는 운영 백엔드 절대 URL.
  // release APK 는 번들 자산을 http://localhost 로 서빙하므로 상대경로(/prafta)는
  // 백엔드가 아니라 번들 서버 자신을 가리킨다. 셸이 주입한 절대 URL 을 최우선으로 쓴다.
  // (Flutter: --dart-define=APP_BASE_URL=https://api... → window.__APP_BASE_URL__)
  if (typeof window !== 'undefined' && window.__APP_BASE_URL__) {
    const context = window.__APP_CONTEXT__ || '/prafta'
    return `${window.__APP_BASE_URL__}${context}`
  }

  const cfg = (typeof window !== 'undefined' && window.__APP_CONFIG__) || {}
  if (cfg.API_BASE) {
    const context = cfg.CONTEXT ?? '/prafta'
    return `${cfg.API_BASE}${context}`
  }

  // 셸 주입 유실 방어(2026-08-17 iOS 빌드130 실증): 셸 배포 형상(번들 로컬서버 http://localhost
  // 또는 원격 app.prafta.com)인데 __APP_BASE_URL__ 주입이 없으면, 상대경로(/prafta)는 백엔드가
  // 아니라 정적 서버 자신을 때려 전 API 가 죽는다(서버 로그에 요청 자체가 안 남는 증상).
  // 이 형상에서만 운영 API 절대주소로 폴백한다 — 주입이 정상이면 위 분기에서 이미 반환돼 미도달.
  if (typeof window !== 'undefined') {
    const { protocol, hostname } = window.location || {}
    const isBundleServer = protocol === 'http:' && hostname === 'localhost'
    const isRemoteHost = hostname === 'app.prafta.com'
    if (isBundleServer || isRemoteHost) {
      const apiBase = import.meta.env.VITE_BUNDLE_API_BASE || 'https://api.prafta.com'
      const context = import.meta.env.VITE_API_CONTEXT || '/prafta'
      return `${apiBase}${context}`
    }
  }

  // 패키징(file://) 빌드: 절대 URL 필요 (vite 프록시가 없음)
  if (typeof window !== 'undefined' && window.location?.protocol === 'file:') {
    const apiBase = import.meta.env.VITE_FILE_API_BASE || 'http://172.30.1.4:8080'
    const context = import.meta.env.VITE_API_CONTEXT || '/prafta'
    return `${apiBase}${context}`
  }

  // dev/운영 웹: 동일 출처 상대경로 → vite 프록시(/prafta)가 백엔드로 전달
  return '/prafta'
}
