import { record } from "../lib/record.mjs";
record("F1-LIVE", "PASS", {
  title: "F1 사용자 비활성 시 대기요청 자동종결 라이브검증 (재빌드 반영 확인)",
  expected: "D(20260700032) 비활성 시 D 신청 대기요청 자동종결, 타계정 대기건 불변, 원장 정합, 마감 미결 감소",
  actual: "REQ176(D신청,01)→04(취소) PROCESS_COMMENT='신청자/결재자 비활성으로 자동 반려'; REQ177(A신청)·controls 171/172/173/174 전부 01 불변; D USE_YN=N; 마감 pendingReqCnt 3→2(D건 제거); 원장 불일치 0, D grant g15/u1 불변",
  webView: "update-user-infos 200 successCount=1; close-status pendingReqCnt 3→2",
  appView: "D/A appApi/req07/attd-correction 201 CREATED (176/177)",
  dbCheck: "REQ176 04 자동취소·actor=QTHR; 대상자건만 전이(타계정 불변); GRANT.USED_DAYS=SUM(CONFIRMED) 불일치 0",
  note: "신청자방향=PASS(F1 재빌드 실반영 확인, 신청자건은 취소'04'/결재자건은 반려'03' 설계). 결재자방향=미검증(BLOCKED): QT노드 n1 SELF_ATTD_APPRV_YN='Y'로 결재선이 붕괴되어 approverUserCds=[D]가 무시됨(TB_USER_ATTD_REQ_APPROVAL 행 0)→ D를 결재자로 배치할 라이브 셋업 불가. 결재자방향 코드경로는 소속이동 발효와 단일출처(E6 검증분) 공유. 마감뚫림=PASS. 원장정합=PASS. 잔여 아티팩트: REQ177(A활성 대기건) 존치, QTUSERD USE_YN=N 존치.",
});
console.log("RECORDED");
