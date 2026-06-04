# prafta-app-014 작업지시서 — 앱 출퇴근 "하루 최대 2회" 상한 (스케줄 구간 수 무관)

> 분해 담당: planner (Claude)
> 영역: app (모바일 webview Vue + 백엔드 attd01)
> 상태: 분해완료 (사용자 확정 요청 기반). **Notion 등록은 메인 세션이 대행** (서브에이전트 Notion 접근 없음).
> 원본 요청: 사용자 채팅 확정 + 메인 세션 사전 코드조사(재조사 불필요).

---

## 0. 배경 / 문제

근태 도메인 전체가 "하루 구간 수"를 **스케줄에 2구간(`secSchStrTime`)이 존재하는가**(`isTwoSlot`)로만 판정한다. 즉 실제 출근 기록 개수가 아니라 **스케줄이 상한을 정한다**.

- 1구간 스케줄: 하루 1회만 출근 가능 (`maxCheckIn = isTwoSlot ? 2 : 1` → 1).
- 스케줄 없는 날(§7.5): 직전 미퇴근만 차단, **상한 없음** (3회·4회도 가능).

사용자 확정 변경: **스케줄 구간 수와 무관하게 하루 최대 출근(근무) 횟수를 항상 2회로 한다.** 1구간 스케줄·스케줄 없는 날도 2회까지 출퇴근 등록이 가능해야 하며, 스케줄 없는 날도 2회로 캡한다.

## 1. 확정 결정사항 (사용자)

| # | 결정 |
| --- | --- |
| D1 | 2번째 출근은 **일반 근무**로 본다(자동 초과근무 분류 아님). 해당 근무 전체를 사후 초과근무로 상신하는 기존 구조는 유지/호환. |
| D2 | 2번째 슬롯은 대응 스케줄 구간이 없으므로 **원본 출퇴근 시각만 표시**. 표준화/지각·조퇴 판정 **미적용**. |
| D3 | 스케줄 없는 날(§7.5)도 **하루 2회까지로 상한**. 현재 "직전 퇴근만 했으면 무제한"을 2회 캡으로 변경. |
| D4 | 홈 카드 + 일상세 카드 **양쪽 모두** 2번째 출근 노출(표시 일관성). |

## 2. 설계 방향 (planner 확정 — 메인 세션 제안 보정)

### 2.1 핵심 개념: effectiveSlotCount

"하루 구간 수"를 스케줄 단독이 아니라 다음으로 도출하고 **2로 캡**한다.

```
scheduleSlots      = isTwoSlot ? 2 : (hasSchedule ? 1 : 0)   // 스케줄이 정의한 구간
attdSlots          = 그날 실제 출근기록(WORK_SEQ DISTINCT) 개수
effectiveSlotCount = min( max(scheduleSlots, attdSlots, 1), 2 )   // 항상 1~2
```

- 스케줄 유무와 무관하게 화면/상태/액션은 effectiveSlotCount 기준으로 일관 동작.
- "최대 2회" 상한은 **항상 2** (스케줄 구간 수와 무관). 즉 출근 가능 횟수 = `min(2, …)`.

### 2.2 "스케줄 대응 슬롯" 여부를 슬롯 단위로 분리 (D2 핵심)

표준화·지각·조퇴는 **스케줄 구간이 실제 존재하는 슬롯에만** 적용한다.

- 슬롯1: `hasSchedule` 이면 스케줄 대응 → 표준화/지각·조퇴 적용. 스케줄 없는 날의 1번째 출근은 스케줄 대응 없음.
- 슬롯2: **스케줄이 2구간일 때만** 스케줄 대응(=§5.5 야간 2구간 경로). 그 외(1구간 스케줄의 2번째 출근, 스케줄 없는 날의 2번째 출근)는 스케줄 대응 없음 → **원본 시각만**.

이를 위해 슬롯 응답에 **`hasSchedule`(슬롯 단위)** 의미의 플래그를 노출한다(SlotResponse). 이미 `schedule.startTime` 등이 null 이면 "스케줄 없음"으로 프론트가 판별 가능하므로, 신규 필드 없이 **`slot.schedule.startTime == null`** 을 "스케줄 미대응 슬롯"의 단일 판별식으로 삼는다(서버는 미대응 슬롯의 `schedule`을 빈/ null 로 내려준다).

