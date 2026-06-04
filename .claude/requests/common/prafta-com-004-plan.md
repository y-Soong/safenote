# prafta-com-004 — 연차 결재 PUSH 알림 (분해 plan / 단일 출처)

> **작업 ID prefix**: `PRAFTA-COM-004`
> **영역**: 공통 백엔드(outbox 생산자 추가). 화면(Vue) 작업 없음.
> **본 문서 위상**: planner 분해 결과 단일 출처. developer/qa/security 는 본 문서의 작업 단위 · 정책서 출처 · 검증 기준을 따른다.
> **요청서**: `.claude/requests/common/prafta-com-004.md` (정독 완료)
> **작성일**: 2026-06-03

---

## 0. 범위 한 줄 요약

연차 도메인에 PUSH 생산자 2종(outbox PENDING 적재)을 추가한다. 시나리오 A = 결재 차례가 도래한 단계의 결재자 1인에게, 시나리오 B = 무결재 즉시확정 연차의 신청자 소속 노드 main/sub 관리자에게. 실제 FCM 발송 활성화는 본 작업 범위 밖(com-002 워커 재사용, 게이트 OFF 유지).

---

## 1. 사용자 확정 결정 (재질문 금지, 그대로 반영)

| # | 결정 | 반영 위치 |
|---|---|---|
| C1 | 시나리오 A = `01(신청)`로 전환되는 단계의 `APPROVER_USER_CD` **1인만**. 자동승인 skip 단계·본인 단계·반려·최종승인은 제외. master/hr 자동포함 없음. | T3 |
| C2 | 시나리오 B = `aprvRequired=false` 즉시확정 연차를 **신청자 소속 노드 main/sub 관리자에게만**(master/hr 제외). `recordDirectLeaveUsage`(Attd_05 셀 직접입력)는 PUSH 제외. | T2, T4 |
| C3 | **PII = (a)안**: 본문에 신청자 실명 평문 포함. consumer 무수정. `tb_noti_outbox.BODY` 평문 저장 허용(사용자 승인). | T3, T4 |
| C4 | 본 작업은 생산자(PENDING 적재)까지만 + 단위테스트. FCM 실발송(앱 토큰 등록 + 워커 게이트 ON)은 범위 밖. | T6 |

---

## 2. planner 코드 조사로 확정된 사실 (재조사 불필요)

### 2-1. ⚠️ PII 사실 정정 (요청서 §5 / 결정 #3 의 전제 오류 — 중요)

요청서 §5 와 사용자 결정 #3 은 "신청자명이 `TB_USER` AES-GCM 암호화 컬럼이므로 INSERT 시점에 복호화하여 본문 합성"이라고 전제했으나, **이는 사실과 다르다.**

- `schema-full.sql` 293줄: `TB_USER.USER_NM varchar(50) NOT NULL COMMENT '사용자명'` — **평문 컬럼**이다.
- `LeaveDashboardMapper.xml` 149줄 주석: "USER_NM은 평문 varchar(utf8mb4_unicode_ci) 컬럼이라 콜레이션 기준 정렬이 가나다·ABC와 일치한다" — 평문 확정.
- AES-GCM 암호화 대상은 휴대폰(`MBL_NO_ENC`/`MBL_NO_HMAC`, 208~210줄)이며 **이름은 암호화 대상이 아니다.**

→ **결론**: 본문에 실명을 넣기 위한 **복호화 로직은 불필요**하다. 단순 `SELECT USER_NM`(CMPNY_CD+USER_CD 스코프)으로 평문 이름을 얻어 본문에 합성한다. 결정 #3(a안 = 본문 평문 저장)의 방향은 그대로 유효하되, 구현은 "복호화"가 아니라 "평문 조회"다. developer 는 복호화 유틸(AesGcmUtil)을 호출하지 말 것.
> PII 노출 수위: USER_NM 은 이미 평문 저장된 데이터이므로 outbox.BODY 에 합성해도 신규 평문 PII 생성이 아니다(동일 분류 데이터의 재기록). security 는 이 점을 근거로 검토한다.

### 2-2. 발송 hook 위치 (확정)

