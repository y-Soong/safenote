# prafta-033 TBM 교육관리 고도화 — 웹 작업지시서 (마스터 플랜)

> 본 문서는 prafta-033(TBM 모듈 고도화)의 **웹 파트** 작업지시서 묶음의 인덱스다.
> 원본 요청서: `.claude/requests/prafta-033.md`
> To-Be 설계 사양(외부 작성): `.claude/requests/ref/prafta-033/` (00~06 문서 + SVG 2종)
> 본 묶음은 위 To-Be 사양을 **PRAFTA 실제 코드/스키마/규약에 맞춰 구체화한 개발 명령서**다.

---

## 0. 작성 목적 / 성격

- 이 문서들은 **개발 명령(작업지시서)** 이며, 직접 구현이 아니다. developer 에이전트가 받아서 구현한다.
- To-Be 사양서(ref/prafta-033/*)는 이상화된 명세(`/api/web/...`, Long PK, `.ts`)로 작성되어 있다.
  본 작업지시서는 **PRAFTA 실제 규약이 우선**한다(CLAUDE.md). 충돌 시 본 문서 기준.
- 모든 테이블/컬럼/엔드포인트/DTO 명칭은 **실제 스키마(MCP 확인 완료)와 기존 tbm01 구현**에 맞춰 확정했다.

---

## 1. 사용자 확정 결정 (2026-05-27)

| # | 결정 | 내용 |
|---|---|---|
| D1 | **콘텐츠 모델 = 방향 A (묶음 모델 유지)** | 기존 `TB_TBM_EDU_MTRL`(묶음 마스터) + `TB_TBM_EDU_MTRL_ITEM`(세부항목) 유지. 단일 모델로 재구성하지 않는다. |
| D2 | **기존 테이블 ALTER 확장 허용** | 신규 테이블만 만드는 게 아니라, 기존 콘텐츠 테이블에 컬럼을 추가(`SITE_CD` 등)한다. |
| D3 | **4단계 분할** | A(DDL+콘텐츠) → B(세션관리) → C(실시간) → D(이력). 순서 확정. |
| D4 | **C(실시간 W-07~11) 보류** | 모바일 앱 착수 이후 진행. 본 묶음에서 **명령서를 명확히 분리**(별도 파일 + DEFERRED 표기). A·B·D만 즉시 착수 대상. |

---

## 2. 작업지시서 구성 (파일 맵)

| 단계 | 파일 | 화면(To-Be) | 상태 | 산출 |
|---|---|---|---|---|
| **A** | `prafta-033-A-ddl-content.md` | W-01~03 | 🟢 착수 | DDL(ALTER+신규) + 콘텐츠 라이브러리 보강 |
| **B** | `prafta-033-B-session-mgmt.md` | W-04~06 | 🟢 착수 | TBM 세션 개설/목록/상세/수정/취소 |
| **C** | `prafta-033-C-live-session-DEFERRED.md` | W-07~11 | 🔴 **보류** | 실시간 진행 콘솔/QR출결/종료 — **앱 이후** |
| **D** | `prafta-033-D-history.md` | W-12~15 | 🟢 착수 | 이력/출결상세/미이수처리/사용자별이수 |

> 착수 순서: **A → B → D**. C는 앱(M-01~13) 백엔드/프론트가 나온 뒤 본 묶음의 DEFERRED 문서를 활성화한다.

---

## 3. To-Be 사양 ↔ PRAFTA 실제 매핑 (전 문서 공통 기준)

ref 사양서가 가정한 명칭과 **실제 스키마/규약의 차이**. 모든 단계 문서는 아래 "실제" 컬럼을 사용한다.

### 3.1 식별자 / 멀티테넌시

| ref 사양 | 실제 (MCP 확인) | 비고 |
|---|---|---|
| `USER_ID` (정규직 PK) | **`TB_USER` PK = (`CMPNY_CD` varchar(50), `USER_CD` varchar(20))** | 정규직 식별은 `USER_CD`. `USER_ID`는 표시용 자동생성 컬럼(다름) |
| `DAILY_USER_NO` (일용직 PK) | **`TB_DAILY_USER` PK = (`CMPNY_CD`, `USER_CD`)** | 일용직도 `USER_CD`로 식별. `DAILY_USER_NO` **존재하지 않음** |
| 일용직 만료 `EXPIRE_DT` | **`TB_DAILY_USER.WORK_EXPIRE_DATE` (varchar(8) YYYYMMDD)** | 자정 배치 기준 만료일 |
| 일용직 끝4자리 | **`TB_DAILY_USER.MBL_NO_LAST4` (char(4))** | 평문 4자리(마스킹 표시용). 전체 휴대폰은 `MBL_NO_ENC`(AES-GCM) — 평문 출력 금지 |
| 슬롯 고정여부 | **`TB_DAILY_USER_SLOT.FIXED_YN`** ([SYS017]), PK (`CMPNY_CD`,`SITE_CD`,`SLOT_NO`), `CURR_USER_CD` | "고정 슬롯=만료 없음" 정책의 근거 |
| `eduSessionNo` (Long PK) | **세션 PK = varchar 코드 `SESSION_CD`** (채번) | PRAFTA는 Long auto PK 대신 코드 채번 관례 |
| `CMPNY_CD` 폭 | **표준 varchar(50)** (TB_USER/TB_SITE/TB_DAILY_USER/TB_FILE_INFO/TB_RISK_ASSESSMENT 모두 50) | 기존 `TB_TBM_EDU_MTRL`만 varchar(10) 예외 → **신규 테이블은 varchar(50)** 사용 |

### 3.2 외부 의존 테이블 (실제 구조, 신규 테이블이 참조)

| 항목 | 테이블 | PK / 핵심 컬럼 |
|---|---|---|
| 정규직 | `TB_USER` | (CMPNY_CD, USER_CD), USER_NM(50), SITE_CD, NODE_CD, USE_YN, ACCOUNT_STATUS |
| 일용직 | `TB_DAILY_USER` | (CMPNY_CD, USER_CD), MBL_NO_LAST4, WORK_EXPIRE_DATE, ACCOUNT_STATUS |
| 일용직 슬롯 | `TB_DAILY_USER_SLOT` | (CMPNY_CD, SITE_CD, SLOT_NO), FIXED_YN, CURR_USER_CD, SLOT_STATUS |
| 위험성평가 | `TB_RISK_ASSESSMENT` | (CMPNY_CD, SITE_CD, PROCESS_CD, ASSESSMENT_CD), ASSESSMENT_STATUS[SYS011] — **TITLE 컬럼 없음**(공정/유해요인 기반, 표시명은 조인 산출) |
| 사업장 | `TB_SITE` | (CMPNY_CD, SITE_CD), SITE_NM, GPS_RANGE(varchar4 — 기존 GPS 반경 개념 존재), SITE_ADMIN_CD |
| 파일 | `TB_FILE_INFO` | FILE_MGMT_CD, FILE_NM, FILE_TYPE[SYS010], FILE_PATH, FILE_EXT (기존 tbm01 파일 업로드/Base64 흐름 그대로 활용) |
| 시스템코드 | `tb_syst_val_m` / `tb_syst_val_d` | SYS*** 코드 그룹/상세 |
| 회사별코드 | `tb_baim_val_m` / `tb_baim_val_d` | COM*** (콘텐츠 카테고리 = COM003 자료유형) |
| 채번 | `tb_cmm_seq` + `FNC_CMM_SEQ_NEXTVAL(cmpnyCd, '채널키')` | 코드 PK 채번 |

### 3.3 규약 (전 단계 공통)

- **URL**: `/api/web/tbm/...`(사양) → **`/webApi/tbmXX/...`**(실제). 신규 모듈은 기존 `tbm01` 패턴 따라 번호 모듈로 분리(아래 §5).
- **언어**: TypeScript 미사용 → Vue 컴포저블/스크립트 모두 **JavaScript**.
- **UI**: Element Plus 등 외부 그리드 미사용 → **기존 PRAFTA 디자인 시스템(scoped CSS + CSS 변수 + 공통 컴포넌트)** 사용. (CLAUDE.md "화면 작업 시 절대 규칙")
- **DTO 플로우**: request → param(`from()`) → query/command → result → response. Param/Query/Command/Result = record, Request/Response = Lombok. (기존 tbm01 + 02_BACKEND_SPEC_COMMON 동일)
- **MyBatis**: leading comma, `#{}` 바인딩, `SELECT *` 금지, SQL 첫 줄 `/* Mapper.method */`, UPSERT는 `ON DUPLICATE KEY ... AS NEW`.
- **DB 컬럼 COMMENT**: 코드성 컬럼은 `'설명[SYSxxx] 코드값:의미'` 필수 (feedback 규칙).
- **권한**: `JwtUtil.getAllClaimsAsMap(authorization)` → 토큰 클레임. 권한 게이트는 기존 `AuthRoleUtils`/권한 유틸 활용(developer 로컬 확인). master/safe/hr/999999 + 회사별 커스텀.

---

## 4. 모듈/패키지 배치 (신규)

기존 `com.prafta.web.tbm.tbm01` 패턴을 따라 화면 그룹별 번호 모듈로 분리한다.

| 단계 | 백엔드 패키지 | 프론트 뷰 | 비고 |
|---|---|---|---|
| A | (기존 `tbm01` 확장) | `src/views/tbm/Tbm_01.vue` + `popup/TbmEduMtrlInfo.vue` 보강 | 신규 모듈 없음, 기존 보강 |
| B | `com.prafta.web.tbm.tbm02` | `src/views/tbm/Tbm_02.vue` (+ 팝업) | TBM 세션 관리 |
| C | `com.prafta.web.tbm.tbm03` (보류) | `src/views/tbm/Tbm_03.vue` (보류) | 실시간 진행 — 앱 이후 |
| D | `com.prafta.web.tbm.tbm04` | `src/views/tbm/Tbm_04.vue` (+ 팝업) | 이력/출결 |

> 모듈 번호(tbm02~04)는 제안값. developer는 착수 시 라우팅/viewResolver 충돌만 확인하고 그대로 사용.

---

## 5. C(실시간) 보류 경계 — 명확한 분리

C 단계가 **A·B·D 와 분리되어 보류**되는 이유와 경계:

1. **데이터 생성 주체가 모바일**: 정규직의 입실/서명은 앱(M-01~10)에서 발생한다. 웹 단독으로는 출결 데이터가 생성되지 않는다(일용직 QR 입실조차 W-09=실시간 콘솔에 속함).
2. **SSE 동기화의 구독자가 근로자 앱**: 슬라이드 동기화 broadcast의 수신 대상이 모바일 참여자다.
3. 따라서 C는 **앱 백엔드/프론트(04_BACKEND_SPEC_APP, 06_xx)와 함께** 진행해야 의미 있는 검증이 가능하다.

**경계 합의:**
- A의 DDL은 **출결/이벤트/상태 테이블까지 모두 생성**한다(D의 이력 화면이 읽어야 하므로). 단 **쓰기 경로(입실/종료/동기화)는 C/앱에서 구현**.
- B의 세션은 `OPENED`까지 다룬다. `IN_PROGRESS` 전이(교육 시작)·`sync-state`·`end`·`force-end`는 **C 소관**.
- D(이력)는 출결 테이블을 **읽기 전용**으로 다룬다. 데이터가 없으면 빈 목록/테스트 데이터로 검증.
- `prafta-033-C-live-session-DEFERRED.md`는 활성화 전까지 developer가 착수하지 않는다(파일 상단 DEFERRED 배너).

---

## 6. 단계별 의존 관계

```
A (DDL + 콘텐츠 보강)
   │  └ 신규 테이블 전체 생성 (세션/매핑/출결/이벤트/상태/비번실패)
   ▼
B (세션 관리)  ── 세션 CRUD, OPENED까지. A의 세션/매핑 테이블 사용
   │
   ├──────────────► D (이력)  ── A의 세션/출결 테이블 읽기. B의 세션 데이터 표시
   │
   ▼
C (실시간) [보류] ── 앱과 함께. 출결/이벤트/상태 쓰기 + SSE. B의 OPENED 세션을 IN_PROGRESS로
```

---

## 7. 공통 검증/보안 체크리스트 (전 단계 적용)

- [ ] 모든 쿼리에 `CMPNY_CD = #{gvCmpnyCd}` 스코프 (멀티테넌시 격리)
- [ ] 사업장 권한 사용자는 자기 `SITE_CD` + 회사공통(`SITE_CD IS NULL`)만
- [ ] PII(이름/휴대폰) 평문 노출 주의 — 일용직은 `MBL_NO_LAST4`만 사용, 전체번호 평문 금지
- [ ] 권한 게이트: 콘텐츠 등록(공통=master/safe), 세션 개설(safe+커스텀), 이력(master/safe+커스텀)
- [ ] 상태 전이 서버 재검증 (프론트 1차검증 ≠ 최종 권위)
- [ ] 신규 SYS/COM 코드는 마이그레이션으로 등록(추측 금지, 컬럼 COMMENT에 코드표 명시)

---

## 8. 미해결/후속 확인 항목 (작성 중 발견, developer/사용자 확인 필요)

1. **위험성평가 표시명**: `TB_RISK_ASSESSMENT`에 TITLE이 없음 → 세션-위험성평가 연계 시 화면 표시 문자열(공정명+유해요인 등)을 어떻게 구성할지 위험성평가 모듈 담당과 확정 필요. (B 문서 §위험성평가 연결에서 플래그)
2. ~~신규 SYS 코드 그룹 번호~~ **[해소 2026-05-27]**: 현재 최대 SYS045 확인 → **SYS046~SYS055 배정 확정**(A 문서 §4.2). 착수 시 부재확인 후 시드.
3. **이벤트 로그 PK**: 고볼륨 append 로그라 `BIGINT AUTO_INCREMENT` 제안(코드 채번 대신). PRAFTA 관례와의 예외 — A 문서에서 플래그. (검토 시 유지 결정, 이견 시 전환)
4. **리치 텍스트 에디터**: 교육 내용 HTML 입력기(Tiptap 등) 기존 사용 여부 — B 착수 시 프론트 확인. 없으면 도입 협의.
5. **Excel/PDF 출력**: 백엔드(POI/iText) vs 프론트(SheetJS) — D 착수 시 기존 라이브러리 확인.

---

**최종 업데이트**: 2026-05-27 — Phase A/B/D 작업지시서 작성 시작, C는 보류 분리.
