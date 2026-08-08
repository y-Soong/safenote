// Phase 2-2: 반차 신청 UI 정밀 실측 — A-1(파트 카드+경계) · H-1(반반차 부재) · J-1(미배정일)
import { appLogin, closeAll } from "../lib/browser.mjs";
const SHOT = "C:/PRAFTA/.claude/refs/무인테스트_증거";

const dump = async (page, label, n = 700) => {
  const t = await page.evaluate(() => document.body.innerText);
  console.log(`===== ${label} =====`);
  console.log(t.slice(0, n).replace(/\n{3,}/g, "\n\n"));
  return t;
};

const main = async () => {
  const { page } = await appLogin("QTUSERA", "QtTest!2026");
  await page.goto("https://localhost:8082/#/LeaveApplyView", { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(2000);

  // 1) 연차(첫 항목) 선택
  await page.locator("text=연차").first().click().catch((e) => console.log("연차 클릭 실패:", e.message.split("\n")[0]));
  await page.waitForTimeout(1200);
  await dump(page, "1. 연차 종류 선택 후", 500);

  // 2) 날짜 입력 — 필드 탐색
  const inputs = await page.evaluate(() =>
    [...document.querySelectorAll("input, [class*=date], [class*=Date]")].map((i) => ({
      tag: i.tagName, type: i.type || "", ph: i.placeholder || "", cls: (i.className || "").toString().slice(0, 40), val: (i.value || "").slice(0, 16),
    })).filter((x) => x.tag === "INPUT" || x.cls.toLowerCase().includes("date"))
  );
  console.log("=== 입력 필드 ===");
  inputs.forEach((i) => console.log(JSON.stringify(i)));

  // 3) 사용단위 칩 덤프 (H-1)
  const chips = await page.evaluate(() =>
    [...document.querySelectorAll("button, [class*=chip], [class*=unit]")].map((b) => ({
      text: (b.innerText || "").slice(0, 20).replace(/\n/g, " "), cls: (b.className || "").toString().slice(0, 40), disabled: b.disabled,
    })).filter((x) => x.text)
  );
  console.log("=== 버튼/칩 전체 ===");
  chips.forEach((c) => console.log(JSON.stringify(c)));

  await page.screenshot({ path: `${SHOT}/P2b-1-typeSelected.png`, fullPage: true });
  await closeAll();
};
main();
