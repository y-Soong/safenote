-- =============================================================================
-- PRAFTA-SUBCON-T6-01 : 순회점검 구성 연동 + 점검 결과 통합(write-through) DDL
--
-- 적용 순서
--   1) prafta-subcon-t6-1-chkpt-link-ddl.sql   (본 파일)
--   2) prafta-subcon-t6-2-perform-backfill.sql (수행주체 backfill)
--
-- 선행 : T0(문항 사업장 분리) + T2(사업장 미러, tb_site_link) 적용 완료.
--
-- 계약 확장 근거(메인 세션 확정 — Q1 승인)
--   - 마스터 §2.1/§2.2 계약에 없던 컬럼 3묶음을 추가한다.
--     (1) tb_site_link 점검연동 상태 3컬럼 : 사업장 연동 위에 점검 구성 연동을 별도 on/off 하기 위함.
--         점검 연동은 사업장 링크에 1:1 종속(사업장 링크 없이 존재 불가)이라 별도 링크 테이블을 두지 않는다.
--     (2) tb_chkpt_inspect_answer 수행주체 3컬럼 : 요청서 §2.4(점검자 성명+ID 표시 / 수행 주체 relabel) 구현에 필수.
--         현행 테이블에는 점검자 컬럼이 아예 없고 INSERT_NO(입력자)뿐인데, 전파 행의 INSERT_NO 는 'SYSTEM' 이 되므로
--         사용자 표시 근거로 쓸 수 없다.
--     (3) tb_chkpt_defect_action 조치주체 3컬럼 + 조치사진 1컬럼 : 조치 수행자 표시 및 조치 사진 전파.
--
-- [정책 변경 이력 — 후행 덮어쓰기(last-writer-wins) 전환]
--   최초 설계는 "선수행 우선(먼저 점검/조치한 쪽이 완료, 후행은 차단)"이었고, 이를 위해
--   PERFORM_KEY / ACTION_KEY(수행/조치 주체 불투명 키) 컬럼으로 소유를 판정했다.
--   사용자 확정으로 정책을 뒤집는다: 후행 데이터가 선행 데이터를 덮어쓴다(무조건 UPSERT).
--   소유 판정이 사라지므로 PERFORM_KEY / ACTION_KEY 컬럼과 OwnerKeyHasher 를 제거했다.
--   대신 앱 저장 시 기존 데이터가 있으면 "덮어쓰시겠습니까?" 확인 팝업으로 사용자 동의를 받고,
--   선행 점검자를 성명(ID)으로 표시한다(표시 근거 = 로드 응답의 PERFORM_USER_NM/PERFORM_USER_CD).
--   PERFORM_CMPNY_CD/PERFORM_USER_CD/PERFORM_USER_NM(및 조치의 ACTION_*) 3컬럼은 유지한다
--   ("마지막에 누가 수행했는지" 표시용, 덮어쓸 때마다 최신 수행자로 갱신).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- (1) 점검 구성 연동 상태 — 사업장 링크(T2) 위에 얹는 별도 on/off.
--     사업장만 연동하고 점검은 각자 운영하는 경우를 허용한다.
-- -----------------------------------------------------------------------------
ALTER TABLE `tb_site_link`
    ADD COLUMN `CHKPT_LINK_STATUS`  varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE'
        COMMENT '점검 구성 연동 상태 NONE:미연동, ACTIVE:연동중 (해제 시 NONE 복귀 - 재실행 가능)',
    ADD COLUMN `CHKPT_LINK_DTIME`   datetime    DEFAULT NULL COMMENT '점검 구성 연동 실행/해제 일시',
    ADD COLUMN `CHKPT_LINK_USER_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '점검 구성 연동 처리자(USER_CD)';

-- -----------------------------------------------------------------------------
-- (2) 점검대상 출처(마스터 §2.2 LINK_SRC 계약 확장).
--     NULL=자체 점검대상, NOT NULL=미러(수정 잠금 - 담당자 지정만 허용).
--     미러 CHKPT_CD 는 수신 회사 시퀀스로 신규 채번하므로(plan D1) 원본 코드를 LINK_SRC_CHKPT_CD 로 보관한다.
--
--     PREV_LINK_SRC_* (보안검토 M2) : 점검연동 해제(독립화) 시 LINK_SRC_* 를 여기로 옮겨 보관한다.
--       독립화가 LINK_SRC 를 NULL 로만 만들고 행은 남기므로, 재연동 시 원본 좌표를 잃으면 미러가 새 코드로
--       다시 INSERT 되어 수신 테넌트에 무제한 중복이 쌓인다. 재연동은 이 컬럼으로 기존 행을 찾아 재귀속한다.
-- -----------------------------------------------------------------------------
ALTER TABLE `tb_chkpt_type_mgmt`
    ADD COLUMN `LINK_SRC_CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 회사코드 - NULL=자체, NOT NULL=미러(수정 잠금, 담당자 지정만 허용)',
    ADD COLUMN `LINK_SRC_SITE_CD`  varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 사업장코드',
    ADD COLUMN `LINK_SRC_CHKPT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 점검대상코드',
    ADD COLUMN `PREV_LINK_SRC_CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '직전 연동 원본 회사코드(독립화 시 보관 - 재연동 재귀속용)',
    ADD COLUMN `PREV_LINK_SRC_SITE_CD`  varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '직전 연동 원본 사업장코드(독립화 시 보관)',
    ADD COLUMN `PREV_LINK_SRC_CHKPT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '직전 연동 원본 점검대상코드(독립화 시 보관)',
    ADD KEY `IX_CHKPT_TYPE_LINK_SRC` (`LINK_SRC_CMPNY_CD`, `LINK_SRC_SITE_CD`, `LINK_SRC_CHKPT_CD`),
    ADD KEY `IX_CHKPT_TYPE_PREV_LINK_SRC` (`PREV_LINK_SRC_CMPNY_CD`, `PREV_LINK_SRC_SITE_CD`, `PREV_LINK_SRC_CHKPT_CD`);

-- -----------------------------------------------------------------------------
-- (3) 점검문항 출처(동일 규칙 - 미러 문항은 전면 수정 잠금). PREV_LINK_SRC_* 규칙은 (2)와 동일.
-- -----------------------------------------------------------------------------
ALTER TABLE `tb_chkpt_inspect_item`
    ADD COLUMN `LINK_SRC_CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 회사코드 - NULL=자체, NOT NULL=미러(수정 전면 잠금)',
    ADD COLUMN `LINK_SRC_SITE_CD`  varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 사업장코드',
    ADD COLUMN `LINK_SRC_ITEM_CD`  varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연동 원본(직상위) 점검문항코드',
    ADD COLUMN `PREV_LINK_SRC_CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '직전 연동 원본 회사코드(독립화 시 보관 - 재연동 재귀속용)',
    ADD COLUMN `PREV_LINK_SRC_SITE_CD`  varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '직전 연동 원본 사업장코드(독립화 시 보관)',
    ADD COLUMN `PREV_LINK_SRC_ITEM_CD`  varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '직전 연동 원본 점검문항코드(독립화 시 보관)',
    ADD KEY `IX_CHKPT_ITEM_LINK_SRC` (`LINK_SRC_CMPNY_CD`, `LINK_SRC_SITE_CD`, `LINK_SRC_ITEM_CD`),
    ADD KEY `IX_CHKPT_ITEM_PREV_LINK_SRC` (`PREV_LINK_SRC_CMPNY_CD`, `PREV_LINK_SRC_SITE_CD`, `PREV_LINK_SRC_ITEM_CD`);

-- -----------------------------------------------------------------------------
-- (3-1) [qa M-1] 미러 문항 시행일(STR_DATE) 규칙 — 컬럼 변경 없음(규칙만 명문화).
--     미러 STR_DATE = GREATEST(원본 STR_DATE, 점검연동 실행일(KST)).
--       원본 시행일을 그대로 복제하면 연동 이전 기간까지 수신사 확인서(ChkLstRstPop)에서
--       "점검 의무가 있었던 흰 셀"로 표시된다(수신사에는 그때 그 점검대상이 존재하지도 않았다).
--       수신사 기준 의무는 연동 시점부터 발생한다 -> plan §4-1/T6-04 "시행일 원본 그대로" 문구는 폐기.
--       선례: T0 "타 사업장 문항 가져오기"도 STR_DATE = 실행일(KST) 로 세팅한다(ChkLst02ServiceImpl).
--     전파(원본 개정) 시에도 하한을 유지한다: GREATEST(원본 STR_DATE, 미러 행 INSERT_DATE(=그 티어 연동일)).
--       -> 원본이 시행일을 과거로 내려도 미러 시행일은 연동일 밑으로 재하강하지 않는다.
--     미러 문항 HIST(01 등록 / 02 수정)는 저장 직후 미러 행을 그대로 스냅샷하므로 동일 값이 기록된다
--       (확인서 회색 게이팅이 이력 기반이라 필수).
--     구현: ChkptLinkMapper.xml insertMirrorInspectItem / propagateMirrorInspectItem.
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
-- (4) 점검문항 이력 출처(미러 이력 식별용).
--     이력 행은 불변이므로 독립화(연동 해제) 시에도 NULL 화하지 않는다.
-- -----------------------------------------------------------------------------
ALTER TABLE `tb_chkpt_inspect_item_hist`
    ADD COLUMN `LINK_SRC_CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경 시점의 연동 원본 회사코드(미러 이력 식별 - 이력 불변)',
    ADD COLUMN `LINK_SRC_SITE_CD`  varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경 시점의 연동 원본 사업장코드',
    ADD COLUMN `LINK_SRC_ITEM_CD`  varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경 시점의 연동 원본 문항코드';

