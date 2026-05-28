# prafta-018 단계 2 — 세션 핸드오프 노트

작성일: 2026-05-21
사유: PLNprafta-018004 점검 보고서 위임 중 API 529 Overloaded 에러로 작업 중단. 새 세션에서 이어가기 위한 상태 보존.
보충 자료: `.claude/requests/prafta-018-stage2-handover.md` (단계 2 전체 분해본 + Q&A 결정 사항 — 기준 문서로 함께 정독)

---

## 1. prafta-018 전체 목표와 단계

법정 연차 부여 정책 도메인 신설. 원본 요청서 `.claude/requests/prafta-018.md`.

| 단계 | 범위 | 상태 |
|---|---|---|
| **단계 1** (DDL/시드) | tb_user 3컬럼 + tb_leave_type_mgmt SYSTEM_YN + tb_user_leave_grant 5컬럼 3인덱스 + 신규 테이블 5종 + SYS035~042 33건 시드 | **DB 적용 완료, 검증 끝** |
| **단계 2** (백엔드 도메인) | PLNprafta-018001~005 — STATUS 동기화 / 시스템 시드 / 활성 정책 보장 / 정합성 점검 / schema dump | **진행 중 (003 완료, 004 중단)** |
| **단계 3** (화면/배치) | Baim_07 (정책 메인/직접설정/영향분석) + UserInfoPop 수정 + Attd_09 (대시보드/사용자 상세/수동부여) + 연차 부여 스케줄러 | **미착수** |

작업서명: `prafta-018` / ID 형식: `PLNprafta-018XXX` (소문자 prefix 유지).

---

## 2. 단계 2 진행 현황

### 2.1 완료된 작업 (Notion 상태: 완료)

#### PLNprafta-018001 — STATUS 동기화 서비스 + 만료 배치
- 신규 6개 파일 (`common/cmm/leave/` 전체 + `common/schedule/leave/scheduler/LeaveStatusScheduler.java`)
- 보안 검토 후 Mapper.xml `updateStatusWithSync` WHERE에 STATUS 가드 1줄 추가:
  ```
  AND (STATUS != 'CANCELED' OR #{status} = 'CANCELED')
  ```
- QA pass-with-notes (정책서 §8.5.8 매핑표 6항목 PASS, 엣지 13 PASS)
- TODO 잔존: LEAVE_MINUTES 환산 표준 / LEAVE_STATUS 카탈로그 (Attd_09 시점 결정)

#### PLNprafta-018002 — 시스템 LEAVE_CD 시드 6종
- 신규 2개 SQL 파일 (백필 + 신규 회사 프로시저용 템플릿). 자바 service 클래스 없음 (사용자 결정).
- 디렉토리 신설: `prafta-backend/src/main/resources/sql/migration/`
- 수정 4개 파일: `AttdErrorCode.java` (+ATTD_403_010), `Attd03Mapper.java`/`xml` (selectSystemYn), `Attd03ServiceImpl.java` (guardSystemSeedReadOnly 메서드)
- 시드 6종: SYS_ANNUAL / SYS_MONTHLY / SYS_TENURE_BONUS / SYS_PROMOTION / SYS_PREGRANT / SYS_BIRTHDAY (LEAVE_NATURE_TYPE='01', SYS_BIRTHDAY만 '02')
- DB 실 테이블명: `tb_cmpny` (정책서/핸드오버의 `tb_cmpny_m`이 아님)
- QA pass-with-notes (멱등성 실측 PASS)
- 발견: **`GRANT_ASSIGN_MMDD` 컬럼 부재 vs Attd03Mapper.xml 참조 불일치** → 018004로 이관

#### PLNprafta-018003 — TB_LEAVE_POLICY 활성 1건 보장 + Baim_07 backend
- 신규 21개 파일:
  - `common/cmm/leave/`: 9개 (`vo/LeavePolicyVO.java`, `LeavePolicyHistoryVO.java`, `ImpactSummaryVO.java`, `PagedResult.java`, `command/LeavePolicyCommand.java`, `mapper/LeavePolicyMapper.java`, `LeavePolicyMapper.xml`, `service/LeavePolicyService.java`, `service/impl/LeavePolicyServiceImpl.java` (471→479줄))
  - `web/baim/baim07/`: 12개 (controller/service/impl/application/param × 3 + dto/request × 2 + dto/response × 4)
