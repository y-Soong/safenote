// day7 에 누락된 1차 구간(0900~1400) 추가 — 기존 행 유지 + 새 행 추가
import { webLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

const main = async () => {
  const { page } = await webLogin("QTHR", "QtTest!2026");
  await page.goto("http://localhost:8081/safenote/main/Attd_07", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2500);
  await page.locator("button", { hasText: "조회" }).first().click().catch(() => {});
  await page.waitForTimeout(3000);

  const cellBox = await page.evaluate(() => {
    const rows = [...document.querySelectorAll("tr")];
    const row = rows.find((r) => r.innerText.includes("QT사원에이"));
    const table = row.closest("table");
    const head = table.querySelector("tr");
    let colIdx = -1;
    [...head.cells].forEach((c, i) => { if (c.innerText.trim().split("\n")[0] === "7") colIdx = i; });
    const cell = row.cells[colIdx];
    const r = cell.getBoundingClientRect();
    return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
  });
  await page.mouse.dblclick(cellBox.x, cellBox.y);
  await page.waitForTimeout(2500);

  // 현재 구간 상태 덤프
  let vals = await page.evaluate(() => [...document.querySelectorAll("input.seg-time")].map((i) => i.value));
  console.log("기존 seg 값:", JSON.stringify(vals));

  await page.locator("button", { hasText: "구간 추가" }).first().click();
  await page.waitForTimeout(700);
  const seg = page.locator("input.seg-time");
  const n = await seg.count();
  console.log("구간 추가 후 입력 수:", n);
  // 행 순서 = 시간순 검증 → 4칸을 시간순으로 재기입.
  // 마스크드 입력(:value+@input)이라 fill 이 기존 값에 안 먹힘 → 포커스+전체선택+키입력
  const typeTime = async (el, v) => {
    await el.click();
    await el.press("Control+a");
    await el.pressSequentially(v, { delay: 40 });
    await el.dispatchEvent("blur");
    await page.waitForTimeout(150);
  };
  await typeTime(seg.nth(0), "0900");
  await typeTime(seg.nth(1), "1400");
  await typeTime(seg.nth(2), "1500");
  await typeTime(seg.nth(3), "1900");
  vals = await page.evaluate(() => [...document.querySelectorAll("input.seg-time")].map((i) => i.value));
  console.log("입력 후 seg 값:", JSON.stringify(vals));

  const reasonEl = page.locator(".prafta-modal-popup textarea, .prafta-modal-popup input[placeholder*='사유']").last();
  if (await reasonEl.count()) await reasonEl.fill("무인테스트 1차 구간 추가");
  await page.locator(".prafta-modal-popup button.save-btn").first().click();
  await page.waitForTimeout(1200);
  for (let i = 0; i < 3; i++) {
    const modal = page.locator(".prafta-modal-popup").last();
    if (!(await modal.count())) break;
    const t = (await modal.innerText().catch(() => "")).slice(0, 200);
    if (t.includes("저장") || t.includes("확인")) console.log("모달:", t.split("\n").slice(0, 3).join(" / "));
    await modal.locator("button", { hasText: /확인|닫기/ }).first().click().catch(() => {});
    await page.waitForTimeout(700);
  }
  await page.screenshot({ path: `${SHOT}/P5-day7-fixed.png`, fullPage: true });
  await closeAll();
};
main();
