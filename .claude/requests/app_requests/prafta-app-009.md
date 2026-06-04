# prafta-app-009 — prafta-app-007 follow-up: 결재선 통합 + 추가 보완

> **작업 ID prefix**: `PRAFTA-APP-009` (분해 단계에서 단일 출처 plan `prafta-app-009-plan.md` 작성 예정)
> **본 문서 위상**: prafta-app-007 의 follow-up 모음. **작업 요청서 단계** (아직 plan 으로 분해되지 않음). 사용자가 본 문서를 검토 후 "분해 진행" 지시하면 planner 가 `prafta-app-009-plan.md` 로 분해한다.
>
> **관련 산출물**:
> - `.claude/requests/app_requests/prafta-app-007-plan.md` — 1차 등록 폼 3종 (결재선 미포함). 본 문서가 의존.
> - `.claude/requests/app_requests/refs/prafta-app-007/prafta_request_forms_v8.html` — 시안 §5 결재선 케이스 4종 (line 1849~).
> - `project_prafta_020_aprv_preset` 메모리 — USER_04 결재라인 프리셋 (다중 프리셋, 사용자별 소유, `tb_aprv_line_preset(+_D)`, 사업장 세션 고정, LeaveApplyPop 연동 패턴).

---

## 0. 배경

prafta-app-007 1차 완료 후 결재선 영역이 분리되었다. 1차는 단순 등록 → 관리자 1단 승인 흐름 (`tb_user_attd_req` INSERT 만). 본 작업은 다음을 추가한다:

1. prafta-app-007 의 3종 폼 (스케줄 수정 / 근태 보정 / 초과근무) 에 **결재선 영역 추가**.
2. **prafta-020 USER_04 결재라인 프리셋 연동** (프리셋 드롭다운 / 사용자 검색 / 프리셋 저장).
3. 시안 `prafta_request_forms_v8.html` **§5 결재선 케이스 4종 (1849줄~)** UI 반영.
4. 결재선 INSERT 흐름 (`tb_user_attd_req_approval` 다중 행 INSERT, prafta-020 결재 처리 패턴).

추가로:
- prafta-app-007 작성 과정에서 식별된 비결재선 follow-up 후보 (F1~F11).
- prafta-app-007 1차 **구현 완료 후 BE/FE 라운드에서 신규 식별된** 보안·정합성·운영 가드 follow-up (F12~F17).

분해 시 우선순위로 분할 가능.

---

## 0.1 prafta-app-007 1차 구현 완료 시점 상태 (2026-05-29)

본 문서 갱신 시점에 prafta-app-007 1차 BE/FE 가 모두 완료되어 빌드 통과 (`BUILD SUCCESSFUL in 16s` / `vite build ✓ built in 8.80s`). 다음 항목들은 **의도적 미구현 (Q3 등 결정으로 분리)** 또는 **별도 의존 추가 회피로 미구현 (P11/P20)** 으로 남았다:

| 코드 | 항목 | 상태 |
|---|---|---|
| Q3 | 결재선 INSERT (`tb_user_attd_req_approval`) | 의도적 미구현 (본 문서 §1.D) |
| P4 | 알림 발송 (outbox) | 의도적 미구현 (§2 F1) |
| P11 | 마감 가드 (`isMonthClosed`) | 미구현 — ATTD_400_099 enum 등록 (throw 대기) (§2 F12) |
| P11 | 본인 스케줄 존재 가드 | 미구현 — ATTD_400_098 enum 등록 (throw 대기) (§2 F13) |
| P20 | NODE_CD 검증 | 미구현 — 현재 클라 nodeCd 그대로 INSERT (§2 F14) |
| F2 | SCH_CD 목록 BottomSheet | 미구현 — 현재 native `<input type="text">` (§2 F2 — 기존) |
| —  | 동시 등록 race condition 차단 | 미구현 — 인덱스 UNIQUE 아님, advisory lock 없음 (§2 F15) |
| —  | 마이그 적용 순서 운영 가이드 | 미작성 — endpoint 배포 전 마이그 필수 (§2 F16) |
| —  | day-detail 응답의 `siteName` 필드 존재 확인 | 미검증 — FE 골격이 가정 (§2 F17) |

