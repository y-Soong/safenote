// 약관동의 이후 단계 추적 — 다음 게이트 확인
import { chromium } from "@playwright/test";
import fs from "node:fs";
const SHOT_DIR = "C:/PRAFTA/.claude/refs/무인테스트_증거";
fs.mkdirSync(SHOT_DIR, { recursive: true });

const main = async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await (await browser.newContext({ viewport: { width: 1600, height: 950 } })).newPage();
  page.on("response", async (r) => {
    if (r.url().includes("Api/")) {
      let body = ""; try { body = (await r.text()).slice(0, 200); } catch {}
      const u = r.url().replace(/http:\/\/localhost:808[01]/, "").split("?")[0];
      console.log(`[NET] ${r.status()} ${u}  ${body.slice(0, 150)}`);
    }
  });

  await page.goto("http://localhost:8081/safenote", { waitUntil: "networkidle", timeout: 20000 });
  await page.fill("#userId", "QTHR");
  await page.fill('input[type="password"]', "QtTest!2026");
  await page.click("button.login-btn");
  await page.waitForTimeout(2500);

  console.log("--- 약관 게이트 처리 ---");
  await page.locator("text=전체 동의하기").first().click();
  await page.waitForTimeout(500);
  await page.locator('button:has-text("약관동의")').first().click();
  await page.waitForTimeout(4000);

  console.log("=== 약관동의 후 URL:", page.url());
  const text = await page.evaluate(() => document.body.innerText);
  console.log("=== 화면 텍스트 ===");
  console.log(text.slice(0, 500));
  await page.screenshot({ path: `${SHOT_DIR}/DIAG-3-after-terms.png` });
  await browser.close();
};
main();
