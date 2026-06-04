# prafta-app-018-F — 작업 분해 (웹 BE+FE: 일자상세 팝업 "확정 연차" 표시)

> 분해자: planner / 출처: `prafta-app-018-F-web-confirmed-leave.md` (+ 상위 `prafta-app-018-leave-apply-plan.md`, 선행 `prafta-app-018-D-tasks.md`)
> 영역: web (PRAFTA/prafta-backend + PRAFTA/prafta-web-frontend). 앱 A/B/C/E 와 파일 비충돌.
> 운영규칙: 재개불가 자율진행(Write). Notion 금지(메인 세션 대행). DB 컬럼 추측금지(MCP/실매퍼 검증). MyBatis record 위치매핑. SQL leading콤마·#{}·SELECT * 금지·스코프 WHERE. 화면 CSS변수/scoped/비TS.
> 정책출처: attd §8(연차 사용단위·차감)·§9(근무관리 표시), prafta-019(시간차/LEAVE_DAYS·LEAVE_MINUTES decimal), prafta-024(사용단위 SYS025), prafta-041(직접 연차 사용 REQ_ID NULL·CONFIRMED).

---

## 0. 사실 확정 (코드 정독 결과)

### 0-1. daily-attd-details 권한 경로 (그대로 승계 — ⚠️ 신규 권한 설계 금지)

`Attd07ServiceImpl.getDailyAttdDetails`(116~181행)는 신규 `selectDailyConfirmedLeave` 가 끼어들 자리다. 권한 가드는 **이미 메서드 진입부에 2단으로 존재**하며, 신규 매퍼는 이 가드를 통과한 뒤 동일 `DailyAttdDetailsQuery` 로 호출되므로 **추가 권한 코드 불필요**(가드 승계):

1. **매니저 게이트** (124행): `attdCloseService.canManageNode(gvAuthCd, gvUserCd, gvCmpnyCd, siteCd, nodeCd)` 실패 시 `ATTD_403_002`. → master/hr 또는 해당 부서(상위 포함) 정·부 관리자만 일자상세 조회 가능.
2. **cross-user/site IDOR 재검증** (134행): `selectUserExistInCmpnySite(gvCmpnyCd, siteCd, userCd) <= 0` 이면 `ATTD_404_011`. → 대상 사용자가 호출자 회사/사업장 scope 안에 실재함을 DB로 재확인.

→ 신규 매퍼는 이 두 가드 **뒤** 에서 `selectDailyConfirmedLeave(DailyAttdDetailsQuery.from(param))` 로 호출한다(기존 `selectMonthlyAttdReq` 호출 직후/직전). cross-site IDOR 는 가드 + 쿼리 WHERE 스코프(CMPNY/SITE/USER) 이중으로 차단된다.

### 0-2. TB_USER_LEAVE_USE 실 컬럼 (실매퍼 INSERT 로 검증 — schema-full.sql 에는 테이블 부재(스냅샷 낡음))

`LeaveFlowMapper.xml#insertLeaveUse`(148~189행) INSERT 컬럼으로 실재 확정:
`LEAVE_ID, CMPNY_CD, SITE_CD, USER_CD, LEAVE_CD, REQ_ID, GRANT_ID, START_DATE, START_TIME, END_DATE, END_TIME, USE_UNIT_TYPE, LEAVE_DAYS, LEAVE_MINUTES, LEAVE_REASON, LEAVE_STATUS, DEL_YN, INSERT_NO, INSERT_DATE`.
조인 대상: `TB_LEAVE_TYPE_MGMT (LEAVE_CD → LEAVE_NM)`. 단위라벨: `FNC_CMM_INFO_SRCH(cmpny,'SYST_VAL',USE_UNIT_TYPE,'SYS025')`.
> developer: 빌드 전 MCP 가용 시 `DESCRIBE TB_USER_LEAVE_USE` 로 `START_DATE`(yyyyMMdd char), `LEAVE_DAYS`/`LEAVE_MINUTES` 정밀도, `START_TIME`/`END_TIME`(HHmm) 재확인 권장. 본 분해 컬럼은 운영 INSERT/SELECT 에서 검증된 실존 컬럼만 사용.

