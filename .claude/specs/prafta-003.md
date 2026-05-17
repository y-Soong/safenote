# PRAFTA-003 — 초과근무 요청 API 신규 + TB_USER_ATTD_REQ 스키마 마이그레이션

- 정책서: `.claude/policies/prafta-003.txt`
- 작업 영역: web (관리자 화면) + 백엔드(`com.prafta.web.attd.attd07`)
- 기준 DB 스키마: 정책서에 첨부된 **신규** `TB_USER_OVERTIME_MGMT`, **신규 구조** `TB_USER_ATTD_REQ`
  - 주의: `.claude/context/schema-full.sql`(line 957)에는 **옛 구조의 `tb_user_attd_req`**만 존재. PRAFTA-003 적용 시 새 DDL이 반영되어야 코드와 정합. 본 명세는 정책서의 새 DDL이 곧 반영된다는 전제로 작성.

---

## 1. 서브태스크 개요

| ID | 유형 | 제목 | 영향 파일 핵심 |
|----|------|------|----------------|
| B1 | backend | 신규 API `POST /attd07/update-user-overtime-requests` | controller/service/mapper/dto/param/command 신규, `TB_USER_OVERTIME_MGMT` INSERT |
| B2-1 | backend | `result.UserAttdReqResult` 필드 마이그레이션 | `result/UserAttdReqResult.java` (attdId→targetId, checkIn/Out → start/end, otType/leaveType/leaveDays/processDate/updateNo/updateDate 추가) |
| B2-2 | backend | `result.MonthlyAttdReqResult` 필드 마이그레이션 | `result/MonthlyAttdReqResult.java` (checkIn/Out 4컬럼 → start/end 4컬럼) + 한글 깨진 주석 영어로 교체 |
| B2-3 | backend | `mapper/Attd07Mapper.xml.selectMonthlyAttdReq` SQL 마이그 | CHECK_IN_*/CHECK_OUT_* → START_*/END_*, REQ_STATUS `'01'` → `'REQUESTED'` |
| B2-4 | backend | `mapper/Attd07Mapper.xml.selectMonthlyAttdReqSummary` SQL 마이그 | REQ_STATUS `'01'` → `'REQUESTED'` |
| B2-5 | backend | `mapper/Attd07Mapper.xml.selectUserAttdReqByReqId` SQL 마이그 | SELECT 컬럼 ATTD_ID → TARGET_ID, 결과 매핑 변경 |
| B2-6 | backend | `mapper/Attd07Mapper.xml.updateUserAttdReqApprove` SQL 마이그 | SET REQ_STATUS `'02'` → `'APPROVED'`, `ATTD_ID = #{...}` → `TARGET_ID = #{...}`, `PROCESS_DATE = NOW()`, `UPDATE_NO = #{gvUserCd}`, `UPDATE_DATE = NOW()` 추가, WHERE REQ_STATUS `'01'` → `'REQUESTED'` |
| B2-7 | backend | `service/impl/Attd07ServiceImpl.updateUserAttdRequest` 상태 코드 마이그 | `"01".equals(reqRow.reqStatus())` → `"REQUESTED".equals(...)`, `reqRow.attdId()` 호출부 → `reqRow.targetId()` |
| B2-8 | backend | `application/command/UpdateUserAttdRequestCommand` 변경 | `attdId` 필드명 → `targetId` (의미 확장) |
| F1 | frontend-screen | `AttdDayDetailPop.vue` 추가근무 등록 신규 API 연동 골격 | 추가근무 카드의 UI(이미 있음)에 저장 버튼/유효성 안내 영역 추가, payload 정의는 골격만, script body는 developer가 채움 |
| F2 | frontend-screen | `AttdDayDetailPop.vue` 기존 요청 페이로드 키 마이그 | `checkInDate/Time/checkOutDate/Time` → `startDate/Time/endDate/Time`. 응답 매핑 4건(reqCards.aftIn/aftOut, fillSegmentFromReq) 동시 마이그 |

---

## 2. B1 — 신규 API 명세

### 2-1. 엔드포인트

- 메서드: `POST`
- URL: `/attd07/update-user-overtime-requests`
- 인증: JWT 필수 (`Authorization` 헤더, `gvCmpnyCd / gvUserCd / gvSiteCd` claim)
- Content-Type: `application/json`

