# prafta-041 작업 분해 (planner)

Attd_05.vue 근무계획 저장 "변경된 셀만 저장(dirty)" 개선 + 셀 비우기(삭제) 신규.

선행 전제: 직전 핫픽스(조회/저장 스코프 = 조회월+조회부서 사용자)가 working tree 에 적용된 상태에서 그 위에 분해.

---

## 정책서 출처 (INDEX 경유 확인 결과)

- `attd/06-schedule.md` §6.4 스케줄관리 화면 기능 — 근무 타입/연차 타입 일괄 적용. 본 작업은 이 화면의 적용/저장 동작을 dirty 저장 + 셀 비우기로 확장.
- `attd/06-schedule.md` §6.3 미래 반영 시 덮어쓰기 옵션 — "미래 휴가 데이터는 모두 유지(고정)". 셀 비우기는 관리자 명시 행위이므로 이 보호와 구분되는 의도된 삭제다(자동 재생성이 아님).
- `attd/08-leave.md` §8.2 휴가 등록·신청 — "관리자는 스케줄관리 화면에서 휴가를 등록(연차 타입 적용 버튼)". 연차 셀의 등록 경로.
- `attd/08-leave.md` §8.3.1 일 단위 휴가 출근 차단 — 연차 셀(WORK_PLAN_CD=leaveCd)이 출근 차단 블록 역할. 셀 비우기 시 이 블록도 함께 제거된다.
- `attd/08-leave.md` §8.5.8 멱등성 + 기 부여 보호 — "시스템은 기 발생 연차를 사후 차감하지 않는다", "tb_user_leave_use 사용 이력은 어떤 옵션에서도 삭제하지 않는다". ⚠️ 연차 셀 비우기의 "차감 복원"은 이 규칙과의 정합 검토가 필요(아래 리스크 R1). 차감 복원은 부여(GRANT) 취소가 아니라 직접 사용기록(USE)의 취소이며, 기존 결재 반려 경로(`cancelLeaveUseByReqId` + `recomputeGrantUsedDays`)와 동형이다.
- `attd/13-attendance-close.md` (마감) — 마감월 차단 가드(이미 적용됨, isClosedForUser). 셀 비우기/삭제도 동일 가드를 타야 한다.

정책서에 "셀 비우기" 자체의 명시 규칙은 없다(화면 기능 확장). 결정 1~3(요청서 6절)이 단일 출처이며, 정책서와 충돌하는 부분은 R1 만 검토 대상.

---

## 현행 동작 파악 결과 (코드 근거)

### 프론트 `Attd_05.vue`
- `scheduleData` = `{ "${userCd}_${workYmd}": workPlanCd }` 단일 ref. baseline 스냅샷 없음.
- `fnApplySchType` / `fnApplyLeaveType` 가 선택영역 셀을 `scheduleData` 에 덮어쓰고 해당 user 를 `checkedRows` 에 자동 체크. (셀 비우기 수단 없음)
- `fnSave` (901~968): 체크된 사용자 + 조회월(ymPrefix) 셀 전체를 `saveList` 로 모아 그대로 전송. 변경 여부 비교 없음 → 미변경 셀도 재전송.
- `fnDelete` (971~1007): 체크된 사용자 전체를 **월 단위**(`workYm`) 삭제로 전송. 셀 단위 아님.
- payload 키 파싱: `workYmd = key.slice(-8)`, `userCd = key.substring(0, key.length - 9)` (구분자 `_` 1자 가정). baseline diff 시에도 동일 파싱 재사용 가능.

### 백엔드 `com.prafta.web.attd.attd05`
- `Attd05Controller`:
  - `POST /attd05/save-user-work-plans` `List<SchTypeRequst>{siteCd,userCd,workYmd,workPlanCd}` → 업서트(기존, dirty 업서트 그대로 재사용 가능).
  - `POST /attd05/delete-user-work-plans` `List<SchTypeDeleRequst>{siteCd,userCd,workYm}` → **월 단위 삭제**(셀 단위 아님).
- `Attd05ServiceImpl.saveUserWorkPlans`: 매니저 권한 가드 + 마감 가드 + 근무타입 effective-dating 검증 + **법정휴가 셀은 `leaveFlowService.recordDirectLeaveUsage` 로 차감**(멱등). 업서트.
- `Attd05ServiceImpl.deleteUserWorkPlans`: 매니저 권한 가드 + 마감 가드 + `deleteUserWorkPlans` 매퍼(`WORK_YMD LIKE workYm%`) **월 단위 DELETE**. ⚠️ **연차 차감 복원 없음** — 현재 월 단위 삭제는 연차 사용기록(`tb_user_leave_use`)을 건드리지 않는다.
- `Attd05Mapper.xml`: `saveUserWorkPlans`(ON DUP KEY UPDATE), `deleteUserWorkPlans`(WORK_YMD LIKE 월).

