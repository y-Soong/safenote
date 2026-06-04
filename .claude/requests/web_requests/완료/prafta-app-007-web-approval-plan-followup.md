# prafta-app-007-web-approval — §11 후속 보완: 카드 BEFORE/AFTER 스케줄 표시 + 처리 이력 노출

> **작업 ID prefix**: `PRAFTA-APP-007-WEB`(후속) — 본 문서 내부 식별자 `WEB-5`~`WEB-7`.
> **작업 영역**: 웹/백엔드 (`com.prafta.web.attd.attd07` + `prafta-web-frontend AttdDayDetailPop.vue`). 모바일 앱 변경 없음.
> **단일 출처(SSOT)**: 본 문서는 모(母) plan `prafta-app-007-web-approval-plan.md` 의 §11 후속 보완이다. 후속 developer/qa/security 는 모 plan + 본 문서 + 명시 정책서 섹션만 정독하면 된다.
> **메인 세션 위임**: planner(본 세션)는 Notion 접근 불가. Notion 등록은 메인 세션이 §6 항목으로 대행.
> **선행 완료 전제**: WEB-1(승인)·WEB-2(반려)·WEB-3(schCd 조회)·WEB-4(프론트 라우팅 분기)는 **이미 구현 완료(코드 검증)**. 본 후속은 그 위에서 표시(카드 BEFORE/AFTER)와 이력 노출만 보완한다.

---

## §11.0 진단 요약 (코드+DB+정책서 검증, 추측 아님)

### 공통 — 현 구현 상태 (모 plan 작업 WEB-1~4 는 모두 반영됨)
- 승인 `Attd07ServiceImpl#approveSchedModifyRequest`(L478~558): 7단 가드 후 `upsertUserWorkPlan` + `updateUserSchedModifyReqApprove`(REQ_STATUS='02', PROCESS_COMMENT 상수 `'SCHED_MODIFY_APPROVED'`, PROCESS_USER_CD, PROCESS_DATE=NOW()). 단일 `@Transactional`.
- 반려 `#rejectSchedModifyRequest`(L560~): 동일 가드 + `updateUserAttdReqReject`(REQ_STATUS='03', PROCESS_COMMENT=rejectReason, PROCESS_DATE=NOW()), `tb_user_work_plan` 미변경.
- `selectMonthlyAttdReq`(xml L939~996): SELECT 절 끝에 `A.SCH_CD AS schCd` 이미 추가됨. **WHERE `A.REQ_STATUS = '01'`** — 신청 상태만 카드로 노출(처리되면 카드 사라짐).
- `MonthlyAttdReqResult` record: 마지막 필드 `String schCd` 이미 존재.
- 프론트 `fnApproveReq`(L2362 분기)·`fnRejectReq`(L2479 분기)·`onRejectConfirm`(kind='schedModify'): `reqType==='10'` 라우팅 이미 존재.

### 이슈1 — 카드 BEFORE/AFTER 미표시 (원인 확정)
- 카드 데이터 빌드 `reqCards` computed(`AttdDayDetailPop.vue` L1494~1516): **모든** 요청을 출퇴근 시각 모델로 만든다.
  - `befIn = fmtTime(r["act"+n+"InTime"])`, `befOut = fmtTime(r["act"+n+"OutTime"])` — record(실제 출퇴근)에서.
  - `aftIn = fmtTime(req.startTime)`, `aftOut = fmtTime(req.endTime)` — REQ 의 START/END_TIME 에서.
- type='10'(스케줄 수정)은 REQ 의 `START_TIME/END_TIME` 이 **null**(앱 등록 시 SCH_CD 만 채우고 시각은 null — 모 plan §0-1). 따라서:
  - `aftIn/aftOut = "-"` (목표 스케줄 시각 미표시)
  - `befIn/befOut` = 그 날 실제 출퇴근 시각(스케줄 수정과 무관 — 의미 없음)
- 카드 템플릿(L209~245)은 BEFORE/AFTER 컬럼에 "출근/퇴근" 라벨 + befIn/befOut/aftIn/aftOut 하드코딩. → type='10' 에서는 무의미한 빈 카드.
- `req.schCd`(목표 스케줄 코드)는 카드까지 내려오지만 `reqCards` 가 활용하지 않는다.
- **현재 스케줄(WORK_PLAN_CD)·목표 스케줄(SCH_CD)의 시각 정보(FST/SEC_SCH_*)는 day-detail 응답 어디에도 없다.** `record`(=`selectDailyAttdDetails`)는 plan1Start/End·plan2Start/End(그 날 배정된 스케줄의 시각)를 가질 수 있으나(확인 필요 §7-A), 목표 SCH_CD 의 시각은 전혀 없다 → **백엔드가 내려줘야 함.**

### 이슈2 — 처리 이력 미기록 (원인 확정)
- 이력 병합 `Attd07ServiceImpl#getDailyAttdDetails`(L142~159): `selectDailyAttdDetailHistory`(TB_USER_ATTD_HIST: 근태/OT) + `selectDailyLeaveApprovalHistory`(TB_USER_ATTD_REQ_APPROVAL: 연차 05/06 결재 단계) 두 소스만 병합·insertDate 내림차순.
- 스케줄 수정(10)은 (a) HIST 미기록(모 plan D3), (b) 결재라인(TB_USER_ATTD_REQ_APPROVAL) 미사용(단순 매니저 모델 — REQ 행에 PROCESS_* 직접 기록) → **두 소스 어디에도 안 잡힘** → 처리 이력에 안 나옴.
- 단, **처리된 REQ 행 자체에 처리 흔적이 남아 있다**: `TB_USER_ATTD_REQ`(REQ_TYPE='10', REQ_STATUS IN '02'/'03', PROCESS_USER_CD, PROCESS_COMMENT, PROCESS_DATE NOT NULL). → 신규 테이블 불필요. REQ 행을 이력 소스로 추가하면 됨.