### 2.3 §5.5 야간 2구간 분기 보존

스케줄 2구간(`isTwoSlot=true`)에서의 §5.5 Case A/B/C, `isSecondSlotTimeWindow`, WORK_SEQ=2 강제 채번은 **그대로 보존**. 이번 변경은 "1구간/스케줄없음에서도 2회 허용"을 추가하는 것이지 야간 2구간 로직을 건드리지 않는다.

### 2.4 프론트 표시 디커플링 (display-only 유지)

프론트는 비즈니스 판정을 하지 않는다. 따라서 백엔드가 다음을 명시적으로 내려주어 프론트가 그대로 따른다.

- `slotCount`(신규, int): effectiveSlotCount. 프론트 구분선 라벨/슬롯 반복의 기준.
- `slots`: effectiveSlotCount 만큼 항상 채워 내려준다(스케줄 미대응 슬롯은 `schedule=null`).
- `workStatus`: effectiveSlotCount 기준으로 산출(1구간 2회 케이스에서도 2번째 출근 버튼이 뜨도록).
- `actions.canCheckIn` / `sheetActions`: effectiveSlotCount 기준.

> ⚠️ 기존 `isTwoSlot` 필드는 **하위호환을 위해 유지하되 의미를 "스케줄이 2구간인가"로 고정**(현행 의미 유지). 프론트의 슬롯/상태/버튼 판정은 `isTwoSlot` 대신 신규 `slotCount`(또는 server-computed status/actions)로 옮긴다. `isTwoSlot`는 §5.5(야간 2구간) 전용 표시(예: 주/월 "2구간" 배지)에만 잔존시킨다.

---

## 3. 정책서 출처 (developer/qa/security 정독 대상)

| 요구사항 | 정책서 출처 | 본 변경 후 상태 |
| --- | --- | --- |
| 출퇴근 횟수/구간 제한 | `attd/05-checkin-limits.md` §5.1~§5.4 | **정정 필요** (§5.1 표·§5.3·§5.4) — 본 작업으로 재정의 |
| 재출근 조건 | `attd/05-checkin-limits.md` §5.2 | 유지 (직전 퇴근 후에만 재출근) |
| §5.5 야간 2구간 | (코드/메모리 `project_prafta_app_008_nightshift_offsite`) | 보존 |
| 출퇴근 기본 규칙 | `attd/07-checkin-checkout.md` §7.1 | **정정 필요** ("2구간 근무일은 최대 2회" → "하루 최대 2회") |
| 스케줄 없는 날의 근무 | `attd/07-checkin-checkout.md` §7.5 | **정정 필요** (무제한 → 2회 상한 추가) |
| 시간 표준화 | `attd/10-attendance-calc.md` §10.2 | 슬롯 단위 적용 명문화(스케줄 대응 슬롯만) |
| 지각/조퇴(정규 근무 계산) | `attd/10-attendance-calc.md` §10.1 | 슬롯 단위 적용 명문화 |
| 초과근무 사후 상신 | `attd/09-requests-approval.md` §9.3 + `request-approval/03-policy-alignment.md` §3.2 | 유지(D1: 2번째 근무 전체 사후 상신 가능) |
| 1일 2구간 제한(스케줄) | `attd/06-schedule.md` §6.6 | 유지(스케줄 정의는 여전히 최대 2구간) |

> 정책서 충돌 해소: §5.1/§5.3/§5.4/§7.1/§7.5 는 근태관리 정책서(우선순위 3)이며, 요청승인관리 재기획서·공통 정책서와 충돌하지 않는다. 따라서 사용자 확정 변경을 근태관리 정책서에 직접 반영(정정)한다. 정정 문구는 §6 참조.

---

## 4. 단위 작업

### prafta-app-014-A [backend] 출근 상한을 effective 2회로 변경

