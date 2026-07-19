// P3 릴레이 구조 — B가 C 근태 스냅샷 수령 → A가 B 요청 시 B가 C스냅샷을 릴레이(bundle). RELAY_INCLUDED_YN 검증.
import { call } from "./lib/http.mjs";
import { loadSessions } from "./lib/sessions.mjs";

const s = loadSessions();
const j = (r) => (r.json ? JSON.stringify(r.json) : (r.text || "").slice(0, 200));

const main = async () => {
  // 1) B → C 근태 공유요청 (B의 사업장 00002 를 C가 미러보유).
  console.log("[B→C] 근태 공유요청");
  const bcReq = await call("POST", "/webApi/subcon03/share-req-create", { token: s.B.token,
    body: { prvCmpnyCd: s.C.cmpnyCd, siteCd: "00002", dataType: "ATTD", periodStr: "20260601", periodEnd: "20260630", closedOnlyYn: "N", purpose: "B가 C에 근태요청" } });
  console.log("  create:", bcReq.status, j(bcReq));
  const bcId = bcReq.json?.shareReqId;
  const bcAp = await call("POST", "/webApi/subcon03/share-req-approve", { token: s.C.token, body: { shareReqId: bcId } });
  console.log("  C 승인:", bcAp.status, j(bcAp));
  const snapFromC = bcAp.json?.snapshotId;

  // 2) A → B 근태 공유요청(재요청 = 새 버전). B 승인 시 C스냅샷 릴레이.
  console.log("[A→B] 근태 재요청(릴레이 유도)");
  const abReq = await call("POST", "/webApi/subcon03/share-req-create", { token: s.A.token,
    body: { prvCmpnyCd: s.B.cmpnyCd, siteCd: "00001", dataType: "ATTD", periodStr: "20260601", periodEnd: "20260630", closedOnlyYn: "N", purpose: "A가 B에 근태요청(릴레이)" } });
  console.log("  create:", abReq.status, j(abReq));
  const abId = abReq.json?.shareReqId;

  // 3) B 승인 사전정보 → relayCandidates 확인.
  const info = await call("GET", `/webApi/subcon03/share-req-approve-info?shareReqId=${abId}`, { token: s.B.token });
  console.log("  B approve-info relayCandidates:", JSON.stringify(info.json?.relayCandidates || []));

  // 4) B 승인 + C스냅샷 릴레이(bundle).
  const bundle = snapFromC ? [snapFromC] : [];
  const abAp = await call("POST", "/webApi/subcon03/share-req-approve", { token: s.B.token, body: { shareReqId: abId, bundleSnapshotIds: bundle } });
  console.log("  B 승인(bundle=" + JSON.stringify(bundle) + "):", abAp.status, j(abAp));
};

main().catch((e) => { console.error(e); process.exit(1); });
