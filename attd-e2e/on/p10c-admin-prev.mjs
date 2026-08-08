import { appLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";
const main = async () => {
  const { page } = await appLogin("QTHR", "QtTest!2026");
  await page.goto("https://localhost:8082/#/AdminAttdDetail", { timeout: 15000 });
  await page.waitForTimeout(3000);
  for (let k = 1; k <= 3; k++) {
    await page.locator('button[aria-label="이전 날짜"]').click();
    await page.waitForTimeout(2000);
    const t = await page.evaluate(() => document.body.innerText);
    const d = (t.match(/2026\.08\.\d{2}[^\n]*/) || [""])[0];
    const i = t.indexOf("QT사원에이");
    console.log(`=== [P5] ${d} ===`);
    console.log(i >= 0 ? t.slice(Math.max(0, i - 80), i + 240).replace(/\n{2,}/g, "\n") : "(QTUSERA 미노출)\n" + t.slice(0, 200).replace(/\n{2,}/g, "\n"));
    await page.screenshot({ path: `${SHOT}/P5-admin-back${k}.png`, fullPage: true });
  }
  await closeAll();
};
main();
