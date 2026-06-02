# Prafta 연차 관리 시스템 - 작업 지시서 (v2)

> 마지막 업데이트: 2026-05-19
> 대상: Claude Code (백엔드 + 프론트엔드 일괄 구현)
> 우선순위: MVP 출시 전 필수 기능

---

## 0. 작업 전 필독 사항

### 0.1 이 문서의 사용법

- 각 섹션은 **독립적인 작업 단위**로 구성됨
- 작업 진행 시 **항상 0번 → 1번 → 2번 순서**로 진행할 것 (DDL 먼저, 코드 나중)
- 모든 화면은 `/home/claude/prafta-v2/` 디렉토리의 HTML 시안을 참고
- 참고용 자료는 `00-design-reference.md` 참조

### 0.2 코드 컨벤션 (Prafta 기존 규칙 준수)

```
- 패키지 루트: com.prafta.web.{prefix}.{submodule}
- 레이어: Controller → Service → ServiceImpl → Mapper → XML
- DTO: Request → Param → Command/Query → Result → Response
- 레코드 타입(Param, Command, Query, Result): Java record + 정적 from() 팩토리
- 일반 DTO(Request, Response): Lombok
- 멀티테넌시: 모든 쿼리에 CMPNY_CD 스코프 필수
- XML 쿼리 주석: /* MapperName.methodName */
- MyBatis: <choose>/<when>/<otherwise> 우선, 문자열 비교는 .equals()
- Upsert: MySQL 8.0.20+ 행 별칭 (INSERT ... AS NEW ... ON DUPLICATE KEY UPDATE)
- 보안: AES-GCM (AesGcmUtil), HMAC + SecureRandom (PasswordHasher)
- API URL: kebab-case, update-{domain}-{target} 형식
- 에러: ApiException.appendf()
- 토큰 필드: gvCmpnyCd / gvUserCd (모든 POST Command)
```

### 0.3 v1 작업지시서와의 주요 차이 (v2 변경사항)

| 항목 | v1 | v2 |
|------|-----|-----|
| 휴가 그룹 탭 | 임원/그룹 추가 가능 | **제거** (회사 전체 단일 정책) |
| 사용자 정보 - 근무 형태 | 있음 | **제거** |
| 사용자 정보 - 휴가 그룹 | 있음 | **제거** |
| 경력 인정 적용 방식 | 소급/점진 선택 | **점진으로 통일** |
| 입사일 변경 사유 | 카테고리 + 텍스트 | **자유 텍스트만** |
| 3번 axis 활성 옵션 | 항상 5개 | **1번 axis에 따라 활성/비활성** |
| 사용 단위 - 시작 시각 | 옵션 있음 | **제거** (근무시간 내 고정) |
| 사용 단위 - 1일 환산 시간 | 옵션 있음 | **제거** (스케줄에 비례) |
| 수동 부여 유효기간 | 입력 가능 | **제거** |
| 수동 부여 "부여일" | 명칭 | **"사용 가능일"로 변경** |
| 정책 변경 - 권장 적용일 박스 | 있음 | **제거** |
| 정책 변경 - 위험도 컬럼 | 있음 | **제거** |
| 정책 변경 - 주요 영향 | 자동 생성(규칙 없음) | **5단계 우선순위 규칙 정의** |
| 정책 시뮬레이션 화면 | 있음 | **제거** |
| 직원 일괄 업로드 | 있음 | **별도 기획 (현재 작업 제외)** |

---

## 1. 데이터베이스 변경 (제일 먼저 실행)

### 1.1 TB_USER 컬럼 추가

**이게 뭐다**: 기존 사용자 마스터 테이블에 입사일/고용형태/계약종료일 컬럼을 추가한다. 입사일은 모든 연차 계산의 기준이 되고, 고용형태는 계약직 만료 알림 등 부가 기능에 사용된다.

```sql
ALTER TABLE TB_USER
  ADD COLUMN HIRE_DATE VARCHAR(8) NULL COMMENT '입사일 YYYYMMDD' AFTER BIRTH_DATE,
  ADD COLUMN EMPLOYMENT_TYPE VARCHAR(20) NULL COMMENT '고용형태: REGULAR/CONTRACT/DAILY/EXECUTIVE' AFTER HIRE_DATE,
  ADD COLUMN CONTRACT_END_DATE VARCHAR(8) NULL COMMENT '계약 종료일 YYYYMMDD (계약직만)' AFTER EMPLOYMENT_TYPE;

-- 인덱스 (계약 만료 임박 직원 조회용)
ALTER TABLE TB_USER ADD INDEX IX_TB_USER_CONTRACT (CMPNY_CD, CONTRACT_END_DATE);
```

**중요**:
- `HIRE_DATE`는 이미 추가되어 있을 수 있으니 컬럼 존재 여부 먼저 확인 후 진행
- `LEAVE_GROUP_CD`, `WORK_TYPE_CD`는 **추가하지 않음** (v2에서 제거됨)

### 1.2 운영 기초정보 휴가그룹타입 코드 추가

**이게 뭐다**: 휴가 그룹은 별도 화면 없이 운영 기초정보(`TB_BASE_CD`)에 코드로만 등록해서 활용한다. 사용자 신청 휴가 타입(출산휴가, 교육 등) 생성 시 그룹 라벨로만 사용한다. 법정 연차에는 사용하지 않는다.

```sql
-- TB_BASE_CD 또는 운영 기초정보 테이블 (Prafta 구조에 따라)
INSERT INTO TB_BASE_CD (CMPNY_CD, BASE_CD_GROUP, BASE_CD, BASE_NM, USE_YN, ...)
VALUES
  ('{CMPNY_CD}', 'LEAVE_GROUP', 'DEFAULT', '전체 직원', 'Y', ...),
  ('{CMPNY_CD}', 'LEAVE_GROUP', 'EXECUTIVE', '임원', 'Y', ...),
  ('{CMPNY_CD}', 'LEAVE_GROUP', 'CONTRACT', '계약직', 'Y', ...);
```

**중요**: 기존 운영 기초정보 관리 화면(예: Baim_02)에서 등록 가능하도록 코드 그룹만 추가하면 됨. 별도 화면 개발 불필요.

### 1.3 신규 테이블 7개

#### 1.3.1 TB_USER_SERVICE_CREDIT (경력 인정)

**이게 뭐다**: 직원의 경력 인정(M&A 고용승계, 경력직 입사, 그룹사 이동 등)을 개월 수로 분해 저장하는 테이블. 한 직원이 여러 건의 경력 인정을 가질 수 있다 (예: M&A 60개월 + 핵심인력 보상 12개월). 합산하여 근속 가산 계산에 사용한다.

```sql
CREATE TABLE TB_USER_SERVICE_CREDIT (
  CREDIT_SEQ        BIGINT NOT NULL AUTO_INCREMENT COMMENT '경력 인정 일련번호',
  CMPNY_CD          VARCHAR(20) NOT NULL COMMENT '회사 코드',
  USER_CD           VARCHAR(20) NOT NULL COMMENT '사용자 코드',
  CREDIT_MONTHS     INT NOT NULL COMMENT '인정 개월 수',
  REASON_TYPE       VARCHAR(30) NOT NULL COMMENT '사유 유형: MA_TRANSFER/EXPERIENCE_SAME/EXPERIENCE_DIFF/CONTRACT_TO_REGULAR/GROUP_MOVE/OTHER',
  REASON_DETAIL     VARCHAR(500) NULL COMMENT '상세 설명',
  USE_YN            CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
  REG_USER_CD       VARCHAR(20) NOT NULL,
  REG_DTIME         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MOD_USER_CD       VARCHAR(20) NULL,
  MOD_DTIME         DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (CREDIT_SEQ),
  INDEX IX_USC_USER (CMPNY_CD, USER_CD, USE_YN)
) COMMENT='사용자 경력 인정';
```

**핵심 비즈니스 규칙**:
- 한 직원의 합산 개월: `SELECT SUM(CREDIT_MONTHS) FROM TB_USER_SERVICE_CREDIT WHERE CMPNY_CD=? AND USER_CD=? AND USE_YN='Y'`
- 법적 근속 기준일 계산: `HIRE_DATE - SUM(CREDIT_MONTHS)`개월
- 적용 방식은 **항상 점진 부여**. 즉 근속 가산 시점 계산에만 사용하고, 과거 연차 소급 부여는 하지 않는다.

#### 1.3.2 TB_USER_HIRE_DATE_HISTORY (입사일 변경 이력)

**이게 뭐다**: 입사일이 변경된 이력을 영구 보관하는 테이블. 노무 감사 시 증빙 자료. 변경 전/후 입사일, 변경 사유, 처리 방식, 영향받은 부여 이력 스냅샷을 남긴다.

```sql
CREATE TABLE TB_USER_HIRE_DATE_HISTORY (
  HISTORY_SEQ       BIGINT NOT NULL AUTO_INCREMENT,
  CMPNY_CD          VARCHAR(20) NOT NULL,
  USER_CD           VARCHAR(20) NOT NULL,
  PREV_HIRE_DATE    VARCHAR(8) NOT NULL COMMENT '변경 전 입사일',
  NEW_HIRE_DATE     VARCHAR(8) NOT NULL COMMENT '변경 후 입사일',
  CHANGE_REASON     VARCHAR(1000) NOT NULL COMMENT '변경 사유 (자유 텍스트)',
  HANDLING_TYPE     VARCHAR(30) NOT NULL COMMENT 'KEEP_AND_BACKFILL/KEEP_AND_APPLY_NEW/RESET_ALL',
  AFFECTED_GRANT_SNAPSHOT JSON NULL COMMENT '영향받은 부여 이력 스냅샷',
  CHANGED_BY        VARCHAR(20) NOT NULL COMMENT '변경자',
  CHANGED_AT        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (HISTORY_SEQ),
  INDEX IX_HIRE_HISTORY_USER (CMPNY_CD, USER_CD, CHANGED_AT)
) COMMENT='입사일 변경 이력';
```

**핵심 비즈니스 규칙**:
- 권한: **최고관리자만 INSERT 가능** (서비스 레이어에서 권한 체크)
- 변경 사유는 **카테고리 없이 자유 텍스트만** (v2 변경사항)
- `HANDLING_TYPE`은 3종: 누락분만 소급 / 신규만 적용 / 전체 재계산
- 변경 후 TB_USER의 HIRE_DATE 업데이트와 트랜잭션으로 묶을 것

#### 1.3.3 TB_LEAVE_POLICY (회사 연차 정책)

**이게 뭐다**: 회사별 단일 연차 정책 저장 테이블. 4개 프리셋 중 하나를 선택하거나, 9개 axis를 직접 조합하여 저장한다. CMPNY_CD당 활성 정책은 항상 1개만 유지된다 (USE_YN='Y'로 관리).

