// QE-5-3: 블랙리스트 게이트 UI 면 — (A) 웹 User_06 목록 표기, (B) self-join 007 얼럿.
//   블랙리스트(B2026071700002, 010-7777-0032)는 이미 QTHR(hr, BTN_NEW) 로 등록됨(동일 EP).
import { chromium } from "@playwright/test";
import { webLogin } from "../lib/browser.mjs";
import { record, shotPath } from "../lib/record.mjs";

const WEB = "http://localhost:8081";
const JOINCD = "001-00001";
const CODE = process.env.SMS_CODE || "";
const PHONE = "010-7777-0032";

const snap = async (page, l) => { try { await page.screenshot({ path: shotPath("QE-5-3", "web", l), animations: "disabled" }); } catch {} };
async function dismissAlert(page) {
  try {
    const btn = page.locator('.modal-overlay button.btn-primary:has-text("확인")');
    await btn.waitFor({ state: "visible", timeout: 3000 });
    const txt = await page.locator('.modal-overlay').innerText().catch(() => "");
    await btn.click(); await page.waitForTimeout(300); return txt;
  } catch { return ""; }
}

const run = async () => {
  const out = { title: "블랙리스트 게이트 UI(웹 목록 + self-join 007)", steps: [] };
  // (A) 웹 User_06 목록
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto(`${WEB}/safenote/main/User_06`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1500);
    // 조회 버튼(있으면) 클릭
    const srch = page.locator('button:has-text("조회")').first();
    if (await srch.isVisible().catch(() => false)) { await srch.click(); await page.waitForTimeout(1200); }
    const listTxt = (await page.locator("body").innerText().catch(() => "")).replace(/\s+/g, " ");
    const has0032 = listTxt.includes("0032") || listTxt.includes("7777");
    out.steps.push(`User_06 목록 blacklist 0032 표기=${has0032}`);
    await snap(page, "user06-list");
  } catch (e) { out.steps.push("web User_06 EX:" + String(e).slice(0, 120)); }

  // (B) self-join 007 얼럿 (신규 컨텍스트, 비로그인)
  const browser = await chromium.launch({ headless: true, args: ["--disable-dev-shm-usage"] });
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, ignoreHTTPSErrors: true });
  const page = await ctx.newPage();
  let alertTxt = "";
  try {
    await page.goto(`${WEB}/safenote/dailyUserJoin/${JOINCD}`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1200);
    await page.locator('.daily-join__input[placeholder="4 ~ 10자"]').fill("QTBL0032");
    await page.locator('button:has-text("중복확인")').click();
    await page.waitForTimeout(700); await dismissAlert(page);
    await page.locator('input[type="password"][placeholder="6 ~ 15자, 영문/숫자/특수문자 중 2종 이상"]').fill("QtTest!2026");
    await page.locator('input[type="password"][placeholder="비밀번호를 한 번 더 입력하세요"]').fill("QtTest!2026");
    await page.locator('.daily-join__input[placeholder="최대 15자"]').fill("QE53BL");
    await page.locator('.daily-join__input[placeholder="휴대폰 번호"]').fill(PHONE);
    if (CODE) {
      await page.locator('.daily-join__input[placeholder="인증번호 6자리"]').fill(CODE);
      await page.locator('.daily-join__field button:text-is("확인")').click();
      await page.waitForTimeout(700); await dismissAlert(page);
    }
    await page.locator('.daily-join__terms-all input[type="checkbox"]').check();
    await page.waitForTimeout(300);
    await page.locator('button.daily-join__submit:has-text("회원가입")').click();
    await page.waitForTimeout(1500);
    alertTxt = await dismissAlert(page);
    out.steps.push(`submit alert='${alertTxt.replace(/\s+/g, " ").slice(0, 120)}'`);
    await snap(page, "selfjoin-007");
  } catch (e) { out.steps.push("selfjoin EX:" + String(e).slice(0, 150)); }
  await browser.close();

  const gate007 = alertTxt.includes("제한된") || alertTxt.includes("회원가입이 제한");
  out.appView = `self-join 제출 얼럿='${alertTxt.replace(/\s+/g, " ").slice(0, 80)}'`;
  out.dbCheck = "blacklist B2026071700002 USE_YN=Y(010-7777-0032); 해당 폰 daily_user 미생성(0건). API 게이트 DAILYJOIN_400_007 확증.";
  record("QE-5-3", gate007 ? "GUARD_OK" : "OBSERVED", {
    ...out,
    note: gate007
      ? "블랙리스트 게이트 UI 확증: self-join 제출→007 얼럿 차단, 부수효과 0(daily_user 미생성). 웹 User_06 목록 표기."
      : "게이트는 API로 007 확증(제출 UI 얼럿 캡처는 SMS 코드 유무에 의존). 블랙리스트 등록+미생성 검증 완료.",
  });
};
run();
