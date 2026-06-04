# prafta-app-007 — 모바일 앱 근태 요청 폼 3종 (스케줄 수정 / 근태 보정 / 초과근무 신청) 분해 plan

> **작업 ID prefix**: `PRAFTA-APP-007`
> **단일 출처 (SSOT) 선언**: 본 plan 이 prafta-app-007 의 단일 출처이다. 후속 developer / qa / security 는 본 plan 만 정독하면 된다.
>
> **폐기 대상 (절대 다시 정독 금지)**:
> - `.claude/requests/app_requests/prafta-app-007.md` — 본 plan 으로 대체. 원본 요청서는 의도만 명시(연결 포인트 찾기·웹 프로세스 참고)되어 있어 본 plan 이 가정 / 결정 / 분해 / 골격을 모두 새로 정의한다.
> - `.claude/requests/app_requests/refs/prafta-app-007/prafta_request_forms_v8.html` 의 §4 연차 케이스 (5화면) 와 §5 결재선 케이스 (4화면) — 본 작업 범위 외. 연차 신청은 prafta 기존 `LeaveFlow` 와 `LeaveApplyPop` 등으로 처리. 결재선 통합은 `.claude/requests/app_requests/prafta-app-009.md` 로 분리.
> - 시안 화면들의 시각 톤 (여백·라운드·배지·구간 카드 패턴) 만 참조용. 시안 안의 임의 값 (예: ST002 조기근무 같은 SCH_CD 라벨 형식, "신청 합계 1시간 30분" 색상) 은 prafta 기존 CSS 변수 팔레트로 매핑.
>
> 본 plan 과 원본 요청서가 충돌하면 **본 plan 이 무조건 우선**한다.

---

## 0. 개요

### 0.1 배경

PRAFTA 모바일 앱 `MyAttendanceView` (`prafta-app-002`) 의 "이번주 카드 탭 → 바텀시트 4종 액션" 중 다음 3종이 1차 stub(`showAlert('준비 중입니다')`) 으로 남아 있다:

1. 스케줄 수정 요청 (`scheduleModify`)
2. 근태 보정 요청 (`attendanceCorrection`)
3. 초과근무 신청 (`overtime`)

네 번째 액션인 연차 신청(`leave`) 은 기존 prafta `LeaveFlow` 자산(`com.prafta.web.attd.leaveflow`) 이 있으므로 본 작업 범위 외 (별도 라운드).

본 작업은 위 3종을 백엔드 endpoint 신설 + 모바일 라우트·폼 화면 신설로 1차 완성한다. **결재선 영역은 1차 미포함** (단순 등록 → 관리자 승인 흐름). 결재선 통합·USER_04 프리셋 연동은 prafta-app-009 로 분리.

### 0.2 본 작업의 실제 범위

- **DB 스키마 변경 1개 파일**: 마이그레이션 1건 (`prafta-app-007-attd-req-extensions.sql`)
  - `tb_syst_val_d` 에 SYS032 신규 코드 `10` (스케줄 수정 요청) 추가
  - `tb_user_attd_req` 에 `SCH_CD varchar(20) NULL` 컬럼 추가 (SYS032=10 전용)
- **백엔드 1개 패키지 신규** (`com.prafta.app.req.req07`):
  - 3개 endpoint: `POST /appApi/req07/sched-modify`, `POST /appApi/req07/attd-correction`, `POST /appApi/req07/overtime`
  - Controller / Service / Mapper / DTO 한 벌 + 등록 채번 SQL.
- **프론트엔드 1개 라우트 + 1개 컨테이너 + 3개 폼 컴포넌트 + 1개 슬롯 카드 컴포넌트** (`prafta-app-frontend/src/views/req/AttdRequest*`).
- **진입 동선 연결** 3곳:
  - `MyAttendanceView.vue` 의 `onSheetAction` 본문 교체 (3종 라우팅).
  - `MyAttendanceView.vue` 의 `onDayDetailAction` 본문 교체 (`AttendanceDayDetailCard` 의 빠른 액션 2종).
  - `MyAttendanceView.vue` 의 `onTodayAction` 의 `requestModify` 분기 — 1차 stub 유지(범위 외, follow-up).
- **라우트 1개 신설**: `/AttdRequest` (PascalCase, 비-public, 쿼리 파라미터 기반 폼 분기).

### 0.3 본 작업의 실제 비범위 (폐기·미구현)

- **연차 신청 폼** — 기존 LeaveFlow 가 있음. 시안 §4 5화면은 본 작업과 무관.
- **결재선 영역** (시안 §5 4화면 / prafta-020 USER_04 프리셋 / `tb_user_attd_req_approval` INSERT 흐름) — `.claude/requests/app_requests/prafta-app-009.md` 로 분리.
- **요청 상세 화면** — prafta-app-006 §7 follow-up F1 미구현 상태. 본 작업의 등록 endpoint 만 우선.
- **사용자 직접 취소** — prafta-app-006 Q3 결정 (정책 미정). 본 작업도 동일.
- **스케줄 선택 UX 의 native picker 대체 / 일자 셀렉터** — 1차는 native `<input type="date">` / native `<select>` (또는 단순 BottomSheet) 로 단순화.
- **충돌 검증 (이미 같은 일자에 같은 종류 미처리 요청 존재 시)** — 본 plan 의 P10 결정으로 추가 (단순 중복 체크 only).
- **푸시 알림** — `prafta-031` outbox consumer 미구현. 등록 직후 노티 발송은 후속.
- **2구간 시간 겹침 검증** — `attd §6.6` "겹침 금지". 본 plan P9 결정 — 1차는 클라 단순 경고 + 서버 강제 검증.

### 0.4 사용자 1차 컨펌 (자율 진행 모드)

본 plan 은 `prafta-app-006-plan.md` 와 동일하게 사용자 컨펌 없이 한 번에 작성한다. 결정 포인트는 §1.B planner 결정으로 명시했다. 메인 세션이 본 plan 본문을 그대로 Notion "작업 로그" 에 일괄 등록한다 (planner 는 Notion 직접 접근 불가 — `feedback_subagent_no_resume.md`).

---

## 1. 결정 사항

### 1.A 사용자 확정 결정 (2026-05-29 채팅)

| # | 항목 | 결정 |
|---|---|---|
| **Q1** | 스케줄 수정 REQ_TYPE | **SYS032 신규 코드 `10` (스케줄 수정 요청) 추가** + 운영 마이그레이션 1건. 기존 02 (근태 수정) 와 구분 (시각 수정 vs 스케줄 자체 변경). |
| **Q2** | 근태 보정 REQ_TYPE | **출퇴근 행 유무 자동 분기**: 해당 일자에 기존 출퇴근 행이 없으면 `01 근태 생성 요청`, 있으면 `02 근태 수정 요청`. 백엔드 Service 가 분기 판정. 클라가 보내는 reqType 무시 (서버 결정). |
| **Q3** | 결재선 영역 | **1차는 결재선 미포함** — 단순 등록 → 관리자 승인 흐름. prafta-020 USER_04 결재라인 프리셋 연동은 **prafta-app-009 로 분리** (본 plan §7 명시). |
| **Q4** | 2구간 데이터 모델 | **단일 REQ_ID + WORK_SEQ 분리해 행 2개**. 1구간 = WORK_SEQ=1, 2구간 = WORK_SEQ=2. 동일 REQ_ID 그룹화. 관리자 승인 시 두 행 동시 처리. tb_user_attd_req 의 기존 WORK_SEQ 컬럼 활용. |
| **Q5** | 진입 동선 | **별도 라우트 풀스크린** — `/AttdRequest?type=...&workYmd=...` 변형. prafta-app-006 `MyRequestsView` 패턴 일관. **모달 아님**. |
| **Q6** | 백엔드 패키지명 | `com.prafta.app.req.req07` — prafta-app-007 작업 ID 매핑. prafta-app-006 `req06` 패턴 일관. |

### 1.B planner 결정 포인트 (가이드라인·기존 패턴 근거 자율 결정)

