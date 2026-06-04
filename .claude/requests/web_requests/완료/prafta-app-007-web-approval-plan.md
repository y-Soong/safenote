# prafta-app-007-web-approval — 웹 관리자 스케줄 수정 요청(REQ_TYPE='10') 승인/반려 구현 분해 plan

> **작업 ID prefix**: `PRAFTA-APP-007-WEB`
> **작업 영역**: 웹/백엔드 (PRAFTA/prafta-backend 웹 패키지 `com.prafta.web.attd.attd07` + PRAFTA/prafta-web-frontend `AttdDayDetailPop.vue`). 모바일 앱 변경 없음.
> **단일 출처(SSOT)**: 본 plan 이 본 작업의 단일 출처이다. 후속 developer/qa/security 는 본 plan + 명시된 정책서 섹션만 정독하면 된다.
> **연계 출처(참고만, 폐기 아님)**: `.claude/requests/app_requests/prafta-app-007-plan.md` (앱 등록 측 SSOT). 본 plan 은 그 plan 의 P8 "관리자 승인 시 SCH_CD 로 스케줄 변경" 의도를 웹 구현으로 완성한다.
> **메인 세션 위임**: planner(본 세션)는 Notion 접근 불가. Notion 등록은 메인 세션이 §8 의 항목으로 대행한다.

---

## 0. 확정된 진단 요약 (코드+DB+정책서로 검증, 추측 아님)

1. **앱은 등록만 한다.** `com.prafta.app.req.req07.service.impl.AppReq07ServiceImpl#registerSchedModify` 가 REQ_TYPE='10'(SYS032 신규), REQ_STATUS='01', TARGET_ID=null, WORK_SEQ(slot별), SCH_CD=목표 스케줄코드, START/END_DATE/TIME 전부 null 로 `tb_user_attd_req` 에 INSERT 한다. 현재 앱은 단일 스케줄(WORK_SEQ=1 단건)로 제출(메모리 `project_prafta_app_sched_modify_single`).
2. **웹에 승인/반려 처리 경로가 0건.** grep 확인: 전 백엔드에 `REQ_TYPE_SCHED_MODIFY="10"`(`com.prafta.web.attd.attd07.util.AttdReqTypeUtils`)을 승인/반려하는 서비스/엔드포인트가 없다. 등록 측(app req07)에서만 사용.
3. **기존 웹 근태 승인/반려 엔드포인트는 '10'을 fail-closed 로 거부한다.**
   - `Attd07ServiceImpl#updateUserAttdRequest`(승인)·`#rejectUserAttdRequest`(반려)는 SEC-018 가드 `AttdReqTypeUtils.isAttendanceReqType(reqType)`(01/02만 true)에 막혀 '10'이면 `AttdErrorCode.ATTD_400_006`("요청을 처리할 수 없습니다.") 거부.
   - OT 경로(`updateUserOvertimeRequests`/`rejectUserOvertimeRequest`)는 `isOvertimeReqType`(03/04)로, 연차는 leaveflow 로 분기. '10'은 어디에도 안 걸린다.
4. **프론트(`AttdDayDetailPop.vue`)도 '10' 전용 분기가 없다.**
   - `fnApproveReq`(~L2309): 03/04→OT엔드포인트, 05/06→leaveflow, **그 외(10 포함)→기본분기 `/webApi/attd07/update-user-attd-requests`** → 위 가드에 막힘.
   - `fnRejectReq`(~L2435)+`onRejectConfirm`(~L2465): 03/04→overtime, 05/06→leave, **그 외(10 포함)→kind='attd'→`/webApi/attd07/reject-user-attd-requests`** → 막힘.
5. **승인 시 "스케줄 반영"의 실제 대상은 `tb_user_work_plan` 단일 행이다 (DB 스키마 검증).**
   - `tb_user_work_plan` PK=(CMPNY_CD, SITE_CD, USER_CD, WORK_YMD), 단일 컬럼 `WORK_PLAN_CD`(주석 "근무계획코드[SCH_CD, LEAVE_CD]"). **WORK_SEQ 컬럼이 없다 — 사용자-일자당 스케줄 코드는 1개.**
   - 즉 "1구간/2구간"은 스케줄 마스터 `tb_sch_mgmt`(PK CMPNY_CD,SITE_CD,SCH_CD)의 `SCH_CD` 자체가 인코딩한다(`FST_SCH_*` 1구간, 2구간 컬럼). 사용자-일자 plan 은 그 SCH_CD 를 가리키는 한 칸일 뿐이다.
   - Attd_05 의 쓰기 경로: `Attd05Mapper.saveUserWorkPlans` = `INSERT ... ON DUPLICATE KEY UPDATE WORK_PLAN_CD=NEW.WORK_PLAN_CD`. 셀 비우기 = `deleteUserWorkPlanCell`(PK 완전일치 DELETE).
6. **이 사실이 "기존 2구간 스케줄 처리" 우려를 해소한다(D2 참조).** 사용자-일자 스케줄에는 2구간이 별도 행으로 존재하지 않는다. 승인은 `WORK_PLAN_CD = 요청 SCH_CD` 로 그 날의 단일 스케줄 코드를 교체(upsert)하는 것이다. 교체할 "2구간 행"이 물리적으로 없다.

