# PRAFTA-app-012 — 아차사고/사건 보고 도메인 (앱 파트) 작업 분해 및 plan

> 단일 출처(plan). 영역: app (`prafta-app-frontend` webview Vue + `PRAFTA_FLUTTER/safenote` 셸).
> 설계 출처(단일): `.claude/context/near-miss-incident-design.md` (§3 채널매트릭스, §4-A/4-B, §5-A/5-B, §6 API, §7 결정필요).
> 웹 파트(공유 자원): `.claude/requests/web_requests/prafta-040.md` + `prafta-040-near-miss.sql`(테이블/코드/메뉴/권한 — **이미 코드/마이그 작성 완료**).
> 작성: planner. 상태: 분해완료(미구현). 후속: developer(BE+FE script) → qa → security.
>
> **핵심 원칙(app-010 기조)**: 앱 백엔드 완전 분리. 앱 화면은 `/appApi/nearmiss/*`(신규 `com.prafta.app.nearmiss.nearmiss01`)만 호출. web 컨트롤러(`/webApi/nearmiss01/*`) 호출 절대 금지. 동일 테이블(`tb_near_miss`)을 보더라도 mapper/service는 앱 패키지에 별도 구현.

---

## 0. 한눈에 보기 — 확정안

| 항목 | 확정 값 | 근거 |
|---|---|---|
| 앱 BE 패키지 | `com.prafta.app.nearmiss.nearmiss01` | app risk01/tbm01 레이어링 동일 |
| 앱 BE 컨트롤러 base | `@RequestMapping("/nearmiss")` → `/appApi/nearmiss/...` | app-010 / ApiPrefixConfig 자동 `/appApi` |
| 공유 테이블 | `tb_near_miss` (PK CMPNY_CD,SITE_CD,NEAR_MISS_ID) | prafta-040-near-miss.sql (작성 완료) |
| 공유 코드 | SYS061(사건유형)/SYS062(잠재중대성)/SYS063(처리상태) | prafta-040 (작성 완료) |
| 채번 | `NM + YYYYMMDD + 3자리 SEQ` (사업장+당일 MAX+1) | 웹 `selectNextNearMissId` 동일 SQL 재사용 |
| 사진 첨부 | `tb_file_info` **단일 파일**(FILE_MGMT_CD 1개=1장). FILE_TYPE 신규 '004'(아차사고) | app risk01 multipart 패턴, SYS010 |
| 권한 가드 | 근로자=본인 사업장(JWT siteCd) INSERT / 관리자=`assertSiteAccess`(master·hr 전사 + `tb_user_site_auth`) | 웹 `NearMiss01ServiceImpl.assertSiteAccess` 동일 |
| 관리자 조치범위 | **상태전환(100→200) + 임시조치 메모**까지만 (정밀조사=웹) | 설계 §7-1 권고, D1 확정 반영 |
| 임시조치 메모 컬럼 | **신규 `ADMIN_TEMP_ACTION_DESC varchar(500)`** (보고자 IMMEDIATE_ACTION_DESC 와 분리) | 결정필요 D-A1 (권고안 전제) |
| 푸시 | `tb_noti_outbox` 재사용 + SYS045 신규 `NEAR_MISS_REPORTED`. 잠재중대성≥중대(200/300) 신규보고 시 안전관리자 PENDING INSERT | prafta-031 인프라, consumer 미구현 |
| 진입 동선 | 근로자=메인 `SafetyActivityCard` 3분할로 "아차사고 보고" 추가 / 관리자="사건 관리" 별도 진입(메인 신규 진입점) | 결정필요 D-A5 (제안) |
| 보존 | 물리삭제 금지(USE_YN) | 설계 §7-5 |

---

## 1. 작업 단위 분해표 (의존순서)

