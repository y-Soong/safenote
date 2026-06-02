# prafta-022 연차 부여엔진 수정 · 작업 분해 계획서 (마스터)

> 보고된 두 버그(문제1: handlingType 미반영·RESET_ALL 재부여 불가 / 문제2: 정책 AXIS1 미반영)를
> 근본원인 검증 결과에 따라 **서로 의존하는 작업 단위(A~F)** 로 분해한다.
> 본 문서가 단일 출처(supersede)이며, 개발 착수 시 본 문서의 작업표·결정 로그·필요 DDL을 따른다.
>
> 근본원인은 사용자 지시에 따라 재조사하지 않고 그대로 신뢰했으며, 본 분해에서는 코드/스키마 실측으로 **고칠 지점과 재사용 블록만 확정**했다.
> 검증한 파일: `LeaveDashboardServiceImpl`(common/cmm/leave), `User01ServiceImpl`(web/user/user01), `LeaveGrantStatusServiceImpl`, `Attd09Controller/ServiceImpl`, `Attd_09.vue`, `LeavePolicyVO`, schema-full.sql(tb_user_leave_grant / tb_user_hire_date_history / tb_leave_policy).

---

## 1. 결정 로그 (확정)

| # | 항목 | 결정 |
|---|---|---|
| 1 | 적용 트리거 (문제1 = B안) | 입사일 수정(`User01ServiceImpl.updateUserHireDate`)은 **기록만** 유지(현행 446-447행 NOTE 그대로). 실제 grant 조작은 **Attd_09 "정책 기준 부여" 버튼** 경로(`LeaveDashboardServiceImpl.hireDateGrant`)에서만 수행한다. |
| 2 | 기준 입사일 | 부여 계산 기준 입사일 = 현재 `TB_USER.HIRE_DATE`(여러 번 수정한 누적 결과치). 별도 anniversary 추정 금지. |
| 3 | 적용할 handlingType | 그 직원의 **최신 입사일변경 이력 1건**(`TB_USER_HIRE_DATE_HISTORY`에서 `INSERT_DATE` 최신)의 `HANDLING_TYPE`을 읽어 적용. 이력이 없으면 신규 부여로 간주(=`KEEP_AND_APPLY_NEW`와 동치 처리). |
| 4 | handlingType 분기 정의 | `KEEP_AND_BACKFILL`=기존 GRANT 유지 + 멱등키 누락분만 INSERT. `KEEP_AND_APPLY_NEW`=기존 GRANT 유지 + 신규분만 부여(현행 멱등 INSERT와 동일 효과). `RESET_ALL`=기존 `STATUTORY_*` ACTIVE/EXHAUSTED GRANT를 `cancelGrant`로 CANCELED 소프트 처리 후 재발급. `tb_user_leave_use` 사용 이력은 어떤 옵션에서도 삭제 금지(§8.5.8 #5). |
| 5 | RESET_ALL 무한 재발급 차단 (멱등키 재설계) | 현행 멱등키 `{userCd}_{baseYear}_{grantType}_HIRE`는 한 번 부여 후 영구 skip → RESET_ALL이 cancel+재발급을 못 한다. **적용 회차 추적용 컬럼을 이력 테이블에 추가**(필요 DDL §5 D1)하고, RESET_ALL은 (a) 기존 STATUTORY_* 취소 → (b) 멱등키에 **적용 회차(seq) 접미사**를 붙여 새 키로 재발급하는 방식으로 재실행 가능하게 한다. "이미 적용 완료(APPLIED)"한 이력은 재클릭 시 재실행하지 않는다(멱등). 상세는 작업 C/D. |
| 6 | 정책 기준 부여 전환 (문제2) | `hireDateGrant`에서 입사일 anniversary 하드코딩 제거. **AXIS1 분기 필수**: `HIRE_DATE`=현행 입사일 근속 기준 / `FISCAL_YEAR`=AXIS2(회계연도 시작 MM/DD) 기준 부여연도 산정. 첫해 방식(AXIS3)은 §결정#8 경계까지 반영. |
| 7 | 버튼 라벨/정체성 변경 | Attd_09 버튼 라벨 "입사일 기준 연차 부여" → **"정책 기준 부여"**. 백엔드 메서드/엔드포인트명은 호환 위해 단계적(작업 E에서 신규 엔드포인트 추가 + 기존 path 유지 여부는 developer 판단, 본 분해는 신규 경로 권장). |
| 8 | 022/023 경계 (AXIS3/AXIS4 범위) | **022 포함**: AXIS1 분기(HIRE_DATE/FISCAL_YEAR), AXIS2 회계연도 산정, AXIS3=`MONTHLY_ONLY`(1년 미만 월차만) + `NEXT_YEAR_BULK`(첫해 본연차를 차년도 회계연도에 일괄) 최소 동작, AXIS5 근속가산(현행 `tenureBonusDays` 재사용), AXIS6 유효기간(현행 재사용). **023 분리**: AXIS3=`PRORATE` 정밀 비례부여 + AXIS4 반올림(CEIL/ROUND/FLOOR/HALF_DAY) 풀 매트릭스, 자동 정기 부여 스케줄러, BACKFILL 과거연도 백필 확장(다년도). 근거 = §결정 로그 하단 "경계 근거". |
| 9 | 부여 엔진 구조 | `hireDateGrant`의 부여 핵심 로직을 **공용 부여 엔진 서비스 `LeaveGrantEngineService`로 추출**한다. 이유: (a) Attd_09 버튼 경로와 향후 스케줄러(023)가 동일 엔진을 공유, (b) handlingType 분기·정책 AXIS 해석·멱등 재발급을 한 곳에 모아 테스트 가능. `LeaveDashboardServiceImpl`은 대시보드 조회 책임만 남기고 부여 책임을 엔진으로 위임한다. |
| 10 | 영향분석 실측화 (이번 범위) | `User01ServiceImpl.buildApproxImpact`(약 456행)가 기존부여/사용/누락을 전부 "0일" 하드코딩 → 실집계로 교체. 단 입사일 수정 트랜잭션은 여전히 "기록만"이므로, buildApproxImpact는 **현재 GRANT/USE 실집계 + 변경 시 예상 차이**를 조회로 산출(부여 실행은 하지 않음). |
| 11 | 프리뷰 | 버튼 클릭 시 선택 직원별 적용 대상(handlingType, 취소될 grant 건수, 추가될 일수)을 **집계 프리뷰 모달**로 먼저 보여주고 확인 후 적용. 프리뷰 전용 조회 엔드포인트(부여 미수행) + 신규 팝업 컴포넌트 1종. |

### 경계 근거 (결정 #8 상세)
- 문제2가 "의미"를 가지려면 회계연도 정책에서 **부여 기준연도가 입사일 anniversary가 아니라 회계연도로 이동**하는 것이 핵심이다. 이는 AXIS1=FISCAL_YEAR + AXIS2 산정만으로 달성되며, 첫해 방식 중 `MONTHLY_ONLY`/`NEXT_YEAR_BULK`는 "본연차를 언제 줄지"의 정수(整數)일 부여라 별도 비례계산이 없어 022에 안전히 넣을 수 있다.
- 반면 `PRORATE`는 "입사 후 잔여기간 ÷ 1년 × 15일"의 소수 계산 + AXIS4 반올림 4종이 결합되어 검증 매트릭스가 크다. 이를 022에 넣으면 범위가 폭증하고 버그1·2 수정이 지연된다 → 023으로 분리.
- AXIS5(근속가산)·AXIS6(유효기간)은 현행 코드(`tenureBonusDays`, `resolveValidityMonths`)가 이미 정책값을 읽으므로 022에서 그대로 재사용(신규 작업 아님).

---

## 2. 근본원인 → 수정 지점 매핑 (검증 결과)

| 버그 | 근본원인 (검증 확정) | 수정 지점 |
|---|---|---|
| 문제1-① | `updateUserHireDate` 446-447행 NOTE로 grant 조작 미수행, handlingType은 이력에 기록만 됨 | 유지(B안). 적용은 부여 버튼 경로로 이동 → 작업 C/D |
| 문제1-② | `hireDateGrant`가 `userCds`만 받고 handlingType 미수신 | 엔진이 직원별 최신 이력 HANDLING_TYPE 조회 → 작업 C/D |
| 문제1-③ | 멱등키 `{userCd}_{baseYear}_{grantType}_HIRE`로 올해분 1회 부여 후 영구 skip → RESET_ALL 재발급 불가 (`grantComponent` 503-508행) | 멱등키 재설계 + RESET_ALL cancel→재발급 → 작업 C/D, 필요 DDL D1 |
| 문제1-④ | FE `fnHireDateGrant`가 `{userCds}`만 POST (Attd_09.vue 758-760행) | 프리뷰 + 적용 페이로드 변경 → 작업 E |
| 문제2 | `hireDateGrant`가 AXIS5/AXIS6만 읽고 AXIS1~AXIS4 무시. 입사일 anniversary·15일 하드코딩(`BASE_ANNUAL_DAYS`, `monthsBetweenNow`) | AXIS1/AXIS2/AXIS3 해석 추가 → 작업 B |
| 영향분석 | `buildApproxImpact` 486-489행 전부 "0일"/근사 하드코딩 | 실집계 조회로 교체 → 작업 F |

---

## 3. 작업 분해 · 의존성 · 순서

| 코드 | 작업 | 핵심 산출물 (파일경로 수준) | 의존 |
|---|---|---|---|
| **A** | 공용 부여 엔진 서비스 추출 (`hireDateGrant`의 부여 핵심을 `LeaveGrantEngineService`로 이관, `LeaveDashboardServiceImpl`은 위임). 기존 동작 동치 유지(리팩터링 단계, 정책/멱등 변경 없음). | (신규) `common/cmm/leave/service/LeaveGrantEngineService.java` + `impl/LeaveGrantEngineServiceImpl.java`<br>(수정) `common/cmm/leave/service/impl/LeaveDashboardServiceImpl.java`(부여 메서드 이관) | 독립 |
| **B** | 정책 기준 부여 전환 (문제2). 엔진에 AXIS1 분기(HIRE_DATE/FISCAL_YEAR) + AXIS2 회계연도 부여연도 산정 + AXIS3(MONTHLY_ONLY / NEXT_YEAR_BULK) 최소 반영. 입사일 anniversary·15일 하드코딩 제거(정책값으로 대체, 정책 없으면 법정 기본). | (수정) `LeaveGrantEngineServiceImpl.java`(부여연도/baseYear 산정 로직, AXIS 해석)<br>(참조) `LeavePolicyVO`(axis1~axis6, 변경 없음) | **A** |
| **C** | handlingType 연동 + 멱등키 재설계 (문제1 핵심). 엔진이 직원별 최신 `TB_USER_HIRE_DATE_HISTORY.HANDLING_TYPE` 조회 → 분기. RESET_ALL은 기존 STATUTORY_* `cancelGrant` 후 재발급. 멱등키에 적용 회차 접미사 부여하여 재실행 가능. APPLIED 추적(필요 DDL D1). | (수정) `LeaveGrantEngineServiceImpl.java`(handlingType 분기, RESET_ALL cancel→재발급, 멱등키)<br>(신규 매퍼) `LeaveDashboardMapper` 또는 신규 `LeaveGrantEngineMapper`에 `selectLatestHandlingType`, `selectActiveStatutoryGrantIds`, `markHistoryApplied` 등<br>(재사용) `LeaveGrantStatusService.cancelGrant` | **A·B**, 필요 DDL **D1** |
| **D** | 적용 프리뷰 집계 (부여 미수행 dry-run). 선택 직원별 handlingType·취소예정 grant 건수·추가예정 일수를 조회로 집계하는 엔진 메서드 + 신규 GET/POST 프리뷰 엔드포인트. | (수정) `LeaveGrantEngineServiceImpl.java`(`previewPolicyGrant` dry-run)<br>(수정) `Attd09Controller.java` / `Attd09ServiceImpl.java`(프리뷰 엔드포인트 + 적용 엔드포인트 페이로드)<br>(신규 VO/Request/Response) attd09 dto | **C** |
| **E** | 프론트 Attd_09 수정 지점 + 프리뷰 모달 신규. 버튼 라벨 "정책 기준 부여"로 변경, 클릭 시 프리뷰 조회 → 모달 표시 → 확인 시 적용 POST. 신규 팝업 컴포넌트 1종. | (수정) `views/attd/Attd_09.vue`(버튼 라벨/`fnHireDateGrant`→`fnPolicyGrant`/페이로드)<br>(신규 골격) `components/popup/PolicyGrantPreviewPop.vue` | **D** (골격 자체는 D와 병렬 작성 가능, 실배선은 D 후) |
| **F** | 영향분석 실측화. `User01ServiceImpl.buildApproxImpact`를 현재 GRANT/USE 실집계 + 변경 시 예상 차이로 교체. 입사일 수정은 여전히 기록만(부여 미수행). | (수정) `web/user/user01/service/impl/User01ServiceImpl.java`(`buildApproxImpact`/`analyzeHireDateImpact`)<br>(신규 매퍼) `User01Mapper` 또는 leave 매퍼에 GRANT/USE 실집계 쿼리<br>(수정) `User01Mapper.xml` | 독립(엔진 미의존). 단 집계 쿼리는 C의 신규 집계 쿼리와 **공유 가능** → C 후 착수 권장 |

**권장 순서**: `A`(엔진 추출) → `B`(정책 AXIS) → `C`(handlingType+멱등) → `D`(프리뷰 백엔드) → `E`(프론트) → `F`(영향분석, C 집계쿼리 재사용 위해 C 후).
- `A`는 동치 리팩터링이라 회귀 위험이 가장 낮아 선행. `B`/`C`는 엔진 내부 변경이므로 `A` 완료 필수.
- `E` 프리뷰 모달 **골격(template/style)** 은 `D`와 병렬로 작성해도 무방하나, axios 배선은 `D`의 엔드포인트 확정 후.

---

## 4. 재사용 블록 (신규 작성 금지, 그대로 호출)

| 블록 | 위치 | 용도 |
|---|---|---|
| `cancelGrant(cmpnyCd, grantId, reasonByUserCd)` | `LeaveGrantStatusServiceImpl` | RESET_ALL 시 기존 STATUTORY_* 소프트 취소(CANCELED, 사용이력 보존) |
| `grantComponent(...)` | `LeaveDashboardServiceImpl`(→ A에서 엔진으로 이관) | 멱등 INSERT 1건 부여 |
| `recalcStatus` / `recalcUsedDays` / `expireOverdueGrants` | `LeaveGrantStatusServiceImpl` | 상태 재평가 (취소/재발급 후 호출 필요 시) |
| `findActivePolicy(cmpnyCd)` | `LeavePolicyService` | 활성 정책(7-axis VO) 조회 |
| `tenureBonusDays(policy, year)` / `resolveValidityMonths(cmpnyCd)` | `LeaveDashboardServiceImpl`(→ A에서 엔진으로 이관) | AXIS5 근속가산 / AXIS6 유효기간 (변경 없음) |
| VO 필드 | `LeavePolicyVO` | axis1GrantBase, axis2FiscalStartMm/Dd, axis3FirstYearMethod, axis4ProrateRounding, axis5*, axis6ValidityMonths |

---

## 5. 필요 DDL (사용자 승인 대상 — 임의 적용 금지)

> 아래 DDL은 본 분해의 가정이며, **사용자 승인 전까지 적용하지 않는다.** 컬럼명/타입은 기존 `TB_USER_HIRE_DATE_HISTORY` 스키마 컨벤션(대문자, varchar 코드)을 따랐다.

### D1. `TB_USER_HIRE_DATE_HISTORY` — 적용(APPLIED) 추적 컬럼 추가 (작업 C 필수)
RESET_ALL 무한 재발급 차단(결정 #5)과 "이미 적용 완료" 멱등을 위해 이력 행에 적용 상태를 둔다.

```sql
ALTER TABLE `tb_user_hire_date_history`
  ADD COLUMN `APPLIED_YN` char(1) NOT NULL DEFAULT 'N'
      COMMENT '정책 기준 부여 적용 완료 여부 (Attd_09 부여 버튼에서 적용 시 Y)' AFTER `AFFECTED_GRANT_SNAPSHOT`,
  ADD COLUMN `APPLIED_DATE` datetime DEFAULT NULL
      COMMENT '적용 일시' AFTER `APPLIED_YN`,
  ADD COLUMN `APPLIED_BY` varchar(50) DEFAULT NULL
      COMMENT '적용 수행자 (USER_CD)' AFTER `APPLIED_DATE`;
```

- 적용 흐름: 부여 버튼 → 직원별 최신 미적용(`APPLIED_YN='N'`) 이력 1건의 HANDLING_TYPE으로 부여 수행 → 성공 시 해당 이력 `APPLIED_YN='Y'`, `APPLIED_DATE`/`APPLIED_BY` 기록.
- RESET_ALL 재실행: 새 입사일 변경 이력이 추가되면 `APPLIED_YN='N'` 신규 행이 생기므로 다시 적용 가능. 같은 이력 재클릭은 `APPLIED_YN='Y'`라 멱등 skip.

### D2. (대안 검토 — 채택 안 함) 멱등키만으로 회차 구분
`tb_user_leave_grant.IDEMPOTENCY_KEY`에 회차 접미사(`..._HIRE_2`)를 붙여 재발급하는 방식 단독으로도 재실행은 가능하나, "이미 적용된 이력인지" 판단 근거가 GRANT 테이블 추정에 의존해 취약하다. **D1(이력 APPLIED 플래그)을 단일 진실원으로 채택**하고, 멱등키 회차 접미사는 D1과 함께 보조로만 사용한다.

> 위 외 신규 테이블/컬럼은 없다. `tb_user_leave_grant` / `tb_leave_policy`는 기존 컬럼으로 충분(검증 완료).

---

## 6. 프론트 수정 지점 명세 (Attd_09.vue — 신규 화면 아님)

`prafta-web-frontend/prafta-web-frontend/src/views/attd/Attd_09.vue` 기존 화면 수정.

| 지점 | 현행 | 변경 |
|---|---|---|
| 버튼 라벨 (124행) | "입사일 기준 연차 부여" | **"정책 기준 부여"** |
| 핸들러 (733행 `fnHireDateGrant`) | `$confirm` 후 바로 `POST /webApi/attd09/leave-grant/hire-date-grant` `{userCds}` | `fnPolicyGrant`로 개명. ① 프리뷰 조회(작업 D 엔드포인트) → ② `PolicyGrantPreviewPop` 모달 표시 → ③ 모달 "적용" 확인 시 적용 POST(페이로드 `{userCds}` 동일, 서버가 직원별 최신 handlingType·정책 해석) → ④ 결과 alert + `fnSearch()` |
| 입사일 미입력 검증 (737-749행) | 유지 (프론트 1차 가드) | 유지 |
| 결과 표시 (762-768행) | grantedCount/skippedCount/grantedDays alert | 유지 + (선택) 취소 grant 건수 표기 |

### 신규 프리뷰 모달 — `components/popup/PolicyGrantPreviewPop.vue`
- 호출 방식: 기존 `openPop(Component, props)` 패턴(ManualGrantPop/LeaveDetailPop와 동일). props로 프리뷰 집계 결과 + `onConfirm` 콜백 전달.
- 구조: 기존 팝업 컨벤션(`modal-overlay`/`modal-content-normal`/드래그 `modal-header`/`viewBody`) + `BatchResultPop`식 요약+테이블 레이아웃 차용.
- 표시 내용:
  - 요약: "선택 N명 · 신규부여 X명 · 재발급(RESET_ALL) Y명 · 변경없음 Z명"
  - 테이블 컬럼: 직원명 / 처리방식(handlingType 한글 라벨) / 취소예정 건수 / 추가예정 일수 / 비고
  - 푸터: [취소] [적용] — [적용] 클릭 시 `emit('confirm')` 또는 props.onConfirm 호출(API 호출은 부모 Attd_09가 수행, 모달은 UI만)
- 골격은 작업 E에서 template + style(scoped, CSS 변수)만 작성. axios/store/router 로직은 developer가 채움(planner 골격 규칙 §3-2).

> 본 모달은 별도 화면(view)이 아니라 전역 팝업이므로 router 등록 불필요(메모리: viewResolver/팝업 패턴). UI 명세 ID는 메인 세션이 Notion "도메인 지식 베이스"에 등록할 때 채번한다(서브에이전트 Notion 미사용).

---

## 7. DTO / SQL 규칙 (CLAUDE.md 준수 — 전 작업 공통)
- DB 컬럼: 대문자 + 언더스코어. Java DTO 필드: 대문자 유지(`private String HANDLING_TYPE;`) 또는 기존 leave 도메인 VO 컨벤션(camelCase getter + resultMap 매핑) 일관 유지 — **기존 `LeavePolicyVO`/`LeaveGrantInsertVO` 스타일을 따른다**.
- MyBatis: leading comma, `#{...}` 바인딩(정렬 컬럼명 등만 `${}`), `SELECT *` 금지(명시 컬럼).
- 모든 SQL은 실 스키마와 100% 일치. 신규 컬럼(D1)은 **DDL 승인 후에만** 쿼리에서 참조.
- 신규 멱등키/취소/재발급은 `UNIQUE(CMPNY_CD, IDEMPOTENCY_KEY)` 제약과 정합. `DuplicateKeyException` 경합 처리는 기존 `grantComponent` 패턴 재사용.
- 사용 이력(`tb_user_leave_use`)은 어떤 경로에서도 DELETE/사후차감 금지(§8.5.8 #2·#5).

---

## 8. 정책서 출처 (작업별 정독 가이드)
- 작업 B (정책 AXIS): `attd/08-leave.md` §8.5.2(7-axis 정의), §8.5.3(Cross-axis 활성 매트릭스), §8.5.4(1년 미만 월차).
- 작업 C (handlingType/멱등): `attd/08-leave.md` §8.5.6(입사일 변경 처리 매트릭스), §8.5.8(멱등·기부여보호 절대규칙).
- 작업 D/E (프리뷰): §8.5.6(처리방식별 영향), §8.5.7(권한 — POST는 MASTER/HR).
- 작업 F (영향분석): §8.5.6(영향 스냅샷), §8.5.8(기부여 보호).
- 전 작업 권한 가드: §8.5.7(부여/적용 POST = `AUTH_MASTER` OR `AUTH_HR_MANAGER`, `AuthRoleUtils.isManager` 진입부 강제 — 기존 `ensureManager` 패턴 유지).

---

## prafta-023 후속 분리 항목

> 메인 세션은 본 섹션으로 `prafta-023.md`(요청서) 또는 `prafta-023-plan.md`를 작성한다. prafta-021 종료 후 진행.

1. **AXIS3=PRORATE 정밀 비례부여 + AXIS4 반올림 풀 매트릭스**
   - 입사 후 잔여기간 ÷ 1년 × 본연차 15일의 소수 계산, AXIS4(CEIL/ROUND/FLOOR/HALF_DAY) 4종 적용.
   - §8.5.3 매트릭스: AXIS3=PRORATE일 때만 AXIS4 입력 활성, 그 외 분기는 AXIS4='CEIL' 강제(검증 케이스 다수).
   - 022에서는 AXIS3=PRORATE 선택 시 "비례부여는 후속(023)" 안내 또는 NEXT_YEAR_BULK로 폴백하는 경계 처리(작업 B에서 결정).
2. **자동 정기 부여 스케줄러**
   - 일배치로 정책 기준 자동 부여(현재는 Attd_09 버튼 수동 트리거만). `LeaveGrantEngineService`(022 작업 A에서 추출)를 재사용.
   - 본연차 `{USER_CD}_{YYYY}_ANNUAL`, 월차 `{USER_CD}_{YYYYMM}_MONTHLY`, 근속가산 `{USER_CD}_{YYYY}_TENURE_BONUS` 정식 멱등키 체계(§8.5.8 #1)로 전환(022의 `_HIRE` 접미사 키는 수동 트리거 한정).
3. **BACKFILL 과거연도 백필 확장 (다년도)**
   - `KEEP_AND_BACKFILL`을 올해뿐 아니라 입사 이후 누락된 과거 연도 전체로 확장. 022에서는 당해 연도 누락분 중심.
4. **EXPIRE_YN deprecation 동기 로직 / 트리거** (§8.5.8 #4, 별도 작업으로 이미 분리되어 있음 — 023 묶음 후보).

### 023 분리 근거 (요약)
- 위 항목은 모두 "정밀 계산·정기 자동화·다년도 소급"으로, 보고된 버그1·2의 직접 원인이 아니다. 022는 "버그를 막고 정책이 반영되게" 하는 최소 정합 범위로 한정하고, 정밀도/자동화는 별도 검증 사이클(023)로 분리해 022 착수 지연을 막는다.
</content>
</invoke>