-- -----------------------------------------------------------------------------
-- (5) 점검 응답 수행주체(신설).
--     PERFORM_CMPNY_CD 는 "그 테넌트에서 보이는" 수행 회사다 = 인접 1차 relabel 값(마스터 §1-3 가시성).
--       -> 2차 이하 회사코드가 조상 테넌트에 물리적으로 남지 않는다.
--     PERFORM_USER_NM 은 저장 시점 스냅샷 문자열이다.
--       -> 타사 USER_CD 를 자기 회사 사용자 테이블에 조인하면 동명이인/빈값 오표시가 나므로 조인 금지.
--     PERFORM_USER_CD 는 원 수행자 코드 그대로 보관한다(표시 전용). 앱 로드 응답이 "성명(ID)" 확인 팝업에
--       쓰는 값이다(선행 점검자 표시). 조인 금지.
--
--     [정책 변경] 후행 덮어쓰기 전환으로 소유 판정용 PERFORM_KEY 컬럼은 두지 않는다.
--       저장/전파는 무조건 UPSERT(덮어쓰기)이며, 덮어쓸 때마다 아래 3컬럼을 최신 수행자로 갱신한다.
--       relabel 규칙(타 티어 복제행 = 인접 1차 회사, 2차 이하 회사코드 비노출)은 그대로 유지한다.
--     IX_CHKPT_ANSWER_PERFORM 은 수행 회사/사용자 기준 조회(불량목록 수행회사 표시 등)를 위해 유지한다.
-- -----------------------------------------------------------------------------
ALTER TABLE `tb_chkpt_inspect_answer`
    ADD COLUMN `PERFORM_CMPNY_CD` varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수행(점검) 회사코드 - 타 티어 복제행은 인접 1차 회사로 relabel (표시 전용)',
    ADD COLUMN `PERFORM_USER_CD`  varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수행(점검)자 USER_CD - 원 수행자 코드 그대로(표시 전용, 조인 금지)',
    ADD COLUMN `PERFORM_USER_NM`  varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수행(점검)자 성명 스냅샷 - 표시용',
    ADD KEY `IX_CHKPT_ANSWER_PERFORM` (`CMPNY_CD`, `PERFORM_CMPNY_CD`, `PERFORM_USER_CD`);

