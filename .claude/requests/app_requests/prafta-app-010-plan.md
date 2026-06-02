# prafta-app-010 작업 분해 (단일 출처)

> 대상: 모바일 앱 (`prafta-app-frontend` + `prafta-backend` 앱 API). 요청서가 `app_requests/` 하위이므로 영역은 **app**.
> 시안 명세(`refs/prafta-app-010/prafta-request-my-page.md`)는 가정이 많아 실제 스키마/코드와 다수 충돌한다. 본 문서는 **메인 세션이 MCP `prafta-mysql` + 실제 코드로 확정한 사실 + 사용자 확정 결정(2026-05-30)**을 단일 진실로 삼아 교정한 결과다.
> 화면 명세: `prafta-app-010-ui-spec.md` / 질의·결정 정리: `prafta-app-010-questions.md`
> ✅ **사용자 확정(2026-05-30) 반영 완료.** P0 5건 전건 확정 → 백엔드 착수 차단 해제. 아래 §0.5 "사용자 확정 결정" 참조.

---

## 0. 진입점 (확정)

- MainView(`src/views/main/MainView.vue`) 우측 상단 아바타 → `HomeHeader.vue`의 `@click:avatar="onAvatarClick"`.
- **현재 `onAvatarClick`은 로그아웃 confirm을 띄우는 임시 동작**(MainView.vue L353-360, 주석에 "아바타 메뉴 UI는 본 라운드 outside scope"로 명시). 본 작업이 그 후속이다.
- 교체 방향: `onAvatarClick` → `router.push('/MyPage')`. (기존 로그아웃 로직은 마이페이지 메인의 로그아웃 버튼으로 이동.)
- 라우터는 정적(`src/router/index.js`). 신규 경로를 정적 등록한다(동적 DB 메뉴 아님). 기존 패턴: `/MyLeaveSummaryView`, `/MyRequests` 등과 동일하게 `routes` 배열에 추가. **단, beforeEach 토큰 게이트가 걸리도록 `publicPaths`에 넣지 않는다**(마이페이지는 인증 필수).

---

## 0.5 사용자 확정 결정 (2026-05-30, 단일 진실)

본 6개 결정이 본 plan의 교정표·작업단위표·API명세보다 **상위**다. 충돌 시 본 절을 따른다.

| D# | 결정 | 영향 |
|---|---|---|
| **D1 (구 Q1)** | **PII = 마스킹 표시 + 수정 진입 시에만 복호화 프리필.** 메인/조회 응답은 마스킹(휴대폰 `010-****-8295`=LAST4 활용, 이메일·생년월일도 마스킹). "개인정보 수정" 화면 진입 시에만 서버가 복호화해 프리필. webview 로그/캐시 평문 잔존 주의. | 010-01은 마스킹 전용. 복호화 전체값은 010-01b(수정 진입 전용) 분리 |
| **D2 (구 Q2)** | **앱 코드 완전 분리. web `com.prafta.web.*` API 재사용 금지.** 프리셋을 web `user04`로 재사용하려던 계획 **폐기**. 앱 전용 신규 모듈(`com.prafta.app.mypage.mypage01`)로 controller/service/mapper XML/DTO 전부 신규 작성. **테이블 `tb_aprv_line_preset`/`_d`는 데이터 저장소이므로 공유 OK**(신규 테이블 만들지 말 것). user04의 SQL/로직은 "참고"만, web 코드 호출·의존 금지. 결재자 후보 검색도 앱 전용 신규 엔드포인트. | 010-05를 user04 재사용 → 앱전용 신규 모듈로 전면 교체 |
| **D2-부가** | **전수조사 결과: 현재 앱이 web 전용 API(`/user04/*` 등 `com.prafta.web.*`)를 직접 호출하는 화면은 0건**(전부 `/appApi` 또는 `/comApi`). 따라서 본 작업의 web 분리 대상은 이 프리셋 건뿐이다. | 추가 분리 작업 없음 |
| **D3 (구 Q3 일부)** | **`/comApi/*` 공통 레이어(login/baseinfo/auth)는 재사용 유지**(app·web 공용 common 레이어이며 web 전용 아님). 로그아웃은 기존 `/comApi/login/logout` 재사용. 단 D4 휴대폰 변경 검증은 예외로 신규. | 010-06(로그아웃)은 기존 comApi 재사용 → BE 신규 불필요 |
| **D4 (구 Q3)** | **휴대폰 변경 검증 = 토큰 미발급 앱 전용 엔드포인트 신규.** 기존 `/comApi/login/verify-phone-auth`는 검증 성공 시 로그인 토큰을 발급하는 부작용이 있어 마이페이지에서 사용 불가. `/appApi/*`에 "SMS 발송 + 검증(토큰 미발급, 성공 시 단발성 verificationToken만 반환)" 전용 엔드포인트 신규 설계. SMS 발송 인프라(문자 게이트웨이/`tb_sms_auth_code`)는 재사용하되 엔드포인트는 앱 전용. | 010-03 확정. 발송도 앱 전용 래핑 엔드포인트로 신규(인프라만 재사용) |
| **D5 (구 Q13)** | **회원 탈퇴 시 연차 자동취소 로직 구현하지 않음.** 퇴사일 기준 사용연차 정산은 별도 페이지로 개발 예정. 탈퇴 처리에서 `tb_user_attd_req` 연차건을 건드리지 말 것. 탈퇴 범위: `ACCOUNT_STATUS='03'` + PII 마스킹(USER_NM `김○○`, MBL/EMAIL/BIRTH 암호화필드 처리) + `WITHDRAWAL_DATE` 기록 + `tb_del_user` 기록 + 토큰 폐기. 연차/결재 자동처리 제외. 프리셋 hard delete는 유지. | 010-07에서 연차 자동취소 단계(①) + 결재자 알림(Q14) **제거** |
| **D6 (구 Q6)** | **성별 = SYS004 시스템코드 확정: 100:남성 / 200:여성** (`tb_syst_val_d`, 앱 회원가입 JoinUser와 동일 패턴으로 systCode 드롭다운 재사용). NULL(미설정) 허용. | gender 코드값 확정. select는 SYS004 systCode 조회 재사용 |

