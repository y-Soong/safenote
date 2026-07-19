// QE-2-7 — Attd_05에서 D 7/30 연차 셀 더블클릭 → LeaveChangeRequestPop → DELETE 발의.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const cellInfo = await page.evaluate(() => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes("QTUSERD"));
      const tds = [...row.querySelectorAll("td")];
      const dayTds = tds.slice(-31);
      const td = dayTds[29];
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: td.innerText.trim() };
    });
    console.log("30일 셀:", JSON.stringify(cellInfo));
    await page.mouse.dblclick(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(2000);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 팝업 ===");
    const pi = text.indexOf("변경");
    console.log(text.slice(text.length - 1800));
    await page.screenshot({ path: shotPath("QE-2-7", "web", "request-pop"), fullPage: true });
    const selects = await page.locator("select:visible").evaluateAll((els) => els.map((e) => [...e.options].map((o) => o.text).join("/").slice(0, 80)));
    console.log("selects:", JSON.stringify(selects));
    const radios = await page.locator('input[type="radio"]:visible').evaluateAll((els) => els.map((e) => ({ v: e.value, checked: e.checked })));
    console.log("radios:", JSON.stringify(radios));
    const tas = await page.locator("textarea:visible").count();
    console.log("textarea:", tas);
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