### 2-2. 요청 DTO — `UpdateUserOvertimeRequestRequest`

| 필드 | 타입 | 필수 | 검증 | 의미 |
|------|------|------|------|------|
| `userCd` | String | Y | NotBlank | 대상 근로자 코드 |
| `siteCd` | String | Y | NotBlank | 사업장 코드 |
| `nodeCd` | String | N | - | 근무 노드 |
| `workYmd` | String | Y | `^[0-9]{8}$` | 기준 근무일 (YYYYMMDD) |
| `attdId` | String | N | - | 연관 근태 ID (정규근태가 있는 경우) |
| `reqId` | String | N | - | 연관 요청 ID (요청 경유 등록 시) |
| `overtimes` | List | Y | 1건 이상 | 등록할 OT 구간들 |
| `overtimes[].otType` | String | Y | EXTEND/NIGHT/HOLIDAY | OT 유형 |
| `overtimes[].startDate` | String | Y | `^[0-9]{8}$` | 시작 일자 |
| `overtimes[].startTime` | String | Y | `^[0-9]{4}$` | 시작 시각 HHMM |
| `overtimes[].endDate` | String | Y | `^[0-9]{8}$` | 종료 일자 |
| `overtimes[].endTime` | String | Y | `^[0-9]{4}$` | 종료 시각 HHMM |
| `reqReason` | String | N | Size <= 500 | 사유 |

### 2-3. 응답

- HTTP 200, body empty (기존 update 패턴과 통일)
- 실패 시 `ApiException` → `AttdErrorCode`

### 2-4. 에러 코드 (신규 추가 필요)

| 코드 | HTTP | 메시지 (영어) | 용도 |
|------|------|---------------|------|
| `ATTD_400_010` | 400 | `Overtime list is empty.` | overtimes가 비어있음 |
| `ATTD_400_011` | 400 | `Overtime range is invalid.` | 시작 >= 종료, HHMM 파싱 실패 |
| `ATTD_400_012` | 400 | `Overtime exceeds allowed window outside of schedule.` | 시간 검증 규칙(2-5) 위배 |
| `ATTD_400_013` | 400 | `Overtime segments overlap.` | OT 구간끼리 겹침 |
| `ATTD_404_010` | 404 | `Schedule for the work day not found.` | TB_USER_WORK_PLAN/TB_SCH_MGMT 미존재 |

### 2-5. 시간 검증 알고리즘 (정책서 2,3번)

**개념**: `허용 OT 범위 = 표준화 적용 근무시간 − 스케줄 시간` (집합 차집합). 각 OT 구간은 이 차집합에 완전 포함되어야 한다.

**의사코드** (분 단위 정수 stamp 사용. 자정 넘김 처리 위해 workYmd 기준 0~2879분):

```
INPUT:
  schSegs   = [(s,e), ...]  // TB_SCH_MGMT 1,2구간 (FST_SCH_*, SEC_SCH_*)
  stdSegs   = [(s,e), ...]  // 표준화 적용 실제 근무 구간 (FNC_STD_TIME 적용 결과,
                            //   TB_USER_ATTD_MGMT 1·2차 row에서 계산)
  reqOts    = [(s,e,type), ...]  // 요청으로 들어온 OT 구간

STEP 1. normalize:
  모든 (date,time) -> 분 stamp.
  workYmd 자정 = 0. 익일이면 +1440.
  s < e 보장. 아니면 ATTD_400_011.

STEP 2. mergeIntervals(stdSegs)  // 표준화 구간 합치기
STEP 3. mergeIntervals(schSegs)
STEP 4. allowed = subtractIntervals(stdSegs, schSegs)
         // 차집합: 표준화에는 있고 스케줄에는 없는 부분 = OT 허용 영역
STEP 5. mergeIntervals(reqOts)
        // 요청들끼리 겹치면 ATTD_400_013

STEP 6. for each ot in reqOts:
          if NOT exists a ∈ allowed such that a.s <= ot.s AND a.e >= ot.e:
              throw ATTD_400_012
```

