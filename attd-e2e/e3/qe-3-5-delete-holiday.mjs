// QE-3-5 — 웹 휴일관리에서 제헌절(공휴일, 7/17) 삭제 시도. 캘린더 17일 선택 → 상세 패널 실측 → 삭제 수단 탐색.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openAttd02, gotoMonth, selectDay, bodyText } from "./lib-holiday.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    const dialogs = [];
    page.on("dialog", async (d) => { dialogs.push(d.message()); await d.accept(); });
    await openAttd02(page);
    await gotoMonth(page, 2026, 7);
    await selectDay(page, 17);
    // 상세 탭
    await page.click('button:has-text("상세")');
    await page.waitForTimeout(1200);
    let t = await bodyText(page);
    const di = t.indexOf("상세");
    console.log("=== 상세 패널 ===");
    console.log(t.slice(di, di + 700).replace(/\n/g, " | "));
    const vis = await page.evaluate(() =>
      [...document.querySelectorAll("button")].filter((b) => b.offsetParent).map((b) => ({ t: b.innerText.trim(), dis: b.disabled })).filter((b) => b.t)
    );
    console.log("보이는 버튼:", JSON.stringify(vis));
    await page.screenshot({ path: shotPath("QE-3-5", "web", "attd02-detail-17"), fullPage: true });
    // 상세 패널 내 삭제/해제 계열 버튼 시도
    const del = page.locator('button:has-text("삭제"), button:has-text("해제"), button:has-text("휴무 해제")').first();
    if ((await del.count()) === 0) { console.log("삭제/해제 버튼 자체 없음 — 공휴일 UI 삭제 불가"); return; }
    console.log("삭제 버튼 발견 — 클릭");
    await del.click();
    await page.waitForTimeout(1500);
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1200); }
    }
    console.log("dialog:", JSON.stringify(dialogs));
    t = await bodyText(page);
    const ci = t.indexOf("17");
    console.log("=== 삭제 후 캘린더 17 주변 ===");
    console.log(t.slice(t.indexOf("14"), t.indexOf("14") + 200).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-3-5", "web", "attd02-after-delete-attempt"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
