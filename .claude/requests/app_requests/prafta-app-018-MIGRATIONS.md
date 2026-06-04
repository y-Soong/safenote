# prafta-app-018 마이그레이션 작업 정리 (앱 연차 신청 A/B/C/D)

작성: 메인 세션. 대상: 운영(또는 타 환경) 배포 전 DB 점검. 현재 개발 DB 상태는 MCP(prafta-mysql)로 실측 확인함.

## 결론 먼저
**app-018(A/B/C/D)은 신규 DDL/마이그레이션이 없다.**
- 신규 테이블/컬럼 추가 없음 — 기존 `TB_USER_ATTD_REQ` / `TB_USER_LEAVE_USE` / `TB_USER_ATTD_REQ_APPROVAL` / `TB_USER_LEAVE_GRANT` / `TB_LEAVE_USAGE_POLICY` / `TB_LEAVE_TYPE_MGMT` / `TB_LEAVE_POLICY` / `TB_APRV_LINE_PRESET(+_D)` 만 사용.
- 신규 에러코드 `ATTD_400_100`(app-017)·`102`(app-018-B)는 **Java enum `AttdErrorCode` 상수**라 DB 변경 없음.
- 신규 SYS 코드 없음 — 사용단위 SYS025(00~04)·SYS033/SYS044 등은 기존 시드(prafta-019-A 등) 사용.
- 따라서 **새로 작성·실행할 마이그 SQL은 없다.**

## 단, 전제(prerequisite) 마이그레이션이 적용돼 있어야 동작한다
app-018은 아래 **기존** 마이그레이션 산출 컬럼/테이블에 의존한다. 미적용 환경이면 apply-meta/신청 SQL이 `Unknown column`으로 500. 운영 배포 전 반드시 확인할 것.

| 의존 객체 | 출처 마이그 | app-018 사용처 | 개발 DB 실측 |
|---|---|---|---|
| `TB_LEAVE_USAGE_POLICY.USAGE_UNIT` (varchar20) | prafta-024 | 법정 허용단위 산출(A apply-meta), 단위 게이팅(B) | ✅ 존재 |
| `TB_LEAVE_POLICY.APRV_USE_YN` (char1) | prafta-019-E | 법정 결재필요 판정(A/B) | ✅ 존재 |
| `TB_USER_ATTD_REQ.LEAVE_TYPE` / `LEAVE_DAYS`(decimal(8,5)) | prafta-019(-E) | 신청 INSERT/차감(B), 카드 표시(D) | ✅ 존재(LEAVE_DAYS=8,5) |
| `TB_USER_LEAVE_USE.LEAVE_DAYS`(decimal(8,5)) / `USE_UNIT_TYPE` | prafta-019(-A/E) | 차감 SSOT(B), 카드 단위/일수(D) | ✅ 존재(8,5) |
| `TB_USER_ATTD_REQ_APPROVAL` (결재단계) | prafta-019-D | 결재선 INSERT/자기승인(B) | ✅ 존재 |
| `TB_APRV_LINE_PRESET` (+`_D`) | prafta-020 | 결재선 프리셋 조회/전개(A/B/C) | ✅ 존재 |
| SYS025 코드(00종일~04 30분) 시드 | prafta-019-A | 단위 라벨/게이팅(A/B/C/D) | ✅ (운영 매퍼 사용중) |

> 개발 DB에는 위 7종 **전부 적용 확인됨**(`INFORMATION_SCHEMA` 실측). 즉 현재 개발 환경에서는 추가 마이그 작업 불필요.

## 운영 배포 전 점검 쿼리 (해당 환경에서 실행)
```sql
-- 컬럼 존재/정밀도 확인
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
  FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND ( (TABLE_NAME='tb_leave_usage_policy' AND COLUMN_NAME='USAGE_UNIT')
      OR (TABLE_NAME='tb_leave_policy'       AND COLUMN_NAME='APRV_USE_YN')
      OR (TABLE_NAME='tb_user_attd_req'      AND COLUMN_NAME IN ('LEAVE_TYPE','LEAVE_DAYS'))
      OR (TABLE_NAME='tb_user_leave_use'     AND COLUMN_NAME IN ('LEAVE_DAYS','USE_UNIT_TYPE')) );
-- 테이블 존재 확인
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
 WHERE TABLE_SCHEMA=DATABASE()
   AND TABLE_NAME IN ('tb_user_attd_req_approval','tb_aprv_line_preset','tb_aprv_line_preset_d');
-- SYS025 코드 시드 확인(00~04)
SELECT DTL_CD FROM TB_SYST_VAL_D WHERE SYST_VAL_CD='SYS025' ORDER BY DTL_CD;
```
기대: LEAVE_DAYS 2건 모두 `decimal(8,5)`, 컬럼/테이블 전부 존재, SYS025 00~04. 하나라도 누락이면 해당 출처 마이그(prafta-024 / 019-D / 019-E / 020 / 019-A)를 **app-018 배포 전에** 적용.

## 함께 배포할 비-DB 변경(참고)
- Java/리소스(앱 BE 신규 모듈 `com.prafta.app.leave.leaveflow`, AttdErrorCode 100/101/102, web attd07 카드 컬럼) — 빌드/배포로 반영(마이그 아님).
- 앱 FE(`prafta-app-frontend`): 빌드 후 Flutter `assets/vue_app/` 복사 필요(연차 폼/진입점).
- 웹 FE(`prafta-web-frontend`): 빌드 반영(AttdDayDetailPop).

## 별건 follow-up (마이그 아님, 정책 판단 필요)
- 웹 `LeaveFlowServiceImpl#submitLeave` 결재자 cross-site 스코프 가드 부재(앱-B는 `countValidApprovers`로 막음, 웹은 잔존).
- 웹 자가 연차신청 UI(LeaveApplyPop) 미연결(고아) — 웹에서도 자가신청 열지 여부.
- prafta-app-009 결재선 통합을 스케줄/보정/초과근무 폼까지 일반화.
- 앱 결재함(승인/반려) 미러(현재 앱은 신청 쓰기만).
