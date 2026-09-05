package com.prafta.web.subcon.subcon03.result;

/**
 * NF-2a(2026-08-07): 스냅샷 기간 내 확정 <b>부분연차(반차 {@code USE_UNIT_TYPE='01'})</b> 1건의 면제 시각 구간.
 *
 * <p>하도급 근태 공유 스냅샷의 {@code ATTD_STATUS_CD} 가 원 스케줄 시각만으로 판정되어
 * 반차일에 지각·조퇴가 잘못 찍히던 결함을 닫는다(웹 Attd_08/Attd_11·앱과 답이 갈리던 지점).
 * 판정 자체는 서비스가 {@code PartialLeaveWindowUtils} 단일 출처로 수행하고, 여기서는 원본 시각만 내려준다.
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — {@code Subcon03Mapper.selectHalfLeaveWindows} 와 순서 일치.
 */
public record HalfLeaveWindowRow(
        String userCd
        , String workYmd        // = TB_USER_LEAVE_USE.START_DATE (연차 1행 = 하루 불변식)
        , String startTime      // HHmm
        , String endTime        // HHmm
        , String useUnitType    // BW-05: 01 반차 / 02~04 시간차 (★record 끝 - 위치매핑)
        , String brkWaiveYn     // BW-05: 휴게 무시 요청 Y/N (반차 01 + Y = 그날 휴게 공제 0)
        , Integer brkWaiveMin   // v2(BW2-13): 넘긴 휴게 분량(NULL + Y = v1 전부) — 공제 = max(0, 쉰 휴게 − W) (★record 끝)
) {
}
