// QE-2-9 — G 앱 출근 시도(오늘=확정 연차일) → 차단/확인팝업 관찰.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { waitLoaded, bodyText } from "./lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERG", "QtTest!2026");
    await waitLoaded(page);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    let text = await bodyText(page);
    console.log("=== G 홈(연차일) ===");
    console.log(text.slice(0, 1000));
    await page.screenshot({ path: shotPath("QE-2-9", "app", "g-home-leaveday"), fullPage: true });
    const btn = page.locator('button:has-text("출근하기")');
    console.log("출근하기 버튼 수:", await btn.count(), "disabled=", (await btn.count()) ? await btn.first().isDisabled() : "-");
    if (await btn.count()) {
      await btn.first().click();
      await page.waitForTimeout(2000);
      text = await bodyText(page);
      console.log("=== 출근 클릭 후(팝업 관찰) ===");
      console.log(text.split("\n").slice(-25).join(" | "));
      await page.screenshot({ path: shotPath("QE-2-9", "app", "checkin-popup"), fullPage: true });
      // 팝업 버튼 실측
      const btns = await page.locator(".modal-overlay button, [class*='popup'] button, [class*='sheet'] button").allInnerTexts();
      console.log("팝업 버튼:", JSON.stringify(btns));
      // 진행형 팝업이면 '확인 후 진행' 여부 관찰 — 진행은 하지 않고 취소 우선
      const cancel = page.locator('button:has-text("취소")').last();
      if (await cancel.count()) { await cancel.click().catch(() => {}); console.log("취소 클릭(슬롯 미생성 유지)"); }
      else {
        const ok = page.locator("button:has-text('확인')").last();
        if (await ok.count()) { await ok.click().catch(() => {}); console.log("확인 클릭"); }
      }
      await page.waitForTimeout(1500);
      text = await bodyText(page);
      console.log("=== 처리 후 홈 ===");
      console.log(text.slice(0, 600));
      await page.screenshot({ path: shotPath("QE-2-9", "app", "after"), fullPage: true });
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
