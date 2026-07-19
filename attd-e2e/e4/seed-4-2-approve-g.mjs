// QE-4-2 시드 ② — G(QT신입지) 결재함에서 A의 8/10 종일 연차 승인
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openLeaveApprovalList, openApprovalDetail, approveInDetail, bodyText } from "../e2/lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERG", "QtTest!2026");
    await openLeaveApprovalList(page);
    console.log("결재함:", (await bodyText(page)).replace(/\n/g, " | ").slice(0, 500));
    await openApprovalDetail(page, "QT사원에이");
    console.log("상세:", (await bodyText(page)).replace(/\n/g, " | ").slice(0, 500));
    await approveInDetail(page);
    await page.waitForTimeout(1500);
    console.log("승인 후:", (await bodyText(page)).replace(/\n/g, " | ").slice(0, 300));
    await page.screenshot({ path: shotPath("QE-4-2", "app", "seed-approved-0810") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
