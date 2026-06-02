# PRAFTA · DB 테이블 전체 DDL

> MySQL 8.0.42 · `INFORMATION_SCHEMA` 실시간 스냅샷 기반 재구성. 컬럼 정의·PK·인덱스·코멘트 포함.
> 총 **76개 테이블**. 각 테이블은 (1) 한눈에 보는 컬럼 표 + (2) CREATE TABLE DDL 로 구성.

---

## 목차 (도메인별)

### 1. 시스템·공통 (코드/메뉴/권한/약관/알림/회사)
- [`seq_site_cd`](#seqsitecd)
- [`tb_audit_log`](#tbauditlog) — 감사 로그 (다운로드/권한 변경/상태 변경 등)
- [`tb_auth_token`](#tbauthtoken) — 인증 토큰 (refresh 등)
- [`tb_baim_val_d`](#tbbaimvald) — 사업장 공통코드 상세
- [`tb_baim_val_m`](#tbbaimvalm) — 사업장 공통코드 마스터
- [`tb_cmm_seq`](#tbcmmseq) — 공통 채번 시퀀스
- [`tb_cmpny`](#tbcmpny) — 회사 마스터
- [`tb_file_info`](#tbfileinfo) — 파일 정보 (첨부/증빙)
- [`tb_holiday`](#tbholiday) — 휴일관리
- [`tb_holiday_rule`](#tbholidayrule) — 휴일규칙(매년고정 회사휴일)
- [`tb_noti_outbox`](#tbnotioutbox) — 푸시 알림 outbox (발송 대기/이력)
- [`tb_sms_auth_code`](#tbsmsauthcode) — SMS 인증코드
- [`tb_syst_auth_menu`](#tbsystauthmenu) — 권한별 메뉴 접근 제어 (Role x Menu x CRUD x Platform)
- [`tb_syst_menu_d`](#tbsystmenud) — 시스템 메뉴 상세
- [`tb_syst_menu_m`](#tbsystmenum) — 시스템 메뉴 마스터
- [`tb_syst_val_d`](#tbsystvald) — 시스템 공통코드 상세 (SYS 코드값)
- [`tb_syst_val_m`](#tbsystvalm) — 시스템 공통코드 마스터 (SYS 코드그룹)
- [`tb_terms`](#tbterms) — 이용약관
- [`tb_terms_id_version`](#tbtermsidversion) — 약관 버전
- [`tb_terms_user_agr_mgmt`](#tbtermsuseragrmgmt) — 사용자 약관 동의 관리

### 2. 사업장·조직·권한
- [`tb_site`](#tbsite) — 사업장 마스터
- [`tb_site_node`](#tbsitenode) — 조직도 노드 (사업장 하위 트리)
- [`tb_user_site_auth`](#tbusersiteauth) — 사용자 사업장 권한 매핑

### 3. 사용자·계정
- [`tb_del_user`](#tbdeluser) — 탈퇴 사용자 보관
- [`tb_user`](#tbuser) — 사용자
- [`tb_user_device`](#tbuserdevice) — 정규직 디바이스 점유 (글로벌 유니크 - 회사 가로질러 1디바이스=1계정)

### 4. 일일계정·슬롯
- [`tb_daily_link_mgmt`](#tbdailylinkmgmt) — 일용직 계정 생성 링크 관리
- [`tb_daily_user`](#tbdailyuser) — 일용직 사용자
- [`tb_daily_user_link_policy`](#tbdailyuserlinkpolicy) — 사업장별 일일계정 발급 정책
- [`tb_daily_user_slot`](#tbdailyuserslot) — 일일계정 슬롯(현재 점유 상태만 관리)
- [`tb_daily_user_slot_his`](#tbdailyuserslothis) — 일일계정 슬롯 사용 이력

### 5. 근무타입·교대·근무계획
- [`tb_sch_mgmt`](#tbschmgmt) — 사업장 근무타입 관리
- [`tb_sch_mgmt_hist`](#tbschmgmthist) — 사업장 근무타입 이력관리
- [`tb_shift_sch_assign_mgmt`](#tbshiftschassignmgmt) — 교대 스케줄 배정 관리
- [`tb_shift_sch_mgmt`](#tbshiftschmgmt) — 교대 스케줄 관리
- [`tb_shift_sch_ptrn_mgmt`](#tbshiftschptrnmgmt) — 교대 스케줄 패턴 관리
- [`tb_shift_sch_team_meta_info`](#tbshiftschteammetainfo) — 교대근무 팀 메타 정보
- [`tb_shift_sch_team_mgmt`](#tbshiftschteammgmt) — 교대근무 팀 관리
- [`tb_shift_sch_team_user`](#tbshiftschteamuser) — 교대근무 팀 소속 사용자 관리
- [`tb_user_work_plan`](#tbuserworkplan) — 사용자 근무 계획

### 6. 근태(출퇴근/정산/마감/요청)
- [`tb_attd_close`](#tbattdclose) — 근태 마감 상태 (회사+사업장+월)
- [`tb_attd_close_hist`](#tbattdclosehist) — 근태 마감/해제 이력
- [`tb_attd_std_time_rule`](#tbattdstdtimerule) — 출퇴근 시간 표준화 규칙
- [`tb_attd_std_time_rule_his`](#tbattdstdtimerulehis) — 출퇴근 시간 표준화 규칙 변경 이력
- [`tb_user_attd_gps`](#tbuserattdgps) — 근태 GPS 기록
- [`tb_user_attd_hist`](#tbuserattdhist) — 근태 처리 이력
- [`tb_user_attd_mgmt`](#tbuserattdmgmt) — 근태관리 (출퇴근 원장/정산)
- [`tb_user_attd_req`](#tbuserattdreq) — 사용자 근태 관련 요청 관리
- [`tb_user_attd_req_approval`](#tbuserattdreqapproval) — 연차 요청별 결재라인 (사용자 정의)
- [`tb_user_overtime_mgmt`](#tbuserovertimemgmt) — 사용자 초과근무 실적 관리

### 7. 연차·휴가·결재라인
- [`tb_aprv_line_preset`](#tbaprvlinepreset) — 연차 결재라인 프리셋 (사용자별 마스터)
- [`tb_aprv_line_preset_d`](#tbaprvlinepresetd) — 연차 결재라인 프리셋 디테일 (결재 순서)
- [`tb_leave_policy`](#tbleavepolicy) — 회사 법정 연차 부여 정책 (7개 axis)
- [`tb_leave_policy_history`](#tbleavepolicyhistory) — 연차 정책 변경 이력
- [`tb_leave_type_mgmt`](#tbleavetypemgmt) — 연차(휴가) 타입 관리
- [`tb_leave_usage_policy`](#tbleaveusagepolicy) — 연차 사용 단위 정책 (휴게시간/시간단위 시작시각/1일환산은 시스템 강제)
- [`tb_user_hire_date_history`](#tbuserhiredatehistory) — 입사일 변경 이력 (노무 감사용)
- [`tb_user_leave_grant`](#tbuserleavegrant) — 사용자 연차 부여 이력
- [`tb_user_leave_use`](#tbuserleaveuse) — 사용자 연차 사용 실적
- [`tb_user_service_credit`](#tbuserservicecredit) — 사용자 경력 인정 (점진 부여 전용)

### 8. TBM(작업 전 안전미팅)
- [`tb_tbm_attendance`](#tbtbmattendance) — TBM 출결(정규직/일용직 통합)
- [`tb_tbm_attendance_event`](#tbtbmattendanceevent) — TBM 출결 이벤트 로그
- [`tb_tbm_edu_mtrl`](#tbtbmedumtrl) — TBM 교육자료
- [`tb_tbm_edu_mtrl_item`](#tbtbmedumtrlitem) — TBM 교육자료 항목
- [`tb_tbm_pwd_fail`](#tbtbmpwdfail) — TBM 비밀번호 실패 로그
- [`tb_tbm_session`](#tbtbmsession) — TBM 세션
- [`tb_tbm_session_content`](#tbtbmsessioncontent) — TBM 세션-콘텐츠 묶음 매핑
- [`tb_tbm_session_risk`](#tbtbmsessionrisk) — TBM 세션-위험성평가 매핑
- [`tb_tbm_session_state`](#tbtbmsessionstate) — TBM 세션 실시간 동기화 상태(UPSERT)

### 9. 산업안전(위험성평가/안전점검/아차사고)
- [`tb_chkpt_inspect_answer`](#tbchkptinspectanswer) — 안전점검 점검 답변
- [`tb_chkpt_inspect_item`](#tbchkptinspectitem) — 안전점검 점검 항목
- [`tb_chkpt_type_mgmt`](#tbchkpttypemgmt) — 안전점검 체크포인트 유형 관리
- [`tb_near_miss`](#tbnearmiss) — 아차사고/사건 보고
- [`tb_risk_assessment`](#tbriskassessment) — 위험성평가
- [`tb_risk_site_hazard`](#tbrisksitehazard) — 사업장 유해위험요인
- [`tb_risk_type`](#tbrisktype) — 위험성평가 유형

---


# 1. 시스템·공통 (코드/메뉴/권한/약관/알림/회사)

<a id="seqsitecd"></a>
## `seq_site_cd`

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `DATE_PREFIX` | varchar(8) | — | PK |  |  |
| `LAST_SEQ` | int | Y |  | `0` |  |

```sql
CREATE TABLE `seq_site_cd` (
  `DATE_PREFIX` varchar(8) NOT NULL,
  `LAST_SEQ` int DEFAULT 0,
  PRIMARY KEY (`DATE_PREFIX`)
);
```

<a id="tbauditlog"></a>
## `tb_audit_log` — 감사 로그 (다운로드/권한 변경/상태 변경 등)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `AUDIT_ID` | varchar(25) | — | PK |  | 감사 로그 ID (PK, 회사별 채번: A + YYYYMMDD + SEQ) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사 코드 |
| `USER_CD` | varchar(20) | Y |  |  | 행위자 사용자 코드(비로그인 행위는 NULL) |
| `ACTION_TYPE` | varchar(30) | — |  |  | 감사 액션 유형[SYS060] 01:다운로드 |
| `RESOURCE_TYPE` | varchar(50) | — |  |  | 대상 리소스 유형 (예: USER_CREATE_TEMPLATE) |
| `RESOURCE_KEY` | varchar(200) | Y |  |  | 대상 리소스 식별자(양식 다운로드는 NULL) |
| `IP_ADDRESS` | varchar(45) | Y |  |  | 요청 IP (IPv6 지원, 추출 실패 시 NULL) |
| `USER_AGENT` | varchar(500) | Y |  |  | 요청 User-Agent |
| `DETAIL` | json | Y |  |  | 추가 페이로드(JSON, PII 평문 금지) |
| `DEL_YN` | varchar(1) | — |  | `N` | 삭제 여부(감사는 무삭제 원칙) |
| `INSERT_NO` | varchar(50) | — |  |  | 등록자(=USER_CD or SYSTEM) |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 등록 일시 |

```sql
CREATE TABLE `tb_audit_log` (
  `AUDIT_ID` varchar(25) NOT NULL COMMENT '감사 로그 ID (PK, 회사별 채번: A + YYYYMMDD + SEQ)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) COMMENT '행위자 사용자 코드(비로그인 행위는 NULL)',
  `ACTION_TYPE` varchar(30) NOT NULL COMMENT '감사 액션 유형[SYS060] 01:다운로드',
  `RESOURCE_TYPE` varchar(50) NOT NULL COMMENT '대상 리소스 유형 (예: USER_CREATE_TEMPLATE)',
  `RESOURCE_KEY` varchar(200) COMMENT '대상 리소스 식별자(양식 다운로드는 NULL)',
  `IP_ADDRESS` varchar(45) COMMENT '요청 IP (IPv6 지원, 추출 실패 시 NULL)',
  `USER_AGENT` varchar(500) COMMENT '요청 User-Agent',
  `DETAIL` json COMMENT '추가 페이로드(JSON, PII 평문 금지)',
  `DEL_YN` varchar(1) NOT NULL DEFAULT 'N' COMMENT '삭제 여부(감사는 무삭제 원칙)',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '등록자(=USER_CD or SYSTEM)',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  PRIMARY KEY (`AUDIT_ID`),
  KEY `IX_AUDIT_LOG_ACTION` (`CMPNY_CD`, `ACTION_TYPE`, `INSERT_DATE`),
  KEY `IX_AUDIT_LOG_RESOURCE` (`CMPNY_CD`, `RESOURCE_TYPE`, `INSERT_DATE`),
  KEY `IX_AUDIT_LOG_TIME` (`CMPNY_CD`, `INSERT_DATE`),
  KEY `IX_AUDIT_LOG_USER` (`CMPNY_CD`, `USER_CD`, `INSERT_DATE`)
) COMMENT='감사 로그 (다운로드/권한 변경/상태 변경 등)';
```

<a id="tbauthtoken"></a>
## `tb_auth_token` — 인증 토큰 (refresh 등)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `USER_CD` | varchar(50) | — | PK |  | 사용자코드 |
| `TOKEN_ID` | varchar(50) | — | PK |  | 토큰(세션) 식별자 |
| `CLIENT_TYPE` | varchar(10) | — |  |  | WEB/APP |
| `DEVICE_ID` | varchar(100) | Y |  |  | 앱 디바이스 식별(가능하면) |
| `REFRESH_TOKEN_HASH` | varchar(128) | — | UQ |  | 리프레시 토큰 해시 |
| `ISSUED_DTIME` | datetime | — |  | `CURRENT_TIMESTAMP` |  |
| `EXPIRE_DTIME` | datetime | — | IDX |  | 리프레시 토큰 만료 |
| `REVOKED_YN` | varchar(2) | — |  | `N` |  |
| `REVOKED_DTIME` | datetime | Y |  |  |  |
| `IP_ADDR` | varchar(45) | Y |  |  |  |
| `USER_AGENT` | varchar(255) | Y |  |  |  |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` |  |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` |  |
| `UPDATE_NO` | varchar(50) | Y |  |  |  |
| `UPDATE_DATE` | datetime | Y |  |  |  |

```sql
CREATE TABLE `tb_auth_token` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `USER_CD` varchar(50) NOT NULL COMMENT '사용자코드',
  `TOKEN_ID` varchar(50) NOT NULL COMMENT '토큰(세션) 식별자',
  `CLIENT_TYPE` varchar(10) NOT NULL COMMENT 'WEB/APP',
  `DEVICE_ID` varchar(100) COMMENT '앱 디바이스 식별(가능하면)',
  `REFRESH_TOKEN_HASH` varchar(128) NOT NULL COMMENT '리프레시 토큰 해시',
  `ISSUED_DTIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `EXPIRE_DTIME` datetime NOT NULL COMMENT '리프레시 토큰 만료',
  `REVOKED_YN` varchar(2) NOT NULL DEFAULT 'N',
  `REVOKED_DTIME` datetime,
  `IP_ADDR` varchar(45),
  `USER_AGENT` varchar(255),
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50),
  `UPDATE_DATE` datetime,
  PRIMARY KEY (`CMPNY_CD`, `USER_CD`, `TOKEN_ID`),
  KEY `IX_TOKEN_EXPIRE` (`EXPIRE_DTIME`),
  KEY `IX_TOKEN_USER` (`CMPNY_CD`, `USER_CD`, `CLIENT_TYPE`, `REVOKED_YN`),
  UNIQUE KEY `UX_AUTH_TOKEN_RTH` (`REFRESH_TOKEN_HASH`)
) COMMENT='인증 토큰 (refresh 등)';
```

<a id="tbbaimvald"></a>
## `tb_baim_val_d` — 사업장 공통코드 상세

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `BAIM_VAL_CD` | varchar(50) | — | PK |  | 운영사변수코드 |
| `BAIM_VAL_D_CD` | varchar(50) | — | PK |  | 운영사상세변수코드 |
| `BAIM_VAL_D_NM` | varchar(100) | — |  |  | 운영사상세변수이름 |
| `SORT_IDX` | int | Y |  |  | 순번 |
| `USE_YN` | varchar(2) | Y |  | `Y` | 사용여부 |
| `VAL_D_INFO_1` | varchar(50) | Y |  |  | 운영사상세변수 정보1 |
| `VAL_D_INFO_2` | varchar(50) | Y |  |  | 운영사상세변수 정보2 |
| `VAL_D_DESC` | varchar(500) | Y |  |  | 운영사상세변수 설명 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` |  |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` |  |
| `UPDATE_NO` | varchar(50) | Y |  |  |  |
| `UPDATE_DATE` | date | Y |  |  |  |

```sql
CREATE TABLE `tb_baim_val_d` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `BAIM_VAL_CD` varchar(50) NOT NULL COMMENT '운영사변수코드',
  `BAIM_VAL_D_CD` varchar(50) NOT NULL COMMENT '운영사상세변수코드',
  `BAIM_VAL_D_NM` varchar(100) NOT NULL COMMENT '운영사상세변수이름',
  `SORT_IDX` int COMMENT '순번',
  `USE_YN` varchar(2) DEFAULT 'Y' COMMENT '사용여부',
  `VAL_D_INFO_1` varchar(50) COMMENT '운영사상세변수 정보1',
  `VAL_D_INFO_2` varchar(50) COMMENT '운영사상세변수 정보2',
  `VAL_D_DESC` varchar(500) COMMENT '운영사상세변수 설명',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50),
  `UPDATE_DATE` date,
  PRIMARY KEY (`CMPNY_CD`, `BAIM_VAL_CD`, `BAIM_VAL_D_CD`)
) COMMENT='사업장 공통코드 상세';
```

<a id="tbbaimvalm"></a>
## `tb_baim_val_m` — 사업장 공통코드 마스터

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `BAIM_VAL_CD` | varchar(50) | — | PK |  | 운영사변수코드 |
| `BAIM_VAL_NM` | varchar(100) | — |  |  | 운영사변수이름 |
| `USE_YN` | varchar(2) | Y |  | `Y` | 사용여부 |
| `VAL_INFO_1` | varchar(50) | Y |  |  | 운영사변수 정보1 |
| `VAL_INFO_2` | varchar(50) | Y |  |  | 운영사변수 정보2 |
| `VAL_DESC` | varchar(500) | Y |  |  | 운영사변수 비고 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` |  |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` |  |
| `UPDATE_NO` | varchar(50) | Y |  |  |  |
| `UPDATE_DATE` | date | Y |  |  |  |

```sql
CREATE TABLE `tb_baim_val_m` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `BAIM_VAL_CD` varchar(50) NOT NULL COMMENT '운영사변수코드',
  `BAIM_VAL_NM` varchar(100) NOT NULL COMMENT '운영사변수이름',
  `USE_YN` varchar(2) DEFAULT 'Y' COMMENT '사용여부',
  `VAL_INFO_1` varchar(50) COMMENT '운영사변수 정보1',
  `VAL_INFO_2` varchar(50) COMMENT '운영사변수 정보2',
  `VAL_DESC` varchar(500) COMMENT '운영사변수 비고',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50),
  `UPDATE_DATE` date,
  PRIMARY KEY (`CMPNY_CD`, `BAIM_VAL_CD`)
) COMMENT='사업장 공통코드 마스터';
```

<a id="tbcmmseq"></a>
## `tb_cmm_seq` — 공통 채번 시퀀스

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  |  |
| `SEQ_KEY` | varchar(50) | — | PK |  |  |
| `CURR_VAL` | int | — |  | `0` |  |
| `MAX_VAL` | int | — |  | `99999` |  |

```sql
CREATE TABLE `tb_cmm_seq` (
  `CMPNY_CD` varchar(50) NOT NULL,
  `SEQ_KEY` varchar(50) NOT NULL,
  `CURR_VAL` int NOT NULL DEFAULT 0,
  `MAX_VAL` int NOT NULL DEFAULT 99999,
  PRIMARY KEY (`CMPNY_CD`, `SEQ_KEY`)
) COMMENT='공통 채번 시퀀스';
```

<a id="tbcmpny"></a>
## `tb_cmpny` — 회사 마스터

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `CMPNY_NM` | varchar(50) | — |  |  | 회사명 |
| `BSNS_LCN_NO` | varchar(50) | — |  |  | 사업자번호 |
| `ADDR_1` | varchar(100) | Y |  |  | 주소 |
| `ADDR_2` | varchar(200) | Y |  |  | 상세주소 |
| `ZIP_CODE` | varchar(50) | Y |  |  | 우편번호 |
| `USE_YN` | varchar(2) | Y |  |  | 사용여부 |
| `CONTRACT_YN` | varchar(2) | Y |  |  | 계약여부 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_cmpny` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `CMPNY_NM` varchar(50) NOT NULL COMMENT '회사명',
  `BSNS_LCN_NO` varchar(50) NOT NULL COMMENT '사업자번호',
  `ADDR_1` varchar(100) COMMENT '주소',
  `ADDR_2` varchar(200) COMMENT '상세주소',
  `ZIP_CODE` varchar(50) COMMENT '우편번호',
  `USE_YN` varchar(2) COMMENT '사용여부',
  `CONTRACT_YN` varchar(2) COMMENT '계약여부',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`)
) COMMENT='회사 마스터';
```

<a id="tbfileinfo"></a>
## `tb_file_info` — 파일 정보 (첨부/증빙)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `FILE_MGMT_CD` | varchar(50) | — | PK |  | 파일코드 |
| `FILE_NM` | varchar(500) | Y |  |  | 파일명 |
| `FILE_TYPE` | varchar(3) | — |  |  | 파일타입[SYS010] |
| `FILE_PATH` | varchar(500) | — |  |  | 파일저장경로 |
| `FILE_EXT` | varchar(10) | — |  |  | 파일확장자 |
| `INSERT_NO` | varchar(50) | Y |  |  | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_file_info` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `FILE_MGMT_CD` varchar(50) NOT NULL COMMENT '파일코드',
  `FILE_NM` varchar(500) COMMENT '파일명',
  `FILE_TYPE` varchar(3) NOT NULL COMMENT '파일타입[SYS010]',
  `FILE_PATH` varchar(500) NOT NULL COMMENT '파일저장경로',
  `FILE_EXT` varchar(10) NOT NULL COMMENT '파일확장자',
  `INSERT_NO` varchar(50) COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `FILE_MGMT_CD`)
) COMMENT='파일 정보 (첨부/증빙)';
```

<a id="tbholiday"></a>
## `tb_holiday` — 휴일관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `HOLIDAY_ID` | varchar(10) | — | PK |  | 휴일ID |
| `HOLIDAY_NM` | varchar(200) | — |  |  | 휴일명 |
| `HOLIDAY_YMD` | date | — |  |  | 휴일 |
| `HOLIDAY_TYPE` | varchar(2) | — |  |  | 휴일타입[SYS020] |
| `USE_YN` | char(1) | — |  | `Y` | 사용여부(Y/N) |
| `INSERT_NO` | varchar(50) | — |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_holiday` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `HOLIDAY_ID` varchar(10) NOT NULL COMMENT '휴일ID',
  `HOLIDAY_NM` varchar(200) NOT NULL COMMENT '휴일명',
  `HOLIDAY_YMD` date NOT NULL COMMENT '휴일',
  `HOLIDAY_TYPE` varchar(2) NOT NULL COMMENT '휴일타입[SYS020]',
  `USE_YN` char(1) NOT NULL DEFAULT 'Y' COMMENT '사용여부(Y/N)',
  `INSERT_NO` varchar(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `HOLIDAY_ID`),
  KEY `IX_HOLIDAY_DAY` (`CMPNY_CD`, `HOLIDAY_YMD`),
  KEY `IX_HOLIDAY_DAY_TYPE` (`CMPNY_CD`, `HOLIDAY_YMD`, `HOLIDAY_TYPE`)
) COMMENT='휴일관리';
```

<a id="tbholidayrule"></a>
## `tb_holiday_rule` — 휴일규칙(매년고정 회사휴일)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `HOLIDAY_RULE_ID` | varchar(10) | — | PK |  | 휴일규칙ID |
| `HOLIDAY_RULE_NM` | varchar(200) | — |  |  | 휴일규칙명 |
| `HOLIDAY_MM` | char(2) | — |  |  | 월(01~12) |
| `HOLIDAY_DD` | char(2) | — |  |  | 일(01~31) |
| `HOLIDAY_TYPE` | varchar(2) | — |  |  | 휴일타입[SYS020] |
| `USE_YN` | char(1) | — |  | `Y` | 사용여부(Y/N) |
| `INSERT_NO` | varchar(50) | — |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_holiday_rule` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `HOLIDAY_RULE_ID` varchar(10) NOT NULL COMMENT '휴일규칙ID',
  `HOLIDAY_RULE_NM` varchar(200) NOT NULL COMMENT '휴일규칙명',
  `HOLIDAY_MM` char(2) NOT NULL COMMENT '월(01~12)',
  `HOLIDAY_DD` char(2) NOT NULL COMMENT '일(01~31)',
  `HOLIDAY_TYPE` varchar(2) NOT NULL COMMENT '휴일타입[SYS020]',
  `USE_YN` char(1) NOT NULL DEFAULT 'Y' COMMENT '사용여부(Y/N)',
  `INSERT_NO` varchar(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `HOLIDAY_RULE_ID`),
  KEY `IX_HOLIDAY_RULE_USE` (`CMPNY_CD`, `USE_YN`),
  UNIQUE KEY `UK_HOLIDAY_RULE_MMDD` (`CMPNY_CD`, `HOLIDAY_MM`, `HOLIDAY_DD`, `HOLIDAY_TYPE`)
) COMMENT='휴일규칙(매년고정 회사휴일)';
```

<a id="tbnotioutbox"></a>
## `tb_noti_outbox` — 푸시 알림 outbox (발송 대기/이력)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `NOTI_ID` | varchar(20) | — | PK |  | 알림 ID (PK, 회사별 채번: N + YYYYMMDD + SEQ) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사 코드 |
| `SITE_CD` | varchar(50) | Y |  |  | 사업장 코드 (없으면 NULL) |
| `TARGET_USER_CD` | varchar(20) | — |  |  | 수신 대상 사용자 코드 |
| `NOTI_TYPE` | varchar(30) | — |  |  | 알림 유형[SYS045] LEAVE_GRANT_RECALLED:부여 연차 회수 |
| `CHANNEL` | varchar(10) | — |  | `PUSH` | 발송 채널 PUSH:푸시 |
| `TITLE` | varchar(200) | — |  |  | 알림 제목 |
| `BODY` | varchar(1000) | — |  |  | 알림 본문 |
| `DATA_PAYLOAD` | json | Y |  |  | 추가 데이터 페이로드(JSON) |
| `SEND_STATUS` | varchar(10) | — |  | `PENDING` | 발송 상태 PENDING:대기 / SENT:완료 / FAILED:실패 |
| `SENT_DATE` | datetime | Y |  |  | 발송 완료 일시 |
| `RETRY_CNT` | int | — |  | `0` | 재시도 횟수 |
| `ERROR_MSG` | varchar(500) | Y |  |  | 발송 실패 사유 |
| `DEDUP_KEY` | varchar(100) | Y |  |  | 중복 발송 방지 키(이벤트당 1건) |
| `DEL_YN` | varchar(1) | — |  | `N` | 삭제 여부 |
| `INSERT_NO` | varchar(50) | — |  |  | 등록자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 등록 일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정 일시 |

```sql
CREATE TABLE `tb_noti_outbox` (
  `NOTI_ID` varchar(20) NOT NULL COMMENT '알림 ID (PK, 회사별 채번: N + YYYYMMDD + SEQ)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COMMENT '사업장 코드 (없으면 NULL)',
  `TARGET_USER_CD` varchar(20) NOT NULL COMMENT '수신 대상 사용자 코드',
  `NOTI_TYPE` varchar(30) NOT NULL COMMENT '알림 유형[SYS045] LEAVE_GRANT_RECALLED:부여 연차 회수',
  `CHANNEL` varchar(10) NOT NULL DEFAULT 'PUSH' COMMENT '발송 채널 PUSH:푸시',
  `TITLE` varchar(200) NOT NULL COMMENT '알림 제목',
  `BODY` varchar(1000) NOT NULL COMMENT '알림 본문',
  `DATA_PAYLOAD` json COMMENT '추가 데이터 페이로드(JSON)',
  `SEND_STATUS` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT '발송 상태 PENDING:대기 / SENT:완료 / FAILED:실패',
  `SENT_DATE` datetime COMMENT '발송 완료 일시',
  `RETRY_CNT` int NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
  `ERROR_MSG` varchar(500) COMMENT '발송 실패 사유',
  `DEDUP_KEY` varchar(100) COMMENT '중복 발송 방지 키(이벤트당 1건)',
  `DEL_YN` varchar(1) NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정 일시',
  PRIMARY KEY (`NOTI_ID`),
  KEY `IX_NOTI_OUTBOX_PENDING` (`CMPNY_CD`, `SEND_STATUS`, `INSERT_DATE`),
  KEY `IX_NOTI_OUTBOX_TARGET` (`CMPNY_CD`, `TARGET_USER_CD`, `NOTI_TYPE`),
  UNIQUE KEY `UK_NOTI_OUTBOX_DEDUP` (`CMPNY_CD`, `DEDUP_KEY`)
) COMMENT='푸시 알림 outbox (발송 대기/이력)';
```

<a id="tbsmsauthcode"></a>
## `tb_sms_auth_code` — SMS 인증코드

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `SMS_ID` | bigint | — | PK AI |  |  |
| `MBL_NO_ENC` | varchar(200) | — |  |  |  |
| `MBL_NO_HMAC` | char(43) | — | IDX |  |  |
| `AUTH_CD` | varchar(6) | — |  |  |  |
| `EXPIRED_AT` | datetime | — |  |  |  |
| `VERIFIED_YN` | varchar(2) | Y |  | `N` |  |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` |  |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` |  |
| `UPDATE_NO` | varchar(50) | Y |  |  |  |
| `UPDATE_DATE` | datetime | Y |  |  |  |

```sql
CREATE TABLE `tb_sms_auth_code` (
  `SMS_ID` bigint NOT NULL AUTO_INCREMENT,
  `MBL_NO_ENC` varchar(200) NOT NULL,
  `MBL_NO_HMAC` char(43) NOT NULL,
  `AUTH_CD` varchar(6) NOT NULL,
  `EXPIRED_AT` datetime NOT NULL,
  `VERIFIED_YN` varchar(2) DEFAULT 'N',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50),
  `UPDATE_DATE` datetime,
  PRIMARY KEY (`SMS_ID`),
  KEY `idx_sms_auth_mbl_hmac_exp` (`MBL_NO_HMAC`, `EXPIRED_AT`),
  KEY `idx_sms_auth_mbl_hmac_ins` (`MBL_NO_HMAC`, `INSERT_DATE`)
) COMMENT='SMS 인증코드';
```

<a id="tbsystauthmenu"></a>
## `tb_syst_auth_menu` — 권한별 메뉴 접근 제어 (Role x Menu x CRUD x Platform)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `AUTH_CD` | varchar(10) | — | PK |  | 권한코드 |
| `MENU_D_ID` | varchar(50) | — | PK |  |  |
| `USE_YN` | varchar(2) | Y |  |  |  |
| `BTN_SRCH` | varchar(2) | Y |  | `Y` | 조회권한 |
| `BTN_NEW` | varchar(2) | Y |  | `Y` | 신규권한 |
| `BTN_DELT` | varchar(2) | Y |  | `Y` | 삭제권한 |
| `BTN_SAVE` | varchar(2) | Y |  | `Y` | 저장권한 |
| `BTN_EXCL` | varchar(2) | Y |  | `Y` | 엑셀권한 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_syst_auth_menu` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `AUTH_CD` varchar(10) NOT NULL COMMENT '권한코드',
  `MENU_D_ID` varchar(50) NOT NULL,
  `USE_YN` varchar(2),
  `BTN_SRCH` varchar(2) DEFAULT 'Y' COMMENT '조회권한',
  `BTN_NEW` varchar(2) DEFAULT 'Y' COMMENT '신규권한',
  `BTN_DELT` varchar(2) DEFAULT 'Y' COMMENT '삭제권한',
  `BTN_SAVE` varchar(2) DEFAULT 'Y' COMMENT '저장권한',
  `BTN_EXCL` varchar(2) DEFAULT 'Y' COMMENT '엑셀권한',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `AUTH_CD`, `MENU_D_ID`)
) COMMENT='권한별 메뉴 접근 제어 (Role x Menu x CRUD x Platform)';
```

<a id="tbsystmenud"></a>
## `tb_syst_menu_d` — 시스템 메뉴 상세

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `MENU_D_ID` | varchar(50) | — | PK |  |  |
| `MENU_M_ID` | varchar(10) | — | PK |  | 대메뉴ID |
| `MENU_VIEW` | varchar(50) | Y |  |  |  |
| `MENU_NM` | varchar(50) | — |  |  | 메뉴명 |
| `MENU_IDX` | int | Y |  |  | 메뉴순번 |
| `MENU_DESC` | varchar(200) | Y |  |  | 비고 |
| `USE_YN` | varchar(50) | Y |  | `Y` | 사용여부 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_syst_menu_d` (
  `MENU_D_ID` varchar(50) NOT NULL,
  `MENU_M_ID` varchar(10) NOT NULL COMMENT '대메뉴ID',
  `MENU_VIEW` varchar(50),
  `MENU_NM` varchar(50) NOT NULL COMMENT '메뉴명',
  `MENU_IDX` int COMMENT '메뉴순번',
  `MENU_DESC` varchar(200) COMMENT '비고',
  `USE_YN` varchar(50) DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`MENU_D_ID`, `MENU_M_ID`)
) COMMENT='시스템 메뉴 상세';
```

<a id="tbsystmenum"></a>
## `tb_syst_menu_m` — 시스템 메뉴 마스터

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `MENU_M_ID` | varchar(10) | — | PK |  | 대메뉴ID |
| `MENU_SRC` | varchar(3) | — |  |  | 메뉴사용처[SYS007] |
| `MENU_NM` | varchar(50) | — |  |  | 메뉴명 |
| `MENU_IDX` | int | Y |  |  | 메뉴순번 |
| `MENU_DESC` | varchar(200) | Y |  |  | 비고 |
| `USE_YN` | varchar(50) | Y |  | `Y` | 사용여부 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_syst_menu_m` (
  `MENU_M_ID` varchar(10) NOT NULL COMMENT '대메뉴ID',
  `MENU_SRC` varchar(3) NOT NULL COMMENT '메뉴사용처[SYS007]',
  `MENU_NM` varchar(50) NOT NULL COMMENT '메뉴명',
  `MENU_IDX` int COMMENT '메뉴순번',
  `MENU_DESC` varchar(200) COMMENT '비고',
  `USE_YN` varchar(50) DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`MENU_M_ID`)
) COMMENT='시스템 메뉴 마스터';
```

<a id="tbsystvald"></a>
## `tb_syst_val_d` — 시스템 공통코드 상세 (SYS 코드값)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `SYST_VAL_CD` | varchar(50) | — | PK |  | 시스템변수코드 |
| `SYST_VAL_D_CD` | varchar(50) | — | PK |  | 시스템변수코드 |
| `SYST_VAL_D_NM` | varchar(100) | — |  |  | 시스템변수이름 |
| `SORT_IDX` | int | Y |  |  | 순번 |
| `USE_YN` | varchar(2) | Y |  | `Y` | 사용여부 |
| `VAL_D_INFO_1` | varchar(50) | Y |  |  | 시스템변수 정보1 |
| `VAL_D_INFO_2` | varchar(50) | Y |  |  | 시스템변수 정보2 |
| `VAL_D_DESC` | varchar(500) | Y |  |  | 시스템변수 설명 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` |  |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` |  |
| `UPDATE_NO` | varchar(50) | Y |  |  |  |
| `UPDATE_DATE` | date | Y |  |  |  |

```sql
CREATE TABLE `tb_syst_val_d` (
  `SYST_VAL_CD` varchar(50) NOT NULL COMMENT '시스템변수코드',
  `SYST_VAL_D_CD` varchar(50) NOT NULL COMMENT '시스템변수코드',
  `SYST_VAL_D_NM` varchar(100) NOT NULL COMMENT '시스템변수이름',
  `SORT_IDX` int COMMENT '순번',
  `USE_YN` varchar(2) DEFAULT 'Y' COMMENT '사용여부',
  `VAL_D_INFO_1` varchar(50) COMMENT '시스템변수 정보1',
  `VAL_D_INFO_2` varchar(50) COMMENT '시스템변수 정보2',
  `VAL_D_DESC` varchar(500) COMMENT '시스템변수 설명',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50),
  `UPDATE_DATE` date,
  PRIMARY KEY (`SYST_VAL_CD`, `SYST_VAL_D_CD`)
) COMMENT='시스템 공통코드 상세 (SYS 코드값)';
```

<a id="tbsystvalm"></a>
## `tb_syst_val_m` — 시스템 공통코드 마스터 (SYS 코드그룹)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `SYST_VAL_CD` | varchar(50) | — | PK |  | 시스템변수코드 |
| `SYST_VAL_NM` | varchar(100) | — |  |  | 시스템변수이름 |
| `USE_YN` | varchar(2) | Y |  | `Y` | 사용여부 |
| `VAL_INFO_1` | varchar(50) | Y |  |  | 시스템변수 정보1 |
| `VAL_INFO_2` | varchar(50) | Y |  |  | 시스템변수 정보2 |
| `VAL_DESC` | varchar(500) | Y |  |  | 시스템변수 설명 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` |  |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` |  |
| `UPDATE_NO` | varchar(50) | Y |  |  |  |
| `UPDATE_DATE` | date | Y |  |  |  |

```sql
CREATE TABLE `tb_syst_val_m` (
  `SYST_VAL_CD` varchar(50) NOT NULL COMMENT '시스템변수코드',
  `SYST_VAL_NM` varchar(100) NOT NULL COMMENT '시스템변수이름',
  `USE_YN` varchar(2) DEFAULT 'Y' COMMENT '사용여부',
  `VAL_INFO_1` varchar(50) COMMENT '시스템변수 정보1',
  `VAL_INFO_2` varchar(50) COMMENT '시스템변수 정보2',
  `VAL_DESC` varchar(500) COMMENT '시스템변수 설명',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50),
  `UPDATE_DATE` date,
  PRIMARY KEY (`SYST_VAL_CD`)
) COMMENT='시스템 공통코드 마스터 (SYS 코드그룹)';
```

<a id="tbterms"></a>
## `tb_terms` — 이용약관

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `TERMS_ID` | varchar(3) | — | PK |  | 약관ID(SYS008) |
| `TERMS_VERSION` | varchar(10) | — |  |  | 약관버전 |
| `REQUIRED_YN` | varchar(10) | — |  |  | 필수여부 |
| `TERMS_CONTENT` | longtext | — |  |  | 약관본문 |
| `STR_DATE` | varchar(8) | — |  |  | 시행일자 |
| `USE_YN` | varchar(2) | Y |  | `Y` | 사용유무 |
| `TERMS_DESC` | varchar(500) | Y |  |  | 비고 |
| `INSERT_NO` | varchar(50) | Y |  |  | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_terms` (
  `TERMS_ID` varchar(3) NOT NULL COMMENT '약관ID(SYS008)',
  `TERMS_VERSION` varchar(10) NOT NULL COMMENT '약관버전',
  `REQUIRED_YN` varchar(10) NOT NULL COMMENT '필수여부',
  `TERMS_CONTENT` longtext NOT NULL COMMENT '약관본문',
  `STR_DATE` varchar(8) NOT NULL COMMENT '시행일자',
  `USE_YN` varchar(2) DEFAULT 'Y' COMMENT '사용유무',
  `TERMS_DESC` varchar(500) COMMENT '비고',
  `INSERT_NO` varchar(50) COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`TERMS_ID`)
) COMMENT='이용약관';
```

<a id="tbtermsidversion"></a>
## `tb_terms_id_version` — 약관 버전

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `TERMS_ID` | varchar(3) | — | PK |  | 약관ID(SYS008) |
| `TERMS_VERSION` | varchar(10) | — | PK |  | 약관버전 |
| `REQUIRED_YN` | varchar(10) | — |  |  | 필수여부 |
| `TERMS_CONTENT` | longtext | — |  |  | 약관본문 |
| `STR_DATE` | varchar(8) | — |  |  | 시행일자 |
| `TERMS_DESC` | varchar(500) | Y |  |  | 비고 |
| `INSERT_NO` | varchar(50) | Y |  |  | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_terms_id_version` (
  `TERMS_ID` varchar(3) NOT NULL COMMENT '약관ID(SYS008)',
  `TERMS_VERSION` varchar(10) NOT NULL COMMENT '약관버전',
  `REQUIRED_YN` varchar(10) NOT NULL COMMENT '필수여부',
  `TERMS_CONTENT` longtext NOT NULL COMMENT '약관본문',
  `STR_DATE` varchar(8) NOT NULL COMMENT '시행일자',
  `TERMS_DESC` varchar(500) COMMENT '비고',
  `INSERT_NO` varchar(50) COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`TERMS_ID`, `TERMS_VERSION`)
) COMMENT='약관 버전';
```

<a id="tbtermsuseragrmgmt"></a>
## `tb_terms_user_agr_mgmt` — 사용자 약관 동의 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `USER_CD` | varchar(20) | — | PK |  | 사용자코드 |
| `TERMS_ID` | varchar(3) | — | PK |  | 약관ID(SYS008) |
| `TERMS_VERSION` | varchar(10) | — | PK |  | 약관버전 |
| `AGR_YN` | varchar(2) | Y |  | `Y` | 동의여부 |
| `INSERT_NO` | varchar(50) | Y |  |  | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_terms_user_agr_mgmt` (
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자코드',
  `TERMS_ID` varchar(3) NOT NULL COMMENT '약관ID(SYS008)',
  `TERMS_VERSION` varchar(10) NOT NULL COMMENT '약관버전',
  `AGR_YN` varchar(2) DEFAULT 'Y' COMMENT '동의여부',
  `INSERT_NO` varchar(50) COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`USER_CD`, `TERMS_ID`, `TERMS_VERSION`)
) COMMENT='사용자 약관 동의 관리';
```


# 2. 사업장·조직·권한

<a id="tbsite"></a>
## `tb_site` — 사업장 마스터

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `SITE_NO` | varchar(50) | — |  |  | 사업장번호 |
| `SITE_NM` | varchar(100) | — |  |  | 사업장명 |
| `ADDR_1` | varchar(200) | Y |  |  | 주소 |
| `ADDR_2` | varchar(200) | Y |  |  | 상세주소 |
| `ZIP_CODE` | varchar(20) | Y |  |  | 우편번호 |
| `STR_DATE` | varchar(8) | Y |  |  | 사업개시일 |
| `END_DATE` | varchar(8) | Y |  |  | 사업종료일 |
| `USE_YN` | varchar(2) | Y |  | `Y` | 사용여부 |
| `SITE_ADMIN_CD` | varchar(50) | Y |  |  | 사업장관리자코드 |
| `TEL_NO` | varchar(20) | Y |  |  | 사업장전화번호 |
| `LAT` | decimal(10,7) | Y |  |  | 사업장 중심 위도 |
| `LON` | decimal(10,7) | Y |  |  | 사업장 중심 경도 |
| `GPS_RANGE` | varchar(4) | Y |  |  |  |
| `SITE_DESC` | varchar(500) | Y |  |  | 비고 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | date | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_site` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `SITE_NO` varchar(50) NOT NULL COMMENT '사업장번호',
  `SITE_NM` varchar(100) NOT NULL COMMENT '사업장명',
  `ADDR_1` varchar(200) COMMENT '주소',
  `ADDR_2` varchar(200) COMMENT '상세주소',
  `ZIP_CODE` varchar(20) COMMENT '우편번호',
  `STR_DATE` varchar(8) COMMENT '사업개시일',
  `END_DATE` varchar(8) COMMENT '사업종료일',
  `USE_YN` varchar(2) DEFAULT 'Y' COMMENT '사용여부',
  `SITE_ADMIN_CD` varchar(50) COMMENT '사업장관리자코드',
  `TEL_NO` varchar(20) COMMENT '사업장전화번호',
  `LAT` decimal(10,7) COMMENT '사업장 중심 위도',
  `LON` decimal(10,7) COMMENT '사업장 중심 경도',
  `GPS_RANGE` varchar(4),
  `SITE_DESC` varchar(500) COMMENT '비고',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` date COMMENT '수정일시',
  PRIMARY KEY (`SITE_CD`, `CMPNY_CD`)
) COMMENT='사업장 마스터';
```

<a id="tbsitenode"></a>
## `tb_site_node` — 조직도 노드 (사업장 하위 트리)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(10) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(20) | — | PK |  | 사업장코드 |
| `NODE_CD` | varchar(50) | — | PK |  | 노드ID |
| `NODE_NM` | varchar(200) | — |  |  | 노드명 |
| `NODE_TYPE` | varchar(5) | — |  |  | 노드타입[COM004] |
| `PARENT_NODE_CD` | varchar(50) | Y |  |  | 부모노드ID |
| `SELF_ATTD_APPRV_YN` | char(1) | — |  | `N` | 자체근태승인여부 |
| `MAIN_ADMIN_CD` | varchar(50) | Y |  |  | 부서 정 관리자 |
| `SUB_ADMIN_CD` | varchar(50) | Y |  |  | 부서 부 관리자 |
| `INSERT_NO` | varchar(50) | — |  |  | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | — |  |  | 수정자 |
| `UPDATE_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 수정일시 |

```sql
CREATE TABLE `tb_site_node` (
  `CMPNY_CD` varchar(10) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(20) NOT NULL COMMENT '사업장코드',
  `NODE_CD` varchar(50) NOT NULL COMMENT '노드ID',
  `NODE_NM` varchar(200) NOT NULL COMMENT '노드명',
  `NODE_TYPE` varchar(5) NOT NULL COMMENT '노드타입[COM004]',
  `PARENT_NODE_CD` varchar(50) COMMENT '부모노드ID',
  `SELF_ATTD_APPRV_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '자체근태승인여부',
  `MAIN_ADMIN_CD` varchar(50) COMMENT '부서 정 관리자',
  `SUB_ADMIN_CD` varchar(50) COMMENT '부서 부 관리자',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `NODE_CD`),
  KEY `IX_NODE_PARENT` (`CMPNY_CD`, `SITE_CD`, `PARENT_NODE_CD`)
) COMMENT='조직도 노드 (사업장 하위 트리)';
```

<a id="tbusersiteauth"></a>
## `tb_user_site_auth` — 사용자 사업장 권한 매핑

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `USER_CD` | varchar(20) | — | PK |  | 사용자CD |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `USE_YN` | varchar(2) | Y |  | `Y` | 사용여부 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_user_site_auth` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자CD',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `USE_YN` varchar(2) DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `USER_CD`, `SITE_CD`)
) COMMENT='사용자 사업장 권한 매핑';
```


# 3. 사용자·계정

<a id="tbdeluser"></a>
## `tb_del_user` — 탈퇴 사용자 보관

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `USER_ID` | varchar(50) | — | PK |  | 사용자ID |
| `USER_NM` | varchar(50) | — |  |  | 사용자명 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일자 |

```sql
CREATE TABLE `tb_del_user` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `USER_ID` varchar(50) NOT NULL COMMENT '사용자ID',
  `USER_NM` varchar(50) NOT NULL COMMENT '사용자명',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일자',
  PRIMARY KEY (`CMPNY_CD`, `USER_ID`)
) COMMENT='탈퇴 사용자 보관';
```

<a id="tbuser"></a>
## `tb_user` — 사용자

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `USER_CD` | varchar(20) | — | PK |  | 사용자코드 |
| `USER_ID` | varchar(50) | — |  |  | 사용자ID |
| `USER_NM` | varchar(50) | — |  |  | 사용자명 |
| `USER_PW` | varchar(100) | Y |  |  | 비밀번호(해시) |
| `SITE_CD` | varchar(50) | Y |  |  | 사업장코드 |
| `NODE_CD` | varchar(50) | Y |  |  | 소속부서 |
| `AUTH_CD` | varchar(10) | — |  |  | 권한코드 |
| `RANK_CD` | varchar(10) | Y |  |  | 직급 코드 (BAIM_VAL COM007 직급 코드그룹 참조) |
| `MBL_NO_ENC` | text | Y |  |  | 휴대폰번호 AES-GCM (v1.base64url) |
| `MBL_NO_HMAC` | varchar(43) | Y |  |  | 휴대폰번호 HMAC-SHA256 Base64URL (equals/중복/계정찾기) |
| `MBL_NO_LAST4` | char(4) | Y |  |  | 휴대폰번호 마지막4자리(마스킹/리스트용) |
| `EMAIL_ENC` | text | Y |  |  | 이메일 AES-GCM (v1.base64url) |
| `EMAIL_HMAC` | varchar(43) | Y |  |  | 이메일 HMAC-SHA256 Base64URL (equals/중복/계정찾기) |
| `EMAIL_DOMAIN` | varchar(100) | Y |  |  | 이메일 도메인(선택) |
| `BIRTH_DT_ENC` | text | Y |  |  | 생년월일 AES-GCM (v1.base64url) |
| `HIRE_DATE` | varchar(8) | Y |  |  | 입사일 (YYYYMMDD) — 연차 부여 기준 |
| `EMPLOYMENT_TYPE` | varchar(20) | Y |  |  | 고용형태[SYS041] REGULAR/CONTRACT/DAILY/EXECUTIVE |
| `CONTRACT_END_DATE` | varchar(8) | Y |  |  | 계약 종료일 (YYYYMMDD, EMPLOYMENT_TYPE=CONTRACT일 때 필수) |
| `GENDER` | varchar(6) | Y |  |  | 성별 |
| `USE_YN` | varchar(2) | Y |  | `Y` | 사용여부 |
| `ACCOUNT_STATUS` | varchar(20) | — |  | `01` | 계정상태[SYS013] 01:활성화 02:잠김 03:탈퇴 04:인증대기 |
| `PWD_LOCK_YN` | varchar(2) | — |  | `N` | 비밀번호잠금여부 |
| `PWD_FAIL_CNT` | int | — |  | `0` | 비밀번호실패횟수 |
| `PWD_LOCK_EXPIRE_DTIME` | datetime | Y |  |  | 비밀번호 인증 실패 잠금 만료일시 |
| `PWD_CHG_DTIME` | datetime | Y |  |  | 비밀번호변경일시 |
| `WITHDRAWAL_DATE` | varchar(8) | Y |  |  | 회원탈퇴일 |
| `LAST_LOGIN_DTIME` | datetime | Y |  |  | 마지막로그인 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_user` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자코드',
  `USER_ID` varchar(50) NOT NULL COMMENT '사용자ID',
  `USER_NM` varchar(50) NOT NULL COMMENT '사용자명',
  `USER_PW` varchar(100) COMMENT '비밀번호(해시)',
  `SITE_CD` varchar(50) COMMENT '사업장코드',
  `NODE_CD` varchar(50) COMMENT '소속부서',
  `AUTH_CD` varchar(10) NOT NULL COMMENT '권한코드',
  `RANK_CD` varchar(10) COMMENT '직급 코드 (BAIM_VAL COM007 직급 코드그룹 참조)',
  `MBL_NO_ENC` text COMMENT '휴대폰번호 AES-GCM (v1.base64url)',
  `MBL_NO_HMAC` varchar(43) COMMENT '휴대폰번호 HMAC-SHA256 Base64URL (equals/중복/계정찾기)',
  `MBL_NO_LAST4` char(4) COMMENT '휴대폰번호 마지막4자리(마스킹/리스트용)',
  `EMAIL_ENC` text COMMENT '이메일 AES-GCM (v1.base64url)',
  `EMAIL_HMAC` varchar(43) COMMENT '이메일 HMAC-SHA256 Base64URL (equals/중복/계정찾기)',
  `EMAIL_DOMAIN` varchar(100) COMMENT '이메일 도메인(선택)',
  `BIRTH_DT_ENC` text COMMENT '생년월일 AES-GCM (v1.base64url)',
  `HIRE_DATE` varchar(8) COMMENT '입사일 (YYYYMMDD) — 연차 부여 기준',
  `EMPLOYMENT_TYPE` varchar(20) COMMENT '고용형태[SYS041] REGULAR/CONTRACT/DAILY/EXECUTIVE',
  `CONTRACT_END_DATE` varchar(8) COMMENT '계약 종료일 (YYYYMMDD, EMPLOYMENT_TYPE=CONTRACT일 때 필수)',
  `GENDER` varchar(6) COMMENT '성별',
  `USE_YN` varchar(2) DEFAULT 'Y' COMMENT '사용여부',
  `ACCOUNT_STATUS` varchar(20) NOT NULL DEFAULT '01' COMMENT '계정상태[SYS013] 01:활성화 02:잠김 03:탈퇴 04:인증대기',
  `PWD_LOCK_YN` varchar(2) NOT NULL DEFAULT 'N' COMMENT '비밀번호잠금여부',
  `PWD_FAIL_CNT` int NOT NULL DEFAULT 0 COMMENT '비밀번호실패횟수',
  `PWD_LOCK_EXPIRE_DTIME` datetime COMMENT '비밀번호 인증 실패 잠금 만료일시',
  `PWD_CHG_DTIME` datetime COMMENT '비밀번호변경일시',
  `WITHDRAWAL_DATE` varchar(8) COMMENT '회원탈퇴일',
  `LAST_LOGIN_DTIME` datetime COMMENT '마지막로그인',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `USER_CD`),
  KEY `IX_TB_USER_CONTRACT` (`CMPNY_CD`, `CONTRACT_END_DATE`),
  KEY `IX_TB_USER_STATUS` (`CMPNY_CD`, `USE_YN`, `ACCOUNT_STATUS`),
  UNIQUE KEY `UX_TB_USER_ID` (`CMPNY_CD`, `USER_ID`),
  UNIQUE KEY `UX_TB_USER_MBL_NO` (`CMPNY_CD`, `MBL_NO_HMAC`)
) COMMENT='사용자';
```

<a id="tbuserdevice"></a>
## `tb_user_device` — 정규직 디바이스 점유 (글로벌 유니크 - 회사 가로질러 1디바이스=1계정)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `DEVICE_UUID` | varchar(100) | — | PK |  | 디바이스UUID |
| `USER_CD` | varchar(20) | — | IDX |  | 사용자코드 |
| `DEVICE_TYPE` | varchar(20) | — |  |  | 디바이스종류(IOS/ANDROID) |
| `DEVICE_MODEL` | varchar(50) | Y |  |  | 디바이스모델 |
| `OS_VERSION` | varchar(20) | Y |  |  | OS버전 |
| `APP_VERSION` | varchar(20) | Y |  |  | 앱버전 |
| `PUSH_TOKEN` | varchar(500) | Y |  |  | FCM/APNS 푸시토큰 |
| `LAST_LOGIN_DTIME` | datetime | Y |  |  | 최종로그인일시 |
| `LAST_LOGIN_IP` | varchar(45) | Y |  |  | 최종로그인IP |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_user_device` (
  `DEVICE_UUID` varchar(100) NOT NULL COMMENT '디바이스UUID',
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자코드',
  `DEVICE_TYPE` varchar(20) NOT NULL COMMENT '디바이스종류(IOS/ANDROID)',
  `DEVICE_MODEL` varchar(50) COMMENT '디바이스모델',
  `OS_VERSION` varchar(20) COMMENT 'OS버전',
  `APP_VERSION` varchar(20) COMMENT '앱버전',
  `PUSH_TOKEN` varchar(500) COMMENT 'FCM/APNS 푸시토큰',
  `LAST_LOGIN_DTIME` datetime COMMENT '최종로그인일시',
  `LAST_LOGIN_IP` varchar(45) COMMENT '최종로그인IP',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`DEVICE_UUID`),
  KEY `idx_user_device_user` (`USER_CD`)
) COMMENT='정규직 디바이스 점유 (글로벌 유니크 - 회사 가로질러 1디바이스=1계정)';
```


# 4. 일일계정·슬롯

<a id="tbdailylinkmgmt"></a>
## `tb_daily_link_mgmt` — 일용직 계정 생성 링크 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `LINK_MGMT_ID` | varchar(50) | — | PK |  | 링크관리ID |
| `LINK_TOKEN_HASH` | varchar(128) | — |  |  | 링크토큰 해시(원문 저장 금지) |
| `SITE_CD` | varchar(50) | — |  |  | 사업장코드 |
| `CNT_BASE_DT` | date | Y |  |  | 생성카운트 기준일(일별 제한용) |
| `MAX_CREATE_CNT` | int | — |  | `1` | 생성가능 인원수 |
| `CREATED_CNT` | int | — |  | `0` | 생성완료 인원수 |
| `SMS_VERIFY_YN` | varchar(2) | — |  | `N` | SMS 인증 사용여부 |
| `EXPIRE_DTIME` | datetime | Y |  |  | 링크 만료일시 |
| `USE_YN` | varchar(2) | — |  | `Y` | 사용여부 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_daily_link_mgmt` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `LINK_MGMT_ID` varchar(50) NOT NULL COMMENT '링크관리ID',
  `LINK_TOKEN_HASH` varchar(128) NOT NULL COMMENT '링크토큰 해시(원문 저장 금지)',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `CNT_BASE_DT` date COMMENT '생성카운트 기준일(일별 제한용)',
  `MAX_CREATE_CNT` int NOT NULL DEFAULT 1 COMMENT '생성가능 인원수',
  `CREATED_CNT` int NOT NULL DEFAULT 0 COMMENT '생성완료 인원수',
  `SMS_VERIFY_YN` varchar(2) NOT NULL DEFAULT 'N' COMMENT 'SMS 인증 사용여부',
  `EXPIRE_DTIME` datetime COMMENT '링크 만료일시',
  `USE_YN` varchar(2) NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `LINK_MGMT_ID`),
  KEY `IX_DAILY_LINK_EXPIRE` (`CMPNY_CD`, `USE_YN`, `EXPIRE_DTIME`),
  KEY `IX_DAILY_LINK_SITE` (`CMPNY_CD`, `SITE_CD`),
  UNIQUE KEY `UK_DAILY_LINK_TOKEN_HASH` (`CMPNY_CD`, `LINK_TOKEN_HASH`)
) COMMENT='일용직 계정 생성 링크 관리';
```

<a id="tbdailyuser"></a>
## `tb_daily_user` — 일용직 사용자

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — |  |  | 사업장코드 |
| `USER_CD` | varchar(20) | — | PK |  | 사용자코드 |
| `USER_ID` | varchar(50) | — |  |  | 사용자ID(USER_CD 기반 자동생성, 표시용) |
| `USER_NM` | varchar(50) | — |  |  | 사용자명 |
| `USER_PW` | varchar(100) | — |  |  | 비밀번호(해시) - QR발급 사용자는 난수 |
| `MBL_NO_ENC` | text | Y |  |  | 휴대폰번호 AES-GCM (v1.base64url) |
| `MBL_NO_HMAC` | varchar(43) | Y |  |  | 휴대폰번호 HMAC-SHA256 Base64URL |
| `MBL_NO_LAST4` | char(4) | Y |  |  | 휴대폰번호 마지막4자리(마스킹/리스트용) |
| `REG_TYPE` | varchar(20) | — |  |  | 가입경로SYS030] |
| `USE_YN` | varchar(2) | — |  | `Y` | 사용여부 |
| `ACCOUNT_STATUS` | varchar(20) | — |  | `01` | 계정상태[SYS013] 01:활성화 02:잠김 03:탈퇴 04:인증대기 |
| `WORK_EXPIRE_DATE` | varchar(8) | — | IDX |  | 계정 만료일(YYYYMMDD, 자정 배치 기준) |
| `WITHDRAWAL_DATE` | varchar(8) | Y |  |  | 회원탈퇴일 |
| `PWD_FAIL_CNT` | int | — |  | `0` | 비밀번호실패횟수 |
| `PWD_LOCK_YN` | varchar(2) | — |  | `N` | 비밀번호잠금여부 |
| `PWD_LOCK_EXPIRE_DTIME` | datetime | Y |  |  | 비밀번호 인증 실패 잠금 만료일시 |
| `LAST_LOGIN_DTIME` | datetime | Y |  |  | 마지막 로그인일시 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_daily_user` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자코드',
  `USER_ID` varchar(50) NOT NULL COMMENT '사용자ID(USER_CD 기반 자동생성, 표시용)',
  `USER_NM` varchar(50) NOT NULL COMMENT '사용자명',
  `USER_PW` varchar(100) NOT NULL COMMENT '비밀번호(해시) - QR발급 사용자는 난수',
  `MBL_NO_ENC` text COMMENT '휴대폰번호 AES-GCM (v1.base64url)',
  `MBL_NO_HMAC` varchar(43) COMMENT '휴대폰번호 HMAC-SHA256 Base64URL',
  `MBL_NO_LAST4` char(4) COMMENT '휴대폰번호 마지막4자리(마스킹/리스트용)',
  `REG_TYPE` varchar(20) NOT NULL COMMENT '가입경로SYS030]',
  `USE_YN` varchar(2) NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `ACCOUNT_STATUS` varchar(20) NOT NULL DEFAULT '01' COMMENT '계정상태[SYS013] 01:활성화 02:잠김 03:탈퇴 04:인증대기',
  `WORK_EXPIRE_DATE` varchar(8) NOT NULL COMMENT '계정 만료일(YYYYMMDD, 자정 배치 기준)',
  `WITHDRAWAL_DATE` varchar(8) COMMENT '회원탈퇴일',
  `PWD_FAIL_CNT` int NOT NULL DEFAULT 0 COMMENT '비밀번호실패횟수',
  `PWD_LOCK_YN` varchar(2) NOT NULL DEFAULT 'N' COMMENT '비밀번호잠금여부',
  `PWD_LOCK_EXPIRE_DTIME` datetime COMMENT '비밀번호 인증 실패 잠금 만료일시',
  `LAST_LOGIN_DTIME` datetime COMMENT '마지막 로그인일시',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `USER_CD`),
  KEY `IX_TB_DAILY_USER_EXPIRE` (`WORK_EXPIRE_DATE`, `USE_YN`),
  KEY `IX_TB_DAILY_USER_MBL_LOOKUP` (`CMPNY_CD`, `MBL_NO_HMAC`),
  KEY `IX_TB_DAILY_USER_SITE` (`CMPNY_CD`, `SITE_CD`, `USE_YN`),
  UNIQUE KEY `UX_TB_DAILY_USER_MBL` (`CMPNY_CD`, `None`)
) COMMENT='일용직 사용자';
```

<a id="tbdailyuserlinkpolicy"></a>
## `tb_daily_user_link_policy` — 사업장별 일일계정 발급 정책

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사 코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장 코드 |
| `USE_YN` | char(1) | — |  | `N` | 정책 사용 여부 |
| `DAY_LIMIT_CNT` | int | — |  |  | 발급 허용 수(슬롯/정원) |
| `INSERT_NO` | varchar(50) | — |  |  | 등록자 ID |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 등록 일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 ID |
| `UPDATE_DATE` | datetime | Y |  |  | 수정 일시 |

```sql
CREATE TABLE `tb_daily_user_link_policy` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장 코드',
  `USE_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '정책 사용 여부',
  `DAY_LIMIT_CNT` int NOT NULL COMMENT '발급 허용 수(슬롯/정원)',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '등록자 ID',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자 ID',
  `UPDATE_DATE` datetime COMMENT '수정 일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`)
) COMMENT='사업장별 일일계정 발급 정책';
```

<a id="tbdailyuserslot"></a>
## `tb_daily_user_slot` — 일일계정 슬롯(현재 점유 상태만 관리)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `SLOT_NO` | varchar(4) | — | PK |  | 슬롯 번호(1~N) |
| `SLOT_TYPE` | varchar(2) | — |  |  | 슬롯구분[SYS014] |
| `FIXED_YN` | varchar(2) | Y |  | `N` | 고정여부[SYS017] |
| `USE_YN` | varchar(2) | — |  | `Y` | 사용여부[SYS003] |
| `CURR_USER_CD` | varchar(50) | Y |  |  | 현재 점유중 사용자CD |
| `SLOT_STATUS` | varchar(2) | — |  | `00` | 슬롯상태[SYS015] |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_daily_user_slot` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `SLOT_NO` varchar(4) NOT NULL COMMENT '슬롯 번호(1~N)',
  `SLOT_TYPE` varchar(2) NOT NULL COMMENT '슬롯구분[SYS014]',
  `FIXED_YN` varchar(2) DEFAULT 'N' COMMENT '고정여부[SYS017]',
  `USE_YN` varchar(2) NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
  `CURR_USER_CD` varchar(50) COMMENT '현재 점유중 사용자CD',
  `SLOT_STATUS` varchar(2) NOT NULL DEFAULT '00' COMMENT '슬롯상태[SYS015]',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `SLOT_NO`),
  KEY `IDX_DAILY_SLOT_STATUS` (`CMPNY_CD`, `SITE_CD`, `SLOT_STATUS`),
  KEY `IDX_DAILY_SLOT_USER` (`CMPNY_CD`, `CURR_USER_CD`)
) COMMENT='일일계정 슬롯(현재 점유 상태만 관리)';
```

<a id="tbdailyuserslothis"></a>
## `tb_daily_user_slot_his` — 일일계정 슬롯 사용 이력

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `HIS_ID` | varchar(20) | — | PK |  | 이력ID(PK) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사코드 |
| `SITE_CD` | varchar(50) | — |  |  | 사업장코드 |
| `SLOT_NO` | varchar(4) | — |  |  | 슬롯 번호 |
| `WORK_DATE` | char(8) | — |  |  | 사용 일자(YYYYMMDD) |
| `USER_ID` | varchar(50) | — |  |  | 할당 사용자ID |
| `ISSUE_CHANNEL` | varchar(20) | — |  |  | 발급채널[SYS014] |
| `OCCUPY_DTIME` | datetime | — |  |  | 점유 시작 일시 |
| `RELEASE_DTIME` | datetime | Y |  |  | 해제 일시 |
| `RELEASE_USER` | varchar(20) | Y |  | `SYSTEM` | 점유해제자 |
| `RELEASE_TYPE` | varchar(20) | Y |  |  | 해제유형[SYS016] |
| `RELEASE_REASON` | varchar(200) | Y |  |  | 해제 사유 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_daily_user_slot_his` (
  `HIS_ID` varchar(20) NOT NULL COMMENT '이력ID(PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `SLOT_NO` varchar(4) NOT NULL COMMENT '슬롯 번호',
  `WORK_DATE` char(8) NOT NULL COMMENT '사용 일자(YYYYMMDD)',
  `USER_ID` varchar(50) NOT NULL COMMENT '할당 사용자ID',
  `ISSUE_CHANNEL` varchar(20) NOT NULL COMMENT '발급채널[SYS014]',
  `OCCUPY_DTIME` datetime NOT NULL COMMENT '점유 시작 일시',
  `RELEASE_DTIME` datetime COMMENT '해제 일시',
  `RELEASE_USER` varchar(20) DEFAULT 'SYSTEM' COMMENT '점유해제자',
  `RELEASE_TYPE` varchar(20) COMMENT '해제유형[SYS016]',
  `RELEASE_REASON` varchar(200) COMMENT '해제 사유',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`HIS_ID`),
  KEY `IDX_SLOT_HIS_DAY` (`CMPNY_CD`, `SITE_CD`, `WORK_DATE`),
  KEY `IDX_SLOT_HIS_SLOT` (`CMPNY_CD`, `SITE_CD`, `SLOT_NO`, `WORK_DATE`),
  KEY `IDX_SLOT_HIS_USER` (`CMPNY_CD`, `USER_ID`, `WORK_DATE`)
) COMMENT='일일계정 슬롯 사용 이력';
```


# 5. 근무타입·교대·근무계획

<a id="tbschmgmt"></a>
## `tb_sch_mgmt` — 사업장 근무타입 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `SCH_CD` | varchar(20) | — | PK |  | 스케줄 코드 |
| `SCH_NO` | varchar(50) | — |  |  | 스케줄 번호 |
| `SCH_TYPE` | varchar(2) | — |  |  | 스케줄타입[SYS019] |
| `BASE_YN` | varchar(2) | Y |  |  | 기본스케줄여부[SYS003] |
| `APPLY_DATE` | varchar(8) | — |  |  | 적용일자 |
| `FST_SCH_STR_TIME` | varchar(4) | — |  |  | 1구간 시작시간 |
| `FST_SCH_END_TIME` | varchar(4) | — |  |  | 1구간 종료시간 |
| `FST_SCH_BRK_MIN` | varchar(3) | Y |  |  | 1구간 휴게시간 |
| `FST_BRK_STR_TIME` | varchar(4) | Y |  |  | 1구간 휴게 시작(HHMM) |
| `FST_BRK_END_TIME` | varchar(4) | Y |  |  | 1구간 휴게 종료(HHMM) |
| `SEC_SCH_STR_TIME` | varchar(4) | Y |  |  | 2구간 시작시간 |
| `SEC_SCH_END_TIME` | varchar(4) | Y |  |  | 2구간 종료시간 |
| `SEC_SCH_BRK_MIN` | varchar(3) | Y |  |  | 2구간 휴게시간 |
| `SEC_BRK_STR_TIME` | varchar(4) | Y |  |  | 2구간 휴게 시작(HHMM) |
| `SEC_BRK_END_TIME` | varchar(4) | Y |  |  | 2구간 휴게 종료(HHMM) |
| `USE_YN` | varchar(2) | — |  | `Y` | 사용여부 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_sch_mgmt` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `SCH_CD` varchar(20) NOT NULL COMMENT '스케줄 코드',
  `SCH_NO` varchar(50) NOT NULL COMMENT '스케줄 번호',
  `SCH_TYPE` varchar(2) NOT NULL COMMENT '스케줄타입[SYS019]',
  `BASE_YN` varchar(2) COMMENT '기본스케줄여부[SYS003]',
  `APPLY_DATE` varchar(8) NOT NULL COMMENT '적용일자',
  `FST_SCH_STR_TIME` varchar(4) NOT NULL COMMENT '1구간 시작시간',
  `FST_SCH_END_TIME` varchar(4) NOT NULL COMMENT '1구간 종료시간',
  `FST_SCH_BRK_MIN` varchar(3) COMMENT '1구간 휴게시간',
  `FST_BRK_STR_TIME` varchar(4) COMMENT '1구간 휴게 시작(HHMM)',
  `FST_BRK_END_TIME` varchar(4) COMMENT '1구간 휴게 종료(HHMM)',
  `SEC_SCH_STR_TIME` varchar(4) COMMENT '2구간 시작시간',
  `SEC_SCH_END_TIME` varchar(4) COMMENT '2구간 종료시간',
  `SEC_SCH_BRK_MIN` varchar(3) COMMENT '2구간 휴게시간',
  `SEC_BRK_STR_TIME` varchar(4) COMMENT '2구간 휴게 시작(HHMM)',
  `SEC_BRK_END_TIME` varchar(4) COMMENT '2구간 휴게 종료(HHMM)',
  `USE_YN` varchar(2) NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `SCH_CD`),
  KEY `IX_TB_SCH_MGMT_LIST` (`CMPNY_CD`, `SITE_CD`, `USE_YN`, `SCH_TYPE`, `SCH_CD`)
) COMMENT='사업장 근무타입 관리';
```

<a id="tbschmgmthist"></a>
## `tb_sch_mgmt_hist` — 사업장 근무타입 이력관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `HIST_IDX` | int | — | PK |  | 이력시퀀스 |
| `SCH_CD` | varchar(20) | — | PK |  | 스케줄 코드 |
| `APPLY_DATE` | varchar(8) | — |  |  | 적용일자 |
| `FST_SCH_STR_TIME` | varchar(4) | — |  |  | 1구간 시작시간 |
| `FST_SCH_END_TIME` | varchar(4) | — |  |  | 1구간 종료시간 |
| `FST_SCH_BRK_MIN` | varchar(3) | Y |  |  | 1구간 휴게시간 |
| `FST_BRK_STR_TIME` | varchar(4) | Y |  |  | 1구간 휴게 시작(HHMM) |
| `FST_BRK_END_TIME` | varchar(4) | Y |  |  | 1구간 휴게 종료(HHMM) |
| `SEC_SCH_STR_TIME` | varchar(4) | Y |  |  | 2구간 시작시간 |
| `SEC_SCH_END_TIME` | varchar(4) | Y |  |  | 2구간 종료시간 |
| `SEC_SCH_BRK_MIN` | varchar(3) | Y |  |  | 2구간 휴게시간 |
| `SEC_BRK_STR_TIME` | varchar(4) | Y |  |  | 2구간 휴게 시작(HHMM) |
| `SEC_BRK_END_TIME` | varchar(4) | Y |  |  | 2구간 휴게 종료(HHMM) |
| `USE_YN` | varchar(2) | — |  | `Y` | 사용여부 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_sch_mgmt_hist` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `HIST_IDX` int NOT NULL COMMENT '이력시퀀스',
  `SCH_CD` varchar(20) NOT NULL COMMENT '스케줄 코드',
  `APPLY_DATE` varchar(8) NOT NULL COMMENT '적용일자',
  `FST_SCH_STR_TIME` varchar(4) NOT NULL COMMENT '1구간 시작시간',
  `FST_SCH_END_TIME` varchar(4) NOT NULL COMMENT '1구간 종료시간',
  `FST_SCH_BRK_MIN` varchar(3) COMMENT '1구간 휴게시간',
  `FST_BRK_STR_TIME` varchar(4) COMMENT '1구간 휴게 시작(HHMM)',
  `FST_BRK_END_TIME` varchar(4) COMMENT '1구간 휴게 종료(HHMM)',
  `SEC_SCH_STR_TIME` varchar(4) COMMENT '2구간 시작시간',
  `SEC_SCH_END_TIME` varchar(4) COMMENT '2구간 종료시간',
  `SEC_SCH_BRK_MIN` varchar(3) COMMENT '2구간 휴게시간',
  `SEC_BRK_STR_TIME` varchar(4) COMMENT '2구간 휴게 시작(HHMM)',
  `SEC_BRK_END_TIME` varchar(4) COMMENT '2구간 휴게 종료(HHMM)',
  `USE_YN` varchar(2) NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `HIST_IDX`, `SCH_CD`),
  KEY `IX_TB_SCH_MGMT_HIST_LIST` (`CMPNY_CD`, `SITE_CD`, `HIST_IDX`, `USE_YN`, `SCH_CD`)
) COMMENT='사업장 근무타입 이력관리';
```

<a id="tbshiftschassignmgmt"></a>
## `tb_shift_sch_assign_mgmt` — 교대 스케줄 배정 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `SHIFT_CD` | varchar(50) | — | PK |  | 교대근무코드 |
| `TEAM_IDX` | tinyint unsigned | — | PK |  | 팀 순번 |
| `DAY_NO` | tinyint unsigned | — | PK |  | 일자(1~SHIFT_CYCLE_DAYS) |
| `ASSIGN_YN` | char(1) | — |  |  | 스케줄유무 |
| `SCH_CD` | varchar(20) | Y |  |  | 스케줄코드 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_shift_sch_assign_mgmt` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) NOT NULL COMMENT '교대근무코드',
  `TEAM_IDX` tinyint unsigned NOT NULL COMMENT '팀 순번',
  `DAY_NO` tinyint unsigned NOT NULL COMMENT '일자(1~SHIFT_CYCLE_DAYS)',
  `ASSIGN_YN` char(1) NOT NULL COMMENT '스케줄유무',
  `SCH_CD` varchar(20) COMMENT '스케줄코드',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `SHIFT_CD`, `TEAM_IDX`, `DAY_NO`),
  KEY `IX_SHIFT_ASSIGN_01` (`CMPNY_CD`, `SITE_CD`, `SHIFT_CD`, `DAY_NO`),
  KEY `IX_SHIFT_ASSIGN_02` (`CMPNY_CD`, `SITE_CD`, `SHIFT_CD`, `TEAM_IDX`)
) COMMENT='교대 스케줄 배정 관리';
```

<a id="tbshiftschmgmt"></a>
## `tb_shift_sch_mgmt` — 교대 스케줄 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `SHIFT_CD` | varchar(50) | — | PK |  | 교대근무코드 |
| `SHIFT_NO` | varchar(50) | — |  |  | 교대근무번호 |
| `SHIFT_PTRN_CNT` | tinyint unsigned | — |  |  | 교대 패턴 수 |
| `SHIFT_TEAM_CNT` | tinyint unsigned | — |  |  | 교대 팀 수 |
| `SHIFT_CYCLE_DAYS` | tinyint unsigned | — |  |  | 근무 교대주기 |
| `USE_YN` | char(1) | — |  | `Y` | 사용여부(Y/N) |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_shift_sch_mgmt` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) NOT NULL COMMENT '교대근무코드',
  `SHIFT_NO` varchar(50) NOT NULL COMMENT '교대근무번호',
  `SHIFT_PTRN_CNT` tinyint unsigned NOT NULL COMMENT '교대 패턴 수',
  `SHIFT_TEAM_CNT` tinyint unsigned NOT NULL COMMENT '교대 팀 수',
  `SHIFT_CYCLE_DAYS` tinyint unsigned NOT NULL COMMENT '근무 교대주기',
  `USE_YN` char(1) NOT NULL DEFAULT 'Y' COMMENT '사용여부(Y/N)',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `SHIFT_CD`),
  KEY `IX_TB_SHIFT_SCH_MGMT_LIST` (`CMPNY_CD`, `SITE_CD`, `SHIFT_NO`)
) COMMENT='교대 스케줄 관리';
```

<a id="tbshiftschptrnmgmt"></a>
## `tb_shift_sch_ptrn_mgmt` — 교대 스케줄 패턴 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `SHIFT_CD` | varchar(50) | — | PK |  | 교대근무코드 |
| `PTRN_IDX` | tinyint unsigned | — | PK |  | 교대패턴 순번 |
| `SCH_CD` | varchar(20) | — |  |  | 스케줄 코드 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_shift_sch_ptrn_mgmt` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) NOT NULL COMMENT '교대근무코드',
  `PTRN_IDX` tinyint unsigned NOT NULL COMMENT '교대패턴 순번',
  `SCH_CD` varchar(20) NOT NULL COMMENT '스케줄 코드',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `SHIFT_CD`, `PTRN_IDX`),
  KEY `IX_TB_SHIFT_SCH_PTRN_MGMT_LIST` (`CMPNY_CD`, `SITE_CD`, `SHIFT_CD`, `SCH_CD`)
) COMMENT='교대 스케줄 패턴 관리';
```

<a id="tbshiftschteammetainfo"></a>
## `tb_shift_sch_team_meta_info` — 교대근무 팀 메타 정보

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `SHIFT_CD` | varchar(50) | — | PK |  | 교대근무코드 |
| `TEAM_IDX` | tinyint unsigned | — | PK |  | 팀 순번 |
| `TEAM_NM` | varchar(1) | — |  |  | 팀명 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_shift_sch_team_meta_info` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) NOT NULL COMMENT '교대근무코드',
  `TEAM_IDX` tinyint unsigned NOT NULL COMMENT '팀 순번',
  `TEAM_NM` varchar(1) NOT NULL COMMENT '팀명',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `SHIFT_CD`, `TEAM_IDX`)
) COMMENT='교대근무 팀 메타 정보';
```

<a id="tbshiftschteammgmt"></a>
## `tb_shift_sch_team_mgmt` — 교대근무 팀 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `SHIFT_CD` | varchar(50) | — | PK |  | 교대근무코드 |
| `SHIFT_TEAM_ID` | varchar(12) | — | PK |  | 교대근무팀ID |
| `SHIFT_TEAM_NM` | varchar(100) | Y |  |  | 교대근무팀명 |
| `STR_DATE` | varchar(8) | Y |  |  | 시작일자 |
| `END_DATE` | varchar(8) | Y |  |  | 종료일자 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_shift_sch_team_mgmt` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) NOT NULL COMMENT '교대근무코드',
  `SHIFT_TEAM_ID` varchar(12) NOT NULL COMMENT '교대근무팀ID',
  `SHIFT_TEAM_NM` varchar(100) COMMENT '교대근무팀명',
  `STR_DATE` varchar(8) COMMENT '시작일자',
  `END_DATE` varchar(8) COMMENT '종료일자',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `SHIFT_CD`, `SHIFT_TEAM_ID`),
  KEY `IDX_SHIFT_SCH_TEAM_MGMT_01` (`CMPNY_CD`, `SITE_CD`, `SHIFT_CD`),
  KEY `IDX_SHIFT_SCH_TEAM_MGMT_02` (`CMPNY_CD`, `SITE_CD`, `STR_DATE`, `END_DATE`)
) COMMENT='교대근무 팀 관리';
```

<a id="tbshiftschteamuser"></a>
## `tb_shift_sch_team_user` — 교대근무 팀 소속 사용자 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `SHIFT_CD` | varchar(50) | — | PK |  | 교대근무코드 |
| `SHIFT_TEAM_ID` | varchar(12) | — | PK |  | 교대근무팀ID |
| `TEAM_IDX` | tinyint unsigned | — |  |  | 팀 순번 |
| `USER_CD` | varchar(50) | — | PK |  | 사용자코드 |
| `LEADER_YN` | varchar(50) | — |  |  | 팀리더여부 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_shift_sch_team_user` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) NOT NULL COMMENT '교대근무코드',
  `SHIFT_TEAM_ID` varchar(12) NOT NULL COMMENT '교대근무팀ID',
  `TEAM_IDX` tinyint unsigned NOT NULL COMMENT '팀 순번',
  `USER_CD` varchar(50) NOT NULL COMMENT '사용자코드',
  `LEADER_YN` varchar(50) NOT NULL COMMENT '팀리더여부',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `SHIFT_CD`, `SHIFT_TEAM_ID`, `USER_CD`),
  KEY `IDX_SHIFT_TEAM_USER_01` (`CMPNY_CD`, `SITE_CD`, `USER_CD`)
) COMMENT='교대근무 팀 소속 사용자 관리';
```

<a id="tbuserworkplan"></a>
## `tb_user_work_plan` — 사용자 근무 계획

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `USER_CD` | varchar(20) | — | PK |  | 사용자코드 |
| `WORK_YMD` | varchar(8) | — | PK |  | 근무일 |
| `WORK_PLAN_CD` | varchar(20) | Y |  |  | 근무계획코드[SCH_CD, LEAVE_CD] |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_user_work_plan` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자코드',
  `WORK_YMD` varchar(8) NOT NULL COMMENT '근무일',
  `WORK_PLAN_CD` varchar(20) COMMENT '근무계획코드[SCH_CD, LEAVE_CD]',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `USER_CD`, `WORK_YMD`),
  KEY `IX_WORK_PLAN_SITE` (`CMPNY_CD`, `SITE_CD`),
  KEY `IX_WORK_PLAN_USER` (`CMPNY_CD`, `USER_CD`),
  KEY `IX_WORK_PLAN_USER_YMD` (`CMPNY_CD`, `USER_CD`, `WORK_YMD`)
) COMMENT='사용자 근무 계획';
```


# 6. 근태(출퇴근/정산/마감/요청)

<a id="tbattdclose"></a>
## `tb_attd_close` — 근태 마감 상태 (회사+사업장+월)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사 코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장 코드 |
| `NODE_CD` | varchar(50) | — | PK | `*` | 마감 대상 부서 노드 (전체 사업장 마감은 '*') |
| `INC_SUB_YN` | varchar(1) | — |  | `Y` | 하위부서 포함 여부 (Y 포함 / N 해당부서만) |
| `CLOSE_YM` | char(6) | — | PK |  | 마감 기준월 (YYYYMM) |
| `CLOSE_STATUS` | varchar(10) | — |  | `OPEN` | 마감 상태 (OPEN 미마감 / CLOSED 마감) |
| `CLOSE_DTIME` | datetime | Y |  |  | 마감 일시 |
| `CLOSE_USER_CD` | varchar(20) | Y |  |  | 마감자 사용자 코드 |
| `UNCLOSE_DTIME` | datetime | Y |  |  | 마감 해제 일시 |
| `UNCLOSE_USER_CD` | varchar(20) | Y |  |  | 마감 해제자 사용자 코드 |
| `CLOSE_DESC` | varchar(500) | Y |  |  | 마감/해제 사유 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_attd_close` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장 코드',
  `NODE_CD` varchar(50) NOT NULL DEFAULT '*' COMMENT '마감 대상 부서 노드 (전체 사업장 마감은 ''*'')',
  `INC_SUB_YN` varchar(1) NOT NULL DEFAULT 'Y' COMMENT '하위부서 포함 여부 (Y 포함 / N 해당부서만)',
  `CLOSE_YM` char(6) NOT NULL COMMENT '마감 기준월 (YYYYMM)',
  `CLOSE_STATUS` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '마감 상태 (OPEN 미마감 / CLOSED 마감)',
  `CLOSE_DTIME` datetime COMMENT '마감 일시',
  `CLOSE_USER_CD` varchar(20) COMMENT '마감자 사용자 코드',
  `UNCLOSE_DTIME` datetime COMMENT '마감 해제 일시',
  `UNCLOSE_USER_CD` varchar(20) COMMENT '마감 해제자 사용자 코드',
  `CLOSE_DESC` varchar(500) COMMENT '마감/해제 사유',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `NODE_CD`, `CLOSE_YM`)
) COMMENT='근태 마감 상태 (회사+사업장+월)';
```

<a id="tbattdclosehist"></a>
## `tb_attd_close_hist` — 근태 마감/해제 이력

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `HIST_ID` | varchar(20) | — | PK |  | 이력 ID (FNC_CMM_SEQ_NEXTVAL, PK) |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사 코드 |
| `SITE_CD` | varchar(50) | — |  |  | 사업장 코드 |
| `CLOSE_YM` | char(6) | — |  |  | 마감 기준월 (YYYYMM) |
| `NODE_CD` | varchar(50) | — |  | `*` | 마감 대상 부서 노드 (전체 사업장 마감은 '*') |
| `INC_SUB_YN` | varchar(1) | — |  | `Y` | 하위부서 포함 여부 (Y 포함 / N 해당부서만) |
| `ACTION_TYPE` | varchar(10) | — |  |  | 액션 (CLOSE 마감 / UNCLOSE 해제) |
| `ACTION_USER_CD` | varchar(20) | Y |  |  | 액션 수행자 |
| `ACTION_DTIME` | datetime | — |  | `CURRENT_TIMESTAMP` | 액션 일시 |
| `ACTION_DESC` | varchar(500) | Y |  |  | 액션 사유 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_attd_close_hist` (
  `HIST_ID` varchar(20) NOT NULL COMMENT '이력 ID (FNC_CMM_SEQ_NEXTVAL, PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장 코드',
  `CLOSE_YM` char(6) NOT NULL COMMENT '마감 기준월 (YYYYMM)',
  `NODE_CD` varchar(50) NOT NULL DEFAULT '*' COMMENT '마감 대상 부서 노드 (전체 사업장 마감은 ''*'')',
  `INC_SUB_YN` varchar(1) NOT NULL DEFAULT 'Y' COMMENT '하위부서 포함 여부 (Y 포함 / N 해당부서만)',
  `ACTION_TYPE` varchar(10) NOT NULL COMMENT '액션 (CLOSE 마감 / UNCLOSE 해제)',
  `ACTION_USER_CD` varchar(20) COMMENT '액션 수행자',
  `ACTION_DTIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '액션 일시',
  `ACTION_DESC` varchar(500) COMMENT '액션 사유',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`, `HIST_ID`),
  KEY `IX_TB_ATTD_CLOSE_HIST` (`CMPNY_CD`, `SITE_CD`, `CLOSE_YM`, `ACTION_DTIME`)
) COMMENT='근태 마감/해제 이력';
```

<a id="tbattdstdtimerule"></a>
## `tb_attd_std_time_rule` — 출퇴근 시간 표준화 규칙

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `STD_TIME_RULE_TYPE` | varchar(2) | — | PK |  | 시간 표준화 적용 타입[SYS028] |
| `STD_TIME_TYPE` | varchar(2) | — |  |  | 시간 표준화 타입[SYS029] |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_attd_std_time_rule` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `STD_TIME_RULE_TYPE` varchar(2) NOT NULL COMMENT '시간 표준화 적용 타입[SYS028]',
  `STD_TIME_TYPE` varchar(2) NOT NULL COMMENT '시간 표준화 타입[SYS029]',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `STD_TIME_RULE_TYPE`)
) COMMENT='출퇴근 시간 표준화 규칙';
```

<a id="tbattdstdtimerulehis"></a>
## `tb_attd_std_time_rule_his` — 출퇴근 시간 표준화 규칙 변경 이력

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `HIST_IDX` | varchar(20) | — | PK |  | 이력IDX |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `STD_TIME_RULE_TYPE` | varchar(2) | — |  |  | 시간 표준화 적용 타입[SYS028] |
| `STD_TIME_TYPE` | varchar(2) | — |  |  | 시간 표준화 타입[SYS029] |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_attd_std_time_rule_his` (
  `HIST_IDX` varchar(20) NOT NULL COMMENT '이력IDX',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `STD_TIME_RULE_TYPE` varchar(2) NOT NULL COMMENT '시간 표준화 적용 타입[SYS028]',
  `STD_TIME_TYPE` varchar(2) NOT NULL COMMENT '시간 표준화 타입[SYS029]',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`HIST_IDX`, `CMPNY_CD`)
) COMMENT='출퇴근 시간 표준화 규칙 변경 이력';
```

<a id="tbuserattdgps"></a>
## `tb_user_attd_gps` — 근태 GPS 기록

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `GPS_ID` | varchar(20) | — | PK |  | GPS고유ID |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `ATTD_ID` | varchar(20) | — |  |  | 근태고유ID |
| `SITE_CD` | varchar(50) | — |  |  | 사업장코드 |
| `USER_CD` | varchar(20) | — |  |  | 사용자코드 |
| `GPS_INFO_TYPE` | varchar(2) | Y |  |  | GPS정보타입[SYS028] |
| `LAT` | decimal(10,7) | — |  |  | 위도 |
| `LON` | decimal(10,7) | — |  |  | 경도 |
| `ACCURACY` | decimal(7,2) | Y |  |  | 정확도(m, 임계값 검증용) |
| `API_CALL_DATE` | varchar(8) | — |  |  | 측정일자(YYYYMMDD) |
| `API_CALL_TIME` | varchar(6) | — |  |  | 측정시간(HHmmss) |
| `IS_MOCKED` | char(1) | — |  | `N` | Mock위치여부(Y/N) |
| `IP_ADDR` | varchar(45) | Y |  |  | IP주소(IPv6 대응) |
| `OFFSITE_REASON` | varchar(500) | Y |  |  | 외근(근무지 외) 사유 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_user_attd_gps` (
  `GPS_ID` varchar(20) NOT NULL COMMENT 'GPS고유ID',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `ATTD_ID` varchar(20) NOT NULL COMMENT '근태고유ID',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자코드',
  `GPS_INFO_TYPE` varchar(2) COMMENT 'GPS정보타입[SYS028]',
  `LAT` decimal(10,7) NOT NULL COMMENT '위도',
  `LON` decimal(10,7) NOT NULL COMMENT '경도',
  `ACCURACY` decimal(7,2) COMMENT '정확도(m, 임계값 검증용)',
  `API_CALL_DATE` varchar(8) NOT NULL COMMENT '측정일자(YYYYMMDD)',
  `API_CALL_TIME` varchar(6) NOT NULL COMMENT '측정시간(HHmmss)',
  `IS_MOCKED` char(1) NOT NULL DEFAULT 'N' COMMENT 'Mock위치여부(Y/N)',
  `IP_ADDR` varchar(45) COMMENT 'IP주소(IPv6 대응)',
  `OFFSITE_REASON` varchar(500) COMMENT '외근(근무지 외) 사유',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`, `GPS_ID`),
  KEY `idx_gps_abnormal` (`CMPNY_CD`, `API_CALL_DATE`),
  KEY `idx_gps_attd` (`CMPNY_CD`, `ATTD_ID`),
  KEY `idx_gps_search` (`CMPNY_CD`, `SITE_CD`, `API_CALL_DATE`),
  KEY `idx_gps_user` (`CMPNY_CD`, `USER_CD`, `API_CALL_DATE`)
) COMMENT='근태 GPS 기록';
```

<a id="tbuserattdhist"></a>
## `tb_user_attd_hist` — 근태 처리 이력

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `HIST_ID` | varchar(20) | — | PK |  | 이력고유ID |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사코드 |
| `ATTD_ID` | varchar(20) | — |  |  | 근태고유ID |
| `SITE_CD` | varchar(50) | — |  |  | 사업장코드 |
| `HIST_TYPE` | varchar(10) | — |  |  | 이력구분[SYS032] |
| `PROCESS_REASON` | varchar(500) | Y |  |  | 처리사유 |
| `WORK_YMD` | varchar(8) | Y |  |  | 근무일 |
| `BEF_CHECK_IN_DATE` | varchar(8) | Y |  |  | 변경전 출근일자 |
| `BEF_CHECK_IN_TIME` | varchar(4) | Y |  |  | 변경전 출근시간 |
| `BEF_CHECK_OUT_DATE` | varchar(8) | Y |  |  | 변경전 퇴근일자 |
| `BEF_CHECK_OUT_TIME` | varchar(4) | Y |  |  | 변경전 퇴근시간 |
| `AFT_CHECK_IN_DATE` | varchar(8) | Y |  |  | 변경후 출근일자 |
| `AFT_CHECK_IN_TIME` | varchar(4) | Y |  |  | 변경후 출근시간 |
| `AFT_CHECK_OUT_DATE` | varchar(8) | Y |  |  | 변경후 퇴근일자 |
| `AFT_CHECK_OUT_TIME` | varchar(4) | Y |  |  | 변경후 퇴근시간 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_user_attd_hist` (
  `HIST_ID` varchar(20) NOT NULL COMMENT '이력고유ID',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `ATTD_ID` varchar(20) NOT NULL COMMENT '근태고유ID',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `HIST_TYPE` varchar(10) NOT NULL COMMENT '이력구분[SYS032]',
  `PROCESS_REASON` varchar(500) COMMENT '처리사유',
  `WORK_YMD` varchar(8) COMMENT '근무일',
  `BEF_CHECK_IN_DATE` varchar(8) COMMENT '변경전 출근일자',
  `BEF_CHECK_IN_TIME` varchar(4) COMMENT '변경전 출근시간',
  `BEF_CHECK_OUT_DATE` varchar(8) COMMENT '변경전 퇴근일자',
  `BEF_CHECK_OUT_TIME` varchar(4) COMMENT '변경전 퇴근시간',
  `AFT_CHECK_IN_DATE` varchar(8) COMMENT '변경후 출근일자',
  `AFT_CHECK_IN_TIME` varchar(4) COMMENT '변경후 출근시간',
  `AFT_CHECK_OUT_DATE` varchar(8) COMMENT '변경후 퇴근일자',
  `AFT_CHECK_OUT_TIME` varchar(4) COMMENT '변경후 퇴근시간',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`HIST_ID`),
  KEY `IDX_ATTD_HIST_ATTD` (`CMPNY_CD`, `ATTD_ID`),
  KEY `IDX_ATTD_HIST_SITE` (`CMPNY_CD`, `SITE_CD`)
) COMMENT='근태 처리 이력';
```

<a id="tbuserattdmgmt"></a>
## `tb_user_attd_mgmt` — 근태관리 (출퇴근 원장/정산)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `ATTD_ID` | varchar(20) | — | PK |  | 근태고유ID |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사코드 |
| `SITE_CD` | varchar(50) | — |  |  | 사업장코드 |
| `USER_CD` | varchar(20) | — |  |  | 사용자코드 |
| `WORK_YMD` | varchar(8) | — |  |  | 근무일 |
| `NODE_CD` | varchar(50) | Y |  |  | 소속부서 |
| `WORK_SEQ` | int | — |  |  | 근무차수 |
| `CHECK_IN_DATE` | varchar(8) | — |  |  | 출근일자 |
| `CHECK_IN_TIME` | varchar(4) | — |  |  | 출근시간 |
| `CHECK_IN_METHOD` | varchar(2) | — |  |  | 출근방법[SYS031] |
| `CHECK_OUT_DATE` | varchar(8) | Y |  |  | 퇴근일자 |
| `CHECK_OUT_TIME` | varchar(4) | Y |  |  | 퇴근시간 |
| `CHECK_OUT_METHOD` | varchar(2) | Y |  |  | 퇴근방법[SYS031] |
| `DEL_YN` | varchar(1) | — |  |  | 삭제여부 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_user_attd_mgmt` (
  `ATTD_ID` varchar(20) NOT NULL COMMENT '근태고유ID',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자코드',
  `WORK_YMD` varchar(8) NOT NULL COMMENT '근무일',
  `NODE_CD` varchar(50) COMMENT '소속부서',
  `WORK_SEQ` int NOT NULL COMMENT '근무차수',
  `CHECK_IN_DATE` varchar(8) NOT NULL COMMENT '출근일자',
  `CHECK_IN_TIME` varchar(4) NOT NULL COMMENT '출근시간',
  `CHECK_IN_METHOD` varchar(2) NOT NULL COMMENT '출근방법[SYS031]',
  `CHECK_OUT_DATE` varchar(8) COMMENT '퇴근일자',
  `CHECK_OUT_TIME` varchar(4) COMMENT '퇴근시간',
  `CHECK_OUT_METHOD` varchar(2) COMMENT '퇴근방법[SYS031]',
  `DEL_YN` varchar(1) NOT NULL COMMENT '삭제여부',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`ATTD_ID`),
  KEY `IDX_ATTD_NODE_DATGE` (`CMPNY_CD`, `NODE_CD`, `WORK_YMD`, `DEL_YN`),
  KEY `IDX_ATTD_SITE_DATE` (`CMPNY_CD`, `SITE_CD`, `WORK_YMD`, `NODE_CD`, `DEL_YN`),
  KEY `IDX_ATTD_USER_DATE` (`CMPNY_CD`, `SITE_CD`, `USER_CD`, `WORK_YMD`, `DEL_YN`)
) COMMENT='근태관리 (출퇴근 원장/정산)';
```

<a id="tbuserattdreq"></a>
## `tb_user_attd_req` — 사용자 근태 관련 요청 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `REQ_ID` | varchar(20) | — | PK |  | 요청 ID (PK) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사 코드 |
| `SITE_CD` | varchar(50) | — |  |  | 사업장 코드 |
| `USER_CD` | varchar(20) | — |  |  | 요청자 사용자 코드 |
| `REQ_TYPE` | varchar(20) | — |  |  | 요청 유형 (SYS032: 01근태생성/02근태수정/03초과근무생성/04초과근무수정/05연차사용/06연차수정) |
| `TARGET_ID` | varchar(20) | Y | IDX |  | 수정 대상 ID (수정 요청 시: ATTD_ID / OT_ID / LEAVE_ID, 생성 요청 시 NULL) |
| `REQ_STATUS` | varchar(10) | — |  |  | 요청 상태 (SYS033: 01신청/02승인/03반려/04취소) |
| `REQ_REASON` | varchar(500) | Y |  |  | 요청 사유 |
| `WORK_YMD` | varchar(8) | Y |  |  | 근무 일자 (YYYYMMDD) |
| `NODE_CD` | varchar(50) | Y |  |  | 근무 노드 코드 |
| `WORK_SEQ` | int | Y |  |  | 근무 순번 |
| `START_DATE` | varchar(8) | Y |  |  | 시작 일자 (YYYYMMDD) |
| `START_TIME` | varchar(4) | Y |  |  | 시작 시각 (HHMM) |
| `END_DATE` | varchar(8) | Y |  |  | 종료 일자 (YYYYMMDD) |
| `END_TIME` | varchar(4) | Y |  |  | 종료 시각 (HHMM) |
| `OT_TYPE` | varchar(10) | Y |  |  | 초과근무 유형 (EXTEND:연장 / NIGHT:야간 / HOLIDAY:휴일) |
| `LEAVE_TYPE` | varchar(10) | Y |  |  | 연차 유형 (ANNUAL:연차 / HALF_AM:오전반차 / HALF_PM:오후반차 / SICK:병가 / FAMILY:경조사 등) |
| `LEAVE_DAYS` | decimal(8,5) | Y |  |  | 사용 일수(시간차 환산) |
| `PROCESS_USER_CD` | varchar(20) | Y |  |  | 처리자 사용자 코드 |
| `PROCESS_COMMENT` | varchar(500) | Y |  |  | 처리 코멘트 |
| `PROCESS_DATE` | datetime | Y |  |  | 처리 일시 |
| `DEL_YN` | varchar(1) | — |  | `N` | 삭제 여부 |
| `INSERT_NO` | varchar(50) | — |  |  | 등록자 |
| `INSERT_DATE` | datetime | — |  |  | 등록 일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정 일시 |

```sql
CREATE TABLE `tb_user_attd_req` (
  `REQ_ID` varchar(20) NOT NULL COMMENT '요청 ID (PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장 코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '요청자 사용자 코드',
  `REQ_TYPE` varchar(20) NOT NULL COMMENT '요청 유형 (SYS032: 01근태생성/02근태수정/03초과근무생성/04초과근무수정/05연차사용/06연차수정)',
  `TARGET_ID` varchar(20) COMMENT '수정 대상 ID (수정 요청 시: ATTD_ID / OT_ID / LEAVE_ID, 생성 요청 시 NULL)',
  `REQ_STATUS` varchar(10) NOT NULL COMMENT '요청 상태 (SYS033: 01신청/02승인/03반려/04취소)',
  `REQ_REASON` varchar(500) COMMENT '요청 사유',
  `WORK_YMD` varchar(8) COMMENT '근무 일자 (YYYYMMDD)',
  `NODE_CD` varchar(50) COMMENT '근무 노드 코드',
  `WORK_SEQ` int COMMENT '근무 순번',
  `START_DATE` varchar(8) COMMENT '시작 일자 (YYYYMMDD)',
  `START_TIME` varchar(4) COMMENT '시작 시각 (HHMM)',
  `END_DATE` varchar(8) COMMENT '종료 일자 (YYYYMMDD)',
  `END_TIME` varchar(4) COMMENT '종료 시각 (HHMM)',
  `OT_TYPE` varchar(10) COMMENT '초과근무 유형 (EXTEND:연장 / NIGHT:야간 / HOLIDAY:휴일)',
  `LEAVE_TYPE` varchar(10) COMMENT '연차 유형 (ANNUAL:연차 / HALF_AM:오전반차 / HALF_PM:오후반차 / SICK:병가 / FAMILY:경조사 등)',
  `LEAVE_DAYS` decimal(8,5) COMMENT '사용 일수(시간차 환산)',
  `PROCESS_USER_CD` varchar(20) COMMENT '처리자 사용자 코드',
  `PROCESS_COMMENT` varchar(500) COMMENT '처리 코멘트',
  `PROCESS_DATE` datetime COMMENT '처리 일시',
  `DEL_YN` varchar(1) NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정 일시',
  PRIMARY KEY (`REQ_ID`),
  KEY `IDX_ATTD_REQ_STATUS` (`CMPNY_CD`, `SITE_CD`, `REQ_STATUS`, `REQ_TYPE`),
  KEY `IDX_ATTD_REQ_TARGET` (`TARGET_ID`),
  KEY `IDX_ATTD_REQ_USER` (`CMPNY_CD`, `SITE_CD`, `USER_CD`, `REQ_STATUS`),
  KEY `IDX_ATTD_REQ_WORK_YMD` (`CMPNY_CD`, `SITE_CD`, `WORK_YMD`)
) COMMENT='사용자 근태 관련 요청 관리';
```

<a id="tbuserattdreqapproval"></a>
## `tb_user_attd_req_approval` — 연차 요청별 결재라인 (사용자 정의)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `REQ_ID` | varchar(20) | — | PK |  | 연관 요청 (tb_user_attd_req.REQ_ID) |
| `APPROVAL_STEP` | int | — | PK |  | 결재 단계 (1부터) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사 코드 |
| `APPROVER_USER_CD` | varchar(20) | — | IDX |  | 지정 결재자 |
| `APPROVAL_STATUS` | varchar(2) | — |  | `00` | 단계 상태 [SYS044] |
| `APPROVAL_COMMENT` | varchar(500) | Y |  |  | 결재 코멘트 |
| `APPROVAL_DATE` | datetime | Y |  |  | 처리 일시 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` |  |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` |  |
| `UPDATE_NO` | varchar(50) | Y |  |  |  |
| `UPDATE_DATE` | datetime | Y |  |  |  |

```sql
CREATE TABLE `tb_user_attd_req_approval` (
  `REQ_ID` varchar(20) NOT NULL COMMENT '연관 요청 (tb_user_attd_req.REQ_ID)',
  `APPROVAL_STEP` int NOT NULL COMMENT '결재 단계 (1부터)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `APPROVER_USER_CD` varchar(20) NOT NULL COMMENT '지정 결재자',
  `APPROVAL_STATUS` varchar(2) NOT NULL DEFAULT '00' COMMENT '단계 상태 [SYS044]',
  `APPROVAL_COMMENT` varchar(500) COMMENT '결재 코멘트',
  `APPROVAL_DATE` datetime COMMENT '처리 일시',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50),
  `UPDATE_DATE` datetime,
  PRIMARY KEY (`REQ_ID`, `APPROVAL_STEP`),
  KEY `IX_TB_USER_ATTD_REQ_APPROVAL_APPROVER` (`APPROVER_USER_CD`, `APPROVAL_STATUS`),
  KEY `IX_TB_USER_ATTD_REQ_APPROVAL_REQ` (`CMPNY_CD`, `REQ_ID`)
) COMMENT='연차 요청별 결재라인 (사용자 정의)';
```

<a id="tbuserovertimemgmt"></a>
## `tb_user_overtime_mgmt` — 사용자 초과근무 실적 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `OT_ID` | varchar(20) | — | PK |  | 초과근무 ID (PK) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사 코드 |
| `SITE_CD` | varchar(50) | — |  |  | 사업장 코드 |
| `USER_CD` | varchar(20) | — |  |  | 근무자 사용자 코드 |
| `ATTD_ID` | varchar(20) | Y | IDX |  | 연관 근태 ID (tb_user_attd_mgmt.ATTD_ID, 휴일근무 등 정규근태 없는 경우 NULL) |
| `REQ_ID` | varchar(20) | Y | IDX |  | 연관 요청 ID (tb_user_attd_req.REQ_ID, 사후 등록 시 NULL) |
| `WORK_YMD` | varchar(8) | — |  |  | 근무 일자 (YYYYMMDD) |
| `NODE_CD` | varchar(50) | Y |  |  | 근무 노드 코드 |
| `OT_TYPE` | varchar(10) | — |  |  | 초과근무 유형 (EXTEND:연장 / NIGHT:야간 / HOLIDAY:휴일) |
| `PLAN_START_DATE` | varchar(8) | Y |  |  | 계획 시작 일자 (YYYYMMDD) |
| `PLAN_START_TIME` | varchar(4) | Y |  |  | 계획 시작 시각 (HHMM) |
| `PLAN_END_DATE` | varchar(8) | Y |  |  | 계획 종료 일자 (YYYYMMDD) |
| `PLAN_END_TIME` | varchar(4) | Y |  |  | 계획 종료 시각 (HHMM) |
| `ACTUAL_START_DATE` | varchar(8) | — |  |  | 실제 시작 일자 (YYYYMMDD) |
| `ACTUAL_START_TIME` | varchar(4) | — |  |  | 실제 시작 시각 (HHMM) |
| `ACTUAL_START_METHOD` | varchar(2) | Y |  |  | 시작 체크 방식 (GPS/QR/MANUAL 등) |
| `ACTUAL_END_DATE` | varchar(8) | Y |  |  | 실제 종료 일자 (YYYYMMDD) |
| `ACTUAL_END_TIME` | varchar(4) | Y |  |  | 실제 종료 시각 (HHMM) |
| `ACTUAL_END_METHOD` | varchar(2) | Y |  |  | 종료 체크 방식 (GPS/QR/MANUAL 등) |
| `WORK_MINUTES` | int | Y |  |  | 실제 근무 시간 (분 단위, 휴게시간 제외) |
| `BREAK_MINUTES` | int | Y |  | `0` | 휴게 시간 (분 단위) |
| `OT_STATUS` | varchar(10) | — |  |  | 초과근무 상태 (IN_PROGRESS:진행중 / COMPLETED:완료 / CANCELLED:취소) |
| `DEL_YN` | varchar(1) | — |  | `N` | 삭제 여부 |
| `INSERT_NO` | varchar(50) | — |  |  | 등록자 |
| `INSERT_DATE` | datetime | — |  |  | 등록 일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정 일시 |

```sql
CREATE TABLE `tb_user_overtime_mgmt` (
  `OT_ID` varchar(20) NOT NULL COMMENT '초과근무 ID (PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장 코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '근무자 사용자 코드',
  `ATTD_ID` varchar(20) COMMENT '연관 근태 ID (tb_user_attd_mgmt.ATTD_ID, 휴일근무 등 정규근태 없는 경우 NULL)',
  `REQ_ID` varchar(20) COMMENT '연관 요청 ID (tb_user_attd_req.REQ_ID, 사후 등록 시 NULL)',
  `WORK_YMD` varchar(8) NOT NULL COMMENT '근무 일자 (YYYYMMDD)',
  `NODE_CD` varchar(50) COMMENT '근무 노드 코드',
  `OT_TYPE` varchar(10) NOT NULL COMMENT '초과근무 유형 (EXTEND:연장 / NIGHT:야간 / HOLIDAY:휴일)',
  `PLAN_START_DATE` varchar(8) COMMENT '계획 시작 일자 (YYYYMMDD)',
  `PLAN_START_TIME` varchar(4) COMMENT '계획 시작 시각 (HHMM)',
  `PLAN_END_DATE` varchar(8) COMMENT '계획 종료 일자 (YYYYMMDD)',
  `PLAN_END_TIME` varchar(4) COMMENT '계획 종료 시각 (HHMM)',
  `ACTUAL_START_DATE` varchar(8) NOT NULL COMMENT '실제 시작 일자 (YYYYMMDD)',
  `ACTUAL_START_TIME` varchar(4) NOT NULL COMMENT '실제 시작 시각 (HHMM)',
  `ACTUAL_START_METHOD` varchar(2) COMMENT '시작 체크 방식 (GPS/QR/MANUAL 등)',
  `ACTUAL_END_DATE` varchar(8) COMMENT '실제 종료 일자 (YYYYMMDD)',
  `ACTUAL_END_TIME` varchar(4) COMMENT '실제 종료 시각 (HHMM)',
  `ACTUAL_END_METHOD` varchar(2) COMMENT '종료 체크 방식 (GPS/QR/MANUAL 등)',
  `WORK_MINUTES` int COMMENT '실제 근무 시간 (분 단위, 휴게시간 제외)',
  `BREAK_MINUTES` int DEFAULT 0 COMMENT '휴게 시간 (분 단위)',
  `OT_STATUS` varchar(10) NOT NULL COMMENT '초과근무 상태 (IN_PROGRESS:진행중 / COMPLETED:완료 / CANCELLED:취소)',
  `DEL_YN` varchar(1) NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정 일시',
  PRIMARY KEY (`OT_ID`),
  KEY `IDX_OT_ATTD` (`ATTD_ID`),
  KEY `IDX_OT_REQ` (`REQ_ID`),
  KEY `IDX_OT_SITE_YMD` (`CMPNY_CD`, `SITE_CD`, `WORK_YMD`, `OT_STATUS`),
  KEY `IDX_OT_USER_YMD` (`CMPNY_CD`, `SITE_CD`, `USER_CD`, `WORK_YMD`)
) COMMENT='사용자 초과근무 실적 관리';
```


# 7. 연차·휴가·결재라인

<a id="tbaprvlinepreset"></a>
## `tb_aprv_line_preset` — 연차 결재라인 프리셋 (사용자별 마스터)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사 코드 |
| `PRESET_ID` | varchar(20) | — | PK |  | 프리셋 ID (회사별 채번: P + YYYYMMDD + SEQ) |
| `USER_CD` | varchar(20) | — |  |  | 소유 사용자 (본인 프리셋) |
| `PRESET_NM` | varchar(100) | — |  |  | 프리셋 이름 |
| `DEFAULT_YN` | char(1) | — |  | `N` | 기본 프리셋 여부 (사용자당 최대 1개) |
| `USE_YN` | char(1) | — |  | `Y` | 사용 여부 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_aprv_line_preset` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `PRESET_ID` varchar(20) NOT NULL COMMENT '프리셋 ID (회사별 채번: P + YYYYMMDD + SEQ)',
  `USER_CD` varchar(20) NOT NULL COMMENT '소유 사용자 (본인 프리셋)',
  `PRESET_NM` varchar(100) NOT NULL COMMENT '프리셋 이름',
  `DEFAULT_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '기본 프리셋 여부 (사용자당 최대 1개)',
  `USE_YN` char(1) NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `PRESET_ID`),
  KEY `IX_TB_APRV_LINE_PRESET_OWNER` (`CMPNY_CD`, `USER_CD`, `USE_YN`)
) COMMENT='연차 결재라인 프리셋 (사용자별 마스터)';
```

<a id="tbaprvlinepresetd"></a>
## `tb_aprv_line_preset_d` — 연차 결재라인 프리셋 디테일 (결재 순서)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사 코드 |
| `PRESET_ID` | varchar(20) | — | PK |  | 프리셋 ID (tb_aprv_line_preset.PRESET_ID) |
| `STEP_NO` | int | — | PK |  | 결재 단계 순서 (1부터) |
| `APPROVER_USER_CD` | varchar(20) | — | IDX |  | 지정 결재자 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_aprv_line_preset_d` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `PRESET_ID` varchar(20) NOT NULL COMMENT '프리셋 ID (tb_aprv_line_preset.PRESET_ID)',
  `STEP_NO` int NOT NULL COMMENT '결재 단계 순서 (1부터)',
  `APPROVER_USER_CD` varchar(20) NOT NULL COMMENT '지정 결재자',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `PRESET_ID`, `STEP_NO`),
  KEY `IX_TB_APRV_LINE_PRESET_D_APPROVER` (`APPROVER_USER_CD`)
) COMMENT='연차 결재라인 프리셋 디테일 (결재 순서)';
```

<a id="tbleavepolicy"></a>
## `tb_leave_policy` — 회사 법정 연차 부여 정책 (7개 axis)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `POLICY_SEQ` | bigint | — | PK AI |  | 정책 일련번호 (PK) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사 코드 |
| `POLICY_PRESET` | varchar(30) | — |  |  | 프리셋: HIRE_DATE/FISCAL_PRORATE/FISCAL_MONTHLY/HIRE_DATE_PREGRANT/CUSTOM |
| `AXIS1_GRANT_BASE` | varchar(20) | — |  |  | 1번: HIRE_DATE/FISCAL_YEAR [SYS036] |
| `AXIS2_FISCAL_START_MM` | char(2) | Y |  |  | 2번: 회계연도 시작월 (01~12) |
| `AXIS2_FISCAL_START_DD` | char(2) | Y |  |  | 2번: 회계연도 시작일 (01~31) |
| `AXIS3_FIRST_YEAR_METHOD` | varchar(30) | — |  |  | 3번: MONTHLY_ONLY/PRORATE/NEXT_YEAR_BULK [SYS037] |
| `AXIS3_PREGRANT_YN` | char(1) | — |  | `N` | 3번 보조: 입사일 일괄선부여 여부 (프리셋 4번 표현) |
| `AXIS4_PRORATE_ROUNDING` | varchar(20) | — |  | `CEIL` | 4번: CEIL/ROUND/FLOOR/HALF_DAY [SYS038] (AXIS3=PRORATE 시만 유효, 그 외는 CEIL 강제) |
| `AXIS5_TENURE_MODE` | varchar(10) | — |  | `LEGAL` | 5번: LEGAL/CUSTOM |
| `AXIS5_START_YEAR` | int | — |  | `3` | 5번: 가산 시작 연차 (1~3, LEGAL 시 3 강제) |
| `AXIS5_INTERVAL` | int | — |  | `2` | 5번: 가산 주기 (1~2, LEGAL 시 2 강제) |
| `AXIS5_MAX_DAYS` | int | — |  | `25` | 5번: 최대 연차일수 (25 이상, 법정) |
| `AXIS6_VALIDITY_MONTHS` | int | — |  | `12` | 6번: 유효기간(개월) 12 또는 24 |
| `AXIS7_USE_PROMOTION` | char(1) | — |  | `N` | 7번: 사용촉진 Y/N |
| `APRV_USE_YN` | char(1) | — |  | `N` | 법정연차 신청 결재 여부 (Y: 결재라인, N: 즉시확정) |
| `USE_YN` | char(1) | — |  | `Y` | 활성 여부 (회사당 Y는 1건, 서비스 레이어에서 보장) |
| `APPLY_FROM_DATE` | varchar(8) | — |  |  | 정책 적용 시작일 (YYYYMMDD) |
| `INSERT_NO` | varchar(50) | — |  |  |  |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` |  |
| `UPDATE_NO` | varchar(50) | Y |  |  |  |
| `UPDATE_DATE` | datetime | Y |  |  |  |

```sql
CREATE TABLE `tb_leave_policy` (
  `POLICY_SEQ` bigint NOT NULL AUTO_INCREMENT COMMENT '정책 일련번호 (PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `POLICY_PRESET` varchar(30) NOT NULL COMMENT '프리셋: HIRE_DATE/FISCAL_PRORATE/FISCAL_MONTHLY/HIRE_DATE_PREGRANT/CUSTOM',
  `AXIS1_GRANT_BASE` varchar(20) NOT NULL COMMENT '1번: HIRE_DATE/FISCAL_YEAR [SYS036]',
  `AXIS2_FISCAL_START_MM` char(2) COMMENT '2번: 회계연도 시작월 (01~12)',
  `AXIS2_FISCAL_START_DD` char(2) COMMENT '2번: 회계연도 시작일 (01~31)',
  `AXIS3_FIRST_YEAR_METHOD` varchar(30) NOT NULL COMMENT '3번: MONTHLY_ONLY/PRORATE/NEXT_YEAR_BULK [SYS037]',
  `AXIS3_PREGRANT_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '3번 보조: 입사일 일괄선부여 여부 (프리셋 4번 표현)',
  `AXIS4_PRORATE_ROUNDING` varchar(20) NOT NULL DEFAULT 'CEIL' COMMENT '4번: CEIL/ROUND/FLOOR/HALF_DAY [SYS038] (AXIS3=PRORATE 시만 유효, 그 외는 CEIL 강제)',
  `AXIS5_TENURE_MODE` varchar(10) NOT NULL DEFAULT 'LEGAL' COMMENT '5번: LEGAL/CUSTOM',
  `AXIS5_START_YEAR` int NOT NULL DEFAULT 3 COMMENT '5번: 가산 시작 연차 (1~3, LEGAL 시 3 강제)',
  `AXIS5_INTERVAL` int NOT NULL DEFAULT 2 COMMENT '5번: 가산 주기 (1~2, LEGAL 시 2 강제)',
  `AXIS5_MAX_DAYS` int NOT NULL DEFAULT 25 COMMENT '5번: 최대 연차일수 (25 이상, 법정)',
  `AXIS6_VALIDITY_MONTHS` int NOT NULL DEFAULT 12 COMMENT '6번: 유효기간(개월) 12 또는 24',
  `AXIS7_USE_PROMOTION` char(1) NOT NULL DEFAULT 'N' COMMENT '7번: 사용촉진 Y/N',
  `APRV_USE_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '법정연차 신청 결재 여부 (Y: 결재라인, N: 즉시확정)',
  `USE_YN` char(1) NOT NULL DEFAULT 'Y' COMMENT '활성 여부 (회사당 Y는 1건, 서비스 레이어에서 보장)',
  `APPLY_FROM_DATE` varchar(8) NOT NULL COMMENT '정책 적용 시작일 (YYYYMMDD)',
  `INSERT_NO` varchar(50) NOT NULL,
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50),
  `UPDATE_DATE` datetime,
  PRIMARY KEY (`POLICY_SEQ`),
  KEY `IX_TB_LEAVE_POLICY_ACTIVE` (`CMPNY_CD`, `USE_YN`, `APPLY_FROM_DATE`),
  UNIQUE KEY `UX_TB_LEAVE_POLICY_ACTIVE` (`None`)
) COMMENT='회사 법정 연차 부여 정책 (7개 axis)';
```

<a id="tbleavepolicyhistory"></a>
## `tb_leave_policy_history` — 연차 정책 변경 이력

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `HIST_ID` | varchar(20) | — | PK |  | 이력 ID (PK) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사 코드 |
| `POLICY_SEQ` | bigint | — |  |  | 변경된 TB_LEAVE_POLICY.POLICY_SEQ |
| `CHANGE_TYPE` | varchar(20) | — |  |  | CREATE/UPDATE/PRESET_CHANGE |
| `PREV_SNAPSHOT` | json | Y |  |  | 변경 전 정책 전체 스냅샷 |
| `NEW_SNAPSHOT` | json | — |  |  | 변경 후 정책 전체 스냅샷 |
| `CHANGE_REASON` | varchar(500) | Y |  |  |  |
| `IMPACT_SUMMARY` | json | Y |  |  | 영향 분석 결과 (영향 인원, 추가 부담) |
| `INSERT_NO` | varchar(50) | — |  |  |  |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` |  |

```sql
CREATE TABLE `tb_leave_policy_history` (
  `HIST_ID` varchar(20) NOT NULL COMMENT '이력 ID (PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `POLICY_SEQ` bigint NOT NULL COMMENT '변경된 TB_LEAVE_POLICY.POLICY_SEQ',
  `CHANGE_TYPE` varchar(20) NOT NULL COMMENT 'CREATE/UPDATE/PRESET_CHANGE',
  `PREV_SNAPSHOT` json COMMENT '변경 전 정책 전체 스냅샷',
  `NEW_SNAPSHOT` json NOT NULL COMMENT '변경 후 정책 전체 스냅샷',
  `CHANGE_REASON` varchar(500),
  `IMPACT_SUMMARY` json COMMENT '영향 분석 결과 (영향 인원, 추가 부담)',
  `INSERT_NO` varchar(50) NOT NULL,
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`HIST_ID`),
  KEY `IX_TB_LEAVE_POLICY_HIST` (`CMPNY_CD`, `INSERT_DATE`)
) COMMENT='연차 정책 변경 이력';
```

<a id="tbleavetypemgmt"></a>
## `tb_leave_type_mgmt` — 연차(휴가) 타입 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `LEAVE_CD` | varchar(20) | — | PK |  | 연차코드 |
| `LEAVE_NO` | varchar(20) | — |  |  | 연차번호 |
| `LEAVE_NM` | varchar(200) | — |  |  | 연차명 |
| `LEAVE_TYPE` | char(2) | — |  |  | 연차타입[SYS021] |
| `GRANT_TYPE` | char(2) | Y |  |  | 부여방식[SYS022] |
| `PAID_TYPE` | char(2) | — |  |  | 유급구분[SYS023] |
| `LEAVE_NATURE_TYPE` | varchar(2) | — |  |  | 휴가성격[SYS024] |
| `USE_YN` | char(1) | Y |  | `Y` | 사용여부 |
| `SYSTEM_YN` | char(1) | — |  | `N` | 시스템 시드 여부 (Y: PRAFTA-018 법정 연차용, 화면 편집 불가) |
| `LEAVE_DESC` | varchar(500) | Y |  |  | 비고 |
| `MAX_APLY_DAYS` | tinyint unsigned | Y |  |  | 최대 신청일수 |
| `USE_UNIT_TYPE` | varchar(2) | Y |  |  | 연차 사용 단위[SYS025] |
| `AVAIL_TERM_TYPE` | varchar(2) | Y |  |  | 연차 사용가능기간 타입[SYS026] |
| `AVAIL_FROM_DT` | varchar(4) | Y |  |  | 연차 사용기간 FROM |
| `AVAIL_TO_DT` | varchar(4) | Y |  |  | 연차 사용기간 TO |
| `GRANT_DAYS` | tinyint unsigned | Y |  |  | 부여일 수 |
| `ADMIN_AVAIL_TERM_TYPE` | varchar(2) | Y |  |  | 관리자 부여 연차 사용가능기간 타입[SYS026] |
| `ADMIN_AVAIL_FROM_DT` | varchar(6) | Y |  |  | 관리자 부여 연차 사용기간 FROM |
| `ADMIN_AVAIL_TO_DT` | varchar(6) | Y |  |  | 관리자 부여 연차 사용기간 TO |
| `GRANT_BASE_TYPE` | varchar(2) | Y |  |  | 자동 부여 기준일[SYS027] |
| `GRANT_OFFSET_MONTH` | tinyint unsigned | Y |  |  | 자동부여 실행시점 |
| `GRANT_ASSIGN_MMDD` | char(4) | Y |  |  | 자동부여 지정일 MMDD (기준일=03 부여일지정 시 필수) |
| `APRV_USE_YN` | char(1) | Y |  | `N` | 결재여부 |
| `EVIDENCE_YN` | char(1) | Y |  | `N` | 증빙여부 |
| `EVIDENCE_GUIDE_MSG` | varchar(500) | Y |  |  | 증빙안내문구 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_leave_type_mgmt` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `LEAVE_CD` varchar(20) NOT NULL COMMENT '연차코드',
  `LEAVE_NO` varchar(20) NOT NULL COMMENT '연차번호',
  `LEAVE_NM` varchar(200) NOT NULL COMMENT '연차명',
  `LEAVE_TYPE` char(2) NOT NULL COMMENT '연차타입[SYS021]',
  `GRANT_TYPE` char(2) COMMENT '부여방식[SYS022]',
  `PAID_TYPE` char(2) NOT NULL COMMENT '유급구분[SYS023]',
  `LEAVE_NATURE_TYPE` varchar(2) NOT NULL COMMENT '휴가성격[SYS024]',
  `USE_YN` char(1) DEFAULT 'Y' COMMENT '사용여부',
  `SYSTEM_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '시스템 시드 여부 (Y: PRAFTA-018 법정 연차용, 화면 편집 불가)',
  `LEAVE_DESC` varchar(500) COMMENT '비고',
  `MAX_APLY_DAYS` tinyint unsigned COMMENT '최대 신청일수',
  `USE_UNIT_TYPE` varchar(2) COMMENT '연차 사용 단위[SYS025]',
  `AVAIL_TERM_TYPE` varchar(2) COMMENT '연차 사용가능기간 타입[SYS026]',
  `AVAIL_FROM_DT` varchar(4) COMMENT '연차 사용기간 FROM',
  `AVAIL_TO_DT` varchar(4) COMMENT '연차 사용기간 TO',
  `GRANT_DAYS` tinyint unsigned COMMENT '부여일 수',
  `ADMIN_AVAIL_TERM_TYPE` varchar(2) COMMENT '관리자 부여 연차 사용가능기간 타입[SYS026]',
  `ADMIN_AVAIL_FROM_DT` varchar(6) COMMENT '관리자 부여 연차 사용기간 FROM',
  `ADMIN_AVAIL_TO_DT` varchar(6) COMMENT '관리자 부여 연차 사용기간 TO',
  `GRANT_BASE_TYPE` varchar(2) COMMENT '자동 부여 기준일[SYS027]',
  `GRANT_OFFSET_MONTH` tinyint unsigned COMMENT '자동부여 실행시점',
  `GRANT_ASSIGN_MMDD` char(4) COMMENT '자동부여 지정일 MMDD (기준일=03 부여일지정 시 필수)',
  `APRV_USE_YN` char(1) DEFAULT 'N' COMMENT '결재여부',
  `EVIDENCE_YN` char(1) DEFAULT 'N' COMMENT '증빙여부',
  `EVIDENCE_GUIDE_MSG` varchar(500) COMMENT '증빙안내문구',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `LEAVE_CD`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_01` (`CMPNY_CD`, `LEAVE_NO`, `USE_YN`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_02` (`CMPNY_CD`, `LEAVE_NO`, `USE_YN`, `LEAVE_TYPE`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_03` (`CMPNY_CD`, `LEAVE_NO`, `USE_YN`, `LEAVE_TYPE`, `GRANT_TYPE`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_SYSTEM` (`CMPNY_CD`, `SYSTEM_YN`, `USE_YN`)
) COMMENT='연차(휴가) 타입 관리';
```

<a id="tbleaveusagepolicy"></a>
## `tb_leave_usage_policy` — 연차 사용 단위 정책 (휴게시간/시간단위 시작시각/1일환산은 시스템 강제)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `POLICY_SEQ` | bigint | — | PK |  | TB_LEAVE_POLICY.POLICY_SEQ 1:1 |
| `CMPNY_CD` | varchar(50) | — |  |  | 회사 코드 |
| `ALLOW_FULL_DAY` | char(1) | — |  | `Y` | 1일 단위 (항상 Y, 변경불가) |
| `ALLOW_HALF_DAY` | char(1) | — |  | `Y` | 0.5일 단위 (AXIS4=HALF_DAY 시 Y 강제) |
| `ALLOW_HOUR_2` | char(1) | — |  | `N` | 시간차 2시간 허용 (SYS025-02) |
| `ALLOW_HOUR_1` | char(1) | — |  | `N` | 시간차 1시간 허용 (SYS025-03) |
| `ALLOW_MIN_30` | char(1) | — |  | `N` | 시간차 30분 허용 (SYS025-04) |
| `USAGE_UNIT` | varchar(20) | — |  | `FULL_DAY` | ?뚯궗 ?덉슜 ?ъ슜 ?⑥쐞 (?⑥씪): FULL_DAY/HALF_DAY/HOUR_2/HOUR_1/MIN_30 |
| `INSERT_NO` | varchar(50) | — |  |  |  |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` |  |
| `UPDATE_NO` | varchar(50) | Y |  |  |  |
| `UPDATE_DATE` | datetime | Y |  |  |  |

```sql
CREATE TABLE `tb_leave_usage_policy` (
  `POLICY_SEQ` bigint NOT NULL COMMENT 'TB_LEAVE_POLICY.POLICY_SEQ 1:1',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `ALLOW_FULL_DAY` char(1) NOT NULL DEFAULT 'Y' COMMENT '1일 단위 (항상 Y, 변경불가)',
  `ALLOW_HALF_DAY` char(1) NOT NULL DEFAULT 'Y' COMMENT '0.5일 단위 (AXIS4=HALF_DAY 시 Y 강제)',
  `ALLOW_HOUR_2` char(1) NOT NULL DEFAULT 'N' COMMENT '시간차 2시간 허용 (SYS025-02)',
  `ALLOW_HOUR_1` char(1) NOT NULL DEFAULT 'N' COMMENT '시간차 1시간 허용 (SYS025-03)',
  `ALLOW_MIN_30` char(1) NOT NULL DEFAULT 'N' COMMENT '시간차 30분 허용 (SYS025-04)',
  `USAGE_UNIT` varchar(20) NOT NULL DEFAULT 'FULL_DAY' COMMENT '?뚯궗 ?덉슜 ?ъ슜 ?⑥쐞 (?⑥씪): FULL_DAY/HALF_DAY/HOUR_2/HOUR_1/MIN_30',
  `INSERT_NO` varchar(50) NOT NULL,
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50),
  `UPDATE_DATE` datetime,
  PRIMARY KEY (`POLICY_SEQ`)
) COMMENT='연차 사용 단위 정책 (휴게시간/시간단위 시작시각/1일환산은 시스템 강제)';
```

<a id="tbuserhiredatehistory"></a>
## `tb_user_hire_date_history` — 입사일 변경 이력 (노무 감사용)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `HIST_ID` | varchar(20) | — | PK |  | 이력 ID (PK) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사 코드 |
| `USER_CD` | varchar(20) | — |  |  | 사용자 코드 |
| `PREV_HIRE_DATE` | varchar(8) | — |  |  | 변경 전 입사일 |
| `NEW_HIRE_DATE` | varchar(8) | — |  |  | 변경 후 입사일 |
| `CHANGE_REASON` | varchar(1000) | — |  |  | 변경 사유 (자유 텍스트, 필수) |
| `HANDLING_TYPE` | varchar(30) | — |  |  | 처리 방식[SYS039] KEEP_AND_BACKFILL/KEEP_AND_APPLY_NEW/RESET_ALL |
| `AFFECTED_GRANT_SNAPSHOT` | json | Y |  |  | 영향받은 부여 이력 스냅샷 |
| `OLD_GRANT_TOTAL` | decimal(5,1) | Y |  |  | 변경 전 법정 부여 총량 (수동 조정 추적, MANUAL 한정) |
| `NEW_GRANT_TOTAL` | decimal(5,1) | Y |  |  | 변경 후 목표 법정 부여 총량 (수동 조정 추적, MANUAL 한정) |
| `WITHDRAW_REASON` | varchar(500) | Y |  |  | 회수 사유 (차액<0 회수 발생 시 필수, MANUAL 한정) |
| `APPLIED_YN` | char(1) | — |  | `N` | ?뺤콉 湲곗? 遺?뿬 ?곸슜 ?꾨즺 ?щ? (Attd_09 遺?뿬 踰꾪듉?먯꽌 ?곸슜 ??Y) |
| `APPLIED_DATE` | datetime | Y |  |  | ?곸슜 ?쇱떆 |
| `APPLIED_BY` | varchar(50) | Y |  |  | ?곸슜 ?섑뻾??(USER_CD) |
| `INSERT_NO` | varchar(50) | — |  |  | 변경자 (AUTH_MASTER 또는 AUTH_HR_MANAGER) |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 변경일시 |

```sql
CREATE TABLE `tb_user_hire_date_history` (
  `HIST_ID` varchar(20) NOT NULL COMMENT '이력 ID (PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자 코드',
  `PREV_HIRE_DATE` varchar(8) NOT NULL COMMENT '변경 전 입사일',
  `NEW_HIRE_DATE` varchar(8) NOT NULL COMMENT '변경 후 입사일',
  `CHANGE_REASON` varchar(1000) NOT NULL COMMENT '변경 사유 (자유 텍스트, 필수)',
  `HANDLING_TYPE` varchar(30) NOT NULL COMMENT '처리 방식[SYS039] KEEP_AND_BACKFILL/KEEP_AND_APPLY_NEW/RESET_ALL',
  `AFFECTED_GRANT_SNAPSHOT` json COMMENT '영향받은 부여 이력 스냅샷',
  `OLD_GRANT_TOTAL` decimal(5,1) COMMENT '변경 전 법정 부여 총량 (수동 조정 추적, MANUAL 한정)',
  `NEW_GRANT_TOTAL` decimal(5,1) COMMENT '변경 후 목표 법정 부여 총량 (수동 조정 추적, MANUAL 한정)',
  `WITHDRAW_REASON` varchar(500) COMMENT '회수 사유 (차액<0 회수 발생 시 필수, MANUAL 한정)',
  `APPLIED_YN` char(1) NOT NULL DEFAULT 'N' COMMENT '?뺤콉 湲곗? 遺?뿬 ?곸슜 ?꾨즺 ?щ? (Attd_09 遺?뿬 踰꾪듉?먯꽌 ?곸슜 ??Y)',
  `APPLIED_DATE` datetime COMMENT '?곸슜 ?쇱떆',
  `APPLIED_BY` varchar(50) COMMENT '?곸슜 ?섑뻾??(USER_CD)',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '변경자 (AUTH_MASTER 또는 AUTH_HR_MANAGER)',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경일시',
  PRIMARY KEY (`HIST_ID`),
  KEY `IX_TB_HIRE_HIST_USER` (`CMPNY_CD`, `USER_CD`, `INSERT_DATE`)
) COMMENT='입사일 변경 이력 (노무 감사용)';
```

<a id="tbuserleavegrant"></a>
## `tb_user_leave_grant` — 사용자 연차 부여 이력

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `GRANT_ID` | varchar(20) | — | PK |  | 부여 ID (PK) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사 코드 |
| `USER_CD` | varchar(20) | — |  |  | 사용자 코드 |
| `LEAVE_CD` | varchar(20) | — |  |  | 연차 코드 (tb_leave_type_mgmt.LEAVE_CD) |
| `GRANT_TYPE` | varchar(40) | Y |  |  | 부여 분류[SYS035] STATUTORY_*/MANUAL_* prefix (법정/약정 구분) |
| `GRANT_DAYS` | decimal(5,1) | — |  |  | 부여 일수 (반차 0.5 단위 고려) |
| `USED_DAYS` | decimal(8,5) | — |  | `0.00000` | 사용 일수 캐시 (tb_user_leave_use 합계와 동기화) |
| `GRANT_REASON` | varchar(500) | Y |  |  | 부여 사유 (자동부여/관리자수동/특별부여 등) |
| `GRANT_BY_TYPE` | varchar(2) | — |  |  | 부여 방식 (AUTO:자동 / ADMIN:관리자수동) |
| `POLICY_SEQ` | bigint | Y |  |  | 적용 정책 (TB_LEAVE_POLICY.POLICY_SEQ, 수동 부여는 NULL) |
| `GRANT_DATE` | varchar(8) | — |  |  | 부여 일자 (YYYYMMDD) |
| `AVAIL_FROM_DATE` | varchar(8) | — |  |  | 사용 가능 시작일 (YYYYMMDD) |
| `AVAIL_TO_DATE` | varchar(8) | — |  |  | 사용 가능 종료일 (YYYYMMDD, 소멸일) |
| `IDEMPOTENCY_KEY` | varchar(100) | Y |  |  | 중복 부여 방지 키 ({USER_CD}_{YYYY}_ANNUAL 등). 자동부여 시 필수 |
| `STATUS` | varchar(20) | — |  | `ACTIVE` | 상태[SYS040] ACTIVE/EXHAUSTED/EXPIRED/CANCELED (EXPIRE_YN과 동기화 — 정책서 §8.5.8) |
| `EXPIRE_YN` | varchar(1) | — |  | `N` | 소멸 여부 (배치로 AVAIL_TO_DATE 경과 시 Y) |
| `EXPIRE_DATE` | datetime | Y |  |  | 소멸 처리 일시 |
| `CANCEL_REASON` | varchar(500) | Y |  |  | 회수(취소) 사유 |
| `CANCEL_DATE` | datetime | Y |  |  | 회수(취소) 일시 |
| `CANCEL_BY` | varchar(50) | Y |  |  | 회수 수행자 (USER_CD) |
| `DEL_YN` | varchar(1) | — |  | `N` | 삭제 여부 |
| `INSERT_NO` | varchar(50) | — |  |  | 등록자 |
| `INSERT_DATE` | datetime | — |  |  | 등록 일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정 일시 |

```sql
CREATE TABLE `tb_user_leave_grant` (
  `GRANT_ID` varchar(20) NOT NULL COMMENT '부여 ID (PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자 코드',
  `LEAVE_CD` varchar(20) NOT NULL COMMENT '연차 코드 (tb_leave_type_mgmt.LEAVE_CD)',
  `GRANT_TYPE` varchar(40) COMMENT '부여 분류[SYS035] STATUTORY_*/MANUAL_* prefix (법정/약정 구분)',
  `GRANT_DAYS` decimal(5,1) NOT NULL COMMENT '부여 일수 (반차 0.5 단위 고려)',
  `USED_DAYS` decimal(8,5) NOT NULL DEFAULT 0.00000 COMMENT '사용 일수 캐시 (tb_user_leave_use 합계와 동기화)',
  `GRANT_REASON` varchar(500) COMMENT '부여 사유 (자동부여/관리자수동/특별부여 등)',
  `GRANT_BY_TYPE` varchar(2) NOT NULL COMMENT '부여 방식 (AUTO:자동 / ADMIN:관리자수동)',
  `POLICY_SEQ` bigint COMMENT '적용 정책 (TB_LEAVE_POLICY.POLICY_SEQ, 수동 부여는 NULL)',
  `GRANT_DATE` varchar(8) NOT NULL COMMENT '부여 일자 (YYYYMMDD)',
  `AVAIL_FROM_DATE` varchar(8) NOT NULL COMMENT '사용 가능 시작일 (YYYYMMDD)',
  `AVAIL_TO_DATE` varchar(8) NOT NULL COMMENT '사용 가능 종료일 (YYYYMMDD, 소멸일)',
  `IDEMPOTENCY_KEY` varchar(100) COMMENT '중복 부여 방지 키 ({USER_CD}_{YYYY}_ANNUAL 등). 자동부여 시 필수',
  `STATUS` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '상태[SYS040] ACTIVE/EXHAUSTED/EXPIRED/CANCELED (EXPIRE_YN과 동기화 — 정책서 §8.5.8)',
  `EXPIRE_YN` varchar(1) NOT NULL DEFAULT 'N' COMMENT '소멸 여부 (배치로 AVAIL_TO_DATE 경과 시 Y)',
  `EXPIRE_DATE` datetime COMMENT '소멸 처리 일시',
  `CANCEL_REASON` varchar(500) COMMENT '회수(취소) 사유',
  `CANCEL_DATE` datetime COMMENT '회수(취소) 일시',
  `CANCEL_BY` varchar(50) COMMENT '회수 수행자 (USER_CD)',
  `DEL_YN` varchar(1) NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정 일시',
  PRIMARY KEY (`GRANT_ID`),
  KEY `IDX_LEAVE_GRANT_AVAIL` (`CMPNY_CD`, `USER_CD`, `AVAIL_TO_DATE`, `EXPIRE_YN`),
  KEY `IDX_LEAVE_GRANT_TYPE` (`CMPNY_CD`, `LEAVE_CD`),
  KEY `IDX_LEAVE_GRANT_USER` (`CMPNY_CD`, `USER_CD`, `LEAVE_CD`, `EXPIRE_YN`),
  KEY `IX_LEAVE_GRANT_GTYPE` (`CMPNY_CD`, `GRANT_TYPE`, `GRANT_DATE`),
  KEY `IX_LEAVE_GRANT_STATUS` (`CMPNY_CD`, `USER_CD`, `STATUS`, `AVAIL_TO_DATE`),
  UNIQUE KEY `UK_LEAVE_GRANT_IDEMPOTENCY` (`CMPNY_CD`, `IDEMPOTENCY_KEY`)
) COMMENT='사용자 연차 부여 이력';
```

<a id="tbuserleaveuse"></a>
## `tb_user_leave_use` — 사용자 연차 사용 실적

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `LEAVE_ID` | varchar(20) | — | PK |  | 연차 사용 ID (PK) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사 코드 |
| `SITE_CD` | varchar(50) | — |  |  | 사업장 코드 |
| `USER_CD` | varchar(20) | — |  |  | 사용자 코드 |
| `LEAVE_CD` | varchar(20) | — |  |  | 연차 코드 (tb_leave_type_mgmt.LEAVE_CD) |
| `REQ_ID` | varchar(20) | Y | IDX |  | 연관 요청 ID (tb_user_attd_req.REQ_ID, 결재 미사용 또는 사후 등록 시 NULL) |
| `GRANT_ID` | varchar(20) | Y | IDX |  | 차감 대상 부여 ID (tb_user_leave_grant.GRANT_ID) |
| `START_DATE` | varchar(8) | — |  |  | 사용 시작일 (YYYYMMDD) |
| `START_TIME` | varchar(4) | Y |  |  | 시작 시각 (HHMM, 시간단위 휴가 시) |
| `END_DATE` | varchar(8) | — |  |  | 사용 종료일 (YYYYMMDD) |
| `END_TIME` | varchar(4) | Y |  |  | 종료 시각 (HHMM, 시간단위 휴가 시) |
| `USE_UNIT_TYPE` | varchar(2) | — |  |  | 사용 단위 (tb_leave_type_mgmt.USE_UNIT_TYPE 복사, SYS025) |
| `LEAVE_DAYS` | decimal(8,5) | — |  |  | 사용 일수 (시간차 동적 환산) |
| `LEAVE_MINUTES` | int | Y |  |  | 사용 분 (시간단위 휴가 시) |
| `LEAVE_REASON` | varchar(500) | Y |  |  | 사용 사유 |
| `EVIDENCE_FILE_ID` | varchar(50) | Y |  |  | 증빙 파일 ID (tb_leave_type_mgmt.EVIDENCE_YN=Y 시) |
| `LEAVE_STATUS` | varchar(10) | — |  |  | 사용 상태 (CONFIRMED:확정 / CANCELLED:취소) |
| `CANCEL_REASON` | varchar(500) | Y |  |  | 취소 사유 |
| `CANCEL_DATE` | datetime | Y |  |  | 취소 일시 |
| `DEL_YN` | varchar(1) | — |  | `N` | 삭제 여부 |
| `INSERT_NO` | varchar(50) | — |  |  | 등록자 |
| `INSERT_DATE` | datetime | — |  |  | 등록 일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정 일시 |
| `DIRECT_USE_KEY` | varchar(80) | Y |  |  | 직접 차감(결재 없음) 멱등 키 — 결재경유/취소건은 NULL |

```sql
CREATE TABLE `tb_user_leave_use` (
  `LEAVE_ID` varchar(20) NOT NULL COMMENT '연차 사용 ID (PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장 코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자 코드',
  `LEAVE_CD` varchar(20) NOT NULL COMMENT '연차 코드 (tb_leave_type_mgmt.LEAVE_CD)',
  `REQ_ID` varchar(20) COMMENT '연관 요청 ID (tb_user_attd_req.REQ_ID, 결재 미사용 또는 사후 등록 시 NULL)',
  `GRANT_ID` varchar(20) COMMENT '차감 대상 부여 ID (tb_user_leave_grant.GRANT_ID)',
  `START_DATE` varchar(8) NOT NULL COMMENT '사용 시작일 (YYYYMMDD)',
  `START_TIME` varchar(4) COMMENT '시작 시각 (HHMM, 시간단위 휴가 시)',
  `END_DATE` varchar(8) NOT NULL COMMENT '사용 종료일 (YYYYMMDD)',
  `END_TIME` varchar(4) COMMENT '종료 시각 (HHMM, 시간단위 휴가 시)',
  `USE_UNIT_TYPE` varchar(2) NOT NULL COMMENT '사용 단위 (tb_leave_type_mgmt.USE_UNIT_TYPE 복사, SYS025)',
  `LEAVE_DAYS` decimal(8,5) NOT NULL COMMENT '사용 일수 (시간차 동적 환산)',
  `LEAVE_MINUTES` int COMMENT '사용 분 (시간단위 휴가 시)',
  `LEAVE_REASON` varchar(500) COMMENT '사용 사유',
  `EVIDENCE_FILE_ID` varchar(50) COMMENT '증빙 파일 ID (tb_leave_type_mgmt.EVIDENCE_YN=Y 시)',
  `LEAVE_STATUS` varchar(10) NOT NULL COMMENT '사용 상태 (CONFIRMED:확정 / CANCELLED:취소)',
  `CANCEL_REASON` varchar(500) COMMENT '취소 사유',
  `CANCEL_DATE` datetime COMMENT '취소 일시',
  `DEL_YN` varchar(1) NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정 일시',
  `DIRECT_USE_KEY` varchar(80) STORED GENERATED COMMENT '직접 차감(결재 없음) 멱등 키 — 결재경유/취소건은 NULL',
  PRIMARY KEY (`LEAVE_ID`),
  KEY `IDX_LEAVE_USE_GRANT` (`GRANT_ID`),
  KEY `IDX_LEAVE_USE_REQ` (`REQ_ID`),
  KEY `IDX_LEAVE_USE_SITE` (`CMPNY_CD`, `SITE_CD`, `START_DATE`, `LEAVE_STATUS`),
  KEY `IDX_LEAVE_USE_TYPE` (`CMPNY_CD`, `LEAVE_CD`),
  KEY `IDX_LEAVE_USE_USER` (`CMPNY_CD`, `USER_CD`, `START_DATE`, `LEAVE_STATUS`),
  UNIQUE KEY `UK_LEAVE_USE_DIRECT` (`CMPNY_CD`, `DIRECT_USE_KEY`)
) COMMENT='사용자 연차 사용 실적';
```

<a id="tbuserservicecredit"></a>
## `tb_user_service_credit` — 사용자 경력 인정 (점진 부여 전용)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CREDIT_ID` | varchar(20) | — | PK |  | 경력 인정 ID (PK) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사 코드 |
| `USER_CD` | varchar(20) | — |  |  | 사용자 코드 |
| `CREDIT_MONTHS` | int | — |  |  | 인정 개월 수 (0 이상) |
| `REASON_TYPE` | varchar(30) | — |  |  | 사유 유형[SYS042] |
| `REASON_DETAIL` | varchar(500) | Y |  |  | 상세 설명 |
| `USE_YN` | char(1) | — |  | `Y` | 사용 여부 |
| `INSERT_NO` | varchar(50) | — |  |  | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_user_service_credit` (
  `CREDIT_ID` varchar(20) NOT NULL COMMENT '경력 인정 ID (PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) NOT NULL COMMENT '사용자 코드',
  `CREDIT_MONTHS` int NOT NULL COMMENT '인정 개월 수 (0 이상)',
  `REASON_TYPE` varchar(30) NOT NULL COMMENT '사유 유형[SYS042]',
  `REASON_DETAIL` varchar(500) COMMENT '상세 설명',
  `USE_YN` char(1) NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CREDIT_ID`),
  KEY `IX_TB_USER_SERVICE_CREDIT_USER` (`CMPNY_CD`, `USER_CD`, `USE_YN`)
) COMMENT='사용자 경력 인정 (점진 부여 전용)';
```


# 8. TBM(작업 전 안전미팅)

<a id="tbtbmattendance"></a>
## `tb_tbm_attendance` — TBM 출결(정규직/일용직 통합)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `ATTENDANCE_CD` | varchar(20) | — | PK |  | 출결코드 (PK, 채번 A+YYYYMMDD+SEQ) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사코드 |
| `SESSION_CD` | varchar(20) | — |  |  | TBM 세션코드 |
| `USER_TYPE_CD` | varchar(20) | — |  |  | 대상유형[SYS050] REGULAR:정규직(TB_USER) DAILY:일용직(TB_DAILY_USER) |
| `USER_CD` | varchar(20) | — |  |  | 대상 USER_CD (유형에 따라 TB_USER 또는 TB_DAILY_USER) |
| `ENTRY_TYPE_CD` | varchar(20) | Y |  |  | 입실경로[SYS051] SELF_DEVICE:본인디바이스 MANAGER_QR_SCAN:관리자QR스캔 |
| `ENTRY_BY_MANAGER_USER_CD` | varchar(20) | Y |  |  | QR 입실 처리 관리자 USER_CD |
| `ENTRY_AT` | datetime | Y |  |  | 입실 시각 |
| `ENTRY_GPS_LAT` | decimal(10,7) | Y |  |  | 입실 위도 |
| `ENTRY_GPS_LON` | decimal(10,7) | Y |  |  | 입실 경도 |
| `ENTRY_DISTANCE_M` | int | Y |  |  | 입실 시 개설지점과의 거리(m) |
| `ENTRY_SIGN_FILE_MGMT_CD` | varchar(50) | Y |  |  | 입실 서명 파일코드 |
| `EXIT_TYPE_CD` | varchar(20) | Y |  |  | 종료경로[SYS052] SELF:본인 MANAGER_QR_SCAN:관리자QR MANAGER_FORCED:관리자강제 |
| `EXIT_BY_MANAGER_USER_CD` | varchar(20) | Y |  |  | 종료 처리 관리자 USER_CD |
| `EXIT_AT` | datetime | Y |  |  | 종료 시각(NULL=미종료) |
| `EXIT_SIGN_FILE_MGMT_CD` | varchar(50) | Y |  |  | 종료 서명 파일코드 |
| `EXIT_FORCED_REASON` | varchar(500) | Y |  |  | 강제종료 사유(관리자 책임 기록) |
| `COMPLETION_STATUS_CD` | varchar(20) | Y |  |  | 이수상태[SYS053] COMPLETED:이수 NOT_COMPLETED:미이수 |
| `NOT_COMPLETED_REASON` | varchar(500) | Y |  |  | 미이수 사유 |
| `STATUS_UPDATED_BY` | varchar(20) | Y |  |  | 이수상태 마지막 변경자 |
| `STATUS_UPDATED_AT` | datetime | Y |  |  | 이수상태 마지막 변경 시각 |
| `DEL_YN` | varchar(2) | — |  | `N` | 삭제여부 Y/N |
| `INSERT_NO` | varchar(50) | — |  |  | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | — |  |  | 수정자 |
| `UPDATE_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 수정일시 |

```sql
CREATE TABLE `tb_tbm_attendance` (
  `ATTENDANCE_CD` varchar(20) NOT NULL COMMENT '출결코드 (PK, 채번 A+YYYYMMDD+SEQ)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) NOT NULL COMMENT 'TBM 세션코드',
  `USER_TYPE_CD` varchar(20) NOT NULL COMMENT '대상유형[SYS050] REGULAR:정규직(TB_USER) DAILY:일용직(TB_DAILY_USER)',
  `USER_CD` varchar(20) NOT NULL COMMENT '대상 USER_CD (유형에 따라 TB_USER 또는 TB_DAILY_USER)',
  `ENTRY_TYPE_CD` varchar(20) COMMENT '입실경로[SYS051] SELF_DEVICE:본인디바이스 MANAGER_QR_SCAN:관리자QR스캔',
  `ENTRY_BY_MANAGER_USER_CD` varchar(20) COMMENT 'QR 입실 처리 관리자 USER_CD',
  `ENTRY_AT` datetime COMMENT '입실 시각',
  `ENTRY_GPS_LAT` decimal(10,7) COMMENT '입실 위도',
  `ENTRY_GPS_LON` decimal(10,7) COMMENT '입실 경도',
  `ENTRY_DISTANCE_M` int COMMENT '입실 시 개설지점과의 거리(m)',
  `ENTRY_SIGN_FILE_MGMT_CD` varchar(50) COMMENT '입실 서명 파일코드',
  `EXIT_TYPE_CD` varchar(20) COMMENT '종료경로[SYS052] SELF:본인 MANAGER_QR_SCAN:관리자QR MANAGER_FORCED:관리자강제',
  `EXIT_BY_MANAGER_USER_CD` varchar(20) COMMENT '종료 처리 관리자 USER_CD',
  `EXIT_AT` datetime COMMENT '종료 시각(NULL=미종료)',
  `EXIT_SIGN_FILE_MGMT_CD` varchar(50) COMMENT '종료 서명 파일코드',
  `EXIT_FORCED_REASON` varchar(500) COMMENT '강제종료 사유(관리자 책임 기록)',
  `COMPLETION_STATUS_CD` varchar(20) COMMENT '이수상태[SYS053] COMPLETED:이수 NOT_COMPLETED:미이수',
  `NOT_COMPLETED_REASON` varchar(500) COMMENT '미이수 사유',
  `STATUS_UPDATED_BY` varchar(20) COMMENT '이수상태 마지막 변경자',
  `STATUS_UPDATED_AT` datetime COMMENT '이수상태 마지막 변경 시각',
  `DEL_YN` varchar(2) NOT NULL DEFAULT 'N' COMMENT '삭제여부 Y/N',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`ATTENDANCE_CD`),
  KEY `IX_TBM_ATTENDANCE_01` (`CMPNY_CD`, `SESSION_CD`),
  KEY `IX_TBM_ATTENDANCE_02` (`CMPNY_CD`, `USER_TYPE_CD`, `USER_CD`),
  UNIQUE KEY `UK_TBM_ATTENDANCE_01` (`CMPNY_CD`, `SESSION_CD`, `USER_TYPE_CD`, `USER_CD`)
) COMMENT='TBM 출결(정규직/일용직 통합)';
```

<a id="tbtbmattendanceevent"></a>
## `tb_tbm_attendance_event` — TBM 출결 이벤트 로그

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `EVENT_NO` | bigint | — | PK AI |  | 이벤트 일련번호 (PK) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사코드 |
| `SESSION_CD` | varchar(20) | — |  |  | TBM 세션코드(비정규화, 조회용) |
| `ATTENDANCE_CD` | varchar(20) | — |  |  | 출결코드 |
| `EVENT_TYPE_CD` | varchar(30) | — |  |  | 이벤트유형[SYS054] ENTER/START/SLIDE_CHANGED/GPS_UPDATED/BACKGROUND_IN/BACKGROUND_OUT/NETWORK_LOST/SIGNATURE_STARTED/END/FORCED_END |
| `EVENT_TIME` | datetime(3) | — |  |  | 이벤트 발생시각(클라이언트 보고, ms) |
| `SERVER_RECEIVED_AT` | datetime(3) | — |  | `CURRENT_TIMESTAMP(3)` | 서버 수신시각(ms, 위조불가 기준) |
| `EVENT_DATA` | json | Y |  |  | 이벤트 부가데이터(JSON) |
| `INSERT_NO` | varchar(50) | — |  |  | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_tbm_attendance_event` (
  `EVENT_NO` bigint NOT NULL AUTO_INCREMENT COMMENT '이벤트 일련번호 (PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) NOT NULL COMMENT 'TBM 세션코드(비정규화, 조회용)',
  `ATTENDANCE_CD` varchar(20) NOT NULL COMMENT '출결코드',
  `EVENT_TYPE_CD` varchar(30) NOT NULL COMMENT '이벤트유형[SYS054] ENTER/START/SLIDE_CHANGED/GPS_UPDATED/BACKGROUND_IN/BACKGROUND_OUT/NETWORK_LOST/SIGNATURE_STARTED/END/FORCED_END',
  `EVENT_TIME` datetime(3) NOT NULL COMMENT '이벤트 발생시각(클라이언트 보고, ms)',
  `SERVER_RECEIVED_AT` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '서버 수신시각(ms, 위조불가 기준)',
  `EVENT_DATA` json COMMENT '이벤트 부가데이터(JSON)',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`EVENT_NO`),
  KEY `IX_TBM_ATT_EVENT_01` (`CMPNY_CD`, `ATTENDANCE_CD`, `EVENT_TIME`),
  KEY `IX_TBM_ATT_EVENT_02` (`CMPNY_CD`, `SESSION_CD`, `EVENT_TYPE_CD`)
) COMMENT='TBM 출결 이벤트 로그';
```

<a id="tbtbmedumtrl"></a>
## `tb_tbm_edu_mtrl` — TBM 교육자료

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `MTRL_CD` | varchar(20) | — | PK |  | 교육자료 코드 |
| `CMPNY_CD` | varchar(10) | — | IDX |  | 회사코드 |
| `SITE_CD` | varchar(50) | Y |  |  | 사업장코드 (NULL=회사공통, 값=해당 사업장 전용) |
| `TITLE` | varchar(200) | — |  |  | 교육자로 제목 |
| `CONTENTS` | varchar(500) | Y |  |  | 교육자료 설명 |
| `MTRL_TYPE` | varchar(8) | Y |  |  | 교육자로 타입 |
| `USE_YN` | char(1) | — |  | `Y` | 사용유무 |
| `INSERT_NO` | varchar(50) | — |  |  | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | — |  |  | 수정자 |
| `UPDATE_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 수정일시 |

```sql
CREATE TABLE `tb_tbm_edu_mtrl` (
  `MTRL_CD` varchar(20) NOT NULL COMMENT '교육자료 코드',
  `CMPNY_CD` varchar(10) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COMMENT '사업장코드 (NULL=회사공통, 값=해당 사업장 전용)',
  `TITLE` varchar(200) NOT NULL COMMENT '교육자로 제목',
  `CONTENTS` varchar(500) COMMENT '교육자료 설명',
  `MTRL_TYPE` varchar(8) COMMENT '교육자로 타입',
  `USE_YN` char(1) NOT NULL DEFAULT 'Y' COMMENT '사용유무',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`MTRL_CD`),
  KEY `IX_TBM_EDU_MTRL_01` (`CMPNY_CD`, `USE_YN`),
  KEY `IX_TBM_EDU_MTRL_02` (`CMPNY_CD`, `SITE_CD`, `USE_YN`)
) COMMENT='TBM 교육자료';
```

<a id="tbtbmedumtrlitem"></a>
## `tb_tbm_edu_mtrl_item` — TBM 교육자료 항목

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `MTRL_ITEM_CD` | varchar(20) | — | PK |  | 교육자료 항목 코드 |
| `MTRL_CD` | varchar(20) | — | IDX |  | 교육자료 코드 |
| `SORT_IDX` | int | — |  | `1` | 정렬순서 |
| `MTRL_ITEM_TYPE` | varchar(2) | — |  |  | 교육자료 항목 타입 |
| `MTRL_DESC` | varchar(500) | Y |  |  | 교육자료 항목 설명 |
| `FILE_MGMT_CD` | varchar(40) | Y |  |  | 파일코드 |
| `THUMB_FILE_MGMT_CD` | varchar(50) | Y |  |  | 썸네일 파일코드 (동영상 첫프레임/PDF 첫페이지/이미지 리사이즈 자동생성) |
| `DURATION_SEC` | int | Y |  |  | 미디어 길이(초) - 동영상만 |
| `URL` | varchar(1000) | Y |  |  | 외부링크 |
| `USE_YN` | char(1) | — |  | `Y` | 사용여부[SYS003] |
| `INSERT_NO` | varchar(50) | — |  |  | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | — |  |  | 수정자 |
| `UPDATE_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 수정일시 |

```sql
CREATE TABLE `tb_tbm_edu_mtrl_item` (
  `MTRL_ITEM_CD` varchar(20) NOT NULL COMMENT '교육자료 항목 코드',
  `MTRL_CD` varchar(20) NOT NULL COMMENT '교육자료 코드',
  `SORT_IDX` int NOT NULL DEFAULT 1 COMMENT '정렬순서',
  `MTRL_ITEM_TYPE` varchar(2) NOT NULL COMMENT '교육자료 항목 타입',
  `MTRL_DESC` varchar(500) COMMENT '교육자료 항목 설명',
  `FILE_MGMT_CD` varchar(40) COMMENT '파일코드',
  `THUMB_FILE_MGMT_CD` varchar(50) COMMENT '썸네일 파일코드 (동영상 첫프레임/PDF 첫페이지/이미지 리사이즈 자동생성)',
  `DURATION_SEC` int COMMENT '미디어 길이(초) - 동영상만',
  `URL` varchar(1000) COMMENT '외부링크',
  `USE_YN` char(1) NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`MTRL_ITEM_CD`),
  KEY `IX_TBM_EDU_MTRL_ITEM_01` (`MTRL_CD`, `USE_YN`, `SORT_IDX`),
  KEY `IX_TBM_EDU_MTRL_ITEM_02` (`MTRL_CD`, `SORT_IDX`)
) COMMENT='TBM 교육자료 항목';
```

<a id="tbtbmpwdfail"></a>
## `tb_tbm_pwd_fail` — TBM 비밀번호 실패 로그

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `FAIL_NO` | bigint | — | PK AI |  | 실패 일련번호 (PK) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사코드 |
| `SESSION_CD` | varchar(20) | — |  |  | TBM 세션코드 |
| `PWD_TYPE_CD` | varchar(10) | — |  |  | 비번유형[SYS055] ENTRY:입실 EXIT:종료 |
| `USER_TYPE_CD` | varchar(20) | Y |  |  | 대상유형[SYS050] REGULAR:정규직 DAILY:일용직 |
| `USER_CD` | varchar(20) | Y |  |  | 시도자 USER_CD(식별 가능 시) |
| `ATTEMPTED_AT` | datetime | — |  | `CURRENT_TIMESTAMP` | 시도 시각 |
| `INSERT_NO` | varchar(50) | Y |  |  | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_tbm_pwd_fail` (
  `FAIL_NO` bigint NOT NULL AUTO_INCREMENT COMMENT '실패 일련번호 (PK)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) NOT NULL COMMENT 'TBM 세션코드',
  `PWD_TYPE_CD` varchar(10) NOT NULL COMMENT '비번유형[SYS055] ENTRY:입실 EXIT:종료',
  `USER_TYPE_CD` varchar(20) COMMENT '대상유형[SYS050] REGULAR:정규직 DAILY:일용직',
  `USER_CD` varchar(20) COMMENT '시도자 USER_CD(식별 가능 시)',
  `ATTEMPTED_AT` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '시도 시각',
  `INSERT_NO` varchar(50) COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`FAIL_NO`),
  KEY `IX_TBM_PWD_FAIL_01` (`CMPNY_CD`, `SESSION_CD`, `ATTEMPTED_AT`)
) COMMENT='TBM 비밀번호 실패 로그';
```

<a id="tbtbmsession"></a>
## `tb_tbm_session` — TBM 세션

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `SESSION_CD` | varchar(20) | — | PK |  | TBM 세션코드 (PK, 채번 T+YYYYMMDD+SEQ) |
| `CMPNY_CD` | varchar(50) | — | IDX |  | 회사코드 |
| `SITE_CD` | varchar(50) | — |  |  | 사업장코드 |
| `EDU_TYPE_CD` | varchar(20) | — |  | `TBM` | 교육유형[SYS047] TBM:툴박스미팅 (확장용 고정값) |
| `TITLE` | varchar(200) | — |  |  | 세션 제목 |
| `CONTENT_BODY` | mediumtext | Y |  |  | 교육 내용(리치 HTML). 개설 시 필수(서버 검증) |
| `CONTENT_FORMAT_CD` | varchar(20) | — |  | `RICH_HTML` | 교육내용 형식 RICH_HTML:리치텍스트(MVP 고정값) |
| `STATUS_CD` | varchar(20) | — |  | `DRAFT` | 세션상태[SYS046] DRAFT:작성중 OPENED:개설 IN_PROGRESS:진행중 COMPLETED:종료 CANCELLED:취소 |
| `ENTRY_PWD` | varchar(10) | Y |  |  | 입실 비밀번호(랜덤6자리, OPENED부터 생성) |
| `EXIT_PWD` | varchar(10) | Y |  |  | 종료 비밀번호(입실≠종료) |
| `MANAGER_USER_CD` | varchar(20) | — |  |  | 개설자 USER_CD |
| `MANAGER_GPS_LAT` | decimal(10,7) | Y |  |  | 개설 위도(AUTO 모드 시) |
| `MANAGER_GPS_LON` | decimal(10,7) | Y |  |  | 개설 경도(AUTO 모드 시) |
| `GPS_VERIFY_TYPE_CD` | varchar(10) | — |  | `AUTO` | GPS검증유형[SYS048] AUTO:자동 MANUAL:수동확인 DISABLED:비활성 |
| `GPS_VERIFY_RADIUS_M` | int | — |  | `100` | GPS 검증반경(m, 50~1000) |
| `GPS_MANUAL_CONFIRM_YN` | varchar(2) | — |  | `N` | MANUAL 모드 관리자 확인여부 Y:확인 |
| `OPENED_AT` | datetime | Y |  |  | 개설 시각 |
| `STARTED_AT` | datetime | Y |  |  | 교육 시작 시각(IN_PROGRESS 전이) [C단계] |
| `ENDED_AT` | datetime | Y |  |  | 교육 종료 시각 [C단계] |
| `CANCELLED_AT` | datetime | Y |  |  | 취소 시각 |
| `CANCEL_REASON` | varchar(500) | Y |  |  | 취소 사유 |
| `DEL_YN` | varchar(2) | — |  | `N` | 삭제여부 Y/N (DRAFT 물리관리용, OPENED+ 는 STATUS_CD=CANCELLED 사용) |
| `INSERT_NO` | varchar(50) | — |  |  | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | — |  |  | 수정자 |
| `UPDATE_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 수정일시 |

```sql
CREATE TABLE `tb_tbm_session` (
  `SESSION_CD` varchar(20) NOT NULL COMMENT 'TBM 세션코드 (PK, 채번 T+YYYYMMDD+SEQ)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `EDU_TYPE_CD` varchar(20) NOT NULL DEFAULT 'TBM' COMMENT '교육유형[SYS047] TBM:툴박스미팅 (확장용 고정값)',
  `TITLE` varchar(200) NOT NULL COMMENT '세션 제목',
  `CONTENT_BODY` mediumtext COMMENT '교육 내용(리치 HTML). 개설 시 필수(서버 검증)',
  `CONTENT_FORMAT_CD` varchar(20) NOT NULL DEFAULT 'RICH_HTML' COMMENT '교육내용 형식 RICH_HTML:리치텍스트(MVP 고정값)',
  `STATUS_CD` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '세션상태[SYS046] DRAFT:작성중 OPENED:개설 IN_PROGRESS:진행중 COMPLETED:종료 CANCELLED:취소',
  `ENTRY_PWD` varchar(10) COMMENT '입실 비밀번호(랜덤6자리, OPENED부터 생성)',
  `EXIT_PWD` varchar(10) COMMENT '종료 비밀번호(입실≠종료)',
  `MANAGER_USER_CD` varchar(20) NOT NULL COMMENT '개설자 USER_CD',
  `MANAGER_GPS_LAT` decimal(10,7) COMMENT '개설 위도(AUTO 모드 시)',
  `MANAGER_GPS_LON` decimal(10,7) COMMENT '개설 경도(AUTO 모드 시)',
  `GPS_VERIFY_TYPE_CD` varchar(10) NOT NULL DEFAULT 'AUTO' COMMENT 'GPS검증유형[SYS048] AUTO:자동 MANUAL:수동확인 DISABLED:비활성',
  `GPS_VERIFY_RADIUS_M` int NOT NULL DEFAULT 100 COMMENT 'GPS 검증반경(m, 50~1000)',
  `GPS_MANUAL_CONFIRM_YN` varchar(2) NOT NULL DEFAULT 'N' COMMENT 'MANUAL 모드 관리자 확인여부 Y:확인',
  `OPENED_AT` datetime COMMENT '개설 시각',
  `STARTED_AT` datetime COMMENT '교육 시작 시각(IN_PROGRESS 전이) [C단계]',
  `ENDED_AT` datetime COMMENT '교육 종료 시각 [C단계]',
  `CANCELLED_AT` datetime COMMENT '취소 시각',
  `CANCEL_REASON` varchar(500) COMMENT '취소 사유',
  `DEL_YN` varchar(2) NOT NULL DEFAULT 'N' COMMENT '삭제여부 Y/N (DRAFT 물리관리용, OPENED+ 는 STATUS_CD=CANCELLED 사용)',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`SESSION_CD`),
  KEY `IX_TBM_SESSION_01` (`CMPNY_CD`, `SITE_CD`, `STATUS_CD`),
  KEY `IX_TBM_SESSION_02` (`CMPNY_CD`, `MANAGER_USER_CD`),
  KEY `IX_TBM_SESSION_03` (`CMPNY_CD`, `INSERT_DATE`)
) COMMENT='TBM 세션';
```

<a id="tbtbmsessioncontent"></a>
## `tb_tbm_session_content` — TBM 세션-콘텐츠 묶음 매핑

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SESSION_CD` | varchar(20) | — | PK |  | TBM 세션코드 |
| `MTRL_CD` | varchar(20) | — | PK |  | 교육자료 묶음코드 (TB_TBM_EDU_MTRL) |
| `DISPLAY_ORDER` | int | — |  | `0` | 세션 내 표시 순서 |
| `OVERRIDE_DESC` | varchar(500) | Y |  |  | 세션별 설명 override (이 세션에서만 다른 설명) |
| `INSERT_NO` | varchar(50) | — |  |  | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_tbm_session_content` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) NOT NULL COMMENT 'TBM 세션코드',
  `MTRL_CD` varchar(20) NOT NULL COMMENT '교육자료 묶음코드 (TB_TBM_EDU_MTRL)',
  `DISPLAY_ORDER` int NOT NULL DEFAULT 0 COMMENT '세션 내 표시 순서',
  `OVERRIDE_DESC` varchar(500) COMMENT '세션별 설명 override (이 세션에서만 다른 설명)',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`, `SESSION_CD`, `MTRL_CD`),
  KEY `IX_TBM_SESSION_CONTENT_01` (`CMPNY_CD`, `SESSION_CD`, `DISPLAY_ORDER`),
  KEY `IX_TBM_SESSION_CONTENT_02` (`CMPNY_CD`, `MTRL_CD`)
) COMMENT='TBM 세션-콘텐츠 묶음 매핑';
```

<a id="tbtbmsessionrisk"></a>
## `tb_tbm_session_risk` — TBM 세션-위험성평가 매핑

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SESSION_CD` | varchar(20) | — | PK |  | TBM 세션코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 위험성평가 사업장코드 |
| `PROCESS_CD` | varchar(10) | — | PK |  | 위험성평가 공정코드[COM002] |
| `ASSESSMENT_CD` | varchar(10) | — | PK |  | 위험성평가 평가코드 |
| `DISPLAY_ORDER` | int | — |  | `0` | 표시 순서 |
| `INSERT_NO` | varchar(50) | — |  |  | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |

```sql
CREATE TABLE `tb_tbm_session_risk` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) NOT NULL COMMENT 'TBM 세션코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '위험성평가 사업장코드',
  `PROCESS_CD` varchar(10) NOT NULL COMMENT '위험성평가 공정코드[COM002]',
  `ASSESSMENT_CD` varchar(10) NOT NULL COMMENT '위험성평가 평가코드',
  `DISPLAY_ORDER` int NOT NULL DEFAULT 0 COMMENT '표시 순서',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`, `SESSION_CD`, `SITE_CD`, `PROCESS_CD`, `ASSESSMENT_CD`),
  KEY `IX_TBM_SESSION_RISK_01` (`CMPNY_CD`, `SESSION_CD`, `DISPLAY_ORDER`)
) COMMENT='TBM 세션-위험성평가 매핑';
```

<a id="tbtbmsessionstate"></a>
## `tb_tbm_session_state` — TBM 세션 실시간 동기화 상태(UPSERT)

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SESSION_CD` | varchar(20) | — | PK |  | TBM 세션코드 |
| `CURRENT_MTRL_CD` | varchar(20) | Y |  |  | 현재 표시중 콘텐츠 묶음코드 |
| `CURRENT_ITEM_CD` | varchar(20) | Y |  |  | 현재 표시중 세부항목코드 |
| `CURRENT_SLIDE_INDEX` | int | — |  | `0` | 현재 슬라이드 인덱스 |
| `SYNC_STATE_CD` | varchar(20) | — |  | `PAUSED` | 동기화상태[SYS049] PLAYING:재생 PAUSED:정지 |
| `LAST_UPDATED_BY` | varchar(20) | Y |  |  | 마지막 갱신 관리자 |
| `INSERT_NO` | varchar(50) | — |  |  | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_tbm_session_state` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) NOT NULL COMMENT 'TBM 세션코드',
  `CURRENT_MTRL_CD` varchar(20) COMMENT '현재 표시중 콘텐츠 묶음코드',
  `CURRENT_ITEM_CD` varchar(20) COMMENT '현재 표시중 세부항목코드',
  `CURRENT_SLIDE_INDEX` int NOT NULL DEFAULT 0 COMMENT '현재 슬라이드 인덱스',
  `SYNC_STATE_CD` varchar(20) NOT NULL DEFAULT 'PAUSED' COMMENT '동기화상태[SYS049] PLAYING:재생 PAUSED:정지',
  `LAST_UPDATED_BY` varchar(20) COMMENT '마지막 갱신 관리자',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SESSION_CD`)
) COMMENT='TBM 세션 실시간 동기화 상태(UPSERT)';
```


# 9. 산업안전(위험성평가/안전점검/아차사고)

<a id="tbchkptinspectanswer"></a>
## `tb_chkpt_inspect_answer` — 안전점검 점검 답변

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  |  |
| `CHKPT_CD` | varchar(50) | — | PK |  |  |
| `INSPECT_ITEM_CD` | varchar(20) | — | PK |  | 점검항목코드 |
| `WORK_DATE` | varchar(8) | — | PK |  | 점검일자 |
| `INSPECT_ANSWER_TYPE` | varchar(2) | — |  |  | 점검답변타입[SYS009] |
| `ANSWER_DESC` | text | Y |  |  | 점검답변상세 |
| `FILE_MGMT_CD` | varchar(50) | Y |  |  | 첨부사진코드 |
| `INSERT_NO` | varchar(50) | Y |  |  | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_chkpt_inspect_answer` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL,
  `CHKPT_CD` varchar(50) NOT NULL,
  `INSPECT_ITEM_CD` varchar(20) NOT NULL COMMENT '점검항목코드',
  `WORK_DATE` varchar(8) NOT NULL COMMENT '점검일자',
  `INSPECT_ANSWER_TYPE` varchar(2) NOT NULL COMMENT '점검답변타입[SYS009]',
  `ANSWER_DESC` text COMMENT '점검답변상세',
  `FILE_MGMT_CD` varchar(50) COMMENT '첨부사진코드',
  `INSERT_NO` varchar(50) COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `CHKPT_CD`, `INSPECT_ITEM_CD`, `WORK_DATE`)
) COMMENT='안전점검 점검 답변';
```

<a id="tbchkptinspectitem"></a>
## `tb_chkpt_inspect_item` — 안전점검 점검 항목

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `CHKLST_TYPE` | varchar(10) | — | PK |  | 체크리스트 타입 |
| `INSPECT_ITEM_CD` | varchar(20) | — | PK |  | 점검항목코드 |
| `INSPECT_ITEM_SUBJ` | varchar(200) | — |  |  | 점검항목명칭 |
| `SORT_IDX` | int | Y |  |  | 정렬순서 |
| `STR_DATE` | varchar(6) | — |  |  | 시행일자 |
| `USE_YN` | varchar(2) | Y |  | `Y` | 사용유무 |
| `INSERT_NO` | varchar(50) | Y |  |  | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_chkpt_inspect_item` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `CHKLST_TYPE` varchar(10) NOT NULL COMMENT '체크리스트 타입',
  `INSPECT_ITEM_CD` varchar(20) NOT NULL COMMENT '점검항목코드',
  `INSPECT_ITEM_SUBJ` varchar(200) NOT NULL COMMENT '점검항목명칭',
  `SORT_IDX` int COMMENT '정렬순서',
  `STR_DATE` varchar(6) NOT NULL COMMENT '시행일자',
  `USE_YN` varchar(2) DEFAULT 'Y' COMMENT '사용유무',
  `INSERT_NO` varchar(50) COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `CHKLST_TYPE`, `INSPECT_ITEM_CD`)
) COMMENT='안전점검 점검 항목';
```

<a id="tbchkpttypemgmt"></a>
## `tb_chkpt_type_mgmt` — 안전점검 체크포인트 유형 관리

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `CHKLST_TYPE` | varchar(10) | — | PK |  | 체크리스트 타입 |
| `CHKPT_CD` | varchar(50) | — | PK |  | 체크포인트 코드 |
| `CHKPT_NM` | varchar(100) | Y |  |  | 체크포인트명 |
| `CHKPT_DESC` | varchar(500) | Y |  |  | 비고 |
| `MGMT_USER_CD` | varchar(20) | Y |  |  | 체크포인트 관리자ID |
| `USE_YN` | varchar(2) | Y |  | `Y` | 사용여부 |
| `INSERT_NO` | varchar(50) | Y |  |  | 입력자 |
| `INSERT_DATE` | datetime | Y |  |  | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_chkpt_type_mgmt` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `CHKLST_TYPE` varchar(10) NOT NULL COMMENT '체크리스트 타입',
  `CHKPT_CD` varchar(50) NOT NULL COMMENT '체크포인트 코드',
  `CHKPT_NM` varchar(100) COMMENT '체크포인트명',
  `CHKPT_DESC` varchar(500) COMMENT '비고',
  `MGMT_USER_CD` varchar(20) COMMENT '체크포인트 관리자ID',
  `USE_YN` varchar(2) DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COMMENT '입력자',
  `INSERT_DATE` datetime COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `CHKLST_TYPE`, `CHKPT_CD`)
) COMMENT='안전점검 체크포인트 유형 관리';
```

<a id="tbnearmiss"></a>
## `tb_near_miss` — 아차사고/사건 보고

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `NEAR_MISS_ID` | varchar(20) | — | PK |  | 사건 ID (사업장별 채번: NM + YYYYMMDD + SEQ) |
| `INCIDENT_TYPE_CD` | varchar(10) | — |  |  | 사건유형[SYS061] 100:아차사고 200:경미사고 300:유해·위험요인발견 |
| `PROCESS_CD` | varchar(10) | Y |  |  | 공정코드[COM002] |
| `OCCUR_DTIME` | datetime | — |  |  | 발생일시 |
| `LOCATION_DESC` | varchar(200) | Y |  |  | 발생장소(직접입력) |
| `DESCRIPTION` | varchar(500) | — |  |  | 사건 경위(무슨 일이 있었나) |
| `POTENTIAL_SEVERITY_CD` | varchar(10) | Y |  |  | 잠재적 중대성[SYS062] 100:경미 200:중대 300:치명(실제 사고였다면) |
| `IMMEDIATE_ACTION_DESC` | varchar(500) | Y |  |  | 보고자 즉시 조치사항 |
| `ADMIN_TEMP_ACTION_DESC` | varchar(500) | Y |  |  | 관리자 임시조치 메모(앱 1차확인 시 입력, 보고자 IMMEDIATE_ACTION_DESC 와 분리) |
| `CAUSE_DESC` | varchar(500) | Y |  |  | 추정 원인(웹 정밀조사) |
| `PREVENTION_DESC` | varchar(500) | Y |  |  | 재발방지 대책(웹 정밀조사) |
| `FILE_MGMT_CD` | varchar(50) | Y |  |  | 현장 사진(tb_file_info 관리코드) |
| `REPORT_STATUS_CD` | varchar(10) | — |  | `100` | 처리상태[SYS063] 100:접수 200:검토중 300:조치중 400:완료 900:반려 |
| `REPORTER_ID` | varchar(50) | — |  |  | 보고자(tb_user.USER_CD) |
| `REPORT_DTIME` | datetime | — |  | `CURRENT_TIMESTAMP` | 보고일시 |
| `REVIEWER_ID` | varchar(50) | Y |  |  | 검토 관리자(tb_user.USER_CD) |
| `REVIEW_DTIME` | datetime | Y |  |  | 검토(분류)일시 |
| `SRC_PROCESS_CD` | varchar(10) | Y |  |  | 원 위험성평가요청 공정코드(재분류 출처) |
| `SRC_ASSESSMENT_CD` | varchar(10) | Y |  |  | 원 위험성평가요청 ID(tb_risk_assessment.ASSESSMENT_CD, 재분류 출처) |
| `REJECT_REASON` | varchar(500) | Y |  |  | 반려 사유(처리상태 900 반려 시 기록, 추정원인 CAUSE_DESC 와 분리) |
| `USE_YN` | varchar(2) | — |  | `Y` | 사용여부 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | — |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_near_miss` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `NEAR_MISS_ID` varchar(20) NOT NULL COMMENT '사건 ID (사업장별 채번: NM + YYYYMMDD + SEQ)',
  `INCIDENT_TYPE_CD` varchar(10) NOT NULL COMMENT '사건유형[SYS061] 100:아차사고 200:경미사고 300:유해·위험요인발견',
  `PROCESS_CD` varchar(10) COMMENT '공정코드[COM002]',
  `OCCUR_DTIME` datetime NOT NULL COMMENT '발생일시',
  `LOCATION_DESC` varchar(200) COMMENT '발생장소(직접입력)',
  `DESCRIPTION` varchar(500) NOT NULL COMMENT '사건 경위(무슨 일이 있었나)',
  `POTENTIAL_SEVERITY_CD` varchar(10) COMMENT '잠재적 중대성[SYS062] 100:경미 200:중대 300:치명(실제 사고였다면)',
  `IMMEDIATE_ACTION_DESC` varchar(500) COMMENT '보고자 즉시 조치사항',
  `ADMIN_TEMP_ACTION_DESC` varchar(500) COMMENT '관리자 임시조치 메모(앱 1차확인 시 입력, 보고자 IMMEDIATE_ACTION_DESC 와 분리)',
  `CAUSE_DESC` varchar(500) COMMENT '추정 원인(웹 정밀조사)',
  `PREVENTION_DESC` varchar(500) COMMENT '재발방지 대책(웹 정밀조사)',
  `FILE_MGMT_CD` varchar(50) COMMENT '현장 사진(tb_file_info 관리코드)',
  `REPORT_STATUS_CD` varchar(10) NOT NULL DEFAULT '100' COMMENT '처리상태[SYS063] 100:접수 200:검토중 300:조치중 400:완료 900:반려',
  `REPORTER_ID` varchar(50) NOT NULL COMMENT '보고자(tb_user.USER_CD)',
  `REPORT_DTIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '보고일시',
  `REVIEWER_ID` varchar(50) COMMENT '검토 관리자(tb_user.USER_CD)',
  `REVIEW_DTIME` datetime COMMENT '검토(분류)일시',
  `SRC_PROCESS_CD` varchar(10) COMMENT '원 위험성평가요청 공정코드(재분류 출처)',
  `SRC_ASSESSMENT_CD` varchar(10) COMMENT '원 위험성평가요청 ID(tb_risk_assessment.ASSESSMENT_CD, 재분류 출처)',
  `REJECT_REASON` varchar(500) COMMENT '반려 사유(처리상태 900 반려 시 기록, 추정원인 CAUSE_DESC 와 분리)',
  `USE_YN` varchar(2) NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `NEAR_MISS_ID`),
  KEY `IX_TB_NEAR_MISS_REPORTER` (`CMPNY_CD`, `REPORTER_ID`),
  KEY `IX_TB_NEAR_MISS_SRC` (`CMPNY_CD`, `SITE_CD`, `SRC_PROCESS_CD`, `SRC_ASSESSMENT_CD`),
  KEY `IX_TB_NEAR_MISS_STATUS` (`CMPNY_CD`, `SITE_CD`, `REPORT_STATUS_CD`)
) COMMENT='아차사고/사건 보고';
```

<a id="tbriskassessment"></a>
## `tb_risk_assessment` — 위험성평가

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `SITE_CD` | varchar(50) | — | PK |  | 사업장코드 |
| `PROCESS_CD` | varchar(10) | — | PK |  | 공정코드[COM002] |
| `RISK_TYPE_CD` | varchar(10) | — |  |  | 위험요인구분코드 |
| `HAZARD_CD` | varchar(10) | Y |  |  | 유해요인코드 |
| `ASSESSMENT_CD` | varchar(10) | — | PK |  | 평가코드 |
| `ASSESSMENT_DESC` | varchar(500) | Y |  |  | 유해요인 직접입력 |
| `ASSESSMENT_STATUS` | varchar(3) | — |  |  | 진행상태[SYS011] |
| `INIT_LIKELIHOOD_SCORE` | int | Y |  |  | 초기평가 발생빈도 |
| `INIT_SEVERITY_SCORE` | int | Y |  |  | 초기평가 중대성 |
| `INIT_RISK_LV` | varchar(10) | Y |  |  | 초기평가 위험성LEVEL |
| `INIT_DESC` | varchar(500) | Y |  |  | 유해요인설명 |
| `INIT_FILE_MGMT_CD` | varchar(50) | Y |  |  | 유해요인사진 |
| `INIT_ASSESSOR_ID` | varchar(50) | Y |  |  | 초기평가자 |
| `INIT_ASSESS_DATE` | datetime | Y |  |  | 초기평가일시 |
| `REVAL_DATE` | varchar(8) | Y |  |  | 개선예정일 |
| `REVAL_BEFORE_DESC` | varchar(500) | Y |  |  | 개선전 임시조치 사항 |
| `REVAL_LIKELIHOOD_SCORE` | int | Y |  |  | 개선 후 발생빈도 |
| `REVAL_SEVERITY_SCORE` | int | Y |  |  | 개선 후 중대성 |
| `REVAL_RISK_LV` | varchar(10) | Y |  |  | 개선 후 평가 위험성LEVEL |
| `REVAL_DESC` | varchar(500) | Y |  |  | 개선내용 |
| `REVAL_FILE_MGMT_CD` | varchar(50) | Y |  |  | 개선사진 |
| `REVAL_ASSESSOR_ID` | varchar(50) | Y |  |  | 개선후평가자 |
| `REVAL_ASSESS_DATE` | datetime | Y |  |  | 개선완료일 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_risk_assessment` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) NOT NULL COMMENT '사업장코드',
  `PROCESS_CD` varchar(10) NOT NULL COMMENT '공정코드[COM002]',
  `RISK_TYPE_CD` varchar(10) NOT NULL COMMENT '위험요인구분코드',
  `HAZARD_CD` varchar(10) COMMENT '유해요인코드',
  `ASSESSMENT_CD` varchar(10) NOT NULL COMMENT '평가코드',
  `ASSESSMENT_DESC` varchar(500) COMMENT '유해요인 직접입력',
  `ASSESSMENT_STATUS` varchar(3) NOT NULL COMMENT '진행상태[SYS011]',
  `INIT_LIKELIHOOD_SCORE` int COMMENT '초기평가 발생빈도',
  `INIT_SEVERITY_SCORE` int COMMENT '초기평가 중대성',
  `INIT_RISK_LV` varchar(10) COMMENT '초기평가 위험성LEVEL',
  `INIT_DESC` varchar(500) COMMENT '유해요인설명',
  `INIT_FILE_MGMT_CD` varchar(50) COMMENT '유해요인사진',
  `INIT_ASSESSOR_ID` varchar(50) COMMENT '초기평가자',
  `INIT_ASSESS_DATE` datetime COMMENT '초기평가일시',
  `REVAL_DATE` varchar(8) COMMENT '개선예정일',
  `REVAL_BEFORE_DESC` varchar(500) COMMENT '개선전 임시조치 사항',
  `REVAL_LIKELIHOOD_SCORE` int COMMENT '개선 후 발생빈도',
  `REVAL_SEVERITY_SCORE` int COMMENT '개선 후 중대성',
  `REVAL_RISK_LV` varchar(10) COMMENT '개선 후 평가 위험성LEVEL',
  `REVAL_DESC` varchar(500) COMMENT '개선내용',
  `REVAL_FILE_MGMT_CD` varchar(50) COMMENT '개선사진',
  `REVAL_ASSESSOR_ID` varchar(50) COMMENT '개선후평가자',
  `REVAL_ASSESS_DATE` datetime COMMENT '개선완료일',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `PROCESS_CD`, `ASSESSMENT_CD`)
) COMMENT='위험성평가';
```

<a id="tbrisksitehazard"></a>
## `tb_risk_site_hazard` — 사업장 유해위험요인

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `RISK_TYPE_CD` | varchar(10) | — | PK |  | 위험요인구분코드 |
| `HAZARD_CD` | varchar(10) | — | PK |  | 유해요인코드 |
| `HAZARD_NM` | varchar(100) | — |  |  | 유해요인명 |
| `SITE_CD` | varchar(50) | Y |  |  | 사업장코드 |
| `HAZARD_DESC` | varchar(500) | Y |  |  | 유해요인설명 |
| `USE_YN` | varchar(2) | Y |  | `Y` |  |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_risk_site_hazard` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `RISK_TYPE_CD` varchar(10) NOT NULL COMMENT '위험요인구분코드',
  `HAZARD_CD` varchar(10) NOT NULL COMMENT '유해요인코드',
  `HAZARD_NM` varchar(100) NOT NULL COMMENT '유해요인명',
  `SITE_CD` varchar(50) COMMENT '사업장코드',
  `HAZARD_DESC` varchar(500) COMMENT '유해요인설명',
  `USE_YN` varchar(2) DEFAULT 'Y',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `RISK_TYPE_CD`, `HAZARD_CD`)
) COMMENT='사업장 유해위험요인';
```

<a id="tbrisktype"></a>
## `tb_risk_type` — 위험성평가 유형

| 컬럼 | 타입 | NULL | KEY | 기본값 | 설명 |
| --- | --- | :---: | :---: | --- | --- |
| `CMPNY_CD` | varchar(50) | — | PK |  | 회사코드 |
| `RISK_TYPE_CD` | varchar(10) | — | PK |  | 위험요인구분코드 |
| `RISK_TYPE_NM` | varchar(100) | — |  |  | 위험요인구분명 |
| `SITE_CD` | varchar(50) | Y |  |  | 사업장코드 |
| `PROCESS_CD` | varchar(10) | — |  |  | 공정코드[COM002] |
| `USE_YN` | varchar(2) | — |  |  | 사용여부 |
| `RISK_TYPE_DESC` | varchar(500) | Y |  |  | 위험요인비고 |
| `INSERT_NO` | varchar(50) | Y |  | `SYSTEM` | 입력자 |
| `INSERT_DATE` | datetime | Y |  | `CURRENT_TIMESTAMP` | 입력일시 |
| `UPDATE_NO` | varchar(50) | Y |  |  | 수정자 |
| `UPDATE_DATE` | datetime | Y |  |  | 수정일시 |

```sql
CREATE TABLE `tb_risk_type` (
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사코드',
  `RISK_TYPE_CD` varchar(10) NOT NULL COMMENT '위험요인구분코드',
  `RISK_TYPE_NM` varchar(100) NOT NULL COMMENT '위험요인구분명',
  `SITE_CD` varchar(50) COMMENT '사업장코드',
  `PROCESS_CD` varchar(10) NOT NULL COMMENT '공정코드[COM002]',
  `USE_YN` varchar(2) NOT NULL COMMENT '사용여부',
  `RISK_TYPE_DESC` varchar(500) COMMENT '위험요인비고',
  `INSERT_NO` varchar(50) DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COMMENT '수정자',
  `UPDATE_DATE` datetime COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `RISK_TYPE_CD`)
) COMMENT='위험성평가 유형';
```
