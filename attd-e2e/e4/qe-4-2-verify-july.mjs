// QE-4-2 보조 — Attd_05 7월 그리드(과거·적용일 훨씬 이전)도 신시각으로 소급 표기되는지 확인
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(3000);
    const row = await page.evaluate(() => {
      const r = [...document.querySelectorAll("tr")].find((x) => x.innerText.includes("QT사원에이"));
      if (!r) return null;
      return [...r.querySelectorAll("td")].slice(-31).map((td, i) => `${i + 1}:${td.innerText.trim().replace(/\n/g, "/")}`).filter((s) => !s.endsWith(":"));
    });
    console.log("Attd_05 7월 A행:", JSON.stringify(row));
    await page.screenshot({ path: shotPath("QE-4-2", "web", "attd05-jul-after"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
