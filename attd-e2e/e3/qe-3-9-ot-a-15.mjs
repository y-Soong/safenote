// QE-3-9 — A 7/15(실근태 없음) OT 신청 시도: 폼의 등록가능시간 표기·차단 여부 실측 → 가능하면 19:00~21:00 상신.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { setStepperTime, clickPopupOk } from "../e1/lib-form.mjs";

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
      const cells = [...document.querySelectorAll("button, td")];
      const el = cells.find((c) => c.innerText && c.innerText.trim().split("\n")[0] === "15");
      if (el) el.click();
    });
    await page.waitForTimeout(2000);
    const detail = await page.evaluate(() => document.body.innerText.slice(-500));
    console.log("=== 15일 상세 ===");
    console.log(detail.replace(/\n/g, " | "));
    await page.locator('button:has-text("수정 요청")').first().click();
    await page.waitForTimeout(1500);
    const sheet = await page.evaluate(() => document.body.innerText.slice(-400));
    console.log("=== 액션 시트 ===");
    console.log(sheet.replace(/\n/g, " | "));
    const otAction = page.locator(':text("초과근무 신청")').first();
    if ((await otAction.count()) === 0) { console.log("초과근무 신청 액션 없음"); return; }
    await otAction.click();
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== OT 폼 초기(등록가능시간 관찰) ===");
    console.log(text.slice(0, 1200).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-3-9", "app", "ot-form-a-15-initial"), fullPage: true });
    const tsf = page.locator("button.tsf-field");
    const cnt = await tsf.count();
    console.log("tsf-field 개수:", cnt);
    if (cnt >= 2) {
      await setStepperTime(page, tsf.nth(0), 19, 0);
      await setStepperTime(page, tsf.nth(1), 21, 0);
      await page.fill("textarea", "[QE-3-9] 실근태 밖 구간 OT 대기 시드");
      await page.waitForTimeout(800);
      text = await page.evaluate(() => document.body.innerText);
      const warn = text.split("\n").filter((l) => /없어요|경고|불가|초과근무/.test(l)).slice(0, 8);
      console.log("경고 후보:", JSON.stringify(warn));
      const submit = page.locator('button:has-text("요청하기")').last();
      console.log("요청하기 disabled=", await submit.isDisabled().catch(() => "?"));
      await page.screenshot({ path: shotPath("QE-3-9", "app", "ot-form-a-15-filled"), fullPage: true });
      if (!(await submit.isDisabled().catch(() => true))) {
        await submit.click();
        await page.waitForTimeout(2500);
        text = await page.evaluate(() => document.body.innerText);
        console.log("=== 제출 직후 ===");
        console.log(text.split("\n").slice(-10).join(" | "));
        await page.screenshot({ path: shotPath("QE-3-9", "app", "ot-a-15-submit") });
        await clickPopupOk(page);
      }
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
