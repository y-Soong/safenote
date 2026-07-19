// E3 공용 — 앱 관찰: 내근태 이번달 캘린더 특정일 셀 + (옵션) 연차현황 요약.
// node e3/app-observe.mjs <USERID> <day> <caseId> <label> [withLeave]
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , userId, dayStr, caseId, label, withLeave = ""] = process.argv;
const day = Number(dayStr);

const waitLoaded = (p) => p.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 }).catch(() => {});

const main = async () => {
  try {
    const { page } = await appLogin(userId, "QtTest!2026");
    await waitLoaded(page);
    // 로그인 직후 동의/공지 시트 회피(나중에 버튼 있으면 닫기)
    const later = page.locator('button:has-text("나중에")');
    if (await later.count()) { await later.click().catch(() => {}); await page.waitForTimeout(800); }
    // 내근태 이번달 탭
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2500);
    await waitLoaded(page);
    await page.click('.attd-seg__item:has-text("이번달")');
    await page.waitForTimeout(2500);
    await waitLoaded(page);
    // 캘린더 일 셀 텍스트 채집(전체 innerText 중 해당 일 주변)
    const cellText = await page.evaluate((dd) => {
      const cells = [...document.querySelectorAll("button, td")].filter((el) => {
        const first = (el.innerText ?? "").trim().split("\n")[0];
        return first === String(dd);
      });
      return cells.map((c) => c.innerText.replace(/\n/g, "|")).join(" /// ");
    }, day);
    console.log(`=== ${userId} 이번달 캘린더 ${day}일 셀 ===`);
    console.log(cellText || "(셀 미발견)");
    // 일 셀 클릭 → 하단 상세
    await page.evaluate((dd) => {
      const cells = [...document.querySelectorAll("button, td")].filter((el) => {
        const first = (el.innerText ?? "").trim().split("\n")[0];
        return first === String(dd);
      });
      if (cells[0]) cells[0].click();
    }, day);
    await page.waitForTimeout(2000);
    const body = await page.evaluate(() => document.body.innerText);
    // 하단 상세부(캘린더 아래) 텍스트
    const di = body.lastIndexOf(String(day));
    console.log("=== 일 선택 후 하단(끝 600자) ===");
    console.log(body.slice(-600).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath(caseId, "app", `${label}-cal${day}`), fullPage: true });

    if (withLeave) {
      // 마이 → 연차 현황
      await page.click('button.app-tabbar__tab:has-text("마이")');
      await page.waitForTimeout(2000);
      await waitLoaded(page);
      const my = await page.evaluate(() => document.body.innerText);
      const li = my.indexOf("연차");
      console.log("=== 마이 연차 요약 ===");
      console.log(my.slice(li, li + 300).replace(/\n/g, " | "));
      await page.locator('[aria-label="연차 현황 보기"]').first().click();
      await page.waitForTimeout(2500);
      await waitLoaded(page);
      const lv = await page.evaluate(() => document.body.innerText);
      console.log("=== 연차현황 화면(앞 800자) ===");
      console.log(lv.slice(0, 800).replace(/\n/g, " | "));
      await page.screenshot({ path: shotPath(caseId, "app", `${label}-leave-summary`), fullPage: true });
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
