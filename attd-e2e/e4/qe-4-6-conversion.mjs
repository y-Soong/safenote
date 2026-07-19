// QE-4-6 — 환산시간 변경/원복 — node e4/qe-4-6-conversion.mjs <minutes> <applyYmdDash> <label>
// 예: node e4/qe-4-6-conversion.mjs 480 2026-08-01 set480  /  400 2026-08-01 restore400
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , minutes, applyDash, label] = process.argv;

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Baim_07", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3500);
    // 현재 적용값 + 이력 채집
    const before = await page.evaluate(() => {
      const cur = document.querySelector(".lp-conv-current")?.innerText.replace(/\n/g, " ") || "(없음)";
      const hist = [...document.querySelectorAll(".lp-conv-table tbody tr")].map((r) => r.innerText.replace(/\n/g, " | "));
      return { cur, hist };
    });
    console.log("[before] 현재값:", before.cur);
    console.log("[before] 이력:", JSON.stringify(before.hist));

    // 새 환산시간 입력
    await page.evaluate((v) => {
      const inp = document.querySelector("input.lp-conv-num");
      inp.value = v;
      inp.dispatchEvent(new Event("input", { bubbles: true }));
    }, minutes);
    await page.waitForTimeout(400);
    // 적용일(flatpickr) — .lp-conv 내부 calendar-input
    await page.evaluate((v) => {
      const inp = document.querySelector(".lp-conv input.calendar-input");
      if (inp?._flatpickr) inp._flatpickr.setDate(v, true);
    }, applyDash);
    await page.waitForTimeout(600);
    // 저장 → $alert
    const dialogs = [];
    page.on("dialog", async (d) => { dialogs.push(d.message()); await d.accept().catch(() => {}); });
    await page.locator(".lp-conv-input-row button:has-text('저장')").click();
    await page.waitForTimeout(2500);
    // $alert 는 커스텀 모달일 수 있음 — 모달 텍스트 채집
    const modalTxt = await page.evaluate(() => {
      const o = [...document.querySelectorAll(".modal-overlay, .alert-modal, [class*=modal]")].at(-1);
      return o ? o.innerText.trim().replace(/\n/g, " | ") : "(모달 없음)";
    });
    console.log("[save] dialogs:", JSON.stringify(dialogs), "modal:", modalTxt);
    // 확인 닫기
    for (let i = 0; i < 2; i++) {
      const ok = page.locator('button:has-text("확인")').last();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(800); }
    }
    await page.waitForTimeout(1500);
    const after = await page.evaluate(() => {
      const cur = document.querySelector(".lp-conv-current")?.innerText.replace(/\n/g, " ") || "(없음)";
      const hist = [...document.querySelectorAll(".lp-conv-table tbody tr")].map((r) => r.innerText.replace(/\n/g, " | "));
      return { cur, hist };
    });
    console.log("[after] 현재값:", after.cur);
    console.log("[after] 이력:", JSON.stringify(after.hist));
    await page.screenshot({ path: shotPath("QE-4-6", "web", `conv-${label}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
