// P-1(ⓘ 시트) + I(MyPage 카드) 재실행 — 정확한 셀렉터/라우트
import { appLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

const main = async () => {
  const { page } = await appLogin("QTUSERA", "QtTest!2026");

  await page.goto("https://localhost:8082/#/MyLeaveSummaryView", { timeout: 15000 });
  await page.waitForTimeout(3000);
  await page.locator(".card__guide").first().click();
  await page.waitForTimeout(1200);
  const t = await page.evaluate(() => document.body.innerText);
  const i = t.indexOf("잔여 연차 시간 표기 안내");
  console.log("=== [P-1] ⓘ 시트 ===");
  console.log(i >= 0 ? t.slice(i, i + 450).replace(/\n{2,}/g, "\n") : "(시트 미노출)");
  // 스크롤 잠금
  console.log("[P-1] body overflow:", await page.evaluate(() => getComputedStyle(document.body).overflow));
  await page.screenshot({ path: `${SHOT}/P1-info-sheet.png`, fullPage: true });
  await page.locator("button.lv-guide__close").click().catch(() => {});
  await page.waitForTimeout(600);
  console.log("[P-1] 닫은 후 overflow:", await page.evaluate(() => getComputedStyle(document.body).overflow));

  await page.goto("https://localhost:8082/#/MyPage", { timeout: 15000 });
  await page.waitForTimeout(2500);
  const m = await page.evaluate(() => document.body.innerText);
  const mi = m.search(/연차|잔여/);
  console.log("=== [I] MyPage 카드 ===");
  console.log(mi >= 0 ? m.slice(Math.max(0, mi - 80), mi + 350).replace(/\n{2,}/g, "\n") : m.slice(0, 250));
  await page.screenshot({ path: `${SHOT}/I-mypage.png`, fullPage: true });
  await closeAll();
};
main();
