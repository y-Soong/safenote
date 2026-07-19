// 커맨드라인 판정 기록기 — node e1/rec.mjs <id> <verdict> <json-file|json-string>
import { record } from "../lib/record.mjs";
import { readFileSync, existsSync } from "node:fs";

const [, , id, verdict, arg] = process.argv;
let detail = {};
if (arg) {
  detail = existsSync(arg) ? JSON.parse(readFileSync(arg, "utf8")) : JSON.parse(arg);
}
record(id, verdict, detail);
