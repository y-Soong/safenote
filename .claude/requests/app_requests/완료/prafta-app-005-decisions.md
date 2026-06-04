# prafta-app-005 확정 결정 (단일 출처)

> 분해: prafta-app-005-plan.md (§0~§2), UI: prafta-app-005-ui-spec.md. 본 문서는 사용자 확정 답변. developer/qa/security 는 이 결정을 사실로 따른다.

## 확정 (사용자 컨펌)
- **D-공식(배치1)**: 요청서 §2.3 폐기·교정 채택. `usedTotal=SUM(USED_DAYS)`, `planned(=화면 "사용예정")=USED_DAYS 중 START_DATE>오늘 CONFIRMED 분`, `used(=화면 "사용")=usedTotal-planned`, `remaining=granted-usedTotal`. (이중차감 제거 + home01 자동 정합)
- **D-Q1/Q3 활성집합**: `STATUS='ACTIVE' AND EXPIRE_YN='N' AND DEL_YN='N'` (home01과 100% 일치). expiringSoon 대상도 동일 활성집합.
- **D-Q4 그룹분류**: `GRANT_TYPE LIKE 'STATUTORY\_%'`=법정, `LIKE 'MANUAL\_%'`=법정외. `STATUTORY_TENURE_BONUS`는 법정. **TOTAL = prefix 무관 활성 전체합**(home01 정합).
- **D-Q5 사용률(확정)**: `usageRate = (granted==0) ? 0 : round(usedTotal / granted * 100)`. **현재 토글 그룹 기준**으로 갱신(§3.2 우선; 메타카드에 표시하되 그룹 전환 시 변경).
- **D-Q6 근속/경력**: `serviceMonths`=입사일~오늘 실근속(서버계산, 경력 미포함). `serviceCreditMonths`=SUM(TB_USER_SERVICE_CREDIT.CREDIT_MONTHS, USE_YN='Y') 별도 보조라벨, 0이면 숨김.
- **D-Q7 동선**: 진입 = MainView `AttendanceSummaryCard @click:leave` → 본 화면(라우트 `/MyLeaveSummaryView`, viewResolver 규약). 푸터 [연차 신청하기] = **앱 연차신청 폼 미구현 → "준비 중입니다" 폴백 + `// TODO(developer): 연차신청 폼 연동`**. Pull-to-refresh 미지원(진입 1회 GET).
- **D-Q8 콜아웃 닫기**: 32px 절충 유지 + `aria-label="닫기"`. 세션 한정 닫기(SessionStorage/메모리, 재진입 시 재표시).
- **엔드포인트**: `GET /appApi/leave01/my-leave-summary` (신규 `com.prafta.app.leave.leave01`, home01/attd01 패턴, JWT 클레임으로 userCd/cmpnyCd 도출, 파라미터 미수신).

## 응답 계약 (확정)
plan §1-2 JSON 그대로. `usageRate`만 위 D-Q5로 확정(=round(usedTotal/granted*100), 그룹별). hireDate 는 서버 YYYYMMDD 원본 → FE 포맷.

## 작업 단위
- 슬롯 A [backend]: `AppLeave01Controller/Service/ServiceImpl/Mapper(.xml)/DTO/result/param/query` — plan §1-3/§1-4 계산. **활성집합에 EXPIRE_YN='N' 반드시 포함**(plan §1-3 SQL은 EXPIRE_YN 누락이므로 D-Q1/Q3대로 추가). LeaveDashboardMapper 로직 SSOT 차용. 마이그레이션 없음.
- 슬롯 B~G [frontend]: planner 작성 골격(`views/leave/MyLeaveSummaryView.vue` + components 5종)에 로직 채우기. onMounted 1회 GET, 그룹토글 상태→자식 주입(추가 API 없음), 콜아웃 세션닫기, 푸터 활성=remaining>0, 진입연동(MainView onLeaveClick 교체), 일단위 표기(정수/소수1자리).