### 스키마 사실 (DB DDL 검증)
- `tb_sch_mgmt` (PK CMPNY_CD,SITE_CD,SCH_CD): `SCH_TYPE varchar(2) [SYS019]`, `FST_SCH_STR_TIME/FST_SCH_END_TIME varchar(4) NOT NULL`(HHmm), `FST_SCH_BRK_MIN varchar(3)`, `SEC_SCH_STR_TIME/SEC_SCH_END_TIME varchar(4) NULL`(2구간 없으면 NULL), `SEC_SCH_BRK_MIN varchar(3)`, `USE_YN`. **SCH_NM 없음 — schNo(varchar50)가 식별번호. 라벨은 시각으로 조립.**
- `tb_user_work_plan` (PK CMPNY_CD,SITE_CD,USER_CD,WORK_YMD): 단일 `WORK_PLAN_CD varchar(20)` COMMENT **"근무계획코드[SCH_CD, LEAVE_CD]"** → ⚠️ **WORK_PLAN_CD 는 SCH_CD 일 수도 LEAVE_CD(연차) 일 수도 있다.** "현재 스케줄" 시각 조회 시 SCH_CD 가 아니면(연차 코드면) tb_sch_mgmt 조인이 0건 → 시각 없음으로 처리.
- `tb_user_attd_req`: START_DATE/START_TIME/END_DATE/END_TIME varchar(8)/(4) NULL — type='10' 에선 null. PROCESS_USER_CD/PROCESS_COMMENT(varchar500)/PROCESS_DATE(datetime) 존재.
- 앱 `SchedOptionResult`(req07): `schCd, schNo, baseYn, fstStrTime, fstEndTime, secStrTime, secEndTime` — 원시 시각만 내리고 프론트가 라벨 조립. 앱 `SchedModifyForm.vue` 라벨 포맷: 1구간 `"09:00~18:00"`, 2구간 `"09:00~12:00 / 13:00~18:00"`. **본 작업도 동일 포맷 재사용.**

---

## §11.1 이슈1 설계 결정 (D10~D13)

### D10 — 백엔드 데이터 전달 방식: monthlyAttdReq 응답 확장(원시 시각 필드 추가)
- **결정**: `selectMonthlyAttdReq` 가 type='10' 카드용으로 **현재 스케줄 + 목표 스케줄의 원시 시각 필드**를 함께 내린다. `MonthlyAttdReqResult` 에 8개 필드 추가:
  - 목표 스케줄(SCH_CD): `tgtSchType, tgtFstStrTime, tgtFstEndTime, tgtSecStrTime, tgtSecEndTime`
  - 현재 스케줄(WORK_PLAN_CD): `curSchType, curFstStrTime, curFstEndTime, curSecStrTime, curSecEndTime`
  - (브레이크 노출은 D12 에서 제외 → BRK 필드는 내리지 않음)
- **방식**: `selectMonthlyAttdReq` 에 `tb_sch_mgmt` 2회 LEFT JOIN.
  - 목표: `LEFT JOIN tb_sch_mgmt TS ON TS.CMPNY_CD=A.CMPNY_CD AND TS.SITE_CD=A.SITE_CD AND TS.SCH_CD=A.SCH_CD`
  - 현재: `LEFT JOIN tb_user_work_plan WP ON WP.CMPNY_CD=A.CMPNY_CD AND WP.SITE_CD=A.SITE_CD AND WP.USER_CD=A.USER_CD AND WP.WORK_YMD=A.WORK_YMD` 후 `LEFT JOIN tb_sch_mgmt CS ON CS.CMPNY_CD=A.CMPNY_CD AND CS.SITE_CD=A.SITE_CD AND CS.SCH_CD=WP.WORK_PLAN_CD`
  - WORK_PLAN_CD 가 LEAVE_CD 면 CS 조인 0건 → cur* 전부 NULL(프론트 "없음" 처리, D11).
- **근거**: 모 plan §0-5 스키마 사실 + `attd/09 §9.6.3`("요청 내용 비교 Before/After") + `request-approval/06 §6.1`("변경 내용=시간·근무타입코드"). 기존 컨벤션(앱 SchedOptionResult)과 동형으로 원시 시각만 전달(라벨은 프론트). 카드 1건당 day 컨텍스트 단일 사용자·단일 일자라 JOIN 비용 무시 가능(N+1 아님 — 단일 select 내 join).
- **대안 기각**: ① record 확장(`selectDailyAttdDetails`)은 record 가 단일 행이라 카드(요청 N건)와 매핑 안 됨. ② 별도 조회 endpoint 는 왕복 추가·동기화 부담. → monthlyAttdReq 확장이 최소 변경·기존 카드 데이터 흐름 유지.
- **회귀 주의**: 추가 필드는 type='10' 외 요청(01~06)에서 NULL. record 위치 기반 매핑이므로 **SELECT 컬럼 순서 = record 필드 순서**를 반드시 일치(기존 schCd 가 SELECT 끝·record 끝인 패턴 유지 — 신규 8필드는 schCd 뒤에 같은 순서로).

