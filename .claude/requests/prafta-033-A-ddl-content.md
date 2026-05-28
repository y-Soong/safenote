# prafta-033-A — DDL + 콘텐츠 라이브러리 보강 (W-01~03)

> 마스터 플랜: `prafta-033-plan.md` (결정사항 D1~D4, 매핑표 §3 우선 숙지)
> To-Be 사양: `ref/prafta-033/05_01_CONTENT_LIBRARY.md`, `03_BACKEND_SPEC_WEB.md §4`
> 본 단계 = **전체 신규 스키마 생성 + 기존 콘텐츠 화면(tbm01) 보강**.

---

## 0. 범위

| 포함 | 제외 |
|---|---|
| ① 기존 콘텐츠 테이블 ALTER (`TB_TBM_EDU_MTRL`, `_ITEM`) | 세션 개설/관리 로직 (→ B) |
| ② **신규 테이블 전체 DDL** (세션/매핑/출결/이벤트/상태/비번실패) — *생성만* | 출결/동기화 **쓰기** 경로 (→ C, 앱) |
| ③ 공통코드 시드 (SYS018 PDF 추가 + 신규 코드그룹) | 이력 화면 (→ D) |
| ④ 콘텐츠 라이브러리 화면(W-01~03) 보강 (백엔드 tbm01 + 프론트) | |

> ⚠️ A에서 **모든 테이블을 생성**하는 이유: D(이력)가 출결 테이블을 읽어야 하고, 스키마를 한 번에 일관 설계해야 하므로. 단 출결/이벤트/상태 테이블의 **INSERT/UPDATE는 C·앱 단계**에서 구현한다. A는 DDL과 (D가 쓸) 조회 매퍼만.

---

## 0.5. ★ DDL 검토 확정 (2026-05-27, MCP 재검증 결과)

실제 스키마 재조회로 아래를 확정했다. 본 문서 DDL은 이 결과를 **이미 반영**했다.

| 검증 항목 | 결과 → 반영 |
|---|---|
| 파일코드 폭 | `TB_FILE_INFO.FILE_MGMT_CD = varchar(50)` → 신규 파일참조 컬럼 **varchar(50)** (기존 tbm_item의 varchar(40)은 레거시, 유지) |
| GPS 컬럼 규약 | 기존 `tb_user_attd_gps` = `LAT`/`LON` `decimal(10,7)` → 신규도 **`*_GPS_LAT`/`*_GPS_LON` `decimal(10,7)`** (LNG 아님, **LON** 통일) |
| SYS 코드 최대번호 | 현재 최대 `SYS045` → 신규 그룹 **SYS046~SYS055** 배정 (§4.2 확정) |
| 카테고리 코드원 | `COM003 = "TBM교육타입"`(회사별 3건) 존재 → 카테고리는 **기존 COM003 재사용**(신규 그룹 미생성) |
| CMPNY_CD 폭 | 표준 varchar(50). 기존 tbm 테이블만 varchar(10) → 신규는 **varchar(50)** |

**신규 SYS 코드 그룹 배정 (확정):**

| SYS | 그룹명 | 적용 컬럼 |
|---|---|---|
| SYS046 | TBM 세션 상태 | `TB_TBM_SESSION.STATUS_CD` |
| SYS047 | TBM 교육 유형 | `TB_TBM_SESSION.EDU_TYPE_CD` |
| SYS048 | TBM GPS 검증유형 | `TB_TBM_SESSION.GPS_VERIFY_TYPE_CD` |
| SYS049 | TBM 동기화 상태 | `TB_TBM_SESSION_STATE.SYNC_STATE_CD` |
| SYS050 | TBM 출결 대상유형 | `TB_TBM_ATTENDANCE.USER_TYPE_CD` |
| SYS051 | TBM 입실 경로 | `TB_TBM_ATTENDANCE.ENTRY_TYPE_CD` |
| SYS052 | TBM 종료 경로 | `TB_TBM_ATTENDANCE.EXIT_TYPE_CD` |
| SYS053 | TBM 이수 상태 | `TB_TBM_ATTENDANCE.COMPLETION_STATUS_CD` |
| SYS054 | TBM 출결 이벤트 유형 | `TB_TBM_ATTENDANCE_EVENT.EVENT_TYPE_CD` |
| SYS055 | TBM 비번 유형 | `TB_TBM_PWD_FAIL.PWD_TYPE_CD` |

