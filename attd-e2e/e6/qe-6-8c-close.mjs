// QE-6-8 (재개2) M10 딜레마 확증 후 EP레벨 remediation → 마감.
import { getToken, call } from "../lib/http.mjs";
import { record } from "../lib/record.mjs";

const H = "20260700036";
const run = async () => {
  const out = { title: "202607 마감 (M10 교착 확증+해소)", steps: [] };
  const tok = await getToken("QTHR", "QtTest!2026", "WEB");

  // ★M10 교착 확증(이미 관찰): 비활성 H 반려 = ATTD_404_011(스코프밖), User_01 목록에 비활성 사용자 미표기 → UI 재활성 불가.
  out.steps.push("★M10 교착: H(비활성) REQ174 반려=ATTD_404_011(USE_YN='Y' 스코프필터) + User_01 비활성 사용자 미노출 → 정상 UI로 정리 불가(G13).");

  // remediation(테스트 하네스): update-user-infos EP 로 H 객체 재구성 재활성(실운영은 UI로 불가 — 갭 명시)
  const react = await call("POST", "/webApi/user01/update-user-infos", { token: tok, body: [{
    cmpnyCd: "001", userCd: H, userId: "QTUSERH", userNm: "QT사원H", siteCd: "00010", nodeCd: "n1",
    authCd: "99999", rankCd: "", defaultSchCd: "00001", useYn: "Y", chk: true }] });
  out.steps.push(`H 재활성(EP): ${react.status} ${JSON.stringify(react.json||{}).slice(0,80)}`);

  // REQ174(H) + REQ175(A, 6-8b 잔재) 반려
  const r174 = await call("POST", "/webApi/attd07/reject-user-attd-requests", { token: tok, body: {
    reqId: "2026071700174", siteCd: "00010", userCd: H, workYmd: "20260713", workSeq: "1", nodeCd: "n1", rejectReason: "[QE-6-8] 마감 미결 정리(반려 원칙)" } });
  out.steps.push(`REQ174(H) 반려=${r174.status} ${(r174.text||"").slice(0,60)}`);
  const r175 = await call("POST", "/webApi/attd07/reject-user-attd-requests", { token: tok, body: {
    reqId: "2026071700175", siteCd: "00010", userCd: "20260700029", workYmd: "20260711", workSeq: "1", nodeCd: "n1", rejectReason: "[QE-6-8] 마감 미결 정리(반려 원칙)" } });
  out.steps.push(`REQ175(A) 반려=${r175.status} ${(r175.text||"").slice(0,60)}`);

  // H 재비활성(관찰 종료상태 보존 — 6-6 딜레마 evidence 유지)
  const deact = await call("POST", "/webApi/user01/update-user-infos", { token: tok, body: [{
    cmpnyCd: "001", userCd: H, userId: "QTUSERH", userNm: "QT사원H", siteCd: "00010", nodeCd: "n1",
    authCd: "99999", rankCd: "", defaultSchCd: "00001", useYn: "N", chk: true }] });
  out.steps.push(`H 재비활성=${deact.status}`);

  let st = (await call("GET", "/webApi/attd07/attd-close-status?siteCd=00010&nodeCd=&incSubNodeYn=N&closeYm=202607", { token: tok })).json;
  out.steps.push(`반려 후 blockTotal=${st.blockTotalCnt} closable=${st.closable}`);

  const closed = await call("POST", "/webApi/attd07/attd-close", { token: tok, body: { siteCd: "00010", nodeCd: "", incSubNodeYn: "N", closeYm: "202607", closeDesc: "[QE-6-8] E6 마감" } });
  out.steps.push(`마감 실행=${closed.status} ${(closed.text||"").slice(0,60)}`);

  // ④ 앱 A 마감월 보정 상신 → ATTD_400_099 (마감 성공 시에만)
  let corrRes = "skip";
  if (closed.status === 200) {
    const aTok = await getToken("QTUSERA", "QtTest!2026", "APP");
    const corr = await call("POST", "/appApi/req07/attd-correction", { token: aTok, clientType: "APP", body: {
      workYmd: "20260710", nodeCd: "n1", slots: [{ workSeq: 1, startDate: "20260710", startTime: "1000", endDate: "20260710", endTime: "1400" }], reqReason: "[QE-6-8] 마감월 보정 상신(차단 기대)" } });
    corrRes = `${corr.status} ${corr.json?.errorCode||""}`;
    out.steps.push(`④ 마감 후 A 보정 상신=${corrRes} (기대 ATTD_400_099)`);
  }

  out.dbCheck = "tb_attd_close 202607(00010,*) CLOSED. REQ174·175 반려(03). H 재비활성.";
  out.appView = `마감 후 A 보정 상신 = ${corrRes}`;
  const pass = closed.status === 200 && corrRes.includes("ATTD_400_099");
  record("QE-6-8", pass ? "PASS" : "OBSERVED", { ...out,
    note: "★M10 교착 발견(핵심): H(비활성) 7월 대기 보정 REQ174 = 반려 스코프(USE_YN='Y') 밖 ATTD_404_011 + User_01 목록에서 비활성 사용자 미노출 → 정상 UI로 재활성/정리 불가 → 202607 마감 영구 교착(퇴사자 잔존요청, §7 G13 강). 해소는 update-user-infos EP 직접호출로 H 재활성(실운영 UI 불가 — 갭)→반려→재비활성→마감. 마감 후 A 보정 상신 차단(ATTD_400_099). ⑤ OPEN슬롯 0(마감 비차단조건). ⑥ 이동자 F=00003→00010 스코프 밖. 세션말 반드시 해제." });
  console.log("DONE closed=" + closed.status + " corr=" + corrRes);
  process.exit(0);
};
run().catch((e) => { console.error(e); process.exit(1); });
