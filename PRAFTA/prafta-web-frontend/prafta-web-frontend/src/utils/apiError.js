/**
 * API 에러 메시지 공통 처리 유틸 (prafta-008 / 작업 prafta-010-001)
 *
 * 목적
 *  - 백엔드 ApiException 이 직렬화한 에러 메시지(`response.data.message`)를
 *    사용자에게 일관되게 노출하기 위한 단일 진입점.
 *
 * 폴백 순서
 *  1) err.response.data.message  — 백엔드 메시지 최우선.
 *  2) fallbackMessage            — 백엔드 메시지가 없을 때 화면이 넘긴 기본 메시지.
 *
 * SEC-013 관련 결정 (prafta-008)
 *  - 과거 prafta-007 에서는 SEC-013(정보 누설 우려)로 일부 화면이 `response.data.message`를
 *    의도적으로 폐기하고 일반 메시지만 표시했다.
 *  - 그러나 백엔드 에러코드 enum(AttdErrorCode 등)이 민감한 코드에 대해서는 이미
 *    일반화된 메시지만 내려주도록 관리하고 있어 message 직접 노출 시 정보 누설이 없다.
 *  - 따라서 prafta-008 결정에 따라 백엔드 메시지를 그대로 노출하며,
 *    prafta-007 의 SEC-013 일반화 처리는 본 유틸 호출로 되돌린다.
 *
 * axios 원시 메시지 비노출
 *  - `err.message`("Network Error" 등 영문 원시 메시지)는 사용자에게 노출하지 않는다.
 *    네트워크 오류 등 백엔드 메시지가 없는 경우의 폴백은 화면이 넘긴 fallbackMessage 로 한정한다.
 *
 * @param {*} err axios catch 블록에서 받은 에러 객체.
 * @param {string} fallbackMessage 백엔드 메시지가 없을 때 표시할 화면별 기본 메시지.
 * @returns {string} 사용자에게 표시할 에러 메시지.
 */
export function resolveApiErrorMessage(err, fallbackMessage) {
  const backendMessage = err?.response?.data?.message;
  if (typeof backendMessage === "string" && backendMessage.trim() !== "") {
    return backendMessage;
  }
  return fallbackMessage;
}

/**
 * blob 응답(responseType: "blob") 전용 에러 메시지 처리.
 *
 * 왜 별도 함수가 필요한가
 *  - 스트림 EP(계약서 미리보기, 서명본 열람/다운로드 등)는 responseType 이 "blob" 이라
 *    에러 응답의 JSON 본문도 Blob 으로 도착한다. 이때 `err.response.data.message` 는
 *    항상 undefined 라서 resolveApiErrorMessage 는 무조건 폴백 문구만 반환한다.
 *  - 그 결과 서버가 원인을 정확히 내려줘도("계약서 원본 파일을 찾을 수 없습니다") 화면에는
 *    "미리보기 중 오류가 발생했습니다" 같은 일반 문구만 떠서 운영자가 원인을 알 수 없었다.
 *
 * 동작
 *  - 본문이 Blob 이면 텍스트로 읽어 JSON 파싱 후 `message` 를 사용한다.
 *  - Blob 이 아니거나(네트워크 오류 등) 파싱 실패면 기존 동기 로직/폴백으로 되돌아간다.
 *
 * @param {*} err axios catch 블록에서 받은 에러 객체.
 * @param {string} fallbackMessage 백엔드 메시지를 얻지 못했을 때 표시할 화면별 기본 메시지.
 * @returns {Promise<string>} 사용자에게 표시할 에러 메시지.
 */
export async function resolveBlobApiErrorMessage(err, fallbackMessage) {
  const data = err?.response?.data;
  if (typeof Blob !== "undefined" && data instanceof Blob) {
    try {
      const parsed = JSON.parse(await data.text());
      const backendMessage = parsed?.message;
      if (typeof backendMessage === "string" && backendMessage.trim() !== "") {
        return backendMessage;
      }
    } catch (e) {
      // 본문이 JSON 이 아니거나 읽기 실패 → 폴백 문구 사용.
    }
    return fallbackMessage;
  }
  return resolveApiErrorMessage(err, fallbackMessage);
}

/**
 * 회사 월간 AI 토큰 쿼터 소진(AI_429_001) 여부 — AI 화면 공통 alert 분기용.
 * (플랫폼-AI-토큰쿼터 §2-5: 소진 시 서버 message 를 Alert 모달로 우선 표출)
 *
 * @param {*} err axios catch 블록에서 받은 에러 객체.
 * @returns {boolean} 쿼터 소진 에러 여부.
 */
export function isAiQuotaExceeded(err) {
  return err?.response?.data?.errorCode === "AI_429_001";
}
