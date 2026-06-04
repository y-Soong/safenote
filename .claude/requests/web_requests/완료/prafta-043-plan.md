# prafta-043 — 초과근무 유형(OT_TYPE) 전면 파기 · 작업 분해 (planner)

> 단일 출처. 원 요청서: `.claude/requests/web_requests/prafta-043.md`.
> 선행: prafta-app-016(앱 신청 폼 유형 입력 제거 + `tb_user_attd_req.OT_TYPE` NULL 저장) 완료 전제.
> 본 작업 = 그 위에서 DB 컬럼 · 웹 관리자 표시/입력 · 전 계층 정합까지 완전 제거.

---

## 0. 정독 결과 / 사실 확인 (추측 아님 — 코드·스키마 직접 확인)

### 0-1. DB 스키마 (schema-full.sql L1044, L1185 직접 확인)
- `tb_user_attd_req.OT_TYPE` : `varchar(10)` **NULL 허용, 기본값 없음**, 인덱스/제약 없음. 값은 리터럴 `EXTEND/NIGHT/HOLIDAY`. (신청 행 — app-016이 NULL 저장.)
- `tb_user_overtime_mgmt.OT_TYPE` : `varchar(10)` **NOT NULL, 기본값 없음**, 인덱스/PK/FK에 미포함. 값은 리터럴 `EXTEND/NIGHT/HOLIDAY`. (최종 정산 행 — 관리자 승인 시 INSERT/UPDATE.)
- **두 컬럼 모두 SYS 공통코드와 무관**(컬럼 COMMENT에 `[SYSxxx]` 태그 없음, 자유 텍스트 리터럴). → **SYS 코드 폐기/보존 결정 불요**(애초에 등록 없음). AppReq06ServiceImpl 주석도 "자유 텍스트 컬럼"임을 명시(§7 follow-up 메모 존재).

### 0-2. 별개 도메인(파기 금지) — 요청서 가정 정정
- 요청서 §2 "baim04/baim05의 OT_TYPE(별개 도메인)"는 **사실과 다름**. baim04/baim05/`common.cmm.dailyjoin` 에는 `OT_TYPE` 컬럼이 **존재하지 않는다.** 해당 파일들의 grep 매칭은 전부 `SLOT_TYPE` / `slotType`(문자열 "sl**otTyp**e" 부분일치) 오탐.
- 즉 **"초과근무와 무관한 OT_TYPE"는 시스템에 없다.** 일일계정 슬롯은 `SLOT_TYPE`(SYS014)이며 본 작업과 무관 → **건드리지 않음**(명시).

### 0-3. 정책서 출처 (INDEX 경유)
- 근태관리 정책서 `attd/09-requests-approval.md` §9.3.3(초과근무 발생 케이스: 조기출근/연장/휴일근무) / §9.3.4(관리자 승인 액션) / §9.6.3(상세 패널 — 계산 비교 3단). **§9.3.3은 "발생 시나리오" 설명일 뿐 저장형 유형값을 정의하지 않는다.** 정책서 어디에도 "OT_TYPE(연장/야간/휴일) 값을 저장·관리·표시하라"는 규정이 **없다.**
- 결론: **OT_TYPE은 정책서가 요구하지 않은 구현 산출물**이다. 파기는 정책서와 충돌하지 않으며, 정책서 본문 정정은 불요. 단, §9.3.4/§9.6.3 부근에 "유형(연장/야간/휴일)을 저장/표시하지 않는다"는 명확화 1줄 + CHANGELOG 항목만 추가(혼동 방지 목적). → 작업 PRAFTA-043-009.

### 0-4. 에러코드
- `ATTD_400_095`("초과근무 유형이 올바르지 않습니다.") : app-016에서 사용처 제거되어 **현재 호출 0건(데드)**. 전 계층 OT_TYPE 제거 후 완전 삭제 가능. (단, enum 상수 삭제 시 다른 참조 0건 재확인 — 본 분해 PRAFTA-043-001에 포함.)

