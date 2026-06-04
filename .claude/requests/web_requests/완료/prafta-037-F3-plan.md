# prafta-037-F3 — 앱(webview) 인증대기 분기 : 작업 분해 계획

> 작성: planner 세션 (2026-05-29).
> 원본 요청서: `.claude/requests/web_requests/prafta-037.md` §2 (F3).
> 상위 컨텍스트: `.claude/requests/web_requests/prafta-036-plan.md` (백엔드 자산 — 본 plan 의 모든 호출이 그 위에서 동작).
> 정책서 출처: 공통 §3.2(로그인), §3.4(세션 관리/토큰), §3.5(계정 상태), §11.1(PII 최소수집), §13.3(공통 UI/UX).
> 본 plan 은 F3 의 단일 출처(SSOT)다. 후속 developer/security/qa 는 본 문서의 결정과 분해를 따른다.

---

## 0. 개요 (한 페이지)

prafta-036 백엔드는 `ACCOUNT_STATUS='04'` 인증대기 계정의 로그인 응답에 `nextStep='PHONE_AUTH'` + 임시 scope=PHONE_AUTH JWT(10분) 를 내려준다. 웹 프론트는 F2 에서 `PhoneAuthPop.vue` 모달로 분기 처리 완료. 앱(`prafta-app-frontend`) 은 아직 미대응 — 응답을 무시하고 임시 토큰을 정식 토큰처럼 sessionStorage 에 저장한 뒤 메인으로 라우팅하므로 모든 후속 API 가 scope 미스매치로 실패하거나 PII 미인증 상태로 정식 기능 접근.

본 F3 작업은 **앱 프론트엔드만** 변경한다. 백엔드 무변경, 신규 스키마 무변경. 앱 통신 계층(`api/axios.js`, `composables/useAuth.js`, `stores/userStore.js`)도 이미 prafta-app-vite-and-api-align 작업에서 정렬되어 있어 본 F3 는 그대로 활용한다.

산출:
- 신규 라우트 `/PhoneAuth` (해시 라우터 컨벤션에 맞춰 `#/PhoneAuth`)
- 신규 화면 `src/views/login/PhoneAuthView.vue` (별도 라우트 화면 — 사용자 확정 결정 F3-1)
- 기존 `src/views/login/LoginView.vue` 의 `fnSubmitLogin` 응답 핸들러에 `nextStep === 'PHONE_AUTH'` 분기 추가
- `router/index.js` 의 routes / publicPaths 추가

---

## 1. 사용자 확정 결정

### 1.1 채팅으로 확정된 결정 (2026-05-29)

| # | 결정 | 내용 |
|---|---|---|
| F3-1 | **UI 형태** | **별도 라우트 화면**(모달 아님). 라우트 경로 `#/PhoneAuth`. 모바일 webview 환경에서 화면 전환 자연스러움 + 뒤로가기 동작 일관성. |
| F3-2 | **인증 취소 시 동작** | **로그인 화면 복귀**(`router.replace('/')`). 임시 토큰은 `sessionStorage.removeItem('token')` 로 즉시 폐기. **앱 종료 아님**. |

### 1.2 planner 결정 포인트 (본 문서 안에서 추가 확정)

채팅 확정 결정만으로 분해가 막히지 않도록, 의문 영역은 다음과 같이 결정해 둔다. developer 가 정독 후 합리적 사유로 변경하면 본 문서의 §7 Follow-up 에 사유와 함께 기록.

