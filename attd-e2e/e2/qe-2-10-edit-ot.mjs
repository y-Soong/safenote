// QE-2-10 ① — 기저장 OT행 인플레이스 수정: 종료 10:00→10:30. node e2/qe-2-10-edit-ot.mjs <이름> <dayIdx0> <신규종료HHMM> <라벨>
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , who = "QT자기승인씨", dayIdxStr = "14", newEnd = "1030", label = "edit1"] = process.argv;
const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_07", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    await page.locator('button:has-text("조회")').first().click();
    await page.waitForTimeout(3000);
    const cellInfo = await page.evaluate(({ nm, di }) => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes(nm));
      const td = [...row.querySelectorAll("td.m-day-cell")][di];
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
    }, { nm: who, di: Number(dayIdxStr) });
    await page.mouse.dblclick(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    const oi = text.indexOf("초과근무");
    console.log("=== 팝업 OT 영역 ===");
    console.log(text.slice(oi, oi + 600));
    // OT 행 입력 실측
    const ot = page.locator("input.ot-time:visible");
    const n = await ot.count();
    console.log("ot-time 입력 수:", n);
    for (let i = 0; i < n; i++) console.log(`  [${i}]`, await ot.nth(i).inputValue());
    if (n === 0) { console.log("OT 행 없음 — 편집 불가 관찰"); await page.screenshot({ path: shotPath("QE-2-10", "web", `no-ot-row-${label}`), fullPage: true }); return; }
    // 기저장행 체크박스(편집 대상 지정) — OT 행 근처 체크박스 탐색 후 체크
    await page.evaluate(() => {
      const otInput = document.querySelector("input.ot-time");
      let el = otInput;
      for (let k = 0; k < 6 && el; k++) {
        el = el.parentElement;
        const cb = el?.querySelector('input[type="checkbox"]');
        if (cb && !cb.checked) { cb.click(); return "checked"; }
        if (cb && cb.checked) return "already";
      }
      return "cb-none";
    }).then((r) => console.log("체크박스:", r));
    await page.waitForTimeout(500);
    // 종료 수정
    const endInput = ot.nth(n - 1);
    await endInput.click({ clickCount: 3 });
    await endInput.press("Control+a");
    await endInput.pressSequentially(newEnd, { delay: 60 });
    await endInput.press("Tab");
    await page.waitForTimeout(500);
    console.log("수정 후 값:", await endInput.inputValue());
    await page.screenshot({ path: shotPath("QE-2-10", "web", `edited-${label}`), fullPage: true });
    const saveBtn = page.locator('button:has-text("초과근무 저장")');
    console.log("저장 disabled=", await saveBtn.isDisabled());
    await saveBtn.click({ timeout: 10000 });
    await page.waitForTimeout(2500);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 저장 응답 ===");
    console.log(text.split("\n").slice(-12).join(" | "));
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
    }
    text = await page.evaluate(() => document.body.innerText);
    const oi2 = text.indexOf("초과근무");
    console.log("=== 저장 후 ===");
    console.log(text.slice(oi2, oi2 + 400));
    await page.screenshot({ path: shotPath("QE-2-10", "web", `saved-${label}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
