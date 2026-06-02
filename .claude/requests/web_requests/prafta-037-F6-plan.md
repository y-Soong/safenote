# prafta-037-F6 — 엑셀 업로드 비동기/스트리밍 : 작업 분해 계획

> 작성: 메인 세션 (2026-05-29).
> 원본 요청서: `.claude/requests/web_requests/prafta-037.md` §9 (F6).
> 상위 컨텍스트: `.claude/requests/web_requests/prafta-036-plan.md` (동기 업로드 → 본 작업이 비동기 변형).
> 정책서 출처: 공통 §10(알림), §11.1(파일 업로드 최소 수집), §11.3(감사).
> 본 plan 은 F6 의 단일 출처(SSOT)다.

---

## 0. 개요

- 현재 `POST /webApi/user01/upload-user-creates` 는 동기 처리(1000행 한도, 5MB). 1000행도 행 검증/INSERT/HMAC/AES-GCM 으로 충분히 무거울 수 있어 클라이언트 타임아웃 / 사용자 대기 UX 문제 가능.
- 본 F6 작업은 **Spring `@Async` + DB 폴링** 방식으로 비동기 엔드포인트 1쌍을 신설한다. 기존 동기 엔드포인트는 무변경(호환성).
- 잡 인프라는 신규 테이블 `tb_user_upload_job` 1개로 단순화. Redis Stream 등 외부 큐 도입 없음.
- 행별 INSERT 로직은 기존 `User01BatchService.insertUserBatch` 그대로 재사용(트랜잭션 모델 동일: REQUIRES_NEW).
- 프론트는 `User_01.vue` 엑셀 업로드 버튼에 진행률 폴링 UI 추가. 완료 시 기존 `BatchResultPop` 으로 결과 표시.

---

## 1. 사용자 확정 결정 + 메인 세션 결정

### 1.1 사용자 확정 (2026-05-29 채팅, "(a)안 진행")

| # | 결정 | 내용 |
|---|---|---|
| **D1** | 잡 큐 인프라 | **Spring `@Async` + DB 폴링** (신규 의존성 0). Redis Stream 등 외부 큐 불채택. |
| **D2** | 신규 테이블 | `tb_user_upload_job` — jobId/status/totalRows/processedRows/successCount/failCount/failsJson/fileName/fileSize/userCd 필드. 채번 outbox 패턴(`U`+YYYYMMDD+SEQ). |
| **D3** | endpoint 모델 | 기존 동기 `POST /webApi/user01/upload-user-creates` **유지**. 신규 `POST /webApi/user01/upload-user-creates-async` + `GET /webApi/user01/upload-job/{jobId}` 추가. |
| **D4** | 진행률 폴링 | 프론트 약 1.5초 간격. 완료 상태(SUCCESS/FAILED/PARTIAL) 도달 시 폴링 중단. |

### 1.2 메인 세션 자율 결정 (본 문서 권위)

