// P5 재시도: AdminAttdDetail 날짜를 08-06/08-07 로 — 날짜 표시 클릭 → 스테퍼 시트
import { appLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

async function setDay(page, d) {
  await page.locator("text=/2026\\.08\\.\\d{2}/").first().click();
  await page.waitForSelector(".wp-keyin__in--y", { timeout: 5000 }).catch(() => null);
  if (!(await page.locator(".wp-keyin__in--y").count())) { console.log("(스테퍼 미오픈)"); return false; }
  const setIn = async (sel, val) => {
    const el = page.locator(sel);
    await el.click(); await el.fill(String(val)); await el.dispatchEvent("blur");
  };
  await setIn(".wp-keyin__in--y", 2026);
  await setIn(".wp-keyin__in:not(.wp-keyin__in--y) >> nth=0", "08");
  await setIn(".wp-keyin__in:not(.wp-keyin__in--y) >> nth=1", d);
  await page.locator("button.wp-confirm").click();
  await page.waitForTimeout(2500);
  return true;
}

const main = async () => {
  const { page } = await appLogin("QTHR", "QtTest!2026");
  await page.goto("https://localhost:8082/#/AdminAttdDetail", { timeout: 15000 });
  await page.waitForTimeout(3000);

  for (const d of ["06", "07", "05"]) {
    if (!(await setDay(page, d))) break;
    const t = await page.evaluate(() => document.body.innerText);
    // QTUSERA 근처 텍스트
    const i = t.indexOf("QT사원에이");
    console.log(`=== [P5] 08-${d} ===`);
    console.log(i >= 0 ? t.slice(Math.max(0, i - 60), i + 260).replace(/\n{2,}/g, "\n") : t.slice(0, 250).replace(/\n{2,}/g, "\n"));
    await page.screenshot({ path: `${SHOT}/P5-admin-08${d}.png`, fullPage: true });
  }
  await closeAll();
};
main();
