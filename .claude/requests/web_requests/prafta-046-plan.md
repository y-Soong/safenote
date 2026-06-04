# prafta-046 분해 plan — 노드–관리자 정합성 가드 ("관리자 없는 노드에 근로자 배정 차단")

> 작업 ID prefix: `PRAFTA-046`
> 작성: planner (2026-06-04)
> SSOT 요청서: `.claude/requests/web_requests/prafta-046.md`
> 정책서 출처: `common/08-permissions.md` §8.4 (조직 스코프 — 노드 담당 정/부 관리자), §8.4.1 (관리 권한은 담당 정/부 지정자만)
> 작업 영역: `PRAFTA/prafta-backend` (web/user, common) + `prafta-web-frontend` (노드 선택 — 이미 구현됨, 검증/follow-up 위주)
> 선행 관계: 본 작업이 prafta-app-009 `'Y'`(자체근태승인) 케이스의 선행 불변식.

---

## 0. 분해 요약 (TL;DR)

근로자가 노드 소속을 신규 획득하는 3경로(A-1 관리자 생성 / A-2 회원가입 / A-3 노드 이동)에 "대상 노드에 정 또는 부 관리자가 존재하는가" 가드를 추가한다. 검증 헬퍼는 신규 mapper `selectNodeHasAdmin` 1개로 통일하고, 에러코드는 BE `USER_400_056` / 회원가입 `LOGIN_400_015` 신규 2개를 추가한다.

핵심 실측 결론(아래 §1 상세):
- A-1 단건/엑셀(동기)/엑셀(비동기) 3경로는 모두 `User01ServiceImpl.insertUserOne` 단일 메서드를 통과한다 → 가드 1곳만 추가하면 3경로 전부 커버. 엑셀 행 단위 에러 수집은 기존 패턴이 `ApiException`을 그대로 잡아 처리(Q4 해소).
- FE(D)는 사실상 이미 구현되어 있다. 모든 노드 선택(회원가입·사용자 생성·사용자 수정·인라인 편집)은 공용 `SiteNodeSearchPop.vue` → `selectSiteNodeList` mapper를 호출하는데, 이 mapper는 **이미** `(NULLIF(MAIN_ADMIN_CD,'') IS NOT NULL OR NULLIF(SUB_ADMIN_CD,'') IS NOT NULL)` 필터를 적용한다(351줄). 따라서 신규 화면/골격은 불필요하고, FE는 QA 검증 + (선택) follow-up만 남는다.
- 마이그레이션 0건(스키마 변경 없음, 백필 없음). 에러코드는 enum 정의라 SQL 무관.

작업 수: 백엔드 4 (PRAFTA-046-1 ~ 046-4) + 검증/FE follow-up 1 (PRAFTA-046-5).

---

## 1. 실측 결과 (코드/스키마)

### 1.1 가드 적용 3경로 — 진입점 단일화 확인

| 경로 | 요청서 식별 | 실측 진입점 | 비고 |
|---|---|---|---|
| A-1 단건 생성 | `createUser` 611줄 직후 | `User01ServiceImpl.insertUserOne(UserCreateParam)` — `User01ServiceImpl.java:554`, 부서 존재검증 `selectSiteNodeExists` 호출은 **611줄** | 컨트롤러 `User01Controller.insertUserOne`(204줄) → `insertUserOne` |
| A-1 엑셀(동기) | 엑셀 일괄 | `User01BatchServiceImpl.uploadUserCreates`(173줄) → `insertUserBatch`(113줄) → **행별 `insertUserOne` 호출**(133줄) | 컨트롤러 `/upload-user-creates`(236줄) |
| A-1 엑셀(비동기) | 엑셀 일괄 | `UploadJobAsyncRunnerImpl.runAsync`(41줄) → **행별 `insertUserOne` 호출**(57줄) | 컨트롤러 `/upload-user-creates-async`(246줄) |
| A-2 회원가입 | `login.insertUserInfo` | `LoginServiceImpl.insertUserInfo(UserJoinParam)` — `LoginServiceImpl.java:195`, `loginMapper.insertUserInfo(userJoinCommand)` 228줄 | 컨트롤러 `LoginController.insertUserInfo`(75줄) `@NoAuth /login/insert-user-info`. 입력 `nodeCd`/`siteCd`는 `UserJoinParam` → `UserJoinCommand`(13~14줄) |
| A-3 노드 이동 | `updateOneUserInfo` 183줄 | `User01ServiceImpl.updateOneUserInfo(UserInfoModel)` — `User01ServiceImpl.java:181`, 노드변경 분기 182~196줄 | `UserInfoModel`에 `nodeCd`(14줄)/`oriNodeCd`(15줄) 보유 |

