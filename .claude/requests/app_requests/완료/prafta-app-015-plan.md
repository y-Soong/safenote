# prafta-app-015 작업 분해 (planner)

요청서: `.claude/requests/app_requests/prafta-app-015.md`
작업 영역: 모바일 앱(`PRAFTA/prafta-app-frontend` + 백엔드 `PRAFTA/prafta-backend` app 영역)
처리 워크플로우: planner → developer → qa → security

---

## 0. 핵심 결론 (요약)

2구간 스케줄에서 출근 구간(1구간/2구간)을 **서버 시각 자동 추정(`isSecondSlotTimeWindow`)으로 결정하던 것을 사용자 명시 선택으로 전환**한다. 사용자가 선택한 구간이 곧 WORK_SEQ가 되며(1구간=WORK_SEQ 1, 2구간=WORK_SEQ 2), 순서 자유(2→1 가능)·1구간 누락 허용·각 구간 1회 제한을 적용한다.

- **DDL 변경 없음**: TB_USER_ATTD_MGMT.WORK_SEQ(int NOT NULL) 기존 컬럼 그대로. TB_SCH_MGMT.FST/SEC_SCH_*도 그대로. 신규 테이블/컬럼/코드 없음.
- **API 계약 변경**: `CheckInRequest`에 `targetWorkSeq`(Integer, 1|2|null) 추가. 오늘/일상세 응답의 `slots[]`에 구간별 "출근 가능/완료" 플래그 추가.
- **에러코드 운명**: 084/085 폐기 (자동 추정·1구간 선행 강제 가드 제거). 081(직전 미퇴근 재출근)·080(2회 초과)·082(전날 미퇴근)·083(연차일)은 유지. `confirmSkipPrevSlot`(Case C 확인) 폐기.
- **정책서 정정 필요**: 근태관리 §5장에 "야간 2구간 §5.5"가 .md로 존재하지 않음(코드 주석/메모리에만 §5.5 표기). 이번 변경으로 §5.5 자동추정·Case A/B/C가 폐기되므로, **CHANGELOG에 prafta-app-015 항목 추가 + 05-checkin-limits.md에 "2구간 출근 구간 선택" 규칙 명문화**가 필요(developer 영역, 단 정책서 .md 수정 여부는 사용자 확정 대기 — 아래 §6).

---

## 1. 확정한 설계 결정 (요청서 §"분해 시 반드시 다룰 설계 결정" 대응)

요청서 7개 결정 항목에 대한 planner 확정안:

### D1. 구간 선택 입력 — `targetWorkSeq` (Integer 1|2|null)
- `CheckInRequest`에 `Integer targetWorkSeq` 추가. `CheckInParam`도 동일 전파.
- 의미: 2구간 스케줄에서만 유효. 1구간 스케줄/스케줄 없는 날은 무시(서버가 강제 무시).
- 필드명 근거: 기존 코드가 "WORK_SEQ"를 구간 번호로 사용(주석/채번 일관). `slotSeq`보다 도메인 일관.

### D2. 자동 추정(`isSecondSlotTimeWindow`) 처리 — **제거 + 누락 시 400 강제**
- `isSecondSlotTimeWindow`/`circularMinuteDistance` 호출을 §5.5 분기에서 제거(메서드는 데드코드로 남기되 호출 0건; developer가 삭제 여부 판단).
- **2구간 스케줄인데 `targetWorkSeq`가 null/범위 외(1·2 아님)면 400 에러**(신규 코드 `ATTD_400_087` 또는 기존 080대 재사용 — 아래 §3-1). 구버전 호환(추정 유지) 안 함: 자동 추정이 문제의 근원이므로 명시 선택을 강제한다. 앱은 항상 최신이며(webview 번들), 구버전 클라 호환 부담 낮음.
- 1구간 스케줄/스케줄 없는 날은 `targetWorkSeq`가 와도 무시(있어도 동작 영향 없음 → 그 날의 채번 규칙은 기존대로).