> ⚠️ **D1 마스킹 구현 메모**: 010-01(메인/조회)은 마스킹 문자열만 반환(복호화 X 또는 복호화 후 마스킹). 010-01b(수정 진입 전용 조회)에서만 복호화 전체값을 반환한다. 단일 엔드포인트에 `mode=edit` 쿼리로 분기할지, 별도 엔드포인트로 둘지는 §4 참조(본 plan은 별도 엔드포인트 `GET /appApi/mypage/profile/edit` 권장 — 책임 분리·캐시 정책 명확).

---

## 1. 요청서 가정 vs 실제 스키마 교정표 (작업자 필독)

| # | 시안 가정 | 실제 (확정 사실) | 작업 시 적용 |
|---|---|---|---|
| C1 | 사용자 테이블 `tb_user_mgmt` | **`tb_user`** | 모든 컬럼 참조를 tb_user로 |
| C2 | 아이디 = `USER_CD` | `USER_CD`(PK,내부코드)와 `USER_ID`(로그인ID)가 **별도** | "아이디" 표시는 `USER_ID`, 내부키는 `USER_CD` |
| C3 | 소속부서 `DEPT_CD`/`tb_dept_mgmt` | 소속은 **`NODE_CD`**(조직 노드). 부서명은 세션 `gv_nodeNm` 또는 노드 조회 | 부서명 = NODE_CD→노드명. 세션 `gv_nodeNm` 우선 |
| C4 | 휴대폰 `MOBILE_NO`(평문) | **`MBL_NO_ENC`(AES-GCM)+`MBL_NO_HMAC`+`MBL_NO_LAST4`** | **D1**: 조회는 LAST4 마스킹(`010-****-8295`), 수정 진입 시에만 복호화 전체. 변경은 ENC/HMAC/LAST4 3컬럼 동시 갱신 (D4) |
| C5 | 이메일 `EMAIL`(평문) | **`EMAIL_ENC`+`EMAIL_HMAC`+`EMAIL_DOMAIN`** | **D1**: 조회는 마스킹(`t***@test.com`), 수정 진입 시에만 복호화 전체. 변경은 3컬럼 동시 갱신 |
| C6 | 생년월일 `BIRTH_DATE`(varchar8 평문) | **`BIRTH_DT_ENC`(AES-GCM)** | **D1**: 조회는 마스킹(`1993-09-**` 등), 수정 진입 시에만 복호화 전체. 변경은 ENC 갱신 |
| C7 | 비밀번호 `PASSWORD_HASH` | **`USER_PW`** (해시). 변경일시 `PWD_CHG_DTIME`, 잠금 `PWD_LOCK_YN`/`PWD_FAIL_CNT`/`PWD_LOCK_EXPIRE_DTIME` 인프라 존재 | 비번 변경 시 `USER_PW`+`PWD_CHG_DTIME` 갱신. 잠금은 기존 로그인 정책과 정합 (Q9) |
| C8 | 입사일 `HIRE_DATE` varchar8 | 동일(varchar8 YYYYMMDD) | 읽기 전용 표시(YYYY-MM-DD 포맷) |
| C9 | 성별 `GENDER` varchar10, 'MALE'/'FEMALE' | `GENDER` varchar6, NULL 허용. **D6 확정: SYS004 코드 100:남성/200:여성** | gender 코드값 100/200, NULL 허용. select는 SYS004 systCode 재사용(JoinUser 패턴) |
| C10 | `LAST_LOGIN_AT` datetime | **`LAST_LOGIN_DTIME`** | 표시용 |
| C11 | 계정상태 `USE_STATUS='WITHDRAWN'` 신규 | **`ACCOUNT_STATUS`(varchar20 [SYS013])** 이미 존재. 01활성/02잠김/03탈퇴/04인증대기 | 탈퇴 = `ACCOUNT_STATUS='03'`. ENUM 신규 불필요 |
| C12 | 탈퇴일 컬럼 신규 추가 | **`WITHDRAWAL_DATE`(varchar8) 이미 존재** | 탈퇴 시 그대로 사용. 컬럼 추가 불필요 |
| C13 | 탈퇴 이력 `tb_user_withdrawal_history` 신규 | **`tb_del_user` 이미 존재** (CMPNY_CD, USER_ID, USER_NM, INSERT_NO, INSERT_DATE) | 마스킹 이름 컬럼 없음 → USER_NM에 마스킹 이름 저장 (Q11 권장안) |
| C14 | 프리셋 `tb_user_approval_preset`/`_member` | **`tb_aprv_line_preset`/`tb_aprv_line_preset_d`** | 마스터 PK(CMPNY_CD,PRESET_ID)+USER_CD/PRESET_NM/DEFAULT_YN/USE_YN. 디테일 PK(CMPNY_CD,PRESET_ID,STEP_NO)+APPROVER_USER_CD. **D2: 테이블만 공유, 앱 전용 매퍼 신규** |
| C15 | 기본 프리셋 컬럼 `IS_DEFAULT='Y'` | **`DEFAULT_YN`** (Y/N) | 정렬·기본지정 모두 DEFAULT_YN |
| C16 | 프리셋 멤버 `STEP_SEQ`(0-base+1) | **`STEP_NO`** | 순번 컬럼명 STEP_NO |
| C17 | 프리셋 유니크 `(CMPNY_CD,USER_CD,PURPOSE_TYPE,PRESET_NM)` | 실제 `tb_aprv_line_preset`에 **PURPOSE_TYPE 컬럼 없음** (소유자별 단일 프리셋군) | 이름 중복은 (CMPNY_CD,USER_CD,PRESET_NM) 기준. PURPOSE_TYPE 가정 폐기 |
| C18 | 마이페이지 전용 프리셋 API 9개 신규 | web `user04`에 CRUD 완비. **그러나 D2로 재사용 금지** → 앱 전용 신규 모듈 `com.prafta.app.mypage.mypage01`에 controller/service/mapper XML/DTO 신규 작성. user04 SQL은 참고만 | **D2 적용. user04 호출·의존 금지** |
| C19 | 결재자 검색 `/api/app/attd/req/leave/approver-search` | user04 `/approval-candidates`(후보 목록 + `myRankSortIdx`) | **D2**: web 호출 금지. 앱 전용 `GET /appApi/mypage/approval-candidates` 신규(user04 SQL 참고하여 앱 매퍼에 재작성) |
| C20 | 신청 연차 상태 변경(탈퇴 시) | `tb_user_attd_req.REQ_STATUS` [SYS033] 01신청/02승인/03반려/04취소 | **D5: 탈퇴 시 연차 자동취소 미구현.** tb_user_attd_req 미변경 |
| C21 | SMS 게이트웨이 신규 연동 | **기존 인프라 존재**: SMS 발송(문자 게이트웨이/`tb_sms_auth_code`), 검증. 발송 인프라 재사용 가능. 단 `/comApi/login/verify-phone-auth`는 로그인 응답 발급 부작용 | **D4**: 발송·검증 모두 **앱 전용 엔드포인트** 신규(인프라만 재사용, comApi verify 직접 호출 금지) |
| C22 | 마이페이지 파일 경로 `prafta-web-frontend/src/views/mypage/` | **오기**. app_requests 영역 → `prafta-app-frontend/prafta-app-frontend/src/views/mypage/` | 골격을 앱 프론트에 작성(완료) |
| C23 | 앱 API 프리픽스 `/api/app/...` | 실제 앱 컨트롤러는 `@RequestMapping("/{module}")` + `ApiPrefixConfig`가 `com.prafta.app.*`에 `/prafta/appApi` 자동 부여 → 최종 `/prafta/appApi/{module}/...`. 프론트 호출 표기는 `/appApi/...` kebab | 모든 신규 엔드포인트 `@RequestMapping("/mypage")` 등 → 최종 `/appApi/mypage/...` kebab |
| C24 | 알림센터 진입(`/api/app/notifications/unread-count`) | 공지/알림 도메인 **미구축**(app-001에서 0 고정) | 마이페이지 알림 아이콘 미노출(Q12 권장안). 도메인 구축 후 일괄 도입 |

