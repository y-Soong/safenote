// F1 라이브검증 (deactivate) — QTUSERD useYn→N. F1 훅으로 D 대기요청 자동 종결 기대.
import { getToken, call } from "../lib/http.mjs";

const PW = "QtTest!2026";
const line = (o) => console.log(JSON.stringify(o));

const run = async () => {
  const wtok = await getToken("QTHR", PW, "WEB");

  // 대상 조회
  const list = await call("GET", "/webApi/user01/user-info-lists?userKeyword=QTUSERD", { token: wtok });
  const arr = list.json?.userInfoList || list.json?.list || [];
  const target = arr.find((u) => u.userId === "QTUSERD" || u.userCd === "20260700032");
  line({ tag: "target-keys", keys: target ? Object.keys(target) : null, useYn: target?.useYn, userCd: target?.userCd });
  if (!target) { console.log("NO_TARGET"); process.exit(1); }

  // 비활성 저장 (동적 if 업데이트 — 부분필드 안전, chk:true)
  const save = await call("POST", "/webApi/user01/update-user-infos", { token: wtok, body: [{ ...target, useYn: "N", chk: true }] });
  line({ tag: "deactivate(D)", status: save.status, body: save.json, text: (save.text||"").slice(0,200) });

  // 마감 미결현황 after
  const st = await call("GET", "/webApi/attd07/attd-close-status?siteCd=00010&nodeCd=&incSubNodeYn=N&closeYm=202607", { token: wtok });
  line({ tag: "close-status-after", status: st.status, body: st.json });

  console.log("DEACT_DONE");
};
run().then(() => process.exit(0)).catch((e) => { console.error("ERR", e); process.exit(1); });
