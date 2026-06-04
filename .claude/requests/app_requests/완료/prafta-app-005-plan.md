# prafta-app-005 분해 plan — 모바일 앱 "연차 현황"(본인 잔여연차 상세)

작성: planner | 기준일: 2026-05-29 | 영역: app(webview Vue) + backend(공통 1벌)

본 문서는 요청서 `refs/prafta-app-005/prafta-request-my-leave.md`(planner 수준 초안)를
**실제 스키마 + 기존 구현(home01, 웹 LeaveDashboard, leaveflow)에 맞춰 검증·교정**하고
개발 착수 가능 상태로 만든 산출물이다. 코드(백엔드/JS 로직)는 작성하지 않는다(분해·명세·Vue 골격까지).

---

## 0. 핵심 결론 (먼저 읽을 것)

### 0-1. "이중 차감" 우려는 코드 확인 결과 **실재한다**. 본 화면의 §2.3 공식을 폐기/교정해야 한다.

요청서 §2.3은 `remaining = granted - used - planned`, `planned = 승인+대기 미래신청`이라고 정의했다.
그러나 코드 확인 결과 **PLANNED는 이미 USED_DAYS 안에 들어 있다**. 그대로 또 빼면 이중 차감이다.

확인 경로(코드 사실, 추측 아님):
- `LeaveFlowServiceImpl.submitLeave()` (line 199~209): 연차 **신청 시점에** `TB_USER_LEAVE_USE` 행을
  `LEAVE_STATUS='CONFIRMED'`로 **즉시 INSERT**하고, `recomputeGrantUsedDays()`로
  `TB_USER_LEAVE_GRANT.USED_DAYS = SUM(LEAVE_DAYS where LEAVE_STATUS='CONFIRMED')`로 동기화한다.
  → **결재 대기('01')든 승인('02')이든, 미래 휴가일이든 무관하게 USED_DAYS에 즉시 반영**된다.
- 반려 시(`rejectStep`, line 293~297): `cancelLeaveUseByReqId`로 `LEAVE_STATUS='CANCELLED'` 후
  `recomputeGrantUsedDays`로 USED_DAYS 원복 → 잔여가 다시 늘어난다(요청서 §2.3 마지막 줄 UX와 일치).
- 즉 USED_DAYS의 **정의 = "취소되지 않은 모든 차감(과거 사용분 + 미래 예정분)"**.

→ 따라서 home01의 `remaining = SUM(GRANT_DAYS - USED_DAYS)`는
   **이미 `부여 - (과거사용 + 미래예정)`** 이다. 본 화면이 의도하는 잔여와 동일 개념이다.

### 0-2. 웹 "연차 현황"(LeaveDashboardMapper)이 그룹/수치 정의의 **단일 출처(SSOT)**. 그대로 차용한다.

웹 대시보드의 직원별 행/상세는 이미 이 화면이 필요로 하는 정의를 정확히 갖고 있다:

| 개념 | 웹 LeaveDashboardMapper 정의 (그대로 채용) |
|---|---|
| 그룹 분류 | `GRANT_TYPE LIKE 'STATUTORY\_%'` = 법정, `LIKE 'MANUAL\_%'` = 법정외 |
| 활성 부여 | `STATUS='ACTIVE' AND DEL_YN='N'` |
| 부여(granted) | `SUM(GRANT_DAYS)` (활성·그룹) |
| 사용+예정 합계(usedTotal) | `SUM(USED_DAYS)` (활성·그룹) ← 과거사용 + 미래예정 모두 포함 |
| 예정(scheduled/planned) | `SUM(TB_USER_LEAVE_USE.LEAVE_DAYS)` where `LEAVE_STATUS='CONFIRMED' AND START_DATE > 오늘`, GRANT_ID로 활성·그룹 부여에 조인 (usedTotal의 부분집합) |
| 순수 사용(used, 과거분) | `usedTotal - scheduled` |
| 잔여(remaining) | `granted - usedTotal` ( = granted - used - scheduled, 이중차감 없음) |

