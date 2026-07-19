// E4 판정 기록 CLI — node e4/rec.mjs <id> <verdict> <json detail>
import { record } from "../lib/record.mjs";
const [, , id, verdict, detailJson] = process.argv;
record(id, verdict, detailJson ? JSON.parse(detailJson) : {});