---

## 2. 재사용 자산 목록

### 백엔드

> ⚠️ **D2 핵심**: 결재선 프리셋은 **web `user04` 코드를 호출하지 않는다.** 앱 전용 신규 모듈에 SQL을 재작성한다. 아래 user04 목록은 **"참고용 SQL 출처"**일 뿐 의존 대상이 아니다.

- **(참고만) 결재선 프리셋 CRUD SQL** — `com.prafta.web.user.user04` 패키지.
  - 참고 매퍼: `User04Mapper.xml`의 `selectPresetMasters`/`selectPresetStepsByUser`/`selectPresetOwner`/`selectNextPresetId`/`countActiveCandidate`/`insertPresetMaster`/`updatePresetMaster`/`insertPresetStep`/`deletePresetSteps`/`deletePresetMaster`/`clearDefaultForUser`/`setDefault`/`selectApprovalCandidates`/`selectUserRankSortIdx`.
  - 참고 DTO 형태(앱에 동등 신규 작성): `PresetListResponse{presets:[{presetId,presetNm,defaultYn,steps:[{stepNo,approverUserCd,userNm,userId}]}]}`, `PresetSaveRequest{presetId,presetNm,defaultYn,approverUserCds[]}`, `PresetActionRequest{presetId}`, `ApprovalCandidateListResponse{myRankSortIdx,candidates:[{userCd,userNm,rankNm,nodeNm}]}`.
  - **앱 신규 작성 대상**: `AppMypage01Controller`/`Service`/`ServiceImpl`/`AppMypage01Mapper(.java/.xml)` + 위 DTO/Result를 `com.prafta.app.mypage.mypage01.*` 하위에 전부 신규.
  - 자기결재 차단(근태 §9.5)·이름 중복·소유자=토큰 userCd 강제 로직도 앱 service에 신규 구현(user04 로직 참고).

