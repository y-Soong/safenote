# prafta-app-001 — 앱 메인화면 데이터 동기화 작업 분해

> 요청서: `.claude/requests/app_requests/prafta-app-001.md`
> 대상: `prafta-app-frontend` MainView + 5개 카드, 백엔드 `com.prafta.app` (appApi 프리픽스)
> 분해자: planner / 분해 라운드 (코드 미수정, 명세만)

---

## 0. 사전 조사 요약 (사실 확인된 것만)

### 0-1. 화면 구조
- 메인화면: `src/views/main/MainView.vue` — 5개 자식 카드를 reactive state(`ref`)로 조립.
- 자식 카드(모두 props-only, 자체 API 호출 없음 — 데이터는 부모 MainView 가 주입):
  - `components/HomeHeader.vue` (사업장명 / 알림수 / 아바타 이니셜)
  - `components/AttendanceCard.vue` (출퇴근 상태 / 스케줄·출근 시각 / 위치)
  - `components/AttendanceSummaryCard.vue` (잔여·부여 연차 / 승인대기 건수)
  - `components/SafetyActivityCard.vue` (출근 전 차단 여부만 — MainView computed)
  - `components/TbmAttendCard.vue` (TBM 세션 메타 / 참석 상태)
  - `components/NoticeListCard.vue` (공지 목록 / 미열람 수)
- 현재 데이터 출처: **MainView.vue 의 하드코딩 mock 4케이스**(`CASE1_BEFORE_WORK`~`CASE4_NO_TBM`) + dev 케이스 picker. `onMounted` → `loadMockSummary()` → `applyCase('working')`. 즉 **백엔드 연동 0건**, 전부 mock.
- 코드 주석에 이미 연동 지점이 설계되어 있음: `// TODO(backend-ready): GET /api/app/home/summary` (MainView L142, L365, L373). 단 이 엔드포인트는 **현재 백엔드에 존재하지 않음**(아래 2장).

### 0-2. 세션 클레임 (로그인 후 sessionStorage `gv_*`)
`LoginView.vue` / `App.vue` / `axios.js` / `useAuth.js`(SESSION_KEYS) 정독 결과, 로그인 응답으로 저장되는 비-PII 클레임 12종:

| 키 | 의미 | 비고 |
|---|---|---|
| gv_cmpnyCd | 회사코드 | |
| gv_userCd | 사용자코드 | 서버 조회 키(요청 바디 아님, JWT 클레임으로 전달) |
| gv_userId | 로그인 아이디 | |
| gv_userNm | **사용자 이름** | 헤더 아바타/인사말 소스 |
| gv_siteCd | 사업장코드 | |
| gv_siteNo | 사업장번호 | |
| gv_siteNm | **사업장명** | 헤더 사업장 표시 소스 |
| gv_nodeCd | 소속부서코드 | |
| gv_nodeNm | **부서명** | 부서 표시 필요 시 소스 |
| gv_authCd | 권한코드 | |
| gv_authLevel | 권한레벨 | |
- PII(휴대폰/이메일)는 정책 §11.1 에 따라 세션/스토어에 **없음**.
- 백엔드 app 컨트롤러는 요청 바디로 userCd 를 받지 않고 `jwtUtil.getAllClaimsAsMap(Authorization)` → `TokenInfo` 로 회사/사용자/사업장을 도출(AppChkLst01Controller / AppRisk01Controller 확인). **신규 조회 API 도 동일 패턴**을 따라야 함(userCd 를 쿼리로 받지 않음).

### 0-3. 백엔드 현황 (appApi)
- 프리픽스 자동등록: `com.prafta.app.*` → `/prafta/appApi/...` (ApiPrefixConfig 확인). 컨트롤러 `@RequestMapping`은 모듈 경로만(`/chkLst01` 등), 프리픽스는 자동 부착.
- **현존 app 컨트롤러는 단 2개**: `AppChkLst01Controller`, `AppRisk01Controller`. 메인화면용 조회 API(홈 요약 / 오늘 근태 / 연차요약 / TBM / 공지) **전부 없음** → 신규 필요.
- 출퇴근(check-in/check-out) 앱 엔드포인트 **없음** (`check-in`/`check-out` grep 결과는 전부 TBM 참석 또는 web 근태보정 코드).

