# UI 명세 — Attd_05 근무계획관리 (dirty 저장 + 셀 비우기)

- 화면 ID: UI (Attd_05 보완)
- 연결 작업: PRAFTA-041-3
- 화면 위치: `prafta-web-frontend/prafta-web-frontend/src/views/attd/Attd_05.vue`
- 작업유형: 보완 (신규 화면 아님)
- 참조 패턴: 동일 파일의 기존 적용 툴바(`.attd05-toolbar`, `.btn-toolbar-apply`), 드래그 선택(`selectionRange`/`fnApplySchType`), ViewHeader save/delete 버튼 패턴. 신규 컴포넌트 없음 — 기존 native 툴바 버튼 패턴 재사용.

## 현재 동작 (변경 전)
- 적용 툴바: [근무 타입] 선택+적용 / [법정 연차] 적용 / [엑셀 업로드]. 셀을 비우는 수단 없음.
- 저장: 체크된 사용자의 조회월 셀 전체를 그대로 재전송(변경 여부 무관).
- 삭제(ViewHeader delete): 체크된 사용자의 해당 월 전체 row 삭제.

## 의도된 동작 (변경 후)
1. 툴바에 "지우기" 버튼 추가. 드래그로 선택한 영역의 셀 값을 비운다(빈값). 비운 셀의 row 는 자동 체크. 마감월이면 alert 후 차단.
2. 저장 시 baseline(조회 직후 스냅샷) 대비 변경 셀만 전송:
   - 신규/값변경 → 업서트(`save-user-work-plans`)
   - 값있던 셀 → 빈값(비우기) → 삭제(`delete-user-work-plan-cells`)
   - 동일값 → 전송 제외
   - 모두 (체크 row) AND (조회월) 교집합. 업서트·삭제 모두 0건이면 "변경된 내용이 없습니다" 안내 + API 미호출.
3. 저장 성공 → `fnSearch` 재조회 → baseline 자연 갱신(직후 재저장 0건).

## 레이아웃 와이어프레임 (툴바만 — 그 외 그리드 변경 없음)

```
┌─ 적용 툴바 ─────────────────────────────────────────────────────────────┐
│ 근무 타입 [select▼] [선택영역] (○휴일제외 ○휴일포함) [적용]  선택:..건   │
│  │구분선│                                                                 │
│ 법정 연차 [본연차칩] [선택영역] (○휴일제외 ○휴일포함) [적용]  선택:..건   │
│  │구분선│                                                                 │
│ [지우기]  ← 신규                                  (spacer)  [엑셀 업로드] │
└─────────────────────────────────────────────────────────────────────────┘
```

- "지우기" 버튼은 두 적용 섹션 뒤(엑셀 업로드 앞, 또는 법정연차 섹션 다음 구분선 뒤)에 배치. 색은 중립/경고 톤(기존 `.btn-toolbar-upload` 의 outline 스타일 변형 또는 회색 outline). 하드코딩 금지 — `var(--color-border)`, `var(--color-text)` 등 기존 토큰 재사용.

## 컴포넌트 매핑

| 영역 | 사용 컴포넌트/요소 | 비고 |
| --- | --- | --- |
| 헤더 저장/삭제 | `ViewHeader` (@save=fnSave, @delete=fnDelete) | 기존 유지 |
| 지우기 버튼 | native `<button class="btn-toolbar-clear">` | 기존 툴바 버튼 패턴(공통 버튼 컴포넌트 없음 — 툴바는 native 일관) |
| 선택영역 표시 | 기존 `.toolbar-selection-box` 재사용 | 변경 없음 |
| 그리드 셀 | 기존 `.td-day`/`.td-val` | 빈값 시 기존 "-"/공백 렌더 유지 |
| 안내/확인 | `proxy.$alert` / `proxy.$confirm` | 기존 패턴 |

## 상태별 동작
- loading: 기존 조회 흐름 유지(별도 스피너 없음).
- empty(선택영역 없음에서 지우기): "지울 영역을 선택해주세요." alert.
- 변경없음 저장: "변경된 내용이 없습니다." alert, API 미호출.
- error: `resolveApiErrorMessage` 로 기존 동일 처리.
- success: 저장/삭제 합산 결과 alert(savedCount + 비우기/삭제 건수) 후 재조회.

