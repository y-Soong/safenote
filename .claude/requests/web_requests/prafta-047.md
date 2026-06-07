\# 작업지시서 — 공지사항(Notice) 기능

&#x20;

> 대상: PRAFTA 서비스 연동 Claude Code

> 작성 기준: PRAFTA 기존 컨벤션(DTO 플로우, DDL 네이밍, SYS 코드 운영, `tb\_file\_info`, `tb\_cmm\_seq` 채번, master 권한 체계, 3축 권한 A-6)

> \*\*DDL은 Claude Code가 생성\*\* — 본 문서는 서비스 플로우/명세/테이블 구조 \*요구사항\*만 정의(실 DDL 미작성)

> 멀티테넌트: 모든 테이블·쿼리 `CMPNY\_CD` 스코프 필수

> 날짜 컨벤션: 날짜 `VARCHAR(8)`(YYYYMMDD), 시각 `VARCHAR(4)`(HHmm), 감사 컬럼 `INSERT\_NO/INSERT\_DATE/UPDATE\_NO/UPDATE\_DATE`, 삭제 `DEL\_YN`

&#x20;

\---

&#x20;

\## 0. 범위 요약

&#x20;

| 항목 | 결정 |

| --- | --- |

| 등록/관리 | \*\*웹 전용\*\* |

| 사용자 노출 | 웹 + 모바일(로그인 시 팝업) |

| 발행 권한 | 공통 생성 버튼 권한(`tb\_syst\_auth\_menu` 버튼별 권한)으로 통제. 고객이 Role별로 직접 ON/OFF. \*\*공지 전용 권한 규칙 신설하지 않음\*\* |

| 대상 지정 | \*\*전사 / 사업장 / 사업장+노드\*\* 다중 지정. 노드는 하위(자손) 포함 옵션. 일용직 포함 옵션 별도 |

| 대상 선택 범위 | \*\*발행자의 기존 3축 권한 스코프(A-6)를 그대로 적용\*\*. 서버에서 재검증 필수 |

| 노출 판정 기준 | \*\*수신자의 현재 소속(사업장+노드) 기준, 팝업 조회 시점 계산\*\*. 발행 시점 스냅샷 미사용 |

| 공지 보호 | 작성 시 \*\*비밀번호 필수\*\*(위임용). BCrypt 단독 해시 저장. \*\*master 관리자는 비밀번호 없이 전체 수정/삭제 가능\*\* |

| 삭제 | 논리삭제 `DEL\_YN='Y'`. 첨부파일은 별도 삭제 안 함 |

| 첨부 | `tb\_file\_info` 재사용, 다건. 공지↔파일 매핑 테이블 별도 |

| 팝업 노출 판정 | 서버 단일 쿼리로 결정(파생값 쿼리 시점 계산 원칙 준수) |

| 확인 이력 | 영구확인(CONFIRMED) / 한시숨김(SNOOZED 7일) 분리 |

| 이력 정리 배치 | \*\*팝업 기간(TO)이 지난 공지\*\*의 확인 이력만 정기 삭제. 보존 6개월 |

| 다건 팝업 | \*\*캐러셀\*\* UI |

| 일용직 | 사업장 단위로만 노출(노드 무시). 팝업 버튼은 \*\*\[확인]\[닫기]만\*\* |

| 모바일 다운로드 | 서버가 단기 만료 다운로드 토큰 발급 → \*\*앱 네이티브가 처리(별도 앱 작업 항목)\*\* |

&#x20;

\---

&#x20;

\## 1. 신규 테이블 요구사항 (DDL은 Claude Code 작성)

&#x20;

\### 1-1. `tb\_notice` — 공지사항 마스터

&#x20;

| 논리 컬럼 | 타입(권장) | NULL | KEY | 설명 |

| --- | --- | :---: | :---: | --- |

| `CMPNY\_CD` | varchar(50) | — | PK | 회사코드 |

| `NOTICE\_ID` | varchar(20) | — | PK | 공지ID (회사별 채번: N + YYYYMMDD + SEQ, `tb\_cmm\_seq` 사용) |

| `TITLE` | varchar(200) | — |  | 제목 |

| `CONTENT` | longtext | — |  | 내용(리치텍스트 가능) |

| `EDIT\_PWD` | varchar(100) | — |  | 수정 비밀번호 \*\*BCrypt 해시\*\*(평문 저장 금지) |

| `TARGET\_SCOPE` | varchar(10) | — |  | 대상 스코프 \[신규 SYS코드] `ALL`전사 / `SITE`사업장 / `NODE`사업장+노드. 상세 대상은 `tb\_notice\_target` |

