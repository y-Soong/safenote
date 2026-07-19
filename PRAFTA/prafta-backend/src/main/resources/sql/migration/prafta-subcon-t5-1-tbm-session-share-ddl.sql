-- ============================================================================
-- PRAFTA-SUBCON-T5-1 — TBM 연동 회사 지정(+재지정 체인) DDL
-- 출처: PRAFTA-SUBCON.md §2.1 / §1-9 / §1-3, T5 요청서 §2, T5.plan §0-5(C1·C2)
-- 선행: prafta-subcon-t1-1-relation-ddl.sql (tb_cmpny_relation)
-- 적용: 사용자 수동. 본 파일은 작성만(개발 세션은 read-only). 재실행 금지(멱등 아님).
-- 적용 전 부재 확인: SHOW TABLES LIKE 'tb_tbm_session_share';
--
-- ⚠ Option A(메인 세션 승인 2026-07-14) 동반 적용:
--    실 DB 의 tb_tbm_session / tb_tbm_attendance 는 PK 가 (CMPNY_CD, SESSION_CD) /
--    (CMPNY_CD, ATTENDANCE_CD) 복합키이며(prafta-tenant-1-composite-pk-ddl.sql),
--    SESSION_CD / ATTENDANCE_CD 는 FNC_CMM_SEQ_NEXTVAL(회사별 채번) 이라 전역 유일이 아니다.
--    T5 는 회사 경계를 넘어 SESSION_CD 단독으로 세션/출결을 식별하므로, 코드의 전역 유일성이
--    전제된다. 아래 §0 에서 전역 유일키 + 전역 채번 시퀀스를 도입해 이 전제를 DB 로 보장한다.
--    (복합 PK 는 그대로 유지 — 변경하지 않는다.)
--
-- ⚠⚠ 배포 순서 하드 의존(반드시 지킬 것) ⚠⚠
--   (1) [SQL 먼저] 본 파일의 §0(특히 §0-3 '__GLOBAL__' 시퀀스 시드)을 애플리케이션 배포 **이전에**
--       실행해야 한다. 새 코드는 TBM 세션/출결 코드를 FNC_CMM_SEQ_NEXTVAL('__GLOBAL__', ...) 로
--       채번한다. 시드 행이 없으면 함수가 CURR_VAL=1 부터 시작해 기존 회사별 코드와 충돌하고,
--       신설 UNIQUE 에 걸려 TBM 개설·입실이 즉시 실패한다.
--   (2) [SQL 과 코드는 동시 배포] 반대로 SQL 만 적용하고 구 코드가 돌면(회사별 채번 유지) 두 번째
--       테넌트가 TBM 을 쓰는 순간 코드가 충돌해 UNIQUE 위반이 난다. 롤백 시에도 SQL 롤백과
--       매퍼 채번 인자 롤백을 반드시 함께 되돌린다(파일 하단 롤백 주석 참조).
-- ============================================================================


-- ============================================================================
-- 0. Option A — TBM 세션/출결 코드의 전역 유일성 보장
-- ============================================================================

-- 0-1. 사전 점검(중복이 있으면 0-2 UNIQUE 부여가 실패한다. 반드시 0건이어야 한다).
--      2026-07-14 실측: 세션 10건 / 출결 8건, 회사 '001' 단독 사용 → 중복 0건.
SELECT COUNT(*) AS dup_session_cd
  FROM (
        SELECT SESSION_CD
          FROM tb_tbm_session
         GROUP BY SESSION_CD
        HAVING COUNT(*) > 1
       ) X;

SELECT COUNT(*) AS dup_attendance_cd
  FROM (
        SELECT ATTENDANCE_CD
          FROM tb_tbm_attendance
         GROUP BY ATTENDANCE_CD
        HAVING COUNT(*) > 1
       ) Y;

-- 0-2. 전역 유일키 부여(복합 PK 는 유지. 향후 회사간 코드 충돌을 조용한 데이터 오염이 아니라
--      즉시 실패로 드러낸다 — T5 스코프 쿼리(SESSION_CD 단독)의 안전핀).
ALTER TABLE `tb_tbm_session`
    ADD UNIQUE KEY `UX_TBM_SESSION_CD` (`SESSION_CD`);

ALTER TABLE `tb_tbm_attendance`
    ADD UNIQUE KEY `UX_TBM_ATTENDANCE_CD` (`ATTENDANCE_CD`);