### D3. 순서 자유화 — WORK_SEQ = 선택 구간 번호 직접 채번
- 2구간 스케줄: `workSeq = targetWorkSeq`(1 또는 2). `existing+1` 추정 폐기(2구간 스케줄 한정).
- WORK_SEQ=1 레코드 없이 WORK_SEQ=2 단독 INSERT 허용. 그 후 WORK_SEQ=1 추가 등록 허용(순서 무관).
- 1구간 스케줄/스케줄 없는 날: 기존 `existing+1` 채번 유지(이번 변경 대상 아님).
- TB_USER_ATTD_MGMT에 (USER+YMD+WORK_SEQ) UNIQUE 제약은 없음(현행) → 중복 차단은 §D4 애플리케이션 레벨 + 기존 `lockUserForCheckIn` 비관적 잠금으로 직렬화(TOCTOU 가드 기존 보존).

### D4. 중복 차단 — 구간별 1회 (서버 가드 + 프론트 버튼 비활성)
- 2구간 스케줄에서 `targetWorkSeq` 구간에 이미 WORK_SEQ 레코드 존재 → 차단. 기존 081("이전 근무 퇴근 후 재출근") 대신 **신규 코드 `ATTD_400_088`(이미 해당 구간 출근 등록됨)** 권장(메시지 명확화). developer가 081 재사용 여부 판단 가능.
- 응답 `slots[]`에 구간별 플래그(아래 §3-2)로 프론트가 해당 구간 버튼 disabled.
- 하루 2회 캡(prafta-app-014, 080)은 상위에서 유지: 2구간 스케줄도 최대 2레코드(=구간1+구간2)이므로 자연 정합.

### D5. 기존 §5.5 Case A/B/C·081/084/085 운명
| 가드 | 현행 | prafta-app-015 후 |
| --- | --- | --- |
| 자동 추정(`isSecondSlotTimeWindow`) | 사용 | **폐기**(호출 제거) |
| Case C / 084 / `confirmSkipPrevSlot` | 2구간 시도 시 1구간 없으면 소프트 차단 | **폐기**(1구간 누락 허용=사용자 선택으로 대체) |
| Case B / 085 (1구간 미마감 시 2구간 차단) | 강한 차단 | **폐기**(순서 자유화 요구와 정면 충돌 → 제거). 2구간 출근에 1구간 마감 선행 불요. |
| Case A (1구간 마감 후 2구간 즉시) | 정상 흐름 | 흡수(명시 선택이면 무관) |
| 081 (직전 미퇴근 재출근) | 2구간/1구간/스케줄없음 공통 | **유지하되 의미 한정**: "동일 구간 중복" 차단은 D4(088)로, 081은 "선택 구간이 이미 진행 중(미퇴근)인데 같은 구간 재출근" 같은 모순 입력 방어로 남김. 단 순서 자유화로 "1구간 미퇴근 상태에서 2구간 출근"은 **허용**(081 미발동). developer가 분기 정밀 재작성. |
| 080 (하루 2회 초과) | 유지 | 유지 |
| 082 (전날 미퇴근) | 유지 | 유지 |
| 083 (연차일) | 유지 | 유지 |

  ⚠️ developer 주의: Case B 폐기로 "1구간 미퇴근 + 2구간 출근"이 열린다. 이때 1구간은 퇴근 누락 상태로 남고 다음날 게이트(082)/보정 흐름으로 해소된다(§7.4). 이는 정책 §5.4 "퇴근 미등록 상태 재출근 차단"과 충돌 가능 → §6 정책서 정정 + qa/security 검토 포인트.

### D6. 프론트 구간 선택 UI
- **2구간 스케줄일 때만** 구간 선택 노출. 1구간/스케줄없음은 기존 단일 "출근하기" 버튼 유지(UI 무변경).
- 표현: AttendanceTodayCard(오늘 카드) 푸터 primary 버튼 영역에서, 2구간 스케줄이고 아직 출근 가능 구간이 있으면 "1구간 출근"·"2구간 출근" 2버튼 노출. 각 버튼은 해당 구간 이미 등록 시 disabled.
- MainView(메인 홈) 출퇴근 카드도 동일 게이팅. 단 MainView는 요약 카드이므로 "구간 선택은 근태 화면에서" 단순화 옵션 가능 → planner 권장: MainView도 2버튼 노출(일관). developer가 카드 폭 제약 시 조정.
- 자동추정/084 confirm 흐름 제거: `callCheckInOut`의 084 분기·`askConfirm('1구간 출근 데이터 없이...')` 제거. 대신 버튼 클릭 시 선택 구간을 `targetWorkSeq`로 전달.
- 086(외근 사유)·080/081/082/083 분기는 유지.

### D7. 정책서 영향 — §6 참조 (정책서 .md 정정 항목 별도 명시)

