// QE-4-2 📋 종일 연차일 포함 기간의 근무타입 적용일 변경
// 사용법: node e4/qe-4-2-applydate-change.mjs <apply일 YYYY-MM-DD> <시 HH> <분 MM> <라벨>
// 1차: 2026-08-09 09 30 (지시서 원안 — A 8/12 시간차 LV080이 창에 포함돼 162 예상)
// 2차: 2026-08-13 09 30 (격리판 — 창 내 시간차 없음·H 8/18 종일 연차만 → 종일 차단 여부 판별)
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openAttd01, openSchEdit, setPopupApplyDate, setPopupTime, savePopupAndCollect, readSchRow, closePopup } from "./lib-sch.mjs";

const [, , applyDate, hh, mm, label] = process.argv;

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await openAttd01(page);
    console.log("QT8H 행(전):", await readSchRow(page, "QT8H"));
    await openSchEdit(page, "QT8H");
    await setPopupApplyDate(page, applyDate);
    await setPopupTime(page, 0, hh, mm);
    await page.screenshot({ path: shotPath("QE-4-2", "web", `popup-${label}`) });
    const { modalTexts, dialogs } = await savePopupAndCollect(page);
    console.log("모달:", JSON.stringify(modalTexts, null, 1));
    if (dialogs.length) console.log("dialog:", JSON.stringify(dialogs));
    await page.screenshot({ path: shotPath("QE-4-2", "web", `after-${label}`), fullPage: true });
    await closePopup(page);
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(2000);
    console.log("QT8H 행(후):", await readSchRow(page, "QT8H"));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