| # | 작업ID | 유형 | 모듈 | 작업유형 | 요약 | 선행 |
|---|---|---|---|---|---|---|
| 1 | PRAFTA-app-012-1 | backend | nearmiss/migration | 신규 | 마이그레이션(SYS045 NEAR_MISS_REPORTED + tb_near_miss ALTER ADMIN_TEMP_ACTION_DESC + FILE_TYPE '004') | 040-1 |
| 2 | PRAFTA-app-012-2 | backend | app/nearmiss/nearmiss01 | 신규 | 보고등록(multipart)·내보고목록·사업장목록·상세·상태전환 API + 푸시 outbox INSERT (앱 패키지) | 012-1 |
| 3 | PRAFTA-app-012-3 | frontend-screen | nearmiss | 신규 | NearMissReport.vue 근로자 아차사고 보고 화면 (설계 5-A) | 012-2 |
| 4 | PRAFTA-app-012-4 | frontend-screen | nearmiss | 신규 | NearMissManageList.vue 관리자 사업장 사건 목록(상태탭/배지) | 012-2 |
| 5 | PRAFTA-app-012-5 | frontend-screen | nearmiss | 신규 | NearMissManageDetail.vue 관리자 1차확인 상세(임시조치+상태전환) | 012-2, 012-4 |

> 5개 한도 충족. mixed 없음(BE/FE 분할 완료).
> **부수 변경(012-3/4/5 와 함께 developer 처리, 별도 작업ID 미부여)**: ① `src/router/index.js` 라우트 3개 추가 ② 메인 `MainView`/`SafetyActivityCard` 진입점 추가(D-A5 확정 후). 라우트/진입점은 화면 작업의 종속 변경이라 골격 단계에서 TODO 마커로 표기.
> 012-1 은 운영 미적용(파일만). DB 적용은 사용자 수동(read-only MCP).

---

## 2. 정책서/설계 출처 매핑

| 요구사항 | 출처 |
|---|---|
| 아차사고는 위험성평가와 분리된 별도 '사건' 도메인 | 설계 §1 (산안법 §36 수시 위험성평가 / §57 산업재해 / 중처법 §4·시행령 §4) |
| 앱 채널: 근로자 보고 + 관리자 1차확인(읽기+상태전환+짧은 메모) | 설계 §3 채널매트릭스(하이브리드), §4-A 1~3단계 |
| 보고 INSERT(REPORT_STATUS_CD='100', REPORTER_ID=본인, SRC_*=NULL) | 설계 §4-A 1단계 |
| 상태 전이 100→200(앱/웹), 200→300→400(웹), 900 반려 | 설계 §4 상태전이 다이어그램, §7-1 |
| 잠재중대성≥중대 신규보고 시 안전관리자 푸시 / 경미는 배지만 | 설계 §4-A 2단계, §3 매트릭스 푸시 행 |
| 정밀조사(원인·재발방지)·완결·통계는 웹 전용(앱 비범위) | 설계 §3, §6, §7-1 |
| 사업장 스코프·관리노드 IDOR 가드 | 설계 §6, 공통 정책서 §6/§8 |
| 보존(물리삭제 금지, USE_YN) | 설계 §7-5, 공통 정책서 §11 |
| 앱 백엔드 완전 분리(web 호출 금지) | app-010 기조 / MEMORY project_prafta_app_vite_and_api_align |
| 푸시 outbox 재사용(신규 알림 테이블 금지) | MEMORY project_prafta_031_recall_and_outbox |

> 별도 "아차사고 정책서" 섹션은 `.claude/context/policies/` 에 없음 → 설계문서가 법적 근거를 정리한 단일 출처(README 우선순위 4번 기술정책서보다 본 작업의 단일 출처로 인용).

---

## 3. PRAFTA-app-012-1 — 마이그레이션 (운영 미적용, 파일만)

산출물(3개 파일):
- `prafta-backend/src/main/resources/sql/migration/prafta-app-012-sys045-near-miss.sql` — SYS045 `NEAR_MISS_REPORTED` 디테일 1건 추가.
- `prafta-backend/src/main/resources/sql/migration/prafta-app-012-near-miss-admin-action.sql` — `tb_near_miss` 에 `ADMIN_TEMP_ACTION_DESC` 컬럼 ALTER ADD (D-A1 권고안).
- `prafta-backend/src/main/resources/sql/migration/prafta-app-012-sys010-filetype.sql` — SYS010(FILE_TYPE)에 '004'(아차사고) 디테일 추가. **단, SYS010 현행 코드값 미확인 → developer 가 MCP 로 SYS010 확인 후 값/존재여부 확정.** (risk01 은 '002' 위험성평가 사용. chkLst/tbm 코드값은 developer 확인.)