### 0-5. 프론트 실태 (AttdDayDetailPop 정정)
- 요청서는 "AttdDayDetailPop에 유형 입력 UI"가 있다고 가정하나, **실제 OT 입력 row 템플릿(L519~575)에는 유형 선택 UI가 없다.** 시작/종료 날짜·시각 입력뿐. `type:'extend'`는 내부 하드코딩 기본값이며 화면에 노출되지 않는다. → **이 팝업은 템플릿 변경 없음(유형 셀렉터 자체가 없음)**, 순수 script(`mapOtType`/`reverseOtType`/기본값/payload otType) 제거만.
- Attd_08.vue : `otTypeLabel`/`workTypeLabel`이 "초과근무(연장/야간/휴일)"을 실제 표시(L808~826). → "초과근무"로 격하.
- Attd_10.vue : 승인 payload `otType: r.otType` 전달(L454). → 제거.
- 앱: OvertimeForm.vue L113 안내문구 "초과근무 유형(연장/야간/휴일)은 승인 시 확정돼요." → 거짓이 되므로 정정/제거. (payload otType은 app-016에서 이미 제거됨.)

---

## 1. 작업 분해 결과

> 화면 신규/컴포넌트 신설 없음. 전부 백엔드·DB·기존 화면의 라벨/입력 제거(보완/리팩터링/버그수정). 따라서 UI 명세(UI-xxx)·Vue 골격 산출물 없음.

### PRAFTA-043-001 — 웹 attd07 OT_TYPE 제거 (승인/등록/수정 본체)
- **유형**: backend · **영역**: web · **모듈**: attd/attd07 · **작업유형**: 리팩터링(필드 제거)
- **요구사항 요약**: 관리자 초과근무 등록/승인/수정 경로에서 OT_TYPE 입력·검증·저장을 제거(요청서 요청 3·4).
- **정책서 출처**: 근태 §9.3.4(승인 액션), §9.6.3(상세 패널) — 유형값 비저장 명확화 대상.
- **상세**:
  - 핵심: 1) 요청 DTO `OvertimeItemRequest.otType` 필드 + `@Pattern(EXTEND|NIGHT|HOLIDAY)` 검증 제거. 2) `OvertimeItemModel(otType)` / `InsertUserOvertimeCommand(otType)` / `UpdateUserOvertimeRequestParam.from`의 otType 매핑 제거. 3) `Attd07Mapper.updateUserOvertimeModify`의 `@Param otType` 제거. 4) `Attd07ServiceImpl` L972 modify 호출 인자 otType 제거. 5) 에러코드 `ATTD_400_095` 삭제(전 참조 0건 재확인 후).
  - 영향 파일:
    - `web/attd/attd07/dto/request/OvertimeItemRequest.java`
    - `web/attd/attd07/dto/request/UpdateUserOvertimeRequestRequest.java`(javadoc otType 서술 정정)
    - `web/attd/attd07/application/model/OvertimeItemModel.java`
    - `web/attd/attd07/application/command/InsertUserOvertimeCommand.java`
    - `web/attd/attd07/application/param/UpdateUserOvertimeRequestParam.java`
    - `web/attd/attd07/service/impl/Attd07ServiceImpl.java`(L972 등)
    - `web/attd/attd07/mapper/Attd07Mapper.java`(updateUserOvertimeModify @Param otType)
    - `web/attd/attd07/result/{MonthlyOvertimeResult,DailyOvertimeResult,MonthlyAttdReqResult}.java`(otType 필드 — 조회 응답에서 제거; 단 record 컬럼순서 함정 주의)
    - `common/error/attd/AttdErrorCode.java`(ATTD_400_095)
  - endpoint: `POST /webApi/attd07/update-user-overtime-requests`, OT 조회 endpoint들.
  - 예상 산출물: DTO/Command/Param/Model/Service/Mapper(java) 수정.
- **선행 작업**: 없음(코드 제거가 마이그보다 선행). PRAFTA-043-008(마이그)보다 **반드시 먼저**.
- **우선순위 근거**: 법적 책임 영역(attd) +1 격상 + 컬럼 DROP 선행조건(런타임 안전). **1순위.**

