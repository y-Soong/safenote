# prafta-app-020 — 근태/OT/스케줄수정 웹 승인 "단일승인 → 다단계 결재" 전환

> **작업 ID prefix**: `PRAFTA-APP-020`
> **본 문서 위상**: 작업 요청서(분해 전). prafta-app-009 §1.E 에서 "범위가 커 별도 작업 단위로 분리"로 위임된 항목을 독립 요청서로 분리한 것. 사용자가 "분해 진행" 지시 시 planner 가 `prafta-app-020-plan.md` 로 분해한다.
> **분리 결정(2026-06-04)**: prafta-app-009 분해 시 사용자가 "승인 다단계화는 prafta-app-020 으로 분리, app-009 범위에서 제외" 지시.

---

## 0.0 범위 확정 (2026-06-04) — `'N'` 케이스 전용

app-009 분해에서 근태 결재가 노드 `SELF_ATTD_APPRV_YN` 으로 분기 확정:
- `'Y'`(자체근태승인): **결재선 미사용**. 노드 Main/Sub 관리자 OR 단일승인(app-009 §8 D3~D5에서 처리). **본 작업 대상 아님.**
- `'N'`: 결재선 다단계. **본 작업(app-020)은 `'N'` 케이스의 웹 승인 다단계화만 담당.**

따라서 아래 §1 의 다단계 승인/반려 전환은 **`'N'` 케이스(결재라인이 INSERT 된 요청)에만 적용**한다. `'Y'` 케이스는 현행 단일승인 + 노드관리자 게이팅(app-009)이라 본 작업이 건드리지 않는다. 승인 메서드 진입 시 **결재라인 존재 여부로 분기**(라인 있으면 다단계, 없으면 기존 단일승인 경로)하는 것이 안전하다.

---

## 0. 배경 — 왜 분리되었나

prafta-app-009 는 **신청 측 결재선 INSERT**(§1.A~D, 앱 폼에서 결재자 지정 → `tb_user_attd_req_approval` 다중행 INSERT)까지를 범위로 한다. 그러나 그 결재선이 "차례대로" 진행되려면 **웹 승인 경로가 다단계로 동작**해야 하는데, 현재 근태/OT/스케줄수정 웹 승인은 **단계 개념이 없는 단일 승인**이다. 이 전환은 웹 승인 서비스/화면을 광범위하게 건드리므로 별도 작업으로 분리한다.

### 현행 단일승인 구조 (확인 완료 2026-06-04)

`com.prafta.web.attd.attd07.service.impl.Attd07ServiceImpl`:

| 메서드 | 위치(라인) | 전이 | 구조 |
|---|---|---|---|
| `updateUserAttdRequest` (근태 보정 01/02) | 237~384 (전이 378) | `REQ_STATUS '01'→'02'` 직접 | 단계 개념 없음. `REQ_STATUS='01'` 조건만 확인 후 즉시 `02`. |
| `updateUserOvertimeRequests` (OT 03) | 760~1033 (전이 1003) | `REQ_STATUS '01'→'02'` 직접 | 동일 |
| `approveSchedModifyRequest` (스케줄수정 10) | 500~593 (전이 584) | `REQ_STATUS '01'→'02'` 직접 | 동일 |

→ `tb_user_attd_req_approval`(결재라인 테이블)을 **읽지도 쓰지도 않는다.** 현재 이 테이블은 연차(leaveflow)만 사용 중.

---

## 1. 목표 설계 — 연차 다단계 결재 패턴 미러

웹 승인 3종을 연차(`LeaveFlowServiceImpl`)의 다단계 패턴으로 전환한다. **신규 발명 없이 검증된 연차 패턴을 그대로 미러링**하는 것이 핵심 원칙.

### 1.1 재사용 가능 자산 (이미 존재)

| 자산 | 위치 | 비고 |
|---|---|---|
| `tb_user_attd_req_approval` 테이블 | 실재 (REQ_ID/APPROVAL_STEP/CMPNY_CD/APPROVER_USER_CD/APPROVAL_STATUS/APPROVAL_COMMENT/APPROVAL_DATE) | **다목적 설계**(REQ_ID→tb_user_attd_req). 근태 요청도 동일 구조로 사용 가능. |
| `ApprovalLineMapper` | `com.prafta.common.cmm.approval.mapper.ApprovalLineMapper` | `insertApprovalStep` / `updateStepStatus` / `selectFirstWaitingStep` / `selectApprovalStep`. **REQ_ID 기반이라 근태 REQ_ID 에도 그대로 적용 가능.** |
| `ApprovalStepVO` | `com.prafta.common.cmm.approval.vo.ApprovalStepVO` | 단계 상태 VO. |
| SYS044 결재단계상태 | `00`대기 / `01`신청(차례도래) / `02`승인 / `03`반려 | 연차와 동일 코드 그대로. |
| `LeaveApprovalNotiService` | `com.prafta.common.cmm.leave.service.LeaveApprovalNotiService` | PUSH 적재. 근태용으로 일반화 또는 미러(§1.4). |

