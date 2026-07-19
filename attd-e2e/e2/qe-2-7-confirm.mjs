// QE-2-7 — 웹 Attd_13에서 AGREED 건 확인(confirm) → Attd_14 이력 확인.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_13", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    // QT교대디 행의 활성 '확인' 버튼
    const row = page.locator("tr", { hasText: "QT교대디" }).first();
    console.log("행 텍스트:", (await row.innerText()).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-2-7", "web", "attd13-agreed"), fullPage: true });
    await row.locator('button:has-text("확인")').click();
    await page.waitForTimeout(2000);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 확인 팝업 ===");
    console.log(text.slice(text.length - 1500));
    await page.screenshot({ path: shotPath("QE-2-7", "web", "confirm-pop"), fullPage: true });
    // 팝업 내 확정 버튼 추정 클릭
    for (const sel of ['button:has-text("확정")', 'button:has-text("반영")', 'button:has-text("확인")']) {
      const b = page.locator(`.modal ${sel}, .popup ${sel}, ${sel}`).last();
      if (await b.count()) { console.log("클릭:", sel); await b.click(); break; }
    }
    await page.waitForTimeout(2500);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 확정 응답 ===");
    console.log(text.split("\n").slice(-14).join(" | "));
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
    }
    await page.screenshot({ path: shotPath("QE-2-7", "web", "confirm-done"), fullPage: true });
    // Attd_14 이력
    await page.goto("http://localhost:8081/safenote/main/Attd_14", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== Attd_14 ===");
    const idx = text.indexOf("연차 변경 요청 이력");
    console.log(text.slice(idx >= 0 ? idx : 0).slice(0, 2000));
    await page.screenshot({ path: shotPath("QE-2-7", "web", "attd14"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
