// QE-2-7 — D 동의 시트에서 [동의] 클릭.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { waitLoaded, bodyText, clickPopupOk } from "./lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERD", "QtTest!2026");
    await waitLoaded(page);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.waitForTimeout(2000);
    await page.screenshot({ path: shotPath("QE-2-7", "app", "consent-sheet"), fullPage: true });
    await page.locator('button:has-text("동의")').last().click();
    await page.waitForTimeout(2000);
    let text = await bodyText(page);
    console.log("=== 동의 클릭 후 ===");
    console.log(text.split("\n").slice(-20).join(" | "));
    await clickPopupOk(page, 3);
    text = await bodyText(page);
    console.log("=== 최종 홈 ===");
    console.log(text.slice(0, 700));
    await page.screenshot({ path: shotPath("QE-2-7", "app", "consent-done"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