### PRAFTA-043-002 — 웹 attd07 Mapper.xml OT_TYPE 제거
- **유형**: backend · **영역**: web · **모듈**: attd/attd07 · **작업유형**: 리팩터링
- **요구사항 요약**: `Attd07Mapper.xml`의 OT_TYPE SELECT/INSERT/UPDATE 컬럼 제거.
- **정책서 출처**: 근태 §9.3.4.
- **상세**:
  - 핵심: 1) SELECT `O.OT_TYPE AS otType`(L494, L1212) 제거. 2) `A.OT_TYPE AS otType`(신청행 조회 L977) 제거. 3) INSERT 컬럼/값 `OT_TYPE`,`#{otType}`(L1688, L1721) 제거. 4) `updateUserOvertimeModify`의 `SET OT_TYPE = #{otType}`(L1750) 제거.
  - 영향 파일: `web/attd/attd07/mapper/Attd07Mapper.xml`
  - 주의: resultMap이 record(위치기반)면 SELECT 컬럼 제거 시 PRAFTA-043-001의 record 필드 제거와 **동일 순서 정합** 필수(메모리: MyBatis record 컬럼순서 함정).
  - 예상 산출물: mapper xml 수정.
- **선행 작업**: PRAFTA-043-001(DTO/record와 동시 정합).
- **우선순위 근거**: attd +1. 001과 한 묶음. **1순위.**

### PRAFTA-043-003 — 웹 reqinbox / attd08 OT_TYPE 조회 제거
- **유형**: backend · **영역**: web · **모듈**: attd/reqinbox, attd/attd08 · **작업유형**: 리팩터링
- **요구사항 요약**: 접수함·근태목록 조회 결과의 otType 노출 제거.
- **정책서 출처**: 근태 §9.6.3.
- **상세**:
  - 핵심: 1) `reqinbox/result/PendingReqResult.otType` + `ReqInboxMapper.xml` `R.OT_TYPE AS otType`(L26) 제거. 2) `attd08/result/AttdListsResult.otType` + `Attd08Mapper.xml`(L146 NULL AS otType, L261 O.OT_TYPE AS otType) 제거.
  - 영향 파일: `web/attd/reqinbox/result/PendingReqResult.java`, `web/attd/reqinbox/mapper/ReqInboxMapper.xml`, `web/attd/attd08/result/AttdListsResult.java`, `web/attd/attd08/mapper/Attd08Mapper.xml`
  - 주의: record 컬럼순서 — AttdListsResult는 NORMAL/ OT 두 UNION 가지 모두 동일 위치(L146/L261)에서 제거해야 함.
  - 예상 산출물: result java + mapper xml 수정.
- **선행 작업**: 없음(독립). 단 프론트(PRAFTA-043-005,006)와 응답계약 동시 정합.
- **우선순위 근거**: attd +1. **2순위.**

### PRAFTA-043-004 — 앱 req07/req06 OT_TYPE 잔여 제거
- **유형**: backend · **영역**: app · **모듈**: app/req/req07, app/req/req06 · **작업유형**: 리팩터링
- **요구사항 요약**: 앱 신청 INSERT의 OT_TYPE 컬럼 매핑 잔여 + 내 요청 목록의 otType 라벨 노출 제거.
- **정책서 출처**: 근태 §9.3.1, §9.3.4.
- **상세**:
  - 핵심:
    1) `AppReq07Mapper.xml` INSERT 컬럼/값 `OT_TYPE`,`#{otType}`(L34,L56) 제거. (현재 항상 NULL 저장 — 컬럼 DROP 전 제거 필수.)
    2) `app/req/req07/application/command/AttdReqInsertCommand.otType` 필드 + 호출부(AppReq07ServiceImpl L102/193/266의 `null //OT_TYPE` 인자) 제거. javadoc otType 서술 정정.
    3) `app/req/req07/dto/request/{OvertimeRequest,SlotRequest}` 의 `otType` 필드 제거(공유 DTO 호환 명분 소멸 — 더 이상 보존 불요). 앱 FE는 이미 미전송(app-016).
    4) `app/req/req06/result/MyReqItemResult.otType` + `AppReq06Mapper.xml` `A.OT_TYPE AS otType`(L70) 제거.
    5) `AppReq06ServiceImpl` `OT_TYPE_LABEL` 맵 + 설명 문자열에 `· 연장/야간/휴일` append 로직(L61~, L141~146) 제거.
  - 영향 파일: `app/req/req07/mapper/AppReq07Mapper.xml`, `app/req/req07/application/command/AttdReqInsertCommand.java`, `app/req/req07/service/impl/AppReq07ServiceImpl.java`, `app/req/req07/dto/request/OvertimeRequest.java`, `app/req/req07/dto/request/SlotRequest.java`, `app/req/req06/result/MyReqItemResult.java`, `app/req/req06/mapper/AppReq06Mapper.xml`, `app/req/req06/service/impl/AppReq06ServiceImpl.java`
  - endpoint: `POST /appApi/req07/*`(신청), `GET /appApi/req06/*`(내 요청).
  - 예상 산출물: command/dto/result/service/mapper 수정.