> ⚠️ FILE_TYPE 코드값은 schema 스냅샷 미확인 영역 → **결정필요 D-A3**. 1차는 신규 '004' 가정. 이미 적합한 코드가 있으면 신규 INSERT 생략하고 그 코드 재사용.
> 멱등: PK/UK 중복 시 에러. 운영 적용 후 보관용.

---

## 4. PRAFTA-app-012-2 — 앱 백엔드 API (`com.prafta.app.nearmiss.nearmiss01`)

레이어링은 `app.risk.risk01` / `app.tbm.tbm01` 동일: controller / service(+impl) / mapper(+XML) / dto(request·response) / result / application(param·query·command).

컨트롤러 base: `@RequestMapping("/nearmiss")`. 최종 URL `/appApi/nearmiss/...` (ApiPrefixConfig 자동).
식별자(cmpnyCd/siteCd/userCd/authCd)는 **JWT 클레임에서만** 도출(`jwtUtil.getAllClaimsAsMap(authorization)` → Param.from). 요청 바디/파라미터의 식별자는 신뢰하지 않음(IDOR 차단). multipart 파일 업로드는 risk01/tbm01 의 `@ModelAttribute + @RequestPart(value="item")` 단일 파일 패턴 동일.

### 4.1 엔드포인트 명세

| # | Method | Path (`/appApi` 생략) | 용도 | 권한 | 요청(주요, 식별자 제외) | 응답(주요) |
|---|---|---|---|---|---|---|
| A1 | POST | `/nearmiss/report` (multipart/form-data) | 근로자 아차사고 보고 등록 | 로그인 사용자(본인 사업장) | incidentTypeCd, processCd?, occurDtime('YYYY-MM-DD HH:mm'), locationDesc?, description(필수), potentialSeverityCd?, immediateActionDesc?, item(파일 단일·선택) | `{ nearMissId }` |
| A2 | GET | `/nearmiss/my-reports` | 내 보고 목록 | 로그인 사용자 | reportStatusCd?, (선택)limit/offset | `{ incidentResultList[] }` |
| A3 | GET | `/nearmiss/site-incidents` | 관리자 사업장 사건 목록 | `assertSiteAccess` | reportStatusCd?, incidentTypeCd?, potentialSeverityCd?, startDate?, endDate? | `{ incidentResultList[] }` |
| A4 | GET | `/nearmiss/status-counts` | 관리자 상태 카운트(탭 배지) | `assertSiteAccess` | (목록 필터 동일) | `{ statusCount:{receivedCnt,reviewingCnt,actingCnt,completedCnt} }` |
| A5 | GET | `/nearmiss/detail` | 사건 단건 상세 | `assertSiteAccess`(관리자) / 본인 보고건은 보고자 허용 | nearMissId | `{ incidentInfo }` (헤더 전 컬럼 + 코드명 + 파일경로 + ADMIN_TEMP_ACTION_DESC) |
| A6 | POST | `/nearmiss/change-status` (json) | 1차 확인: 100→200 전환 + 임시조치 메모 | `assertSiteAccess` | nearMissId, reportStatusCd('200' or '900'), adminTempActionDesc?, rejectReason?(900 시 필수) | 200 |

> **앱 관리자 조치범위 한정(D1=전자)**: A6 의 정방향 전이는 **100→200 만 허용**(웹은 200→300→400). 앱에서 300/400 전이 요청이 오면 422. 반려(900)는 어느 단계든 사유 필수(웹 동일 정책). adminTempActionDesc 는 전이와 함께 `ADMIN_TEMP_ACTION_DESC` 에 기록(보고자 IMMEDIATE_ACTION_DESC 불변).
> A5 권한: 관리자는 `assertSiteAccess`. 추가로 "내 보고 상세"를 근로자가 보려면 `REPORTER_ID = JWT userCd` 조건 OR 관리자. developer 가 두 경로(my vs site) 분기 — 권고: A5 는 관리자 전용으로 두고, 근로자 목록 A2 는 상세 진입 없이 카드 요약만(또는 A5 에 reporter 셀프 허용). **결정필요 D-A2**.
> A1 보고는 `REPORT_STATUS_CD='100'`, `REPORTER_ID=JWT userCd`, `SITE_CD=JWT siteCd`, `SRC_*=NULL` 고정. 채번은 웹 `selectNextNearMissId` SQL 동일.