- **공유 테이블** — `tb_aprv_line_preset`/`tb_aprv_line_preset_d`(데이터 저장소, 공유 OK). 신규 테이블 생성 금지.
- **SMS 발송 인프라** — 문자 게이트웨이 + `tb_sms_auth_code`(인증코드 저장). **D4**: 인프라는 재사용하되 앱 전용 엔드포인트로 래핑(comApi 직접 호출 금지). 발송 로직은 `com.prafta.common.*`의 공용 발송 컴포넌트가 있으면 그것을 주입해 호출(developer가 baseinfo sms-auth-sends 내부 구현 확인 후 공용 서비스 빈 재사용; 없으면 앱 service에서 직접 인프라 호출).
- **로그아웃** — **D3**: 기존 `POST /comApi/login/logout`(refreshToken body) 재사용. 앱 전용 신규 불필요(010-06은 "기존 재사용" 확정).
- **암호화 유틸** — `common.util.AesGcmUtil`(메모리 참조), HMAC 산출 유틸. PII 컬럼 read(복호화)/write 시 사용.
- **성별 코드(SYS004)** — `tb_syst_val_d` SYS004(100:남성/200:여성). systCode 조회는 앱 회원가입(JoinUser) 패턴 재사용. 프로필 응답에 코드+라벨 동반 권장.

### 프론트 (`prafta-app-frontend/prafta-app-frontend`)
- `@/api/axios` (`api.get/post`, 인터셉터가 Authorization + gv_* 처리). 호출 표기 `/appApi/...`/`/comApi/...`.
- 세션: `sessionStorage.getItem('gv_*')` (userNm/siteNm/nodeNm/cmpnyCd/userCd). PII(휴대폰/이메일)는 세션에 **없음**(정책 §11.1) → 프로필 조회 API로만 취득. **D1**: 메인 조회는 마스킹값만, 수정 진입 시 복호화값.
- 전역 모달: `proxy.$alert` / `proxy.$confirm` (폴백 window.alert/confirm).
- 로그아웃 로직: MainView.vue `logout()` + `clearSession`/`removeRefreshToken`(`@/composables/useAuth`). 서버 로그아웃은 `POST /comApi/login/logout`(body refreshToken) — **D3 재사용**.
- 바텀시트 패턴: `views/req/components/BaseBottomSheet.vue` / `OffsiteReasonSheet.vue`(transition + dimmer + v-model).
- 풀스크린 화면 패턴: `MyLeaveSummaryView.vue`(헤더 56px + 본문 스크롤 + 푸터 + 인라인 SVG sprite + 루트에 디자인 토큰 1회 선언).
- 휴대폰 입력 포맷/검증: `proxy.$util.formatPhoneNumber` / `validatePhoneNumber`(PhoneAuthView 참조).

### 디자인 토큰 (하드코딩 금지, 루트 컨테이너에 1회 선언 패턴)
`--color-primary:#16a34a` 외 MainView/MyLeaveSummaryView 루트의 토큰 세트를 그대로 사용. 신규 색 도입 금지.

---

## 2.5 신규 앱 백엔드 패키지/엔드포인트 확정안 (developer 착수 기준)

> 앱 백엔드 네이밍 컨벤션(실측): `com.prafta.app.{module}.{module}NN` / 클래스 `App{Module}NN{Layer}` / 매퍼 XML은 `src/main/resources/com/prafta/app/{module}/{moduleNN}/mapper/App{Module}NN Mapper.xml` / 컨트롤러 `@RequestMapping("/{module}")` + `ApiPrefixConfig` 자동 프리픽스 `/prafta/appApi` → 최종 `/prafta/appApi/{module}/...`. 식별자(cmpnyCd/userCd 등)는 바디·쿼리로 받지 않고 `jwtUtil.getAllClaimsAsMap(authorization)` → `TokenInfo`로만 도출(IDOR 차단).

### 패키지 1: `com.prafta.app.mypage.mypage01` (마이페이지 본체 + 프리셋)
프로필 조회/수정, 휴대폰 변경 인증, 비밀번호 변경, 결재선 프리셋 CRUD, 결재자 후보를 **한 모듈**에 둔다(앱 마이페이지 단일 도메인). 컨트롤러는 `@RequestMapping("/mypage")`.

레이어(전부 신규):
```
com.prafta.app.mypage.mypage01
├─ controller/AppMypage01Controller.java        @RequestMapping("/mypage")
├─ service/AppMypage01Service.java               (interface)
├─ service/impl/AppMypage01ServiceImpl.java
├─ mapper/AppMypage01Mapper.java
├─ dto/request/  ProfileUpdateRequest, MobileVerifyRequest, MobileSendRequest,
│                PasswordChangeRequest, PresetSaveRequest, PresetActionRequest,
│                ApprovalCandidateRequest
├─ dto/response/ MypageProfileResponse(마스킹), MypageProfileEditResponse(복호화),
│                MobileVerifyResponse(verificationToken), PresetListResponse,
│                PresetItemResult, PresetStepItem, ApprovalCandidateListResponse
├─ result/       UserProfileResult, PresetMasterResult, PresetStepResult,
│                ApprovalCandidateResult
├─ application/param/    각 *Param.from(request, tokenInfo)
├─ application/command/  PresetSaveCommand, ProfileUpdateCommand, WithdrawCommand(010-07용은 auth 모듈로)
└─ resources/com/prafta/app/mypage/mypage01/mapper/AppMypage01Mapper.xml
```