---

## 작업 분해 결과

### prafta-app-015-1
- **유형**: backend
- **영역**: app
- **모듈**: attd/attd01
- **작업 유형**: 보완
- **요구사항 요약**: 2구간 스케줄 출근 구간 자동추정을 폐기하고 `targetWorkSeq`(1|2) 명시 선택으로 전환. 순서 자유·1구간 누락 허용·구간별 1회 차단을 서버에서 강제.
- **상세 설명**:
  - [Phase 1]
  - 핵심 요구사항:
    1) `CheckInRequest`/`CheckInParam`에 `Integer targetWorkSeq` 추가(정규화: 1·2만 채택, 그 외 null). `confirmSkipPrevSlot` 폐기(필드 제거 또는 미사용 데드).
    2) `AppAttd01ServiceImpl.checkIn` §5.5 분기(약 930~975행) 재작성: `isTwoSlot`일 때 (a) `targetWorkSeq` null/범위외 → 400, (b) 선택 구간 기존 레코드 존재 → 차단, (c) WORK_SEQ=`targetWorkSeq` 직접 채번(forceSecondSlotSeq/existing+1 추정 폐기). 1구간/스케줄없음은 기존 채번 유지.
    3) `isSecondSlotTimeWindow`/`circularMinuteDistance` 호출 제거(메서드 데드 처리/삭제 판단).
    4) 081/084/085 가드 정리(D5 표): 084/085 폐기, 081은 "동일 구간 진행중 재출근" 모순 방어로 한정, 신규 087(구간 미선택)·088(구간 중복) 추가 또는 기존 코드 재사용 판단.
    5) 하루 2회 캡(080)·전날 미퇴근(082)·연차일(083)·외근 사유(086)·월마감(042)·Mock(005)·`lockUserForCheckIn` 직렬화는 불변.
  - 비즈니스 정책서 출처:
    - 근태관리 §5.1 출근 횟수 하루 2회 (`attd/05-checkin-limits.md` §5.1) — 캡 불변 확인.
    - 근태관리 §5.2 재출근 조건 (`attd/05-checkin-limits.md` §5.2) — "이전 퇴근 후 재출근"이 순서 자유화로 일부 무효화됨(정정 대상, §6).
    - 근태관리 §5.4 예외 케이스 (`attd/05-checkin-limits.md` §5.4) — "퇴근 미등록 재출근 차단"이 2구간 순서 자유화로 정정됨(§6).
    - 근태관리 §6.6 1일 2구간 제한 (`attd/06-schedule.md` §6.6) — 2구간 정의 근거(불변).
    - ⚠️ 코드 주석상 "§5.5 야간 2구간"은 정책서 .md에 미존재(prafta-app-008 도입 시 코드/메모리에만 반영). 이번 변경으로 그 규칙이 폐기되므로 정책서 명문화/정정 필요(§6).
  - 영향 받는 파일:
    - (백엔드) `prafta-backend/src/main/java/com/prafta/app/attd/attd01/dto/request/CheckInRequest.java`
    - (백엔드) `prafta-backend/src/main/java/com/prafta/app/attd/attd01/application/param/CheckInParam.java`
    - (백엔드) `prafta-backend/src/main/java/com/prafta/app/attd/attd01/service/impl/AppAttd01ServiceImpl.java` (checkIn 약 930~1007행 + `isSecondSlotTimeWindow` 약 1388행)
    - (백엔드) `prafta-backend/src/main/java/com/prafta/common/error/attd/AttdErrorCode.java` (087/088 신설·084/085 정리)
  - 영향 받는 endpoint: `POST /appApi/attd/check-in`
  - 예상 산출물: dto(request/param) 수정, service 분기 재작성, errorcode 정리
  - 연결 UI 명세: 없음(API 계약 변경. 화면 골격은 별도 frontend 작업)
- **선행 작업**: 없음
- **우선순위 근거**: 법적 책임 영역(attd) +1단계 격상. API 변경이 프론트 선행. 데이터 정합성(WORK_SEQ 채번) 영향.

