# PLNprafta-018004 developer 위임 prompt (전문)

용도: 새 세션에서 Agent 도구 호출 시 `prompt` 파라미터에 그대로 복붙.
호출 시 함께 지정할 값:
- `subagent_type`: `developer`
- `description`: `PLNprafta-018004 점검 보고서`

이전 세션에서 동일 prompt로 위임 시도했으나 API 529 Overloaded로 실패. 본 prompt 자체에는 결함 없음.

---

## (이하 위임 prompt 본문 — 그대로 복사하여 사용)

## 작업

**PLNprafta-018004** — SYS022/023/024 + `tb_leave_type_mgmt` 매핑 + GRANT_ASSIGN_MMDD 정합성 **READ-ONLY 점검** 후 마크다운 보고서 작성.

Notion 작업 로그: https://www.notion.so/3664cf4dc46081b09ed6ea160aaa9572 (상태: 개발중)

## 작업 성격

- **READ-ONLY**: DB SELECT만. INSERT/UPDATE/DELETE 금지.
- **산출물**: 마크다운 보고서 1개 + 디렉토리 1개 (신규)
- 데이터 불일치 발견 시 보정 SQL **draft만** 작성 (보고서 본문 첨부), 실 적용 금지. 별도 PLNprafta-018XXX 작업으로 분리.

## 사전 정독 필수

1. **핸드오버 노트**: `C:\prafta\.claude\requests\prafta-018-stage2-handover.md` §2 PLNprafta-018004 항목
2. **정책서**: `C:\prafta\.claude\context\policies\attd\08-leave.md`
   - **§8.1.1** 휴가성격 표기 (법정/약정)
   - **§8.5.5** 시스템 LEAVE_CD 시드 6종 + LEAVE_NATURE_TYPE 매핑 ('01' 법정, SYS_BIRTHDAY만 '02')
3. **단계 1 SQL**: `C:\prafta\.claude\requests\prafta-018.sql` (특히 섹션 8 주석 — 단계 2 점검 가이드, "SYS024 '약정/특별' 표기 정합성 일괄 점검" 명시)
4. **이전 보안 검토 결과**: PLNprafta-018002 보안 검토에서 발견된 `GRANT_ASSIGN_MMDD` 컬럼 부재 vs `Attd03Mapper.xml` 참조 불일치 이슈 — 본 작업에서 함께 점검.

## 점검 항목

### 1. SYS022 / SYS023 / SYS024 / SYS025 코드값 점검

MCP MySQL 미가용 시 `Bash(mysql *)`로 직접:
```
mysql -h127.0.0.1 -P3306 -udev_prafta -p'prafta12345!' --default-character-set=utf8mb4 --batch prafta -e "SELECT SYST_VAL_CD, SYST_VAL_D_CD, SYST_VAL_D_NM, USE_YN FROM tb_syst_val_d WHERE SYST_VAL_CD IN ('SYS022','SYS023','SYS024','SYS025') ORDER BY SYST_VAL_CD, SYST_VAL_D_CD;"
```

검증:
- SYS022 (LEAVE_TYPE): '01'=사용자신청, '02'=관리자부여 (또는 정책서 §8.1.1과 일치하는 명칭)
- SYS023 (GRANT_TYPE): '01'=자동부여, '02'=수동부여 등 — 단계 1 시드 코드값 `GRANT_TYPE='01'`과 정합 여부
- SYS024 (LEAVE_NATURE_TYPE): '01'=법정 / '02'=특별 — 정책서 §8.1.1 "법정/약정" 표기와 일치 여부 ("약정" → "특별"로 정정 필요한지)
- SYS025 (USE_UNIT_TYPE): '01'=1일, '02'=0.5일, '03'=0.25일, '04'=0.125일

### 2. tb_leave_type_mgmt 실데이터 점검

```
SELECT CMPNY_CD, LEAVE_CD, LEAVE_NM, LEAVE_TYPE, GRANT_TYPE, PAID_TYPE, LEAVE_NATURE_TYPE, SYSTEM_YN, USE_YN FROM tb_leave_type_mgmt;
```

