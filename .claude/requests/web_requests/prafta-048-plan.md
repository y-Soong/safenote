# PRAFTA-048 사고관리(Accident Management) 도메인 — 작업지시서 (planner 분해 결과)

> 작성자: planner (Claude) · 작성일: 2026-06-05
> 영역: **웹/백엔드** (`PRAFTA/prafta-backend`, `PRAFTA/prafta-web-frontend`). 앱 미대상.
> 단일 출처 설계: `.claude/context/accident-management-design.md` (이하 "설계문서").
> 요청서: `.claude/requests/web_requests/prafta-048.md` · 목업: `ref/prafta-048/accident_management_v2.html` · `사고관리_temp.png`.
> 연계(구현완료): 아차사고 `.claude/context/near-miss-incident-design.md` (`tb_near_miss`).
> 정책: `.claude/context/policies/common/04-user-tracks.md` (일용직 트랙, 2026-06-05 정정).
> 모듈 약어: **`accident`** (대메뉴 약어 확정, 설계문서 §8).
>
> ⚠️ 이 문서는 분해 결과(작업지시서)다. Notion 미접근 환경이므로 파일로만 산출한다.
> 작업 ID는 `PRAFTA-048-{순번}`로 채번한다(요청서 prafta-048 기준).

---

## 0. 스키마 검증 결과 (추측 아님 — 실제 조회 기준)

작업 분해 전 연계 테이블의 실제 컬럼/타입을 재확인했다. 스냅샷(`.claude/context/schema-full.sql`)과 운영 DB가 일부 불일치하므로 출처를 표기한다.

| 테이블 | 출처 | PK | 사고관리가 쓰는 핵심 컬럼 |
|---|---|---|---|
| `tb_user` | 스냅샷 L910 | `CMPNY_CD,USER_CD` | `USER_NM`, `MBL_NO_ENC/HMAC/LAST4`(PII), `SITE_CD`, `NODE_CD`, `HIRE_DATE`, `EMPLOYMENT_TYPE` |
| `tb_daily_user` | 스냅샷 L201 | `CMPNY_CD,USER_CD` | `USER_NM`, `MBL_NO_*`, `SITE_CD`, `REG_TYPE`(SYS030) · **스케줄 없음** |
| `tb_user_attd_mgmt` | 스냅샷 L1001 | `ATTD_ID` | `USER_CD,WORK_YMD,WORK_SEQ`, `CHECK_IN_DATE/TIME`, `CHECK_OUT_DATE/TIME`, `DEL_YN` (HHMM=varchar4) |
| `tb_user_work_plan` | 스냅샷 L1245 | `CMPNY_CD,SITE_CD,USER_CD,WORK_YMD` | `WORK_PLAN_CD`(SCH_CD 또는 LEAVE_CD) |
| `tb_sch_mgmt` | 스냅샷 L528 | `CMPNY_CD,SITE_CD,SCH_CD,SCH_NO` | 스케줄 정의(시각). work_plan의 WORK_PLAN_CD→스케줄 결합 |
| `tb_chkpt_type_mgmt` | 스냅샷 L132 | `CMPNY_CD,SITE_CD,CHKLST_TYPE,CHKPT_CD` | `CHKPT_NM`, `MGMT_USER_CD`, `USE_YN`. **점검대상 카탈로그** |
| `tb_chkpt_inspect_item` | 스냅샷 L115 | `CMPNY_CD,CHKLST_TYPE,INSPECT_ITEM_CD` | `INSPECT_ITEM_SUBJ`. **항목은 SITE/CHKPT 무관, CHKLST_TYPE 단위** |
| `tb_chkpt_inspect_answer` | 스냅샷 L97 | `CMPNY_CD,SITE_CD,CHKPT_CD,INSPECT_ITEM_CD,WORK_DATE` | `INSPECT_ANSWER_TYPE`(SYS009 Y/N), `ANSWER_DESC`. **점검 결과** |
| `tb_risk_assessment` | 스냅샷 L460 | `CMPNY_CD,SITE_CD,PROCESS_CD,ASSESSMENT_CD` | `RISK_TYPE_CD`, `HAZARD_CD`, `ASSESSMENT_STATUS`(SYS011), `INIT_RISK_LV`, `REVAL_RISK_LV`, `INIT_ASSESS_DATE` |
| `tb_risk_type` | 스냅샷 L511 | `CMPNY_CD,RISK_TYPE_CD` | `RISK_TYPE_NM`, `PROCESS_CD`. 위험요인구분 카탈로그 |
| `tb_risk_site_hazard` | 스냅샷 L494 | `CMPNY_CD,RISK_TYPE_CD,HAZARD_CD` | `HAZARD_NM`. 유해요인 카탈로그 |
| `tb_tbm_session` | **백엔드 매퍼 검증**(Tbm04Mapper.xml L86) | `SESSION_CD`(+CMPNY_CD) | `SITE_CD`, `TITLE`, `STATUS_CD`(SYS046), `MANAGER_USER_CD`, `OPENED_AT/STARTED_AT/ENDED_AT`(datetime) · **스냅샷에 없음(stale)** |
| `tb_tbm_attendance` | **백엔드 매퍼 검증**(Tbm04Mapper.xml L65) | (SESSION_CD,USER_CD,USER_TYPE_CD 추정) | `USER_TYPE_CD`(REGULAR/DAILY), `COMPLETION_STATUS_CD`(COMPLETED/NOT_COMPLETED), `DEL_YN` · **스냅샷에 없음(stale)** |
| `tb_near_miss` | **마이그 검증**(prafta-near-miss-deploy.sql) | `CMPNY_CD,SITE_CD,NEAR_MISS_ID` | `INCIDENT_TYPE_CD`(SYS061), `POTENTIAL_SEVERITY_CD`(SYS062), `OCCUR_DTIME`, `DESCRIPTION`, `REPORT_STATUS_CD`(SYS063), `USE_YN` |
| `tb_syst_val_m/d` | 스냅샷 L794/812 | (val_m)`SYST_VAL_CD` / (val_d)`SYST_VAL_CD,SYST_VAL_D_CD` | **CMPNY_CD 없음(전사 공통)** |
| `tb_syst_menu_m/d` | 스냅샷 L778/761 | (m)`MENU_M_ID` / (d)`MENU_D_ID,MENU_M_ID` | **CMPNY_CD 없음(전사 공통)**. `MENU_SRC`(SYS007), `MENU_VIEW`, `MENU_IDX` |
| `tb_syst_auth_menu` | 스냅샷 L742 | `CMPNY_CD,AUTH_CD,MENU_D_ID` | **CMPNY_CD='001' 단일**. `USE_YN`, `BTN_SRCH/NEW/DELT/SAVE/EXCL` |
| `tb_file_info` | 스냅샷 L301 | `CMPNY_CD,FILE_MGMT_CD` | `FILE_TYPE`(SYS010), 첨부 1관리코드 |

**`tb_accident` 부재 확인**: 스냅샷·DB 모두 사고 관련 테이블 전무 → 신규 도메인(greenfield). 확정.

### 0.1 설계문서 대비 교정 사항 (스키마 정합)
1. **순회점검 점검항목은 SITE/CHKPT가 아니라 `CHKLST_TYPE` 단위로 정의**된다(`tb_chkpt_inspect_item` PK = `CMPNY_CD,CHKLST_TYPE,INSPECT_ITEM_CD`). 결과(`tb_chkpt_inspect_answer`)는 `SITE_CD+CHKPT_CD+INSPECT_ITEM_CD+WORK_DATE` 단위. → "점검대상별 1주일 집계" = answer를 `CHKPT_CD` + `WORK_DATE BETWEEN 사고일-7 AND 사고일`로 묶어 `INSPECT_ANSWER_TYPE='Y'/'N'` 카운트.
2. **TBM 코드그룹**: 세션 상태 = `SYS046`(설계문서엔 미명시). TBM 출결 완료여부는 코드값이 아닌 문자열 `COMPLETED`/`NOT_COMPLETED`(SYS코드 매핑은 별도 SYS, 매퍼는 리터럴 사용 중).
3. **`tb_syst_menu_m/d`에 CMPNY_CD 없음** → 메뉴 seed는 회사 비종속. 권한(`tb_syst_auth_menu`)만 `CMPNY_CD='001'`. (설계문서 §4.3의 "회사 비종속 확인 필요"는 이 사실로 해소 — 잔여확인에서 멀티테넌트 대응만 남김.)
4. **HHMM 시각은 varchar4** (`CHECK_IN_TIME` 등). `tb_accident.OCCUR_TIME`도 varchar4로 통일(설계문서와 동일).