- **유형**: backend / **모듈**: app/attd/attd01 / **작업유형**: 보완
- **요구사항 요약**: checkIn() step6 출근 상한을 스케줄 구간 수 무관 항상 2회로. 스케줄 없는 날도 2회 캡 추가.
- **상세**:
  1. `AppAttd01ServiceImpl.checkIn()` step6: `maxCheckIn = isTwoSlot ? 2 : 1` → **`maxCheckIn = 2`** (스케줄 유무·구간 수 무관).
     - `:870` `if (existing >= maxCheckIn) throw ATTD_400_080` 가드는 그대로 동작(이제 2회 초과부터 차단).
  2. 1구간 스케줄 재출근(`else if (existing >= 1)` 분기, `:922-930`): 직전 미퇴근이면 `ATTD_400_081` 차단 — **유지**. 직전 퇴근 완료면 2번째 출근 허용(현재는 maxCheckIn=1 가드에서 먼저 막혔음 → 이제 통과).
  3. 스케줄 없는 날(§7.5, `:931-941`): 현재 `existing>=1 && open>0` 만 차단 → **`existing >= 2` 면 `ATTD_400_080` 차단(2회 캡)** 추가. 직전 미퇴근 차단(`ATTD_400_081`)도 유지.
  4. WORK_SEQ 채번(`:945`): 기본 `existing+1`. §5.5 Case C 의 `forceSecondSlotSeq=true` 경로(스케줄 2구간)는 그대로. (1구간 2번째 출근/스케줄없음 2번째는 existing+1=2 로 자연 채번.)
  5. **D1**: 2번째 출근의 분류/플래그를 "초과근무"로 자동 마킹하지 **않는다**(일반 근무 INSERT 그대로). 사후 초과근무 상신은 기존 흐름.
- **영향 파일**: `PRAFTA/prafta-backend/src/main/java/com/prafta/app/attd/attd01/service/impl/AppAttd01ServiceImpl.java`
- **영향 endpoint**: `POST /appApi/attd/check-in` (셀프 출근)
- **선행 작업**: 없음
- **우선순위 근거**: 법적 책임 영역(attd) +1격상. 쓰기 경로 핵심 가드 변경 → 최우선.

### prafta-app-014-B [backend] 조회 응답을 effectiveSlotCount 기준으로 일관화