---

## 1. 작업 범위

### (A) prafta-app-007 의 3종 폼에 결재선 영역 추가

대상 컴포넌트 (prafta-app-007 §3.3 신설):
- `SchedModifyForm.vue`
- `AttdCorrectionForm.vue`
- `OvertimeForm.vue`

추가할 UI 섹션 (시안 §5 형식 — `prafta_request_forms_v8.html` 1849줄~):
- "결재선" 섹션 (사유 입력 위 또는 아래에 위치)
- 결재 단계 표시 (1단 → 2단 → ... → N단)
- 단계별 결재자 카드 (이름·직급·소속 노드)
- 자기 승인 표기 (요청자=결재자 인 경우 "자동 승인" 라벨)
- 프리셋 선택 트리거 (드롭다운 + 변경 버튼)
- 결재자 추가/삭제 (드래그 핸들 또는 위/아래 버튼)

emits 보완:
- 기존 `submit ({ slots, reqReason })` → `submit ({ slots, reqReason, approvalLine: [{stepNo, userCd, ...}] })`

검증:
- 결재선 최소 1단계 필수.
- 동일 사용자 중복 차단.
- 마지막 단계가 HR 사용자인지 (선택적 — HR 최종 ON 정책 적용 시).

### (B) prafta-020 USER_04 결재라인 프리셋 연동

기존 자산 (메모리 `project_prafta_020_aprv_preset` 인용):
- `tb_aprv_line_preset` (마스터 — PRESET_ID, USER_CD, NAME, USE_TYPE 등)
- `tb_aprv_line_preset_d` (디테일 — PRESET_ID, STEP_NO, APROVER_USER_CD)
- 사용자별 소유 다중 프리셋 (한 사용자가 N개의 결재라인 보유 가능)
- 사업장 세션 고정 (현재 사업장 기준 프리셋만 노출)
- LeaveApplyPop 가 동일 패턴 사용 — UI 패턴 참조

신규 endpoint (모바일 앱):
- `GET /appApi/req09/aprv-line-presets` — 본인의 프리셋 목록 조회 (USE_TYPE='ATTD_REQ' 필터링 — prafta-020 USE_TYPE 분류 활용)
- `GET /appApi/req09/aprv-line-presets/{presetId}` — 특정 프리셋 디테일 조회 (결재자 사용자 정보 포함)
- `POST /appApi/req09/aprv-line-presets` — 프리셋 신규 저장
- `PUT /appApi/req09/aprv-line-presets/{presetId}` — 프리셋 수정
- `DELETE /appApi/req09/aprv-line-presets/{presetId}` — 프리셋 삭제
- `GET /appApi/req09/users/search` — 결재자 후보 사용자 검색 (이름·사번 일부 일치)
  - 본인 사업장 + 본인 조직 상위/하위 + 사이트 권한 가진 사용자 범위 (정책 `common/08-permissions.md` §8.4 조직 스코프)

UX:
- 폼 진입 시 본인의 "기본 프리셋" 자동 선택 (USE_TYPE='ATTD_REQ' 의 IS_DEFAULT='Y' 또는 최근 사용)
- "프리셋 변경" 클릭 → BottomSheet (BaseBottomSheet) 에서 프리셋 목록 선택
- "사용자 검색하여 추가" 클릭 → 검색 BottomSheet → 결과 선택 → 결재 단계 끝에 추가
- 결재선 편집 후 "프리셋으로 저장" 옵션 (체크박스 + 이름 입력)

### (C) 시안 §5 결재선 케이스 4종

| 케이스 | 단계 수 | 시안 줄 (대략) |
|---|---|---|
| 5-1 | 1단 | 1849~ (단일 결재자) |
| 5-2 | 2단 | (1단 + 2단, 본인 자동 승인 케이스 포함) |
| 5-3 | 3단 |  |
| 5-4 | 4단 + HR | (마지막 단계 HR) |

