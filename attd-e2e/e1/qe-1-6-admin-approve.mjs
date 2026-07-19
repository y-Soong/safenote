// QE-1-6 — AdminHome → 승인 관리 → A 7/12 보정 승인(APPROVE_ASIS).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTHR", "QtTest!2026");
    await page.waitForTimeout(2000);
    await page.click('button.app-tabbar__tab:has-text("마이")');
    await page.waitForTimeout(2000);
    await page.locator(':text("관리자 모드")').first().click();
    await page.waitForTimeout(2500);
    await page.locator(':text("승인 관리")').first().click();
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("URL:", page.url());
    console.log("=== 승인 관리 ===");
    console.log(text.slice(0, 1500));
    await page.screenshot({ path: shotPath("QE-1-6", "app", "admin-approval"), fullPage: true });

    // A의 보정 건 선택
    const item = page.locator(':text("QT사원에이")').first();
    if (!(await item.count())) { console.log("A 요청 미발견"); return; }
    await item.click();
    await page.waitForTimeout(2000);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 상세 ===");
    console.log(text.slice(0, 1500));
    await page.screenshot({ path: shotPath("QE-1-6", "app", "admin-detail"), fullPage: true });
    const btns = await page.evaluate(() =>
      [...document.querySelectorAll("button")].map((b) => ({ t: b.innerText.replace(/\n/g, " ").trim(), d: b.disabled })).filter((b) => b.t)
    );
    console.log("버튼:", JSON.stringify(btns));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
