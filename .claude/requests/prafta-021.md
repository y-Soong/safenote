\##  화면 수정 요청의 건



1\. 사용자 연차관리

&#x09;- 사용률 컬럼에 그래프 표시 제거(그냥 단순 퍼센트 값만 나오도록 수정 필요) (완료)

&#x09;- 법정휴가, 법정 휴가 외 값을 더한 값을 표시해 줄 전체 컬럼을 추가해줘(법정휴가, 법정 휴가 외 처럼 2행으로 표현해주면 돼 (전체 : 부여, 사용, 잔여)) (완료)

&#x09;- 테이블 내 관리 컬럼 삭제, 관리 컬럼에 버튼형태로 묶여있떤 팝업 표시 이벤트는 행 더블클릭으로 전환 (완료)

2\. 연차 타입 관리 화면

&#x09;- 테이블에 수동여부 표시 컬럼 추가 (완료)

3\. 근무 계획 관리 화면 수정

&#x09;- 연차 타입 적용 관련된 부분 수정

&#x09;	: 연차타입을 골라서 적용 시키는 구조에서 법정 휴가만 적용 가능한 형태로 수정 (완료)

&#x09;	: 해당 화면에서 스케줄로 지정된 연차가 있다면 별도의 결재요청 필요 X, 그냥 연차 사용 기록만 남기면 됨 (혹시 문제있다면 채팅으로 질의하여 방향성 설정) (완료 — B안: 사용기록+잔여차감, 매니저 게이트/이중차감 가드 적용. 후속: 셀 제거/변경 시 차감 복원)

&#x09;- 연차 신청 관련된 부분 삭제 (관리자가 사용자의 연차를 입력해주는 성격의 화면이라 별도의 결재 요청이나 신청의 개념은 아님) (완료)

4\. 하기 공간에 사용자가 근태 요청, 초가근무 요청, 연차 사용 요청을 할때별로 데이터 insert 예문좀 써줘 (완료)