### 0-4. DB 스키마 (schema-full.sql 스냅샷 기준 — 운영 검증 필요 표기)
| 항목 | 테이블 | 핵심 컬럼 | 상태 |
|---|---|---|---|
| 일별 출퇴근 기록 | `tb_user_attd_mgmt` | WORK_YMD, WORK_SEQ, CHECK_IN_DATE, CHECK_IN_TIME(varchar4 HHMM), CHECK_OUT_TIME, CHECK_*_METHOD[SYS031], NODE_CD, DEL_YN | 확인됨 |
| 사용자 근무계획 | `tb_user_work_plan` | WORK_YMD, WORK_PLAN_CD([SCH_CD 또는 LEAVE_CD]) | 확인됨 |
| 스케줄(시각 정의) | `tb_sch_mgmt` | SCH_CD, FST_SCH_STR_TIME / FST_SCH_END_TIME(1구간), SEC_SCH_*(2구간), APPLY_DATE, USE_YN | 확인됨 |
| 연차 부여 | `tb_user_leave_grant` | LEAVE_CD, GRANT_DAYS, USED_DAYS, GRANT_TYPE[SYS035 STATUTORY_*/MANUAL_*], STATUS[SYS040], AVAIL_TO_DATE, EXPIRE_YN | 확인됨 |
| 근태 요청 | `tb_user_attd_req` | REQ_TYPE[SYS032], REQ_STATUS[SYS033 01신청..], USER_CD | 확인됨 |
| GPS | `tb_user_attd_gps` | (컬럼 미정독 — 지오펜스 작업 시 추가 확인) | 미정독 |
| **공지사항** | **없음** | — | ⚠️ schema-full.sql 에 `tb_notice` 류 테이블 **부재** |
| **TBM 세션/참석** | **없음** (edu_mtrl 만 존재) | `tb_tbm_edu_mtrl`(교육자료)뿐, 세션/참석 테이블 없음 | ⚠️ |

> ⚠️ schema-full.sql 은 스냅샷이라 최신이 아닐 수 있음(스냅샷에 tb_attd 류 일부 누락 관측됨). **공지/TBM 테이블 부재 여부는 MCP `prafta-mysql` 로 운영 스키마 재확인 필요**(질문 Q5/Q6).

---

## 1. Mock 항목 전수 표

소스 위치는 `src/views/main/` 기준. "현재값"은 기본 적용 케이스(`CASE2_WORKING`) 값.

