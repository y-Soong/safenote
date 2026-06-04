# prafta-app-018-C — 작업 분해 (앱 FE 연차 신청 폼 + 진입점)

분해자: planner. 상위: `prafta-app-018-leave-apply-plan.md` / 단위 요청서: `prafta-app-018-C-leave-form.md`.
선행: **018-A·018-B 구현 완료**(BE 계약 확정). 본 단위는 앱 프론트(`prafta-app-frontend`)만. 화면 골격(template+style)은 planner 가 작성 완료, script(메타 조회/제출/시각계산/결재선 전개)는 developer.

운영 규칙: 재개불가 — 자율진행(Write 완료). Notion 금지(메인 세션 대행). 화면 규약: CSS 변수만·공통 컴포넌트 우선·TS 금지·`<style scoped>`.

## 정책 출처 (정독 결과)
- attd §8 / §8.5(연차·사용단위·휴게 가로지름) / §9 / §9.5(결재·자기승인) — `.claude/context/policies/attd/08-leave.md`
- prafta-019(시간차/결재라인·LeaveFlow), prafta-020(결재선 프리셋), prafta-024(USAGE_UNIT 단일화)
- 상위 확정: D1 결재선(신청자 구성·INSERT), D2 단위 게이팅(허용단위만 노출/제출), D2-a (Y) 계층형(설정=허용 최소 단위), D3 잔여검증(제출 시 서버), D4 진입점 2곳 동일 폼.
- D2-a 종일/반차의 시간차 표현 + **종일/반차 편의버튼**(시각 자동입력, 제출은 단위/시각): 상위 §D2-a.

## 기존 패턴 정독 결과 (재사용 근거)
- **라우트 컨테이너 패턴**: `views/req/AttdRequestView.vue` — 헤더 + 본문 스크롤 + 폼 컴포넌트 분기 + sessionStorage 컨텍스트(1회 읽고 제거) + API 제출(`api.post` + `resolveApiErrorMessage`) + **디자인 토큰 세트를 루트 클래스에 1회 선언**(앱 FE 는 전역 토큰 파일이 아니라 화면 루트가 토큰을 직접 선언). 본 작업은 `LeaveApplyView` 가 동일 토큰 세트를 `.leave-apply-view` 루트에 선언.
- **프레젠테이션 폼 패턴**: `OvertimeForm.vue`/`AttdCorrectionForm.vue` — `props.context`/`props.submitting` + `emit('submit'|'cancel')`, `.ctx`/`.fs`/`.field`/`.form-ft`(sticky) 클래스 + `DateStepperField`/`TimeStepperField` 공통 컴포넌트. 폼 자신은 API/라우팅 미보유.
- **결재자 시트 패턴**: `views/mypage/components/PresetApproverPickerSheet.vue` — transition+dimmer+검색 input+다중 체크+푸터. 차이: 본 작업은 페이징(018-A approver-search hasNext) 추가 → `LeaveApproverPickerSheet` 신규.
- **공통 컴포넌트**: `DateStepperField`(v-model 'YYYY-MM-DD'), `TimeStepperField`(v-model 'HH:MM'). native `<input type=date/time>` 직접 사용 금지(이미 휠 시트로 대체됨) → 그대로 사용.
- **CSS 변수**: 화면 루트가 선언하는 토큰(`--color-primary`/`--color-surface`/`--color-text-*`/`--color-border`/`--space-*`/`--radius-*`/`--color-*-tint`/`--color-danger`/`--color-warning-*`). 골격은 이 토큰만 사용(하드코딩 없음). 시트는 body teleport 가능성 대비 `var(--x, #fallback)` 패턴(PresetApproverPickerSheet 동일).
- **진입/제출 컨텍스트 전달**: `MyAttendanceView.navigateToAttdRequest` — sessionStorage 저장 후 `router.push`. 폼 라우트가 onMounted 에서 읽고 즉시 제거. 본 작업도 동형(키 `leave_apply_ctx_v1`).

---

# 화면 명세

## UI-018C-1 LeaveApplyView (+ LeaveApplyForm, LeaveApproverPickerSheet)
- 연결 작업: prafta-app-018-C-01 / -02 / -03
- 화면 위치(신규):
  - `src/views/leave/LeaveApplyView.vue` — 라우트 컨테이너(토큰 루트 + 메타/제출 + 라우팅)
  - `src/views/leave/components/LeaveApplyForm.vue` — 입력/표시/검증 폼
  - `src/views/leave/components/LeaveApproverPickerSheet.vue` — 결재자 추가 바텀시트