**참고 케이스 검증** (정책서 Ex1):
- 스케줄 09:00~18:00 → schSegs = [(540, 1080)]
- 표준화 09:00~21:00, 22:00~23:00 → stdSegs = [(540, 1260), (1320, 1380)]
- allowed = stdSegs − schSegs = [(1080, 1260), (1320, 1380)] → 18:00~21:00, 22:00~23:00 ✔

**참고 케이스 Ex2**:
- 스케줄 03:00~09:00, 14:00~20:00 → [(180, 540), (840, 1200)]
- 표준화 03:00~09:30, 13:00~21:00 → [(180, 570), (780, 1260)]
- allowed = [(540, 570), (780, 840), (1200, 1260)] → 09:00~09:30, 13:00~14:00, 20:00~21:00 ✔

### 2-6. 처리 로직 (Service)

1. JWT에서 `gvCmpnyCd`, `gvUserCd` 추출.
2. `TB_USER_WORK_PLAN` + `TB_SCH_MGMT` 조회 → schSegs 계산 (없으면 ATTD_404_010).
3. `TB_USER_ATTD_MGMT` 1·2차 row 조회 + `FNC_STD_TIME` 함수로 표준화 시각 변환 → stdSegs.
4. 시간 검증 알고리즘(2-5) 수행.
5. for each ot: `selectOtId(gvCmpnyCd)`(시퀀스) → `INSERT INTO TB_USER_OVERTIME_MGMT (..., OT_STATUS='COMPLETED', PLAN_*=요청값, ACTUAL_*=요청값, INSERT_NO=gvUserCd, INSERT_DATE=NOW())`.
6. `@Transactional` 보장. 한 건이라도 실패하면 전체 롤백.

### 2-7. 시퀀스

- `selectOtId(gvCmpnyCd)` → `CONCAT(DATE_FORMAT(NOW(),'%Y%m%d'), FNC_CMM_SEQ_NEXTVAL(#{gvCmpnyCd}, 'OT_ID'))`. (기존 `ATTD_ID` 시퀀스 패턴 동일).

---

## 3. B2 — 마이그 체크리스트 (변경 매트릭스)

### 3-1. TB_USER_ATTD_REQ 컬럼 매핑

| 영역 | 변경 전 | 변경 후 | 비고 |
|------|---------|---------|------|
| 컬럼 | `ATTD_ID` | `TARGET_ID` | 의미 확장 (ATTD/OT/LEAVE 공용) |
| 컬럼 | `CHECK_IN_DATE` | `START_DATE` | |
| 컬럼 | `CHECK_IN_TIME` | `START_TIME` | |
| 컬럼 | `CHECK_OUT_DATE` | `END_DATE` | |
| 컬럼 | `CHECK_OUT_TIME` | `END_TIME` | |
| 신규 컬럼 | - | `PROCESS_DATE` | 승인/반려 시점 |
| 신규 컬럼 | - | `UPDATE_NO`, `UPDATE_DATE` | 수정 추적 |
| 신규 컬럼 | - | `OT_TYPE`, `LEAVE_TYPE`, `LEAVE_DAYS` | 요청 유형별 부가 |
| 값 (REQ_STATUS) | `'01'` (신청) | `'REQUESTED'` | |
| 값 (REQ_STATUS) | `'02'` (승인) | `'APPROVED'` | |
| 값 (REQ_STATUS) | - | `'REJECTED'`, `'CANCELLED'` | 추가 |

### 3-2. 영향 받는 파일 매트릭스

