// 범용 휴일 등록 — node e3/register-holiday.mjs <y> <m> <d> <name> <caseId>
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openAttd02, registerHoliday, bodyText } from "./lib-holiday.mjs";

const [, , y, m, d, name, caseId] = process.argv;

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await openAttd02(page);
    const r = await registerHoliday(page, Number(y), Number(m), Number(d), name);
    console.log("등록 중 dialog:", JSON.stringify(r.dialogs));
    const after = await bodyText(page);
    const idx = after.indexOf(name);
    console.log("등록 후 캘린더:", idx >= 0 ? after.slice(Math.max(0, idx - 40), idx + 60).replace(/\n/g, " | ") : "미노출!");
    await page.screenshot({ path: shotPath(caseId, "web", `attd02-registered-${m}-${d}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
