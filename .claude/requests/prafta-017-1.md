\## 4. 화면 3: 연차 부여 정책 (신규) ⭐ 핵심



\### 4.1 기능 설명



\*\*이게 뭐다\*\*: 회사 전체에 적용되는 연차 부여 정책을 설정하는 화면. \*\*7개 의사결정 axis + 휴가 사용 단위 정책\*\*을 한 화면에서 통합 관리. 프리셋 화면과 직접 설정 화면을 통합한 단일 정책 설정 페이지.



\*\*v2 핵심 변경\*\*: 화면 3(프리셋)과 화면 4(직접 설정)를 통합. 프리셋 카드 영역 제거 (axis 직접 설정으로 모든 케이스 표현 가능). 1번+2번 axis 조합 결과를 \*\*부여 시점 미리보기 팝업\*\*으로 시각화.



\### 4.2 화면 구성



```

1\. 페이지 헤더

&#x20;  - 페이지 타이틀

&#x20;  - 우측 액션: \[변경 이력] \[조회] 버튼



2\. 부여 시점 미리보기 안내 카드

&#x20;  - 안내 문구

&#x20;  - \[부여 시점 미리보기] 버튼 → 팝업 오픈



3\. 7개 axis 카드 (순서대로)

&#x20;  - 1번: 연차 부여 기준 (HIRE\_DATE / FISCAL\_YEAR)

&#x20;  - 2번: 입사 첫해 처리 방식 (MONTHLY\_ONLY / PRORATE / NEXT\_YEAR\_BULK)

&#x20;  - 3번: 비례 부여 시 반올림 (조건부 활성)

&#x20;  - 4번: 회계연도 시작일 (조건부 활성)

&#x20;  - 5번: 근속 가산 정책 (법정/직접 입력 + max)

&#x20;  - 6번: 연차 유효기간

&#x20;  - 7번: 연차 사용촉진 제도



4\. 휴가 사용 단위 정책 (별도 섹션)

&#x20;  - 회사가 허용하는 사용 단위 (좌측)

&#x20;  - 같은 날 다중 신청 (우측)



5\. 고급 기능

&#x20;  - 정책 변경 영향 분석 → 화면 8로 이동



6\. 저장/취소 버튼

```



\### 4.2.1 axis 순서 (v2 재배치)



| # | axis | 컬럼 | 옵션 / 입력 |

|---|------|------|-----------|

| 1 | 연차 부여 기준 | AXIS1\_GRANT\_BASE | HIRE\_DATE / FISCAL\_YEAR |

| 2 | 입사 첫해 처리 방식 | AXIS2\_FIRST\_YEAR\_METHOD | MONTHLY\_ONLY / PRORATE / NEXT\_YEAR\_BULK |

| 3 | 비례 부여 시 반올림 | AXIS3\_PRORATE\_ROUNDING | CEIL / ROUND / FLOOR / HALF\_DAY (AXIS2=PRORATE 일 때만 활성) |

| 4 | 회계연도 시작일 | AXIS4\_FISCAL\_START\_MM/DD | 1\~12월, 1\~31일 (AXIS1=FISCAL\_YEAR 일 때만 활성) |

| 5 | 근속 가산 정책 | AXIS5\_TENURE\_MODE + START\_YEAR + INTERVAL + MAX\_DAYS | LEGAL / CUSTOM + n, m, max |

| 6 | 연차 유효기간 | AXIS6\_VALIDITY\_MONTHS | 12(법정) / 24(연장) |

| 7 | 사용촉진 제도 | AXIS7\_USE\_PROMOTION | Y / N |



\*\*v2 axis 번호 변경 이유\*\*:

\- 1번(부여 기준) + 2번(첫해 처리)이 정책 동작의 핵심 결정 요소이므로 인접 배치

\- 3번(비례 반올림)을 2번 바로 뒤로 배치 (비례 부여 선택 시 즉시 보임)

\- 4번(회계연도 시작일)은 1번에 종속된 부가 설정이므로 후순위로 이동

\- 5\~7번은 기본 운영 정책 (그대로 유지)



