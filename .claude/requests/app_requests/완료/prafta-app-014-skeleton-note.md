# prafta-app-014 골격 작성 현황 (planner addendum)

본 메모는 `prafta-app-014-plan.md` §산출물 의 정정/보강이다.

## 실제로 디스크에 반영한 Vue 골격

- **`AttendanceTodayCard.vue` (014-C)** — 디스크 반영 완료(display-only 패치).
  - `isTwoSlot` → 서버 `detail.slotCount` 기준으로 슬롯 개수 판정(`slotCount`/`hasMultiSlot`).
  - 스케줄 미대응 슬롯(`slot.schedule==null`) → 스케줄 행 "스케줄 없음/추가근무", **표준화 행 `v-if` 로 숨김**(D2), 구분선 라벨 "추가 근무".
  - primary 버튼 → `actions.canCheckIn`(서버 effective 산출) 단독 기준(상태 문자열 의존 제거, D4). 라벨 "출근하기"/"퇴근하기".
  - `TWO_SLOT_WORKING` 배지/알림 문구를 slotCount>=2 공통 문구로 보정.
  - 구버전 응답 폴백: `slotCount` 미제공 시 `slots.length` → `isTwoSlot` 순.
  - API/store/router 로직 미작성(emit + TODO(developer) 유지).

## 디스크 변경하지 않은 컴포넌트 (의도적)

- **`AttendanceCard.vue` (014-D, 홈 카드)** — **골격 변경 불요**.
  - 이미 `canCheckIn`/`canCheckOut`(서버 산출) props 직결이며 자체 슬롯/구간 로직이 없다.
  - 동작 변경의 본질은 **백엔드 계약**(home01 의 attendance status·canCheckIn 을 effective 기준으로 보정 — `prafta-app-014-plan.md` §7-1 미해결). 프론트 골격은 그대로 두고 서버가 canCheckIn=true 를 내려주면 출근 버튼이 자동 재활성된다.
  - 단, 호출부 `MainView.vue` 가 home01 응답의 status/canCheckIn 을 AttendanceCard props 로 매핑하는 부분은 developer 가 home01 보정과 함께 점검(신규 로직 아님).

- **`AttendanceWeekList.vue` / 월 화면** — `day.isTwoSlot` "2구간" 배지는 §5.5(스케줄 2구간) 표시 용도로 **그대로 유지**(메모리 의미 고정). 단 요약/합계는 백엔드(014-B §9)가 attdSlots 기준으로 내려주므로 프론트 표시는 자동 반영(골격 변경 불요).

## developer 인계 포인트

1. 백엔드(014-A/B) 머지 후 `slotCount` 필드가 응답에 실제로 실리는지 확인(없으면 Today 카드는 폴백 동작).
2. home01 status 보정(§7-1) 결정 전까지 홈 카드의 "1구간 1회 퇴근 후 2번째 출근" 은 V7 일부 미충족 가능 — qa 는 §7-1 전제 명시 후 검증.
