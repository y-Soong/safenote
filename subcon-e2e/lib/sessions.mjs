// 세션 로더 — run/<role>.session.json 3개를 읽어 { A, B, C } 로 반환.
import { readFileSync } from "node:fs";

export function loadSessions() {
  const read = (role) => JSON.parse(readFileSync(new URL(`../run/${role}.session.json`, import.meta.url), "utf8"));
  return { A: read("A_PRIME"), B: read("B_SUB"), C: read("C_SUBSUB") };
}
