// QE-1-4 — D seg1 출근 → seg1 OPEN 중 seg2 출근 시도(차단 기대 ATTD_400_081).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const clickThroughPopups = async (page, n = 2) => {
  for (let i = 0; i < n; i++) {
    const ok = page.locator("button:has-text('확인')").first();
    if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
  }
};

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERD", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.type(), d.message()); await d.accept(); });

    // 1) seg1 출근
    await page.click('button:has-text("1구간 출근")');
    await page.waitForTimeout(1500);
    await clickThroughPopups(page);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== seg1 출근 후 홈 ===");
    console.log(text.slice(0, 400));
    await page.screenshot({ path: shotPath("QE-1-4", "app", "seg1-open") });

    // 2) seg1 OPEN 상태에서 seg2 출근 시도
    const seg2 = page.locator('button:has-text("2구간 출근")').first();
    if (!(await seg2.count())) {
      console.log("(관찰) seg2 출근 버튼 미노출 — UI 선차단 형태");
      await page.screenshot({ path: shotPath("QE-1-4", "app", "seg2-hidden") });
    } else {
      const disabled = await seg2.isDisabled();
      console.log("seg2 버튼 disabled=", disabled);
      if (!disabled) {
        await seg2.click();
        await page.waitForTimeout(1500);
        text = await page.evaluate(() => document.body.innerText);
        console.log("=== seg2 시도 직후 ===");
        console.log(text.split("\n").slice(-20).join(" | "));
        await page.screenshot({ path: shotPath("QE-1-4", "app", "seg2-attempt") });
        // 확인(출근하시겠어요?) 팝업이면 진행해서 서버 차단 관찰
        const ok = page.locator("button:has-text('확인')").first();
        if (await ok.count()) {
          await ok.click().catch(() => {});
          await page.waitForTimeout(2000);
          text = await page.evaluate(() => document.body.innerText);
          console.log("=== seg2 확인 후(차단 얼럿 기대) ===");
          console.log(text.split("\n").slice(-20).join(" | "));
          await page.screenshot({ path: shotPath("QE-1-4", "app", "seg2-blocked") });
          await clickThroughPopups(page, 1);
        }
      } else {
        await page.screenshot({ path: shotPath("QE-1-4", "app", "seg2-disabled") });
      }
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