---

## 1. 핵심 설계 결정 (D1~D9)

### D1 — 승인 시 "스케줄 반영"의 대상 테이블/연산
- **결정**: `tb_user_work_plan` 의 해당 (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD) 행의 `WORK_PLAN_CD` 를 요청의 `SCH_CD` 로 **upsert** 한다(`INSERT ... ON DUPLICATE KEY UPDATE`). Attd_05 의 `saveUserWorkPlans` 와 동일 패턴/동일 컬럼.
- **근거**: §0-5 스키마 검증 + `attd/09-requests-approval.md §9.2` "관리자 승인 시 반영" + `request-approval/06-approval-flows.md §6.1` "승인 효과 = 해당 일자 스케줄 갱신". 스키마상 사용자-일자 스케줄은 단일 WORK_PLAN_CD 이므로 upsert 가 정확한 연산이다.
- **재사용 가능성**: Attd05Mapper.saveUserWorkPlans 의 SQL 본문을 그대로 본 패키지(attd07) Mapper 에 복제하거나, 동일 시그니처 command 로 호출한다. (cross-package mapper 직접 호출보다 attd07 Mapper 에 동일 upsert 1개 신설 권장 — 모듈 경계 유지.)

### D2 — 기존 다구간 스케줄 교체 정책 (사용자가 우려한 핵심 지점)
- **결정**: **그 날의 `WORK_PLAN_CD` 한 칸을 요청 SCH_CD 로 통째 교체한다. "기존 2구간을 부분 유지/제거"하는 연산은 존재하지 않는다.** WORK_SEQ 가 여러 개여도(예: 앱이 향후 2 slot 으로 보내도) 사용자-일자 스케줄 plan 은 단일 컬럼이므로, **승인 처리는 요청 그룹(같은 REQ 묶음)의 대표 SCH_CD 1개로 그 날 WORK_PLAN_CD 를 설정**한다.
- **하위 결정 D2-a (다중 slot 요청 시 어떤 SCH_CD 를 쓰나)**: 현재 앱은 단일 slot(WORK_SEQ=1)만 보낸다(§0-1). 본 1차 구현은 **WORK_SEQ=1(최소 WORK_SEQ) slot 의 SCH_CD 를 채택**한다. 2 slot 으로 서로 다른 SCH_CD 가 오는 케이스는 현재 앱에서 발생 불가하므로, **요청 그룹에 서로 다른 SCH_CD 가 2개 이상이면 fail-closed 로 거부(신규 에러코드, D6)** 하고 승인하지 않는다(데이터 정합성 보호). 단일 SCH_CD 면 그 값으로 upsert.
- **근거**: §0-5/§0-6 스키마 사실 + `attd/06-schedule.md §6.6`(1일 2구간은 SCH_CD 내부 구조이지 사용자-일자 plan 의 다중 행이 아님). 사용자 우려("2구간 제거?")에 대한 답: **제거할 행이 없다. 한 칸 교체다.** 정책서에 "부분 구간 승인" 개념이 없으므로 추측 구현 금지.
- **주의(qa/security 체크포인트)**: 승인 후 그 날 스케줄은 요청 SCH_CD 가 정의하는 구간 수(1 or 2)를 따른다. 즉 기존이 2구간이고 요청 SCH_CD 가 1구간짜리면, 승인 후 그 날은 1구간 스케줄이 된다(정상 — 사용자가 "스케줄 자체를 바꾸겠다"고 요청한 것). 이는 버그가 아니라 의도된 동작이다.

### D3 — 처리 이력(HIST) 기록 여부
- **결정**: **승인/반려 모두 `tb_user_attd_hist` 에 별도 HIST 행을 남기지 않는다(1차).** 이유: (1) HIST 의 ATTD_ID 는 NOT NULL 인데 스케줄 수정은 근태기록(ATTD_ID)이 없을 수 있다(미래 일자 스케줄 수정엔 출퇴근 기록 없음). (2) `tb_user_attd_hist` 의 BEF_*/AFT_* 컬럼은 출퇴근 시각 모델이라 SCH_CD 변경을 담기 부적합. (3) `tb_user_work_plan` 에는 자체 hist 테이블이 없다(`tb_sch_mgmt_hist` 는 마스터 정의 이력이지 사용자-일자 plan 이력이 아님).
- **요청 상태 이력은 보존된다**: `tb_user_attd_req` 의 REQ_STATUS('02' 승인/'03' 반려) + PROCESS_USER_CD + PROCESS_COMMENT(반려 사유) + PROCESS_DATE 로 "요청/승인/반려 이력(사유 포함)"을 충족한다(`attd/09-requests-approval.md §9.2` 이력 보존 요건 = REQ 행 자체로 만족).
- **근거**: `attd/09-requests-approval.md §9.2`(이력 보존 = 요청/승인/반려 + 사유), §9.5(반려 사유 필수). HIST 테이블 미적합은 스키마 사실. **follow-up FU-1**: 스케줄 변경 전/후 SCH_CD 를 별도 이력으로 남기려면 신규 HIST_TYPE + 컬럼/테이블 필요 — 본 작업 비범위.
- **AttdDayDetailPop "처리 이력" 타임라인 노출**: 스케줄 수정 승인/반려는 HIST 미기록이므로 일자 상세의 "처리 이력"(TB_USER_ATTD_HIST 기반) 목록에 안 나타난다. 단 "근로자 요청 카드"(`selectMonthlyAttdReq`)는 REQ_STATUS='01' 만 노출하므로, 처리 후 카드가 사라지는 것으로 사용자에게 피드백된다(기존 근태 보정과 동일 UX). 이력 타임라인 노출은 FU-1.