각 케이스의 UI 차이:
- 단계 수에 따른 카드 세로 배치
- 본인 결재 자동 승인 라벨 (요청자=Nth 결재자 인 경우)
- HR 최종 단계 배지 ("HR 최종 승인")
- 상위 위임 표기 (담당 정 부재 시 담당 부 또는 위임 노드)

### (D) 백엔드 결재선 INSERT 흐름

대상 테이블:
- `tb_user_attd_req_approval` (다중 행, STEP_NO 별 결재자)
- 컬럼 (확인 필요): REQ_ID, STEP_NO, APROVER_USER_CD, STATUS ('01'대기/'02'승인/'03'반려), DECIDED_DATE, COMMENT 등

INSERT 흐름 (prafta-app-007 의 endpoint 확장):
1. tb_user_attd_req INSERT (1차 로직 그대로)
2. **자기 승인 자동 처리**: STEP_NO=1 의 결재자가 요청자 본인이면 STATUS='02'(승인) + DECIDED_DATE=NOW() 로 INSERT (정책 `attd §9.5` 자기 승인 원칙).
3. 나머지 단계는 STATUS='01' (대기) 로 INSERT.
4. 모든 단계가 승인 상태로 INSERT 되면 (1단계 자동 승인 + 그 뒤 단계 없음) → tb_user_attd_req.REQ_STATUS='02' 승인으로 동시 갱신.

prafta-app-007 endpoint 의 확장 지점 (`AppReq07ServiceImpl` 각 register 메서드 끝부분의 `// TODO(prafta-app-009)` 마커) 에서 분기.

기존 자산:
- prafta-020 의 web 측 결재 처리 패턴 (`com.prafta.web.user.user04.*` 또는 결재 처리 service) 참조.
- LeaveFlow 의 다단 결재 처리 (`LeaveFlowServiceImpl#applyLeave` 의 6번째 단계 "결재 Y → 라인 일괄 생성") 직접 참조 가능.

### (E) 결재 "차례 도래" PUSH 통합 (신규 — prafta-com-004 와 동일 유형)

> 사용자 요청(2026-06-03): 근태·초과근무·스케줄수정 요청이 결재선을 탈 때, **결재 단계가 진행되어 '내 차례'가 도래하는 결재자에게 PUSH**. 연차는 결재선 인프라가 이미 있어 `prafta-com-004` 로 선행 처리하고, 근태/OT/스케줄수정은 결재선 자체가 본 작업(§1 A~D)에서 신설되므로 **본 작업에 PUSH 를 함께 통합**한다.

발송 시점(코드 hook):
1. **신청 시 첫 결재자 지정** — 본 작업 §1.D 의 결재선 INSERT 흐름에서 `tb_user_attd_req_approval` 의 첫 수동(비-자동승인) 단계를 `APPROVAL_STATUS='01'(신청)` 로 INSERT 하는 지점. (연차 `submitLeave` 의 `currentIdx` 단계 패턴 미러)
2. **다음 단계 진행** — 근태/OT/스케줄수정 결재 승인에서 다음 대기 단계를 `01` 로 전환하는 지점. ⚠️ **현재 근태/OT/스케줄수정 승인은 단계 개념 없는 단일 승인**(`Attd07ServiceImpl.updateUserAttdRequest` / `updateUserOvertimeRequests` / `approveSchedModifyRequest` — REQ_STATUS `01→02` 직접 전이)이다. 본 작업에서 다단계 승인 처리(연차 `approveStep` 의 `selectFirstWaitingStep`→`STEP_APPLIED` 패턴)를 **웹 승인 경로에 신설**해야 차례 도래 개념이 성립한다. 이는 §1.D(신청 측 INSERT)보다 범위가 큼(웹 승인 화면/서비스 다단계화) → 분해 시 별도 작업 단위로 분리 가능.

발송 대상/제외: 연차(prafta-com-004 §1.A)와 동일 규칙 — 진행되는 단계의 `APPROVER_USER_CD` 1인만. 자기 승인 자동 `02` 로 건너뛴 단계·신청자 본인 단계·반려·최종 승인(다음 단계 없음)은 제외. master/hr 자동 포함 없음.

