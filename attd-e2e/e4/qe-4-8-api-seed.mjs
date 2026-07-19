// QE-4-8 대체 시드(앱 UI 미래일 보정 선차단) — API로 A 8/11 보정 상신
import { getToken, call } from "../lib/http.mjs";

const main = async () => {
  const token = await getToken("QTUSERA", "QtTest!2026", "APP");
  const body = {
    workYmd: "20260811",
    nodeCd: "n1",
    slots: [{ workSeq: 1, startDate: "20260811", startTime: "1000", endDate: "20260811", endTime: "1700" }],
    reqReason: "[QE-4-8] 8/11 보정 대기(API 시드 - 앱 미래일 선차단 우회)",
  };
  const r = await call("POST", "/appApi/req07/attd-correction", { token, body, clientType: "APP" });
  console.log("status:", r.status, "body:", (r.text || "").slice(0, 400));
};
main();
