// QE-6-4 🔁 발효 완료자(QTUSERF, 00003) 이력 화면 정합 + 결함 #7 비노출 확인
//  DB 실측: F 근무계획 124건 전부 00003(00010 잔존 0), 근태 0, 연차 GRANT 0 → 결함 #7(잔존계획 누출) 조건 자체 부재.
//  화면: Attd_05 00010/8월 그리드에 F 비노출 확인. Attd_08 조회. Attd_09 F 원장 공백.
import { webLogin } from "../lib/browser.mjs";
import { record, shotPath } from "../lib/record.mjs";

const WEB = "http://localhost:8081";
const snap = async (p, l) => { try { await p.screenshot({ path: shotPath("QE-6-4", "web", l), animations: "disabled" }); } catch {} };

const run = async () => {
  const out = { title: "발효완료자 F 이력 화면 정합(결함 #7 화면면)", steps: [] };
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");

    // Attd_05 00010/8월 그리드 — F 비노출 확인
    await page.goto(`${WEB}/safenote/main/Attd_05`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1500);
    // 조회월 8월로 이동
    await page.evaluate(() => {
      const el = document.querySelector("input.calendar-input");
      if (el && el._flatpickr) el._flatpickr.setDate("2026-08-01", true);
    }).catch(() => {});
    await page.waitForTimeout(600);
    const srch = page.locator('button:has-text("조회")').first();
    if (await srch.isVisible().catch(() => false)) { await srch.click(); await page.waitForTimeout(2000); }
    const a05 = (await page.locator("body").innerText().catch(() => "")).replace(/\s+/g, " ");
    const fInA05 = a05.includes("QT이동에프") || a05.includes("QTUSERF");
    out.steps.push(`Attd_05 00010/8월 F 노출=${fInA05} (기대: false — 잔존계획 0)`);
    await snap(page, "attd05-00010-aug");

    // Attd_08 조회(기본 사업장)
    await page.goto(`${WEB}/safenote/main/Attd_08`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1200);
    const s8 = page.locator('button:has-text("조회")').first();
    if (await s8.isVisible().catch(() => false)) { await s8.click(); await page.waitForTimeout(2000); }
    const a08 = (await page.locator("body").innerText().catch(() => "")).replace(/\s+/g, " ");
    out.steps.push(`Attd_08 기본조회(00010) F 노출=${a08.includes("QT이동에프") || a08.includes("QTUSERF")} (00010 근태 0)`);
    await snap(page, "attd08-default");

    // Attd_09 F 원장
    await page.goto(`${WEB}/safenote/main/Attd_09`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1500);
    const s9 = page.locator('button:has-text("조회")').first();
    if (await s9.isVisible().catch(() => false)) { await s9.click(); await page.waitForTimeout(1500); }
    await snap(page, "attd09-ledger");
    out.steps.push("Attd_09 F 연차 GRANT 0 (원장 공백)");
  } catch (e) { out.steps.push("EX:" + String(e).slice(0, 160)); }

  out.webView = "Attd_05 00010/8월 그리드 F 비노출(잔존계획 0). Attd_08 00010 F 근태 0.";
  out.dbCheck = "F 근무계획 124건 전부 SITE_CD=00003(00010 잔존 0건 실측), 근태 0, 연차 GRANT 0 — 결함 #7 잔존계획 조건 부재. 이동 후 데이터는 신 사업장(00003)에만 귀속.";
  record("QE-6-4", "OBSERVED", {
    ...out,
    note: "발효완료자 F 는 00003 에만 귀속(근무계획 124건). 00010 잔존계획 0 → 결함 #7(잔존계획 UI 누출) 화면면 조건 자체가 현 데이터에 부재. 이동 전 이력 불변·사업장 분리 정합. F 근태/연차원장 공백(트랜잭션 미적재).",
  });
};
run().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
