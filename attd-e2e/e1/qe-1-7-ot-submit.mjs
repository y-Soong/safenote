// QE-1-7 — QTUSERA 7/12 초과근무 신청(OvertimeForm, 기존 OT 표시 확인 포함).
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
    await page.evaluate(() => {
      const cells = [...document.querySelectorAll("button, td, [class*='cell'], [class*='day']")];
      const el = cells.find((c) => c.innerText && c.innerText.trim().split("\n")[0] === "12");
      if (!el) throw new Error("12일 셀 미발견");
      el.click();
    });
    await page.waitForTimeout(2000);
    await page.locator(':text("수정 요청")').first().click();
    await page.waitForTimeout(1500);
    await page.locator(':text("초과근무 신청")').first().click();
    await page.waitForTimeout(2500);
    console.log("URL:", page.url());
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== OT 폼 초기(기존 OT 표시 영역 관찰) ===");
    console.log(text.slice(0, 1300));
    await page.screenshot({ path: shotPath("QE-1-7", "app", "ot-form-initial"), fullPage: true });

    // 시간 입력 — tsf 필드 실측 후 13:00~17:00
    const tsf = page.locator("button.tsf-field");
    console.log("tsf-field 개수:", await tsf.count());
    await setStepperTime(page, tsf.nth(0), 13, 0);
    await setStepperTime(page, tsf.nth(1), 17, 0);
    await page.fill("textarea", "[QE-1-7] 주말 사후 초과근무 신청");
    await page.waitForTimeout(500);
    await page.screenshot({ path: shotPath("QE-1-7", "app", "ot-form-filled"), fullPage: true });
    await page.click('button:has-text("요청하기"), button:has-text("신청하기")');
    await page.waitForTimeout(2000);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 제출 직후 ===");
    console.log(text.split("\n").slice(-12).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-7", "app", "ot-submit-response") });
    await clickPopupOk(page);
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
