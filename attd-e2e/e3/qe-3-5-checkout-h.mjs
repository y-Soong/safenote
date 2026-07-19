// QE-3-5 — H 퇴근(당일 지정휴무 등록→해제 후 정상 여부).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const waitLoaded = (p) => p.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 }).catch(() => {});

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERH", "QtTest!2026");
    await waitLoaded(page);
    const out = page.locator('button:has-text("퇴근하기")').first();
    console.log("퇴근하기 disabled=", await out.isDisabled());
    await out.click();
    await page.waitForTimeout(1500);
    for (let i = 0; i < 2; i++) {
      const body = await page.evaluate(() => document.body.innerText);
      console.log(`단계${i} 하단:`, body.split("\n").filter((l) => l.trim()).slice(-5).join(" | "));
      const proceed = page.locator('.modal-overlay button:has-text("확인")').last();
      if (await proceed.count()) { await proceed.click().catch(() => {}); await page.waitForTimeout(2000); } else break;
    }
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== 퇴근 후 홈(앞 300자) ===");
    console.log(text.slice(0, 300).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-3-5", "app", "h-checkout-after"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
