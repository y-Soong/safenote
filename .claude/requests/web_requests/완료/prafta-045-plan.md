# prafta-045 작업 분해 (planner) — 연차 타입 "사용 가능 기간" enforcement

> 분해 담당: planner 서브에이전트 (Notion 미접근). 본 문서가 단일 출처.
> 영역: 백엔드(`PRAFTA/prafta-backend`) — `common.cmm.leave` 수동부여 경로 중심.
> 정책 출처: 근태 정책서 §8.1.1(사용 가능 기간 속성)·§8.5.2~8.5.4(AXIS6 법정 전용)·§8.5.5(시스템 시드)·§8.5.8(멱등·기부여보호).

---

## 0. 요약 (결론 먼저)

- **진짜 갭은 단 한 곳**: `LeaveDashboardServiceImpl.manualGrant`(관리자 수동 부여)가 부여건의 `AVAIL_TO_DATE`를 **타입의 사용가능기간 설정이 아니라 회사 공통 AXIS6(validityMonths)** 로 계산한다(line 348~350). 타입 `AVAIL_TERM_TYPE`/`ADMIN_AVAIL_TERM_TYPE`(SYS026 01/02/03 + from/to)이 부여 유효일에 전혀 흐르지 않는다.
- **법정 부여엔진(`LeaveGrantEngineServiceImpl`)은 손대지 않는다.** 이 엔진은 전부 `STATUTORY_*`(SYS_ANNUAL/SYS_MONTHLY/SYS_TENURE_BONUS)만 INSERT하며, `TB_LEAVE_TYPE_MGMT.AVAIL_TERM_TYPE`을 읽지도 않는다. AXIS6 / 월차 만1년 일괄소멸(§8.5.4)은 법정 의무라 타입 avail-term 적용 대상이 아니다(아래 §2 회귀선).
- **소비창은 이미 enforce 중**: web/app `selectDeductibleGrant`가 `AVAIL_FROM_DATE <= workYmd AND AVAIL_TO_DATE >= workYmd`로 이미 창을 강제한다. → **결정포인트 5는 "추가 작업 없음"이지만, 이 때문에 `01`(무기한)=`AVAIL_TO_DATE=null` 처리에 치명적 부작용이 있다(아래 §3 Critical).**
- **자동부여 경로**: 관리자 부여 타입(`LEAVE_TYPE='02' + GRANT_TYPE='01'`)의 "자동부여"는 `TB_LEAVE_POLICY` 법정 엔진과 별개로 **현재 구현된 부여 잡이 없다**(시드 SYS_*만 자동 부여됨). 즉 비법정 관리자 타입의 자동부여 실행 코드는 존재하지 않는다 → **본 작업 범위에서 제외**(구현 대상 없음). 결정포인트 1의 "관리자 자동부여"는 실체가 없으므로 적용 불가.

---

## 1. 부여 경로별 현황 조사 결과 (결정포인트 1 — 직접 확인)

| 부여 경로 | 코드 위치 | GRANT_TYPE | 현재 AVAIL_FROM_DATE | 현재 AVAIL_TO_DATE | 타입 avail-term 반영? |
| --- | --- | --- | --- | --- | --- |
| 관리자 **수동부여** | `LeaveDashboardServiceImpl.manualGrant` (L305~397) | `MANUAL_OTHER` | 폼 입력 `availFromDate`(필수, YYYYMMDD) | `availFromDate + AXIS6(validityMonths)` (L348~350) | ❌ **안 됨 (갭)** |
| 정책 **부여엔진** | `LeaveGrantEngineServiceImpl.hireDateGrant` / `grantComponent`(L1479~) / `computeMonthlyPeriods` | `STATUTORY_ANNUAL/MONTHLY/TENURE_BONUS` | 본연차=발생일, 월차=입사+m개월 | 본연차·가산=발생일+AXIS6, 월차=만1년 도래일(§8.5.4) | ❌ (의도적, 법정 전용 — **유지**) |
| 입사일변경 수동조정 | `LeaveGrantEngineServiceImpl.adjustStatutoryGrantsByHireDateChange`(L382~) | `STATUTORY_*` | 발생일/오늘 | 발생일+AXIS6 | ❌ (법정 전용 — **유지**) |
| 관리자 **자동부여**(비법정) | **없음** (미구현) | — | — | — | — (대상 없음) |

