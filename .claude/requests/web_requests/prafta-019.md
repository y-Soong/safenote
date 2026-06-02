# 시간차 연차 + 결재 흐름 작업 요청서

> **목적**: 법정 연차 + 약정 휴가에 시간차 사용(2시간/1시간/30분) 도입, 사후 신청 시 결재 흐름(NODE 기반 상향 순차) 구현
>
> **대상 모듈**: 근태관리(attd06 등) / 연차 관리
>
> **DB**: MySQL 8.0.42, schema=`prafta`

---

## 1. 핵심 비즈니스 개요

### 1.1 연차 사용 단위 (6종)

| 코드 | 단위 | 사용자 입력 | 차감 계산 |
|---|---|---|---|
| 00 | 1일 (FULL) | 날짜만 | -1.000 |
| 01 | 반차 (HALF) | 일반 1구간 스케줄: 오전/오후 선택 / 1구간+2구간 또는 교대근무: 시작 시각 입력 | -0.500 |
| 02 | 시간차 2시간 (HOUR_2) | 날짜 + 시작 시각 (배수 길이) | -(신청분 ÷ 1일 소정근로분), `decimal(8,5)` |
| 03 | 시간차 1시간 (HOUR_1) | 날짜 + 시작 시각 (배수 길이) | -(신청분 ÷ 1일 소정근로분), `decimal(8,5)` |
| 04 | 시간차 30분 (MIN_30) | 날짜 + 시작 시각 (배수 길이) | -(신청분 ÷ 1일 소정근로분), `decimal(8,5)` |

- **코드값은 SYS025에 시스템 시드로 강제 등록** (회사가 임의 변경 불가)
- 시간차는 **선택한 단위의 배수**로 사용 (예: 30분 단위 선택 시 30분/60분/90분/120분 신청 가능, 1건으로 저장)
- **1일 소정근로분 = `tb_sch_mgmt`의 (1구간 + 2구간) 시간 - 1구간/2구간 휴게시간**

### 1.2 사용 가능 조건

- **FULL (1일)**: 무조건 가능, 스케줄 등록 여부 무관
- **HALF / HOUR_* / MIN_30**: 해당 일자에 `tb_user_work_plan` 등록 필수
- **휴게시간 겹침 정책**:
  - 시간차: 휴게시간을 가로지르는 신청 → **거부**
  - 반차: 휴게시간 가로질러도 OK (반차는 차감 0.5 고정이므로)
- **신청 분 > 그 일자 1일 소정근로분**: 거부 (안전장치)
- **잔여 < 신청 차감량**: 더 작은 단위 안내 (가능한 단위 없으면 잔여 부족 안내)

### 1.3 사후 신청 + 결재 흐름

- **사전 신청 (시작 시각 도래 전)**: 결재 없이 즉시 승인
- **사후 신청 (시작 시각 이후)**: NODE 기반 상향 순차 결재 흐름
- 사후 신청 가능 기간: 회사별 설정 (TB_BAIM_VAL_M/D 신규 코드 활용)

### 1.4 결재선 구성 로직

신청자 NODE부터 시작해서 상위로 추적:

```
신청자 NODE
  ├─ SELF_ATTD_APPRV_YN = 'Y' 이고 관리자 존재 → 결재선에 포함
  ├─ SELF_ATTD_APPRV_YN = 'N' → PARENT_NODE_CD로 상위 추적
  └─ 관리자 없는 NODE → PASS (다음 NODE로)
```

**결재선의 결재자 우선순위 (같은 NODE 내)**:
- 일반 근로자 신청: MAIN/SUB 누구든 결재 가능
- SUB ADMIN 본인 신청: 같은 NODE의 MAIN_ADMIN이 결재, MAIN_ADMIN 없으면 다음 NODE
- MAIN ADMIN 본인 신청: 그 Step 자동 승인

**결재선 단계 진행**:
- 신청 시점에 결재선 전체 Step row 일괄 생성
- Step 1: 상태 '01 신청', 다음 Step: '00 대기중'
- Step 1 승인 → Step 2 '01 신청'으로 전환 + 알림
- 중간 단계 반려 시 → REQ_STATUS = '03 반려', 후속 Step은 '00 대기중' 그대로 종결 (REQ_STATUS로 판단)
- 결재선이 비는 경우 (모든 NODE 관리자 없음) → 자동 승인 + Step 1 로그 1건

