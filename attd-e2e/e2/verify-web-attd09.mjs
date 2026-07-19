// 웹 Attd_09 사용자 연차관리 조회/캡처 공용 — node e2/verify-web-attd09.mjs <caseId> [사용자명필터]
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , caseId = "QE-2-x", nameFilter = ""] = process.argv;
const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_09", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2500);
    // 조회 버튼이 있으면 클릭
    const q = page.locator('button:has-text("조회")').first();
    if (await q.count()) { await q.click(); await page.waitForTimeout(2500); }
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== Attd_09 ===");
    if (nameFilter) {
      const lines = text.split("\n");
      const hits = [];
      lines.forEach((l, i) => { if (l.includes(nameFilter)) hits.push(lines.slice(Math.max(0, i - 1), i + 6).join(" | ")); });
      console.log(hits.join("\n---\n") || "(필터 매칭 없음)");
      console.log("=== 전체 상단 ===");
      console.log(text.slice(0, 1500));
    } else {
      console.log(text.slice(0, 2500));
    }
    await page.screenshot({ path: shotPath(caseId, "web", "attd09"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