### 4.2 푸시 outbox INSERT 로직 (A1 보고 등록 트랜잭션 내)

- 조건: `potentialSeverityCd IN ('200','300')` (중대/치명) 인 신규 보고만.
- 대상: 보고 사업장(SITE_CD)의 안전관리자. **대상 사용자 도출 쿼리 미확정** → developer 가 `tb_user_site_auth` + 안전직군(AUTH_CD safe 등) 조인으로 SITE 안전관리자 USER_CD 목록 SELECT 후 각자에게 1행 INSERT. (웹 prafta-031 은 단일 대상이라 직접 참고 한계 → 대상 해석 쿼리는 신규 작성, **결정필요 D-A4**.)
- INSERT 컬럼: NOTI_ID(채번 N+YYYYMMDD+SEQ, prafta-031 패턴), CMPNY_CD, SITE_CD, TARGET_USER_CD, NOTI_TYPE='NEAR_MISS_REPORTED', CHANNEL='PUSH', TITLE/BODY(예: "[사업장명] 중대 잠재 아차사고 접수"), DATA_PAYLOAD(json: {nearMissId, incidentTypeCd, potentialSeverityCd}), SEND_STATUS='PENDING', DEDUP_KEY('NEARMISS_REPORTED_'+nearMissId+'_'+targetUserCd), INSERT_NO=JWT userCd.
- **consumer 미구현**: INSERT 까지만(실발송 미연동). 트랜잭션은 보고 INSERT 와 동일 단위(@Transactional rollbackFor). 푸시 대상 0명이어도 보고는 성공.

### 4.3 DTO/Result 필드 (tb_near_miss 컬럼 기준, camelCase)

result(`IncidentResult`): cmpnyCd, siteCd, nearMissId, incidentTypeCd, incidentTypeNm, processCd, processNm, occurDtime, locationDesc, description, potentialSeverityCd, potentialSeverityNm, immediateActionDesc, adminTempActionDesc, causeDesc, preventionDesc, fileMgmtCd, fileName, filePath, reportStatusCd, reportStatusNm, reporterId, reporterNm, reportDtime, reviewerId, reviewerNm, reviewDtime, rejectReason, useYn.

> 코드명/공정명/보고자명/파일경로는 웹 `incidentColumns` 의 `FNC_CMM_INFO_SRCH(...)` 해석 패턴 그대로 앱 mapper 에 복제(SYS061/062/063, COM002 공정, USER_NM, FILE_NAME/FILE_PATH).

### 4.4 mapper XML

위치: `prafta-backend/src/main/resources/com/prafta/app/nearmiss/nearmiss01/mapper/AppNearMiss01Mapper.xml`.
SQL 규칙: leading 콤마, `#{}` 바인딩, `SELECT *` 금지, 스키마 100% 일치, 사업장 스코프 WHERE 필수, ADMIN_TEMP_ACTION_DESC 포함.
재사용 쿼리(웹에서 복제): `countUserSiteAuth`, `selectNextNearMissId`, `selectIncidentList`(my=REPORTER_ID 조건 / site=SITE 조건), `selectStatusCounts`, `selectReportStatus`, `selectIncidentInfo`. 신규: `insertReport`(보고자 입력 컬럼만), `updateFirstReview`(100→200 + ADMIN_TEMP_ACTION_DESC + REVIEWER_ID/REVIEW_DTIME), `updateReject`(900+REJECT_REASON), 푸시 대상 `selectSiteSafetyManagers`, `selectNextNotiId`, `insertNotiOutbox`.

### 4.5 상태전환 검증 로직 (앱 한정)

