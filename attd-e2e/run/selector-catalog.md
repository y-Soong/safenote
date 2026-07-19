# 셀렉터/라우트 카탈로그 (실측 — 후속 세션 재사용)

> E1 세션(2026-07-17)에서 실측. 앱 내비게이션은 하단 탭/버튼 클릭만(해시 딥링크는 홈 수렴 — 단 폼 진입 후 URL은 참고용으로 기록).

## 앱 (https://localhost:8082)

### 공통
- 하단 탭바: `button.app-tabbar__tab:has-text("홈"|"근태"|"안전"|"TBM"|"마이")`
- 홈 카드 로딩 대기: `waitForFunction(() => !document.body.innerText.includes("불러오는 중"))`
- 확인/성공 팝업: `.modal-overlay` + `button:has-text("확인")` — 액션 팝업(확인/취소) → 완료 팝업(확인) 2단. 팝업 열림 중에는 배경 클릭 불가(modal-overlay가 인터셉트).

### 홈(MainView) 출퇴근 카드
- 출근: `button:has-text("출근하기")` (btn btn--primary) → '출근하시겠어요?' 확인 팝업 → '출근이 등록되었어요.'
- 퇴근: `button:has-text("퇴근하기")` → '퇴근하시겠어요?' → '퇴근이 등록되었어요.'
- 2구간 근무타입 배정 시 홈 카드 버튼이 `1구간 출근` / `2구간 출근` 으로 바뀜. seg1 OPEN 중에는 두 버튼 모두 미노출(일반 출근하기/퇴근하기로 전환).
- 지오펜스 밖 출근 확인 시: '외근 출근 등록' 시트(외근 사유 필수 + [외근으로 등록]/[취소]).

### 내근태(MyAttendanceView)
- 탭: `.attd-seg__item:has-text("오늘"|"이번주"|"이번달")`
- 이번주 요일 카드: `button.dc` (innerText 예: "화\n14\n00005 QTOVN\n스케줄 22:00 ~ 04:30") — 텍스트 매칭은 evaluate+RegExp 권장(:has-text 불안정)
- 이번달 캘린더 일 셀: innerText 첫 줄이 일자 숫자인 button/td — evaluate로 탐색
- 일 선택 후 하단 상세에 `수정 요청` 버튼(반드시 `button:has-text("수정 요청")` — 안내문 "수정 요청해 주세요"와 충돌 주의)
- 수정 요청 → 4액션 시트: `스케줄 수정 요청` / `근태 보정 요청` / `초과근무 신청` / `연차 신청`
- 퇴근 후 액션: `퇴근 시간 재등록`(재퇴근, app-026) / `출근하기 (2회차)`

### 요청 폼(AttdRequestView — /#/AttdRequest?type=attdCorrection|overtime|schedModify&workYmd=YYYYMMDD)
- 날짜 스텝퍼: `button.dsf-field` → 시트에 `input[inputmode="numeric"]:visible` 3개(년/월/일) → `button.wp-confirm`
- 시각 스텝퍼: `button.tsf-field` → 시트에 numeric input 2개(시/분) → `button.wp-confirm`
- 사유: `textarea` (0/100)
- 제출: `button:has-text("요청하기")` (유효성 미충족 시 disabled)
- 보정/OT 모두 결재선 지정 불필요(관리자 처리 모델) 안내 문구 존재
- OT 폼: '등록 가능 시간'(raw−스케줄) 자동 표기+프리필. 스케줄 내 시각 입력 시 인라인 경고 '스케줄 시간 내에는 초과근무를 등록할 수 없어요.' + 제출 비활성. 기승인 OT 존재 시 '이미 등록된 초과근무' 섹션(app-030).

### 내 요청(MyRequests) — 홈 '승인 요청' 카드 클릭 → /#/MyRequests
- 하단 탭바 없음(뒤로가기로 복귀). 카드에 상태(대기/승인/반려) 표기.

### 앱 관리자(QTHR)
- QTHR 앱 로그인 착지는 MainView(사용자 홈). AdminHome 진입: 마이 탭 → `관리자 모드` 버튼 → /#/AdminHome
- AdminHome → `승인 관리` → /#/AdminApproval?module=APPROVAL (승인 대기/이력 탭 + 유형 필터: 스케줄수정/근태보정/초과근무/연차)
- 건 선택 → 상세: `요청대로 승인`/`반려` 선택 → `처리하기` → '요청 내용 그대로 승인할까요?' 확인 팝업

## 웹 (http://localhost:8081)

### 라우트 (/safenote/main/{MENU_D_ID})
- Attd_05 근무계획 관리 / Attd_07 근무 관리 / Attd_08 근로자 근태조회 / Attd_09 사용자 연차관리 / Attd_10 요청 승인 관리 / Attd_02 휴일 관리 / Attd_01 근무타입 관리 / Baim_01 사업장관리
- 로그인: `/safenote` → `#userId`, `input[type=password]`, `button.login-btn` → `/safenote/main`