- **선행 작업**: 없음(독립). PRAFTA-043-008(마이그)보다 먼저(특히 AppReq07Mapper INSERT).
- **우선순위 근거**: attd +1. app FE 계약 정합(req06 응답에서 otType 제거). **2순위.**

### PRAFTA-043-005 — 웹 프론트 Attd_08 근무구분 격하 + Attd_10 payload 제거
- **유형**: frontend-screen · **영역**: web · **모듈**: attd · **작업유형**: 보완(라벨 격하)
- **요구사항 요약**: Attd_08 "초과근무(유형)" → "초과근무" 격하, Attd_10 승인 payload otType 제거(요청서 요청 2·3).
- **정책서 출처**: 근태 §9.6.3, §14(근태 현황 조회).
- **상세**:
  - 핵심:
    1) `Attd_08.vue` `otTypeLabel` 함수 제거, `workTypeLabel`을 `_isOt ? "초과근무" : "정상근무"`로 단순화(L808~826). 템플릿 주석 "초과근무(유형)" → "초과근무"로 정정(L126,L194). otType 참조(매핑 L893 부근 OT 전용 매핑에서 otType 소비처) 정리.
    2) `Attd_10.vue` overtimes payload에서 `otType: r.otType` 제거(L454).
  - 영향 파일: `prafta-web-frontend/.../src/views/attd/Attd_08.vue`, `prafta-web-frontend/.../src/views/attd/Attd_10.vue`
  - 백엔드 의존: PRAFTA-043-001/003 응답에서 otType 제거 → FE 미참조 정합.
  - 작성 범위: 기존 화면 보완(라벨/페이로드). **신규 화면/컴포넌트 없음 → Vue 골격 산출물 없음.** 단 라벨 텍스트는 기존 화면 패턴 그대로, CSS 변수 규칙 영향 없음(스타일 무변경).
  - 예상 산출물: 기존 .vue 2종 수정(developer가 script/template 정리).
- **선행 작업**: PRAFTA-043-001, PRAFTA-043-003.
- **우선순위 근거**: API 제거 후 화면 정합(백엔드 우선 원칙). **3순위.**

### PRAFTA-043-006 — 웹 프론트 AttdDayDetailPop OT 유형 매핑 제거
- **유형**: frontend-component · **영역**: web · **모듈**: attd/popup · **작업유형**: 리팩터링
- **요구사항 요약**: 관리자 OT 편집 팝업의 유형 매핑/기본값/payload otType 제거(요청서 요청 1·3).
- **정책서 출처**: 근태 §9.3.4.
- **상세**:
  - 핵심: 1) `reverseOtType`(L1654) 및 프리필 시 `type: reverseOtType(ot.otType)`(L1735) 제거. 2) `addOt`의 기본값 `type:"extend"`(L1804) 제거. 3) `mapOtType`(L2177) 제거 및 저장 payload `otType: mapOtType(o.type)`(L2199) 제거. 4) otList 자료구조 주석(L1644)·L2444 otType 참조 정리.
  - **템플릿 변경 없음**(OT row 템플릿 L519~575에 유형 셀렉터 미존재). 순수 script 정리.
  - 영향 파일: `prafta-web-frontend/.../src/views/attd/popup/AttdDayDetailPop.vue`
  - 백엔드 의존: PRAFTA-043-001(payload otType 미수용), PRAFTA-043-002(조회 otType 미반환).
  - 예상 산출물: 기존 .vue 1종 script 수정.
- **선행 작업**: PRAFTA-043-001, PRAFTA-043-002.
- **우선순위 근거**: API 제거 후 화면 정합. **3순위.**