| `INCLUDE\_DAILY\_YN` | varchar(1) | — |  | 일용직 포함 여부 Y/N (기본 N). §10 참조 |

| `POPUP\_YN` | varchar(1) | — |  | 로그인 시 팝업 여부 Y/N |

| `POPUP\_FROM\_YMD` | varchar(8) | Y |  | 팝업 시작일 (POPUP\_YN=Y일 때 필수) |

| `POPUP\_TO\_YMD` | varchar(8) | Y |  | 팝업 종료일 (POPUP\_YN=Y일 때 필수) |

| `PIN\_YN` | varchar(1) | — |  | 상단 고정 여부 Y/N |

| `PIN\_ORDER` | int | Y |  | 고정 순번(1부터, PIN\_YN=Y일 때만). 정규화 규칙 §5 참조 |

| `DEL\_YN` | varchar(1) | — |  | 삭제 여부 기본 N |

| `INSERT\_NO` | varchar(50) | — |  | 등록자 USER\_CD |

| `INSERT\_DATE` | datetime | — |  | 등록 일시 |

| `UPDATE\_NO` | varchar(50) | Y |  | 수정자 |

| `UPDATE\_DATE` | datetime | Y |  | 수정 일시 (※ 사용자별 "수정됨" 뱃지 판정 기준 — §7) |

&#x20;

인덱스 권장:

\- `(CMPNY\_CD, DEL\_YN, PIN\_YN, PIN\_ORDER)` — 목록 정렬

\- `(CMPNY\_CD, POPUP\_YN, POPUP\_FROM\_YMD, POPUP\_TO\_YMD)` — 팝업 후보 추출

> ⚠️ \*\*검토 의견\*\*: `POPUP\_FROM/TO`는 `PIN\_YN` 여부와 무관하게 모든 팝업 공지에 적용. 고정 공지(`PIN\_YN=Y`)도 팝업 기간 내에서만 노출되며 기간 종료 후 노출 중단. "고정 = 무기한 노출"이 아님.

> `TARGET\_SCOPE='ALL'`이면 `tb\_notice\_target`에 행이 없을 수 있음(전사는 매핑 불필요). Claude Code는 ALL일 때 대상 매핑 조회를 건너뛰도록 구현.

&#x20;

\### 1-2. `tb\_notice\_target` — 공지 대상 매핑 (공지 1 : 대상 N)

&#x20;

> `TARGET\_SCOPE`가 `SITE` 또는 `NODE`일 때만 행 존재. `ALL`이면 행 없음.

&#x20;

| 논리 컬럼 | 타입(권장) | NULL | KEY | 설명 |

| --- | --- | :---: | :---: | --- |

| `CMPNY\_CD` | varchar(50) | — | PK | 회사코드 |

| `NOTICE\_ID` | varchar(20) | — | PK | 공지ID |

| `TARGET\_SEQ` | int | — | PK | 대상 순번(1부터) |

| `SITE\_CD` | varchar(50) | — |  | 대상 사업장코드 (SITE/NODE 공통 필수) |

| `NODE\_CD` | varchar(50) | Y |  | 대상 노드코드 (NODE일 때만, SITE면 NULL=사업장 전체) |

| `INCLUDE\_DESCENDANTS\_YN` | varchar(1) | — |  | 하위(자손) 노드 포함 여부 Y/N (NODE일 때만 의미, 기본 Y) |

| `INSERT\_NO` | varchar(50) | — |  | 등록자 |

| `INSERT\_DATE` | datetime | — |  | 등록 일시 |

&#x20;

인덱스 권장:

\- `(CMPNY\_CD, SITE\_CD, NODE\_CD)` — 수신자 매칭 역방향 조회

\- `(CMPNY\_CD, NOTICE\_ID)` — 공지별 대상 조회

> 설계 노트:

> - `SITE` 스코프 = `NODE\_CD` NULL 행으로 표현(사업장 전체). `NODE` 스코프 = `NODE\_CD` 지정.

> - 하나의 공지가 여러 사업장/노드를 동시 대상으로 가질 수 있음(다중 대상).

> - `INCLUDE\_DESCENDANTS\_YN='Y'`면 지정 노드 + 모든 자손 노드 소속자가 대상.

&#x20;

\### 1-3. `tb\_notice\_file` — 공지 첨부 매핑

&#x20;

| 논리 컬럼 | 타입(권장) | NULL | KEY | 설명 |

| --- | --- | :---: | :---: | --- |

| `CMPNY\_CD` | varchar(50) | — | PK | 회사코드 |

| `NOTICE\_ID` | varchar(20) | — | PK | 공지ID |

