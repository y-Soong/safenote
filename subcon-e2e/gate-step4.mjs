// 게이트 4단계 — 필수 약관 전체 동의 후 재로그인. run/<role>.session.json 갱신 + Subcon API 접근 확인.
// 사용: node gate-step4.mjs <ROLE>
import { readFileSync, writeFileSync } from "node:fs";
import { call, login } from "./lib/http.mjs";

const role = process.argv[2];
if (!role) { console.error("ROLE 인자 필요"); process.exit(1); }

const main = async () => {
  const companies = JSON.parse(readFileSync(new URL("./run/companies.json", import.meta.url), "utf8"));
  const c = companies.find((x) => x.role === role);
  const sess = JSON.parse(readFileSync(new URL(`./run/${role}.session.json`, import.meta.url), "utf8"));

  // 1) 대기 약관 조회.
  const chk = await call("GET", `/comApi/login/user-terms-agreement-check?userId=${encodeURIComponent(c.adminId)}`, { token: sess.token });
  const pending = (chk.json?.userTermsAgreementCheckList || []).filter((t) => t.userTermsAgrYn === "N");
  console.log(`[${role}] 대기 약관: ${pending.map((t) => t.termsId).join(",") || "(없음)"}`);

  // 2) 전체 동의 제출.
  if (pending.length > 0) {
    const payload = pending.map((t) => ({ systValDCd: t.termsId }));
    const agr = await call("POST", "/comApi/login/update-auth-menu-info", { token: sess.token, body: payload });
    if (!agr.ok) { console.error("약관 동의 실패:", agr.status, agr.text); process.exit(1); }
    console.log(`[${role}] 약관 동의 제출 OK (${pending.length}건)`);
  }

  // 3) 재로그인 → 깨끗한 토큰.
  const re = await login(c.adminId, sess.newPw || c.initialPw);
  if (!re.ok) { console.error("재로그인 실패:", re.text); process.exit(1); }

  // 4) Subcon API 접근 확인.
  const test = await call("GET", "/webApi/subcon01/relation-lists", { token: re.json.token });
  console.log(`[${role}] 재로그인 nextStep=${re.json.nextStep} → relation-lists status=${test.status}`);

  writeFileSync(new URL(`./run/${role}.session.json`, import.meta.url),
    JSON.stringify({ role, cmpnyCd: re.json.cmpnyCd, userCd: re.json.userCd, authCd: re.json.authCd,
      accountStatus: re.json.accountStatus, nextStep: re.json.nextStep, newPw: sess.newPw, token: re.json.token,
      subconOk: test.status === 200 }, null, 2), "utf8");
  console.log(`[${role}] ${test.status === 200 ? "✅ 게이트 완전 통과 — Subcon API 접근 가능" : "⚠️ 아직 차단: " + test.text}`);
};

main().catch((e) => { console.error(e); process.exit(1); });