| hook | 파일 / 위치 | 시나리오 |
|---|---|---|
| 웹 신청 첫 단계 지정 | `web LeaveFlowServiceImpl.submitLeave` 182~198줄(`i == currentIdx` 일 때 `STEP_APPLIED`, 194줄) | A |
| 웹 다음 단계 진행 | `web LeaveFlowServiceImpl.approveStep` 250~252줄(`selectFirstWaitingStep` → `STEP_APPLIED`) | A |
| 웹 무결재 즉시확정 | `web LeaveFlowServiceImpl.submitLeave` `aprvRequired==false` 분기(156줄 reqStatus=REQ_APPROVED), insertLeaveUse(211줄) 직후 | B |
| 앱 신청 첫 단계 지정 | `app AppLeaveFlowServiceImpl.submitLeave` 363~379줄(웹 미러, `i == currentIdx`) | A |
| 앱 무결재 즉시확정 | `app AppLeaveFlowServiceImpl.submitLeave` `aprvRequired==false`, insertLeaveUse(392줄) 직후 | B |
| 앱 다음 단계 진행 | **앱에는 연차 승인(approveStep) 경로가 없다.** `AppLeaveFlowServiceImpl` 은 신청·메타·결재자검색만 보유. 연차 승인은 웹 `approveStep` 단일 경로. → **시나리오 A 의 "다음 단계 진행" hook 은 웹 approveStep 1곳만** (결정 D4 자동 해소). | — |

> 즉 A 발송 지점 = **웹 submitLeave 첫 단계 + 앱 submitLeave 첫 단계 + 웹 approveStep 다음 단계** (3곳). B 발송 지점 = **웹 submitLeave + 앱 submitLeave 무결재 분기** (2곳). `recordDirectLeaveUsage`(REQ_ID=NULL) 는 제외(C2).

### 2-3. fullyAutoApproved 판정 (D1 결정 근거)

`submitLeave` 의 `fullyAutoApproved`(웹 199줄 / 앱 380줄) = 결재선이 전부 본인 자동승인이라 `currentIdx < 0` 인 경우. 215줄(웹)/396줄(앱)에서 REQ_APPROVED 로 즉시 확정된다. **§9.5(자기 승인 원칙)** 상 이 케이스는 "명목상 결재선이 존재하고 본인이 노드 담당 정/부(관리자급)로서 자가승인"한 것이다. → **D1 결정**: §3 참조(제외).

### 2-4. 재사용 자산 (신규 SQL 난립 금지)

- `NotiOutboxInsertVO`(`com.prafta.common.cmm.leave.vo`) — outbox 1행 운반체. 그대로 사용.
- `LeaveDashboardMapper.insertNotiOutbox`(LeaveDashboardMapper.xml 506줄) + `selectNextNotiId`(497줄, `CONCAT('N', YYYYMMDD, FNC_CMM_SEQ_NEXTVAL(cmpnyCd,'NOTI_OUTBOX_ID'))`) — 그대로 재사용.
- 노드 관리자 산출 패턴 = `LeaveRefusalMapper.selectSiteRefusalAdmins`(LeaveRefusalMapper.xml 121~173줄)의 **(2)번 서브쿼리**(TB_SITE_NODE MAIN_ADMIN_CD/SUB_ADMIN_CD ∩ 대상자 NODE_CD)만 분리. (1)번 역할 UNION(master/hr)은 제외 → 신규 매퍼 `selectNodeAdmins`.
- consumer = com-002 `PushSenderServiceImpl`(title/body 그대로 FCM 전송, 렌더링 없음). **무수정**. → 본문 평문 합성을 생산자 측에서 완성해야 함(C3과 정합).
- 예외 격리 패턴 = `LeaveRefusalDetectServiceImpl`(@Transactional 미부여 + 내부 try-catch 흡수 + dedup 멱등). 본 작업 hook 도 동일 패턴.

---

## 3. 미결정 항목 결론 (planner 판단)

- **D1 (시나리오 B 에 fullyAutoApproved 포함 여부)** → **제외 확정.**
  근거: §9.5(자기 승인 원칙)상 fullyAutoApproved 는 "명목상 결재선이 존재"하고 본인이 노드 담당 정/부(관리자)로서 자가승인한 케이스다. C2 의 시나리오 B 정의("결재 없이 즉시 확정 = `aprvRequired=false`")와 다르다. 또한 신청자 본인이 이미 관리자급이어서 "소속 노드 관리자에게 통보"의 실익이 약하다. → 시나리오 B 발송 조건 = **순수 `aprvRequired == false` 1건만**. fullyAutoApproved 분기에서는 어떤 PUSH 도 발송하지 않는다(시나리오 A 도 발송할 "차례 도래" 단계가 없으므로 양쪽 모두 미발송 — 의도된 결과).

