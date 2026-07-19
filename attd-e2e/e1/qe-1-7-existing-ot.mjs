// QE-1-7 보강 — 7/12 OT 폼 재진입 시 기존 승인 OT 표시(app-030) 관찰(제출 없음).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
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
    await page.locator(':text("수정 요청")').first().click();
    await page.waitForTimeout(1500);
    await page.locator(':text("초과근무 신청")').first().click();
    await page.waitForTimeout(2500);
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== OT 폼(기존 OT 존재 상태) ===");
    console.log(text.slice(0, 1300));
    await page.screenshot({ path: shotPath("QE-1-7", "app", "ot-form-existing"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