| # | 결정 포인트 | planner 결정 | 근거 |
|---|---|---|---|
| **P1** | endpoint 개수 | **3개 endpoint** (`/sched-modify`, `/attd-correction`, `/overtime`) — 단일 endpoint + reqType 디스패치 불채택. | 각 폼의 입력 검증 규칙·필수 필드가 다르고 (SCH_CD vs START/END_TIME vs OT_TYPE), 서버 분기 (Q2) 도 endpoint 별로 명시적. DTO 별도 → IDE 지원 강함. |
| **P2** | REQ_ID 채번 | **기존 `LeaveFlowMapper.selectNextReqId` 패턴 재사용** — `SELECT CONCAT(DATE_FORMAT(NOW(), '%Y%m%d'), FNC_CMM_SEQ_NEXTVAL(#{cmpnyCd}, 'ATTD_REQ_ID')) FROM DUAL`. | LeaveFlow 의 연차 REQ_ID 와 동일 채번 함수. SCH/AT/OT 접두어 분리 안 함 (정책서 9.1 의 SC/AT/OT 접두어는 재기획서 9.1 "id" 필드의 *권고* 형식이지 강제 아님). |
| **P3** | REQ_STATUS 초기값 | **`'01' 신청` 고정** (등록 직후). | `LeaveFlowMapper.insertLeaveReq` 는 결재 유무에 따라 `aprvRequired ? '01' : '02'` 분기지만, 본 작업은 결재선 미포함 (Q3) → 항상 신청 상태 (관리자 처리 대기). |
| **P4** | 등록 시 알림 발송 | **1차 미발송** — `prafta-031` outbox consumer 미구현. INSERT 만. 알림은 §7 follow-up. | 기존 `LeaveFlowServiceImpl.applyLeave` 도 outbox INSERT 없이 INSERT 만 수행 (메모리 `project_prafta_031_recall_and_outbox`). 본 작업도 동일 패턴. |
| **P5** | 진입 동선 라우트 구조 | **단일 라우트 + 쿼리 파라미터 기반 폼 분기**: `/AttdRequest?type={schedModify|attdCorrection|overtime}&workYmd=YYYYMMDD&nodeCd=N001`. | 3종 라우트 신설보다 router 단순. 메뉴 트리 (DB 동적 라우트) 미등록이라 정적 라우트 1개로 충분. type 검증은 컨테이너에서 분기. |
| **P6** | 컨텍스트 (현재 스케줄·현재 근태) 조회 | **본 작업 endpoint 에 포함시키지 않음** — 화면 진입 시 `MyAttendanceView` 가 이미 day-detail 응답을 가지고 있으므로 라우트 진입 시 day 객체를 router state 또는 sessionStorage 로 전달. 신규 endpoint 없음. | 시안 §1-1 의 "현재 스케줄 ST002 조기근무 / 07:00~15:00 · 1구간" 같은 컨텍스트는 day-detail 응답에 모두 있음 (`day.scheduleSummary`, `day.attendanceSummary`, `day.workPlanName` — `AttendanceActionSheet.vue` metaText 산출). 신규 컨텍스트 endpoint 신설 시 데이터 중복. |
| **P7** | 컨텍스트 전달 방식 | **`sessionStorage` 키 `attd_req_ctx_v1`** 에 day 객체 (workYmd, scheduleSummary, attendanceSummary, workPlanName, slots) 직렬화 + 라우트 진입. 라우트 진입 후 1회 읽고 즉시 제거(stale 방지). | router state 는 페이지 새로고침 시 소실. sessionStorage 는 webview 종료 시 자동 정리. PII 없음 (시각·라벨만). |
| **P8** | 스케줄 코드 (SCH_CD) 저장 | **`tb_user_attd_req` 에 `SCH_CD varchar(20) NULL` 컬럼 신규 추가** (마이그레이션 동일 파일). REQ_TYPE='10' 일 때만 채움, 다른 REQ_TYPE 은 NULL. | 옵션 (a). REQ_REASON 직렬화(옵션 b) 는 해킹스러움. 별도 테이블(옵션 c) 은 단일 컬럼 추가 대비 과한 분리. SCH_CD 는 변경 *목표* 스케줄 코드. 관리자 승인 시 이 값으로 스케줄 변경. |
| **P9** | 2구간 겹침/입력 검증 | **클라 단순 경고 + 서버 강제 검증**. 정책 `attd/06-schedule.md §6.6` "1일 2구간 제한, 겹침 금지". 서버 측: `slot1.endTime <= slot2.startTime` (자정 경계 보정 미적용) 위반 시 4xx. 클라 측: 입력 후 인라인 경고 노출(차단 안 함, 사용자가 보내기 누르면 서버 응답으로 차단). | 시안 §1-2, §2-3 의 2구간 카드 UI 만 있고 검증 가이드는 없음. 정책서 §6.6 명시. 클라 차단까지는 1차 과잉 — 서버 fail-closed 가 정답. |
| **P10** | 중복 요청 차단 | **추가** — 동일 (USER_CD, WORK_YMD, REQ_TYPE) + REQ_STATUS='01' 신청 중인 행이 있으면 4xx (`ATTD_REQ_DUPLICATE`). | 사용자가 같은 일자에 같은 종류 미처리 요청을 두 번 등록하면 관리자 중복 처리 위험. fail-closed. 한국어 메시지 "이미 등록된 미처리 요청이 있습니다". |
| **P11** | 본인 조회 가능 일자 가드 | **서버 검증 — `AppAttd01ServiceImpl.computeWeekActions` 동일 정책 사용**: <br>- 스케줄 수정 (`10`): 미래 + SCH_CD 존재 + 미마감.<br>- 근태 보정 (`01/02`): 과거(SCH_CD 존재) OR 오늘(완료) + 미마감.<br>- 초과근무 (`03`): 과거 OR 오늘 + 완료 + 미마감. | 클라 actions 와 서버 가드가 동일 정책. IDOR 방지: 본인 USER_CD 기준. 마감 판정은 기존 `isMonthClosed` 재사용. |
| **P12** | OT_TYPE 입력 방식 | **3종 선택 시트 (EXTEND/NIGHT/HOLIDAY)** — `BaseBottomSheet` 재사용 + 단일 선택. 기본값 EXTEND. | 시안 §3 에는 OT_TYPE 입력 UI 가 명시되지 않았으나 DB 컬럼 필수 아님 (NULL 허용). 다만 관리자가 분류해야 하므로 클라 필수 입력 화면이 필요. 단일 선택 BottomSheet 가 자연. |
| **P13** | 1구간 only vs 2구간 분기 | **"구간 추가" 버튼 — 슬롯 1개일 때만 노출**(2구간 도달 시 숨김, 시안 §2-3 "3구간 추가 불가"). **`workSeq` 는 구간 식별자(1/2)이지 배열 위치가 아니다.** 삭제 시 남은 구간의 `workSeq` 를 재인덱싱하지 말 것 — 1구간 삭제 시 2구간은 그대로 `workSeq=2` 로 유지(제출도 2). "구간 추가" 는 비어 있는 번호(1 또는 2)를 채우고 오름차순 정렬한다. (정정: 2026-06-02 — 기존 "삭제 시 1구간으로 축소/재인덱싱" 가정은 '1구간 승인·2구간 반려 → 2구간만 재신청' 워크플로우에서 오동작하여 폐기.) | 시안 §1-1/§2-1/§3-1 (1구간) → "구간 추가" 노출. §1-2/§2-2/§3-2 (2구간) → 삭제 버튼 노출. 어느 구간이든 단독 삭제 가능하며 남은 구간의 식별자는 보존. |
| **P14** | 사유(REQ_REASON) 최대 길이 | **500자** (DB 컬럼 길이). 클라는 100자 카운터(시안 §1-1 `0/100`) 표시하되 서버에서는 500 까지 허용 (1차 시안 UI 만 100). | DB `varchar(500)` 한계. 시안 카운터는 UX 가이드일 뿐 강제 아님. 단 클라 카운터는 100 그대로 (시안 일관). |
| **P15** | 시간 입력 형식 | **`HH:MM` (콜론 포함)** 클라 표시 / **`HHmm` (4자리, 콜론 없음)** 서버 저장 — DB `START_TIME varchar(4)` 형식. 클라가 변환해 전송. | 기존 `LeaveFlowMapper.insertLeaveReq` 의 startTime 컬럼이 `varchar(4)` HHmm 사용. |
| **P16** | 폼 진입 시 초기값 | **현재 스케줄/근태 시각을 기본 입력값으로 프리필** (시안 §2-1: 현재 09:42~18:02 → 보정 폼에 09:30/18:02 표시). day 객체에서 slot 별 출근/퇴근 시각 가져와 입력 필드에 채움. | UX: "보정" 의도라면 현재값에서 1~2분만 변경. 빈 폼 시작은 입력 부담. |
| **P17** | 라우팅 후 성공 처리 | **성공 alert + `router.back()`** — `MyAttendanceView` 로 복귀. day-detail 재조회는 `MyAttendanceView.onMounted` 가 처리 (KeepAlive 미사용 — prafta-app-006 P10 동일). | `router.back()` 후 `MyAttendanceView` 가 재 mount 되어 갱신된 actions 노출. |
| **P18** | 디자인 토큰 | `MyAttendanceView/.my-attd-view` 의 토큰 세트 그대로 1회 선언 — `.attd-req-view` 루트. 신규 색상 도입 금지. | MyAttendanceView 가 가장 가까운 형제 화면. 일관성. |
| **P19** | 인라인 SVG sprite | CDN 의존 금지. 본 화면 전용 sprite `<svg width="0" height="0">` 정의 (chev-left, x, plus, trash, info-circle, check). | `MyAttendanceView`, `RequestFilterBar` 패턴 동일. tabler-icons CDN 사용 안 함. |
| **P20** | NODE_CD 처리 | **day 객체의 nodeCd 또는 day-detail 응답의 nodeCd 를 그대로 사용**. 클라가 보낸 nodeCd 는 무시하지 않고 *검증* (서버가 본인의 해당 일자 스케줄/근태의 nodeCd 와 일치 확인). | 정책 가드. 본인이 임의 노드에 요청 등록 차단. |

### 1.C 정책서 출처

| 결정 | 정책서 섹션 |
|---|---|
| 스케줄 수정 요청 조건/처리 | `attd/09-requests-approval.md` §9.2 (스케줄 수정 요청) + `request-approval/06-approval-flows.md` §6.1 |
| 근태 보정 요청 조건/처리 | `attd/11-attendance-correction.md` §11.1, §11.2 + `request-approval/06-approval-flows.md` §6.2 |
| 초과근무 상신 조건/처리 (사후 상신 기한 — 사업장 마감 전까지) | `attd/09-requests-approval.md` §9.3 + `request-approval/03-policy-alignment.md` §3.2 + `request-approval/06-approval-flows.md` §6.3 |
| 1일 2구간 제한 / 겹침 금지 | `attd/06-schedule.md` §6.6 |
| 스케줄 마감 / 근태 마감 | `attd/12-schedule-close.md`, `attd/13-attendance-close.md` |
| 본인 데이터 본인만 (IDOR 가드) | `common/08-permissions.md` §8.4 |
| JWT 토큰 식별 (gv_cmpnyCd/gv_siteCd/gv_userCd) | `common/03-account-auth.md` §3.4 |
| 요청/승인/반려 이력 보존 | `attd/09-requests-approval.md` §9.2~§9.4 (이력 보존 행) |
| 알림 채널 / 트리거 (1차 미구현, follow-up) | `common/10-notifications.md` |
| 모바일 UI 디자인 가이드 | `common/13-ui-ux.md` §13.2~§13.3 |
| 재기획서 §6.3 "마감 도달 시 사후 상신 차단" 서버 가드 | `request-approval/06-approval-flows.md` §6.3 (마감 연동) |

---

## 2. 원본 요청서·시안에서 잘라낸·보류한 항목

### 2.1 폐기 (다시 재고하지 않음)

| 원본/시안 항목 | 폐기 사유 |
|---|---|
| 시안 §4 연차 5화면 (4-1 일/4-2 반차/4-3 시간차/4-4 다일/4-5 다일 다구간) | 본 작업 범위 외. prafta `LeaveFlow` 자산 존재. |
| 시안 §5 결재선 케이스 4화면 (5-1 1단/5-2 2단/5-3 3단/5-4 4단) | Q3 — `prafta-app-009.md` 로 분리. |
| 원본 요청서 "초과근무 수정 요청" 가능성 | 원본 요청서 본문 "초과근무의 경우 수정요청은 없고 신규로 요청하는 기능만 존재" 명시. SYS032=04 (초과근무 수정) 는 백엔드 `prafta-025` 가 구현 (수정 폼 화면은 본 작업 미포함). |
| 시안 카운터 `0/100` 의 100자 강제 | P14 — 클라는 100 가이드만, DB 는 500 까지 허용. |
| 시안 시각 톤 (특정 픽셀·구체 색상값) | P18 — `.attd-req-view` 토큰 세트 사용. 시안 값 직접 사용 금지. |
| tabler-icons CDN (`<i class="ti ti-x">`) | P19 — 인라인 SVG sprite 만. |
| 시안의 SCH_CD 라벨 형식 "ST002 조기근무" 직접 사용 | 라벨은 기존 `selectScheduleByRange` 결과의 workPlanName/scheduleSummary 그대로. SCH_CD 자체는 서버 라우팅 키. |
| 원본 요청서 "팝업" 표현 | Q5 — 모달이 아닌 풀스크린 라우트. 시안의 BottomSheet 풀스크린은 라우트 풀스크린으로 매핑. |

### 2.2 보류 (§7 follow-up 후보)

| 항목 | 보류 사유 |
|---|---|
| 결재선 통합 (시안 §5, prafta-020 USER_04 프리셋) | Q3 — `prafta-app-009.md` 로 분리. |
| 연차 신청 폼 (시안 §4) | 기존 LeaveFlow 자산. 별도 라운드. |
| 알림 발송 (등록 직후 관리자 push) | P4 — outbox consumer 미구현. |
| 일자 셀렉터 컴포넌트 (`<input type="date">` 대체) | prafta-app-006 §7 F8 와 동일. |
| OT 면제 시간/표준화 단위 안내 (시안 §3-1 의 "스케줄(09:30~18:00) 종료 후, 근태 퇴근(19:38) 사이의 18:00 ~ 19:38 까지 신청할 수 있어요") | 1차는 단순 입력. 면제 시간 계산은 `attd §10.2~§10.3` 인용 — 별도 가공. |
| 2구간 겹침 클라 차단 (현재는 서버만 차단) | P9 — UX 강화. |
| 사후 상신 기한 임박 배지 | `request-approval/03-policy-alignment.md` §3.2 "D-N영업일" 배지 — 관리자 화면 영역. |
| 요청 등록 후 상세 화면 이동 (현재는 router.back) | F1 (prafta-app-006 §7) 완료 후 결정. |
| OT_TYPE 자동 추정 (스케줄 종료 후 → EXTEND 등) | 1차는 사용자 선택. |
| 모바일에서 동시에 다건 일자 등록 | 별도 화면 패턴. 본 작업은 단일 일자만. |

---

## 3. 영향 범위 스캔

### 3.1 DB 스키마 변경 (1개 파일 / 운영 적용은 사용자 수동)

**파일**: `prafta-backend/src/main/resources/sql/migration/prafta-app-007-attd-req-extensions.sql`

내용 (2 부분):

```sql
-- ────────────────────────────────────────────────────────────
-- prafta-app-007: SYS032 신규 코드 추가 + tb_user_attd_req.SCH_CD 컬럼 추가
-- 운영 적용 전 부재 확인 권장:
--   SELECT COUNT(*) FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS032' AND SYST_VAL_D_CD='10';
--   SHOW COLUMNS FROM tb_user_attd_req LIKE 'SCH_CD';
-- ────────────────────────────────────────────────────────────

-- 1) SYS032=10 "스케줄 수정 요청" 디테일 행 추가 (마스터는 이미 존재)
INSERT INTO tb_syst_val_d (
      CMPNY_CD
    , SYST_VAL_CD
    , SYST_VAL_D_CD
    , SYST_VAL_D_NM
    , SORT_IDX
    , USE_YN
    , DEL_YN
    , INSERT_NO
    , INSERT_DATE
) VALUES (
      '*'           -- 전사 공통 (운영 적용 시 회사별 INSERT 필요 시 분리)
    , 'SYS032'
    , '10'
    , '스케줄 수정 요청'
    , 10
    , 'Y'
    , 'N'
    , 'SYSTEM'
    , NOW()
);

-- 2) tb_user_attd_req 에 SCH_CD 컬럼 추가
--    REQ_TYPE='10' (스케줄 수정 요청) 일 때만 값 채움.
--    다른 REQ_TYPE 은 NULL.
ALTER TABLE tb_user_attd_req
    ADD COLUMN SCH_CD varchar(20) NULL
    COMMENT '스케줄 코드 (SYS032=10 스케줄 수정 요청 시 변경 목표 SCH_CD)'
    AFTER LEAVE_DAYS;
```

> **CMPNY_CD='*'** 패턴은 운영 DB 의 `tb_syst_val_d` 실제 스키마 확인 후 조정 필요. 회사별 INSERT 가 표준이면 운영 적용 시 사용자가 분기.
> SYST_VAL_D 의 INSERT 가 회사 단위가 아닌 경우 (메모리 `feedback_db_comment_code_convention`) 컬럼 명세 조정.

### 3.2 백엔드 신규 (1 패키지 / 3 endpoint)