**자동 승인 케이스**:
- 신청자 = 해당 Step NODE의 MAIN_ADMIN인 경우
- APPROVED_BY = 신청자 본인, APPROVAL_COMMENT = "자동승인:본인=MAIN_ADMIN"
- 자동 승인 단계도 Step 번호 차지 (감사 추적)

**무결성 보장 (서비스 레이어)**:
- 결재 진행 중인 NODE의 MAIN/SUB 관리자 변경 차단
  - 검증: 변경 대상 NODE_CD가 `tb_user_attd_req_approval`에 포함되고, 연관 `tb_user_attd_req.REQ_STATUS = '01 신청'`인 row 존재 여부
- NODE 삭제: 해당 NODE에 사용자(관리자 포함) 0명일 때만 가능
- NODE 관리자: MAIN과 SUB 동일인 차단

### 1.5 사후 정정

- 결재 완료 후 별도 취소 절차 없음
- 잘못된 결재는 **HR 담당자가 연차 관리 메뉴에서 직접 처리** (별도 화면)
- `tb_user_leave_use.LEAVE_STATUS = 'CANCELLED'` 처리 등

---

## 2. DDL 변경 사항

### 2.1 컬럼 타입 변경

```sql
-- 연차 사용/부여 정밀도 상향
ALTER TABLE tb_user_leave_use
  MODIFY COLUMN LEAVE_DAYS decimal(8,5) NOT NULL COMMENT '사용 일수 (시간차 환산 시 정밀도 필요)';

ALTER TABLE tb_user_leave_grant
  MODIFY COLUMN GRANT_DAYS decimal(5,2) NOT NULL COMMENT '부여 일수 (0.01일 단위 충분)',
  MODIFY COLUMN USED_DAYS decimal(8,5) NOT NULL DEFAULT 0.00000 COMMENT '사용 일수 캐시 (LEAVE_DAYS 합계)';
```

### 2.2 휴게 시각 컬럼 추가

```sql
-- 스케줄 관리
ALTER TABLE tb_sch_mgmt
  ADD COLUMN FST_BRK_STR_TIME varchar(4) NULL COMMENT '1구간 휴게 시작 시각 (HHMM)' AFTER FST_SCH_BRK_MIN,
  ADD COLUMN FST_BRK_END_TIME varchar(4) NULL COMMENT '1구간 휴게 종료 시각 (HHMM)' AFTER FST_BRK_STR_TIME,
  ADD COLUMN SEC_BRK_STR_TIME varchar(4) NULL COMMENT '2구간 휴게 시작 시각 (HHMM)' AFTER SEC_SCH_BRK_MIN,
  ADD COLUMN SEC_BRK_END_TIME varchar(4) NULL COMMENT '2구간 휴게 종료 시각 (HHMM)' AFTER SEC_BRK_STR_TIME;

-- 스케줄 이력
ALTER TABLE tb_sch_mgmt_hist
  ADD COLUMN FST_BRK_STR_TIME varchar(4) NULL COMMENT '1구간 휴게 시작 시각 (HHMM)' AFTER FST_SCH_BRK_MIN,
  ADD COLUMN FST_BRK_END_TIME varchar(4) NULL COMMENT '1구간 휴게 종료 시각 (HHMM)' AFTER FST_BRK_STR_TIME,
  ADD COLUMN SEC_BRK_STR_TIME varchar(4) NULL COMMENT '2구간 휴게 시작 시각 (HHMM)' AFTER SEC_SCH_BRK_MIN,
  ADD COLUMN SEC_BRK_END_TIME varchar(4) NULL COMMENT '2구간 휴게 종료 시각 (HHMM)' AFTER SEC_BRK_STR_TIME;
```

### 2.3 신규 테이블: `tb_user_attd_req_approval`

