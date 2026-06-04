# prafta-app-017 — 작업 분해 (planner)

원본 작업지시서: `.claude/requests/app_requests/prafta-app-017-overtime-guards-plan.md`
분해: planner. 영역 혼합(앱BE / 앱FE / 웹FE) — 작업별 영역 태깅.
정책 출처: 근태관리 정책서 §10.1(정규 근무 계산), §10.3(추가근무 인정·계산), §9.3(초과근무 상신).

## 스키마/코드 정독 결과 (분해 전제 — 추측 아님, 실제 확인값)

- **TB_USER_WORK_PLAN**: PK `(CMPNY_CD, SITE_CD, USER_CD, WORK_YMD)` — **하루 1행**. 컬럼 `WORK_PLAN_CD`(varchar20, nullable) 단일. 주석상 `[SCH_CD, LEAVE_CD]` — 즉 그날 근무계획은 스케줄코드(SCH_CD)이거나 연차코드(LEAVE_CD)다. **WORK_SEQ 컬럼 없음.**
- **TB_SCH_MGMT**: PK `(CMPNY_CD, SITE_CD, SCH_CD)`. `FST_SCH_STR_TIME`/`FST_SCH_END_TIME`(varchar4, NOT NULL) = 1구간, `SEC_SCH_STR_TIME`/`SEC_SCH_END_TIME`(varchar4, nullable) = 2구간. HHMM 문자열.
  - ⇒ **하루 스케줄의 1구간/2구간은 같은 SCH_CD 한 행 안에서 FST*/SEC* 컬럼으로 구분된다.** OT slot.workSeq=1 → FST*, workSeq=2 → SEC* 와 비교.
  - ⇒ WORK_PLAN_CD 가 LEAVE_CD(연차코드)거나 NULL 이면 TB_SCH_MGMT 조인 결과가 없음 → 정규 스케줄 부재 → 겹침검사 면제(전량 OT 허용).
- **TB_USER_ATTD_REQ**: `REQ_TYPE`(SYS032: 01근태생성/02근태수정/03초과근무생성/04초과근무수정/05연차사용/06연차수정/10스케줄수정), `REQ_STATUS`(SYS033: 01신청/02승인/03반려/04취소), `WORK_SEQ`(int, nullable), `TARGET_ID`(생성요청 NULL). 미처리 = `REQ_STATUS='01'`.
- **AppReq07ServiceImpl.ymdToDays(ymd)** 는 이미 `LocalDate.toEpochDay()` 사용 → (일자+시각) 인스턴트 비교 인프라 존재. 신규 겹침판정도 동일 epochDay*1440 + 분 기준으로 작성한다(시:분 단독 비교 금지).
- **OvertimeForm.vue** 에 `stampOf(ymd,hhmm)`(epochDay*1440+분), `toMin`, `hhmmDisplay`, `slotWindowText(workSeq)` 인프라 존재. context.slots[i] = `{ workSeq, schedule:{startTime,endTime(HHMM)}, attendance:{checkInDate,checkInTime,checkOutDate,checkOutTime} }`.
- **AttdDayDetailPop.vue** `reqCards`(1573~1627행): `req.reqType` 사용 가능(다른 핸들러가 이미 `card.reqType === "03"/"04"` 로 OT 분기 — 2419/2575행). 03=OT생성(TARGET_ID null), 04=OT수정. targetId 필드는 응답에서 직접 쓰지 않고 reqType '03'으로 충분히 식별 가능(03은 정의상 TARGET_ID null).

## 신규 에러코드 배정 (AttdErrorCode.java 실제 미사용 확인 후 배정)

기존 사용 현황: ATTD_400_050~060, 070~072, 080~088, 090~099 모두 사용 중. 089 단일 슬롯은 그룹 의미가 모호하므로 신규 그룹으로 채번.

| 코드 | HTTP | 용도(이슈) | 메시지(사용자 친화 — 입력/룰 위반이므로 구체 노출) |
| --- | --- | --- | --- |
| `ATTD_400_100` | 400 | 이슈① OT 시각이 정규 스케줄 구간과 겹침 | `정규 근무 시간과 겹치는 초과근무는 등록할 수 없어요. 스케줄 시간 외로 입력해 주세요.` |
| `ATTD_400_101` | 400 | 이슈② 해당 일자/구간에 미처리 근태·스케줄 요청 존재 | `처리 중인 근태/스케줄 요청이 있어 초과근무를 등록할 수 없어요. 기존 요청 처리 후 다시 시도해 주세요.` |