→ **A-1의 3경로(단건/엑셀동기/엑셀비동기)는 전부 `insertUserOne`을 통과**하므로, 가드를 `insertUserOne` 내부(8단계 `selectSiteNodeExists` 직후, 611~614줄 사이)에 1곳만 추가하면 3경로 모두 막힌다. 엑셀 경로(`insertUserBatch` 130줄 / `runAsync` 51줄)는 행별 `insertUserOne`을 try-catch로 감싸 `ApiException`의 errorCode/메시지를 `UserUpdateFailItem`으로 수집하므로(Q4), 신규 가드가 던지는 `USER_400_056`도 자동으로 "그 행만 실패, 나머지 진행"으로 처리된다 — 추가 작업 불필요.

### 1.2 검증 헬퍼 — 동일 조건문이 이미 존재 (Q1 근거)

- `BaseinfoMapper.xml` `selectSiteNodeList`(292줄)는 노드 목록 조회 시 **이미** 다음 필터를 건다(351줄):
  `AND (NULLIF(A.MAIN_ADMIN_CD, '') IS NOT NULL OR NULLIF(A.SUB_ADMIN_CD, '') IS NOT NULL)`
  → 요청서 §1.(B)가 제안한 검증 조건과 **글자 그대로 동일**. 즉 조직이 노드 목록을 노출할 때부터 관리자 미지정 노드를 숨기는 일관 정책이 이미 적용 중이다.
- 단, `selectSiteNodeExists`(User01Mapper.xml 634줄)는 노드 **존재만** COUNT — 관리자 유무는 보지 않는다. 가드는 별도 판정이 필요.

### 1.3 특수 노드 `'*'` (Q3 근거)

- `'*'` NODE_CD는 **`tb_attd_close`(근태 월마감)의 sentinel 전용**이다(AttdCloseMapper 주석 16줄: "전체 사업장 마감은 NODE_CD='*'"). `tb_site_node`의 실제 노드나 `tb_user.NODE_CD`(사용자 소속)로 `'*'`가 쓰이는 경로는 코드 전수 grep 결과 없음.
- 가드는 `selectNodeHasAdmin`이 `tb_site_node`에서 `(CMPNY_CD, SITE_CD, NODE_CD)` 정확 매칭으로 관리자 유무를 판정하므로, `'*'` 같은 미존재 노드가 들어오면 매칭 0건 → 자연히 차단(fail-closed). **별도 예외 분기 불필요.**

### 1.4 스키마 (MCP/snapshot 실측)

- `tb_site_node` (schema-full.sql 704~720줄): PK `(CMPNY_CD, SITE_CD, NODE_CD)`. `MAIN_ADMIN_CD varchar(50) DEFAULT NULL`, `SUB_ADMIN_CD varchar(50) DEFAULT NULL` — 둘 다 NULL 허용 확인. NOT NULL 화하지 않음(빈 노드 생성 허용, 요청서 §0.2-1·§2).
- 백필 0건: 요청서 §2가 "관리자 0 + 근로자 ≥1 노드 0건(2026-06-04 확인)"이라 명시. 본 분해는 그 사실을 재인용(planner는 운영 쓰기 불가).

### 1.5 에러코드 신규 채번

- `UserErrorCode`(UserErrorCode.java) 현재 최대 4xx 시퀀스: `USER_400_055`(64줄). 다음 빈 자리 = **`USER_400_056`**. (040~055 점유, 030~032·010~013 별도 블록 점유 확인.)
- `LoginErrorCode`(LoginErrorCode.java) 현재 최대 = `LOGIN_400_014`(20줄). 다음 = **`LOGIN_400_015`**.

### 1.6 FE 실측 — 이미 구현됨 (Q5 근거)