-- -----------------------------------------------------------------------------
-- (6) 불량조치 조치주체 + 조치사진(신설). relabel 규칙은 (5)와 동일.
--     [정책 변경] 후행 덮어쓰기 전환으로 소유 판정용 ACTION_KEY 컬럼은 두지 않는다(무조건 UPSERT).
--     FILE_MGMT_CD(조치 사진, 요청서 §2.4 "조치 상태·사진 전파") : 점검응답 사진(tb_chkpt_inspect_answer.FILE_MGMT_CD)과
--       동일 패턴. 미첨부면 NULL(텍스트만). 전파 시 각 티어 소유 파일로 복제한다(FILE_TYPE=SYS010 '006').
-- -----------------------------------------------------------------------------
ALTER TABLE `tb_chkpt_defect_action`
    ADD COLUMN `FILE_MGMT_CD`    varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '조치 첨부사진코드 - 미첨부 NULL, 전파 시 티어별 소유 파일로 복제(FILE_TYPE SYS010 006)',
    ADD COLUMN `ACTION_CMPNY_CD` varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '조치 회사코드 - 타 티어 복제행은 인접 1차 회사로 relabel (표시 전용)',
    ADD COLUMN `ACTION_USER_CD`  varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '조치자 USER_CD(표시 전용, 조인 금지)',
    ADD COLUMN `ACTION_USER_NM`  varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '조치자 성명 스냅샷';