### leaveflow (차감/복원 존재 여부)
- `LeaveFlowService.recordDirectLeaveUsage(cmpnyCd, siteCd, userCd, workYmd, leaveCd, operatorUserCd)`:
  - 멱등: `countDirectLeaveUse`(REQ_ID NULL, LEAVE_STATUS='CONFIRMED', DEL_YN='N', 회사+사용자+연차코드+START_DATE) > 0 이면 SKIPPED_DUP.
  - `selectDeductibleGrant`(만료임박순, FOR UPDATE) → 없으면 INSUFFICIENT.
  - `insertLeaveUse`(REQ_ID NULL, CONFIRMED, UNIT '00', 1일) + `recomputeGrantUsedDays`(GRANT.USED_DAYS = SUM CONFIRMED).
- **역연산(직접 사용기록 취소/복원)은 존재하지 않는다.** 현존 취소는 `cancelLeaveUseByReqId`(REQ_ID 기준)뿐 — 직접 차감은 REQ_ID NULL 이라 매칭 안 됨.
  → **신규 셀 단위 취소 메서드가 leaveflow 에 필요**(아래 PRAFTA-041-2).

### 스키마 (확인된 것 / 미확인)
- `TB_USER_WORK_PLAN` PK = CMPNY_CD + SITE_CD + USER_CD + WORK_YMD (요청서 제공, 확정).
- `TB_USER_LEAVE_USE`: 직접 차감 식별 = REQ_ID IS NULL + LEAVE_STATUS='CONFIRMED' + DEL_YN='N' + (CMPNY_CD,USER_CD,LEAVE_CD,START_DATE). UK_LEAVE_USE_DIRECT 유니크 존재(코드 주석 근거). CANCEL_REASON/CANCEL_DATE/UPDATE_NO/UPDATE_DATE 컬럼 존재(cancelLeaveUseByReqId 근거).
- ⚠️ **미확인 — developer 가 DESCRIBE 로 확인 필요**: `TB_USER_LEAVE_USE` 의 START_DATE 컬럼명/타입, LEAVE_STATUS 허용값('CONFIRMED'/'CANCELLED' 철자 — 메모리상 CANCELLED 철자 이슈 존재), GRANT_ID 컬럼명. `TB_USER_LEAVE_GRANT.USED_DAYS` 재계산 대상.

---

## 작업 분해 (의존 순서)

전체 영역: web. 모듈: attd/attd05 (+ attd/leaveflow). 법적 책임 영역(attd) → 우선순위 격상. 연차 차감/복원은 PII 아님이나 데이터 정합성 영향 큼.

### PRAFTA-041-1 — [backend] leaveflow 직접 연차 사용기록 셀 단위 취소/복원
- 유형: backend / 영역: web / 모듈: attd/leaveflow / 작업유형: 신규
- 요구사항 요약: 근무계획 화면에서 연차 셀을 비울 때, 결재 없이 기록된 직접 연차 사용(`recordDirectLeaveUsage` 결과)을 셀 단위로 취소하고 부여 잔여를 복원한다.
- 상세:
  - 핵심 요구사항:
    1) `LeaveFlowService` 에 `cancelDirectLeaveUsage(cmpnyCd, userCd, workYmd, leaveCd, operatorUserCd)` 신설. `recordDirectLeaveUsage` 의 역연산.
    2) 대상 = `tb_user_leave_use` 중 REQ_ID IS NULL + LEAVE_STATUS='CONFIRMED'(현행 철자 확인) + DEL_YN='N' + (cmpnyCd,userCd,leaveCd,START_DATE=workYmd) 인 직접 사용기록. 해당 행을 `LEAVE_STATUS='CANCELLED'`(또는 현행 철자) + CANCEL_REASON='근무계획 연차 비우기' + CANCEL_DATE/ UPDATE_* 갱신 (이력 보존 = soft cancel, §8.5.8 "사용 이력 삭제 금지" 준수).
    3) 취소된 사용기록의 GRANT_ID 를 찾아 `recomputeGrantUsedDays` 로 USED_DAYS 재동기화(잔여 복원).
    4) 멱등: 대상 0건이면 no-op 반환(이미 취소/미존재). 반환값 enum 추가 검토(`CANCELLED`/`NOT_FOUND`) 또는 void.
  - 영향 파일:
    - `prafta-backend/.../web/attd/leaveflow/service/LeaveFlowService.java` (메서드 시그니처 추가)
    - `prafta-backend/.../web/attd/leaveflow/service/impl/LeaveFlowServiceImpl.java` (구현)
    - `prafta-backend/.../web/attd/leaveflow/mapper/LeaveFlowMapper.java` (신규 매퍼 2종: cancelDirectLeaveUseByCell, selectGrantIdsByDirectLeaveCell)
    - `prafta-backend/.../resources/com/prafta/web/attd/leaveflow/mapper/LeaveFlowMapper.xml` (신규 SQL)
  - endpoint: 없음(내부 서비스). Attd05ServiceImpl 에서 호출.