추가 사실:
- 수동부여 INSERT는 `LeaveDashboardMapper.insertManualGrant`(L410~454) 단일 statement를 **3개 경로가 공유**한다(수동부여 / 엔진 grantComponent / 입사일조정 insertHireAdjustGrant). VO(`LeaveGrantInsertVO`)에 `availToDate`/`availFromDate` 모두 존재. INSERT는 `#{availToDate}` 단순 바인딩이라 **null 허용**(컬럼도 nullable).
- 수동부여 경로는 현재 타입의 avail-term 컬럼을 읽는 쿼리가 **없다**(`selectManualGrantTypes`는 leaveCd/leaveNm만, `countManualGrantType`은 존재여부만). → 신규 SELECT 필요.

---

## 2. 법정 vs 비법정 분리 (결정포인트 2 — 가장 중요, 회귀선)

**확정: 타입 avail-term 산출은 관리자 수동부여(`MANUAL_*`)에만 적용한다. 법정(`STATUTORY_*`)은 절대 미적용.** 정책 근거:

- §8.5.2~8.5.4: AXIS6(`AXIS6_VALIDITY_MONTHS`)는 법정 본연차·근속가산 유효기간 전용이며, 월차는 "만 1년 도래일 일괄 소멸"(§8.5.4, 근로기준법 §60⑦)이라는 **법정 의무**다. 타입 avail-term(01/02/03)은 §8.1.1 관리자/사용자 정의 타입 속성으로, 법정 소멸 규정과 무관하다.
- §8.5.1 도메인 분리: 법정 연차(PRAFTA-018 `TB_LEAVE_POLICY` 7-axis)와 타입 정의(PRAFTA-017 `TB_LEAVE_TYPE_MGMT`)는 **분리 도메인**. 법정 부여행에 타입 avail-term을 주입하면 §8.5.2/8.5.4와 직접 충돌 → prafta-023/029/030 전수 회귀.
- 분리 기준 = `GRANT_TYPE` prefix(§8.5.1): `STATUTORY_*` = 미적용(엔진 유지), `MANUAL_*` = 타입 avail-term 적용.

> **회귀 위험 지점 (qa/security 인계)**: 어떤 변경도 `LeaveGrantEngineServiceImpl` / `computeMonthlyPeriods` / `computeBackfillPeriods` / `adjustStatutoryGrantsByHireDateChange`의 AVAIL_TO_DATE 산출에 닿으면 안 된다. 본 작업은 `LeaveDashboardServiceImpl.manualGrant`와 그 보조 SELECT만 건드린다. 엔진 테스트(`LeaveGrantEngine*Test` 5종)는 무변경 통과해야 한다(회귀 게이트).

---

## 3. 산출 규칙 (결정포인트 3) + Critical 발견

### 3-1. 타입 avail-term → 부여 유효일 산출 규칙 (MANUAL_ 전용)

수동부여는 관리자 부여 타입(`LEAVE_TYPE='02'`)이므로 **`ADMIN_AVAIL_TERM_TYPE` + `ADMIN_AVAIL_FROM_DT`/`ADMIN_AVAIL_TO_DT`(YYYYMMDD 8자, prafta-044-FU2)** 를 읽는다. (사용자신청 `AVAIL_*` MMDD 컬럼은 수동부여 경로와 무관 — 수동부여는 항상 관리자 타입.)

| ADMIN_AVAIL_TERM_TYPE (SYS026) | AVAIL_TO_DATE 산출 | AVAIL_FROM_DATE |
| --- | --- | --- |
| `01` 설정안함 | **무기한** → 아래 §3-2 Critical 참조 (단순 null 금지) | 폼 입력 유지 |
| `02` 해당 연도 내 | 부여 연도(=폼 availFromDate의 YYYY)의 `YYYY1231` | 폼 입력 유지 |
| `03` 기간 설정 | `ADMIN_AVAIL_TO_DT`(YYYYMMDD 절대일) | §3-3 결정 |
| (미설정/null) | **하위호환 = 기존 AXIS6 산출 유지** (폼 availFromDate + validityMonths) | 폼 입력 유지 |

