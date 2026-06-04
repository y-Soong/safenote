# prafta-app-009 — 근태 요청 결재선 통합 + `'Y'`/`'N'` 분기 + 차례도래 PUSH 분해 plan

> **작업 ID prefix**: `PRAFTA-APP-009`
> **단일 출처(SSOT) 선언**: 본 plan 이 prafta-app-009 의 단일 출처다. 후속 developer / qa / security 는 본 plan 만 정독하면 된다.
> **상위 요청서**: `.claude/requests/app_requests/prafta-app-009.md` (특히 §8 분해 확정 결정 — 본 plan 의 D1~D9 근거). 본문 §1~§7 의 일부 가정은 §8.0 실측으로 정정되었으니 §8 이 우선이며, 본 plan 은 그 §8 위에 코드/스키마 실측을 추가 반영했다.
> **선행(별도 요청서, 여기서 분해 안 함)**: `prafta-046`(노드–관리자 정합성 가드). D5 의 전제.
> **분리(별도 요청서, 여기서 분해 안 함)**: `prafta-app-020`(`'N'` 웹 승인 다단계화 + 차례도래 PUSH). 본 plan 은 **신청 측만** 담당.
> **선행 1차(완료)**: `prafta-app-007`(결재선 미포함 등록 폼 3종). 본 작업이 확장. 완료 plan = `.claude/requests/app_requests/완료/prafta-app-007-plan.md`.

---

## 0. 코드/스키마 실측 정정 (분해 전 확인 완료 2026-06-04)

요청서 §8.0 의 실측을 코드 레벨로 재확인하고, **추가로 발견한 "이미 구현된 자산"** 을 반영한다. 이 발견들이 분해 규모를 크게 줄인다.

### 0.1 결재 인프라 컬럼/상태 (실측 = 연차와 100% 동일)

| 항목 | 실제 (확인 경로) |
|---|---|
| 결재단계 테이블 | `TB_USER_ATTD_REQ_APPROVAL` — 컬럼 `REQ_ID / APPROVAL_STEP / CMPNY_CD / APPROVER_USER_CD / APPROVAL_STATUS / APPROVAL_COMMENT / APPROVAL_DATE / INSERT_NO / INSERT_DATE / UPDATE_NO / UPDATE_DATE` (`ApprovalLineMapper.xml` insertApprovalStep 실측). **REQ_ID → tb_user_attd_req**, 다목적 설계. 현재 연차만 사용. |
| 결재단계 상태값 [SYS044] | `00`대기 / `01`신청(차례도래) / `02`승인 / `03`반려 (`ApprovalStepVO` 주석 + `selectFirstWaitingStep` SQL `APPROVAL_STATUS='00'` 실측). |
| 공통 결재 매퍼 | `com.prafta.common.cmm.approval.mapper.ApprovalLineMapper` — `insertApprovalStep` / `updateStepStatus` / `selectFirstWaitingStep` / `selectApprovalStep` / `selectApprovalLineByReqId`. **REQ_ID 기반이라 근태 REQ_ID 에 그대로 적용 가능** (연차 전용 가정 SQL 없음 — INSERT/SELECT 모두 REQ_ID·CMPNY_CD 스코프만). |
| 결재단계 VO | `com.prafta.common.cmm.approval.vo.ApprovalStepVO` (그대로 재사용). |

### 0.2 프리셋 풀 (D1 = 연차와 공유, USE_TYPE 없음 — 실측 확정)

- `TB_APRV_LINE_PRESET (CMPNY_CD, PRESET_ID, USER_CD, PRESET_NM, DEFAULT_YN, USE_YN, INSERT_*, UPDATE_*)` — **USE_TYPE 컬럼 없음** (`User04Mapper.xml` selectPresetMasters/insertPresetMaster 실측). 용도 구분 없이 사용자별 다중 보유.
- `TB_APRV_LINE_PRESET_D (CMPNY_CD, PRESET_ID, STEP_NO, APPROVER_USER_CD, INSERT_*)`.
- → **D1 확정: 연차와 프리셋 풀 공유. 마이그 없음. USE_TYPE 추가 안 함.**

### 0.3 ⚠️ 핵심 발견 — 앱 프리셋 CRUD 5종 + 결재자검색은 이미 구현되어 있다

요청서 §1.B 는 `/appApi/req09/aprv-line-presets` 5종 + `/users/search` 신규 endpoint 를 요구하지만, **연차(prafta-app-018) 가 이미 앱에 동일 기능을 구현**해 두었다. D1(풀 공유)이므로 **신규 endpoint 를 만들지 않고 기존을 재사용**한다.

| 기능 | 이미 존재하는 앱 endpoint | 위치 |
|---|---|---|
| 프리셋 목록 + 스텝 | `GET /appApi/mypage01/approval-presets` / `GET /appApi/leaveflow/approval-presets` | `AppMypage01Controller` / `AppLeaveFlowController` (둘 다 `AppMypage01Mapper` 재사용) |
| 프리셋 단건 상세 | `GET /appApi/mypage01/approval-presets/{presetId}` | `AppMypage01Controller#getPreset` |
| 프리셋 저장(신규/수정) | `POST /appApi/mypage01/approval-presets` | `AppMypage01Controller#savePreset` |
| 프리셋 삭제 | `POST /appApi/mypage01/approval-presets/delete` | `AppMypage01Controller#deletePreset` |
| 프리셋 기본지정 | `POST /appApi/mypage01/approval-presets/set-default` | `AppMypage01Controller#setDefaultPreset` |
| 결재자 후보 검색(사업장 스코프·페이징) | `GET /appApi/leaveflow/approver-search?keyword=&page=&size=` | `AppLeaveFlowController#searchApprovers` |
| 결재자 후보(노드/이름) | `GET /appApi/mypage01/approval-candidates` | `AppMypage01Controller` |

→ **결론: §1.B "프리셋 CRUD 5종 + 사용자검색 1종 신규 endpoint" 는 신규 작업 단위에서 제외**한다(이미 구현됨, D1 풀 공유로 동일). 프론트는 이 기존 endpoint 를 그대로 호출한다. 본 plan 의 백엔드는 **결재선 신청 INSERT(`'N'`) + `'Y'`/`'N'` 분기 + `'Y'` 단일승인 게이팅 + PUSH** 에 집중한다.

### 0.4 ⚠️ 핵심 발견 — 신청 측 결재선 INSERT 흐름의 완성형 미러가 이미 있다

`com.prafta.app.leave.leaveflow.service.impl.AppLeaveFlowServiceImpl#submitLeave`(229~433)가 **앱 신청 폼에서 결재자 지정 → tb_user_attd_req_approval 다중행 INSERT + 자기승인 자동처리 + 결재자 스코프 가드 + 차례도래 PUSH** 를 이미 구현했다. app-009 §1.A~D 는 이 흐름을 **근태/스케줄수정/OT 등록 경로에 미러**하는 것이다(신규 발명 0).

미러 대상 핵심 블록(submitLeave 기준):
- 결재자 결정: `resolveApprovers`(approverUserCds 1차, 비면 presetId 전개 via `selectPresetStepsById`).
- 스코프 가드: `countValidApprovers`(distinct = valid 아니면 `COMMON_400_001`) — **D8**.
- 자기승인 자격: `selectUserNodeSelfApproveYn`(본인+노드 자체근태승인+노드관리자) → 자기 단계 `02`, 자격 미달이면 `ATTD_400_056` — **D7**.
- 라인 INSERT: 첫 수동단계 `01`(STEP_APPLIED), 나머지 `00`(STEP_WAIT), 본인 `02`(STEP_APPROVED).
- PUSH: `notifyApprovalTurn`(차례도래 결재자 1인, 예외 격리 try-catch) — **D9**.

### 0.5 F14(NODE_CD IDOR)는 app-007 후속에서 이미 해소됨

`OvertimeParam.from`(44~47) 등 3 Param 은 이미 `nodeCd = tokenInfo.gv_nodeCd()` 로 **JWT 도출**한다(바디 무시, IDOR-safe). 메모리 `project_prafta_app_req07_token_logout_bug` 와 정합. → **F14 는 사실상 완료. 본 plan 은 "재확인(qa/security)"만 한다**(신규 코드 작업 단위 아님). 단 3 Param 모두 동일한지 분해 시 점검 항목으로 남긴다(아래 PRAFTA-APP-009-9).

### 0.6 D5 의 'Y' 케이스 단일승인 게이팅 — 신규 가드 위치

