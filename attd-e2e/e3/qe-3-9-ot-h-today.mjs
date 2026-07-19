// QE-3-9 — H 오늘(7/17 제헌절·휴일) OT 신청: 실근태 10:33~10:36, 스케줄 없음 → 등록가능시간 실측 후 상신.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { setStepperTime, clickPopupOk } from "../e1/lib-form.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERH", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2500);
    // 오늘 탭 기본 — 수정 요청
    await page.locator('button:has-text("수정 요청")').first().click();
    await page.waitForTimeout(1500);
    const sheet = await page.evaluate(() => document.body.innerText.slice(-300));
    console.log("액션 시트:", sheet.replace(/\n/g, " | "));
    const ot = page.locator(':text("초과근무 신청")').first();
    const disabled = await ot.evaluate((el) => el.closest("button")?.disabled ?? false).catch(() => "?");
    console.log("초과근무 신청 disabled=", disabled);
    if (disabled === true) { console.log("H도 선차단"); return; }
    await ot.click();
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== OT 폼 초기 ===");
    console.log(text.slice(0, 1100).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-3-9", "app", "ot-form-h-today"), fullPage: true });
    const tsf = page.locator("button.tsf-field");
    console.log("tsf 개수:", await tsf.count());
    await page.fill("textarea", "[QE-3-9] 휴일(제헌절) 당일 OT 대기 시드");
    await page.waitForTimeout(800);
    const submit = page.locator('button:has-text("요청하기")').last();
    console.log("요청하기 disabled=", await submit.isDisabled().catch(() => "?"));
    if (await submit.isDisabled().catch(() => true)) {
      // 프리필이 비었을 수 있음 — 10:33~10:36 수동 입력
      await setStepperTime(page, tsf.nth(0), 10, 33);
      await setStepperTime(page, tsf.nth(1), 10, 36);
      await page.waitForTimeout(800);
      console.log("수동입력 후 disabled=", await submit.isDisabled().catch(() => "?"));
    }
    await page.screenshot({ path: shotPath("QE-3-9", "app", "ot-form-h-filled"), fullPage: true });
    if (!(await submit.isDisabled().catch(() => true))) {
      await submit.click();
      await page.waitForTimeout(2500);
      text = await page.evaluate(() => document.body.innerText);
      console.log("=== 제출 직후 ===");
      console.log(text.split("\n").slice(-10).join(" | "));
      await clickPopupOk(page);
    } else {
      const warn = (await page.evaluate(() => document.body.innerText)).split("\n").filter((l) => /없어요|불가|초과근무 등록/.test(l));
      console.log("차단 경고:", JSON.stringify(warn.slice(0, 6)));
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
