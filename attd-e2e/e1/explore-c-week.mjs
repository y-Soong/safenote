// QE-1-5 사전 — QTUSERC 내근태 이번주 탭에서 7/14 진입 경로 실측.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERC", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2000);
    await page.click('.attd-seg__item:has-text("이번주")');
    await page.waitForTimeout(2500);
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== 이번주 ===");
    console.log(text.slice(0, 1800));
    await page.screenshot({ path: shotPath("QE-1-5", "app", "week-before"), fullPage: true });
    // 클릭 가능한 요소 후보
    const els = await page.evaluate(() =>
      [...document.querySelectorAll("button, [class*='day'], [class*='row'], li")].map((e) => ({
        tag: e.tagName, cls: String(e.className).slice(0, 50), txt: (e.innerText || "").replace(/\n/g, " ").trim().slice(0, 60),
      })).filter((e) => e.txt).slice(0, 40)
    );
    console.log("=== 요소 ===");
    for (const e of els) console.log(JSON.stringify(e));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
