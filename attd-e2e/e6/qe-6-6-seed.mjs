// QE-6-6 시드: H 잔존 데이터 구성 (deactivate 전).
//  (b) H 대기 연차(8/20 종일, 결재자 G)
//  (g) G 대기 연차(8/25 종일, 결재자 H) — 퇴사자가 결재자(교착 관찰)
//  (e) H 보정 대기(7/13 과거일, 관리자처리) — 202607 마감 차단 유발
//  (f) H OT 대기(7/16) — best-effort
import { getToken, call } from "../lib/http.mjs";

const main = async () => {
  const out = {};
  const hTok = await getToken("QTUSERH", "QtTest!2026", "APP");
  const gTok = await getToken("QTUSERG", "QtTest!2026", "APP");

  // (b) H 대기 연차 8/20, 결재자 G(20260700034)
  const b = await call("POST", "/appApi/leaveflow/apply", { token: hTok, clientType: "APP",
    body: { leaveCd: "SYS_ANNUAL", leaveType: "ANNUAL", workYmd: "20260820", useUnitType: "00", reason: "[QE-6-6b] H 대기 연차", approverUserCds: ["20260700034"] } });
  out.b = `${b.status} ${(b.text||"").slice(0,120)}`;

  // (g) G 대기 연차 8/25, 결재자 H(20260700036) — 교착 시드
  const g = await call("POST", "/appApi/leaveflow/apply", { token: gTok, clientType: "APP",
    body: { leaveCd: "SYS_MONTHLY", leaveType: "MONTHLY", workYmd: "20260825", useUnitType: "00", reason: "[QE-6-6g] G 대기 연차(결재자=H 퇴사예정)", approverUserCds: ["20260700036"] } });
  out.g = `${g.status} ${(g.text||"").slice(0,120)}`;

  // (e) H 보정 대기 7/13 과거일 (관리자처리, 202607 마감 차단원)
  const e = await call("POST", "/appApi/req07/attd-correction", { token: hTok, clientType: "APP",
    body: { workYmd: "20260713", nodeCd: "n1", slots: [{ workSeq: 1, startDate: "20260713", startTime: "0900", endDate: "20260713", endTime: "1800" }], reqReason: "[QE-6-6e] H 보정 대기(마감 차단원)" } });
  out.e = `${e.status} ${(e.text||"").slice(0,140)}`;

  // (f) H OT 대기 7/16 (best-effort — raw근태 밖 OT 미충족 시 실패 허용)
  const f = await call("POST", "/appApi/req07/overtime", { token: hTok, clientType: "APP",
    body: { workYmd: "20260716", nodeCd: "n1", slots: [{ workSeq: 1, startDate: "20260716", startTime: "1800", endDate: "20260716", endTime: "2000" }], reqReason: "[QE-6-6f] H OT 대기" } });
  out.f = `${f.status} ${(f.text||"").slice(0,160)}`;

  console.log(JSON.stringify(out, null, 2));
  process.exit(0);
};
main().catch((e) => { console.error(e); process.exit(1); });
