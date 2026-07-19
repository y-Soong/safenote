// QE-1-5 사전 — 7/14 일 선택 → 시트 → 근태 보정 폼 도달 + 구조 덤프.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERC", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2000);
    await page.click('.attd-seg__item:has-text("이번주")');
    await page.waitForTimeout(2000);
    await page.evaluate(() => {
      const btn = [...document.querySelectorAll("button.dc")].find((b) => /화\s*14/.test(b.innerText));
      if (!btn) throw new Error("7/14 일 버튼 미발견");
      btn.click();
    });
    await page.waitForTimeout(1500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 일 선택 후 ===");
    console.log(text.split("\n").slice(-30).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-5", "app", "day-sheet") });

    // 시트에서 근태 보정 액션
    const fix = page.locator(':text("근태 보정")').first();
    if (await fix.count()) {
      await fix.click();
      await page.waitForTimeout(2500);
      text = await page.evaluate(() => document.body.innerText);
      console.log("=== 보정 폼 ===");
      console.log(text.slice(0, 1200));
      console.log("URL:", page.url());
      const btns = await page.evaluate(() =>
        [...document.querySelectorAll("button")].map((b) => ({ cls: String(b.className).slice(0, 40), txt: b.innerText.replace(/\n/g, " ").trim().slice(0, 40), dis: b.disabled })).filter((b) => b.txt)
      );
      console.log("=== 버튼 ===");
      for (const b of btns) console.log(JSON.stringify(b));
      await page.screenshot({ path: shotPath("QE-1-5", "app", "form-initial"), fullPage: true });
    } else {
      console.log("근태 보정 액션 미발견");
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
