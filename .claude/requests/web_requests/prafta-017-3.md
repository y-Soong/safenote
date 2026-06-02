## prafta-017-3 — 공통코드(TB_SYST_VAL_M/D) 미등록 타입 정리 (향후 작업 리스트업)

> 작성일: 2026-05-21
> 배경: prafta-017-2 진행 중 `tb_user_leave_grant.GRANT_BY_TYPE`가 공통코드(TB_SYST_VAL) 없이
>       문자열('AUTO'/'ADMIN')로 쓰일 뻔한 케이스 발견 → **SYS043(연차 부여 방식)** 신설 후 코드값('01'/'02') 사용으로 정리함.
> 본 문서는 "공통으로 쓰는 타입인데 TB_SYST_VAL_M/D에 담지 않고 하드코딩한" **유사 케이스 후보**를 모아둔 것이다.
> 실제 정리(SYS 신설 + 코드 치환)는 **별도 요청 시** 진행한다.

### 스코프 / 신뢰도
- 본 리스트는 **연차/근태 정책 도메인**(prafta-017/018에서 구축한 `common/cmm/leave`, `web/baim/baim07`, `web/baim/baim08`) 중심이다. 전 모듈 전수 감사는 아니며, 필요 시 별도 패스 권장.
- 분류 기준: "여러 곳에서 의미가 고정된 값으로 쓰이는 타입(enum 성격)"인데 TB_SYST_VAL_M에 마스터가 없는 것.

---

### A. 정리 권고 후보 (SYS 마스터 없음 → 하드코딩)

| # | 대상(컬럼/용도) | 현재 값 | 사용 위치 | SYS 유무 | 권고 |
| --- | --- | --- | --- | --- | --- |
| A-1 | `TB_LEAVE_POLICY.AXIS5_TENURE_MODE` (근속가산 모드) | `LEGAL` / `CUSTOM` | `LeavePolicyServiceImpl.java:70-71,417-423` 상수+검증, `Baim_07.vue`(라디오) | **없음** | 신규 SYS(예: "연차 정책 AXIS5 모드") 신설 후 코드 치환. **AXIS1=SYS036 / AXIS3=SYS037 / AXIS4=SYS038은 이미 SYS화됐는데 AXIS5만 누락**되어 일관성 결여(우선순위↑). |
| A-2 | `TB_LEAVE_POLICY_HISTORY.CHANGE_TYPE` (정책 변경 유형) | `CREATE` / `UPDATE` (CCI는 `PRESET_CHANGE`도 언급) | `LeavePolicyServiceImpl.java:95-96,112,121,263` | **없음** | 신규 SYS(예: "연차 정책 변경 유형") 신설 권고. 이력/감사 컬럼이라 코드화 가치 있음. |
| A-3 | `TB_LEAVE_POLICY.POLICY_PRESET` (정책 프리셋) | `CUSTOM` (그 외 `HIRE_DATE`/`FISCAL_PRORATE`/`FISCAL_MONTHLY`/`HIRE_DATE_PREGRANT` 정의됨) | `LeavePolicySaveParam.java:46`(현재 "CUSTOM" 고정), 컬럼 정의 | **없음** | 통합 화면(Baim_07)에서 프리셋 제거되어 현재 'CUSTOM' 고정. (a) SYS 신설하여 코드화 또는 (b) 컬럼 자체를 deprecate/제거 검토. 우선순위↓. |

### B. 경계 사례 (검토 필요, 필수 아님)

| # | 대상 | 현재 값 | 비고 |
| --- | --- | --- | --- |
| B-1 | 영향분석 diff `changeType` (응답 DTO) | `CHANGED`/`DEACTIVATED`/`ACTIVATED`/`UNCHANGED` | `LeavePolicyServiceImpl.java` buildDiff. **DB 저장값이 아니라 API 응답 전용 enum**이라 공통코드 대상으로는 약함. 프론트와의 계약 상수로 관리해도 무방. |
| B-2 | 대시보드 `natureBadge` | `LEGAL`/`NON_LEGAL` | `LeaveDashboardServiceImpl` 응답 라벨(파생값, 저장 안 함). GRANT_TYPE prefix(SYS035)에서 유도. SYS 불필요. |

### C. 미구축 도메인 — 향후 구축 시 SYS 정합 주의 (참고)

| # | 대상 | 비고 |
| --- | --- | --- |
| C-1 | `TB_USER_LEAVE_USE.USE_UNIT` (사용 단위) | 향후 연차 사용/신청 도메인 구축 시 `SYS025(연차사용단위, 01/02/03/04)`와 정합 필요. CCI는 `FULL_DAY/HALF_DAY/QUARTER_DAY/HOURLY` 문자열을 쓰는데 실제는 SYS025 코드 → **CCI 문자열을 그대로 쓰지 말 것**. (HANDOFF "추정 잔존 항목"의 LEAVE_MINUTES 환산/USE_UNIT 관련) |
| C-2 | `TB_USER_LEAVE_USE.LEAVE_STATUS` 등 사용 상태 | HANDOFF에서 "LEAVE_STATUS 카탈로그 미정"으로 표기됨. 사용 도메인 구축 시 SYS 신설 또는 기존 코드 매핑 확정 필요. |

---

### D. 이미 SYS화되어 정합 양호 (참고 — 정리 불필요)

prafta-018에서 아래는 이미 TB_SYST_VAL에 등록되어 코드/스키마와 정합함:
- `SYS035` 연차 부여 분류 (`STATUTORY_*`/`MANUAL_*` — tb_user_leave_grant.GRANT_TYPE)
- `SYS036` AXIS1 (HIRE_DATE/FISCAL_YEAR), `SYS037` AXIS3 (MONTHLY_ONLY/PRORATE/NEXT_YEAR_BULK), `SYS038` AXIS4 (CEIL/ROUND/FLOOR/HALF_DAY)
- `SYS039` 입사일 변경 처리 방식, `SYS040` 연차 부여 상태 (ACTIVE/EXHAUSTED/EXPIRED/CANCELED)
- `SYS041` 고용 형태, `SYS042` 경력 인정 사유
- `SYS021~027` 연차타입/부여타입/유급구분/성격타입/사용단위/사용가능기간/자동부여타입
- **`SYS043` 연차 부여 방식 (01 자동 / 02 관리자수동)** — prafta-017-2에서 신설(GRANT_BY_TYPE 코드화 완료)

---

### E. 추가 권고
- `tb_user_leave_grant.GRANT_BY_TYPE` 컬럼 주석이 아직 "AUTO/ADMIN" 문자열을 명시 → `[SYS043]` 참조로 주석 갱신 권고(컬럼 COMMENT 변경, 별도 DDL).
- 전 모듈 전수 감사를 원하면, 각 `tb_*` 테이블의 "*_TYPE / *_STATUS / *_GUBUN" 류 컬럼을 TB_SYST_VAL_M 마스터 목록과 대조하는 별도 작업으로 분리 권장.
