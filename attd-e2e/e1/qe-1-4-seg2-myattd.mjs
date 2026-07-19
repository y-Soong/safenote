// QE-1-4 보조 — 내근태 화면에서 seg1 OPEN 중 seg2 출근 버튼 존재/시도 여부 관찰.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERD", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.type(), d.message()); await d.accept(); });
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2500);
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== 내근태(seg1 OPEN) ===");
    console.log(text.slice(0, 900));
    await page.screenshot({ path: shotPath("QE-1-4", "app", "myattd-seg1open"), fullPage: true });
    const seg2 = page.locator('button:has-text("2구간 출근")').first();
    if (await seg2.count()) {
      console.log("내근태 seg2 버튼 존재, disabled=", await seg2.isDisabled());
      if (!(await seg2.isDisabled())) {
        await seg2.click();
        await page.waitForTimeout(1500);
        let t2 = await page.evaluate(() => document.body.innerText);
        console.log("=== seg2 클릭 직후 ===");
        console.log(t2.split("\n").slice(-15).join(" | "));
        const ok = page.locator("button:has-text('확인')").first();
        if (await ok.count()) {
          await ok.click().catch(() => {});
          await page.waitForTimeout(2200);
          t2 = await page.evaluate(() => document.body.innerText);
          console.log("=== seg2 확인 후 ===");
          console.log(t2.split("\n").slice(-15).join(" | "));
          await page.screenshot({ path: shotPath("QE-1-4", "app", "seg2-server-response") });
          const ok2 = page.locator("button:has-text('확인')").first();
          if (await ok2.count()) await ok2.click().catch(() => {});
        }
      }
    } else {
      console.log("내근태에도 seg2 출근 버튼 미노출");
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
