package com.prafta.app.home.home01.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.home.home01.application.query.HomeSummaryQuery;
import com.prafta.app.home.home01.result.AppliedLeaveSummaryResult;
import com.prafta.app.home.home01.result.AttdMgmtResult;
import com.prafta.app.home.home01.result.LeaveSummaryResult;
import com.prafta.app.home.home01.result.OvernightScheduleResult;
import com.prafta.app.home.home01.result.PrevDayOpenAttdResult;
import com.prafta.app.home.home01.result.ScheduleResult;
import com.prafta.app.home.home01.result.TbmStatusResult;

/**
 * prafta-app-001: 앱 메인화면 요약 조회 Mapper.
 * <p>4개 영역을 개별 SELECT 로 조회한다(영역별 테이블/조건이 달라 단일 쿼리 무리).
 */
@Mapper
public interface AppHome01Mapper {

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

    /** 오늘 예정 스케줄(work_plan → sch_mgmt 1구간). 매칭 스케줄 미존재 시 null. */
    ScheduleResult selectTodaySchedule(@Param("param") HomeSummaryQuery query);

    /**
     * prafta-com-008-E (H1): 기준일(baseYmd) 종일 연차일 여부(USE_UNIT_TYPE='00' 확정 연차 존재 카운트).
     * <p>attd01 checkIn(083) 의 AppAttd01Mapper.countFullDayLeaveOn 과 동일 술어(단일출처 기준).
     *   E-2 전환으로 연차일에도 work_plan 에 SCH_CD 가 남아 스케줄이 조회되는 문제를 보정하기 위해
     *   출퇴근 카드에서 종일 연차일이면 출근 버튼을 비활성화한다. &gt; 0 이면 종일 연차일.
     *   부분연차(반차/시간차)는 근무일 유지 → 미카운트.
     */
    int countFullDayLeaveOn(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("userCd") String userCd
            , @Param("baseYmd") String baseYmd
    );

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

    /**
     * prafta-app-021: 직전일(WORK_YMD = today-1) 열린(미퇴근) 근태 1건(WORK_SEQ DESC). 없으면 null.
     * <p>존재하면 메인 카드의 전날 미퇴근 마감 대기 신호(prevDayCheckoutPending)와 퇴근 버튼 활성(canCheckOut)
     *   근거가 된다(§7.6). 본인 스코프(cmpny/site/user) + DEL_YN='N' + CHECK_IN_TIME 有 + CHECK_OUT_TIME NULL.
     * @param query  본인 스코프(query.todayYmd() 는 baseYmd 가 아닌 DB 기준 오늘 — 호출부에서 today 주입)
     * @param prevYmd 직전일(today-1, YYYYMMDD)
     */
    PrevDayOpenAttdResult selectPrevDayOpenAttd(
            @Param("param") HomeSummaryQuery query
            , @Param("prevYmd") String prevYmd);

    /** 연차 요약 합산(ACTIVE & 미소멸 전체). 부여 이력 없으면 SUM null 가능. */
    LeaveSummaryResult selectLeaveSummary(@Param("param") HomeSummaryQuery query);

    /**
     * 연차 개편(표시): 홈 요약용 신청형 휴가('01') 집계(보유 타입 수 + 총잔여).
     * <p>법정/관리자부여(selectLeaveSummary)와 분리. 사용분 술어는 AppLeaveFlowMapper.selectFiscalUsedDays 동일.
     *   회계연도 경계는 서비스가 {@code FiscalYearUtils} 로 산출해 주입. '01' 0개면 typeCount=0, SUM null.
     */
    AppliedLeaveSummaryResult selectAppliedLeaveSummary(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd
            , @Param("fiscalStartYmd") String fiscalStartYmd
            , @Param("fiscalEndYmdExclusive") String fiscalEndYmdExclusive
    );

    /** 결재 대기(REQ_STATUS='01') 본인 건수. */
    int selectPendingApprovalCount(@Param("param") HomeSummaryQuery query);

    /** 오늘 TBM 세션(최신 1건) + 본인 참석 상태. 세션 없으면 null. */
    TbmStatusResult selectTodayTbm(@Param("param") HomeSummaryQuery query);

    /**
     * prafta-app-015: 본인 소속부서 NODE_CD 조회 (월마감 커버리지 판정용).
     * <p>없으면 null. TB_USER. (AppAttd01Mapper.selectUserNodeCd 동일 패턴)
     */
    String selectUserNodeCd(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("userCd") String userCd
    );

    /**
     * prafta-app-015: 해당 월이 본인 부서를 덮는 마감(CLOSED)으로 닫혀 있는지 여부.
     * <p>1 이상이면 마감(closed). prafta-028 부서단위 마감 커버리지('*'/자기노드/INC_SUB 상위).
     *   attd01 의 구간 게이팅(canCheckInThisSlot)과 의미 정렬을 위해 home01 도 동일 판정.
     *   (AppAttd01Mapper.countCoveringClose 동일 패턴)
     */
    int countCoveringClose(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("nodeCd") String nodeCd
            , @Param("closeYm") String closeYm
    );
}