| `FILE\_MGMT\_CD` | varchar(50) | — | PK | `tb\_file\_info` FK |

| `SORT\_IDX` | int | — |  | 첨부 정렬순서 |

| `INSERT\_NO` | varchar(50) | — |  | 등록자 |

| `INSERT\_DATE` | datetime | — |  | 등록 일시 |

&#x20;

> 실제 파일 메타는 `tb\_file\_info`에 저장. 공지용 `FILE\_TYPE` 코드값을 \*\*SYS010에 신규 추가\*\*(§3). 공지 삭제 시 이 매핑/파일은 물리 삭제하지 않음.

&#x20;

\### 1-4. `tb\_notice\_user\_ack` — 사용자 확인/숨김 이력

&#x20;

| 논리 컬럼 | 타입(권장) | NULL | KEY | 설명 |

| --- | --- | :---: | :---: | --- |

| `CMPNY\_CD` | varchar(50) | — | PK | 회사코드 |

| `NOTICE\_ID` | varchar(20) | — | PK | 공지ID |

| `USER\_CD` | varchar(20) | — | PK | 사용자코드(정규/일용 공통) |

| `ACK\_TYPE` | varchar(10) | — |  | `CONFIRMED`(영구확인) / `SNOOZED`(한시숨김) |

| `SNOOZE\_UNTIL\_YMD` | varchar(8) | Y |  | 숨김 만료일(ACK\_TYPE=SNOOZED일 때, 처리일+7일) |

| `LAST\_READ\_DATE` | datetime | Y |  | 마지막 열람 일시 (※ "수정됨" 뱃지 판정 — §7) |

| `INSERT\_NO` | varchar(50) | — |  | |

| `INSERT\_DATE` | datetime | — |  | |

| `UPDATE\_NO` | varchar(50) | Y |  | |

| `UPDATE\_DATE` | datetime | Y |  | |

&#x20;

> PK `(CMPNY\_CD, NOTICE\_ID, USER\_CD)` — 사용자×공지당 1행. 확인/숨김 변경 시 UPSERT(MySQL 8.0.20+ `INSERT ... AS NEW ... ON DUPLICATE KEY UPDATE`).

> 인덱스: `(CMPNY\_CD, USER\_CD, NOTICE\_ID)`.

> ⚠️ 일용직 USER\_CD도 여기 쌓임. 일용직 계정 만료 삭제 시 고아 행이 생기나, §9 배치로 정리되므로 허용. 일용직은 SNOOZED를 사용하지 않음(§10).

&#x20;

\---

&#x20;

\## 2. 발행 권한 \& 대상 선택 범위 (기존 3축 권한 A-6 재사용)

&#x20;

> \*\*핵심 원칙: 공지 기능은 새 권한 규칙을 만들지 않는다. 기존 화면/버튼 권한 + 조직 스코프를 그대로 따른다.\*\*

&#x20;

\### 2-1. 발행 권한

\- "신규 공지 생성"은 \*\*공통 생성 버튼 권한\*\*(`tb\_syst\_auth\_menu`의 버튼별 권한)에 연결. 고객이 Role별로 직접 ON/OFF 관리.

\- 별도 하드코딩된 Role 제한(master/hr/safe 고정 등) 두지 않음.

\### 2-2. 대상 선택 가능 범위 (발행자 스코프)

PRAFTA 권한 규칙(A-6)을 그대로 적용:

&#x20;

| 발행자 상황 | 대상 지정 가능 범위 |

| --- | --- |

| \*\*소속 사업장\*\* | 본인 소속 노드 + 그 하위(자손) 노드 |

| \*\*비소속이나 권한 보유 사업장\*\* | 해당 사업장 \*\*전 노드\*\* |

| \*\*master 관리자\*\* | 전사(모든 사업장·노드) + `TARGET\_SCOPE=ALL` 가능 |

&#x20;

\- 프론트는 발행자 스코프에 맞춰 대상 선택 트리를 제한 렌더.

\- ⚠️ \*\*서버 재검증 필수\*\*: 공지 저장 API에서 발행자가 지정한 모든 `tb\_notice\_target` 행이 발행자 스코프 내인지 \*\*기존 3축 검증 로직을 재호출하여 확인\*\*. 범위 밖 대상 지정 시 `ApiException.appendf(CommonErrorCode.COMMON\_400\_xxx, "\\n대상 권한 범위 초과 - {SITE\_CD}/{NODE\_CD}")`로 거부. 클라이언트 전달값 신뢰 금지.

\---

&#x20;

\## 3. SYS 코드 추가 요구사항