| # | 결정 포인트 | planner 결정 | 근거 |
|---|---|---|---|
| P1 | 라우트 경로 명칭 | `/PhoneAuth` | 앱 기존 PascalCase 라우트(`/ActInfoSrch`, `/TermsInfo`, `/JoinUser`) 패턴 일치. 사용자 권장은 `/login/phone-auth` 였지만 앱 라우터(`router/index.js`)에 슬래시 중첩/케밥 케이스 라우트가 없어 일관성을 위해 PascalCase 단일 세그먼트 채택. |
| P2 | 임시 토큰 보관 슬롯 | **`sessionStorage.token` 일시 점유** (웹 F2 와 동일 패턴) | 앱 axios 요청 인터셉터가 `sessionStorage.getItem('token')` 만 Authorization 헤더에 부착(`api/axios.js:120-123`). 별도 슬롯을 만들면 SMS 발송/인증 API 호출 시 헤더 부착이 따로 필요해진다. 인증 성공 시 정식 token 으로 교체, 취소·실패·라우트 이탈 시 `removeItem('token')` 으로 즉시 폐기. |
| P3 | sessionStorage 의 다른 `gv_*` 키 | **세팅하지 않음** | 임시 토큰 단계에서 `gv_userCd/gv_cmpnyCd/gv_authLevel` 등이 sessionStorage 에 있으면 axios 인터셉터가 모든 요청에 부착(`axios.js:83-95`). 인증대기 단계에서는 SMS 발송/인증 외 호출이 일어나면 안 되므로 의도적으로 비워둔다. PhoneAuthView 는 cmpnyCd 를 라우트 state 로 받아 SMS 발송 body 에만 사용. |
| P4 | LoginView → PhoneAuthView 전달 데이터 | **vue-router state** 활용 (`router.push({ path: '/PhoneAuth', state: { phoneAuthToken, cmpnyCd } })`) + **PhoneAuthView 의 `history.state` 에서 즉시 추출 후 sessionStorage.token 에 저장**. URL 쿼리스트링 금지(JWT 노출). | 앱 라우터는 createWebHashHistory 사용 — state 는 History API 의 state 로 보존. 새로고침 시 state 소실되어 인증대기 상태가 끊기지만 그것은 의도(임시 토큰 보호). |
| P5 | 인증 성공 시 후속 처리 | LoginView 의 `fnApplyLoginResponse` 패턴을 PhoneAuthView 내부에 동일 구조로 구현 — sessionStorage gv_* 12개 키 세팅 + refreshToken localStorage + `axios.defaults.headers.common.Authorization` 갱신 + `router.replace('/MainView')`. **userStore.setUser 도 호출(웹 F2 동일)** — 앱 store 가 향후 화면에서 사용될 수 있음. | 웹 F2 의 `fnApplyLoginResponse`(LoginView.vue:263-314) 동일 패턴 유지. |
| P6 | 임시 토큰 10분 만료 시 동작 | 화면 진입 시점에 JWT exp claim 을 디코딩해 화면 내 타이머 카운트다운(`mm:ss`) 표시. 만료 시 사용자에게 한글 안내 alert + 로그인 화면 복귀(F3-2 와 동일 흐름). | 사용자가 만료 안내 받고 재로그인하면 동일 흐름 재시작. SMS 인증 통과 직전에 만료되면 verify-phone-auth 가 401 반환 → axios 401 인터셉터가 refresh 시도 → refreshToken 도 없음(인증대기 단계는 refresh 미발급) → forceLogout 으로 동일 결과. 화면 내 타이머는 UX 보조. |
| P7 | 휴대폰 번호 입력 UX | **사용자가 직접 입력** — 사전입력 없음. 휴대폰 번호 자체가 인증 대상이고 (a) prafta-036 D2 결정상 관리자가 입력한 번호와 사용자가 인증하는 번호가 다를 수 있고 (b) 앱은 PII 를 sessionStorage 에 보관하지 않는다(§11.1) | 웹 F2 패턴 동일. |
| P8 | 한국어 메시지 표준 | 본 plan §6.3 표 참조. 모든 alert/안내문은 한글. 백엔드 메시지가 있으면 우선 표시(`resolveApiErrorMessage` 활용). | apiError.js 컨벤션. |
| P9 | 신규 화면 스타일 | 기존 앱 로그인 흐름 화면들(`LoginView.vue`, `ActInfoSrch.vue`)이 사용하는 **인라인 스타일/tailwind 유사 유틸리티 + scoped CSS 혼합 패턴** 따른다. 색상: `#5cb85c`(primary), `#1f1f1f`(text), `#e5e7eb`(border), `#9ca3af`(muted), `#ef4444`(error) — 기존 LoginView.vue 와 동일 팔레트. 앱 프론트엔드는 CSS 변수 시스템이 정착되지 않음(LoginView.vue:236~449 이 모두 하드코딩) — 본 작업에서 디자인 토큰 도입은 범위 밖. | 앱 프론트의 디자인 토큰 미정. |
| P10 | back 버튼/뒤로가기 동작 | 헤더 좌측 ← 버튼 누름 = 취소 흐름과 동일(F3-2). webview 의 H/W back 버튼은 vue-router `onBeforeRouteLeave` 가드로 동일 동작. `proxy.$confirm` 으로 사용자 확인 후 진행. | 임시 토큰이 폐기되지 않은 채 LoginView 로 돌아가면 다음 로그인 시 잔존 토큰이 인터셉터에 의해 부착되는 사고 방지. |

---

## 2. 초안에서 잘라낸 / 보류한 항목

| 초안/유사 옵션 | 처리 | 이유 |
|---|---|---|
| 모달(PhoneAuthPop 앱 포팅) | **불채택** | 사용자 확정 F3-1: 별도 라우트 화면. webview 환경에서 풀스크린 라우트가 입력/키보드 처리에 자연스러움. |
| 앱 종료(인증 취소 시) | **불채택** | 사용자 확정 F3-2: 로그인 화면 복귀. Flutter 셸의 webview 라우팅과 일관. |
| 자동 재시도(인증번호 발송 실패 시) | **범위 밖** | F3 는 기본 흐름만. 발송 실패 시 사용자에게 alert + 같은 화면 유지. 재시도 버튼 누르면 동일 호출. |
| 인증 실패 횟수 제한 / lock-out | **범위 밖** | 백엔드 정책. 본 작업은 프론트만 변경. follow-up 가능. |
| 비밀번호 강제 변경 동시 처리(F1 의 결합) | **불채택** | F1 은 별도 follow-up. 본 작업은 F3 단독 분리. §7 F3-X3 으로 기록. |
| 신규 임시 토큰 보관 슬롯(예: `sessionStorage.phoneAuthToken`) | **불채택** | P2 결정: 기존 token 슬롯 일시 점유. 앱 axios 인터셉터가 자동으로 부착. |
| 디자인 토큰(CSS 변수) 도입 | **범위 밖** | P9 결정. 별도 follow-up. |
| QR 스캔/카메라 권한 흐름 | **무관** | 인증대기는 SMS 만 사용. Flutter 권한 트리거 없음. |

