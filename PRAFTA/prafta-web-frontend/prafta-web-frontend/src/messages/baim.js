/**
 * Baim(사업장/조직) 관련 메시지
 * code - message 형태로 관리
 */
export const BAIM_MESSAGES = {
  ASSIGN_MANAGER_CONFIRM:
    "담당 정/부로 지정된 사용자의 소속 부서는\n해당 부서로 자동 변경됩니다.\n\n저장하시겠습니까?",
  // 저장 전 노드는 DB 에 없는 임시 코드라 담당자를 지정하면 소속이 유령 부서로 박힌다.
  ASSIGN_MANAGER_NEED_SAVE:
    "저장되지 않은 구성요소에는\n담당 정/부를 지정할 수 없습니다.\n\n먼저 저장한 뒤 지정해 주세요.",
  ORG_DATA_REQUIRED: "저장할 조직 데이터가 없습니다.",
  DELETE_NODE_CONFIRM:
    "해당 부서를 삭제하시겠습니까?\n\n( 주의 ! )\n해당 부서에 속한 모든 근로자의 소속이\n초기화 됩니다.",
  SITE_ALL_DELETE_CONFIRM:
    "[{siteNm}] 사업장의 조직도를\n일괄 삭제하시겠습니까?\n\n( 주의 ! )\n해당 사업장에 속한 모든 근로자의 소속이\n초기화 됩니다.",
  NODE_COPY_CONFIRM:
    "[{siteNm}] 사업장의 조직도를\n[{targetSiteNm}] 사업장 조직도로\n덮어쓰기 하시겠습니까?\n\n( 주의 ! )\n[{siteNm}] 사업장에 속한 모든 근로자의\n소속이 초기화 됩니다.",
  NODE_COPY_SUCCESS: "조직부서 복사가 완료되었습니다.",
  COM005_SORT_IDX_RANGE:
    "권한타입의 경우 권한등급은 3이상 999미만으로만 입력할 수 있습니다.",
};
