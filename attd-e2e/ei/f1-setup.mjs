// F1 라이브검증 (setup) — QTUSERD 비활성 전, 대기요청 2건 생성.
//  ① 신청자 방향: D 본인 근태보정(workYmd 20260711, 마감교착 핵심 재현)
//  ② 결재자 방향(best-effort): QTUSERA 근태보정 + approverUserCds=[D]
// 실제 상태전이/원장/마감 검증은 MCP DB + f1-deactivate.mjs 로.
import { getToken, call } from "../lib/http.mjs";

const PW = "QtTest!2026";
const D = "20260700032"; // QTUSERD
const line = (o) => console.log(JSON.stringify(o));

const run = async () => {
  // 0) 기준선: 202607 마감 미결현황(before)
  const wtok = await getToken("QTHR", PW, "WEB");
  const st0 = await call("GET", "/webApi/attd07/attd-close-status?siteCd=00010&nodeCd=&incSubNodeYn=N&closeYm=202607", { token: wtok });
  line({ tag: "close-status-before", status: st0.status, body: st0.json });

  // ① 신청자 방향 — D 본인 근태보정 상신
  const dTok = await getToken("QTUSERD", PW, "APP");
  const a = await call("POST", "/appApi/req07/attd-correction", { token: dTok, clientType: "APP", body: {
    workYmd: "20260711", nodeCd: "n1",
    slots: [{ workSeq: 1, startDate: "20260711", startTime: "1000", endDate: "20260711", endTime: "1400" }],
    reqReason: "[F1-verify] 신청자방향 마감교착 재현",
  }});
  line({ tag: "applicant-req(D)", status: a.status, body: a.json, text: (a.text||"").slice(0,200) });

  // ② 결재자 방향(best-effort) — A 근태보정 + 결재자=D 지정
  const aTok = await getToken("QTUSERA", PW, "APP");
  const b = await call("POST", "/appApi/req07/attd-correction", { token: aTok, clientType: "APP", body: {
    workYmd: "20260712", nodeCd: "n1",
    slots: [{ workSeq: 1, startDate: "20260712", startTime: "1000", endDate: "20260712", endTime: "1400" }],
    reqReason: "[F1-verify] 결재자방향(D=approver)",
    approverUserCds: [D],
  }});
  line({ tag: "approver-req(A,approver=D)", status: b.status, body: b.json, text: (b.text||"").slice(0,200) });

  console.log("SETUP_DONE");
};
run().then(() => process.exit(0)).catch((e) => { console.error("ERR", e); process.exit(1); });
