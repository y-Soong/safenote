-- =============================================================================
-- qe-e2e-cleanup.sql  —  근태 E2E(Playwright, QE) 통합테스트 적재분 정리
-- =============================================================================
-- 작성: 2026-07-17 (세션 E7)  ·  성격: 테스트 데이터 물리정리
--
-- ★★★ 미실행 상태로 산출됨 — 반드시 사용자 승인 후 수동 실행할 것. ★★★
--   본 스크립트는 자동 실행되지 않는다. 아래 절차로 검토 후 커밋한다:
--     1) [사전검증] 블록의 SELECT 로 삭제 대상 건수를 눈으로 확인
--     2) START TRANSACTION ~ 삭제문 실행
--     3) [사후검증] 블록의 SELECT 가 전부 0 인지 확인
--     4) 이상 없으면  COMMIT;  (문제 있으면  ROLLBACK;)
--
-- 대상 범위(QE 적재분만):
--   · QE 재사용 QT 계정(A/C/D/G = 20260700029/031/032/034): E0 개시 시점 트랜잭션
--     데이터가 0 건이었음(baseline-ledger.md P-1). 따라서 현재 grant/use/attd/req/ot/
--     work_plan 은 전부 QE 적재분 → 삭제. 단 tb_user 계정 행 자체는 QT 유산이므로 보존.
--   · QE 신설 계정(H/I = 20260700036/037, 일용직 QTDAILY2 = D2026071700020):
--     계정 행 포함 전부 삭제.
--   · QE 태그 휴일 13건([QE-*], 전부 USE_YN='N' soft-delete) 물리삭제.
--   · 블랙리스트/소속이동예약/소모근무타입(QE9H)/환산 20260801 잔여행/입장요청/약관동의.
--
-- 미접촉(보존) — 삭제 금지:
--   · QT 유산 시드(qt-integration-test-cleanup.sql 대상): 계정 A/C/D/G/QTHR/B/F 행,
--     근무타입 00001~00005, 연차정책, [QT-0]/[QT-10-1]/[QT-10-2] 휴일, 국가공휴일(제헌절 등).
--     → QE cleanup 은 QT 시드 위에 쌓인 QE분만 해체(실행 순서: QE → QT 역순).
--   · QTUSERB/F(00003) 및 그 근무계획 124건(소속이동 발효 산출물 — QT/발효 유산).
--   · 연차정책 POLICY_SEQ 이력(11→24 누적): 이력성 데이터라 삭제 대상 아님(감사 추적 보존).
--   · 환산정책 20260714(=400, 실효값) 행: 현행 유효 설정이므로 보존. 20260801 행만 삭제
--     (QE-4-6 upsert 원복 artifact, 실효 400 동일이라 무해하나 잔재 제거).
--
-- 판단 필요(주석) 항목:
--   · [work_plan A/C/D/G] : QE 태그가 없는 QE E0 시드. 기본 삭제에 포함하되, QT 계정을
--     후속 QT/발효 검증에 재사용할 계획이면 §7 블록을 건너뛸 것(주석으로 분리).
--   · [tb_attd_close 00010/202606] : QE-E6 마감 메커니즘 증명용 왕복 artifact(CLOSE_STATUS
--     =OPEN, 무해). 정합엔 영향 없으나 잔재 제거를 원하면 §12 실행(선택).
--
-- FK/참조 순서: 자식 → 부모 (req_approval→req, attd_hist→attd_mgmt, use→grant,
--   work_plan→sch_mgmt, entry_request/daily_user/terms→tb_user 순).
--   PRAFTA 는 물리 FK 보다 논리키 위주지만, 안전을 위해 자식부터 삭제한다.
-- =============================================================================

-- 회사/DB 가드: 로컬/개발 DB 인지 반드시 확인(운영 실행 금지).
SELECT DATABASE() AS current_db;   -- 기대: 로컬 prafta 스키마