&#x20;

`tb\_syst\_val\_m` / `tb\_syst\_val\_d`에 신규 등록(코드그룹 번호는 기존 미사용 번호로 채번 — §13-1 확인):

&#x20;

| 코드그룹 | 의미 | 상세값 |

| --- | --- | --- |

| (신규) 파일타입 | SYS010에 \*\*공지 첨부\*\* 타입값 추가 | 예: `NTC` 공지첨부 |

| (신규) 대상 스코프 | `TARGET\_SCOPE` | `ALL` 전사 / `SITE` 사업장 / `NODE` 사업장+노드 |

| (신규) ACK\_TYPE | 공지 확인 유형 | `CONFIRMED` 영구확인 / `SNOOZED` 한시숨김 |

&#x20;

> ACK\_TYPE·TARGET\_SCOPE는 값이 적어 코드 테이블 대신 컬럼 상수로 둬도 무방 — Claude Code가 기존 패턴(코드 테이블 vs 컬럼 상수)에 맞춰 일관 결정.

&#x20;

\---

&#x20;

\## 4. DTO 플로우 (기존 컨벤션 준수)

&#x20;

`request → param → query/command → result → response`

\- 리스트 요청(첨부 다건, \*\*대상 다건\*\*)은 \*\*별도 Model 객체\*\* 경유

\- `Param`/`Command`/`Query`/`Result`는 record + static `from()`

\- 필수값 검증은 `ApiException.appendf(CommonErrorCode.COMMON\_400\_001, "\\n필수값 누락 - ...")`

\- MyBatis XML 헤더 주석 `/\* MapperName.methodName \*/`

\- `gvCmpnyCd`/`gvUserId`는 JWT(`tokenInfo`)에서 주입 — 클라이언트 전달값(특히 `CMPNY\_CD`) 신뢰 금지

\### 4-1. 주요 엔드포인트별 DTO 구성

&#x20;

| 엔드포인트 | Request | Param | Query/Command | Result | Response |

| --- | --- | --- | --- | --- | --- |

| 공지 목록 조회 (관리) | `NoticeListRequest`(검색조건) | `NoticeListParam` | `NoticeListQuery` | `NoticeResult`(+사용자별 읽음/수정 플래그) | `NoticeListResponse` |

| 공지 단건 조회(상세/팝업) | path/param | `NoticeDetailParam` | `NoticeDetailQuery` | `NoticeResult` + `NoticeFileResult` list + `NoticeTargetResult` list | `NoticeDetailResponse` |

| 공지 생성 | `NoticeSaveRequest`(+첨부 list→`NoticeFileModel`, +대상 list→`NoticeTargetModel`) | `NoticeSaveParam` | `NoticeSaveCommand`, `NoticeTargetSaveCommand`, `NoticeFileSaveCommand` | — | 201/200 |

| 공지 수정 | `NoticeSaveRequest`(+`editPwd`, +대상 list) | `NoticeSaveParam` | `NoticeUpdateCommand`, 대상 재설정 Command | — | 200 |

| 비밀번호 검증 | `NoticePwdRequest` | `NoticePwdParam` | `NoticePwdQuery`(해시 조회) | `NoticePwdResult` | `NoticePwdResponse` |

| 공지 삭제 | path/param | `NoticeDeleteParam` | `NoticeDeleteCommand`(DEL\_YN=Y) | — | 200 |

| 발행자 대상선택 트리 조회 | header(JWT) | `NoticeScopeParam` | `NoticeScopeQuery`(발행자 스코프) | `NoticeScopeResult`(선택가능 사업장/노드 트리) | `NoticeScopeResponse` |

| 로그인 팝업 대상 조회 (web/mobile 공통) | header(JWT) | `NoticePopupParam` | `NoticePopupQuery` | `NoticePopupResult` list | `NoticePopupResponse` |

| 확인 처리(CONFIRMED) | `NoticeAckRequest` | `NoticeAckParam` | `NoticeAckCommand`(UPSERT) | — | 200 |

| 일주일 숨김(SNOOZED) | `NoticeAckRequest` | `NoticeAckParam` | `NoticeSnoozeCommand`(UPSERT, SNOOZE\_UNTIL=오늘+7) | — | 200 |

| 첨부 다운로드 토큰 발급 | path/param | `NoticeFileDlParam` | `NoticeFileQuery` | `NoticeFileResult`(+단기토큰) | `NoticeFileDlResponse` |

&#x20;

> 대상 다건은 `NoticeTargetModel`(siteCd, nodeCd, includeDescendantsYn)을 리스트로 받아 `NoticeSaveParam` 내부에 담는다(ChkptInfoModel 패턴 동일).

