// QE-1-11 — 지오펜스 밖 좌표(35.0,128.0)에서 QTUSERC 출근 시도 → 차단/경고 관찰.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { clickPopupOk } from "./lib-form.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERC", "QtTest!2026", { geo: { latitude: 35.0, longitude: 128.0 } });
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.screenshot({ path: shotPath("QE-1-11", "app", "home-before") });
    const btn = page.locator('button:has-text("출근하기")');
    console.log("출근하기 disabled=", await btn.isDisabled());
    await btn.click();
    await page.waitForTimeout(1800);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 출근 클릭 직후 ===");
    console.log(text.split("\n").slice(-18).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-11", "app", "attempt") });
    // 확인 팝업(출근하시겠어요?) 진행 → 서버/클라 지오펜스 반응 관찰
    const ok = page.locator("button:has-text('확인')").first();
    if (await ok.count()) {
      await ok.click().catch(() => {});
      await page.waitForTimeout(2500);
      text = await page.evaluate(() => document.body.innerText);
      console.log("=== 확인 후(차단 기대) ===");
      console.log(text.split("\n").slice(-18).join(" | "));
      await page.screenshot({ path: shotPath("QE-1-11", "app", "blocked") });
      await clickPopupOk(page, 1);
    }
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 홈 최종 ===");
    console.log(text.slice(0, 350));
    await page.screenshot({ path: shotPath("QE-1-11", "app", "home-after") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
