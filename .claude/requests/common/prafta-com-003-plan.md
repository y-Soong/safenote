# prafta-com-003 — 디바이스 식별 기반 부정 출퇴근 탐지 (planner 분해 결과)

> 작업지시서(단일출처): `.claude/requests/common/prafta-com-003.md` (확정결정 D1~D6 고정)
> 본 문서는 planner 가 4영역(Flutter / 앱FE / 백엔드 / 웹관리자) 교차 작업을 개발 단위로 분해한 결과다.
> 자율 진행 원칙에 따라 모호점은 합리적 기본값으로 채우되 §10 "채팅 확인 필요"에 보류 항목을 분리 보고한다.

---

## 0. 스키마/코드 확인 결과 (분해 정확도 직결)

### 0-1. ★결론: gv_deviceId 의 출퇴근 도달 경로 — **JWT 클레임 아님, 요청 바디 추가 필요**

검증 결과(파일 직접 정독):

| 확인 대상 | 사실 |
|---|---|
| `axios.js` (앱FE) | 모든 요청에 `gv_deviceId: getDeviceId()` 를 머지. GET=params, POST(JSON)=body 에 주입. → check-in/out 요청 body 에 `gv_deviceId` 가 **이미 실려서 도착**한다. |
| `TokenInfo.java` | record 에 `gv_deviceId` 필드가 있고 `claims.get("gv_deviceId")` 로 읽는다. |
| `JwtUtil.generateToken(UserResult)` | **`gv_deviceId` 클레임을 넣지 않는다**(cmpnyCd~nodeNm 만). 따라서 TokenInfo.gv_deviceId() 는 **항상 null**. |
| `CheckInRequest` / `CheckOutRequest` | `gv_deviceId` 필드가 **없다** → Jackson 이 body 의 gv_deviceId 를 **무시(흡수)** 한다. |
| `AppAttd01Controller.checkIn/checkOut` | 모든 식별값을 `TokenInfo`(JWT)에서만 추출. body 의 deviceId 는 닿지 않음. |

**→ D3 구현 방식 확정:**
- JWT 클레임 경로는 **불가**(generateToken 이 deviceId 를 모름 — 로그인 시점에 클라 deviceId 를 서버가 알 길은 별도 전달뿐이고, JWT 에 넣어도 토큰 수명 동안 고정돼 "출퇴근 시점 기기"를 못 잡음).
- **요청 바디에 `deviceId` 필드 추가**가 정답: `CheckInRequest`/`CheckOutRequest` 에 `private String deviceId;` (또는 `gvDeviceId`) 추가 → `CheckInParam`/`CheckOutParam.from()` 에서 정규화(trim/길이컷 100, 빈값 null) → Command → INSERT/UPDATE 시 `CHECK_IN_DEVICE_UUID` / `CHECK_OUT_DEVICE_UUID` 도장.
- 신뢰경계: deviceId 는 클라 제공값(위조 가능) — 식별/인가에 쓰지 않고 **표시·탐지 보조**로만(작업지시서 §5). userCd/siteCd 등은 기존대로 JWT 출처 유지.
- 앱 추가작업 0(axios 가 이미 전송 중) — body 키명만 맞추면 됨. **axios 가 보내는 키는 `gv_deviceId`** 이므로 DTO 필드는 `@JsonProperty("gv_deviceId")` 로 바인딩하거나 필드명을 `gv_deviceId` 로 둔다(developer 결정, 바인딩만 일치하면 됨).

### 0-2. 로그인 흐름 — 훅 위치 + 디바이스 메타 부재

| 확인 대상 | 사실 |
|---|---|
| `LoginController.Login` | `@RequestBody LoginRequest` + `X-Client-Type` 헤더. |
| `LoginRequest` | `userId/userNm/userPw/systValDCd` 만. **디바이스 메타(deviceId/model/os/appVersion/deviceType) 없음** → 추가 필요. |
| `LoginParam` | `userId/userPw/clientType` 만. |
| `LoginServiceImpl.Login` | 성공 경로 끝에 `loginMapper.updateUserLastLoginDtime(userCd)` 호출(L142). **이 직후가 디바이스 upsert + 로그인이력 INSERT 훅 자리**. |
| 예외 격리 선례 | com-001 체크인 훅(`AppAttd01ServiceImpl` L1058-1061): `try { detectAndAlert(...) } catch(Exception e){ log.error }`. **동일 패턴으로 디바이스 적재 try-catch 격리**(적재 실패가 로그인 막지 않게). |
| deviceId 도착 여부 | axios 가 로그인 요청 body 에도 `gv_deviceId` 를 머지 → body 로 도착. **단 model/os/appVersion/deviceType 은 현재 전송 안 함** → 앱FE 가 로그인 요청에 동봉하도록 추가 필요. |
| ★주의 | `verifyPhoneAuth`(인증대기 04→01 활성화) 도 정식 토큰 발급 + `updateUserLastLoginDtime` 호출(L309-310). 일관성 위해 **이 경로에도 동일 훅** 적용 권장(단 verify 요청 body 에 디바이스 메타가 오는지 확인 필요 → 안 오면 deviceId 만 적재하거나 이 경로는 v1 제외). → §10 확인항목. |

### 0-3. 스키마 (schema-full.sql 스냅샷 기준 + 작업지시서 보정)

**`tb_user_device`** (PK DEVICE_UUID) — 스냅샷 L1064-1080:
- 컬럼: DEVICE_UUID, USER_CD, DEVICE_TYPE('IOS'/'ANDROID'), DEVICE_MODEL, OS_VERSION, APP_VERSION, PUSH_TOKEN(null), LAST_LOGIN_DTIME, LAST_LOGIN_IP, INSERT_NO/DATE, UPDATE_NO/DATE.
- ⚠️ **스냅샷에 `DEL_YN` 없음**. 작업지시서는 com-002 에서 DEL_YN 추가됐다고 명시 → 스냅샷이 낡음. **developer 는 MCP(prafta-mysql)로 `SHOW CREATE TABLE TB_USER_DEVICE` 실제 확인 필수**(DEL_YN 존재 가정으로 upsert 작성, 미존재 시 보고).
- 로그인 시 **upsert**: DEVICE_UUID 기준 ON DUPLICATE KEY UPDATE (USER_CD/메타/LAST_LOGIN_DTIME/LAST_LOGIN_IP 갱신, INSERT 시 신규행). com-002 의 DEL_YN 은 upsert 시 'N' 로 되살림(재로그인 = 활성).

