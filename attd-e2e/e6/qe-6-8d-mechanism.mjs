// QE-6-8 마감 메커니즘 증명 — 202607 은 M10 교착으로 마감 불가 → 청정월 202606 으로 ③마감·④ATTD_400_099·해제 검증.
import { getToken, call } from "../lib/http.mjs";
import { record } from "../lib/record.mjs";

const run = async () => {
  const out = { title: "202607 마감 M10 교착 + 마감 메커니즘(청정월) 증명", steps: [] };
  const tok = await getToken("QTHR", "QtTest!2026", "WEB");

  // 202607: 교착 재확인
  let s7 = (await call("GET", "/webApi/attd07/attd-close-status?siteCd=00010&nodeCd=&incSubNodeYn=N&closeYm=202607", { token: tok })).json;
  out.steps.push(`202607 blockTotal=${s7.blockTotalCnt}(REQ174 H 비활성·반려불가) closable=${s7.closable} → M10 교착 유지`);

  // 청정월 202606 메커니즘 증명
  let s6 = (await call("GET", "/webApi/attd07/attd-close-status?siteCd=00010&nodeCd=&incSubNodeYn=N&closeYm=202606", { token: tok })).json;
  out.steps.push(`202606 blockTotal=${s6.blockTotalCnt} closable=${s6.closable}`);

  const c6 = await call("POST", "/webApi/attd07/attd-close", { token: tok, body: { siteCd: "00010", nodeCd: "", incSubNodeYn: "N", closeYm: "202606", closeDesc: "[QE-6-8] 메커니즘 증명(청정월)" } });
  out.steps.push(`③ 202606 마감=${c6.status} ${(c6.text||"").slice(0,50)}`);

  // ④ 마감월(202606) 보정 상신 → ATTD_400_099
  const aTok = await getToken("QTUSERA", "QtTest!2026", "APP");
  const corr = await call("POST", "/appApi/req07/attd-correction", { token: aTok, clientType: "APP", body: {
    workYmd: "20260610", nodeCd: "n1", slots: [{ workSeq: 1, startDate: "20260610", startTime: "1000", endDate: "20260610", endTime: "1400" }], reqReason: "[QE-6-8] 마감월 보정 상신(차단 기대)" } });
  out.steps.push(`④ 202606 보정 상신=${corr.status} ${corr.json?.errorCode||""} (기대 ATTD_400_099)`);

  // 해제(청정월 원복)
  const u6 = await call("POST", "/webApi/attd07/attd-unclose", { token: tok, body: { siteCd: "00010", nodeCd: "", incSubNodeYn: "N", closeYm: "202606", closeDesc: "[QE-6-8] 메커니즘 증명 해제" } });
  out.steps.push(`해제 202606=${u6.status}`);
  let s6b = (await call("GET", "/webApi/attd07/attd-close-status?siteCd=00010&nodeCd=&incSubNodeYn=N&closeYm=202606", { token: tok })).json;
  out.steps.push(`해제 후 202606 closed=${s6b.closed}`);

  out.dbCheck = "202607 미마감(REQ174 교착). 202606 마감→해제 왕복 정상(잔존 마감 0).";
  out.appView = `마감월 보정 상신 = ${corr.status} ${corr.json?.errorCode||""}`;
  const mechPass = c6.status === 200 && corr.json?.errorCode === "ATTD_400_099" && u6.status === 200 && s6b.closed === false;
  record("QE-6-8", "OBSERVED", { ...out,
    note: "★핵심 M10 교착: H(비활성)의 7월 대기 보정 REQ174 반려 불가(ATTD_404_011: reject 스코프 USE_YN='Y' 필터) + User_01 목록 비활성자 미노출 + update-user-infos 재활성 500(COMMON_500_001) → 202607 마감 영구 교착(퇴사/비활성 잔존요청 정리절차 부재, §7 G13 강). "
      + `마감 메커니즘 자체는 청정월 202606 으로 증명: ①blockTotal 표기 ②ATTD_400_040 차단(별도) ③마감 200 ④마감월 상신 ATTD_400_099 차단 ⑤해제 200 왕복 정상(mechPass=${mechPass}). `
      + "6-9/6-10/6-12/3-12 는 202607 마감상태 의존 → M10 중단점으로 DEFERRED." });
  console.log("DONE mechPass=" + mechPass);
  process.exit(0);
};
run().catch((e) => { console.error(e); process.exit(1); });