> 두 코드 모두 사용자 본인 입력/상태에 대한 안내이므로 구체 메시지 노출(타인·타사업장 정보 누출 없음). enum 추가 위치: `// ===== PRAFTA-APP-017 — 초과근무 등록 가드(스케줄 겹침 / 미처리 요청) =====` 섹션 신설.

---

# 작업 1 — [앱BE] OT 등록 시 정규 스케줄 겹침 거부 (이슈①)

- **유형**: backend / **영역**: app / **모듈**: app.req.req07 / **작업유형**: 보완
- **정책 출처**: 근태관리 정책서 §10.3(추가근무 = 정규 구간 밖 초과분), §10.1(정규 근무 = 스케줄 구간 기준), §9.3(초과근무 상신).
- **요구사항 요약**: `registerOvertime` 에 OT [start,end] ∩ 정규스케줄[schStart,schEnd] = ∅ 검증을 추가. 한 순간이라도 겹치면 요청 전체 거부(`ATTD_400_100`).

## 대상 파일/메서드
- `prafta-backend/src/main/java/com/prafta/app/req/req07/service/impl/AppReq07ServiceImpl.java` — `registerOvertime` 에 검증 로직 + private 헬퍼 추가.
- `prafta-backend/src/main/java/com/prafta/app/req/req07/mapper/AppReq07Mapper.java` — 신규 매퍼 메서드 1개.
- `prafta-backend/src/main/resources/com/prafta/app/req/req07/mapper/AppReq07Mapper.xml` — 신규 SQL 1개.
- `prafta-backend/src/main/java/com/prafta/common/error/attd/AttdErrorCode.java` — `ATTD_400_100` 추가.

## 구현 지시 (developer)
1. **스케줄 1건 조회**: `registerOvertime` 트랜잭션 시작 직후(구조 검증·중복 차단 이후), 해당 (cmpnyCd, siteCd, userCd, workYmd) 의 근무계획 스케줄 1건을 신규 매퍼로 조회.
   - WORK_PLAN_CD 가 SCH_CD 인 경우에만 FST*/SEC* 시각이 채워진다. LEAVE_CD/NULL/매칭 SCH_CD 없음 → null 반환 → **겹침검사 전체 면제(현 동작 유지, 전량 OT 허용)**.
2. **구간 매핑 + 겹침 판정**: 각 slot 에 대해
   - `slot.workSeq == 1` → (FST_SCH_STR_TIME, FST_SCH_END_TIME)
   - `slot.workSeq == 2` → (SEC_SCH_STR_TIME, SEC_SCH_END_TIME)
   - 해당 구간 시각이 null/공백이면 그 slot 은 정규구간 없음 → 면제(continue).
   - 스케줄 구간 인스턴트화: `schStart = stamp(workYmd, schStrTime)`, `schEnd = stamp(workYmd, schEndTime)`. **schEnd ≤ schStart 이면 종료를 익일로 보정**(`schEnd += 1440`) — 야간/자정 넘김(웹 Attd_11 / 앱 attd01 isEarlyStamp 와 동일 규약).
   - OT 인스턴트화: `otStart = stamp(slot.startDate, slot.startTime)`, `otEnd = stamp(slot.endDate, slot.endTime)` (이미 validateSlotsTimes 가 otEnd > otStart 보장).
   - **겹침 조건**: `otStart < schEnd && schStart < otEnd` → 겹침 → `throw new ApiException(AttdErrorCode.ATTD_400_100)`.
     - 경계(접함)는 겹침 아님: OT 07:00~07:12, schEnd=07:00 → `otStart(07:00) < schEnd(07:00)` 거짓 → 허용. ✅
3. **stamp 헬퍼**: `ymdToDays(ymd) * 1440L + parseHHmm(hhmm)` 재사용(기존 private 메서드). parseHHmm 결과 <0 또는 ymdToDays 0 등 형식 위반은 이미 validateSlotsTimes 통과분이므로 OT 측은 안전. 스케줄 측 schStrTime/schEndTime 은 DB varchar(4) HHMM 이므로 parseHHmm 으로 파싱하되, 파싱 실패(예외적 데이터)면 해당 구간 면제(fail-open? → **아니오, fail-closed 원칙**: 스케줄 시각 파싱 실패 시 그 구간은 정규구간 판정 불가이므로 겹침검사를 면제하되 WARN 로그. 정규구간을 "없는 것"으로 보는 것이 사용자 차단보다 안전하며, 거부 게이트는 데이터 오류로 정상요청을 막지 않도록 한다. 단 schedule 행 자체 부재와 구분해 로그.).
4. **로그**: 거부 시 `log.info("[prafta-app-017] OT 스케줄 겹침 거부 — userCd={}, workYmd={}, workSeq={}, ot=[{}~{}], sch=[{}~{}]", ...)` (PII 없음, 시각/코드만).

