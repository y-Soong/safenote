# prafta-044 작업 분해 (planner)

원본 요청서: `.claude/requests/web_requests/prafta-044.md`
작업 영역: 웹FE(`PRAFTA/prafta-web-frontend`) + 백엔드(`PRAFTA/prafta-backend`)
처리 워크플로우: planner → developer → qa → security (메인 세션 Notion 대행)

---

## 0. 스키마 / 코드값 확인 결과 (추측 아님 · 실제 확인)

- `TB_LEAVE_TYPE_MGMT.USE_UNIT_TYPE varchar(2) DEFAULT NULL COMMENT '연차 사용 단위[SYS025]'`
  — schema-full.sql L413 확인. **컬럼 이미 존재, 신규 컬럼/마이그 불필요.**
- `TB_USER_LEAVE_USE.USE_UNIT_TYPE varchar(2) NOT NULL COMMENT '사용 단위 (tb_leave_type_mgmt.USE_UNIT_TYPE 복사, SYS025)'`
  — L1153 확인. 소비 시 타입의 USE_UNIT_TYPE를 복사함(D3 소비 단위 결선 증거).
- SYS025: 00=1일 / 01=반차 / 02=시간차(2h) / 03=시간차(1h) / 04=시간차(30분).
- SYS021 leaveType: 01=사용자 신청 / 02=관리자 부여. SYS022 grantType: 01=자동부여 / 02=수동부여.
- 연차타입 저장 endpoint = `POST /webApi/attd03/update-leave-types` (요청서가 추정한 baim 아님 → **attd03 모듈**).
- 수동부여 endpoint = `POST /webApi/attd09/leave-grant/manual-grant` · `/bulk-manual-grant`.

### 백엔드 현행 결선 (핵심)

1. `LeaveTypeRequest`/`LeaveTypeParam`/`LeaveTypeCommand` 세 곳 모두 이미 `useUnitType` 필드를 무조건 운반한다.
2. `Attd03Mapper.xml > updateLeaveType` INSERT 절은 이미 `, USE_UNIT_TYPE = #{useUnitType}` (L50) 바인딩.
   → **신규 타입 INSERT 시 FE가 보내는 useUnitType 값이 그대로 영속된다.** 백엔드는 받는 값을 저장만 한다.
3. 같은 SQL의 `ON DUPLICATE KEY UPDATE` 절은 LEAVE_DESC / USE_YN / UPDATE_NO / UPDATE_DATE 만 갱신.
   → **수정 모드에서는 USE_UNIT_TYPE를 바꾸지 않는다.** 이는 화면의 '01'(사용자신청) 패턴과 정합
     (사용단위 select가 `:disabled="isEditMode"` = 생성시만 입력, 수정시 잠금).
4. `Attd03ServiceImpl.validateLeaveTypeRule`은 수동부여(02/02) 분기에서 grantBaseType/grantOffsetMonth/grantAssignMmdd만
   검증하고 **useUnitType은 검증/금지하지 않는다.** → useUnitType 영속에 막는 서버 가드 없음.

> **결론: D1의 백엔드는 "이미 받는 값을 저장"하므로 신규 코드 변경이 필요 없다(검증 항목).**
> 문제의 본질은 FE가 leaveType!=='01'일 때 `useUnitType: null`로 강제 전송하는 것(LeaveTypeCreatePop L725)뿐이다.
> 따라서 D1은 사실상 **FE 단독 변경**으로 해결된다.

### FE 현행 (문제 지점)

- `LeaveTypeCreatePop.vue`
  - 사용단위 select는 `leaveType==='01'` 섹션(L216~229)에만 존재. 수동부여 섹션(L311 `v-else` = 02&&02)엔 없음.
  - 저장 payload L725: `useUnitType: leaveType.value === "01" ? useUnitType.value : null` → 수동부여는 항상 null.
  - `canSave`(L535~)에는 '01'에서만 useUnitType 필수 검증.
- `ManualGrantPop.vue`
  - 부여 일수 input L99~105: `type="number" min="0.5" step="0.5"`.
  - `fnValidate` L311~315: "0.5일 단위(days*2 정수)" 검증.
  - 제출 L263: `parseFloat(form.value.grantDays)`.
