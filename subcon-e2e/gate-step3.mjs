// 게이트 3단계 — 강제 비밀번호 변경 후 새 비번으로 재로그인(깨끗한 정식 토큰). run/<role>.session.json 갱신.
// 사용: node gate-step3.mjs <ROLE>
import { readFileSync, writeFileSync } from "node:fs";
import { call, login } from "./lib/http.mjs";

export const NEW_PW = "Prafta!2026";

const role = process.argv[2];
if (!role) { console.error("ROLE 인자 필요"); process.exit(1); }

const scopeOf = (t) => { try { return JSON.parse(Buffer.from(t.split(".")[1], "base64").toString()).gv_scope ?? "(full)"; } catch { return "?"; } };

const main = async () => {
  const companies = JSON.parse(readFileSync(new URL("./run/companies.json", import.meta.url), "utf8"));
  const c = companies.find((x) => x.role === role);
  const sess = JSON.parse(readFileSync(new URL(`./run/${role}.session.json`, import.meta.url), "utf8"));

  // 1) 강제 비번 변경(현재=초기PW(휴대폰), 신규=NEW_PW).
  const chg = await call("POST", "/webApi/user01/update-my-passwd", {
    token: sess.token, body: { currentPw: c.initialPw, newPw: NEW_PW },
  });
  if (!chg.ok) { console.error("비번 변경 실패:", chg.status, chg.text); process.exit(1); }
  console.log(`[${role}] 비번 변경 OK`);

  // 2) 새 비번으로 재로그인 → 깨끗한 토큰.
  const re = await login(c.adminId, NEW_PW);
  if (!re.ok) { console.error("재로그인 실패:", re.status, re.text); process.exit(1); }
  console.log(`[${role}] 재로그인 — nextStep=${re.json.nextStep} authCd=${re.json.authCd} mustChangePwd=${re.json.mustChangePassword} tokenScope=${re.json.token ? scopeOf(re.json.token) : "none"}`);

  writeFileSync(new URL(`./run/${role}.session.json`, import.meta.url),
    JSON.stringify({ role, cmpnyCd: re.json.cmpnyCd, userCd: re.json.userCd, authCd: re.json.authCd,
      accountStatus: re.json.accountStatus, nextStep: re.json.nextStep, newPw: NEW_PW, token: re.json.token }, null, 2), "utf8");
  console.log(`[${role}] 세션 갱신 완료`);
};

main().catch((e) => { console.error(e); process.exit(1); });