\*\*참고\*\*: 1년 미만 월차(법정 의무, 매월 만근 시 1일씩 최대 11일)는 정책과 무관하게 항상 자동 부여되므로 별도 axis로 두지 않음. 2번 axis 카드 내 안내문으로 표시.



\### 4.2.2 v1 → v2 변경 사항 요약



| 변경 항목 | 변경 내용 |

|----------|----------|

| 화면 통합 | 화면 3(프리셋) + 화면 4(직접 설정) → 단일 화면으로 통합 |

| 프리셋 카드 제거 | 7개 axis 직접 설정으로 모든 정책 표현 가능 |

| axis 순서 재배치 | 1→2→3→4 순으로 의사결정 흐름에 맞게 재배치 |

| 부여 시점 미리보기 | 1번+2번 조합 결과를 시간순 표로 시각화하는 팝업 추가 |

| 헤더 액션 통합 | \[변경 이력] \[조회] 버튼을 통합 화면 우측에 배치 |

| 정책 변경 영향 분석 | 고급 기능 영역에 진입점 배치 (기존과 동일) |

| 3번 axis 옵션 | NONE 제거, PREGRANT 제거 (1년차 37일 비현실적), MONTHLY\_ACCRUAL → MONTHLY\_ONLY 라벨 명확화. 최종 3개 옵션 |

| 6번 axis 옵션 | "회계연도말까지" 제거 (법정 12개월 위반 가능성). 12/24개월 2개 |

| 휴가 사용 단위 | "휴게시간 처리" 옵션 제거 (시스템 강제). 사용 단위 + 다중 신청만 좌우 배치 |



\### 4.3 ⭐ 1번 + 2번 axis 활성/비활성 매트릭스 (필수 구현)



| 2번 옵션 | AXIS1=HIRE\_DATE | AXIS1=FISCAL\_YEAR |

|---------|:---:|:---:|

| MONTHLY\_ONLY (월차만 부여) | ✅ | ✅ |

| PRORATE (회계연도 시점 비례 본연차) | ❌ | ✅ |

| NEXT\_YEAR\_BULK (차년도 시점 15일 일괄) | ❌ | ✅ |



\*\*구현 방법\*\*:

\- 프론트엔드: AXIS1 변경 시 AXIS2 옵션의 disabled 토글

\- 백엔드: 저장 시 매트릭스 위반 검증 후 거부

\- \*\*AXIS1=HIRE\_DATE 일 때 AXIS2는 사실상 MONTHLY\_ONLY 한 가지만 선택 가능\*\*



\### 4.4 ⭐ axis별 조건부 활성 처리



| axis | 활성 조건 | 비활성 시 동작 |

|------|----------|---------------|

| 3번 (비례 반올림) | AXIS2 = PRORATE | "조건부 활성" 배지 노출, 모든 옵션 disabled, 백엔드 저장 시 기본값 'CEIL' |

| 4번 (회계연도 시작일) | AXIS1 = FISCAL\_YEAR | "조건부 활성" 배지 노출, 입력 필드 disabled, 백엔드 저장 시 NULL |



\### 4.5 ⭐ 2번 axis와 법정 월차의 관계



\- \*\*2번 axis (본연차 정책)\*\*: 입사 첫해의 본연차 추가 부여 방식 (회사 정책)

\- \*\*법정 월차 (axis 아님)\*\*: 1년 미만 매월 만근 시 1일씩 부여 (근로기준법 제60조 제2항, 정책 무관 강제)



두 가지는 \*\*별개로 동작\*\*한다. 예를 들어 AXIS2=NEXT\_YEAR\_BULK인 경우:

\- 법정 월차: 입사일 기준 매월 발생 (최대 11일)

\- 2번 본연차: 차년도 회계연도 시작일에 15일 일괄



두 개가 모두 부여된다. UI에서 2번 axis 카드 내 안내문으로 명확히 표시.



\*\*참고: MONTHLY\_ONLY는 "본연차 추가 부여 없이 법정 월차로만 운영한다"는 선언\*\*. 법정 월차 자체는 어떤 정책에서도 동일하게 발생.



\### 4.6 ⭐ 3번 axis와 사용 단위 정책의 관계



\*\*AXIS3\_PRORATE\_ROUNDING='HALF\_DAY' 선택 시\*\*:

\- TB\_LEAVE\_USAGE\_POLICY.ALLOW\_HALF\_DAY를 'Y'로 자동 설정

\- 프론트엔드: 0.5일 체크박스가 자동 체크되고 disabled 처리

\- 백엔드: 저장 시 강제 'Y' 처리 (사용자가 'N'으로 보내도 무시)



\### 4.7 ⭐ 5번 axis 근속 가산 정책 상세



\#### 4.7.1 컬럼 구조



| 컬럼 | 타입 | 설명 |

|------|------|------|

| AXIS5\_TENURE\_MODE | VARCHAR(10) | `LEGAL` / `CUSTOM` |

| AXIS5\_START\_YEAR | INT | 가산 시작 연차 (n) — LEGAL 시 자동 3 |

| AXIS5\_INTERVAL | INT | 가산 주기 (m) — LEGAL 시 자동 2 |

| AXIS5\_MAX\_DAYS | INT | 최대 연차일수 (기본 25) |



\#### 4.7.2 입력 규칙



```

LEGAL 모드:

\- n = 3 (고정), m = 2 (고정)

\- max\_days만 입력 가능 (기본 25, min 25)



CUSTOM 모드:

\- n: 1\~3 (법정 위반 방지, 3 초과 시 가산 발생이 늦어져 근로자 손해)

\- m: 1\~2 (법정 위반 방지, 2 초과 시 가산 주기가 길어져 근로자 손해)

\- max\_days: 25\~40 (25 미만 시 법정 위반)

```



\#### 4.7.3 부여 계산 로직



```java

public int calculateAnnualDays(int yearOfService, int startYear, int interval, int maxDays) {

&#x20;   int baseDays = 15;

&#x20;   if (yearOfService < startYear) {

&#x20;       return baseDays;

&#x20;   }

&#x20;   int bonus = (yearOfService - startYear) / interval + 1;

&#x20;   return Math.min(baseDays + bonus, maxDays);

}

```



\#### 4.7.4 미리보기 (프론트엔드 표시)



화면에서 입력값에 따라 실시간으로 부여 시뮬레이션을 표시:



```

LEGAL 기본값 (n=3, m=2, max=25):

&#x20; 1\~2년차: 15일

&#x20; 3년차:   16일

&#x20; 5년차:   17일

&#x20; 7년차:   18일

&#x20; ...

&#x20; 21년차:  25일 (최대 도달)



CUSTOM 예시 (n=1, m=1, max=25):

&#x20; 1년차:   16일

&#x20; 2년차:   17일

&#x20; 3년차:   18일

&#x20; ...

&#x20; 11년차:  25일 (최대 도달)

```



\### 4.8 ⭐ 부여 시점 미리보기 팝업 (v2 신규)



\#### 4.8.1 진입점



화면 상단의 안내 카드 우측 \[부여 시점 미리보기] 버튼 클릭 시 팝업 오픈.



\#### 4.8.2 표시 내용



2025-07-15 입사자를 기준으로 4가지 정책의 시간순 부여를 표로 비교:



| 정책 | 25.07.15 (입사) | 25.08\~12 | 26.01.01 | 26.01\~06 | 26.07.15 | 27.01.01 | 27.07.15 | 28.01.01 |

|------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|

| 입사일 기준 - 월차만 부여 | 입사 | 월차 5 | - | 월차 6 | \*\*본연차 15\*\* | - | 본연차 15 | - |

| 회계연도 기준 - 월차만 부여 | 입사 | 월차 5 | - | 월차 6 | - | \*\*본연차 15\*\* | - | 본연차 15 |

| 회계연도 기준 - 비례 부여 | 입사 | 월차 5 | \*\*비례 약 7\*\* | 월차 6 | - | 본연차 15 | - | 본연차 15 |

| 회계연도 기준 - 차년도 일괄 부여 | 입사 | 월차 5 | \*\*본연차 15\*\* | 월차 6 | - | 본연차 15 | - | 본연차 15 |



\#### 4.8.3 추가 정보



팝업 하단에 다음 정보 표시:

\- \*\*회사 부담 비교 (1년차)\*\*: 정책별 누적 부여량 순위

\- \*\*첫 본연차 부여 시점\*\*: 정책별 차이



