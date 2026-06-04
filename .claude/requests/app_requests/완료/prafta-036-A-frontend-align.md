# prafta-036-A — 앱 로그인/가입 흐름 FE URL/메서드 정렬 (백엔드 무변경)

> 원본 요청서: `.claude/requests/prafta-036.md`
> 사용자 확정사항(2026-05-28) Q1 = **A안**: 가입 흐름 13개 호출만 FE에서 URL/메서드 정렬해 즉시 가용성 회복. 백엔드 변경 없음.
> 분해 마스터 메시지(채팅, 2026-05-28): "응답 키 보존(앱 FE 변경 최소화)"는 백엔드 측 자유도이며, **본 단계는 백엔드를 건드리지 않으므로 응답 키가 바뀐 곳은 FE에서 destructure 키를 백엔드 현재 응답에 맞춰 정렬한다.** (B-1/B-2 풀 리팩터에서는 응답 키 보존을 강제한다 — 본 A 단계는 이미 케밥/record 전환이 완료된 baseinfo/login 모듈의 신호를 따라간다.)

---

## 0. 작성 목적 / 성격

- 이 문서는 **개발 명령(작업지시서)** 이며 developer 에이전트가 실행한다.
- 본 단계는 **앱 프론트엔드 4개 파일만** 손댄다. 백엔드 소스/DDL/엔드포인트는 일절 변경 금지.
- "왜 깨졌는가": 백엔드 `BaseinfoController` / `LoginController`가 이미 케밥-케이스 URL + `@ModelAttribute` GET + record 응답 DTO로 리팩터되어 있다(웹 프론트는 정렬 완료). 앱 프론트만 옛 URL/메서드(`POST /comApi/baseinfo/getXxx`, snake/대문자 응답 키)를 호출하고 있어서 가입/로그인 흐름이 404 또는 destructure 실패로 동작하지 않는다.

---

## 1. 배경 — 정찰 결과 요약

### 1.1 백엔드 현재 시그니처 (이미 적용된 상태, 변경 금지)

| 백엔드 경로 | 메서드 | 요청 바인딩 | 요청 DTO 필드 | 응답 DTO 필드 (camelCase record) |
|---|---|---|---|---|
| `/baseinfo/syst-info-lists` | GET | `@ModelAttribute SystInfoListRequest` | `List<String> systCodeList` | `SystInfoListResponse { List<SystInfoResult> systInfoList }` / `SystInfoResult { systValCd, systValNm, sortIdx, systValDCd, systValDNm }` |
| `/baseinfo/terms-detail-infos` | GET | `@ModelAttribute TermsDetailInfoRequest` | `String termsId` | `TermsDetailInfoResponse { TermsDetailInfoResult termsDetailInfoResult }` / `TermsDetailInfoResult { termsId, termsVersion, requiredYn, termsContent, strDate, useYn, termsDesc, ... }` |
| `/baseinfo/cmpny-infos` | GET | `@ModelAttribute CmpnyInfoRequest` | `String cmpnyCd` | `CmpnyInfoResponse { CmpnyInfoResult cmpnyInfoResult }` / `CmpnyInfoResult { cmpnyCd, cmpnyNm, useYn }` |
| `/baseinfo/user-id-duple-checks` | GET | `@ModelAttribute UserIdDupleCheckRequest` | `cmpnyCd, userId` | `UserIdDupleCheckResponse { String uniqueYn }` |
| `/baseinfo/sms-auth-sends` | POST | `@RequestBody UserSmsAuthNoRequest` | `cmpnyCd, mblNo, dupChkYn` | (없음, 200 OK) |
| `/baseinfo/sms-auth-checks` | POST | `@RequestBody UserSmsAuthNoCheckRequest` | `cmpnyCd, mblNo, certNo` | (없음, 200 OK) |
| `/login/insert-user-info` | POST | `@Valid @RequestBody UserJoinRequest` | `cmpnyCd, userId, userPw, userNm, siteCd, mblNo, birthDt, nodeCd?, authCd?, email?, gender?, useYn?` | (없음, 200 OK) |
| `/baseinfo/site-lists` | GET | `@ModelAttribute SiteInfoRequest` | `cmpnyCd, siteNo, siteNm` | `SiteInfoResponse { List<SiteInfoResult> siteInfoResultList }` / `SiteInfoResult { siteCd, siteNo, siteNm, siteAdminCd, siteAdminNm, addr1, addr2, telNo }` |
| `/baseinfo/user-ids` | GET | `@ModelAttribute UserIdInfoRequest` | `userNm, mblNo` | `UserIdInfoResponse { UserIdInfoResult userIdInfoResult }` / `UserIdInfoResult { cmpnyCd, userCd, userId }` |
| `/baseinfo/update-user-password` | POST | `@RequestBody UserPasswordRequest` | `cmpnyCd, userCd, userPw` ⚠ **`userCd`** (userId 아님) | (없음, 200 OK) |