### prafta-app-015-2
- **유형**: backend
- **영역**: app
- **모듈**: attd/attd01
- **작업 유형**: 보완
- **요구사항 요약**: 오늘/일상세 응답에 구간별 "출근 가능/완료" 플래그를 추가하여 프론트가 1구간/2구간 버튼을 게이팅하도록 한다.
- **상세 설명**:
  - [Phase 1]
  - 핵심 요구사항:
    1) `SlotResponse`(또는 신규 응답 필드)에 구간 단위 게이팅 플래그 추가. 권장: `MyAttendanceDayResponse`에 `slotCheckInFlags`(또는 각 SlotResponse에 `canCheckInThisSlot`/`alreadyCheckedIn`) 추가. 전부 서버 산출(프론트 표시만, 기존 컨벤션).
    2) 산출 규칙(2구간 스케줄 한정): 구간 N(1·2)에 대해 `alreadyCheckedIn = attdBySeq.get(N) != null`, `canCheckInThisSlot = isTwoSlot && !alreadyCheckedIn && !closed && withinCheckoutWindow && (하루2회 미초과) && (동일구간 진행중 아님)`. 1구간/스케줄없음 날은 기존 단일 `actions.canCheckIn` 유지(2구간 플래그는 미노출/false).
    3) 기존 `actions.canCheckIn`(effective 단일 버튼)·`computeDayActions`와의 정합: 2구간 스케줄에서는 구간별 플래그가 단일 canCheckIn을 대체/보완. 단일 canCheckIn은 1구간·스케줄없음 하위호환으로 유지.
    4) 자정보정·표준화·지각조퇴·effectiveSlotCount(prafta-app-014)·slotCount 산출은 불변. 구간 플래그는 "출근 버튼 게이팅"만.
  - 비즈니스 정책서 출처:
    - 근태관리 §5.1 (`attd/05-checkin-limits.md` §5.1) 하루 2회 캡 — 플래그 산출 상한 근거.
    - 근태관리 §6.6 (`attd/06-schedule.md` §6.6) 2구간 정의.
  - 영향 받는 파일:
    - (백엔드) `prafta-backend/src/main/java/com/prafta/app/attd/attd01/dto/response/SlotResponse.java` 또는 `MyAttendanceDayResponse.java`
    - (백엔드) `prafta-backend/src/main/java/com/prafta/app/attd/attd01/service/impl/AppAttd01ServiceImpl.java` (`selectToday`/`buildSlot`/`computeDayActions` 약 155~252·391~442행)
  - 영향 받는 endpoint: `GET /appApi/attd/my/today`, `GET /appApi/attd/my/day-detail`
  - 예상 산출물: response dto 필드 추가, service 산출 로직
  - 연결 UI 명세: UI-app-015-01 (오늘 카드 구간 선택)
- **선행 작업**: prafta-app-015-1 (채번/차단 규칙 확정 후 플래그 산출 일관)
- **우선순위 근거**: attd +1단계. 프론트 게이팅 데이터 공급. 015-1과 동일 서비스 파일이므로 동반 구현 권장.

### prafta-app-015-3
- **유형**: frontend-screen
- **영역**: app
- **모듈**: attd
- **작업 유형**: 보완
- **요구사항 요약**: 오늘 근태 카드(AttendanceTodayCard) + MyAttendanceView 출근 흐름에 2구간 스케줄 구간 선택(1구간/2구간 버튼) 추가. 이미 등록한 구간 버튼 비활성. 084 confirm·자동추정 흐름 제거, `targetWorkSeq` 전송.
- **상세 설명**:
  - [Phase 1]
  - 핵심 요구사항:
    1) `AttendanceTodayCard.vue` 푸터: 2구간 스케줄이면(`detail.isTwoSlot` && 서버 구간 플래그 존재) "1구간 출근"/"2구간 출근" 2버튼 노출(스크립트 일부=UI 토글/표시; emit에 `targetWorkSeq` 포함). 1구간/스케줄없음=기존 단일 버튼 유지.
    2) 각 구간 버튼 enabled = 서버 구간 플래그(015-2). 이미 등록 구간 disabled(`.bt-x`).
    3) `MyAttendanceView.vue` `startCheckInOut`/`callCheckInOut`: `targetWorkSeq` 파라미터 추가하여 body에 포함. 084 분기·`confirmSkipPrevSlot` 제거(085도 제거 — 폐기됨). 086/080/081/082/083 분기 유지.
    4) 신규 087(구간 미선택)·088(구간 중복) 에러 수신 시 메시지 노출(서버 message 우선) — 정상 흐름에선 발생 안 하나 방어.
  - 비즈니스 정책서 출처:
    - 근태관리 §5.1/§5.2/§5.4 (`attd/05-checkin-limits.md`) — 버튼 게이팅 의미(표시는 서버 산출).
    - 공통 디자인 가이드 (`common/13-ui-ux.md` §13) — 버튼/비활성 표현 토큰 사용.
  - 영향 받는 파일:
    - (프론트) `prafta-app-frontend/prafta-app-frontend/src/views/attd/components/AttendanceTodayCard.vue` (푸터 약 112~137행, script 약 476~499행)
    - (프론트) `prafta-app-frontend/prafta-app-frontend/src/views/attd/MyAttendanceView.vue` (`callCheckInOut` 491~559행, `startCheckInOut` 579~612행)
  - 영향 받는 endpoint: `POST /appApi/attd/check-in`(body targetWorkSeq), `GET /appApi/attd/my/today`
  - 예상 산출물: view/component 골격 수정(template+style; script는 emit/표시 토글까지, API 호출 body 조립은 developer)
  - 연결 UI 명세: UI-app-015-01
