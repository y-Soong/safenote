# prafta-030 — 입사일 변경 처리방식별 동작 확인·수정 : 작업 분해 계획

> 작성: planner 세션. 단일 출처 = `.claude/requests/prafta-030-decisions.md`(D1~D5). 본 계획은 그 결정을 developer가 바로 착수 가능한 단위로 쪼갠 것이다.
> 정답표 출처 = `.claude/requests/ref/prafta-030/_xlsx_dump.txt` (SHEET4 = 18케이스, SHEET7 = 경계A, SHEET8 = 경계B, SHEET9 = 분류 매트릭스).
> 오늘(테스트 고정) = 2026-05-26.

---

## 0. 핵심 진단 (코드 정독 결과 — 왜 현행이 정답표와 어긋나는가)

정답표 SHEET4를 현행 엔진(`LeaveGrantEngineServiceImpl`)에 대입해 본 결과, **수정이 필요한 셀은 옵션1(KEEP_AND_BACKFILL) 과거변경의 "근속 증가" 케이스 + 경계B 뿐**이다. 옵션2/옵션3/미래변경은 이미 일치한다(결정문서 §2).

현행 `KEEP_AND_BACKFILL`의 한계(코드 라인):
- `buildUserPlan`(:445) 당기부여 + `computeBackfillPeriods`(:697) 과거 anniversary 백필 + `computeMonthlyPeriods`(:809) 월차로 구성된다.
- 백필은 **유효기간(AXIS6=12개월) 경과분을 항상 제외**(:738 `availTo < today` continue)한다. 그래서 #1(+1)·#7/#13(+2)·경계B(+8)에서 기대되는 "과거 근속 증가분"이 **소멸 처리되어 0**으로 떨어진다.
  - 예 #1(HIRE_DATE, 과거 2023→2021): 새 기준 5년차 본연차 17일을 오늘 보유해야 하는데(SHEET3), 당기부여는 달력연도 2026 본연차 15 + 가산(5년차 tier=+1, 별도 컴포넌트)만, 과거 anniversary(2022~2025)는 전부 유효기간 경과 제외 → 가산 tier가 "현재 근속연차" 기준으로 정확히 잡히면 16(=15+1)까지는 가나 17(+1 추가)에 미달. 결국 +1 부족.
- 또한 백필/당기/월차가 **컴포넌트별 멱등키로 각자 INSERT**된다. 정답표는 "차액(새 기준 누적 − 기존 누적)"이라는 **단일 보전 금액**을 기대한다. 현행 컴포넌트 백필을 그대로 두면 차액 보전과 **이중부여**가 난다(결정문서 D1 명시 리스크).

→ D1 결론: **옵션1 경로를 "컴포넌트 백필" 대신 "차액 보전 단건"으로 재설계**한다. 차액 산식이 컴포넌트 백필을 대체·포함한다.

월차 게이트(D2)의 현행 한계:
- `computeMonthlyPeriods`(:809)는 `actualMonths` 기준 월차(최대 11)를 본연차 발생 여부와 무관하게 항상 부여한다. 경계B 과거변경 후 "이미 본연차가 발생해야 하는" 직원에게도 월차 11일이 그대로 부여되어 정답표(월차 미발생 + 본연차)와 어긋난다.

---

## 1. 단위 작업 분해 (권장 착수 순서)

> 착수 순서: **BE-2 → BE-1 → TEST(1차) → BE-3 → FE-1 → DOC**.
> 근거: 월차 게이트(BE-2)가 entitlement 산정의 전제(본연차 발생 판정)를 바꾸므로 차액보전(BE-1)보다 먼저 확정해야 BE-1이 안정된 entitlement 위에서 차액을 계산한다. 미리보기 API(BE-3)는 BE-1/BE-2의 산식을 read-only로 재사용하므로 그 뒤. FE는 BE-3 응답 계약 확정 후.

---

### PRAFTA-030-1 (BE-2 / D2) — 1년 미만 월차 게이트: 본연차 발생 시 월차 미발생

- **유형**: backend
- **영역**: web (공통 엔진 `common.cmm.leave`)
- **모듈**: cmm/leave
- **작업 유형**: 버그수정 / 보완
- **목적**: 고용승계·과거변경으로 "본연차가 실제 발생하는" 직원에게 월차가 중복 부여되지 않게 막는다(§60② 위반 아님 — 더 유리한 처우). 단, 본연차가 발생하지 않는 부분기간(FISCAL crossed==0 등)에는 월차를 유지해 공백을 막는다.
- **대상 파일**:
  - `prafta-backend/.../common/cmm/leave/service/impl/LeaveGrantEngineServiceImpl.java`
    - `computeMonthlyPeriods`(:809) — 게이트 추가
    - `resolveEntitlement`(:533)/`resolveHireDateEntitlement`(:558)/`resolveFiscalEntitlement`(:583) — "본연차 실제 발생" 판정 신호를 월차 산정과 공유
- **변경 요지**:
  1. "본연차 실제 발생" 판정 헬퍼를 추가한다(예: `hasAnnualAccrued(policy, hire, creditMonths, today)`). HIRE_DATE면 `creditedMonths >= 12`, FISCAL이면 `crossedFiscalStarts >= 1`(crossed==1 PRORATE/NEXT_YEAR_BULK·crossed>=2 모두 본연차 발생). **게이트 조건은 "산정근속≥12"가 아니라 "본연차가 실제로 발생하는 경우"로 묶는다**(결정문서 D2/D3 — FISCAL 첫 부분기간 crossed==0은 본연차 미발생이므로 월차 유지).
  2. `computeMonthlyPeriods`에서, 본연차가 실제 발생하는 직원이면 월차 후보를 **생성하지 않는다**(빈 목록 반환 또는 루프 전 가드). 본연차 미발생이면 현행대로 `actualMonths` 기준 월차(실근속 유지 — 결정문서 D2: "월차 일수는 실근속 기준 유지").
  3. 월차 게이트 판정은 **실근속(actualMonths)이 아닌 산정근속/회계연도 도래로 결정되는 "본연차 발생 여부"** 를 쓴다(경력 인정 포함). 월차 "일수"는 여전히 실근속.