```sql
CREATE TABLE TB_LEAVE_POLICY (
  POLICY_SEQ                BIGINT NOT NULL AUTO_INCREMENT,
  CMPNY_CD                  VARCHAR(20) NOT NULL,
  POLICY_PRESET             VARCHAR(30) NOT NULL COMMENT '프리셋: HIRE_DATE / FISCAL_PRORATE / FISCAL_MONTHLY / HIRE_DATE_PREGRANT / CUSTOM',

  -- 9개 axis (직접 설정 시 활용, 프리셋이면 자동 채워짐)
  AXIS1_GRANT_BASE          VARCHAR(20) NOT NULL COMMENT '1번: HIRE_DATE/FISCAL_YEAR',
  AXIS2_FISCAL_START_MM     CHAR(2) NULL COMMENT '2번: 회계연도 시작월',
  AXIS2_FISCAL_START_DD     CHAR(2) NULL COMMENT '2번: 회계연도 시작일',
  AXIS3_FIRST_YEAR_METHOD   VARCHAR(30) NOT NULL COMMENT '3번: MONTHLY_ACCRUAL/PRORATE/PREGRANT/NEXT_YEAR_BULK/NONE',
  -- AXIS4는 법정 의무로 항상 입사일 기준이므로 컬럼 없음
  AXIS5_TENURE_BONUS        VARCHAR(30) NOT NULL COMMENT '5번: YEARLY_FROM_1/BIYEARLY_FROM_3/YEARLY_FROM_5',
  AXIS6_MAX_DAYS            INT NOT NULL DEFAULT 25 COMMENT '6번: 최대 연차일수',
  AXIS7_VALIDITY_MONTHS     INT NOT NULL DEFAULT 12 COMMENT '7번: 유효기간(개월)',
  AXIS8_PRORATE_ROUNDING    VARCHAR(20) NOT NULL DEFAULT 'CEIL' COMMENT '8번: CEIL/ROUND/FLOOR/HALF_DAY',
  AXIS9_USE_PROMOTION       CHAR(1) NOT NULL DEFAULT 'N' COMMENT '9번: 사용촉진 사용여부 Y/N',

  USE_YN                    CHAR(1) NOT NULL DEFAULT 'Y',
  APPLY_FROM_DATE           VARCHAR(8) NOT NULL COMMENT '정책 적용 시작일',
  REG_USER_CD               VARCHAR(20) NOT NULL,
  REG_DTIME                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MOD_USER_CD               VARCHAR(20) NULL,
  MOD_DTIME                 DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (POLICY_SEQ),
  UNIQUE KEY UK_LEAVE_POLICY_ACTIVE (CMPNY_CD, USE_YN, APPLY_FROM_DATE)
) COMMENT='회사별 연차 정책';
```

**핵심 비즈니스 규칙 (절대 어기지 말 것)**:
- 회사당 활성 정책 1개 (`USE_YN='Y'` 기준)
- **3번 axis 활성 매트릭스 검증 (서비스 레이어 필수)**:
  - AXIS1='HIRE_DATE'인 경우: AXIS3은 'MONTHLY_ACCRUAL', 'PREGRANT', 'NONE'만 허용
  - AXIS1='FISCAL_YEAR'인 경우: AXIS3은 'MONTHLY_ACCRUAL', 'PRORATE', 'NEXT_YEAR_BULK', 'NONE'만 허용
  - 매트릭스 위반 시 `ApiException.appendf("AXIS1=%s 일 때 AXIS3=%s는 허용되지 않습니다", axis1, axis3)`
- **AXIS8='HALF_DAY' 선택 시 → TB_LEAVE_USAGE_POLICY의 ALLOW_HALF가 강제 'Y'로 변경되고 해제 불가**

#### 1.3.4 TB_LEAVE_POLICY_HISTORY (정책 변경 이력)

**이게 뭐다**: 정책이 변경될 때마다 전체 axis 스냅샷을 저장하는 이력 테이블. 노무 감사 / 분쟁 시 "그 시점에 어떤 정책이었나"를 추적할 수 있어야 한다.

```sql
CREATE TABLE TB_LEAVE_POLICY_HISTORY (
  HISTORY_SEQ       BIGINT NOT NULL AUTO_INCREMENT,
  CMPNY_CD          VARCHAR(20) NOT NULL,
  POLICY_SEQ        BIGINT NOT NULL COMMENT '변경된 TB_LEAVE_POLICY.POLICY_SEQ',
  CHANGE_TYPE       VARCHAR(20) NOT NULL COMMENT 'CREATE/UPDATE/PRESET_CHANGE',
  PREV_SNAPSHOT     JSON NULL COMMENT '변경 전 정책 전체 스냅샷',
  NEW_SNAPSHOT      JSON NOT NULL COMMENT '변경 후 정책 전체 스냅샷',
  CHANGE_REASON     VARCHAR(500) NULL,
  IMPACT_SUMMARY    JSON NULL COMMENT '영향 분석 결과 (영향 인원, 추가 부담 등)',
  CHANGED_BY        VARCHAR(20) NOT NULL,
  CHANGED_AT        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (HISTORY_SEQ),
  INDEX IX_POLICY_HISTORY (CMPNY_CD, CHANGED_AT)
) COMMENT='정책 변경 이력';
```

#### 1.3.5 TB_LEAVE_USAGE_POLICY (휴가 사용 단위 정책)

**이게 뭐다**: 연차 부여 정책과 별개로, "허용 사용 단위 / 휴게시간 처리 / 다중 신청"을 정의하는 정책. TB_LEAVE_POLICY와 1:1 관계. 시간 단위 시작 시각 제약 / 1일 환산 시간은 v2에서 제거됨.

```sql
CREATE TABLE TB_LEAVE_USAGE_POLICY (
  POLICY_SEQ            BIGINT NOT NULL COMMENT 'TB_LEAVE_POLICY.POLICY_SEQ 1:1',
  CMPNY_CD              VARCHAR(20) NOT NULL,
  ALLOW_FULL_DAY        CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '1일 단위 허용 (항상 Y, 변경불가)',
  ALLOW_HALF_DAY        CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '0.5일 단위',
  ALLOW_QUARTER_DAY     CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '0.25일 단위',
  ALLOW_HOURLY          CHAR(1) NOT NULL DEFAULT 'N' COMMENT '0.125일(1시간) 단위',
  BREAK_TIME_HANDLING   VARCHAR(20) NOT NULL DEFAULT 'AUTO_EXCLUDE' COMMENT 'AUTO_EXCLUDE/INCLUDE/NOT_ALLOWED',
  MAX_DAILY_REQUEST     INT NOT NULL DEFAULT 3 COMMENT '같은 날 최대 신청 건수 (0=불허)',
  MOD_USER_CD           VARCHAR(20) NULL,
  MOD_DTIME             DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (POLICY_SEQ),
  CONSTRAINT FK_LUP_POLICY FOREIGN KEY (POLICY_SEQ) REFERENCES TB_LEAVE_POLICY(POLICY_SEQ)
) COMMENT='휴가 사용 단위 정책';
```

**핵심 비즈니스 규칙**:
- ALLOW_FULL_DAY는 항상 'Y' (체크박스 disabled)
- **TB_LEAVE_POLICY.AXIS8_PRORATE_ROUNDING='HALF_DAY' 일 때 ALLOW_HALF_DAY='Y' 강제**, 'N'으로 저장 시도 시 거부
- 시간 단위 차감은 근로자의 근무 스케줄(`TB_USER_WORK_PLAN`) 시간에 비례하여 처리 (정책 컬럼 없이 로직으로)

#### 1.3.6 TB_LEAVE_GRANT (연차 부여 이력)

**이게 뭐다**: 모든 연차 부여 기록(법정/수동 모두). 시스템이 자동 부여하는 법정연차, 관리자가 수동 부여하는 포상휴가 등이 모두 한 테이블에 적재된다. GRANT_TYPE으로 구분.

```sql
CREATE TABLE TB_LEAVE_GRANT (
  GRANT_SEQ           BIGINT NOT NULL AUTO_INCREMENT,
  CMPNY_CD            VARCHAR(20) NOT NULL,
  USER_CD             VARCHAR(20) NOT NULL,
  GRANT_TYPE          VARCHAR(40) NOT NULL COMMENT
    'STATUTORY_ANNUAL/STATUTORY_MONTHLY/STATUTORY_TENURE_BONUS/'
    'MANUAL_BONUS/MANUAL_CONDOLENCE/MANUAL_LONG_SERVICE/MANUAL_OTHER',
  GRANT_DAYS          DECIMAL(5,2) NOT NULL COMMENT '부여 일수',
  USED_DAYS           DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '사용 일수',
  GRANT_DATE          VARCHAR(8) NOT NULL COMMENT '부여 일자 (시스템 기록 시점)',
  AVAILABLE_FROM      VARCHAR(8) NOT NULL COMMENT '사용 가능 시작일',
  EXPIRES_AT          VARCHAR(8) NOT NULL COMMENT '만료일 (유효기간 계산)',
  GRANT_REASON        VARCHAR(500) NULL COMMENT '부여 사유',
  POLICY_SEQ          BIGINT NULL COMMENT '적용된 정책 (수동 부여는 NULL)',
  IDEMPOTENCY_KEY     VARCHAR(100) NOT NULL COMMENT '중복 부여 방지키 (예: USER_CD_2026_ANNUAL)',
  STATUS              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXHAUSTED/EXPIRED/CANCELED',
  REG_USER_CD         VARCHAR(20) NOT NULL,
  REG_DTIME           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MOD_USER_CD         VARCHAR(20) NULL,
  MOD_DTIME           DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (GRANT_SEQ),
  UNIQUE KEY UK_GRANT_IDEMPOTENCY (CMPNY_CD, IDEMPOTENCY_KEY),
  INDEX IX_GRANT_USER (CMPNY_CD, USER_CD, STATUS, EXPIRES_AT),
  INDEX IX_GRANT_TYPE (CMPNY_CD, GRANT_TYPE, GRANT_DATE)
) COMMENT='연차 부여 이력';
```

**핵심 비즈니스 규칙 (절대 어기지 말 것)**:
- **법정 연차는 자동 부여 시 IDEMPOTENCY_KEY로 중복 방지**: 예) `JOHN_2026_ANNUAL`, `JOHN_2025_05_MONTHLY`
- **GRANT_TYPE 구분 (UI 표시 분류)**:
  - 법정 휴가 (`STATUTORY_*`): 연차 현황 화면의 "법정 휴가" 컬럼에 합산
  - 법정 휴가 외 (`MANUAL_*`): "법정 휴가 외" 컬럼에 합산
- **수동 부여 시 AVAILABLE_FROM = 입력값**, GRANT_DATE = 시스템 기록 시점
- **유효기간 계산**: `EXPIRES_AT = AVAILABLE_FROM + 정책의 AXIS7_VALIDITY_MONTHS`
- **시스템은 절대 GRANT_DAYS를 사후 감소시키지 않음** (정책 변경에도 기존 부여 보호)

#### 1.3.7 TB_LEAVE_USE (연차 사용 이력)

**이게 뭐다**: 연차 사용(신청 승인 완료) 기록. 기존 휴가 신청 테이블이 있다면 그 테이블을 활용해도 됨. 부여 이력(`TB_LEAVE_GRANT`)과 N:M 관계 (한 사용이 여러 부여에서 차감될 수 있음 - FIFO 원칙).

```sql
CREATE TABLE TB_LEAVE_USE (
  USE_SEQ           BIGINT NOT NULL AUTO_INCREMENT,
  CMPNY_CD          VARCHAR(20) NOT NULL,
  USER_CD           VARCHAR(20) NOT NULL,
  GRANT_SEQ         BIGINT NOT NULL COMMENT '차감 대상 부여 이력',
  USE_DATE          VARCHAR(8) NOT NULL,
  USE_DAYS          DECIMAL(5,2) NOT NULL,
  USE_UNIT          VARCHAR(20) NOT NULL COMMENT 'FULL_DAY/HALF_DAY/QUARTER_DAY/HOURLY',
  USE_TIME_FROM     VARCHAR(5) NULL COMMENT 'HH:mm (시간 단위 시)',
  USE_TIME_TO       VARCHAR(5) NULL COMMENT 'HH:mm (시간 단위 시)',
  REQUEST_SEQ       BIGINT NULL COMMENT '연결된 휴가 신청',
  REG_USER_CD       VARCHAR(20) NOT NULL,
  REG_DTIME         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (USE_SEQ),
  INDEX IX_USE_USER (CMPNY_CD, USER_CD, USE_DATE),
  CONSTRAINT FK_LU_GRANT FOREIGN KEY (GRANT_SEQ) REFERENCES TB_LEAVE_GRANT(GRANT_SEQ)
) COMMENT='연차 사용 이력';
```