&#x20;

\---

&#x20;

\## 5. 상단 고정 순번 정규화 규칙 (서버 강제)

&#x20;

클라이언트 `PIN\_ORDER`를 그대로 신뢰하지 않고 서버 정규화:

&#x20;

1\. \*\*목록 표시 정렬\*\*: `ORDER BY PIN\_YN DESC, PIN\_ORDER ASC, INSERT\_DATE DESC` (고정 우선, 순번 오름차순, 비고정은 최신순)

2\. \*\*순번 압축\*\*: 고정 공지 N개일 때 표시 위치는 1..N 연속. 사용자가 "10번" 지정해도 현재 고정이 2개뿐이면 \*\*3번째에 배치\*\*.

&#x20;  - 구현: 저장 시 `min(요청PIN\_ORDER, 현재고정수+1)`로 클램프 후 동일/이후 순번 +1 시프트(또는 저장 후 전체 재정렬 UPDATE).

3\. \*\*삭제/해제 시 재정렬\*\*: 고정 공지 삭제·고정 해제 시 남은 고정 공지 `PIN\_ORDER`를 1..M 재압축.

4\. \*\*동시성\*\*: 순번 시프트는 단일 트랜잭션 + `CMPNY\_CD` 단위 `SELECT ... FOR UPDATE`.

\---

&#x20;

\## 6. 로그인 팝업 노출 판정 로직 (핵심 — 대상 필터 포함)

&#x20;

`POST /notice/popup` — 로그인 직후 web/mobile 공통 호출. 수신자의 \*\*현재 소속(사업장+노드) 기준\*\*으로 다음을 모두 만족하는 공지 반환.

&#x20;

\### 6-1. 대상(수신자) 매칭 조건

수신자 컨텍스트: `gvCmpnyCd`, `gvUserId`, 현재 로그인 사업장 `curSiteCd`, 소속 노드 `curNodeCd`, 일용직 여부 `isDaily`.

&#x20;

공지가 수신자에게 도달하는 조건:

\- `TARGET\_SCOPE='ALL'` → 회사 내 전원 도달.

\- `TARGET\_SCOPE='SITE'` → `tb\_notice\_target`에 `SITE\_CD=curSiteCd AND NODE\_CD IS NULL` 행 존재.

\- `TARGET\_SCOPE='NODE'` (정규직만) → `tb\_notice\_target` 행 중

&#x20; - `SITE\_CD=curSiteCd AND NODE\_CD=curNodeCd` (직접 매칭), 또는

&#x20; - `SITE\_CD=curSiteCd AND INCLUDE\_DESCENDANTS\_YN='Y' AND curNodeCd가 NODE\_CD의 자손` (트리 하향 매칭).

&#x20;   - 자손 판정: `tb\_site\_node`의 `PARENT\_NODE\_CD` 트리 재귀(MySQL 8 재귀 CTE) 또는 경로 기반. Claude Code가 기존 조직 트리 조회 방식과 일관되게 구현.

\### 6-2. 일용직 처리

\- 일용직(`isDaily=true`)은 \*\*노드 매칭 제외\*\*. `TARGET\_SCOPE='NODE'` 공지는 일용직에게 노출하지 않음.

\- 일용직은 `TARGET\_SCOPE='ALL'` 또는 `TARGET\_SCOPE='SITE'(현재 점유 사업장)` 공지 중, \*\*공지의 `INCLUDE\_DAILY\_YN='Y'`\*\* 인 것만 노출.

\- 일용직 노출 공지의 팝업 버튼은 \*\*\[확인]\[닫기]만\*\*(SNOOZED 미제공) — §10.

\### 6-3. 노출 제외(이력) 조건

\- \*\*한시 숨김(SNOOZED)\*\*: `tb\_notice\_user\_ack`에 본인의 `ACK\_TYPE='SNOOZED' AND SNOOZE\_UNTIL\_YMD >= today` 행 있으면 제외(정규직 한정).

\- \*\*영구 확인(CONFIRMED)\*\*: 비고정 공지(`PIN\_YN='N'`)에서 본인 `ACK\_TYPE='CONFIRMED'` 행 있으면 제외. \*\*고정 공지(`PIN\_YN='Y'`)는 CONFIRMED 무시(계속 노출)\*\*.

\### 6-4. 공통 게이트

\- `DEL\_YN='N'`, `POPUP\_YN='Y'`, `today BETWEEN POPUP\_FROM\_YMD AND POPUP\_TO\_YMD`.

\### 6-5. 의사 쿼리 구조

