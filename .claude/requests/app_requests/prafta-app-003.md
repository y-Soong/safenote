# prafta-app-003 — 앱 출퇴근 액션(GPS) + TBM 입실 액션 (prafta-app-001에서 분리)

> 출처: prafta-app-001(앱 메인화면 데이터 동기화) 작업 중 범위 분리. 사용자 지시(#5)로 "범위가 커지는 부분은 신규 세션에서 진행하도록 정리".
> prafta-app-001에서 **표시(읽기) 동기화는 완료**했고, 본 문서는 **쓰기 액션 + 네이티브 GPS** 영역을 별도 세션으로 넘긴 명세다.

---

## 분리 사유 (prafta-app-001 작업 중 확인된 사실)

- `tb_user_attd_mgmt` 에는 GPS/사업장외 판정 컬럼이 **없다**. 지오펜스 판정은 `tb_user_attd_gps`(별도 테이블) + 사업장 좌표/반경 기준으로 서버가 수행해야 한다.
- 앱에서 실제 출근/퇴근을 기록하려면 **Flutter 네이티브 위치권한 + 위경도 수집 → webview JS-bridge 로 Vue 전달 → 백엔드 POST** 경로가 필요하다(현재 앱에 위치 브리지 없음).
- 출퇴근 가능 여부/횟수는 정책 §5(checkin-limits), §7(checkin-checkout)에 종속되고, prafta-028 근태 월마감 게이트(마감 시 쓰기 차단)와도 연동된다 — 법적 책임 영역이라 단독 세션에서 신중히 다룬다.
- TBM "입실"은 세션 입실 비밀번호(`tb_tbm_session.ENTRY_PWD`) 또는 관리자 QR 스캔(`ENTRY_TYPE_CD`) + 입실 GPS(`tb_tbm_attendance.ENTRY_GPS_*`, `ENTRY_DISTANCE_M`) 기록이 필요한 쓰기 액션이다. prafta-001에서는 **읽기전용 상태 표시만** 처리했다.

---

## 본 세션(app-003)에서 할 일

### ⚠️ A-0. prafta-app-002 연계 — 퇴근(check-out)은 백엔드만 선구현됨, GPS 브리지로 완결 필요 (필수)

prafta-app-002("내 근태 조회" + 퇴근하기) 작업에서 **퇴근 백엔드는 옵션 B로 이미 구현 완료**했다. 단, 앱에 GPS 획득 수단(Flutter 위치 브리지)이 없어 **프론트의 실제 GPS 좌표 연동은 app-003으로 넘겼다.** app-003은 아래를 반드시 포함해야 한다(중복 신규개발 금지 — 기존 산출물에 연결만):

- **이미 구현됨(app-002, 재사용)**: `POST /appApi/attd/check-out` 엔드포인트(`com.prafta.app.attd.attd01`). 본인 열린 근태 UPDATE(CHECK_OUT_DATE/TIME=서버 raw 시각, CHECK_OUT_METHOD='01'), **D+1 윈도우**(근무일 당일·익일만 퇴근 허용, 초과 시 퇴근 불가→처리필요), **사업장 다름 차단**(세션 siteCd ≠ 출근 레코드 SITE_CD 거부), **GPS 필수(②A)** — 요청 본문의 LAT/LON/ACCURACY/IS_MOCKED 수신 → `tb_user_attd_gps`(GPS_INFO_TYPE='02') INSERT(GPS_ID=`CONCAT(yyyymmdd, FNC_CMM_SEQ_NEXTVAL(cmpnyCd,'GPS_ID'))`). 감사이력(HIST)은 미기록(①A, SYS032에 본인퇴근 코드 없음).
- **app-003에서 해야 할 것(퇴근 완결)**:
  1. Flutter geolocator(또는 동급) + 위치권한 추가 → 현재 위경도/정확도/Mock여부를 JS-bridge로 Vue에 전달하는 **GPS 채널 신설**.
  2. 앱 FE `AttendanceTodayCard`의 [퇴근하기] 버튼에 박힌 `// TODO(app-003 GPS bridge)` 자리에 GPS 브리지 호출을 연결 → 좌표를 담아 기구현된 `POST /appApi/attd/check-out` 호출 → 성공 시 화면 재조회. (엔드포인트/검증 로직은 신규개발 불필요, 이미 있음.)
  3. **GPS 미수신/Mock(IS_MOCKED='Y') 시 퇴근 거부**(②A) — 백엔드가 이미 거부하지만, 프론트도 좌표 없으면 호출 차단 + 안내.
- ⚠️ 백엔드 체크아웃의 "사업장 다름 차단"은 현재 **세션 siteCd vs 출근 레코드 SITE_CD 동일성**으로만 판정한다. `tb_site`에 좌표(LAT/LON)가 없어 **거리 기반 지오펜스는 불가**하다. 거리 지오펜스가 필요하면 app-003에서 사업장 좌표 컬럼 추가(스키마 변경)를 별도 검토.
- ⚠️ **(QA Low-1, app-003에서 해결)** 현재 `POST check-out`은 요청에 대상 일자를 받지 않고 **가장 최신 열린 근태 1건(WORK_YMD DESC LIMIT 1)**을 퇴근 처리한다. 어제(D-1)·오늘(D) 둘 다 미퇴근인 오버나이트 사용자가 day-detail에서 *어제 카드* 퇴근을 누르면 *오늘 건*이 퇴근되는 카드-대상 불일치가 가능하다. app-003에서 실호출 배선 시 **요청 body에 workYmd(또는 attdId) 추가 → 서버가 본인·열린·D+1윈도우 재검증 후 그 건만 퇴근** 하도록 보완할 것. (현재는 옵션B로 실호출 차단 상태라 미발현.)
- ⚠️ **운영 확인**: `FNC_CMM_SEQ_NEXTVAL`의 'GPS_ID' 시퀀스가 회사별 정상 채번되는지(미초기화 시 함수가 자동 생성하나 운영 검증 권장), p6spy로 UPDATE(raw 시각)+GPS INSERT 트랜잭션 원자성 실측.

### A. 앱 출퇴근 액션 (출근/퇴근 버튼 실제 동작)
1. Flutter: 위치권한 요청 + 현재 위경도 수집 → JS-bridge(`postMessage`/`addJavaScriptChannel`)로 Vue 에 전달하는 채널 신설(양쪽 명세 동시 갱신, CLAUDE.md Flutter 규약 준수). **(A-0의 GPS 채널과 동일 — 출근/퇴근 공용)**
2. 백엔드: `POST /appApi/attd/check-in` **신규**, `POST /appApi/attd/check-out` **(app-002에서 구현 완료 — 재사용)**
   - 출근(check-in): `tb_user_attd_mgmt` INSERT(ATTD_ID 채번, WORK_SEQ, CHECK_IN_DATE·TIME(HHMM), CHECK_IN_METHOD[SYS031]='01'). **다음날 출근은 전날 미완료 퇴근이 없을 때만 가능**(전날 열린 근태 있으면 출근 차단 — 사용자 확정 정책).
   - 지오펜스/사업장외 여부: 위 A-0 한계(사업장 좌표 부재) 동일 적용.
   - 정책 §5 출퇴근 제한(횟수/구간), prafta-028 월마감 게이트(마감월 쓰기 차단) 적용.
   - 인증: 기존 app 패턴(JWT 클레임 → TokenInfo), userCd 바디로 받지 않음.
3. 앱 FE: AttendanceCard/AttendanceTodayCard 출근/퇴근 버튼 → 위치 수집 → API 호출 → 성공 시 home-summary/근태조회 재조회.

### B. home-summary 의 isOffsite 실시간 GPS 화
- prafta-001에서는 `isOffsite=false` placeholder. 실제로는 조회 시점 GPS 또는 마지막 출근 판정값으로 채운다(설계 결정 필요: 저장값 vs 실시간).

### C. TBM 입실/종료 액션
- 입실 비밀번호/관리자 QR + 입실 GPS 기록(`tb_tbm_attendance`). prafta-033 앱 TBM 보류분과 정합 확인.

---

## 선행/참조
- 정책: `.claude/context/policies/attd/INDEX.md` → §5(checkin-limits), §7(checkin-checkout). prafta-028 월마감.
- 스키마: `tb_user_attd_mgmt`, `tb_user_attd_gps`, `tb_tbm_attendance`, `tb_tbm_session`.
- 앱 통신/세션 정렬: 메모리 `project_prafta_app_vite_and_api_align`.
- prafta-app-001 산출물(읽기 home-summary 엔드포인트)을 재사용.