```sql
CREATE TABLE `tb_user_attd_req_approval` (
  `APPROVAL_ID` varchar(20) NOT NULL COMMENT '결재 ID (PK)',
  `REQ_ID` varchar(20) NOT NULL COMMENT '연관 요청 ID (tb_user_attd_req.REQ_ID)',
  `CMPNY_CD` varchar(50) NOT NULL COMMENT '회사 코드',
  `APPROVAL_STEP` int NOT NULL COMMENT '결재 단계 (1부터 시작, 직속 NODE → 상위)',
  `NODE_CD` varchar(50) NOT NULL COMMENT '결재 단계의 NODE 코드 (tb_site_node.NODE_CD)',
  `APPROVED_BY` varchar(20) DEFAULT NULL COMMENT '실제 결재한 사용자 코드 (자동 승인 시 신청자)',
  `APPROVAL_STATUS` varchar(10) NOT NULL COMMENT '결재 상태 [SYS043] 00:대기중/01:신청/02:승인/03:반려',
  `APPROVAL_COMMENT` varchar(500) DEFAULT NULL COMMENT '결재 코멘트 (자동 승인 시 시스템 메시지)',
  `APPROVAL_DATE` datetime DEFAULT NULL COMMENT '결재 처리 일시',
  `INSERT_NO` varchar(50) NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`APPROVAL_ID`),
  UNIQUE KEY `UX_REQ_STEP` (`REQ_ID`, `APPROVAL_STEP`),
  KEY `IX_REQ_APPROVAL_REQ` (`REQ_ID`),
  KEY `IX_REQ_APPROVAL_NODE` (`CMPNY_CD`, `NODE_CD`, `APPROVAL_STATUS`),
  KEY `IX_REQ_APPROVAL_APPROVER` (`APPROVED_BY`, `APPROVAL_STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
  COMMENT='근태 요청 결재선 추적';
```

### 2.4 컬럼 삭제 (기존 `tb_leave_usage_policy`)

```sql
-- ALLOW_* 4개 컬럼 폐기 (회사별 사용 단위 제어는 TB_BAIM_VAL로 이관)
ALTER TABLE tb_leave_usage_policy
  DROP COLUMN ALLOW_FULL_DAY,
  DROP COLUMN ALLOW_HALF_DAY,
  DROP COLUMN ALLOW_QUARTER_DAY,
  DROP COLUMN ALLOW_HOURLY;
```

> ⚠️ 만약 `tb_leave_usage_policy`에 다른 컬럼들도 의미가 변경되어야 하는 경우 별도 검토 필요. 위 4개만 명시적으로 폐기.

---

## 3. 마스터 데이터 작업

### 3.1 SYS025 (연차 사용 단위) 시스템 시드 등록

```sql
-- tb_syst_val_m에 SYS025 등록 (없는 경우)
INSERT INTO tb_syst_val_m (SYST_VAL_CD, SYST_VAL_NM, USE_YN, VAL_DESC)
VALUES ('SYS025', '연차 사용 단위', 'Y', '시스템 시드. 회사는 사용 가능 단위만 TB_BAIM_VAL로 제어')
ON DUPLICATE KEY UPDATE SYST_VAL_NM = VALUES(SYST_VAL_NM);

-- tb_syst_val_d에 5개 코드 등록
INSERT INTO tb_syst_val_d (SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN, VAL_D_DESC) VALUES
('SYS025', '00', '1일',          1, 'Y', '1일 전체 (FULL)'),
('SYS025', '01', '반차',         2, 'Y', '0.5일 (HALF)'),
('SYS025', '02', '시간차 2시간', 3, 'Y', '120분 단위 (HOUR_2)'),
('SYS025', '03', '시간차 1시간', 4, 'Y', '60분 단위 (HOUR_1)'),
('SYS025', '04', '시간차 30분',  5, 'Y', '30분 단위 (MIN_30)')
ON DUPLICATE KEY UPDATE SYST_VAL_D_NM = VALUES(SYST_VAL_D_NM);
```

### 3.2 SYS043 (결재 단계 상태) 신규 등록

```sql
INSERT INTO tb_syst_val_m (SYST_VAL_CD, SYST_VAL_NM, USE_YN, VAL_DESC)
VALUES ('SYS043', '결재 단계 상태', 'Y', 'tb_user_attd_req_approval.APPROVAL_STATUS 코드');

INSERT INTO tb_syst_val_d (SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, SORT_IDX, USE_YN, VAL_D_DESC) VALUES
('SYS043', '00', '대기중', 1, 'Y', '이전 단계 처리 중, 아직 본인 차례 아님'),
('SYS043', '01', '신청',   2, 'Y', '본인 차례, 알림 받음, 처리 대기'),
('SYS043', '02', '승인',   3, 'Y', '결재 승인 완료'),
('SYS043', '03', '반려',   4, 'Y', '결재 반려');
```

### 3.3 TB_BAIM_VAL 신규 코드 (회사별 정책)

```sql
-- 운영사 변수: 연차 정책
INSERT INTO tb_baim_val_m (CMPNY_CD, BAIM_VAL_CD, BAIM_VAL_NM, USE_YN, VAL_DESC) VALUES
('${CMPNY_CD}', 'BAIM_LEAVE_BACKDATE_LIMIT', '사후 연차 신청 허용 일수', 'Y', '0=당일만, N=N일 이내 사후 신청 가능'),
('${CMPNY_CD}', 'BAIM_LEAVE_USE_UNIT', '회사 허용 연차 사용 단위', 'Y', 'SYS025 코드 중 어떤 것을 회사에서 허용할지 다중 선택');