- **선행 작업**: prafta-app-015-1, prafta-app-015-2
- **우선순위 근거**: attd +1단계. 백엔드 API 후행. 사용자 직접 노출 화면.

### prafta-app-015-4
- **유형**: frontend-screen
- **영역**: app
- **모듈**: main
- **작업 유형**: 보완
- **요구사항 요약**: 메인 홈(MainView) 출퇴근 카드에도 2구간 스케줄 구간 선택(1구간/2구간) 게이팅 적용. 084 confirm·자동추정 흐름 제거.
- **상세 설명**:
  - [Phase 1]
  - 핵심 요구사항:
    1) MainView 출퇴근 카드 영역(약 60~70행 `:can-check-in`/`@click:checkin`)에 2구간 스케줄 시 구간 선택 노출. home-summary 응답에 구간 플래그가 없다면 today API 재사용 또는 home01 응답 확장 여부 확인(아래 [질문] 후보).
    2) `callCheckInOut`(약 460~535행): `targetWorkSeq` 포함, 084 분기/`confirmSkipPrevSlot`/085 제거. 086/080/081/082/083 유지.
  - 비즈니스 정책서 출처:
    - 근태관리 §5.1/§5.2 (`attd/05-checkin-limits.md`).
    - 공통 디자인 가이드 (`common/13-ui-ux.md` §13).
  - 영향 받는 파일:
    - (프론트) `prafta-app-frontend/prafta-app-frontend/src/views/main/MainView.vue` (출퇴근 카드 약 60~70·192·279·445~535행)
    - (참고/가능) home01 응답 DTO — 구간 플래그 미제공 시 today API 호출 또는 home01 확장 결정 필요
  - 영향 받는 endpoint: `POST /appApi/attd/check-in`, `GET /appApi/home01/home-summary`(또는 `/appApi/attd/my/today`)
  - 예상 산출물: view 골격 수정(template+style; API body 조립은 developer)
  - 연결 UI 명세: UI-app-015-02
- **선행 작업**: prafta-app-015-1, prafta-app-015-2
- **우선순위 근거**: attd +1단계. 메인 진입 화면. 015-3과 흐름 동일(공통화 가능).

---

## 화면 명세

> 참고: 본 작업은 기존 화면 보완이며 신규 화면이 아니다. AttendanceTodayCard/MainView 출퇴근 카드의 출근 버튼 영역만 변경한다. 디자인 토큰/공통 컴포넌트는 기존 화면 패턴을 그대로 따른다(native button + scoped class, 디자인 시스템 토큰 `--color-*`/`--space-*`/`--radius-*` 사용. 기존 카드가 공통 BaimButton이 아니라 자체 `.bt` 클래스를 쓰므로 동일 패턴 유지).

