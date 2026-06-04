# prafta-app-018-B — 작업 분해 (앱 BE 연차 신청 쓰기 + 결재선 INSERT)

분해자: planner. 상위: `prafta-app-018-leave-apply-plan.md` / 단위 요청서: `prafta-app-018-B-leave-submit.md`.
선행: **018-A 구현 완료**(`com.prafta.app.leave.leaveflow` 모듈 + helper `LeaveUnitGranularity` 디스크 확인). 본 단위는 동일 모듈에 **쓰기 트랜잭션 1개**(`POST /appApi/leaveflow/apply`)를 추가한다. 신규 테이블 없음. 화면 골격 없음(BE).

본 단위는 핵심 트랜잭션이다. 웹 `com.prafta.web.attd.leaveflow.service.impl.LeaveFlowServiceImpl#submitLeave`(99~227)를 **앱으로 미러**하되, D1(앱 결재선)·D2(단위 게이팅 신규)를 반영한다.

---

## 정책 출처 (정독 결과)
- attd §8 / §8.5(사용단위·휴게 가로지름·잔여 차감) — `.claude/context/policies/attd/08-leave.md`
- attd §9 / §9.5(결재·자기승인 원칙) — 동일 파일 §9 영역
- prafta-019(시간차/결재라인·LeaveFlow), prafta-024(USAGE_UNIT 단일화), prafta-028(부서 단위 근태마감)
- 상위 확정: **D1 결재선 옵션(a)**(신청자가 결재선 구성·INSERT), **D2 단위 게이팅**(허용단위 외 거부), **D2-a (Y) 계층형**, **D3 잔여검증**(즉시 차감대상 1건, 없으면 051)
- prafta-031: 향후 알림 outbox는 **본 작업 미포함**(명시)

## 스키마 확인 결과 (실측 — 추측 아님)
- `tb_user_attd_req`: PK REQ_ID. 컬럼 REQ_TYPE(SYS032, '05'=연차사용), REQ_STATUS(SYS033, 01신청/02승인), REQ_REASON, WORK_YMD, NODE_CD, START_DATE/START_TIME/END_DATE/END_TIME, LEAVE_TYPE(varchar10), **LEAVE_DAYS `decimal(3,1)`**, DEL_YN, INSERT_NO/DATE. ⚠️ LEAVE_DAYS 가 (3,1) 이라 시간차 차감 소수도 0.1 단위로 반올림 저장됨(요청 테이블은 표시용; 정밀 차감은 leave_use 의 `decimal(5,1)` LEAVE_DAYS·LEAVE_MINUTES 가 SSOT). 웹과 동일 거동(웹도 동일 컬럼에 BigDecimal 전달) → 미러 정합.
- `tb_user_leave_use`: PK LEAVE_ID. LEAVE_CD, REQ_ID(NULL 가능), GRANT_ID, START_DATE/START_TIME/END_DATE/END_TIME, USE_UNIT_TYPE(varchar2, SYS025), **LEAVE_DAYS `decimal(5,1)`**, LEAVE_MINUTES(int), LEAVE_REASON, LEAVE_STATUS(CONFIRMED/CANCELLED), DEL_YN, INSERT_NO/DATE.
- `tb_user_leave_grant`: GRANT_ID, LEAVE_CD, GRANT_DAYS/USED_DAYS `decimal(5,1)`, STATUS(ACTIVE..), EXPIRE_YN, DEL_YN, AVAIL_FROM_DATE/AVAIL_TO_DATE(YYYYMMDD), GRANT_DATE. (selectDeductibleGrant: STATUS='ACTIVE'+DEL_YN='N'+유효기간+잔여≥신청, AVAIL_TO_DATE ASC 만료임박 우선, `FOR UPDATE`).
- `tb_user_attd_req_approval`(스냅샷 stale·실재): 컬럼은 공통 `ApprovalLineMapper.xml#insertApprovalStep` 기준 — REQ_ID, APPROVAL_STEP(int 1부터), CMPNY_CD, APPROVER_USER_CD, APPROVAL_STATUS(SYS044 00대기/01신청/02승인/03반려), APPROVAL_COMMENT, APPROVAL_DATE, INSERT_NO/DATE, UPDATE_NO/DATE.
- `tb_site_node`: SELF_ATTD_APPRV_YN char(1), MAIN_ADMIN_CD, SUB_ADMIN_CD, NODE_CD (PK CMPNY/SITE/NODE) — 자기승인 자격 판정 출처.
- `tb_leave_type_mgmt`: SYSTEM_YN, APRV_USE_YN, USE_UNIT_TYPE (018-A 와 동일). `tb_leave_policy.APRV_USE_YN`(법정 결재여부, USE_YN='Y' 활성 1건).

## SYS 코드 상수 (웹 미러 동일)
- SYS032 REQ_TYPE: '05'=연차사용 (본 작업 고정).
- SYS033 REQ_STATUS: '01'신청 / '02'승인.
- SYS044 APPROVAL_STATUS: '00'대기 / '01'신청 / '02'승인 / '03'반려.
- SYS025 USE_UNIT_TYPE: 00종일 / 01반차 / 02시간2h / 03시간1h / 04시간30분.
- LEAVE_STATUS: CONFIRMED.