재사용: `NotiOutboxInsertVO` + `LeaveDashboardMapper.insertNotiOutbox`(prafta-031), 발송은 prafta-com-002 공용 워커. SYS045 신규 코드 `ATTD_APPROVAL_TURN`(또는 prafta-com-004 의 `LEAVE_APPROVAL_TURN` 과 합친 범용 `APPROVAL_TURN` — 분해 시 결정). dedupKey = `"REQ_TURN_" + reqId + "_" + approvalStep`. PII/IDOR/트랜잭션 격리 검토는 prafta-com-004 §5 와 동일.

---

## 2. prafta-app-007 plan §7 follow-up 후보 (인용)

prafta-app-007-plan.md §7 의 F-A 는 본 문서 (=prafta-app-009) 로 이전. 그 외:

| # | 항목 | 비고 |
|---|---|---|
| F1 | 알림 발송 (등록 직후 관리자 push outbox INSERT) | **→ §1.E 로 승격(2026-06-03)**. 결재 "차례 도래" PUSH 를 본 작업에 통합. com-002 워커·com-001 마이그 운영 반영 완료. |
| F2 | SCH_CD 목록 조회 endpoint (스케줄 수정 폼) | 본 작업과 독립. 분해 시 우선순위 분리. |
| F3 | 시안 §3-1 "신청 가능 범위 안내" (스케줄 종료~근태 퇴근 사이 자동 계산) | OT 폼 UX 강화. 본 작업과 독립. |
| F4 | OT_TYPE 자동 추정 | OT 폼 UX. |
| F5 | 증빙 첨부 (근태 보정 — 정책 §11.2 "필요 시") | 파일 업로드 인프라. |
| F6 | 2구간 겹침 클라 차단 (현재 인라인 경고만) | 작은 UX 개선. |
| F7 | 등록 후 상세 화면 이동 (현재 router.back) | prafta-app-006 F1 (요청 상세) 의존. |
| F8 | 사후 상신 기한 임박 배지 ("D-N영업일") | 클라 표시 + 백엔드 deadlineAt 계산. |
| F9 | onTodayAction 의 requestModify (오늘 카드의 근태 수정 액션) 연동 | MyAttendanceView stub. |
| F10 | 일자 셀렉터 컴포넌트 (`<input type="date">` 대체) | iOS/안드로이드 native picker 통일. |
| F11 | 자정 넘김 (endDate = workYmd +1) 자동 보정 UX | 클라 자동 보정. |
| **F12** | **마감 가드 (P11)** — `isMonthClosed` 또는 `AttdCloseService` 호출 후 ATTD_400_099 throw | **BE 식별 (2026-05-29)**. ATTD_400_099 enum 은 이미 등록됨 (`AttdErrorCode`). 호출만 추가하면 됨. 별도 Service 의존 추가 필요. |
| **F13** | **본인 스케줄 존재 가드 (P11)** — `AppAttd01ServiceImpl.canRequestScheduleModify` 동등 로직으로 SCH_CD 존재 검증, 부재 시 ATTD_400_098 throw | **BE 식별**. ATTD_400_098 enum 등록됨. AppAttd01 의 `selectScheduleByYmd` 재사용 검토. 스케줄 수정/근태 보정 모두 영향. |
| **F14** | **NODE_CD 검증 (P20)** — JWT `gv_nodeCd` 또는 일자 컨텍스트 기반 권한 검증 | **BE 식별**. 현재는 클라가 보낸 `nodeCd` 그대로 INSERT — 노드 cross-site IDOR 가능. JWT 토큰의 `gv_nodeCd` 또는 본인의 노드 매핑 검증 필요. |
| **F15** | **동시 등록 race condition 차단** | **BE 식별**. 현재 `countDuplicateReq` SELECT → INSERT 사이 race window. 옵션: ① `tb_user_attd_req` 에 UNIQUE INDEX (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD, WORK_SEQ, REQ_TYPE, REQ_STATUS='01') 추가 — 마이그 1건 / ② MySQL advisory lock (`GET_LOCK(?, ?)`). 정확도 필요 케이스에서 결정. |
| **F16** | **마이그 적용 순서 운영 가이드** | **BE/FE 식별**. `prafta-app-007-attd-req-extensions.sql` (SYS032=10 + `tb_user_attd_req.SCH_CD` ADD) 적용 전 endpoint 배포 시 `Unknown column 'SCH_CD'` 런타임 에러 발생. 운영 적용 순서 = ① 마이그 SQL → ② 백엔드 배포 → ③ Flutter APK 재빌드. 운영자 가이드 1줄 작성. |
| **F17** | **`day.siteName` 필드 백엔드 검증** | **FE 식별**. `MyAttendanceView` day-detail 응답에 `siteName` 필드가 실제 존재하는지 미검증. FE 골격이 가정한 상태 — 부재 시 폼 컨텍스트 박스의 사업장명이 빈 문자열로 표시. 백엔드 응답 DTO 점검 + 필요 시 추가. |