### D4 — 반려 시 효과
- **결정**: 반려는 `tb_user_attd_req` 의 REQ_STATUS='03' + PROCESS_COMMENT=반려사유 + PROCESS_USER_CD + PROCESS_DATE 만 갱신한다. **`tb_user_work_plan` 은 일절 건드리지 않는다(기존 스케줄 유지).**
- **근거**: `attd/09-requests-approval.md §9.2`("반려 시 기존 유지"), `request-approval/06-approval-flows.md §6.1`("반려(사유 필수)").

### D5 — 마감 가드 / 권한 / 자기승인 / 중복처리 가드
- **결정**: 기존 근태 승인/반려와 동일 가드를 신규 경로에 그대로 적용한다.
  1. **권한**: `attdCloseService.canManageNode(authCd, userCd, cmpnyCd, siteCd, nodeCd)` — master/hr 또는 해당/상위 부서 정·부 관리자. 실패 시 ATTD_403_002.
  2. **마감 가드**: `ensureNotClosed(cmpnyCd, siteCd, nodeCd, workYmd)` — `isClosedForNode`(PRAFTA-028 근태 월마감). 마감 월이면 ATTD_400_042.
     - **주의(D5-a)**: 정책서는 스케줄 수정 = "스케줄 마감 전"(`§9.2`/`§6.1`), 근태 보정/OT = "근태 마감 전"으로 표현하나, **PRAFTA 구현에는 마감 메커니즘이 `tb_attd_close`(근태 월마감, PRAFTA-028) 한 종뿐이다(스케줄 전용 마감 테이블/서비스 없음 — 코드 검증).** 따라서 스케줄 수정 승인/반려도 근태 월마감(`isClosedForNode`)을 마감 가드로 사용한다. 이는 정책서의 "스케줄 마감"을 현행 단일 마감으로 매핑한 것이며, qa/security 는 이 매핑을 전제로 검증한다.
  3. **REQ_TYPE allow-list(SEC-018 동형)**: 본 신규 경로는 `AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY`("10")만 처리. 그 외 reqType 이 들어오면 fail-closed(ATTD_400_006). → `AttdReqTypeUtils` 에 `isScheduleModifyReqType(String)` 헬퍼 신설.
  4. **중복처리/멱등 가드**: REQ_STATUS='01'(신청) 행만 승인/반려 가능. 처리 후 재요청 시 ATTD_409_001. UPDATE 는 `... AND REQ_STATUS='01'` 조건으로 affected rows=0 이면 ATTD_409_001(동시 처리 충돌, 트랜잭션 롤백). 기존 `updateUserAttdReqApprove`/`updateUserAttdReqReject` 와 동형.
  5. **cross-site/cross-user IDOR(SEC-017 동형)**: 권위 REQ 행을 `selectUserAttdReqByReqId(reqId, gvCmpnyCd)` 로 로드(회사 스코프). body 의 userCd/siteCd/workYmd/workSeq/nodeCd 가 REQ 행과 불일치하면 ATTD_400_005(변조). 대상 사용자가 회사/사이트 스코프에 실재하는지 `selectUserExistInCmpnySite` 로 재확인(없으면 ATTD_404_011).
  6. **자기승인**: 기존 근태/OT 경로가 자기승인 차단을 주석 처리(비활성)해 둔 현행 상태(`Attd07ServiceImpl` L257~262, L560~564)와 **동일하게 비활성 유지**(정책 §9.5 "요청자=승인권자면 자동 승인 가능"의 단순 등록 모델 — 결재선 미통합 1차). 별도 자기승인 차단 추가하지 않음(일관성). 정책 §9.5 의 "상위 공동 처리 범위 본인 건 자동 승인 금지"는 결재선 통합(prafta-app-009) 영역.
- **근거**: `request-approval/06-approval-flows.md §6.1`(차단 사유: ①스케줄 마감 후 ②본인 결재 ③다른 관리자 선점), `attd/09-requests-approval.md §9.5`, `common/08-permissions.md §8.4`(IDOR), 기존 `Attd07ServiceImpl` 패턴.

### D6 — 신규 에러코드 필요 여부
- **결정**: 신규 에러코드 **추가하지 않고 기존 코드 재사용**을 기본으로 한다.
  - 승인/반려 대상 아닌 reqType → `ATTD_400_006`(기존, "요청을 처리할 수 없습니다").
  - 권한 없음 → `ATTD_403_002`(기존).
  - 마감 → `ATTD_400_042`(기존).
  - REQ 없음 → `ATTD_404_001`(기존). 변조 → `ATTD_400_005`(기존). 대상 사용자 스코프 밖 → `ATTD_404_011`(기존). 이미 처리/동시충돌 → `ATTD_409_001`(기존).
  - **D2-b 다중 SCH_CD 충돌(요청 그룹에 SCH_CD 2개 이상)**: 발생 불가 케이스(앱 단일 slot)지만 fail-closed 가 필요. 기존 `ATTD_400_005`(변조/일관성 위반) 재사용으로 충분 — 신규 코드 불필요. developer 는 로그 메시지로 사유를 구분.