---

## 핵심 재사용 결정 (중복 신설 금지 / 웹·앱 경계)

| 항목 | 출처 | 재사용 방식 | 근거 |
|---|---|---|---|
| 단위 granularity 계층 SSOT | `app.leave.leaveflow.application.helper.LeaveUnitGranularity` (018-A) | **그대로 호출**(신규 정의 금지) | 게이팅 검증과 메타 산출이 동일 상수 공유 |
| 시간차 차감 계산 | `common.cmm.leave.service.LeaveDeductionService` (`getDailyStdWorkMinutes`/`calcDeductionDays`/`crossesBreak`) | `@Service` 빈 주입 후 그대로 호출 | common 계층 공용, 웹도 동일 빈 사용 |
| 결재선 단계 저장/진행 | `common.cmm.approval.mapper.ApprovalLineMapper` (`insertApprovalStep`/`selectFirstWaitingStep`) + VO `ApprovalStepVO` | `@Mapper` 빈 주입, INSERT 그대로 | 웹 결재선과 완전 동일 테이블/구조 |
| 사후 마감 판정 | `com.prafta.web.attd.attd07.service.AttdCloseService#isClosedForUser` | **빈 주입 후 호출** (아래 결정 1) | 부서 단위 마감 정밀판정 로직 단일출처(prafta-028) |
| 프리셋 전개(presetId→스텝) | 018-A `selectApprovalPresets` 가 쓰는 mypage01 매퍼 + 본 단위 신규 전개 매퍼 | 아래 결정 2 | 본문 presetId 단축 입력 지원 시 |
| 연차 신청 SQL(요청/사용/부여/타입/잔여/자기승인) | **앱 신규** `AppLeaveFlowMapper` 에 메서드 추가(웹 SQL 미러) | 웹 `LeaveFlowMapper` 를 **직접 호출하지 않는다** | 앱/웹 패키지 분리 원칙(메모리 project_prafta_app_010: web직접호출 0건). 단 SQL 본문은 웹 미러. |

### 결정 1 — 사후 마감(050) 판정 경로 = AttdCloseService 빈 주입 (확정)
- req07(app)은 마감 가드를 "패키지 단독 의존 회피"로 follow-up 처리했으나, 본 단위는 **연차 차감을 동반하는 정밀 트랜잭션**이고 사후 마감 차단(050)이 웹 미러 로직의 일부이며 법적책임(attd) 영역이라 **미적용 시 정합성 결함**이다.
- 따라서 `AttdCloseService`(스프링 빈, `isClosedForUser(cmpny,site,user,closeYm)`)를 **앱 서비스에 직접 주입**하여 호출한다. 이 메서드는 내부에서 사용자 NODE_CD 를 서버 조회하므로 클라 nodeCd 불신뢰가 보장된다.
- 이는 의도된 단일 app→web 빈 의존(읽기전용 판정, 로직 미복제). security/qa 검토 포인트로 명시(아래 §보안). **마감 SQL/로직을 앱에 재구현하지 않는다**(중복 금지).

### 결정 2 — 결재선 입력 계약 = approverUserCds 우선 + presetId 보조 (확정)
- 본문은 `approverUserCds`(결재자 USER_CD 순서 배열)를 **1차 계약**으로 받는다(웹 `LeaveApplyParam.approverUserCds` 미러).
- `presetId`(선택)도 받아, 주어지고 `approverUserCds` 가 비어있으면 **서버에서 본인 소유 프리셋 스텝을 전개**하여 approverUserCds 로 환산한다(클라가 스텝 배열을 못 보낸 경우 대비). 전개는 신규 매퍼 `selectPresetStepUserCds(cmpnyCd,userCd,presetId)` 로 STEP_NO 오름차순 USER_CD 만 조회. 소유권은 `USER_CD=#{userCd}` 스코프로 강제(타인 프리셋 차단).
- 둘 다 비어있고 결재필요면 `COMMON_400_001`(웹 미러). 둘 다 주어지면 `approverUserCds` 를 채택(presetId 무시).

---

# 작업 분해 결과

