package com.prafta.web.attd.attd16.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.attd.attd16.application.query.LeaveUsageCalendarQuery;
import com.prafta.web.attd.attd16.result.LeaveUsageCalendarRowResult;

/**
 * ATTD16-T1 - 연차 사용 현황 캘린더 매퍼.
 *
 * <p>단일 SELECT 로 "월 범위와 교차하는 확정 연차 사용 실적"을 일자 단위까지 전개해 반환한다
 * (date_seq 재귀 CTE — Dashboard01Mapper.selectPatrolMonthMiss 의 DAYS CTE 패턴 미러).
 * 부서 하위 전개는 nodeCd 가 있고 incSubNodeYn='Y' 일 때만 node_tree 재귀 CTE 로 처리하며,
 * nodeCd 가 비어 있으면 부서 필터를 적용하지 않는다(사업장 전체 — plan §3).
 */
@Mapper
public interface Attd16Mapper {

    /**
     * 월 범위 교차 연차 사용 실적(LEAVE_STATUS='CONFIRMED', DEL_YN='N')을 일자 전개하여 조회한다.
     * 정렬은 일자 → 사용자명(가나다) → 시작시각. 부서 판정은 TB_USER.NODE_CD 단일 출처.
     */
    List<LeaveUsageCalendarRowResult> selectLeaveUsageCalendarList(LeaveUsageCalendarQuery query);
}
