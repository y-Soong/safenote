# PRAFTA-047 공지사항(Notice) 기능 — 작업 분해 문서

> 출처 요청서: `.claude/requests/web_requests/prafta-047.md`
> 작업 영역: 웹/백엔드 (`PRAFTA/prafta-backend` + `PRAFTA/prafta-web-frontend`)
> 분해자: planner (서브에이전트, 자율 진행)
> 정책서 우선순위: 요청승인관리 재기획서 > 공통 정책서 > 근태관리 정책서 > 기술 정책서
> ⚠️ 본 문서는 분해/명세만 담는다. 실제 비즈니스 로직 코드는 developer가, 실시간 스키마 확정은 메인세션이 MCP로 수행한다.

---

## 0. 정책서 출처 매핑 (전체 작업 공통 근거)

| 요청서 항목 | 정책서 근거 | 비고 |
| --- | --- | --- |
| 발행 권한 = 공통 생성 버튼 권한(tb_syst_auth_menu) | 공통 §8.2.2 (화면 권한 단위 구조 — 생성/조회/저장/삭제/엑셀) | 공지 전용 권한 신설 금지 |
| 대상 선택 범위 = 발행자 3축 스코프 | 공통 §8.1(3축 AND), §8.4(조직 스코프), §8.5(master 예외) | 서버 재검증 필수 |
| master 비밀번호 면제·전사 수정/삭제 | 공통 §8.5 (마스터관리자 예외) | master 판별은 서버에서 |
| 노출 판정 = 수신자 현재 소속 기준 | 공통 §8.4.1(소속 사업장 자기노드+자손), §8.3.3(사이트 스위처 컨텍스트) | 발행시점 스냅샷 미사용 |
| 일용직 노드 제외·사업장 단위 노출 | 공통 §4(사용자 트랙), §5(슬롯/점유) | 일용직 현재 점유 사업장 판정은 §13-7 미확정 |
| 공지/알림 채널·오발송 방지 | 공통 §10(알림/공지), §10.3(권한 스코프 위반 시 발송 차단) | 로그인 팝업은 인앱 노출 |
| 첨부 다운로드 감사 로그 | 공통 §11.3(감사 로그 대상) | tb_audit_log ACTION_TYPE 다운로드 |
| PII 미보관 원칙 | 공통 §11.1 | 공지 본문/대상에 PII 없음(USER_CD만) |

> 공지 도메인 자체 비즈니스 룰(팝업 판정, PIN 정규화, ACK 이력 등)은 정책서에 직접 항목이 없고 **요청서 prafta-047.md가 1차 출처**다. 정책서는 권한·조직·알림·보안의 상위 제약만 제공한다. 충돌 없음.

---

## 1. 작업 개수 & 착수 순서 요약

총 **9개 작업**으로 분해 (PRAFTA-047-1 ~ PRAFTA-047-9).
요청서 §11(앱 네이티브 다운로드)은 web_requests 범위 밖 → **앱 백로그(별도 app_requests 티켓)** 로 분리 메모만.

착수 순서 (의존성 기반):
```
[선행] PRAFTA-047-1 (DDL 4종 + 인덱스)         ← 모든 백엔드의 토대
   └─ PRAFTA-047-2 (SYS 코드 + 메뉴 + master 권한 DML)  ← 화면 진입/코드 의존
        ├─ PRAFTA-047-3 (백엔드 관리: 목록/상세/생성/수정/삭제/비번/대상트리)
        │     └─ PRAFTA-047-6 (FE 공지 관리 목록 Notice_01)
        │     └─ PRAFTA-047-7 (FE 생성 팝업 NoticeCreatePop)
        │     └─ PRAFTA-047-8 (FE 조회/수정 팝업 NoticeInfoPop)
        ├─ PRAFTA-047-4 (백엔드 노출: 팝업 판정/ACK/다운로드 토큰)
        │     └─ PRAFTA-047-9 (FE 로그인 팝업 캐러셀 NoticePopupCarousel)
        └─ PRAFTA-047-5 (배치: 확인 이력 정리)  ← 독립, 후순위
```

병렬 가능: 047-3 / 047-4 는 047-1·047-2 완료 후 동시 진행 가능. FE(047-6~9)는 대응 백엔드 EP 계약 확정 후 script 연동.

---

## 2. 미해결 항목(§13) → MVP 기본 결정

| § | 항목 | 결정(MVP) | 근거 |
| --- | --- | --- | --- |
| 13-1 | SYS 코드 운영 방식 | **SYS010에 공지첨부값 신규 추가**(코드 테이블). **TARGET_SCOPE / ACK_TYPE는 컬럼 상수**(코드 테이블 미생성) | 값이 3개 이하로 적고 변동 없음. nearmiss가 5값 상태는 코드테이블로 했으나 ACK_TYPE/SCOPE는 enum 성격이 강해 상수가 일관. FILE_TYPE은 기존 SYS010 운영 관례(004 nearmiss 선례)상 코드값 추가가 정석 |
| 13-2 | 수정 정책 ⓐ/ⓑ | **ⓐ(팝업 기간 중 수정 전면 차단) 채택**. ⓑ는 컬럼/플래그를 미리 두지 않고 service에 확장 포인트 주석만 | 요청서 §8-1이 MVP 기본을 ⓐ로 명시. ⓑ는 재노출 사유 입력 UI/CONFIRMED 초기화 등 추가 설계 필요 → 별도 티켓 |
| 13-3 | 캐러셀 다건 상한 | **최대 10건**. 서버 팝업 조회 쿼리에 `LIMIT 10`, 정렬 §5(고정 우선·순번·최신순) 적용 | 로그인 직후 과다 노출 방지. CLAUDE.md 대량 SELECT LIMIT 원칙 |
| 13-4 | 다운로드 토큰 방식 | **자체 단기 토큰(JWT, scope=NOTICE_FILE_DL, 만료 5분)** 발급. 파일 저장소는 로컬(FILE_UPLOAD_BASE_DIR) 전제 → presigned URL 미사용 | 메모리 backend_path_externalize(로컬 업로드 경로 외부화), 기존 PhoneAuth scope JWT 선례(scope=PHONE_AUTH 10분) |
| 13-5 | 앱 네이티브 다운로드 | **app_requests 별도 티켓(prafta-app-021 가칭)** 으로 분리. 본 작업 범위 밖 | 요청서 §11 명시. webview iOS 다운로드 제약 |
| 13-6 | 노드 자손 매칭 | **MySQL 8 재귀 CTE** (tb_site_node.PARENT_NODE_CD). 기존 조직 트리 조회와 일관 | tb_site_node에 PARENT_NODE_CD·IX_NODE_PARENT 인덱스 존재 확인됨 |
| 13-7 | 일용직 현재 점유 사업장 판정 | **tb_daily_user.SITE_CD 기준**(스키마상 일용직은 (CMPNY_CD, SITE_CD, USER_CD) 키로 사업장에 직접 귀속). 단 슬롯/토큰 컨텍스트와의 정합은 **메인세션 MCP 확인 필요(아래 §7)** | tb_daily_user.SITE_CD NOT NULL 확인됨. 다만 점유 슬롯 테이블과의 우선순위는 미확정 |