### D11 — 프론트 카드 렌더링: type='10' 전용 분기 (출퇴근 카드 회귀 금지)
- **결정**: `reqCards` computed 와 카드 템플릿(L209~245)을 **type 별로 분기**한다. 최소 변경 원칙: 기존 출퇴근/OT 카드(01~04)·연차(05/06)는 현행 그대로, type='10' 만 새 표현.
- **reqCards 분기**: type='10' 이면 카드 객체에 `mode:'sched'` + `befSched`(현재 스케줄 라벨)·`aftSched`(목표 스케줄 라벨)·`schedChanged`(불리언) 를 세팅. 그 외는 기존 `mode:'time'` + befIn/befOut/aftIn/aftOut 유지.
- **라벨 조립(프론트 책임, D10 원시 시각 사용)**: 앱 `SchedModifyForm` 동일 포맷.
  - 1구간(secStr/secEnd 없음): `"00:00~07:00"`
  - 2구간(secStr/secEnd 있음): `"00:00~07:00 / 13:00~18:00"`
  - 시각은 `fmtTime`(기존 헬퍼) 재사용("0000"→"00:00"). 구간 수 표기는 `(1구간)`/`(2구간)` suffix 옵션 — D12 결정.
- **"없음" 처리**: 현재 스케줄 미배정(cur* 전부 NULL = WORK_PLAN_CD 없음 또는 연차 코드)이면 `befSched = "없음"`. 목표(tgt*)가 NULL 이면(데이터 부재 — 발생 시 비정상) `aftSched = "-"`.
- **변경 강조**: `schedChanged = (befSched !== aftSched)` 일 때 AFTER 에 기존 `is-changed` 클래스 적용(출퇴근 카드와 동일 시각적 일관성).
- **템플릿**: `req-diff` 블록을 `v-if="card.mode==='sched'"`(스케줄 1행: 라벨 그대로) / `v-else`(기존 출근·퇴근 2행) 로 분기. BEFORE/AFTER 헤더·화살표·is-changed 골격 재사용.
- **근거**: `common/13-ui-ux.md`(피드백·일관성), `attd/09 §9.6.3`, 기존 카드 골격. 신규 컴포넌트 없음(모 plan D9 일관 — 기존 팝업 내 분기).

### D12 — 구간 수·휴게시간 노출 정책
- **결정**: ① 구간 수는 **라벨에 `(1구간)`/`(2구간)` suffix 로 노출**(예: `"07:00~15:00 (1구간)"`). 사용자가 "2구간→1구간" 변경을 직관적으로 인지하게. ② **휴게시간(BRK_MIN)은 카드에 노출하지 않음**(1차). 카드는 변경 핵심(시간대·구간 수)만. 휴게 비교는 follow-up.
- **근거**: 모 plan D2 주의(2구간→1구간 교체가 의도된 동작)를 사용자에게 시각적으로 전달 = 구간 수 노출이 효과적. 휴게는 부차 정보 → 카드 과밀 방지 위해 제외. `attd/09 §9.6.3` 은 Before/After 비교만 요구(휴게 필수 아님).

### D13 — 라벨 포맷 단일 출처
- **결정**: 라벨 조립 함수 `schedLabel(schType, fstStr, fstEnd, secStr, secEnd)` 를 `AttdDayDetailPop.vue` 내 1개 신설(reqCards 에서만 사용). 앱과 코드 공유는 안 함(프로젝트 분리 — 동일 포맷을 각자 보유). `fmtTime` 재사용.
- **근거**: 메모리 `project_prafta_app_vite_and_api_align`(앱/웹 프론트 분리). 라벨 정책은 프론트 책임이며 화면마다 독립.

---

## §11.2 이슈2 설계 결정 (D14~D16)

### D14 — 처리 이력 소스: 처리된 type='10' REQ 행을 신규 쿼리로 병합
- **결정**: `selectDailySchedModifyHistory`(신규) 를 `selectDailyLeaveApprovalHistory` 패턴으로 작성하고 `getDailyAttdDetails` 병합에 추가(3번째 소스). `TB_USER_ATTD_REQ` 직접 조회(결재라인 없음 — 단순 매니저 모델, REQ 행에 PROCESS_* 기록).
- **WHERE**: `CMPNY_CD/SITE_CD/USER_CD/WORK_YMD` 일치 + `REQ_TYPE='10'` + `REQ_STATUS IN ('02','03')` + `PROCESS_DATE IS NOT NULL` + `DEL_YN='N'`. ORDER BY `PROCESS_DATE DESC`.
- **result 매핑**: `DailyAttdDetailHistoryResult` 재사용(연차 이력과 동일 모델 — 시각/구간 컬럼 전부 NULL). 매핑:
  - `histType` = `R.REQ_STATUS`(02/03)
  - `histTypeNm` = `CONCAT(FNC_CMM_INFO_SRCH(..., R.REQ_TYPE, 'SYS032'), ' ', FNC_CMM_INFO_SRCH(..., R.REQ_STATUS, 'SYS033'))` → 예: `"스케줄 수정 요청 승인"` / `"스케줄 수정 요청 반려"` (⚠️ 코드명 SYS032='10' / SYS033='02','03' 실제 라벨은 §7-B 확인 — 연차 이력은 SYS044 를 썼으나 스케줄 수정은 결재라인 상태가 아닌 REQ 상태이므로 **SYS033** 사용. 연차 패턴의 SYS044 와 다름에 주의).
  - `processReason` = D15 결정값(승인은 마커 숨김, 반려는 사유).
  - `insertNo` = `R.PROCESS_USER_CD`, `insertNm` = `FNC_CMM_INFO_SRCH(..., R.PROCESS_USER_CD, 'USER_NM' 형태)`(연차 이력 동일 패턴 확인 §7-C — `FNC_CMM_INFO_SRCH(CMPNY_CD,'USER_NM',USER_CD,null)`).
  - `insertDate` = `R.PROCESS_DATE`(연차 이력 AP.APPROVAL_DATE 와 동일하게 원시 datetime → 문자열 내림차순 정렬 일치). 단 PROCESS_DATE 는 `datetime` 타입이라 포맷 형태("YYYY-MM-DD HH:mm:ss") 가 기존 INSERT_DATE 와 동일한지 §7-D 확인(다르면 프론트 `fmtInsertDate` 가 14자리/12자리만 처리 — 정렬·표시 영향).
