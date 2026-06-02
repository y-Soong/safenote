# prafta-037 — prafta-036 follow-up 일괄 처리 (F1, F3~F9)

> 출처: `.claude/requests/web_requests/prafta-036-plan.md` §7 (미해결 follow-up 9건 중 F2 는 prafta-036 세션 안에서 추가 완료)
> 관련 메모리: `project_prafta_036_user_create_pending_auth`
> 작성 의도: 내일 세션에서 그대로 이어받을 수 있도록 follow-up 8건을 단일 요청서로 묶는다. 각 항목은 planner 가 다시 분해해도 되고, 단순 작업은 developer 가 바로 작업해도 된다.

---

## 0. 컨텍스트 (prafta-036 요약)

관리자 단건/엑셀 사용자 생성 + SYS013 `04 인증대기` 신설 + 첫 로그인 시 휴대폰 본인인증 흐름이 완성된 상태(A~E + F2). 본 요청서는 그때 범위 밖으로 분류했던 8건의 후속 작업을 묶는다.

**prafta-036 에서 도입된 주요 자산 (본 요청서 모든 항목이 참조)**
- DB: `SYS013='04' 인증대기` 코드, `tb_user.ACCOUNT_STATUS` / `tb_daily_user.ACCOUNT_STATUS` COMMENT 정렬 완료(운영 적용됨)
- 백엔드 endpoint: `POST /webApi/user01/insert-user-info`, `GET /webApi/user01/user-create-template`, `POST /webApi/user01/upload-user-creates`, `POST /login/verify-phone-auth`
- 신규 util: `web/user/user01/util/UserExcelTemplateBuilder.java`, `UserExcelRowParser.java`
- 신규 컴포넌트: `views/login/popup/PhoneAuthPop.vue`, `views/user/popup/UserInfoPop.vue` (`callmethod_p='C'` 모드 분기)
- 임시 scope JWT: `JwtUtil.generatePhoneAuthScopeToken(cmpnyCd, userCd, ttlMinutes)` — claim `gv_scope='PHONE_AUTH'`, TTL 10분

**관리자 생성 계정의 초기 상태 (prafta-036 D2/D3 결정)**
- `ACCOUNT_STATUS='04'` 인증대기
- `USER_PW = BCrypt(휴대폰번호 11자리)`
- `PWD_CHG_DTIME = NULL`

---

## 1. 우선순위 / 의존성 / 권장 착수 순서

| 우선 | 코드 | 항목 | 운영 영향 | 의존 |
|---|---|---|---|---|
| **1** | F3 | 앱(webview) 인증대기 분기 | 앱 사용자가 관리자 생성 계정으로 로그인 시 동작 불가 — **차단요인** | prafta-036 백엔드 (완료) |
| **2** | F1 | 첫 로그인 강제 비밀번호 변경 | 초기 PW = 휴대폰번호 동일 — 보안 권장 | prafta-036 백엔드 (완료) |
| **3** | F9 | 다른 화면 SYS013 04 라벨 회귀 | 인증대기 사용자가 사용자 리스트 등에서 라벨 깨지는지 점검 | 없음 (QA) |
| **4** | F5 | 다운로드 감사 로그 | 정책서 §11.3 — 다운로드는 감사 대상 | 없음 |
| **5** | F4 | 엑셀 양식 Data Validation | 양식 사용성 — 양식이 더 명확해짐 | prafta-036 양식 빌더 (완료) |
| **6** | F8 | JwtUtil scope 토큰 일반화 | 향후 PASSWORD_RESET 등 다른 scope 재사용 위한 리팩터 | 없음 |
| **7** | F7 | 단건 생성 시 다중 사이트 권한 | 현재 단일 사이트만, 별도 권한 화면(Baim)에서 추가 가능 | 없음 |
| **8** | F6 | 엑셀 업로드 비동기/스트리밍 | 1000행 동기 처리는 충분, 확장성 개선 | 없음 |

**권장 착수**: F3 → F1 → F9 → F5 → F4 → F8 → F7 → F6. 단 F4/F5/F8 은 서로 독립이라 병렬 가능.

---

## 2. F3 — 앱(webview) 측 인증대기 분기 ★ 최우선

### 배경
prafta-036 의 백엔드(`/login/login` 응답 `nextStep='PHONE_AUTH'` + `/login/verify-phone-auth`) 흐름이 웹은 처리(F2 완료)하지만 앱 측 `prafta-app-frontend` 는 모른다. 관리자가 만든 계정으로 앱에서 로그인하면 nextStep 을 무시하고 정식 토큰처럼 처리해 오작동 가능.

