// QE-I 발효 셋업 — QTUSERC 예약(이동일 내일 7/19) + 대기 보정요청 1건(발효 시 종결 대상).
// 예약 먼저(C 클린 상태 → eligibility 통과), 그다음 대기요청 추가.
import { call, login } from "../lib/http.mjs";

const main = async () => {
  const hr = await login("QTHR", "QtTest!2026", "WEB");
  const t = hr.json.token;

  // 1) 예약 생성(이동일 내일 — API가 오늘 거부). 이후 사용자가 DB로 오늘 백데이팅.
  const resv = await call("POST", "/webApi/user01/transfer-reservation", {
    token: t,
    body: {
      userCd: "20260700031", // QTUSERC (00010 active)
      toSiteCd: "00003", toNodeCd: "n1", toDefaultSchCd: "00001",
      moveDate: "20260719",
      moveReason: "[QE-I-1] 발효 실행+종결+원장원복 검증",
    },
  });
  console.log(`예약: ${resv.status}`, resv.status !== 200 ? resv.text.slice(0, 300) : resv.text.slice(0, 200));
  if (resv.status !== 200) return;

  // 2) C 앱 로그인 → 대기 보정요청(과거 주말 7/11, 발효 시 종결 대상).
  const c = await login("QTUSERC", "QtTest!2026", "APP");
  if (c.status !== 200) { console.log("C 로그인 실패:", c.status, c.text.slice(0, 150)); return; }
  const corr = await call("POST", "/appApi/req07/attd-correction", {
    token: c.json.token, clientType: "APP",
    body: {
      workYmd: "20260711",
      checkInTime: "1300", checkOutTime: "1700",
      reqReason: "[QE-I-1] 발효 시 자동종결 대상",
    },
  });
  console.log(`대기 보정요청: ${corr.status}`, corr.status >= 300 ? corr.text.slice(0, 250) : corr.text.slice(0, 200));
};
main().catch((e) => { console.error("실패:", e.message); process.exitCode = 1; });