엔드포인트 (`@RequestMapping("/mypage")` → 최종 `/prafta/appApi/mypage/...`; 프론트 표기 `/appApi/mypage/...`):

| 메서드 | 경로(프론트표기) | 작업ID | 비고 |
|---|---|---|---|
| GET | `/appApi/mypage/profile` | 010-01 | **마스킹 응답**(D1). PII는 마스킹 문자열만 |
| GET | `/appApi/mypage/profile/edit` | 010-01b | **복호화 전체 응답**(D1, 수정 진입 전용). no-store 캐시 헤더 권장 |
| PUT | `/appApi/mypage/profile` | 010-02 | 변경 필드 저장. 휴대폰 변경 시 verificationToken 필수 |
| POST | `/appApi/mypage/mobile/request-verification` | 010-03a | 앱 전용 SMS 발송(D4). 인프라 재사용 |
| POST | `/appApi/mypage/mobile/verify` | 010-03b | 앱 전용 검증(D4). **로그인 토큰 미발급**, verificationToken만 반환 |
| PUT | `/appApi/mypage/password` | 010-04 | USER_PW+PWD_CHG_DTIME 갱신 |
| GET | `/appApi/mypage/approval-presets` | 010-05 | 앱 전용 신규(user04 SQL 참고) |
| GET | `/appApi/mypage/approval-presets/{presetId}` | 010-05 | 단건 |
| POST | `/appApi/mypage/approval-presets` | 010-05 | 저장(신규/수정). body presetId null=신규 |
| POST | `/appApi/mypage/approval-presets/set-default` | 010-05 | |
| POST | `/appApi/mypage/approval-presets/delete` | 010-05 | user04 deletePreset은 hard delete(매퍼 확인). 앱도 동일 정책 채택 |
| GET | `/appApi/mypage/approval-candidates` | 010-05 | 본인 제외·활성·동일 사업장. user04 SQL 참고 |

### 패키지 2: `com.prafta.app.auth.auth01` (탈퇴)
탈퇴는 토큰 폐기·계정상태 변경 등 인증/계정 도메인이라 mypage와 분리. 컨트롤러 `@RequestMapping("/auth")`.
- **로그아웃(010-06)은 신규 만들지 않음**(D3: `/comApi/login/logout` 재사용). 따라서 auth01에는 **탈퇴만** 둔다.

```
com.prafta.app.auth.auth01
├─ controller/AppAuth01Controller.java           @RequestMapping("/auth")
├─ service/AppAuth01Service.java / impl/...
├─ mapper/AppAuth01Mapper.java (+ .xml)
├─ dto/request/WithdrawRequest
├─ dto/response/WithdrawResponse
└─ application/command/WithdrawCommand
```

| 메서드 | 경로(프론트표기) | 작업ID | 비고 |
|---|---|---|---|
| POST | `/appApi/auth/withdraw` | 010-07 | 탈퇴(D5 반영: 연차 자동취소·결재자 알림 제외) |

> ⚠️ 모듈 분할 대안: mypage01 한 모듈에 탈퇴까지 포함해도 컨벤션 위반은 아니나, "auth/계정상태·토큰" 책임을 분리하는 편이 보안 검토(security) 경계를 명확히 한다. developer가 빈 주입 간소화를 이유로 합치고자 하면 auth01 → mypage01 흡수도 허용(컨트롤러 경로만 `/auth`/`/mypage`로 분리 유지). 본 plan 권장은 분리.

---

## 3. 작업 단위 분해표

> 의존순서: BE 모듈 골격(패키지/매퍼) → BE API → FE script(developer). FE 골격(template/style)은 작성 완료(본 plan 산출). developer는 골격의 script 영역만 채운다.
> ✅ P0 확정으로 **모든 백엔드 단위 착수 가능**.

### 백엔드 (app API)