- **근거**: `attd/09 §9.2`(이력 보존 = 요청/승인/반려 + 사유), §9.6.1(처리 이력 타임라인), §9.6.3, 기존 `selectDailyLeaveApprovalHistory` 템플릿. 모 plan D3(HIST 테이블 미적합)는 유지 — REQ 행을 이력 소스로 직접 쓰므로 신규 테이블/HIST_TYPE 불필요.

### D15 — 이력 표시 내용: 승인 마커 숨김, 반려 사유 노출, before→after 미표시(1차)
> **[확정안 갱신 — 사용자 확정, 원안 ⓐ 덮어씀]** 처리 이력에도 "변경 전→후 스케줄"을 **표시**한다(무마이그). 승인 트랜잭션이 upsert 직전 현재 WORK_PLAN_CD 를 SELECT 캡처하여 기존 `PROCESS_COMMENT`(varchar500)에 구조화 마커 `SCHED_MODIFY_APPROVED:OLD=<oldCode>` 로 저장하고(상수 대체), 이력 쿼리(`selectDailySchedModifyHistory`)가 OLD 코드→tb_sch_mgmt(변경 전), R.SCH_CD→tb_sch_mgmt(변경 후) 로 원시 시각을 내려 프론트 schedLabel 로 라벨링한다. 반려는 무변경(bef/aft NULL + 사유). 마커 문자열은 사용자 비노출(승인 사유 NULL + 프론트 숨김). 아래 원안 ⓐ 본문은 히스토리 보존용.
- **결정**:
  - **유형명+상태명**: histTypeNm(D14) 로 "스케줄 수정 요청 승인/반려" 표기.
  - **사유(processReason)**: 승인은 PROCESS_COMMENT 가 상수 `'SCHED_MODIFY_APPROVED'`(비친화적) → **이력에 노출하지 않음**(NULL 또는 빈값으로 매핑하여 프론트 "사유 없음"/빈칸). 반려는 PROCESS_COMMENT=실제 rejectReason → 그대로 노출.
    - 구현: SQL 에서 `CASE WHEN R.REQ_STATUS='02' THEN NULL ELSE R.PROCESS_COMMENT END AS processReason`. (승인 마커가 화면에 새지 않도록 fail-safe.)
  - **처리자/처리일시**: insertNm(PROCESS_USER_CD→USER_NM)·insertDate(PROCESS_DATE).
  - **before→after 스케줄**: **1차 미표시(ⓐ 안 채택).** 근거: 승인 시점에 `tb_user_work_plan` 직전 WORK_PLAN_CD 는 upsert 로 덮어써져 사후 복원 불가(근태 이력이 `selectAttdSnapshotById` 로 승인 직전 스냅샷을 BEF 에 박는 것과 달리, 스케줄 수정 승인 트랜잭션은 직전 값을 캡처하지 않음 — 모 plan D3/현 구현 검증). before→after 를 남기려면 승인 트랜잭션에서 직전 WORK_PLAN_CD 를 캡처·직렬화(PROCESS_COMMENT) 하거나 별도 스냅샷 필요 → 본 후속 1차 범위 초과(FU-8). 이력에는 "스케줄 수정 승인/반려 + 처리자 + 일시 + (반려)사유"만. **요청 카드(이슈1)가 처리 전 before/after 를 이미 보여주므로**, 처리 후 타임라인은 "처리 사실"만 보여도 정책 §9.2(이력 보존: 요청/승인/반려+사유) 충족.
- **근거**: `attd/09 §9.2`(이력 보존 요건 = 요청/승인/반려+사유 — before/after 스케줄 값 보존은 명시 요건 아님), §9.6.3. 모 plan D3(HIST 부적합·요청 상태 이력으로 충족) 연장선.
- **대안(ⓑ before 캡처) 기각 사유**: 승인 트랜잭션 변경(직전 WORK_PLAN_CD SELECT→PROCESS_COMMENT 직렬화 또는 신규 스냅샷 컬럼/테이블) 필요 → 모 plan D8(마이그 불필요)·D3(HIST 미적합) 전제를 깬다. before/after 정보 수요가 확인되면 FU-8 로 별도 설계.