| # | 결정 | 내용 | 근거 |
|---|---|---|---|
| **D5** | 상태머신 | `PENDING → RUNNING → SUCCESS | FAILED | PARTIAL` | PENDING=잡 생성 직후 / RUNNING=행 처리 중 / SUCCESS=전체 성공 / PARTIAL=일부 실패 / FAILED=초기 검증 실패 또는 치명 예외 |
| **D6** | SYS코드 | **SYS061 신설** ("사용자 업로드 잡 상태"). SYS046~SYS055 prafta-033 TBM 점유, SYS060 prafta-037-F5 점유 → SYS061 사용. | 잘못된 코드그룹 충돌 재발 방지. |
| **D7** | 권한 모델 — 잡 조회 | `isManager` + **잡 생성자 본인만** (`job.userCd == gv_userCd`). master/hr 라도 타인 잡 조회 차단. | IDOR 가드. master 가 타인 작업 모니터링은 §7 follow-up. |
| **D8** | 트랜잭션 | 행 INSERT 는 기존 `insertUserBatch` (`REQUIRES_NEW`) 그대로. 잡 상태 UPDATE 는 매 행마다 별도 트랜잭션(`REQUIRES_NEW`)으로 즉시 가시화. | 1000행 한도 — 매 행 UPDATE 부담 무시 가능. 진행률 실시간성 확보. |
| **D9** | 파일 처리 | Controller 가 POI 파싱(동기)으로 `List<UserCreateParam>` 만든 후 비동기 호출. 원본 파일 바이트는 보관하지 않음 — 메모리 압력 최소. | 1000행/5MB 한도 → 파싱 자체는 빠름. 파싱 실패는 동기 응답(파싱 후 비동기). |
| **D10** | `@EnableAsync` | `PraftaBackendApplication` 메인 클래스에 `@EnableAsync` 추가. 별도 `TaskExecutor` Bean 신설 없이 Spring 기본 SimpleAsyncTaskExecutor 사용. | 신규 의존성/Bean 최소화. 동시 잡 수가 적어 별도 풀 불필요. 워커 풀 필요 시 §7 follow-up. |
| **D11** | 잡 보존 정책 | 영구 보관(자동 삭제 잡 없음). | 감사 로그(prafta-037-F5)와 동일 정책. |
| **D12** | 진행률 UPDATE 빈도 | **매 행 처리 직후 1회 UPDATE**. 별도 batch 없음. | 단순성 + 폴링 실시간성. 1000행 한도. |
| **D13** | 실패 사유 JSON | `FAILS_JSON` 컬럼에 `[{index, userId, errorCode, message}]` 배열을 압축 없이 저장(JSON 타입). | 폴링 응답에서 그대로 BatchResultPop 으로 전달. |
| **D14** | TokenInfo 전달 | 비동기 서비스 메서드는 `TokenInfo` 직접 인자로 받음(security context 전파 우회). | `@Async` 호출 시 SecurityContext / RequestContext 비전파 회피. 매 행마다 토큰 갱신 안 함. |
| **D15** | 동시 잡 한도 | 본 작업 범위 밖. 사용자별 동시 잡 1개로 제한 등은 §7 follow-up. | 1000행 한도 + 사용자 개입 패턴이라 동시성 위험 낮음. |
| **D16** | 감사 로그 적재 | F5 `AuditLogService` 호출하여 비동기 업로드 시작 시 1행 적재. ACTION_TYPE 은 기존 SYS060='01' 다운로드 그대로(단일 코드만 신설). 향후 UPLOAD 코드 추가는 §7. | F5 인프라 활용. 본 작업의 1차 적재 대상은 양식 다운로드라 §7 follow-up 으로 분리. |

---

## 2. 초안에서 잘라낸 / 보류한 항목

| 항목 | 처리 | 이유 |
|---|---|---|
| Redis Stream / 외부 큐 도입 | 불채택 | D1. 신규 의존성/운영 부담. |
| 잡 보존 자동 삭제 | §7 follow-up | D11. |
| master 가 타인 잡 조회 | §7 follow-up | D7. IDOR 가드 우선. |
| 워커 풀(TaskExecutor) 분리 | §7 follow-up | D10. 동시 잡 수 적음. |
| 폴링 대신 WebSocket / SSE | 불채택 | 폴링 단순성 우선(1.5초 간격). 본 부하 가벼움. |
| 행 처리 일괄 batch UPDATE | 불채택 | D12. 1000행 한도라 매 행 UPDATE 부담 무시 가능. |
| 사용자별 동시 잡 1개 제한 | §7 follow-up | D15. 정책 결정 필요. |
| 업로드 액션 감사 로그 적재 (UPLOAD ACTION_TYPE 신설) | §7 follow-up | D16. F5 1차 범위는 양식 다운로드. |
| 파일 자체 보존 (S3 등) | 불채택 | D9. 파일은 파싱 후 폐기. |

---

## 3. 영향 범위

### 3.1 스키마 / 마이그레이션

| 작업 | 파일 | 변경 |
|---|---|---|
| 신규 테이블 `tb_user_upload_job` | `prafta-backend/src/main/resources/sql/migration/prafta-037-F6-user-upload-job.sql` (신규) | CREATE TABLE — 아래 §3.1.1. |
| 신규 채번 키 `USER_UPLOAD_JOB_ID` | 같은 파일 안 | `FNC_CMM_SEQ_NEXTVAL` 자동 채번 — 별도 INSERT 불필요. |
| SYS061 마스터 + 디테일 시드 | 같은 파일 안 | "사용자 업로드 잡 상태" + 5개 디테일 (PENDING/RUNNING/SUCCESS/FAILED/PARTIAL). |