**핵심 비즈니스 규칙**:
- FIFO 차감: 만료일 빠른 부여부터 차감
- USE_DAYS 합산이 TB_LEAVE_GRANT의 GRANT_DAYS - USED_DAYS 초과 시 차감 거부
- 트랜잭션 내에서 `TB_LEAVE_GRANT.USED_DAYS` 업데이트 동기화

---

## 2. 화면 1: 사용자 정보 팝업 (기존 화면 수정)

### 2.1 기능 설명

**이게 뭐다**: 기존 사용자 정보 팝업(`/main/User_01`)을 수정하여 근태/연차 관련 필드를 추가한다. v2에서 **근무 형태, 휴가 그룹, 적용 방식(소급/점진)은 제거**한다.

### 2.2 수정 사항

#### 추가 필드
| 필드 | 컬럼 | 비고 |
|------|------|------|
| 입사일 | `TB_USER.HIRE_DATE` | readonly + [입사일 수정] 버튼 (master, hr 권한 사용자만) |
| 경력 인정 | `TB_USER_SERVICE_CREDIT` | 토글 ON 시 노출, 다중 항목 입력 가능 |

#### 제거 항목 (v2)
- ~~근무 형태~~ (드롭다운)
- ~~휴가 그룹~~ (드롭다운)
- ~~적용 방식 (소급/점진 라디오)~~ → 항상 점진 부여로 통일

### 2.3 API 명세

```
GET  /api/user/{userCd}/detail          - 사용자 정보 + 경력 인정 조회
POST /api/user/update-user-credit       - 경력 인정 일괄 저장 (delete-and-insert)
PUT  /api/user/{userCd}/employment      - 고용 형태/계약 종료일 업데이트
```

### 2.4 검증 규칙

- EMPLOYMENT_TYPE='계약직' → CONTRACT_END_DATE 필수
- CONTRACT_END_DATE는 HIRE_DATE보다 이후
- 경력 인정 개월은 0 이상 정수

### 2.5 테스트 케이스

```
TC-01: 경력 인정 토글 OFF → 인정 항목 카드 토글처리
TC-02: 경력 인정 2건 입력 (60개월 + 12개월) → 총 인정 72개월 표시
TC-03: [입사일 수정] 버튼 → master, hr 권한이 아니면 비활성
```

---

## 3. 화면 2: 입사일 수정 모달 (신규)

### 3.1 기능 설명

**이게 뭐다**: 사용자 정보 팝업의 [입사일 수정] 버튼 클릭 시 열리는 모달. 입사일을 변경하면 연차 부여, 4대보험 신고, 근속 계산에 모두 영향이 가므로 영향 분석 + 처리 방식 선택 + 사유 입력 후 적용한다. **최고관리자만 사용 가능**.

### 3.2 화면 구성

```
1. 입사일 변경
   - 기존 입사일 (readonly) → 변경할 입사일 (date input)
   - 변경 요약 (자동 계산): 입사일 N일 앞당김/늦춤 + 근속 변화

2. 영향 분석 (자동 계산, 4개 카드)
   - 기존 부여 (변경 전 입사일 기준)
   - 사용된 연차
   - 누락된 부여 (변경 후 기준 본래 받았어야 할 일수)
   - 다음 부여 예정 시점 (변경 후 anniversary)
   + 시나리오 태그: "1년 미만 · 입사일 과거로" 등 4가지 케이스 구분

3. 처리 방식 (라디오, 3택1, 필수) - 각 옵션 상세 안내 포함
   - 기존 부여 유지 + 누락분만 소급 부여 (KEEP_AND_BACKFILL, 권장)
   - 기존 부여 유지 + 신규 부여만 변경된 입사일로 계산 (KEEP_AND_APPLY_NEW)
   - 모든 부여 삭제 후 재계산 (RESET_ALL, 위험)
   각 옵션 카드 내부:
   - 옵션 라벨 + 배지(권장/위험)
   - 옵션 설명 (사용자 친화 문구)
   - 처리 동작 (기술적 동작 상세)
   - "이 케이스 결과" 박스 (현재 입력값 기준 예상 결과)

4. 변경 사유 (자유 텍스트, 필수)

5. 하단 경고 박스 (4대보험/임금/퇴직금 영향 안내)
```

### 3.3 화면 UX 가이드 (운영자 이해 돕기)

화면이 단순 입력폼이 아니라, **운영자가 처리 방식 선택의 결과를 미리 이해할 수 있도록 가이드** 제공:

- **시나리오 태그**: 4가지 케이스 자동 판별하여 상단 표시
  - "1년 미만 · 입사일 과거로" / "1년 미만 · 입사일 미래로"
  - "1년 초과 · 입사일 과거로" / "1년 초과 · 입사일 미래로"

- **각 처리 옵션 카드에 "이 케이스 결과" 영역**: 현재 시나리오 + 옵션 조합 시 실제 부여될 내용을 사전 표시

- **유효기간 보존 안내**: "이미 부여된 연차의 유효기간은 발생일 기준으로 결정되어 보존되며, 입사일 변경 후에도 변경되지 않습니다."

- **위험 시나리오 자동 경고**:
  - 입사일을 미래로 변경 + 사용 이력 있음 → "이미 사용한 연차가 새 입사일 기준으로는 과다 부여 상태가 됩니다" 경고
  - 입사일 변경 폭 12개월 이상 → "변경 폭이 큽니다. 신중히 검토하세요" 경고

### 3.4 4가지 시나리오 처리 매트릭스

화면에서 사용자가 입력한 (현재 입사일, 새 입사일, 오늘) 조합으로 자동 시나리오 판별:

| 시나리오 | 현재 상태 | 입사일 방향 | 권장 처리 방식 |
|---------|----------|-------------|---------------|
| A | 1년 미만 | 과거로 | **KEEP_AND_BACKFILL** |
| B | 1년 미만 | 미래로 | **KEEP_AND_APPLY_NEW** (사용 이력 있을 시) |
| C | 1년 초과 | 과거로 | **RESET_ALL** (입사일 정책 시) / KEEP_AND_BACKFILL (회계연도 정책 시) |
| D | 1년 초과 | 미래로 | **KEEP_AND_APPLY_NEW** (사용 이력 있을 시) |

**자동 권장 옵션**: 시나리오 판별 결과 권장 옵션을 화면에 [권장] 배지로 표시.

### 3.5 처리 방식별 상세 동작

#### 3.5.1 KEEP_AND_BACKFILL (누락분만 소급 부여) - 권장

```
[동작 흐름]
1. 기존 TB_LEAVE_GRANT는 그대로 유지 (회수 없음)
2. 변경 후 입사일 기준으로 본래 발생했어야 할 부여 목록 생성
   - 월차: 변경 후 입사일 기준 매월 발생일 (최대 11개월)
   - 본연차: 1년 도래 시점 (anniversary)
   - 회계연도 정책 시: 비례부여 또는 차년도 일괄 부여
3. 각 부여마다 IDEMPOTENCY_KEY 생성하여 중복 확인
   - 형식: {USER_CD}_{YYYY}_{TYPE} 또는 {USER_CD}_{YYYYMM}_MONTHLY
4. 기존에 동일 KEY가 없는 항목만 신규 INSERT
5. TB_USER.HIRE_DATE 업데이트
6. TB_USER_HIRE_DATE_HISTORY에 변경 이력 + 영향 스냅샷 INSERT

[유효기간]
- 신규 부여의 EXPIRES_AT = AVAILABLE_FROM + AXIS6_VALIDITY_MONTHS
- 기존 부여의 EXPIRES_AT은 변경하지 않음
```

#### 3.5.2 KEEP_AND_APPLY_NEW (신규만 변경된 입사일로 계산)

```
[동작 흐름]
1. 기존 TB_LEAVE_GRANT는 그대로 유지
2. 누락된 부여는 추가하지 않음
3. TB_USER.HIRE_DATE 업데이트
4. 다음 정기 배치(법정 연차 자동 부여 배치)부터 새 입사일 기준 계산
   - 이미 IDEMPOTENCY_KEY가 발급된 시점에 대해서는 중복 부여하지 않음
   - 다음 새 anniversary부터 정상 부여

[주의]
- 입사일을 과거로 변경한 경우: 누락분 손해 가능 → 경고 표시
- 입사일을 미래로 변경한 경우: 적합 (이미 받은 연차 보존)
```

#### 3.5.3 RESET_ALL (전체 재계산) - 위험

```
[동작 흐름]
1. 기존 STATUTORY_* GRANT를 STATUS='CANCELED'로 변경 (소프트 삭제)
   - 데이터는 보존, 감사 추적 가능
   - MANUAL_* GRANT는 영향 없음 (수동 부여는 별도)
2. TB_LEAVE_USE 사용 이력은 그대로 보존 (삭제 X)
   - GRANT_SEQ 외래키는 CANCELED된 GRANT를 가리키지만 데이터 무결성 유지
3. 변경 후 입사일 기준으로 전체 재발급
4. 사용 이력 처리:
   - 사용 일수 총량을 새 GRANT의 USED_DAYS에 합산 반영
   - 1:1 매핑은 하지 않음 (시점이 다르므로 불가능)
5. TB_USER.HIRE_DATE 업데이트
6. TB_USER_HIRE_DATE_HISTORY에 전체 변경 이력 + 스냅샷 INSERT

[주의]
- 입사일을 미래로 변경 + 사용 이력 있는 경우: 과다 사용 충돌 → 차단 권장
- 단순 입력 오류 정정 시에만 사용 권장
```

### 3.6 사전 검증 로직

```java
public ValidationResult validateHireDateChange(Employee emp, LocalDate newHireDate) {
    // 1. 권한 체크 (최고관리자)
    if (!isAdmin(currentUser)) {
        return ValidationResult.error("최고관리자만 입사일을 변경할 수 있습니다.");
    }

    // 2. 입사일을 미래로 변경 + 사용 이력 있음 → 경고
    if (newHireDate.isAfter(emp.hireDate) && hasUsageHistory(emp)) {
        long usedDays = countUsedDaysAfter(emp, newHireDate);
        long shouldBeGranted = calculateGrantsAsOf(newHireDate, today);
        if (usedDays > shouldBeGranted) {
            return ValidationResult.warning(
                String.format("이미 사용한 연차 %d일이 새 입사일 기준 발생 가능한 %d일을 초과합니다. 수동 정산이 필요할 수 있습니다.",
                    usedDays, shouldBeGranted));
        }
    }

    // 3. 변경 폭 12개월 이상 → 경고
    long monthsDiff = ChronoUnit.MONTHS.between(emp.hireDate, newHireDate);
    if (Math.abs(monthsDiff) > 12) {
        return ValidationResult.warning("입사일 변경 폭이 12개월 이상입니다. 신중히 검토하세요.");
    }

    // 4. 미래 30일 이상 → 차단
    if (newHireDate.isAfter(today.plusDays(30))) {
        return ValidationResult.error("입사일을 30일 이상 미래로 설정할 수 없습니다.");
    }

    return ValidationResult.ok();
}
```

### 3.7 API 명세

