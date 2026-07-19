// QE-1-4 시드 — QTUSERD 오늘(7/17) 근무계획을 QT2SEG(00004)로 교체.
import { call, getToken } from "../lib/http.mjs";

const main = async () => {
  const t = await getToken("QTHR", "QtTest!2026", "WEB");
  const r = await call("POST", "/webApi/attd05/save-user-work-plans", {
    token: t,
    body: [{ siteCd: "00010", userCd: "20260700032", workYmd: "20260717", workPlanCd: "00004" }],
  });
  console.log("교체:", r.status, r.text.slice(0, 200));
};
main().catch((e) => { console.error("실패:", e.message); process.exitCode = 1; });