`'Y'` 단일승인의 "노드 Main/Sub 관리자만 승인" 게이팅은 **승인 경로(웹 Attd07ServiceImpl)** 에 들어가야 의미가 있다. 그런데 웹 승인 다단계화는 app-020 범위다. 본 plan 의 `'Y'` 게이팅은 **"신청 시점에 결재선을 INSERT 하지 않는다"(tb_user_attd_req 만 INSERT)는 분기 + 승인요망 PUSH 적재** 까지로 한정하고, **승인자 OR 게이팅(노드관리자 1인 승인=완료)의 권한 체크 자체는 app-020(웹 승인 분기)이 결재라인 존재 여부로 분기**하면서 함께 처리한다(app-020 §0.0 "결재라인 존재 여부로 분기"). 본 plan 은 그 경계를 명시하고, `'Y'` 승인요망 PUSH(노드관리자 대상)만 신청 hook 에 추가한다.

> ⚠️ **경계 주의**: `'Y'` 케이스에서 "노드관리자만 승인 가능" 의 **서버 권한 강제**는 app-020(승인 메서드 게이팅)에서 최종 구현된다. app-009 단독 상태에서는 `'Y'` 요청도 현행 단일승인 경로(누구든 관리자 화면 접근자가 승인)로 처리될 수 있다. 이는 app-020 완료 전까지의 알려진 갭이며, app-009 의 책임은 (a) 결재선 미INSERT 분기 (b) 승인요망 PUSH 적재 (c) 신청자=노드관리자 시 즉시 자동승인(D4) 이다.

---

## 1. 확정 결정 (D1~D9) + 미결정 포인트(Q1~Q8) 분해 결정

### 1.1 요청서 §8.1 확정 결정 (그대로 적용)

| # | 결정 | 본 plan 적용 |
|---|---|---|
| **D1** | 프리셋 풀 연차와 공유, USE_TYPE 없음, 마이그 없음 | §0.2/§0.3 — 기존 mypage01/leaveflow 프리셋·검색 endpoint 재사용. 신규 프리셋 endpoint 0건. |
| **D2** | 노드 `tb_site_node.SELF_ATTD_APPRV_YN` 으로 분기. 근태에서의 의미는 D3/D4 (연차와 의미 상이 → 정책서 명시) | `selectAttdSelfApprvYn(cmpny, site, userCd)` 신규 mapper(신청자 소속 노드의 `SELF_ATTD_APPRV_YN` 단순 조회. ⚠️ 연차의 `selectUserNodeSelfApproveYn` 는 "본인이 노드관리자일 때만 Y" 라 의미가 달라 **재사용 불가** — 신규). |
| **D3** | `'Y'`(자체근태승인): 결재선 미사용. 현행 단일승인 유지 + 승인자=노드 Main/Sub 관리자 게이팅(OR 승인). 신청 측 결재선 INSERT 안 함. 승인자에게 "승인 요망" PUSH | 신청 hook: 결재라인 미INSERT + `notifyAttdApprovalRequest`(노드관리자 대상 PUSH). 승인자 OR 게이팅의 **권한 강제는 app-020** (§0.6 경계). |
| **D4** | `'Y'` + 신청자가 그 노드 Main/Sub 관리자 → 즉시 자동승인(§9.5). PUSH 미발송 | 신청 시 `selectIsNodeAdmin(cmpny, site, userCd)` Y면 `REQ_STATUS='02'` 즉시 + PUSH skip. |
| **D5** | `'Y'` + 노드 Main/Sub 관리자 0명 → 설정오류 에러(폴백 없음). prafta-046 이 구조 차단 | 신청 시 `'Y'` 인데 `selectNodeAdmins` 결과 0건이면 `ATTD_400_105`(신규) throw. (정상 흐름 미발생, 최소 방어.) |
| **D6** | `'N'`: 결재선 다단계. 신청 측 §1.A~D. 웹 승인 다단계화는 app-020 | 본 plan 핵심. submitLeave 미러로 결재라인 INSERT. |
| **D7** | `'N'` 자기승인 자격: `selectUserNodeSelfApproveYn` 미러. 'Y'면 자기단계 `02`, 'N'이면 본인 지정 불가 `ATTD_400_056` | submitLeave 패턴 그대로(연차 `selectUserNodeSelfApproveYn` 재사용 — 이건 "본인이 노드관리자+자체근태승인" 의미라 D7 의도와 일치하므로 재사용 OK). |
| **D8** | 결재자 스코프 가드: `countValidApprovers`(CMPNY+SITE+재직+활성) 미러. 클라 주입 차단 | `AppLeaveFlowMapper.countValidApprovers` 와 동일 SQL 의 근태 전용 매퍼(또는 공용화). cross-tenant PII 누수 방지. |
| **D9** | SYS045 PUSH 코드: 근태 차례도래 신규. app-020 과 일치 | 아래 Q-신규 결정 참조. |

### 1.2 미결정 포인트 분해 결정 (요청서 §5 Q1~Q8 — 합리적 기본값 + 근거)

| # | 결정 포인트 | **planner 결정** | 근거 |
|---|---|---|---|
| **Q1** | 결재선 UX 인라인 vs 2단 | **인라인(1화면)** — 폼 하단 "결재선" 섹션 | 연차 `LeaveApplyForm.vue` 가 이미 인라인 결재선 섹션을 채택해 동작 중. 일관성 + 단순. |
| **Q2** | 프리셋 자동 적용 우선순위 | **자동선택 안 함** (사용자 명시 선택) | 연차 폼도 자동선택 안 함(`onSelectType` 시 결재선 비움). DEFAULT_YN='Y' 는 "기본" 배지로 노출만. 자동 전개는 잘못된 결재선 무자각 제출 위험. |
| **Q3** | 결재자 검색 방식 | **이름 키워드 검색**(`approver-search?keyword=`) | 기존 `AppLeaveFlowController#searchApprovers` 가 keyword 검색+페이징으로 구현됨. 부서 트리는 앱 미존재(신규 비용). |
| **Q4** | 자기승인 자동처리 범위 | **본인이 등장하는 모든 단계 자동승인** | submitLeave 패턴(루프에서 `isSelf` 단계마다 `02`). §9.5 자기승인 원칙. |
| **Q5** | 결재 단계 최대 수 | **무제한(서버 검증 없음)** — 단 UI 는 추가 버튼 노출 | 정책서 명시 없음. 시안 §5-4(4단)는 예시. DB 제약 없음. 과도 입력은 사용자 책임(연차도 무제한). 단 클라 0단계는 제출 차단. |
| **Q6** | 결재선이 비어 있을 때(`'N'`) | **제출 거부**(`COMMON_400_001`) | submitLeave 도 비면 거부. 자동 채움은 노드 담당 정 조회 추가 로직 — 1차 과잉. `'N'` 인데 결재자 0명은 사용자 입력 오류. |
| **Q7** | HR 최종 ON 표기 | **미적용**(배지/자동추가 안 함) | 정책서에 "HR 최종 ON" 강제 규칙 부재(요청서 §1.A "선택적"). 연차 폼도 HR 배지 없음. 임의 추가 금지(planner 금지사항). |
| **Q8** | F1~F17 묶음 범위 | **결재선(A·B·C·D) + High 가드 F12·F13·F15 + D-PUSH 만 1차**. F14 는 재확인만(§0.5). F2~F11 Medium/Low 는 분리(별도 follow-up, 본 plan 비포함) | 요청서 §8.2 권장(High 함께). F2~F11 은 결재선과 무관 UX. |

### 1.3 D9 신규 결정 — SYS045 PUSH 코드 (app-020 과 일치)

- **결정**: 근태 차례도래 = **`ATTD_APPROVAL_TURN`**(신규), `'Y'` 승인요망 = **`ATTD_APPROVAL_REQUEST`**(신규). 연차 `LEAVE_APPROVAL_TURN` 과 **통합 범용 코드 만들지 않음**(범용화는 메시지 템플릿 분기 복잡도↑, 연차 상수 `LeaveApprovalNotiConst` 재사용 시 결합도↑).
- **근거**: app-020 §1.4 가 "`ATTD_APPROVAL_TURN` 신규 또는 범용" 중 택일을 app-009 에 위임. 도메인별 분리 코드가 추적/필터 용이. app-020 은 본 결정(`ATTD_APPROVAL_TURN`)을 그대로 사용한다(차례도래는 app-020 승인 경로에서 발화).
- **dedupKey**: 차례도래 = `"ATTD_TURN_" + reqId + "_" + approvalStep` / 승인요망 = `"ATTD_REQ_" + reqId + "_" + targetUserCd` (요청서 §1.E `"REQ_TURN_..."` 와 다르게 도메인 prefix 명확화 — app-020 도 본 규약 사용).
- **신규 상수 클래스**: `com.prafta.app.req.req09.AttdApprovalNotiConst`(또는 공용 `com.prafta.common.cmm.approval`)에 SYS045 코드/제목/본문 템플릿 정의. 연차 `LeaveApprovalNotiConst` 미러.
- **마이그**: SYS045 `tb_syst_val_d` 에 `ATTD_APPROVAL_TURN` / `ATTD_APPROVAL_REQUEST` 2행 INSERT (1건). app-020 과 공유(중복 INSERT 금지 — 본 plan 의 마이그가 단일 출처, app-020 은 재사용).

