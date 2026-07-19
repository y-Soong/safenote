// QE-2-4 시드 2 — 수동 부여 팝업 작성 → 부여하기 (포상휴가 1일, [QE-2-4]).
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_09", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2500);
    const q = page.locator('button:has-text("조회")').first();
    if (await q.count()) { await q.click(); await page.waitForTimeout(2500); }
    await page.evaluate(() => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes("QT사원H"));
      const cb = row.querySelector('input[type="checkbox"]');
      cb.click();
    });
    await page.waitForTimeout(800);
    await page.click('button:has-text("일괄 수동 부여")');
    await page.waitForTimeout(2000);
    // 부여 유형 select — 팝업 내 select
    await page.locator("select:visible").last().selectOption({ label: "포상휴가" });
    await page.waitForTimeout(500);
    await page.locator('input[type="number"]:visible').last().fill("1");
    // 사용 가능일(flatpickr) — input에 직접 값 주입
    await page.evaluate(() => {
      const el = document.querySelector("input.calendar-input");
      if (el && el._flatpickr) { el._flatpickr.setDate("2026-08-18", true); }
      else if (el) { el.value = "2026-08-18"; el.dispatchEvent(new Event("input", { bubbles: true })); el.dispatchEvent(new Event("change", { bubbles: true })); }
    });
    await page.waitForTimeout(500);
    const ta = page.locator("textarea:visible").last();
    if (await ta.count()) await ta.fill("[QE-2-4] 잔여부족 가드 시드 포상 1일");
    await page.screenshot({ path: shotPath("QE-2-4", "web", "grant-filled"), fullPage: true });
    await page.click('button:has-text("부여하기")');
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 부여 후 ===");
    console.log(text.split("\n").slice(-15).join(" | "));
    // 확인 팝업 처리
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
    }
    text = await page.evaluate(() => document.body.innerText);
    const idx = text.indexOf("QT사원H");
    console.log("=== H 행 ===");
    console.log(text.slice(idx, idx + 200));
    await page.screenshot({ path: shotPath("QE-2-4", "web", "grant-done"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
