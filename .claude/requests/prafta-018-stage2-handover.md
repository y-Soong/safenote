# PRAFTA-018 단계 2 — 세션 핸드오버 노트

작성일: 2026-05-20
작성자: Claude Code (사용자 요청)
목적: 다음 Claude Code 세션에서 prafta-018 단계 2 작업을 끊김 없이 이어가기 위한 컨텍스트 스냅샷.

---

## 1. 현재 진행 상태

| 단계 | 상태 | 비고 |
|---|---|---|
| 단계 1 (DDL/시드) | **완료** | `.claude/requests/prafta-018.sql`을 prafta DB에 적용 완료. 전 섹션 실측 검증 완료 (HIRE_DATE/EMPLOYMENT_TYPE/CONTRACT_END_DATE + SYSTEM_YN + 5컬럼 3인덱스 + 신규 테이블 5종 + SYS035~SYS042 시드 8+33건). |
| 단계 2 분해 | **완료 (Notion 등록 대기)** | planner가 5개 작업으로 분해. 사용자 Q&A로 보정 완료. Notion "작업 로그" 등록은 사용자 최종 승인 후. |
| 단계 2 구현 | **미착수** | Notion 등록 후 PLNprafta-018001부터 developer 위임 예정. |
| 화면 작업 (Baim_07/UserInfoPop/Attd_09) + 스케줄러 배치 | **미착수 (단계 3)** | 단계 2 종료 후 별도 분해 예정. `prafta-018.md` 원본 요청서에 명시. |

> **Notion에는 이미 단계 1 흔적이 `PRAFTA_018-DDL-*` prefix로 등록되어 있음** (이전 세션 MCP 연결 문제로 완료 처리 미흡 가능). 단계 2 작업 ID는 planner.md 규칙상 `PLNprafta-018XXX` prefix를 사용 — 두 prefix가 혼재하는 것은 의도된 상태.

---

## 2. 단계 2 분해 결과 (최종 확정본)

작업서명: `prafta-018` / ID 형식: `PLNprafta-018XXX` / 채번 시작: **001**

### PLNprafta-018001 — STATUS 동기화 서비스 + 만료 배치

- 유형: backend / 영역: web / 모듈: `common/cmm/leave` (신설) + `common/schedule/leave/scheduler/` (신설)
- 정책서 출처: `attd/08-leave.md` §8.5.8 (STATUS↔EXPIRE_YN/DEL_YN 매핑표), §8.5.6 (RESET_ALL 시 CANCELED)
- 핵심:
  1. `STATUS`를 단일 SoT로 두고 `EXPIRE_YN`/`DEL_YN`은 STATUS 변경 시 함께 UPDATE (단방향)
  2. 만료 배치: `@Scheduled(cron="0 5 0 * * *")` — 자정+5분, `LeaveStatusScheduler` 신설. `AVAIL_TO_DATE < CURRENT_DATE AND STATUS='ACTIVE'` → `STATUS='EXPIRED'`
  3. 소진 동기화: `recalcUsedDays(grantId)` API 노출 (`tb_user_leave_use` 집계). `USED_DAYS ≥ GRANT_DAYS` 도달 시 `STATUS='EXHAUSTED'`
  4. 취소: `STATUS='CANCELED'` 전이 시 `DEL_YN`은 'N' 유지 (소프트 취소)
  5. `EXPIRE_YN` deprecation 계획: 본 작업에서는 컬럼 유지(외부 조회 호환), 신규 SELECT는 STATUS만 사용. 컬럼 DROP은 단계 3 이후 별도 작업.
- **호출처 hook 연결은 본 작업에서 하지 않음** — Attd_09 화면 작업(단계 3) 시 `LeaveGrantStatusService.recalcUsedDays(grantId)`를 호출하도록 연결. 본 작업은 service 인터페이스만 channel-agnostic하게 노출.
- 영향 파일:
  - 신규: `prafta-backend/src/main/java/com/prafta/common/cmm/leave/` (Service/ServiceImpl/Mapper/Mapper.xml/VO)
  - 신규: `prafta-backend/src/main/java/com/prafta/common/schedule/leave/scheduler/LeaveStatusScheduler.java`
  - 확인: `MainApplication.java`에 `@EnableScheduling` 활성 여부 (미활성 시 추가)