-- 사후 신청 허용 일수 (예: 7일)
INSERT INTO tb_baim_val_d (CMPNY_CD, BAIM_VAL_CD, BAIM_VAL_D_CD, BAIM_VAL_D_NM, SORT_IDX, USE_YN, VAL_D_INFO_1, VAL_D_DESC) VALUES
('${CMPNY_CD}', 'BAIM_LEAVE_BACKDATE_LIMIT', 'DEFAULT', '기본 사후 신청 허용 일수', 1, 'Y', '7', '값은 VAL_D_INFO_1에 일수로 저장');

-- 회사 허용 사용 단위 (회사가 ON/OFF로 관리)
INSERT INTO tb_baim_val_d (CMPNY_CD, BAIM_VAL_CD, BAIM_VAL_D_CD, BAIM_VAL_D_NM, SORT_IDX, USE_YN, VAL_D_INFO_1, VAL_D_DESC) VALUES
('${CMPNY_CD}', 'BAIM_LEAVE_USE_UNIT', '00', '1일',          1, 'Y', 'SYS025-00', '1일은 항상 Y'),
('${CMPNY_CD}', 'BAIM_LEAVE_USE_UNIT', '01', '반차',         2, 'Y', 'SYS025-01', ''),
('${CMPNY_CD}', 'BAIM_LEAVE_USE_UNIT', '02', '시간차 2시간', 3, 'N', 'SYS025-02', '회사가 ON 시 사용 가능'),
('${CMPNY_CD}', 'BAIM_LEAVE_USE_UNIT', '03', '시간차 1시간', 4, 'N', 'SYS025-03', ''),
('${CMPNY_CD}', 'BAIM_LEAVE_USE_UNIT', '04', '시간차 30분',  5, 'N', 'SYS025-04', '');
```

> 💡 BAIM_VAL_M/D의 정확한 코드 컨벤션은 기존 PRAFTA 운영사 변수 체계를 따라 조정 가능. 위는 제안값.

---

## 4. 테이블 관계도

### 4.1 연차 사용 관련

```
tb_user (1) ─────────────────────────────┐
   │                                       │
   ▼                                       ▼
tb_user_work_plan (N) ──────── tb_sch_mgmt (1)
   │ (WORK_PLAN_CD = SCH_CD)        ↑
   │                                 │ 휴게시간 4개 컬럼 추가
   ▼                                 │
tb_user_attd_req (1) ────────────────┘
   │                                
   ├─── tb_user_attd_req_approval (N) [신규]
   │      └─ NODE_CD ──────────► tb_site_node (1)
   │                                 │
   │                                 └─ MAIN_ADMIN_CD, SUB_ADMIN_CD → tb_user
   │
   ▼
tb_user_leave_use (N) ────► tb_user_leave_grant (1)
   │ (GRANT_ID FK)                    ▲
   │                                   │ (POLICY_SEQ FK)
   │                                   │
   │                              tb_leave_policy (1)
   │                                   │
   │                                   ▼
   └────────────────────────► tb_leave_type_mgmt (LEAVE_CD)
```

### 4.2 결재 흐름 (NODE 계층)

```
A (최상위)
└── B (SELF_ATTD_APPRV_YN='Y', 관리자 NULL → PASS)
    └── C (SELF_ATTD_APPRV_YN='Y', MAIN/SUB 존재)
        └── D (SELF_ATTD_APPRV_YN='Y', MAIN/SUB 존재) ← 신청자

[D 사용자 사후 신청 시 결재선]
Step 1: NODE=D, 신청자 본인 NODE → D의 MAIN/SUB가 결재
Step 2: NODE=C, 상위 NODE → C의 MAIN/SUB가 결재
Step 3: NODE=A, 최상위 NODE → A의 MAIN/SUB가 결재
       (B는 관리자 없어서 PASS, Step 자체 생성 안 됨)

[신청자가 SUB ADMIN인 경우]
Step 1: NODE=D, D의 MAIN_ADMIN이 결재 (SUB는 본인이라 제외)
       MAIN_ADMIN이 NULL이면 → Step 1 자체가 다음 NODE로 이동

