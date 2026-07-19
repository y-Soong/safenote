// QE-1-7 웹 면 — Attd_07(근무 관리) 7/12 일자상세 OT행 확인.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_07", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    const i = text.indexOf("근무 관리");
    console.log("=== Attd_07 초기 ===");
    console.log(text.slice(i, i + 1200));
    const inputs = await page.evaluate(() =>
      [...document.querySelectorAll("input")].map((x) => ({ ph: x.placeholder, val: x.value, cls: String(x.className).slice(0, 30) })).slice(0, 12)
    );
    console.log("INPUTS:", JSON.stringify(inputs));
    await page.screenshot({ path: shotPath("QE-1-7", "web", "attd07-initial"), fullPage: true });
    await page.click('button:has-text("조회")').catch(() => {});
    await page.waitForTimeout(3000);
    text = await page.evaluate(() => document.body.innerText);
    const lines = text.split("\n").filter((l) => l.includes("QT사원에이") || l.includes("07-12") || l.includes("07.12"));
    console.log("=== A/7-12 관련 행 ===");
    console.log(lines.join("\n").slice(0, 800));
    await page.screenshot({ path: shotPath("QE-1-7", "web", "attd07-list"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