---

## 3. 영향 범위

### 3.1 백엔드

**무변경**. 다음 기존 자산만 호출:
- `POST /comApi/baseinfo/sms-auth-sends` — body `{ cmpnyCd, mblNo }` (앱 기존 패턴 `JoinUser.vue:669`, `ActInfoSrch.vue:277` 동일). 응답 200 이면 발송 성공.
- `POST /comApi/login/verify-phone-auth` — body `{ mblNo, certNo }`, Authorization 헤더 = 임시 scope=PHONE_AUTH 토큰. 응답: 정식 LoginResponse(token, refreshToken, gv_* 11개 키). 백엔드 `LoginController.verifyPhoneAuth:99`.

> 주의: 백엔드 `verifyPhoneAuth` 내부에서 `BaseinfoService.userSmsAuthCheck` 를 호출해 인증번호를 검증하므로 프론트는 별도 `/sms-auth-checks` 를 부르지 않는다. 웹 F2 `PhoneAuthPop.vue:194` 동일 패턴.

### 3.2 스키마 / 마이그레이션

**무변경**.

### 3.3 프론트엔드 (변경 대상)

| 파일 | 변경 내용 |
|---|---|
| `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/router/index.js` | `/PhoneAuth` 라우트 1건 추가, publicPaths 에 `/PhoneAuth` 추가(임시 토큰 단계는 정식 토큰 부재 → public 라우트로 취급). |
| `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/login/LoginView.vue` | `fnSubmitLogin` 의 응답 핸들러에 `if (response.data?.nextStep === 'PHONE_AUTH')` 분기 추가. 분기에서 `router.push({ path: '/PhoneAuth', state: { phoneAuthToken: response.data.token, cmpnyCd: response.data.cmpnyCd } })`. 기존 일반 로그인 흐름은 무변경. |
| `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/login/PhoneAuthView.vue` | **신규**. 본 plan §8 골격 참조. |

### 3.4 프론트엔드 (영향만 받고 변경 없는 파일)

| 파일 | 영향 |
|---|---|
| `src/api/axios.js` | 변경 없음. 임시 token 이 sessionStorage 에 있으면 Authorization 헤더 자동 부착(인터셉터:120-123). PhoneAuthView 의 SMS 발송/인증 호출도 동일 흐름. 401 인터셉터의 refresh 시도는 인증대기 단계에서는 refreshToken 부재로 강제 로그아웃으로 떨어짐 — 의도된 동작(P6). |
| `src/composables/useAuth.js` | 변경 없음. forceLogout 이 sessionStorage.clear + refreshToken 제거 — 본 작업이 의도한 클린업과 일치. |
| `src/stores/userStore.js` | 변경 없음. PhoneAuthView 가 인증 성공 시 setUser 호출(P5). |

---

## 4. 작업 단위 분해 (권장 착수 순서)

> 권장 순서: **F3-α → F3-β → F3-γ**. α 가 라우트 + 빈 화면 + LoginView 분기, β 가 PhoneAuthView 본 로직(SMS 발송/인증/완료), γ 가 경계 처리(취소/만료/뒤로가기).
>
> α 만 단독 배포해도 회귀 위험은 없지만 인증대기 사용자는 진입은 하되 화면이 빈 상태가 된다. β/γ 까지 같은 PR 권장.

---

### PRAFTA-037-F3-α — 라우트 등록 + PhoneAuthView 스켈레톤 + LoginView 분기

- **유형**: frontend-screen
- **영역**: app
- **모듈**: app-frontend / login
- **작업 유형**: 신규
- **목적**: F3-1 결정 반영. 라우트 등록·스켈레톤 화면 생성·LoginView 응답 분기 한 번에. 본 단계 완료 시점에 인증대기 응답을 받은 사용자는 신규 화면으로 라우팅되며 임시 토큰이 sessionStorage 에 일시 보관.
- **산출물**:
  - `src/views/login/PhoneAuthView.vue` (신규 — 본 plan §8 골격)
  - `src/router/index.js` 수정 — `/PhoneAuth` 라우트 추가 + publicPaths 등록
  - `src/views/login/LoginView.vue` 수정 — `fnSubmitLogin` 응답 핸들러 분기
- **핵심 파일**: 위 3개
- **정책서 출처**:
  - 공통 §3.2(로그인 응답 분기 — `최초 로그인 시 권한에 따라 진입 화면이 분기된다`).
  - 공통 §3.5(계정 상태 `04 인증대기`는 prafta-036 D2 신설 — `03-account-auth.md` §3.5 표는 현재 활성/미사용/탈퇴 3개만이지만 prafta-036 D2 로 확장 결정됨, 정책서 본문 미반영 → §7 F3-X1).
  - 백엔드 자산: `prafta-036-plan.md` §3.2 / `LoginController.verifyPhoneAuth`(:99).
