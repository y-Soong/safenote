/**
 * User(사용자) 관련 메시지
 */
export const USER_MESSAGES = {
  USER_SELECT_REQUIRED: "지정할 사용자를 선택해주세요.",
  SITE_USER_ONLY: "[{siteNm}] 사업장에 속한\n사용자만 지정할 수 있습니다.",
  REQUEST_FAILED: "요청처리에 실패했습니다.\n관리자에게 문의해주세요.",
  PHONE_VERIFY: "휴대폰 번호를 확인해주세요.",
  PHONE_AUTH_REQUIRED: "휴대폰 번호를 인증해주세요.",
  BIRTH_AUTH_REQUIRED: "생년월일을 인증해주세요.",
  USER_DELETE_CONFIRM:
    "사용자의 계정을 삭제합니다.\n삭제된 계정은 복구할 수 없습니다.",
  USER_PW_RESET_CONFIRM: "사용자의 비밀번호를 초기화합니다.",

  // UserInfoPop
  USER_INFO_SMS_SENT: "인증번호가 발송되었습니다.",
  USER_INFO_SMS_VERIFIED: "인증번호가 확인되었습니다.",
  USER_INFO_CERT_NO_REQUIRED: "인증번호를 입력해주세요.",
  USER_INFO_PHONE_REQUIRED: "휴대폰번호를 입력해주세요.",
  USER_INFO_WITHDRAWAL_DATE_REQUIRED: "탈퇴예정일을 선택해주세요.",
  USER_INFO_WITHDRAWAL_DATE_CONFIRM:
    "{withdrawalDate} 에 회원탈퇴 처리됩니다.\n탈퇴예정일을 설정하시겠습니까?",
  USER_INFO_WITHDRAWAL_DATE_SET: "탈퇴예정일이 설정되었습니다.",
  USER_INFO_WITHDRAWAL_DATE_FAILED: "탈퇴예정일 설정 중 오류가 발생했습니다.",

  // MyInfoPop
  MY_INFO_CURRENT_PW_REQUIRED: "현재 비밀번호를 입력해주세요.",
  MY_INFO_NEW_PW_REQUIRED: "새 비밀번호를 입력해주세요.",
  MY_INFO_PW_MISMATCH: "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.",
  MY_INFO_PW_TOO_SHORT: "비밀번호는 8자 이상이어야 합니다.",
  MY_INFO_PW_CHANGE_CONFIRM: "비밀번호를 변경하시겠습니까?",
  MY_INFO_PW_CHANGED: "비밀번호가 변경되었습니다.",
  MY_INFO_PW_CHANGE_FAILED: "비밀번호 변경 중 오류가 발생했습니다.",
  MY_INFO_WITHDRAWAL_CONFIRM:
    "탈퇴하시면 모든 개인정보가 삭제되며 복구가 불가능합니다.\n정말 회원탈퇴 하시겠습니까?",
  MY_INFO_WITHDRAWAL_SUCCESS: "회원탈퇴가 완료되었습니다.",
  MY_INFO_WITHDRAWAL_FAILED: "회원탈퇴 처리 중 오류가 발생했습니다.",
};
