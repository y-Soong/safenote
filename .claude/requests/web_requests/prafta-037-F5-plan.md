# prafta-037-F5 — 다운로드 감사 로그 인프라 + 양식 다운로드 1차 적재 : 작업 분해 계획

> 작성: planner 세션 (2026-05-29).
> 원본 요청서: `.claude/requests/web_requests/prafta-037.md` §5 (F5 다운로드 감사 로그).
> 확정 결정 단일 출처: 본 문서 §1 (사용자 확정 + planner 권장). 원본 요청서는 본 문서로 **대체(supersede)** 된다.
> 정책서 참조: 공통 §11.3 (감사 — 다운로드/상세 위치 조회는 명시적 감사 대상).

---

## 0. 개요

- prafta-036 의 양식 다운로드(`GET /webApi/user01/user-create-template`) 는 현재 `log.info` 만 적재한다. 정책서 §11.3 이 다운로드를 감사 대상으로 명시하므로 별도 감사 테이블 적재가 필요.
- prafta 백엔드에 기존 감사 인프라 0건 (grep 결과: `tb_audit*`, `tb_*audit*`, `tb_*log*` 모두 schema-full.sql 미스).
- 본 작업은 신규 `tb_audit_log` 테이블 + 공통 `AuditLogService` 인프라를 도입하고 양식 다운로드부터 1차 적재한다.
- 다른 다운로드/권한 변경/상태 변경/조직 변경/삭제/상세 위치 조회 적재는 §7 follow-up.

---

## 1. 사용자 확정 결정 + planner 권장 (2026-05-29)

| # | 결정 | 내용 |
|---|---|---|
| D1 | **인프라 방향** | 신규 `tb_audit_log` 테이블 도입(사용자 확정). 양식 다운로드 1차 적재. 향후 다른 액션은 같은 인프라 재사용. |
| D2 | **본 작업 1차 적재 대상** | `GET /webApi/user01/user-create-template` 만(사용자 확정). 단건 생성/엑셀 업로드/사용자 권한 변경/사용자 리스트 export 등은 §7 follow-up. |
| P1 | **PK 채번** (planner 권장) | `AUDIT_ID varchar(25)` + `'A' + YYYYMMDD + FNC_CMM_SEQ_NEXTVAL(cmpnyCd,'AUDIT_LOG_ID')`. prafta-031 NOTI_ID 패턴(`'N' + YYYYMMDD + SEQ`) 일치. auto_increment 아님 — prafta 컨벤션 준수. |
| P2 | **ACTION_TYPE SYS코드** (planner 권장) | SYS060 신설. 본 작업 시드 1건: `01=다운로드`. 향후 `02=권한변경`, `03=상태변경`, `04=조직변경`, `05=삭제`, `06=상세위치조회` 등은 follow-up 에서 추가. |
| P3 | **RESOURCE_TYPE** (planner 권장) | 자유 varchar(50) + 상수 카탈로그(`AuditResourceType.USER_CREATE_TEMPLATE` 등). SYS코드 미사용(고정 enum 카탈로그). |
| P4 | **IP 추출 방식** (planner 권장) | `X-Forwarded-For` 1순위(첫 IP, 콤마 분리 시 trim), 없으면 `HttpServletRequest.getRemoteAddr()`. 신규 `ClientIpExtractor` 유틸 도입(현재 prafta 백엔드에 IP 추출 0건). |
| P5 | **보존 정책** (planner 권장) | 영구 보관(자동 삭제 잡 없음). 자동 삭제는 §7 follow-up. |
| P6 | **본업 영향 모델** (planner 권장) | 감사 적재 실패는 본업(양식 다운로드)을 막지 않음. Service 내부 try/catch + `log.error`. 본업은 항상 정상 진행. |
| P7 | **트랜잭션 모델** (planner 권장) | `@Transactional(propagation=REQUIRES_NEW)` 독립 트랜잭션. 본업 트랜잭션과 분리. |
| P8 | **PII 처리** (planner 권장) | `DETAIL` JSON 컬럼에 평문 PII(이름/휴대폰/이메일/생년월일) 금지. 본 작업 1차 적재는 PII 없음(`DETAIL=NULL`). |
| P9 | **USER_AGENT 저장** (planner 권장) | varchar(500) nullable. 트림 후 500자 이상이면 절단. |
| P10 | **적재 위치** (planner 권장) | Service 안 권한 가드 통과 직후 적재. Controller 가 `HttpServletRequest` 로부터 `AuditContext(ipAddress, userAgent)` 만들어 Service 에 전달 — Service 계층이 `HttpServletRequest` 직접 의존 회피. |
| P11 | **감사 액션 ACTION_TYPE 저장 형식** (planner 권장) | SYS060 코드값(`01`) 그대로 저장. 의미는 `AuditActionType.DOWNLOAD = "01"` 상수로 매핑. |

