/**
 * API 에러 메시지 공통 처리 유틸 (web 프론트 @/utils/apiError 와 동일)
 *
 * 폴백 순서
 *  1) err.response.data.message  — 백엔드 메시지 최우선
 *  2) fallbackMessage            — 백엔드 메시지가 없을 때 화면이 넘긴 기본 메시지
 *
 * axios 원시 메시지("Network Error" 등)는 사용자에게 노출하지 않는다.
 *
 * @param {*} err axios catch 블록에서 받은 에러 객체
 * @param {string} fallbackMessage 백엔드 메시지가 없을 때 표시할 화면별 기본 메시지
 * @returns {string} 사용자에게 표시할 에러 메시지
 */
export function resolveApiErrorMessage(err, fallbackMessage) {
  const backendMessage = err?.response?.data?.message
  if (typeof backendMessage === 'string' && backendMessage.trim() !== '') {
    return backendMessage
  }
  return fallbackMessage
}
