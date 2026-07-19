// QE-4-6 — G 8/13(적용일 8/1 이후) 시간차 1h preview 관찰(제출 안 함) → 신분모 480 확인
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openLeaveApply, selectLeaveType, selectUnit, setDate, setStartTime, readPreview, bodyText, waitLoaded } from "../e2/lib-leave.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERG", "QtTest!2026");
    await waitLoaded(page);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await openLeaveApply(page);
    await selectLeaveType(page, "월차");
    await selectUnit(page, "1시간");
    await setDate(page, 2026, 8, 13);
    await setStartTime(page, 9, 0);
    await page.waitForTimeout(800);
    const preview = await readPreview(page);
    console.log("=== PREVIEW (8/13, conv 480 기대: 1시간=60/480=0.125일) ===");
    console.log(preview);
    await page.screenshot({ path: shotPath("QE-4-6", "app", "preview-g-0813"), fullPage: true });
    // 비교용: 8/1 이전(7/28 등)은 400 분모여야 함 — 날짜만 바꿔 재관찰
    await setDate(page, 2026, 7, 28);
    await setStartTime(page, 9, 0);
    await page.waitForTimeout(800);
    const previewPast = await readPreview(page);
    console.log("=== PREVIEW (7/28, conv 400 기대: 1시간=60/400=0.15일) ===");
    console.log(previewPast);
    await page.screenshot({ path: shotPath("QE-4-6", "app", "preview-g-0728"), fullPage: true });
    console.log("본문:", (await bodyText(page)).slice(0, 800).replace(/\n/g, " | "));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