\#### 4.8.4 동작 사양



\- 모달 팝업 형태 (배경 클릭 또는 X 버튼으로 닫힘)

\- 표는 스크롤 가능 (가로 스크롤, min-width 900px)

\- 본연차/비례부여 발생 셀은 노란색 강조 (가독성 향상)



\### 4.9 휴가 사용 단위 정책



7개 axis와 별도 섹션으로 노출. 좌우 2단 배치:



| 영역 | 위치 | 입력 |

|------|------|------|

| 회사가 허용하는 사용 단위 | 좌측 | 1일(필수) / 0.5일 / 0.25일 / 0.125일 다중 선택 |

| 같은 날 다중 신청 | 우측 | 허용(N건) / 불허 |



\*\*제거된 항목\*\*:

\- \~\~휴게시간 처리 옵션\~\~ → 시스템에서 일률적으로 "휴게시간 자동 제외 + 신청 불가" 강제

\- \~\~시간 단위 시작 시각 제약\~\~ → 기본값 "근무 시간 내에서만" 고정

\- \~\~1일 환산 시간\~\~ → 근무 스케줄 시간에 비례하여 자동 처리



\*\*휴게시간 처리 로직 (시스템 강제)\*\*:

\- 시간 단위 휴가 신청 시 근로자별 근무 스케줄(TB\_USER\_WORK\_PLAN)의 휴게시간을 자동 인식

\- 휴게시간을 가로지르는 시간대는 신청 자체가 불가능 (UI에서 시간대 선택 비활성)

\- 이유: 사용 단위 정합성 보장 (0.25일 신청했는데 휴게시간 제외로 0.125일만 차감되는 모순 방지)



\### 4.10 시간 단위 차감 로직 (참고)



```

사용 단위 → 차감 시간 계산

\- FULL\_DAY     → 사용자 스케줄 1일 전체

\- HALF\_DAY     → 사용자 스케줄 1일의 50%

\- QUARTER\_DAY  → 사용자 스케줄 1일의 25%

\- HOURLY       → 사용자 스케줄 1일의 12.5% (=0.125일)



예: 8시간 근무자

\- HOURLY 1회 = 1시간 차감 (=0.125일)

예: 6시간 근무자 (단축근무)

\- HOURLY 1회 = 0.75시간 차감 (=0.125일)

```



\### 4.11 API 명세



```

GET  /api/leave-policy                    - 현재 정책 조회 (USE\_YN='Y')

POST /api/leave-policy/save-policy        - 정책 저장 (Upsert)

GET  /api/leave-policy/history            - 정책 변경 이력 조회

GET  /api/leave-policy/preview-grant      - 부여 시점 미리보기 (선택적, 클라이언트 계산 가능)

&#x20;    Query: { axis1, axis2, axis4\_mm, axis4\_dd, hireDate (default: 2025-07-15) }

&#x20;    Response: { policies: \[{ name, events: \[{ date, type, amount }] }] }

```



\### 4.12 검증 규칙 (전체)



```

1\. AXIS1 + AXIS2 매트릭스 검증

&#x20;  - AXIS2 = PRORATE 일 때 → AXIS1 = FISCAL\_YEAR 필수

&#x20;  - AXIS2 = NEXT\_YEAR\_BULK 일 때 → AXIS1 = FISCAL\_YEAR 필수

2\. AXIS3 = HALF\_DAY → TB\_LEAVE\_USAGE\_POLICY.ALLOW\_HALF\_DAY='Y' 강제

3\. AXIS2 != PRORATE → AXIS3은 기본값('CEIL') 저장 (사용자 입력 무시)

4\. AXIS1 != FISCAL\_YEAR → AXIS4\_FISCAL\_START\_MM/DD는 NULL 저장

5\. AXIS4\_FISCAL\_START\_MM: 1\~12, AXIS4\_FISCAL\_START\_DD: 1\~31 (월별 최대일 검증 필요)

6\. AXIS5\_MODE = CUSTOM 일 때:

&#x20;  - 1 <= AXIS5\_START\_YEAR <= 3

&#x20;  - 1 <= AXIS5\_INTERVAL <= 2

7\. AXIS5\_MAX\_DAYS >= 25 (법정 위반 시 거부)

8\. AXIS6\_VALIDITY\_MONTHS >= 12 (법정 최소)

9\. 저장 시 TB\_LEAVE\_POLICY\_HISTORY에 변경 스냅샷 기록

10\. 활성 정책 1개만 유지 (USE\_YN='Y' 유니크)

```



