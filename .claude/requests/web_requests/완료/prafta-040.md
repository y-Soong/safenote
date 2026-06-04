# PRAFTA-040 — 아차사고/사건 보고 도메인 (웹 파트) 작업 분해 및 plan

> 단일 출처(plan). 본 도메인 설계 출처: `.claude/context/near-miss-incident-design.md` (이하 "설계문서").
> 영역: web (백엔드 `prafta-backend` + 웹프론트 `prafta-web-frontend`). **앱 파트는 본 요청서 범위 외** → `app_requests/prafta-app-012.md` 로 분리.
> 작성: planner. 상태: 분해완료(미구현). 후속: developer(BE+FE script) → qa → security.

---

## 0. 한눈에 보기 — 확정안

| 항목 | 확정 값 | 근거 |
|---|---|---|
| 신규 대메뉴 슬러그 | `nearMiss` | 설계문서 채널매트릭스 / 메뉴 컨벤션 |
| 대메뉴 한글명 | **사건관리** | (아차사고 외 경미사고/유해위험요인 발견 포괄 → "사건관리"가 적합. 결정필요 D3 참조) |
| 대메뉴 MENU_IDX | **7** | 현행 user1/baim2/chkLst3/risk4/tbm5/attd6 다음 |
| 소메뉴 | `NearMiss_01` (사건 관리), MENU_VIEW=`nearMiss/NearMiss_01.vue` | viewResolver 자동 라우팅 |
| 신규 테이블 | `tb_near_miss` | 설계문서 §2 DDL 그대로 |
| 신규 코드그룹 | SYS061(사건유형) / SYS062(잠재중대성) / SYS063(처리상태) | 설계문서 §2 |
| 웹 BE 패키지 | `com.prafta.web.nearmiss.nearmiss01` | risk03 레이어링 동일 |
| 웹 BE 컨트롤러 base | `@RequestMapping("/nearmiss01")` (axios `/webApi/nearmiss01/...`) | risk03 패턴 동일 |
| 권한 | `Risk_03` 행 복제(안전직군 Y, hr=N) | "확정 사실" |
| 보존 | 물리삭제 금지(USE_YN) | 설계문서 §7-5 |

---

## 1. 작업 단위 분해표 (의존순서)

| # | 작업ID | 유형 | 모듈 | 작업유형 | 요약 | 선행 |
|---|---|---|---|---|---|---|
| 1 | PRAFTA-040-1 | backend | nearmiss/migration | 신규 | 마이그레이션 SQL(테이블+SYS061~063+메뉴 menu_m/menu_d+권한) 작성 | 없음 |
| 2 | PRAFTA-040-2 | backend | nearmiss/nearmiss01 | 신규 | 사건 CRUD/목록/상세/상태전환/재분류 API (web 패키지) | 040-1 |
| 3 | PRAFTA-040-3 | frontend-screen | nearMiss | 신규 | NearMiss_01.vue 사건 관리 목록 화면 | 040-2 |
| 4 | PRAFTA-040-4 | frontend-screen | nearMiss/popup | 신규 | popup/NearMissInfo.vue 정밀조사 상세 팝업(설계 5-C) | 040-2 |
| 5 | PRAFTA-040-5 | frontend-screen | risk | 보완 | RiskAssessInfo.vue "아차사고로 전환" 액션(설계 4-B) | 040-2 |

> 5개 한도 충족. mixed 없음(BE/FE 분할 완료). 040-1은 운영 미적용(파일만), DB 적용은 사용자 수동(read-only MCP).

---

## 2. 정책서/설계 출처 매핑

| 요구사항 | 출처 |
|---|---|
| 아차사고는 위험성평가와 분리된 별도 '사건' 도메인 | 설계문서 §1 (산안법 §36 수시 위험성평가 / §57 산업재해 / 중처법 §4·시행령 §4) |
| `tb_near_miss` 컬럼/PK/채번 | 설계문서 §2 DDL |
| SYS061~063 코드 | 설계문서 §2 |
| 상태 전이 100→200→300→400, 900 반려 | 설계문서 §4 상태전이 다이어그램 |
| 위험성평가요청 → 아차사고 재분류(SRC_*) | 설계문서 §4-B |
| 권한(안전직군 접근, 인사 차단) | 공통 정책서 §8(권한) + "확정 사실"(Risk_03 복제) |
| 보존(물리삭제 금지, USE_YN) | 설계문서 §7-5, 공통 정책서 §11(감사로그/보존) |
| 사업장 스코프·IDOR 가드 | 설계문서 §6, 공통 정책서 §6/§8 |

> 별도 "아차사고 정책서" 섹션은 정책서 디렉토리에 없음 → 설계문서가 법적 근거를 정리한 단일 출처로 인용(README 우선순위 4번 기술정책서보다 도메인 설계문서가 본 작업의 단일 출처).

---

## 3. PRAFTA-040-1 — 마이그레이션 SQL (DDL+코드+메뉴+권한)

산출물: `prafta-backend/src/main/resources/sql/migration/prafta-040-near-miss.sql` (본 요청서와 함께 작성됨).

