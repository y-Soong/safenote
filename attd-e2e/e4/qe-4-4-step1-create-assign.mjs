// QE-4-4 step1 — 소모용 근무타입 QE9H 생성(적용일 2026-07-01, 09:00~19:00, 휴게0)
//               + H(20260700036) 7/16 배정(API 시드 — Attd_05 과거일 UI 배정은 무반응 skip 실측 E3)
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openAttd01, openSchCreate, setPopupApplyDate, setPopupTime, savePopupAndCollect, readSchRow } from "./lib-sch.mjs";
import { getToken, call } from "../lib/http.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await openAttd01(page);
    if (await readSchRow(page, "QE9H")) {
      console.log("QE9H 이미 존재 — 생성 스킵");
    } else {
      await openSchCreate(page);
      await page.fill('.modal-content-sch-info input[placeholder="예: ST008"]', "QE9H");
      await setPopupApplyDate(page, "2026-07-01");
      await setPopupTime(page, 0, "09", "00");
      await setPopupTime(page, 1, "19", "00");
      await page.screenshot({ path: shotPath("QE-4-4", "web", "create-popup") });
      const { modalTexts } = await savePopupAndCollect(page);
      console.log("생성 모달:", JSON.stringify(modalTexts));
      await page.click('button:has-text("조회")');
      await page.waitForTimeout(2000);
      console.log("QE9H 행:", await readSchRow(page, "QE9H"));
    }
    // 신규 SCH_CD 확인은 API 목록으로
    const token = await getToken("QTHR", "QtTest!2026", "WEB");
    const list = await call("GET", "/webApi/attd01/sch-info-lists?siteCd=00010&schType=01&useYn=Y", { token });
    const qe9h = (list.json?.schInfoResultList ?? []).find((s) => s.schNo === "QE9H");
    console.log("QE9H 메타:", JSON.stringify(qe9h));
    if (!qe9h) throw new Error("QE9H 조회 실패");
    // H 7/16 배정 (시드 API)
    const r = await call("POST", "/webApi/attd05/save-user-work-plans", {
      token,
      body: [{ siteCd: "00010", userCd: "20260700036", workYmd: "20260716", workPlanCd: qe9h.schCd }],
    });
    console.log("7/16 배정 응답:", r.status, r.text.slice(0, 200));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
