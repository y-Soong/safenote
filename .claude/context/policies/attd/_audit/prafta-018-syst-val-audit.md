# PRAFTA-018 단계 2 — SYS022/023/024 + tb_leave_type_mgmt 정합성 점검 보고서

## 1. 점검 개요

| 항목 | 내용 |
| --- | --- |
| 작업 ID | PLNprafta-018004 |
| 작성일 | 2026-05-21 (live DB 재측정) |
| 점검 범위 | (a) `tb_syst_val_m`/`tb_syst_val_d` SYS021~SYS027 코드값, (b) `tb_leave_type_mgmt` 실데이터(시드 6종 + 사용자 채번 행), (c) `tb_leave_type_mgmt.GRANT_ASSIGN_MMDD` 컬럼 부재 vs `Attd03Mapper.xml` 참조 불일치, (d) 정책서 "약정" 표기 |
| 점검 방식 | READ-ONLY. SELECT / SHOW / information_schema 조회만 수행. DB INSERT/UPDATE/DELETE/DDL 일절 미수행. |
| 점검 도구 | `mysql` CLI (host=127.0.0.1:3306, user=dev_prafta, db=prafta, MySQL 8.0.42) + Grep + schema-full.sql 대조 |
| 사전 정독 | `attd/08-leave.md` §8.1.1 §8.1.2 §8.5.5 / `prafta-018.sql` 섹션 8 주석 / `prafta-018-stage2-handover.md` §2 / `policies/README.md` 우선순위 |

> 본 보고서는 점검 결과만 기록한다. 발견된 불일치에 대한 보정 SQL은 **draft**로만 첨부하며, 실 적용은 별도 `PLNprafta-018XXX` 작업으로 분리한다.

---

## 2. 점검 결과 요약

| 분류 | 건수 | 비고 |
| --- | --- | --- |
| 정합 | 다수 | SYS021~SYS027 코드값, 시드 6종 LEAVE_NATURE_TYPE 매핑, SYSTEM_YN 표시 모두 일치 |
| 표기 불일치 (정책서 ↔ DB) | 1 | DB SYS024.02 = "특별" 인데 정책서 §8.1.1 line 14는 "약정" 표기 (§8.5.1 line 87/93은 `MANUAL_*` prefix 설명 맥락이라 별도 판단) |
| 데이터 불일치 | 0 | `LEAVE_NATURE_TYPE`은 모두 정의된 코드값('01' 또는 '02')만 사용. SYS024 미정의 값('03' 등) 0건. |
| 컬럼 불일치 | 1 | `tb_leave_type_mgmt.GRANT_ASSIGN_MMDD` 컬럼 **부재** (live DB + schema-full.sql 양쪽 모두 없음). `Attd03Mapper.xml` 4개 라인에서 참조 → 런타임 SQL 오류 가능 |
| 기타 발견 | 2 | (a) 본 작업 지시서·핸드오버 노트의 "SYS022 = LEAVE_TYPE" 기술은 실제와 다름. DB상 **SYS021 = LEAVE_TYPE(연차타입)**, **SYS022 = GRANT_TYPE(연차부여타입)**. (b) §8.1.1 주석은 `GRANT_DAYS`가 PRAFTA-017로 제거됐다고 기술하나 live DB·schema-full.sql 양쪽에 `GRANT_DAYS` 컬럼 잔존 (본 작업 핵심 범위 밖, 참고용 기록) |

---

## 3. SYS021/022/023/024/025/026/027 코드값 실측 결과

> 작업 지시서는 SYS022/023/024/025 점검을 요구하나, 지시서의 "SYS022(LEAVE_TYPE)" 가정과 DB 실측이 불일치하여 SYS021·SYS026·SYS027까지 범위를 넓혀 정합 확인.

### 3.1 `tb_syst_val_m` 마스터 (실측)

```
SYST_VAL_CD  SYST_VAL_NM            USE_YN
SYS021       연차타입               Y
SYS022       연차부여타입           Y
SYS023       연차유급구분           Y
SYS024       연차성격타입           Y
SYS025       연차사용단위           Y
SYS026       연차사용가능기간타입   Y
SYS027       연차자동부여타입       Y
```

