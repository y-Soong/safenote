// QE-1-6 — QTHR 앱 마이 탭에서 관리자 기능 진입점 탐색.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTHR", "QtTest!2026");
    await page.waitForTimeout(2000);
    await page.click('button.app-tabbar__tab:has-text("마이")');
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 마이 ===");
    console.log(text.slice(0, 1500));
    await page.screenshot({ path: shotPath("QE-1-6", "app", "mypage"), fullPage: true });
    const clickables = await page.evaluate(() =>
      [...document.querySelectorAll("button, a, [role='button'], li")].map((e) => (e.innerText || "").replace(/\n/g, " ").trim().slice(0, 40)).filter(Boolean)
    );
    console.log("클릭 후보:", JSON.stringify(clickables));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