- **근거**: 기존 에러코드로 모든 분기 커버 가능. 코드 표면적 최소화.

### D7 — 엔드포인트 네이밍 (기존 컨벤션 준수)
- **결정**:
  - 승인: `POST /webApi/attd07/approve-sched-modify-requests`
  - 반려: `POST /webApi/attd07/reject-sched-modify-requests`
- **근거**: 기존 attd07 컨벤션 `update-user-attd-requests`/`reject-user-attd-requests`/`update-user-overtime-requests`/`reject-user-overtime-requests` 의 kebab-case + 동사-명사-requests 형식. 스케줄 수정은 "update" 가 아니라 "approve"(스케줄 plan 갱신이 부수효과)로 명명해 근태 보정 endpoint 와 의미 충돌 회피. (대안: `update-user-sched-requests` 도 컨벤션 합치하나, "approve" 가 단일 동작을 더 명확히 표현.)
- **컨트롤러 위치**: 기존 `Attd07Controller` 에 메서드 2개 추가(신규 컨트롤러 만들지 않음 — attd07 관리자 액션의 단일 진입).

### D8 — 마이그레이션 필요 여부
- **결정**: **DB 마이그레이션 불필요.**
  - SYS032='10'(스케줄 수정 요청) 코드 + `tb_user_attd_req.SCH_CD` 컬럼은 이미 prafta-app-007 마이그레이션(`prafta-app-007-attd-req-extensions.sql`)에서 정의됨(운영 적용은 사용자 수동 — 등록 측 의존). 본 작업은 그 위에서 읽기/쓰기만 한다.
  - 승인 시 쓰는 `tb_user_work_plan` 은 기존 테이블/컬럼.
  - HIST 미기록(D3)이므로 신규 HIST_TYPE 코드 불필요.
- **선결 조건(사용자 확인 필요, §9-1)**: 본 웹 승인 기능이 의미를 가지려면 `prafta-app-007-attd-req-extensions.sql`(SYS032=10 + SCH_CD 컬럼)이 **운영 DB 에 선적용**되어 있어야 한다. 미적용이면 앱 등록 자체가 SCH_CD 컬럼 부재로 실패하거나 NULL 저장되어 승인이 데이터 부재로 거부된다.

### D9 — 프론트 라우팅 분기 (Vue 골격 신규 작성 불필요 — 분기 추가만)
- **결정**: `AttdDayDetailPop.vue` 의 `fnApproveReq`/`fnRejectReq`/`onRejectConfirm` 에 `reqType === '10'` 분기를 추가한다. 신규 화면/컴포넌트 없음. 상세 명세는 §4.

---

## 2. 영향 범위

### 2.1 백엔드 (신규/수정)
```
com.prafta.web.attd.attd07
├── controller/Attd07Controller.java                 (수정 — 2 메서드 추가)
├── service/Attd07Service.java                        (수정 — 2 메서드 시그니처 추가)
├── service/impl/Attd07ServiceImpl.java               (수정 — approveSchedModifyRequest / rejectSchedModifyRequest 구현)
├── util/AttdReqTypeUtils.java                        (수정 — isScheduleModifyReqType 헬퍼 추가)
├── application/param/
│   ├── ApproveSchedModifyRequestParam.java           (신규 record, JWT 기반)
│   └── (반려는 기존 RejectUserAttdRequestParam 재사용 가능 — workSeq/nodeCd 포함 검토; 부족 시 신규)
├── application/command/
│   └── (승인 upsert command — UpsertUserWorkPlanCommand 신규 또는 Attd05 command 재사용 검토)
└── dto/request/
    ├── ApproveSchedModifyRequest.java                (신규)
    └── (반려 — RejectUserAttdRequestRequest 재사용)

src/main/resources/com/prafta/web/attd/attd07/mapper/
└── Attd07Mapper.xml                                  (수정)
    - upsertUserWorkPlan         (신규 — Attd05 saveUserWorkPlans 동형 INSERT..ON DUP UPDATE)
    - selectMonthlyAttdReq       (수정 — SELECT 절에 A.SCH_CD AS schCd 추가; result/매핑 동반)
    - (승인/반려 UPDATE 는 기존 updateUserAttdReqApprove / updateUserAttdReqReject 재사용 가능)
```
- `MonthlyAttdReqResult.java`(record) 에 `String schCd` 필드 추가(프론트 카드에 목표 스케줄 노출용 — §4).

### 2.2 프론트엔드 (수정만)
```
PRAFTA/prafta-web-frontend/prafta-web-frontend/src/views/attd/popup/AttdDayDetailPop.vue
  - fnApproveReq   : reqType==='10' 분기 추가 → POST /webApi/attd07/approve-sched-modify-requests
  - fnRejectReq    : reqType==='10' → kind='schedModify' 로 분기
  - onRejectConfirm: kind==='schedModify' → POST /webApi/attd07/reject-sched-modify-requests
  - (선택) 요청 카드 렌더링: reqType==='10' 일 때 목표 스케줄(schCd/스케줄명) 표시 — 데이터는 selectMonthlyAttdReq 의 schCd
```