- 참조 패턴: AttdRequestView(컨테이너) / OvertimeForm·AttdCorrectionForm(폼) / PresetApproverPickerSheet(시트)
- 라우트: `/LeaveApply` (연차현황 진입, 일자 컨텍스트 없음) · `/LeaveApply?workYmd=YYYYMMDD&nodeCd=N001` (내 근태 액션시트 진입). 라우터 등록 완료(`router/index.js`, publicPaths 미포함 → 토큰 게이트).

### 레이아웃 와이어프레임
```
┌──────────────────────────────────────┐
│ ‹  연차 신청                          │  ← lav-hd (sticky)
├──────────────────────────────────────┤
│ [컨텍스트 박스]  (일자 진입 시만)     │  ← ctx (workYmd/site/scheduleSummary)
│  2026년 6월 3일 · ○○사업장            │
│  스케줄  주간 09:00~18:00             │
│                                       │
│ 연차 종류                             │  ← type-list (allowedUnits/balance/applicable)
│  ┌ 연차          잔여 12일 ┐ (선택)   │
│  ┌ 보건휴가(반차)  잔여 0일 ┐ (disabled)│
│                                       │
│ ┌선택한 연차 잔여        12일┐        │  ← balance-box
│                                       │
│ 사용 단위  [종일][반차][2시간][1시간] │  ← unit-list (서버 allowedUnits 만)
│                                       │
│ 신청 일자  [ 날짜 선택 ▾ ]            │  ← DateStepperField
│                                       │
│ 신청 시간            [종일][반차]      │  ← (시간차 단위일 때만) 편의버튼
│  시작 [ 09:00 ▾ ]  종료 [ 11:00 ▾ ]   │  ← TimeStepperField × 2
│  · 1시간 단위로 신청…휴게 가로지름 불가│
│                                       │
│ 신청 사유 [____________] 0/500        │  ← textarea
│                                       │
│ 결재선  (aprvRequired 종류만)         │  ← preset-list + aprv-list + btn-add
│  [기본결재선*] [팀장전결]             │
│  ① 홍길동  영업1팀·과장   ✕          │
│  ② 김부장  영업본부·부장   ✕          │
│  [+ 결재자 추가]  → LeaveApproverPickerSheet
│                                       │
│ ⚠ 신청 일수가 남은 연차보다 많아요…   │  ← overBalanceWarning(선택)
│ · 신청 후 결재선의 승인을 거쳐…       │  ← helper
├──────────────────────────────────────┤
│ [ 취소 ]   [   신청하기   ]           │  ← form-ft (sticky)
└──────────────────────────────────────┘
```

### 컴포넌트 매핑
| 영역 | 컴포넌트/요소 | 비고 |
|---|---|---|
| 라우트 컨테이너/헤더/토큰 | `LeaveApplyView.vue` | 토큰 루트 1회 선언, 메타/제출/라우팅 보유 |
| 날짜 입력 | `DateStepperField`(공통) | v-model 'YYYY-MM-DD' |
| 시각 입력(시간차) | `TimeStepperField`(공통) × 2 | v-model 'HH:MM' |
| 연차 종류/단위/사유/결재선 | `LeaveApplyForm.vue` 내부 요소 | 종류=버튼리스트, 단위=칩, 사유=textarea |
| 결재자 추가 시트 | `LeaveApproverPickerSheet.vue`(신규) | 검색+다중체크+페이징, approver-search |
| Alert | `proxy.$alert` 폴백(window.alert) | 기존 폼 동일 패턴 |

### 상태별 동작
- **loading**: 컨테이너 `isLoadingMeta=true` → "불러오는 중..." (폼 미렌더).
- **error(메타 조회 실패)**: `metaError` 표시 + [다시 시도](loadMeta 재호출).
- **empty(신청 가능 종류 0)**: 폼 종류 리스트에 "신청 가능한 연차 종류가 없어요". (applicable=false 종류는 목록 유지·disabled.)
- **success(제출 완료)**: showAlert 후 `router.back()`(연차현황/근태 화면 복귀 → 현황 재마운트로 갱신).

### 사용자 플로우
1. 진입(A: 내 근태 액션시트 leave / B: 연차현황 "연차 신청하기") → `/LeaveApply`.
2. 컨테이너가 018-A `apply-meta`+`approval-presets` 조회 → 폼 렌더.
3. 연차 종류 선택 → 그 종류의 allowedUnits/balanceDays/aprvRequired 로 단위 칩/결재선 영역 재구성.
4. 단위 선택(시간차면 시작~종료 시각 입력, 종일/반차 편의버튼으로 시각 자동입력) → 날짜/사유 입력.
5. (aprvRequired) 프리셋 선택 또는 결재자 추가(시트, approver-search) → 결재선 구성.
6. 신청하기 → 018-B `POST /apply` → 성공 안내 → 이전 화면 복귀. 서버 에러 메시지 표면화.