```
com.prafta.app.req.req07
├── controller/AppReq07Controller.java          (신규 — 3 endpoint)
├── service/AppReq07Service.java                (신규 인터페이스)
├── service/impl/AppReq07ServiceImpl.java       (신규)
├── mapper/AppReq07Mapper.java                  (신규)
├── application/
│   ├── param/
│   │   ├── SchedModifyParam.java               (record, JWT 기반)
│   │   ├── AttdCorrectionParam.java            (record)
│   │   └── OvertimeParam.java                  (record)
│   └── command/
│       └── AttdReqInsertCommand.java           (record — INSERT 시 매퍼 직결)
├── dto/
│   ├── request/
│   │   ├── SchedModifyRequest.java
│   │   ├── AttdCorrectionRequest.java
│   │   ├── OvertimeRequest.java
│   │   └── SlotRequest.java                    (공통 slot 1/2)
│   └── response/
│       └── RegisterReqResponse.java            ({ reqId, reqType, reqStatus, workSeqs[] })

src/main/resources/com/prafta/app/req/req07/mapper/
└── AppReq07Mapper.xml                          (신규 — 채번 SELECT + 3 INSERT + 중복 체크 SELECT + 가드 SELECT)
```

### 3.3 프론트엔드 신규 (앱)

```
prafta-app-frontend/src/views/req/
├── AttdRequestView.vue                         (라우트 컨테이너 — type 분기, 헤더, 푸터 공통)
└── components/
    ├── SlotCard.vue                            (1구간/2구간 공통 카드 — slot tag + 입력 영역 slot + 삭제 버튼)
    ├── SchedModifyForm.vue                     (스케줄 수정 폼 — SCH_CD 단일 입력)
    ├── AttdCorrectionForm.vue                  (근태 보정 폼 — 출근/퇴근 시각 입력)
    └── OvertimeForm.vue                        (초과근무 폼 — 시작/종료 시각 + OT_TYPE 선택)
```

(BaseBottomSheet 는 prafta-app-006 에서 신설 — 본 작업은 재사용. OT_TYPE 선택 시 import.)

### 3.4 라우트 / 진입 동선

| 파일 | 변경 |
|---|---|
| `prafta-app-frontend/src/router/index.js` | `routes[]` 에 `{ path: '/AttdRequest', name: 'AttdRequest', component: () => import('@/views/req/AttdRequestView.vue') }` 추가. publicPaths 미추가. |
| `prafta-app-frontend/src/views/attd/MyAttendanceView.vue` | `onSheetAction` 본문 교체 (3종 type 별 라우팅 + sessionStorage 컨텍스트 저장). `onDayDetailAction` 본문 교체 (attendanceCorrection/overtime 라우팅). |

---

## 4. 작업 단위 분해

### 4.1 작업표

| 작업 ID | 유형 | 영역 | 모듈 | 작업유형 | 요구사항 요약 |
|---|---|---|---|---|---|
| **PRAFTA-APP-007-1** | backend | app | req/req07 | 신규 | 마이그레이션 1건 작성 (SYS032=10 추가 + tb_user_attd_req.SCH_CD ADD COLUMN). 운영 적용은 사용자 수동. |
| **PRAFTA-APP-007-2** | backend | app | req/req07 | 신규 | `POST /appApi/req07/sched-modify` — 스케줄 수정 요청 등록 endpoint. REQ_TYPE='10' INSERT. 1/2구간 분기. |
| **PRAFTA-APP-007-3** | backend | app | req/req07 | 신규 | `POST /appApi/req07/attd-correction` — 근태 보정 요청 등록 endpoint. 출퇴근 행 유무 분기 (REQ_TYPE='01' or '02'). 1/2구간. |
| **PRAFTA-APP-007-4** | backend | app | req/req07 | 신규 | `POST /appApi/req07/overtime` — 초과근무 신청 등록 endpoint. REQ_TYPE='03'. 1/2구간. OT_TYPE 검증. |
| **PRAFTA-APP-007-5** | frontend-component | app | req | 신규 | `SlotCard.vue` — 1/2구간 공통 카드. slot-tag + 입력 영역 slot + 삭제 버튼. |
| **PRAFTA-APP-007-6** | frontend-component | app | req | 신규 | `SchedModifyForm.vue` — 스케줄 수정 폼. SCH_CD 선택(BottomSheet) + 1/2구간 + 변경 사유. |
| **PRAFTA-APP-007-7** | frontend-component | app | req | 신규 | `AttdCorrectionForm.vue` — 근태 보정 폼. 출근/퇴근 시각 + 1/2구간 + 보정 사유. |
| **PRAFTA-APP-007-8** | frontend-component | app | req | 신규 | `OvertimeForm.vue` — 초과근무 폼. 시작/종료 시각 + OT_TYPE 선택 + 1/2구간 + 신청 합계 + 신청 사유. |
| **PRAFTA-APP-007-9** | frontend-screen | app | req | 신규 | `AttdRequestView.vue` — 라우트 컨테이너. type 분기, 헤더, 푸터, 폼 컴포넌트 mount, API 호출, sessionStorage 컨텍스트 로드. |
| **PRAFTA-APP-007-10** | frontend-screen | app | router/attd | 보완 | 라우트 `/AttdRequest` 등록 + `MyAttendanceView.onSheetAction` & `onDayDetailAction` 핸들러 교체. |

### 4.2 권장 착수 순서

1. **마이그레이션 작성**: 1 (단독).
2. **백엔드 등록 endpoint**: 2 → 3 → 4 (서로 독립이지만 INSERT 패턴 공유). 순서 무관, 병렬 가능.
3. **프론트 leaf 컴포넌트**: 5 (slot 카드 공통).
4. **프론트 폼 3종**: 6, 7, 8 (서로 독립, 병렬 가능 — slot 카드 의존).
5. **프론트 컨테이너**: 9 (3 폼 통합).
6. **진입 동선**: 10.

### 4.3 상세 설명 (Notion "작업 로그" 의 "상세 설명" 칸 그대로)

#### PRAFTA-APP-007-1 (마이그레이션)

```
[backend / 마이그레이션]

[정책 근거]
- attd/09-requests-approval.md §9.2 (스케줄 수정 요청 — 변경 내용 = 스케줄 자체)
- common/13-ui-ux.md (디자인 토큰 영역과 무관, DB 스키마 변경만)

[핵심 요구사항]
1) SYS032 신규 코드 '10' (스케줄 수정 요청) — tb_syst_val_d INSERT 1행.
2) tb_user_attd_req ADD COLUMN SCH_CD varchar(20) NULL AFTER LEAVE_DAYS — 스케줄 수정 시 변경 목표 SCH_CD.
3) 운영 적용은 사용자 수동 (prafta-037-F5/F6 패턴 동일).
4) 운영 적용 전 부재 확인 쿼리 주석으로 명시.

[영향 받는 파일]
- (신규) prafta-backend/src/main/resources/sql/migration/prafta-app-007-attd-req-extensions.sql

[비범위]
- 운영 DB 직접 실행 금지 (사용자 수동).
- SYS032=10 코드를 사용하는 Java 상수는 PRAFTA-APP-007-2 작업에서 추가.
```

#### PRAFTA-APP-007-2 (스케줄 수정 endpoint)

```
[backend]

[정책 근거]
- attd/09-requests-approval.md §9.2 (스케줄 수정 요청 신청 조건/필수 입력/마감 제한)
- request-approval/06-approval-flows.md §6.1 (차단 사유: 스케줄 마감 후 / 본인 결재 / 다른 관리자 선점)
- attd/06-schedule.md §6.6 (1일 2구간 제한, 겹침 금지)
- common/03-account-auth.md §3.4 (JWT 식별)
- common/08-permissions.md §8.4 (본인 데이터)

[핵심 요구사항]
1) POST /appApi/req07/sched-modify — 본인의 스케줄 수정 요청 등록 (REQ_TYPE='10' 신규).
2) 인증: JWT 의 gv_cmpnyCd / gv_siteCd / gv_userCd 만 사용. 바디의 식별값 무시 (IDOR 가드).
3) 요청 body:
   - workYmd (YYYYMMDD, 8자)
   - nodeCd (varchar20)
   - slots[] (배열, 1 또는 2개):
     - workSeq (int, 1 or 2)
     - schCd (varchar20, 변경 목표 SCH_CD)
   - reqReason (varchar500, 필수, 1자 이상)
4) 검증:
   - workYmd 미래(>오늘) 여부, nodeCd 본인 스케줄 존재 여부 (AppAttd01ServiceImpl.canRequestScheduleModify 동등 정책).
   - 스케줄 마감 여부 (기존 isMonthClosed / 또는 schedule-close 판정 — 정책서 §12).
   - slots.length in [1, 2], workSeq 중복 금지.
   - 중복 요청 차단 (USER_CD + WORK_YMD + REQ_TYPE='10' + REQ_STATUS='01' 행 존재 시 4xx).
5) INSERT (트랜잭션):
   - REQ_ID 채번 (LeaveFlowMapper.selectNextReqId 동일 패턴 — ATTD_REQ_ID 시퀀스).
   - slots 개수만큼 tb_user_attd_req INSERT.
     - REQ_TYPE='10', REQ_STATUS='01', TARGET_ID=null
     - WORK_SEQ=slot.workSeq, SCH_CD=slot.schCd
     - START_DATE/TIME/END_DATE/TIME=null (스케줄 수정은 시각 명세 없음. SCH_CD 만 변경)
     - WORK_YMD/NODE_CD/REQ_REASON 공통
     - INSERT_NO=gv_userCd, INSERT_DATE=NOW(), DEL_YN='N'
6) 응답: { reqId, reqType:'10', reqStatus:'01', workSeqs:[1] or [1,2] }, HTTP 201.

[영향 받는 파일]
- (신규) com.prafta.app.req.req07.controller.AppReq07Controller#registerSchedModify
- (신규) com.prafta.app.req.req07.service.AppReq07Service + Impl#registerSchedModify
- (신규) com.prafta.app.req.req07.mapper.AppReq07Mapper#insertAttdReq + selectNextReqId + countDuplicateReq + selectScheduleByYmd
- (신규) com.prafta.app.req.req07.application.param.SchedModifyParam
- (신규) com.prafta.app.req.req07.application.command.AttdReqInsertCommand
- (신규) com.prafta.app.req.req07.dto.request.SchedModifyRequest + SlotRequest
- (신규) com.prafta.app.req.req07.dto.response.RegisterReqResponse
- (신규) resources/com/prafta/app/req/req07/mapper/AppReq07Mapper.xml

[재사용]
- com.prafta.web.attd.attd07.util.AttdReqTypeUtils (REQ_TYPE/REQ_STATUS 상수 — 단 SYS032=10 신규 상수는 본 패키지 또는 AttdReqTypeUtils 에 추가 필요).
  → 본 작업에서 AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY = "10" 추가 (메모리 `project_prafta_app_007` 노출).
- com.prafta.common.security.JwtUtil, com.prafta.common.dto.TokenInfo.
- com.prafta.app.attd.attd01.mapper.AppAttd01Mapper#selectScheduleByYmd (또는 동등 SELECT) — 본인 스케줄 존재 확인 재사용.
- com.prafta.web.attd.attd07.mapper (마감 판정) — isMonthClosed.

[Endpoint]
POST /appApi/req07/sched-modify
body: { workYmd, nodeCd, slots:[{workSeq, schCd}], reqReason }

[예상 산출물]
controller method / service+impl method / mapper xml(insert + select 3종) / DTO 3종 / Param·Command / 상수 추가.

[비범위]
- 결재선 (tb_user_attd_req_approval) INSERT 미수행 (Q3 — prafta-app-009).
- 알림 발송 미수행 (P4 — outbox follow-up).
- SYS032=10 마이그레이션 적용 (PRAFTA-APP-007-1 별도 작업).
```

#### PRAFTA-APP-007-3 (근태 보정 endpoint)

