# prafta-com-003 — 디바이스 식별 기반 부정 출퇴근(대리 출퇴근) 탐지

작업 영역(교차): Flutter 셸(`PRAFTA_FLUTTER/safenote`) + 앱 프론트(`PRAFTA/prafta-app-frontend`) + 백엔드(`PRAFTA/prafta-backend`) + 웹 관리자(`PRAFTA/prafta-web-frontend`)

> 본 문서는 사용자(YJ)와의 대화로 확정한 설계 작업지시서다. planner가 이를 정독해 영역별 단위로 분해한다.

## 0. 목표 / 배경

직원이 **타인 계정으로 로그인해 대리 출퇴근**하는 부정을 관리자가 탐지할 수 있게 한다. 핵심 신호: **하나의 물리적 기기에서 여러 계정이 출퇴근을 찍는 것**.

현재 상태(조사 결과):
- 앱은 이미 `gv_deviceId`(webview localStorage의 클라 생성 UUID)를 매 요청에 전송하고, 백엔드는 이를 로그인/리프레시/로그아웃의 디바이스 바인딩에 사용 중.
- 그러나 `tb_user_device`에 **저장하지 않음(0행)**, 로그인 이력도 없음, 출퇴근과 연결도 없음.
- Flutter에 FCM/네이티브 디바이스ID 코드 없음. `tb_user_device.PUSH_TOKEN`도 채워진 적 없음.

→ 본 작업은 (1) **신뢰도 높은 네이티브 디바이스ID 앵커링**, (2) **디바이스/로그인 이력 적재**, (3) **출퇴근 레코드에 디바이스 도장**, (4) **관리자 대조·탐지 화면**을 구현한다.

## 1. 확정 결정 (대화로 합의 — 고정)

- **D1 디바이스ID 소스 = Flutter 네이티브** (현 webview UUID 대체/보강).
  - Android: `ANDROID_ID`(`Settings.Secure.ANDROID_ID`) — 재설치·앱데이터삭제에도 유지, 공장초기화 시만 변경. (`android_id` 패키지 또는 동등.)
  - iOS: `identifierForVendor`(IDFV, `device_info_plus`) — 동일 vendor 앱 전체 삭제 시만 초기화.
  - **안드로이드·iOS 동시 구현**(서비스 비중 반반). `DEVICE_TYPE`('ANDROID'/'IOS')로 구분 저장.
  - 부가 메타: 모델/OS버전(`device_info_plus`), 앱버전(`package_info_plus`).
  - Flutter 네이티브에서 획득 → **기존 JS 브리지로 webview에 주입** → `gv_deviceId`를 네이티브값으로 설정. 네이티브 획득 실패 시 기존 localStorage UUID로 graceful 폴백.
  - ⚠️ 한계 수용: 루팅/탈옥 기기는 위조 가능(어떤 방법으로도 불가피). ANDROID_ID/IDFV 수준이면 "앱데이터 삭제로 회피"는 차단되어 내부 근태앱 탐지 앵커로 충분 — v1은 이 한계를 안고 간다.

- **D2 데이터 모델 = 현재상태 1 + 이력 1 (2테이블)**
  - `tb_user_device`(기존, PK DEVICE_UUID): 기기 1행 = 최근 로그인 상태(USER_CD/메타/LAST_LOGIN_*). com-002에서 DEL_YN 추가됨.
  - **신규 로그인 이력 테이블(append-only)**: (DEVICE_UUID, USER_CD, DEVICE_TYPE, 로그인시각, IP, 메타...). **"이 기기가 지금껏 어떤 계정들에 쓰였는지"의 증빙 소스** = 부정탐지 분석 기준 테이블. (PK는 DEVICE_UUID라 덮어쓰는 현재상태로는 다계정 신호가 사라지므로 이력 별도 필수.)