### 2.3 마이그레이션
- 없음(D8). 단 선결: `prafta-app-007-attd-req-extensions.sql` 운영 선적용(§9-1).

---

## 3. developer 단위 작업 분해

> 각 작업의 정책서 출처는 §1 의 D-결정에 매핑. CLAUDE.md 규칙에 따라 상세 설명에 출처 명시.

### PRAFTA-APP-007-WEB-1 — REQ_TYPE 가드 헬퍼 + 승인 백엔드
- **유형**: backend / 신규 · 보완
- **정책 근거**: `attd/09-requests-approval.md §9.2`(스케줄 수정 승인 효과·이력), `request-approval/06-approval-flows.md §6.1`(승인 효과=스케줄 갱신, 차단 사유 3종), `attd/06-schedule.md §6.6`, `common/08-permissions.md §8.4`(IDOR), `common/03-account-auth.md §3.4`(JWT 식별).
- **핵심 요구사항**:
  1) `AttdReqTypeUtils.isScheduleModifyReqType(String)` 추가 — `REQ_TYPE_SCHED_MODIFY`("10")만 true(fail-closed).
  2) `POST /webApi/attd07/approve-sched-modify-requests` — Controller/Service/Impl 메서드 `approveSchedModifyRequest`.
  3) 가드 순서(기존 `updateUserAttdRequest` 와 동형): ①권위 REQ 로드(`selectUserAttdReqByReqId(reqId, gvCmpnyCd)`, 없으면 ATTD_404_001) → ②`isScheduleModifyReqType` 아니면 ATTD_400_006 → ③`canManageNode` 아니면 ATTD_403_002 → ④`ensureNotClosed`(ATTD_400_042) → ⑤REQ_STATUS='01' 아니면 ATTD_409_001 → ⑥body↔REQ 키 일치(userCd/siteCd/workYmd/workSeq/nodeCd) 아니면 ATTD_400_005 → ⑦대상 사용자 스코프 `selectUserExistInCmpnySite` 0건이면 ATTD_404_011.
  4) **스케줄 반영(D1/D2)**: 같은 요청 묶음의 SCH_CD 를 결정 — 1차는 단일 REQ 행(reqId) 기준 그 행의 SCH_CD 사용. (다중 slot 그룹 처리 모델은 현재 앱 단일 slot 이라 단건 처리로 충분; 그룹 처리는 FU-3.) SCH_CD 가 null/빈값이면 ATTD_400_005(데이터 부재 — 마이그 미적용/등록 오류, fail-closed).
  5) `upsertUserWorkPlan`(신규 mapper) 호출 → `tb_user_work_plan` 의 (cmpnyCd, siteCd, userCd, workYmd) 행 WORK_PLAN_CD = SCH_CD upsert(INSERT..ON DUP UPDATE; Attd05 동형).
  6) `updateUserAttdReqApprove`(기존 재사용; TARGET_ID 는 스케줄 수정에 무의미하므로 null 또는 미사용) → REQ_STATUS='02', PROCESS_*. affected=0 이면 ATTD_409_001(롤백).
  7) HIST 미기록(D3).
  8) `@Transactional` 단일 트랜잭션.
- **영향 파일**: AttdReqTypeUtils / Attd07Controller / Attd07Service(+Impl) / Attd07Mapper.xml(upsertUserWorkPlan 신규) / ApproveSchedModifyRequestParam·Request 신규 / (upsert command).
- **재사용**: `selectUserAttdReqByReqId`, `updateUserAttdReqApprove`, `selectUserExistInCmpnySite`, `canManageNode`, `ensureNotClosed`, Attd05 `saveUserWorkPlans` SQL 본문.
- **비범위**: 결재선(prafta-app-009), 알림(prafta-031), HIST 기록(FU-1), 다중 SCH_CD 그룹 승인(FU-3).

### PRAFTA-APP-007-WEB-2 — 반려 백엔드
- **유형**: backend / 신규
- **정책 근거**: `attd/09-requests-approval.md §9.2`(반려 시 기존 유지), §9.5(반려 사유 필수), `request-approval/06-approval-flows.md §6.1`.
- **핵심 요구사항**:
  1) `POST /webApi/attd07/reject-sched-modify-requests` — `rejectSchedModifyRequest`.
  2) 가드: 승인과 동일(①~⑦, 단 마감 가드는 REQ 권위 nodeCd/workYmd 기준 적용 — 기존 `rejectUserOvertimeRequest` 가 reqRow 기준으로 마감 검사하는 패턴 참고). reqType='10' 아니면 ATTD_400_006.
  3) 반려 사유(rejectReason) 빈값이면 거부(서버 필수 — §9.5). 기존 반려 DTO 가 사유 필수 검증 없으면 본 경로에서 `@NotBlank` 또는 service 검증 추가.
  4) `updateUserAttdReqReject`(기존 재사용) → REQ_STATUS='03', PROCESS_COMMENT=사유. affected=0 → ATTD_409_001.
  5) `tb_user_work_plan` 미변경(D4). HIST 미기록(D3).
