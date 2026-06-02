# PRAFTA-042 작업 분해 계획서

역할(master/hr/safe) 기반 화면권한 잠금 + 전사 사업장/노드 접근 규칙 적용

- 요청서: `.claude/requests/web_requests/prafta-042.md` (D1~D7 전부 확정)
- 영역: web (백엔드 + 프론트엔드 일부)
- 작성: planner (Notion 미접근 — 메인 세션 대행 기록)

---

## 0. 정책서 출처 (INDEX 경유 확인)

| 주제 | 정책서 섹션 | 적용 |
| --- | --- | --- |
| 권한 결정 3축(화면권한×사업장권한×조직스코프) | 공통 §8.1 | 잠금/전사접근 전체 골격 |
| 화면 권한(Role) 관리·권한 단위 구조(USE_YN/BTN_*) | 공통 §8.2.1~§8.2.2 | R1 잠금 단위(D4) |
| 운영 원칙(마스터관리자 비활성화 불가, 변경이력 보존) | 공통 §8.2.3 | master 전체 잠금 근거(D5/D6) |
| 사업장 권한 관리(소속 자동, 그 외 명시 부여) | 공통 §8.3.1~§8.3.3 | R2 자동부여(D3) |
| 조직 스코프(소속 vs 타 사업장, 노드 정/부) | 공통 §8.4.1~§8.4.4 | canManageNode safe 보정 |
| 마스터관리자 예외(전 사업장 자동 매핑) | 공통 §8.5 | D3 자동부여·기존 mergeMasterSiteAuthSet 정합 |

> 충돌 없음. 본 작업은 공통 정책서 §8 단독 근거. 근태/재기획서 섹션은 직접 관련 없음(다만 §8.4 노드 판정 보정이 attd05/07/11 동작에 닿으므로 회귀 주의 — 5장 리스크).

---

## 1. 현행 확정 파악 (직접 코드/스키마 재확인 완료)

### 1.1 역할 유틸 — `com.prafta.common.util.AuthRoleUtils`
- 상수: `AUTH_MASTER="master"`, `AUTH_HR_MANAGER="hr"`, `AUTH_SAFETY_MANAGER="safe"`, `AUTH_NONE="999999"`.
- `isManager` = master || hr (safe 미포함).
- `canManageCommon` = master || safe.
- `isCompanyWide` = canManageCommon = master || safe (hr 미포함).
- 잠금 규칙 상수는 **현재 없음** → 신설 필요(D6 단일 출처).

### 1.2 메뉴/권한 스키마 (schema-full.sql 직접 확인)
- `tb_syst_menu_m` : PK `MENU_M_ID`(varchar10). `MENU_SRC` = SYS007 사용처(web/app)이지 대메뉴코드 아님. **대메뉴코드(attd/baim/chkLst/risk/tbm/nearMiss/user)는 `MENU_M_ID` 값 자체**.
- `tb_syst_menu_d` : PK `(MENU_D_ID, MENU_M_ID)`. `MENU_VIEW`(컴포넌트 경로), `MENU_NM`. → MENU_D_ID 가 화면 단위, 상위 MENU_M_ID 로 대메뉴 귀속.
- `tb_syst_auth_menu` : PK `(CMPNY_CD, AUTH_CD, MENU_D_ID)`. 컬럼 `USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL`(모두 varchar2, DEFAULT 'Y' 단 USE_YN 은 DEFAULT NULL).
  - ⚠️ 컬럼명은 **BTN_DELT**(삭제). DTO/모델은 `btnDel`로 매핑(AuthMenuInfoModel/Command). SQL alias 주의.
- `tb_user_site_auth` : PK `(CMPNY_CD, USER_CD, SITE_CD)`, `USE_YN` DEFAULT 'Y'.
- `tb_user` : PK `(CMPNY_CD, USER_CD)`, `AUTH_CD` NOT NULL, `SITE_CD` nullable(소속 사업장).
- `tb_site` : PK `(SITE_CD, CMPNY_CD)`, `SITE_CD` 코드 컬럼.

