// Phase 2-3: A-1(반차 카드+경계 14:00) · H-1(반반차 부재) · J-1(미배정일 disable) · F-5(휴무일 종일 안내)
import { appLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

const chipDump = (page) =>
  page.evaluate(() =>
    [...document.querySelectorAll(".unit-chip")].map((b) => ({
      text: b.innerText.replace(/\n/g, " "), on: b.className.includes("--on"), disabled: b.disabled,
    }))
  );

const main = async () => {
  const { page } = await appLogin("QTUSERA", "QtTest!2026");

  // ── [배정일 08-10] A-1 · H-1 ──
  await page.goto("https://localhost:8082/#/LeaveApply?workYmd=20260810", { timeout: 15000 });
  await page.waitForTimeout(2500);
  await page.locator(".type-item", { hasText: "연차" }).first().click();
  await page.waitForTimeout(2000);

  console.log("=== [H-1] 사용단위 칩 (08-10 배정일) ===");
  (await chipDump(page)).forEach((c) => console.log(JSON.stringify(c)));

  // 반차 칩 클릭
  await page.locator(".unit-chip", { hasText: "반차" }).first().click();
  await page.waitForTimeout(2500); // day-schedule 조회 대기

  console.log("=== [A-1] 반차 파트 카드 ===");
  const cards = await page.evaluate(() =>
    [...document.querySelectorAll(".half-card, [class*=half-card]")].map((c) => ({
      text: c.innerText.replace(/\n/g, " | ").slice(0, 120), cls: c.className.slice(0, 50), disabled: c.disabled,
    }))
  );
  cards.forEach((c) => console.log(JSON.stringify(c)));
  const bodyText = await page.evaluate(() => document.body.innerText);
  const bIdx = bodyText.indexOf("늦게 출근");
  console.log("=== 파트 영역 원문 ===");
  console.log(bodyText.slice(Math.max(0, bIdx - 200), bIdx + 400));
  await page.screenshot({ path: `${SHOT}/A-1-halfcards-0810.png`, fullPage: true });

  // ── [미배정일 08-08(토)] J-1 · F-5 ──
  await page.goto("https://localhost:8082/#/LeaveApply?workYmd=20260808", { timeout: 15000 });
  await page.waitForTimeout(2500);
  await page.locator(".type-item", { hasText: "연차" }).first().click();
  await page.waitForTimeout(2500);

  console.log("=== [J-1] 사용단위 칩 (08-08 미배정일) ===");
  (await chipDump(page)).forEach((c) => console.log(JSON.stringify(c)));

  // 종일 선택 시 휴무일 안내 (F-5)
  await page.locator(".unit-chip", { hasText: "종일" }).first().click().catch(() => {});
  await page.waitForTimeout(2000);
  const t2 = await page.evaluate(() => document.body.innerText);
  const hIdx2 = t2.search(/휴무|근무계획|스케줄이 없|미배정/);
  console.log("=== [F-5] 종일 선택 후 안내 문구 탐색 ===");
  console.log(hIdx2 >= 0 ? t2.slice(Math.max(0, hIdx2 - 150), hIdx2 + 300) : "(안내 문구 미검출)");
  await page.screenshot({ path: `${SHOT}/J1-F5-0808-unassigned.png`, fullPage: true });

  await closeAll();
};
main();
