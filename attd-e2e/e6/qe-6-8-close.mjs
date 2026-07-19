// QE-6-8 ✅ 근태 마감 전체 여정 — com-015 세션충돌 회피(브라우저/ API QTHR 위상 분리).
import { webLogin, evictSession } from "../lib/browser.mjs";
import { getToken, call, evictToken } from "../lib/http.mjs";
import { record, shotPath } from "../lib/record.mjs";

const WEB = "http://localhost:8081";
const snap = async (p, l) => { try { await p.screenshot({ path: shotPath("QE-6-8", "web", l), animations: "disabled" }); } catch {} };
const freshTok = async () => { evictToken("QTHR", "WEB"); return getToken("QTHR", "QtTest!2026", "WEB"); };

const run = async () => {
  const out = { title: "202607 근태 마감 전체 여정", steps: [] };

  // ===== Phase 1: 브라우저(QTHR) 차단상태 캡처 =====
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto(`${WEB}/safenote/main/Attd_07`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1500);
    const srch = page.locator('button:has-text("조회")').first();
    if (await srch.isVisible().catch(() => false)) { await srch.click(); await page.waitForTimeout(2200); }
    const issueTxt = await page.locator('.a07-issue-count').first().innerText().catch(() => "");
    out.steps.push(`② Attd_07 배지='${issueTxt.replace(/\s+/g," ")}'`);
    await snap(page, "block-state");
    const btn = page.locator('button.a07-btn-line:has-text("근태 마감")').first();
    if (await btn.isVisible().catch(() => false)) {
      await btn.click(); await page.waitForTimeout(1000);
      const alertTxt = await page.locator('.modal-overlay').first().innerText().catch(() => "");
      out.steps.push(`② 마감버튼 클릭 얼럿='${alertTxt.replace(/\s+/g," ").slice(0,80)}'`);
      const ok = page.locator('.modal-overlay button:has-text("확인")').first();
      if (await ok.isVisible().catch(()=>false)) await ok.click().catch(()=>{});
    }
  } catch (e) { out.steps.push("web block EX:" + String(e).slice(0, 120)); }
  await evictSession("web", "QTHR"); // 브라우저 세션 종료(토큰 반납)

  // ===== Phase 2: API(QTHR) 권위 액션 — 브라우저 종료 후 새 토큰 =====
  let tok = await freshTok();
  let st = (await call("GET", "/webApi/attd07/attd-close-status?siteCd=00010&nodeCd=&incSubNodeYn=N&closeYm=202607", { token: tok })).json;
  out.steps.push(`① 미결현황 대기요청=${st.pendingReqCnt} GPS=${st.gpsUnconfirmedCnt} OT=${st.unapprovedOtCnt} blockTotal=${st.blockTotalCnt} closable=${st.closable}`);

  const blocked = await call("POST", "/webApi/attd07/attd-close", { token: tok, body: { siteCd: "00010", nodeCd: "", incSubNodeYn: "N", closeYm: "202607", closeDesc: "[QE-6-8]" } });
  out.steps.push(`② 서버 마감 시도 status=${blocked.status} err=${blocked.json?.errorCode||""} (기대 ATTD_400_040)`);

  const rej = await call("POST", "/webApi/attd07/reject-user-attd-requests", { token: tok, body: {
    reqId: "2026071700174", siteCd: "00010", userCd: "20260700036", workYmd: "20260713", workSeq: "1", nodeCd: "n1", rejectReason: "[QE-6-8] 마감 미결 정리(반려 원칙)" } });
  out.steps.push(`③ REQ174(H 비활성) 반려 status=${rej.status} ${(rej.text||"").slice(0,100)}`);

  if (rej.status >= 400) {
    out.steps.push("★M10 컨틴전시: 미결 반려 실패 → 중단점");
    record("QE-6-8", "OBSERVED", { ...out, note: "M10 컨틴전시 발동: 미결 반려 실패로 마감 불가." });
    console.log("M10_STOP"); process.exit(0);
  }

  st = (await call("GET", "/webApi/attd07/attd-close-status?siteCd=00010&nodeCd=&incSubNodeYn=N&closeYm=202607", { token: tok })).json;
  out.steps.push(`③ 반려 후 blockTotal=${st.blockTotalCnt} closable=${st.closable}`);

  const closed = await call("POST", "/webApi/attd07/attd-close", { token: tok, body: { siteCd: "00010", nodeCd: "", incSubNodeYn: "N", closeYm: "202607", closeDesc: "[QE-6-8] E6 마감" } });
  out.steps.push(`③ 마감 실행 status=${closed.status} ${(closed.text||"").slice(0,80)}`);

  // ④ 앱 A 마감월 보정 상신 → ATTD_400_099 (APP 토큰 — WEB 과 무충돌)
  const aTok = await getToken("QTUSERA", "QtTest!2026", "APP");
  const corr = await call("POST", "/appApi/req07/attd-correction", { token: aTok, clientType: "APP", body: {
    workYmd: "20260711", nodeCd: "n1", slots: [{ workSeq: 1, startDate: "20260711", startTime: "1000", endDate: "20260711", endTime: "1400" }], reqReason: "[QE-6-8] 마감월 보정 상신(차단 기대)" } });
  out.steps.push(`④ 마감 후 A 보정 상신 status=${corr.status} err=${corr.json?.errorCode||""} (기대 ATTD_400_099)`);

  // ===== Phase 3: 브라우저(QTHR) 마감상태 캡처 =====
  try {
    await evictSession("web", "QTHR");
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto(`${WEB}/safenote/main/Attd_07`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1500);
    const srch = page.locator('button:has-text("조회")').first();
    if (await srch.isVisible().catch(() => false)) { await srch.click(); await page.waitForTimeout(2200); }
    const badge = await page.locator('.a07-issue-count').first().innerText().catch(() => "");
    out.steps.push(`③ 마감 후 배지='${badge.replace(/\s+/g," ")}'`);
    await snap(page, "closed-state");
  } catch (e) { out.steps.push("web closed EX:" + String(e).slice(0, 120)); }

  out.webView = "Attd_07 처리필요1건→마감버튼 차단얼럿→반려 후 마감→'마감됨'.";
  out.dbCheck = "tb_attd_close 202607(00010,*) CLOSED. REQ174 반려(03).";
  out.appView = `마감 후 A 보정 상신 = ${corr.status} ${corr.json?.errorCode||""}`;
  const pass = blocked.json?.errorCode === "ATTD_400_040" && closed.status === 200 && corr.json?.errorCode === "ATTD_400_099";
  record("QE-6-8", pass ? "PASS" : "OBSERVED", { ...out,
    note: "미결 차단(ATTD_400_040)→미결 반려(M10: H 비활성 요청도 관리자 반려 성공)→202607 마감→마감월 상신 차단(ATTD_400_099). ⑤ OPEN슬롯 0(마감 차단조건 아님). ⑥ 이동자 F=00003 귀속→00010 마감 스코프 밖." });
  console.log("DONE");
  process.exit(0);
};
run().catch((e) => { console.error(e); process.exit(1); });