- **D2 (PII)** → 2-1 참조. 복호화 불필요(USER_NM 평문). (a)안(평문 BODY) 방향 유지. consumer 무수정.

- **D3 (시나리오 A 발송 단위)** → 단계 1인 확정(C1).

- **D4 (앱 결재 처리 경로)** → 2-2 참조. 앱에 연차 승인 경로 없음 → A 의 "다음 단계" hook 은 웹 approveStep 1곳만.

- **D5 (DATA_PAYLOAD 스펙)** → §6 참조(앱 라우팅 키 확정).

- **트랜잭션 격리** → 예외 격리 채택. hook 은 본 흐름(submitLeave/approveStep)의 `@Transactional` **내부**에서 호출하되, **hook 자체를 try-catch 로 감싸 outbox 적재 실패가 연차 본 흐름을 롤백/실패시키지 않게** 한다(노무수령거부 패턴). dedup UNIQUE(CMPNY_CD, DEDUP_KEY)로 멱등 보장. (단, 본 흐름이 롤백되면 같은 트랜잭션 내 outbox INSERT 도 함께 롤백되어 "유령 알림"이 남지 않는 이점도 동시에 확보 — 격리는 어디까지나 hook 예외가 본 흐름으로 전파되지 않게 하는 방향.)

---

## 4. 작업 분해

### PRAFTA-COM-004-1 — SYS045 알림 유형 2건 시드 마이그레이션

- **유형**: backend (DB 마이그레이션)
- **영역**: web (공통)
- **모듈**: cmm/leave
- **작업 유형**: 신규
- **요구사항 요약**: `tb_syst_val_d` 에 SYS045 디테일 2건(차례 도래 / 무결재 통보) 추가.
- **상세 설명**:
  - [Phase 1]
  - 핵심 요구사항:
    1) `LEAVE_APPROVAL_TURN` (연차 결재 차례 도래, 결재자 대상) 추가.
    2) `LEAVE_USED_NO_APRV` (무결재 연차 사용 통보, 노드 관리자 대상) 추가.
    3) 두 건 모두 `VAL_D_INFO_1='PUSH'`, `USE_YN='Y'`. SORT_IDX 는 기존 최대값+1 / +2.
  - 영향 받는 파일: `prafta-backend/src/main/resources/sql/migration/prafta-com-004-sys045-noti-type.sql` (신규)
  - 형식: prafta-031-sys045-noti-type.sql 와 동일. SYS045 마스터(`tb_syst_val_m`)는 이미 존재하므로 **디테일(`tb_syst_val_d`)만 INSERT**. 적용 전 부재 확인 SELECT 주석 포함. 멱등 안내(이미 존재 시 건너뜀) 주석 포함.
  - 정책서 출처: 공통 §10.1(채널=PUSH), §10.3(중복 방지 — NOTI_TYPE 카탈로그).
- **선행 작업**: 없음 (단, 운영 적용은 코드 배포 전 선행 — §7 참조)
- **우선순위 근거**: 코드가 참조하는 코드값 카탈로그. 최우선.
- **검증 기준**:
  - `SELECT 1 FROM tb_syst_val_m WHERE SYST_VAL_CD='SYS045'` 가 1행(이미 존재) → 마스터 INSERT 생략 확인.
  - 디테일 2건 PK = (SYS045, LEAVE_APPROVAL_TURN) / (SYS045, LEAVE_USED_NO_APRV) 중복 없음.

---

### PRAFTA-COM-004-2 — 노드 관리자 산출 매퍼 `selectNodeAdmins`