- 수정 1개: `AttdErrorCode.java` (+ATTD_400_020, ATTD_403_011, ATTD_409_010)
- 5 endpoint:
  - `POST /api/web/baim/baim07/policy` (createPolicy)
  - `PUT /api/web/baim/baim07/policy/{policySeq}` (updatePolicy)
  - `GET /api/web/baim/baim07/policy/active`
  - `GET /api/web/baim/baim07/policy/history`
  - `POST /api/web/baim/baim07/policy/impact-preview`
- 7-axis cross 검증 + IMPACT_SUMMARY 계산 + 권한 가드(AUTH_MASTER/AUTH_HR_MANAGER)
- **보안 검토 후 추가 적용된 2가지 변경**:
  1. **AXIS3 PRORATE+PREGRANT_YN='Y' 동시 활성화 금지 규칙 5줄 추가** (`LeavePolicyServiceImpl.java:300-304` 부근, 사용자 결정으로 정책서 보강)
  2. **TB_LEAVE_POLICY UX_TB_LEAVE_POLICY_ACTIVE UNIQUE 제약 DDL 추가**
     - 파일: `prafta-backend/src/main/resources/sql/migration/prafta-018-stage2-policy-active-unique.sql`
     - 실 DB 적용 검증: `NON_UNIQUE=0`, `EXPRESSION=(case when (USE_YN = 'Y') then CMPNY_CD end)` 확인 완료
  3. **QA 후 try-catch 1줄 패치 적용** (아래 §3 상세)
- 컴파일/리소스 모두 PASS, DB 검증 PASS
- QA pass-with-notes (정책서 §8.5 14 PASS, 엣지 19 PASS / 1 WARN-Medium 해소됨)

### 2.2 중단된 작업

#### PLNprafta-018004 — SYS022/023/024 + tb_leave_type_mgmt 정합성 READ-ONLY 점검
- **Notion 상태: 개발중** (위임 직전 갱신했으나 위임 자체가 529 에러로 실패)
- **위임 prompt는 작성 완료 — 새 세션에서 동일 prompt로 재위임 가능**
- 산출물 예정 경로: `.claude/context/policies/attd/_audit/prafta-018-syst-val-audit.md` (디렉토리는 신설 필요)
- 점검 대상 4개 항목 (§4에서 상세)

#### PLNprafta-018005 — schema-full.sql 스냅샷 갱신
- **Notion 상태: 분해완료**
- 단계 2 모든 작업 완료 후 mysqldump 실행. 미착수.

---

## 3. LeavePolicyServiceImpl.java try-catch 패치 상세

QA WARN-Medium #1 해소를 위한 1줄 보강. 동시 INSERT race에서 DB UNIQUE 위반(`DataIntegrityViolationException`)이 raw 500으로 노출되는 것을 `ATTD_409_010`으로 매핑.

### 변경 1 — import 추가 (L13~14)

```java
import org.springframework.dao.DataIntegrityViolationException;  // 신규
import org.springframework.dao.PessimisticLockingFailureException;  // 기존
```

### 변경 2 — `saveInternal` step 7 try/catch 감싸기 (L202~211)

```java
// 7. 신규 정책 INSERT (POLICY_SEQ 회수)
//    UX_TB_LEAVE_POLICY_ACTIVE UNIQUE 위반(동시 INSERT race) 시 ATTD_409_010 매핑.
LeavePolicyVO newPolicy = buildNewPolicyVO(cmpnyCd, command, userCd);
try {
    leavePolicyMapper.insertPolicy(newPolicy);
} catch (DataIntegrityViolationException e) {
    log.warn("활성 정책 UNIQUE 위반 (동시 INSERT race). cmpnyCd={}, changeType={}",
            cmpnyCd, changeType, e);
    throw new ApiException(AttdErrorCode.ATTD_409_010);
}
Long newPolicySeq = newPolicy.getPolicySeq();
```

### 변경 3 — DB DDL 적용 (실 DB + 파일 산출물 동시)

```sql
-- prafta-018-stage2-policy-active-unique.sql
ALTER TABLE `TB_LEAVE_POLICY`
  ADD UNIQUE KEY `UX_TB_LEAVE_POLICY_ACTIVE`
    ((CASE WHEN `USE_YN` = 'Y' THEN `CMPNY_CD` END));
```

