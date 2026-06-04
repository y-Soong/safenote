# prafta-com-004 — 연차 결재 PUSH 알림 (차례 도래 + 무결재 사용 통보)

> **작업 ID prefix**: `PRAFTA-COM-004` (분해 시 단일 출처 plan `prafta-com-004-plan.md` 작성 예정)
> **본 문서 위상**: 작업 요청서. 사용자가 검토 후 "분해 진행" 지시하면 planner 가 plan 으로 분해한다.
> **영역**: 공통(백엔드 outbox 생산자 추가) + 앱(PUSH 수신). 발송 워커는 prafta-com-002(공용 FCM consumer) 재사용.
>
> **관련 산출물**:
> - `project_prafta_031_recall_and_outbox` 메모리 — tb_noti_outbox / SYS045 인프라.
> - `project_prafta_com_002_fcm_push_worker` 메모리 — PENDING→FCM 공용 consumer(게이트 기본 off).
> - `project_prafta_com_001_leave_refusal_push` 메모리 — 노드 관리자 산출 패턴(`selectSiteRefusalAdmins`)·통지/감지 outbox 패턴.
> - `prafta-app-009.md` — 근태/OT/스케줄수정 결재선 + 차례 도래 PUSH(본 문서와 동일 PUSH 유형이나 결재선 인프라가 없어 별도 작업).

---

## 0. 배경

PRAFTA PUSH 는 outbox 패턴이다. 업무 로직이 `tb_noti_outbox` 에 PENDING 행을 적재(생산자)하고, prafta-com-002 의 공용 워커(`PushSendScheduler`→`PushSenderService`→`FcmClient`)가 FCM 으로 실제 발송한다. 현재 생산자는 4종(연차 회수 / 노무수령거부 통지·감지 / 아차사고 보고)뿐이고, **연차 결재 흐름에는 PUSH 가 없다.**

본 작업은 **연차** 도메인에 PUSH 생산자 2종을 추가한다. 연차는 결재선 인프라(`tb_user_attd_req_approval`, prafta-019)가 이미 완성돼 있어 즉시 구현 가능하다. (근태·OT·스케줄수정은 결재선 자체가 미구현이므로 prafta-app-009 로 분리.)

---

## 1. 범위

### 시나리오 A — 연차 결재 "차례 도래" PUSH (단계별 결재자)

연차 신청이 결재선을 타는 동안, 어떤 결재 단계가 **`01(신청/내 차례)`** 상태로 전환되는 순간 **그 단계의 지정 결재자(`APPROVER_USER_CD`) 1인에게만** PUSH 한다.

발송 시점(코드 hook):
1. **신청 시 첫 결재자 지정** — `web LeaveFlowServiceImpl.submitLeave` 182~198줄 / `app AppLeaveFlowServiceImpl.submitLeave`. 결재선 일괄 생성에서 `STEP_APPLIED('01')` 로 세팅되는 단계(= 첫 수동 단계 `currentIdx`, 194줄)의 결재자.
2. **다음 단계 진행** — `web LeaveFlowServiceImpl.approveStep` 250~252줄. `selectFirstWaitingStep` 으로 찾은 다음 단계를 `STEP_APPLIED('01')` 로 전환할 때 그 결재자.

발송 제외:
- 자기 승인(`SELF_ATTD_APPRV_YN` ON)으로 자동 `02` 처리되어 **건너뛴 단계**는 발송하지 않는다(194줄 `STEP_WAIT`/`STEP_APPROVED` 분기, approveStep 의 "자동 skip").
- 신청자 본인이 결재자인 단계(자동승인)도 발송 안 함.
- 반려(`rejectStep`)·최종 승인(다음 단계 없음)은 본 시나리오 대상 아님(차례 도래가 아님).

수신자: 해당 단계의 `APPROVER_USER_CD` **단 1인**. master/hr 자동 포함 없음(사용자 확정).

### 시나리오 B — 무결재 연차 "사용 통보" PUSH (소속 노드 관리자)

