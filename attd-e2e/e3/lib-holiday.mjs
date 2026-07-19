// E3 공용 — 휴일관리(Attd_02) 등록/삭제/패널 채집 헬퍼. 실측: e3/explore-attd02*.mjs (2026-07-17)
// 등록: 캘린더 월 이동 → 일 셀 클릭 → [휴일 등록] → 휴일명 입력(일자 프리필) → [등록]
// 삭제: 목록 탭에서 휴일명 행의 [삭제] — 공휴일(type 01) 행은 삭제 버튼 없음('-')
export const ATTD02 = "http://localhost:8081/safenote/main/Attd_02";

export async function openAttd02(page) {
  await page.goto(ATTD02, { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2500);
}

// 캘린더 헤더 "2026년 7월" 기준으로 목표 월까지 </> 클릭
export async function gotoMonth(page, y, m) {
  for (let i = 0; i < 14; i++) {
    const head = await page.evaluate(() => {
      const el = [...document.querySelectorAll("*")].find((n) => /^\d{4}년 \d{1,2}월$/.test(n.innerText?.trim() ?? "") && n.children.length === 0);
      return el ? el.innerText.trim() : null;
    });
    if (!head) throw new Error("캘린더 헤더 미발견");
    const [, cy, cm] = head.match(/(\d{4})년 (\d{1,2})월/);
    if (Number(cy) === y && Number(cm) === m) return;
    const dir = y * 12 + m > Number(cy) * 12 + Number(cm) ? ">" : "<";
    await page.click(`button:has-text("${dir}")`);
    await page.waitForTimeout(900);
  }
  throw new Error(`월 이동 실패: ${y}-${m}`);
}

// 캘린더에서 일 셀 클릭(첫 줄이 일자 숫자인 td)
export async function selectDay(page, day) {
  const ok = await page.evaluate((dd) => {
    const tds = [...document.querySelectorAll("td")];
    const td = tds.find((t) => {
      const first = (t.innerText ?? "").trim().split("\n")[0];
      return first === String(dd);
    });
    if (!td) return false;
    td.scrollIntoView();
    td.click();
    return true;
  }, day);
  if (!ok) throw new Error(`일 셀 미발견: ${day}`);
  await page.waitForTimeout(900);
}

// 휴일 등록 — 반환: { dialogs: [등록 중 alert/confirm 문구], panelText }
export async function registerHoliday(page, y, m, d, name) {
  const dialogs = [];
  const handler = async (dlg) => { dialogs.push(dlg.message()); await dlg.accept(); };
  page.on("dialog", handler);
  await gotoMonth(page, y, m);
  await selectDay(page, d);
  await page.click('button:has-text("휴일 등록")');
  await page.waitForTimeout(1200);
  // 일자 프리필 확인(선택 셀 반영) — 다르면 flatpickr 직접 세팅
  const dateVal = await page.locator("input.calendar-input").last().inputValue().catch(() => "");
  const want = `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
  if (dateVal !== want) {
    await page.evaluate((v) => {
      const inp = [...document.querySelectorAll("input.calendar-input")].at(-1);
      if (inp?._flatpickr) inp._flatpickr.setDate(v, true);
    }, want);
    await page.waitForTimeout(600);
  }
  await page.fill('input[placeholder="예: 창립기념일"]', name);
  await page.waitForTimeout(300);
  await page.locator('button:has-text("등록")').last().click();
  await page.waitForTimeout(2000);
  // 모달형 확인 팝업 처리(있으면)
  for (let i = 0; i < 2; i++) {
    const ok = page.locator("button:has-text('확인')").first();
    if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1000); }
  }
  page.off("dialog", handler);
  const panelText = await page.evaluate(() => document.body.innerText);
  return { dialogs, dateVal, panelText };
}

// 목록 탭에서 휴일명으로 행 찾아 삭제 — 반환 { found, hadDeleteBtn, dialogs }
export async function deleteHolidayByName(page, y, m, name) {
  const dialogs = [];
  const handler = async (dlg) => { dialogs.push(dlg.message()); await dlg.accept(); };
  page.on("dialog", handler);
  await gotoMonth(page, y, m);
  await page.click('button:has-text("목록")');
  await page.waitForTimeout(1000);
  const row = page.locator("li, tr, div", { hasText: name }).last();
  if ((await row.count()) === 0) { page.off("dialog", handler); return { found: false, hadDeleteBtn: false, dialogs }; }
  const delBtn = row.locator('button:has-text("삭제")').first();
  const hadDeleteBtn = (await delBtn.count()) > 0;
  if (hadDeleteBtn) {
    await delBtn.click();
    await page.waitForTimeout(1500);
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1000); }
    }
  }
  page.off("dialog", handler);
  return { found: true, hadDeleteBtn, dialogs };
}

export async function bodyText(page) {
  return page.evaluate(() => document.body.innerText);
}