### PRAFTA-043-007 — 앱 프론트 OvertimeForm 안내문구 정정
- **유형**: frontend-component · **영역**: app · **모듈**: req · **작업유형**: 버그수정(거짓 안내문)
- **요구사항 요약**: "유형은 승인 시 확정돼요" 안내가 파기 후 거짓 → 정정/제거.
- **정책서 출처**: 근태 §9.3.1.
- **상세**:
  - 핵심: `OvertimeForm.vue` L113 안내문구에서 "초과근무 유형(연장/야간/휴일)은 승인 시 확정돼요." 문장 제거(나머지 "관리자 승인 후 추가근무로 반영돼요."는 유지). 파일 상단 주석(L6: #2 유형 칩 제거)도 본 작업 반영 1줄 보강 가능.
  - 영향 파일: `prafta-app-frontend/.../src/views/req/components/OvertimeForm.vue` (+ `AttdRequestView.vue` 주석 L122는 사실과 부합 — 변경 선택).
  - 예상 산출물: 기존 .vue 1종 문구 수정.
- **선행 작업**: 없음(독립, 즉시 가능).
- **우선순위 근거**: 단순 문구. **4순위.**

### PRAFTA-043-008 — DB 마이그레이션 (OT_TYPE 컬럼 파기, expand/contract 2단계)
- **유형**: backend(DB) · **영역**: web+app 공통 · **모듈**: db/migration · **작업유형**: 신규(마이그)
- **요구사항 요약**: 두 OT_TYPE 컬럼 안전 제거(요청서 요청 4). 운영 미적용(파일만).
- **정책서 출처**: 없음(스키마 변경 — 기술 영역). CLAUDE.md DB 규칙.
- **상세**:
  - 핵심: 보안 검토 High(배포 순서 가용성 리스크) 해소를 위해 단일 DROP을 **expand/contract 2단계**로 분리(아래 §3 설계).
    1) **EXPAND** `prafta-043-1-expand-ot-type-nullable.sql`: `tb_user_overtime_mgmt.OT_TYPE`을 `MODIFY COLUMN ... NULL DEFAULT NULL`로 완화(NOT NULL 해제). `tb_user_attd_req.OT_TYPE`은 이미 nullable → 확인만. 효과: 구/신버전 코드 모두 동작 → 배포 순서 무관, 가용성 창 제거.
    2) **CONTRACT** `prafta-043-2-contract-drop-ot-type.sql`: 두 컬럼 `DROP COLUMN`. 각 롤백 SQL(ADD COLUMN, 원래 타입/NULL여부/위치 AFTER 복원) 동반.
  - **적용 순서(완화됨)**: ① EXPAND 적용(아무 때나, 구버전 안전) → ② PRAFTA-043-001~004(전 코드 OT_TYPE 참조 제거) 배포·재기동 → ③ 안정화 후 CONTRACT 적용. EXPAND만 적용하고 CONTRACT를 미뤄도 무해(컬럼 잔존하나 NULL 허용·무참조). 운영 적용은 사용자 수동.
  - 예상 산출물: `prafta-backend/src/main/resources/sql/migration/prafta-043-1-expand-ot-type-nullable.sql`, `prafta-043-2-contract-drop-ot-type.sql`(둘 다 운영 미적용). ※ 기존 단일 `prafta-043-drop-ot-type.sql`은 삭제(2파일로 대체).
- **선행 작업**: CONTRACT만 PRAFTA-043-001, 002, 003, 004 (전부)에 종속. EXPAND는 선행 없음(코드 배포보다 먼저 적용 권장).
- **우선순위 근거**: 데이터 정합/스키마 변경. EXPAND는 가장 먼저, CONTRACT는 가장 마지막. **5순위(논리상 종착).**

### PRAFTA-043-009 — 정책서 명확화 + CHANGELOG
- **유형**: 문서(정책) · **영역**: 공통 · **모듈**: policies/attd · **작업유형**: 보완
- **요구사항 요약**: 초과근무 유형 비저장/비표시 명확화 1줄 + CHANGELOG 항목.
- **정책서 출처**: 근태 `attd/09-requests-approval.md` §9.3.4 / §9.6.3.
- **상세**:
  - 핵심: 1) §9.3.4 또는 §9.6.3 부근에 "초과근무는 유형(연장/야간/휴일)을 저장·입력·표시하지 않는다(시스템 단일 '초과근무')" 명확화 1줄 + 변경이력 주석. (§9.3.3 발생 케이스 표는 시나리오 설명으로 유지.) 2) `CHANGELOG.md`에 prafta-043 항목 추가.
  - 영향 파일: `.claude/context/policies/attd/09-requests-approval.md`, `.claude/context/policies/CHANGELOG.md`
  - 예상 산출물: 정책서 md 2종 수정.