**`tb_user_attd_mgmt`** (PK ATTD_ID) — 스냅샷 L1001-1024:
- 컬럼 추가 위치: CHECK_IN_METHOD 뒤(출근군) / CHECK_OUT_METHOD 뒤(퇴근군)에 각각 신규 컬럼.
- **신규**: `CHECK_IN_DEVICE_UUID varchar(100) DEFAULT NULL COMMENT '출근 실행 디바이스UUID(클라 제공, 부정탐지 보조)'`, `CHECK_OUT_DEVICE_UUID varchar(100) DEFAULT NULL COMMENT '퇴근 실행 디바이스UUID(클라 제공, 부정탐지 보조)'`. NULL 허용(기존 행·웹 등록분 호환).
- 탐지쿼리 인덱스: `KEY IDX_ATTD_INDEVICE (CMPNY_CD, WORK_YMD, CHECK_IN_DEVICE_UUID, DEL_YN)` 추가 권장(규칙1 그룹 조회 성능).

**신규 `tb_user_device_login_hist`** (append-only) — planner 설계(§3):
```
CREATE TABLE `tb_user_device_login_hist` (
  `DEVICE_LOGIN_NO` varchar(20)  NOT NULL COMMENT '디바이스 로그인 이력 번호(PK)',
  `CMPNY_CD`        varchar(50)  NOT NULL COMMENT '회사코드',
  `DEVICE_UUID`     varchar(100) NOT NULL COMMENT '디바이스UUID(클라 제공)',
  `USER_CD`         varchar(20)  NOT NULL COMMENT '로그인 사용자코드',
  `DEVICE_TYPE`     varchar(20)      NULL COMMENT '디바이스종류[자유값] ANDROID:안드로이드 IOS:iOS (네이티브 미주입 시 NULL)',
  `DEVICE_MODEL`    varchar(50)      NULL COMMENT '디바이스모델',
  `OS_VERSION`      varchar(20)      NULL COMMENT 'OS버전',
  `APP_VERSION`     varchar(20)      NULL COMMENT '앱버전',
  `CLIENT_TYPE`     varchar(10)      NULL COMMENT '클라이언트구분[자유값] APP:앱 WEB:웹',
  `LOGIN_IP`        varchar(45)      NULL COMMENT '로그인 IP(HttpServletRequest 추출)',
  `LOGIN_DTIME`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '로그인 일시',
  `INSERT_NO`       varchar(50)      NULL DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`DEVICE_LOGIN_NO`),
  KEY `IDX_DLH_DEVICE` (`CMPNY_CD`,`DEVICE_UUID`,`LOGIN_DTIME`),
  KEY `IDX_DLH_USER`   (`CMPNY_CD`,`USER_CD`,`LOGIN_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='디바이스 로그인 이력(append-only, 부정탐지 baseline 소스)';
```
- PK 채번: `FNC_CMM_SEQ_NEXTVAL(cmpnyCd,'DEVICE_LOGIN_NO')` + 날짜 prefix(LoginMapper.selectUserCd 패턴 동일: `CONCAT(DATE_FORMAT(NOW(),'%Y%m'), FNC_CMM_SEQ_NEXTVAL(...))`). varchar(20) 충분 확인.
- ⚠️ MyBatis record 매핑 시 **SELECT 컬럼순서 = record 생성자 인자순서**(위치기반) 함정 주의([[feedback_mybatis_record_column_order]]).
- 코드성 컬럼 COMMENT: DEVICE_TYPE/CLIENT_TYPE 는 '설명[코드체계] 값:의미' 형식 준수([[feedback_db_comment_code_convention]]). (SYS코드 아닌 자유값이라 [자유값] 표기.)

### 0-4. 관리자 인가 패턴 (cross-site IDOR 가드)

- Attd_11(prafta-034, 가장 가까운 선례) 정독 결과: `attdCloseService.canManageNode(authCd, userCd, cmpnyCd, siteCd, nodeCd)` 단일 게이트.
  - master/hr/safe = 전사(`canManageAllNodes(authCd)`) 즉시 통과. 그 외 = 본인 관리 노드만, nodeCd 미지정/타부서 차단.
  - 권한 없으면 `ATTD_403_002`.
- **모니터링 API 도 동일 `canManageNode` 재사용**. siteCd/nodeCd 는 화면 입력이되 서버가 canManageNode 로 강제(프론트 가드 우회 방지). PII(사번/이름) 노출 화면이므로 서버 게이트 필수.
- Attd_11 화면 패턴(viewComm/ViewHeader/viewSearch/viewBody + 사업장·부서 검색팝업 + 단일월 CalendarSrchMonth + 테이블)을 모니터링 화면이 그대로 차용.

### 0-5. Flutter 브리지 패턴

- `web_app.dart`: `_ctl.addJavaScriptHandler(handlerName, callback)` + Vue 가 `window.flutter_inappwebview.callHandler('GET_GPS')` 로 호출(pull 모델). 응답은 Map.
- **`GET_DEVICE_INFO` 핸들러 신규 추가**(GET_GPS 와 동일 pull 패턴): `{deviceId, deviceType:'ANDROID'|'IOS', model, osVersion, appVersion}` 반환. 비즈로직 금지(값 취득만).
- 의존성: `device_info_plus`(모델/OS/IDFV), `package_info_plus`(앱버전), `android_id`(ANDROID_ID, 안드 전용). `flutter pub get` 은 사용자 위임(빌드 hang 방지) — developer 는 pubspec.yaml 편집까지만.

---

## 1. 분해 구조 / 산출물 분리

작업지시서 §6 + prafta-040/app-012 선례에 따라 **영역별 작업 단위 + 요청서 디렉토리 분리**:
- 앱/Flutter 영역 → `app_requests/` (본 작업은 common 출발이나 산출물은 영역 기준)
- 백엔드/웹 영역 → 백엔드는 공통, 웹관리자는 web 영역

ID 는 `prafta-com-003-{순번}` 통합 채번(작업지시서가 common 출처).

### 의존성 그래프

```
[C1 마이그(BE)] ──┬──────────────────────────────────────────────┐
  신규 이력테이블  │                                              │
  attd 디바이스컬럼 │                                              │
  메뉴 SQL         ▼                                              ▼
            [C2 로그인 훅 적재(BE)]                      [C4 출퇴근 도장(BE)]
            device upsert + 이력 INSERT                 CheckIn/Out Request+Param+
                    ▲                                    Command+Mapper 도장
                    │                                          ▲
            [C3 앱FE 로그인 메타 동봉]                  [F1 Flutter GET_DEVICE_INFO]
            브리지 수신 → gv_deviceId 세팅              네이티브ID+메타 브리지
            → 로그인 body 에 메타 추가                         │
                    ▲────────────────────────────────────────┘
                    │ (F1 의 deviceId/메타를 C3 가 소비)
                    │
            [C5 관리자 탐지 API(BE)] ◀── C1,C2,C4 데이터 필요
                    │
            [C6 웹 모니터링 화면(web)] ◀── C5 API
