package com.prafta.common.cmm.leave.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.vo.DailyScheduleVO;

/**
 * 시간차 연차 동적 차감 전용 조회 Mapper (prafta-019-A §2.4).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1.1(사용단위), §8.5.9(1일 환산)
 */
@Mapper
public interface LeaveDeductionMapper {

    /**
     * 사용자/일자의 적용 근무 스케줄 시각 조회.
     *
     * <p>{@code tb_user_work_plan}(PK: CMPNY_CD,SITE_CD,USER_CD,WORK_YMD)에서 WORK_PLAN_CD 를
     * 얻고, 이를 {@code tb_sch_mgmt.SCH_CD}로 조인한다. WORK_PLAN_CD 가 연차 코드이거나
     * 근무 계획/스케줄이 없으면 {@code null}.
     */
    DailyScheduleVO selectDailySchedule(@Param("cmpnyCd") String cmpnyCd,
                                        @Param("siteCd") String siteCd,
                                        @Param("userCd") String userCd,
                                        @Param("workYmd") String workYmd);
}