- `02` "부여 연도" 기준 = 폼 `availFromDate`의 연도(부여 시점이 아니라 사용시작일 연도). from이 12/31 이후로 잡히는 일은 없으니 from<=to 자연 성립. (대안: 부여실행일 today 연도. → **채팅 확인 필요**, 아래 §7.)
- `03` from/to는 prafta-044 저장 시 `from<=to`/실존일 검증 완료. 산출에서 재검증은 방어적으로 1회만.

### 3-2. ⚠️ Critical — `01` 무기한 = AVAIL_TO_DATE null 이 소비창을 깬다

소비 차감 쿼리(`selectDeductibleGrant`, web+app)는 `AVAIL_TO_DATE >= #{workYmd}` 조건을 건다. SQL에서 `NULL >= x = NULL(거짓)` 이므로 **AVAIL_TO_DATE=null 부여건은 영원히 차감 후보에서 제외 = 사용 불가**. "무기한"의 정반대 결과.

만료배치/대시보드는 null 안전(확인 완료):
- `expireOverdueGrants`: `AVAIL_TO_DATE < DATE_FORMAT(NOW())` → NULL은 거짓, 만료 안 됨 ✅
- `isExpired`(Java): null/8자 아님 → false ✅
- `legalNearestExpire`: STATUTORY 한정 MIN이라 MANUAL null과 무관 ✅
- `expiringSoon30`: BETWEEN → NULL 제외 ✅

→ **유일한 깨짐은 소비창**. 두 가지 대안 중 택1 (**채팅 확인 필요, §7**):

- **(A) 권장 — sentinel 종료일**: `01` 무기한을 `AVAIL_TO_DATE = '99991231'`(먼 미래)로 INSERT. 소비창/만료/대시보드 전부 기존 로직 그대로 안전하게 통과(코드 변경 0, SQL 변경 0). null의 의미적 순수성은 잃지만 운영 안전.
- **(B) null + 소비쿼리 수정**: `AVAIL_TO_DATE=null` INSERT + web/app `selectDeductibleGrant`를 `(G.AVAIL_TO_DATE IS NULL OR G.AVAIL_TO_DATE >= #{workYmd})`로 수정. 의미는 깨끗하나 **소비 SQL 2곳을 건드려 회귀면이 넓어짐**(법정 차감도 같은 쿼리를 탄다 — 정상 동작엔 영향 없으나 qa 부담↑).

> planner 권장 = **(A) sentinel**. 법정 차감 SQL을 안 건드려 회귀선 보존. 단 sentinel 값/표시 정책은 사용자 확인.

### 3-3. AVAIL_FROM_DATE 결정 (결정포인트 4)

- 현행: 수동부여 폼의 `availFromDate`(필수, YYYYMMDD)를 AVAIL_FROM_DATE로 직접 사용.
- 본 작업: **AVAIL_FROM_DATE = 폼 입력 그대로 유지**, **AVAIL_TO_DATE만 타입 avail-term으로 재산출**. 폼 입력 의미를 바꾸지 않는다(최소 변경 원칙).
- 예외 후보: `03` 기간설정에서 타입에 `ADMIN_AVAIL_FROM_DT`도 있으니 from도 타입값으로 덮을지 → **채팅 확인 필요(§7)**. planner 잠정안: from은 폼 우선(관리자가 "언제부터 쓸지"를 부여 시점에 정하는 게 자연스러움), 타입 from은 무시. 단 `from(폼) > to(타입)` 모순 시 검증 거부.

---

## 4. 소비창 enforce (결정포인트 5)

- **추가 enforce 불필요.** web/app `selectDeductibleGrant`가 이미 `AVAIL_FROM_DATE <= workYmd AND AVAIL_TO_DATE >= workYmd`로 창 밖 신청을 자연 차단한다(차감할 부여건이 안 잡히면 잔여부족 에러로 귀결).
- 유일 작업은 §3-2 Critical의 null/sentinel 처리뿐. (B)안 선택 시에만 소비 SQL 2곳 수정이 본 결정포인트에 포함된다.

---

## 5. 백필/마이그레이션 (결정포인트 6)

- **신규 부여분만 적용. 기존 부여건 백필 없음.** 컬럼(`AVAIL_FROM_DATE`/`AVAIL_TO_DATE`) 모두 기존재 → **DDL 마이그레이션 없음**.
- (A)안 채택 시에도 sentinel은 신규 INSERT에만 적용, 기존 행 UPDATE 없음.

---

## 6. 작업 단위 분해

