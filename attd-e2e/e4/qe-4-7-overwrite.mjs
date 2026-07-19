// QE-4-7 step2 — 웹 Attd_05 8월 그리드에서 A 8/13(대기 연차 오버레이) 셀을 근무타입 QT9H(00003)로 덮어쓰기 시도
// node e4/qe-4-7-overwrite.mjs <표시명> <day> <schCd> <caseId>
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , uid = "QT테스터에이", dayStr = "13", schCd = "00003", caseId = "QE-4-7", monthStr = "8"] = process.argv;
const day = Number(dayStr), month = Number(monthStr);

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    const dialogs = [];
    page.on("dialog", async (d) => { dialogs.push(d.message()); await d.accept().catch(() => {}); });
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    // 8월 조회
    await page.evaluate((m) => {
      const el = document.querySelector("input.calendar-input");
      if (el && el._flatpickr) el._flatpickr.setDate(`2026-${String(m).padStart(2, "0")}-01`, true);
    }, month);
    await page.waitForTimeout(800);
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(3000);
    const cellInfo = await page.evaluate(({ u, dd }) => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes(u));
      if (!row) return "행 미발견: " + u;
      const tds = [...row.querySelectorAll("td")];
      const lastDay = new Date(2026, Number(document.body.innerText.match(/(\d{1,2})월/)?.[1] ?? 8), 0).getDate();
      const dayTds = tds.slice(-lastDay);
      const td = dayTds[dd - 1];
      if (!td) return `일 셀 미발견(day=${dd})`;
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: td.innerText.trim(), cls: td.className };
    }, { u: uid, dd: day });
    console.log(`${day}일 셀(전):`, JSON.stringify(cellInfo));
    if (typeof cellInfo === "string") throw new Error(cellInfo);
    await page.mouse.click(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(700);
    // 근무타입 선택 + 적용(근무타입 섹션 = 첫 btn-toolbar-apply, 법정휴가 제외)
    await page.selectOption("select.toolbar-sch-select", schCd);
    await page.waitForTimeout(400);
    const selLabel = await page.evaluate(() => document.querySelector(".toolbar-count-label")?.innerText || "");
    console.log("선택 라벨/건수:", selLabel);
    await page.locator("button.btn-toolbar-apply:not(.btn-toolbar-apply-leave)").first().click();
    await page.waitForTimeout(1200);
    const afterApply = await page.evaluate(({ u, dd }) => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes(u));
      const tds = [...row.querySelectorAll("td")];
      const lastDay = new Date(2026, 8, 0).getDate();
      const td = tds.slice(-lastDay)[dd - 1];
      const overlay = [...document.querySelectorAll(".modal-overlay")].at(-1);
      return { cell: td?.innerText.trim(), cls: td?.className, modal: overlay ? overlay.innerText.trim().replace(/\n/g, " | ") : null };
    }, { u: uid, dd: day });
    console.log("적용 직후 셀:", JSON.stringify(afterApply));
    await page.screenshot({ path: shotPath(caseId, "web", `apply-${day}`), fullPage: true });
    // 저장
    await page.locator('button:has-text("저장")').first().click();
    await page.waitForTimeout(2500);
    const saveModal = await page.evaluate(() => {
      const o = [...document.querySelectorAll(".modal-overlay")].at(-1);
      return o ? o.innerText.trim().replace(/\n/g, " | ") : "(모달 없음)";
    });
    console.log("저장 모달:", saveModal, "| dialogs:", JSON.stringify(dialogs));
    await page.screenshot({ path: shotPath(caseId, "web", `save-${day}`), fullPage: true });
    for (let i = 0; i < 3; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1000); }
    }
    // 저장 후 셀 재확인
    await page.waitForTimeout(1500);
    const finalCell = await page.evaluate(({ u, dd }) => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes(u));
      const tds = [...row.querySelectorAll("td")];
      const td = tds.slice(-31)[dd - 1];
      return { cell: td?.innerText.trim(), cls: td?.className };
    }, { u: uid, dd: day });
    console.log(`${day}일 셀(저장후):`, JSON.stringify(finalCell));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
