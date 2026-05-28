# PRAFTA-017-4 사용자정보 화면 UI 명세

연계 작업 요청서: `.claude/requests/prafta-017-4.md`
참고 시안: `.claude/requests/ref/prafta-017/01-user-info-popup.html`, `02-hire-date-edit-modal.html`
정책서 출처: 근태관리 §8.5.6(입사일 변경 처리 매트릭스), §8.5.7(권한 매핑), §8.5.8(멱등성/기 부여 보호), §8.1(연차 타입)

---

## UI-001 UserInfoPop (사용자정보 팝업) 보완

- 연결 작업: PRAFTA-017-4-3 (frontend-screen), PRAFTA-017-4-1 / 017-4-2 (backend)
- 화면 위치: `src/views/user/popup/UserInfoPop.vue` (기존 파일 보완 — developer가 직접 편집)
- 참조 패턴: 기존 UserInfoPop.vue (modal-content-narrow + form-row-max + 탈퇴 다이얼로그), 01-user-info-popup.html

### 권한 게이트 (핵심)

- 신규 "근태/연차 정보" 섹션, "경력 인정" 섹션, [입사일 수정] 버튼은 **master/hr 권한 사용자에게만** 노출/편집 가능.
- 프론트 기준: `sessionStorage.getItem('gv_authCd')` 가 `master` 또는 `hr` 일 때만 렌더.
- 비권한자: 기존 기본정보 영역만 노출(현행 100% 유지). 신규 섹션은 DOM에 그리지 않음(`v-if`).
- 백엔드는 신규 조회/저장 endpoint에서 `AuthRoleUtils.isManager(authCd)` 가드(이중 방어).

### 레이아웃 와이어프레임 (master/hr 권한 시)

```
+------------------------------------------------------+
| 사용자정보                                      [ X ] |  ← 기존 modal-header (드래그)
+------------------------------------------------------+
| [ 기본 정보 ]  (기존 영역 100% 유지)                  |
|   사용자  [ID(disabled)] [이름(disabled)]            |
|   권한    [BaseSelect COM005 + authLevel 필터]        |
|   휴대폰  [입력] [인증요청]                            |
|   인증번호 [입력] [확인] / 인증메시지                  |
|   이메일  [입력]                                      |
|   성별    [BaseSelect SYS004]                         |
|   사용여부 [BaseSelect SYS003]                        |
|   사업장  [입력] [찾기]                                |
|   소속부서 [입력] [찾기]                               |
|   생년월일 [입력]                                      |
|   (탈퇴예정일 행: withdrawalDate 있을 때만)            |
| - - - - - - - - - - - - - - - - - - - - - - - - - -  |
| [ 근태/연차 정보 ]   ← 신규, master/hr 전용           |
|   입사일  [2018-03-12 (readonly)] [입사일 수정 ✎]     |
|   ⓘ 입사일 변경은 연차 부여 등 노무 계산에 영향을 줍니다 |
| - - - - - - - - - - - - - - - - - - - - - - - - - -  |
| [ 경력 인정 ]        ← 신규, master/hr 전용           |
|   ┌ 인정 항목 #1                          [삭제 🗑] ┐ |
|   │  인정 개월 [ 60 ] 개월                          │ |
|   │  상세 설명 [ ............................. ]    │ |
|   └────────────────────────────────────────────────┘ |
|   ┌ 인정 항목 #2 ...                                 │ |
|   [ + 인정 항목 추가 ]                                |
|   ┌ 총 인정: 72개월(6년) · 법적 근속 기준일 2012.03.12┐|
+------------------------------------------------------+
|                       [비밀번호 초기화] [저장]        |  ← 기존 modal-footer 유지
+------------------------------------------------------+
```

### 컴포넌트 매핑

| 영역 | 컴포넌트/요소 | 비고 |
| --- | --- | --- |
| 기본 정보 전체 | 기존 그대로 | `form-row-max`, `BaseSelect`, native input(기존 컨벤션) — 변경 금지 |
| 근태/연차 정보 섹션 | `form-row-max` + native readonly input | 입사일은 표시 전용 |
| [입사일 수정] 버튼 | `button.btn.btn-sm` (또는 기존 `btn btn-primary`) | 클릭 시 `openPop(HireDateEditPop, ...)` |
| 경력 인정 항목 입력 | native `<input type="number">` + `<input type="text">` | 신규 공통 컴포넌트 없음 → 기존 폼 컨벤션의 native input 사용 |
| 항목 추가/삭제 | `button` | 추가 = push, 삭제 = splice |
| 저장/비번초기화 | 기존 footer 버튼 유지 | 저장 시 기존 update-user-infos + 경력인정 저장(분리 호출) |