```sql
/* ============================================================
   근태 / 초과근무 / 연차 사용 요청 테스트 INSERT 예문
   (로컬 dev, 회사 001 / 사업장 00001 / 신청자 20260400013 / 결재자 20260400010 / 노드 n1 기준)

   REQ_TYPE(SYS032): 01 근태생성 / 02 근태수정 / 03 초과근무생성 / 04 초과근무수정 / 05 연차사용 / 06 연차수정
   REQ_STATUS(SYS033): 01 신청 / 02 승인 / 03 반려 / 04 취소
   ※ REQ_ID/LEAVE_ID는 운영은 채번(CONCAT(YYYYMMDD, FNC_CMM_SEQ_NEXTVAL(cmpny,'KEY')))이나,
     테스트는 유니크한 임의값을 직접 넣으면 됨. 컬럼/사업장/사용자코드는 실제 값에 맞게 교체.
   ============================================================ */

/* ── 1) 근태 요청 (근태생성, REQ_TYPE='01') ───────────────────────────────
   출근/퇴근 누락분 보정 신청. 처리: 요청승인관리 '근태 보정' 탭에서 매니저 승인/반려.
   (근태/초과는 단일단계 처리라 TB_USER_ATTD_REQ_APPROVAL 미사용) */
INSERT INTO TB_USER_ATTD_REQ (
    REQ_ID, CMPNY_CD, SITE_CD, USER_CD, REQ_TYPE, REQ_STATUS, REQ_REASON,
    WORK_YMD, NODE_CD, WORK_SEQ, START_DATE, START_TIME, END_DATE, END_TIME,
    DEL_YN, INSERT_NO, INSERT_DATE
) VALUES (
    'REQTEST0000000001', '001', '00001', '20260400013', '01', '01', '출근 기록 누락 보정',
    '20260520', 'n1', 1, '20260520', '0900', '20260520', '1800',
    'N', '20260400013', NOW()
);

/* ── 2) 초과근무 요청 (초과근무생성, REQ_TYPE='03') ───────────────────────
   처리: 요청승인관리 '초과근무' 탭에서 매니저 승인/반려. OT_TYPE은 SYS 코드값에 맞게. */
INSERT INTO TB_USER_ATTD_REQ (
    REQ_ID, CMPNY_CD, SITE_CD, USER_CD, REQ_TYPE, REQ_STATUS, REQ_REASON,
    WORK_YMD, NODE_CD, OT_TYPE, START_DATE, START_TIME, END_DATE, END_TIME,
    DEL_YN, INSERT_NO, INSERT_DATE
) VALUES (
    'REQTEST0000000002', '001', '00001', '20260400013', '03', '01', '월말 정산 초과근무',
    '20260521', 'n1', '01', '20260521', '1800', '20260521', '2030',
    'N', '20260400013', NOW()
);

/* ── 3) 연차 사용 요청 (연차사용, REQ_TYPE='05') ──────────────────────────
   결재 흐름이라 3개 테이블 동반:
     (1) TB_USER_ATTD_REQ          - 요청 본문 (LEAVE_TYPE/LEAVE_DAYS 포함)
     (2) TB_USER_ATTD_REQ_APPROVAL - 결재라인 (단계별 결재자, 현재 진행단계='01')
     (3) TB_USER_LEAVE_USE         - 차감 예약행 (CONFIRMED) — 화면의 연차타입/사용단위가 여기서 표시됨
   처리: 결재자가 요청승인관리 '연차 상신'(내 결재함)에서 승인/반려. */
-- (1) 요청 본문
INSERT INTO TB_USER_ATTD_REQ (
    REQ_ID, CMPNY_CD, SITE_CD, USER_CD, REQ_TYPE, REQ_STATUS, REQ_REASON,
    WORK_YMD, NODE_CD, LEAVE_TYPE, LEAVE_DAYS,
    DEL_YN, INSERT_NO, INSERT_DATE
) VALUES (
    'REQTEST0000000003', '001', '00001', '20260400013', '05', '01', '개인 연차 사용',
    '20260522', 'n1', 'ANNUAL', 1.00000,
    'N', '20260400013', NOW()
);
-- (2) 결재라인 1단계 (결재자 20260400010, 진행중 '01'=신청 / 대기단계는 '00')
INSERT INTO TB_USER_ATTD_REQ_APPROVAL (
    REQ_ID, APPROVAL_STEP, CMPNY_CD, APPROVER_USER_CD, APPROVAL_STATUS,
    INSERT_NO, INSERT_DATE
) VALUES (
    'REQTEST0000000003', 1, '001', '20260400010', '01', '20260400013', NOW()
);
-- (3) 차감 예약행 (USE_UNIT_TYPE: 00 1일/01 반차/02 2h/03 1h/04 30분 — 시간차는 START/END_TIME+LEAVE_MINUTES)
INSERT INTO TB_USER_LEAVE_USE (
    LEAVE_ID, CMPNY_CD, SITE_CD, USER_CD, LEAVE_CD, REQ_ID, GRANT_ID,
    START_DATE, START_TIME, END_DATE, END_TIME, USE_UNIT_TYPE, LEAVE_DAYS, LEAVE_MINUTES,
    LEAVE_REASON, LEAVE_STATUS, DEL_YN, INSERT_NO, INSERT_DATE
) VALUES (
    'LUSETEST00000003', '001', '00001', '20260400013', '00016', 'REQTEST0000000003', NULL,
    '20260522', NULL, '20260522', NULL, '00', 1.00000, NULL,
    '개인 연차 사용', 'CONFIRMED', 'N', '20260400013', NOW()
);
```



\## 이 마크다운 파일에 요청내용을 계속 추가할 예정이니 작업이 끝났으면 해당 작업란 옆에 "(완료)" 라고 표시할 것

\## 추가 설명이 필요하거나 모호한 부분이 있을 경우는 채팅으로 질문해줘