```

핵심 선행:
- **F1(Flutter) → C3(앱FE)**: 앱FE 가 브리지에서 받아야 메타가 채워짐. 단 F1 미완성이어도 C3 는 폴백(localStorage UUID)으로 동작 → 병렬 가능, 통합은 F1 후.
- **C1(마이그) → C2/C4/C5**: 테이블/컬럼 선행.
- **C5 → C6**: API 선행.
- C4 는 C1 만 의존(앱FE 무변경 — axios 가 이미 전송).

---

## 2. 작업 분해 결과

### prafta-com-003-1 — [Flutter] 네이티브 디바이스ID/메타 브리지
- **유형**: backend (Flutter/native, Vue 골격 아님)
- **영역**: app (Flutter 셸)
- **모듈**: PRAFTA_FLUTTER/safenote
- **작업 유형**: 신규
- **요구사항 요약**: Android ANDROID_ID / iOS IDFV + 모델/OS/앱버전을 네이티브에서 취득해 JS 브리지로 webview 에 노출(값 전달만, 비즈로직 금지).
- **상세 설명**:
  - [backend] 핵심 요구사항:
    1) `pubspec.yaml` 의존성 추가: `device_info_plus`, `package_info_plus`, `android_id`(또는 동등). (`flutter pub get` 은 사용자 위임.)
    2) `web_app.dart` 에 `addJavaScriptHandler('GET_DEVICE_INFO', ...)` 추가. GET_GPS 와 동일 pull 패턴.
    3) 반환 계약: `{deviceId, deviceType:'ANDROID'|'IOS', model, osVersion, appVersion}`. Android=ANDROID_ID(`android_id`), iOS=IDFV(`device_info_plus.iosInfo.identifierForVendor`).
    4) 네이티브 획득 실패 시 `{deviceId:null}` 반환(앱FE 가 localStorage UUID 로 폴백, D1 graceful).
    5) 비즈니스 로직 금지(저장/판정 없음, 값 취득·전달만 — CLAUDE.md Flutter 역할분담).
  - 영향 받는 파일: `PRAFTA_FLUTTER/safenote/lib/web_app.dart`, `pubspec.yaml`
  - 예상 산출물: web_app.dart 브리지 핸들러, pubspec 의존성
- **선행 작업**: 없음
- **우선순위 근거**: 앱FE(C3) 통합 선행. 단 폴백으로 분리 진행 가능.

### prafta-com-003-2 — [백엔드] 마이그레이션 (신규 이력 테이블 + attd 디바이스 컬럼 + 메뉴)
- **유형**: backend
- **영역**: web (백엔드 공통 — 앱/웹 양쪽이 쓰는 스키마)
- **모듈**: common / attd
- **작업 유형**: 신규
- **요구사항 요약**: tb_user_device_login_hist 신규 + tb_user_attd_mgmt 디바이스 컬럼 2개 + 모니터링 화면 메뉴 등록. 운영 미적용, 롤백 SQL 동반.
- **상세 설명**:
  - [backend] 핵심 요구사항:
    1) `tb_user_device_login_hist` CREATE (§0-3 설계대로, COMMENT 규칙 준수).
    2) `tb_user_attd_mgmt` ALTER: `CHECK_IN_DEVICE_UUID`, `CHECK_OUT_DEVICE_UUID` 추가 + `IDX_ATTD_INDEVICE` 인덱스.
    3) ★MCP 로 `tb_user_device` 실제 DEL_YN 존재 확인(com-002 반영) 후, **없으면** DEL_YN 추가도 본 마이그에 포함(있으면 생략).
    4) 모니터링 화면 메뉴 SQL(attd 대메뉴 하위 신규 화면 등록 — prafta-040 메뉴 패턴 참고; auth_menu/권한 매핑은 developer 가 실제 메뉴 테이블 구조 MCP 확인 후 작성).
    5) 운영 **미적용**(수동), 각 변경에 롤백 SQL.
  - 영향 받는 파일: `prafta-backend/src/main/resources/db/migration/prafta-com-003-*.sql` (신규)
  - 예상 산출물: 마이그 SQL + 롤백 SQL
- **선행 작업**: 없음 (C2/C4/C5 의 선행)
- **우선순위 근거**: 데이터 정합성·법적책임영역(attd) +1격상. 모든 BE 작업의 토대.

### prafta-com-003-3 — [백엔드] 로그인 훅: 디바이스 upsert + 로그인 이력 적재
- **유형**: backend
- **영역**: web (백엔드 공통 — common.cmm.login)
- **모듈**: common/cmm/login
- **작업 유형**: 보완
- **요구사항 요약**: 로그인 성공 시 tb_user_device upsert + tb_user_device_login_hist INSERT. 예외 격리(적재 실패가 로그인 막지 않음).
- **상세 설명**:
  - [backend] 핵심 요구사항:
    1) `LoginRequest` 에 디바이스 메타 필드 추가: `deviceId`(=gv_deviceId), `deviceType`, `deviceModel`, `osVersion`, `appVersion`. 모두 nullable(웹 로그인은 미전송 → null).
    2) `LoginParam` 에 위 필드 전파 + 정규화(trim/길이컷, 빈값 null). clientType 은 기존 헤더값 재사용(APP/WEB).
    3) `LoginServiceImpl.Login` 의 `updateUserLastLoginDtime` 직후에 **try-catch 격리** 블록 추가:
       - deviceId 가 있으면 `loginMapper.upsertUserDevice(...)`(ON DUPLICATE KEY) + `loginMapper.insertDeviceLoginHist(...)`.
       - deviceId 가 없으면(웹/구버전 앱) skip.
       - 실패 시 `log.error` 만(com-001 체크인 훅 패턴 동일). 로그인 트랜잭션 롤백/실패 금지.
    4) LOGIN_IP = `ClientIpExtractor.extract(httpRequest)` (Controller 에서 추출해 Param 주입 — checkIn 패턴 동일). → LoginController 에 `HttpServletRequest` 파라미터 추가 필요.
    5) `LoginMapper` + `LoginMapper.xml` 에 `upsertUserDevice`, `insertDeviceLoginHist` 추가. DEVICE_LOGIN_NO 채번 = FNC_CMM_SEQ_NEXTVAL 패턴.
    6) (확인 후 결정) `verifyPhoneAuth` 경로 동일 훅 — §10-A.
  - 영향 받는 파일:
    - `LoginRequest.java`, `LoginParam.java`, `LoginController.java`, `LoginServiceImpl.java`, `LoginMapper.java`, `LoginMapper.xml`
    - 신규 Command: `DeviceUpsertCommand`, `DeviceLoginHistCommand` (또는 단일 Command 재사용)
  - 영향 endpoint: POST /comApi/login/login (요청 본문 확장, 하위호환)
  - 예상 산출물: request/param/command/service/mapper 변경
- **선행 작업**: prafta-com-003-2 (테이블)
- **우선순위 근거**: PII(LOGIN_IP/기기ID) 적재 +1격상. baseline 데이터 소스.

### prafta-com-003-4 — [앱FE] 브리지 수신 → gv_deviceId 네이티브화 + 로그인 메타 동봉
- **유형**: frontend-component (화면 아님 — 통신/세션 계층)
- **영역**: app (prafta-app-frontend)
- **모듈**: app/api,composables
- **작업 유형**: 보완
- **요구사항 요약**: Flutter 브리지(GET_DEVICE_INFO)에서 네이티브 deviceId/메타 수신 → gv_deviceId 를 네이티브값으로 세팅(폴백 유지) → 로그인 요청에 디바이스 메타 동봉.
- **상세 설명**:
  - [frontend-component] 핵심 요구사항(script 영역 — developer 구현, Vue 골격 없음):
    1) `axios.js getDeviceId()` 정렬: 네이티브 deviceId 가 주입돼 있으면 그 값을, 없으면 기존 localStorage UUID 폴백(D1). (네이티브값을 받으면 localStorage 의 gv_deviceId 를 네이티브값으로 덮어쓰는 게 권장 — 캐싱.)
    2) 브리지 호출 유틸 신규(예: `utils/deviceBridge.js`): `window.flutter_inappwebview.callHandler('GET_DEVICE_INFO')` await → 캐시. 브리지 없음(웹 미리보기)/실패 시 폴백.
    3) 로그인 흐름(로그인 화면/composable)에서 로그인 요청 body 에 `deviceType/deviceModel/osVersion/appVersion` 추가(deviceId 는 axios 인터셉터가 gv_deviceId 로 이미 머지). 또는 브리지 결과를 인터셉터가 일괄 동봉.
    4) 출퇴근은 무변경(axios 가 gv_deviceId 이미 전송 — §0-1 확인). 단 BE(C4)가 body 키 `gv_deviceId` 를 받도록 맞췄는지 계약 일치 확인.
  - 영향 받는 파일: `prafta-app-frontend/src/api/axios.js`, 신규 `src/utils/deviceBridge.js`, 로그인 화면/composable
  - 예상 산출물: axios/utils/login 흐름 script
- **선행 작업**: prafta-com-003-1 (브리지), prafta-com-003-3 (로그인 메타 수신 BE)
- **우선순위 근거**: F1+C3 통합 지점. 폴백으로 단독 동작 가능.

### prafta-com-003-5 — [백엔드] 출퇴근 레코드 디바이스 도장 (D3)
- **유형**: backend
- **영역**: app (백엔드 app.attd.attd01)
- **모듈**: app/attd/attd01
- **작업 유형**: 보완
- **요구사항 요약**: check-in/out 요청 body 의 deviceId 를 CHECK_IN/OUT_DEVICE_UUID 에 도장. 직접증거 기록.
- **상세 설명**:
  - [backend] 핵심 요구사항:
    1) `CheckInRequest`/`CheckOutRequest` 에 deviceId 필드 추가 — axios 전송 키 `gv_deviceId` 와 바인딩 일치(`@JsonProperty("gv_deviceId")` 권장).
    2) `CheckInParam`/`CheckOutParam.from()` 에서 정규화(trim/100자컷/빈값 null). **JWT 출처 식별값과 분리**(deviceId 는 신뢰경계 밖 표시·탐지 보조).
    3) `CheckInCommand`/`CheckOutCommand` 에 deviceUuid 전파.
    4) `AppAttd01Mapper.xml` INSERT(체크인)에 `CHECK_IN_DEVICE_UUID`, UPDATE(체크아웃)에 `CHECK_OUT_DEVICE_UUID` 추가.
    5) NULL 허용(구버전 앱/측위와 무관하게 deviceId 없으면 NULL 도장).
  - 영향 받는 파일:
    - `CheckInRequest.java`, `CheckOutRequest.java`, `CheckInParam.java`, `CheckOutParam.java`, `CheckInCommand.java`, `CheckOutCommand.java`, `AppAttd01Mapper.xml`
  - 영향 endpoint: POST /appApi/attd/check-in, /check-out (body 확장, 하위호환)
  - 예상 산출물: request/param/command/mapper 변경
- **선행 작업**: prafta-com-003-2 (컬럼)
- **우선순위 근거**: 법적책임영역(attd) +1격상. 탐지 직접증거 소스.

### prafta-com-003-6 — [백엔드] 관리자 부정 출퇴근 탐지 API (on-view 대조)
- **유형**: backend
- **영역**: web (백엔드 web.attd — 신규 서브모듈)
- **모듈**: web/attd/attd12 (신규, attd11 다음 번호)
- **작업 유형**: 신규
- **요구사항 요약**: 규칙1(한 기기→같은날 2계정 이상 출퇴근) + 보조2/3. 근태 관리자 인가·노드/사업장 스코프·cross-site IDOR 가드. 표시 전용.
- **상세 설명**:
  - [backend] 핵심 요구사항:
    1) GET 조회 API(읽기전용). 파라미터: workYm 또는 from~to, siteCd, nodeCd, incSubNodeYn (Attd_11 입력 패턴 차용).
    2) **인가: `attdCloseService.canManageNode(authCd, userCd, cmpnyCd, siteCd, nodeCd)` 재사용**(master/hr/safe 전사, 그 외 본인 노드만, 미통과 ATTD_403_002). PII 노출 화면 → 서버 강제.
    3) 규칙1(결정타): 같은 CMPNY_CD·WORK_YMD·CHECK_IN_DEVICE_UUID(NOT NULL) 그룹에서 서로 다른 USER_CD ≥ 2 → 의심 케이스. 각 케이스에 날짜·기기UUID·관련 계정들(userCd/userNm/사번)·출퇴근시각·노드/사업장.
    4) 보조2: 어떤 출근의 CHECK_IN_DEVICE_UUID 가 본인 로그인이력(tb_user_device_login_hist) baseline 기기집합과 다름.
    5) 보조3: 한 번도 본 적 없는(이력에 없는) 기기에서 출근.
    6) 조회 스코프 = canManageNode 게이트와 동일 노드/사업장. cross-site IDOR: 결과 행도 시도자 관리범위로 필터(타 사업장 누수 금지).
    7) deviceId 위조 가능성 신뢰경계 인지(작업지시서 §5) — 탐지 보조 신호, 차단 아님(D4).
    8) 응답 PII 최소화(이름/사번은 관리 권한 전제 노출, 기기UUID 는 식별자라 그대로).
  - 영향 받는 파일(신규): `com.prafta.web.attd.attd12.*`(controller/service/impl/mapper + xml + request/param/query/result/response). 인가는 attd07 `AttdCloseService` 주입 재사용.
  - 영향 endpoint: GET /webApi/attd12/fraud-attd-suspects (가칭)
  - 예상 산출물: controller/service/mapper/dto 신규
- **선행 작업**: prafta-com-003-2, -3, -5 (데이터)
- **우선순위 근거**: PII 조회 + 보안(IDOR) +1격상. 화면(C6) 선행.

### prafta-com-003-7 — [웹관리자] 부정 출퇴근 의심 모니터링 화면
- **유형**: frontend-screen
- **영역**: web (prafta-web-frontend)
- **모듈**: attd
- **작업 유형**: 신규
- **요구사항 요약**: 의심 케이스 목록 화면(필터: 월/사업장/부서/하위부서). 규칙1 중심, 표시 전용. Attd_11 패턴 차용.
- **상세 설명**:
  - [frontend-screen] 핵심 요구사항:
    1) Attd_11 화면 구조(viewComm/ViewHeader/viewSearch/viewBody + 테이블) 차용.
    2) 필터: 조회월(CalendarSrchMonth) + 사업장(검색팝업 SiteSearchPop) + 소속부서(SiteNodeSearchPop) + 하위부서 체크 + (선택)규칙 필터.
    3) 결과 테이블: 날짜 / 기기UUID(축약 표시) / 관련 계정들(사번·이름 다건) / 출퇴근 시각 / 노드·사업장 / 의심유형(규칙1/2/3 배지).
    4) 읽기전용(생성/저장/삭제 버튼 숨김, 엑셀 노출 — Attd_11 fnButtonControll 패턴). 차단/경고 액션 없음(D4).
    5) 상태별: loading(스피너)/empty("의심 케이스가 없습니다")/error(alert)/success(목록).
    6) 진입은 attd 대메뉴 하위 신규 메뉴(C2 메뉴 SQL). viewResolver 가 컴포넌트명으로 자동 로딩(라우터 수동등록 없음 — [[project_prafta_frontend_layout]]).
  - 영향 받는 파일(신규): `prafta-web-frontend/.../src/views/attd/Attd_12.vue` (또는 명세상 FraudAttdMonitor — §10-C 네이밍 확인)
  - 영향 endpoint: GET /webApi/attd12/fraud-attd-suspects (prafta-com-003-6)
  - 연결 UI 명세: UI-{순번} (아래 §3)
  - 예상 산출물: Attd_12.vue (template+style 골격, script 는 developer)
- **선행 작업**: prafta-com-003-6 (API)
- **우선순위 근거**: PII 노출 화면. API 후행. 화면 UI 개선 후순위지만 본 작업의 가시적 산출물.

---

## 3. 화면 명세

### UI-{순번} Attd_12 (부정 출퇴근 의심 모니터링)
- 연결 작업: prafta-com-003-7
- 화면 위치: `prafta-web-frontend/prafta-web-frontend/src/views/attd/Attd_12.vue`
- 참조 패턴: **Attd_11.vue** (읽기전용 월별 집계 — viewComm/ViewHeader/viewSearch/viewBody, 사업장·부서 검색팝업, CalendarSrchMonth, 읽기전용 버튼 제어, 2단 헤더 sticky 테이블). 디자인 토큰: `assets/css/tokens.css`.
- 레이아웃 (ASCII 와이어프레임):
```
┌─────────────────────────────────────────────────────────────┐
│ [ViewHeader] 부정 출퇴근 의심 모니터링        [조회] [엑셀]    │
├─────────────────────────────────────────────────────────────┤
│ [viewSearch]                                                  │
│  조회월 [2026-06▼]  사업장 [코드][🔍][명]  소속부서 [코드][🔍][명] │
│  ☐ 하위부서 조회   의심유형 [전체▼]                            │
├─────────────────────────────────────────────────────────────┤
│ [viewBody] 테이블 (한 기기→다계정 그룹 단위 행)               │
│ ┌──────┬──────────┬───────────────┬───────────┬────────┬─────┐│
│ │ 날짜 │ 기기(축약)│ 관련 계정      │ 출/퇴근시각│ 부서/사업장│유형 ││
│ ├──────┼──────────┼───────────────┼───────────┼────────┼─────┤│
│ │06-02 │ a1b2…f9  │ 홍길동(1001)   │ 0859/1801 │ 생산1팀 │[규칙1]││
│ │      │          │ 김철수(1002)   │ 0902/1759 │ 생산1팀 │      ││
│ └──────┴──────────┴───────────────┴───────────┴────────┴─────┘│
│  (0건) "의심 케이스가 없습니다."                               │
└─────────────────────────────────────────────────────────────┘
```
- 컴포넌트 매핑:

| 영역 | 컴포넌트 | 비고 |
|---|---|---|
| 헤더 | `ViewHeader` | title/buttons, @search/@excel emit |
| 조회월 | `CalendarSrchMonth` | v-model 단일 월 |
| 사업장 검색 | `SiteSearchPop` (popup) | useModal open |
| 부서 검색 | `SiteNodeSearchPop` (popup) | useModal open |
| 검색아이콘 | `search_icon.png` | Attd_11 동일 |
| 본문 | native table(.data-grid 패턴) | Attd_11 테이블 클래스 차용 |

- 상태별 동작:
  - loading: 전역 로딩 스피너(axios 인터셉터 자동).
  - empty: 테이블 단일행 "의심 케이스가 없습니다."
  - error: `proxy.$alert(resolveApiErrorMessage(...))`.
  - success: 그룹(기기·날짜) 단위 행, 관련 계정 2명+ 멀티라인 표기, 의심유형 배지.
- 사용자 플로우: 진입(메뉴) → 조회월/사업장/부서 입력 → [조회] → on-view 대조 결과 표시 → (표시 전용, 액션 없음) → 필요시 [엑셀].
- 반응형: 관리자 데스크탑 전용(Attd_11 동일, 별도 break point 없음).
- 백엔드 의존: GET /webApi/attd12/fraud-attd-suspects (prafta-com-003-6)

---

## 4. Vue 골격 (prafta-com-003-7)

> Attd_11 패턴 차용. template + style 만 작성. script 는 import/ref 선언 + TODO(developer) 표시. API/store/router 로직 미작성.
> ⚠️ 실제 골격 .vue 파일은 메인 세션 승인 후 디스크 작성(planner 서브에이전트는 Write 보류). 아래는 제안 골격 전문.

```vue
<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @excel="fnExcel"
    />

    <!-- 조회 영역 (Attd_11 패턴 차용) -->
    <div class="viewSearch">
      <div>
        <label>조회월</label>
        <CalendarSrchMonth v-model="workYm" class="a12-nav-month-picker" />
      </div>
      <div>
        <label>사업장</label>
        <input id="siteNo" ref="siteNoFcs" type="text" v-model="siteNo"
          placeholder="사업장코드" :disabled="siteDisabled" @blur="focusKill" />
        <button class="search-btn" :disabled="siteDisabled" @click="fnSiteSearchPopOpen()">
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input id="siteNm" type="text" v-model="siteNm"
          placeholder="사업장명" :disabled="siteDisabled" @blur="focusKill" />
      </div>
      <div>
        <label>소속부서</label>
        <input id="nodeCd" type="text" v-model="nodeCd"
          placeholder="부서코드" :disabled="nodeDisabled" @blur="focusKill" />
        <button class="search-btn" :disabled="nodeDisabled" @click="fnSiteNodeSearchPopOpen()">
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input id="nodeNm" type="text" v-model="nodeNm"
          placeholder="부서명" :disabled="nodeDisabled" @blur="focusKill" />
      </div>
      <div>
        <label class="checkbox-label">
          <input type="checkbox" v-model="incSubNodeYn" :disabled="!nodeCd" />
          하위부서 조회
        </label>
      </div>
      <div>
        <label>의심유형</label>
        <select v-model="suspectType" class="a12-type-select">
          <option value="">전체</option>
          <option value="RULE1">한 기기 다계정</option>
          <option value="RULE2">평소 기기와 다름</option>
          <option value="RULE3">신규 기기</option>
        </select>
      </div>
    </div>

    <!-- 본문: 의심 케이스(기기·날짜 그룹) 목록 -->
    <div class="viewBody a12-body">
      <div class="a12-table-wrap">
        <table class="a12-table">
          <thead>
            <tr>
              <th>날짜</th>
              <th>기기</th>
              <th>관련 계정</th>
              <th>출근시각</th>
              <th>퇴근시각</th>
              <th>부서</th>
              <th>사업장</th>
              <th>의심유형</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="rows.length === 0">
              <td colspan="8" class="a12-empty">의심 케이스가 없습니다.</td>
            </tr>
            <!-- TODO(developer): 한 기기·날짜 그룹 = 관련 계정 N명. 그룹 단위 렌더링 구조는
                 응답 형태 확정 후 v-for 구성(rowspan 또는 멀티라인). 아래는 단일행 가정 골격. -->
            <tr v-for="r in rows" :key="r.suspectKey">
              <td>{{ r.workYmd }}</td>
              <td class="a12-cell-device">{{ r.deviceShort }}</td>
              <td class="a12-cell-left">
                <div v-for="m in r.members" :key="m.userCd">
                  {{ m.userNm }} ({{ m.userId }})
                </div>
              </td>
              <td class="a12-cell-num">{{ r.checkInTime }}</td>
              <td class="a12-cell-num">{{ r.checkOutTime }}</td>
              <td class="a12-cell-left">{{ r.nodeNm }}</td>
              <td class="a12-cell-left">{{ r.siteNm }}</td>
              <td>
                <span class="a12-badge" :class="r.suspectClass">{{ r.suspectLabel }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, defineProps, defineOptions, onMounted } from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CalendarSrchMonth from "@/components/common/CalendarSrchMonth.vue";