---

## 3. 백엔드 작업

### PRAFTA-047-1 — DDL: 공지 테이블 4종 + 인덱스
- **유형**: backend / **영역**: web / **모듈**: (신규) `notice` / **작업유형**: 신규
- **요구사항 요약**: 요청서 §1의 tb_notice / tb_notice_target / tb_notice_file / tb_notice_user_ack 4개 테이블 DDL과 인덱스를 PRAFTA DDL 컨벤션으로 생성.
- **상세 설명**:
  - 핵심 요구사항:
    1) `tb_notice` 마스터 — PK(CMPNY_CD, NOTICE_ID). 컬럼은 요청서 §1-1 표 그대로. EDIT_PWD는 BCrypt(평문 금지). 인덱스 (CMPNY_CD, DEL_YN, PIN_YN, PIN_ORDER) / (CMPNY_CD, POPUP_YN, POPUP_FROM_YMD, POPUP_TO_YMD).
    2) `tb_notice_target` — PK(CMPNY_CD, NOTICE_ID, TARGET_SEQ). SITE/NODE 스코프일 때만 행 존재. 인덱스 (CMPNY_CD, SITE_CD, NODE_CD) 역방향 매칭 / (CMPNY_CD, NOTICE_ID).
    3) `tb_notice_file` — PK(CMPNY_CD, NOTICE_ID, FILE_MGMT_CD). tb_file_info FK 매핑.
    4) `tb_notice_user_ack` — PK(CMPNY_CD, NOTICE_ID, USER_CD). UPSERT 대상. 인덱스 (CMPNY_CD, USER_CD, NOTICE_ID).
    5) 감사 컬럼 컨벤션: INSERT_NO/INSERT_DATE/UPDATE_NO/UPDATE_DATE. 날짜 VARCHAR(8), 시각 VARCHAR(4). DEL_YN.
  - 정책서/요청서 출처: 요청서 §1-1~1-4, CLAUDE.md DDL/날짜 컨벤션. 멀티테넌트 CMPNY_CD 스코프(요청서 머리말).
  - 산출물: `PRAFTA/prafta-backend/src/main/resources/sql/migration/prafta-047-1-notice-ddl.sql`
- **선행 작업**: 없음
- **우선순위 근거**: 모든 백엔드의 토대. 가장 먼저.
- **⚠️ 메인세션 MCP 확인**: 4개 테이블이 운영 DB에 이미 없는지(부재 확인), tb_file_info의 FILE_MGMT_CD 실타입(varchar(50) 추정), 컬럼 길이/COLLATE를 실시간 검증 후 DDL 확정.

### PRAFTA-047-2 — SYS 코드 + 메뉴 + master 권한 DML (사용자 추가 지시 ①②)
- **유형**: backend(DML) / **영역**: web / **모듈**: `notice` + `cmm` / **작업유형**: 신규
- **요구사항 요약**: 공지 첨부 FILE_TYPE(SYS010) 신규값 + 공지 관리 화면 진입용 대메뉴/소메뉴(tb_syst_menu_m/d) + master 등 역할의 접근/생성 권한(tb_syst_auth_menu) DML 등록.
- **상세 설명**:
  - 핵심 요구사항:
    1) **SYS010 신규 FILE_TYPE 추가** — 공지첨부. 코드값은 SYS010 현행(001 일일점검/002 위험성평가/003 TBM/004 아차사고)의 **다음 미사용 번호 '005'** 권장. SYST_VAL_D_NM='공지첨부'. (스냅샷 기준 005 미사용 추정 → MCP 확인)
    2) **대메뉴 등록** — tb_syst_menu_m. MENU_M_ID='notice', MENU_SRC='001'(웹, SYS007), MENU_NM='공지사항', MENU_IDX는 현행 최대+1(nearMiss=7이므로 8 추정 → MCP 확인). USE_YN='Y'.
    3) **소메뉴 등록** — tb_syst_menu_d. MENU_D_ID='Notice_01', MENU_M_ID='notice', MENU_VIEW='notice/Notice_01.vue', MENU_NM='공지사항 관리', MENU_IDX=1.
    4) **권한 매핑(tb_syst_auth_menu)** — CMPNY_CD='001'. **master 행 필수**: USE_YN='Y', BTN_SRCH='Y', BTN_NEW='Y'(생성 가능), BTN_SAVE='Y', BTN_DELT='Y', BTN_EXCL='N'. 그 외 역할(00001/00004/00006/00008/99999/hr/safe/system)은 nearmiss/User_04 선례 폭과 동일하게 부여하되, **생성/저장/삭제 버튼 ON/OFF는 고객이 권한관리 화면에서 직접 조정**하는 것이 §2-1 원칙이므로 시드값은 보수적으로(조회 Y, 생성/저장/삭제는 master만 Y, 나머지 역할은 조회만 Y 권장).
       - ⚠️ 사용자 지시 핵심: **"master 계정 기준 접근/생성 가능"** 이 필수 보장 대상. master 행의 USE_YN/BTN_NEW='Y' 가 누락되면 안 됨.
    5) 멱등성 주석: PK 충돌 시 재실행 금지. 적용 전 부재 확인 SELECT 포함.
  - 정책서/요청서 출처: 요청서 §3(SYS 코드), §2-1(발행 권한=버튼 권한), 공통 §8.2.2(버튼 권한 구조), §8.5(master). 사용자 추가 지시 ①②.
  - 산출물: `prafta-047-2-notice-codes-menu-auth.sql`
