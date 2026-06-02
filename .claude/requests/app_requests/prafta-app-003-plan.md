# prafta-app-003 작업 분해 — 앱 출퇴근 액션(GPS) + isOffsite + TBM 입실

> 작성: 메인 세션(인라인 — sub-agent API 일시 과부하 529로 planner 대체).
> 단일 출처: 본 문서. 원본 지시서 `prafta-app-003.md`.
> 대상: Flutter 셸 `PRAFTA_FLUTTER/safenote/` + 앱 프론트 `PRAFTA/prafta-app-frontend/prafta-app-frontend/` + 백엔드 `PRAFTA/prafta-backend`.
> 성격: **법적 책임 도메인(근태) + 네이티브 GPS 쓰기**.

---

## 0. 확정 모델 (사용자 컨펌 2026-05) — ⚠️ app-002 퇴근 백엔드 수정 포함

### 0-1. 위치권한: 앱 실행 시 필수, 미동의 시 앱 사용 불가
- Flutter 셸이 앱 기동 시 위치권한을 요청하고, **거부/영구거부 시 앱 진입 차단**(웹뷰 로드 전 차단 화면 + 설정 유도, 재요청). 카메라 등 기존 권한과 별개로 위치는 **하드 게이트**.
- 권한이 보장되므로, 출퇴근 시점에 앱은 **항상 현재 GPS를 읽을 수 있다**(좌표 부재 케이스는 예외적 — 서비스 OFF 등).

### 0-2. GPS 좌표는 "지오펜스 범위 밖" 출퇴근에만 수집/저장 (정책 §7.2 정합)
- 출근/퇴근 찍을 때: 앱이 현재 GPS(lat/lon/accuracy/isMocked)를 **항상 서버로 전송** → **서버가 사업장 중심좌표(tb_site.LAT/LON) 기준 거리(haversine) vs GPS_RANGE 로 지오펜스 판정**.
  - **범위 안(정상)**: 근태 레코드만 기록. **GPS 행 미저장**(온사이트).
  - **범위 밖(외근)**: 근태 레코드 + **`tb_user_attd_gps` 행 INSERT**(GPS_INFO_TYPE 01출근/02퇴근). 응답 `isOffsite=true` → 앱이 "외근으로 처리되었습니다" 안내.
- **외근 판정 = GPS 행 존재 여부**(기존 웹 attd07 관례 "ATTD_ID 가 TB_USER_ATTD_GPS 에 존재하면 외근='Y'"와 동일 — 신규 컬럼 불요).
- Mock 위치(`isMocked='Y'`): **거부**(부정 방지). ☞ 잔여 확인: 거부 vs 허용+미확인표시 (기본=거부).
- 서비스 OFF/좌표획득 실패(권한은 있으나 측위 실패): 정책 §7.3 → 등록 허용 + "GPS 미확인" (마감 차단 사유). 단 0-1로 권한은 보장되므로 드묾.

### 0-3. tb_site 좌표: 존재 가정 (사용자가 컬럼 추가 예정)
- **백엔드는 `tb_site.LAT`, `tb_site.LON` 이 있다고 가정**하고 거리 지오펜스 구현. GPS_RANGE(기존 varchar(4), m)와 함께 사용.
- ⚠️ **사용자 작업(코드와 컬럼명 일치 필수)**: `tb_site` 에 아래 컬럼 추가 예정 — 코드가 참조할 명/타입:
  ```sql
  ALTER TABLE tb_site
    ADD COLUMN LAT DECIMAL(10,7) NULL COMMENT '사업장 중심 위도',
    ADD COLUMN LON DECIMAL(10,7) NULL COMMENT '사업장 중심 경도';
  ```
  (tb_user_attd_gps.LAT/LON 과 동일 타입. 명칭이 다르면 본 문서에 반영 필요.)
- 데이터 미입력 사업장(LAT/LON NULL) 폴백: 지오펜스 판정 불가 → 임시로 "온사이트로 간주"(또는 "미확인") — 잔여 확인. 사용자가 "추후 GPS 무조건 있게 보완" 예정이므로 NULL은 과도기.

---

## 1. 작업 단위 (APP003-x) — 순서/사이즈/선행

| ID | 유형 | 산출물 | 선행 | 사이즈 |
| --- | --- | --- | --- | --- |
| **A0-1** | flutter+frontend | GPS 브리지 + **위치권한 하드 게이트** | 없음 | M |
| **A0-2** | backend+frontend | 퇴근 지오펜스화(app-002 수정) + 실연동 + Low-1 | A0-1, tb_site컬럼 | M |
| **A1** | backend | 출근(check-in) 신규(지오펜스 포함) | A0-2 모델, tb_site컬럼 | L |
| **A2** | frontend | 출근 버튼 실연동 | A0-1, A1 | S |
| **B** | backend+frontend | home-summary isOffsite = 최근 출근 GPS행 존재 | A0-2 모델 | S |
| **C1** | 조사(메인) | tb_tbm_* 스키마 + prafta-033 정합 | MCP | S |
| **C2** | backend+frontend | TBM 입실/종료(+GPS) | C1, A0-1 | L |

