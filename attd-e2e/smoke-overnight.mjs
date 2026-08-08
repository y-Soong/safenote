// 무인 야간 테스트 사전 스모크 — 웹/앱 로그인 + 스크린샷 (2026-08-08)
import { webLogin, appLogin, closeAll } from "./lib/browser.mjs";

const SHOT_DIR = "C:/PRAFTA/.claude/refs/무인테스트_증거";
import fs from "node:fs";
fs.mkdirSync(SHOT_DIR, { recursive: true });

const main = async () => {
  let fail = 0;
  // 1) 웹 관리자 로그인
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.screenshot({ path: `${SHOT_DIR}/SMOKE-web-login.png` });
    console.log("[SMOKE-6a] 웹 로그인 OK →", page.url());
  } catch (e) {
    console.log("[SMOKE-6a] 웹 로그인 FAIL:", e.message.split("\n")[0]);
    fail = 1;
  }
  // 2) 앱 근로자 로그인 (geo mock 포함)
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.screenshot({ path: `${SHOT_DIR}/SMOKE-app-login.png` });
    console.log("[SMOKE-6b] 앱 로그인 OK →", page.url());
  } catch (e) {
    console.log("[SMOKE-6b] 앱 로그인 FAIL:", e.message.split("\n")[0]);
    fail = 1;
  }
  await closeAll();
  process.exitCode = fail;
};
main();
