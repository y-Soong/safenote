# prafta-app-018-A — 작업 분해 (앱 BE 연차 신청 폼 메타 조회 API)

분해자: planner. 상위: `prafta-app-018-leave-apply-plan.md` / 단위 요청서: `prafta-app-018-A-leave-meta.md`.
본 단위는 **읽기 전용 백엔드**(신규 테이블 없음). 쓰기는 018-B, FE 폼은 018-C 선후행.

## 정책 출처 (정독 결과)
- attd §8(연차) / §8.5(연차부여정책·사용단위) / §9(결재) — `.claude/context/policies/attd/08-leave.md`
- prafta-024(USAGE_UNIT 단일화·`ALLOW_*`/`MAX_DAILY_REQUEST` 폐기) — `src/main/resources/sql/migration/prafta-024-leave-usage-unit.sql`
- prafta-019(시간차/결재라인·LeaveFlow), prafta-020(결재선 프리셋 `tb_aprv_line_preset`)
- 상위 확정: **D2-a (Y) 계층형** — 설정 단위는 "허용 최소 단위", granularity 이하(더 큰 단위 전부) 허용.

## 스키마 확인 결과 (실측 — 추측 아님)
- ⚠️ `schema-full.sql` 스냅샷이 **stale**: `tb_leave_usage_policy` 에 아직 `ALLOW_*`/`MAX_DAILY_REQUEST` 가 보이나, prafta-024 마이그가 `USAGE_UNIT varchar(20) NOT NULL DEFAULT 'FULL_DAY'`(값 `FULL_DAY/HALF_DAY/HOUR_2/HOUR_1/MIN_30`) 를 신설하고 `MAX_DAILY_REQUEST` 를 DROP 했다. **본 작업은 `USAGE_UNIT` 을 SSOT 로 사용**(코드/매퍼에서 확인됨: `LeavePolicyMapper.xml`, `LeavePolicyServiceImpl.java`).
- `tb_leave_type_mgmt`: PK(`CMPNY_CD`,`LEAVE_CD`). 컬럼 `LEAVE_NM`, `LEAVE_TYPE`(SYS021), `SYSTEM_YN`(법정시드 Y), `USE_YN`, `USE_UNIT_TYPE`(varchar(2), SYS025), `APRV_USE_YN`(char(1)).
- `tb_leave_policy`: 회사 활성 1건(`USE_YN='Y'`), `APRV_USE_YN`(법정 결재여부), `POLICY_SEQ`.
- `tb_leave_usage_policy`: `POLICY_SEQ`(=정책 1:1), `USAGE_UNIT`(회사 단일 허용 단위).
- `tb_user_leave_grant`: `GRANT_DAYS`/`USED_DAYS` decimal(5,1), `LEAVE_CD`, `STATUS`(ACTIVE..), `EXPIRE_YN`, `DEL_YN`, `AVAIL_FROM_DATE`/`AVAIL_TO_DATE`(YYYYMMDD), `GRANT_TYPE`(STATUTORY_*/MANUAL_*).
- `tb_aprv_line_preset`(PK CMPNY_CD,PRESET_ID; USER_CD/PRESET_NM/DEFAULT_YN/USE_YN) + `tb_aprv_line_preset_d`(STEP_NO/APPROVER_USER_CD).
- SYS025 코드: `00`=1일(FULL_DAY) / `01`=반차(HALF_DAY) / `02`=2시간(HOUR_2) / `03`=1시간(HOUR_1) / `04`=30분(MIN_30).

