# 근태관리(PRAFTA) 정책서 INDEX (v1.0)

PRAFTA 근태관리 모듈 전용 정책. 공통 주제(권한/조직/계정 등)는 `../common/` 참조.

## 빠른 검색 가이드

### 근무 유형 / 교대
- 근무 유형 구성 요소 / 운영 규칙 → `03-work-types.md`
- 교대 근무 타입 → `04-shift-types.md`

### 출퇴근
- 스케줄 구간 기반 출퇴근 횟수 제한 → `05-checkin-limits.md` §5.1
- 재출근 조건 → `05-checkin-limits.md` §5.2
- 초과 출근 차단 → `05-checkin-limits.md` §5.3
- 출퇴근 기본 규칙 → `07-checkin-checkout.md` §7.1
- **GPS 지오펜스 판정** → `07-checkin-checkout.md` §7.2
- GPS 미확인 처리(IS_MOCKED, isOutsideYn) → `07-checkin-checkout.md` §7.3
- 오류 / 누락 / 중복 처리 → `07-checkin-checkout.md` §7.4
- 스케줄 없는 날의 근무 → `07-checkin-checkout.md` §7.5

### 스케줄
- 기본 스케줄 → `06-schedule.md` §6.1
- 연간 스케줄 자동 생성 → `06-schedule.md` §6.2
- 미래 반영 시 덮어쓰기 옵션 → `06-schedule.md` §6.3
- 스케줄관리 화면 기능 → `06-schedule.md` §6.4
- 스케줄 수정 경합 방지 → `06-schedule.md` §6.5
- **1일 2구간 제한** → `06-schedule.md` §6.6

### 휴가 / 연차
- 연차 타입 관리 → `08-leave.md` §8.1
- 휴가 등록·신청 → `08-leave.md` §8.2
- 출근 차단 (노무 수령 거부) → `08-leave.md` §8.3
- 시간 단위 휴가일 예외 → `08-leave.md` §8.4

### 요청 / 승인
- 스케줄 수정 요청 → `09-requests-approval.md` §9.2
- **초과근무 상신 (사후 상신 포함)** → `09-requests-approval.md` §9.3
  - ⚠️ §9.3.1 사후 상신 기한은 `../request-approval/03-policy-alignment.md` §3.2에서 D+5일 → **사업장별 근태 마감 전까지**로 재정의됨. 최신 규칙은 요청승인관리 재기획서를 따른다.
- 연차(휴가) 신청 → `09-requests-approval.md` §9.4
- 관리자 승인/반려 공통 규칙 → `09-requests-approval.md` §9.5
- 요청 승인 관리 화면 (3탭 → 4탭으로 갱신됨) → `09-requests-approval.md` §9.6
  - ⚠️ 화면 구조는 `../request-approval/` 단일 출처 우선

### 근태 계산
- 정규 근무 계산 → `10-attendance-calc.md` §10.1
- **출퇴근 시간 표준화 (30분 단위 등)** → `10-attendance-calc.md` §10.2
- 추가근무(초과근무) 인정 및 계산 → `10-attendance-calc.md` §10.3
- 자동 휴게시간 공제 → `10-attendance-calc.md` §10.4

### 보정 / 마감
- 근태 보정 대상 / 요청·승인 → `11-attendance-correction.md`
- 스케줄 마감 → `12-schedule-close.md`
- **근태 마감 (선행 조건, 차단 조건)** → `13-attendance-close.md`

### 조회 / 알림
- 근태 현황 조회 → `14-attendance-view.md`
- 근태 알림 트리거 → `15-notifications.md`

### 시나리오
- 표준 업무 흐름 → `16-scenarios.md`

## 파일 목록

| 파일 | 영역 |
| --- | --- |
| `01-overview.md` | 1장 개요 |
| `02-regulations.md` | 2장 관련 규정 및 준수 사항 |
| `03-work-types.md` | 3장 근무 유형 관리 |
| `04-shift-types.md` | 4장 교대 근무 타입 관리 |
| `05-checkin-limits.md` | 5장 출퇴근 횟수 및 구간 제한 |
| `06-schedule.md` | 6장 기본 스케줄 및 스케줄 관리 |
| `07-checkin-checkout.md` | 7장 출퇴근 정책 |
| `08-leave.md` | 8장 휴가 정책 |
| `09-requests-approval.md` | 9장 근로자 요청 · 관리자 승인 |
| `10-attendance-calc.md` | 10장 근태기록 산정 규칙 |
| `11-attendance-correction.md` | 11장 근태 보정 |
| `12-schedule-close.md` | 12장 스케줄 마감 |
| `13-attendance-close.md` | 13장 근태 마감 |
| `14-attendance-view.md` | 14장 근태 현황 조회 |
| `15-notifications.md` | 15장 근태 알림 정책 |
| `16-scenarios.md` | 16장 운영 시나리오 |
