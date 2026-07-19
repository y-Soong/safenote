// E4 공용 — 근무타입 관리(Attd_01) 목록/편집 팝업(SchInfoPop) 조작 헬퍼.
// 실소스: web src/views/attd/Attd_01_1.vue + popup/SchInfoPop.vue (2026-07-17 정독)
// - 목록 행 더블클릭 → SchInfoPop(.modal-content-sch-info)
// - 적용일: CalendarSrch(input.calendar-input, flatpickr) — 수정모드는 "오늘 이후"만 허용(FE 가드)
// - 시각: TimeInput(button.time-select 시/분 → body 텔레포트 ul.time-select-dropdown li)
// - 저장: [저장] → $confirm(확인/취소) → POST /webApi/attd01/update-sch-infos → 성공/오류 $alert
export const ATTD01 = "http://localhost:8081/safenote/main/Attd_01";

export async function openAttd01(page) {
  await page.goto(ATTD01, { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2500);
  await page.click('button:has-text("조회")');
  await page.waitForTimeout(2500);
}

export function bodyText(page) {
  return page.evaluate(() => document.body.innerText);
}

// 목록에서 근무코드(schNo) 행 텍스트 채집
export async function readSchRow(page, schNo) {
  return page.evaluate((no) => {
    const rows = [...document.querySelectorAll("tbody tr")];
    const row = rows.find((r) => r.innerText.includes(no));
    return row ? row.innerText.replace(/\n/g, " | ") : null;
  }, schNo);
}

// 행 더블클릭 → 편집 팝업 열기
export async function openSchEdit(page, schNo) {
  const ok = await page.evaluate((no) => {
    const rows = [...document.querySelectorAll("tbody tr")];
    const row = rows.find((r) => r.innerText.includes(no));
    if (!row) return false;
    row.scrollIntoView();
    const ev = new MouseEvent("dblclick", { bubbles: true, cancelable: true });
    row.dispatchEvent(ev);
    return true;
  }, schNo);
  if (!ok) throw new Error(`근무타입 행 미발견: ${schNo}`);
  await page.waitForTimeout(1500);
  const pop = page.locator(".modal-content-sch-info");
  if ((await pop.count()) === 0) throw new Error("SchInfoPop 미노출");
}

// [+ 생성] 버튼으로 신규 팝업 열기 (ViewHeader create)
export async function openSchCreate(page) {
  await page.locator('button:has-text("생성")').first().click();
  await page.waitForTimeout(1500);
  const pop = page.locator(".modal-content-sch-info");
  if ((await pop.count()) === 0) throw new Error("SchInfoPop(생성) 미노출");
}

// 팝업 내 적용일 세팅(YYYY-MM-DD) — flatpickr 직접 호출
export async function setPopupApplyDate(page, ymdDash) {
  await page.evaluate((v) => {
    const pop = document.querySelector(".modal-content-sch-info");
    const inp = pop?.querySelector("input.calendar-input");
    if (inp?._flatpickr) inp._flatpickr.setDate(v, true);
  }, ymdDash);
  await page.waitForTimeout(800);
}

// 팝업 내 idx번째 TimeInput(.time-input-wrap 순서: 구간1시작/구간1종료/휴게1시작/(구간2...)) 시·분 세팅
export async function setPopupTime(page, idx, hh, mm) {
  const wraps = page.locator(".modal-content-sch-info .time-input-wrap");
  const wrap = wraps.nth(idx);
  // 시
  await wrap.locator("button.time-select").nth(0).click();
  await page.waitForTimeout(500);
  await page.locator("ul.time-select-dropdown:visible li", { hasText: new RegExp(`^${hh}$`) }).first().click();
  await page.waitForTimeout(400);
  // 분
  await wrap.locator("button.time-select").nth(1).click();
  await page.waitForTimeout(500);
  await page.locator("ul.time-select-dropdown:visible li", { hasText: new RegExp(`^${mm}$`) }).first().click();
  await page.waitForTimeout(400);
}

// 팝업 내 사용여부 select 세팅 (BaseSelect — 실제 select 요소로 렌더되는지 실측 필요, 폴백 포함)
export async function setPopupUseYn(page, yn) {
  const pop = page.locator(".modal-content-sch-info");
  const sel = pop.locator("select");
  if (await sel.count()) {
    await sel.last().selectOption(yn);
    await page.waitForTimeout(400);
    return "select";
  }
  // BaseSelect 커스텀 렌더 폴백: 버튼 클릭 → 옵션 텍스트 클릭
  const label = yn === "Y" ? "사용" : "미사용";
  await pop.locator(".base-select, [class*=select]").last().click();
  await page.waitForTimeout(400);
  await page.locator(`li:has-text("${label}"), option:has-text("${label}")`).last().click();
  await page.waitForTimeout(400);
  return "custom";
}

// 팝업 저장 → confirm 확인 → 결과 모달 텍스트 수집(성공/오류 공용)
// 반환: { modalTexts: [...], dialogs: [...] }
export async function savePopupAndCollect(page) {
  const dialogs = [];
  const handler = async (dlg) => { dialogs.push(dlg.message()); await dlg.accept(); };
  page.on("dialog", handler);
  const modalTexts = [];
  await page.locator('.modal-content-sch-info button:has-text("저장"), .modal-content-sch-info button:has-text("생성")').first().click();
  await page.waitForTimeout(1200);
  // confirm(저장하시겠습니까) → 확인, 이어지는 결과 alert → 텍스트 채집 후 확인
  for (let i = 0; i < 4; i++) {
    const txt = await page.evaluate(() => {
      // SchInfoPop 자신을 제외한 얕은 알럿/컨펌 모달 텍스트
      const overlays = [...document.querySelectorAll(".modal-overlay")];
      const top = overlays[overlays.length - 1];
      if (!top) return null;
      if (top.querySelector(".modal-content-sch-info") && overlays.length === 1) return null;
      return top.innerText.trim();
    });
    if (txt) modalTexts.push(txt.replace(/\n/g, " | "));
    const ok = page.locator('button:has-text("확인")').last();
    if (await ok.count()) {
      await ok.click().catch(() => {});
      await page.waitForTimeout(1200);
    } else break;
  }
  page.off("dialog", handler);
  return { modalTexts, dialogs };
}

// 팝업 닫기(X)
export async function closePopup(page) {
  const btn = page.locator(".modal-content-sch-info .icon-button").first();
  if (await btn.count()) { await btn.click().catch(() => {}); await page.waitForTimeout(600); }
}
