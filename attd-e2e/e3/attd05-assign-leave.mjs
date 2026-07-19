// 웹 Attd_05 법정휴가 자동차감 배정 공용 — node e3/attd05-assign-leave.mjs <USERID표시명> <day> <caseId> [month(기본7)]
// qe-2-6-assign.mjs 일반화판. 셀 선택 → 두번째 [적용](법정 휴가) → 저장.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , uid, dayStr, caseId, monthStr = "7"] = process.argv;
const day = Number(dayStr);
const month = Number(monthStr);

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    // 조회월 = flatpickr → 대상 월 세팅 후 조회
    if (month !== 7) {
      await page.evaluate((m) => {
        const el = document.querySelector("input.calendar-input");
        if (el && el._flatpickr) el._flatpickr.setDate(`2026-${String(m).padStart(2, "0")}-01`, true);
      }, month);
      await page.waitForTimeout(800);
      await page.click('button:has-text("조회")');
      await page.waitForTimeout(3000);
    }
    const header = await page.evaluate(() => document.body.innerText.match(/\d{4}년 \d{1,2}월|\d{4}-\d{2}/)?.[0] ?? "(월 표기 미발견)");
    console.log("그리드 월:", header);
    const cellInfo = await page.evaluate(({ u, dd }) => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes(u));
      if (!row) return "행 미발견";
      const tds = [...row.querySelectorAll("td")];
      const dayCount = tds.length; // 마지막 N개가 일자 셀 — 월별 말일 기준 31/30
      const lastDay = new Date(2026, Number(document.body.innerText.match(/(\d{1,2})월/)?.[1] ?? 7), 0).getDate();
      const dayTds = tds.slice(-lastDay);
      const td = dayTds[dd - 1];
      if (!td) return `일 셀 미발견(day=${dd}, 셀수=${dayTds.length})`;
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: td.innerText.trim(), cls: td.className };
    }, { u: uid, dd: day });
    console.log(`${day}일 셀:`, JSON.stringify(cellInfo));
    if (typeof cellInfo === "string") throw new Error(cellInfo);
    await page.mouse.click(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(800);
    const applyBtns = page.locator('button:has-text("적용")');
    await applyBtns.nth(1).click();
    await page.waitForTimeout(1500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("적용 직후 하단:", text.split("\n").slice(-8).join(" | "));
    const di = text.indexOf(uid);
    console.log("적용 직후 행:", text.slice(di, di + 300).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath(caseId, "web", `attd05-apply-${day}`), fullPage: true });
    await page.click('button:has-text("저장")');
    await page.waitForTimeout(2500);
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
    }
    text = await page.evaluate(() => document.body.innerText);
    const di2 = text.indexOf(uid);
    console.log("저장 후 행:", text.slice(di2, di2 + 300).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath(caseId, "web", `attd05-saved-${day}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
