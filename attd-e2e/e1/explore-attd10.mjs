// 웹 Attd_10(요청 승인 관리) 구조 실측 — 보정 요청 승인 경로.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_10", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2000);
    await page.click('button:has-text("조회")').catch(() => {});
    await page.waitForTimeout(2500);
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== Attd_10 ===");
    console.log(text.slice(text.indexOf("요청 승인"), text.indexOf("요청 승인") + 1800));
    await page.screenshot({ path: shotPath("QE-1-5", "web", "attd10-list"), fullPage: true });
    const btns = await page.evaluate(() =>
      [...document.querySelectorAll("button")].map((b) => b.innerText.replace(/\n/g, " ").trim()).filter(Boolean).slice(0, 40)
    );
    console.log("버튼:", JSON.stringify(btns));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