→ **SYS021이 연차타입(LEAVE_TYPE), SYS022가 연차부여타입(GRANT_TYPE)** 임을 마스터 명칭이 명확히 확정.

### 3.2 `tb_syst_val_d` 상세 vs 정책서 명칭 비교 (실측)

| SYS_CD | D_CD | DB SYST_VAL_D_NM | 매핑 컬럼 | 정책서 명칭 (§8.1.1/§8.5.5) | 정합 |
| --- | --- | --- | --- | --- | --- |
| SYS021 | 01 | 사용자 신청 | `LEAVE_TYPE` | 사용자 신청 | OK |
| SYS021 | 02 | 관리자 부여 | `LEAVE_TYPE` | 관리자 부여 | OK |
| SYS022 | 01 | 자동부여 | `GRANT_TYPE` | 자동부여 (`GRANT_TYPE='01'`) | OK |
| SYS022 | 02 | 수동부여 | `GRANT_TYPE` | 수동부여 | OK |
| SYS023 | 01 | 유급 | `PAID_TYPE` | 유급 (`PAID_TYPE='01'`) | OK |
| SYS023 | 02 | 무급 | `PAID_TYPE` | 무급 | OK |
| SYS024 | 01 | 법정 | `LEAVE_NATURE_TYPE` | 법정 | OK |
| SYS024 | 02 | **특별** | `LEAVE_NATURE_TYPE` | **약정** (정책서 §8.1.1 본문) | **표기 불일치** |
| SYS025 | 01 | 1 | `USE_UNIT_TYPE` | 1일 | OK (라벨 단순화) |
| SYS025 | 02 | 0.5 | `USE_UNIT_TYPE` | 반일 / 0.5일 | OK |
| SYS025 | 03 | 0.25 | `USE_UNIT_TYPE` | 0.25일 | OK |
| SYS025 | 04 | 0.125 | `USE_UNIT_TYPE` | 0.125일 / 시간 | OK |
| SYS026 | 01 | 설정안함 | `AVAIL_TERM_TYPE` | (참고) 설정 안 함 | OK |
| SYS026 | 02 | 해당 년도 내 | `AVAIL_TERM_TYPE` | 해당 연도 내 | OK (라벨 표기 차) |
| SYS026 | 03 | 기간설정 | `AVAIL_TERM_TYPE` | 기간 지정 | OK (라벨 차이만) |
| SYS027 | 01 | 입사일 | `GRANT_BASE_TYPE` | 입사일 (§8.1.2) | OK |
| SYS027 | 02 | 생일 | `GRANT_BASE_TYPE` | 생일 | OK |
| SYS027 | 03 | 부여일지정 | `GRANT_BASE_TYPE` | 부여일지정 | OK |

> 비고: 작업 지시서 §"점검 항목 1"의 "SYS022 (LEAVE_TYPE): '01'=사용자신청, '02'=관리자부여" 및 "SYS023 (GRANT_TYPE)" 기술은 실제 DB와 어긋난다. 실측상 LEAVE_TYPE↔SYS021, GRANT_TYPE↔SYS022, PAID_TYPE↔SYS023, NATURE↔SYS024, USE_UNIT↔SYS025이다. `Attd03Mapper.xml` 및 schema-full.sql 컬럼 주석(`[SYS021]`~`[SYS027]`)은 올바른 매핑이라 코드 수준에서는 정합 상태.

---

## 4. tb_leave_type_mgmt 실데이터 점검

### 4.1 전체 10행 (실측)

