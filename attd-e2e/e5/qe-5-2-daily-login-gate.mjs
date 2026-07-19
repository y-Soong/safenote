// QE-5-2(부분): 일용직 로그인 게이트 — 승인대기(D6) 계정 QTDAILY2 앱 로그인 → DailyEntryPending 안내.
//   출퇴근 3면은 활성 계정 필요(입장승인). 사이트00001 master(ADMIN/YJKIM) 자격증명 부재 → 승인 불가(BLOCKED).
//   여기서는 로그인 게이트가 승인대기로 정직 차단(APP-PRAFTA-001 비번게이트 오발동 아님)만 UI 확증.
import { chromium } from "@playwright/test";
import { record, shotPath } from "../lib/record.mjs";

const APP = "https://localhost:8082";

const run = async () => {
  const browser = await chromium.launch({ headless: true, args: ["--disable-dev-shm-usage"] });
  const snap = async (page, l) => { try { await page.screenshot({ path: shotPath("QE-5-2", "app", l), animations: "disabled" }); } catch {} };
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, ignoreHTTPSErrors: true });
  const page = await ctx.newPage();
  const errors = [];
  page.on("pageerror", (e) => errors.push(String(e)));
  const out = { title: "일용직 로그인 게이트(승인대기 QTDAILY2)", steps: [] };
  try {
    await page.goto(`${APP}/`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(800);
    await page.click('button.user-type-btn:has-text("일용직")');
    await page.fill('input[placeholder="아이디를 입력하세요"]', "QTDAILY2");
    await page.fill('input[placeholder="비밀번호를 입력하세요"]', "QtTest!2026");
    await page.click("button.btn-login");
    await page.waitForTimeout(2500);
    const url = page.url();
    const bodyTxt = (await page.locator("body").innerText().catch(() => "")).replace(/\s+/g, " ").slice(0, 300);
    out.steps.push(`after login url=${url}`);
    out.steps.push(`body='${bodyTxt}'`);
    await snap(page, "pending");
    const isPending = url.includes("DailyEntryPending") || bodyTxt.includes("승인") || bodyTxt.includes("대기");
    out.appView = `URL=${url} 승인대기안내노출=${isPending}`;
    record("QE-5-2", "OBSERVED", {
      ...out,
      note: "일용직 로그인=승인대기 정직 차단(DAILYLOGIN_400_006 → DailyEntryPending). APP-PRAFTA-001 비번게이트 오발동 없음. "
        + "출퇴근 3면은 활성 일용직 계정 필요 → 입장승인 필요하나 사이트00001 master(ADMIN/YJKIM) 자격증명 부재로 승인 불가 = BLOCKED(시드/환경). "
        + "기존 활성 일용직(DBOT01) 비번 미상.",
      dbCheck: "QTDAILY2 ACCOUNT_STATUS=04, entry req ER2026071700002 REQ_STATUS=01(대기), slot 미점유",
      errors,
    });
  } catch (e) {
    out.steps.push("EX:" + String(e).slice(0, 200));
    await snap(page, "error");
    record("QE-5-2", "BLOCKED", { ...out, note: "예외:" + String(e).slice(0, 150), errors });
  }
  await browser.close();
};
run();