-- 0-3. 전역 채번 시퀀스 시드.
--      FNC_CMM_SEQ_NEXTVAL 은 (CMPNY_CD, SEQ_KEY) 단위로 CURR_VAL 을 증가시킨다. TBM 세션/출결
--      코드만 회사코드 대신 전역 센티넬 '__GLOBAL__' 로 채번하도록 매퍼 5곳을 전환했다.
--      CURR_VAL 은 기존 회사별 최댓값 이상 + 여유(1000)로 시드해 기존 코드와의 충돌을 원천 차단한다.
--      ※ '__GLOBAL__' 는 실제 회사코드로 발급될 수 없는 값이다(tb_cmpny 미등록).
--
--      ⚠ MAX_VAL 을 반드시 명시한다(F3). 함수 본문:
--          CURR_VAL >= MAX_VAL 이면 예외가 아니라 "1 로 되감김(wrap-around)" 한다.
--        테이블 기본값 99999(5자리)를 그대로 쓰면 전역 전환으로 소비 속도가 테넌트 수만큼 빨라지고,
--        같은 날 안에서 되감김이 일어나면 이미 발급된 코드와 동일한 값이 재생성되어 신설 UNIQUE
--        (UX_TBM_SESSION_CD / UX_TBM_ATTENDANCE_CD)에 걸려 개설·입실이 전면 실패한다.
--        → MAX_VAL = 9999999(7자리)로 상향한다.
--
--      ⚠ 코드 길이 변화: 함수는 LPAD(v_next, CHAR_LENGTH(MAX_VAL), '0') 이므로 일련번호가 7자리가 된다.
--        세션코드 'T'(1) + YYYYMMDD(8) + SEQ(7) = 16자, 출결코드 'A' + YYYYMMDD + SEQ = 16자.
--        (기존 14자 → 16자. 두 컬럼 모두 varchar(20) 이라 여유 있음. 코드 길이를 가정하는 로직은
--         백엔드 전수 grep 결과 없음 — 파싱/substring/length 검증 없음.)
--
--      [허용 잔여 위험 — 구조 유지, 기록만]
--        (1) hot row 경합: 전역 행 1개를 모든 테넌트가 UPDATE 하므로 TBM 개설/입실이 폭주하면
--            해당 행에 락 경합이 생길 수 있다(현 트래픽 규모에서는 무시 가능).
--        (2) 사이드채널: 코드의 일련번호 증가폭으로 타 테넌트의 TBM 활동량을 대략 추정할 수 있다
--            (세션/출결 코드는 관리자에게만 노출되며 민감 데이터가 아니므로 수용).
INSERT INTO tb_cmm_seq (CMPNY_CD, SEQ_KEY, CURR_VAL, MAX_VAL)
SELECT '__GLOBAL__'
     , 'TBM_SESSION_CD'
     , IFNULL((SELECT MAX(CURR_VAL) FROM tb_cmm_seq WHERE SEQ_KEY = 'TBM_SESSION_CD'), 0) + 1000
     , 9999999
  FROM DUAL
 WHERE NOT EXISTS (
        SELECT 1
          FROM tb_cmm_seq
         WHERE CMPNY_CD = '__GLOBAL__'
           AND SEQ_KEY  = 'TBM_SESSION_CD'
       );

INSERT INTO tb_cmm_seq (CMPNY_CD, SEQ_KEY, CURR_VAL, MAX_VAL)
SELECT '__GLOBAL__'
     , 'TBM_ATTENDANCE_CD'
     , IFNULL((SELECT MAX(CURR_VAL) FROM tb_cmm_seq WHERE SEQ_KEY = 'TBM_ATTENDANCE_CD'), 0) + 1000
     , 9999999
  FROM DUAL
 WHERE NOT EXISTS (
        SELECT 1
          FROM tb_cmm_seq
         WHERE CMPNY_CD = '__GLOBAL__'
           AND SEQ_KEY  = 'TBM_ATTENDANCE_CD'
       );

-- 0-4. 확인.
SELECT CMPNY_CD, SEQ_KEY, CURR_VAL, MAX_VAL
  FROM tb_cmm_seq
 WHERE SEQ_KEY IN ('TBM_SESSION_CD', 'TBM_ATTENDANCE_CD');


-- ============================================================================
-- 1. TB_TBM_SESSION_SHARE — TBM 세션 연동회사 지정(재지정 체인)
-- ============================================================================