- **의존성**: prafta-036 백엔드(이미 완료).
- **우선순위 근거**: F3 의 시작 — α 없이 β/γ 가 의미 없음. 위험이 낮음.

---

### PRAFTA-037-F3-β — PhoneAuthView SMS 발송 + 인증 본 로직

- **유형**: frontend-screen (script 영역 — developer 본 작업)
- **영역**: app
- **모듈**: app-frontend / login
- **작업 유형**: 신규(α 의 스켈레톤 골격에 본 로직 채움)
- **목적**: PhoneAuthView 의 SMS 발송 / 인증번호 입력 / 인증 호출 / 정식 LoginResponse 적용 / MainView 라우팅 흐름 완성. P2~P8 결정 그대로 적용.
- **산출물**:
  - `PhoneAuthView.vue` 의 script 영역(`fnSendSms`, `fnVerify`, `fnApplyLoginResponse`, `fnCancel`, `fnCleanupToken`, 만료 타이머)
- **호출 endpoint**:
  - `POST /comApi/baseinfo/sms-auth-sends` — body `{ cmpnyCd, mblNo: mblNo.replace(/-/g, '') }`
  - `POST /comApi/login/verify-phone-auth` — body `{ mblNo: mblNo.replace(/-/g, ''), certNo }`, Authorization 자동 부착(axios 인터셉터)
- **정책서 출처**:
  - 공통 §3.4(자동 토큰 갱신/만료 — 임시 토큰 10분 만료는 자동 갱신 대상 아님; 정식 토큰 발급 전 단계).
  - 공통 §11.1(PII 최소 수집 — 휴대폰 번호 입력값은 인증 호출에만 사용하고 sessionStorage 보관 금지).
  - 공통 §13.3(공통 UI/UX — 안내 문구·확인 흐름·키보드 처리).
  - 백엔드: `LoginController.verifyPhoneAuth`(:99~122), 임시 토큰 scope 검증은 백엔드 `VerifyPhoneAuthParam.from` 내부(:31~32).
- **의존성**: α 완료.
- **우선순위 근거**: F3 의 핵심 사용자 흐름.

---

### PRAFTA-037-F3-γ — 경계 처리(취소/만료/뒤로가기/오류) + 정리

- **유형**: frontend-screen
- **영역**: app
- **모듈**: app-frontend / login
- **작업 유형**: 보완
- **목적**: F3-2 결정(취소→로그인 복귀) + P6(만료 타이머/만료 시 강제 복귀) + P10(뒤로가기 동작) + 에러 메시지 한국어화. β 의 happy path 위에 비행위/실패 경로 보강.
- **산출물**:
  - `PhoneAuthView.vue` 의 `fnCancel`/`onBeforeRouteLeave`/`fnCleanupToken`/만료 타이머 + 알림 표준 메시지 일관화
- **정책서 출처**:
  - 공통 §3.4(세션 만료 — 임시 토큰 만료 시 명시적 안내).
  - 공통 §11.1(임시 토큰 폐기 — `sessionStorage.removeItem('token')` 즉시 호출).
  - 공통 §13.3(공통 UI/UX — confirm/alert 흐름).
- **의존성**: β 완료.
- **우선순위 근거**: F3 의 안전 가드.

---

## 5. 의존성 그래프

```
prafta-036 (백엔드 완료, 운영 적용 완료)
   └─→ F3-α (라우트 + 스켈레톤 + LoginView 분기)
          └─→ F3-β (SMS 발송 / 인증 본 로직)
                 └─→ F3-γ (취소 / 만료 / 뒤로가기 / 메시지 정리)
```

F3-α/β/γ 는 모두 동일 PR 권장(작업 단위만 분해).

---

## 6. 비기능 요구사항

### 6.1 보안

| 항목 | 요구사항 |
|---|---|
| 임시 토큰 보관 | `sessionStorage.token` 일시 점유(P2). 인증 성공 시 정식 token 으로 교체. 인증 취소·실패·만료·라우트 이탈·뒤로가기 시 `removeItem('token')` 으로 즉시 폐기. |
| sessionStorage 의 다른 gv_* 키 | 임시 토큰 단계에서는 세팅 금지(P3). |
| 휴대폰 번호 입력 | sessionStorage / localStorage / userStore 어디에도 보관 금지(§11.1). ref(`mblNo`) 메모리에만 머묾. |
| URL 쿼리스트링 | 임시 토큰/휴대폰번호/cmpnyCd 를 URL 에 노출 금지(P4). vue-router state 사용. |
| 401 인터셉터 | 인증대기 단계의 401 → 인터셉터가 refresh 시도 → refreshToken 부재 → forceLogout → 로그인 화면. 본 화면에서 추가 처리 없음. |
| CSRF | JWT Bearer 인증 — 대상 아님. |
| PII 로그 | console.log 에 mblNo/certNo 출력 금지. 디버그용은 `[PHONE_AUTH]` prefix + 마스킹 또는 미출력. |

### 6.2 UI/UX (공통 §13.3)

