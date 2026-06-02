package com.prafta.app.leave.leave01.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.leave.leave01.application.query.MyLeaveSummaryQuery;
import com.prafta.app.leave.leave01.result.LeaveExpiringResult;
import com.prafta.app.leave.leave01.result.LeaveGroupAggResult;
import com.prafta.app.leave.leave01.result.LeaveUserResult;

/**
 * prafta-app-005: 앱 "연차 현황" 조회 Mapper.
 * <p>그룹 집계(STATUTORY/MANUAL/TOTAL)는 grantTypePrefix 주입으로 동일 SQL 을 3회 호출한다
 *   (prefix null=TOTAL prefix 무관 전체합). LeaveDashboardMapper(웹 연차현황) SSOT 차용.
 */
@Mapper
public interface AppLeave01Mapper {

    /** DB 기준 오늘 일자(YYYYMMDD, varchar8). 그룹/예정/소멸 쿼리 공통 기준일. */
    String selectTodayYmd();

    /**
     * 그룹별 집계 1건(granted/usedTotal/planned).
     * <p>query.grantTypePrefix 가 null 이면 TOTAL(prefix 무관 전체합),
     *   'STATUTORY\_%'/'MANUAL\_%' 이면 해당 그룹. 부여 없으면 SUM 이 null.
     */
    LeaveGroupAggResult selectGroupAgg(@Param("param") MyLeaveSummaryQuery query);

    /** 소멸 임박(D-30) 집계 1건(그룹 무관, 활성집합 동일). 대상 0건이면 targetCount=0. */
    LeaveExpiringResult selectExpiringSoon(@Param("param") MyLeaveSummaryQuery query);

    /** 사용자 메타(userNm/hireDate/serviceCreditMonths). 스코프 밖/미존재면 null. */
    LeaveUserResult selectUser(@Param("param") MyLeaveSummaryQuery query);
}
