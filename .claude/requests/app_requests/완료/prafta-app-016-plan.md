# prafta-app-016 작업 분해 (planner)

앱 초과근무 신청 화면 개선 — (1) 근태 기반 카드 프리필, (2) OT 유형 입력 제거, (3) 구간별 등록 가능 시간 표시.

- 요청서: `.claude/requests/app_requests/prafta-app-016.md`
- 작업 영역: 모바일 앱 (`PRAFTA/prafta-app-frontend` + 백엔드 `PRAFTA/prafta-backend` app 영역 req07)
- 정책서 출처 정독 완료: `attd/09-requests-approval.md` §9.3 / §9.6, `attd/10-attendance-calc.md` §10.1~§10.3, `request-approval/06-approval-flows.md` §6.3 + `07-interactions.md` §7.6 (INDEX 경유 확인).

---

## 0. 핵심 사실 확인 (코드/스키마/정책 정독 결과)

### 0-1. OT_TYPE 의 두 컬럼 (요청 #2 의 핵심)
- `tb_user_attd_req.OT_TYPE` (schema-full.sql L1044): **`DEFAULT NULL` (nullable)**. 앱 신청 INSERT 가 들어가는 곳. 즉 "신청값(상신값)".
- `tb_user_overtime_mgmt.OT_TYPE` (L1185): **`NOT NULL`**. 최종 초과근무 정산 레코드. 웹 승인 단계에서만 INSERT 됨.
- 웹 승인 흐름(`Attd07ServiceImpl.updateUserOvertimeRequest`, L952~991)은 최종 OT 행의 `OT_TYPE` 을 **승인 단계에서 관리자가 보낸 값**(`UpdateUserOvertimeRequestParam` → `OvertimeItemModel.otType` → `InsertUserOvertimeCommand.otType`, L91)으로 채운다. **앱 신청 행의 OT_TYPE 을 자동 승계하지 않는다.**
- 따라서 앱 폼에서 OT_TYPE 입력을 제거해도 최종 정산 레코드 무결성에는 영향이 없다 — 관리자가 승인 시 유형을 확정한다.

> 정책 근거: §9.3.4 "시간 조정 후 승인(시스템 계산 참고값 함께 표시)", §9.6.3 "(초과근무) 계산 비교 3단: 시스템 계산값 / 상신값 / 승인값", request-approval §6.3 + §7.6(시간 조정 후 승인). → **유형 확정은 관리자(승인 단계)의 책임**임이 정책으로 뒷받침됨.

### 0-2. 컨텍스트는 이미 충분 (요청 #1·#3 신규 API 불필요)
- `MyAttendanceView.buildContextFromDay`(L666~)가 day-detail 응답의 `slots` 를 그대로 `context.slots` 로 전달(L677, L622). `AttdRequestView`(L130~135)는 `payload.slots` 를 그대로 body 로 전송 — **OvertimeForm 이 `context.slots` 만으로 프리필/윈도우 표시가 가능**. 신규 entry-context 불필요.

### 0-3. ⚠️ 슬롯 attendance 필드명 — 기존 AttdCorrectionForm 의 잠복 버그
- 백엔드 `AttendanceResponse`(직렬화 키): `checkInDate / checkInTime / checkOutDate / checkOutTime` (`AppAttd01ServiceImpl` L319~328 builder 확인).
- `ScheduleResponse`: `startTime / endTime / breakMinutes / workMinutes` (HHMM).
- `StandardizedResponse`: `applied / startTime / endTime / settledMinutes`.
- **AttdCorrectionForm `buildInitialSlots`(L142~149)는 `s.attendance?.startTime / endTime` 을 읽는다 — 이 키는 attendance 응답에 존재하지 않는다(`checkInTime/checkOutTime` 이 정답).** 즉 보정 폼의 근태 프리필이 사실상 빈 값을 채우고 있을 가능성이 높은 잠복 버그.
- prafta-app-016 OvertimeForm 프리필은 **올바른 키(`checkInTime/checkOutTime`)** 로 구현한다. (AttdCorrectionForm 동일 버그 수정은 본 작업 범위 밖 — `prafta-app-016-FU1` 후속 메모로 남김. 회귀 위험 분리.)