| FE 진입 | 노드 선택 컴포넌트 | 데이터 소스 | 필터 적용 |
|---|---|---|---|
| 회원가입 `JoinUserPop.vue`(738줄 `fnSiteNodeSearchPopOpen`) | `SiteNodeSearchPop.vue` | `/comApi/baseinfo/site-node-lists`(253줄) → `selectSiteNodeList` | ✅ 이미 필터(351줄) |
| 사용자 생성/수정 `UserInfoPop.vue`(1119줄) | `SiteNodeSearchPop.vue` | 동상 | ✅ |
| 사용자 관리 인라인 `User_01.vue`(995/1022줄) | `SiteNodeSearchPop.vue` | 동상 | ✅ |

- `SiteNodeSearchPop.vue`는 이미 "담당자가 정해진 부서만 조회됩니다."(65줄) 빨간 안내문을 노출 중. UX 사전차단이 이미 의도·구현되어 있다.
- 회원가입 비로그인 변형: 백엔드는 `@NoAuth /join-site-node-lists`(BaseinfoController 205줄)를 제공하며 내부적으로 동일 `selectSiteNodeList`를 호출(BaseinfoServiceImpl 304줄) → 회원가입도 필터 적용됨.

→ 결론: **신규 FE 화면/골격 불필요.** 화면 명세(UI-xxx) 등록 대상 아님. FE는 (a) QA가 위 경로의 필터 작동을 회귀 검증, (b) follow-up 1건(아래 §3-FU)만 남는다.

---

## 2. 작업 분해표

### PRAFTA-046-1
- 유형: backend
- 영역: web
- 모듈: web/user/user01 (+ common 검증 헬퍼)
- 작업 유형: 신규 (검증 헬퍼 mapper)
- 요구사항 요약: 노드에 정/부 관리자 존재 여부를 1/0으로 판정하는 신규 mapper `selectNodeHasAdmin` 추가.
- 상세 설명:
  - 정책서 출처: `common/08-permissions.md` §8.4.1 (관리 권한은 담당 정/부 지정자만 → "관리자 0 노드"는 관리·승인 주체 부재 상태).
  - 핵심 요구사항:
    1) 신규 mapper `selectNodeHasAdmin(cmpnyCd, siteCd, nodeCd)` → `tb_site_node` 정확 매칭 후 `(NULLIF(TRIM(MAIN_ADMIN_CD),'') IS NOT NULL OR NULLIF(TRIM(SUB_ADMIN_CD),'') IS NOT NULL)` 이면 1, 아니면(노드 미존재 포함) 0.
    2) Q1 결정: **신규 mapper 신설** (기존 `selectSiteNodeExists` 확장 통합 안 함). 근거 — `selectSiteNodeExists`는 A-1 8단계에서 "노드 존재"라는 별개 의미로 이미 쓰이고(부재 시 `USER_400_044` "부서코드없음"), 관리자 유무와 의미·에러코드가 다르다. 통합하면 호출처 의미가 흐려지고 기존 "부서코드없음" 진단이 깨진다. 별도 메서드가 단일 책임·가독성 우위.
    3) A-3(노드 이동)에서도 같은 헬퍼를 재사용한다.
  - 영향 받는 파일:
    - (BE) `prafta-backend/src/main/java/com/prafta/web/user/user01/mapper/User01Mapper.java` — `int selectNodeHasAdmin(@Param... )` 시그니처 추가
    - (BE) `prafta-backend/src/main/resources/com/prafta/web/user/user01/mapper/User01Mapper.xml` — `selectNodeHasAdmin` `<select resultType="int">` 추가 (선행 `selectSiteNodeExists` 635줄 패턴 차용, leading comma·`#{}` 바인딩 규칙 준수)
  - 영향 받는 endpoint: 없음 (내부 헬퍼)
  - 예상 산출물: mapper interface 1메서드 + xml 1쿼리
- 선행 작업: 없음
- 우선순위 근거: A-1/A-3 가드의 공통 의존. 데이터 정합성 불변식의 기반. 법적 책임 영역 아님이나 권한 정합 핵심.