연차가 **결재 없이 즉시 확정**되는 경우(`aprvRequired=false` → `REQ_APPROVED`+`USE_CONFIRMED`), 신청자 **소속 노드의 main/sub 관리자에게만** "누가 / 언제 / 어떤 단위의 연차를 사용했다"를 통보 PUSH 한다.

발송 시점(코드 hook):
- `web LeaveFlowServiceImpl.submitLeave` — `aprvRequired==false` 분기(156줄 `reqStatus=REQ_APPROVED`). 7)단계 `insertLeaveUse`(211줄) 직후, 같은 트랜잭션에서 outbox INSERT.
- `app AppLeaveFlowServiceImpl.submitLeave` — 동일 무결재 분기.
- ⚠️ **결재 Y인데 전 단계 자동승인으로 즉시 확정된 경우**(`fullyAutoApproved`, 215줄)도 "결재 없이 확정"에 해당하는지 분해 단계에서 판정 필요(아래 §6 결정 D1).

**발송 제외(사용자 확정):**
- 관리자가 **근무계획관리(Attd_05) 화면에서 연차셀 직접 입력**하는 경로(`recordDirectLeaveUsage`, REQ_ID=NULL)는 PUSH 하지 않는다.

수신자: 신청자가 속한 노드(`TB_SITE_NODE`)의 `MAIN_ADMIN_CD` + `SUB_ADMIN_CD`. **master/hr 제외**(노무수령거부 `selectSiteRefusalAdmins` 의 (2)번 노드 서브쿼리만 사용, (1) 역할기반 UNION 제외 — 신규 매퍼 `selectNodeAdmins` 필요).

메시지 본문 구성 데이터(`TB_USER_LEAVE_USE`):
- 종일/반차/시간차 구분 = `USE_UNIT_TYPE`(SYS025: 00종일/01반차/02·03·04 시간차)
- 일수 = `LEAVE_DAYS`(0.5/1.0 등)
- **시간차일 때 시작~종료** = `START_TIME`/`END_TIME`(HHMM)
- 예) 종일: "○○○님이 2026-06-10 연차 1일을 사용했습니다."
- 예) 시간차: "○○○님이 2026-06-10 09:00~11:00 시간차 연차를 사용했습니다."

---

## 2. 재사용 자산 (신규 SQL 난립 금지)

| 자산 | 위치 | 용도 |
|---|---|---|
| `NotiOutboxInsertVO` | `com.prafta.common.cmm.leave.vo` | outbox 1행 운반체(targetUserCd/notiType/channel/title/body/dataPayload/dedupKey) |
| `LeaveDashboardMapper.insertNotiOutbox` | `com.prafta.common.cmm.leave.mapper` | outbox INSERT(`SEND_STATUS='PENDING'`) + NOTI_ID 채번 |
| 공용 FCM 워커 | prafta-com-002(`PushSendScheduler` 등) | 실제 발송(본 작업은 생산자만, 워커 무수정) |
| 노드 관리자 산출 패턴 | `LeaveRefusalMapper.selectSiteRefusalAdmins` (121~173줄) | (2)번 노드 main/sub 서브쿼리를 분리한 `selectNodeAdmins` 신규 작성 기준 |

---

## 3. SYS045 (알림 유형) 신규 코드 — 마이그레이션

`tb_syst_val_d` 에 SYS045 디테일 2건 추가(시드 SQL `prafta-com-004-sys045-noti-type.sql`):

| SYST_VAL_D_CD | 의미 | 채널 |
|---|---|---|
| `LEAVE_APPROVAL_TURN` | 연차 결재 차례 도래(결재자) | PUSH |
| `LEAVE_USED_NO_APRV` | 무결재 연차 사용 통보(노드 관리자) | PUSH |

> 멱등성: 기존 prafta-031-sys045 시드와 동일 형식. PK 중복 시 건너뜀. `VAL_D_INFO_1='PUSH'`.

---

## 4. 중복 발송 방지 (dedupKey)