- **영향 파일**: Attd07Controller / Service(+Impl) / (RejectUserAttdRequestParam·Request 재사용 또는 신규) / Attd07Mapper.xml(기존 updateUserAttdReqReject 재사용).
- **재사용**: `updateUserAttdReqReject`, 승인 작업의 가드 헬퍼.

### PRAFTA-APP-007-WEB-3 — 요청 카드에 목표 스케줄 노출 (조회 보완)
- **유형**: backend / 보완
- **정책 근거**: `attd/09-requests-approval.md §9.6.3`(상세 패널 — 요청 내용 Before/After), `§6.1`(변경 내용=시간·근무타입코드).
- **핵심 요구사항**:
  1) `Attd07Mapper.selectMonthlyAttdReq` SELECT 절에 `A.SCH_CD AS schCd` 추가.
  2) `MonthlyAttdReqResult` record 에 `String schCd` 필드 추가.
  3) (선택) 스케줄명 라벨이 필요하면 `tb_sch_mgmt` 조인으로 SCH_NM 동반 — 1차는 schCd 만(라벨 조인은 FU-2). 프론트는 schCd 표기 또는 day 컨텍스트의 스케줄명 활용.
- **영향 파일**: Attd07Mapper.xml(selectMonthlyAttdReq) / MonthlyAttdReqResult.java.
- **비범위**: SCH_NM 라벨 조인(FU-2).

### PRAFTA-APP-007-WEB-4 — 프론트 라우팅 분기 (AttdDayDetailPop)
- **유형**: frontend-screen / 보완 (골격 신규 작성 불필요 — 기존 핸들러 분기 추가)
- **정책 근거**: `common/13-ui-ux.md`(피드백/인터랙션), 기존 03/04·05/06 분기 패턴.
- **핵심 요구사항(명세는 §4)**:
  1) `fnApproveReq`: `card.reqType === '10'` 분기 추가 → `POST /webApi/attd07/approve-sched-modify-requests`.
  2) `fnRejectReq`: `reqType === '10'` → `rejectModal.kind = 'schedModify'`.
  3) `onRejectConfirm`: `kind === 'schedModify'` → `POST /webApi/attd07/reject-sched-modify-requests`.
  4) (선택) 카드 표시에 목표 스케줄(schCd/스케줄명) 노출.
  5) 성공 시 `fnSearch()` 재조회(기존 패턴), 에러는 `resolveApiErrorMessage`.
- **영향 파일**: `AttdDayDetailPop.vue`.
- **선행**: WEB-1, WEB-2(엔드포인트 확정). 카드 표시는 WEB-3.

### 권장 착수 순서
WEB-1 → WEB-2 (백엔드, 가드/구조 공유) → WEB-3 (조회 보완, 독립) → WEB-4 (프론트 분기, 엔드포인트 의존).

---

## 4. 프론트 분기 추가 명세 (AttdDayDetailPop.vue)

`card.raw` 가 보유하는 값(현행): `reqId, userCd, siteCd, nodeCd, workYmd, workSeq, reqType, reqReason, startDate/Time, endDate/Time, otType`. WEB-3 적용 후 `schCd` 추가.

### 4.1 `fnApproveReq` — 03/04(OT) 분기 위에 또는 05/06(leave) 분기와 나란히 '10' 분기 삽입
```
// 스케줄 수정 요청(10) → 전용 승인 엔드포인트
const isSchedModifyReq = card?.reqType === "10";
if (isSchedModifyReq) {
  const payload = {
    reqId: raw.reqId,
    userCd: raw.userCd || props.userCd_p,
    siteCd: raw.siteCd || props.siteCd_p,
    nodeCd: raw.nodeCd || props.nodeCd_p || "",
    workYmd: raw.workYmd || ymdDashToNum(props.date_p),
    workSeq: String(parseInt(raw.workSeq, 10) || card?.workSeq || 1),
    // schCd 는 서버가 REQ 행에서 권위 조회하므로 전송 불필요(전송해도 서버 미신뢰).
  };
  // POST /webApi/attd07/approve-sched-modify-requests → 200 시 $alert(SAVE_COMPLETED) + fnSearch()
  // 에러: resolveApiErrorMessage
  return;
}
```
- **payload 키**: 서버 가드(D5-5 body↔REQ 일치)에 필요한 reqId/userCd/siteCd/nodeCd/workYmd/workSeq. SCH_CD 는 서버가 REQ 행에서 읽으므로 클라가 보내지 않음(IDOR/변조 방지).
- **confirm**: 기존 `proxy.$confirm(MSG.REQ_APPROVE_CONFIRM)` 재사용, `guardClosed()` 선행.

### 4.2 `fnRejectReq` — kind 분기에 'schedModify' 추가
```
const isSchedModifyReq = card?.reqType === "10";
rejectModal.value = {
  open: true,
  kind: isLeaveReq ? "leave" : isOtReq ? "overtime" : isSchedModifyReq ? "schedModify" : "attd",
  busy: false,
  context: { reqId, siteCd, userCd, workYmd, workSeq, nodeCd, approvalStep },
};
```