### PRAFTA-046-2
- 유형: backend
- 영역: web
- 모듈: web/user/user01
- 작업 유형: 보완 (가드 추가)
- 요구사항 요약: A-1 — 관리자 단건/엑셀(동기·비동기) 사용자 생성에서 대상 노드 관리자 존재 검증 후 부재 시 차단.
- 상세 설명:
  - 정책서 출처: `common/08-permissions.md` §8.4.1.
  - 핵심 요구사항:
    1) `User01ServiceImpl.insertUserOne`(554줄) 8단계 부서 존재검증(`selectSiteNodeExists`, 611~614줄) **직후**에 `selectNodeHasAdmin` 호출 가드 추가. 0이면 `throw new ApiException(UserErrorCode.USER_400_056)`.
    2) 단건/엑셀(동기 `insertUserBatch`)/엑셀(비동기 `runAsync`) 3경로 모두 `insertUserOne`을 통과하므로 이 1곳 추가로 전부 커버(§1.1 실측). 엑셀 경로는 행별 try-catch가 `ApiException`을 `UserUpdateFailItem`으로 수집 → 신규 코드도 "해당 행만 실패, 나머지 진행"으로 동작(Q4: 전체 롤백 아님, 기존 user01 엑셀 패턴 유지).
    3) 신규 에러코드 `USER_400_056` 추가 — 메시지(엑셀 BatchResultPop 표시용, D6 한글 사유 일관): 예 "관리자미지정부서" 또는 친화형 "관리자가 지정되지 않은 부서에는 사용자를 배정할 수 없습니다.\n먼저 부서에 담당 정/부 관리자를 임명해 주세요." — developer가 기존 040~055 메시지 톤(짧은 사유 라벨)과 일관되게 결정. BatchResultPop 표시 폭 고려 시 짧은 라벨 권장.
  - 영향 받는 파일:
    - (BE) `prafta-backend/src/main/java/com/prafta/web/user/user01/service/impl/User01ServiceImpl.java` (insertUserOne, 611줄 직후)
    - (BE) `prafta-backend/src/main/java/com/prafta/common/error/user/UserErrorCode.java` (`USER_400_056` 추가)
  - 영향 받는 endpoint: `POST /webApi/user01/insert-user-one`, `POST /webApi/user01/upload-user-creates`, `POST /webApi/user01/upload-user-creates-async` (모두 간접)
  - 예상 산출물: service 가드 1블록 + error enum 1값
- 선행 작업: PRAFTA-046-1
- 우선순위 근거: "넣는 쪽" 3경로 중 최다 유입 경로. 데이터 정합성 직접 영향.

### PRAFTA-046-3
- 유형: backend
- 영역: web (common/login)
- 모듈: common/cmm/login
- 작업 유형: 보완 (가드 추가)
- 요구사항 요약: A-2 — 회원가입(self-signup)에서 가입 요청 노드의 관리자 존재 검증 후 부재 시 가입 거부.
- 상세 설명:
  - 정책서 출처: `common/08-permissions.md` §8.4.1; `common/03-account-auth.md` §3.1 (회원가입 입력) 참조.
  - 핵심 요구사항:
    1) `LoginServiceImpl.insertUserInfo`(195줄)에서 `loginMapper.insertUserInfo` 호출(228줄) **전에** 대상 노드 관리자 존재 검증. `UserJoinParam`은 `cmpnyCd`/`siteCd`/`nodeCd` 보유(`UserJoinCommand` 13~14줄로 전달됨).
    2) 검증 헬퍼는 PRAFTA-046-1의 `User01Mapper.selectNodeHasAdmin`을 재사용하거나, login 패키지 의존을 피하려면 `LoginMapper`에 동형 `selectNodeHasAdmin` 1개를 신설(공통 SQL). **Q1 보강 결정**: login 패키지가 web/user mapper에 의존하는 것은 계층 위반이므로 `LoginMapper`에 동형 쿼리를 1개 더 두는 것을 권장(중복 SQL 1줄, 패키지 경계 보존). developer가 기존 login↔user 의존 관례를 확인 후 최종 결정.
    3) 부재 시 신규 `LoginErrorCode.LOGIN_400_015` (가입자 친화 메시지, 회사/내부구조 노출 차단): 예 "선택하신 부서는 현재 가입할 수 없습니다.\n관리자에게 문의해 주세요."
  - 영향 받는 파일:
    - (BE) `prafta-backend/src/main/java/com/prafta/common/cmm/login/service/impl/LoginServiceImpl.java` (insertUserInfo, 228줄 전)
    - (BE) `prafta-backend/src/main/java/com/prafta/common/cmm/login/mapper/LoginMapper.java` + `.../login/mapper/LoginMapper.xml` (동형 `selectNodeHasAdmin` 신설 시)
    - (BE) `prafta-backend/src/main/java/com/prafta/common/error/login/LoginErrorCode.java` (`LOGIN_400_015` 추가)
  - 영향 받는 endpoint: `POST /comApi/login/insert-user-info` (`@NoAuth`)
  - 예상 산출물: service 가드 1블록 + (선택) mapper 1쿼리 + error enum 1값