- **선행 작업**: PRAFTA-047-1
- **우선순위 근거**: 화면 진입·코드 의존. DDL 다음.
- **⚠️ 메인세션 MCP 확인(중요)**:
  - SYS010 현재 최대 SYST_VAL_D_CD (005 미사용 확인).
  - tb_syst_menu_m 현재 MENU_M_ID 목록 + 최대 MENU_IDX (notice 미등록 확인, IDX 충돌 회피).
  - tb_syst_auth_menu의 **실제 AUTH_CD 목록**(역할 코드 00001/00004/.../master/hr/safe/system이 운영 DB에 존재하는지) — 멀티테넌트면 CMPNY_CD별 행 추가 필요.
  - SYS007(메뉴 사용처) '001'=WEB 매핑 확인.
  - **실제 INSERT문은 developer가 MCP 실시간 스키마 확인 후 작성**(선행조건: 메인세션 MCP 스키마 확인).

### PRAFTA-047-3 — 백엔드: 공지 관리 (목록/상세/생성/수정/삭제/비번검증/대상트리)
- **유형**: backend / **영역**: web / **모듈**: `notice/notice01` / **작업유형**: 신규
- **요구사항 요약**: 공지 CRUD + 비밀번호 검증 + 발행자 대상선택 트리 조회 + PIN 정규화 + 발행자 스코프 서버 재검증.
- **상세 설명**:
  - 핵심 요구사항:
    1) 패키지 배치(nearmiss01 미러): `com.prafta.web.notice.notice01.{controller,service,service.impl,mapper,application.param,application.query,application.command,application.model,dto.request,dto.response,result}`. 컨트롤러 `@RequestMapping("/notice01")`, axios 프리픽스 `/webApi/notice01/...`. 식별자(cmpnyCd/userId)는 JwtUtil 클레임에서만 도출(IDOR 차단).
    2) 엔드포인트(요청서 §4-1):
       - `GET /notice01/notice-lists` — 관리 목록. NoticeListRequest→Param→Query→NoticeResult list(LEFT JOIN tb_notice_user_ack로 isUnread/isUpdated 본인 기준 계산 §7)→NoticeListResponse. 정렬 §5(PIN_YN DESC, PIN_ORDER ASC, INSERT_DATE DESC). 대상 요약(전사/사업장명/노드명 외 N건) 포함.
       - `GET /notice01/notice-info` — 단건 상세(상세/수정 진입). NoticeResult + NoticeFileResult list + NoticeTargetResult list.
       - `POST /notice01/save-notice` — 생성. NoticeSaveRequest(+첨부 list NoticeFileModel, +대상 list NoticeTargetModel)→SaveParam. NOTICE_ID 채번(tb_cmm_seq, N+YYYYMMDD+SEQ). EDIT_PWD BCrypt. PIN 정규화(§5). **§2-2 발행자 스코프 서버 재검증**(모든 target 행이 발행자 3축 스코프 내인지 기존 권한 로직 재호출). 범위 밖이면 ApiException.appendf(COMMON_400_xxx, "대상 권한 범위 초과 - {SITE_CD}/{NODE_CD}").
       - `POST /notice01/update-notice` — 수정. editPwd 검증(master 면제, 서버 판별). **§8 수정 정책 ⓐ**: POPUP_YN='Y' & today BETWEEN FROM~TO 이면 내용 수정 차단(ApiException). 대상 재설정 + §2-2 재검증.
       - `POST /notice01/verify-pwd` — 비밀번호 검증. NoticePwdRequest→해시 조회→BCrypt match. master면 통과.
       - `POST /notice01/delete-notice` — 논리삭제(DEL_YN='Y'). 비번/ master 동일. 삭제 시 PIN 재압축(§5-3).
       - `GET /notice01/scope-tree` — 발행자 대상선택 트리. NoticeScopeQuery(발행자 스코프: 소속 사업장 자기노드+자손 / 권한 보유 사업장 전 노드 / master 전사+ALL). NoticeScopeResult(선택가능 사업장/노드 트리).
    3) PIN 정규화: 저장 시 min(요청PIN_ORDER, 현재고정수+1) 클램프 후 시프트 또는 전체 재정렬 UPDATE. 단일 트랜잭션 + CMPNY_CD 단위 SELECT ... FOR UPDATE(§5-4).
    4) DTO 컨벤션: record + static from(), 필수값 ApiException.appendf(COMMON_400_001, ...), 대상/첨부 다건은 별도 Model 경유(ChkptInfoModel 패턴).
    5) MyBatis: XML 헤더 `/* Notice01Mapper.methodName */`, leading comma, #{} 바인딩, SELECT * 금지, CMPNY_CD 스코프 전 쿼리 필수.
  - Mapper 메서드 목록(권장):
    - `selectNoticeList`, `selectNoticeOne`, `selectNoticeFileList`, `selectNoticeTargetList`,
    - `selectNoticeIdSeq`(채번), `insertNotice`, `insertNoticeTarget`(foreach), `insertNoticeFile`(foreach),
    - `updateNotice`, `deleteNoticeTargetByNoticeId`, `deleteNoticeFileByNoticeId`(재설정용 논리/물리 정책 결정),
    - `updateNoticeDelYn`(논리삭제), `selectNoticePwdHash`,
    - `selectPinnedForUpdate`(FOR UPDATE), `shiftPinOrder` / `compactPinOrder`,
    - `selectScopeSiteNodeTree`(재귀 CTE).
  - DTO 플로우 표(엔드포인트별):

    | 엔드포인트 | Request | Param | Query/Command | Result | Response |
    | --- | --- | --- | --- | --- | --- |
    | notice-lists | NoticeListRequest | NoticeListParam | NoticeListQuery | NoticeResult(+isUnread/isUpdated/대상요약) | NoticeListResponse |
    | notice-info | NoticeInfoRequest | NoticeDetailParam | NoticeDetailQuery | NoticeResult+NoticeFileResult[]+NoticeTargetResult[] | NoticeDetailResponse |
    | save-notice | NoticeSaveRequest(+NoticeFileModel[]+NoticeTargetModel[]) | NoticeSaveParam | NoticeSaveCommand,NoticeTargetSaveCommand,NoticeFileSaveCommand | — | 200/201 |
    | update-notice | NoticeSaveRequest(+editPwd,+대상[]) | NoticeSaveParam | NoticeUpdateCommand,(대상 재설정 Command) | — | 200 |
    | verify-pwd | NoticePwdRequest | NoticePwdParam | NoticePwdQuery | NoticePwdResult | NoticePwdResponse |
    | delete-notice | NoticeDeleteRequest | NoticeDeleteParam | NoticeDeleteCommand | — | 200 |
    | scope-tree | header(JWT) | NoticeScopeParam | NoticeScopeQuery | NoticeScopeResult | NoticeScopeResponse |

  - 정책서/요청서 출처: 요청서 §4,§5,§7,§8. 공통 §8.1/§8.4/§8.5(스코프 재검증·master). CLAUDE.md DTO/SQL 컨벤션.
  - 영향 받는 파일: 신규 `com.prafta.web.notice.notice01.**`, `resources/com/prafta/web/notice/notice01/mapper/Notice01Mapper.xml`(경로는 기존 mapper resource 규칙 따름 → developer 확인).
  - 예상 산출물: controller 1, service+impl, mapper(if+xml), param/query/command/result/response/model 다수.
