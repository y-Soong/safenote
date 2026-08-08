// Phase 1-1: Baim_07 사용단위 FULL_DAY → 30분 단위(전체 허용) 변경
// 검증 겸행: H-2 (반반차 라디오 소멸 확인)
import { webLogin, closeAll } from "../lib/browser.mjs";
import fs from "node:fs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

const main = async () => {
  const { page } = await webLogin("QTHR", "QtTest!2026");
  await page.goto("http://localhost:8081/safenote/main/Baim_07", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2500);

  // 화면 전체 텍스트 + 라디오 덤프 (H-2 검증)
  const text = await page.evaluate(() => document.body.innerText);
  console.log("=== Baim_07 화면 텍스트 (사용단위 근처) ===");
  const idx = text.indexOf("사용");
  console.log(text.slice(Math.max(0, idx - 100), idx + 700));

  const radios = await page.evaluate(() =>
    [...document.querySelectorAll('input[type=radio]')].map((r) => {
      const label = r.closest("label");
      return { name: r.name, value: r.value, checked: r.checked, text: (label ? label.innerText : "").slice(0, 30) };
    })
  );
  console.log("=== 라디오 전체 ===");
  radios.forEach((r) => console.log(JSON.stringify(r)));

  await page.screenshot({ path: `${SHOT}/P1-baim07-before.png`, fullPage: true });
  console.log("=== (여기까지 조회. 변경은 다음 스크립트에서) ===");
  await closeAll();
};
main();
