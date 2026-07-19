import { record } from "../lib/record.mjs";
record("QE-6-7", "OBSERVED", {
  title: "OPEN 슬롯 보유자 비활성 + 모니터링",
  webView: "OPEN 슬롯은 마감 3대 차단조건(대기요청/GPS미확인/미승인OT)에 미포함 — 코드 확인.",
  appView: "H OPEN 슬롯 시드 실패(2회차 출근 500). H 오늘 슬롯 CLOSED(10:33~10:36) 상태로 비활성.",
  dbCheck: "00010 OPEN 슬롯 0건. H 비활성 후 CLOSED 슬롯 잔존.",
  note: "OPEN 슬롯 보유자 비활성 시나리오는 2회차 출근 500(DEFECT E6-1)로 시드 불가. 코드상 OPEN 슬롯은 마감 차단조건 미포함 → 마감이 OPEN 슬롯을 차단하지도 강제마감하지도 않음(무관). 6-8⑤ 재확인.",
});
record("QE-6-DEF-E6-1", "DEFECT", {
  title: "[신규 #E6-1] 같은날 2회차 출근(재출근) API 500 COMMON_500_000",
  expected: "당일 CLOSED 슬롯 존재 계정의 2회차 출근은 신규 슬롯 생성 또는 명확한 4xx 가드",
  actual: "POST /appApi/attd01/check-in 2회차 호출 시 500 COMMON_500_000. H·A 재현. 슬롯 미생성(클린 롤백).",
  dbCheck: "A/H 당일 근태행 불변(부수효과 0). 데이터 오염 없음, 응답만 5xx.",
  note: "조건: 당일 CLOSED 슬롯 존재 + 오늘=제헌절(휴일). 앱 '출근하기(2회차)' 경로 서버 정합성 결함 후보. 2계정 재현. 날짜조작 불가로 휴일요인 격리 미완. 5xx 규칙상 DEFECT 채번.",
});
console.log("recorded");