---

## 2. 초안에서 잘라낸 / 보류한 항목

| 초안/유사요구 | 처리 | 이유 |
|---|---|---|
| 다른 다운로드(사용자 리스트 export, Attd_07 export 등) 동시 적재 | **§7 follow-up** | 1차는 양식 다운로드만(D2). 인프라가 갖춰지면 한 줄 호출로 추가 가능. |
| prafta-036 단건 생성/엑셀 업로드 적재 | **§7 follow-up** | 권한 변경 적재로 분류되며, ACTION_TYPE=`02 권한변경` + `03 상태변경` 코드 추가가 필요. F5 1차 범위 밖. |
| 비동기 적재 (큐 도입) | **불채택** | INSERT 1건이라 동기로 충분. |
| 감사 조회 화면(운영자용) | **§7 follow-up** | 본 작업은 적재만. 조회는 별도 PR. |
| 보존 자동 삭제 잡 | **§7 follow-up** | 영구 보관(P5). |
| `@Auditable` AOP 화 | **§7 follow-up** | 1차는 명시적 호출. 6~7개 적재처가 모인 후 AOP 검토. |
| 외부 SIEM 연계 / 모니터링 | **§7 follow-up** | 본 작업 범위 밖. |

---

## 3. 영향 범위

### 3.1 스키마 / 마이그레이션

| 작업 | 파일 | 변경 |
|---|---|---|
| 신규 테이블 `tb_audit_log` | `prafta-backend/src/main/resources/sql/migration/prafta-037-F5-audit-log.sql` (신규) | CREATE TABLE — 아래 §3.1.1 확정안. |
| 신규 채번 키 `AUDIT_LOG_ID` | 같은 파일 안 | `FNC_CMM_SEQ_NEXTVAL` 채번 키는 별도 INSERT 불필요(시퀀스 함수가 회사별 자동 채번). prafta-031 NOTI_OUTBOX_ID 패턴과 동일. |
| SYS060 마스터 추가 | 같은 파일 안 | `INSERT INTO tb_syst_val_m (SYST_VAL_CD='SYS060', SYST_VAL_NM='감사 액션 유형', USE_YN='Y', VAL_DESC='tb_audit_log.ACTION_TYPE 코드', INSERT_NO='SYSTEM')`. |
| SYS060 디테일 시드 1건 | 같은 파일 안 | `INSERT INTO tb_syst_val_d (SYST_VAL_CD='SYS060', SYST_VAL_D_CD='01', SYST_VAL_D_NM='다운로드', SORT_IDX=1, USE_YN='Y', INSERT_NO='SYSTEM')`. |

#### 3.1.1 `tb_audit_log` 스키마 확정안

