# prafta-app-018-C — [앱 FE] 연차 신청 폼 + 진입점

상위: `prafta-app-018-leave-apply-plan.md`. 018-A(메타 조회)·018-B(신청 쓰기) 선행. 앱 프론트(`prafta-app-frontend`)만.

## 목표
연차 신청 폼 화면 신규 + 두 진입점을 동일 폼으로 연결. 웹 `LeaveApplyPop.vue` UX 참조하되 모바일 단일 화면/시트로 재구성. 비TS·CSS변수·scoped·공통 컴포넌트(DateStepperField/TimeStepperField/BaseSelect 대응) 사용.

## 화면/입력 (LeaveApplyForm)
- **연차 종류** 선택: 018-A `apply-meta`의 신청가능 목록. 선택 시 그 종류의 `allowedUnits`/`balanceDays`/`aprvRequired` 로 폼 동적 구성.
- **사용 단위** 드롭다운: 선택한 종류의 `allowedUnits`(D2 게이팅)만 노출. 그 외 단위는 미표시(서버도 거부 — 이중).
- **날짜**: workYmd 선택. 시간차 단위면 **시작~종료 시각**(TimeStepper) 노출 + 안내("○○ 단위로 신청, 휴게시간 가로지름 불가").
- **사유** 입력(maxlength 정책).
- **결재선**(D1): `aprvRequired`일 때 노출. 프리셋 선택(018-A `approval-presets`, 기본 프리셋 자동적용) + 결재자 추가/순서(018-A `approver-search`). 결재 불필요 종류면 결재선 영역 숨김.
- **잔여 표시**: 선택 종류 `balanceDays` 표시. 신청 일수가 잔여 초과면 사전 안내(서버 051 도 표면화).
- 제출 → 018-B `POST /appApi/leaveflow/apply`. 성공 시 안내 후 이전화면/현황 갱신. 서버 에러(050/051/052/054/055/056/단위게이팅) 메시지 표면화(앱 인터셉터 토큰오류 오발동 주의 — ATTD_400_* 안전).

## 진입점 (D4)
- `MyAttendanceView.onSheetAction` 의 `leave` 분기: 기존 `showAlert('준비 중입니다')`(약 731~732행) 제거 → 신규 폼으로 라우팅(보정/초과근무와 동일 패턴: 컨텍스트 sessionStorage 저장 후 router.push). day 컨텍스트(workYmd/nodeCd/siteName) 전달.
- `MyLeaveSummaryView` "연차 신청하기" 버튼(약 180행 stub): 동일 폼으로 라우팅(이 진입은 특정 일자 컨텍스트 없을 수 있음 → 폼에서 날짜 직접 선택).
- 라우터에 신규 화면 등록(앱 FE 라우팅 규약 따름).

## 수용 기준
- 종류별로 단위 드롭다운이 다르게 열린다(D2). 시간차만 시각입력 노출. 결재 불필요 종류는 결재선 숨김.
- 두 진입점이 동일 폼으로 연결되고 "준비 중" 잔존 없음(연차 한정).
- workSeq류 식별자 위치 재인덱싱 금지(해당 시). 하드코딩 색상/문구 없음(CSS변수/공통). SFC 컴파일/eslint 통과.
- A/B 응답 키와 정확히 정합(필드명/타입). 잔여·단위·결재필요 플래그 출처는 서버(클라 추측 금지).

## 정책 출처
attd §8/§8.5/§9, prafta-019/020/024. UI는 화면 작업 규약(planner UI 명세 → script 채움).