본 작업 분해 시 결재선 영역 (A·B·C·D) 만 1차 분해할지, F1~F11 / F12~F17 을 함께 묶을지는 planner 가 분해 단계에서 결정.

**우선순위 권장** (분해 시 참고):
- 🔴 **High (보안·정합성)**: F12 (마감), F13 (스케줄 존재), F14 (NODE_CD IDOR), F15 (race). 본 작업과 함께 묶어 처리 권장.
- 🟡 **Medium (UX/완성도)**: F2, F3, F4, F5, F6, F10, F11.
- 🟢 **Low (운영/기타)**: F1 (알림 — outbox 의존), F7 (상세 화면 — prafta-app-006 F1 의존), F8, F9, F16, F17.

---

## 3. prafta-020 의 결재라인 자산 활용 가이드

본 작업 분해 시 다음을 정독:

| 자산 | 위치 (예상) | 용도 |
|---|---|---|
| `tb_aprv_line_preset` 스키마 | MCP MySQL `DESCRIBE tb_aprv_line_preset` | 컬럼·인덱스 확인 |
| `tb_aprv_line_preset_d` 스키마 | MCP MySQL `DESCRIBE tb_aprv_line_preset_d` | 디테일 컬럼 |
| `tb_user_attd_req_approval` 스키마 | MCP MySQL | 결재 단계 저장 테이블 |
| web 측 USER_04 화면 | `prafta-web-frontend/src/views/user/user04/` | 프리셋 관리 UI 패턴 |
| web 측 USER_04 endpoint | `com.prafta.web.user.user04.*` | endpoint 패턴 |
| LeaveFlow 결재 INSERT | `com.prafta.web.attd.leaveflow.service.impl.LeaveFlowServiceImpl#applyLeave` (line 152~ 의 결재 라인 일괄 생성 부분) | 다단 결재 INSERT 패턴 |
| LeaveApplyPop 결재선 UI | `prafta-web-frontend/src/views/.../LeaveApplyPop.vue` | 결재선 표시 UI 패턴 |

분해 시 정책서 출처:
- `attd/09-requests-approval.md` §9.5 (자기 승인 원칙 — 본인이 결재자인 경우 자동 승인)
- `request-approval/06-approval-flows.md` §6.1~§6.4 (각 결재 플로우)
- `request-approval/09-data-structures.md` §9.1 (lock·decision·history 필드)
- `common/08-permissions.md` §8.4 (조직 스코프 — 결재자 후보 가시 범위)
- `common/13-ui-ux.md` §13.3 (인터랙션 — 결재선 카드)

---

## 4. 의존성

- **선행 (반드시 완료 후 본 작업 착수)**: prafta-app-007 (1차 등록 폼 3종) 전 작업.
- **선행 (메모리만 활용 — 작업 자체는 별도)**: prafta-020 USER_04 결재라인 프리셋 (web 측 완료).
- **병행 가능**: prafta-031 outbox consumer (F1 알림 — 본 작업 내 단계별 알림 통합 시 필요).

---

