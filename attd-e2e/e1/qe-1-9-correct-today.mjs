// QE-1-9 시드 — A 오늘 근태를 10:00~17:00로 보정 상신(지각+조퇴 동시 조건).
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
    await page.locator('button:has-text("수정 요청")').first().click();
    await page.waitForTimeout(1500);
    await page.locator(':text("근태 보정 요청")').first().click();
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 폼 초기(기존 근태 프리필) ===");
    console.log(text.slice(0, 700));
    // 기존 04:11~04:16 → 10:00~17:00
    const tsf = page.locator("button.tsf-field");
    await setStepperTime(page, tsf.nth(0), 10, 0);
    await setStepperTime(page, tsf.nth(1), 17, 0);
    await page.fill("textarea", "[QE-1-9] 지각+조퇴 판정 표기 검증용 보정");
    await page.waitForTimeout(500);
    await page.screenshot({ path: shotPath("QE-1-9", "app", "form-filled"), fullPage: true });
    await page.click('button:has-text("요청하기")');
    await page.waitForTimeout(2000);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 제출 ===");
    console.log(text.split("\n").slice(-10).join(" | "));
    await clickPopupOk(page);
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