> ⚠️ **PUSH 실도달 한계(요청서 §8.3)**: `tb_user_device` PUSH_TOKEN 0건 + 워커 게이트 OFF → `'Y'` 승인요망/`'N'` 차례도래 PUSH 모두 **outbox 적재까지만** 동작. 실단말 도달은 앱 FCM 토큰등록 + prafta-com-002 게이트 ON 별도 선행. 본 plan 은 적재(생산자)만 책임.

---

## 2. 작업 단위 분해

### 2.1 작업표

| 작업 ID | 유형 | 영역 | 모듈 | 작업유형 | 요구사항 요약 | 우선순위 |
|---|---|---|---|---|---|---|
| **PRAFTA-APP-009-1** | backend | app | req/req09 | 신규 | 마이그 1건: SYS045 `ATTD_APPROVAL_TURN` / `ATTD_APPROVAL_REQUEST` 2행 INSERT + AttdErrorCode 신규(`ATTD_400_105`). app-020 공유. | P2 |
| **PRAFTA-APP-009-2** | backend | app | req/req09 | 신규 | 근태 결재 PUSH 생산자 `AttdApprovalNotiService`(+Impl) — 차례도래 1인 적재 + `'Y'` 승인요망 노드관리자 적재. 연차 `LeaveApprovalNotiService` 미러. outbox 재사용. | P2(법적책임 attd +1 → **P1**) |
| **PRAFTA-APP-009-3** | backend | app | req/req09 | 신규 | 결재 분기/라인 INSERT 공용 서비스 `AttdApprovalLineService`(+Impl) — `SELF_ATTD_APPRV_YN` 분기(D2~D7), `'N'` 결재라인 INSERT(submitLeave 미러), 자기승인·스코프가드, `'Y'` 분기(미INSERT/즉시승인/설정오류). | **P1** |
| **PRAFTA-APP-009-4** | backend | app | req/req07 | 보완 | `AppReq07ServiceImpl` 3 register 메서드의 `TODO(prafta-app-009)` 마커(114/214/311줄)에서 009-3 서비스 호출 + 3 Param/Request 에 `approverUserCds`/`presetId` 필드 추가. | **P1** |
| **PRAFTA-APP-009-5** | backend | app | req/req07 | 보완 | High 가드 F12(마감)·F13(스케줄존재) 3 register 메서드 추가 — `ATTD_400_099`/`ATTD_400_098`(enum 기존) throw. `AttdCloseService`/`selectScheduleByYmd` 재사용. | **P1**(정합성) |
| **PRAFTA-APP-009-6** | frontend-component | app | req | 신규 | `ApprovalLineSection.vue` — 연차 `LeaveApplyForm.vue` 결재선 섹션(프리셋 칩 + 결재자 리스트 + 추가/삭제) 추출 재사용 컴포넌트. `[UI 명세: UI-009-1]` | P3 |
| **PRAFTA-APP-009-7** | frontend-component | app | req | 신규 | `AttdApproverPickerSheet.vue` — 결재자 검색 바텀시트(`approver-search` 호출). 연차 `LeaveApproverPickerSheet.vue` 미러. `[UI 명세: UI-009-2]` | P3 |
| **PRAFTA-APP-009-8** | frontend-component | app | req | 보완 | `SchedModifyForm`·`AttdCorrectionForm`·`OvertimeForm` 3 폼에 `ApprovalLineSection` 섹션 추가 + emit payload 에 `approverUserCds`/`presetId` 포함. 결재 불필요(`'Y'`/즉시승인) 분기 표시. `[UI 명세: UI-009-3]` | P3 |
| **PRAFTA-APP-009-9** | backend | app | req/req07 | 보완 | F15(race) 가드 + F14(NODE_CD IDOR) 3 Param 재확인. F15: 중복차단 SELECT→INSERT race window 에 advisory lock 또는 UNIQUE 인덱스(마이그 1건). | P2 |

> **착수 권장 순서**: 1 → 2 → 3 → 4(3 의존) → 5 → 9 (백엔드). FE 는 6 → 7 → 8 (8 은 6·7 의존, BE 응답 계약 확정 후). BE/FE 병렬 가능(계약은 §3 으로 고정).

> **F12~F17 처리**: F12·F13(009-5), F14(009-9 재확인), F15(009-9). F16(마이그 순서 가이드)는 009-1 마이그 SQL 헤더 주석으로 흡수. F17(`day.siteName`)는 app-007 시점 미검증 항목 — 009-8 FE 작업에서 폼 컨텍스트 표시 점검(부재 시 BE day-detail DTO 보강은 별도 follow-up, 본 plan 비포함).

### 2.2 상세 설명 (Notion "작업 로그" 상세 설명 칸 그대로)

#### PRAFTA-APP-009-1 (마이그 + 에러코드)

```
[backend / 마이그레이션]

[정책 근거]
- attd/09-requests-approval.md §9.5 (자기 승인 원칙 / 알림)
- common/10-notifications.md (알림 채널 — outbox)

[확정결정] D5, D9 (요청서 §8.1)

[핵심 요구사항]
1) tb_syst_val_d SYS045 INSERT 2행: 'ATTD_APPROVAL_TURN'(근태 결재 차례 도래) / 'ATTD_APPROVAL_REQUEST'('Y' 단일승인 승인요망). app-020 과 공유(중복 INSERT 금지 — 본 마이그가 단일 출처).
2) AttdErrorCode 신규 ATTD_400_105 "승인 가능한 부서 관리자가 지정되어 있지 않습니다. 관리자에게 문의해 주세요." (D5 'Y' 노드관리자 0명 설정오류).
3) 운영 적용 전 부재 확인 쿼리 주석(SELECT COUNT(*) FROM tb_syst_val_d WHERE SYST_VAL_CD='SYS045' AND SYST_VAL_D_CD IN(...)).
4) F16: 마이그 헤더에 운영 적용 순서 1줄 — ① 마이그 SQL → ② 백엔드 배포 → ③ Flutter APK 재빌드.

[영향 받는 파일]
- (신규) prafta-backend/src/main/resources/sql/migration/prafta-app-009-attd-approval-noti.sql
- (보완) com.prafta.common.error.attd.AttdErrorCode (ATTD_400_105 추가)

[비범위]
- 운영 DB 직접 실행 금지(사용자 수동).
- SYS044(결재단계상태)는 기존 — 추가 안 함.
```

#### PRAFTA-APP-009-2 (근태 결재 PUSH 생산자)

```
[backend]

[정책 근거]
- attd/09-requests-approval.md §9.5 (처리 결과 알림)
- common/08-permissions.md §8.4 (노드 관리자 = 결재/승인 권한자 스코프)
- common/10-notifications.md (outbox 채널/상태)

[확정결정] D3, D4, D9 (요청서 §8.1) + 본 plan §1.3

[핵심 요구사항]
1) 신규 com.prafta.app.req.req09.service.AttdApprovalNotiService(+impl) — 연차 LeaveApprovalNotiService 미러. @Transactional 미부여, 내부 try-catch 예외 흡수(신청 본 흐름 영향 금지).
2) notifyAttdApprovalTurn(cmpny, site, applicantUserCd, reqId, approvalStep, approverUserCd, insertNo):
   - 'N' 결재라인 첫 수동단계 차례도래 1인 적재. NOTI_TYPE='ATTD_APPROVAL_TURN'.
   - dedupKey = "ATTD_TURN_" + reqId + "_" + approvalStep.
   - 제목/본문 = AttdApprovalNotiConst (신규 상수, LeaveApprovalNotiConst 미러: "[근태 결재 요청]" / "%s님이 신청한 근태 결재를 기다리고 있습니다.").
3) notifyAttdApprovalRequest(cmpny, site, applicantUserCd, reqId, insertNo):
   - 'Y' 단일승인 — 신청자 소속 노드 Main/Sub 관리자 전원 적재(자기 제외). NOTI_TYPE='ATTD_APPROVAL_REQUEST'.
   - dedupKey = "ATTD_REQ_" + reqId + "_" + targetUserCd. DuplicateKeyException 흡수(멱등).
   - 대상 = LeaveApprovalNotiMapper.selectNodeAdmins(cmpny, site, applicantUserCd) 재사용(노드 Main∪Sub, 활성, master/hr 미포함).
4) outbox 적재 = LeaveDashboardMapper.insertNotiOutbox + selectNextNotiId 재사용(신규 SQL 금지, prafta-031 인프라).
5) DATA_PAYLOAD 는 라우팅 키만(reqId/approvalStep/applicantUserCd), 평문 PII 미포함. 본문 신청자명은 평문 USER_NM(selectUserNm 재사용, 복호화 호출 금지).

[영향 받는 파일]
- (신규) com.prafta.app.req.req09.service.AttdApprovalNotiService + impl/AttdApprovalNotiServiceImpl
- (신규) com.prafta.app.req.req09.AttdApprovalNotiConst
- (재사용) com.prafta.common.cmm.leave.mapper.LeaveApprovalNotiMapper#selectNodeAdmins / #selectUserNm
- (재사용) com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper#insertNotiOutbox / #selectNextNotiId
- (재사용) com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO

[비범위]
- 'N' 다음 단계 진행 PUSH(승인 경로) → prafta-app-020.
- 실제 FCM 발송 → prafta-com-002 워커.
- master/hr 자동 포함 없음(노드 관리자만 — D3/연차 시나리오 B 동일).
```

