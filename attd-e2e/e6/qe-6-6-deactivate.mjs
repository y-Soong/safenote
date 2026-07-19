// QE-6-6/6-7 H 비활성/퇴사 + OPEN 슬롯 → 잔존 데이터 관찰.
//  6-7: 비활성 직전 H OPEN 슬롯 생성(2회차 출근). 6-6: 비활성 후 원장/계획/로그인/요청 상태 관찰.
import { getToken, call, evictToken } from "../lib/http.mjs";
import { record } from "../lib/record.mjs";

const H = "20260700036";
const main = async () => {
  const out = { title: "H(QTUSERH) 비활성/퇴사 + OPEN 슬롯 잔존데이터 관찰", steps: [] };

  // (6-7) H OPEN 슬롯 생성 — 오늘 2회차 출근(미퇴근)
  let openSlot = "n/a";
  try {
    const hTok = await getToken("QTUSERH", "QtTest!2026", "APP");
    const ci = await call("POST", "/appApi/attd01/check-in", { token: hTok, clientType: "APP", body: { lat: 37.5665, lon: 126.978, isMocked: "N" } });
    openSlot = `${ci.status} ${(ci.text||"").slice(0,120)}`;
    out.steps.push(`H 2회차 출근(OPEN) status=${openSlot}`);
  } catch (e) { out.steps.push("H checkin EX:" + String(e).slice(0, 120)); }

  // (deactivate) update-user-infos useYn=N (저장 EP)
  try {
    const wtok = await getToken("QTHR", "QtTest!2026", "WEB");
    const list = await call("GET", "/webApi/user01/user-info-lists?userKeyword=QTUSERH", { token: wtok });
    const arr = list.json?.userInfoList || [];
    const target = arr.find(u => u.userId === "QTUSERH");
    if (target) {
      const save = await call("POST", "/webApi/user01/update-user-infos", { token: wtok, body: [{ ...target, useYn: "N", chk: true }] });
      out.steps.push(`H 비활성 저장 status=${save.status} ${JSON.stringify(save.json||{}).slice(0,100)}`);
    } else { out.steps.push("H 미발견"); }
  } catch (e) { out.steps.push("deactivate EX:" + String(e).slice(0, 120)); }

  // (④) H 앱 로그인 차단
  try {
    evictToken("QTUSERH", "APP");
    const r = await call("POST", "/comApi/login/login", { clientType: "APP", body: { userId: "QTUSERH", userPw: "QtTest!2026" } });
    out.steps.push(`H 앱 로그인 status=${r.status} ${(r.json?.errorCode||"").toString().slice(0,40)}`);
  } catch (e) { out.steps.push("H login EX:" + String(e).slice(0, 120)); }

  out.webView = "User_01 H useYn=미사용. 결재함/원장/계획 관찰은 후속 DB+화면.";
  out.appView = `H OPEN 슬롯=${openSlot}, 비활성 후 로그인 차단.`;
  out.dbCheck = "H 원장/계획/요청 상태 후속 DB 쿼리(오염 판정).";
  record("QE-6-6", "OBSERVED", { ...out, note: "H 비활성 실행. OPEN 슬롯 잔존(6-7). 원장/계획/요청/로그인/이력 상세는 후속 DB+화면 관찰로 채움." });
  console.log("DONE");
  process.exit(0);
};
main().catch((e) => { console.error(e); process.exit(1); });
