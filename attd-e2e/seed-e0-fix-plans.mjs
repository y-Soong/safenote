// E0 시드 보정 — UTC 하루밀림 배정분 삭제 후 KST 기준 평일로 재배정.
import { call, login } from "./lib/http.mjs";

const USERS = { A: "20260700029", C: "20260700031", D: "20260700032", G: "20260700034" };

// KST 평일 나열 — Date 객체 대신 순수 문자열 산술(UTC 함정 원천 차단).
function kstWeekdays(fromYmd, toYmd) {
  const out = [];
  let y = +fromYmd.slice(0, 4), m = +fromYmd.slice(4, 6), d = +fromYmd.slice(6, 8);
  const endNum = +toYmd;
  const daysIn = (yy, mm) => new Date(Date.UTC(yy, mm, 0)).getUTCDate();
  // 요일: Zeller 대신 UTC Date 사용하되 날짜 문자열은 별도 유지(표기만 사용).
  while (y * 10000 + m * 100 + d <= endNum) {
    const dow = new Date(Date.UTC(y, m - 1, d)).getUTCDay(); // UTC라도 같은 달력 날짜의 요일은 동일
    if (dow >= 1 && dow <= 5) out.push(`${y}${String(m).padStart(2, "0")}${String(d).padStart(2, "0")}`);
    d++;
    if (d > daysIn(y, m)) { d = 1; m++; if (m > 12) { m = 1; y++; } }
  }
  return out;
}

const main = async () => {
  const hr = await login("QTHR", "QtTest!2026", "WEB");
  const t = hr.json.token;

  // 1) 잘못 들어간 기존 배정(7/16~8/30, UTC 밀림분) 셀 삭제 — A/C/G/D 전 행 (H/I는 서버 생성이라 제외).
  const wrong = [];
  for (const [k, userCd] of Object.entries(USERS)) {
    // 밀림분은 내가 넣은 유일한 데이터이므로 7/16~8/30 전 날짜를 셀 삭제 대상으로.
    let y = 2026, m = 7, d = 16;
    while (y * 10000 + m * 100 + d <= 20260830) {
      wrong.push({ siteCd: "00010", userCd, workYmd: `${y}${String(m).padStart(2, "0")}${String(d).padStart(2, "0")}` });
      d++;
      const dim = new Date(Date.UTC(y, m, 0)).getUTCDate();
      if (d > dim) { d = 1; m++; }
    }
  }
  const del = await call("POST", "/webApi/attd05/delete-user-work-plan-cells", { token: t, body: wrong });
  console.log(`밀림분 셀 삭제: ${del.status}`, del.text.slice(0, 200));

  // 2) KST 평일 재배정.
  const rows = [];
  for (const [k, userCd] of Object.entries(USERS)) {
    const to = k === "D" ? "20260731" : "20260831";
    for (const ymd of kstWeekdays("20260717", to)) {
      rows.push({ siteCd: "00010", userCd, workYmd: ymd, workPlanCd: "00001" });
    }
  }
  const save = await call("POST", "/webApi/attd05/save-user-work-plans", { token: t, body: rows });
  console.log(`재배정: ${save.status} (요청 ${rows.length}건)`, save.text.slice(0, 200));
};
main().catch((e) => { console.error("실패:", e.message); process.exitCode = 1; });