- 정책 출처: `attd/08-leave.md` §8.5.8(이력 보존·soft cancel), §8.3.1(출근차단 블록), 기존 결재 반려 경로(`cancelLeaveUseByReqId`)와 동형.
- 선행 작업: 없음(독립 구현 가능, 단 PRAFTA-041-2 가 이를 호출).
- 우선순위 근거: 데이터 정합성(연차 잔여) 직접 영향 + 법적 책임 영역(attd) → 최우선.
- ⚠️ developer 확인: LEAVE_STATUS 철자(CONFIRMED/CANCELLED), START_DATE/GRANT_ID 컬럼명을 DESCRIBE 로 확정 후 SQL 작성. 동일 셀에 여러 사용기록(분할)이 있을 가능성은 일 단위 직접 차감 1건 가정(UK_LEAVE_USE_DIRECT)이나, 방어적으로 다건 취소 + 각 GRANT 재계산.

### PRAFTA-041-2 — [backend] 근무계획 셀 단위 삭제 경로 신설 + 연차 복원 연계
- 유형: backend / 영역: web / 모듈: attd/attd05 / 작업유형: 신규(기존 월단위 delete 와 병존)
- 요구사항 요약: 비워진 셀(사용자+근무일)을 셀 단위로 삭제하고, 그 셀이 법정연차였으면 PRAFTA-041-1 을 호출해 차감을 복원한다.
- 상세:
  - 핵심 요구사항:
    1) 신규 endpoint `POST /attd05/delete-user-work-plan-cells` (또는 기존 delete-user-work-plans 를 셀 단위로 확장 — **결정: 신규 경로 신설** 권장. 기존 월 단위 삭제(fnDelete 의 row 전체 삭제)는 그대로 둔다. 혼동/회귀 방지).
    2) 요청 DTO = `List<{siteCd, userCd, workYmd, workPlanCd?}>` (workYmd 단위). 셀 단위 식별을 위해 workYmd 필수.
    3) 서비스: 매니저 권한 가드 + 마감 가드(workYmd→월 추출, isClosedForUser) — 기존 패턴 동일.
    4) 셀이 법정휴가코드(`selectLegalLeaveCds` 집합)에 속하면 삭제 **전/후** PRAFTA-041-1 `cancelDirectLeaveUsage` 호출(차감 복원). 순서: 사용기록 취소 → 부여 복원 → WORK_PLAN row DELETE (또는 역순; 트랜잭션 내 원자성 보장). 비-연차 셀은 단순 DELETE.
    5) 셀 단위 DELETE 매퍼 신설: `DELETE ... WHERE CMPNY_CD AND SITE_CD AND USER_CD AND WORK_YMD = #{workYmd}` (PK 완전 일치).
    6) `@Transactional` 로 묶어 부분 실패 시 롤백.
  - 영향 파일:
    - `prafta-backend/.../web/attd/attd05/controller/Attd05Controller.java` (endpoint 추가)
    - `prafta-backend/.../web/attd/attd05/service/Attd05Service.java` / `.../service/impl/Attd05ServiceImpl.java`
    - `prafta-backend/.../web/attd/attd05/dto/request/` 신규 셀삭제 Request + application/param·model·command (기존 SchTypeDele* 가 workYm 기준이라 재사용 불가 → 신규 `WorkPlanCellDele*` 권장)
    - `prafta-backend/.../web/attd/attd05/mapper/Attd05Mapper.java` + `.../resources/.../attd05/mapper/Attd05Mapper.xml` (셀 단위 delete 추가)
  - endpoint: POST /webApi/attd05/delete-user-work-plan-cells (신규)
  - 예상 산출물: controller(+1 method), service, dto/param/model/command, mapper.xml(+1 delete)