- 백엔드 수동부여 일수 검증: `LeaveDashboardServiceImpl.isValidGrantDays`(L643~653)가
  "0.5일 단위(days*2 정수)"를 허용 → **정수 강제로 정정 필요(서버 권위).**
  - 일수 입력 객체는 `BigDecimal grantDays`(ManualGrantRequest/BulkManualGrantRequest/ManualGrantCommand).
  - INSERT 시 `grantDaysScaled = grantDays.setScale(1, HALF_UP)` (L353) → 정수여도 N.0 저장 무해.

---

## 작업 분해 결과

### prafta-044-1
- **유형**: frontend-screen (팝업 보완)
- **영역**: web
- **모듈**: attd / attd03 (연차 타입 관리)
- **작업 유형**: 보완
- **요구사항 요약**: 연차타입 팝업의 관리자 수동부여 섹션(leaveType='02' && grantType='02')에 사용단위(SYS025) 입력을 추가하고, 저장 payload가 해당 타입의 useUnitType를 null이 아닌 입력값으로 전송하도록 한다. (D1)
- **상세 설명**:
  - 정책서 출처: 근태 §8.1.1(구성 속성 — 사용단위), §8.5.9(사용 단위 정책 SYS025 00~04). 충돌 없음.
  - 핵심 요구사항:
    1) 수동부여 섹션(template L311 `v-else` 블록 = leaveType==='02' && grantType==='02')의 `.form-grid`에
       "사용단위" select를 추가한다. 옵션은 `systCodeArr['SYS025']` 전체(일/반차/시간차) — '01' 섹션 L216~229와 동일 구조 복제.
    2) `v-model="useUnitType"` 재사용(기존 ref). 편집모드는 '01' 패턴과 **일관**되게 `:disabled="isEditMode"` 적용
       (생성시 입력 · 수정시 잠금). 백엔드 ON DUPLICATE 절이 USE_UNIT_TYPE를 update하지 않으므로 정합.
    3) 필수(*) 표기: '01' 섹션이 `<span class="required">*</span>`로 필수이므로 수동부여도 **필수**로 통일(권장).
    4) `fnSave` payload L725 수정:
       `useUnitType: (leaveType.value === "01") ? useUnitType.value : ((leaveType.value === "02" && grantType.value === "01") ? null : useUnitType.value)`
       → 즉 **자동부여(02/01)만 null, 사용자신청(01)·수동부여(02/02)는 입력값 전송.** (자동부여 섹션엔 사용단위 입력이 없으므로 null 유지.)
    5) `canSave`(L535~)에 수동부여 분기 useUnitType 필수 검증 추가(필수로 결정한 경우): `if (leaveType==='02' && grantType==='02' && !useUnitType.value) return false`.
    6) 수정모드 진입 시 `fnGetSystinfoList`가 이미 `useUnitType.value = r.useUnitType ?? firstValid('SYS025')`로 채움(L629) → 표시 정합.
  - 영향 받는 파일:
    - (프론트) `prafta-web-frontend/src/views/attd/popup/LeaveTypeCreatePop.vue`
  - 영향 받는 endpoint: `POST /webApi/attd03/update-leave-types` (호출만, 계약 불변).
  - 예상 산출물: LeaveTypeCreatePop.vue 수정(template 사용단위 select 추가 + payload/canSave 분기).
  - 연결 UI 명세: UI-044-1.
  - 백엔드 변경: **없음**(0 섹션 결론). developer는 정수/단위 영속을 코드변경 없이 회귀 확인만.
- **선행 작업**: 없음
- **우선순위 근거**: 법정 책임 영역(attd) +1 격상. D1은 D2/D3가 의미를 가지려면 선행되어야 함(소비단위 동작의 원천).