```

SELECT n.\*

FROM tb\_notice n

WHERE n.CMPNY\_CD = :gvCmpnyCd

&#x20; AND n.DEL\_YN = 'N'

&#x20; AND n.POPUP\_YN = 'Y'

&#x20; AND :today BETWEEN n.POPUP\_FROM\_YMD AND n.POPUP\_TO\_YMD

&#x20; -- (A) 대상 매칭: §6-1 (+ 일용직이면 §6-2 규칙으로 NODE 제외 \& INCLUDE\_DAILY\_YN='Y' 강제)

&#x20; AND ( /\* TARGET\_SCOPE / tb\_notice\_target / 노드 트리 매칭 서브쿼리 \*/ )

&#x20; -- (B) 한시 숨김 제외 (정규직)

&#x20; AND NOT EXISTS (SELECT 1 FROM tb\_notice\_user\_ack a

&#x20;                  WHERE a.CMPNY\_CD=n.CMPNY\_CD AND a.NOTICE\_ID=n.NOTICE\_ID

&#x20;                    AND a.USER\_CD=:gvUserId AND a.ACK\_TYPE='SNOOZED'

&#x20;                    AND a.SNOOZE\_UNTIL\_YMD >= :today)

&#x20; -- (C) 비고정 + CONFIRMED 제외

&#x20; AND ( n.PIN\_YN='Y'

&#x20;       OR NOT EXISTS (SELECT 1 FROM tb\_notice\_user\_ack a2

&#x20;                       WHERE a2.CMPNY\_CD=n.CMPNY\_CD AND a2.NOTICE\_ID=n.NOTICE\_ID

&#x20;                         AND a2.USER\_CD=:gvUserId AND a2.ACK\_TYPE='CONFIRMED') )

ORDER BY n.PIN\_YN DESC, n.PIN\_ORDER ASC, n.INSERT\_DATE DESC

```

&#x20;

\### 6-6. 노출 규칙 정리

| 공지 유형 | 동작 | 버튼(정규직) | 버튼(일용직) |

| --- | --- | --- | --- |

| \*\*고정(PIN\_YN=Y)\*\* | 기간 내 매 로그인 노출(CONFIRMED 불가), SNOOZED 7일은 적용 | \[일주일간 보지 않기] \[닫기] | \[확인] \[닫기] |

| \*\*비고정(PIN\_YN=N)\*\* | \[확인]→이후 미노출, \[닫기]→재노출 | \[확인] \[닫기] | \[확인] \[닫기] |

&#x20;

\- `\[닫기]`: 이력 없음(다음 로그인 재노출).

\- `\[확인]`: `ACK\_TYPE=CONFIRMED` UPSERT.

\- `\[일주일간 보지 않기]`: `ACK\_TYPE=SNOOZED`, `SNOOZE\_UNTIL\_YMD=오늘+7` UPSERT (정규직·고정 공지 한정).

\- 팝업 열람 시 `LAST\_READ\_DATE` 갱신(§7).

\---

&#x20;

\## 7. "수정됨/미열람" 뱃지 — 사용자별 계산

&#x20;

> 공지 관리/목록 화면은 \*\*관리자·사용자 양쪽이 보는 화면\*\*. 뱃지는 \*\*조회하는 본인 기준\*\* 계산.

&#x20;

| 플래그 | 계산식 |

| --- | --- |

| `isUnread` | 본인의 `tb\_notice\_user\_ack.LAST\_READ\_DATE` 없음(NULL/행 없음) |

| `isUpdated` | `LAST\_READ\_DATE` 존재 AND `tb\_notice.UPDATE\_DATE > LAST\_READ\_DATE` |

&#x20;

\- 둘 중 하나라도 true → 목록에서 \*\*NEW/UPDATE 뱃지 표시\*\*.

\- 사용자가 다시 열람 → `LAST\_READ\_DATE` 현재시각 갱신 → 뱃지 소멸.

\- 목록 쿼리에서 `tb\_notice` LEFT JOIN `tb\_notice\_user\_ack`(USER\_CD=본인) 한 번에 계산.

\- ⚠️ 저장 컬럼 아님, \*\*조회 시 계산\*\*. `tb\_notice`에 "수정됨" 상태 컬럼 두지 말 것(사용자마다 값이 다름).

\---

&#x20;

\## 8. 수정 정책

&#x20;

1\. \*\*POPUP\_YN='Y' 공지\*\*: 기본 \*\*팝업 기간 중 내용 수정 차단\*\*(사용자가 본 내용과 달라지는 혼란 방지).

&#x20;  - 수정 가능: 게시 \*\*전(POPUP\_FROM 이전)\*\* 또는 \*\*후(POPUP\_TO 경과)\*\*.

