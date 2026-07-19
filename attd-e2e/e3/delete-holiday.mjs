// 범용 휴일 삭제(목록 탭 행 삭제) — node e3/delete-holiday.mjs <y> <m> <name부분문자열> <caseId>
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openAttd02, gotoMonth, bodyText } from "./lib-holiday.mjs";

const [, , y, m, name, caseId] = process.argv;

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    const dialogs = [];
    page.on("dialog", async (d) => { dialogs.push(d.message()); await d.accept(); });
    await openAttd02(page);
    await gotoMonth(page, Number(y), Number(m));
    await page.click('button:has-text("목록")');
    await page.waitForTimeout(1200);
    // 목록에서 이름 포함 최소 단위 요소의 가장 가까운 삭제 버튼
    const clicked = await page.evaluate((nm) => {
      // 삭제 버튼들 중, 같은 행(li/tr/div)에 nm 텍스트가 있는 것
      const dels = [...document.querySelectorAll("button")].filter((b) => b.innerText.trim() === "삭제" && b.offsetParent);
      for (const b of dels) {
        let el = b;
        for (let i = 0; i < 6; i++) {
          el = el.parentElement;
          if (!el) break;
          if (el.innerText.includes(nm)) {
            // 다른 휴일까지 포함하는 큰 컨테이너 오클릭 방지: 텍스트 내 '삭제' 출현이 1회인 컨테이너만
            if ((el.innerText.match(/삭제/g) || []).length === 1) { b.click(); return el.innerText.replace(/\n/g, " | ").slice(0, 150); }
          }
        }
      }
      return null;
    }, name);
    console.log("삭제 클릭 대상 행:", clicked ?? "미발견");
    if (!clicked) { console.log("삭제 버튼 미발견 — 실패"); process.exitCode = 2; return; }
    await page.waitForTimeout(1500);
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1200); }
    }
    console.log("dialog:", JSON.stringify(dialogs));
    const t = await bodyText(page);
    console.log("삭제 후 목록 잔존 여부:", t.includes(name) ? "잔존!" : "소멸 확인");
    await page.screenshot({ path: shotPath(caseId, "web", `attd02-deleted-${m}-${name.replace(/[^\w가-힣-]/g, "").slice(0, 12)}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
