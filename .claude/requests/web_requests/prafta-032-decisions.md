# prafta-032 — 입사일 변경 "처리방식" 폐기 및 수동 연차 조정 전환 : 확정 결정

> 작성: main 세션(분석·사용자 합의 결과). 다운스트림(planner/developer/qa/security)의 **단일 출처**.
> 합의일: 2026-05-27.

## 0. 작업 성격 (중요)
- 요청서: `.claude/requests/prafta-032.md`(요약), 세부: `.claude/requests/ref/prafta-032-참고.md`.
- prafta-030(차액보전 D1·옵션 시뮬 미리보기)·prafta-031(수동부여 연차 회수·noti outbox)을 **전제**로, 입사일 변경 시 동작하던 **자동 처리방식(SYS039: `KEEP_AND_BACKFILL`/`KEEP_AND_APPLY_NEW`/`RESET_ALL`)을 폐기**하고 **관리자 수동 일수 조정**으로 전환한다.
- 사유: 처리방식 자동계산은 케이스별로 회사/근로자 유불리가 일률적이지 않아 고객 니즈를 모르는 상태에서 고정 자동계산은 리스크가 큼. **시스템은 검증·이력만, 연차 부여량은 관리자가 직접 결정**한다.
- 본 작업 = 신규 구축이 아니라, prafta-030에서 정교화한 엔진의 "처리방식 분기/자동 차액보전·재발급"을 **걷어내고**, 그 자리에 "관리자가 입력한 목표 부여량과의 차액을 추가/회수"하는 단순·명시 경로를 넣는 것.

## 1. 확정 결정 (사용자 합의 2026-05-27)

### D1 — 처리방식 자동계산 폐기 (★핵심, 비활성화 후 제거)
- **FE**: `HireDateEditPop.vue`에서 처리방식 라디오 3종(460·480~507줄) + prafta-030 옵션 시뮬 미니카드(234~265줄) **제거**. 영향분석의 "누락된 부여 / 다음 부여 예정 시점 / FISCAL 다음 회계연도 발생예정"은 **유지**.
- **BE**: `update-user-hire-date`의 `handlingType` 파라미터 **제거**. `LeaveGrantEngineServiceImpl.buildUserPlan`의 `HANDLING_TYPE` 분기·`isReset`(RESET_ALL 취소+재발급)·`isBackfill`(`computeBackfillShortfall` 차액보전) **호출 경로를 끊는다(데드코드 비활성화)**. 물리 삭제는 다음 단계(롤백 안전성 확보 후).
- `TB_USER_HIRE_DATE_HISTORY.HANDLING_TYPE`은 신규분 `'MANUAL'` 고정. 기존 이력 값(KEEP_*/RESET_ALL)은 **보존**(감사 추적).
- **prafta-029/030 가드 보존**: RESET 회차키 누수 가드(`countActiveBySuffixVariant` 등), 차액보전 멱등키(`_BF{histId}`) 관련 기존 데이터·테스트가 깨지지 않게 한다(경로를 끄되 메서드/키 체계는 잔존 허용).

### D2 — 수동 연차 조정 (법정 휴가만)
- `HireDateEditPop.vue`에 **현재 연차 상태 표시**: 법정/법정 외(약정)/전체 각각 부여·사용·잔여(요청서 화면 ②).
- **조정 입력은 법정 휴가(STATUTORY_*) 부여량만**. 약정(법정 외, MANUAL_*)은 **표시만** 하고 수정 입력 없음. **약정 변경은 기존 수동부여 경로(Attd_09 manualGrant)로만 대응**(이번 범위 밖).
- 차액 = (관리자 입력 목표 법정 부여량) − (현재 법정 부여량). `>0` 추가부여(D4), `<0` 회수(D5), `=0` 무처리.
- 입력 미입력 시 현재값 유지(=차액 0).

### D3 — 검증 (회수 가능량)
- **차단식**: `(현재 법정 부여량 − 수정 법정 부여량) > 법정 회수가능량` → 저장 불가.
- **법정 회수가능량** = 현재 ACTIVE 법정 부여 `GRANT_DAYS`합 − `USED_DAYS`합 − **승인 대기/진행 중인 휴가 신청 일수**.
  - 근거(요청서 §3.2, 요약본 13줄 "연차 승인 요청으로 잡힌것들도 카운트"): 이미 사용했거나 **사용 예정(신청 승인 대기/승인완료 미래사용)** 인 연차는 회수 불가.
  - 현행 잔액은 확정 `USED_DAYS`만 반영(승인 전 미차감)하므로, **회수가능량 산정 시 신청 진행 분을 추가로 차감**해야 함. 신청 진행 일수는 휴가신청(leaveflow/`tb_user_attd_req`·`tb_user_leave_use` 예약분)에서 산출(developer가 현행 신청 상태값 확인 후 구현).