---

## 1. 작업 분해 요약 (착수 순서)

| 작업 ID | 유형 | 제목 | 선행 | 비고 |
|---|---|---|---|---|
| **PRAFTA-048-01** | backend(DDL/seed) | 신규 테이블 4종 + 코드그룹 + 메뉴/권한 마이그레이션 | 없음 | 모든 후속의 blocking 선행 |
| **PRAFTA-048-02** | backend(seed/콘텐츠) | `tb_accident_legal_step_master` 법정절차 seed (목업 buildActions 이관) | 048-01 | 노무사 검토 게이트(잔여확인 2) |
| **PRAFTA-048-03** | backend | accident01 모듈 — 사고 CRUD + 목록/상세 | 048-01 | 컨트롤러/서비스/매퍼/DTO 골격 |
| **PRAFTA-048-04** | backend | 5개 연계 도메인 사고일 기준 조회 API | 048-01, 048-03 | 근태/순회/위험/TBM/아차 |
| **PRAFTA-048-05** | backend | 연계 확정 스냅샷 저장 + ②법정절차 체크/비고 저장 | 048-02, 048-04 | `tb_accident_link`/`legal_step` 쓰기 |
| **PRAFTA-048-06** | frontend-screen | `Accident_01.vue` 3탭 + 사고등록 팝업 + 수평선 확정화면 | 048-03(API 계약) | UI-001 |
| **PRAFTA-048-07** | frontend-component | `ChkptSearchPop.vue` (점검대상 검색, 다건) | — | UI-002, 048-06과 병렬 가능 |

**병렬 가능**: 048-03과 048-07(공통 팝업)은 독립. 048-02는 048-03/04와 독립(seed 콘텐츠 작업).
**blocking**: 048-01은 전 작업의 선행. 048-05는 048-02(절차 seed)+048-04(조회) 둘 다 필요.

**우선순위 근거**: 사고관리는 법적 책임 영역(산안법 §54/§57 보고의무)이자 PII(재해자 이름/휴대폰) 처리 → planner 우선순위 규칙상 +1단계 격상. 단 신규 도메인이므로 DDL/API 선행이 절대 blocking.

---

## 2. PRAFTA-048-01 — 신규 테이블 DDL + 코드그룹 + 메뉴/권한 (backend)

- **유형**: backend (DDL/seed)
- **영역**: web / **모듈**: accident
- **작업 유형**: 신규
- **요구사항 요약**: 사고관리 4종 테이블, 코드그룹 SYS065~067, 대/소메뉴 + 권한 9종 시드.
- **정책/설계 출처**: 설계문서 §2(테이블), §3(코드그룹), §4(메뉴/권한). 메뉴/권한 시드 패턴은 `prafta-near-miss-deploy.sql` 선례 정합.

### 2.1 `tb_accident` (사고 헤더)

```sql
CREATE TABLE `tb_accident` (
    `CMPNY_CD`             varchar(50)  NOT NULL COMMENT '회사코드',
    `SITE_CD`             varchar(50)  NOT NULL COMMENT '사업장코드',
    `ACCIDENT_ID`         varchar(20)  NOT NULL COMMENT '사고 ID (사업장별 채번: ACC + YYYYMMDD + SEQ4)',
    `VICTIM_USER_TYPE_CD` varchar(10)  NOT NULL COMMENT '재해자 사용자유형[SYS050] REGULAR:정규 DAILY:일용',
    `VICTIM_USER_CD`      varchar(20)  NOT NULL COMMENT '재해자 사용자코드(tb_user.USER_CD 또는 tb_daily_user.USER_CD)',
    `OCCUR_YMD`           varchar(8)   NOT NULL COMMENT '사고 발생일(YYYYMMDD)',
    `OCCUR_TIME`          varchar(4)   NOT NULL COMMENT '발생 시각(HHMM)',
    `OCCUR_PLACE`         varchar(200)          DEFAULT NULL COMMENT '발생 장소(직접입력)',
    `ACCIDENT_GRADE_CD`   varchar(10)  NOT NULL COMMENT '재해등급[SYS065] 100:중대재해 200:일반산재 300:신고제외',
    `ACCIDENT_DESC`       varchar(1000) NOT NULL COMMENT '사고 경위',
    `EMPLOYER_DESC`       varchar(200)          DEFAULT NULL COMMENT '신고의무자(직영/하수급 등 직접입력)',
    `PROCESS_STATUS_CD`   varchar(10)  NOT NULL DEFAULT '100' COMMENT '처리상태[SYS066] 100:접수 200:처리중 300:종결',
    `USE_YN`              varchar(2)   NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
    `DEL_YN`              varchar(1)   NOT NULL DEFAULT 'N' COMMENT '삭제여부',
    `INSERT_NO`           varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`           varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`         datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `ACCIDENT_ID`),
    KEY `IX_TB_ACCIDENT_GRADE`  (`CMPNY_CD`, `SITE_CD`, `ACCIDENT_GRADE_CD`),
    KEY `IX_TB_ACCIDENT_STATUS` (`CMPNY_CD`, `SITE_CD`, `PROCESS_STATUS_CD`),
    KEY `IX_TB_ACCIDENT_OCCUR`  (`CMPNY_CD`, `SITE_CD`, `OCCUR_YMD`),
    KEY `IX_TB_ACCIDENT_VICTIM` (`CMPNY_CD`, `VICTIM_USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고관리 헤더';
```

- **`SRC_NEAR_MISS_*` 미포함 결정**: 연관 아차사고는 다건(설계문서 §2.2)이므로 헤더에 단일 FK를 두지 않고 `tb_accident_link`(LINK_DOMAIN_CD='NEAR_MISS')로 통일. (목업 `srcNearMiss` 단일값은 다건 link로 대체.)
- **`SYS050` 재해자 유형**: 설계문서가 SYS050 REGULAR/DAILY 재사용을 명시. **확인 필요(잔여확인 §8-A)**: SYS050이 실제로 REGULAR/DAILY 코드를 갖는지 미검증. TBM 출결은 `USER_TYPE_CD` 리터럴 'REGULAR'/'DAILY'를 쓰고 있어 동일 컨벤션 추정. 048-01 적용 전 `SELECT * FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS050'`로 확정.

### 2.2 `tb_accident_link` (연계 데이터 스냅샷)

```sql
CREATE TABLE `tb_accident_link` (
    `CMPNY_CD`        varchar(50)  NOT NULL COMMENT '회사코드',
    `SITE_CD`         varchar(50)  NOT NULL COMMENT '사업장코드',
    `ACCIDENT_ID`     varchar(20)  NOT NULL COMMENT '사고 ID(tb_accident.ACCIDENT_ID)',
    `LINK_DOMAIN_CD`  varchar(20)  NOT NULL COMMENT '연계도메인[SYS067] ATTD:근태 CHKPT:순회점검 RISK:위험성평가 TBM:TBM NEAR_MISS:아차사고',
    `LINK_SEQ`        int          NOT NULL COMMENT '도메인 내 확정 순번(다건)',
    `LINK_KEY_JSON`   text                  DEFAULT NULL COMMENT '연결 원본키 묶음(JSON; 예 {"chkptCd":"...","workDate":"..."} )',
    `SNAPSHOT_JSON`   text                  DEFAULT NULL COMMENT '확정 시점 조회값 고정(JSON; 사고 날짜·시각 기준 스냅샷)',
    `INSERT_NO`       varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`       varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`     datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `ACCIDENT_ID`, `LINK_DOMAIN_CD`, `LINK_SEQ`),
    KEY `IX_TB_ACCIDENT_LINK_DOMAIN` (`CMPNY_CD`, `SITE_CD`, `ACCIDENT_ID`, `LINK_DOMAIN_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고 연계 데이터 스냅샷';