- **유형**: backend (Mapper)
- **영역**: web (공통)
- **모듈**: cmm/leave
- **작업 유형**: 신규
- **요구사항 요약**: 신청자 소속 노드의 main/sub 관리자 USER_CD 목록 산출(역할 master/hr 제외).
- **상세 설명**:
  - [Phase 1]
  - 핵심 요구사항:
    1) `LeaveRefusalMapper.selectSiteRefusalAdmins` 의 (2)번 노드 서브쿼리만 분리한 신규 메서드 `selectNodeAdmins(cmpnyCd, siteCd, userCd)` 작성. (1)번 역할 UNION 제거.
    2) 결과 = 대상 근로자 NODE_CD 매칭 `TB_SITE_NODE` 의 MAIN_ADMIN_CD ∪ SUB_ADMIN_CD, DISTINCT, 활성 계정만(`USE_YN='Y'`, `ACCOUNT_STATUS='01'`).
    3) `NULLIF(TRIM(...),'') IS NOT NULL` 로 빈 관리자 칸 제외(원본 동일).
    4) CMPNY_CD + SITE_CD + 대상자 USER_CD(→NODE_CD 조인) 스코프 격리.
  - 영향 받는 파일:
    - `prafta-backend/src/main/java/com/prafta/common/cmm/leave/mapper/LeaveRefusalMapper.java` (메서드 추가) **또는** 신규 전용 매퍼 `LeaveApprovalNotiMapper`. → **권장: 신규 매퍼 `LeaveApprovalNotiMapper`** (노무수령거부와 도메인 분리, 향후 com-004 전용 SQL 응집). developer 판단으로 LeaveRefusalMapper 재사용해도 무방하나, 본 plan 은 신규 매퍼를 기준으로 한다.
    - `prafta-backend/src/main/resources/com/prafta/common/cmm/leave/mapper/LeaveApprovalNotiMapper.xml` (신규)
  - 예상 산출물: Mapper 인터페이스 + XML `selectNodeAdmins`
  - 정책서 출처: 공통 §8.4.1(소속 사업장 — 노드 담당 정/부 관리 범위), §10.3(오발송 방지 — 조직 스코프).
- **선행 작업**: 없음
- **우선순위 근거**: 시나리오 B 수신자 산출의 필수 의존.
- **검증 기준**:
  - 동일 입력에 대해 `selectSiteRefusalAdmins` 결과에서 master/hr(역할 UNION)만 빠진 부분집합과 일치.
  - 타 사업장/타 회사 NODE_CD 의 관리자가 결과에 섞이지 않음(IDOR 가드 — security 필수 검토).
  - 신청자가 어떤 노드에도 속하지 않거나 관리자 칸이 비면 빈 리스트 반환(no-op 허용).

---

### PRAFTA-COM-004-3 — 공용 상수/메시지 빌더 + 시나리오 A·B 적재 서비스

- **유형**: backend (Service + 상수)
- **영역**: web (공통, `com.prafta.common.cmm.leave`)
- **모듈**: cmm/leave
- **작업 유형**: 신규
- **요구사항 요약**: 연차 결재 PUSH 공용 상수(`LeaveApprovalNotiConst`) + 메시지 빌더 + outbox 적재 서비스(`LeaveApprovalNotiService`) 신설. web/app 양쪽 hook 이 공용 호출.
- **상세 설명**:
  - [Phase 1]
  - 핵심 요구사항:
    1) **상수 클래스** `LeaveApprovalNotiConst` (LeaveRefusalConst 패턴): SYS045 코드 2종, CHANNEL_PUSH, SEND_STATUS_PENDING, 제목/본문 템플릿 상수.
       - A 제목: `"[연차 결재 요청]"`, A 본문 템플릿: `"%s님이 신청한 연차 결재를 기다리고 있습니다."` (`%s`=신청자명 평문)
       - B 종일 본문: `"%s님이 %s 연차 %s일을 사용했습니다."` (신청자명 / YYYY-MM-DD / 일수)
       - B 시간차 본문: `"%s님이 %s %s~%s 시간차 연차를 사용했습니다."` (신청자명 / 날짜 / START HH:MM / END HH:MM)
       - B 반차 본문: `"%s님이 %s 반차를 사용했습니다."` (신청자명 / 날짜)
       - 문구는 상수화하여 추후 노무 검토 시 교체 용이하게 한다(LeaveRefusalConst 와 동일 철학).
    2) **메시지 빌더**: USE_UNIT_TYPE(00종일/01반차/02·03·04 시간차)·LEAVE_DAYS·START_TIME/END_TIME(HHMM→HH:MM 포맷)·workYmd(YYYYMMDD→YYYY-MM-DD)로 본문 합성. 신청자명은 평문 `USER_NM` 조회값(2-1).
    3) **서비스** `LeaveApprovalNotiService` (구현 `LeaveApprovalNotiServiceImpl`, `@Transactional` 미부여 — 예외 격리):
       - `notifyApprovalTurn(cmpnyCd, siteCd, applicantUserCd, applicantNm, reqId, approvalStep, approverUserCd, insertNo)` — 시나리오 A. 결재자 1인에게 outbox 1건. dedupKey = `"LV_TURN_" + reqId + "_" + approvalStep`.
       - `notifyLeaveUsedNoAprv(cmpnyCd, siteCd, applicantUserCd, applicantNm, leaveId, useUnitType, leaveDays, workYmd, startTime, endTime, insertNo)` — 시나리오 B. `selectNodeAdmins` 로 산출한 관리자별 outbox 1건. dedupKey = `"LV_USED_" + leaveId + "_" + targetUserCd`.
       - 두 메서드 모두 내부 try-catch 로 예외 흡수(로그만). 관리자/결재자 0명이면 no-op + 로그.
       - **신청자 본인이 수신 대상에 포함되면(시나리오 B 에서 신청자=노드 관리자) 제외**(자기 알림 방지 — 빌더에서 `targetUserCd.equals(applicantUserCd)` 스킵).
    4) **신청자명 조회**: hook 호출부(submitLeave/approveStep)에서 평문 `USER_NM` 을 조회하여 서비스에 전달하거나, 서비스가 직접 조회. → **서비스가 직접 조회** 권장(호출부 단순화). 신규 매퍼 메서드 `selectUserNm(cmpnyCd, userCd)`(평문, CMPNY_CD 스코프) 를 `LeaveApprovalNotiMapper` 에 추가. **AesGcmUtil 복호화 호출 금지**(2-1).
    5) **DATA_PAYLOAD**: Jackson `ObjectMapper` 로 JSON 직렬화(LeaveRefusalDetectServiceImpl 패턴). 신청자명 평문은 payload 에 **넣지 않음**(BODY 에만, payload 는 라우팅 키만 — §6).
  - 영향 받는 파일:
    - `prafta-backend/src/main/java/com/prafta/common/cmm/leave/service/LeaveApprovalNotiConst.java` (신규)
    - `prafta-backend/src/main/java/com/prafta/common/cmm/leave/service/LeaveApprovalNotiService.java` (신규 인터페이스)
    - `prafta-backend/src/main/java/com/prafta/common/cmm/leave/service/impl/LeaveApprovalNotiServiceImpl.java` (신규)
    - `LeaveApprovalNotiMapper.java`/`.xml` (PRAFTA-COM-004-2 에서 생성, `selectUserNm` 추가)
    - 재사용: `LeaveDashboardMapper.insertNotiOutbox` / `selectNextNotiId`, `NotiOutboxInsertVO`
  - 정책서 출처: 공통 §10.1(PUSH 채널), §10.2(요청 등록→승인권자 알림 트리거), §10.3(중복 방지), §8.4.1(노드 관리 범위), `attd/09-requests-approval.md` §9.5(자기 승인 원칙 — 자동승인 단계 PUSH 제외 근거).