| # | 항목 | 현재 하드코딩값 | 소스 위치(파일:라인) | 연동 소스 | 필요 작업 |
|---|---|---|---|---|---|
| H1 | 사업장명(헤더) | `'중곡사업장'` | MainView.vue:144(siteName), :238 / HomeHeader.vue:13 | **세션** `gv_siteNm` | 세션값 바인딩 |
| H2 | 알림 미확인 수 | `3` | MainView.vue:145, :237 | **API**(공지/알림 집계) | 신규 API (Q5) |
| H3 | 사용자 이니셜(아바타) | `'김민'` | MainView.vue:146, :239 / HomeHeader.vue:37 | **세션** `gv_userNm` 가공(앞 2자) | 세션값에서 파생 (Q1) |
| G1 | 인사말 날짜 | `new Date()` 클라이언트 로컬 | MainView.vue:151-156 | (클라이언트) | 유지 가능 / 서버시각 여부 Q7 |
| G2 | 인사말 문구 | `'오늘도 좋은 하루 되세요'` | MainView.vue:33 | (정적) | 유지(정책 7.7 미확정) |
| A1 | 출퇴근 상태 | `'WORKING'` | MainView.vue:161, :240 | **API** 오늘근태(tb_user_attd_mgmt 유무/퇴근여부) | 신규 API |
| A2 | 사업장 외 여부 | `false` | MainView.vue:162, :241 | **API** GPS 판정(tb_user_attd_gps / 지오펜스) | 신규 API (Q3) |
| A3 | 스케줄 시작시각 | `'0930'` | MainView.vue:163, :242 | **API** tb_user_work_plan→tb_sch_mgmt.FST_SCH_STR_TIME | 신규 API |
| A4 | 스케줄 종료시각 | `'1800'` | MainView.vue:164, :243 | **API** tb_sch_mgmt.FST_SCH_END_TIME(2구간 처리 Q4) | 신규 API |
| A5 | 실제 출근시각 | `'0928'` | MainView.vue:165, :244 | **API** tb_user_attd_mgmt.CHECK_IN_TIME | 신규 API |
| A6 | 출근 가능 여부 | `false` | MainView.vue:166, :245 | **API**(서버 산출, 출퇴근 횟수/구간 정책 §5) | 신규 API |
| A7 | 퇴근 가능 여부 | `true` | MainView.vue:167, :246 | **API**(서버 산출) | 신규 API |
| A8 | 위치표시 사업장명 | siteName 재사용 | AttendanceCard.vue:135-139 | **세션** `gv_siteNm`(A1과 공유) | 세션값 바인딩 |
| S1 | 잔여 연차 | `12` | MainView.vue:172, :247 | **API** SUM(GRANT_DAYS-USED_DAYS) ACTIVE | 신규 API (Q2) |
| S2 | 부여 연차 | `20` | MainView.vue:173, :248 | **API** SUM(GRANT_DAYS) | 신규 API (Q2) |
| S3 | 승인대기 건수 | `3` | MainView.vue:174, :249 | **API** tb_user_attd_req REQ_STATUS='01'(신청) 본인건 | 신규 API (Q8: 상태코드) |
| F1 | 안전활동 차단 | computed(=출근전) | MainView.vue:180 | (A1에서 파생) | A1 연동되면 자동 |
| T1 | TBM 상태 | `'AVAILABLE'` | MainView.vue:185, :250 | **API** TBM 세션/참석 | ⚠️ 테이블 부재 (Q6) |
| T2 | TBM 세션시각 | `'1000'` | MainView.vue:186, :251 | **API** TBM 세션 | ⚠️ (Q6) |
| T3 | TBM 장소 | `'1구역 회의실'` | MainView.vue:187, :252 | **API** TBM 세션 | ⚠️ (Q6) |
| T4 | TBM 진행자 | `'안전팀 박과장'` | MainView.vue:188, :253 | **API** TBM 세션 | ⚠️ (Q6) |
| T5 | TBM 참석시각 | `''`/`'1008'` | MainView.vue:189, :282 | **API** TBM 참석 | ⚠️ (Q6) |
| N1 | 공지 목록(3행) | `[N123,N122,N121]` 더미 | MainView.vue:228-232 등 | **API** 공지 | ⚠️ 테이블 부재 (Q5) |
| N2 | 공지 미열람 수 | `2` | MainView.vue:227, :255 | **API** 공지 | ⚠️ (Q5) |
| TB1 | 하단탭 TBM 뱃지 | computed(=AVAILABLE) | MainView.vue:200 | (T1에서 파생) | T1 연동되면 자동 |
| D0 | dev 케이스 picker 전체 | mock 토글 UI | MainView.vue:100-112, 320-369, 594-637(style) | (제거 대상) | 연동 후 삭제 |

### 연동 소스 분류 집계
- **세션(gv_*)만으로 즉시 가능**: H1(siteNm), H3(userNm→이니셜), A8(siteNm). → 백엔드 불필요.
- **백엔드 조회 API 필요(데이터 존재)**: A1,A3,A4,A5,A6,A7, S1,S2,S3. → 테이블 확인됨, 신규 조회 API 작성.
- **백엔드 미확정(테이블 부재 의심)**: H2(알림수), N1,N2(공지), T1~T5(TBM). → 질문 선결 전 착수 불가.

---

## 2. 신규 백엔드 API 필요 여부 목록

> 모든 신규 app 조회 API 는 기존 패턴(AppChkLst01Controller) 준수: `com.prafta.app.{module}` 패키지(→ `/prafta/appApi` 자동 프리픽스), `@RequestHeader Authorization` → `jwtUtil.getAllClaimsAsMap` → `TokenInfo`(cmpny/site/user 도출), userCd 를 쿼리/바디로 받지 않음.