→ **본 화면의 4종 수치를 다음으로 확정 교정한다**(요청서 §2.3 → 아래로 대체):
  - `granted` = SUM(GRANT_DAYS) (활성·그룹)
  - `usedTotal` = SUM(USED_DAYS) (활성·그룹) — **내부값**
  - `scheduled`(=화면 "사용예정") = USED_DAYS 중 START_DATE 미도래 CONFIRMED 분
  - `used`(=화면 "사용") = `usedTotal - scheduled` (과거/오늘까지 확정 소비분)
  - `remaining` = `granted - usedTotal` = `granted - used - scheduled`
  - `usageRate` = round(`used` / `granted` * 100), granted=0이면 0
    - ⚠️ 결정질문 Q5: 사용률 분자를 `used`(순수사용)로 할지 `usedTotal`(예정포함)로 할지 확정 필요.
      시안 케이스1(부여20/사용6/예정2/사용률40%)은 6/15? 6/20? → 6/15≈40%(법정), 6/20=30%.
      케이스1은 **전체** 토글인데 40%이므로 분자=used(6), 분모=??? 불일치. 아래 Q5 참조.

### 0-3. home01 KPI와의 정합(§3.7)은 **자동으로 성립**한다.

home01 `remainingDays = SUM(GRANT_DAYS - USED_DAYS)` (전체, ACTIVE, EXPIRE_YN='N').
본 화면 TOTAL.remaining = `SUM(GRANT_DAYS) - SUM(USED_DAYS)` (전체, ACTIVE) = 동일식.
home01 `grantedDays = SUM(GRANT_DAYS)` = 본 화면 TOTAL.granted.
→ 두 값은 같은 테이블·같은 필터로 산출되므로 **별도 보정 없이 일치**. (단 EXPIRE_YN 차이는 Q3 참조)

---

## 1. 데이터 모델 / API 계약 (교정 확정본)

### 1-1. 엔드포인트 (요청서 §5.1 교정)

요청서의 `GET /api/app/leave/my/summary` 는 본 프로젝트 app 규약과 불일치. 교정:

- **실제 매핑 경로**: `GET /prafta/appApi/leave01/my-leave-summary`
  - 자동 프리픽스: `com.prafta.app.*` 패키지 → `/prafta/appApi` (ApiPrefixConfig, home01과 동일)
  - 컨트롤러 `@RequestMapping("/leave01")` + `@GetMapping("/my-leave-summary")`
  - axios 호출(baseURL=`/prafta`): `api.get('/appApi/leave01/my-leave-summary')`
- **신규 모듈**: `com.prafta.app.leave.leave01` (기존 app 모듈 attd01/home01/chkLst01/risk01 관례 동일)
  - ⚠️ 모듈명 `leave01` 제안. 기존 app에 leave 모듈 없음(신규). 확정 질문 불필요(관례상 자명)하나
    사용자가 다른 명칭 선호 시 교체 가능.
- 요청 파라미터 없음. 식별값은 JWT 클레임에서(`jwtUtil.getAllClaimsAsMap` → TokenInfo → userCd/cmpnyCd).
  바디/쿼리로 userCd 받지 않음 (home01/chkLst01 패턴 강제).

### 1-2. 응답 JSON (교정 확정본)

요청서 §5.1 JSON을 스키마/실계산에 맞춰 교정. `used`/`planned` 정의 명확화 + serviceCreditMonths 출처 확정.

```json
{
  "user": {
    "userNm": "김여진",
    "hireDate": "20240918",
    "serviceMonths": 20,
    "serviceCreditMonths": 0
  },
  "groups": {
    "TOTAL":         { "granted": 20.0, "used": 6.0, "planned": 2.0, "remaining": 12.0, "usageRate": 40 },
    "STATUTORY":     { "granted": 15.0, "used": 6.0, "planned": 2.0, "remaining": 7.0,  "usageRate": 40 },
    "NON_STATUTORY": { "granted": 5.0,  "used": 0.0, "planned": 0.0, "remaining": 5.0,  "usageRate": 0  }
  },
  "expiringSoon": {
    "exists": true,
    "daysUntilExpiry": 3,
    "totalRemainingDays": 5.0,
    "expiryDate": "20260601"
  }
}
```

