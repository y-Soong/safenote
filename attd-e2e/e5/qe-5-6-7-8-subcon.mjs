// QE-5-6/5-7/5-8: 하도급(연동 근로자 근태/TBM 귀속) — 시드 데이터레벨 이연 상태 → 이월(DEFERRED)
// 근거(DB 실측, 2026-07-17): subcon 메커니즘 시드는 생존하나 실데이터 귀속은 subcon P3 'option 2'로 이연된 상태.
import { record } from "../lib/record.mjs";

const SUBCON_DB = "관계: A→B(rel8)ACCEPTED·B→C(rel5)ACCEPTED / 링크: A00001→B00003(link4)ACTIVE·B00002→C00002(link3)ACTIVE·A00001→B00002(link2)TERMINATED / "
  + "ATTD스냅샷: 원청(nrTnBj) 소유 4건(id2/4/5/6) 전부 ROW_CNT=0(실근태 미공유) / "
  + "미러근로자: b_worker1(B/00002)·c_worker1(C/00002) 둘다 ACCOUNT_STATUS='04'(승인대기, 앱로그인 불가) / "
  + "TBM지정공유: SHARE_ID 5(RELATION_TERMINATED)·6(MANUAL) 전부 DEL_YN='Y'(해제됨).";

record("QE-5-6", "DEFERRED", {
  title: "[연동 근로자 근태 귀속] 미러 사업장 근로자 출퇴근 → 원청 화면 귀속",
  dbCheck: SUBCON_DB,
  note: "이월 사유: (1)미러 근로자 status'04' → 앱 로그인 불가, 활성화엔 미러 사업장(B/00002) 바운드 master 필요하나 부재(subcon D-2 blocker). "
    + "(2)현재 ACTIVE 미러 링크(link4)는 B/00003 대상=근로자 0명이라 b_worker1(00002)의 근태가 원청으로 흐르지 않음. "
    + "(3)ATTD 스냅샷 전부 ROW_CNT=0(실근태 미시딩, subcon P3 option2 이연). 실데이터 귀속엔 워커활성+재링크+근태생성+공유요청·승인 다단 재구축(>10분) 필요 → '무리한 재구축 금지' 원칙에 따라 이월. "
    + "메커니즘(요청/승인/스냅샷 소유=원청/relabel/IDOR)은 subcon P3 PASS + 본 세션 DB 생존확인으로 커버.",
});

record("QE-5-7", "DEFERRED", {
  title: "[A2×E9] 연동 해지 후 기존 귀속 근태 표기(독립화)",
  dbCheck: SUBCON_DB + " 원청 스냅샷 2/4/5/6 잔존(해지 후에도 소유=수신사).",
  note: "이월 사유: 5-6 실데이터(근태행 귀속) 부재로 '해지 전후 스냅샷 UI 표기 대조'가 성립 불가. "
    + "해지=독립화 정책 자체는 subcon P8 PASS(link2 TERMINATED→B/00002 독립화, 스냅샷 소유 수신사 잔존, 하위체인 존속) 완료. "
    + "원청 master 자격증명 부재로 UI 재조회 미수행. 데이터레벨 재구축 필요 → 이월.",
});

record("QE-5-8", "DEFERRED", {
  title: "[E9] TBM 입실 데이터 귀속 — 해지 전후",
  dbCheck: SUBCON_DB,
  note: "이월 사유: TBM 지정공유 체인(SHARE_ID 5/6) 전량 DEL_YN='Y'(해제: RELATION_TERMINATED/MANUAL) → 지정 체인 미생존. "
    + "하청 근로자 활성계정 부재(status'04')로 TBM 입실 1건 생성 불가. "
    + "TBM 지정 relabel(P5)·해지 자동회수(P8) 메커니즘은 subcon PASS. 재지정+워커활성 재구축 없이 이월.",
});
console.log("QE-5-6/7/8 recorded DEFERRED");