### prafta-044-2
- **유형**: mixed → backend + frontend-screen 분할
- **영역**: web
- **모듈**: attd / attd09 (수동부여) + common/cmm/leave
- **작업 유형**: 보완 (버그수정 성격: 0.5 하드코딩 제거)
- **요구사항 요약**: 수동부여 부여 일수를 1일 단위(정수)로만 입력/검증하도록 FE·BE 양쪽을 정정한다. 0.5 단위 잔재 제거. (D2)
- **상세 설명**:
  - 정책서 출처: 근태 §8.1.1(관리자 부여 타입 부여 일수는 수동부여 행위 시점 결정), §8.5.8(수동부여 멱등/레코드). 부여 단위 1일은 본 요청서 확정설계 D2. 정책서에 "수동부여=0.5단위" 명시 없음 → 정수화는 정책 위반 아님.
  - 핵심 요구사항(프론트 = prafta-044-2a):
    1) `ManualGrantPop.vue` 부여 일수 input L99~105: `min="0.5" step="0.5"` → `min="1" step="1"`.
    2) `fnValidate`(L300~323): 0.5 단위 검증(L312~315) 제거 → **정수 검증**으로 교체
       (예: `if (!Number.isInteger(days) || days < 1) { alert("부여 일수는 1일 단위 정수로 입력해 주세요."); return false }`).
       기존 `days <= 0` 검증은 정수검증에 흡수.
    3) 제출(L263) `parseFloat` 유지 무방하나 `parseInt`/정수 보장 권장(정수만 통과하므로 값 동일).
    4) USE_UNIT_TYPE 표시/전송 추가는 **불필요**(부여는 단위 무관, manual-types 응답 변경 없음).
  - 핵심 요구사항(백엔드 = prafta-044-2b · 서버 권위):
    1) `LeaveDashboardServiceImpl.isValidGrantDays`(L643~653) 0.5단위 허용 로직 → **정수 강제**.
       교체안: `days != null && days > 0 && days.stripTrailingZeros().scale() <= 0` (정수 = scale<=0).
    2) 위반 시 기존 `AttdErrorCode.ATTD_400_031` 재사용(에러코드 메시지 "일수 단위" 문구가 0.5 가정이면 메시지만 정정 검토 — developer 확인).
    3) 상한 `MAX_GRANT_DAYS`, `setScale(1, HALF_UP)` INSERT는 그대로(정수도 N.0 저장, decimal(5,1) 정합).
    4) `ManualGrantRequest`/`BulkManualGrantRequest`/`ManualGrantCommand`의 grantDays 타입(BigDecimal) 및 주석
       "0.5일 단위" → "1일 단위(정수)"로 주석 정정(코드 동작은 서비스 검증으로 강제).
  - 영향 받는 파일:
    - (프론트) `prafta-web-frontend/src/views/attd/popup/ManualGrantPop.vue`
    - (백엔드) `common/cmm/leave/service/impl/LeaveDashboardServiceImpl.java` (isValidGrantDays)
    - (백엔드 주석만) `web/attd/attd09/dto/request/ManualGrantRequest.java`, `BulkManualGrantRequest.java`, `common/cmm/leave/command/ManualGrantCommand.java`
  - 영향 받는 endpoint: `POST /webApi/attd09/leave-grant/manual-grant`, `/bulk-manual-grant` (계약 불변, 검증 규칙만 강화).
  - 예상 산출물: service 검증 정정 + 팝업 step/min/validate 정정.
  - 연결 UI 명세: UI-044-2.
- **선행 작업**: 없음 (prafta-044-1과 독립적이나 같은 PR 권장)
- **우선순위 근거**: 법정 책임 영역(attd) +1. 데이터 정합성(부여 단위) 영향. 서버 권위 검증이 핵심.