## 신규 매퍼 시그니처
```java
/**
 * prafta-app-017: OT 겹침 검증용 — 해당 근무일의 근무계획 스케줄 1건(1·2구간 시각) 조회.
 * WORK_PLAN_CD 가 SCH_CD 일 때만 TB_SCH_MGMT 조인이 성립한다.
 * 연차코드/미배정/매칭 스케줄 없음 → null 반환(정규구간 부재 → 겹침검사 면제).
 * 식별값은 Param 의 JWT 도출값만 사용(IDOR).
 */
ScheduleWindowResult selectWorkPlanSchedule(@Param("cmpnyCd") String cmpnyCd
                                            , @Param("siteCd") String siteCd
                                            , @Param("userCd") String userCd
                                            , @Param("workYmd") String workYmd);
```

신규 결과 DTO (record): `com.prafta.app.req.req07.dto.response.result.ScheduleWindowResult`
```java
public record ScheduleWindowResult(
        String schCd
        , String fstStrTime   // HHMM
        , String fstEndTime   // HHMM
        , String secStrTime   // HHMM (nullable)
        , String secEndTime   // HHMM (nullable)
) {}
```

## 신규 SQL 스케치 (leading 콤마 / #{} 바인딩 / SELECT * 금지 / 실제 컬럼)
```xml
<!-- ============ prafta-app-017: 근무계획 스케줄 1건 (OT 겹침 검증용) ============
     TB_USER_WORK_PLAN(하루 1행).WORK_PLAN_CD 가 SCH_CD 일 때만 TB_SCH_MGMT 와 INNER 조인 성립.
     LEAVE_CD/NULL/미존재 → 결과 0행 → Service 에서 면제. -->
<select id="selectWorkPlanSchedule" resultType="com.prafta.app.req.req07.dto.response.result.ScheduleWindowResult">
/* AppReq07Mapper.selectWorkPlanSchedule */
SELECT S.SCH_CD            AS schCd
     , S.FST_SCH_STR_TIME  AS fstStrTime
     , S.FST_SCH_END_TIME  AS fstEndTime
     , S.SEC_SCH_STR_TIME  AS secStrTime
     , S.SEC_SCH_END_TIME  AS secEndTime
  FROM TB_USER_WORK_PLAN P
  INNER JOIN TB_SCH_MGMT S
     ON S.CMPNY_CD = P.CMPNY_CD
    AND S.SITE_CD  = P.SITE_CD
    AND S.SCH_CD   = P.WORK_PLAN_CD
 WHERE P.CMPNY_CD = #{cmpnyCd}
   AND P.SITE_CD  = #{siteCd}
   AND P.USER_CD  = #{userCd}
   AND P.WORK_YMD = #{workYmd}
 LIMIT 1
</select>
```
> 주의: `resultType` record 매핑은 SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반). 위 순서(schCd, fstStr, fstEnd, secStr, secEnd)를 record 인자 순서와 100% 일치시킬 것(메모리: MyBatis record 컬럼순서 함정).

## 수용 기준 (엣지 포함)
- AC1: 스케줄 00:00~07:00, OT 06:58~07:12(앞 2분 겹침) → `ATTD_400_100` 거부.
- AC2: OT 07:00~07:12(schEnd 07:00 에 접함) → 허용(겹침 아님).
- AC3: OT 06:30~06:58(전부 정규구간 내) → 거부.
- AC4: WORK_PLAN_CD = LEAVE_CD(연차일) → 스케줄 없음 → 전량 허용.
- AC5: WORK_PLAN_CD NULL/근무계획 행 없음 → 전량 허용.
- AC6: 2구간 스케줄(FST 09:00~18:00, SEC 19:00~22:00), OT slot.workSeq=2 가 18:30~18:55 → SEC 구간(19:00~22:00)과 비교 → 겹침 없음 → 허용(FST 와는 비교 안 함 — 구간 매핑이 workSeq 기준). slot.workSeq=2 가 19:30~20:00 → SEC 와 겹침 → 거부.
- AC7: 야간 스케줄 1구간 22:00~익일06:00(schEnd<schStart → +1일 보정), OT 익일 06:00~07:00 → 겹침 없음 허용. OT 익일 05:30~07:00 → 겹침 거부.
- AC8: SEC* 컬럼 null(1구간 전용 스케줄)인데 OT slot.workSeq=2 제출 → 정규구간 없음 → 그 slot 면제(허용).
- AC9: 동일 입력에 대해 FE 사전차단 결과와 BE 거부 결과 일치(작업 3과 교차검증). BE 가 최종 권위.