```

- **스냅샷 사유**: 위험성평가/순회점검은 개선 재평가·수정으로 값이 변함 → 법적 정합성 위해 확정 시점 값을 `SNAPSHOT_JSON`에 고정(설계문서 §2.2, 목업 legend "사고 시점 정합성 보존"). 조회는 `LINK_KEY_JSON`으로 원본 추적 가능.
- **JSON 컬럼 타입**: MySQL 8.0이므로 네이티브 `JSON` 타입 사용 가능하나, 본 프로젝트 관행(스냅샷에 JSON 컬럼 선례 없음)을 따라 `text`로 둔다. **확인 필요(잔여확인 §8-B)**: JSON 네이티브 vs text. developer가 매퍼 작성 시 한 쪽으로 확정.

### 2.3 `tb_accident_legal_step` (법정 처리/기한 — 탭②)

```sql
CREATE TABLE `tb_accident_legal_step` (
    `CMPNY_CD`     varchar(50)  NOT NULL COMMENT '회사코드',
    `SITE_CD`      varchar(50)  NOT NULL COMMENT '사업장코드',
    `ACCIDENT_ID`  varchar(20)  NOT NULL COMMENT '사고 ID(tb_accident.ACCIDENT_ID)',
    `STEP_CD`      varchar(20)  NOT NULL COMMENT '절차코드(tb_accident_legal_step_master.STEP_CD)',
    `IS_DONE_YN`   varchar(2)   NOT NULL DEFAULT 'N' COMMENT '조치완료여부(Y/N) — 목업 처리버튼→체크 방식',
    `DONE_DTIME`   datetime              DEFAULT NULL COMMENT '조치완료 처리일시',
    `DONE_USER_CD` varchar(20)           DEFAULT NULL COMMENT '조치완료 처리자(tb_user.USER_CD)',
    `REMARK`       varchar(500)          DEFAULT NULL COMMENT '항목별 비고',
    `INSERT_NO`    varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`    varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`  datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `ACCIDENT_ID`, `STEP_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고 법정 처리/기한 진행상태';
```

- 사고 등록 시 등급에 해당하는 master 절차를 행으로 생성하거나(eager), 조회 시 master JOIN으로 미생성 절차를 합쳐 보여줄 수 있음(lazy). **권고: lazy** — 등급은 추후 변경 가능(목업 form-hint "등급 변경 시 법정 기한 재계산")하므로 등록 시점 고정 행 생성은 불리. 조회는 master를 등급으로 필터링하고 `tb_accident_legal_step` LEFT JOIN으로 체크/비고를 덧입힘. 체크/비고 저장 시에만 UPSERT.

### 2.4 `tb_accident_legal_step_master` (seed, 등급별 절차 정의)

```sql
CREATE TABLE `tb_accident_legal_step_master` (
    `STEP_CD`            varchar(20)  NOT NULL COMMENT '절차코드(전사 공통)',
    `ACCIDENT_GRADE_CD`  varchar(10)  NOT NULL COMMENT '적용 재해등급[SYS065] 100/200/300, 또는 ALL(전등급 공통)',
    `STEP_IDX`           int          NOT NULL COMMENT '절차 표시 순서',
    `STEP_NM`            varchar(100) NOT NULL COMMENT '절차명(예: 중대재해 발생보고)',
    `ACTION_GUIDE`       varchar(500) NOT NULL COMMENT '행동강령 문구(관리자 가이드)',
    `LEGAL_BASIS`        varchar(300)          DEFAULT NULL COMMENT '근거조문/과태료',
    `DEADLINE_RULE_CD`   varchar(20)  NOT NULL COMMENT '기한규칙[SYS068] IMMEDIATE:지체없이 MONTH_PLUS_1:발생일+1개월(산안법 시행규칙§73) NONE:기한없음 TRACK:별도트랙',
    `STEP_NOTE`          varchar(500)          DEFAULT NULL COMMENT '추가 안내(예: 시스템이 기한 계산 안 함)',
    `USE_YN`             varchar(2)   NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
    `INSERT_NO`          varchar(50)           DEFAULT 'SYSTEM' COMMENT '입력자',
    `INSERT_DATE`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`          varchar(50)           DEFAULT NULL COMMENT '수정자',
    `UPDATE_DATE`        datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`STEP_CD`),
    KEY `IX_TB_ACCIDENT_STEP_MASTER_GRADE` (`ACCIDENT_GRADE_CD`, `STEP_IDX`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고 법정절차 정의(seed)';
```

- **CMPNY_CD 없음**: 법정절차는 전국 공통 법령 기준 → 전사 공통(코드/메뉴와 동일 관행). `STEP_CD`는 회사 비종속.
- **`DEADLINE_RULE_CD`를 `SYS068`로 신설**할지, master 내 enum 문자열로만 둘지는 048-02에서 결정(아래). 본 작업지시서는 코드그룹화를 권고하지 않고 **상수 문자열** 사용을 권고(기한규칙은 3~4종 고정, 화면 로직 분기용). → SYS068 미신설, COMMENT의 `[SYS068]` 표기 제거하고 'IMMEDIATE/MONTH_PLUS_1/NONE/TRACK 중 하나'로 COMMENT 수정. **확인 필요(잔여확인 §8-C)**.

### 2.5 채번 규칙 (ACCIDENT_ID)
- 형식: `ACC` + `YYYYMMDD`(사고 발생일 OCCUR_YMD) + `SEQ4`(사업장+일자 내 순번, zero-pad 4자리). 예: `ACC20260530001`.
- 채번 소스: 아차사고(`NM`+YYYYMMDD+SEQ)와 동일 컨벤션. 시퀀스 테이블은 `tb_cmm_seq`(스냅샷 L150, `CMPNY_CD,SEQ_KEY`) 또는 아차사고가 쓰는 방식을 따른다. **확인 필요**: 아차사고 채번 구현부(`com.prafta.web.nearmiss.nearmiss01`)를 developer가 정독해 동일 SEQ 발급기를 재사용. 길이 = `ACC`(3)+`8`+`4` = 15자 ≤ varchar(20) OK.
- **기준일**: 채번 일자는 **발생일(OCCUR_YMD)** 기준(목업 ID `ACC-20260530-001`이 발생일 기준). 등록일이 아님에 주의.

### 2.6 코드그룹 seed (SYS065~067)

`tb_syst_val_m` / `tb_syst_val_d` INSERT (CMPNY_CD 없음, 전사 공통). nearMiss seed 동일 패턴.

```sql
-- 마스터
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`,`SYST_VAL_NM`,`USE_YN`,`VAL_DESC`,`INSERT_NO`) VALUES
    ('SYS065','재해등급',     'Y','tb_accident.ACCIDENT_GRADE_CD 코드','SYSTEM')
  , ('SYS066','사고 처리상태','Y','tb_accident.PROCESS_STATUS_CD 코드','SYSTEM')
  , ('SYS067','사고 연계도메인 구분','Y','tb_accident_link.LINK_DOMAIN_CD 코드','SYSTEM');

-- SYS065 재해등급
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`,`SYST_VAL_D_CD`,`SYST_VAL_D_NM`,`SORT_IDX`,`USE_YN`,`INSERT_NO`) VALUES
    ('SYS065','100','중대재해', 1,'Y','SYSTEM')
  , ('SYS065','200','일반산재', 2,'Y','SYSTEM')
  , ('SYS065','300','신고제외', 3,'Y','SYSTEM');

