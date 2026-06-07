# PRAFTA-050 작업지시서 — ChkLst_04 점검 불량(개선) 관리 화면 신규

- 요청서: `.claude/requests/web_requests/prafta-050.md`
- 영역: web (PRAFTA/prafta-backend + PRAFTA/prafta-web-frontend)
- 모듈: chkLst / chkLst04 (신규 서브모듈, chkLst01~03 구조 그대로 차용)
- 작성: planner (2026-06-07)
- 정책서 출처: `.claude/context/policies/` 내 점검불량(개선) 전용 비즈니스 룰은 없음.
  본 화면의 모든 규칙은 **요청서 prafta-050.md 본문 + 메인 세션 확정사항(Q1/Q2/Q3)**가 단일 출처.
  점검(체크리스트) 도메인은 기존 chkLst01~03 구현 패턴/권한·사업장 스코프 정책을 그대로 승계한다.
  → developer/security/qa 는 정책서 별도 정독 대상 없음. 단 security 단계에서 chkLst 표준
  cross-site IDOR 가드(사업장 스코프, chkLst03 `TB_USER_SITE_AUTH` 조인 패턴)는 반드시 적용.

---

## 0. 확정 사항 요약 (메인 세션 채팅 확정 — 변경 시 본 문서부터 갱신)

- **Q1 = (a)**: 조치 기록 = 불량 1건당 조치 1건(upsert, 수정 가능). 신규 테이블 PK = 불량 식별키
  (CMPNY_CD, SITE_CD, CHKPT_CD, INSPECT_ITEM_CD, WORK_DATE). 조치여부는 조치상세(행) 존재 여부로 파생.
- **Q2 = yes**: ChkLst_04 를 기존 점검 메뉴군(ChkLst_01~03) 하위 신규 소메뉴로 등록. 접근권한/사업장
  스코프는 ChkLst_01~03 동일 정책. cross-site IDOR 가드는 security 단계 표준 적용.
- **Q3 = yes**: 조치여부 조회 필터 select = 전체 / 조치완료 / 미조치. "조치완료 = 조치상세 입력됨"
  파생(별도 SYS코드·상태컬럼 추가 없음).
- **불량 원천값 불변(요청서 2번)**: `tb_chkpt_inspect_answer.INSPECT_ANSWER_TYPE` 의 'N'(불량)을
  조치했다고 'Y'로 절대 변경하지 않는다. 조치 내역은 신규 테이블에만 기록.
- 조회 기본 정렬: 점검일자(WORK_DATE) 최신순.
- 조회 그리드 컬럼(읽기전용): 조치여부 / 사업장 / 점검구분 / 점검대상명칭 / 점검항목명 / 불량내용(상세 버튼)
  / 점검자 / 점검일자 / 조치(입력 버튼). "상세"·"입력" 버튼만 동작.
- 두 검색팝업(점검대상/점검문항)은 **사업장 + 점검구분 선택 후에만** 열린다.

---

## 1. 확정 스키마 사실 (메인 세션 MCP 조회 — 추측 금지, 그대로 사용)

### tb_chkpt_inspect_answer (불량/양호 답변, 조회 원천)
PK (CMPNY_CD, SITE_CD, CHKPT_CD, INSPECT_ITEM_CD, WORK_DATE)
- INSPECT_ANSWER_TYPE varchar2 [SYS009] N=불량 / Y=양호  → 불량 = 'N'. **변경 금지**
- ANSWER_DESC text (불량 비고)
- FILE_MGMT_CD varchar50 (첨부사진)
- INSERT_NO varchar50 (점검자 = 입력자 user id)
- WORK_DATE varchar8 (점검일자 YYYYMMDD)
- INSERT_DATE datetime

### tb_chkpt_type_mgmt (점검대상 마스터)
PK (CMPNY_CD, SITE_CD, CHKLST_TYPE, CHKPT_CD)
- CHKPT_NM varchar100 (점검대상명칭), CHKPT_DESC varchar500 (비고)
- MGMT_USER_CD varchar20 (관리자ID), USE_YN