&#x20;  - \*\*예외 옵션(YJ 결정)\*\*: 기간 중 수정 허용 + 재노출 사유 문구 입력 시 CONFIRMED 이력 초기화/무시하여 재팝업.

&#x20;    - 구현: ⓐ 기간 중 수정 전면 차단(MVP 기본) / ⓑ 사유 입력 시 재노출 허용(플래그 토글, 확장 포인트 주석).

2\. \*\*비밀번호 검증\*\*: 일반 관리자는 수정/삭제 전 `EDIT\_PWD` BCrypt 검증 통과 필요.

3\. \*\*master 관리자\*\*: 비밀번호 불필요. 수정 팝업 진입 시 \*\*비밀번호 입력 컴포넌트 비활성화(disabled)\*\*. 전 공지 수정/삭제 가능. master 판별은 서버에서(클라이언트 플래그 신뢰 금지).

4\. \*\*대상 수정\*\*: 수정 시 대상(`tb\_notice\_target`)도 변경 가능. 단 §2-2 발행자 스코프 재검증 동일 적용.

\---

&#x20;

\## 9. 삭제 정책 \& 이력 정리 배치

&#x20;

\### 삭제

\- 공지 삭제 = `tb\_notice.DEL\_YN='Y'` 논리삭제.

\- 첨부(`tb\_file\_info`)·매핑(`tb\_notice\_file`/`tb\_notice\_target`)·확인 이력(`tb\_notice\_user\_ack`)은 \*\*물리 삭제 안 함\*\*.

\- 삭제 공지는 목록/팝업 모두 제외(`DEL\_YN='N'` 필터 일괄).

\### 이력 정리 배치

\- \*\*대상\*\*: `POPUP\_TO\_YMD < (오늘 - 6개월)` 공지의 `tb\_notice\_user\_ack` 행.

&#x20; - 사유: 팝업 종료 6개월 경과 공지는 재노출 없음 → 이력 안전 삭제.

&#x20; - ⚠️ \*\*현행 노출 중 공지의 CONFIRMED 이력 절대 삭제 금지\*\*(삭제 시 확인한 사용자에게 재팝업되는 버그).

\- \*\*주기\*\*: 일/주 배치(Claude Code 스케줄 확정).

\- SNOOZED 만료 행은 쿼리(`SNOOZE\_UNTIL\_YMD >= today`)로 걸러지므로 별도 정리 불필요(원하면 배치 포함 가능).

\---

&#x20;

\## 10. 일용직(Daily User) 처리 상세

&#x20;

\- 일용직은 노드 소속이 불명확하므로 \*\*노드 단위 공지(`TARGET\_SCOPE='NODE'`) 대상에서 제외\*\*.

\- 일용직 노출 조건: 공지 `INCLUDE\_DAILY\_YN='Y'` AND (`TARGET\_SCOPE='ALL'` OR (`TARGET\_SCOPE='SITE'` AND 현재 점유 사업장 매칭)).

\- 일용직 팝업 버튼은 \*\*\[확인]\[닫기]만\*\* 제공. \*\*\[일주일간 보지 않기](SNOOZED) 미제공\*\* — 일용직 계정 수명이 짧아 무의미.

&#x20; - 즉 일용직은 `ACK\_TYPE='CONFIRMED'`만 기록(비고정 공지). 고정 공지는 일용직에게도 매 로그인 노출되나 SNOOZED 없음.

\- 일용직 확인 이력은 계정 만료 시 고아 행이 되나 §9 배치로 정리.

\---

&#x20;

\## 11. 첨부파일 다운로드 (모바일 포함)

&#x20;

\### 서버

\- 첨부 다운로드 요청 시 \*\*단기 만료 다운로드 토큰/서명 URL\*\* 발급(만료 짧게).

\- 다운로드 행위는 `tb\_audit\_log`(ACTION\_TYPE 다운로드)에 기록 — 기존 감사 패턴 준수.

\### 앱(별도 작업 항목)

> ⚠️ \*\*선행/병행 필수\*\*: 웹뷰(Flutter+Vue)에서 iOS 다운로드는 웹뷰 직접 처리로 보장 안 됨. \*\*Flutter 네이티브 다운로드 핸들러 구현 전제\*\*.

\- 네이티브가 URL/토큰 수신 → OS 다운로드/공유시트(iOS: share sheet/`UIDocumentInteractionController`, Android: DownloadManager) 호출.

\- 웹뷰가 파일 스트림 직접 수신 금지(대용량 메모리·iOS 제약 회피).

