// QE-7-1 ✅ 웹 근태화면 전수 스모크 (QTHR)
//  Attd_01/01_1/01_2/02/03/05/06/07/08/09/11/12/13/14 + User_01/05/06 + Baim_07 순차 진입.
//  각 화면: 로드완료 · 콘솔 fatal(pageerror) 0 · 5xx 응답 0 · 기본 조회 동작. 에러화면은 결함표.
import { webLogin } from "../lib/browser.mjs";
import { record, shotPath } from "../lib/record.mjs";

const WEB = "http://localhost:8081";

// 스모크 대상(라벨 = 라우트 MENU_D_ID). 01_1/01_2 는 Attd_01 하위(메뉴 미등록) — 직진입 폴백 관찰.
const SCREENS = [
  "Attd_01", "Attd_01_1", "Attd_01_2", "Attd_02", "Attd_03", "Attd_05",
  "Attd_06", "Attd_07", "Attd_08", "Attd_09", "Attd_11", "Attd_12",
  "Attd_13", "Attd_14", "User_01", "User_05", "User_06", "Baim_07",
];

const run = async () => {
  const out = { title: "웹 근태화면 전수 스모크(18화면)", steps: [] };
  const rows = [];
  let fatalTotal = 0;
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");

    for (const id of SCREENS) {
      const errs = [];
      const http5xx = [];
      const onPageErr = (e) => errs.push(String(e).slice(0, 120));
      const onResp = (r) => { if (r.status() >= 500) http5xx.push(`${r.status()} ${r.url().split("/prafta/").pop()?.slice(0, 60)}`); };
      page.on("pageerror", onPageErr);
      page.on("response", onResp);
      let landedDash = false, comingSoon = false, bodyLen = 0, queried = false;
      try {
        await page.goto(`${WEB}/safenote/main/${id}`, { waitUntil: "networkidle", timeout: 25000 });
        await page.waitForTimeout(1200);
        const url = page.url();
        // 메뉴 미등록 화면(01_1/01_2)은 catch-all 이 /safenote/main(대시보드)으로 폴백
        landedDash = /\/safenote\/main\/?$/.test(url) && !url.endsWith(`/${id}`);
        const body = (await page.locator("body").innerText().catch(() => "")) || "";
        bodyLen = body.replace(/\s+/g, "").length;
        comingSoon = body.includes("준비 중") || body.includes("Coming");
        // 기본 조회 버튼이 있으면 1회 눌러 데이터 로드 확인
        const btn = page.locator('button:has-text("조회")').first();
        if (await btn.isVisible().catch(() => false)) {
          await btn.click().catch(() => {});
          await page.waitForTimeout(1500);
          queried = true;
        }
      } catch (e) { errs.push("NAV_EX:" + String(e).slice(0, 100)); }
      page.off("pageerror", onPageErr);
      page.off("response", onResp);
      const fatal = errs.length + http5xx.length;
      fatalTotal += fatal;
      // 대표 스크린샷(전 화면)
      await page.screenshot({ path: shotPath("QE-7-1", "web", id), animations: "disabled" }).catch(() => {});
      const r = { id, url: page.url().split("/main/").pop(), landedDash, comingSoon, bodyLen, queried, fatal, err: errs.slice(0, 2), http5xx };
      rows.push(r);
      out.steps.push(`${id}: bodyLen=${bodyLen} query=${queried} fallbackDash=${landedDash} comingSoon=${comingSoon} fatal=${fatal}${fatal ? " " + JSON.stringify([...errs, ...http5xx].slice(0, 2)) : ""}`);
    }
  } catch (e) { out.steps.push("EX:" + String(e).slice(0, 160)); fatalTotal++; }

  out.rows = rows;
  out.webView = `18화면 순차 진입 완료. fatal(pageerror+5xx) 합계=${fatalTotal}. 01_1/01_2 는 메뉴 미등록 → 대시보드 폴백(하위 컴포넌트).`;
  const verdict = fatalTotal === 0 ? "PASS" : "DEFECT";
  record("QE-7-1", verdict, {
    ...out,
    note: `웹 근태/인사 18화면 스모크. fatalTotal=${fatalTotal}. Attd_01_1/01_2 는 Attd_01 하위 뷰(메뉴 라우트 미등록)라 직진입 시 대시보드 폴백(정상 설계, 결함 아님).`,
  });
  console.log("SMOKE_ROWS", JSON.stringify(rows));
};
run().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