### 상태별 동작

| 상태 | UI |
| --- | --- |
| loading | 신규 detail(`leave-info`) 조회 중에는 근태/연차·경력인정 영역 비표시 또는 스켈레톤. 기본정보는 기존 흐름대로 즉시 표시 |
| empty | 경력 인정 항목 0건 → 항목 없이 [+ 인정 항목 추가] 버튼만, 요약은 "총 인정 0개월" |
| error | 신규 detail 조회 실패 → 알럿. 기본정보 영역은 영향 없음(독립 호출) |
| success | 입사일/경력인정 정상 표시, 요약 자동 계산 |

### 사용자 플로우

1. 사용자 목록에서 행 선택 → UserInfoPop 진입 (기존 흐름).
2. `gv_authCd ∈ {master, hr}` 이면 근태/연차·경력 인정 섹션 추가 노출.
3. 입사일 옆 [입사일 수정] 클릭 → HireDateEditPop 중첩 오픈(`openPop`).
4. 경력 인정: [+ 인정 항목 추가]로 행 추가, 각 행에 인정 개월/상세 설명 입력, [🗑]로 삭제.
5. 요약 영역: 총 인정 개월 = SUM(인정 개월), N년 = floor(개월/12), 법적 근속 기준일 = HIRE_DATE - 총 개월(프론트 계산 또는 백엔드 detail 응답값 사용).
6. [저장] → 기존 update-user-infos + 경력인정 저장(update-user-credit, delete-and-insert). master/hr만.

### 백엔드 의존

- `GET /webApi/user01/{userCd}/leave-info` — 입사일/고용형태/경력인정 list/총개월/법적근속기준일 (신규, PRAFTA-017-4-1)
- `POST /webApi/user01/update-user-credit` — 경력 인정 delete-and-insert (신규, PRAFTA-017-4-1)
- 기존: `GET user-info-lists`, `POST update-user-infos`, `update-user-passwd`, `schedule-withdrawal`, `cancel-withdrawal` — 변경 없음

### 시안 대비 변경(요청서 §2-3)

- "신규 추가 영역" span 텍스트 삭제.
- "M&A, 경력직, 그룹사 이동 등" span 텍스트 삭제.
- 경력 인정 토글 스위치(`creditToggle`)는 본 화면에서 항상 활성 영역으로 두며 별도 on/off 스위치는 두지 않음(요청서에 토글 요구 없음, USE_YN='Y' 항목만 운영). developer는 토글 UI 미구현.

---

## UI-002 HireDateEditPop (입사일 수정 모달) 신규

- 연결 작업: PRAFTA-017-4-4 (frontend-screen), PRAFTA-017-4-2 (backend)
- 화면 위치: `src/views/user/popup/HireDateEditPop.vue` (신규 — planner 골격 작성 완료, developer가 script 채움)
- 참조 패턴: 기존 popup(modal-overlay + modal-header 드래그 + useCenteredDraggable + emit close), 02-hire-date-edit-modal.html
- 진입: UserInfoPop의 [입사일 수정] 버튼 → `openPop(HireDateEditPop, { ... })` 중첩 호출
  - 주의: `useModal()`은 인스턴스별 독립 app/container이므로 부모 UserInfoPop을 닫지 않음(SiteSearchPop와 동일 패턴).

### 레이아웃 와이어프레임