## 핵심 재사용 발견 (중복 신설 금지)
앱 `com.prafta.app.mypage.mypage01`(prafta-app-010) 이 **이미 결재선 프리셋 + 결재자 후보 검색을 앱 전용으로 구현**해 두었다.
- `AppMypage01Mapper.selectPresetMasters` / `selectPresetStepsByUser` — 본인 프리셋 마스터+스텝.
- `AppMypage01Mapper.selectApprovalCandidates` — 동일 회사/사업장 활성 사용자(본인·system 제외), `USER_NM LIKE`, 직급(COM007) SORT_IDX 정렬.
- result record: `PresetMasterResult(presetId,presetNm,defaultYn)`, `PresetStepResult(presetId,stepNo,approverUserCd,userNm,userId,rankNm,nodeNm)`, `ApprovalCandidateResult(userCd,userId,userNm,rankCd,rankNm,rankSortIdx,nodeNm)`.

→ **결정**: 018-A 엔드포인트 2·3(프리셋/결재자검색)은 신규 SQL 을 짜지 말고 **mypage01 매퍼/서비스의 기존 메서드를 호출(위임)** 한다. leaveflow 신규 매퍼는 엔드포인트 1(apply-meta)에만 필요하다. (단 LIMIT/페이징 보강은 아래 작업으로 명시.)

---

# 작업 분해 결과

## prafta-app-018-A-01
- **유형**: backend
- **영역**: app
- **모듈**: leave/leaveflow (신규 `com.prafta.app.leave.leaveflow`)
- **작업 유형**: 신규
- **요구사항 요약**: `GET /appApi/leaveflow/apply-meta` — 신청 가능 연차종류 목록(systemYn·aprvRequired·allowedUnits·balanceDays) 단일 조회. 식별값 JWT only.
- **상세 설명**:
  - 핵심 요구사항:
    1) 회사 활성 연차종류(`tb_leave_type_mgmt USE_YN='Y'`)를 종류별로 내려준다.
    2) 종류별 `allowedUnits`(SYS025 코드 목록)를 D2-a (Y) 계층형으로 산출(법정/비법정 출처 분기, 아래 §allowedUnits 알고리즘).
    3) 종류별 `balanceDays`(현재 잔여) = leave01/웹과 동일 공식(SUM(GRANT_DAYS)-SUM(USED_DAYS), 활성집합).
    4) `aprvRequired`(결재 필요): 법정(systemYn='Y')=`tb_leave_policy.APRV_USE_YN`, 비법정=`tb_leave_type_mgmt.APRV_USE_YN`.
    5) 신청 불가(잔여 0 등) 종류 처리 방침: **목록 유지 + `applicable=false` 플래그**(disable, §applicable 규칙). FE 가 비활성 표시.
  - 영향 받는 파일 (신규):
    - controller `app/leave/leaveflow/controller/AppLeaveFlowController.java` (`@RequestMapping("/leaveflow")`)
    - service `app/leave/leaveflow/service/AppLeaveFlowService.java` + `impl/AppLeaveFlowServiceImpl.java`
    - param `app/leave/leaveflow/application/param/LeaveApplyMetaParam.java` (`from(TokenInfo)` — leave01 패턴)
    - mapper `app/leave/leaveflow/mapper/AppLeaveFlowMapper.java` + `resources/.../AppLeaveFlowMapper.xml`
    - result `app/leave/leaveflow/result/LeaveTypeMetaRow.java`, `LeaveUsagePolicyRow.java`
    - DTO `app/leave/leaveflow/dto/response/LeaveApplyMetaResponse.java` (+ 중첩 `LeaveTypeItem`)
  - 영향 받는 endpoint: `GET /prafta/appApi/leaveflow/apply-meta` (자동 prefix `com.prafta.app.*`)
  - 예상 산출물: controller/service/param/mapper/xml/result/response DTO
  - 연결 UI 명세: 없음(읽기전용 BE, Vue 골격 없음 — 폼 화면은 018-C)
- **선행 작업**: 없음 (prafta-app-010 mypage01 결재선 인프라는 이미 운영 — 02/03 에서 재사용)
- **우선순위 근거**: 법적 책임 영역(attd 연차) +1단계; 018-B/C 의 선행 데이터 공급원.