#### PRAFTA-APP-009-3 (결재 분기 + 라인 INSERT 공용 서비스)

```
[backend]

[정책 근거]
- attd/09-requests-approval.md §9.5 (자기 승인 원칙)
- request-approval/06-approval-flows.md §6.1~§6.4 (결재 플로우)
- request-approval/09-data-structures.md §9.1 (결재단계 필드)
- common/08-permissions.md §8.4 (결재자 조직 스코프)

[확정결정] D2, D3, D4, D5, D6, D7, D8 (요청서 §8.1)
[참조 미러] com.prafta.app.leave.leaveflow.service.impl.AppLeaveFlowServiceImpl#submitLeave (229~433, 특히 336~392 결재라인 생성)

[핵심 요구사항]
1) 신규 com.prafta.app.req.req09.service.AttdApprovalLineService(+impl). req07 의 3 register 메서드가 INSERT(tb_user_attd_req) 직후 본 서비스를 호출하여 결재 분기/라인 처리. 동일 @Transactional 안에서 실행(req07 트랜잭션에 참여).
2) 입력: cmpnyCd, siteCd, userCd(신청자), reqId(대표 — 슬롯 다건이면 ⚠️아래 5 참조), approverUserCds[], presetId, 그리고 적재용 컨텍스트.
3) 분기 판정: selfApprvYn = selectAttdSelfApprvYn(cmpny, site, userCd) (신규 mapper — 신청자 소속 노드 SELF_ATTD_APPRV_YN 단순 조회. ⚠️연차 selectUserNodeSelfApproveYn 와 의미 달라 재사용 금지).
   - 'Y' 케이스(D3):
     a) selectIsNodeAdmin(cmpny, site, userCd) Y → 즉시 자동승인: tb_user_attd_req.REQ_STATUS='02' 갱신, 결재라인 미INSERT, PUSH 미발송 (D4).
     b) N(일반 근로자) → 결재라인 미INSERT. selectNodeAdmins 결과 0건이면 ATTD_400_105 throw (D5). 1건 이상이면 notifyAttdApprovalRequest 적재.
   - 'N' 케이스(D6): 결재선 다단계 — 아래 4.
4) 'N' 결재라인 생성 (submitLeave 336~392 미러):
   - approvers = resolveApprovers(approverUserCds 1차, 비면 presetId 전개 via AppMypage01Mapper.selectPresetStepsById — 소유자 스코프, 재사용).
   - 비어있으면 COMMON_400_001 (Q6).
   - distinctApprovers vs countValidApprovers(cmpny, site, distinct) 불일치 시 COMMON_400_001 (D8 — cross-tenant 차단).
   - selfAllowed = selectUserNodeSelfApproveYn(cmpny, userCd) (D7 — 본 케이스는 의미 일치하므로 연차 매퍼 재사용 OK).
   - 루프: isSelf && !selfAllowed → ATTD_400_056. isSelf → step '02'(자동승인). 첫 수동단계 '01'(STEP_APPLIED), 나머지 '00'. ApprovalLineMapper.insertApprovalStep.
   - 전 단계 본인 자동승인(fullyAutoApproved) → tb_user_attd_req.REQ_STATUS='02' 즉시.
   - 첫 수동단계 결재자 있으면 notifyAttdApprovalTurn 적재(예외 격리).
5) ⚠️ 슬롯 다건 REQ_ID 처리: req07 은 슬롯마다 새 REQ_ID 채번(insertAttdReq 루프, AppReq07ServiceImpl 88~109). 결재라인은 REQ_ID 단위. 결정: 결재라인은 "요청 그룹" 단위가 자연스러우나 현 구조는 REQ_ID=슬롯. → 1차 결정: 각 REQ_ID(슬롯)마다 동일 결재선 INSERT(슬롯 2건이면 라인 2벌). PUSH dedupKey 가 reqId 포함이라 슬롯별 1건씩 적재(중복 아님). (대안: 슬롯 묶음 단일 REQ_ID 화는 req07 INSERT 구조 변경 → 범위 과대, 보류.) developer 는 이 결정을 따르되, 슬롯 2건+'N' 시 결재함에 2건 표시되는 UX 영향을 qa 와 공유.

[영향 받는 파일]
- (신규) com.prafta.app.req.req09.service.AttdApprovalLineService + impl/AttdApprovalLineServiceImpl
- (신규) com.prafta.app.req.req09.mapper.AppReq09Mapper#selectAttdSelfApprvYn + #selectIsNodeAdmin (+ xml)
- (재사용) com.prafta.common.cmm.approval.mapper.ApprovalLineMapper (insert/selectFirstWaitingStep/updateStepStatus)
- (재사용) com.prafta.common.cmm.approval.vo.ApprovalStepVO
- (재사용) com.prafta.app.mypage.mypage01.mapper.AppMypage01Mapper#selectPresetStepsById
- (재사용/신규) countValidApprovers + selectUserNodeSelfApproveYn — AppLeaveFlowMapper 의 것을 공용화하거나 req09 매퍼에 동일 SQL 재정의(연차 매퍼 직접 의존은 app 모듈 간 결합 — 분해 시 결정. 권장: req09 매퍼에 동일 SQL 복제로 모듈 독립.)
- (재사용) tb_user_attd_req REQ_STATUS 갱신 매퍼 (req07 또는 신규 updateReqStatus)

[비범위]
- 'N' 웹 승인 다단계화/차례도래(승인측) → prafta-app-020.
- 'Y' 승인자 OR 게이팅 권한 강제 → prafta-app-020(승인 메서드). 본 작업은 신청측 분기/PUSH 만.
```

#### PRAFTA-APP-009-4 (req07 register 확장)

```
[backend / 보완]

[정책 근거] = 009-3 동일
[확정결정] D6, D7, D8

[핵심 요구사항]
1) AppReq07ServiceImpl 3 메서드(registerSchedModify 114줄 / registerAttdCorrection 214줄 / registerOvertime 311줄)의 TODO(prafta-app-009) 마커에서 AttdApprovalLineService 호출.
   - 호출 위치: tb_user_attd_req INSERT 루프 완료 직후, 같은 @Transactional 안.
   - 전달: cmpny/site/user, reqId(대표/슬롯별 — 009-3 §5 결정 따름), param.approverUserCds(), param.presetId().
2) 3 Request DTO(SchedModifyRequest/AttdCorrectionRequest/OvertimeRequest)에 List<String> approverUserCds, String presetId 필드 추가.
3) 3 Param(SchedModifyParam/AttdCorrectionParam/OvertimeParam)에 approverUserCds/presetId 전달(Param.from 에서 request → param 매핑). 식별값은 기존대로 JWT(IDOR 불변).
4) 기존 TODO(prafta-031) outbox 주석은 009-2/009-3 의 PUSH hook 으로 대체(결재 서비스 내부에서 적재).

[영향 받는 파일]
- (보완) com.prafta.app.req.req07.service.impl.AppReq07ServiceImpl (3 메서드 호출 추가)
- (보완) com.prafta.app.req.req07.dto.request.{SchedModify,AttdCorrection,Overtime}Request
- (보완) com.prafta.app.req.req07.application.param.{SchedModify,AttdCorrection,Overtime}Param

[비범위] 승인 측(app-020).
```

#### PRAFTA-APP-009-5 (High 가드 F12 마감 / F13 스케줄존재)