[신청자가 MAIN ADMIN인 경우]
Step 1: 자동 승인 (APPROVED_BY = 신청자, COMMENT = "자동승인:본인=MAIN_ADMIN")
```

### 4.3 마스터 데이터 참조

```
tb_user_leave_use.USE_UNIT_TYPE ─► tb_syst_val_d (SYS025: 00~04)
tb_user_attd_req_approval.APPROVAL_STATUS ─► tb_syst_val_d (SYS043: 00~03)
tb_user_attd_req.REQ_STATUS ─► tb_syst_val_d (SYS033: 01~04)
tb_user_attd_req.REQ_TYPE ─► tb_syst_val_d (SYS032: 01~06)

회사별 연차 정책:
tb_baim_val_d (BAIM_LEAVE_BACKDATE_LIMIT) ── 사후 신청 일수
tb_baim_val_d (BAIM_LEAVE_USE_UNIT) ── 허용 단위 (USE_YN으로 ON/OFF)
```

---

## 5. 서비스 레이어 비즈니스 로직

### 5.1 1일 소정근로분 계산 함수

```java
/**
 * 해당 일자의 사용자 스케줄 기반 1일 소정근로분(분 단위) 조회
 * 
 * @param cmpnyCd 회사 코드
 * @param siteCd 사업장 코드
 * @param userCd 사용자 코드
 * @param workYmd 근무일 (YYYYMMDD)
 * @return 1일 소정근로분 (분 단위, int). 스케줄 없으면 null
 */
public Integer getDailyStdWorkMinutes(String cmpnyCd, String siteCd, String userCd, String workYmd) {
    // 1. tb_user_work_plan에서 WORK_PLAN_CD 조회
    // 2. WORK_PLAN_CD = SCH_CD로 tb_sch_mgmt 조회
    // 3. 계산:
    //    실근무분 = (FST_SCH_END - FST_SCH_STR) - FST_SCH_BRK_MIN
    //           + (SEC_SCH_END - SEC_SCH_STR) - SEC_SCH_BRK_MIN  [2구간 있을 때만]
    // 4. 반환
}
```

### 5.2 시간차 신청 검증 로직

```
시간차 신청 시 검증 순서:
1. 해당 일자에 tb_user_work_plan 존재 여부 (없으면 거부)
2. 1일 소정근로분 계산 (5.1 함수)
3. 회사 허용 단위 검증 (TB_BAIM_VAL 'BAIM_LEAVE_USE_UNIT' 해당 코드 USE_YN='Y')
4. 신청 시각 + 길이가 스케줄 범위 내인지 검증
5. 휴게시간 가로지름 검증:
   - 시작 ~ 종료 구간이 (FST_BRK_STR_TIME ~ FST_BRK_END_TIME)와 겹치지 않을 것
   - 시작 ~ 종료 구간이 (SEC_BRK_STR_TIME ~ SEC_BRK_END_TIME)와 겹치지 않을 것
6. 신청 분 ≤ 1일 소정근로분 검증
7. 같은 날 기존 신청과 시간 겹침 검증 (동일 회사 정책 MAX_DAILY_REQUEST 카운트 확인)
8. 차감 일수 계산: 신청분 ÷ 1일 소정근로분 (decimal(8,5) 반올림)
9. 잔여 검증:
   - tb_user_leave_grant 잔여 합계 = SUM(GRANT_DAYS - USED_DAYS) WHERE STATUS='ACTIVE'
   - 잔여 ≥ 차감 일수면 OK
   - 부족하면 더 작은 단위(허용된 것 중)로 안내 메시지
10. (사후 신청인 경우) 결재선 구성 후 tb_user_attd_req_approval 일괄 생성
11. (사전 신청인 경우) 즉시 승인 처리
```

### 5.3 반차 신청 처리 로직

```
반차 신청 시:
1. 해당 일자에 tb_user_work_plan 존재 여부 (없으면 거부)
2. 스케줄 조회: 1구간만 있는지, 2구간도 있는지 확인
3. 분기:
   case 1구간만:
     - UI: "오전/오후" 선택만
     - 오전: 1구간 시작부터 (1일 소정근로분/2) 만큼
     - 오후: 1구간 종료부터 거꾸로 (1일 소정근로분/2) 만큼
     - 휴게 가로지름 OK (반차는 별도 정책)
   case 1구간+2구간:
     - UI: 사용자가 패턴 선택 + 시작 시각 입력
     - 패턴 1 (합산 균등 분할): 총 근무분/2를 1구간 앞 또는 2구간 뒤에서 적용
     - 패턴 2 (구간별 분할): 1구간/2 또는 2구간/2를 각 구간 내에서 적용
   case 교대근무:
     - UI: 사용자가 시작 시각 직접 입력
     - 길이 = 1일 소정근로분/2 자동 계산