-- SYS066 사고 처리상태
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`,`SYST_VAL_D_CD`,`SYST_VAL_D_NM`,`SORT_IDX`,`USE_YN`,`INSERT_NO`) VALUES
    ('SYS066','100','접수',  1,'Y','SYSTEM')
  , ('SYS066','200','처리중',2,'Y','SYSTEM')
  , ('SYS066','300','종결',  3,'Y','SYSTEM');

-- SYS067 사고 연계도메인 구분
INSERT INTO `tb_syst_val_d` (`SYST_VAL_CD`,`SYST_VAL_D_CD`,`SYST_VAL_D_NM`,`SORT_IDX`,`USE_YN`,`INSERT_NO`) VALUES
    ('SYS067','ATTD','근태',      1,'Y','SYSTEM')
  , ('SYS067','CHKPT','순회점검', 2,'Y','SYSTEM')
  , ('SYS067','RISK','위험성평가',3,'Y','SYSTEM')
  , ('SYS067','TBM','TBM',        4,'Y','SYSTEM')
  , ('SYS067','NEAR_MISS','아차사고',5,'Y','SYSTEM');
```

- **SYS067 코드그룹 vs 상수 권고**: LINK_DOMAIN_CD는 5종 고정·코드 추가 가능성 낮음·DB 드롭다운/조회 라벨 필요 없음(화면은 도메인별 전용 카드) → **상수로 충분**. 다만 설계문서가 "SYS067 또는 상수"를 열어두었고, 향후 통계 화면에서 도메인별 그룹핑·라벨이 필요할 수 있어 **코드그룹 등록을 권고**(비용 낮음, 라벨 일관성↑). 위 seed는 코드그룹 등록안. 상수만 쓸 경우 SYS067 INSERT를 생략하고 `tb_accident_link.LINK_DOMAIN_CD` COMMENT의 `[SYS067]`를 제거. **최종 결정은 사용자(잔여확인 §8-D).**

### 2.7 메뉴/권한 seed (accident, MENU_IDX=8)

nearMiss(IDX=7) 선례 정합. 메뉴는 회사 비종속, 권한만 CMPNY_CD='001'.

```sql
-- 대메뉴 (MENU_SRC '001' = 웹[SYS007])
INSERT INTO `tb_syst_menu_m` (`MENU_M_ID`,`MENU_SRC`,`MENU_NM`,`MENU_IDX`,`USE_YN`,`INSERT_NO`,`INSERT_DATE`) VALUES
    ('accident','001','사고관리', 8,'Y','SYSTEM',NOW());

-- 소메뉴 (MENU_VIEW = views 하위 상대경로; viewResolver 컴포넌트명 자동 라우팅)
INSERT INTO `tb_syst_menu_d` (`MENU_D_ID`,`MENU_M_ID`,`MENU_VIEW`,`MENU_NM`,`MENU_IDX`,`USE_YN`,`INSERT_NO`,`INSERT_DATE`) VALUES
    ('Accident_01','accident','accident/Accident_01.vue','사고관리', 1,'Y','SYSTEM',NOW());

-- 권한 (AUTH_CD 9종; 안전직군은 등록·처리 가능, 삭제는 master만)
INSERT INTO `tb_syst_auth_menu`
    (`CMPNY_CD`,`AUTH_CD`,`MENU_D_ID`,`USE_YN`,`BTN_SRCH`,`BTN_NEW`,`BTN_DELT`,`BTN_SAVE`,`BTN_EXCL`,`INSERT_NO`,`INSERT_DATE`) VALUES
    ('001','master', 'Accident_01','Y','Y','Y','Y','Y','N','SYSTEM',NOW())
  , ('001','hr',     'Accident_01','Y','Y','Y','N','Y','N','SYSTEM',NOW())
  , ('001','safe',   'Accident_01','Y','Y','Y','N','Y','N','SYSTEM',NOW())
  , ('001','system', 'Accident_01','Y','Y','Y','Y','Y','N','SYSTEM',NOW())
  , ('001','00001',  'Accident_01','Y','Y','N','N','N','N','SYSTEM',NOW())
  , ('001','00004',  'Accident_01','Y','Y','N','N','N','N','SYSTEM',NOW())
  , ('001','00006',  'Accident_01','Y','Y','N','N','N','N','SYSTEM',NOW())
  , ('001','00008',  'Accident_01','Y','Y','N','N','N','N','SYSTEM',NOW())
  , ('001','99999',  'Accident_01','Y','Y','N','N','N','N','SYSTEM',NOW());
```

- **nearMiss 선례와의 차이 (의도적)**: nearMiss는 모든 역할 SRCH+SAVE만(NEW='N'). 사고관리는 **웹에서 직접 사고를 등록**(목업 "+ 사고 등록" 버튼)하므로 master/hr/safe/system에 `BTN_NEW='Y'`를 부여. 삭제(`BTN_DELT`)는 master/system만(설계문서 §7 "삭제는 master"). 엑셀(`BTN_EXCL`)은 N(MVP 미사용).
- **`MenuLockPolicy`(prafta-042 단일출처) 정합 필요**: prafta-042가 master/hr/safe 화면권한을 BE에서 강제 보정한다. 위 BTN 매트릭스가 MenuLockPolicy와 충돌하지 않는지 developer가 정합화. 특히 hr/safe의 `BTN_NEW='Y'`가 정책에 위배되면 정책 측을 따른다. **확인 필요(잔여확인 §8 — prafta-042 정합)**.
- **AUTH_CD 9종 근거**: nearMiss 선례의 9종(00001/00004/00006/00008/99999/hr/master/safe/system). 일반 역할(00001 등)은 SRCH만(열람).

### 2.8 마이그레이션 파일 경로 (운영 미적용·수동 원칙)
- `prafta-backend/src/main/resources/sql/migration/prafta-048-accident-domain.sql` — 위 4개 CREATE + SYS065~067 + (선택)SYS068 + 메뉴 + 권한을 1파일 통합(nearMiss 선례처럼).
- 별도 권고: 절차 seed(048-02)는 `prafta-048-accident-legal-step-seed.sql`로 분리(노무사 검토 게이트가 별도이므로 본체와 분리해 검토·재실행 용이).
- **운영 미적용**: 작성만 하고 운영 DB 적용은 사용자 수동(프로젝트 원칙). 파일 헤더에 부재확인 SELECT·멱등성 경고·적용 전 검증 쿼리를 nearMiss 선례처럼 포함.

---

## 3. PRAFTA-048-02 — 법정절차 seed (backend, 노무사 검토 게이트)

- **유형**: backend (seed/콘텐츠)
- **요구사항 요약**: 목업 `buildActions()` 로직을 `tb_accident_legal_step_master` 행으로 이관.
- **출처**: 설계문서 §2.4, 요청서 §4(법정 처리/기한 탭), 목업 `buildActions()` (L265~280).

### 3.1 목업 buildActions → master 행 매핑 (등급별)

목업 로직을 정독해 등급별 절차를 추출(아래는 seed 초안; 행동강령/조문 문구는 목업 그대로 이관):

