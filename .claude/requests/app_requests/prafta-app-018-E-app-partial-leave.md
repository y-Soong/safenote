# prafta-app-018-E — [앱 BE+FE] 내 근태 시간차/부분연차 상세 표시

상위: `prafta-app-018-leave-apply-plan.md`. 연차 테스트 중 발견(이슈 2, 사용자 (a)안 확정). 앱 영역.

## 배경/문제
앱 "내 근태"(주간/월간/오늘)가 그날 연차 사용을 **종일 휴가처럼 "월차"로만** 표시한다. 실제로는 시간차(예: 90분, 0.1875일) **부분 사용**인데 단위/시각/차감일수가 안 보인다. (시간차/반차는 근무계획이 연차로 대체되지 않아 그날은 이미 근무일로 유지됨 — `upsertWorkPlanLeave`는 종일만. 표시 마커만 빈약.)

## 확정 결정 (사용자 (a)안)
연차 사용 마커를 부분휴가 상세로: 예 `월차 · 시간차 · 03:00~04:30 · 0.19일` (반차=`월차 · 반차 · 0.5일`, 종일=현행 유지). 그날은 **근무일로 유지**(시간차/반차).

## 현황 (조사 완료)
- 앱 BE `app/attd/attd01`: `LeaveUseResult`(startDate/endDate/leaveCd/leaveNm/**leaveDays만**) + `selectLeaveUseByRange`(LEAVE_STATUS='CONFIRMED'). `expandLeave`→leaveByYmd→주간/월간/오늘 응답에 isLeaveUsed/leaveTypeName/leaveDays 정도. **useUnitType/START_TIME/END_TIME/LEAVE_MINUTES 없음**.
- TB_USER_LEAVE_USE 에 USE_UNIT_TYPE(SYS025)/START_TIME/END_TIME/LEAVE_MINUTES/LEAVE_DAYS 존재(웹 D에서 확인).

## 구현
### BE (app attd01)
- `LeaveUseResult` + `selectLeaveUseByRange` 에 `useUnitType`(U.USE_UNIT_TYPE)·`startTime`(U.START_TIME)·`endTime`(U.END_TIME)·`leaveMinutes`(U.LEAVE_MINUTES) 추가. record 위치매핑(SELECT 끝=record 끝). 단위 라벨은 FE 매핑 또는 FNC SYS025(둘 중 planner 결정 — 앱 다른 코드라벨 관례 따름).
- 주간/월간/오늘 응답의 연차 표시 필드 확장: 기존 leaveTypeName 옆에 `leaveUnitLabel`(종일/반차/시간차N)·`leaveTimeRange`(시간차면 시작~종료)·`leaveDays` 노출. WeekDayResponse/MonthDayResponse/MyAttendanceDay(today·day-detail) 일관 적용. ⚠️ 여러 응답 DTO 확장 — Lombok boolean is-접두 함정/@JsonProperty 주의(메모리 참조).
- 시각 표시는 (일자+시각) 정합 불필요(같은 날 부분구간) — HHMM 그대로. 종일/반차는 시각 null.

### FE (app-frontend MyAttendanceView + 주간/월간/오늘 컴포넌트)
- 연차 사용 마커를 `leaveTypeName · leaveUnitLabel · (시간차면 timeRange) · {leaveDays}일` 로 표기. 차감일수 정규화(0.18750→0.19 또는 0.1875 — 표시 자릿수 planner 결정, 웹 D normalizeDays 와 정합 권장).
- 종일은 현행 유지(라벨만). 시간차/반차는 그날 근무 스케줄도 함께 보이게(근무일 유지) — 휴가로 그날을 덮지 않음.

## 수용 기준/엣지
- 시간차/반차/종일 3종 마커 정확. 시간차는 시각+일수, 종일은 라벨만.
- 부분휴가일이 근무일로 유지(스케줄 표시 유지, 출퇴근/표준화 로직 무영향).
- 차감일수 표시 정규화. 다중 연차 사용/같은 날 복수 건 처리.
- record 위치매핑 밀림 없음. 빌드/SFC 통과.

## 정책 출처
attd §8(연차)·§10(근태표시), prafta-019(시간차·LEAVE_DAYS), [[project_prafta_app_002_my_attendance]].
