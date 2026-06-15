// src/utils/fileUrl.js (APP)
// 업로드 파일 서빙 URL 빌더.
//
// 백엔드 정적 서빙은 루트 '/uploads/**' 에 마운트된다(ApiPrefixConfig). API 의 '/prafta'
// 컨텍스트와 무관하므로 이미지 URL 은 컨텍스트 없는 "호스트"에 붙여야 한다.
//
// 서빙 파일명은 반드시 확장자 포함명(fileName = FILE_MGMT_CD + FILE_EXT, 예: 002-...-00028.jpg)을
// 사용한다. fileMgmtCd(확장자 없음)로 URL 을 만들면 정적 서빙 핸들러가 디스크 파일을 찾지 못해(404)
// 사진이 안 보인다(웹 NearMissInfo.vue 와 동일 규칙).
//
// filePath(DB 값)는 '\uploads\001\...\002' 처럼 백슬래시 디렉터리 경로이므로 슬래시 정규화한다.

// '/uploads' 정적 서빙은 컨텍스트(/prafta) 밖이므로 호스트만 필요(baseUrl.resolveBaseURL 과 달리 컨텍스트 미부착).
const resolveFileHost = () => {
  if (typeof window !== 'undefined' && window.__APP_BASE_URL__) {
    return window.__APP_BASE_URL__
  }
  const cfg = (typeof window !== 'undefined' && window.__APP_CONFIG__) || {}
  if (cfg.API_BASE) {
    return cfg.API_BASE
  }
  // 패키징(file://) 빌드: 절대 URL 필요(vite 프록시 없음).
  if (typeof window !== 'undefined' && window.location?.protocol === 'file:') {
    return import.meta.env.VITE_FILE_API_BASE || 'http://172.30.1.4:8080'
  }
  // dev/운영 웹: 동일 출처 상대경로(/uploads 는 원점 기준).
  return ''
}

/**
 * 업로드 파일 서빙 URL 생성.
 * @param {string} filePath - DB FILE_PATH (예: '\uploads\001\20260614\00001\002')
 * @param {string} fileName - 확장자 포함 파일명 (예: '002-20260614-00028.jpg')
 * @returns {string} 서빙 가능한 절대/상대 URL. 입력이 비면 '' 반환.
 */
export const buildFileUrl = (filePath, fileName) => {
  if (!filePath || !fileName) return ''

  // 이미 절대 URL 이면 파일명만 붙인다.
  if (/^https?:\/\//i.test(filePath)) {
    return `${filePath.replace(/\/+$/, '')}/${fileName}`
  }

  const normalizedPath = String(filePath).replace(/\\/g, '/')
  const cleanPath = normalizedPath.startsWith('/') ? normalizedPath : `/${normalizedPath}`

  const host = resolveFileHost()
  if (host) {
    const base = host.endsWith('/') ? host.slice(0, -1) : host
    return `${base}${cleanPath}/${fileName}`
  }
  // 호스트 미상(dev 웹): 원점 기준 상대경로.
  return `${cleanPath}/${fileName}`
}
