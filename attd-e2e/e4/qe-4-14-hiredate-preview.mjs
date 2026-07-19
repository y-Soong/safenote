// QE-4-14 — G 입사일 변경 팝업 hire-date-impact 프리뷰 관찰(★저장 절대 금지★)
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const NAME = "QT신입지"; // G

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/User_01", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    // 조회 버튼 있으면 클릭(사용자 목록 로드)
    const q = page.locator('button:has-text("조회")').first();
    if (await q.count()) { await q.click().catch(() => {}); await page.waitForTimeout(2500); }
    // G 행 존재 확인
    const hasG = await page.evaluate((n) => [...document.querySelectorAll("tbody tr")].some((r) => r.innerText.includes(n)), NAME);
    console.log("G 행 존재:", hasG);
    if (!hasG) { console.log("목록에 G 미표시 — 상단 텍스트:", (await page.evaluate(() => document.body.innerText)).slice(0, 400).replace(/\n/g, " | ")); }
    // G 행 dblclick → UserInfoPop
    await page.evaluate((n) => {
      const row = [...document.querySelectorAll("tbody tr")].find((r) => r.innerText.includes(n));
      if (row) { const td = row.querySelector("td:nth-child(3)") || row.querySelector("td"); td.dispatchEvent(new MouseEvent("dblclick", { bubbles: true })); }
    }, NAME);
    await page.waitForTimeout(2500);
    console.log("UserInfoPop 열림:", await page.locator('button:has-text("입사일 수정")').count() > 0);
    await page.screenshot({ path: shotPath("QE-4-14", "web", "userinfo"), fullPage: true });
    // 입사일 수정 버튼
    await page.locator('button:has-text("입사일 수정")').first().click();
    await page.waitForTimeout(2000);
    // 기존 입사일 채집
    const prev = await page.evaluate(() => {
      const t = document.body.innerText;
      const m = t.match(/기존 입사일[\s\S]{0,40}/);
      return m ? m[0].replace(/\n/g, " ") : "(미발견)";
    });
    console.log("기존 입사일 영역:", prev);
    // newHireDate flatpickr — 팝업 내 calendar-input(마지막이 변경할 입사일일 가능성). 여러 시나리오 관찰.
    for (const testDate of ["2023-01-01", "2026-07-01", "2027-01-01"]) {
      await page.evaluate((v) => {
        const inps = [...document.querySelectorAll(".hire-date-pop input.calendar-input, input.calendar-input")];
        const inp = inps[inps.length - 1];
        if (inp?._flatpickr) inp._flatpickr.setDate(v, true);
      }, testDate);
      await page.waitForTimeout(2500); // impact API 로딩 대기
      const impact = await page.evaluate(() => {
        const grab = (sel) => document.querySelector(sel)?.innerText.trim() || null;
        const cards = [...document.querySelectorAll(".hire-date-pop__impact-card")].map((c) => c.innerText.replace(/\n/g, " ").trim());
        return {
          scenario: grab(".hire-date-pop__impact-scenario") || document.querySelector('[class*="scenario"]')?.innerText.trim(),
          cards,
          note: grab(".hire-date-pop__impact-note"),
        };
      });
      console.log(`[newHireDate=${testDate}] impact:`, JSON.stringify(impact));
      await page.screenshot({ path: shotPath("QE-4-14", "web", `impact-${testDate}`), fullPage: true });
    }
    console.log("★ 저장 미클릭(조회/프리뷰만) — 팝업 닫기");
    await page.locator('button:has-text("취소")').first().click().catch(() => {});
    await page.waitForTimeout(800);
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
