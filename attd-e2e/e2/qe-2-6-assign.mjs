// QE-2-6 — 웹 Attd_05: QTUSERD 7/30 셀 선택 → 법정 휴가 적용 → 저장(자동차감).
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    // D 행의 30일 셀 클릭 (마지막 31개 td = 1~31일)
    const cellInfo = await page.evaluate(() => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes("QTUSERD"));
      if (!row) return "행 미발견";
      const tds = [...row.querySelectorAll("td")];
      const dayTds = tds.slice(-31);
      const td = dayTds[29]; // 30일
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: td.innerText.trim(), cls: td.className };
    });
    console.log("30일 셀:", JSON.stringify(cellInfo));
    if (typeof cellInfo === "string") throw new Error(cellInfo);
    await page.mouse.click(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(800);
    await page.screenshot({ path: shotPath("QE-2-6", "web", "cell-selected") });
    // '법정 휴가' 라벨 옆 적용 버튼 = 두번째 '적용'
    const applyBtns = page.locator('button:has-text("적용")');
    console.log("적용 버튼 수:", await applyBtns.count());
    await applyBtns.nth(1).click();
    await page.waitForTimeout(1500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 적용 직후 (D 행) ===");
    const di = text.indexOf("QTUSERD");
    console.log(text.slice(di, di + 400).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-2-6", "web", "after-apply"), fullPage: true });
    // 저장
    await page.click('button:has-text("저장")');
    await page.waitForTimeout(2500);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 저장 응답 ===");
    console.log(text.split("\n").slice(-12).join(" | "));
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
    }
    text = await page.evaluate(() => document.body.innerText);
    const di2 = text.indexOf("QTUSERD");
    console.log("=== 저장 후 D 행 ===");
    console.log(text.slice(di2, di2 + 400).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-2-6", "web", "after-save"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