포함 내용:
1. `tb_near_miss` CREATE TABLE — 설계문서 §2 DDL 100% 복사(컬럼/PK/인덱스/COMMENT 일치).
2. 코드그룹 시드:
   - `tb_syst_val_m`: SYS061/SYS062/SYS063 (마스터 3건)
   - `tb_syst_val_d`: 각 그룹 코드값 (SYS061 3 / SYS062 3 / SYS063 5 = 11건)
   - ⚠️ **`tb_syst_val_d`/`tb_syst_val_m`에는 CMPNY_CD 컬럼이 없다**(schema-full.sql L794~824 확인). "확정 사실"의 `tb_syst_val_d (CMPNY_CD, ...)` 기술은 스키마와 불일치 → 실제 스키마(CMPNY_CD 없음)를 따름. **결정필요 D5로 보고**.
3. 대메뉴 `tb_syst_menu_m`: `nearMiss / MENU_SRC='001'(웹) / MENU_NM='사건관리' / MENU_IDX=7 / USE_YN='Y'`.
4. 소메뉴 `tb_syst_menu_d`: `NearMiss_01 / MENU_M_ID='nearMiss' / MENU_VIEW='nearMiss/NearMiss_01.vue' / MENU_NM='사건 관리' / MENU_IDX=1 / USE_YN='Y'`.
5. 권한 `tb_syst_auth_menu`: CMPNY_CD='001', `Risk_03` 동일 정책 — `master/safe/system/00001/00004/00006/00008/99999`=Y 전권(조회/저장만 사용, 신규는 인라인), `hr`='N'(차단).
   - ⚠️ CMPNY_CD는 실제 Risk_03 행의 회사코드와 동일해야 함. prafta-019-F 등 기존 메뉴 마이그레이션이 `'001'`을 사용하므로 `'001'` 채택. 멀티테넌트라면 회사별 추가 필요 → **결정필요 D5**.

코드 COMMENT 규칙(MEMORY feedback_db_comment): 코드성 컬럼은 `설명[SYS코드] 코드값:의미`. DDL은 설계문서가 이미 이 형식을 따름(예: `INCIDENT_TYPE_CD ... [SYS061] 100:아차사고 ...`).

---

## 4. PRAFTA-040-2 — 웹 백엔드 API (`com.prafta.web.nearmiss.nearmiss01`)

레이어링은 `web.risk.risk03` 동일: controller / service(+impl) / mapper(+XML) / dto(request·response) / result / application(param·query·command).

컨트롤러 base: `@RequestMapping("/nearmiss01")`. axios 프리픽스는 risk03과 동일하게 `/webApi/nearmiss01/...`.
식별자(cmpnyCd/siteCd/userCd)는 **JWT 클레임에서만** 도출(`jwtUtil.getAllClaimsAsMap(authorization)` → Param.from). 요청 바디의 cmpnyCd/siteCd는 신뢰하지 않음(IDOR 차단). 사업장 스코프 가드 필수.

### 4.1 엔드포인트 명세

| # | Method | Path | 용도 | 요청(주요) | 응답(주요) |
|---|---|---|---|---|---|
| E1 | GET | `/nearmiss01/incident-lists` | 사건 목록(상태탭/필터/사업장스코프) | siteCd, reportStatusCd, incidentTypeCd, potentialSeverityCd, startDate, endDate | `incidentResultList[]` |
| E2 | GET | `/nearmiss01/incident-info` | 사건 단건 상세 | siteCd, nearMissId | `incidentInfo` (헤더 전 컬럼 + 코드명 + 파일경로 + 출처) |
| E3 | GET | `/nearmiss01/status-counts` | 상태별 카운트(탭 배지) | siteCd, (필터) | `{접수,검토중,조치중,완료}` |
| E4 | POST | `/nearmiss01/save-incident` | 정밀조사 저장(원인/재발방지/임시조치 등) | siteCd, nearMissId, causeDesc, preventionDesc, immediateActionDesc | 200 |
| E5 | POST | `/nearmiss01/change-status` | 상태 전환(100→200→300→400 / 900 반려) | siteCd, nearMissId, reportStatusCd, (rejectReason) | 200 |
| E6 | POST | `/nearmiss01/reclassify-from-assessment` | 위험성평가요청→아차사고 재분류(설계 4-B) | srcProcessCd, srcAssessmentCd, incidentType/occurDtime/description 등 | `{nearMissId}` |

> 채번: E6 및 (앱 보고)에서 `NEAR_MISS_ID = 'NM' + YYYYMMDD + SEQ(사업장별 일련)`. SEQ 채번 쿼리는 mapper에서 `MAX(SUBSTR)` 또는 별도 시퀀스 — developer 확정.
> E6은 web에서만 호출(RiskAssessInfo 전환). 원 `tb_risk_assessment` 이관 처리(상태값 vs USE_YN='N')는 **결정필요 D2** — 미확정이라 골격 TODO로 두고 developer가 확정 후 구현.
> E1 정밀조사/완결/재분류는 web 전용. 앱은 `/appApi/nearmiss/*` 별도(app-012, web 호출 금지).

### 4.2 DTO/Result 필드 (tb_near_miss 컬럼 기준, camelCase 매핑)