### 0-3. 해당일 매칭 조건 (확정: `U.START_DATE = #{workYmd}`)

기존 운영 쿼리 `countDirectLeaveUse`(193~204행)/`selectDirectLeaveGrantIdsByCell`(221~233행)이 **그날 확정 연차를 `START_DATE = #{workYmd}` 단일 등치로 매칭**한다. PRAFTA 연차 사용기록은 일(日) 단위로 1행이 생성되므로(다일 연차도 일자별 분해 저장), 팝업의 단일 일자(workYmd, yyyyMMdd)에는 `U.START_DATE = #{workYmd}` 가 정확하다.
- 범위조건(`START_DATE ≤ workYmd ≤ END_DATE`)은 **불채택**: 현행 데이터 모델이 일 단위 저장이라 등치로 충분하고, 범위로 풀면 동일 연차가 인접일 팝업에 중복 노출될 위험.

### 0-4. ⚠️ 이중표시 핵심 사실 (결재형 연차도 leave_use 는 즉시 CONFIRMED)

`LeaveFlowServiceImpl.submitLeave`:
- 156행: `reqStatus = aprvRequired ? REQ_APPLIED('01') : REQ_APPROVED('02')` — REQ 상태는 결재요부에 따라 분기.
- 202~211행: leave_use 는 **결재 요부와 무관하게 항상 `LEAVE_STATUS='CONFIRMED'` 로 즉시 INSERT**(차감 예약).

따라서 TB_USER_LEAVE_USE 의 CONFIRMED 만으로 거르면:
- **결재형 연차(APRV_USE_YN='Y')**: REQ_STATUS='01'(미처리) 인데 leave_use 는 CONFIRMED → D 의 "근로자 요청" 카드(`selectMonthlyAttdReq`, 미처리01+결재자EXISTS)에도 뜨고, F 의 확정 섹션에도 떠서 **이중표시**.
- **자동확정 연차(APRV_USE_YN='N')**: REQ_STATUS='02', 결재선 0건 → D 카드에 **안 뜸**(F 가 고칠 대상).
- **직접 적용(Attd_05, REQ_ID NULL)**: REQ 자체 없음 → D 카드 무관, F 섹션에만.

→ **이중표시 방지 규칙(확정, §1-A 쿼리에 박는다)**: F 의 `selectDailyConfirmedLeave` 는
  `( U.REQ_ID IS NULL  OR  연결 REQ 의 REQ_STATUS <> '01' )` 인 확정 연차만 내린다.
  즉 **미처리(01) 결재 대기 중인 연차는 D 의 요청 카드가 소유** → F 는 자동확정(02)/직접(REQ_ID NULL)/반려후재... 가 아닌 **확정완료분만** 표시. (반려03 REQ 는 leave_use 가 CANCELLED 로 전이되어 CONFIRMED 필터에서 자동 제외 — `rejectLeave` 흐름이 `cancelLeaveUseByReqId` 호출.)

이 규칙으로 결재형/자동확정/직접을 **상호배타**로 분할 → 같은 연차가 D 카드와 F 섹션에 동시 노출되지 않는다.

### 0-5. 06(연차수정) 처리

06 은 기존 연차 사용기록(LEAVE_ID=REQ.TARGET_ID)을 **일자/일수만** 수정한다(메모리 prafta-025: 단위·종류 보존). 06 자체는 별도 leave_use 행을 신규 생성하지 않고 기존 CONFIRMED 행을 갱신한다.
→ F 섹션은 그날의 **CONFIRMED leave_use 행 자체**를 소스로 하므로, 06 으로 수정된 결과(갱신된 일자/일수)가 자연히 반영된다. 06 의 미처리 결재(REQ_STATUS='01')는 D 카드가 소유(0-4 규칙으로 F 에서 자동 제외). → **06 별도 분기 불필요**.

---

## prafta-app-018-F-1 [backend] selectDailyConfirmedLeave 신규 + DailyAttdDetailsResponse 확장