## prafta-app-018-B-01
- **유형**: backend
- **영역**: app
- **모듈**: leave/leaveflow (`com.prafta.app.leave.leaveflow` — 018-A 기존 모듈에 쓰기 추가)
- **작업 유형**: 신규
- **요구사항 요약**: `POST /appApi/leaveflow/apply` — 단일 `@Transactional(rollbackFor=Exception.class)` 안에서 연차 신청을 처리(웹 submitLeave 미러 + D1 결재선 + D2 단위 게이팅). 식별값 JWT only(IDOR).
- **상세 설명**:
  - [Phase B] 핵심 요구사항(처리 순서 = 웹 99~227 미러):
    1) **요청 DTO/Param**: `LeaveApplyRequest`(camelCase) → `LeaveApplyParam.from(request, TokenInfo)`. 식별값(cmpny/site/user) 토큰 강제, leaveCd/workYmd/useUnitType 필수 검증(웹 param 미러).
    2) **타입 메타 + 결재필요 판정**: `selectLeaveTypeInfo(cmpny, leaveCd)` 없으면 `ATTD_404_030`. 결재필요 = 법정(systemYn='Y')이면 `selectPolicyAprvUseYn`, 비법정이면 타입 `APRV_USE_YN`.
    3) **단위 게이팅(신규, 웹엔 없음)**: 허용단위 집합 산출 = 법정이면 `selectCompanyUsageUnit().usageUnit` → `LeaveUnitGranularity.usageUnitToCode` → `allowedUnitsByCode`; 비법정이면 타입 `USE_UNIT_TYPE`(NULL→'00') → `allowedUnitsByCode`. 제출 `useUnitType` 이 허용 집합에 **없으면 `ATTD_400_102`**(신규) 거부. **이 게이트는 구조검증/차감 전에 수행**(잘못된 단위로 차감계산 진입 방지).
    4) **단위 구조검증 + 차감(웹 99~135 동일)**: 00→1.0 / 01→0.5(+leaveMinutes=daily/2) / 02·03·04→시각필수·`(e-s)%unitMin==0`·`minutes≤daily`·휴게 가로지름 거부·`calcDeductionDays`. 위반 시 052/054/055.
    5) **사후 마감(웹 137~146)**: `workYmd < today` 면 `AttdCloseService.isClosedForUser(cmpny,site,user, workYmd[0:6])` true → `ATTD_400_050`.
    6) **잔여 부여 선택(웹 148~152)**: `selectDeductibleGrant(cmpny,user,leaveCd,workYmd,leaveDays)` (FOR UPDATE) 없으면 `ATTD_400_051`.
    7) **요청 INSERT(웹 154~159)**: `selectNextReqId` → `insertLeaveReq`(REQ_TYPE='05', REQ_STATUS = aprvRequired?'01':'02', LEAVE_TYPE/LEAVE_DAYS/시각/노드).
    8) **결재선/자기승인(웹 161~200)**: aprvRequired 시 approverUserCds(또는 presetId 전개) 비면 `COMMON_400_001`. selfAllowed=`selectUserNodeSelfApproveYn` 'Y'. 루프: isSelf && !selfAllowed → `ATTD_400_056`; 첫 비-self 가 currentIdx. 단계 INSERT: self→'02'+"자체근태승인 자동 승인", 비-self→(i==currentIdx?'01':'00'). fullyAutoApproved = (currentIdx<0).
    9) **leave_use INSERT + grant recompute(웹 202~212)**: `selectNextLeaveId` → `insertLeaveUse`(LEAVE_STATUS='CONFIRMED', REQ_ID, GRANT_ID, 단위/일수/분) → `recomputeGrantUsedDays(cmpny, grantId, user)`.
    10) **즉시확정 + 출근차단(웹 214~223)**: aprvRequired && fullyAutoApproved → `updateReqStatus('02')`. confirmedNow(=!aprvRequired || fullyAutoApproved) && 단위='00' → `upsertWorkPlanLeave`(일단위 출근차단).
  - 영향 받는 파일 (신규 — 018-A 모듈에 추가):
    - controller: `app/leave/leaveflow/controller/AppLeaveFlowController.java` — `@PostMapping("/apply")` 메서드 **추가**(기존 컨트롤러 공용)
    - service: `app/leave/leaveflow/service/AppLeaveFlowService.java` — `void submitLeave(LeaveApplyParam)` **추가**
    - service impl: `app/leave/leaveflow/service/impl/AppLeaveFlowServiceImpl.java` — `@Transactional` submitLeave 구현 + 빈 주입(LeaveDeductionService/ApprovalLineMapper/AttdCloseService) **추가**
    - DTO: `app/leave/leaveflow/dto/request/LeaveApplyRequest.java` (신규)
    - param: `app/leave/leaveflow/application/param/LeaveApplyParam.java` (신규, `from(request, TokenInfo)`)
    - command: `app/leave/leaveflow/application/command/LeaveReqInsertCommand.java` (신규) · `LeaveUseCommand`(또는 VO) (신규)
    - mapper: `app/leave/leaveflow/mapper/AppLeaveFlowMapper.java` — 신규 메서드 9~10개 **추가** + `AppLeaveFlowMapper.xml`
    - result/VO: `DeductibleGrantRow`(grantId/grantDays/usedDays), `LeaveTypeInfoRow`(systemYn/aprvUseYn/useUnitType) (신규)
    - 에러코드: `common/error/attd/AttdErrorCode.java` — `ATTD_400_102` **1건 추가**
  - 영향 받는 endpoint: `POST /prafta/appApi/leaveflow/apply` (자동 prefix `com.prafta.app.*`)
  - 예상 산출물: controller 메서드/service+impl/param/command/mapper+xml/result VO/신규 에러코드
  - 연결 UI 명세: 없음(BE; 폼 화면은 018-C)