```
[backend]

[정책 근거]
- attd/11-attendance-correction.md §11.1 (보정 대상 — 누락/중복/순서/3회 이상/시간 휴가 구간/일자 귀속/GPS/5일 경과분/예외)
- attd/11-attendance-correction.md §11.2 (요청 필수 입력 — 대상자/일자/구간/수정 내용/사유/(필요 시) 증빙)
- request-approval/06-approval-flows.md §6.2 (신청 조건: 근태 마감 전)
- attd/06-schedule.md §6.6 (1일 2구간 제한, 겹침 금지)
- common/03-account-auth.md §3.4 (JWT 식별)

[핵심 요구사항]
1) POST /appApi/req07/attd-correction — 본인의 근태 보정 요청 등록.
2) Q2 자동 분기: 서버가 해당 (USER_CD, WORK_YMD, WORK_SEQ) 의 기존 tb_user_attd_mgmt 행 존재 여부로 REQ_TYPE 결정.
   - 존재 → REQ_TYPE='02' (근태 수정), TARGET_ID=기존 ATTD_ID
   - 부재 → REQ_TYPE='01' (근태 생성), TARGET_ID=null
   - 클라가 보낸 reqType 은 무시.
3) 요청 body:
   - workYmd (YYYYMMDD, 8자)
   - nodeCd (varchar20)
   - slots[] (배열, 1 또는 2개):
     - workSeq (int, 1 or 2)
     - startDate (YYYYMMDD — 시각 입력 시 보통 workYmd 와 동일, 자정 넘김 시 +1일)
     - startTime (HHmm, 4자)
     - endDate (YYYYMMDD)
     - endTime (HHmm, 4자)
   - reqReason (varchar500, 필수)
4) 검증:
   - 근태 마감 여부 (정책 §13.3 차단).
   - 본인 권한 (canRequestAttendanceCorrection 동등 — 과거(SCH_CD 존재) OR 오늘(완료)).
   - slot 시각 형식 (HHmm 4자리, 00:00~23:59), 시각 순서 (start < end, 자정 넘김 보정).
   - 2구간 시 겹침 금지 (P9 — slot1.endTime ≤ slot2.startTime).
   - 중복 요청 차단 (P10).
5) INSERT (트랜잭션):
   - REQ_ID 채번 (slot 1·2 공유).
   - slot 별 분기: tb_user_attd_mgmt 행 존재 → REQ_TYPE='02', TARGET_ID=조회된 ATTD_ID. 부재 → '01', TARGET_ID=null.
   - tb_user_attd_req INSERT × slots.length.
   - REQ_STATUS='01', 공통 컬럼 동일.
6) 응답: { reqId, reqType: '02' or '01' or 'MIXED', reqStatus:'01', workSeqs:[1] or [1,2] }.
   - 2구간 중 slot 별로 REQ_TYPE 다른 케이스 (1구간 행 존재 + 2구간 행 부재) → 응답 reqType='MIXED' (클라 표시용).

[영향 받는 파일]
- (신규) AppReq07Controller#registerAttdCorrection
- (신규) AppReq07Service + Impl#registerAttdCorrection
- (신규) AppReq07Mapper#selectExistingAttdRow (cmpnyCd, siteCd, userCd, workYmd, workSeq → ATTD_ID 또는 null) + insertAttdReq + countDuplicate + 마감 가드
- (신규) AttdCorrectionParam, AttdCorrectionRequest, SlotRequest 재사용
- AppReq07Mapper.xml: 위 SELECT/INSERT 추가

[재사용]
- AttdReqTypeUtils (REQ_TYPE 01/02 기존 상수)
- 기존 tb_user_attd_mgmt 행 조회 매퍼가 있다면 재사용 (없으면 본 패키지 신규 SELECT)
- 마감 판정 util

[Endpoint]
POST /appApi/req07/attd-correction
body: { workYmd, nodeCd, slots:[{workSeq, startDate, startTime, endDate, endTime}], reqReason }

[예상 산출물]
controller method / service method / mapper xml(분기 SELECT + insert) / DTO / Param.

[비범위]
- 결재선 INSERT (Q3)
- 알림 발송 (P4)
- 증빙 첨부 (정책 §11.2 "필요 시" — 본 작업 1차 미구현; follow-up)
```

#### PRAFTA-APP-007-4 (초과근무 endpoint)

```
[backend]

[정책 근거]
- attd/09-requests-approval.md §9.3 (초과근무 상신 — 사전/사후 모두 허용)
- request-approval/03-policy-alignment.md §3.2 (사후 상신 기한 — 사업장 근태 마감 전까지. D+5 폐기)
- request-approval/06-approval-flows.md §6.3 (발생 케이스 — 조기 출근/연장/휴일/구간 수 초과)
- attd/10-attendance-calc.md §10.3 (추가근무 인정 및 계산 — 본 작업은 인정값 계산 안 함, 시각만 저장)
- common/03-account-auth.md §3.4

[핵심 요구사항]
1) POST /appApi/req07/overtime — 본인의 초과근무 신청 등록 (사전·사후 모두 허용).
2) REQ_TYPE='03' (초과근무 생성 요청) 고정. 수정 요청은 본 작업 미포함 (원본 요청서 명시).
3) 요청 body:
   - workYmd (YYYYMMDD)
   - nodeCd (varchar20)
   - slots[] (배열, 1 또는 2개):
     - workSeq (int, 1 or 2)
     - startDate (YYYYMMDD)
     - startTime (HHmm)
     - endDate (YYYYMMDD)
     - endTime (HHmm)
     - otType (varchar10, EXTEND | NIGHT | HOLIDAY — P12 필수)
   - reqReason (varchar500, 필수)
4) 검증:
   - 근태 마감 여부 (사후 상신 기한 = 마감 전까지). 마감 후 fail-closed.
   - 본인 권한 (canRequestOvertime 동등 — 과거 OR 오늘 + 완료).
   - slot 시각 형식 / 순서 / 자정 넘김 보정.
   - otType allow-list (EXTEND/NIGHT/HOLIDAY).
   - 2구간 겹침 금지 (P9).
   - 중복 요청 차단 (P10).
5) INSERT:
   - REQ_ID 채번.
   - slot 별 tb_user_attd_req INSERT.
     - REQ_TYPE='03', REQ_STATUS='01', TARGET_ID=null.
     - WORK_SEQ, START_DATE/TIME, END_DATE/TIME, OT_TYPE 채움.
6) 응답: { reqId, reqType:'03', reqStatus:'01', workSeqs:[1] or [1,2] }, HTTP 201.

[영향 받는 파일]
- (신규) AppReq07Controller#registerOvertime
- (신규) AppReq07Service + Impl#registerOvertime
- (신규) AppReq07Mapper#insertAttdReq (재사용) + selectAttdCompletion (canRequestOvertime 가드용)
- (신규) OvertimeParam, OvertimeRequest, SlotRequest

[재사용]
- AttdReqTypeUtils.REQ_TYPE_OT_REGISTER ('03')
- 마감 판정 util

[Endpoint]
POST /appApi/req07/overtime
body: { workYmd, nodeCd, slots:[{workSeq, startDate, startTime, endDate, endTime, otType}], reqReason }

[예상 산출물]
controller method / service method / mapper / DTO / Param.

[비범위]
- 초과근무 수정 ('04') 폼은 본 작업 외 (원본 요청서 명시).
- 결재선 INSERT (Q3).
- 알림 발송 (P4).
- 면제 시간/표준화 단위 계산 (정책 §10.2~§10.3) — 신청값만 저장, 관리자 처리 시 계산.
```

#### PRAFTA-APP-007-5 (SlotCard)

```
[frontend-component]

[정책 근거]
- common/13-ui-ux.md §13.2 (모바일 터치 영역 44px)
- common/13-ui-ux.md §13.3 (인터랙션 — 삭제 액션 명확)

[핵심 요구사항]
1) 1구간/2구간 공통 카드. props: workSeq (1 or 2), removable (boolean), title (string, "1구간" 등).
2) emits: remove (workSeq).
3) 구조:
   - 상단 header: slot-tag (배경 #f3f4f6, 12px 라운드, 패딩 4/10) + (removable=true 시) 삭제 버튼 (휴지통 SVG, 24×24)
   - 본체: default slot — 폼 입력 영역
4) 삭제 버튼 클릭 → emit('remove', workSeq).
5) 인라인 SVG (휴지통) — CDN 의존 금지.

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/SlotCard.vue
```

#### PRAFTA-APP-007-6 (SchedModifyForm)

```
[frontend-component]

[정책 근거]
- attd/09-requests-approval.md §9.2 (스케줄 수정 요청 필수 입력 — 변경 내용/사유)
- attd/06-schedule.md §6.6 (1일 2구간 제한)

[핵심 요구사항]
1) props: context (day 객체 — workYmd, nodeCd, scheduleSummary, workPlanName, slots[]).
2) emits: submit ({ slots:[{workSeq, schCd}], reqReason }), cancel.
3) 구조:
   - 컨텍스트 박스: 날짜 + 사업장 + 현재 스케줄 (context.workPlanName + context.scheduleSummary)
   - "변경할 스케줄" 섹션
   - slots[] 렌더링 (1구간 카드 → SlotCard + SCH_CD 선택 트리거)
   - SCH_CD 선택 트리거 클릭 → BaseBottomSheet 열기 (SCH_CD 옵션 리스트, 1차는 placeholder text "스케줄을 선택해 주세요" — SCH_CD 목록 endpoint 는 후속 보강. 본 작업은 native <select> 또는 단순 텍스트 입력으로 대체 — TODO(developer) 마커)
   - "구간 추가" 버튼 (slots.length === 1 일 때만)
   - 사유 textarea (100자 카운터)
   - 헬퍼 메시지 "요청은 관리자 승인 후 반영돼요. 스케줄 마감 전까지 신청해 주세요."
4) 검증:
   - 모든 slot 의 schCd 비어있지 않음.
   - reqReason 1자 이상.
5) 검증 실패 시 alert. 성공 시 emit submit.

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/SchedModifyForm.vue

[비범위]
- SCH_CD 목록 조회 endpoint 신설 (1차는 입력 placeholder. follow-up).
```

#### PRAFTA-APP-007-7 (AttdCorrectionForm)

```
[frontend-component]

[정책 근거]
- attd/11-attendance-correction.md §11.1, §11.2 (보정 요청 필수 입력 — 대상자/일자/구간/수정 내용/사유)
- attd/06-schedule.md §6.6 (2구간 겹침 금지 — 클라 경고만, 서버 강제)

[핵심 요구사항]
1) props: context (day 객체).
2) emits: submit ({ slots:[{workSeq, startDate, startTime, endDate, endTime}], reqReason }), cancel.
3) 구조:
   - 컨텍스트 박스: 날짜 + 사업장 + 스케줄 + 현재 근태 (지각/조퇴 시 warn 색상)
   - "보정할 시간" 섹션
   - slots[] (1구간 카드 → SlotCard + 출근 input-dt + 퇴근 input-dt)
   - input-dt: date input + time input 2개 (P15 — HH:MM 표시, HHmm 변환은 emit 시점)
   - 초기값: context.slots[*].attendance.startTime/endTime (시안 §2-1 — 보정 시 현재값 프리필)
   - "구간 추가" 버튼 (1구간 only 시)
   - 사유 textarea (100자 카운터)
   - 헬퍼 "관리자 승인 후 근태에 반영돼요. 근태 마감 전까지 신청해 주세요. 원본 출퇴근 기록은 보존돼요."
4) 검증:
   - 모든 slot 의 startTime/endTime 형식 HH:MM.
   - start < end (자정 넘김 시 endDate +1일 권장 알림).
   - 2구간 시 slot1.endTime ≤ slot2.startTime (위반 시 인라인 경고 — 차단 안 함, 서버 강제).
   - reqReason 1자 이상.

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/AttdCorrectionForm.vue
```

#### PRAFTA-APP-007-8 (OvertimeForm)

```
[frontend-component]

[정책 근거]
- attd/09-requests-approval.md §9.3 (초과근무 상신 — 시작/종료 시간 + 사유)
- request-approval/06-approval-flows.md §6.3 (발생 케이스 — 시안 §3-1 "스케줄 종료 후, 근태 퇴근 사이의 18:00 ~ 19:38 까지 신청할 수 있어요" 는 1차 미반영, follow-up)
- attd/10-attendance-calc.md §10.3 (인정값 계산은 관리자 처리 시점)

[핵심 요구사항]
1) props: context (day 객체).
2) emits: submit ({ slots:[{workSeq, startDate, startTime, endDate, endTime, otType}], reqReason }), cancel.
3) 구조:
   - 컨텍스트 박스: 날짜 + 사업장 + 스케줄 + 근태
   - "초과근무 시간" 섹션
   - slots[] (1구간 카드 → SlotCard + 시작 input-dt + 종료 input-dt + OT_TYPE 트리거)
   - OT_TYPE 트리거 클릭 → BaseBottomSheet 단일 선택 (EXTEND/NIGHT/HOLIDAY, P12)
   - "구간 추가" 버튼 (1구간 only 시)
   - 신청 합계 박스 (primary-tint 배경) — slots[] 합산 분 자동 계산 표시 (예: "1시간 30분")
   - 사유 textarea (100자 카운터)
   - 헬퍼 "관리자 승인 후 추가근무로 반영돼요. 근태 마감 전까지 신청해 주세요."
4) 검증:
   - 모든 slot 의 시각 형식, 순서, otType 필수.
   - 2구간 겹침 (클라 인라인 경고).
   - reqReason 1자 이상.

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/OvertimeForm.vue

[비범위]
- 시안 §3-1 "신청 가능 범위 안내" (스케줄 종료~근태 퇴근 사이) 자동 계산 — follow-up.
- OT_TYPE 자동 추정 — follow-up.
```