- 선행 작업: PRAFTA-046-1 (헬퍼 재사용 시) — 단, login 패키지 독립 mapper 채택 시 병렬 가능
- 우선순위 근거: 비로그인 외부 유입 경로. fail-closed 필수. PII 미관여이나 권한 정합 핵심.

### PRAFTA-046-4
- 유형: backend
- 영역: web
- 모듈: web/user/user01
- 작업 유형: 보완 (가드 추가)
- 요구사항 요약: A-3 — 사용자 정보 수정 시 **들어가는(새) 노드**의 관리자 존재 검증. 기존 "떠나는 노드" 방어는 유지.
- 상세 설명:
  - 정책서 출처: `common/08-permissions.md` §8.4.1.
  - 핵심 요구사항:
    1) `User01ServiceImpl.updateOneUserInfo`(181줄)의 노드변경 분기(182~196줄)에서, **노드가 실제로 바뀌는 경우**(`nodeCd != null && oriNodeCd != null && !nodeCd.equals(oriNodeCd)`)에 한해 들어가는 노드 `selectNodeHasAdmin(cmpnyCd, siteCd, nodeCd)` 검증 추가. 0이면 `USER_400_056`(PRAFTA-046-2와 동일 코드 재사용).
    2) 기존 "떠나는 노드" 방어(`selectUserNodeAdminCheck`, 187줄 — 노드 관리자인 사용자의 이동·비활성 차단 `USER_400_001/002`)는 **변경 없이 유지**. 신규 가드는 그 분기와 독립적으로, 노드가 바뀌는 케이스에 한정해 들어가는 노드만 추가 판정.
    3) 비활성(`useYn='N'`) 케이스는 노드에서 빠지는 방향이므로 신규 가드 미적용(들어가는 노드 없음). 노드를 안 바꾸는 단순 정보 수정도 미적용.
    4) 엑셀 일괄 수정(`User01BatchServiceImpl.updateUserInfoBatch` 42줄 → 행별 `updateOneUserInfo` 59줄)도 동일 메서드를 통과하므로 자동 커버 — 행별 `ApiException` 수집 기존 패턴 유지.
  - 영향 받는 파일:
    - (BE) `prafta-backend/src/main/java/com/prafta/web/user/user01/service/impl/User01ServiceImpl.java` (updateOneUserInfo, 182~196줄 분기)
  - 영향 받는 endpoint: `POST /webApi/user01/update-one-user-info`, 일괄 수정 endpoint (간접)
  - 예상 산출물: service 가드 1블록 (에러코드는 046-2의 056 재사용)
- 선행 작업: PRAFTA-046-1, PRAFTA-046-2 (USER_400_056 정의)
- 우선순위 근거: "넣는 쪽" 노드 이동 경로. 데이터 정합성 직접 영향.