### 1.2 승인 흐름 (연차 `approveStep` 287~318 미러)

각 승인 메서드를 다음 단계 처리로 교체:

1. 현재 단계 `updateStepStatus(reqId, approvalStep, '02'(승인), comment, approver)`.
2. `selectFirstWaitingStep(cmpny, reqId)` 로 다음 `00`(대기) 단계 조회 (자기승인 `02` 단계는 자동 skip).
3. **다음 단계 있으면**: `updateStepStatus(reqId, nextStep, '01'(신청), null, approver)` → 차례 도래. **PUSH 적재**(§1.4).
4. **다음 단계 없으면(최종 승인)**: `REQ_STATUS='02'` 갱신 + **기존 단일승인의 "승인 시 반영 로직"을 이 분기로 이동**. ⚠️ 핵심 주의점:
   - 근태 보정: `updateUserAttdRequest` 의 출퇴근 기록 생성/수정 반영부(378줄 이후 로직).
   - OT: `updateUserOvertimeRequests` 의 추가근무 정산 반영부(1003줄 이후).
   - 스케줄수정: `approveSchedModifyRequest` 의 `tb_user_work_plan` 교체부(584줄 이후, prafta-app-007 단일 WORK_PLAN_CD 교체 패턴).
   - **즉 "반영"은 단일승인에서는 승인 즉시 일어났지만, 다단계화 후엔 최종 단계 승인 시점으로 미뤄진다.** 중간 단계 승인 시 반영하면 안 됨.

### 1.3 반려 흐름 (연차 `rejectStep` 322~ 미러)

1. 반려 사유 필수 검증(비어있으면 `ATTD_400_057` 등가).
2. 현재 단계 `updateStepStatus(reqId, approvalStep, '03'(반려), comment, approver)`.
3. `REQ_STATUS='03'` 갱신.
4. **반영 없음** — 어떤 단계에서 반려되든 출퇴근/OT/스케줄 원본 불변(연차 06 반려가 기존기록 불변인 것과 동일 원칙).

### 1.4 "차례 도래" PUSH 통합 (prafta-com-004 / app-009 §1.E 연계)

- 다음 단계 `01` 전환 지점(§1.2-3)에서 `notifyApprovalTurn` 류 호출, **예외 격리(try-catch)** 필수(연차 293~305 미러).
- SYS045 PUSH 타입: `ATTD_APPROVAL_TURN` 신규 또는 연차 `LEAVE_APPROVAL_TURN` 과 통합한 범용 `APPROVAL_TURN`. **app-009 §1.E 분해 시 결정한 코드와 일치**시킬 것(중복 정의 금지).
- 발송 대상: 차례 도래한 단계의 `APPROVER_USER_CD` 1인. 자기승인 자동 `02` skip 단계·반려·최종승인(다음 없음) 제외. master/hr 자동포함 없음.
- dedupKey = `"REQ_TURN_" + reqId + "_" + approvalStep` (app-009 §1.E 와 동일 규약).
- 재사용: `NotiOutboxInsertVO` + `LeaveDashboardMapper.insertNotiOutbox`(prafta-031).
- ⚠️ **실도달 한계**: 현재 `tb_user_device` PUSH_TOKEN 0건 + 워커 게이트 OFF → outbox 적재까지만 동작, 실단말 도달 0. 앱 FCM 토큰 등록 + 게이트 ON 은 별도 선행(prafta-com-002 메모리 참조).

### 1.5 선점/잠금·마감 가드

- 선점(처리 잠금): 연차 `loadProcessableReq` 패턴 — 동시 처리 방지(정책 `attd/09` §9.5, 공통 9장).
- 마감 가드: 연차 `ensureLeaveNotClosed`(prafta-028 부서단위 마감) 미러. 근태 보정/OT/스케줄수정 모두 마감 기간이면 승인/반려 차단.

---

## 2. 프론트엔드 — 웹 승인 화면 (정책 `attd/09` §9.6 통합 승인 3탭)

`prafta-web-frontend` 의 요청 승인 관리 화면(스케줄수정/초과근무/연차 3탭):

