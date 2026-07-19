// QE-2-7 — Attd_13 생성 팝업 탐색 + DELETE 요청 작성(QT교대디 7/30).
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_13", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    await page.click('button:has-text("생성")');
    await page.waitForTimeout(2000);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 생성 팝업 ===");
    const gi = text.indexOf("요청 목록");
    console.log(text.slice(gi >= 0 ? gi : 0).slice(0, 2000));
    await page.screenshot({ path: shotPath("QE-2-7", "web", "create-popup"), fullPage: true });
    const selects = await page.locator("select:visible").evaluateAll((els) => els.map((e) => [...e.options].map((o) => o.text).join("/").slice(0, 120)));
    console.log("selects:", JSON.stringify(selects, null, 1));
    const inputs = await page.locator("input:visible").evaluateAll((els) => els.map((e) => ({ type: e.type, ph: e.placeholder, cls: e.className.slice(0, 40) })));
    console.log("inputs:", JSON.stringify(inputs));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
