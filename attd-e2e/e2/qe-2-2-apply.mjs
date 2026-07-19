// QE-2-2 — QTUSERA 시간차 30분 신청(7/28 10:00~10:30). preview 캡처 필수.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openLeaveApply, selectLeaveType, selectUnit, setDate, setStartTime, addApprover, readPreview, clickPopupOk, bodyText, waitLoaded } from "./lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await waitLoaded(page);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await openLeaveApply(page);
    await selectLeaveType(page, "연차");
    await selectUnit(page, "30분");
    await setDate(page, 2026, 7, 28);
    await setStartTime(page, 10, 0);
    const preview = await readPreview(page);
    console.log("=== PREVIEW ===");
    console.log(preview);
    // 종료 스텝퍼 표시값
    const endVal = await page.locator(".end-stepper__val").innerText().catch(() => "-");
    console.log("종료 표시:", endVal);
    await page.fill("textarea", "[QE-2-2] 시간차 30분 신청 E2E");
    await addApprover(page, "QT신입지");
    await page.waitForTimeout(500);
    const text0 = await bodyText(page);
    console.log("=== 폼 상태(잔여/차감 표기) ===");
    console.log(text0.slice(0, 2200));
    await page.screenshot({ path: shotPath("QE-2-2", "app", "form-preview"), fullPage: true });
    const submit = page.locator('button:has-text("신청하기")').last();
    console.log("신청하기 disabled=", await submit.isDisabled());
    await submit.click();
    await page.waitForTimeout(2500);
    let text = await bodyText(page);
    console.log("=== 제출 직후 ===");
    console.log(text.split("\n").slice(-12).join(" | "));
    await clickPopupOk(page);
    await page.waitForTimeout(1500);
    text = await bodyText(page);
    console.log("=== 처리 후(연차 현황 — N일 H시간 M분 표기 관찰) ===");
    console.log(text.slice(0, 1400));
    await page.screenshot({ path: shotPath("QE-2-2", "app", "after-submit-summary"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