---

# 작업 2 — [앱BE] OT 등록 시 미처리 근태/스케줄 요청 차단 (이슈②)

- **유형**: backend / **영역**: app / **모듈**: app.req.req07 / **작업유형**: 보완
- **정책 출처**: 근태관리 정책서 §9.3(초과근무 상신), §11(근태 보정 요청), §9.2(스케줄 수정 요청) — 미처리 요청과의 정합성.
- **요구사항 요약**: `registerOvertime` 에서 (a) 근태보정 미처리(REQ_TYPE IN '01','02', STATUS '01')가 해당 (USER, WORK_YMD, WORK_SEQ) 에 존재하면 그 구간 OT 거부, (b) 스케줄수정 미처리(REQ_TYPE '10', STATUS '01')가 해당 (USER, WORK_YMD) 에 존재하면 그날 모든 구간 OT 거부. 위반 시 `ATTD_400_101`.

## 대상 파일/메서드
- `AppReq07ServiceImpl.java` — `registerOvertime` 에 검증 추가(작업1 검증과 같은 위치 군).
- `AppReq07Mapper.java` — 신규 매퍼 2개.
- `AppReq07Mapper.xml` — 신규 SQL 2개.
- `AttdErrorCode.java` — `ATTD_400_101` 추가.

## 구현 지시 (developer)
1. **스케줄수정 미처리(전일 차단)**: slots 루프 전 1회 검사.
   - `int pendSched = mapper.countPendingSchedModify(cmpnyCd, siteCd, userCd, workYmd);`
   - `if (pendSched > 0) throw new ApiException(AttdErrorCode.ATTD_400_101);`
2. **근태보정 미처리(구간 차단)**: 각 slot 에 대해
   - `int pendCorr = mapper.countPendingAttdCorrectionBySlot(cmpnyCd, siteCd, userCd, workYmd, slot.getWorkSeq());`
   - `if (pendCorr > 0) throw new ApiException(AttdErrorCode.ATTD_400_101);`
   - 효율을 위해 slots 루프 내 INSERT 전에 검사하거나, 작업1 겹침검사와 동일 사전 루프에서 일괄 검사(권장: 모든 가드를 INSERT 시작 전에 모아 fail-closed).
3. **순서**: 구조검증 → 중복차단(기존 countDuplicateReq) → **작업2 미처리 가드(스케줄수정 전일 → 슬롯별 근태보정)** → **작업1 겹침 가드** → INSERT. (거부 게이트를 모두 INSERT 앞에 두어 부분 INSERT 방지. `@Transactional` 이라 롤백되지만 게이트 선행이 명확.)
4. **WORK_SEQ NULL 대비**: 근태보정 요청이 WORK_SEQ NULL 로 저장된 레거시가 있을 수 있으나, 본 카운트는 `WORK_SEQ = #{workSeq}` 정확 매칭. 정책상 보정요청은 구간 단위이므로 NULL 레거시는 매칭에서 빠짐(그 구간 OT 허용) — 사용자 노출 안전(과도차단 회피). 필요 시 follow-up.

## 신규 매퍼 시그니처
```java
/**
 * prafta-app-017: 미처리 근태보정 요청(생성01·수정02) 카운트 — 구간 단위.
 * REQ_TYPE IN ('01','02') AND REQ_STATUS='01' AND DEL_YN='N', 해당 WORK_SEQ.
 */
int countPendingAttdCorrectionBySlot(@Param("cmpnyCd") String cmpnyCd
                                     , @Param("siteCd") String siteCd
                                     , @Param("userCd") String userCd
                                     , @Param("workYmd") String workYmd
                                     , @Param("workSeq") Integer workSeq);

/**
 * prafta-app-017: 미처리 스케줄수정 요청(10) 카운트 — 그날 전체(구간 무관).
 * REQ_TYPE='10' AND REQ_STATUS='01' AND DEL_YN='N'.
 */
int countPendingSchedModify(@Param("cmpnyCd") String cmpnyCd
                            , @Param("siteCd") String siteCd
                            , @Param("userCd") String userCd
                            , @Param("workYmd") String workYmd);
```