4. tb_user_leave_use 저장:
   USE_UNIT_TYPE = '01', LEAVE_DAYS = 0.5, LEAVE_MINUTES = (1일 소정근로분/2)
```

### 5.4 결재선 구성 로직

```java
/**
 * 신청자 NODE부터 상향으로 결재선 구성
 * 
 * @return List<NODE_CD> 결재선 순서대로
 */
public List<String> buildApprovalChain(String cmpnyCd, String siteCd, String applicantNodeCd, String applicantUserCd) {
    List<String> chain = new ArrayList<>();
    Set<String> visited = new HashSet<>();  // 무한 루프 방지
    String currentNode = applicantNodeCd;
    int depth = 0;
    final int MAX_DEPTH = 10;  // 안전장치

    while (currentNode != null && depth < MAX_DEPTH) {
        if (visited.contains(currentNode)) break;  // 순환 참조 방지
        visited.add(currentNode);

        SiteNode node = siteNodeMapper.selectNode(cmpnyCd, siteCd, currentNode);
        if (node == null) break;

        if ("Y".equals(node.getSelfAttdApprvYn())) {
            // 결재 가능 여부 확인
            boolean hasApprover = hasValidApprover(node, applicantUserCd);
            if (hasApprover) {
                chain.add(currentNode);
            }
            // 관리자 없거나 신청자 본인뿐이면 PASS (chain에 안 추가)
        }

        currentNode = node.getParentNodeCd();
        depth++;
    }

    // 최상위 NODE는 무조건 결재선에 포함 (관리자 있다면)
    // (위 로직에서 자연스럽게 처리됨)

    return chain;
}

private boolean hasValidApprover(SiteNode node, String applicantUserCd) {
    // 신청자가 SUB ADMIN인 경우: MAIN_ADMIN_CD가 존재해야 함
    // 신청자가 일반 근로자인 경우: MAIN 또는 SUB 둘 중 하나라도 존재하면 OK
    // 신청자가 MAIN ADMIN인 경우: 자동 승인이므로 결재선에는 포함하되 자동 승인 처리
    
    if (applicantUserCd.equals(node.getSubAdminCd())) {
        // 신청자가 SUB
        return node.getMainAdminCd() != null;
    }
    return node.getMainAdminCd() != null || node.getSubAdminCd() != null;
}
```

### 5.5 결재선 row 일괄 생성

```
사후 신청 요청 수신 시:
1. buildApprovalChain() 호출 → [D, C, A]
2. 결재선이 비어있으면:
   - 자동 승인 + Step 1 로그 1건 생성 (APPROVED_BY=신청자, COMMENT='자동승인:결재선없음')
   - tb_user_attd_req.REQ_STATUS = '02 승인'
3. 결재선이 있으면 트랜잭션으로:
   FOR each NODE in chain (idx=1, 2, 3...):
     IF idx == 1:
       자동 승인 케이스 체크:
         - 신청자가 해당 NODE의 MAIN_ADMIN이면 자동 승인
         - APPROVAL_STATUS = '02', APPROVED_BY = 신청자, COMMENT = '자동승인:본인=MAIN_ADMIN'
         - 다음 Step으로 진행 ('01 신청' 부여)
       자동 승인 아니면:
         - APPROVAL_STATUS = '01 신청', 알림 발송
     ELSE:
       자동 승인 케이스 체크 (위와 동일)
       자동 승인 아니면:
         - APPROVAL_STATUS = '00 대기중', 알림 보류
   END FOR
4. 첫 번째 '01 신청' 상태인 Step의 결재자에게 알림 발송
5. 모든 Step이 자동 승인되면 tb_user_attd_req.REQ_STATUS = '02 승인' 즉시 처리
```

### 5.6 결재 처리 흐름

```
Step N의 결재자가 승인 시:
1. tb_user_attd_req_approval Step N:
   - APPROVAL_STATUS = '02 승인'
   - APPROVED_BY = 처리자
   - APPROVAL_DATE = NOW()
   - APPROVAL_COMMENT = 입력값
2. Step N+1 존재 확인:
   - 존재: Step N+1 자동 승인 케이스 체크
     - 자동 승인이면: Step N+1을 '02 승인' 처리하고 다시 Step N+2 확인 (재귀)
     - 자동 승인 아니면: Step N+1을 '01 신청'으로 전환 + 알림
   - 없음: tb_user_attd_req.REQ_STATUS = '02 승인', 신청자 알림