- **선행 작업**: prafta-app-018-A (모듈/helper/mapper 기반). prafta-app-010(mypage01 프리셋 매퍼)·공통 ApprovalLineMapper/LeaveDeductionService·web AttdCloseService 는 기존 운영.
- **우선순위 근거**: 법적 책임 영역(attd 연차) +1단계; 연차 차감을 동반하는 데이터 정합성 핵심 트랜잭션. 018-C(FE)의 제출 대상.

---

# 1) 엔드포인트 / 요청 DTO 확정

## 엔드포인트
| 메서드/경로(자동 prefix `/prafta/appApi`) | 식별값 | Tx | 비고 |
|---|---|---|---|
| `POST /leaveflow/apply` | JWT(cmpny/site/user) | `@Transactional(rollbackFor=Exception.class)` | 연차 신청 1건 처리(요청+사용+부여+결재선) |

## 요청 본문 `LeaveApplyRequest` (camelCase JSON)
| 키 | 타입 | 필수 | 검증 | 비고 |
|---|---|---|---|---|
| `leaveCd` | String | Y | `@NotBlank @Size(max=20)` | 연차코드 |
| `leaveType` | String | N | `@Size(max=10)` | 성격코드(ANNUAL/HALF_AM..). 표시·분류용(보안 비민감). 미전송 시 null 저장(웹 미러). |
| `workYmd` | String | Y | `@NotBlank @Pattern(\d{8})` | 근무일 YYYYMMDD |
| `useUnitType` | String | Y | `@NotBlank @Size(max=2)` | SYS025 |
| `startTime` | String | N(시간차 시 필수) | `@Size(max=4)` | HHMM |
| `endTime` | String | N(시간차 시 필수) | `@Size(max=4)` | HHMM |
| `reason` | String | N | `@Size(max=500)` | 사유 |
| `approverUserCds` | List\<String\> | N(결재필요+프리셋 미사용 시 필수) | — | 결재자 순서(1단계부터) |
| `presetId` | String | N | `@Size(max=20)` | 보조: approverUserCds 비었고 결재필요 시 본인 프리셋 전개 |

- ⚠️ **식별값(cmpny/site/user)·WORK_SEQ·nodeCd 등은 본문 비신뢰**. `nodeCd` 는 웹 미러상 요청 INSERT NODE_CD 에 들어가지만, 신뢰 토큰 경로가 없으면 **토큰/사용자 조회로 도출**하거나 null 저장. planner 권고: 본문 nodeCd 를 받지 말고 서버에서 사용자 NODE_CD 조회(또는 null) — 결재선 자기승인 판정은 어차피 `selectUserNodeSelfApproveYn` 가 서버 USER→NODE 조인으로 독립 수행하므로 nodeCd 본문 불요. (developer 가 NODE_CD 저장값을 사용자 조회로 채울지 null 로 둘지 확정; 표시영향만.)
- `Param.from` 에서 토큰 gv_cmpnyCd/gv_siteCd/gv_userCd 공백 검증 → `COMMON_400_001`(또는 003). 018-A `LeaveApplyMetaParam` 패턴 일관.

---

# 2) 처리 순서 단위작업화 (재사용 vs 신규)

> 단일 `@Transactional` 내부. 모든 게이트 통과 후에만 INSERT. 예외 시 전체 롤백(fail-closed).

| # | 단계 | 호출 | 재사용/신규 | 실패 코드 |
|---|---|---|---|---|
| 1 | 타입 메타 조회 | `AppLeaveFlowMapper.selectLeaveTypeInfo` | **신규 앱 매퍼**(웹 SQL 미러) | 404_030 |
| 2 | 결재필요 판정 | (법정) `selectPolicyAprvUseYn` / (비법정) 타입 APRV_USE_YN | **신규 앱 매퍼**(법정) + 타입행 재사용 | — |
| 3 | **단위 게이팅(신규)** | (법정) `selectCompanyUsageUnit`(018-A 기존) + `LeaveUnitGranularity`(018-A) | **재사용**(018-A 매퍼+helper) | **400_102(신규)** |
| 4 | 일 소정근로분 | `LeaveDeductionService.getDailyStdWorkMinutes` | **재사용**(common 빈) | (반차/시간차 분기서 052) |
| 5 | 구조검증/차감 | 단위분기 + `calcDeductionDays` + `crossesBreak` | **재사용**(common 빈) + 로컬 분기 | 052/054/055 |
| 6 | 사후 마감 | `AttdCloseService.isClosedForUser` | **재사용**(web 빈, 결정 1) | 050 |
| 7 | 잔여 부여 선택 | `AppLeaveFlowMapper.selectDeductibleGrant` (FOR UPDATE) | **신규 앱 매퍼**(웹 SQL 미러) | 051 |
| 8 | 요청 INSERT | `selectNextReqId` + `insertLeaveReq` | **신규 앱 매퍼**(웹 SQL 미러) | — |
| 9 | 결재선/자기승인 | `selectUserNodeSelfApproveYn`(신규 앱) + `ApprovalLineMapper.insertApprovalStep`(공통) + (옵션)`selectPresetStepUserCds`(신규 앱) | 혼합 | 056 / COMMON_400_001 |
| 10 | leave_use INSERT | `selectNextLeaveId` + `insertLeaveUse` | **신규 앱 매퍼**(웹 SQL 미러) | — |
| 11 | grant USED_DAYS recompute | `recomputeGrantUsedDays` | **신규 앱 매퍼**(웹 SQL 미러) | — |
| 12 | 즉시확정 | `updateReqStatus('02')` | **신규 앱 매퍼**(웹 SQL 미러) | — |
| 13 | 출근차단(단위='00') | `upsertWorkPlanLeave` | **신규 앱 매퍼**(웹 SQL 미러) | — |