- 실 DB 적용 완료 (mysql CLI로 직접 ALTER 실행)
- 검증: information_schema.STATISTICS로 NON_UNIQUE=0 + EXPRESSION 일치 확인

### 컴파일 검증

`gradlew compileJava --no-daemon -q` → EXIT 0 (PASS).

---

## 4. PLNprafta-018004 점검 보고서 — 작성 방향

작업 성격: **READ-ONLY 점검** (DB SELECT만, INSERT/UPDATE/DELETE 금지). 보정 SQL은 draft로만 작성, 별도 작업으로 분리.

### 점검 대상 4개 항목

1. **SYS022/023/024/025 코드값 점검**
   ```
   mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' --default-character-set=utf8mb4 --batch prafta -e \
     "SELECT SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, USE_YN FROM tb_syst_val_d \
      WHERE SYST_VAL_CD IN ('SYS022','SYS023','SYS024','SYS025') \
      ORDER BY SYST_VAL_CD, SYST_VAL_D_CD;"
   ```
   - SYS022 LEAVE_TYPE: '01'/'02' 명칭
   - SYS023 GRANT_TYPE: '01'(자동)/'02'(수동) 등 — 단계 1 시드 GRANT_TYPE='01'과 정합?
   - SYS024 LEAVE_NATURE_TYPE: '01'(법정)/'02'(특별) — 정책서 §8.1.1 "법정/약정" 표기와 일치?
   - SYS025 USE_UNIT_TYPE: '01'/'02'/'03'/'04' (1일/0.5일/0.25일/0.125일)

2. **tb_leave_type_mgmt 실데이터 점검**
   ```
   SELECT CMPNY_CD, LEAVE_CD, LEAVE_NM, LEAVE_TYPE, GRANT_TYPE, PAID_TYPE, LEAVE_NATURE_TYPE, SYSTEM_YN, USE_YN FROM tb_leave_type_mgmt;
   ```
   - 단계 1 시드 6종 + 사용자 채번 행 검증
   - LEAVE_NATURE_TYPE 값이 SYS024 코드값과 일치
   - SYSTEM_YN 분포

3. **`GRANT_ASSIGN_MMDD` 컬럼 부재 vs Attd03Mapper.xml 참조 불일치** (PLNprafta-018002 보안 검토에서 발견된 이슈)
   - `SHOW COLUMNS FROM tb_leave_type_mgmt LIKE 'GRANT%';` 결과
   - `Attd03Mapper.xml`에서 GRANT_ASSIGN_MMDD 참조 줄 grep
   - 결론: (a) 컬럼 실재 → schema-full.sql stale (b) 컬럼 부재 → mapper.xml 수정 필요 OR DDL 마이그레이션 draft

4. **정책서 "약정" vs "특별" 표기 점검**
   - `.claude/context/policies/attd/` 하위 .md 전체에서 "약정" 단어 grep
   - 발견 시 파일/줄/문맥 정리 → "특별"로 정정 권고 vs "약정"이 의도된 별도 표기인지 판단 요청

### 산출물

- 마크다운 보고서: `C:\prafta\.claude\context\policies\attd\_audit\prafta-018-syst-val-audit.md` (디렉토리 신규 생성)
- 보고서 구조: 점검 개요 / 결과 요약 / 4개 항목 실측 결과 / 권고 조치 / 검증 명령 재실행용

### 절대 금지

- DB INSERT/UPDATE/DELETE
- 정책서 본문 직접 수정 (점검만)
- mapper.xml 수정 (발견 시 별도 작업으로 분리 권고만)
- 추정으로 항목 채우지 말 것

---

## 5. git 상태 (작성 시점)

### 5.1 추적된 파일 — 수정 (M)

작업 디렉토리 전체에 70개 이상의 M 파일이 있으나, **prafta-018 단계 2와 직접 연관된 수정 파일은 다음 2개만**:

- `prafta-backend/src/main/java/com/prafta/common/error/attd/AttdErrorCode.java` (018002에서 ATTD_403_010 추가, 018003에서 ATTD_400_020/403_011/409_010 추가)
- `prafta-backend/src/main/java/com/prafta/web/attd/attd03/service/impl/Attd03ServiceImpl.java` (018002에서 guardSystemSeedReadOnly 호출 + 메서드 추가)
- `prafta-backend/src/main/java/com/prafta/web/attd/attd03/mapper/Attd03Mapper.java` (018002에서 selectSystemYn 메서드 추가)
- `prafta-backend/src/main/resources/com/prafta/web/attd/attd03/mapper/Attd03Mapper.xml` (018002에서 selectSystemYn select 블록 추가 + 기존 변경 잔존)

나머지 M 파일은 prafta-017 등 이전 작업 진행분이며 본 단계 2와 직접 무관.

### 5.2 추적되지 않은 파일 — 신규 (??)

prafta-018 단계 2가 신규로 추가한 디렉토리/파일:

- `prafta-backend/src/main/java/com/prafta/common/cmm/leave/` (018001+003 전체 신규)
- `prafta-backend/src/main/java/com/prafta/common/schedule/leave/` (018001 스케줄러)
- `prafta-backend/src/main/java/com/prafta/web/baim/baim07/` (018003 전체 신규)
- `prafta-backend/src/main/resources/com/prafta/common/cmm/leave/` (Mapper.xml 2종)
- `prafta-backend/src/main/resources/sql/migration/` (018002 시드 SQL 2종 + 018003 UNIQUE DDL 1종)
- `.claude/context/policies/attd/_audit/` (018004 보고서 예정 위치 — 디렉토리는 아직 미생성)
- `.claude/requests/prafta-018-stage2-handover.md` (분해 시점 핸드오버)
- `.claude/requests/prafta-018-004-delegate-prompt.md` (018004 위임 prompt 전문 — 새 세션 재위임용)
- `.claude/requests/prafta-018.md`, `prafta-018.sql` (원본 요청서 + 단계 1 SQL)
- `HANDOFF.md` (본 파일)

### 5.3 git diff 통계 (참고)

- 총 74 files changed, 7612 insertions, 6252 deletions
- 대부분 prafta-017 frontend 작업이 누적된 결과. 단계 2 직접 추가분은 21개 신규 + 4개 수정 = 약 1500줄.

### 5.4 최근 commit

```
e73043f commit
be3232d commit
22b2b6f commit
d8d9fdf commit
15aaf96 근태관리 연차타입까지 개발 완료
```

**단계 2 작업은 아직 commit되지 않음. 새 세션에서 commit 정책 결정 필요.**

---

## 6. Notion 페이지 연동 상태

작업 로그 DB: `35e4cf4d-c460-8095-89fe-f07c5506345e`

| 작업 ID | Notion 페이지 ID | 현재 상태 | 비고 |
|---|---|---|---|
| PLNprafta-018001 | `3664cf4d-c460-819e-8a1c-f78ec5061eff` | **완료** | 담당: planner/developer/security/qa |
| PLNprafta-018002 | `3664cf4d-c460-81c2-aa73-f8ad65b0496e` | **완료** | 담당: planner/developer/security/qa |
| PLNprafta-018003 | `3664cf4d-c460-8166-8e75-d362fa249034` | **완료** | 담당: planner/developer/security/qa |
| PLNprafta-018004 | `3664cf4d-c460-81b0-9ed6-ea160aaa9572` | **개발중** ⚠️ | 위임 실패로 실제 작업 미진행. 새 세션에서 재위임 필요 (상태 그대로 두거나 "분해완료"로 되돌리기) |
| PLNprafta-018005 | `3664cf4d-c460-817d-b515-c77e5d3f74b2` | 분해완료 | 단계 2 모든 작업 완료 후 진행 |

기존 단계 1 흔적 (참고용, 본 작업과 별개):
- `PRAFTA-018-DDL-01`, `PRAFTA-018-DDL-02`, `PRAFTA-018-DOCS-01` (대문자 prefix, 별도 채번)

**Notion "보안 리뷰 로그" / "QA 로그" DB는 아직 행 등록 안 됨** — 단계 2 종료 후 일괄 등록 예정 (security 보고서에서 모두 "후속 등록 권고"로 표기됨).
- 보안 검토 ID 예정: `plnprafta-018001-001~003`, `plnprafta-018002-001~004`, `plnprafta-018003-001~003`
- QA ID 예정: `plnprafta-018001-001`, `plnprafta-018002-001`, `plnprafta-018003-001`

