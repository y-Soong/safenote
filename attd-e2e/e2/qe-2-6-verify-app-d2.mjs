// QE-2-6 앱 검증 보완 — 7/30 셀(두번째 '30') 선택.
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
    const info = await page.evaluate(() => {
      const cells = [...document.querySelectorAll("button")].filter((c) => c.innerText && c.innerText.trim().split("\n")[0] === "30");
      const el = cells[cells.length - 1]; // 7/30 (마지막 매칭)
      if (!el) return "미발견";
      const t = el.innerText.replace(/\n/g, "|");
      el.click();
      return t;
    });
    console.log("셀 텍스트:", info);
    await page.waitForTimeout(2000);
    const text = await bodyText(page);
    console.log("=== 상세 ===");
    console.log(text.slice(text.length - 900));
    await page.screenshot({ path: shotPath("QE-2-6", "app", "d-month-30b"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
