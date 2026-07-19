// QE-1-3 재퇴근(app-026) — 내근태의 [퇴근 시간 재등록] → OUT 갱신·행수 불변.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.type(), d.message()); await d.accept(); });

    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2500);
    await page.screenshot({ path: shotPath("QE-1-3", "app", "before") });

    const btn = page.locator('button:has-text("퇴근 시간 재등록"), :text("퇴근 시간 재등록")').first();
    if (!(await btn.count())) { console.log("재퇴근 버튼 없음!"); process.exitCode = 1; return; }
    await btn.click();
    await page.waitForTimeout(1500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 재퇴근 클릭 직후 ===");
    console.log(text.split("\n").slice(-20).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-3", "app", "confirm") });

    // 확인 팝업 2단 처리
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1800); }
    }
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 처리 후 내근태 ===");
    console.log(text.slice(0, 900));
    await page.screenshot({ path: shotPath("QE-1-3", "app", "after"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
