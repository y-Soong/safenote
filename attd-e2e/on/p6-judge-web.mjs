// Phase 6: 판정 대조 웹면 — Attd_08(P1) · Attd_11(P2)
// 기대: 08-05 지각 1분 / 08-06 정상 / 08-04 결근(반차+무출근) / 08-07 2구간
import { webLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

const main = async () => {
  const { page } = await webLogin("QTHR", "QtTest!2026");

  // ── P1: Attd_08 ──
  await page.goto("http://localhost:8081/safenote/main/Attd_08", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2500);
  await page.locator("button", { hasText: "조회" }).first().click().catch(() => {});
  await page.waitForTimeout(3500);

  const rowsA = await page.evaluate(() => {
    const out = [];
    document.querySelectorAll("tr").forEach((tr) => {
      if (tr.innerText.includes("QT사원에이")) out.push(tr.innerText.replace(/\n/g, " | ").slice(0, 400));
    });
    return out;
  });
  console.log("=== [P1 Attd_08] QTUSERA 행 ===");
  rowsA.forEach((r) => console.log(r));
  await page.screenshot({ path: `${SHOT}/P6-attd08.png`, fullPage: true });

  // 행 클릭 → 일자별 상세 패널이 있으면 덤프
  const rowEl = page.locator("tr", { hasText: "QT사원에이" }).first();
  await rowEl.click().catch(() => {});
  await page.waitForTimeout(2000);
  const detail = await page.evaluate(() => {
    const t = document.body.innerText;
    const i = t.indexOf("08.04");
    return i >= 0 ? t.slice(i - 100, i + 800) : "(일자 패널 미검출)";
  });
  console.log("=== [P1] 일자별 패널 (08-04~) ===");
  console.log(detail.replace(/\n{2,}/g, "\n").slice(0, 900));
  await page.screenshot({ path: `${SHOT}/P6-attd08-detail.png`, fullPage: true });

  // ── P2: Attd_11 ──
  await page.goto("http://localhost:8081/safenote/main/Attd_11", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2500);
  await page.locator("button", { hasText: "조회" }).first().click().catch(() => {});
  await page.waitForTimeout(3500);
  const rowsB = await page.evaluate(() => {
    const out = [];
    document.querySelectorAll("tr").forEach((tr) => {
      if (tr.innerText.includes("QT사원에이")) out.push(tr.innerText.replace(/\n/g, " | ").slice(0, 400));
    });
    // 헤더도
    const head = document.querySelector("table tr");
    out.unshift("[HEAD] " + (head ? head.innerText.replace(/\n/g, " | ").slice(0, 400) : ""));
    return out;
  });
  console.log("=== [P2 Attd_11] ===");
  rowsB.forEach((r) => console.log(r));
  await page.screenshot({ path: `${SHOT}/P6-attd11.png`, fullPage: true });

  await closeAll();
};
main();