### 1.3 권한관리 저장 경로 (User02)
- Controller `POST /user02/update-auth-menu-infos` → `List<AuthMenuInfoRequest>` 수신 → `AuthMenuInfoParam.from()` → Service `updateAuthMenuInfo()` → 행마다 `User02Mapper.mergeAuthMenuInfo`(INSERT ON DUP UPDATE).
- `AuthMenuInfoModel/Command` 필드: authCd, menuDId, useYn, btnSrch, btnNew, btnDel, btnSave, btnExcl, gvCmpnyCd, gvUserCd. **MENU_M_ID(대메뉴)는 현재 미포함** → 잠금판정 위해 menuDId→menuMId 매핑 조회가 필요(매퍼 신설).
- 조회 SQL `selectAuthMenuList`는 menu_m_id/menu_src 까지 내려보냄(FE가 menuMId 사용).
- **BE 잠금 검증 전무** → devtools/직접 API 로 잠금 메뉴 USE_YN='N' 저장 가능(보안 High).

### 1.4 화면권한 잠금 FE 현행 — `src/views/user/User_02.vue`
- `getMenuModuleId(menu)` : `menuMId`(예 `Baim_01`)의 `_` 앞 토큰을 **소문자**로 — `baim`, `chklst`(주의: chkLst→chklst), `risk`, `tbm`, `attd`, `user`, `nearmiss`.
- `isRowCheckboxDisabled(menu)` (약 371~384):
  - master → 항상 true(체크박스 숨김 컬럼 `isCheckboxColumnHidden`).
  - hr → `["baim","attd","user"]` 비활성.
  - safe → `["baim","user","risk","tbm","chklst"]` 비활성. ← **nearMiss(소문자 `nearmiss`) 미포함**.
- 정정 대상: safe 배열에 `"nearmiss"` 추가. hr/master 현행 유지.

### 1.5 사업장 자동권한 부여 현행
- 신규 사업장 생성: `Baim01ServiceImpl.saveSiteInfo()` — `model.siteCd()==null` 이면 신규(채번), 아니면 수정. 신규/수정 공통으로 `baim01Mapper.mergeMasterSiteAuthSet(...)` 호출.
  - `Baim01Mapper.mergeMasterSiteAuthSet` : `WHERE AUTH_CD IN ('master','system')` 사용자에게 해당 SITE_CD INSERT ON DUP(멱등). ← **'hr','safe' 미포함** → 확장 대상.
- 사용자 신규 생성: `User01ServiceImpl.insertUserOne()` 18단계 — `insertOneUserSiteAuth(소속 siteCd 1건)` + 18.1 추가사이트. **전사 부여 로직 없음** → master/hr/safe 신규 시 전 사업장 INSERT 필요.
- 사용자 역할 변경: `User01ServiceImpl.updateOneUserInfo()` — `model.authCd()` 반영(`mergeUserInfo`). 사이트변경(소속 site 변경) 시에만 site_auth delete+insert. **역할변경에 따른 전사부여/회수 로직 없음** → D3-②/D7 추가 대상.
  - ⚠️ 역할 변경 진입점이 `updateOneUserInfo` 외 별도 경로가 있는지(엑셀 일괄 `User01BatchServiceImpl`) 추가 확인 필요(리스크 R-3).

### 1.6 노드 관리 스코프 — `canManageNode`
- 정의: `AttdCloseServiceImpl.canManageNode(authCd,userCd,cmpnyCd,siteCd,nodeCd)` (인터페이스 `AttdCloseService`).
  - `AuthRoleUtils.isManager(authCd)`(master/hr) → 전사 통과. 그 외 → 노드관리자(countNodeAdmin>0)만.
  - safe 는 isManager 아님 → 현재 전사 통과 못함(노드관리자여야 통과).
- 사용처(직접): `Attd07ServiceImpl`(5곳), `Attd11ServiceImpl`(1곳), `canManageUser`(내부 위임) → `Attd05ServiceImpl`(303 `canManageUser`).
- 그 외 isCompanyWide/isManager 사용처(전수 후보, 6장 표): attd07/attd11/nearmiss01/tbm02/tbm04/user01/leave*/reqinbox/baim07 등.
- D3 노드 보정 방향: 요청서는 "노드 단위 관리 판정에 safe 포함" 또는 "사업장권한 보유 기반 일관 판정". → **`canManageNode` 의 전사 통과 조건을 isManager(master/hr)에서 master/hr/safe(=새 헬퍼)로 확장**하는 방식 채택. 단 회귀(attd05/07/11) 검증 필수.

---

## 2. 잠금 규칙 단일 출처 설계 (D6)