## prafta-app-018-A-02
- **유형**: backend
- **영역**: app
- **모듈**: leave/leaveflow
- **작업 유형**: 신규(위임)
- **요구사항 요약**: `GET /appApi/leaveflow/approval-presets` — 본인 소유 결재선 프리셋 목록(마스터+스텝, 기본 플래그). mypage01 기존 매퍼 재사용.
- **상세 설명**:
  - 핵심 요구사항:
    1) 본인(`gv_userCd`)·회사(`gv_cmpnyCd`) 소유 `USE_YN='Y'` 프리셋 마스터 목록(`defaultYn` 포함, 기본 우선 정렬).
    2) 각 프리셋의 스텝(STEP_NO 순) + 결재자 표시정보(userNm/rankNm/nodeNm). PII 최소노출(USER_ID/이름/직급/부서만, 휴대폰·이메일 등 미포함).
    3) 신규 SQL 금지 — `AppMypage01Mapper.selectPresetMasters` + `selectPresetStepsByUser` 호출(서비스 레이어 위임 또는 매퍼 직접 주입).
  - 영향 받는 파일:
    - controller에 `@GetMapping("/approval-presets")` 추가(018-A-01 컨트롤러 공용)
    - service 에 `selectApprovalPresets(param)` 추가 → mypage01 매퍼/서비스 위임
    - DTO `dto/response/ApprovalPresetListResponse.java`(마스터 list + 스텝 그룹핑) — 또는 mypage01 응답 DTO 재사용 검토
  - 영향 받는 endpoint: `GET /prafta/appApi/leaveflow/approval-presets`
  - 예상 산출물: controller 메서드/service 메서드/응답 DTO (매퍼 재사용)
- **선행 작업**: 없음
- **우선순위 근거**: 018-C 결재선 선택 UI 데이터 공급(연차=결재 D1).

## prafta-app-018-A-03
- **유형**: backend
- **영역**: app
- **모듈**: leave/leaveflow
- **작업 유형**: 신규(위임)
- **요구사항 요약**: `GET /appApi/leaveflow/approver-search?keyword=&page=&size=` — 결재자 후보 검색(동일 회사/사업장, PII 최소노출, LIMIT/페이징 강제).
- **상세 설명**:
  - 핵심 요구사항:
    1) `keyword`(이름 부분일치, 선택) 로 결재자 후보 검색. 본인·system·비활성 제외(`USE_YN='Y'`,`ACCOUNT_STATUS='01'`,`WITHDRAWAL_DATE IS NULL`).
    2) 사업장 스코프는 JWT `gv_siteCd` 강제(클라 입력 금지, IDOR). 자기 자신(`gv_userCd`) 제외.
    3) 응답 PII 최소노출: `userCd/userId/userNm/rankNm/nodeNm` 만(휴대폰/이메일/생년 금지). mypage01 `ApprovalCandidateResult` 형태 그대로.
    4) **LIMIT 강제**: `mypage01.selectApprovalCandidates` 에는 현재 LIMIT 가 없으므로 본 단위에서 **페이징(LIMIT/OFFSET) 추가**가 필요 → 신규 매퍼 메서드 `searchApprovers(keyword,siteCd,cmpnyCd,excludeUserCd,limit,offset)` 또는 mypage01 매퍼에 LIMIT 파라미터 추가. (planner 권고: leaveflow 전용 검색 SQL 신설하여 LIMIT/keyword 단순화 — mypage01 원본 무변경.)
  - 영향 받는 파일:
    - controller에 `@GetMapping("/approver-search")` 추가
    - service `searchApprovers(param)`
    - mapper `AppLeaveFlowMapper.searchApprovers` + xml (LIMIT/OFFSET 포함; selectApprovalCandidates SQL 차용)
    - param `LeaveApproverSearchParam.from(TokenInfo, keyword, page, size)` (size 상한 캡 — 예: 기본 20, 최대 50)
    - DTO `dto/response/ApproverSearchResponse.java`(list + 페이징 메타 hasNext)
  - 영향 받는 endpoint: `GET /prafta/appApi/leaveflow/approver-search`
  - 예상 산출물: controller/service/param/mapper/xml/response DTO