```sql
CREATE TABLE `tb_audit_log` (
    `AUDIT_ID`        varchar(25)   NOT NULL COMMENT '감사 로그 ID (PK, 회사별 채번: A + YYYYMMDD + SEQ)',
    `CMPNY_CD`        varchar(50)   NOT NULL COMMENT '회사 코드',
    `USER_CD`         varchar(20)   NULL     COMMENT '행위자 사용자 코드(비로그인 행위는 NULL)',
    `ACTION_TYPE`     varchar(30)   NOT NULL COMMENT '감사 액션 유형[SYS060] 01:다운로드',
    `RESOURCE_TYPE`   varchar(50)   NOT NULL COMMENT '대상 리소스 유형 (예: USER_CREATE_TEMPLATE)',
    `RESOURCE_KEY`    varchar(200)  NULL     COMMENT '대상 리소스 식별자(양식 다운로드는 NULL)',
    `IP_ADDRESS`      varchar(45)   NULL     COMMENT '요청 IP (IPv6 지원, 추출 실패 시 NULL)',
    `USER_AGENT`      varchar(500)  NULL     COMMENT '요청 User-Agent',
    `DETAIL`          json          NULL     COMMENT '추가 페이로드(JSON, PII 평문 금지)',
    `DEL_YN`          varchar(1)    NOT NULL DEFAULT 'N' COMMENT '삭제 여부(감사는 무삭제 원칙)',
    `INSERT_NO`       varchar(50)   NOT NULL COMMENT '등록자(=USER_CD or SYSTEM)',
    `INSERT_DATE`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    PRIMARY KEY (`AUDIT_ID`),
    KEY `IX_AUDIT_LOG_TIME` (`CMPNY_CD`, `INSERT_DATE`),
    KEY `IX_AUDIT_LOG_USER` (`CMPNY_CD`, `USER_CD`, `INSERT_DATE`),
    KEY `IX_AUDIT_LOG_ACTION` (`CMPNY_CD`, `ACTION_TYPE`, `INSERT_DATE`),
    KEY `IX_AUDIT_LOG_RESOURCE` (`CMPNY_CD`, `RESOURCE_TYPE`, `INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='감사 로그 (다운로드/권한 변경/상태 변경 등)';
