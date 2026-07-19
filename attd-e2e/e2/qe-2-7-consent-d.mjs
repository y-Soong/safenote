// QE-2-7 — 앱(QTUSERD) 연차 변경 동의 카드 확인 → 동의.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { waitLoaded, bodyText, clickPopupOk } from "./lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERD", "QtTest!2026");
    await waitLoaded(page);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    let text = await bodyText(page);
    console.log("=== D 홈 ===");
    console.log(text.slice(0, 1600));
    await page.screenshot({ path: shotPath("QE-2-7", "app", "d-home"), fullPage: true });
    // 동의 관련 카드/배너 탐색
    const hit = await page.evaluate(() => {
      const els = [...document.querySelectorAll("button, a, [role='button'], div")];
      const el = els.find((e) => e.innerText && /동의|변경 요청|삭제 요청/.test(e.innerText) && e.innerText.length < 200);
      if (!el) return null;
      const t = el.innerText.replace(/\n/g, "|");
      el.click();
      return t;
    });
    console.log("동의 카드:", hit);
    await page.waitForTimeout(2500);
    await waitLoaded(page);
    text = await bodyText(page);
    console.log("=== 진입 화면 ===");
    console.log(text.slice(0, 1800));
    await page.screenshot({ path: shotPath("QE-2-7", "app", "d-consent-view"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