### tb_chkpt_inspect_item (점검항목 마스터)  ※사업장 무관
PK (CMPNY_CD, CHKLST_TYPE, INSPECT_ITEM_CD)
- INSPECT_ITEM_SUBJ varchar200 (점검항목명), STR_DATE varchar6 (시행월 YYYYMM), SORT_IDX, USE_YN

### 코드/조인 사실
- 점검구분 = CHKLST_TYPE = COM001 베이스코드 (baimValDCd / baimValDNm) — `/comApi/baseinfo/base-info-lists`
- SYS009: N=불량 / Y=양호
- 점검자 이름: `INSERT_NO`(= user id)를 `TB_USER` 와 조인해 `USER_NM` 표시.
  **확인됨**: TB_USER 컬럼은 `USER_CD`(PK), `USER_NM`. (chkLst01 매퍼: `A.MGMT_USER_CD = C.USER_CD`, `C.USER_NM`)

---

## 2. 단위 작업 분해

### PRAFTA-050-01 (backend) — 신규 테이블 + 메뉴/권한 시드 마이그레이션
- 유형: 신규 / backend(DB)
- 산출물:
  - `PRAFTA/prafta-backend/src/main/resources/sql/migration/prafta-050-chkpt-defect-action.sql`
    (신규 테이블 `tb_chkpt_defect_action` DDL — 본 문서 3장 그대로)
  - `PRAFTA/prafta-backend/src/main/resources/sql/migration/prafta-050-menu-register.sql`
    (ChkLst_04 소메뉴 + 권한 매핑 — 본 문서 4장)
- 운영 적용은 developer/메인세션이 수행(본 분해 시점 미적용 전제).
- 선행: 없음
- 우선순위 근거: 법적 책임 영역(chkLst) +1단계. 테이블/엔드포인트가 프론트 선행이므로 최우선.
- ⚠️ 확인 필요: ChkLst 대메뉴(MENU_M_ID) 실제 ID 와 기존 ChkLst_01~03 의 `MENU_IDX`·권한 `AUTH_CD`
  세트는 시드 DML 파일/스키마 dump 에 없다(운영 DB seed). developer/메인세션이 운영 DB에서
  `SELECT MENU_M_ID, MENU_IDX FROM tb_syst_menu_d WHERE MENU_D_ID IN ('ChkLst_01','ChkLst_02','ChkLst_03')`
  와 `SELECT AUTH_CD, USE_YN, BTN_* FROM tb_syst_auth_menu WHERE MENU_D_ID='ChkLst_03'` 를 조회해
  **동일 MENU_M_ID / 다음 MENU_IDX / 동일 권한 세트**로 4장 시드의 placeholder 를 확정해야 한다.

