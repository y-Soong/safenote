// 범용 Attd_05 셀 근무타입 교체(휴일 포함 모드) — node e4/swap-cell.mjs <표시명> <month> <day> <schCd> <caseId>
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , uid, monthStr, dayStr, schCd, caseId = "QE-x"] = process.argv;
const month = Number(monthStr), day = Number(dayStr);
const lastDay = new Date(2026, month, 0).getDate();

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    const dialogs = [], pageerrs = [];
    page.on("dialog", async (d) => { dialogs.push(d.message()); await d.accept().catch(() => {}); });
    page.on("pageerror", (e) => pageerrs.push(e.message));
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    if (month !== 7) {
      await page.evaluate((m) => { const el = document.querySelector("input.calendar-input"); if (el?._flatpickr) el._flatpickr.setDate(`2026-${String(m).padStart(2,"0")}-01`, true); }, month);
      await page.waitForTimeout(700);
      await page.click('button:has-text("조회")');
      await page.waitForTimeout(2500);
    }
    const cell = await page.evaluate(({ u, dd, ld }) => {
      const row = [...document.querySelectorAll("tr")].find((r) => r.innerText.includes(u));
      if (!row) return "행 미발견";
      const td = [...row.querySelectorAll("td")].slice(-ld)[dd - 1]; td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: td.innerText.trim(), cls: td.className };
    }, { u: uid, dd: day, ld: lastDay });
    console.log("셀(전):", JSON.stringify(cell));
    if (typeof cell === "string") throw new Error(cell);
    await page.mouse.click(cell.x, cell.y);
    await page.waitForTimeout(600);
    await page.locator('input[type=radio][value="include"]').first().check().catch(() => {});
    await page.waitForTimeout(300);
    await page.selectOption("select.toolbar-sch-select", schCd);
    await page.waitForTimeout(300);
    await page.locator("button.btn-toolbar-apply:not(.btn-toolbar-apply-leave)").first().click();
    await page.waitForTimeout(900);
    await page.locator('button:has-text("저장")').first().click();
    await page.waitForTimeout(1300);
    console.log("확인모달:", await page.evaluate(() => [...document.querySelectorAll(".modal-overlay")].at(-1)?.innerText.trim().replace(/\n/g," | ") || "-"));
    await page.locator('.modal-overlay button:has-text("확인")').last().click().catch(() => {});
    await page.waitForTimeout(2500);
    console.log("결과모달:", await page.evaluate(() => [...document.querySelectorAll(".modal-overlay")].at(-1)?.innerText.trim().replace(/\n/g," | ") || "-"), "| dialogs:", JSON.stringify(dialogs), "| pageerrors:", JSON.stringify(pageerrs));
    for (let i = 0; i < 3; i++) { const ok = page.locator("button:has-text('확인')").first(); if (await ok.count()) { await ok.click().catch(()=>{}); await page.waitForTimeout(800);} }
    await page.waitForTimeout(1000);
    const fin = await page.evaluate(({ u, dd, ld }) => {
      const row = [...document.querySelectorAll("tr")].find((r) => r.innerText.includes(u));
      const td = [...row.querySelectorAll("td")].slice(-ld)[dd - 1];
      return { cell: td?.innerText.trim(), cls: td?.className };
    }, { u: uid, dd: day, ld: lastDay });
    console.log("셀(후):", JSON.stringify(fin));
    await page.screenshot({ path: shotPath(caseId, "web", `swap-${month}-${day}-${schCd}`), fullPage: true });
  } catch (e) { console.log("FAIL:", e.message); process.exitCode = 1; } finally { await closeAll(); }
};
main();
