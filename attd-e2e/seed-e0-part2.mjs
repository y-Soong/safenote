// E0 시드 2부 — H/I 활성화 완결: verify-phone-auth → set-default-sch → 비번변경 → 약관동의 → policy-grant.
// 사용법: node seed-e0-part2.mjs <H코드> <I코드>
import { call, login } from "./lib/http.mjs";
import { readFileSync } from "node:fs";

const [codeH, codeI] = process.argv.slice(2);
if (!codeH || !codeI) { console.error("사용법: node seed-e0-part2.mjs <H코드> <I코드>"); process.exit(1); }

const gates = JSON.parse(readFileSync(new URL("./run/e0-gates.json", import.meta.url), "utf8"));
const ACCTS = [
  { id: "QTUSERH", mbl: "01077770021", code: codeH, initPw: "01077770021" },
  { id: "QTUSERI", mbl: "01077770022", code: codeI, initPw: "01077770022" },
];
const NEW_PW = "QtTest!2026";

const main = async () => {
  const userCds = [];
  for (const a of ACCTS) {
    let tempToken = gates[a.id]?.tempToken;
    // 1) 휴대폰 인증.
    let r = await call("POST", "/comApi/login/verify-phone-auth", { token: tempToken, body: { mblNo: a.mbl, certNo: a.code } });
    console.log(`${a.id} verify-phone-auth: ${r.status} nextStep=${r.json?.nextStep}`, r.status !== 200 ? r.text.slice(0, 150) : "");
    let token = r.json?.token;
    // 2) 기본근무 게이트(발동 시).
    if (r.json?.nextStep === "DEFAULT_SCH") {
      r = await call("POST", "/comApi/login/set-default-sch", { token, body: { defaultSchCd: "00001" } });
      console.log(`${a.id} set-default-sch: ${r.status} nextStep=${r.json?.nextStep}`);
      token = r.json?.token;
    }
    // 3) 비번 변경(mustChangePassword).
    const userCd = r.json?.userCd;
    r = await call("POST", "/webApi/user01/update-my-passwd", { token, body: { currentPw: a.initPw, newPw: NEW_PW } });
    console.log(`${a.id} update-my-passwd: ${r.status}`, r.status !== 200 ? r.text.slice(0, 150) : "");
    // 4) 재로그인(정상화 확인).
    r = await login(a.id, NEW_PW, "WEB");
    console.log(`${a.id} 재로그인: ${r.status} nextStep=${r.json?.nextStep} userCd=${r.json?.userCd}`);
    if (r.status === 200) {
      userCds.push(r.json.userCd);
      // 5) 필수약관 일괄 동의(앱 EP — webApi 게이트 해소).
      const ag = await call("POST", "/appApi/terms01/agree-required-terms", { token: r.json.token, body: {}, clientType: "APP" });
      console.log(`${a.id} 약관동의: ${ag.status}`, ag.status !== 200 ? ag.text.slice(0, 150) : "");
    }
  }
  // 6) H/I 연차 부여.
  if (userCds.length) {
    const hr = await login("QTHR", NEW_PW, "WEB");
    const g = await call("POST", "/webApi/attd09/leave-grant/policy-grant", { token: hr.json.token, body: { userCds } });
    console.log("H/I policy-grant:", g.status, g.text.slice(0, 300));
  }
  console.log("E0 2부 완료. userCds=", userCds.join(","));
};
main().catch((e) => { console.error("E0 2부 실패:", e.message); process.exitCode = 1; });