검증:
- 단계 1 시드 6종(`SYS_*`)이 정확히 INSERT됐는지 (PLNprafta-018002 결과)
- LEAVE_NATURE_TYPE 값이 SYS024 정의된 코드값과 일치 ('01' 또는 '02')
- 시드 외 사용자 채번 행이 SYSTEM_YN='N'인지

### 3. GRANT_ASSIGN_MMDD 컬럼 부재 vs Attd03Mapper.xml 참조 불일치

```
mysql ... -e "SHOW COLUMNS FROM tb_leave_type_mgmt LIKE 'GRANT%';"
```

`prafta-backend/src/main/resources/com/prafta/web/attd/attd03/mapper/Attd03Mapper.xml`에서 GRANT_ASSIGN_MMDD 참조 줄을 grep으로 추출. DB 실 컬럼명과 비교.

- 컬럼이 실제로 부재면 mapper.xml 수정 또는 컬럼 추가 마이그레이션 SQL **draft** 작성.
- 단순 schema-full.sql stale 이슈인지(컬럼이 실재하나 스냅샷이 stale) 구분.

### 4. 정책서 §8.1.1 "약정" vs "특별" 표기 점검

`.claude/context/policies/attd/` 디렉토리의 모든 .md 파일에서 "약정" 단어 grep. 발견 시 위치 정리.

## 산출물

### A. 보고서

**경로**: `C:\prafta\.claude\context\policies\attd\_audit\prafta-018-syst-val-audit.md`

**구조**:
```markdown
# PRAFTA-018 단계 2 — SYS022/023/024 + tb_leave_type_mgmt 정합성 점검 보고서

## 1. 점검 개요
- 작업 ID, 작성일, 점검 범위, 점검 방식

## 2. 점검 결과 요약
- 정합 / 표기 불일치 / 데이터 불일치 / 컬럼 불일치 항목 카운트

## 3. SYS022/023/024/025 코드값 실측 결과
- 4개 SYS 코드별 표 (`SYST_VAL_D_CD` / `SYST_VAL_D_NM` / 정책서 명칭 비교)

## 4. tb_leave_type_mgmt 실데이터 점검
- 시드 6종 + 사용자 채번 행 결과
- LEAVE_NATURE_TYPE 분포

## 5. GRANT_ASSIGN_MMDD 컬럼 점검
- DB 실 컬럼 SHOW COLUMNS 결과
- mapper.xml 참조 줄 grep 결과
- 결론: (a) 컬럼 실재 → schema-full.sql stale (b) 컬럼 부재 → 수정 필요

## 6. 정책서 "약정" 표기 점검
- grep 결과 (파일/줄/문맥)
- 권고: "특별"로 정정 vs "약정"이 의도된 표기인지 사용자 결정 요청

## 7. 권고 조치
- 발견된 불일치별 후속 작업 (별도 PLNprafta-018XXX로 분리 권장 리스트)
- 보정 SQL draft (있다면 코드 블록)

## 8. 검증 명령 (재실행용)
- 모든 SELECT 명령 그대로 첨부
```

### B. 디렉토리

`C:\prafta\.claude\context\policies\attd\_audit\` 신규 생성 (보고서 저장 위치).

## 절대 금지 사항

- DB INSERT/UPDATE/DELETE 일절 금지.
- 정책서 본문 직접 수정 금지 (점검만 — 보고서에서 수정 권고만).
- 자바/SQL 코드 변경 금지 (mapper.xml `GRANT_ASSIGN_MMDD` 발견 시도 본 작업에서 수정 X, 별도 작업 분리 권고).
- 추정/추측으로 항목 채우지 마세요. 실측만 적습니다.
- `${...}` 바인딩 사용 검토 시도 금지 (본 작업은 점검만).

## 완료 후 보고 형식

```
## 완료 보고: PLNprafta-018004

### 보고서 위치
- (절대 경로)

### 점검 결과 요약
- 정합: N건 / 표기 불일치: N건 / 데이터 불일치: N건 / 컬럼 불일치: N건

### 후속 조치 권고
- (별도 PLNprafta-018XXX로 분리할 항목 리스트)

### 보정 SQL draft (있다면)
- (코드 블록 또는 "없음")

### 검증 결과
- 보고서 markdown 작성: PASS
- DB INSERT/UPDATE/DELETE 미수행: PASS
```

질문이 있으면 즉시 보고하고 진행 중단.