| API | 메서드/경로(제안) | 데이터 출처 | 응답 필드(제안) | 상태 |
|---|---|---|---|---|
| 홈 요약(통합) | `GET /appApi/home/home-summary` (kebab) | 아래 묶음 | site/user(세션 중복분은 제외 가능), attendance{status,scheduleStart,scheduleEnd,checkIn,isOffsite,canCheckIn,canCheckOut}, leave{remaining,granted}, approval{pendingCount} | **신규 필요** — 핵심 |
| 오늘 근태 단건 | (홈 요약에 포함 권장) | tb_user_attd_mgmt(오늘 WORK_YMD), tb_user_work_plan+tb_sch_mgmt | A1,A3,A4,A5,A6,A7 | **신규 필요** |
| 연차 요약 | (홈 요약에 포함 권장) | tb_user_leave_grant | S1,S2 | **신규 필요** |
| 승인대기 건수 | (홈 요약에 포함 권장) | tb_user_attd_req(REQ_STATUS 신청) | S3 | **신규 필요** |
| 공지 목록/미열람 | `GET /appApi/notice/notice-lists` 류 | **테이블 미확인** | N1,N2,H2 | **보류** — 테이블 확인 선결(Q5) |
| TBM 오늘 세션/참석 | `GET /appApi/tbm/today-session` 류 | **세션/참석 테이블 부재 의심** | T1~T5,TB1 | **보류** — 데이터 모델 선결(Q6) |
| 출근 처리 | `POST /appApi/attd/check-in` | tb_user_attd_mgmt INSERT + GPS | — | **본 작업 범위 밖**(요청서는 "표시 동기화"). 별도 작업 |
| 퇴근 처리 | `POST /appApi/attd/check-out` | tb_user_attd_mgmt UPDATE | — | **본 작업 범위 밖** |

> 설계 권고: 메인화면은 다카드 동시표시이므로 **단일 `home-summary` 엔드포인트로 묶는 것을 1순위 제안**(라운드트립 최소화, MainView 의 기존 `GET /api/app/home/summary` TODO 와 정합). 공지/TBM 은 데이터 모델 미확정이므로 summary 에서 분리하거나 nullable 로 둔다(Q9).

---

## 3. developer 착수 작업 단위

### [확정 착수 가능 — 질문 불요]
- **APP-FE-1 (frontend)**: 세션 클레임 직결 항목 연동.
  - H1/A8(사업장명 ← `gv_siteNm`), H3(아바타 이니셜 ← `gv_userNm` 앞 2자, 규칙 Q1 확정 시).
  - MainView 의 `siteName`/`userInitial` 초기화를 sessionStorage(or userStore) 에서 채우도록 변경.
  - 카드 컴포넌트 수정 불요(props 그대로). MainView script 만 수정.

### [백엔드 선행 필요 — 데이터 확인됨]
- **APP-BE-1 (backend)**: `GET /appApi/home/home-summary` 신규.
  - 패키지 신설 `com.prafta.app.home.home01`(controller/service/mapper/dto). 기존 app 패턴 준수.
  - 오늘 근태(tb_user_attd_mgmt) + 근무계획/스케줄(tb_user_work_plan→tb_sch_mgmt) + 연차(tb_user_leave_grant) + 승인대기(tb_user_attd_req) 조회.
  - 정책 출처: attd `07-checkin-checkout.md` §7.1(출퇴근 기본), `05-checkin-limits.md` §5.1~5.3(출퇴근 가능 여부 산출), `08-leave.md` §8.1(연차), `06-schedule.md` §6.6(2구간 처리), 요청승인 재기획서(승인대기 정의).
  - canCheckIn/canCheckOut 는 **서버 산출**(클라 판정 금지).
- **APP-FE-2 (frontend)**: APP-BE-1 응답으로 A1~A7, S1~S3, F1(파생) 연동 + mock 케이스/dev picker 제거.
  - 선행: APP-BE-1. MainView script 만 수정(`onMounted` 에서 `api.get('/appApi/home/home-summary')`).

### [보류 — 질문 선결 후 분해]
- **APP-?-3 공지(H2/N1/N2)**: 공지 테이블/도메인 부재. Q5 답변 후 별도 분해.
- **APP-?-4 TBM(T1~T5/TB1)**: TBM 세션·참석 데이터 모델 부재. Q6 답변 후 별도 분해. (메모리상 TBM 실시간/앱 파트는 prafta-033 에서 "앱 이후 보류"로 분류된 이력 있음 — 정합 확인 필요)

### 우선순위
1. APP-FE-1 (세션 직결, 의존 없음, 즉시)
2. APP-BE-1 (근태 — 법적 책임 영역 attd, +1 격상)
3. APP-FE-2 (BE-1 의존)
4. 공지/TBM 은 질문 해소 후

---

## 4. 사용자 확인 필요 질문 목록

- **Q1 (아바타 이니셜 규칙)**: 헤더 아바타가 mock 에서 `'김민'`(2자)임. `gv_userNm` 에서 **이름 앞 2자**를 자르는 규칙으로 확정해도 되는지? (예: '김민수'→'김민'). 영문/한자/1자 이름 폴백 규칙은? (현재 컴포넌트는 빈값이면 '?' 폴백)

