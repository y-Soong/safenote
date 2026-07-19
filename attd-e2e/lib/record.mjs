// 판정 기록기 — 케이스 단위 JSONL append + 스크린샷 파일명 규약.
// results/QE-results.jsonl 에 1케이스 1행. 분류: PASS | GUARD_OK | DEFECT | OBSERVED(현행동작 기록) | DEFERRED(이월) | BLOCKED
import { appendFileSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";

const resultsDir = fileURLToPath(new URL("../results", import.meta.url));
const shotsDir = fileURLToPath(new URL("../results/shots", import.meta.url));
mkdirSync(shotsDir, { recursive: true });

const RESULTS = `${resultsDir}/QE-results.jsonl`;

/**
 * 케이스 판정 기록.
 * @param {string} id       예: "QE-1-3"
 * @param {string} verdict  PASS|GUARD_OK|DEFECT|OBSERVED|DEFERRED|BLOCKED
 * @param {object} detail   { title, expected, actual, webView, appView, dbCheck, note }
 */
export function record(id, verdict, detail = {}) {
  const row = { id, verdict, at: new Date().toISOString(), ...detail };
  appendFileSync(RESULTS, JSON.stringify(row) + "\n", "utf8");
  console.log(`[${verdict}] ${id} ${detail.title ?? ""}`);
  return row;
}

// 스크린샷 경로 규약: results/shots/QE-1-3_web_after.png
export function shotPath(id, face, label) {
  return `${shotsDir}/${id}_${face}_${label}.png`;
}