```
[backend / 보완]

[정책 근거]
- attd/12-schedule-close.md / attd/13-attendance-close.md (마감 가드)
- attd/09-requests-approval.md §9.2 (스케줄 존재 조건)
- request-approval/06-approval-flows.md §6.1~§6.3 (마감 후 차단)

[확정결정] 요청서 F12/F13 (§2 High)

[핵심 요구사항]
1) F12 마감 가드: 3 register 메서드(또는 009-3 진입부)에서 attdCloseService.isClosedForUser(cmpny, site, user, workYmd 앞6자) true 면 ATTD_400_099 throw (enum 기존). 연차 submitLeave 313~320 패턴 미러(과거 일자에만? — 스케줄수정은 미래도 마감대상이므로 일자 무관 적용. developer 가 정책 §12/§13 으로 확정).
2) F13 본인 스케줄 존재 가드: 스케줄수정(10)/근태보정 시 selectScheduleByYmd(또는 AppAttd01 canRequestScheduleModify 동등) 부재면 ATTD_400_098 throw. OT 는 실근태 가드(기존 104)로 충족 — 추가 안 함.
3) 가드는 INSERT 시작 전 fail-closed.

[영향 받는 파일]
- (보완) AppReq07ServiceImpl 3 메서드 (또는 009-3 공용 진입 가드)
- (재사용) com.prafta.web.attd.attd07.service.AttdCloseService#isClosedForUser
- (재사용/신규) 스케줄 존재 SELECT (AppAttd01Mapper.selectScheduleByYmd 또는 req07 신규)

[비범위] 마감 해제/보정 우회 흐름.
```

#### PRAFTA-APP-009-6 (ApprovalLineSection 컴포넌트)

```
[frontend-component]

[정책 근거]
- common/13-ui-ux.md §13.2 (모바일 터치 44px) / §13.3 (결재선 카드 인터랙션)
- common/08-permissions.md §8.4 (결재자 가시 범위 — 검색 endpoint 스코프)

[확정결정] Q1(인라인), Q2(자동선택 안 함), Q4(자기승인 표시), Q5(무제한)
[참조 패턴] views/leave/components/LeaveApplyForm.vue 의 "결재선" 섹션(171~267) 추출
[UI 명세: UI-009-1]

[핵심 요구사항]
1) props: presets(배열 [{presetId,presetNm,defaultYn,steps[]}]), submitting.
2) v-model: approverList([{approverUserCd,userNm,userId,rankNm,nodeNm}]) — 순서=결재 단계.
3) 구조: 프리셋 칩(기본 배지) + 결재자 순서 리스트(step 번호·이름·meta·삭제) + "결재자 추가" 버튼 + 빈 상태 안내.
4) emits: openPicker(부모가 AttdApproverPickerSheet 열기), update:modelValue.
5) 식별자(approverUserCd) 기반 dedup/삭제 — index 재인덱싱 금지(LeaveApplyForm 동일 주석).
6) 프리셋 선택 토글/직접추가 시 프리셋 이탈 표기.

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/ApprovalLineSection.vue

[비범위] API 호출(부모/시트). script 비즈니스 로직(developer).
```

#### PRAFTA-APP-009-7 (AttdApproverPickerSheet)

```
[frontend-component]

[정책 근거] common/08-permissions.md §8.4 (사업장 스코프 검색)
[참조 패턴] views/leave/components/LeaveApproverPickerSheet.vue (approver-search 호출 + BaseBottomSheet)
[UI 명세: UI-009-2]

[핵심 요구사항]
1) v-model open(bottomsheet), props: excludedUserCds[].
2) 검색 입력 → GET /appApi/leaveflow/approver-search?keyword=&page=&size= (기존 endpoint 재사용, D1 풀 공유).
3) 결과 리스트(이름/사번/직급/노드), 다중 선택, hasNext 페이징.
4) emit add(picked[] = [{userCd,userId,userNm,rankNm,nodeNm}]).
5) BaseBottomSheet 재사용(prafta-app-006 신설). 신규 시트 인프라 금지.

[영향 받는 파일]
- (신규) prafta-app-frontend/src/views/req/components/AttdApproverPickerSheet.vue

[비범위] 신규 검색 endpoint(기존 재사용).
```

#### PRAFTA-APP-009-8 (3 폼 결재선 섹션 통합)

```
[frontend-component / 보완]

[정책 근거] attd/09 §9.5 / common/13-ui-ux §13.3
[확정결정] Q1, D2~D7
[UI 명세: UI-009-3]

[핵심 요구사항]
1) SchedModifyForm/AttdCorrectionForm/OvertimeForm 에 ApprovalLineSection + AttdApproverPickerSheet 추가(사유 아래).
2) 결재 메타 조회: 폼 진입 시 GET /appApi/mypage01/approval-presets(또는 leaveflow/approval-presets)로 presets 로드. developer 가 부모(AttdRequestView) 또는 폼에서 호출.
3) emit submit payload 확장: 기존 { slots, reqReason } → { slots, reqReason, approverUserCds, presetId }.
   - presetId 는 미전송(approverUserCds 를 SSOT 로 전개 전송 — 연차 018-B 결정 정합). 단 백엔드는 presetId 도 허용(009-3 resolveApprovers).
4) 결재선 표시 분기: 'Y'/즉시승인 케이스는 결재선 섹션 숨김 또는 "관리자 승인" 안내(노드 SELF_ATTD_APPRV_YN 은 서버 권위 — 클라가 알 수 없으면 항상 결재선 노출하되, 비우고 제출 시 서버가 'Y' 분기로 무시 가능. ⚠️정합 주의: 'N' 인데 빈 결재선 제출 → 서버 COMMON_400_001. developer 는 결재선 필수 여부를 서버 메타로 받을지 결정 — 본 plan 권장: 폼 진입 시 GET 으로 selfApprvYn 메타 1건 받아 결재선 필수/숨김 분기. (신규 메타 endpoint 1건 필요 시 009-3 매퍼 재사용한 GET /appApi/req09/approval-context?workYmd= 추가 — developer 결정.)
5) F17: 컨텍스트 박스 siteName 표시 점검(부재 시 빈 문자열 — BE 보강은 별도 follow-up).

[영향 받는 파일]
- (보완) prafta-app-frontend/src/views/req/components/SchedModifyForm.vue
- (보완) prafta-app-frontend/src/views/req/components/AttdCorrectionForm.vue
- (보완) prafta-app-frontend/src/views/req/components/OvertimeForm.vue
- (보완 가능) prafta-app-frontend/src/views/req/AttdRequestView.vue (presets 로드/전달)

[비범위] 결재선 케이스 시안 §5 픽셀 직접 사용(토큰만). 승인 화면(app-020).
```

#### PRAFTA-APP-009-9 (F15 race + F14 재확인)

```
[backend / 보완]

[정책 근거] common/08-permissions.md §8.4 (IDOR) / attd/09 §9.5 (선점·정합)
[확정결정] 요청서 F14/F15 (§2 High)

[핵심 요구사항]
1) F15 race: 현 countDuplicateReq SELECT→INSERT 사이 race window 차단.
   - 옵션A(권장): tb_user_attd_req 에 (CMPNY_CD,SITE_CD,USER_CD,WORK_YMD,WORK_SEQ,REQ_TYPE) + REQ_STATUS='01' 조건 partial UNIQUE — MySQL 8 functional/generated 컬럼 인덱스(마이그 1건). PK 충돌 시 ATTD_400_090 변환.
   - 옵션B: GET_LOCK(키, timeout) advisory lock(트랜잭션 시작 시). 마이그 없음.
   - developer 가 인덱스 충돌 가능성(2구간 동일키) 검토 후 택1.
2) F14 재확인: 3 Param.from 이 모두 nodeCd=tokenInfo.gv_nodeCd() 인지 점검(OvertimeParam 은 확인됨). 누락 Param 있으면 동일 패턴 적용. (이미 해소면 코드 변경 없이 qa/security 확인 항목으로 종결.)

[영향 받는 파일]
- (신규 가능) prafta-backend/src/main/resources/sql/migration/prafta-app-009-attd-req-unique.sql (옵션A 채택 시)
- (점검/보완) com.prafta.app.req.req07.application.param.{SchedModify,AttdCorrection,Overtime}Param

[비범위] 선점(처리 잠금)은 승인 측(app-020 §1.5).
```

---

## 3. 백엔드↔프론트 계약 (고정 — 병렬 진행 기준)

### 3.1 등록 요청 body (3 endpoint 공통 확장)

```
POST /appApi/req07/sched-modify | /attd-correction | /overtime
{
  workYmd, nodeCd(서버 JWT 우선), slots:[...],   // 기존
  reqReason,                                     // 기존
  approverUserCds: ["U001","U002"],              // 신규 ('N' 결재선. 'Y'/즉시승인이면 무시/빈 허용)
  presetId: null                                 // 신규 (approverUserCds 비었을 때 폴백 전개)
}
```

### 3.2 결재 컨텍스트 메타 (009-8 분기용 — developer 결정 시 신설)

```
GET /appApi/req09/approval-context?workYmd=YYYYMMDD   (선택 — 009-3 매퍼 재사용)
→ { selfApprvYn: 'Y'|'N', isNodeAdmin: true|false }
   - selfApprvYn='Y' → 폼 결재선 섹션 숨김(또는 "관리자 승인" 안내).
   - selfApprvYn='N' → 결재선 필수.
   ⚠️ 없으면 폼은 항상 결재선 노출 + 서버 분기에 위임(빈 결재선 'N' 제출 시 COMMON_400_001 표면화).
```