- **유형**: backend / **모듈**: app/attd/attd01 / **작업유형**: 보완
- **요구사항 요약**: 일상세/오늘/주/월 응답의 슬롯 목록·상태·액션·완료판정·합계를 effectiveSlotCount 기준으로 변경. 스케줄 미대응 슬롯은 표준화/지각·조퇴 미적용(원본만).
- **상세**:
  1. **effectiveSlotCount 헬퍼 신규**: `int effectiveSlotCount(boolean isTwoSlot, boolean hasSchedule, Map<Integer,AttdRecordResult> attdBySeq)` = `min(max(scheduleSlots, attdSlots, 1), 2)`. (attdSlots = `attdBySeq.keySet()` 중 1·2 존재 개수.)
  2. **`buildDayResponse` 슬롯 목록(`:178-188`)**: `if (sched != null && !isLeaveDay)` 1구간 추가 후, **`if (slotCount >= 2)` 일 때 2구간 슬롯 추가**(현재 `if (isTwoSlot)`). 단 스케줄 미대응 슬롯이면 `buildSlot` 에 스케줄 null 을 전달(아래 4). 스케줄 없는 날(`sched==null`)도 attdSlots≥1 이면 슬롯을 만들어야 하므로 분기 확장: 스케줄 없는 날이어도 실제 출근기록이 있으면 슬롯 1·2를 생성(스케줄 null).
  3. **응답에 `slotCount` 추가**: `MyAttendanceDayResponse`(+ Week/Month 일자 응답이 슬롯 라벨에 쓰면) 빌더에 `.slotCount(slotCount)` 추가. `isTwoSlot` 은 현행 의미로 유지.
  4. **`buildSlot` D2 분기**: 슬롯이 "스케줄 미대응"이면(슬롯1=스케줄없는날, 슬롯2=스케줄1구간/스케줄없는날) `schedule` 을 null 로 내리고 `standardized` 산출을 **건너뛴다**(원본 출퇴근만). `buildSlot` 호출부에서 `hasScheduleForSlot` 을 전달하거나, `sched==null || (seq==2 && !isTwoSlot)` 조건으로 내부 분기.
  5. **`computeWorkStatus`(`:306-340`)**: effectiveSlotCount 기준으로 재작성. slotCount==2 면 기존 isTwoSlot 분기 로직 사용(s1Out&&s2Out → CHECKED_OUT, s1In → TWO_SLOT_WORKING…). slotCount==1 이면 기존 1구간 로직. **결과: 1구간 스케줄에서 1번째 퇴근 완료 + (2회 가능) 상태일 때 "퇴근완료"로 고정되지 않고 2번째 출근 버튼이 뜨도록** — 단, 1구간 스케줄에서 출근 1회·퇴근 완료만 있고 2번째 출근 전이면 slotCount=1(attdSlots=1, scheduleSlots=1) → 상태가 CHECKED_OUT 이 되어 버튼이 안 뜬다. **이 문제 해결을 위해 액션 산출(아래 6)에서 "추가 출근 가능"을 effective 상한 기준으로 별도 계산**한다(상태와 분리).
  6. **`computeDayActions`(`:356-407`) canCheckIn 재정의**: `canCheckIn = withinCheckoutWindow && (직전 슬롯 퇴근 완료) && (현재 출근기록 수 < 2) && !closed`. 즉 스케줄 구간 수가 아니라 **effective 상한(2)** 과 실제 기록 수로 판정. 2구간 스케줄의 §5.2(1구간 퇴근 후 2구간 출근)도 이 식으로 포괄. 1구간 스케줄·스케줄 없는 날의 2번째 출근도 동일 식으로 허용.
     - canCheckOut: 진행 중(출근有 퇴근NULL) 슬롯 존재 시 — 기존 유지.
  7. **`computeActionFlags`/`computeWeekActions`(`:531~593`) `completed` 재정의**: `completed = (출근기록 수 == effectiveSlotCount) && 모든 슬롯 퇴근완료`. 현재 `isTwoSlot ? (s1Out&&s2Out) : s1Out` → effective 기준으로. (1구간 스케줄에서 2회 출근·둘 다 퇴근해야 완료.)
  8. **합계(`actualCompletedMinutes`/`plannedMinutes`)**: `actualCompletedMinutes` 는 실제 출퇴근 기준이므로 **slot2 도 합산**하도록 `isTwoSlot` 게이트를 "slot2 기록 존재"로 변경. `plannedMinutes` 는 스케줄 기준이라 그대로(스케줄 없는 추가근무는 예정=0, 사후 초과근무 상신으로 반영).
  9. **`hasActionRequired`(`:1060-1093`)**: 미퇴근/결근 판정을 effectiveSlotCount 기준으로. slot2 기록이 있는데 퇴근 누락이면 보정 필요(현재 isTwoSlot 게이트로 1구간 2번째 누락을 놓침).
  10. **`computeAttendanceStatus`(`:504-529`) 지각/조퇴**: 스케줄 대응 슬롯만 판정. slot2 가 스케줄 미대응이면 slot2 의 조퇴 판정 제외(마지막 스케줄 대응 슬롯 기준). slot1 이 스케줄 없는 날이면 지각 판정 자체 제외(스케줄 시작이 없으므로).
- **영향 파일**:
  - `.../attd01/service/impl/AppAttd01ServiceImpl.java` (대부분)
  - `.../attd01/service/dto/MyAttendanceDayResponse.java` (+`slotCount`), 필요 시 Week/Month 일자 DTO
  - (slot DTO `SlotResponse` 는 신규 필드 없이 `schedule=null` 로 표현 가능 — 변경 최소화)
- **영향 endpoint**: `GET /appApi/attd/my/today`, `/day-detail`, `/week`, `/month`
- **선행 작업**: prafta-app-014-A (개념 공유) — 병렬 가능하나 effectiveSlotCount 헬퍼는 B에서 단일 정의 후 A가 참조해도 됨. 권장: A→B 순.
- **우선순위 근거**: attd +1격상. A 와 함께 핵심.

### prafta-app-014-C [frontend-component] 일상세/오늘 카드 — 2번째 슬롯·2번째 출근 버튼

