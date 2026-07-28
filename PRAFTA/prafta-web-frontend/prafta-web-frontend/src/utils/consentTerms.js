// consentTerms.js — 약관 동의(선택약관) 관련 웹 공용 상수/헬퍼.
//   앱(prafta-app-frontend/src/utils/termsGate.js)의 동일 상수와 값을 맞춘다.

/**
 * 연동 회사 제3자 제공 동의 약관 ID(SYS008 '006').
 *
 * - 내 정보 팝업의 선택약관 토글에서 '철회(Y→N)' 확인 팝업 판별에 쓴다.
 * - 로그인 게이트(ThirdPartyConsentPop)는 서버가 상수로 고정하므로 클라가 termsId 를 보내지 않는다.
 *   (본 상수는 화면 분기 판별 전용 — 요청 본문에 싣지 말 것)
 */
export const THIRD_PARTY_CONSENT_TERMS_ID = "006";
