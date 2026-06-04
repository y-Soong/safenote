# prafta-app-016 — 앱 초과근무 신청 화면 개선(근태 기반 프리필 + 유형 제거 + 구간별 등록가능시간 표시)

작업 영역: 모바일 앱(`PRAFTA/prafta-app-frontend` + 백엔드 `PRAFTA/prafta-backend` app 영역 req07)

## 배경 / 현재 동작

앱 초과근무 신청 화면(`OvertimeForm.vue`, 라우트 `/AttdRequest?type=overtime`)은 현재:
- 슬롯을 항상 **1구간 1개·시간 공란**으로 초기화하고, 사용자가 "구간 추가"를 눌러야 2구간이 생긴다(`OvertimeForm.vue` `slots` 초기값 159~168행, `onAddSlot` 220행).
- 각 구간 카드에 **유형(OT_TYPE)** 칩(연장/야간/휴일)을 사용자가 직접 선택(필수). 백엔드 `registerOvertime`가 OT_TYPE allow-list를 강제(`AppReq07ServiceImpl` 238~244행, 누락/오류 시 ATTD_400_095)하고 `OT_TYPE` 컬럼에 저장.
- 구간별 "등록 가능 시간" 안내 없음.

비교: 근태 보정 폼(`AttdCorrectionForm.vue`)은 이미 `context.slots[*].attendance.startTime/endTime`로 **존재하는 구간 모두를 실근무 시각으로 프리필**한다(`buildInitialSlots` 142~162행). 초과근무 폼만 이 패턴을 쓰지 않는다.

컨텍스트 전달: 이전 화면(`MyAttendanceView.vue` `buildContextFromDay` 666~)이 day-detail 응답을 sessionStorage(`attd_req_ctx_v1`)에 저장 → `AttdRequestView.vue`가 읽어 폼에 `context`로 주입. `context.slots`는 day-detail의 슬롯 배열로, 구간별 `schedule`(스케줄 시각)·`attendance`(실 출퇴근 시각)를 포함한다(`SlotResponse`).

테스트 케이스: `USER_CD='20260400013'`, 금일 1구간·2구간 근태가 모두 존재. 현재는 초과근무 신청 시 1구간 카드 1개만(시간 공란) 뜬다.

## 요청 (사용자 의도)

1. **근태 기반 카드 프리필**: 기존 근태가 있으면 해당 근무 구간 카드가 미리 세팅돼 있어야 한다. 위 테스트처럼 1·2구간 근태가 모두 있으면 **두 구간 카드가 모두 나오고**, 각 카드의 시간이 **기존 근무 시간(실 출퇴근)으로 자동 세팅**돼 있어야 한다. (보정 폼과 동일하게 `context.slots` 활용.)
2. **유형(OT_TYPE) 입력 제거**: 1·2구간 카드 아래의 "유형" 값은 사용자에게 받지 않는다. 연장/야간/휴일 구분은 **시스템이 판단**할 일이다. → FE에서 유형 칩 제거 + 백엔드가 OT_TYPE를 사용자 입력 없이 처리(서버 파생 또는 승인 단계 판단)하도록 조정.
3. **구간별 등록 가능 시간 표시**: 각 구간 카드에 등록 가능한 시간 범위를 표시한다. 산식은 **전체 근무시간 − 스케줄 시간**으로, 스케줄 윈도우 앞/뒤로 등록 가능한 구간(예: 실출근~스케줄시작 = 앞 초과, 스케줄종료~실퇴근 = 뒤 초과)을 보여 준다.

## 제약 / 참고

- 백엔드 1벌(`PRAFTA/prafta-backend`)을 앱이 사용. app 프리픽스 `/appApi`. req07 모듈. JWT 세션 클레임/IDOR 가드(`OvertimeParam.from`) 유지.
- 프로젝트 원칙상 비즈니스 판정/게이팅은 서버 산출이 원칙(프론트 비즈니스 판정 지양). OT_TYPE 파생·등록가능 윈도우 산출을 **서버에서 할지(권장 검토)** 프론트에서 표시 계산만 할지 planner가 결정. 단 등록가능시간은 단순 표시이고 데이터(schedule/attendance)가 이미 컨텍스트에 있으므로 프론트 표시 계산도 가능 — 정합/단일출처 관점에서 판단.
- OT_TYPE 컬럼(`TB_USER_ATTD_REQ` OT_TYPE)·`AttdReqInsertCommand`·SlotRequest 계약 변경 영향 검토. 유형 제거 시 백엔드 검증(ATTD_400_095)·INSERT 값·웹 승인측(attd07) 표시와의 정합 확인.
- "전체 근무시간 − 스케줄 시간" 및 연장/야간/휴일 정의는 근태관리 정책서(§5 초과근무/§6 스케줄, 표준화 방향 포함)에서 출처 확인. planner가 INDEX 경유 정독 후 명시.
- 자정 넘김(야간 2구간)·prafta-app-015(구간 선택)·표준화(출근올림/퇴근내림) 등 기존 정책과 충돌 없게.
- 영향 파일(추정): FE `OvertimeForm.vue`, (필요시 `AttdRequestView.vue`/`MyAttendanceView.vue` 컨텍스트). BE `AppReq07ServiceImpl`(registerOvertime), `OvertimeRequest`/`SlotRequest`/`OvertimeParam`, `AttdReqInsertCommand`, `AttdErrorCode`(095 운명), 필요시 day-detail 슬롯 응답 또는 신규 OT entry-context.

## 처리 방식

CLAUDE.md 에이전트 워크플로우 준수: planner → developer → qa → security. 메인 세션이 Notion 작업 로그 대행.