### PRAFTA-050-02 (backend) — chkLst04 조회/조치 upsert API
- 유형: 신규 / backend
- 패키지: `com.prafta.web.chkLst.chkLst04` (Controller/Service/Mapper, chkLst01~03 구조 그대로)
- 엔드포인트(prefix `/webApi/chkLst04`):
  1. `GET  /webApi/chkLst04/defect-lists`
     - params: siteCd, chkLstType, chkptCd(점검대상 선택값, optional), inspectItemCd(점검문항 선택값, optional),
       actionStatus(전체=''/조치완료='Y'/미조치='N')
     - 원천: tb_chkpt_inspect_answer WHERE INSPECT_ANSWER_TYPE='N' (불량만)
       + tb_chkpt_type_mgmt(점검대상명칭 chkptNm) + tb_chkpt_inspect_item(점검항목명 inspectItemSubj)
       + TB_USER(점검자명 inspectorNm = USER_NM, INSERT_NO=USER_CD 조인)
       + LEFT JOIN tb_chkpt_defect_action(조치상세 actionDesc; 존재시 조치완료)
     - actionStatus 파생 필터: 조치행 존재여부('Y'면 EXISTS, 'N'이면 NOT EXISTS)로 처리(상태컬럼 신설 X)
     - 정렬: WORK_DATE DESC (기본 최신순)
     - 사업장 스코프/IDOR: chkLst 표준대로 siteCd 가 호출자 권한 사업장인지 서버 검증(security 단계).
     - SELECT * 금지, leading-comma, `#{}` 바인딩 — CLAUDE.md 엄수.
  2. `GET  /webApi/chkLst04/defect-detail` (불량내용 상세: ANSWER_DESC + FILE_MGMT_CD + filePath)
     - 단건. 상세 팝업이 호출. (또는 목록 응답에 answerDesc/fileMgmtCd/filePath 포함시켜 별도 호출 생략 가능 —
       developer 판단. 기존 ChkLstRstPop 은 목록에 동봉된 데이터를 그대로 표시함.)
  3. `GET  /webApi/chkLst04/chkpt-target-lists` (점검대상 검색팝업용; siteCd+chkLstType 필수)
     - tb_chkpt_type_mgmt 에서 (chkptNm, mgmtUserNm[USER_NM 조인], chkptDesc, chkptCd) 반환, USE_YN='Y'
     - chkLst01 의 chkpt-lists 와 유사하나 팝업 표시 3컬럼만.
  4. `GET  /webApi/chkLst04/inspect-item-lists` (점검문항 검색팝업용; chkLstType 필수)
     - tb_chkpt_inspect_item 에서 (inspectItemSubj, strDate, inspectItemCd) 반환, USE_YN='Y'
     - chkLst02 의 chkpt-inspect-item-lists 재사용 가능(codeCd=chkLstType). 신규 만들지, 재사용할지 developer 판단.
  5. `POST /webApi/chkLst04/save-defect-action` (조치 입력/수정 upsert)
     - body: { siteCd, chkptCd, inspectItemCd, workDate, actionDesc }
     - upsert: PK 존재시 ACTION_DESC/UPDATE_NO/UPDATE_DATE 갱신, 없으면 INSERT. (INSERT ON DUPLICATE KEY UPDATE)
     - INSERT_NO/UPDATE_NO = 세션 사용자(gvUserCd). cmpnyCd = 세션값.
     - IDOR: 대상 불량행이 호출자 사업장 소속인지 서버 검증 필수(security).
- DTO 필드 대문자 유지 규칙은 매퍼 resultType(camelCase alias) 기존 컨벤션 따름(chkLst01~03 동일).
- 선행: PRAFTA-050-01
- 우선순위 근거: 프론트 선행 API. 법적 영역 +1.

### PRAFTA-050-03 (frontend-screen) — ChkLst_04.vue 조회 화면
- 유형: 신규 / frontend-screen
- 위치: `PRAFTA/prafta-web-frontend/prafta-web-frontend/src/views/chkLst/ChkLst_04.vue`
- 연결 UI 명세: UI-050-01
- planner 가 template+style 골격 작성(본 PR). developer 가 script(API/세션/검증) 채움.
- 선행: PRAFTA-050-02

### PRAFTA-050-04 (frontend-component) — ChkptTargetSearchPop.vue (점검대상 검색 팝업)
- 위치: `src/views/chkLst/popup/ChkptTargetSearchPop.vue`
- 연결 UI 명세: UI-050-02
- 표시 컬럼: 점검대상명칭 / 관리자 / 비고. 더블클릭 선택 → 부모에 (chkptCd, chkptNm) 반환.
- 선행: PRAFTA-050-02

### PRAFTA-050-05 (frontend-component) — InspectItemSearchPop.vue (점검문항 검색 팝업)
- 위치: `src/views/chkLst/popup/InspectItemSearchPop.vue`
- 연결 UI 명세: UI-050-03
- 표시 컬럼: 점검항목명 / 시행월. 더블클릭 선택 → 부모에 (inspectItemCd, inspectItemSubj) 반환.
- 선행: PRAFTA-050-02

> ※ 작업 5개 초과분(DefectDetailPop / DefectActionInputPop)은 같은 화면 묶음이라 함께 산출.
>   Notion 등록 시 PRAFTA-050-06 / PRAFTA-050-07 로 채번(아래).

### PRAFTA-050-06 (frontend-component) — DefectDetailPop.vue (불량내용 상세)
- 위치: `src/views/chkLst/popup/DefectDetailPop.vue`
- 연결 UI 명세: UI-050-04
- **재사용 판단**: ChkLstRstPop 은 "점검결과 확인서"(월간 전체·31일 그리드·프린트) 전용으로 무겁고
  목적이 다르다. 단건 불량(비고+사진)만 보여주면 되므로 **신규 경량 팝업 신설**이 맞다.
  단, 이미지 서빙 URL 조립 로직(`VITE_API_BASE_URL + filePath + '/' + fileMgmtCd`)과 사진 팝업 패턴은
  ChkLstRstPop 의 `openImagePopup` 구현을 그대로 차용(developer).