| 항목 | 요구사항 |
|---|---|
| webview 라우트 깊이 | 1단계 유지. LoginView → PhoneAuthView → MainView 또는 LoginView(취소). |
| 뒤로가기 | H/W back 버튼·헤더 ← 버튼 모두 `fnCancel` 흐름과 동일 (P10). `onBeforeRouteLeave` 가드. |
| 키보드 포커스 | 진입 즉시 휴대폰 번호 input 포커스. "인증요청" 후 인증번호 input 자동 포커스. |
| 안내 문구 | "관리자가 생성한 계정은 첫 로그인 시 본인인증이 필요합니다. 등록된 휴대폰번호로 인증을 진행해 주세요." (웹 F2 `PhoneAuthPop.vue:30-33` 동일). |
| 타이머 | (a) 임시 토큰 잔여 시간(`mm:ss`, P6) 화면 상단. (b) SMS 인증번호 재발송 카운트다운(60초) 인증요청 버튼 옆. |
| 색상 팔레트 | 기존 LoginView.vue 동일(P9). |
| 한국어 메시지 | 모든 alert/confirm/안내 한글. `resolveApiErrorMessage(err, fallback)` 우선 표시. |
| 모바일 키보드 | input type 분리 — 휴대폰 tel / 인증번호 tel(또는 number). |

### 6.3 한국어 메시지 표준

| 상황 | 메시지 |
|---|---|
| 휴대폰번호 형식 오류 | `휴대폰번호를 올바르게 입력해 주세요.` |
| SMS 발송 성공 | `인증번호가 발송되었습니다.\n6자리 인증번호를 입력해 주세요.` |
| SMS 발송 실패(백엔드 메시지 없음) | `인증번호 발송 중 오류가 발생했습니다.` |
| 인증번호 미입력 | `인증번호를 입력해 주세요.` |
| 인증 실패(백엔드 메시지 없음) | `인증에 실패했습니다.` |
| 취소 confirm | `본인인증을 취소하면 로그인 화면으로 돌아갑니다.\n계속하시겠습니까?` |
| 임시 토큰 만료 | `인증 시간이 만료되었습니다.\n다시 로그인해 주세요.` |
| 화면 진입 안내 | `관리자가 생성한 계정은 첫 로그인 시 본인인증이 필요합니다.\n등록된 휴대폰번호로 인증을 진행해 주세요.` |
| state 없이 직접 진입(새로고침 등) | `인증 정보가 없습니다.\n다시 로그인해 주세요.` |

### 6.4 트랜잭션 / 상태 일관성

- PhoneAuthView 가 정식 LoginResponse 를 받기 직전까지 sessionStorage 의 gv_* 키는 비어 있다. 정식 응답 적용은 P5 의 `fnApplyLoginResponse` 패턴(웹 F2 동일)으로 한 번에. 부분 적용 후 라우팅 실패 시 사용자가 어색한 상태에 갇히지 않도록 setItem 들과 `router.replace` 는 같은 함수 안에서 순차 실행.

### 6.5 테스트 / 검증

| 케이스 | 기대 동작 |
|---|---|
| 정상 로그인 (인증대기 아닌 계정) | 기존 흐름 그대로. PhoneAuthView 미경유. |
| 인증대기 계정 정상 인증 | LoginView → PhoneAuthView(임시 토큰 부착) → SMS 발송 → 인증번호 입력 → verify-phone-auth → 정식 LoginResponse → MainView. |
| 취소(헤더 ← 버튼) | confirm → "확인" 시 임시 토큰 폐기 + `router.replace('/')`. 재로그인 가능. |
| 뒤로가기(H/W back) | `onBeforeRouteLeave` 가드 동일 흐름. |
| SMS 발송 실패(5xx) | alert + 같은 화면 유지. 임시 토큰 보존. 재시도 가능. |
| 인증번호 오류(4xx) | alert + 같은 화면 유지. |
| 임시 토큰 만료(10분) | 타이머 0 도달 시 alert + `router.replace('/')`. sessionStorage.token 제거. |
| 화면 새로고침(F5/webview reload) | history state 소실 → 진입 직후 alert("인증 정보가 없습니다.") + `router.replace('/')`. |
| 인증 후 메인 진입 직후 후속 화면 | 정상 토큰으로 axios 호출. 인터셉터 변경 없으니 회귀 위험 낮음. |
| 401 인터셉터 회귀 | refreshToken 부재 → forceLogout. 무한 루프 없음. |

---

## 7. 미해결 / Follow-up 후보

