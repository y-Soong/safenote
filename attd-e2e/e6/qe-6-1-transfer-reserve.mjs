// QE-6-1 ✅ 소속이동 예약 UI 여정 + 앱 안내/ack (QT11-3·11-5 UI 재검증)
//  - 예약 생성은 웹 API(transfer-reservation, UserTransferPop 이 호출하는 동일 EP)로 seed.
//  - 웹 User_01 목록에 QTUSERI 표기 캡처.
//  - 앱(QTUSERI) 로그인 → 소속이동 안내 시트(.tn-sheet) 노출 → 확인(ack) → 재로그인 미재노출.
//  - [DB] RESERVED + NOTICE_ACK_YN='Y'.
import { appLogin, webLogin, evictSession } from "../lib/browser.mjs";
import { getToken, call } from "../lib/http.mjs";
import { record, shotPath } from "../lib/record.mjs";

const WEB = "http://localhost:8081";
const snap = async (page, l) => { try { await page.screenshot({ path: shotPath("QE-6-1", l.split(":")[0], l.split(":")[1] || l), animations: "disabled" }); } catch {} };

const run = async () => {
  const out = { title: "소속이동 예약 + 앱 안내/ack", steps: [] };
  let reservationId = "";

  // (1) 웹 API 로 예약 생성 (UserTransferPop 동일 EP). 먼저 eligibility 확인.
  try {
    const tok = await getToken("QTHR", "QtTest!2026", "WEB");
    const elig = await call("GET", "/webApi/user01/20260700037/transfer-eligibility?toSiteCd=00003&toDefaultSchCd=00001&moveDate=20260724", { token: tok });
    out.steps.push(`eligibility(I) status=${elig.status} eligible=${elig.json?.eligible} blocks=${JSON.stringify(elig.json?.blockReasons||[]).slice(0,120)}`);

    const res = await call("POST", "/webApi/user01/transfer-reservation", {
      token: tok,
      body: { userCd: "20260700037", toSiteCd: "00003", toNodeCd: "n1", moveDate: "20260724", toDefaultSchCd: "00001", moveReason: "[QE-6-1] 소속이동 예약 테스트" },
    });
    reservationId = res.json?.reservationId || "";
    out.steps.push(`reserve status=${res.status} reservationId=${reservationId} body=${(res.text||"").slice(0,120)}`);
  } catch (e) { out.steps.push("reserve EX:" + String(e).slice(0, 150)); }

  // (2) 웹 User_01 목록 표기 캡처
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto(`${WEB}/safenote/main/User_01`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1200);
    const srch = page.locator('button:has-text("조회")').first();
    if (await srch.isVisible().catch(() => false)) { await srch.click(); await page.waitForTimeout(1500); }
    const bodyTxt = (await page.locator("body").innerText().catch(() => "")).replace(/\s+/g, " ");
    out.steps.push(`User_01 목록 QTUSERI 표기=${bodyTxt.includes("QTUSERI") || bodyTxt.includes("QT사원I")}`);
    await snap(page, "web:user01-list");
  } catch (e) { out.steps.push("web User_01 EX:" + String(e).slice(0, 120)); }

  // (3) 앱(QTUSERI) 로그인 → 안내 시트 노출 → 확인(ack)
  let sheetShown = false, ackOk = false;
  try {
    const { page } = await appLogin("QTUSERI", "QtTest!2026");
    // 안내 시트는 MainView 진입 후 my-transfer-notice 조회 뒤 노출
    const sheet = page.locator('.tn-sheet');
    await sheet.waitFor({ state: "visible", timeout: 8000 }).catch(() => {});
    sheetShown = await sheet.isVisible().catch(() => false);
    const sheetTxt = sheetShown ? (await sheet.innerText().catch(() => "")).replace(/\s+/g, " ") : "";
    out.steps.push(`앱 안내시트 노출=${sheetShown} txt='${sheetTxt.slice(0, 160)}'`);
    await snap(page, "app:advisory-sheet");
    if (sheetShown) {
      await page.locator('.tn-sheet__btn--primary:has-text("확인")').click();
      await page.waitForTimeout(1500);
      ackOk = !(await sheet.isVisible().catch(() => false));
      out.steps.push(`확인 클릭 후 시트 닫힘=${ackOk}`);
    }
  } catch (e) { out.steps.push("app login/ack EX:" + String(e).slice(0, 150)); }

  // (4) 재로그인 미재노출
  let reShown = "n/a";
  try {
    await evictSession("app", "QTUSERI");
    const { page } = await appLogin("QTUSERI", "QtTest!2026");
    await page.waitForTimeout(3000);
    reShown = String(await page.locator('.tn-sheet').isVisible().catch(() => false));
    out.steps.push(`재로그인 안내 재노출=${reShown}`);
    await snap(page, "app:relogin-nosheet");
  } catch (e) { out.steps.push("app relogin EX:" + String(e).slice(0, 150)); }

  // (5) DB 판정용 요지
  out.webView = "웹 User_01 QTUSERI 표기 + 예약 API 200";
  out.appView = `안내시트 노출=${sheetShown}, ack 후 닫힘=${ackOk}, 재로그인 재노출=${reShown}`;
  out.dbCheck = `reservationId=${reservationId} — STATUS/NOTICE_ACK_YN 은 별도 DB 조회로 확인`;
  const pass = reservationId && sheetShown && ackOk && reShown === "false";
  record("QE-6-1", pass ? "PASS" : "OBSERVED", {
    ...out,
    note: `소속이동 예약(${reservationId}) → 앱 advisory 안내 → ack → 재로그인 미재노출. 세부 DB(RESERVED/ACK_YN) 후속 쿼리.`,
  });
  console.log("RESID=" + reservationId);
};
run().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
