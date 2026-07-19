// 웹 관리자(8081) QTHR 로그인 스모크 + 근태 화면 진입 확인.
import { webLogin, closeAll } from "./lib/browser.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    console.log("STEP1 웹 로그인 OK, url=", page.url());

    // 근태 화면 하나 진입(라우트 존재 확인 — Attd_01 근태조회 가정, 실패해도 메뉴 텍스트 덤프).
    await page.goto("http://localhost:8081/safenote/main/Attd_01", { waitUntil: "networkidle", timeout: 20000 });
    const text = (await page.evaluate(() => document.body.innerText)).replace(/\n+/g, " | ").slice(0, 400);
    console.log("STEP2 Attd_01 텍스트:", text);
    await page.screenshot({ path: "run/smoke-web-attd01.png", fullPage: true });
    console.log("SMOKE RESULT: PASS");
  } catch (e) {
    console.log("SMOKE RESULT: FAIL —", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
