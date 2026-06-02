/**
 * Attd(근태) 관련 메시지
 */
export const ATTD_MESSAGES = {
  APPLY_DATE_FUTURE: "수정 시 적용일은 내일 이후만 선택 가능합니다.",
  APPLY_DATE_FUTURE_ONLY: "적용일은 내일 이후만 선택 가능합니다.",
  LEAVE_SAVE_CONFIRM:
    "저장 후에는 아래 항목을 제외한 나머지 항목을\n수정할 수 없습니다.\n\n[연차명, 사용여부, 비고, 증빙 안내 문구]\n\n저장하시겠습니까?",
  LEAVE_HISTORY_PREPARING: "{leaveNm} 변경이력 기능은 준비 중입니다.",
  HOLIDAY_NAME_REQUIRED: "휴일명을 입력해주세요.",
  HOLIDAY_DATE_REQUIRED: "일자를 선택해주세요.",
  LEAVE_DELETE_CONFIRM: "해당 휴무를 삭제하시겠습니까?",
  YEAR_RANGE: "년도는 2000~2100 사이로 입력해주세요.",
  MONTH_RANGE: "월은 1~12 사이로 선택해주세요.",
  SHIFT_DAY_REQUIRED: "A조의 모든 Day를 선택해주세요.",

  // ── Attd_05 (근무계획관리 dirty 저장 / 셀 비우기, PRAFTA-041) ──
  SCHEDULE_NO_CHANGE: "변경된 내용이 없습니다.",
  SCHEDULE_CLEAR_SELECT_REQUIRED: "지울 영역을 선택해주세요.",

  // ── Attd_07 (월별 근태) ────────────────────────────────────
  SITE_REQUIRED_FIRST: "사업장을 먼저 선택해 주세요.",
  SITE_INPUT_REQUIRED: "사업장을 입력해 주세요.",
  MONTH_CLOSE_BLOCKED: "처리 필요 항목이 남아 있어 마감할 수 없습니다.",
  SEARCH_ERROR_DEFAULT: "조회 오류",
  SEARCH_ERROR: "조회 중 오류가 발생했습니다.",
  ISSUE_LIST_PREPARING: "[AttdIssueListPop] 추후 구현",
  MONTH_CLOSE_PREPARING: "[AttdMonthClosePop] 추후 구현",
  EXCEL_UPLOAD_PREPARING: "[AttdExcelUploadPop] 추후 구현",

  // ── AttdDayDetailPop (일자 상세) ───────────────────────────
  SEG_CHECKIN_REQUIRED: "{idx}구간 출근 일자/시간을 입력해 주세요.",
  SEG_CHECKOUT_REQUIRED: "{idx}구간 퇴근 일자/시간을 입력해 주세요.",
  SEG_TIME_FORMAT: "{idx}구간 시간은 4자리(HHMM)로 입력해 주세요.",
  SEG_OUT_DATE_BEFORE_IN: "{idx}구간 퇴근 일자가 출근 일자보다 빠릅니다.",
  SEG_OUT_TIME_BEFORE_IN:
    "{idx}구간 퇴근 시간이 출근 시간보다 빠르거나 같습니다.",
  SEG2_IN_AFTER_SEG1_OUT:
    "2구간 출근 시간은 1구간 퇴근 시간보다\n늦어야 합니다.",
  DAY_NO_CHANGES: "수정된 데이터가 없습니다.",
  SAVE_ERROR: "저장 중 오류가 발생했습니다.",
  REASON_REQUIRED: "사유를 입력해 주세요.",
  DELETE_ERROR: "삭제 중 오류가 발생했습니다.",
  FORM_RESET_CONFIRM:
    "변경한 내용을 모두 초기화하시겠습니까?\n팝업을 불러온 시점의 값으로 되돌립니다.",

  // ── 근로자 요청 (monthlyAttdReqResultList) ──────────────
  REQ_APPROVE_CONFIRM: "해당 요청을 승인하시겠습니까?",
  REQ_REJECT_CONFIRM: "해당 요청을 반려하시겠습니까?",
  REQ_DIRECT_EDIT_FILLED:
    "요청 값이 직접 수정 폼에 적용되었습니다.\n사유를 입력하고 저장해 주세요.",
  REQ_ACTION_PREPARING: "요청 승인/반려 처리는 추후 구현 예정입니다.",
  // 승인 시 form.reason 에 자동 채워지는 값
  REQ_APPROVED_REASON: "사용자 요청 승인",
  // 승인 처리하려는데 해당 일자가 수정 불가능한 상태일 때
  REQ_APPROVE_NOT_EDITABLE:
    "현재 일자는 직접 수정이 불가능한 상태입니다.\n승인 처리를 진행할 수 없습니다.",

  // ── 추가근무 (PRAFTA-003) ────────────────────────────────
  OT_SAVE_CONFIRM: "추가근무를 저장하시겠습니까?",
  OT_LIST_EMPTY: "저장할 추가근무 내역이 없습니다.",
  OT_RANGE_INVALID:
    "추가근무 시작/종료 시간이 올바르지 않습니다.\n시작 시간이 종료 시간보다 빨라야 합니다.",
  OT_OUTSIDE_ALLOWED:
    "추가근무 시간이 등록 가능 범위를 벗어났습니다.\n표시된 등록 가능 시간 안에서만 등록할 수 있습니다.",
  OT_OVERLAP:
    "추가근무 구간이 서로 겹칩니다.\n시간이 겹치지 않도록 수정해 주세요.",
  OT_SAVE_ERROR: "추가근무 저장 중 오류가 발생했습니다.",
};