### 작업 범위
1. `prafta-app-frontend/src/views/login/LoginView.vue` (또는 동등 파일) 정독 — 로그인 응답 핸들러 위치 파악
2. 응답에 `nextStep === 'PHONE_AUTH'` 분기 추가
3. 앱용 휴대폰 인증 화면(또는 팝업) 신규 — 웹 `PhoneAuthPop.vue` 의 앱 버전
   - 임시 토큰을 sessionStorage 또는 앱 통신 계층의 토큰 슬롯에 일시 보관
   - `POST /baseinfo/sms-auth-sends` (사실 앱은 `appApi` 프리픽스일 수도, kebab-case 정독 필요)
   - `POST /login/verify-phone-auth` (정확한 프리픽스 정독 필요)
4. 정식 LoginResponse 적용 후 메인 라우팅 — 기존 앱 로그인 성공 핸들러 재사용

### 정독 필요 (작업 전)
- 앱 프론트의 로그인 흐름 — 메모리 `project_prafta_app_vite_and_api_align` 참조 (loginChk → login 전환, gv_* 세션 클레임 갱신 등 비자명한 변경 있음)
- 앱은 webview 셸이므로 PhoneAuthPop 을 모달로 띄울 수 있는지 / 별도 라우트가 자연스러운지 검토
- 백엔드 endpoint 프리픽스: 웹은 `/comApi/login/*`, 앱은 `/appApi/*` 일 수도. 정확히 확인 후 호출

### 결정 필요 (planner 가 사용자에게 질의)
- 앱에서 인증대기 흐름의 UI 형태(별도 화면 vs 모달)
- 인증 취소 시 동작(로그인 화면 복귀 / 앱 종료)

---

## 3. F1 — 첫 로그인 강제 비밀번호 변경

### 배경
prafta-036 D3: 관리자 생성 계정의 초기 PW = 휴대폰번호 11자리 BCrypt 해시. 휴대폰번호는 사용자가 알고 있는 값이라 첫 로그인 직후 강제 변경이 보안상 권장. 기존 `tb_user.PWD_CHG_DTIME` 컬럼은 있으나 강제 변경 흐름이 정의돼 있지 않다.

### 작업 범위
1. 정책 결정 (planner 가 사용자에게 질의)
   - "첫 로그인"의 정의 — `PWD_CHG_DTIME IS NULL` 인 모든 사용자 vs 인증대기 통과 직후만 vs 관리자 생성 계정만 (`INSERT_NO != 'SYSTEM'` 같은 식별자)
   - 강제 변경을 거부했을 때 동작 — 메인 진입 차단 vs 알림 후 진입 허용
2. 백엔드
   - `LoginResponse` 또는 `verify-phone-auth` 응답에 `mustChangePassword: boolean` 플래그 추가
   - 기존 `POST /webApi/user01/update-my-passwd` 재사용 (이미 PWD_CHG_DTIME 갱신 로직 포함 — `User01Mapper.xml updateMyPw` 참조)
3. 프론트엔드
   - `LoginView.fnApplyLoginResponse` 에서 `mustChangePassword` 분기 → 강제 변경 팝업 띄움
   - 강제 변경 팝업(신규) — 신규 PW 입력 + 정책 검증(`User01ServiceImpl.isValidPassword` 와 동일 규칙: 6~15자, 숫자/영문/특수 중 2가지 이상)

### 정독 필요 (작업 전)
- `User01ServiceImpl.updateMyPw` (정책 검증), `User01Mapper.xml updateMyPw`
- `views/user/MyPasswordPop.vue` 또는 마이프로필 PW 변경 화면 (재사용 가능한지)

### 결정 필요
- 강제 변경 거부 시 동작
- "첫 로그인" 식별 기준

---

## 4. F9 — 다른 화면 SYS013 04 라벨 회귀 확인 (QA)

### 배경
prafta-036 에서 `SYS013='04' 인증대기` 코드를 추가했고 `tb_user.ACCOUNT_STATUS` COMMENT 도 정렬했다. 그러나 인증대기 상태의 사용자가 다른 화면(사용자 리스트 등)에 노출됐을 때 라벨이 깨지거나 부재(빈값)로 보이지는 않는지 점검 필요.

