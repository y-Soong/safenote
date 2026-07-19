// 세션 토큰 갱신 — 해당 role 을 새 비번으로 재로그인해 run/<role>.session.json 의 token 을 최신화.
// UI(Playwright) 로그인이 단일 활성세션 정책으로 기존 API 토큰을 무효화한 뒤 사용.
// 사용: node refresh-session.mjs <ROLE>
import { readFileSync, writeFileSync } from "node:fs";
import { login } from "./lib/http.mjs";

const role = process.argv[2];
if (!role) { console.error("ROLE 인자 필요"); process.exit(1); }

const main = async () => {
  const companies = JSON.parse(readFileSync(new URL("./run/companies.json", import.meta.url), "utf8"));
  const c = companies.find((x) => x.role === role);
  const url = new URL(`./run/${role}.session.json`, import.meta.url);
  const sess = JSON.parse(readFileSync(url, "utf8"));

  const re = await login(c.adminId, sess.newPw || c.initialPw);
  if (!re.ok || !re.json?.token) { console.error("재로그인 실패:", re.status, re.text); process.exit(1); }

  writeFileSync(url, JSON.stringify({ ...sess, token: re.json.token, nextStep: re.json.nextStep }, null, 2), "utf8");
  console.log(`[${role}] 토큰 갱신 완료 (nextStep=${re.json.nextStep})`);
};

main().catch((e) => { console.error(e); process.exit(1); });
