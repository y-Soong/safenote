// P2-a — A 사업장 00001 을 B 로 미러 제안→수락(미러 생성 트랜잭션). run/links.json 에 linkId 기록.
import { writeFileSync } from "node:fs";
import { call } from "./lib/http.mjs";
import { loadSessions } from "./lib/sessions.mjs";

const s = loadSessions();
const dump = (l, r) => console.log(`  ${l}: status=${r.status} ${r.json ? JSON.stringify(r.json) : (r.text || "").slice(0, 200)}`);

const main = async () => {
  console.log("[A→B] 사업장 00001 미러 제안");
  const propose = await call("POST", "/webApi/subcon02/site-link-propose", { token: s.A.token, body: { tgtCmpnyCd: s.B.cmpnyCd, siteCd: "00001" } });
  dump("propose", propose);

  // B 수신 목록에서 PROPOSED 찾기.
  const list = await call("GET", "/webApi/subcon02/site-link-lists", { token: s.B.token });
  console.log("  B site-link-lists:", JSON.stringify(list.json).slice(0, 600));
  const rows = list.json?.receivedList || list.json?.links || list.json?.list || [];
  const proposed = (Array.isArray(rows) ? rows : []).filter((x) => (x.status === "PROPOSED"));
  const linkId = propose.json?.linkId ?? proposed[0]?.linkId;
  console.log("  linkId =", linkId);

  const accept = await call("POST", "/webApi/subcon02/site-link-accept", { token: s.B.token, body: { linkId } });
  dump("accept", accept);

  writeFileSync(new URL("./run/links.json", import.meta.url), JSON.stringify({ AB_site: { srcSiteCd: "00001", linkId } }, null, 2), "utf8");
  console.log("run/links.json 기록");
};

main().catch((e) => { console.error(e); process.exit(1); });
