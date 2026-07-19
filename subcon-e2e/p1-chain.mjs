// P1 — 3뎁스 관계 체인(A→B, B→C) 수립. 각 단계 응답을 출력하고 run/relations.json 에 relationId 기록.
import { writeFileSync } from "node:fs";
import { call } from "./lib/http.mjs";
import { loadSessions } from "./lib/sessions.mjs";

const s = loadSessions();
const dump = (label, r) => console.log(`  ${label}: status=${r.status} ${r.json ? JSON.stringify(r.json) : (r.text || "").slice(0, 200)}`);

// provider 가 target 회사에 연동 요청 → target 이 수락. 수락에 쓸 relationId 반환.
async function requestAndAccept(provider, target, label) {
  console.log(`\n[${label}] ${provider.role} → ${target.role} (tgt=${target.cmpnyCd})`);

  // 1) provider 가 target 회사코드 정확검색.
  const search = await call("GET", `/webApi/subcon01/cmpny-exact-search?cmpnyCd=${encodeURIComponent(target.cmpnyCd)}`, { token: provider.token });
  dump("exact-search", search);

  // 2) provider 가 연동 요청.
  const req = await call("POST", "/webApi/subcon01/relation-request", { token: provider.token, body: { tgtCmpnyCd: target.cmpnyCd } });
  dump("relation-request", req);

  // 3) target 이 받은 요청 목록에서 REQUESTED 찾기.
  const list = await call("GET", "/webApi/subcon01/relation-lists", { token: target.token });
  const received = (list.json?.relations || []).filter((x) => x.direction === "RECEIVED" && x.status === "REQUESTED" && x.otherCmpnyCd === provider.cmpnyCd);
  console.log(`  target 받은 REQUESTED: ${received.map((x) => x.relationId).join(",") || "(없음)"}`);
  if (received.length === 0) { console.error("  ⚠️ 받은 요청 없음 — 중단"); return null; }
  const relationId = received[0].relationId;

  // 4) target 이 수락.
  const accept = await call("POST", "/webApi/subcon01/relation-accept", { token: target.token, body: { relationId } });
  dump("relation-accept", accept);

  return relationId;
}

const main = async () => {
  const abId = await requestAndAccept(s.A, s.B, "A→B");
  const bcId = await requestAndAccept(s.B, s.C, "B→C");

  writeFileSync(new URL("./run/relations.json", import.meta.url),
    JSON.stringify({ AB: { provider: s.A.cmpnyCd, target: s.B.cmpnyCd, relationId: abId },
                     BC: { provider: s.B.cmpnyCd, target: s.C.cmpnyCd, relationId: bcId } }, null, 2), "utf8");
  console.log("\nrun/relations.json 기록 완료");
};

main().catch((e) => { console.error(e); process.exit(1); });
