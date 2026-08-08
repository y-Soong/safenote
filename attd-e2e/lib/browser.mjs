// 브라우저 헬퍼 — 웹 관리자(8081)·앱(8082) 로그인 및 컨텍스트 관리.
// 케이스 실행은 순차이므로 브라우저 1개를 재사용하고, 계정/면(웹·앱)별 컨텍스트를 캐시한다.
import { chromium } from "@playwright/test";

const WEB = "http://localhost:8081";
const APP = "https://localhost:8082";
// QT통합테스트사업장(00010) 지오펜스 좌표는 세션에서 필요 시 setGeolocation 으로 갱신.
const DEFAULT_GEO = { latitude: 37.5665, longitude: 126.978 };

let browser = null;
const ctxCache = new Map(); // key: `${face}:${userId}` → { ctx, page }

export async function getBrowser() {
  if (!browser) browser = await chromium.launch({ headless: true });
  return browser;
}

export async function closeAll() {
  for (const { ctx } of ctxCache.values()) await ctx.close().catch(() => {});
  ctxCache.clear();
  if (browser) { await browser.close().catch(() => {}); browser = null; }
}

// 웹 관리자 로그인 — /safenote 로그인 화면 경유(#userId / password / button.login-btn).
// 로그인 후 게이트 팝업(약관 재동의 등) 처리 — 제3자 제공동의 신설(07-28)로 전 계정 재동의 발생.
// 게이트가 없으면 조용히 통과한다(timeout 짧게).
async function passLoginGates(page) {
  // 웹 = TermsPop 팝업("약관동의" + Confirm "확인") / 앱 = #/TermsAgree 전용 페이지("동의하고 시작")
  try {
    const agreeAll = page.locator("text=전체 동의하기").first();
    await agreeAll.waitFor({ state: "visible", timeout: 4000 });
    await agreeAll.click();
    await page.waitForTimeout(400);
    const submit = page.locator('button:has-text("약관동의"), button:has-text("동의하고 시작")').first();
    await submit.click();
    // 웹 경로는 Confirm 모달("저장하시겠습니까?")이 한 번 더 뜬다 — 앱 경로엔 없으므로 실패해도 무시
    await page.locator('button:has-text("확인")').first().click({ timeout: 4000 }).catch(() => {});
    await page.waitForTimeout(1500);
  } catch {}
}

export async function webLogin(userId, userPw) {
  const key = `web:${userId}`;
  if (ctxCache.has(key)) return ctxCache.get(key);
  const b = await getBrowser();
  const ctx = await b.newContext({ viewport: { width: 1600, height: 950 } });
  const page = await ctx.newPage();
  await page.goto(`${WEB}/safenote`, { waitUntil: "networkidle", timeout: 20000 });
  await page.fill("#userId", userId);
  await page.fill('input[type="password"]', userPw);
  await page.click("button.login-btn");
  await passLoginGates(page);
  await page.waitForURL(/\/safenote\/main/, { timeout: 20000 });
  const entry = { ctx, page, face: "web", userId };
  ctxCache.set(key, entry);
  return entry;
}

// 앱 로그인 — https 자가서명 무시 + geolocation 주입 + 모바일 뷰포트.
// userType: REGULAR(기본) | DAILY — DAILY 는 로그인 화면에서 탭 전환.
export async function appLogin(userId, userPw, { userType = "REGULAR", geo = DEFAULT_GEO } = {}) {
  const key = `app:${userId}`;
  if (ctxCache.has(key)) return ctxCache.get(key);
  const b = await getBrowser();
  const ctx = await b.newContext({
    ignoreHTTPSErrors: true,
    geolocation: geo,
    permissions: ["geolocation"],
    viewport: { width: 390, height: 844 },
  });
  const page = await ctx.newPage();
  await page.goto(`${APP}/`, { waitUntil: "networkidle", timeout: 20000 });
  if (userType === "DAILY") {
    await page.click('button.user-type-btn:has-text("일용직")');
  }
  await page.fill('input[placeholder="아이디를 입력하세요"]', userId);
  await page.fill('input[placeholder="비밀번호를 입력하세요"]', userPw);
  await page.click("button.btn-login");
  await passLoginGates(page);
  await page.waitForURL(/MainView|AdminHome/, { timeout: 20000 });
  await page.waitForLoadState("networkidle");
  const entry = { ctx, page, face: "app", userId };
  ctxCache.set(key, entry);
  return entry;
}

// 컨텍스트 강제 폐기(로그아웃/권한 변경 후 재로그인용).
export async function evictSession(face, userId) {
  const key = `${face}:${userId}`;
  const e = ctxCache.get(key);
  if (e) { await e.ctx.close().catch(() => {}); ctxCache.delete(key); }
}