| STEP_CD | 적용등급 | IDX | STEP_NM | ACTION_GUIDE | LEGAL_BASIS | DEADLINE_RULE | STEP_NOTE |
|---|---|---|---|---|---|---|---|
| `STEP_INIT` | ALL | 1 | 초기 조치 / 응급 | 부상자 처치·현장 보존·2차 재해 방지 | 사업주 일반 안전조치 의무 | NONE | |
| `STEP_CRIT_REPORT` | 100 | 2 | 중대재해 발생보고 | 재해개요·피해상황·조치·전망을 관할 지방고용노동관서에 보고하세요 | 산안법 §54② / 시행규칙 §67 · 미이행 과태료 3,000만원 | IMMEDIATE | "지체없이" — 시스템이 기한을 계산하지 않습니다. 즉시 보고 후 완료 처리하세요 |
| `STEP_CRIT_INVST` | 100 | 3 | 산업재해조사표 제출 | 관할 지방고용노동관서 제출(중대재해도 별도 제출) | 산안법 §57③ / 시행규칙 §73 | MONTH_PLUS_1 | |
| `STEP_NORM_INVST` | 200 | 2 | 산업재해조사표 제출 | 사망 또는 3일↑ 휴업 시 관할 지방고용노동관서 제출 | 산안법 §57③ / 시행규칙 §73 · 미제출 과태료 1,500만원 | MONTH_PLUS_1 | |
| `STEP_EXEMPT_REC` | 300 | 2 | 재해 기록·보존 | 신고 의무 없음. 사업장·인적사항·발생경위·재발방지계획 기록·보존 | 산안법 §57① · 3일 미만 휴업도 기록 대상 | NONE | |
| `STEP_COMP_CLAIM` | 100,200 | 4 | 근로복지공단 요양급여 신청 | 산재조사표 제출과 별개 트랙. 재해자/유족 신청, 회사 지원 | 산재보상보험법 §41 · 조사표 제출과 독립 | TRACK | |
| `STEP_INVESTIGATE` | ALL | 5 | 사고 조사 / 재발방지 계획 | 원인 분석·재발방지 대책 수립·기록 보존 | §57① 기록·보존 의무 | NONE | |
| `STEP_SETTLE` | 100,200 | 6 | 보상 / 합의 | 위로금·합의 진행 및 합의서 보관 | 민사·사내 절차 | TRACK | |

- **"적용등급 ALL / 다중등급" 처리**: master PK가 `STEP_CD` 단일이므로 한 절차가 여러 등급에 적용될 때 행을 분리하거나(STEP_CD를 등급별로 채번) `ACCIDENT_GRADE_CD='ALL'` + 조회 시 OR 분기. **권고: `ACCIDENT_GRADE_CD`에 'ALL'·'100'·'200'·'300' 단일값을 두고, 다중등급(요양급여/보상)은 행을 등급별로 복제**(STEP_CD를 `STEP_COMP_CLAIM_100`/`STEP_COMP_CLAIM_200`처럼 분리). 그래야 PK 단일·조회 단순. → 048-02에서 seed 행을 등급별로 평탄화. **확인 필요(잔여확인 §8-C 포함)**.
- **기한 계산**: `MONTH_PLUS_1` = 목업의 `rd.setMonth(occur.getMonth()+1)` (발생월+1개월). D-day·경과 표시는 **프론트에서 계산**(목업 renderActions). BE는 발생일과 규칙만 내려준다.
- **면책 문구**: 화면 하단 고정(설계문서 §6 "법정 기한·조문은 노무사 최종확인 대상"). seed 자체엔 면책 불필요.

> ⚠️ **노무사 검토 게이트(잔여확인 §8-2)**: 위 조문·과태료·기한은 목업 작성자의 정리이며 법적 검증 미완. 운영 적용 전 노무사 확인 필수. 작업지시서는 seed 구조까지만 확정하고, 문구의 법적 정확성은 검증 대상으로 남긴다.

---

## 4. PRAFTA-048-03 — accident01 백엔드 모듈 (CRUD + 목록/상세)

- **유형**: backend
- **모듈**: `com.prafta.web.accident.accident01`
- **출처**: 설계문서 §5.3(화면), §7(보안), 요청서 §4.
- **참조 패턴**: `com.prafta.web.nearmiss.nearmiss01`(가장 유사한 신규 도메인 선례) + `com.prafta.web.tbm.tbm04`(연계 조회 패턴).

### 4.1 패키지/파일 구조 (prafta 컨벤션)
```
prafta-backend/src/main/java/com/prafta/web/accident/accident01/
  ├── controller/Accident01Controller.java
  ├── application/Accident01Service.java (+ 필요 시 query/command 객체)
  ├── mapper/Accident01Mapper.java (interface)
  ├── dto/ (요청 DTO; DB 컬럼명 대문자 유지 규칙)
  └── result/ (응답 result record/class)
prafta-backend/src/main/resources/com/prafta/web/accident/accident01/mapper/Accident01Mapper.xml
```

### 4.2 엔드포인트 (`/webApi/accident01/*`, kebab-case)

| Method | Path | 설명 | 출처 |
|---|---|---|---|
| GET | `/webApi/accident01/list` | 사고 목록(좌측 리스트; 재해자·등급·상태·발생일시) | 목업 ACCIDENTS, §5.3 |
| GET | `/webApi/accident01/detail` | 사고 단건 상세(헤더 + 재해자 근무현황) | §5.3 ① |
| POST | `/webApi/accident01/create` | 사고 등록(`tb_accident` INSERT, 채번) | §5.1 [등록] |
| POST | `/webApi/accident01/update` | 사고 수정(등급/경위/상태 변경 — 등급 변경 시 기한 재계산은 FE) | 목업 form-hint |
| POST | `/webApi/accident01/delete` | 사고 soft delete(`DEL_YN='Y'`, master만) | §7 |
| GET | `/webApi/accident01/victim-search` | 재해자 검색(정규 `tb_user` + 일용 `tb_daily_user` 양 풀) | §5.1 |

- **재해자 검색(victim-search)**: 두 풀(`tb_user`·`tb_daily_user`)을 UNION하여 사업장 스코프 내 검색. 응답에 `VICTIM_USER_TYPE_CD`(REGULAR/DAILY) 포함. PII는 마스킹된 이름/`MBL_NO_LAST4`만 리스트에 노출(§7, app-010 패턴).

### 4.3 보안 가드 (필수 — 설계문서 §7)
1. `cmpnyCd`/`userCd`/`siteCd`는 **JWT 클레임에서만 도출**(요청 body의 siteCd 신뢰 금지).
2. **사업장 스코프·cross-site IDOR 차단**: 사고 조회/수정/삭제 시 `assertSiteAccess(siteCd)` 호출(prafta-034/040 선례). master/hr/safe는 전사 접근(prafta-042, `tb_user_site_auth` 자동부여). 그 외 역할은 소속/관리 사업장만.
3. **재해자 PII 마스킹**: 목록/상세에서 이름 마스킹·휴대폰 last4만. 평문 복호화는 필요 시점(상세 편집 등)만(app-010 패턴). 화면 출력에 평문 PII 금지(CLAUDE.md).
4. 등록/처리는 `BTN_NEW`/`BTN_SAVE` 권한 보유 역할만(MenuLockPolicy 정합).

### 4.4 DTO 매핑 규칙 (CLAUDE.md)
- DB 컬럼 대문자+언더스코어 → Java DTO 필드 대문자 유지(`private String ACCIDENT_ID;`).
- MyBatis result는 column→property 명시 매핑(또는 AS alias 카멜백 — 기존 매퍼 관행 확인 후 통일).
- 컬럼 콤마 leading, `#{}` 바인딩, `SELECT *` 금지.

---

## 5. PRAFTA-048-04 — 5개 연계 도메인 사고일 기준 조회 API (backend)

- **유형**: backend
- **출처**: 설계문서 §1(연계 매핑), §5.2(수평선 조회), §6(안내문구), 요청서 §3.
- **참조 패턴**: 근태=Attd_11(월집계)/Attd_07, 순회=ChkLst_01, 위험=Risk_01, TBM=Tbm_04.

각 원(수평선) 클릭 시 조회. 등록 팝업의 "연관 데이터 조회 조건"이 있으면 좁히고, 없으면 전체를 펼친다.

