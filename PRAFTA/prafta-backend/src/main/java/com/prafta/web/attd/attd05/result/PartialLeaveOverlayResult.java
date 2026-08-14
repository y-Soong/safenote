package com.prafta.web.attd.attd05.result;

/**
 * Attd_05 그리드 부분 휴가(반차/시간차) 오버레이 단건.
 *
 * <p>종일 연차(USE_UNIT_TYPE='00')는 work_plan 셀 값을 "연차"로 대체하지만(LeaveOverlayResult),
 * 반차/시간차(USE_UNIT_TYPE != '00')는 그날 근무 스케줄(SCH_CD)이 그대로 살아있고 하루 일부만
 * 휴가이므로 셀 값을 덮지 않고 근무 스케줄 위에 별도 마커(칩)로 얹는다. 그래서 종일 오버레이와
 * 분리된 단일 출처로 (userCd, workYmd) 키에 휴가 단위/시간대를 동반 반환한다.
 *
 * <p>useUnitNm 은 SYS025(사용 단위) 코드명을 DB 에서 직접 도출한다(코드→라벨 단일 출처).
 * startTime/endTime 은 HHMM(시간단위 휴가 시. 반차 등 시각 미보유면 null), leaveMinutes 는 사용 분.
 *
 * <p>2026-08-14: {@code pendingYn}('Y'/'N') 동반 — 연결된 연차사용 요청이 결재 대기('01')인지.
 * 종일 오버레이({@code LeaveOverlayResult})와 동일 술어·동일 목적(승인 전/후 구분 표시).
 *
 * <p>⚠️ MyBatis 위치매핑 — record 필드 순서 = SELECT 컬럼 순서.
 */
public record PartialLeaveOverlayResult(
        String userCd
        , String workYmd
        , String useUnitType
        , String useUnitNm
        , String leaveCd
        , String startTime
        , String endTime
        , Integer leaveMinutes
        , String pendingYn
) {
}