```

### 3.2 백엔드

#### 신규 패키지 / 파일 — 공통 인프라

| 영역 | 파일 | 변경 |
|---|---|---|
| Service 인터페이스 | `common/cmm/audit/service/AuditLogService.java` (신규) | `void record(AuditLogCommand command, AuditContext context)` 단일 메서드. |
| Service 구현 | `common/cmm/audit/service/impl/AuditLogServiceImpl.java` (신규) | `@Service @RequiredArgsConstructor`. `@Transactional(propagation=REQUIRES_NEW)`. 내부 try/catch + `log.error` (본업 영향 차단). VO 만들어 mapper 호출. |
| Command | `common/cmm/audit/command/AuditLogCommand.java` (신규) | `@Builder` 또는 `record`. 필드: `cmpnyCd, userCd, actionType, resourceType, resourceKey, detailJson`. |
| Context | `common/cmm/audit/AuditContext.java` (신규) | `record AuditContext(String ipAddress, String userAgent)`. Controller 에서 만들어 Service 에 전달. |
| VO | `common/cmm/audit/vo/AuditLogInsertVO.java` (신규) | Lombok `@Getter @Setter`. mapper INSERT 파라미터. `auditId/cmpnyCd/userCd/actionType/resourceType/resourceKey/ipAddress/userAgent/detailJson/insertNo`. |
| Mapper | `common/cmm/audit/mapper/AuditLogMapper.java` (신규) | `String selectNextAuditId(@Param("cmpnyCd") String cmpnyCd)`, `void insertAuditLog(AuditLogInsertVO vo)`. |
| Mapper XML | `src/main/resources/com/prafta/common/cmm/audit/mapper/AuditLogMapper.xml` (신규) | `selectNextAuditId` — `SELECT CONCAT('A', DATE_FORMAT(NOW(),'%Y%m%d'), FNC_CMM_SEQ_NEXTVAL(#{cmpnyCd}, 'AUDIT_LOG_ID')) FROM DUAL`. `insertAuditLog` — 명시 컬럼/leading comma/`#{}`. |
| 상수 카탈로그 | `common/cmm/audit/AuditActionType.java` (신규) | `public final class AuditActionType { public static final String DOWNLOAD = "01"; }`. 향후 02~06 추가. |
| 상수 카탈로그 | `common/cmm/audit/AuditResourceType.java` (신규) | `public final class AuditResourceType { public static final String USER_CREATE_TEMPLATE = "USER_CREATE_TEMPLATE"; }`. 향후 다른 리소스 추가. |
| IP 추출 유틸 | `common/util/ClientIpExtractor.java` (신규) | `public static String extract(HttpServletRequest request)`. X-Forwarded-For 1순위 + 콤마 분리 시 첫 IP trim. 없거나 빈 값이면 `getRemoteAddr()`. 모두 실패 시 null. |

#### 변경 파일 — 1차 적재 결선

| 영역 | 파일 | 변경 |
|---|---|---|
| Controller | `web/user/user01/controller/User01Controller.java` | `getUserCreateTemplate` 시그니처에 `HttpServletRequest httpRequest` 추가. `ClientIpExtractor.extract(httpRequest)` + `httpRequest.getHeader("User-Agent")` (≤500자 절단) 로 `AuditContext` 생성 후 Service 호출 시그니처에 전달. 응답 form 무변경. |
| Service 인터페이스 | `web/user/user01/service/User01Service.java` | `byte[] buildUserCreateTemplate(TokenInfo tokenInfo, AuditContext auditContext)` — 인자 추가. |
| Service 구현 | `web/user/user01/service/impl/User01ServiceImpl.java` | 권한 가드 통과 직후, `auditLogService.record(AuditLogCommand.builder()....build(), auditContext)` 한 줄 호출. 기존 `log.info` 유지(이중 로깅). 의존성 `private final AuditLogService auditLogService` 주입. |

#### 변경하지 않는 파일

- 기존 `LoginServiceImpl`/다른 user01 endpoint: 무변경. F5 1차 적재 대상은 양식 다운로드 한 endpoint.

### 3.3 프론트엔드

**무변경**. 본 작업은 백엔드 적재만.

---

## 4. 작업 단위 분해 (권장 착수 순서)

> 권장 순서: **F5-1 → F5-2 → F5-3** 직렬. 병렬화 불가.

---

### PRAFTA-037-F5-1 — 스키마 신설 + SYS060 시드

- **유형**: backend (DB 마이그레이션만)
- **영역**: web
- **모듈**: common/cmm (시드)
- **작업 유형**: 신규
- **목적**: 신규 `tb_audit_log` 테이블 + SYS060(감사 액션 유형) 마스터/디테일 시드 1건(`01=다운로드`).
- **산출물**:
  - `prafta-backend/src/main/resources/sql/migration/prafta-037-F5-audit-log.sql` (신규)
    - 헤더 주석(작성일/적용 환경/변경 요약/적용 전 부재 확인 쿼리)
    - CREATE TABLE `tb_audit_log` (§3.1.1 확정안 그대로)
    - INSERT SYS060 마스터 1건
    - INSERT SYS060 디테일 1건 (`01=다운로드`)
    - 패턴은 `prafta-031-noti-outbox.sql` / `prafta-031-sys045-noti-type.sql` 헤더 형식 그대로.
- **핵심 파일**:
  - `prafta-backend/src/main/resources/sql/migration/prafta-037-F5-audit-log.sql`
- **정책서/스키마 출처**:
  - 정책서 공통 §11.3 (감사 — 다운로드/상세 위치 조회).
  - 참조 패턴: `prafta-031-noti-outbox.sql`(테이블 + INDEX), `prafta-031-sys045-noti-type.sql`(SYS코드 시드).
- **의존성**: 없음
- **운영 적용**: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.

---

### PRAFTA-037-F5-2 — 공통 감사 인프라 (`AuditLogService` + IP 유틸 + 상수 카탈로그)

- **유형**: backend
- **영역**: web
- **모듈**: common/cmm/audit (신규 패키지)
- **작업 유형**: 신규
- **목적**: 한 줄 호출(`auditLogService.record(command, context)`)로 INSERT 1행 적재 가능한 공통 인프라. 본업 영향 0(내부 try/catch + REQUIRES_NEW).
- **산출물**:
  - `AuditLogService` (인터페이스) — 단일 메서드 `void record(AuditLogCommand command, AuditContext context)`.
  - `AuditLogServiceImpl` — `@Service @RequiredArgsConstructor @Slf4j`. 메서드는 `@Transactional(propagation=Propagation.REQUIRES_NEW, rollbackFor=Exception.class)` 단, 내부 전체 try/catch 로 본업 영향 차단(catch 시 `log.error` 만 남기고 정상 return).
    1. `cmpnyCd/actionType/resourceType` null/blank 검증 (위반 시 `log.warn` + return — 호출 측 본업은 무영향)
    2. `selectNextAuditId(cmpnyCd)` 채번
    3. `AuditLogInsertVO` 생성(`insertNo = command.userCd() ?? "SYSTEM"`)
    4. `insertAuditLog(vo)`
    5. `log.debug` 한 줄
  - `AuditLogCommand` — Lombok `@Builder` + `@Getter`(또는 record). 필드: `cmpnyCd, userCd, actionType, resourceType, resourceKey, detailJson`.
  - `AuditContext` — `public record AuditContext(String ipAddress, String userAgent)`.
  - `AuditLogInsertVO` — `@Getter @Setter`. 필드: `auditId, cmpnyCd, userCd, actionType, resourceType, resourceKey, ipAddress, userAgent, detailJson, insertNo`. (`insertDate` 는 DB DEFAULT 사용.)
  - `AuditLogMapper` + `.xml` — `selectNextAuditId` / `insertAuditLog`. 모든 SQL 명시 컬럼/`#{}`/leading comma/`SELECT *` 금지.
  - `AuditActionType` — `public final class` + `private` 생성자. `public static final String DOWNLOAD = "01";`. (향후 02~06 추가 위치.)
  - `AuditResourceType` — `public final class` + `private` 생성자. `public static final String USER_CREATE_TEMPLATE = "USER_CREATE_TEMPLATE";`.
  - `ClientIpExtractor` — `public final class` + `private` 생성자. `public static String extract(HttpServletRequest request)`.
    - 1) `X-Forwarded-For` 헤더 읽기. null/blank 가 아니면 콤마 split 후 첫 토큰 trim 반환.
    - 2) 없으면 `request.getRemoteAddr()` 반환.
    - 3) 둘 다 null/blank 면 null 반환.
