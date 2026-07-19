-- =============================================================================
-- PRAFTA-SUBCON-T5 E2E 검증용 테스트 픽스처 (옵션 2 — read-side 검증 셋업)
--
-- ⚠️ 마이그레이션 아님. E2E 검증이 끝나면 맨 아래 [CLEANUP] 블록으로 반드시 원복할 것.
--    운영 스키마/데이터를 바꾸지 않는다(테스트 세션 1건 + share 2행 + B↔C 관계 1행만 추가).
--
-- 등장 회사 (실측 확정)
--   A(개설) = '001'                   JPC,               site 00001(중곡사업장)
--   B(1차)  = 'mLP5JWe5EOFPS17zZOKj'  NEWCO 프로비저닝검증, 관리자 USER_CD='20260700001'(뉴코관리자)
--   C(2차)  = 'IqzQKPKMpu75RsCoDN6m'  NEWCO3 시드검증,      관리자 USER_CD='20260700001'(뉴코쓰리관리자, B와 코드 동일!)
--                                                          사원   USER_CD='20260700002'(뉴코쓰리사원)
--
-- 체인: A --지정--> B --재지정--> C   (SHARE 2행)
--   → M1 핸들 검증 포인트: B와 C가 USER_CD '20260700001' 로 동일. 옛 설계면 대리입실이
--     TBM_403_040 으로 거부됐어야 하고, 핸들 설계면 회사가 봉인돼 정상 구분되어야 한다.
--
-- 세션은 STATUS='OPENED' + GPS 'DISABLED' + ENTRY_PWD='1234' 로 만들어 옵션 1(라이브 입실)에서
--   GPS 없이 바로 입실 테스트가 가능하게 한다.
--
-- MANAGER_USER_CD 는 표시/소유판정에 무관한 값이라 001 관리자 '20260400010' 로 둔다(필요 시 교체).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- [1] 테스트 세션 (A=001 개설, OPENED)
-- -----------------------------------------------------------------------------
INSERT INTO `tb_tbm_session` (
      `SESSION_CD`, `CMPNY_CD`, `SITE_CD`, `EDU_TYPE_CD`, `TITLE`
    , `CONTENT_FORMAT_CD`, `STATUS_CD`, `ENTRY_PWD`
    , `MANAGER_USER_CD`, `GPS_VERIFY_TYPE_CD`, `GPS_VERIFY_RADIUS_M`, `GPS_MANUAL_CONFIRM_YN`
    , `OPENED_AT`, `DEL_YN`, `INSERT_NO`, `INSERT_DATE`, `UPDATE_NO`, `UPDATE_DATE`
) VALUES (
      'T5E2EREAD0001', '001', '00001', 'TBM', '[T5 E2E] 연동 세션 검증'
    , 'RICH_HTML', 'OPENED', '1234'
    , '20260400010', 'DISABLED', 100, 'N'
    , NOW(), 'N', 'SYSTEM', NOW(), 'SYSTEM', NOW()
);

-- -----------------------------------------------------------------------------
-- [2] SHARE — A→B (개설사 직접 지정), B→C (재지정)
--     insertShare 매퍼와 동일한 컬럼/값 형태. HOST_CMPNY_CD 는 항상 개설사(001).
-- -----------------------------------------------------------------------------
INSERT INTO `tb_tbm_session_share` (
      `SESSION_CD`, `HOST_CMPNY_CD`, `SHARE_CMPNY_CD`, `DESIGNATED_BY_CMPNY_CD`
    , `DESIGNATED_BY_USER_CD`, `DESIGNATED_DTIME`, `DEL_YN`, `INSERT_NO`, `INSERT_DATE`
) VALUES
      ('T5E2EREAD0001', '001', 'mLP5JWe5EOFPS17zZOKj', '001'                 , '20260400010', NOW(), 'N', '20260400010', NOW())  -- A→B
    , ('T5E2EREAD0001', '001', 'IqzQKPKMpu75RsCoDN6m', 'mLP5JWe5EOFPS17zZOKj', '20260700001', NOW(), 'N', '20260700001', NOW()); -- B→C(재지정)

-- -----------------------------------------------------------------------------
-- [3] B↔C ACCEPTED 관계 (옵션 1 라이브에서 재지정/관계해지 캐스케이드 테스트에 필요)
--     ACTIVE_PAIR_KEY 는 STORED GENERATED 라 INSERT 하지 않는다(자동 계산).
-- -----------------------------------------------------------------------------
INSERT INTO `tb_cmpny_relation` (
      `REQ_CMPNY_CD`, `TGT_CMPNY_CD`, `STATUS`, `REQ_USER_CD`
    , `PROCESS_CMPNY_CD`, `PROCESS_USER_CD`, `PROCESS_DTIME`, `PROCESS_COMMENT`
    , `INSERT_NO`, `INSERT_DATE`
) VALUES (
      'mLP5JWe5EOFPS17zZOKj', 'IqzQKPKMpu75RsCoDN6m', 'ACCEPTED', '20260700001'
    , 'IqzQKPKMpu75RsCoDN6m', '20260700001', NOW(), '[T5 E2E] 픽스처'
    , 'SYSTEM', NOW()
);

-- =============================================================================
-- [검증용 조회 — 참고. 메인 세션(Claude)이 MCP read-only 로 대신 수행한다]
--   -- 체인: SELECT SHARE_CMPNY_CD, DESIGNATED_BY_CMPNY_CD FROM tb_tbm_session_share WHERE SESSION_CD='T5E2EREAD0001' AND DEL_YN='N';
-- =============================================================================


-- #############################################################################
-- [CLEANUP] E2E 종료 후 실행 — 픽스처 완전 제거 (옵션 1에서 만든 참석/이벤트도 함께 정리)
--   safe update mode 회피: DELETE WHERE 가 PK 선두 아님 → 세션 한정 해제.
-- #############################################################################
-- SET @OLD_SU = @@SQL_SAFE_UPDATES; SET SQL_SAFE_UPDATES = 0;
--
-- DELETE FROM `tb_tbm_attendance_event` WHERE `ATTENDANCE_CD` IN
--        (SELECT `ATTENDANCE_CD` FROM `tb_tbm_attendance` WHERE `SESSION_CD`='T5E2EREAD0001');
-- DELETE FROM `tb_tbm_attendance`       WHERE `SESSION_CD`='T5E2EREAD0001';
-- DELETE FROM `tb_tbm_session_share`    WHERE `SESSION_CD`='T5E2EREAD0001';
-- DELETE FROM `tb_tbm_session`          WHERE `SESSION_CD`='T5E2EREAD0001';
-- DELETE FROM `tb_cmpny_relation`
--        WHERE `REQ_CMPNY_CD`='mLP5JWe5EOFPS17zZOKj' AND `TGT_CMPNY_CD`='IqzQKPKMpu75RsCoDN6m'
--          AND `PROCESS_COMMENT`='[T5 E2E] 픽스처';
--
-- SET SQL_SAFE_UPDATES = @OLD_SU;
-- #############################################################################
