-- prafta-057: 다른 환경 로그인 감지 — TB_AUTH_TOKEN 로그인 세션 패밀리 식별자(LOGIN_ID)
--
-- 목적:
--   회전(refresh) 시 승계되고 신규 로그인 시 새로 발급되는 "로그인 세션 패밀리" 식별자.
--   매 요청 AuthAspect 가 "현재 토큰 LOGIN_ID 패밀리에 활성 토큰이 남아있는지"로
--   다른 환경 신규 로그인(=기존 세션 RT 폐기)을 감지하여 즉시 강제 로그아웃한다.
--   같은 환경 멀티탭은 회전이 LOGIN_ID 를 승계하므로 오탐(자기 탭 강제 로그아웃)이 없다.
--
-- 운영 적용 순서: 본 DDL 적용 → 백엔드 재배포. (컬럼 nullable + 백필이라 무중단 안전.)

ALTER TABLE TB_AUTH_TOKEN
  ADD COLUMN LOGIN_ID varchar(50)
      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL
      COMMENT '로그인 세션 패밀리 식별자(회전 시 승계, 신규 로그인 시 신규 발급)'
      AFTER TOKEN_ID;

-- 기존 행 백필: 배포 이전 발급 토큰은 자기 TOKEN_ID 를 패밀리로 간주(각자 단일 세션).
--   배포 이전 발급된 액세스 토큰에는 LOGIN_ID 클레임이 없어 AuthAspect 검사 대상에서 제외되며,
--   다음 로그인/회전부터 정상적으로 패밀리가 부여된다.
-- MySQL Workbench 안전 업데이트 모드(Error 1175) 회피: LOGIN_ID 는 키 컬럼이 아니라
--   일괄 백필 UPDATE 가 막힌다. 세션 한정으로 안전모드를 잠시 해제한 뒤 복구한다.
SET SQL_SAFE_UPDATES = 0;
UPDATE TB_AUTH_TOKEN
   SET LOGIN_ID = TOKEN_ID
 WHERE LOGIN_ID IS NULL;
SET SQL_SAFE_UPDATES = 1;

-- 감지 조회 인덱스: (회사, 사용자, 패밀리, 폐기여부, 만료시각) 활성 카운트 / 다른 패밀리 활성 카운트용.
--   매 WEB 요청 경로의 countActiveByLoginId / countActiveOtherLogin 이 EXPIRE_DTIME > NOW() 까지
--   인덱스 온리로 평가되도록 EXPIRE_DTIME 을 후행 컬럼으로 포함한다.
ALTER TABLE TB_AUTH_TOKEN
  ADD KEY IX_TOKEN_LOGIN (CMPNY_CD, USER_CD, LOGIN_ID, REVOKED_YN, EXPIRE_DTIME);
