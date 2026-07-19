// E3 — 휴일 등록 팝업 + 목록/상세 패널 실측.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_02", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    // 1) 목록 탭
    await page.click('button:has-text("목록")');
    await page.waitForTimeout(1200);
    let text = await page.evaluate(() => document.body.innerText);
    const li = text.indexOf("목록");
    console.log("=== 목록 패널 ===");
    console.log(text.slice(li, li + 900));
    await page.screenshot({ path: shotPath("E3-explore", "web", "attd02-list"), fullPage: true });
    // 2) 등록 팝업
    await page.click('button:has-text("휴일 등록")');
    await page.waitForTimeout(1500);
    text = await page.evaluate(() => document.body.innerText);
    const pi = text.lastIndexOf("휴일 등록");
    console.log("=== 등록 팝업 텍스트 ===");
    console.log(text.slice(Math.max(0, pi - 200), pi + 900));
    const inputs = await page.evaluate(() =>
      [...document.querySelectorAll("input:not([type=hidden]), select, textarea")].filter((i) => i.offsetParent).map((i) => ({
        tag: i.tagName, type: i.type ?? "", ph: i.placeholder ?? "", cls: i.className.slice(0, 70), val: (i.value ?? "").slice(0, 20),
      }))
    );
    console.log("=== 보이는 입력 ===", JSON.stringify(inputs, null, 1));
    const btns = await page.evaluate(() =>
      [...document.querySelectorAll("button")].filter((b) => b.offsetParent).map((b) => b.innerText.trim()).filter(Boolean)
    );
    console.log("=== 보이는 버튼 ===", JSON.stringify(btns));
    // select 옵션
    const selects = await page.evaluate(() =>
      [...document.querySelectorAll("select")].filter((s) => s.offsetParent).map((s) => [...s.options].map((o) => `${o.value}:${o.text}`))
    );
    console.log("=== select 옵션 ===", JSON.stringify(selects));
    await page.screenshot({ path: shotPath("E3-explore", "web", "attd02-regpop"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
