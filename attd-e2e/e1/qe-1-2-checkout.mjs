// QE-1-2 앱 퇴근 — QTUSERA 홈 [퇴근하기] → 완료 확인 + 내근태 표기.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    await page.screenshot({ path: shotPath("QE-1-2", "app", "before") });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.type(), d.message()); await d.accept(); });

    await page.click('button:has-text("퇴근하기")');
    await page.waitForTimeout(1500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 퇴근 클릭 직후 ===");
    console.log(text.split("\n").slice(-25).join(" | "));
    // 확인 팝업 처리
    const confirm = page.locator(".modal-overlay button:has-text('확인'), button:has-text('확인')").first();
    if (await confirm.count()) { await confirm.click().catch(() => {}); await page.waitForTimeout(2000); }
    // 성공 팝업 처리
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 1차 처리 후 ===");
    console.log(text.split("\n").slice(-25).join(" | "));
    const ok2 = page.locator("button:has-text('확인')").first();
    if (await ok2.count()) { await ok2.click().catch(() => {}); await page.waitForTimeout(1500); }
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 홈 최종 ===");
    console.log(text.slice(0, 500));
    await page.screenshot({ path: shotPath("QE-1-2", "app", "home-after") });

    // 내근태 오늘
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2500);
    const attd = await page.evaluate(() => document.body.innerText);
    console.log("=== 내근태 ===");
    console.log(attd.slice(0, 1200));
    await page.screenshot({ path: shotPath("QE-1-2", "app", "myattd-today"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
