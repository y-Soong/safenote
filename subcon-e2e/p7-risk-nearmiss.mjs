// P7 — A-B 관계/미러 재수립(P8에서 해지됨) 후 RISK/NEARMISS 공유 요청→승인→스냅샷 검증.
import { call } from "./lib/http.mjs";
import { loadSessions } from "./lib/sessions.mjs";

const s = loadSessions();
const j = (r) => (r.json ? JSON.stringify(r.json) : (r.text || "").slice(0, 160));

const main = async () => {
  // 1) 관계 재수립 A→B.
  console.log("== 관계 재수립 A→B ==");
  const req = await call("POST", "/webApi/subcon01/relation-request", { token: s.A.token, body: { tgtCmpnyCd: s.B.cmpnyCd } });
  console.log("  request:", req.status, j(req));
  const list = await call("GET", "/webApi/subcon01/relation-lists", { token: s.B.token });
  const recv = (list.json?.relations || []).find((x) => x.direction === "RECEIVED" && x.status === "REQUESTED" && x.otherCmpnyCd === s.A.cmpnyCd);
  if (recv) { const ac = await call("POST", "/webApi/subcon01/relation-accept", { token: s.B.token, body: { relationId: recv.relationId } }); console.log("  accept:", ac.status); }

  // 2) 사업장 미러 재수립 A/00001 → B.
  console.log("== 사업장 미러 재수립 ==");
  const prop = await call("POST", "/webApi/subcon02/site-link-propose", { token: s.A.token, body: { tgtCmpnyCd: s.B.cmpnyCd, siteCd: "00001" } });
  console.log("  propose:", prop.status, j(prop));
  const slist = await call("GET", "/webApi/subcon02/site-link-lists", { token: s.B.token });
  const prow = (slist.json?.links || []).find((x) => x.status === "PROPOSED");
  if (prow) { const ac = await call("POST", "/webApi/subcon02/site-link-accept", { token: s.B.token, body: { linkId: prow.linkId } }); console.log("  accept:", ac.status, "linkId=", prow.linkId); }

  // 3) RISK / NEARMISS 요청→승인→스냅샷.
  for (const dt of ["RISK", "NEARMISS"]) {
    console.log(`== ${dt} 공유 ==`);
    const c = await call("POST", "/webApi/subcon03/share-req-create", { token: s.A.token, body: { prvCmpnyCd: s.B.cmpnyCd, siteCd: "00001", dataType: dt, periodStr: "20260101", periodEnd: "20260630", closedOnlyYn: "N", purpose: dt + " 검증" } });
    console.log("  create:", c.status, j(c));
    const rid = c.json?.shareReqId;
    if (rid) {
      const ap = await call("POST", "/webApi/subcon03/share-req-approve", { token: s.B.token, body: { shareReqId: rid } });
      console.log("  approve:", ap.status, j(ap));
    }
  }

  // 4) A 스냅샷 목록 (RISK/NEARMISS 유형 확인).
  const snap = await call("GET", "/webApi/subcon03/snapshot-lists", { token: s.A.token });
  console.log("== A 스냅샷 유형 ==", JSON.stringify((snap.json?.snapshots || []).map((x) => ({ id: x.snapshotId, dt: x.dataType, cnt: x.rowCnt }))));
};

main().catch((e) => { console.error(e); process.exit(1); });