웹 `NearMiss01ServiceImpl.changeStatus` 와 동형이되 **FORWARD_TRANSITION 을 {100→200} 만**으로 제한. 200/300 → 다음 단계 전이 요청은 422(앱 비허용). 반려(900)는 사유 필수. 에러코드는 신규 `NearMissErrorCode`(앱 공용) 또는 web 의 `com.prafta.common.error.nearmiss.NearMissErrorCode` 재사용(common 패키지라 app 도 import 가능 — 재사용 권장).

---

## 5. 화면 (PRAFTA-app-012-3/4/5) — UI 명세는 `prafta-app-012-ui-spec.md` 참조

- 012-3 `NearMissReport.vue`(근로자 보고): 유형 라디오(SYS061)·발생일시·장소·경위(필수)·잠재중대성 라디오(SYS062)·사진 단일 첨부(카메라/갤러리)·즉시조치 → [보고하기]. multipart 로 A1 호출. MainView/MyAttendance 디자인 토큰 + scoped + `@/api/axios`.
- 012-4 `NearMissManageList.vue`(관리자 목록): 사업장 스코프 + 상태탭 카운트(접수/검토중/조치중) + 잠재중대성 배지 + 행→상세. A3/A4 호출. MyRequestsView 리스트+필터 패턴.
- 012-5 `NearMissManageDetail.vue`(관리자 1차확인): 보고내용 읽기 + 임시조치 메모 입력 + [접수→검토중(200)] + [반려]. A5/A6 호출. SafetyInspectSavedView 푸터 버튼 패턴.

Vue 골격(template+style)은 본 작업과 함께 디스크에 작성됨. script 는 `// TODO(developer):` 마커만(한국어).

---

## 6. 진입 동선 (제안 — D-A5 확정 대상)

- **근로자**: 메인 `SafetyActivityCard` 는 현재 2분할("안전점검 시작"/"위험성 발굴"). 여기에 "아차사고 보고"를 **3번째 버튼**으로 추가(3분할 그리드) → `router.push('/NearMissReport')`. 안전 활동 카드의 `blocked`(출근 전 차단) 정책을 아차사고에도 적용할지 확인 필요(아차사고는 "지금 위험을 봤다" 즉시성이라 출근 전에도 허용이 합리적일 수 있음). **결정필요 D-A5**.
- **관리자**: 안전관리자 전용 진입점. 후보 ① 메인 신규 "사건 관리" 카드/아이콘 ② 마이페이지/관리 메뉴. 관리자 여부는 세션 `gv_authCd`(safe/master 등)로 게이팅. 앱은 메뉴 동적 라우트(`app-menu-lists`) 기반이라 nearMiss 웹 메뉴(`MENU_SRC='001'`)는 앱에 안 뜸 → **앱 진입점은 화면 코드에 직접 추가**해야 함. 위치/노출 권한은 D-A5 로 확인.

> 진입점 추가는 012-3/4/5 의 종속 변경(별도 작업ID 미부여). 골격에는 진입 라우트만 router 에 추가하고, MainView 진입 버튼은 D-A5 확정 후 developer 가 반영(골격에 TODO 표기).

---

## 7. Flutter 셸 연동 (구현은 developer/별도)

상세는 본 plan 과 함께 작성된 "Flutter 연동 메모"(본 문서 §10) 참조. 요약:
- 카메라/갤러리 사진은 risk01 이 이미 쓰는 **웹뷰 표준 `<input type="file" accept="image/*">`** 로 동작(네이티브 브리지 불요). 신규 브리지 없음.
- 위치는 발생장소 보조(선택). 1차 범위에서는 텍스트 직접 입력만(GPS 자동기입 미포함) → 신규 권한/브리지 불요.
- 결론: **Flutter 신규 변경 없음**(기존 카메라/저장소 권한으로 충분). 권한 prompt 사전 안내만.

---

## 8. 우선순위 근거