신규 클래스 `com.prafta.common.util.MenuLockPolicy`(또는 AuthRoleUtils에 상수 추가). 권장: **전용 클래스**로 분리(AuthRoleUtils 비대화 방지, 단일 책임).

- 상수 매핑(역할 → 잠금 대메뉴 MENU_M_ID 집합):
  - master → 전체(특수 마커: `LOCK_ALL`).
  - hr → `{attd, baim, user}` (D1).
  - safe → `{chkLst, risk, tbm, baim, user, nearMiss}` (D2).
- ⚠️ MENU_M_ID **실제 표기(대소문자) 확정 필요** — DB 의 `MENU_M_ID` 값이 `chkLst`/`nearMiss`(camel) 인지 소문자인지 MCP 로 1회 확인하여 상수 표기를 DB와 100% 일치. (FE는 소문자 비교라 무관하나 BE는 DB값과 equals 비교 → 표기 불일치 시 잠금 누락. **R-1 리스크**)
- API:
  - `boolean isLockedMenu(String authCd, String menuMId)` — master면 무조건 true, hr/safe면 집합 포함 여부.
  - (선택) `Set<String> lockedMenuMIds(String authCd)`.

---

## 3. 단위 작업 분해 (PRAFTA-042-N, 의존순서)

> 의존순서: 042-1 → 042-2 → (042-3 FE 병행 가능) / 042-4 → 042-5 → 042-6(독립, 회귀검증 무겁게).

### PRAFTA-042-1 [backend] 잠금 규칙 단일출처 + menuDId→menuMId 매핑 조회
- 유형: 신규
- 정책: 공통 §8.2.2(USE_YN/BTN_*), §8.2.3, §8.5
- 내용:
  1) `MenuLockPolicy` 신설(역할→잠금 MENU_M_ID 집합 상수, `isLockedMenu`). master=LOCK_ALL.
  2) DB의 MENU_M_ID 실표기 확인 후 상수 표기 정합(R-1).
  3) `User02Mapper.selectMenuMIdByMenuDId(cmpnyCd?, menuDId)` 또는 일괄 `selectMenuMIdMap(List<menuDId>)` 신설(`tb_syst_menu_d` 조회). menuDId→menuMId.
- 영향 파일:
  - (신규) `prafta-backend/.../common/util/MenuLockPolicy.java`
  - `prafta-backend/.../web/user/user02/mapper/User02Mapper.java` + `resources/.../user02/mapper/User02Mapper.xml`
- endpoint: 없음(내부).
- 마이그레이션: 없음.
- 선행: 없음.
- 리스크: R-1(MENU_M_ID 표기), R-4(다수 행 매핑 성능 — 일괄 IN 조회 권장).

### PRAFTA-042-2 [backend] update-auth-menu-infos 잠금 강제 보정(D5) + 변조 warn 로그
- 유형: 보완
- 정책: 공통 §8.2.2/§8.2.3, D4/D5
- 내용:
  1) `User02ServiceImpl.updateAuthMenuInfo()`에서 각 model 의 menuDId→menuMId 조회(042-1) 후 `MenuLockPolicy.isLockedMenu(authCd, menuMId)` 판정.
  2) 잠금 대상이면 USE_YN/BTN_SRCH/NEW/DELT/SAVE/EXCL 을 모두 'Y'로 **서버 강제 보정** 후 merge. 거부 아님(D5).
  3) 입력값이 잠금과 달랐으면(어느 컬럼이든 'N') `log.warn("화면권한 잠금 위반 변조 시도 - authCd={}, menuDId={}, menuMId={}, 입력={}", ...)` (PII 없음, 한국어).
  4) authCd 는 요청 body 의 각 행 `authCd`(편집 대상 역할). master 행 전체 보정.
- 영향 파일:
  - `prafta-backend/.../web/user/user02/service/impl/User02ServiceImpl.java`
  - (보정값을 새 Command로 만들거나 Model 변환) `AuthMenuInfoModel`/`AuthMenuInfoCommand` — 보정은 Service단에서 새 record 생성으로 처리(불변 record라 with 없음 → 새 인스턴스 생성).
- endpoint: `POST /webApi/user02/update-auth-menu-infos` (동작 변경, 시그니처 불변).
- 마이그레이션: 없음.
- 선행: PRAFTA-042-1.
- 리스크: R-1(표기), R-2(보정 후 INSERT_NO/UPDATE_NO 일관), 우회 방어가 본 작업 핵심(AC4).

