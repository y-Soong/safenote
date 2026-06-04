# prafta-045 — 연차 타입 "사용 가능 기간" enforcement (타입 설정 → 부여 유효일 → 소비/만료 반영)

작업 영역: 백엔드(`PRAFTA/prafta-backend`) 중심(common.cmm.leave 부여엔진/소비) + 필요 시 web/app leaveflow. 화면 변경 최소.

## 배경 / 문제 (prafta-044 후속, QA 발견)

연차 타입(`TB_LEAVE_TYPE_MGMT`)의 "사용 가능 기간"(SYS026: `01`설정안함/`02`해당연도내/`03`기간설정)이 **저장만 되고 실제로 enforce되지 않는다.** prafta-044/FU2로 입력·저장·포맷(관리자 ADMIN_AVAIL_* YYYYMMDD)은 정합됐으나, 그 값이 부여/소비/만료에 영향을 주지 않는다.

### 이미 존재하는 인프라 (재사용 — 신규 구축 아님)
- `TB_USER_LEAVE_GRANT`: `AVAIL_FROM_DATE`/`AVAIL_TO_DATE`(YYYYMMDD, 소멸일), `STATUS`(SYS040 ACTIVE/EXHAUSTED/EXPIRED/CANCELED), `EXPIRE_YN`, `EXPIRE_DATE`.
- **만료 배치 존재**: `LeaveGrantStatusServiceImpl.expireOverdueGrants()` + `resolveNextStatus`(AVAIL_TO_DATE<오늘 → EXPIRED, EXPIRE_YN='Y'). 정책서 §8.5.8 STATUS↔EXPIRE_YN 매핑.
- **소비**: `AppLeaveFlowServiceImpl`/(web) leaveflow가 차감 시 "만료 임박 우선, 잔여 충분" 부여를 선택(FOR UPDATE). 만료/소진 부여는 제외.

### 진짜 갭
부여 시 `AVAIL_TO_DATE`(소멸일)가 **타입의 사용가능기간 설정이 아니라 회사 공통 유효개월(validityMonths/AXIS6)** 로 계산된다(`LeaveGrantEngineServiceImpl.buildGrantContext` → `resolveValidityMonths(cmpnyCd)` → `addMonthsYyyymmdd(today, validityMonths)`). 수동부여(`LeaveDashboardMapper.insertManualGrant`)는 폼의 `availFromDate` 입력을 쓰고 AVAIL_TO_DATE는 별도 산정/미설정. → **타입별 "사용 가능 기간"(01/02/03 + 날짜) 설정이 부여 유효일에 반영되지 않아, 소비창 제한·만료가 타입 설정대로 동작하지 않는다.**

## 요청 (목표)

연차 타입의 "사용 가능 기간" 설정이 **부여 시 그 부여건의 `AVAIL_FROM_DATE`/`AVAIL_TO_DATE`로 산출**되어, 기존 만료 배치 + 소비 차감이 타입 설정대로 enforce되게 한다.

산출 규칙(타입 avail-term → 부여 유효일):
- `01` 설정안함: 무기한 → `AVAIL_TO_DATE` 미설정(null) = 소멸 없음(만료배치 isExpired가 null을 만료로 보지 않는지 확인).
- `02` 해당 연도 내: `AVAIL_TO_DATE` = 부여 연도의 `YYYY1231`.
- `03` 기간 설정: 타입의 from/to를 부여건 유효일로. 관리자 타입은 `ADMIN_AVAIL_FROM/TO_DT`(YYYYMMDD 절대일, 044-FU2), 사용자신청 타입은 `AVAIL_FROM/TO_DT`(MMDD 월일 → 부여 연도로 연도 해석).

## 검토 / 결정 포인트 (planner)

1. **적용 범위(부여 경로)**: 수동부여(`insertManualGrant`)·관리자 자동부여·정책 부여엔진(`LeaveGrantEngineServiceImpl`) 중 어디에 타입 avail-term 산출을 적용할지.
2. **법정연차 vs 관리자/커스텀 타입 충돌(핵심)**: 정책 부여엔진은 의도적으로 **회사 공통 validityMonths(AXIS6)** 로 법정 본연차 소멸일을 계산한다(prafta-023/029/030). 타입 avail-term을 무조건 적용하면 법정연차 엔진과 충돌. → **법정(SYS_*/STATUTORY) 타입은 기존 엔진(validityMonths) 유지, 관리자수동/자동·비법정 타입만 타입 avail-term 적용**이 안전한지 planner 확정(정책서 §8.5 유효기간 규정 정독).
3. **수동부여 폼 availFromDate와의 관계**: 수동부여 팝업은 "사용 가능일(availFromDate)" 입력이 있다. 이 입력을 AVAIL_FROM_DATE로 쓰고 AVAIL_TO_DATE만 타입 term으로 산출할지, 아니면 타입 term이 from/to 모두 결정할지(그럼 폼 입력 의미 재정의). 일관성 결정.
4. **01 무기한 처리**: AVAIL_TO_DATE=null이 만료배치/소비/대시보드(MIN(AVAIL_TO_DATE) nearestExpire 등)에서 안전한지 전수 확인(NULL 취급).
5. **소비창(AVAIL_FROM_DATE) enforce**: 시작일 이전 신청 차단이 필요한지(대부분 즉시 사용이라 from은 부여일=today일 가능성). 신청 시 AVAIL_FROM_DATE~AVAIL_TO_DATE 창 밖 거부를 web/app leaveflow에 추가할지(현재 만료분 제외만 하는지 확인).
6. **소급/기존 데이터**: 기존 부여건의 AVAIL_TO_DATE를 백필할지(기본: 신규 부여분만 적용, 백필 안 함). 마이그 없음 가능성(컬럼 기존재).
7. **정책서 출처**: 근태/연차 정책서 §8.5(유효기간/소멸) INDEX 경유 정독, 충돌 시 우선순위.

## 영향 파일 (추정, planner 확정)
- BE: `common.cmm.leave` — `LeaveGrantEngineServiceImpl`(부여 컨텍스트 avail 산출), `LeaveDashboardServiceImpl`/`LeaveDashboardMapper`(insertManualGrant), `LeaveGrantStatusServiceImpl`(만료 — 주로 확인), 부여 시 타입 avail-term 조회(`TB_LEAVE_TYPE_MGMT`).
- 소비(필요 시): web/app leaveflow(`AppLeaveFlowServiceImpl`/web 대응) — AVAIL_FROM 창 enforce 추가 여부.
- 화면: 원칙적으로 없음(타입관리 입력은 044 완료). 필요 시 표시만.

## 처리 방식
CLAUDE.md 에이전트 워크플로우: planner → developer → qa → security. 메인 세션 Notion 대행. 법정연차 엔진 회귀 위험이 크므로 planner가 범위를 보수적으로(법정 제외) 잡고 결정 포인트를 명확히 보고.