- **정책/정답표 출처**:
  - 정책서 §8.5.4(1년 미만 월차 법정 의무) + 결정문서 D2/D3.
  - 정답표: SHEET8(경계B) — 과거변경으로 1년 초과가 된 직원은 월차 11일이 아니라 본연차로 전환. SHEET6 경계 개요. SHEET9 분류 매트릭스(경계B 옵션1: 기존 월차 유지 + 추가는 STATUTORY_ANNUAL).
  - ⚠️ 주의: 경계B는 "기존 월차 7일 유지"(이미 부여된 것)이고, BE-2는 "새로 부여할 때 월차를 안 만든다"는 것. 기존 부여 보호(§8.5.8)와 충돌하지 않는다(기존 월차는 BE-2가 건드리지 않음 — 미래 부여 산정만 게이트).
- **검증 포인트**:
  - HIRE_DATE, 입사 2021-01-01(만 5년), creditMonths=0 → `computeMonthlyPeriods` 빈 목록(본연차 발생).
  - HIRE_DATE, 입사 2026-02-01(만 ~4개월) → 월차 4일 유지(본연차 미발생).
  - FISCAL(1/1 시작), 입사 2026-02-01 → crossed==0 → 본연차 미발생 → 월차 4일 유지(공백 방지).
  - FISCAL, 입사 2023-10-01 새기준(경계B) → crossed>=1 → 월차 미발생, 본연차 경로.
  - 단위테스트: PRAFTA-030-TEST에 흡수(경계B + #1·#7·#13에서 월차 0 확인).
- **선행 작업**: 없음
- **우선순위 근거**: 법적 책임 영역(attd 연차) +1단계. entitlement 산정 전제를 바꿔 BE-1의 입력을 안정화하므로 선행.

---

### PRAFTA-030-2 (BE-1 / D1) — 옵션1 KEEP_AND_BACKFILL 차액 보전으로 재설계  ★핵심

- **유형**: backend
- **영역**: web (공통 엔진 `common.cmm.leave`)
- **모듈**: cmm/leave
- **작업 유형**: 리팩터링 / 버그수정 (정답표 정합)
- **목적**: 과거변경(근속 증가)으로 "새 기준 부여누적 > 기존 부여누적"이면 그 **차액을 단건 추가 부여**한다. 줄이지 않는다(미래변경/근속감소면 차액≤0 → 추가 없음 = 옵션2와 동일). 컴포넌트 백필과의 이중부여를 제거한다.
- **대상 파일**:
  - `prafta-backend/.../common/cmm/leave/service/impl/LeaveGrantEngineServiceImpl.java`
    - `hireDateGrant`(:115) (c-2) 백필 블록(:170~183) — BACKFILL 경로를 차액보전 호출로 교체(RESET_ALL은 기존 컴포넌트 백필 경로 유지)
    - `previewPolicyGrant`(:227) BACKFILL 합산부(:267~275) — 동일 차액 산식 read-only 재사용
    - 신규 private 메서드 `computeBackfillShortfall(...)`(차액 산정, read-only) + 보전 INSERT(차액 단건)
    - `estimateBackfillDays`(:322) — 차액 산식으로 재정의(영향분석 정합)
  - `prafta-backend/.../common/cmm/leave/mapper/LeaveDashboardMapper.java` + `LeaveDashboardMapper.xml` (또는 `LeaveGrantEngineMapper`) — "기존 부여누적(소멸 제외, 사용 포함)" 합계 조회 SQL 신규 1건
- **변경 요지**:
  1. **차액 산식(결정문서 D1 / SHEET8 경계B 핵심포인트1)**:
     `차액 = (새 기준 부여누적) − (기존 부여누적)`, **단 본연차/가산만 대상**(월차는 별도).
     - **새 기준 부여누적**: 새 입사일 기준으로 "오늘 시점 유효(소멸 제외)해야 할" 본연차+가산 누적. 정답표는 "오늘 보유했어야 할 양"(SHEET3·SHEET8 = 최신 발생분만 유효기간 내 보유)이다. 즉 **유효기간 내 발생분의 누적**(현행 백필 후보 합 + 당기분).
     - **기존 부여누적**: 변경 전 입사일로 이미 발생·부여된 본연차+가산의 누적. "소멸 제외, 사용 포함"(SHEET8: 기존 발생 누적 7 기준, 사용 5일은 차액에 영향 없음). → live(STATUS!='CANCELED' AND DEL_YN='N') STATUTORY_ANNUAL/TENURE_BONUS의 `GRANT_DAYS` 합(USED_DAYS 무관, 사용분 포함). 월차(STATUTORY_MONTHLY)는 누적에서 제외.
     - 차액 > 0이면 그만큼 **STATUTORY_ANNUAL 단건** 추가 부여. ≤ 0이면 추가 없음.
  2. **이중부여 차단(D1 핵심)**: BACKFILL 경로에서 `computeBackfillPeriods` 컴포넌트 INSERT를 **호출하지 않는다**(차액 단건이 이를 대체). 당기부여(`plan.components`)도 BACKFILL일 때는 차액 산식에 흡수 — 단, 멱등 충돌을 피하려면 차액 보전을 **별도 멱등키 단건**으로 두고, BACKFILL일 때 당기 컴포넌트/컴포넌트 백필을 모두 끄거나(권장) 차액 = (목표누적 − 기존누적 − 당기신규부여분)으로 보정. **developer 결정 포인트**: "BACKFILL이면 당기/컴포넌트백필 끄고 차액 단건만" 방식이 가장 단순하고 정답표와 정합(아래 검증값으로 확인). RESET_ALL/APPLY_NEW 경로는 무변경.
  3. **추가분 태깅**:
     - `GRANT_TYPE = STATUTORY_ANNUAL` (경계B처럼 월차→연차 전환이어도 부족분은 ANNUAL — SHEET8/SHEET9).
     - `GRANT_REASON = "입사일 변경 보전(INSADAY_CHANGE_BACKFILL)"` (결정문서 D1/D5: 코드 접두어로 식별 가능하게). 기존 상수 `HIRE_GRANT_REASON`과 별도 신규 상수 `BACKFILL_GRANT_REASON` 추가.
     - `AVAIL_FROM_DATE = today`(부여일), `AVAIL_TO_DATE = 새 기준 동일 발생항목의 정상 소멸일`. 결정문서 D1: "발생일 + AXIS6". 차액은 "가장 최신 발생분"의 부족이므로 소멸일 = 최신 발생항목 발생일 + AXIS6. **developer 결정 포인트**: 단순화하려면 `today + AXIS6`(현행 당기부여 availTo와 동일)로 둘 수 있으나, 정답표는 "발생일 기준 소멸"을 명시 → 최신 발생일 기준 권장. 검증값(#1 등 소멸 2027-01-01)으로 확인.
  4. **멱등키 체계(prafta-029 가드 보존 — 결정문서 §6/D1)**:
     - 보전 단건 멱등키 = `{userCd}_{최신발생연도라벨}_STATUTORY_ANNUAL_BF{histId}` 같은 **보전 전용 접미사**(`_BF{histId}`)로 둔다. 표준키(`""`)·리셋키(`_R{histId}`)와 충돌하지 않게 별도 네임스페이스.
     - 재클릭 멱등: 같은 미적용 이력(histId)으로 다시 누르면 같은 `_BF{histId}` 키 → `alreadyGranted` live-only dual-read로 skip. 이력이 적용(APPLIED_YN='Y') 마킹되면 재클릭 자체가 막힘(현행 (d) 블록).
     - ⚠️ `alreadyGranted`의 `countActiveBySuffixVariant`(:1027)는 "표준키 클릭일 때 변형키 ACTIVE면 기부여로 간주"한다. 보전키(`_BF{histId}`)는 keySuffix가 비어있지 않으므로 이 검사 비대상(정상). 단 **표준키 당기부여가 보전 단건과 같은 (연도,ANNUAL) 변형으로 잡히면** 이후 표준키 클릭이 막힐 수 있다 → BACKFILL일 때 표준키 당기 ANNUAL 부여를 끄는 방식(위 2)을 쓰면 회피된다. **developer는 이 상호작용을 단위테스트로 반드시 검증**(prafta-029 RESET 재클릭 + 030 보전 재클릭 동시 시나리오).
  5. **경계(월차→연차)**: 기존 월차(STATUTORY_MONTHLY) 부여는 **유지**(차액 산식이 월차를 누적에서 제외하므로 회수/취소 없음). 부족분은 STATUTORY_ANNUAL로 보전(SHEET8: 잔여 월차 2일 + 추가 연차 8일 = 10일).
- **정책/정답표 출처**:
  - 결정문서 D1, D5. 정책서 §8.5.6(처리 매트릭스)·§8.5.8(멱등·기부여보호 — 절대 줄이지 않음, CANCELED 소프트, 사용이력 불변).
  - 정답표 SHEET4 #1(+1)·#7(+2)·#13(+2), #2/#8/#14(미래=0), SHEET8 경계B 옵션1(+8, STATUTORY_ANNUAL, INSADAY_CHANGE_BACKFILL).
- **검증 포인트** (오늘=2026-05-26, fiscal 1/1, AXIS5 법정 3/2/25):
  - #1: A(HIRE_DATE), 2023-01-01→2021-01-01. 기존 누적 16(SHEET2 A), 새 기준 누적 17(SHEET3 A) → **차액 +1**, 최종 17. (현행은 +0~잘못)
  - #7: B(FISCAL+PRORATE) 과거변경. 기존 15, 새 기준 17 → **+2**, 최종 17.
  - #13: C(FISCAL+NEXT_YEAR_BULK) 과거변경. 기존 15, 새 기준 17 → **+2**, 최종 17.
  - #2/#8/#14(미래변경): 새 기준 누적 ≤ 기존 → **+0**(추가 없음), 최종 = 기존 보유.
  - 경계B: 2025-10-01→2023-10-01, 기존 월차 누적 7(사용5·잔여2), 새 기준 본연차 누적 15 → **차액 +8(STATUTORY_ANNUAL, BACKFILL 사유)**, 월차 7 보존, 최종 보유 = 잔여월차2 + 8 = 10.
  - 멱등 재클릭: 같은 histId로 재실행 시 보전 0건(skip), 취소·재발급 누수 없음(prafta-029 가드 유지).
- **선행 작업**: PRAFTA-030-1 (월차 게이트가 "새 기준 누적"의 월차 제외/본연차 발생 판정을 결정)
- **우선순위 근거**: 데이터 정합성 + 법적 책임 영역(attd) — 최우선 격상. 정답표 채점 핵심 셀.

---

### PRAFTA-030-3 (BE-3 / D4) — 옵션별 시뮬레이션 미리보기 API (read-only)

- **유형**: backend
- **영역**: web
- **모듈**: user/user01 (+ 엔진 read-only 재사용)
- **작업 유형**: 신규 (기존 영향분석 확장)
- **목적**: HireDateEditPop이 옵션1/2/3 각각의 (추가/회수/최종보유)와 회수표시·FISCAL 다음 회계연도 발생예정을 한 번에 받도록 한다.
- **대상 파일**:
  - `prafta-backend/.../web/user/user01/controller/User01Controller.java` — `GET /{userCd}/hire-date-impact`(:159) 확장 또는 신규 `GET /{userCd}/hire-date-preview`
  - `prafta-backend/.../web/user/user01/service/(impl/)User01Service(Impl).java` — `buildApproxImpact`(:492) 확장
  - `prafta-backend/.../web/user/user01/dto/response/HireDateImpactResponse.java` — 옵션별 시뮬 필드 추가(또는 신규 `HireDatePreviewResponse`)
  - `prafta-backend/.../common/cmm/leave/service/(impl/)LeaveGrantEngineService(Impl).java` — 옵션별 시뮬 산정 read-only 메서드(차액·회수량·최종보유) 신규. BE-1/BE-2 산식 재사용.
- **변경 요지** (엔드포인트 확장 vs 신규 — **planner 제안: 기존 `hire-date-impact` GET 확장**):
  - 근거: FE가 이미 `newDate` 1개 파라미터로 진입 시/입사일 변경 시 호출 중(HireDateEditPop `fnGetImpact`). 응답에 옵션별 시뮬 블록을 추가하면 호출부 1곳만 손대면 된다. 신규 엔드포인트는 라우팅·권한·param 중복 비용만 늘린다. 단 응답 DTO가 커지므로 `options` 하위 객체로 구조화(아래).
  - 응답 추가 필드(JSON):
    ```
    options: [
      { code:"KEEP_AND_BACKFILL", addDays, reclaimDays, finalHoldDays, reclaimNote },
      { code:"KEEP_AND_APPLY_NEW", addDays:0, reclaimDays:0, finalHoldDays, reclaimNote },
      { code:"RESET_ALL", addDays, reclaimDays, finalHoldDays, reclaimNote }
    ],
    fiscalNextGrantText   // FISCAL 정책일 때 "본연차 다음 회계연도(YYYY-MM-DD) 발생 예정", 아니면 ""
    ```
    - `addDays`: 옵션1 = BE-1 차액(>0만), 옵션2 = 0, 옵션3 = 새 기준 재발급 누적.
    - `reclaimDays`: 옵션1/2 = 0(회수 안 함), 옵션3 = 기존 live STATUTORY_* 누적(취소 예정량).
    - `reclaimNote`: 옵션1/2에서 "본래 회수돼야 할 N일이 있으나 회수하지 않습니다"(미래변경으로 새 기준<기존일 때 N=기존−새기준, N>0일 때만). 옵션3은 "기존 N일 취소 후 재부여".
    - `finalHoldDays`: 옵션1 = 기존보유 + 차액, 옵션2 = 기존보유, 옵션3 = 새 기준 재발급 누적(SHEET4 "최종 보유"와 일치).
  - **read-only 절대 준수**: cancelGrant/INSERT/UPDATE 호출 금지. 옵션3 reclaim/재발급은 "산정만"(실제 취소·부여는 Attd_09 버튼). 권한 가드는 기존 `analyzeHireDateImpact`의 `AuthRoleUtils.isManager` 유지.
- **정책/정답표 출처**: 결정문서 D4/D5. SHEET4(옵션별 추가/회수/최종), SHEET5 인사이트1(미래변경 옵션1=옵션2), SHEET7/SHEET8 옵션 처리표. §8.5.7 권한.
- **검증 포인트**:
  - #1 응답: 옵션1 add=1·reclaim=0·final=17 / 옵션2 add=0·final=16 / 옵션3 reclaim=16·add=17·final=17.
  - #6(미래·옵션3): 옵션1 reclaimNote="본래 회수 1일이 있으나..."(16→15), final=16 / 옵션3 reclaim=16·add=15·final=15.
  - FISCAL 정책: `fiscalNextGrantText` = 다음 회계연도 시작일(예 2027-01-01) 노출.
  - HIRE_DATE 정책: `fiscalNextGrantText`="" (FE에서 미표시).
- **선행 작업**: PRAFTA-030-2, PRAFTA-030-1 (시뮬 산식이 BE-1/BE-2 결과를 read-only 재사용)
- **우선순위 근거**: API 없이 FE 작업 불가(우선순위 원칙 2). 법적영역 +1.

---

### PRAFTA-030-4 (FE-1 / D4) — HireDateEditPop.vue 영향분석/처리방식 영역 교체

- **유형**: frontend-screen
- **영역**: web
- **모듈**: user/popup
- **작업 유형**: 보완
- **목적**: 기존 "영향 분석 4카드 + 처리방식 라디오 안내문"을 **옵션별 미리보기(추가/회수/최종보유) + 회수표시 + 수정된 안내문구 + FISCAL 다음 회계연도 발생예정**으로 바꾼다.
- **대상 파일**:
  - `prafta-web-frontend/prafta-web-frontend/src/views/user/popup/HireDateEditPop.vue` (template + scoped style 교체; script는 developer가 BE-3 응답 바인딩)
- **변경 요지**:
  1. "2. 영향 분석" 섹션: 기존 4카드(기존부여/사용/누락부여/다음부여)는 유지하되, FISCAL 정책일 때 "본연차 다음 회계연도 발생 예정" 줄 추가(`impact.fiscalNextGrantText`).
  2. "3. 처리 방식" 섹션: 각 옵션 라디오 아래에 **옵션별 시뮬 미니 카드**(추가 N일 / 회수 N일 / 최종 보유 N일) + 회수표시 문구(`reclaimNote`)를 표시. 옵션 선택 시 해당 옵션 카드 강조.
  3. 안내문구 수정(결정문서 D4 — 문구 수준만, 워크플로우 신설 금지):
     - 미래변경 감지 시: "미래 입사일 변경은 이미 부여된 연차를 줄이지 않으므로 옵션1·2가 동일하게 동작합니다."
     - 경계(월차↔연차) 감지 시: "근속 1년 경계가 바뀝니다. 기존 월차는 유지되고 부족한 연차가 법정 연차로 추가 부여됩니다."
     - 옵션3(RESET_ALL) 선택 시 기존 danger 경고 강화: "근로자 동의 또는 입력 오류 정정에 한해 사용하세요. 사용한 연차가 있으면 별도 처리가 필요합니다."
  4. **신설 금지**: 변경사유 분기 드롭다운(오류정정/경력인정), 옵션3 전자동의 워크플로우는 추가하지 않는다(결정문서 OUT). 4번 변경사유 textarea·footer는 무변경.
  5. 공통 컴포넌트(CalendarSrch) 유지, scoped CSS + 기존 CSS 변수/BEM(`hire-date-pop__*`) 유지, TS 금지.
- **연결 UI 명세**: 아래 §3 (코드블록 골격).
- **백엔드 의존**: `GET /webApi/user01/{userCd}/hire-date-impact` (PRAFTA-030-3 확장 응답).
- **검증 포인트**: 옵션 전환 시 미니카드 값 변동, 미래변경 시 안내문구 노출, FISCAL 시 발생예정 줄 노출, RESET_ALL 선택 시 경고 강조.
- **선행 작업**: PRAFTA-030-3
- **우선순위 근거**: API 후행. 법적영역 +1이나 화면이라 BE 뒤.

---

### PRAFTA-030-5 (TEST) — 정답표 18케이스 + 경계 A/B 결정적 단위테스트

- **유형**: backend (test)
- **영역**: web
- **모듈**: cmm/leave (test)
- **작업 유형**: 신규
- **목적**: 정답표(SHEET4/7/8)를 기대값으로 고정해 BE-1/BE-2 회귀를 막는다.
- **대상 파일**:
  - `prafta-backend/src/test/java/.../leave/service/impl/LeaveGrantEnginePrafta030Test.java` (신규; `LeaveGrantEnginePrafta029Test`/`...ScenarioTest` 하니스 확장)
- **변경 요지**:
  1. `mockStatic(LocalDate)` → 2026-05-26 고정(`CALLS_REAL_METHODS` + `LocalDate::now`).
  2. **2단계 시뮬 하니스**: 정답표는 "기존 부여 누적"을 전제로 한다. 따라서 (a) 변경 전 입사일로 부여한 결과(기존 누적)를 Mockito 스텁의 live 부여 합으로 주입 → (b) 변경 후 입사일 + 미적용 이력(handlingType)으로 `previewPolicyGrant`/`hireDateGrant` 호출 → addDays/reclaim/final 검증.
     - 기존 부여 누적 주입: `selectUserStatutoryLeaveSummary`(영향분석)·BE-1 "기존 누적" SQL mock을 SHEET2/SHEET8 값으로 스텁. live dual-read(`countLiveByIdempotencyKey`)는 기존 부여 컴포넌트가 있는 것으로 스텁.
  3. 18케이스(#1~#18) 각 셀: (규칙 A/B/C) × (옵션1/2/3) × (과거/미래). 기대 add/reclaim/final = SHEET4.
  4. 경계 A(SHEET7: 2024-01-01→2026-01-01) / 경계 B(SHEET8: 2025-10-01→2023-10-01) 별도 테스트.
  5. 월차 게이트(BE-2): #1/#7/#13·경계B에서 월차 컴포넌트 0 확인.
  6. 멱등 재클릭(prafta-029 가드 유지 + 보전키): 같은 histId 재실행 시 보전 0건.
- **정책/정답표 출처**: SHEET4(18케이스 결과), SHEET7(경계A), SHEET8(경계B), SHEET9(분류). 결정문서 §6(컴파일+결정적 단위테스트까지가 에이전트 범위).
- **검증 포인트**: 전 셀 PASS. RESET 누수/이중부여 0(prafta-029 회귀 동시 통과).
- **선행 작업**: PRAFTA-030-2, PRAFTA-030-3 (테스트 대상). 1차 BE-1/BE-2 직후 골격 작성 가능.
- **우선순위 근거**: 정합성 검증 필수(법적영역). BE 직후.

---

### PRAFTA-030-6 (DOC) — 가이드/정책서/CHANGELOG 갱신

- **유형**: backend (문서)
- **영역**: web
- **모듈**: docs
- **작업 유형**: 보완
- **목적**: 차액보전·월차게이트·옵션별 미리보기의 동작을 문서에 반영해 "왜 입사일 바꿔도 안 변하지" 혼동과 stale 설명을 정리한다.
- **대상 파일**:
  - `.claude/context/hire-date-change-handling-guide.md` §3(처리방식별 동작 상세)·§10(알려진 한계). §3.1에서 "KEEP_AND_BACKFILL = 차액 보전 단건" 동작 추가, §10 항목2(KEEP_*는 일수 변화 자동보정 안 함)를 "BACKFILL은 차액 보전으로 근속 증가 반영(prafta-030)"으로 갱신.
  - `.claude/context/policies/attd/08-leave.md` §8.5.4(월차 게이트: 본연차 발생 시 월차 미발생 명시)·§8.5.6(옵션1 차액보전·보전 GRANT_REASON 태깅).
  - `.claude/context/policies/CHANGELOG.md` 상단에 prafta-030 항목 추가.
- **변경 요지**: 정책서는 INDEX 경유 해당 섹션만 수정. 차액 산식·보전 사유 코드(INSADAY_CHANGE_BACKFILL)·월차 게이트 조건("본연차 실제 발생 시")·옵션별 미리보기 API 정합을 기술.
- **정책/정답표 출처**: 결정문서 전체. 정답표 SHEET9 분류 체계(GRANT_REASON enum 중 prafta가 채택하는 것은 자유텍스트 접두어).
- **검증 포인트**: 문서 내 라인 참조·코드 동작 일치(엔진 수정 후 라인 재확인).
- **선행 작업**: PRAFTA-030-2, PRAFTA-030-3
- **우선순위 근거**: 코드 개선 후순위. BE 확정 후.

---

## 2. 화면 명세

### UI-(채번) HireDateEditPop (보완)
- 연결 작업: PRAFTA-030-4
- 화면 위치: `src/views/user/popup/HireDateEditPop.vue`
- 참조 패턴: 기존 자기 자신(드래그 모달 `modal-content-normal`, BEM `hire-date-pop__*`, step badge, warn-box, CalendarSrch). 4카드 영향분석 구조 유지하며 옵션 섹션에 미니카드 추가.
- 현재 동작: "2. 영향 분석" 4카드(기존부여/사용/누락부여/다음부여) + "3. 처리 방식" 라디오 3종(정적 안내문). 옵션별 시뮬·회수표시·FISCAL 발생예정 없음.
- 의도된 동작:
  - 영향분석에 FISCAL 정책 시 "본연차 다음 회계연도(YYYY-MM-DD) 발생 예정" 줄 추가.
  - 처리방식 라디오 각각 아래에 옵션별 미니카드(추가/회수/최종보유 + 회수표시 문구).
  - 미래변경/경계/옵션3 상황별 안내문구(문구 수준).
- 레이아웃 (옵션 섹션 추가분):
  ```
  [3] 처리 방식 *
   ( ) 기존 부여 유지 + 누락분만 소급 부여  [권장]
       설명...
       ┌ 미리보기 ──────────────┐
       │ 추가 +N일  회수 0일  최종 N일 │
       │ (회수표시: 본래 회수 N일…)    │   ← 미래변경 시
       └────────────────────┘
   ( ) 기존 부여 유지 + 신규부터 새 기준
       ...  추가 0 / 회수 0 / 최종 N
   ( ) 모든 부여 삭제 후 재계산  [위험]
       ...  추가 N / 회수 N / 최종 N
   [!] 미래변경/경계/옵션3 안내문구 (상황별)
  ```
- 컴포넌트 매핑:
  | 영역 | 컴포넌트/요소 |
  |---|---|
  | 입사일 입력 | CalendarSrch (기존 유지) |
  | 옵션 라디오 | 기존 커스텀 radio(`hire-date-pop__option`) 유지 |
  | 미니카드 | 신규 `hire-date-pop__option-sim` (div, CSS 변수) |
  | 안내문구 | 기존 `hire-date-pop__warn-box` 재사용 |
- 상태별 동작: loading=`impactLoading` 스피너 텍스트 / empty=입사일 미입력 시 미니카드 "-" / error=`$alert` / success=옵션별 값 표시.
- 사용자 플로우: 팝업 진입 → 변경 입사일 선택 → 영향분석+옵션별 시뮬 자동 조회 → 옵션 라디오 선택(미니카드/안내 갱신) → 사유 입력 → 변경 적용.
- 백엔드 의존: `GET /webApi/user01/{userCd}/hire-date-impact?newDate=YYYYMMDD` (PRAFTA-030-3).

---

## 3. Vue 골격 (HireDateEditPop.vue — 영향분석/처리방식 영역 교체분)

> 아래는 **변경되는 "2. 영향 분석"·"3. 처리 방식" 섹션 template + 신규 scoped style**만 제시한다. 나머지(헤더/1.입사일/4.사유/footer/script 대부분)는 기존 파일 유지. script는 developer가 BE-3 응답(`impact.options`, `impact.fiscalNextGrantText`)을 바인딩하므로 `// TODO(developer)`로 자리만 남긴다. 기존 CSS 변수/BEM·scoped·TS 금지 준수.

```vue
<!-- ===== 2. 영향 분석 (FISCAL 다음 회계연도 발생예정 줄 추가) ===== -->
<div class="hire-date-pop__section">
  <p class="hire-date-pop__section-title">
    <span class="hire-date-pop__step">2</span>
    영향 분석
    <span class="hire-date-pop__scenario" v-show="impact.scenarioLabel">
      {{ impact.scenarioLabel }}
    </span>
  </p>

  <div class="hire-date-pop__impact-loading" v-show="impactLoading">
    영향 분석 중...
  </div>

  <div class="hire-date-pop__impact-grid" v-show="!impactLoading">
    <div class="hire-date-pop__impact-card">
      <p class="hire-date-pop__impact-label">기존 부여 (변경 전 입사일 기준)</p>
      <p class="hire-date-pop__impact-value">{{ impact.existingGrantText || "-" }}</p>
    </div>
    <div class="hire-date-pop__impact-card">
      <p class="hire-date-pop__impact-label">사용된 연차</p>
      <p class="hire-date-pop__impact-value">{{ impact.usedText || "-" }}</p>
    </div>
    <div class="hire-date-pop__impact-card hire-date-pop__impact-card--warn">
      <p class="hire-date-pop__impact-label">누락된 부여 (변경 후 기준)</p>
      <p class="hire-date-pop__impact-value">{{ impact.missingGrantText || "-" }}</p>
    </div>
    <div class="hire-date-pop__impact-card hire-date-pop__impact-card--ok">
      <p class="hire-date-pop__impact-label">다음 부여 예정 시점</p>
      <p class="hire-date-pop__impact-value">{{ impact.nextGrantText || "-" }}</p>
    </div>
  </div>

  <!-- FISCAL(회계연도) 정책일 때만: 본연차 다음 회계연도 발생예정 (D3/D4) -->
  <div
    class="hire-date-pop__fiscal-note"
    v-show="!impactLoading && impact.fiscalNextGrantText"
  >
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
         stroke-linecap="round" stroke-linejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
    <span>{{ impact.fiscalNextGrantText }}</span>
  </div>

  <p class="hire-date-pop__impact-note">
    ※ 영향 분석은 정책(회계연도/입사일 기준)을 반영한 시뮬레이션입니다.
    실제 부여(소급/회수/재발급)는 사용자 연차관리(Attd_09)의 '정책 기준 부여'에서 처리됩니다.
  </p>
</div>

<!-- ===== 3. 처리 방식 (옵션별 미리보기 미니카드 + 회수표시 + 안내문구) ===== -->
<div class="hire-date-pop__section">
  <p class="hire-date-pop__section-title">
    <span class="hire-date-pop__step">3</span>
    처리 방식<span class="hire-date-pop__required">*</span>
  </p>

  <div class="hire-date-pop__options">
    <label
      v-for="opt in handlingOptions"
      :key="opt.code"
      class="hire-date-pop__option"
      :class="{ 'is-selected': handlingType === opt.code }"
    >
      <input
        class="hire-date-pop__radio-native"
        type="radio"
        :value="opt.code"
        v-model="handlingType"
        name="hireDateHandling"
      />
      <span class="hire-date-pop__radio" aria-hidden="true"></span>
      <span class="hire-date-pop__option-text">
        <span class="hire-date-pop__option-label">
          {{ opt.label }}
          <span
            v-if="opt.badge"
            class="hire-date-pop__badge"
            :class="`hire-date-pop__badge--${opt.badgeType}`"
          >
            {{ opt.badge }}
          </span>
        </span>
        <span class="hire-date-pop__option-sub">{{ opt.sub }}</span>

        <!-- 옵션별 시뮬 미니카드 (BE-3 응답: optionSim[opt.code]) -->
        <span
          class="hire-date-pop__option-sim"
          v-show="!impactLoading && optionSim(opt.code)"
        >
          <span class="hire-date-pop__sim-item">
            추가
            <strong class="hire-date-pop__sim-add">+{{ simAddText(opt.code) }}</strong>
          </span>
          <span class="hire-date-pop__sim-item">
            회수
            <strong class="hire-date-pop__sim-reclaim">{{ simReclaimText(opt.code) }}</strong>
          </span>
          <span class="hire-date-pop__sim-item">
            최종 보유
            <strong class="hire-date-pop__sim-final">{{ simFinalText(opt.code) }}</strong>
          </span>
        </span>

        <!-- 회수표시 문구 (옵션1·2: 본래 회수돼야 할 N일 미회수 안내) -->
        <span
          class="hire-date-pop__sim-reclaim-note"
          v-show="!impactLoading && simReclaimNote(opt.code)"
        >
          {{ simReclaimNote(opt.code) }}
        </span>
      </span>
    </label>
  </div>

  <!-- 상황별 안내문구 (문구 수준 — 워크플로우 신설 없음) -->
  <div
    class="hire-date-pop__warn-box hire-date-pop__warn-box--warn"
    v-show="isFutureChange"
  >
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
         stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="12" r="10" /><line x1="12" y1="16" x2="12" y2="12" />
      <line x1="12" y1="8" x2="12.01" y2="8" />
    </svg>
    <p class="hire-date-pop__warn-text">
      미래 입사일 변경은 이미 부여된 연차를 줄이지 않으므로
      <strong>옵션1·2가 동일하게 동작</strong>합니다(회수 없음).
    </p>
  </div>

  <div
    class="hire-date-pop__warn-box hire-date-pop__warn-box--warn"
    v-show="isBoundaryChange"
  >
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
         stroke-linecap="round" stroke-linejoin="round">
      <path d="M12 2v20M2 12h20" />
    </svg>
    <p class="hire-date-pop__warn-text">
      근속 <strong>1년 경계</strong>가 변경됩니다. 기존 월차는 유지되고,
      부족한 연차는 <strong>법정 연차</strong>로 추가 부여됩니다.
    </p>
  </div>

  <div
    class="hire-date-pop__warn-box hire-date-pop__warn-box--danger"
    v-show="handlingType === 'RESET_ALL'"
  >
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
         stroke-linecap="round" stroke-linejoin="round">
      <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
      <line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" />
    </svg>
    <p class="hire-date-pop__warn-text">
      <strong>주의</strong>: 기존 발생 이력이 모두 취소(소프트)됩니다.
      <strong>근로자 동의</strong> 또는 <strong>입력 오류 정정</strong>에 한해 사용하세요.
      사용한 연차가 있으면 별도 처리가 필요합니다.
    </p>
  </div>

  <div class="hire-date-pop__warn-box hire-date-pop__warn-box--warn">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
         stroke-linecap="round" stroke-linejoin="round">
      <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
      <line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" />
    </svg>
    <p class="hire-date-pop__warn-text">
      <strong>참고</strong>: 이미 부여된 연차의 유효기간(만료일)은 발생일 기준으로 보존됩니다.
      입사일과 변경 이력만 저장되며, 실제 연차 부여(소급/취소/재발급)는
      사용자 연차관리(Attd_09)의 '정책 기준 부여'에서 처리됩니다.
    </p>
  </div>
</div>
```

```js
// ===== script setup 추가/변경분 (developer가 채울 자리) =====
// 기존 import/ref 유지. 아래는 신규 자리만.

// impact reactive 에 옵션별 시뮬 + FISCAL 발생예정 필드 추가
// const impact = reactive({
//   scenarioLabel: "", existingGrantText: "", usedText: "",
//   missingGrantText: "", nextGrantText: "", changeSummaryText: "",
//   options: [],            // BE-3: [{ code, addDays, reclaimDays, finalHoldDays, reclaimNote }]
//   fiscalNextGrantText: "" // BE-3: FISCAL 정책 시 "본연차 다음 회계연도(YYYY-MM-DD) 발생 예정"
// });

// 옵션 코드로 시뮬 객체 조회 (BE-3 응답 options 배열에서 find)
const optionSim = (code) => {
  // TODO(developer): impact.options 에서 code 일치 항목 반환(없으면 null)
  return null;
};

// 미니카드 표시용 텍스트 (일수 → "N일"). 값 없으면 "-".
const simAddText = (code) => {
  // TODO(developer): optionSim(code).addDays 를 "N일"로 포맷(0이면 "0일")
  return "-";
};
const simReclaimText = (code) => {
  // TODO(developer): optionSim(code).reclaimDays 를 "N일"로 포맷
  return "-";
};
const simFinalText = (code) => {
  // TODO(developer): optionSim(code).finalHoldDays 를 "N일"로 포맷
  return "-";
};
// 회수표시 문구 (옵션1·2: 미래변경으로 회수돼야 할 N일 미회수 안내). 없으면 "".
const simReclaimNote = (code) => {
  // TODO(developer): optionSim(code).reclaimNote 반환(서버가 내려준 문구 그대로)
  return "";
};

// 변경 방향/경계 플래그 (안내문구 노출용) — scenarioLabel 또는 BE-3 응답 기반
const isFutureChange = computed(() => {
  // TODO(developer): impact.scenarioLabel 에 "미래" 포함 여부 등으로 판정
  return false;
});
const isBoundaryChange = computed(() => {
  // TODO(developer): 변경 전후 근속 1년 경계 교차 여부(BE-3 응답 플래그 권장)
  return false;
});
```

```css
/* ===== <style scoped> 추가분 (기존 CSS 변수/명명 규칙 준수, !important 금지) ===== */

/* FISCAL 다음 회계연도 발생예정 줄 */
.hire-date-pop__fiscal-note {
  margin-top: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--color-bg, #f9fafb);
  border: 1px dashed var(--color-primary, #16a34a);
  border-radius: var(--input-radius, 10px);
  font-size: 0.6875rem;
  color: var(--color-primary, #16a34a);
}
.hire-date-pop__fiscal-note svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

/* 옵션별 시뮬 미니카드 */
.hire-date-pop__option-sim {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  margin-top: 0.5rem;
  padding: 0.5rem 0.625rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
}
.hire-date-pop__sim-item {
  display: inline-flex;
  align-items: baseline;
  gap: 0.25rem;
  font-size: 0.6875rem;
  color: var(--color-text-muted, #4b5563);
}
.hire-date-pop__sim-item strong {
  font-size: 0.8125rem;
  font-weight: 600;
}
.hire-date-pop__sim-add {
  color: var(--color-primary, #16a34a);
}
.hire-date-pop__sim-reclaim {
  color: var(--color-danger, #ef4444);
}
.hire-date-pop__sim-final {
  color: var(--color-text-strong, #111827);
}
.hire-date-pop__sim-reclaim-note {
  display: block;
  margin-top: 0.375rem;
  font-size: 0.6875rem;
  line-height: 1.5;
  color: var(--color-warning-text, #b45309);
}
.hire-date-pop__option.is-selected .hire-date-pop__option-sim {
  border-color: var(--color-primary, #16a34a);
}
```

---

## 4. 주요 리스크 / 결정 포인트 (developer가 반드시 결정·검증)

1. **차액 보전과 기존 컴포넌트(당기/백필/월차) 이중부여** (BE-1 최대 리스크): BACKFILL 경로에서 "당기 ANNUAL/TENURE + 컴포넌트 백필"을 끄고 차액 단건만 부여할지, 아니면 차액 = 목표누적 − 기존누적 − 당기신규부여로 보정할지. planner 권장 = **BACKFILL이면 차액 단건만**(가장 단순·정답표 정합). 단 월차(BE-2 게이트 통과분)는 별도 유지.
2. **보전 멱등키 네임스페이스** (`_BF{histId}`): prafta-029의 `countActiveBySuffixVariant`(표준키 클릭 시 변형키 ACTIVE면 기부여 간주)와 충돌하지 않게 설계. 보전키는 keySuffix 비어있지 않아 해당 검사 비대상이나, 표준키 당기 ANNUAL과 같은 (연도)로 잡히지 않도록 BACKFILL일 때 표준 당기 부여를 끄는 게 안전.
3. **AVAIL_TO_DATE(소멸일) 산정** (BE-1): "발생일 + AXIS6"(정답표 정합, 권장) vs "today + AXIS6"(현행 당기와 동일, 단순). 정답표 #1 소멸 2027-01-01 검증으로 확정.
4. **월차 게이트 범위** (BE-2): "본연차 실제 발생 시"로만 묶어야 FISCAL crossed==0 부분기간 월차 공백이 안 난다. "산정근속≥12"로 묶으면 안 됨(결정문서 D2/D3).
5. **엔드포인트 확장 vs 신규** (BE-3): planner 권장 = 기존 `hire-date-impact` GET 확장(FE 호출부 1곳). 응답 DTO `options` 하위 구조화. 신규 분리는 비용만 증가.
6. **기존 "기존 부여누적" 합계 SQL** (BE-1): live(STATUS!='CANCELED' AND DEL_YN='N') STATUTORY_ANNUAL+TENURE_BONUS의 GRANT_DAYS 합(USED 포함, 월차 제외). 기존 `selectUserStatutoryLeaveSummary`는 월차도 합산하므로 그대로 못 씀 → 본연차/가산 한정 신규 SQL 필요. SELECT * 금지·leading comma·#{} 준수, 스키마는 schema-full.sql/MCP로 컬럼 확인.
7. **FISCAL 비례·NEXT_YEAR_BULK의 "새 기준 누적"**: BE-1 "새 기준 누적"은 현행 entitlement(당기) + computeBackfillPeriods(과거 유효분)를 read-only로 합산해 산정해야 #7/#13(+2)가 맞는다. 유효기간 12개월 고정이라 과거 회계연도분은 소멸 제외되지만 "오늘 보유했어야 할 양"은 최신 발생분(=17)이므로 차액이 기존(15) 대비 +2로 떨어진다 — 산식이 "유효 내 누적"임을 테스트로 고정.
8. **prafta-029 회귀 동시 통과**: RESET_ALL 회차키 누수/재활성화 가드를 BE-1이 깨지 않는지 PRAFTA-030-5에서 prafta-029 시나리오 함께 검증.

---

## 5. 작업ID 목록 + 권장 착수 순서 요약

| 순서 | 작업ID | 유형 | 제목 | 선행 |
|---|---|---|---|---|
| 1 | PRAFTA-030-1 | backend | BE-2 월차 게이트(본연차 발생 시 월차 미발생) | 없음 |
| 2 | PRAFTA-030-2 | backend | BE-1 옵션1 차액 보전 재설계 ★핵심 | 030-1 |
| 3 | PRAFTA-030-5 | backend(test) | 정답표 18케이스+경계A/B 결정적 테스트 | 030-2, 030-3 |
| 4 | PRAFTA-030-3 | backend | BE-3 옵션별 시뮬 미리보기 API(read-only) | 030-1, 030-2 |
| 5 | PRAFTA-030-4 | frontend-screen | FE-1 HireDateEditPop 옵션별 미리보기/회수표시/안내 | 030-3 |
| 6 | PRAFTA-030-6 | backend(docs) | 가이드/정책서/CHANGELOG 갱신 | 030-2, 030-3 |

> 5개 초과(6개)이나 본 작업은 단일 결정문서(D1~D5)의 분해이며 mixed가 아닌 단위 작업들이라 함께 제시한다. Notion 등록은 main 세션 몫.