#### PRAFTA-APP-007-9 (AttdRequestView)

```
[frontend-screen]

[정책 근거]
- common/13-ui-ux.md §13.2~§13.3 (피드백 / 인터랙션)
- common/03-account-auth.md §3.4 (JWT 토큰 — axios interceptor 가 자동 부착)

[핵심 요구사항]
1) 라우트: /AttdRequest?type=schedModify|attdCorrection|overtime&workYmd=YYYYMMDD&nodeCd=N001 (비-public).
2) type / workYmd 둘 다 없으면 alert 후 router.back. type allow-list 외 값도 거부.
3) 헤더 (56px): 백 버튼 + 타이틀 (type 별 — "스케줄 수정 요청" / "근태 보정 요청" / "초과근무 신청").
4) 본문: type 별 폼 컴포넌트 mount (SchedModifyForm | AttdCorrectionForm | OvertimeForm).
5) 컨텍스트 로드:
   - sessionStorage['attd_req_ctx_v1'] 에서 day 객체 JSON 파싱 (workYmd 일치 확인).
   - 일치 시 폼 props.context 로 전달 + sessionStorage 즉시 제거 (stale 방지).
   - 불일치/부재 시 라우트 진입 거부 (alert + router.back).
6) 푸터 (sticky bottom, 두 버튼):
   - 취소 버튼 (회색 보더) → router.back().
   - 요청하기 버튼 (primary) → 폼의 submit emit 후 API 호출.
7) API 호출:
   - type 별 endpoint:
     - schedModify → POST /appApi/req07/sched-modify
     - attdCorrection → POST /appApi/req07/attd-correction
     - overtime → POST /appApi/req07/overtime
   - body: { workYmd, nodeCd, slots[], reqReason }
   - 성공 (201): showAlert('요청이 등록되었습니다') + router.back().
   - 실패: resolveApiErrorMessage 한국어 alert (네트워크/4xx/5xx).
8) 로딩 상태: 요청하기 버튼 disabled + 라벨 "등록 중...".

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/AttdRequestView.vue

[디자인 토큰]
.attd-req-view 루트에 MyAttendanceView 와 동일 팔레트 1회 선언.

[비범위]
- 결재선 영역 (Q3, prafta-app-009)
- 등록 후 상세 화면 이동 (현재는 router.back; prafta-app-006 F1 의존)
```

#### PRAFTA-APP-007-10 (라우트 + 진입 동선)

```
[frontend-screen / 보완]

[핵심 요구사항]
1) router/index.js routes[] 에 다음 추가:
   {
     path: '/AttdRequest',
     name: 'AttdRequest',
     component: () => import('@/views/req/AttdRequestView.vue'),
   }
   publicPaths 미추가.
2) MyAttendanceView.vue 의 onSheetAction 본문 교체:
   - before: showAlert('준비 중입니다')
   - after: type 분기 → sessionStorage 저장 → router.push('/AttdRequest?type=...&workYmd=...&nodeCd=...').
     - scheduleModify → type=schedModify
     - attendanceCorrection → type=attdCorrection
     - overtime → type=overtime
     - leave → showAlert('준비 중입니다') 유지 (본 작업 외).
3) MyAttendanceView.vue 의 onDayDetailAction 본문 교체:
   - payload.type === 'attendanceCorrection' → type=attdCorrection 라우팅
   - payload.type === 'overtime' → type=overtime 라우팅
4) MyAttendanceView.vue 의 onTodayAction (오늘 카드의 requestModify 분기) 는 stub 유지 (별도 follow-up).
5) sessionStorage 저장: { workYmd, nodeCd, scheduleSummary, workPlanName, attendanceSummary, slots: day.slots } (slots 는 출퇴근 시각 프리필용).

[영향 받는 파일]
- prafta-app-frontend/src/router/index.js
- prafta-app-frontend/src/views/attd/MyAttendanceView.vue

[비범위]
- AttendanceDayDetailCard 내부 직접 변경 (현재 emit 으로 처리 — MyAttendanceView 의 onDayDetailAction 만 교체).
```

---

## 5. 의존성 그래프

```
PRAFTA-APP-007-1 (마이그레이션 SQL)
    └─ (운영 적용 후 PRAFTA-APP-007-2 의 SYS032=10 INSERT 가 의미를 가짐)
         ↓
PRAFTA-APP-007-2 (BE 스케줄수정)
PRAFTA-APP-007-3 (BE 근태보정)
PRAFTA-APP-007-4 (BE 초과근무)
    │ (서로 독립, 병렬 가능. 1번 마이그 후 2번만 강결합. 3·4는 마이그 무관)
    │
    │ (응답 스키마 확정)
    ↓
PRAFTA-APP-007-5 (FE SlotCard) ───┐
                                   ├─ PRAFTA-APP-007-6 (SchedModifyForm)
                                   ├─ PRAFTA-APP-007-7 (AttdCorrectionForm)
                                   └─ PRAFTA-APP-007-8 (OvertimeForm)

         ↓ (전부 완료 후)
PRAFTA-APP-007-9 (AttdRequestView) ── 통합 (3 폼 + 3 BE 호출)
         ↓
PRAFTA-APP-007-10 (라우트 + 진입 동선)
```

병렬: BE 2·3·4 와 FE 5·6·7·8 동시 진행 가능 (응답 스키마는 §4.3 각 작업의 4) 응답 정의로 고정).

---

## 6. 비기능 요구사항

### 6.1 보안

- **JWT 기반 식별 강제**: `Param.from(TokenInfo)` 패턴. 바디의 cmpnyCd·siteCd·userCd 절대 사용 금지 (IDOR 가드).
- **REQ_TYPE allow-list**: 각 endpoint 별 강제. `01/02` (보정), `03` (OT), `10` (스케줄수정). 그 외 fail-closed.
- **시스템 REQ_TYPE 07/08/09 차단**: 사용자 endpoint 에서 받지 않음.
- **NODE_CD 검증**: 본인의 해당 일자 스케줄/근태의 NODE_CD 와 일치 확인 (P20).
- **마감 가드**: 스케줄 마감 (스케줄 수정) / 근태 마감 (보정·OT) — fail-closed.
- **중복 요청 차단**: P10 — 동일 (USER_CD, WORK_YMD, REQ_TYPE) + REQ_STATUS='01' 행 존재 시 4xx.
- **감사 로그**: 본인 요청 INSERT 는 단순 등록 — 감사 미대상 (`common/11-security-privacy.md` §11.3 — 다운로드/PII 접근/중요 삭제만).
- **PII**: 시각·SCH_CD·사유만 입력. PII 없음.

### 6.2 SQL

- 명시 컬럼 나열, `SELECT *` 금지.
- `#{}` 바인딩 (`${}` 금지).
- Leading comma.
- `IDX_ATTD_REQ_USER`, `IDX_ATTD_REQ_WORK_YMD` 활용.
- INSERT 시 `INSERT_NO = #{userCd}`, `INSERT_DATE = NOW()`, `DEL_YN = 'N'`.

### 6.3 프론트엔드

- **TypeScript 금지** (`<script setup>` JS).
- **scoped style 필수**.
- **CSS 변수만** 사용. 신규 색상 도입 금지 (P18).
- **!important 금지**.
- **인라인 SVG sprite** (P19 — CDN 금지).
- **터치 영역 최소 44×44** (`common/13-ui-ux.md` §13.2).
- **반응형**: 360~414px 기준 폭 (앱 webview).
- **한국어 텍스트** (사용자 노출 문구 모두).
- **공통 컴포넌트 우선 사용**: BaseBottomSheet (prafta-app-006 신설) 재사용. 신규 BottomSheet 만들지 말 것.

### 6.4 트랜잭션 / 정합성

- 2구간 INSERT 는 단일 트랜잭션 (Spring `@Transactional`).
- REQ_ID 채번 후 1·2구간 동일 REQ_ID 로 INSERT.
- 트랜잭션 내 중복 체크 + INSERT — race condition 시 `IDX_ATTD_REQ_USER` 인덱스로 보호 (필요 시 PRIMARY KEY 충돌로 4xx 변환).

### 6.5 에러 처리

- 401/403: axios 인터셉터가 강제 로그아웃 (기존 MainView 패턴).
- 400/422: 서버 한국어 메시지 그대로 alert.
- 5xx / 네트워크: 한국어 폴백 alert.
- 중복 요청 (P10): 한국어 메시지 "이미 등록된 미처리 요청이 있습니다. 기존 요청을 확인해 주세요."

### 6.6 성능

- INSERT 2건 최대 (1+2구간). 부하 무시 가능.
- 컨텍스트 조회 endpoint 없음 (sessionStorage 재사용, P6/P7).

---

## 7. Follow-up 후보

| # | 항목 | 이유 |
|---|---|---|
| **F-A** | **결재선 통합 (시안 §5 + prafta-020 USER_04 프리셋 연동)** → 별도 산출물 `.claude/requests/app_requests/prafta-app-009.md` | Q3 — 사용자 명시 분리. |
| F1 | 알림 발송 (등록 직후 관리자 push outbox INSERT) | `prafta-031` outbox consumer 구현 후 연결 (P4). |
| F2 | SCH_CD 목록 조회 endpoint (스케줄 수정 폼에서 선택지 노출) | 본 작업 1차는 placeholder. SCH_CD 목록 endpoint 신설 필요. |
| F3 | 시안 §3-1 "신청 가능 범위 안내" (스케줄 종료~근태 퇴근 사이 자동 계산) | 면제 시간 / 표준화 단위 계산 (`attd §10.2~§10.3`). |
| F4 | OT_TYPE 자동 추정 (스케줄 종료 후 → EXTEND 등) | 1차는 사용자 선택. |
| F5 | 증빙 첨부 (근태 보정 — 정책 §11.2 "필요 시") | 파일 업로드 인프라 필요. |
| F6 | 2구간 겹침 클라 차단 (현재는 인라인 경고만) | UX 강화. |
| F7 | 등록 후 상세 화면 이동 (현재 router.back) | prafta-app-006 F1 (요청 상세) 완료 후. |
| F8 | 사후 상신 기한 임박 배지 ("D-N영업일") | `request-approval/03-policy-alignment.md` §3.2 — 관리자 화면 영역. 클라 표시 가능. |
| F9 | onTodayAction 의 requestModify (오늘 카드의 근태 수정 액션) 연동 | MyAttendanceView 의 stub 1개 남음. |
| F10 | 일자 셀렉터 컴포넌트 (`<input type="date">` 대체) | prafta-app-006 F8 동일. |
| F11 | 자정 넘김 (endDate = workYmd +1) 자동 보정 UX | 현재는 클라가 직접 endDate 입력. |

---

## 8. Vue 컴포넌트 골격 (작성·디스크 등록은 사용자 승인 후)

> **모든 골격은 template + style 만 완성. script 영역은 `// TODO(developer):` 마커 + 반응형 변수 선언만.**
> **CSS 변수만 사용. native HTML 태그는 input/button/textarea 등 필수 입력 요소만 사용 (앱 프론트는 공통 폼 컴포넌트가 부분적).**

### 8.1 `prafta-app-frontend/src/views/req/AttdRequestView.vue`