| ID | 유형 | 모듈(패키지) | 작업유형 | 요약 | 정책 출처 | 선행 | 비고 |
|---|---|---|---|---|---|---|---|
| **PRAFTA-APP-010-01** | backend | app.mypage.mypage01 | 신규 | `GET /appApi/mypage/profile` — 마이페이지 메인 **마스킹** 응답(이름/사업장명/부서명/입사일/휴대폰LAST4마스킹/이메일마스킹/성별코드+라벨/생년월일마스킹/마지막로그인/프리셋개수) | 공통 §3.1, §11.1(PII 최소·마스킹 표시), §11.2 | 없음 | **D1**: 마스킹만. 복호화 안 함(또는 복호화 후 마스킹) |
| **PRAFTA-APP-010-01b** | backend | app.mypage.mypage01 | 신규 | `GET /appApi/mypage/profile/edit` — **수정 진입 전용 복호화 전체** 응답(휴대폰/이메일/생년월일 평문). 캐시 no-store | 공통 §11.1(목적 제한·최소 노출) | 01 | **D1**: 수정 진입 시에만 호출. webview 캐시 잔존 주의(no-store) |
| **PRAFTA-APP-010-02** | backend | app.mypage.mypage01 | 신규 | `PUT /appApi/mypage/profile` — 이름/성별(SYS004)/생년월일/이메일/휴대폰 저장. 휴대폰 변경 시 verificationToken 검증. PII는 ENC/HMAC/LAST4·EMAIL_ENC·BIRTH_DT_ENC 동시 갱신 | 공통 §3.1, §11.1 | 01b,03 | 이름 즉시·무제한(Q5 확정). §11.3은 상태변경만 감사 |
| **PRAFTA-APP-010-03** | backend | app.mypage.mypage01 | 신규 | 휴대폰 변경 인증 앱전용 2엔드포인트: `POST /appApi/mypage/mobile/request-verification`(발송, 본인 외 중복 HMAC 검증) + `POST /appApi/mypage/mobile/verify`(검증→단발성 verificationToken 5분, **로그인 토큰 미발급**) | 공통 §3.3(휴대폰 본인인증), §11.1 | 없음 | **D4**: SMS 인프라만 재사용, comApi verify 호출 금지. 신규 앱 엔드포인트 |
| **PRAFTA-APP-010-04** | backend | app.mypage.mypage01 | 신규 | `PUT /appApi/mypage/password` — 현재비번 검증(USER_PW)·규칙·현재≠새 검증·USER_PW+PWD_CHG_DTIME 갱신. 세션 유지 | 공통 §3.4(세션) | 없음 | 잠금 인프라(PWD_*) 정합 Q9. 타 디바이스 세션 Q10 |
| **PRAFTA-APP-010-05** | backend | app.mypage.mypage01 | 신규 | **앱 전용 신규** 결재선 프리셋 CRUD + 결재자 후보: `GET /approval-presets`(목록), `/{presetId}`(상세), `POST`(저장), `/set-default`, `/delete`, `GET /approval-candidates`. user04 SQL **참고만**, 앱 매퍼/서비스/DTO 전부 신규 | 근태 §9.5(자기결재 차단), 재기획서 결재선 | 없음 | **D2**: web user04 호출·의존 금지. 테이블만 공유. 자기결재·중복·소유자 강제 앱 service에 재구현 |
| **PRAFTA-APP-010-06** | (BE 신규 없음) | — | 재사용 | 로그아웃 = 기존 `POST /comApi/login/logout`(refreshToken) 재사용 | 공통 §3.4, §9(선점) | 없음 | **D3**: 앱 신규 불필요. FE만 호출 연결 |
| **PRAFTA-APP-010-07** | backend | app.auth.auth01 | 신규 | `POST /appApi/auth/withdraw` — 트랜잭션: ①프리셋 hard delete(tb_aprv_line_preset/_d) ②tb_user: ACCOUNT_STATUS='03'·WITHDRAWAL_DATE·USER_NM 마스킹(`김○○`)·MBL/EMAIL/BIRTH ENC 등 PII 무효화·USER_PW 무효화 ③tb_del_user INSERT(USER_NM 마스킹) ④토큰폐기·선점해제. **연차 자동취소·결재자 알림 제외(D5)** | 공통 §3.5(탈퇴), §11.3(상태변경 감사대상), 근태 §9.5 | 05 | C11~C13. 마스킹 규칙 Q11. 출퇴근/근태/안전/TBM 기록 보존(근로기준법 3년) |

### 프론트 화면 (frontend-screen) — 골격 작성 완료, developer는 script만

| ID | 유형 | 작업유형 | 산출물(골격) | 연결 UI | 호출 API | 선행 |
|---|---|---|---|---|---|---|
| **PRAFTA-APP-010-10** | frontend-screen | 신규 | `views/mypage/MyPageView.vue` (메인: 프로필카드+메뉴2그룹+로그아웃버튼+탈퇴링크+버전) | UI-A010 | 010-01, 010-06(comApi logout), 010-07 | 010-01,07 |
| **PRAFTA-APP-010-11** | frontend-screen | 신규 | `views/mypage/ProfileEditView.vue` (개인정보 수정 3그룹) | UI-A011 | 010-01b(복호화 프리필),02,03 | 010-01b,02,03 |
| **PRAFTA-APP-010-12** | frontend-screen | 신규 | `views/mypage/PasswordChangeView.vue` (비번 변경 3필드+규칙가이드) | UI-A012 | 010-04 | 010-04 |
| **PRAFTA-APP-010-13** | frontend-screen | 신규 | `views/mypage/ApprovalPresetListView.vue` (프리셋 카드 리스트+추가) | UI-A013 | 010-05 | 010-05 |
| **PRAFTA-APP-010-14** | frontend-screen | 신규 | `views/mypage/ApprovalPresetEditView.vue` (이름+기본토글+결재자리스트+삭제) | UI-A014 | 010-05 | 010-05 |

### 프론트 컴포넌트 (frontend-component) — 골격 작성 완료

