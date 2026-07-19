// QE-2-1 앱 검증 — A 요청 카드 상태 + 이번달 캘린더 7/27 연차 표기.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { waitLoaded, bodyText } from "./lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await waitLoaded(page);
    // 홈 → 승인 요청 카드 → MyRequests
    await page.locator(':text("승인 요청")').first().click();
    await page.waitForTimeout(2500);
    await waitLoaded(page);
    let text = await bodyText(page);
    console.log("=== MyRequests ===");
    console.log(text.slice(0, 1200));
    await page.screenshot({ path: shotPath("QE-2-1", "app", "myrequests"), fullPage: true });
    await page.goBack();
    await page.waitForTimeout(2000);
    // 내근태 이번달 27일 셀
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2000);
    await page.click('.attd-seg__item:has-text("이번달")');
    await page.waitForTimeout(2500);
    await page.evaluate(() => {
      const cells = [...document.querySelectorAll("button, td, [class*='cell'], [class*='day']")];
      const el = cells.find((c) => c.innerText && c.innerText.trim().split("\n")[0] === "27");
      if (!el) throw new Error("27일 셀 미발견");
      el.click();
    });
    await page.waitForTimeout(2000);
    text = await bodyText(page);
    console.log("=== 이번달 27일 선택 ===");
    const idx = text.indexOf("27");
    console.log(text.slice(Math.max(0, text.length - 1500)));
    await page.screenshot({ path: shotPath("QE-2-1", "app", "month-27"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
