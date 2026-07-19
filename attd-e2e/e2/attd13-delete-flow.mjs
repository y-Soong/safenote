// Attd_13 삭제 흐름 공용 — node e2/attd13-delete-flow.mjs <USERID> <day> <caseId> <사유>
// 1) Attd_05 셀 더블클릭→DELETE 발의 2) 대상자 앱 동의 3) QTHR confirm.
import { webLogin, appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { waitLoaded, bodyText, clickPopupOk } from "./lib-leave.mjs";

const [, , userId, dayStr, caseId, reason] = process.argv;
const day = Number(dayStr);

const main = async () => {
  try {
    // 1) 발의
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const cellInfo = await page.evaluate(({ uid, dd }) => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes(uid));
      const tds = [...row.querySelectorAll("td")];
      const td = tds.slice(-31)[dd - 1];
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: td.innerText.trim() };
    }, { uid: userId, dd: day });
    console.log("셀:", JSON.stringify(cellInfo));
    await page.mouse.dblclick(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(2000);
    const radio = page.locator('input[type="radio"][value="DELETE"]');
    if ((await radio.count()) === 0) { console.log("발의 팝업 미노출 — 진입 불가"); process.exit(2); }
    await radio.check();
    await page.fill("textarea:visible", reason);
    await page.locator('button:has-text("요청")').last().click();
    await page.waitForTimeout(2000);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("발의 응답:", text.split("\n").slice(-6).join(" | "));
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1200); }
    }

    // 2) 대상자 동의
    const { page: ap } = await appLogin(userId, "QtTest!2026");
    await waitLoaded(ap);
    await ap.waitForTimeout(2000);
    await ap.locator('button:has-text("동의")').last().click();
    await ap.waitForTimeout(2000);
    text = await bodyText(ap);
    console.log("동의 응답:", text.split("\n").slice(-6).join(" | "));
    await clickPopupOk(ap, 2);

    // 3) confirm
    await page.goto("http://localhost:8081/safenote/main/Attd_13", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const row = page.locator("tr", { hasText: "동의(확인대기)" }).first();
    console.log("행:", (await row.innerText()).replace(/\n/g, " | "));
    await row.locator('button:has-text("확인")').click();
    await page.waitForTimeout(2000);
    await page.locator('button:has-text("최종 확인(반영)")').click();
    await page.waitForTimeout(2500);
    text = await page.evaluate(() => document.body.innerText);
    console.log("confirm 응답:", text.split("\n").slice(-6).join(" | "));
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1200); }
    }
    await page.screenshot({ path: shotPath(caseId, "web", "attd13-cleanup-done"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
