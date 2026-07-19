// QE-2-5 검증 — G 앱: 연차현황 잔여 복원 + 요청 카드 반려 상태.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { waitLoaded, bodyText } from "./lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERG", "QtTest!2026");
    await waitLoaded(page);
    // 연차 현황
    await page.click('button.app-tabbar__tab:has-text("마이")');
    await page.waitForTimeout(2000);
    await waitLoaded(page);
    await page.locator('[aria-label="연차 현황 보기"]').first().click();
    await page.waitForTimeout(2500);
    await waitLoaded(page);
    let text = await bodyText(page);
    console.log("=== G 연차 현황(반려 후) ===");
    console.log(text.slice(0, 700));
    await page.screenshot({ path: shotPath("QE-2-5", "app", "g-summary-after-reject"), fullPage: true });
    // 홈 → 승인 요청 카드 (연차현황은 탭바 없음 — 뒤로가기 2회)
    await page.goBack();
    await page.waitForTimeout(1500);
    await page.click('button.app-tabbar__tab:has-text("홈")');
    await page.waitForTimeout(2000);
    await waitLoaded(page);
    await page.locator(':text("승인 요청")').first().click();
    await page.waitForTimeout(2500);
    text = await bodyText(page);
    console.log("=== G MyRequests ===");
    console.log(text.slice(0, 1500));
    await page.screenshot({ path: shotPath("QE-2-5", "app", "g-myrequests"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
