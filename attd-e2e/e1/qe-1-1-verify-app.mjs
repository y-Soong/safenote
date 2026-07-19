// QE-1-1 후속 — 내근태(오늘) 표기 확인. 탭바 셀렉터는 button.app-tabbar__tab 고정.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    const home = await page.evaluate(() => document.body.innerText);
    console.log("=== 홈 카드(재로그인 후) ===");
    console.log(home.slice(0, 400));
    await page.screenshot({ path: shotPath("QE-1-1", "app", "home-status") });

    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2500);
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== 내근태 화면 ===");
    console.log(text.slice(0, 1500));
    await page.screenshot({ path: shotPath("QE-1-1", "app", "myattd-today"), fullPage: true });
    // 탭 구조 덤프(오늘/주/월)
    const tabs = await page.evaluate(() =>
      [...document.querySelectorAll("button, .tab, [class*='tab']")].map((b) => b.className + "|" + (b.innerText || "").replace(/\n/g, " ").trim().slice(0, 30)).filter((s) => s.includes("탭") || /오늘|주|월/.test(s)).slice(0, 20)
    );
    console.log("=== 탭 후보 ===");
    for (const t of tabs) console.log(t);
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
