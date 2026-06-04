# prafta-043 — 초과근무 유형(OT_TYPE) 전면 파기 (웹+백엔드+DB+앱 정합)

작업 영역: 웹/백엔드(`PRAFTA/prafta-backend`, `PRAFTA/prafta-web-frontend`) 중심 + 앱(`PRAFTA/prafta-app-frontend`) 잔여 정합 + DB 마이그레이션

> ⚠️ 본 요청서는 사용자가 "이번(prafta-app-016) 이후 별도 요청"으로 진행하기로 한 **예약 작업지시서**다. prafta-app-016(앱 초과근무 폼에서 유형 입력만 제거, 신청 행 OT_TYPE=NULL 저장)이 선행 완료된 상태를 전제로 한다. 착수 시 planner가 본 문서를 정독해 분해한다.

## 배경 / 결정 근거

사용자 결정: 초과근무 "유형"(연장/야간/휴일) 값은 실질적 효용이 없으므로 시스템에서 **전면 파기**한다. (예: 13~15시 초과를 '야간'으로 지정해도 그 구분을 의미 있게 활용/표시하는 흐름이 사실상 없다는 판단.)

단, 사전 조사(prafta-app-016 진행 중) 결과 OT_TYPE은 **현재 웹 관리자 화면에 실제로 표시·입력되고 있다.** 따라서 "파기"는 단순 컬럼 삭제가 아니라 아래 표시/입력 기능 제거 + DB 마이그레이션 + 전 계층 정합을 포함한다. planner는 각 사용처가 "표시", "입력", "단순 전달", "별개 도메인" 중 무엇인지 분류해 분해한다.

## 확인된 OT_TYPE 사용처 (조사 스냅샷 — planner가 재확인)

### 1) 파기 대상 (초과근무 유형)
- DB
  - `tb_user_attd_req.OT_TYPE` (nullable) — 앱/웹 초과근무 **신청 행**. prafta-app-016 이후 앱은 NULL 저장. → 컬럼 제거 또는 무시.
  - `tb_user_overtime_mgmt.OT_TYPE` (**NOT NULL**) — **최종 정산** 행. 관리자가 승인 단계에서 입력. → 컬럼 제거 시 NOT NULL 제약·기존 데이터 마이그레이션 필요(컬럼 DROP 또는 nullable화 + 코드 제거).
- 백엔드(web attd07 등)
  - `web/attd/attd07`: `MonthlyOvertimeResult`, `DailyOvertimeResult`, `MonthlyAttdReqResult`, `OvertimeItemModel`, `InsertUserOvertimeCommand`, `UpdateUserOvertimeRequestParam/Request`, `OvertimeItemRequest`, `Attd07ServiceImpl`(승인 시 OT_TYPE 기록 L952~991 부근), `Attd07Mapper(.xml)`.
  - `web/attd/reqinbox`: `PendingReqResult`(+mapper).
  - `web/attd/attd08`: `AttdListsResult`(+`Attd08Mapper.xml`).
  - `app/req/req07`: `OvertimeRequest`/`SlotRequest`/`AttdReqInsertCommand`/`AppReq07Mapper.xml`(잔여 OT_TYPE 컬럼 매핑) — prafta-app-016에서 NULL 저장으로 일부 정리됨, 잔여 제거.
  - `app/req/req06`: `MyReqItemResult`(+`AppReq06Mapper.xml`) — 내 요청 목록에 otType 노출 여부 확인.
  - 에러코드 `ATTD_400_095`(OT_TYPE allow-list) — prafta-app-016에서 비활성(보존)됨 → 완전 제거 가능.
- 웹 프론트(`prafta-web-frontend`)
  - `attd/Attd_08.vue` — **근무구분 표시**: `otTypeLabel`/`workTypeLabel`이 "초과근무(야간/연장/휴일)" 노출(L808~826). → "초과근무"만 표기하도록 격하.
  - `attd/Attd_10.vue` — 승인 화면에서 otType 전달(L452~). → 유형 입력/표시 제거.
  - `attd/popup/AttdDayDetailPop.vue` — 관리자 OT 편집: `mapOtType`/`reverseOtType`/타입 기본값 '연장'(L1652~, L1796~, L2176~). → 유형 입력 UI·매핑 제거.
- 앱 프론트(`prafta-app-frontend`)
  - prafta-app-016에서 신청 폼 유형 칩 제거 완료. 잔여로 내 요청/상세에 otType 표시가 있으면 제거.

### 2) 파기 금지 — 별개 도메인 (혼동 주의)
- `web/baim/baim04`·`baim05`의 `OT_TYPE`(`DailyUserSlot*` 계열, `Baim04Mapper.xml`/`Baim05Mapper.xml`, `DailyUserSlotCommand` 등)은 **일용직 슬롯 관련 별개 컬럼/의미**로 보인다. 초과근무 유형과 무관 → planner가 별개임을 확인하고 **건드리지 않는다**.

## 요청 (사용자 의도)

1. 초과근무 "유형(연장/야간/휴일)" 개념을 시스템에서 제거한다. 사용자/관리자 모두 더 이상 유형을 입력하지 않는다.
2. 웹 관리자 화면의 "근무구분: 초과근무(유형)" 표시는 "초과근무"로 격하(유형 표기 제거).
3. 승인/편집 흐름에서 유형 입력·매핑을 제거한다.
4. DB의 OT_TYPE 컬럼(초과근무 관련 2개)을 안전하게 제거/무력화한다. 일용직 슬롯 OT_TYPE은 보존.

## 제약 / 검토 포인트 (planner)

- 마이그레이션: `tb_user_overtime_mgmt.OT_TYPE`이 NOT NULL이므로 DROP 또는 nullable화 + 기존 행 처리 방식 결정. 운영 적용 전 백업/롤백 SQL 동반. (CLAUDE.md DB 규칙: 마이그 파일 작성·운영 미적용 보고.)
- SYS 코드(OT_TYPE용 공통코드가 SYS에 등록돼 있으면) 폐기/보존 결정.
- 초과근무 정산/리포트(attd08/attd11/월마감)가 OT_TYPE에 의존해 집계/표시하는지 확인 — 의존 시 대체(유형 무관 단일 "초과근무"로) 정의.
- 웹/앱/백엔드 동시 정합: 한쪽만 제거 시 직렬화/계약 깨짐. 단일 라운드로 전 계층 정합.
- 정책서: 초과근무 유형 정의가 근태관리 정책서에 있으면 정정(파기 반영) + CHANGELOG.
- prafta-app-016과의 관계: 016은 앱 입력 제거 + NULL 저장까지. 본 작업이 그 위에서 컬럼·표시·승인입력까지 완전 제거.

## 처리 방식

CLAUDE.md 에이전트 워크플로우: planner → developer → qa → security. 메인 세션 Notion 대행. **사용자가 별도 착수 지시 시 진행**(현재는 예약 문서).
