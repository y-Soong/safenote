// Phase 8: 앱면 배치 — Q(반차↔시간차 겹침 거부) · I(연차현황 F-3 표기) · P-1(잔여 ⓘ)
import { appLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

async function pickDate(page, y, m, d) {
  const fieldBtn = page.locator("button", { hasText: /날짜 선택|\d{4}\.\s?\d{2}\.\s?\d{2}/ }).first();
  await fieldBtn.click();
  await page.waitForSelector(".wp-keyin__in--y", { timeout: 5000 });
  const setIn = async (sel, val) => {
    const el = page.locator(sel);
    await el.click(); await el.fill(String(val)); await el.dispatchEvent("blur");
  };
  await setIn(".wp-keyin__in--y", y);
  await setIn(".wp-keyin__in:not(.wp-keyin__in--y) >> nth=0", m);
  await setIn(".wp-keyin__in:not(.wp-keyin__in--y) >> nth=1", d);
  await page.locator("button.wp-confirm").click();
  await page.waitForTimeout(2500);
}

const main = async () => {
  const { page } = await appLogin("QTUSERA", "QtTest!2026");

  // ── [Q] 08-12 반차 신청 → 기확정 시간차(10:00~11:00)와 겹침 → 거부 기대 ──
  await page.goto("https://localhost:8082/#/LeaveApply", { timeout: 15000 });
  await page.waitForTimeout(2000);
  await page.locator(".type-item", { hasText: "연차" }).first().click();
  await page.waitForTimeout(1200);
  await page.locator(".unit-chip", { hasText: "반차" }).first().click();
  await page.waitForTimeout(600);
  await pickDate(page, 2026, "08", "12");
  await page.locator("button[class*=half-card]", { hasText: "늦게 출근" }).first().click();
  await page.waitForTimeout(600);
  await page.locator("textarea").first().fill("무인테스트 겹침 거부 기대");
  await page.locator("button", { hasText: "결재자 추가" }).first().click();
  await page.waitForTimeout(1800);
  await page.locator("li.picker__item").first().click();
  await page.locator("button.picker__apply").click();
  await page.waitForTimeout(1000);
  await page.locator("button", { hasText: "신청하기" }).last().click();
  await page.waitForTimeout(3000);
  const qText = await page.evaluate(() => document.body.innerText);
  const qi = qText.search(/겹치|중복|이미|불가|없어요|실패/);
  console.log("=== [Q] 08-12 겹침 제출 결과 ===");
  console.log(qi >= 0 ? qText.slice(Math.max(0, qi - 150), qi + 250).replace(/\n{2,}/g, "\n") : qText.slice(0, 250));
  await page.screenshot({ path: `${SHOT}/Q-overlap-0812.png`, fullPage: true });
  await page.locator("button", { hasText: "확인" }).last().click().catch(() => {});
  await page.waitForTimeout(500);

  // ── [I] 연차 현황 표기 (MyLeaveSummaryView) ──
  await page.goto("https://localhost:8082/#/MyLeaveSummaryView", { timeout: 15000 });
  await page.waitForTimeout(3000);
  const iText = await page.evaluate(() => document.body.innerText);
  console.log("=== [I] MyLeaveSummaryView 전문(상위 900자) ===");
  console.log(iText.slice(0, 900).replace(/\n{2,}/g, "\n"));
  await page.screenshot({ path: `${SHOT}/I-myleave-summary.png`, fullPage: true });

  // ⓘ 안내 버튼 (잔여 근사치 안내 시트, P-1)
  const infoBtn = page.locator("button[class*=info], [class*=hint-btn], button:has-text('ⓘ')").first();
  if (await infoBtn.count()) {
    await infoBtn.click();
    await page.waitForTimeout(1200);
    const sheet = await page.evaluate(() => document.body.innerText);
    const si = sheet.search(/근사|참고|환산/);
    console.log("=== [P-1] ⓘ 시트 ===");
    console.log(si >= 0 ? sheet.slice(Math.max(0, si - 100), si + 300) : "(시트 문구 미검출)");
    await page.screenshot({ path: `${SHOT}/P1-info-sheet.png`, fullPage: true });
    await page.locator("button", { hasText: /닫기|확인/ }).last().click().catch(() => {});
  } else {
    console.log("[P-1] ⓘ 버튼 미발견 — MyPage 쪽에서 재확인 필요");
  }

  // ── [I] 마이페이지 카드 (MyPageView) ──
  await page.goto("https://localhost:8082/#/MyPageView", { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(2500);
  const mText = await page.evaluate(() => document.body.innerText);
  const mi = mText.search(/사용|잔여/);
  console.log("=== [I] MyPageView (연차 카드 근처) ===");
  console.log(mi >= 0 ? mText.slice(Math.max(0, mi - 50), mi + 400).replace(/\n{2,}/g, "\n") : mText.slice(0, 300));
  await page.screenshot({ path: `${SHOT}/I-mypage.png`, fullPage: true });

  await closeAll();
};
main();