- **유형**: backend (보완)
- **영역**: web
- **모듈**: attd/attd07
- **작업유형**: 보완(신규 매퍼 1 + 신규 result record 1 + 응답 DTO +1 필드)
- **요구사항 요약**: 일자상세 응답에 그날 확정 연차 사용내역(`confirmedLeaves[]`)을 실어, 결재 유무·요청 상태와 무관하게 확정된 연차(특히 자동확정 시간차)를 팝업이 표시할 수 있게 한다.

### 핵심 요구사항

1) **신규 result record** `ConfirmedLeaveResult.java` 작성 (`...attd07/result/`):
   - 필드(SELECT 컬럼 순서와 1:1 위치매핑, record 끝=SELECT 끝):
     1. `String leaveCd` — 연차 종류 코드 (TB_USER_LEAVE_USE.LEAVE_CD)
     2. `String leaveNm` — 연차 종류명 (TB_LEAVE_TYPE_MGMT.LEAVE_NM, 예 "월차")
     3. `String useUnitType` — 사용단위 코드 [SYS025] ('00'종일/'01'반차/'02'2시간/'03'1시간/'04'30분)
     4. `String unitNm` — 사용단위 한글 라벨 (SYS025 FNC 산출: 종일/반차/2시간/1시간/30분)
     5. `String startTime` — 시작 시각 (HHmm, 시간차일 때만 의미)
     6. `String endTime` — 종료 시각 (HHmm, 시간차일 때만 의미)
     7. `String leaveDays` — 차감 일수 (decimal 문자열 그대로 — FE 정규화)
   - 각 필드 한국어 주석 + 클래스 상단에 "⚠️ record 끝 = SELECT 끝, 위치기반 매핑" 경고 1줄.
   - (LEAVE_MINUTES 는 현 표시 포맷에 미사용 → 미포함. 필요 시 follow-up.)

2) **신규 매퍼 메서드** `Attd07Mapper.java`:
   ```java
   /** PRAFTA-APP-018-F - 그날(workYmd) 확정 연차 사용내역. 결재 유무 무관, 미처리(01) 결재대기분은 제외(D 카드 소유). */
   List<ConfirmedLeaveResult> selectDailyConfirmedLeave(DailyAttdDetailsQuery query);
   ```
   - import 추가: `com.prafta.web.attd.attd07.result.ConfirmedLeaveResult`.

3) **신규 매퍼 XML** `Attd07Mapper.xml#selectDailyConfirmedLeave` (`selectMonthlyAttdReq` 직후/`selectDailyLeaveApprovalHistory` 근처에 배치). SELECT 컬럼 순서는 record 필드 순서와 **정확히 동일**:
   ```xml
   <select id="selectDailyConfirmedLeave"
           parameterType="com.prafta.web.attd.attd07.application.query.DailyAttdDetailsQuery"
           resultType="com.prafta.web.attd.attd07.result.ConfirmedLeaveResult">
   /* Attd07Mapper.selectDailyConfirmedLeave */
   <!-- PRAFTA-APP-018-F: 그날 확정 연차(TB_USER_LEAVE_USE, CONFIRMED) 표시.
        결재형은 leave_use 가 즉시 CONFIRMED 라도 REQ 미처리(01)면 D 의 '근로자 요청' 카드가 소유 →
        ( U.REQ_ID IS NULL OR R.REQ_STATUS <> '01' ) 로 미처리 결재대기분을 제외해 D/F 이중표시를 막는다.
        단위라벨은 SYS025, 종류명은 TB_LEAVE_TYPE_MGMT(LT). 스코프는 일자상세와 동일(CMPNY/SITE/USER). -->
   SELECT
         U.LEAVE_CD                                                          AS leaveCd
       , LT.LEAVE_NM                                                         AS leaveNm
       , U.USE_UNIT_TYPE                                                     AS useUnitType
       , FNC_CMM_INFO_SRCH(U.CMPNY_CD, 'SYST_VAL', U.USE_UNIT_TYPE, 'SYS025') AS unitNm
       , U.START_TIME                                                        AS startTime
       , U.END_TIME                                                          AS endTime
       , U.LEAVE_DAYS                                                        AS leaveDays
     FROM TB_USER_LEAVE_USE U
     LEFT JOIN TB_LEAVE_TYPE_MGMT LT
            ON LT.CMPNY_CD = U.CMPNY_CD
           AND LT.LEAVE_CD = U.LEAVE_CD
     <!-- 미처리(01) 결재대기 연차 제외용: 연결 REQ 의 상태 확인(LEFT JOIN — 직접사용은 REQ_ID NULL). -->
     LEFT JOIN TB_USER_ATTD_REQ R
            ON R.CMPNY_CD = U.CMPNY_CD
           AND R.REQ_ID   = U.REQ_ID
    WHERE U.CMPNY_CD     = #{gvCmpnyCd}
      AND U.SITE_CD      = #{siteCd}
      AND U.USER_CD      = #{userCd}
      AND U.START_DATE   = #{workYmd}
      AND U.LEAVE_STATUS = 'CONFIRMED'
      AND U.DEL_YN       = 'N'
      AND ( U.REQ_ID IS NULL OR R.REQ_STATUS <> '01' )   <!-- 미처리 결재대기분은 D 카드가 소유(이중표시 방지) -->
    ORDER BY U.START_TIME, U.LEAVE_ID
   </select>
   ```
   - ⚠️ `siteCd`/`userCd` 는 `DailyAttdDetailsQuery` 의 필드명 그대로 바인딩(query 에 `gvCmpnyCd`, `siteCd`, `userCd`, `workYmd` 존재 — `DailyAttdDetailsQuery.java` 7~16행 확인).
   - leading 콤마·`#{}`·SELECT * 금지 준수. WHERE 에 CMPNY/SITE/USER/일자 스코프 고정(IDOR 불변).

