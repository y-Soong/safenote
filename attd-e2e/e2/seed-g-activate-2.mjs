// G 활성화 2단계 — node e2/seed-g-activate-2.mjs <인증코드>
// verify-phone-auth → default-sch-options → set-default-sch(00001) → PUT password → 재로그인 확인.
import { call } from "../lib/http.mjs";
import { readFileSync } from "node:fs";

const code = process.argv[2];
const main = async () => {
  const { token } = JSON.parse(readFileSync(new URL("./g-temp-token.json", import.meta.url), "utf8"));
  const v = await call("POST", "/comApi/login/verify-phone-auth", {
    token, clientType: "APP", body: { mblNo: "01077770007", certNo: code },
  });
  console.log("verify:", v.status, "nextStep:", v.json?.nextStep);
  if (v.status !== 200) { console.log(v.text.slice(0, 300)); process.exit(1); }
  let tk = v.json.token || token;

  if (v.json?.nextStep === "DEFAULT_SCH") {
    const opts = await call("GET", "/comApi/login/default-sch-options", { token: tk, clientType: "APP" });
    console.log("options:", opts.status, JSON.stringify(opts.json?.schedules?.map((s) => s.schCd ?? s)).slice(0, 300));
    const sch = await call("POST", "/comApi/login/set-default-sch", {
      token: tk, clientType: "APP", body: { defaultSchCd: "00001" },
    });
    console.log("set-default-sch:", sch.status, "nextStep:", sch.json?.nextStep, "mustChangePassword:", sch.json?.mustChangePassword);
    if (sch.json?.token) tk = sch.json.token;
  }

  const pw = await call("PUT", "/appApi/mypage/password", {
    token: tk, clientType: "APP", body: { currentPassword: "01077770007", newPassword: "QtTest!2026" },
  });
  console.log("passwd:", pw.status, pw.text.slice(0, 200));

  const re = await call("POST", "/comApi/login/login", {
    body: { userId: "QTUSERG", userPw: "QtTest!2026" }, clientType: "APP",
  });
  console.log("relogin:", re.status, "nextStep:", re.json?.nextStep, "siteCd:", re.json?.siteCd);
};
main();