### PRAFTA-042-3 [frontend-screen] User_02.vue safe 잠금 집합에 nearMiss 추가
- 유형: 보완
- 정책: 공통 §8.2.2(FE는 UX 보조, 강제는 BE)
- 내용: `isRowCheckboxDisabled` safe 분기 배열에 `"nearmiss"` 추가. hr/master 현행 유지. (UI 명세 prafta-042-ui-spec.md)
- 영향 파일: `prafta-web-frontend/prafta-web-frontend/src/views/user/User_02.vue` (script 영역 약 380~382, 1줄)
- endpoint: 없음(기존 조회/저장 재사용).
- 마이그레이션: 없음.
- 선행: 없음(042-2와 독립이나, BE 보정이 진짜 방어. FE는 UX만).
- 리스크: 낮음. (단 `getMenuModuleId`가 nearMiss를 `nearmiss`로 소문자화하므로 배열도 소문자 `"nearmiss"` — 표기 주의)

### PRAFTA-042-4 [backend] 사업장 자동권한 부여 — 신규 사업장(D3-①)
- 유형: 보완
- 정책: 공통 §8.3.1, §8.5
- 내용: `Baim01Mapper.mergeMasterSiteAuthSet` 의 `WHERE AUTH_CD IN ('master','system')` 를 **`('master','hr','safe','system')`** 로 확장(기존 멱등 ON DUP 유지). system 계정 유지 여부 확인(현행 보존 권장).
  - ⚠️ 수정 저장 경로에서도 호출되므로(신규/수정 공통) 멱등 INSERT 라 부작용 적음 — 다만 "기존 사업장 수정 시마다 전 master/hr/safe 재부여(USE_YN='Y' 복구)" 가 의도인지 확인. 회수된 권한을 수정으로 되살리는 부작용 가능 → **신규 생성(siteCd==null)일 때만 호출하도록 Service 분기 권장**(D7 회수와 충돌 방지). 본 분기 변경 포함.
- 영향 파일:
  - `prafta-backend/.../web/baim/baim01/service/impl/Baim01ServiceImpl.java`(신규생성 분기에서만 호출)
  - `prafta-backend/.../resources/.../baim01/mapper/Baim01Mapper.xml`(IN 목록 확장)
- endpoint: 기존 사업장 저장 endpoint(Baim01) 동작 변경.
- 마이그레이션: 없음(런타임).
- 선행: 없음.
- 리스크: R-5(수정 경로 재부여로 D7 회수분 부활), system 계정 정책 확인.

### PRAFTA-042-5 [backend] 사업장 자동권한 부여/회수 — 사용자 생성·역할변경(D3-②, D7)
- 유형: 보완
- 정책: 공통 §8.3.1, §8.4, §8.5
- 내용:
  1) **신규 생성**(`User01ServiceImpl.insertUserOne`): 신규 authCd 가 master/hr/safe 면 전 사업장 `tb_user_site_auth` INSERT(멱등 ON DUP). 신규 매퍼 `insertAllSiteAuthForUser(cmpnyCd,userCd,gvUserCd)`(tb_site 전사 SELECT→INSERT). 기존 소속 site 1건 INSERT(18단계)와 ON DUP로 충돌 없음.
  2) **역할 변경**(`User01ServiceImpl.updateOneUserInfo`): 변경 전 AUTH_CD 조회(서버) vs 신규 authCd 비교.
     - 비대상→master/hr/safe 진입: 전 사업장 INSERT(멱등).
     - master/hr/safe→비대상 이탈(D7): 자동부여분 회수하되 **소속 사업장(TB_USER.SITE_CD) 1건만 잔존**. 회수 매퍼 `deleteSiteAuthExceptHome(cmpnyCd,userCd,homeSiteCd)`(소속 SITE_CD 제외 DELETE). 소속 site 행은 보존/없으면 INSERT.
     - 역할 변동 없음 + master/hr/safe 유지: 무처리(또는 멱등 재부여 — 신규 사업장 백필은 042-4가 담당하므로 여기선 불필요).
  3) 멱등: 모든 INSERT 는 ON DUP UPDATE USE_YN='Y'. 회수는 명시 DELETE(소속 제외).
