-- ============================================================================
-- PRAFTA-SUBCON-T3-1 — 데이터 공유(요청/스냅샷/근태상세/번들) DDL
-- 작성일: 2026-07-13
-- 적용 환경: MySQL 8.0.42
-- 출처: PRAFTA-SUBCON.md §2.1(계약) + PRAFTA-SUBCON-T3.plan.md §0-3(계약확장 C1~C3)·§3-1,
--       메인 세션 MCP 실측(2026-07-13 — 원천 테이블 컬럼/타입 대조)
-- 선행: prafta-subcon-t1-1-relation-ddl.sql(tb_cmpny_relation), prafta-subcon-t2-1-site-link-ddl.sql(tb_site_link)
--
-- 계약 확장 근거(마스터 §2.4 — 메인 세션 승인 2026-07-13):
--   C1) tb_cmpny_share_req.REQ_SITE_CD  : 요청자 테넌트 사업장. 없으면 ① 요청자 화면에 자기 사업장명을
--       표시할 수 없고(TARGET_SITE_CD 는 제공측 테넌트 코드) ② 릴레이 후보의 사업장 체인 일치
--       판정(plan §5-7 조건 2)이 불가능하다.
--   C2) tb_cmpny_share_snapshot.OWNER_CMPNY_CD : 수신 소유사(= SHARE_REQ.REQ_CMPNY_CD 비정규화).
--       조회/상세의 테넌트 스코프를 조인 없이 1차 WHERE 로 강제(IDOR 방어선 단순화 + 인덱스).
--   C3) tb_cmpny_share_snapshot_bundle.OWNER_CMPNY_CD / ROW_CNT : 번들은 제공측 전용 감사 기록이라
--       소유 테넌트 컬럼이 없으면 감사 조회가 2단 조인이 된다. ROW_CNT 는 감사 재구성용.
--
-- PII 최소수집(요청서 §2 + 메인 세션 지시):
--   - 상세행에 저장하는 인적 정보는 성명 + 소속표시(회사명 문자열)뿐이다.
--   - USER_CD/USER_ID/사번/직위/부서/휴대폰(MBL_*)/DEVICE_UUID/연차사유(LEAVE_REASON) 는 저장하지 않는다.
--   - 성명은 원천(tb_user.USER_NM / tb_daily_user.USER_NM)이 varchar(50) 평문이므로 스냅샷도 평문
--     varchar(50) 로 둔다(메인 세션 MCP 실측 — 암호화 컬럼은 MBL_NO_ENC 계열뿐). 보호는 접근통제
--     (master/system 메뉴 권한 + OWNER_CMPNY_CD 테넌트 스코프)로 한다.
--   - 릴레이 복사분은 하위 회사 식별자(회사코드/회사명/사용자코드/하위 스냅샷ID)를 한 컬럼도 담지 않는다.
--
-- 보존: 스냅샷은 무기한 존속(사용자 확정 2026-07-13). 관계/사업장 링크 해지에도 삭제하지 않는다.
--       삭제 경로(서비스 코드) 없음.
--
-- 적용 전 부재 확인(운영 적용 직전 권장):
--   SHOW TABLES LIKE 'tb_cmpny_share%';   -- 0건이어야 함
--
-- 멱등성: CREATE TABLE 재실행 시 에러 → 이미 반영된 환경에서는 건너뛸 것.
-- 운영 적용: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