```
CMPNY_CD  LEAVE_CD          LEAVE_NM            LEAVE_TYPE  GRANT_TYPE  PAID_TYPE  LEAVE_NATURE_TYPE  SYSTEM_YN  USE_YN
001       00013             하계휴가(4일)       01          NULL        01         01                 N          Y
001       00016             LEAVE_01            01          NULL        01         01                 N          Y
001       00017             LEAVE_ADMIN_AUTH    02          01          01         01                 N          Y
001       00018             LEAVE_ADMIN_MANUAL  02          02          01         01                 N          Y
001       SYS_ANNUAL        연차                02          01          01         01                 Y          Y
001       SYS_BIRTHDAY      생일 안식휴가       02          01          01         02                 Y          Y
001       SYS_MONTHLY       월차                02          01          01         01                 Y          Y
001       SYS_PREGRANT      일괄선부여 연차     02          01          01         01                 Y          Y
001       SYS_PROMOTION     사용촉진 연차       02          01          01         01                 Y          Y
001       SYS_TENURE_BONUS  근속가산 연차       02          01          01         01                 Y          Y
```

### 4.2 시드 6종 정책서 §8.5.5 일치 검증

| LEAVE_CD | LEAVE_NM | LEAVE_TYPE | GRANT_TYPE | PAID_TYPE | DB NATURE | 정책서 §8.5.5 NATURE | 정합 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SYS_ANNUAL | 연차 | 02 | 01 | 01 | 01 (법정) | 01 (법정) | OK |
| SYS_MONTHLY | 월차 | 02 | 01 | 01 | 01 (법정) | 01 (법정) | OK |
| SYS_TENURE_BONUS | 근속가산 연차 | 02 | 01 | 01 | 01 (법정) | 01 (법정) | OK |
| SYS_PROMOTION | 사용촉진 연차 | 02 | 01 | 01 | 01 (법정) | 01 (법정) | OK |
| SYS_PREGRANT | 일괄선부여 연차 | 02 | 01 | 01 | 01 (법정) | 01 (법정) | OK |
| SYS_BIRTHDAY | 생일 안식휴가 | 02 | 01 | 01 | 02 (특별) | 02 (정책서 본문 "특별") | OK |

- 공통 코드값 `LEAVE_TYPE='02'`(관리자 부여), `GRANT_TYPE='01'`(자동), `PAID_TYPE='01'`(유급) 6행 모두 정책서 §8.5.5 후반부 가이드와 일치.
- 6행 모두 `SYSTEM_YN='Y'`. PLNprafta-018002 시드 작업 결과가 정확히 반영됨.

### 4.3 시드 외 사용자 채번 행 검증

- 4건(`00013`, `00016`, `00017`, `00018`) 모두 `SYSTEM_YN='N'`. 정상.
- `LEAVE_TYPE='01'`(사용자 신청)인 `00013`, `00016`은 `GRANT_TYPE=NULL`. 스키마상 `GRANT_TYPE`이 `DEFAULT NULL`(SYS022, char(2))이므로 위반 아님. **PLNprafta-018002 가드 로직에서 NULL 분기를 누락하지 않도록 주의** 권고.

### 4.4 `LEAVE_NATURE_TYPE` 값 분포 (SYS024 정의 외 값 점검, 실측)

```
LEAVE_NATURE_TYPE  SYS024 D_NM  CNT
01                 법정         9
02                 특별         1
```

- '03' 등 SYS024 미정의 값 0건. 데이터 정합성 OK.

---

## 5. GRANT_ASSIGN_MMDD 컬럼 점검

### 5.1 DB 실 컬럼 (`SHOW COLUMNS FROM tb_leave_type_mgmt LIKE 'GRANT%'`, 실측)

```
Field               Type              Null  Key  Default  Extra
GRANT_TYPE          char(2)           YES        NULL
GRANT_DAYS          tinyint unsigned  YES        NULL
GRANT_BASE_TYPE     varchar(2)        YES        NULL
GRANT_OFFSET_MONTH  tinyint unsigned  YES        NULL
```

추가 확인: `information_schema.COLUMNS`에서 `COLUMN_NAME='GRANT_ASSIGN_MMDD'` 조회 시 **0행** 반환 → 컬럼 부재 확정.

### 5.2 schema-full.sql 스냅샷 대조

