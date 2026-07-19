// QE-4-7 정리 — G 결재함에서 A 8/13 대기 연차 반려
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openLeaveApprovalList, openApprovalDetail, rejectInDetail, bodyText } from "../e2/lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERG", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await openLeaveApprovalList(page);
    const before = await bodyText(page);
    console.log("결재함:", before.slice(0, 700).replace(/\n/g, " | "));
    await openApprovalDetail(page, "QT사원에이");
    const detail = await bodyText(page);
    console.log("상세:", detail.slice(0, 500).replace(/\n/g, " | "));
    await rejectInDetail(page, "[QE-4-7] 스케줄 덮어쓰기 테스트 종료 반려 정리");
    await page.waitForTimeout(1500);
    console.log("반려 후:", (await bodyText(page)).slice(0, 500).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-4-7", "app", "rejected") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