- **선행 작업**: PRAFTA-047-1, PRAFTA-047-2
- **우선순위 근거**: 법적 영역 아님. 단 발행자 스코프 재검증은 보안 민감 → security 검토 대상.
- **⚠️ 메인세션 MCP 확인**: tb_cmm_seq SEQ_KEY 채번 규칙(공지용 키 신규), 기존 3축 스코프 검증 로직(공통 컴포넌트/Mapper)이 어디 있는지(baim06 UserNodeInfo / 노드 자손 조회 재사용 후보) — developer가 재사용처 식별.

### PRAFTA-047-4 — 백엔드: 노출(로그인 팝업 판정 / ACK / 다운로드 토큰)
- **유형**: backend / **영역**: web(웹/모바일 공통 호출) / **모듈**: `notice/notice01` / **작업유형**: 신규
- **요구사항 요약**: 로그인 직후 팝업 노출 판정(대상 매칭+이력 제외) + 확인/숨김 UPSERT + 첨부 다운로드 단기 토큰 발급 + 다운로드 감사 로그.
- **상세 설명**:
  - 핵심 요구사항:
    1) `POST /notice01/popup` — 수신자 현재 소속(curSiteCd/curNodeCd, isDaily) 기준 노출 공지 list. 요청서 §6 의사쿼리 그대로:
       - 공통 게이트: DEL_YN='N', POPUP_YN='Y', today BETWEEN FROM~TO.
       - 대상 매칭 §6-1: ALL=전원 / SITE=(SITE_CD=cur AND NODE_CD IS NULL) / NODE=직접매칭 OR (INCLUDE_DESCENDANTS_YN='Y' AND cur가 자손, 재귀 CTE).
       - 일용직 §6-2: NODE 제외, INCLUDE_DAILY_YN='Y' 강제, (ALL OR SITE=현재 점유 사업장)만.
       - 이력 제외 §6-3: SNOOZED(SNOOZE_UNTIL>=today, 정규직만) 제외 / 비고정+CONFIRMED 제외(고정은 CONFIRMED 무시).
       - 정렬 §5 + **LIMIT 10(§13-3 결정)**.
       - 응답에 각 공지의 버튼 셋 결정용 플래그(PIN_YN, isDaily) 포함.
    2) `POST /notice01/ack-confirm` — ACK_TYPE='CONFIRMED' UPSERT(MySQL INSERT ... ON DUPLICATE KEY UPDATE). LAST_READ_DATE 갱신.
    3) `POST /notice01/ack-snooze` — ACK_TYPE='SNOOZED', SNOOZE_UNTIL_YMD=오늘+7 UPSERT. **정규직·고정공지 한정**(일용직/비고정 거부 ApiException).
    4) `POST /notice01/read` (or popup 호출 시 일괄) — LAST_READ_DATE 갱신(§7 뱃지 소멸용). 열람 시 갱신.
    5) `GET /notice01/file-download-token` — NoticeFileDlParam→단기 토큰(JWT scope=NOTICE_FILE_DL, exp 5분, claim: cmpnyCd/noticeId/fileMgmtCd/userCd). NoticeFileDlResponse(token + 만료).
    6) `GET /notice01/file-download?token=...` — 토큰 검증 후 파일 스트림(웹 직접 다운로드). 다운로드 시 tb_audit_log ACTION_TYPE 다운로드 기록(§11 / 공통 §11.3). **앱은 토큰만 받아 네이티브가 처리(별도 티켓)**.
  - 일용직 현재 점유 사업장: §13-7 결정대로 tb_daily_user.SITE_CD(또는 JWT의 gv_siteCd) — MCP 확인 후 developer 확정.
  - Mapper 메서드: `selectPopupNoticeList`(핵심 단일 쿼리), `selectNodeDescendants`(재귀 CTE, 047-3과 공유 가능), `upsertNoticeAck`, `updateNoticeLastReadDate`, `selectNoticeFileOne`.
  - DTO 플로우:

    | 엔드포인트 | Request | Param | Query/Command | Result | Response |
    | --- | --- | --- | --- | --- | --- |
    | popup | header(JWT) | NoticePopupParam | NoticePopupQuery | NoticePopupResult[] | NoticePopupResponse |
    | ack-confirm | NoticeAckRequest | NoticeAckParam | NoticeAckCommand(UPSERT) | — | 200 |
    | ack-snooze | NoticeAckRequest | NoticeAckParam | NoticeSnoozeCommand(UPSERT) | — | 200 |
    | file-download-token | NoticeFileDlRequest | NoticeFileDlParam | NoticeFileQuery | NoticeFileResult(+token) | NoticeFileDlResponse |

  - 정책서/요청서 출처: 요청서 §6,§7,§10,§11. 공통 §10(알림/공지)·§10.3(스코프 위반 발송 차단)·§11.3(감사 로그)·§8.4(소속 기준 노출).
  - 예상 산출물: 위 EP controller 메서드 + service + mapper + DTO.
