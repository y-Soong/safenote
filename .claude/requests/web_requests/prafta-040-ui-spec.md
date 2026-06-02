# PRAFTA-040 — 웹 화면 UI 명세 (Notion "도메인 지식 베이스" 등록 초안)

> 서브에이전트는 Notion 등록 불가 → 메인세션이 본 md 내용으로 UI-{순번} 행 등록.
> 검증 상태: `Claude 분석`. 영역: web. 모듈: nearMiss / risk.
> 참조 패턴: `src/views/risk/Risk_03.vue`(목록), `src/views/risk/popup/RiskAssessInfo.vue`(2단 드래그 모달).

---

## UI-NM-01 — NearMiss_01 (사건 관리 목록)

- 연결 작업: PRAFTA-040-3
- 화면 위치: `src/views/nearMiss/NearMiss_01.vue`
- 현재 동작: 신규 작성
- 참조 패턴: Risk_03.vue (ViewHeader + viewSearch + viewBody table-wrapper/subtitle/data-grid + ThSortable + useModal)

### 레이아웃 (와이어프레임)
```
┌────────────────────────────────────────────────────────────────────┐
│ ViewHeader  "사건 관리"                                  [조회]      │
├────────────────────────────────────────────────────────────────────┤
│ viewSearch                                                          │
│  사업장 [코드][🔍][명]   사건유형 [▾]  잠재중대성 [▾]              │
│  발생기간 [시작]~[종료]                                             │
├────────────────────────────────────────────────────────────────────┤
│ 상태 탭:  [ 접수 2 ][ 검토중 1 ][ 조치중 0 ][ 완료 5 ]  (전체)      │
├────────────────────────────────────────────────────────────────────┤
│ subtitle: 사건 리스트                                               │
│ ┌──┬───────────┬──────┬──────────┬──────┬───────┬───────────┬─────┐│
│ │No│ 사건ID     │ 유형 │잠재중대성│ 공정 │보고자 │ 발생일시  │상태 ││
│ ├──┼───────────┼──────┼──────────┼──────┼───────┼───────────┼─────┤│
│ │1 │NM20260530-│아차  │ ●중대    │3공정 │김작업 │05-30 14:20│접수 ││
│ │  │003        │사고  │          │      │       │           │     ││
│ └──┴───────────┴──────┴──────────┴──────┴───────┴───────────┴─────┘│
│            (행 더블클릭 → NearMissInfo 팝업)                        │
└────────────────────────────────────────────────────────────────────┘
```

### 컴포넌트 매핑
| 영역 | 컴포넌트 |
|---|---|
| 헤더/조회버튼 | `ViewHeader` (@search) |
| 사업장 검색 | native input + `SiteSearchPop`(useModal) — Risk_03 동일 |
| 필터 셀렉트 | native select (Risk_03 동일, 코드 v-for) |
| 정렬 헤더 | `ThSortable` + `useTableSort`/`useColumnResize` |
| 상태탭 | 자체 버튼 그룹(class 기반, count 배지) |
| 상세 팝업 | `NearMissInfo`(useModal `open`) |

### 상태별 동작
- loading: 조회 중 그리드 영역 비움(기존 패턴엔 별도 스피너 없음 → 빈 tbody 유지).
- empty: `등록된 사건이 없습니다.` colspan row.
- error: `proxy.$alert(resolveApiErrorMessage)`.
- success: 목록 렌더 + 상태탭 카운트 갱신.

### 사용자 플로우
진입(LNB 사건관리>사건 관리) → 사업장 세션 자동세팅 → 상태탭/필터 선택 → 조회 → 행 더블클릭 → NearMissInfo 팝업 → 저장/상태전환 후 onSave 콜백으로 목록 새로고침.

### 권한 게이팅
- 진입 자체가 `tb_syst_auth_menu` USE_YN='Y'인 직군만(hr 차단). 저장 버튼은 BTN_SAVE 기준(팝업 측).

### 백엔드 의존
- GET `/webApi/nearmiss01/incident-lists` (PRAFTA-040-2 E1)
- GET `/webApi/nearmiss01/status-counts` (E3)
- 코드: `/comApi/baseinfo/syst-info-lists` (SYS061/SYS062/SYS063), `/comApi/baseinfo/base-info-lists`(COM002)

---

## UI-NM-02 — NearMissInfo (정밀조사 상세 팝업, 설계 5-C)

- 연결 작업: PRAFTA-040-4
- 화면 위치: `src/views/nearMiss/popup/NearMissInfo.vue`
- 현재 동작: 신규 작성
- 참조 패턴: RiskAssessInfo.vue (modal-overlay/modal-content-wide + useDraggable + 2단 risk-assess-content + modal-footer)

