// P1 프론트 검증 — Subcon_01 '내 회사코드 복사' 버튼 실제 브라우저 클릭 + 클립보드 검증.
// A(원청) master 로 UI 로그인 → Subcon_01 진입 → 버튼 클릭 → 클립보드 == A 회사코드 확인.
import { chromium } from "@playwright/test";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

const outPath = (name) => fileURLToPath(new URL(`./run/${name}`, import.meta.url));

const BASE = "http://localhost:8081";
const A = JSON.parse(readFileSync(new URL("./run/A_PRIME.session.json", import.meta.url), "utf8"));
const EXPECT = A.cmpnyCd; // nrTnBjSa2woeztqfPGIP
const PW = A.newPw || "Prafta!2026";

const main = async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext();
  await ctx.grantPermissions(["clipboard-read", "clipboard-write"], { origin: BASE });
  const page = await ctx.newPage();
  const logs = [];
  page.on("console", (m) => logs.push(`[console.${m.type()}] ${m.text()}`));

  try {
    // 1) 로그인 페이지.
    await page.goto(`${BASE}/safenote`, { waitUntil: "networkidle" });
    await page.fill("#userId", "subt_prime");
    await page.fill('input[type="password"]', PW);
    await page.click("button.login-btn");

    // 2) 메인 진입 대기.
    await page.waitForURL(/\/safenote\/main/, { timeout: 15000 });
    console.log("로그인 성공 → 메인 진입");

    // 3) Subcon_01 직접 진입.
    await page.goto(`${BASE}/safenote/main/Subcon_01`, { waitUntil: "networkidle" });
    await page.waitForSelector("text=연동 중인 회사", { timeout: 15000 });
    console.log("Subcon_01 화면 로드됨");

    // 4) 복사 버튼 클릭(클립보드 초기화 후).
    await page.evaluate(() => navigator.clipboard.writeText("__CLEARED__"));
    const btn = page.getByRole("button", { name: "내 회사코드 복사" });
    await btn.waitFor({ state: "visible", timeout: 10000 });
    await btn.click();
    await page.waitForTimeout(500); // writeText + $alert 반영 대기

    // 5) 클립보드 검증.
    const clip = await page.evaluate(() => navigator.clipboard.readText());
    console.log(`클립보드 값: "${clip}"`);
    console.log(`기대 회사코드: "${EXPECT}"`);
    const pass = clip === EXPECT;
    console.log(pass ? "✅ PASS — 복사된 값이 A 회사코드와 일치" : "❌ FAIL — 불일치");

    await page.screenshot({ path: outPath("p1-copybtn.png"), fullPage: true });
    console.log("스크린샷 → run/p1-copybtn.png");

    process.exitCode = pass ? 0 : 1;
  } catch (e) {
    console.error("테스트 오류:", e.message);
    console.log("최근 콘솔 로그:\n" + logs.slice(-10).join("\n"));
    try { await page.screenshot({ path: outPath("p1-copybtn-error.png"), fullPage: true }); } catch {}
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
};

main();
