// QE-6-6 ①⑤ + 6-7 모니터링 화면 관찰: 결재 교착(G leave 결재자=H 비활성) / H 과거이력 / Attd_11·12.
import { webLogin } from "../lib/browser.mjs";
import { record, shotPath } from "../lib/record.mjs";

const WEB = "http://localhost:8081";
const snap = async (p, l) => { try { await p.screenshot({ path: shotPath("QE-6-6", "web", l), animations: "disabled" }); } catch {} };

const run = async () => {
  const out = { steps: [] };
  const { page } = await webLogin("QTHR", "QtTest!2026");

  // ① Attd_10 연차 상신 — 교착 관찰
  try {
    await page.goto(`${WEB}/safenote/main/Attd_10`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1500);
    const leaveTab = page.locator('button:has-text("연차 상신")').first();
    if (await leaveTab.isVisible().catch(() => false)) { await leaveTab.click(); await page.waitForTimeout(1500); }
    const t = (await page.locator("body").innerText().catch(() => "")).replace(/\s+/g, " ");
    out.steps.push(`Attd_10 연차상신: QE-6-6g(G)=${t.includes("QE-6-6g")||t.includes("QT신입지")} QE-6-6b(H)=${t.includes("QE-6-6b")||t.includes("QT사원H")}`);
    await snap(page, "attd10-leave-pending");
  } catch (e) { out.steps.push("Attd_10 EX:" + String(e).slice(0, 120)); }

  // ⑤ Attd_08 — H 과거 이력 조회 가능 여부
  try {
    await page.goto(`${WEB}/safenote/main/Attd_08`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1200);
    const s = page.locator('button:has-text("조회")').first();
    if (await s.isVisible().catch(() => false)) { await s.click(); await page.waitForTimeout(1800); }
    const t = (await page.locator("body").innerText().catch(() => "")).replace(/\s+/g, " ");
    out.steps.push(`Attd_08 비활성 H 이력 노출=${t.includes("QT사원H") || t.includes("QTUSERH")}`);
    await snap(page, "attd08-h-history");
  } catch (e) { out.steps.push("Attd_08 EX:" + String(e).slice(0, 120)); }

  // 6-7 모니터링 Attd_11/12 로드
  for (const menu of ["Attd_11", "Attd_12"]) {
    try {
      await page.goto(`${WEB}/safenote/main/${menu}`, { waitUntil: "networkidle", timeout: 20000 });
      await page.waitForTimeout(1200);
      const s = page.locator('button:has-text("조회")').first();
      if (await s.isVisible().catch(() => false)) { await s.click(); await page.waitForTimeout(1500); }
      const errs = await page.evaluate(() => window.__pageerr || 0).catch(() => 0);
      out.steps.push(`${menu} 로드 OK(pageerr=${errs})`);
      await snap(page, menu.toLowerCase());
    } catch (e) { out.steps.push(`${menu} EX:` + String(e).slice(0, 100)); }
  }

  out.webView = out.steps.join(" | ");
  out.dbCheck = "H/G 대기요청 REQ172(H leave 8/20,결재자 G)·REQ173(G leave 8/25,결재자 H) 모두 REQ_STATUS='01' 불변 — 비활성이 대기 요청을 자동정리하지 않음(교착). H 원장 정합(diff 0), 미래계획 119건 잔존.";
  record("QE-6-6b", "OBSERVED", { ...out, title: "H 비활성 후 결재교착/이력/모니터링",
    note: "교착 확정: 결재자=H(비활성)인 G 대기연차 REQ173, 신청자=H(비활성)인 H 대기연차 REQ172·보정 REQ174 모두 대기 잔존(자동 반려/취소 없음). 확정연차(LV078)·미래계획(119)·과거이력 보존. 원장 무손상. 표준 정리 절차 부재 → §7 G13." });
  console.log("DONE"); process.exit(0);
};
run().catch((e) => { console.error(e); process.exit(1); });
