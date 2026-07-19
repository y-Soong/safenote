// QE-1-10 후속 — 분 경과 후 재출근 재시도(영구 차단 여부) + 2회차 경로 관찰.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { clickPopupOk } from "./lib-form.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERC", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });

    // 1) 홈 재출근 재시도
    await page.click('button:has-text("출근하기")');
    await page.waitForTimeout(1200);
    const ok = page.locator("button:has-text('확인')").first();
    if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(2200); }
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 홈 재시도 응답 ===");
    console.log(text.split("\n").slice(-10).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-10", "app", "retry-later") });
    await clickPopupOk(page, 1);

    // 2) 내근태 '출근하기 (2회차)' 경로
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2200);
    const seg2 = page.locator('button:has-text("출근하기 (2회차)")').first();
    if (await seg2.count()) {
      await seg2.click();
      await page.waitForTimeout(1200);
      const ok2 = page.locator("button:has-text('확인')").first();
      if (await ok2.count()) { await ok2.click().catch(() => {}); await page.waitForTimeout(2200); }
      text = await page.evaluate(() => document.body.innerText);
      console.log("=== 2회차 시도 응답 ===");
      console.log(text.split("\n").slice(-10).join(" | "));
      await page.screenshot({ path: shotPath("QE-1-10", "app", "seg2-retry") });
      await clickPopupOk(page, 1);
    } else {
      console.log("2회차 버튼 없음");
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
