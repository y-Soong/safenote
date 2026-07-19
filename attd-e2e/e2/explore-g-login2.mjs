// QTUSERG 로그인 상세 프로브 — 게이트/얼럿 관찰.
import { chromium } from "@playwright/test";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ ignoreHTTPSErrors: true, viewport: { width: 390, height: 844 } });
  const page = await ctx.newPage();
  page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
  page.on("response", (r) => { if (r.url().includes("login")) console.log("RES:", r.status(), r.url()); });
  await page.goto("https://localhost:8082/", { waitUntil: "networkidle", timeout: 20000 });
  await page.fill('input[placeholder="아이디를 입력하세요"]', "QTUSERG");
  await page.fill('input[placeholder="비밀번호를 입력하세요"]', "QtTest!2026");
  await page.click("button.btn-login");
  await page.waitForTimeout(6000);
  console.log("URL:", page.url());
  const text = await page.evaluate(() => document.body.innerText);
  console.log(text.slice(0, 1000));
  await page.screenshot({ path: shotPath("QE-2-x", "app", "g-login-probe2"), fullPage: true });
  await browser.close();
};
main();
