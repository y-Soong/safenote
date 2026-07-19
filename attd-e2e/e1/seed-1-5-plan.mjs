// QE-1-5 시드 — QTUSERC 7/14(화) 근무계획 QTOVN(00005) 배정.
import { call, getToken } from "../lib/http.mjs";

const main = async () => {
  const t = await getToken("QTHR", "QtTest!2026", "WEB");
  const r = await call("POST", "/webApi/attd05/save-user-work-plans", {
    token: t,
    body: [{ siteCd: "00010", userCd: "20260700031", workYmd: "20260714", workPlanCd: "00005" }],
  });
  console.log("배정:", r.status, r.text.slice(0, 200));
};
main().catch((e) => { console.error("실패:", e.message); process.exitCode = 1; });
