// 게이트 2단계 — 본인인증 검증(+필요시 기본근무 설정)으로 정식 토큰 확보. run/<role>.session.json 저장.
// 사용: node gate-step2.mjs <ROLE> <CERT_NO>
import { readFileSync, writeFileSync } from "node:fs";
import { call } from "./lib/http.mjs";

const role = process.argv[2];
const certNo = process.argv[3];
if (!role || !certNo) { console.error("사용: node gate-step2.mjs <ROLE> <CERT_NO>"); process.exit(1); }

const scopeOf = (token) => {
  try { return JSON.parse(Buffer.from(token.split(".")[1], "base64").toString()).gv_scope ?? "(full)"; }
  catch { return "?"; }
};

const main = async () => {
  const gate = JSON.parse(readFileSync(new URL(`./run/${role}.gate.json`, import.meta.url), "utf8"));

  // 1) 본인인증 검증 → '01' 전이.
  const verify = await call("POST", "/comApi/login/verify-phone-auth", {
    token: gate.phoneAuthToken,
    body: { mblNo: gate.phone, certNo },
  });
  if (!verify.ok) { console.error("본인인증 실패:", verify.status, verify.text); process.exit(1); }
  let resp = verify.json;
  console.log(`[${role}] 본인인증 OK — nextStep=${resp.nextStep} tokenScope=${resp.token ? scopeOf(resp.token) : "none"}`);

  let finalToken = resp.token;

  // 2) 기본근무 게이트가 남아 있으면 설정.
  if (resp.nextStep === "DEFAULT_SCH" || (resp.token && scopeOf(resp.token) === "DEFAULT_SCH")) {
    const opts = await call("GET", "/comApi/login/default-sch-options", { token: resp.token });
    const list = opts.json?.schedules ?? opts.json ?? [];
    if (!Array.isArray(list) || list.length === 0) { console.error("근무타입 옵션 없음:", opts.text); process.exit(1); }
    const schCd = list[0].schCd;
    const setr = await call("POST", "/comApi/login/set-default-sch", {
      token: resp.token, body: { defaultSchCd: schCd },
    });
    if (!setr.ok) { console.error("기본근무 설정 실패:", setr.status, setr.text); process.exit(1); }
    resp = setr.json;
    finalToken = setr.json.token;
    console.log(`[${role}] 기본근무 설정 OK — schCd=${schCd} 최종 nextStep=${resp.nextStep} authCd=${resp.authCd} tokenScope=${finalToken ? scopeOf(finalToken) : "none"}`);
  }

  writeFileSync(new URL(`./run/${role}.session.json`, import.meta.url),
    JSON.stringify({ role, cmpnyCd: gate.cmpnyCd, userCd: gate.userCd, authCd: resp.authCd,
      accountStatus: resp.accountStatus, nextStep: resp.nextStep, mustChangePassword: resp.mustChangePassword,
      token: finalToken }, null, 2), "utf8");
  console.log(`[${role}] 세션 저장 완료 → run/${role}.session.json (authCd=${resp.authCd}, mustChangePwd=${resp.mustChangePassword})`);
};

main().catch((e) => { console.error(e); process.exit(1); });