- 변경 사유 **필수**. 회수 발생(차액<0) 시 **회수 사유 필수**(요청서는 통합 사유 + 회수 사유 분리; 통합 1개 + 회수 시 별도 1개로 구현).
- 차단 메시지: "회수 가능한 연차는 N일입니다. (잔여 N일) 이미 사용했거나 사용 예정인 연차는 회수할 수 없습니다."

### D4 — 추가 부여 (차액 > 0): 소급 발생일 + 오늘 폴백
1. 새 입사일 기준 **미부여 발생일(오늘 이전)** 을 빠른 순으로 산출(기존 부여 발생일과 비교).
2. 추가 일수만큼 미부여 발생일에 소급 부여: `GRANT_DATE`=오늘(부여 실행일), `AVAIL_FROM_DATE`=발생일, `AVAIL_TO_DATE`=발생일+AXIS6(유효기간), `GRANT_REASON`='입사일 변경 소급(INSADAY_CHANGE_BACKFILL)'.
   - **GRANT_DATE 정합 정정(prafta-032 #3, 2026-05-27)**: 발생일은 `GRANT_DATE`가 아니라 `AVAIL_FROM_DATE`에 기록한다. 이는 기존 정책 기준 부여(`grantComponent`)·소급 백필(`computeBackfillPeriods`)이 모두 `GRANT_DATE`=오늘 / 발생일=`AVAIL_FROM_DATE` 컨벤션을 쓰는 것과 일관시킨 것이다(초안의 "GRANT_DATE=발생일"은 폐기).
3. 미부여 발생일 소진 후 잔여 일수는 **오늘 폴백**: `GRANT_DATE`=오늘, `AVAIL_FROM_DATE`=오늘, `AVAIL_TO_DATE`=오늘+AXIS6, `GRANT_REASON`='입사일 변경 초과 부여(MANUAL_OVERAGE)'.
- **GRANT_TYPE 자동판단**(GRANT_DATE 시점 산정근속, 경력인정 포함): `<12m`→`STATUTORY_MONTHLY`, `12~36m`→`STATUTORY_ANNUAL`, `≥36m`→`STATUTORY_TENURE_BONUS`. (본 기능은 법정만 다루므로 AGREED는 제외.)
- **GRANT_BY_TYPE**: 현행 자동부여 STATUTORY는 `'01'`. 추가부여는 관리자 명시 행위지만 산정 자체는 정책 기반이므로 **`'01'` 유지 + `GRANT_REASON` 태깅으로 출처 식별**(developer가 SYS043 의미·기존 사용처 확인 후 확정; 단 prafta-031 회수 대상(`MANUAL_%`+`'02'`)에 걸리지 않도록 STATUTORY는 회수 대상에서 자연 제외됨).
- 멱등키: 기존 표준키와 충돌하지 않는 입사일변경 전용 접미사 부여(예: `_HD{histId}`). prafta-030 `_BF`·prafta-029 `_R` 체계와 구분.

### D5 — 회수 (차액 < 0): A안(직접 차감) 변형 ★결정 확정
- **대상**: ACTIVE 법정(STATUTORY_*) 부여행만.
- **우선순위**: ① 소멸일(AVAIL_TO_DATE) 가까운 순 → ② 부여일(GRANT_DATE) 최근 순 → ③ GRANT_ID 큰 순.
- **차감 방식**: 정렬된 행을 순회하며 회수량(차액 절대값)을 차감.
  - 행의 잔여(`GRANT_DAYS − USED_DAYS`) 전체를 회수하고 `USED_DAYS=0`인 행 → `STATUS='CANCELED'`(prafta-031 패턴: `CANCEL_REASON`/`CANCEL_DATE`/`CANCEL_BY` 기록).
  - 부분 회수이거나 `USED_DAYS>0`인 행 → `GRANT_DAYS` 직접 차감(UPDATE). `USED_DAYS`는 불변(사용분 보존), `tb_user_leave_use` FK 불변.
- **추적**: `TB_USER_HIRE_DATE_HISTORY`에 회수 전/후 스냅샷(`AFFECTED_GRANT_SNAPSHOT` json) + 신규 컬럼(`OLD_GRANT_TOTAL`/`NEW_GRANT_TOTAL`/`WITHDRAW_REASON`). 필요 시 회수된 행 `GRANT_REASON`에 `[입사일변경 회수 -N일, HIST_xxx]` 태깅.
- **새 회수 테이블(TB_LEAVE_WITHDRAW_HIST) 만들지 않음.** (B안 기각: 현행 잔액·차감 SQL이 전부 ACTIVE GRANT_DAYS 합 기준 → 별도 테이블이면 모든 집계 SQL에 join 추가 필요, 회귀 위험. A안은 잔액 SQL 무수정.)
- prafta-031 LeaveDetailPop/LeaveRecallPop의 기존 MANUAL 회수 버튼/모달은 **건드리지 않음**(별개 경로).

### D6 — Attd_09 "정책 기준 부여" 동작 변경
- **부여 이력이 전혀 없는 사용자만 신규 부여**(신규 입사자/일괄 부여 대상). 부여 이력이 있는 사용자는 **"변경 없음"(skip)**.
- BE: `LeaveGrantEngineServiceImpl`의 처리방식 분기 제거 후 단일 동작 = "기존 부여 있으면 NO_CHANGE, 없으면 정책+입사일+경력인정 기준 신규 부여(월차 D2-B 만1년 일괄소멸 포함 — prafta-030 결정 유지)".
- 미리보기(`PolicyGrantPreviewPop.vue`): **"재발급" 컬럼/카운트 제거**, "처리방식" 컬럼 제거. **신규 부여 / 변경 없음** 만 표시. 응답에서 `reissueCount`·handlingType 제거.

### D7 — 데이터 / 마이그레이션
- `TB_USER_HIRE_DATE_HISTORY`: 신규 컬럼 추가
  - `OLD_GRANT_TOTAL DECIMAL(5,1) NULL` (변경 전 법정 부여 총량)
  - `NEW_GRANT_TOTAL DECIMAL(5,1) NULL` (변경 후 목표 법정 부여 총량)
  - `WITHDRAW_REASON VARCHAR(500) NULL` (회수 사유; 요청서 TEXT 제안이나 prafta CANCEL_REASON과 동일 varchar(500)로 통일)
  - `HANDLING_TYPE`은 NOT NULL 유지하되 신규는 `'MANUAL'`. (NULL 허용으로 바꾸지 않음 — 기존 NOT NULL 제약·데이터 호환.)
- 마이그레이션 파일은 `prafta-backend/src/main/resources/sql/migration/prafta-032-*.sql`로 생성하되 **운영 미적용(사용자 수동 적용)** — prafta-031 패턴.
- 기존 처리방식 이력/자동부여 이력은 보존.

### D8 — 트랜잭션
- 입사일 변경 + 연차 조정(추가/회수) + 이력 INSERT는 **단일 `@Transactional(rollbackFor=Exception.class)`**. 검증 실패 시 전체 롤백.

## 2. 범위 In / Out
- **IN**: `HireDateEditPop.vue` 개편(처리방식 제거, 연차상태 표시, 법정 부여량 입력+차액, 회수 사유), `update-user-hire-date` BE 개편(입사일+수동조정 통합 트랜잭션), 추가부여(소급+오늘폴백+GRANT_TYPE 자동판단), 회수(A안), 검증(회수가능량=잔여−신청진행분), 처리방식 코드 비활성화, `Attd_09`/`PolicyGrantPreviewPop` 변경, 마이그레이션, 테스트(TC-001~403), 가이드/CHANGELOG/정책 갱신.
- **OUT**: 약정(법정 외) 휴가 수동 조정(기존 Attd_09 manualGrant로 대응), 처리방식 코드 물리 삭제(다음 단계), 별도 회수 이력 테이블(B안), 회계연도 형평성 보전·퇴직정산, 자동 알림 consumer(outbox 적재는 선택), 엑셀 대량 업로드.

## 3. 핵심 코드 / 스키마 위치 (탐색 결과 2026-05-27)
- **FE**:
  - `prafta-web-frontend/prafta-web-frontend/src/views/user/popup/HireDateEditPop.vue` — 처리방식 라디오 460·480~507, 옵션 시뮬 234~265, 영향분석 134~188, 입사일/사유 71~95·363~375, API 호출 649~705(GET `/webApi/user01/{userCd}/hire-date-impact`, POST `/webApi/user01/update-user-hire-date`).
  - `.../views/user/popup/UserInfoPop.vue` — 호출부 785~795(`fnHireDateEditOpen`, `onSaved` 콜백), 경력인정 안내.
  - `.../views/attd/Attd_09.vue` — "정책 기준 부여" 131~148(`fnPolicyGrant` 810~859), 미리보기 호출 831·864.
  - `.../components/popup/PolicyGrantPreviewPop.vue` — 요약/테이블 37~95(reissueCount·처리방식 컬럼 제거 대상).
  - `.../views/attd/popup/LeaveDetailPop.vue`, `LeaveRecallPop.vue` — prafta-031 회수(**불변**).
- **BE**:
  - `prafta-backend/.../web/user/user01/.../User01ServiceImpl.java` — `updateUserHireDate`(408~460, handlingType 검증 426~433 제거 대상).
  - `User01Mapper.xml` — `updateUserHireDate`(526~535), `insertUserHireDateHistory`(537~563).
  - `prafta-backend/.../common/cmm/leave/service/impl/LeaveGrantEngineServiceImpl.java` — `hireDateGrant`(143~213), `buildUserPlan`(617~642, HANDLING_TYPE 분기), `computeBackfillShortfall`(881~887)·`computeNewBasis*Cumulative`, `previewReallocationOptions`(376~477, 옵션 시뮬 제거 대상), `resolveEntitlement`(706~728), `resolveHireDateEntitlement`(731~749), `resolveFiscalEntitlement`(756~814), `computeMonthlyPeriods`(1107~1162, D2-B 만1년 소멸 유지), `tenureBonusDays`(1404~1422), 처리방식 상수(107~110), 멱등키 prefix(`_BF` 85~87).
  - `Attd09ServiceImpl.java` — `policyGrant`(168~183), `previewPolicyGrant`(147~165).
  - `LeaveDashboardServiceImpl.java` — `recallGrant`(405~472, 031 회수), `getDetail`(232~287, legal/nonLegal 부여·사용·잔여), noti outbox(478~517).
  - `LeaveDashboardMapper.xml` — 잔액 집계(262~330), 회수 SQL(459~542), `selectCreditMonths`(649~656).
  - `LeaveGrantEngineMapper.xml` — `selectLatestUnappliedHandling`(14~25), `selectActiveStatutoryGrantIds`(30~41).
- **스키마**(`.claude/context/schema-full.sql`, MCP `prafta-mysql`로 사전 확인):
  - `TB_USER_LEAVE_GRANT`(1104~1134): GRANT_ID varchar20, GRANT_TYPE varchar40, GRANT_DAYS dec(5,1), USED_DAYS dec(5,1), GRANT_REASON varchar500, GRANT_BY_TYPE varchar2(01:AUTO/02:ADMIN), STATUS varchar20(ACTIVE/EXHAUSTED/EXPIRED/CANCELED), AVAIL_FROM/TO_DATE varchar8, IDEMPOTENCY_KEY varchar100, CANCEL_REASON/DATE/BY(prafta-031), DEL_YN.
  - `TB_USER_HIRE_DATE_HISTORY`(1084~1100): HIST_ID varchar20, PREV/NEW_HIRE_DATE varchar8, CHANGE_REASON varchar1000, HANDLING_TYPE varchar30 NOT NULL, AFFECTED_GRANT_SNAPSHOT json, APPLIED_YN/DATE/BY.
  - `TB_USER_LEAVE_USE`(1138~1169): GRANT_ID FK, LEAVE_DAYS dec(5,1), LEAVE_STATUS(CONFIRMED/CANCELLED), START/END_DATE.

## 4. 컨벤션 / 주의
- SQL: leading comma, `#{}` 바인딩, `SELECT *` 금지, 스키마 100% 일치, MCP `prafta-mysql`로 사전 확인.
- DTO 필드명 대문자 SNAKE_CASE, MyBatis column↔property 명시.
- FE: Vue3+JS(TypeScript 금지), scoped CSS + CSS 변수, 공통 컴포넌트 우선.
- 주석/로그 한국어, 식별자 영어. Bash heredoc 금지·비대화형 옵션·타임아웃.
- **잔액/차감 SQL(ACTIVE GRANT_DAYS 합)을 깨지 말 것** — A안이 이를 안 건드리는 게 핵심 이점.
- prafta-030 월차 게이트(`isCreditDoubleDip`)·D2-B 만1년 일괄소멸은 **유지**(Attd_09 신규부여 경로에 그대로 적용).
- 런타임(앱+DB) 시나리오 검증은 사용자 환경 몫. 에이전트는 컴파일 + 결정적 단위테스트(`mockStatic(LocalDate)`)까지.
- **교훈(prafta-030)**: write-heavy 서브에이전트는 실패 테스트를 단정 변경으로 덮을 수 있으니, 변경된 테스트 단정을 메인이 직접 감사.
