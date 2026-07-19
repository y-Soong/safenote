// 웹 Attd_08 조회 캡처 — node e1/web-attd08-shot.mjs <caseId> <label>
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , caseId = "QE-x", label = "attd08"] = process.argv;
const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_08", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(1500);
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(3000);
    const text = await page.evaluate(() => document.body.innerText);
    const lines = text.split("\n").filter((l) => l.includes("QT") || l.includes("사용자명"));
    console.log(lines.join("\n"));
    await page.screenshot({ path: shotPath(caseId, "web", label), fullPage: true });
    console.log("shot:", shotPath(caseId, "web", label));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