신규 앱 매퍼 메서드(웹 `LeaveFlowMapper` SQL 본문을 그대로 복사, 네임스페이스만 app): `selectNextReqId`, `selectNextLeaveId`, `selectLeaveTypeInfo`, `selectPolicyAprvUseYn`, `selectDeductibleGrant`, `insertLeaveReq`, `insertLeaveUse`, `recomputeGrantUsedDays`, `updateReqStatus`, `upsertWorkPlanLeave`, `selectUserNodeSelfApproveYn`, (옵션)`selectPresetStepUserCds`.
> 웹 `LeaveFlowMapper` 를 직접 주입하지 않는 이유: 앱/웹 패키지 분리(메모리 project_prafta_app_010 "web직접호출 0건"). 단 **SQL 본문·컬럼·바인딩은 웹과 100% 동일**(미러). common 계층(`ApprovalLineMapper`, `LeaveDeductionService`)과 web `AttdCloseService` 만 빈 재사용.

---

# 3) 신규/재사용 매퍼 시그니처 + SQL 스케치 + 신규 에러코드

> 규칙: leading 콤마, `#{}` 바인딩, `SELECT *` 금지, 실제 컬럼만, 스코프 WHERE 명시. record/VO 위치매핑(SELECT 순서=생성자 인자 순서).

## 신규 에러코드 (실제 미사용 확인)
- `AttdErrorCode` 현재 최대 사용: `ATTD_400_101`(prafta-app-017). `ATTD_400_102` **미사용** 확인 → 단위 게이팅 거부에 배정.
- 추가: `ATTD_400_102(HttpStatus.BAD_REQUEST, "선택한 연차에서 사용할 수 없는 사용 단위입니다.")`
  - 사용자 친화 메시지(입력/비즈니스 룰 위반, 본인 인지·조치 가능). 정보 누출 없음.
- 재사용: 050/051/052/053(미사용 경로)/054/055/056/404_030 + COMMON_400_001.

## AppLeaveFlowMapper — 추가 메서드 (웹 SQL 미러)

```java
// 시퀀스/ID
String  selectNextReqId(@Param("cmpnyCd") String cmpnyCd);
String  selectNextLeaveId(@Param("cmpnyCd") String cmpnyCd);

// 타입/정책
LeaveTypeInfoRow selectLeaveTypeInfo(@Param("cmpnyCd") String c, @Param("leaveCd") String l);
String  selectPolicyAprvUseYn(@Param("cmpnyCd") String cmpnyCd);

// 잔여 부여 (FOR UPDATE, 만료임박 우선)
DeductibleGrantRow selectDeductibleGrant(@Param("cmpnyCd") String c, @Param("userCd") String u,
    @Param("leaveCd") String l, @Param("workYmd") String w, @Param("neededDays") BigDecimal n);

// 요청/사용/부여
int insertLeaveReq(LeaveReqInsertCommand cmd);
int insertLeaveUse(LeaveUseCommand cmd);
int recomputeGrantUsedDays(@Param("cmpnyCd") String c, @Param("grantId") String g, @Param("updateNo") String u);
int updateReqStatus(@Param("cmpnyCd") String c, @Param("reqId") String r,
    @Param("reqStatus") String s, @Param("processUserCd") String p, @Param("processComment") String cm);
int upsertWorkPlanLeave(@Param("cmpnyCd") String c, @Param("siteCd") String s, @Param("userCd") String u,
    @Param("workYmd") String w, @Param("leaveCd") String l, @Param("insertNo") String i);

// 자기승인 자격 (웹 selectUserNodeSelfApproveYn 미러)
String selectUserNodeSelfApproveYn(@Param("cmpnyCd") String c, @Param("userCd") String u);

// (옵션) presetId → 스텝 USER_CD 전개 (본인 소유 스코프 강제)
List<String> selectPresetStepUserCds(@Param("cmpnyCd") String c,
    @Param("userCd") String u, @Param("presetId") String p);
```

