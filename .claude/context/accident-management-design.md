# 사고관리(Accident Management) 도메인 — 설계 문서

> 상태: **설계 확정안(미구현)**. prafta-048 기획 논의 결과. 단일 출처.
> 채널: **웹 관리자 전용** (`com.prafta.web.accident`, `/webApi/accident01`). 앱 미대상.
> 참고: 요청서 `.claude/requests/web_requests/prafta-048.md`, 목업 `ref/prafta-048/accident_management_v2.html`, `사고관리_temp.png`.
> 선행/연계: 아차사고 `near-miss-incident-design.md`(`tb_near_miss`, 구현완료), 정책 `common/04-user-tracks.md`(일용직 트랙, 2026-06-05 정정).

---

## 0. 한 줄 정의

사고관리는 **실제 피해(부상·사망)가 발생한 재해**를 관리하는 신규 도메인이다. 현재 시스템에 사고 관련 테이블·화면이 전혀 없다(`tb_accident` 부재). 핵심 가치는 "사고 한 건을 등록하면, **사고일·발생시각 기준으로 근태·순회점검·위험성평가·TBM·아차사고 5개 기존 도메인을 자동 조회·확정(스냅샷)해 안전관리 맥락을 한 화면에 묶고**, 등급별 법정 처리 절차를 가이드한다"이다.

아차사고(`tb_near_miss`)와의 경계: 아차사고 = 피해 없는 전조 사건(이미 구현). 사고관리 = 실제 피해 발생 재해. 둘은 `SRC_NEAR_MISS_*`로 연계한다.

---

## 1. 연계 도메인 매핑 (실제 스키마 기준, 추측 아님)

| 도메인(수평선 원) | 테이블 | 사고일 매칭 키 | 조회 기간 |
|---|---|---|---|
| 근태 | `tb_user_work_plan`(스케줄) + `tb_user_attd_mgmt`(실근태) | `USER_CD`+`WORK_YMD`=발생일 | 당일 |
| 순회점검 | `tb_chkpt_type_mgmt`(점검대상) + `tb_chkpt_inspect_item`(항목) + `tb_chkpt_inspect_answer`(결과) | `SITE_CD`+`CHKPT_CD`+`WORK_DATE` | **1주일** |
| 위험성평가 | `tb_risk_assessment`(+`tb_risk_type`,`tb_risk_site_hazard`) | `SITE_CD`+`PROCESS_CD`/`RISK_TYPE_CD`/`HAZARD_CD` | **3개월** |
| TBM | `tb_tbm_session` + `tb_tbm_attendance` | `SITE_CD`+`DATE(OPENED_AT)`=발생일, 출결 `USER_CD`+`USER_TYPE_CD` | **당일 고정** |
| 아차사고 | `tb_near_miss` (구현완료) | `SITE_CD`+`OCCUR_DTIME`, 유형 `SYS061`/잠재중대성 `SYS062` | **3개월** |

조회 기간 차이는 각 원 화면에 **안내문구**로 명시(오해 방지, §6 참조).

### 1.1 분류 체계 교정(목업 vs 실제)
- 순회점검: 목업의 "점검구분"(HIGH_WORK 등)은 실제 base코드 **`COM001`**(체크리스트 타입)이다. 점검대상은 별도 등록 데이터 `tb_chkpt_type_mgmt.CHKPT_CD`/`CHKPT_NM`(화면 `ChkLst_01.vue` "점검대상명칭"). → 사고등록 팝업은 **점검구분(COM001) + 점검대상(CHKPT_CD)** 2단 + 점검대상 검색팝업(신규 `ChkptSearchPop`) 필요.
- 위험성평가: 분류는 **3계층 = 공정(`PROCESS_CD`) → 위험요인구분(`RISK_TYPE_CD`) → 유해요인(`HAZARD_CD`)**. 관리자는 0~3계층을 원하는 만큼만 입력(미입력 계층은 하위 전체). 목업의 "위험분류/위험발생상황" 2단 표기는 이 3계층으로 대체.
- 순회점검 결과값: `SYS009` = **`Y`(양호) / `N`(불량)** 2값. 목업 예시 "정상/미흡"은 양호/불량으로 매핑. 집계 표현 = "총 N항목 중 양호 X / 불량 Y + 불량항목 목록".

