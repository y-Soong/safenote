# prafta-app-013 확정 결정 (단일 출처)

요청서: `prafta-app-013.md` (내 근태 화면 — 오늘/이번달 탭에 4액션 시트 추가 + 4액션 게이팅 규칙 변경)
본 문서는 사용자 채팅 확정(2026-05-31) 결과를 고정한다. 구현/검증은 이 문서를 1차 기준으로 한다.

---

## 0. 배경 (현재 구조 사실)

- 게이팅은 **전부 백엔드 산출**(`AppAttd01ServiceImpl`), 프론트는 표시만(비즈니스 판정 금지).
- **이번주**: `WeekDayActionsResponse`(4플래그) → `AttendanceActionSheet` 로 구동 중.
- **오늘 / 이번달(day-detail)**: `DayActionsResponse` 에 `canRequestModify`(단일)뿐 → 4액션 시트 미연동.
  - 이번달 일자상세카드가 `canRequestAttendanceCorrection/Overtime` 를 참조하나 BE가 안 내려줘 항상 비활성(잠복).
- 따라서 본 작업 = **백엔드(4액션 산출 통일 + 규칙 변경) + 프론트(오늘/이번달 시트 연결) + 정책서 수정**.

---

## 1. 게이팅 판정 프리미티브 (일자별)

| 이름 | 의미 |
| --- | --- |
| `isPast` / `isToday` / `isFuture` | 대상일 vs 오늘 |
| `closed` | 해당 월 근태마감 여부 (prafta-028 월마감) |
| `hasSchedule` | 근무 스케줄(SCH_CD) 존재 (연차/휴무-only 제외, 기존 `hasSchCd`) |
| `hasAttendance` | **출근 기록 1건 이상 존재** (퇴근/완료 무관) — Q2(a) |
| `isWorking` | 오늘 진행 중 = `hasAttendance && !completed` (출근했으나 전 구간 미완료) |
| `completed` | 전 구간 출퇴근 완료 (기존 정의 유지) |

> `hasAttendance` 는 **출근 기록 존재**로 정의(Q2=a). 기존 초과근무가 쓰던 `completed` 와 구분.

---

## 2. 4액션 게이팅 규칙 (오늘/이번주/이번달 **3탭 공통**)

기존 규칙을 아래로 **교체**한다. 세 탭 모두 동일 산출 로직을 쓴다.

| 액션 | 신규 enabled 조건 | 기존(폐기) |
| --- | --- | --- |
| **스케줄 수정** `canRequestScheduleModify` | `hasSchedule && !closed` (과거·현재·미래 모두) | `isFuture && hasSchCd && !closed` |
| **근태 보정** `canRequestAttendanceCorrection` | `!isFuture && !closed && !isWorking` (과거·현재, 미래 차단, 오늘 근무중 비활성) | `!closed && ((isPast && hasSchCd) \|\| (isToday && completed))` |
| **초과근무** `canRequestOvertime` | `!isFuture && !closed && hasAttendance && !isWorking` (과거·현재, 미래 차단, **출근기록 존재**, 오늘 근무중 비활성) | `(isPast \|\| isToday) && completed && !closed` |
| **연차 신청** `canRequestLeave` | `!closed && !hasAttendance` (과거·현재·미래 모두, **출근기록 있으면 불가**, 스케줄 무관) | `isFuture && hasSchCd && !closed` |

공통:
- **마감(`closed`)이면 4액션 전부 비활성** (Q3 — 요청서 누락분, 차단 유지 확정).
- **미래 차단**은 근태 보정·초과근무에만. 스케줄 수정·연차는 미래 허용.

### 2-1. 연차 — 스케줄 없는 날 (Q4)

- `canRequestLeave` 자체는 스케줄 유무와 무관(`!closed && !hasAttendance`).
- 단 **스케줄이 없는 날(`!hasSchedule`)** 은 **full-day(온전한 하루) 연차만** 허용. 반차·시간차(시간차 2시간 등) **불가**.
- 표현: 응답에 힌트 플래그 `leaveFullDayOnly = !hasSchedule` 를 동봉한다.
- ⚠️ 실제 "full-day만" 강제는 **연차 신청 폼**에서 일어나는데, 현재 앱 연차 신청 폼은 미구현(stub, "준비 중"; 메모리 `project_prafta_app_005_my_leave` 참조). → 본 작업은 **플래그 계약만 내려주고**, 폼 강제는 연차 신청 폼 구현 시 follow-up. (planner가 follow-up 으로 명시)