| # | 항목 | 비고 |
|---|---|---|
| F3-X1 | 공통 정책서 §3.5 계정 상태 표 정정 | 현재 `03-account-auth.md` §3.5 는 활성/미사용/탈퇴 3개만 기재. prafta-036 D2 에서 `04 인증대기` 신설했으나 정책서 본문 미반영. 본 작업과 별개로 정책서 정정 follow-up. |
| F3-X2 | 디자인 토큰(CSS 변수) 도입 | P9 결정대로 본 작업은 LoginView 의 하드코딩 색상 팔레트와 일치시키지만, 앱 프론트 전반의 디자인 토큰 정착은 별도 follow-up. |
| F3-X3 | F1(첫 로그인 강제 비밀번호 변경)과 결합 | F1 적용 시 LoginResponse 에 `mustChangePassword` 가 추가될 가능성. 그때 PhoneAuthView 의 `fnApplyLoginResponse` 가 분기를 인지하지 못하면 인증대기 통과 직후 강제 변경 화면을 거치지 못한다. F1 분해 시 본 화면도 동시 보완 필요. |
| F3-X4 | 인증 실패 횟수 제한 / lock-out | 백엔드 정책. 본 작업은 프론트만. 백엔드가 lock-out 응답 코드 추가 시 본 화면이 식별해 별도 안내. |
| F3-X5 | 동일 계정의 동일 휴대폰 충돌 | prafta-036 E 단계 결정: 다른 사용자의 `MBL_NO_HMAC` 충돌 시 "이미 다른 계정에서 사용 중인 휴대폰번호입니다" 메시지. 본 화면은 백엔드 메시지를 그대로 표시. UX 보완 가능. |
| F3-X6 | 디바이스 등록(`gv_deviceId`)과의 결합 | 앱 axios 인터셉터가 모든 요청에 `gv_deviceId` 부착(`axios.js:95`). 임시 토큰 단계의 verify-phone-auth 호출도 동일. 백엔드가 이 deviceId 를 인증대기 단계에서 어떻게 처리하는지 별도 확인 필요. 본 작업에서는 자동 부착만 신뢰. |
| F3-X7 | 만료 타이머 정밀도 | P6 의 타이머는 JWT exp 디코딩 기반 카운트다운. webview 가 백그라운드 진입(앱 전환) 후 복귀 시 동기화 필요. `visibilitychange` 이벤트 핸들러 추가 권장 — γ 단계에서 포함 가능. |
| F3-X8 | E2E 자동화 테스트 | Flutter 셸 + webview 통합 환경에서의 자동화는 현재 미구축. 본 작업은 수동 검증 + 백엔드 단위 테스트 의존. |

---

## 8. PhoneAuthView.vue 골격 (template + style 만, script 는 TODO)

> α 단계에서 디스크에 작성할 파일의 골격. β/γ 단계에서 developer 가 `// TODO(developer):` 부분을 채운다.
> 파일 위치: `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/login/PhoneAuthView.vue`