## 5. 분해 시점 결정 사항 (planner 분해 단계로 위임)

다음은 분해 단계에서 planner 가 결정:

| # | 결정 포인트 | 후보 |
|---|---|---|
| Q1 | 결재선 영역 UX — 폼 내 인라인 vs 별도 단계 (Step 2) | 인라인 (1화면) vs 2단 진행 |
| Q2 | 프리셋 자동 적용 우선순위 | IS_DEFAULT='Y' > 최근 사용 > 사용자 선택 |
| Q3 | 결재자 검색 — 부서 트리 vs 이름 검색 | 둘 다 vs 이름 검색만 |
| Q4 | 자기 승인 자동 처리 정책 | 1단계만 자동 처리 vs 본인 등장 모든 단계 자동 처리 |
| Q5 | 결재 단계 최대 수 | 정책서 명시 없음 — 4단 (시안 §5-4) vs 무제한 |
| Q6 | 결재선이 비어 있을 때 | 폼 제출 거부 vs 1단 (해당 노드 담당 정) 자동 채움 |
| Q7 | HR 최종 ON 표기 | UI 배지 + 자동 추가 vs 사용자 수동 |
| Q8 | 본 작업의 F1~F11 묶음 | 결재선만 1차 / F2·F3·F8 등 일부 묶기 / 전부 분해 |

---

## 6. 추정 작업 단위 (분해 전 가이드)

분해 시 작업 단위는 다음 수준으로 예상 (정확한 수는 planner 가 결정):

### 결재선 통합 (§1 A·B·C·D)
- **마이그레이션**: 0건 또는 1건 (`tb_aprv_line_preset` 가 USE_TYPE='ATTD_REQ' 분류를 이미 지원하는지 DB 확인 필요. 미지원 시 SYS 코드 추가 마이그레이션 1건).
- **백엔드 신규 패키지**: `com.prafta.app.req.req09` (또는 `com.prafta.app.aprv.aprv09`) — 프리셋 CRUD 5종 + 사용자 검색 1종 + 결재선 INSERT 확장.
- **백엔드 보완**: prafta-app-007 의 `com.prafta.app.req.req07` 3 endpoint 에 결재선 INSERT 분기 추가 (Service 메서드 끝부분).
- **프론트엔드 신규 컴포넌트**: 3~5개 (ApprovalLineSection / ApprovalStepCard / PresetSelectSheet / UserSearchSheet / SavePresetDialog).
- **프론트엔드 보완**: prafta-app-007 의 3 폼 (SchedModifyForm / AttdCorrectionForm / OvertimeForm) 에 결재선 섹션 추가.

### F12~F17 High 우선순위 가드 (별도 작업 단위 또는 결재선과 묶음)
- **F12 마감 가드**: `com.prafta.app.req.req07.service.impl.AppReq07ServiceImpl` 3 메서드 (`registerSchedModify` / `registerAttdCorrection` / `registerOvertime`) 에 `attdCloseService.isMonthClosed` 또는 동등 호출 추가 + ATTD_400_099 throw. `AttdCloseService` 의 app 패키지 호환성 확인 필요.
- **F13 본인 스케줄 존재 가드**: `AppAttd01ServiceImpl.canRequestScheduleModify` 의 SELECT 로직 재사용 또는 별도 mapper 추가. `registerSchedModify` 와 `registerAttdCorrection` 에 적용.
- **F14 NODE_CD 검증**: JWT TokenInfo 에 `gv_nodeCd` 가 있다면 그것으로 강제. 없다면 본인 노드 매핑 SELECT 추가. 3 endpoint 공통.
- **F15 race condition 차단**: 마이그 1건 추가 — `tb_user_attd_req` 에 partial UNIQUE INDEX (가능 조건만, MySQL functional index 검토). 또는 advisory lock 패턴 (Service 메서드 시작 시 `GET_LOCK`).
- **F16 운영 가이드**: 운영자용 README 또는 마이그 SQL 헤더 주석 보강 1건.
- **F17 `day.siteName` 검증**: BE 응답 DTO 점검 (`AppAttd01ServiceImpl` 의 day-detail 응답 빌드 부분) + 필요 시 추가.

