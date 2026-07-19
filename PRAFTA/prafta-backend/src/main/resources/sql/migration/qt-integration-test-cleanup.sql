-- =====================================================================
-- QT 근태/인사 통합 시나리오 테스트 — 적재 데이터 정리(cleanup)
--   작성: 2026-07-12 (테스트 세션 0~6 실행분)
--   대상: CMPNY_CD='001' / SITE_CD='00010'(QT통합테스트사업장) 하위 전량
--   ★ 실행 전 반드시 백업. 트랜잭션으로 감싸고 검증 후 COMMIT 한다.
--   ★★ 이 스크립트는 START TRANSACTION 으로 열린다. 실행했다면 **반드시 COMMIT 또는 ROLLBACK 으로 닫을 것.**
--       열어둔 채 방치하면 TB_USER 등의 행 락이 유지되어 애플리케이션 로그인/근태가 전부 락 대기로 멈춘다
--       (2026-07-12 실제 발생: 미커밋 트랜잭션 1,121행 수정 상태 → 로그인 30초 타임아웃).
--   ★ 회사 단위 설정(연차정책·환산시간)은 QT 전용이 아니므로 §6 에서 개별 원복한다.
--
--   [콜레이션 주의 — 2026-07-12 수정]
--   사용자 변수(SET @CMPNY := '001')를 쓰면 Workbench 기본 접속(utf8mb4_0900_ai_ci)과
--   컬럼(utf8mb4_unicode_ci)이 둘 다 coercibility=IMPLICIT 이라 Error 1267(Illegal mix of
--   collations)이 발생한다. 문자열 리터럴은 coercibility 가 더 낮아(COERCIBLE) 컬럼 콜레이션이
--   우선되므로 충돌하지 않는다. → 변수 없이 리터럴만 사용한다.
-- =====================================================================

START TRANSACTION;

-- 삭제 대상 사용자(QT 정규직 7 + 일용직 1). USER_ID prefix 로 식별.
DROP TEMPORARY TABLE IF EXISTS tmp_qt_users;
CREATE TEMPORARY TABLE tmp_qt_users AS
SELECT USER_CD FROM TB_USER
 WHERE CMPNY_CD = '001'
   AND (USER_ID LIKE 'QTUSER%' OR USER_ID = 'QTHR' OR USER_ID LIKE 'QTDAILY%');

-- ---------------------------------------------------------------------
-- 1. 근태/OT/요청
-- ---------------------------------------------------------------------
DELETE FROM TB_USER_ATTD_GPS           WHERE CMPNY_CD='001' AND SITE_CD='00010';
-- TB_USER_ATTD_HIST 는 USER_CD 컬럼이 없다(ATTD_ID/SITE_CD 로 식별) — 스키마 실조회 확인분.
DELETE FROM TB_USER_ATTD_HIST          WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_USER_OVERTIME_MGMT      WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_USER_ATTD_REQ_APPROVAL
 WHERE CMPNY_CD='001'
   AND REQ_ID IN (SELECT REQ_ID FROM TB_USER_ATTD_REQ WHERE CMPNY_CD='001' AND SITE_CD='00010');
DELETE FROM TB_USER_ATTD_REQ           WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_USER_ATTD_MGMT          WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_USER_WORK_PLAN          WHERE CMPNY_CD='001' AND SITE_CD='00010';

-- ---------------------------------------------------------------------
-- 2. 연차(사용/부여/변경요청)
-- ---------------------------------------------------------------------
DELETE FROM TB_LEAVE_CHANGE_REQUEST
 WHERE CMPNY_CD='001' AND TARGET_USER_CD IN (SELECT USER_CD FROM tmp_qt_users);
DELETE FROM TB_USER_LEAVE_USE
 WHERE CMPNY_CD='001' AND USER_CD IN (SELECT USER_CD FROM tmp_qt_users);
DELETE FROM TB_USER_LEAVE_GRANT
 WHERE CMPNY_CD='001' AND USER_CD IN (SELECT USER_CD FROM tmp_qt_users);

-- ---------------------------------------------------------------------
-- 3. 마감 / 소속이동 / 알림
-- ---------------------------------------------------------------------
DELETE FROM TB_ATTD_CLOSE_HIST WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_ATTD_CLOSE      WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_USER_TRANSFER_RESERVATION
 WHERE CMPNY_CD='001' AND USER_CD IN (SELECT USER_CD FROM tmp_qt_users);
DELETE FROM TB_NOTI_OUTBOX
 WHERE CMPNY_CD='001' AND TARGET_USER_CD IN (SELECT USER_CD FROM tmp_qt_users);

-- ---------------------------------------------------------------------
-- 4. 일용직 / 계정 / 디바이스
-- ---------------------------------------------------------------------
DELETE FROM TB_DAILY_USER_SLOT_HIS WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_DAILY_USER_SLOT     WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_DAILY_USER          WHERE CMPNY_CD='001' AND SITE_CD='00010';
-- 블랙리스트(QT-7-5 로 등록한 010-7777-0009). 테이블명은 TB_DAILY_BLACKLIST (스키마 실조회 확인분).
DELETE FROM TB_DAILY_BLACKLIST WHERE CMPNY_CD='001' AND REASON LIKE '[QT-7-5]%';

DELETE FROM TB_USER_DEVICE_LOGIN_HIST
 WHERE CMPNY_CD='001' AND USER_CD IN (SELECT USER_CD FROM tmp_qt_users);