- **핵심 파일** (모두 신규):
  - `prafta-backend/src/main/java/com/prafta/common/cmm/audit/service/AuditLogService.java`
  - `prafta-backend/src/main/java/com/prafta/common/cmm/audit/service/impl/AuditLogServiceImpl.java`
  - `prafta-backend/src/main/java/com/prafta/common/cmm/audit/command/AuditLogCommand.java`
  - `prafta-backend/src/main/java/com/prafta/common/cmm/audit/AuditContext.java`
  - `prafta-backend/src/main/java/com/prafta/common/cmm/audit/vo/AuditLogInsertVO.java`
  - `prafta-backend/src/main/java/com/prafta/common/cmm/audit/mapper/AuditLogMapper.java`
  - `prafta-backend/src/main/resources/com/prafta/common/cmm/audit/mapper/AuditLogMapper.xml`
  - `prafta-backend/src/main/java/com/prafta/common/cmm/audit/AuditActionType.java`
  - `prafta-backend/src/main/java/com/prafta/common/cmm/audit/AuditResourceType.java`
  - `prafta-backend/src/main/java/com/prafta/common/util/ClientIpExtractor.java`
- **정책서/스키마 출처**:
  - 정책서 공통 §11.3.
  - 참조 패턴: prafta-031 outbox (`NotiOutboxInsertVO` + Service mapper 직접 호출), `LeaveDashboardMapper.xml selectNextNotiId` SQL 채번 형식.
