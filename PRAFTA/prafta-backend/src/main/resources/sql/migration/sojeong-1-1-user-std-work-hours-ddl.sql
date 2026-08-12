-- ============================================================================
-- SOJEONG-1-1 — 근로자별 소정근로시간 이력 테이블 신설 (TB_USER_STD_WORK_HOURS)
-- 작성일: 2026-08-12
-- 적용 환경: MySQL 8.0.42 이상 (★개발·운영 동시 적용 — feedback_db_migration_apply_both_envs)
-- 참조: .claude/requests/web_requests/작업지시서_근로자별-소정근로시간-관리-도입.md §0단계
--       .claude/requests/web_requests/작업지시서_근로자별-소정근로시간-관리-도입.plan.md §1.1
--
-- 변경 요약
--  1) TB_USER_STD_WORK_HOURS 신설 — 근로자별 "계약량"(주 소정근로분)의 effective-dating 이력.
--     연차 비례부여 분모 / 정산(3단계 버킷) 분모 / 단시간 판정의 유일한 원천.
--     tb_user 단일 컬럼 금지(지시서) — 계약 변경 시점별 이력이 소급 재계산의 전제.
--
-- 규약
--  - 멀티테넌시 복합키 관례: CMPNY_CD 선두 PK (project_prafta_company_provisioning_gaps).
--  - 이력 원칙: 값 변경 = 직전 열린 행(APPLY_END_DATE IS NULL) 을 신규 적용일 전일로 마감 +
--    신규 행 INSERT. 물리 UPDATE 로 과거 값 덮어쓰기 금지(소급 재계산 전제 훼손).
--    오입력 정정만 동일 APPLY_STR_DATE 행 UPDATE 허용(StdWorkHoursService.correct).
--  - 기간 겹침 금지 / 단축 사유(CHILDCARE·PREGNANCY·FAMILY_CARE) 종료일 필수 /
--    주 15시간(900분) 미만 경고 / 육아기 주 15~35시간(900~2100분) 밖 경고 /
--    일용직(EMPLOYMENT_TYPE='DAILY') 등록 차단 — 전부 서비스 레이어 검증
--    (com.prafta.common.cmm.stdwork.service.impl.StdWorkHoursServiceImpl).
--  - 요일 패턴(주 며칠 x 몇 시간) 컬럼은 두지 않는다 — 첫 단시간 실계약 확인 후 확장(지시서).
--  - REASON_CD 는 SYS083 코드그룹 참조(sojeong-1-4). 2단계 차감·부여 분기는 코드값이 아니라
--    SYS083 의 VAL_D_INFO_1/2 데이터를 읽어 분기한다(하드코딩 금지 — 행정해석 변동 흡수).
--
-- 멱등성: CREATE TABLE 중복 실행 시 에러(Table already exists). 이미 반영된 환경에서는 건너뛸 것.
-- 적용 순서: sojeong-1-1 → 1-2 → 1-3 → 1-4 → 1-5 순으로 적용.
--            ★BE 재기동 전 선적용 필수 — 미적용 상태로 신규 코드가 뜨면 StdWorkHoursMapper
--            조회가 전부 1146(Table doesn't exist)로 실패한다.
-- 운영 적용: 사용자 수동(Workbench). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

CREATE TABLE `tb_user_std_work_hours` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `APPLY_STR_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '적용 시작일 (YYYYMMDD, 당일 포함)',
  `APPLY_END_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '적용 종료일 (YYYYMMDD, 당일 포함. NULL=무기한 — 후속 행 등록 시 전일로 자동 마감)',
  `WEEK_STD_MINUTES` int NOT NULL COMMENT '주 소정근로 분 (2400=주40h, 1200=주20h). 연차 비례부여 분자·정산 분모·단시간 판정의 계약값',
  `REASON_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사유코드[SYS083] NORMAL:통상 PART_TIME:단시간계약 CHILDCARE:육아기단축 PREGNANCY:임신기단축 FAMILY_CARE:가족돌봄단축',
  `REASON_DETAIL` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사유 상세 (자유 텍스트)',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`USER_CD`,`APPLY_STR_DATE`),
  KEY `IX_TB_USER_STD_WORK_HOURS_ACTIVE` (`CMPNY_CD`,`USER_CD`,`APPLY_END_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근로자별 소정근로시간 이력 (계약량 축 — 연차 비례부여·정산 분모·단시간 판정 원천)';

-- ── 백필 없음 ──
-- 기존 계정은 "미입력 허용 + 통상 기준값 간주 폴백"(지시서 확정). 일괄 시드 마이그레이션을
-- 두지 않는다(현재 전원 풀타임 실태와 정합). 단시간/단축 인력 발생 시 그 계정에만 명시 입력.
-- 폴백 체인: 이력 행 → TB_CMPNY_STD_WORK_POLICY(sojeong-1-2) → 코드 상수 2400분.