- **선행 작업**: PRAFTA-COM-004-2 (selectNodeAdmins / selectUserNm)
- **우선순위 근거**: 적재 로직 본체. hook 보다 선행.
- **검증 기준**: T6 단위테스트로 검증(아래).

---

### PRAFTA-COM-004-4 — 웹 hook 연결 (`LeaveFlowServiceImpl`)

- **유형**: backend (Service 수정)
- **영역**: web
- **모듈**: web/attd/leaveflow
- **작업 유형**: 보완
- **요구사항 요약**: 웹 연차 신청/승인 흐름에 PUSH 적재 hook 삽입.
- **상세 설명**:
  - [Phase 1]
  - 핵심 요구사항:
    1) `submitLeave` — 결재 라인 생성 루프(182~198줄)에서 `i == currentIdx` 인 단계(=STEP_APPLIED)의 결재자에 대해, 해당 단계가 **본인 자동승인이 아닐 때만** 시나리오 A 호출(`notifyApprovalTurn`, approvalStep = currentIdx+1). `fullyAutoApproved`(currentIdx<0)면 A 미발송.
    2) `submitLeave` — `aprvRequired == false` 분기(즉시확정)이면, insertLeaveUse(211줄) 이후 시나리오 B 호출(`notifyLeaveUsedNoAprv`). **fullyAutoApproved 는 B 미발송(D1)**. 즉 B 조건은 `!aprvRequired` 단독.
    3) `approveStep` — `selectFirstWaitingStep` 이 다음 단계를 찾아 STEP_APPLIED 로 전환(250~252줄)할 때, 그 단계 결재자에 시나리오 A 호출(`notifyApprovalTurn`, approvalStep = nextStep). 최종 승인(nextStep == null)·반려는 미발송.
    4) hook 호출은 모두 try-catch 로 감싸 본 흐름에 예외 전파 금지(서비스 내부에서도 흡수하지만 호출부도 방어).
    5) 신청자명: 서비스가 직접 조회(PRAFTA-COM-004-3 #4)하므로 호출부는 식별자만 전달.
    6) `notifyApprovalTurn` 에 넘길 approverUserCd: submitLeave 는 `approvers.get(currentIdx)`, approveStep 은 nextStep 단계의 결재자(`selectApprovalStep` 또는 단계 결재자 조회). **단계 번호↔결재자 매핑 정확성 주의**(approvalStep 은 1-based, 리스트 인덱스는 0-based — req07 위치밀림 버그 교훈).
  - 영향 받는 파일:
    - `prafta-backend/src/main/java/com/prafta/web/attd/leaveflow/service/impl/LeaveFlowServiceImpl.java` (의존성 주입 + hook 3곳)
  - 정책서 출처: `attd/09-requests-approval.md` §9.5, §9.2/§9.4(연차 결재 흐름), 공통 §10.2.