#### 3.1.1 `tb_user_upload_job` 스키마 확정안

```sql
CREATE TABLE `tb_user_upload_job` (
    `JOB_ID`          varchar(25)   NOT NULL COMMENT '잡 ID (PK, 회사별 채번: U + YYYYMMDD + SEQ)',
    `CMPNY_CD`        varchar(50)   NOT NULL COMMENT '회사 코드',
    `USER_CD`         varchar(20)   NOT NULL COMMENT '잡 생성한 사용자 (작업 조회 권한 검증용)',
    `FILE_NAME`       varchar(255)  NULL     COMMENT '원본 파일명 (감사용)',
    `FILE_SIZE`       bigint        NULL     COMMENT '파일 바이트 크기',
    `TOTAL_ROWS`      int           NOT NULL DEFAULT 0 COMMENT '파싱된 데이터 행 수',
    `PROCESSED_ROWS`  int           NOT NULL DEFAULT 0 COMMENT '처리 완료 행 수 (성공+실패)',
    `SUCCESS_COUNT`   int           NOT NULL DEFAULT 0 COMMENT '성공 행 수',
    `FAIL_COUNT`      int           NOT NULL DEFAULT 0 COMMENT '실패 행 수',
    `FAILS_JSON`      json          NULL     COMMENT '실패 항목 JSON 배열 [{index,userId,errorCode,message}]',
    `STATUS`          varchar(20)   NOT NULL DEFAULT 'PENDING' COMMENT '잡 상태[SYS061] PENDING:대기 RUNNING:진행 SUCCESS:성공 FAILED:실패 PARTIAL:일부실패',
    `ERROR_MSG`       varchar(1000) NULL     COMMENT '치명 예외 사유 (FAILED 상태일 때)',
    `INSERT_NO`       varchar(50)   NOT NULL COMMENT '등록자',
    `INSERT_DATE`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    `UPDATE_NO`       varchar(50)   NULL     COMMENT '수정자',
    `UPDATE_DATE`     datetime      NULL     COMMENT '수정 일시',
    PRIMARY KEY (`JOB_ID`),
    KEY `IX_USER_UPLOAD_JOB_USER` (`CMPNY_CD`, `USER_CD`, `INSERT_DATE`),
    KEY `IX_USER_UPLOAD_JOB_STATUS` (`CMPNY_CD`, `STATUS`, `INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 일괄 생성 잡 (PRAFTA-037-F6 비동기)';
```

#### 3.1.2 SYS061 시드

```sql
INSERT INTO `tb_syst_val_m` (`SYST_VAL_CD`, `SYST_VAL_NM`, `USE_YN`, `VAL_DESC`, `INSERT_NO`)
VALUES ('SYS061', '사용자 업로드 잡 상태', 'Y', 'tb_user_upload_job.STATUS 코드', 'SYSTEM');