CREATE TABLE `tb_tbm_session_share` (
    `SHARE_ID`               bigint       NOT NULL AUTO_INCREMENT COMMENT '세션 지정ID(PK)',
    `SESSION_CD`             varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TBM 세션코드(tb_tbm_session.SESSION_CD — UX_TBM_SESSION_CD 로 전역 유일)',
    `HOST_CMPNY_CD`          varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '세션 개설 회사코드(비정규화 — 체인 루트 판정. tb_tbm_session.CMPNY_CD 와 동일)',
    `SHARE_CMPNY_CD`         varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '지정된 회사코드(이 회사 소속 근로자가 입실 가능)',
    `DESIGNATED_BY_CMPNY_CD` varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '지정자 회사코드(재지정 체인의 부모. 개설사 직접 지정이면 HOST_CMPNY_CD 와 동일)',
    `DESIGNATED_BY_USER_CD`  varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '지정자 사용자코드(지정자 회사 소속)',
    `DESIGNATED_DTIME`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '지정 일시',
    `DEL_YN`                 varchar(2)   COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '지정 해제여부 Y:해제(신규 입실 차단, 기존 참석 유지) N:유효',
    `RELEASE_REASON_CD`      varchar(20)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '해제 사유구분 MANUAL:수동해제 CASCADE:상위 해제 전파 RELATION_TERMINATED:회사 연동관계 해지',
    `RELEASED_BY_CMPNY_CD`   varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '해제자 회사코드(CASCADE/RELATION_TERMINATED 는 트리거 회사)',
    `RELEASED_BY_USER_CD`    varchar(20)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '해제자 사용자코드(자동 해제는 트리거 행위자)',
    `RELEASED_DTIME`         datetime     DEFAULT NULL COMMENT '해제 일시',
    `INSERT_NO`              varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`            datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`              varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`            datetime     DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`SHARE_ID`),
    -- 마스터 §2.1 계약: UK(SESSION_CD, SHARE_CMPNY_CD). DEL_YN 미포함 →
    --   ① 같은 회사가 한 세션에 두 번 지정될 수 없다(루프/중복 자연 차단),
    --   ② 해제 후 재지정은 INSERT 가 아니라 기존 행 RESTORE(UPDATE) 여야 한다(중복키 함정).
    UNIQUE KEY `UK_TBM_SESSION_SHARE_01` (`SESSION_CD`, `SHARE_CMPNY_CD`),
    KEY `IX_TBM_SESSION_SHARE_01` (`SESSION_CD`, `DEL_YN`),
    KEY `IX_TBM_SESSION_SHARE_02` (`SHARE_CMPNY_CD`, `DEL_YN`, `SESSION_CD`),
    KEY `IX_TBM_SESSION_SHARE_03` (`SESSION_CD`, `DESIGNATED_BY_CMPNY_CD`, `DEL_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션 연동회사 지정(재지정 체인)';


-- ============================================================================
-- 2. 출결 인덱스 보강
-- ============================================================================
-- T5 는 출결을 SESSION_CD 단독으로 조회한다(개설사 콘솔/집계 — 참석자 회사가 여러 개).
-- 기존 인덱스는 (CMPNY_CD, SESSION_CD) 선두라 SESSION_CD 단독 조회를 못 탄다.
ALTER TABLE `tb_tbm_attendance`
    ADD KEY `IX_TBM_ATTENDANCE_03` (`SESSION_CD`, `DEL_YN`);


-- ============================================================================
-- 롤백
-- ============================================================================
--   ALTER TABLE `tb_tbm_attendance` DROP KEY `IX_TBM_ATTENDANCE_03`;
--   DROP TABLE IF EXISTS `tb_tbm_session_share`;
--   DELETE FROM tb_cmm_seq WHERE CMPNY_CD = '__GLOBAL__' AND SEQ_KEY IN ('TBM_SESSION_CD','TBM_ATTENDANCE_CD');
--   ALTER TABLE `tb_tbm_attendance` DROP KEY `UX_TBM_ATTENDANCE_CD`;
--   ALTER TABLE `tb_tbm_session`    DROP KEY `UX_TBM_SESSION_CD`;
--   ※ 롤백 시 매퍼의 채번 인자('__GLOBAL__' → #{gvCmpnyCd})도 함께 되돌려야 한다.