### Attd_08 (근로자 근태조회)
- 기본 사업장 프리필(QT001), 기간 최근 1개월. `button:has-text("조회")` → 결과 테이블 행: 사용자명/근무구분/근무일/차수/스케줄/출퇴근시각/실근로/인정/상태(정상·지각·조퇴)
- OT는 근무구분='초과근무' 별도 행

### Attd_10 (요청 승인 관리) — 보정·OT 승인 실경로
- 상단 유형 탭: `button:has-text("근태 보정")` / `초과근무 상신` / `연차 상신` (건수 배지)
- 좌측 대기 카드(요청자명 텍스트 클릭) → 우측 상세 → 라디오 `요청대로 승인`/`반려` → `button:has-text("처리하기")` → '승인/반려 처리하시겠습니까?' 모달 `확인` → '승인되었습니다.'
- 겹침 승인 차단 얼럿: '다른 근무 구간과 시간이 겹칩니다.'(ATTD_400_113)

### Attd_07 (근무 관리)
- 조회 후 캘린더 매트릭스: 행 tr(사용자), 일 셀 `td.m-day-cell` — 일자상세 팝업은 **더블클릭**(@dblclick)
- 일자상세 팝업: 근무계획/실제 출퇴근/비고(초과근무 N건 인정)/관리자 직접 수정/초과근무 구간 편집

### 사업장 GPS (Baim_01 → SiteInfoPop)
- lat/lon은 주소 지오코딩(카카오 SDK) 산출 — headless 임의 좌표 불가. 시드는 UI 동일 endpoint `POST /webApi/baim01/save-site-infos` (payload에 lat/lon/gpsRange 문자열) 사용.

## API (시드용, http://localhost:8080/prafta)
- 로그인: `POST /comApi/login/login` {userId,userPw} + X-Client-Type — 응답 `token`
- 근무계획 배정: `POST /webApi/attd05/save-user-work-plans` [{siteCd,userCd,workYmd,workPlanCd}]
- 근무계획 셀 삭제: `POST /webApi/attd05/delete-user-work-plan-cells` [{siteCd,userCd,workYmd}]
- 사업장 저장: `POST /webApi/baim01/save-site-infos` [...SiteInfoPop payload...]

## 연차 (E2 실측 append)

### 앱 연차 신청(LeaveApplyView — /#/LeaveApply)
- 진입: 마이 탭 → `[aria-label="연차 현황 보기"]`(연차 요약 섹션) → MyLeaveSummaryView → `button:has-text("연차 신청하기")`. (연차현황 화면은 하단 탭바 없음 — 복귀는 goBack)
- 종류: `button.type-item:has-text("연차"|"월차"|"포상휴가"...)` — 잔여 0/비대상은 disabled(`.type-item--off`)
- 단위 칩: `button.unit-chip` — 종일/반차/반반차(0.25일)/2시간/1시간/30분 (allowedUnits 서버 게이팅. 월차도 전 단위 허용, 포상휴가=종일만·무결재 즉시반영)
- 날짜: `button.dsf-field` → numeric 3개 시트 → `button.wp-confirm`
- 시간차: 시작 `button.tsf-field`(30분 스텝) / 종료는 [−][+] 스텝퍼(`button[aria-label="종료 시각 늘리기"]`), 표시값 `.end-stepper__val`
- preview 카드: `.preview-card` → "예상 차감 | 0일 30분 (0.075일)" 형식. 디바운스 400ms
- 결재자 추가: `button.btn-add` → 시트 `.laps__search-input`(이름 검색) → `button.laps__item:has-text(이름)` → `.laps__add-btn`. 후보 조건: 같은 사업장+USE_YN=Y+ACCOUNT_STATUS='01'+본인 제외
- 제출: `button:has-text("신청하기")` (마지막 매칭 사용 — "연차 신청하기"와 충돌 주의)
- 서버 가드 실측: 스케줄 밖 시간차="근무 시간 내에서만 시간차 연차를 신청할 수 있어요" / 잔여·가용창 부족="잔여 연차가 부족합니다"(둘 다 동일 문구)

### 앱 연차 결재함(LeaveApproval — 결재자 사용자 모드)
- 진입: 마이 탭 → `button.mp-menu__row:has-text("연차 결재 관리")` → 결재 대기/처리 내역 탭
- 카드: `article.lac` (요청자명+시각 텍스트로 다건 구분) → 상세 `/#/LeaveApprovalDetail`
- 승인: `button.lad-btn--approve`("요청대로 승인") / 반려: `button.lad-btn--reject` → 시트 `.ap-field__textarea`(10자↑) → `button:has-text("반려하기")`

