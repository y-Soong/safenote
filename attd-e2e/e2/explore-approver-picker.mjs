// 결재자 피커 후보 탐색 — 초기 목록 + 검색어 변형별 결과 실측.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { openLeaveApply, selectLeaveType, bodyText, waitLoaded } from "./lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await waitLoaded(page);
    await openLeaveApply(page);
    await selectLeaveType(page, "연차");
    await page.locator("button.btn-add").click();
    await page.waitForTimeout(2000);
    let items = await page.locator("button.laps__item").allInnerTexts();
    console.log("=== 초기 후보 (", items.length, ") ===");
    console.log(items.map((t) => t.replace(/\n/g, " / ")).join("\n"));
    for (const kw of ["신입", "QT", "지"]) {
      await page.fill(".laps__search-input", kw);
      await page.waitForTimeout(2000);
      items = await page.locator("button.laps__item").allInnerTexts();
      console.log(`=== 검색 '${kw}' (${items.length}) ===`);
      console.log(items.map((t) => t.replace(/\n/g, " / ")).join("\n"));
    }
    const state = await page.locator(".laps__state").allInnerTexts();
    console.log("state:", state.join(" | "));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