- **선행 작업**: 없음
- **우선순위 근거**: 018-C 결재선 직접 추가 UI 데이터 공급; PII/검색이라 security 검토 대상(LIMIT·평문노출).

---

# 엔드포인트 3종 확정

| # | 메서드/경로(자동 prefix `/prafta/appApi`) | 식별값 | 페이징 | 비고 |
|---|---|---|---|---|
| 1 | `GET /leaveflow/apply-meta` | JWT(cmpny/user) | X | 종류 목록+allowedUnits+balanceDays |
| 2 | `GET /leaveflow/approval-presets` | JWT(cmpny/user) | X | 본인 프리셋(mypage01 재사용) |
| 3 | `GET /leaveflow/approver-search?keyword=&page=&size=` | JWT(cmpny/site/user) | O(LIMIT/OFFSET) | 결재자 검색, PII 최소 |

모듈명: 신규 `com.prafta.app.leave.leaveflow`(컨트롤러 `@RequestMapping("/leaveflow")`). 018-B 의 `POST /leaveflow/apply` 와 동일 모듈을 공유하도록 leaveflow 로 명명(leave02 대신 leaveflow 채택 — 웹 미러 명칭 일치).

---

# 응답 DTO record 필드 계약 (확정 키명 — FE/018-C 그대로 소비)

> ⚠️ **MyBatis record 위치매핑 함정**: result record(`LeaveTypeMetaRow` 등)는 생성자 인자 **순서 = SELECT 컬럼 순서**(위치 기반)다. SELECT 컬럼 추가/재배치 시 record 인자 순서도 동일하게 맞춘다. 응답 DTO(`@Builder`/명시 생성)는 서비스에서 조립하므로 위치 무관하나, mapper 가 직접 채우는 record 는 반드시 순서 일치.

## 1) apply-meta — `LeaveApplyMetaResponse`
```
record LeaveApplyMetaResponse(
      List<LeaveTypeItem> leaveTypes   // 신청 가능 연차종류
)

record LeaveTypeItem(
      String  leaveCd        // 연차코드 (tb_leave_type_mgmt.LEAVE_CD)
    , String  leaveNm        // 연차명
    , String  systemYn       // 법정여부 'Y'/'N'
    , boolean aprvRequired   // 결재필요 (법정=policy.APRV_USE_YN / 비법정=type.APRV_USE_YN, 'Y'→true)
    , List<String> allowedUnits  // 허용 사용단위 SYS025 코드 목록 (계층, 굵은단위→잘은단위 순: 00,01,02,03,04 중 부분집합)
    , double  balanceDays    // 현재 잔여(부여-사용 합, 활성집합, 소수1자리)
    , boolean applicable     // 신청가능(잔여>0). false 면 FE disabled
)
```
- `aprvRequired`/`applicable` 은 **Lombok+Jackson is- 접두 함정 주의** — record 라 getter 가 `aprvRequired()`/`applicable()` 이고 Jackson 직렬화 키는 `aprvRequired`/`applicable` 로 안전(record 는 boolean is- 탈락 이슈 없음). 단 응답을 Lombok `@Builder` POJO 로 바꾸면 `boolean applicable` → JSON `applicable` 유지 위해 `@JsonProperty("applicable")` 권장. **record 사용 권장**.

