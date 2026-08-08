// Phase 9: F(주52 Attd_15 소정 감소) + P6(웹 대시보드 근태 상태)
import { webLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

const main = async () => {
  const { page } = await webLogin("QTHR", "QtTest!2026");

  // ── [F] Attd_15 주52 ──
  await page.goto("http://localhost:8081/safenote/main/Attd_15", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2500);
  await page.locator("button", { hasText: "조회" }).first().click().catch(() => {});
  await page.waitForTimeout(3500);
  const wText = await page.evaluate(() => {
    const out = [];
    const head = document.querySelector("table tr");
    out.push("[HEAD] " + (head ? head.innerText.replace(/\n/g, " | ").slice(0, 300) : ""));
    document.querySelectorAll("tr").forEach((tr) => {
      if (tr.innerText.includes("QT사원에이")) out.push(tr.innerText.replace(/\n/g, " | ").slice(0, 350));
    });
    return out;
  });
  console.log("=== [F] Attd_15 주52 ===");
  wText.forEach((r) => console.log(r));
  await page.screenshot({ path: `${SHOT}/F-attd15-week52.png`, fullPage: true });

  // ── [P6] 대시보드 ──
  await page.goto("http://localhost:8081/safenote/main", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(3500);
  const dText = await page.evaluate(() => document.body.innerText);
  const di = dText.search(/근태|출근/);
  console.log("=== [P6] 대시보드 (근태 위젯 근처) ===");
  console.log(di >= 0 ? dText.slice(Math.max(0, di - 50), di + 600).replace(/\n{2,}/g, "\n") : dText.slice(0, 400));
  await page.screenshot({ path: `${SHOT}/P6-dashboard.png`, fullPage: true });
  await closeAll();
};
main();
