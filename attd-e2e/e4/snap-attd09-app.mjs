// 공용 스냅샷 — 웹 Attd_09(QT 계정 행) + 앱 연차현황(지정 계정) 채집. node e4/snap-attd09-app.mjs <caseId> <라벨> [앱계정]
import { webLogin, appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , caseId = "QE-4-5", label = "snap", appUser = "QTUSERG"] = process.argv;

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_09", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2500);
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(3000);
    const rows = await page.evaluate(() =>
      [...document.querySelectorAll("tbody tr")]
        .map((r) => r.innerText.replace(/\n/g, " | "))
        .filter((t) => t.includes("QT"))
    );
    console.log(`=== Attd_09 (${label}) ===`);
    for (const r of rows) console.log(r);
    await page.screenshot({ path: shotPath(caseId, "web", `attd09-${label}`), fullPage: true });

    if (appUser !== "none") {
      const { page: app } = await appLogin(appUser, "QtTest!2026");
      await app.click('button.app-tabbar__tab:has-text("마이")');
      await app.waitForTimeout(2500);
      await app.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 15000 }).catch(() => {});
      await app.locator('[aria-label="연차 현황 보기"]').first().click();
      await app.waitForTimeout(3000);
      const t = await app.evaluate(() => document.body.innerText);
      console.log(`=== 앱 ${appUser} 연차현황 (${label}) ===`);
      console.log(t.slice(0, 1500).replace(/\n/g, " | "));
      await app.screenshot({ path: shotPath(caseId, "app", `leave-summary-${label}`), fullPage: true });
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
