// P1 가드 검증 — 자기검색/중복요청/역연동(루프)/IDOR/없는코드. 실제 응답을 그대로 관찰(결함 발견 목적).
import { readFileSync } from "node:fs";
import { call } from "./lib/http.mjs";
import { loadSessions } from "./lib/sessions.mjs";

const s = loadSessions();
const rel = JSON.parse(readFileSync(new URL("./run/relations.json", import.meta.url), "utf8"));
const line = (label, r, expect) => console.log(`${label}\n   → status=${r.status} ${r.json ? JSON.stringify(r.json) : (r.text || "").slice(0, 200)}\n   기대: ${expect}\n`);

const main = async () => {
  // G1 — 자기 회사코드 정확검색(열거 방지: 자기 자신 제외 기대).
  line("G1 자기검색 (A가 A 코드 검색)",
    await call("GET", `/webApi/subcon01/cmpny-exact-search?cmpnyCd=${s.A.cmpnyCd}`, { token: s.A.token }),
    "자기 제외 → 빈 결과 또는 오류");

  // G2 — 이미 ACCEPTED 인 A→B 재요청(중복 차단 기대).
  line("G2 중복요청 (A→B 재요청, 이미 연동중)",
    await call("POST", "/webApi/subcon01/relation-request", { token: s.A.token, body: { tgtCmpnyCd: s.B.cmpnyCd } }),
    "중복 차단(4xx)");

  // G3 — C→A 역방향 요청(체인 조상 역연동/루프 차단 여부 관찰).
  line("G3 역연동 (C→A 요청, C는 A의 2차 하위)",
    await call("POST", "/webApi/subcon01/relation-request", { token: s.C.token, body: { tgtCmpnyCd: s.A.cmpnyCd } }),
    "루프면 차단 기대(관계레벨 허용 가능성도 관찰)");

  // G4 — IDOR: A가 B-C 관계(relationId=BC)를 수락 시도(자사 아님).
  line("G4 IDOR 수락 (A가 B-C 관계 수락 시도)",
    await call("POST", "/webApi/subcon01/relation-accept", { token: s.A.token, body: { relationId: rel.BC.relationId } }),
    "타사 관계 → 차단(4xx)");

  // G5 — 존재하지 않는 회사코드 검색.
  line("G5 없는코드 검색 (A가 ZZZZ 검색)",
    await call("GET", "/webApi/subcon01/cmpny-exact-search?cmpnyCd=ZZZZNONEXIST0000", { token: s.A.token }),
    "빈 결과/오류");

  // G6 — IDOR: C가 A-B 관계(relationId=AB) 취소 시도.
  line("G6 IDOR 취소 (C가 A-B 관계 취소 시도)",
    await call("POST", "/webApi/subcon01/relation-cancel", { token: s.C.token, body: { relationId: rel.AB.relationId } }),
    "타사 관계 → 차단(4xx)");

  // G7 — 자기 자신에게 요청(A→A).
  line("G7 자기요청 (A→A)",
    await call("POST", "/webApi/subcon01/relation-request", { token: s.A.token, body: { tgtCmpnyCd: s.A.cmpnyCd } }),
    "차단(4xx)");
};

main().catch((e) => { console.error(e); process.exit(1); });