### 3.3 프리셋/검색 (기존 재사용 — 신규 0)

- `GET /appApi/mypage01/approval-presets` → `{ presets:[{presetId,presetNm,defaultYn,steps:[{stepNo,approverUserCd,userNm,userId,rankNm,nodeNm}]}] }`
- `GET /appApi/leaveflow/approver-search?keyword=&page=&size=` → `{ approvers:[{userCd,userId,userNm,rankNm,nodeNm}], hasNext }`

---

## 4. 화면 명세

### UI-009-1 ApprovalLineSection

- 연결 작업: PRAFTA-APP-009-6
- 화면 위치: `src/views/req/components/ApprovalLineSection.vue`
- 참조 패턴: `views/leave/components/LeaveApplyForm.vue` 결재선 섹션(171~267, 스타일 867~998) — 동일 디자인 토큰/클래스 명명 차용.
- 레이아웃:
```
┌─ 결재선 ─────────────────────────────┐
│ [프리셋A 기본] [프리셋B] [프리셋C]     │  ← 프리셋 칩(가로 wrap, 선택 시 primary-tint)
│ ┌──────────────────────────────────┐ │
│ │ ① 김부장  생산1팀 · 부장      [×] │ │  ← 결재자 행(step 번호·이름·meta·삭제)
│ │ ② 이차장  생산1팀 · 차장      [×] │ │
│ └──────────────────────────────────┘ │
│        [ + 결재자 추가 ]               │  ← 대시 보더 버튼 → openPicker emit
└──────────────────────────────────────┘
(결재자 0명 시: "결재자를 추가해 주세요" 대시 박스)
```
- 컴포넌트 매핑:

| 영역 | 컴포넌트/요소 | 비고 |
|---|---|---|
| 프리셋 칩 | native `<button>` (연차 폼 동일, 신규 공통 컴포넌트 불요) | DEFAULT_YN='Y' → "기본" 태그 |
| 결재자 리스트 | `<ul>/<li>` | step = idx+1 표시(저장은 STEP_NO=배열순서) |
| 삭제 | 인라인 SVG(x) | approverUserCd 필터 |
| 추가 | 대시 보더 버튼 | `openPicker` emit → 부모가 시트 open |

- 상태별 동작: loading(프리셋 로딩 — 부모 책임, 섹션은 빈 칩) / empty(프리셋 0건 → 칩 영역 숨김, 추가 버튼만) / 결재자 0명(대시 안내) / 정상(리스트).
- 사용자 플로우: (프리셋 칩 탭 → steps 전개) 또는 (추가 버튼 → 시트 → 선택 append) → 삭제로 조정 → 부모 폼 submit 시 approverUserCds 전개 전송.
- 반응형: 360~414px webview. 칩 wrap, 터치 44px.
- 백엔드 의존: `GET /appApi/mypage01/approval-presets`(부모 로드, PRAFTA-APP-009-3/기존), submit 시 `POST /appApi/req07/*`(PRAFTA-APP-009-4).

### UI-009-2 AttdApproverPickerSheet

- 연결 작업: PRAFTA-APP-009-7
- 화면 위치: `src/views/req/components/AttdApproverPickerSheet.vue`
- 참조 패턴: `views/leave/components/LeaveApproverPickerSheet.vue` + `components/.../BaseBottomSheet.vue`
- 레이아웃:
```
┌─ 바텀시트 ───────────────────────────┐
│ 결재자 추가                      [×]  │
│ [🔍 이름으로 검색            ]        │
│ ┌──────────────────────────────────┐ │
│ │ ☐ 김부장  사번 1001 · 부장 · 생산1 │ │
│ │ ☑ 이차장  사번 1002 · 차장 · 생산1 │ │
│ └──────────────────────────────────┘ │
│ [ 더 보기 ]  (hasNext 시)            │
│ ┌──────────────────────────────────┐ │
│ │        [ 선택 추가 (N) ]          │ │  ← sticky
│ └──────────────────────────────────┘ │
└──────────────────────────────────────┘
```
- 컴포넌트 매핑:

| 영역 | 컴포넌트 | 비고 |
|---|---|---|
| 시트 셸 | `BaseBottomSheet` | 재사용(신규 금지) |
| 검색 | native `<input>` (디바운스) | keyword 쿼리 |
| 결과 | 체크 리스트 | excludedUserCds 제외 표시 |
| 페이징 | "더 보기" | hasNext |

- 상태별 동작: loading(스피너/스켈레톤) / empty("검색 결과가 없어요") / error("불러오지 못했어요. 다시 시도") / success(리스트).
- 사용자 플로우: 검색어 입력 → 결과 → 다중 체크 → "선택 추가" → emit add(picked[]) → 시트 닫힘.
- 백엔드 의존: `GET /appApi/leaveflow/approver-search?keyword=&page=&size=`(기존 재사용).

### UI-009-3 3 폼 결재선 통합 (보완)

- 연결 작업: PRAFTA-APP-009-8
- 화면 위치: `src/views/req/components/{SchedModifyForm,AttdCorrectionForm,OvertimeForm}.vue`
- 참조 패턴: `LeaveApplyForm.vue`(결재선 통합 완성형) / 각 폼 기존 구조(컨텍스트 박스 + 필드 + sticky 푸터).
- 현재 동작(변경 전): 3 폼 모두 결재선 없이 `{ slots, reqReason }` 만 emit(prafta-app-007).
- 의도된 동작:
```
[컨텍스트 박스]
[폼별 입력 — 스케줄/시각/OT]
[사유 textarea]
─────────────────────
[결재선]  ← ApprovalLineSection (selfApprvYn='N' 시 노출, 'Y' 시 안내문)
─────────────────────
[헬퍼 메시지]
[취소] [요청하기]   ← sticky
+ AttdApproverPickerSheet (조건부 mount)
```
- 컴포넌트 매핑:

| 영역 | 컴포넌트 |
|---|---|
| 결재선 | `ApprovalLineSection`(UI-009-1) |
| 결재자 추가 | `AttdApproverPickerSheet`(UI-009-2) |
| 기존 입력 | 폼별 기존(SchedPickSheet/DateStepperField/TimeStepperField 등) |

- 상태별 동작: selfApprvYn 미상(메타 미조회)이면 결재선 노출 + 서버 위임 / 'N' 결재자 0명 → 제출 버튼 비활성 또는 서버 400 표면화 / submitting → 버튼 "등록 중...".
- 사용자 플로우: (기존 입력) → 결재선 구성 → 요청하기 → 부모 API 호출 → 성공 alert + back.
- 백엔드 의존: `POST /appApi/req07/{sched-modify|attd-correction|overtime}`(확장, PRAFTA-APP-009-4), `GET /appApi/mypage01/approval-presets`, (선택)`GET /appApi/req09/approval-context`.

---

## 5. Vue 컴포넌트 골격

> 아래 골격은 developer 가 `<script setup>` 의 API 호출·비즈니스 로직·라우팅·store 영역을 채운다. template + style 은 연차 `LeaveApplyForm.vue`/`LeaveApproverPickerSheet.vue` 의 검증된 패턴/토큰을 차용했다. 하드코딩 색상/픽셀 없음(CSS 변수만, `.attd-req-view` 토큰 세트는 부모 `AttdRequestView` 가 선언 — 자식은 상속).

### 5.1 ApprovalLineSection.vue (PRAFTA-APP-009-6)

