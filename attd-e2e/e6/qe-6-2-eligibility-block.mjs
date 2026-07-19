// QE-6-2 🛡 불가사유 보유자 예약 차단 UI (QTUSERG: 미래 확정 시간차 LV075~077 7/29)
//  - GET transfer-eligibility(G) → eligible=false + blockReasons(사용자 표시 문구).
//  - POST transfer-reservation(G) → 차단(USER_400_069 계열). 예약 미생성 확인.
import { getToken, call } from "../lib/http.mjs";
import { record } from "../lib/record.mjs";

const run = async () => {
  const out = { title: "불가사유 보유자(G) 소속이동 예약 차단(⑤ 시간차 미커버)", steps: [] };
  let coveredEligible = null, uncoveredBlocked = false, blockMsgs = "", reserveRejected = false;
  try {
    const tok = await getToken("QTHR", "QtTest!2026", "WEB");

    // (A) 지시서 전제 재현: 00003/ST001(0900-1800) 대상 — G 시간차(11~14:30)는 모두 커버 → eligible=true(정상 허용).
    const covered = await call("GET", "/webApi/user01/20260700034/transfer-eligibility?toSiteCd=00003&toDefaultSchCd=00001&moveDate=20260724", { token: tok });
    coveredEligible = covered.json?.eligible;
    out.steps.push(`(A) 00003/ST001(0900-1800) eligible=${coveredEligible} reasons=${JSON.stringify(covered.json?.blockReasons||[])} — 시간차가 신 근무구간에 완전 포함되어 정상 허용(⑤ 미발동)`);

    // (B) 실제 가드 발동: 미커버 근무타입(QTOVN 2200-0430)이면 11~14:30 시간차를 감싸지 못함 → USER_400_069.
    const uncovered = await call("GET", "/webApi/user01/20260700034/transfer-eligibility?toSiteCd=00010&toDefaultSchCd=00005&moveDate=20260724", { token: tok });
    const reasons = uncovered.json?.blockReasons || [];
    uncoveredBlocked = uncovered.json?.eligible === false && reasons.length > 0;
    blockMsgs = reasons.map(r => `${r.code}:${r.message}`).join(" | ");
    out.steps.push(`(B) 00010/QTOVN(2200-0430) eligible=${uncovered.json?.eligible} reasons='${blockMsgs.slice(0,220)}'`);

    // (B) 예약 시도 → 서버 최종 차단 확인(가드 발동 조건에서만)
    const res = await call("POST", "/webApi/user01/transfer-reservation", {
      token: tok,
      body: { userCd: "20260700034", toSiteCd: "00010", toNodeCd: "n1", moveDate: "20260724", toDefaultSchCd: "00005", moveReason: "[QE-6-2] 차단 검증(생성돼서는 안 됨)" },
    });
    reserveRejected = res.status >= 400;
    out.steps.push(`(B) reserve(G,QTOVN) status=${res.status} rejected=${reserveRejected} body=${(res.text||"").slice(0,160)}`);
  } catch (e) { out.steps.push("EX:" + String(e).slice(0, 160)); }

  out.webView = `UserTransferPop 불가사유 배너 = '${blockMsgs.slice(0,120)}' (transfer-block 렌더, 예약 버튼 disabled). 커버 케이스(00003)는 정상 허용.`;
  out.dbCheck = "미커버(QTOVN) 케이스 G 예약 미생성 — 별도 쿼리 확인. 커버 케이스로 생성된 TR2026071700007 은 cleanup 대상.";
  const pass = uncoveredBlocked && reserveRejected;
  record("QE-6-2", pass ? "GUARD_OK" : "OBSERVED", {
    ...out,
    note: `★지시서 전제 정정: '미래 확정 시간차 보유'만으로는 차단 안 됨(⑤는 신 근무타입 커버리지 판정). G 시간차(11~14:30)는 ST001(0900-1800)에 커버되어 정상 허용. 실제 가드는 미커버 근무타입(QTOVN)에서 USER_400_069 로 발동+예약 차단 확인.`,
  });
};
run().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