- **선행 작업**: PRAFTA-047-1, PRAFTA-047-2
- **우선순위 근거**: PII 미보관이나 다운로드 토큰·노출 판정이 보안 민감 → security 검토 필수.
- **⚠️ 메인세션 MCP 확인**: tb_audit_log 구조 + ACTION_TYPE 코드값(다운로드 코드 존재 여부), JWT scope 토큰 발급 유틸(PhoneAuth scope 선례) 위치, tb_daily_user vs 슬롯 테이블의 현재 사업장 단일 출처.

### PRAFTA-047-5 — 백엔드: 확인 이력 정리 배치
- **유형**: backend / **영역**: web / **모듈**: `common.cmm.schedule` 또는 `notice` 배치 / **작업유형**: 신규
- **요구사항 요약**: 팝업 종료(POPUP_TO_YMD) 6개월 경과 공지의 tb_notice_user_ack 행 정기 삭제.
- **상세 설명**:
  - 핵심 요구사항:
    1) 대상: `POPUP_TO_YMD < (오늘 - 6개월)` 공지의 tb_notice_user_ack 행만 물리 삭제.
    2) **현행 노출 중 공지의 CONFIRMED 이력 절대 삭제 금지**(§9 경고).
    3) 주기: 일배치(@Scheduled cron). 기존 push consumer/슬롯 만료 배치 스케줄 패턴(com.prafta.common.cmm.schedule.*) 따름. **게이트 플래그 기본 off** 권장(운영 ON 전 검증).
    4) SNOOZED 만료 행은 쿼리로 걸러지므로 필수 아님(옵션 포함 가능).
  - Mapper: `deleteExpiredNoticeAck`(POPUP_TO_YMD 6개월 경과 조인 삭제).
  - 정책서/요청서 출처: 요청서 §9. 공통 §11(보존). 메모리 prafta-com-002(스케줄 배치 게이트 패턴).
  - 예상 산출물: 스케줄러 컴포넌트 + mapper 1메서드.
- **선행 작업**: PRAFTA-047-1
- **우선순위 근거**: 독립·데이터 정합성 영향 낮음 → 후순위.
- **⚠️ 메인세션 MCP 확인**: 6개월 경과 후보 건수(운영 영향), 기존 @Scheduled 게이트 환경변수 컨벤션.

---

## 4. 프론트엔드 작업 (화면 명세 + Vue 골격)