- **선행 작업**: PRAFTA-COM-004-3
- **우선순위 근거**: 법적 책임 영역(attd) +1 격상. 핵심 발송 경로.
- **검증 기준**:
  - 결재선 [A자동(본인), B, C] → 신청 시 B 에게만 A-PUSH 1건(A자동 skip, C 는 WAIT). B 승인 → C 에게 A-PUSH. C 승인(최종) → 무발송.
  - 무결재 연차 신청 → 신청자 소속 노드 관리자에게 B-PUSH. 반차/시간차/종일 본문 분기 정확.
  - fullyAutoApproved(전건 본인 자동승인) → A·B 모두 무발송.
  - approvalStep↔approverUserCd 매핑 정확(엉뚱한 결재자에게 발송 금지) — qa 가 인덱스 경계 도전.

---

### PRAFTA-COM-004-5 — 앱 hook 연결 (`AppLeaveFlowServiceImpl`)

- **유형**: backend (Service 수정)
- **영역**: app
- **모듈**: app/leave/leaveflow
- **작업 유형**: 보완
- **요구사항 요약**: 앱 연차 신청 흐름에 PUSH 적재 hook 삽입(신청 첫 단계 A + 무결재 B). 앱에 연차 승인 경로 없으므로 "다음 단계 진행" hook 없음.
- **상세 설명**:
  - [Phase 1]
  - 핵심 요구사항:
    1) `submitLeave` — 결재 라인 생성 루프(363~379줄)의 `i == currentIdx` 단계(본인 자동승인 아님)의 결재자에 시나리오 A 호출(`notifyApprovalTurn`, approvalStep = currentIdx+1). fullyAutoApproved 면 미발송.
    2) `submitLeave` — `aprvRequired == false` 즉시확정 분기(insertLeaveUse 392줄 이후)에 시나리오 B 호출. fullyAutoApproved 제외(D1).
    3) 호출부 try-catch 방어. 신청자명 식별자만 전달(서비스 직접 조회).
    4) 앱 submitLeave 는 `@Transactional(rollbackFor = Exception.class)` 이므로 hook 예외가 본 트랜잭션으로 전파되지 않도록 반드시 흡수.
  - 영향 받는 파일:
    - `prafta-backend/src/main/java/com/prafta/app/leave/leaveflow/service/impl/AppLeaveFlowServiceImpl.java` (의존성 주입 + hook 2곳)
  - 정책서 출처: `attd/09-requests-approval.md` §9.4(연차 신청 — 모바일 앱), §9.5, 공통 §10.2.
- **선행 작업**: PRAFTA-COM-004-3
- **우선순위 근거**: 법적 책임 영역(attd) +1. 앱 신청 경로 커버.
- **검증 기준**:
  - 앱 신청(결재 Y) → 첫 수동 결재자에게 A-PUSH 1건.
  - 앱 무결재 신청 → 신청자 소속 노드 관리자에게 B-PUSH. 본문 단위별 분기.
  - 앱은 승인 경로 부재이므로 "다음 단계" 발송이 일어나지 않음(웹 approveStep 가 단일 경로). 추후 앱 결재함 구현 시 hook 추가 follow-up.

---

### PRAFTA-COM-004-6 — 단위테스트