### 작업 범위 (QA / Grep 중심, 코드 변경은 발견 시만)
1. `ACCOUNT_STATUS` 를 `<BaseSelect>` / 라벨 표시에 쓰는 모든 화면 grep
   - 후보: `User_01.vue` (이미 렌더링), 사용자 리스트의 다른 grid, 일용직 화면(`tb_daily_user` 도 동일 COMMENT)
2. 각 화면이 SYS013 코드를 어떻게 로드하는지 확인
   - 정상: `systCodeArr['SYS013']` 동적 로드 → `04 인증대기` 자동 포함
   - 위험: 04 가 누락된 정적 매핑 / 하드코딩된 라벨
3. 누락 발견 시 해당 화면 정리 (별도 PR 또는 본 요청서 안에서 수정)

### 정독 필요
- 화면들의 시스템 코드 로드 패턴

### 결정 필요
- 누락 발견 시 즉시 수정 vs 별도 issue

---

## 5. F5 — 다운로드 감사 로그

### 배경
정책서 공통 §11.3 — 다운로드는 감사 대상. 현재 prafta-036 의 양식 다운로드(`GET /user-create-template`) 는 `log.info` 만 남긴다(application log). 별도 감사 테이블 적재가 필요.

### 작업 범위
1. 기존 감사 인프라 정독
   - 회사에 기존 감사 테이블 / 컴포넌트가 있는지 확인 (`tb_audit_log` 같은 후보 grep)
   - 없으면 신규 테이블 설계 — `cmpnyCd, userCd, action, resourceType, resourceKey, insertDate, ipAddress` 정도
2. 백엔드 — 양식 다운로드 endpoint 에 감사 적재 추가
3. 정책서에 "어떤 다운로드를 감사하는지" 명세가 있는지 확인 — 있으면 그 명세 우선

### 정독 필요
- 정책서 공통 §11.3 (감사 로그 정책)
- 기존 감사 인프라 grep

### 결정 필요
- 신규 테이블 vs application log 강화 vs 기존 인프라 재사용
- 양식 다운로드 외에 다른 다운로드(엑셀 export 화면 등)도 같이 정리할지

---

## 6. F4 — 엑셀 양식 Data Validation (드롭다운)

### 배경
prafta-036 의 양식은 헤더에 한글 라벨만 있고 셀 자체에 입력 제한이 없다. 사용자가 고용형태/성별/사유유형을 잘못 입력하면 업로드 시점에 행 검증으로 거부되지만, 양식 작성 단계에서부터 드롭다운으로 강제하면 사용성 개선.

### 작업 범위
1. `UserExcelTemplateBuilder.java` 에 POI `DataValidation` API 적용
   - 고용형태(F열): `"REGULAR,CONTRACT,DAILY,EXECUTIVE"` 드롭다운
   - 성별(H열): `"M,F"` 드롭다운
   - 경력인정사유유형(O열): SYS042 코드들 `"CONTRACT_TO_REGULAR,EXPERIENCE_DIFF,EXPERIENCE_SAME,GROUP_MOVE,MA_TRANSFER,OTHER"`
2. 검증 범위: 4행 ~ N행 (N=1000 정도) — `MAX_DATA_ROWS` 와 일치
3. 오류 메시지 한글로 (Data Validation 거부 시 표시되는 메시지)

### 정독 필요
- `UserExcelTemplateBuilder.java` 현재 구조
- POI `DataValidationHelper` API 사용법

### 결정 필요
- 드롭다운 vs 자유 입력 + 양식 안내문 추가만 — 사용자 선호도

---

## 7. F8 — JwtUtil scope 토큰 일반화 검토

### 배경
prafta-036 에서 `JwtUtil.generatePhoneAuthScopeToken(cmpnyCd, userCd, ttlMinutes)` 를 추가했다. 향후 다른 임시 scope 토큰(예: 비밀번호 재설정 — F1 의 자연스러운 확장, 이메일 인증, 디바이스 등록 등)을 도입할 때 같은 패턴이 필요할 수 있다.

### 작업 범위 (리팩터)
1. 현재 `generatePhoneAuthScopeToken` 시그니처를 일반화 검토
   - 후보: `generateScopeToken(cmpnyCd, userCd, scope, ttlMinutes)` 로 변경, `generatePhoneAuthScopeToken` 은 deprecated wrapper 로 유지하거나 즉시 교체