- **선행 작업**: 없음(병행 가능). 코드 결정 확정 후 문구 확정 권장.
- **우선순위 근거**: 코드 개선/문서. **4순위.**

---

## 2. 의존성 그래프 (코드제거 ↔ 마이그 순서)

```
[코드 OT_TYPE 참조 제거 — 컬럼 DROP의 선행조건]
  PRAFTA-043-001 (web attd07 java) ──┐
  PRAFTA-043-002 (web attd07 xml) ───┤  (001↔002 record 컬럼순서 동시 정합)
  PRAFTA-043-003 (reqinbox/attd08) ──┤
  PRAFTA-043-004 (app req07/req06) ──┘
  PRAFTA-043-008-EXPAND (overtime_mgmt.OT_TYPE NULL 허용)  ← 코드 배포보다 먼저 적용 가능(하위호환)
            │ (전부 완료 + 재컴파일/재기동)
            ▼
  PRAFTA-043-008-CONTRACT (DB 마이그: 두 OT_TYPE 컬럼 DROP)  ← 운영 미적용, 사용자 수동(안정화 후)

[프론트 정합 — 해당 백엔드 응답/계약 제거 후]
  001,003 → PRAFTA-043-005 (Attd_08 격하 / Attd_10 payload)
  001,002 → PRAFTA-043-006 (AttdDayDetailPop script)
  (독립)   PRAFTA-043-007 (앱 OvertimeForm 문구)

[문서]
  (병행)   PRAFTA-043-009 (정책서 명확화 + CHANGELOG)
```

**핵심 안전 규칙(expand/contract 적용 후 완화)**: `tb_user_overtime_mgmt.OT_TYPE`이 NOT NULL이라 단일 DROP은 "코드 배포↔마이그" 순서를 엄격히 지켜야 했다(그 사이 창에서 신버전 컬럼 생략 INSERT는 NOT NULL 위반, 구버전은 Unknown column으로 실패). → 보안 검토 High 해소를 위해 **EXPAND(NULL 허용)를 먼저 적용**하면 구/신버전 모두 안전해져 배포 순서 의존이 사라진다. 권장 순서: ① EXPAND → ② 001~004 코드 배포·재기동 → ③ 안정화 후 CONTRACT(DROP). EXPAND만 적용하고 CONTRACT 보류해도 무해.

---

## 3. 마이그레이션 파일 설계 — expand/contract 2단계 (운영 미적용 — 파일만)

보안 검토 High(배포 순서 가용성 리스크) 해소를 위해 단일 DROP을 2파일로 분리한다.

파일 1 (EXPAND): `prafta-backend/src/main/resources/sql/migration/prafta-043-1-expand-ot-type-nullable.sql`
파일 2 (CONTRACT): `prafta-backend/src/main/resources/sql/migration/prafta-043-2-contract-drop-ot-type.sql`
※ 기존 단일 `prafta-043-drop-ot-type.sql`은 삭제(위 2파일로 대체).

