// E4 드리프트 복구 — QE-4-2 부작용으로 QT8H(00001) APPLY_DATE가 20260813으로 남음.
// 8/13 이전 날짜의 스케줄 해소 불능 → E5/E6 판정 오염 방지 위해 20260101로 원복.
// 시간·휴게 동일 유지(가드 guardScheduleTimeChange 회피). 테스트 데이터 원복이며 제품 코드 무수정.
import { call, login } from "../lib/http.mjs";

const main = async () => {
  const hr = await login("QTHR", "QtTest!2026", "WEB");
  const t = hr.json.token;
  const body = {
    siteCd: "00010", schCd: "00001", schNo: "QT8H", schType: "01",
    applyDate: "20260101",
    fstSchStrTime: "0900", fstSchEndTime: "1800", fstSchBrkMin: "60",
    fstBrkStrTime: "1200", fstBrkEndTime: "1300",
    secSchStrTime: "", secSchEndTime: "", secSchBrkMin: "", secBrkStrTime: "", secBrkEndTime: "",
    useYn: "Y",
  };
  const r = await call("POST", "/webApi/attd01/update-sch-infos", { token: t, body });
  console.log(`QT8H applyDate 원복: ${r.status}`, r.status !== 200 ? r.text.slice(0, 250) : "OK");
};
main().catch((e) => { console.error("실패:", e.message); process.exitCode = 1; });
