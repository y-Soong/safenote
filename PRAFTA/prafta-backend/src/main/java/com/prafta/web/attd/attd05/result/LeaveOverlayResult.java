package com.prafta.web.attd.attd05.result;

/**
 * prafta-com-008-E-6: Attd_05 그리드 연차 오버레이 단건.
 *
 * <p>연차-스케줄 모델 전환(E-2) 이후 work_plan 셀에는 SCH_CD 가 유지되므로,
 * 그리드 셀의 "연차" 표시는 leave_use(종일 CONFIRMED) 오버레이로 렌더한다.
 * (userCd, workYmd) 키로 프론트가 work_plan 위에 오버레이한다.
 *
 * <p>prafta-com-008-B-5(D-E8): 연차 셀 개별 동의요청(attd13 DELETE) 진입을 위해 leaveId 를 동반한다.
 * 프론트(B-7)가 해당 셀의 LEAVE_ID 로 LeaveChangeRequestPop(DELETE) 을 열어 동의요청을 생성한다.
 *
 * <p>⚠️ MyBatis 위치매핑 — record 필드 순서 = SELECT 컬럼 순서.
 */
public record LeaveOverlayResult(
        String userCd
        , String workYmd
        , String leaveCd
        , String leaveId
) {
}