## 신규 SQL 스케치
```xml
<!-- ============ prafta-app-017: 미처리 근태보정 요청 카운트(구간 단위) ============ -->
<select id="countPendingAttdCorrectionBySlot" resultType="int">
/* AppReq07Mapper.countPendingAttdCorrectionBySlot */
SELECT COUNT(1)
  FROM TB_USER_ATTD_REQ A
 WHERE A.CMPNY_CD   = #{cmpnyCd}
   AND A.SITE_CD    = #{siteCd}
   AND A.USER_CD    = #{userCd}
   AND A.WORK_YMD   = #{workYmd}
   AND A.WORK_SEQ   = #{workSeq}
   AND A.REQ_TYPE   IN ('01', '02')
   AND A.REQ_STATUS = '01'
   AND A.DEL_YN     = 'N'
</select>

<!-- ============ prafta-app-017: 미처리 스케줄수정 요청 카운트(그날 전체) ============ -->
<select id="countPendingSchedModify" resultType="int">
/* AppReq07Mapper.countPendingSchedModify */
SELECT COUNT(1)
  FROM TB_USER_ATTD_REQ A
 WHERE A.CMPNY_CD   = #{cmpnyCd}
   AND A.SITE_CD    = #{siteCd}
   AND A.USER_CD    = #{userCd}
   AND A.WORK_YMD   = #{workYmd}
   AND A.REQ_TYPE   = '10'
   AND A.REQ_STATUS = '01'
   AND A.DEL_YN     = 'N'
</select>
```

## 수용 기준 (엣지 포함)
- AC1: 근태생성(01) 미처리 WORK_SEQ=1 존재, OT slot.workSeq=1 → `ATTD_400_101` 거부. OT slot.workSeq=2 → 허용(구간 단위).
- AC2: 근태수정(02) 미처리 존재 → 동일하게 그 구간 거부.
- AC3: 스케줄수정(10) 미처리 존재 → 그날 1·2구간 모두 거부.
- AC4: 반려(03)/취소(04)/승인(02) 상태 요청만 존재 → OT 허용(미처리 아님).
- AC5: 다른 일자(WORK_YMD)의 미처리 → 무관(허용).
- AC6: IDOR — userCd/siteCd/cmpnyCd 는 Param JWT 도출값만(본문 신뢰 금지). 이미 OvertimeParam.from 이 강제.
- AC7: 2구간 OT 제출 중 1구간만 미처리 → 1구간 slot 검사 시점에 거부(전체 트랜잭션 롤백, 2구간도 미INSERT).

---

# 작업 3 — [앱FE] OvertimeForm 제출 전 사전 가드(겹침 + 미처리) (이슈①·② FE)

- **유형**: frontend-screen / **영역**: app / **모듈**: app.req(OvertimeForm) / **작업유형**: 보완
- **정책 출처**: 작업1·2 와 동일(§10.1/§10.3/§9.3). FE 는 UX 사전차단이며 **서버 검증이 최종 권위**(이중).
- **요구사항 요약**: ① 제출 시각이 정규 스케줄 구간과 겹치면 제출 비활성 + 안내. ② 컨텍스트에 미처리 요청 정보가 있으면 비활성 + 안내; 없으면 서버 에러 표면화로 범위 축소.
- **연결 UI 명세**: UI 명세 불필요(기존 컴포넌트 수정). 아래 "FE 수정 지시" 가 정확 diff 수준.

> ⚠️ 본 작업은 신규 .vue 골격이 아니라 **기존 OvertimeForm.vue 수정**이다. planner 는 정확한 수정 위치/전후 로직/엣지를 아래에 명세하고, script 로직 본체는 developer 가 채운다(가드 computed 의 시각 비교는 기존 stampOf/toMin 재사용이므로 골격성 로직이며 식별자 보존이 핵심).

## 대상 파일
- `prafta-app-frontend/prafta-app-frontend/src/views/req/components/OvertimeForm.vue`

## FE 수정 지시 (이슈① 겹침 사전차단)

기존 인프라 재사용: `stampOf(ymd, hhmm)`(epochDay*1440+분, NaN 가드), `toMin`, `hhmmDisplay`, context.slots[i].schedule.{startTime,endTime}(HHMM).

