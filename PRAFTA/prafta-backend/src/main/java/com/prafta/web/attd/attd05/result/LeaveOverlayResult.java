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
 * <p>2026-08-14: {@code pendingYn}('Y'/'N') 동반 — 연결된 연차사용 요청이 결재 대기('01')인지.
 * 연차 신청 시점에 사용실적이 CONFIRMED 로 선차감 생성되므로 승인 전/후가 화면에서 구분되지 않았다.
 * 프론트가 대기 건을 시각적으로 구분해 표시한다(표시 전용 — 잠금 판정은 서버가 별도로 한다).
 *
 * <p>⚠️ MyBatis 위치매핑 — record 필드 순서 = SELECT 컬럼 순서.
 */
public record LeaveOverlayResult(
        String userCd
        , String workYmd
        , String leaveCd
        , String leaveId
        , String pendingYn
) {
}