설계 의도:
- **EXPAND**: `tb_user_overtime_mgmt.OT_TYPE`의 NOT NULL만 해제(`MODIFY COLUMN OT_TYPE varchar(10) NULL DEFAULT NULL`, 타입/COLLATE/위치 유지). 이렇게 하면 구버전(값 INSERT)·신버전(컬럼 생략 INSERT) 코드가 모두 동작 → 배포 순서 무관, 가용성 창 제거. `tb_user_attd_req.OT_TYPE`은 이미 nullable이라 EXPAND 불요(확인만).
- **CONTRACT**: 두 컬럼 모두 인덱스/FK/PK 미포함 → 단순 `ALTER TABLE ... DROP COLUMN`. 신버전 코드 전면 롤아웃·안정화 후에만 적용.
- 멱등성: MySQL 8.0.42는 `DROP COLUMN IF EXISTS` 미지원 → 기존 마이그 관례(단순 ALTER + 1회 적용 주석)에 맞춰 단순 DDL + 헤더 주석으로 처리. EXPAND의 MODIFY는 동일 정의 재실행 안전.
- 롤백 SQL 동반:
  - EXPAND 롤백: `MODIFY COLUMN OT_TYPE varchar(10) NOT NULL`(NULL 행 있으면 백필 후). 신버전 미배포 직후라면 NULL 행이 없어 바로 복원 가능.
  - CONTRACT 롤백: `tb_user_attd_req`는 `ADD COLUMN OT_TYPE varchar(10) NULL ... AFTER END_TIME`(원래 nullable, 무손실 구조 롤백). `tb_user_overtime_mgmt`는 우선 `ADD COLUMN ... NULL ... AFTER NODE_CD`로 복원 후 필요 시 백필→NOT NULL 재부여(2단계). 값 무손실 롤백 불가(파기 본질).
- 백업 안내 주석: 적용 전 OT_TYPE 값 보존이 필요하면 `CREATE TABLE ... AS SELECT ... WHERE OT_TYPE IS NOT NULL` 스냅샷 권고(파기 결정상 불요지만 안전망).

DDL 골자:
```sql
-- [EXPAND] prafta-043-1 : 가용성 창 제거(하위호환) — 코드 배포보다 먼저 적용 가능
ALTER TABLE tb_user_overtime_mgmt
    MODIFY COLUMN OT_TYPE varchar(10) COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL;  -- NOT NULL 해제
-- tb_user_attd_req.OT_TYPE 은 이미 nullable → 변경 불요(SHOW COLUMNS 확인만)

-- [CONTRACT] prafta-043-2 : 신버전 전면 롤아웃·안정화 후 적용
ALTER TABLE tb_user_attd_req       DROP COLUMN OT_TYPE;   -- nullable, 신청행(app-016 NULL저장), AFTER END_TIME
ALTER TABLE tb_user_overtime_mgmt  DROP COLUMN OT_TYPE;   -- EXPAND후 NULL, 최종정산행, AFTER NODE_CD

-- 롤백(파기 본질상 값은 복원 불가 — 컬럼 구조만 복원):
-- ALTER TABLE tb_user_attd_req      ADD COLUMN OT_TYPE varchar(10) NULL DEFAULT NULL COMMENT '초과근무 유형(복원)' AFTER END_TIME;
-- ALTER TABLE tb_user_overtime_mgmt ADD COLUMN OT_TYPE varchar(10) NULL DEFAULT NULL COMMENT '초과근무 유형(복원, NOT NULL 재부여 전 백필 필요)' AFTER NODE_CD;
```

운영 런북 체크리스트(권장 순서):
- [ ] ① EXPAND(prafta-043-1) 적용 — 아무 때나, 구버전 안전.
- [ ] ② 신버전 코드(043-001~004) 배포·재기동 — OT_TYPE 무참조.
- [ ] ③ 안정화 확인 후 CONTRACT(prafta-043-2) 적용 — 두 컬럼 DROP.
- 부분 배포/역순 위험: CONTRACT를 신버전 롤아웃 전 적용하면 구버전 Unknown column 실패. EXPAND 생략 시 신버전 NOT NULL 위반. EXPAND만 적용·CONTRACT 보류는 무해.

---

## 4. 확정 설계 결정 (자율 진행 — 합리적 기본값)

