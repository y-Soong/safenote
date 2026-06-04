# 역할(master/hr/safe) 기반 화면권한 잠금 + 전사 사업장/노드 접근 규칙 적용 요청의 건

## 1. 배경 / 목표

master·hr·safe 역할의 권한 모델을 명확히 하고, 일부 화면권한을 **해제 불가(잠금)** 로 강제한다.

- 공통: master/hr/safe 는 **모든 사업장 및 소속 노드에 접근** 가능해야 한다.
- 차이(해제 불가 메뉴):
  - **master**: 모든 화면 권한을 해제할 수 없다(항상 부여).
  - **hr**: 대메뉴 **근태관리(attd)** 하위 모든 화면 권한을 해제할 수 없다.
  - **safe**: 대메뉴 **순회점검(chkLst) · 위험성평가(risk) · TBM관리(tbm) · 아차사고 관리(nearMiss)** 하위 모든 화면 권한을 해제할 수 없다.

## 2. 현행 조사 결과 (전수조사 — 읽기전용)

### 2.1 역할 코드
- `com.prafta.common.util.AuthRoleUtils` : `master`/`hr`/`safe`(그 외 `999999`=차단).
- 헬퍼: `isManager`=master||hr, `canManageCommon`=master||safe, `isCompanyWide`=master||safe. (※ hr 은 isCompanyWide 에 미포함)

### 2.2 메뉴/권한 데이터 모델
- `tb_syst_auth_menu` (역할별 화면권한) — PK `(CMPNY_CD, AUTH_CD, MENU_D_ID)`, 컬럼 `USE_YN, BTN_SRCH/NEW/DELT/SAVE/EXCL`.
- `tb_syst_menu_m` (대메뉴) — PK `MENU_M_ID`.
- `tb_syst_menu_d` (화면) — PK `(MENU_D_ID, MENU_M_ID)`, `MENU_VIEW`(컴포넌트 경로).
- 대메뉴 코드(실DB): `attd`=근태관리, `baim`=기초정보관리, `chkLst`=순회점검관리, `risk`=위험성평가, `tbm`=TBM관리, `nearMiss`=아차사고 관리, `user`=사용자관리, `login`=로그인.

### 2.3 권한 관리 화면/엔드포인트
- FE: `prafta-web-frontend/.../src/views/user/User_02.vue` (역할 선택 → 화면별 권한 체크/해제 → 저장).
- BE: `GET /webApi/user02/auth-menu-lists`, `POST /webApi/user02/update-auth-menu-infos`(User02Controller/Service/Mapper, MERGE=INSERT ON DUP UPDATE).
- 사업장권한(별개): `tb_user_site_auth`(사용자-사업장), 화면 User_03 / `POST /webApi/user03/update-user-site-auth`.

### 2.4 잠금 규칙 현재 적용 여부 → **FE UI 에만 부분 적용, BE 검증 전무(우회 가능, 보안 High)**
- master: 체크박스 숨김(전 메뉴) — `User_02.vue` 약 387~391.
- hr: `attd, baim, user` 비활성화 — `User_02.vue` 약 377~378.
- safe: `risk, tbm, chkLst, baim, user` 비활성화 — `User_02.vue` 약 380~381.
- BE(`update-auth-menu-infos`/Param/Service)에 잠금 검증 **없음** → 브라우저로 disabled 우회 시 USE_YN='N' 저장 가능.

### 2.5 전사 사업장/노드 접근 → **미적용**
- 현재 master/hr/safe 도 `tb_user_site_auth` 에 행이 있어야 해당 사업장 접근. 역할 기반 전사 예외 로직 없음.
- 데이터 스코프 헬퍼 `isCompanyWide`(master/safe)는 존재하나 hr 미포함, 그리고 화면권한과는 별개 경로.

## 3. 적용해야 할 규칙 (구현 목표)

### R1. 화면권한 잠금(해제 불가) — **FE + BE 양쪽 강제** (D1/D2/D4 확정)
- 잠금 = 잠금 대상 화면은 **화면접근(USE_YN='Y') + 모든 버튼권한(BTN_SRCH/NEW/DELT/SAVE/EXCL='Y')** 이 항상 "열린 채로 고정"되어야 한다(해제 불가). (D4)
- 역할별 잠금 대메뉴(확정):
  - **master** → 전 대메뉴(전 화면).
  - **hr** → `attd`(근태관리), `baim`(기초정보관리), `user`(사용자관리). (D1: 현행 유지)
  - **safe** → `chkLst`(순회점검), `risk`(위험성평가), `tbm`(TBM관리), `baim`, `user`, **`nearMiss`(아차사고, 신규 추가)**. (D2: 현행 + nearMiss)
- **BE 검증 필수**: `update-auth-menu-infos` 저장 시 잠금 대상(역할×잠금대메뉴 하위 MENU_D_ID)은 USE_YN 및 BTN_* 를 'Y' 로 **서버가 강제 보정**(D5). FE 우회 차단. 잠금 규칙은 Java 단일 출처(D6)에서 조회.
- FE: 현행 disabled/숨김 유지(잠금 집합은 위와 일치 — hr 은 현행 그대로, safe 는 nearMiss 만 추가).

### R2. 전사 사업장/노드 접근 (D3 확정 — 사업장권한 자동부여 방식)
- 별도 역할 예외 분기 대신, **master/hr/safe 사용자에게 모든 사업장의 `tb_user_site_auth` 권한을 항상 보유**시켜, 기존 "사업장 권한 유무" 검사 로직을 그대로 활용한다(관리 스코프 포함).
- 자동 부여 트리거(누락 방지):
  1. **신규 사업장 생성 시** → 그 시점의 모든 master/hr/safe 사용자에게 신규 사업장 권한 INSERT.
  2. **사용자가 master/hr/safe 로 신규 생성/역할 변경 시** → 기존 모든 사업장 권한 INSERT.
  3. **(1회) 기존 사업장 × 기존 master/hr/safe 사용자 백필** 마이그레이션.
  4. (선택) 역할이 master/hr/safe 에서 이탈 시 자동부여분 회수 여부 — D7(아래).
