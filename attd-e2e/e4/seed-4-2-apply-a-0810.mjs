// QE-4-2 시드 ① — QTUSERA 8/10 종일 연차 신청(결재자 QT신입지). qe-2-1-apply 재사용판.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openLeaveApply, selectLeaveType, setDate, addApprover, clickPopupOk, bodyText, waitLoaded } from "../e2/lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await waitLoaded(page);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await openLeaveApply(page);
    await selectLeaveType(page, "연차");
    await setDate(page, 2026, 8, 10);
    await page.fill("textarea", "[QE-4-2] 8/10 종일 연차 시드");
    await addApprover(page, "QT신입지");
    await page.waitForTimeout(500);
    const submit = page.locator('button:has-text("신청하기")').last();
    console.log("신청하기 disabled=", await submit.isDisabled());
    await submit.click();
    await page.waitForTimeout(2500);
    console.log("제출 직후:", (await bodyText(page)).split("\n").slice(-12).join(" | "));
    await clickPopupOk(page);
    await page.screenshot({ path: shotPath("QE-4-2", "app", "seed-applied-0810"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