### 그 외 F1~F11 (Medium/Low)
- 별도 작업 단위 (분해 시 결정). 결재선 통합과 묶을지 분리할지 planner 가 결정.

---

## 7. 채팅 노트

본 문서는 작업 요청서 (사용자 작성 또는 planner 작성 요약). prafta-app-007 1차 BE/FE 완료 (2026-05-29) 후 사용자가 본 문서를 검토하고 "prafta-app-009 분해 진행" 지시하면 planner 가 `prafta-app-009-plan.md` 로 분해한다.

### 본 문서 갱신 이력
- **2026-05-29 (초안)**: prafta-app-007 plan 작성과 동시에 결재선 분리 follow-up 문서로 작성. F1~F11 (plan 작성 시점 follow-up) 포함.
- **2026-05-29 (1차 구현 완료 후 갱신)**: BE/FE 구현 완료 시점에 추가 식별된 follow-up F12~F17 추가. §0.1 1차 구현 완료 상태 표 신설. §2 우선순위 권장 (High/Medium/Low) 분류 추가. §6 작업 단위 추정 보완.

### 분해 전 사용자에게 질문할 후보
- Q1 (인라인 vs 2단) — UX 톤 영향 큼.
- Q4 (자기 승인 자동 처리 범위) — 비즈니스 규칙.
- Q5 (최대 결재 단계 수) — 데이터 한계.
- Q8 (F1~F17 묶음 범위) — 분해 규모.
- **(신규)** 본 작업 1차 분해 시 F12~F15 High 가드를 결재선과 묶을지 분리할지. 분리 시 별도 단기 작업 (`prafta-app-008` 등) 으로 처리 가능.

---

## 8. 분해 확정 결정 (2026-06-04) — 단일 출처

본 섹션이 분해의 단일 출처다. 본문 §1~§7 의 일부 가정은 코드/스키마 실측으로 정정되었으니, **충돌 시 본 §8 이 우선**한다.

### 8.0 스키마/코드 실측 정정 (요청서 추측 → 실제)

| 항목 | 요청서 본문 가정 | 실제(2026-06-04 실측) |
|---|---|---|
| 결재단계 컬럼명 (§1.D) | `STEP_NO / APROVER_USER_CD / STATUS / DECIDED_DATE / COMMENT` | **`APPROVAL_STEP / APPROVER_USER_CD / APPROVAL_STATUS / APPROVAL_COMMENT / APPROVAL_DATE`** (`tb_user_attd_req_approval` 실재, REQ_ID→tb_user_attd_req, 현재 연차만 사용) |
| 결재단계 상태값 (§1.D) | `'01'대기/'02'승인/'03'반려` | **SYS044: `00`대기 / `01`신청(차례도래) / `02`승인 / `03`반려** (연차와 동일) |
| 프리셋 USE_TYPE (§1.B/§3/§6) | `tb_aprv_line_preset.USE_TYPE='ATTD_REQ'` 로 분류 | **USE_TYPE 컬럼 없음.** 프리셋은 용도 구분 없이 사용자별 다중 보유. → **연차와 프리셋 풀 공유**(아래 D1). |
| SYS045 PUSH 타입 | `ATTD_APPROVAL_TURN` 신규 | 현재 `LEAVE_APPROVAL_TURN` 만 존재. 근태용 신규 코드 필요(D5). |

### 8.1 확정 결정