- **유형**: frontend-component / **영역**: app / **모듈**: attd / **작업유형**: 보완
- **요구사항 요약**: `AttendanceTodayCard.vue` 가 `slotCount`(서버) 기준으로 구간 구분선·슬롯 반복을 그리고, 스케줄 미대응 슬롯(`schedule==null`)은 표준화 행을 숨기며, primary 버튼이 1구간 스케줄/스케줄없음에서도 2번째 출근으로 뜨도록.
- **상세 (D2/D4)**:
  1. `isTwoSlot` 의존 제거 → **`slotCount`(신규 prop/`detail.slotCount`) 기준**으로 구간 구분선(`v-if`) 표시. `slotCount >= 2` 면 구분선("1구간"/"2구간" → 스케줄 미대응이면 "추가 근무" 라벨) 노출.
  2. 슬롯 반복(`v-for slots`)은 유지. 슬롯의 스케줄 행/표준화 행은 **`slot.schedule` 이 null 이면 표준화 행 숨김 + 스케줄 행을 "스케줄 없음(추가근무)"** 으로 표기(원본 출퇴근만 표시).
  3. primary 버튼 게이팅: 현재 `isTwoSlotCheckIn = workStatus==='TWO_SLOT_WORKING' && canCheckIn` → **`canCheckIn`(서버 actions) 단독 기준**으로 변경(상태 문자열 의존 제거). 라벨은 `canCheckIn` 이면 "2번째 출근", 아니면 "퇴근하기". (서버가 effective 기준으로 canCheckIn 을 산출하므로 1구간/스케줄없음도 자동 동작.)
  4. 상태 배지 텍스트: `TWO_SLOT_WORKING` 라벨을 "2구간 근무" → 스케줄 미대응 슬롯 진행 시 의미가 모호하므로 **"근무중(추가)"** 등으로 분기(서버 workStatus 추가 값 없이 slotCount+schedule null 로 프론트 표시 분기, 비즈니스 판정 아님 — 표시 라벨링).
  5. 인라인 알림 문구(`TWO_SLOT_WORKING`): "2구간 근무까지 모두 끝난 뒤…" → slotCount==2 일반 문구로 보정.
- **연결 UI 명세**: UI-A014-1 (§5)
- **영향 파일**: `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/attd/components/AttendanceTodayCard.vue`
- **선행 작업**: prafta-app-014-B (서버 `slotCount`/`canCheckIn`/`schedule=null` 계약 선행)
- **우선순위 근거**: 화면(후순위) but attd +1격상. B 이후.

### prafta-app-014-D [frontend-component] 홈 카드 — 2번째 출근 노출

- **유형**: frontend-component / **영역**: app / **모듈**: main / **작업유형**: 보완
- **요구사항 요약**: 홈 `AttendanceCard.vue` 가 1구간 스케줄/스케줄없음에서도 2번째 출근 버튼을 띄우도록(서버 canCheckIn 직결).
- **상세 (D4)**:
  - 홈 카드는 이미 `canCheckIn`/`canCheckOut`(서버 산출) 직결. 백엔드(B)가 effective 기준으로 canCheckIn 을 내려주면 **골격 변경 거의 없음**. 단:
  1. `status` prop(`BEFORE_WORK`/`WORKING`/`OFF_WORK`)은 home01 응답(`prafta-app-013(A)` OFF_WORK 신설) 기준. 1구간 1회 퇴근 후 2번째 출근 가능 상태에서 **`OFF_WORK`(퇴근 완료) 가 아니라 "출근 가능" 상태**로 보이게 home01 의 attendance status 산출이 effective 기준이어야 함 → **home01 백엔드도 영향**(아래 §4 메모: home01 은 attd01 와 별도 서비스). 본 작업 범위에서 home01 status 산출 보정을 backend 작업(014-B 확장 또는 014-E)으로 둘지 결정 필요 → **§7 미해결 1**.
  2. 골격: 출근 버튼 라벨/위치 고정. `canCheckIn` true 면 출근 버튼 활성(2번째 출근도 동일 버튼). 별도 "2번째 출근" 라벨 분기 불요(홈은 단순 출근/퇴근 2분할).
- **연결 UI 명세**: UI-A014-2 (§5)
- **영향 파일**: `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/main/components/AttendanceCard.vue` (+ 호출부 `MainView.vue` 가 status/canCheckIn 매핑)
- **선행 작업**: prafta-app-014-B + (home01 status 보정 결정)
- **우선순위 근거**: 화면 후순위. attd +1격상.

### prafta-app-014-E [docs] 정책서 §5/§7 정정

