// Phase 4: Attd_05 미결 반차 잠금 — e0b88178 신문구 검증 (G-2 + F-7 정정분)
// QTHR → Attd_05 → 8월 → QTUSERA 08-11 셀에 QT7H 배정 시도 → 스킵 사유 문구 확인
import { webLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

const main = async () => {
  const { page } = await webLogin("QTHR", "QtTest!2026");
  await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(3000);

  const text = await page.evaluate(() => document.body.innerText);
  console.log("=== Attd_05 초기 화면 (상위 500자) ===");
  console.log(text.slice(0, 500).replace(/\n{2,}/g, "\n"));

  // 그리드/셀 구조 파악
  const cells = await page.evaluate(() => {
    const out = { tables: document.querySelectorAll("table").length, rows: [] };
    document.querySelectorAll("tr").forEach((tr, i) => {
      const t = tr.innerText.replace(/\n/g, "|").slice(0, 100);
      if (t.includes("QT사원에이") || (i < 4 && t)) out.rows.push({ i, t });
    });
    return out;
  });
  console.log("=== 그리드 구조 ===", JSON.stringify(cells.tables));
  cells.rows.forEach((r) => console.log(`row[${r.i}]`, r.t));

  await page.screenshot({ path: `${SHOT}/P4-attd05-grid.png`, fullPage: true });
  await closeAll();
};
main();
