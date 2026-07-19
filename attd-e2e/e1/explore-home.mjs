// E1 사전 탐사 — QTUSERA 앱 홈 화면 구조 실측(출근 버튼 셀렉터 카탈로그용).
import { appLogin, closeAll } from "../lib/browser.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    console.log("URL:", page.url());
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== HOME innerText ===");
    console.log(text.slice(0, 1500));
    // 버튼 전수 덤프
    const btns = await page.evaluate(() =>
      [...document.querySelectorAll("button")].map((b) => ({
        cls: b.className,
        txt: b.innerText.replace(/\n/g, " ").trim().slice(0, 40),
        disabled: b.disabled,
      }))
    );
    console.log("=== BUTTONS ===");
    for (const b of btns) console.log(JSON.stringify(b));
    await page.screenshot({ path: "results/shots/E1_explore_home.png", fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