```
GET  /api/user/{userCd}/hire-date-impact?newDate=YYYYMMDD
     → 영향 분석 결과 반환
     Response: {
       currentHireDate, newHireDate,
       changeDirection: "PAST" | "FUTURE",
       scenario: "UNDER_1Y_PAST" | "UNDER_1Y_FUTURE" | "OVER_1Y_PAST" | "OVER_1Y_FUTURE",
       recommendedHandling: "KEEP_AND_BACKFILL" | "KEEP_AND_APPLY_NEW" | "RESET_ALL",
       summary: {
         existingGrant: { monthlyDays, annualDays },
         usedDays,
         missingGrant: { monthlyDays, annualDays },
         nextGrantDate
       },
       warnings: ["..."],
       perOptionResult: {
         KEEP_AND_BACKFILL: { newlyGranted: { ... }, description: "..." },
         KEEP_AND_APPLY_NEW: { newlyGranted: { ... }, description: "..." },
         RESET_ALL: { newlyGranted: { ... }, canceledGrants: N, description: "..." }
       }
     }

POST /api/user/update-user-hire-date
     → 입사일 변경 적용
     Body: { userCd, newHireDate, handlingType, changeReason }
     Response: { success, affectedGrantSeqs, newGrantSeqs }
```

### 3.8 트랜잭션 처리

```java
@Transactional
public void updateUserHireDate(HireDateChangeCommand command) {
  // 1. 권한 체크 (최고관리자)
  validatePermission(currentUser);

  // 2. 사전 검증
  ValidationResult vr = validateHireDateChange(emp, command.newHireDate());
  if (vr.isError()) throw ApiException.appendf(vr.message());
  // 경고는 사용자가 화면에서 확인 후 force=true로 호출했다고 가정

  // 3. 영향 분석 재실행 (스냅샷용)
  ImpactAnalysis impact = analyzeImpact(emp, command.newHireDate());

  // 4. TB_USER_HIRE_DATE_HISTORY INSERT (스냅샷 포함)
  insertHireDateHistory(emp, command, impact);

  // 5. TB_USER.HIRE_DATE UPDATE
  updateUserHireDate(emp.userCd(), command.newHireDate());

  // 6. HANDLING_TYPE에 따른 TB_LEAVE_GRANT 처리
  switch (command.handlingType()) {
    case KEEP_AND_BACKFILL:
      backfillMissingGrants(emp, command.newHireDate());
      break;
    case KEEP_AND_APPLY_NEW:
      // 추가 처리 없음 (HIRE_DATE 업데이트만)
      break;
    case RESET_ALL:
      cancelAllStatutoryGrants(emp);
      regrantFromScratch(emp, command.newHireDate());
      break;
  }
}
```

### 3.9 핵심 비즈니스 규칙

1. **시스템은 절대 기 발생 연차를 자동 차감하지 않음** (회수 없음, RESET_ALL은 CANCELED 처리)
2. **IDEMPOTENCY_KEY로 중복 부여 방지** (KEEP_AND_BACKFILL의 핵심)
3. **사용 이력(TB_LEAVE_USE)은 절대 삭제하지 않음** (RESET_ALL에서도 보존)
4. **유효기간(EXPIRES_AT)은 GRANT 시점에 결정되어 사후 변경하지 않음**
5. **회계연도 정책 + 1년 미만 케이스**: BACKFILL 시 비례부여 차이도 누락분으로 인식하여 추가 부여
6. **MANUAL_* GRANT는 입사일 변경에 영향 없음** (수동 부여는 별도)
7. **변경 이력은 TB_USER_HIRE_DATE_HISTORY에 영구 보관** (노무 감사용)

### 3.10 테스트 케이스

```
TC-01: 권한 없는 사용자가 호출 → 403 거부
TC-02: 변경 사유 미입력 → 저장 실패
TC-03: 영향 분석 결과가 화면 표시와 트랜잭션 처리 모두 동일한지 검증
TC-04: 미래 30일 이상으로 입사일 설정 → 검증 실패

[시나리오 A: 1년 미만 + 입사일 과거로]
TC-05: 입사 25.07.15, 오늘 26.05.02, 새 입사일 25.03.01, KEEP_AND_BACKFILL
       → 월차 2일 신규 부여 (기존 9 + 신규 2 = 11) + 본연차 15일 신규 부여
TC-06: 동일 조건, KEEP_AND_APPLY_NEW
       → 기존 부여 유지, 추가 부여 없음, HIRE_DATE만 업데이트
TC-07: 동일 조건, RESET_ALL
       → 기존 GRANT 9건 CANCELED, 새 부여 26일 (월차 11 + 본연차 15) 발급

[시나리오 B: 1년 미만 + 입사일 미래로]
TC-08: 입사 25.07.15, 오늘 26.07.17, 새 입사일 25.09.25,
       기존 부여 26일 모두 사용 완료 상태에서 KEEP_AND_APPLY_NEW
       → 기존 부여/사용 보존, HIRE_DATE만 업데이트
TC-09: 동일 조건에서 RESET_ALL 시도 → 경고 표시 (과다 사용 충돌)

[시나리오 C: 1년 초과 + 입사일 과거로]
TC-10: 입사 22.07.15, 오늘 26.05.02, 새 입사일 22.01.01, RESET_ALL
       → 기존 GRANT 모두 CANCELED, 새 입사일 기준 전체 재발급
       (월차 11 + 23년 본연차 15 + 24년 15 + 25년 16 + 26년 16)

[시나리오 D: 1년 초과 + 입사일 미래로]
TC-11: 입사 22.07.15, 오늘 26.07.17, 새 입사일 22.12.12,
       26.07.15 부여 본연차 모두 사용 완료 상태에서 KEEP_AND_APPLY_NEW
       → 기존 부여/사용 보존, 다음 anniversary부터 새 기준

[유효기간 보존]
TC-12: 기존 부여의 EXPIRES_AT은 입사일 변경 후에도 동일하게 유지되는지 검증
TC-13: 신규 부여(BACKFILL/RESET_ALL)의 EXPIRES_AT은 AVAILABLE_FROM + AXIS6_VALIDITY_MONTHS로 정확히 계산되는지 검증
```

---

## 4. 화면 3: 연차 부여 정책 (신규)

### 4.1 기능 설명

**이게 뭐다**: 회사 전체 연차 부여 정책을 설정하는 메인 화면. 4개 프리셋 카드 중 선택하거나 [직접 설정하기]로 9개 axis를 조합한다. **v2에서 휴가 그룹 탭 제거됨** (회사 전체 단일 정책).

### 4.2 화면 구성

```
1. 추천 정책 (4개 프리셋 카드)
   - 입사일 기준
   - 회계연도 기준 (비례부여)
   - 회계연도 기준 (월차누적)
   - 입사일 기준 (일괄선부여)
2. [직접 설정하기] → 화면 4로 이동
3. 공통 옵션
   - 회계연도 시작일 (회계연도 기준(비례부여, 월차누적) 시만 활성)
   - 연차 유효기간
   - 비례 부여 시 반올림 (비례 부여 선택 시만 활성)
    - 올림(근로자에게 유리), 반올림(표준방식), 내림(회사에 유리), 0.5일 단위 절사(반차 운영 회사용)
   - 연차 사용촉진 제도
4. 고급 기능
   - 정책 변경 영향 분석 (화면 8로 이동)
5. [취소] [저장]
```

### 4.2.1 프리셋 별 예시
# 2025년 7월 15일 입사자를 기준으로 한 예시 가이드
1. 입사일 기준
  2025-07-15  입사 → 본연차 0일

  [1년 미만: 월차 발생 + 본연차는 anniversary까지 대기]
  2025-08-15  월차 +1일 (만료: 2026-08-14)
  2025-09-15  월차 +1일
  2025-10-15  월차 +1일
  2025-11-15  월차 +1일
  2025-12-15  월차 +1일
  2026-01-15  월차 +1일
  2026-02-15  월차 +1일
  2026-03-15  월차 +1일
  2026-04-15  월차 +1일
  2026-05-15  월차 +1일
  2026-06-15  월차 +1일
  ────────── 월차 총 11일 발생

  2026-07-15  ⭐ 입사 1년 도래 (anniversary)
              → 본연차 15일 일괄 부여
              → 이 시점부터 "정상" 운영 시작
              
              잔여:
              - 월차 11일 (각각 만료일 다름, 가장 빠른 게 2026-08-14 만료)
              - 본연차 15일 (만료: 2027-07-14)
              - 합계 26일 ← 1년차 법정 최대치

  2027-07-15  본연차 +15일 (2년차)
  2028-07-15  본연차 +15일 (3년차) + 근속 가산 +1일 = 16일 ← AXIS5 적용 시작

2. 회계연도 기준 (비례부여)
  2025-07-15  입사 → 본연차 0일

  [입사 후 ~ 회계연도 도래까지: 월차만]
  2025-08-15  월차 +1일
  2025-09-15  월차 +1일
  2025-10-15  월차 +1일
  2025-11-15  월차 +1일
  2025-12-15  월차 +1일
  ────────── 월차 5일 누적

  2026-01-01  ⭐ 회계연도 도래 → 비례 부여 발동
              계산: 15일 × (잔여 비율)
              예) 15 × 6/12 = 7.5일 → 올림 8일 (AXIS8 = CEIL)
              → 본연차 8일 일괄 부여 (만료: 2026-12-31 또는 +12개월)
              
              잔여:
              - 월차 5일
              - 본연차(비례) 8일
              - 합계 13일

  [회계연도 ~ 입사 1년 도래까지: 월차 계속 발생]
  2026-01-15  월차 +1일
  2026-02-15  월차 +1일
  2026-03-15  월차 +1일
  2026-04-15  월차 +1일
  2026-05-15  월차 +1일
  2026-06-15  월차 +1일
  ────────── 월차 총 11일 (5+6) 누적

  2026-07-15  입사 1년 도래 → 월차 발생 종료 (4번 axis 종료)
              ※ 비례 부여 정책에서는 1년 도래 시점에 본연차 새로 안 줌
                (이미 회계연도에서 비례로 받았기 때문)

  2027-01-01  ⭐ 첫 정상 회계연도 부여 → 본연차 +15일
              → 이 시점부터 "정상" 운영 시작

  2028-01-01  본연차 +15일
  2029-01-01  본연차 +15일 + 근속 가산 (입사 기준 3년차) +1일 = 16일

3. 회계연도 기준 (월차누적)
  2025-07-15  입사 → 본연차 0일

  [입사 첫해 ~ 입사 1년 도래까지: 월차만 (비례 부여 없음)]
  2025-08-15  월차 +1일
  2025-09-15  월차 +1일
  2025-10-15  월차 +1일
  2025-11-15  월차 +1일
  2025-12-15  월차 +1일

  2026-01-01  회계연도 도래 → 그러나 첫해는 본연차 부여 없음
              (3번 axis가 MONTHLY_ACCRUAL이라 첫해는 월차로만)
              
              잔여:
              - 월차 5일
              - 본연차 0일

  2026-01-15  월차 +1일
  2026-02-15  월차 +1일
  2026-03-15  월차 +1일
  2026-04-15  월차 +1일
  2026-05-15  월차 +1일
  2026-06-15  월차 +1일
  ────────── 월차 총 11일

  2026-07-15  입사 1년 도래 → 월차 발생 종료
              ※ 본연차도 여기서 안 줌 (회계연도 정책이라 회계연도 시작일에 줌)

  2027-01-01  ⭐ 첫 정상 회계연도 부여 → 본연차 +15일
              → 이 시점부터 "정상" 운영 시작

  2028-01-01  본연차 +15일
  2029-01-01  본연차 +15일 + 근속 가산 +1일 = 16일

