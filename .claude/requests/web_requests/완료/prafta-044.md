# prafta-044 — 관리자 수동부여 연차: 타입에 사용단위 입력 + 수동부여는 1일 단위 부여

작업 영역: 웹(`PRAFTA/prafta-web-frontend`) + 백엔드(`PRAFTA/prafta-backend`)

## 배경 / 문제

웹 "사용자 연차관리" 화면 → row 더블클릭 팝업 → "수동 부여" 버튼 → `ManualGrantPop` 에서 부여 일수가 **무조건 0.5 단위로만** 올라간다. 원인 조사 결과:

1. **연차 타입 관리(`LeaveTypeCreatePop`)에 관리자 수동부여 타입용 "사용단위" 입력 칸이 없다.**
   - 사용단위(`USE_UNIT_TYPE`, SYS025: `00`=1일/`01`=반차/`02`=시간차2h/`03`=1h/`04`=30분) 입력은 `leaveType==='01'`(사용자 신청) 섹션에만 존재.
   - `leaveType==='02' && grantType==='02'`(관리자 수동부여) 섹션엔 사용단위 입력이 없고, 저장 로직이 `useUnitType: leaveType==='01' ? useUnitType : null` 이라 **수동부여 타입은 USE_UNIT_TYPE가 항상 null로 저장**된다.
   - 실제 데이터: 수동부여 타입(`00018 LEAVE_ADMIN_MANUAL`, LEAVE_TYPE='02'/GRANT_TYPE='02')의 USE_UNIT_TYPE = null.

2. **수동부여 팝업(`ManualGrantPop`)이 부여 일수를 0.5로 하드코딩**: `<input step="0.5" min="0.5">` + 검증 "0.5 단위(값×2 정수)". 선택 타입의 단위와 무관.

## 확정 설계 (사용자 결정)

부여(grant)와 사용(consume) 단위를 분리한다:

- **D1. 연차 타입 관리 팝업에 사용단위 입력 노출**: `LeaveTypeCreatePop`의 **관리자 수동부여 섹션(leaveType='02' && grantType='02')에도 "사용단위"(SYS025) 입력을 추가**하고, 저장 시 USE_UNIT_TYPE를 **null이 아니라 입력값으로 영속화**한다. 백엔드 연차타입 생성/수정 저장이 이 타입에 대해 USE_UNIT_TYPE를 받도록 한다.
- **D2. 수동부여는 항상 1일 단위 부여**: `ManualGrantPop`의 부여 일수는 **1일 단위(정수)로만** 입력/증가(step=1, min=1, 정수 검증). 0.5 하드코딩 제거. 선택 타입의 USE_UNIT_TYPE와 무관하게 부여는 1일 단위. (수동부여 팝업은 USE_UNIT_TYPE를 알 필요 없음.)
- **D3. 사용 시점에 사용단위 적용**: 사용자가 해당 (수동부여된) 연차를 **사용/신청할 때**는 타입 생성 시 입력한 USE_UNIT_TYPE 단위(일/반차/시간차)에 따라 입력하게 한다. → 기존 연차 사용/신청 흐름이 USE_UNIT_TYPE를 읽어 적용(앱 leaveflow 매퍼가 이미 `USE_UNIT_TYPE AS useUnitType` 조회). 본 작업은 "수동부여 타입에 단위가 실제로 저장되게" 해서 이 소비 단위가 동작하도록 만드는 것.

## 세부 / 검토 포인트 (planner)

- D1 사용단위 드롭다운: 수동부여 섹션에서도 SYS025 전체 노출(일/반차/시간차) — 부여는 1일 단위로 분리됐으므로 시간차 환산 문제 없음. 편집모드(isEditMode) 편집 가능 여부는 `leaveType==='01'` 패턴(생성시 입력·편집시 disabled)과 일관되게 할지 planner 판단(권장: 일관). 필수(*) 여부도 '01' 패턴 참고.
- D1 백엔드: 연차타입 저장(생성/수정) 경로가 LEAVE_TYPE='02'/GRANT_TYPE='02'에 대해 USE_UNIT_TYPE를 INSERT/UPDATE 하도록 수정. 기존 '01' 저장 로직과 컬럼 정합. (DTO/Param/Command/Mapper.)
- D2 수동부여: `ManualGrantPop` template step/min=1, 검증을 "정수(1일 단위)"로 교체. 백엔드 수동부여(`attd09/leave-grant/manual-grant`·`bulk-manual-grant`) 검증도 정수 일수 강제(서버 권위) 확인/정정. `ManualTypesResponse`/`selectManualGrantTypes`에 USE_UNIT_TYPE 추가는 **불필요**(부여는 단위 무관).
- D3 소비: 수동부여 타입 USE_UNIT_TYPE가 null일 때(기존 데이터/미설정) 소비 단위 기본값(=1일/풀데이) 동작 확인. 단위 설정 후 사용/신청(웹·앱 leaveflow)이 그 단위로 동작하는지 회귀 확인. (신규 구현보다 검증/정합 위주 — 갭 발견 시 보고.)
- 관련 컬럼: `TB_LEAVE_TYPE_MGMT.USE_UNIT_TYPE`(varchar2, SYS025). 신규 컬럼/마이그 불필요(컬럼 이미 존재). 코드성 컬럼 COMMENT 규칙은 스키마 변경 없으니 해당 없음.
- 정책서: 연차 사용단위/수동부여 관련 섹션(공통/근태 정책) INDEX 경유 확인. prafta-024(USAGE_UNIT 단일화)·prafta-031(수동부여 회수)·prafta-032(수동조정) 맥락 참고.

## 영향 파일 (추정)
- 웹FE: `views/attd/popup/LeaveTypeCreatePop.vue`(수동부여 섹션 사용단위 입력+저장), `views/attd/popup/ManualGrantPop.vue`(1일 단위).
- 백엔드: 연차타입 저장 모듈(LeaveTypeCreatePop 저장 대상 — baim/연차타입 관리 컨트롤러/서비스/매퍼), `web.attd.attd09`(수동부여 일수 정수 검증), (검증) 연차 사용/신청 leaveflow.

## 처리 방식
CLAUDE.md 에이전트 워크플로우: planner → developer → qa → security. 메인 세션 Notion 작업 로그 대행.