### 0-4. 등록 가능 시간 산식 (요청 #3) — 정책 정합
- 정책 §10.1: 정규 근무는 스케줄 구간 기준. 스케줄 시작 전 출근/스케줄 종료 후 퇴근은 정규로 인정 안 함(=초과 후보).
- 정책 §9.3.3: "조기 출근(스케줄 시작 전)"·"연장(스케줄 종료 후)"·"스케줄 없는 날 전량"·"구간 수 초과분 전체" 가 초과근무 상신 대상.
- 산식: 구간별 등록가능 윈도우 = (실 근무 구간) − (스케줄 구간).
  - 앞 OT = `실출근(checkInTime) ~ 스케줄시작(schedule.startTime)` (실출근 < 스케줄시작 일 때)
  - 뒤 OT = `스케줄종료(schedule.endTime) ~ 실퇴근(checkOutTime)` (실퇴근 > 스케줄종료 일 때)
  - 스케줄이 없는 구간(`schedule == null`) = 그 구간 실근무 전체가 등록가능 (§9.3.3 "스케줄 없는 날 전량").
  - 실근무가 스케줄 안에 완전히 포함되면 등록가능 윈도우 없음 → "등록 가능한 초과 시간이 없어요" 안내.

---

## 작업 분해 결과

### prafta-app-016-1 — 백엔드: 초과근무 신청 OT_TYPE 제거 (allow-list/095 폐기, NULL 저장)
- **유형**: backend
- **영역**: app
- **모듈**: req/req07
- **작업 유형**: 보완
- **요구사항 요약**: 앱 초과근무 신청에서 OT_TYPE 사용자 입력을 받지 않도록 백엔드 검증/저장을 조정. 최종 유형은 웹 승인 단계에서 관리자가 확정(기존 동작).
- **상세 설명**:
  - [Phase 1]
  - 정책서 출처: `attd/09-requests-approval.md` §9.3.4 / §9.6.3, `request-approval/06-approval-flows.md` §6.3, `request-approval/07-interactions.md` §7.6.
  - 핵심 요구사항:
    1) `registerOvertime`(`AppReq07ServiceImpl` L238~244)의 OT_TYPE allow-list 강제 + `ATTD_400_095` 발화를 **제거**한다.
    2) INSERT 시 `AttdReqInsertCommand` 의 `otType` 인자를 **`null`** 로 전달(L271 `s.getOtType()` → `null`). `tb_user_attd_req.OT_TYPE` 가 nullable 이므로 INSERT SQL/매퍼 무변경.
    3) `SlotRequest.otType` 필드는 **계약 호환을 위해 보존**하되(다른 endpoint 와 공유 DTO, 제거 시 영향 큼) 초과근무 경로에서 무시한다. 필드 Javadoc 에 "초과근무: 미사용(서버 무시, 유형은 승인 단계 확정)" 로 갱신.
    4) `ATTD_400_095` 에러 코드 자체는 **삭제하지 않고 유지**(다른 참조 가능성·롤백 안전). 단 OT register 경로에서는 더 이상 던지지 않음. (참조처 0건 확인은 developer 가 grep 으로 검증.)
    5) 웹 승인측(`tb_user_overtime_mgmt.OT_TYPE` NOT NULL)은 무변경 — 관리자 승인 시 유형 확정(기존 흐름).
  - 영향 받는 파일:
    - (백엔드) `prafta-backend/.../app/req/req07/service/impl/AppReq07ServiceImpl.java` (L60 `ALLOWED_OT_TYPES`, L238~244 검증, L271 INSERT 인자)
    - (백엔드) `prafta-backend/.../app/req/req07/dto/request/SlotRequest.java` (Javadoc 갱신만)
  - 영향 받는 endpoint: `POST /appApi/req07/overtime`
  - 예상 산출물: service 수정 (검증 블록 제거 + INSERT otType=null), DTO Javadoc
  - 스키마 영향: **없음**(컬럼 추가/변경/마이그레이션 없음, OT_TYPE nullable 활용).
- **선행 작업**: 없음
- **우선순위 근거**: 법적 책임 영역(attd) +1 격상. 프론트 #2(칩 제거)의 선행 — 백엔드가 NULL 을 수용해야 프론트가 미전송 가능.

---

