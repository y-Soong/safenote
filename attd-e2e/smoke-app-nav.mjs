// 앱 내비게이션 스모크 v2 — 하단 탭 클릭 방식으로 근태 화면 진입 검증.
import { appLogin, closeAll } from "./lib/browser.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");

    // 하단 탭 "근태" 클릭.
    await page.click('text=근태');
    await page.waitForTimeout(1500);
    console.log("근태 탭 후 url=", page.url());
    let text = (await page.evaluate(() => document.body.innerText)).replace(/\n+/g, " | ").slice(0, 400);
    console.log("근태 화면:", text);
    await page.screenshot({ path: "run/smoke-nav-attd.png", fullPage: true });

    // 마이 탭.
    await page.click('text=마이');
    await page.waitForTimeout(1500);
    console.log("마이 탭 후 url=", page.url());
    text = (await page.evaluate(() => document.body.innerText)).replace(/\n+/g, " | ").slice(0, 400);
    console.log("마이 화면:", text);
    console.log("SMOKE RESULT: PASS");
  } catch (e) {
    console.log("SMOKE RESULT: FAIL —", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