- 영향 파일:
  - `prafta-backend/.../web/user/user01/service/impl/User01ServiceImpl.java`(insertUserOne, updateOneUserInfo)
  - (역할변경 일괄 경로 존재 시) `User01BatchServiceImpl.java` — R-3 확인 후 동일 처리.
  - `prafta-backend/.../web/user/user01/mapper/User01Mapper.java` + xml(신규: insertAllSiteAuthForUser, deleteSiteAuthExceptHome, selectUserAuthCd(변경 전 조회) — 기존 조회 재사용 가능 검토)
- endpoint: `POST /webApi/user01/...`(사용자 저장/생성 — 동작 변경, 시그니처 불변).
- 마이그레이션: 없음(런타임).
- 선행: 042-4(자동부여 정책 일관성), 042-1 불필요.
- 리스크: R-3(역할변경 진입점 누락 — 엑셀일괄/다른 update 경로), R-6(소속 SITE_CD 가 null 인 사용자의 회수 잔존 처리), R-7(전 사업장 INSERT 대량 — 사업장 수만큼, 보통 소량이나 LIMIT 불요·트랜잭션 내).

### PRAFTA-042-6 [backend] 노드 관리 스코프에 safe 포함 보정
- 유형: 보완
- 정책: 공통 §8.4.1~§8.4.4, §8.5
- 내용:
  1) (D3 채택안) `canManageNode` 전사 통과 조건을 master/hr → master/hr/safe 로 확장. 구현: AuthRoleUtils 에 신규 헬퍼 `canManageAllNodes(authCd)`(= master||hr||safe) 신설 후 `AttdCloseServiceImpl.canManageNode` 에서 `isManager` 대신 사용. **isManager 자체는 변경 금지**(다른 의미 사용처 회귀 방지 — OT 대리등록 등).
  2) `canManageUser`(AttdCloseServiceImpl 184~196)도 동일 헬퍼로 전사 통과 확장(safe 가 타사용자 근태 관리 가능해야 함).
  3) isCompanyWide(=master||safe) 는 이미 safe 포함 → 변경 불필요. **hr 가 isCompanyWide 미포함**인 현행은 요청서 R2가 "사업장권한 보유 기반"으로 전사 접근을 해소하므로 그대로 둠(hr 도 042-4/5로 전 사업장 site_auth 보유 → 사업장권한 검사 통과). isCompanyWide 변경은 범위 외(보류).
- 영향 파일:
  - `prafta-backend/.../common/util/AuthRoleUtils.java`(canManageAllNodes 신설)
  - `prafta-backend/.../web/attd/attd07/service/impl/AttdCloseServiceImpl.java`(canManageNode, canManageUser)
- endpoint: 직접 변경 없음(내부 판정). 영향 API: attd05/07/11 노드 스코프 게이트.
- 마이그레이션: 없음.
- 선행: 없음(042-5와 독립이나, 사업장권한+노드권한 둘 다 충족해야 실제 접근되므로 함께 배포 권장).
- 리스크: R-8(attd05/07/11 회귀 — safe 가 기존엔 노드관리자만 통과했는데 이제 전사 통과 → 근태마감/판정 권한 확대. 요청서 의도와 일치하나 마감 쓰기차단 등 부수효과 QA 필요), R-9(`isManager`와 `canManageAllNodes` 혼용 지점 식별 정확도).

### PRAFTA-042-7 [migration] 기존 사업장 × 기존 master/hr/safe 백필 (D3-③)
- 유형: 신규(마이그레이션 SQL 파일만, 운영적용은 사용자)
- 정책: 공통 §8.3.1, §8.5
- 내용: 모든 (master/hr/safe USE_YN='Y' 사용자) × (전 사업장 tb_site) 조합을 tb_user_site_auth 에 INSERT(멱등 ON DUP UPDATE USE_YN='Y'). CMPNY_CD 단위. system 계정 포함 여부는 042-4와 일치.
- 영향 파일: (신규) `prafta-backend/src/main/resources/sql/migration/prafta-042-site-auth-backfill.sql`
- 마이그레이션: 있음(파일만 작성, 운영 미적용).
- 선행: 042-4/5 정책 확정 후(부여 대상 역할집합 일치).
- 리스크: R-10(대량 INSERT — 사용자×사업장 카티전. 회사 규모 작으면 무해. 운영 적용 전 건수 확인 권장).

---

