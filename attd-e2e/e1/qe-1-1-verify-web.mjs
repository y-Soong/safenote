// QE-1-1 웹 면 — QTHR 웹 Attd_08(근로자 근태조회) 당일 QTUSERA 출근시각 확인.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_08", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== Attd_08 초기 ===");
    console.log(text.slice(0, 1600));
    await page.screenshot({ path: shotPath("QE-1-1", "web", "attd08"), fullPage: true });

    // QTUSERA 검색 시도 — 입력 필드 덤프
    const inputs = await page.evaluate(() =>
      [...document.querySelectorAll("input, select")].map((i) => ({
        tag: i.tagName, type: i.type, ph: i.placeholder || "", cls: (i.className || "").slice(0, 40), val: (i.value || "").slice(0, 20),
      }))
    );
    console.log("=== INPUTS ===");
    for (const i of inputs) console.log(JSON.stringify(i));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
