// QE-1-4 사전 — QTUSERD 홈 카드 상태 실측(2구간 UI).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERD", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== D 홈 ===");
    console.log(text.slice(0, 600));
    await page.screenshot({ path: shotPath("QE-1-4", "app", "home-before") });
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2500);
    const attd = await page.evaluate(() => document.body.innerText);
    console.log("=== D 내근태 ===");
    console.log(attd.slice(0, 900));
    await page.screenshot({ path: shotPath("QE-1-4", "app", "myattd-before") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