- 표시: 점검항목명 / 점검일자 / 비고(ANSWER_DESC) / 첨부사진(있으면 이미지, 없으면 "-").

### PRAFTA-050-07 (frontend-component) — DefectActionInputPop.vue (조치 입력)
- 위치: `src/views/chkLst/popup/DefectActionInputPop.vue`
- 연결 UI 명세: UI-050-05
- textarea 1개(조치 상세) + 저장/닫기. 기존 조치행 있으면 actionDesc 프리필(수정), 없으면 빈칸(신규).
- 저장 시 부모에 onSaved 콜백 → 목록 새로고침(조치여부 갱신).

---

## 3. 신규 테이블 DDL — tb_chkpt_defect_action

PK = 불량 1:1 (CMPNY_CD, SITE_CD, CHKPT_CD, INSPECT_ITEM_CD, WORK_DATE) — tb_chkpt_inspect_answer 와 동일 키.
조치여부는 본 테이블 행 존재 여부로 파생(별도 상태컬럼/코드 없음, Q3).

```sql
CREATE TABLE `tb_chkpt_defect_action` (
    `CMPNY_CD`        varchar(50)  NOT NULL COMMENT '회사코드',
    `SITE_CD`         varchar(50)  NOT NULL COMMENT '사업장코드',
    `CHKPT_CD`        varchar(50)  NOT NULL COMMENT '체크포인트 코드(점검대상)',
    `INSPECT_ITEM_CD` varchar(20)  NOT NULL COMMENT '점검항목코드',
    `WORK_DATE`       varchar(8)   NOT NULL COMMENT '점검일자(YYYYMMDD) — 불량 발생일',
    `ACTION_DESC`     text         NOT NULL COMMENT '조치 상세 내역(불량 처리 내용)',
    `INSERT_NO`       varchar(50)           DEFAULT NULL COMMENT '입력자(tb_user.USER_CD)',
    `INSERT_DATE`     datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    `UPDATE_NO`       varchar(50)           DEFAULT NULL COMMENT '수정자(tb_user.USER_CD)',
    `UPDATE_DATE`     datetime              DEFAULT NULL COMMENT '수정일시',
    PRIMARY KEY (`CMPNY_CD`, `SITE_CD`, `CHKPT_CD`, `INSPECT_ITEM_CD`, `WORK_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='점검 불량 조치(개선) 내역';
```

- 코드성 컬럼 없음 → SYSxxx 주석 규칙 적용 대상 컬럼 없음(요청서 4번 항목은 모두 원천 테이블 파생/조회 표시용).
- FK 제약은 기존 chkLst 테이블 관례상 생성하지 않음(논리적 1:1, 앱 레벨 정합).
- 마이그 파일: `prafta-backend/src/main/resources/sql/migration/prafta-050-chkpt-defect-action.sql`
  (헤더 주석 컨벤션 = 기존 prafta-near-miss-deploy.sql 스타일: 적용일/환경/부재확인 SELECT/멱등성 경고).

---

## 4. 메뉴/권한 시드 — ✅ 운영 DB 조회로 확정 (메인 세션 MCP)

ChkLst_04 를 기존 점검 대메뉴 하위 소메뉴로 등록. **placeholder 확정값**:
- MENU_M_ID = `chkLst` (ChkLst_01~03 동일)
- 다음 MENU_IDX = `4` (현재 ChkLst_01=1 / _02=2 / _03=3)
- MENU_D_ID = `ChkLst_04`, MENU_VIEW = `chkLst/ChkLst_04.vue`, MENU_NM = `점검 불량 관리`
- 권한 세트 = ChkLst_03 와 동일(아래 SELECT 복제가 정확): AUTH_CD 9건
  (00001/00004/00006/00008/99999/master/safe = 전부 USE_YN·BTN 'Y',
   `hr` = USE_YN·BTN 전부 'N', `system` = USE_YN 'Y'·BTN_SRCH 'Y'·NEW/DELT/SAVE/EXCL 'N')

```sql
-- (1) 소메뉴 등록
INSERT INTO tb_syst_menu_d (MENU_D_ID, MENU_M_ID, MENU_VIEW, MENU_NM, MENU_IDX, USE_YN, INSERT_NO, INSERT_DATE) VALUES
    ('ChkLst_04', 'chkLst', 'chkLst/ChkLst_04.vue', '점검 불량 관리', 4, 'Y', 'SYSTEM', NOW());