4) **서비스 배선** `Attd07ServiceImpl.getDailyAttdDetails`:
   - 권한 가드(124행 canManageNode + 134행 selectUserExistInCmpnySite) **뒤**, `selectMonthlyAttdReq` 호출부(170행) 인근에 추가:
     ```java
     List<ConfirmedLeaveResult> confirmedLeaveResultList =
         attd07Mapper.selectDailyConfirmedLeave(DailyAttdDetailsQuery.from(param));
     ```
   - `DailyAttdDetailsResponse.builder()` 에 `.confirmedLeaveResultList(confirmedLeaveResultList)` 추가.
   - import: `com.prafta.web.attd.attd07.result.ConfirmedLeaveResult`.

5) **응답 DTO 확장** `DailyAttdDetailsResponse.java`:
   - 필드 1개 추가(@Value @Builder record-like — 기존 List 필드들과 동형):
     ```java
     /** PRAFTA-APP-018-F: 그날 확정 연차 사용내역(자동확정/직접 포함, 미처리 결재대기 제외). */
     List<ConfirmedLeaveResult> confirmedLeaveResultList;
     ```
   - import: `com.prafta.web.attd.attd07.result.ConfirmedLeaveResult`.
   - ⚠️ `@Value @Builder` 는 위치매핑 아님(빌더 명시) → 필드 위치 자유. record 위치매핑 경고는 **`ConfirmedLeaveResult`(매퍼 결과)에만** 적용.

### 영향 파일
- (신규) `prafta-backend/.../web/attd/attd07/result/ConfirmedLeaveResult.java`
- `prafta-backend/.../web/attd/attd07/mapper/Attd07Mapper.java` (+메서드 1)
- `prafta-backend/.../resources/com/prafta/web/attd/attd07/mapper/Attd07Mapper.xml` (+select 1, `selectMonthlyAttdReq`(1055행) 뒤 인근)
- `prafta-backend/.../web/attd/attd07/service/impl/Attd07ServiceImpl.java` (getDailyAttdDetails +1 호출, builder +1)
- `prafta-backend/.../web/attd/attd07/dto/response/DailyAttdDetailsResponse.java` (+필드 1)

### 영향 endpoint
- 기존 일자상세 조회(`POST /webApi/attd/attd07/daily-attd-details`, controller `Attd07Controller`) 응답에 `confirmedLeaveResultList` 키 추가. **신규 endpoint 없음**.