필드 교정 사항:
- `user.userNm`: `TB_USER.USER_NM`(평문 varchar, PII 평문이지만 본인 자기조회 → 노출 허용. 단 로그 금지).
- `user.hireDate`: `TB_USER.HIRE_DATE` (YYYYMMDD 문자열). FE에서 `2024-09-18`로 포맷.
  - ⚠️ 요청서는 `2024-09-18`(하이픈)으로 적었으나 DB는 YYYYMMDD. **서버는 YYYYMMDD 원본 전달, FE 포맷** 권장.
- `user.serviceMonths`: 입사일~오늘 개월수. **서버 계산**(LocalDate.between). 경력인정 미포함(실근속).
- `user.serviceCreditMonths`: `SUM(TB_USER_SERVICE_CREDIT.CREDIT_MONTHS) WHERE USE_YN='Y'`
  (웹 LeaveDashboardMapper.selectCreditMonths 동일). **존재 확인됨**(웹에서 사용 중).
- `groups.*.granted/used/planned/remaining`: §0-2 정의. decimal → double, 소수1자리 반올림(home01 toScaledDouble 패턴).
- `groups.*.usageRate`: Q5 확정 후. (잠정 round(used/granted*100))
- `expiringSoon`: §0-4 참조.

### 1-3. 계산 로직 (developer/백엔드 개발자 전달용 — SQL 차원)

그룹 그룹별로 다음을 산출(STATUTORY/NON_STATUTORY 각각, TOTAL은 두 그룹 합):

```
granted   = SUM(G.GRANT_DAYS)   WHERE G.USER_CD=:userCd AND G.CMPNY_CD=:cmpnyCd
                                  AND G.STATUS='ACTIVE' AND G.DEL_YN='N'
                                  AND G.GRANT_TYPE LIKE :prefix      -- 'STATUTORY\_%' / 'MANUAL\_%'
usedTotal = SUM(G.USED_DAYS)    (동일 WHERE)
scheduled = SUM(LU.LEAVE_DAYS)  FROM TB_USER_LEAVE_USE LU
                                  JOIN TB_USER_LEAVE_GRANT G ON G.CMPNY_CD=LU.CMPNY_CD AND G.GRANT_ID=LU.GRANT_ID
                                    AND G.STATUS='ACTIVE' AND G.DEL_YN='N' AND G.GRANT_TYPE LIKE :prefix
                                  WHERE LU.USER_CD=:userCd AND LU.CMPNY_CD=:cmpnyCd
                                    AND LU.LEAVE_STATUS='CONFIRMED' AND LU.DEL_YN='N'
                                    AND LU.START_DATE > :todayYmd
used      = usedTotal - scheduled
remaining = granted - usedTotal
usageRate = (granted=0) ? 0 : ROUND(used / granted * 100)   -- Q5 확정 후
```
- LIKE 언더스코어 이스케이프: `'STATUTORY\_%'`, `'MANUAL\_%'` (웹과 동일 규칙).
- TOTAL은 prefix 필터 없이(또는 STATUTORY+MANUAL UNION) 동일 산출. (`SYS_*` 외 prefix 출현 시 Q4)
- 기준 오늘(todayYmd): `DATE_FORMAT(NOW(), '%Y%m%d')` 서버 1회 산출(home01 selectTodayYmd 패턴).

### 1-4. expiringSoon (소멸 임박) 계산 (요청서 §3.3 + 정책 §15.1 정합)