### D16 — 처리 이력 노출이 요청 카드 동작과 충돌하지 않음 (정합성)
- **결정**: 처리되면(02/03) `selectMonthlyAttdReq`(REQ_STATUS='01' 필터)에서 카드는 사라지고, 동시에 `selectDailySchedModifyHistory`(REQ_STATUS IN 02/03)에서 이력으로 등장 → 카드↔이력 상호 배타·일관. 같은 REQ 가 카드와 이력에 동시 노출되지 않음.
- **근거**: REQ_STATUS 전이 단방향(01→02/03). 두 쿼리의 status 필터가 상보적.

---

## §11.3 영향 범위

### 백엔드 (수정/신규)
```
com.prafta.web.attd.attd07
├── result/MonthlyAttdReqResult.java            (수정 — 현재/목표 스케줄 시각 8필드 추가, schCd 뒤)
└── service/impl/Attd07ServiceImpl.java          (수정 — getDailyAttdDetails 병합에 selectDailySchedModifyHistory 추가)

src/main/resources/com/prafta/web/attd/attd07/mapper/Attd07Mapper.xml
├── selectMonthlyAttdReq                          (수정 — tb_sch_mgmt 2회 + tb_user_work_plan LEFT JOIN, 시각 8컬럼 추가; SELECT 순서=record 순서)
├── selectDailySchedModifyHistory                 (신규 — 처리된 type='10' REQ → DailyAttdDetailHistoryResult)
└── Attd07Mapper.java                             (수정 — selectDailySchedModifyHistory 메서드 선언)
```
- DTO/Command 신규 없음. 신규 endpoint 없음. 마이그레이션 없음(D8 연장 — 기존 테이블/컬럼만).

### 프론트엔드 (수정만)
```
PRAFTA/prafta-web-frontend/prafta-web-frontend/src/views/attd/popup/AttdDayDetailPop.vue
├── reqCards computed (L1494~1516)                (수정 — type='10' 분기: mode/befSched/aftSched/schedChanged)
├── 카드 템플릿 req-diff 블록 (L209~245)          (수정 — mode==='sched' v-if 분기, 기존 time 블록 v-else)
└── schedLabel() 헬퍼                             (신규 — 1줄 라벨 조립, fmtTime 재사용)
```
- historyView(L1478~1490)·historyList 흐름은 **무변경** — 백엔드가 병합해 내려주면 기존 매핑이 그대로 표시(시각/구간 NULL → "-"·"-구간", histTypeNm/insertNm/insertDate/reason 표시). 단 §7-D PROCESS_DATE 포맷 확인 결과에 따라 `fmtInsertDate` 보완 가능성(아래 WEB-7 비고).

---

## §11.4 developer 단위 작업 분해

### PRAFTA-APP-007-WEB-5 — 요청 카드 BEFORE/AFTER 스케줄 데이터(백엔드 조회 확장)
- **유형**: backend / 보완
- **정책 근거**: `attd/09 §9.6.3`(요청 내용 Before/After), `request-approval/06 §6.1`(변경 내용=시간·근무타입코드). 스키마: tb_sch_mgmt(FST/SEC_SCH_*), tb_user_work_plan(WORK_PLAN_CD=SCH_CD|LEAVE_CD).
- **핵심 요구사항**:
  1) `selectMonthlyAttdReq`(xml)에 LEFT JOIN 추가(D10): 목표 `tb_sch_mgmt`(A.SCH_CD), 현재 `tb_user_work_plan`(A.USER_CD,A.WORK_YMD)→`tb_sch_mgmt`(WORK_PLAN_CD). SELECT 끝(기존 schCd 뒤)에 `tgtSchType,tgtFstStrTime,tgtFstEndTime,tgtSecStrTime,tgtSecEndTime,curSchType,curFstStrTime,curFstEndTime,curSecStrTime,curSecEndTime` 추가.
  2) `MonthlyAttdReqResult` record 에 동일 순서 8(=10)필드 추가(schCd 뒤). **SELECT 컬럼 순서=record 필드 순서**(위치 기반 매핑 — 회귀 핵심).
  3) WORK_PLAN_CD 가 LEAVE_CD(스케줄 아님)면 현재 스케줄 JOIN 0건 → cur* NULL(정상). 목표 SCH_CD JOIN 0건(삭제된 스케줄 등)도 tgt* NULL 허용(프론트 "-").
  4) type 01~06 요청은 신규 필드 전부 NULL — 기존 카드 회귀 없음 확인.
- **영향 파일**: Attd07Mapper.xml(selectMonthlyAttdReq) / MonthlyAttdReqResult.java.
- **비범위**: 휴게시간(BRK) 전달 안 함(D12). SCH_NM 라벨 없음(존재 안 함 — 시각 조립).