- 정책 출처: `attd/06-schedule.md` §6.4(화면 기능), §8.5.8(연차 사용기록 soft cancel), `attd/13-attendance-close.md`(마감 가드), 결정 1.
- 선행 작업: PRAFTA-041-1 (cancelDirectLeaveUsage 필요).
- 우선순위 근거: 셀 비우기 핵심 백엔드 경로 + 정합성. API 없이는 프론트 저장 불가 → 프론트보다 선행.
- ⚠️ developer 확인: 기존 `delete-user-work-plans`(월 단위) 호출처가 fnDelete 뿐인지 확인(영향 격리). 신규 경로 추가가 기존 동작에 영향 없게.

### PRAFTA-041-3 — [frontend-screen] Attd_05.vue dirty 저장 + 셀 비우기 UX
- 유형: frontend-screen / 영역: web / 모듈: attd/attd05 / 작업유형: 보완
- 요구사항 요약: 조회 시 baseline 스냅샷 보관, 저장 시 변경 셀만 업서트/삭제로 분리 전송, 툴바 "지우기" 버튼 + 드래그 선택영역 비우기, 변경 없음 안내, 저장 후 baseline 갱신.
- 상세:
  - 핵심 요구사항:
    1) baseline 스냅샷: `fnSearch` 응답 적재 직후 `scheduleData` 의 깊은 복사본 `scheduleBaseline`(plain object) 보관. (template/style 골격은 planner, diff 로직 body 는 developer)
    2) "지우기" 버튼(툴바): 드래그 선택영역의 셀을 `scheduleData[key]` 에서 빈값/삭제 처리. 해당 row 자동 체크(기존 apply 패턴 동일). 마감월이면 차단. (handler stub + TODO)
    3) dirty 판정(developer 구현, stub): baseline 대비 (a) 신규/값변경 → 업서트 대상, (b) baseline 에 값 있었고 현재 빈값 → 삭제 대상, (c) 동일값 → 제외. 모두 (체크된 row) AND (조회월) 교집합.
    4) `fnSave` 분기: 업서트 리스트는 기존 `POST /attd05/save-user-work-plans`, 삭제 리스트는 신규 `POST /attd05/delete-user-work-plan-cells`(PRAFTA-041-2). 둘 다 비면 "변경된 내용이 없습니다" 안내 후 API 미호출(MSG 키 확인/추가).
    5) 호출 순서: 삭제 → 업서트(또는 병렬 후 합산). 성공 시 `fnSearch` 재조회로 baseline 자연 갱신.
    6) 셀 비우기 표시: getCellNmValue 가 빈값이면 기존처럼 "-"/공백 렌더(기존 로직 유지).
  - 영향 파일:
    - `prafta-web-frontend/prafta-web-frontend/src/views/attd/Attd_05.vue` (template: 툴바 "지우기" 버튼 추가 / script: baseline·diff·fnClearCells·fnSave 분기 — script body 는 developer / style: 버튼 스타일은 기존 토큰 재사용)
  - endpoint(프론트 호출): GET /webApi/attd05/user-work-plans(기존), POST /webApi/attd05/save-user-work-plans(기존), POST /webApi/attd05/delete-user-work-plan-cells(PRAFTA-041-2)
  - 연결 UI 명세: UI(아래 prafta-041-ui-spec.md)
- 정책 출처: `attd/06-schedule.md` §6.4, 결정 1·2·3.
- 선행 작업: PRAFTA-041-2 (삭제 endpoint).
- 우선순위 근거: 화면. 백엔드 두 작업 이후.
- ⚠️ planner→developer 경계: planner 는 "지우기" 버튼 template/style + 반응형 변수 선언(`scheduleBaseline`) + handler stub(TODO) 만. baseline diff·payload 분리·API 호출은 developer.

---

