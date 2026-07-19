// E0 시드 1부 — 근무계획 배정(A/C/G/D) + policy-grant(A/C/D/G) + QTUSERH/I 생성 + SMS 발송까지.
// (SMS 코드는 DB에서 읽어야 하므로 2부에서 인증 완결)
import { call, login } from "./lib/http.mjs";
import { writeFileSync } from "node:fs";

const PW = "QtTest!2026";
const USERS = { A: "20260700029", C: "20260700031", D: "20260700032", G: "20260700034" };

// 평일(월~금) 나열 — 휴일 포함(시스템 DEFAULT_SCH 동작 미러).
function weekdays(fromYmd, toYmd) {
  const out = [];
  const d = new Date(`${fromYmd.slice(0, 4)}-${fromYmd.slice(4, 6)}-${fromYmd.slice(6, 8)}T00:00:00+09:00`);
  const end = new Date(`${toYmd.slice(0, 4)}-${toYmd.slice(4, 6)}-${toYmd.slice(6, 8)}T00:00:00+09:00`);
  while (d <= end) {
    const dow = d.getDay();
    if (dow >= 1 && dow <= 5) {
      out.push(d.toISOString().slice(0, 10).replace(/-/g, ""));
    }
    d.setDate(d.getDate() + 1);
  }
  return out;
}

const main = async () => {
  const hr = await login("QTHR", PW, "WEB");
  if (hr.status !== 200) throw new Error(`QTHR 로그인 실패: ${hr.status}`);
  const t = hr.json.token;
  console.log("QTHR 로그인 OK");

  // 1) 근무계획 배정: A/C/G = 7/17~8/31, D = 7/17~7/31 (전부 QT8H=00001)
  const rows = [];
  for (const [k, userCd] of Object.entries(USERS)) {
    const to = k === "D" ? "20260731" : "20260831";
    for (const ymd of weekdays("20260717", to)) {
      rows.push({ siteCd: "00010", userCd, workYmd: ymd, workPlanCd: "00001" });
    }
  }
  const save = await call("POST", "/webApi/attd05/save-user-work-plans", { token: t, body: rows });
  console.log(`근무계획 배정: ${save.status} (요청 ${rows.length}건)`, save.text.slice(0, 200));

  // 2) 연차 부여: preview → apply
  const prev = await call("POST", "/webApi/attd09/leave-grant/policy-grant/preview", { token: t, body: { userCds: Object.values(USERS) } });
  console.log("policy-grant preview:", prev.status, prev.text.slice(0, 400));
  const grant = await call("POST", "/webApi/attd09/leave-grant/policy-grant", { token: t, body: { userCds: Object.values(USERS) } });
  console.log("policy-grant apply:", grant.status, grant.text.slice(0, 400));

  // 3) QTUSERH / QTUSERI 생성 (hireDate=20250101 — 첫해 월차 함정 회피)
  for (const [id, mbl] of [["QTUSERH", "010-7777-0007"], ["QTUSERI", "010-7777-0008"]]) {
    const r = await call("POST", "/webApi/user01/insert-user-info", {
      token: t,
      body: {
        userId: id, userNm: `QT사원${id.slice(-1)}`, authCd: "99999",
        siteNo: "QT001", nodeCd: "n1", mblNo: mbl,
        hireDate: "20250101", employmentType: "REGULAR", defaultSchCd: "00001",
      },
    });
    console.log(`${id} 생성: ${r.status}`, r.text.slice(0, 200));
  }

  // 4) H/I 초기 로그인(초기비번=휴대폰 숫자만) → nextStep 확인 + SMS 발송
  const gates = {};
  for (const [id, mbl] of [["QTUSERH", "01077770007"], ["QTUSERI", "01077770008"]]) {
    const r = await login(id, mbl, "WEB");
    console.log(`${id} 초기 로그인: ${r.status} nextStep=${r.json?.nextStep} mustChange=${r.json?.mustChangePassword}`);
    gates[id] = { tempToken: r.json?.token, nextStep: r.json?.nextStep, mbl };
    if (r.json?.nextStep === "PHONE_AUTH") {
      const s = await call("POST", "/comApi/baseinfo/sms-auth-sends", { token: r.json.token, body: { mblNo: mbl } });
      console.log(`${id} sms-auth-sends: ${s.status}`, s.text.slice(0, 120));
    }
  }
  writeFileSync(new URL("./run/e0-gates.json", import.meta.url), JSON.stringify(gates, null, 2));
  console.log("E0 1부 완료 — SMS 코드 확인 후 2부 진행");
};
main().catch((e) => { console.error("E0 1부 실패:", e.message); process.exitCode = 1; });