### UI-app-015-01 AttendanceTodayCard 2구간 구간 선택
- 연결 작업: prafta-app-015-3
- 화면 위치: `src/views/attd/components/AttendanceTodayCard.vue` (보완)
- 참조 패턴: 기존 동일 컴포넌트 푸터 `.ft > .bt`(수정요청 + primary) 구조. prafta-app-014 단일 primary 버튼.
- 현재 동작: 푸터에 "수정 요청" + 단일 primary("출근하기"/"퇴근하기", `actions.canCheckIn`/`canCheckOut`). 출근 시 구간은 서버 자동 추정.
- 의도된 동작(레이아웃):
```
┌─────────────────────────────────────────┐
│ (날짜/스케줄/근태/표준화 행 — 기존 동일)    │
│ [인라인 알림] (기존 동일)                  │
│─────────────────────────────────────────│
│ 푸터 액션                                  │
│  ┌ 2구간 스케줄 && 출근 가능 구간 존재 시 ┐ │
│  │ [수정 요청]  [1구간 출근] [2구간 출근] │ │
│  │              ^enabled=서버 ^disabled    │ │
│  └ 그 외(1구간/스케줄없음/출근완료) ──────┐│
│  │ [수정 요청]        [출근하기/퇴근하기]  ││ (기존 단일)
│  └─────────────────────────────────────┘│
└─────────────────────────────────────────┘
```
- 컴포넌트 매핑:
  | 영역 | 요소 | 게이팅 출처 |
  | --- | --- | --- |
  | 수정 요청 | `.bt.bt-s` (기존) | 항상 활성 |
  | 1구간 출근 | `.bt.bt-p`/`.bt-x` | 서버 slot1 canCheckInThisSlot |
  | 2구간 출근 | `.bt.bt-p`/`.bt-x` | 서버 slot2 canCheckInThisSlot |
  | 단일 출근/퇴근(폴백) | `.bt.bt-p`/`.bt-x` | 기존 actions.canCheckIn/canCheckOut |
- 상태별 동작:
  - loading: 카드 자체 로딩(기존 컨테이너 처리). 변화 없음.
  - 2구간·둘 다 미출근: [1구간 출근](활성) [2구간 출근](활성).
  - 2구간·1구간만 등록: [1구간 출근](비활성) [2구간 출근](활성).
  - 2구간·2구간만 등록(순서 자유): [1구간 출근](활성) [2구간 출근](비활성).
  - 2구간·둘 다 등록: 구간 버튼 숨김 → primary는 퇴근/완료(기존 흐름).
  - 진행 중(미퇴근) 구간 존재: 해당 구간은 퇴근 우선(기존 canCheckOut), 구간 버튼은 서버 플래그에 따름.
- 사용자 플로우: 오늘 카드 → 2구간 스케줄 → "2구간 출근" 탭 → 확인 모달 → GPS → `POST check-in {targetWorkSeq:2}` → 성공 시 reload → 1구간 버튼만 활성 잔존.
- 백엔드 의존: `GET /appApi/attd/my/today`(구간 플래그, prafta-app-015-2), `POST /appApi/attd/check-in`(targetWorkSeq, prafta-app-015-1).

### UI-app-015-02 MainView 출퇴근 카드 구간 선택
- 연결 작업: prafta-app-015-4
- 화면 위치: `src/views/main/MainView.vue` (보완)
- 참조 패턴: 기존 MainView 출퇴근 요약 카드(`:can-check-in`/`@click:checkin`).
- 현재 동작: 단일 출근 버튼(`canCheckIn`). 구간 서버 자동 추정.
- 의도된 동작: 2구간 스케줄이고 출근 가능 구간이 있으면 1구간/2구간 선택 노출(카드 폭 제약 시 컴팩트 2버튼). 등록된 구간 비활성. 그 외 기존 단일 버튼.
- 상태별 동작: UI-app-015-01과 동일 매트릭스(메인 카드 컴팩트 버전).
- 사용자 플로우: 메인 홈 → 2구간 스케줄 → 구간 선택 → 확인 → GPS → check-in(targetWorkSeq) → 카드 갱신.
- 백엔드 의존: `GET /appApi/home01/home-summary` 또는 `GET /appApi/attd/my/today`(구간 플래그 출처 확정 필요 — 아래 [질문]), `POST /appApi/attd/check-in`.

---

## 2. 의존성 그래프

```
prafta-app-015-1 (BE: targetWorkSeq + 채번/차단/에러 재작성)
      │
      ▼
prafta-app-015-2 (BE: 구간별 출근가능/완료 플래그 응답)
      ├──────────────┐
      ▼              ▼
prafta-app-015-3   prafta-app-015-4
 (FE: 오늘카드)     (FE: 메인홈)
```
- 015-1·015-2는 동일 서비스 파일(`AppAttd01ServiceImpl`) → developer 1회 작업으로 동반 구현 권장.
- 015-3·015-4는 출근 흐름 공통 로직(2-pass callCheckInOut, targetWorkSeq) 공유 → 가능하면 공통 함수화.

---

