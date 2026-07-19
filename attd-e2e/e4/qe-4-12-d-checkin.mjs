// QE-4-12 step1 — D 앱 홈 상태 관찰 + 출근 시도(OPEN 슬롯 생성 여부)
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERD", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.type(), d.message()); await d.accept(); });
    const home = await page.evaluate(() => document.body.innerText);
    console.log("=== D 홈(전) ===");
    console.log(home.slice(0, 900).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-4-12", "app", "home-before"), fullPage: true });
    // 출근 버튼 존재 여부
    const btns = await page.evaluate(() => [...document.querySelectorAll("button")].map((b) => b.innerText.trim()).filter(Boolean).slice(0, 20));
    console.log("버튼들:", JSON.stringify(btns));
    const inBtn = page.locator('button:has-text("출근하기"), button:has-text("1구간 출근"), button:has-text("2구간 출근")').first();
    if (await inBtn.count()) {
      console.log("출근 버튼 클릭:", await inBtn.innerText());
      await inBtn.click();
      await page.waitForTimeout(2000);
      // 확인 팝업 / 외근 시트 처리
      const body1 = await page.evaluate(() => document.body.innerText);
      console.log("클릭 직후:", body1.slice(0, 500).replace(/\n/g, " | "));
      const ok = page.locator('.modal-overlay button:has-text("출근"), button:has-text("확인")').last();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(2000); }
      const ok2 = page.locator('button:has-text("확인")').last();
      if (await ok2.count()) { await ok2.click().catch(() => {}); await page.waitForTimeout(1500); }
      console.log("출근 처리 후:", (await page.evaluate(() => document.body.innerText)).slice(0, 600).replace(/\n/g, " | "));
    } else {
      console.log("출근 버튼 없음(재출근 불가/완료 상태)");
    }
    await page.screenshot({ path: shotPath("QE-4-12", "app", "home-after"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