\### 4.13 테스트 케이스



```

TC-01: AXIS1=HIRE\_DATE 선택 → AXIS2의 PRORATE, NEXT\_YEAR\_BULK 비활성 (MONTHLY\_ONLY만 가능)

TC-02: AXIS1=FISCAL\_YEAR 선택 → AXIS2의 3개 옵션 모두 활성

TC-03: AXIS1=HIRE\_DATE + AXIS2=PRORATE로 POST 호출 → 400 거부

TC-04: AXIS2 != PRORATE 일 때 3번 axis 비활성 + "조건부 활성" 배지 노출

TC-05: AXIS1 != FISCAL\_YEAR 일 때 4번 axis(회계연도 시작일) 비활성

TC-06: AXIS3=HALF\_DAY 저장 → TB\_LEAVE\_USAGE\_POLICY.ALLOW\_HALF\_DAY='Y' 강제 변경

TC-07: 5번 axis LEGAL 모드 → n=3, m=2 고정, max만 입력 가능

TC-08: 5번 axis CUSTOM 모드 + n=5로 POST 호출 → 400 거부 (법정 위반)

TC-09: 5번 axis CUSTOM 모드 + m=3로 POST 호출 → 400 거부 (법정 위반)

TC-10: AXIS5\_MAX\_DAYS=20으로 POST 호출 → 400 거부 (법정 25일 미만)

TC-11: 5번 axis 미리보기: n=1, m=1, max=25 입력 시 1년차 16일, 11년차 25일(최대) 표시

TC-12: \[부여 시점 미리보기] 클릭 → 팝업 오픈 + 4가지 정책 시간순 표 표시

TC-13: 1번=회계연도, 2번=NEXT\_YEAR\_BULK, 2024.07.15 입사자 시뮬레이션:

&#x20;      2026.02 잔여 = 월차 5일(2024.08\~12) + 본연차 15일 = 20일

TC-14: 시간 단위 휴가 신청 시 휴게시간(예: 12:00\~13:00) 가로지르는 시간대 선택 불가

TC-15: 저장 후 TB\_LEAVE\_POLICY\_HISTORY에 NEW\_SNAPSHOT 기록 확인

TC-16: 같은 회사에 USE\_YN='Y' 정책이 이미 존재 시 → 새 저장은 기존 정책의 USE\_YN='N' 처리 + 신규 INSERT

```



\### 4.14 TB\_LEAVE\_POLICY DDL (정식)