Step N의 결재자가 반려 시:
1. tb_user_attd_req_approval Step N:
   - APPROVAL_STATUS = '03 반려'
   - APPROVED_BY = 처리자, APPROVAL_DATE = NOW(), APPROVAL_COMMENT = 사유
2. Step N+1 이후는 '00 대기중' 그대로 종결
3. tb_user_attd_req.REQ_STATUS = '03 반려', 신청자 알림
```

### 5.7 무결성 검증 (서비스 레이어)

```
NODE 관리자 변경 시 (tb_site_node UPDATE):
1. 변경 대상 NODE_CD에 대해 진행중 결재 존재 검증:
   SELECT COUNT(*) FROM tb_user_attd_req_approval a
   JOIN tb_user_attd_req r ON a.REQ_ID = r.REQ_ID
   WHERE a.CMPNY_CD = ? AND a.NODE_CD = ?
     AND r.REQ_STATUS = '01'  -- 진행중인 요청만
     AND r.DEL_YN = 'N'
2. > 0이면: 변경 차단 + "진행 중인 결재가 있어 관리자 변경 불가" 안내
3. = 0이면: 변경 허용

NODE 삭제 시 (tb_site_node DELETE):
1. 해당 NODE에 소속된 사용자(관리자 포함) 0명 검증:
   SELECT COUNT(*) FROM tb_user WHERE CMPNY_CD=? AND NODE_CD=? AND USE_YN='Y'
2. > 0이면: 삭제 차단 + "소속 사용자가 있어 삭제 불가" 안내
3. = 0이면: 삭제 허용