`tb_noti_outbox` UNIQUE(CMPNY_CD, DEDUP_KEY) 활용:
- 시나리오 A: `"LV_TURN_" + reqId + "_" + approvalStep` (단계별 1건). 같은 단계 재진입 없음 전제이나 멱등 보장.
- 시나리오 B: `"LV_USED_" + leaveId + "_" + targetUserCd` (관리자별 1건).

---

## 5. 보안·PII 검토 포인트 (분해 시 security 에이전트 필수)

- **PII 평문 저장 회피**: 메시지 본문의 신청자명은 `TB_USER` AES-GCM 암호화 값이다. 노무수령거부 패턴(`LeaveRefusalConst.NOTICE_BODY_FORMAT`)은 "이름을 body 에 합성하지 않고 consumer 가 렌더링 시점에 합성"하는 것을 전제로 한다. **단, prafta-com-002 consumer 가 실제로 템플릿 렌더링(이름 합성)을 지원하는지 미확인** → 미지원이면 (a) consumer 에 렌더링 추가 또는 (b) 본 작업에서 body 평문 저장하되 PII 노출 위험 수용 여부를 결정해야 함(D2).
- **IDOR**: 시나리오 B 노드 관리자 산출은 신청자의 `CMPNY_CD`+`SITE_CD`+`NODE_CD` 스코프로 격리(노무수령거부 매퍼와 동일). 신청자가 보낸 nodeCd 를 그대로 신뢰하지 말고 서버 세션/요청 row 기준으로 산출.
- **트랜잭션 격리**: outbox INSERT 실패가 연차 신청/결재 본 흐름을 롤백시키지 않도록 예외 격리(노무수령거부 체크인 hook 패턴 참조). 단 같은 @Transactional 내 INSERT 면 정합성과 롤백 정책을 명시.

---

## 6. 분해 단계 결정 사항 (planner)

| # | 결정 포인트 | 후보 |
|---|---|---|
| D1 | 시나리오 B 에 `fullyAutoApproved`(결재 Y지만 전건 자동승인 즉시확정, 215줄) 포함 여부 | 포함(=결재 없이 확정과 동일) vs 제외(명목상 결재선 존재) |
| D2 | 신청자명 PII 처리 | consumer 렌더링 합성(권장) vs body 평문 저장 |
| D3 | 시나리오 A 발송 단위 | 단계 1인만(확정) — 확인용 |
| D4 | 앱 결재 처리 경로 존재 여부 | 현재 연차 승인은 웹 `approveStep` 중심. 앱 결재함에서 승인하는 경로가 생기면 그 hook 도 포함 |
| D5 | DATA_PAYLOAD 스펙 | 앱 라우팅용 `type`/`reqId`/`leaveId` 등 키 확정(앱 수신측과 동시 명세) |

---

## 7. 의존성

- **선행(운영)**: prafta-031 / com-001 / com-002 마이그레이션 — **사용자 확인 결과 모두 운영 반영 완료**(2026-06-03). 본 작업 마이그(SYS045 시드 + 신규 매퍼)만 추가.
- **발송 실동작 전제**: prafta-com-002 워커 게이트(`prafta.push.worker.enabled`)가 켜져 있어야 하고, `tb_user_device.PUSH_TOKEN` 이 채워져 있어야 실제 단말 도달. (앱 토큰 등록이 미구현이면 outbox 적재까지만 동작 — com-003 A-3 / 별도 follow-up.)
- **병행**: prafta-app-009(근태/OT/스케줄 결재선 PUSH) — 동일 PUSH 유형이나 결재선 인프라 선행 필요.

---

## 8. 정책서 출처 (분해 시 정독)

- 공통 정책서 §10 (알림/공지 — 발송 채널·중복 방지)
- `attd/09-requests-approval.md` §9.5 (자기 승인 원칙 — 자동승인 단계 PUSH 제외 근거)
- `common/08-permissions.md` §8.4 (조직 스코프 — 노드 관리자 가시 범위)
