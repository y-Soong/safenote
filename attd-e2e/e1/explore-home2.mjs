// E1 사전 탐사 v2 — 홈 카드 로딩 완료 대기 후 구조 실측.
import { appLogin, closeAll } from "../lib/browser.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    // 로딩 문구 소멸 대기(최대 20초)
    try {
      await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    } catch { console.log("(경고) 20초 후에도 로딩 중"); }
    await page.waitForTimeout(1000);
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== HOME innerText ===");
    console.log(text.slice(0, 2000));
    const btns = await page.evaluate(() =>
      [...document.querySelectorAll("button")].map((b) => ({
        cls: b.className, txt: b.innerText.replace(/\n/g, " ").trim().slice(0, 50), disabled: b.disabled,
      }))
    );
    console.log("=== BUTTONS ===");
    for (const b of btns) console.log(JSON.stringify(b));
    await page.screenshot({ path: "results/shots/E1_explore_home2.png", fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
