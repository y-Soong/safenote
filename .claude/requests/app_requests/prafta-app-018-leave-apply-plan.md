# prafta-app-018 — 앱 연차(휴가) 신청 (상위 개요 + 분할 인덱스)

## 배경
앱 "내 근태" 액션시트의 "연차 신청"과 연차현황 화면 "연차 신청하기"가 현재 stub("준비 중입니다")다(`MyAttendanceView.onSheetAction` leave 분기, `MyLeaveSummaryView`). 연차 신청은 prafta-app-007 범위가 아니었고(요청서는 스케줄수정/근태보정/초과근무 3종만), 앱에는 연차 **신청(쓰기)** 경로가 없다. 읽기 `GET /appApi/leave01/my-leave-summary`만 존재.
⚠️ 웹 연차 신청의 실제 상태(정정): **백엔드** `POST /webApi/leaveflow/apply`(LeaveFlow, prafta-019)는 종일/반차/시간차·차감·결재선까지 완비. 그러나 이를 호출하는 **웹 FE 팝업 `LeaveApplyPop.vue` 는 어느 화면에도 연결되지 않은 고아 컴포넌트**(정적 참조 0건)다 → 웹에는 현재 작동하는 자가 연차신청 UI가 없다. 웹의 연차 사용은 Attd_05(근무계획관리)에서 **관리자가 법정연차를 종일로 직접 적용**하는 경로뿐(신청/결재 아님·시간차 없음). 따라서 본 앱 작업이 **첫 실사용 자가 연차신청 면**이며, **백엔드 LeaveFlow 로직이 유일한 미러 레퍼런스**(웹 FE 패리티 대상 아님)다.

## 확정 결정 (사용자)
- **D1 결재선**: 옵션(a) — 앱에서도 신청자가 결재선(결재자/프리셋)을 구성하고 결재선 INSERT까지 한다(= prafta-app-009 결재선 통합을 연차 범위에서 함께 구현). 자기승인 원칙(§9.5) 포함.
- **D2 사용단위 게이팅**: 단위 전부 지원하되 **휴가별 허용 단위로 게이팅**한다. 법정(SYSTEM_YN='Y')은 `TB_LEAVE_USAGE_POLICY.USAGE_UNIT`(회사 단일값), 비법정은 `TB_LEAVE_TYPE_MGMT.USE_UNIT_TYPE`(타입 단일값)을 출처로 한다. ⚠️ 웹 고아 팝업(LeaveApplyPop)은 SYS025 전체를 노출하고 단위 검증도 안 했고(prafta-024 "신청 강제 보류"), 그나마 미연결이라 실사용 안 됨 → **게이팅은 본 앱 폼이 처음 구현**한다. FE 드롭다운 노출 제한 + BE 제출 단위 검증(이중).
- **D3 잔여검증**: 웹과 동일하게 신청 즉시 차감대상 부여 1건 선택(만료 임박 우선), 없으면 거부(`ATTD_400_051`).
- **D4 진입점**: 액션시트 leave + 연차현황 "연차 신청하기" → **동일 폼** 라우팅.

## D2-a 사용단위 게이팅 의미 — **확정 (Y) 계층형**
설정 단위는 "허용 **최소** 단위"다. 설정값 granularity 이하(=설정 단위 + 더 큰 단위 전부)를 허용한다.
granularity 순서(굵→잘게): FULL_DAY(00) → HALF_DAY(01) → HOUR_2(02) → HOUR_1(03) → MIN_30(04).
- 예: 허용단위=1시간(03) → 종일·반차·2시간·1시간 신청 가능, 30분(04)만 불가.
- **종일/반차의 시간차 표현(사용자 확정)**: 허용단위가 시간 단위(예 1시간)인 휴가에서 종일은 "시작=스케줄 시작시각, 종료=스케줄 종료시각"으로 시간 단위 증가시켜 표현하고, 반차도 동일 방식. 차감일수는 기존 `calcDeductionDays`(요청분÷소정근로분, 휴게 제외)로 자동 산출됨(반차는 0.5 고정).
- **편의 기능(사용자 승인)**: 폼에 "종일/반차" 버튼을 두어 해당 시각을 자동 입력(시작=스케줄 시작, 종료=종일이면 스케줄 종료/반차면 절반). 표시 편의일 뿐 제출은 단위/시각으로 처리.