- 법적 책임 영역(산안법/중처법 안전 사건 기록) → +1 격상. PII 직접 처리는 없음(보고자=세션 userCd, 평문 PII 미저장).
- 의존: 012-1(마이그) → 012-2(BE) → 012-3/4/5(FE). 012-5 는 012-4(목록 진입) 후.
- 웹(prafta-040)은 이미 코드/마이그 완료 상태이므로 공유 테이블/코드/SEQ/가드 패턴을 그대로 미러링하여 리스크 최소화.

---

## 9. 결정 필요 사항 (앱 영향분) — §11 상세

- **D-A1** 임시조치 메모 저장 컬럼: 신규 `ADMIN_TEMP_ACTION_DESC` vs IMMEDIATE_ACTION_DESC 재사용. 권고=신규 컬럼(보고자 데이터 오염 방지). → 본 plan 은 신규 컬럼 전제.
- **D-A2** A5 상세 접근 주체: 관리자 전용 vs 보고자 셀프 허용. 권고=관리자 전용 상세 + 근로자 목록은 카드 요약만(1차 단순화).
- **D-A3** 사진 FILE_TYPE 코드값: SYS010 신규 '004' vs 기존 코드 재사용. developer MCP 확인 후 확정.
- **D-A4** 푸시 대상(SITE 안전관리자) 해석 기준: `tb_user_site_auth` + 안전직군 AUTH_CD 조인 정의. 안전직군 코드 목록(safe/00001/00004/00006/00008 등) 확정 필요.
- **D-A5** 진입 동선/노출 권한: 근로자 "아차사고 보고"를 안전 활동 카드 3분할로(+출근전 차단 적용 여부), 관리자 "사건 관리" 진입점 위치/노출 권한(gv_authCd 게이팅).

---

## 10. Flutter 연동 메모 (상세)

| 항목 | 결론 | 근거 |
|---|---|---|
| 사진 촬영/선택 | 신규 브리지 불요. 웹뷰 `<input type="file" accept="image/*">` (risk01 동일) | Risk_01.vue L73~82 동작 확인 |
| 카메라 권한 | 기존 InAppWebView 권한 핸들러로 충족(앱이 이미 위험성평가 사진 촬영 지원) | 기존 chkLst/risk 사진 기능 동작 중 |
| 저장소 권한 | 기존 권한으로 충족 | 동일 |
| 위치(발생장소) | 1차 미포함(텍스트 직접 입력). GPS 자동기입은 follow-up | 설계 §4-A 장소=직접입력 |
| JS-bridge 변경 | **없음** | — |
| 사전 안내 | 카메라 첫 사용 시 OS 권한 prompt 발생 가능 — 사용자 안내 | CLAUDE.md Flutter 규약 |

> Flutter 측 구현 작업 없음. `assets/vue_app/` 직접 편집 금지(빌드 후 복사). 본 작업은 Vue 변경만 빌드 대상.

---

## 11. 결정 필요 — 현황/권고/결정이유 (산문)

### D-A1. 관리자 임시조치 메모 저장 위치
**현황**: `tb_near_miss` 최소안 DDL(prafta-040-near-miss.sql)에는 관리자 임시조치 전용 컬럼이 없다. 보고자 즉시조치(`IMMEDIATE_ACTION_DESC`)와 정밀조사 추정원인(`CAUSE_DESC`)/재발방지(`PREVENTION_DESC`)만 있다. 설계 5-B 와이어프레임은 관리자 1차확인에 "임시조치" 입력칸을 둔다.
**권고**: `tb_near_miss` 에 `ADMIN_TEMP_ACTION_DESC varchar(500)` 신규 컬럼 추가(앱 마이그 prafta-app-012-near-miss-admin-action.sql). 웹 정밀조사 화면(prafta-040-4)도 이 컬럼을 읽기로 노출하면 추적 일관.
**결정이유**: `IMMEDIATE_ACTION_DESC` 재사용은 **주체가 다른 데이터(보고자 vs 관리자)를 한 칸에 덮어써 데이터 오염**된다. 보고자가 적은 즉시조치를 관리자가 임시조치로 갈아치우면 보고 원본이 사라진다. 별도 컬럼이 감사·중처법 이행증거 측면에서도 안전. 마이그 운영 미적용이라 컬럼 추가 비용도 낮다.

