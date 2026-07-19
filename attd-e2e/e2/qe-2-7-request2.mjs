// QE-2-7 — 팝업에서 삭제(DELETE) 선택 + 사유 입력 → 요청 제출.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const cellInfo = await page.evaluate(() => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes("QTUSERD"));
      const tds = [...row.querySelectorAll("td")];
      const td = tds.slice(-31)[29];
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
    });
    await page.mouse.dblclick(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(2000);
    await page.locator('input[type="radio"][value="DELETE"]').check();
    await page.waitForTimeout(500);
    await page.fill("textarea:visible", "[QE-2-7] 관리자 삭제 요청 - 동의 흐름 E2E");
    await page.screenshot({ path: shotPath("QE-2-7", "web", "request-filled"), fullPage: true });
    await page.locator('button:has-text("요청")').last().click();
    await page.waitForTimeout(2000);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 제출 응답 ===");
    console.log(text.split("\n").slice(-12).join(" | "));
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
    }
    await page.screenshot({ path: shotPath("QE-2-7", "web", "request-submitted"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