```
대상 = TB_USER_LEAVE_GRANT G
       WHERE G.USER_CD=:userCd AND G.CMPNY_CD=:cmpnyCd AND G.STATUS='ACTIVE' AND G.DEL_YN='N'
         AND (G.GRANT_DAYS - G.USED_DAYS) > 0                          -- 잔여 > 0 (§3.3)
         AND G.AVAIL_TO_DATE BETWEEN :todayYmd AND :today+30d           -- 30일 이내 소멸 (§3.3, 웹 expiringSoon30 동일)
exists              = (대상 1건 이상)
daysUntilExpiry     = DATEDIFF(MIN(AVAIL_TO_DATE), today)               -- 가장 임박한 D-day
totalRemainingDays  = SUM(GRANT_DAYS - USED_DAYS)  (대상 전체)
expiryDate          = MIN(AVAIL_TO_DATE)                                 -- UI 미표시, 디버깅/확장용
```
- 그룹 토글과 무관하게 STATUTORY+MANUAL 전체 합산(§3.4: 콜아웃은 "전체" 단위 1회).
- AVAIL_TO_DATE = 소멸일(YYYYMMDD). 정책 §8.5.6/§8.5.4 부여 시 산정되어 컬럼에 저장됨(존재 확인됨).
- 정책 출처: `attd/15-notifications.md` §15.1(만료 리마인더) + `attd/08-leave.md` §8.5.8(STATUS/소멸).

---

## 2. 작업 단위 분해

작업 ID는 메인 세션이 Notion "작업 로그" 최대 PRAFTA-{N} 조회 후 채번. 본 문서는 슬롯 표기.

| 슬롯 | 유형 | 영역 | 모듈 | 산출물 | 선행 |
|---|---|---|---|---|---|
| A | backend | app | app/leave/leave01 | `GET /appApi/leave01/my-leave-summary` (Controller/Service/Mapper/DTO) | 없음 |
| B | frontend-screen | app | views/leave | `MyLeaveSummaryView.vue` (컨테이너 + 그룹토글 상태 + API 연동 위임) | A |
| C | frontend-component | app | views/leave/components | `LeaveGroupToggle.vue` (전체/법정/법정외 세그먼트) | B |
| D | frontend-component | app | views/leave/components | `LeaveExpiryCallout.vue` (소멸임박 콜아웃 + 세션닫기) | B |
| E | frontend-component | app | views/leave/components | `LeaveBalanceCard.vue` (메인 잔여 + 진행바 + 범례) | B |
| F | frontend-component | app | views/leave/components | `LeaveSplitKpi.vue` (3분할 부여/사용/사용예정) | B |
| G | frontend-component | app | views/leave/components | `LeaveMetaCard.vue` (입사일/근속/사용률) | B |

> 5개 초과지만 동일 화면의 분리 컴포넌트라 1차 일괄 분해(planner 규칙상 "작업 5개 초과 시 1차 5개"는
> 독립 작업 기준. 본 건은 1화면 1백엔드의 강결합 묶음이므로 함께 둔다). 메인 세션 판단으로 C~G를
> 단일 작업으로 묶어 등록해도 무방.

### 슬롯별 상세

**A (backend)** — `[backend]`
- 핵심: 1) 신규 모듈 `com.prafta.app.leave.leave01` 2) §1-1 엔드포인트 3) §1-3 계산 SQL(그룹3종)
  4) §1-4 expiringSoon 5) user 메타(userNm/hireDate/serviceMonths/serviceCreditMonths)
- 영향 파일(신규):
  - `app/leave/leave01/controller/AppLeave01Controller.java`
  - `app/leave/leave01/service/AppLeave01Service.java` + `impl/AppLeave01ServiceImpl.java`
  - `app/leave/leave01/mapper/AppLeave01Mapper.java` + `resources/.../AppLeave01Mapper.xml`
  - `app/leave/leave01/dto/response/MyLeaveSummaryResponse.java`
  - `app/leave/leave01/application/{param,query}/...` (home01 패턴: TokenInfo→Param→Query+todayYmd)
  - `app/leave/leave01/result/*Result.java` (그룹집계/만료/유저 result)
- endpoint: `GET /appApi/leave01/my-leave-summary`
- 정책 출처: `attd/08-leave.md` §8.5.1(STATUTORY/MANUAL 분류)·§8.5.6·§8.5.8(STATUS/소멸/멱등),
  `attd/15-notifications.md` §15.1(D-30 만료). 기존구현 정합: `LeaveDashboardMapper`(SSOT) 차용.
