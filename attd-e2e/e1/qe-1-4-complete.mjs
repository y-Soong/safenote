// QE-1-4 완결 — seg1 퇴근 → seg2 출근 → seg2 퇴근 (WORK_SEQ 2벌 생성).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const popups = async (page, n = 2) => {
  for (let i = 0; i < n; i++) {
    const ok = page.locator("button:has-text('확인')").first();
    if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1600); }
  }
};

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERD", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { await d.accept(); });

    // 1) seg1 퇴근 (홈 카드)
    await page.click('button:has-text("퇴근하기")');
    await page.waitForTimeout(1500);
    await popups(page);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== seg1 퇴근 후 홈 ===");
    console.log(text.slice(0, 350));

    // 2) seg2 출근 — 홈 카드 버튼 재관찰
    const btns = await page.evaluate(() => [...document.querySelectorAll("button")].map((b) => b.innerText.trim()).filter(Boolean));
    console.log("홈 버튼:", JSON.stringify(btns));
    const seg2in = page.locator('button:has-text("2구간 출근")').first();
    if (await seg2in.count()) {
      await seg2in.click();
      await page.waitForTimeout(1500);
      await popups(page);
      text = await page.evaluate(() => document.body.innerText);
      console.log("=== seg2 출근 후 ===");
      console.log(text.slice(0, 350));
      await page.screenshot({ path: shotPath("QE-1-4", "app", "seg2-open") });
      // 3) seg2 퇴근
      await page.click('button:has-text("퇴근하기")');
      await page.waitForTimeout(1500);
      await popups(page);
      text = await page.evaluate(() => document.body.innerText);
      console.log("=== seg2 퇴근 후 ===");
      console.log(text.slice(0, 350));
    } else {
      console.log("(관찰) seg1 퇴근 후에도 seg2 출근 버튼 없음 — 버튼 목록 참조");
    }
    // 내근태 최종
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2500);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 내근태 최종 ===");
    console.log(text.slice(0, 900));
    await page.screenshot({ path: shotPath("QE-1-4", "app", "complete"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