### 웹 Attd_09 (사용자 연차관리)
- 행 체크박스 선택 → `button:has-text("일괄 수동 부여")` → 팝업: select(부여 유형), `input[type=number]`(일수), `input.calendar-input`(사용 가능일 = AVAIL_FROM_DATE, TO는 연말 고정) → `button:has-text("부여하기")`
- 시간차 표기: "N일 H시간 M분" 정확(원장 그대로) — 앱 요약과 달리 반올림 없음

### 웹 Attd_05 (근무계획 관리 — 연차 배정/변경요청)
- 법정휴가 배정: 대상 셀 클릭(선택) → 두번째 `button:has-text("적용")`(법정 휴가) → 셀 '법정휴가' 프리뷰 → `저장` → 확인 → 셀 '연차'. 휴일 셀은 자동 제외(당일이 휴일이면 "선택된 항목이 없습니다")
- 연차 변경/삭제 발의: 연차 셀 **더블클릭** → LeaveChangeRequestPop(radio MOVE/DELETE + 사유 textarea) → `요청`. (Attd_13 화면의 '생성' 버튼은 발의 진입 아님)

### 연차 변경 동의 흐름(Attd_13/14)
- 앱 대상자: 로그인 시 홈에 동의 시트 자동 노출([거부]/[동의]/[나중에])
- 웹 confirm: Attd_13 목록 '동의(확인대기)' 행 `button:has-text("확인")` → 팝업 `button:has-text("최종 확인(반영)")`
- 이력: Attd_14 (발의일시/근로자응답/처리상태)