### prafta-app-016-2 — 프론트: OvertimeForm 근태 프리필 + 유형 칩 제거 + 등록가능시간 표시
- **유형**: frontend-screen
- **영역**: app
- **모듈**: req (OvertimeForm 컴포넌트)
- **작업 유형**: 보완
- **요구사항 요약**: OvertimeForm 을 `context.slots` 기반으로 존재 구간 모두 카드 프리필(실 출퇴근 시각), OT 유형 칩 제거, 각 카드에 구간별 등록 가능 시간(앞/뒤 OT 윈도우) 표시.
- **상세 설명**:
  - [Phase 1]
  - 정책서 출처: `attd/09-requests-approval.md` §9.3.1/§9.3.3, `attd/10-attendance-calc.md` §10.1.
  - 핵심 요구사항:
    1) **프리필(요청 #1)**: `buildInitialSlots` 를 `context.slots` 기반으로 변경. 존재하는 구간 모두 카드 생성(최대 2). 시작=`attendance.checkInTime`(+`checkInDate`), 종료=`attendance.checkOutTime`(+`checkOutDate`). **올바른 키 사용**(0-3 참조). `attendance == null` 인 구간은 빈 값 카드(스케줄만 있고 근태 없음). `context.slots` 비어있으면 기존 폴백(1구간 빈 카드).
    2) **유형 제거(요청 #2)**: `OT_TYPE_OPTIONS` 상수, 유형 칩 `<label>`(template L53~67), `slot.otType` 초기값/`onAddSlot`·`isValid` 의 `otType` 조건, `onSubmit` emit 의 `otType` 키를 모두 제거. emit payload 에서 `otType` 미포함 → AttdRequestView 무변경(payload.slots 통과)·백엔드 NULL 저장(016-1).
    3) **등록가능시간 표시(요청 #3)**: 각 카드에 구간별 윈도우 표시 영역 추가. 산식(0-4): 앞 OT=`실출근~스케줄시작`, 뒤 OT=`스케줄종료~실퇴근`. `slot.schedule == null` 이면 "이 구간 근무 전체 등록 가능". 윈도우 없으면 "등록 가능한 초과 시간이 없어요". **표시 전용 계산**(프론트), 차단 아님. 자정 넘김은 schedule/attendance 의 date 필드(checkInDate/checkOutDate) 기준 단순 비교(1차).
    4) "구간 추가" 버튼은 **유지**하되, 프리필로 이미 2구간이면 자동 숨김(`v-if="slots.length === 1"` 기존 로직 유지). 근태 없는 1구간만 있을 때 수동 추가 케이스 대비.
    5) `workSeq` 는 구간 식별자(위치 재인덱싱 금지) 규칙 유지 — prafta-app-007 메모리 준수.
  - 영향 받는 파일:
    - (프론트) `prafta-app-frontend/.../src/views/req/components/OvertimeForm.vue` (전면 보완)
    - (확인만, 변경 없음) `AttdRequestView.vue`(payload.slots 통과 확인), `MyAttendanceView.vue`(context.slots 전달 확인)
  - 영향 받는 endpoint: `POST /appApi/req07/overtime` (body 에서 otType 제거)
  - 예상 산출물: OvertimeForm.vue 컴포넌트 (script 프리필/윈도우 계산 로직 + template 윈도우 표시 + 칩 제거)
  - 연결 UI 명세: UI-(메인 세션이 도메인 지식 베이스 최대 UI- 번호 +1 로 채번)
- **선행 작업**: prafta-app-016-1 (백엔드가 NULL 수용해야 칩 제거 후 미전송 안전)
- **우선순위 근거**: 법적 책임 영역(attd) +1. 016-1 의존(API 가 NULL 수용 선행).

---

## 의존성 그래프

```
016-1 (BE: OT_TYPE 제거/NULL 저장)  ──선행──▶  016-2 (FE: 칩 제거 + 프리필 + 윈도우 표시)
```

- 016-1 은 단독 배포 가능(기존 칩 전송도 무시되므로 무해 — 칩값을 받아도 NULL 저장).
- 016-2 는 016-1 이 머지된 뒤 검증해야 안전(NULL 전송 수용 확인).

---

## 확정한 설계 결정 (요청 5개 항목)

| # | 결정 | 근거 |
|---|------|------|
| 1 프리필 | `context.slots` 의 존재 구간 모두 카드화, 실 출퇴근(`checkInTime/checkOutTime`)으로 프리필. 근태 없는 구간=빈 카드. "구간 추가" 유지(1구간만일 때만). | 요청 #1, AttdCorrectionForm 패턴 + 0-3 키 교정 |
| 2 유형 | FE 칩 완전 제거. 신청 행 `tb_user_attd_req.OT_TYPE`=**NULL** 저장. 최종 유형은 **웹 승인 단계 관리자 확정**(기존 구현). allow-list/095 폐기(에러코드는 보존). | 요청 #2, 0-1, §9.3.4·§9.6.3, request-approval §6.3·§7.6 |
| 3 등록가능시간 | **프론트 표시 전용** 계산(전체근무−스케줄, 앞/뒤 윈도우). 차단 아님. 서버 권위 검증은 웹 승인측 `selectAllowedWindow` 가 이미 수행(중복 정의 회피). | 요청 #3, §10.1·§9.3.3, web attd07 `OvertimeAllowedWindowQuery` 재사용 불요(앱은 표시만) |
| 4 계약/스키마 | 신규 컬럼/테이블/마이그레이션 **없음**. `SlotRequest.otType` 필드 보존(공유 DTO)·초과근무 경로 무시. `OvertimeRequest`/`AttdReqInsertCommand` 구조 무변경(otType=null 전달). day-detail 응답 무변경(기존 slots 충분). 신규 OT entry-context **불필요**. | 0-2, nullable 컬럼 활용 |
| 5 정책 출처 | §9.3(상신)·§9.3.3(발생케이스)·§9.3.4(승인액션)·§9.6.3(3단 비교)·§10.1(정규계산)·§10.3(추가근무계산)·request-approval §6.3·§7.6. 각 작업에 명시. | INDEX 경유 정독 완료 |