- ⚠️ 보안: 본인 자기조회만(JWT userCd). 타인 userCd 주입 불가(파라미터 미수신). cross-company 차단(cmpnyCd 클레임).
  PII(USER_NM 평문) 응답 포함되나 본인 한정 + 로그 금지.

**B (frontend-screen)** — `[frontend-screen]` `[UI 명세: UI-A005]`
- 핵심: 1) 헤더(연차 현황)+그룹토글+콜아웃+메인카드+3분할+메타+푸터 조립
  2) onMounted 시 `GET /appApi/leave01/my-leave-summary` 1회(캐시 없음, §3.6)
  3) 그룹 토글 상태(activeGroup) 관리 + 자식에 groups[activeGroup] 주입(추가 API 없음)
  4) 콜아웃 세션닫기 상태 5) 푸터 [연차 신청하기] 활성/비활성 + (TODO)연차신청 라우팅
- 영향 파일(신규): `views/leave/MyLeaveSummaryView.vue`
- 의존 endpoint: A
- developer 보완(TODO): API 호출 body 채우기, 라우팅(연차신청 폼·뒤로·홈), 로딩/에러, 진입연동
  (MainView `onLeaveClick` → 본 화면 push로 교체).

**C~G (frontend-component)** — `[frontend-component]`
- C `LeaveGroupToggle.vue`: props `modelValue`(TOTAL/STATUTORY/NON_STATUTORY), emit update. role=tablist.
- D `LeaveExpiryCallout.vue`: props `info`(expiringSoon), emit `close`. 본 화면 "전체" 토글에서만 부모가 렌더.
- E `LeaveBalanceCard.vue`: props `group`(granted/used/planned/remaining), `label`. 진행바3분할+범례.
- F `LeaveSplitKpi.vue`: props `group`. 부여/사용/사용예정 3셀. 0값 muted.
- G `LeaveMetaCard.vue`: props `user`(hireDate/serviceMonths/serviceCreditMonths), `usageRate`.
- 정책 출처(C~G 공통): UI 정책(요청서 §4) + `attd/08-leave.md` §8.7류 사용단위 표기는 일단위만(§3.8).

---

## 3. 기존 웹/home01과 배치되는 부분 + 사용자 결정 질문 (요구 #3)

아래는 **개발 착수 전 사용자 컨펌이 필요한 항목**. Notion 등록은 메인 세션이 컨펌 반영 후 수행.

### [배치 1 — 해소됨] §2.3 "remaining = granted - used - planned" vs home01 / 이중차감
- **결론**: 요청서 §2.3을 그대로 구현하면 이중차감(PLANNED가 USED_DAYS에 이미 포함). §0 교정안으로 대체.
- **조치**: 화면 표시는 시안 그대로(사용/사용예정/잔여 3분할 유지) 두되, 서버 계산을
  `used = usedTotal - scheduled`, `remaining = granted - usedTotal`로 정의. 시각·합산 모두 시안과 일치.
- 사용자 확인만 필요(추가 결정 없음): "이 교정안으로 진행" 동의 여부.

### [Q1] 활성 STATUS 집합 — EXHAUSTED 포함 여부
- 요청서 §3.1은 "STATUS=ACTIVE 또는 IN_USE"라 했으나 `IN_USE`는 실재 안 함(SYS040: ACTIVE/EXHAUSTED/EXPIRED/CANCELED).
- home01·웹 대시보드 모두 **`STATUS='ACTIVE'`만** 집계(EXHAUSTED 제외).
- EXHAUSTED(잔여0 소진완료) 부여는 granted/used에 더하면 합계가 커지나 잔여는 0 기여.
- **질문**: 본 화면도 `STATUS='ACTIVE'`만(home01·웹과 동일)으로 갈지? (권장: 동일). EXHAUSTED를
  부여/사용 누적에 포함하면 "올해 총 부여/총 사용" 누계 표시는 풍부해지나 home01 KPI와 어긋난다.
  → **정합 우선이면 ACTIVE만**. 확정 요청.

