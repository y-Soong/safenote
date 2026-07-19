// 게이트 1단계 — master 로그인(PHONE_AUTH) + SMS 발송. run/<role>.gate.json 저장.
// 사용: node gate-step1.mjs <ROLE>   (ROLE = A_PRIME | B_SUB | C_SUBSUB)
import { readFileSync, writeFileSync } from "node:fs";
import { call, login } from "./lib/http.mjs";

const role = process.argv[2];
if (!role) { console.error("ROLE 인자 필요"); process.exit(1); }

const main = async () => {
  const companies = JSON.parse(readFileSync(new URL("./run/companies.json", import.meta.url), "utf8"));
  const c = companies.find((x) => x.role === role);
  if (!c) { console.error(`companies.json 에 ${role} 없음`); process.exit(1); }

  const auth = await login(c.adminId, c.initialPw);
  if (!auth.ok || auth.json?.nextStep !== "PHONE_AUTH") {
    console.error("로그인/게이트 예상과 다름:", auth.status, JSON.stringify(auth.json)); process.exit(1);
  }
  const phoneAuthToken = auth.json.token;
  const { cmpnyCd, userCd } = auth.json;

  const sms = await call("POST", "/comApi/baseinfo/sms-auth-sends", {
    body: { mblNo: c.adminMbl, dupChkYn: "N" },
  });
  if (!sms.ok) { console.error("SMS 발송 실패:", sms.status, sms.text); process.exit(1); }

  writeFileSync(new URL(`./run/${role}.gate.json`, import.meta.url),
    JSON.stringify({ role, cmpnyCd, userCd, phone: c.adminMbl, phoneAuthToken }, null, 2), "utf8");

  console.log(`[${role}] SMS 발송 완료 — cmpnyCd=${cmpnyCd} userCd=${userCd} phone=${c.adminMbl}`);
  console.log("→ 이제 MCP로 인증번호를 조회하세요(1분 내 검증 필요).");
};

main().catch((e) => { console.error(e); process.exit(1); });