### 백엔드 의존 (작업 ID 연결)
- `GET /appApi/leaveflow/apply-meta` (018-A-01) → `meta.leaveTypes[]`
- `GET /appApi/leaveflow/approval-presets` (018-A-02) → `presets[]`
- `GET /appApi/leaveflow/approver-search?keyword=&page=&size=` (018-A-03) → 시트 후보
- `POST /appApi/leaveflow/apply` (018-B-01) → 제출

### 반응형
- 모바일 webview 단일 컬럼(앱 FE 표준). 별도 break point 없음(기존 폼 동일). sticky 헤더/푸터 + 본문 스크롤.

---

# 작업 분해 결과

## prafta-app-018-C-01
- **유형**: frontend-screen
- **영역**: app
- **모듈**: leave (신규 화면 `views/leave/LeaveApplyView.vue`)
- **작업 유형**: 신규
- **요구사항 요약**: 연차 신청 라우트 컨테이너 — 018-A 메타/프리셋 조회 + 018-B 제출 + 라우팅. 토큰 루트 선언.
- **상세 설명**:
  - [frontend-screen] 핵심 요구사항:
    1) onMounted: (선택) sessionStorage 컨텍스트 로드(1회 읽고 제거, workYmd 정합 검사) + 018-A 메타/프리셋 병렬 조회.
    2) 로딩/에러/폼 상태 분기 렌더. 에러 시 재시도.
    3) 폼 `submit` 수신 → 018-B `POST /apply`(payload 그대로, 식별값/nodeCd 서버 JWT). 성공 안내+`router.back()`. 실패 `resolveApiErrorMessage` 표면화.
  - 영향 받는 파일(신규): `src/views/leave/LeaveApplyView.vue` (골격 작성 완료, script 채움)
  - 영향 endpoint: GET apply-meta / GET approval-presets / POST apply
  - 예상 산출물: view(컨테이너) script 본문
  - 연결 UI 명세: UI-018C-1
- **선행 작업**: prafta-app-018-A, prafta-app-018-B
- **우선순위 근거**: 법적 책임 영역(attd 연차) +1단계; 진입점 2곳의 라우팅 대상.

## prafta-app-018-C-02
- **유형**: frontend-component
- **영역**: app
- **모듈**: leave (신규 `views/leave/components/LeaveApplyForm.vue`)
- **작업 유형**: 신규
- **요구사항 요약**: 연차 신청 입력/표시/검증 폼 — 종류 선택, 단위 게이팅 드롭다운, 날짜/시간차 시각, 종일·반차 편의버튼, 사유, 결재선 구성, 잔여 표시.
- **상세 설명**:
  - [frontend-component] 핵심 요구사항:
    1) 종류 선택 시 allowedUnits/balanceDays/aprvRequired 로 동적 구성. 종류 변경 시 단위/시각/결재선 재초기화.
    2) 단위 칩은 선택 종류 allowedUnits(서버)만. 시간차(02·03·04)면 시각 입력 노출 + 안내.
    3) 종일/반차 편의버튼: 시각 자동입력(시작=스케줄시작, 종일=스케줄종료/반차=절반). 제출은 단위/시각.
    4) 결재선: aprvRequired 종류만. 프리셋 전개 + 결재자 추가/제거/순서(식별자 기준, 위치 재인덱싱 금지).
    5) 제출 payload 조립(018-B 키 1:1) + emit('submit'). isValid 1차 차단.
  - 영향 받는 파일(신규): `src/views/leave/components/LeaveApplyForm.vue` (골격 완료, script 채움)
  - 예상 산출물: 폼 컴포넌트 script 본문
  - 연결 UI 명세: UI-018C-1
- **선행 작업**: prafta-app-018-A, prafta-app-018-B
- **우선순위 근거**: 법적 책임 영역 +1; C-01 컨테이너가 props 로 주입.

