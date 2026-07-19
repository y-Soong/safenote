// E4 드리프트 복구 — QE-4-5가 maxDays 26→25(policySeq 21) 변경 후 원복 전 사망.
// 현재 활성 정책 전체 축을 읽어 AXIS5_MAX_DAYS만 26으로 되돌린 새 버전 생성.
import { call, login } from "../lib/http.mjs";

const main = async () => {
  const hr = await login("QTHR", "QtTest!2026", "WEB");
  const t = hr.json.token;

  const act = await call("GET", "/webApi/baim07/policy/active", { token: t });
  if (act.status !== 200) { console.error("활성정책 조회 실패:", act.status, act.text.slice(0, 200)); process.exit(1); }
  const p = act.json.policy;
  console.log("현재 활성 정책:", JSON.stringify({ seq: p.policySeq, maxDays: p.axis5MaxDays }, null, 0));

  const body = { ...p };
  delete body.policySeq; delete body.insertNo; delete body.insertDate; delete body.updateNo; delete body.updateDate;
  body.axis5MaxDays = 26;
  body.changeReason = "[QE-4-5 복구] maxDays 26 원복";

  const r = await call("POST", "/webApi/baim07/policy", { token: t, body });
  console.log(`정책 복구 POST: ${r.status}`, r.status !== 200 ? r.text.slice(0, 300) : r.text.slice(0, 200));
};
main().catch((e) => { console.error("실패:", e.message); process.exitCode = 1; });