| Method | Path | 조회 범위 | 매칭키 | 출처 |
|---|---|---|---|---|
| GET | `/webApi/accident01/link/attendance` | **당일** | `VICTIM_USER_CD`+`OCCUR_YMD` → `tb_user_work_plan`(스케줄, 정규만) + `tb_user_attd_mgmt`(실근태). 발생시각 마커 | §1, §5.2 근태 |
| GET | `/webApi/accident01/link/patrol` | **1주일**(사고일-7 ~ 사고일) | `SITE_CD`+(`CHKLST_TYPE`,`CHKPT_CD` 선택) → `tb_chkpt_inspect_answer` `WORK_DATE` BETWEEN. 점검대상별 양호/불량 집계 | §1, §5.2 순회 |
| GET | `/webApi/accident01/link/risk` | **3개월**(사고일-3M ~ 사고일) | `SITE_CD`+(`PROCESS_CD`/`RISK_TYPE_CD`/`HAZARD_CD` 0~3계층 부분입력) → `tb_risk_assessment` | §1, §5.2 위험 |
| GET | `/webApi/accident01/link/tbm` | **당일 고정** | `SITE_CD`+`DATE(OPENED_AT)`=OCCUR_YMD → `tb_tbm_session` + `tb_tbm_attendance`(재해자 이수여부) | §1, §5.2 TBM |
| GET | `/webApi/accident01/link/near-miss` | **3개월** | `SITE_CD`+`OCCUR_DTIME` BETWEEN, (`INCIDENT_TYPE_CD`/`POTENTIAL_SEVERITY_CD` 선택) → `tb_near_miss` `USE_YN='Y'` | §1, §5.2 아차 |
| GET | `/webApi/accident01/patrol/chkpt-options` | — | 점검대상 검색 옵션(ChkptSearchPop용; `tb_chkpt_type_mgmt` SITE+CHKLST_TYPE 필터) | §5.1, 048-07 |
| GET | `/webApi/accident01/risk/category-options` | — | 위험성평가 3계층 드롭다운 옵션(`tb_risk_type`/`tb_risk_site_hazard`) | §5.1 |

### 5.1 도메인별 조회 세부

**근태(attendance)**: 정규직은 `tb_user_work_plan`(스케줄, WORK_PLAN_CD→`tb_sch_mgmt` 시각 결합) + `tb_user_attd_mgmt`(WORK_SEQ별 CHECK_IN/OUT). **일용직은 스케줄 없음**(정책 04-user-tracks §4.3) → 실근태만, 그마저 현재 데이터 0건일 수 있음(모바일 미완료). 응답에 `hasSchedule:false`·`scheduleNote:"일용직은 스케줄 없음"`·실근태 empty 시 안내 플래그. 발생시각 마커는 `OCCUR_TIME`을 타임라인 좌표로 FE 전달.

**순회점검(patrol)**: `tb_chkpt_inspect_answer`를 `CHKPT_CD` 기준 그룹, `WORK_DATE` BETWEEN [OCCUR_YMD-7, OCCUR_YMD]. 집계 = `COUNT(*)` 총항목, `SUM(INSPECT_ANSWER_TYPE='Y')` 양호, `SUM('N')` 불량, 불량항목(`ANSWER_DESC`) 목록. 조건 미입력 시 사업장 전체 CHKPT_CD를 펼침(관리자가 선택). (설계문서 §1.1: SYS009 Y=양호/N=불량. 목업 "정상/미흡"은 양호/불량 매핑.)

**위험성평가(risk)**: 3계층(`PROCESS_CD`→`RISK_TYPE_CD`→`HAZARD_CD`) 0~3 부분입력. 미입력 계층은 하위 전체. `INIT_ASSESS_DATE`가 사고일-3M ~ 사고일인 유효 평가. 리스트 = 평가코드·유해요인·위험도(INIT_RISK_LV/REVAL_RISK_LV)·진행상태(SYS011). (설계문서 §1.1: 목업 "위험분류/위험발생상황" 2단 → 3계층 대체.)

**TBM(tbm)**: `DATE(OPENED_AT)=OCCUR_YMD`인 당일 세션. 재해자 이수여부 = `tb_tbm_attendance`에서 `USER_CD=VICTIM_USER_CD` AND `USER_TYPE_CD`(REGULAR/DAILY) AND `COMPLETION_STATUS_CD`. 기록 없으면 "시스템 기록 없음"(목업 pill no). 세션 상태는 SYS046.

**아차사고(near-miss)**: `OCCUR_DTIME` BETWEEN [OCCUR_YMD-3M 00:00, OCCUR_YMD 23:59], `USE_YN='Y'`. (`INCIDENT_TYPE_CD`/`POTENTIAL_SEVERITY_CD` 선택 필터.) 전조 선택 시 link로 확정.

### 5.2 안내문구 (설계문서 §6 — FE가 각 원 화면에 표시, BE 응답에 `notice` 필드로 동봉 권고)
- 순회: "사고일로부터 1주일 이내 점검 결과를 집계합니다. (양호/불량 기준)"
- 위험: "사고일로부터 최근 3개월 이내 유효 위험성평가입니다. (사고 날짜·시각 기준 조회)"
- 아차: "사고일로부터 최근 3개월 이내 보고된 아차사고/사건입니다."
- TBM: "사고 발생 당일 진행된 TBM만 표시합니다. (당일 기준 고정)"
- 공통: "모든 항목은 본 시스템 기록 기준이며, '기록 없음'은 행위 부재가 아니라 입력 부재일 수 있습니다."

### 5.3 보안 (전 조회 공통)
- 위 모든 link 조회는 **신규 cross-site 조회 경로**다. `siteCd`를 JWT/사고 헤더에서만 도출하고 `assertSiteAccess` 가드 필수(다른 사업장 근태/순회/위험/TBM/아차 데이터를 사고 ID 조작으로 읽지 못하게). prafta-034/040 IDOR 선례 준수.
- 재해자 근태 조회는 PII(휴대폰) 미포함, 이름 마스킹.

---

## 6. PRAFTA-048-05 — 확정 스냅샷 저장 + 법정절차 체크/비고 저장 (backend)

- **유형**: backend
- **출처**: 설계문서 §2.2(스냅샷), §2.3(법정절차), §5.2 [확인], §5.3 ②.
- **선행**: 048-02(절차 seed), 048-04(조회).

| Method | Path | 설명 |
|---|---|---|
| POST | `/webApi/accident01/link/confirm` | 수평선 [확인] → 4도메인(+아차) 확정 결과를 `tb_accident_link`에 스냅샷 INSERT/REPLACE(도메인당 다건) |
| GET | `/webApi/accident01/link/snapshot` | ①탭 — 확정된 `tb_accident_link` 스냅샷 조회(카드 표시) |
| GET | `/webApi/accident01/legal-step/list` | ②탭 — 등급별 master 절차 + 진행상태(`tb_accident_legal_step` LEFT JOIN) |
| POST | `/webApi/accident01/legal-step/save` | ②탭 — 조치완료 체크/비고 UPSERT(`IS_DONE_YN`/`DONE_DTIME`/`DONE_USER_CD`/`REMARK`) |
| GET | `/webApi/accident01/legal-step/history` | ③탭 — 완료체크·비고·완료일시를 시간순 롤업(파생 뷰, 별도 저장 없음) |

- **③ 처리이력 = 파생 뷰**(설계문서 §5.3): 목업의 freeform timeline(LOGS) **폐기**. ②탭의 `tb_accident_legal_step`에서 `IS_DONE_YN='Y'`인 행을 `DONE_DTIME` 순으로 정렬해 읽기 쉽게 보여줌. **별도 입력 테이블 없음**.
- **스냅샷 저장**: confirm 시 각 도메인의 확정 선택을 `LINK_KEY_JSON`(원본키) + `SNAPSHOT_JSON`(조회시점 값)으로 적재. 재확정 시 해당 사고+도메인 link 삭제 후 재INSERT(REPLACE 전략) 권고.
- **법정절차 lazy UPSERT**: save 시 `tb_accident_legal_step` PK(`...,STEP_CD`) UPSERT. 미존재 시 INSERT, 존재 시 UPDATE. 등급 변경으로 STEP 구성이 바뀌어도 기존 행은 보존(추적성).
- **보안**: 모든 쓰기는 `assertSiteAccess` + `BTN_SAVE` 권한. confirm/save 처리자 `DONE_USER_CD`/`UPDATE_NO`는 JWT userCd.

---

## 화면 명세 (frontend 작업)

