-- =============================================================================
-- PRAFTA-SUBCON-T6-01 : 기존 점검 응답 / 불량조치의 수행주체 backfill
--
-- 적용 순서 : prafta-subcon-t6-1-chkpt-link-ddl.sql 적용 후 실행.
--
-- 기존 행은 전부 자사 단독 수행분이므로 INSERT_NO(입력자)가 곧 수행자다.
-- FNC_CMM_INFO_SRCH 는 회사 스코프 조회라 자사 행에는 안전하다.
-- 성명 조회에 실패해 NULL 로 남는 행(퇴사/삭제 사용자 등)은 화면에서 PERFORM_USER_CD 로 폴백 표시한다.
--
-- [정책 변경] 후행 덮어쓰기 전환으로 소유 판정용 PERFORM_KEY / ACTION_KEY 는 폐기됐다(컬럼 자체 삭제).
--   backfill 은 표시용 3컬럼(PERFORM_CMPNY_CD/USER_CD/USER_NM, 조치의 ACTION_*)만 채운다.
-- =============================================================================

UPDATE `tb_chkpt_inspect_answer`
   SET `PERFORM_CMPNY_CD` = `CMPNY_CD`
     , `PERFORM_USER_CD`  = `INSERT_NO`
     , `PERFORM_USER_NM`  = FNC_CMM_INFO_SRCH(`CMPNY_CD`, 'USER_NM', `INSERT_NO`, NULL)
 WHERE `PERFORM_USER_CD` IS NULL
   AND `INSERT_NO` IS NOT NULL;

UPDATE `tb_chkpt_defect_action`
   SET `ACTION_CMPNY_CD` = `CMPNY_CD`
     , `ACTION_USER_CD`  = `INSERT_NO`
     , `ACTION_USER_NM`  = FNC_CMM_INFO_SRCH(`CMPNY_CD`, 'USER_NM', `INSERT_NO`, NULL)
 WHERE `ACTION_USER_CD` IS NULL
   AND `INSERT_NO` IS NOT NULL;

-- =============================================================================
-- 검증
--   SELECT COUNT(*) FROM tb_chkpt_inspect_answer WHERE PERFORM_USER_CD IS NULL;  -- INSERT_NO NULL 행 수와 동일해야 한다
--   SELECT COUNT(*) FROM tb_chkpt_defect_action  WHERE ACTION_USER_CD  IS NULL;  -- 동일
-- =============================================================================