- `schema-full.sql` line 355~390 `CREATE TABLE tb_leave_type_mgmt`에 `GRANT_OFFSET_MONTH`(line 376) 다음이 곧바로 `APRV_USE_YN`(line 377)이며 `GRANT_ASSIGN_MMDD` **없음**.
- → 스냅샷과 live DB가 **동일하게 컬럼 부재**. "스냅샷 stale" 가설 기각.

### 5.3 `Attd03Mapper.xml` 참조 위치 (grep 결과)

| 라인 | 위치/맥락 | 사용 형태 |
| --- | --- | --- |
| 28 | `updateLeaveType` `INSERT INTO ... ON DUPLICATE KEY UPDATE` 컬럼 목록 | `, GRANT_ASSIGN_MMDD` |
| 63 | 동 `VALUES (...)` 바인딩 | `, #{grantAssignMmdd}   -- GRANT_ASSIGN_MMDD` |
| 116 | `selectLeaves` 외부 SELECT alias | `, A.GRANT_ASSIGN_MMDD AS grantAssignMmdd` |
| 157 | 동 내부 서브쿼리 SELECT 컬럼 | `, A.GRANT_ASSIGN_MMDD` |

### 5.4 결론

- 진단: **(b) 컬럼 부재 + mapper.xml 선반영** 확정. (a) "컬럼 실재 + 스냅샷 stale"은 §5.1/§5.2 실측으로 명확히 기각.
- 영향: `updateLeaveType`(INSERT) 및 `selectLeaves`(SELECT) 실행 시 `ERROR 1054 (42S22): Unknown column 'GRANT_ASSIGN_MMDD'` 발생 가능. attd03 연차 타입 등록/조회 경로가 현 상태로 깨졌을 가능성.
- 정책서 §8.1.2 자동부여 규칙 표는 기준일=`03`(부여일지정)에서 `GRANT_ASSIGN_MMDD`(MMDD 4자리)를 필수 컬럼으로 명시하나, 단계 1 DDL(`prafta-018.sql`)에서도 추가되지 않아 마땅히 있어야 할 컬럼이 누락된 상태.
- 본 작업은 점검만 — mapper.xml/자바 코드/DDL 수정은 수행하지 않음. §5.5 보정 SQL은 draft.

### 5.5 보정 SQL draft (별도 PLNprafta-018XXX로 분리 권장, 본 작업에서 적용 금지)

```sql
-- (DRAFT) PRAFTA-018 누락 컬럼 보강: tb_leave_type_mgmt.GRANT_ASSIGN_MMDD
-- 위치: GRANT_OFFSET_MONTH 다음
-- 정책서 §8.1.2: 자동부여 기준일='03' 부여일지정 시 MMDD 4자리 (예: '0301' = 3월 1일)
-- 형식 검증: ^(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])$  / 02/29 입력 허용, 평년 fallback은 스케줄러 책임
ALTER TABLE `tb_leave_type_mgmt`
  ADD COLUMN `GRANT_ASSIGN_MMDD` char(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL
    COMMENT '자동부여 MMDD (기준일=03 부여일지정 시 필수)'
    AFTER `GRANT_OFFSET_MONTH`;
```

> 적용 후 §8.1.2 cross-field validation(기준일=`03`이면 MMDD 필수+OFFSET NULL, `01`/`02`이면 반대)이 `LeaveTypeRequest`/`LeaveTypeCommand`/`LeaveTypeParam`/프론트 `LeaveTypeCreatePop.vue`에 이미 반영되어 있는지 별도 점검 필요.

---

## 6. 정책서 "약정" 표기 점검

### 6.1 Grep 결과 (`.claude/context/policies/attd/` 하위 `약정` 출현, `_audit` 제외)

| 파일 | 라인 | 문맥 |
| --- | --- | --- |
| `08-leave.md` | 14 | `| 휴가성격 | 법정 / 약정 |` (§8.1.1 구성 속성 표 — SYS024 직접 매핑) |
| `08-leave.md` | 87 | `... 사용자 신청 휴가(병가/출산/교육 등) + 약정/포상휴가 등 관리자 부여 휴가 타입 정의` (§8.5.1 도메인 분리 표) |
| `08-leave.md` | 93 | `- MANUAL_* — 약정/포상 (관리자 수동 부여, PRAFTA-017 타입 기반)` (§8.5.1 GRANT_TYPE prefix 설명) |