---

## 2. 신규 테이블 (DDL 초안 — planner/developer가 스키마 최종 확정)

### 2.1 `tb_accident` (사고 헤더)
- PK: `CMPNY_CD`, `SITE_CD`, `ACCIDENT_ID`(채번 `ACC`+YYYYMMDD+SEQ)
- 재해자: `VICTIM_USER_TYPE_CD`(SYS050 REGULAR/DAILY 재사용) + `VICTIM_USER_CD`
- `OCCUR_YMD`(varchar8), `OCCUR_TIME`(varchar4 HHMM), `OCCUR_PLACE`(varchar200)
- `ACCIDENT_GRADE_CD`(**SYS065** 재해등급), `ACCIDENT_DESC`(사고경위, varchar500↑)
- `EMPLOYER_DESC`(신고의무자, 직영/하수급 등 수동입력 가능)
- `SRC_NEAR_MISS_ID`(연관 아차사고, nullable) — 다건이면 link 테이블로
- `PROCESS_STATUS_CD`(**SYS066** 처리상태)
- 표준 감사컬럼(`USE_YN`/`DEL_YN`/`INSERT_*`/`UPDATE_*`)

### 2.2 `tb_accident_link` (연계 데이터 스냅샷 — 도메인당 N행)
- PK: `CMPNY_CD`, `ACCIDENT_ID`, `LINK_DOMAIN_CD`, `LINK_SEQ`
- `LINK_DOMAIN_CD`: ATTD / CHKPT / RISK / TBM / NEAR_MISS (신규 코드그룹 **SYS067** 또는 상수)
- `LINK_KEY_JSON`(연결 원본키 묶음) + `SNAPSHOT_JSON`(**사고 날짜·시각 기준 조회 시점 값 고정**)
- 사유: 위험성평가 등은 개선 재평가로 값이 변함 → 법적 정합성 위해 스냅샷 저장(목업 legend의 "사고 시점 정합성 보존"을 데이터로 보장).
- **다건 지원**: 점검대상·위험성평가·아차사고 모두 수평선/등록 화면에서 다건 확정 가능 → 도메인당 여러 행.

### 2.3 `tb_accident_legal_step` (법정 처리/기한 — 탭②)
- PK: `CMPNY_CD`, `ACCIDENT_ID`, `STEP_CD`
- `IS_DONE_YN`(조치완료 체크), `DONE_DTIME`, `REMARK`(항목별 비고, varchar500)
- 절차 정의(절차명·행동강령·근거조문·과태료·기한규칙)는 가변 텍스트가 길어 **seed 마스터**로 분리:

### 2.4 `tb_accident_legal_step_master` (seed, 등급별 절차 정의)
- PK: `STEP_CD`
- `ACCIDENT_GRADE_CD`(SYS065 적용 등급), `STEP_IDX`(순서), `STEP_NM`
- `ACTION_GUIDE`(행동강령 문구, 예: "재해개요·피해상황·조치를 관할 지방고용노동관서에 보고하세요")
- `LEGAL_BASIS`(근거조문/과태료), `DEADLINE_RULE_CD`(기한규칙: 지체없이/발생일+1개월/없음 등). ※ MONTH_PLUS_1 = **사고 발생일부터 1개월**(산안법 시행규칙 §73 "발생한 날부터 1개월 이내"). "발생월말+1개월"이 아님 — 2026-06-06 법령 확인 정정.
- 초기 seed = 목업 `buildActions()` 로직(중대재해/일반산재/신고제외 분기) 그대로 이관. ※ 법정 기한·조문은 **노무사 최종확인 대상**이며 화면에 면책 문구 표시.

---

## 3. 신규 코드그룹 (현재 SYS064까지 사용 → SYS065부터)

