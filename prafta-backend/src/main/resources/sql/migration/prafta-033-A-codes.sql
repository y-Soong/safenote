-- ============================================================================
-- PRAFTA-033-A — 공통코드 시드 (SYS018 PDF 추가 + 신규 SYS046~SYS055)
-- 작성일: 2026-05-27
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/prafta-033-A-ddl-content.md §4
--       시드 패턴: prafta-031-sys045-noti-type.sql
--
-- 변경 요약
--  1) SYS018(교육자료 항목 타입)에 '04' PDF 추가. 기존 01 이미지/02 동영상/03 유튜브URL 유지.
--  2) 신규 SYS046~SYS055 그룹 시드(tb_syst_val_m 마스터 1건 + tb_syst_val_d 상세 N건).
--     - 본 A 단계가 생성하는 세션/출결 테이블의 코드성 컬럼 카탈로그.
--
-- 신규 SYS 그룹 (확정, 현재 최대 SYS045):
--   SYS046 TBM 세션 상태 / SYS047 TBM 교육 유형 / SYS048 TBM GPS 검증유형
--   SYS049 TBM 동기화 상태 / SYS050 TBM 출결 대상유형 / SYS051 TBM 입실 경로
--   SYS052 TBM 종료 경로 / SYS053 TBM 이수 상태 / SYS054 TBM 출결 이벤트 유형
--   SYS055 TBM 비번 유형
--
-- 적용 전 부재 확인:
--   SELECT 1 FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS018' AND SYST_VAL_D_CD='04';
--   SELECT SYST_VAL_CD FROM tb_syst_val_m WHERE SYST_VAL_CD IN ('SYS046','SYS055');
-- 멱등성: PK 중복 시 에러. 운영 적용 후 보관용(재실행 금지). 이미 존재 시 건너뛸 것.
-- ⚠️ 운영 적용은 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) SYS018 교육자료 항목 타입 — '04' PDF 추가
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS018', '04', 'PDF', 4, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (2) SYS046 TBM 세션 상태
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS046', 'TBM 세션 상태', 'Y', 'tb_tbm_session.STATUS_CD 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS046', 'DRAFT', '작성중', 1, 'Y', 'SYSTEM'),
  ('SYS046', 'OPENED', '개설', 2, 'Y', 'SYSTEM'),
  ('SYS046', 'IN_PROGRESS', '진행중', 3, 'Y', 'SYSTEM'),
  ('SYS046', 'COMPLETED', '종료', 4, 'Y', 'SYSTEM'),
  ('SYS046', 'CANCELLED', '취소', 5, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (3) SYS047 TBM 교육 유형
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS047', 'TBM 교육 유형', 'Y', 'tb_tbm_session.EDU_TYPE_CD 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS047', 'TBM', '툴박스미팅', 1, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (4) SYS048 TBM GPS 검증유형
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS048', 'TBM GPS 검증유형', 'Y', 'tb_tbm_session.GPS_VERIFY_TYPE_CD 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS048', 'AUTO', '자동', 1, 'Y', 'SYSTEM'),
  ('SYS048', 'MANUAL', '수동확인', 2, 'Y', 'SYSTEM'),
  ('SYS048', 'DISABLED', '비활성', 3, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (5) SYS049 TBM 동기화 상태
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS049', 'TBM 동기화 상태', 'Y', 'tb_tbm_session_state.SYNC_STATE_CD 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS049', 'PLAYING', '재생', 1, 'Y', 'SYSTEM'),
  ('SYS049', 'PAUSED', '정지', 2, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (6) SYS050 TBM 출결 대상유형
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS050', 'TBM 출결 대상유형', 'Y', 'tb_tbm_attendance.USER_TYPE_CD 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS050', 'REGULAR', '정규직', 1, 'Y', 'SYSTEM'),
  ('SYS050', 'DAILY', '일용직', 2, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (7) SYS051 TBM 입실 경로
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS051', 'TBM 입실 경로', 'Y', 'tb_tbm_attendance.ENTRY_TYPE_CD 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS051', 'SELF_DEVICE', '본인디바이스', 1, 'Y', 'SYSTEM'),
  ('SYS051', 'MANAGER_QR_SCAN', '관리자QR', 2, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (8) SYS052 TBM 종료 경로
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS052', 'TBM 종료 경로', 'Y', 'tb_tbm_attendance.EXIT_TYPE_CD 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS052', 'SELF', '본인', 1, 'Y', 'SYSTEM'),
  ('SYS052', 'MANAGER_QR_SCAN', '관리자QR', 2, 'Y', 'SYSTEM'),
  ('SYS052', 'MANAGER_FORCED', '관리자강제', 3, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (9) SYS053 TBM 이수 상태
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS053', 'TBM 이수 상태', 'Y', 'tb_tbm_attendance.COMPLETION_STATUS_CD 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS053', 'COMPLETED', '이수', 1, 'Y', 'SYSTEM'),
  ('SYS053', 'NOT_COMPLETED', '미이수', 2, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (10) SYS054 TBM 출결 이벤트 유형
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS054', 'TBM 출결 이벤트 유형', 'Y', 'tb_tbm_attendance_event.EVENT_TYPE_CD 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS054', 'ENTER', '입실', 1, 'Y', 'SYSTEM'),
  ('SYS054', 'START', '교육시작', 2, 'Y', 'SYSTEM'),
  ('SYS054', 'SLIDE_CHANGED', '슬라이드변경', 3, 'Y', 'SYSTEM'),
  ('SYS054', 'GPS_UPDATED', 'GPS갱신', 4, 'Y', 'SYSTEM'),
  ('SYS054', 'BACKGROUND_IN', '백그라운드진입', 5, 'Y', 'SYSTEM'),
  ('SYS054', 'BACKGROUND_OUT', '백그라운드복귀', 6, 'Y', 'SYSTEM'),
  ('SYS054', 'NETWORK_LOST', '네트워크끊김', 7, 'Y', 'SYSTEM'),
  ('SYS054', 'SIGNATURE_STARTED', '서명시작', 8, 'Y', 'SYSTEM'),
  ('SYS054', 'END', '종료', 9, 'Y', 'SYSTEM'),
  ('SYS054', 'FORCED_END', '강제종료', 10, 'Y', 'SYSTEM');

-- ----------------------------------------------------------------------------
-- (11) SYS055 TBM 비번 유형
-- ----------------------------------------------------------------------------
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS055', 'TBM 비번 유형', 'Y', 'tb_tbm_pwd_fail.PWD_TYPE_CD 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS055', 'ENTRY', '입실', 1, 'Y', 'SYSTEM'),
  ('SYS055', 'EXIT', '종료', 2, 'Y', 'SYSTEM');