```sql

CREATE TABLE TB\_LEAVE\_POLICY (

&#x20; POLICY\_SEQ                BIGINT NOT NULL AUTO\_INCREMENT,

&#x20; CMPNY\_CD                  VARCHAR(20) NOT NULL,



&#x20; -- 7개 axis (v2 순서 재배치)

&#x20; AXIS1\_GRANT\_BASE          VARCHAR(20) NOT NULL

&#x20;   COMMENT '1번: HIRE\_DATE/FISCAL\_YEAR',

&#x20; AXIS2\_FIRST\_YEAR\_METHOD   VARCHAR(30) NOT NULL

&#x20;   COMMENT '2번: MONTHLY\_ONLY/PRORATE/NEXT\_YEAR\_BULK',

&#x20; AXIS3\_PRORATE\_ROUNDING    VARCHAR(20) NOT NULL DEFAULT 'CEIL'

&#x20;   COMMENT '3번: CEIL/ROUND/FLOOR/HALF\_DAY (AXIS2=PRORATE 시만 유효)',

&#x20; AXIS4\_FISCAL\_START\_MM     CHAR(2) NULL

&#x20;   COMMENT '4번: 회계연도 시작월 (AXIS1=FISCAL\_YEAR 시만)',

&#x20; AXIS4\_FISCAL\_START\_DD     CHAR(2) NULL

&#x20;   COMMENT '4번: 회계연도 시작일 (AXIS1=FISCAL\_YEAR 시만)',

&#x20; AXIS5\_TENURE\_MODE         VARCHAR(10) NOT NULL DEFAULT 'LEGAL'

&#x20;   COMMENT '5번 모드: LEGAL/CUSTOM',

&#x20; AXIS5\_START\_YEAR          INT NOT NULL DEFAULT 3

&#x20;   COMMENT '5번 가산 시작 연차 (1\~3)',

&#x20; AXIS5\_INTERVAL            INT NOT NULL DEFAULT 2

&#x20;   COMMENT '5번 가산 주기 (1\~2)',

&#x20; AXIS5\_MAX\_DAYS            INT NOT NULL DEFAULT 25

&#x20;   COMMENT '5번 최대 연차일수 (25 이상)',

&#x20; AXIS6\_VALIDITY\_MONTHS     INT NOT NULL DEFAULT 12

&#x20;   COMMENT '6번: 유효기간(개월) 12 or 24',

&#x20; AXIS7\_USE\_PROMOTION       CHAR(1) NOT NULL DEFAULT 'N'

&#x20;   COMMENT '7번: 사용촉진 사용여부 Y/N',



&#x20; USE\_YN                    CHAR(1) NOT NULL DEFAULT 'Y',

&#x20; APPLY\_FROM\_DATE           VARCHAR(8) NOT NULL COMMENT '정책 적용 시작일',

&#x20; REG\_USER\_CD               VARCHAR(20) NOT NULL,

&#x20; REG\_DTIME                 DATETIME NOT NULL DEFAULT CURRENT\_TIMESTAMP,

&#x20; MOD\_USER\_CD               VARCHAR(20) NULL,

&#x20; MOD\_DTIME                 DATETIME NULL ON UPDATE CURRENT\_TIMESTAMP,



&#x20; PRIMARY KEY (POLICY\_SEQ),

&#x20; UNIQUE KEY UK\_LEAVE\_POLICY\_ACTIVE (CMPNY\_CD, USE\_YN, APPLY\_FROM\_DATE)

) COMMENT='회사별 연차 정책';

```



\### 4.15 TB\_LEAVE\_USAGE\_POLICY DDL (정식)



```sql

CREATE TABLE TB\_LEAVE\_USAGE\_POLICY (

&#x20; POLICY\_SEQ            BIGINT NOT NULL

&#x20;   COMMENT 'TB\_LEAVE\_POLICY.POLICY\_SEQ 1:1',

&#x20; CMPNY\_CD              VARCHAR(20) NOT NULL,

&#x20; ALLOW\_FULL\_DAY        CHAR(1) NOT NULL DEFAULT 'Y'

&#x20;   COMMENT '1일 단위 허용 (항상 Y, 변경불가)',

&#x20; ALLOW\_HALF\_DAY        CHAR(1) NOT NULL DEFAULT 'Y'

&#x20;   COMMENT '0.5일 단위',

&#x20; ALLOW\_QUARTER\_DAY     CHAR(1) NOT NULL DEFAULT 'Y'

&#x20;   COMMENT '0.25일 단위',

&#x20; ALLOW\_HOURLY          CHAR(1) NOT NULL DEFAULT 'N'

&#x20;   COMMENT '0.125일(1시간) 단위',

&#x20; MAX\_DAILY\_REQUEST     INT NOT NULL DEFAULT 3

&#x20;   COMMENT '같은 날 최대 신청 건수 (0=불허)',

&#x20; MOD\_USER\_CD           VARCHAR(20) NULL,

&#x20; MOD\_DTIME             DATETIME NULL ON UPDATE CURRENT\_TIMESTAMP,

&#x20; PRIMARY KEY (POLICY\_SEQ),

&#x20; CONSTRAINT FK\_LUP\_POLICY FOREIGN KEY (POLICY\_SEQ) REFERENCES TB\_LEAVE\_POLICY(POLICY\_SEQ)

) COMMENT='휴가 사용 단위 정책';



\-- 휴게시간 처리 컬럼 제거 (시스템 강제로 일률 처리)

\-- 기존: BREAK\_TIME\_HANDLING — v2에서 제거

```



\---

