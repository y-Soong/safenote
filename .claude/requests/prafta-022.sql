/* ============================================================
   prafta-022 연차 부여엔진 수정 — 필요 DDL
   (로컬/개발 DB 전용. 운영 DB 직접 적용 금지)

   목적: 입사일 변경 처리방식(handlingType)을 Attd_09 "정책 기준 부여"
        버튼에서 적용할 때, "이미 적용 완료한 이력"을 추적해
        RESET_ALL 무한 재발급을 차단하고 버튼 재클릭을 멱등 처리한다.
        (prafta-022-plan.md §5 D1)

   대상: TB_USER_HIRE_DATE_HISTORY (입사일 변경 이력, 노무 감사용)
   영향: 기존 행은 APPLIED_YN DEFAULT 'N'으로 채워짐(과거 이력은 미적용 간주).
        nullable/defaulted 컬럼 추가뿐이라 기존 데이터/쿼리에 파괴적 변경 없음.
   ============================================================ */

ALTER TABLE `tb_user_hire_date_history`
  ADD COLUMN `APPLIED_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      NOT NULL DEFAULT 'N'
      COMMENT '정책 기준 부여 적용 완료 여부 (Attd_09 부여 버튼에서 적용 시 Y)'
      AFTER `AFFECTED_GRANT_SNAPSHOT`,
  ADD COLUMN `APPLIED_DATE` datetime DEFAULT NULL
      COMMENT '적용 일시'
      AFTER `APPLIED_YN`,
  ADD COLUMN `APPLIED_BY` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
      DEFAULT NULL
      COMMENT '적용 수행자 (USER_CD)'
      AFTER `APPLIED_DATE`;

/* 적용 흐름:
     부여 버튼 → 직원별 최신 미적용(APPLIED_YN='N') 이력 1건의 HANDLING_TYPE으로 부여
     → 성공 시 해당 이력 APPLIED_YN='Y', APPLIED_DATE/APPLIED_BY 기록.
   재실행:
     새 입사일 변경 → APPLIED_YN='N' 신규 행 생성 → 다시 적용 가능.
     같은 이력 재클릭 → APPLIED_YN='Y'라 멱등 skip.
   조회 인덱스:
     기존 IX_TB_HIRE_HIST_USER (CMPNY_CD, USER_CD, INSERT_DATE)로 "최신 1건" 정렬 커버. */
