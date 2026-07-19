// QE-4-13 시드 — A 8/14 스케줄수정요청(REQ_TYPE 10) 대기 생성. QT8H->QT9H(00003)
import { getToken, call } from "../lib/http.mjs";

const main = async () => {
  const token = await getToken("QTUSERA", "QtTest!2026", "APP");
  const body = {
    workYmd: "20260814",
    nodeCd: "n1",
    slots: [{ workSeq: 1, schCd: "00003" }],
    reqReason: "[QE-4-13] 8/14 스케줄수정요청 대기(E6 마감 차단 연계용)",
  };
  const r = await call("POST", "/appApi/req07/sched-modify", { token, body, clientType: "APP" });
  console.log("status:", r.status, "body:", (r.text || "").slice(0, 400));
};
main();
