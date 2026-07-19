// G 약관동의 게이트 해소 — POST /appApi/terms01/agree-required-terms
import { call, getToken } from "../lib/http.mjs";

const main = async () => {
  const token = await getToken("QTUSERG", "QtTest!2026", "APP");
  const r = await call("POST", "/appApi/terms01/agree-required-terms", { token, clientType: "APP" });
  console.log("agree:", r.status, r.text.slice(0, 200));
};
main();