### prafta-044-3
- **유형**: backend (검증 전용 · 신규 구현 없음)
- **영역**: web + app
- **모듈**: attd/leaveflow (web) + app/leave/leaveflow (app)
- **작업 유형**: 검증(회귀)
- **요구사항 요약**: 수동부여 타입에 USE_UNIT_TYPE가 저장된 후, 연차 사용/신청 흐름(웹·앱 leaveflow)이 해당 단위로 동작하는지, null일 때 풀데이 기본값으로 동작하는지 회귀 검증한다. 갭 발견 시에만 작업화. (D3)
- **상세 설명**:
  - 정책서 출처: 근태 §8.5.9(사용 단위 정책), §8.2(휴가 등록·신청), §8.4(시간 단위 휴가 예외).
  - 확인된 결선(검증 근거):
    1) `web/attd/leaveflow/mapper/LeaveFlowMapper.xml` — 연차타입 조회가 `A.USE_UNIT_TYPE AS useUnitType`(L20),
       소비 INSERT가 `, USE_UNIT_TYPE`(L160/181), 사용내역이 `U.USE_UNIT_TYPE AS useUnitType`(L286/355/419) 사용.
    2) `app/leave/leaveflow/mapper/AppLeaveFlowMapper.xml` — 동일하게 USE_UNIT_TYPE 조회/적용(요청서 §D3 명시).
    3) `TB_USER_LEAVE_USE.USE_UNIT_TYPE NOT NULL` → 소비 시 타입 단위를 복사하는 구조.
  - 검증 항목(qa/developer 공동):
    1) USE_UNIT_TYPE='01'(반차) 수동부여 타입을 사용/신청 시 반차 단위 입력 UI/계산이 적용되는가.
    2) USE_UNIT_TYPE='02/03/04'(시간차) 시 시간 입력·1일 환산이 적용되는가(§8.5.9 동적 환산).
    3) **USE_UNIT_TYPE가 null인 기존 데이터**(예: 00018 LEAVE_ADMIN_MANUAL) 사용 시 풀데이(1일) 기본값으로 안전 동작하는가
       (NOT NULL 복사 컬럼에 null이 흘러 들어가 오류나는지 — copy 시점 COALESCE/기본값 처리 확인).
    4) leaveflow가 useUnitType를 신뢰하는 출처(타입 테이블 vs 부여행)의 정합.
  - 영향 받는 파일(읽기/검증): 위 두 mapper + 그 service(LeaveFlowService / AppLeaveFlowService).
  - 영향 받는 endpoint: 웹/앱 연차 신청·사용 조회 endpoint(읽기 검증).
  - 예상 산출물: 신규 구현 없음. **갭 보고서**(있으면 별도 작업 prafta-044-3-FU 채번 권고).
- **선행 작업**: prafta-044-1 (타입에 단위가 저장되어야 검증 가능)
- **우선순위 근거**: 검증 위주, 후순위. 단 null 기본값 동작(검증항목 3)은 정합성 리스크 → 갭 발견 시 격상.

---

## 의존성 그래프

```
prafta-044-1 (FE: 수동부여 사용단위 입력+영속)  ──선행──>  prafta-044-3 (D3 소비 회귀검증)
prafta-044-2 (FE+BE: 부여 1일 정수)            (독립)
```

- 044-1, 044-2는 서로 독립. 같은 화면군이므로 단일 PR 묶음 권장.
- 044-3은 044-1 이후(타입에 단위 저장된 상태에서) 검증.

---

## 정책 출처 요약

| 작업 | 정책서 섹션 |
| --- | --- |
| 044-1 | 근태 §8.1.1(사용단위 속성), §8.5.9(SYS025 00~04 사용단위 정책) |
| 044-2 | 근태 §8.1.1(부여 일수는 수동부여 시점 결정), §8.5.8(수동부여 멱등/레코드); 1일 단위는 확정설계 D2 |
| 044-3 | 근태 §8.5.9, §8.2, §8.4 |

정책서 충돌 없음. 정책서에 "수동부여=0.5단위" 명시 없음 → D2 정수화는 정책 위반 아님(확정설계가 채움).

---

## 확정 결정(요청서 §확정설계) 반영 여부

- D1 — 044-1로 반영. **백엔드 신규 변경 불필요**(INSERT가 이미 useUnitType 영속, 막는 가드 없음). FE payload null 강제만 제거. ✅
- D2 — 044-2로 반영. FE step/min/validate + **BE isValidGrantDays 정수 강제(서버 권위)**. manual-types 미변경. ✅
- D3 — 044-3 검증으로 반영. 웹·앱 leaveflow가 USE_UNIT_TYPE 이미 읽음(신규구현 없음). null→풀데이 기본값 동작은 검증항목. ✅

---

## 채팅 확인 필요 (블로킹 아님 · 합리적 기본값으로 진행)

자율 진행 지시에 따라 아래는 **기본값으로 결정**하고 진행. 사용자가 다르게 원하면 알려주세요.

1. **수동부여 섹션 사용단위 필수 여부** → 기본값: **필수(*)**.
   근거: '01' 섹션이 필수이고, USE_UNIT_TYPE varchar(2) DEFAULT NULL이나 소비 단위 결정에 필요하므로 입력 강제가 안전.
   (만약 "선택, 미입력 시 null=풀데이"를 원하면 canSave 분기/required 표기를 제거.)