### 타 statement 회귀 점검
- `ConfirmedLeaveResult` 는 신규 record → 기존 매퍼/응답 무영향.
- `DailyAttdDetailsResponse` 에 List 필드 1개 추가 → @Builder 라 기존 빌더 호출(다른 화면 없음, 이 응답은 일자상세 전용) 무영향.
- `selectMonthlyAttdReq`(D 카드)·`selectDailyLeaveApprovalHistory`·`selectDailyOvertimeList` 전부 **불변**. F 는 신규 쿼리 1건만 추가.

### 보안/스코프
- 권한: getDailyAttdDetails 진입부 2단 가드(canManageNode + selectUserExistInCmpnySite) **그대로 승계**(0-1). 신규 쿼리는 가드 통과 후 호출.
- 쿼리 WHERE: `CMPNY_CD/SITE_CD/USER_CD/START_DATE` 스코프 고정 → cross-site/cross-user IDOR 불가(타인 연차 무단노출 없음). gv* 식별값은 JWT 도출(DailyAttdDetailsQuery 경유).
- LT/R 조인은 `CMPNY_CD` 동일성 + `LEAVE_CD`/`REQ_ID` 1:1 고정으로 타 회사·타 요청 유출 차단.

### 선행 작업
- 없음(D-1 과 독립 — D-1 은 `selectMonthlyAttdReq`/`MonthlyAttdReqResult` 만 건드림, F 는 신규 쿼리/record). 병렬 가능.

### 우선순위 근거
- 법적 책임영역(attd) +1 격상. 자동확정 연차가 근무관리 팝업에 전혀 안 보이는 정보 누락 정정(관리자 가시성 직결).

---

## prafta-app-018-F-2 [frontend-screen] AttdDayDetailPop 확정 연차 섹션 신규

- **유형**: frontend-screen (보완) — 기존 화면 보완. 신규 UI 명세 없이 신규 섹션(템플릿 블록 + computed + CSS) 추가.
- **영역**: web
- **모듈**: attd/attd07
- **화면 위치**: `prafta-web-frontend/prafta-web-frontend/src/views/attd/popup/AttdDayDetailPop.vue`
- **요구사항 요약**: 응답의 `confirmedLeaveResultList` 를 "연차 사용"(확정) 전용 섹션으로 표시. `{종류명} · {단위라벨(시간차면 접두)} · (시간차면 시각) · {정규화 일수}일 차감`. D 의 요청 카드와 시각적으로 구분.

### 핵심 요구사항

1) **데이터 보관 ref 추가**(1093행 `reqList` 근처):
   ```js
   const confirmedLeaves = ref([]); // 그날 확정 연차 사용내역 (confirmedLeaveResultList)
   ```

2) **로드 배선**(2885행 `reqList.value = ...` 근처):
   ```js
   confirmedLeaves.value = response.data?.confirmedLeaveResultList ?? [];
   ```

3) **표시용 computed** `confirmedLeaveCards`(reqCards computed 1684행 인근):
   - D 의 leave 분기 표시 규약을 **재사용**(라벨/시간차 접두/일수 정규화 동일 톤). 단 소스가 leave_use 직접이라 reqType 없음 → 종류명(leaveNm)을 머리라벨로.
   ```js
   // PRAFTA-APP-018-F: 그날 확정 연차(자동확정/직접 포함) 표시 카드.
   //   D 의 요청 카드(미처리 결재대기)와 상호배타(백엔드가 미처리01 제외) → 이중표시 없음.
   //   포맷: {leaveNm} · {단위(시간차면 '시간차 ' 접두)} · (시간차면 시각) · {정규화}일 차감.
   const confirmedLeaveCards = computed(() =>
     (confirmedLeaves.value || []).map((lv, i) => {
       const unitCode = lv.useUnitType ?? null;          // '00'~'04' 또는 null
       const isTimed = ["02", "03", "04"].includes(unitCode);
       const unitLabel = lv.unitNm
         ? (isTimed ? `시간차 ${lv.unitNm}` : lv.unitNm)
         : "연차";
       const days = normalizeDays(lv.leaveDays);          // 기존 헬퍼 재사용
       return {
         key: `cl-${i}`,
         leaveNm: lv.leaveNm || "연차사용",
         unitLabel,
         timeRange: isTimed ? `${fmtTime(lv.startTime)}~${fmtTime(lv.endTime)}` : null, // 기존 fmtTime 재사용
         leaveDaysLabel: days ? `${days}일 차감` : "",
       };
     })
   );
   ```
   - normalizeDays(1128행)/fmtTime(1105행) **기존 헬퍼 그대로 재사용**(신규 헬퍼 불필요).