## 사용자 플로우
진입(조회) → baseline 스냅샷 보관 → (드래그 선택 → 근무타입/연차 적용 또는 "지우기") → [저장] → baseline diff(업서트/삭제 분리) → 삭제 API + 업서트 API(0건이면 skip) → 성공 시 재조회·baseline 갱신.

## 백엔드 의존
- GET /webApi/attd05/user-work-plans (기존, baseline 적재)
- POST /webApi/attd05/save-user-work-plans (기존, dirty 업서트 — PRAFTA-041 범위에선 무변경, 전송건수만 감소)
- POST /webApi/attd05/delete-user-work-plan-cells (신규, PRAFTA-041-2)

## planner 작성 범위 (골격) vs developer 범위
- planner: 툴바 "지우기" 버튼 template + style(토큰 기반), 반응형 변수 선언(`scheduleBaseline`), `fnClearCells`/baseline 보관/`fnSave` 분기의 **stub + TODO(developer)**.
- developer: baseline 깊은복사 시점·diff 계산·payload 분리·삭제/업서트 API 호출·결과 합산 안내·재조회 연계.

## Vue 수정 포인트 (template + style 골격)

> 전체 파일 재작성이 아니라 아래 삽입/수정 지점만 명시. developer 가 script body 를 채운다.

### (1) template — 적용 툴바 내 "엑셀 업로드" 앞에 지우기 버튼 추가
`<div class="toolbar-spacer"></div>` 직전 또는 법정연차 섹션 뒤 구분선 다음에 삽입:

```vue
<!-- ── 셀 비우기 (지우기) ─────────────────────────────── -->
<button
  class="btn-toolbar-clear"
  :disabled="isMonthClosed"
  @click="fnClearCells"
>
  지우기
</button>
```

### (2) script — 반응형 변수 + handler stub (developer 가 body 채움)

```js
// ── baseline 스냅샷 (조회 직후 깊은 복사 — dirty 비교 기준) ──
const scheduleBaseline = ref({});

// ── 셀 비우기 (지우기) ──────────────────────────────────────
// 드래그 선택영역의 셀을 빈값으로 만들고 해당 row 를 자동 체크한다.
const fnClearCells = async () => {
  if (isMonthClosed.value) {
    await proxy.$alert("마감된 월입니다. 셀을 비울 수 없습니다.");
    return;
  }
  if (!selectionRange.value) {
    await proxy.$alert("지울 영역을 선택해주세요.");
    return;
  }
  // TODO(developer): 선택영역 셀을 scheduleData 에서 빈값/삭제 처리
  //   + 해당 user 들을 checkedRows 에 자동 체크 (fnApplySchType 의 자동체크 패턴 참고)
};
```

- `fnSearch` 응답 적재부(`scheduleData.value = {...}` 직후): `// TODO(developer): scheduleBaseline 갱신(깊은 복사)`.
- `fnSave`: `// TODO(developer): baseline 대비 업서트/삭제 분리, 0건이면 "변경된 내용이 없습니다" 안내, 삭제는 /attd05/delete-user-work-plan-cells, 업서트는 기존 save-user-work-plans, 성공 시 fnSearch 재조회`.

### (3) style — 지우기 버튼 (기존 토큰만 사용, !important 금지)

```css
/* ── 셀 비우기(지우기) 버튼 ─────────────────────────────── */
.btn-toolbar-clear {
  padding: 0.35rem 1rem;
  background: #fff;
  border: 1px solid var(--color-border, #d1d5db);
  color: var(--color-text, #374151);
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
  transition: background 0.15s;
  white-space: nowrap;
}
.btn-toolbar-clear:hover {
  background: var(--color-bg, #f3f4f6);
}
.btn-toolbar-clear:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
```

> 골격 코드는 명세 참고용(승인 후 developer 가 Attd_05.vue 에 반영). planner 단계에서는 별도 .vue 신규 파일을 생성하지 않는다(기존 화면 보완이므로 Edit 대상이며, 본 작업의 script body 는 developer 영역).
