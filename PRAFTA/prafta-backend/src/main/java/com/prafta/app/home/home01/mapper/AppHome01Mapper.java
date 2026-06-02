package com.prafta.app.home.home01.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.home.home01.application.query.HomeSummaryQuery;
import com.prafta.app.home.home01.result.AttdMgmtResult;
import com.prafta.app.home.home01.result.LeaveSummaryResult;
import com.prafta.app.home.home01.result.OvernightScheduleResult;
import com.prafta.app.home.home01.result.ScheduleResult;
import com.prafta.app.home.home01.result.TbmStatusResult;

/**
 * prafta-app-001: 앱 메인화면 요약 조회 Mapper.
 * <p>4개 영역을 개별 SELECT 로 조회한다(영역별 테이블/조건이 달라 단일 쿼리 무리).
 */
@Mapper
public interface AppHome01Mapper {

    /** DB 기준 오늘 일자(YYYYMMDD, varchar8). 4개 영역의 공통 기준일. */
    String selectTodayYmd();

    /** 오늘 근태 1건(가장 큰 WORK_SEQ). 없으면 null. */
    AttdMgmtResult selectTodayAttd(@Param("param") HomeSummaryQuery query);

    /** 오늘 근태 전체(구간별, WORK_SEQ 오름차순). 2구간 카드의 현재 구간 판정용. 없으면 빈 리스트. */
    java.util.List<AttdMgmtResult> selectTodayAttdList(@Param("param") HomeSummaryQuery query);

    /**
     * 오늘 가장 최근 출근 레코드(ATTD_ID)에 출근 GPS 행(GPS_INFO_TYPE='01')이 존재하는지(외근 여부).
     * <p>존재(1)=근무지 밖(외근), 미존재/오늘 출근 없음(0)=정상. 본인 스코프 + DEL_YN='N'.
     *   GPS 행은 지오펜스 밖일 때만 저장되므로(prafta-app-003 A0-2 확정 모델), 존재=외근 좌표.
     */
    int selectTodayCheckInOffsite(@Param("param") HomeSummaryQuery query);

    /** 오늘 예정 스케줄(work_plan → sch_mgmt 1구간). 연차/휴무 또는 미존재 시 null. */
    ScheduleResult selectTodaySchedule(@Param("param") HomeSummaryQuery query);

    /**
     * prafta-app-013-1: 전일 스케줄 1건(FST/SEC 시작·종료시각 4개). 오버나이트 기준일 판정용.
     * <p>전일 WORK_YMD 의 TB_USER_WORK_PLAN → TB_SCH_MGMT INNER JOIN.
     *   휴무/미배정(WORK_PLAN_CD null) 또는 매칭 스케줄 없으면 결과 없음 → null.
     * @param query   본인 스코프(cmpny/site/user)
     * @param prevYmd 전일 일자(YYYYMMDD)
     */
    OvernightScheduleResult selectScheduleEndForOvernight(
            @Param("param") HomeSummaryQuery query
            , @Param("prevYmd") String prevYmd);

    /** 연차 요약 합산(ACTIVE & 미소멸 전체). 부여 이력 없으면 SUM null 가능. */
    LeaveSummaryResult selectLeaveSummary(@Param("param") HomeSummaryQuery query);

    /** 결재 대기(REQ_STATUS='01') 본인 건수. */
    int selectPendingApprovalCount(@Param("param") HomeSummaryQuery query);

    /** 오늘 TBM 세션(최신 1건) + 본인 참석 상태. 세션 없으면 null. */
    TbmStatusResult selectTodayTbm(@Param("param") HomeSummaryQuery query);
}
