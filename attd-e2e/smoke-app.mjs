// 앱 프론트(8082, https) 브라우저 단독 스모크 — QTUSERA 로그인 → 홈(MainView) 도달.
// 브리지 폴백 검증: deviceId(localStorage UUID), GPS(브라우저 geolocation 주입).
import { chromium } from "@playwright/test";

const APP = "https://localhost:8082";
// QT 사업장(00010) 좌표는 아직 모름 — 스모크에서는 서울시청 좌표 주입(지오펜스는 본 세션에서 다룸).
const GEO = { latitude: 37.5665, longitude: 126.978 };

const main = async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({
    ignoreHTTPSErrors: true,
    geolocation: GEO,
    permissions: ["geolocation"],
    viewport: { width: 390, height: 844 }, // 모바일 근사(iPhone 12급)
  });
  const page = await ctx.newPage();
  const consoleLogs = [];
  page.on("console", (m) => consoleLogs.push(`[${m.type()}] ${m.text()}`));
  page.on("pageerror", (e) => consoleLogs.push(`[pageerror] ${e.message}`));

  try {
    // 1) 로그인 화면.
    await page.goto(`${APP}/`, { waitUntil: "networkidle", timeout: 20000 });
    await page.screenshot({ path: "run/smoke-app-1-login.png" });
    console.log("STEP1 로그인 화면 로드 OK, title=", await page.title());

    // 2) 정규직 로그인 (기본 REGULAR 탭).
    await page.fill('input[placeholder="아이디를 입력하세요"]', "QTUSERA");
    await page.fill('input[placeholder="비밀번호를 입력하세요"]', "QtTest!2026");
    await page.click("button.btn-login");

    // 3) 홈 도달 대기.
    await page.waitForURL(/MainView/, { timeout: 20000 });
    await page.waitForLoadState("networkidle");
    await page.screenshot({ path: "run/smoke-app-2-main.png", fullPage: true });
    console.log("STEP2 MainView 도달 OK, url=", page.url());

    // 4) 브리지 폴백 확인.
    const deviceId = await page.evaluate(() => localStorage.getItem("gv_deviceId"));
    console.log("STEP3 deviceId 폴백:", deviceId ? `OK (${deviceId.slice(0, 13)}...)` : "NULL(미생성)");

    // 5) 홈 텍스트 일부 덤프(어떤 위젯이 뜨는지).
    const bodyText = (await page.evaluate(() => document.body.innerText)).replace(/\n+/g, " | ").slice(0, 600);
    console.log("STEP4 홈 텍스트:", bodyText);

    console.log("SMOKE RESULT: PASS");
  } catch (e) {
    await page.screenshot({ path: "run/smoke-app-fail.png", fullPage: true }).catch(() => {});
    console.log("SMOKE RESULT: FAIL —", e.message);
    console.log("콘솔 로그 말미:", consoleLogs.slice(-15).join("\n"));
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
};
main();