- 우측 상세 패널에 **결재 단계 타임라인**(N단 중 현재 단계, 각 단계 결재자/상태/처리일시) 표시 — 연차 탭의 단계 타임라인 UI 패턴 재사용.
- 승인/반려 버튼은 **현재 차례(`01`)인 결재자 본인에게만 활성** (`gvUserCd` == 현재 단계 `APPROVER_USER_CD`). 연차 일자상세/Attd_07 게이팅 패턴 참조(메모리 `project_prafta_daydetail_leave_approval`).
- 일괄 승인/반려 시 단계 개념과의 정합(자기 차례 아닌 건 제외) 주의.

---

## 3. 정책서 출처

- `attd/09-requests-approval.md` §9.5 (자기 승인 원칙 / 선점 / 반려 사유 필수), §9.6 (통합 승인 화면 3탭 / 엣지 케이스).
- `request-approval/06-approval-flows.md` §6.1~§6.4 (결재 플로우).
- `request-approval/09-data-structures.md` §9.1 (lock·decision·history 필드).
- `common/08-permissions.md` §8.4 (결재자 가시 범위/조직 스코프).

---

## 4. 의존성

- **선행 (반드시 완료)**: prafta-app-009 §1.A~D (신청 측 결재선 INSERT). 결재라인이 INSERT 되어 있어야 다단계 승인이 의미를 가짐.
- **연계**: prafta-com-004 (연차 차례도래 PUSH — 동일 인프라/패턴), prafta-com-002 (FCM 워커 — 실발송).
- **참조 패턴**: `LeaveFlowServiceImpl#approveStep`(287~318) / `#rejectStep`(322~) / `submitLeave` 결재라인 생성(166~224).

---

## 5. 분해 시점 결정 사항 (planner 분해 단계로 위임)

| # | 결정 포인트 | 비고 |
|---|---|---|
| Q1 | ~~근태 "결재 필요 여부" 판정 기준~~ → **확정** | **결정됨(app-009 §8 D2~D6)**: 노드 `SELF_ATTD_APPRV_YN='N'` 만 결재선(다단계). `'Y'` 는 노드관리자 OR 단일승인(본 작업 대상 아님). 승인 메서드는 **결재라인 존재 여부로 분기**(§0.0). |
| Q2 | `selectFirstWaitingStep` 등 ApprovalLineMapper 의 reqType 무관 재사용 가능성 검증 | REQ_ID 기반이므로 가능 추정. 단 연차 전용 가정(예: 연차 REQ_TYPE 필터)이 mapper SQL 에 박혀있지 않은지 확인 필요. |
| Q3 | "승인 시 반영 로직"의 최종단계 이동 시 트랜잭션 경계 | 중간단계 승인과 최종단계 반영의 원자성. |
| Q4 | 앱 승인 경로 추가 여부 | 현재 근태 승인은 웹만(연차도 앱 승인경로 없음). 본 작업도 웹만으로 제한 권장. |
| Q5 | 일괄 승인 시 다단계 정합 | 자기 차례 아닌 요청 일괄 제외 규칙. |

---

## 6. 추정 작업 단위 (분해 전 가이드)

- **마이그레이션**: 0~1건 (SYS045 PUSH 타입은 app-009 와 공유. 신규 테이블 없음 — 결재라인 테이블 이미 존재).
- **백엔드**: `Attd07ServiceImpl` 3 승인 메서드를 다단계 승인/반려로 재작성 + `ApprovalLineMapper` 근태 적용 + PUSH hook. (반려 메서드가 별도면 그것도.)
- **백엔드 검증**: ApprovalLineMapper SQL 의 연차 전용 가정 부재 확인(Q2).
- **프론트엔드**: 웹 승인 3탭 상세 패널에 단계 타임라인 + 차례 기반 버튼 게이팅.
- **테스트**: 다단계 승인 진행/최종승인 반영/중간반려/자기승인 skip/PUSH 적재 단위 테스트.

---

## 7. 채팅 노트

- 본 문서는 prafta-app-009 분해(2026-06-04) 시 사용자 지시로 분리 생성.
- prafta-app-009 가 신청 측(결재선 생성)을, 본 문서가 처리 측(다단계 승인)을 담당. **두 작업이 모두 완료되어야 근태/OT/스케줄수정의 다단계 결재 + 차례도래 PUSH 가 end-to-end 동작**한다.
- ⚠️ Q1(근태 결재 필요 여부 판정)은 app-009 §1.D 와 본 작업의 공통 전제 — 한 곳에서 확정하고 양쪽이 참조할 것.
