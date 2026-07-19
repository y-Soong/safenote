// QE-4-2 검증 — ① 웹 Attd_05 8월 그리드 셀 시각(8/12 이전 0900 vs 8/13 이후 0930, 소급 없음)
//              ② A 8/10 연차 표기 유지 ③ 앱(A) 8월 캘린더 표기
import { webLogin, appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    // ── 웹 Attd_05 8월 ──
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    await page.evaluate(() => {
      const el = document.querySelector("input.calendar-input");
      if (el && el._flatpickr) el._flatpickr.setDate("2026-08-01", true);
    });
    await page.waitForTimeout(800);
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(3000);
    // A/H 행의 일자 셀 텍스트(1~31) 채집
    const grid = await page.evaluate(() => {
      const out = {};
      for (const nm of ["QT사원에이", "QT사원H"]) {
        const row = [...document.querySelectorAll("tr")].find((r) => r.innerText.includes(nm));
        if (!row) { out[nm] = null; continue; }
        const tds = [...row.querySelectorAll("td")].slice(-31);
        out[nm] = tds.map((td, i) => `${i + 1}:${td.innerText.trim().replace(/\n/g, "/")}`).filter((s) => !s.endsWith(":"));
      }
      return out;
    });
    console.log("=== Attd_05 8월 A행 ===");
    console.log(JSON.stringify(grid["QT사원에이"]));
    console.log("=== Attd_05 8월 H행 ===");
    console.log(JSON.stringify(grid["QT사원H"]));
    await page.screenshot({ path: shotPath("QE-4-2", "web", "attd05-aug-after"), fullPage: true });

    // ── 앱 A 8월 캘린더 ──
    const { page: app } = await appLogin("QTUSERA", "QtTest!2026");
    await app.click('button.app-tabbar__tab:has-text("근태")');
    await app.waitForTimeout(2500);
    await app.locator('.attd-seg__item:has-text("이번달")').click();
    await app.waitForTimeout(2000);
    await app.locator('button[aria-label="다음 달"]').click();
    await app.waitForTimeout(2000);
    // 8/10(연차), 8/12(전 시각), 8/13·8/14(신 시각) 셀 상세
    for (const d of [10, 12, 13]) {
      await app.evaluate((dd) => {
        const cells = [...document.querySelectorAll("button, td")];
        const c = cells.find((x) => (x.innerText ?? "").trim().split("\n")[0] === String(dd) && x.className.includes("cal"));
        if (c) c.click();
      }, d);
      await app.waitForTimeout(1500);
      const detail = await app.evaluate(() => document.body.innerText);
      const anchor = detail.indexOf("상세") >= 0 ? detail.indexOf("상세") : 0;
      console.log(`=== 앱 8/${d} 선택 후 하단 ===`);
      console.log(detail.slice(-700).replace(/\n/g, " | "));
      await app.screenshot({ path: shotPath("QE-4-2", "app", `cal-aug-${d}`), fullPage: true });
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
