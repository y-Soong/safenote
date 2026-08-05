package com.prafta.common.cmm.schedule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.schedule.mapper.result.LeaveLockDayResult;
import com.prafta.common.cmm.schedule.mapper.result.SchWindowResult;

/**
 * 공통 스케줄 변경 가드 매퍼(prafta-com-016 shared-schedule-guard).
 *
 * <p>확정 연차 / 미결 시간차 신청(E3) / 초과근무 보유 일자를 (사용자, 날짜목록) 입력으로 일괄 조회한다.
 * 술어는 attd05 / leaveflow 의 기존 단일출처와 동일하다:
 * <ul>
 *   <li>확정 연차: TB_USER_LEAVE_USE LEAVE_STATUS='CONFIRMED' AND DEL_YN='N'
 *       AND START_DATE &lt;= ymd &lt;= END_DATE (USE_UNIT_TYPE 무관 — 종일·반차·시간차 전부).</li>
 *   <li>미결 시간차(E3 당일분모 전환): TB_USER_ATTD_REQ REQ_TYPE='05' AND REQ_STATUS='01'
 *       AND DEL_YN='N' AND START_TIME/END_TIME NOT NULL AND WORK_YMD = ymd
 *       (시각 보유 = 시간차만 — 반차·반반차·종일 신청은 시각 미기록. F-F 실측 술어).</li>
 *   <li>OT: 등록(tb_user_overtime_mgmt 비취소) ∪ 신청(tb_user_attd_req REQ_TYPE 03/04, 상태 01/02).
 *       attd05 countDayOvertime 와 동일.</li>
 * </ul>
 */
@Mapper
public interface ScheduleGuardMapper {

    /**
     * 입력 날짜목록 중 연차 잠금 날짜를 (workYmd, useUnitType, pendingYn) 로 반환한다.
     * 확정 연차(pendingYn='N'): 다일 연차의 중간 일자도 START_DATE~END_DATE 구간으로 포함(그리드
     * 오버레이와 동일). 동일 일자에 사용단위가 여러 건이면 종일('00') 우선, 그 외엔 임의 1건(표시용).
     * 미결 시간차(pendingYn='Y', E3): useUnitType=null. 같은 날 확정+미결 공존 시 2행 반환.
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

    /**
     * 교차일 겹침 가드용 — (SCH_CD, asOfYmd) 기준 effective(APPLY_DATE &lt;= asOfYmd 중 최신) 근무타입
     * 버전 1건의 1·2구간 시각을 반환한다. 현재본(TB_SCH_MGMT)+이력본(TB_SCH_MGMT_HIST) 합집합에서
     * 동일 APPLY_DATE 중복 시 HIST_IDX 최댓값 1건을 채택한다. 해당 SCH_CD 가 없거나(예: 값이 휴가코드)
     * asOfYmd 이전 버전이 없으면 null.
     */
    SchWindowResult selectEffectiveSchWindow(@Param("cmpnyCd") String cmpnyCd,
                                             @Param("siteCd") String siteCd,
                                             @Param("schCd") String schCd,
                                             @Param("asOfYmd") String asOfYmd);

    /**
     * 교차일 겹침 가드용 — (cmpny, site, user, ymd) 한 칸의 WORK_PLAN_CD 를 반환한다(없으면 null).
     * 이웃 날짜의 적용 스케줄 코드 조회에 사용한다.
     */
    String selectUserWorkPlanCd(@Param("cmpnyCd") String cmpnyCd,
                                @Param("siteCd") String siteCd,
                                @Param("userCd") String userCd,
                                @Param("workYmd") String workYmd);
}