- **D3 출퇴근 레코드에 디바이스 도장**
  - `tb_user_attd_mgmt`에 `CHECK_IN_DEVICE_UUID`(+ `CHECK_OUT_DEVICE_UUID`) 컬럼 신규 추가(마이그).
  - 출퇴근 시점에 요청의 `gv_deviceId`를 그대로 기록 → "이 출근/퇴근 클릭이 어느 기기에서 실행됐나"를 **직접증거(정황 아님)** 로 남김.
  - 근거: 로그인 이력만으로 출결-기기 귀속은 "시간순 join 추정"이라 세션 지속/토큰 리프레시(재로그인 없이 다음날 출근)·다기기·동시로그인에서 오귀속/누락 발생. deviceId가 이미 출퇴근 요청에 실려 오므로 도장 비용 ≈ 컬럼 1~2개 + 1회 write(앱 추가작업 0).

- **D4 탐지 = 표시 전용(차단 아님)**. 관리자에게 의심 케이스를 보여주기만. 출근 시점 하드블록/경고 없음.

- **D5 공용기기 없음(확정)** → "한 기기 → 여러 계정 출퇴근"은 항상 의심으로 취급(키오스크 예외 불요).

- **D6 관리자 화면 = 전용 모니터링 화면(웹 신규) + 즉석 대조 쿼리(표시 전용)** *(제안 기본값 — 사용자 조정 가능)*
  - 전용 "부정 출퇴근 의심 모니터링" 화면(web)에 의심 케이스 목록.
  - 탐지는 관리자가 화면 열 때 **on-view 대조 쿼리**(사전 플래그 적재/추가 write 없음, D4 표시전용과 정합). v1.
  - 대표 규칙:
    1. (결정타) **한 기기(CHECK_IN_DEVICE_UUID) → 같은 날 2명 이상 서로 다른 계정이 출퇴근** = 강한 의심.
    2. (보조) 어떤 직원의 출근 기기가 **본인 평소/소유 기기(로그인 이력 baseline)와 다른** 경우.
    3. (보조) 한 번도 본 적 없는 새 기기에서 출근.
  - 표시: 의심 케이스 행(날짜·기기·관련 계정들·출퇴근 시각·노드/사업장). 권한은 근태 관리자(master/hr/노드관리자) 범위 — planner가 기존 근태 인가 패턴 확인.

## 2. 범위 / 비범위

포함: 네이티브 디바이스ID 획득(안드+iOS)·브리지·로그인 시 디바이스/이력 적재·출퇴근 도장(마이그)·관리자 모니터링 화면.

비범위(이번 작업 아님):
- FCM PUSH 토큰 등록(A-3) — 별개. 단 본 작업의 `tb_user_device` 적재 흐름이 향후 PUSH_TOKEN 채움과 같은 자리라 시너지(메모만, 구현 X).
- 출근 시점 차단/경고(D4 표시전용).
- 루팅/위조 방지 고급 어테스테이션(SafetyNet/Play Integrity 등).
- 개인정보 보관기간/동의 정책 — 운영/법무 영역(코드 결정 아님, 보고만). 로그인 IP·기기ID는 내부 부정탐지 정당이익 전제, 보관기간은 별도 정책.

## 3. 스키마 (planner가 MCP로 재확인)

- `tb_user_device`(기존): DEVICE_UUID(PK), USER_CD, DEVICE_TYPE, DEVICE_MODEL, OS_VERSION, APP_VERSION, PUSH_TOKEN(nullable), LAST_LOGIN_DTIME, LAST_LOGIN_IP, DEL_YN(com-002 추가), INSERT/UPDATE_*. → 로그인 시 upsert.
- **신규 로그인 이력 테이블**: 컬럼/명명 planner 설계(예: DEVICE_LOGIN_NO PK, DEVICE_UUID, USER_CD, DEVICE_TYPE, LOGIN_DTIME, LOGIN_IP, 메타, INSERT_*). 코드성 컬럼 COMMENT 규칙 준수.
- `tb_user_attd_mgmt`(기존): 출퇴근 레코드. → `CHECK_IN_DEVICE_UUID`(varchar) + `CHECK_OUT_DEVICE_UUID` 컬럼 추가(마이그). NULL 허용(기존 행/웹 등록 분 호환).
- 마이그는 파일 작성·**운영 미적용**(수동), 롤백 SQL 동반.

