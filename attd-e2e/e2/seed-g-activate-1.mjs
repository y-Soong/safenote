// G(QTUSERG) 활성화 1단계 — 초기비번 로그인 + SMS 발송. 임시토큰을 run 파일로 저장.
import { call } from "../lib/http.mjs";
import { writeFileSync } from "node:fs";

const main = async () => {
  const r = await call("POST", "/comApi/login/login", {
    body: { userId: "QTUSERG", userPw: "01077770007" }, clientType: "APP",
  });
  console.log("login status:", r.status, "nextStep:", r.json?.nextStep, "mustChangePassword:", r.json?.mustChangePassword);
  if (!r.json?.token) { console.log(r.text.slice(0, 300)); process.exit(1); }
  writeFileSync(new URL("./g-temp-token.json", import.meta.url), JSON.stringify({ token: r.json.token }), "utf8");
  const s = await call("POST", "/comApi/baseinfo/sms-auth-sends", {
    token: r.json.token, clientType: "APP",
    body: { cmpnyCd: "001", mblNo: "01077770007" },
  });
  console.log("sms-send:", s.status, s.text.slice(0, 200));
};
main();