### [Q2] "사용예정(planned)"이 USED_DAYS와 겹치는지 (확정용)
- 코드상 planned ⊂ usedTotal(USED_DAYS). 본 화면 used = usedTotal - planned로 분리 표시.
- **질문**: 시안 케이스1 "부여20 / 사용6 / 사용예정2 / 잔여12"는 `12 = 20-6-2`로 정합.
  이 표기(used=과거확정6, planned=미래예정2)가 의도와 맞는지 확인. (맞으면 §0 교정안 그대로)

### [Q3] home01과의 EXPIRE_YN 필터 차이
- home01: `STATUS='ACTIVE' AND EXPIRE_YN='N'`. 웹 대시보드: `STATUS='ACTIVE'`만(EXPIRE_YN 미사용).
- 정책 §8.5.8: ACTIVE면 EXPIRE_YN='N'이 정상(동기화), 이론상 둘은 동치. 단 동기화 미완 행이 있으면 미세 차이.
- **질문**: 본 화면은 어느 쪽? **home01과 100% 일치를 보장하려면 home01과 동일하게
  `STATUS='ACTIVE' AND EXPIRE_YN='N'` 채택** 권장. (웹 대시보드는 관리자용이라 약간 다름)
  → 권장: home01과 동일 필터. 확정 요청.

### [Q4] GRANT_TYPE prefix 분류 (요청서 §6.1 교정 확인)
- 요청서 §6.1은 STATUTORY_/MANUAL_/`BONUS_`로 분류했으나 **`BONUS_` prefix는 실재 안 함**.
  실제(SYS035): 법정=`STATUTORY_*`(ANNUAL/MONTHLY/TENURE_BONUS), 법정외=`MANUAL_*`(BONUS/CONDOLENCE/LONG_SERVICE/OTHER).
- **조치**: 분류는 `STATUTORY_` vs `MANUAL_` 2분기만. `STATUTORY_TENURE_BONUS`는 이름에 BONUS 있어도 **법정**.
- **질문**(확인): 위 2분기로 확정 동의? (웹 대시보드와 동일). prefix가 둘 다 아닌 값이 DB에 있으면
  TOTAL에는 포함되나 어느 그룹에도 안 들어가는 누락 가능 → TOTAL을 prefix무관 전체합으로 둘지(권장),
  아니면 STATUTORY+NON_STATUTORY 합으로 둘지. **권장: TOTAL = prefix무관 활성 전체합**(home01 정합).

### [Q5] 사용률(usageRate) 분자/분모 정의
- 시안 케이스1: 전체 토글에서 "사용률 40%". 케이스1 전체 부여20·사용6 → 6/20=30%이지 40% 아님.
  6/15≈40%는 법정(부여15) 기준. 즉 **시안의 40%는 used/granted 계산과 불일치**(시안 표기 오류로 보임).
- **질문**: 사용률 = `used / granted` (순수사용/부여) 인지, `usedTotal / granted`(예정포함/부여)인지,
  분모를 그룹 부여로 할지. 권장: **그룹별 `round(usedTotal / granted * 100)`**(예정 포함이 "소진 진척"에
  더 부합) 또는 `round(used/granted*100)`. 시안 수치는 참고만 하고 정의를 확정해 주세요.
  - 메타카드 "사용률"은 그룹 토글 영향 받는지도 확인 필요(요청서 §3.2는 "사용률도 그룹 집계로 갱신",
    그러나 §4.6 메타는 "그룹 무관 동일"이라 했고 사용률을 메타에 둠 → **모순**). → 사용률을 그룹별로
    바꿀지(§3.2) 전체 고정할지(§4.6) 확정 요청. 권장: 사용률은 **현재 토글 그룹 기준**(§3.2 우선).

