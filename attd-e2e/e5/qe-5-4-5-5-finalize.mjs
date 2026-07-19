// QE-5-4 (일용직 정규기능 게이팅) + QE-5-5 (만료 슬롯 표기)
import { webLogin } from "../lib/browser.mjs";
import { record, shotPath } from "../lib/record.mjs";

const WEB = "http://localhost:8081";

const run = async () => {
  // QE-5-5: 웹 User_05 화면 캡처 (슬롯 점유이력 = 만료/해제 표기)
  let user05ok = false;
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto(`${WEB}/safenote/main/User_05`, { waitUntil: "networkidle", timeout: 20000 });
    await page.waitForTimeout(1500);
    try { await page.screenshot({ path: shotPath("QE-5-5", "web", "user05"), animations: "disabled" }); user05ok = true; } catch {}
  } catch (e) { /* 무시 */ }

  record("QE-5-5", "OBSERVED", {
    title: "만료 슬롯 상태의 화면 표기",
    webView: "웹 User_05(일일계정관리) = 슬롯 점유이력(occupyDtime/releaseDtime, slotNo). site00001 14행 — releaseDtime 세팅행=만료/해제 슬롯 표기. 화면 캡처=" + user05ok,
    appView: "만료(status'05') 계정 앱 일용직 로그인 → DAILYLOGIN_400_001 '아이디 혹은 비밀번호를 확인해주세요'(계정존재 비노출·마스킹). 실제 만료계정 비번 미상으로 만료전용 문구 격리 불가(보안상 통합 메시지가 정상).",
    dbCheck: "site00001 status'05' 만료 계정 다수(D2026071500018/User01/User02/BOT10/D202606xx). QTDAILY2(status'04' 승인대기)는 슬롯 미점유라 User_05 이력에 미표기 — 승인제(D6)로 점유 이연됨(추가 관찰).",
    note: "만료 계정 로그인 거부 UX=통합 마스킹 메시지(400_001). User_05는 슬롯 점유/해제 이력 기반이라 미점유(승인대기/미승인) 일용직은 미노출. §7 G17 후보.",
  });

  // QE-5-4: 일용직 정규기능 게이팅 — 활성 일용직 세션 부재로 UI 게이팅 미검증(BLOCKED 시드)
  record("QE-5-4", "BLOCKED", {
    title: "일용직 계정의 정규 기능 접근 UI 게이팅",
    note: "활성 일용직 세션 필요(연차/OT 메뉴 비노출·직접URL 403 확인). QTDAILY2=승인대기('04')로 앱 홈 진입 불가 + 사이트00001 master(ADMIN/YJKIM) 자격증명 부재로 입장승인 불가. 기존 활성 일용직(DBOT01) 비번 미상 → BLOCKED(시드/환경).",
    dbCheck: "설계상 게이팅 근거(코드 확인): 일용직 TB_USER EMPLOYMENT_TYPE='DAILY'·NODE_CD=NULL·AUTH_CD='99999'. LoginView 가 gv_employmentType='DAILY' 저장 → 앱 화면들이 연차/근태조회 등 정규 카드/메뉴 숨김(J1-4). 서버측 인가는 별도. 런타임 UI 미검증.",
    appView: "(미검증 — 활성 세션 부재)",
  });
  console.log("QE-5-4/5-5 recorded (user05 shot:" + user05ok + ")");
};
run();
