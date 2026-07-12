-- ============================================================================
-- APP-PRAFTA-001 — 셀프 회원가입 계정 강제 비번변경 게이트 오발동 정상화(backfill)
-- 작성일: 2026-07-08
-- 적용 환경: MySQL 8.0.42
-- 참조: .claude/requests/app_requests/APP-PRAFTA-001.md (2번 항목)
--
-- 배경
--  강제 비밀번호 변경 게이트(AuthMapper.selectGateUserInfo)는 PWD_CHG_DTIME IS NULL 로
--  "비번 변경 미완료(=강제변경 필요)"를 판정한다. 이 게이트는 원래 엑셀/관리자 일괄등록
--  계정(임시비번)만을 대상으로 한 것인데, LoginMapper.insertUserInfo(셀프 회원가입)가
--  PWD_CHG_DTIME 을 세팅하지 않아 셀프가입 정규직도 NULL 로 남아 잘못 게이트에 걸렸다.
--  코드 수정(insertUserInfo 에 PWD_CHG_DTIME=NOW() 추가)과 함께, 이미 가입된 계정을 1회 정합한다.
--
-- 대상 한정(자기 비번을 직접 정한 셀프가입 정규직만):
--   1) PWD_CHG_DTIME IS NULL           — 아직 미세팅(멱등: 이미 세팅된 행은 자연 제외)
--   2) INSERT_NO = 'SYSTEM'            — 셀프가입 표식(insertUserInfo). 관리자/엑셀 등록은
--                                        INSERT_NO=등록자 USER_CD 라 제외됨 → 임시비번 강제변경 유지(의도).
--   3) AUTH_CD = '99999'              — 셀프가입 기본권한(insertUserInfo IFNULL 기본값)으로 "양성 식별".
--                                        ※ INSERT_NO='SYSTEM' 은 셀프가입 외에 플랫폼 부트스트랩 운영자
--                                          (PlatformOperatorBootstrapRunner→CompanyProvisionMapper.insertUser)
--                                          master 계정도 만든다. 그 계정도 PWD_CHG_DTIME/EMPLOYMENT_TYPE NULL 이라
--                                          조건 1)2)4) 만으로는 매칭되어 최고권한 계정의 최초 비번변경 강제가
--                                          무력화된다. AUTH_CD='99999' 로 좁혀 master/hr/safe/부트스트랩을 전부 배제.
--   4) EMPLOYMENT_TYPE <> 'DAILY'      — 일용직은 게이트 면제 대상이라 손대지 않음(일용직도 '99999' 라 가드 유지).
-- 멱등성: 조건 1) 로 재실행 안전.
--
-- ⚠️ 운영 적용 금지(파일만). 적용 전 아래 사전 점검 쿼리로 대상 건수를 확인할 것.
-- ============================================================================

-- [사전 점검] 백필 예상 대상 건수(회사 단위로 확인 권장).
-- SELECT CMPNY_CD, COUNT(*) AS expectedRows
-- FROM TB_USER
-- WHERE PWD_CHG_DTIME IS NULL
--   AND INSERT_NO = 'SYSTEM'
--   AND AUTH_CD = '99999'
--   AND (EMPLOYMENT_TYPE IS NULL OR EMPLOYMENT_TYPE <> 'DAILY')
-- GROUP BY CMPNY_CD;

UPDATE TB_USER
   SET PWD_CHG_DTIME = NOW()
     , UPDATE_NO     = 'SYSTEM'
     , UPDATE_DATE   = NOW()
 WHERE PWD_CHG_DTIME IS NULL
   AND INSERT_NO = 'SYSTEM'
   AND AUTH_CD = '99999'
   AND (EMPLOYMENT_TYPE IS NULL OR EMPLOYMENT_TYPE <> 'DAILY');
