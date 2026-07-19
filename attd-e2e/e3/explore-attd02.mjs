// E3 — 휴일관리(Attd_02) 화면 실측: 라우트/등록·삭제 UI 셀렉터 채집.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_02", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== 화면 텍스트(앞 2500자) ===");
    console.log(text.slice(0, 2500));
    // 버튼 전수
    const btns = await page.evaluate(() =>
      [...document.querySelectorAll("button")].map((b) => b.innerText.trim()).filter(Boolean).slice(0, 60)
    );
    console.log("=== 버튼 ===", JSON.stringify(btns));
    // 입력 필드
    const inputs = await page.evaluate(() =>
      [...document.querySelectorAll("input, select")].map((i) => ({
        tag: i.tagName, type: i.type ?? "", ph: i.placeholder ?? "", cls: i.className.slice(0, 60), val: (i.value ?? "").slice(0, 20),
      })).slice(0, 30)
    );
    console.log("=== 입력 ===", JSON.stringify(inputs, null, 1));
    // 테이블 헤더/행 표본
    const rows = await page.evaluate(() =>
      [...document.querySelectorAll("tr")].slice(0, 12).map((r) => r.innerText.replace(/\n/g, " | ").slice(0, 160))
    );
    console.log("=== 행 표본 ===");
    rows.forEach((r) => console.log(r));
    await page.screenshot({ path: shotPath("E3-explore", "web", "attd02"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
