/**
 * Login/Terms 관련 메시지
 */
export const LOGIN_MESSAGES = {
  LOGIN_INPUT_REQUIRED: "아이디와 비밀번호를 모두 입력해주세요",
  TERMS_UPDATE_SUCCESS: "이용약관이 업데이트 됐습니다.",
  TERMS_REQUIRED: "필수약관 미동의 시 서비스 이용이 불가합니다.",
  TERMS_AGREEMENT_REQUIRED: "동의하지 않은 필수 약관이 있습니다.",
  TERMS_SELECT_REQUIRED: "이용약관을 선택해주세요.",

  // 연동 회사 제3자 제공 동의(006) 로그인 게이트 — 필수약관과 달리 미동의도 정상 통과(강제 아님).
  THIRD_PARTY_CONSENT_DISAGREE_CONFIRM:
    "동의하지 않아도 서비스 이용에는 제한이 없습니다.\n내 정보에서 언제든 동의로 변경할 수 있습니다.\n계속하시겠습니까?",
  THIRD_PARTY_CONSENT_FAILED: "동의 처리에 실패했습니다.\n잠시 후 다시 시도해주세요.",
};