| # | 파일 | 변경 사항 |
|---|------|----------|
| 1 | `result/UserAttdReqResult.java` | `attdId` → `targetId` 필드명 변경. 의미 확장 주석 추가. |
| 2 | `result/MonthlyAttdReqResult.java` | `checkInDate/Time, checkOutDate/Time` → `startDate/Time, endDate/Time`. 한글 깨진 javadoc을 영어로 교체. |
| 3 | `mapper/Attd07Mapper.xml` / `selectMonthlyAttdReq` | `A.CHECK_IN_DATE AS checkInDate` 등 4건 → `A.START_DATE AS startDate` 등 4건. WHERE `A.REQ_STATUS = '01'` → `'REQUESTED'`. |
| 4 | `mapper/Attd07Mapper.xml` / `selectMonthlyAttdReqSummary` | WHERE `A.REQ_STATUS = '01'` → `'REQUESTED'`. 다른 컬럼 사용 없음. |
| 5 | `mapper/Attd07Mapper.xml` / `selectUserAttdReqByReqId` | SELECT 절 `R.ATTD_ID AS attdId` → `R.TARGET_ID AS targetId`. |
| 6 | `mapper/Attd07Mapper.xml` / `updateUserAttdReqApprove` | `REQ_STATUS = '02'` → `'APPROVED'`. `ATTD_ID = #{attdId}` → `TARGET_ID = #{targetId}`. SET 절에 `PROCESS_DATE = NOW(), UPDATE_NO = #{gvUserCd}, UPDATE_DATE = NOW()` 추가. WHERE `REQ_STATUS = '01'` → `'REQUESTED'`. |
| 7 | `application/command/UpdateUserAttdRequestCommand.java` | `attdId` 필드명 → `targetId`. factory method 시그니처 `from(String targetId, ...)`. |
| 8 | `service/impl/Attd07ServiceImpl.java` (`updateUserAttdRequest`) | (a) `"01".equals(reqRow.reqStatus())` → `"REQUESTED".equals(reqRow.reqStatus())` (b) `reqRow.attdId()` 사용처 → `reqRow.targetId()` (c) `UpdateUserAttdRequestCommand.from(attdId, param)` 호출부에서 변수명 `targetId`로 정리 (값 자체는 ATTD_ID 동일, REQ_TYPE이 근태 수정인 경우 한정) |
| 9 | `mapper/Attd07Mapper.xml.selectMonthlyAttdReq` cont. | (위 #3과 동일 항목) — REQ_TYPE/REQ_STATUS의 코드값 시스템(SYS032/SYS033)이 더 이상 `'01'`을 신청으로 매핑하지 않을 가능성. `FNC_CMM_INFO_SRCH(...'SYS033')` 호출은 새 코드값(REQUESTED 등)이 코드 마스터에 등록되어 있다는 전제. **검증 필요 — developer가 baim 코드 마스터 확인 후 코드값 row 보강** |
| 10 | (선택) `common/error/attd/AttdErrorCode.java` | OT 검증용 신규 코드 5건 추가 (`ATTD_400_010~013`, `ATTD_404_010`). |

### 3-3. 손대지 않을 영역 (정책서 27행 준수)

- `selectMonthlyAttdList`, `selectDailyAttdDetails`, `selectDailyAttdDetailHistory` — TB_USER_ATTD_MGMT/HIST만 사용하므로 변경 없음.
- `updateUserAttdInfos`, `insertUserAttdInfos` (TB_USER_ATTD_HIST) — TB_USER_ATTD_REQ를 안 다룸. 변경 없음.
- `dailyAttdDetailDelete`, `insertDailyAttdDetailDeleteHist` — TB_USER_ATTD_REQ를 안 다룸. 변경 없음.
- `MonthlyAttdReqSummaryResult.java` — `reqId, workYmd, userCd`만 가지며 변경된 컬럼 사용 없음. 변경 없음.

---

## 4. 신규/수정 파일 트리

```
prafta-backend/src/main/java/com/prafta/web/attd/attd07/
  ├── controller/Attd07Controller.java               (B1: @PostMapping("/update-user-overtime-requests") 추가)
  ├── service/Attd07Service.java                     (B1: void updateUserOvertimeRequests(...) 시그니처 추가)
  ├── service/impl/Attd07ServiceImpl.java            (B1: 구현 + 시간 검증, B2-7: 상태값/필드명 마이그)
  ├── mapper/Attd07Mapper.java                       (B1: insertUserOvertime/selectOtId/스케줄·표준화 조회용 메서드 추가)
  ├── dto/request/UpdateUserOvertimeRequestRequest.java         (B1 신규)
  ├── dto/request/OvertimeItemRequest.java                      (B1 신규, overtimes 리스트 element)
  ├── application/param/UpdateUserOvertimeRequestParam.java     (B1 신규)
  ├── application/model/OvertimeItemModel.java                  (B1 신규)
  ├── application/command/InsertUserOvertimeCommand.java        (B1 신규)
  ├── application/query/OvertimeAllowedWindowQuery.java         (B1 신규 — 스케줄/표준화 조회용)
  ├── application/command/UpdateUserAttdRequestCommand.java     (B2-8: attdId → targetId)
  └── result/UserAttdReqResult.java                             (B2-1: attdId → targetId)
  └── result/MonthlyAttdReqResult.java                          (B2-2: checkIn/Out → start/end + 주석 영어)

prafta-backend/src/main/resources/com/prafta/web/attd/attd07/mapper/
  └── Attd07Mapper.xml                              (B2-3,4,5,6 + B1 신규 statement insertUserOvertime, selectOtId, selectAllowedWindow)

prafta-backend/src/main/java/com/prafta/common/error/attd/
  └── AttdErrorCode.java                            (B1: OT 에러 5건 추가)

prafta-web-frontend/prafta-web-frontend/src/views/attd/popup/
  └── AttdDayDetailPop.vue                          (F1: OT 저장 영역 골격 + F2: REQ 페이로드 키 마이그)
```

---

## 5. F1 / F2 — Vue 명세

### 5-1. F2 — REQ 페이로드 키 마이그 (line 단위)

| 화면 위치(라인 추정) | 변경 전 | 변경 후 |
|----------------------|---------|---------|
| 1310 | `aftIn: fmtTime(req.checkInTime)` | `aftIn: fmtTime(req.startTime)` |
| 1311 | `aftOut: fmtTime(req.checkOutTime)` | `aftOut: fmtTime(req.endTime)` |
| 1602 | `seg.inDate = ymdNumToDash(req.checkInDate)` | `seg.inDate = ymdNumToDash(req.startDate)` |
| 1603 | `seg.in = req.checkInTime` | `seg.in = req.startTime` |
| 1604 | `seg.outDate = ymdNumToDash(req.checkOutDate)` | `seg.outDate = ymdNumToDash(req.endDate)` |
| 1605 | `seg.out = req.checkOutTime` | `seg.out = req.endTime` |
| 1633~1638 (fnApproveReq payload) | `checkInDate / checkInTime / checkOutDate / checkOutTime` | **유지** (이 payload는 `update-user-attd-requests` API용 — B2-7로 인해 서버 측 ParamMapping이 변경되더라도 prafta 변환 정책상 request DTO는 그대로 두는 게 안전. 단, developer가 백엔드 신규 Request DTO 키도 동시에 마이그하면 함께 변경) |

**developer 작업 가이드**: 1500~1546 라인의 `fnSave`는 `update-user-attd-infos`(TB_USER_ATTD_MGMT 직접 갱신) API 호출이라 TB_USER_ATTD_REQ 컬럼 변경과 **무관**. 손대지 말 것.

### 5-2. F1 — 추가근무 카드 신규 API 연동 골격

**현재 상태 (line 439~533)**: 이미 추가근무 UI(시작/종료 입력, 추가/삭제 버튼)가 정규근무 segment 안에 들어있고, `addOt`/`removeOt`/`segSummary`만 화면 상태 관리됨. 저장 API 미연동.

**골격 추가 사항**:
1. 추가근무 블록 헤더 우측에 "허용 범위 안내" 영역 신설 — 정책서 2,3번에 따라 표준화-스케줄 차집합을 사용자에게 보여준다 (예: "초과근무 허용 범위: 18:00~21:00 / 22:00~23:00").
2. 추가근무 블록 하단에 "초과근무 저장" 버튼 신설 (`fnApproveOvertime`). 기존 `fnSave`(정규근무 저장)와는 별도 버튼으로 분리한다 — 정규근무와 초과근무는 다른 테이블에 저장되므로 합치지 않는다.
3. 추가근무 블록 하단에 "반려" 버튼 (`fnRejectOvertime`) — REQ_ID 경유 등록일 때만 의미 있음. `v-if`로 조건부 노출.
4. payload 키는 정책서 신규 테이블 컬럼명 기준: `otType`, `startDate`, `startTime`, `endDate`, `endTime`.

**컴포넌트 매핑**:

| 영역 | 컴포넌트 |
|------|----------|
| 시작/종료 일자 | `CalendarSrch` (기존) |
| 시작/종료 시각 | native `<input type=text inputmode=numeric>` (기존 패턴 유지) |
| 저장/반려 버튼 | native `<button>` (기존 `.save-btn` 패턴) |
| 알림 | `proxy.$alert` / `proxy.$confirm` |

**허용 범위 표시 데이터 소스**: developer는 컴포넌트 props/record 값에서 `plan1Start/End`, `plan2Start/End` (스케줄) + `act1InStdTime/act1OutStdTime`, `act2InStdTime/act2OutStdTime` (표준화) 를 사용해 `computed otAllowedWindows`를 계산한다.

**사용자 플로우**:
1. 추가근무 추가 버튼 → 빈 행 push.
2. 입력 → 클라이언트 검증 (정책서 2,3번 차집합).
3. 저장 버튼 → `POST /attd07/update-user-overtime-requests` 호출 → 200 시 alert + emit close.
4. 서버 검증 실패 시 alert에 에러 메시지 노출.

**상태별 동작**:
- loading: 저장 중 버튼 disabled.
- empty: otList 비어있으면 저장 버튼 disabled.
- error: 서버 에러 메시지 alert.
- success: emit('saved') + emit('close').

**골격 코드 변경 범위 (template + style)** — script body는 developer가 채움. 골격에는 다음만 추가:

```js
// === overtime request ===
const otForm = ref({ /* TODO developer fill */ });
const otAllowedWindows = computed(() => { /* TODO developer: derive from plan1*/std times */ return []; });
const canSaveOt = computed(() => { /* TODO developer fill */ return false; });
const fnApproveOvertime = async () => { /* TODO developer fill */ };
const fnRejectOvertime = async () => { /* TODO developer fill */ };
```

---

## 6. 우선순위 & 영향 범위

| ID | 우선순위 근거 | 선행 |
|----|--------------|------|
| B2-1 ~ B2-8 | TB_USER_ATTD_REQ 스키마 변경은 기존 모든 REQ 관련 코드가 컴파일/런타임 깨짐 → 최우선 | DB 마이그 SQL 적용 |
| B1 | 신규 기능 — B2 완료 후 OT_ID 시퀀스 등록(`FNC_CMM_SEQ_NEXTVAL` 키 `'OT_ID'`) 선행 필요 | B2, 시퀀스 등록 |
| F2 | B2 완료 시 응답 키가 바뀌므로 동시 배포 필요 | B2-3 |
| F1 | B1 완료 후 연동 | B1, F2 |

법적 책임 영역(attd) → 우선순위 +1단계 격상.

---

## 7. developer 인계 핵심 주의사항

1. **schema-full.sql에는 새 TB_USER_ATTD_REQ DDL/TB_USER_OVERTIME_MGMT DDL이 아직 반영되지 않았다.** 코드 변경 전에 DB 마이그 SQL을 먼저 적용하고 `schema-full.sql`을 갱신할 것. 갱신 없이 코드만 바꾸면 CLAUDE.md의 "스키마 ↔ 코드 불일치 즉시 보고" 룰 위반.
2. **REQ_STATUS 코드 마스터 row 등록 필요**: `FNC_CMM_INFO_SRCH(..., 'SYS033')`가 `'REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED'`를 인식하려면 `tb_baim_*` 코드 마스터에 row 추가 필요. baim 작업 별도 분리 또는 본 PR 동반 마이그 데이터 SQL 포함.
3. **`OT_ID` 시퀀스 키 등록**: `FNC_CMM_SEQ_NEXTVAL`이 사용하는 시퀀스 마스터 테이블에 `'OT_ID'` row 등록 필요.
4. **시간 검증의 자정 넘김 정규화**: workYmd 기준 0~1439분, 익일 1440~2879분 표기. CalendarSrch가 `YYYY-MM-DD`를 주므로 `inDate < outDate` 시 +1440 보정.
5. **정책서 27행 절대 준수** — TB_USER_ATTD_REQ 변경에 무관한 메서드(`selectMonthlyAttdList` 등)는 손대지 말 것.
6. **Java 파일 한글 금지** — 정책서, 명세서, Notion에는 한글 OK. 백엔드 `.java` 신규/수정 시 주석·로그·에러메시지 모두 영어로.