### 6.2 DB SYS024 정의

- `SYST_VAL_D_NM` = '특별' (D_CD='02'). '약정'은 DB에 없음.

### 6.3 권고 (사용자/별도 작업 결정 사항 — 본 보고서는 권고만)

- **§8.1.1 line 14**는 SYS024 휴가성격을 직접 매핑하는 표이므로 DB 표기 "특별"에 맞추는 정정을 권장.
  - 대안 A (권장): 정책서 line 14 "약정" → "특별".
  - 대안 B: DB SYS024.02 `SYST_VAL_D_NM`을 "약정"으로 UPDATE. 화면 라벨/i18n 영향 범위가 크고 SYS_BIRTHDAY의 '02' 분류와 의미상 어색("생일=약정")하여 비권장.
  - 대안 C: 정책서 본문 병기 "법정 / 특별(약정)".
- **§8.5.1 line 87/93**의 "약정/포상"은 `MANUAL_*` GRANT_TYPE prefix 설명 맥락으로 SYS024와 직접 매핑되지 않으므로 **유지 무방**. (정책서 우선순위상 본문 수정은 사용자 권한)
- 정책서 본문 직접 수정은 본 작업 금지사항이므로 수행하지 않음.

---

## 7. 권고 조치

| # | 발견 항목 | 권고 후속 작업 | 비고 |
| --- | --- | --- | --- |
| 1 | `tb_leave_type_mgmt.GRANT_ASSIGN_MMDD` 컬럼 부재 + mapper.xml 4곳 참조 (런타임 오류 가능) | **별도 PLNprafta-018XXX**: §5.5 ALTER 적용 + `LeaveTypeRequest`/`LeaveTypeCommand`/`LeaveTypeParam`/`LeaveTypeResult`에 필드/cross-field validation 동작 검증 | 정책서 §8.1.2가 기준일=`03` 케이스를 강제하나 컬럼 부재로 영구 미지원 상태 → 우선순위 +1단계 격상 권장 |
| 2 | 정책서 §8.1.1 line 14 "약정" 표기 (DB="특별") | **별도 PLNprafta-018XXX**: 정책서 본문 "약정"→"특별" 정정 검토 (사용자 결정) | 단순 표기. 코드 변경 없음. §8.5.1 line 87/93은 유지 무방 |
| 3 | 시드 외 행 `LEAVE_TYPE='01'`+`GRANT_TYPE=NULL` 케이스 | PLNprafta-018002 가드 작성 시 NULL 분기 명시 처리 확인 | 신규 작업 분리 불필요. PLNprafta-018002 범위 내 |
| 4 | "SYS022=LEAVE_TYPE" 오기재 (작업 지시서·핸드오버 노트 본문) | 후속 분해 시 본 보고서 §3 매핑표 참조. 핸드오버 노트 부록에 정정 메모 | 정책서/코드 영향 없음. 문서 명확성만 |
| 5 | §8.1.1 주석 "GRANT_DAYS PRAFTA-017로 제거" vs live DB·snapshot에 잔존 | (참고) 본 작업 범위 밖. 별도 확인 필요 시 분해 | GRANT_DAYS는 NULL 허용 컬럼으로 잔존. 데이터/코드 오류는 아님 |

### 7.1 본 작업에서 작성한 보정 SQL draft 요약

- §5.5 `ALTER TABLE tb_leave_type_mgmt ADD COLUMN GRANT_ASSIGN_MMDD char(4) ...` 1건만 draft. **실 적용은 별도 작업에서 트랜잭션·롤백·validation 동작 검증 포함 재검토 후 수행**.

---

## 8. 검증 명령 (재실행용)

모두 read-only. Windows + Git Bash, MySQL 8.0.42 기준.