## prafta-app-018-C-03
- **유형**: frontend-component
- **영역**: app
- **모듈**: leave (신규 `views/leave/components/LeaveApproverPickerSheet.vue`)
- **작업 유형**: 신규
- **요구사항 요약**: 결재자 추가 바텀시트 — 018-A approver-search(검색+페이징) 호출, 다중 선택 후 emit('add').
- **상세 설명**:
  - [frontend-component] 핵심 요구사항:
    1) 시트 오픈 시 keyword/page/checkedSet 초기화 + 첫 페이지 조회.
    2) 검색(enter)·더보기(hasNext) → approver-search. 누적 append(중복 userCd 제외).
    3) excludedUserCds(이미 추가된 결재자) disabled. 선택 결과 emit('add', picked[]).
  - 영향 받는 파일(신규): `src/views/leave/components/LeaveApproverPickerSheet.vue` (골격 완료, script 채움)
  - 영향 endpoint: GET approver-search
  - 예상 산출물: 시트 컴포넌트 script 본문
  - 연결 UI 명세: UI-018C-1
- **선행 작업**: prafta-app-018-A
- **우선순위 근거**: 결재선 구성(D1) 데이터 입력 UI.

## prafta-app-018-C-04
- **유형**: frontend-screen
- **영역**: app
- **모듈**: attd / leave (진입점 2곳 수정)
- **작업 유형**: 보완
- **요구사항 요약**: 두 진입점의 "준비 중" stub 제거 → `/LeaveApply` 라우팅(연차 한정).
- **상세 설명**:
  - [frontend-screen] 핵심 요구사항(정확한 diff 지침은 아래 §진입점 수정):
    1) `MyAttendanceView.onSheetAction` 의 `leave` 분기(약 738~739행) stub 제거 → 컨텍스트 sessionStorage 저장 후 `/LeaveApply` push(보정/초과근무 패턴).
    2) `MyLeaveSummaryView.onApply`(약 177~181행) stub 제거 → `/LeaveApply` push(일자 컨텍스트 없음).
  - 영향 받는 파일: `src/views/attd/MyAttendanceView.vue`, `src/views/leave/MyLeaveSummaryView.vue`
  - 예상 산출물: 두 핸들러 본문 교체(라우팅)
- **선행 작업**: prafta-app-018-C-01 (라우트 존재)
- **우선순위 근거**: "준비 중" 잔존 제거(수용 기준). 라우트 등록 후 가능.

---

# §진입점 수정 (developer diff 지침 — 정확한 위치/방식)

> 라우트 `/LeaveApply` 는 planner 가 `router/index.js` 에 등록 완료(MyLeaveSummaryView 블록 다음). developer 는 아래 두 핸들러 본문만 교체한다. **다른 분기/stub 은 건드리지 않는다**(`onTodayAction`/`onDayDetailAction` 의 "준비 중"은 연차 무관 — 유지).

## 1) MyAttendanceView.vue — onSheetAction 의 leave 분기
- 위치: `onSheetAction`(약 725~740행). 현재 leave 분기는 함수 말미 `showAlert('준비 중입니다')`(약 739행).
- 컨텍스트 빌더는 **기존 `buildContextFromDay(day)`(673~686행) 재사용**(workYmd/nodeCd/siteName/scheduleSummary/workPlanName/slots 포함 — 폼 컨텍스트와 정합). 단 sessionStorage 키는 **연차 전용 `leave_apply_ctx_v1`**(근태요청 키 `attd_req_ctx_v1` 와 분리, 폼 컨테이너가 그 키로 읽음).
- 교체 방식(예시 — developer 가 기존 `navigateToAttdRequest` 패턴을 미러한 `navigateToLeaveApply` 신규 헬퍼 또는 인라인):
```js
// payload.type: scheduleModify | attendanceCorrection | overtime | leave
const onSheetAction = (payload) => {
  actionSheetOpen.value = false
  const type = payload?.type
  const day = payload?.day || actionSheetDay.value
  if (type === 'scheduleModify') return navigateToAttdRequest('schedModify', day)
  if (type === 'attendanceCorrection') return navigateToAttdRequest('attdCorrection', day)
  if (type === 'overtime') return navigateToAttdRequest('overtime', day)
  if (type === 'leave') {
    // prafta-app-018-C: 연차 신청 폼 라우팅. day 컨텍스트(workYmd/nodeCd/siteName/schedule) 전달.
    if (!day || !day.workYmd) { showAlert('대상 일자를 확인할 수 없습니다.'); return }
    try {
      sessionStorage.setItem('leave_apply_ctx_v1', JSON.stringify(buildContextFromDay(day)))
    } catch (e) {
      console.error('[MyAttendance] 연차 컨텍스트 저장 실패:', e?.message)
      showAlert('컨텍스트 저장에 실패했습니다.'); return
    }
    return router.push({ path: '/LeaveApply', query: { workYmd: day.workYmd, nodeCd: day.nodeCd || '' } })
  }
}
```
- ⚠️ 기존 leave stub `showAlert('준비 중입니다')` 한 줄만 위 분기로 치환. `router`/`buildContextFromDay`/`showAlert` 는 이미 import/정의됨(추가 import 불요).