1. **신규 computed `slotOverlap(workSeq)`** (표시·차단 공용 헬퍼, 차단 판정용이므로 fail 시 false 가 아니라 **차단 측 안전**은 BE 가 담당 → FE 는 표시 신뢰성 우선이라 계산 불가 시 false 반환=비차단, BE 가 막음):
   - context.slots 에서 해당 workSeq 의 schedule 을 찾는다(없으면 false).
   - schedule.startTime/endTime 둘 다 없으면 정규구간 없음 → false(겹침 아님).
   - 입력값 slot(=`slots.value`의 해당 workSeq 행)의 startDate/startTime/endDate/endTime 으로 OT 인스턴트 산출:
     - `otStart = stampOf(slot.startDate→YMD, slot.startTime→HHMM)`, `otEnd = stampOf(...)`.
     - ⚠️ slots.value 의 startDate/startTime 은 **input 포맷(`YYYY-MM-DD`, `HH:MM`)** 이다. stampOf 는 YYYYMMDD/HHMM 을 받으므로 `inputToYmd()`/`timeToHhmm()` 로 변환 후 전달(기존 유틸 존재).
   - 스케줄 인스턴트: `schStart = stampOf(context.workYmd, schedule.startTime)`, `schEnd = stampOf(context.workYmd, schedule.endTime)`. **schEnd ≤ schStart → schEnd += 1440**(야간 보정, BE 와 동일 규약).
   - NaN 가드: 어느 값이라도 NaN → false(차단 안 함, BE 최종 판정).
   - **겹침 조건**: `otStart < schEnd && schStart < otEnd` → true.
2. **신규 computed `hasOverlap`**: `slots.value.some(s => slotOverlap(s.workSeq))`.
3. **isValid 확장**: 기존 `isValid` 에 `&& !hasOverlap` 추가(제출 버튼 disabled 연동, 기존 `:disabled="!isValid || submitting"` 그대로 활용).
4. **안내 문구**: 기존 `overlapWarning`(2구간 상호 겹침) 메시지 아래 또는 해당 slot 카드 내에 겹침 경고 표시. 권장: SlotCard 안 `.ot-window` 영역 근처에 조건부 `<p class="warn-msg" v-if="slotOverlap(slot.workSeq)">스케줄 시간 내에는 초과근무를 등록할 수 없어요.</p>` 추가. 스타일은 기존 `.warn-msg` 재사용(신규 CSS 변수/하드코딩 금지).
   - ⚠️ workSeq 식별자 보존: `slotOverlap(slot.workSeq)` 로 호출(index 사용 금지). 메모리 경고(planner 골격 로직버그=QA 사각) 준수 — 위치 재인덱싱 금지.

## FE 수정 지시 (이슈② 미처리 사전차단)

- 현재 OvertimeForm 의 `context` 에 미처리 요청 정보가 있는지는 **상위(폼을 띄우는 화면)의 entry-context 응답에 달려 있다.** 작업지시서 §이슈② FE 가 "컨텍스트에 없으면 서버 에러 표면화로 범위 축소 가능" 으로 명시.
- **planner 판단(범위 확정)**: OvertimeForm props.context 현재 스키마(workYmd/slots[{schedule,attendance}]/workPlanName/...)에는 **미처리 요청 정보가 없다.** 이를 FE 사전차단까지 끌어오려면 entry-context API(상위 화면, 본 작업지시서 범위 밖) 확장이 필요하므로 **본 작업에서는 서버 에러 표면화로 범위를 축소**한다.
  - 즉 이슈② FE 는 **신규 사전차단 로직을 넣지 않고**, 제출 후 BE 가 `ATTD_400_101` 을 던지면 상위 화면의 공통 에러 핸들러가 그 메시지(`처리 중인 근태/스케줄 요청이 있어...`)를 그대로 알림으로 노출하도록 보장하는 것으로 충족.
  - **확인 필요(작업 5, 아래)**: 상위 화면의 submit 에러 핸들러가 서버 메시지를 surfacing 하는지. 만약 일반화 메시지로 덮으면 메시지 노출되도록 보완.
- **미처리 컨텍스트 사전차단은 follow-up** 으로 분리(작업지시서 §이슈② FE 명시적 허용). tasks.md 말미 "Follow-up" 참조.

