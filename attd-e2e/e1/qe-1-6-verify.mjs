// QE-1-6 검증 — A 앱 요청 카드 상태(대기→승인) + 이번주/이번달 7/12 표기.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    // 홈 → 승인 요청 카드
    await page.locator(':text("승인 요청")').first().click();
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("URL:", page.url());
    console.log("=== 내 요청 ===");
    console.log(text.slice(0, 1200));
    await page.screenshot({ path: shotPath("QE-1-6", "app", "myrequests"), fullPage: true });

    // 내근태 이번달 7/12 재확인
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2000);
    await page.click('.attd-seg__item:has-text("이번달")');
    await page.waitForTimeout(2500);
    await page.evaluate(() => {
      const cells = [...document.querySelectorAll("button, td, [class*='cell'], [class*='day']")];
      const el = cells.find((c) => c.innerText && c.innerText.trim().split("\n")[0] === "12");
      if (el) el.click();
    });
    await page.waitForTimeout(2000);
    text = await page.evaluate(() => document.body.innerText);
    const i = text.indexOf("2026년 7월 12일");
    console.log("=== 7/12 상세 ===");
    console.log(text.slice(i, i + 500));
    await page.screenshot({ path: shotPath("QE-1-6", "app", "day12-after"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
