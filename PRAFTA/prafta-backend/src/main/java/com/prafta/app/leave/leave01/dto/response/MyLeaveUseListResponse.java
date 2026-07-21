package com.prafta.app.leave.leave01.dto.response;

import java.util.List;

import com.prafta.app.leave.leave01.result.LeaveUseHistoryRow;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 "연차 현황" 사용 내역(연 단위) 응답.
 * <p>list 는 조회 연도(START_DATE 기준) CONFIRMED 사용 실적, 최신순.
 * 미래 시작일(사용예정) 확정분도 포함한다 — 표시 구분은 FE 가 startDate 로 판정.
 */
@Getter
@Builder
public class MyLeaveUseListResponse {

    /** 조회 연도(YYYY) — 요청 미지정 시 서버 보정값(올해). */
    private final String year;

    /** 사용 내역(최신순, 최대 500건). */
    private final List<LeaveUseHistoryRow> list;
}