### D-A2. A5 상세 접근 주체(관리자 전용 vs 보고자 셀프)
**현황**: 근로자가 본인 보고 목록(A2)을 본 뒤 상세를 열고 싶을 수 있다. 그러나 상세(A5)는 관리자 1차확인용 데이터(임시조치/검토자)를 포함한다.
**권고**: 1차는 **A5 = 관리자 전용**(`assertSiteAccess`). 근로자 목록(A2)은 상세 진입 없이 카드에 요약(유형/잠재중대성/상태/일시)만 노출. 근로자 셀프 상세가 꼭 필요하면 차기 라운드에 `REPORTER_ID = JWT userCd` 허용 분기 추가.
**결정이유**: 근로자에게 관리자 검토 메모/검토자까지 노출할 필요가 없고, 권한 분기 단순화로 IDOR 표면을 줄인다. 근로자 핵심 가치는 "보고했다"의 확인이지 상세 추적이 아니다.

### D-A3. 사진 첨부 FILE_TYPE 코드값
**현황**: `tb_file_info.FILE_TYPE`[SYS010] 코드 카탈로그가 스냅샷에서 미확인. risk01 은 '002'(위험성평가) 사용. 아차사고 전용 분류가 필요한지 불명.
**권고**: 신규 '004'(아차사고) 가정으로 마이그 초안 작성하되, **developer 가 MCP 로 SYS010 현행값을 확인**해 (가) 적합한 기존 코드가 있으면 그것을 재사용하고 마이그 INSERT 생략, (나) 없으면 '004' 신설. 사진은 단일 파일(FILE_MGMT_CD 1개=1장) 전제.
**결정이유**: 첨부 분류 코드는 파일 관리/통계에 영향을 주지만 본 도메인 핵심은 아니다. 추측으로 INSERT 하면 코드 충돌 위험이 있어 MCP 확인을 게이트로 둔다.

### D-A4. 푸시 대상(사업장 안전관리자) 해석 기준
**현황**: "잠재중대성≥중대 신규보고 시 사업장 안전관리자에게 푸시"인데, "안전관리자" 집합을 어떤 컬럼/권한으로 도출할지 미정. prafta-031 은 단일 수신자(회수 대상자 본인)라 직접 참고가 한정적.
**권고**: `tb_user_site_auth`(해당 SITE_CD, USE_YN='Y') ∩ 안전직군 AUTH_CD(safe/master 또는 prafta-040 권한표의 안전직군 코드군)로 USER_CD 목록을 도출해 각자에게 1행 INSERT. 안전직군 코드 목록은 prafta-040-near-miss.sql 권한 시드(00001/00004/00006/00008/master/safe/system)와 정합되게 확정.
**결정이유**: 사업장 단위 안전 책임자에게만 알리는 게 설계 의도. 회사 전체로 뿌리면 노이즈. 단 대상 0명이어도 보고는 성공해야 하므로 푸시 INSERT 실패가 보고를 막지 않도록 설계(대상 없음=정상).

### D-A5. 진입 동선 및 노출 권한
**현황**: 근로자 보고 진입은 메인 안전 활동 카드가 자연스럽다(현재 2분할). 관리자 "사건 관리"는 앱에 진입점이 없다(웹 메뉴 `MENU_SRC='001'`은 앱 동적 라우트에 안 뜸).
**권고**: 근로자=안전 활동 카드를 3분할로 확장("안전점검/위험성 발굴/아차사고 보고"). 단 아차사고는 "지금 위험을 봤다" 즉시성이라 **출근 전 차단(blocked) 미적용** 검토(안전점검은 출근 후 정책이나 아차사고는 예외 가능). 관리자=세션 `gv_authCd` 가 안전직군일 때만 메인에 "사건 관리" 진입(카드 또는 헤더 영역). 노출 권한 코드군은 D-A4 와 동일 집합.
**결정이유**: 근로자 보고는 노출 위치가 명확할수록 신고율이 오른다(중처법 종사자 의견청취 증거). 관리자 진입은 권한 게이팅이 핵심(비관리자에게 목록 노출 금지). 출근 전 차단 여부는 안전 정책 판단이라 사용자 확정 필요.