## 2) approval-presets — `ApprovalPresetListResponse`
```
record ApprovalPresetListResponse(
      List<PresetItem> presets
)

record PresetItem(
      String presetId
    , String presetNm
    , boolean defaultYn     // 기본 프리셋 여부 (DEFAULT_YN='Y'→true)
    , List<PresetStepItem> steps
)

record PresetStepItem(
      int    stepNo
    , String approverUserCd
    , String userNm
    , String userId
    , String rankNm
    , String nodeNm
)
```
- 매퍼는 mypage01 `PresetMasterResult`/`PresetStepResult`(record) 를 그대로 받고, 서비스에서 presetId 기준 그룹핑하여 위 DTO 조립(매퍼 직접 매핑 아님 → 위치매핑 함정 비해당).
- `defaultYn` 은 DB char(1) → 서비스에서 `"Y".equals()` 로 boolean 변환.

## 3) approver-search — `ApproverSearchResponse`
```
record ApproverSearchResponse(
      List<ApproverItem> approvers
    , boolean hasNext       // size+1 조회로 다음페이지 존재 판정
)

record ApproverItem(
      String userCd
    , String userId
    , String userNm
    , String rankNm
    , String nodeNm
)
```
- **PII 정책**: `rankCd`/`rankSortIdx` 등 정렬 보조값은 응답에서 제외(내부 정렬용). 휴대폰/이메일/생년 절대 미포함. `userId`(로그인ID) 노출은 결재자 식별 최소 필요범위로 허용(mypage01 동일).

---

# §allowedUnits 산출 규칙 (D2-a (Y) 계층형)

## 출처 분기
- **법정(systemYn='Y')**: `tb_leave_usage_policy.USAGE_UNIT`(회사 단일값) — 회사 활성 정책(`tb_leave_policy USE_YN='Y'`)의 `POLICY_SEQ` 1:1. 모든 법정 종류가 동일 단위.
- **비법정(systemYn='N')**: `tb_leave_type_mgmt.USE_UNIT_TYPE`(타입 단일 SYS025 코드). NULL 이면 기본 `00`(FULL_DAY) 폴백.

## USAGE_UNIT(문자열) ↔ SYS025(코드) 매핑표
| SYS025 코드 | USAGE_UNIT 문자열 | 의미 | granularity 순위(0=굵음) |
|---|---|---|---|
| `00` | `FULL_DAY` | 1일(종일) | 0 |
| `01` | `HALF_DAY` | 반차(0.5일) | 1 |
| `02` | `HOUR_2`   | 2시간 | 2 |
| `03` | `HOUR_1`   | 1시간 | 3 |
| `04` | `MIN_30`   | 30분 | 4 |

## 변환 알고리즘 (단일값 → 코드목록, 계층형)
```
입력: settingCode (SYS025 두자리, 비법정은 USE_UNIT_TYPE 그대로 / 법정은 USAGE_UNIT 문자열→코드 매핑)
표준순서 UNIT_ORDER = ["00","01","02","03","04"]   // 굵→잘게
idx = UNIT_ORDER.indexOf(settingCode)
if idx < 0:  idx = 0   // 알 수 없는 값은 FULL_DAY 만 허용(안전 폴백)
allowedUnits = UNIT_ORDER[0 .. idx]   // 설정 단위 + 그보다 굵은 단위 전부 (D2-a: 설정=허용 최소 단위)
```
- 예 설정=`HOUR_1`(03) → `["00","01","02","03"]`(종일·반차·2시간·1시간), `04`(30분) 제외. ✔ 상위 문서 예시 일치.
- 예 설정=`FULL_DAY`(00) → `["00"]`(종일만).
- 예 설정=`MIN_30`(04) → `["00","01","02","03","04"]`(전부).
- 매핑 상수는 서비스에 enum/Map 으로 1벌만 둔다(법정 문자열→코드 + 순서). 018-B 단위 게이팅 검증과 **동일 상수 공유**(SSOT)하도록 공통 유틸로 분리 권장(예 `LeaveUnitGranularity`).

---

# §balanceDays 산출 (이중차감 금지)

