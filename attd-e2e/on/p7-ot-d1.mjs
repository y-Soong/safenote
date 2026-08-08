// Phase 7: ★★ D-1 — 종료기준 반차일(08-07) 2차 재출근 구간 OT 등록
// 기대: 15:00~19:00 전량 → 거부(연차 면제 겹침) / 18:00~19:00 → 성공
// + FE 칩(.ot-allowed-item)이 연차 면제를 반영하지 않는 것도 실측 기록
import { webLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";
const [otIn, otOut, tag] = process.argv.slice(2); // 예: 1500 1900 deny

async function drain(page, label) {
  const out = [];
  for (let i = 0; i < 4; i++) {
    const modals = page.locator(".prafta-modal-popup");
    const n = await modals.count();
    if (!n) break;
    const top = modals.last();
    const t = (await top.innerText().catch(() => "")).replace(/\n{2,}/g, "\n");
    // 일자상세 팝업 자체는 유지, 알림/확인 모달만 처리
    if (t.includes("QT사원에이") && !t.includes("하시겠습니까") && !t.includes("실패") && !t.includes("벗어났")) break;
    out.push(t.slice(0, 300));
    console.log(`--- [모달@${label}] ---\n${t.slice(0, 300)}`);
    await top.locator("button", { hasText: /확인|닫기/ }).first().click().catch(() => {});
    await page.waitForTimeout(700);
  }
  return out.join("\n");
}

const main = async () => {
  const { page } = await webLogin("QTHR", "QtTest!2026");
  await page.goto("http://localhost:8081/safenote/main/Attd_07", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2500);
  await page.locator("button", { hasText: "조회" }).first().click().catch(() => {});
  await page.waitForTimeout(3000);
  const cellBox = await page.evaluate(() => {
    const row = [...document.querySelectorAll("tr")].find((r) => r.innerText.includes("QT사원에이"));
    const head = row.closest("table").querySelector("tr");
    let colIdx = -1;
    [...head.cells].forEach((c, i) => { if (c.innerText.trim().split("\n")[0] === "7") colIdx = i; });
    const r = row.cells[colIdx].getBoundingClientRect();
    return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
  });
  await page.mouse.dblclick(cellBox.x, cellBox.y);
  await page.waitForTimeout(2500);

  // FE 허용 칩 덤프 (검토자 지적 실측)
  const chips = await page.evaluate(() =>
    [...document.querySelectorAll(".ot-allowed-item")].map((c) => c.innerText.replace(/\n/g, " "))
  );
  console.log("[FE 칩] 등록 가능 범위:", JSON.stringify(chips));

  // 2구간 OT 추가
  await page.locator("button", { hasText: "2구간 초과근무 추가" }).first().click();
  await page.waitForTimeout(700);
  const otTimes = page.locator("input.ot-time");
  const n = await otTimes.count();
  console.log("ot-time 입력 수:", n);
  const typeTime = async (el, v) => {
    await el.click(); await el.press("Control+a");
    await el.pressSequentially(v, { delay: 40 });
    await el.dispatchEvent("blur"); await page.waitForTimeout(150);
  };
  await typeTime(otTimes.nth(n - 2), otIn);
  await typeTime(otTimes.nth(n - 1), otOut);

  // OT 행 체크박스 확인/체크 + 상태 덤프
  const otState = await page.evaluate(() => {
    return [...document.querySelectorAll(".ot-row")].map((r) => ({
      checked: r.querySelector("input[type=checkbox]")?.checked,
      times: [...r.querySelectorAll("input.ot-time")].map((i) => i.value),
    }));
  });
  console.log("OT 행 상태:", JSON.stringify(otState));
  for (const [idx, s] of otState.entries()) {
    if (s.checked === false) {
      await page.locator(".ot-row input[type=checkbox]").nth(idx).check();
    }
  }

  // OT 행 내 사유 입력(있으면) — ot-row 안의 텍스트 입력
  const otReason = page.locator(".ot-row input[type=text]:not(.ot-time), .ot-row textarea").last();
  if (await otReason.count()) await otReason.fill(`무인테스트 OT ${tag}`).catch(() => {});
  await page.screenshot({ path: `${SHOT}/D1-ot-${tag}-input.png`, fullPage: true });

  // OT 전용 저장 버튼 (정규 저장 .save-btn 과 별개)
  await page.locator("button.ot-save-btn").last().click();
  await page.waitForTimeout(1200);
  await drain(page, `OT-${tag}`);
  await page.waitForTimeout(800);
  await drain(page, `OT-${tag}-2`);
  await page.screenshot({ path: `${SHOT}/D1-ot-${tag}-result.png`, fullPage: true });
  await closeAll();
};
main();
