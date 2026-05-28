// src/api/baseUrl.js (APP)
// baseURL 결정 규칙. web 프론트(@/api/baseUrl)와 동일한 정책을 사용한다.
export const resolveBaseURL = () => {
  const cfg = (typeof window !== 'undefined' && window.__APP_CONFIG__) || {}
  if (cfg.API_BASE) {
    const context = cfg.CONTEXT ?? '/prafta'
    return `${cfg.API_BASE}${context}`
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