### PRAFTA-APP-007-WEB-6 — 처리 이력에 스케줄 수정(10) 처리 건 병합(백엔드)
- **유형**: backend / 보완
- **정책 근거**: `attd/09 §9.2`(이력 보존), §9.6.1(처리 이력 타임라인), §9.6.3. 기존 `selectDailyLeaveApprovalHistory` 템플릿.
- **핵심 요구사항**:
  1) `selectDailySchedModifyHistory`(신규 xml)·`Attd07Mapper.java` 선언 추가. result=`DailyAttdDetailHistoryResult`(재사용).
  2) WHERE: CMPNY/SITE/USER/WORK_YMD 일치 + `REQ_TYPE='10'` + `REQ_STATUS IN ('02','03')` + `PROCESS_DATE IS NOT NULL` + `DEL_YN='N'`. ORDER BY PROCESS_DATE DESC.
  3) 매핑(D14/D15): histType=REQ_STATUS, histTypeNm=`CONCAT(SYS032(REQ_TYPE), ' ', SYS033(REQ_STATUS))`, 시각/구간/befaft 전부 NULL, processReason=`CASE WHEN REQ_STATUS='02' THEN NULL ELSE PROCESS_COMMENT END`(승인 마커 숨김), insertNo=PROCESS_USER_CD, insertNm=`FNC_CMM_INFO_SRCH(CMPNY_CD,'USER_NM',PROCESS_USER_CD,null)`, insertDate=PROCESS_DATE.
  4) `getDailyAttdDetails`(Impl L142~159) 병합 로직에 3번째 소스로 addAll 후 동일 insertDate 내림차순 정렬에 포함. (기존 leaveApprovalHistory 병합 패턴 그대로 확장 — null/empty 가드 동일.)
- **영향 파일**: Attd07Mapper.xml(selectDailySchedModifyHistory 신규) / Attd07Mapper.java / Attd07ServiceImpl.java(getDailyAttdDetails).
- **재사용**: DailyAttdDetailHistoryResult, FNC_CMM_INFO_SRCH, 기존 병합/정렬.
- **비범위**: before→after 스케줄 값(D15 ⓐ — FU-8). HIST 테이블 기록(모 plan D3 유지).

### PRAFTA-APP-007-WEB-7 — 요청 카드 type='10' 렌더링 분기(프론트)
- **유형**: frontend-screen / 보완 (골격 신규 작성 불필요 — 기존 computed/템플릿 분기)
- **정책 근거**: `common/13-ui-ux.md`, `attd/09 §9.6.3`, 기존 카드 골격.
- **핵심 요구사항(명세 §11.5)**:
  1) `schedLabel()` 헬퍼 신설(D13): 원시 시각 → `"HH:MM~HH:MM"`(1구간) / `"HH:MM~HH:MM / HH:MM~HH:MM"`(2구간) + `(1구간)`/`(2구간)` suffix. fmtTime 재사용. 빈값/NULL 처리.
  2) `reqCards` 분기(D11): `req.reqType==='10'` → `mode:'sched'` + `befSched=schedLabel(cur*)` 또는 `"없음"`(cur 전부 NULL) + `aftSched=schedLabel(tgt*)` 또는 `"-"` + `schedChanged=(befSched!==aftSched)`. 그 외 → 기존 `mode:'time'` 그대로.
  3) 카드 템플릿(L209~245) req-diff: `v-if="card.mode==='sched'"` 블록(BEFORE 라벨 1행 / → / AFTER 라벨 1행, schedChanged 시 is-changed) / `v-else` 기존 출근·퇴근 2행 블록.
  4) 처리 이력은 백엔드(WEB-6)가 병합해 내려주므로 historyView 무변경. 단 §7-D 결과 PROCESS_DATE 포맷이 14/12자리 아니면 `fmtInsertDate` 보완(원시 datetime "YYYY-MM-DD HH:mm:ss" 케이스 추가) 검토.
  5) 하드코딩 색상/픽셀 금지(CSS 변수). 신규 스타일은 기존 req-diff 클래스 재사용 우선.
- **영향 파일**: AttdDayDetailPop.vue.
- **선행**: WEB-5(시각 필드 응답). WEB-6 은 독립(이력은 historyList 자동 표시).

### 권장 착수 순서
WEB-5(응답 확장) → WEB-7(프론트 카드, WEB-5 의존) ‖ WEB-6(이력 병합, 독립). WEB-6 과 WEB-7 은 병렬 가능.

---

## §11.5 프론트 분기 명세 (AttdDayDetailPop.vue)

### schedLabel 헬퍼(신규, fmtTime 인근)
```
// 스케줄 원시 시각 → 표시 라벨. 1구간/2구간 자동 판별 + 구간 수 suffix.
// 입력은 tb_sch_mgmt 의 HHmm 4자리 문자열. 2구간 없으면 secStr/secEnd 가 빈값/NULL.
const schedLabel = (fstStr, fstEnd, secStr, secEnd) => {
  if (!fstStr && !fstEnd) return "-";          // 목표 데이터 부재
  const fst = `${fmtTime(fstStr)}~${fmtTime(fstEnd)}`;
  if (secStr && secEnd) {
    return `${fst} / ${fmtTime(secStr)}~${fmtTime(secEnd)} (2구간)`;
  }
  return `${fst} (1구간)`;
};
```
- schType 인자는 표시에 직접 안 쓰고 secStr/secEnd 유무로 구간 수 판정(앱 SchedModifyForm 동일 — schType 신뢰보다 실제 시각 유무가 안전). schType 은 응답에 두되 라벨엔 미사용(향후 코드명 노출 시 활용 — 선택).