```bash
# 8.1 SYS021~SYS027 마스터
mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' --default-character-set=utf8mb4 --batch prafta -e \
"SELECT SYST_VAL_CD, SYST_VAL_NM, USE_YN FROM tb_syst_val_m WHERE SYST_VAL_CD IN ('SYS021','SYS022','SYS023','SYS024','SYS025','SYS026','SYS027') ORDER BY SYST_VAL_CD;"

# 8.2 SYS021~SYS027 상세
mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' --default-character-set=utf8mb4 --batch prafta -e \
"SELECT SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, USE_YN FROM tb_syst_val_d WHERE SYST_VAL_CD IN ('SYS021','SYS022','SYS023','SYS024','SYS025','SYS026','SYS027') ORDER BY SYST_VAL_CD, SYST_VAL_D_CD;"

# 8.3 tb_leave_type_mgmt 실데이터
mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' --default-character-set=utf8mb4 --batch prafta -e \
"SELECT CMPNY_CD, LEAVE_CD, LEAVE_NM, LEAVE_TYPE, GRANT_TYPE, PAID_TYPE, LEAVE_NATURE_TYPE, SYSTEM_YN, USE_YN FROM tb_leave_type_mgmt ORDER BY CMPNY_CD, LEAVE_CD;"

# 8.4 LEAVE_NATURE_TYPE 분포 + SYS024 명칭 조인
mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' --default-character-set=utf8mb4 --batch prafta -e \
"SELECT t.LEAVE_NATURE_TYPE, d.SYST_VAL_D_NM, COUNT(*) AS CNT FROM tb_leave_type_mgmt t LEFT JOIN tb_syst_val_d d ON d.SYST_VAL_CD='SYS024' AND d.SYST_VAL_D_CD = t.LEAVE_NATURE_TYPE GROUP BY t.LEAVE_NATURE_TYPE, d.SYST_VAL_D_NM ORDER BY t.LEAVE_NATURE_TYPE;"

# 8.5 GRANT% 컬럼 부재 확인
mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' --default-character-set=utf8mb4 --batch prafta -e \
"SHOW COLUMNS FROM tb_leave_type_mgmt LIKE 'GRANT%';"

# 8.6 GRANT_ASSIGN_MMDD 존재 여부 (information_schema, 0행 기대)
mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' --default-character-set=utf8mb4 --batch prafta -e \
"SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tb_leave_type_mgmt' AND COLUMN_NAME='GRANT_ASSIGN_MMDD';"
```

Grep 재현:

```
# 정책서 "약정" 표기
Grep pattern="약정" path=".claude/context/policies/attd" -n

# mapper.xml GRANT_ASSIGN_MMDD 참조
Grep pattern="GRANT_ASSIGN_MMDD" path="prafta-backend/src/main/resources/com/prafta/web/attd/attd03/mapper/Attd03Mapper.xml" -n

# schema-full.sql tb_leave_type_mgmt DDL 대조 (GRANT_ASSIGN_MMDD 없음 확인)
Grep pattern="GRANT_ASSIGN_MMDD|GRANT_OFFSET_MONTH" path=".claude/context/schema-full.sql" -n
```

---

## 9. 점검자 메모 (참고)

- 본 점검 중 DB INSERT/UPDATE/DELETE/DDL은 단 1건도 수행하지 않음. 모든 명령은 SELECT/SHOW/information_schema로 한정.
- 정책서 본문 / 자바 코드 / mapper.xml 직접 수정 없음. 본 보고서 작성만 산출물.
- 2026-05-20자 초판 보고서가 동일 위치에 존재했으며, 본 2026-05-21 재측정에서 모든 핵심 수치(코드값, 시드 6종, 분포, 컬럼 부재)가 변동 없이 재확인되어 갱신 작성함.
- SYS022/SYS021 매핑 혼동(작업 지시서·핸드오버 노트 본문)은 코드 영향이 없으므로 코드 수정은 권고하지 않음. 후속 분해 시 §3 매핑표를 참조하기를 권장.