## 2) MyLeaveSummaryView.vue — onApply
- 위치: `onApply`(약 177~181행). 현재 `showAlert('준비 중입니다')`.
- 이 진입은 **특정 일자 컨텍스트 없음** → sessionStorage 미저장, 쿼리 없이 push(폼에서 날짜 직접 선택).
```js
const onApply = () => {
  // prafta-app-018-C: 연차 신청 폼. 일자 컨텍스트 없음 → 폼에서 날짜 직접 선택.
  router.push('/LeaveApply')
}
```
- ⚠️ `canApply`(잔여>0) disabled 가드는 현행 유지(버튼 비활성). `router` 이미 정의됨(109행).

---

# §developer script 채울 항목 (컴포넌트별)

## LeaveApplyView.vue (C-01)
| 항목 | 내용 |
|---|---|
| `loadMeta()` | `Promise.all([api.get('/appApi/leaveflow/apply-meta'), api.get('/appApi/leaveflow/approval-presets')])` → `meta.value = metaRes.data \|\| {leaveTypes:[]}`, `presets.value = presetRes.data?.presets \|\| []`. 실패 시 `metaError`. finally `isLoadingMeta=false`. (401/403/500 인터셉터) |
| `onSubmit(payload)` | `api.post('/appApi/leaveflow/apply', payload)` → 성공 showAlert+`router.back()`. 실패 `resolveApiErrorMessage`. `isSubmitting` 가드. workYmd 폴백은 폼에서 처리되므로 payload 그대로 전송(아래 폼 §제출 참조). |
| onMounted | sessionStorage `leave_apply_ctx_v1` 로드(있으면)+제거+workYmd 정합, 이후 `loadMeta()`. (골격에 구현됨 — 확인만) |

## LeaveApplyForm.vue (C-02) — script 계약

### 상태(골격 선언됨, 값/리셋 보완)
`selectedLeaveCd`(ref ''), `useUnitType`(ref '', SYS025), `workDateInput`(ref '' 'YYYY-MM-DD'), `startTimeInput`/`endTimeInput`(ref '' 'HH:MM'), `reason`(ref ''), `selectedPresetId`(ref ''), `approverList`(ref [] — `[{approverUserCd,userNm,userId,rankNm,nodeNm}]` 순서=단계), `approverPickerOpen`(ref false).

### computed(골격 구현됨 — 변경 금지 권장)
`leaveTypes`, `selectedType`, `aprvRequired`, `unitOptions`(allowedUnits→{code,label}), `isTimeUnit`(02/03/04), `canQuickFullDay/HalfDay`(allowedUnits 포함 여부), `approverUserCds`(approverList→userCd 배열, **순서 보존**), `isValid`(필수입력만), `overBalanceWarning`(현재 placeholder=false → 아래 보완).

### developer 가 채울 메서드
| 메서드 | 시그니처/동작 |
|---|---|
| `onSelectType(lt)` | `selectedLeaveCd=lt.leaveCd`. **종류 변경 시 재초기화**: `useUnitType=''`(또는 allowedUnits[0] 자동선택 정책 확정), `startTimeInput/endTimeInput=''`, `selectedPresetId=''`, `approverList=[]`. |
| `onSelectUnit(code)` | `useUnitType=code`. 시간차→종일/반차 전환 시 `startTimeInput/endTimeInput` 비우기(종일/반차는 시각 미제출). |
| `onQuickFill(unitCode)` | 편의버튼('00'/'01'): `useUnitType=unitCode` 세팅 + 시각 자동입력. 시작=스케줄시작(`context.slots[*].schedule.startTime` HHMM→'HH:MM'), 종일=스케줄종료, 반차=시작+소정근로/2(절반). **표시·BE 차감용**이며 제출 useUnitType 은 unitCode. 스케줄 출처 없으면 시각 비움(서버 차감). |
| `onSelectPreset(preset)` | `selectedPresetId=preset.presetId`. `approverList = preset.steps.map(s=>({approverUserCd:s.approverUserCd,userNm:s.userNm,userId:s.userId,rankNm:s.rankNm,nodeNm:s.nodeNm}))` (**STEP_NO/배열 순서 보존**). 재선택 토글 정책 developer 확정. |
| `onAddApprovers(picked)` | 시트 emit('add') 수신. `picked[{userCd,userId,userNm,rankNm,nodeNm}]` → `approverList` 에 `{approverUserCd:userCd,...}` 형태로 append. **userCd 식별자 dedup**(이미 있으면 skip). `approverPickerOpen=false`. 직접 추가 시 `selectedPresetId=''`(프리셋 이탈) 정책 권고. |
| `onRemoveApprover(approverUserCd)` | `approverList = approverList.filter(a=>a.approverUserCd!==approverUserCd)`. **위치 index 재인덱싱 금지**(식별자 필터). |
| `onOpenApproverPicker()` | 골격 구현됨(`approverPickerOpen=true`). |
| `overBalanceWarning` 보완 | 신청 일수 추정(종일 1.0 / 반차 0.5 / 시간차 `(end-start)분 / 소정근로분`, 휴게 제외 근사) > `selectedType.balanceDays` 면 true. **표시 전용**(서버 051 최종). 계산 불가 시 false. |
| `onSubmit()` | payload 조립 후 `emit('submit', payload)`. **isValid 1차 가드**(computed). |