2. **편집모드 사용단위 수정 가능 여부** → 기본값: **수정 불가(:disabled=isEditMode)**.
   근거: '01' 패턴 일관 + 백엔드 ON DUPLICATE 절이 USE_UNIT_TYPE를 update하지 않음.
   (편집모드 수정을 허용하려면 ON DUPLICATE KEY UPDATE 절에 `USE_UNIT_TYPE = NEW.USE_UNIT_TYPE` 추가가 별도로 필요 — 그 경우 044-1에 백엔드 작업 추가.)
3. **D3 null 기존데이터(LEAVE_ADMIN_MANUAL 등) 일괄 백필 여부** → 기본값: **백필 안 함**(소비 시 풀데이 기본 동작 검증으로 충분). 갭이면 044-3-FU로 보고.
4. **ATTD_400_031 메시지 문구가 "0.5일 단위" 가정이면** → developer가 "1일 단위" 문구로 정정(코드 한국어 메시지 규칙). 별도 결정 불요.

---

## UI 명세

### UI-044-1 LeaveTypeCreatePop (수동부여 섹션 사용단위 추가)
- 연결 작업: prafta-044-1
- 화면 위치: `src/views/attd/popup/LeaveTypeCreatePop.vue` (기존 팝업 보완)
- 참조 패턴: 동일 팝업 '01'(사용자 신청) 섹션의 사용단위 select(L216~229)
- 변경 전: 수동부여 섹션(C. 부여 규칙, 02&&02)에는 "사용 가능기간(선택)"·"기간 설정"만 존재. 사용단위 입력 없음 → 저장 시 USE_UNIT_TYPE=null.
- 변경 후: 같은 form-grid 상단에 "사용단위 *" select(SYS025 전체) 추가. 생성시 입력·수정시 disabled.
- 레이아웃(수동부여 섹션, 변경 후):
```
┌ C. 부여 규칙 [필수] ────────────────────────────┐
│  [ 사용단위 * ▼ ]        [ 사용 가능기간(선택) ▼ ] │
│  ( adminAvailTermType==03 시 기간설정 캘린더 행 )   │
└───────────────────────────────────────────────┘
```
- 컴포넌트 매핑:
  | 영역 | 컴포넌트/요소 |
  | --- | --- |
  | 사용단위 | native `<select v-model="useUnitType">` (기존 '01' 섹션과 동일 native select 패턴 — 신규 공통컴포넌트 도입 안 함) |
  | 사용 가능기간 | 기존 native select(adminAvailTermType) |
  | 기간 설정 | 기존 CalendarSrch |
- 상태별 동작: 생성=select 활성/필수, 수정=disabled(값 표시), error=canSave=false로 저장버튼 비활성.
- 사용자 플로우: 관리자 부여 타입 선택 → 수동부여 선택 → C 섹션에서 사용단위 선택 → 저장 → USE_UNIT_TYPE 영속.
- 백엔드 의존: `POST /webApi/attd03/update-leave-types` (prafta-044-1, 계약 불변).
- 검증 상태: Claude 분석.

### UI-044-2 ManualGrantPop (부여 일수 1일 단위)
- 연결 작업: prafta-044-2
- 화면 위치: `src/views/attd/popup/ManualGrantPop.vue` (기존 팝업 보완)
- 참조 패턴: 동일 팝업 기존 부여 일수 input
- 변경 전: 부여 일수 `min=0.5 step=0.5`, 0.5단위 검증 → 0.5일 증가.
- 변경 후: `min=1 step=1`, 정수 검증 → 1일 단위만.
- 레이아웃: 변경 없음(입력 속성/검증만 변경).
```
┌ 부여 일수 * ──────┐  ┌ 사용 가능일 * ──┐
│ [   1    ] 일      │  │ [ 2026-06-03 ]   │
└──────────────────┘  └─────────────────┘
```
- 상태별 동작: 비정수/0 이하 입력 시 alert로 차단(FE 1차), 서버 isValidGrantDays 2차 권위.
- 백엔드 의존: `POST /webApi/attd09/leave-grant/manual-grant`·`/bulk-manual-grant` (prafta-044-2, 검증 강화).
- 검증 상태: Claude 분석.

> Vue 골격 전체 재작성은 불필요(기존 팝업 부분 수정). developer가 위 변경 지점만 편집. planner는 신규 .vue 골격 파일을 쓰지 않음.
