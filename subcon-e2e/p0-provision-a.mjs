// P0 보정 — 원청 A 재생성(휴대폰 충돌). 후보 번호를 순차 시도해 빈 번호로 A 생성 후 companies.json 병합.
import { readFileSync, writeFileSync } from "node:fs";
import { call, login } from "./lib/http.mjs";

const SYSADMIN = { id: "sysadmin", pw: "tnsrl916" };
const BASE_A = { role: "A_PRIME", cmpnyNm: "테스트원청", bsnsLcnNo: "9990000011", adminNm: "원청관리자", adminId: "subt_prime" };

// 충돌 회피용 후보(이전 테스트에서 안 썼을 법한 번호대).
const CANDIDATES = [
  "01044440001", "01055550001", "01066660001", "01033330001",
  "01522330001", "01611220001", "01812345670", "01098765401",
];

const main = async () => {
  const auth = await login(SYSADMIN.id, SYSADMIN.pw);
  if (!auth.ok) { console.error("sysadmin 로그인 실패", auth.text); process.exit(1); }

  let created = null;
  for (const phone of CANDIDATES) {
    const r = await call("POST", "/platformApi/company", {
      token: auth.json.token,
      body: { ...BASE_A, adminMbl: phone },
    });
    if (r.ok) { created = { phone, resp: r.json }; console.log(`[A_PRIME] OK phone=${phone} cmpnyCd=${r.json?.cmpnyCd}`); break; }
    const code = r.json?.errorCode ?? "?";
    console.log(`[A_PRIME] phone=${phone} status=${r.status} ${code}`);
    if (code !== "PLATFORM_400_008") { console.log("   ⚠️ 휴대폰 외 오류:", r.text); process.exit(1); }
  }
  if (!created) { console.error("모든 후보 번호 충돌 — 후보 확장 필요"); process.exit(1); }

  const url = new URL("./run/companies.json", import.meta.url);
  const arr = JSON.parse(readFileSync(url, "utf8"));
  const entry = { ...BASE_A, adminMbl: created.phone, initialPw: created.phone, status: 200, response: created.resp };
  const idx = arr.findIndex((x) => x.role === "A_PRIME");
  if (idx >= 0) arr[idx] = entry; else arr.unshift(entry);
  writeFileSync(url, JSON.stringify(arr, null, 2), "utf8");
  console.log("companies.json 갱신 완료");
};

main().catch((e) => { console.error(e); process.exit(1); });
