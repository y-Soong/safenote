package com.prafta.common.cmm.leave.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import com.prafta.common.cmm.leave.vo.DailyScheduleVO;
import com.prafta.common.cmm.leave.vo.HourlyLeaveAggVO;
import com.prafta.common.cmm.leave.vo.LeaveTimeWindowVO;

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

    /**
     * PC-03(D1·N4): 사용자 기본 근무타입({@code tb_user.DEFAULT_SCH_CD})의 대상일 기준 유효
     * 스케줄 시각 조회 — 개인 분모(1일 환산시간) 산출 입력.
     *
     * <p>{@code selectDailySchedule} 의 effective-dating 서브쿼리 패턴을 재사용하되, WORK_PLAN
     * 조인 대신 {@code tb_user.DEFAULT_SCH_CD} 를 직접 참조한다(사업장은 사용자 현재 SITE_CD 파생).
     * DEFAULT_SCH_CD 미지정(NULL/빈값)이거나 참조 스케줄이 없으면 {@code null}.
     */
    DailyScheduleVO selectUserDefaultSchedule(@Param("cmpnyCd") String cmpnyCd,
                                              @Param("userCd") String userCd,
                                              @Param("workYmd") String workYmd);

    /**
     * 그날 기존 시간차(02/03/04) CONFIRMED 누적 분·누적 차감 합 (LC-03, F3).
     *
     * <p><b>전 연차타입 합산</b>(LEAVE_CD 불문 — 타입을 나눠 쪼개는 우회 차단, F3).
     * 고정단위(종일 00/반차 01/반반차 05)는 하한 마일스톤 누적에서 제외(plan §8-⑤).
     * 항상 1행 반환(대상 없으면 0/0).
     */
    HourlyLeaveAggVO selectHourlyLeaveAggOnDate(@Param("cmpnyCd") String cmpnyCd,
                                                @Param("userCd") String userCd,
                                                @Param("workYmd") String workYmd);

    /**
     * sec N-2(2026-08-07): 그날 "시각 보유" 연차(반차 '01' + 시간차 '02'~'04') CONFIRMED 행의 시각 구간 목록.
     *
     * <p>종전 겹침 판정({@code countOverlappingTimeLeaveOnDate})은 SQL 안에서 각 행의 wrap 을
     * 독립적으로 해석해 한쪽만 wrap 되는 조합의 프레임이 어긋났다. 판정을 Java 로 옮기기 위해
     * <b>원본 시각만</b> 내려주고 절대 시각 환산은 {@code PartialLeaveWindowUtils.exemptStampRange}
     * 단일 진입점이 담당한다. 술어는 구 SQL 과 100% 동일(회귀 0).
     *
     * <p>조회 단위가 (사용자, 날짜)라 행 수는 통상 0~3건이다.
     *
     * <p>O-1(2026-08-07): 형제 쿼리({@code Attd07Mapper.selectLeaveExemptWindows} /
     * {@code AppAttd01Mapper.selectPartialLeaveWindowsOn})와 동일하게 {@code SITE_CD} 스코프를 건다.
     */
    List<LeaveTimeWindowVO> selectTimeLeaveWindowsOnDate(@Param("cmpnyCd") String cmpnyCd,
                                                         @Param("siteCd") String siteCd,
                                                         @Param("userCd") String userCd,
                                                         @Param("workYmd") String workYmd);
}