## 수용 기준 (엣지 포함)
- AC1: 스케줄 00:00~07:00, 입력 OT 06:58~07:12 → `slotOverlap(1)` true → 제출 버튼 disabled + 경고 표시.
- AC2: OT 07:00~07:12(접함) → false → 제출 가능.
- AC3: 스케줄 없는 구간(schedule null) → false → 제출 가능(전량 OT).
- AC4: 야간 스케줄 22:00~06:00(익일), 입력 OT 익일 05:30~07:00 → schEnd 보정(+1440) 후 겹침 true → 차단. 익일 06:00~07:00 → 허용.
- AC5: slots.value 의 input 포맷(`YYYY-MM-DD`/`HH:MM`) 을 inputToYmd/timeToHhmm 으로 변환 후 stampOf 호출(미변환 시 NaN → 오판). 변환 누락 회귀 금지.
- AC6: workSeq 로 schedule/slot 매칭(index 금지). 2구간만 입력해도 정확 매칭.
- AC7: 동일 입력에 대해 BE(작업1) 거부와 결과 일치.
- AC8: 표시 전용 slotWindowText 는 기능 불변(회귀 없음).

---

# 작업 4 — [웹FE] OT 생성 요청 카드 BEFORE 공란화 (이슈③)

- **유형**: frontend-screen / **영역**: web / **모듈**: web.attd(AttdDayDetailPop) / **작업유형**: 버그수정
- **정책 출처**: §10.1(정규 근무) — OT 생성은 정규근태를 "변경 전"으로 가질 수 없음(별개 신규 기록). 표시 정합성.
- **요구사항 요약**: `reqCards`(약 1617~1625행) 가 01~06 을 한 분기로 묶어 OT생성(03)도 BEFORE 에 정규근태(`act{n}InTime/OutTime`)를 노출 → OT 생성(03)은 BEFORE 공란("-" 또는 "없음") 표기. 04/타 타입 회귀 방지.

## 대상 파일/위치
- `prafta-web-frontend/prafta-web-frontend/src/views/attd/popup/AttdDayDetailPop.vue` — `reqCards` computed, **1617~1626행**(그 외 분기 return).

## 수정 전 (현행)
```js
    // 그 외(01~06): 기존 출퇴근 시각 BEFORE/AFTER 모델 (현행 유지).
    return {
      ...base,
      mode: "time",
      befIn: fmtTime(r[`act${n}InTime`]) || "-",
      befOut: fmtTime(r[`act${n}OutTime`]) || "-",
      aftIn: fmtTime(req.startTime) || "-",
      aftOut: fmtTime(req.endTime) || "-",
    };
```

## 수정 후 (지시)
- **03(OT 생성)** 은 BEFORE 를 정규근태에서 끌어오지 않는다. `befIn/befOut` 을 `"-"`(또는 빈 표기 "없음")로 고정.
- **04(OT 수정)** 및 **01/02 근태보정**, **05/06 연차** 는 현행 유지(회귀 금지).
- 분기 키는 `req.reqType === "03"`(=OT 생성, 정의상 TARGET_ID null). targetId 필드가 응답에 내려오면 `req.targetId == null` 보조 가드도 허용하나, 03 단독으로 충분.

```js
    // 그 외(01~06): 출퇴근 시각 BEFORE/AFTER 모델.
    //   [prafta-app-017 이슈③] OT '생성'(03, TARGET_ID null)은 "변경 전"이 없으므로
    //   BEFORE 를 정규근태(act{n}*)에서 끌어오면 안 된다 → 공란("-").
    //   OT '수정'(04, TARGET_ID=기존 OT)·근태보정(01/02)·연차(05/06)는 현행 유지.
    const isOtCreate = req.reqType === "03";
    return {
      ...base,
      mode: "time",
      befIn: isOtCreate ? "-" : (fmtTime(r[`act${n}InTime`]) || "-"),
      befOut: isOtCreate ? "-" : (fmtTime(r[`act${n}OutTime`]) || "-"),
      aftIn: fmtTime(req.startTime) || "-",
      aftOut: fmtTime(req.endTime) || "-",
    };
```

> 04(OT 수정)의 BEFORE 를 "기존 OT 값"으로 정밀화하는 것은 데이터 출처(기존 OT 행) 확인이 필요하므로 본 작업 범위에서 제외(현행 유지). 작업지시서 §이슈③ 의 "불확실하면 03만 정정하고 04는 현행 유지 + follow-up" 을 따름. follow-up 주석 1줄 추가 권장: `// TODO(developer): OT 수정(04) BEFORE 를 기존 OT 행 값으로 정밀화 — 별도 작업(prafta-app-017 follow-up).`

