// 앱 근태 핵심 화면 도달성 스모크 — MyAttendance / AttdRequest / MyLeaveSummaryView / LeaveApply.
import { appLogin, closeAll } from "./lib/browser.mjs";

const SCREENS = [
  ["MyAttendance", "내 근태"],
  ["AttdRequest", "근태 요청"],
  ["MyLeaveSummaryView", "연차 요약"],
  ["LeaveApply", "연차 신청"],
  ["MyRequests", "내 요청"],
];

const main = async () => {
  let fail = 0;
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    for (const [route, label] of SCREENS) {
      try {
        await page.goto(`https://localhost:8082/#/${route}`, { timeout: 15000 });
        await page.waitForLoadState("networkidle", { timeout: 15000 });
        const text = (await page.evaluate(() => document.body.innerText)).replace(/\n+/g, " | ").slice(0, 200);
        console.log(`OK  ${route} (${label}): ${text.slice(0, 150)}`);
      } catch (e) {
        fail++;
        console.log(`FAIL ${route} (${label}): ${e.message.slice(0, 120)}`);
      }
    }
    console.log(fail === 0 ? "SMOKE RESULT: PASS" : `SMOKE RESULT: ${fail} FAIL`);
    process.exitCode = fail === 0 ? 0 : 1;
  } finally {
    await closeAll();
  }
};
main();