### PRAFTA-046-5
- 유형: frontend-screen (검증 + follow-up 위주, 신규 화면 없음)
- 영역: web
- 모듈: user / login / common(popup)
- 작업 유형: 검증 (+선택 보완)
- 요구사항 요약: FE 노드 선택 드롭다운의 관리자 미지정 노드 제외가 이미 작동함을 회귀 검증하고, 회원가입 비로그인 노드 조회 정합 follow-up을 점검.
- 상세 설명:
  - 정책서 출처: `common/08-permissions.md` §8.4.1; `common/13-ui-ux.md` §13.3 (사전 차단 인터랙션).
  - 핵심 요구사항:
    1) (검증) 회원가입/사용자생성/사용자수정/인라인편집 4경로가 모두 `SiteNodeSearchPop.vue` → `selectSiteNodeList`(이미 관리자 필터) 를 통과함을 확인. BE 가드(046-2/3/4)가 SSOT이고 FE 필터는 편의임을 명시.
    2) (follow-up, 선택) 회원가입 `JoinUserPop.vue`의 `SiteNodeSearchPop`은 `/comApi/baseinfo/site-node-lists`(인증 필수, Authorization required=true — BaseinfoController 179줄)를 호출한다. 비로그인 회원가입 단계에서 호출 시 401 위험이 있고, 백엔드는 `@NoAuth /join-site-node-lists`(205줄)를 별도 제공한다. 이 호출 정합(회원가입 팝업은 join-변형을 써야 함)은 본 가드 작업 범위 밖의 기존 FE 결함 가능성 → developer/QA가 재현 후 별도 티켓 여부 판단. **본 plan은 사실만 기록**(추측 수정 금지).
  - 영향 받는 파일:
    - (FE) `prafta-web-frontend/.../components/popup/SiteNodeSearchPop.vue` (변경 없음 — 검증 대상)
    - (FE) `prafta-web-frontend/.../components/popup/JoinUserPop.vue` (follow-up 후보, 본 작업서 미수정)
  - 영향 받는 endpoint: `GET /comApi/baseinfo/site-node-lists`, `GET /comApi/baseinfo/join-site-node-lists`
  - 예상 산출물: 없음(검증) / follow-up 티켓 제안
  - 연결 UI 명세: 없음 (신규 화면 아님 — 도메인 지식 베이스 등록 대상 아님)
- 선행 작업: PRAFTA-046-2, PRAFTA-046-3, PRAFTA-046-4
- 우선순위 근거: BE 가드 완료 후 회귀 확인. 화면 신규 없음 → 후순위.

---

## 3. §5 Q1~Q5 결정 (실측 근거)

| # | 결정 | 근거 |
|---|---|---|
| Q1 | 검증 헬퍼는 **신규 `selectNodeHasAdmin`** 신설(기존 `selectSiteNodeExists` 확장 통합 안 함). login 경로는 `LoginMapper`에 동형 쿼리 별도 1개 권장(패키지 경계). | `selectSiteNodeExists`는 "부서 존재"라는 별개 의미·에러코드(`USER_400_044`)로 이미 사용 중(User01ServiceImpl 611줄). 통합 시 의미 혼선. `selectSiteNodeList`(351줄)에 동일 필터문이 이미 있어 SQL 검증됨. |
| Q2 | A-2 진입점 = `LoginServiceImpl.insertUserInfo`(195줄), 컨트롤러 `@NoAuth POST /login/insert-user-info`(LoginController 75줄). `insertUserInfo` 228줄 직전이 가드 지점. | 코드 정독 완료. `UserJoinParam`→`UserJoinCommand`가 `nodeCd`/`siteCd` 운반(13~14줄). |
| Q3 | 특수 노드 `'*'` **가드 예외 불필요**. | `'*'`는 `tb_attd_close` 마감 sentinel 전용(AttdCloseMapper 16줄). `tb_site_node`/`tb_user.NODE_CD`엔 미사용(grep 0건). `selectNodeHasAdmin` 미존재 노드 → 0 반환 → fail-closed로 자연 차단. |
| Q4 | 엑셀 일괄 생성 일부 행만 관리자 미지정 노드일 때 = **해당 행만 실패 수집, 나머지 진행**(기존 user01 엑셀 패턴). | `insertUserBatch`(130줄)·`runAsync`(57줄)가 행별 `insertUserOne`을 try-catch로 감싸 `ApiException`→`UserUpdateFailItem` 수집. 신규 `USER_400_056`도 자동 동일 처리. 별도 코드 불필요. |
| Q5 | FE 노드 필터(D)는 **이미 구현됨** — 신규 화면/골격 불필요. 회원가입 화면 포함 4경로 모두 공용 `SiteNodeSearchPop` → `selectSiteNodeList`(관리자 필터 적용)로 커버. | `SiteNodeSearchPop.vue` 253줄 + `selectSiteNodeList` 351줄 + `selectJoinSiteNodeList`도 동일 mapper(BaseinfoServiceImpl 304줄). 안내문 "담당자가 정해진 부서만 조회됩니다."(65줄) 이미 노출. follow-up: 회원가입 팝업이 인증 엔드포인트를 호출하는 정합 이슈(PRAFTA-046-5). |

