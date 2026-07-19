// QE-4-3 🔁 배정 존재 근무타입(QT2SEG 00004) 비활성 차단(ATTD_400_163)
// 미래 배정: D(20260700032) 오늘 7/17 1건(DB 실측). 팝업에서 사용여부 미사용(N) → 저장 → 차단 관찰.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openAttd01, openSchEdit, savePopupAndCollect, readSchRow, closePopup } from "./lib-sch.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await openAttd01(page);
    console.log("QT2SEG 행(전):", await readSchRow(page, "QT2SEG"));
    await openSchEdit(page, "QT2SEG");
    // 사용여부 select → N (SchInfoPop 내 select는 BaseSelect 1개뿐)
    const sel = page.locator(".modal-content-sch-info select");
    console.log("select 개수:", await sel.count());
    await sel.last().selectOption("N");
    await page.waitForTimeout(600);
    await page.screenshot({ path: shotPath("QE-4-3", "web", "popup-useYn-N") });
    const { modalTexts } = await savePopupAndCollect(page);
    console.log("모달:", JSON.stringify(modalTexts, null, 1));
    await page.screenshot({ path: shotPath("QE-4-3", "web", "after-save"), fullPage: true });
    await closePopup(page);
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(2000);
    console.log("QT2SEG 행(후):", await readSchRow(page, "QT2SEG"));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