### A0-1 — GPS 브리지 + 위치권한 하드 게이트 (최우선, 결정 무관)
- **Flutter** (`safenote/`):
  - `pubspec.yaml`: `geolocator` 추가(Position.isMocked). `flutter pub get`.
  - AndroidManifest: `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`.
  - `lib/main.dart` 또는 `web_app.dart`: **앱 기동 시 위치권한 요청 → 거부 시 웹뷰 로드 차단 + 안내/재요청/설정이동**(미동의=앱 사용불가). 영구거부는 `openAppSettings()` 유도.
  - `web_app.dart`: `addJavaScriptHandler('GET_GPS', ...)` — geolocator로 위치 취득 후 `{status,lat,lon,accuracy,isMocked}` 반환. 권한/서비스/타임아웃/mock status 구분.
- **Vue**: `src/utils/gpsBridge.js` — `requestGps()` (callHandler 래핑, 타임아웃, 브리지부재 처리). 앱 진입 가드와 연동(권한 없으면 진입 차단 — Flutter가 1차, Vue는 폴백 안내).
- 검증: `flutter pub get`+`flutter analyze`(메인). 실기기 권한/측위는 사용자 위임.

### A0-2 — 퇴근 지오펜스화(app-002 수정) + 실연동 + Low-1
- **백엔드 수정**(app-002 check-out): ㉠ "좌표없음 거부" 제거(권한 보장 전제; 좌표 오면 사용). ㉡ **GPS 항상 INSERT → "지오펜스 밖일 때만 INSERT"** 로 변경. ㉢ 지오펜스 계산 헬퍼(haversine, tb_site.LAT/LON+GPS_RANGE) 추가, mock 거부 유지. ㉣ 응답에 `isOffsite` 추가. ㉤ Low-1: body `workYmd`(또는 attdId) 받아 그 일자 열린건만 퇴근(서버 재검증).
- **프론트**: 퇴근 버튼 → gpsBridge → check-out(body에 좌표+workYmd) → 응답 isOffsite면 "외근 처리" 안내 → 재조회.

### A1 — 출근(check-in) 백엔드 신규
- `POST /appApi/attd/check-in`. 정책 §5/§7: 구간기반 출근횟수(§5.1), 재출근=이전퇴근 후(§5.2), 초과출근 차단+"초과근무 상신"안내(§5.3/§5.4), 스케줄없는날 허용+추가근무필수(§7.5), **다음날 출근=전날 열린근태 없을때만**(사용자확정), 사업장/마감 게이트, **지오펜스(밖이면 GPS행+외근, mock거부)**, ATTD_ID/GPS_ID 채번. tb_user_attd_mgmt INSERT.

### A2 — 출근 프론트 연동 / B — isOffsite(=최근 출근 ATTD_ID의 GPS행 존재) / C — TBM(스키마확인 후)

---

## 2. GET_GPS 브리지 계약
요청: `window.flutter_inappwebview.callHandler('GET_GPS')` → Promise.
정상: `{status:'OK', lat, lon, accuracy, isMocked}`.
실패: `{status:'PERMISSION_DENIED'|'SERVICE_DISABLED'|'TIMEOUT'|'BRIDGE_UNAVAILABLE'}`.
- 백엔드 전송: lat/lon/accuracy(Number) + isMocked('Y'/'N'). 서버가 지오펜스/저장 결정.

## 3. 정책서 출처
- §5.1~5.4 출근횟수/재출근/초과차단 → `05-checkin-limits.md`
- §7.1 기본/§7.2 지오펜스("근무지 외" 태그)/§7.3 GPS미확인·mock/§7.5 스케줄없는날 → `07-checkin-checkout.md`
- 초과근무 사후=마감 전까지 → `request-approval/03-policy-alignment.md §3.2`
- 마감 차단(부서단위) → `13-attendance-close.md`(prafta-028)
- 사업장 GPS 허용범위 → 공통정책서 6장

## 4. 권장 착수 순서
1. **A0-1(GPS 브리지+권한 게이트)** — 즉시 착수(컬럼/모델 무관).
2. (사용자) tb_site LAT/LON 컬럼 추가.
3. A0-2(퇴근 지오펜스화) → A1/A2(출근) → B(isOffsite) → C(TBM).
4. 각 단계 developer→security→qa.

## 5. 미확인/리스크
- ⚠️ tb_tbm_session/tb_tbm_attendance 스키마(C, 메인 MCP 확인 예정).
- ⚠️ tb_site LAT/LON 컬럼 추가는 사용자 작업 — 코드 컬럼명 일치 확인.
- ⚠️ LAT/LON NULL 사업장 폴백 정책(잔여 확인).
- ⚠️ mock 거부 vs 허용+미확인(기본=거부, 잔여 확인).
- ⚠️ iOS 위치권한(Info.plist)·실기기 실측 사용자 위임. geolocator/AGP 호환 `pub get` 확인.