| 그룹 | 그룹명 | 코드값 |
|---|---|---|
| **SYS065** | 재해등급 | `100` 중대재해 / `200` 일반산재 / `300` 신고제외 |
| **SYS066** | 사고 처리상태 | `100` 접수 / `200` 처리중 / `300` 종결 |
| **SYS067**(선택) | 연계 도메인 구분 | `ATTD` 근태 / `CHKPT` 순회점검 / `RISK` 위험성평가 / `TBM` / `NEAR_MISS` 아차사고 — 상수로 둘 수도 있음 |

`tb_syst_val_m`(그룹) + `tb_syst_val_d`(코드값)에 등록. COMMENT 규칙(코드성 컬럼 '설명[SYS코드] 값:의미') 준수.

---

## 4. 신규 메뉴 / 권한 데이터

### 4.1 대메뉴 `tb_syst_menu_m`
| MENU_M_ID | MENU_SRC | MENU_NM | MENU_IDX | USE_YN |
|---|---|---|---|---|
| **`accident`** | `001`(웹, SYS007) | 사고관리 | **8** (nearMiss=7 다음) | Y |

### 4.2 소메뉴 `tb_syst_menu_d`
| MENU_M_ID | MENU_D_ID | MENU_VIEW | MENU_NM | MENU_IDX |
|---|---|---|---|---|
| accident | **`Accident_01`** | `accident/Accident_01.vue` | 사고관리 | 1 |

(MVP는 단일 화면. 추후 통계/대시보드 추가 시 Accident_02~)

### 4.3 권한 `tb_syst_auth_menu`
- 안전관리 도메인이므로 기존 안전 메뉴(risk/chkLst/tbm)와 동일하게 **AUTH_CD 9종**(`00001`,`00004`,`00006`,`00008`,`99999`,`hr`,`master`,`safe`,`system`) 시드 권장. (nearMiss는 master 1행만 시드된 상태라 그 선례는 따르지 않음 — 누락으로 판단)
- BTN_* 기본 매트릭스(권장, planner가 `MenuLockPolicy`(prafta-042 단일 출처)와 정합화):
  - `master`: SRCH/NEW/DELT/SAVE/EXCL 전부 Y
  - `hr`,`safe`: SRCH/NEW/SAVE/EXCL Y, DELT N (등록·처리 가능, 삭제는 master만)
  - 그 외(`00001` 등 일반): SRCH만 Y(열람), 나머지 N — 또는 정책상 미노출
- CMPNY_CD: 메뉴/권한 시드는 회사코드 `'001'` 기준(prafta-042 D5 참조, auth_menu는 회사 비종속 운영 관행 확인 필요).

---

## 5. 화면 흐름 (UI)

### 5.1 사고 등록 팝업 (목업 modal 확장)
- 발생일·발생시각·발생사업장·재해자(정규+일용 검색) — 재해자 선택은 **두 풀(`tb_user`+`tb_daily_user`) 모두** 지원
- **연관 데이터 조회 조건**(선택 입력, 사고일 자동조회 범위를 좁히는 용도):
  - 순회점검: 점검구분(COM001) + **점검대상(CHKPT_CD, 다건, ChkptSearchPop 신규)**
  - 위험성평가: 공정 → 위험요인구분 → 유해요인 (3계층, 0~3 부분입력 허용)
  - 아차사고: 사건유형(SYS061) + 잠재중대성(SYS062) (미입력=전체)
- 사고내용: 재해등급(SYS065 3택) + 사고경위
- [등록] → `tb_accident` INSERT → 수평선 화면 진입