---

## 3. 프론트 동작 (Q6 — 권장안 수용)

- 오늘 카드 "수정 요청" 버튼 / 이번달 일자상세 "수정 요청" 버튼 → **항상 눌러서 `AttendanceActionSheet` 오픈** (개별 게이팅은 시트 내부 4행이 담당).
- 이번달 일자상세의 기존 "근태 보정 / 초과근무" **2버튼은 제거하고 시트로 통일**.
- 오늘 탭은 `todayDetail` 에서, 이번달 탭은 `dayDetail` 에서 시트용 `day` 객체(`workYmd` + 4액션 `actions`) 를 구성해 전달.
- 시트 액션 라우팅은 기존 `onSheetAction`(scheduleModify/attendanceCorrection/overtime/leave) 재사용. `leave` 는 기존대로 stub("준비 중") 유지(연차 폼 미구현).

---

## 4. 백엔드 변경 (정책 우선, 임의 규칙 금지)

- `computeWeekActions` 의 4액션 규칙을 §2 로 교체.
- `today` / `day-detail` 응답에도 동일 4액션을 산출해 내려준다(시트 연동용). `DayActionsResponse` 확장 또는 `WeekDayActionsResponse` 재사용 — planner/developer 설계.
  - 기존 `canCheckOut/canCheckIn`(퇴근/2구간 출근 primary 버튼)·`canRequestModify` 는 **유지**(오늘 카드 primary 버튼·하위호환). "수정 요청" 버튼 enable 은 프론트에서 "항상 오픈"이라 `canRequestModify` 의존을 끊는다.
- `hasAttendance`(출근기록 존재)·`leaveFullDayOnly`(=`!hasSchedule`) 산출 추가.
- 보안: 신규 노출 없음(기존 my-* 조회 = 본인 세션 USER_CD, IDOR 가드 기존 유지). 단 security 재검토 대상.

---

## 5. 정책서 수정 (Q1 — 정책서를 덮어쓰며 수정)

`.claude/context/policies/attd/` 아래 다음을 본 결정에 맞게 갱신(planner 담당):

- **09-requests-approval.md §9.2 스케줄 수정**: "신청 조건 = 스케줄 존재 + 미마감". **시간 방향 제한 없음(과거·현재·미래 모두)** 명시.
- **§9.3 초과근무**: 기존 "사전/사후 모두 허용" → **사전(미래) 신청 차단, 과거·현재만**. 조건에 **출근 기록 존재** 추가. 기한은 "근태 마감 전까지"(D+5 사후기한은 이미 폐기, 재기획서 §3.2). §9.3.2/9.3.3 의 미래·D+5 서술 정리.
- **§9.4 연차**: "신청 조건 = (출근 기록 없음) + 미마감, 시간 방향 무관". 스케줄 없는 날은 **full-day 연차만**(반차/시간차 불가) 명시. 기존 "스케줄 존재" 필수 조건 완화.
- **11-attendance-correction.md**: 근태 보정 신청 시점 = "미래 제외 과거·현재, 오늘 근무중 제외, 미마감". 기존 서술과 충돌 시 본 결정 우선.
- 변경 이력 한 줄(출처: prafta-app-013) 각 섹션에 남길 것.

---

## 6. 미해결/Follow-up

- 연차 full-day-only 강제: 연차 신청 폼 구현 시(별도 작업) `leaveFullDayOnly` 소비.
- 마감월 전탭 비활성 시 "수정 요청" 버튼을 열면 4행 전부 비활성 시트가 뜸 → 사용자 수용("항상 오픈"). UX 개선(전부 불가 시 버튼 비활성)은 선택적 follow-up.
- 근태 보정에서 "스케줄도 근태기록도 없는 과거/오늘"(보정 대상 부재)일 때 enable 되는 엣지 — 요청 문구 그대로 허용. qa 가 엣지 확인.