> `CONTENT_FORMAT_CD`는 MVP 단일 고정값(`RICH_HTML`)이라 SYS 그룹 미부여(컬럼 COMMENT로만 명시). 확장 시 그룹 부여.

---

## 1. DDL — 기존 콘텐츠 테이블 ALTER (방향 A 확장)

### 1.1 `TB_TBM_EDU_MTRL` (교육자료 묶음 마스터) — 컬럼 추가

현재: PK `MTRL_CD`(20), `CMPNY_CD`(10), `TITLE`(200), `CONTENTS`(500), `MTRL_TYPE`(8, =카테고리/COM003), `USE_YN`, audit.

```sql
-- prafta-033-A: 콘텐츠 스코프(회사공통/사업장) 추가
ALTER TABLE `tb_tbm_edu_mtrl`
  ADD COLUMN `SITE_CD` varchar(50) NULL COMMENT '사업장코드 (NULL=회사공통, 값=해당 사업장 전용)' AFTER `CMPNY_CD`;

-- 조회 인덱스 보강 (스코프 필터: 회사공통 OR 자기사업장)
ALTER TABLE `tb_tbm_edu_mtrl`
  ADD INDEX `IX_TBM_EDU_MTRL_02` (`CMPNY_CD`, `SITE_CD`, `USE_YN`);
```

- **카테고리**는 신규 코드그룹 만들지 않고 **기존 `MTRL_TYPE`(COM003 "TBM교육타입")** 을 그대로 "카테고리"로 사용 (사양서의 `SYS_TBM_CONTENT_CAT` 신설 제안 **기각**, MCP로 COM003 존재 확인).
- `CMPNY_CD`는 기존 varchar(10) **그대로 둔다**(ALTER 시 데이터/제약 리스크). 신규 테이블만 varchar(50) 사용 — 조인은 콜레이션 동일하여 문제 없음.

### 1.2 `TB_TBM_EDU_MTRL_ITEM` (세부항목 = 미디어 1건) — 컬럼 추가

현재: PK `MTRL_ITEM_CD`(20), `MTRL_CD`(20, FK), `SORT_IDX`, `MTRL_ITEM_TYPE`(2, SYS018), `MTRL_DESC`(500), `FILE_MGMT_CD`(40), `URL`(1000), `USE_YN`, audit.

```sql
-- prafta-033-A: 썸네일/영상길이 추가 (W-02 미리보기, W-03 상세)
ALTER TABLE `tb_tbm_edu_mtrl_item`
  ADD COLUMN `THUMB_FILE_MGMT_CD` varchar(50) NULL COMMENT '썸네일 파일코드 (동영상 첫프레임/PDF 첫페이지/이미지 리사이즈 자동생성)' AFTER `FILE_MGMT_CD`,
  ADD COLUMN `DURATION_SEC` int NULL COMMENT '미디어 길이(초) - 동영상만' AFTER `THUMB_FILE_MGMT_CD`;
```

- `MTRL_ITEM_TYPE`(SYS018): 현재 `01`이미지/`02`동영상/`03`유튜브URL → **`04` PDF 추가**(§4.1). YOUTUBE는 이미 `03`으로 존재(사양의 YOUTUBE = 기존 03).
- `FILE_MGMT_CD`(varchar40)는 그대로. 신규 파일참조 컬럼은 `TB_FILE_INFO.FILE_MGMT_CD`(varchar50)에 맞춰 varchar(50).

---

## 2. DDL — 신규 테이블 (세션·매핑) [B 단계가 사용]

> 명명: `TB_TBM_SESSION` 계열. PK는 **varchar 코드 채번**(`FNC_CMM_SEQ_NEXTVAL`). `CMPNY_CD` = varchar(50).
> GPS: `decimal(10,7)`, 컬럼명 `*_GPS_LAT`/`*_GPS_LON` (기존 `tb_user_attd_gps` 규약).

### 2.1 `TB_TBM_SESSION` (TBM 세션)