---

## 채팅으로 확인 필요한 질문 (자율 진행으로 기본값 채택, 사용자 확정 권장)

1. **[유형 자동 파생 vs 관리자 확정]** 요청 #2 는 "연장/야간/휴일 구분은 시스템이 판단할 일"이라고 함. 본 분해는 **(b) 신청 시 NULL 저장 + 승인 단계 관리자 확정**을 기본값으로 채택했다(정책 §9.3.4/§9.6.3 가 "승인값"을 관리자 책임으로 규정, 최종 컬럼 NOT NULL 도 승인 단계에서 채워짐). **서버 자동 파생(a)** 도 가능하나, 야간(22~06시)·휴일(공휴일/주휴) 판정 규칙이 정책서에 정량 정의돼 있지 않아(휴일 캘린더 테이블·야간 시간대 상수 부재) 추측 구현은 금지 규칙에 저촉된다. 자동 파생을 원하면 야간 시간대·휴일 소스(테이블/기준)를 확정해 별도 요청서로 분리 필요. → **이 기본값(b)으로 진행해도 되는지, 자동 파생을 원하는지** 확인.

2. **[등록가능시간 산출 위치]** 프로젝트 원칙은 "비즈니스 판정 서버 산출"이나, 등록가능시간은 **차단 게이트가 아니라 단순 안내 표시**이고 데이터(schedule/attendance)가 이미 `context.slots` 에 있다. 권위 있는 윈도우 검증은 웹 승인측이 이미 보유. 그래서 **프론트 표시 전용 계산**을 기본값으로 채택. 만약 앱에서도 서버 산출 윈도우를 응답으로 받길 원하면 day-detail 슬롯 응답에 `allowedOtWindows` 필드 추가(신규 백엔드 작업)로 확장 가능. → **표시 전용으로 충분한지** 확인.

3. **[AttdCorrectionForm 잠복 버그]** 0-3 의 보정 폼 `startTime/endTime` 키 오류는 본 작업 범위 밖으로 두고 후속(`prafta-app-016-FU1`)으로 분리했다. 보정 폼 근태 프리필이 실제로 빈 값일 가능성이 있으므로 **함께 수정할지** 확인(회귀 위험 분리 차원에서 기본값=분리).

---

## 메인 세션이 Notion 에 반영할 항목

### 작업 로그 DB (작업ID = PRAFTA-{기존 최대+1}부터 순차)
- **prafta-app-016-1**: 영역 app / 모듈 req/req07 / 작업유형 보완 / 상태 분해완료 / 담당 planner
  - 상세: `[backend]` OT_TYPE allow-list·ATTD_400_095 폐기, 신청 행 OT_TYPE=NULL 저장(승인 단계 관리자 확정). 파일 `AppReq07ServiceImpl`(L60/238~244/271)·`SlotRequest`(Javadoc). endpoint `POST /appApi/req07/overtime`. 마이그레이션 없음. 정책 §9.3.4/§9.6.3.
  - 산출물: (백엔드, 비움)
- **prafta-app-016-2**: 영역 app / 모듈 req / 작업유형 보완 / 상태 분해완료 / 담당 planner
  - 상세: `[frontend-screen]` OvertimeForm 근태 프리필(context.slots, checkInTime/checkOutTime)+유형칩 제거+구간별 등록가능시간 표시(표시전용). 선행 016-1. `[UI 명세: UI-XXX]`. 파일 `OvertimeForm.vue`. 정책 §9.3.1/§9.3.3/§10.1.
  - 산출물: `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/req/components/OvertimeForm.vue`

### 도메인 지식 베이스 DB (frontend 명세)
- 이름: `UI-XXX OvertimeForm(초과근무 신청 개선)` / 영역 app / 모듈 req
- 현재 동작: 1구간 빈 카드 1개 초기화·유형 칩 필수 입력·등록가능시간 안내 없음.
- 의도된 동작: 아래 화면 명세 markdown 전체.
- 검증 상태: Claude 분석
