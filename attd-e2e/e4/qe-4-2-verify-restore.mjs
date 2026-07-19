// QE-4-2 원복 검증 — 앱(A) 8/13 스케줄이 09:00로 돌아왔는지
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2500);
    await page.locator('.attd-seg__item:has-text("이번달")').click();
    await page.waitForTimeout(2000);
    await page.locator('button[aria-label="다음 달"]').click();
    await page.waitForTimeout(2000);
    await page.evaluate(() => {
      const cells = [...document.querySelectorAll("button, td")];
      const c = cells.find((x) => (x.innerText ?? "").trim().split("\n")[0] === "13" && x.className.includes("cal"));
      if (c) c.click();
    });
    await page.waitForTimeout(1500);
    const t = await page.evaluate(() => document.body.innerText);
    console.log("앱 8/13:", t.slice(-400).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-4-2", "app", "cal-aug-13-restored") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