### PRAFTA-045-1 [backend] 수동부여 AVAIL_TO_DATE를 타입 avail-term으로 산출 (핵심)
- **영역**: web (common.cmm.leave) / **모듈**: cmm/leave / **작업유형**: 보완
- **요구사항 요약**: 관리자 수동부여 시 부여건 AVAIL_TO_DATE를 회사 공통 AXIS6가 아니라 해당 타입의 `ADMIN_AVAIL_TERM_TYPE`(01/02/03)에 따라 산출.
- **상세 설명**:
  - 정책 출처: §8.1.1(사용 가능 기간), §8.5.1(도메인 분리), §8.5.8(멱등/기부여보호 — 산식만 바뀌고 멱등키/INSERT 불변).
  - 핵심 요구사항:
    1) `manualGrant`에서 leaveCd로 타입의 `ADMIN_AVAIL_TERM_TYPE` + `ADMIN_AVAIL_FROM_DT`/`ADMIN_AVAIL_TO_DT`를 조회하는 신규 SELECT 추가(`LeaveDashboardMapper`).
    2) avail-term 코드별 AVAIL_TO_DATE 산출(§3-1): `01`→sentinel(§3-2 결정), `02`→YYYY1231, `03`→ADMIN_AVAIL_TO_DT, null→기존 AXIS6 폴백.
    3) 산출 로직은 `MANUAL_*` 경로에만. 엔진/입사일조정 경로는 절대 미변경(§2).
    4) `from(AVAIL_FROM_DATE) > to(AVAIL_TO_DATE)` 모순 방어 검증(거부 → ATTD_400_032 재사용 또는 신규 코드).
  - 영향 파일:
    - `LeaveDashboardServiceImpl.java` (manualGrant L305~397: validityMonths 산출부 L348~350 교체)
    - `LeaveDashboardMapper.java` / `LeaveDashboardMapper.xml` (타입 avail-term 조회 SELECT 신규)
    - (신규) avail-term 조회 결과 VO 1개
  - endpoint: 기존 수동부여 POST(변경 없음, 내부 산출만). 컨트롤러 무변경.
  - 예상 산출물: service 수정 + mapper SELECT 신규 + VO 신규.
- **선행 작업**: 없음 (prafta-044 완료 전제)
- **우선순위 근거**: 데이터 정합성(부여 유효일) + 법정연차 도메인 인접 → 격상. 1순위.

### PRAFTA-045-2 [backend] `01` 무기한 소비창 안전 처리
- **영역**: web / **모듈**: cmm/leave / **작업유형**: 보완(버그 예방)
- **상세 설명**:
  - 정책 출처: §8.1.1(설정안함=무기한), §8.5.8.
  - 핵심 요구사항: §3-2 Critical 해소.
    - (A)안 채택 시: 045-1 산출에서 `01`→`'99991231'` sentinel. **소비/만료/대시보드 SQL 무변경**(이 작업은 045-1에 흡수, 별도 코드 없음 — 문서상 분리만).
    - (B)안 채택 시: web `LeaveFlowMapper.xml` + app `AppLeaveFlowMapper.xml`의 `selectDeductibleGrant`를 `(AVAIL_TO_DATE IS NULL OR AVAIL_TO_DATE >= #{workYmd})`로 수정.
  - 영향 파일((B)안 한정): `web/attd/leaveflow/mapper/LeaveFlowMapper.xml`, `app/leave/leaveflow/mapper/AppLeaveFlowMapper.xml`.
- **선행 작업**: PRAFTA-045-1
- **우선순위 근거**: 무기한 타입이 "사용 불가"가 되는 정합성 버그 직결. (A)안이면 045-1과 동시 처리.
- **상태**: §7 결정(A/B) 확정 전까지 착수 보류.

### PRAFTA-045-3 [backend] 회귀 가드 테스트 (qa 인계 보조)
- **영역**: web / **모듈**: cmm/leave / **작업유형**: 신규(테스트)
- **상세 설명**:
  - 핵심 요구사항: 수동부여 avail-term 분기(01/02/03/null) 단위테스트 + 법정 엔진 무변경 회귀(기존 `LeaveGrantEngine*Test` 전수 통과 확인).
  - 영향 파일: `src/test/java/com/prafta/common/cmm/leave/...` 신규.