- 선행: 없음 (단계 1 SQL 적용 완료가 사실상 선행)
- 우선순위: 단계 2의 토대. attd 영역 +1단계 격상.

### PLNprafta-018002 — 시스템 LEAVE_CD 6종 시드

- 유형: backend / 영역: web / 모듈: 마이그레이션 SQL + (attd03 가드)
- 정책서 출처: `attd/08-leave.md` §8.5.5 (시드 6종 표)
- 산출물 **순수 SQL 2개** (자바 service 클래스 없음 — 사용자 결정):
  1. **일회성 백필 SQL**: `prafta-backend/src/main/resources/sql/migration/prafta-018-seed-system-leave-cd.sql` — 현재 `tb_cmpny_m` 활성 회사 전체 대상 6종 시드 멱등 INSERT (`INSERT ... WHERE NOT EXISTS`)
  2. **신규 회사 생성 프로시저용 시드 SQL 조각**: `prafta-backend/src/main/resources/sql/migration/prafta-018-seed-system-leave-cd-template.sql` — `:CMPNY_CD`를 파라미터로 받는 INSERT 템플릿. **추후 사용자가 만들 "신규 고객 생성 프로시저"에 포함**될 예정 (별도 분해 대상).
- 시드 6종 코드값 (§8.5.5):
  - `SYS_ANNUAL`, `SYS_MONTHLY`, `SYS_TENURE_BONUS`, `SYS_PROMOTION`, `SYS_PREGRANT`, `SYS_BIRTHDAY`
  - 공통: `LEAVE_TYPE='02'`, `GRANT_TYPE='01'`, `PAID_TYPE='01'`, `LEAVE_NATURE_TYPE='01'` (SYS_BIRTHDAY만 `'02'`), `SYSTEM_YN='Y'`, `USE_YN='Y'`, `INSERT_NO='SYSTEM'`
- 추가 자바 코드:
  - `Attd03ServiceImpl` LeaveType UPDATE/DELETE 진입부에 `SYSTEM_YN='Y'` 가드 추가 → `ApiException(ATTD_403_SYSTEM_SEED_READONLY)` 반환
  - `AttdErrorCode.java`에 `ATTD_403_SYSTEM_SEED_READONLY` 추가
- 선행: PLNprafta-018001 (안전성 위해 순차 권장)
- 우선순위: 자동 부여(STATUTORY_*)가 LEAVE_CD 매칭 가능해야 함. attd 영역 +1단계 격상.

### PLNprafta-018003 — TB_LEAVE_POLICY 활성 1건 보장 + Baim_07 backend 모듈

- 유형: backend / 영역: web / 모듈: `baim/baim07` (신규 모듈 전체) + `common/cmm/leave`
- 정책서 출처: `attd/08-leave.md` §8.5.2 (회사당 활성 1개 + 7-axis), §8.5.3 (cross-axis 검증), §8.5.7 (권한 AUTH_MASTER/AUTH_HR_MANAGER), §8.5.8 (기 부여 보호)
- 핵심:
  1. `@Transactional` + `SELECT ... FOR UPDATE`로 `TB_LEAVE_POLICY` 회사 행 락 → 직렬화
  2. 처리 순서: 기존 활성 정책 `USE_YN='N'` UPDATE → 신규 정책 INSERT(`USE_YN='Y'`) → `TB_LEAVE_USAGE_POLICY` 1:1 INSERT → `TB_LEAVE_POLICY_HISTORY`에 PREV/NEW snapshot JSON 기록. 어느 한 단계 실패 시 전체 롤백
  3. Cross-axis 검증: AXIS3≠PRORATE면 AXIS4='CEIL' 강제, AXIS4=HALF_DAY이면 `TB_LEAVE_USAGE_POLICY.ALLOW_HALF_DAY='Y'` 강제, AXIS5_TENURE_MODE=LEGAL이면 START_YEAR=3/INTERVAL=2 강제
  4. `APPLY_FROM_DATE`는 미래/오늘만 (과거 소급 금지 — §8.5.8)
  5. 권한 가드 진입부 강제
  6. `IMPACT_SUMMARY` JSON 계산 (영향 인원/예상 추가 부여 일수)
