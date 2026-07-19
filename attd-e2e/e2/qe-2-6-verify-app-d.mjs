// QE-2-6 앱 검증 — D 월캘린더 7/30 연차 표기 + 연차현황 잔여 반영.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { waitLoaded, bodyText } from "./lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERD", "QtTest!2026");
    await waitLoaded(page);
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2000);
    await page.click('.attd-seg__item:has-text("이번달")');
    await page.waitForTimeout(2500);
    await page.evaluate(() => {
      const cells = [...document.querySelectorAll("button, td, [class*='cell'], [class*='day']")];
      const el = cells.find((c) => c.innerText && c.innerText.trim().split("\n")[0] === "30");
      if (!el) throw new Error("30일 셀 미발견");
      el.click();
    });
    await page.waitForTimeout(2000);
    let text = await bodyText(page);
    console.log("=== D 이번달 30일 ===");
    console.log(text.slice(text.length - 1200));
    await page.screenshot({ path: shotPath("QE-2-6", "app", "d-month-30"), fullPage: true });
    // 연차 현황
    await page.click('button.app-tabbar__tab:has-text("마이")');
    await page.waitForTimeout(2000);
    await waitLoaded(page);
    await page.locator('[aria-label="연차 현황 보기"]').first().click();
    await page.waitForTimeout(2500);
    await waitLoaded(page);
    text = await bodyText(page);
    console.log("=== D 연차 현황 ===");
    console.log(text.slice(0, 600));
    await page.screenshot({ path: shotPath("QE-2-6", "app", "d-leave-summary"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