| ID | 유형 | 산출물(골격) | 호출자 | 비고 |
|---|---|---|---|---|
| **PRAFTA-APP-010-20** | frontend-component | `views/mypage/components/MobileVerificationField.vue` | ProfileEditView | 발송/검증은 **앱 전용** `/appApi/mypage/mobile/*`(D4). PhoneAuthView 인터랙션 재사용 |
| **PRAFTA-APP-010-21** | frontend-component | `views/mypage/components/PasswordRuleGuide.vue` | PasswordChangeView | UI 토글만(규칙 충족 표시는 props) |
| **PRAFTA-APP-010-22** | frontend-component | `views/mypage/components/PresetApproverPickerSheet.vue` | ApprovalPresetEditView | BaseBottomSheet 패턴. **앱 전용** `/appApi/mypage/approval-candidates`(D2) |
| **PRAFTA-APP-010-23** | frontend-component | `views/mypage/components/LogoutConfirmDialog.vue` | MyPageView | $confirm 대체 가능성 검토(Q12) |
| **PRAFTA-APP-010-24** | frontend-component | `views/mypage/components/WithdrawalConfirmDialog.vue` | MyPageView | 체크박스 체크 시에만 활성. 콜아웃은 D5 반영(연차 자동취소 문구 제거, "별도 정산 페이지 예정"은 노출 안 함) |
| **PRAFTA-APP-010-25** | frontend-screen(연동) | MainView.vue `onAvatarClick` → `/MyPage` 라우팅 + 라우터 정적 등록 | — | 기존 로그아웃 임시동작 제거→마이페이지로 이전 |

> 시안 §9의 `ProfileCard`/`ApprovalPresetCard`/`ApproverRow`는 본 분해에서 **부모 화면 골격에 인라인 통합**(과분해 방지). 결재자 추가 시트만 분리(010-22).

---

## 4. 백엔드 API 명세 (appApi 프리픽스, kebab, 실제 tb_user 컬럼 기준)

> ⚠️ **D1 적용**: 010-01(메인/조회)은 **마스킹 문자열**만 반환. 복호화 전체값은 010-01b(수정 진입 전용)에서만.

### 010-01 GET /appApi/mypage/profile  (마스킹 응답)
- 요청: 없음(JWT 클레임 gv_cmpnyCd/gv_userCd 기준)
- 응답:
```json
{
  "userId": "yjkim",
  "userNm": "김여진",
  "siteNm": "중곡사업장",
  "nodeNm": "2본부",
  "hireDate": "20250717",
  "mblNoMasked": "010-****-8295",
  "emailMasked": "t***@test.com",
  "genderCode": "200",
  "genderNm": "여성",
  "birthDateMasked": "1993-**-**",
  "lastLoginDtime": "2026-05-25T20:13:49",
  "presetCount": 3
}
```
- 처리: PII는 **복호화 후 마스킹**(또는 LAST4/DOMAIN 등 마스킹 메타로 조립). 평문을 응답에 싣지 않는다. genderCode/genderNm은 SYS004(100/200) + NULL이면 둘 다 null.

### 010-01b GET /appApi/mypage/profile/edit  (수정 진입 전용, 복호화 전체)
- 요청: 없음(JWT). **수정 화면 진입 시에만 호출.**
- 응답: 010-01 + 복호화 전체값(`mblNo`,`email`,`birthDate`). 응답 헤더 `Cache-Control: no-store`.
```json
{
  "userId":"yjkim","userNm":"김여진","siteNm":"중곡사업장","nodeNm":"2본부",
  "hireDate":"20250717",
  "mblNo":"010-6766-8295","email":"test@test.com",
  "genderCode":"200","birthDate":"19930916",
  "lastLoginDtime":"2026-05-25T20:13:49"
}
```
- ⚠️ webview 로그/캐시 평문 잔존 리스크(D1) → no-store + 프론트는 응답을 store 영속화 금지(폼 로컬 ref만).

### 010-02 PUT /appApi/mypage/profile
- 요청:
```json
{
  "userNm": "김여진",
  "genderCode": "200",
  "birthDate": "19930916",
  "email": "test@test.com",
  "mblNo": "010-6766-8295",
  "mobileVerificationToken": "vfy_..."
}
```
- 처리: 변경 필드만 UPDATE. 휴대폰 변경 시 verificationToken(010-03b) 검증 후 MBL_NO_ENC/MBL_NO_HMAC/MBL_NO_LAST4 동시 갱신. 이메일 EMAIL_ENC/EMAIL_HMAC/EMAIL_DOMAIN. 생년월일 BIRTH_DT_ENC. 성별 GENDER(SYS004 100/200, 빈값=NULL). 검증 실패 422.
- 검증: userNm 1~50자(trim 후 1자+), genderCode ∈ {100,200,null}, birthDate YYYYMMDD·미래불가(만14세 하한은 Q7), email RFC5322 또는 빈값, mblNo 형식.

### 010-03 휴대폰 변경 인증 (앱 전용, 2엔드포인트, D4)
- `POST /appApi/mypage/mobile/request-verification` body `{ "mblNo":"01012345678" }` → 6자리 발송(앱 전용; SMS 인프라/`tb_sms_auth_code` 재사용), 본인 외 사용중(HMAC) 시 422 `MOBILE_DUP`. 응답 `{ "expiresInSeconds":180 }`
- `POST /appApi/mypage/mobile/verify` body `{ "mblNo","verificationCode" }` → 성공 `{ "verified":true, "verificationToken":"vfy_..." }`(5분). 실패 422 `INVALID_CODE`/`EXPIRED`/`TOO_MANY_ATTEMPTS`(Q8)
- ⚠️ **로그인 토큰을 발급하지 않는다**(comApi verify-phone-auth와 다른 신규 앱 엔드포인트). comApi verify 직접 호출 금지.