- **유형**: docs(정책 동기화) / **작업유형**: 보완
- **요구사항 요약**: `attd/05-checkin-limits.md`, `attd/07-checkin-checkout.md` 를 본 변경에 맞게 정정. (본 작업지시서 §6 의 확정 문구를 그대로 반영.)
- **영향 파일**: `.claude/context/policies/attd/05-checkin-limits.md`, `.claude/context/policies/attd/07-checkin-checkout.md`, `.claude/context/policies/CHANGELOG.md`(이력)
- **선행 작업**: 없음(코드와 동시 또는 선행). **QA 가 올바른 기준으로 검증하도록 코드 머지 전 정정 권장.**
- **우선순위 근거**: 스펙·코드 일치(직전 "스펙 버그 누수" 재발 방지) → A/B 와 동시 진행.

> 작업 5개 초과 없음(A~E = 5). 추가 필요 작업(home01 status 보정)은 §7 미해결로 분리 보고.

---

## 5. UI 명세

### UI-A014-1 AttendanceTodayCard (일상세/오늘 카드) — 2번째 슬롯·버튼

- 연결 작업: prafta-app-014-C
- 화면 위치: `src/views/attd/components/AttendanceTodayCard.vue` (보완)
- 참조 패턴: 본인(기존 5변형 통합 카드), `views/main/components/AttendanceCard.vue`
- 현재 동작: `isTwoSlot`(스케줄 2구간)일 때만 2번째 슬롯/구분선 표시. primary 버튼은 `workStatus==='TWO_SLOT_WORKING'` 일 때만 "2구간 출근".
- 의도된 동작:
  - `slotCount>=2`(서버) → 슬롯 2개 + 구분선. 스케줄 미대응 2번째 슬롯은 라벨 "추가 근무", 표준화 행 숨김, 스케줄 행 "스케줄 없음".
  - primary 버튼: `actions.canCheckIn`(서버) true → "2번째 출근" 활성. 아니면 "퇴근하기"(canCheckOut).
- 레이아웃 와이어프레임(1구간 스케줄에서 2번째 출근 진행 중):
```
┌──────────────────────────────────────┐
│ 2026년 6월 2일   화요일 · 중곡사업장      │
│ [● 근무중]  주간고정                     │
│ ── 1구간 ───────────────────────────── │
│  스케줄  09:00~18:00   휴게60 · 8시간     │
│  근태    09:02~18:05   출근 중곡          │
│  표준화  09:00~18:00   근무시간 8시간     │
│ ── 추가 근무 ─────────────────────────── │  ← schedule=null
│  스케줄  스케줄 없음(추가근무)             │
│  근태    19:10 ~ -      출근 중곡          │
│  (표준화 행 없음)                         │
│ [ℹ 안내문구]                             │
│ [ 수정 요청 ]            [ 퇴근하기 ]     │
└──────────────────────────────────────┘
```
- 컴포넌트 매핑: native button(기존 그대로, 신규 공통 컴포넌트 도입 없음 — 본 카드는 자체 스타일 컴포넌트).
- 상태별 동작:
  - loading: `detail==null` → 렌더 안 함(기존).
  - empty: 슬롯 없음(미래 등) → 기존.
  - success: slotCount 1/2 분기. canCheckIn/canCheckOut 으로 버튼.
- 사용자 플로우: 진입 → (1번째 퇴근 완료 & canCheckIn) → "2번째 출근" → emit('action', checkIn) → (developer: 출근 모달→POST check-in) → 2번째 슬롯 진행 → 퇴근.
- 백엔드 의존: `GET /appApi/attd/my/today|day-detail` (prafta-app-014-B, `slotCount`/`canCheckIn`/`schedule=null`).

### UI-A014-2 AttendanceCard (홈 출퇴근 카드) — 2번째 출근

- 연결 작업: prafta-app-014-D
- 화면 위치: `src/views/main/components/AttendanceCard.vue` (보완)
- 참조 패턴: 본인.
- 현재 동작: 출근/퇴근 2분할 버튼, `canCheckIn`/`canCheckOut`(서버) 직결. 1구간 1회 퇴근 후엔 OFF_WORK(퇴근 완료)로 canCheckIn=false.
- 의도된 동작: 서버가 effective 기준으로 `canCheckIn=true`(2번째 가능) 를 내려주면 출근 버튼 재활성. 골격은 거의 불변(서버 계약 변경이 본질). status 라벨은 home01 보정 결정(§7) 따라감.
- 와이어프레임: 기존과 동일(출근/퇴근 2분할). 변화는 버튼 활성 상태뿐.
- 백엔드 의존: `GET /appApi/home01/home-summary` (status/canCheckIn) — §7 미해결.