### 핵심 SQL 스케치 (웹 미러, 네임스페이스 app)
- `selectLeaveTypeInfo`/`selectPolicyAprvUseYn`/`selectDeductibleGrant`/`insertLeaveReq`/`insertLeaveUse`/`recomputeGrantUsedDays`/`updateReqStatus`/`upsertWorkPlanLeave`/`selectUserNodeSelfApproveYn`: **웹 `LeaveFlowMapper.xml` 의 동명 statement 본문을 그대로 복사**(컬럼·바인딩·`FOR UPDATE`·`ON DUPLICATE KEY` 포함). 위 web XML(L5~410)이 검증된 원본.
  - `selectDeductibleGrant`: `neededDays` 파라미터명 사용(웹 XML `#{neededDays}` 와 동일하게 — 서비스에서 leaveDays 를 neededDays 로 전달).
  - `insertLeaveReq` VALUES 의 REQ_TYPE 은 리터럴 `'05'` 고정(웹 동일, 타입혼동 방지).
- (옵션) `selectPresetStepUserCds` (신규, 본인 소유 스코프):
```sql
/* AppLeaveFlowMapper.selectPresetStepUserCds */
SELECT D.APPROVER_USER_CD
  FROM TB_APRV_LINE_PRESET P
  JOIN TB_APRV_LINE_PRESET_D D
    ON  D.CMPNY_CD  = P.CMPNY_CD
    AND D.PRESET_ID = P.PRESET_ID
 WHERE P.CMPNY_CD = #{cmpnyCd}
   AND P.USER_CD  = #{userCd}        -- 본인 소유만(타인 프리셋 차단)
   AND P.PRESET_ID = #{presetId}
   AND P.USE_YN   = 'Y'
 ORDER BY D.STEP_NO ASC
```
  ⚠️ `tb_aprv_line_preset_d` 컬럼명(APPROVER_USER_CD/STEP_NO/USE_YN)은 018-A mypage01 매퍼와 동일 가정 — developer 가 mypage01 `selectPresetStepsByUser` SQL 의 실제 컬럼으로 교차확인(스냅샷 stale 가능). 본 매퍼 신설 대신 mypage01 의 기존 스텝 조회를 재사용해 서비스에서 presetId 필터링해도 동등(중복 회피 우선).

## 재사용 매퍼/서비스 (무변경)
- `ApprovalLineMapper.insertApprovalStep(ApprovalStepVO)` / `selectFirstWaitingStep` — 공통, 웹과 동일.
- `LeaveDeductionService.{getDailyStdWorkMinutes, calcDeductionDays, crossesBreak}` — common 빈.
- `AttdCloseService.isClosedForUser(cmpny, site, user, closeYm)` — web 빈(결정 1).
- 018-A: `AppLeaveFlowMapper.selectCompanyUsageUnit` + `LeaveUnitGranularity.{usageUnitToCode, allowedUnitsByCode}`.

## 신규 result VO / command (위치매핑 주의)
```java
record LeaveTypeInfoRow(String systemYn, String aprvUseYn, String useUnitType)        // SELECT 순서 일치
record DeductibleGrantRow(String grantId, BigDecimal grantDays, BigDecimal usedDays)   // SELECT 순서 일치
record LeaveReqInsertCommand(String reqId, String cmpnyCd, String siteCd, String userCd,
    String reqStatus, String reqReason, String workYmd, String nodeCd,
    String startDate, String startTime, String endDate, String endTime,
    String leaveType, BigDecimal leaveDays, String insertNo)                           // 웹 command 미러
class  LeaveUseCommand { leaveId,cmpnyCd,siteCd,userCd,leaveCd,reqId,grantId,
    startDate,startTime,endDate,endTime,useUnitType,leaveDays,leaveMinutes,
    leaveReason,leaveStatus,insertNo }   // 웹 LeaveUseVO 미러(@Getter/@Builder, INSERT 파라미터 이름매핑이라 위치무관)
```
> `insertLeaveReq`/`insertLeaveUse` 는 **이름 기반 `#{...}` 매핑**(위치 아님)이라 위치매핑 함정 비해당. 위치매핑은 `selectDeductibleGrant`/`selectLeaveTypeInfo` 처럼 **record 로 받는 SELECT** 에만 적용 → SELECT 컬럼 순서 = record 인자 순서 엄수.

---

# 4) 결재선 INSERT 데이터모델 + 자기승인 판정

## TB_USER_ATTD_REQ_APPROVAL 단계 INSERT (공통 ApprovalLineMapper.insertApprovalStep, ApprovalStepVO)
| 컬럼 | 값 | 비고 |
|---|---|---|
| REQ_ID | 신규 reqId | 요청 1:N 단계 |
| APPROVAL_STEP | i+1 (1부터) | approverUserCds 순서 |
| CMPNY_CD | gv_cmpnyCd | |
| APPROVER_USER_CD | approverUserCds[i] (또는 preset 전개값) | |
| APPROVAL_STATUS [SYS044] | self→`'02'` / 비-self & i==currentIdx→`'01'` / 그외→`'00'` | currentIdx=첫 비-self 인덱스 |
| APPROVAL_COMMENT | self→`"자체근태승인 자동 승인"` / else null | |
| INSERT_NO | gv_userCd | |
| (APPROVAL_DATE/UPDATE_*) | INSERT 시 NULL/NOW (매퍼 고정) | |