```vue
<template>
  <div class="phone-auth-page">
    <!-- 상단 헤더 (뒤로가기) -->
    <div class="header">
      <button @click="fnCancel" class="back-btn" aria-label="뒤로가기">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" fill="none"
             viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <!-- 임시 토큰 잔여시간 카운트다운(P6) -->
      <span class="ttl-timer" v-if="ttlSeconds > 0">
        남은 시간 {{ formattedTtl }}
      </span>
    </div>

    <!-- 본문 -->
    <div class="content">
      <h2 class="title">휴대폰 본인인증</h2>

      <p class="notice">
        관리자가 생성한 계정은 첫 로그인 시 본인인증이 필요합니다.<br />
        등록된 휴대폰번호로 인증을 진행해 주세요.
      </p>

      <!-- 휴대폰 번호 입력 -->
      <div class="field">
        <label class="field-label" for="mblNo">휴대폰 번호</label>
        <div class="input-with-action">
          <input
            id="mblNo"
            ref="mblNoFcs"
            v-model="mblNo"
            type="tel"
            placeholder="휴대폰번호 11자리"
            maxlength="13"
            :disabled="verified"
            @blur="focusKill"
            class="form-input"
          />
          <button
            class="btn btn-primary"
            @click="fnSendSms"
            :disabled="resendTimer > 0 || verified"
          >
            {{ resendTimer > 0 ? `${resendTimer}초 후 재요청` : '인증요청' }}
          </button>
        </div>
      </div>

      <!-- 인증번호 입력 -->
      <div class="field">
        <label class="field-label" for="certNo">인증번호</label>
        <div class="input-with-action">
          <input
            id="certNo"
            ref="certNoFcs"
            v-model="certNo"
            type="tel"
            placeholder="인증번호 6자리"
            maxlength="6"
            :disabled="!authReqSent || verified"
            class="form-input"
          />
          <button
            class="btn btn-primary"
            @click="fnVerify"
            :disabled="!authReqSent || verified"
          >
            확인
          </button>
          <span class="ok-mark" v-show="verified">✅</span>
        </div>
      </div>
    </div>

    <!-- 하단 -->
    <div class="footer">
      <button class="btn btn-secondary" @click="fnCancel">취소</button>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
// PRAFTA-037-F3: 앱(webview) 인증대기 분기 화면.
// 라우트 진입 시 history.state.phoneAuthToken, cmpnyCd 를 받아 sessionStorage.token 에 일시 점유.
// 인증 성공 시 정식 LoginResponse 를 sessionStorage / userStore 에 적용 후 MainView 로 이동.
// 취소 / 만료 / 뒤로가기 시 임시 토큰 폐기 + 로그인 화면 복귀.

import { ref, computed, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import axios from '@/api/axios'
import { useUserStore } from '@/stores/userStore'
import { resolveApiErrorMessage } from '@/utils/apiError'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()

// ───────────── 상태 ─────────────
const cmpnyCd = ref('')
const mblNo = ref('')
const mblNoFcs = ref(null)
const certNo = ref('')
const certNoFcs = ref(null)
const authReqSent = ref(false)
const verified = ref(false)

// 인증번호 재발송 카운트다운(60초)
const resendTimer = ref(0)
let resendInterval = null

// 임시 토큰 잔여시간(P6)
const ttlSeconds = ref(0)
let ttlInterval = null
const formattedTtl = computed(() => {
  const m = String(Math.floor(ttlSeconds.value / 60)).padStart(2, '0')
  const s = String(ttlSeconds.value % 60).padStart(2, '0')
  return `${m}:${s}`
})

// 화면 이탈 직전 정리 완료 표시(중복 cleanup 방지)
let cleanedUp = false

// ───────────── 라이프사이클 ─────────────
onMounted(() => {
  // TODO(developer):
  // 1) history.state 에서 phoneAuthToken, cmpnyCd 추출
  //    - 없으면 alert("인증 정보가 없습니다.\n다시 로그인해 주세요.") + router.replace('/')
  // 2) sessionStorage.setItem('token', phoneAuthToken)
  //    - axios 인터셉터가 자동으로 Authorization 부착
  // 3) cmpnyCd.value 세팅 (SMS 발송 body 에 사용)
  // 4) JWT exp claim 디코딩 후 ttlSeconds 계산 + 1초 간격 setInterval 시작
  //    - 0 도달 시 fnExpire() 호출
  // 5) mblNoFcs.value.focus()
})

onBeforeUnmount(() => {
  if (resendInterval) clearInterval(resendInterval)
  if (ttlInterval) clearInterval(ttlInterval)
})

// 뒤로가기 / 라우트 이탈 가드(P10)
onBeforeRouteLeave(async (to, from, next) => {
  // TODO(developer):
  // - cleanedUp 면 그대로 next() (이미 정리됨)
  // - 인증 성공(verified.value === true)이면 그대로 next() (MainView 이동)
  // - 그 외 사용자 뒤로가기:
  //   - proxy.$confirm("본인인증을 취소하면 로그인 화면으로 돌아갑니다.\n계속하시겠습니까?")
  //   - 확인: fnCleanupToken() 후 next()
  //   - 취소: next(false)
})

// ───────────── 메서드 ─────────────
const fnCleanupToken = () => {
  // TODO(developer):
  // - sessionStorage.removeItem('token')
  // - axios.defaults.headers.common.Authorization 제거
  // - resendInterval / ttlInterval clear
  // - cleanedUp = true
}

const fnExpire = async () => {
  // TODO(developer):
  // - proxy.$alert("인증 시간이 만료되었습니다.\n다시 로그인해 주세요.")
  // - fnCleanupToken()
  // - router.replace('/')
}

const fnSendSms = async () => {
  // TODO(developer):
  // - 휴대폰번호 형식 검증(proxy.$util.validatePhoneNumber). 실패 시 alert + 포커스 복귀.
  // - POST /comApi/baseinfo/sms-auth-sends { cmpnyCd: cmpnyCd.value, mblNo: mblNo.value.replace(/-/g, '') }
  // - 성공:
  //   - authReqSent.value = true
  //   - alert("인증번호가 발송되었습니다.\n6자리 인증번호를 입력해 주세요.")
  //   - certNoFcs.value.focus()
  //   - resendTimer 60 카운트다운 시작
  // - 실패:
  //   - alert(resolveApiErrorMessage(err, "인증번호 발송 중 오류가 발생했습니다."))
}

const fnVerify = async () => {
  // TODO(developer):
  // - certNo 입력 검증(빈 값 / 6자리). 실패 시 alert.
  // - POST /comApi/login/verify-phone-auth { mblNo: mblNo.value.replace(/-/g, ''), certNo: certNo.value }
  //   (Authorization 은 인터셉터 자동 부착)
  // - 성공: response.data === 정식 LoginResponse
  //   - verified.value = true
  //   - fnApplyLoginResponse(response.data)
  // - 실패:
  //   - alert(resolveApiErrorMessage(err, "인증에 실패했습니다."))
  //   - 401 인 경우는 axios 인터셉터가 forceLogout 처리(임시 토큰 무효) → 본 화면이 더 처리할 것 없음
}

const fnApplyLoginResponse = (data) => {
  // TODO(developer): (P5 — 웹 F2 LoginView.vue fnApplyLoginResponse 동일 패턴)
  // - { token, refreshToken, userCd, userId, userNm, cmpnyCd, siteCd, siteNo, siteNm,
  //     nodeCd, nodeNm, authCd, authLevel } = data
  // - sessionStorage.setItem 12개 (token + gv_* 11)
  //   - PII(mblNo/email) 는 저장 금지(§11.1)
  // - localStorage.setItem('refreshToken', refreshToken)
  // - axios.defaults.headers.common.Authorization = `Bearer ${token}`
  // - userStore.setUser({ cmpnyCd, userCd, userId, userNm, siteCd, siteNo, siteNm,
  //                        nodeCd, nodeNm, authCd, authLevel })
  // - cleanedUp = true (라우트 이탈 가드가 차단하지 않도록)
  // - router.replace('/MainView')
}

const fnCancel = async () => {
  // TODO(developer):
  // - proxy.$confirm("본인인증을 취소하면 로그인 화면으로 돌아갑니다.\n계속하시겠습니까?")
  // - 확인: fnCleanupToken() + router.replace('/')
  // - 취소: 아무 동작 없음
}

const focusKill = (e) => {
  // TODO(developer):
  // - id === 'mblNo' 일 때 휴대폰번호 포맷팅(proxy.$util.formatPhoneNumber) 적용
}
</script>

<style scoped>
.phone-auth-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  box-sizing: border-box;
}

/* 헤더 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  margin-top: 1rem;
}

.back-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #1f1f1f;
  padding: 0.25rem;
}

.ttl-timer {
  font-size: 0.85rem;
  color: #ef4444;
  font-weight: 600;
}

/* 본문 */
.content {
  flex: 1;
  padding: 0 1.5rem;
}

.title {
  font-size: 1.5rem;
  font-weight: 800;
  color: #1f1f1f;
  margin: 1rem 0 1.25rem;
}

.notice {
  background: #eff6ff;
  border-radius: 10px;
  padding: 0.75rem 0.875rem;
  font-size: 0.8rem;
  color: #1d4ed8;
  line-height: 1.5;
  margin: 0 0 1.5rem;
}

/* 필드 */
.field {
  margin-bottom: 1rem;
}

.field-label {
  display: block;
  font-size: 0.85rem;
  color: #4b5563;
  margin-bottom: 0.4rem;
}

.input-with-action {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.form-input {
  flex: 1;
  padding: 0.75rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 0.95rem;
  color: #1f1f1f;
  background: #fff;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.15s;
}

.form-input::placeholder {
  color: #9ca3af;
}

.form-input:focus {
  border-color: #5cb85c;
}

.form-input:disabled {
  background: #f9fafb;
  color: #6b7280;
}

/* 버튼 */
.btn {
  padding: 0.7rem 1rem;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #5cb85c;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #4ca94c;
}

.btn-secondary {
  background: #fff;
  color: #4b5563;
  border: 1px solid #e5e7eb;
}

.btn-secondary:hover {
  background: #f9fafb;
}

.ok-mark {
  font-size: 1rem;
  color: #16a34a;
}

/* 푸터 */
.footer {
  padding: 1rem 1.5rem 1.5rem;
}

.footer .btn {
  width: 100%;
  padding: 0.85rem;
}
</style>
```

