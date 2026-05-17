# 요청승인관리 재기획서 INDEX (v0.1)

PRAFTA 근태관리 정책서 §9.6의 "요청 승인 관리 화면"을 별도 문서로 분리·재기획한 단일 출처. 화면 구성과 처리 흐름은 본 문서를 따른다.

## 빠른 검색 가이드

### 단일 출처 선언
- 작성 배경 / 단일 출처 선언 → `01-overview.md` §1
  - "요청 승인 관리 화면" 자체에 대한 정의는 본 문서가 단일 출처. 근태 정책서 §9.6은 본 문서를 참조하는 형식으로 운영.

### 화면명 / 정책 변경 사항
- 화면명 검토 결과 → `02-screen-naming.md`
- **§9.6 3탭 → 4탭 변경** → `03-policy-alignment.md` §3.1
- **§9.3.1 사후 상신 기한 변경 (D+5일 → 사업장별 근태 마감 전까지)** → `03-policy-alignment.md` §3.2
- 자동 마감 금지 · 강제 마감 미도입 → `03-policy-alignment.md` §3.3
- v1 스코프 — 임금 정산 미포함 → `03-policy-alignment.md` §3.4

### IA / 권한
- 권한별 가시 범위 → `04-ia-permissions.md`

### 화면 구조
- 전체 레이아웃 → `05-screen-structure.md` §5.1
- 페이지 헤더 & 알림 배너 → `05-screen-structure.md` §5.2
- **4탭 구성 (스케줄/근태보정/초과/연차)** → `05-screen-structure.md` §5.3
- 필터·검색·정렬 → `05-screen-structure.md` §5.4
- 접수함 리스트 컬럼·정렬·시각화 → `05-screen-structure.md` §5.5
- 일괄 처리 영역 → `05-screen-structure.md` §5.6
- 상세 패널 공통 구조 → `05-screen-structure.md` §5.7
- 상세 패널 탭별 차이 → `05-screen-structure.md` §5.8

### 결재 플로우
- 스케줄 수정 요청 (§9.2) → `06-approval-flows.md` §6.1
- 근태 보정 요청 (§11) → `06-approval-flows.md` §6.2
- 초과근무 상신 (§9.3) → `06-approval-flows.md` §6.3
- 연차 상신 (§9.4) → `06-approval-flows.md` §6.4

### 인터랙션
- 행 클릭·상세 진입 → `07-interactions.md` §7.1
- 선점(처리 잠금) → `07-interactions.md` §7.2 (공통 §9 참조)
- 본인 결재 → `07-interactions.md` §7.3
- 마감 차단 → `07-interactions.md` §7.4
- 충돌 경고 → `07-interactions.md` §7.5
- 시간 조정 후 승인 → `07-interactions.md` §7.6
- 일괄 처리 → `07-interactions.md` §7.7
- 알림·실시간 갱신 → `07-interactions.md` §7.8

### 예외 / 데이터 / 권고
- 예외 케이스 → `08-edge-cases.md`
- **공통 엔티티 ApprovalRequest** → `09-data-structures.md` §9.1
- 서브타입 페이로드 (schedule_edit / attendance_correction / overtime / annual_leave) → `09-data-structures.md` §9.2
- 권한·스코프 연동 → `09-data-structures.md` §9.3
- 마감 기준일 데이터 연동 → `09-data-structures.md` §9.4
- **정책 개정 권고 (근태 정책서 반영 사항)** → `10-revision-recommendations.md`
- 확인 사항 (Open Questions) → `11-open-questions.md`
- 변경 이력 → `12-changelog.md`

## 파일 목록

| 파일 | 영역 |
| --- | --- |
| `01-overview.md` | 1장 문서 개요 + 단일 출처 선언 |
| `02-screen-naming.md` | 2장 화면명 검토 |
| `03-policy-alignment.md` | 3장 정책 정합성 점검 (변경 사항) |
| `04-ia-permissions.md` | 4장 IA·권한·플랫폼 |
| `05-screen-structure.md` | 5장 화면 구조 (재기획) |
| `06-approval-flows.md` | 6장 케이스별 결재 플로우 |
| `07-interactions.md` | 7장 인터랙션 정의 |
| `08-edge-cases.md` | 8장 예외 케이스 |
| `09-data-structures.md` | 9장 데이터 구조 |
| `10-revision-recommendations.md` | 10장 정책 개정 권고 |
| `11-open-questions.md` | 11장 확인 사항 |
| `12-changelog.md` | 변경 이력 |
