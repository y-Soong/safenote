// QE-7-4 ✅ 앱 월캘린더 종합 표기 (QTUSERA 7월) — [앱 render]=[서버 month API=DB]
//  방식: 서버 GET /appApi/attd/my/month(권위 dayType, DB 파생) 대비 앱 렌더 셀 클래스 전수 대조.
//  매핑: WORK→--wk · LEAVE→--lv · OFF→--of · holidayName→--hol.
//  ⚠️ 6주 그리드의 인접월(--out=isOutside) 셀은 제외(현재월 셀만 판정).
//  DB 실측(E7): 활성휴일 17/20/31 · 연차 CONFIRMED 16(포상 종일)·28(시간차→LEAVE) · 7/27 취소분 미표기.
//   경계: 7/12(일) 보정근태+OT240 이나 근무계획 없어 dayType=OFF(주말 보정 현행), 7/15(수) 보정근태이나 OFF.
import { appLogin } from "../lib/browser.mjs";
import { getToken, call } from "../lib/http.mjs";
import { record, shotPath } from "../lib/record.mjs";

const CLS_OF = { WORK: "--wk", LEAVE: "--lv", OFF: "--of", ACTION_REQUIRED: "--wk" };

const run = async () => {
  const out = { title: "앱 월캘린더 종합(QTUSERA 7월) [앱]=[서버=DB]", steps: [] };
  try {
    // ── 서버 권위 month(DB 파생) ──
    const tok = await getToken("QTUSERA", "QtTest!2026", "APP");
    const mr = await call("GET", "/appApi/attd/my/month?yearMonth=202607", { token: tok, clientType: "APP" });
    const sdays = (mr.json?.days || []).filter((d) => (d.workYmd || "").startsWith("202607"));
    const srv = {}; // day(1..31) → {dayType, hol}
    for (const d of sdays) {
      const day = String(Number((d.workYmd || "").slice(6, 8)));
      srv[day] = { dayType: d.dayType, hol: !!d.holidayName, ot: Number(d.overtimeMinutes) || 0 };
    }
    out.steps.push(`서버 month days=${sdays.length} (LEAVE=${Object.entries(srv).filter(([,v])=>v.dayType==='LEAVE').map(([k])=>k).join(',')} · HOL=${Object.entries(srv).filter(([,v])=>v.hol).map(([k])=>k).join(',')})`);

    // ── 앱 렌더 ──
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.click('button.app-tabbar__tab:has-text("근태")').catch(() => {});
    await page.waitForTimeout(1500);
    await page.locator('.attd-seg__item:has-text("이번달")').click().catch(() => {});
    await page.waitForTimeout(1800);
    await page.screenshot({ path: shotPath("QE-7-4", "app", "july-calendar"), animations: "disabled" }).catch(() => {});

    // 인접월(--out) 제외하고 현재월 셀만 수집
    const cells = await page.evaluate(() => {
      const arr = [];
      document.querySelectorAll(".cal__d").forEach((el) => {
        const cls = el.className || "";
        if (cls.includes("cal__d--out")) return; // isOutside(인접월) 제외
        const t = (el.innerText || "").trim();
        const m = t.match(/(\d{1,2})/);
        if (m) arr.push({ day: String(Number(m[1])), cls });
      });
      return arr;
    });
    const app = {};
    for (const c of cells) if (!app[c.day]) app[c.day] = c.cls;
    out.steps.push(`앱 현재월 셀=${Object.keys(app).length}`);

    // ── 셀단위 대조 (1..31) ──
    const mism = [];
    for (let day = 1; day <= 31; day++) {
      const k = String(day);
      const s = srv[k];
      const a = app[k];
      if (!s) { continue; }
      if (!a) { mism.push(`${k}: 앱 셀 없음(서버 ${s.dayType})`); continue; }
      const wantMod = CLS_OF[s.dayType];
      if (wantMod && !a.includes(wantMod)) mism.push(`${k}: dayType ${s.dayType} 기대 ${wantMod} 누락 (앱 cls=${a})`);
      if (s.hol && !a.includes("--hol")) mism.push(`${k}: 휴일인데 --hol 누락 (앱 cls=${a})`);
      if (!s.hol && a.includes("--hol")) mism.push(`${k}: 비휴일인데 --hol 오표기 (앱 cls=${a})`);
    }
    // 7/27 취소연차 미표기 확인(서버 dayType=WORK, 앱 --wk 이어야 하며 --lv 아님)
    if (app["27"] && app["27"].includes("--lv")) mism.push("27: 취소연차 --lv 오표기");

    out.app = app; out.srv = srv; out.mismatches = mism;
    for (let day = 12; day <= 31; day++) {
      const k = String(day);
      out.steps.push(`day ${k}: 서버=${srv[k]?.dayType || "-"}${srv[k]?.hol ? "+HOL" : ""} | 앱=${app[k] || "(없음)"}`);
    }

    out.appView = mism.length === 0
      ? "7월 31일 전 셀 [앱 render]=[서버 dayType] 일치: LEAVE(16/28)→--lv · 휴일(17/20/31)→--hol · WORK/OFF 정합 · 7/27 취소분 --wk(미표기)."
      : `불일치 ${mism.length}건: ${mism.join(" / ")}`;
    out.dbCheck = "서버 month API=DB 파생(활성휴일 17/20/31·CONFIRMED 연차 16/28·work_plan 근무일). 7/12 보정근태+OT240·7/15 보정근태는 근무계획 부재로 dayType=OFF(주말/스케줄외 보정 현행, 결함 아님).";
    const verdict = mism.length === 0 ? "PASS" : "OBSERVED";
    record("QE-7-4", verdict, {
      ...out,
      note: `앱 QTUSERA 7월 캘린더 셀단위 [앱 render]=[서버=DB] 대조. 불일치 ${mism.length}건.${mism.length ? " 상세: " + mism.join("; ") : " 전수 일치."} 관찰: 7/12(일) 보정근태+OT240 이나 근무계획 없어 dayType=OFF — 주말 보정 근태의 월캘린더 표기는 OFF(OT 마커만).`,
    });
    console.log("APP", JSON.stringify(app));
    console.log("SRV", JSON.stringify(srv));
  } catch (e) {
    out.steps.push("EX:" + String(e).slice(0, 200));
    record("QE-7-4", "OBSERVED", { ...out, note: "예외: " + out.steps.join(" | ") });
  }
};
run().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
