// QE-2-10 ② 재시도 — 네트워크 응답 캡처 포함, A 7/12 OT 종료 16:00 수정 저장.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    page.on("response", async (r) => {
      if (r.url().includes("webApi") && r.request().method() === "POST" && /overtime|attd07|ot/i.test(r.url())) {
        let body = "";
        try { body = (await r.text()).slice(0, 300); } catch {}
        console.log("RES:", r.status(), r.url().split("/prafta")[1] || r.url(), body);
      }
    });
    await page.goto("http://localhost:8081/safenote/main/Attd_07", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    await page.locator('button:has-text("조회")').first().click();
    await page.waitForTimeout(3000);
    const cellInfo = await page.evaluate(() => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes("QT사원에이"));
      const td = [...row.querySelectorAll("td.m-day-cell")][11];
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
    });
    await page.mouse.dblclick(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(2500);
    // 재조회된 OT 값 확인(이전 '3시간' 표시가 서버 반영인지 판별)
    const ot = page.locator("input.ot-time:visible");
    console.log("재오픈 OT 값:", await ot.nth(0).inputValue(), "~", await ot.nth(1).inputValue());
    // 체크 + 수정
    await page.evaluate(() => {
      const otInput = document.querySelector("input.ot-time");
      let el = otInput;
      for (let k = 0; k < 6 && el; k++) {
        el = el.parentElement;
        const cb = el?.querySelector('input[type="checkbox"]');
        if (cb && !cb.checked) { cb.click(); break; }
        if (cb) break;
      }
    });
    await page.waitForTimeout(500);
    const endInput = ot.nth(1);
    await endInput.click({ clickCount: 3 });
    await endInput.press("Control+a");
    await endInput.pressSequentially("1600", { delay: 60 });
    await endInput.press("Tab");
    await page.waitForTimeout(500);
    const saveBtn = page.locator('button:has-text("초과근무 저장")');
    console.log("저장 disabled=", await saveBtn.isDisabled());
    if (!(await saveBtn.isDisabled())) {
      await saveBtn.click();
      await page.waitForTimeout(1200);
      // 확인 모달 처리 후 에러 표시 캡처
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); }
      await page.waitForTimeout(2500);
      const text = await page.evaluate(() => document.body.innerText);
      console.log("=== 저장 직후 화면 꼬리 ===");
      console.log(text.split("\n").slice(-14).join(" | "));
      await page.screenshot({ path: shotPath("QE-2-10", "web", "edit2-response"), fullPage: true });
      // 남은 팝업 정리
      const ok2 = page.locator("button:has-text('확인')").first();
      if (await ok2.count()) { await ok2.click().catch(() => {}); }
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