-- =============================================================================
-- 롤백
-- =============================================================================
-- ALTER TABLE `tb_chkpt_defect_action`
--     DROP COLUMN `FILE_MGMT_CD`, DROP COLUMN `ACTION_CMPNY_CD`, DROP COLUMN `ACTION_USER_CD`,
--     DROP COLUMN `ACTION_USER_NM`;
-- ALTER TABLE `tb_chkpt_inspect_answer`
--     DROP KEY `IX_CHKPT_ANSWER_PERFORM`,
--     DROP COLUMN `PERFORM_CMPNY_CD`, DROP COLUMN `PERFORM_USER_CD`, DROP COLUMN `PERFORM_USER_NM`;
-- ALTER TABLE `tb_chkpt_inspect_item_hist`
--     DROP COLUMN `LINK_SRC_CMPNY_CD`, DROP COLUMN `LINK_SRC_SITE_CD`, DROP COLUMN `LINK_SRC_ITEM_CD`;
-- ALTER TABLE `tb_chkpt_inspect_item`
--     DROP KEY `IX_CHKPT_ITEM_LINK_SRC`, DROP KEY `IX_CHKPT_ITEM_PREV_LINK_SRC`,
--     DROP COLUMN `LINK_SRC_CMPNY_CD`, DROP COLUMN `LINK_SRC_SITE_CD`, DROP COLUMN `LINK_SRC_ITEM_CD`,
--     DROP COLUMN `PREV_LINK_SRC_CMPNY_CD`, DROP COLUMN `PREV_LINK_SRC_SITE_CD`, DROP COLUMN `PREV_LINK_SRC_ITEM_CD`;
-- ALTER TABLE `tb_chkpt_type_mgmt`
--     DROP KEY `IX_CHKPT_TYPE_LINK_SRC`, DROP KEY `IX_CHKPT_TYPE_PREV_LINK_SRC`,
--     DROP COLUMN `LINK_SRC_CMPNY_CD`, DROP COLUMN `LINK_SRC_SITE_CD`, DROP COLUMN `LINK_SRC_CHKPT_CD`,
--     DROP COLUMN `PREV_LINK_SRC_CMPNY_CD`, DROP COLUMN `PREV_LINK_SRC_SITE_CD`, DROP COLUMN `PREV_LINK_SRC_CHKPT_CD`;
-- ALTER TABLE `tb_site_link`
--     DROP COLUMN `CHKPT_LINK_STATUS`, DROP COLUMN `CHKPT_LINK_DTIME`, DROP COLUMN `CHKPT_LINK_USER_CD`;
