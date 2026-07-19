// QE-2-1 — QTUSERA 연차 종일 신청(7/27, 결재자 QT신입지 지정, 사유 [QE-2-1]).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openLeaveApply, selectLeaveType, setDate, addApprover, clickPopupOk, bodyText, waitLoaded } from "./lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await waitLoaded(page);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    page.on("pageerror", (e) => console.log("PAGEERROR:", e.message));

    await openLeaveApply(page);
    console.log("URL:", page.url());
    let text = await bodyText(page);
    console.log("=== 신청 폼 초기(종류 목록/잔여 표기) ===");
    console.log(text.slice(0, 1200));
    await page.screenshot({ path: shotPath("QE-2-1", "app", "form-initial"), fullPage: true });

    await selectLeaveType(page, "연차");
    text = await bodyText(page);
    console.log("=== 종류 선택 후(단위 옵션/잔여) ===");
    console.log(text.slice(0, 1500));

    // 종일이 기본 단위인지 확인(allowedUnits에 00 있으면 기본 선택됨)
    await setDate(page, 2026, 7, 27);
    await page.fill("textarea", "[QE-2-1] 연차 종일 신청 E2E");
    await addApprover(page, "QT신입지");
    await page.waitForTimeout(500);
    text = await bodyText(page);
    console.log("=== 입력 완료 상태 ===");
    console.log(text.slice(0, 2000));
    await page.screenshot({ path: shotPath("QE-2-1", "app", "form-filled"), fullPage: true });

    const submit = page.locator('button:has-text("신청하기")').last();
    console.log("신청하기 disabled=", await submit.isDisabled());
    await submit.click();
    await page.waitForTimeout(2500);
    text = await bodyText(page);
    console.log("=== 제출 직후 ===");
    console.log(text.split("\n").slice(-15).join(" | "));
    await page.screenshot({ path: shotPath("QE-2-1", "app", "submit-response") });
    await clickPopupOk(page);
    await page.waitForTimeout(1500);
    text = await bodyText(page);
    console.log("=== 처리 후 화면 ===");
    console.log(text.slice(0, 1200));
    await page.screenshot({ path: shotPath("QE-2-1", "app", "after-submit"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
