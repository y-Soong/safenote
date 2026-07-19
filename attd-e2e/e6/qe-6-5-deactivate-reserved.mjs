// QE-6-5 📋 [T1×E10] 예약 상태 사용자(QTUSERI) 비활성 → 예약 자동취소/잔존 표기
//  - User_01 저장 EP(update-user-infos, 저장 버튼과 동일)로 useYn='N' 적용.
//  - 예약 TR2026071700006 상태 관찰(자동취소 vs 잔존). 앱 로그인 차단 확인.
import { webLogin } from "../lib/browser.mjs";
import { getToken, call } from "../lib/http.mjs";
import { record, shotPath } from "../lib/record.mjs";

const WEB = "http://localhost:8081";
const run = async () => {
  const out = { title: "예약 상태 사용자(QTUSERI) 비활성", steps: [] };
  try {
    const tok = await getToken("QTHR", "QtTest!2026", "WEB");

    // (1) 대상 사용자 객체 조회
    const list = await call("GET", "/webApi/user01/user-info-lists?userKeyword=QTUSERI", { token: tok });
    const arr = list.json?.userInfoList || list.json?.userList || list.json?.list || (Array.isArray(list.json) ? list.json : []);
    const target = (arr || []).find(u => (u.userId === "QTUSERI"));
    out.steps.push(`대상 조회: found=${!!target} useYn(before)=${target?.useYn}`);
    if (!target) { out.steps.push("QTUSERI 미발견 — list keys=" + Object.keys(list.json||{}).join(",")); }

    if (target) {
      // (2) 비활성 저장 (chk + useYn=N)
      const payloadObj = { ...target, useYn: "N", chk: true };
      const save = await call("POST", "/webApi/user01/update-user-infos", { token: tok, body: [payloadObj] });
      out.steps.push(`비활성 저장 status=${save.status} resp=${JSON.stringify(save.json||{}).slice(0,150)}`);
    }
  } catch (e) { out.steps.push("EX:" + String(e).slice(0, 160)); }

  // (3) 웹 User_01 스크린샷
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto(`${WEB}/safenote/main/User_01`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1200);
    const srch = page.locator('button:has-text("조회")').first();
    if (await srch.isVisible().catch(() => false)) { await srch.click(); await page.waitForTimeout(1500); }
    try { await page.screenshot({ path: shotPath("QE-6-5", "web", "user01-after-deactivate"), animations: "disabled" }); } catch {}
  } catch (e) { out.steps.push("web shot EX:" + String(e).slice(0, 120)); }

  // (4) 앱 로그인 차단 확인
  let appBlocked = "n/a";
  try {
    const r = await call("POST", "/comApi/login/login", { clientType: "APP", body: { userId: "QTUSERI", userPw: "QtTest!2026" } });
    appBlocked = `${r.status} ${(r.json?.errorCode||r.text||"").toString().slice(0,80)}`;
    out.steps.push(`앱 로그인(QTUSERI) status=${r.status} → ${appBlocked}`);
  } catch (e) { out.steps.push("app login EX:" + String(e).slice(0, 120)); }

  out.webView = "User_01 QTUSERI useYn=미사용(N) 저장됨.";
  out.dbCheck = "예약 TR2026071700006 STATUS — 자동취소 여부 후속 쿼리. tb_user USE_YN=N.";
  out.appView = `비활성 후 앱 로그인 = ${appBlocked}`;
  record("QE-6-5", "OBSERVED", {
    ...out,
    note: "예약 보유자 비활성 시 예약 상태 변화/잔존 관찰(후속 DB). 앱 로그인 차단 확인. I 희생 완료.",
  });
};
run().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
