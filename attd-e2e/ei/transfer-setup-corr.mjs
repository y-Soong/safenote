// C 대기 보정요청 재상신(올바른 slots 구조) — 발효 시 자동종결 대상.
import { call, login } from "../lib/http.mjs";

const main = async () => {
  const c = await login("QTUSERC", "QtTest!2026", "APP");
  if (c.status !== 200) { console.log("C 로그인 실패:", c.status, c.text.slice(0, 150)); return; }
  const corr = await call("POST", "/appApi/req07/attd-correction", {
    token: c.json.token, clientType: "APP",
    body: {
      workYmd: "20260711", nodeCd: "n1",
      slots: [{ workSeq: 1, startDate: "20260711", startTime: "1300", endDate: "20260711", endTime: "1700" }],
      reqReason: "[QE-I-1] 발효 시 자동종결 대상",
    },
  });
  console.log(`대기 보정요청: ${corr.status}`, corr.status >= 300 ? corr.text.slice(0, 250) : corr.text.slice(0, 200));
};
main().catch((e) => { console.error("실패:", e.message); process.exitCode = 1; });
