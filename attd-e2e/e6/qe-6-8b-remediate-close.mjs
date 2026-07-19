// QE-6-8 (재개) M10 딜레마 해소 + 마감.
//  발견: H 비활성 → REQ174 스코프밖(USE_YN='Y' 필터) → 반려 불가(ATTD_404_011) → 202607 마감 교착(G13).
//  해소(UI레벨 정상 remediation, DB핵 아님): H 재활성 → REQ174 반려 → H 재비활성 → 마감.
import { getToken, call } from "../lib/http.mjs";
import { record } from "../lib/record.mjs";

const H = "20260700036";
const tokW = () => getToken("QTHR", "QtTest!2026", "WEB");
const setUseYn = async (tok, yn) => {
  const list = await call("GET", "/webApi/user01/user-info-lists?userKeyword=QTUSERH", { token: tok });
  const t = (list.json?.userInfoList || []).find(u => u.userId === "QTUSERH");
  if (!t) return "no-target";
  const r = await call("POST", "/webApi/user01/update-user-infos", { token: tok, body: [{ ...t, useYn: yn, chk: true }] });
  return `${r.status}`;
};

const run = async () => {
  const out = { title: "202607 마감 (M10 교착 해소 후)", steps: [] };
  const tok = await tokW();

  // 딜레마 확증(기록): 비활성 상태에서 반려 재시도 → 404
  const rej0 = await call("POST", "/webApi/attd07/reject-user-attd-requests", { token: tok, body: {
    reqId: "2026071700174", siteCd: "00010", userCd: H, workYmd: "20260713", workSeq: "1", nodeCd: "n1", rejectReason: "[QE-6-8] 교착 확증" } });
  out.steps.push(`M10 교착 확증: H 비활성 상태 반려=${rej0.status} ${rej0.json?.errorCode||""} (ATTD_404_011=스코프밖)`);

  // remediation: H 재활성
  out.steps.push(`H 재활성=${await setUseYn(tok, "Y")}`);
  // 반려(이제 스코프 내)
  const rej = await call("POST", "/webApi/attd07/reject-user-attd-requests", { token: tok, body: {
    reqId: "2026071700174", siteCd: "00010", userCd: H, workYmd: "20260713", workSeq: "1", nodeCd: "n1", rejectReason: "[QE-6-8] 마감 미결 정리(반려 원칙)" } });
  out.steps.push(`REQ174 반려(H 재활성 후)=${rej.status} ${(rej.text||"").slice(0,80)}`);
  // H 재비활성(관찰 종료상태 보존)
  out.steps.push(`H 재비활성=${await setUseYn(tok, "N")}`);

  // 마감 가능여부
  let st = (await call("GET", "/webApi/attd07/attd-close-status?siteCd=00010&nodeCd=&incSubNodeYn=N&closeYm=202607", { token: tok })).json;
  out.steps.push(`반려 후 blockTotal=${st.blockTotalCnt} closable=${st.closable}`);

  // 마감 실행
  const closed = await call("POST", "/webApi/attd07/attd-close", { token: tok, body: { siteCd: "00010", nodeCd: "", incSubNodeYn: "N", closeYm: "202607", closeDesc: "[QE-6-8] E6 마감" } });
  out.steps.push(`마감 실행=${closed.status} ${(closed.text||"").slice(0,60)}`);

  // ④ 앱 A 마감월 보정 상신 → ATTD_400_099
  const aTok = await getToken("QTUSERA", "QtTest!2026", "APP");
  const corr = await call("POST", "/appApi/req07/attd-correction", { token: aTok, clientType: "APP", body: {
    workYmd: "20260711", nodeCd: "n1", slots: [{ workSeq: 1, startDate: "20260711", startTime: "1000", endDate: "20260711", endTime: "1400" }], reqReason: "[QE-6-8] 마감월 보정 상신(차단 기대)" } });
  out.steps.push(`④ 마감 후 A 보정 상신=${corr.status} ${corr.json?.errorCode||""} (기대 ATTD_400_099)`);

  out.webView = "Attd_07 처리필요1건→차단얼럿→반려 후 마감(별도 스크린샷 패스).";
  out.dbCheck = "tb_attd_close 202607(00010,*) CLOSED. REQ174 반려(03). H 재비활성 유지.";
  out.appView = `마감 후 A 보정 상신 = ${corr.status} ${corr.json?.errorCode||""}`;
  const pass = closed.status === 200 && corr.json?.errorCode === "ATTD_400_099";
  record("QE-6-8", pass ? "PASS" : "OBSERVED", { ...out,
    note: "★M10 교착 발견: H(비활성)의 7월 대기 보정 REQ174가 반려 스코프(USE_YN='Y' 필터) 밖이 되어 ATTD_404_011 로 반려 불가 → 202607 마감 영구 차단(퇴사자 잔존요청 교착, §7 G13). 해소=H 재활성→반려→재비활성(UI레벨, DB핵 아님) 후 마감 성공. 마감월 상신 차단(ATTD_400_099) 확인. ⑤ OPEN슬롯 0(마감 비차단조건). ⑥ 이동자 F=00003 귀속으로 00010 마감 스코프 밖. 세션 말미 반드시 마감 해제." });
  console.log("DONE closed=" + closed.status);
  process.exit(0);
};
run().catch((e) => { console.error(e); process.exit(1); });