- 신규 endpoint:
  - `POST   /api/web/baim/baim07/policy`
  - `PUT    /api/web/baim/baim07/policy/{policySeq}`
  - `GET    /api/web/baim/baim07/policy/active`
  - `GET    /api/web/baim/baim07/policy/history`
  - `POST   /api/web/baim/baim07/policy/impact-preview`
- 영향 파일:
  - 신규: `baim/baim07/` 전체 (controller/service/impl/mapper/Mapper.xml/dto/application/result/vo)
  - 신규: `common/cmm/leave/service/LeavePolicyService.java`
  - 수정: `AttdErrorCode.java` (`ATTD_400_AXIS_VIOLATION`, `ATTD_403_POLICY_FORBIDDEN`, `ATTD_409_POLICY_CONCURRENT` 추가)
- 선행: PLNprafta-018001, PLNprafta-018002
- 우선순위: 단계 2 핵심. 동시성/락 경합 가능성으로 단독 작업 분리. attd 영역 +1단계 격상.

### PLNprafta-018004 — SYS022/023/024 표기·매핑 정합성 점검 (READ-ONLY)

- 유형: backend (점검 위주, 데이터 불일치 발견 시 보정 SQL은 별도 작업으로 분리)
- 정책서 출처: `attd/08-leave.md` §8.1.1 (휴가성격 법정/약정), §8.5.5 (시드 시 LEAVE_NATURE_TYPE 매핑)
- 핵심:
  1. MCP MySQL SELECT 쿼리로 SYS022/023/024 코드값 명칭 + `tb_leave_type_mgmt` 실제 행 데이터 점검
  2. 정책서 표(§8.1.1 §8.5.5)와 DB 코드값 1:1 비교
  3. 결과 분류:
     - **정합** → 추가 마이그레이션 불필요
     - **표기 불일치** (정책서에 "약정" 잔존) → 정책서 본문 정정 (별도 사용자 검토)
     - **데이터 불일치** (`LEAVE_NATURE_TYPE` 값에 '03' 등 미정의 값) → 보정 SQL draft 작성. **본 작업에서 적용하지 않고 별도 PLNprafta-018XXX로 분리**
- 산출물: 마크다운 점검 보고서 → **`.claude/context/policies/attd/_audit/prafta-018-syst-val-audit.md`** 로 저장
- 선행: PLNprafta-018002와 병렬 가능 (시드 코드값 단일 출처는 §8.5.5 정책서이므로 점검 결과로 코드값이 바뀌지 않음)
- 우선순위: SELECT 위주. 단 잘못된 nature 분류는 휴가성격 오분류 → 법적 영역 +1단계 격상.

### PLNprafta-018005 — schema-full.sql 스냅샷 갱신

- 유형: backend (인프라/스냅샷)
- 정책서 출처: `CLAUDE.md` "DB 스키마 참조 규칙 - 2순위 schema-full.sql 정기 갱신"
- 핵심:
  1. 단계 2 모든 작업(001~004 + 그에서 발생한 보정 SQL) 적용 완료 직후 실행
  2. `mysqldump --no-data --routines --triggers --events` 옵션으로 DDL+트리거/프로시저/뷰 본문 포함, 데이터 제외
  3. 출력 위치: `.claude/context/schema-full.sql` (덮어쓰기). 상단에 `-- Updated 2026-MM-DD after PLNprafta-018001~004` 주석 추가
  4. 부수 검증: `grep -E "TB_LEAVE_POLICY|HIRE_DATE|SYSTEM_YN|IDEMPOTENCY_KEY|SYS035" schema-full.sql`
  5. 실제 dump 명령은 사용자가 로컬에서 실행하는 것이 안전 (자격증명). developer는 명령어만 제공.
- 선행: PLNprafta-018001, 002, 003, 004 모두 완료
- 우선순위: 단계 2 종료 후 마지막. 단계 3 화면 작업 시 최신 스냅샷 필요.

---

