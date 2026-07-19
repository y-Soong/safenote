// Attd_05 상단 조회 조건/월 내비 실측.
import { webLogin, closeAll } from "../lib/browser.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const controls = await page.evaluate(() => {
      const els = [...document.querySelectorAll("input, select, button")].filter((e) => e.offsetParent);
      return els.slice(0, 40).map((e) => ({
        tag: e.tagName, type: e.type ?? "", cls: e.className.slice(0, 50), txt: (e.innerText ?? e.value ?? "").trim().slice(0, 30), ph: e.placeholder ?? "",
      }));
    });
    console.log(JSON.stringify(controls, null, 1));
    const text = await page.evaluate(() => document.body.innerText.slice(0, 600));
    console.log("=== 상단 텍스트 ===");
    console.log(text.replace(/\n/g, " | "));
  } catch (e) {
    console.log("FAIL:", e.message);
  } finally {
    await closeAll();
  }
};
main();
