-- ============================================================================
-- prafta-029: RESET 회차키(_R{HIST_ID}) ↔ 표준키 멱등 누수로 생긴
--             '같은 (기간·종류) ACTIVE 중복 부여' 진단/정리 (로컬/개발 DB)
--
-- 배경: RESET_ALL 직후 APPLY_NEW 클릭이 표준키로 같은 기간(특히 월차)을 재부여하던 누수.
--   엔진 수정(alreadyGranted 전환가드, countActiveBySuffixVariant)으로 재발은 차단됨.
--   본 스크립트는 (a) 이미 생긴 ACTIVE 중복을 진단하고, (b) 필요 시 보수적으로 정리한다.
--
-- 주의(§8.5.8 기부여 보호): 정리는 '취소(STATUS=CANCELED) 소프트 처리'만 한다(물리 삭제 금지).
--   tb_user_leave_use 사용 이력은 절대 건드리지 않는다.
-- ============================================================================

-- (a) 진단: 같은 (USER_CD, 기간·종류 base_key)에 ACTIVE STATUTORY 부여가 2건 이상인 행.
--     base_key = IDEMPOTENCY_KEY 에서 후행 접미사(_HIRE / _R{HIST_ID})를 제거한 표준 키.
SELECT USER_CD,
       REGEXP_REPLACE(IDEMPOTENCY_KEY, '(_HIRE)?(_R[0-9A-Za-z]+)?$', '') AS base_key,
       COUNT(*) AS active_dups,
       GROUP_CONCAT(GRANT_ID ORDER BY INSERT_DATE) AS grant_ids
  FROM TB_USER_LEAVE_GRANT
 WHERE CMPNY_CD = '001'              -- 대상 회사
   AND STATUS = 'ACTIVE' AND DEL_YN = 'N'
   AND GRANT_TYPE LIKE 'STATUTORY\_%'
 GROUP BY USER_CD, base_key
HAVING COUNT(*) > 1
 ORDER BY USER_CD, base_key;
-- → 2026-05-25 기준 실행 결과 0건(중복 없음). 운영/재테스트 시 재확인용.

-- (b) 정리 템플릿(중복 발견 시에만, 사용자 검토 후 실행):
--     base_key 그룹마다 '가장 최근 INSERT_DATE 1건만 ACTIVE 유지'하고 나머지는 CANCELED 소프트 처리.
--     ※ 어떤 행을 살릴지는 정책 판단이 필요하므로 기본은 주석 처리. 실행 전 (a)로 대상 확인.
--
-- UPDATE TB_USER_LEAVE_GRANT T
--   JOIN (
--     SELECT GRANT_ID
--       FROM (
--         SELECT GRANT_ID,
--                ROW_NUMBER() OVER (
--                  PARTITION BY USER_CD,
--                               REGEXP_REPLACE(IDEMPOTENCY_KEY, '(_HIRE)?(_R[0-9A-Za-z]+)?$', '')
--                  ORDER BY INSERT_DATE DESC, GRANT_ID DESC
--                ) AS rn
--           FROM TB_USER_LEAVE_GRANT
--          WHERE CMPNY_CD = '001'
--            AND STATUS = 'ACTIVE' AND DEL_YN = 'N'
--            AND GRANT_TYPE LIKE 'STATUTORY\_%'
--       ) X
--      WHERE X.rn > 1               -- 최신 1건 제외한 중복분
--   ) D ON D.GRANT_ID = T.GRANT_ID
--    SET T.STATUS = 'CANCELED', T.UPDATE_DATE = NOW()
--  WHERE T.CMPNY_CD = '001';