- **유형**: backend (Test)
- **영역**: web/app (공통)
- **모듈**: cmm/leave
- **작업 유형**: 신규
- **요구사항 요약**: 메시지 빌더 + 적재 서비스 동작을 단위테스트로 검증(생산자 적재까지, 실발송 제외).
- **상세 설명**:
  - [Phase 1]
  - 핵심 요구사항:
    1) **메시지 빌더 테스트**: 종일/반차/시간차(02·03·04) 각 본문 포맷 정확(날짜 YYYY-MM-DD, 시각 HH:MM, 일수 표기). 신청자명 평문 합성.
    2) **시나리오 A 테스트**(`LeaveApprovalNotiServiceImpl` mock 매퍼): `notifyApprovalTurn` → outbox 1건(targetUserCd=결재자, notiType=LEAVE_APPROVAL_TURN, dedupKey=`LV_TURN_{reqId}_{step}`). 동일 키 재호출 시 DuplicateKeyException 흡수(멱등 no-op).
    3) **시나리오 B 테스트**: `notifyLeaveUsedNoAprv` → `selectNodeAdmins` 반환 관리자 수만큼 outbox(각 dedupKey=`LV_USED_{leaveId}_{admin}`). 신청자 본인이 관리자 목록에 있으면 제외. 관리자 0명 → no-op.
    4) **예외 격리 테스트**: insertNotiOutbox 가 예외를 던져도 서비스가 흡수(상위로 전파 안 함). qa 가 hook 호출부(submitLeave)에서 본 흐름 비롤백을 추가 검증.
    5) **D1 회귀 테스트**: fullyAutoApproved 케이스에서 A·B 어떤 outbox 도 적재되지 않음(hook 미호출 또는 조건 차단). → 이 검증은 LeaveFlowServiceImpl/AppLeaveFlowServiceImpl 레벨 테스트가 더 적합(서비스 단독으론 조건 분기 검증 불가). developer 판단으로 hook 조건을 별도 헬퍼로 추출해 테스트 가능하게 할 수 있음.
  - 영향 받는 파일:
    - `prafta-backend/src/test/java/com/prafta/common/cmm/leave/service/impl/LeaveApprovalNotiServiceImplTest.java` (신규)
    - (선택) hook 조건 회귀: 기존 leaveflow 테스트 보강
  - 정책서 출처: §10.3(중복 방지 검증 = dedup 멱등), §9.5(자동승인 제외 검증).
- **선행 작업**: PRAFTA-COM-004-3, 4, 5
- **우선순위 근거**: 결정 C4(생산자 단위테스트 검증 필수).
- **검증 기준**: `gradlew.bat test --tests *LeaveApprovalNotiServiceImplTest*` 통과(타임아웃 600초). 빌드 인코딩 주의(주석 한국어 OK, dev-facing 문자열은 CLAUDE.md 인코딩 메모 준수).

---

## 5. 의존 순서 (DAG)

```
COM-004-1 (SYS045 시드)        ── 독립 (단, 운영 선적용 필수)
COM-004-2 (selectNodeAdmins)  ── 독립
        │
        ▼
COM-004-3 (상수+빌더+서비스)   ── 2 의존
        │
        ├──▶ COM-004-4 (웹 hook)
        ├──▶ COM-004-5 (앱 hook)
        │
        ▼
COM-004-6 (단위테스트)         ── 3,4,5 의존
```

구현 권장 순서: **1 → 2 → 3 → (4 ∥ 5) → 6**.

---

## 6. DATA_PAYLOAD 스펙 (D5 — 앱 수신측 라우팅용)

LeaveRefusalDetectServiceImpl `buildAlertPayload` 패턴(Jackson 직렬화, 실패 시 `{}` 폴백). **신청자명 평문은 payload 에 미포함**(BODY 에만). 앱 라우팅 키만:

| 시나리오 | payload 키 |
|---|---|
| A (차례 도래) | `{"type":"LEAVE_APPROVAL_TURN","reqId":"...","approvalStep":N,"applicantUserCd":"..."}` |
| B (무결재 통보) | `{"type":"LEAVE_USED_NO_APRV","leaveId":"...","applicantUserCd":"...","workYmd":"YYYYMMDD"}` |

> 실제 앱 수신 라우팅 화면 연결(딥링크/탭 이동)은 **토큰 인프라 작업(앱 PUSH_TOKEN 등록 + 워커 ON)과 함께 별도 작업**으로 분리한다(C4). 본 작업은 payload 키만 확정·적재한다. 앱 FE 변경 없음.

---

## 7. 의존성 / 운영 주의