## 3. 에러코드/계약 변경 상세 (developer 참고)

### 3-1. AttdErrorCode
- 폐기(미사용 데드 또는 메시지 보존): `ATTD_400_084`, `ATTD_400_085`.
- 신설 권장:
  - `ATTD_400_087` — "출근할 구간(1구간/2구간)을 선택해 주세요." (2구간 스케줄에서 targetWorkSeq 누락/범위외)
  - `ATTD_400_088` — "이미 해당 구간 출근이 등록되어 있어요." (선택 구간 중복)
- 유지: 080·081(의미 한정)·082·083·086·042·005.
- ⚠️ 한국어 인코딩 함정(메모리 feedback_korean_encoding): AttdErrorCode 메시지가 기존 한국어를 잘 보존하는지 확인. 기존 파일이 한국어 메시지를 쓰고 있으므로(080~086 확인됨) 동일 방식 추가는 안전.

### 3-2. 응답 플래그 형태 (권장; developer 최종 판단)
- `SlotResponse`에 추가:
  - `canCheckInThisSlot`(boolean) — 이 구간 지금 출근 가능(서버 산출).
  - `alreadyCheckedIn`(boolean) — 이 구간 이미 출근 등록됨.
- ⚠️ Lombok+Jackson boolean is-접두 함정(메모리 feedback_lombok_jackson_boolean_is_prefix): `boolean isXxx` 회피하거나 `@JsonProperty`로 키 고정. `canCheckInThisSlot`/`alreadyCheckedIn`은 is-접두가 아니므로 안전하나, 빌더/게터 직렬화 키를 프론트 계약과 1:1 확인.

---

## 4. 회귀/정합성 검토 포인트 (qa/security 인계)

1. **prafta-app-014(하루 2회 캡·effectiveSlotCount)**: 2구간 스케줄에서 WORK_SEQ를 2 먼저 채번 → effectiveSlotCount/slotCount 산출(약 1413행 `effectiveSlotCount`)이 attdBySeq.get(2)만 있는 경우에도 2를 반환하는지(현행 max 로직상 2 — 정합). slots 반복(약 195행 `for seq=1..slotCount`)이 WORK_SEQ=1 레코드 없이 seq=1 슬롯을 schedule만 있고 attendance=null로 렌더하는지 확인(정상).
2. **prafta-app-008(084/085/confirmSkipPrevSlot)**: 해당 흐름 전면 폐기. 잔존 호출 0건 확인(프론트 callCheckInOut 084 분기·askConfirm 제거).
3. **순서 자유화 부작용**: "1구간 미퇴근 + 2구간 출근" 허용 시 1구간 퇴근 누락이 §7.4 보정/다음날 게이트(082)로만 해소됨 → 근태 마감 차단 사유(GPS/미결) 정합 확인. 정책 §5.4와의 충돌은 §6 정정으로 봉합.
4. **IDOR/직렬화**: `lockUserForCheckIn` 비관적 잠금 유지(동시 동일 구간 INSERT 직렬화). WORK_SEQ 직접 채번이 동시성에서 중복 INSERT를 만들지 않는지(잠금 범위 내 count→insert) — security High 후보.
5. **1구간 스케줄/스케줄없음 무변경**: targetWorkSeq가 와도 무시되어 기존 existing+1 채번·canCheckIn 단일 버튼 유지.

---

## 5. 메인 세션이 Notion에 기록할 항목

> 서브에이전트는 Notion 접근이 없으므로 메인 세션이 아래를 "작업 로그"/"도메인 지식 베이스"에 대행 등록.

### 작업 로그 DB (4행)
| 작업ID | 영역 | 모듈 | 작업유형 | 상태 | 담당 | 요구사항 요약 | 상세 | 선행 | 산출물 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| prafta-app-015-1 | app | attd/attd01 | 보완 | 분해완료 | planner | 출근 구간 자동추정→targetWorkSeq 명시선택, 순서자유/1회차단 서버강제 | [backend] (위 015-1 상세) | 없음 | (비움, developer) |
| prafta-app-015-2 | app | attd/attd01 | 보완 | 분해완료 | planner | 오늘/일상세 응답 구간별 출근가능/완료 플래그 | [backend] (위 015-2 상세) [UI 명세: UI-app-015-01] | prafta-app-015-1 | (비움) |
| prafta-app-015-3 | app | attd | 보완 | 분해완료 | planner | 오늘카드+MyAttendanceView 구간선택 버튼, 084/자동추정 제거 | [frontend-screen] (위 015-3 상세) [UI 명세: UI-app-015-01] | 015-1,015-2 | AttendanceTodayCard.vue / MyAttendanceView.vue |
| prafta-app-015-4 | app | main | 보완 | 분해완료 | planner | 메인홈 출퇴근 카드 구간선택 게이팅 | [frontend-screen] (위 015-4 상세) [UI 명세: UI-app-015-02] | 015-1,015-2 | MainView.vue |

