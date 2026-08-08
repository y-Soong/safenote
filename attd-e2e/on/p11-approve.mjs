// M: 일자상세(08-04) 연차 사용 요청 승인 → REQ 02 전환 + 반차 표기
import { webLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";
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
    [...head.cells].forEach((c, i) => { if (c.innerText.trim().split("\n")[0] === "4") colIdx = i; });
    const r = row.cells[colIdx].getBoundingClientRect();
    return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
  });
  await page.mouse.dblclick(cellBox.x, cellBox.y);
  await page.waitForTimeout(2500);
  // 요청 카드 덤프
  const t = await page.evaluate(() => document.body.innerText);
  const i = t.indexOf("연차 사용 요청");
  console.log("=== 요청 카드 ===");
  console.log(i >= 0 ? t.slice(i, i + 260).replace(/\n{2,}/g, "\n") : "(요청 카드 미검출)");
  await page.screenshot({ path: `${SHOT}/M-approve-before.png`, fullPage: true });
  // 승인 클릭
  await page.locator(".prafta-modal-popup button", { hasText: "승인" }).first().click();
  await page.waitForTimeout(1000);
  for (let k = 0; k < 3; k++) {
    const modal = page.locator(".prafta-modal-popup").last();
    const mt = (await modal.innerText().catch(() => "")).slice(0, 150);
    if (mt.includes("하시겠습니까") || mt.includes("승인") && mt.length < 100) {
      console.log("모달:", mt.split("\n").slice(0, 2).join(" / "));
      await modal.locator("button", { hasText: "확인" }).first().click().catch(() => {});
      await page.waitForTimeout(1000);
    } else break;
  }
  await page.waitForTimeout(1500);
  const t2 = await page.evaluate(() => document.body.innerText);
  const i2 = t2.search(/승인 완료|처리되었|승인되었/);
  console.log("=== 승인 후 ===", i2 >= 0 ? t2.slice(i2 - 50, i2 + 150).replace(/\n{2,}/g, " ") : "(문구 미검출 — DB로 확인)");
  await page.screenshot({ path: `${SHOT}/M-approve-after.png`, fullPage: true });
  await closeAll();
};
main();
