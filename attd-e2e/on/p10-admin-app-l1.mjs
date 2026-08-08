// Phase 10: P5(앱 관리자 근태현황 AdminAttdDetail) + L-1(개인정보 수정 버튼 규약)
import { appLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

const main = async () => {
  const { page } = await appLogin("QTHR", "QtTest!2026");

  // ── [P5] 앱 관리자 근태현황 ──
  await page.goto("https://localhost:8082/#/AdminAttdDetail", { timeout: 15000 });
  await page.waitForTimeout(3500);
  let t = await page.evaluate(() => document.body.innerText);
  console.log("=== [P5] AdminAttdDetail 초기(오늘) ===");
  console.log(t.slice(0, 400).replace(/\n{2,}/g, "\n"));
  await page.screenshot({ path: `${SHOT}/P5-admin-attd-today.png`, fullPage: true });

  // 날짜를 08-06(경계 정시 정상 케이스)으로 이동 — 화면 내 날짜 네비게이션 탐색
  const navBtns = await page.evaluate(() =>
    [...document.querySelectorAll("button")].map((b) => b.innerText.replace(/\n/g, " ").slice(0, 16)).filter(Boolean).slice(0, 20)
  );
  console.log("[P5] 버튼 목록:", JSON.stringify(navBtns));
  // '‹' 또는 '이전' 버튼으로 이틀 전(08-06)까지 이동 시도
  const prev = page.locator("button", { hasText: /‹|이전|◀/ }).first();
  if (await prev.count()) {
    for (let i = 0; i < 2; i++) { await prev.click(); await page.waitForTimeout(1500); }
    t = await page.evaluate(() => document.body.innerText);
    console.log("=== [P5] 이틀 이전 화면 ===");
    console.log(t.slice(0, 500).replace(/\n{2,}/g, "\n"));
    await page.screenshot({ path: `${SHOT}/P5-admin-attd-0806.png`, fullPage: true });
  } else {
    console.log("[P5] 날짜 네비 버튼 미발견");
  }

  // ── [L-1] 마이페이지 → 개인정보 수정 버튼 규약 ──
  await page.goto("https://localhost:8082/#/MyPage", { timeout: 15000 });
  await page.waitForTimeout(2000);
  await page.locator("text=개인정보 수정").first().click();
  await page.waitForTimeout(2500);
  const btns = await page.evaluate(() => {
    // 하단 액션 버튼들의 순서·클래스
    const cand = [...document.querySelectorAll("button")].filter((b) => /저장|취소/.test(b.innerText));
    return cand.map((b) => {
      const r = b.getBoundingClientRect();
      return { text: b.innerText.trim(), cls: b.className.slice(0, 60), x: Math.round(r.x), w: Math.round(r.width) };
    });
  });
  console.log("=== [L-1] 개인정보 수정 버튼 ===");
  btns.forEach((b) => console.log(JSON.stringify(b)));
  await page.screenshot({ path: `${SHOT}/L1-profile-buttons.png`, fullPage: true });
  await closeAll();
};
main();