- **의존성**: PRAFTA-037-F5-1 (테이블 존재 가정).
- **우선순위 근거**: F5-3 직접 의존. 향후 follow-up(권한/상태/조직/삭제/조회 적재)의 공통 기반.

---

### PRAFTA-037-F5-3 — 양식 다운로드 적재 결선

- **유형**: backend
- **영역**: web
- **모듈**: user/user01
- **작업 유형**: 보완
- **목적**: `getUserCreateTemplate` 가 `AuditLogService.record(...)` 호출. Controller 에서 IP/UA 추출 → `AuditContext` → Service. 응답 form 무변경.
- **산출물**:
  - Controller 시그니처 변경:
    - `getUserCreateTemplate(@RequestHeader Authorization, HttpServletRequest httpRequest)`
    - 내부에서 `String ip = ClientIpExtractor.extract(httpRequest)`, `String ua = trimUA(httpRequest.getHeader("User-Agent"))` (≤500자 절단 헬퍼).
    - `AuditContext ctx = new AuditContext(ip, ua)`
    - `byte[] xlsx = user01Service.buildUserCreateTemplate(tokenInfo, ctx)`
  - Service 인터페이스 시그니처: `byte[] buildUserCreateTemplate(TokenInfo tokenInfo, AuditContext auditContext)`.
  - Service 구현:
    1. 기존 권한 가드 유지(매니저 검증).
    2. **권한 가드 통과 직후** `auditLogService.record(AuditLogCommand.builder().cmpnyCd(tokenInfo.gv_cmpnyCd()).userCd(tokenInfo.gv_userCd()).actionType(AuditActionType.DOWNLOAD).resourceType(AuditResourceType.USER_CREATE_TEMPLATE).resourceKey(null).detailJson(null).build(), auditContext)` 호출.
    3. 기존 `log.info` 유지(이중 로깅 — application log + DB 적재).
    4. 양식 빌드 후 반환.
  - 의존성 주입: `private final AuditLogService auditLogService;`
- **핵심 파일**:
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/controller/User01Controller.java`
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/service/User01Service.java`
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/service/impl/User01ServiceImpl.java`
- **정책서/스키마 출처**:
  - 정책서 공통 §11.3.
- **의존성**: PRAFTA-037-F5-2.
- **우선순위 근거**: 1차 적재 대상(D2). 향후 follow-up 의 참고 구현.

---

## 5. 작업 순서 / 의존성 그래프

```
F5-1 (마이그) → F5-2 (인프라) → F5-3 (결선)
```

직렬. 병렬화 불가.

---

## 6. 비기능 요구사항

### 6.1 보안

- 감사 적재 실패가 본업(양식 다운로드)을 막지 않음 — Service 내부 try/catch + `log.error`.
- 트랜잭션 모델: `@Transactional(propagation=REQUIRES_NEW)` 독립 트랜잭션.
- `DETAIL` JSON 평문 PII 금지 — 본 작업 1차 적재는 PII 없음(`DETAIL=NULL`).
- `cmpnyCd/userCd` 는 토큰에서만(IDOR 방지). Request body 무시.
- 모든 SQL: 명시 컬럼 / `#{}` 바인딩 / leading comma / `SELECT *` 금지.

### 6.2 운영 / 성능

- INSERT 1건이라 동기 적재로 충분.
- INDEX 4개로 시간/사용자/액션/리소스 조회 최적화.
- 보존 정책: 영구 보관(자동 삭제 잡 없음 — §7).

### 6.3 로깅 / i18n

- 로그 메시지/주석 한국어 (CLAUDE.md 컨벤션).
- `log.info("사용자 생성 양식 다운로드 - 요청자={}", userCd)` 기존 유지 + DB 적재(이중).

