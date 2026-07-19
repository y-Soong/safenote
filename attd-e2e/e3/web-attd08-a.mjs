// Attd_08 A 행 채집 공용 — node e3/web-attd08-a.mjs <caseId> <label> [이름(기본 QT사원에이)]
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , caseId, label, name = "QT사원에이"] = process.argv;
const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_08", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const q = page.locator('button:has-text("조회")').first();
    if (await q.count()) { await q.click(); await page.waitForTimeout(3000); }
    const rows = await page.evaluate((nm) =>
      [...document.querySelectorAll("tr")].filter((r) => r.innerText.includes(nm)).map((r) => r.innerText.replace(/\n/g, " | ").slice(0, 240)),
      name);
    console.log(`=== Attd_08 ${name} 행 ===`);
    rows.forEach((r) => console.log(r));
    await page.screenshot({ path: shotPath(caseId, "web", `attd08-${label}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