- **노드 관리 스코프**: `canManageNode` 는 현재 master/hr 만 전사 통과(safe 미포함). safe 도 접근 가능해야 하므로 노드 단위 관리 판정에 **safe 포함**되도록 보정(또는 사업장권한 보유 기반으로 일관 판정). 적용 지점(canManageNode/isCompanyWide/각 스코프 검사) 은 planner/developer 가 전수 식별.

## 4. 결정 사항 (확정 / 일부 확인중)

- **D1 (hr 잠금 범위)** → **확정: 현행 유지** = `attd, baim, user`(업무상 필요로 baim/user 도 잠금 유지).
- **D2 (safe 잠금 범위)** → **확정: 현행 + nearMiss 추가** = `chkLst, risk, tbm, baim, user, nearMiss`.
- **D3 (전사 접근 구현 방식)** → **확정: 사업장권한 자동부여 방식**(R2 참조). 사업장 생성/역할 부여 시 master/hr/safe 에 `tb_user_site_auth` 자동 INSERT + 기존 백필. 관리 스코프까지 포함. 노드 판정엔 safe 포함 보정.
- **D4 (잠금 단위)** → **확정: 화면접근(USE_YN) + 버튼권한(BTN_*) 모두 'Y' 로 열린 채 잠금.**
- **D5 (위반 처리)** → **확정: (b) 강제 보정**. 잠금 위반 입력이 도달해도 서버가 잠금 대상의 USE_YN/BTN_* 를 'Y' 로 강제 보정한 뒤 저장한다(입력이 잠금과 다르면 warn 로그로 변조 흔적 기록). 거부(에러) 아님. (FE 는 이미 체크박스 disabled 라 정상 경로에선 발생 안 함 — 본 처리는 devtools/직접 API 우회 방어용.)
- **D6 (잠금 규칙 저장)** → **확정: Java 비즈니스 로직 상수 매핑**. 역할→잠금 대메뉴 규칙을 한 곳(예 `AuthRoleUtils` 또는 전용 권한정책 클래스)에 상수로 정의·단일 출처화. **신규 테이블 없음**(규칙이 정적이라 설정테이블 불필요). `TB_USER_SITE_AUTH` 는 D3(사업장 접근)용일 뿐 잠금 규칙과 무관(구조상 담을 수 없음).
- **D7 (역할 이탈 시 사업장권한 회수)** → **확정: 회수하되 소속 사업장만 잔존**. 사용자가 master/hr/safe 에서 다른 역할로 바뀌면 자동부여된 `tb_user_site_auth` 를 회수하고, **그 사용자의 소속 사업장(TB_USER.SITE_CD) 권한 1건만 남긴다.**

## 5. 영향 범위 (예상)
- FE: `User_02.vue`(잠금 집합 정정·BE 응답과 정합), 필요 시 `User_03.vue`(전사 사업장 안내).
- BE: `User02Controller/Service/Impl`, `User02Mapper(.xml)`, `AuthMenuInfoParam`(잠금 검증 추가), `AuthRoleUtils`(safe/hr 헬퍼·전사 판정), 사업장/노드 데이터 스코프 검사 지점(R2).
- 정책 출처: 권한/인증·메뉴 관련 정책서 섹션(planner 가 INDEX 경유 확인).
- 신규 테이블/마이그레이션 여부는 D6 에 따라 결정(가급적 신규 없이 기존 구조 활용).

## 6. 수용 기준 (AC)
1. hr 잠금대메뉴(`attd, baim, user`) 하위 화면의 USE_YN/BTN_* 를 'N' 으로 저장 시도 → FE 비활성 + BE 가 'Y' 강제(또는 거부). DB 결과 USE_YN 및 모든 BTN_*='Y' 유지.
2. safe 잠금대메뉴(`chkLst, risk, tbm, baim, user, nearMiss`) 하위 화면도 동일하게 USE_YN/BTN_* 잠금 보장(nearMiss 가 신규 잠금에 포함됨).
3. master 계정은 전 대메뉴 화면접근/버튼권한 모두 해제 불가.
4. 브라우저 개발자도구로 FE disabled 제거 후 USE_YN/BTN_*='N' 을 직접 전송해도 BE 가 차단/보정(우회 불가).
5. 신규 사업장 생성 시 기존 master/hr/safe 사용자에게 해당 사업장 `tb_user_site_auth` 가 자동 부여되어, 추가 설정 없이 해당 사업장/소속 노드 데이터에 접근·관리 가능. (기존 사업장도 백필로 동일)
6. 사용자가 master/hr/safe 로 신규/변경되면 기존 모든 사업장 권한이 자동 부여된다.
7. 잠금 규칙은 Java 비즈니스 로직(상수, 단일 출처)으로 정의되며 master=전체 / hr=attd,baim,user / safe=chkLst,risk,tbm,baim,user,nearMiss 가 한 곳에 표현된다(신규 테이블 없음).
8. 사용자가 master/hr/safe → 다른 역할로 변경되면 자동부여 사업장권한이 회수되고 소속 사업장(TB_USER.SITE_CD) 권한 1건만 남는다. (D7)
9. 비대상 역할/메뉴의 권한 부여·해제는 기존대로 자유롭게 동작(회귀 없음).

---

# 추가로 정해줘야 하거나 모호한 게 있으면 채팅으로 질의해줘. (특히 D1~D6)