| # | 결정 | 내용 |
|---|---|---|
| **D1** | 프리셋 공유 (Q1.B / 본문 §1.B) | **연차와 동일 프리셋 풀 공유**(b안). `tb_aprv_line_preset` 에 USE_TYPE 컬럼 **추가하지 않음**, 마이그 없음. 결재자 풀이 사실상 동일하므로 분리 불필요. |
| **D2** | 결재 분기 기준 | 신청자 소속 노드 `tb_site_node.SELF_ATTD_APPRV_YN` 으로 분기. ⚠️ **이 컬럼은 연차에서는 "본인 결재자 자동승인 자격"** 의미이나, **근태에서는 아래 D3/D4 의미로 사용**(도메인 간 의미 상이 — 정책서에 명시). |
| **D3** | `'Y'`(자체근태승인) 케이스 | 결재선 **미사용**. 현행 단일승인(`REQ_STATUS '01'→'02'`) 유지하되, **승인 가능자 = 그 노드 Main/Sub 관리자**로 권한 게이팅. **둘 중 1인이라도 승인하면 완료(OR 승인)** — 단일 전이라 자연 성립. 신청 측(§1.D) 결재선 INSERT **안 함**(tb_user_attd_req 만 INSERT). 승인자(Main+Sub)에게 "승인 요망" PUSH. |
| **D4** | `'Y'` + 신청자가 그 노드 Main/Sub 관리자 | **즉시 자동 승인**(정책 §9.5 자기 승인 원칙). PUSH 미발송. |
| **D5** | `'Y'` + 노드 Main/Sub 관리자 0명 | **설정오류 에러**(폴백 없음). master/hr 폴백 **하지 않음** — prafta-046 이 "관리자 없는 노드에 근로자 배정"을 구조적으로 차단하므로 정상 흐름에선 발생 불가. 만약을 위한 최소 방어로 `ATTD_4xx` throw. |
| **D6** | `'N'` 케이스 | 결재선(다단계). 신청 측 §1.A~D(앱 폼 결재자 지정 → `tb_user_attd_req_approval` 다중행 INSERT). **웹 승인 다단계화는 본 작업 범위 밖 → `prafta-app-020`**. 차례도래 PUSH 도 app-020. |
| **D7** | `'N'` 자기 승인 자격 (Q4) | 연차 패턴 미러: `selectUserNodeSelfApproveYn` 으로 본인 결재자 단계 자동승인('Y'면 `02`, 'N'이면 본인 지정 불가 `ATTD_400_056` 등가). |
| **D8** | 결재자 스코프 가드 | 연차 web `countValidApprovers` 미러(CMPNY+SITE+재직+활성). 클라 결재자 주입 차단(cross-tenant PII 누수 방지). |
| **D9** | SYS045 PUSH 코드 | 근태 차례도래 코드 신규(D6 PUSH는 app-020). 연차 `LEAVE_APPROVAL_TURN` 과 통합한 범용 `APPROVAL_TURN` vs 신규 `ATTD_APPROVAL_TURN` — **planner 가 app-020 과 일치시켜 결정**(중복 정의 금지). |

### 8.2 선행/범위 경계

- **선행**: `prafta-046`(노드–관리자 정합성 가드, web/user). D5 의 전제. **app-009 착수 전 또는 병행**하되, app-009 `'Y'` 케이스 동작 검증 전 완료 권장.
- **범위 밖(분리)**: `'N'` 웹 승인 다단계화 + 차례도래 PUSH → `prafta-app-020`.
- **본 작업(app-009) 범위**: ① 결재선 신청 INSERT(`'N'` 케이스, §1.A~D) ② 프리셋 CRUD+사용자검색 앱 endpoint(§1.B, 공유 풀) ③ `'Y'`/`'N'` 분기 + `'Y'` 단일승인 노드관리자 게이팅(D3/D4/D5) ④ 앱 폼 결재선 UI(§1.A·C).
- **F12~F17**: High 가드(F12 마감/F13 스케줄존재/F14 NODE_CD IDOR/F15 race)는 planner 가 본 작업과 묶을지 분리 결정(Q8). 권장: 함께.

### 8.3 미해결/주의

- **PUSH 실도달 0**: `tb_user_device` PUSH_TOKEN 0건 + 워커 게이트 OFF → `'Y'` 승인요망 PUSH 도 outbox 적재까지만. 실발송은 앱 토큰등록+게이트 ON 별도 선행(prafta-com-002 참조).
- **D2 의미 상이**: `SELF_ATTD_APPRV_YN` 이 연차/근태에서 다른 의미 → 정책서(`attd/09` 또는 노드 관리)에 1줄 명시 필요.