> 디자인 토큰/패턴 정독 결과: prafta 웹은 **Options-style + setup 혼용, scoped CSS, `viewComm`/`ViewHeader`/`viewSearch`/`data-grid` 레이아웃**을 쓴다. 공통 컴포넌트: `ViewHeader`(상단 버튼바), `BaseSelect`, 전역 팝업 `SiteSearchPop`/`UsersMultiSearchPop`/`UserSearchPop`. CSS 변수는 각 화면 scoped 또는 전역 토큰을 따른다(목업의 `--primary`/`--text` 류는 prafta 토큰으로 치환; 하드코딩 금지). 신규 화면은 nearMiss(`NearMiss_01.vue`)의 좌목록+우상세 구조와 chkLst의 검색바를 참조한다.

### UI-001 Accident_01 (사고관리 메인 — 3탭 + 등록팝업 + 수평선 확정)
- 연결 작업: PRAFTA-048-06
- 화면 위치: `src/views/accident/Accident_01.vue` (+ `src/views/accident/popup/AccidentCreatePop.vue`, `AccidentLinkConfirmPop.vue`, 도메인별 조회 팝업)
- 참조 패턴: `views/nearMiss/NearMiss_01.vue`(좌목록+우상세 3탭), `views/tbm/Tbm_04.vue`(이력), `views/chkLst/ChkLst_01.vue`(검색바+data-grid)
- 현재 동작: 신규 작성
- 백엔드 의존: 048-03(list/detail/create), 048-04(link/*), 048-05(snapshot/legal-step/*)

**레이아웃 와이어프레임 (메인)**:
```
┌ ViewHeader: 사고관리           [+ 사고 등록] ┐
├──────────────┬──────────────────────────────┤
│ 사고 목록(좌) │ [재해자명] [등급칩][상태칩]    │
│ ┌──────────┐ │ ACC... · 사고일시 · 장소·사업장 │
│ │이현수 中  │ │ ┌탭①안전관리현황 ②법정처리 ③이력┐│
│ │05-30 ...  │ │ │                              ││
│ │ACC-...    │ │ │  (탭 콘텐츠)                  ││
│ ├──────────┤ │ │                              ││
│ │박정호 산재│ │ └──────────────────────────────┘│
│ └──────────┘ │                                │
│ [등급 범례]   │                                │
└──────────────┴──────────────────────────────┘
```

**탭별 콘텐츠**:
- **① 안전관리 현황**: `link/snapshot` 스냅샷 카드(재해자/근무현황, 당일 TBM, 순회점검 이력, 위험성평가) — 목업 grid 레이아웃. 하단 면책 legend.
- **② 법정 처리/기한**: `legal-step/list` 절차 카드(번호·절차명·행동강령·근거조문). 우측 기한/D-day(FE 계산). **조치완료 체크박스 + 항목별 비고 입력** + [저장]. 면책 banner.
- **③ 처리 이력**: `legal-step/history` 파생 롤업(완료 절차를 시간순). 입력 폼 없음.

**사고 등록 팝업 (AccidentCreatePop)** — 목업 modal 확장:
```
사고 발생일* | 발생시각*
발생 사업장*  (SiteSearchPop 연동)
재해자*       (정규+일용 검색; victim-search)
── 연관 데이터 조회 조건 ──
순회점검: 점검구분(COM001 BaseSelect) + 점검대상(CHKPT_CD, 다건, ChkptSearchPop 신규)  ← 요청서 §2 추가요구
위험성평가: 공정 → 위험요인구분 → 유해요인 (3계층 BaseSelect, 0~3 부분)
아차사고: 사건유형(SYS061) + 잠재중대성(SYS062)  ← 요청서 §2 추가요구(흐름 정의)
── 사고 내용 ──
재해등급*(SYS065 3택 카드) + 사고 경위*
[취소] [등록] → 수평선 확정화면
```

**수평선 확정 화면 (AccidentLinkConfirmPop, `사고관리_temp.png`)**:
```
   ●─────────●─────────●─────────●     (+ 아차사고)
  근태     순회점검  위험성평가   TBM
  각 원 클릭 → 도메인 조회결과 팝업(확인·다건선택·확정)
  [확인] → link/confirm 스냅샷 저장 → ①탭 집계
```
각 원 팝업은 §6 안내문구 표시. 등록 조건 있으면 좁혀 표시, 없으면 당일/기간 전체 펼침.

**상태별 동작**: loading=스피너(`LoadingSpinner`), empty="기록 없음"(행위부재 아님 안내), error=alert 모달, success=목록 갱신.

### UI-002 ChkptSearchPop (점검대상 검색, 다건)
- 연결 작업: PRAFTA-048-07
- 화면 위치: `src/components/popup/ChkptSearchPop.vue` (전역 공용 팝업; `SiteSearchPop` 형제)
- 참조 패턴: `components/popup/SiteSearchPop.vue`(드래그 모달+data-grid+조회), `UsersMultiSearchPop.vue`(다건 체크 선택)
- 현재 동작: 신규 작성
- 백엔드 의존: 048-04 `patrol/chkpt-options`
- 동작: 사업장+점검구분(CHKLST_TYPE) 필터로 `tb_chkpt_type_mgmt` 점검대상 검색, **체크박스 다건 선택** 후 부모(등록팝업)로 emit. 목업 onChklstChange의 "점검대상" 2단 요구(요청서 §2) 충족.

---

## Vue 골격 (frontend — 승인 후 디스크 작성)

> ⚠️ planner는 **승인 전 Write 금지**. 아래는 골격 초안이다. script는 import/ref 선언·props/emits까지만, 로직은 `// TODO(developer):`로 위임한다. CSS는 scoped, prafta 토큰만(하드코딩 금지). 본 작업지시서에는 신규 공용 팝업 **ChkptSearchPop**의 골격만 대표로 싣고, `Accident_01.vue` 등 대형 화면 골격은 048-06 착수 시점에 디자인 토큰 최종 확정(prafta 전역 CSS 변수 파일 정독) 후 별도 제시한다. (사유: Accident_01은 3탭+다수 팝업으로 골격 분량이 크고, 토큰 치환 정확도를 위해 착수 직전 정독이 필요.)

```vue
<!-- src/components/popup/ChkptSearchPop.vue -->
<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-wide" ref="modalRef">
        <div class="modal-header" @mousedown="startDrag">
          <span>점검대상 검색</span>
          <button class="icon-button" @click="$emit('close')" aria-label="닫기">✕</button>
        </div>

        <div class="viewSearch">
          <div class="form-left">
            <label>점검구분</label>
            <BaseSelect v-model="chklstType">
              <option value="">— 전체 —</option>
              <!-- TODO(developer): COM001 베이스코드 옵션 바인딩 -->
            </BaseSelect>
            <label>점검대상명</label>
            <input v-model.trim="chkptNm" />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>

        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th class="check-col">
                    <input type="checkbox" v-model="headChk" @change="fnHeadChk" />
                  </th>
                  <th>점검구분</th>
                  <th>점검대상명</th>
                  <th>관리자</th>
                </tr>
              </thead>
              <tbody>
                <!-- TODO(developer): rows v-for + 행 체크 바인딩 -->
              </tbody>
            </table>
          </div>
        </div>

        <div class="modal-foot">
          <button class="btn btn-ghost" @click="$emit('close')">취소</button>
          <button class="btn btn-primary" @click="fnSelect">선택({{ selectedCount }})</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed } from "vue";
import BaseSelect from "@/components/common/BaseSelect.vue";

const props = defineProps({
  siteCd: { type: String, default: "" }, // 부모(등록팝업)에서 선택된 사업장
});
const emit = defineEmits(["close", "select"]);

// 반응형 상태 (developer: 초기값/리셋 보완)
const chklstType = ref("");
const chkptNm = ref("");
const headChk = ref(false);
const rows = ref([]);        // TODO(developer): 조회 결과 채움
const modalRef = ref(null);

const selectedCount = computed(() => rows.value.filter((r) => r.checked).length);

// UI 토글/단순 동작만 (비즈니스/ API는 developer)
const startDrag = () => {
  // TODO(developer): 드래그 이동 (SiteSearchPop 패턴 재사용)
};
const fnHeadChk = () => {
  // TODO(developer): 전체선택 토글
};
const fnSearch = () => {
  // TODO(developer): GET /webApi/accident01/patrol/chkpt-options 호출 (siteCd, chklstType, chkptNm)
};
const fnSelect = () => {
  // TODO(developer): 선택 행 다건 emit('select', selectedRows) 후 close
};
</script>

<style scoped>
/* prafta 전역 토큰만 사용. 하드코딩 색상/픽셀 금지. !important 금지. */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: var(--overlay-bg, rgba(0, 0, 0, 0.45));
  display: flex;
  align-items: flex-start;
  justify-content: center;
}
.modal-content-wide {
  background: var(--color-surface, #fff);
  border-radius: var(--radius-md);
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md);
  cursor: move;
}
.modal-foot {
  display: flex;
  gap: var(--space-sm);
  justify-content: flex-end;
  padding: var(--space-md);
}
/* TODO(developer): 048-06 착수 시 prafta 전역 CSS 변수 파일 정독 후
   --overlay-bg/--radius-md/--space-* 등 실제 토큰명으로 치환 검증 */