```vue
<!--
  ApprovalLineSection.vue — 근태 요청 결재선 섹션 (prafta-app-009, UI-009-1)
  - 참조 패턴: views/leave/components/LeaveApplyForm.vue 결재선 섹션(171~267)
  - 역할: 프리셋 칩 + 결재자 순서 리스트 + 추가/삭제(프레젠테이션). 검색은 부모가 연 시트가 담당.
  - v-model: approverList ([{ approverUserCd, userNm, userId, rankNm, nodeNm }]) — 순서 = 결재 단계.
  - ⚠️ approverUserCd 는 식별자. 위치 index 로 재인덱싱하지 않는다(서버가 STEP_NO=배열 순서로 INSERT).
-->
<template>
  <section class="aprv-sec">
    <p class="aprv-sec__title">결재선</p>

    <!-- 프리셋 칩 (Q2: 자동선택 안 함 — 사용자 명시 선택) -->
    <div v-if="presets.length > 0" class="preset-list">
      <button
        v-for="p in presets"
        :key="p.presetId"
        type="button"
        class="preset-chip"
        :class="{ 'preset-chip--on': selectedPresetId === p.presetId }"
        @click="onSelectPreset(p)"
      >
        {{ p.presetNm }}
        <span v-if="p.defaultYn" class="preset-chip__tag">기본</span>
      </button>
    </div>

    <!-- 결재자 순서 리스트 -->
    <ul v-if="modelValue.length > 0" class="aprv-list">
      <li v-for="(ap, idx) in modelValue" :key="ap.approverUserCd" class="aprv-row">
        <span class="aprv-row__step">{{ idx + 1 }}</span>
        <div class="aprv-row__info">
          <p class="aprv-row__name">{{ ap.userNm }}</p>
          <p class="aprv-row__meta">{{ metaOf(ap) }}</p>
        </div>
        <button
          type="button"
          class="aprv-row__del"
          aria-label="결재자 제거"
          @click="onRemove(ap.approverUserCd)"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </li>
    </ul>
    <p v-else class="aprv-empty">결재자를 추가해 주세요</p>

    <button type="button" class="btn-add" @click="$emit('open-picker')">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
        stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <line x1="12" y1="5" x2="12" y2="19" />
        <line x1="5" y1="12" x2="19" y2="12" />
      </svg>
      결재자 추가
    </button>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  // 003/기존 GET /appApi/mypage01/approval-presets 의 presets 배열
  presets: { type: Array, default: () => [] },
  // 결재자 순서 리스트(v-model)
  modelValue: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'open-picker'])

// 선택된 프리셋 표기용(전개 후 직접 편집 시 이탈 표시)
const selectedPresetId = ref('')

// 표시 헬퍼(UI — 허용)
const metaOf = (ap) => [ap?.nodeNm, ap?.rankNm].filter(Boolean).join(' · ')

// 프리셋 선택 → steps 를 결재자 리스트로 전개(STEP_NO=배열 순서 보존). 재선택 토글 해제.
const onSelectPreset = (preset) => {
  if (!preset) return
  if (selectedPresetId.value === preset.presetId) {
    selectedPresetId.value = ''
    emit('update:modelValue', [])
    return
  }
  selectedPresetId.value = preset.presetId
  const list = (preset.steps || []).map((s) => ({
    approverUserCd: s.approverUserCd,
    userNm: s.userNm,
    userId: s.userId,
    rankNm: s.rankNm,
    nodeNm: s.nodeNm,
  }))
  emit('update:modelValue', list)
}

// 결재자 제거 — userCd 식별자 필터(위치 index 재인덱싱 금지). 프리셋 이탈 표기.
const onRemove = (approverUserCd) => {
  emit('update:modelValue', props.modelValue.filter((a) => a.approverUserCd !== approverUserCd))
  selectedPresetId.value = ''
}

// TODO(developer): 부모가 시트 add(picked[]) 수신 후 modelValue append 시,
//   직접 추가면 selectedPresetId 를 비워 프리셋 이탈을 표시(이 컴포넌트는 노출 후 부모가 호출하는
//   resetPreset() 을 expose 하거나, 부모가 update:modelValue 와 함께 프리셋 해제를 관리).
//   값 가공/검증(중복 dedup)은 부모 폼 또는 developer 가 보완.
defineExpose({
  resetPreset: () => { selectedPresetId.value = '' },
})
</script>

<style scoped>
.aprv-sec {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.aprv-sec__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 프리셋 칩 */
.preset-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}
.preset-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.preset-chip--on {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
  color: var(--color-primary-text-deep);
  font-weight: 500;
}
.preset-chip__tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-surface);
}

/* 결재자 리스트 */
.aprv-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.aprv-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.aprv-row__step {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary-text-deep);
  font-size: 12px;
  font-weight: 600;
}
.aprv-row__info {
  flex: 1;
  min-width: 0;
}
.aprv-row__name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.aprv-row__meta {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.aprv-row__del {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  cursor: pointer;
}
.aprv-empty {
  margin: 0;
  padding: var(--space-md);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
  background: var(--color-surface);
  border: 0.5px dashed var(--color-border);
  border-radius: var(--radius-md);
}

.btn-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 44px;
  background: var(--color-surface);
  border: 0.5px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.btn-add:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
</style>
```

### 5.2 AttdApproverPickerSheet.vue (PRAFTA-APP-009-7)

```vue
<!--
  AttdApproverPickerSheet.vue — 결재자 검색/선택 바텀시트 (prafta-app-009, UI-009-2)
  - 참조 패턴: views/leave/components/LeaveApproverPickerSheet.vue (approver-search + BaseBottomSheet)
  - D1 풀 공유: 결재자 검색은 기존 GET /appApi/leaveflow/approver-search 재사용(신규 endpoint 없음).
  - v-model: open(시트 표시), props.excludedUserCds(이미 선택된 결재자 제외).
  - emit: add(picked[] = [{ userCd, userId, userNm, rankNm, nodeNm }]).
-->
<template>
  <BaseBottomSheet v-model="open" title="결재자 추가">
    <div class="picker">
      <!-- 검색 -->
      <div class="picker__search">
        <svg class="picker__search-ic" width="18" height="18" viewBox="0 0 24 24" fill="none"
          stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
          aria-hidden="true">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          v-model="keyword"
          class="picker__search-input"
          type="text"
          placeholder="이름으로 검색"
          @input="onKeywordInput"
        />
      </div>

      <!-- 상태별 -->
      <p v-if="loading" class="picker__state">불러오는 중...</p>
      <p v-else-if="error" class="picker__state picker__state--err">
        결재자를 불러오지 못했어요. 다시 시도해 주세요.
      </p>
      <p v-else-if="results.length === 0" class="picker__state">검색 결과가 없어요</p>

      <!-- 결과 리스트 (다중 선택) -->
      <ul v-else class="picker__list">
        <li v-for="r in results" :key="r.userCd" class="picker__item">
          <label class="picker__check">
            <input
              type="checkbox"
              :checked="picked.has(r.userCd)"
              :disabled="excludedUserCds.includes(r.userCd)"
              @change="onToggle(r)"
            />
            <span class="picker__item-info">
              <span class="picker__item-name">{{ r.userNm }}</span>
              <span class="picker__item-meta">{{ metaOf(r) }}</span>
            </span>
          </label>
        </li>
      </ul>

      <button v-if="hasNext && !loading" type="button" class="picker__more" @click="onLoadMore">
        더 보기
      </button>
    </div>

    <!-- sticky 추가 버튼 -->
    <template #footer>
      <button type="button" class="picker__apply" :disabled="picked.size === 0" @click="onApply">
        선택 추가{{ picked.size > 0 ? ` (${picked.size})` : '' }}
      </button>
    </template>
  </BaseBottomSheet>
</template>

<script setup>
import { ref } from 'vue'
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue' // TODO(developer): 실제 BaseBottomSheet 경로 확인

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  excludedUserCds: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'add'])

// 시트 open 양방향 (BaseBottomSheet v-model 프록시)
const open = ref(props.modelValue) // TODO(developer): computed get/set 으로 v-model 동기화

// 검색 상태 (developer: API 호출/디바운스/페이징)
const keyword = ref('')
const results = ref([])      // [{ userCd, userId, userNm, rankNm, nodeNm }]
const picked = ref(new Set()) // 선택된 userCd 집합
const loading = ref(false)
const error = ref(false)
const hasNext = ref(false)
const page = ref(1)

// 표시 헬퍼 (UI — 허용)
const metaOf = (r) => [r?.nodeNm, r?.rankNm, r?.userId ? `사번 ${r.userId}` : '']
  .filter(Boolean).join(' · ')

// 선택 토글 (UI 상태 — 허용)
const onToggle = (r) => {
  const next = new Set(picked.value)
  if (next.has(r.userCd)) next.delete(r.userCd)
  else next.add(r.userCd)
  picked.value = next
}

// TODO(developer): 검색 호출 — GET /appApi/leaveflow/approver-search?keyword=&page=&size=
//   디바운스(300ms) + page=1 리셋. hasNext/results 채움. excludedUserCds 는 표시 disabled.
const onKeywordInput = () => {
  // TODO(developer): debounce 후 fetch(page=1)
}

// TODO(developer): 다음 페이지 append.
const onLoadMore = () => {
  // TODO(developer): page+1 fetch 후 results append
}

// 선택 결재자 emit (results 에서 picked 만 추려 객체 배열로). 시트 닫기.
const onApply = () => {
  const selected = results.value.filter((r) => picked.value.has(r.userCd))
  emit('add', selected)
  picked.value = new Set()
  // TODO(developer): open=false + update:modelValue
}
</script>

<style scoped>
.picker {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding: var(--space-sm) 0;
}
.picker__search {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  height: 44px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.picker__search-ic {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}
.picker__search-input {
  flex: 1;
  min-width: 0;
  border: 0;
  background: transparent;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: inherit;
}
.picker__search-input:focus {
  outline: none;
}
.picker__state {
  margin: 0;
  padding: var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.picker__state--err {
  color: var(--color-danger);
}
.picker__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  max-height: 50vh;
  overflow-y: auto;
}
.picker__item {
  border-radius: var(--radius-md);
}
.picker__check {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  min-height: 48px;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
}
.picker__item-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.picker__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.picker__item-meta {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.picker__more {
  height: 40px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}
.picker__apply {
  width: 100%;
  height: 48px;
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-md);
  color: var(--color-surface);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.picker__apply:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
</style>
```