### §제출 payload 계약 (018-B 요청 본문 1:1 — 키/타입 엄수)
```js
emit('submit', {
  leaveCd: selectedLeaveCd.value,            // String, 필수
  leaveType: selectedType.value?.leaveType,  // String|null. meta LeaveTypeItem 에 leaveType 키 없음 →
                                             //   018-B 는 leaveType 선택(@Size max10, 미전송 시 null 저장).
                                             //   ⚠️ 018-A apply-meta 응답에 leaveType(성격코드) 없음 → 폼이 보낼 값 없음 →
                                             //   기본 생략(null). 필요 시 developer 가 meta 에 leaveType 추가 요청(별도) 또는 미전송.
  workYmd: toYmd(workDateInput.value),       // 'YYYY-MM-DD' → 'YYYYMMDD'. 컨텍스트 진입이어도 폼 날짜가 SSOT.
  useUnitType: useUnitType.value,            // SYS025 '00'~'04'
  startTime: isTimeUnit.value ? toHHMM(startTimeInput.value) : null,  // 시간차만. 'HH:MM'→'HHMM'. 종일/반차 null.
  endTime:   isTimeUnit.value ? toHHMM(endTimeInput.value)   : null,
  reason: reason.value.trim(),               // ''면 null/생략 정책 developer(서버 @Size max500, 선택)
  approverUserCds: aprvRequired.value ? approverUserCds.value : undefined,  // 결재 불필요면 미전송
  presetId: undefined,                       // §결재선 제출 규칙 참조(아래)
})
```
- **§결재선 제출 규칙(018-B 결정 2 정합)**: 018-B 는 `approverUserCds` 1차, `presetId` 보조(둘 다 오면 approverUserCds 채택). **폼은 항상 `approverUserCds`(전개된 최종 순서)를 보낸다** → 프리셋을 골랐어도 폼이 `approverList`(step 전개)를 보유하므로 approverUserCds 로 충분. `presetId` 는 생략(undefined) 권장. (프리셋 미전개 채로 서버 전개를 위임하려면 presetId 만 보내고 approverUserCds 생략 — 단 본 폼은 전개 보유하므로 비권장.)
- ⚠️ **leaveType 갭(보고 대상)**: 018-A `LeaveTypeItem` 에 `leaveType`(성격코드) 필드가 **없다**(leaveCd/leaveNm/systemYn/aprvRequired/allowedUnits/balanceDays/applicable). 018-B `LeaveApplyRequest.leaveType` 는 선택(미전송 시 null 저장, 웹 미러)이므로 **폼은 leaveType 을 생략(null)** 해도 제출 가능. developer 는 leaveType 을 보내지 말 것(추측 금지). 표시·분류용 leaveType 이 필요하면 별도 018-A 보강(planner 재분해) — 본 단위 범위 밖.

### 형식 유틸(기존 폼에서 차용 — developer 추가)
`toYmd(s)`= `s.replace(/-/g,'')`, `toHHMM(s)`= `s.replace(':','').slice(0,4)` (OvertimeForm `inputToYmd`/`timeToHhmm` 동일). 골격엔 미포함 → developer 추가.