```vue
<!--
  AttdRequestView.vue — 근태 요청 폼 라우트 컨테이너 (모바일 앱)
  - 작업 ID: PRAFTA-APP-007-9 (분해: .claude/requests/app_requests/prafta-app-007-plan.md)
  - 라우트: /AttdRequest?type=schedModify|attdCorrection|overtime&workYmd=YYYYMMDD&nodeCd=N001
  - planner 라운드 스코프: 헤더/푸터/폼 mount/sessionStorage 컨텍스트 로드 (template/style 완성)
  - developer 라운드 스코프(아래 TODO): 실 API 호출, 에러 처리, 라우팅 후속
  - 디자인 토큰: MyAttendanceView(.my-attd-view) 와 동일 세트를 .attd-req-view 루트에 1회 선언.
-->
<template>
  <div class="attd-req-view">
    <!-- 헤더 -->
    <header class="req-hd">
      <button type="button" class="req-hd__back" aria-label="뒤로" @click="onCancel">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-req-chev-left" />
        </svg>
      </button>
      <h1 class="req-hd__title">{{ headerTitle }}</h1>
      <span class="req-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 (스크롤 영역, 폼 컴포넌트 분기) -->
    <main class="req-body">
      <SchedModifyForm
        v-if="formType === 'schedModify' && context"
        :context="context"
        @submit="onSubmit"
        @cancel="onCancel"
      />
      <AttdCorrectionForm
        v-else-if="formType === 'attdCorrection' && context"
        :context="context"
        @submit="onSubmit"
        @cancel="onCancel"
      />
      <OvertimeForm
        v-else-if="formType === 'overtime' && context"
        :context="context"
        @submit="onSubmit"
        @cancel="onCancel"
      />

      <!-- 컨텍스트 누락 폴백 -->
      <div v-else class="req-fallback">
        <p>요청 화면을 열 수 없습니다. 근태 화면으로 돌아가 주세요.</p>
      </div>
    </main>

    <!-- 인라인 SVG sprite -->
    <svg width="0" height="0" class="req-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-req-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import SchedModifyForm from './components/SchedModifyForm.vue'
import AttdCorrectionForm from './components/AttdCorrectionForm.vue'
import OvertimeForm from './components/OvertimeForm.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 폼 타입 (쿼리 파라미터)
const ALLOWED_TYPES = ['schedModify', 'attdCorrection', 'overtime']
const formType = computed(() => {
  const t = String(route.query.type || '')
  return ALLOWED_TYPES.includes(t) ? t : ''
})

const HEADER_TITLES = {
  schedModify: '스케줄 수정 요청',
  attdCorrection: '근태 보정 요청',
  overtime: '초과근무 신청',
}
const headerTitle = computed(() => HEADER_TITLES[formType.value] || '근태 요청')

// 컨텍스트 (sessionStorage 로 이전 화면에서 전달)
const context = ref(null)
const isSubmitting = ref(false)

const onCancel = () => {
  router.back()
}

const onSubmit = (payload) => {
  // payload: { slots:[...], reqReason, otType?:string }  (폼별로 다름)
  isSubmitting.value = true
  // TODO(developer): 폼 타입별 endpoint 호출
  //   schedModify     → POST /appApi/req07/sched-modify
  //   attdCorrection  → POST /appApi/req07/attd-correction
  //   overtime        → POST /appApi/req07/overtime
  // body: { workYmd, nodeCd, slots, reqReason }
  // 성공: showAlert('요청이 등록되었습니다') → router.back()
  // 실패: showAlert(resolveApiErrorMessage(e))
  isSubmitting.value = false
}

const CONTEXT_KEY = 'attd_req_ctx_v1'

onMounted(() => {
  // 1) type / workYmd 유효성
  if (!formType.value) {
    showAlert('잘못된 요청 화면입니다.')
    router.back()
    return
  }
  const workYmd = String(route.query.workYmd || '')
  if (!/^\d{8}$/.test(workYmd)) {
    showAlert('대상 일자가 없습니다.')
    router.back()
    return
  }

  // 2) sessionStorage 에서 컨텍스트 로드 + 즉시 제거 (P7 stale 방지)
  try {
    const raw = sessionStorage.getItem(CONTEXT_KEY)
    if (!raw) {
      showAlert('컨텍스트가 만료되었습니다. 근태 화면에서 다시 시도해 주세요.')
      router.back()
      return
    }
    const parsed = JSON.parse(raw)
    sessionStorage.removeItem(CONTEXT_KEY)
    if (parsed.workYmd !== workYmd) {
      showAlert('컨텍스트가 일치하지 않습니다.')
      router.back()
      return
    }
    context.value = parsed
  } catch (e) {
    showAlert('컨텍스트를 불러오지 못했습니다.')
    router.back()
  }
})
</script>

<style scoped>
.attd-req-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-primary-text-deep: #15803d;
  --color-primary-text-darkest: #14532d;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100vh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.req-hd {
  height: 56px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
}
.req-hd__back {
  width: 44px;
  height: 44px;
  background: transparent;
  border: 0;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.req-hd__title {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.req-hd__spacer {
  width: 44px;
  height: 44px;
}

/* 본문 */
.req-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.req-fallback {
  padding: 40px 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.icon {
  display: block;
}
</style>
```

### 8.2 `prafta-app-frontend/src/views/req/components/SlotCard.vue`

```vue
<!--
  SlotCard.vue — 1구간/2구간 공통 카드 (3 폼 공유)
  - 작업 ID: PRAFTA-APP-007-5
-->
<template>
  <div class="slot-card">
    <div class="slot-card__head">
      <span class="slot-card__tag">{{ title }}</span>
      <button
        v-if="removable"
        type="button"
        class="slot-card__del"
        aria-label="구간 삭제"
        @click="$emit('remove', workSeq)"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M3 6h18" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
          <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          <line x1="10" y1="11" x2="10" y2="17" />
          <line x1="14" y1="11" x2="14" y2="17" />
        </svg>
      </button>
    </div>
    <div class="slot-card__body">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  workSeq: { type: Number, required: true },
  title: { type: String, default: '구간' },
  removable: { type: Boolean, default: false },
})
defineEmits(['remove'])
</script>

<style scoped>
.slot-card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.slot-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.slot-card__tag {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}

.slot-card__del {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.slot-card__del:hover {
  color: var(--color-danger);
}

.slot-card__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
</style>
```

### 8.3 `prafta-app-frontend/src/views/req/components/SchedModifyForm.vue`

```vue
<!--
  SchedModifyForm.vue — 스케줄 수정 요청 폼
  - 작업 ID: PRAFTA-APP-007-6
  - props.context: day 객체 { workYmd, nodeCd, scheduleSummary, workPlanName, slots[] }
  - emits: submit ({ slots:[{workSeq, schCd}], reqReason }), cancel
-->
<template>
  <form class="sched-form" @submit.prevent="onSubmit">
    <!-- 컨텍스트 박스 -->
    <section class="ctx">
      <p class="ctx__date">
        <strong>{{ ctxDateDisplay }}</strong>
        <small>{{ ctxSiteDisplay }}</small>
      </p>
      <div class="ctx__row">
        <span class="ctx__lbl">현재 스케줄</span>
        <span class="ctx__val">{{ context.workPlanName || '-' }}</span>
      </div>
      <div v-if="context.scheduleSummary" class="ctx__row">
        <span class="ctx__lbl"></span>
        <span class="ctx__val ctx__val--muted">{{ context.scheduleSummary }}</span>
      </div>
    </section>

    <!-- 변경할 스케줄 -->
    <section class="fs">
      <p class="fs__title">변경할 스케줄</p>

      <SlotCard
        v-for="(slot, idx) in slots"
        :key="slot.workSeq"
        :work-seq="slot.workSeq"
        :title="slot.workSeq + '구간'"
        :removable="slots.length > 1"
        @remove="onRemoveSlot"
      >
        <label class="field">
          <span class="field__label"><span class="req">*</span>근무 타입</span>
          <!-- TODO(developer): SCH_CD 목록 endpoint 도입 시 BaseBottomSheet 로 교체 -->
          <input
            v-model="slot.schCd"
            class="field__input"
            type="text"
            placeholder="스케줄 코드를 입력해 주세요"
            maxlength="20"
          />
        </label>
      </SlotCard>

      <button
        v-if="slots.length === 1"
        type="button"
        class="btn-add"
        @click="onAddSlot"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        구간 추가
      </button>

      <label class="field">
        <span class="field__label">
          <span class="req">*</span>변경 사유
          <span class="field__help">{{ reqReason.length }}/100</span>
        </span>
        <textarea
          v-model="reqReason"
          class="field__textarea"
          placeholder="사유를 입력해 주세요."
          maxlength="100"
          rows="4"
        ></textarea>
      </label>
    </section>

    <!-- 헬퍼 메시지 -->
    <p class="helper">
      <span class="helper__dot" aria-hidden="true">·</span>
      요청은 관리자 승인 후 반영돼요. 스케줄 마감 전까지 신청해 주세요.
    </p>

    <!-- 푸터 -->
    <footer class="form-ft">
      <button type="button" class="btn btn--x" @click="$emit('cancel')">취소</button>
      <button type="submit" class="btn btn--p" :disabled="!isValid">요청하기</button>
    </footer>
  </form>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import SlotCard from './SlotCard.vue'

const props = defineProps({
  context: { type: Object, required: true },
})
const emit = defineEmits(['submit', 'cancel'])

const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (m) => (proxy?.$alert ? proxy.$alert(m) : window.alert(m))

// 1구간 기본 (사용자가 "구간 추가" 누르면 2구간 추가)
const slots = ref([{ workSeq: 1, schCd: '' }])
const reqReason = ref('')

// 컨텍스트 표시 (workYmd → "YYYY년 M월 D일" + 요일)
// TODO(developer): 한국 요일·날짜 포맷 유틸 사용 (기존 attdFormat)
const ctxDateDisplay = computed(() => {
  const y = props.context.workYmd?.slice(0, 4)
  const m = props.context.workYmd?.slice(4, 6)
  const d = props.context.workYmd?.slice(6, 8)
  return y && m && d ? `${y}년 ${Number(m)}월 ${Number(d)}일` : '-'
})
const ctxSiteDisplay = computed(() => props.context.siteName || '')

const isValid = computed(() => {
  if (!reqReason.value.trim()) return false
  return slots.value.every((s) => s.schCd.trim())
})

const onAddSlot = () => {
  if (slots.value.length >= 2) return
  // workSeq 는 구간 식별자(1/2)이므로 비어 있는 번호를 채운다. (위치 기반 재인덱싱 금지)
  const existing = new Set(slots.value.map((s) => s.workSeq))
  const missing = [1, 2].find((n) => !existing.has(n))
  if (!missing) return
  slots.value.push({ workSeq: missing, schCd: '' })
  slots.value.sort((a, b) => a.workSeq - b.workSeq)
}

// 구간 삭제 — workSeq 는 구간 식별자이므로 남은 구간 번호를 재인덱싱하지 않는다.
const onRemoveSlot = (workSeq) => {
  slots.value = slots.value.filter((s) => s.workSeq !== workSeq)
}

const onSubmit = () => {
  if (!isValid.value) {
    showAlert('모든 필수 항목을 입력해 주세요.')
    return
  }
  emit('submit', {
    slots: slots.value.map((s) => ({ workSeq: s.workSeq, schCd: s.schCd.trim() })),
    reqReason: reqReason.value.trim(),
  })
}
</script>

<style scoped>
.sched-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 컨텍스트 박스 */
.ctx {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.ctx__date {
  margin: 0 0 var(--space-xs);
  display: flex;
  flex-direction: column;
}
.ctx__date strong {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.ctx__date small {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ctx__row {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: var(--space-sm);
  align-items: baseline;
}
.ctx__lbl {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ctx__val {
  font-size: 13px;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.ctx__val--muted {
  color: var(--color-text-tertiary);
}

/* 폼 섹션 */
.fs {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.fs__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 필드 */
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field__label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.field__label .req {
  color: var(--color-danger);
}
.field__help {
  margin-left: auto;
  font-size: 11px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}
.field__input,
.field__textarea {
  width: 100%;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: inherit;
  box-sizing: border-box;
}
.field__textarea {
  resize: vertical;
  min-height: 96px;
}
.field__input:focus,
.field__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 구간 추가 버튼 */
.btn-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 40px;
  background: var(--color-surface);
  border: 0.5px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.btn-add:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* 헬퍼 */
.helper {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-warning-tint);
  border: 0.5px solid var(--color-warning);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--color-warning-text);
  display: flex;
  gap: var(--space-xs);
}
.helper__dot {
  color: var(--color-warning);
}

/* 푸터 */
.form-ft {
  position: sticky;
  bottom: 0;
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: var(--space-sm);
  padding: var(--space-sm) 0 calc(var(--space-sm) + env(safe-area-inset-bottom));
  background: var(--color-bg);
  border-top: 0.5px solid var(--color-border);
  margin: 0 calc(-1 * var(--space-lg));
  padding-left: var(--space-lg);
  padding-right: var(--space-lg);
}

.btn {
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.btn--x {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  color: var(--color-text-secondary);
}
.btn--p {
  background: var(--color-primary);
  border: 0;
  color: var(--color-surface);
}
.btn--p:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
</style>
```

### 8.4 `prafta-app-frontend/src/views/req/components/AttdCorrectionForm.vue`