-- prafta-tenant-2 마이그 이후: tb_user_device 에 CMPNY_CD 가 생기고 인덱스가 (CMPNY_CD, USER_CD) 로 재정의됐다.
-- USER_CD 단독은 더 이상 선두 키 컬럼이 아니라 safe update mode(Error 1175) 에 걸린다 → CMPNY_CD 를 동반한다.
DELETE FROM TB_USER_DEVICE
 WHERE CMPNY_CD='001' AND USER_CD IN (SELECT USER_CD FROM tmp_qt_users);
DELETE FROM TB_AUTH_TOKEN
 WHERE CMPNY_CD='001' AND USER_CD IN (SELECT USER_CD FROM tmp_qt_users);
DELETE FROM TB_TERMS_USER_AGR_MGMT
 WHERE CMPNY_CD='001' AND USER_CD IN (SELECT USER_CD FROM tmp_qt_users);
DELETE FROM TB_USER_SITE_AUTH
 WHERE CMPNY_CD='001' AND USER_CD IN (SELECT USER_CD FROM tmp_qt_users);

-- 노드 담당자 참조 해제 후 사용자 삭제(FK/논리참조 순서).
UPDATE TB_SITE_NODE
   SET MAIN_ADMIN_CD = NULL, SUB_ADMIN_CD = NULL
 WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_USER
 WHERE CMPNY_CD='001' AND USER_CD IN (SELECT USER_CD FROM tmp_qt_users);

-- ---------------------------------------------------------------------
-- 5. 사업장/근무타입/휴일/링크정책
-- ---------------------------------------------------------------------
DELETE FROM TB_DAILY_USER_LINK_POLICY WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_SCH_MGMT_HIST          WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_SCH_MGMT               WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_SITE_NODE              WHERE CMPNY_CD='001' AND SITE_CD='00010';
DELETE FROM TB_SITE                   WHERE CMPNY_CD='001' AND SITE_CD='00010';

-- 테스트로 등록한 휴일 3건(명칭에 [QT- 태그).
DELETE FROM TB_HOLIDAY WHERE CMPNY_CD='001' AND HOLIDAY_NM LIKE '[QT-%';

DROP TEMPORARY TABLE tmp_qt_users;

-- ---------------------------------------------------------------------
-- 6. 회사 단위 설정 원복 (★ QT 전용이 아니므로 반드시 확인 후 실행)
--    (a) 연차 환산시간: 테스트가 conv=400(적용일 20260714)을 신설했다. 480 기본으로 되돌리려면 삭제.
--    (b) 연차정책: 세션 0에서 allowQuarter='Y'(policySeq 10), 세션 4에서 maxDays=26(policySeq 11) 으로 변경됨.
--        원복이 필요하면 화면(Baim_07)에서 policySeq 9 값(allowQuarter=N, maxDays=25)으로 재설정할 것.
--        ※ 정책은 이력(TB_LEAVE_POLICY_HISTORY) 기반이라 DELETE 하지 말고 화면에서 재변경할 것.
-- ---------------------------------------------------------------------
-- (a) 환산시간 원복이 필요할 때만:
-- DELETE FROM TB_LEAVE_CONVERSION_POLICY WHERE CMPNY_CD='001' AND APPLY_FROM_DATE='20260714';

-- =====================================================================
-- 검증(COMMIT 전 실행): 모두 0 이어야 한다.
-- =====================================================================
SELECT
    (SELECT COUNT(*) FROM TB_USER            WHERE CMPNY_CD='001' AND (USER_ID LIKE 'QTUSER%' OR USER_ID='QTHR' OR USER_ID LIKE 'QTDAILY%')) AS users_left
  , (SELECT COUNT(*) FROM TB_SITE            WHERE CMPNY_CD='001' AND SITE_CD='00010')  AS site_left
  , (SELECT COUNT(*) FROM TB_USER_ATTD_MGMT  WHERE CMPNY_CD='001' AND SITE_CD='00010')  AS attd_left
  , (SELECT COUNT(*) FROM TB_USER_LEAVE_USE  WHERE CMPNY_CD='001' AND SITE_CD='00010')  AS leave_use_left
  , (SELECT COUNT(*) FROM TB_HOLIDAY         WHERE CMPNY_CD='001' AND HOLIDAY_NM LIKE '[QT-%') AS holiday_left;

-- 확인 후:
-- COMMIT;
-- ROLLBACK;


-- =====================================================================
-- 부록) 2026-07-13 멀티테넌시/전역ID 검증에서 생긴 테스트 데이터
--   ※ 위 QT 정리와 독립적으로 실행 가능. 실행 전 대상 확인 필수.
-- =====================================================================

-- (1) 001 회사에 만든 교차중복 검증용 계정 — 근태/연차 데이터 없음(생성만 함).
SELECT CMPNY_CD, USER_CD, USER_ID, USER_NM FROM TB_USER WHERE USER_ID = 'QTGLOBALOK1';
-- DELETE FROM TB_USER_SITE_AUTH WHERE CMPNY_CD='001' AND USER_CD = (SELECT USER_CD FROM (SELECT USER_CD FROM TB_USER WHERE CMPNY_CD='001' AND USER_ID='QTGLOBALOK1') T);
-- DELETE FROM TB_USER WHERE CMPNY_CD='001' AND USER_ID='QTGLOBALOK1';

-- (2) 검증용 신규 고객사(NEWCO~NEWCO5). 운영 전환 전 정리 대상.
--     회사코드는 랜덤 20자라 회사명으로 식별한다.
SELECT CMPNY_CD, CMPNY_NM, INSERT_DATE FROM TB_CMPNY WHERE CMPNY_NM LIKE 'NEWCO%';
-- 회사 단위 삭제는 참조 테이블이 많아 일괄 스크립트가 필요하다(미작성).
