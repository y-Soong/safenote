// Phase 5: Attd_07 일자상세에서 관리자 근태 직접 입력 (파라미터: day, in1, out1, [in2, out2])
// 예: node p5-attd07-input.mjs 6 1400 1800        → 08-06 14:00~18:00
//     node p5-attd07-input.mjs 7 0900 1400 1500 1900 → 08-07 2구간
import { webLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";
const [day, in1, out1, in2, out2] = process.argv.slice(2);

async function drainModal(page, label) {
  const texts = [];
  for (let i = 0; i < 4; i++) {
    const modal = page.locator(".prafta-modal-popup").last();
    if (!(await modal.count()) || !(await modal.isVisible().catch(() => false))) break;
    const t = (await modal.innerText().catch(() => "")).replace(/\n{2,}/g, "\n");
    texts.push(t);
    console.log(`--- [모달@${label}] ---\n${t.slice(0, 400)}`);
    const btn = modal.locator("button", { hasText: /확인|닫기/ }).first();
    if (await btn.count()) await btn.click().catch(() => {});
    await page.waitForTimeout(700);
  }
  return texts.join("\n");
}

const main = async () => {
  const { page } = await webLogin("QTHR", "QtTest!2026");
  await page.goto("http://localhost:8081/safenote/main/Attd_07", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2500);

  // 조회 (헤더 조회 버튼)
  await page.locator("button", { hasText: "조회" }).first().click().catch(() => {});
  await page.waitForTimeout(3000);

  // QTUSERA 행에서 day 셀 클릭 — 캘린더 그리드 구조 파악 후 클릭
  const cellBox = await page.evaluate((d) => {
    // 후보: 테이블 기반 그리드
    const rows = [...document.querySelectorAll("tr")];
    const row = rows.find((r) => r.innerText.includes("QT사원에이"));
    if (!row) return { err: "행 미발견", rows: rows.slice(0, 5).map((r) => r.innerText.slice(0, 60)) };
    // 헤더에서 day 열 찾기
    const table = row.closest("table");
    const head = table.querySelector("tr");
    let colIdx = -1;
    [...head.cells].forEach((c, i) => {
      const t = c.innerText.trim();
      if (t === String(Number(d)) || t.startsWith(`${Number(d)}(`) || t.split("\n")[0] === String(Number(d))) colIdx = i;
    });
    if (colIdx < 0) return { err: "열 미발견", head: head.innerText.slice(0, 200) };
    const cell = row.cells[colIdx];
    const r = cell.getBoundingClientRect();
    return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: cell.innerText.trim().slice(0, 30) };
  }, day);
  if (cellBox.err) { console.log("FAIL:", JSON.stringify(cellBox)); await closeAll(); return; }
  console.log(`day=${day} 셀 현재값:`, JSON.stringify(cellBox.text));
  await page.mouse.dblclick(cellBox.x, cellBox.y);   // Attd_07 캘린더 셀은 @dblclick
  await page.waitForTimeout(2500);

  // 팝업 상태 덤프
  const popText = await page.evaluate(() => {
    const pop = document.querySelector(".prafta-modal-popup, [class*=day-detail], [class*=popup]");
    return pop ? pop.innerText.slice(0, 600).replace(/\n{2,}/g, "\n") : "(팝업 미발견)";
  });
  console.log("=== 일자상세 팝업 ===\n" + popText);

  // 1구간 입력 — 구간 행이 없으면 "+ 구간 추가"로 생성
  let segTimes = page.locator("input.seg-time");
  if ((await segTimes.count()) < 2) {
    await page.locator("button", { hasText: "구간 추가" }).first().click();
    await page.waitForTimeout(700);
    segTimes = page.locator("input.seg-time");
  }
  const segCnt = await segTimes.count();
  console.log("seg-time 입력 수:", segCnt);
  if (segCnt < 2) { console.log("FAIL: 구간 입력 UI 없음"); await page.screenshot({ path: `${SHOT}/P5-day${day}-fail.png`, fullPage: true }); await closeAll(); return; }
  await segTimes.nth(0).fill(in1);
  await segTimes.nth(1).fill(out1);

  // 2구간 (옵션)
  if (in2 && out2) {
    await page.locator("button", { hasText: "구간 추가" }).first().click();
    await page.waitForTimeout(700);
    const seg2 = page.locator("input.seg-time");
    await seg2.nth(2).fill(in2);
    await seg2.nth(3).fill(out2);
  }

  // 사유 입력(저장 활성 조건) — 팝업 내 마지막 textarea/사유 input
  const reasonEl = page.locator(".prafta-modal-popup textarea, .prafta-modal-popup input[placeholder*='사유']").last();
  if (await reasonEl.count()) await reasonEl.fill("무인테스트 근태 직접 입력");
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${SHOT}/P5-day${day}-input.png`, fullPage: true });

  // 저장
  await page.locator(".prafta-modal-popup button.save-btn").first().click();
  await page.waitForTimeout(1200);
  const result = await drainModal(page, `day${day}-저장`);
  console.log("=== 저장 플로우 종료 ===", result.includes("저장") ? "OK추정" : "확인필요");
  await page.screenshot({ path: `${SHOT}/P5-day${day}-saved.png`, fullPage: true });
  await closeAll();
};
main();
