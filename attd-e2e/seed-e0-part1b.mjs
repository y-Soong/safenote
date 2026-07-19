// E0 시드 1부-b — QTUSERH/I 생성 재시도(birthDt 필수 확인분 반영) + SMS 발송.
import { call, login } from "./lib/http.mjs";
import { writeFileSync } from "node:fs";

const main = async () => {
  const hr = await login("QTHR", "QtTest!2026", "WEB");
  const t = hr.json.token;

  for (const [id, mbl, birth] of [["QTUSERH", "010-7777-0021", "900101"], ["QTUSERI", "010-7777-0022", "910101"]]) {
    const r = await call("POST", "/webApi/user01/insert-user-info", {
      token: t,
      body: {
        userId: id, userNm: `QT사원${id.slice(-1)}`, authCd: "99999",
        siteNo: "QT001", nodeCd: "n1", mblNo: mbl, birthDt: birth, gender: "M",
        hireDate: "20250101", employmentType: "REGULAR", defaultSchCd: "00001",
      },
    });
    console.log(`${id} 생성: ${r.status}`, r.text.slice(0, 200));
  }

  const gates = {};
  for (const [id, mbl] of [["QTUSERH", "01077770021"], ["QTUSERI", "01077770022"]]) {
    const r = await login(id, mbl, "WEB");
    console.log(`${id} 초기 로그인: ${r.status} nextStep=${r.json?.nextStep}`);
    gates[id] = { tempToken: r.json?.token, nextStep: r.json?.nextStep, mbl };
    if (r.json?.nextStep === "PHONE_AUTH") {
      const s = await call("POST", "/comApi/baseinfo/sms-auth-sends", { token: r.json.token, body: { mblNo: mbl } });
      console.log(`${id} sms-auth-sends: ${s.status}`, s.text.slice(0, 120));
    }
  }
  writeFileSync(new URL("./run/e0-gates.json", import.meta.url), JSON.stringify(gates, null, 2));
};
main().catch((e) => { console.error("실패:", e.message); process.exitCode = 1; });
