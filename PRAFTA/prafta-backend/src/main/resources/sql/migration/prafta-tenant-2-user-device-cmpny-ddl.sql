-- =====================================================================
-- PRAFTA 멀티테넌시 결함 수정 (2/3) — tb_user_device 에 회사코드 추가
--   작성: 2026-07-12
--
--   배경(치명):
--     USER_CD 는 회사별 채번이라 **전역 유일하지 않다**. 실제로 서로 다른 신규 고객사 2곳이
--     모두 USER_CD='20260700001' 을 갖는다(실증). 그런데 tb_user_device 에는 CMPNY_CD 가 없어
--     USER_CD 만으로 기기를 찾는 쿼리가 있었다:
--       - PushOutboxMapper.selectDeviceTokens : 다른 회사 동일 USER_CD 사용자의 기기로 **푸시 오배송**
--       - LoginMapper.deactivateOtherUserDevices : 다른 회사 사용자의 기기를 **강제 로그아웃(비활성)**
--     기존 코드 주석("tb_user_device 엔 CMPNY_CD 없음 = 글로벌 유니크")의 전제 자체가 틀렸다.
--
--   조치: CMPNY_CD 컬럼 추가 + 백필 + 조회 인덱스 재정의. PK(DEVICE_UUID)는 유지한다
--         (기기 1대 = 1행 모델. 기기가 다른 회사 사용자에게 넘어가면 그 행의 CMPNY_CD 가 갱신된다).
--
--   ★ 백필 안전성 사전 확인 완료: tb_user_device 11행 전부 tb_user 로 회사 확정 가능,
--     USER_CD 가 2개 이상 회사에 걸치는 모호 행 0건. 실행 전 §1 로 재확인할 것.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 사전 점검 — 백필 모호성(같은 USER_CD 가 여러 회사에 존재) 확인. 반드시 0행이어야 한다.
-- ---------------------------------------------------------------------
SELECT D.USER_CD, COUNT(DISTINCT U.CMPNY_CD) AS company_cnt
  FROM tb_user_device D
  JOIN tb_user U ON U.USER_CD = D.USER_CD
 GROUP BY D.USER_CD
HAVING company_cnt > 1;

-- ---------------------------------------------------------------------
-- 2. 컬럼 추가 + 백필
-- ---------------------------------------------------------------------
ALTER TABLE tb_user_device
    ADD COLUMN CMPNY_CD VARCHAR(50) NOT NULL DEFAULT '' COMMENT '회사코드' AFTER DEVICE_UUID;

UPDATE tb_user_device D
  JOIN tb_user U ON U.USER_CD = D.USER_CD
   SET D.CMPNY_CD = U.CMPNY_CD;

-- 백필 누락(소유자 미상) 확인 — 0 이어야 한다.
SELECT COUNT(*) AS unresolved FROM tb_user_device WHERE CMPNY_CD = '';

-- ---------------------------------------------------------------------
-- 3. 인덱스 재정의 — 조회는 항상 (회사, 사용자) 로 좁힌다.
-- ---------------------------------------------------------------------
ALTER TABLE tb_user_device DROP INDEX idx_user_device_user;
ALTER TABLE tb_user_device ADD INDEX idx_user_device_user (CMPNY_CD, USER_CD);

-- ---------------------------------------------------------------------
-- 4. 검증 — 회사코드가 채워졌고 인덱스가 (CMPNY_CD, USER_CD) 인지.
-- ---------------------------------------------------------------------
SELECT CMPNY_CD, COUNT(*) AS devices FROM tb_user_device GROUP BY CMPNY_CD;

SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLS
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user_device'
 GROUP BY INDEX_NAME;

-- ---------------------------------------------------------------------
-- 9. 롤백(필요 시)
-- ---------------------------------------------------------------------
-- ALTER TABLE tb_user_device DROP INDEX idx_user_device_user;
-- ALTER TABLE tb_user_device ADD INDEX idx_user_device_user (USER_CD);
-- ALTER TABLE tb_user_device DROP COLUMN CMPNY_CD;
