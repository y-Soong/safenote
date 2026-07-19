// QE-1-9 앱 면 — A 내근태 오늘/이번주 판정 표기.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2200);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 오늘 탭 ===");
    console.log(text.slice(0, 700));
    await page.screenshot({ path: shotPath("QE-1-9", "app", "today-judgment"), fullPage: true });
    await page.click('.attd-seg__item:has-text("이번주")');
    await page.waitForTimeout(2200);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 이번주 탭 ===");
    console.log(text.slice(0, 800));
    await page.screenshot({ path: shotPath("QE-1-9", "app", "week-judgment"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