---

## 4. 마이그레이션 / 데이터

- **스키마 변경 없음**: `MAIN_ADMIN_CD`/`SUB_ADMIN_CD` NULL 허용 유지(빈 노드 생성 허용, 요청서 §2·§0.2-1).
- **백필 0건**: "관리자 0 + 근로자 ≥1" 노드 현재 0건(요청서 §2, 2026-06-04 확인). planner는 운영 쓰기 권한이 없어 재인용만 함 — developer/QA가 착수 시 MCP read-only로 재확인 권장:
  `SELECT N.CMPNY_CD, N.SITE_CD, N.NODE_CD FROM TB_SITE_NODE N WHERE (NULLIF(TRIM(N.MAIN_ADMIN_CD),'') IS NULL AND NULLIF(TRIM(N.SUB_ADMIN_CD),'') IS NULL) AND EXISTS (SELECT 1 FROM TB_USER U WHERE U.CMPNY_CD=N.CMPNY_CD AND U.SITE_CD=N.SITE_CD AND U.NODE_CD=N.NODE_CD) LIMIT 50;`
  → 0행이어야 가드 도입 후 기존 데이터 충돌 없음.
- **에러코드**: enum/메시지 정의(`UserErrorCode`/`LoginErrorCode`)이므로 코드성 테이블 미사용 → SQL 마이그 0건.

---

## 5. 의존성 / 후속

- **후행**: prafta-app-009 `'Y'`(자체근태승인 OR 승인) — 본 불변식 완성 후 master/hr 폴백 불필요(설정오류 최소 방어만 잔존).
- **대칭 기존 가드(변경 없음)**: `baim06.saveSiteNodeMainAdmin/SubAdmin/deleteSiteNodeAdmin`(빼는 쪽 — 근로자 잔존 시 관리자 제거 차단), `user01.updateOneUserInfo` `selectUserNodeAdminCheck`(빼는 쪽 — 노드 관리자 이동·비활성 차단). 본 작업은 "넣는 쪽"으로 대칭 보강.
- **가드 제외(정상 경로)**: `baim06.updateUserNode`(관리자 임명), `user01.withdrawMyAccount`(NODE_CD=NULL), `mypage01`(노드 미변경) — 요청서 §1 제외 목록 유지.

---

## 6. 권장 처리 순서

1. PRAFTA-046-1 (검증 헬퍼) — 선행.
2. PRAFTA-046-2 (A-1, 에러코드 056 정의) ∥ PRAFTA-046-3 (A-2, login 독립 mapper 채택 시 병렬 가능).
3. PRAFTA-046-4 (A-3, 056 재사용 — 046-2 이후).
4. PRAFTA-046-5 (FE 회귀 검증 + follow-up 점검) — BE 완료 후.
- 보안 검토(security): A-2가 `@NoAuth` 외부 유입이므로 fail-closed·메시지 정보노출 점검 권장. A-1/A-3는 인가 기존 가드 유지 확인.
- QA: 3경로 차단 + 엑셀 행별 부분 실패 + 노드 미변경 수정 통과 + `'*'`/미존재 노드 fail-closed 엣지.

---

## 7. Notion 등록 (메인 세션 대행 필요)

서브에이전트는 Notion 접근 불가. 메인 세션이 "작업 로그" DB에 아래 6행을 등록한다(상태=분해완료, 담당=planner, 상세설명에 본 plan의 각 작업 블록 + 정책서 출처 `common/08-permissions.md §8.4.1` 태그 포함):
- PRAFTA-046-1 [backend] 검증 헬퍼 selectNodeHasAdmin
- PRAFTA-046-2 [backend] A-1 생성 3경로 가드 + USER_400_056
- PRAFTA-046-3 [backend] A-2 회원가입 가드 + LOGIN_400_015
- PRAFTA-046-4 [backend] A-3 노드이동 가드(056 재사용)
- PRAFTA-046-5 [frontend-screen] FE 노드필터 회귀검증 + follow-up (신규 화면/UI명세 없음 → 도메인 지식 베이스 등록 대상 아님)

화면 명세(도메인 지식 베이스 UI-xxx) 등록 대상 없음 — 신규/변경 화면이 없고 기존 공용 팝업이 이미 필터를 적용하기 때문.