```
+----------------------------------------------------------+
| 입사일 수정                                         [ X ] |
+----------------------------------------------------------+
| 대상자: <김도현> · <소속부서> · <ID>                       |
| 입사일 변경은 연차 부여/4대보험/근속 계산에 영향...        |
|                                                          |
| ① 입사일 변경                                            |
|   기존 입사일 [____(readonly)]  →  변경할 입사일 [____*]  |
|   ⟳ 입사일 N일 앞당김 · 근속 ... → ... (요약, 옵션)        |
|                                                          |
| ② 영향 분석   [ 시나리오 태그 ]                          |
|   ┌ 기존 부여      ┐ ┌ 사용된 연차    ┐                  |
|   └ 월차X·본연차Y └ └ 0일           ┘                  |
|   ┌ 누락 부여(warn)┐ ┌ 다음 부여(ok) ┐                  |
|   └ ...           ┘ └ YYYY-MM-DD    ┘                  |
|   ※ 부여 엔진 미적용 → 근사치, 데이터 없으면 0           |
|                                                          |
| ③ 처리 방식 *                                            |
|   (◉) 기존 유지 + 누락분 소급 부여        [권장]         |
|   ( ) 기존 유지 + 신규만 새 입사일 계산                   |
|   ( ) 모든 부여 삭제 후 재계산            [위험]         |
|   ⚠ 참고: 유효기간은 발생일 기준 보존...                  |
|                                                          |
| ④ 변경 사유 *                                            |
|   [ textarea ........................................... ]|
|   ⛔ 4대보험/임금/퇴직금에도 영향. 단순 경력 인정은 ...   |
+----------------------------------------------------------+
|                                  [취소]  [변경 적용]      |
+----------------------------------------------------------+
```

### 컴포넌트 매핑

| 영역 | 컴포넌트/요소 | 비고 |
| --- | --- | --- |
| 모달 셸 | `modal-overlay` + `modal-content-normal`(width 90%, max 720px) | 기존 popup 패턴 |
| 헤더 | `modal-header` + `icon-button` | 드래그(`startDrag`) |
| 기존/변경 입사일 | `CalendarSrch` (공통) | 기존 disabled, 변경은 v-model. 초기값=기존 입사일 |
| 영향 분석 카드 | `div` 그리드 4칸 | warn/ok variant |
| 처리 방식 | native `<input type="radio">` (시각 커스텀) | [SYS039] 3종, 기본 KEEP_AND_BACKFILL |
| 변경 사유 | native `<textarea>` (maxlength 1000) | 필수 |
| 푸터 | `btn btn-second`(취소) + `btn btn-primary`(변경 적용) | |

### 상태별 동작

| 상태 | UI |
| --- | --- |
| loading | 변경할 입사일 변경 시 영향 분석 호출 중 `impactLoading` → "영향 분석 중..." 표시, 카드 숨김 |
| empty | 부여 데이터 없음(현재 대부분) → 영향 카드 모두 0/근사, note 안내 노출 |
| error | 영향 분석/적용 실패 → 알럿. 모달 유지 |
| success | 변경 적용 성공 → `props.onSaved?.()` 호출 후 `emit("close")`. 부모 UserInfoPop 입사일/이력 갱신 |
| 적용 비활성 | 변경 입사일 미입력 / 사유 공백 / (입사일 미변경) 시 [변경 적용] disabled |

### 사용자 플로우

1. UserInfoPop [입사일 수정] → 모달 오픈, 기존/변경 입사일 모두 기존 입사일로 세팅.
2. 변경할 입사일 선택 → 영향 분석 자동 호출(근사) → 카드/요약 갱신.
3. 처리 방식 선택(기본=권장 KEEP_AND_BACKFILL).
4. 변경 사유 입력(필수).
5. [변경 적용] → confirm → POST update-user-hire-date → 성공 시 부모 갱신 + 닫기.

### 백엔드 의존

- `GET /webApi/user01/{userCd}/hire-date-impact?newDate=YYYYMMDD` — 근사 영향 분석 (신규, master/hr, PRAFTA-017-4-2)
- `POST /webApi/user01/update-user-hire-date` — HIRE_DATE UPDATE + tb_user_hire_date_history INSERT (신규, master/hr, @Transactional, PRAFTA-017-4-2)

### 범위 분리 노트 (중요)

- 본 단계에서 백엔드는 **HIRE_DATE 변경 + tb_user_hire_date_history 기록(처리방식/사유/영향 스냅샷 JSON)까지만** 수행.
- `KEEP_AND_BACKFILL` / `RESET_ALL` 의 실제 grant 백필/취소/재발급(부여 조작)은 **연차 부여 엔진(PRAFTA-018) 완성 후 별도 작업**으로 분리. UI는 3택을 모두 노출하되 선택값은 HANDLING_TYPE 컬럼에 저장만.
