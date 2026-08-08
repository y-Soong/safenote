// Phase 4b: QTUSERA 08-11(미결 반차일) 셀에 QT7H 적용→저장 → 스킵 사유 신문구 검증
import { webLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

const main = async () => {
  const { page } = await webLogin("QTHR", "QtTest!2026");
  await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(3000);

  // 1) 근무타입 QT7H(00002) 선택 — select 드롭다운
  await page.locator("select:has(option[value='00002'])").first().selectOption("00002");
  await page.waitForTimeout(500);

  // 2) QTUSERA 행 × 11(화) 열 셀 클릭 (헤더 cellIndex 매칭)
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
  await page.mouse.click(box.x, box.y);
  await page.waitForTimeout(500);

  // 3) 적용
  await page.locator("button", { hasText: "적용" }).first().click();
  await page.waitForTimeout(800);

  // 셀 반영 상태 확인
  const cellAfter = await page.evaluate(() => {
    const table = document.querySelector("table");
    const row = [...table.querySelectorAll("tr")].find((r) => r.innerText.includes("QT사원에이"));
    return row ? row.innerText.replace(/\n/g, "|").slice(0, 200) : "";
  });
  console.log("적용 후 행:", cellAfter);

  // 4) 저장 → Confirm 확인
  await page.locator("button", { hasText: "저장" }).first().click();
  await page.waitForTimeout(1000);
  await page.locator("button", { hasText: "확인" }).last().click().catch(() => {});
  await page.waitForTimeout(2500);

  // 5) 결과 문구 덤프 — ★ 핵심: "반차 또는 시간 단위 연차" 신문구 여부
  const text = await page.evaluate(() => document.body.innerText);
  const idx = text.search(/승인 대기 중인|변경할 수 없습니다|제외|스킵|저장/);
  console.log("=== 저장 결과 문구 ===");
  console.log(idx >= 0 ? text.slice(Math.max(0, idx - 200), idx + 400) : text.slice(0, 400));
  await page.screenshot({ path: `${SHOT}/G2-attd05-pending-lock.png`, fullPage: true });

  // 모달 남아있으면 닫기
  await page.locator("button", { hasText: "확인" }).last().click().catch(() => {});
  await closeAll();
};
main();
