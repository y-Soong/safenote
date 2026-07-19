// 판정 기록 CLI — node e3/rec.mjs <id> <verdict> <title> <note...>
import { record } from "../lib/record.mjs";
const [, , id, verdict, title, ...note] = process.argv;
record(id, verdict, { title, note: note.join(" ") });