## 3. 의존성 그래프

```
[단계 1 SQL 적용 완료]
        |
        v
PLNprafta-018001 (STATUS 동기화 + 만료 배치)
        |
        +--> PLNprafta-018002 (시스템 LEAVE_CD 시드)
        |        |
        |        +--> PLNprafta-018004 (점검 보고서, 병렬 가능)
        |
        +--> PLNprafta-018003 (TB_LEAVE_POLICY 활성 보장 + Baim_07 backend)
                 |
                 v
              PLNprafta-018005 (schema-full.sql dump 재실행)
```

병렬 진행 가능: **001 완료 후 002와 003을 병렬**로 진행. 004는 002와 병렬. 005는 모두 종료 후.

---

## 4. 사용자 Q&A 결정 사항 (반영 완료)

| # | 질문 | 결정 |
|---|---|---|
| 1 | 채번 시작점 | `PLNprafta-018001`부터. Notion의 `PRAFTA_018-DDL-*`(단계 1)와 prefix가 다르나 의도된 상태. |
| 2 | 시스템 시드 — 회사 생성 자바 훅 | **불필요.** 자바 service 클래스 제거. 추후 사용자가 만들 "신규 고객 생성 프로시저"에 포함될 시드 SQL 조각을 별도 산출. |
| 3 | `tb_user_leave_use` 변경 진입점 | 현재 prafta 코드에 없음. **API만 노출**, 호출처 hook은 단계 3 Attd_09 작업 시 연결. |
| 4 | 만료 배치 cron | `0 5 0 * * *` (자정+5분) 그대로 진행. |
| 5 | 점검 보고서 위치 | `.claude/context/policies/attd/_audit/prafta-018-syst-val-audit.md` 신규 디렉토리 + 파일. |

---

## 5. 다음 세션이 해야 할 일 (순서대로)

### 5.1 즉시 실행 가능 (사용자 승인이 이미 떨어진 상태라면)

1. **planner에게 Notion "작업 로그" 일괄 등록 위임**
   - 5개 행(PLNprafta-018001~005) 등록
   - 각 행 "상세 설명" 첫 줄에 `[단계 2]` 태그 + 정책서 출처
   - "선행 작업" 컬럼은 Notion Relation으로 연결 (단계 1 행 `PRAFTA_018-DDL-*` 참조 가능하면 연결)
   - **planner의 Notion 도구 가용성 먼저 확인**: 이전 세션에서 "현재 세션에서 Notion 직접 호출 불가"라고 잘못 보고한 적 있음. ToolSearch로 `mcp__claude_ai_Notion__*` 가용 여부 확인 필요.

2. **PLNprafta-018001부터 developer 위임**
   - 작업 1건 단위로 위임 (`subagent_type=developer`)
   - 위임 시 prompt에 포함할 컨텍스트:
     - 작업 ID, 정책서 출처(파일 경로 + 섹션)
     - 영향 파일 목록
     - 선행 작업 완료 여부
     - 본 핸드오버 노트(`.claude/requests/prafta-018-stage2-handover.md`) 참조 지시
   - developer 완료 후 qa/security 차례 검증 위임

### 5.2 사용자 추가 결정이 필요한 경우

- **단계 1 Notion 행(`PRAFTA_018-DDL-*`) 상태 정리** — 사용자가 직접 Notion에서 완료 처리. Claude가 대신 하려면 사용자 명시적 지시 필요.
- **PLNprafta-018003의 5개 endpoint URL 패턴**이 prafta 컨벤션과 일치하는지 사용자 검토. 일치 안 하면 보정.
- **PLNprafta-018002의 마이그레이션 SQL 디렉토리 위치** (`prafta-backend/src/main/resources/sql/migration/`)가 prafta에 이미 있는지 확인. 없으면 사용자 협의 후 생성.

### 5.3 단계 3 (장기 계획, 본 핸드오버 범위 밖)

prafta-018.md 원본 요청서에 명시된 화면 작업:
- `baim/Baim_07.vue` (정책 메인 + 직접설정 + 영향분석) — 신규 3화면
- `user/UserInfoPop.vue` 수정 — 입사일 편집 모달
- `attd/Attd_09.vue` (대시보드 + 사용자 상세 + 수동부여) — 신규 3화면
- 연차 부여 스케줄러 배치 (별도 프로젝트로 분리 예정, 임시 위치는 `common/schedule/leave/`)

