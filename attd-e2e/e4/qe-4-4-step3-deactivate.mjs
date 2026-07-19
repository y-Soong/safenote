// QE-4-4 step3 — QE9H(00006) 비활성(사용여부 N). 미래 배정 없음(7/16 과거만) → 성공 예상.
// 수정모드 FE 가드로 적용일은 내일(2026-07-18)로 세팅해야 저장 가능.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openAttd01, openSchEdit, setPopupApplyDate, savePopupAndCollect, readSchRow, closePopup } from "./lib-sch.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await openAttd01(page);
    await openSchEdit(page, "QE9H");
    await setPopupApplyDate(page, "2026-07-18");
    await page.locator(".modal-content-sch-info select").last().selectOption("N");
    await page.waitForTimeout(600);
    const { modalTexts } = await savePopupAndCollect(page);
    console.log("모달:", JSON.stringify(modalTexts));
    await page.screenshot({ path: shotPath("QE-4-4", "web", "deactivated"), fullPage: true });
    await closePopup(page);
    // 미사용 필터로 확인
    await page.selectOption('.viewSearch select >> nth=1', "N").catch(async () => {
      // 사용유무 select 위치 폴백: 두 select 중 마지막
      const sels = page.locator(".viewSearch select");
      await sels.last().selectOption("N");
    });
    await page.waitForTimeout(500);
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(2000);
    console.log("미사용 필터 QE9H 행:", await readSchRow(page, "QE9H"));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