## 4. 화면 변경 요약 (UI 명세는 prafta-042-ui-spec.md)
- User_02.vue: safe 선택 시 nearMiss 대메뉴 하위 행 체크박스 disabled 추가. 그 외 동작/레이아웃 불변. (상세 prafta-042-ui-spec.md UI-001)
- User_03.vue: 변경 없음(전사 사업장 안내 문구는 요청서 "필요 시" — 본 분해에서는 미포함. 추가 원하면 별도 작업).

---

## 5. 리스크 / 미확인 사항 (developer·qa·security 인계)

| ID | 내용 | 처리 |
| --- | --- | --- |
| R-1 | `tb_syst_menu_m.MENU_M_ID` 실제 표기(camel `chkLst`/`nearMiss` vs 소문자). BE equals 비교라 불일치 시 잠금 누락 | 042-1 착수 시 MCP `SELECT MENU_M_ID FROM tb_syst_menu_m` 1회 확인 후 상수 정합 (필수) |
| R-2 | tb_syst_auth_menu 삭제 컬럼명 `BTN_DELT` ↔ 모델 `btnDel` alias | merge xml 이미 처리됨(btnDel→BTN_DELT). 신규 코드도 동일 매핑 유지 |
| R-3 | 사용자 역할변경 진입점이 updateOneUserInfo 외 존재?(엑셀일괄 User01BatchServiceImpl 등) | 042-5 착수 시 AUTH_CD 변경 가능 경로 전수 grep 후 동일 처리 |
| R-4 | menuDId→menuMId 행별 조회 N+1 | 일괄 IN 조회로 1쿼리 처리 |
| R-5 | Baim01 수정 저장이 mergeMasterSiteAuthSet 재호출 → D7로 회수한 권한 부활 | 042-4: 신규생성(siteCd==null) 분기에서만 호출하도록 변경 |
| R-6 | TB_USER.SITE_CD 가 null 인 master/hr/safe 사용자 D7 회수 시 잔존 site 없음 | 회수 SQL: 소속 site null이면 전량 회수(잔존 0건) 허용 — 정책상 명시 없음, 가정 처리 후 보고 |
| R-7/R-10 | 전 사업장 INSERT/백필 대량 | 사업장 수 보통 소량. 백필 운영 적용 전 건수 확인 권장 |
| R-8 | safe 노드 전사 통과 확대 → attd05/07/11 근태마감·판정 권한 확대 부수효과 | qa: 마감 쓰기차단/판정 회귀 시나리오 필수. 요청서 의도(safe 전사 접근)와 일치 확인됨 |
| R-9 | isManager vs canManageAllNodes 혼용 식별 | 042-6: isManager 불변, canManageNode/canManageUser만 신헬퍼로 전환. OT 대리등록 등 isManager 사용처는 의도적 미변경 |
| R-11 | system 계정 자동부여 유지 여부(기존 master+system) | 현행 유지(보존) 가정. 미동의 시 보고 |

---

## 6. canManageNode / isCompanyWide / isManager 사용처 (전수 후보 — developer 재확인 기준)

`canManageNode` 직접 호출:
- web/attd/attd07/.../AttdCloseServiceImpl(정의+ensureCanManageScope 내부)
- web/attd/attd07/.../Attd07ServiceImpl (121, 242, 382, 479, 550)
- web/attd/attd11/.../Attd11ServiceImpl (58)
- canManageUser(내부 위임) → web/attd/attd05/.../Attd05ServiceImpl (303)

`isManager`/`isCompanyWide`/`canManageCommon` 출현 파일(26): attd07, attd11, attd09, nearmiss01(web/app), user01(+batch+upload), tbm01/02/04, leave(GrantEngine/Dashboard/Policy), reqinbox, baim07, AuthRoleUtils.
- 042-6 범위는 **canManageNode/canManageUser 한정**. 나머지 isManager 사용처는 의미가 다르므로(생성권한/대리등록 등) 변경하지 않음 — developer가 042-6 구현 시 위 목록에서 canManageNode 계열만 손댄다.

---

## 7. 실행 순서 권장
1. 042-1 → 042-2 (잠금 BE 핵심, AC1~4·7)
2. 042-3 (FE UX, 042-2와 병행 가능)
3. 042-4 → 042-5 (사업장 자동부여/회수, AC5·6·8)
4. 042-6 (노드 safe 보정 — 회귀검증 무겁게, AC와 직접 매핑은 없으나 R2 "관리 스코프 포함" 충족)
5. 042-7 (백필 SQL, 운영적용 사용자)