> **3 폼(009-8) 골격 미작성 사유**: `SchedModifyForm`/`AttdCorrectionForm`/`OvertimeForm` 은 이미 디스크에 존재하는 보완 작업이다(신규 파일 아님). 결재선 섹션 추가 = `<ApprovalLineSection v-model="approverList" :presets="presets" @open-picker="..."/>` + `<AttdApproverPickerSheet v-model="pickerOpen" :excluded-user-cds="approverUserCds" @add="onAddApprovers"/>` 를 사유 섹션 아래에 삽입하고, emit submit payload 에 `approverUserCds`/`presetId` 를 추가하는 in-place 수정이다. 연차 `LeaveApplyForm.vue` 의 `onAddApprovers`/`onRemoveApprover`/`approverUserCds` computed 패턴을 그대로 차용한다(전체 골격 재출력 대신 삽입 지점만 명시 — developer 가 기존 파일 Edit). 삽입 지점/계약은 §3.1·UI-009-3 에 고정.

---

## 6. 의존성 그래프

```
prafta-046 (선행, 별도) ─┐  D5 전제(노드 관리자 불변식)
                         ↓
PRAFTA-APP-009-1 (마이그 SYS045 + ATTD_400_105)
        ↓
PRAFTA-APP-009-2 (PUSH 생산자) ──┐
                                 ├─ PRAFTA-APP-009-3 (분기+라인 INSERT 공용 서비스)
                                 │         ↓
PRAFTA-APP-009-5 (F12/F13 가드)  │   PRAFTA-APP-009-4 (req07 register 확장 — 009-3 호출)
PRAFTA-APP-009-9 (F15/F14)       │
                                 │  (BE 응답 계약 §3 고정)
                                 ↓
PRAFTA-APP-009-6 (ApprovalLineSection) ─┐
PRAFTA-APP-009-7 (AttdApproverPickerSheet) ─┤
                                            └─ PRAFTA-APP-009-8 (3 폼 통합)

prafta-app-020 (후행, 별도): 'N' 웹 승인 다단계화 + 차례도래 PUSH(승인측) + 'Y' 승인자 OR 게이팅 권한 강제.
```

병렬: BE(1→2→3→4·5·9)와 FE(6·7→8) 동시 진행 가능(§3 계약 고정).

---

## 7. 비기능 요구사항

### 7.1 보안 (security 정독 — High)
- **결재자 스코프 가드(D8)**: `countValidApprovers`(CMPNY+SITE+재직+활성) ≠ distinct → `COMMON_400_001`. 클라가 타 사업장/회사/미존재 USER_CD 주입 시 그 결재함에 신청자 실명 PUSH 노출(cross-tenant PII 침해) 차단 — submitLeave 와 동일 필수.
- **자기승인 자격(D7)**: 본인 결재자 지정은 노드 자체근태승인+노드관리자일 때만(`ATTD_400_056`). 무자격 자기 지정으로 결재 우회 차단.
- **IDOR(F14)**: 식별값 JWT 전용. nodeCd=gv_nodeCd (재확인 009-9).
- **PUSH PII**: DATA_PAYLOAD 라우팅 키만, 본문 신청자명 평문 USER_NM(복호화 금지). dedup UNIQUE 멱등.
- **D5 설정오류**: `'Y'` 노드관리자 0명 fail-closed(`ATTD_400_105`), master/hr 폴백 없음(prafta-046 구조 차단).
- **race(F15)**: 중복 미처리 요청 차단의 SELECT→INSERT window — UNIQUE/advisory lock.

### 7.2 SQL
- 신규 매퍼(selectAttdSelfApprvYn/selectIsNodeAdmin): 명시 컬럼, `#{}` 바인딩, CMPNY+SITE 스코프, `SELECT *` 금지.
- 결재라인/outbox/프리셋/검색은 **기존 매퍼 재사용**(신규 SQL 난립 금지).

### 7.3 프론트엔드
- TypeScript 금지. `<style scoped>` 필수. CSS 변수만(`.attd-req-view` 토큰 상속). `!important` 금지. 터치 44px. 한국어. BaseBottomSheet 재사용(신규 시트 금지). 인라인 SVG(CDN 금지).
- 식별자(approverUserCd) 기반 dedup/삭제 — index 재인덱싱 금지(메모리 `feedback_planner_skeleton_logic_bug_qa_blindspot` 경계: 골격에 위치밀림 버그 박지 말 것).

### 7.4 트랜잭션/정합성
- 결재라인 INSERT 는 req07 등록 트랜잭션에 참여(단일 `@Transactional`). PUSH 적재는 예외 격리(try-catch, 본 흐름 영향 금지).
- 슬롯 다건 REQ_ID = 슬롯별 라인 INSERT(009-3 §5 결정).

---

## 8. 정책서 출처 (후속 에이전트 정독 범위)

| 작업 | 정책서 섹션 |
|---|---|
| 자기승인/선점/반려사유/알림(D3·D4·D7) | `attd/09-requests-approval.md` §9.5, §9.6 |
| 결재 플로우(D6) | `request-approval/06-approval-flows.md` §6.1~§6.4 |
| 결재단계 데이터 구조 | `request-approval/09-data-structures.md` §9.1 |
| 결재자 조직 스코프(D8) | `common/08-permissions.md` §8.4 |
| 결재선 카드 인터랙션(Q1·UI) | `common/13-ui-ux.md` §13.3 |
| 알림 채널(PUSH outbox) | `common/10-notifications.md` |
| 마감 가드(F12) | `attd/12-schedule-close.md`, `attd/13-attendance-close.md` |
| 스케줄 존재 조건(F13) | `attd/09-requests-approval.md` §9.2 |

> ⚠️ **D2 의미 상이 정책서 명시 필요(요청서 §8.3)**: `SELF_ATTD_APPRV_YN` 이 연차("본인 결재자 자동승인 자격")와 근태(D3/D4 "노드 자체승인 분기")에서 의미가 다름. `attd/09-requests-approval.md` 또는 노드 관리 정책에 1줄 명시 필요 — **본 plan 분해 범위 밖(정책 문서 갱신은 사용자/문서 담당)**. developer/qa 는 이 상이를 인지하고 매퍼 재사용 가부(D2 신규 vs D7 재사용)를 본 plan §1.1 D2/D7 결정대로 따른다.

---

## 9. 미해결/주의 (분해 후 잔여)

1. **PUSH 실도달 0**: `tb_user_device` PUSH_TOKEN 0건 + 워커 OFF → outbox 적재까지만. 앱 FCM 토큰등록 + com-002 게이트 ON 별도 선행(범위 밖).
2. **`'Y'` 승인자 권한 강제는 app-020**: app-009 단독 상태에서 `'Y'` 요청은 현행 단일승인 경로로 처리될 수 있음(§0.6 경계). app-020 완료로 OR 게이팅 권한 강제 완성.
3. **슬롯 다건 결재라인 중복 표시**: 'N'+2구간 시 결재함에 REQ_ID 2건. UX 영향 qa 공유(009-3 §5).
4. **`approval-context` 메타 endpoint 신설 여부**: 009-8 결재선 필수/숨김 분기를 위해 selfApprvYn 메타 GET 이 필요할 수 있음(§3.2). developer 결정(009-3 매퍼 재사용 시 소규모).
5. **D2 정책서 1줄 명시**: 문서 갱신은 본 plan 분해 범위 밖.

---

## 10. Notion 등록 안내 (planner 는 Notion 접근 불가 — 메인 세션 대행)

- "작업 로그" DB: PRAFTA-APP-009-1 ~ -9 (위 §2.2 상세 설명 그대로). 상태=분해완료, 담당=planner, 영역=app, 모듈=req/req09(또는 req/req07 보완).
- "도메인 지식 베이스" DB: UI-009-1/UI-009-2/UI-009-3 (위 §4 화면 명세). 검증상태=Claude 분석.
- 산출물 컬럼: FE 작업(6/7/8)에 Vue 골격 경로 기록 — `src/views/req/components/ApprovalLineSection.vue`, `AttdApproverPickerSheet.vue`, 3 폼 보완.
- 선행 Relation: PRAFTA-046, PRAFTA-APP-007(완료), 후행 PRAFTA-APP-020.
