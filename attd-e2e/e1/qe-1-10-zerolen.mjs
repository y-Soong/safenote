// QE-1-10 — C 출근 직후 같은 분 내 퇴근(길이 0 근태) → 재출근 시도 관찰.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { clickPopupOk } from "./lib-form.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERC", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });

    // 1) 출근
    await page.click('button:has-text("출근하기")');
    await page.waitForTimeout(1200);
    await clickPopupOk(page); // 확인(출근하시겠어요?) + 완료 팝업
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 출근 후 ===", text.match(/근무중[\s\S]{0,30}/)?.[0]?.replace(/\n/g, " "));

    // 2) 같은 분 내 퇴근
    await page.click('button:has-text("퇴근하기")');
    await page.waitForTimeout(1200);
    await clickPopupOk(page);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 퇴근 후 ===", text.match(/퇴근 완료[\s\S]{0,40}/)?.[0]?.replace(/\n/g, " "));
    await page.screenshot({ path: shotPath("QE-1-10", "app", "zero-length") });

    // 3) 재출근 시도 — 홈 출근하기 버튼 상태
    const inBtn = page.locator('button:has-text("출근하기")').first();
    console.log("홈 출근하기 disabled=", await inBtn.isDisabled());
    if (!(await inBtn.isDisabled())) {
      await inBtn.click();
      await page.waitForTimeout(1500);
      text = await page.evaluate(() => document.body.innerText);
      console.log("=== 재출근 클릭 직후 ===");
      console.log(text.split("\n").slice(-14).join(" | "));
      await page.screenshot({ path: shotPath("QE-1-10", "app", "reattempt-1") });
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) {
        await ok.click().catch(() => {});
        await page.waitForTimeout(2200);
        text = await page.evaluate(() => document.body.innerText);
        console.log("=== 재출근 확인 후 ===");
        console.log(text.split("\n").slice(-14).join(" | "));
        await page.screenshot({ path: shotPath("QE-1-10", "app", "reattempt-response") });
        await clickPopupOk(page, 1);
      }
    }
    // 4) 내근태 화면의 재출근(2회차) 경로도 관찰
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2200);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 내근태 ===");
    console.log(text.slice(0, 800));
    await page.screenshot({ path: shotPath("QE-1-10", "app", "myattd"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