### 5.2 수평선(타임라인) 확정 화면 (`사고관리_temp.png`)
- 수평선 위 원 4개(근태·순회점검·위험성평가·TBM) + 아차사고. 각 원 클릭 → 해당 도메인 조회결과 팝업.
- 각 팝업에서 관리자가 데이터를 **확인·선택(다건)·확정**. 등록 팝업에서 조건을 넣었으면 좁혀서, 안 넣었으면 당일/기간 전체를 펼쳐 선택.
  - 근태: 발생시각 마커를 스케줄/실근태 타임라인에 표시("어느 시점 발생"). 일용직은 스케줄 없음 → 실근태만(없을 수 있음, 안내).
  - 순회점검: 선택 점검대상별 1주일 양호/불량 집계.
  - 위험성평가: 3계층 매칭 평가 리스트(사고 날짜·시각 기준).
  - TBM: 당일 세션 + 재해자 이수여부(`COMPLETION_STATUS_CD`).
  - 아차사고: 3개월 내 사건 리스트 → 전조 선택(`SRC_NEAR_MISS`).
- 4개(+아차사고) 확정 → [확인] → 확정 결과를 `tb_accident_link`에 **스냅샷 저장** → 사고관리 화면 ①탭에 집계.

### 5.3 사고관리 화면 `Accident_01.vue` (목업 3탭)
- 좌측 사고 목록(재해자·등급칩·상태칩) + 우측 상세 3탭:
  - **① 안전관리 현황(사고일 기준)**: `tb_accident_link` 스냅샷 카드 레이아웃(목업 그대로).
  - **② 법정 처리/기한**: 등급별 절차(`tb_accident_legal_step_master`)를 절차 카드로, **조치완료 체크박스 + 항목별 비고 입력**(목업의 "처리 버튼"→체크 방식). 기한 계산·D-day 표시. 면책 문구.
  - **③ 처리 이력**: 목업의 freeform 타임라인 **폐기**. ②탭의 완료체크·비고·완료일시를 시간순으로 읽기 쉽게 롤업한 **파생 뷰**(별도 입력 없음).

---

## 6. 화면 안내문구 (오해 방지)
- 순회점검: "사고일로부터 **1주일 이내** 점검 결과를 집계합니다. (양호/불량 기준)"
- 위험성평가: "사고일로부터 **최근 3개월 이내** 유효 위험성평가입니다. (사고 날짜·시각 기준 조회)"
- 아차사고: "사고일로부터 **최근 3개월 이내** 보고된 아차사고/사건입니다."
- TBM: "**사고 발생 당일** 진행된 TBM만 표시합니다. (당일 기준 고정)"
- ②법정 처리: "법정 기한·조문은 노무사 최종확인 대상이며, 본 화면은 실무 보조용입니다."
- 공통: "모든 항목은 본 시스템 기록 기준이며, '기록 없음'은 행위 부재가 아니라 입력 부재일 수 있습니다."

---

## 7. 보안/권한 (개발 시 필수)
- 식별자(cmpnyCd/userCd/siteCd)는 JWT 클레임에서만 도출. 사업장 스코프·관리노드 권한 가드(IDOR 차단) — 신규 attd/chkpt/risk/tbm 조회가 cross-site 접근 못하게(prafta-034/040 선례 `assertSiteAccess`).
- 사고관리는 master/hr/safe 전사 접근(prafta-042). 등록/처리는 안전관리자 권한, 삭제는 master.
- 재해자 PII(이름/휴대폰) 노출 최소화 — 마스킹, 복호화는 필요 시점만(app-010 패턴).

---

## 8. 결정 완료 / 잔여 확인

**결정 완료(prafta-048 논의)**: 재해자=정규직+일용직 / 연계=스냅샷(사고 날짜·시각 기준) / 위험성평가=3계층 부분입력 / 기간=순회1주·위험3개월·아차3개월·TBM당일 / 코드그룹 SYS065~ 등록 / 웹 전용 / 점검대상·연계 다건 / 약어 `accident`.

**개발 전 잔여 확인**:
1. `tb_user_attd_mgmt`가 일용직 출퇴근을 실제 저장하는지(현재 데이터 0건이라 빈 표시 처리 전제).
2. `tb_accident_legal_step_master` 초기 seed의 법정 절차·기한·조문 — 노무사 검토.
3. 메뉴/권한 시드의 CMPNY_CD 범위(회사 비종속 `'001'` 관행 확정).
4. `SYS067`(도메인 구분)을 코드그룹으로 둘지 상수로 둘지.