\- \*\*이 항목 누락 시 "서버는 됐는데 iOS 다운로드 안 됨" 발생\*\* → 앱 백로그로 별도 티켓화.

\---

&#x20;

\## 12. 화면 명세

&#x20;

\### 12-1. 공지사항 관리 화면 (웹)

\- \*\*목록 컬럼\*\*: 제목 / 내용(말줄임 `...`) / 첨부 여부(아이콘) / \*\*대상 요약\*\*(전사 / 사업장명 / 노드명 외 N건) / 등록 일시 / 등록자 / (사용자별)뱃지(NEW·UPDATE)

&#x20; - `PIN\_YN`은 컬럼 미노출, \*\*고정 공지 row 배경 연한 붉은색\*\*.

&#x20; - 정렬: §5 규칙.

\- \*\*신규 생성 버튼\*\*(공통 생성 버튼 권한) → 생성 팝업.

\- \*\*row 더블클릭\*\* → 조회/수정 팝업(§12-3).

\### 12-2. 신규 공지 생성 팝업 (웹)

\- 제목 / 내용(리치텍스트).

\- \*\*비밀번호 설정(필수)\*\* — 저장 시 BCrypt.

\- \*\*대상 지정\*\*:

&#x20; - 스코프 선택: 전사 / 사업장 / 사업장+노드(다중).

&#x20; - 대상 선택 트리는 \*\*발행자 스코프(§2-2)로 제한 렌더\*\*. 다중 선택.

&#x20; - 노드 선택 시 \*\*하위 노드 포함 체크박스\*\*(`INCLUDE\_DESCENDANTS\_YN`, 기본 체크).

&#x20; - \*\*일용직 포함 체크박스\*\*(`INCLUDE\_DAILY\_YN`, 기본 해제).

\- \*\*로그인 시 팝업 여부\*\* 토글 → ON 시 \*\*팝업 기간(FROM\~TO)\*\* 필수.

\- \*\*상단 고정\*\* 토글 → ON 시 \*\*고정 순번 입력\*\*(§5 서버 보정).

\- \*\*파일 첨부\*\* — 다건.

\### 12-3. 기존 공지 조회/수정 팝업 (웹)

\- 조회: 제목/내용/첨부(다운로드)/대상 표시.

\- \*\*비밀번호 입력 → 검증 통과 시 수정 모드\*\*.

\- \*\*master\*\*: 비밀번호 컴포넌트 \*\*비활성화\*\*, 즉시 수정.

\- 수정 범위 §8, 대상 수정 시 §2-2 재검증.

\### 12-4. 로그인 팝업 (web/mobile 공통)

\- 표시: 제목 / 내용 / 첨부 / 게시일자.

\- \*\*다건 노출 시 캐러셀\*\*(좌우 이동, 인디케이터). 모바일 좁은 폭(\~380px)·웹 양쪽 대응, UI 품질 중요.

\- 하단 버튼: §6-6 표 기준(정규직/일용직, 고정/비고정 분기).

\- 노출/열람 시 `LAST\_READ\_DATE` 갱신.

\---

&#x20;

\## 13. 미해결/확정 필요 항목 (Claude Code 진행 전 체크)

&#x20;

1\. \*\*SYS 코드 그룹 번호\*\*: 공지 첨부 파일타입(SYS010 신규값), TARGET\_SCOPE, ACK\_TYPE 운영 방식(코드 테이블 vs 컬럼 상수) — 기존 패턴에 맞춰 확정.

2\. \*\*수정 정책 ⓐ/ⓑ\*\*: MVP는 ⓐ(기간 중 수정 차단) 기본. ⓑ(사유 입력 재노출) 토글 여부 확인.

3\. \*\*캐러셀 다건 노출 상한\*\*: 로그인 시 노출 공지 과다 시 상한(예: 최대 N건) 둘지 — 운영 판단.

4\. \*\*다운로드 토큰 구현 방식\*\*: 파일 저장소(로컬/S3)에 따라 presigned URL vs 자체 토큰.

5\. \*\*앱 네이티브 다운로드 핸들러\*\*: 별도 앱 작업 티켓 생성(§11).

6\. \*\*노드 자손 매칭 구현 방식\*\*: 재귀 CTE vs 경로 컬럼 — 기존 조직 트리 조회 방식과 일관되게.

7\. \*\*일용직 현재 점유 사업장 판정\*\*: 팝업 조회 시 일용직의 "현재 사업장"을 어느 테이블/세션값에서 가져올지 확정(`tb\_daily\_user.SITE\_CD` 또는 슬롯/토큰 컨텍스트).

&#x20;