- self 단계 자동승인 처리는 **insert 시점 '02'** 로 박는다(웹 191~192 동일). 별도 update 없음.
- 전 단계가 모두 self('02')라 currentIdx<0 → `fullyAutoApproved=true` → leave_use INSERT 후 `updateReqStatus('02')` 로 요청 즉시 확정(웹 199/214~217).

## 자기승인 판정 입력 (SELF_ATTD_APPRV_YN 출처)
- `selectUserNodeSelfApproveYn(cmpny, user)`(웹 미러): `TB_USER U JOIN TB_SITE_NODE N (U.NODE_CD)` 에서 **N.SELF_ATTD_APPRV_YN='Y' AND (N.MAIN_ADMIN_CD=U.USER_CD OR N.SUB_ADMIN_CD=U.USER_CD)** 이면 'Y' 반환, 아니면 행없음(NULL=fail-closed).
- selfAllowed = `"Y".equals(...)`. **본인이 결재자인데 selfAllowed=false → `ATTD_400_056`**(본인 지정 불가). 자격 충족 시에만 self 단계 자동승인.
- 입력은 **gv_userCd(JWT) 만**(클라 nodeCd 불신뢰). 노드/관리자 자격은 전부 서버 조인으로 산출.

---

# 5) 수용 기준 / 엣지

## 공통
- [ ] 식별값(cmpny/site/user) JWT(`TokenInfo gv_*`)에서만. 본문 식별값/nodeCd/WORK_SEQ 미신뢰(IDOR 차단). approverUserCds/presetId 만 본문 신뢰(소유권은 서버 검증).
- [ ] 단일 `@Transactional(rollbackFor=Exception.class)`. 모든 게이트 통과 후 INSERT. 예외 시 요청/사용/부여/결재선 전부 롤백(fail-closed).
- [ ] 모든 SQL: `SELECT *` 없음, leading 콤마, `#{}` 바인딩, 스코프 WHERE 명시. 웹 미러 SQL 컬럼 100% 일치.
- [ ] record SELECT 위치매핑 일치(LeaveTypeInfoRow/DeductibleGrantRow). REQ_TYPE='05' 리터럴 고정(타입혼동 방지).
- [ ] 빌드 `gradlew compileJava compileTestJava --no-daemon` 통과.

## 단위 게이팅(D2, 신규)
- [ ] 허용단위 외 제출 → `ATTD_400_102` 거부(구조검증/차감 진입 전). 예: 법정 USAGE_UNIT='HOUR_1'(허용 00/01/02/03)에서 `useUnitType='04'` → 102.
- [ ] 허용 집합 산출이 018-A `apply-meta.allowedUnits` 와 **동일 상수**(LeaveUnitGranularity SSOT) → 메타에서 노출된 단위는 제출 시 102 안 남(메타-제출 정합).
- [ ] 비법정 USE_UNIT_TYPE=NULL → 허용 [00] → 00 외 단위 102.
- [ ] 법정정책 미존재(usagePolicy null) → 허용 [00] 폴백(메타 §폴백과 동일). 102 외 500 폭주 없음.

## 구조검증/차감(웹 미러)
- [ ] 00 → leaveDays=1.0, 시각 null.
- [ ] 01 → 0.5 + leaveMinutes=daily/2(daily null이면 minutes만 null, 일수 0.5 유지=웹 동일).
- [ ] 02/03/04 → start/end 필수, e>s, `(e-s)%unitMin==0` 아니면 054, `minutes>daily` 또는 daily null 이면 052, **휴게 가로지름(crossesBreak) → 055**, calcDeductionDays null → 052.
- [ ] 알 수 없는 단위 코드(게이팅 통과 후에도) → 054(웹 else 분기 미러).
- [ ] 시간차 휴게 가로지름 엣지: 신청 [s,e) 가 휴게 [bs,be) 와 겹치면 055. 휴게 미설정/0폭이면 통과(보수처리=웹 동일).

## 사후 마감(prafta-028)
- [ ] `workYmd < today` & 사용자 소속부서 해당월 마감 → `ATTD_400_050`. (당일/미래는 마감검사 skip — 웹 동일.)
- [ ] AttdCloseService.isClosedForUser 가 사용자 NODE_CD 서버 조회 → 클라 nodeCd 불신뢰.

## 잔여(D3)
- [ ] 차감대상 부여(ACTIVE+DEL_YN='N'+유효기간 포함+잔여≥leaveDays) 없으면 `ATTD_400_051`. 만료 임박(AVAIL_TO_DATE ASC) 우선 소진.
- [ ] `FOR UPDATE` 로 동시 차감 직렬화. recompute 후 USED_DAYS = 해당 부여 CONFIRMED 합계.
- [ ] 018-A apply-meta `applicable=true`(낙관적 표시)였어도 제출 시점 잔여부족이면 051(비관적 차감) — 정상 동작.

