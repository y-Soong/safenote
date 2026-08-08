// Phase 3: 반차 실제 제출 — 08-10 시작기준(늦게 출근) + F-6(결재자 시트) 검증
// QTUSERA → 신청 → Alert 확인. 제출 후 DB 검증은 별도(MCP).
import { appLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

async function pickDate(page, y, m, d) {
  const fieldBtn = page.locator("button", { hasText: /날짜 선택|\d{4}\.\s?\d{2}\.\s?\d{2}/ }).first();
  await fieldBtn.click();
  await page.waitForSelector(".wp-keyin__in--y", { timeout: 5000 });
  const setIn = async (sel, val) => {
    const el = page.locator(sel);
    await el.click();
    await el.fill(String(val));
    await el.dispatchEvent("blur");
  };
  await setIn(".wp-keyin__in--y", y);
  await setIn(".wp-keyin__in:not(.wp-keyin__in--y) >> nth=0", m);
  await setIn(".wp-keyin__in:not(.wp-keyin__in--y) >> nth=1", d);
  await page.locator("button.wp-confirm").click();
  await page.waitForTimeout(2500);
}

const main = async () => {
  const { page } = await appLogin("QTUSERA", "QtTest!2026");
  await page.goto("https://localhost:8082/#/LeaveApply", { timeout: 15000 });
  await page.waitForTimeout(2000);
  await page.locator(".type-item", { hasText: "연차" }).first().click();
  await page.waitForTimeout(1200);
  await page.locator(".unit-chip", { hasText: "반차" }).first().click();
  await page.waitForTimeout(600);
  await pickDate(page, 2026, process.argv[2] || "08", process.argv[3] || "11");

  // 파트: 늦게 출근(START) 선택
  await page.locator("button[class*=half-card]", { hasText: process.argv[4] === "END" ? "일찍 퇴근" : "늦게 출근" }).first().click();
  await page.waitForTimeout(600);

  // 사유 입력
  await page.locator("textarea").first().fill("무인테스트 반차 (자동)");

  // ── F-6: 결재자 추가 시트 ──
  await page.locator("button", { hasText: "결재자 추가" }).first().click();
  await page.waitForTimeout(2000);
  const sheetText = await page.evaluate(() => document.body.innerText);
  const si = sheetText.indexOf("결재자");
  console.log("=== [F-6] 결재자 시트 오픈 ===");
  console.log(sheetText.slice(si, si + 500));
  // 스크롤 잠금 확인 (F-6 핵심)
  const bodyLock = await page.evaluate(() => ({
    overflow: getComputedStyle(document.body).overflow,
    htmlOverflow: getComputedStyle(document.documentElement).overflow,
  }));
  console.log("[F-6] 시트 오픈 중 body overflow:", JSON.stringify(bodyLock));
  await page.screenshot({ path: `${SHOT}/F6-approver-sheet.png`, fullPage: true });

  // 결재자 후보 선택 — li.picker__item 행 클릭 → picker__apply 확정 (실제 시트 구조)
  const rows = await page.locator("li.picker__item").allInnerTexts();
  console.log("[F-6] 후보 행:", rows.map((r) => r.replace(/\n/g, " ").slice(0, 40)));
  const cand = page.locator("li.picker__item", { hasText: /QT관리자|QT팀장/ }).first();
  if (await cand.count()) {
    await cand.click();
  } else {
    await page.locator("li.picker__item").first().click();
  }
  await page.waitForTimeout(500);
  await page.locator("button.picker__apply").click();
  await page.waitForTimeout(1000);

  // 시트 닫힘/선택 반영 후 상태
  const after = await page.evaluate(() => document.body.innerText);
  const ai = after.indexOf("결재선");
  console.log("=== 결재선 상태 ===");
  console.log(after.slice(ai, ai + 250));
  // 스크롤 잠금 해제 확인
  const bodyUnlock = await page.evaluate(() => getComputedStyle(document.body).overflow);
  console.log("[F-6] 시트 닫힌 후 body overflow:", bodyUnlock);

  await page.screenshot({ path: `${SHOT}/A-2-before-submit.png`, fullPage: true });

  // ── 제출 ──
  await page.locator("button", { hasText: "신청하기" }).last().click();
  await page.waitForTimeout(3000);
  const final = await page.evaluate(() => document.body.innerText);
  console.log("=== 제출 결과 화면 ===");
  console.log(final.slice(0, 300));
  await page.screenshot({ path: `${SHOT}/A-2-after-submit.png`, fullPage: true });
  // Alert 확인 버튼 있으면 닫기
  await page.locator("button", { hasText: "확인" }).last().click().catch(() => {});
  await closeAll();
};
main();