---

## 7. 내일 새 세션이 첫 번째로 실행할 명령

### Step 1 — 컨텍스트 로드 (Read 3개)
```
Read C:\prafta\HANDOFF.md
Read C:\prafta\.claude\requests\prafta-018-stage2-handover.md
Read C:\prafta\.claude\requests\prafta-018.md
```

### Step 2 — 환경 sanity check (Bash 1회)
```
mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' --default-character-set=utf8mb4 --batch prafta -e \
  "SELECT 'tb_user_leave_grant'           AS table_name, COUNT(*) AS col_cnt
     FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user_leave_grant'
      AND COLUMN_NAME IN ('GRANT_TYPE','USED_DAYS','POLICY_SEQ','IDEMPOTENCY_KEY','STATUS')
    UNION ALL
   SELECT 'UX_TB_LEAVE_POLICY_ACTIVE     '   AS table_name, COUNT(*) AS col_cnt
     FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'TB_LEAVE_POLICY' AND INDEX_NAME = 'UX_TB_LEAVE_POLICY_ACTIVE';"
```
기대 결과: 5 + 1 = 두 행 출력. 0이 있으면 DB 상태가 깨진 것 — 사용자에게 보고.

### Step 3 — PLNprafta-018004 재위임

Agent 호출:
- subagent_type: `developer`
- description: `PLNprafta-018004 점검 보고서`
- prompt: **`C:\prafta\.claude\requests\prafta-018-004-delegate-prompt.md` 파일을 Read한 뒤, 그 파일의 "(이하 위임 prompt 본문 — 그대로 복사하여 사용)" 이후 내용 전체를 `prompt` 파라미터에 넣어 호출**

해당 파일에는 이전 세션에서 사용했던 위임 prompt 전문이 그대로 저장되어 있어 100% 동일 위임이 가능합니다. (이전 시도가 529 에러로 실패했을 뿐, prompt 자체에는 결함 없음.)

대안: HANDOFF.md §4의 "점검 대상 4개 항목" + "산출물" + "절대 금지" 섹션을 재구성해도 90% 동일하게 동작합니다.

### Step 4 — 018004 완료 후 흐름

본 작업은 산출물이 마크다운 보고서 1개 + 디렉토리 1개라 security/qa 검증을 생략하고 사용자 검토만 받는 게 합리적. 사용자 결정 요청.

승인되면 PLNprafta-018005 위임 (schema-full.sql dump).

---

## 8. 새 세션이 알아야 할 주의 사항

1. **MCP MySQL 서버 (`prafta-mysql`) 미연결 가능성** — Bash `mysql` CLI로 우회. settings.local.json에 `Bash(mysql *)` 허용 등록됨.
2. **MCP Notion 도구는 서브에이전트에 위임되지 않음** — Notion 상태 갱신은 메인 세션이 직접 `mcp__claude_ai_Notion__notion-update-page`로 처리. ToolSearch로 로드 후 사용.
3. **API 529 Overloaded 에러는 일시적** — 동일 위임을 재시도하면 보통 통과. 30초~몇 분 후 재시도.
4. **DB 컬럼명 대소문자 주의** — `lower_case_table_names=1` 설정으로 information_schema는 소문자 반환. 정책서는 대문자 표기. 코드 작성 시 SQL은 대문자, 자바 식별자는 camelCase.
5. **단계 1 SQL 적용 후 schema-full.sql은 stale** — PLNprafta-018005에서 dump 재실행으로 해소 예정. 단계 2 진행 중에는 schema-full.sql을 1순위 참조하지 말고 실 DB DESCRIBE를 우선.
6. **추정 잔존 항목 4종** (Attd_09 단계 3에서 결정):
   - LEAVE_MINUTES 환산 표준
   - LEAVE_STATUS 카탈로그
   - 활성 사용자 정의 (현재 보수적: USE_YN='Y' AND WITHDRAWAL_DATE IS NULL AND ACCOUNT_STATUS='01')
   - IMPACT_SUMMARY 정확한 부여 공식
7. **PRAFTA-017 prafta-017-001 권한 가드 부재 이슈** — attd03 endpoint에 권한 가드 미적용. baim07은 처음부터 가드 적용했으나, attd03 보정은 별도 작업.

---

끝.
