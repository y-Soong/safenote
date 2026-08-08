// Phase 4c: Attd_05 저장 — 모달 드레인 방식 (각 단계에서 모달 내용 덤프 후 닫기)
import { webLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

// 떠 있는 모달의 텍스트를 덤프하고 확인/닫기 버튼으로 해소
async function drainModal(page, label) {
  for (let i = 0; i < 3; i++) {
    const modal = page.locator(".prafta-modal-popup").last();
    if (!(await modal.count()) || !(await modal.isVisible().catch(() => false))) return;
    const t = (await modal.innerText().catch(() => "")).replace(/\n{2,}/g, "\n");
    console.log(`--- [모달@${label}] ---`);
    console.log(t.slice(0, 500));
    const btn = modal.locator("button", { hasText: /확인|닫기|취소/ }).first();
    if (await btn.count()) await btn.click().catch(() => {});
    else await page.keyboard.press("Escape").catch(() => {});
    await page.waitForTimeout(800);
  }
}

const main = async () => {
  const { page } = await webLogin("QTHR", "QtTest!2026");
  await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(3000);
  await drainModal(page, "초기");

  await page.locator("select:has(option[value='00002'])").first().selectOption("00002");
  await page.waitForTimeout(400);
  await drainModal(page, "타입선택후");

  const box = await page.evaluate(() => {
    const table = document.querySelector("table");
    const headRow = table.querySelector("tr");
    let colIdx = -1;
    [...headRow.cells].forEach((c, i) => { if (c.innerText.trim().startsWith("11(")) colIdx = i; });
    const row = [...table.querySelectorAll("tr")].find((r) => r.innerText.includes("QT사원에이"));
    if (colIdx < 0 || !row) return null;
    const cell = row.cells[colIdx];
    const r = cell.getBoundingClientRect();
    return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: cell.innerText.trim() };
  });
  if (!box) { console.log("FAIL: 셀 좌표 미확보"); await closeAll(); return; }
  console.log("11일 셀 현재값:", JSON.stringify(box.text));
  // 배지(하단, click.stop)가 아닌 셀 상단 25% 지점을 드래그 모델(mousedown→mouseup)로 선택
  const selY = box.y - 10;
  await page.mouse.move(box.x, selY);
  await page.mouse.down();
  await page.waitForTimeout(150);
  await page.mouse.up();
  await page.waitForTimeout(600);
  await drainModal(page, "셀클릭후");
  // 선택 표시 확인
  const selInfo = await page.evaluate(() => {
    const sel = document.querySelectorAll("[class*=selected], [class*=--sel], td.sel");
    return sel.length;
  });
  console.log("선택 표시 요소 수:", selInfo);

  await page.locator("button", { hasText: "적용" }).first().click({ timeout: 8000 }).catch((e) => console.log("적용 실패:", e.message.split("\n")[0]));
  await page.waitForTimeout(800);
  await drainModal(page, "적용후");

  await page.locator("button", { hasText: "저장" }).first().click({ timeout: 8000 }).catch((e) => console.log("저장 실패:", e.message.split("\n")[0]));
  await page.waitForTimeout(1200);
  await drainModal(page, "저장확인");   // Confirm("저장하시겠습니까?") → 확인
  await page.waitForTimeout(2500);
  await page.screenshot({ path: `${SHOT}/G2-attd05-result.png`, fullPage: true });
  await drainModal(page, "저장결과");   // ★ 결과 모달 — 스킵 사유 문구 기대

  await closeAll();
};
main();
