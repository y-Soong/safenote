// QE-4-12 step2 — Attd_05 D 오늘(7/17, 휴일) 셀 타입 교체(휴일 포함 모드) — 기존 근태 있는 오늘 셀 변경 관찰
// node e4/qe-4-12-swap.mjs <schCd>
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , schCd = "00001"] = process.argv;
const uid = "QT교대디", day = 17;

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    const dialogs = [], pageerrs = [];
    page.on("dialog", async (d) => { dialogs.push(d.message()); await d.accept().catch(() => {}); });
    page.on("pageerror", (e) => pageerrs.push(e.message));
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const cellInfo = await page.evaluate(({ u, dd }) => {
      const row = [...document.querySelectorAll("tr")].find((r) => r.innerText.includes(u));
      const tds = [...row.querySelectorAll("td")].slice(-31);
      const td = tds[dd - 1]; td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: td.innerText.trim(), cls: td.className };
    }, { u: uid, dd: day });
    console.log("오늘 셀(전):", JSON.stringify(cellInfo));
    await page.mouse.click(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(600);
    // 휴일 포함 라디오 선택
    await page.locator('input[type=radio][value="include"]').first().check().catch(() => {});
    await page.waitForTimeout(300);
    await page.selectOption("select.toolbar-sch-select", schCd);
    await page.waitForTimeout(300);
    await page.locator("button.btn-toolbar-apply:not(.btn-toolbar-apply-leave)").first().click();
    await page.waitForTimeout(1000);
    const afterApply = await page.evaluate(({ u, dd }) => {
      const row = [...document.querySelectorAll("tr")].find((r) => r.innerText.includes(u));
      const td = [...row.querySelectorAll("td")].slice(-31)[dd - 1];
      return { cell: td?.innerText.trim(), cls: td?.className };
    }, { u: uid, dd: day });
    console.log("적용 직후:", JSON.stringify(afterApply));
    await page.locator('button:has-text("저장")').first().click();
    await page.waitForTimeout(1500);
    const confirmModal = await page.evaluate(() => {
      const o = [...document.querySelectorAll(".modal-overlay")].at(-1);
      return o ? o.innerText.trim().replace(/\n/g, " | ") : "(모달 없음)";
    });
    console.log("확인 모달:", confirmModal);
    // 저장하시겠습니까 → 확인
    await page.locator('.modal-overlay button:has-text("확인")').last().click().catch(() => {});
    await page.waitForTimeout(2500);
    const resultModal = await page.evaluate(() => {
      const o = [...document.querySelectorAll(".modal-overlay")].at(-1);
      return o ? o.innerText.trim().replace(/\n/g, " | ") : "(모달 없음)";
    });
    console.log("결과 모달:", resultModal, "| dialogs:", JSON.stringify(dialogs), "| pageerrors:", JSON.stringify(pageerrs));
    for (let i = 0; i < 3; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1000); }
    }
    await page.waitForTimeout(1200);
    const finalCell = await page.evaluate(({ u, dd }) => {
      const row = [...document.querySelectorAll("tr")].find((r) => r.innerText.includes(u));
      const td = [...row.querySelectorAll("td")].slice(-31)[dd - 1];
      return { cell: td?.innerText.trim(), cls: td?.className };
    }, { u: uid, dd: day });
    console.log("오늘 셀(저장후):", JSON.stringify(finalCell));
    await page.screenshot({ path: shotPath("QE-4-12", "web", `swap-${schCd}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