## 공식 (leave01/웹 정합)
종류(LEAVE_CD)별, 활성집합(`STATUS='ACTIVE' AND EXPIRE_YN='N' AND DEL_YN='N'`):
```
balanceDays(leaveCd) = SUM(G.GRANT_DAYS) - SUM(G.USED_DAYS)
```
- `USED_DAYS` 는 사용 캐시(확정 사용 합계 동기화값)이므로 **`tb_user_leave_use` 를 다시 빼지 않는다**(leave01 메모리 §2.3 이중차감 주의 = 회피). `planned` 같은 별도 차감 없음.
- SUM 이 null(부여 없음) → `0.0` 폴백. 소수1자리 `RoundingMode.HALF_UP`(leave01 `toScaledDouble` 동일).
- 신청 가능 잔여를 정확히 표현하려면 **법정/비법정 무관 LEAVE_CD 단위 합산**(grantTypePrefix 분기 없음). 종류 목록 쿼리에 LEAVE_CD 별 grant 집계를 LEFT JOIN/서브쿼리로 결합.

## §applicable / disabled 처리 방침 (planner 결정)
- **목록에서 제외하지 않고 유지**, `applicable=false` 로 내려 FE 가 비활성(disabled) 표시.
  - 근거: 종류 자체는 회사가 운영 중(USE_YN='Y')이나 일시적 잔여 0 일 뿐 → 사용자에게 "왜 안 보이나" 혼란 방지. 잔여 정보도 함께 보여줘야 폼 UX(잔여 표시) 충족.
- `applicable = (balanceDays > 0)`. (유효기간 외/소멸 등은 활성집합 필터로 이미 balance 에 반영됨 → 별도 플래그 불필요.)
- ⚠️ 단, **018-B(제출)** 는 잔여검증을 `selectDeductibleGrant`(유효기간·FOR UPDATE)로 재수행하므로 `applicable=true` 라도 제출 시점 거부 가능(낙관적 표시 vs 비관적 차감). 본 메타는 표시용임을 018-C 에 명시.
- `USE_YN='N'` 종류는 **쿼리에서 제외**(목록에 아예 없음).

---

# §매퍼 메서드 시그니처 + SQL 스케치

> 규칙: leading 콤마, `#{}` 바인딩, `SELECT *` 금지, 실제 컬럼만, 스코프 WHERE(cmpny/site/user) 명시.

## AppLeaveFlowMapper (신규)

### m1. selectApplicableLeaveTypes — 종류 + 잔여 (apply-meta 본체)
```java
List<LeaveTypeMetaRow> selectApplicableLeaveTypes(
    @Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);
```
```sql
/* AppLeaveFlowMapper.selectApplicableLeaveTypes */
SELECT
      T.LEAVE_CD        AS leaveCd
    , T.LEAVE_NM        AS leaveNm
    , T.SYSTEM_YN       AS systemYn
    , T.APRV_USE_YN     AS typeAprvUseYn      -- 비법정 결재여부
    , T.USE_UNIT_TYPE   AS useUnitType        -- 비법정 단위(SYS025)
    , IFNULL((
          SELECT SUM(G.GRANT_DAYS) - SUM(G.USED_DAYS)
            FROM TB_USER_LEAVE_GRANT G
           WHERE G.CMPNY_CD = T.CMPNY_CD
             AND G.USER_CD  = #{userCd}
             AND G.LEAVE_CD = T.LEAVE_CD
             AND G.STATUS   = 'ACTIVE'
             AND G.EXPIRE_YN = 'N'
             AND G.DEL_YN   = 'N'
      ), 0) AS balanceDays
  FROM TB_LEAVE_TYPE_MGMT T
 WHERE T.CMPNY_CD = #{cmpnyCd}
   AND T.USE_YN   = 'Y'
 ORDER BY T.SYSTEM_YN DESC, T.LEAVE_NO ASC, T.LEAVE_CD ASC
```
- result record `LeaveTypeMetaRow(leaveCd, leaveNm, systemYn, typeAprvUseYn, useUnitType, balanceDays)` — **SELECT 순서와 동일**.
- balanceDays 타입 `BigDecimal`(decimal(5,1) 합) → 서비스에서 `toScaledDouble`.