> ⚠ `update-user-password`는 백엔드가 `userCd`를 받는데 앱은 `userId`를 보내고 있다. `getUserIdInfo`(=`/baseinfo/user-ids`)가 `userCd`를 응답에 포함하므로 그것을 저장했다가 비밀번호 변경 시 사용해야 한다.

### 1.2 앱 프론트엔드 현재 호출 (변경 대상)

| 화면 | 위치 | 라인 (현재) | 현재 호출 |
|---|---|---|---|
| TermsInfo.vue | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/login/TermsInfo.vue` | L129 | `axios.get('/comApi/baseinfo/syst-info-list', { params: { systCodeList } })` |
| TermsInfo.vue | 동상 | L157 | `proxy.$alert(err.response.data.message)` — **옵셔널 체이닝 없음(NPE 위험)** |
| TermsDetail.vue | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/login/TermsDetail.vue` | L72 | `axios.post('/comApi/baseinfo/getTermsDInfo', { termsId })` |
| TermsDetail.vue | 동상 | L77 | `termsContent.value = response.data.TERMS_CONTENT` — **대문자 키 destructure** |
| TermsDetail.vue | 동상 | L80 | `alert(err.response.data.message)` — 옵셔널 체이닝 없음 |
| JoinUser.vue | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/login/JoinUser.vue` | L505 | `axios.get('/comApi/baseinfo/syst-info-list', { params: { systCodeList } })` |
| JoinUser.vue | 동상 | L537 | `axios.post('/comApi/baseinfo/getCmpnyInfo', { cmpnyCd })` |
| JoinUser.vue | 동상 | L541-543 | `response.data.USE_YN`, `response.data.CMPNY_NM` destructure |
| JoinUser.vue | 동상 | L563 | `axios.post('/comApi/baseinfo/getUserIdDupleChk', { cmpnyCd, userId })` |
| JoinUser.vue | 동상 | L568 | `response.data.UNIQUE_YN` destructure |
| JoinUser.vue | 동상 | L599 | `axios.post('/comApi/baseinfo/getSmsAuthReq', { mblNo, dupChkYn })` |
| JoinUser.vue | 동상 | L646 | `axios.post('/comApi/baseinfo/getSmsAuthChk', { mblNo, certNo })` |
| JoinUser.vue | 동상 | L674 | `axios.post('/comApi/login/insertUserInfo', { ... })` |
| JoinUser.vue | 동상 | L793 | `axios.post('/comApi/baseinfo/getSiteInfoList', { cmpnyCd, siteNm })` |
| JoinUser.vue | 동상 | L799 | `siteList.value = response.data` — **배열 가정**(현재 응답은 `{siteInfoResultList:[]}`) |
| ActInfoSrch.vue | `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/login/ActInfoSrch.vue` | L251 | `axios.post('/comApi/baseinfo/getUserIdInfo', { userNm, mblNo })` |
| ActInfoSrch.vue | 동상 | L256-257 | `response.data.USER_ID`, `response.data.CMPNY_CD` destructure |
| ActInfoSrch.vue | 동상 | L267 | `axios.post('/comApi/baseinfo/getSmsAuthReq', { mblNo, dupChkYn })` |
| ActInfoSrch.vue | 동상 | L317 | `axios.post('/comApi/baseinfo/getSmsAuthChk', { mblNo, certNo })` |
| ActInfoSrch.vue | 동상 | L339 | `axios.post('/comApi/baseinfo/updateUserPw', { cmpnyCd, userId, userPw })` |

> URL의 `/comApi` prefix는 axios baseURL(`/prafta`) 뒤에 붙는 가상 prefix로, 백엔드 `RestController` 매핑(`/baseinfo`, `/login`)이 실제 path. 즉 `/prafta/comApi/baseinfo/syst-info-lists` 같은 패턴으로 동작. **`/comApi` prefix 자체는 변경하지 않음**(다른 화면들도 동일 prefix 사용).

---

## 2. 변경 매핑표 (정렬 대상)

각 호출에 대해 "현재 → 변경 후"를 한 표로 정리.

| # | 화면 | 현재 호출 | 변경 후 (URL/메서드) | 요청 데이터 |
|---|---|---|---|---|
| 1 | TermsInfo | `GET /comApi/baseinfo/syst-info-list` | `GET /comApi/baseinfo/syst-info-lists` | `params: { systCodeList: ['SYS008'] }` (배열 그대로 — axios가 `systCodeList=SYS008` 형태로 쿼리스트링 직렬화) |
| 2 | TermsDetail | `POST /comApi/baseinfo/getTermsDInfo` (body) | `GET /comApi/baseinfo/terms-detail-infos` | `params: { termsId: termsId.value }` |
| 3 | JoinUser | `GET /comApi/baseinfo/syst-info-list` | `GET /comApi/baseinfo/syst-info-lists` | `params: { systCodeList: ['SYS004'] }` |
| 4 | JoinUser | `POST /comApi/baseinfo/getCmpnyInfo` (body) | `GET /comApi/baseinfo/cmpny-infos` | `params: { cmpnyCd: cmpnyCd.value }` |
| 5 | JoinUser | `POST /comApi/baseinfo/getUserIdDupleChk` (body) | `GET /comApi/baseinfo/user-id-duple-checks` | `params: { cmpnyCd, userId }` |
| 6 | JoinUser | `POST /comApi/baseinfo/getSmsAuthReq` (body) | `POST /comApi/baseinfo/sms-auth-sends` | body: `{ cmpnyCd: cmpnyCd.value, mblNo: mblNo.value, dupChkYn: 'Y' }` (body 유지, URL만 변경) |
| 7 | JoinUser | `POST /comApi/baseinfo/getSmsAuthChk` (body) | `POST /comApi/baseinfo/sms-auth-checks` | body: `{ cmpnyCd: cmpnyCd.value, mblNo, certNo }` |
| 8 | JoinUser | `POST /comApi/login/insertUserInfo` (body) | `POST /comApi/login/insert-user-info` | body: 기존 그대로 — `UserJoinRequest` 필드명과 100% 일치(이미 일치) |
| 9 | JoinUser | `POST /comApi/baseinfo/getSiteInfoList` (body) | `GET /comApi/baseinfo/site-lists` | `params: { cmpnyCd: cmpnyCd.value, siteNm: keyword }` (siteNo 빈문자열 또는 미전달) |
| 10 | ActInfoSrch | `POST /comApi/baseinfo/getUserIdInfo` (body) | `GET /comApi/baseinfo/user-ids` | `params: { userNm: userNm.value, mblNo: mblNo.value }` |
| 11 | ActInfoSrch | `POST /comApi/baseinfo/getSmsAuthReq` (body) | `POST /comApi/baseinfo/sms-auth-sends` | body: `{ cmpnyCd: cmpnyCd.value, mblNo, dupChkYn: 'N' }` |
| 12 | ActInfoSrch | `POST /comApi/baseinfo/getSmsAuthChk` (body) | `POST /comApi/baseinfo/sms-auth-checks` | body: `{ cmpnyCd: cmpnyCd.value, mblNo, certNo }` |
| 13 | ActInfoSrch | `POST /comApi/baseinfo/updateUserPw` (body) | `POST /comApi/baseinfo/update-user-password` | body: `{ cmpnyCd: cmpnyCd.value, userCd: userCd.value, userPw: userPwConfirm.value }` ⚠ **`userCd` 사용**(아래 §3 참조) |

### 2.1 POST→GET 전환 시 axios 사용법

기존:
```js
const response = await axios.post('/comApi/baseinfo/getCmpnyInfo', { cmpnyCd: cmpnyCd.value })
```
변경 후:
```js
const response = await axios.get('/comApi/baseinfo/cmpny-infos', {
  params: { cmpnyCd: cmpnyCd.value },
})
```
- axios의 `params:`는 쿼리스트링으로 직렬화된다. 서버 측 `@ModelAttribute`가 받는다.
- 배열 파라미터(`systCodeList: ['SYS008']`)는 axios 기본 직렬화로 `systCodeList[]=SYS008` 또는 `systCodeList=SYS008` 형태로 전송된다. Spring `@ModelAttribute`가 `List<String>`로 정상 매핑함(기존 웹 프론트에서도 동일 패턴).
- `sessionStorage`의 `gv_*` 값은 axios 요청 인터셉터(`src/api/axios.js` L101-117)가 method별로 자동 부착한다. GET이면 params에 머지, POST면 body에 머지. **별도 추가 작업 불필요**.

---

## 3. 응답 키 매핑 (destructure 변경)

응답이 record로 감싸진 경우 한 단계 더 들어가야 하며, 키도 camelCase로 통일되었다.

| # | 화면 | 현재 destructure | 변경 후 destructure |
|---|---|---|---|
| 1 | TermsInfo | `response.data.systInfoList` (이미 camelCase, 변경 없음 — 키 확정) | `response.data.systInfoList` 유지 |
| 2 | TermsDetail | `response.data.TERMS_CONTENT` | `response.data.termsDetailInfoResult.termsContent` |
| 3 | JoinUser (syst) | `response.data.systInfoList` | `response.data.systInfoList` 유지 |
| 4 | JoinUser (cmpny) | `response.data.USE_YN`, `response.data.CMPNY_NM` | `response.data.cmpnyInfoResult.useYn`, `response.data.cmpnyInfoResult.cmpnyNm` |
| | | (`USE_YN`은 'Y'/'N' 문자열 → `!response.data.USE_YN` 같은 truthy 평가는 부정확. 정렬 후 `response.data.cmpnyInfoResult.useYn !== 'Y'`로 명시 비교) | |
| 5 | JoinUser (dup) | `response.data.UNIQUE_YN == 'N'` | `response.data.uniqueYn === 'N'` |
| 6-7 | JoinUser (sms) | 응답 없음(200 OK 본문 빈값) | 동일 |
| 8 | JoinUser (join) | 응답 없음 | 동일 |
| 9 | JoinUser (site) | `response.data` 를 배열로 가정 | `response.data.siteInfoResultList` (배열). 키 매핑: 기존 `SITE_ID/SITE_NM` → 신규 `siteCd/siteNm`. `SidePanel`의 `keyField="SITE_ID"`/`labelField="SITE_NM"`를 `siteCd`/`siteNm`으로 변경. `selectSites` 콜백 내 `selected.SITE_CD` → `selected.siteCd`, `selected.SITE_NO` → `selected.siteNo`, `selected.SITE_NM` → `selected.siteNm` 로 변경 |
| 10 | ActInfoSrch (idInfo) | `response.data.USER_ID`, `response.data.CMPNY_CD` | `response.data.userIdInfoResult.userId`, `response.data.userIdInfoResult.cmpnyCd`. **추가로 `userCd` 도 저장(#13에서 사용)**: `userCd.value = response.data.userIdInfoResult.userCd` |
| 11-12 | ActInfoSrch (sms) | 응답 없음 | 동일 |
| 13 | ActInfoSrch (updPw) | 응답 없음 | 동일. **body 필드명 `userId` → `userCd` 변경 필수** |

### 3.1 `userId` vs `userCd` 정리 (#13)

- 백엔드 `UserPasswordRequest`는 `cmpnyCd, userCd, userPw` 세 필드만 받는다.
- 앱 ActInfoSrch.vue 는 `userId.value`만 가지고 있으며 `userCd`는 별도 변수가 없다.
- 해법: `fnUserIdSrch`(#10) 응답에서 `userCd`를 추출해 새 ref `userCd = ref('')` 에 저장 → `fnUserPwUpdate`(#13)에서 body의 `userCd`로 사용.
- `userId.value`는 화면 표시(아이디 input)에 그대로 유지. 라우터 query에도 `userId`로 계속 전달 가능(로그인 페이지 입력 보조용).

---

## 4. 에러 핸들러 안전 접근 통일

현재 일부 호출은 `err.response.data.message` 로 옵셔널 체이닝 없이 접근 → 404/500/네트워크 끊김 시 `Cannot read properties of undefined` JS 에러가 surface된다.

- 모든 catch 블록의 `err.response.data.message` 패턴을 `err.response?.data?.message || '<폴백 메시지>'` 로 통일.
- 적용 위치(최소):
  - TermsInfo.vue L157
  - TermsDetail.vue L80 (`alert(err.response.data.message)` → `alert(err.response?.data?.message || '약관 정보를 불러올 수 없습니다.')`)
  - JoinUser.vue L526, L546, L581, L662 (이미 일부는 `?.` 사용 — 일관성 확인)
  - ActInfoSrch.vue L261, L331, L358
- 메시지 폴백 문구 예:
  - 약관 조회 실패: "약관 정보를 불러올 수 없습니다."
  - 회사코드 조회 실패: "회사코드를 확인할 수 없습니다."
  - 아이디 중복체크 실패: "아이디 중복확인 중 오류가 발생했습니다."
  - 사용자 ID 조회 실패: "계정 정보를 조회할 수 없습니다."

---

## 5. 단계별 작업 항목 (developer 착수 단위)

순서대로 진행. 각 항목 끝에 앱 빌드 1회 + 사용자 위임 회귀 테스트 1회.

| # | 항목 | 대상 파일 | 변경 |
|---|---|---|---|
| 1 | TermsInfo URL 정렬 | `views/login/TermsInfo.vue` | §2 #1, §3 #1, §4 |
| 2 | TermsDetail URL/메서드/응답 정렬 | `views/login/TermsDetail.vue` | §2 #2, §3 #2, §4. POST→GET 전환, `response.data.termsDetailInfoResult.termsContent` |
| 3 | JoinUser URL/메서드/응답 정렬 (syst) | `views/login/JoinUser.vue` | §2 #3, §3 #3 |
| 4 | JoinUser 회사코드 조회 정렬 | 동상 | §2 #4, §3 #4. `useYn` 비교 로직 변경(`!= 'Y'` 비활성) |
| 5 | JoinUser 아이디 중복 정렬 | 동상 | §2 #5, §3 #5 |
| 6 | JoinUser SMS 발송/확인 정렬 | 동상 | §2 #6,#7. URL만 변경. 본문에 `cmpnyCd` 누락 시 추가(`cmpnyCd: cmpnyCd.value`) |
| 7 | JoinUser 가입 정렬 | 동상 | §2 #8. URL만 변경. body는 이미 일치 |
| 8 | JoinUser 사업장 목록 정렬 | 동상 | §2 #9, §3 #9. POST→GET, 응답 키 `siteInfoResultList`로 destructure. SidePanel `keyField`/`labelField` 키 교체 + `selectSites` 콜백 키 교체 |
| 9 | ActInfoSrch userId 조회 정렬 | `views/login/ActInfoSrch.vue` | §2 #10, §3 #10. **`userCd` ref 신설** + 응답에서 저장 |
| 10 | ActInfoSrch SMS 정렬 | 동상 | §2 #11,#12. URL만 변경. `cmpnyCd` 본문 추가 |
| 11 | ActInfoSrch 비번 재설정 정렬 | 동상 | §2 #13. URL 변경 + body의 `userId` → `userCd` 변경 |
| 12 | 에러 핸들러 안전화 | 4파일 전부 | §4 |

---

## 6. 검증 (개발자 자체 점검)

본 단계는 백엔드 변경이 없으므로 백엔드 재기동 불필요. 앱 빌드만으로 가용성 회복.

- [ ] `cd PRAFTA/prafta-app-frontend/prafta-app-frontend && npm run build` (또는 dev 모드) 성공
- [ ] 브라우저 콘솔에서 각 화면 진입 시 네트워크 탭에 새 URL로 요청 가는지 확인
- [ ] 404/500 발생 없음, 응답 구조 destructure 정상
- [ ] 회사코드 → 사용자ID 중복확인 → SMS 인증 → 가입 → 로그인 흐름 정상
- [ ] 약관 보기(TermsDetail) HTML 본문 정상 표시
- [ ] 사업장 찾기 SidePanel 목록 표시 + 선택 시 사업장명 input 반영
- [ ] 계정찾기(ActInfoSrch) 흐름 — 아이디 조회 → SMS 발송 → 인증 → 비밀번호 재설정 정상
- [ ] 에러 시 alert 메시지 표시(JS undefined 에러 없음)

> ⚠ **Flutter 재빌드는 사용자 위임**. Vue dev/web build 검증까지만 Claude가 수행. 위임 신호: "FE 빌드 OK / Flutter 재빌드 부탁드립니다".

---

## 7. 위험 / 주의

| 항목 | 내용 |
|---|---|
| **응답 키 변화** | §3 destructure 누락은 화면 빈값/이상동작으로 surface. 각 화면마다 직접 console.log로 응답 확인 권장 |
| **`userId` vs `userCd` 혼동** | 비밀번호 재설정 #13에서 누락 시 백엔드는 `userCd=null`로 받아 비번 업데이트 실패. **#9 화면 작업 시 반드시 `userCd` ref 신설** |
| **SidePanel keyField/labelField** | `SITE_ID` → `siteCd`로 바꾸면 SidePanel 내부 동작에 영향. 동일 컴포넌트를 다른 화면에서 다른 키로 호출하는지 grep 확인(`grep -r "keyField=\"SITE_ID\""`) |
| **SMS body에 `cmpnyCd` 누락 영향** | 백엔드 `UserSmsAuthNoRequest`는 `cmpnyCd`를 받지만 `@NotBlank`가 아니다(현재 DTO 확인). 누락 시 서비스 로직 분기에 영향 가능성 → 안전하게 `cmpnyCd: cmpnyCd.value` (없으면 빈 문자열) 항상 전송 |
| **git 브랜치 분리** | 권장: `fix/prafta-036-a-app-login-align`. 본 변경은 앱 FE에 국한되지만 4파일 동시 수정이므로 PR 단위로 묶기 |
| **백엔드 변경 시도 금지** | developer는 본 단계에서 백엔드 어떤 파일도 수정하지 않는다. 시그니처 불일치가 발견되면 즉시 사용자 보고(추측 수정 금지) |

---

## 8. 비즈니스 정책서 출처

- 회원가입 필수/선택 입력, 약관 동의 → `.claude/context/policies/common/03-account-auth.md` §3.1
- 계정 찾기 / 비밀번호 재설정 → `.claude/context/policies/common/03-account-auth.md` §3.3
- 약관 종류 / 관리 방식 → `.claude/context/policies/common/12-terms.md`
- 사업장 구성 요소 → `.claude/context/policies/common/06-site-management.md` §6.1
- PII / 휴대폰 / 위치정보 처리 → `.claude/context/policies/common/11-security-privacy.md` §11.1

> 본 단계는 **정책 변경이 아닌 URL/메서드 정렬**이므로 정책서 본문에 직접 영향을 주지 않는다. developer는 흐름 변경 없는 단순 정렬임을 확인하기 위해서만 정독한다.

---

## 9. 산출물

- 수정 파일 4종 (소스 변경):
  - `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/login/TermsInfo.vue`
  - `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/login/TermsDetail.vue`
  - `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/login/JoinUser.vue`
  - `PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/login/ActInfoSrch.vue`
- 신규 파일 없음
- DDL/마이그레이션 없음
- 백엔드 변경 없음

---

## 10. 후속 단계

본 A 단계 완료 후:
- prafta-036-B1 (chkLst01 풀 리팩터) → prafta-036-B2 (risk01 풀 리팩터) 순서로 진행
- 메모리 `project_prafta_app_vite_and_api_align`의 "남은 화면 엔드포인트 ~12개 follow-up" 중 일부가 본 A 단계에서 해소됨. 잔여 화면 정렬은 별도 요청서로 분해

**최종 업데이트**: 2026-05-28 — prafta-036 Q1(A안) 분해 결과.