---

## 6. 정책서 정정 문구 (prafta-app-014-E 가 반영)

### 6.1 `attd/05-checkin-limits.md` 정정

**§5.1 표 교체:**

| 스케줄 | 최대 출근 가능 횟수 | 설명 |
| --- | --- | --- |
| 1구간 스케줄 | **2회** | 1구간 출근→퇴근 후 추가 1회까지(2번째는 일반 근무, 스케줄 미대응=원본시각) |
| 2구간 스케줄 | 2회 | 출근→퇴근→재출근→퇴근 2쌍 |
| 스케줄 없음 | **2회** | 직전 퇴근 후 재출근 가능, 단 하루 2회 상한. 전량 추가근무 상신 대상(§7.5) |

추가 문장: "출근 가능 횟수는 스케줄 구간 수와 무관하게 **하루 최대 2회**로 한다. 2번째 출근은 일반 근무로 기록하며, 필요 시 해당 근무 전체를 초과근무로 사후 상신할 수 있다(§9.3)."

**§5.3 정정:** "스케줄 구간 수를 초과하는" → "**하루 2회를 초과하는**".

**§5.4 표 정정:**

| 상황 | 처리 |
| --- | --- |
| 1구간 스케줄에서 출근 1회·퇴근 후 재출근 | **허용(하루 2회 이내, 2번째는 일반 근무)** |
| 하루 2회 출근 완료 후 추가 출근 필요 | 초과 차단 → 초과근무 상신 |
| 퇴근 미등록 상태에서 재출근 시도 | 차단. 근태 보정 요청으로 해소 |
| 스케줄 없는 날 출근(2회 이내) | 허용 + 전량 추가근무 상신 필수 |
| 스케줄 없는 날 2회 초과 출근 | 차단 → 초과근무 상신 |

### 6.2 `attd/07-checkin-checkout.md` 정정

**§7.1:** "2구간 근무일은 출근/퇴근 최대 2회가 정상 흐름" → "**하루 출근/퇴근은 최대 2회**(스케줄 구간 수와 무관)". "출근 가능 횟수는 스케줄 구간 수에 따름" → "출근 가능 횟수는 **하루 2회 상한**(5장 참조)".

**§7.5 정정:** 항목 추가 "출퇴근 등록은 허용하되 **하루 2회까지로 제한**(2회 초과는 차단, 초과근무 상신 대상)."

### 6.3 `CHANGELOG.md`

"prafta-app-014: 출근 상한을 스케줄 구간 수 기반에서 '하루 2회 고정'으로 변경(§5.1/§5.3/§5.4/§7.1/§7.5). 2번째 출근=일반 근무, 스케줄 미대응 슬롯은 표준화·지각/조퇴 미적용."

---

## 7. 미해결 / 사용자·메인세션 결정 필요

1. **home01(메인홈) status 산출 보정 범위**: 홈 카드 OFF_WORK/canCheckIn 은 `home01` 서비스(별도, prafta-app-013(A))가 산출. 1구간 1회 퇴근 후 "2번째 출근 가능"을 홈에서 보이려면 home01 의 attendance status·canCheckIn 도 effective 기준으로 보정해야 한다. 본 분해는 014-D(프론트)만 두었고 home01 백엔드 보정은 **별도 작업(prafta-app-014-F 후속)** 으로 분리 제안. → developer 착수 전 메인세션 확인.
2. **OFF_WORK 와 "2번째 출근 가능" 의 표현**: 홈 카드는 status 3종(BEFORE/WORKING/OFF_WORK)뿐이라 "퇴근했지만 다시 출근 가능"을 표현할 상태가 없음. 라벨/상태 추가 여부 결정 필요(예: OFF_WORK 이면서 canCheckIn=true 면 출근 버튼만 재활성, 배지는 "퇴근 완료" 유지 — 잠정안).
3. **scheduleSummary/attendanceSummary**(주/월 1줄 요약)에 2번째 슬롯 표기: 1구간 스케줄에서 2회 출근 시 `attendanceSummary` 가 slot2 를 포함하도록 `isTwoSlot` 게이트를 attdSlots 기준으로 바꿔야 함(014-B §9에 포함시켰으나 요약 함수도 동일 패턴 점검 필요).