## PRAFTA-041-4 [backend] attd05 인가 canManageNode 확장 (보안 후속)
- 배경: 보안이슈 prafta-041-001 — attd05 근무계획 쓰기 3경로(saveUserWorkPlans / deleteUserWorkPlans / deleteUserWorkPlanCells)가 `if(!isManager) 403` 로 master/hr 만 허용. 사용자 결정으로 노드(MAIN/SUB) 어드민도 "자기 소속(및 하위) 직원"의 근무계획을 관리하도록 확장(Attd07/Attd11 과 동일 canManageNode 스코프).
- 구현:
  - `AttdCloseService.canManageUser(authCd, requesterUserCd, cmpnyCd, siteCd, targetUserCd)` 신규(편의 메서드). master/hr 즉시 true(노드 조회 0회), 그 외 `selectUserNodeCd` 로 대상 부서 해석 후 `canManageNode` 위임. 클라이언트 nodeCd 불신뢰.
  - `Attd05ServiceImpl` 3개 메서드의 상단 isManager 가드 제거 → 대상 사용자별 `canManageUser` 검증으로 교체. distinct userCd 캐시(Map)로 셀 반복 중복조회 방지. 권한 없는 대상 1건이라도 있으면 ATTD_403_002 throw(트랜잭션 전체 롤백, 부분 스킵 아님). 마감/검증/연차 로직 불변.
  - master/hr 은 canManageUser 즉시 true → 기존과 100% 동일 동작(노드 조회 0회).
- 수정 파일: `attd07/service/AttdCloseService.java`(+Impl), `attd05/service/impl/Attd05ServiceImpl.java`.
- 신규 SQL 없음(기존 selectUserNodeCd/countNodeAdmin 재사용).
- 정책 출처: 사용자 결정(노드 어드민 스코프 확장) + Attd07/Attd11 canManageNode 패턴 일관.
- 검증: gradlew compileJava --no-daemon BUILD SUCCESSFUL.

---

## 의존 순서 요약
PRAFTA-041-1 (leaveflow 셀단위 취소) → PRAFTA-041-2 (attd05 셀삭제 endpoint, 1 호출) → PRAFTA-041-3 (프론트, 2 호출). PRAFTA-041-4 (인가 스코프 확장, 보안 후속) 는 1~3 완료 후 독립.

## 미확인 / 리스크
- **R1 (정합성·정책)**: §8.5.8 은 "기 발생 연차 사후 차감 금지 / 사용 이력 삭제 금지"를 규정. 본 작업의 "차감 복원"은 **부여(GRANT) 취소가 아니라 직접 사용기록(USE)의 soft cancel + USED_DAYS 재계산**이므로 §8.5.8 위반 아님(부여는 그대로, 사용 이력은 삭제가 아닌 LEAVE_STATUS='CANCELLED' soft cancel 로 보존). 기존 결재 반려(`cancelLeaveUseByReqId`)와 동일 모델. → developer/security/qa 가 이 동형성을 재확인. 만약 사내 운영상 "관리자 임의 비우기로 사용 이력 취소"를 막아야 한다면 결정 추가 필요(현 요청서 결정 1 은 "취소/복원 필수"로 명시 → 진행).
- **R2 (스키마 미확정)**: TB_USER_LEAVE_USE 의 LEAVE_STATUS 철자(메모리: CANCELLED 미등록/철자 이슈), START_DATE·GRANT_ID 컬럼명. developer 가 DESCRIBE/SHOW CREATE 로 확정 후 SQL 작성. 추측 금지.
- **R3 (멱등 경계)**: 동일 셀에 직접 사용기록이 1건이라는 UK 가정. 방어적으로 다건 취소 + GRANT 별 재계산.
- **R4 (호출처 격리)**: 기존 월 단위 `delete-user-work-plans` 는 fnDelete(row 전체 삭제) 전용으로 유지. 셀 비우기는 신규 경로. fnDelete 동작 변경 없음(요청서 범위 밖).
- **R5 (no-op 동시성)**: dirty 비교는 클라이언트 baseline 기준. 다른 관리자가 같은 셀을 동시 수정한 경우 last-write 가능 — §6.5 경합방지는 "근로자 수정요청 대기 중 관리자 직접수정 차단"만 규정(관리자간 동시성은 범위 밖). 현행 유지.
- **MSG**: "변경된 내용이 없습니다" 메시지 키(`MSG.NO_CHANGE` 등) 존재 여부 developer 확인, 없으면 기존 `MSG.SAVE_DATA_REQUIRED` 재사용 또는 신규.

## Notion 기록 필요 (메인 세션 대행)
- 작업 로그 3행: PRAFTA-041-1(backend), PRAFTA-041-2(backend), PRAFTA-041-3(frontend-screen). 상태 분해완료, 담당 planner, 선행관계 1→2→3.
- 도메인 지식 베이스 1행: UI 명세(prafta-041-ui-spec.md) — Attd_05 근무계획관리 보완(셀 비우기 + dirty 저장).
- planner 는 Notion 미접근 — 위 등록은 메인 세션이 대행.