- **선행 작업**: PRAFTA-045-1
- **우선순위 근거**: 법정 회귀 위험이 커 게이트 필수.

> 분해 4건 이하 — 1차에서 전부 제시. 화면 작업 없음(타입관리 입력은 044 완료, 표시 변경도 불필요).

---

## 7. 채팅 확인 필요 결정사항 (사용자 승인 전 분해 확정 금지)

1. **[01 무기한 처리]** §3-2: (A) sentinel `99991231`(소비 SQL 무변경, 회귀선 보존, planner 권장) vs (B) null + 소비쿼리 2곳 수정. 어느 쪽?
2. **[법정 제외 범위]** §2: "타입 avail-term은 `MANUAL_*` 수동부여에만 적용, `STATUTORY_*` 엔진은 전면 미변경" 확정 동의 여부. (정책상 명백하나 회귀 영향이 커 명시 확인.)
3. **[폼 availFromDate 관계]** §3-3: AVAIL_FROM_DATE는 폼 입력 유지 + AVAIL_TO_DATE만 타입 term으로 재산출(planner 잠정안). `03`에서 타입 `ADMIN_AVAIL_FROM_DT`로 from까지 덮을지 여부.
4. **[02 기준연도]** §3-1: "해당 연도"의 YYYY = 폼 availFromDate 연도(잠정안) vs 부여실행일(today) 연도.
5. **[관리자 자동부여]** §0: 비법정 관리자 타입 자동부여는 현재 미구현(실행 잡 없음)이라 본 작업 범위 제외 확정 동의 여부. (요청서 결정포인트 1에 등장하나 실체 없음.)

---

## 8. 메인 세션 Notion 반영 항목 (서브에이전트 미접근)

"작업 로그" DB에 아래 3행 등록(승인 후):

| 작업ID | 영역 | 모듈 | 작업유형 | 상태 | 담당 | 요구사항 요약 | 상세 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PRAFTA-045-1 | web | cmm/leave | 보완 | 분해완료 | planner | 수동부여 AVAIL_TO_DATE를 타입 avail-term으로 산출 | [backend] §6 PRAFTA-045-1 본문 / 출처 §8.1.1·§8.5.1·§8.5.8 |
| PRAFTA-045-2 | web | cmm/leave | 보완 | 분해완료(결정대기) | planner | 01 무기한 소비창 안전 처리(sentinel or null+SQL) | [backend] §6 PRAFTA-045-2 / 선행 045-1 |
| PRAFTA-045-3 | web | cmm/leave | 신규 | 분해완료 | planner | avail-term 분기 + 법정 엔진 무변경 회귀 테스트 | [backend] §6 PRAFTA-045-3 / 선행 045-1 |

선행관계: 045-2, 045-3 ← 045-1 (Relation).

---

## 9. qa / security 인계 메모

- **회귀 게이트(법정연차)**: `LeaveGrantEngineServiceImpl` 및 엔진 테스트 5종(`LeaveGrantEngineProrationTest`, `*Prafta029/030/032Test`, `*ScenarioTest`)은 **무변경 통과**해야 한다. 변경 diff가 이 파일에 닿으면 즉시 반려.
- **소비창 회귀(B안 채택 시)**: `selectDeductibleGrant`는 법정/비법정 차감이 공유한다. `IS NULL OR` 추가가 법정 차감(항상 AVAIL_TO_DATE 존재)에 영향 없음을 회귀 확인.
- **null/sentinel 전수 확인 지점(이미 planner 검증, qa 재확인 권장)**: `expireOverdueGrants`(SQL), `isExpired`(Java), `legalNearestExpire`(MIN), `expiringSoon30`(BETWEEN), `selectDeductibleGrant`(web+app).
- **보안(security)**: 권한 가드(`ensureManager`, §8.5.7)는 기존 manualGrant 진입부에 존재 — 변경 없음. 신규 SELECT는 cmpnyCd 스코프 격리 필수(타사 타입 avail-term 조회 차단). leaveCd 화이트리스트(`countManualGrantType`)는 기존 유지.
- **스키마 주의**: schema-full.sql 스냅샷은 `ADMIN_AVAIL_FROM_DT/TO_DT`를 varchar(6)으로 표기하나 **prafta-044-FU2에서 varchar(8) YYYYMMDD로 마이그됨(코드 권위)**. developer는 MCP 라이브 DB로 확정할 것.
