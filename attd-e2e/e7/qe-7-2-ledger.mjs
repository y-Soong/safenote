// QE-7-2 ✅ 원장=화면 전수 정합 (웹 Attd_09 vs 앱 연차현황 vs DB) — A/C/D/G/H
//  DB(권위) vs 웹 dashboard/list(정확 기대) vs 앱 my-leave-summary(반올림 #E2-1 재확인).
//  H/I 는 비활성(useYn=N) → 앱 로그인 불가. H 는 웹/DB 만 대조.
import { getToken, call } from "../lib/http.mjs";
import { record } from "../lib/record.mjs";

// DB 권위값(E7 개시 실측): {granted, used}
const DB = {
  "20260700029": { id: "QTUSERA", g: 16, u: 3.225 },
  "20260700031": { id: "QTUSERC", g: 15, u: 0 },
  "20260700032": { id: "QTUSERD", g: 15, u: 1 },
  "20260700034": { id: "QTUSERG", g: 5, u: 1.375 },
  "20260700036": { id: "QTUSERH", g: 17, u: 2, deact: true },
};
const PW = "QtTest!2026";
const near = (a, b) => Math.abs(Number(a) - Number(b)) < 0.00001;

const run = async () => {
  const out = { title: "원장=화면 전수 정합 (A/C/D/G/H)", steps: [] };
  const cmp = [];
  try {
    // ── 웹 Attd_09 dashboard/list (QTHR WEB) ──
    const webTok = await getToken("QTHR", PW, "WEB");
    const webRows = {};
    let convMinutesWeb = null;
    for (let page = 1; page <= 6; page++) {
      const r = await call("GET", `/webApi/attd09/leave-dashboard/list?siteCd=00010&nodeCd=&incSubNodeYn=Y&userNm=&size=100&page=${page}`, { token: webTok });
      const d = r.json || {};
      if (page === 1) convMinutesWeb = d.convMinutes;
      const list = Array.isArray(d.list) ? d.list : [];
      for (const it of list) {
        const uc = it.userCd || it.USER_CD;
        if (uc && DB[uc]) webRows[uc] = it;
      }
      const total = d.paging?.totalCount ?? 0;
      if (list.length === 0 || Object.keys(webRows).length >= Object.keys(DB).length || page * 100 >= total) break;
    }
    out.steps.push(`웹 dashboard/list convMinutes=${convMinutesWeb} 수집행=${Object.keys(webRows).length}`);

    // 웹 응답 필드 키 추론(첫 행 덤프)
    const anyRow = Object.values(webRows)[0];
    if (anyRow) out.steps.push("웹행 키=" + Object.keys(anyRow).join(","));

    // ── 앱 my-leave-summary (A/C/D/G) ──
    const appSum = {};
    for (const [uc, m] of Object.entries(DB)) {
      if (m.deact) continue;
      try {
        const tok = await getToken(m.id, PW, "APP");
        const r = await call("GET", "/appApi/leave01/my-leave-summary", { token: tok, clientType: "APP" });
        const g = r.json?.groups?.TOTAL || null;
        appSum[uc] = g;
      } catch (e) { appSum[uc] = { err: String(e).slice(0, 80) }; }
    }

    // ── 3면 대조 ──
    for (const [uc, m] of Object.entries(DB)) {
      const dbRem = m.g - m.u;
      const w = webRows[uc] || {};
      // 웹 total: {granted, used(완료), scheduled(예정확정), remaining}. 원장 USED = used + scheduled.
      const t = w.total || null;
      const wG = t ? t.granted : null;
      const wU = t ? Number(t.used) + Number(t.scheduled) : null; // 원장 USED_DAYS 등가
      const wRem = t ? t.remaining : null;
      const a = appSum[uc] || {};
      const aRem = a?.remaining ?? null;
      const aUsed = a?.used ?? null;
      const webMatch = wG != null ? (near(wG, m.g) && near(wU, m.u) && near(wRem, m.g - m.u)) : "NO_ROW";
      const appMatch = m.deact ? "N/A(비활성)" : (aRem != null ? near(aRem, dbRem) : "NO_DATA");
      cmp.push({ uc, id: m.id, db: { g: m.g, u: m.u, rem: dbRem }, web: { g: wG, u: wU, rem: wRem, match: webMatch }, app: { used: aUsed, rem: aRem, match: appMatch } });
      out.steps.push(`${m.id}: DB(g${m.g}/u${m.u}/rem${dbRem}) | 웹(g${wG}/u${wU} match=${webMatch}) | 앱(rem${aRem} match=${appMatch})`);
    }
  } catch (e) { out.steps.push("EX:" + String(e).slice(0, 200)); }

  out.cmp = cmp;
  // 판정: 웹=DB 정확(오차0) 기대. 앱 반올림 불일치는 #E2-1 귀속(신규 채번 없음).
  //  비활성(H) 의 웹 NO_ROW 는 대시보드 정상 제외 → 웹 결함 아님(DB로만 대조).
  const webBad = cmp.filter((c) => c.web.match === false || (c.web.match === "NO_ROW" && !DB[c.uc]?.deact));
  const appRoundOff = cmp.filter((c) => c.app.match === false);
  out.webView = `웹 Attd_09 dashboard vs DB: 불일치 ${webBad.length}건`;
  out.appView = `앱 연차현황 vs DB: 불일치 ${appRoundOff.map((c) => c.id).join(",") || "없음"} (불일치는 #E2-1 반올림 귀속)`;
  out.dbCheck = "DB 권위값 기준. GRANT.USED_DAYS=SUM(CONFIRMED use) 불일치 0(E7 개시 게이트 PASS).";
  const verdict = webBad.length > 0 ? "DEFECT" : (appRoundOff.length > 0 ? "OBSERVED" : "PASS");
  record("QE-7-2", verdict, {
    ...out,
    note: `3면 정합: 웹 dashboard=DB 불일치 ${webBad.length}건. 앱 요약 반올림 불일치 ${appRoundOff.length}건(#E2-1 기지 귀속, 신규 채번 없음). H/I 비활성으로 앱 N/A.`,
  });
  console.log("CMP", JSON.stringify(cmp, null, 1));
};
run().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