4) **템플릿 신규 섹션**(189행 "근로자 요청" `.req-section` 블록 **뒤**, 같은 left pane 안):
   - D 의 요청 카드 섹션과 **분리**된 독립 섹션. 헤더 "연차 사용". `.req-leave-line`/`.req-leave-seg` 기존 클래스 재사용(D 가 추가해둠).
   ```html
   <!-- PRAFTA-APP-018-F: 확정 연차 사용 섹션 (자동확정/직접 포함, 요청 카드와 별개) -->
   <div v-if="confirmedLeaveCards.length" class="leave-use-section">
     <div class="req-section-head">
       <h3>연차 사용</h3>
       <span class="req-count">({{ confirmedLeaveCards.length }})</span>
     </div>
     <div class="leave-use-list">
       <div
         v-for="card in confirmedLeaveCards"
         :key="card.key"
         class="req-leave-line"
       >
         <span class="req-leave-seg">{{ card.leaveNm }}</span>
         <span class="req-leave-seg">{{ card.unitLabel }}</span>
         <span v-if="card.timeRange" class="req-leave-seg">{{ card.timeRange }}</span>
         <span v-if="card.leaveDaysLabel" class="req-leave-seg">{{ card.leaveDaysLabel }}</span>
       </div>
     </div>
   </div>
   ```
   - 승인/반려 버튼 **없음**(확정분은 처리 대상 아님, 표시 전용) → D 요청 카드와 구분되는 핵심 차이.

5) **CSS**(scoped, CSS 변수 기반 — `.req-section` 톤 따름):
   ```css
   /* PRAFTA-APP-018-F: 확정 연차 사용 섹션 (요청 카드와 구분, 표시 전용) */
   .leave-use-section { margin-top: 14px; }
   .leave-use-list { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
   ```
   - `.req-leave-line`/`.req-leave-seg` 는 D 가 이미 정의(3415~3434행) → 재사용. 신규 색상/픽셀 하드코딩 금지(`var()` 폴백 패턴 D 와 동일 유지). `!important` 금지.

### 표시 규약 (D 와 일치)
- 시간차(02/03/04): `{leaveNm} · 시간차 N시간(또는 30분) · {시작}~{종료} · {N}일 차감`. 예) `월차 · 시간차 1시간 · 03:00~04:30 · 0.19일 차감`.
- 종일(00): `{leaveNm} · 종일 · 1일 차감`. timeRange 미표시(공란 아님).
- 반차(01): `{leaveNm} · 반차 · 0.5일 차감`. timeRange 미표시.
- 단위 NULL(비정상): unitLabel '연차' fallback, timeRange 없음 — 카드 안 깨짐.
- 일수 0/NULL: leaveDaysLabel '' → 숨김(빈 "· 일 차감" 방지).

### 영향 파일
- `prafta-web-frontend/prafta-web-frontend/src/views/attd/popup/AttdDayDetailPop.vue`
  (ref +1, 로드 배선 +1, computed +1 `confirmedLeaveCards`, 템플릿 신규 섹션 +1, scoped CSS +2 클래스)

### 백엔드 의존
- F-1 신규 응답 키 `confirmedLeaveResultList[]` (필드: leaveCd/leaveNm/useUnitType/unitNm/startTime/endTime/leaveDays). endpoint 불변(기존 일자상세 조회 응답에 포함).

### 선행 작업
- prafta-app-018-F-1 (BE 응답 필드 선행). FE 자체는 NULL-safe(`?? []`)라 필드 없이도 빈 섹션으로 안전 렌더 → 병렬 작성 후 통합.

### 우선순위 근거
- attd +1 격상. F-1 후속.