- **선행 운영 반영 완료**(2026-06-03 사용자 확인): prafta-031 / com-001 / com-002 마이그레이션. `tb_noti_outbox`, SYS045 마스터, 공용 FCM 워커 인프라 존재.
- **본 작업 마이그(PRAFTA-COM-004-1) 선적용 필수**: SYS045 디테일 2건이 없으면 outbox INSERT 시 NOTI_TYPE 가 카탈로그에 없는 값으로 적재된다(FK 강제 여부는 스키마 확인 필요 — 강제면 INSERT 실패 → 예외 격리로 알림만 누락, 본 흐름은 보호됨). **코드 배포 전 시드 적용** 권장.
- **실발송 도달 = 0 (현 상태)**: com-002 워커 게이트(`prafta.push.worker.enabled`) 기본 OFF + `tb_user_device.PUSH_TOKEN` 0건. 본 작업은 outbox 적재까지만 검증. 실단말 도달은 앱 토큰 등록 + 워커 ON(별도 작업) 후.
- **병행**: prafta-app-009(근태/OT/스케줄 결재선 PUSH) — 동일 PUSH 유형, 결재선 인프라 선행 필요(본 작업과 독립).

---

## 8. 보안 검토 포인트 (security 에이전트)

1. **PII**: USER_NM 은 평문 컬럼(2-1). BODY 평문 합성은 신규 PII 평문 생성이 아님(동일 분류 데이터 재기록, 사용자 승인 C3). AesGcmUtil 복호화 호출이 코드에 없는지 확인.
2. **IDOR**: `selectNodeAdmins` 가 신청자 CMPNY_CD+SITE_CD+NODE_CD 스코프로 격리되는지(타 사업장 관리자 누수 차단). 신청자 식별자는 서버 세션/요청 row 기준(본문 nodeCd 비신뢰 — 앱 submitLeave 는 이미 nodeCd=null 저장, 327~331줄).
3. **자기 알림 방지**: 시나리오 B 에서 신청자 본인이 노드 관리자인 경우 수신 제외 처리 확인.
4. **트랜잭션/멱등**: hook 예외가 연차 본 흐름을 실패/롤백시키지 않음 + dedup UNIQUE 로 중복 발송 방지(§10.3).
5. **payload PII 미포함**: DATA_PAYLOAD 에 평문 이름 미포함(BODY 한정) 확인.

---

## 9. 분해 결과 요약

**작업 목록(6건, 모두 backend, 화면 작업 없음):**
1. PRAFTA-COM-004-1 — SYS045 디테일 2건 시드 마이그레이션
2. PRAFTA-COM-004-2 — `selectNodeAdmins` (+ `selectUserNm`) 노드 관리자/이름 매퍼
3. PRAFTA-COM-004-3 — `LeaveApprovalNotiConst` + 메시지 빌더 + `LeaveApprovalNotiService(Impl)` 적재 서비스
4. PRAFTA-COM-004-4 — 웹 `LeaveFlowServiceImpl` hook 3곳(submitLeave 첫단계 A / submitLeave 무결재 B / approveStep 다음단계 A)
5. PRAFTA-COM-004-5 — 앱 `AppLeaveFlowServiceImpl` hook 2곳(submitLeave 첫단계 A / submitLeave 무결재 B)
6. PRAFTA-COM-004-6 — 단위테스트(빌더/적재/멱등/예외격리/D1 회귀)

**핵심 결정:**
- D1: 시나리오 B 는 **순수 aprvRequired=false 만**. fullyAutoApproved 제외(§9.5 근거).
- D4: 앱에 연차 승인 경로 없음 → A "다음 단계" hook 은 **웹 approveStep 1곳만**.
- PII: USER_NM 은 **평문 컬럼**(요청서 §5/결정#3 의 "AES-GCM 복호화" 전제는 오류). 복호화 불필요, 평문 SELECT 로 본문 합성. (a)안 방향 유지.
- 트랜잭션: hook 예외 격리(본 흐름 비롤백) + dedup 멱등.
- recordDirectLeaveUsage(Attd_05 셀 직접입력)는 PUSH 제외(C2).

**잔여 결정필요 / follow-up (작업 분해는 막지 않음):**
- 앱 수신측 PUSH 라우팅(딥링크/탭 이동) — 별도 작업(앱 토큰 등록 + 워커 ON 과 함께).
- 앱 연차 결재함(승인) 구현 시 A "다음 단계" hook 앱 추가 — follow-up.
- prafta-app-009(근태/OT/스케줄 결재선 PUSH) — 별도.
- `tb_noti_outbox.NOTI_TYPE` 의 SYS045 FK 강제 여부 스키마 확인(강제면 시드 선적용 더욱 critical) — developer 가 마이그 적용 전 확인.
