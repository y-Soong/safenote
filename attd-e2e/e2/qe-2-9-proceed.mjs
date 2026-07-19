// QE-2-9 후속 — 확인 팝업에서 [출근하기] 진행 → 슬롯 생성 여부 기록 → 즉시 퇴근으로 원복.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { waitLoaded, bodyText, clickPopupOk } from "./lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERG", "QtTest!2026");
    await waitLoaded(page);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.locator('button:has-text("출근하기")').first().click();
    await page.waitForTimeout(1500);
    // 연차일 확인 팝업 → 출근하기(진행)
    await page.locator('button:has-text("출근하기")').last().click();
    await page.waitForTimeout(2500);
    let text = await bodyText(page);
    console.log("=== 진행 후 ===");
    console.log(text.split("\n").slice(-20).join(" | "));
    await page.screenshot({ path: shotPath("QE-2-9", "app", "proceed-checkin"), fullPage: true });
    await clickPopupOk(page, 2);
    text = await bodyText(page);
    console.log("=== 홈 상태 ===");
    console.log(text.slice(0, 500));
    // 즉시 퇴근(원복)
    const out = page.locator('button:has-text("퇴근하기")');
    if (await out.count()) {
      await out.first().click();
      await page.waitForTimeout(1500);
      await clickPopupOk(page, 2);
      text = await bodyText(page);
      console.log("=== 퇴근 후 ===");
      console.log(text.slice(0, 400));
    }
    await page.screenshot({ path: shotPath("QE-2-9", "app", "proceed-done"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
