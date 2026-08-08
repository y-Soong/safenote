// 앱 로그인 게이트 진단
import { chromium } from "@playwright/test";
import fs from "node:fs";
const SHOT_DIR = "C:/PRAFTA/.claude/refs/무인테스트_증거";
fs.mkdirSync(SHOT_DIR, { recursive: true });

const main = async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({
    ignoreHTTPSErrors: true,
    geolocation: { latitude: 37.5665, longitude: 126.978 },
    permissions: ["geolocation"],
    viewport: { width: 390, height: 844 },
  });
  const page = await ctx.newPage();
  page.on("response", async (r) => {
    if (r.url().includes("Api/")) {
      let body = ""; try { body = (await r.text()).slice(0, 180); } catch {}
      console.log(`[NET] ${r.status()} ${r.url().replace(/https?:\/\/localhost:808[02]/, "").split("?")[0]}  ${body.slice(0, 140)}`);
    }
  });

  await page.goto("https://localhost:8082/", { waitUntil: "networkidle", timeout: 20000 });
  await page.fill('input[placeholder="아이디를 입력하세요"]', "QTUSERA");
  await page.fill('input[placeholder="비밀번호를 입력하세요"]', "QtTest!2026");
  await page.click("button.btn-login");
  await page.waitForTimeout(5000);

  console.log("=== 5초 후 URL:", page.url());
  const text = await page.evaluate(() => document.body.innerText);
  console.log("=== 화면 텍스트 ===");
  console.log(text.slice(0, 600));
  await page.screenshot({ path: `${SHOT_DIR}/DIAG-4-app-after-login.png` });
  await browser.close();
};
main();