## 결재선/자기승인(D1, §9.5)
- [ ] aprvRequired & approverUserCds·presetId 모두 비면 `COMMON_400_001`.
- [ ] presetId 만 주고 approverUserCds 비면 본인 프리셋 전개(타인 프리셋 PRESET 소유 스코프로 차단 → 빈 전개면 COMMON_400_001).
- [ ] 본인이 결재자 + selfAllowed=false → `ATTD_400_056`(본인 지정 불가). selfAllowed=true → 해당 단계 '02' 자동승인.
- [ ] 첫 비-self 단계만 '01', 나머지 비-self '00'. self 는 '02'.
- [ ] 전 단계 self 자동승인(currentIdx<0) → 요청 즉시 '02' 확정 + (단위='00'이면) 출근차단 upsert.
- [ ] aprvRequired=false(법정 APRV_USE_YN='N' 또는 비법정 타입 'N') → 결재선 INSERT 없음, REQ_STATUS '02', leave_use CONFIRMED, 단위='00'이면 출근차단.

## 출근차단(§8.3)
- [ ] confirmedNow && 단위='00' → upsertWorkPlanLeave(WORK_PLAN_CD=leaveCd). 반차/시간차는 미적용(출퇴근 단계서 leave_use 기반 차단 — 웹 동일).
- [ ] 결재 대기('01') 상태면 출근차단 미적용(확정 전).

## 보안 검토 위임 포인트 (security)
- approverUserCds 의 결재자 USER_CD 가 **타 회사/타 사업장** 사용자일 가능성 — 웹 submitLeave 는 결재자 사업장 스코프 검증을 하지 않음(미러 시 동일 갭). planner 권고: 본 단위에서 결재자 후보가 018-A approver-search(사업장 스코프) 산출이라는 전제이나, 서버가 **approverUserCds 각 항목의 회사/사업장 스코프를 검증하지 않으면 cross-site 결재자 주입** 가능 → security 가 검증 필요성 판단(웹 패리티 유지 vs 앱에서 강화). 최소한 self 판정 외 결재자 유효성(존재·동일회사) 가드 검토.
- AttdCloseService(web 빈) app 주입 = 의도된 단일 app→web 의존(읽기전용 판정). 순환참조/스코프 확인.
- presetId 전개 SQL 의 본인 소유 스코프(`P.USER_CD=#{userCd}`) — 타인 프리셋 차단 확인.
- leave_use/grant FOR UPDATE 락 + recompute 정합(이중차감 없음, USED_DAYS=CONFIRMED 합계).

## QA 엣지 (스펙 가정 도전 — 골격 로직버그 사각 주의)
- [ ] approverUserCds 에 **중복 USER_CD**(같은 결재자 2회) — 웹은 단계만 늘림(중복 허용). 의도인지 도전. (현 미러=허용; 정책 미정이면 보고.)
- [ ] approverUserCds 에 **본인이 여러 번** + selfAllowed=true → 모든 self 단계 '02', currentIdx 는 비-self 첫 인덱스. 전부 self면 즉시확정.
- [ ] `useUnitType='01'`(반차)인데 startTime/endTime 도 함께 옴 → 반차는 시각 무시(웹: 반차 분기서 start/end null 유지). leave_use 에 시각 null 저장 확인(요청자가 보낸 시각이 새지 않는지).
- [ ] daily(소정근로분) null(연차일 등 스케줄 없음) + 시간차 신청 → 052(웹 동일). 반차 신청 → 0.5 저장되나 leaveMinutes null.
- [ ] LEAVE_DAYS 컬럼 정밀도 차이: 요청 테이블(3,1) vs 사용 테이블(5,1). 시간차 차감 0.33.. → 요청은 0.3 반올림, 사용은 0.3? 실제 차감(USED_DAYS recompute)은 **leave_use.LEAVE_DAYS(5,1) 합**이 SSOT → 잔여 정합은 leave_use 기준. (요청 LEAVE_DAYS 는 표시값.) qa 가 차감 정합을 leave_use/grant 로 검증(요청 테이블 아님).
- [ ] 동시 2건 같은 부여 차감(경합) → FOR UPDATE 직렬화로 한쪽 대기, 둘째가 잔여부족이면 051.

## follow-up (본 단위 밖)
- 결재자 cross-site 스코프 강화(웹 공통 갭) — security 판단 후 별도.
- prafta-app-009 결재선 일반화(스케줄/보정/초과근무 폼) — 본 단위는 연차 범위만.
- prafta-031 알림 outbox(연차 신청/결재 통지) — 미포함.
- 반려/승인(approveStep/rejectStep) 앱 미러 — 본 단위는 **신청(쓰기)만**. 웹 my-approvals/approve/reject 의 앱 미러는 별도(앱 결재함 화면 시).