### reqCards computed 분기(L1494~1516 교체)
```
const reqCards = computed(() => {
  const r = record.value ?? {};
  return (reqList.value || []).map((req) => {
    const n = parseInt(req.workSeq, 10) || 1;
    const base = {
      raw: req, reqId: req.reqId, reqType: req.reqType,
      reqTypeNm: req.reqTypeNm || "-", reqStatus: req.reqStatus,
      reqStatusNm: req.reqStatusNm || "", insertDate: fmtInsertDate(req.insertDate),
      workSeq: n, reqReason: req.reqReason || "", approvalStep: req.approvalStep ?? null,
    };
    if (req.reqType === "10") {
      const hasCur = !!(req.curFstStrTime || req.curFstEndTime);
      const befSched = hasCur
        ? schedLabel(req.curFstStrTime, req.curFstEndTime, req.curSecStrTime, req.curSecEndTime)
        : "없음";
      const aftSched = schedLabel(req.tgtFstStrTime, req.tgtFstEndTime, req.tgtSecStrTime, req.tgtSecEndTime);
      return { ...base, mode: "sched", befSched, aftSched, schedChanged: befSched !== aftSched };
    }
    return {
      ...base, mode: "time",
      befIn: fmtTime(r[`act${n}InTime`]) || "-",
      befOut: fmtTime(r[`act${n}OutTime`]) || "-",
      aftIn: fmtTime(req.startTime) || "-",
      aftOut: fmtTime(req.endTime) || "-",
    };
  });
});
```

### 카드 템플릿 req-diff 분기(L209~245)
```
<div class="req-diff">
  <!-- 스케줄 수정(10): 근무시간(스케줄) 기준 Before/After -->
  <template v-if="card.mode === 'sched'">
    <div class="req-diff-col">
      <div class="req-diff-head">BEFORE</div>
      <div class="req-diff-row">
        <span class="req-diff-lbl">스케줄</span>
        <span class="req-diff-val">{{ card.befSched }}</span>
      </div>
    </div>
    <div class="req-diff-arrow">→</div>
    <div class="req-diff-col">
      <div class="req-diff-head">AFTER</div>
      <div class="req-diff-row">
        <span class="req-diff-lbl">스케줄</span>
        <span class="req-diff-val" :class="{ 'is-changed': card.schedChanged }">{{ card.aftSched }}</span>
      </div>
    </div>
  </template>
  <!-- 그 외(01~06): 기존 출퇴근 시각 Before/After (현행 유지) -->
  <template v-else>
    ... (기존 출근/퇴근 2행 블록 그대로) ...
  </template>
</div>
```
- 스타일: 기존 `req-diff-*` 클래스 재사용. 신규 색/픽셀 없음. 라벨 한 줄이 길면(2구간) `req-diff-val` 의 줄바꿈/말줄임은 기존 CSS 변수 기반(필요 시 word-break만, !important 금지).

---

## §11.6 메인 세션이 Notion 에 기록할 항목 (planner 직접 접근 불가)

"작업 로그" DB 에 3행 추가(작업ID 는 메인 세션 통합 채번):

| 내부 ID | 영역 | 모듈 | 작업유형 | 요구사항 요약 |
|---|---|---|---|---|
| APP-007-WEB-5 | web | attd/attd07 | 보완 | selectMonthlyAttdReq + MonthlyAttdReqResult 에 현재/목표 스케줄 시각 10필드 추가(tb_sch_mgmt 2회+tb_user_work_plan LEFT JOIN). type='10' 카드 BEFORE/AFTER 데이터. [정책: attd/09 §9.6.3] |
| APP-007-WEB-6 | web | attd/attd07 | 보완 | selectDailySchedModifyHistory 신규 + getDailyAttdDetails 병합 추가(처리된 type='10' REQ → 처리 이력). 승인 마커 숨김·반려 사유 노출·before/after 미표시. [정책: attd/09 §9.2/§9.6.1] |
| APP-007-WEB-7 | web | attd/attd07 | 보완 | AttdDayDetailPop reqCards/카드 템플릿 type='10' 분기(schedLabel·mode='sched'·befSched/aftSched/schedChanged). [정책: attd/09 §9.6.3] |

- 선행: WEB-7 ← WEB-5. WEB-6 독립.
- 우선순위: attd(법적 책임) +1 격상. 데이터 정합성 영향 낮음(읽기/표시 보완) → 기능 버그 수정 등급.
- 모 plan 의 §3 작업(WEB-1~4)은 이미 구현 완료 — 별도 등록 불필요(이미 등록되었으면 상태만 갱신).

---

## §11.7 developer 확인 필요 (착수 전 MCP/코드 재확인 항목)

> 본 항목들은 planner 가 스냅샷 DDL·코드로 확정했으나, 운영 코드명/포맷은 developer 가 MCP(prafta-mysql)·코드로 재확인 후 구현. 추측 금지.

- **§7-A**: `selectDailyAttdDetails`(record)가 plan1Start/End·plan2Start/End(그 날 배정 스케줄 시각)를 내리는가. (WEB-5 는 record 와 무관하게 monthlyAttdReq JOIN 으로 현재 스케줄을 독립 조회하므로 불필요하지만, 중복 데이터 소스 정합 위해 확인 권장.)
- **§7-B**: SYS032 의 '10' 라벨 실제 문자열(예 "스케줄 수정 요청"), SYS033 의 '02'/'03' 라벨(예 "승인"/"반려"). histTypeNm CONCAT 결과 확인. ⚠️ 연차 이력은 SYS044(결재상태)를 썼으나 스케줄 수정은 **SYS033(요청상태)** 사용 — 코드그룹 혼동 금지.
- **§7-C**: `FNC_CMM_INFO_SRCH(CMPNY_CD,'USER_NM',USER_CD,null)` 가 PROCESS_USER_CD→사용자명 해석에 동일 적용 가능한지(연차 이력 selectDailyLeaveApprovalHistory L1025 와 동형).
- **§7-D**: `TB_USER_ATTD_REQ.PROCESS_DATE`(datetime) 를 result 로 내릴 때 문자열 포맷. 연차 이력 AP.APPROVAL_DATE 와 동일 포맷("YYYY-MM-DD HH:mm:ss")인지. 다르면 (a) 정렬(merged.sort 문자열 비교)이 INSERT_DATE 와 정합한지, (b) 프론트 `fmtInsertDate`(14/12자리 정규식만 처리 — datetime 문자열은 그대로 passthrough)가 올바르게 표시하는지. **연차 이력이 이미 AP.APPROVAL_DATE(datetime) 를 같은 방식으로 내려 정상 표시 중이므로 동일하면 무변경**(연차 이력으로 검증된 경로 재사용).
- **§7-E**: 테스트 데이터(USER_CD='20260400013', WORK_YMD='20260602') 현재 WORK_PLAN_CD 값(모 plan 진단상 '00005'=2구간 00:00~07:00+13:00~18:00). 목표 SCH_CD='00003'(1구간 07:00~15:00). MCP 로 tb_sch_mgmt 의 해당 SCH_CD 시각 실측 후 카드 라벨 기대값 확정.