```vue
<!--
  AttdCorrectionForm.vue — 근태 보정 요청 폼
  - 작업 ID: PRAFTA-APP-007-7
  - emits: submit ({ slots:[{workSeq, startDate, startTime, endDate, endTime}], reqReason }), cancel
-->
<template>
  <form class="attd-form" @submit.prevent="onSubmit">
    <!-- 컨텍스트 박스 -->
    <section class="ctx">
      <p class="ctx__date">
        <strong>{{ ctxDateDisplay }}</strong>
        <small>{{ ctxSiteDisplay }}</small>
      </p>
      <div class="ctx__row">
        <span class="ctx__lbl">스케줄</span>
        <span class="ctx__val">{{ context.workPlanName }} · {{ context.scheduleSummary || '-' }}</span>
      </div>
      <div v-if="context.attendanceSummary" class="ctx__row">
        <span class="ctx__lbl">현재 근태</span>
        <span class="ctx__val" :class="{ 'ctx__val--warn': hasIssue }">
          {{ context.attendanceSummary }}{{ hasIssue ? ' (확인 필요)' : '' }}
        </span>
      </div>
    </section>

    <!-- 보정할 시간 -->
    <section class="fs">
      <p class="fs__title">보정할 시간</p>

      <SlotCard
        v-for="slot in slots"
        :key="slot.workSeq"
        :work-seq="slot.workSeq"
        :title="slot.workSeq + '구간'"
        :removable="slots.length > 1"
        @remove="onRemoveSlot"
      >
        <label class="field">
          <span class="field__label"><span class="req">*</span>출근</span>
          <div class="input-dt">
            <input
              v-model="slot.startDate"
              type="date"
              class="field__input field__input--date"
            />
            <input
              v-model="slot.startTime"
              type="time"
              class="field__input field__input--time"
            />
          </div>
        </label>
        <label class="field">
          <span class="field__label"><span class="req">*</span>퇴근</span>
          <div class="input-dt">
            <input
              v-model="slot.endDate"
              type="date"
              class="field__input field__input--date"
            />
            <input
              v-model="slot.endTime"
              type="time"
              class="field__input field__input--time"
            />
          </div>
        </label>
      </SlotCard>

      <button
        v-if="slots.length === 1"
        type="button"
        class="btn-add"
        @click="onAddSlot"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        구간 추가
      </button>

      <!-- 2구간 겹침 인라인 경고 (P9, 차단 안 함) -->
      <p v-if="overlapWarning" class="warn-msg">
        2구간 시작 시각은 1구간 종료 시각 이후여야 합니다.
      </p>

      <label class="field">
        <span class="field__label">
          <span class="req">*</span>보정 사유
          <span class="field__help">{{ reqReason.length }}/100</span>
        </span>
        <textarea
          v-model="reqReason"
          class="field__textarea"
          placeholder="사유를 입력해 주세요."
          maxlength="100"
          rows="4"
        ></textarea>
      </label>
    </section>

    <p class="helper">
      <span class="helper__dot" aria-hidden="true">·</span>
      관리자 승인 후 근태에 반영돼요. 근태 마감 전까지 신청해 주세요. 원본 출퇴근 기록은 보존돼요.
    </p>

    <footer class="form-ft">
      <button type="button" class="btn btn--x" @click="$emit('cancel')">취소</button>
      <button type="submit" class="btn btn--p" :disabled="!isValid">요청하기</button>
    </footer>
  </form>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import SlotCard from './SlotCard.vue'

const props = defineProps({
  context: { type: Object, required: true },
})
const emit = defineEmits(['submit', 'cancel'])

const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (m) => (proxy?.$alert ? proxy.$alert(m) : window.alert(m))

// 현재 근태 시각으로 프리필 (P16). context.slots[*].attendance.startTime/endTime 가정.
// 형식: 'HH:MM' (input[type=time] 호환)
const buildInitialSlots = () => {
  const ctxSlots = (props.context?.slots || []).map((s, i) => ({
    workSeq: s.workSeq ?? i + 1,
    startDate: ymdToInput(props.context.workYmd),
    startTime: hhmmToTime(s.attendance?.startTime),
    endDate: ymdToInput(props.context.workYmd),
    endTime: hhmmToTime(s.attendance?.endTime),
  }))
  if (ctxSlots.length === 0) {
    return [
      {
        workSeq: 1,
        startDate: ymdToInput(props.context.workYmd),
        startTime: '',
        endDate: ymdToInput(props.context.workYmd),
        endTime: '',
      },
    ]
  }
  return ctxSlots.slice(0, 2)
}

const slots = ref(buildInitialSlots())
const reqReason = ref('')

// 형식 유틸 (developer 가 외부 유틸로 분리 가능)
function ymdToInput(ymd) {
  if (!ymd || ymd.length !== 8) return ''
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
}
function hhmmToTime(hhmm) {
  if (!hhmm) return ''
  if (hhmm.length === 4) return `${hhmm.slice(0, 2)}:${hhmm.slice(2)}`
  if (/^\d{2}:\d{2}/.test(hhmm)) return hhmm.slice(0, 5)
  return ''
}
function inputToYmd(s) {
  return s ? s.replace(/-/g, '') : ''
}
function timeToHhmm(s) {
  return s ? s.replace(':', '').slice(0, 4) : ''
}

const ctxDateDisplay = computed(() => {
  const y = props.context.workYmd?.slice(0, 4)
  const m = props.context.workYmd?.slice(4, 6)
  const d = props.context.workYmd?.slice(6, 8)
  return y && m && d ? `${y}년 ${Number(m)}월 ${Number(d)}일` : '-'
})
const ctxSiteDisplay = computed(() => props.context.siteName || '')
const hasIssue = computed(() => Boolean(props.context.hasIssue))

const overlapWarning = computed(() => {
  if (slots.value.length < 2) return false
  const s1End = slots.value[0].endTime
  const s2Start = slots.value[1].startTime
  if (!s1End || !s2Start) return false
  return s2Start < s1End
})

const isValid = computed(() => {
  if (!reqReason.value.trim()) return false
  return slots.value.every(
    (s) => s.startDate && s.startTime && s.endDate && s.endTime,
  )
})

const onAddSlot = () => {
  if (slots.value.length >= 2) return
  // workSeq 는 구간 식별자(1/2)이므로 비어 있는 번호를 채운다. (위치 기반 재인덱싱 금지)
  const existing = new Set(slots.value.map((s) => s.workSeq))
  const missing = [1, 2].find((n) => !existing.has(n))
  if (!missing) return
  slots.value.push({
    workSeq: missing,
    startDate: ymdToInput(props.context.workYmd),
    startTime: '',
    endDate: ymdToInput(props.context.workYmd),
    endTime: '',
  })
  slots.value.sort((a, b) => a.workSeq - b.workSeq)
}
// 구간 삭제 — workSeq 는 구간 식별자이므로 남은 구간 번호를 재인덱싱하지 않는다.
const onRemoveSlot = (workSeq) => {
  slots.value = slots.value.filter((s) => s.workSeq !== workSeq)
}

const onSubmit = () => {
  if (!isValid.value) {
    showAlert('모든 필수 항목을 입력해 주세요.')
    return
  }
  emit('submit', {
    slots: slots.value.map((s) => ({
      workSeq: s.workSeq,
      startDate: inputToYmd(s.startDate),
      startTime: timeToHhmm(s.startTime),
      endDate: inputToYmd(s.endDate),
      endTime: timeToHhmm(s.endTime),
    })),
    reqReason: reqReason.value.trim(),
  })
}
</script>

<style scoped>
.attd-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.ctx {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.ctx__date {
  margin: 0 0 var(--space-xs);
  display: flex;
  flex-direction: column;
}
.ctx__date strong {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.ctx__date small {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ctx__row {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: var(--space-sm);
  align-items: baseline;
}
.ctx__lbl {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ctx__val {
  font-size: 13px;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.ctx__val--warn {
  color: var(--color-warning-text);
}

.fs {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.fs__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field__label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.field__label .req {
  color: var(--color-danger);
}
.field__help {
  margin-left: auto;
  font-size: 11px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.input-dt {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}

.field__input {
  height: 44px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: inherit;
  font-variant-numeric: tabular-nums;
  box-sizing: border-box;
}
.field__input:focus {
  outline: none;
  border-color: var(--color-primary);
}
.field__textarea {
  width: 100%;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: inherit;
  box-sizing: border-box;
  resize: vertical;
  min-height: 96px;
}
.field__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

.btn-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 40px;
  background: var(--color-surface);
  border: 0.5px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.btn-add:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.warn-msg {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-danger-tint);
  border: 0.5px solid var(--color-danger);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--color-danger);
}

.helper {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-warning-tint);
  border: 0.5px solid var(--color-warning);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--color-warning-text);
  display: flex;
  gap: var(--space-xs);
}
.helper__dot {
  color: var(--color-warning);
}

.form-ft {
  position: sticky;
  bottom: 0;
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: var(--space-sm);
  padding: var(--space-sm) 0 calc(var(--space-sm) + env(safe-area-inset-bottom));
  background: var(--color-bg);
  border-top: 0.5px solid var(--color-border);
  margin: 0 calc(-1 * var(--space-lg));
  padding-left: var(--space-lg);
  padding-right: var(--space-lg);
}

.btn {
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.btn--x {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  color: var(--color-text-secondary);
}
.btn--p {
  background: var(--color-primary);
  border: 0;
  color: var(--color-surface);
}
.btn--p:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
</style>
```

### 8.5 `prafta-app-frontend/src/views/req/components/OvertimeForm.vue`