-- ── (1) 데이터 공유 요청. 전이: REQUESTED→APPROVED|REJECTED|CANCELLED. 재요청 = 새 행(목록이 곧 이력).
CREATE TABLE `tb_cmpny_share_req` (
    `SHARE_REQ_ID`     bigint       NOT NULL AUTO_INCREMENT COMMENT '공유요청ID(PK)',
    `RELATION_ID`      bigint       NOT NULL COMMENT '회사 관계ID(tb_cmpny_relation — ACCEPTED 전제)',
    `REQ_CMPNY_CD`     varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청측(수신 희망) 회사코드',
    `REQ_SITE_CD`      varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청측 사업장코드(요청자 테넌트 — 계약확장 C1)',
    `PRV_CMPNY_CD`     varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '제공측 회사코드',
    `TARGET_SITE_CD`   varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '제공측 대응 사업장코드(서버가 tb_site_link/tb_site 체인으로 해석 — 클라 입력 불신)',
    `DATA_TYPE`        varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '공유 데이터유형[SYS077] ATTD:근태 (RISK/NEARMISS 는 T7)',
    `PERIOD_STR`       char(8)      COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대상 기간 시작(YYYYMMDD)',
    `PERIOD_END`       char(8)      COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대상 기간 종료(YYYYMMDD)',
    `CLOSED_ONLY_YN`   char(1)      COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '마감 근태만[Y:마감분만(기본)/N:미마감 포함 허용] — ATTD 전용',
    `PURPOSE`          varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '제공 목적(개보법 근거 문서화 — 필수)',
    `STATUS`           varchar(20)  COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REQUESTED' COMMENT '요청 상태[SYS078] REQUESTED:요청중, APPROVED:승인, REJECTED:거부됨, CANCELLED:취소됨',
    `REQ_USER_CD`      varchar(50)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청자 사용자코드(요청측 소속)',
    `PROCESS_CMPNY_CD` varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리자 소속 회사코드(토큰 gv_cmpnyCd — 관계해지 자동취소 시 NULL)',
    `PROCESS_USER_CD`  varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리자 사용자코드',
    `PROCESS_DTIME`    datetime     DEFAULT NULL COMMENT '처리일시',
    `PROCESS_COMMENT`  varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리 코멘트(거부 사유 등)',
    `INSERT_NO`        varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`        varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`      datetime     DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`SHARE_REQ_ID`),
    KEY `IX_SHARE_REQ_REQ` (`REQ_CMPNY_CD`, `STATUS`, `SHARE_REQ_ID`),
    KEY `IX_SHARE_REQ_PRV` (`PRV_CMPNY_CD`, `STATUS`, `SHARE_REQ_ID`),
    KEY `IX_SHARE_REQ_REL` (`RELATION_ID`, `STATUS`),
    KEY `IX_SHARE_REQ_VER` (`REQ_CMPNY_CD`, `PRV_CMPNY_CD`, `TARGET_SITE_CD`, `DATA_TYPE`, `PERIOD_STR`, `PERIOD_END`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회사 간 데이터 공유 요청';
-- 활성 UNIQUE 백스톱 없음: 동일 조건 재요청 허용(마스터 §4 소결정 ①)이 스펙이다.
--   REQUESTED 중복 연타만 서비스 가드(plan §5-3 #6)로 차단한다.

-- ── (2) 스냅샷 헤더(수신사 소유 복제본 — 생성 후 불변).
CREATE TABLE `tb_cmpny_share_snapshot` (
    `SNAPSHOT_ID`          bigint      NOT NULL AUTO_INCREMENT COMMENT '스냅샷ID(PK)',
    `SHARE_REQ_ID`         bigint      NOT NULL COMMENT '공유요청ID',
    `OWNER_CMPNY_CD`       varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스냅샷 소유(수신) 회사코드 = SHARE_REQ.REQ_CMPNY_CD 비정규화(테넌트 스코프 강제 — 계약확장 C2)',
    `VERSION`              int         NOT NULL DEFAULT 1 COMMENT '버전(동일 조건 재요청 승인 시 증가 — 마스터 §4 소결정 ①)',
    `UNCLOSED_INCLUDED_YN` char(1)     COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '미마감 근태 포함 여부[Y:실제 포함(영구 표식·상위 CLOSED_ONLY 요청에 릴레이 불가)/N:전량 마감분]',
    `CONSENT_EXCLUDED_CNT` int         NOT NULL DEFAULT 0 COMMENT '제3자 제공 미동의로 제외된 인원 수(T4 동의 연동 전에는 0)',
    `ROW_CNT`              int         NOT NULL DEFAULT 0 COMMENT '상세행 총 건수(자체 생성분 + 릴레이 복사분)',
    `RELAY_INCLUDED_YN`    char(1)     COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '릴레이 묶음 포함 여부[Y/N] — 포함 사실만, 하위 회사 식별자는 미노출',
    `CREATE_DTIME`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '스냅샷 생성일시(승인 시점 고정)',
    `INSERT_NO`            varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`          datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`            varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자(스냅샷 불변 — ROW_CNT 확정 1회 외 미사용)',
    `UPDATE_DATE`          datetime    DEFAULT NULL COMMENT '수정일시(스냅샷 불변 — ROW_CNT 확정 1회 외 미사용)',
    PRIMARY KEY (`SNAPSHOT_ID`),
    UNIQUE KEY `UX_SNAPSHOT_REQ_VER` (`SHARE_REQ_ID`, `VERSION`),
    KEY `IX_SNAPSHOT_OWNER` (`OWNER_CMPNY_CD`, `SNAPSHOT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='데이터 공유 스냅샷 헤더(수신사 소유)';

-- ── (3) 근태(ATTD) 스냅샷 상세행 — 승인 시점 고정 복사. T7 유형 확장은 형제 테이블(_RISK/_NEARMISS)로.
CREATE TABLE `tb_cmpny_share_snapshot_attd` (
    `DETAIL_ID`      bigint       NOT NULL AUTO_INCREMENT COMMENT '상세행ID(PK)',
    `SNAPSHOT_ID`    bigint       NOT NULL COMMENT '스냅샷ID',
    `WORKER_SEQ`     int          NOT NULL COMMENT '스냅샷 내 근로자 일련번호(동일 인물=동일 번호. 원본 USER_CD 미반출 — 그룹핑 전용)',
    `WORKER_TYPE`    varchar(10)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근로자 구분[REGULAR:정규(tb_user)/DAILY:일용직(tb_daily_user)]',
    `WORKER_NM`      varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근로자 성명(원천이 평문 varchar(50) — 동일 형태 보관. 보호는 접근통제)',
    `AFFIL_CMPNY_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '소속표시(회사명 문자열 스냅샷). 릴레이 복사분은 제공사로 relabel — 하위 회사 식별 불가',
    `ROW_TYPE`       varchar(10)  COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '행 유형[ATTD:근태행/OT_ONLY:근태 없는 초과근무/LEAVE_ONLY:근태 없는 연차]',
    `WORK_YMD`       char(8)      COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근무일자(YYYYMMDD)',
    `WORK_SEQ`       int          NOT NULL DEFAULT 0 COMMENT '근무차수(1/2. OT_ONLY·LEAVE_ONLY 는 0)',
    `SCH_NM`         varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무타입명(tb_sch_mgmt.SCH_NO 스냅샷 — 코드 아님)',
    `SCH_TYPE`       varchar(2)   COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무타입 구분[SYS019] 스냅샷',
    `PLAN_STR_TIME`  char(4)      COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '스케줄 시작시각(HHMM, 해당 차수 — effective-dating 해석값)',
    `PLAN_END_TIME`  char(4)      COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '스케줄 종료시각(HHMM, 해당 차수)',
    `PLAN_BRK_MIN`   int          DEFAULT NULL COMMENT '스케줄 휴게(분, 해당 차수)',
    `CHECK_IN_DATE`  char(8)      COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '출근일자(YYYYMMDD. OT_ONLY 는 초과근무 실제 시작일)',
    `CHECK_IN_TIME`  char(4)      COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '출근시각(HHMM)',
    `CHECK_OUT_DATE` char(8)      COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '퇴근일자(YYYYMMDD)',
    `CHECK_OUT_TIME` char(4)      COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '퇴근시각(HHMM)',
    `ATTD_STATUS_CD` varchar(20)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근태 판정[NORMAL:정상/LATE:지각/EARLY_LEAVE:조퇴/ABSENT:미출근] — attd08 판정식 결과 고정',
    `OT_MINUTES`     int          NOT NULL DEFAULT 0 COMMENT '초과근무 인정시간(분 — 해당 근로자·근무일의 COMPLETED 합계, 휴게 제외)',
    `LEAVE_NM`       varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 구분명(tb_leave_type_mgmt.LEAVE_NM varchar(200) 정합 — 코드/사유 아님. 같은 날 다건이면 콤마 결합)',
    `LEAVE_DAYS`     decimal(8,5) DEFAULT NULL COMMENT '연차 사용일수',
    `LEAVE_MINUTES`  int          DEFAULT NULL COMMENT '연차 사용분(시간단위 휴가)',
    `LEAVE_END_YMD`  char(8)      COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 종료일(LEAVE_ONLY 다일 연차 — 시작일은 WORK_YMD)',
    `INSERT_NO`      varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`      varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자(불변 — 미사용)',
    `UPDATE_DATE`    datetime     DEFAULT NULL COMMENT '수정일시(불변 — 미사용)',
    PRIMARY KEY (`DETAIL_ID`),
    KEY `IX_SNAP_ATTD_LIST` (`SNAPSHOT_ID`, `WORK_YMD`, `WORKER_SEQ`, `WORK_SEQ`),
    KEY `IX_SNAP_ATTD_WORKER` (`SNAPSHOT_ID`, `WORKER_SEQ`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태 공유 스냅샷 상세행(시점 고정 복사)';

-- ── (4) 릴레이 묶음 감사(제공측 테넌트 전용 — 수신측 조회 API 는 절대 조인하지 않는다).
CREATE TABLE `tb_cmpny_share_snapshot_bundle` (
    `BUNDLE_ID`                bigint      NOT NULL AUTO_INCREMENT COMMENT '번들ID(PK)',
    `SNAPSHOT_ID`              bigint      NOT NULL COMMENT '제공측이 생성해 상위로 보낸 스냅샷ID',
    `INCLUDED_RCV_SNAPSHOT_ID` bigint      NOT NULL COMMENT '묶어 넣은, 제공측이 하위로부터 수신 보유하던 스냅샷ID',
    `OWNER_CMPNY_CD`           varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '기록 소유 테넌트 = 제공측 회사코드(감사 스코프 — 계약확장 C3)',
    `ROW_CNT`                  int         NOT NULL DEFAULT 0 COMMENT '해당 하위 스냅샷에서 복사한 상세행 수',
    `INSERT_NO`                varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`              datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    PRIMARY KEY (`BUNDLE_ID`),
    UNIQUE KEY `UX_BUNDLE_PAIR` (`SNAPSHOT_ID`, `INCLUDED_RCV_SNAPSHOT_ID`),
    KEY `IX_BUNDLE_OWNER` (`OWNER_CMPNY_CD`, `SNAPSHOT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='스냅샷 릴레이 묶음 감사(제공측 전용)';

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS `tb_cmpny_share_snapshot_bundle`;
-- DROP TABLE IF EXISTS `tb_cmpny_share_snapshot_attd`;
-- DROP TABLE IF EXISTS `tb_cmpny_share_snapshot`;
-- DROP TABLE IF EXISTS `tb_cmpny_share_req`;
-- ============================================================================