```sql
CREATE TABLE `tb_tbm_session` (
  `SESSION_CD`            varchar(20)  NOT NULL COMMENT 'TBM 세션코드 (PK, 채번 T+YYYYMMDD+SEQ)',
  `CMPNY_CD`              varchar(50)  NOT NULL COMMENT '회사코드',
  `SITE_CD`               varchar(50)  NOT NULL COMMENT '사업장코드',
  `EDU_TYPE_CD`           varchar(20)  NOT NULL DEFAULT 'TBM' COMMENT '교육유형[SYS047] TBM:툴박스미팅 (확장용 고정값)',
  `TITLE`                 varchar(200) NOT NULL COMMENT '세션 제목',
  `CONTENT_BODY`          mediumtext   NULL     COMMENT '교육 내용(리치 HTML). 개설 시 필수(서버 검증)',
  `CONTENT_FORMAT_CD`     varchar(20)  NOT NULL DEFAULT 'RICH_HTML' COMMENT '교육내용 형식 RICH_HTML:리치텍스트(MVP 고정값)',
  `STATUS_CD`             varchar(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '세션상태[SYS046] DRAFT:작성중 OPENED:개설 IN_PROGRESS:진행중 COMPLETED:종료 CANCELLED:취소',
  `ENTRY_PWD`             varchar(10)  NULL     COMMENT '입실 비밀번호(랜덤6자리, OPENED부터 생성)',
  `EXIT_PWD`              varchar(10)  NULL     COMMENT '종료 비밀번호(입실≠종료)',
  `MANAGER_USER_CD`       varchar(20)  NOT NULL COMMENT '개설자 USER_CD',
  `MANAGER_GPS_LAT`       decimal(10,7) NULL    COMMENT '개설 위도(AUTO 모드 시)',
  `MANAGER_GPS_LON`       decimal(10,7) NULL    COMMENT '개설 경도(AUTO 모드 시)',
  `GPS_VERIFY_TYPE_CD`    varchar(10)  NOT NULL DEFAULT 'AUTO' COMMENT 'GPS검증유형[SYS048] AUTO:자동 MANUAL:수동확인 DISABLED:비활성',
  `GPS_VERIFY_RADIUS_M`   int          NOT NULL DEFAULT 100 COMMENT 'GPS 검증반경(m, 50~1000)',
  `GPS_MANUAL_CONFIRM_YN` varchar(2)   NOT NULL DEFAULT 'N' COMMENT 'MANUAL 모드 관리자 확인여부 Y:확인',
  `OPENED_AT`             datetime     NULL     COMMENT '개설 시각',
  `STARTED_AT`            datetime     NULL     COMMENT '교육 시작 시각(IN_PROGRESS 전이) [C단계]',
  `ENDED_AT`              datetime     NULL     COMMENT '교육 종료 시각 [C단계]',
  `CANCELLED_AT`          datetime     NULL     COMMENT '취소 시각',
  `CANCEL_REASON`         varchar(500) NULL     COMMENT '취소 사유',
  `DEL_YN`                varchar(2)   NOT NULL DEFAULT 'N' COMMENT '삭제여부 Y/N (DRAFT 물리관리용, OPENED+ 는 STATUS_CD=CANCELLED 사용)',
  `INSERT_NO`             varchar(50)  NOT NULL COMMENT '입력자',
  `INSERT_DATE`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO`             varchar(50)  NOT NULL COMMENT '수정자',
  `UPDATE_DATE`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`SESSION_CD`),
  KEY `IX_TBM_SESSION_01` (`CMPNY_CD`, `SITE_CD`, `STATUS_CD`),
  KEY `IX_TBM_SESSION_02` (`CMPNY_CD`, `MANAGER_USER_CD`),
  KEY `IX_TBM_SESSION_03` (`CMPNY_CD`, `INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션';
```

### 2.2 `TB_TBM_SESSION_CONTENT` (세션 ↔ 콘텐츠 묶음 매핑, M:N)

```sql
CREATE TABLE `tb_tbm_session_content` (
  `CMPNY_CD`      varchar(50)  NOT NULL COMMENT '회사코드',
  `SESSION_CD`    varchar(20)  NOT NULL COMMENT 'TBM 세션코드',
  `MTRL_CD`       varchar(20)  NOT NULL COMMENT '교육자료 묶음코드 (TB_TBM_EDU_MTRL)',
  `DISPLAY_ORDER` int          NOT NULL DEFAULT 0 COMMENT '세션 내 표시 순서',
  `OVERRIDE_DESC` varchar(500) NULL     COMMENT '세션별 설명 override (이 세션에서만 다른 설명)',
  `INSERT_NO`     varchar(50)  NOT NULL COMMENT '입력자',
  `INSERT_DATE`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`, `SESSION_CD`, `MTRL_CD`),
  KEY `IX_TBM_SESSION_CONTENT_01` (`CMPNY_CD`, `SESSION_CD`, `DISPLAY_ORDER`),
  KEY `IX_TBM_SESSION_CONTENT_02` (`CMPNY_CD`, `MTRL_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션-콘텐츠 묶음 매핑';
```

- 방향 A: **묶음(MTRL_CD) 단위로 첨부**. 세부항목(슬라이드)은 묶음 안에서 자동 포함.
- `MTRL_CD`는 `TB_TBM_EDU_MTRL` 참조하나 **FK 제약은 걸지 않음**(콘텐츠 소프트삭제·이력 보존 위해; 무결성은 서비스에서). 사양 §"콘텐츠 사용한 TBM 이력은 유지" 준수.

### 2.3 `TB_TBM_SESSION_RISK` (세션 ↔ 위험성평가 매핑, M:N, 옵션)

```sql
CREATE TABLE `tb_tbm_session_risk` (
  `CMPNY_CD`      varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD`    varchar(20) NOT NULL COMMENT 'TBM 세션코드',
  `SITE_CD`       varchar(50) NOT NULL COMMENT '위험성평가 사업장코드',
  `PROCESS_CD`    varchar(10) NOT NULL COMMENT '위험성평가 공정코드[COM002]',
  `ASSESSMENT_CD` varchar(10) NOT NULL COMMENT '위험성평가 평가코드',
  `DISPLAY_ORDER` int         NOT NULL DEFAULT 0 COMMENT '표시 순서',
  `INSERT_NO`     varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`, `SESSION_CD`, `SITE_CD`, `PROCESS_CD`, `ASSESSMENT_CD`),
  KEY `IX_TBM_SESSION_RISK_01` (`CMPNY_CD`, `SESSION_CD`, `DISPLAY_ORDER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션-위험성평가 매핑';
```

- `TB_RISK_ASSESSMENT` PK가 (CMPNY_CD, SITE_CD, PROCESS_CD, ASSESSMENT_CD) 복합이라 매핑도 동일 키를 보유.
- ⚠️ **후속확인(plan §8-1)**: 위험성평가에 TITLE 컬럼 없음 → 화면 표시명 구성 방식은 B 단계에서 위험성평가 모듈 담당과 확정.

### 2.4 `TB_TBM_SESSION_STATE` (실시간 동기화 상태, 세션 1:1) [C 단계 쓰기]

```sql
CREATE TABLE `tb_tbm_session_state` (
  `CMPNY_CD`            varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD`          varchar(20) NOT NULL COMMENT 'TBM 세션코드',
  `CURRENT_MTRL_CD`     varchar(20) NULL     COMMENT '현재 표시중 콘텐츠 묶음코드',
  `CURRENT_ITEM_CD`     varchar(20) NULL     COMMENT '현재 표시중 세부항목코드',
  `CURRENT_SLIDE_INDEX` int         NOT NULL DEFAULT 0 COMMENT '현재 슬라이드 인덱스',
  `SYNC_STATE_CD`       varchar(20) NOT NULL DEFAULT 'PAUSED' COMMENT '동기화상태[SYS049] PLAYING:재생 PAUSED:정지',
  `LAST_UPDATED_BY`     varchar(20) NULL     COMMENT '마지막 갱신 관리자',
  `INSERT_NO`           varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO`           varchar(50) NULL     COMMENT '수정자',
  `UPDATE_DATE`         datetime    NULL     COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`, `SESSION_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션 실시간 동기화 상태(UPSERT)';
```

---

## 3. DDL — 신규 테이블 (출결·이벤트·비번실패) [C/앱 쓰기, D 읽기]

### 3.1 `TB_TBM_ATTENDANCE` (출결 통합 — 정규직/일용직)

```sql
CREATE TABLE `tb_tbm_attendance` (
  `ATTENDANCE_CD`            varchar(20)  NOT NULL COMMENT '출결코드 (PK, 채번 A+YYYYMMDD+SEQ)',
  `CMPNY_CD`                 varchar(50)  NOT NULL COMMENT '회사코드',
  `SESSION_CD`               varchar(20)  NOT NULL COMMENT 'TBM 세션코드',
  `USER_TYPE_CD`             varchar(20)  NOT NULL COMMENT '대상유형[SYS050] REGULAR:정규직(TB_USER) DAILY:일용직(TB_DAILY_USER)',
  `USER_CD`                  varchar(20)  NOT NULL COMMENT '대상 USER_CD (유형에 따라 TB_USER 또는 TB_DAILY_USER)',
  `ENTRY_TYPE_CD`            varchar(20)  NULL     COMMENT '입실경로[SYS051] SELF_DEVICE:본인디바이스 MANAGER_QR_SCAN:관리자QR스캔',
  `ENTRY_BY_MANAGER_USER_CD` varchar(20)  NULL     COMMENT 'QR 입실 처리 관리자 USER_CD',
  `ENTRY_AT`                 datetime     NULL     COMMENT '입실 시각',
  `ENTRY_GPS_LAT`            decimal(10,7) NULL    COMMENT '입실 위도',
  `ENTRY_GPS_LON`            decimal(10,7) NULL    COMMENT '입실 경도',
  `ENTRY_DISTANCE_M`         int          NULL     COMMENT '입실 시 개설지점과의 거리(m)',
  `ENTRY_SIGN_FILE_MGMT_CD`  varchar(50)  NULL     COMMENT '입실 서명 파일코드',
  `EXIT_TYPE_CD`             varchar(20)  NULL     COMMENT '종료경로[SYS052] SELF:본인 MANAGER_QR_SCAN:관리자QR MANAGER_FORCED:관리자강제',
  `EXIT_BY_MANAGER_USER_CD`  varchar(20)  NULL     COMMENT '종료 처리 관리자 USER_CD',
  `EXIT_AT`                  datetime     NULL     COMMENT '종료 시각(NULL=미종료)',
  `EXIT_SIGN_FILE_MGMT_CD`   varchar(50)  NULL     COMMENT '종료 서명 파일코드',
  `EXIT_FORCED_REASON`       varchar(500) NULL     COMMENT '강제종료 사유(관리자 책임 기록)',
  `COMPLETION_STATUS_CD`     varchar(20)  NULL     COMMENT '이수상태[SYS053] COMPLETED:이수 NOT_COMPLETED:미이수',
  `NOT_COMPLETED_REASON`     varchar(500) NULL     COMMENT '미이수 사유',
  `STATUS_UPDATED_BY`        varchar(20)  NULL     COMMENT '이수상태 마지막 변경자',
  `STATUS_UPDATED_AT`        datetime     NULL     COMMENT '이수상태 마지막 변경 시각',
  `DEL_YN`                   varchar(2)   NOT NULL DEFAULT 'N' COMMENT '삭제여부 Y/N',
  `INSERT_NO`                varchar(50)  NOT NULL COMMENT '입력자',
  `INSERT_DATE`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO`                varchar(50)  NOT NULL COMMENT '수정자',
  `UPDATE_DATE`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`ATTENDANCE_CD`),
  UNIQUE KEY `UK_TBM_ATTENDANCE_01` (`CMPNY_CD`, `SESSION_CD`, `USER_TYPE_CD`, `USER_CD`),
  KEY `IX_TBM_ATTENDANCE_01` (`CMPNY_CD`, `SESSION_CD`),
  KEY `IX_TBM_ATTENDANCE_02` (`CMPNY_CD`, `USER_TYPE_CD`, `USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 출결(정규직/일용직 통합)';
```

- **정규직/일용직 통합 한 테이블**: `USER_TYPE_CD`로 구분(둘 다 (CMPNY_CD,USER_CD) 식별이지만 테이블이 달라 유형 분리 필수). UK에 `USER_TYPE_CD` 포함 → 중복출결 방지 + 멱등.
- `ATTENDANCE_CD` surrogate PK → 이벤트 로그가 단일 컬럼으로 참조 가능.

### 3.2 `TB_TBM_ATTENDANCE_EVENT` (이벤트 로그)

```sql
CREATE TABLE `tb_tbm_attendance_event` (
  `EVENT_NO`           bigint       NOT NULL AUTO_INCREMENT COMMENT '이벤트 일련번호 (PK)',
  `CMPNY_CD`           varchar(50)  NOT NULL COMMENT '회사코드',
  `SESSION_CD`         varchar(20)  NOT NULL COMMENT 'TBM 세션코드(비정규화, 조회용)',
  `ATTENDANCE_CD`      varchar(20)  NOT NULL COMMENT '출결코드',
  `EVENT_TYPE_CD`      varchar(30)  NOT NULL COMMENT '이벤트유형[SYS054] ENTER/START/SLIDE_CHANGED/GPS_UPDATED/BACKGROUND_IN/BACKGROUND_OUT/NETWORK_LOST/SIGNATURE_STARTED/END/FORCED_END',
  `EVENT_TIME`         datetime(3)  NOT NULL COMMENT '이벤트 발생시각(클라이언트 보고, ms)',
  `SERVER_RECEIVED_AT` datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '서버 수신시각(ms, 위조불가 기준)',
  `EVENT_DATA`         json         NULL     COMMENT '이벤트 부가데이터(JSON)',
  `INSERT_NO`          varchar(50)  NOT NULL COMMENT '입력자',
  `INSERT_DATE`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`EVENT_NO`),
  KEY `IX_TBM_ATT_EVENT_01` (`CMPNY_CD`, `ATTENDANCE_CD`, `EVENT_TIME`),
  KEY `IX_TBM_ATT_EVENT_02` (`CMPNY_CD`, `SESSION_CD`, `EVENT_TYPE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 출결 이벤트 로그';
```

- ⚠️ **PRAFTA 관례 예외(plan §8-3)**: 고볼륨 append 로그라 코드 채번 대신 `BIGINT AUTO_INCREMENT` 채택. 이견 시 채번 방식으로 전환 가능.

### 3.3 `TB_TBM_PWD_FAIL` (입실/종료 비번 실패 로그)

```sql
CREATE TABLE `tb_tbm_pwd_fail` (
  `FAIL_NO`       bigint      NOT NULL AUTO_INCREMENT COMMENT '실패 일련번호 (PK)',
  `CMPNY_CD`      varchar(50) NOT NULL COMMENT '회사코드',
  `SESSION_CD`    varchar(20) NOT NULL COMMENT 'TBM 세션코드',
  `PWD_TYPE_CD`   varchar(10) NOT NULL COMMENT '비번유형[SYS055] ENTRY:입실 EXIT:종료',
  `USER_TYPE_CD`  varchar(20) NULL     COMMENT '대상유형[SYS050] REGULAR/DAILY',
  `USER_CD`       varchar(20) NULL     COMMENT '시도자 USER_CD(식별 가능 시)',
  `ATTEMPTED_AT`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '시도 시각',
  `INSERT_NO`     varchar(50) NULL     COMMENT '입력자',
  `INSERT_DATE`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`FAIL_NO`),
  KEY `IX_TBM_PWD_FAIL_01` (`CMPNY_CD`, `SESSION_CD`, `ATTEMPTED_AT`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 비밀번호 실패 로그';
```

- ★ **검토 반영**: 시도한 비번 평문(`ATTEMPTED_PWD`)은 **저장하지 않음**(보안). 실패 횟수/시각만 기록 → 무한재시도 정책의 통계·감사 목적 충족. (security 권고)

---

## 4. 공통코드 시드 (마이그레이션)

### 4.1 SYS018 (교육자료 항목 타입) — PDF 추가

```sql
INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS018', '04', 'PDF', 4, 'Y', 'SYSTEM');
-- 기존: 01 이미지 / 02 동영상 / 03 유튜브 URL (그대로 유지)
```

### 4.2 신규 SYS 코드 그룹 (SYS046~SYS055, 확정)

각 그룹은 `tb_syst_val_m`(마스터 1건) + `tb_syst_val_d`(상세 N건) 시드. 마스터/상세 컬럼은 prafta-031 SYS045 시드 패턴(`tb_syst_val_m(SYST_VAL_CD, SYST_VAL_NM, USE_YN, VAL_DESC, INSERT_NO)`, `tb_syst_val_d(SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN, INSERT_NO)`) 그대로 따른다.

| SYS | 그룹명 | 상세 코드값:의미 (SORT 순) |
|---|---|---|
| SYS046 | TBM 세션 상태 | DRAFT:작성중 / OPENED:개설 / IN_PROGRESS:진행중 / COMPLETED:종료 / CANCELLED:취소 |
| SYS047 | TBM 교육 유형 | TBM:툴박스미팅 |
| SYS048 | TBM GPS 검증유형 | AUTO:자동 / MANUAL:수동확인 / DISABLED:비활성 |
| SYS049 | TBM 동기화 상태 | PLAYING:재생 / PAUSED:정지 |
| SYS050 | TBM 출결 대상유형 | REGULAR:정규직 / DAILY:일용직 |
| SYS051 | TBM 입실 경로 | SELF_DEVICE:본인디바이스 / MANAGER_QR_SCAN:관리자QR |
| SYS052 | TBM 종료 경로 | SELF:본인 / MANAGER_QR_SCAN:관리자QR / MANAGER_FORCED:관리자강제 |
| SYS053 | TBM 이수 상태 | COMPLETED:이수 / NOT_COMPLETED:미이수 |
| SYS054 | TBM 출결 이벤트 유형 | ENTER / START / SLIDE_CHANGED / GPS_UPDATED / BACKGROUND_IN / BACKGROUND_OUT / NETWORK_LOST / SIGNATURE_STARTED / END / FORCED_END |
| SYS055 | TBM 비번 유형 | ENTRY:입실 / EXIT:종료 |

> 착수 시 `SELECT SYST_VAL_CD FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS046'` 등으로 **부재 확인 후** 시드(중복 INSERT 방지). 현재 최대 SYS045 확인 완료.

---

## 5. 콘텐츠 라이브러리 화면 보강 (W-01~03)

> 기존 `tbm01` 모듈(백엔드)과 `Tbm_01.vue` + `popup/TbmEduMtrlInfo.vue`(프론트)를 **보강**. 신규 모듈 만들지 않음.
> 기존 엔드포인트(`/webApi/tbm01/*`) 유지하되 아래 항목 추가/수정.

### 5.1 백엔드 보강 (tbm01)

| # | 항목 | 변경 |
|---|---|---|
| BE-1 | **스코프 필터** | 목록 조회(`selectTbmEduInfo`)에 `siteCd` 파라미터 추가. 조회 조건 = `CMPNY_CD=#{gvCmpnyCd} AND (SITE_CD IS NULL OR SITE_CD = #{siteCd})`. 회사공통+자기사업장 함께 노출 |
| BE-2 | **스코프 저장** | 저장(`mergeTbmEduInfo`)에 `SITE_CD` 반영. `SITE_CD IS NULL`(회사공통) 저장은 **권한 게이트(master/safe)** — 위반 시 `ApiException`(권한 에러코드) |
| BE-3 | **카테고리 필터** | 목록에 `mtrlType`(COM003) 필터 파라미터 (이미 일부 존재 시 재사용) |
| BE-4 | **PDF 타입** | 세부항목 저장/검증에 `MTRL_ITEM_TYPE='04'`(PDF) 허용. 파일 확장자 pdf 검증 |
| BE-5 | **썸네일/길이** | 세부항목 저장 시 `THUMB_FILE_MGMT_CD`, `DURATION_SEC` 매핑. 자동 생성 로직은 기존 파일서비스 활용(없으면 수동 입력 fallback, 후속) |
| BE-6 | **W-03 상세 + 사용이력** | 신규 조회 엔드포인트 `GET /webApi/tbm01/tbm-edu-detail`: 묶음+세부항목 + **이 묶음을 사용한 세션 목록**(`TB_TBM_SESSION_CONTENT` JOIN `TB_TBM_SESSION`, COMPLETED 위주). B 이전에는 빈 목록 |
| BE-7 | **권한 가시성** | 사업장 관리자=자기 사업장+공통만, `999999`=콘텐츠 화면 진입 차단(서버에서도 거부) |
| BE-8 | **소프트 삭제 유지** | 삭제는 기존 `USE_YN='N'`(물리삭제 금지). "사용한 TBM 이력 유지" 보장 |

**DTO 영향**: `TbmEduInfoListRequest/Param/Query`에 `siteCd` 추가, `TbmEduInfoResult`에 `siteCd`/`isCommonContent`(SITE_CD IS NULL 산출) 추가, `TbmEduItemInfoResult`에 `thumbFileMgmtCd`/`durationSec` 추가. W-03용 신규 `TbmEduDetailResponse`(묶음+항목+usedSessions).

### 5.2 프론트 보강 (`Tbm_01.vue`, `TbmEduMtrlInfo.vue`)

| # | 화면 | 변경 (To-Be 05_01 §2.2 보완표 대응) |
|---|---|---|
| FE-1 | W-01 목록 | **스코프 컬럼**(공통/사업장 배지) 추가. 사업장 필터 드롭다운 추가. 검색 디바운싱 500ms |
| FE-2 | W-02 등록/수정 | **스코프 라디오**(회사공통/사업장) 추가. 회사공통은 권한(master/safe) 없으면 비활성+안내 |
| FE-3 | W-02 | 미디어 타입에 **PDF 추가**, 타입별 입력 활성/비활성(파일 vs URL) |
| FE-4 | W-02 | 썸네일 미리보기 컬럼, 버튼 명확화("행 추가"/"선택 행 삭제"/"저장"/"닫기") |
| FE-5 | W-02 | 수정 모드 시 **스코프 변경 불가**(혼란 방지, 05_01 §2.7) |
| FE-6 | W-03 상세 | 신규 상세 뷰: 미디어 미리보기(영상/유튜브/이미지/PDF) + **사용 TBM 이력** 영역 |

- 정렬순서 드래그앤드롭은 기존 라이브러리 사용 여부 확인 후(없으면 ↑↓ 버튼 fallback).
- 모든 색상/간격은 CSS 변수, `<style scoped>`, 공통 컴포넌트 우선 (CLAUDE.md).

---

## 6. 마이그레이션 파일 (산출물)

`prafta-backend/src/main/resources/sql/migration/` 아래:

| 파일 | 내용 |
|---|---|
| `prafta-033-A-alter-content.sql` | §1 기존 콘텐츠 테이블 ALTER (SITE_CD, 썸네일, 길이, 인덱스) |
| `prafta-033-A-tbm-session.sql` | §2 세션/매핑/상태 테이블 4종 CREATE |
| `prafta-033-A-tbm-attendance.sql` | §3 출결/이벤트/비번실패 테이블 3종 CREATE |
| `prafta-033-A-codes.sql` | §4 SYS018 PDF + 신규 코드그룹(SYS046~055) 시드 |

- 각 파일 멱등성 없음(중복 실행 에러) — 상단에 적용 주의 배너. prafta-031 마이그 관례 따름.
- ⚠️ **운영 적용은 사용자 수동**(read-only MCP). developer는 파일만 작성하고 로컬 적용 안내.

---

## 7. 작업 항목 분해 (developer 착수 단위)

1. **A-DDL-1**: §1 ALTER 마이그레이션 작성 + 로컬 적용 안내
2. **A-DDL-2**: §2 세션/매핑/상태 테이블 마이그레이션
3. **A-DDL-3**: §3 출결/이벤트/비번실패 마이그레이션
4. **A-DDL-4**: §4 코드 시드 마이그레이션 (SYS046~055 부재확인 후 시드)
5. **A-BE-1**: tbm01 목록/저장에 스코프(SITE_CD) + 권한 게이트 (BE-1,2,3,7)
6. **A-BE-2**: PDF 타입 + 썸네일/길이 매핑 (BE-4,5)
7. **A-BE-3**: W-03 상세+사용이력 엔드포인트 (BE-6)
8. **A-FE-1**: Tbm_01 목록 보강 (FE-1)
9. **A-FE-2**: TbmEduMtrlInfo 등록/수정 보강 (FE-2~5)
10. **A-FE-3**: W-03 상세 화면 (FE-6) — 신규 컴포넌트

---

## 8. 검증 기준 (QA/Security)

- [ ] ALTER 후 기존 tbm01 화면 회귀 없음 (목록/저장/삭제 정상)
- [ ] 회사공통(SITE_CD NULL) 콘텐츠 등록을 비-master/safe가 시도 → 서버 차단
- [ ] 사업장 관리자가 타 사업장 콘텐츠 조회/수정 불가 (스코프 격리)
- [ ] `999999` 권한 콘텐츠 화면 진입/API 차단
- [ ] PDF 타입 등록·미리보기 정상, SYS018 04 코드 존재
- [ ] 신규 테이블 8종 + ALTER 2종이 실제 스키마와 일치(컬럼명/타입/COMMENT 코드표)
- [ ] 모든 신규 쿼리 `CMPNY_CD` 스코프 포함
- [ ] 소프트 삭제(USE_YN='N') — 물리삭제 없음

---

**다음 단계**: `prafta-033-B-session-mgmt.md` (세션 개설/관리). 본 A의 세션/매핑 테이블 사용.