-- -----------------------------------------------------------------------------
-- [사전검증] 삭제 대상 건수 스냅샷 — 실행 전에 눈으로 확인 (기대값 주석 병기)
-- -----------------------------------------------------------------------------
SELECT 'req_approval(자식)'      AS target, COUNT(*) AS cnt FROM tb_user_attd_req_approval
        WHERE REQ_ID IN (SELECT REQ_ID FROM tb_user_attd_req
          WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037'))            -- 기대 15
UNION ALL SELECT 'attd_req',      COUNT(*) FROM tb_user_attd_req
        WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')                -- 기대 29
UNION ALL SELECT 'attd_hist(자식)', COUNT(*) FROM tb_user_attd_hist
        WHERE ATTD_ID IN (SELECT ATTD_ID FROM tb_user_attd_mgmt
          WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037'))            -- 기대 10
UNION ALL SELECT 'attd_gps',      COUNT(*) FROM tb_user_attd_gps
        WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')                -- 기대 0
UNION ALL SELECT 'attd_mgmt',     COUNT(*) FROM tb_user_attd_mgmt
        WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')                -- 기대 11
UNION ALL SELECT 'overtime_mgmt', COUNT(*) FROM tb_user_overtime_mgmt
        WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')                -- 기대 3
UNION ALL SELECT 'leave_use',     COUNT(*) FROM tb_user_leave_use
        WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')                -- 기대 19
UNION ALL SELECT 'leave_grant',   COUNT(*) FROM tb_user_leave_grant
        WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')                -- 기대 13
UNION ALL SELECT 'work_plan',     COUNT(*) FROM tb_user_work_plan
        WHERE USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')                                   -- 기대 436
UNION ALL SELECT 'transfer_resv', COUNT(*) FROM tb_user_transfer_reservation
        WHERE RESERVATION_ID IN ('TR2026071700006','TR2026071700007')                                                                            -- 기대 2
UNION ALL SELECT 'terms_mgmt',    COUNT(*) FROM tb_terms_user_agr_mgmt
        WHERE USER_CD IN ('20260700036','20260700037','D2026071700020')                                                                          -- 기대 15
UNION ALL SELECT 'terms_hist',    COUNT(*) FROM tb_terms_user_agr_hist
        WHERE USER_CD IN ('20260700036','20260700037','D2026071700020')                                                                          -- 기대 10
UNION ALL SELECT 'daily_entry',   COUNT(*) FROM tb_daily_entry_request WHERE REQ_ID='ER2026071700002'                                            -- 기대 1
UNION ALL SELECT 'daily_user',    COUNT(*) FROM tb_daily_user WHERE USER_CD='D2026071700020'                                                     -- 기대 1
UNION ALL SELECT 'tb_user(신설)', COUNT(*) FROM tb_user WHERE USER_CD IN ('20260700036','20260700037','D2026071700020')                          -- 기대 3
UNION ALL SELECT 'blacklist',     COUNT(*) FROM tb_daily_blacklist WHERE BLACKLIST_ID='B2026071700002'                                           -- 기대 1
UNION ALL SELECT 'schtype QE9H',  COUNT(*) FROM tb_sch_mgmt WHERE CMPNY_CD='001' AND SITE_CD='00010' AND SCH_CD='00006'                          -- 기대 1
UNION ALL SELECT 'conv 20260801', COUNT(*) FROM tb_leave_conversion_policy WHERE CMPNY_CD='001' AND APPLY_FROM_DATE='20260801'                   -- 기대 1
UNION ALL SELECT 'holiday QE',    COUNT(*) FROM tb_holiday WHERE CMPNY_CD='001' AND HOLIDAY_NM LIKE '[QE-%'                                       -- 기대 13
UNION ALL SELECT 'close 202606',  COUNT(*) FROM tb_attd_close      WHERE SITE_CD='00010' AND CLOSE_YM='202606'                                   -- 기대 1(선택)
UNION ALL SELECT 'close_hist',    COUNT(*) FROM tb_attd_close_hist WHERE SITE_CD='00010' AND CLOSE_YM='202606';                                  -- 기대 4(선택)


-- =============================================================================
--  삭제 트랜잭션 시작 (검토 후 아래 블록 실행)
-- =============================================================================
START TRANSACTION;

-- §1. 근태요청 결재라인(자식) → 근태요청 ------------------------------------------------
DELETE FROM tb_user_attd_req_approval
 WHERE REQ_ID IN (SELECT REQ_ID FROM tb_user_attd_req
   WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037'));

DELETE FROM tb_user_attd_req
 WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037');
-- 포함: 고아 대기요청 REQ2026071700172/173/174(H 비활성 교착 원인)·REQ2026071700171(A sched-modify)
--       + QE 승인/반려 이력 전량. (M10 마감교착 잔재도 여기서 해소)

-- §2. 근태 이력(자식) → GPS → 근태 본행 -------------------------------------------------
DELETE FROM tb_user_attd_hist
 WHERE ATTD_ID IN (SELECT ATTD_ID FROM tb_user_attd_mgmt
   WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037'));

DELETE FROM tb_user_attd_gps
 WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037');

DELETE FROM tb_user_attd_mgmt
 WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037');

-- §3. 초과근무 -----------------------------------------------------------------------
DELETE FROM tb_user_overtime_mgmt
 WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037');

-- §4. 연차 사용(자식, GRANT_ID 참조) → 연차 부여 ----------------------------------------
DELETE FROM tb_user_leave_use
 WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037');
-- 포함: 대기연차 선반영 LV2026071700089(H 8/20)·LV2026071700090(G 8/25) + 확정/취소 전량

DELETE FROM tb_user_leave_grant
 WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037');
-- 포함: E0 policy-grant(A/C/D 15·G 5·H/I 15) + QE 포상 GRANT 전량

-- §5. 소속이동 예약 ------------------------------------------------------------------
DELETE FROM tb_user_transfer_reservation
 WHERE RESERVATION_ID IN ('TR2026071700006','TR2026071700007');
-- TR006=QTUSERI(고아, I 비활성) · TR007=QTUSERG(6-2 커버케이스 잔재)

-- §6. 약관동의(신설/일용 계정) 이력 → 본행 ---------------------------------------------
DELETE FROM tb_terms_user_agr_hist
 WHERE USER_CD IN ('20260700036','20260700037','D2026071700020');
DELETE FROM tb_terms_user_agr_mgmt
 WHERE USER_CD IN ('20260700036','20260700037','D2026071700020');

-- §7. 근무계획(work_plan) ------------------------------------------------------------
--  ⚠️ 판단 필요: A/C/D/G(재사용 QT계정) 근무계획은 QE E0 시드(무태그). 기본 삭제 포함.
--     QT 계정을 후속 QT/발효 검증에 재사용하려면 아래 첫 DELETE 를 건너뛸 것.
DELETE FROM tb_user_work_plan
 WHERE USER_CD IN ('20260700029','20260700031','20260700032','20260700034');   -- QE 재사용계정 E0 시드(선택적 보존 가능)
DELETE FROM tb_user_work_plan
 WHERE USER_CD IN ('20260700036','20260700037');                                -- 신설 H/I DEFAULT_SCH 자동생성분(반드시 삭제)

-- §8. 일용직(QTDAILY2) 입장요청 → 일용사용자 본행 --------------------------------------
DELETE FROM tb_daily_entry_request WHERE REQ_ID='ER2026071700002';
DELETE FROM tb_daily_user          WHERE USER_CD='D2026071700020';
--  참고: QTDAILY2 는 슬롯 미점유(승인대기 04) → tb_daily_user_slot 삭제 대상 없음.
--        계약(tb_daily_contract_sign)도 미승인이라 미생성(대상 없음).

-- §9. 신설/일용 계정 본행(tb_user) ----------------------------------------------------
--  ⚠️ A/C/D/G/QTHR/B/F 는 QT 유산이므로 여기서 삭제하지 않는다(위 §1~§7 로 트랜잭션만 정리).
DELETE FROM tb_user WHERE USER_CD IN ('20260700036','20260700037','D2026071700020');

-- §10. 블랙리스트 --------------------------------------------------------------------
DELETE FROM tb_daily_blacklist WHERE BLACKLIST_ID='B2026071700002';

-- §11. 소모 근무타입(QE9H) + 환산 20260801 잔여행 -------------------------------------
--  QE9H(00006): §7 에서 이를 참조하던 H 근무계획을 먼저 삭제했으므로 안전.
--  (근무타입 이력 tb_sch_mgmt_hist 에 00006 파생행이 있으면 함께 삭제 — 아래 2번째 DELETE)
DELETE FROM tb_sch_mgmt_hist WHERE CMPNY_CD='001' AND SITE_CD='00010' AND SCH_CD='00006';
DELETE FROM tb_sch_mgmt      WHERE CMPNY_CD='001' AND SITE_CD='00010' AND SCH_CD='00006';

DELETE FROM tb_leave_conversion_policy WHERE CMPNY_CD='001' AND APPLY_FROM_DATE='20260801';

-- §12. QE 태그 휴일 13건 물리삭제(전부 USE_YN='N' soft-delete 상태) --------------------
--   [QT-*]·국가공휴일은 미접촉. 명칭 prefix '[QE-' 로만 한정.
DELETE FROM tb_holiday WHERE CMPNY_CD='001' AND HOLIDAY_NM LIKE '[QE-%';

-- §13. (선택) 마감 왕복 artifact — 00010/202606 --------------------------------------
--   QE-E6 마감 메커니즘 증명용 왕복(CLOSE_STATUS=OPEN, 무해). 잔재 제거를 원할 때만 실행.
DELETE FROM tb_attd_close_hist WHERE SITE_CD='00010' AND CLOSE_YM='202606';
DELETE FROM tb_attd_close      WHERE SITE_CD='00010' AND CLOSE_YM='202606';


-- -----------------------------------------------------------------------------
-- [사후검증] 아래 SELECT 가 전부 0 이어야 함 (홀드된 트랜잭션 내에서 확인)
-- -----------------------------------------------------------------------------
SELECT 'attd_req_left'   AS chk, COUNT(*) AS cnt FROM tb_user_attd_req      WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')
UNION ALL SELECT 'attd_left',    COUNT(*) FROM tb_user_attd_mgmt            WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')
UNION ALL SELECT 'ot_left',      COUNT(*) FROM tb_user_overtime_mgmt        WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')
UNION ALL SELECT 'use_left',     COUNT(*) FROM tb_user_leave_use            WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')
UNION ALL SELECT 'grant_left',   COUNT(*) FROM tb_user_leave_grant          WHERE CMPNY_CD='001' AND USER_CD IN ('20260700029','20260700031','20260700032','20260700034','20260700036','20260700037')
UNION ALL SELECT 'wp_new_left',  COUNT(*) FROM tb_user_work_plan            WHERE USER_CD IN ('20260700036','20260700037')
UNION ALL SELECT 'resv_left',    COUNT(*) FROM tb_user_transfer_reservation WHERE RESERVATION_ID IN ('TR2026071700006','TR2026071700007')
UNION ALL SELECT 'newuser_left', COUNT(*) FROM tb_user                      WHERE USER_CD IN ('20260700036','20260700037','D2026071700020')
UNION ALL SELECT 'daily_left',   COUNT(*) FROM tb_daily_user                WHERE USER_CD='D2026071700020'
UNION ALL SELECT 'blk_left',     COUNT(*) FROM tb_daily_blacklist           WHERE BLACKLIST_ID='B2026071700002'
UNION ALL SELECT 'schQE9H_left', COUNT(*) FROM tb_sch_mgmt                  WHERE CMPNY_CD='001' AND SITE_CD='00010' AND SCH_CD='00006'
UNION ALL SELECT 'convaug_left', COUNT(*) FROM tb_leave_conversion_policy   WHERE CMPNY_CD='001' AND APPLY_FROM_DATE='20260801'
UNION ALL SELECT 'holQE_left',   COUNT(*) FROM tb_holiday                   WHERE CMPNY_CD='001' AND HOLIDAY_NM LIKE '[QE-%';

-- 원장 정합 최종 게이트: 잔존(보존) 계정의 GRANT.USED_DAYS = SUM(CONFIRMED use) 불일치 0 이어야 함.
--   (QE 정리 후 A/C/D/G 는 grant/use 0 → 자명 정합. QTHR/B/F 등 타 계정 무영향.)
SELECT g.USER_CD, g.GRANT_ID, g.USED_DAYS AS ledger_used,
       COALESCE((SELECT SUM(u.LEAVE_DAYS) FROM tb_user_leave_use u
                  WHERE u.GRANT_ID=g.GRANT_ID AND u.LEAVE_STATUS='CONFIRMED' AND u.DEL_YN='N'),0) AS use_sum
  FROM tb_user_leave_grant g
 WHERE g.CMPNY_CD='001' AND g.DEL_YN='N' AND g.STATUS='ACTIVE'
HAVING ABS(ledger_used - use_sum) > 0.00001;   -- 기대: 0 행

-- =============================================================================
--  검토 완료 후 아래 중 하나를 실행 (기본은 미커밋 — 안전)
-- =============================================================================
-- COMMIT;     -- 사후검증 통과 시 주석 해제하여 확정
-- ROLLBACK;   -- 취소(원상복구)

-- =============================================================================
--  후속(별도): QE 정리 확정 후, 필요 시 QT 유산까지 정리하려면
--    qt-integration-test-cleanup.sql 을 이어서 실행(QE → QT 역순 해체).
--    ※ 소속이동 발효(QE-I-1~3) 재검증을 남겨둘 계획이면 QT 시드는 보존할 것.
-- =============================================================================