- **Q2 (잔여 연차 정의)**: 카드의 "잔여/부여"는 `AttendanceSummaryCard` 주석상 "통합 표시". 다음 중 무엇인가?
  - (a) **법정 연차만**(STATUTORY_*) 합산? (메모리 prafta-032: 회수가능량=ACTIVE 법정 SUM)
  - (b) 법정+약정(MANUAL_*) 전체 합산?
  - "부여"=`SUM(GRANT_DAYS)`, "잔여"=`SUM(GRANT_DAYS-USED_DAYS)` 이며 **STATUS='ACTIVE' & EXPIRE_YN='N'** 만 집계가 맞는지?

- **Q3 (사업장 외 여부 A2/isOffsite)**: 출근 전(BEFORE_WORK)에는 위치판정이 의미없음. "근무중"일 때 isOffsite 는 **마지막 출근 시점의 지오펜스 판정 결과를 저장해 둔 값**인가, 아니면 **조회 시점 실시간 GPS 재판정**인가? 후자면 앱이 GPS 를 summary 호출에 같이 보내야 함(엔드포인트 설계 영향). 정책 `07-checkin-checkout.md` §7.2/§7.3 범위.

- **Q4 (2구간 스케줄)**: tb_sch_mgmt 는 1·2구간(FST/SEC)을 가짐. 메인 출퇴근 카드의 "예정 HH:MM~HH:MM" 은 **1구간만** 표시하는지, 2구간이 있으면 어떻게 표기하는지? (AttendanceCard 주석에 "2구간 정책 7.3 보완 필요" 명시됨)

- **Q5 (공지 도메인)**: schema-full.sql 에 **공지사항 테이블이 없음**. (a) 공지 기능 자체가 아직 미구축이라 이번 라운드는 **공지 카드를 빈/숨김 처리**할지, (b) 별도 공지 테이블/API 가 다른 곳에 이미 있는지(운영 DB MCP 재확인 필요), (c) 알림 벨 카운트(H2)도 공지/알림 도메인 확정 전까지 0 고정할지?

- **Q6 (TBM 도메인)**: TBM 관련 테이블은 교육자료(`tb_tbm_edu_mtrl`)뿐이고 **TBM 세션/참석 테이블이 없음**. 메모리상 앱 TBM 은 prafta-033 에서 보류된 이력이 있음. 이번 라운드에서 TBM 카드는 (a) mock 유지 / (b) "오늘 TBM 없음(NONE)" 고정 / (c) 별도 작업으로 분리 중 무엇으로?

- **Q7 (인사말 날짜 G1)**: 현재 클라이언트 로컬시각. 디바이스 시계 조작 가능성 고려해 **서버 시각 기준**으로 바꿔야 하는지(=summary 응답에 serverDate 포함), 아니면 클라이언트 로컬로 유지 가능?

- **Q8 / Q3 코드값 확인 (승인대기 S3)**: tb_user_attd_req.REQ_STATUS 는 schema 주석상 SYS033 "01신청/02승인/03반려/04취소". 카드 컴포넌트 주석은 `REQ_STATUS='REQUESTED'` 라 표기(문자열). **실제 신청대기 상태코드가 '01' 인지 'REQUESTED' 인지** 코드/스키마 불일치 — 어느 쪽이 사실인지 확정 필요(developer 가 매핑 전 반드시 확인).

- **Q9 (엔드포인트 묶음 형태)**: home-summary 를 **단일 통합 엔드포인트**로 만들지(권장), 아니면 attendance/leave/approval 을 각각 분리할지? 통합 시 공지/TBM(미확정)을 응답에서 제외(또는 null)할지?

- **Q10 (범위 경계)**: 요청서는 "표시 데이터 동기화"가 골자. 출근/퇴근 **버튼 동작(POST check-in/out)** 은 이번 작업 범위 밖(별도 작업)으로 두는 게 맞는지 확인. (현재 버튼은 "준비 중입니다" alert)

---

## 5. 비고
- 본 분해는 코드 미수정(명세만). Vue 골격 신규 작성 없음 — 기존 MainView/카드 구조 재사용, 변경은 MainView `<script>` 의 데이터 주입부 + 신규 백엔드 모듈.
- schema-full.sql 은 스냅샷. 공지/TBM 테이블 부재는 **MCP `prafta-mysql` 로 운영 스키마 재확인**이 정확(Q5/Q6 선결 시 동반 확인 권장).
- Notion 등록은 메인 세션이 대행.