### m2. selectCompanyUsageUnit — 법정 회사 단일 허용단위 + 법정 결재여부
```java
LeaveUsagePolicyRow selectCompanyUsageUnit(@Param("cmpnyCd") String cmpnyCd);
```
```sql
/* AppLeaveFlowMapper.selectCompanyUsageUnit */
SELECT
      UP.USAGE_UNIT    AS usageUnit       -- 회사 단일 허용단위 (FULL_DAY..MIN_30)
    , P.APRV_USE_YN    AS policyAprvUseYn  -- 법정 결재여부
  FROM TB_LEAVE_POLICY P
  JOIN TB_LEAVE_USAGE_POLICY UP
    ON UP.POLICY_SEQ = P.POLICY_SEQ
 WHERE P.CMPNY_CD = #{cmpnyCd}
   AND P.USE_YN   = 'Y'
 ORDER BY P.POLICY_SEQ DESC
 LIMIT 1
```
- result record `LeaveUsagePolicyRow(usageUnit, policyAprvUseYn)` — 순서 일치. 없으면 null → 서비스가 법정단위 `FULL_DAY`/결재여부 'N' 폴백(또는 정책서대로 에러; planner 권고: 법정정책 미존재는 비정상 → log.warn + FULL_DAY 폴백).

### m3. searchApprovers — 결재자 검색 (LIMIT/OFFSET)
```java
List<ApproverRow> searchApprovers(
    @Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
    @Param("excludeUserCd") String excludeUserCd, @Param("keyword") String keyword,
    @Param("limit") int limit, @Param("offset") int offset);
```
```sql
/* AppLeaveFlowMapper.searchApprovers (mypage01.selectApprovalCandidates 차용 + LIMIT) */
SELECT
      U.USER_CD                                                   AS userCd
    , U.USER_ID                                                   AS userId
    , U.USER_NM                                                   AS userNm
    , FNC_CMM_INFO_SRCH(U.CMPNY_CD,'BAIM_VAL',U.RANK_CD,'COM007') AS rankNm
    , FNC_CMM_INFO_SRCH(U.CMPNY_CD,'NODE',U.SITE_CD,U.NODE_CD)    AS nodeNm
  FROM TB_USER U
  LEFT JOIN TB_BAIM_VAL_D D
    ON  D.CMPNY_CD = U.CMPNY_CD AND D.BAIM_VAL_CD='COM007'
    AND D.BAIM_VAL_D_CD = U.RANK_CD AND D.USE_YN='Y'
 WHERE U.CMPNY_CD = #{cmpnyCd}
   AND U.SITE_CD  = #{siteCd}
   AND U.USE_YN   = 'Y'
   AND U.WITHDRAWAL_DATE IS NULL
   AND U.ACCOUNT_STATUS  = '01'
   AND U.USER_CD != #{excludeUserCd}
   AND U.AUTH_CD != 'system'
   <if test="keyword != null and keyword != ''">
     AND U.USER_NM LIKE CONCAT('%', #{keyword}, '%')
   </if>
 ORDER BY (D.SORT_IDX IS NULL), D.SORT_IDX ASC, U.USER_NM ASC, U.USER_CD ASC
 LIMIT #{limit} OFFSET #{offset}
```
- result record `ApproverRow(userCd, userId, userNm, rankNm, nodeNm)` — 순서 일치. PII 정렬 보조(rankCd/sortIdx) 비노출.
- hasNext 판정: 서비스가 `limit = size + 1` 로 조회 → 결과 size 초과면 hasNext=true, 초과분 잘라 반환.

## 엔드포인트 2 매퍼: 신규 없음
- `AppMypage01Mapper.selectPresetMasters` + `selectPresetStepsByUser` 재사용(파라미터 cmpnyCd/userCd 동일). 서비스 위임.

