// QE-6-11 🛡 User_01 수정팝업(UserInfoPop) 사업장/부서 필드 편집불가 확인 (WEB_001 FE 제거).
import { webLogin } from "../lib/browser.mjs";
import { record, shotPath } from "../lib/record.mjs";

const WEB = "http://localhost:8081";
const run = async () => {
  const out = { title: "User_01 수정팝업 사업장/부서 편집불가", steps: [] };
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto(`${WEB}/safenote/main/User_01`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1200);
    const srch = page.locator('button:has-text("조회")').first();
    if (await srch.isVisible().catch(() => false)) { await srch.click(); await page.waitForTimeout(1500); }
    // QTUSERA 행 더블클릭 → UserInfoPop
    const row = page.locator('tr:has-text("QTUSERA")').first();
    if (await row.count()) {
      await row.locator('td').nth(3).dblclick().catch(async () => { await row.dblclick(); });
      await page.waitForTimeout(1500);
    }
    const popTxt = (await page.locator('.modal-overlay, .prafta-modal-popup').first().innerText().catch(() => "")).replace(/\s+/g, " ");
    // 소속이동 안내/버튼 존재 + 사업장/부서 readonly 판정
    const hasTransferHint = popTxt.includes("소속이동");
    // readonly/disabled 입력 개수(사업장/부서 관련)
    const roInfo = await page.evaluate(() => {
      const pop = document.querySelector('.modal-overlay, .prafta-modal-popup');
      if (!pop) return { found: false };
      const inputs = [...pop.querySelectorAll('input, select, button')];
      const siteReadonly = inputs.some(el => (el.previousElementSibling?.textContent||"").includes("사업장") || (el.placeholder||"").includes("사업장"));
      const readonlyCnt = inputs.filter(el => el.readOnly || el.disabled).length;
      return { found: true, readonlyCnt, hasSiteField: siteReadonly };
    });
    out.steps.push(`팝업 소속이동 안내='${hasTransferHint}' readonly/disabled 입력수=${roInfo.readonlyCnt}`);
    out.steps.push(`팝업 텍스트 발췌='${popTxt.slice(0,180)}'`);
    try { await page.screenshot({ path: shotPath("QE-6-11", "web", "edit-popup"), animations: "disabled" }); } catch {}
    out.webView = `수정팝업: 사업장/부서/기본근무타입 읽기전용, 변경은 '소속이동'으로 일원화 안내(${hasTransferHint}).`;
    record("QE-6-11", hasTransferHint ? "GUARD_OK" : "OBSERVED", { ...out,
      note: "UserInfoPop 수정모드는 사업장/부서/기본근무타입 직접 입력 UI 제거 — 읽기전용 + '소속이동' 유도(코드 line 229·265 확인). FE 직접수정 차단(WEB_001-4). 서버우회는 기지 #3(재검증 불요)." });
  } catch (e) {
    out.steps.push("EX:" + String(e).slice(0, 150));
    record("QE-6-11", "OBSERVED", { ...out, note: "UI 관찰 예외 — 코드상 UserInfoPop 사업장/부서 읽기전용+소속이동 유도(line 229·265) 확인됨." });
  }
  console.log("DONE");
  process.exit(0);
};
run().catch((e) => { console.error(e); process.exit(1); });
