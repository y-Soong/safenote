// 약관 팝업 내부 구조 덤프 → 정확한 셀렉터 확정
import { chromium } from "@playwright/test";

const main = async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await (await browser.newContext({ viewport: { width: 1600, height: 950 } })).newPage();

  await page.goto("http://localhost:8081/safenote", { waitUntil: "networkidle", timeout: 20000 });
  await page.fill("#userId", "QTHR");
  await page.fill('input[type="password"]', "QtTest!2026");
  await page.click("button.login-btn");
  await page.waitForTimeout(3000);

  // 팝업 내 인터랙티브 요소 전체 덤프
  const els = await page.evaluate(() => {
    const out = [];
    document.querySelectorAll("input, button, label, [class*=check], [class*=agree]").forEach((el) => {
      const r = el.getBoundingClientRect();
      if (r.width === 0) return;
      out.push({
        tag: el.tagName, type: el.type || "", cls: (el.className || "").toString().slice(0, 50),
        text: (el.innerText || el.value || "").slice(0, 30).replace(/\n/g, " "),
        checked: el.checked, disabled: el.disabled,
      });
    });
    return out;
  });
  console.log("=== 클릭 가능 요소 ===");
  els.forEach((e) => console.log(JSON.stringify(e)));

  // 전체 동의 클릭 시도 → 상태 재확인
  await page.locator("text=전체 동의하기").first().click().catch((e) => console.log("클릭 실패:", e.message.split("\n")[0]));
  await page.waitForTimeout(800);
  const after = await page.evaluate(() =>
    [...document.querySelectorAll('input[type=checkbox]')].map((c) => ({ cls: (c.className||"").slice(0,30), checked: c.checked }))
  );
  console.log("=== 전체동의 클릭 후 체크박스 ===", JSON.stringify(after));

  const btn = await page.evaluate(() =>
    [...document.querySelectorAll("button")].filter((b) => b.innerText.includes("약관동의")).map((b) => ({ cls: b.className.slice(0, 40), disabled: b.disabled, text: b.innerText }))
  );
  console.log("=== 약관동의 버튼 ===", JSON.stringify(btn));

  await browser.close();
};
main();