### [Q6] serviceMonths(근속) 계산 — 경력인정 포함 여부
- 요청서 §4.6 "근속 1년 8개월" + "경력 인정 N개월" 보조 라벨(별도). 즉 근속=실근속, 경력인정=별도 표기.
- **질문**(확인): `serviceMonths`=실근속(입사일~오늘), `serviceCreditMonths`=경력인정 합(별도 표시)으로
  분리 확정 동의? (정책 §1.5 근속 환산은 common 정책서 — 본 화면은 입사일~오늘 단순 환산으로 충분).
  경력인정 0이면 보조 라벨 숨김(요청서 §4.6).

### [Q7] 진입 동선 + Pull-to-refresh + 푸터 라우팅 (요청서 §1.2/§3.5/§3.6)
- 진입: MainView `AttendanceSummaryCard @click:leave` → 현재 `onLeaveClick`이 "준비 중" stub.
  **본 화면이 그 목적지**로 확정 동의? (라우트명 제안: `/MyLeaveSummaryView`, viewResolver 규약상 컴포넌트명 직결).
- 푸터 [연차 신청하기] 목적지: 연차 신청 폼 화면은 **아직 앱 FE 미구현**(MainView onLeave클릭도 stub).
  → 본 화면 푸터도 **TODO(developer) 라우팅 보류**로 두고 "준비 중" 폴백. 확정 요청.
- Pull-to-refresh: 기존 MyAttendanceView 등에 미구현. **본 화면도 미지원**(진입 1회 GET)으로 일관 확정 권장.

### [Q8] 콜아웃 닫기 hit area 32px (요청서 §4.3/§8.1)
- 디자인 가이드 최소 44px 미달(32px). 시안대로 32px 절충 vs 44px 강제.
- **질문**: 시안 32px 유지 동의? (콜아웃이 충분히 크므로 절충 권장). aria-label="닫기" 필수.

### [참고 — 질문 아님] app FE 스타일 체계
- app FE는 Tailwind 설정이 있으나, 기존 화면(MyAttendanceView/AttendanceTodayCard 등)은
  **scoped CSS + 뷰 루트에 디자인 토큰(CSS 변수) 1회 선언 → 자식 상속** 패턴을 쓴다.
  본 화면도 동일 패턴 채용(Tailwind 미사용, 하드코딩 금지, 토큰만). 골격 §4 참조.

---

## 4. 정책서 출처 매핑 (Notion 상세설명용)

| 화면 영역 | 정책 출처 |
|---|---|
| 그룹 분류(법정/법정외) | `attd/08-leave.md` §8.5.1 (STATUTORY_/MANUAL_ prefix) |
| 활성 부여/STATUS/소멸 | `attd/08-leave.md` §8.5.8 (STATUS 4종, 멱등, 기부여보호) |
| 소멸 임박 D-30 | `attd/15-notifications.md` §15.1 (만료 리마인더) + §8.5.6/§8.5.4(소멸일 산정) |
| 사용단위/일단위 표기 | `attd/08-leave.md` §8.5.9 (사용단위), §8.2~8.4 |
| 연차 신청(푸터 동선) | `attd/09-requests-approval.md` §9.4 (휴가 신청) |
| 기존구현 정합 SSOT | `common.cmm.leave.LeaveDashboardMapper`(웹 연차현황), `app.home.home01`(KPI) |

---

## 5. 산출물 체크리스트

- [x] 요청서 정독 + 스키마/기존구현 교정 (§0~§1)
- [x] 이중차감/잔여정의 충돌 코드 확인 + 교정안 (§0-1, 배치1)
- [x] 엔드포인트 네이밍 app 규약 교정 (§1-1)
- [x] 웹 LeaveDashboard / home01 정합 (§0-2, §0-3)
- [x] 작업 분해 7슬롯 + 선행관계 (§2)
- [x] 결정 질문 Q1~Q8 (§3)
- [x] 정책 출처 매핑 (§4)
- [x] UI 명세 → `prafta-app-005-ui-spec.md`
- [x] Vue 골격 6파일 (script TODO 마커만)
- [ ] (메인 세션) Q1~Q8 사용자 컨펌 → Notion "작업 로그"/"도메인 지식 베이스" 등록
