// QE-4-1 🛡 시간차 대기(REQ 2026071700162, G 7/29 1500~1530) 걸린 QT8H 시간 변경 차단(ATTD_400_162)
// 여정: 웹 Attd_01 → QT8H 행 더블클릭 → 적용일 내일(7/18) + 구간1 시작 09:00→09:30 → 저장 → 차단 얼럿 관찰
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openAttd01, openSchEdit, setPopupApplyDate, setPopupTime, savePopupAndCollect, readSchRow, closePopup } from "./lib-sch.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await openAttd01(page);
    console.log("QT8H 행(변경 전):", await readSchRow(page, "QT8H"));
    await openSchEdit(page, "QT8H");
    await page.screenshot({ path: shotPath("QE-4-1", "web", "popup-before") });

    // 적용일: 수정모드 FE 가드(오늘 이후만) → 내일로
    await setPopupApplyDate(page, "2026-07-18");
    // 구간1 시작 09:00 → 09:30 (TimeInput idx0 = 구간1 시작)
    await setPopupTime(page, 0, "09", "30");
    await page.screenshot({ path: shotPath("QE-4-1", "web", "popup-edited") });

    const { modalTexts, dialogs } = await savePopupAndCollect(page);
    console.log("모달 텍스트:", JSON.stringify(modalTexts, null, 1));
    console.log("네이티브 dialog:", JSON.stringify(dialogs));
    await page.screenshot({ path: shotPath("QE-4-1", "web", "after-save-attempt"), fullPage: true });

    await closePopup(page);
    // 목록 재조회로 화면상 불변 확인
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(2000);
    console.log("QT8H 행(시도 후):", await readSchRow(page, "QT8H"));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
