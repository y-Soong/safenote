// 앱 내근태 이번달 탭에서 다음달로 이동 후 특정일 관찰 — node e3/app-observe-month.mjs <USERID> <day> <caseId> <label> [nextCount=1]
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , userId, dayStr, caseId, label, nextCountStr = "1"] = process.argv;
const day = Number(dayStr);
const nextCount = Number(nextCountStr);
const waitLoaded = (p) => p.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 }).catch(() => {});

const main = async () => {
  try {
    const { page } = await appLogin(userId, "QtTest!2026");
    await waitLoaded(page);
    const later = page.locator('button:has-text("나중에")');
    if (await later.count()) { await later.click().catch(() => {}); await page.waitForTimeout(800); }
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2500);
    await waitLoaded(page);
    await page.click('.attd-seg__item:has-text("이번달")');
    await page.waitForTimeout(2500);
    await waitLoaded(page);
    // 월 내비 버튼 실측
    const navBtns = await page.evaluate(() =>
      [...document.querySelectorAll("button")].filter((b) => b.offsetParent).map((b) => ({ t: b.innerText.trim(), aria: b.getAttribute("aria-label") ?? "", cls: b.className.slice(0, 40) })).filter((b) => b.t.length <= 2 || /다음|이전|next|prev/.test(b.aria))
    );
    console.log("월 내비 후보:", JSON.stringify(navBtns));
    for (let i = 0; i < nextCount; i++) {
      const clicked = await page.evaluate(() => {
        const cands = [...document.querySelectorAll("button")].filter((b) => b.offsetParent);
        const byAria = cands.find((b) => /다음/.test(b.getAttribute("aria-label") ?? ""));
        const bySym = cands.find((b) => [">", "›", "▶", "❯"].includes(b.innerText.trim()));
        const t = byAria ?? bySym;
        if (t) { t.click(); return t.getAttribute("aria-label") ?? t.innerText.trim(); }
        return null;
      });
      console.log("다음달 클릭:", clicked);
      if (!clicked) break;
      await page.waitForTimeout(2500);
      await waitLoaded(page);
    }
    const head = await page.evaluate(() => document.body.innerText.match(/\d{4}년 \d{1,2}월/)?.[0]);
    console.log("표시 월:", head);
    await page.evaluate((dd) => {
      const cells = [...document.querySelectorAll("button, td")].filter((el) => (el.innerText ?? "").trim().split("\n")[0] === String(dd));
      if (cells[0]) cells[0].click();
    }, day);
    await page.waitForTimeout(2000);
    const body = await page.evaluate(() => document.body.innerText);
    console.log("=== 일 선택 후(끝 500자) ===");
    console.log(body.slice(-500).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath(caseId, "app", `${label}-cal${day}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