- **D1 마이그 전략 = 컬럼 DROP**(nullable화 후 방치 아님). 근거: 두 컬럼 모두 비-인덱스·비-FK 자유 텍스트, 정책상 비저장 확정 → 잔존 시 혼동/오용 소지. DROP이 "전면 파기" 의도에 부합. 운영 미적용·롤백 SQL 동반.
- **D2 근무구분 격하 = "초과근무" 단일 표기.** 유형 표기 손실 수용(사용자 결정=파기). attd08/attd11/월마감/리포트가 OT_TYPE으로 **집계·분기하지 않음**을 확인(attd08은 표시만, attd11은 OT_TYPE 미참조 — grep 0건). 대체 정의 불요.
- **D3 승인/편집 = 유형 없이 성립.** `InsertUserOvertimeCommand`/modify 모두 otType만 빠지고 시각·근무분으로 정산 성립(OT_TYPE은 정산 계산에 미사용). 무결성 영향 없음.
- **D4 전 계층 동시 정합 = 단일 라운드 배포.** 001~006 동일 PR/배포, 008은 그 후 수동 적용. record 컬럼순서(001↔002, 003) 동시 수정 강제.
- **D5 별개 도메인 = 슬롯은 SLOT_TYPE이며 OT_TYPE 부재 → 무변경.** (요청서 가정 정정.)
- **D6 SYS 코드 = 해당 없음.** OT_TYPE은 SYS 미등록 자유 텍스트.
- **D7 정책서 = 본문 정정 불요(유형값을 정의한 적 없음), 명확화 1줄 + CHANGELOG만**(PRAFTA-043-009).
- **D8 ATTD_400_095 = 완전 삭제**(데드, 참조 0건 — 삭제 전 재확인).

---

## 5. 채팅 확인 필요 질문 (메인 세션 → 사용자)

1. **(낮음) ATTD_400_095 enum 완전 삭제 vs 보존(주석 데드 처리)**: 본 분해는 "완전 삭제"로 기본 결정(app-016이 보존했던 것을 043에서 제거). 외부 안정성(에러코드 번호 재사용 금지 관례)이 우려되면 "데드 보존"으로 전환 가능. → 기본값 삭제로 진행, 이견 시 알려주세요.
2. **(낮음) 마이그 멱등 가드 수준**: 기존 마이그가 단순 ALTER 위주라 043도 단순 DROP + 주석으로 갈지, `information_schema` 가드로 재실행 안전성까지 줄지. → developer가 프로젝트 관례에 맞춤(기본=단순 DROP, 운영 미적용).
3. **(정보)**: 요청서 §2의 "baim04/baim05 별개 OT_TYPE"은 실제 부재(`SLOT_TYPE` 오탐)였음을 보고. 추가 조치 불요.

---

## 6. 메인 세션이 Notion에 반영할 항목

> 서브에이전트는 Notion 접근 없음. 아래를 "작업 로그" DB(상태=분해완료, 담당=planner)에 등록 요청.
> (UI 신규/컴포넌트 신설 없음 → "도메인 지식 베이스" 신규 등록 없음. 기존 화면 보완만.)

| 작업ID | 영역 | 모듈 | 작업유형 | 요구사항 요약 | 선행 |
| --- | --- | --- | --- | --- | --- |
| PRAFTA-043-001 | web | attd/attd07 | 리팩터링 | [backend] OT 승인/등록/수정 java OT_TYPE 제거 + ATTD_400_095 삭제 | - |
| PRAFTA-043-002 | web | attd/attd07 | 리팩터링 | [backend] Attd07Mapper.xml OT_TYPE 컬럼 제거 | 043-001 |
| PRAFTA-043-003 | web | attd/reqinbox,attd08 | 리팩터링 | [backend] 접수함/근태목록 otType 조회 제거 | - |
| PRAFTA-043-004 | app | app/req/req07,req06 | 리팩터링 | [backend] 앱 신청 INSERT/내요청 otType 잔여 제거 | - |
| PRAFTA-043-005 | web | attd | 보완 | [frontend-screen] Attd_08 "초과근무" 격하 + Attd_10 payload otType 제거 | 043-001,003 |
| PRAFTA-043-006 | web | attd/popup | 리팩터링 | [frontend-component] AttdDayDetailPop OT 유형 매핑/payload 제거(템플릿 무변경) | 043-001,002 |
| PRAFTA-043-007 | app | req | 버그수정 | [frontend-component] OvertimeForm 거짓 안내문구 정정 | - |
| PRAFTA-043-008 | web/app | db/migration | 신규 | [backend] OT_TYPE 파기 마이그 expand/contract 2파일(EXPAND=NULL허용 선적용, CONTRACT=2컬럼 DROP; 운영 미적용, 롤백 동반) | EXPAND:- / CONTRACT:043-001~004 |
| PRAFTA-043-009 | 공통 | policies/attd | 보완 | [문서] §9 유형 비저장 명확화 + CHANGELOG | - |

상세 설명 본문은 본 문서 §1 각 작업 항목 그대로 사용.
