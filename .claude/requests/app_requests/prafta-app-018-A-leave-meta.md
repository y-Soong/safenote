# prafta-app-018-A — [앱 BE] 연차 신청 폼 메타 조회 API

상위: `prafta-app-018-leave-apply-plan.md`. 본 단위는 **읽기 전용** — 연차 신청 폼(018-C)이 필요로 하는 메타를 내려준다. 쓰기(018-B) 선행.

## 목표
신규 앱 모듈(예: `com.prafta.app.leave.leaveflow` 또는 `leave02`)에 폼 메타 조회 엔드포인트 추가. 식별값은 JWT(TokenInfo gv_*)만(IDOR). 신규 테이블 없음(기존 조회만).

## 엔드포인트 (제안 — planner가 경로/모듈명 확정)
1. `GET /appApi/leaveflow/apply-meta`
   - 응답: 신청 가능 연차종류 목록. 각 항목:
     - `leaveCd`, `leaveNm`, `systemYn`(법정여부), `aprvRequired`(결재필요: 법정=정책 APRV_USE_YN, 비법정=타입 APRV_USE_YN),
     - `allowedUnits`: 해당 종류로 신청 가능한 사용단위 코드 목록(SYS025). **D2 게이팅 산출**:
       - 법정(systemYn='Y'): `TB_LEAVE_USAGE_POLICY.USAGE_UNIT`(회사 단일값)을 SYS025 코드로 매핑(FULL_DAY=00/HALF_DAY=01/HOUR_2=02/HOUR_1=03/MIN_30=04).
       - 비법정: `TB_LEAVE_TYPE_MGMT.USE_UNIT_TYPE`(타입 단일값, SYS025).
       - **D2-a 의미(상위 개요 확인 결정)**: (Y)계층형이면 설정 granularity 이하(=설정코드+더 큰 단위) 전부, (X)정확이면 설정 코드 1개만. 잠정=(Y). 확정값으로 산출 구현.
     - `balanceDays`: 해당 종류 현재 잔여(부여-사용 합). leave01/웹 산출과 정합(부여 SUM(GRANT)-SUM(USED_DAYS); leave01 메모리 §2.3 이중차감 주의).
   - 신청 불가 종류(잔여 0/유효기간 외 등)는 목록 제외 또는 disabled 플래그 — planner 결정.
2. `GET /appApi/leaveflow/approval-presets` (D1 결재선)
   - 본인 소유 결재선 프리셋 목록(`TB_APRV_LINE_PRESET` + 상세, prafta-020). 웹 `/webApi/user04/presets` 미러. 기본 프리셋 플래그 포함.
3. `GET /appApi/leaveflow/approver-search?keyword=` (D1 결재선)
   - 결재자 후보 사용자 검색(같은 회사 범위, PII 마스킹 정책 준수 — 이름 일부/직책 등 최소노출). 웹 결재자 검색 미러. 페이징/LIMIT 필수.

## 수용 기준
- 모든 식별값 JWT 출처. 사업장/회사 스코프 WHERE 명시. `SELECT *` 금지·leading 콤마·`#{}` 바인딩.
- allowedUnits 가 D2 규칙대로 종류별로 다르게 내려온다(법정 vs 비법정 출처 구분, 단일값→코드목록 변환). USE_YN='Y' SYS025만.
- 잔여(balanceDays)가 leave01/웹과 동일 공식(이중차감 없음).
- 결재자 검색에 PII 평문 과다노출 없음(보안), LIMIT 부착.
- 신규 record/DTO는 MyBatis 위치매핑 함정 주의(SELECT 컬럼순서=생성자 인자순서).
- 빌드: `gradlew compileJava compileTestJava --no-daemon` 통과.

## 정책 출처
attd §8(연차), §8.5(연차부여정책/사용단위), §9(결재). prafta-024(USAGE_UNIT), prafta-019(시간차/결재라인), prafta-020(프리셋).
