// QE-4-7 step1 — A 8/13 종일 연차 대기 신청(결재자 G, 승인 안 함)
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openLeaveApply, selectLeaveType, selectUnit, setDate, addApprover, readPreview, clickPopupOk, bodyText, waitLoaded } from "../e2/lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await waitLoaded(page);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await openLeaveApply(page);
    await selectLeaveType(page, "연차");
    await selectUnit(page, "종일");
    await setDate(page, 2026, 8, 13);
    const preview = await readPreview(page).catch(() => "-");
    console.log("preview:", preview);
    await page.fill("textarea", "[QE-4-7] 대기 연차 8/13 (스케줄 덮어쓰기 테스트용)");
    await addApprover(page, "QT신입지");
    await page.waitForTimeout(500);
    await page.screenshot({ path: shotPath("QE-4-7", "app", "apply-form"), fullPage: true });
    const submit = page.locator('button:has-text("신청하기")').last();
    console.log("신청하기 disabled=", await submit.isDisabled());
    await submit.click();
    await page.waitForTimeout(2500);
    console.log("제출 직후:", (await bodyText(page)).split("\n").slice(-10).join(" | "));
    await clickPopupOk(page);
    await page.screenshot({ path: shotPath("QE-4-7", "app", "submitted") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