</style>
```

> 위 골격의 토큰명(`--space-md` 등)은 **가정값**이다. 048-07 착수 직전 prafta 전역 CSS 변수 정의 파일을 grep(`--color-`/`--space-`/`--radius-`)으로 정독해 실제 토큰명으로 확정한다(planner 화면작업 1단계 규칙). 기존 팝업이 토큰 대신 고정 클래스(`modal-content-wide`)에 의존하면 그 클래스 재사용을 우선한다.

---

## 7. 착수 순서 / 의존성 (요약)

```
PRAFTA-048-01 (DDL+코드+메뉴/권한)  ── blocking 선행 ──┐
                                                       ├─ 048-03 (CRUD) ─┬─ 048-04 (연계조회) ─ 048-05 (스냅샷/절차저장)
PRAFTA-048-02 (절차 seed; 노무사게이트) ───────────────┘                  │
                                                                          └─ 048-06 (Accident_01 화면; API 계약 후)
PRAFTA-048-07 (ChkptSearchPop) ── 독립, 048-04 chkpt-options와만 결합 ── 048-06과 병렬
```
- **1차 착수 권고**: 048-01 → (048-02 병렬) → 048-03 → 048-04. 화면(048-06/07)은 API 계약 확정 후.
- 5개 초과 분해 방지: 본 작업지시서는 7개 단위지만 prafta-048 단일 요청의 자연 분해 단위이며 임의 기능 추가는 없음. 필요 시 048-04를 도메인별로 더 쪼갤 수 있으나 1차는 통합 유지.

---

## 8. 잔여 확인사항 (개발 전 — 설계문서 §8 + 본 분해서 발견사항)

설계문서 §8의 4건을 작업지시서에도 명시하고, 분해 중 발견한 추가 확인사항을 덧붙인다.

**설계문서 §8 원본 4건**:
1. **(§8-1) `tb_user_attd_mgmt` 일용직 실데이터**: 일용직 출퇴근을 실제 저장하는지. 현재 모바일 미완료로 데이터 0건 전제(정책 04-user-tracks §4.3). 근태 조회 화면은 "스케줄 없음 + 실근태만(없을 수 있음)"를 정상 상태로 처리. → 048-04 attendance 응답 플래그로 대응(확정).
2. **(§8-2) 법정절차 seed 노무사 검토**: §3의 조문·과태료·기한 문구의 법적 정확성. 운영 적용 전 노무사 확인. **048-02 운영 적용을 이 검토 게이트 뒤로 둔다.**
3. **(§8-3) 메뉴/권한 CMPNY_CD 범위**: 메뉴(`tb_syst_menu_m/d`)는 CMPNY_CD 없음(전사 공통, 검증 완료). 권한(`tb_syst_auth_menu`)만 `'001'` 단일. **멀티테넌트 환경이면 회사별 권한 행 추가 필요**(nearMiss 선례와 동일 미해결 항목).
4. **(§8-4) SYS067 코드그룹 vs 상수**: §2.6 권고 = 코드그룹 등록(라벨 일관성). 최종 사용자 결정 대기.

**분해 중 추가 발견 (planner)**:
- **(A) SYS050 재해자유형 코드 검증**: `tb_accident.VICTIM_USER_TYPE_CD`가 SYS050 REGULAR/DAILY를 재사용한다는 설계 전제를 실제 `tb_syst_val_d WHERE SYST_VAL_CD='SYS050'`로 미검증. 048-01 적용 전 확인. (TBM은 USER_TYPE_CD 리터럴 'REGULAR'/'DAILY' 사용 중 — 동일 컨벤션 추정.)
- **(B) link 스냅샷 JSON 컬럼 타입**: 네이티브 `JSON` vs `text`. 본 분해서는 `text` 권고(프로젝트에 JSON 스냅샷 선례 없음). developer가 매퍼 작성 시 확정.
- **(C) 법정절차 다중등급 표현**: 한 절차(요양급여/보상)가 100·200 두 등급에 걸침. master PK 단일(`STEP_CD`) 제약상 행을 등급별 복제할지(`STEP_COMP_CLAIM_100/200`) `ACCIDENT_GRADE_CD='ALL'`/OR 분기로 둘지. **권고: 등급별 복제**. 또한 `DEADLINE_RULE_CD`를 SYS068 코드그룹으로 만들지 상수로 둘지(권고: 상수). 사용자 결정 필요.
- **(D) prafta-042 MenuLockPolicy 정합**: §2.7 권한 BTN 매트릭스(master/hr/safe에 `BTN_NEW='Y'`)가 prafta-042 `MenuLockPolicy` 단일출처와 충돌하는지. 충돌 시 정책 측 우선(정책서 우선순위 규칙). developer가 정합화하며, hr/safe의 등록 권한 부여 여부는 사용자 확정 필요(nearMiss는 SAVE만 부여했고 NEW는 전 역할 'N'이었음 — 사고관리는 웹 직접등록이 필수라 차이를 둠).
- **(E) ACCIDENT_ID 채번기 재사용**: 아차사고(`NM`+YYYYMMDD+SEQ) 채번 구현부를 developer가 정독해 동일 SEQ 발급기(`tb_cmm_seq` 또는 별도)를 재사용. 발생일 기준 채번(등록일 아님) 확정.

> 위 A~E 중 사용자 결정이 필요한 항목(C·D·SYS067·§8-3 멀티테넌트)은 개발 착수 전 또는 048-01 적용 전 확정한다. 나머지(A·B·E)는 developer가 실제 스키마/구현부 정독으로 해소 가능.

---

## 부록. 목업과 실제 시스템의 매핑 정정표 (developer 참고)

| 목업 표현 | 실제 시스템 | 근거 |
|---|---|---|
| 순회점검 "점검구분" (HIGH_WORK 등) | `CHKLST_TYPE` (베이스코드 `COM001`) | 설계 §1.1, ChkLst_01.vue |
| 순회점검 결과 "정상/미흡" | `SYS009` Y(양호)/N(불량) | 설계 §1.1, tb_chkpt_inspect_answer |
| 위험성평가 "위험분류/위험발생상황" 2단 | 3계층 PROCESS_CD→RISK_TYPE_CD→HAZARD_CD | 설계 §1.1, tb_risk_assessment |
| ③탭 freeform 처리이력(LOGS) | 폐기 → ②탭 완료체크/비고 파생 롤업 | 설계 §5.3, 요청서 §4 |
| 등급칩 critical/normal/exempt | SYS065 100/200/300 | 설계 §3 |
| 상태칩 open/proc/done | SYS066 100/200/300 | 설계 §3 |
| srcNearMiss 단일값 | tb_accident_link(NEAR_MISS, 다건) | 설계 §2.2 |
| "처리 ▸" 버튼 | 조치완료 체크박스 + 비고 | 요청서 §4, 설계 §5.3② |
```