result(`IncidentResult`): cmpnyCd, siteCd, nearMissId, incidentTypeCd, incidentTypeNm, processCd, processNm, occurDtime, locationDesc, description, potentialSeverityCd, potentialSeverityNm, immediateActionDesc, causeDesc, preventionDesc, fileMgmtCd, filePath, reportStatusCd, reportStatusNm, reporterId, reporterNm, reportDtime, reviewerId, reviewerNm, reviewDtime, srcProcessCd, srcAssessmentCd, useYn.

> 코드명(Nm)은 join 또는 화면측 코드맵으로 해석. risk03은 화면에서 코드 조회 후 표시 → 동일 방식 권장(목록 응답엔 코드값, 화면이 코드명 매핑). developer 확정.

### 4.3 mapper XML

위치: `prafta-backend/src/main/resources/com/prafta/web/nearmiss/nearmiss01/mapper/NearMiss01Mapper.xml`.
SQL 규칙: leading 콤마, `#{}` 바인딩, `SELECT *` 금지, 스키마 100% 일치, 사업장 스코프 WHERE 필수.

---

## 5. 웹 화면 (PRAFTA-040-3/4/5) — UI 명세는 `prafta-040-ui-spec.md` 참조

- 040-3 `NearMiss_01.vue`: 사업장 스코프 + 상태탭(접수/검토중/조치중/완료) + 유형·잠재중대성·기간 필터 + 목록 그리드(사건ID/유형/잠재중대성 배지/공정/보고자/발생일시/상태) + 행 더블클릭→상세 팝업. 권한 BTN_* 게이팅. Risk_03 레이아웃/디자인 토큰 그대로.
- 040-4 `popup/NearMissInfo.vue`: 2단 모달(좌=보고 읽기, 우=조사·조치). RiskAssessInfo.vue 드래그 모달·읽기/입력·상태 select·footer 버튼 패턴 재사용. 잠재중대성은 등급 배지(점수 아님).
- 040-5 `RiskAssessInfo.vue 수정분`: footer 좌측에 [아차사고로 전환] 버튼 추가, 클릭 시 E6 호출 자리(골격 TODO). 실제 로직은 developer.

Vue 골격(template+style)은 본 작업과 함께 디스크에 작성됨(아래 "산출물" 참조). script는 `// TODO(developer):` 마커만.

---

## 6. 권한 매핑 (확정)

| AUTH_CD | NearMiss_01 USE_YN | BTN_SRCH | BTN_SAVE | 비고 |
|---|---|---|---|---|
| master, safe, system, 00001, 00004, 00006, 00008, 99999 | Y | Y | Y | 안전직군 전권. 정밀조사 저장 위해 BTN_SAVE=Y |
| hr | N | - | - | 인사 차단(Risk_03 동일) |

> Risk_03은 조회만(BTN_SRCH=Y, 나머지 N)이지만, 사건관리는 정밀조사 저장이 필수 기능이므로 **BTN_SAVE=Y**로 확장(저장 버튼 노출). 신규(BTN_NEW)는 웹에서 직접 사건 생성 안 함(재분류·앱 보고로만 생성) → N. 삭제(BTN_DELT)는 물리삭제 금지 → N. 엑셀(BTN_EXCL)은 통계 추후 → N. **결정필요 D1과 연동**(앱 관리자 조치범위).

---

## 7. 결정 필요 사항 (웹 영향분 — 설계문서 §7 발췌)

- **D1 (§7-1) 앱 관리자 조치 범위**: 본 웹 작업은 "정밀조사=웹 전용" 전제로 설계. 앱이 재발방지까지 쓰면 웹 화면 권한/필드 잠금 규칙 변경 필요. 권고=전자(상태전환+임시조치만 앱).
- **D2 (§7-2) 재분류 시 원 tb_risk_assessment 처리**: "이관" 상태값 신설(SYS011에 코드 추가) vs USE_YN='N'. E6 구현과 직결. 미확정이라 골격/서비스 TODO. 권고=이관 상태값 신설(추적 보존).
- **D3 (§7-3 / 대메뉴명) 잠재중대성·대메뉴명**: 잠재중대성=코드 3등급(본안, 단순) 확정. 대메뉴명은 "사건관리"(포괄) 제안 — "아차사고관리"로 좁힐지 확인.
- **D4 (§7-4) 경미사고 산재 게이트**: 본 도메인에 산업재해 보고 안내 게이트를 포함할지(웹 상세에 체크/안내) vs 별도 절차. 권고=웹 상세에 "산재 보고 대상 여부" 안내만(법정 신고는 별도).
- **D5 (스키마 정합) tb_syst_val 코드/CMPNY**: 코드그룹 테이블에 CMPNY_CD 없음 확정(전사 공통 코드). 권한 CMPNY_CD='001' 단일로 충분한지(멀티테넌트면 회사별 권한 추가) 확인.

> 사용자 선호: 객관식보다 사실보고+산문. 위 D1~D5는 미확정으로 두고, 웹 구현은 "권고안 + TODO"로 진행 가능(E6·산재게이트·대메뉴명만 확정 대기).
