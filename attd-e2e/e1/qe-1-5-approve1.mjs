// QE-1-5 1건째 승인 — 웹 Attd_10 근태 보정 탭에서 C의 7/14 보정 승인.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_10", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2000);
    await page.click('button:has-text("근태 보정")');
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    const anchor = text.indexOf("근태 보정");
    console.log("=== 근태 보정 탭 ===");
    console.log(text.slice(anchor, anchor + 1500));
    await page.screenshot({ path: shotPath("QE-1-5", "web", "correction-tab"), fullPage: true });
    const btns = await page.evaluate(() =>
      [...document.querySelectorAll("button")].map((b) => b.innerText.replace(/\n/g, " ").trim()).filter(Boolean).slice(0, 40)
    );
    console.log("버튼:", JSON.stringify(btns));
    // 행 클릭(사용자명 포함 행)
    const row = page.locator("tr", { hasText: "QT자기승인씨" }).first();
    if (await row.count()) {
      await row.click();
      await page.waitForTimeout(2000);
      text = await page.evaluate(() => document.body.innerText);
      console.log("=== 행 선택 후 ===");
      console.log(text.slice(text.indexOf("근태 보정"), text.indexOf("근태 보정") + 2000));
      const btns2 = await page.evaluate(() =>
        [...document.querySelectorAll("button")].map((b) => ({ t: b.innerText.replace(/\n/g, " ").trim(), d: b.disabled })).filter((b) => b.t).slice(0, 40)
      );
      console.log("버튼2:", JSON.stringify(btns2));
      await page.screenshot({ path: shotPath("QE-1-5", "web", "row-selected"), fullPage: true });
    } else {
      console.log("행 미발견");
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
