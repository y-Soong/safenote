// Phase 2-1: 앱 연차 신청 화면 진입 + 반차 UI 실측 (A-1 · H-1 · J-1)
// QTUSERA / 대상일 08-10(월, QT8H 배정) → 반차 카드·경계 14:00 확인
import { appLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

const main = async () => {
  const { page } = await appLogin("QTUSERA", "QtTest!2026");

  // 연차 신청 화면 진입 — 라우트 직접 이동 시도 전에 메뉴 텍스트 확인
  const homeText = await page.evaluate(() => document.body.innerText);
  console.log("=== 홈 하단 메뉴/버튼 ===");
  console.log(homeText.slice(0, 400).replace(/\n{2,}/g, "\n"));

  // 연차 신청 링크/버튼 찾기
  const leaveBtn = page.locator('text=연차').first();
  const cnt = await page.locator('text=연차').count();
  console.log(`"연차" 텍스트 요소: ${cnt}개`);

  // 앱 라우트 직접 진입 (기존 앱 라우팅 규칙: #/LeaveApply 형태 추정 → 실패 시 메뉴 클릭)
  for (const route of ["LeaveApplyView", "LeaveApply", "LeaveView"]) {
    await page.goto(`https://localhost:8082/#/${route}`, { timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(1500);
    const url = page.url();
    const t = await page.evaluate(() => document.body.innerText);
    if (!t.includes("페이지를 찾을 수") && (t.includes("연차 신청") || t.includes("휴가") || t.includes("사용 단위"))) {
      console.log(`=== 라우트 ${route} 진입 성공: ${url}`);
      console.log(t.slice(0, 800));
      await page.screenshot({ path: `${SHOT}/P2-leave-route-${route}.png`, fullPage: true });
      break;
    } else {
      console.log(`--- 라우트 ${route}: 미매칭 (${url})`);
    }
  }
  await closeAll();
};
main();