---

## §11.8 qa 체크포인트 (이슈1/이슈2)

1. **카드 BEFORE/AFTER(type=10)**: 미처리(01) 스케줄 수정 카드에 BEFORE=현재 스케줄 라벨(또는 "없음"), AFTER=목표 스케줄 라벨, 구간 수 suffix 표시. 2구간→1구간 케이스(테스트 데이터)에서 BEFORE="00:00~07:00 / 13:00~18:00 (2구간)", AFTER="07:00~15:00 (1구간)", AFTER is-changed 강조.
2. **현재 스케줄 없음**: WORK_PLAN_CD NULL 또는 LEAVE_CD → BEFORE="없음".
3. **회귀(다른 type 카드)**: 01/02(근태)·03/04(OT)·05/06(연차) 카드는 기존 출퇴근 시각 Before/After 그대로(mode='time'). 신규 필드 NULL 이 기존 렌더링 깨지 않음.
4. **처리 이력(type=10)**: 승인 후 처리 이력에 "스케줄 수정 요청 승인" + 처리자 + 처리일시 표시, 사유 빈칸(마커 'SCHED_MODIFY_APPROVED' 노출 안 됨). 반려 후 "스케줄 수정 요청 반려" + 반려 사유 표시.
5. **카드↔이력 배타(D16)**: 처리 전 = 카드만, 처리 후 = 이력만(카드 사라짐). 동일 REQ 가 양쪽 동시 노출 안 됨.
6. **이력 정렬**: 근태/OT/연차/스케줄수정 이력이 처리일시 내림차순 단일 타임라인에 혼재 정렬(최신 위).
7. **before/after 미표시 확인(D15)**: 이력 타임라인의 스케줄 수정 행에 변경 전/후 스케줄 값은 표시 안 됨(시각/구간 NULL → 기존 매핑 "-"). 의도된 1차 동작.
8. **표시 인코딩**: 한국어 라벨(구간/승인/반려/없음) 정상.

## §11.9 security 체크포인트

1. **승인 마커 누수 차단(D15)**: PROCESS_COMMENT='SCHED_MODIFY_APPROVED'(내부 마커)가 이력 화면에 노출되지 않음(CASE 로 NULL 치환). 반려 사유는 사용자 입력이므로 그대로 노출(기존 연차/근태 반려와 동일 — XSS 는 Vue 텍스트 바인딩으로 escape).
2. **IDOR/scope(읽기)**: `getDailyAttdDetails`·`selectMonthlyAttdReq`·신규 `selectDailySchedModifyHistory` 모두 기존 진입 가드(canManageNode + selectUserExistInCmpnySite + CMPNY/SITE/USER/WORK_YMD WHERE) 안에서 동작 — 신규 JOIN/쿼리가 회사·사이트 스코프를 벗어나지 않는지(tb_sch_mgmt 조인도 CMPNY_CD/SITE_CD 일치 조건 필수).
3. **PII**: 처리자명(insertNm=USER_NM)은 기존 연차/근태 이력도 동일 노출 — 관리자 화면 컨텍스트 내 허용. 신규 노출 PII 없음.
4. **JOIN 정합**: tb_user_work_plan→tb_sch_mgmt 조인 시 CMPNY_CD/SITE_CD 누락 없이(타 사업장 스케줄 시각 유출 방지) 4키 또는 PK 키 전부 매칭.
5. **법적 책임 영역(attd) +1 격상**.

---

## §11.10 Follow-up 후보 (모 plan §7 에 추가)

| # | 항목 | 사유 |
|---|---|---|
| FU-8 | 스케줄 수정 이력에 before→after 스케줄 값 노출 | D15 ⓐ — 승인 트랜잭션에서 직전 WORK_PLAN_CD 캡처(스냅샷/PROCESS_COMMENT 직렬화) 필요. 모 plan D8(마이그 불필요) 깸. before/after 수요 확인 시 별도 설계. |
| FU-9 | 카드/이력에 스케줄 휴게시간(BRK_MIN) 비교 노출 | D12 — 1차 카드 과밀 방지로 제외. |
| FU-10 | 스케줄 코드명(SCH_NO/사용자 친화 명칭) 노출 | tb_sch_mgmt 에 SCH_NM 없음 — schNo(식별번호) 또는 별도 명칭 컬럼 신설 시. |
```