## 참조 (코드/테이블)
- 웹 신청 서비스(미러 원본): `web/attd/leaveflow/service/impl/LeaveFlowServiceImpl.java#submitLeave`(단위 구조검증 99~135, 사후마감 137~146, 잔여 148~152, 요청 INSERT 154~159, 결재선 161~227), 컨트롤러 `LeaveFlowController`(`POST /leaveflow/apply`), DTO `LeaveApplyRequest`, 매퍼 `LeaveFlowMapper.xml`.
- 웹 신청 팝업(FE 참조): `web-frontend/.../views/attd/popup/LeaveApplyPop.vue`(unitOptions/결재선/프리셋).
- 앱 기존: `app/leave/leave01/*`(읽기 my-leave-summary), `app/req/req07/*`(요청 폼 3종 — 결재선 INSERT는 TODO(prafta-app-009) 미구현), `prafta-app-frontend/.../views/req/AttdRequestView.vue`·`MyAttendanceView.vue`(라우팅), `views/leave/MyLeaveSummaryView.vue`.
- 테이블: `TB_USER_ATTD_REQ`(REQ_TYPE='05' 연차사용, REQ_STATUS SYS033 01신청/02승인/03반려), `TB_USER_LEAVE_USE`(LEAVE_STATUS CONFIRMED/CANCELLED), `TB_USER_LEAVE_GRANT`(USED_DAYS 재계산), `TB_USER_ATTD_REQ_APPROVAL`(APPROVAL_STEP/APPROVER_USER_CD/APPROVAL_STATUS SYS044), `TB_APRV_LINE_PRESET`(본인 프리셋, prafta-020), `TB_LEAVE_USAGE_POLICY.USAGE_UNIT`, `TB_LEAVE_TYPE_MGMT.USE_UNIT_TYPE/APRV_USE_YN/SYSTEM_YN`. 사용단위 SYS025(00 1일/01 반차/02 2h/03 1h/04 30분).
- 신규 엔드포인트 prefix는 `/appApi`. 식별값(cmpny/site/user/node)은 JWT(TokenInfo gv_*)만(IDOR). 비TS·CSS변수·scoped·공통컴포넌트. 시각비교는 (일자+시각)stamp.

## 분할 인덱스 (순차 의존: A → B → C)
- **prafta-app-018-A** [앱 BE 조회] — 연차 신청 폼 메타 API: 신청가능 연차종류 목록 + 종류별 허용 사용단위(D2 게이팅 출처 산출) + 잔여, 결재선 프리셋/결재자 조회(앱 미러). `prafta-app-018-A-leave-meta.md`
- **prafta-app-018-B** [앱 BE 신청쓰기+결재선] — `POST /appApi/leaveflow/apply`: web submitLeave 앱 미러(요청 INSERT 05 + leave_use + grant recompute + 잔여검증 + 단위 구조검증 + **단위 게이팅(신규)** + 사후마감) + 결재선 INSERT/자기승인(D1, app-009 연차범위). `prafta-app-018-B-leave-submit.md`
- **prafta-app-018-C** [앱 FE] — LeaveApplyForm.vue(종류선택/단위 게이팅 드롭다운/날짜·시간차 시각/사유/결재선 선택/잔여표시) + 진입점 2곳 동일 폼 라우팅 + A·B 연동. `prafta-app-018-C-leave-form.md`
- **prafta-app-018-D** [웹 FE+BE] — 일자상세 팝업(AttdDayDetailPop) 연차(05/06) 요청 카드 표시 정정(단위/시간차 범위/차감일수). 앱 신청의 **웹 승인 측 짝** — 앱 자가신청 활성화 전/함께 완료 권장. `prafta-app-018-D-web-leave-card.md`
- **prafta-app-018-E** [앱 BE+FE] — 내 근태 시간차/부분연차 상세 표시(이슈2 (a)안: `월차·시간차·03:00~04:30·0.19일`, 그날 근무일 유지). `prafta-app-018-E-app-partial-leave.md`
- **prafta-app-018-F** [웹 BE+FE] — 일자상세 팝업에 **확정 연차(시간차) 표시**(이슈3: 자동확정 연차는 요청으로 안 떠 D로도 안 보임 → 확정 연차 사용 내역 별도 표시). `prafta-app-018-F-web-confirmed-leave.md`

> 테스트 중 발견 추가 수정(이미 적용): 시간차 연차 **근무시간 내 검증**(스케줄 밖 거부, `LeaveDeductionService.withinScheduledWorkHours`+`ATTD_400_103`). 웹 submitLeave 동일 갭은 공통 메서드 한 줄 추가로 적용 가능(follow-up).

순차 권장: A → (B, C 병렬 가능) → 통합검증. D는 웹 영역이라 A/B/C와 파일 비충돌 → 병렬 가능(단 B의 LEAVE_TYPE/LEAVE_DAYS 저장 컨벤션과 정합). 각 단위는 planner→developer→qa→security 워크플로우.

## 공통 follow-up
- prafta-app-009 결재선 통합을 연차 외 나머지 앱 요청폼(스케줄/보정/초과근무)까지 일반화(본 작업은 연차 범위만).
- 사후 신청 마감(attdClose) 정밀 판정 앱 적용 여부 확인.