4. 입사일 기준 (일괄선부여)
  2025-07-15  ⭐ 입사 즉시 본연차 11일 일괄 부여
              (만료: 2026-07-14)
              
              잔여:
              - 본연차(일괄) 11일

  [1년 미만: 월차도 발생 (법정), 일괄 부여와 별도]
  2025-08-15  월차 +1일 (만료: 2026-08-14)
  2025-09-15  월차 +1일 (만료: 2026-09-14)
  2025-10-15  월차 +1일 (만료: 2026-10-14)
  2025-11-15  월차 +1일 (만료: 2026-11-14)
  2025-12-15  월차 +1일 (만료: 2026-12-14)
  2026-01-15  월차 +1일 (만료: 2027-01-14)
  2026-02-15  월차 +1일 (만료: 2027-02-14)
  2026-03-15  월차 +1일 (만료: 2027-03-14)
  2026-04-15  월차 +1일 (만료: 2027-04-14)
  2026-05-15  월차 +1일 (만료: 2027-05-14)
  2026-06-15  월차 +1일 (만료: 2027-06-14)
  ────────── 월차 총 11일 추가 발생

              잔여 (2026-06-15 시점):
              - 본연차(일괄) 11일 - 사용분
              - 월차 11일 - 사용분
              - 합계 최대 22일

  2026-07-14  일괄 부여 11일 만료
              ※ 미사용분이 있으면 모두 소멸 (사용촉진 미운영 시 수당 정산 필요)

  2026-07-15  ⭐ 입사 1년 도래 (anniversary) → 본연차 +15일
              → 이 시점부터 "정상" 운영 시작

              잔여:
              - 월차 11일 중 만료 전 남은 분 (2026-08-14부터 순차 만료)
              - 본연차 15일 (만료: 2027-07-14)

  2027-07-15  본연차 +15일
  2028-07-15  본연차 +15일 + 근속 가산 +1일 = 16일

### 4.3 v2 제거 항목

- ~~휴가 그룹 탭 (전체 직원/임원/+그룹 추가)~~
- ~~정책 시뮬레이션 진입 카드~~

### 4.4 API 명세

```
GET  /api/leave-policy                    - 현재 정책 조회 (USE_YN='Y')
POST /api/leave-policy/save-policy        - 정책 저장 (Upsert)
GET  /api/leave-policy/history            - 정책 변경 이력 조회
```

### 4.5 검증 규칙

- 프리셋 선택 시 9개 axis 값이 자동 채워짐 (서비스 레이어)
- 회계연도 시작일은 1~12월, 1~31일 (월별 마지막 날 검증)
- 저장 시 TB_LEAVE_POLICY_HISTORY에 변경 스냅샷 INSERT

### 4.6 테스트 케이스

```
TC-01: "입사일 기준" 프리셋 선택 → AXIS1=HIRE_DATE, AXIS3=MONTHLY_ACCRUAL 자동 설정
TC-02: "입사일 기준 (일괄선부여)" 선택 → AXIS1=HIRE_DATE, AXIS3=PREGRANT
TC-03: 정책 저장 후 TB_LEAVE_POLICY_HISTORY에 NEW_SNAPSHOT 기록 확인
TC-04: 회계연도 시작일에 13월 입력 → 검증 실패
```

---

## 5. 화면 4: 연차 정책 직접 설정 (신규) ⭐ 핵심

### 5.1 기능 설명

**이게 뭐다**: 4개 프리셋에 맞지 않는 회사용. 7개 의사결정 axis를 조합하여 커스텀 정책을 생성한다. **각 axis는 서로 의존성이 있으며 잘못된 조합을 막기 위한 검증 로직이 필수**.

### 5.2 7개 axis 정의

| # | axis | 컬럼 | 옵션 / 입력 |
|---|------|------|-----------|
| 1 | 연차 부여 기준 | AXIS1_GRANT_BASE | HIRE_DATE / FISCAL_YEAR |
| 2 | 회계연도 시작일 | AXIS2_FISCAL_START_MM/DD | 1~12월, 1~31일 (AXIS1=FISCAL_YEAR 일 때만) |
| 3 | 입사 첫해 처리 방식 | AXIS3_FIRST_YEAR_METHOD | MONTHLY_ONLY / PRORATE / NEXT_YEAR_BULK |
| 4 | 비례 부여 시 반올림 | AXIS4_PRORATE_ROUNDING | CEIL / ROUND / FLOOR / HALF_DAY (AXIS3=PRORATE 일 때만 활성) |
| 5 | 근속 가산 정책 | AXIS5_TENURE_MODE + AXIS5_START_YEAR + AXIS5_INTERVAL + AXIS5_MAX_DAYS | LEGAL / CUSTOM + n, m, max |
| 6 | 연차 유효기간 | AXIS6_VALIDITY_MONTHS | 12(법정) / 24(연장) |
| 7 | 사용촉진 제도 | AXIS7_USE_PROMOTION | Y / N |

**참고**: 1년 미만 월차(법정 의무, 매월 만근 시 1일씩 최대 11일)는 정책과 무관하게 항상 자동 부여되므로 axis로 두지 않음. 3번 axis 카드 내에 안내문으로 표시.

### 5.2.1 axis 변경 사항 요약 (v1 → 현재)

| 변경 항목 | 변경 내용 |
|----------|----------|
| 9개 → 7개 | 구 5+6번(근속 가산 + 최대 일수) 통합, 구 4번(법정 월차) axis 제거(안내문 통합), 구 PREGRANT 옵션 제거 |
| 3번 axis 옵션 | 5개 → 3개. NONE 제거, PREGRANT(일괄 부여) 제거 (1년차 37일 발생하는 비현실적 옵션), MONTHLY_ACCRUAL → MONTHLY_ONLY로 라벨 변경 |
| 구 4번 axis (법정 월차) | 별도 카드 제거 → 3번 axis 카드 내 안내문으로 통합 |
| 새 4번 axis | 구 8번(비례 부여 반올림)을 4번 자리로 이동. 3번과 인접 배치로 일관성 향상 |
| 새 5번 axis | 법정/회사 정책 라디오 + 자유 입력(n, m, max) 혼합 방식 |
| 6번 axis 옵션 | 3개 → 2개. "회계연도말까지" 제거 (법정 12개월 위반 가능성) |
| 사용 단위 정책 | "휴게시간 처리" 옵션 제거 (시스템에서 휴게시간 자동 제외 + 신청 불가 강제). "회사가 허용하는 사용 단위"와 "같은 날 다중 신청"을 좌우로 배치 |

### 5.3 ⭐ 3번 axis 활성/비활성 매트릭스 (필수 구현)

| 3번 옵션 | AXIS1=HIRE_DATE | AXIS1=FISCAL_YEAR |
|---------|:---:|:---:|
| MONTHLY_ONLY (월차만 부여, 별도 본연차 없음) | ✅ | ✅ |
| PRORATE (회계연도 시점 비례 본연차 추가) | ❌ | ✅ |
| NEXT_YEAR_BULK (차년도 시점 15일 일괄) | ❌ | ✅ |

**구현 방법**:
- 프론트엔드: AXIS1 변경 시 AXIS3 옵션의 disabled 토글
- 백엔드: 저장 시 매트릭스 위반 검증 후 거부 (`ApiException.appendf("AXIS1=%s 일 때 AXIS3=%s는 허용되지 않습니다", axis1, axis3)`)
- **AXIS1=HIRE_DATE 일 때 AXIS3는 사실상 MONTHLY_ONLY 한 가지만 선택 가능** (회계연도 의존 옵션 모두 비활성)

### 5.4 ⭐ 3번 axis와 법정 월차의 관계 (중요)

- **3번 axis (본연차 정책)**: 입사 첫해의 본연차 추가 부여 방식 (회사 정책)
- **법정 월차 (axis 아님)**: 1년 미만 매월 만근 시 1일씩 부여 (근로기준법 제60조 제2항, 정책 무관 강제)

두 가지는 **별개로 동작**한다. 예를 들어 AXIS3=NEXT_YEAR_BULK인 경우:
- 법정 월차: 입사일 기준 매월 발생 (최대 11일)
- 3번 본연차: 차년도 회계연도 시작일에 15일 일괄

두 개가 모두 부여된다. UI에서 3번 axis 카드 내 안내문으로 명확히 표시.

**참고: MONTHLY_ONLY는 "본연차 추가 부여 없이 법정 월차로만 운영한다"는 선언**. 법정 월차 자체는 어떤 정책에서도 동일하게 발생.

### 5.5 ⭐ 4번 axis와 사용 단위 정책의 관계

**AXIS4_PRORATE_ROUNDING='HALF_DAY' 선택 시**:
- TB_LEAVE_USAGE_POLICY.ALLOW_HALF_DAY를 'Y'로 자동 설정
- 프론트엔드: 0.5일 체크박스가 자동 체크되고 disabled 처리
- 백엔드: 저장 시 강제 'Y' 처리 (사용자가 'N'으로 보내도 무시)

### 5.6 ⭐ 4번 axis Conditional 활성 처리

4번 axis는 **AXIS3=PRORATE 일 때만 의미가 있음**. 다른 옵션에서는 비례 계산이 발생하지 않으므로 비활성 처리.

**구현 방법**:
- 프론트엔드: 3번 axis 변경 시 4번 axis 영역 활성/비활성 토글 + opacity 처리
- AXIS3 != PRORATE 시: 4번 axis 옵션 모두 disabled + "조건부 활성" 배지 노출
- AXIS3 == PRORATE 시: 4번 axis 활성화 + 4개 옵션 선택 가능
- 백엔드: AXIS3 != PRORATE 시 AXIS4_PRORATE_ROUNDING은 기본값('CEIL') 저장

### 5.7 ⭐ 5번 axis 근속 가산 정책 상세

구 5번(근속 가산 시점) + 구 6번(최대 연차일수)을 통합한 axis. **라디오 모드 + 직접 입력**의 혼합 방식.

#### 5.7.1 컬럼 구조

| 컬럼 | 타입 | 설명 |
|------|------|------|
| AXIS5_TENURE_MODE | VARCHAR(10) | `LEGAL` / `CUSTOM` |
| AXIS5_START_YEAR | INT | 가산 시작 연차 (n) — LEGAL 시 자동 3 |
| AXIS5_INTERVAL | INT | 가산 주기 (m) — LEGAL 시 자동 2 |
| AXIS5_MAX_DAYS | INT | 최대 연차일수 (기본 25) |

#### 5.7.2 입력 규칙

```
LEGAL 모드:
- n = 3 (고정), m = 2 (고정)
- max_days만 입력 가능 (기본 25, min 25)

CUSTOM 모드:
- n: 1~3 (법정 위반 방지, 3 초과 시 가산 발생이 늦어져 근로자 손해)
- m: 1~2 (법정 위반 방지, 2 초과 시 가산 주기가 길어져 근로자 손해)
- max_days: 25~40 (25 미만 시 법정 위반)
```

#### 5.7.3 검증 규칙

```
1. AXIS5_MAX_DAYS >= 25 (법정 위반 시 거부)
2. AXIS5_MODE='LEGAL' 시: AXIS5_START_YEAR=3, AXIS5_INTERVAL=2 강제
3. AXIS5_MODE='CUSTOM' 시:
   - 1 <= AXIS5_START_YEAR <= 3
   - 1 <= AXIS5_INTERVAL <= 2
4. 위반 시: ApiException.appendf("근속 가산 정책이 법정 기준보다 회사에 유리합니다. 법정 위반.")
```

#### 5.7.4 부여 계산 로직

```java
public int calculateAnnualDays(int yearOfService, int startYear, int interval, int maxDays) {
    int baseDays = 15;
    if (yearOfService < startYear) {
        return baseDays;
    }
    int bonus = (yearOfService - startYear) / interval + 1;
    return Math.min(baseDays + bonus, maxDays);
}
```

#### 5.7.5 미리보기 (프론트엔드 표시)

화면에서 입력값에 따라 실시간으로 부여 시뮬레이션을 표시:

```
LEGAL 기본값 (n=3, m=2, max=25):
  1~2년차: 15일
  3년차:   16일
  5년차:   17일
  7년차:   18일
  ...
  21년차:  25일 (최대 도달)

CUSTOM 예시 (n=1, m=1, max=25):
  1년차:   16일
  2년차:   17일
  3년차:   18일
  ...
  11년차:  25일 (최대 도달)
```

### 5.8 ⭐ 휴가 사용 단위 정책

3번 axis 그룹과 별도 섹션으로 노출. 좌우 2단 배치:

| 영역 | 위치 | 입력 |
|------|------|------|
| 회사가 허용하는 사용 단위 | 좌측 | 1일(필수) / 0.5일 / 0.25일 / 0.125일 다중 선택 |
| 같은 날 다중 신청 | 우측 | 허용(N건) / 불허 |

**제거된 항목**:
- ~~휴게시간 처리 옵션~~ → 시스템에서 일률적으로 "휴게시간 자동 제외 + 신청 불가" 강제
- ~~시간 단위 시작 시각 제약~~ → 기본값 "근무 시간 내에서만" 고정
- ~~1일 환산 시간~~ → 근무 스케줄 시간에 비례하여 자동 처리

**휴게시간 처리 로직 (시스템 강제)**:
- 시간 단위 휴가 신청 시 근로자별 근무 스케줄(TB_USER_WORK_PLAN)의 휴게시간을 자동 인식
- 휴게시간을 가로지르는 시간대는 신청 자체가 불가능 (UI에서 시간대 선택 비활성)
- 이유: 사용 단위 정합성 보장 (0.25일 신청했는데 휴게시간 제외로 0.125일만 차감되는 모순 방지)

### 5.9 시간 단위 차감 로직 (참고)

```
사용 단위 → 차감 시간 계산
- FULL_DAY     → 사용자 스케줄 1일 전체
- HALF_DAY     → 사용자 스케줄 1일의 50%
- QUARTER_DAY  → 사용자 스케줄 1일의 25%
- HOURLY       → 사용자 스케줄 1일의 12.5% (=0.125일)

예: 8시간 근무자
- HOURLY 1회 = 1시간 차감 (=0.125일)
예: 6시간 근무자 (단축근무)
- HOURLY 1회 = 0.75시간 차감 (=0.125일)
```

### 5.10 검증 규칙 (전체)

```
1. AXIS1 + AXIS3 매트릭스 검증
2. AXIS3 = PRORATE 일 때 → AXIS1 = FISCAL_YEAR 필수
3. AXIS3 = NEXT_YEAR_BULK 일 때 → AXIS1 = FISCAL_YEAR 필수
4. AXIS4 = HALF_DAY → TB_LEAVE_USAGE_POLICY.ALLOW_HALF_DAY='Y' 강제
5. AXIS3 != PRORATE → AXIS4는 기본값('CEIL') 저장 (사용자 입력 무시)
6. AXIS5_MODE = CUSTOM 일 때:
   - 1 <= AXIS5_START_YEAR <= 3
   - 1 <= AXIS5_INTERVAL <= 2
7. AXIS5_MAX_DAYS >= 25 (법정 위반 시 거부)
8. AXIS6_VALIDITY_MONTHS >= 12 (법정 최소)
9. 저장 시 TB_LEAVE_POLICY_HISTORY에 변경 스냅샷 기록
```

### 5.11 테스트 케이스

```
TC-01: AXIS1=HIRE_DATE 선택 → AXIS3의 PRORATE, NEXT_YEAR_BULK 비활성 (MONTHLY_ONLY만 가능)
TC-02: AXIS1=FISCAL_YEAR 선택 → AXIS3의 3개 옵션 모두 활성
TC-03: AXIS1=HIRE_DATE + AXIS3=PRORATE로 POST 호출 → 400 거부
TC-04: AXIS3 != PRORATE 일 때 4번 axis 비활성 + "조건부 활성" 배지 노출
TC-05: AXIS4=HALF_DAY 저장 → TB_LEAVE_USAGE_POLICY.ALLOW_HALF_DAY='Y' 강제 변경
TC-06: 5번 axis LEGAL 모드 → n=3, m=2 고정, max만 입력 가능
TC-07: 5번 axis CUSTOM 모드 + n=5로 POST 호출 → 400 거부 (법정 위반)
TC-08: 5번 axis CUSTOM 모드 + m=3로 POST 호출 → 400 거부 (법정 위반)
TC-09: AXIS5_MAX_DAYS=20으로 POST 호출 → 400 거부 (법정 25일 미만)
TC-10: 5번 axis 미리보기: n=1, m=1, max=25 입력 시 1년차 16일, 11년차 25일(최대) 표시
TC-11: 1번=회계연도, 3번=NEXT_YEAR_BULK, 2024.07.15 입사자 시뮬레이션:
       2026.02 잔여 = 월차 5일(2024.08~12) + 본연차 15일 = 20일
TC-12: 시간 단위 휴가 신청 시 휴게시간(예: 12:00~13:00) 가로지르는 시간대 선택 불가
```

### 5.12 TB_LEAVE_POLICY DDL 변경 사항 (요약)

기존 DDL에서 다음 컬럼들이 변경됨. 정식 DDL은 1.3.3 섹션 참조.

```sql
-- 변경: 9개 → 7개 axis로 컬럼 재구성

AXIS3_FIRST_YEAR_METHOD   VARCHAR(30) NOT NULL 
  COMMENT '3번: MONTHLY_ONLY/PRORATE/NEXT_YEAR_BULK';
  -- 기존: MONTHLY_ACCRUAL/PRORATE/PREGRANT/NEXT_YEAR_BULK/NONE

-- 컬럼 삭제: 구 4번 (1년 미만 월차) — 법정 의무로 정책 컬럼 불필요

-- 새 4번 axis (구 8번 → 위치 이동, 컬럼명 변경)
AXIS4_PRORATE_ROUNDING    VARCHAR(20) NOT NULL DEFAULT 'CEIL' 
  COMMENT '4번: CEIL/ROUND/FLOOR/HALF_DAY (AXIS3=PRORATE 시만 유효)';

-- 5번 axis 통합 (구 5+6번)
AXIS5_TENURE_MODE         VARCHAR(10) NOT NULL DEFAULT 'LEGAL' 
  COMMENT '5번 모드: LEGAL/CUSTOM';
AXIS5_START_YEAR          INT NOT NULL DEFAULT 3 
  COMMENT '5번 가산 시작 연차 (1~3)';
AXIS5_INTERVAL            INT NOT NULL DEFAULT 2 
  COMMENT '5번 가산 주기 (1~2)';
AXIS5_MAX_DAYS            INT NOT NULL DEFAULT 25 
  COMMENT '5번 최대 연차일수 (25 이상)';

-- 6번 (구 7번): 옵션 축소
AXIS6_VALIDITY_MONTHS     INT NOT NULL DEFAULT 12 
  COMMENT '6번: 유효기간(개월) 12 or 24';

-- 7번 (구 9번): 컬럼명 변경
AXIS7_USE_PROMOTION       CHAR(1) NOT NULL DEFAULT 'N' 
  COMMENT '7번: 사용촉진 사용여부 Y/N';

-- 삭제된 컬럼: 구 AXIS6_MAX_DAYS (AXIS5_MAX_DAYS로 통합)
```

### 5.13 TB_LEAVE_USAGE_POLICY DDL 변경 사항

```sql
-- 휴게시간 처리 컬럼 제거 (시스템 강제로 일률 처리)
-- 기존: BREAK_TIME_HANDLING VARCHAR(20) NOT NULL DEFAULT 'AUTO_EXCLUDE'
-- 제거됨. 모든 신청은 "휴게시간 가로지르는 신청 자체 불가"로 강제 처리.

-- 유지 컬럼
ALLOW_FULL_DAY        CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '1일 단위 (항상 Y)';
ALLOW_HALF_DAY        CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '0.5일 단위';
ALLOW_QUARTER_DAY     CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '0.25일 단위';
ALLOW_HOURLY          CHAR(1) NOT NULL DEFAULT 'N' COMMENT '0.125일(1시간) 단위';
MAX_DAILY_REQUEST     INT NOT NULL DEFAULT 3 COMMENT '같은 날 최대 신청 건수 (0=불허)';
```

---

## 6. 화면 5: 연차 현황 대시보드 (신규)

### 6.1 기능 설명

**이게 뭐다**: 회사 전체 직원의 연차 현황을 한눈에 보는 화면. v2에서 **경력 인정 표시, 체크박스 + 일괄 작업, 연차사용계획서 조회, 2단 헤더(법정/법정외)** 기능이 추가됨.

### 6.2 v2 신규 기능

#### 6.2.1 체크박스 + 일괄 작업
- 테이블 최좌측에 체크박스
- 1명 이상 선택 시 [일괄 수동 부여] / [연차사용계획서 조회] 버튼 활성화
- 헤더 체크박스로 전체 선택/해제

#### 6.2.2 일괄 수동 부여
- 선택된 직원들에게 동일한 수동 부여를 한번에 적용
- 화면 7(수동 부여 모달) 재사용, 다만 대상 직원이 N명

#### 6.2.3 연차사용계획서 조회
- 선택된 직원들의 연차사용계획서를 통합 조회하는 별도 화면 (또는 모달)

#### 6.2.4 경력 인정 표시
- 직원명 아래 "+ 경력 인정 60개월" 표시 (TB_USER_SERVICE_CREDIT 합산)

#### 6.2.5 2단 헤더 (법정 / 법정 외)

```
┌─────┬──────┬──────┬──────┬─────────────────┬─────────────────┬──────┬──────┐
│체크 │ 직원 │입사일│ 근속 │   법정 휴가     │   법정 휴가 외  │사용률│ 관리 │
│     │      │      │      ├────┬────┬────┼────┬────┬────┤      │      │
│     │      │      │      │부여│사용│잔여│부여│사용│잔여│      │      │
└─────┴──────┴──────┴──────┴────┴────┴────┴────┴────┴────┴──────┴──────┘
```

**분류 규칙**:
- 법정 휴가: `TB_LEAVE_GRANT.GRANT_TYPE LIKE 'STATUTORY_%'`
- 법정 휴가 외: `TB_LEAVE_GRANT.GRANT_TYPE LIKE 'MANUAL_%'`

### 6.3 API 명세

```
GET /api/leave-dashboard/list
    ?deptCd=&employmentType=&searchKeyword=&sortBy=&page=&size=
    Response: 직원별 법정/법정외 부여·사용·잔여 + 경력 인정 합산

POST /api/leave-grant/bulk-manual-grant
     Body: { userCds: [...], grantType, grantDays, availableFrom, reason }
     → 선택된 직원들에게 동일 수동 부여 일괄 적용

GET /api/leave-plan/bulk-view
    ?userCds=...
    → 선택된 직원들의 연차사용계획서 조회
```

### 6.4 핵심 쿼리 패턴