## LeaveApproverPickerSheet.vue (C-03) — script 계약
| 항목 | 내용 |
|---|---|
| `onSearch()` | `page.value=0` 리셋 + `GET /appApi/leaveflow/approver-search?keyword={keyword}&page=0&size=20`. 응답 `{approvers, hasNext}` → `candidates.value = approvers`, `hasNext.value = hasNext`. |
| `onLoadMore()` | `page.value+=1` + 동일 호출(page=page.value). `candidates` 에 append(**userCd dedup**), `hasNext` 갱신. |
| `watch(modelValue)` | 오픈 시 `keyword=''`,`page=0`,`candidates=[]`,`checkedSet=new Set()` 초기화 + `onSearch()`(첫 페이지). |
| `loadCandidates` 공통화 | 위 두 메서드 내부 호출 공통 helper 권고(isLoading 토글, 에러 시 showAlert). |
| import | `api` from `@/api/axios`. (골격에 주석 처리됨 — 활성화) |
- 응답 키: `approvers[].{userCd,userId,userNm,rankNm,nodeNm}` (018-A-03 `ApproverItem` 정합). PII 추가 노출 금지.

---

# §A·B 응답 키 매핑 (FE 소비 — 그대로 사용, 추측 금지)

| FE 소비 위치 | 키 | 출처 |
|---|---|---|
| 종류 리스트 | `meta.leaveTypes[].{leaveCd,leaveNm,systemYn,aprvRequired,allowedUnits[],balanceDays,applicable}` | 018-A-01 `LeaveApplyMetaResponse.LeaveTypeItem` |
| 단위 칩 | `selectedType.allowedUnits[]` (SYS025 '00'~'04' 부분집합, 계층) | 018-A-01 (D2-a) |
| 잔여 표시 | `selectedType.balanceDays`(double) | 018-A-01 |
| 결재 영역 노출 | `selectedType.aprvRequired`(boolean) | 018-A-01 |
| 종류 비활성 | `selectedType.applicable`(boolean, false→disabled) | 018-A-01 |
| 프리셋 칩/전개 | `presets[].{presetId,presetNm,defaultYn,steps[].{stepNo,approverUserCd,userNm,userId,rankNm,nodeNm}}` | 018-A-02 `ApprovalPresetListResponse` |
| 결재자 검색 | `approvers[].{userCd,userId,userNm,rankNm,nodeNm}` + `hasNext` | 018-A-03 `ApproverSearchResponse` |
| 제출 본문 | `{leaveCd,leaveType?,workYmd,useUnitType,startTime?,endTime?,reason?,approverUserCds?,presetId?}` | 018-B `LeaveApplyRequest` |
| 서버 에러 | `ATTD_400_050/051/052/054/055/056/102`, `COMMON_400_001`, `ATTD_404_030` | 018-B §3 |

⚠️ **boolean is- 접두 함정 인지**: 018-A 응답이 record 라 `aprvRequired`/`applicable` 키 안전(메모리 feedback_lombok_jackson_boolean_is_prefix). 단 BE 가 record→@Builder POJO 로 바뀌면 `@JsonProperty` 필요 — FE 는 `aprvRequired`/`applicable` 키로 소비(현 계약). 불일치 시 보고.

---

# §수용 기준

## 공통
- [ ] 색상/폰트/간격 CSS 변수만(하드코딩 없음). 시트는 `var(--x,#fallback)` 패턴. `!important` 없음. `<style scoped>`.
- [ ] 공통 컴포넌트(DateStepperField/TimeStepperField) 사용. native date/time input 직접 사용 없음.
- [ ] TS 문법 없음. SFC 컴파일/eslint(앱 FE 설정) 통과.
- [ ] API 호출/라우팅은 컨테이너(C-01)·진입점(C-04)·시트(C-03)만. 폼(C-02)은 emit 만(직접 호출 없음).

## 기능
- [ ] 종류별 단위 칩이 다르게 열림(allowedUnits 서버). 메타에 없는 단위 미노출.
- [ ] 시간차(02/03/04)만 시각 입력 노출. 종일/반차는 시각 미노출·미제출.
- [ ] aprvRequired=false 종류는 결재선 영역 숨김(제출 시 approverUserCds 미전송).
- [ ] applicable=false 종류 disabled(목록 유지). 종류 0건 → "신청 가능한 연차 종류가 없어요".
- [ ] 두 진입점 모두 `/LeaveApply` 로 연결, "준비 중" 잔존 없음(연차 한정). 내 근태 진입은 일자 컨텍스트 동반, 연차현황 진입은 폼에서 날짜 선택.
- [ ] 종류 변경 시 단위/시각/결재선 재초기화.
- [ ] 결재자 추가/제거/순서가 식별자(userCd) 기준(위치 index 재인덱싱 금지). 프리셋 전개 STEP_NO 순서 보존.
- [ ] 제출 payload 키/타입이 018-B `LeaveApplyRequest` 와 정합(workYmd 8자리, startTime/endTime HHMM 또는 null, useUnitType SYS025).

