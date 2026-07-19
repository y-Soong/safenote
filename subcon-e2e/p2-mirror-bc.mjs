// P2-b — B 가 받은 미러 사업장 00002 를 C 로 재공유(n차) + 루프 가드(B→A 역공유) 검증.
import { readFileSync, writeFileSync } from "node:fs";
import { call } from "./lib/http.mjs";
import { loadSessions } from "./lib/sessions.mjs";

const s = loadSessions();
const dump = (l, r) => console.log(`  ${l}: status=${r.status} ${r.json ? JSON.stringify(r.json) : (r.text || "").slice(0, 200)}`);

const main = async () => {
  // 1) B → C 재공유(미러 사업장 00002).
  console.log("[B→C] 미러 사업장 00002 재공유(n차)");
  const propose = await call("POST", "/webApi/subcon02/site-link-propose", { token: s.B.token, body: { tgtCmpnyCd: s.C.cmpnyCd, siteCd: "00002" } });
  dump("propose", propose);
  const linkId = propose.json?.linkId;

  const list = await call("GET", "/webApi/subcon02/site-link-lists", { token: s.C.token });
  const rows = list.json?.links || [];
  const proposed = rows.filter((x) => x.status === "PROPOSED");
  console.log("  C 수신 PROPOSED:", JSON.stringify(proposed.map((x) => ({ id: x.linkId, other: x.otherCmpnyNm, src: x.srcSiteNm }))));
  const accept = await call("POST", "/webApi/subcon02/site-link-accept", { token: s.C.token, body: { linkId: linkId ?? proposed[0]?.linkId } });
  dump("accept", accept);

  // 2) 루프 가드 — B 가 미러 00002 를 원본 회사 A 로 역공유 시도.
  console.log("\n[루프] B→A 미러 00002 역공유(원본 회사로 되돌리기)");
  const loop = await call("POST", "/webApi/subcon02/site-link-propose", { token: s.B.token, body: { tgtCmpnyCd: s.A.cmpnyCd, siteCd: "00002" } });
  dump("loop propose", loop);

  const links = JSON.parse(readFileSync(new URL("./run/links.json", import.meta.url), "utf8"));
  links.BC_site = { srcSiteCd: "00002", linkId };
  writeFileSync(new URL("./run/links.json", import.meta.url), JSON.stringify(links, null, 2), "utf8");
  console.log("\nrun/links.json 갱신");
};

main().catch((e) => { console.error(e); process.exit(1); });