단계 2 종료 후 planner에 별도 분해 요청.

---

## 6. 단계 1 적용 검증 증거 (재확인용)

다음 SQL을 실행하여 단계 1이 깨지지 않았는지 빠르게 검증 가능 (read-only):

```sql
-- 1. tb_user 신규 컬럼 3개 존재
SELECT COLUMN_NAME FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user'
   AND COLUMN_NAME IN ('HIRE_DATE','EMPLOYMENT_TYPE','CONTRACT_END_DATE');
-- 기대: 3행

-- 2. tb_user_leave_grant 신규 5컬럼 + 3인덱스
SELECT COUNT(*) AS COL_CNT FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user_leave_grant'
   AND COLUMN_NAME IN ('GRANT_TYPE','USED_DAYS','POLICY_SEQ','IDEMPOTENCY_KEY','STATUS');
-- 기대: 5

-- 3. 신규 테이블 5종
SELECT COUNT(*) FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME IN ('TB_USER_SERVICE_CREDIT','TB_USER_HIRE_DATE_HISTORY',
                       'TB_LEAVE_POLICY','TB_LEAVE_POLICY_HISTORY','TB_LEAVE_USAGE_POLICY');
-- 기대: 5

-- 4. SYS035~SYS042 시드
SELECT COUNT(*) FROM tb_syst_val_m WHERE SYST_VAL_CD BETWEEN 'SYS035' AND 'SYS042';
-- 기대: 8
SELECT COUNT(*) FROM tb_syst_val_d WHERE SYST_VAL_CD BETWEEN 'SYS035' AND 'SYS042';
-- 기대: 33 (SYS035=7 + SYS036=2 + SYS037=3 + SYS038=4 + SYS039=3 + SYS040=4 + SYS041=4 + SYS042=6)
```

검증 명령 예시 (Windows + Git Bash):
```
mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' --default-character-set=utf8mb4 --batch prafta -e "<위 SQL>"
```

---

## 7. 환경 / 도구 메모

- DB: MySQL 8.0.42 / 계정: `dev_prafta` / 호스트: `127.0.0.1:3306` / DB명: `prafta`
- MCP MySQL 서버 (`prafta-mysql`) 가용성: 본 세션에서는 ToolSearch 결과 미노출. Bash로 `mysql` CLI 직접 호출은 settings.local.json에서 허용됨 (`Bash(mysql *)`)
- MCP Notion: `mcp__claude_ai_Notion__*` 도구는 이번 세션에서 ToolSearch로 로드 가능. planner는 정의상 Notion 도구를 가져야 하나, 이전 세션에서 "직접 호출 불가" 오인이 있었음 → 다음 세션 처음에 가용성 재확인 필요
- 정책서 진입점: `.claude/context/policies/README.md` → `attd/INDEX.md` → `attd/08-leave.md` §8.5
- 본 분해 원본 요청서: `.claude/requests/prafta-018.md`
- 단계 1 SQL 원본: `.claude/requests/prafta-018.sql`
- 본 핸드오버 노트: `.claude/requests/prafta-018-stage2-handover.md` (이 파일)

---

## 8. 미해결 / 주의 사항

1. **`prafta-backend/src/main/resources/sql/migration/` 디렉토리 존재 여부 미확인** — 다음 세션에서 PLNprafta-018002 진행 전 확인 필요.
2. **`@EnableScheduling` 활성 여부 미확인** — `MainApplication.java`에 어노테이션이 있는지 PLNprafta-018001 진행 전 확인.
3. **단계 1 Notion 행 정리 미완** — `PRAFTA_018-DDL-*` 행들의 상태가 "완료"가 아닌 경우 다음 세션에서 사용자에게 안내 후 정리.
4. **본 분해는 사용자 승인 후 Notion에 등록되어야 함** — 본 핸드오버 노트 작성 시점 기준 Notion 등록은 아직 안 됨.
