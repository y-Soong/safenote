// QE-1-6 승인 — QTHR 앱 로그인(AdminHome) → 승인 화면 탐색 → A 7/12 보정 승인.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTHR", "QtTest!2026");
    console.log("URL:", page.url());
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 관리자 홈 ===");
    console.log(text.slice(0, 1000));
    await page.screenshot({ path: shotPath("QE-1-6", "app", "admin-home"), fullPage: true });

    // 승인/결재 진입점 클릭
    const entry = page.locator(':text("승인"), :text("결재")').first();
    if (!(await entry.count())) { console.log("승인 진입점 미발견"); return; }
    await entry.click();
    await page.waitForTimeout(2500);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 승인 화면 ===");
    console.log(text.slice(0, 1500));
    console.log("URL:", page.url());
    await page.screenshot({ path: shotPath("QE-1-6", "app", "admin-approval-list"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