### 도메인 지식 베이스 DB (2행)
- `UI-app-015-01 AttendanceTodayCard 2구간 구간선택` — 영역 app / 모듈 attd / 현재동작=단일 primary(자동추정) / 의도된동작=위 명세 / 검증상태 Claude 분석
- `UI-app-015-02 MainView 출퇴근 카드 구간선택` — 영역 app / 모듈 main / 현재동작=단일 출근버튼 / 의도된동작=위 명세 / 검증상태 Claude 분석

---

## 6. 정책서 정정 필요 항목 (사용자 확정 대기 — planner는 .md 직접 수정 안 함)

이번 변경은 근태 도메인 규칙을 바꾸므로 정책서 .md 정정이 필요. **단 정책서 수정 여부/문구는 사용자 확정 후 developer 또는 메인 세션이 반영**(planner는 분해만):

1. `attd/05-checkin-limits.md`:
   - §5.2 "재출근은 반드시 이전 퇴근 완료 후" — 2구간 스케줄에서는 **순서 자유(구간 명시 선택)로 예외**임을 명문화.
   - §5.4 "퇴근 미등록 상태에서 재출근 시도 → 차단" — 2구간 스케줄에서 다른 구간 출근은 허용(동일 구간 중복만 차단)으로 정정.
   - 신규 §5.5(또는 §5.2에 흡수) "2구간 출근 구간 선택"을 명문화: 자동추정 폐기, 사용자가 1구간/2구간 선택, WORK_SEQ=선택 구간, 각 구간 1회.
2. `.claude/context/policies/CHANGELOG.md`: prafta-app-015 항목 추가(§5.2/§5.4 정정 + §5.5 신설, 자동추정·084·085·confirmSkipPrevSlot 폐기, 단일 출처 본 plan 문서).
3. 코드 주석/메모리의 "§5.5 야간 2구간 Case A/B/C"는 본 변경으로 폐기 → developer가 관련 Javadoc/주석 정정.

> ⚠️ 정책서 우선순위(README): 근태관리 정책서는 3순위. 본 변경은 사용자 요청서(앱 요구) 기반이며 상위 정책서(요청승인/공통)와의 충돌은 없음(출근 구간 선택은 근태 전용 규칙). 정책서 .md 정정은 사용자 승인 사항.

---

## 7. 채팅으로 확인 필요한 질문 (메인 세션 → 사용자)

1. **[질문] 정책서 .md 정정 범위**: 위 §6의 §5.2/§5.4 정정 + §5.5 신설 + CHANGELOG 추가를 이번 라운드에 함께 반영할까요, 아니면 코드만 먼저 반영하고 정책서는 별도로 둘까요? (planner는 자율 진행 지시에 따라 "코드 우선, 정책서는 사용자 확정 후" 기본값으로 분해함.)
2. **[질문] MainView 구간 플래그 출처**: MainView가 쓰는 `home-summary`(home01) 응답에 구간별 출근가능 플래그가 없습니다. (a) home01 응답을 확장할지, (b) MainView가 출근 시점에 `/appApi/attd/my/today`를 호출해 구간 플래그를 받아올지 결정 필요. planner 기본 권장: (b) today 재사용(home01 스키마 확장 최소화). 단 MainView가 today를 이미 호출하지 않으면 추가 호출 비용 발생 — developer가 현행 호출 그래프 확인 후 결정.
3. **[질문] 087/088 신규 vs 기존 재사용**: 구간 미선택(087)·구간 중복(088)을 신규 코드로 둘지, 기존 080/081 재사용할지. planner 권장: 신규(메시지 명확). developer 최종 판단.

> 자율 진행 지시에 따라 위 질문들은 **합리적 기본값으로 분해를 완료**했으며(코드 우선·today 재사용·신규 코드 권장), 메인 세션이 사용자에게 확인 후 조정 가능.