> 디자인 토큰: `src/assets/css/tokens.css`(--color-primary #16a34a 등). 단 `--space-*`/`--radius-*`는 토큰 파일에 미정의 → 기존 화면(NearMiss_01)이 `var(--space-sm, 0.5rem)` 폴백 형식을 쓴다. 골격도 **동일 폴백 형식**을 사용한다.
> 공통 컴포넌트: ViewHeader, CalendarSrch, ThSortable, BaseSelect, TimeInput, LoadingSpinner. 팝업 전역 호출: useModal().open. 대상 선택은 SiteSearchPop / SiteNodeSearchPop / UsersMultiSearchPop 재사용 후보.
> viewResolver가 컴포넌트명으로 자동 로드 → 라우터 등록 불필요(메모리 frontend_layout). MENU_VIEW 경로만 맞추면 됨.

### PRAFTA-047-6 — FE: 공지사항 관리 목록 (Notice_01)
- **유형**: frontend-screen / **영역**: web / **모듈**: notice / **작업유형**: 신규
- 연결 UI 명세: UI-NTC-01
- 화면 위치: `src/views/notice/Notice_01.vue`
- 연결 백엔드: PRAFTA-047-3 (`/webApi/notice01/notice-lists`)
- 골격 산출물: 아래 §5 코드블록. script는 TODO(developer)로 API/store/router 지점만 표시.

### PRAFTA-047-7 — FE: 신규 공지 생성 팝업 (NoticeCreatePop)
- **유형**: frontend-component / **영역**: web / **모듈**: notice / **작업유형**: 신규
- 연결 UI 명세: UI-NTC-02
- 화면 위치: `src/views/notice/popup/NoticeCreatePop.vue`
- 연결 백엔드: PRAFTA-047-3 (save-notice, scope-tree)

### PRAFTA-047-8 — FE: 공지 조회/수정 팝업 (NoticeInfoPop)
- **유형**: frontend-component / **영역**: web / **모듈**: notice / **작업유형**: 신규
- 연결 UI 명세: UI-NTC-03
- 화면 위치: `src/views/notice/popup/NoticeInfoPop.vue`
- 연결 백엔드: PRAFTA-047-3 (notice-info, verify-pwd, update-notice, delete-notice, scope-tree), 047-4(file-download-token)

### PRAFTA-047-9 — FE: 로그인 팝업 캐러셀 (NoticePopupCarousel)
- **유형**: frontend-component / **영역**: web (모바일 app FE는 별도 미러 티켓) / **모듈**: notice / **작업유형**: 신규
- 연결 UI 명세: UI-NTC-04
- 화면 위치: `src/components/popup/NoticePopupCarousel.vue` (전역 호출 공용 팝업)
- 연결 백엔드: PRAFTA-047-4 (popup, ack-confirm, ack-snooze, file-download-token)
- **통합 지점**: LoginView.fnMoveMainPath() 이후 메인 진입 시점(또는 MainView onMounted)에서 `useModal().open(NoticePopupCarousel)` 호출. developer가 LoginView/MainView 중 어디서 띄울지 결정(메인 진입 후 1회 권장). 라우터 이동 로직은 골격에 넣지 않음.

---

## 5. 화면 명세 (UI)

### UI-NTC-01 Notice_01 (공지사항 관리 목록)
- 참조 패턴: `views/nearMiss/NearMiss_01.vue` (ViewHeader + 검색바 + data-grid + dblclick → 팝업).
- 레이아웃:
```
+----------------------------------------------------------+
| ViewHeader (제목 "공지사항 관리"  [신규][검색])           |
+----------------------------------------------------------+
| 검색바: [제목 키워드] [팝업여부 select] [고정여부 select] |
|         [등록기간 CalendarSrch ~ CalendarSrch]           |
+----------------------------------------------------------+
| subtitle "공지 리스트"                                    |
| +------------------------------------------------------+ |
| | No | 뱃지 | 제목 | 내용(...) | 첨부 | 대상요약 |      | |
| |    |      |      |           | 📎  | 전사/사업장.. |   | |
| |    | [NEW]| ...  |  더블클릭 → NoticeInfoPop          | |
| |  (PIN_YN=Y row: 배경 연한 붉은색)                     | |
| +------------------------------------------------------+ |
+----------------------------------------------------------+
```
- 컴포넌트 매핑:

  | 영역 | 컴포넌트 |
  | --- | --- |
  | 헤더/버튼 | ViewHeader (@search, buttons.create) |
  | 등록기간 | CalendarSrch (startDate/endDate) |
  | 정렬 헤더 | ThSortable + useTableSort/useColumnResize |
  | 빈 상태 | tbody colspan 안내문 |
  | 신규/조회 팝업 | useModal().open(NoticeCreatePop / NoticeInfoPop) |

- 상태별 동작: loading(목록 비움)→ empty("등록된 공지가 없습니다.")→ success(행 렌더, 뱃지/고정 배경)→ error($alert).
- 사용자 플로우: 진입 → onMounted 코드/목록 조회 → 검색조건 변경 후 [검색] → [신규] 생성 팝업 / row 더블클릭 조회·수정 팝업 → 저장 후 목록 새로고침.
- 뱃지: isUnread→NEW, isUpdated→UPDATE (본인 기준, 백엔드 계산값 그대로 표시).
- 백엔드 의존: GET /webApi/notice01/notice-lists (PRAFTA-047-3).

### UI-NTC-02 NoticeCreatePop (신규 공지 생성)
- 참조 패턴: SiteInfoPop / NearMissInfo (드래그 모달 + form-container + 2단 또는 단일 컬럼).
- 레이아웃:
```
+--------- modal-content-wide (드래그 헤더 "신규 공지") [x] ---------+
| 제목      [____________________________]                          |
| 내용      [ 리치텍스트/textarea                ]                   |
| 비밀번호* [______]  (저장 시 BCrypt, 필수)                         |
|------------------------------------------------------------------|
| 대상 지정                                                         |
|  스코프: ( ) 전사  ( ) 사업장  ( ) 사업장+노드                    |
|  [대상 선택 트리 — 발행자 스코프 제한 렌더]  [+ 대상 추가]        |
|   선택된 대상 목록(칩): 사업장A / 노드X [자손포함 ☑] [삭제]       |
|  ☐ 일용직 포함 (INCLUDE_DAILY_YN)                                 |
|------------------------------------------------------------------|
| ☐ 로그인 시 팝업   → ON: 팝업기간 [CalendarSrch ~ CalendarSrch]*  |
| ☐ 상단 고정        → ON: 고정순번 [__] (서버 보정 안내문)         |
| 첨부파일 [파일 선택] (다건) — 목록/삭제                            |
|------------------------------------------------------------------|
|                                        [취소] [저장]              |
+------------------------------------------------------------------+
```
- 컴포넌트 매핑: 스코프 라디오/체크는 native + label(공통 라디오 컴포넌트 없음 → native 허용, tokens 스타일), 대상 선택은 SiteSearchPop/SiteNodeSearchPop(useModal), 팝업기간 CalendarSrch, 첨부는 native file input(공통 파일 컴포넌트 미존재 — 메인세션 확인 필요, 부재 시 native).
- 상태별 동작: 스코프=전사 → 대상 트리/일용직 체크 비활성. 팝업 OFF → 기간 비활성. 고정 OFF → 순번 비활성.
- 사용자 플로우: 입력 → 필수 검증(제목/내용/비번, 팝업 ON 시 기간) → 저장 → 부모 목록 새로고침.
- 백엔드 의존: scope-tree(트리 제한), save-notice (PRAFTA-047-3).

### UI-NTC-03 NoticeInfoPop (조회/수정)
- 참조 패턴: NearMissInfo (좌 읽기 / 우 편집) 또는 SiteInfoPop 단일 폼 + 모드 토글.
- 레이아웃:
```
+--------- modal-content-wide (드래그 헤더 "공지 상세") [x] ---------+
| [조회 모드]                                                       |
|  제목/내용/대상/첨부(다운로드 링크)/등록자/등록일시                |
|------------------------------------------------------------------|
| 비밀번호 [______] [확인]  → 검증 통과 시 수정 모드                |
|  (master: 비밀번호 컴포넌트 disabled, 즉시 수정 가능)             |
|------------------------------------------------------------------|
| [수정 모드] NoticeCreatePop과 동일 입력 필드 + [삭제] [저장]      |
|  (POPUP_YN=Y & 기간 중: 내용 수정 영역 readonly + 안내문 §8-1ⓐ)  |
+------------------------------------------------------------------+
```
- 컴포넌트 매핑: 첨부 다운로드는 file-download-token 발급 후 a[href]/window 처리(developer), master 판별은 서버 응답 플래그(클라 신뢰 금지) — gv_authCd로 UI 보조만.
- 상태별 동작: 조회→비번검증→수정. master 진입 시 비번 disabled. 기간중+POPUP_YN=Y면 내용 readonly.
- 백엔드 의존: notice-info, verify-pwd, update-notice, delete-notice, scope-tree, file-download-token.

### UI-NTC-04 NoticePopupCarousel (로그인 팝업 캐러셀)
- 참조 패턴: 신규(전역 modal-overlay) + 캐러셀 직접 구현(공통 캐러셀 컴포넌트 없음). 모바일 ~380px·웹 양쪽 대응.
- 레이아웃:
```
+------ modal-overlay (centered) ------+
| +-- carousel card --+               |
| |  제목             |  ‹  1/3  ›    |   ← 좌우 이동 + 인디케이터
| |  게시일자         |               |
| |  내용 (스크롤)    |               |
| |  첨부: 파일1 ⬇    |               |
| +-------------------+               |
| 하단 버튼(§6-6 분기):              |
|  정규+비고정: [확인][닫기]         |
|  정규+고정:   [일주일간 보지 않기][닫기] |
|  일용직:      [확인][닫기]         |
| ● ○ ○  (인디케이터)               |
+--------------------------------------+
```
- 컴포넌트 매핑: 캐러셀 자체 구현(transform translateX, ‹ › 버튼, 인디케이터 dots). 버튼 셋은 currentItem.pinYn / isDaily(서버 플래그)로 computed 분기. 첨부 다운로드는 file-download-token.
- 상태별 동작: empty(노출 공지 0건 → 팝업 자체 미오픈, 부모가 미호출)/ single(좌우 화살표 숨김)/ multi(화살표·인디케이터 표시). [확인]→ack-confirm, [일주일간 보지 않기]→ack-snooze, [닫기]→이력없이 close. 슬라이드/오픈 시 read 갱신.
- 반응형: 데스크탑 카드 max-width 480px, 모바일(<=480px) 폭 92vw. CSS 변수 + 미디어쿼리(기존 화면 break point 따름).
- 백엔드 의존: popup, ack-confirm, ack-snooze, file-download-token (PRAFTA-047-4).
- 통합: 로그인 후 메인 진입 1회 호출(LoginView.fnMoveMainPath 직후 또는 MainView onMounted) — developer 결정.

---

## 6. 메뉴/권한 DML 작업 개요 (PRAFTA-047-2 상세)

- **선행조건**: 메인세션 MCP 스키마 확인 (SYS010 최대값, tb_syst_menu_m 최대 IDX, tb_syst_auth_menu 실제 AUTH_CD 목록, 멀티테넌트 시 CMPNY_CD 분기).
- **대상 테이블**: tb_syst_val_d(SYS010 추가) / tb_syst_menu_m(notice 대메뉴) / tb_syst_menu_d(Notice_01 소메뉴) / tb_syst_auth_menu(역할별 권한).
- **INSERT 항목 개요** (실제 INSERT는 developer가 MCP 확인 후 작성):
  - SYS010: ('SYS010','005','공지첨부',5,'Y','SYSTEM') — 코드값은 MCP로 미사용 번호 확정.
  - tb_syst_menu_m: ('notice','001','공지사항',{최대IDX+1},'Y','SYSTEM',NOW()).
  - tb_syst_menu_d: ('Notice_01','notice','notice/Notice_01.vue','공지사항 관리',1,'Y','SYSTEM',NOW()).
  - tb_syst_auth_menu: **master 행 필수** (USE_YN='Y',BTN_SRCH='Y',BTN_NEW='Y',BTN_SAVE='Y',BTN_DELT='Y',BTN_EXCL='N'). 그 외 역할은 조회(Y)만 시드, 생성/저장/삭제는 고객이 권한관리에서 직접 ON(§2-1). nearmiss/User_04 AUTH 세트 폭 참고.
- **멱등성**: PK 충돌 시 재실행 금지, 적용 전 부재 확인 SELECT 동반.

---

## 7. 메인세션이 MCP로 실시간 확인해야 할 목록 (스냅샷 낡음 위험)

1. **공지 4개 테이블 부재 확인** (tb_notice / _target / _file / _user_ack 미존재).
2. **tb_syst_val_d SYS010** 현재 최대 SYST_VAL_D_CD ('005' 미사용 확인 / 충돌 시 다음 번호).
3. **tb_syst_menu_m** 현재 MENU_M_ID 전체 + 최대 MENU_IDX (notice 미등록·IDX 충돌 회피).
4. **tb_syst_menu_d** Notice_01 미등록 확인.
5. **tb_syst_auth_menu** 운영 DB의 실제 AUTH_CD 목록 (00001/00004/00006/00008/99999/master/hr/safe/system 존재 여부) + 멀티테넌트 시 CMPNY_CD 분포(현행 '001' 단일인지).
6. **SYS007** 메뉴 사용처 '001'=WEB 매핑 확인.
7. **tb_file_info** FILE_MGMT_CD 실타입/길이 (FK 매핑 컬럼 타입 일치).
8. **tb_cmm_seq** 공지 채번용 SEQ_KEY 신규 키 규칙(N+YYYYMMDD 일자 키 vs 누적 키) — 기존 채번 사용처 패턴.
9. **tb_site_node** PARENT_NODE_CD 재귀 깊이/순환 가드 (자손 매칭 CTE 안전성).
10. **tb_daily_user.SITE_CD vs 슬롯/토큰 컨텍스트** — 일용직 "현재 점유 사업장" 단일 출처(§13-7).
11. **tb_audit_log** 구조 + ACTION_TYPE 코드값(다운로드용 코드 존재/신설 필요).
12. **3축 스코프 검증 재사용 로직** 위치 (baim06 UserNodeInfo / 노드 관리자 조회 / com.prafta.common 권한 유틸) — 발행자 대상 재검증·노드 자손 조회 재사용.
13. **JWT scope 토큰 발급 유틸** (PhoneAuth scope=PHONE_AUTH 선례) — 다운로드 토큰 scope=NOTICE_FILE_DL 재사용 가능 여부.

---

## 8. 앱 백로그로 분리한 항목 (web_requests 범위 밖)

- **prafta-app-021(가칭) 공지 앱 처리**: ① Flutter 네이티브 다운로드 핸들러(iOS share sheet / Android DownloadManager, 토큰 수신 → OS 다운로드) ② app-frontend 로그인 팝업 캐러셀 미러(NoticePopupCarousel app 버전, /appApi/notice/popup) ③ 일용직 모바일 노출. → app_requests/ 별도 티켓.
- 본 web 작업의 다운로드 토큰 API(PRAFTA-047-4)는 앱 작업의 **선행/병행 전제**(요청서 §11).

---

## 9. 메인 세션이 Notion에 기록할 항목

### "작업 로그" DB (PRAFTA-047-1 ~ 047-9, 상태=분해완료, 담당=planner)

| 작업ID | 영역 | 모듈 | 작업유형 | 유형태그 | 요구사항 요약 | 산출물 | 선행 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PRAFTA-047-1 | web | notice | 신규 | [backend] | 공지 4테이블 DDL+인덱스 | prafta-047-1-notice-ddl.sql | - |
| PRAFTA-047-2 | web | notice/cmm | 신규 | [backend-DML] | SYS010+메뉴+master권한 등록(선행:MCP 스키마확인) | prafta-047-2-notice-codes-menu-auth.sql | 047-1 |
| PRAFTA-047-3 | web | notice/notice01 | 신규 | [backend] | 공지 관리 CRUD+비번+대상트리+PIN정규화+스코프재검증 | com.prafta.web.notice.notice01.** + Notice01Mapper.xml | 047-1,2 |
| PRAFTA-047-4 | web | notice/notice01 | 신규 | [backend] | 로그인 팝업 판정+ACK UPSERT+다운로드토큰+감사로그 | (047-3 패키지 내 EP/서비스/매퍼) | 047-1,2 |
| PRAFTA-047-5 | web | notice(배치) | 신규 | [backend] | 확인이력 6개월 정리 배치(게이트 off) | 스케줄러+deleteExpiredNoticeAck | 047-1 |
| PRAFTA-047-6 | web | notice | 신규 | [frontend-screen][UI 명세: UI-NTC-01] | 공지 관리 목록 | src/views/notice/Notice_01.vue | 047-3 |
| PRAFTA-047-7 | web | notice | 신규 | [frontend-component][UI 명세: UI-NTC-02] | 신규 공지 생성 팝업 | src/views/notice/popup/NoticeCreatePop.vue | 047-3 |
| PRAFTA-047-8 | web | notice | 신규 | [frontend-component][UI 명세: UI-NTC-03] | 공지 조회/수정 팝업 | src/views/notice/popup/NoticeInfoPop.vue | 047-3,4 |
| PRAFTA-047-9 | web | notice | 신규 | [frontend-component][UI 명세: UI-NTC-04] | 로그인 팝업 캐러셀 | src/components/popup/NoticePopupCarousel.vue | 047-4 |

### "도메인 지식 베이스" DB (화면 명세, 검증상태=Claude 분석)

| 이름 | 영역 | 모듈 | 현재 동작 | 의도된 동작 |
| --- | --- | --- | --- | --- |
| UI-NTC-01 Notice_01 | web | notice | 신규 작성 | 본 문서 §5 UI-NTC-01 |
| UI-NTC-02 NoticeCreatePop | web | notice | 신규 작성 | 본 문서 §5 UI-NTC-02 |
| UI-NTC-03 NoticeInfoPop | web | notice | 신규 작성 | 본 문서 §5 UI-NTC-03 |
| UI-NTC-04 NoticePopupCarousel | web | notice | 신규 작성 | 본 문서 §5 UI-NTC-04 |

> UI ID는 메인세션이 Notion "도메인 지식 베이스"의 UI- prefix 최대 ID 조회 후 UI-{순번}으로 재채번 권장(본 문서는 UI-NTC-NN 임시 라벨 사용).
