// QE-3-5 사전 — D 앱 홈 출근 가능 방식 실측(2SEG 완결+길이0 seg2 상태). 출근 성공 여부 관찰.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const waitLoaded = (p) => p.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 }).catch(() => {});

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERD", "QtTest!2026");
    await waitLoaded(page);
    const later = page.locator('button:has-text("나중에")');
    if (await later.count()) { await later.click().catch(() => {}); await page.waitForTimeout(800); }
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== D 홈(앞 900자) ===");
    console.log(text.slice(0, 900).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-3-5", "app", "d-home-before"), fullPage: true });
    // 출근 계열 버튼 후보
    const btns = await page.evaluate(() =>
      [...document.querySelectorAll("button")].filter((b) => b.offsetParent && /출근|퇴근/.test(b.innerText)).map((b) => b.innerText.trim())
    );
    console.log("출근/퇴근 버튼:", JSON.stringify(btns));
    const target = page.locator('button:has-text("출근")').first();
    if ((await target.count()) === 0) { console.log("출근 버튼 없음 — D 불가"); return; }
    await target.click();
    await page.waitForTimeout(1500);
    // 확인 팝업 → 진행
    for (let i = 0; i < 2; i++) {
      const okBtns = await page.evaluate(() => [...document.querySelectorAll("button")].filter((b) => b.offsetParent).map((b) => b.innerText.trim()));
      console.log(`팝업 단계${i} 버튼:`, JSON.stringify(okBtns.slice(0, 15)));
      const proceed = page.locator('button:has-text("확인"), button:has-text("출근하기")').last();
      if (await proceed.count()) { await proceed.click().catch(() => {}); await page.waitForTimeout(2000); }
    }
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 출근 시도 후(앞 700자) ===");
    console.log(text.slice(0, 700).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-3-5", "app", "d-checkin-attempt"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
