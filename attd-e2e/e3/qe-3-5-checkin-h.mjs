// QE-3-5 — H 앱 출근(OPEN 슬롯 만들기). 홈 상태 채집 → 출근하기 → 팝업 처리.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const waitLoaded = (p) => p.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 }).catch(() => {});

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERH", "QtTest!2026");
    await waitLoaded(page);
    const later = page.locator('button:has-text("나중에")');
    if (await later.count()) { await later.click().catch(() => {}); await page.waitForTimeout(800); }
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== H 홈 before(앞 700자) ===");
    console.log(text.slice(0, 700).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-3-5", "app", "h-home-before"), fullPage: true });
    const inBtn = page.locator('button:has-text("출근하기")').first();
    if ((await inBtn.count()) === 0) { console.log("출근하기 버튼 없음"); return; }
    console.log("출근하기 disabled=", await inBtn.isDisabled());
    if (await inBtn.isDisabled()) { console.log("H도 선차단 — 불가"); return; }
    await inBtn.click();
    await page.waitForTimeout(1500);
    for (let i = 0; i < 3; i++) {
      const vis = await page.evaluate(() => [...document.querySelectorAll("button")].filter((b) => b.offsetParent).map((b) => b.innerText.trim()).filter(Boolean));
      console.log(`단계${i} 보이는 버튼:`, JSON.stringify(vis.slice(0, 15)));
      const body = await page.evaluate(() => document.body.innerText);
      const tail = body.split("\n").filter((l) => l.trim()).slice(-8).join(" | ");
      console.log(`단계${i} 하단:`, tail);
      // 확인/출근하기 계열 진행
      const proceed = page.locator('.modal-overlay button:has-text("확인"), .modal-overlay button:has-text("출근")').last();
      if (await proceed.count()) { await proceed.click().catch(() => {}); await page.waitForTimeout(2000); } else break;
    }
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 출근 시도 후(앞 700자) ===");
    console.log(text.slice(0, 700).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-3-5", "app", "h-checkin-after"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
