// 범용 연차 신청 — node e2/apply-leave.mjs <신청자ID> <종류명> <단위라벨> <M> <D> <caseId> <사유> [시작HH:MM] [step증가횟수] [결재자명]
// 단위라벨: 종일|반차|2시간|1시간|30분. 시간차면 시작시각 필수. step증가횟수: 종료 [+] 클릭 수(기본 0).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openLeaveApply, selectLeaveType, selectUnit, setDate, setStartTime, stepUpEnd, addApprover, readPreview, clickPopupOk, bodyText, waitLoaded } from "./lib-leave.mjs";

const [, , userId, typeName, unitLabel, mm, dd, caseId, reason, startTime = "", stepUps = "0", approverName = "QT사원에이"] = process.argv;

const main = async () => {
  try {
    const { page } = await appLogin(userId, "QtTest!2026");
    await waitLoaded(page);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await openLeaveApply(page);
    let text = await bodyText(page);
    console.log("=== 종류 목록 ===");
    console.log(text.split("신청 일자")[0].slice(0, 900));

    // 종류 버튼 disabled 여부 실측
    const typeBtn = page.locator(`button.type-item:has-text("${typeName}")`).first();
    if ((await typeBtn.count()) === 0) { console.log(`종류 '${typeName}' 미존재`); process.exit(2); }
    const disabled = await typeBtn.isDisabled();
    console.log(`종류 '${typeName}' disabled=`, disabled);
    if (disabled) { console.log("APPLICABLE=FALSE — 선택 불가"); process.exit(3); }
    await typeBtn.click();
    await page.waitForTimeout(800);
    const units = await page.locator("button.unit-chip").allInnerTexts();
    console.log("허용 단위:", JSON.stringify(units));
    if (!units.some((u) => u.includes(unitLabel))) { console.log(`단위 '${unitLabel}' 미허용`); process.exit(4); }
    await selectUnit(page, unitLabel);
    await setDate(page, 2026, Number(mm), Number(dd));
    if (startTime) {
      const [h, m] = startTime.split(":").map(Number);
      await setStartTime(page, h, m);
      await stepUpEnd(page, Number(stepUps));
      const endVal = await page.locator(".end-stepper__val").innerText().catch(() => "-");
      console.log("종료 표시:", endVal);
    }
    const preview = await readPreview(page);
    console.log("PREVIEW:", preview);
    await page.fill("textarea", reason);
    if (await page.locator("button.btn-add").count()) {
      await addApprover(page, approverName);
    } else {
      console.log("결재선 섹션 없음(aprvRequired=false) — 즉시 반영형");
    }
    await page.waitForTimeout(500);
    await page.screenshot({ path: shotPath(caseId, "app", `form-${dd}-${startTime.replace(":", "") || unitLabel}`), fullPage: true });
    const submit = page.locator('button:has-text("신청하기")').last();
    console.log("신청하기 disabled=", await submit.isDisabled());
    await submit.click();
    await page.waitForTimeout(2500);
    text = await bodyText(page);
    console.log("=== 제출 직후 ===");
    console.log(text.split("\n").slice(-12).join(" | "));
    await page.screenshot({ path: shotPath(caseId, "app", `submit-${dd}-${startTime.replace(":", "") || unitLabel}`) });
    await clickPopupOk(page);
    await page.waitForTimeout(1200);
    text = await bodyText(page);
    console.log("=== 처리 후 ===");
    console.log(text.slice(0, 700));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