### 010-04 PUT /appApi/mypage/password
- 요청 `{ "currentPassword","newPassword" }`
- 처리: USER_PW 현재비번 검증 → 규칙(8자/대소문자/숫자/특수) → 현재≠새 → USER_PW+PWD_CHG_DTIME UPDATE. 세션 유지(Q10).
- 에러 422: `INVALID_CURRENT_PASSWORD`/`PASSWORD_RULE_VIOLATION`/`SAME_AS_CURRENT`. 5회 초과 잠금은 PWD_* 인프라 정합(Q9).

### 010-05 결재선 프리셋 (앱 전용 신규, D2 — user04 SQL 참고만)
- `GET /appApi/mypage/approval-presets` → `{ "presets":[{ "presetId","presetNm","defaultYn":"Y"|"N","steps":[{ "stepNo":1,"approverUserCd","userNm","userId" }] }] }`
- `GET /appApi/mypage/approval-presets/{presetId}` → 단일 PresetItem
- `POST /appApi/mypage/approval-presets` body `{ "presetId":null, "presetNm","defaultYn":"Y"|"N","approverUserCds":["TEST02","KIMY01"] }` → `{ "presetId":"..." }`. presetId null=신규/존재=수정
- `POST /appApi/mypage/approval-presets/set-default` body `{ "presetId" }`
- `POST /appApi/mypage/approval-presets/delete` body `{ "presetId" }` (user04 deletePreset은 hard delete — 앱도 동일. 디테일 먼저 DELETE 후 마스터 DELETE)
- `GET /appApi/mypage/approval-candidates` → `{ "myRankSortIdx":N, "candidates":[{ "userCd","userNm","rankNm","nodeNm" }] }` (동일 회사·사업장·활성·본인/system 제외, 직급 SORT_IDX 정렬 — user04 SQL 참고)
- ⚠️ 자기결재 차단(근태 §9.5)·이름 중복(CMPNY_CD,USER_CD,PRESET_NM)·소유자=토큰 userCd 강제는 **앱 service에 신규 구현**. 비활성 결재자 배지는 응답 필드 부재로 1차 미노출.

### 010-06 로그아웃 (BE 신규 없음, D3)
- 기존 `POST /comApi/login/logout`(body refreshToken) 재사용. 토큰 폐기 + 선점 해제. FE만 연결.

### 010-07 POST /appApi/auth/withdraw  (D5 반영)
- 요청 `{ "confirmed":true }`
- 처리(단일 트랜잭션):
  ① 프리셋 hard delete(`tb_aprv_line_preset_d` 먼저 → `tb_aprv_line_preset`, 소유자=토큰 userCd)
  ② `tb_user` UPDATE: ACCOUNT_STATUS='03', WITHDRAWAL_DATE=오늘8자, USER_NM 마스킹(`김○○`), MBL_NO_ENC/EMAIL_ENC/BIRTH_DT_ENC/관련 HMAC/LAST4/DOMAIN/GENDER 무효화·마스킹, USER_PW 무효화
  ③ `tb_del_user` INSERT(CMPNY_CD, USER_ID, USER_NM(마스킹), INSERT_NO, INSERT_DATE)
  ④ 토큰폐기·선점해제
- **제외(D5)**: tb_user_attd_req 연차 자동취소, 결재자 알림. 퇴사 사용연차 정산은 별도 페이지.
- 보존(DELETE 안 함): 출퇴근/근태/안전/TBM 기록(USER_CD 유지, 근로기준법 3년).
- 응답 `{ "success":true }`.

---

## 5. 우선순위 / 착수 권고 (의존순서 포함)

1. **선결 완료**: P0 5건(D1~D6) 전건 사용자 확정 → BE 착수 차단 해제.
2. **법적 책임 영역 격상**: 010-07(탈퇴-근로기준법 보존/감사로그 §11.3) +1단계. PII 처리(010-01/01b/02/03) +1단계.
3. **백엔드 의존순서(권장 착수 순)**:
   - (1) **010-01 / 010-01b** — 프로필 조회(마스킹/복호화). 프로필 Result/Mapper/마스킹 유틸 기반 마련.
   - (2) **010-03** — 휴대폰 발송/검증(앱 전용). verificationToken 발급 메커니즘(단기 토큰/서버 플래그) 확정.
   - (3) **010-02** — 프로필 저장(01b·03 의존: 휴대폰 변경 시 토큰 검증).
   - (4) **010-04** — 비밀번호 변경(독립, 병렬 가능).
   - (5) **010-05** — 프리셋 CRUD + 후보(앱 전용 신규 모듈, 독립, 병렬 가능).
   - (6) **010-07** — 탈퇴(05 선행: 프리셋 삭제 매퍼 재사용).
   - 010-06은 BE 신규 없음(comApi 재사용).
4. **프론트 의존순서**: 골격(완료) → developer script.
   - 010-25(MainView 진입+라우트 등록) 먼저 → 010-10(메인) → 010-11/12/13/14 → 컴포넌트(20~24)는 각 부모 화면과 함께.
   - FE는 대응 BE 엔드포인트 완료 후 script 연결.
