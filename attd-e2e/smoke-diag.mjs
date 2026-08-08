// 스모크 실패 진단 — 로그인 시도 후 화면 상태·API 응답 덤프
import { chromium } from "@playwright/test";
import fs from "node:fs";

const SHOT_DIR = "C:/PRAFTA/.claude/refs/무인테스트_증거";
fs.mkdirSync(SHOT_DIR, { recursive: true });

const main = async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 950 } });
  const page = await ctx.newPage();

  // API 응답 감청
  page.on("response", async (r) => {
    if (r.url().includes("Api/") || r.url().includes("/login")) {
      let body = "";
      try { body = (await r.text()).slice(0, 300); } catch {}
      console.log(`[NET] ${r.status()} ${r.url().replace("http://localhost:8081", "").replace("http://localhost:8080", "[BE]")}`);
      if (body) console.log(`      ${body}`);
    }
  });
  page.on("console", (m) => { if (m.type() === "error") console.log("[CONSOLE-ERR]", m.text().slice(0, 200)); });

  await page.goto("http://localhost:8081/safenote", { waitUntil: "networkidle", timeout: 20000 });
  console.log("=== 로그인 페이지 도달:", page.url());
  await page.screenshot({ path: `${SHOT_DIR}/DIAG-1-login-page.png` });

  // 셀렉터 존재 확인
  for (const sel of ["#userId", 'input[type="password"]', "button.login-btn"]) {
    const n = await page.locator(sel).count();
    console.log(`  셀렉터 ${sel} → ${n}개`);
  }

  await page.fill("#userId", "QTHR").catch((e) => console.log("fill userId FAIL:", e.message.split("\n")[0]));
  await page.fill('input[type="password"]', "QtTest!2026").catch((e) => console.log("fill pw FAIL:", e.message.split("\n")[0]));
  await page.click("button.login-btn").catch((e) => console.log("click FAIL:", e.message.split("\n")[0]));
  await page.waitForTimeout(5000);

  console.log("=== 클릭 5초 후 URL:", page.url());
  const text = await page.evaluate(() => document.body.innerText);
  console.log("=== 화면 텍스트 (상위 600자) ===");
  console.log(text.slice(0, 600));
  await page.screenshot({ path: `${SHOT_DIR}/DIAG-2-after-click.png` });

  await browser.close();
};
main();
