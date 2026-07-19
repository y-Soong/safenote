// QE-4-4 step4 — 비활성 후 잔존 표기 관찰: 앱(H) 홈 + 내근태 7/16 타입 라벨, 웹 Attd_08 H 7/16 행
import { webLogin, appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    // ── 앱 H ──
    const { page: app } = await appLogin("QTUSERH", "QtTest!2026");
    await app.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 }).catch(() => {});
    await app.waitForTimeout(2000);
    const home = await app.evaluate(() => document.body.innerText);
    console.log("=== 앱 H 홈 ===");
    console.log(home.slice(0, 800).replace(/\n/g, " | "));
    await app.screenshot({ path: shotPath("QE-4-4", "app", "h-home-after-deact"), fullPage: true });
    // 내근태 이번달 7/16
    await app.click('button.app-tabbar__tab:has-text("근태")');
    await app.waitForTimeout(2000);
    await app.click('.attd-seg__item:has-text("이번달")');
    await app.waitForTimeout(2500);
    await app.evaluate(() => {
      const cells = [...document.querySelectorAll("button, td")];
      const el = cells.find((c) => c.innerText && c.innerText.trim().split("\n")[0] === "16" && c.className.includes("cal"));
      if (el) el.click();
    });
    await app.waitForTimeout(2000);
    const day = await app.evaluate(() => document.body.innerText);
    console.log("=== 앱 H 7/16 상세(비활성 후) ===");
    console.log(day.slice(-600).replace(/\n/g, " | "));
    await app.screenshot({ path: shotPath("QE-4-4", "app", "h-0716-after-deact"), fullPage: true });

    // ── 웹 Attd_08 ──
    const { page: web } = await webLogin("QTHR", "QtTest!2026");
    await web.goto("http://localhost:8081/safenote/main/Attd_08", { waitUntil: "networkidle", timeout: 30000 });
    await web.waitForTimeout(2500);
    await web.click('button:has-text("조회")');
    await web.waitForTimeout(3000);
    const rows = await web.evaluate(() => {
      return [...document.querySelectorAll("tbody tr")]
        .map((r) => r.innerText.replace(/\n/g, " | "))
        .filter((t) => t.includes("QT사원H"));
    });
    console.log("=== Attd_08 H 행 ===");
    for (const r of rows) console.log(r);
    await web.screenshot({ path: shotPath("QE-4-4", "web", "attd08-h"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