---

## 9. LoginView.vue 패치 골격 (분기 추가)

> α 단계에서 LoginView.vue 의 `fnSubmitLogin` 응답 핸들러에만 분기 추가. 그 외 코드는 무변경.

```javascript
// 기존 (LoginView.vue:128~194 의 try 블록 안)
const response = await axios.post('/comApi/login/login', {
  userId: userId.value,
  userPw: password.value,
})

if (response.status === 200) {
  // ↓↓↓ PRAFTA-037-F3 신규 분기 (P3/P4) ↓↓↓
  if (response.data?.nextStep === 'PHONE_AUTH') {
    // 임시 토큰/회사코드는 URL 쿼리스트링이 아닌 history state 로 전달(JWT 노출 방지).
    router.push({
      path: '/PhoneAuth',
      state: {
        phoneAuthToken: response.data.token,
        cmpnyCd: response.data.cmpnyCd,
      },
    })
    return
  }
  // ↑↑↑ 신규 분기 끝 ↑↑↑

  // ↓ 기존 코드(정상 토큰 적용) — 그대로 ↓
  const { token, userCd, userId: id, userNm, cmpnyCd, ... } = response.data
  // ... (이하 기존 코드 무변경)
}
```

---

## 10. router/index.js 패치 골격

```javascript
// 기존 routes 배열에 추가 (라우트 정의 6 ~ 36 번 줄 안)
{
  path: '/PhoneAuth',
  name: 'PhoneAuth',
  component: () => import('@/views/login/PhoneAuthView.vue'),
},

// 기존 publicPaths 배열에 추가 (라우트 정의 46 ~ 54 번 줄)
const publicPaths = [
  '/',
  '/ActInfoSrch',
  '/TermsInfo',
  '/TermsDetail',
  '/QrScanner',
  '/JoinUser',
  '/PhoneAuth', // PRAFTA-037-F3: 인증대기 단계는 정식 토큰 미발급 → public 라우트로 취급
]
```

---

## 11. 차후 분해 시 메모

- F3 는 단일 PR 권장. α/β/γ 분해는 작업 진행 추적용.
- 본 plan 의 결정은 prafta-036-plan §1 의 결정과 일관(임시 토큰 sessionStorage 슬롯 활용 등).
- F1(첫 로그인 강제 비밀번호 변경) 분해 시 PhoneAuthView 의 `fnApplyLoginResponse` 가 `mustChangePassword` 분기를 인지하도록 동시 보완(§7 F3-X3).
- 본 plan 은 단일 출처(SSOT)이며, 후속 developer/security/qa 모두 본 문서를 따른다.

---

**최종 작성**: 2026-05-29 — planner 분해 완료.
