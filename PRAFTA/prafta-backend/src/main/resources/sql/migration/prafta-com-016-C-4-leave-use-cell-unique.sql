-- ============================================================================
-- PRAFTA-COM-016-C-4 — 자동 법정휴가(소멸 임박 통합순) 셀 단위 이중차감 방지 유니크
-- 작성일: 2026-06-19
-- 적용 환경: MySQL 8.0.42
-- 참조: 근무계획관리(Attd_05) "법정 휴가" 자동 차감 — 보안 리뷰 prafta-com-016-c-4-001 (TOCTOU 이중차감)
--
-- 배경
--  기존 UK_LEAVE_USE_DIRECT(prafta-021)의 멱등 키는 (USER_CD|START_DATE|LEAVE_CD)로 LEAVE_CD 를
--  포함한다. 종류를 고정 지정하던 직접 차감(연차 또는 월차 단건)에서는 셀당 종류가 1개라 완전 방어됐다.
--  그러나 C-4 자동 차감은 후보(연차/월차) 중 소멸 임박 종류를 매번 고르므로, 동일 셀에 대한 동시 저장
--  2건이 서로 다른 종류를 선택하면 키가 달라 DB 가 막지 못한다(REPEATABLE READ 스냅샷상 사전 count 도
--  상대 커밋분을 못 봄) → 한 날 종일 연차 2건 = 1일 초과 차감.
--
-- 조치
--  결재 없는 종일 직접 차감(REQ_ID IS NULL AND LEAVE_STATUS='CONFIRMED' AND USE_UNIT_TYPE='00')에 대해
--  종류 무관(LEAVE_CD 제외) 셀 단위 키 (USER_CD|START_DATE) 유니크를 추가한다. 두 번째 INSERT 는
--  종류가 달라도 DB 가 거부하고, 서비스의 DuplicateKeyException 핸들러가 SKIPPED_DUP 로 흡수한다.
--  결재 경유/취소(CANCELLED)/반차·시간차 건은 키가 NULL 이라 제약 대상 아님(MySQL 유니크 NULL 다중 허용).
--  기존 UK_LEAVE_USE_DIRECT 는 그대로 둔다(상호 독립, 회귀 없음).
--
-- 멱등성: 컬럼/인덱스 중복 생성 시 에러. 이미 반영된 환경에서는 건너뛸 것.
--
-- ⚠️ 운영 적용 전 [사전 점검]으로 충돌 데이터(같은 셀에 종류 다른 종일 직접연차 2건)가 0 인지 확인할 것.
--    (2026-06-19 기준 운영 0건 확인 완료. 충돌이 있으면 ALTER 가 실패하므로 선정리 필요.)
-- ============================================================================

-- [사전 점검] 충돌 데이터 — 결과가 0행이어야 적용 가능.
-- SELECT CMPNY_CD, USER_CD, START_DATE, COUNT(*) AS cnt, GROUP_CONCAT(LEAVE_CD) AS cds
--   FROM tb_user_leave_use
--  WHERE REQ_ID IS NULL AND LEAVE_STATUS = 'CONFIRMED' AND USE_UNIT_TYPE = '00'
--  GROUP BY CMPNY_CD, USER_CD, START_DATE
-- HAVING COUNT(*) > 1;

ALTER TABLE `tb_user_leave_use`
    ADD COLUMN `DIRECT_USE_CELL_KEY` varchar(60)
        GENERATED ALWAYS AS (
            CASE WHEN `REQ_ID` IS NULL
                  AND `LEAVE_STATUS` = 'CONFIRMED'
                  AND `USE_UNIT_TYPE` = '00'
                 THEN CONCAT(`USER_CD`, '|', `START_DATE`)
                 ELSE NULL END
        ) STORED COMMENT '직접 차감(결재없음·종일) 셀 단위 멱등 키 — 종류 무관, 결재경유/취소/반차·시간차는 NULL',
    ADD UNIQUE INDEX `UK_LEAVE_USE_DIRECT_CELL` (`CMPNY_CD`, `DIRECT_USE_CELL_KEY`);