-- (2) 권한 매핑: ChkLst_03 의 AUTH_CD/USE_YN/BTN 세트를 그대로 복제(가장 안전·정확).
INSERT INTO tb_syst_auth_menu (CMPNY_CD, AUTH_CD, MENU_D_ID, USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, INSERT_NO, INSERT_DATE)
  SELECT CMPNY_CD, AUTH_CD, 'ChkLst_04', USE_YN, BTN_SRCH, BTN_NEW, BTN_DELT, BTN_SAVE, BTN_EXCL, 'SYSTEM', NOW()
    FROM tb_syst_auth_menu WHERE MENU_D_ID = 'ChkLst_03';
```

> 검증됨: tb_syst_menu_d (MENU_D_ID/MENU_M_ID/MENU_VIEW/MENU_NM/MENU_IDX/USE_YN, CMPNY_CD 없음),
> tb_syst_auth_menu (PK CMPNY_CD/AUTH_CD/MENU_D_ID, 버튼컬럼 BTN_SRCH/BTN_NEW/BTN_DELT/BTN_SAVE/BTN_EXCL, CMPNY_CD='001').
> 멱등성: 재적용 대비 INSERT 전 `DELETE FROM ... WHERE MENU_D_ID='ChkLst_04'` 또는 존재확인 SELECT 를 헤더에 둘 것.

---

## 5. 화면 명세

### UI-050-01 ChkLst_04 (점검 불량 관리)
- 연결 작업: PRAFTA-050-03
- 위치: src/views/chkLst/ChkLst_04.vue
- 참조 패턴: ChkLst_01.vue (viewComm/ViewHeader/viewSearch 사업장 코드-버튼-명칭 + COM001 select + data-grid)
- 레이아웃:
```
+------------------------------------------------------------------+
| ViewHeader [점검 불량 관리]                       (조회)          |
+------------------------------------------------------------------+
| viewSearch                                                       |
|  사업장 [코드][🔍][명칭]   점검구분[select COM001]                |
|  점검대상명칭 [input(disabled)][🔍]   점검문항 [input(disabled)][🔍]|
|  조치여부 [select 전체/조치완료/미조치]                            |
+------------------------------------------------------------------+
| viewBody  data-grid                                              |
|  No | 조치여부 | 사업장 | 점검구분 | 점검대상명칭 | 점검항목명     |
|       | 불량내용[상세] | 점검자 | 점검일자 | 조치[입력]           |
+------------------------------------------------------------------+
```
- 컴포넌트 매핑:
  | 영역 | 컴포넌트 |
  |------|----------|
  | 헤더/조회버튼 | ViewHeader |
  | 사업장 검색 | SiteSearchPop (useModal openPop) |
  | 점검구분 | native select + baseCodeArr['COM001'] (ChkLst_01 동일) |
  | 점검대상명칭 | input(disabled) + 버튼 → ChkptTargetSearchPop |
  | 점검문항 | input(disabled) + 버튼 → InspectItemSearchPop |
  | 불량내용 상세 | 행 [상세] 버튼 → DefectDetailPop |
  | 조치 입력 | 행 [입력] 버튼 → DefectActionInputPop |
- 상태별 동작:
  - loading: (developer) 조회 중 그리드 비움
  - empty: tbody 빈 행 "조회된 불량 항목이 없습니다." (colspan)
  - error: proxy.$alert(resolveApiErrorMessage)
  - success: 불량 목록 표시, 조치여부 = actionDesc 존재 시 "조치완료" else "미조치"
- 사용자 플로우: 진입(세션 사업장 프리필) → 점검구분 선택 → (선택) 점검대상/점검문항 팝업 선택 →
  조치여부 필터 → 조회 → 행의 [상세]로 불량 확인 / [입력]으로 조치 작성·저장 → 목록 갱신.
- 가드: 점검대상/점검문항 검색 버튼은 사업장+점검구분 미선택 시 alert 후 팝업 미오픈(developer).
- 백엔드 의존: GET /webApi/chkLst04/defect-lists (PRAFTA-050-02), /comApi/baseinfo/base-info-lists(COM001)

### UI-050-02 ChkptTargetSearchPop (점검대상 검색)
- 연결: PRAFTA-050-04 / 참조: SiteSearchPop(모달 셸·드래그·더블클릭 select) + ChkLst_01 데이터
- 표시 컬럼: 점검대상명칭 / 관리자 / 비고. 더블클릭 → onSelect(chkptCd, chkptNm).
- 백엔드 의존: GET /webApi/chkLst04/chkpt-target-lists (siteCd, chkLstType)

### UI-050-03 InspectItemSearchPop (점검문항 검색)
- 연결: PRAFTA-050-05 / 참조: SiteSearchPop 셸 + ChkLst_02 우측 그리드(점검항목명/시행월)
- 표시 컬럼: 점검항목명 / 시행월. 더블클릭 → onSelect(inspectItemCd, inspectItemSubj).
- 백엔드 의존: GET /webApi/chkLst04/inspect-item-lists (chkLstType)

### UI-050-04 DefectDetailPop (불량내용 상세)
- 연결: PRAFTA-050-06 / 참조: ChkLstRstPop 의 이미지 서빙·사진 팝업 패턴(차용, 단건 경량)
- 표시: 점검항목명 / 점검일자 / 비고(ANSWER_DESC) / 첨부사진(FILE_MGMT_CD 있으면 이미지)
- 백엔드 의존: 목록 응답 동봉 데이터 사용(별도 호출 불요) 또는 GET /webApi/chkLst04/defect-detail

### UI-050-05 DefectActionInputPop (조치 입력)
- 연결: PRAFTA-050-07 / 참조: 일반 모달 + textarea
- 입력: 조치 상세 textarea(필수). 기존행 있으면 프리필(수정). 저장/닫기.
- 백엔드 의존: POST /webApi/chkLst04/save-defect-action (upsert)

---

## 6. 미해결 / 확인 필요 (developer/메인세션)

1. **메뉴/권한 시드 placeholder**: ChkLst 대메뉴 MENU_M_ID, 다음 MENU_IDX, ChkLst_03 권한 세트를
   운영 DB에서 조회해 4장 placeholder 확정(seed 데이터가 마이그 파일/스키마 dump에 없음).
2. **조치 저장 권한(BTN_SAVE)**: 본 화면 [입력] 저장이 메뉴 권한과 연동되어야 하는지(ChkLst_03 정책 확인).
   "상세/입력"은 표준 5버튼 밖 인라인 버튼이라 메뉴 BTN 과 별개일 수 있음 — security/메인 확인.
3. **defect-detail 별도 EP vs 목록 동봉**: ChkLstRstPop 은 목록에 비고/사진을 동봉. developer 가
   목록 응답에 answerDesc/fileMgmtCd/filePath 를 포함해 상세 팝업 별도 호출을 생략할지 결정.
4. **inspect-item-lists 재사용**: chkLst02 의 `/webApi/chkLst02/chkpt-inspect-item-lists`(codeCd=chkLstType)
   를 그대로 호출할지, chkLst04 전용 EP를 신설할지 — 도메인 경계상 chkLst04 신설 권장(분리), developer 판단.
5. 마이그 2종 운영 미적용 전제(developer/메인세션이 부재확인 후 적용).

---

## 7. 산출물 목록
- 작업지시서: 본 파일
- 마이그(설계만, 본 문서 3·4장): prafta-050-chkpt-defect-action.sql / prafta-050-menu-register.sql (developer가 파일화·적용)
- Vue 골격(본 PR로 디스크 생성):
  - src/views/chkLst/ChkLst_04.vue
  - src/views/chkLst/popup/ChkptTargetSearchPop.vue
  - src/views/chkLst/popup/InspectItemSearchPop.vue
  - src/views/chkLst/popup/DefectDetailPop.vue
  - src/views/chkLst/popup/DefectActionInputPop.vue
