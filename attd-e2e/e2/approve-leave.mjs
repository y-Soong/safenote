// 연차 결재함 승인/반려 공용 — node e2/approve-leave.mjs <결재자ID> <요청자명> <approve|reject> [caseId] [반려사유]
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openLeaveApprovalList, openApprovalDetail, approveInDetail, rejectInDetail, bodyText } from "./lib-leave.mjs";

const [, , approverId, requesterName, action = "approve", caseId = "QE-2-x", reason = "", timeMatch = ""] = process.argv;
const main = async () => {
  try {
    const { page } = await appLogin(approverId, "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await openLeaveApprovalList(page);
    let text = await bodyText(page);
    console.log("=== 결재함 목록 ===");
    console.log(text.slice(0, 1200));
    await page.screenshot({ path: shotPath(caseId, "app", `approval-list-${approverId}`), fullPage: true });
    if (timeMatch) {
      // 시각 문자열로 카드 특정(같은 요청자 다건 구분)
      await page.evaluate(({ name, tm }) => {
        const nodes = [...document.querySelectorAll("article.lac")];
        const el = nodes.find((n) => n.innerText.includes(name) && n.innerText.includes(tm));
        if (!el) throw new Error(`카드 미발견: ${name} ${tm}`);
        el.click();
      }, { name: requesterName, tm: timeMatch });
      await page.waitForTimeout(2500);
    } else {
      await openApprovalDetail(page, requesterName);
    }
    text = await bodyText(page);
    console.log("=== 결재 상세 ===");
    console.log(text.slice(0, 1500));
    await page.screenshot({ path: shotPath(caseId, "app", "approval-detail"), fullPage: true });
    if (action === "approve") {
      await approveInDetail(page);
    } else {
      await rejectInDetail(page, reason || `[${caseId}] 반려 사유 테스트 10자 이상`);
    }
    await page.waitForTimeout(1500);
    text = await bodyText(page);
    console.log("=== 처리 후 ===");
    console.log(text.slice(0, 1000));
    await page.screenshot({ path: shotPath(caseId, "app", `after-${action}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