INSERT INTO `tb_syst_val_d`
  (`SYST_VAL_CD`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM`, `SORT_IDX`, `USE_YN`, `INSERT_NO`)
VALUES
  ('SYS061', 'PENDING',  '대기',     1, 'Y', 'SYSTEM'),
  ('SYS061', 'RUNNING',  '진행',     2, 'Y', 'SYSTEM'),
  ('SYS061', 'SUCCESS',  '성공',     3, 'Y', 'SYSTEM'),
  ('SYS061', 'PARTIAL',  '일부실패', 4, 'Y', 'SYSTEM'),
  ('SYS061', 'FAILED',   '실패',     5, 'Y', 'SYSTEM');
```

### 3.2 백엔드

#### 신규 파일

| 영역 | 파일 | 변경 |
|---|---|---|
| Status 상수 | `web/user/user01/upload/UploadJobStatus.java` (신규) | `public final class` + 상수 5개 (`PENDING/RUNNING/SUCCESS/PARTIAL/FAILED`). |
| Mapper | `web/user/user01/upload/UploadJobMapper.java` (신규) + `.xml` | `selectNextJobId`, `insertUploadJob`, `selectUploadJob(jobId,cmpnyCd)`, `updateUploadJobProgress`, `updateUploadJobFinal`. |
| VO | `web/user/user01/upload/UploadJobInsertVO.java` (신규) | INSERT 운반체. Lombok @Getter @Setter. |
| Result | `web/user/user01/upload/UploadJobResult.java` (신규) | 조회 결과 record. |
| Service | `web/user/user01/upload/UploadJobService.java` + `Impl` (신규) | 잡 생성/상태조회/비동기 실행 워크로드. |
| Response DTO | `web/user/user01/upload/UserUploadJobStartResponse.java` (신규) | record (jobId, totalRows). |
| Response DTO | `web/user/user01/upload/UserUploadJobStatusResponse.java` (신규) | record (jobId, status, totalRows, processedRows, successCount, failCount, fails, errorMsg). |
| Param | `web/user/user01/upload/UploadJobQueryParam.java` (신규) | jobId + 토큰 (IDOR 가드). |

#### 변경 파일

| 영역 | 파일 | 변경 |
|---|---|---|
| Spring Boot 메인 | `PraftaBackendApplication.java` | `@EnableAsync` 추가. |
| Controller | `web/user/user01/controller/User01Controller.java` | 신규 endpoint 2개: `POST /upload-user-creates-async`, `GET /upload-job/{jobId}`. |
| Error Code | `common/error/user/UserErrorCode.java` | `USER_404_002` (잡 없음 또는 권한 없음 — 단일 메시지로 통합, 회사/사용자 노출 차단). |

#### 변경하지 않는 파일

- 기존 `POST /upload-user-creates` 동기 endpoint, `User01BatchService`, `UserExcelRowParser`, `UserExcelTemplateBuilder` — **무변경**.

### 3.3 프론트엔드

#### 변경 파일 — `User_01.vue` (비동기 업로드 + 폴링)

| 영역 | 변경 |
|---|---|
| `fnExcelFileChange` | 호출 엔드포인트를 `/upload-user-creates-async` 로 변경. 응답 `{jobId, totalRows}` 수령 후 폴링 시작. |
| `fnPollUploadJob(jobId)` | 신규. `GET /upload-job/{jobId}` 1.5초 간격 호출. status ∈ {SUCCESS,PARTIAL,FAILED} 이면 폴링 중단. |
| 진행률 UI | 폴링 중 화면 잠금 + 진행률 바(`processedRows/totalRows`) 표시. 완료 후 `BatchResultPop` 으로 결과(fails) 전달. |
| 에러 처리 | FAILED 응답 시 `errorMsg` 알럿 + 폴링 중단. |
| 폴링 정리 | 컴포넌트 언마운트 / 라우트 이동 시 setInterval clear. |

#### 변경하지 않는 파일

- 기존 동기 업로드 흐름은 `BatchResultPop` 동일 사용 패턴이라 결과 표시 로직 변경 없음.

---

## 4. 작업 단위 분해 (권장 착수 순서)

### PRAFTA-037-F6-1 — 스키마 + SYS061

- **목적**: `tb_user_upload_job` 신설 + SYS061 시드.
- **산출물**: `prafta-037-F6-user-upload-job.sql`.
- **운영 적용**: 사용자 수동.

### PRAFTA-037-F6-2 — 백엔드 잡 인프라 + 엔드포인트

- **목적**: 신규 9개 Java/XML 파일 + 1개 메인 클래스 변경 + 1개 컨트롤러 변경.
- **순서**:
  1. `UploadJobStatus`, `UploadJobInsertVO`, `UploadJobResult`, `UploadJobMapper`+xml
  2. `UploadJobService`+`Impl` (잡 생성, 상태 조회, `@Async` 실행 메서드)
  3. `UserUploadJobStartResponse`, `UserUploadJobStatusResponse`, `UploadJobQueryParam`
  4. `User01Controller` 신규 endpoint 2개
  5. `PraftaBackendApplication` 에 `@EnableAsync`
  6. `UserErrorCode.USER_404_002` 신규
- **의존성**: F6-1 (테이블 존재 가정).
- **빌드 검증**: `./gradlew.bat compileJava` BUILD SUCCESSFUL.

### PRAFTA-037-F6-3 — 프론트 비동기 업로드 + 폴링

- **목적**: `User_01.vue` 의 `fnExcelFileChange` 를 비동기 엔드포인트로 전환 + 폴링 UI 추가.
- **산출물**: `User_01.vue` 수정.
- **의존성**: F6-2.
- **빌드 검증**: `vite build`.

---

## 5. 의존성 그래프

```
F6-1 (스키마) → F6-2 (백엔드) → F6-3 (프론트)
```

직렬. 운영 적용 후 백엔드 → 프론트.

---

## 6. 비기능 요구사항

### 6.1 보안

- **권한**: `AuthRoleUtils.isManager` 가드 (양 endpoint 동일).
- **IDOR**: 잡 조회는 `job.userCd == tokenInfo.gv_userCd` 검증. 다른 사용자 jobId 추측 시 404 (잡 없음 메시지로 회사/존재 노출 차단).
- **회사 스코프**: 모든 mapper SQL 에 `CMPNY_CD = tokenInfo.gv_cmpnyCd` 강제.
- **파일 검증**: 기존 동기 endpoint 와 동일(`.xlsx`, ≤5MB, ≤1000행). 파싱 실패 시 동기 응답으로 4xx.
- **PII 로그**: `userId`, `userNm` 등 평문 PII 는 application log 에 출력 금지. errorCode/index/message(표준) 만.

### 6.2 트랜잭션 / 동시성

- 잡 INSERT: 동기 endpoint 트랜잭션.
- 비동기 실행: 매 행 `REQUIRES_NEW` (기존 `insertUserOne` 패턴 그대로).
- 진행률 UPDATE: 매 행 직후 별도 `REQUIRES_NEW` UPDATE (가시성 확보).
- 동시 잡 한도: 본 작업 범위 밖. SimpleAsyncTaskExecutor 기본 동작 — Spring 이 새 스레드 생성. 동시 잡 수가 많아지면 풀 도입 follow-up.

### 6.3 한국어 메시지

- 에러: `USER_404_002` = "업로드 작업을 찾을 수 없습니다."
- 잡 실패 errorMsg: 한국어. 예: "엑셀 파일을 읽을 수 없습니다.", "처리 중 오류가 발생했습니다."
- 프론트 진행률 라벨: "처리 중 N/M (X%)".

### 6.4 로깅

- `log.info("엑셀 비동기 업로드 시작 - jobId={}, 요청자={}, 파일={}, totalRows={}", ...)`
- `log.info("엑셀 비동기 업로드 완료 - jobId={}, status={}, 성공={}, 실패={}", ...)`
- `log.error("엑셀 비동기 업로드 치명 예외 - jobId={}", e)`

### 6.5 폴링 UX

- 1.5초 간격. 완료 상태 도달 시 즉시 중단.
- 폴링 중 사용자 행동 차단(엑셀 업로드 버튼 비활성화).
- 화면 이탈/언마운트 시 setInterval 정리.

---

## 7. 미해결 / Follow-up 후보

| # | 항목 | 비고 |
|---|---|---|
| F6-FU1 | master 가 타인 잡 조회 | D7. 운영 모니터링 화면 신설 시. |
| F6-FU2 | 워커 풀 분리 (`TaskExecutor`) | D10. 동시 잡 수 증가 시. |
| F6-FU3 | 사용자별 동시 잡 1개 제한 | D15. 정책 결정 필요. |
| F6-FU4 | 업로드 ACTION_TYPE 감사 적재 | D16. F5 인프라 활용. SYS060 디테일 신규 추가. |
| F6-FU5 | 잡 보존 자동 삭제 | D11. 보존 정책 정의 후. |
| F6-FU6 | 폴링 → WebSocket / SSE | 부하 폭증 시. |
| F6-FU7 | 파일 자체 보존 (S3) | 감사 강화 시. |
| F6-FU8 | 잡 목록 조회 화면 | 사용자별 본인 잡 이력 조회. |

---

**최종 작성**: 2026-05-29 — 메인 세션 분해 완료. 본 문서는 후속 구현의 단일 출처(SSOT).
