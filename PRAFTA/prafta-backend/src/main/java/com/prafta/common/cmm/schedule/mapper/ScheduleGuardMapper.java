package com.prafta.common.cmm.schedule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.schedule.mapper.result.LeaveLockDayResult;

/**
 * 공통 스케줄 변경 가드 매퍼(prafta-com-016 shared-schedule-guard).
 *
 * <p>확정 연차 / 초과근무 보유 일자를 (사용자, 날짜목록) 입력으로 일괄 조회한다.
 * 술어는 attd05 / leaveflow 의 기존 단일출처와 동일하다:
 * <ul>
 *   <li>연차: TB_USER_LEAVE_USE LEAVE_STATUS='CONFIRMED' AND DEL_YN='N'
 *       AND START_DATE &lt;= ymd &lt;= END_DATE (USE_UNIT_TYPE 무관 — 종일·반차·시간차 전부).</li>
 *   <li>OT: 등록(tb_user_overtime_mgmt 비취소) ∪ 신청(tb_user_attd_req REQ_TYPE 03/04, 상태 01/02).
 *       attd05 countDayOvertime 와 동일.</li>
 * </ul>
 */
@Mapper
public interface ScheduleGuardMapper {

    /**
     * 입력 날짜목록 중 확정 연차가 덮인 날짜를 (workYmd, useUnitType) 로 반환한다.
     * 다일 연차의 중간 일자도 START_DATE~END_DATE 구간으로 포함한다(그리드 오버레이와 동일).
     * 동일 일자에 사용단위가 여러 건이면 종일('00') 우선, 그 외엔 임의 1건으로 묶는다(표시용).
     */
    List<LeaveLockDayResult> selectLeaveLockedDays(@Param("cmpnyCd") String cmpnyCd,
                                                   @Param("userCd") String userCd,
                                                   @Param("workYmds") List<String> workYmds);

    /**
     * 입력 날짜목록 중 초과근무(등록 또는 신청)가 있는 날짜를 반환한다.
     * 판정 술어는 attd05 countDayOvertime 와 동일(SITE_CD 포함).
     */
    List<String> selectOvertimeLockedDays(@Param("cmpnyCd") String cmpnyCd,
                                          @Param("siteCd") String siteCd,
                                          @Param("userCd") String userCd,
                                          @Param("workYmds") List<String> workYmds);
}
