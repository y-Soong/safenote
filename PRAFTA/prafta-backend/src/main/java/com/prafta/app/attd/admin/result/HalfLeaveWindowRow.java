package com.prafta.app.attd.admin.result;

/**
 * NF-1(2026-08-07): 그날 확정 <b>부분연차(반차 {@code USE_UNIT_TYPE='01'})</b> 1건의 면제 시각 구간.
 *
 * <p>앱 관리자 근태 현황(일자·월별)이 지각·조퇴를 원 스케줄 시각으로만 판정해
 * "Attd_08·Attd_11·앱 홈은 정상인데 앱 관리자 화면만 지각"이 되던 결함을 닫는다.
 * 웹 {@code Attd08Mapper/Attd11Mapper.selectHalfLeaveWindows} 와 <b>동일 술어</b>의 미러 조회다.
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — {@code AppAdminAttdMapper.selectHalfLeaveWindows} 와 순서 일치.
 */
public record HalfLeaveWindowRow(
        String userCd
        , String workYmd        // = TB_USER_LEAVE_USE.START_DATE (연차 1행 = 하루 불변식)
        , String startTime      // HHmm
        , String endTime        // HHmm
) {
}