## 수용 기준 (엣지 포함)
- AC1: OT 생성요청(reqType '03') 카드 BEFORE = "-"(정규근태 미노출). AFTER = 요청 시각(req.startTime/endTime).
- AC2: OT 수정요청(reqType '04') 카드 BEFORE = 현행(act{n}*) 유지(회귀 없음).
- AC3: 근태보정(01/02) 카드 BEFORE = 현행(act{n}*) 유지.
- AC4: 연차(05/06) 카드 = 현행 유지(이 분기는 time 모델 그대로).
- AC5: 스케줄수정(10) 카드 = sched 모델(상단 분기, 미변경).
- AC6: workSeq(n) 매핑·연차 라우팅(approvalStep) 등 base 필드 회귀 없음.

---

# 작업 5 — [앱FE] OT 제출 서버 에러 메시지 표면화 확인/보완 (이슈② FE 축소분)

- **유형**: frontend-screen / **영역**: app / **모듈**: app.req(OvertimeForm 상위 화면) / **작업유형**: 보완(확인성)
- **정책 출처**: §9.3(초과근무 상신) — 거부 사유 사용자 전달.
- **요구사항 요약**: BE 작업1·2 가 던지는 `ATTD_400_100`/`ATTD_400_101` 메시지가 OT 제출 실패 시 사용자에게 그대로 노출되는지 확인하고, 일반화 메시지로 덮이면 서버 메시지 surfacing 으로 보완.

## 대상 파일(후보 — developer 가 OvertimeForm submit 핸들러 호출처 추적)
- OvertimeForm 의 `@submit` 을 받는 상위 화면(`src/views/req/` 하위 — emit('submit', payload) 수신부)과 그 axios 호출/에러 핸들러.

## 구현 지시 (developer)
1. OvertimeForm `emit('submit', ...)` 수신 화면에서 `POST /appApi/req07/overtime` 호출 후 에러 처리부 확인.
2. 서버 에러 응답의 메시지(`message`/`errorCode`)가 `$alert` 등으로 사용자에게 노출되는지 확인. ATTD_400 대역은 사용자 친화 메시지이므로 그대로 노출이 원칙.
3. 만약 공통 인터셉터가 4xx 를 일반 메시지로 덮거나, 특정 코드(예: COMMON_400_003/600)를 토큰오류로 강제 로그아웃 처리하는 경로(메모리: 앱 근태수정 토큰튕김 버그)에 `ATTD_400_*` 가 잘못 걸리지 않는지 확인(ATTD_400_100/101 은 토큰오류 아님 — 정상 4xx 표면화).

## 수용 기준
- AC1: 미처리 요청 존재 상태에서 OT 제출 → BE `ATTD_400_101` → 사용자에게 "처리 중인 근태/스케줄 요청이 있어..." 노출.
- AC2: 스케줄 겹침 OT 제출(FE 사전차단 우회/비활성 미반영 케이스) → BE `ATTD_400_100` → 메시지 노출.
- AC3: 강제 로그아웃 오발동 없음(ATTD_400_* 는 인터셉터 토큰오류 분기에 걸리지 않음).

---

# 우선순위

1. **작업 1, 2 (앱BE)** — 서버가 최종 권위. 데이터 정합성/법적 책임 영역(attd) +1 격상. 선행 없음(상호 독립이나 같은 메서드 수정이므로 동일 PR 권장).
2. **작업 4 (웹FE)** — 버그수정(오표시). BE 무의존, 즉시 가능.
3. **작업 3 (앱FE)** — UX 사전차단. 작업1 의 규칙과 정합 필요(작업1 후 또는 병행).
4. **작업 5 (앱FE 확인)** — 작업2 의존(에러코드 존재 후 검증).

선행: 작업3·5 는 작업1·2(에러코드·규칙 확정) 선행 권장. 작업4 는 독립.

---

# Follow-up (본 작업 범위 외, 분리)

- **FU-1**: OT 폼 entry-context API 에 "해당 일자/구간 미처리 요청" 정보를 실어 OvertimeForm 에서 이슈② 사전차단(비활성+안내). 상위 화면 + BE entry-context 확장 필요(작업지시서 §이슈② FE 명시 허용).
- **FU-2**: 웹 OT 수정(04) 카드 BEFORE 를 기존 OT 행 값으로 정밀화(데이터 출처: 기존 OT 행). 작업4 에서 제외.
- **FU-3**: 근태보정 요청 WORK_SEQ NULL 레거시 행이 있을 경우 이슈② 구간 매칭 정책 재확인(현재는 과도차단 회피 위해 NULL 미매칭).
- **마이그레이션**: 신규 에러코드는 enum 상수만(DB 스키마 변경 없음). 스케줄수정 REQ_TYPE='10' 은 기존 `prafta-app-007-attd-req-extensions.sql` 적용 전제.
