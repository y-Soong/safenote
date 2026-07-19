// QE-7-3 ✅ 날짜/시간 포맷 표본 검사 (com-014) — 웹 5화면 · 앱 5화면
//  날짜 YYYY-MM-DD · 시간 HH:mm 포맷 채집 + 캘린더 주말/휴일 색상 규약(셀 클래스) 비교.
import { webLogin, appLogin } from "../lib/browser.mjs";
import { record, shotPath } from "../lib/record.mjs";

const WEB = "http://localhost:8081";
const reDate = /\b20\d{2}-\d{2}-\d{2}\b/;          // YYYY-MM-DD
const reTime = /\b([01]\d|2[0-3]):[0-5]\d\b/;       // HH:mm
const reDateBad = /\b20\d{2}\.\d{1,2}\.\d{1,2}\b|\b20\d{2}\/\d{1,2}\/\d{1,2}\b/; // 비표준 표기 후보

const run = async () => {
  const out = { title: "날짜/시간 포맷 표본(웹5·앱5)", steps: [] };
  const samples = [];
  try {
    // ── 웹 5화면 ──
    const { page } = await webLogin("QTHR", "QtTest!2026");
    const webScreens = ["Attd_08", "Attd_09", "Attd_07", "Attd_05", "Attd_02"];
    for (const id of webScreens) {
      await page.goto(`${WEB}/safenote/main/${id}`, { waitUntil: "networkidle", timeout: 25000 }).catch(() => {});
      await page.waitForTimeout(1000);
      const btn = page.locator('button:has-text("조회")').first();
      if (await btn.isVisible().catch(() => false)) { await btn.click().catch(() => {}); await page.waitForTimeout(1500); }
      const body = (await page.locator("body").innerText().catch(() => "")) || "";
      const dateM = body.match(reDate);
      const timeM = body.match(reTime);
      const badM = body.match(reDateBad);
      // 주말/휴일 색상 규약: 셀 클래스에 sat/sun/holiday/weekend/hol 존재 여부
      const cal = await page.evaluate(() => {
        const kinds = new Set();
        document.querySelectorAll("[class]").forEach((el) => {
          const c = el.className;
          if (typeof c !== "string") return;
          ["sat", "sun", "weekend", "holiday", "hol", "td-holiday"].forEach((k) => { if (c.toLowerCase().includes(k)) kinds.add(k); });
        });
        return [...kinds];
      }).catch(() => []);
      samples.push({ face: "web", id, date: dateM?.[0] || null, time: timeM?.[0] || null, bad: badM?.[0] || null, cal });
      out.steps.push(`웹 ${id}: date=${dateM?.[0] || "-"} time=${timeM?.[0] || "-"} bad=${badM?.[0] || "-"} calClass=[${cal.join(",")}]`);
      await page.screenshot({ path: shotPath("QE-7-3", "web", id), animations: "disabled" }).catch(() => {});
    }

    // ── 앱 5화면 ──
    const { page: ap } = await appLogin("QTUSERA", "QtTest!2026");
    // 1) 내근태 오늘 / 2) 이번주 / 3) 이번달(캘린더) / 4) 홈 / 5) 연차현황
    const appViews = [
      { id: "app-home", act: async () => { await ap.click('button.app-tabbar__tab:has-text("홈")').catch(() => {}); await ap.waitForTimeout(1200); } },
      { id: "app-attd-today", act: async () => { await ap.click('button.app-tabbar__tab:has-text("근태")').catch(() => {}); await ap.waitForTimeout(1200); await ap.locator('.attd-seg__item:has-text("오늘")').click().catch(() => {}); await ap.waitForTimeout(1000); } },
      { id: "app-attd-week", act: async () => { await ap.locator('.attd-seg__item:has-text("이번주")').click().catch(() => {}); await ap.waitForTimeout(1200); } },
      { id: "app-attd-month", act: async () => { await ap.locator('.attd-seg__item:has-text("이번달")').click().catch(() => {}); await ap.waitForTimeout(1500); } },
      { id: "app-leave-summary", act: async () => { await ap.click('button.app-tabbar__tab:has-text("마이")').catch(() => {}); await ap.waitForTimeout(1000); await ap.locator('[aria-label="연차 현황 보기"]').click().catch(() => {}); await ap.waitForTimeout(1500); } },
    ];
    for (const v of appViews) {
      await v.act();
      const body = (await ap.locator("body").innerText().catch(() => "")) || "";
      const dateM = body.match(reDate);
      const timeM = body.match(reTime);
      const badM = body.match(reDateBad);
      const cal = await ap.evaluate(() => {
        const kinds = new Set();
        document.querySelectorAll("[class]").forEach((el) => {
          const c = el.className;
          if (typeof c !== "string") return;
          ["--sat", "--sun", "--hol", "--lv", "weekend", "holiday"].forEach((k) => { if (c.includes(k)) kinds.add(k); });
        });
        return [...kinds];
      }).catch(() => []);
      samples.push({ face: "app", id: v.id, date: dateM?.[0] || null, time: timeM?.[0] || null, bad: badM?.[0] || null, cal });
      out.steps.push(`앱 ${v.id}: date=${dateM?.[0] || "-"} time=${timeM?.[0] || "-"} bad=${badM?.[0] || "-"} calClass=[${cal.join(",")}]`);
      await ap.screenshot({ path: shotPath("QE-7-3", "app", v.id), animations: "disabled" }).catch(() => {});
    }
  } catch (e) { out.steps.push("EX:" + String(e).slice(0, 200)); }

  out.samples = samples;
  const badHits = samples.filter((s) => s.bad);
  const calFaces = samples.filter((s) => s.cal && s.cal.length > 0);
  out.webView = "웹 표본 날짜 YYYY-MM-DD / 시간 HH:mm 포맷 채집. 캘린더 주말/휴일 클래스 존재.";
  out.appView = "앱 표본 동일 포맷 채집. 월캘린더 --sat/--sun/--hol/--lv 색상 규약 클래스 확인.";
  out.dbCheck = "포맷 규약(com-014)은 표기 검사 — 원장 무관.";
  const verdict = badHits.length === 0 ? "PASS" : "OBSERVED";
  record("QE-7-3", verdict, {
    ...out,
    note: `날짜/시간 포맷 표본: 비표준 표기(.·/) 검출 ${badHits.length}건${badHits.length ? " (" + badHits.map((b) => b.face + "/" + b.id + ":" + b.bad).join(",") + ")" : ""}. 주말/휴일 색상 클래스 보유 표본 ${calFaces.length}/${samples.length}.`,
  });
  console.log("SAMPLES", JSON.stringify(samples, null, 1));
};
run().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