NODE 관리자 등록/변경 시:
1. MAIN_ADMIN_CD = SUB_ADMIN_CD 같은 사용자 지정 차단 (둘 다 NOT NULL인 경우)
```

---

## 6. API 엔드포인트 (예시)

> URL 컨벤션: kebab-case, `update-{domain}-{target}` 구조

| 메서드 | URL | 기능 |
|---|---|---|
| POST | `/api/leave/save-leave-request` | 연차 신청 (사전/사후) |
| POST | `/api/leave/cancel-leave-request` | 연차 신청 취소 (시작 시각 이전만) |
| GET | `/api/leave/select-leave-balance` | 잔여 연차 조회 (사용자 기준) |
| POST | `/api/leave/save-approval-decision` | 결재 처리 (승인/반려) |
| GET | `/api/leave/select-approval-list` | 결재 대기 목록 (관리자) |
| GET | `/api/leave/select-leave-list` | 사용자 연차 사용 이력 |

### 요청 DTO 예시 (사용자 시간차 신청)

```java
// LeaveRequestRequest
{
  "useDate": "20260530",
  "leaveCd": "ANNUAL_2026",  // 사용할 연차 코드 (tb_leave_type_mgmt or 법정연차)
  "useUnitType": "03",       // SYS025: 03=시간차 1시간
  "startTime": "1300",       // HHMM (시간차 단위인 경우만)
  "lengthMinutes": 60,       // 신청 길이(분) - 단위 배수
  "leaveReason": "병원 진료",
  "halfPattern": null        // 1구간+2구간 반차 시: '1' or '2', 그 외 null
}
```

### 응답 DTO 예시

```java
// LeaveRequestResponse
{
  "reqId": "REQ20260522001",
  "reqStatus": "01",          // SYS033: 01 신청 (사후) or 02 즉시 승인 (사전)
  "leaveDays": 0.12500,        // 차감 일수
  "leaveMinutes": 60,
  "remainingDays": 14.87500,   // 신청 후 잔여
  "approvalChain": [           // 사후 신청 시만
    { "step": 1, "nodeCd": "NODE_D", "status": "01", "approverNm": "홍길동(MAIN)" },
    { "step": 2, "nodeCd": "NODE_A", "status": "00" }
  ]
}
```

---

## 7. 작업 체크리스트

### Phase 1: DDL + 마스터 데이터
- [ ] `tb_user_leave_use.LEAVE_DAYS` → `decimal(8,5)` 변경
- [ ] `tb_user_leave_grant.GRANT_DAYS` → `decimal(5,2)`, `USED_DAYS` → `decimal(8,5)` 변경
- [ ] `tb_sch_mgmt`, `tb_sch_mgmt_hist`에 휴게 시각 4개 컬럼 추가
- [ ] `tb_user_attd_req_approval` 테이블 신규 생성
- [ ] `tb_leave_usage_policy` ALLOW_* 4개 컬럼 삭제
- [ ] SYS025 시스템 시드 5개 등록
- [ ] SYS043 신규 코드 그룹 + 4개 등록
- [ ] TB_BAIM_VAL 신규 코드 등록 (회사별 사후 신청 일수, 사용 단위 허용)

### Phase 2: 서비스 레이어 (Java)
- [ ] `getDailyStdWorkMinutes()` 유틸 함수 구현
- [ ] 휴게 가로지름 검증 함수
- [ ] 잔여 연차 조회 함수
- [ ] 시간차 신청 검증 + 차감 로직
- [ ] 반차 신청 처리 (1구간 / 1+2구간 / 교대 분기)
- [ ] `buildApprovalChain()` 결재선 구성
- [ ] 결재선 일괄 생성 (자동 승인 케이스 포함)
- [ ] 결재 처리 (승인/반려) + 다음 단계 자동 진행
- [ ] 무결성 검증 (NODE 관리자 변경/삭제 차단)

### Phase 3: DTO + Mapper
- [ ] Request/Param/Command/Query/Result/Response DTO 작성 (`/createApi` 활용)
- [ ] MyBatis XML Mapper 작성
- [ ] `gvCmpnyCd`, `gvUserCd` 토큰 필드 포함

### Phase 4: Controller + API
- [ ] 신청/조회/취소/결재 처리 API 컨트롤러 작성
- [ ] kebab-case URL 컨벤션 준수

### Phase 5: 테스트
- [ ] 시간차 신청 정밀도 테스트 (10-18 근무자 1시간 = 0.14286)
- [ ] 휴게시간 가로지름 거부 테스트
- [ ] 잔여 부족 시 더 작은 단위 안내
- [ ] 결재선 구성 (PASS, 자동 승인, 빈 결재선)
- [ ] 사후 신청 후 결재 흐름 전체 케이스
- [ ] NODE 관리자 변경 차단 검증

---

## 8. 결정된 정책 요약 (참고)

| 항목 | 결정 |
|---|---|
| 0.125 정밀도 | `decimal(8,5)`, 1년 누적 오차 무시 가능 |
| 1일 소정근로분 출처 | `tb_user_work_plan` + `tb_sch_mgmt` 동적 조회 |
| 시간차 휴게 가로지름 | 거부 |
| 반차 휴게 가로지름 | 허용 (차감 0.5 고정) |
| 신청분 > 1일 소정 | 거부 (안전장치) |
| 1일 다중 시간차 | `MAX_DAILY_REQUEST` 카운트로 제어 (기존 컬럼) |
| 같은 날 단위 혼용 | 허용 (시간 겹침만 검증) |
| 잔여 < 신청 | 더 작은 단위 안내 |
| 시간차 취소 | 시작 시각 이전까지만 |
| 사전 신청 | 즉시 승인 |
| 사후 신청 | NODE 기반 결재 |
| 사후 신청 기간 | 회사별 설정 (TB_BAIM_VAL) |
| 출퇴근 데이터 | 화면 종합 표시, 데이터는 별도 유지 |
| 잔량 표시 | "N.MMMMM일" 일수 형태 (시간 환산 없음) |
| 과거 일자 스케줄 | 변경 불가 |
| 미래 일자 스케줄 변경 | 영향받는 시간차 자동 재계산 또는 사용자 동의 후 삭제 |
| 사후 신청 적용 범위 | 모든 연차 단위 (1일/반차/시간차) |
| 결재선 NODE 추적 | 신청자 NODE → 상향 순차, PASS 처리 |
| 자기 결재 | 본인이 MAIN인 경우만 자동 승인 |
| SUB ADMIN 신청 | MAIN_ADMIN이 결재, 없으면 다음 NODE |
| 중간 단계 반려 | 후속 Step '00 대기중' 그대로 종결 |
| 결재 진행 중 관리자 변경 | 차단 |
| NODE 삭제 | 소속 사용자 0명일 때만 |
| 사후 정정 | HR 담당자가 연차 관리 메뉴에서 직접 처리 |
| 결재선 row 생성 시점 | 신청 시점에 일괄 생성 |

---

## 9. 다음 단계 (현재 작업 범위 외)

- 연차 관리 메뉴 (HR 담당자용 사후 정정 화면)
- 출근/지각/조퇴 판단 로직 (시간차 연차와 출퇴근 데이터 종합)
- OT 계산 시 시간차 연차 처리 (유급 인정)
- 약정 휴가 그룹 관리 (생일/경조사/포상)
- 알림 시스템 (Push, 인앱 알림)
