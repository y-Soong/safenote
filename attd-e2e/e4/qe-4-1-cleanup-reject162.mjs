// QE-4-1 정리 — A(QTUSERA) 결재함에서 G의 7/29 1500~1530 대기 시간차(REQ 2026071700162) 반려
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openLeaveApprovalList, openApprovalDetail, rejectInDetail, bodyText } from "../e2/lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await openLeaveApprovalList(page);
    console.log("결재함 목록:", (await bodyText(page)).replace(/\n/g, " | ").slice(0, 600));
    await page.screenshot({ path: shotPath("QE-4-1", "app", "cleanup-approval-list") });
    await openApprovalDetail(page, "QT신입지");
    console.log("상세:", (await bodyText(page)).replace(/\n/g, " | ").slice(0, 600));
    await page.screenshot({ path: shotPath("QE-4-1", "app", "cleanup-detail") });
    await rejectInDetail(page, "[QE-4-1] 케이스 종료 정리 반려 처리");
    await page.waitForTimeout(1500);
    console.log("반려 후:", (await bodyText(page)).replace(/\n/g, " | ").slice(0, 400));
    await page.screenshot({ path: shotPath("QE-4-1", "app", "cleanup-after-reject") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
