// QE-1-7 검증 — A 내근태 오늘/이번주/이번달 OT 표시(승인분 7/12 4h).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2000);

    // 오늘 탭
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 오늘 탭 ===");
    console.log(text.slice(0, 700));
    await page.screenshot({ path: shotPath("QE-1-7", "app", "tab-today"), fullPage: true });

    // 이번주 탭
    await page.click('.attd-seg__item:has-text("이번주")');
    await page.waitForTimeout(2200);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 이번주 탭 ===");
    console.log(text.slice(0, 900));
    await page.screenshot({ path: shotPath("QE-1-7", "app", "tab-week"), fullPage: true });

    // 이번달 탭 + 7/12 상세
    await page.click('.attd-seg__item:has-text("이번달")');
    await page.waitForTimeout(2500);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 이번달 탭 요약 ===");
    console.log(text.slice(0, 500));
    await page.evaluate(() => {
      const cells = [...document.querySelectorAll("button, td, [class*='cell'], [class*='day']")];
      const el = cells.find((c) => c.innerText && c.innerText.trim().split("\n")[0] === "12");
      if (el) el.click();
    });
    await page.waitForTimeout(2000);
    text = await page.evaluate(() => document.body.innerText);
    const i = text.indexOf("2026년 7월 12일");
    console.log("=== 7/12 상세(OT 표시) ===");
    console.log(i >= 0 ? text.slice(i, i + 600) : text.slice(-400));
    await page.screenshot({ path: shotPath("QE-1-7", "app", "tab-month-day12"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