### 6.4 호환성

- 응답 form 무변경 — 프론트엔드 영향 0.
- Controller 시그니처에 `HttpServletRequest` 추가는 Spring MVC 가 자동 주입(클라이언트 호환 변화 없음).

---

## 7. 미해결 / Follow-up 후보 (이번 범위 밖)

| # | 항목 | 비고 |
|---|---|---|
| FA | **감사 조회 화면(운영자용)** | master/hr 전용 화면. 필터: 회사/사용자/액션유형/리소스/기간. CSV export. |
| FB | **다른 다운로드 endpoint 일괄 적재** | User_01 사용자 리스트 export, Attd_07 export 등. F5-3 패턴 그대로 한 줄 추가. |
| FC | **권한 변경 적재** | SYS060 `02` 코드 추가 + prafta-036 단건 생성/엑셀 업로드/User_01 권한 변경 적재. |
| FD | **사용자 상태 변경 적재** | SYS060 `03` 코드 추가 + 인증대기→활성화 / 미사용 / 탈퇴 전이 적재. |
| FE | **조직 구조 변경 적재** | SYS060 `04` 코드 추가 + 사업장/부서/관리자 변경 적재. |
| FF | **중요 데이터 삭제 적재** | SYS060 `05` 코드 추가. |
| FG | **상세 위치 조회 적재** | SYS060 `06` 코드 추가 + 정책 §11.3 명시 대상. |
| FH | **prafta-036 단건 생성/엑셀 업로드 적재** | FC/FD 묶음. 단건은 1행 / 엑셀은 N행 적재. |
| FI | **보존 자동 삭제 잡** | 정책서 보존기간 정의 필요 → 스케줄러 잡. |
| FJ | **모니터링 / 알람 연계** | 비정상 감사 패턴(같은 IP 다량 다운로드 등) 감지. |
| FK | **외부 SIEM 연계** | 감사 외부 export. |
| FL | **`@Auditable` AOP 화** | 6~7개 적재처가 모인 후 검토. 1차는 명시적 호출 유지. |

---

## 8. 정독 완료 사실 / 참조 파일 절대 경로

- 입력 요청서: `C:\PRAFTA\.claude\requests\web_requests\prafta-037.md`
- 상위 plan: `C:\PRAFTA\.claude\requests\web_requests\prafta-036-plan.md`
- 정책서: `C:\PRAFTA\.claude\context\policies\common\11-security-privacy.md`
- 참조 코드(NotiOutbox VO): `C:\PRAFTA\PRAFTA\prafta-backend\src\main\java\com\prafta\common\cmm\leave\vo\NotiOutboxInsertVO.java`
- 참조 mapper.xml (채번 SQL): `C:\PRAFTA\PRAFTA\prafta-backend\src\main\resources\com\prafta\common\cmm\leave\mapper\LeaveDashboardMapper.xml`
- 참조 migration(outbox + SYS코드): `C:\PRAFTA\PRAFTA\prafta-backend\src\main\resources\sql\migration\prafta-031-noti-outbox.sql`, `prafta-031-sys045-noti-type.sql`
- 1차 적재 대상 Controller: `C:\PRAFTA\PRAFTA\prafta-backend\src\main\java\com\prafta\web\user\user01\controller\User01Controller.java:203-217`
- 1차 적재 대상 Service: `C:\PRAFTA\PRAFTA\prafta-backend\src\main\java\com\prafta\web\user\user01\service\impl\User01ServiceImpl.java:681-692`
- TokenInfo: `com.prafta.common.dto.TokenInfo` (record — `gv_cmpnyCd`, `gv_userCd`, `gv_authCd`, `gv_authLevel`)
- AuthRoleUtils: `com.prafta.common.util.AuthRoleUtils.isManager(authCd)`

---

**최종 업데이트**: 2026-05-29 — planner 분해 완료. 본 문서는 후속 작업(developer/security/qa) 모든 결정의 단일 출처(SSOT)다.