2. 검증 측에서 scope 일관성 — `VerifyPhoneAuthParam.from` 에서 `"PHONE_AUTH"` 하드코딩을 상수화
3. 새 scope 도입 시 사용 예시 코드/주석 한 줄 추가

### 정독 필요
- 현재 `JwtUtil.java`, `VerifyPhoneAuthParam.java`, `LoginController.verifyPhoneAuth`
- 향후 도입 가능한 scope 후보 (F1 의 강제 PW 변경 토큰이 가장 가까움)

### 결정 필요
- 일반화 범위 — 단순 메서드 시그니처 변경 vs 별도 클래스(`ScopeTokenIssuer` 등) 분리

---

## 8. F7 — 단건 생성 시 다중 사이트 권한 부여

### 배경
prafta-036 의 단건 생성은 `tb_user_site_auth` 에 1개 사이트만 INSERT 한다. 다중 사이트 권한이 필요한 사용자는 생성 후 별도 `Baim` 권한 화면에서 추가하도록 분리. 사용성 개선이 가능하지만 우선순위 후순위.

### 작업 범위
1. `UserCreateRequest` 에 `additionalSiteCdList: List<String>` 추가
2. `UserInfoPop.vue` 생성 모드에 "추가 권한 사이트" 다중 선택 UI 추가 (SiteSearchPop 멀티 선택 변형)
3. `User01ServiceImpl.insertUserOne` 의 18단계 직후, 추가 사이트들에 대해 `insertOneUserSiteAuth` 반복 호출
4. 엑셀 양식에는 미적용 (1행=1사용자=1사이트 단순화 유지)

### 정독 필요
- `tb_user_site_auth` 스키마, 기존 `SiteSearchPop` 다중 선택 가능 여부

### 결정 필요
- 다중 선택 UI 필요 여부 — 별도 권한 화면(Baim) 사용으로 충분한지

---

## 9. F6 — 엑셀 업로드 비동기/스트리밍

### 배경
현재 1000행 동기 처리는 충분하나, 향후 수천~수만 행 지원이 필요하면 비동기 잡 큐 도입 필요. 작업 분량 큼.

### 작업 범위 (설계 + 구현)
1. 잡 큐 인프라 선택 — Spring `@Async` + 진행률 폴링 vs 외부 큐(Redis Stream 등)
2. 신규 테이블 — `tb_user_upload_job` (jobId, status, totalRows, processedRows, failCount, fails, createdAt 등)
3. 백엔드 endpoint
   - `POST /upload-user-creates` 응답에 `jobId` 즉시 반환 (비동기)
   - `GET /upload-job/{jobId}` 진행률 조회
4. 프론트엔드 — 업로드 후 진행률 폴링 + 완료 시 BatchResultPop 표시
5. 행 검증/INSERT 로직(`insertUserBatch`)은 그대로 재사용

### 정독 필요
- 회사에 기존 비동기 잡 인프라가 있는지 grep
- 정책서 §10 알림 (작업 완료 시 push 발송 등)

### 결정 필요
- 잡 큐 인프라 — 신규 도입 vs 기존 인프라
- 비동기 시점의 트랜잭션 모델

---

## 10. 비기능 / 공통 요구사항

- 모든 변경은 prafta-036 의 보안 가드 패턴 유지 (`AuthRoleUtils.isManager` + `cmpnyCd` 토큰 강제 + AES-GCM/HMAC PII)
- 신규 SQL 마이그레이션이 있으면 `prafta-backend/src/main/resources/sql/migration/prafta-037-*.sql` 명명, 운영 적용은 사용자 수동
- 신규 코드는 한국어 주석, 영어 식별자 (CLAUDE.md 컨벤션)
- 로그 메시지 한국어
- Bash 호출 시 타임아웃 명시, 비대화형 옵션 강제

---

## 11. 차후 분해 시 메모

- planner 에게 본 요청서를 던질 때 "F3 만 우선 분해" 또는 "F3/F1/F9 묶음 분해" 같이 범위 지정 가능
- developer 에게 단순 항목(F4/F8/F9)은 직접 작업 가능 — planner 통과 없이도 됨
- F6 는 설계 결정이 많아 planner 분해 권장
- 본 요청서의 follow-up 들이 서로 영향을 주지 않게 작업 — F1 이 F8 의 시그니처 변경에 영향을 받을 수 있으니 F8 을 먼저 끝내거나 F1 안에서 같이 처리 검토
