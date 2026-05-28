# PRAFTA DB 스키마 개요

- 출처: `.claude/context/schema-full.sql` (mysqldump 스냅샷)
- 추출일: 2026-05-21
- DBMS: MySQL 8.0.42 / CHARSET utf8mb4 / COLLATE utf8mb4_unicode_ci
- 테이블 수: 61개 (뷰/트리거/프로시저 없음)

> 컬럼명은 DB 원본(대문자+언더스코어) 그대로 표기. PK 컬럼은 컬럼명에 🔑 표시.

## 목차

| # | 그룹 | 테이블 | 설명 | 컬럼수 |
|---|------|--------|------|--------|
| 1 | 회사 / 조직 / 사업장 | [`tb_cmpny`](#tb_cmpny) | - | 12 |
| 2 | 회사 / 조직 / 사업장 | [`tb_site`](#tb_site) | - | 18 |
| 3 | 회사 / 조직 / 사업장 | [`tb_site_node`](#tb_site_node) | 일일계정 슬롯(현재 점유 상태만 관리) | 13 |
| 4 | 사용자 / 계정 | [`tb_user`](#tb_user) | 사용자 | 31 |
| 5 | 사용자 / 계정 | [`tb_del_user`](#tb_del_user) | - | 5 |
| 6 | 사용자 / 계정 | [`tb_user_device`](#tb_user_device) | 정규직 디바이스 점유 (글로벌 유니크 - 회사 가로질러 1디바이스=1계정) | 13 |
| 7 | 사용자 / 계정 | [`tb_user_site_auth`](#tb_user_site_auth) | - | 8 |
| 8 | 사용자 / 계정 | [`tb_user_service_credit`](#tb_user_service_credit) | 사용자 경력 인정 (점진 부여 전용) | 11 |
| 9 | 사용자 / 계정 | [`tb_user_hire_date_history`](#tb_user_hire_date_history) | 입사일 변경 이력 (노무 감사용) | 10 |
| 10 | 인증 / 보안 | [`tb_auth_token`](#tb_auth_token) | - | 16 |
| 11 | 인증 / 보안 | [`tb_sms_auth_code`](#tb_sms_auth_code) | - | 10 |
| 12 | 인증 / 보안 | [`tb_terms`](#tb_terms) | - | 11 |
| 13 | 인증 / 보안 | [`tb_terms_id_version`](#tb_terms_id_version) | - | 8 |
| 14 | 인증 / 보안 | [`tb_terms_user_agr_mgmt`](#tb_terms_user_agr_mgmt) | - | 6 |
| 15 | 근태 - 출퇴근 | [`tb_user_attd_mgmt`](#tb_user_attd_mgmt) | 근태관리 | 18 |
| 16 | 근태 - 출퇴근 | [`tb_user_attd_hist`](#tb_user_attd_hist) | 근태 처리 이력 | 17 |
| 17 | 근태 - 출퇴근 | [`tb_user_attd_gps`](#tb_user_attd_gps) | 근태 GPS 기록 | 15 |
| 18 | 근태 - 출퇴근 | [`tb_user_attd_req`](#tb_user_attd_req) | 사용자 근태 관련 요청 관리 | 26 |
| 19 | 근태 - 출퇴근 | [`tb_attd_std_time_rule`](#tb_attd_std_time_rule) | - | 7 |
| 20 | 근태 - 출퇴근 | [`tb_attd_std_time_rule_his`](#tb_attd_std_time_rule_his) | - | 6 |
| 21 | 근태 - 연차 정책/타입 | [`tb_leave_policy`](#tb_leave_policy) | 회사 법정 연차 부여 정책 (7개 axis) | 21 |
| 22 | 근태 - 연차 정책/타입 | [`tb_leave_policy_history`](#tb_leave_policy_history) | 연차 정책 변경 이력 | 10 |
| 23 | 근태 - 연차 정책/타입 | [`tb_leave_type_mgmt`](#tb_leave_type_mgmt) | - | 32 |
| 24 | 근태 - 연차 정책/타입 | [`tb_leave_usage_policy`](#tb_leave_usage_policy) | 연차 사용 단위 정책 (휴게시간/시간단위 시작시각/1일환산은 시스템 강제) | 11 |
| 25 | 근태 - 연차 부여/사용 | [`tb_user_leave_grant`](#tb_user_leave_grant) | 사용자 연차 부여 이력 | 22 |
| 26 | 근태 - 연차 부여/사용 | [`tb_user_leave_use`](#tb_user_leave_use) | 사용자 연차 사용 실적 | 24 |
| 27 | 근태 - 연장근무 / 근무계획 | [`tb_user_overtime_mgmt`](#tb_user_overtime_mgmt) | 사용자 초과근무 실적 관리 | 27 |
| 28 | 근태 - 연장근무 / 근무계획 | [`tb_user_work_plan`](#tb_user_work_plan) | 사용자 근무 계획 | 9 |
| 29 | 근태 - 휴일 | [`tb_holiday`](#tb_holiday) | 휴일관리 | 10 |
| 30 | 근태 - 휴일 | [`tb_holiday_rule`](#tb_holiday_rule) | 휴일규칙(매년고정 회사휴일) | 11 |
| 31 | 스케줄 / 교대근무 | [`tb_sch_mgmt`](#tb_sch_mgmt) | 사업장 근무타입 관리 | 18 |
| 32 | 스케줄 / 교대근무 | [`tb_sch_mgmt_hist`](#tb_sch_mgmt_hist) | 사업장 근무타입 이력관리 | 14 |
| 33 | 스케줄 / 교대근무 | [`tb_shift_sch_mgmt`](#tb_shift_sch_mgmt) | - | 12 |
| 34 | 스케줄 / 교대근무 | [`tb_shift_sch_ptrn_mgmt`](#tb_shift_sch_ptrn_mgmt) | - | 9 |
| 35 | 스케줄 / 교대근무 | [`tb_shift_sch_assign_mgmt`](#tb_shift_sch_assign_mgmt) | - | 11 |
| 36 | 스케줄 / 교대근무 | [`tb_shift_sch_team_mgmt`](#tb_shift_sch_team_mgmt) | - | 11 |
| 37 | 스케줄 / 교대근무 | [`tb_shift_sch_team_meta_info`](#tb_shift_sch_team_meta_info) | - | 9 |
| 38 | 스케줄 / 교대근무 | [`tb_shift_sch_team_user`](#tb_shift_sch_team_user) | 교대근무 팀 소속 사용자 관리 | 11 |
| 39 | 일용직 계정 / 슬롯 | [`tb_daily_user`](#tb_daily_user) | 일용직 사용자 | 22 |
| 40 | 일용직 계정 / 슬롯 | [`tb_daily_user_slot`](#tb_daily_user_slot) | 일일계정 슬롯(현재 점유 상태만 관리) | 12 |
| 41 | 일용직 계정 / 슬롯 | [`tb_daily_user_slot_his`](#tb_daily_user_slot_his) | 일일계정 슬롯 사용 이력 | 14 |
| 42 | 일용직 계정 / 슬롯 | [`tb_daily_link_mgmt`](#tb_daily_link_mgmt) | 일용직 계정 생성 링크 관리 | 14 |
| 43 | 일용직 계정 / 슬롯 | [`tb_daily_user_link_policy`](#tb_daily_user_link_policy) | 사업장별 일일계정 발급 정책 | 8 |
| 44 | 점검 (체크포인트) | [`tb_chkpt_type_mgmt`](#tb_chkpt_type_mgmt) | - | 12 |
| 45 | 점검 (체크포인트) | [`tb_chkpt_inspect_item`](#tb_chkpt_inspect_item) | - | 11 |
| 46 | 점검 (체크포인트) | [`tb_chkpt_inspect_answer`](#tb_chkpt_inspect_answer) | - | 12 |
| 47 | 위험성평가 | [`tb_risk_assessment`](#tb_risk_assessment) | - | 28 |
| 48 | 위험성평가 | [`tb_risk_type`](#tb_risk_type) | - | 11 |
| 49 | 위험성평가 | [`tb_risk_site_hazard`](#tb_risk_site_hazard) | - | 11 |
| 50 | TBM 교육 | [`tb_tbm_edu_mtrl`](#tb_tbm_edu_mtrl) | - | 10 |
| 51 | TBM 교육 | [`tb_tbm_edu_mtrl_item`](#tb_tbm_edu_mtrl_item) | - | 12 |
| 52 | 시스템 - 메뉴 / 공통코드 | [`tb_syst_menu_m`](#tb_syst_menu_m) | - | 10 |
| 53 | 시스템 - 메뉴 / 공통코드 | [`tb_syst_menu_d`](#tb_syst_menu_d) | - | 11 |
| 54 | 시스템 - 메뉴 / 공통코드 | [`tb_syst_auth_menu`](#tb_syst_auth_menu) | - | 13 |
| 55 | 시스템 - 메뉴 / 공통코드 | [`tb_syst_val_m`](#tb_syst_val_m) | - | 10 |
| 56 | 시스템 - 메뉴 / 공통코드 | [`tb_syst_val_d`](#tb_syst_val_d) | - | 12 |
| 57 | 운영사 변수 | [`tb_baim_val_m`](#tb_baim_val_m) | - | 11 |
| 58 | 운영사 변수 | [`tb_baim_val_d`](#tb_baim_val_d) | - | 13 |
| 59 | 공통 / 시퀀스 / 파일 | [`tb_cmm_seq`](#tb_cmm_seq) | - | 4 |
| 60 | 공통 / 시퀀스 / 파일 | [`seq_site_cd`](#seq_site_cd) | - | 2 |
| 61 | 공통 / 시퀀스 / 파일 | [`tb_file_info`](#tb_file_info) | - | 10 |

## 회사 / 조직 / 사업장

### tb_cmpny

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| CMPNY_NM | varchar(50) | NO |  | 회사명 |
| BSNS_LCN_NO | varchar(50) | NO |  | 사업자번호 |
| ADDR_1 | varchar(100) | YES | NULL | 주소 |
| ADDR_2 | varchar(200) | YES | NULL | 상세주소 |
| ZIP_CODE | varchar(50) | YES | NULL | 우편번호 |
| USE_YN | varchar(2) | YES | NULL | 사용여부 |
| CONTRACT_YN | varchar(2) | YES | NULL | 계약여부 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_cmpny` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `CMPNY_NM` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사명',
  `BSNS_LCN_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업자번호',
  `ADDR_1` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '주소',
  `ADDR_2` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '상세주소',
  `ZIP_CODE` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '우편번호',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사용여부',
  `CONTRACT_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계약여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_site

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| SITE_NO | varchar(50) | NO |  | 사업장번호 |
| SITE_NM | varchar(100) | NO |  | 사업장명 |
| ADDR_1 | varchar(200) | YES | NULL | 주소 |
| ADDR_2 | varchar(200) | YES | NULL | 상세주소 |
| ZIP_CODE | varchar(20) | YES | NULL | 우편번호 |
| STR_DATE | varchar(8) | YES | NULL | 사업개시일 |
| END_DATE | varchar(8) | YES | NULL | 사업종료일 |
| USE_YN | varchar(2) | YES | 'Y' | 사용여부 |
| SITE_ADMIN_CD | varchar(50) | YES | NULL | 사업장관리자코드 |
| TEL_NO | varchar(20) | YES | NULL | 사업장전화번호 |
| GPS_RANGE | varchar(4) | YES | NULL |  |
| SITE_DESC | varchar(500) | YES | NULL | 비고 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | date | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`SITE_CD`,`CMPNY_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_site` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SITE_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장번호',
  `SITE_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장명',
  `ADDR_1` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '주소',
  `ADDR_2` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '상세주소',
  `ZIP_CODE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '우편번호',
  `STR_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업개시일',
  `END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업종료일',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `SITE_ADMIN_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장관리자코드',
  `TEL_NO` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장전화번호',
  `GPS_RANGE` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SITE_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` date DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`SITE_CD`,`CMPNY_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_site_node

**일일계정 슬롯(현재 점유 상태만 관리)**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(10) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(20) | NO |  | 사업장코드 |
| 🔑 NODE_CD | varchar(50) | NO |  | 노드ID |
| NODE_NM | varchar(200) | NO |  | 노드명 |
| NODE_TYPE | varchar(5) | NO |  | 노드타입[COM004] |
| PARENT_NODE_CD | varchar(50) | YES | NULL | 부모노드ID |
| SELF_ATTD_APPRV_YN | char(1) | NO | 'N' | 자체근태승인여부 |
| MAIN_ADMIN_CD | varchar(50) | YES | NULL | 부서 정 관리자 |
| SUB_ADMIN_CD | varchar(50) | YES | NULL | 부서 부 관리자 |
| INSERT_NO | varchar(50) | NO |  | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | NO |  | 수정자 |
| UPDATE_DATE | datetime | NO | CURRENT_TIMESTAMP | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`NODE_CD`)`
- **INDEX**: `KEY `IX_NODE_PARENT` (`CMPNY_CD`,`SITE_CD`,`PARENT_NODE_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_site_node` (
  `CMPNY_CD` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `NODE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '노드ID',
  `NODE_NM` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '노드명',
  `NODE_TYPE` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '노드타입[COM004]',
  `PARENT_NODE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부모노드ID',
  `SELF_ATTD_APPRV_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '자체근태승인여부',
  `MAIN_ADMIN_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부서 정 관리자',
  `SUB_ADMIN_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부서 부 관리자',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`NODE_CD`),
  KEY `IX_NODE_PARENT` (`CMPNY_CD`,`SITE_CD`,`PARENT_NODE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일일계정 슬롯(현재 점유 상태만 관리)';
```

</details>

## 사용자 / 계정

### tb_user

**사용자**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 USER_CD | varchar(20) | NO |  | 사용자코드 |
| USER_ID | varchar(50) | NO |  | 사용자ID |
| USER_NM | varchar(50) | NO |  | 사용자명 |
| USER_PW | varchar(100) | YES | NULL | 비밀번호(해시) |
| SITE_CD | varchar(50) | YES | NULL | 사업장코드 |
| NODE_CD | varchar(50) | YES | NULL | 소속부서 |
| AUTH_CD | varchar(10) | NO |  | 권한코드 |
| MBL_NO_ENC | text | YES |  | 휴대폰번호 AES-GCM (v1.base64url) |
| MBL_NO_HMAC | varchar(43) | YES | NULL | 휴대폰번호 HMAC-SHA256 Base64URL (equals/중복/계정찾기) |
| MBL_NO_LAST4 | char(4) | YES | NULL | 휴대폰번호 마지막4자리(마스킹/리스트용) |
| EMAIL_ENC | text | YES |  | 이메일 AES-GCM (v1.base64url) |
| EMAIL_HMAC | varchar(43) | YES | NULL | 이메일 HMAC-SHA256 Base64URL (equals/중복/계정찾기) |
| EMAIL_DOMAIN | varchar(100) | YES | NULL | 이메일 도메인(선택) |
| BIRTH_DT_ENC | text | YES |  | 생년월일 AES-GCM (v1.base64url) |
| HIRE_DATE | varchar(8) | YES | NULL | 입사일 (YYYYMMDD) — 연차 부여 기준 |
| EMPLOYMENT_TYPE | varchar(20) | YES | NULL | 고용형태[SYS041] REGULAR/CONTRACT/DAILY/EXECUTIVE |
| CONTRACT_END_DATE | varchar(8) | YES | NULL | 계약 종료일 (YYYYMMDD, EMPLOYMENT_TYPE=CONTRACT일 때 필수) |
| GENDER | varchar(6) | YES | NULL | 성별 |
| USE_YN | varchar(2) | YES | 'Y' | 사용여부 |
| ACCOUNT_STATUS | varchar(20) | NO | '01' | 계정상태[SYS013] |
| PWD_LOCK_YN | varchar(2) | NO | 'N' | 비밀번호잠금여부 |
| PWD_FAIL_CNT | int | NO | '0' | 비밀번호실패횟수 |
| PWD_LOCK_EXPIRE_DTIME | datetime | YES | NULL | 비밀번호 인증 실패 잠금 만료일시 |
| PWD_CHG_DTIME | datetime | YES | NULL | 비밀번호변경일시 |
| WITHDRAWAL_DATE | varchar(8) | YES | NULL | 회원탈퇴일 |
| LAST_LOGIN_DTIME | datetime | YES | NULL | 마지막로그인 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`USER_CD`)`
- **UNIQUE**: `UNIQUE KEY `UX_TB_USER_ID` (`CMPNY_CD`,`USER_ID`)`
- **UNIQUE**: `UNIQUE KEY `UX_TB_USER_MBL_NO` (`CMPNY_CD`,`MBL_NO_HMAC`)`
- **INDEX**: `KEY `IX_TB_USER_STATUS` (`CMPNY_CD`,`USE_YN`,`ACCOUNT_STATUS`)`
- **INDEX**: `KEY `IX_TB_USER_CONTRACT` (`CMPNY_CD`,`CONTRACT_END_DATE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `USER_ID` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자ID',
  `USER_NM` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자명',
  `USER_PW` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비밀번호(해시)',
  `SITE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장코드',
  `NODE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '소속부서',
  `AUTH_CD` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '권한코드',
  `MBL_NO_ENC` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '휴대폰번호 AES-GCM (v1.base64url)',
  `MBL_NO_HMAC` varchar(43) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '휴대폰번호 HMAC-SHA256 Base64URL (equals/중복/계정찾기)',
  `MBL_NO_LAST4` char(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '휴대폰번호 마지막4자리(마스킹/리스트용)',
  `EMAIL_ENC` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '이메일 AES-GCM (v1.base64url)',
  `EMAIL_HMAC` varchar(43) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '이메일 HMAC-SHA256 Base64URL (equals/중복/계정찾기)',
  `EMAIL_DOMAIN` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '이메일 도메인(선택)',
  `BIRTH_DT_ENC` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '생년월일 AES-GCM (v1.base64url)',
  `HIRE_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입사일 (YYYYMMDD) — 연차 부여 기준',
  `EMPLOYMENT_TYPE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '고용형태[SYS041] REGULAR/CONTRACT/DAILY/EXECUTIVE',
  `CONTRACT_END_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계약 종료일 (YYYYMMDD, EMPLOYMENT_TYPE=CONTRACT일 때 필수)',
  `GENDER` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '성별',
  `USE_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `ACCOUNT_STATUS` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '01' COMMENT '계정상태[SYS013]',
  `PWD_LOCK_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '비밀번호잠금여부',
  `PWD_FAIL_CNT` int NOT NULL DEFAULT '0' COMMENT '비밀번호실패횟수',
  `PWD_LOCK_EXPIRE_DTIME` datetime DEFAULT NULL COMMENT '비밀번호 인증 실패 잠금 만료일시',
  `PWD_CHG_DTIME` datetime DEFAULT NULL COMMENT '비밀번호변경일시',
  `WITHDRAWAL_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '회원탈퇴일',
  `LAST_LOGIN_DTIME` datetime DEFAULT NULL COMMENT '마지막로그인',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`USER_CD`),
  UNIQUE KEY `UX_TB_USER_ID` (`CMPNY_CD`,`USER_ID`),
  UNIQUE KEY `UX_TB_USER_MBL_NO` (`CMPNY_CD`,`MBL_NO_HMAC`),
  KEY `IX_TB_USER_STATUS` (`CMPNY_CD`,`USE_YN`,`ACCOUNT_STATUS`),
  KEY `IX_TB_USER_CONTRACT` (`CMPNY_CD`,`CONTRACT_END_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자';
```

</details>

### tb_del_user

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 USER_ID | varchar(50) | NO |  | 사용자ID |
| USER_NM | varchar(50) | NO |  | 사용자명 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일자 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`USER_ID`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_del_user` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `USER_ID` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자ID',
  `USER_NM` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자명',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일자',
  PRIMARY KEY (`CMPNY_CD`,`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_user_device

**정규직 디바이스 점유 (글로벌 유니크 - 회사 가로질러 1디바이스=1계정)**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 DEVICE_UUID | varchar(100) | NO |  | 디바이스UUID |
| USER_CD | varchar(20) | NO |  | 사용자코드 |
| DEVICE_TYPE | varchar(20) | NO |  | 디바이스종류(IOS/ANDROID) |
| DEVICE_MODEL | varchar(50) | YES | NULL | 디바이스모델 |
| OS_VERSION | varchar(20) | YES | NULL | OS버전 |
| APP_VERSION | varchar(20) | YES | NULL | 앱버전 |
| PUSH_TOKEN | varchar(500) | YES | NULL | FCM/APNS 푸시토큰 |
| LAST_LOGIN_DTIME | datetime | YES | NULL | 최종로그인일시 |
| LAST_LOGIN_IP | varchar(45) | YES | NULL | 최종로그인IP |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`DEVICE_UUID`)`
- **INDEX**: `KEY `idx_user_device_user` (`USER_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_device` (
  `DEVICE_UUID` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '디바이스UUID',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `DEVICE_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '디바이스종류(IOS/ANDROID)',
  `DEVICE_MODEL` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '디바이스모델',
  `OS_VERSION` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'OS버전',
  `APP_VERSION` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '앱버전',
  `PUSH_TOKEN` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'FCM/APNS 푸시토큰',
  `LAST_LOGIN_DTIME` datetime DEFAULT NULL COMMENT '최종로그인일시',
  `LAST_LOGIN_IP` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '최종로그인IP',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`DEVICE_UUID`),
  KEY `idx_user_device_user` (`USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='정규직 디바이스 점유 (글로벌 유니크 - 회사 가로질러 1디바이스=1계정)';
```

</details>

### tb_user_site_auth

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 USER_CD | varchar(20) | NO |  | 사용자CD |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| USE_YN | varchar(2) | YES | 'Y' | 사용여부 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`USER_CD`,`SITE_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_site_auth` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자CD',
  `SITE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USE_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`USER_CD`,`SITE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_user_service_credit

**사용자 경력 인정 (점진 부여 전용)**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CREDIT_ID | varchar(20) | NO |  | 경력 인정 ID (PK) |
| CMPNY_CD | varchar(50) | NO |  | 회사 코드 |
| USER_CD | varchar(20) | NO |  | 사용자 코드 |
| CREDIT_MONTHS | int | NO |  | 인정 개월 수 (0 이상) |
| REASON_TYPE | varchar(30) | NO |  | 사유 유형[SYS042] |
| REASON_DETAIL | varchar(500) | YES | NULL | 상세 설명 |
| USE_YN | char(1) | NO | 'Y' | 사용 여부 |
| INSERT_NO | varchar(50) | NO |  | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CREDIT_ID`)`
- **INDEX**: `KEY `IX_TB_USER_SERVICE_CREDIT_USER` (`CMPNY_CD`,`USER_CD`,`USE_YN`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_service_credit` (
  `CREDIT_ID` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '경력 인정 ID (PK)',
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 코드',
  `CREDIT_MONTHS` int NOT NULL COMMENT '인정 개월 수 (0 이상)',
  `REASON_TYPE` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사유 유형[SYS042]',
  `REASON_DETAIL` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '상세 설명',
  `USE_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CREDIT_ID`),
  KEY `IX_TB_USER_SERVICE_CREDIT_USER` (`CMPNY_CD`,`USER_CD`,`USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 경력 인정 (점진 부여 전용)';
```

</details>

### tb_user_hire_date_history

**입사일 변경 이력 (노무 감사용)**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 HIST_ID | varchar(20) | NO |  | 이력 ID (PK) |
| CMPNY_CD | varchar(50) | NO |  | 회사 코드 |
| USER_CD | varchar(20) | NO |  | 사용자 코드 |
| PREV_HIRE_DATE | varchar(8) | NO |  | 변경 전 입사일 |
| NEW_HIRE_DATE | varchar(8) | NO |  | 변경 후 입사일 |
| CHANGE_REASON | varchar(1000) | NO |  | 변경 사유 (자유 텍스트, 필수) |
| HANDLING_TYPE | varchar(30) | NO |  | 처리 방식[SYS039] KEEP_AND_BACKFILL/KEEP_AND_APPLY_NEW/RESET_ALL |
| AFFECTED_GRANT_SNAPSHOT | json | YES | NULL | 영향받은 부여 이력 스냅샷 |
| INSERT_NO | varchar(50) | NO |  | 변경자 (AUTH_MASTER 또는 AUTH_HR_MANAGER) |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 변경일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`HIST_ID`)`
- **INDEX**: `KEY `IX_TB_HIRE_HIST_USER` (`CMPNY_CD`,`USER_CD`,`INSERT_DATE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_hire_date_history` (
  `HIST_ID` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력 ID (PK)',
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 코드',
  `PREV_HIRE_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 전 입사일',
  `NEW_HIRE_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 후 입사일',
  `CHANGE_REASON` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 사유 (자유 텍스트, 필수)',
  `HANDLING_TYPE` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '처리 방식[SYS039] KEEP_AND_BACKFILL/KEEP_AND_APPLY_NEW/RESET_ALL',
  `AFFECTED_GRANT_SNAPSHOT` json DEFAULT NULL COMMENT '영향받은 부여 이력 스냅샷',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경자 (AUTH_MASTER 또는 AUTH_HR_MANAGER)',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경일시',
  PRIMARY KEY (`HIST_ID`),
  KEY `IX_TB_HIRE_HIST_USER` (`CMPNY_CD`,`USER_CD`,`INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='입사일 변경 이력 (노무 감사용)';
```

</details>

## 인증 / 보안

### tb_auth_token

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 USER_CD | varchar(50) | NO |  | 사용자코드 |
| 🔑 TOKEN_ID | varchar(50) | NO |  | 토큰(세션) 식별자 |
| CLIENT_TYPE | varchar(10) | NO |  | WEB/APP |
| DEVICE_ID | varchar(100) | YES | NULL | 앱 디바이스 식별(가능하면) |
| REFRESH_TOKEN_HASH | varchar(128) | NO |  | 리프레시 토큰 해시 |
| ISSUED_DTIME | datetime | NO | CURRENT_TIMESTAMP |  |
| EXPIRE_DTIME | datetime | NO |  | 리프레시 토큰 만료 |
| REVOKED_YN | varchar(2) | NO | 'N' |  |
| REVOKED_DTIME | datetime | YES | NULL |  |
| IP_ADDR | varchar(45) | YES | NULL |  |
| USER_AGENT | varchar(255) | YES | NULL |  |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' |  |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP |  |
| UPDATE_NO | varchar(50) | YES | NULL |  |
| UPDATE_DATE | datetime | YES | NULL |  |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`USER_CD`,`TOKEN_ID`)`
- **UNIQUE**: `UNIQUE KEY `UX_AUTH_TOKEN_RTH` (`REFRESH_TOKEN_HASH`)`
- **INDEX**: `KEY `IX_TOKEN_USER` (`CMPNY_CD`,`USER_CD`,`CLIENT_TYPE`,`REVOKED_YN`)`
- **INDEX**: `KEY `IX_TOKEN_EXPIRE` (`EXPIRE_DTIME`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_auth_token` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `USER_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `TOKEN_ID` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '토큰(세션) 식별자',
  `CLIENT_TYPE` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'WEB/APP',
  `DEVICE_ID` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '앱 디바이스 식별(가능하면)',
  `REFRESH_TOKEN_HASH` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '리프레시 토큰 해시',
  `ISSUED_DTIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `EXPIRE_DTIME` datetime NOT NULL COMMENT '리프레시 토큰 만료',
  `REVOKED_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `REVOKED_DTIME` datetime DEFAULT NULL,
  `IP_ADDR` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `USER_AGENT` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` datetime DEFAULT NULL,
  PRIMARY KEY (`CMPNY_CD`,`USER_CD`,`TOKEN_ID`),
  UNIQUE KEY `UX_AUTH_TOKEN_RTH` (`REFRESH_TOKEN_HASH`),
  KEY `IX_TOKEN_USER` (`CMPNY_CD`,`USER_CD`,`CLIENT_TYPE`,`REVOKED_YN`),
  KEY `IX_TOKEN_EXPIRE` (`EXPIRE_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_sms_auth_code

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 SMS_ID | bigint | NO | AUTO_INCREMENT |  |
| MBL_NO_ENC | varchar(200) | NO |  |  |
| MBL_NO_HMAC | char(43) | NO |  |  |
| AUTH_CD | varchar(6) | NO |  |  |
| EXPIRED_AT | datetime | NO |  |  |
| VERIFIED_YN | varchar(2) | YES | 'N' |  |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' |  |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP |  |
| UPDATE_NO | varchar(50) | YES | NULL |  |
| UPDATE_DATE | datetime | YES | NULL |  |

키/인덱스:

- **PK**: `PRIMARY KEY (`SMS_ID`)`
- **INDEX**: `KEY `idx_sms_auth_mbl_hmac_exp` (`MBL_NO_HMAC`,`EXPIRED_AT`)`
- **INDEX**: `KEY `idx_sms_auth_mbl_hmac_ins` (`MBL_NO_HMAC`,`INSERT_DATE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_sms_auth_code` (
  `SMS_ID` bigint NOT NULL AUTO_INCREMENT,
  `MBL_NO_ENC` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `MBL_NO_HMAC` char(43) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `AUTH_CD` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `EXPIRED_AT` datetime NOT NULL,
  `VERIFIED_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'N',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` datetime DEFAULT NULL,
  PRIMARY KEY (`SMS_ID`),
  KEY `idx_sms_auth_mbl_hmac_exp` (`MBL_NO_HMAC`,`EXPIRED_AT`),
  KEY `idx_sms_auth_mbl_hmac_ins` (`MBL_NO_HMAC`,`INSERT_DATE`)
) ENGINE=InnoDB AUTO_INCREMENT=216 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_terms

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 TERMS_ID | varchar(3) | NO |  | 약관ID(SYS008) |
| TERMS_VERSION | varchar(10) | NO |  | 약관버전 |
| REQUIRED_YN | varchar(10) | NO |  | 필수여부 |
| TERMS_CONTENT | longtext | NO |  | 약관본문 |
| STR_DATE | varchar(8) | NO |  | 시행일자 |
| USE_YN | varchar(2) | YES | 'Y' | 사용유무 |
| TERMS_DESC | varchar(500) | YES | NULL | 비고 |
| INSERT_NO | varchar(50) | YES | NULL | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`TERMS_ID`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_terms` (
  `TERMS_ID` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관ID(SYS008)',
  `TERMS_VERSION` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관버전',
  `REQUIRED_YN` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '필수여부',
  `TERMS_CONTENT` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관본문',
  `STR_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시행일자',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용유무',
  `TERMS_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`TERMS_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_terms_id_version

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 TERMS_ID | varchar(3) | NO |  | 약관ID(SYS008) |
| 🔑 TERMS_VERSION | varchar(10) | NO |  | 약관버전 |
| REQUIRED_YN | varchar(10) | NO |  | 필수여부 |
| TERMS_CONTENT | longtext | NO |  | 약관본문 |
| STR_DATE | varchar(8) | NO |  | 시행일자 |
| TERMS_DESC | varchar(500) | YES | NULL | 비고 |
| INSERT_NO | varchar(50) | YES | NULL | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`TERMS_ID`,`TERMS_VERSION`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_terms_id_version` (
  `TERMS_ID` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관ID(SYS008)',
  `TERMS_VERSION` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관버전',
  `REQUIRED_YN` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '필수여부',
  `TERMS_CONTENT` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관본문',
  `STR_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시행일자',
  `TERMS_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`TERMS_ID`,`TERMS_VERSION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_terms_user_agr_mgmt

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 USER_CD | varchar(20) | NO |  | 사용자코드 |
| 🔑 TERMS_ID | varchar(3) | NO |  | 약관ID(SYS008) |
| 🔑 TERMS_VERSION | varchar(10) | NO |  | 약관버전 |
| AGR_YN | varchar(2) | YES | 'Y' | 동의여부 |
| INSERT_NO | varchar(50) | YES | NULL | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`USER_CD`,`TERMS_ID`,`TERMS_VERSION`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_terms_user_agr_mgmt` (
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `TERMS_ID` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관ID(SYS008)',
  `TERMS_VERSION` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관버전',
  `AGR_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '동의여부',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`USER_CD`,`TERMS_ID`,`TERMS_VERSION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

## 근태 - 출퇴근

### tb_user_attd_mgmt

**근태관리**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 ATTD_ID | varchar(20) | NO |  | 근태고유ID |
| CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| SITE_CD | varchar(50) | NO |  | 사업장코드 |
| USER_CD | varchar(20) | NO |  | 사용자코드 |
| WORK_YMD | varchar(8) | NO |  | 근무일 |
| NODE_CD | varchar(50) | YES | NULL | 소속부서 |
| WORK_SEQ | int | NO |  | 근무차수 |
| CHECK_IN_DATE | varchar(8) | NO |  | 출근일자 |
| CHECK_IN_TIME | varchar(4) | NO |  | 출근시간 |
| CHECK_IN_METHOD | varchar(2) | NO |  | 출근방법[SYS031] |
| CHECK_OUT_DATE | varchar(8) | YES | NULL | 퇴근일자 |
| CHECK_OUT_TIME | varchar(4) | YES | NULL | 퇴근시간 |
| CHECK_OUT_METHOD | varchar(2) | YES | NULL | 퇴근방법[SYS031] |
| DEL_YN | varchar(1) | NO |  | 삭제여부 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`ATTD_ID`)`
- **INDEX**: `KEY `IDX_ATTD_USER_DATE` (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`WORK_YMD`,`DEL_YN`)`
- **INDEX**: `KEY `IDX_ATTD_SITE_DATE` (`CMPNY_CD`,`SITE_CD`,`WORK_YMD`,`NODE_CD`,`DEL_YN`)`
- **INDEX**: `KEY `IDX_ATTD_NODE_DATGE` (`CMPNY_CD`,`NODE_CD`,`WORK_YMD`,`DEL_YN`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_attd_mgmt` (
  `ATTD_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근태고유ID',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `WORK_YMD` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근무일',
  `NODE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '소속부서',
  `WORK_SEQ` int NOT NULL COMMENT '근무차수',
  `CHECK_IN_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '출근일자',
  `CHECK_IN_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '출근시간',
  `CHECK_IN_METHOD` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '출근방법[SYS031]',
  `CHECK_OUT_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '퇴근일자',
  `CHECK_OUT_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '퇴근시간',
  `CHECK_OUT_METHOD` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '퇴근방법[SYS031]',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '삭제여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`ATTD_ID`),
  KEY `IDX_ATTD_USER_DATE` (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`WORK_YMD`,`DEL_YN`),
  KEY `IDX_ATTD_SITE_DATE` (`CMPNY_CD`,`SITE_CD`,`WORK_YMD`,`NODE_CD`,`DEL_YN`),
  KEY `IDX_ATTD_NODE_DATGE` (`CMPNY_CD`,`NODE_CD`,`WORK_YMD`,`DEL_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태관리';
```

</details>

### tb_user_attd_hist

**근태 처리 이력**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 HIST_ID | varchar(20) | NO |  | 이력고유ID |
| CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| ATTD_ID | varchar(20) | NO |  | 근태고유ID |
| SITE_CD | varchar(50) | NO |  | 사업장코드 |
| HIST_TYPE | varchar(10) | NO |  | 이력구분[SYS032] |
| PROCESS_REASON | varchar(500) | YES | NULL | 처리사유 |
| WORK_YMD | varchar(8) | YES | NULL | 근무일 |
| BEF_CHECK_IN_DATE | varchar(8) | YES | NULL | 변경전 출근일자 |
| BEF_CHECK_IN_TIME | varchar(4) | YES | NULL | 변경전 출근시간 |
| BEF_CHECK_OUT_DATE | varchar(8) | YES | NULL | 변경전 퇴근일자 |
| BEF_CHECK_OUT_TIME | varchar(4) | YES | NULL | 변경전 퇴근시간 |
| AFT_CHECK_IN_DATE | varchar(8) | YES | NULL | 변경후 출근일자 |
| AFT_CHECK_IN_TIME | varchar(4) | YES | NULL | 변경후 출근시간 |
| AFT_CHECK_OUT_DATE | varchar(8) | YES | NULL | 변경후 퇴근일자 |
| AFT_CHECK_OUT_TIME | varchar(4) | YES | NULL | 변경후 퇴근시간 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`HIST_ID`)`
- **INDEX**: `KEY `IDX_ATTD_HIST_ATTD` (`CMPNY_CD`,`ATTD_ID`)`
- **INDEX**: `KEY `IDX_ATTD_HIST_SITE` (`CMPNY_CD`,`SITE_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_attd_hist` (
  `HIST_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력고유ID',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `ATTD_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근태고유ID',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `HIST_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력구분[SYS032]',
  `PROCESS_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리사유',
  `WORK_YMD` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무일',
  `BEF_CHECK_IN_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경전 출근일자',
  `BEF_CHECK_IN_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경전 출근시간',
  `BEF_CHECK_OUT_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경전 퇴근일자',
  `BEF_CHECK_OUT_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경전 퇴근시간',
  `AFT_CHECK_IN_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경후 출근일자',
  `AFT_CHECK_IN_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경후 출근시간',
  `AFT_CHECK_OUT_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경후 퇴근일자',
  `AFT_CHECK_OUT_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경후 퇴근시간',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`HIST_ID`),
  KEY `IDX_ATTD_HIST_ATTD` (`CMPNY_CD`,`ATTD_ID`),
  KEY `IDX_ATTD_HIST_SITE` (`CMPNY_CD`,`SITE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태 처리 이력';
```

</details>

### tb_user_attd_gps

**근태 GPS 기록**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 GPS_ID | varchar(20) | NO |  | GPS고유ID |
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| ATTD_ID | varchar(20) | NO |  | 근태고유ID |
| SITE_CD | varchar(50) | NO |  | 사업장코드 |
| USER_CD | varchar(20) | NO |  | 사용자코드 |
| GPS_INFO_TYPE | varchar(2) | YES | NULL | GPS정보타입[SYS028] |
| LAT | decimal(10,7) | NO |  | 위도 |
| LON | decimal(10,7) | NO |  | 경도 |
| ACCURACY | decimal(7,2) | YES | NULL | 정확도(m, 임계값 검증용) |
| API_CALL_DATE | varchar(8) | NO |  | 측정일자(YYYYMMDD) |
| API_CALL_TIME | varchar(6) | NO |  | 측정시간(HHmmss) |
| IS_MOCKED | char(1) | NO | 'N' | Mock위치여부(Y/N) |
| IP_ADDR | varchar(45) | YES | NULL | IP주소(IPv6 대응) |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`GPS_ID`)`
- **INDEX**: `KEY `idx_gps_attd` (`CMPNY_CD`,`ATTD_ID`)`
- **INDEX**: `KEY `idx_gps_user` (`CMPNY_CD`,`USER_CD`,`API_CALL_DATE`)`
- **INDEX**: `KEY `idx_gps_search` (`CMPNY_CD`,`SITE_CD`,`API_CALL_DATE`)`
- **INDEX**: `KEY `idx_gps_abnormal` (`CMPNY_CD`,`API_CALL_DATE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_attd_gps` (
  `GPS_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'GPS고유ID',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `ATTD_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근태고유ID',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `GPS_INFO_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'GPS정보타입[SYS028]',
  `LAT` decimal(10,7) NOT NULL COMMENT '위도',
  `LON` decimal(10,7) NOT NULL COMMENT '경도',
  `ACCURACY` decimal(7,2) DEFAULT NULL COMMENT '정확도(m, 임계값 검증용)',
  `API_CALL_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '측정일자(YYYYMMDD)',
  `API_CALL_TIME` varchar(6) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '측정시간(HHmmss)',
  `IS_MOCKED` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT 'Mock위치여부(Y/N)',
  `IP_ADDR` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP주소(IPv6 대응)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`,`GPS_ID`),
  KEY `idx_gps_attd` (`CMPNY_CD`,`ATTD_ID`),
  KEY `idx_gps_user` (`CMPNY_CD`,`USER_CD`,`API_CALL_DATE`),
  KEY `idx_gps_search` (`CMPNY_CD`,`SITE_CD`,`API_CALL_DATE`),
  KEY `idx_gps_abnormal` (`CMPNY_CD`,`API_CALL_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태 GPS 기록';
```

</details>

### tb_user_attd_req

**사용자 근태 관련 요청 관리**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 REQ_ID | varchar(20) | NO |  | 요청 ID (PK) |
| CMPNY_CD | varchar(50) | NO |  | 회사 코드 |
| SITE_CD | varchar(50) | NO |  | 사업장 코드 |
| USER_CD | varchar(20) | NO |  | 요청자 사용자 코드 |
| REQ_TYPE | varchar(20) | NO |  | 요청 유형 (SYS032: 01근태생성/02근태수정/03초과근무생성/04초과근무수정/05연차사용/06연차수정) |
| TARGET_ID | varchar(20) | YES | NULL | 수정 대상 ID (수정 요청 시: ATTD_ID / OT_ID / LEAVE_ID, 생성 요청 시 NULL) |
| REQ_STATUS | varchar(10) | NO |  | 요청 상태 (SYS033: 01신청/02승인/03반려/04취소) |
| REQ_REASON | varchar(500) | YES | NULL | 요청 사유 |
| WORK_YMD | varchar(8) | YES | NULL | 근무 일자 (YYYYMMDD) |
| NODE_CD | varchar(50) | YES | NULL | 근무 노드 코드 |
| WORK_SEQ | int | YES | NULL | 근무 순번 |
| START_DATE | varchar(8) | YES | NULL | 시작 일자 (YYYYMMDD) |
| START_TIME | varchar(4) | YES | NULL | 시작 시각 (HHMM) |
| END_DATE | varchar(8) | YES | NULL | 종료 일자 (YYYYMMDD) |
| END_TIME | varchar(4) | YES | NULL | 종료 시각 (HHMM) |
| OT_TYPE | varchar(10) | YES | NULL | 초과근무 유형 (EXTEND:연장 / NIGHT:야간 / HOLIDAY:휴일) |
| LEAVE_TYPE | varchar(10) | YES | NULL | 연차 유형 (ANNUAL:연차 / HALF_AM:오전반차 / HALF_PM:오후반차 / SICK:병가 / FAMILY:경조사 등) |
| LEAVE_DAYS | decimal(3,1) | YES | NULL | 사용 일수 (0.5, 1.0, 2.0...) |
| PROCESS_USER_CD | varchar(20) | YES | NULL | 처리자 사용자 코드 |
| PROCESS_COMMENT | varchar(500) | YES | NULL | 처리 코멘트 |
| PROCESS_DATE | datetime | YES | NULL | 처리 일시 |
| DEL_YN | varchar(1) | NO | 'N' | 삭제 여부 |
| INSERT_NO | varchar(50) | NO |  | 등록자 |
| INSERT_DATE | datetime | NO |  | 등록 일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정 일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`REQ_ID`)`
- **INDEX**: `KEY `IDX_ATTD_REQ_USER` (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`REQ_STATUS`)`
- **INDEX**: `KEY `IDX_ATTD_REQ_STATUS` (`CMPNY_CD`,`SITE_CD`,`REQ_STATUS`,`REQ_TYPE`)`
- **INDEX**: `KEY `IDX_ATTD_REQ_WORK_YMD` (`CMPNY_CD`,`SITE_CD`,`WORK_YMD`)`
- **INDEX**: `KEY `IDX_ATTD_REQ_TARGET` (`TARGET_ID`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_attd_req` (
  `REQ_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청 ID (PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청자 사용자 코드',
  `REQ_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청 유형 (SYS032: 01근태생성/02근태수정/03초과근무생성/04초과근무수정/05연차사용/06연차수정)',
  `TARGET_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정 대상 ID (수정 요청 시: ATTD_ID / OT_ID / LEAVE_ID, 생성 요청 시 NULL)',
  `REQ_STATUS` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청 상태 (SYS033: 01신청/02승인/03반려/04취소)',
  `REQ_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '요청 사유',
  `WORK_YMD` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무 일자 (YYYYMMDD)',
  `NODE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무 노드 코드',
  `WORK_SEQ` int DEFAULT NULL COMMENT '근무 순번',
  `START_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시작 일자 (YYYYMMDD)',
  `START_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시작 시각 (HHMM)',
  `END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 일자 (YYYYMMDD)',
  `END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 시각 (HHMM)',
  `OT_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '초과근무 유형 (EXTEND:연장 / NIGHT:야간 / HOLIDAY:휴일)',
  `LEAVE_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 유형 (ANNUAL:연차 / HALF_AM:오전반차 / HALF_PM:오후반차 / SICK:병가 / FAMILY:경조사 등)',
  `LEAVE_DAYS` decimal(3,1) DEFAULT NULL COMMENT '사용 일수 (0.5, 1.0, 2.0...)',
  `PROCESS_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리자 사용자 코드',
  `PROCESS_COMMENT` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리 코멘트',
  `PROCESS_DATE` datetime DEFAULT NULL COMMENT '처리 일시',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`REQ_ID`),
  KEY `IDX_ATTD_REQ_USER` (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`REQ_STATUS`),
  KEY `IDX_ATTD_REQ_STATUS` (`CMPNY_CD`,`SITE_CD`,`REQ_STATUS`,`REQ_TYPE`),
  KEY `IDX_ATTD_REQ_WORK_YMD` (`CMPNY_CD`,`SITE_CD`,`WORK_YMD`),
  KEY `IDX_ATTD_REQ_TARGET` (`TARGET_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 근태 관련 요청 관리';
```

</details>

### tb_attd_std_time_rule

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 STD_TIME_RULE_TYPE | varchar(2) | NO |  | 시간 표준화 적용 타입[SYS028] |
| STD_TIME_TYPE | varchar(2) | NO |  | 시간 표준화 타입[SYS029] |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`STD_TIME_RULE_TYPE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_attd_std_time_rule` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `STD_TIME_RULE_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시간 표준화 적용 타입[SYS028]',
  `STD_TIME_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시간 표준화 타입[SYS029]',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`STD_TIME_RULE_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_attd_std_time_rule_his

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 HIST_IDX | varchar(20) | NO |  | 이력IDX |
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| STD_TIME_RULE_TYPE | varchar(2) | NO |  | 시간 표준화 적용 타입[SYS028] |
| STD_TIME_TYPE | varchar(2) | NO |  | 시간 표준화 타입[SYS029] |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`HIST_IDX`,`CMPNY_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_attd_std_time_rule_his` (
  `HIST_IDX` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력IDX',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `STD_TIME_RULE_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시간 표준화 적용 타입[SYS028]',
  `STD_TIME_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시간 표준화 타입[SYS029]',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`HIST_IDX`,`CMPNY_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

## 근태 - 연차 정책/타입

### tb_leave_policy

**회사 법정 연차 부여 정책 (7개 axis)**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 POLICY_SEQ | bigint | NO | AUTO_INCREMENT | 정책 일련번호 (PK) |
| CMPNY_CD | varchar(50) | NO |  | 회사 코드 |
| POLICY_PRESET | varchar(30) | NO |  | 프리셋: HIRE_DATE/FISCAL_PRORATE/FISCAL_MONTHLY/HIRE_DATE_PREGRANT/CUSTOM |
| AXIS1_GRANT_BASE | varchar(20) | NO |  | 1번: HIRE_DATE/FISCAL_YEAR [SYS036] |
| AXIS2_FISCAL_START_MM | char(2) | YES | NULL | 2번: 회계연도 시작월 (01~12) |
| AXIS2_FISCAL_START_DD | char(2) | YES | NULL | 2번: 회계연도 시작일 (01~31) |
| AXIS3_FIRST_YEAR_METHOD | varchar(30) | NO |  | 3번: MONTHLY_ONLY/PRORATE/NEXT_YEAR_BULK [SYS037] |
| AXIS3_PREGRANT_YN | char(1) | NO | 'N' | 3번 보조: 입사일 일괄선부여 여부 (프리셋 4번 표현) |
| AXIS4_PRORATE_ROUNDING | varchar(20) | NO | 'CEIL' | 4번: CEIL/ROUND/FLOOR/HALF_DAY [SYS038] (AXIS3=PRORATE 시만 유효, 그 외는 CEIL 강제) |
| AXIS5_TENURE_MODE | varchar(10) | NO | 'LEGAL' | 5번: LEGAL/CUSTOM |
| AXIS5_START_YEAR | int | NO | '3' | 5번: 가산 시작 연차 (1~3, LEGAL 시 3 강제) |
| AXIS5_INTERVAL | int | NO | '2' | 5번: 가산 주기 (1~2, LEGAL 시 2 강제) |
| AXIS5_MAX_DAYS | int | NO | '25' | 5번: 최대 연차일수 (25 이상, 법정) |
| AXIS6_VALIDITY_MONTHS | int | NO | '12' | 6번: 유효기간(개월) 12 또는 24 |
| AXIS7_USE_PROMOTION | char(1) | NO | 'N' | 7번: 사용촉진 Y/N |
| USE_YN | char(1) | NO | 'Y' | 활성 여부 (회사당 Y는 1건, 서비스 레이어에서 보장) |
| APPLY_FROM_DATE | varchar(8) | NO |  | 정책 적용 시작일 (YYYYMMDD) |
| INSERT_NO | varchar(50) | NO |  |  |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP |  |
| UPDATE_NO | varchar(50) | YES | NULL |  |
| UPDATE_DATE | datetime | YES | NULL |  |

키/인덱스:

- **PK**: `PRIMARY KEY (`POLICY_SEQ`)`
- **UNIQUE**: `UNIQUE KEY `UX_TB_LEAVE_POLICY_ACTIVE` (((case when (`USE_YN` = _utf8mb4'Y') then `CMPNY_CD` end)))`
- **INDEX**: `KEY `IX_TB_LEAVE_POLICY_ACTIVE` (`CMPNY_CD`,`USE_YN`,`APPLY_FROM_DATE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_leave_policy` (
  `POLICY_SEQ` bigint NOT NULL AUTO_INCREMENT COMMENT '정책 일련번호 (PK)',
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `POLICY_PRESET` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '프리셋: HIRE_DATE/FISCAL_PRORATE/FISCAL_MONTHLY/HIRE_DATE_PREGRANT/CUSTOM',
  `AXIS1_GRANT_BASE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '1번: HIRE_DATE/FISCAL_YEAR [SYS036]',
  `AXIS2_FISCAL_START_MM` char(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2번: 회계연도 시작월 (01~12)',
  `AXIS2_FISCAL_START_DD` char(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2번: 회계연도 시작일 (01~31)',
  `AXIS3_FIRST_YEAR_METHOD` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '3번: MONTHLY_ONLY/PRORATE/NEXT_YEAR_BULK [SYS037]',
  `AXIS3_PREGRANT_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '3번 보조: 입사일 일괄선부여 여부 (프리셋 4번 표현)',
  `AXIS4_PRORATE_ROUNDING` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CEIL' COMMENT '4번: CEIL/ROUND/FLOOR/HALF_DAY [SYS038] (AXIS3=PRORATE 시만 유효, 그 외는 CEIL 강제)',
  `AXIS5_TENURE_MODE` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LEGAL' COMMENT '5번: LEGAL/CUSTOM',
  `AXIS5_START_YEAR` int NOT NULL DEFAULT '3' COMMENT '5번: 가산 시작 연차 (1~3, LEGAL 시 3 강제)',
  `AXIS5_INTERVAL` int NOT NULL DEFAULT '2' COMMENT '5번: 가산 주기 (1~2, LEGAL 시 2 강제)',
  `AXIS5_MAX_DAYS` int NOT NULL DEFAULT '25' COMMENT '5번: 최대 연차일수 (25 이상, 법정)',
  `AXIS6_VALIDITY_MONTHS` int NOT NULL DEFAULT '12' COMMENT '6번: 유효기간(개월) 12 또는 24',
  `AXIS7_USE_PROMOTION` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '7번: 사용촉진 Y/N',
  `USE_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '활성 여부 (회사당 Y는 1건, 서비스 레이어에서 보장)',
  `APPLY_FROM_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '정책 적용 시작일 (YYYYMMDD)',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` datetime DEFAULT NULL,
  PRIMARY KEY (`POLICY_SEQ`),
  UNIQUE KEY `UX_TB_LEAVE_POLICY_ACTIVE` (((case when (`USE_YN` = _utf8mb4'Y') then `CMPNY_CD` end))),
  KEY `IX_TB_LEAVE_POLICY_ACTIVE` (`CMPNY_CD`,`USE_YN`,`APPLY_FROM_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회사 법정 연차 부여 정책 (7개 axis)';
```

</details>

### tb_leave_policy_history

**연차 정책 변경 이력**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 HIST_ID | varchar(20) | NO |  | 이력 ID (PK) |
| CMPNY_CD | varchar(50) | NO |  | 회사 코드 |
| POLICY_SEQ | bigint | NO |  | 변경된 TB_LEAVE_POLICY.POLICY_SEQ |
| CHANGE_TYPE | varchar(20) | NO |  | CREATE/UPDATE/PRESET_CHANGE |
| PREV_SNAPSHOT | json | YES | NULL | 변경 전 정책 전체 스냅샷 |
| NEW_SNAPSHOT | json | NO |  | 변경 후 정책 전체 스냅샷 |
| CHANGE_REASON | varchar(500) | YES | NULL |  |
| IMPACT_SUMMARY | json | YES | NULL | 영향 분석 결과 (영향 인원, 추가 부담) |
| INSERT_NO | varchar(50) | NO |  |  |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP |  |

키/인덱스:

- **PK**: `PRIMARY KEY (`HIST_ID`)`
- **INDEX**: `KEY `IX_TB_LEAVE_POLICY_HIST` (`CMPNY_CD`,`INSERT_DATE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_leave_policy_history` (
  `HIST_ID` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력 ID (PK)',
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `POLICY_SEQ` bigint NOT NULL COMMENT '변경된 TB_LEAVE_POLICY.POLICY_SEQ',
  `CHANGE_TYPE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'CREATE/UPDATE/PRESET_CHANGE',
  `PREV_SNAPSHOT` json DEFAULT NULL COMMENT '변경 전 정책 전체 스냅샷',
  `NEW_SNAPSHOT` json NOT NULL COMMENT '변경 후 정책 전체 스냅샷',
  `CHANGE_REASON` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IMPACT_SUMMARY` json DEFAULT NULL COMMENT '영향 분석 결과 (영향 인원, 추가 부담)',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`HIST_ID`),
  KEY `IX_TB_LEAVE_POLICY_HIST` (`CMPNY_CD`,`INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 정책 변경 이력';
```

</details>

### tb_leave_type_mgmt

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 LEAVE_CD | varchar(20) | NO |  | 연차코드 |
| LEAVE_NO | varchar(20) | NO |  | 연차번호 |
| LEAVE_NM | varchar(200) | NO |  | 연차명 |
| LEAVE_TYPE | char(2) | NO |  | 연차타입[SYS021] |
| GRANT_TYPE | char(2) | YES | NULL | 부여방식[SYS022] |
| PAID_TYPE | char(2) | NO |  | 유급구분[SYS023] |
| LEAVE_NATURE_TYPE | varchar(2) | NO |  | 휴가성격[SYS024] |
| USE_YN | char(1) | YES | 'Y' | 사용여부 |
| SYSTEM_YN | char(1) | NO | 'N' | 시스템 시드 여부 (Y: PRAFTA-018 법정 연차용, 화면 편집 불가) |
| LEAVE_DESC | varchar(500) | YES | NULL | 비고 |
| MAX_APLY_DAYS | tinyint unsigned | YES | NULL | 최대 신청일수 |
| USE_UNIT_TYPE | varchar(2) | YES | NULL | 연차 사용 단위[SYS025] |
| AVAIL_TERM_TYPE | varchar(2) | YES | NULL | 연차 사용가능기간 타입[SYS026] |
| AVAIL_FROM_DT | varchar(4) | YES | NULL | 연차 사용기간 FROM |
| AVAIL_TO_DT | varchar(4) | YES | NULL | 연차 사용기간 TO |
| GRANT_DAYS | tinyint unsigned | YES | NULL | 부여일 수 |
| ADMIN_AVAIL_TERM_TYPE | varchar(2) | YES | NULL | 관리자 부여 연차 사용가능기간 타입[SYS026] |
| ADMIN_AVAIL_FROM_DT | varchar(6) | YES | NULL | 관리자 부여 연차 사용기간 FROM |
| ADMIN_AVAIL_TO_DT | varchar(6) | YES | NULL | 관리자 부여 연차 사용기간 TO |
| GRANT_BASE_TYPE | varchar(2) | YES | NULL | 자동 부여 기준일[SYS027] |
| GRANT_OFFSET_MONTH | tinyint unsigned | YES | NULL | 자동부여 실행시점 |
| GRANT_ASSIGN_MMDD | char(4) | YES | NULL | 자동부여 지정일 MMDD (기준일=03 부여일지정 시 필수) |
| APRV_USE_YN | char(1) | YES | 'N' | 결재여부 |
| APRV_STEP_CNT | tinyint unsigned | YES | NULL | 결재 단계 수 |
| HR_FINAL_APRV_YN | char(1) | YES | 'N' | 인사팀 최종 승인 여부 |
| EVIDENCE_YN | char(1) | YES | 'N' | 증빙여부 |
| EVIDENCE_GUIDE_MSG | varchar(500) | YES | NULL | 증빙안내문구 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`LEAVE_CD`)`
- **INDEX**: `KEY `IX_TB_LEAVE_TYPE_MGMT_01` (`CMPNY_CD`,`LEAVE_NO`,`USE_YN`)`
- **INDEX**: `KEY `IX_TB_LEAVE_TYPE_MGMT_02` (`CMPNY_CD`,`LEAVE_NO`,`USE_YN`,`LEAVE_TYPE`)`
- **INDEX**: `KEY `IX_TB_LEAVE_TYPE_MGMT_03` (`CMPNY_CD`,`LEAVE_NO`,`USE_YN`,`LEAVE_TYPE`,`GRANT_TYPE`)`
- **INDEX**: `KEY `IX_TB_LEAVE_TYPE_MGMT_SYSTEM` (`CMPNY_CD`,`SYSTEM_YN`,`USE_YN`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_leave_type_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `LEAVE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차코드',
  `LEAVE_NO` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차번호',
  `LEAVE_NM` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차명',
  `LEAVE_TYPE` char(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차타입[SYS021]',
  `GRANT_TYPE` char(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부여방식[SYS022]',
  `PAID_TYPE` char(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '유급구분[SYS023]',
  `LEAVE_NATURE_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴가성격[SYS024]',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `SYSTEM_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '시스템 시드 여부 (Y: PRAFTA-018 법정 연차용, 화면 편집 불가)',
  `LEAVE_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `MAX_APLY_DAYS` tinyint unsigned DEFAULT NULL COMMENT '최대 신청일수',
  `USE_UNIT_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 사용 단위[SYS025]',
  `AVAIL_TERM_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 사용가능기간 타입[SYS026]',
  `AVAIL_FROM_DT` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 사용기간 FROM',
  `AVAIL_TO_DT` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 사용기간 TO',
  `GRANT_DAYS` tinyint unsigned DEFAULT NULL COMMENT '부여일 수',
  `ADMIN_AVAIL_TERM_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '관리자 부여 연차 사용가능기간 타입[SYS026]',
  `ADMIN_AVAIL_FROM_DT` varchar(6) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '관리자 부여 연차 사용기간 FROM',
  `ADMIN_AVAIL_TO_DT` varchar(6) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '관리자 부여 연차 사용기간 TO',
  `GRANT_BASE_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '자동 부여 기준일[SYS027]',
  `GRANT_OFFSET_MONTH` tinyint unsigned DEFAULT NULL COMMENT '자동부여 실행시점',
  `GRANT_ASSIGN_MMDD` char(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '자동부여 지정일 MMDD (기준일=03 부여일지정 시 필수)',
  `APRV_USE_YN` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '결재여부',
  `APRV_STEP_CNT` tinyint unsigned DEFAULT NULL COMMENT '결재 단계 수',
  `HR_FINAL_APRV_YN` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '인사팀 최종 승인 여부',
  `EVIDENCE_YN` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '증빙여부',
  `EVIDENCE_GUIDE_MSG` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '증빙안내문구',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`LEAVE_CD`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_01` (`CMPNY_CD`,`LEAVE_NO`,`USE_YN`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_02` (`CMPNY_CD`,`LEAVE_NO`,`USE_YN`,`LEAVE_TYPE`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_03` (`CMPNY_CD`,`LEAVE_NO`,`USE_YN`,`LEAVE_TYPE`,`GRANT_TYPE`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_SYSTEM` (`CMPNY_CD`,`SYSTEM_YN`,`USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_leave_usage_policy

**연차 사용 단위 정책 (휴게시간/시간단위 시작시각/1일환산은 시스템 강제)**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 POLICY_SEQ | bigint | NO |  | TB_LEAVE_POLICY.POLICY_SEQ 1:1 |
| CMPNY_CD | varchar(50) | NO |  | 회사 코드 |
| ALLOW_FULL_DAY | char(1) | NO | 'Y' | 1일 단위 (항상 Y, 변경불가) |
| ALLOW_HALF_DAY | char(1) | NO | 'Y' | 0.5일 단위 (AXIS4=HALF_DAY 시 Y 강제) |
| ALLOW_QUARTER_DAY | char(1) | NO | 'Y' | 0.25일 단위 |
| ALLOW_HOURLY | char(1) | NO | 'N' | 0.125일(1시간) 단위 |
| MAX_DAILY_REQUEST | int | NO | '3' | 같은 날 최대 신청 건수 (0=불허) |
| INSERT_NO | varchar(50) | NO |  |  |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP |  |
| UPDATE_NO | varchar(50) | YES | NULL |  |
| UPDATE_DATE | datetime | YES | NULL |  |

키/인덱스:

- **PK**: `PRIMARY KEY (`POLICY_SEQ`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_leave_usage_policy` (
  `POLICY_SEQ` bigint NOT NULL COMMENT 'TB_LEAVE_POLICY.POLICY_SEQ 1:1',
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `ALLOW_FULL_DAY` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '1일 단위 (항상 Y, 변경불가)',
  `ALLOW_HALF_DAY` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '0.5일 단위 (AXIS4=HALF_DAY 시 Y 강제)',
  `ALLOW_QUARTER_DAY` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '0.25일 단위',
  `ALLOW_HOURLY` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '0.125일(1시간) 단위',
  `MAX_DAILY_REQUEST` int NOT NULL DEFAULT '3' COMMENT '같은 날 최대 신청 건수 (0=불허)',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` datetime DEFAULT NULL,
  PRIMARY KEY (`POLICY_SEQ`),
  CONSTRAINT `FK_TB_LEAVE_USAGE_POLICY` FOREIGN KEY (`POLICY_SEQ`) REFERENCES `tb_leave_policy` (`POLICY_SEQ`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 사용 단위 정책 (휴게시간/시간단위 시작시각/1일환산은 시스템 강제)';
```

</details>

## 근태 - 연차 부여/사용

### tb_user_leave_grant

**사용자 연차 부여 이력**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 GRANT_ID | varchar(20) | NO |  | 부여 ID (PK) |
| CMPNY_CD | varchar(50) | NO |  | 회사 코드 |
| USER_CD | varchar(20) | NO |  | 사용자 코드 |
| LEAVE_CD | varchar(20) | NO |  | 연차 코드 (tb_leave_type_mgmt.LEAVE_CD) |
| GRANT_TYPE | varchar(40) | YES | NULL | 부여 분류[SYS035] STATUTORY_*/MANUAL_* prefix (법정/약정 구분) |
| GRANT_DAYS | decimal(5,1) | NO |  | 부여 일수 (반차 0.5 단위 고려) |
| USED_DAYS | decimal(5,1) | NO | '0.0' | 사용 일수 캐시 (tb_user_leave_use 합계와 동기화) |
| GRANT_REASON | varchar(500) | YES | NULL | 부여 사유 (자동부여/관리자수동/특별부여 등) |
| GRANT_BY_TYPE | varchar(2) | NO |  | 부여 방식 (AUTO:자동 / ADMIN:관리자수동) |
| POLICY_SEQ | bigint | YES | NULL | 적용 정책 (TB_LEAVE_POLICY.POLICY_SEQ, 수동 부여는 NULL) |
| GRANT_DATE | varchar(8) | NO |  | 부여 일자 (YYYYMMDD) |
| AVAIL_FROM_DATE | varchar(8) | NO |  | 사용 가능 시작일 (YYYYMMDD) |
| AVAIL_TO_DATE | varchar(8) | NO |  | 사용 가능 종료일 (YYYYMMDD, 소멸일) |
| IDEMPOTENCY_KEY | varchar(100) | YES | NULL | 중복 부여 방지 키 ({USER_CD}_{YYYY}_ANNUAL 등). 자동부여 시 필수 |
| STATUS | varchar(20) | NO | 'ACTIVE' | 상태[SYS040] ACTIVE/EXHAUSTED/EXPIRED/CANCELED (EXPIRE_YN과 동기화 — 정책서 §8.5.8) |
| EXPIRE_YN | varchar(1) | NO | 'N' | 소멸 여부 (배치로 AVAIL_TO_DATE 경과 시 Y) |
| EXPIRE_DATE | datetime | YES | NULL | 소멸 처리 일시 |
| DEL_YN | varchar(1) | NO | 'N' | 삭제 여부 |
| INSERT_NO | varchar(50) | NO |  | 등록자 |
| INSERT_DATE | datetime | NO |  | 등록 일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정 일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`GRANT_ID`)`
- **UNIQUE**: `UNIQUE KEY `UK_LEAVE_GRANT_IDEMPOTENCY` (`CMPNY_CD`,`IDEMPOTENCY_KEY`)`
- **INDEX**: `KEY `IDX_LEAVE_GRANT_USER` (`CMPNY_CD`,`USER_CD`,`LEAVE_CD`,`EXPIRE_YN`)`
- **INDEX**: `KEY `IDX_LEAVE_GRANT_AVAIL` (`CMPNY_CD`,`USER_CD`,`AVAIL_TO_DATE`,`EXPIRE_YN`)`
- **INDEX**: `KEY `IDX_LEAVE_GRANT_TYPE` (`CMPNY_CD`,`LEAVE_CD`)`
- **INDEX**: `KEY `IX_LEAVE_GRANT_STATUS` (`CMPNY_CD`,`USER_CD`,`STATUS`,`AVAIL_TO_DATE`)`
- **INDEX**: `KEY `IX_LEAVE_GRANT_GTYPE` (`CMPNY_CD`,`GRANT_TYPE`,`GRANT_DATE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_leave_grant` (
  `GRANT_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '부여 ID (PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 코드',
  `LEAVE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차 코드 (tb_leave_type_mgmt.LEAVE_CD)',
  `GRANT_TYPE` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부여 분류[SYS035] STATUTORY_*/MANUAL_* prefix (법정/약정 구분)',
  `GRANT_DAYS` decimal(5,1) NOT NULL COMMENT '부여 일수 (반차 0.5 단위 고려)',
  `USED_DAYS` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '사용 일수 캐시 (tb_user_leave_use 합계와 동기화)',
  `GRANT_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부여 사유 (자동부여/관리자수동/특별부여 등)',
  `GRANT_BY_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '부여 방식 (AUTO:자동 / ADMIN:관리자수동)',
  `POLICY_SEQ` bigint DEFAULT NULL COMMENT '적용 정책 (TB_LEAVE_POLICY.POLICY_SEQ, 수동 부여는 NULL)',
  `GRANT_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '부여 일자 (YYYYMMDD)',
  `AVAIL_FROM_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 가능 시작일 (YYYYMMDD)',
  `AVAIL_TO_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 가능 종료일 (YYYYMMDD, 소멸일)',
  `IDEMPOTENCY_KEY` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '중복 부여 방지 키 ({USER_CD}_{YYYY}_ANNUAL 등). 자동부여 시 필수',
  `STATUS` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '상태[SYS040] ACTIVE/EXHAUSTED/EXPIRED/CANCELED (EXPIRE_YN과 동기화 — 정책서 §8.5.8)',
  `EXPIRE_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '소멸 여부 (배치로 AVAIL_TO_DATE 경과 시 Y)',
  `EXPIRE_DATE` datetime DEFAULT NULL COMMENT '소멸 처리 일시',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`GRANT_ID`),
  UNIQUE KEY `UK_LEAVE_GRANT_IDEMPOTENCY` (`CMPNY_CD`,`IDEMPOTENCY_KEY`),
  KEY `IDX_LEAVE_GRANT_USER` (`CMPNY_CD`,`USER_CD`,`LEAVE_CD`,`EXPIRE_YN`),
  KEY `IDX_LEAVE_GRANT_AVAIL` (`CMPNY_CD`,`USER_CD`,`AVAIL_TO_DATE`,`EXPIRE_YN`),
  KEY `IDX_LEAVE_GRANT_TYPE` (`CMPNY_CD`,`LEAVE_CD`),
  KEY `IX_LEAVE_GRANT_STATUS` (`CMPNY_CD`,`USER_CD`,`STATUS`,`AVAIL_TO_DATE`),
  KEY `IX_LEAVE_GRANT_GTYPE` (`CMPNY_CD`,`GRANT_TYPE`,`GRANT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 연차 부여 이력';
```

</details>

### tb_user_leave_use

**사용자 연차 사용 실적**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 LEAVE_ID | varchar(20) | NO |  | 연차 사용 ID (PK) |
| CMPNY_CD | varchar(50) | NO |  | 회사 코드 |
| SITE_CD | varchar(50) | NO |  | 사업장 코드 |
| USER_CD | varchar(20) | NO |  | 사용자 코드 |
| LEAVE_CD | varchar(20) | NO |  | 연차 코드 (tb_leave_type_mgmt.LEAVE_CD) |
| REQ_ID | varchar(20) | YES | NULL | 연관 요청 ID (tb_user_attd_req.REQ_ID, 결재 미사용 또는 사후 등록 시 NULL) |
| GRANT_ID | varchar(20) | YES | NULL | 차감 대상 부여 ID (tb_user_leave_grant.GRANT_ID) |
| START_DATE | varchar(8) | NO |  | 사용 시작일 (YYYYMMDD) |
| START_TIME | varchar(4) | YES | NULL | 시작 시각 (HHMM, 시간단위 휴가 시) |
| END_DATE | varchar(8) | NO |  | 사용 종료일 (YYYYMMDD) |
| END_TIME | varchar(4) | YES | NULL | 종료 시각 (HHMM, 시간단위 휴가 시) |
| USE_UNIT_TYPE | varchar(2) | NO |  | 사용 단위 (tb_leave_type_mgmt.USE_UNIT_TYPE 복사, SYS025) |
| LEAVE_DAYS | decimal(5,1) | NO |  | 사용 일수 (0.5/1.0/N.0, 일단위 계산용) |
| LEAVE_MINUTES | int | YES | NULL | 사용 분 (시간단위 휴가 시) |
| LEAVE_REASON | varchar(500) | YES | NULL | 사용 사유 |
| EVIDENCE_FILE_ID | varchar(50) | YES | NULL | 증빙 파일 ID (tb_leave_type_mgmt.EVIDENCE_YN=Y 시) |
| LEAVE_STATUS | varchar(10) | NO |  | 사용 상태 (CONFIRMED:확정 / CANCELLED:취소) |
| CANCEL_REASON | varchar(500) | YES | NULL | 취소 사유 |
| CANCEL_DATE | datetime | YES | NULL | 취소 일시 |
| DEL_YN | varchar(1) | NO | 'N' | 삭제 여부 |
| INSERT_NO | varchar(50) | NO |  | 등록자 |
| INSERT_DATE | datetime | NO |  | 등록 일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정 일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`LEAVE_ID`)`
- **INDEX**: `KEY `IDX_LEAVE_USE_USER` (`CMPNY_CD`,`USER_CD`,`START_DATE`,`LEAVE_STATUS`)`
- **INDEX**: `KEY `IDX_LEAVE_USE_SITE` (`CMPNY_CD`,`SITE_CD`,`START_DATE`,`LEAVE_STATUS`)`
- **INDEX**: `KEY `IDX_LEAVE_USE_GRANT` (`GRANT_ID`)`
- **INDEX**: `KEY `IDX_LEAVE_USE_REQ` (`REQ_ID`)`
- **INDEX**: `KEY `IDX_LEAVE_USE_TYPE` (`CMPNY_CD`,`LEAVE_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_leave_use` (
  `LEAVE_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차 사용 ID (PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 코드',
  `LEAVE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차 코드 (tb_leave_type_mgmt.LEAVE_CD)',
  `REQ_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연관 요청 ID (tb_user_attd_req.REQ_ID, 결재 미사용 또는 사후 등록 시 NULL)',
  `GRANT_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '차감 대상 부여 ID (tb_user_leave_grant.GRANT_ID)',
  `START_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 시작일 (YYYYMMDD)',
  `START_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시작 시각 (HHMM, 시간단위 휴가 시)',
  `END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 종료일 (YYYYMMDD)',
  `END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 시각 (HHMM, 시간단위 휴가 시)',
  `USE_UNIT_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 단위 (tb_leave_type_mgmt.USE_UNIT_TYPE 복사, SYS025)',
  `LEAVE_DAYS` decimal(5,1) NOT NULL COMMENT '사용 일수 (0.5/1.0/N.0, 일단위 계산용)',
  `LEAVE_MINUTES` int DEFAULT NULL COMMENT '사용 분 (시간단위 휴가 시)',
  `LEAVE_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사용 사유',
  `EVIDENCE_FILE_ID` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '증빙 파일 ID (tb_leave_type_mgmt.EVIDENCE_YN=Y 시)',
  `LEAVE_STATUS` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 상태 (CONFIRMED:확정 / CANCELLED:취소)',
  `CANCEL_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '취소 사유',
  `CANCEL_DATE` datetime DEFAULT NULL COMMENT '취소 일시',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`LEAVE_ID`),
  KEY `IDX_LEAVE_USE_USER` (`CMPNY_CD`,`USER_CD`,`START_DATE`,`LEAVE_STATUS`),
  KEY `IDX_LEAVE_USE_SITE` (`CMPNY_CD`,`SITE_CD`,`START_DATE`,`LEAVE_STATUS`),
  KEY `IDX_LEAVE_USE_GRANT` (`GRANT_ID`),
  KEY `IDX_LEAVE_USE_REQ` (`REQ_ID`),
  KEY `IDX_LEAVE_USE_TYPE` (`CMPNY_CD`,`LEAVE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 연차 사용 실적';
```

</details>

## 근태 - 연장근무 / 근무계획

### tb_user_overtime_mgmt

**사용자 초과근무 실적 관리**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 OT_ID | varchar(20) | NO |  | 초과근무 ID (PK) |
| CMPNY_CD | varchar(50) | NO |  | 회사 코드 |
| SITE_CD | varchar(50) | NO |  | 사업장 코드 |
| USER_CD | varchar(20) | NO |  | 근무자 사용자 코드 |
| ATTD_ID | varchar(20) | YES | NULL | 연관 근태 ID (tb_user_attd_mgmt.ATTD_ID, 휴일근무 등 정규근태 없는 경우 NULL) |
| REQ_ID | varchar(20) | YES | NULL | 연관 요청 ID (tb_user_attd_req.REQ_ID, 사후 등록 시 NULL) |
| WORK_YMD | varchar(8) | NO |  | 근무 일자 (YYYYMMDD) |
| NODE_CD | varchar(50) | YES | NULL | 근무 노드 코드 |
| OT_TYPE | varchar(10) | NO |  | 초과근무 유형 (EXTEND:연장 / NIGHT:야간 / HOLIDAY:휴일) |
| PLAN_START_DATE | varchar(8) | YES | NULL | 계획 시작 일자 (YYYYMMDD) |
| PLAN_START_TIME | varchar(4) | YES | NULL | 계획 시작 시각 (HHMM) |
| PLAN_END_DATE | varchar(8) | YES | NULL | 계획 종료 일자 (YYYYMMDD) |
| PLAN_END_TIME | varchar(4) | YES | NULL | 계획 종료 시각 (HHMM) |
| ACTUAL_START_DATE | varchar(8) | NO |  | 실제 시작 일자 (YYYYMMDD) |
| ACTUAL_START_TIME | varchar(4) | NO |  | 실제 시작 시각 (HHMM) |
| ACTUAL_START_METHOD | varchar(2) | YES | NULL | 시작 체크 방식 (GPS/QR/MANUAL 등) |
| ACTUAL_END_DATE | varchar(8) | YES | NULL | 실제 종료 일자 (YYYYMMDD) |
| ACTUAL_END_TIME | varchar(4) | YES | NULL | 실제 종료 시각 (HHMM) |
| ACTUAL_END_METHOD | varchar(2) | YES | NULL | 종료 체크 방식 (GPS/QR/MANUAL 등) |
| WORK_MINUTES | int | YES | NULL | 실제 근무 시간 (분 단위, 휴게시간 제외) |
| BREAK_MINUTES | int | YES | '0' | 휴게 시간 (분 단위) |
| OT_STATUS | varchar(10) | NO |  | 초과근무 상태 (IN_PROGRESS:진행중 / COMPLETED:완료 / CANCELLED:취소) |
| DEL_YN | varchar(1) | NO | 'N' | 삭제 여부 |
| INSERT_NO | varchar(50) | NO |  | 등록자 |
| INSERT_DATE | datetime | NO |  | 등록 일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정 일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`OT_ID`)`
- **INDEX**: `KEY `IDX_OT_USER_YMD` (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`WORK_YMD`)`
- **INDEX**: `KEY `IDX_OT_SITE_YMD` (`CMPNY_CD`,`SITE_CD`,`WORK_YMD`,`OT_STATUS`)`
- **INDEX**: `KEY `IDX_OT_ATTD` (`ATTD_ID`)`
- **INDEX**: `KEY `IDX_OT_REQ` (`REQ_ID`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_overtime_mgmt` (
  `OT_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '초과근무 ID (PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근무자 사용자 코드',
  `ATTD_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연관 근태 ID (tb_user_attd_mgmt.ATTD_ID, 휴일근무 등 정규근태 없는 경우 NULL)',
  `REQ_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연관 요청 ID (tb_user_attd_req.REQ_ID, 사후 등록 시 NULL)',
  `WORK_YMD` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근무 일자 (YYYYMMDD)',
  `NODE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무 노드 코드',
  `OT_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '초과근무 유형 (EXTEND:연장 / NIGHT:야간 / HOLIDAY:휴일)',
  `PLAN_START_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계획 시작 일자 (YYYYMMDD)',
  `PLAN_START_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계획 시작 시각 (HHMM)',
  `PLAN_END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계획 종료 일자 (YYYYMMDD)',
  `PLAN_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계획 종료 시각 (HHMM)',
  `ACTUAL_START_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '실제 시작 일자 (YYYYMMDD)',
  `ACTUAL_START_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '실제 시작 시각 (HHMM)',
  `ACTUAL_START_METHOD` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시작 체크 방식 (GPS/QR/MANUAL 등)',
  `ACTUAL_END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '실제 종료 일자 (YYYYMMDD)',
  `ACTUAL_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '실제 종료 시각 (HHMM)',
  `ACTUAL_END_METHOD` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 체크 방식 (GPS/QR/MANUAL 등)',
  `WORK_MINUTES` int DEFAULT NULL COMMENT '실제 근무 시간 (분 단위, 휴게시간 제외)',
  `BREAK_MINUTES` int DEFAULT '0' COMMENT '휴게 시간 (분 단위)',
  `OT_STATUS` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '초과근무 상태 (IN_PROGRESS:진행중 / COMPLETED:완료 / CANCELLED:취소)',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`OT_ID`),
  KEY `IDX_OT_USER_YMD` (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`WORK_YMD`),
  KEY `IDX_OT_SITE_YMD` (`CMPNY_CD`,`SITE_CD`,`WORK_YMD`,`OT_STATUS`),
  KEY `IDX_OT_ATTD` (`ATTD_ID`),
  KEY `IDX_OT_REQ` (`REQ_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 초과근무 실적 관리';
```

</details>

### tb_user_work_plan

**사용자 근무 계획**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 USER_CD | varchar(20) | NO |  | 사용자코드 |
| 🔑 WORK_YMD | varchar(8) | NO |  | 근무일 |
| WORK_PLAN_CD | varchar(20) | YES | NULL | 근무계획코드[SCH_CD, LEAVE_CD] |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`WORK_YMD`)`
- **INDEX**: `KEY `IX_WORK_PLAN_SITE` (`CMPNY_CD`,`SITE_CD`)`
- **INDEX**: `KEY `IX_WORK_PLAN_USER` (`CMPNY_CD`,`USER_CD`)`
- **INDEX**: `KEY `IX_WORK_PLAN_USER_YMD` (`CMPNY_CD`,`USER_CD`,`WORK_YMD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_user_work_plan` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `WORK_YMD` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근무일',
  `WORK_PLAN_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무계획코드[SCH_CD, LEAVE_CD]',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`WORK_YMD`),
  KEY `IX_WORK_PLAN_SITE` (`CMPNY_CD`,`SITE_CD`),
  KEY `IX_WORK_PLAN_USER` (`CMPNY_CD`,`USER_CD`),
  KEY `IX_WORK_PLAN_USER_YMD` (`CMPNY_CD`,`USER_CD`,`WORK_YMD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 근무 계획';
```

</details>

## 근태 - 휴일

### tb_holiday

**휴일관리**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 HOLIDAY_ID | varchar(10) | NO |  | 휴일ID |
| HOLIDAY_NM | varchar(200) | NO |  | 휴일명 |
| HOLIDAY_YMD | date | NO |  | 휴일 |
| HOLIDAY_TYPE | varchar(2) | NO |  | 휴일타입[SYS020] |
| USE_YN | char(1) | NO | 'Y' | 사용여부(Y/N) |
| INSERT_NO | varchar(50) | NO | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`HOLIDAY_ID`)`
- **INDEX**: `KEY `IX_HOLIDAY_DAY` (`CMPNY_CD`,`HOLIDAY_YMD`)`
- **INDEX**: `KEY `IX_HOLIDAY_DAY_TYPE` (`CMPNY_CD`,`HOLIDAY_YMD`,`HOLIDAY_TYPE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_holiday` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `HOLIDAY_ID` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일ID',
  `HOLIDAY_NM` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일명',
  `HOLIDAY_YMD` date NOT NULL COMMENT '휴일',
  `HOLIDAY_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일타입[SYS020]',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부(Y/N)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`HOLIDAY_ID`),
  KEY `IX_HOLIDAY_DAY` (`CMPNY_CD`,`HOLIDAY_YMD`),
  KEY `IX_HOLIDAY_DAY_TYPE` (`CMPNY_CD`,`HOLIDAY_YMD`,`HOLIDAY_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='휴일관리';
```

</details>

### tb_holiday_rule

**휴일규칙(매년고정 회사휴일)**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 HOLIDAY_RULE_ID | varchar(10) | NO |  | 휴일규칙ID |
| HOLIDAY_RULE_NM | varchar(200) | NO |  | 휴일규칙명 |
| HOLIDAY_MM | char(2) | NO |  | 월(01~12) |
| HOLIDAY_DD | char(2) | NO |  | 일(01~31) |
| HOLIDAY_TYPE | varchar(2) | NO |  | 휴일타입[SYS020] |
| USE_YN | char(1) | NO | 'Y' | 사용여부(Y/N) |
| INSERT_NO | varchar(50) | NO | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`HOLIDAY_RULE_ID`)`
- **UNIQUE**: `UNIQUE KEY `UK_HOLIDAY_RULE_MMDD` (`CMPNY_CD`,`HOLIDAY_MM`,`HOLIDAY_DD`,`HOLIDAY_TYPE`)`
- **INDEX**: `KEY `IX_HOLIDAY_RULE_USE` (`CMPNY_CD`,`USE_YN`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_holiday_rule` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `HOLIDAY_RULE_ID` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일규칙ID',
  `HOLIDAY_RULE_NM` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일규칙명',
  `HOLIDAY_MM` char(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '월(01~12)',
  `HOLIDAY_DD` char(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '일(01~31)',
  `HOLIDAY_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일타입[SYS020]',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부(Y/N)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`HOLIDAY_RULE_ID`),
  UNIQUE KEY `UK_HOLIDAY_RULE_MMDD` (`CMPNY_CD`,`HOLIDAY_MM`,`HOLIDAY_DD`,`HOLIDAY_TYPE`),
  KEY `IX_HOLIDAY_RULE_USE` (`CMPNY_CD`,`USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='휴일규칙(매년고정 회사휴일)';
```

</details>

## 스케줄 / 교대근무

### tb_sch_mgmt

**사업장 근무타입 관리**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 SCH_CD | varchar(20) | NO |  | 스케줄 코드 |
| SCH_NO | varchar(50) | NO |  | 스케줄 번호 |
| SCH_TYPE | varchar(2) | NO |  | 스케줄타입[SYS019] |
| BASE_YN | varchar(2) | YES | NULL | 기본스케줄여부[SYS003] |
| APPLY_DATE | varchar(8) | NO |  | 적용일자 |
| FST_SCH_STR_TIME | varchar(4) | NO |  | 1구간 시작시간 |
| FST_SCH_END_TIME | varchar(4) | NO |  | 1구간 종료시간 |
| FST_SCH_BRK_MIN | varchar(3) | YES | NULL | 1구간 휴게시간 |
| SEC_SCH_STR_TIME | varchar(4) | YES | NULL | 2구간 시작시간 |
| SEC_SCH_END_TIME | varchar(4) | YES | NULL | 2구간 종료시간 |
| SEC_SCH_BRK_MIN | varchar(3) | YES | NULL | 2구간 휴게시간 |
| USE_YN | varchar(2) | NO | 'Y' | 사용여부 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SCH_CD`)`
- **INDEX**: `KEY `IX_TB_SCH_MGMT_LIST` (`CMPNY_CD`,`SITE_CD`,`USE_YN`,`SCH_TYPE`,`SCH_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_sch_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SCH_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄 코드',
  `SCH_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄 번호',
  `SCH_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄타입[SYS019]',
  `BASE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '기본스케줄여부[SYS003]',
  `APPLY_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '적용일자',
  `FST_SCH_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '1구간 시작시간',
  `FST_SCH_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '1구간 종료시간',
  `FST_SCH_BRK_MIN` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '1구간 휴게시간',
  `SEC_SCH_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 시작시간',
  `SEC_SCH_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 종료시간',
  `SEC_SCH_BRK_MIN` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 휴게시간',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SCH_CD`),
  KEY `IX_TB_SCH_MGMT_LIST` (`CMPNY_CD`,`SITE_CD`,`USE_YN`,`SCH_TYPE`,`SCH_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사업장 근무타입 관리';
```

</details>

### tb_sch_mgmt_hist

**사업장 근무타입 이력관리**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 HIST_IDX | int | NO |  | 이력시퀀스 |
| 🔑 SCH_CD | varchar(20) | NO |  | 스케줄 코드 |
| APPLY_DATE | varchar(8) | NO |  | 적용일자 |
| FST_SCH_STR_TIME | varchar(4) | NO |  | 1구간 시작시간 |
| FST_SCH_END_TIME | varchar(4) | NO |  | 1구간 종료시간 |
| FST_SCH_BRK_MIN | varchar(3) | YES | NULL | 1구간 휴게시간 |
| SEC_SCH_STR_TIME | varchar(4) | YES | NULL | 2구간 시작시간 |
| SEC_SCH_END_TIME | varchar(4) | YES | NULL | 2구간 종료시간 |
| SEC_SCH_BRK_MIN | varchar(3) | YES | NULL | 2구간 휴게시간 |
| USE_YN | varchar(2) | NO | 'Y' | 사용여부 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`HIST_IDX`,`SCH_CD`)`
- **INDEX**: `KEY `IX_TB_SCH_MGMT_HIST_LIST` (`CMPNY_CD`,`SITE_CD`,`HIST_IDX`,`USE_YN`,`SCH_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_sch_mgmt_hist` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `HIST_IDX` int NOT NULL COMMENT '이력시퀀스',
  `SCH_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄 코드',
  `APPLY_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '적용일자',
  `FST_SCH_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '1구간 시작시간',
  `FST_SCH_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '1구간 종료시간',
  `FST_SCH_BRK_MIN` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '1구간 휴게시간',
  `SEC_SCH_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 시작시간',
  `SEC_SCH_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 종료시간',
  `SEC_SCH_BRK_MIN` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 휴게시간',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`HIST_IDX`,`SCH_CD`),
  KEY `IX_TB_SCH_MGMT_HIST_LIST` (`CMPNY_CD`,`SITE_CD`,`HIST_IDX`,`USE_YN`,`SCH_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사업장 근무타입 이력관리';
```

</details>

### tb_shift_sch_mgmt

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 SHIFT_CD | varchar(50) | NO |  | 교대근무코드 |
| SHIFT_NO | varchar(50) | NO |  | 교대근무번호 |
| SHIFT_PTRN_CNT | tinyint unsigned | NO |  | 교대 패턴 수 |
| SHIFT_TEAM_CNT | tinyint unsigned | NO |  | 교대 팀 수 |
| SHIFT_CYCLE_DAYS | tinyint unsigned | NO |  | 근무 교대주기 |
| USE_YN | char(1) | NO | 'Y' | 사용여부(Y/N) |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`)`
- **INDEX**: `KEY `IX_TB_SHIFT_SCH_MGMT_LIST` (`CMPNY_CD`,`SITE_CD`,`SHIFT_NO`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_shift_sch_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `SHIFT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무번호',
  `SHIFT_PTRN_CNT` tinyint unsigned NOT NULL COMMENT '교대 패턴 수',
  `SHIFT_TEAM_CNT` tinyint unsigned NOT NULL COMMENT '교대 팀 수',
  `SHIFT_CYCLE_DAYS` tinyint unsigned NOT NULL COMMENT '근무 교대주기',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부(Y/N)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`),
  KEY `IX_TB_SHIFT_SCH_MGMT_LIST` (`CMPNY_CD`,`SITE_CD`,`SHIFT_NO`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_shift_sch_ptrn_mgmt

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 SHIFT_CD | varchar(50) | NO |  | 교대근무코드 |
| 🔑 PTRN_IDX | tinyint unsigned | NO |  | 교대패턴 순번 |
| SCH_CD | varchar(20) | NO |  | 스케줄 코드 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`PTRN_IDX`)`
- **INDEX**: `KEY `IX_TB_SHIFT_SCH_PTRN_MGMT_LIST` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`SCH_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_shift_sch_ptrn_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `PTRN_IDX` tinyint unsigned NOT NULL COMMENT '교대패턴 순번',
  `SCH_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄 코드',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`PTRN_IDX`),
  KEY `IX_TB_SHIFT_SCH_PTRN_MGMT_LIST` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`SCH_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_shift_sch_assign_mgmt

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 SHIFT_CD | varchar(50) | NO |  | 교대근무코드 |
| 🔑 TEAM_IDX | tinyint unsigned | NO |  | 팀 순번 |
| 🔑 DAY_NO | tinyint unsigned | NO |  | 일자(1~SHIFT_CYCLE_DAYS) |
| ASSIGN_YN | char(1) | NO |  | 스케줄유무 |
| SCH_CD | varchar(20) | YES | NULL | 스케줄코드 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`TEAM_IDX`,`DAY_NO`)`
- **INDEX**: `KEY `IX_SHIFT_ASSIGN_01` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`DAY_NO`)`
- **INDEX**: `KEY `IX_SHIFT_ASSIGN_02` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`TEAM_IDX`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_shift_sch_assign_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `TEAM_IDX` tinyint unsigned NOT NULL COMMENT '팀 순번',
  `DAY_NO` tinyint unsigned NOT NULL COMMENT '일자(1~SHIFT_CYCLE_DAYS)',
  `ASSIGN_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄유무',
  `SCH_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '스케줄코드',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`TEAM_IDX`,`DAY_NO`),
  KEY `IX_SHIFT_ASSIGN_01` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`DAY_NO`),
  KEY `IX_SHIFT_ASSIGN_02` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`TEAM_IDX`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_shift_sch_team_mgmt

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 SHIFT_CD | varchar(50) | NO |  | 교대근무코드 |
| 🔑 SHIFT_TEAM_ID | varchar(12) | NO |  | 교대근무팀ID |
| SHIFT_TEAM_NM | varchar(100) | YES | NULL | 교대근무팀명 |
| STR_DATE | varchar(8) | YES | NULL | 시작일자 |
| END_DATE | varchar(8) | YES | NULL | 종료일자 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`SHIFT_TEAM_ID`)`
- **INDEX**: `KEY `IDX_SHIFT_SCH_TEAM_MGMT_01` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`)`
- **INDEX**: `KEY `IDX_SHIFT_SCH_TEAM_MGMT_02` (`CMPNY_CD`,`SITE_CD`,`STR_DATE`,`END_DATE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_shift_sch_team_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `SHIFT_TEAM_ID` varchar(12) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무팀ID',
  `SHIFT_TEAM_NM` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '교대근무팀명',
  `STR_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시작일자',
  `END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료일자',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`SHIFT_TEAM_ID`),
  KEY `IDX_SHIFT_SCH_TEAM_MGMT_01` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`),
  KEY `IDX_SHIFT_SCH_TEAM_MGMT_02` (`CMPNY_CD`,`SITE_CD`,`STR_DATE`,`END_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_shift_sch_team_meta_info

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 SHIFT_CD | varchar(50) | NO |  | 교대근무코드 |
| 🔑 TEAM_IDX | tinyint unsigned | NO |  | 팀 순번 |
| TEAM_NM | varchar(1) | NO |  | 팀명 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`TEAM_IDX`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_shift_sch_team_meta_info` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `TEAM_IDX` tinyint unsigned NOT NULL COMMENT '팀 순번',
  `TEAM_NM` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '팀명',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`TEAM_IDX`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_shift_sch_team_user

**교대근무 팀 소속 사용자 관리**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 SHIFT_CD | varchar(50) | NO |  | 교대근무코드 |
| 🔑 SHIFT_TEAM_ID | varchar(12) | NO |  | 교대근무팀ID |
| TEAM_IDX | tinyint unsigned | NO |  | 팀 순번 |
| 🔑 USER_CD | varchar(50) | NO |  | 사용자코드 |
| LEADER_YN | varchar(50) | NO |  | 팀리더여부 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`SHIFT_TEAM_ID`,`USER_CD`)`
- **INDEX**: `KEY `IDX_SHIFT_TEAM_USER_01` (`CMPNY_CD`,`SITE_CD`,`USER_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_shift_sch_team_user` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `SHIFT_TEAM_ID` varchar(12) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무팀ID',
  `TEAM_IDX` tinyint unsigned NOT NULL COMMENT '팀 순번',
  `USER_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `LEADER_YN` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '팀리더여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`SHIFT_TEAM_ID`,`USER_CD`),
  KEY `IDX_SHIFT_TEAM_USER_01` (`CMPNY_CD`,`SITE_CD`,`USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='교대근무 팀 소속 사용자 관리';
```

</details>

## 일용직 계정 / 슬롯

### tb_daily_user

**일용직 사용자**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 USER_CD | varchar(20) | NO |  | 사용자코드 |
| USER_ID | varchar(50) | NO |  | 사용자ID(USER_CD 기반 자동생성, 표시용) |
| USER_NM | varchar(50) | NO |  | 사용자명 |
| USER_PW | varchar(100) | NO |  | 비밀번호(해시) - QR발급 사용자는 난수 |
| MBL_NO_ENC | text | YES |  | 휴대폰번호 AES-GCM (v1.base64url) |
| MBL_NO_HMAC | varchar(43) | YES | NULL | 휴대폰번호 HMAC-SHA256 Base64URL |
| MBL_NO_LAST4 | char(4) | YES | NULL | 휴대폰번호 마지막4자리(마스킹/리스트용) |
| REG_TYPE | varchar(20) | NO |  | 가입경로SYS030] |
| USE_YN | varchar(2) | NO | 'Y' | 사용여부 |
| ACCOUNT_STATUS | varchar(20) | NO | '01' | 계정상태[SYS013] |
| WORK_EXPIRE_DATE | varchar(8) | NO |  | 계정 만료일(YYYYMMDD, 자정 배치 기준) |
| WITHDRAWAL_DATE | varchar(8) | YES | NULL | 회원탈퇴일 |
| PWD_FAIL_CNT | int | NO | '0' | 비밀번호실패횟수 |
| PWD_LOCK_YN | varchar(2) | NO | 'N' | 비밀번호잠금여부 |
| PWD_LOCK_EXPIRE_DTIME | datetime | YES | NULL | 비밀번호 인증 실패 잠금 만료일시 |
| LAST_LOGIN_DTIME | datetime | YES | NULL | 마지막 로그인일시 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`USER_CD`)`
- **UNIQUE**: `UNIQUE KEY `UX_TB_DAILY_USER_MBL` (`CMPNY_CD`,(if((`USE_YN` = _utf8mb4'Y'),`MBL_NO_HMAC`,NULL)))`
- **INDEX**: `KEY `IX_TB_DAILY_USER_MBL_LOOKUP` (`CMPNY_CD`,`MBL_NO_HMAC`)`
- **INDEX**: `KEY `IX_TB_DAILY_USER_EXPIRE` (`WORK_EXPIRE_DATE`,`USE_YN`)`
- **INDEX**: `KEY `IX_TB_DAILY_USER_SITE` (`CMPNY_CD`,`SITE_CD`,`USE_YN`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_daily_user` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `USER_ID` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자ID(USER_CD 기반 자동생성, 표시용)',
  `USER_NM` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자명',
  `USER_PW` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '비밀번호(해시) - QR발급 사용자는 난수',
  `MBL_NO_ENC` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '휴대폰번호 AES-GCM (v1.base64url)',
  `MBL_NO_HMAC` varchar(43) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '휴대폰번호 HMAC-SHA256 Base64URL',
  `MBL_NO_LAST4` char(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '휴대폰번호 마지막4자리(마스킹/리스트용)',
  `REG_TYPE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '가입경로SYS030]',
  `USE_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `ACCOUNT_STATUS` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '01' COMMENT '계정상태[SYS013]',
  `WORK_EXPIRE_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '계정 만료일(YYYYMMDD, 자정 배치 기준)',
  `WITHDRAWAL_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '회원탈퇴일',
  `PWD_FAIL_CNT` int NOT NULL DEFAULT '0' COMMENT '비밀번호실패횟수',
  `PWD_LOCK_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '비밀번호잠금여부',
  `PWD_LOCK_EXPIRE_DTIME` datetime DEFAULT NULL COMMENT '비밀번호 인증 실패 잠금 만료일시',
  `LAST_LOGIN_DTIME` datetime DEFAULT NULL COMMENT '마지막 로그인일시',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`USER_CD`),
  UNIQUE KEY `UX_TB_DAILY_USER_MBL` (`CMPNY_CD`,(if((`USE_YN` = _utf8mb4'Y'),`MBL_NO_HMAC`,NULL))),
  KEY `IX_TB_DAILY_USER_MBL_LOOKUP` (`CMPNY_CD`,`MBL_NO_HMAC`),
  KEY `IX_TB_DAILY_USER_EXPIRE` (`WORK_EXPIRE_DATE`,`USE_YN`),
  KEY `IX_TB_DAILY_USER_SITE` (`CMPNY_CD`,`SITE_CD`,`USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일용직 사용자';
```

</details>

### tb_daily_user_slot

**일일계정 슬롯(현재 점유 상태만 관리)**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 SLOT_NO | varchar(4) | NO |  | 슬롯 번호(1~N) |
| SLOT_TYPE | varchar(2) | NO |  | 슬롯구분[SYS014] |
| FIXED_YN | varchar(2) | YES | 'N' | 고정여부[SYS017] |
| USE_YN | varchar(2) | NO | 'Y' | 사용여부[SYS003] |
| CURR_USER_CD | varchar(50) | YES | NULL | 현재 점유중 사용자CD |
| SLOT_STATUS | varchar(2) | NO | '00' | 슬롯상태[SYS015] |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SLOT_NO`)`
- **INDEX**: `KEY `IDX_DAILY_SLOT_STATUS` (`CMPNY_CD`,`SITE_CD`,`SLOT_STATUS`)`
- **INDEX**: `KEY `IDX_DAILY_SLOT_USER` (`CMPNY_CD`,`CURR_USER_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_daily_user_slot` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SLOT_NO` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '슬롯 번호(1~N)',
  `SLOT_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '슬롯구분[SYS014]',
  `FIXED_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '고정여부[SYS017]',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
  `CURR_USER_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '현재 점유중 사용자CD',
  `SLOT_STATUS` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '00' COMMENT '슬롯상태[SYS015]',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SLOT_NO`),
  KEY `IDX_DAILY_SLOT_STATUS` (`CMPNY_CD`,`SITE_CD`,`SLOT_STATUS`),
  KEY `IDX_DAILY_SLOT_USER` (`CMPNY_CD`,`CURR_USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일일계정 슬롯(현재 점유 상태만 관리)';
```

</details>

### tb_daily_user_slot_his

**일일계정 슬롯 사용 이력**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 HIS_ID | varchar(20) | NO |  | 이력ID(PK) |
| CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| SITE_CD | varchar(50) | NO |  | 사업장코드 |
| SLOT_NO | varchar(4) | NO |  | 슬롯 번호 |
| WORK_DATE | char(8) | NO |  | 사용 일자(YYYYMMDD) |
| USER_ID | varchar(50) | NO |  | 할당 사용자ID |
| ISSUE_CHANNEL | varchar(20) | NO |  | 발급채널[SYS014] |
| OCCUPY_DTIME | datetime | NO |  | 점유 시작 일시 |
| RELEASE_DTIME | datetime | YES | NULL | 해제 일시 |
| RELEASE_USER | varchar(20) | YES | 'SYSTEM' | 점유해제자 |
| RELEASE_TYPE | varchar(20) | YES | NULL | 해제유형[SYS016] |
| RELEASE_REASON | varchar(200) | YES | NULL | 해제 사유 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`HIS_ID`)`
- **INDEX**: `KEY `IDX_SLOT_HIS_DAY` (`CMPNY_CD`,`SITE_CD`,`WORK_DATE`)`
- **INDEX**: `KEY `IDX_SLOT_HIS_SLOT` (`CMPNY_CD`,`SITE_CD`,`SLOT_NO`,`WORK_DATE`)`
- **INDEX**: `KEY `IDX_SLOT_HIS_USER` (`CMPNY_CD`,`USER_ID`,`WORK_DATE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_daily_user_slot_his` (
  `HIS_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력ID(PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SLOT_NO` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '슬롯 번호',
  `WORK_DATE` char(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 일자(YYYYMMDD)',
  `USER_ID` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '할당 사용자ID',
  `ISSUE_CHANNEL` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '발급채널[SYS014]',
  `OCCUPY_DTIME` datetime NOT NULL COMMENT '점유 시작 일시',
  `RELEASE_DTIME` datetime DEFAULT NULL COMMENT '해제 일시',
  `RELEASE_USER` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '점유해제자',
  `RELEASE_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '해제유형[SYS016]',
  `RELEASE_REASON` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '해제 사유',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`HIS_ID`),
  KEY `IDX_SLOT_HIS_DAY` (`CMPNY_CD`,`SITE_CD`,`WORK_DATE`),
  KEY `IDX_SLOT_HIS_SLOT` (`CMPNY_CD`,`SITE_CD`,`SLOT_NO`,`WORK_DATE`),
  KEY `IDX_SLOT_HIS_USER` (`CMPNY_CD`,`USER_ID`,`WORK_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일일계정 슬롯 사용 이력';
```

</details>

### tb_daily_link_mgmt

**일용직 계정 생성 링크 관리**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 LINK_MGMT_ID | varchar(50) | NO |  | 링크관리ID |
| LINK_TOKEN_HASH | varchar(128) | NO |  | 링크토큰 해시(원문 저장 금지) |
| SITE_CD | varchar(50) | NO |  | 사업장코드 |
| CNT_BASE_DT | date | YES | NULL | 생성카운트 기준일(일별 제한용) |
| MAX_CREATE_CNT | int | NO | '1' | 생성가능 인원수 |
| CREATED_CNT | int | NO | '0' | 생성완료 인원수 |
| SMS_VERIFY_YN | varchar(2) | NO | 'N' | SMS 인증 사용여부 |
| EXPIRE_DTIME | datetime | YES | NULL | 링크 만료일시 |
| USE_YN | varchar(2) | NO | 'Y' | 사용여부 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`LINK_MGMT_ID`)`
- **UNIQUE**: `UNIQUE KEY `UK_DAILY_LINK_TOKEN_HASH` (`CMPNY_CD`,`LINK_TOKEN_HASH`)`
- **INDEX**: `KEY `IX_DAILY_LINK_SITE` (`CMPNY_CD`,`SITE_CD`)`
- **INDEX**: `KEY `IX_DAILY_LINK_EXPIRE` (`CMPNY_CD`,`USE_YN`,`EXPIRE_DTIME`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_daily_link_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `LINK_MGMT_ID` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '링크관리ID',
  `LINK_TOKEN_HASH` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '링크토큰 해시(원문 저장 금지)',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `CNT_BASE_DT` date DEFAULT NULL COMMENT '생성카운트 기준일(일별 제한용)',
  `MAX_CREATE_CNT` int NOT NULL DEFAULT '1' COMMENT '생성가능 인원수',
  `CREATED_CNT` int NOT NULL DEFAULT '0' COMMENT '생성완료 인원수',
  `SMS_VERIFY_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT 'SMS 인증 사용여부',
  `EXPIRE_DTIME` datetime DEFAULT NULL COMMENT '링크 만료일시',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`LINK_MGMT_ID`),
  UNIQUE KEY `UK_DAILY_LINK_TOKEN_HASH` (`CMPNY_CD`,`LINK_TOKEN_HASH`),
  KEY `IX_DAILY_LINK_SITE` (`CMPNY_CD`,`SITE_CD`),
  KEY `IX_DAILY_LINK_EXPIRE` (`CMPNY_CD`,`USE_YN`,`EXPIRE_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일용직 계정 생성 링크 관리';
```

</details>

### tb_daily_user_link_policy

**사업장별 일일계정 발급 정책**

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사 코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장 코드 |
| USE_YN | char(1) | NO | 'N' | 정책 사용 여부 |
| DAY_LIMIT_CNT | int | NO |  | 발급 허용 수(슬롯/정원) |
| INSERT_NO | varchar(50) | NO |  | 등록자 ID |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 등록 일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 ID |
| UPDATE_DATE | datetime | YES | NULL | 수정 일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_daily_user_link_policy` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '정책 사용 여부',
  `DAY_LIMIT_CNT` int NOT NULL COMMENT '발급 허용 수(슬롯/정원)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자 ID',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자 ID',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사업장별 일일계정 발급 정책';
```

</details>

## 점검 (체크포인트)

### tb_chkpt_type_mgmt

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 CHKLST_TYPE | varchar(10) | NO |  | 체크리스트 타입 |
| 🔑 CHKPT_CD | varchar(50) | NO |  | 체크포인트 코드 |
| CHKPT_NM | varchar(100) | YES | NULL | 체크포인트명 |
| CHKPT_DESC | varchar(500) | YES | NULL | 비고 |
| MGMT_USER_CD | varchar(20) | YES | NULL | 체크포인트 관리자ID |
| USE_YN | varchar(2) | YES | 'Y' | 사용여부 |
| INSERT_NO | varchar(50) | YES | NULL | 입력자 |
| INSERT_DATE | datetime | YES | NULL | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`CHKLST_TYPE`,`CHKPT_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_chkpt_type_mgmt` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `CHKLST_TYPE` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '체크리스트 타입',
  `CHKPT_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '체크포인트 코드',
  `CHKPT_NM` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '체크포인트명',
  `CHKPT_DESC` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `MGMT_USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '체크포인트 관리자ID',
  `USE_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT NULL COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`CHKLST_TYPE`,`CHKPT_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_chkpt_inspect_item

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 CHKLST_TYPE | varchar(10) | NO |  | 체크리스트 타입 |
| 🔑 INSPECT_ITEM_CD | varchar(20) | NO |  | 점검항목코드 |
| INSPECT_ITEM_SUBJ | varchar(200) | NO |  | 점검항목명칭 |
| SORT_IDX | int | YES | NULL | 정렬순서 |
| STR_DATE | varchar(6) | NO |  | 시행일자 |
| USE_YN | varchar(2) | YES | 'Y' | 사용유무 |
| INSERT_NO | varchar(50) | YES | NULL | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`CHKLST_TYPE`,`INSPECT_ITEM_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_chkpt_inspect_item` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `CHKLST_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '체크리스트 타입',
  `INSPECT_ITEM_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검항목코드',
  `INSPECT_ITEM_SUBJ` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검항목명칭',
  `SORT_IDX` int DEFAULT NULL COMMENT '정렬순서',
  `STR_DATE` varchar(6) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시행일자',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용유무',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`CHKLST_TYPE`,`INSPECT_ITEM_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_chkpt_inspect_answer

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  |  |
| 🔑 CHKPT_CD | varchar(50) | NO |  |  |
| 🔑 INSPECT_ITEM_CD | varchar(20) | NO |  | 점검항목코드 |
| 🔑 WORK_DATE | varchar(8) | NO |  | 점검일자 |
| INSPECT_ANSWER_TYPE | varchar(2) | NO |  | 점검답변타입[SYS009] |
| ANSWER_DESC | text | YES |  | 점검답변상세 |
| FILE_MGMT_CD | varchar(50) | YES | NULL | 첨부사진코드 |
| INSERT_NO | varchar(50) | YES | NULL | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`CHKPT_CD`,`INSPECT_ITEM_CD`,`WORK_DATE`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_chkpt_inspect_answer` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CHKPT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `INSPECT_ITEM_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검항목코드',
  `WORK_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검일자',
  `INSPECT_ANSWER_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검답변타입[SYS009]',
  `ANSWER_DESC` text COLLATE utf8mb4_unicode_ci COMMENT '점검답변상세',
  `FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '첨부사진코드',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`CHKPT_CD`,`INSPECT_ITEM_CD`,`WORK_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

## 위험성평가

### tb_risk_assessment

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 SITE_CD | varchar(50) | NO |  | 사업장코드 |
| 🔑 PROCESS_CD | varchar(10) | NO |  | 공정코드[COM002] |
| RISK_TYPE_CD | varchar(10) | NO |  | 위험요인구분코드 |
| HAZARD_CD | varchar(10) | YES | NULL | 유해요인코드 |
| 🔑 ASSESSMENT_CD | varchar(10) | NO |  | 평가코드 |
| ASSESSMENT_DESC | varchar(500) | YES | NULL | 유해요인 직접입력 |
| ASSESSMENT_STATUS | varchar(3) | NO |  | 진행상태[SYS011] |
| INIT_LIKELIHOOD_SCORE | int | YES | NULL | 초기평가 발생빈도 |
| INIT_SEVERITY_SCORE | int | YES | NULL | 초기평가 중대성 |
| INIT_RISK_LV | varchar(10) | YES | NULL | 초기평가 위험성LEVEL |
| INIT_DESC | varchar(500) | YES | NULL | 유해요인설명 |
| INIT_FILE_MGMT_CD | varchar(50) | YES | NULL | 유해요인사진 |
| INIT_ASSESSOR_ID | varchar(50) | YES | NULL | 초기평가자 |
| INIT_ASSESS_DATE | datetime | YES | NULL | 초기평가일시 |
| REVAL_DATE | varchar(8) | YES | NULL | 개선예정일 |
| REVAL_BEFORE_DESC | varchar(500) | YES | NULL | 개선전 임시조치 사항 |
| REVAL_LIKELIHOOD_SCORE | int | YES | NULL | 개선 후 발생빈도 |
| REVAL_SEVERITY_SCORE | int | YES | NULL | 개선 후 중대성 |
| REVAL_RISK_LV | varchar(10) | YES | NULL | 개선 후 평가 위험성LEVEL |
| REVAL_DESC | varchar(500) | YES | NULL | 개선내용 |
| REVAL_FILE_MGMT_CD | varchar(50) | YES | NULL | 개선사진 |
| REVAL_ASSESSOR_ID | varchar(50) | YES | NULL | 개선후평가자 |
| REVAL_ASSESS_DATE | datetime | YES | NULL | 개선완료일 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`PROCESS_CD`,`ASSESSMENT_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_risk_assessment` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `PROCESS_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '공정코드[COM002]',
  `RISK_TYPE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '위험요인구분코드',
  `HAZARD_CD` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '유해요인코드',
  `ASSESSMENT_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '평가코드',
  `ASSESSMENT_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '유해요인 직접입력',
  `ASSESSMENT_STATUS` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '진행상태[SYS011]',
  `INIT_LIKELIHOOD_SCORE` int DEFAULT NULL COMMENT '초기평가 발생빈도',
  `INIT_SEVERITY_SCORE` int DEFAULT NULL COMMENT '초기평가 중대성',
  `INIT_RISK_LV` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '초기평가 위험성LEVEL',
  `INIT_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '유해요인설명',
  `INIT_FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '유해요인사진',
  `INIT_ASSESSOR_ID` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '초기평가자',
  `INIT_ASSESS_DATE` datetime DEFAULT NULL COMMENT '초기평가일시',
  `REVAL_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선예정일',
  `REVAL_BEFORE_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선전 임시조치 사항',
  `REVAL_LIKELIHOOD_SCORE` int DEFAULT NULL COMMENT '개선 후 발생빈도',
  `REVAL_SEVERITY_SCORE` int DEFAULT NULL COMMENT '개선 후 중대성',
  `REVAL_RISK_LV` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선 후 평가 위험성LEVEL',
  `REVAL_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선내용',
  `REVAL_FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선사진',
  `REVAL_ASSESSOR_ID` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선후평가자',
  `REVAL_ASSESS_DATE` datetime DEFAULT NULL COMMENT '개선완료일',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`PROCESS_CD`,`ASSESSMENT_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_risk_type

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 RISK_TYPE_CD | varchar(10) | NO |  | 위험요인구분코드 |
| RISK_TYPE_NM | varchar(100) | NO |  | 위험요인구분명 |
| SITE_CD | varchar(50) | YES | NULL | 사업장코드 |
| PROCESS_CD | varchar(10) | NO |  | 공정코드[COM002] |
| USE_YN | varchar(2) | NO |  | 사용여부 |
| RISK_TYPE_DESC | varchar(500) | YES | NULL | 위험요인비고 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`RISK_TYPE_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_risk_type` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `RISK_TYPE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '위험요인구분코드',
  `RISK_TYPE_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '위험요인구분명',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장코드',
  `PROCESS_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '공정코드[COM002]',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용여부',
  `RISK_TYPE_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '위험요인비고',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`RISK_TYPE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_risk_site_hazard

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 RISK_TYPE_CD | varchar(10) | NO |  | 위험요인구분코드 |
| 🔑 HAZARD_CD | varchar(10) | NO |  | 유해요인코드 |
| HAZARD_NM | varchar(100) | NO |  | 유해요인명 |
| SITE_CD | varchar(50) | YES | NULL | 사업장코드 |
| HAZARD_DESC | varchar(500) | YES | NULL | 유해요인설명 |
| USE_YN | varchar(2) | YES | 'Y' |  |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`RISK_TYPE_CD`,`HAZARD_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_risk_site_hazard` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `RISK_TYPE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '위험요인구분코드',
  `HAZARD_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '유해요인코드',
  `HAZARD_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '유해요인명',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장코드',
  `HAZARD_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '유해요인설명',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`RISK_TYPE_CD`,`HAZARD_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

## TBM 교육

### tb_tbm_edu_mtrl

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 MTRL_CD | varchar(20) | NO |  | 교육자료 코드 |
| CMPNY_CD | varchar(10) | NO |  | 회사코드 |
| TITLE | varchar(200) | NO |  | 교육자로 제목 |
| CONTENTS | varchar(500) | YES | NULL | 교육자료 설명 |
| MTRL_TYPE | varchar(8) | YES | NULL | 교육자로 타입 |
| USE_YN | char(1) | NO | 'Y' | 사용유무 |
| INSERT_NO | varchar(50) | NO |  | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | NO |  | 수정자 |
| UPDATE_DATE | datetime | NO | CURRENT_TIMESTAMP | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`MTRL_CD`)`
- **INDEX**: `KEY `IX_TBM_EDU_MTRL_01` (`CMPNY_CD`,`USE_YN`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_tbm_edu_mtrl` (
  `MTRL_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교육자료 코드',
  `CMPNY_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `TITLE` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교육자로 제목',
  `CONTENTS` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '교육자료 설명',
  `MTRL_TYPE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '교육자로 타입',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용유무',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`MTRL_CD`),
  KEY `IX_TBM_EDU_MTRL_01` (`CMPNY_CD`,`USE_YN`),
  CONSTRAINT `tb_tbm_edu_mtrl_chk_1` CHECK ((`USE_YN` in (_utf8mb4'Y',_utf8mb4'N')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_tbm_edu_mtrl_item

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 MTRL_ITEM_CD | varchar(20) | NO |  | 교육자료 항목 코드 |
| MTRL_CD | varchar(20) | NO |  | 교육자료 코드 |
| SORT_IDX | int | NO | '1' | 정렬순서 |
| MTRL_ITEM_TYPE | varchar(2) | NO |  | 교육자료 항목 타입 |
| MTRL_DESC | varchar(500) | YES | NULL | 교육자료 항목 설명 |
| FILE_MGMT_CD | varchar(40) | YES | NULL | 파일코드 |
| URL | varchar(1000) | YES | NULL | 외부링크 |
| USE_YN | char(1) | NO | 'Y' | 사용여부[SYS003] |
| INSERT_NO | varchar(50) | NO |  | 입력자 |
| INSERT_DATE | datetime | NO | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | NO |  | 수정자 |
| UPDATE_DATE | datetime | NO | CURRENT_TIMESTAMP | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`MTRL_ITEM_CD`)`
- **INDEX**: `KEY `IX_TBM_EDU_MTRL_ITEM_01` (`MTRL_CD`,`USE_YN`,`SORT_IDX`)`
- **INDEX**: `KEY `IX_TBM_EDU_MTRL_ITEM_02` (`MTRL_CD`,`SORT_IDX`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_tbm_edu_mtrl_item` (
  `MTRL_ITEM_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교육자료 항목 코드',
  `MTRL_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교육자료 코드',
  `SORT_IDX` int NOT NULL DEFAULT '1' COMMENT '정렬순서',
  `MTRL_ITEM_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교육자료 항목 타입',
  `MTRL_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '교육자료 항목 설명',
  `FILE_MGMT_CD` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '파일코드',
  `URL` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '외부링크',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`MTRL_ITEM_CD`),
  KEY `IX_TBM_EDU_MTRL_ITEM_01` (`MTRL_CD`,`USE_YN`,`SORT_IDX`),
  KEY `IX_TBM_EDU_MTRL_ITEM_02` (`MTRL_CD`,`SORT_IDX`),
  CONSTRAINT `FK_TBM_EDU_MTRL_ITEM_01` FOREIGN KEY (`MTRL_CD`) REFERENCES `tb_tbm_edu_mtrl` (`MTRL_CD`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

## 시스템 - 메뉴 / 공통코드

### tb_syst_menu_m

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 MENU_M_ID | varchar(10) | NO |  | 대메뉴ID |
| MENU_SRC | varchar(3) | NO |  | 메뉴사용처[SYS007] |
| MENU_NM | varchar(50) | NO |  | 메뉴명 |
| MENU_IDX | int | YES | NULL | 메뉴순번 |
| MENU_DESC | varchar(200) | YES | NULL | 비고 |
| USE_YN | varchar(50) | YES | 'Y' | 사용여부 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`MENU_M_ID`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_syst_menu_m` (
  `MENU_M_ID` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대메뉴ID',
  `MENU_SRC` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '메뉴사용처[SYS007]',
  `MENU_NM` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '메뉴명',
  `MENU_IDX` int DEFAULT NULL COMMENT '메뉴순번',
  `MENU_DESC` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `USE_YN` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`MENU_M_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_syst_menu_d

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 MENU_D_ID | varchar(50) | NO |  |  |
| 🔑 MENU_M_ID | varchar(10) | NO |  | 대메뉴ID |
| MENU_VIEW | varchar(50) | YES | NULL |  |
| MENU_NM | varchar(50) | NO |  | 메뉴명 |
| MENU_IDX | int | YES | NULL | 메뉴순번 |
| MENU_DESC | varchar(200) | YES | NULL | 비고 |
| USE_YN | varchar(50) | YES | 'Y' | 사용여부 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`MENU_D_ID`,`MENU_M_ID`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_syst_menu_d` (
  `MENU_D_ID` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `MENU_M_ID` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대메뉴ID',
  `MENU_VIEW` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `MENU_NM` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '메뉴명',
  `MENU_IDX` int DEFAULT NULL COMMENT '메뉴순번',
  `MENU_DESC` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `USE_YN` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`MENU_D_ID`,`MENU_M_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_syst_auth_menu

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 AUTH_CD | varchar(10) | NO |  | 권한코드 |
| 🔑 MENU_D_ID | varchar(50) | NO |  |  |
| USE_YN | varchar(2) | YES | NULL |  |
| BTN_SRCH | varchar(2) | YES | 'Y' | 조회권한 |
| BTN_NEW | varchar(2) | YES | 'Y' | 신규권한 |
| BTN_DELT | varchar(2) | YES | 'Y' | 삭제권한 |
| BTN_SAVE | varchar(2) | YES | 'Y' | 저장권한 |
| BTN_EXCL | varchar(2) | YES | 'Y' | 엑셀권한 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`AUTH_CD`,`MENU_D_ID`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_syst_auth_menu` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `AUTH_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '권한코드',
  `MENU_D_ID` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BTN_SRCH` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '조회권한',
  `BTN_NEW` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '신규권한',
  `BTN_DELT` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '삭제권한',
  `BTN_SAVE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '저장권한',
  `BTN_EXCL` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '엑셀권한',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`AUTH_CD`,`MENU_D_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_syst_val_m

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 SYST_VAL_CD | varchar(50) | NO |  | 시스템변수코드 |
| SYST_VAL_NM | varchar(100) | NO |  | 시스템변수이름 |
| USE_YN | varchar(2) | YES | 'Y' | 사용여부 |
| VAL_INFO_1 | varchar(50) | YES | NULL | 시스템변수 정보1 |
| VAL_INFO_2 | varchar(50) | YES | NULL | 시스템변수 정보2 |
| VAL_DESC | varchar(500) | YES | NULL | 시스템변수 설명 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' |  |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP |  |
| UPDATE_NO | varchar(50) | YES | NULL |  |
| UPDATE_DATE | date | YES | NULL |  |

키/인덱스:

- **PK**: `PRIMARY KEY (`SYST_VAL_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_syst_val_m` (
  `SYST_VAL_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시스템변수코드',
  `SYST_VAL_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시스템변수이름',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `VAL_INFO_1` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 정보1',
  `VAL_INFO_2` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 정보2',
  `VAL_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 설명',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` date DEFAULT NULL,
  PRIMARY KEY (`SYST_VAL_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_syst_val_d

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 SYST_VAL_CD | varchar(50) | NO |  | 시스템변수코드 |
| 🔑 SYST_VAL_D_CD | varchar(50) | NO |  | 시스템변수코드 |
| SYST_VAL_D_NM | varchar(100) | NO |  | 시스템변수이름 |
| SORT_IDX | int | YES | NULL | 순번 |
| USE_YN | varchar(2) | YES | 'Y' | 사용여부 |
| VAL_D_INFO_1 | varchar(50) | YES | NULL | 시스템변수 정보1 |
| VAL_D_INFO_2 | varchar(50) | YES | NULL | 시스템변수 정보2 |
| VAL_D_DESC | varchar(500) | YES | NULL | 시스템변수 설명 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' |  |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP |  |
| UPDATE_NO | varchar(50) | YES | NULL |  |
| UPDATE_DATE | date | YES | NULL |  |

키/인덱스:

- **PK**: `PRIMARY KEY (`SYST_VAL_CD`,`SYST_VAL_D_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_syst_val_d` (
  `SYST_VAL_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시스템변수코드',
  `SYST_VAL_D_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시스템변수코드',
  `SYST_VAL_D_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시스템변수이름',
  `SORT_IDX` int DEFAULT NULL COMMENT '순번',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `VAL_D_INFO_1` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 정보1',
  `VAL_D_INFO_2` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 정보2',
  `VAL_D_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 설명',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` date DEFAULT NULL,
  PRIMARY KEY (`SYST_VAL_CD`,`SYST_VAL_D_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

## 운영사 변수

### tb_baim_val_m

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 BAIM_VAL_CD | varchar(50) | NO |  | 운영사변수코드 |
| BAIM_VAL_NM | varchar(100) | NO |  | 운영사변수이름 |
| USE_YN | varchar(2) | YES | 'Y' | 사용여부 |
| VAL_INFO_1 | varchar(50) | YES | NULL | 운영사변수 정보1 |
| VAL_INFO_2 | varchar(50) | YES | NULL | 운영사변수 정보2 |
| VAL_DESC | varchar(500) | YES | NULL | 운영사변수 비고 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' |  |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP |  |
| UPDATE_NO | varchar(50) | YES | NULL |  |
| UPDATE_DATE | date | YES | NULL |  |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`BAIM_VAL_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_baim_val_m` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `BAIM_VAL_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '운영사변수코드',
  `BAIM_VAL_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '운영사변수이름',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `VAL_INFO_1` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사변수 정보1',
  `VAL_INFO_2` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사변수 정보2',
  `VAL_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사변수 비고',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` date DEFAULT NULL,
  PRIMARY KEY (`CMPNY_CD`,`BAIM_VAL_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_baim_val_d

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 BAIM_VAL_CD | varchar(50) | NO |  | 운영사변수코드 |
| 🔑 BAIM_VAL_D_CD | varchar(50) | NO |  | 운영사상세변수코드 |
| BAIM_VAL_D_NM | varchar(100) | NO |  | 운영사상세변수이름 |
| SORT_IDX | int | YES | NULL | 순번 |
| USE_YN | varchar(2) | YES | 'Y' | 사용여부 |
| VAL_D_INFO_1 | varchar(50) | YES | NULL | 운영사상세변수 정보1 |
| VAL_D_INFO_2 | varchar(50) | YES | NULL | 운영사상세변수 정보2 |
| VAL_D_DESC | varchar(500) | YES | NULL | 운영사상세변수 설명 |
| INSERT_NO | varchar(50) | YES | 'SYSTEM' |  |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP |  |
| UPDATE_NO | varchar(50) | YES | NULL |  |
| UPDATE_DATE | date | YES | NULL |  |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`BAIM_VAL_CD`,`BAIM_VAL_D_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_baim_val_d` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `BAIM_VAL_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '운영사변수코드',
  `BAIM_VAL_D_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '운영사상세변수코드',
  `BAIM_VAL_D_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '운영사상세변수이름',
  `SORT_IDX` int DEFAULT NULL COMMENT '순번',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `VAL_D_INFO_1` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사상세변수 정보1',
  `VAL_D_INFO_2` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사상세변수 정보2',
  `VAL_D_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사상세변수 설명',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` date DEFAULT NULL,
  PRIMARY KEY (`CMPNY_CD`,`BAIM_VAL_CD`,`BAIM_VAL_D_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

## 공통 / 시퀀스 / 파일

### tb_cmm_seq

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  |  |
| 🔑 SEQ_KEY | varchar(50) | NO |  |  |
| CURR_VAL | int | NO | '0' |  |
| MAX_VAL | int | NO | '99999' |  |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`SEQ_KEY`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_cmm_seq` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SEQ_KEY` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CURR_VAL` int NOT NULL DEFAULT '0',
  `MAX_VAL` int NOT NULL DEFAULT '99999',
  PRIMARY KEY (`CMPNY_CD`,`SEQ_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### seq_site_cd

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 DATE_PREFIX | varchar(8) | NO |  |  |
| LAST_SEQ | int | YES | '0' |  |

키/인덱스:

- **PK**: `PRIMARY KEY (`DATE_PREFIX`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `seq_site_cd` (
  `DATE_PREFIX` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `LAST_SEQ` int DEFAULT '0',
  PRIMARY KEY (`DATE_PREFIX`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>

### tb_file_info

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| 🔑 CMPNY_CD | varchar(50) | NO |  | 회사코드 |
| 🔑 FILE_MGMT_CD | varchar(50) | NO |  | 파일코드 |
| FILE_NM | varchar(500) | YES | NULL | 파일명 |
| FILE_TYPE | varchar(3) | NO |  | 파일타입[SYS010] |
| FILE_PATH | varchar(500) | NO |  | 파일저장경로 |
| FILE_EXT | varchar(10) | NO |  | 파일확장자 |
| INSERT_NO | varchar(50) | YES | NULL | 입력자 |
| INSERT_DATE | datetime | YES | CURRENT_TIMESTAMP | 입력일시 |
| UPDATE_NO | varchar(50) | YES | NULL | 수정자 |
| UPDATE_DATE | datetime | YES | NULL | 수정일시 |

키/인덱스:

- **PK**: `PRIMARY KEY (`CMPNY_CD`,`FILE_MGMT_CD`)`

<details><summary>원본 DDL</summary>

```sql
CREATE TABLE `tb_file_info` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '파일코드',
  `FILE_NM` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '파일명',
  `FILE_TYPE` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '파일타입[SYS010]',
  `FILE_PATH` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '파일저장경로',
  `FILE_EXT` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '파일확장자',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`FILE_MGMT_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

</details>
