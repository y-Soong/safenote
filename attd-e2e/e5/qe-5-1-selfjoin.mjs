// QE-5-1: 일용직 셀프가입 UI 전체 여정 (웹 self-join 페이지 /safenote/dailyUserJoin/001-00001)
// 주: 일용직 셀프가입 UI 는 앱이 아니라 웹(비로그인 외부 링크 페이지)에 존재.
//     SMS 인증코드는 헤드리스에서 DB 조회 불가 → 동일 백엔드 EP(sms-auth-sends) 를 외부에서 선발송하고
//     코드(SMS_CODE env)를 주입해 certNo 확인만 UI 로 수행(인증요청 클릭은 생략, 동일 EP 이므로 충실).
import { chromium } from "@playwright/test";
import { record, shotPath } from "../lib/record.mjs";

const WEB = "http://localhost:8081";
const JOINCD = "001-00001";
const CODE = process.env.SMS_CODE || "";
const USERID = "QTDAILY2";
const PHONE = "010-7777-0031";
const NAME = "QE5-1일용2"; // maxlength 15
const PW = "QtTest!2026";

async function dismissAlert(page) {
  // 웹 $alert 모달: .modal-overlay .modal-footer button.btn-primary("확인")
  try {
    const btn = page.locator('.modal-overlay button.btn-primary:has-text("확인")');
    await btn.waitFor({ state: "visible", timeout: 3000 });
    await btn.click();
    await page.waitForTimeout(300);
    return true;
  } catch { return false; }
}

const run = async () => {
  const browser = await chromium.launch({ headless: true, args: ["--disable-dev-shm-usage"] });
  const snap = async (page, id, face, label) => {
    try { await page.screenshot({ path: shotPath(id, face, label), animations: "disabled", caret: "hide" }); } catch { /* 크래시 무시 */ }
  };
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, ignoreHTTPSErrors: true });
  const page = await ctx.newPage();
  const errors = [];
  page.on("pageerror", (e) => errors.push(String(e)));
  page.on("console", (m) => { if (m.type() === "error") errors.push("console:" + m.text()); });

  const out = { title: "일용직 셀프가입 UI 전체 여정(웹 self-join)", steps: [] };
  try {
    await page.goto(`${WEB}/safenote/dailyUserJoin/${JOINCD}`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1500);
    // 회사/사업장 resolve 확인
    const cmpnyVal = await page.locator('.daily-join__input[placeholder="회사명"]').inputValue().catch(() => "");
    const siteVal = await page.locator('.daily-join__input[placeholder="사업장명"]').inputValue().catch(() => "");
    out.steps.push(`page loaded cmpnyNm='${cmpnyVal}' siteNm='${siteVal}'`);
    await snap(page, "QE-5-1", "web", "form-loaded");

    if (!cmpnyVal) {
      record("QE-5-1", "BLOCKED", { ...out, note: "self-join 페이지 회사정보 미해결(링크무효?)", errors });
      await browser.close(); return;
    }

    // 아이디 + 중복확인
    await page.locator('.daily-join__input[placeholder="4 ~ 10자"]').fill(USERID);
    await page.locator('button:has-text("중복확인")').click();
    await page.waitForTimeout(800);
    await dismissAlert(page); // 성공 시 모달 없음, 실패 시 dismiss
    const idMsg = await page.locator('.daily-join__msg').first().innerText().catch(() => "");
    out.steps.push(`userId dup-check msg='${idMsg}'`);

    // 비번/확인/이름/폰
    await page.locator('input[type="password"][placeholder="6 ~ 15자, 영문/숫자/특수문자 중 2종 이상"]').fill(PW);
    await page.locator('input[type="password"][placeholder="비밀번호를 한 번 더 입력하세요"]').fill(PW);
    await page.locator('.daily-join__input[placeholder="최대 15자"]').fill(NAME);
    await page.locator('.daily-join__input[placeholder="휴대폰 번호"]').fill(PHONE);
    out.steps.push("filled pw/name/phone");

    // certNo (외부 선발송 코드 주입) → 확인
    if (!CODE) {
      record("QE-5-1", "BLOCKED", { ...out, note: "SMS_CODE env 미주입", errors });
      await snap(page, "QE-5-1", "web", "nocode");
      await browser.close(); return;
    }
    await page.locator('.daily-join__input[placeholder="인증번호 6자리"]').fill(CODE);
    await page.locator('.daily-join__field button:text-is("확인")').click();
    await page.waitForTimeout(800);
    await dismissAlert(page); // "인증번호가 확인되었습니다." 모달
    const smsMsg = await page.locator('.daily-join__msg').nth(3).innerText().catch(() => "");
    out.steps.push(`sms verify msg='${smsMsg}'`);
    // (sms-verified 스냅 제거 — 제출 후 done 스냅만)

    // 약관 전체동의
    await page.locator('.daily-join__terms-all input[type="checkbox"]').check();
    await page.waitForTimeout(300);

    // 회원가입 제출
    await page.locator('button.daily-join__submit:has-text("회원가입")').click();
    await page.waitForTimeout(1500);
    await dismissAlert(page); // 실패 시 alert
    const done = await page.locator('.daily-join__done-mark').innerText().catch(() => "");
    const doneDesc = await page.locator('.daily-join__done-desc').innerText().catch(() => "");
    const joinedId = await page.locator('.daily-join__done-id strong').innerText().catch(() => "");
    out.steps.push(`done mark='${done}' joinedId='${joinedId}' desc='${doneDesc.slice(0,60)}'`);
    await snap(page, "QE-5-1", "web", "done");

    const ok = done.includes("가입 완료") && joinedId === USERID;
    out.appView = `가입완료 화면='${done}' id=${joinedId} 승인대기안내='${doneDesc.includes("관리자 승인")}'`;
    record("QE-5-1", ok ? "OBSERVED" : "DEFECT", {
      ...out,
      note: ok
        ? "웹 self-join UI 여정 성공. 단 (1)셀프가입 UI 는 앱이 아닌 웹 링크페이지, (2)가입=승인대기('04')+입장요청 생성, 슬롯 자동점유 아님(D6 승인제) — 지시서 '슬롯 SLOT_STATUS=02 자동점유' 전제와 상충(설계 진화)."
        : "가입완료 화면 미도달",
      errors,
    });
  } catch (e) {
    out.steps.push("EX:" + String(e).slice(0, 200));
    await snap(page, "QE-5-1", "web", "error");
    record("QE-5-1", "BLOCKED", { ...out, note: "예외: " + String(e).slice(0, 150), errors });
  }
  await browser.close();
};
run();