### 레이아웃 (와이어프레임, 설계 5-C)
```
┌─ modal-header (drag)  "사건 정밀조사"                    [X] ─┐
│ ┌── 좌: 보고 내용(읽기) ──┬── 우: 조사 / 조치 ──────────────┐ │
│ │ 사건ID  NM...-003       │ 추정 원인  [textarea]            │ │
│ │ 유형    아차사고         │ 재발방지 대책 [textarea]         │ │
│ │ 발생    2026-05-30 14:20 │ 임시조치   [textarea]            │ │
│ │ 장소    3공정 컨베이어   │ 처리상태   [select 100~400/900] │ │
│ │ 경위    [읽기 textarea]  │   (반려 선택 시 사유 textarea)   │ │
│ │ 잠재중대성  ●중대(배지)  │ ─────────────────────────────── │ │
│ │ 사진    [img preview]    │ ▣ 수시 위험성평가 생성·연계       │ │
│ │ 보고자  김작업/14:22     │   [위험성평가 생성](TODO)        │ │
│ │ 즉시조치 후진경보 점검   │                                  │ │
│ │ 출처   (앱 직접보고)     │                                  │ │
│ │        또는 RA...-011    │                                  │ │
│ └─────────────────────────┴──────────────────────────────────┘ │
├─ modal-footer ───────────────────────────────────────────────── ┤
│ [좌: 산재 보고 대상 안내(경미사고시)]  [취소][저장][완료처리][반려]│
└──────────────────────────────────────────────────────────────────┘
```

### 컴포넌트 매핑
| 영역 | 구현 |
|---|---|
| 드래그 모달 | `useDraggable` + modal-overlay/modal-content-wide (RiskAssessInfo 동일) |
| 읽기 필드 | native input/textarea readonly (form-row) |
| 잠재중대성 배지 | class 기반 배지(severity-low/medium/high), 점수 아님 |
| 사진 | img preview (RiskAssessInfo beforePhotoUrl 패턴) |
| 처리상태 | native select (SYS063, 상태전이 규칙 옵션 필터) |
| 위험성평가 생성 | 버튼(TODO) — E6/연계는 developer |
| footer 버튼 | btn-cancel/btn-save/btn-report (RiskAssessInfo 스타일) |

### 상태별 동작
- 상태전이 규칙(설계 §4): 현재 reportStatusCd 기준 선택 가능한 다음 상태만 select 노출(100→200, 200→300, 300→400; 어느 단계든 900 반려).
- 완료(400)·반려(900) 건은 입력 readonly(저장 버튼 숨김), 조회만.
- 반려 선택 시 사유 textarea 필수(validation: 빈값 차단 — UI 토글/필수체크만, 비즈니스 분기는 developer).
- error: `proxy.$alert`.

### 사용자 플로우
목록에서 진입 → 좌측 보고내용 확인 → 우측 원인/재발방지 입력 → 처리상태 전환(저장) → (선택)위험성평가 생성 → 완료처리/반려.

### 백엔드 의존
- GET `/webApi/nearmiss01/incident-info` (E2)
- POST `/webApi/nearmiss01/save-incident` (E4)
- POST `/webApi/nearmiss01/change-status` (E5)
- 코드: SYS063(처리상태)

---

## UI-NM-03 — RiskAssessInfo "아차사고로 전환" 액션 (설계 4-B, 보완)

- 연결 작업: PRAFTA-040-5
- 화면 위치: `src/views/risk/popup/RiskAssessInfo.vue` (기존 화면 보완)
- 현재 동작: 위험성평가 정보 2단 모달(개선 전/후), footer 좌측 [개선실행계획서]/[개선완료보고서], 우측 [취소][저장].
- 의도된 동작(변경): footer 좌측에 **[아차사고로 전환]** 버튼 추가. 클릭 시 confirm 후 E6(`reclassify-from-assessment`) 호출 자리(골격 TODO). 성공 시 원 평가건 이관 처리 + 팝업 닫기 + 목록 새로고침.

### 변경 범위 (최소)
- template: `footer-buttons-left`에 버튼 1개 추가(`v-if`로 미완료 상태에서만 노출 권장 — developer 확정).
- script: `// TODO(developer):` 전환 핸들러(E6 호출 + 원 tb_risk_assessment 이관[D2 미확정] + onSave/close).
- style: 기존 `.btn-report` 토큰 재사용(신규 색상 없음).

### 백엔드 의존
- POST `/webApi/nearmiss01/reclassify-from-assessment` (E6) — SRC_PROCESS_CD/SRC_ASSESSMENT_CD에 원 평가건 키 기록.

### 엣지/주의
- 이미 다른 사건으로 전환된 평가건 중복 전환 방지(서버 검증, developer).
- 원 평가건 이관처리 방식(D2) 확정 전까지 버튼은 추가하되 핸들러는 TODO.
