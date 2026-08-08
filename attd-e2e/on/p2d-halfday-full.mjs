// Phase 2-4 (재시도): 날짜를 스테퍼로 실제 선택 — A-1 · H-1 · J-1 · F-5
import { appLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

// 날짜 스테퍼 시트로 YYYY-MM-DD 설정
async function pickDate(page, y, m, d) {
  // 필드 버튼(값 or 플레이스홀더 표시) 클릭 → 시트 오픈
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
  await page.waitForTimeout(2500); // day-schedule 조회 대기
}

const chipDump = (page) =>
  page.evaluate(() =>
    [...document.querySelectorAll(".unit-chip")].map((b) => ({
      text: b.innerText.replace(/\n/g, " "), on: b.className.includes("--on"), disabled: b.disabled,
    }))
  );

const main = async () => {
  const { page } = await appLogin("QTUSERA", "QtTest!2026");
  await page.goto("https://localhost:8082/#/LeaveApply", { timeout: 15000 });
  await page.waitForTimeout(2000);
  await page.locator(".type-item", { hasText: "연차" }).first().click();
  await page.waitForTimeout(1500);

  // ── [A-1·H-1] 반차 선택 → 날짜 08-10 ──
  console.log("=== [H-1] 칩 (선택 직후) ===");
  (await chipDump(page)).forEach((c) => console.log(JSON.stringify(c)));
  await page.locator(".unit-chip", { hasText: "반차" }).first().click();
  await page.waitForTimeout(800);
  await pickDate(page, 2026, "08", "10");

  const t1 = await page.evaluate(() => document.body.innerText);
  const i1 = t1.indexOf("반차 구분");
  console.log("=== [A-1] 08-10 반차 파트 영역 ===");
  console.log(t1.slice(i1, i1 + 350));
  await page.screenshot({ path: `${SHOT}/A-1-halfcards-0810.png`, fullPage: true });

  // 파트 카드 disabled 여부
  const cards1 = await page.evaluate(() =>
    [...document.querySelectorAll("button[class*=half-card]")].map((c) => ({
      text: c.innerText.replace(/\n/g, " | "), disabled: c.disabled, on: c.className.includes("--on"),
    }))
  );
  cards1.forEach((c) => console.log(JSON.stringify(c)));

  // ── [J-1·F-5] 날짜를 08-08(토, 미배정)로 변경 ──
  await pickDate(page, 2026, "08", "08");
  console.log("=== [J-1] 칩 (08-08 미배정일) ===");
  (await chipDump(page)).forEach((c) => console.log(JSON.stringify(c)));
  const t2 = await page.evaluate(() => document.body.innerText);
  const i2 = t2.search(/근무계획|휴무|스케줄이 없|배정된/);
  console.log("=== [J-1/F-4] 안내 문구 ===");
  console.log(i2 >= 0 ? t2.slice(Math.max(0, i2 - 120), i2 + 250) : "(문구 미검출)");
  await page.screenshot({ path: `${SHOT}/J1-0808-chips.png`, fullPage: true });

  // 종일 선택 → F-5 휴무일 안내
  await page.locator(".unit-chip", { hasText: "종일" }).first().click();
  await page.waitForTimeout(2000);
  const t3 = await page.evaluate(() => document.body.innerText);
  const i3 = t3.search(/휴무|근무계획이 없|스케줄이 없/);
  console.log("=== [F-5] 종일 + 미배정일 안내 ===");
  console.log(i3 >= 0 ? t3.slice(Math.max(0, i3 - 120), i3 + 250) : "(문구 미검출)");
  await page.screenshot({ path: `${SHOT}/F5-0808-fullday.png`, fullPage: true });

  await closeAll();
};
main();