```sql
SELECT
  u.USER_CD, u.USER_NM, u.HIRE_DATE, u.EMPLOYMENT_TYPE,
  COALESCE(c.CREDIT_MONTHS_SUM, 0) AS CREDIT_MONTHS,
  COALESCE(SUM(CASE WHEN g.GRANT_TYPE LIKE 'STATUTORY_%' THEN g.GRANT_DAYS END), 0) AS LEGAL_GRANT,
  COALESCE(SUM(CASE WHEN g.GRANT_TYPE LIKE 'STATUTORY_%' THEN g.USED_DAYS END), 0)  AS LEGAL_USED,
  COALESCE(SUM(CASE WHEN g.GRANT_TYPE LIKE 'MANUAL_%' THEN g.GRANT_DAYS END), 0)    AS MANUAL_GRANT,
  COALESCE(SUM(CASE WHEN g.GRANT_TYPE LIKE 'MANUAL_%' THEN g.USED_DAYS END), 0)     AS MANUAL_USED
FROM TB_USER u
LEFT JOIN TB_LEAVE_GRANT g
  ON u.CMPNY_CD = g.CMPNY_CD AND u.USER_CD = g.USER_CD
  AND g.STATUS = 'ACTIVE'
LEFT JOIN (
  SELECT CMPNY_CD, USER_CD, SUM(CREDIT_MONTHS) AS CREDIT_MONTHS_SUM
  FROM TB_USER_SERVICE_CREDIT
  WHERE USE_YN = 'Y'
  GROUP BY CMPNY_CD, USER_CD
) c ON u.CMPNY_CD = c.CMPNY_CD AND u.USER_CD = c.USER_CD
WHERE u.CMPNY_CD = #{gvCmpnyCd}
GROUP BY u.USER_CD
```

### 6.5 테스트 케이스

```
TC-01: 직원 3명 체크 → "3명 선택됨" 표시 + 일괄 버튼 활성화
TC-02: 일괄 수동 부여 → 3명 모두에게 동일 GRANT 레코드 INSERT
TC-03: 법정 부여 19일 + 수동 부여 2일인 직원 → 법정 19/법정외 2 표시
TC-04: 경력 인정 60개월 직원 → "+ 경력 인정 60개월" 표시
TC-05: STATUTORY_* + MANUAL_* 모두 가진 직원의 합산 정확성 검증
```

---

## 7. 화면 6: 직원별 연차 상세 (신규)

### 7.1 기능 설명

**이게 뭐다**: 연차 현황 화면의 [>] 버튼 클릭 시 진입하는 직원 개인 상세. v2에서 **법정/법정외 통계 카드 분리, 부여 이력에 "구분" 컬럼 추가**.

### 7.2 v2 변경 사항

- 상단 통계 카드: 기존 1개 섹션(부여/사용/잔여) → **2개 섹션(법정 휴가 / 법정 휴가 외) 각 3개 카드**
- 부여 이력 테이블: 부여일 옆에 **"구분" 컬럼 추가** (법정/법정 외 배지)

### 7.3 API 명세

```
GET /api/leave-dashboard/{userCd}/detail
    Response: {
      user: { ... },
      legalSummary: { granted, used, remaining },
      nonLegalSummary: { granted, used, remaining },
      grantHistory: [
        { grantDate, type, reason, granted, used, remaining, expiresAt, status }
      ]
    }
```

### 7.4 데이터 정렬

- 부여 이력: GRANT_DATE 내림차순
- 상태 컬럼:
  - ACTIVE: 사용중
  - EXHAUSTED: 소진완료
  - EXPIRED: 만료
  - CANCELED: 취소됨 (입사일 변경 등)

### 7.5 테스트 케이스

```
TC-01: 법정 19일 + 수동 2일인 직원 → 상단에 법정 19/2/5, 법정외 2/2/0 표시
TC-02: 부여 이력에 STATUTORY/MANUAL 혼재 → 각각 "법정"/"법정 외" 배지 표시
TC-03: 만료된 부여 → 상태 컬럼 "만료" + 회색 배지
```

---

## 8. 화면 7: 수동 부여 모달 (신규)

### 8.1 기능 설명

**이게 뭐다**: 관리자가 직원에게 포상휴가/경조사휴가 등을 수동으로 부여하는 모달. v2에서 **유효기간 입력 제거, 부여일 → 사용 가능일로 명칭 변경**.

### 8.2 v2 변경 사항

| 항목 | v1 | v2 |
|------|-----|-----|
| 유효기간 입력 | 있음 | **제거** (정책의 AXIS7_VALIDITY_MONTHS 자동 적용) |
| 부여일 라벨 | "부여일" | **"사용 가능일"** |

### 8.3 화면 구성

```
1. 대상 직원 (단일 / 일괄 N명)
2. 부여 유형 (필수)
   - 포상휴가 (관리자 수동 부여) → MANUAL_BONUS
   - 경조사 휴가 → MANUAL_CONDOLENCE
   - 장기근속 휴가 → MANUAL_LONG_SERVICE
   - 기타 약정 휴가 → MANUAL_OTHER
3. 부여 일수 (0.5 단위, 필수)
4. 사용 가능일 (날짜, 필수)
5. 부여 사유 (자유 텍스트, 선택)
```

### 8.4 API 명세

```
POST /api/leave-grant/manual-grant         - 단일 부여
POST /api/leave-grant/bulk-manual-grant    - 다중 부여
     Body: {
       userCd(s),
       grantType: "MANUAL_BONUS" | ...,
       grantDays: 2,
       availableFrom: "20260519",
       reason: "..."
     }
```

### 8.5 트랜잭션 처리

```java
@Transactional
public void manualGrant(ManualGrantCommand cmd) {
  // 1. 활성 정책 조회 → AXIS7_VALIDITY_MONTHS 가져오기
  // 2. EXPIRES_AT = availableFrom + AXIS7_VALIDITY_MONTHS
  // 3. IDEMPOTENCY_KEY 생성: "{USER_CD}_{TIMESTAMP}_{GRANT_TYPE}"
  // 4. TB_LEAVE_GRANT INSERT
}
```

### 8.6 검증 규칙

- 부여 일수 > 0
- 부여 일수가 0.5 단위인지 확인
- 사용 가능일이 오늘 이전이면 경고 (즉시 사용 가능)
- 사용 가능일이 너무 미래(1년 이상)면 경고

### 8.7 테스트 케이스

```
TC-01: 포상휴가 2일 부여 + 사용가능일 오늘 → 즉시 잔여에 반영
TC-02: 미래 사용가능일 입력 → 부여는 되지만 가용 잔여에는 미반영
TC-03: 일괄 부여로 3명에게 1일씩 → 3건 GRANT INSERT, IDEMPOTENCY_KEY 각각 다름
TC-04: 정책 유효기간 12개월 → EXPIRES_AT = availableFrom + 12개월 자동 계산
```

---

## 9. 화면 8: 정책 변경 영향 분석 (신규) ⭐ 핵심

### 9.1 기능 설명

**이게 뭐다**: 연차 정책을 변경하기 전에, [연차 부여 정책] 화면에서 설정한 변경 사항이 영향받는 직원과 회사 추가 부담을 미리 분석하는 화면. **v2에서 변경할 정책 드롭다운 제거 (이전 화면에서 이미 설정), axis 변경 사항 diff 패널 추가, "주요 영향" 컬럼 데이터 추출 규칙 정의**.

### 9.2 화면 구성

```
1. 정책 요약 영역 (상단)
   - 현재 정책 → 변경할 정책 (둘 다 readonly, 이전 화면에서 받아옴)
   - 변경 적용일 (날짜 입력, 우측)
   - [분석 실행] 버튼 (우측)
2. 변경 사항 상세 영역 (토글 가능, 기본 펼침)
   - 7개 axis별 변경 사항 diff 카드 (2열 그리드)
   - 변경 있는 axis: 이전 값 → 새 값 (취소선 + 화살표 + 강조)
   - 비활성된 axis: "(조건부 비활성)" 또는 "(비활성)" 표시
   - 변경 없는 axis: "변경 없음 · 현재 값" (회색 처리)
3. 요약 카드 4개
   - 전체 직원
   - 정상 적용 (영향 없는 인원)
   - 주의 필요 (영향받는 인원)
   - 추가 부담 합계 (일수)
4. 영향받는 직원 테이블
   컬럼: 직원 / 입사일 / 기존 부여 / 기존 사용 / 예상 추가 / 주요 영향
5. 하단
   - [상세 리포트 다운로드]
   - [정책 변경 진행] → 정책 저장 (TB_LEAVE_POLICY UPDATE + HISTORY INSERT)
```

### 9.3 v2 제거 항목

- ~~변경할 정책 드롭다운~~ (이전 화면에서 이미 선택)
- ~~[⚙ 직접 설정] 진입 버튼~~ (이전 화면에서 이미 axis 설정 완료)
- ~~권장 변경 적용일 박스~~ (시스템이 자동 산출하기엔 로직 복잡)
- ~~위험도 컬럼~~ (추가 부담 일수로 충분히 판단 가능)

### 9.4 ⭐ 화면 진입 흐름 (v2 변경)

```
[연차 부여 정책 화면 (화면 3)]
  사용자가 새 정책 선택 (프리셋 또는 직접 설정)
  → [분석 실행] 버튼 클릭
       ↓ (axis 조합을 클라이언트 상태로 전달)
[정책 변경 영향 분석 화면 (화면 8)]
  - 현재 정책: TB_LEAVE_POLICY에서 USE_YN='Y' 조회
  - 변경할 정책: 이전 화면에서 전달받은 axis 조합
  - 변경 적용일만 사용자 입력
  - [분석 실행] → 영향 분석 API 호출
  - [정책 변경 진행] → 실제 저장 (TB_LEAVE_POLICY UPDATE + TB_LEAVE_POLICY_HISTORY INSERT)
```

**중요**: 변경할 정책은 절대 이 화면에서 수정하지 않는다. 수정이 필요하면 [뒤로가기]로 화면 3으로 돌아가서 다시 설정.

### 9.5 ⭐ 변경 사항 상세 (Diff 패널) 표시 규칙

#### 9.5.1 axis별 표시 유형

| 유형 | 조건 | 표시 |
|------|------|------|
| 변경 있음 | current.axisN != target.axisN | `이전 값 (취소선) → 새 값 (강조)` |
| 비활성됨 | 변경 후 axis가 의미 없어짐 | `이전 값 → (비활성)` 또는 `(조건부 비활성)` |
| 활성화됨 | 변경 후 axis가 새로 의미 생김 | `(비활성) → 새 값` |
| 변경 없음 | current.axisN == target.axisN | `변경 없음 · 현재 값` (회색) |

#### 9.5.2 비활성 판단 로직

```java
public String getAxisDiffLabel(Policy current, Policy target, int axisNum) {
    switch (axisNum) {
        case 2: // 회계연도 시작일
            boolean curActive = current.axis1 == FISCAL_YEAR;
            boolean tgtActive = target.axis1 == FISCAL_YEAR;
            if (!curActive && tgtActive) return "(비활성) → " + target.axis2;
            if (curActive && !tgtActive) return current.axis2 + " → (비활성)";
            break;
        case 4: // 비례 부여 시 반올림
            boolean curActive4 = current.axis3 == PRORATE;
            boolean tgtActive4 = target.axis3 == PRORATE;
            if (!curActive4 && tgtActive4) return "(조건부 비활성) → " + target.axis4;
            if (curActive4 && !tgtActive4) return current.axis4 + " → (조건부 비활성)";
            break;
        // ... 나머지 axis는 단순 변경 비교
    }
    // ...
}
```

#### 9.5.3 토글 동작

- 기본 상태: **펼침** (사용자가 변경 사항 즉시 확인 가능)
- 접고 펼치기 가능 (화살표 아이콘 회전)
- 변경 없는 axis가 많을 때 접어서 화면 정리 가능

### 9.6 ⭐ "주요 영향" 컬럼 데이터 추출 규칙 - 필수 구현

영향받는 직원별로 변경 전후 부여 차이를 분석하여 자동 생성한다.
**우선순위에 따라 한 가지 메시지만 표시**:

```java
public String determineMainImpact(Employee emp, Policy current, Policy target, LocalDate applyDate) {
    int monthsSinceHire = monthsBetween(emp.hireDate, applyDate);
    boolean isUnder1Year = monthsSinceHire < 12;
    boolean alreadyGrantedFiscal = hasFiscalGrant(emp, current);

    // 우선순위 1: 1년 미만 + 회계연도로 이미 부여받음
    if (isUnder1Year && alreadyGrantedFiscal
        && current.axis1 == FISCAL_YEAR && target.axis1 == HIRE_DATE) {
        int existingMonthly = countExistingMonthlyGrants(emp);
        int additionalMonthly = 11 - existingMonthly;
        return String.format("1년 미만 월차 %d일 + 1년차 15일 추가 발생", additionalMonthly);
    }

    // 우선순위 2: 변경 시점부터 12개월 내 1년 도래
    if (monthsSinceHire >= 11 && monthsSinceHire < 12) {
        return "1년차 도래 시 15일 추가 발생";
    }

    // 우선순위 3: 회계연도 → 입사일 변경 시 월차 누락분
    if (current.axis1 == FISCAL_YEAR && target.axis1 == HIRE_DATE) {
        int missingMonthly = calculateMissingMonthly(emp, current, target);
        if (missingMonthly > 0) {
            return String.format("나머지 월차 %d일 발생", missingMonthly);
        }
    }

    // 우선순위 4: 5번 axis (근속 가산 정책) 변경 영향
    if (!current.axis5.equals(target.axis5)) {
        int diff = calculateTenureBonusDiff(emp, current, target);
        if (diff != 0) {
            return String.format("근속 가산 정책 변경으로 %d일 차이", diff);
        }
    }

    // 우선순위 5: 기타
    return "기존 부여 유지";
}
```

### 9.7 API 명세

```
POST /api/leave-policy/analyze-impact
     Body: {
       applyDate: "20260601",
       targetPolicy: {
         axis1: "HIRE_DATE",
         axis2: null,
         axis3: "MONTHLY_ONLY",
         axis4: "CEIL",          // 조건부 비활성일 때 기본값
         axis5_mode: "LEGAL",
         axis5_startYear: 3,
         axis5_interval: 2,
         axis5_maxDays: 25,
         axis6: 12,
         axis7: "N"
       }
       // currentPolicy는 서버에서 TB_LEAVE_POLICY WHERE USE_YN='Y'로 직접 조회
     }
     Response: {
       summary: {
         totalEmployees, normalCount, affectedCount, additionalDaysTotal
       },
       diff: [
         { axisNum: 1, axisName: "연차 부여 기준", fromValue: "회계연도 기준", toValue: "입사일 기준", changeType: "CHANGED" },
         { axisNum: 2, axisName: "회계연도 시작일", fromValue: "1월 1일", toValue: null, changeType: "DEACTIVATED" },
         { axisNum: 5, axisName: "근속 가산 정책", fromValue: null, toValue: null, changeType: "UNCHANGED", note: "법정 기준 유지 (n=3, m=2, max=25)" },
         ...
       ],
       affectedEmployees: [
         {
           userCd, userNm, hireDate,
           currentGrant, currentUsed, expectedAdditional,
           mainImpact: "1년 미만 월차 11일 + 1년차 15일 추가 발생"
         }
       ]
     }

POST /api/leave-policy/apply-policy-change
     Body: { applyDate, targetPolicy, changeReason }
     → 실제 정책 변경 적용 (TB_LEAVE_POLICY UPDATE + TB_LEAVE_POLICY_HISTORY INSERT)
       + IMPACT_SUMMARY를 HISTORY에 함께 저장
```

### 9.8 핵심 비즈니스 로직

```
영향 분석 알고리즘:
1. 현재 정책 조회 (TB_LEAVE_POLICY WHERE CMPNY_CD = ? AND USE_YN = 'Y')
2. 활성 직원 전체 조회 (TB_USER WHERE CMPNY_CD = ? AND USE_YN = 'Y')
3. axis별 diff 계산 → diff 응답에 포함
4. 각 직원별로:
   a. 현재 정책 기준 1년치 부여량 시뮬레이션
   b. 변경 정책 기준 1년치 부여량 시뮬레이션
   c. 차이 계산 (예상 추가 = 변경 정책 - 현재 정책)
   d. 차이 > 0 이면 영향받는 직원
   e. 우선순위 규칙으로 mainImpact 결정
5. 요약 합계
```

### 9.9 분석 결과는 저장하지 않음

- 영향 분석(analyze-impact)은 **읽기 전용 시뮬레이션**
- 실제 정책 변경은 [정책 변경 진행] 클릭 시 apply-policy-change API 호출
- 정책 변경 시 TB_LEAVE_POLICY_HISTORY에 변경 스냅샷 + IMPACT_SUMMARY를 함께 저장

### 9.10 검증 규칙

```
1. 변경 적용일은 오늘 이후 (과거 날짜 거부)
2. 변경 적용일은 12개월 이내 (너무 먼 미래 거부, 경고 후 허용 가능)
3. 변경할 정책의 axis 매트릭스 검증 (화면 4의 5.10 검증 규칙 재사용)
4. 현재 정책과 변경할 정책이 동일하면 → 400 거부 ("변경 사항이 없습니다")
5. AXIS5 (근속 가산 정책) 변경 시 → 추가 검증 (법정 위반 방지)
```

### 9.11 테스트 케이스

```
TC-01: 화면 3에서 정책 변경 없이 [분석 실행] → 400 거부 ("변경 사항 없음")
TC-02: 화면 3 → 화면 8 진입 시 → 현재/변경 정책 readonly 자동 표시
TC-03: 변경 사항 상세 패널 → 변경 있는 axis는 강조, 없는 axis는 회색 표시
TC-04: 변경 사항 상세 패널 토글 → 접기/펼치기 정상 동작
TC-05: 현재 회계연도 비례 → 변경 입사일 기준 + 1년 미만 직원
       → mainImpact = "1년 미만 월차 N일 + 1년차 15일 추가 발생"
TC-06: 변경 시점에 입사 11개월 직원
       → mainImpact = "1년차 도래 시 15일 추가 발생"
TC-07: AXIS5 변경만 한 경우 + 가산 시점이 다른 직원
       → mainImpact = "근속 가산 정책 변경으로 N일 차이"
TC-08: AXIS1 변경 시 → AXIS2(회계연도 시작일) 비활성 표시 (한쪽이 HIRE_DATE인 경우)
TC-09: AXIS3 변경 시 → AXIS4(비례 부여 반올림) 조건부 비활성 표시
TC-10: 변경 적용일을 어제로 입력 → 검증 실패
TC-11: [정책 변경 진행] 클릭 → TB_LEAVE_POLICY UPDATE + HISTORY INSERT (IMPACT_SUMMARY 포함)
```

---

## 10. 법정 연차 자동 부여 배치 (Phase 1 핵심)

### 10.1 기능 설명

**이게 뭐다**: 매일 0시에 실행되어 각 회사 정책에 따라 법정 연차를 자동 부여하는 배치. 입사일 기준 / 회계연도 기준 / 월차 / 근속 가산 등을 모두 처리한다.

### 10.2 처리 시나리오

#### 시나리오 A: 입사일 기준 + MONTHLY_ACCRUAL
```
1. 매일 새벽: TB_USER 전체 스캔
2. 입사 N개월차(N<12) 만근자: STATUTORY_MONTHLY 1일 부여
   IDEMPOTENCY_KEY = "{USER_CD}_{YYYYMM}_MONTHLY"
3. 입사 1년 도래자: STATUTORY_ANNUAL 15일 부여 (+ 근속 가산)
   IDEMPOTENCY_KEY = "{USER_CD}_{YYYY}_ANNUAL"
```

#### 시나리오 B: 회계연도 기준 + PRORATE
```
1. 회계연도 시작일(예: 1.1) 도래 시
2. 입사 1년 미만 직원: 잔여기간 비례 부여
3. 입사 1년 이상 직원: 15일 일괄 부여 (+ 근속 가산)
4. 월차는 별도로 매월 입사일 기준 처리
```

#### 시나리오 C: 회계연도 기준 + NEXT_YEAR_BULK
```
1. 1년 미만 직원: 월차만 발생 (4번 axis 법정)
2. 차년도 회계연도 시작일: 15일 일괄 부여 (본연차)
3. 결과적으로 한 직원이 동시에 월차 5건 + 본연차 15일 보유 가능
```

### 10.3 근속 가산 계산

```
법적 근속 = 현재 - (HIRE_DATE - SUM(CREDIT_MONTHS))

AXIS5_TENURE_BONUS 별:
- BIYEARLY_FROM_3 (법정): 3년차+1, 5년차+2, 7년차+3, ..., 최대 +10
- YEARLY_FROM_1:           1년차+0, 2년차+1, 3년차+2, ...
- YEARLY_FROM_5:           5년차+1, 6년차+2, ...

최대 일수 = AXIS6_MAX_DAYS (기본 25)
```

### 10.4 멱등성 보장

- 모든 부여는 IDEMPOTENCY_KEY로 중복 방지
- 배치 재실행해도 동일 결과
- 실패 시 트랜잭션 롤백 + 알림

### 10.5 테스트 케이스

```
TC-01: 같은 배치 2회 실행 → 부여 건수 동일 (멱등)
TC-02: 입사 1년 도래자 + 근속 5년 → 15일 + 가산 2일 = 17일
TC-03: 경력 인정 60개월 + 입사 1년 → 법적 근속 6년 → 15일 + 가산 2일
TC-04: 회계연도 + NEXT_YEAR_BULK 정책 → 1년차 동안 월차 + 본연차 동시 보유
TC-05: 최대 일수 25 초과 → 25일로 제한
```

---

## 11. 작업 우선순위

### Phase 1 (MVP)
1. **DDL 적용** (섹션 1) — 가장 먼저
2. **운영 기초정보 휴가그룹타입 코드 추가** (섹션 1.2)
3. **화면 1: 사용자 정보 팝업 수정** (섹션 2)
4. **화면 2: 입사일 수정 모달** (섹션 3)
5. **화면 3: 연차 정책** (섹션 4)
6. **법정 연차 자동 부여 배치** (섹션 10)
7. **화면 5: 연차 현황 대시보드** (섹션 6)
8. **화면 6: 직원별 연차 상세** (섹션 7)
9. **화면 7: 수동 부여 모달** (섹션 8)

### Phase 2
1. **화면 4: 정책 직접 설정** (섹션 5)
2. **화면 8: 정책 변경 영향 분석** (섹션 9)
3. 연차 사용촉진 제도 자동화
4. 연차사용계획서 조회

### Phase 3 이후
1. 계약 만료 임박 알림
2. 무기계약직 전환 알림
3. Excel export 보강

---

## 12. 핵심 비즈니스 규칙 요약 (절대 어기지 말 것)

1. **모든 쿼리에 CMPNY_CD 스코프** (멀티테넌시)
2. **시스템은 절대 기 발생 연차를 자동 차감하지 않음** (정책 변경에도 보호)
3. **1년 미만 월차는 법정 의무로 정책 무관 자동 부여** (3번 axis와 별개)
4. **3번 axis는 1번 axis에 의해 활성/비활성** (매트릭스 검증 필수)
5. **AXIS8=HALF_DAY → ALLOW_HALF_DAY 강제 'Y'**
6. **법정 연차 자동 부여는 IDEMPOTENCY_KEY로 중복 방지** (멱등 배치)
7. **입사일 변경은 최고관리자만, 영향 분석 후 처리**
8. **경력 인정은 모두 점진 부여** (소급은 별도 수동 부여)
9. **법정/법정외 구분은 GRANT_TYPE prefix로 판단** (STATUTORY_* vs MANUAL_*)
10. **정책 변경 시 TB_LEAVE_POLICY_HISTORY에 스냅샷 INSERT 필수**