### 4.3 `onRejectConfirm` — kind==='schedModify' 분기
```
} else if (kind === "schedModify") {
  await axios.post("/webApi/attd07/reject-sched-modify-requests", {
    reqId: context.reqId,
    siteCd: context.siteCd,
    userCd: context.userCd,
    workYmd: context.workYmd,
    workSeq: context.workSeq,
    nodeCd: context.nodeCd,
    rejectReason: reason,   // 서버 필수(§9.5)
  });
}
```

### 4.4 (선택) 카드 표시
- reqType==='10' 카드에 "변경 목표 스케줄: {schCd 또는 스케줄명}" 한 줄 추가. 데이터는 WEB-3 의 `schCd`. 스케줄명 라벨은 FU-2(미적용 시 schCd 코드 표기 또는 생략).

---

## 5. qa 체크포인트

1. **타입 가드 통과/차단**: reqType='10' 만 신규 경로 통과. '01'/'02'/'03'/'04'/'05'/'06'/'07'/'08'/'09'/임의값은 ATTD_400_006 거부. 반대로 기존 근태/OT/leave 경로에 '10'을 넣으면 여전히 거부되는지(회귀).
2. **스케줄 반영 정확성(D1/D2)**: 승인 후 `tb_user_work_plan.(cmpnyCd,siteCd,userCd,workYmd).WORK_PLAN_CD` == 요청 SCH_CD. 기존 행 있으면 UPDATE, 없으면 INSERT.
   - **2구간→1구간 교체 케이스**: 기존 SCH_CD 가 2구간짜리, 요청 SCH_CD 가 1구간짜리 → 승인 후 그 날은 1구간(의도된 동작, 버그 아님). 검증: WORK_PLAN_CD 가 요청값으로 통째 교체됨.
   - **테스트 데이터**: USER_CD='20260400013', WORK_YMD='20260602', REQ_ID='2026060200010'(reqType='10', SCH_CD='00003', WORK_SEQ=1, status='01'). 승인 후 WORK_PLAN_CD='00003' 인지.
3. **반려 무반영(D4)**: 반려 후 `tb_user_work_plan` 불변, REQ_STATUS='03', PROCESS_COMMENT=사유.
4. **멱등/중복처리**: 이미 '02'/'03' 인 REQ 재승인/재반려 → ATTD_409_001. 동시 처리(같은 reqId 두 번) → 한쪽만 성공.
5. **마감 차단**: 해당 월(부서) 마감이면 승인/반려 모두 ATTD_400_042. (선결: 마감=근태 월마감 단일 메커니즘 — D5-a 전제 확인.)
6. **권한**: 일반 작업자/타 부서 매니저 → ATTD_403_002. master/hr/해당 부서 정·부 관리자 → 통과.
7. **IDOR/cross-site/cross-user**: body 의 userCd/siteCd/workYmd/workSeq/nodeCd 를 REQ 행과 다르게 위조 → ATTD_400_005. 타 회사 reqId → ATTD_404_001(회사 스코프 로드). 대상 사용자 스코프 밖 → ATTD_404_011.
8. **반려 사유 필수**: 빈 사유 반려 → 거부(§9.5).
9. **SCH_CD 부재 방어**: REQ 행 SCH_CD null(마이그 미적용/등록 오류) → 승인 거부(ATTD_400_005), WORK_PLAN_CD 오염 없음.
10. **프론트**: '10' 카드 승인/반려가 올바른 엔드포인트로 라우팅. 성공 시 카드 사라짐(REQ_STATUS 전이로 selectMonthlyAttdReq 재조회 시 제외). 에러 메시지 한국어.
11. **카드 표시(WEB-3)**: selectMonthlyAttdReq 에 schCd 추가가 기존 03/04/05/06/01/02 카드 렌더링에 회귀 없는지(null 허용).

## 6. security 체크포인트

1. **SEC-018(타입 혼동)**: `isScheduleModifyReqType` fail-closed. '10' 외 전부 거부. 신규 경로가 근태/OT/leave REQ 를 스케줄 수정으로 오처리하지 않는지.
2. **SEC-015(매니저 게이트)**: `canManageNode` JWT gvAuthCd 기반(body 위조 불가). 일반 작업자 차단.
3. **SEC-017(IDOR)**: 권위 REQ 행을 회사 스코프로 로드, body 키 일치 검증, 대상 사용자 스코프 재확인. 클라가 보낸 SCH_CD 미신뢰(서버 REQ 행 값만 사용) — 승인 대상 스케줄 변조 차단.
4. **PRAFTA-028 마감 가드**: 마감 월 쓰기 차단(ATTD_400_042).
5. **PII**: 스케줄 수정 승인/반려는 SCH_CD/사유/시각만 다룸. PII 없음. 감사 로그 대상 아님(`common/11-security-privacy.md §11.3` — 다운로드/PII/중요삭제만).
6. **트랜잭션 정합성**: 스케줄 upsert + REQ 상태 전이가 단일 트랜잭션. REQ UPDATE affected=0(동시충돌) 시 upsert 까지 롤백.
7. **법적 책임 영역(attd) +1 격상**: 근태/스케줄은 attd 도메인 — 우선순위 격상 대상(planner.md §4).

