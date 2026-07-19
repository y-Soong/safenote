// QE-1-6 — QTUSERA 7/12(일, 스케줄 없음) 보정 상신 13:00~17:00.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { setStepperTime, clickPopupOk } from "./lib-form.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });

    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2000);
    await page.click('.attd-seg__item:has-text("이번달")');
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 이번달 상단 ===");
    console.log(text.slice(0, 600));
    await page.screenshot({ path: shotPath("QE-1-6", "app", "month-view") });

    // 캘린더에서 12일 클릭
    await page.evaluate(() => {
      const cells = [...document.querySelectorAll("button, td, [class*='cell'], [class*='day']")];
      const el = cells.find((c) => c.innerText && c.innerText.trim().split("\n")[0] === "12");
      if (!el) throw new Error("12일 셀 미발견");
      el.click();
    });
    await page.waitForTimeout(2000);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 12일 선택 후 ===");
    console.log(text.split("\n").slice(-30).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-6", "app", "day12-detail") });

    // 수정 요청 → 근태 보정 요청
    const fixBtn = page.locator(':text("수정 요청")').first();
    if (await fixBtn.count()) { await fixBtn.click(); await page.waitForTimeout(1500); }
    const corr = page.locator(':text("근태 보정 요청")').first();
    if (!(await corr.count())) { console.log("근태 보정 요청 액션 미발견"); process.exitCode = 1; return; }
    await corr.click();
    await page.waitForTimeout(2500);
    console.log("URL:", page.url());
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 폼 ===");
    console.log(text.slice(0, 500));

    await setStepperTime(page, page.locator("button.tsf-field").nth(0), 13, 0);
    await setStepperTime(page, page.locator("button.tsf-field").nth(1), 17, 0);
    await page.fill("textarea", "[QE-1-6] 주말(일) 스케줄 없는 날 보정");
    await page.waitForTimeout(500);
    await page.screenshot({ path: shotPath("QE-1-6", "app", "form-filled"), fullPage: true });
    await page.click('button:has-text("요청하기")');
    await page.waitForTimeout(2000);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 제출 직후 ===");
    console.log(text.split("\n").slice(-12).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-6", "app", "submit-response") });
    await clickPopupOk(page);
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
