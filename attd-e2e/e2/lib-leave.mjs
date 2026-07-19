// E2 연차 공용 헬퍼 — 신청 폼 진입/입력/제출 + 결재함 승인/반려.
// 셀렉터 출처: LeaveApplyForm.vue / LeaveApproverPickerSheet.vue / LeaveApprovalDetailView.vue 실소스 + run/selector-catalog.md
export async function waitLoaded(page) {
  await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 }).catch(() => {});
}

// 마이 탭 → 연차 요약 섹션 → 연차 현황 → [연차 신청하기] → LeaveApply 폼
export async function openLeaveApply(page) {
  await page.click('button.app-tabbar__tab:has-text("마이")');
  await page.waitForTimeout(2000);
  await waitLoaded(page);
  await page.locator('[aria-label="연차 현황 보기"]').first().click();
  await page.waitForTimeout(2500);
  await waitLoaded(page);
  await page.locator('button:has-text("연차 신청하기")').first().click();
  await page.waitForTimeout(2500);
  await waitLoaded(page);
}

// 연차 종류 선택(.type-item 이름 매칭)
export async function selectLeaveType(page, name) {
  await page.locator(`button.type-item:has-text("${name}")`).first().click();
  await page.waitForTimeout(800);
}

// 사용 단위 칩 선택(라벨: 종일/반차/2시간/1시간/30분)
export async function selectUnit(page, label) {
  await page.locator(`button.unit-chip:has-text("${label}")`).first().click();
  await page.waitForTimeout(800);
}

// 날짜 스텝퍼(DateStepperField → 시트 numeric 3개 → wp-confirm)
export async function setDate(page, y, m, d) {
  await page.locator("button.dsf-field").first().click();
  await page.waitForTimeout(800);
  const inputs = page.locator('input[inputmode="numeric"]:visible');
  const n = await inputs.count();
  if (n < 3) throw new Error(`날짜 시트 입력 ${n}개 — 예상 3`);
  await inputs.nth(0).fill(String(y));
  await inputs.nth(1).fill(String(m));
  await inputs.nth(2).fill(String(d));
  await page.waitForTimeout(300);
  await page.click("button.wp-confirm");
  await page.waitForTimeout(600);
}

// 시작 시각(TimeStepperField) — 시간차 단위 전용
export async function setStartTime(page, hh, mm) {
  await page.locator("button.tsf-field").first().click();
  await page.waitForTimeout(800);
  const inputs = page.locator('input[inputmode="numeric"]:visible');
  const n = await inputs.count();
  if (n < 2) throw new Error(`시간 시트 입력 ${n}개 — 예상 2`);
  await inputs.nth(0).fill(String(hh));
  await inputs.nth(1).fill(String(mm));
  await page.waitForTimeout(300);
  await page.click("button.wp-confirm");
  await page.waitForTimeout(600);
}

// 종료 스텝퍼 [+] n-1 회 (기본 stepCount=1)
export async function stepUpEnd(page, times) {
  for (let i = 0; i < times; i++) {
    await page.locator('button[aria-label="종료 시각 늘리기"]').click();
    await page.waitForTimeout(300);
  }
}

// 결재자 추가 시트 — 검색 후 이름 선택 → 추가
export async function addApprover(page, name) {
  await page.locator("button.btn-add").click();
  await page.waitForTimeout(1200);
  await page.fill(".laps__search-input", name);
  await page.waitForTimeout(1500);
  await page.locator(`button.laps__item:has-text("${name}")`).first().click();
  await page.waitForTimeout(500);
  await page.locator("button.laps__add-btn").click();
  await page.waitForTimeout(800);
}

// preview 카드 텍스트(없으면 null)
export async function readPreview(page) {
  await page.waitForTimeout(1500); // 디바운스 400ms + 호출 여유
  const el = page.locator(".preview-card");
  if ((await el.count()) === 0) return null;
  return (await el.first().innerText()).replace(/\n/g, " | ");
}

// 확인 팝업 처리(액션→완료 2단)
export async function clickPopupOk(page, times = 2) {
  for (let i = 0; i < times; i++) {
    const ok = page.locator("button:has-text('확인')").first();
    if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
  }
}

// 결재함 진입: 마이 탭 → 연차 결재 관리 → 목록
export async function openLeaveApprovalList(page) {
  await page.click('button.app-tabbar__tab:has-text("마이")');
  await page.waitForTimeout(2000);
  await waitLoaded(page);
  await page.locator('button.mp-menu__row:has-text("연차 결재 관리")').click();
  await page.waitForTimeout(2500);
  await waitLoaded(page);
}

// 결재함 목록에서 요청자명 카드 클릭 → 상세
export async function openApprovalDetail(page, requesterName) {
  await page.locator(`:text("${requesterName}")`).first().click();
  await page.waitForTimeout(2500);
  await waitLoaded(page);
}

// 상세에서 승인
export async function approveInDetail(page) {
  await page.locator("button.lad-btn--approve").click();
  await page.waitForTimeout(1500);
  await clickPopupOk(page, 3);
}

// 상세에서 반려(사유 10자 이상 필수 — 반려 시트)
export async function rejectInDetail(page, reason) {
  await page.locator("button.lad-btn--reject").click();
  await page.waitForTimeout(1200);
  await page.fill(".ap-field__textarea", reason);
  await page.waitForTimeout(400);
  // 반려 시트의 확정 버튼(반려하기/확인 계열) — 시트 내 버튼 실측 폴백
  const candidates = ['button:has-text("반려하기")', 'button:has-text("반려 처리")'];
  let clicked = false;
  for (const sel of candidates) {
    const b = page.locator(sel).last();
    if (await b.count()) { await b.click(); clicked = true; break; }
  }
  if (!clicked) {
    // 시트 내 마지막 primary 버튼 폴백
    await page.locator('button:has-text("반려")').last().click();
  }
  await page.waitForTimeout(1500);
  await clickPopupOk(page, 3);
}

export function bodyText(page) {
  return page.evaluate(() => document.body.innerText);
}
