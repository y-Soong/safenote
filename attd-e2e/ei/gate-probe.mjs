// QE-I 게이트 프로브 — QTUSERC를 이동일=오늘로 예약해 발효 스케줄러 게이트/cron 생존 확인.
// 게이트 ON + 짧은 cron이면 RESERVED→APPLIED 자동 전환. 아니면 RESERVED 잔존.
import { call, login } from "../lib/http.mjs";

const main = async () => {
  const hr = await login("QTHR", "QtTest!2026", "WEB");
  const t = hr.json.token;
  const body = {
    userCd: "20260700031", // QTUSERC (00010, active)
    toSiteCd: "00003", toNodeCd: "n1", toDefaultSchCd: "00001",
    moveDate: "20260718", // 오늘 — 발효 대상
    moveReason: "[QE-I-probe] 발효 게이트 생존 확인",
  };
  const r = await call("POST", "/webApi/user01/transfer-reservation", { token: t, body });
  console.log(`예약 등록: ${r.status}`, r.status !== 200 ? r.text.slice(0, 300) : r.text.slice(0, 200));
};
main().catch((e) => { console.error("실패:", e.message); process.exitCode = 1; });
