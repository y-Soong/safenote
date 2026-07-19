// P0 — 테스트 회사 3개(원청 A / 하청 B / 재하청 C) 프로비저닝.
// sysadmin 로그인 → POST /platformApi/company 3회. 결과를 run/companies.json 에 기록.
import { writeFileSync, mkdirSync } from "node:fs";
import { call, login } from "./lib/http.mjs";

const SYSADMIN = { id: "sysadmin", pw: "tnsrl916" };

const COMPANIES = [
  { role: "A_PRIME",  cmpnyNm: "테스트원청",   bsnsLcnNo: "9990000011", adminNm: "원청관리자",   adminId: "subt_prime", adminMbl: "01099990001" },
  { role: "B_SUB",    cmpnyNm: "테스트하청",   bsnsLcnNo: "9990000022", adminNm: "하청관리자",   adminId: "subt_sub1",  adminMbl: "01099990002" },
  { role: "C_SUBSUB", cmpnyNm: "테스트재하청", bsnsLcnNo: "9990000033", adminNm: "재하청관리자", adminId: "subt_sub2",  adminMbl: "01099990003" },
];

const main = async () => {
  mkdirSync(new URL("./run/", import.meta.url), { recursive: true });

  const auth = await login(SYSADMIN.id, SYSADMIN.pw);
  if (!auth.ok || !auth.json?.token) {
    console.error("sysadmin 로그인 실패:", auth.status, auth.text);
    process.exit(1);
  }
  console.log(`sysadmin 로그인 OK (cmpnyCd=${auth.json.cmpnyCd}, auth=${auth.json.authCd})`);
  const sysToken = auth.json.token;

  const results = [];
  for (const c of COMPANIES) {
    const r = await call("POST", "/platformApi/company", {
      token: sysToken,
      body: {
        cmpnyNm: c.cmpnyNm,
        bsnsLcnNo: c.bsnsLcnNo,
        adminNm: c.adminNm,
        adminId: c.adminId,
        adminMbl: c.adminMbl,
      },
    });
    const entry = { ...c, initialPw: c.adminMbl, status: r.status, response: r.json ?? r.text };
    results.push(entry);
    console.log(`[${c.role}] provision status=${r.status} → cmpnyCd=${r.json?.cmpnyCd ?? "?"} adminId=${c.adminId}`);
    if (!r.ok) console.log(`   ⚠️ 응답본문: ${r.text}`);
  }

  writeFileSync(new URL("./run/companies.json", import.meta.url), JSON.stringify(results, null, 2), "utf8");
  console.log("\n결과 → run/companies.json 기록 완료");
};

main().catch((e) => { console.error(e); process.exit(1); });