### 웹 Attd_07 일자상세(OT 인플레이스)
- 셀 더블클릭 → 팝업 '관리자 직접 수정' → `+ 1구간 초과근무 추가` → `input.ot-time`(HHMM, **pressSequentially로 키입력 필수** — fill은 마스킹 미반영) → 기저장행은 행 체크박스 체크 후 편집 → `button:has-text("초과근무 저장")`(허용창 밖이면 disabled 선차단)
- 앱 경유 OT(ATTD_ID null) 수정 → POST /webApi/attd07/update-user-overtime-requests 404 ATTD_404_012 (기지 결함 #2 잔존)

## 주의(함정)
- 같은 날 미처리 보정 요청 잔존 시 신규 보정 상신 선차단('이미 등록된 미처리 요청이 있습니다').
- `:text("수정 요청")`/`:text("근태")`는 안내 문구·카드 제목과 충돌 — button 스코프 필수.
- com-015: QTHR을 앱에서 로그인하면 웹 토큰 revoke — 케이스마다 재로그인(프로세스 분리로 자연 해소).
- DB NOW()는 UTC 표기(KST-9h) — INSERT_DATE 판독 시 주의. 근태 시각 컬럼(HHMM)은 KST 기준.

## 휴일관리 Attd_02 (E3 실측 append)

- 라우트: /safenote/main/Attd_02 — 좌측 월 캘린더 + 우측 [목록|상세] 패널
- 월 이동: `button:has-text("<")` / `button:has-text(">")` (헤더 "2026년 7월")
- 일 선택: 첫 줄이 일자 숫자인 td 클릭(evaluate 탐색)
- 등록: 일 셀 선택 → `button:has-text("휴일 등록")` → 팝업: 휴일명 `input[placeholder="예: 창립기념일"]` + 일자 flatpickr(`input.calendar-input`, 선택일 프리필) + '매년 반복' checkbox → `button:has-text("등록")`(마지막 매칭). 확정 연차/근태 존재일에도 경고 없음(무경고 등록).
- 삭제: [목록] 탭 → 해당 행(지정휴무만 삭제 버튼 존재) `button:has-text("삭제")` — 확인 다이얼로그 없이 즉시 삭제. **DB는 soft-delete(USE_YN='N', 행 잔존)**. 같은 날 재등록 시 비활성 행 재활성이 아니라 신규 HOLIDAY_ID 채번.
- **공휴일(HOLIDAY_TYPE='01')은 UI 삭제 불가**: 목록 행에 '-', 상세 패널에 '국가 지정 공휴일은 삭제할 수 없습니다' 명시.
- 필터: select(.select-display) 전체/공휴일만/지정휴무만/반복휴무만.

## E3 기타 실측
- Attd_05 조회월 이동: `input.calendar-input`(flatpickr) `_flatpickr.setDate("2026-08-01", true)` → `button:has-text("조회")`.
- Attd_05 과거일 배정: 셀 선택·적용 프리뷰는 되지만 저장 시 **무반응 skip**(행 미생성, 안내 없음).
- 앱 내근태 이번달 월 이동: `button[aria-label="다음 달"]` / `[aria-label="이전 달"]` (.mn__btn).
- 앱 월캘린더 셀 상태 클래스: cal__d--wk(근무) / --lv(연차) / --hol(휴일) / --of(오프) / --td(오늘) / --sat/--sun — 연차+휴일 공존 시 --lv --hol 동시 부여.
- 앱 OT/보정 액션시트: 근태 없는 날은 '초과근무 신청' 항목 disabled(선차단).
- 앱 연차 신청: AVAIL_FROM_DATE 이전 과거일은 '잔여 연차가 부족합니다'로 차단(문구 동일 뭉뚱그림). 과거일 시드는 포상휴가(무결재 즉시확정) + 가용일 과거 설정으로 우회.

## E6 실측 append (소속이동·마감·비활성)
- 소속이동 예약(UserTransferPop 동일 EP): GET `/webApi/user01/{userCd}/transfer-eligibility?toSiteCd=&toDefaultSchCd=&moveDate=` → {eligible, blockReasons[]}. POST `/webApi/user01/transfer-reservation` {userCd,toSiteCd,toNodeCd,moveDate(YYYYMMDD 내일이후),toDefaultSchCd,moveReason} → {reservationId}. 불가⑤(USER_400_069)=신 근무타입이 미래 시간차 연차를 커버 못할 때만.
- 앱 소속이동 안내: 로그인 후 MainView 에서 `.tn-sheet`(heading '소속이동 안내') 자동 노출 → `.tn-sheet__btn--primary`('확인')=ack. ack 후 재로그인 미재노출. GET `/appApi/user01/my-transfer-notice` / POST `/appApi/user01/transfer-notice/ack`.
- 근태 마감(Attd_07): 버튼 `button.a07-btn-line`('근태 마감'/'마감 해제'), 배지 `.a07-issue-count`('처리 필요 N건'/'마감됨'). 조회 후에만 활성. GET `/webApi/attd07/attd-close-status?siteCd=&nodeCd=&incSubNodeYn=Y|N&closeYm=YYYYMM` → {closed,closable,pendingReqCnt,gpsUnconfirmedCnt,unapprovedOtCnt,blockTotalCnt}. POST `/webApi/attd07/attd-close` / `/attd-unclose` {siteCd,nodeCd(''=전체'*',master/hr만),incSubNodeYn,closeYm,closeDesc}. 차단=ATTD_400_040, 마감월 상신=ATTD_400_099.
  - 마감 차단조건 3종만: pendingReq(TB_USER_ATTD_REQ REQ_TYPE NOT IN 03,04, 대상월 WORK_YMD, status 01) + unapprovedOt(REQ_TYPE 03,04) + gpsUnconfirmed(mock GPS). **OPEN 슬롯·미래월 요청은 비차단**. 연차=REQ_TYPE '05'(대기시 차단), 보정=01/02, sched-modify=10.
- 사용자 비활성: User_01 행 useYn 셀렉트(SYS003: Y=사용/N=미사용) → 체크 → 저장(POST `/webApi/user01/update-user-infos` [{...full user, useYn, chk:true}]). ⚠️**비활성 사용자는 User_01 목록에서 미노출(useYn=N 조회도 0건)** → 재활성 UI 경로 없음, update-user-infos 재활성도 COMMON_500_001.
- 반려(관리자): POST `/webApi/attd07/reject-user-attd-requests` {reqId,siteCd,userCd,workYmd,workSeq('1'|'2'),nodeCd,rejectReason}. ⚠️ 대상 userCd 가 USE_YN='Y' AND WITHDRAWAL_DATE IS NULL 이어야 함(비활성=ATTD_404_011).
- 앱 시드 EP: 출근 POST `/appApi/attd01/check-in`{lat,lon,isMocked,workYmd?}(⚠️2회차 500=DEFECT#E6-1). 보정 POST `/appApi/req07/attd-correction`{workYmd,nodeCd,slots:[{workSeq,startDate,startTime,endDate,endTime}],reqReason}(미래일=ATTD_400_109). OT POST `/appApi/req07/overtime`(실근태 밖=ATTD_400_104). 연차 POST `/appApi/leaveflow/apply`{leaveCd(SYS_ANNUAL/SYS_MONTHLY),leaveType,workYmd,useUnitType(00종일),startTime,endTime,reason,approverUserCds[]}.
- User_01 수정팝업(UserInfoPop): 사업장/부서/기본근무타입 readonly + '소속이동' 버튼(변경 일원화, WEB_001-4).
- ⚠️com-015 세션충돌: 같은 계정을 브라우저(webLogin)와 getToken 으로 동시 WEB 인증하면 앞 토큰 revoke(AUTH_409_001). 브라우저 위상과 API 위상을 분리(evictSession 후 재발급)하거나 다른 계정 사용.