```vue
<!--
  OvertimeForm.vue — 초과근무 신청 폼
  - 작업 ID: PRAFTA-APP-007-8
  - emits: submit ({ slots:[{workSeq, startDate, startTime, endDate, endTime, otType}], reqReason }), cancel
  - OT_TYPE 선택은 BaseBottomSheet (prafta-app-006) 재사용 — 단일 선택 시트 신규 분리 안 함, 인라인 옵션 토글.
-->
<template>
  <form class="ot-form" @submit.prevent="onSubmit">
    <!-- 컨텍스트 -->
    <section class="ctx">
      <p class="ctx__date">
        <strong>{{ ctxDateDisplay }}</strong>
        <small>{{ ctxSiteDisplay }}</small>
      </p>
      <div class="ctx__row">
        <span class="ctx__lbl">스케줄</span>
        <span class="ctx__val">{{ context.workPlanName }} · {{ context.scheduleSummary || '-' }}</span>
      </div>
      <div v-if="context.attendanceSummary" class="ctx__row">
        <span class="ctx__lbl">근태</span>
        <span class="ctx__val">{{ context.attendanceSummary }}</span>
      </div>
    </section>

    <!-- 초과근무 시간 -->
    <section class="fs">
      <p class="fs__title">초과근무 시간</p>

      <SlotCard
        v-for="slot in slots"
        :key="slot.workSeq"
        :work-seq="slot.workSeq"
        :title="slot.workSeq + '구간 초과근무'"
        :removable="slots.length > 1"
        @remove="onRemoveSlot"
      >
        <label class="field">
          <span class="field__label"><span class="req">*</span>시작</span>
          <div class="input-dt">
            <input v-model="slot.startDate" type="date" class="field__input" />
            <input v-model="slot.startTime" type="time" class="field__input" />
          </div>
        </label>
        <label class="field">
          <span class="field__label"><span class="req">*</span>종료</span>
          <div class="input-dt">
            <input v-model="slot.endDate" type="date" class="field__input" />
            <input v-model="slot.endTime" type="time" class="field__input" />
          </div>
        </label>
        <label class="field">
          <span class="field__label"><span class="req">*</span>유형</span>
          <div class="ot-type-row">
            <button
              v-for="opt in OT_TYPE_OPTIONS"
              :key="opt.code"
              type="button"
              class="ot-type-chip"
              :class="{ 'ot-type-chip--on': slot.otType === opt.code }"
              @click="slot.otType = opt.code"
            >
              {{ opt.label }}
            </button>
          </div>
        </label>
      </SlotCard>

      <button
        v-if="slots.length === 1"
        type="button"
        class="btn-add"
        @click="onAddSlot"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        구간 추가
      </button>

      <!-- 신청 합계 -->
      <div class="total-box">
        <span class="total-box__lbl">신청 합계</span>
        <span class="total-box__val">{{ totalDisplay }}</span>
      </div>

      <p v-if="overlapWarning" class="warn-msg">
        2구간 시작 시각은 1구간 종료 시각 이후여야 합니다.
      </p>

      <label class="field">
        <span class="field__label">
          <span class="req">*</span>신청 사유
          <span class="field__help">{{ reqReason.length }}/100</span>
        </span>
        <textarea
          v-model="reqReason"
          class="field__textarea"
          placeholder="사유를 입력해 주세요."
          maxlength="100"
          rows="4"
        ></textarea>
      </label>
    </section>

    <p class="helper">
      <span class="helper__dot" aria-hidden="true">·</span>
      관리자 승인 후 추가근무로 반영돼요. 근태 마감 전까지 신청해 주세요.
    </p>

    <footer class="form-ft">
      <button type="button" class="btn btn--x" @click="$emit('cancel')">취소</button>
      <button type="submit" class="btn btn--p" :disabled="!isValid">요청하기</button>
    </footer>
  </form>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import SlotCard from './SlotCard.vue'

const props = defineProps({
  context: { type: Object, required: true },
})
const emit = defineEmits(['submit', 'cancel'])

const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (m) => (proxy?.$alert ? proxy.$alert(m) : window.alert(m))

const OT_TYPE_OPTIONS = [
  { code: 'EXTEND', label: '연장' },
  { code: 'NIGHT', label: '야간' },
  { code: 'HOLIDAY', label: '휴일' },
]

function ymdToInput(ymd) {
  if (!ymd || ymd.length !== 8) return ''
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
}
function inputToYmd(s) {
  return s ? s.replace(/-/g, '') : ''
}
function timeToHhmm(s) {
  return s ? s.replace(':', '').slice(0, 4) : ''
}

const slots = ref([
  {
    workSeq: 1,
    startDate: ymdToInput(props.context.workYmd),
    startTime: '',
    endDate: ymdToInput(props.context.workYmd),
    endTime: '',
    otType: 'EXTEND',
  },
])
const reqReason = ref('')

const ctxDateDisplay = computed(() => {
  const y = props.context.workYmd?.slice(0, 4)
  const m = props.context.workYmd?.slice(4, 6)
  const d = props.context.workYmd?.slice(6, 8)
  return y && m && d ? `${y}년 ${Number(m)}월 ${Number(d)}일` : '-'
})
const ctxSiteDisplay = computed(() => props.context.siteName || '')

const overlapWarning = computed(() => {
  if (slots.value.length < 2) return false
  const s1End = slots.value[0].endTime
  const s2Start = slots.value[1].startTime
  if (!s1End || !s2Start) return false
  return s2Start < s1End
})

// 신청 합계 (분 단위 → 시간/분 표시)
const totalMinutes = computed(() => {
  let total = 0
  for (const s of slots.value) {
    if (!s.startTime || !s.endTime) continue
    const sM = toMinutes(s.startTime)
    const eM = toMinutes(s.endTime)
    if (sM < 0 || eM < 0) continue
    let diff = eM - sM
    if (diff < 0) diff += 24 * 60
    total += diff
  }
  return total
})
function toMinutes(hhmm) {
  if (!/^\d{2}:\d{2}/.test(hhmm)) return -1
  return Number(hhmm.slice(0, 2)) * 60 + Number(hhmm.slice(3, 5))
}
const totalDisplay = computed(() => {
  const m = totalMinutes.value
  if (m === 0) return '0분'
  const h = Math.floor(m / 60)
  const min = m % 60
  if (h === 0) return `${min}분`
  if (min === 0) return `${h}시간`
  return `${h}시간 ${min}분`
})

const isValid = computed(() => {
  if (!reqReason.value.trim()) return false
  return slots.value.every(
    (s) => s.startDate && s.startTime && s.endDate && s.endTime && s.otType,
  )
})

const onAddSlot = () => {
  if (slots.value.length >= 2) return
  // workSeq 는 구간 식별자(1/2)이므로 비어 있는 번호를 채운다. (위치 기반 재인덱싱 금지)
  const existing = new Set(slots.value.map((s) => s.workSeq))
  const missing = [1, 2].find((n) => !existing.has(n))
  if (!missing) return
  slots.value.push({
    workSeq: missing,
    startDate: ymdToInput(props.context.workYmd),
    startTime: '',
    endDate: ymdToInput(props.context.workYmd),
    endTime: '',
    otType: 'EXTEND',
  })
  slots.value.sort((a, b) => a.workSeq - b.workSeq)
}
// 구간 삭제 — workSeq 는 구간 식별자이므로 남은 구간 번호를 재인덱싱하지 않는다.
const onRemoveSlot = (workSeq) => {
  slots.value = slots.value.filter((s) => s.workSeq !== workSeq)
}

const onSubmit = () => {
  if (!isValid.value) {
    showAlert('모든 필수 항목을 입력해 주세요.')
    return
  }
  emit('submit', {
    slots: slots.value.map((s) => ({
      workSeq: s.workSeq,
      startDate: inputToYmd(s.startDate),
      startTime: timeToHhmm(s.startTime),
      endDate: inputToYmd(s.endDate),
      endTime: timeToHhmm(s.endTime),
      otType: s.otType,
    })),
    reqReason: reqReason.value.trim(),
  })
}
</script>

<style scoped>
.ot-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* (ctx / fs / field / input-dt / btn-add / helper / form-ft / btn 은 AttdCorrectionForm 과 동일 패턴 — DRY 를 위해 추후 공통 SCSS 모듈로 분리 가능. 골격은 명시적 복제) */
.ctx {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.ctx__date {
  margin: 0 0 var(--space-xs);
  display: flex;
  flex-direction: column;
}
.ctx__date strong {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.ctx__date small {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ctx__row {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: var(--space-sm);
  align-items: baseline;
}
.ctx__lbl {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ctx__val {
  font-size: 13px;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

.fs {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.fs__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field__label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.field__label .req {
  color: var(--color-danger);
}
.field__help {
  margin-left: auto;
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.input-dt {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}

.field__input {
  height: 44px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: inherit;
  font-variant-numeric: tabular-nums;
  box-sizing: border-box;
}
.field__input:focus {
  outline: none;
  border-color: var(--color-primary);
}
.field__textarea {
  width: 100%;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: inherit;
  box-sizing: border-box;
  resize: vertical;
  min-height: 96px;
}
.field__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* OT_TYPE 칩 (단일 선택 인라인) */
.ot-type-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.ot-type-chip {
  height: 36px;
  padding: 0 14px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}
.ot-type-chip--on {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
  font-weight: 500;
}

.btn-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 40px;
  background: var(--color-surface);
  border: 0.5px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.btn-add:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* 신청 합계 박스 */
.total-box {
  background: var(--color-primary-tint);
  border: 0.5px solid var(--color-primary-tint-border);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.total-box__lbl {
  font-size: 13px;
  color: var(--color-primary-text-deep);
  font-weight: 500;
}
.total-box__val {
  font-size: 14px;
  color: var(--color-primary-text-darkest);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.warn-msg {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-danger-tint);
  border: 0.5px solid var(--color-danger);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--color-danger);
}

.helper {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-warning-tint);
  border: 0.5px solid var(--color-warning);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--color-warning-text);
  display: flex;
  gap: var(--space-xs);
}
.helper__dot {
  color: var(--color-warning);
}

.form-ft {
  position: sticky;
  bottom: 0;
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: var(--space-sm);
  padding: var(--space-sm) 0 calc(var(--space-sm) + env(safe-area-inset-bottom));
  background: var(--color-bg);
  border-top: 0.5px solid var(--color-border);
  margin: 0 calc(-1 * var(--space-lg));
  padding-left: var(--space-lg);
  padding-right: var(--space-lg);
}

.btn {
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.btn--x {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  color: var(--color-text-secondary);
}
.btn--p {
  background: var(--color-primary);
  border: 0;
  color: var(--color-surface);
}
.btn--p:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
</style>
```

---

## 9. 라우트 패치 골격

> **PRAFTA-APP-007-10.** `prafta-app-frontend/src/router/index.js` 의 `routes[]` 배열에 다음 1줄 추가 (MyRequests 등록 위치 바로 아래 권장).

```js
// PRAFTA-APP-007: 근태 요청 폼 (스케줄 수정 / 근태 보정 / 초과근무)
{
  path: '/AttdRequest',
  name: 'AttdRequest',
  component: () => import('@/views/req/AttdRequestView.vue'),
},
```

`publicPaths` 미추가 (로그인 필요).

---

## 10. ActionSheet / DayDetailCard 핸들러 패치 골격

> **PRAFTA-APP-007-10.** `prafta-app-frontend/src/views/attd/MyAttendanceView.vue` 의 `onSheetAction` 과 `onDayDetailAction` 본문 교체.

```js
// PRAFTA-APP-007: 폼 라우팅 헬퍼
const CONTEXT_KEY = 'attd_req_ctx_v1'

function buildContextFromDay(day) {
  if (!day) return null
  return {
    workYmd: day.workYmd,
    nodeCd: day.nodeCd,
    siteName: day.siteName, // MyAttendanceView 상위에서 주입 또는 응답에서 가져옴
    scheduleSummary: day.scheduleSummary,
    workPlanName: day.workPlanName,
    attendanceSummary: day.attendanceSummary,
    hasIssue: day.hasIssue,
    slots: day.slots, // 출퇴근 시각 프리필용
  }
}

function navigateToAttdRequest(formType, day) {
  if (!day || !day.workYmd) {
    showAlert('대상 일자를 확인할 수 없습니다.')
    return
  }
  try {
    sessionStorage.setItem(CONTEXT_KEY, JSON.stringify(buildContextFromDay(day)))
  } catch (e) {
    showAlert('컨텍스트 저장에 실패했습니다.')
    return
  }
  router.push({
    path: '/AttdRequest',
    query: { type: formType, workYmd: day.workYmd, nodeCd: day.nodeCd || '' },
  })
}

// 이번주 카드 바텀시트 액션 4종
const onSheetAction = (payload) => {
  // payload: { type: 'scheduleModify' | 'attendanceCorrection' | 'overtime' | 'leave', day }
  actionSheetOpen.value = false
  const type = payload?.type
  if (type === 'scheduleModify') {
    return navigateToAttdRequest('schedModify', payload.day)
  }
  if (type === 'attendanceCorrection') {
    return navigateToAttdRequest('attdCorrection', payload.day)
  }
  if (type === 'overtime') {
    return navigateToAttdRequest('overtime', payload.day)
  }
  // leave 는 본 작업 외 — stub 유지
  showAlert('준비 중입니다')
}

// 이번달 일자 상세 카드의 빠른 액션 (근태보정 / 초과근무)
const onDayDetailAction = (payload) => {
  // payload: { type: 'attendanceCorrection' | 'overtime', detail }
  const type = payload?.type
  if (type === 'attendanceCorrection') {
    return navigateToAttdRequest('attdCorrection', payload.detail)
  }
  if (type === 'overtime') {
    return navigateToAttdRequest('overtime', payload.detail)
  }
  showAlert('준비 중입니다')
}
```

> `onTodayAction` 의 `requestModify` 분기는 본 작업 범위 외 (오늘 카드의 근태 수정 액션 — follow-up F9).

---

## 11. 마이그레이션 SQL 골격

> **PRAFTA-APP-007-1.** §3.1 의 마이그레이션 파일 본문 그대로. 작성 위치: `prafta-backend/src/main/resources/sql/migration/prafta-app-007-attd-req-extensions.sql`.

(§3.1 참조 — 본 절은 중복 명시 회피)

---

## 12. 차후 분해 시 메모

- **F-A (결재선 통합)** 은 본 plan 와 동시에 작성된 `.claude/requests/app_requests/prafta-app-009.md` 가 단일 출처. 본 plan §7 F-A 와 prafta-app-009.md §1 (A)~(D) 내용 동기화 필요.
- **F2 (SCH_CD 목록 endpoint)** 분해 시 기존 `tb_user_attd_schedule` 또는 `tb_schedule_mgmt` (스케줄 마스터) 조회 매퍼 재사용 가능. 본 작업 1차는 placeholder 텍스트 입력.
- **F3 (초과근무 신청 가능 범위 안내)** 분해 시 `attd §10.2~§10.3` 정독 + AppAttd01ServiceImpl 의 `computeAttendanceStatus` 같은 패턴으로 클라 표시 산출.
- BE 등록 endpoint 의 결재선 INSERT 분기를 위해 본 작업 BE 코드 구조는 "INSERT 후 결재선 INSERT" 확장 지점을 명확히 표시 (Service 메서드 끝부분에 `// TODO(prafta-app-009): tb_user_attd_req_approval INSERT` 마커).
- 본 plan 작성 시점에 `prafta-app-frontend/src/views/req/` 디렉토리는 prafta-app-006 으로 생성되어 있음 (BaseBottomSheet 등). 본 작업의 컴포넌트는 이 디렉토리에 추가.
- BaseBottomSheet 가 prafta-app-006 신설 의존. prafta-app-006 의 PRAFTA-APP-006-2 작업이 완료되지 않은 상태에서 본 작업 F1 (SCH_CD/OT_TYPE BottomSheet 도입) 을 시도하면 의존 충돌. 본 작업 1차는 BottomSheet 미사용 (인라인 칩 / native input) 으로 의존 제거.
- AttdReqTypeUtils 에 신규 상수 `REQ_TYPE_SCHED_MODIFY = "10"` 추가 시 web 패키지 util 을 app 에서 import 하는 형태가 됨. 컨벤션 검토 필요 — 본 plan 권장은 (a) `AttdReqTypeUtils` 그대로 사용 또는 (b) `com.prafta.app.req.req07.util.AppReqTypeConstants` 신설. PRAFTA-APP-007-2 작업 시 developer 가 결정.
- 본 plan 의 마이그레이션 SQL 의 `tb_syst_val_d` INSERT 의 `CMPNY_CD='*'` 는 실제 운영 DB 의 SYS032 마스터 적재 패턴 확인 후 조정 필요 (회사별 분리 INSERT 일 수도 있음).
