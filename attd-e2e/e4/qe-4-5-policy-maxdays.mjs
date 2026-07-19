// QE-4-5 ✅ 연차정책 maxDays 변경 — node e4/qe-4-5-policy-maxdays.mjs <값> <라벨> [사유]
// 24=클라 가드 관찰(사유 모달 전 선차단) / 25=변경 저장 / 26=원복 저장
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , valStr, label, reason = "[QE-4-5] E2E 정책 변경 검증"] = process.argv;

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Baim_07", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3500);
    // 최대 연차일수 input — "최대 연차일수" 라벨이 있는 subbox 내 number input
    const cur = await page.evaluate(() => {
      const box = [...document.querySelectorAll(".lp-subbox")].find((b) => b.innerText.includes("최대 연차일수"));
      const inp = box?.querySelector("input[type=number]");
      return inp ? inp.value : null;
    });
    console.log("현재 maxDays 입력값:", cur);
    await page.evaluate((v) => {
      const box = [...document.querySelectorAll(".lp-subbox")].find((b) => b.innerText.includes("최대 연차일수"));
      const inp = box?.querySelector("input[type=number]");
      if (!inp) throw new Error("maxDays input 미발견");
      inp.value = v;
      inp.dispatchEvent(new Event("input", { bubbles: true }));
    }, valStr);
    await page.waitForTimeout(600);
    // 하단 저장
    await page.locator(".lp-footer button:has-text('저장')").click();
    await page.waitForTimeout(1500);
    let body = await page.evaluate(() => document.body.innerText);
    // 사유 모달 노출 여부
    const hasReason = await page.locator(".reason-modal__textarea").count();
    console.log("사유 모달 노출:", hasReason > 0);
    if (hasReason === 0) {
      // 클라 가드 얼럿 예상 — 텍스트 채집
      const overlay = await page.evaluate(() => {
        const o = [...document.querySelectorAll(".modal-overlay")].at(-1);
        return o ? o.innerText.trim().replace(/\n/g, " | ") : "(모달 없음)";
      });
      console.log("가드 얼럿:", overlay);
      await page.screenshot({ path: shotPath("QE-4-5", "web", `guard-${label}`) });
      const ok = page.locator('button:has-text("확인")').last();
      if (await ok.count()) await ok.click().catch(() => {});
    } else {
      await page.fill(".reason-modal__textarea", reason);
      await page.waitForTimeout(400);
      await page.locator('button:has-text("확인")').last().click();
      await page.waitForTimeout(2500);
      const after = await page.evaluate(() => {
        const o = [...document.querySelectorAll(".modal-overlay")].at(-1);
        return o ? o.innerText.trim().replace(/\n/g, " | ") : "(모달 없음)";
      });
      console.log("저장 결과 모달:", after);
      await page.screenshot({ path: shotPath("QE-4-5", "web", `saved-${label}`) });
      for (let i = 0; i < 2; i++) {
        const ok = page.locator('button:has-text("확인")').last();
        if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1000); }
      }
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