## 엣지 (QA 스펙 가정 도전 — 골격 로직버그 사각 주의)
- [ ] **편의버튼 스케줄 출처 없음**(연차현황 진입=컨텍스트 없음, 또는 slots 미동봉): onQuickFill 이 시각을 못 채움 → 빈 시각으로 제출 시 시간차 단위는 isValid=false(시작/종료 필수)로 차단. 종일/반차 단위면 시각 무관(통과). 도전: 편의버튼이 시간차 단위에서만 의미 있는지(종일/반차 단위 선택 시 편의버튼 불필요) — 현 골격은 시간차 영역(`isTimeUnit`) 안에 편의버튼 배치 → **종일/반차가 allowedUnits 라도 시간차 단위를 고르지 않으면 편의버튼 안 보임**. 사용자 의도(시간차 휴가에서 종일/반차를 시간으로 표현)와 정합 확인.
- [ ] **잔여 초과 표시 vs 서버 거부**: overBalanceWarning(낙관적 표시)이 떠도 제출 차단 안 함(서버 051 최종). 반대로 경고 없이도 제출 시점 051 가능(applicable=true 낙관 표시) → 에러 메시지 표면화로 처리.
- [ ] **반차+시각 누수 방지**: useUnitType='01'(반차)인데 startTimeInput/endTimeInput 에 잔존 값 있어도 제출 startTime/endTime=null(isTimeUnit=false). 종류/단위 전환 시 시각 리셋 누락되면 누수 → onSelectType/onSelectUnit 리셋 확인.
- [ ] **프리셋 전개 후 직접 추가/제거**: 프리셋 골라 전개한 뒤 결재자 추가/삭제하면 selectedPresetId 무효(서버는 approverUserCds 채택) → 폼이 approverUserCds 를 SSOT 로 보내므로 정합. presetId 동시 전송 금지(중복 의미).
- [ ] **결재자 중복 userCd**: 시트 add/프리셋 전개에서 동일 userCd 중복 → dedup. (018-B 는 중복 허용=단계 증가지만, 앱 폼은 dedup 권장 — 정책 미정이면 보고.)
- [ ] **앱 인터셉터 토큰오류 오발동**: 제출 에러가 ATTD_400_* 면 인터셉터가 로그아웃 처리 안 함(안전). COMMON_400_003/600 만 토큰오류 처리 — 018-B 는 COMMON_400_001(결재선 누락) 사용(003 아님) → 안전. (메모리 project_prafta_app_req07_token_logout_bug.)
- [ ] **컨텍스트 stale**: sessionStorage 컨텍스트 workYmd 와 query workYmd 불일치 시 컨텍스트 폐기(빈 컨텍스트). 폼 날짜는 사용자 선택 SSOT.

## security 검토 위임 포인트
- approver-search PII 최소노출(userId 까지만, 휴대폰/이메일/생년 없음) — 018-A-03 정합 확인.
- 식별값(cmpny/site/user)·nodeCd 본문 비신뢰(서버 JWT) — 폼이 nodeCd 를 제출 본문에 넣지 않음 확인(query nodeCd 는 표시·미사용).
- 결재자 cross-site 주입(approverUserCds) — 018-B follow-up(웹 공통 갭). FE 는 approver-search(사업장 스코프) 산출만 추가하나 서버가 최종 검증.

---

# §planner 작성 산출물 (골격 — script 미완)
- `src/views/leave/LeaveApplyView.vue` — 컨테이너(template+style 완성, script 선언+TODO)
- `src/views/leave/components/LeaveApplyForm.vue` — 폼(template+style 완성, script 선언+TODO)
- `src/views/leave/components/LeaveApproverPickerSheet.vue` — 시트(template+style 완성, script 선언+TODO)
- `src/router/index.js` — `/LeaveApply` 라우트 등록(완료)

# §follow-up (본 단위 밖)
- leaveType(성격코드) 표시/제출 필요 시 018-A apply-meta 보강(현 미포함, planner 재분해).
- prafta-app-009 결재선 일반화(스케줄/보정/초과근무 폼) — 본 단위는 연차만.
- prafta-031 알림 outbox(신청/결재 통지) — 미포함.