import { useModal } from "@/utils/useModal";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
// TODO(developer): API/메시지/엑셀 유틸 import (Attd_11 동일)
// import axios from "@/api/axios";
// import { getMessage, MSG } from "@/messages";
// import { resolveApiErrorMessage } from "@/utils/apiError";
// import { exportStyledExcel } from "@/utils/excelExport";

defineOptions({ name: "Attd_12" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// 헤더 버튼 (읽기전용 — 생성/저장/삭제 숨김, 엑셀 노출). TODO(developer): fnButtonControll
const localButtons = ref({ ...props.buttons });

// 조회 조건
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
const suspectType = ref("");
const workYm = ref("");
const siteNoFcs = ref(null);

// 조회 결과 (의심 케이스 행). TODO(developer): 응답 매핑
//   { suspectKey, workYmd, deviceShort, members:[{userCd,userId,userNm}],
//     checkInTime, checkOutTime, nodeNm, siteNm, suspectLabel, suspectClass }
const rows = ref([]);

// master/hr 여부 (그 외 사업장+부서 필수 — Attd_11 동일)
const isMasterOrHr = computed(() => {
  const a = sessionStorage.getItem("gv_authCd");
  return a === "master" || a === "hr";
});

// TODO(developer): focusKill / fnSrchSiteInfo / fnSrchNodeInfo / fnCallback /
//   fnSiteSearchPopOpen / fnSiteNodeSearchPopOpen — Attd_11 패턴 그대로 복사 후 조정.
const focusKill = () => { /* TODO(developer) */ };
const fnSiteSearchPopOpen = () => { /* TODO(developer): openPop(SiteSearchPop, {...}) */ };
const fnSiteNodeSearchPopOpen = () => { /* TODO(developer): openPop(SiteNodeSearchPop, {...}) */ };

// 조회
const fnSearch = async () => {
  // TODO(developer): 사업장 필수 + 비 master/hr 부서 필수 검증(Attd_11 동일),
  //   GET /webApi/attd12/fraud-attd-suspects 호출 + rows 매핑.
};

// 엑셀
const fnExcel = async () => {
  // TODO(developer): exportStyledExcel (Attd_11 패턴)
};

onMounted(() => {
  // TODO(developer): fnInit(세션 사업장/부서 프리필) + fnButtonControll + workYm 초기값(현재월)
});
</script>

<style scoped>
/* ── 조회 영역 (Attd_11 패턴 차용) ── */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  cursor: pointer;
  user-select: none;
  margin-left: -1rem;
  margin-right: 0.4rem;
  white-space: nowrap;
}
.checkbox-label input[type="checkbox"] {
  width: 13px;
  height: 13px;
  cursor: pointer;
  accent-color: var(--color-primary);
  flex-shrink: 0;
}
.a12-type-select {
  height: 28px;
  padding: 0 0.5rem;
  border: 1px solid var(--color-border-strong);
  border-radius: 4px;
  font-size: 0.875rem;
  color: var(--color-text-strong);
  background: var(--color-surface);
}
.a12-nav-month-picker {
  display: inline-flex;
  align-items: center;
}
.a12-nav-month-picker :deep(.calendar-input) {
  height: 28px;
  padding: 0 0.5rem;
  border: 1px solid var(--color-border-strong);
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-strong);
  background: var(--color-surface);
  cursor: pointer;
  text-align: center;
  min-width: 110px;
}
.a12-nav-month-picker :deep(.calendar-input:hover) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* ── 본문 / 테이블 (Attd_11 패턴 차용) ── */
.a12-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
.a12-table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-surface);
}
.a12-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}
.a12-table thead th {
  background: var(--thead-bg, #f3f4f6);
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.4rem;
  line-height: 1.2;
  position: sticky;
  top: 0;
  z-index: 1;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text);
  font-weight: 600;
}
.a12-table tbody td {
  border: 1px solid var(--color-border);
  padding: 0.4rem;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text);
  vertical-align: top;
}
.a12-table tbody tr:hover {
  background: var(--color-bg);
}
.a12-cell-left {
  text-align: left;
}
.a12-cell-num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.a12-cell-device {
  font-family: monospace;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
.a12-badge {
  display: inline-block;
  padding: 0.1rem 0.45rem;
  border-radius: 10px;
  font-size: 0.75rem;
  font-weight: 600;
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}
.a12-empty {
  padding: 2rem;
  color: var(--color-text-muted);
  text-align: center;
}
</style>
```

---

## 5. 정책 출처 / 확정결정 반영

- 비즈니스 정책 출처: 본 작업은 **부정탐지·디바이스 식별**으로 PRAFTA 비즈니스 정책서(attd/common/request-approval)의 직접 룰이 아니라 **작업지시서 prafta-com-003 자체가 단일 설계 출처**(D1~D6). 다만 인접 정책:
  - 출퇴근 판정/Mock 위치/지오펜스 = attd 정책서(app-003 선례에서 적용 완료) — 본 작업은 그 위에 deviceId 도장만 추가(판정 로직 무변경).
  - PII 최소수집·JWT 클레임 §11.1 = common 정책서. deviceId/LOGIN_IP 적재는 §11.1 "내부 부정탐지 정당이익" 전제, JWT 클레임에는 추가 안 함(현행 유지).
- 확정결정 반영 매핑:
  - D1(네이티브ID 안드/iOS 동시) → C1(F1).
  - D2(2테이블) → C1 마이그(신규 이력) + C2 upsert(기존 device).
  - D3(출퇴근 도장) → C1 컬럼 + C5 도장. ★도달경로 결론: 바디 추가(§0-1).
  - D4(표시전용) → C6 화면 액션 없음, C5 차단 없음.
  - D5(공용기기 없음) → C5 규칙1 항상 의심 취급(예외 없음).
  - D6(웹 전용 모니터링 + on-view 대조) → C5 on-view 쿼리 + C6 화면.

---

## 6. 보안 검토 예고 (security 에이전트)

- deviceId 신뢰경계: 클라 제공값 위조 가능 → 식별/인가 사용 금지, 표시·탐지 보조만.
- 모니터링 API 인가: canManageNode 게이트 + 결과 cross-site 필터(IDOR). PII(사번/이름) 노출 화면.
- LOGIN_IP/기기ID 로그/응답 PII 최소화.
- 로그인 훅 적재 예외 격리(로그인 흐름 보호).

---

## 7. 메인 세션 Notion 반영 항목

> planner 서브에이전트는 Notion 접근 없음. 메인 세션이 아래를 "작업 로그" DB 에 등록.

| 작업ID | 영역 | 모듈 | 작업유형 | 담당 | 요구사항 요약 | 산출물 |
|---|---|---|---|---|---|---|
| prafta-com-003-1 | app | PRAFTA_FLUTTER/safenote | 신규 | planner→dev | [backend] Flutter 네이티브 deviceId/메타 GET_DEVICE_INFO 브리지 | web_app.dart, pubspec.yaml |
| prafta-com-003-2 | web | common/attd | 신규 | planner→dev | [backend] 마이그: 로그인이력테이블+attd 디바이스컬럼+메뉴(운영미적용) | prafta-com-003-*.sql |
| prafta-com-003-3 | web | common/cmm/login | 보완 | planner→dev | [backend] 로그인훅 device upsert+이력INSERT(예외격리) | Login* + Mapper |
| prafta-com-003-4 | app | app/api,composables | 보완 | planner→dev | [frontend-component] 브리지수신→gv_deviceId네이티브화+로그인메타동봉 | axios.js, deviceBridge.js |
| prafta-com-003-5 | app | app/attd/attd01 | 보완 | planner→dev | [backend] 출퇴근 deviceId 도장(CHECK_IN/OUT_DEVICE_UUID) | CheckIn/Out* + Mapper |
| prafta-com-003-6 | web | web/attd/attd12 | 신규 | planner→dev | [backend] 부정출퇴근 탐지API(규칙1+보조2/3, canManageNode, IDOR) | attd12.* 신규 |
| prafta-com-003-7 | web | attd | 신규 | planner→dev | [frontend-screen] 부정출퇴근 의심 모니터링 화면 [UI 명세: UI-{순번}] | Attd_12.vue |

- "도메인 지식 베이스" DB: `UI-{순번} Attd_12 (부정 출퇴근 의심 모니터링)` — 영역 web / 모듈 attd / 현재동작 "신규 작성" / 의도된 동작=§3 명세 / 검증상태 Claude 분석.
- 선행 Relation: 2→{3,5,6}; 1→4; 3→4,6; 5→6; 6→7.

---

## 8. 마이그레이션 운영 적용 주의

- 모든 마이그 SQL **운영 미적용**(수동). C2(테이블/컬럼) 선적용 없이 C3/C4/C5 배포 시 INSERT/도장/조회 전면 실패 → 배포 순서 = 마이그 먼저.

---

## 9. 향후 시너지 메모 (구현 X)

- C2 의 tb_user_device upsert 자리는 com-002 FCM PUSH_TOKEN 채움과 동일 위치 → 추후 PUSH_TOKEN 도 같은 upsert 에 합류 가능(현재 미구현).

---

## 10. 채팅 확인 필요 (보류 — 합리적 기본값 적용했으나 확인 권장)

- **A. verifyPhoneAuth(인증대기 04→01) 경로 디바이스 훅 적용 여부**: 정식 토큰 발급+lastLogin 갱신 경로라 일관성상 동일 훅이 맞으나, 해당 요청 body 에 디바이스 메타가 오는지(앱 인증 화면이 메타 동봉하는지) 불확실. **기본값**: C3 가 verify 요청에도 메타 동봉 + C3-BE 훅 적용. 미동봉 시 deviceId 만 적재 or 이 경로 v1 제외.
- **B. deviceId DTO 바인딩 키**: axios 전송키는 `gv_deviceId`. BE DTO 필드를 `gv_deviceId` 로 둘지 `@JsonProperty("gv_deviceId") String deviceId` 로 둘지 — developer 재량(바인딩 일치만 보장). 기본값: `@JsonProperty` 매핑.
- **C. 신규 모듈/화면 네이밍**: 백엔드 `attd12`, 화면 `Attd_12.vue` 로 attd 라인 연번 채택(Attd_11 다음). 만약 부정탐지를 attd 가 아닌 별도 보안/감사 도메인으로 분리하길 원하면 모듈명 변경 필요. 기본값: attd12.
- **D. 탐지 조회 단위**: 기본값 = 월(workYm) 단위(Attd_11 동일). 일자 범위(from~to)가 필요하면 확장. 규칙1 은 "같은 날" 기준이라 월 조회 후 날짜별 그룹.
- **E. tb_user_device DEL_YN 실재 여부**: 스냅샷엔 없음(com-002 미반영 추정). developer MCP 확인 필수 — 없으면 C2 에 DEL_YN 추가 포함.