---

## 8. 검증 시나리오 (qa 필수 케이스)

| # | 시나리오 | 기대 |
| --- | --- | --- |
| V1 | **1구간 스케줄, 1회 출근→퇴근→2번째 출근** | check-in 2번째 성공(WORK_SEQ=2, 일반근무). 일상세 슬롯2 노출(스케줄 없음/표준화 없음). |
| V2 | 1구간 스케줄, 2번째 출근 후 3번째 출근 시도 | `ATTD_400_080` 차단. |
| V3 | 1구간 스케줄, 1회 출근(미퇴근)에서 2번째 출근 시도 | `ATTD_400_081` 차단(직전 미퇴근). |
| V4 | **스케줄 없는 날 2회 출근→퇴근, 3번째 시도** | 2회까지 허용, 3번째 `ATTD_400_080` 차단(신규 캡). |
| V5 | 스케줄 없는 날 1회 출근(미퇴근)에서 2번째 출근 | `ATTD_400_081` 차단. |
| V6 | **2번째 슬롯 일상세·주·월 표시** | 일상세: 슬롯2 카드(원본시각, 표준화행 없음). 주/월: attendanceSummary 에 slot2 포함, 합계에 slot2 실근로 합산. |
| V7 | **양쪽 카드 2번째 출근 버튼** | 일상세 카드 + 홈 카드 모두 1구간/스케줄없음에서 1회 퇴근 후 출근 버튼 재활성(home01 보정 전제—§7-1). |
| V8 | 2구간 스케줄(§5.5 야간) Case A/B/C | 기존과 동일 동작(회귀). WORK_SEQ=2 강제 채번 보존. |
| V9 | 1구간 스케줄 2회 출근·둘 다 퇴근 | `completed=true`, "수정 요청"/초과근무 상신 활성, workStatus=CHECKED_OUT. |
| V10 | 표준화/지각·조퇴 | 슬롯1(스케줄 대응)만 표준화/지각·조퇴 적용. 슬롯2(미대응) 원본만. 스케줄 없는 날 슬롯1도 지각 판정 제외. |
| V11 | 월마감된 달 | check-in/out 모두 `ATTD_400_042` 차단(회귀). |
| V12 | 동시 출근(TOCTOU) 2건 | `lockUserForCheckIn` 직렬화로 2회 캡 초과 방지(회귀). |

## 9. 보안 점검 포인트 (security)

- check-in 상한 변경이 **마감/사업장 스코프/IDOR 가드를 우회하지 않는지**(step3 마감, step8 지오펜스, lockUser 직렬화 모두 step6 이후/전 위치 유지 확인).
- 조회 응답 `slotCount`/슬롯2 노출이 **타인 근태 노출로 이어지지 않는지**(기존 cmpny/site/user 스코프 쿼리 그대로 사용 — 신규 쿼리 없음 확인).
- 2회 캡이 서버측 강제인지(프론트 게이팅 의존 금지) — A/B 가 서버 throw 로 강제.

---

## 산출물 / Notion 대행 메모

- 본 작업지시서: `.claude/requests/app_requests/prafta-app-014-plan.md`
- Vue 골격(보완): `AttendanceTodayCard.vue`(014-C), `AttendanceCard.vue`(014-D) — 골격은 본 지시서 §5 + 별도 패치 제안(아래 동봉 파일에 반영하지 않고, 서버 계약 확정 후 developer 가 최소 패치). planner 가 디스크에 덮어쓰지 않음(서버 계약 의존 변경이라 골격 단독 작성 시 미완성 위험). 대신 변경점을 §5/§4-C·D 에 명시.
- **Notion 작업 로그 등록은 메인 세션이 대행**(서브에이전트 Notion 미접근). 등록 시: 작업ID prafta-app-014-A~E, 영역 app, 모듈 attd/main, 상태 분해완료, 담당 planner, 상세설명에 본 §4 요약 + 정책출처 §3 + 검증 §8.