---

## 3. 수용 기준 / 엣지

### 기능
- [자동확정 시간차] APRV_USE_YN='N' 월차 1시간(03) 신청 즉시: 팝업 "연차 사용" 섹션에 `월차 · 시간차 1시간 · 03:00~04:30 · 0.19일 차감` 표시(D 요청 카드엔 안 뜸 = 정상).
- [자동확정 종일] 00: `연차사용/종류명 · 종일 · 1일 차감`. timeRange 미표시. leaveDays 1.00000→'1'.
- [자동확정 반차] 01: `… · 반차 · 0.5일 차감`. timeRange 미표시.
- [직접 적용] Attd_05 에서 관리자가 법정연차 직접 적용(REQ_ID NULL, CONFIRMED): F 섹션에 표시(D 무관).
- [06 연차수정] 수정 후 갱신된 일자/일수가 F 섹션에 반영(기존 CONFIRMED 행 갱신 소스, 06 별도 분기 없음).

### 이중표시 방지 (필수)
- [결재형 미처리] APRV_USE_YN='Y' 연차 REQ_STATUS='01': **D 요청 카드에만** 표시, F 섹션엔 **안 뜸**(쿼리 `R.REQ_STATUS <> '01'` 제외). 같은 연차 동시 노출 0건.
- [결재형 승인후] REQ_STATUS='02': D 카드에서 사라지고 F 섹션으로 이동 표시(상호배타 분할).
- [반려후] REQ_STATUS='03': leave_use CANCELLED 전이 → CONFIRMED 필터에서 자동 제외 → F 섹션 미표시(정상).

### 회귀 (필수 무영향)
- D 의 "근로자 요청" 카드(미처리01 연차/근태보정/OT/스케줄수정): 표시·승인/반려·사유보기·결재 라우팅(approvalStep) 전부 불변.
- 01/02 근태보정·03/04 OT·10 스케줄수정 카드·처리 이력·월마감 disabled: 무영향.
- `selectMonthlyAttdReq`/`selectMonthlyAttdReqSummary`/`selectDailyLeaveApprovalHistory`/`selectDailyOvertimeList`: 쿼리·resultType 불변.

### 권한/스코프
- [매니저 게이트] 비관리자 호출: 기존 `ATTD_403_002` 그대로(F 신규 쿼리는 가드 통과 후만 실행).
- [cross-site IDOR] 타 사업장 사용자 userCd 위조: `selectUserExistInCmpnySite` 404 + 쿼리 WHERE SITE_CD 스코프 이중 차단 → 타인 연차 노출 0.

### 엣지
- [없음] 그날 확정 연차 0건: `confirmedLeaveCards.length===0` → 섹션 `v-if` 미렌더(빈 헤더 방지).
- [LT 미매칭] 종류명 NULL(LEAVE_TYPE_MGMT 행 부재 비정상): leaveNm '연차사용' fallback.
- [정밀도] leaveDays 0.18750 등 → normalizeDays 0.1875 정리. 시간차 차감 소수 안전.
- [다일 연차] 일 단위 저장이라 START_DATE 등치로 그날 분만 — 인접일 팝업 중복 0.

### 검증 메모 (developer/qa)
- BE: 빌드 통과. MCP 가용 시 `DESCRIBE TB_USER_LEAVE_USE`/`TB_LEAVE_TYPE_MGMT` 로 START_DATE/LEAVE_NM/시각 컬럼 재확인. `ConfirmedLeaveResult` SELECT 컬럼 순서=record 필드 순서 위치 일치 1회 확인(밀림 시 시각→일수 침범).
- FE: SFC/eslint 통과. 자동확정 3종(시간차/종일/반차) + 이중표시 4케이스(미처리01/승인02/반려03/직접NULL) + 회귀(D 요청 카드 4종) 수동 1회.
- ⚠️ developer/qa: F 의 핵심 정확성은 "이중표시 방지 WHERE(`U.REQ_ID IS NULL OR R.REQ_STATUS <> '01'`)" 다. 이 절이 빠지면 결재형 연차가 D+F 동시 노출 → 회귀로 반드시 검증.
