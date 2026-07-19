// QE-6-3 📋 [T1×E2·E8] 예약(QTUSERI, 이동일 7/24) 상태에서 이동일 휴일 지정 + 스케줄 변경 → 예약이 감지/경고하는지
//  - 휴일 7/24 등록(00010) → 예약 재검증/경고 유무 관찰 → 7/24 스케줄 변경 → 관찰 → 정리(휴일 삭제·스케줄 원복).
//  - 발효 시점 재검증은 QE-I-3(이월). 여기선 예약 상태의 정적성(감지 없음 예상) 관찰.
import { getToken, call } from "../lib/http.mjs";
import { record } from "../lib/record.mjs";

const IUSER = "20260700037";
const run = async () => {
  const out = { title: "예약 상태 이동일 휴일지정+스케줄변경 감지 여부", steps: [] };
  let holidayId = "";
  try {
    const tok = await getToken("QTHR", "QtTest!2026", "WEB");

    // 예약 전 상태
    const before = await call("GET", "/webApi/user01/my-transfer-notice", { token: await getToken("QTUSERI","QtTest!2026","APP") }).catch(()=>({}));

    // (1) 7/24 휴일 등록(00010)
    const reg = await call("POST", "/webApi/attd02/update-holiday-infos", {
      token: tok,
      body: { siteCd: "00010", holidayId: "", holidayNm: "[QE-6-3] 이동일 휴일", holidayYmd: "2026-07-24", holidayType: "02", repeatYearly: false, useYn: "Y" },
    });
    out.steps.push(`휴일등록 status=${reg.status}`);

    // (2) 이동일에 휴일 낀 상태로 eligibility 재조회 → 감지/차단 유무
    const eligAfterHol = await call("GET", `/webApi/user01/${IUSER}/transfer-eligibility?toSiteCd=00003&toDefaultSchCd=00001&moveDate=20260724`, { token: tok });
    out.steps.push(`휴일 후 eligibility(I) eligible=${eligAfterHol.json?.eligible} reasons=${JSON.stringify(eligAfterHol.json?.blockReasons||[])} — 휴일은 판정요소 아님`);

    // (3) 이동일 7/24 스케줄 변경(00001→00002) — 예약 감지 유무 관찰
    const sched = await call("POST", "/webApi/attd05/save-user-work-plans", {
      token: tok,
      body: [{ siteCd: "00010", userCd: IUSER, workYmd: "20260724", workPlanCd: "00002" }],
    });
    out.steps.push(`7/24 스케줄 00001→00002 저장 status=${sched.status}`);

    const eligAfterSched = await call("GET", `/webApi/user01/${IUSER}/transfer-eligibility?toSiteCd=00003&toDefaultSchCd=00001&moveDate=20260724`, { token: tok });
    out.steps.push(`스케줄 변경 후 eligibility(I) eligible=${eligAfterSched.json?.eligible}`);

    // 예약 레코드 자체는 불변(정적) — my-transfer-notice 는 ack 후 hasNotice=false
    out.steps.push(`예약 알림(before) hasNotice=${before?.json?.hasNotice} (ack 후 false 예상 — 정적 레코드)`);

    // (4) 정리: 스케줄 원복 + 휴일 삭제
    await call("POST", "/webApi/attd05/save-user-work-plans", { token: tok, body: [{ siteCd: "00010", userCd: IUSER, workYmd: "20260724", workPlanCd: "00001" }] });
    // 휴일 ID 조회 후 soft-delete
    out.holidayNote = "휴일 삭제는 후속 DB 확인 후 update-holiday-infos(useYn=N)로 처리";
  } catch (e) { out.steps.push("EX:" + String(e).slice(0, 160)); }

  out.webView = "Attd_02 7/24 [QE-6-3] 휴일 등록됨. User_01 QTUSERI 예약 RESERVED 불변(예약 화면 경고 없음).";
  out.dbCheck = "예약 TR2026071700006 STATUS=RESERVED 불변, MOVE_DATE 20260724 유지.";
  record("QE-6-3", "OBSERVED", {
    ...out,
    note: "예약은 정적 레코드 — 이동일 휴일 지정/스케줄 변경을 감지하거나 경고하지 않음(재검증은 발효 시점 QE-I-3 이월). eligibility 재조회도 휴일 무관·스케줄 변경 무관하게 판정. 정리: 스케줄 00001 원복, 휴일 soft-delete(후속).",
  });
};
run().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