## 4. 영역별 작업 (planner 분해 가이드)

1. **Flutter**(`PRAFTA_FLUTTER/safenote`): 네이티브 디바이스ID(Android ANDROID_ID / iOS IDFV) + 메타 획득, JS 브리지로 webview 노출. 비즈니스 로직 금지(값 전달만). 의존성(`device_info_plus`/`android_id`/`package_info_plus`) 추가. iOS/Android 양 플랫폼.
2. **앱 프론트**(`prafta-app-frontend`): 브리지에서 네이티브 디바이스ID/메타 수신 → `gv_deviceId`를 네이티브값으로 설정(폴백 유지, `axios.js getDeviceId` 정렬) → 로그인 요청에 디바이스 메타 동봉. (출퇴근은 이미 gv_deviceId 전송 중 — 확인.)
3. **백엔드**(`prafta-backend`):
   - 마이그: 신규 로그인 이력 테이블 + tb_user_attd_mgmt 디바이스 컬럼.
   - 로그인 훅(`common.cmm.login`): 기존 로그인 성공 흐름에 tb_user_device upsert + 로그인 이력 INSERT 추가(예외 격리 — 부정탐지 적재 실패가 로그인 자체를 막지 않게).
   - 출퇴근(`app.attd.attd01` check-in/out): 요청의 gv_deviceId를 CHECK_IN/OUT_DEVICE_UUID에 기록. ⚠️ **gv_deviceId가 출퇴근 엔드포인트에 도달하는 경로 확인 필수**(JWT 클레임인지, CheckInRequest 바디에 추가 필요한지) — planner/developer 검증.
   - 관리자 탐지 API: 위 규칙 on-view 대조 쿼리(근태 관리자 인가·노드/사업장 스코프·cross-site IDOR 가드).
4. **웹 관리자**(`prafta-web-frontend`): 부정 출퇴근 의심 모니터링 화면(목록/필터/상세). planner UI 명세 + Vue 골격.

## 5. 보안/운영 (security 검토 대상)

- gv_deviceId는 클라 제공 값 — 백엔드는 신뢰 경계 인지(위조 가능, 탐지 보조). 식별값(userCd 등)은 기존대로 JWT에서만.
- 관리자 탐지 API 인가: 근태 관리자 역할 + 노드/사업장 스코프(cross-site IDOR 가드). 본인 외 직원 출퇴근/기기 조회는 관리 권한 필요.
- 로그인 IP·기기ID 로그/응답 PII 취급 주의(평문 노출 최소화).
- 로그인 훅의 적재 실패는 로그인 흐름을 막지 않게(예외 격리, com-001 체크인훅 패턴 참고).

## 6. 처리 워크플로우

CLAUDE.md 에이전트 워크플로우: planner → developer → qa → security. 영역이 4개라 planner가 app/flutter·web/backend 단위로 분해(필요 시 app_requests/web_requests 산출물 분리). 메인 세션이 Notion 작업 로그 대행.

## 7. 관련 메모리/선행
- [[project_prafta_com_002_fcm_push_worker]] — tb_user_device·DEL_YN·PUSH_TOKEN(같은 테이블, 시너지).
- [[project_prafta_app_vite_and_api_align]] — gv_deviceId/세션 정렬.
- [[project_prafta_app_003_attd_gps]] — 출퇴근 GPS/지오펜스(디바이스+위치 신호 결합 여지).
- 출퇴근 로직: app.attd.attd01(check-in/out). 로그인: common.cmm.login.
