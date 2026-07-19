// QE-1-1 앱 출근 — QTUSERA 홈 [출근하기] 탭 → 완료 표시 + 내근태(오늘) 확인.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    await page.screenshot({ path: shotPath("QE-1-1", "app", "before") });

    // native dialog 대비
    page.on("dialog", async (d) => { console.log("DIALOG:", d.type(), d.message()); await d.accept(); });

    await page.click('button:has-text("출근하기")');
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 출근 클릭 직후 ===");
    console.log(text.slice(0, 900));
    await page.screenshot({ path: shotPath("QE-1-1", "app", "after-click") });

    // 팝업(확인/닫기 버튼) 있으면 처리
    const popupBtn = page.locator('button:has-text("확인"), button:has-text("닫기")').first();
    if (await popupBtn.count()) {
      const btnText = await popupBtn.innerText().catch(() => "");
      console.log("팝업 버튼 감지:", btnText);
      await popupBtn.click().catch(() => {});
      await page.waitForTimeout(2000);
      text = await page.evaluate(() => document.body.innerText);
      console.log("=== 팝업 처리 후 ===");
      console.log(text.slice(0, 900));
    }
    await page.screenshot({ path: shotPath("QE-1-1", "app", "home-after") });

    // 내근태 오늘 탭
    await page.click("text=근태");
    await page.waitForTimeout(2500);
    const attdText = await page.evaluate(() => document.body.innerText);
    console.log("=== 내근태 화면 ===");
    console.log(attdText.slice(0, 1200));
    await page.screenshot({ path: shotPath("QE-1-1", "app", "myattd-today"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
