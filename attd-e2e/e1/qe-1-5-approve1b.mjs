// QE-1-5 1건째 승인 v2 — 좌측 카드 클릭 → 우측 상세 → 승인.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_10", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2000);
    await page.click('button:has-text("근태 보정")');
    await page.waitForTimeout(2500);
    await page.locator(':text("QT자기승인씨")').first().click();
    await page.waitForTimeout(2000);
    let text = await page.evaluate(() => document.body.innerText);
    const i = text.indexOf("대기 요청");
    console.log("=== 선택 후 ===");
    console.log(text.slice(i, i + 1800));
    const btns = await page.evaluate(() =>
      [...document.querySelectorAll("button")].map((b) => ({ t: b.innerText.replace(/\n/g, " ").trim(), d: b.disabled })).filter((b) => b.t).slice(0, 40)
    );
    console.log("버튼:", JSON.stringify(btns));
    await page.screenshot({ path: shotPath("QE-1-5", "web", "detail-1st"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
