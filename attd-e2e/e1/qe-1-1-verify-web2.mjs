// QE-1-1 웹 면 v2 — Attd_08 조회 실행 후 QTUSERA 행 확인.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_08", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(1500);
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(3000);
    const text = await page.evaluate(() => document.body.innerText);
    // QTUSER 관련 행만 추출
    const lines = text.split("\n").filter((l) => l.trim());
    const idx = lines.findIndex((l) => l.includes("사용자명"));
    console.log("=== 결과 구간 ===");
    console.log(lines.slice(Math.max(0, idx)).join("\n").slice(0, 2000));
    await page.screenshot({ path: shotPath("QE-1-1", "web", "attd08-result"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