---

## 7. Follow-up 후보

| # | 항목 | 사유 |
|---|---|---|
| FU-1 | 스케줄 수정 승인/반려 처리 이력(Before/After SCH_CD) 을 일자 상세 "처리 이력" 타임라인에 노출 | D3 — HIST 테이블 모델이 SCH_CD 변경에 부적합. 신규 HIST_TYPE/컬럼 또는 별도 이력 테이블 필요. `§9.6.3` 상세 패널 Before/After. |
| FU-2 | 요청 카드/상세에 스케줄명(SCH_NM) 라벨 노출 (tb_sch_mgmt 조인) | 1차는 schCd 코드만. |
| FU-3 | 다중 slot(2구간 서로 다른 SCH_CD) 스케줄 수정 요청 그룹 처리 | 현재 앱 단일 slot. 앱이 다중 SCH_CD 를 보내는 모델로 가면 그룹 승인 정책 재정의 필요(현재는 단일 SCH_CD 만 허용, 그 외 fail-closed). |
| FU-4 | 결재선 통합(승인 단계 N단) | prafta-app-009 영역(Q3). |
| FU-5 | 승인/반려 후 근로자 알림(push outbox) | prafta-031 consumer 미구현. |
| FU-6 | 스케줄 전용 마감 메커니즘 도입 시 마감 가드 분리 | D5-a — 현재 근태 월마감 단일 메커니즘으로 매핑. 정책서가 "스케줄 마감"을 별도로 정의하면 재검토. |
| FU-7 | 스케줄 수정 경합 잠금(§6.5) — 대기 중 스케줄 직접 수정 차단 표시 | `attd/06-schedule.md §6.5`/`§6.1 수정 경합`. Attd_05 화면에서 '10' 대기 REQ 존재 일자 잠금 표시는 별도 작업. |

---

## 8. 메인 세션이 Notion 에 기록할 항목 (planner 직접 접근 불가)

"작업 로그" DB 에 다음 4행 등록(작업ID 는 메인 세션이 PRAFTA-{순번} 통합 채번으로 재부여; 아래는 본 plan 내부 식별자):

| 내부 ID | 영역 | 모듈 | 작업유형 | 요구사항 요약 |
|---|---|---|---|---|
| APP-007-WEB-1 | web | attd/attd07 | 신규 | 스케줄 수정 요청(10) 승인 엔드포인트 + isScheduleModifyReqType 가드 + tb_user_work_plan upsert. [정책: attd/09 §9.2, request-approval/06 §6.1] |
| APP-007-WEB-2 | web | attd/attd07 | 신규 | 스케줄 수정 요청(10) 반려 엔드포인트(상태 전이 + 사유 필수, 스케줄 무반영). [정책: attd/09 §9.2/§9.5] |
| APP-007-WEB-3 | web | attd/attd07 | 보완 | selectMonthlyAttdReq + MonthlyAttdReqResult 에 schCd 추가(요청 카드 목표 스케줄 노출). [정책: attd/09 §9.6.3] |
| APP-007-WEB-4 | web | attd/attd07 | 보완 | AttdDayDetailPop fnApproveReq/fnRejectReq/onRejectConfirm 에 reqType='10' 분기 추가. |

- 상세 설명에는 본 §3 의 각 작업 블록을 그대로 사용. 선행 관계: WEB-4 ← WEB-1/WEB-2/WEB-3.
- 우선순위: attd(법적 책임) +1 격상 + 데이터 정합성(스케줄 반영) → 상위.

---

## 9. 미해결 / 사용자 확인 필요

1. **(선결, 운영) `prafta-app-007-attd-req-extensions.sql` 운영 DB 선적용 여부.** SYS032=10 + `tb_user_attd_req.SCH_CD` 컬럼이 운영에 없으면 본 승인 기능은 데이터 부재로 동작 불가. 미적용이면 적용 후 본 작업 착수 권장. (D8)
2. **엔드포인트 명칭 `approve-/reject-sched-modify-requests` 채택 동의 여부.** 대안 `update-user-sched-requests` 도 컨벤션 합치(D7). 사용자/메인 세션 선호 확인.
3. **다중 slot 그룹 승인 정책(D2-a/FU-3).** 현재 앱 단일 slot 전제로 단건 처리. 향후 앱이 2구간 서로 다른 SCH_CD 를 보내면 그룹 승인 규칙 재정의 필요. 현행은 단일 SCH_CD 만 허용, 충돌 시 fail-closed. 이 전제 동의 여부.
4. **스케줄 마감 vs 근태 마감 매핑(D5-a).** 정책서는 "스케줄 마감"을 별도 표현하나 PRAFTA 구현은 근태 월마감(tb_attd_close) 단일. 본 작업은 근태 월마감으로 마감 가드 적용. 별도 스케줄 마감 도입 계획이 있으면 알려달라(FU-6).
5. **HIST 미기록(D3) 동의 여부.** 요청 상태 이력(REQ 행)으로 "이력 보존" 충족. 일자 상세 타임라인에 스케줄 변경 Before/After 노출이 1차 요건이면 FU-1 을 본 작업에 포함할지 확인.