---

# §수용 기준 / 엣지

## 공통
- [ ] 모든 식별값(cmpny/site/user) JWT(`TokenInfo gv_*`)에서만 도출. 쿼리/바디 미수신(IDOR 차단, leave01 `from(TokenInfo)` 패턴).
- [ ] 모든 SQL: `SELECT *` 없음, leading 콤마, `#{}` 바인딩, cmpny/site/user 스코프 WHERE 명시.
- [ ] record/result 는 SELECT 컬럼 순서 = 생성자 인자 순서(위치매핑 함정 회피).
- [ ] 빌드 `gradlew compileJava compileTestJava --no-daemon` 통과.

## apply-meta (01)
- [ ] `USE_YN='N'` 종류는 목록에서 제외.
- [ ] 법정 종류 allowedUnits = 회사 USAGE_UNIT 계층 변환과 일치(예 HOUR_1→[00,01,02,03]).
- [ ] 비법정 종류 allowedUnits = 그 타입 USE_UNIT_TYPE 계층 변환. USE_UNIT_TYPE NULL → [00] 폴백.
- [ ] 법정정책 미존재(usagePolicy null) → log.warn + 법정 allowedUnits=[00], aprvRequired=false 폴백(서비스 비폭주).
- [ ] balanceDays 가 leave01 my-leave-summary 의 동일종류 합산과 정합(이중차감 없음). 잔여 0 → applicable=false, 목록 유지.
- [ ] 부여 이력 0건 사용자 → 모든 종류 balanceDays=0.0, applicable=false (500 아님).
- [ ] aprvRequired: 법정=policy.APRV_USE_YN, 비법정=type.APRV_USE_YN ('Y'→true). 둘 다 null → false.

## approval-presets (02)
- [ ] 프리셋 0건 → 빈 list `presets=[]` (200, null 아님).
- [ ] 기본 프리셋(DEFAULT_YN='Y') 이 최상단 정렬(mypage01 ORDER BY 유지).
- [ ] 스텝의 결재자 표시정보만 노출(휴대폰/이메일 미포함). 결재자가 탈퇴(LEFT JOIN userNm null)여도 행 유지(presetId/stepNo/approverUserCd 보존).

## approver-search (03)
- [ ] keyword 미입력(빈/공백) → 전체 후보(사업장 스코프) 첫 페이지 반환(LIKE 생략).
- [ ] LIMIT 항상 부착(size 기본 20, 상한 50 캡). offset = page*size (page 0-base).
- [ ] 본인(`gv_userCd`)·system 계정·비활성(ACCOUNT_STATUS≠'01' / 탈퇴 / USE_YN≠'Y') 제외.
- [ ] cross-site 차단: siteCd 는 JWT 강제(타 사업장 keyword 주입 무력).
- [ ] PII 평문 과다노출 없음(휴대폰/이메일/생년 컬럼 SELECT 안 함). userId 노출은 결재자 식별 최소범위로 허용.
- [ ] keyword 에 `%`/`_` 포함 시 LIKE 와일드카드 오작동 가능 — security/qa 가 이스케이프 필요성 판단(현 mypage01 도 미이스케이프 = 기존동작 정합, 본 단위는 패리티 유지하되 follow-up 표기).

## security 검토 위임 포인트
- approver-search: LIMIT 부재 시 대량조회 DoS / PII 노출 / LIKE 인젝션(와일드카드) — §03 엣지.
- apply-meta: 타 사용자 잔여 조회 불가(userCd JWT only) 확인.

## follow-up (본 단위 밖)
- mypage01 `selectApprovalCandidates` 의 LIKE 와일드카드 미이스케이프(공통) — 별도 정리.
- 018-B 와 allowedUnits 계층 상수 SSOT 공유(`LeaveUnitGranularity` 공통화) — 018-B 착수 시 확정.
