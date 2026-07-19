// 보정/OT 폼 공용 헬퍼 — 스텝퍼 시트 입력 + 팝업 처리.
export async function setStepperTime(page, trigger, hh, mm) {
  await trigger.click();
  await page.waitForTimeout(800);
  const inputs = page.locator('input[inputmode="numeric"]:visible');
  const n = await inputs.count();
  if (n < 2) throw new Error(`시간 시트 입력 ${n}개 — 예상 2`);
  await inputs.nth(0).fill(String(hh));
  await inputs.nth(1).fill(String(mm));
  await page.waitForTimeout(300);
  await page.click("button.wp-confirm");
  await page.waitForTimeout(500);
}

export async function setStepperDate(page, trigger, y, m, d) {
  await trigger.click();
  await page.waitForTimeout(800);
  const inputs = page.locator('input[inputmode="numeric"]:visible');
  const n = await inputs.count();
  if (n < 3) throw new Error(`날짜 시트 입력 ${n}개 — 예상 3`);
  await inputs.nth(0).fill(String(y));
  await inputs.nth(1).fill(String(m));
  await inputs.nth(2).fill(String(d));
  await page.waitForTimeout(300);
  await page.click("button.wp-confirm");
  await page.waitForTimeout(500);
}

export async function clickPopupOk(page, times = 2) {
  for (let i = 0; i < times; i++) {
    const ok = page.locator("button:has-text('확인')").first();
    if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
  }
}

// 내근태 이번주에서 요일버튼 → 시트 액션 진입
export async function openReqForm(page, dayRegex, actionText) {
  await page.click('button.app-tabbar__tab:has-text("근태")');
  await page.waitForTimeout(2000);
  await page.click('.attd-seg__item:has-text("이번주")');
  await page.waitForTimeout(2000);
  await page.evaluate((pattern) => {
    const btn = [...document.querySelectorAll("button.dc")].find((b) => new RegExp(pattern).test(b.innerText));
    if (!btn) throw new Error("일 버튼 미발견: " + pattern);
    btn.click();
  }, dayRegex);
  await page.waitForTimeout(1500);
  await page.locator(`:text("${actionText}")`).first().click();
  await page.waitForTimeout(2500);
}
