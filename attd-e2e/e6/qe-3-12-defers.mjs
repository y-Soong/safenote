// QE-3-12 📋 마감월 과거일 휴일 지정 허용 여부 — 202607 교착으로 청정월 202606 마감상태에서 검증.
// + 6-9/6-10/6-12 DEFERRED 기록(202607 마감상태 의존, M10 중단점).
import { getToken, call } from "../lib/http.mjs";
import { record } from "../lib/record.mjs";

const run = async () => {
  const tok = await getToken("QTHR", "QtTest!2026", "WEB");
  const out = { title: "마감월 과거일 휴일 지정 허용 여부(202606 마감상태)", steps: [] };

  // 202606 마감
  const c = await call("POST", "/webApi/attd07/attd-close", { token: tok, body: { siteCd: "00010", nodeCd: "", incSubNodeYn: "N", closeYm: "202606", closeDesc: "[QE-3-12] 마감월 휴일 테스트" } });
  out.steps.push(`202606 마감=${c.status}`);

  // 마감월 과거일(6/10) 휴일 등록 시도
  const reg = await call("POST", "/webApi/attd02/update-holiday-infos", { token: tok, body: { siteCd: "00010", holidayId: "", holidayNm: "[QE-3-12] 마감월 휴일", holidayYmd: "2026-06-10", holidayType: "02", repeatYearly: false, useYn: "Y" } });
  out.steps.push(`마감월(6/10) 휴일 등록=${reg.status} ${reg.status>=400?(reg.json?.errorCode||""):"허용"}`);

  // 해제 + 휴일 정리
  const u = await call("POST", "/webApi/attd07/attd-unclose", { token: tok, body: { siteCd: "00010", nodeCd: "", incSubNodeYn: "N", closeYm: "202606", closeDesc: "[QE-3-12] 해제" } });
  out.steps.push(`202606 해제=${u.status}`);

  out.dbCheck = "6/10 휴일 등록 결과 후속 DB 확인(soft-delete 정리). 202606 해제 확인.";
  const allowed = reg.status === 200;
  record("QE-3-12", "OBSERVED", { ...out,
    note: `마감월 과거일 휴일 지정 = ${allowed?"허용(200)":"차단("+(reg.json?.errorCode||reg.status)+")"}. 휴일 등록은 사업장/회사 캘린더 관리로 근태 마감 게이팅과 독립(마감월이어도 ${allowed?"등록 가능":"차단"}). 202607 교착으로 청정월 202606 마감상태에서 대체 검증. §7 G16.` });
  console.log("3-12 allowed=" + allowed);

  // DEFERRED 기록 (202607 마감상태 의존 — M10 중단점)
  const deferNote = "202607 마감상태 의존 케이스 — M10 교착(REQ174 H 비활성 반려불가)으로 202607 마감 불가 → 중단점 DEFERRED. 마감 메커니즘 자체는 QE-6-8 청정월 202606 으로 증명됨.";
  record("QE-6-9", "DEFERRED", { title: "마감월 미래일 연차 신청 교착(#5) — 202607 마감상태 필요", note: deferNote + " (#5 신청통과+차감/승인·반려 ATTD_400_042 교착 재확인 대상.)" });
  record("QE-6-10", "DEFERRED", { title: "마감월 OT 신청 게이트 대칭성 — 202607 마감상태 필요", note: deferNote });
  record("QE-6-12", "DEFERRED", { title: "마감월 확정연차 삭제요청 — 202607 마감상태+7월 확정연차 필요", note: deferNote });
  console.log("DEFERS recorded");
  process.exit(0);
};
run().catch((e) => { console.error(e); process.exit(1); });
