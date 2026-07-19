// QE-1-8 — 스케줄 내 시간대 OT 신청 거부 관찰(오늘 7/17, 스케줄 09~18, 시도 10:00~11:00).
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
    // 오늘 탭에서 수정 요청 → 초과근무 신청
    await page.locator('button:has-text("수정 요청")').first().click();
    await page.waitForTimeout(1500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 시트 ===");
    console.log(text.split("\n").slice(-12).join(" | "));
    const ot = page.locator(':text("초과근무 신청")').first();
    if (!(await ot.count())) { console.log("초과근무 신청 액션 미노출 — 관찰 종료"); await page.screenshot({ path: shotPath("QE-1-8", "app", "sheet-no-ot") }); return; }
    await ot.click();
    await page.waitForTimeout(2500);
    console.log("URL:", page.url());
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== OT 폼 초기 ===");
    console.log(text.slice(0, 900));
    await page.screenshot({ path: shotPath("QE-1-8", "app", "ot-form-initial"), fullPage: true });

    // 스케줄 내 10:00~11:00 입력
    const tsf = page.locator("button.tsf-field");
    await setStepperTime(page, tsf.nth(0), 10, 0);
    await setStepperTime(page, tsf.nth(1), 11, 0);
    await page.waitForTimeout(800);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 입력 후(인라인 경고 관찰) ===");
    console.log(text.slice(0, 1000));
    await page.fill("textarea", "[QE-1-8] 스케줄 내 OT 시도 — 거부 기대");
    await page.waitForTimeout(500);
    await page.screenshot({ path: shotPath("QE-1-8", "app", "ot-form-inschedule"), fullPage: true });
    const submit = page.locator('button:has-text("요청하기")');
    console.log("요청하기 disabled=", await submit.isDisabled());
    if (!(await submit.isDisabled())) {
      await submit.click();
      await page.waitForTimeout(2000);
      text = await page.evaluate(() => document.body.innerText);
      console.log("=== 제출 시도 결과 ===");
      console.log(text.split("\n").slice(-14).join(" | "));
      await page.screenshot({ path: shotPath("QE-1-8", "app", "submit-response") });
      await clickPopupOk(page);
    } else {
      console.log("(관찰) 폼 단계 선차단 — 제출 버튼 비활성");
      await page.screenshot({ path: shotPath("QE-1-8", "app", "preblocked") });
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
