// QE-3-5 — H 내근태 오늘 탭 dayType 채집. node e3/qe-3-5-myattd-h.mjs <label>
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , label = "state"] = process.argv;
const waitLoaded = (p) => p.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 }).catch(() => {});

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERH", "QtTest!2026");
    await waitLoaded(page);
    let home = await page.evaluate(() => document.body.innerText);
    console.log("=== 홈 카드(앞 300자) ===");
    console.log(home.slice(0, 300).replace(/\n/g, " | "));
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2500);
    await waitLoaded(page);
    const today = await page.evaluate(() => document.body.innerText);
    console.log("=== 내근태 오늘 탭 ===");
    console.log(today.slice(0, 900).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-3-5", "app", `h-myattd-${label}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
