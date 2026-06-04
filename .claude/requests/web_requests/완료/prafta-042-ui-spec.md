# PRAFTA-042 UI 명세

## UI-001 User_02 (권한별 화면권한 관리) — 잠금 집합 보완

- 연결 작업: PRAFTA-042-3
- 영역: web
- 모듈: user
- 화면 위치: `prafta-web-frontend/prafta-web-frontend/src/views/user/User_02.vue`
- 작업 유형: 보완 (기존 화면, 1줄 수정)
- 참조 패턴: 기존 User_02.vue 자체의 `isRowCheckboxDisabled` / `getMenuModuleId` 패턴 그대로 따름. 신규 컴포넌트/토큰 도입 없음.

### 현재 동작
- 좌측 권한 목록(master/hr/safe 등)에서 역할 선택 → 우측에 대메뉴×화면 권한표(USE_YN/BTN_*) 체크박스 노출.
- 잠금(체크박스 disabled) 규칙(`isRowCheckboxDisabled`):
  - master: 체크박스 열 자체 숨김(`isCheckboxColumnHidden`).
  - hr: 모듈 `baim, attd, user` 행 비활성.
  - safe: 모듈 `baim, user, risk, tbm, chklst` 행 비활성.
- 모듈 식별: `getMenuModuleId`가 `menuMId`(예 `Baim_01`)의 `_` 앞 토큰을 소문자화(`baim`, `chklst`, `nearmiss` 등).

### 의도된 동작 (변경점)
- safe 잠금 모듈 집합에 **nearMiss(소문자 `nearmiss`)** 추가 → `["baim","user","risk","tbm","chklst","nearmiss"]`.
- hr, master 동작은 현행 유지.
- 결과: safe 역할 선택 시 아차사고관리(nearMiss) 대메뉴 하위 화면 행의 체크박스가 disabled 로 표시되어 해제 불가 UX 제공.

### 레이아웃 (변경 없음 — 참고용 와이어프레임)
```
+-------------------------------------------------------------+
| [권한관리]                              [조회][저장][엑셀]   |
+----------------------+--------------------------------------+
| 권한 목록            | 화면권한 (선택 권한 = safe)          |
| ○ master            |  대메뉴   | 화면   |사용|조회|..|엑셀|
| ○ hr                |  순회점검 | ...    |[x] |[x] |..|[x] | (disabled)
| ● safe              |  위험성   | ...    |[x] |..             | (disabled)
|                     |  TBM      | ...    |[x] |..             | (disabled)
|                     |  아차사고 | ...    |[x] |..             | (NEW disabled)
|                     |  근태관리 | ...    |[ ] |..             | (편집가능)
+----------------------+--------------------------------------+
```

### 컴포넌트 매핑 (변경 없음)
| 영역 | 컴포넌트 | 비고 |
| --- | --- | --- |
| 권한 목록/권한표 | 기존 테이블 + ThSortable | 변경 없음 |
| 체크박스 | 기존 체크박스 (`:disabled="isRowCheckboxDisabled(m)"`) | 판정 함수만 보완 |

### 상태별 동작
- loading/empty/error/success: 기존 동작 불변. nearMiss 행이 권한표에 존재할 때만 disabled 적용(없으면 무영향).

### 사용자 플로우
1. 권한관리 진입 → safe 선택.
2. 우측 권한표에서 nearMiss 대메뉴 하위 행 체크박스가 disabled(해제 불가)로 표시.
3. 저장 시도 시 disabled 라 해제 불가. (직접 API 우회는 BE PRAFTA-042-2가 'Y' 강제 보정 — AC4)

### 수정 포인트 (개발자 가이드)
- **script 영역만 수정**. template/style 변경 없음.
- `isRowCheckboxDisabled` 함수 내 safe 분기 배열에 `"nearmiss"` 추가:
  - 위치: `User_02.vue` 약 380~382.
  - 현행: `return ["baim","user","risk","tbm","chklst"].includes(moduleId);`
  - 변경: `return ["baim","user","risk","tbm","chklst","nearmiss"].includes(moduleId);`
- ⚠️ 반드시 소문자 `"nearmiss"` (getMenuModuleId가 `nearMiss`→`nearmiss`로 소문자화하므로). camel 표기 시 매칭 실패.

### 백엔드 의존
- 조회: `GET /webApi/user02/auth-menu-lists` (기존, 변경 없음 — menuMId 내려옴).
- 저장: `POST /webApi/user02/update-auth-menu-infos` (PRAFTA-042-2가 잠금 강제 보정 — FE는 UX 보조, 실제 방어는 BE).
